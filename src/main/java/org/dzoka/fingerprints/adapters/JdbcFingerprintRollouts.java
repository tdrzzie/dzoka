package org.dzoka.fingerprints.adapters;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.foundation.application.*;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Component;

@Component
public final class JdbcFingerprintRollouts implements org.dzoka.fingerprints.ports.FingerprintRollouts {
    private final TenantTransactions tx;
    public JdbcFingerprintRollouts(TenantTransactions tx){this.tx=tx;}
    private static String digest(String value){return ContentDigest.sha256(value.getBytes(StandardCharsets.UTF_8));}
    private static void lock(Connection c,UUID tenant)throws SQLException{
        try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")){s.setString(1,"ai:"+tenant);s.execute();}
    }
    private static PreparedStatement query(Connection c,String sql,UUID tenant,UUID id)throws SQLException{
        var s=c.prepareStatement(sql);s.setObject(1,tenant);s.setObject(2,id);return s;
    }
    private static Plan plan(Connection c,UUID tenant,UUID id)throws SQLException{
        int minimum;String evaluation;
        try(var s=query(c,"SELECT minimum_coverage_bps,evaluation_sha256 FROM dzoka.fingerprint_rollouts WHERE tenant_id=? AND id=?",tenant,id);var r=s.executeQuery()){
            if(!r.next())throw new Failures.NotFound();minimum=r.getInt(1);evaluation=r.getString(2);
        }
        var sources=new ArrayList<Source>();var spaces=new ArrayList<UUID>();
        try(var s=query(c,"SELECT report_id,source_revision FROM dzoka.fingerprint_rollout_sources WHERE tenant_id=? AND rollout_id=? ORDER BY report_id",tenant,id);var r=s.executeQuery()){
            while(r.next())sources.add(new Source(r.getObject(1,UUID.class),r.getInt(2)));
        }
        try(var s=query(c,"SELECT space_id FROM dzoka.fingerprint_rollout_spaces WHERE tenant_id=? AND rollout_id=? ORDER BY space_id",tenant,id);var r=s.executeQuery()){
            while(r.next())spaces.add(r.getObject(1,UUID.class));
        }
        var pins=new ArrayList<PerceptionPin>();
        try(var s=query(c,"SELECT report_id,component_id FROM dzoka.fingerprint_rollout_perception_pins WHERE tenant_id=? AND rollout_id=? ORDER BY report_id,component_id",tenant,id);var r=s.executeQuery()){
            while(r.next())pins.add(new PerceptionPin(r.getObject(1,UUID.class),r.getObject(2,UUID.class)));
        }
        return new Plan(id,sources,spaces,minimum,evaluation,pins);
    }
    private static void validateSources(Connection c,UUID tenant,Plan plan)throws SQLException{
        for(var source:plan.sources())try(var s=query(c,"SELECT current_revision FROM dzoka.reports WHERE tenant_id=? AND id=? AND deleted_at IS NULL AND expires_at>clock_timestamp() FOR UPDATE",tenant,source.reportId());var r=s.executeQuery()){
            if(!r.next()||r.getInt(1)!=source.revision())throw new Failures.Conflict();
        }
        for(var space:plan.spaces())try(var s=query(c,"SELECT 1 FROM dzoka.embedding_spaces WHERE tenant_id=? AND id=? AND enabled FOR SHARE",tenant,space);var r=s.executeQuery()){
            if(!r.next())throw new Failures.Conflict();
        }
        for(var source:plan.sources())perception(c,tenant,source,plan);
    }
    private static void journal(Connection c,VerifiedIdentity actor,UUID tenant,Plan plan,String type)throws SQLException{
        for(var source:plan.sources())try(var s=query(c,"UPDATE dzoka.reports SET version=version+1 WHERE tenant_id=? AND id=? RETURNING version",tenant,source.reportId());var r=s.executeQuery()){
            if(!r.next())throw new Failures.Conflict();MutationJournal.append(c,tenant,actor,type,source.reportId(),r.getLong(1),"report_id");
        }
    }
    private static Status status(Connection c,UUID tenant,UUID id)throws SQLException{
        Plan plan=plan(c,tenant,id);int available=0,unavailable=0;
        try(var s=query(c,"SELECT state,count(*) FROM dzoka.fingerprint_snapshot_slots WHERE tenant_id=? AND rollout_id=? GROUP BY state",tenant,id);var r=s.executeQuery()){
            while(r.next()){if(r.getString(1).equals("AVAILABLE"))available=r.getInt(2);else unavailable=r.getInt(2);}
        }
        try(var s=query(c,"SELECT count(*) FROM dzoka.fingerprint_perception_slots WHERE tenant_id=? AND rollout_id=?",tenant,id);var r=s.executeQuery()){r.next();available+=r.getInt(1);}
        int total=plan.sources().size()*plan.spaces().size()+plan.perceptionPins().size();
        try(var s=query(c,"SELECT state,plan_sha256 FROM dzoka.fingerprint_rollouts WHERE tenant_id=? AND id=?",tenant,id);var r=s.executeQuery()){
            r.next();return new Status(id,r.getString(1),plan.sources().size(),plan.spaces().size(),total,available,unavailable,
                    available*10000/total,plan.minimumCoverageBps(),plan.evaluationSha256(),r.getString(2),plan.perceptionPins().size());
        }
    }
    public Status create(VerifiedIdentity actor,UUID tenant,Plan plan){
        Objects.requireNonNull(plan);
        return tx.execute(actor,tenant,"fingerprints:manage",c->{
            lock(c,tenant);String hash=digest(plan.canonical());
            try(var s=query(c,"SELECT plan_sha256 FROM dzoka.fingerprint_rollouts WHERE tenant_id=? AND id=?",tenant,plan.id());var r=s.executeQuery()){
                if(r.next()){if(!hash.equals(r.getString(1)))throw new Failures.Conflict();return status(c,tenant,plan.id());}
            }
            validateSources(c,tenant,plan);
            try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_rollouts(tenant_id,id,plan_sha256,evaluation_sha256,minimum_coverage_bps) VALUES(?,?,?,?,?)")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setString(3,hash);s.setString(4,plan.evaluationSha256());s.setInt(5,plan.minimumCoverageBps());s.executeUpdate();
            }
            for(var source:plan.sources())try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_rollout_sources(tenant_id,rollout_id,report_id,source_revision) VALUES(?,?,?,?)")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setObject(3,source.reportId());s.setInt(4,source.revision());s.executeUpdate();
            }
            for(var space:plan.spaces())try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_rollout_spaces(tenant_id,rollout_id,space_id) VALUES(?,?,?)")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setObject(3,space);s.executeUpdate();
            }
            for(var pin:plan.perceptionPins())try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_rollout_perception_pins(tenant_id,rollout_id,report_id,component_id) VALUES(?,?,?,?)")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setObject(3,pin.reportId());s.setObject(4,pin.componentId());s.executeUpdate();
            }
            journal(c,actor,tenant,plan,"fingerprint.rollout.created");return status(c,tenant,plan.id());
        });
    }
    private record Slot(UUID space,UUID component,String state,String input,String key){}
    private static List<Slot> slots(Connection c,UUID tenant,Source source,Plan plan)throws SQLException{
        var slots=new ArrayList<Slot>();
        for(var space:plan.spaces())try(var s=c.prepareStatement("""
            SELECT j.state,j.input_sha256,e.space_key,f.id FROM dzoka.ai_jobs j
            JOIN dzoka.embedding_spaces e ON e.tenant_id=j.tenant_id AND e.id=j.space_id
            LEFT JOIN dzoka.fingerprint_components f ON f.tenant_id=j.tenant_id AND f.job_id=j.id
            WHERE j.tenant_id=? AND j.report_id=? AND j.source_revision=? AND j.space_id=?
            """)){
            s.setObject(1,tenant);s.setObject(2,source.reportId());s.setInt(3,source.revision());s.setObject(4,space);
            try(var r=s.executeQuery()){
                if(!r.next())throw new Failures.Conflict();
                UUID component=r.getObject(4,UUID.class);String state=r.getString(1);
                if(!state.equals("UNAVAILABLE")&&!(state.equals("SUCCEEDED")&&component!=null))throw new Failures.Conflict();
                slots.add(new Slot(space,component,component==null?"UNAVAILABLE":"AVAILABLE",r.getString(2),r.getString(3)));
            }
        }
        return slots;
    }
    private record Pinned(UUID id,String key,String input,String output){}
    private static List<Pinned> perception(Connection c,UUID tenant,Source source,Plan plan)throws SQLException{
        var result=new ArrayList<Pinned>();
        for(var pin:plan.perceptionPins()){
            if(!pin.reportId().equals(source.reportId()))continue;
            try(var s=query(c,"""
                SELECT f.profile_id,f.input_sha256,p.profile_key,encode(sha256(convert_to(f.evidence::text,'UTF8')),'hex') output
                FROM dzoka.perception_components f JOIN dzoka.perception_profiles p ON p.tenant_id=f.tenant_id AND p.id=f.profile_id
                JOIN dzoka.perception_jobs j ON j.tenant_id=f.tenant_id AND j.id=f.job_id
                WHERE f.tenant_id=? AND f.id=? AND f.report_id=? AND f.source_revision=? AND p.enabled AND j.state='SUCCEEDED'
                 AND (j.media_id IS NULL OR EXISTS(SELECT 1 FROM dzoka.media_assets m WHERE m.tenant_id=j.tenant_id AND m.id=j.media_id
                  AND m.state='SANITIZED' AND NOT m.purge_pending AND m.expires_at>clock_timestamp() AND m.derivative_sha256=f.input_sha256))
                """,tenant,pin.componentId())){
                s.setObject(3,source.reportId());s.setInt(4,source.revision());try(var r=s.executeQuery()){
                    if(!r.next())throw new Failures.Conflict();UUID profile=r.getObject(1,UUID.class);
                    if(!JdbcPerceptionJobs.verifiedProfileKey(c,tenant,profile).equals(r.getString(3)))throw new Failures.Conflict();
                    result.add(new Pinned(pin.componentId(),r.getString(3),r.getString(2),r.getString(4)));
                }
            }
        }return result;
    }
    private static String manifest(UUID tenant,Source source,List<Slot> slots,List<Pinned> pins){
        var body=new StringBuilder("fingerprint-snapshot-v1\n"+tenant+"\n"+source.reportId()+"\n"+source.revision());
        for(var slot:slots)body.append('\n').append(slot.space()).append(':').append(slot.key()).append(':').append(slot.input())
            .append(':').append(slot.state()).append(':').append(slot.component()==null?"-":slot.component());
        for(var pin:pins)body.append("\nP:").append(pin.id()).append(':').append(pin.key()).append(':').append(pin.input()).append(':').append(pin.output());
        return digest(body.toString());
    }
    public Status seal(VerifiedIdentity actor,UUID tenant,UUID id){
        return tx.execute(actor,tenant,"fingerprints:manage",c->{
            lock(c,tenant);Plan plan=plan(c,tenant,id);validateSources(c,tenant,plan);
            var prior=status(c,tenant,id);if(prior.state().equals("SEALED")){validateSealed(c,tenant,plan);return prior;}
            int available=plan.perceptionPins().size();var bySource=new LinkedHashMap<Source,List<Slot>>();
            for(var source:plan.sources()){var slots=slots(c,tenant,source,plan);bySource.put(source,slots);available+=slots.stream().filter(s->s.component()!=null).count();}
            if((long)available*10000<(long)plan.minimumCoverageBps()*(plan.sources().size()*plan.spaces().size()+plan.perceptionPins().size()))throw new Failures.Conflict();
            for(var entry:bySource.entrySet()){
                var source=entry.getKey();var slots=entry.getValue();var pins=perception(c,tenant,source,plan);
                try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_snapshots(tenant_id,rollout_id,report_id,manifest_sha256) VALUES(?,?,?,?)")){
                    s.setObject(1,tenant);s.setObject(2,id);s.setObject(3,source.reportId());s.setString(4,manifest(tenant,source,slots,pins));s.executeUpdate();
                }
                for(var pin:pins)try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_perception_slots(tenant_id,rollout_id,report_id,component_id) VALUES(?,?,?,?)")){
                    s.setObject(1,tenant);s.setObject(2,id);s.setObject(3,source.reportId());s.setObject(4,pin.id());s.executeUpdate();
                }
                for(var slot:slots)try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_snapshot_slots(tenant_id,rollout_id,report_id,space_id,component_id,state) VALUES(?,?,?,?,?,?)")){
                    s.setObject(1,tenant);s.setObject(2,id);s.setObject(3,source.reportId());s.setObject(4,slot.space());s.setObject(5,slot.component());s.setString(6,slot.state());s.executeUpdate();
                }
            }
            try(var s=query(c,"UPDATE dzoka.fingerprint_rollouts SET state='SEALED',sealed_at=clock_timestamp() WHERE tenant_id=? AND id=?",tenant,id)){s.executeUpdate();}
            journal(c,actor,tenant,plan,"fingerprint.rollout.sealed");return status(c,tenant,id);
        });
    }
    private static void validateSealed(Connection c,UUID tenant,Plan plan)throws SQLException{
        var summary=status(c,tenant,plan.id());
        if(!summary.state().equals("SEALED")||summary.availableSlots()+summary.unavailableSlots()!=summary.expectedSlots()
                ||(long)summary.availableSlots()*10000<(long)summary.minimumCoverageBps()*summary.expectedSlots())throw new Failures.Conflict();
        for(var source:plan.sources()){
            var current=slots(c,tenant,source,plan);var pins=perception(c,tenant,source,plan);
            try(var s=c.prepareStatement("SELECT manifest_sha256 FROM dzoka.fingerprint_snapshots WHERE tenant_id=? AND rollout_id=? AND report_id=?")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setObject(3,source.reportId());try(var r=s.executeQuery()){
                    if(!r.next()||!r.getString(1).equals(manifest(tenant,source,current,pins)))throw new Failures.Conflict();
                }
            }
            for(var pin:pins)try(var s=c.prepareStatement("SELECT 1 FROM dzoka.fingerprint_perception_slots WHERE tenant_id=? AND rollout_id=? AND report_id=? AND component_id=?")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setObject(3,source.reportId());s.setObject(4,pin.id());try(var r=s.executeQuery()){if(!r.next())throw new Failures.Conflict();}
            }
            for(var slot:current)try(var s=c.prepareStatement("SELECT component_id,state FROM dzoka.fingerprint_snapshot_slots WHERE tenant_id=? AND rollout_id=? AND report_id=? AND space_id=?")){
                s.setObject(1,tenant);s.setObject(2,plan.id());s.setObject(3,source.reportId());s.setObject(4,slot.space());try(var r=s.executeQuery()){
                    if(!r.next()||!Objects.equals(slot.component(),r.getObject(1,UUID.class))||!slot.state().equals(r.getString(2)))throw new Failures.Conflict();
                }
            }
        }
    }
    public Status read(VerifiedIdentity actor,UUID tenant,UUID id){return tx.execute(actor,tenant,"fingerprints:manage",c->status(c,tenant,id));}
    public Activation active(VerifiedIdentity actor,UUID tenant){
        return tx.execute(actor,tenant,"fingerprints:manage",c->{
            try(var s=c.prepareStatement("SELECT rollout_id,version FROM dzoka.fingerprint_active WHERE tenant_id=?")){
                s.setObject(1,tenant);try(var r=s.executeQuery()){return r.next()?new Activation(r.getObject(1,UUID.class),r.getLong(2)):new Activation(null,0);}
            }
        });
    }
    public Activation activate(VerifiedIdentity actor,UUID tenant,UUID id,long expectedVersion,boolean rollback){
        if(expectedVersion<0)throw new IllegalArgumentException("Invalid activation version");
        return tx.execute(actor,tenant,"fingerprints:manage",c->{
            lock(c,tenant);UUID previous=null;long version=0;
            try(var s=c.prepareStatement("SELECT rollout_id,version FROM dzoka.fingerprint_active WHERE tenant_id=?")){
                s.setObject(1,tenant);try(var r=s.executeQuery()){if(r.next()){previous=r.getObject(1,UUID.class);version=r.getLong(2);}}
            }
            // Exact retry of an already acknowledged transition, without allowing
            // a stale caller to change the pointer or manufacture a new version.
            if(version==expectedVersion+1&&id.equals(previous)){
                try(var s=c.prepareStatement("SELECT action,actor_ref FROM dzoka.fingerprint_activations WHERE tenant_id=? AND version=?")){
                    s.setObject(1,tenant);s.setLong(2,version);try(var r=s.executeQuery()){
                        if(r.next()&&r.getString(1).equals(rollback?"ROLLBACK":"PROMOTE")&&r.getString(2).equals(actor.reference()))return new Activation(id,version);
                    }
                }
            }
            if(version!=expectedVersion||id.equals(previous))throw new Failures.Conflict();
            Plan plan=plan(c,tenant,id);validateSources(c,tenant,plan);validateSealed(c,tenant,plan);
            if(rollback){
                try(var s=query(c,"SELECT 1 FROM dzoka.fingerprint_activations WHERE tenant_id=? AND rollout_id=?",tenant,id);var r=s.executeQuery()){
                    if(!r.next())throw new Failures.Conflict();
                }
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_active(tenant_id,rollout_id,version) VALUES(?,?,?) ON CONFLICT(tenant_id) DO UPDATE SET rollout_id=excluded.rollout_id,version=excluded.version")){
                s.setObject(1,tenant);s.setObject(2,id);s.setLong(3,version+1);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.fingerprint_activations(tenant_id,version,rollout_id,previous_rollout_id,actor_ref,action) VALUES(?,?,?,?,?,?)")){
                s.setObject(1,tenant);s.setLong(2,version+1);s.setObject(3,id);s.setObject(4,previous);s.setString(5,actor.reference());s.setString(6,rollback?"ROLLBACK":"PROMOTE");s.executeUpdate();
            }
            journal(c,actor,tenant,plan,rollback?"fingerprint.rollout.rolled_back":"fingerprint.rollout.promoted");return new Activation(id,version+1);
        });
    }
}
