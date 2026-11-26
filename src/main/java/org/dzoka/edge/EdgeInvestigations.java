package org.dzoka.edge;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public final class EdgeInvestigations {
    private final TenantTransactions tx;
    private final ObjectMapper json;
    public EdgeInvestigations(TenantTransactions tx,ObjectMapper json) { this.tx=tx;this.json=json; }
    public record Grant(UUID report,UUID camera,String investigator,Instant start,Instant end,Instant expires,
                        int budget,String evidenceSha) {}
    public record Lease(UUID job,UUID token,UUID camera,Instant start,Instant end,int budget,String archiveSha,String authorizationSha) {}
    public void camera(VerifiedIdentity actor,UUID tenant,UUID id,String archiveSha,boolean enabled) {
        if(!digest(archiveSha)) throw new IllegalArgumentException("Archive manifest digest required");
        tx.execute(actor,tenant,"edge:review",db -> {
            try(var query=q(db,"SELECT archive_sha FROM dzoka.edge_cameras WHERE id=?",id);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!archiveSha.equals(row.getString(1))) throw new Failures.Conflict();
                    write(db,"UPDATE dzoka.edge_cameras SET active=? WHERE id=?",enabled,id);
                    return null;
                }
            }
            write(db,"INSERT INTO dzoka.edge_cameras(tenant_id,id,archive_sha,active,registered_by) VALUES(?,?,?,?,?)",
                    tenant,id,archiveSha,enabled,actor.reference());
            return null;
        });
    }
    public record Observation(Instant after,Instant before,boolean gap,String sourceSha,String processorSha,String status) {
        public Observation {
            if(status==null) status=gap?"UNKNOWN":"PRESENT";
        }
        public Observation(Instant after,Instant before,boolean gap,String sourceSha,String processorSha) {
            this(after,before,gap,sourceSha,processorSha,gap?"UNKNOWN":"PRESENT");
        }
    }
    public record Completion(UUID token,List<Observation> observations) {}
    public record Result(String state,List<Observation> observations) {}
    public record Crop(UUID token,int width,int height,byte[] pixels) {
        @Override public String toString() { return "PrivateEdgeCrop"; }
    }
    public void evidence(VerifiedIdentity actor,UUID tenant,UUID job,UUID id,Crop crop) {
        if(crop==null || crop.token()==null || crop.width()<1 || crop.width()>64 || crop.height()<1 || crop.height()>64
                || crop.pixels()==null || crop.pixels().length!=crop.width()*crop.height()*3)
            throw new IllegalArgumentException("Bounded RGB item crop required");
        access(actor,tenant,job,crop.token());
        tx.execute(actor,tenant,"edge:process",db -> {
            try(var query=q(db,"""
                    SELECT grant_id,actor_ref FROM dzoka.edge_jobs WHERE id=? AND lease=? AND worker=?
                     AND state='RUNNING' AND lease_until>clock_timestamp()
                    """,job,crop.token(),actor.reference());var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                scope(db,row.getObject(1,UUID.class),row.getString(2));
            }
            String sha=org.dzoka.foundation.application.ContentDigest.sha256(crop.pixels());
            try(var query=q(db,"SELECT sha,width,height FROM dzoka.edge_evidence WHERE job_id=? AND id=?",job,id);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!sha.equals(row.getString(1)) || crop.width()!=row.getInt(2) || crop.height()!=row.getInt(3)) throw new Failures.Conflict();
                    return null;
                }
            }
            try(var query=q(db,"SELECT count(*) FROM dzoka.edge_evidence WHERE job_id=?",job);var row=query.executeQuery()) {
                row.next();if(row.getInt(1)>=4) throw new Failures.Conflict();
            }
            write(db,"INSERT INTO dzoka.edge_evidence(tenant_id,job_id,id,width,height,pixels,sha) VALUES(?,?,?,?,?,?,?)",
                    tenant,job,id,crop.width(),crop.height(),crop.pixels(),sha);
            return null;
        });
    }
    public void approveEvidence(VerifiedIdentity actor,UUID tenant,UUID job,UUID id,String sha) {
        if(!digest(sha)) throw new IllegalArgumentException("Protected redaction review required");
        tx.execute(actor,tenant,"edge:review",db -> {
            try(var query=q(db,"SELECT g.* FROM dzoka.investigation_grants g WHERE id=(SELECT grant_id FROM dzoka.edge_jobs WHERE id=?)",job);
                var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                var g=scope(db,row.getObject("id",UUID.class),row.getString("investigator"));
                if(actor.reference().equals(g.investigator())) throw new Failures.Forbidden();
            }
            if(write(db,"UPDATE dzoka.edge_evidence SET approved=true,reviewer=?,approval_sha=? WHERE job_id=? AND id=?",
                    actor.reference(),sha,job,id)==0) throw new Failures.NotFound();
            return null;
        });
    }
    public byte[] evidence(VerifiedIdentity actor,UUID tenant,UUID job,UUID id,boolean review) {
        return tx.execute(actor,tenant,review?"edge:review":"edge:investigate",db -> {
            try(var query=q(db,"SELECT grant_id,actor_ref FROM dzoka.edge_jobs WHERE id=?",job);var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                scope(db,row.getObject(1,UUID.class),row.getString(2));
            }
            try(var query=q(db,"SELECT width,height,pixels,approved FROM dzoka.edge_evidence WHERE job_id=? AND id=?",job,id);
                var row=query.executeQuery()) {
                if(!row.next() || (!review && !row.getBoolean(4))) throw new Failures.NotFound();
                int width=row.getInt(1),height=row.getInt(2);
                byte[] pixels=row.getBytes(3);
                var image=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                    int index=(y*width+x)*3;
                    image.setRGB(x,y,((pixels[index]&255)<<16)|((pixels[index+1]&255)<<8)|(pixels[index+2]&255));
                }
                try(var output=new java.io.ByteArrayOutputStream()) {
                    javax.imageio.ImageIO.write(image,"png",output);return output.toByteArray();
                } catch(java.io.IOException failure) { throw new Failures.Unavailable(failure); }
            }
        });
    }
    public int purge(VerifiedIdentity actor,UUID tenant) {
        return tx.execute(actor,tenant,"edge:process",db -> {
            write(db,"""
                    DELETE FROM dzoka.edge_evidence WHERE (job_id,id) IN (
                      SELECT e.job_id,e.id FROM dzoka.edge_evidence e JOIN dzoka.edge_jobs j
                        ON j.tenant_id=e.tenant_id AND j.id=e.job_id
                      JOIN dzoka.investigation_grants g ON g.tenant_id=j.tenant_id AND g.id=j.grant_id
                      WHERE g.revoked OR g.expires_at<=clock_timestamp() ORDER BY e.created_at,e.id LIMIT 100)
                    """);
            return write(db,"""
                UPDATE dzoka.edge_jobs j SET result=NULL,state='CANCELLED'
                WHERE j.id IN (
                  SELECT e.id FROM dzoka.edge_jobs e JOIN dzoka.investigation_grants g ON g.tenant_id=e.tenant_id AND g.id=e.grant_id
                  WHERE (g.revoked OR g.expires_at<=clock_timestamp())
                    AND (e.result IS NOT NULL OR e.state<>'CANCELLED') ORDER BY e.created_at,e.id LIMIT 100)
                """);
        });
    }

    public void grant(VerifiedIdentity actor,UUID tenant,UUID id,Grant g) {
        if(g==null || g.report()==null || g.camera()==null || g.investigator()==null || !g.investigator().matches("[0-9a-f]{64}")
                || g.start()==null || g.end()==null || g.expires()==null || !g.start().isBefore(g.end())
                || Duration.between(g.start(),g.end()).compareTo(Duration.ofHours(24))>0
                || !g.expires().isAfter(Instant.now()) || g.expires().isAfter(Instant.now().plus(Duration.ofDays(7)))
                || g.budget()<1 || g.budget()>100 || !digest(g.evidenceSha())) throw new IllegalArgumentException("Invalid investigation scope");
        tx.execute(actor,tenant,"edge:review",db -> {
            available(db,g.report());
            cameraDigest(db,g.camera());
            try(var q=q(db,"SELECT * FROM dzoka.investigation_grants WHERE id=?",id);var row=q.executeQuery()) {
                if(row.next()) {
                    if(!g.equals(readGrant(row)) || !actor.reference().equals(row.getString("reviewer")) || row.getBoolean("revoked"))
                        throw new Failures.Conflict();
                    return null;
                }
            }
            write(db,"""
                    INSERT INTO dzoka.investigation_grants(tenant_id,id,report_id,camera,investigator,reviewer,starts_at,ends_at,expires_at,budget,evidence_sha)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?)
                    """,tenant,id,g.report(),g.camera(),g.investigator(),actor.reference(),at(g.start()),at(g.end()),at(g.expires()),g.budget(),g.evidenceSha());
            return null;
        });
    }
    public void revoke(VerifiedIdentity actor,UUID tenant,UUID grant) {
        tx.execute(actor,tenant,"edge:review",db -> {
            lockGrant(db,grant);
            if(write(db,"UPDATE dzoka.investigation_grants SET revoked=true WHERE id=?",grant)==0) throw new Failures.NotFound();
            return null;
        });
    }
    public void submit(VerifiedIdentity actor,UUID tenant,UUID job,UUID grant) {
        tx.execute(actor,tenant,"edge:investigate",db -> {
            scope(db,grant,actor.reference());
            try(var query=q(db,"SELECT grant_id,actor_ref FROM dzoka.edge_jobs WHERE id=?",job);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!grant.equals(row.getObject(1,UUID.class)) || !actor.reference().equals(row.getString(2))) throw new Failures.Conflict();
                    return null;
                }
            }
            try(var query=q(db,"SELECT 1 FROM dzoka.edge_jobs WHERE grant_id=?",grant);var row=query.executeQuery()) {
                if(row.next()) throw new Failures.Conflict();
            }
            write(db,"INSERT INTO dzoka.edge_jobs(tenant_id,id,grant_id,actor_ref) VALUES(?,?,?,?)",tenant,job,grant,actor.reference());
            return null;
        });
    }
    public Lease claim(VerifiedIdentity actor,UUID tenant) {
        return tx.execute(actor,tenant,"edge:process",db -> {
            write(db,"""
                    UPDATE dzoka.edge_jobs j SET state='CANCELLED',result=NULL WHERE state IN ('QUEUED','RUNNING')
                    AND EXISTS(SELECT 1 FROM dzoka.investigation_grants g WHERE g.id=j.grant_id
                      AND (g.revoked OR g.expires_at<=clock_timestamp() OR j.attempts>=3))
                    """);
            try(var query=q(db,"""
                    SELECT j.id,j.grant_id,j.actor_ref FROM dzoka.edge_jobs j
                    WHERE state='QUEUED' OR (state='RUNNING' AND lease_until<=clock_timestamp())
                    ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED
                    """);var row=query.executeQuery()) {
                if(!row.next()) return null;
                UUID job=row.getObject(1,UUID.class),grant=row.getObject(2,UUID.class);
                Grant g;
                try { g=scope(db,grant,row.getString(3)); }
                catch(Failures.Conflict | Failures.NotFound denied) {
                    write(db,"UPDATE dzoka.edge_jobs SET state='CANCELLED',result=NULL WHERE id=?",job);
                    return null;
                }
                UUID token=UUID.randomUUID();
                write(db,"""
                        UPDATE dzoka.edge_jobs SET state='RUNNING',lease=?,worker=?,lease_until=least(clock_timestamp()+interval '1 minute',?),
                        attempts=attempts+1 WHERE id=?
                        """,token,actor.reference(),at(g.expires()),job);
                return new Lease(job,token,g.camera(),g.start(),g.end(),g.budget(),cameraDigest(db,g.camera()),g.evidenceSha());
            }
        });
    }
    public Lease access(VerifiedIdentity actor,UUID tenant,UUID job,UUID token) {
        return tx.execute(actor,tenant,"edge:process",db -> {
            try(var query=q(db,"SELECT * FROM dzoka.edge_jobs WHERE id=? FOR UPDATE",job);var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                if(!token.equals(row.getObject("lease",UUID.class)) || !actor.reference().equals(row.getString("worker"))
                        || !"RUNNING".equals(row.getString("state"))
                        || !row.getObject("lease_until",OffsetDateTime.class).toInstant().isAfter(Instant.now()))
                    throw new Failures.Conflict();
                var g=scope(db,row.getObject("grant_id",UUID.class),row.getString("actor_ref"));
                write(db,"UPDATE dzoka.edge_jobs SET lease_until=least(clock_timestamp()+interval '1 minute',?) WHERE id=? AND lease=?",
                        at(g.expires()),job,token);
                return new Lease(job,token,g.camera(),g.start(),g.end(),g.budget(),cameraDigest(db,g.camera()),g.evidenceSha());
            }
        });
    }
    public boolean complete(VerifiedIdentity actor,UUID tenant,UUID job,Completion result) {
        if(result==null || result.token()==null || result.observations()==null) throw new IllegalArgumentException("Missing result");
        return tx.execute(actor,tenant,"edge:process",db -> {
            try(var query=q(db,"SELECT * FROM dzoka.edge_jobs WHERE id=? FOR UPDATE",job);var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                if(!result.token().equals(row.getObject("lease",UUID.class)) || !actor.reference().equals(row.getString("worker"))
                        || !"RUNNING".equals(row.getString("state")) || !row.getObject("lease_until",OffsetDateTime.class).toInstant().isAfter(Instant.now()))
                    throw new Failures.Conflict();
                Grant grant;
                try { grant=scope(db,row.getObject("grant_id",UUID.class),row.getString("actor_ref")); }
                catch(Failures.Conflict | Failures.NotFound revoked) {
                    write(db,"UPDATE dzoka.edge_jobs SET state='CANCELLED',result=NULL WHERE id=?",job);return false;
                }
                if(result.observations().size()>grant.budget()) throw new IllegalArgumentException("Observation budget exceeded");
                Instant previous=grant.start();
                for(var observation:result.observations()) {
                    if(observation==null || observation.after()==null || observation.before()==null
                            || observation.after().isBefore(previous) || observation.before().isBefore(observation.after())
                            || observation.before().isAfter(grant.end()) || !digest(observation.sourceSha()) || !digest(observation.processorSha())
                            || observation.status()==null || !Set.of("PRESENT","NOT_OBSERVED","UNKNOWN").contains(observation.status())
                            || observation.gap()!=observation.status().equals("UNKNOWN"))
                        throw new IllegalArgumentException("Invalid observation interval or provenance");
                    previous=observation.before();
                }
                write(db,"UPDATE dzoka.edge_jobs SET state='COMPLETE',result=?::jsonb WHERE id=?",
                        json.writeValueAsString(result.observations()),job);
                return true;
            }
        });
    }
    public Result read(VerifiedIdentity actor,UUID tenant,UUID job) {
        return tx.execute(actor,tenant,"edge:investigate",db -> {
            try(var query=q(db,"SELECT grant_id,actor_ref,state,result::text FROM dzoka.edge_jobs WHERE id=?",job);var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                scope(db,row.getObject(1,UUID.class),actor.reference());
                return new Result(row.getString(3),row.getString(4)==null?List.of():List.of(json.readValue(row.getString(4),Observation[].class)));
            }
        });
    }
    private static Grant scope(Connection db,UUID id,String investigator) throws SQLException {
        lockGrant(db,id);
        try(var query=q(db,"SELECT * FROM dzoka.investigation_grants WHERE id=?",id);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
            var grant=readGrant(row);
            if(row.getBoolean("revoked") || !grant.expires().isAfter(Instant.now()) || !grant.investigator().equals(investigator))
                throw new Failures.Conflict();
            available(db,grant.report());
            cameraDigest(db,grant.camera());
            return grant;
        }
    }
    private static String cameraDigest(Connection db,UUID camera) throws SQLException {
        try(var query=q(db,"SELECT archive_sha FROM dzoka.edge_cameras WHERE id=? AND active",camera);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
            return row.getString(1);
        }
    }
    private static void lockGrant(Connection db,UUID id) throws SQLException {
        try(var query=q(db,"SELECT pg_advisory_xact_lock(hashtextextended(dzoka.current_tenant()::text||?,22))",id.toString())) { query.execute(); }
    }
    private static void available(Connection db,UUID report) throws SQLException {
        try(var query=q(db,"SELECT 1 FROM dzoka.reports WHERE id=? AND deleted_at IS NULL AND NOT legal_hold AND expires_at>clock_timestamp()",report);
            var row=query.executeQuery()) { if(!row.next()) throw new Failures.NotFound(); }
    }
    private static Grant readGrant(ResultSet row) throws SQLException {
        return new Grant(row.getObject("report_id",UUID.class),row.getObject("camera",UUID.class),row.getString("investigator"),
                row.getObject("starts_at",OffsetDateTime.class).toInstant(),row.getObject("ends_at",OffsetDateTime.class).toInstant(),
                row.getObject("expires_at",OffsetDateTime.class).toInstant(),row.getInt("budget"),row.getString("evidence_sha"));
    }
    private static boolean digest(String value) { return value!=null && value.matches("[0-9a-f]{64}"); }
    private static OffsetDateTime at(Instant value) { return value.atOffset(ZoneOffset.UTC); }
    private static PreparedStatement q(Connection db,String sql,Object... args) throws SQLException {
        var query=db.prepareStatement(sql);for(int i=0;i<args.length;i++) query.setObject(i+1,args[i]);return query;
    }
    private static int write(Connection db,String sql,Object... args) throws SQLException {
        try(var query=q(db,sql,args)) { return query.executeUpdate(); }
    }
}
