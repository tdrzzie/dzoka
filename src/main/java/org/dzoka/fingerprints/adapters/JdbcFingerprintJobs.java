package org.dzoka.fingerprints.adapters;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.fingerprints.domain.*;
import org.dzoka.fingerprints.ports.FingerprintJobs;
import org.dzoka.foundation.application.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Component;

@Component
public final class JdbcFingerprintJobs implements FingerprintJobs {
    private final TenantTransactions tx;
    private final org.dzoka.fingerprints.ports.PerceptionJobs perception;
    public JdbcFingerprintJobs(TenantTransactions tx){this(tx,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public JdbcFingerprintJobs(TenantTransactions tx,org.dzoka.fingerprints.ports.PerceptionJobs perception){this.tx=tx;this.perception=perception;}
    private static void tenantLock(Connection c,UUID tenant) throws SQLException {
        try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")){s.setString(1,"ai:"+tenant);s.execute();}
    }
    private static void report(Connection c,UUID tenant,UUID id,boolean write) throws SQLException {
        try(var s=c.prepareStatement("SELECT owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:write:any') FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL AND expires_at>clock_timestamp() FOR UPDATE")){
            s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();if(write&&!r.getBoolean(1))throw new Failures.Forbidden();}
        }
    }
    private static void journal(Connection c,VerifiedIdentity actor,UUID tenant,UUID report,String type) throws SQLException {
        try(var s=c.prepareStatement("UPDATE dzoka.reports SET version=version+1 WHERE tenant_id=? AND id=? RETURNING version")){
            s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){r.next();MutationJournal.append(c,tenant,actor,type,report,r.getLong(1),"report_id");}
        }
    }
    private static EmbeddingSpace space(Connection c,UUID tenant,UUID id,boolean enabled) throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM dzoka.embedding_spaces WHERE tenant_id=? AND id=?"+(enabled?" AND enabled":""))){
            s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){
                if(!r.next())throw new Failures.Unavailable(new IllegalStateException("Reviewed embedding space unavailable"));
                return new EmbeddingSpace(id,r.getString("provider"),r.getString("model"),r.getString("artifact_revision"),r.getString("artifact_sha256"),
                    r.getString("tokenizer_sha256"),r.getString("preprocessing"),r.getInt("dimensions"),r.getString("metric"),r.getString("normalization"),r.getString("config_sha256"),r.getString("space_key"));
            }
        }
    }
    private static InferenceJob job(ResultSet r) throws SQLException {
        return new InferenceJob(r.getObject("id",UUID.class),r.getObject("report_id",UUID.class),r.getInt("source_revision"),r.getObject("space_id",UUID.class),
            r.getString("state"),r.getInt("attempts"),r.getString("error_code"),r.getTimestamp("deadline").toInstant());
    }
    private static void release(Connection c,UUID tenant,UUID id) throws SQLException {
        try(var s=c.prepareStatement("UPDATE dzoka.ai_jobs SET reservation_released=true WHERE tenant_id=? AND id=? AND NOT reservation_released")){
            s.setObject(1,tenant);s.setObject(2,id);if(s.executeUpdate()==1)
                try(var u=c.prepareStatement("UPDATE dzoka.ai_usage SET pending=pending-1 WHERE tenant_id=?")){u.setObject(1,tenant);u.executeUpdate();}
        }
    }
    @Override public InferenceJob enqueue(VerifiedIdentity actor,UUID tenant,UUID report,UUID spaceId){
        return tx.execute(actor,tenant,"reports:write",c->{
            tenantLock(c,tenant);report(c,tenant,report,true);space(c,tenant,spaceId,true);
            int revision;UUID item;String text;
            try(var s=c.prepareStatement("SELECT r.current_revision,r.item_id,v.normalized_text FROM dzoka.reports r JOIN dzoka.report_revisions v ON v.tenant_id=r.tenant_id AND v.report_id=r.id AND v.revision=r.current_revision WHERE r.tenant_id=? AND r.id=?")){
                s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){r.next();revision=r.getInt(1);item=r.getObject(2,UUID.class);text=r.getString(3);}
            }
            try(var s=c.prepareStatement("SELECT * FROM dzoka.ai_jobs WHERE tenant_id=? AND report_id=? AND source_revision=? AND space_id=?")){
                s.setObject(1,tenant);s.setObject(2,report);s.setInt(3,revision);s.setObject(4,spaceId);try(var r=s.executeQuery()){if(r.next())return job(r);}
            }
            boolean missing=text==null||text.isBlank();String digest=ContentDigest.sha256((text==null?"":text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(!missing){
                try(var s=c.prepareStatement("INSERT INTO dzoka.ai_usage(tenant_id) VALUES(?) ON CONFLICT DO NOTHING")){s.setObject(1,tenant);s.executeUpdate();}
                try(var s=c.prepareStatement("UPDATE dzoka.ai_usage SET pending=pending+1 WHERE tenant_id=? AND pending<32")){s.setObject(1,tenant);if(s.executeUpdate()!=1)throw new Failures.Conflict();}
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.ai_jobs(tenant_id,id,report_id,item_id,source_revision,input_sha256,space_id,state,error_code,reservation_released) VALUES(?,?,?,?,?,?,?,?,?,?) RETURNING *")){
                s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setObject(3,report);s.setObject(4,item);s.setInt(5,revision);s.setString(6,digest);s.setObject(7,spaceId);
                s.setString(8,missing?"UNAVAILABLE":"QUEUED");s.setString(9,missing?"INPUT_UNAVAILABLE":null);
                s.setBoolean(10,missing);
                try(var r=s.executeQuery()){r.next();var result=job(r);journal(c,actor,tenant,report,"fingerprint.requested");return result;}
            }
        });
    }
    @Override public InferenceJob read(VerifiedIdentity actor,UUID tenant,UUID id){
        return tx.execute(actor,tenant,"reports:read",c->{
            try(var s=c.prepareStatement("SELECT * FROM dzoka.ai_jobs WHERE tenant_id=? AND id=?")){
                s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();var j=job(r);report(c,tenant,j.reportId(),false);return j;}
            }
        });
    }
    @Override public Optional<LeasedInput> claim(VerifiedIdentity actor,UUID tenant,UUID spaceId){
        return tx.execute(actor,tenant,"ai:work",c->{
            var profile=space(c,tenant,spaceId,true);tenantLock(c,tenant);
            try(var s=c.prepareStatement("""
                UPDATE dzoka.ai_jobs j SET state='OBSOLETE',error_code='INPUT_REVOKED',lease_token=NULL,lease_owner=NULL,lease_until=NULL
                WHERE tenant_id=? AND state IN ('QUEUED','PROCESSING') AND NOT EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=j.tenant_id AND r.id=j.report_id
                  AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() AND r.current_revision=j.source_revision) RETURNING id
                """)){s.setObject(1,tenant);try(var rows=s.executeQuery()){while(rows.next())release(c,tenant,rows.getObject(1,UUID.class));}}
            try(var s=c.prepareStatement("""
                UPDATE dzoka.ai_jobs SET state='FAILED',error_code='BUDGET_EXHAUSTED',lease_token=NULL,lease_owner=NULL,lease_until=NULL
                WHERE tenant_id=? AND state IN ('QUEUED','PROCESSING') AND (deadline<=clock_timestamp() OR (attempts>=3 AND lease_until<clock_timestamp())) RETURNING id
                """)){s.setObject(1,tenant);try(var rows=s.executeQuery()){while(rows.next())release(c,tenant,rows.getObject(1,UUID.class));}}
            try(var s=c.prepareStatement("""
                SELECT * FROM dzoka.ai_jobs WHERE tenant_id=? AND space_id=? AND attempts<3 AND deadline>clock_timestamp()
                AND (state='QUEUED' OR (state='PROCESSING' AND lease_until<clock_timestamp())) ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED
                """)){
                s.setObject(1,tenant);s.setObject(2,spaceId);try(var r=s.executeQuery()){
                    if(!r.next())return Optional.empty();var j=job(r);UUID lease=UUID.randomUUID();Instant until;
                    try(var u=c.prepareStatement("UPDATE dzoka.ai_jobs SET state='PROCESSING',attempts=attempts+1,lease_token=?,lease_owner=?,lease_until=clock_timestamp()+interval '120 seconds',error_code=NULL WHERE tenant_id=? AND id=? RETURNING lease_until")){
                        u.setObject(1,lease);u.setString(2,actor.reference());u.setObject(3,tenant);u.setObject(4,j.id());try(var x=u.executeQuery()){x.next();until=x.getTimestamp(1).toInstant();}
                    }
                    try(var input=c.prepareStatement("SELECT normalized_text FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=?")){
                        input.setObject(1,tenant);input.setObject(2,j.reportId());input.setInt(3,j.sourceRevision());try(var text=input.executeQuery()){
                            if(!text.next())throw new Failures.Conflict();
                            return Optional.of(new LeasedInput(j.id(),lease,until,j.reportId(),j.sourceRevision(),r.getString("input_sha256"),text.getString(1),profile));
                        }
                    }
                }
            }
        });
    }
    @Override public boolean complete(VerifiedIdentity actor,UUID tenant,UUID id,EmbeddingResult result){
        return tx.execute(actor,tenant,"ai:work",c->{
            tenantLock(c,tenant);
            try(var s=c.prepareStatement("""
                SELECT j.*,r.current_revision,r.deleted_at,r.expires_at,r.legal_hold FROM dzoka.ai_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
                WHERE j.tenant_id=? AND j.id=? FOR UPDATE OF r,j
                """)){
                s.setObject(1,tenant);s.setObject(2,id);try(var r=s.executeQuery()){
                    if(!r.next())return false;var j=job(r);Instant now=Instant.now();
                    if(r.getTimestamp("deleted_at")!=null || !r.getTimestamp("expires_at").toInstant().isAfter(now) || r.getInt("current_revision")!=j.sourceRevision())return false;
                    String digest=ContentDigest.sha256(result.canonical().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    if(!result.lease().equals(r.getObject("lease_token",UUID.class)) || !actor.reference().equals(r.getString("lease_owner")))return false;
                    if(j.state().equals("SUCCEEDED"))return digest.equals(r.getString("completion_sha256"));
                    if(!j.state().equals("PROCESSING") || !r.getTimestamp("lease_until").toInstant().isAfter(now) || !j.deadline().isAfter(now))return false;
                    var profile=space(c,tenant,j.spaceId(),true);
                    if(!result.inputSha256().equals(r.getString("input_sha256")) || !result.spaceKey().equals(profile.spaceKey()) || !result.provider().equals(profile.provider())
                            || result.vector().length!=profile.dimensions() || result.generatedAt().isAfter(now.plusSeconds(30))
                            || result.generatedAt().isBefore(r.getTimestamp("created_at").toInstant().minusSeconds(5)))throw new IllegalArgumentException("Result provenance mismatch");
                    try(var insert=c.prepareStatement("INSERT INTO dzoka.fingerprint_components(tenant_id,id,job_id,report_id,item_id,source_revision,input_sha256,space_id,output_schema,generated_at,vector,token_count,truncated) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                        insert.setObject(1,tenant);insert.setObject(2,UUID.randomUUID());insert.setObject(3,id);insert.setObject(4,j.reportId());insert.setObject(5,r.getObject("item_id",UUID.class));
                        insert.setInt(6,j.sourceRevision());insert.setString(7,result.inputSha256());insert.setObject(8,j.spaceId());insert.setString(9,result.outputSchema());insert.setTimestamp(10,Timestamp.from(result.generatedAt()));
                        insert.setArray(11,c.createArrayOf("float8",Arrays.stream(result.vector()).boxed().toArray(Double[]::new)));insert.setInt(12,result.tokenCount());insert.setBoolean(13,result.truncated());insert.executeUpdate();
                    }
                    try(var u=c.prepareStatement("UPDATE dzoka.ai_jobs SET state='SUCCEEDED',completion_sha256=?,lease_until=NULL WHERE tenant_id=? AND id=?")){
                        u.setString(1,digest);u.setObject(2,tenant);u.setObject(3,id);u.executeUpdate();
                    }
                    release(c,tenant,id);journal(c,actor,tenant,j.reportId(),"fingerprint.completed");return true;
                }
            }
        });
    }
    @Override public boolean fail(VerifiedIdentity actor,UUID tenant,UUID id,UUID lease,String reason){
        if(lease==null || reason==null || !Set.of("PROVIDER_UNAVAILABLE","ARTIFACT_MISMATCH","INVALID_OUTPUT").contains(reason))throw new IllegalArgumentException("Invalid failure envelope");
        return tx.execute(actor,tenant,"ai:work",c->{
            tenantLock(c,tenant);
            try(var s=c.prepareStatement("""
                UPDATE dzoka.ai_jobs SET state=CASE WHEN attempts>=3 THEN 'FAILED' ELSE 'QUEUED' END,error_code=?,lease_token=NULL,lease_owner=NULL,lease_until=NULL
                WHERE tenant_id=? AND id=? AND state='PROCESSING' AND lease_token=? AND lease_owner=? AND lease_until>clock_timestamp() RETURNING state
                """)){
                s.setString(1,reason);s.setObject(2,tenant);s.setObject(3,id);s.setObject(4,lease);s.setString(5,actor.reference());
                try(var r=s.executeQuery()){if(!r.next())return false;if(r.getString(1).equals("FAILED"))release(c,tenant,id);return true;}
            }
        });
    }
    @Override public List<FingerprintComponent> current(VerifiedIdentity actor,UUID tenant,UUID report){
        return tx.execute(actor,tenant,"reports:read",c->{
            tenantLock(c,tenant);report(c,tenant,report,false);return selected(c,tenant,report);
        });
    }
    private static List<FingerprintComponent> selected(Connection c,UUID tenant,UUID report)throws SQLException{
            var found=new ArrayList<FingerprintComponent>();
            try(var s=c.prepareStatement("""
                SELECT f.* FROM dzoka.fingerprint_components f
                JOIN dzoka.reports r ON r.tenant_id=f.tenant_id AND r.id=f.report_id
                JOIN dzoka.embedding_spaces e ON e.tenant_id=f.tenant_id AND e.id=f.space_id
                WHERE f.tenant_id=? AND f.report_id=? AND f.source_revision=r.current_revision AND e.enabled
                AND (NOT EXISTS(SELECT 1 FROM dzoka.fingerprint_active a WHERE a.tenant_id=f.tenant_id)
                OR EXISTS(SELECT 1 FROM dzoka.fingerprint_active a JOIN dzoka.fingerprint_snapshot_slots s
                 ON s.tenant_id=a.tenant_id AND s.rollout_id=a.rollout_id
                 WHERE a.tenant_id=f.tenant_id AND s.report_id=f.report_id AND s.component_id=f.id AND s.state='AVAILABLE'))
                ORDER BY f.received_at,f.id LIMIT 32
                """)){
                s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){while(r.next()){
                    Object[] values=(Object[])r.getArray("vector").getArray();double[] vector=Arrays.stream(values).mapToDouble(v->((Number)v).doubleValue()).toArray();
                    found.add(new FingerprintComponent(r.getObject("id",UUID.class),r.getObject("item_id",UUID.class),report,r.getInt("source_revision"),r.getString("input_sha256"),
                        space(c,tenant,r.getObject("space_id",UUID.class),false),r.getString("output_schema"),r.getTimestamp("generated_at").toInstant(),r.getTimestamp("received_at").toInstant(),vector,r.getInt("token_count"),r.getBoolean("truncated")));
                }}
            }return List.copyOf(found);
    }
    @Override public int purge(VerifiedIdentity actor,UUID tenant){
        return tx.execute(actor,tenant,"reports:purge",c->{
            tenantLock(c,tenant);
            int erased=0;
            try(var s=c.prepareStatement("SELECT r.id FROM dzoka.reports r WHERE r.tenant_id=? AND NOT r.legal_hold AND (r.deleted_at IS NOT NULL OR r.expires_at<=clock_timestamp()) AND (EXISTS(SELECT 1 FROM dzoka.fingerprint_components f WHERE f.tenant_id=r.tenant_id AND f.report_id=r.id) OR EXISTS(SELECT 1 FROM dzoka.ai_jobs j WHERE j.tenant_id=r.tenant_id AND j.report_id=r.id AND j.state<>'CANCELLED')) ORDER BY r.id LIMIT 100 FOR UPDATE SKIP LOCKED")){
                s.setObject(1,tenant);try(var r=s.executeQuery()){while(r.next()){
                    UUID report=r.getObject(1,UUID.class);
                    try(var d=c.prepareStatement("DELETE FROM dzoka.fingerprint_components WHERE tenant_id=? AND report_id=?")){d.setObject(1,tenant);d.setObject(2,report);erased+=d.executeUpdate();}
                    try(var u=c.prepareStatement("UPDATE dzoka.ai_jobs SET state='CANCELLED',error_code='INPUT_REVOKED',lease_token=NULL,lease_owner=NULL,lease_until=NULL,completion_sha256=NULL WHERE tenant_id=? AND report_id=? AND state<>'CANCELLED' RETURNING id")){
                        u.setObject(1,tenant);u.setObject(2,report);try(var rows=u.executeQuery()){while(rows.next())release(c,tenant,rows.getObject(1,UUID.class));}
                    }
                }}
            }return erased;
        });
    }
    @Override public ItemFingerprint snapshot(VerifiedIdentity actor,UUID tenant,UUID report){
        return tx.execute(actor,tenant,"reports:read",c->{
            tenantLock(c,tenant);report(c,tenant,report,false);
            // Hold the report lock across selection and metadata. Promotion also
            // locks every cohort report, preventing torn pointer/component views.
            var components=selected(c,tenant,report);UUID rollout=null;long activation=0;
            try(var s=c.prepareStatement("SELECT rollout_id,version FROM dzoka.fingerprint_active WHERE tenant_id=?")){
                s.setObject(1,tenant);try(var r=s.executeQuery()){if(r.next()){rollout=r.getObject(1,UUID.class);activation=r.getLong(2);}}
            }
            UUID item;int revision;
            try(var s=c.prepareStatement("SELECT item_id,current_revision FROM dzoka.reports WHERE tenant_id=? AND id=?")){
                s.setObject(1,tenant);s.setObject(2,report);try(var r=s.executeQuery()){r.next();item=r.getObject(1,UUID.class);revision=r.getInt(2);}
            }
            var matching=components.stream().filter(f->f.sourceRevision()==revision).toList();
            var text=matching.isEmpty()?ItemFingerprint.EvidenceState.UNAVAILABLE:ItemFingerprint.EvidenceState.AVAILABLE;
            if(matching.isEmpty()&&rollout==null)try(var s=c.prepareStatement("SELECT j.state FROM dzoka.ai_jobs j JOIN dzoka.embedding_spaces e ON e.tenant_id=j.tenant_id AND e.id=j.space_id WHERE j.tenant_id=? AND j.report_id=? AND j.source_revision=? AND e.enabled ORDER BY j.created_at DESC LIMIT 32")){
                s.setObject(1,tenant);s.setObject(2,report);s.setInt(3,revision);try(var r=s.executeQuery()){while(r.next()){
                    String state=r.getString(1);if(Set.of("QUEUED","PROCESSING").contains(state)){text=ItemFingerprint.EvidenceState.PENDING;break;}
                    if(state.equals("FAILED"))text=ItemFingerprint.EvidenceState.FAILED;
                }}
            }
            var unavailable=ItemFingerprint.EvidenceState.UNAVAILABLE;
            if(perception==null)throw new IllegalStateException("Snapshot perception boundary unavailable");
            var derived=perception.selected(actor,tenant,report);var coverage=new HashMap<String,ItemFingerprint.EvidenceState>();coverage.put("text",text);
            for(var entry:Map.of("image","VISION","localFeatures","LOCAL_FEATURES","ocr","OCR","speech","SPEECH","jointImage","JOINT_IMAGE","jointText","JOINT_TEXT","detection","DETECTION","segmentation","SEGMENTATION","attributes","ATTRIBUTES","translation","TRANSLATION").entrySet())
                coverage.put(entry.getKey(),derived.stream().anyMatch(f->f.task().equals(entry.getValue()))?ItemFingerprint.EvidenceState.AVAILABLE:unavailable);
            return new ItemFingerprint(1,tenant,item,report,revision,matching,coverage,rollout,activation,derived);
        });
    }
}
