package org.dzoka.channels.adapters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.EvidenceScoring.Disposition;
import org.dzoka.matching.domain.Matching;
import org.dzoka.matching.domain.Matching.Observation;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
final class ChannelMatching {
    private static final int LIMIT=5;
    private static final String SERVICE="channels:service";
    private final TenantTransactions transactions;
    private final ObjectMapper json;

    record Candidate(UUID report,int revision,UUID intake) {}
    record Run(int revision,List<Candidate> candidates) {}

    ChannelMatching(TenantTransactions transactions,ObjectMapper json) {
        this.transactions=transactions;
        this.json=json;
    }

    void publish(VerifiedIdentity actor,UUID tenant,UUID report,int revision,String summary,boolean enabled) {
        if(revision<1 || summary==null || summary.isBlank() || summary.length()>320
                || summary.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid offer summary");
        transactions.execute(actor,tenant,"channels:admin",db -> {
            try(var query=q(db,"""
                    SELECT 1 FROM dzoka.recovery_intakes i JOIN dzoka.reports r
                       ON r.tenant_id=i.tenant_id AND r.id=i.report_id
                    WHERE dzoka.permitted('recovery:manage') AND r.tenant_id=? AND r.id=?
                      AND (NOT ? OR (r.current_revision=? AND r.kind='FOUND' AND NOT i.blocked AND NOT i.returned
                          AND NOT r.legal_hold AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()))
                    """,tenant,report,enabled,revision);var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
            }
            try(var update=q(db,"""
                    INSERT INTO dzoka.channel_match_offers(tenant_id,report_id,revision,summary,enabled) VALUES(?,?,?,?,?)
                    ON CONFLICT(tenant_id,report_id) DO UPDATE SET revision=excluded.revision,summary=excluded.summary,enabled=excluded.enabled
                    """,tenant,report,revision,summary,enabled)) { update.executeUpdate(); }
            return null;
        });
    }

    Run scan(VerifiedIdentity actor,UUID tenant,UUID report) {
        Observation source=transactions.execute(actor,tenant,"matches:read",db -> {
            try(var query=q(db,"""
                    SELECT r.current_revision,v.normalized_text FROM dzoka.reports r JOIN dzoka.report_revisions v
                      ON v.tenant_id=r.tenant_id AND v.report_id=r.id AND v.revision=r.current_revision
                    WHERE dzoka.permitted('reports:read') AND r.tenant_id=? AND r.id=? AND r.kind='LOST'
                      AND r.owner_ref=dzoka.current_actor_ref() AND NOT r.legal_hold
                      AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
                    """,tenant,report);var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                return observation(report,row.getInt(1),row.getString(2));
            }
        });
        return transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            var entries=new java.util.HashMap<UUID,UUID>();
            var pool=new ArrayList<Observation>();
            try(var query=q(db,"""
                    SELECT o.report_id,o.revision,o.summary,dzoka.channel_candidate_intake(o.report_id,o.revision) AS intake
                    FROM dzoka.channel_match_offers o WHERE o.tenant_id=? AND o.enabled
                    ORDER BY o.report_id LIMIT ?
                    """,tenant,Matching.POOL_LIMIT+1);var rows=query.executeQuery()) {
                int count=0;
                while(rows.next()) {
                    if(++count>Matching.POOL_LIMIT) throw new Failures.Conflict();
                    UUID intake=rows.getObject(4,UUID.class);
                    if(intake==null) continue;
                    UUID known=rows.getObject(1,UUID.class);
                    entries.put(known,intake);
                    pool.add(observation(known,rows.getInt(2),rows.getString(3)));
                }
            }
            var candidates=Matching.search(source,pool,null,LIMIT).candidates().stream()
                    .filter(item -> item.score().disposition()==Disposition.COMPARABLE)
                    .map(item -> new Candidate(item.report(),item.revision(),entries.get(item.report()))).toList();
            return new Run(source.revision(),candidates);
        });
    }

    Run record(VerifiedIdentity actor,UUID tenant,UUID contact,UUID report,UUID id,String quoteKey) {
        Run current=scan(actor,tenant,report);
        return transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            try(var watch=q(db,"""
                    INSERT INTO dzoka.channel_match_watches(tenant_id,contact_id,report_id,original_event,due_at)
                    VALUES(?,?,?,?,clock_timestamp()+interval '5 minutes') ON CONFLICT DO NOTHING
                    """,tenant,contact,report,quoteKey)) { watch.executeUpdate(); }
            try(var write=q(db,"""
                    INSERT INTO dzoka.channel_match_runs(tenant_id,contact_id,report_id,id,revision,candidates)
                    VALUES(?,?,?,?,?,?::jsonb) ON CONFLICT DO NOTHING
                    """,tenant,contact,report,id,current.revision(),json.writeValueAsString(current.candidates()))) {
                write.executeUpdate();
            }
            return read(db,tenant,contact,report,id);
        });
    }

    Candidate choose(VerifiedIdentity actor,UUID tenant,UUID contact,UUID report,UUID id,int index) {
        Run previous=transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> read(db,tenant,contact,report,id));
        Run current=scan(actor,tenant,report);
        if(index>=previous.candidates().size() || previous.revision()!=current.revision()) throw new Failures.NotFound();
        Candidate candidate=previous.candidates().get(index);
        if(!current.candidates().contains(candidate)) throw new Failures.NotFound();
        return candidate;
    }

    private Run read(Connection db,UUID tenant,UUID contact,UUID report,UUID id) throws SQLException {
        try(var query=q(db,"""
                SELECT revision,candidates::text FROM dzoka.channel_match_runs
                WHERE tenant_id=? AND contact_id=? AND report_id=? AND id=?
                  AND created_at>statement_timestamp()-interval '24 hours'
                """,tenant,contact,report,id);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
            return new Run(row.getInt(1),List.of(json.readValue(row.getString(2),Candidate[].class)));
        }
    }

    private static Observation observation(UUID report,int revision,String text) {
        return new Observation(report,revision,text,null,null,null,null,null);
    }

    private static PreparedStatement q(Connection db,String sql,Object... values) throws SQLException {
        var query=db.prepareStatement(sql);
        for(int i=0;i<values.length;i++) query.setObject(i+1,values[i]);
        return query;
    }
}
