"""Reproduce a public catalog proxy with the pinned Dzoka encoder and disposable pgvector."""
import argparse
from collections import Counter, defaultdict
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import time
import unicodedata
import uuid

from test_foundation_container import PGVECTOR_DIAGNOSTIC
from matching_calibration import evaluate

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SHA = 'c5e7723f4a4afa3da4f35c497ab30ac0f8ee5b3e473ec0ffa897511e127e9324'
# Amazon Berkeley Objects by Amazon.com and dataset contributors, CC BY 4.0.
SOURCE = 'https://amazon-berkeley-objects.s3.amazonaws.com/listings/metadata/listings_0.json.gz'
LIMITS = {'SHOES': 240, 'CHAIR': 120, 'HANDBAG': 45, 'CELLULAR_PHONE_CASE': 220}


def normalize(value):
    return ' '.join(unicodedata.normalize('NFC', value).split())


def select(path):
    if hashlib.sha256(path.read_bytes()).hexdigest() != SOURCE_SHA:
        raise ValueError('Expected the frozen public ABO shard')
    retained = defaultdict(list)
    seen_items, seen_titles, seen_images = set(), set(), set()
    with gzip.open(path,'rt',encoding='utf-8') as stream:
        for line in stream:
            row = json.loads(line)
            categories = [value['value'] for value in row.get('product_type', [])]
            category = next((name for name in categories if name in LIMITS), None)
            if category is None or len(retained[category]) >= LIMITS[category]:
                continue
            titles = sorted((title['language_tag'], normalize(title['value'])) for title in row.get('item_name', []))
            english = next((text for language, text in titles if language.startswith('en_') and text), None)
            other = next(((language, text) for language, text in titles
                          if not language.startswith('en_') and text and text.casefold() != (english or '').casefold()), None)
            item, image = row['item_id'], row.get('main_image_id')
            if english is None or item in seen_items or english.casefold() in seen_titles or image in seen_images:
                continue
            seen_items.add(item)
            seen_titles.add(english.casefold())
            if image:
                seen_images.add(image)
            retained[category].append((english, other))
    return retained


def vector(provider, text):
    result = provider.embed(text)
    if result['truncated']:
        return None
    return "'[" + ','.join(map(str,result['vector'])) + "]'::public.vector"


def cases(provider, rows):
    gallery, queries, counts = [], [], Counter()
    for category, members in rows.items():
        for english, other in members:
            target = vector(provider,english)
            if target is None:
                continue
            key = len(gallery)+1
            tenant = key % 2
            gallery.append(f"({key},{tenant},'{category}',{target})")
            if other and category!='CELLULAR_PHONE_CASE' and counts[category]<12:
                language, text = other
                intent = vector(provider,text)
                if intent is not None:
                    if not re.fullmatch(r'[a-zA-Z]{2,8}(?:_[a-zA-Z0-9]{2,8})?',language):
                        raise ValueError('Unexpected catalog language tag')
                    queries.append(f"({key},{tenant},'{category}','{language}',{intent})")
                    counts[category] += 1
    if len(gallery)<513 or counts!=Counter({'SHOES':12,'CHAIR':12,'HANDBAG':12}):
        raise ValueError('Frozen query/gallery coverage differs')
    return gallery,queries


BENCHMARK = """
ANALYZE gallery;
CREATE INDEX m5_catalog_hnsw ON gallery USING hnsw (embedding public.vector_cosine_ops);
CREATE TEMP TABLE measurements(category text,language text,filtered boolean,k int,neighbors int,
    exact_hit boolean,indexed_hit boolean,scope_ok boolean);
DO $$
DECLARE q record; filtered boolean; exact_ids int[]; approx_ids int[]; plan json;
BEGIN
  PERFORM set_config('hnsw.iterative_scan','strict_order',true);
  FOR q IN SELECT * FROM probes ORDER BY id LOOP
    FOREACH filtered IN ARRAY ARRAY[false,true] LOOP
      PERFORM set_config('enable_seqscan','on',true);
      PERFORM set_config('enable_indexscan','off',true);
      PERFORM set_config('enable_bitmapscan','off',true);
      SELECT array(SELECT g.id FROM gallery g WHERE g.tenant=q.tenant AND (NOT filtered OR g.category=q.category)
        ORDER BY g.embedding OPERATOR(public.<=>) q.embedding LIMIT 10) INTO exact_ids;
      PERFORM set_config('enable_seqscan','off',true);
      PERFORM set_config('enable_indexscan','on',true);
      EXECUTE 'EXPLAIN (FORMAT JSON) SELECT id FROM gallery WHERE tenant=$1 AND (NOT $2 OR category=$3)
        ORDER BY embedding OPERATOR(public.<=>) $4 LIMIT 10' INTO plan USING q.tenant,filtered,q.category,q.embedding;
      IF position('m5_catalog_hnsw' IN plan::text)=0 THEN RAISE EXCEPTION 'Expected HNSW plan'; END IF;
      SELECT array(SELECT g.id FROM gallery g WHERE g.tenant=q.tenant AND (NOT filtered OR g.category=q.category)
        ORDER BY g.embedding OPERATOR(public.<=>) q.embedding LIMIT 10) INTO approx_ids;
      INSERT INTO measurements
        SELECT q.category,q.language,filtered,k,
          (SELECT count(*) FROM unnest(approx_ids[1:k]) id WHERE id=ANY(exact_ids[1:k])),
          q.id=ANY(exact_ids[1:k]),q.id=ANY(approx_ids[1:k]),
          cardinality(approx_ids)=10 AND NOT EXISTS(SELECT 1 FROM gallery g WHERE g.id=ANY(approx_ids)
           AND (g.tenant<>q.tenant OR (filtered AND g.category<>q.category)))
        FROM unnest(ARRAY[1,5,10]) k;
    END LOOP;
  END LOOP;
END $$;
SET LOCAL enable_seqscan='on';
SET LOCAL enable_indexscan='off';
SET LOCAL enable_bitmapscan='off';
CREATE TEMP TABLE calibration_pairs AS
  SELECT p.id AS query,g.id AS candidate,(p.id % 4 < 2) AS fit,p.id=g.id AS label,
    greatest(0,least(1,1-(g.embedding OPERATOR(public.<=>) p.embedding))) AS score
  FROM probes p CROSS JOIN LATERAL (
    SELECT chosen.id,chosen.embedding FROM (
      SELECT negative.id,negative.embedding FROM gallery negative
      WHERE negative.id<>p.id AND negative.category=p.category AND (negative.id % 4 < 2)=(p.id % 4 < 2)
      ORDER BY negative.embedding OPERATOR(public.<=>) p.embedding LIMIT 4
    ) chosen
    UNION ALL SELECT positive.id,positive.embedding FROM gallery positive WHERE positive.id=p.id
  ) g;
SELECT json_build_object(
  'galleryCount',(SELECT count(*) FROM gallery),'queryCount',(SELECT count(*) FROM probes),
  'languageCounts',(SELECT json_object_agg(language,n) FROM (SELECT language,count(*) n FROM probes GROUP BY language) s),
  'allIndexedPlansVerified',true,'extensionVersion',(SELECT extversion FROM pg_extension WHERE extname='vector'),
  'metrics',(SELECT json_agg(s ORDER BY category,filtered,k) FROM (
    SELECT coalesce(category,'ALL') AS category,filtered,k,count(*) AS queries,sum(neighbors) AS neighbors_overlap,
      count(*) FILTER(WHERE exact_hit) AS exact_listing_hits,count(*) FILTER(WHERE indexed_hit) AS indexed_listing_hits,
      count(*) FILTER(WHERE NOT scope_ok) AS scope_violations
    FROM measurements GROUP BY GROUPING SETS ((category,filtered,k),(filtered,k))
  ) s),'calibrationPairs',(SELECT json_agg(c ORDER BY query,candidate) FROM calibration_pairs c));
ROLLBACK;
"""


def measure(gallery,queries):
    name = 'dzoka-m5-catalog-'+uuid.uuid4().hex
    password = secrets.token_urlsafe(32)
    env = {**os.environ, 'POSTGRES_PASSWORD':password, 'PGPASSWORD':password, 'PGCLIENTENCODING':'UTF8'}

    def docker(*args, **options):
        return subprocess.run(['docker',*args],env=env,text=True,encoding='utf-8',capture_output=True,
                              timeout=180,check=True,**options).stdout

    try:
        docker('run','--detach','--pull','never','--network','none','--name',name,
               '--env','POSTGRES_PASSWORD',PGVECTOR_DIAGNOSTIC)
        for _ in range(80):
            ready = subprocess.run(['docker','exec',name,'pg_isready','-U','postgres','-h','127.0.0.1'],
                                   capture_output=True,timeout=4)
            if ready.returncode==0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError('Disposable PostgreSQL unavailable')
        body = ("CREATE EXTENSION vector WITH VERSION '0.8.7';\nBEGIN;\nSET LOCAL statement_timeout='60s';\n"
                "CREATE TEMP TABLE gallery(id int,tenant int,category text,embedding public.vector(384));\n"
                "CREATE TEMP TABLE probes(id int,tenant int,category text,language text,embedding public.vector(384));\n"
                "INSERT INTO gallery VALUES "+',\n'.join(gallery)+";\n"
                "INSERT INTO probes VALUES "+',\n'.join(queries)+";\n"+BENCHMARK)
        return json.loads(docker('exec','-i','--env','PGPASSWORD','--env','PGCLIENTENCODING',name,'psql',
                                 '-h','127.0.0.1','-U','postgres','-X','-qAt','-v','ON_ERROR_STOP=1',input=body))
    finally:
        removed = subprocess.run(['docker','rm','--force','--volumes',name],env=env,capture_output=True,timeout=60)
        if removed.returncode!=0 and b'No such container' not in removed.stderr:
            raise RuntimeError('Disposable catalog database cleanup needs attention')


def calibration(pairs,space):
    def digest(value):
        return hashlib.sha256(value.encode()).hexdigest()

    fit = {item for row in pairs if row['fit'] for item in (row['query'],row['candidate'])}
    holdout = {item for row in pairs if not row['fit'] for item in (row['query'],row['candidate'])}
    if fit & holdout:
        raise ValueError('Catalog identity overlaps fit and holdout')
    plan = dict(schemaVersion=1,datasetSha256=SOURCE_SHA,spaceKey=space,scoringVersion='bounded-evidence-v1',
                minimumCoverage=0.3,cases=[
                    dict(caseSha256=digest(f"catalog-pair/{row['query']}/{row['candidate']}"),
                         groupSha256=digest(f"catalog-item/{row['query']}"),split='fit' if row['fit'] else 'holdout',
                         label=row['label'],similarity=row['score'],coverage=0.45*0.7,disposition='COMPARABLE')
                    for row in pairs])
    result = evaluate(plan,digest(json.dumps(plan,sort_keys=True,separators=(',',':'))))
    result['scope'] = 'PUBLIC_CATALOG_VECTOR_ONLY_WITH_FOUR_MINED_NEGATIVES_PER_QUERY'
    result['identityOverlap'] = len(fit & holdout)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--catalog',type=Path,required=True)
    parser.add_argument('--model-dir',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args = parser.parse_args()
    sys.path.insert(0,str(ROOT/'ai'))
    from provider import TextEmbeddingProvider
    provider = TextEmbeddingProvider(args.model_dir)
    gallery, queries = cases(provider,select(args.catalog))
    report = measure(gallery,queries)
    space=provider.profile['space']['spaceKey']
    report['calibration']=calibration(report.pop('calibrationPairs'),space)
    report.update(scope='PUBLIC_CATALOG_MULTILINGUAL_PROXY', source=SOURCE, sourceSha256=SOURCE_SHA,
                  license='CC-BY-4.0', spaceKey=space,
                  image=PGVECTOR_DIAGNOSTIC)
    with args.output.open('x',encoding='utf-8',newline='\n') as stream:
        json.dump(report,stream,indent=2)
        stream.write('\n')


if __name__ == '__main__':
    main()
