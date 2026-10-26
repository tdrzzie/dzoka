package org.dzoka.fingerprints.adapters;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.fingerprints.domain.Perception.*;
import org.dzoka.fingerprints.domain.PerceptionEvidence;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.foundation.application.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.media.ports.PrivateObjects;
import tools.jackson.databind.ObjectMapper;

@org.springframework.stereotype.Component
public final class JdbcPerceptionJobs implements PerceptionJobs {
    private final TenantTransactions tx;private final PrivateObjects objects;private final ObjectMapper json=new ObjectMapper();
    public JdbcPerceptionJobs(TenantTransactions tx,PrivateObjects objects){this.tx=tx;this.objects=objects;}
    private static final String JOB="SELECT j.*,p.task,p.profile_key FROM dzoka.perception_jobs j JOIN dzoka.perception_profiles p ON p.tenant_id=j.tenant_id AND p.id=j.profile_id ";
    private static void lock(Connection c,UUID tenant)throws SQLException{try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")){s.setString(1,"ai:"+tenant);s.execute();}}
    private static PreparedStatement q(Connection c,String sql,UUID tenant,UUID id)throws SQLException{var s=c.prepareStatement(sql);s.setObject(1,tenant);s.setObject(2,id);return s;}
    private record Profile(String task,String key,Map<String,Object> manifest){}
    @SuppressWarnings("unchecked")
    private static Profile profile(Connection c,UUID tenant,UUID id)throws SQLException{
        try(var s=q(c,"SELECT task,profile_key,manifest FROM dzoka.perception_profiles WHERE tenant_id=? AND id=? AND enabled FOR SHARE",tenant,id);var r=s.executeQuery()){
            if(!r.next())throw new Failures.Unavailable(new IllegalStateException("Perception profile unavailable"));
            var codec=new ObjectMapper();Map<String,Object> manifest=codec.readValue(r.getString(3),Map.class);
            String hash=ContentDigest.sha256(codec.writeValueAsString(sorted(manifest)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(!hash.equals(r.getString(2))||!r.getString(1).equals(manifest.get("task"))||!"perception-profile-v1".equals(manifest.get("schema")))
                throw new Failures.Unavailable(new IllegalStateException("Perception profile identity mismatch"));
            return new Profile(r.getString(1),r.getString(2),manifest);
        }
    }
    static String verifiedProfileKey(Connection c,UUID tenant,UUID id)throws SQLException{return profile(c,tenant,id).key();}
    private static int report(Connection c,UUID tenant,UUID id,boolean write)throws SQLException{
        try(var s=q(c,"SELECT current_revision,owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:write:any') FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL AND expires_at>clock_timestamp()"+(write?" FOR UPDATE":""),tenant,id);var r=s.executeQuery()){
            if(!r.next())throw new Failures.NotFound();if(write&&!r.getBoolean(2))throw new Failures.Forbidden();return r.getInt(1);
        }
    }
    private static void journal(Connection c,VerifiedIdentity actor,UUID tenant,UUID report,String type)throws SQLException{
        try(var s=q(c,"UPDATE dzoka.reports SET version=version+1 WHERE tenant_id=? AND id=? RETURNING version",tenant,report);var r=s.executeQuery()){
            r.next();MutationJournal.append(c,tenant,actor,type,report,r.getLong(1),"report_id");
        }
    }
    private static Job job(ResultSet r)throws SQLException{return new Job(r.getObject("id",UUID.class),r.getObject("report_id",UUID.class),r.getInt("source_revision"),r.getObject("profile_id",UUID.class),r.getString("task"),r.getString("state"),r.getInt("attempts"),r.getString("error_code"),r.getTimestamp("deadline").toInstant());}
    private static void release(Connection c,UUID tenant,UUID id)throws SQLException{
        try(var s=q(c,"UPDATE dzoka.perception_jobs SET reservation_released=true WHERE tenant_id=? AND id=? AND NOT reservation_released",tenant,id)){
            if(s.executeUpdate()==1)try(var u=c.prepareStatement("UPDATE dzoka.ai_usage SET pending=pending-1 WHERE tenant_id=?")){u.setObject(1,tenant);u.executeUpdate();}
        }
    }
    @Override public Job enqueue(VerifiedIdentity actor,UUID tenant,UUID report,UUID profileId,UUID media,byte[] audio){
        if(profileId==null||(media==null)==(audio==null))throw new IllegalArgumentException("Exactly one input required");
        final int samples=audio==null?0:PerceptionEvidence.wav(audio);
        return tx.execute(actor,tenant,"reports:write",c->{
            lock(c,tenant);int revision=report(c,tenant,report,true);var profile=profile(c,tenant,profileId);
            if(profile.task().equals("SPEECH")!=(audio!=null))throw new IllegalArgumentException("Wrong input kind");
            String sha;int length,width=0,height=0;
            if(media!=null){
                try(var s=q(c,"SELECT derivative_sha256,derivative_bytes,width,height FROM dzoka.media_assets WHERE tenant_id=? AND id=? AND report_id=? AND state='SANITIZED' AND NOT purge_pending AND expires_at>clock_timestamp()",tenant,media)){
                    s.setObject(3,report);try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();sha=r.getString(1);length=r.getInt(2);width=r.getInt(3);height=r.getInt(4);}
                }
            }else{sha=ContentDigest.sha256(audio);length=audio.length;}
            try(var s=c.prepareStatement(JOB+"WHERE j.tenant_id=? AND j.report_id=? AND j.source_revision=? AND j.profile_id=? AND j.media_id IS NOT DISTINCT FROM CAST(? AS uuid) AND j.input_sha256=?")){
                s.setObject(1,tenant);s.setObject(2,report);s.setInt(3,revision);s.setObject(4,profileId);s.setObject(5,media);s.setString(6,sha);try(var r=s.executeQuery()){if(r.next())return job(r);}
            }
            try(var s=q(c,"SELECT count(*) FROM dzoka.perception_jobs WHERE tenant_id=? AND report_id=? AND state<>'CANCELLED'",tenant,report);var r=s.executeQuery()){
                r.next();if(r.getInt(1)>=64)throw new Failures.Conflict();
            }
            if(audio!=null){
                try(var s=c.prepareStatement("INSERT INTO dzoka.perception_audio_usage(tenant_id) VALUES(?) ON CONFLICT DO NOTHING")){s.setObject(1,tenant);s.executeUpdate();}
                try(var s=c.prepareStatement("UPDATE dzoka.perception_audio_usage SET raw_bytes=raw_bytes+? WHERE tenant_id=? AND raw_bytes+?<=33554432")){
                    s.setInt(1,length);s.setObject(2,tenant);s.setInt(3,length);if(s.executeUpdate()!=1)throw new Failures.Conflict();
                }
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.ai_usage(tenant_id) VALUES(?) ON CONFLICT DO NOTHING")){s.setObject(1,tenant);s.executeUpdate();}
            try(var s=c.prepareStatement("UPDATE dzoka.ai_usage SET pending=pending+1 WHERE tenant_id=? AND pending<32")){s.setObject(1,tenant);if(s.executeUpdate()!=1)throw new Failures.Conflict();}
            UUID id=UUID.randomUUID();
            try(var s=c.prepareStatement("INSERT INTO dzoka.perception_jobs(tenant_id,id,report_id,source_revision,profile_id,media_id,input_sha256,input_bytes,input_type,width,height,samples,state) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'QUEUED')")){
                s.setObject(1,tenant);s.setObject(2,id);s.setObject(3,report);s.setInt(4,revision);s.setObject(5,profileId);s.setObject(6,media);s.setString(7,sha);s.setInt(8,length);s.setString(9,audio==null?"IMAGE_PNG":"AUDIO_WAV");
                s.setObject(10,audio==null?width:null);s.setObject(11,audio==null?height:null);s.setObject(12,audio==null?null:samples);s.executeUpdate();
            }
            if(audio!=null)try(var s=c.prepareStatement("INSERT INTO dzoka.perception_audio(tenant_id,job_id,raw_wav) VALUES(?,?,?)")){s.setObject(1,tenant);s.setObject(2,id);s.setBytes(3,audio);s.executeUpdate();}
            journal(c,actor,tenant,report,"perception.requested");
            try(var s=q(c,JOB+"WHERE j.tenant_id=? AND j.id=?",tenant,id);var r=s.executeQuery()){r.next();return job(r);}
        });
    }
    @Override public Job read(VerifiedIdentity actor,UUID tenant,UUID id){return tx.execute(actor,tenant,"reports:read",c->{
        try(var s=q(c,JOB+"WHERE j.tenant_id=? AND j.id=?",tenant,id);var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();var j=job(r);report(c,tenant,j.reportId(),false);return j;}
    });}
    private static void sweep(Connection c,UUID tenant)throws SQLException{
        try(var s=c.prepareStatement("""
            UPDATE dzoka.perception_jobs j SET state=CASE WHEN deadline<=clock_timestamp() OR attempts>=3 THEN 'FAILED' ELSE 'OBSOLETE' END,
             error_code='LEASE_OR_INPUT_REVOKED',lease_token=NULL,lease_owner=NULL,lease_until=NULL
            WHERE tenant_id=? AND state IN ('QUEUED','PROCESSING') AND (deadline<=clock_timestamp() OR (attempts>=3 AND lease_until<clock_timestamp())
             OR NOT EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=j.tenant_id AND r.id=j.report_id AND r.current_revision=j.source_revision AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp())) RETURNING id
            """)){s.setObject(1,tenant);try(var r=s.executeQuery()){while(r.next())release(c,tenant,r.getObject(1,UUID.class));}}
    }
    @Override public Optional<Lease> claim(VerifiedIdentity actor,UUID tenant,UUID profileId){return tx.execute(actor,tenant,"ai:work",c->{
        lock(c,tenant);var profile=profile(c,tenant,profileId);sweep(c,tenant);
        try(var s=q(c,"SELECT * FROM dzoka.perception_jobs WHERE tenant_id=? AND profile_id=? AND deadline>clock_timestamp() AND attempts<3 AND (state='QUEUED' OR (state='PROCESSING' AND lease_until<clock_timestamp())) ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED",tenant,profileId);var r=s.executeQuery()){
            if(!r.next())return Optional.empty();UUID id=r.getObject("id",UUID.class),lease=UUID.randomUUID();Instant until;
            try(var u=c.prepareStatement("UPDATE dzoka.perception_jobs SET state='PROCESSING',attempts=attempts+1,lease_token=?,lease_owner=?,lease_until=clock_timestamp()+interval '120 seconds',error_code=NULL WHERE tenant_id=? AND id=? RETURNING lease_until")){
                u.setObject(1,lease);u.setString(2,actor.reference());u.setObject(3,tenant);u.setObject(4,id);try(var x=u.executeQuery()){x.next();until=x.getTimestamp(1).toInstant();}
            }
            return Optional.of(new Lease(id,lease,profileId,profile.task(),profile.key(),r.getString("input_type"),r.getString("input_sha256"),r.getInt("input_bytes"),until));
        }
    });}
    private record Input(String sha,int length,String key,byte[] bytes){}
    private static boolean mediaValid(Connection c,UUID tenant,UUID media,String sha)throws SQLException{
        if(media==null)return true;
        try(var s=q(c,"SELECT 1 FROM dzoka.media_assets WHERE tenant_id=? AND id=? AND state='SANITIZED' AND NOT purge_pending AND derivative_sha256=? AND expires_at>clock_timestamp()",tenant,media)){
            s.setString(3,sha);try(var r=s.executeQuery()){return r.next();}
        }
    }
    private static Input inputRef(Connection c,UUID tenant,UUID id,UUID lease)throws SQLException{
        try(var s=q(c,"""
            SELECT j.* FROM dzoka.perception_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
            JOIN dzoka.perception_profiles p ON p.tenant_id=j.tenant_id AND p.id=j.profile_id
            WHERE j.tenant_id=? AND j.id=? AND j.state='PROCESSING' AND j.lease_token=? AND j.lease_owner=dzoka.current_actor_ref()
             AND j.lease_until>clock_timestamp() AND j.deadline>clock_timestamp() AND r.current_revision=j.source_revision
             AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() AND p.enabled
            """,tenant,id)){
            s.setObject(3,lease);try(var r=s.executeQuery()){
                if(!r.next())throw new Failures.NotFound();String sha=r.getString("input_sha256");int size=r.getInt("input_bytes");UUID media=r.getObject("media_id",UUID.class);
                if(!mediaValid(c,tenant,media,sha))throw new Failures.NotFound();
                if(media!=null)try(var m=q(c,"SELECT derivative_key FROM dzoka.media_assets WHERE tenant_id=? AND id=?",tenant,media);var row=m.executeQuery()){
                    if(!row.next())throw new Failures.NotFound();return new Input(sha,size,row.getString(1),null);
                }
                try(var a=q(c,"SELECT raw_wav FROM dzoka.perception_audio WHERE tenant_id=? AND job_id=?",tenant,id);var row=a.executeQuery()){
                    if(!row.next())throw new Failures.NotFound();return new Input(sha,size,null,row.getBytes(1));
                }
            }
        }
    }
    @Override public byte[] input(VerifiedIdentity actor,UUID tenant,UUID id,UUID lease){
        var ref=tx.execute(actor,tenant,"ai:work",c->inputRef(c,tenant,id,lease));
        byte[] bytes=ref.key()==null?ref.bytes():objects.get(ref.key(),ref.sha(),ref.length(),20*1024*1024);
        if(bytes.length!=ref.length()||!ContentDigest.sha256(bytes).equals(ref.sha()))throw new Failures.Unavailable(new IllegalStateException("Perception input integrity"));
        tx.execute(actor,tenant,"ai:work",c->{inputRef(c,tenant,id,lease);return null;});return bytes;
    }
    @Override public boolean complete(VerifiedIdentity actor,UUID tenant,UUID id,Result result){return tx.execute(actor,tenant,"ai:work",c->{
        lock(c,tenant);
        try(var s=q(c,JOB+"JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id WHERE j.tenant_id=? AND j.id=? AND r.current_revision=j.source_revision AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() FOR UPDATE OF j,r",tenant,id);var r=s.executeQuery()){
            if(!r.next())return false;var j=job(r);String evidence=json.writeValueAsString(sorted(result.evidence()));
            String digest=ContentDigest.sha256((result.generatedAt()+"\n"+result.inputSha256()+"\n"+result.profileKey()+"\n"+evidence).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(!result.lease().equals(r.getObject("lease_token",UUID.class))||!actor.reference().equals(r.getString("lease_owner")))return false;
            if(j.state().equals("SUCCEEDED"))return digest.equals(r.getString("completion_sha256"));
            if(!j.state().equals("PROCESSING")||!r.getTimestamp("lease_until").toInstant().isAfter(Instant.now())||!j.deadline().isAfter(Instant.now()))return false;
            var profile=profile(c,tenant,j.profileId());
            if(!result.profileKey().equals(profile.key())||!result.inputSha256().equals(r.getString("input_sha256"))
                    ||result.generatedAt().isAfter(Instant.now().plusSeconds(30))||result.generatedAt().isBefore(r.getTimestamp("created_at").toInstant().minusSeconds(5)))throw new IllegalArgumentException("Perception provenance mismatch");
            if(!mediaValid(c,tenant,r.getObject("media_id",UUID.class),result.inputSha256()))return false;
            PerceptionEvidence.validate(profile.task(),result.evidence(),r.getInt("width"),r.getInt("height"),r.getInt("samples"));
            if(evidence.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>60000)throw new IllegalArgumentException("Output exceeds bound");
            try(var insert=c.prepareStatement("INSERT INTO dzoka.perception_components(tenant_id,id,job_id,report_id,source_revision,profile_id,input_sha256,evidence,generated_at) VALUES(?,?,?,?,?,?,?,CAST(? AS jsonb),?)")){
                insert.setObject(1,tenant);insert.setObject(2,UUID.randomUUID());insert.setObject(3,id);insert.setObject(4,j.reportId());insert.setInt(5,j.sourceRevision());insert.setObject(6,j.profileId());insert.setString(7,result.inputSha256());insert.setString(8,evidence);insert.setTimestamp(9,Timestamp.from(result.generatedAt()));insert.executeUpdate();
            }
            try(var update=c.prepareStatement("UPDATE dzoka.perception_jobs SET state='SUCCEEDED',completion_sha256=?,lease_until=NULL WHERE tenant_id=? AND id=?")){
                update.setString(1,digest);update.setObject(2,tenant);update.setObject(3,id);update.executeUpdate();
            }
            release(c,tenant,id);journal(c,actor,tenant,j.reportId(),"perception.completed");return true;
        }
    });}
    private static Object sorted(Object value){
        if(value instanceof Map<?,?> map){var result=new TreeMap<String,Object>();map.forEach((k,v)->result.put((String)k,sorted(v)));return result;}
        if(value instanceof List<?> list)return list.stream().map(JdbcPerceptionJobs::sorted).toList();return value;
    }
    @Override public boolean fail(VerifiedIdentity actor,UUID tenant,UUID id,UUID lease,String reason){
        if(lease==null||reason==null||!Set.of("PROVIDER_UNAVAILABLE","ARTIFACT_MISMATCH","INVALID_OUTPUT").contains(reason))throw new IllegalArgumentException("Invalid failure");
        return tx.execute(actor,tenant,"ai:work",c->{lock(c,tenant);
            try(var s=c.prepareStatement("UPDATE dzoka.perception_jobs SET state=CASE WHEN attempts>=3 THEN 'FAILED' ELSE 'QUEUED' END,error_code=?,lease_token=NULL,lease_owner=NULL,lease_until=NULL WHERE tenant_id=? AND id=? AND state='PROCESSING' AND lease_token=? AND lease_owner=dzoka.current_actor_ref() AND lease_until>clock_timestamp() RETURNING state")){
                s.setString(1,reason);s.setObject(2,tenant);s.setObject(3,id);s.setObject(4,lease);try(var r=s.executeQuery()){if(!r.next())return false;if(r.getString(1).equals("FAILED"))release(c,tenant,id);return true;}
            }
        });
    }
    @SuppressWarnings("unchecked")
    @Override public List<Component> current(VerifiedIdentity actor,UUID tenant,UUID report){return tx.execute(actor,tenant,"reports:read",c->{
        report(c,tenant,report,false);var found=new ArrayList<Component>();
        try(var s=q(c,"""
            SELECT f.*,p.profile_key,p.task,p.manifest,
             CASE WHEN EXISTS(SELECT 1 FROM dzoka.fingerprint_active a JOIN dzoka.fingerprint_perception_slots s ON s.tenant_id=a.tenant_id AND s.rollout_id=a.rollout_id
              WHERE a.tenant_id=f.tenant_id AND s.component_id=f.id AND s.report_id=f.report_id) THEN 'ACTIVE' ELSE 'SHADOW' END selection
            FROM dzoka.perception_components f
            JOIN dzoka.perception_profiles p ON p.tenant_id=f.tenant_id AND p.id=f.profile_id
            JOIN dzoka.reports r ON r.tenant_id=f.tenant_id AND r.id=f.report_id
            JOIN dzoka.perception_jobs j ON j.tenant_id=f.tenant_id AND j.id=f.job_id
            WHERE f.tenant_id=? AND f.report_id=? AND f.source_revision=r.current_revision AND p.enabled AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()
             AND (j.media_id IS NULL OR EXISTS(SELECT 1 FROM dzoka.media_assets m WHERE m.tenant_id=j.tenant_id AND m.id=j.media_id
               AND m.state='SANITIZED' AND NOT m.purge_pending AND m.expires_at>clock_timestamp() AND m.derivative_sha256=f.input_sha256))
            ORDER BY f.received_at,f.id LIMIT 64
            """,tenant,report);var r=s.executeQuery()){
            while(r.next()){
                var checked=profile(c,tenant,r.getObject("profile_id",UUID.class));
                if(!checked.key().equals(r.getString("profile_key")))throw new Failures.Conflict();
                found.add(new Component(r.getObject("id",UUID.class),report,r.getInt("source_revision"),r.getString("input_sha256"),r.getObject("profile_id",UUID.class),checked.key(),r.getString("task"),
                    r.getTimestamp("generated_at").toInstant(),r.getTimestamp("received_at").toInstant(),checked.manifest(),json.readValue(r.getString("evidence"),Map.class),r.getString("selection")));
            }
        }return List.copyOf(found);
    });}
    @Override public int purge(VerifiedIdentity actor,UUID tenant){return tx.execute(actor,tenant,"reports:purge",c->{
        lock(c,tenant);sweep(c,tenant);int erased=0;
        try(var s=c.prepareStatement("SELECT id FROM dzoka.reports r WHERE tenant_id=? AND NOT legal_hold AND (deleted_at IS NOT NULL OR expires_at<=clock_timestamp()) AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j WHERE j.tenant_id=r.tenant_id AND j.report_id=r.id AND j.state<>'CANCELLED') ORDER BY id LIMIT 100 FOR UPDATE")){
            s.setObject(1,tenant);try(var r=s.executeQuery()){while(r.next()){
                UUID report=r.getObject(1,UUID.class);
                try(var d=q(c,"DELETE FROM dzoka.perception_components WHERE tenant_id=? AND report_id=?",tenant,report)){erased+=d.executeUpdate();}
                long bytes=0;
                try(var d=q(c,"DELETE FROM dzoka.perception_audio a USING dzoka.perception_jobs j WHERE a.tenant_id=? AND j.report_id=? AND j.tenant_id=a.tenant_id AND j.id=a.job_id RETURNING octet_length(a.raw_wav)",tenant,report);var rows=d.executeQuery()){while(rows.next())bytes+=rows.getLong(1);}
                if(bytes>0)try(var d=c.prepareStatement("UPDATE dzoka.perception_audio_usage SET raw_bytes=raw_bytes-? WHERE tenant_id=?")){d.setLong(1,bytes);d.setObject(2,tenant);d.executeUpdate();}
                try(var u=q(c,"UPDATE dzoka.perception_jobs SET state='CANCELLED',error_code='INPUT_REVOKED',lease_token=NULL,lease_owner=NULL,lease_until=NULL,completion_sha256=NULL WHERE tenant_id=? AND report_id=? AND state<>'CANCELLED' RETURNING id",tenant,report);var jobs=u.executeQuery()){
                    while(jobs.next())release(c,tenant,jobs.getObject(1,UUID.class));
                }
            }}
        }return erased;
    });}
}
