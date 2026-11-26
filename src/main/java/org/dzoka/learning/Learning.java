package org.dzoka.learning;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public final class Learning {
    private final TenantTransactions tx;private final ObjectMapper json;
    public Learning(TenantTransactions tx,ObjectMapper json){this.tx=tx;this.json=json;}
    public record Policy(int version,boolean optIn,boolean requireGate,int retentionDays,String purposeSha) {}
    public record Declaration(UUID run,UUID candidate,Boolean positive) {public Declaration{if(run==null||candidate==null||positive==null)throw new IllegalArgumentException();}}
    public record Feedback(UUID id,UUID report,UUID candidate,String kind,String sourceSha,Instant observedAt) {}
    public record Adjudication(UUID feedback,int revision,String label,String institutionSha,String evidenceSha) {public Adjudication{if(feedback==null||label==null)throw new IllegalArgumentException();}}
    public record Case(UUID adjudication,String split) {public Case{if(adjudication==null||split==null)throw new IllegalArgumentException();}}
    public record Dataset(List<Case> cases,String purposeSha) { public Dataset{if(cases==null||cases.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException();cases=List.copyOf(cases);if(cases.isEmpty()||cases.size()>100)throw new IllegalArgumentException();} }
    public record DatasetView(UUID id,String manifestSha,int cases) {}
    public record Experiment(UUID dataset,UUID rollout,String provider,String modelVersion,String modelSha,String configSha,String evaluationSha,int k,List<Evaluation.Result> results) {
        public Experiment {if(dataset==null||rollout==null||provider==null||modelVersion==null||results==null||results.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException();sha(modelSha);sha(configSha);sha(evaluationSha);results=List.copyOf(results);}
    }
    public record Approval(String evidenceSha,double minimumCoverage,double minimumRecall,double maximumBrier) {}
    public record ExperimentView(UUID id,UUID dataset,UUID rollout,String evaluationSha,Evaluation.Metrics metrics) {}
    public static PreparedStatement q(Connection db,String sql,Object... values)throws SQLException {var s=db.prepareStatement(sql);for(int i=0;i<values.length;i++)s.setObject(i+1,values[i]);return s;}
    public static int write(Connection db,String sql,Object... values)throws SQLException {try(var s=q(db,sql,values)){return s.executeUpdate();}}
    public static void sha(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Digest required");}
    public static String hash(String value){return ContentDigest.sha256(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public static void lock(Connection db,UUID tenant)throws SQLException {try(var s=q(db,"SELECT pg_advisory_xact_lock(hashtextextended('learning:'||?::text,25))",tenant)){s.execute();}}
    public void policy(VerifiedIdentity actor,UUID tenant,Policy policy) {
        sha(policy.purposeSha());if(policy.version()<1||policy.retentionDays()<1||policy.retentionDays()>365)throw new IllegalArgumentException();
        tx.execute(actor,tenant,"learning:review",db->{lock(db,tenant);Policy old=policy(db);
            if(old!=null && old.version()>=policy.version()){if(!old.equals(policy))throw new Failures.Conflict();return null;}
            if(policy.version()!=(old==null?1:old.version()+1))throw new Failures.Conflict();
            write(db,"INSERT INTO dzoka.learning_policies(tenant_id,version,opt_in,require_gate,retention_days,purpose_sha,actor_ref) VALUES(?,?,?,?,?,?,?)",tenant,policy.version(),policy.optIn(),policy.requireGate(),policy.retentionDays(),policy.purposeSha(),actor.reference());return null;
        });
    }
    private static Policy policy(Connection db)throws SQLException {
        try(var s=q(db,"SELECT * FROM dzoka.learning_policies ORDER BY version DESC LIMIT 1");var r=s.executeQuery()){
            return !r.next()?null:new Policy(r.getInt("version"),r.getBoolean("opt_in"),r.getBoolean("require_gate"),r.getInt("retention_days"),r.getString("purpose_sha"));
        }
    }
    public Feedback declare(VerifiedIdentity actor,UUID tenant,UUID id,Declaration value) {
        if(value==null||value.run()==null||value.candidate()==null)throw new IllegalArgumentException();
        return tx.execute(actor,tenant,"feedback:submit",db->{lock(db,tenant);
            UUID report;String source;int sourceRevision,candidateRevision;
            try(var s=q(db,"SELECT r.report_id,r.revision,c.revision FROM dzoka.match_search_runs r JOIN dzoka.match_search_candidates c ON(c.tenant_id,c.run_id)=(r.tenant_id,r.id) WHERE r.client_id=? AND c.report_id=? AND r.actor_ref=?",value.run(),value.candidate(),actor.reference());var r=s.executeQuery()){
                if(!r.next())throw new Failures.NotFound();report=r.getObject(1,UUID.class);sourceRevision=r.getInt(2);candidateRevision=r.getInt(3);source=hash(value.run()+":"+report+":"+r.getInt(2)+":"+value.candidate()+":"+r.getInt(3)+":"+value.positive());
            }
            try(var s=q(db,"SELECT source_sha FROM dzoka.learning_feedback WHERE id=?",id);var r=s.executeQuery()){
                if(r.next()){if(!source.equals(r.getString(1)))throw new Failures.Conflict();return feedback(db,id);}
            }
            try(var s=q(db,"SELECT 1 FROM dzoka.learning_feedback WHERE source_sha=?",source);var r=s.executeQuery()){if(r.next())throw new Failures.Conflict();}
            write(db,"INSERT INTO dzoka.learning_feedback(tenant_id,id,report_id,candidate_id,source_id,source_revision,candidate_revision,kind,source_sha,actor_ref,observed_at) VALUES(?,?,?,?,?,?,?,?,?,?,clock_timestamp())",
                    tenant,id,report,value.candidate(),value.run(),sourceRevision,candidateRevision,value.positive()?"DECLARED_MATCH":"DECLARED_REJECTION",source,actor.reference());return feedback(db,id);
        });
    }
    public List<Feedback> feedback(VerifiedIdentity actor,UUID tenant) {
        return tx.execute(actor,tenant,"learning:review",db->{var result=new ArrayList<Feedback>();
            try(var s=q(db,"SELECT * FROM dzoka.learning_feedback WHERE dzoka.learning_source_live(report_id) ORDER BY recorded_at DESC,id LIMIT 100");var r=s.executeQuery()){
                while(r.next())result.add(row(r));
            }return List.copyOf(result);
        });
    }
    private static Feedback row(ResultSet r)throws SQLException {return new Feedback(r.getObject("id",UUID.class),r.getObject("report_id",UUID.class),r.getObject("candidate_id",UUID.class),r.getString("kind"),r.getString("source_sha"),r.getTimestamp("observed_at").toInstant());}
    private static Feedback feedback(Connection db,UUID id)throws SQLException {try(var s=q(db,"SELECT * FROM dzoka.learning_feedback WHERE id=? AND dzoka.learning_source_live(report_id)",id);var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();return row(r);}}
    public void adjudicate(VerifiedIdentity actor,UUID tenant,UUID id,Adjudication value) {
        sha(value.institutionSha());sha(value.evidenceSha());if(value.feedback()==null||value.revision()<1||!Set.of("POSITIVE","NEGATIVE","UNAVAILABLE").contains(value.label()))throw new IllegalArgumentException();
        tx.execute(actor,tenant,"learning:review",db->{lock(db,tenant);feedback(db,value.feedback());
            try(var s=q(db,"SELECT actor_ref FROM dzoka.learning_feedback WHERE id=?",value.feedback());var r=s.executeQuery()){r.next();if(actor.reference().equals(r.getString(1)))throw new Failures.Forbidden();}
            try(var s=q(db,"SELECT feedback_id,revision,label,institution_sha,evidence_sha,actor_ref FROM dzoka.learning_adjudications WHERE id=?",id);var r=s.executeQuery()){
                if(r.next()){if(!value.equals(new Adjudication(r.getObject(1,UUID.class),r.getInt(2),r.getString(3),r.getString(4),r.getString(5)))||!actor.reference().equals(r.getString(6)))throw new Failures.Conflict();return null;}
            }
            try(var s=q(db,"SELECT coalesce(max(revision),0)+1 FROM dzoka.learning_adjudications WHERE feedback_id=?",value.feedback());var r=s.executeQuery()){r.next();if(r.getInt(1)!=value.revision())throw new Failures.Conflict();}
            write(db,"INSERT INTO dzoka.learning_adjudications(tenant_id,id,feedback_id,revision,label,institution_sha,evidence_sha,actor_ref) VALUES(?,?,?,?,?,?,?,?)",tenant,id,value.feedback(),value.revision(),value.label(),value.institutionSha(),value.evidenceSha(),actor.reference());return null;
        });
    }
    public DatasetView dataset(VerifiedIdentity actor,UUID tenant,UUID id,Dataset value) {
        sha(value.purposeSha());
        return tx.execute(actor,tenant,"learning:manage",db->{lock(db,tenant);var policy=policy(db);
            if(policy==null||!policy.optIn()||!policy.purposeSha().equals(value.purposeSha()))throw new Failures.Conflict();
            var ordered=value.cases().stream().sorted(Comparator.comparing(c->c.adjudication().toString())).toList();
            String manifest=hash(json.writeValueAsString(ordered)+":"+policy.version()+":"+value.purposeSha());
            try(var s=q(db,"SELECT manifest_sha,expected_rows FROM dzoka.learning_datasets WHERE id=?",id);var r=s.executeQuery()){
                if(r.next()){if(!manifest.equals(r.getString(1)))throw new Failures.Conflict();return new DatasetView(id,manifest,r.getInt(2));}
            }
            Map<String,String> items=new HashMap<>(),institutions=new HashMap<>();Map<String,List<Instant>> times=new HashMap<>();var prepared=new ArrayList<List<Object>>();var seen=new HashSet<UUID>();var pairs=new HashSet<String>();
            for(var item:ordered){
                if(!Set.of("TRAIN","VALIDATION","TEST").contains(item.split()))throw new IllegalArgumentException();
                try(var s=q(db,"SELECT a.feedback_id,a.label,a.institution_sha,m.* FROM dzoka.learning_adjudications a CROSS JOIN LATERAL dzoka.learning_case_metadata(a.feedback_id) m WHERE a.id=? AND NOT EXISTS(SELECT 1 FROM dzoka.learning_adjudications n WHERE n.feedback_id=a.feedback_id AND n.revision>a.revision)",item.adjudication());var r=s.executeQuery()){
                    if(!r.next()||r.getString("label").equals("UNAVAILABLE"))throw new Failures.Conflict();
                    UUID feedback=r.getObject("feedback_id",UUID.class);if(!seen.add(feedback))throw new IllegalArgumentException("Repeated feedback");
                    String sourceItem=hash(r.getObject("item",UUID.class).toString());UUID candidate=r.getObject("candidate_item",UUID.class);if(candidate==null)throw new Failures.Conflict();String other=candidate==null?null:hash(candidate.toString());
                    if(!pairs.add(sourceItem.compareTo(other)<0?sourceItem+other:other+sourceItem))throw new Failures.Conflict();
                    bind(items,sourceItem,item.split());if(other!=null)bind(items,other,item.split());bind(institutions,r.getString("institution_sha"),item.split());
                    times.computeIfAbsent(item.split()+"_observed",k->new ArrayList<>()).add(r.getTimestamp("observed_at").toInstant());
                    times.computeIfAbsent(item.split()+"_recorded",k->new ArrayList<>()).add(r.getTimestamp("recorded_at").toInstant());
                    prepared.add(Arrays.asList(feedback,item.adjudication(),sourceItem,other,item.split()));
                }
            }
            for(var pair:List.of(List.of("TRAIN","VALIDATION"),List.of("TRAIN","TEST"),List.of("VALIDATION","TEST"))){
                var left=times.get(pair.get(0)+"_recorded");var right=times.get(pair.get(1)+"_observed");
                if(left!=null&&right!=null&&!Collections.max(left).isBefore(Collections.min(right)))throw new Failures.Conflict();
            }
            write(db,"INSERT INTO dzoka.learning_datasets(tenant_id,id,policy_version,manifest_sha,purpose_sha,expected_rows,author_ref,expires_at) VALUES(?,?,?,?,?,?,?,clock_timestamp()+(? * interval '1 day'))",tenant,id,policy.version(),manifest,value.purposeSha(),prepared.size(),actor.reference(),policy.retentionDays());
            for(var item:prepared)write(db,"INSERT INTO dzoka.learning_cases(tenant_id,dataset_id,feedback_id,adjudication_id,item_sha,candidate_item_sha,split) VALUES(?,?,?,?,?,?,?)",tenant,id,item.get(0),item.get(1),item.get(2),item.get(3),item.get(4));
            return new DatasetView(id,manifest,prepared.size());
        });
    }
    private static void bind(Map<String,String> groups,String key,String split){String previous=groups.putIfAbsent(key,split);if(previous!=null&&!previous.equals(split))throw new Failures.Conflict();}
    public ExperimentView experiment(VerifiedIdentity actor,UUID tenant,UUID id,Experiment value) {
        for(String sha:List.of(value.modelSha(),value.configSha(),value.evaluationSha()))sha(sha);
        for(String label:List.of(value.provider(),value.modelVersion()))if(label==null||!label.matches("[A-Za-z0-9._/-]{1,80}"))throw new IllegalArgumentException();
        return tx.execute(actor,tenant,"learning:manage",db->{lock(db,tenant);requireDataset(db,value.dataset());
            String artifact=json.writeValueAsString(value);
            try(var s=q(db,"SELECT artifact::text FROM dzoka.learning_experiments WHERE id=?",id);var r=s.executeQuery()){
                if(r.next()){if(!json.readTree(r.getString(1)).equals(json.readTree(artifact)))throw new Failures.Conflict();return experiment(db,id);}
            }
            try(var s=q(db,"SELECT evaluation_sha256 FROM dzoka.fingerprint_rollouts WHERE id=?",value.rollout());var r=s.executeQuery()){if(!r.next()||!r.getString(1).equals(value.evaluationSha()))throw new Failures.Conflict();}
            try(var s=q(db,"SELECT 1 FROM dzoka.fingerprint_rollout_spaces p JOIN dzoka.embedding_spaces s ON(s.tenant_id,s.id)=(p.tenant_id,p.space_id) WHERE p.rollout_id=? AND s.provider=? AND s.artifact_revision=? AND s.artifact_sha256=? AND s.config_sha256=?",value.rollout(),value.provider(),value.modelVersion(),value.modelSha(),value.configSha());var r=s.executeQuery()){if(!r.next())throw new Failures.Conflict();}
            Map<UUID,Boolean> labels=new HashMap<>();
            try(var s=q(db,"SELECT c.feedback_id,a.label FROM dzoka.learning_cases c JOIN dzoka.learning_adjudications a ON(a.tenant_id,a.id)=(c.tenant_id,c.adjudication_id) WHERE c.dataset_id=? AND c.split='TEST'",value.dataset());var r=s.executeQuery()){
                while(r.next())labels.put(r.getObject(1,UUID.class),r.getString(2).equals("POSITIVE"));
            }
            var metrics=Evaluation.compute(value.results(),labels,value.k());
            write(db,"INSERT INTO dzoka.learning_experiments(tenant_id,id,dataset_id,rollout_id,provider,model_version,model_sha,config_sha,evaluation_sha,artifact,metrics,author_ref) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?)",
                    tenant,id,value.dataset(),value.rollout(),value.provider(),value.modelVersion(),value.modelSha(),value.configSha(),value.evaluationSha(),artifact,json.writeValueAsString(metrics),actor.reference());
            write(db,"INSERT INTO dzoka.mlflow_exports(tenant_id,experiment_id) VALUES(?,?)",tenant,id);return experiment(db,id);
        });
    }
    private static void requireDataset(Connection db,UUID id)throws SQLException {try(var s=q(db,"SELECT dzoka.learning_dataset_live(?)",id);var r=s.executeQuery()){r.next();if(!r.getBoolean(1))throw new Failures.Conflict();}}
    private ExperimentView experiment(Connection db,UUID id)throws SQLException {
        try(var s=q(db,"SELECT dataset_id,rollout_id,evaluation_sha,metrics::text FROM dzoka.learning_experiments WHERE id=?",id);var r=s.executeQuery()){
            if(!r.next())throw new Failures.NotFound();requireDataset(db,r.getObject(1,UUID.class));return new ExperimentView(id,r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),json.readValue(r.getString(4),Evaluation.Metrics.class));
        }
    }
    public ExperimentView experiment(VerifiedIdentity actor,UUID tenant,UUID id){return tx.execute(actor,tenant,"learning:review",db->experiment(db,id));}
    public void approve(VerifiedIdentity actor,UUID tenant,UUID id,Approval value) {
        sha(value.evidenceSha());for(double n:new double[]{value.minimumCoverage(),value.minimumRecall(),value.maximumBrier()})if(!Double.isFinite(n)||n<0||n>1)throw new IllegalArgumentException();
        tx.execute(actor,tenant,"learning:review",db->{lock(db,tenant);var view=experiment(db,id);var m=view.metrics();
            try(var s=q(db,"SELECT author_ref FROM dzoka.learning_experiments WHERE id=?",id);var r=s.executeQuery()){r.next();if(actor.reference().equals(r.getString(1)))throw new Failures.Forbidden();}
            if(m.coverage()<value.minimumCoverage()||m.recallAtK()==null||m.baselineRecallAtK()==null||m.recallAtK()<m.baselineRecallAtK()
                ||m.recallAtK()<value.minimumRecall()||m.brier()==null||m.brier()>value.maximumBrier()||m.probabilityCases()!=m.cases())throw new Failures.Conflict();
            try(var s=q(db,"SELECT count(DISTINCT split) FROM dzoka.learning_cases WHERE dataset_id=? AND split IN('TRAIN','TEST')",view.dataset());var r=s.executeQuery()){r.next();if(r.getInt(1)!=2)throw new Failures.Conflict();}
            try(var s=q(db,"SELECT evidence_sha,minimum_coverage,minimum_recall,maximum_brier,actor_ref FROM dzoka.learning_approvals WHERE experiment_id=?",id);var r=s.executeQuery()){
                if(r.next()){if(!value.equals(new Approval(r.getString(1),r.getDouble(2),r.getDouble(3),r.getDouble(4)))||!actor.reference().equals(r.getString(5)))throw new Failures.Conflict();return null;}
            }
            write(db,"INSERT INTO dzoka.learning_approvals(tenant_id,experiment_id,actor_ref,evidence_sha,minimum_coverage,minimum_recall,maximum_brier) VALUES(?,?,?,?,?,?,?)",tenant,id,actor.reference(),value.evidenceSha(),value.minimumCoverage(),value.minimumRecall(),value.maximumBrier());return null;
        });
    }
    public void revoke(VerifiedIdentity actor,UUID tenant,String kind,UUID id,String evidence) {
        sha(evidence);if(!Set.of("DATASET","EXPERIMENT").contains(kind))throw new IllegalArgumentException();
        tx.execute(actor,tenant,"learning:review",db->{lock(db,tenant);
            try(var s=q(db,"SELECT evidence_sha,actor_ref FROM dzoka.learning_revocations WHERE kind=? AND entity_id=?",kind,id);var r=s.executeQuery()){if(r.next()){if(!evidence.equals(r.getString(1))||!actor.reference().equals(r.getString(2)))throw new Failures.Conflict();return null;}}
            String table=kind.equals("EXPERIMENT")?"learning_experiments":"learning_datasets";
            try(var s=q(db,"SELECT 1 FROM dzoka."+table+" WHERE id=?",id);var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();}
            write(db,"INSERT INTO dzoka.learning_revocations(tenant_id,kind,entity_id,actor_ref,evidence_sha) VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING",tenant,kind,id,actor.reference(),evidence);return null;
        });
    }
    public int purge(VerifiedIdentity actor,UUID tenant){return tx.execute(actor,tenant,"learning:maintain",db->write(db,"DELETE FROM dzoka.learning_cases WHERE dataset_id IN(SELECT id FROM dzoka.learning_datasets WHERE expires_at<=clock_timestamp() AND NOT dzoka.learning_dataset_hold(id) ORDER BY expires_at LIMIT 10)"));}
}
