-- Authorized synthetic data in a disposable diagnostic database only.
BEGIN;
SET LOCAL statement_timeout = '20s';
CREATE TEMP TABLE matching_recall_fixture (
    id integer PRIMARY KEY,
    tenant integer NOT NULL,
    space text NOT NULL,
    active boolean NOT NULL,
    embedding public.vector(3) NOT NULL
);
INSERT INTO matching_recall_fixture
SELECT n, n % 3, CASE WHEN n % 7 = 0 THEN 'incompatible' ELSE 'compatible' END,
       n % 11 <> 0, ARRAY[1.0,n/1000.0,0.01]::public.vector
FROM generate_series(1,1200) AS n;
ANALYZE matching_recall_fixture;
CREATE TEMP TABLE exact_neighbors AS
SELECT id FROM matching_recall_fixture
WHERE tenant=1 AND space='compatible' AND active
ORDER BY embedding OPERATOR(public.<=>) '[1,0,0.01]'::public.vector LIMIT 10;
CREATE INDEX matching_recall_hnsw ON matching_recall_fixture
USING hnsw (embedding public.vector_cosine_ops) WHERE space='compatible';
ANALYZE matching_recall_fixture;
SET LOCAL enable_seqscan=off;
SET LOCAL hnsw.iterative_scan='strict_order';
SET LOCAL hnsw.max_scan_tuples=10000;
DO $$
DECLARE plan json;
BEGIN
  EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON)
    SELECT id FROM matching_recall_fixture
    WHERE tenant=1 AND space=''compatible'' AND active
    ORDER BY embedding OPERATOR(public.<=>) ''[1,0,0.01]''::public.vector LIMIT 10' INTO plan;
  IF position('matching_recall_hnsw' in plan::text)=0 THEN
    RAISE EXCEPTION 'Expected HNSW index plan';
  END IF;
END $$;
CREATE TEMP TABLE index_neighbors AS
SELECT id FROM matching_recall_fixture
WHERE tenant=1 AND space='compatible' AND active
ORDER BY embedding OPERATOR(public.<=>) '[1,0,0.01]'::public.vector LIMIT 10;
SELECT json_build_object(
    'scope','SYNTHETIC_FILTERED_INDEX_RECALL',
    'pool', (SELECT count(*) FROM matching_recall_fixture), 'k',10, 'planUsesHnsw',true,
    'extensionVersion',(SELECT extversion FROM pg_extension WHERE extname='vector'),
    'exactCount',(SELECT count(*) FROM exact_neighbors),
    'indexCount',(SELECT count(*) FROM index_neighbors),
    'overlap',(SELECT count(*) FROM exact_neighbors JOIN index_neighbors USING(id)),
    'filterViolations',(SELECT count(*) FROM index_neighbors JOIN matching_recall_fixture USING(id)
        WHERE tenant<>1 OR space<>'compatible' OR NOT active));
ROLLBACK;
