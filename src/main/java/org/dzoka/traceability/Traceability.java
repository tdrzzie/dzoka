package org.dzoka.traceability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.traceability.EpcisProjection.NativeKind;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public final class Traceability {
    private static final String MANAGE="trace:manage", DISPATCH="trace:dispatch";
    private final TenantTransactions tx;
    private final TraceabilityProvider provider;
    private final ObjectMapper json;

    public record View(UUID id,int mapping,UUID predecessor,String state,String receipt,JsonNode document) {}
    private record Lease(UUID id,UUID token,String state,String receipt,int attempt,byte[] document) {}
    private record NativeObservation(UUID intake,UUID event,String kind) {}

    public Traceability(TenantTransactions tx,TraceabilityProvider provider,ObjectMapper json) {
        this.tx=tx;this.provider=provider;this.json=json;
    }

    public void authorization(VerifiedIdentity actor,UUID tenant,UUID intake,boolean enabled) {
        tx.execute(actor,tenant,MANAGE,db -> {
            lock(db,tenant);
            if(enabled && !available(db,intake)) throw new Failures.NotFound();
            if(!enabled) {
                if(write(db,"UPDATE dzoka.traceability_objects SET enabled=false WHERE intake_id=?",intake)==0)
                    throw new Failures.NotFound();
                write(db,"UPDATE dzoka.epcis_projections SET state='CANCELLED' WHERE intake_id=? AND state='QUEUED'",intake);
            } else write(db,"""
                    INSERT INTO dzoka.traceability_objects(tenant_id,intake_id,enabled) VALUES(?,?,true)
                    ON CONFLICT(tenant_id,intake_id) DO UPDATE SET enabled=true,
                        enabled_at=CASE WHEN traceability_objects.enabled THEN traceability_objects.enabled_at ELSE clock_timestamp() END
                    """,tenant,intake);
            return null;
        });
    }

    public View projectNext(VerifiedIdentity actor,UUID tenant) {
        var next=tx.execute(actor,tenant,MANAGE,db -> {
            try(var query=q(db,"""
                    WITH observations AS (
                        SELECT e.tenant_id,e.intake_id,e.id,e.kind,e.recorded_at
                        FROM dzoka.staff_custody_events e JOIN dzoka.traceability_objects o
                          ON o.tenant_id=e.tenant_id AND o.intake_id=e.intake_id AND o.enabled AND e.recorded_at>=o.enabled_at
                        WHERE e.kind IN ('STORED','TRANSFER_ACCEPTED')
                        UNION ALL
                        SELECT e.tenant_id,e.intake_id,e.id,'RETURNED',e.created_at
                        FROM dzoka.recovery_returns e JOIN dzoka.traceability_objects o
                          ON o.tenant_id=e.tenant_id AND o.intake_id=e.intake_id AND o.enabled AND e.created_at>=o.enabled_at
                    )
                    SELECT n.intake_id,n.id,n.kind FROM observations n
                    WHERE EXISTS(SELECT 1 FROM dzoka.recovery_report_status(n.intake_id) s WHERE s.available)
                      AND NOT EXISTS(SELECT 1 FROM dzoka.epcis_projections p
                        WHERE p.tenant_id=n.tenant_id AND p.intake_id=n.intake_id AND p.native_id=n.id AND p.native_kind=n.kind)
                    ORDER BY n.recorded_at,n.intake_id,n.id,n.kind LIMIT 1
                    """);var row=query.executeQuery()) {
                return row.next()?new NativeObservation(row.getObject(1,UUID.class),row.getObject(2,UUID.class),row.getString(3)):null;
            }
        });
        return next==null?null:project(actor,tenant,next.intake(),next.event(),next.kind(),2,null);
    }

    public View project(VerifiedIdentity actor,UUID tenant,UUID intake,UUID nativeId,String kindName,int mapping,UUID predecessor) {
        if(kindName==null || (mapping!=1 && mapping!=2) || (mapping==1 && predecessor!=null))
            throw new IllegalArgumentException("Unsupported EPCIS mapping");
        NativeKind kind=NativeKind.valueOf(kindName);
        return tx.execute(actor,tenant,MANAGE,db -> {
            lock(db,tenant);
            try(var query=q(db,"""
                    SELECT id,mapping,predecessor,state,receipt,document FROM dzoka.epcis_projections
                    WHERE intake_id=? AND native_id=? AND native_kind=? AND mapping=?
                    """,intake,nativeId,kind.name(),mapping);var row=query.executeQuery()) {
                if(row.next()) {
                    var prior=view(row);
                    if(!java.util.Objects.equals(predecessor,prior.predecessor())) throw new Failures.Conflict();
                    return prior;
                }
            }
            if(!available(db,intake)) throw new Failures.Conflict();
            UUID object;
            try(var query=q(db,"SELECT object_id FROM dzoka.traceability_objects WHERE intake_id=? AND enabled",intake);
                var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.Forbidden();
                object=row.getObject(1,UUID.class);
            }
            var occurred=source(db,intake,nativeId,kind);
            JsonNode previous=null;
            try(var query=q(db,"""
                    SELECT id,mapping,predecessor,state,receipt,document FROM dzoka.epcis_projections
                    WHERE intake_id=? AND native_id=? AND native_kind=?
                    ORDER BY mapping DESC LIMIT 1
                    """,intake,nativeId,kind.name());var row=query.executeQuery()) {
                if(row.next()) {
                    var current=view(row);
                    if(current.mapping()>=mapping || !current.id().equals(predecessor) || !"COMPLETE".equals(current.state()))
                        throw new Failures.Conflict();
                    previous=current.document();
                } else if(predecessor!=null) throw new Failures.Conflict();
            }
            var id=UUID.randomUUID();
            var created=Instant.now().truncatedTo(ChronoUnit.MICROS);
            var document=EpcisProjection.document(json,object,id,occurred,kind,mapping,created,previous);
            write(db,"""
                    INSERT INTO dzoka.epcis_projections(tenant_id,intake_id,native_id,native_kind,mapping,id,predecessor,document,created_at)
                    VALUES(?,?,?,?,?,?,?,?::jsonb,?)
                    """,tenant,intake,nativeId,kind.name(),mapping,id,predecessor,document.toString(),created.atOffset(java.time.ZoneOffset.UTC));
            return new View(id,mapping,predecessor,"QUEUED",null,document);
        });
    }

    public List<View> query(VerifiedIdentity actor,UUID tenant,UUID intake) {
        return tx.execute(actor,tenant,"trace:read",db -> {
            var results=new ArrayList<View>();
            try(var query=q(db,"""
                    SELECT id,mapping,predecessor,state,receipt,document FROM dzoka.epcis_projections
                    WHERE intake_id=? ORDER BY created_at DESC,id LIMIT 50
                    """,intake);var rows=query.executeQuery()) {
                while(rows.next()) results.add(view(rows));
            }
            return List.copyOf(results);
        });
    }

    public View get(VerifiedIdentity actor,UUID tenant,UUID id) {
        return tx.execute(actor,tenant,"trace:read",db -> required(db,id));
    }

    public String dispatch(VerifiedIdentity actor,UUID tenant) {
        tx.execute(actor,tenant,DISPATCH,db -> null);
        if(!provider.enabled()) throw new Failures.Unavailable(new IllegalStateException("EPCIS repository disabled"));
        var lease=tx.execute(actor,tenant,DISPATCH,db -> claim(db,tenant));
        if(lease==null) return "IDLE";
        var result="SUBMITTED".equals(lease.state())?provider.status(lease.receipt()):provider.capture(lease.id(),lease.document());
        return tx.execute(actor,tenant,DISPATCH,db -> {
            lock(db,tenant);
            String state=switch(result.state()) {
                case RETRY -> "SUBMITTED".equals(lease.state())?"SUBMITTED":lease.attempt()<3?"QUEUED":"REJECTED";
                case RUNNING -> "SUBMITTED";
                default -> result.state().name();
            };
            String receipt=result.receipt();
            if(receipt!=null && receiptAttached(db,lease.id(),receipt)) {
                state="UNKNOWN";
                receipt=null;
            }
            write(db,"""
                    UPDATE dzoka.epcis_projections SET state=?,receipt=coalesce(?,receipt),
                      lease=NULL,lease_until=NULL,ready_at=clock_timestamp()+interval '1 minute'
                    WHERE id=? AND lease=?
                    """,state,receipt,lease.id(),lease.token());
            return required(db,lease.id()).state();
        });
    }

    public void reconcile(VerifiedIdentity actor,UUID tenant,UUID id,String receipt,String evidenceSha) {
        if(!provider.enabled() || !EpcisHttp.validReceipt(receipt) || evidenceSha==null || !evidenceSha.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Configured repository receipt and evidence digest required");
        tx.execute(actor,tenant,MANAGE,db -> null);
        tx.execute(actor,tenant,DISPATCH,db -> {
            lock(db,tenant);
            if(receiptAttached(db,id,receipt)) throw new Failures.Conflict();
            if(write(db,"""
                    UPDATE dzoka.epcis_projections SET state='SUBMITTED',receipt=?,reconciliation_sha=?,
                      ready_at=clock_timestamp(),lease=NULL,lease_until=NULL
                    WHERE id=? AND state='UNKNOWN' AND repository=? AND receipt IS NULL
                    """,receipt,evidenceSha,id,provider.repository())==0) throw new Failures.Conflict();
            return null;
        });
    }

    private boolean receiptAttached(Connection db,UUID id,String receipt) throws SQLException {
        try(var query=q(db,"SELECT 1 FROM dzoka.epcis_projections WHERE id<>? AND repository=? AND receipt=?",
                id,provider.repository(),receipt);var row=query.executeQuery()) {
            return row.next();
        }
    }

    private Lease claim(Connection db,UUID tenant) throws SQLException {
        lock(db,tenant);
        write(db,"""
                UPDATE dzoka.epcis_projections SET state='UNKNOWN',lease=NULL,lease_until=NULL
                WHERE (state='SENDING' AND lease_until<=clock_timestamp())
                OR (state='SUBMITTED' AND created_at<clock_timestamp()-interval '1 day')
                OR (state IN ('QUEUED','SUBMITTED') AND repository IS NOT NULL AND repository<>?)
                """,provider.repository());
        write(db,"""
                UPDATE dzoka.epcis_projections p SET state='CANCELLED'
                WHERE p.state='QUEUED' AND (NOT EXISTS(SELECT 1 FROM dzoka.traceability_objects o
                  WHERE o.tenant_id=p.tenant_id AND o.intake_id=p.intake_id AND o.enabled)
                OR NOT EXISTS(SELECT 1 FROM dzoka.recovery_report_status(p.intake_id) s WHERE s.available))
                """);
        try(var query=q(db,"""
                SELECT id,state,receipt,attempts,document::text FROM dzoka.epcis_projections
                WHERE state IN ('QUEUED','SUBMITTED') AND ready_at<=clock_timestamp()
                  AND (lease_until IS NULL OR lease_until<=clock_timestamp())
                ORDER BY ready_at,id LIMIT 1
                """);var row=query.executeQuery()) {
            if(!row.next()) return null;
            UUID id=row.getObject(1,UUID.class),token=UUID.randomUUID();
            String state=row.getString(2);
            boolean capture="QUEUED".equals(state);
            int attempt=row.getInt(4)+(capture?1:0);
            write(db,"""
                    UPDATE dzoka.epcis_projections SET state=?,lease=?,lease_until=clock_timestamp()+interval '2 minutes',
                      attempts=?,repository=? WHERE id=?
                    """,capture?"SENDING":state,token,attempt,provider.repository(),id);
            return new Lease(id,token,state,row.getString(3),attempt,row.getString(5).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static Instant source(Connection db,UUID intake,UUID id,NativeKind kind) throws SQLException {
        String statement=kind==NativeKind.RETURNED
                ?"SELECT created_at FROM dzoka.recovery_returns WHERE intake_id=? AND id=?"
                :"SELECT observed_at FROM dzoka.staff_custody_events WHERE intake_id=? AND id=? AND kind=?";
        try(var query=kind==NativeKind.RETURNED?q(db,statement,intake,id):q(db,statement,intake,id,kind.name());
            var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
            return row.getObject(1,OffsetDateTime.class).toInstant();
        }
    }

    private static boolean available(Connection db,UUID intake) throws SQLException {
        try(var query=q(db,"SELECT 1 FROM dzoka.recovery_report_status(?) s WHERE s.available",intake);
            var row=query.executeQuery()) { return row.next(); }
    }

    private View required(Connection db,UUID id) throws SQLException {
        try(var query=q(db,"""
                SELECT id,mapping,predecessor,state,receipt,document FROM dzoka.epcis_projections WHERE id=?
                """,id);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
            return view(row);
        }
    }

    private View view(java.sql.ResultSet row) throws SQLException {
        return new View(row.getObject(1,UUID.class),row.getInt(2),row.getObject(3,UUID.class),row.getString(4),
                row.getString(5),json.readTree(row.getString(6)));
    }

    private static void lock(Connection db,UUID tenant) throws SQLException {
        try(var query=q(db,"SELECT pg_advisory_xact_lock(hashtextextended(?,21))",tenant.toString())) { query.execute(); }
    }
    private static PreparedStatement q(Connection db,String sql,Object... args) throws SQLException {
        var statement=db.prepareStatement(sql);
        for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]);
        return statement;
    }
    private static int write(Connection db,String sql,Object... args) throws SQLException {
        try(var statement=q(db,sql,args)) { return statement.executeUpdate(); }
    }
}
