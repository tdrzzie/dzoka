package org.dzoka.edge;

import java.time.Instant;
import java.util.*;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Service;

@Service
public final class RecoveryIntelligence {
    private final TenantTransactions tx;
    public RecoveryIntelligence(TenantTransactions tx) { this.tx=tx; }
    public record Edge(UUID source,String kind,UUID from,UUID to,Instant observed,Instant recorded,String classification) {}
    public record Transition(UUID destination,long observations,Double medianElapsedSeconds) {}
    public record Baseline(String version,String state,Instant cutoff,List<Transition> transitions) {}
    public List<Edge> history(VerifiedIdentity actor,UUID tenant,UUID intake,Instant cutoff) {
        if(cutoff==null || cutoff.isAfter(Instant.now())) throw new IllegalArgumentException("Past cutoff required");
        return tx.execute(actor,tenant,"intelligence:read",db -> {
            var edges=new ArrayList<Edge>();
            try(var query=db.prepareStatement("""
                    SELECT * FROM (
                      SELECT id,kind,source_site,destination,observed_at,recorded_at FROM dzoka.staff_custody_events
                      WHERE intake_id=? AND recorded_at<=? AND observed_at<=?
                        AND EXISTS(SELECT 1 FROM dzoka.recovery_report_status(intake_id) s WHERE s.available)
                      UNION ALL
                      SELECT id,'RETURNED',site_id,site_id,created_at,created_at FROM dzoka.recovery_returns
                      WHERE intake_id=? AND created_at<=?
                        AND EXISTS(SELECT 1 FROM dzoka.recovery_report_status(intake_id) s WHERE s.available)
                    ) observations
                    ORDER BY recorded_at,id LIMIT 200
                    """)) {
                query.setObject(1,intake);query.setTimestamp(2,java.sql.Timestamp.from(cutoff));query.setTimestamp(3,java.sql.Timestamp.from(cutoff));
                query.setObject(4,intake);query.setTimestamp(5,java.sql.Timestamp.from(cutoff));
                try(var row=query.executeQuery()) {
                    while(row.next()) edges.add(new Edge(row.getObject(1,UUID.class),row.getString(2),row.getObject(3,UUID.class),
                            row.getObject(4,UUID.class),row.getTimestamp(5).toInstant(),row.getTimestamp(6).toInstant(),
                            Set.of("STORED","TRANSFER_ACCEPTED","INCIDENT","RETURNED").contains(row.getString(2))?"OBSERVED":"UNCONFIRMED"));
                }
            }
            return List.copyOf(edges);
        });
    }
    public Baseline baseline(VerifiedIdentity actor,UUID tenant,UUID excludedIntake,UUID source,Instant cutoff) {
        if(source==null || excludedIntake==null || cutoff==null || cutoff.isAfter(Instant.now()))
            throw new IllegalArgumentException("Target intake, site and past cutoff required");
        return tx.execute(actor,tenant,"intelligence:read",db -> {
            var transitions=new ArrayList<Transition>();
            try(var query=db.prepareStatement("""
                    WITH transitions AS (
                      SELECT e.intake_id,e.destination,
                        (SELECT max(p.observed_at) FROM dzoka.staff_custody_events p
                         WHERE p.tenant_id=e.tenant_id AND p.intake_id=e.intake_id
                           AND p.kind IN ('STORED','TRANSFER_ACCEPTED') AND p.destination=e.source_site
                           AND p.observed_at<e.observed_at AND p.recorded_at<=e.recorded_at) AS previous,
                        e.observed_at,
                        row_number() OVER (PARTITION BY e.intake_id ORDER BY e.observed_at,e.recorded_at,e.id) AS ordinal
                      FROM dzoka.staff_custody_events e
                      WHERE e.intake_id<>? AND e.source_site=? AND e.kind='TRANSFER_ACCEPTED'
                        AND e.recorded_at<=? AND e.observed_at<=?
                        AND EXISTS(SELECT 1 FROM dzoka.recovery_report_status(e.intake_id) s WHERE s.available)
                    )
                    SELECT destination,count(*) AS support,
                      percentile_cont(0.5) WITHIN GROUP(ORDER BY extract(epoch FROM observed_at-previous)) AS median_elapsed
                    FROM transitions WHERE ordinal=1
                    GROUP BY destination HAVING count(*)>=3 ORDER BY support DESC,destination LIMIT 10
                    """)) {
                query.setObject(1,excludedIntake);query.setObject(2,source);
                query.setTimestamp(3,java.sql.Timestamp.from(cutoff));query.setTimestamp(4,java.sql.Timestamp.from(cutoff));
                try(var row=query.executeQuery()) {
                    while(row.next()) transitions.add(new Transition(row.getObject(1,UUID.class),row.getLong(2),row.getObject(3,Double.class)));
                }
            }
            return new Baseline("distinct-item-frequency-v1",transitions.isEmpty()?"UNAVAILABLE":"DESCRIPTIVE",cutoff,List.copyOf(transitions));
        });
    }
}
