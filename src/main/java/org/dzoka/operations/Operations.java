package org.dzoka.operations;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.learning.Learning;
import org.springframework.stereotype.Service;
import tools.jackson.databind.*;

@Service
public final class Operations {
    private final TenantTransactions tx;private final ObjectMapper json;
    public Operations(TenantTransactions tx,ObjectMapper json){this.tx=tx;this.json=json;}
    public JsonNode status(VerifiedIdentity actor,UUID tenant){return tx.execute(actor,tenant,"operations:read",db->{
        try(var s=Learning.q(db,"SELECT dzoka.operations_summary()::text");var r=s.executeQuery()){r.next();return json.readTree(r.getString(1));}
    });}
    public JsonNode analytics(VerifiedIdentity actor,UUID tenant,Instant since){
        if(since==null||since.isAfter(Instant.now())||since.isBefore(Instant.now().minus(Duration.ofDays(366))))throw new IllegalArgumentException();
        return tx.execute(actor,tenant,"analytics:read",db->{try(var s=Learning.q(db,"SELECT (dzoka.institution_analytics(?) || dzoka.audited_outcome_metrics(?))::text",Timestamp.from(since.truncatedTo(java.time.temporal.ChronoUnit.DAYS)),Timestamp.from(since.truncatedTo(java.time.temporal.ChronoUnit.DAYS)));var r=s.executeQuery()){r.next();return json.readTree(r.getString(1));}});
    }
    public record Erasure(UUID report,long version){public Erasure{if(report==null||version<1)throw new IllegalArgumentException();}}
    public record Replay(String sourceSha,List<Erasure> erasures){public Replay{erasures=List.copyOf(erasures);if(erasures.size()>1000)throw new IllegalArgumentException();Learning.sha(sourceSha);}}
    public record Open(String manifestSha,String identityConfigurationReviewSha,String projectionReviewSha){public Open{Learning.sha(manifestSha);Learning.sha(identityConfigurationReviewSha);Learning.sha(projectionReviewSha);}}
    public record Policy(int reportRetentionDays,int dailyActorLimit,int dailyTenantLimit,String evidenceSha){public Policy{Learning.sha(evidenceSha);if(reportRetentionDays<1||reportRetentionDays>365||dailyActorLimit<1||dailyActorLimit>10000||dailyTenantLimit<dailyActorLimit||dailyTenantLimit>100000)throw new IllegalArgumentException();}}
    public void policy(VerifiedIdentity actor,UUID tenant,Policy value){tx.execute(actor,tenant,"retention:manage",db->{Learning.lock(db,tenant);Learning.write(db,"UPDATE dzoka.tenants SET report_retention_days=?,report_daily_actor_limit=?,report_daily_tenant_limit=? WHERE id=?",value.reportRetentionDays(),value.dailyActorLimit(),value.dailyTenantLimit(),tenant);audit(db,tenant,actor,"POLICY",value.evidenceSha(),json.writeValueAsString(value));return null;});}
    public List<Erasure> erasures(VerifiedIdentity actor,UUID tenant,UUID after){return tx.execute(actor,tenant,"retention:recover",db->{var result=new ArrayList<Erasure>();try(var s=Learning.q(db,"SELECT report_id,source_version FROM dzoka.erasure_journal WHERE (?::uuid IS NULL OR report_id>?::uuid) ORDER BY report_id LIMIT 1000",after,after);var r=s.executeQuery()){while(r.next())result.add(new Erasure(r.getObject(1,UUID.class),r.getLong(2)));}return List.copyOf(result);});}
    public void close(VerifiedIdentity actor,UUID tenant,String evidence){
        Learning.sha(evidence);tx.execute(actor,tenant,"retention:recover",db->{Learning.lock(db,tenant);
            Learning.write(db,"INSERT INTO dzoka.restore_barriers(tenant_id,closed,actor_ref,evidence_sha) VALUES(?,true,?,?) ON CONFLICT(tenant_id) DO UPDATE SET closed=true,manifest_sha=NULL,actor_ref=EXCLUDED.actor_ref,evidence_sha=EXCLUDED.evidence_sha,updated_at=clock_timestamp()",tenant,actor.reference(),evidence);audit(db,tenant,actor,"RESTORE_CLOSED",evidence,"{}");return null;
        });
    }
    public String replay(VerifiedIdentity actor,UUID tenant,Replay value){
        var ordered=value.erasures().stream().sorted(Comparator.comparing(e->e.report().toString())).toList();
        if(ordered.stream().map(Erasure::report).distinct().count()!=ordered.size())throw new IllegalArgumentException();
        String manifest=Learning.hash(json.writeValueAsString(ordered)+":"+value.sourceSha());
        return tx.execute(actor,tenant,"retention:recover",db->{Learning.lock(db,tenant);closed(db);
            try(var s=Learning.q(db,"SELECT manifest_sha FROM dzoka.restore_barriers WHERE tenant_id=?",tenant);var r=s.executeQuery()){r.next();if(manifest.equals(r.getString(1)))return manifest;}
            for(var erasure:ordered){
                try(var s=Learning.q(db,"SELECT legal_hold FROM dzoka.reports WHERE id=? FOR UPDATE",erasure.report());var r=s.executeQuery()){if(r.next()&&r.getBoolean(1))throw new Failures.Conflict();}
                Learning.write(db,"UPDATE dzoka.reports SET deleted_at=coalesce(deleted_at,clock_timestamp()),version=greatest(version+1,?) WHERE id=? AND NOT legal_hold",erasure.version(),erasure.report());
                Learning.write(db,"INSERT INTO dzoka.erasure_journal(tenant_id,report_id,source_version) VALUES(?,?,?) ON CONFLICT DO NOTHING",tenant,erasure.report(),erasure.version());
            }
            Learning.write(db,"UPDATE dzoka.restore_barriers SET manifest_sha=?,actor_ref=?,evidence_sha=?,updated_at=clock_timestamp() WHERE tenant_id=?",manifest,actor.reference(),value.sourceSha(),tenant);audit(db,tenant,actor,"ERASURES_REPLAYED",manifest,json.writeValueAsString(Map.of("count",ordered.size())));return manifest;
        });
    }
    public void open(VerifiedIdentity actor,UUID tenant,Open value){
        tx.execute(actor,tenant,"retention:recover",db->{Learning.lock(db,tenant);closed(db);
            try(var s=Learning.q(db,"SELECT manifest_sha,dzoka.restore_erasure_pending() FROM dzoka.restore_barriers WHERE tenant_id=? FOR UPDATE",tenant);var r=s.executeQuery()){
                r.next();if(!value.manifestSha().equals(r.getString(1))||r.getObject(2)==null||r.getLong(2)!=0)throw new Failures.Conflict();
            }
            String review=Learning.hash(value.identityConfigurationReviewSha()+":"+value.projectionReviewSha());Learning.write(db,"UPDATE dzoka.restore_barriers SET closed=false,actor_ref=?,evidence_sha=?,updated_at=clock_timestamp() WHERE tenant_id=?",actor.reference(),review,tenant);audit(db,tenant,actor,"RESTORE_OPENED",review,"{}");return null;
        });
    }
    private static void closed(Connection db)throws SQLException {try(var s=Learning.q(db,"SELECT closed FROM dzoka.restore_barriers FOR UPDATE");var r=s.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new Failures.Conflict();}}
    private static void audit(Connection db,UUID tenant,VerifiedIdentity actor,String kind,String evidence,String detail)throws SQLException{Learning.write(db,"INSERT INTO dzoka.operational_actions(tenant_id,kind,actor_ref,evidence_sha,detail) VALUES(?,?,?,?,?::jsonb)",tenant,kind,actor.reference(),evidence,detail);}
}
