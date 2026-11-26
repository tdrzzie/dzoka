package org.dzoka.recovery.adapters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.MutationJournal;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.recovery.domain.Recovery;
import org.springframework.stereotype.Component;

@Component
public final class StaffCustody {
    private final TenantTransactions transactions;
    public StaffCustody(TenantTransactions transactions) { this.transactions=transactions; }

    public enum Action { STORE, INCIDENT, REQUEST_TRANSFER, ACCEPT_TRANSFER }
    public record Command(Action action,int expectedGeneration,UUID destination,String recipientRef,
                          UUID requestId,Instant observedAt,String evidenceSha) {}
    public record Event(UUID id,String kind,int expectedGeneration,UUID source,UUID destination,
                        UUID requestId,Instant observedAt,Instant recordedAt) {}
    public record Snapshot(int generation,UUID site,boolean blocked,boolean returned,List<Event> recent) {}
    private record Item(UUID report,UUID site,String custodian,int generation,boolean held,boolean returned) {}

    public Snapshot read(VerifiedIdentity actor,UUID tenant,UUID intake) {
        return transactions.execute(actor,tenant,"custody:operate",db -> {
            var item=item(db,tenant,intake);
            var events=new ArrayList<Event>();
            try(var query=q(db,"""
                    SELECT id,kind,expected_generation,source_site,destination,request_id,observed_at,recorded_at
                    FROM dzoka.staff_custody_events WHERE tenant_id=? AND intake_id=?
                    ORDER BY recorded_at DESC,id DESC LIMIT 100
                    """,tenant,intake);var rows=query.executeQuery()) {
                while(rows.next()) events.add(new Event(rows.getObject(1,UUID.class),rows.getString(2),rows.getInt(3),
                        rows.getObject(4,UUID.class),rows.getObject(5,UUID.class),rows.getObject(6,UUID.class),
                        rows.getTimestamp(7).toInstant(),rows.getTimestamp(8).toInstant()));
            }
            return new Snapshot(item.generation(),item.site(),item.held(),item.returned(),List.copyOf(events));
        });
    }

    public String apply(VerifiedIdentity actor,UUID tenant,UUID intake,UUID id,Command command) {
        validate(command);
        return transactions.execute(actor,tenant,"custody:operate",db -> {
            try(var lock=q(db,"SELECT pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||?::text,17))",tenant)) { lock.execute(); }
            Item item=item(db,tenant,intake);
            try(var lock=q(db,"SELECT dzoka.lock_recovery_report(?,?)",tenant,item.report())) { lock.execute(); }
            String requested=switch(command.action()) {
                case STORE -> "STORED";
                case INCIDENT -> "INCIDENT";
                case REQUEST_TRANSFER -> "TRANSFER_REQUESTED";
                case ACCEPT_TRANSFER -> "TRANSFER_ACCEPTED";
            };
            String recipient=command.recipientRef()==null?actor.reference():command.recipientRef();
            try(var query=q(db,"""
                    SELECT kind,actor_ref,recipient_ref,expected_generation,destination,request_id,observed_at,evidence_sha,command
                    FROM dzoka.staff_custody_events WHERE tenant_id=? AND intake_id=? AND id=?
                    """,tenant,intake,id);var rows=query.executeQuery()) {
                if(rows.next()) {
                    if(!rows.getString(2).equals(actor.reference()) || !rows.getString(3).equals(recipient)
                            || rows.getInt(4)!=command.expectedGeneration() || !command.destination().equals(rows.getObject(5,UUID.class))
                            || !java.util.Objects.equals(command.requestId(),rows.getObject(6,UUID.class))
                            || !command.observedAt().equals(rows.getTimestamp(7).toInstant()) || !command.evidenceSha().equals(rows.getString(8))
                            || !rows.getString(9).equals(command.action().name())) throw new Failures.Conflict();
                    return rows.getString(1);
                }
            }
            if(command.action()==Action.ACCEPT_TRANSFER) checkRequest(db,tenant,intake,actor,command);
            else if(!actor.reference().equals(item.custodian())) throw new Failures.Forbidden();
            if(command.action()==Action.REQUEST_TRANSFER && recipient.equals(actor.reference()))
                throw new IllegalArgumentException("Independent recipient required");
            boolean available;
            try(var query=q(db,"SELECT available FROM dzoka.recovery_report_status(?)",intake);var rows=query.executeQuery()) {
                available=rows.next() && rows.getBoolean(1);
            }
            boolean stale=item.generation()!=command.expectedGeneration() || item.returned()
                    || (command.action()!=Action.INCIDENT && (item.held() || !available));
            String outcome=stale?"OFFLINE_CONFLICT":requested;
            try(var insert=q(db,"""
                    INSERT INTO dzoka.staff_custody_events(tenant_id,intake_id,id,actor_ref,recipient_ref,source_site,
                      destination,expected_generation,observed_at,kind,request_id,evidence_sha,command) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,tenant,intake,id,actor.reference(),recipient,item.site(),command.destination(),command.expectedGeneration(),
                    OffsetDateTime.ofInstant(command.observedAt(),java.time.ZoneOffset.UTC),outcome,command.requestId(),
                    command.evidenceSha(),command.action().name())) {
                insert.executeUpdate();
            }
            if(!stale && command.action()!=Action.REQUEST_TRANSFER) {
                boolean incident=command.action()==Action.INCIDENT;
                try(var update=q(db,"""
                        UPDATE dzoka.recovery_intakes SET generation=generation+1,site_id=?,custodian_ref=?,blocked=blocked OR ?
                        WHERE tenant_id=? AND id=?
                        """,incident?item.site():command.destination(),actor.reference(),incident,tenant,intake)) { update.executeUpdate(); }
                String event=switch(command.action()) {
                    case STORE -> "custody.stored";
                    case INCIDENT -> "custody.incident";
                    default -> "custody.transferred";
                };
                MutationJournal.append(db,tenant,actor,event,item.report(),item.generation()+1L,"report_id");
            }
            return outcome;
        });
    }

    private static void checkRequest(Connection db,UUID tenant,UUID intake,VerifiedIdentity actor,Command command) throws SQLException {
        try(var query=q(db,"""
                SELECT 1 FROM dzoka.staff_custody_events WHERE tenant_id=? AND intake_id=? AND id=?
                  AND kind='TRANSFER_REQUESTED' AND recipient_ref=? AND destination=? AND expected_generation=?
                """,tenant,intake,command.requestId(),actor.reference(),command.destination(),command.expectedGeneration());
            var rows=query.executeQuery()) {
            if(!rows.next()) throw new Failures.NotFound();
        }
    }

    private static Item item(Connection db,UUID tenant,UUID intake) throws SQLException {
        try(var query=q(db,"""
                SELECT report_id,site_id,custodian_ref,generation,blocked,returned FROM dzoka.recovery_intakes
                WHERE tenant_id=? AND id=?
                """,tenant,intake);var rows=query.executeQuery()) {
            if(!rows.next()) throw new Failures.NotFound();
            return new Item(rows.getObject(1,UUID.class),rows.getObject(2,UUID.class),rows.getString(3),
                    rows.getInt(4),rows.getBoolean(5),rows.getBoolean(6));
        }
    }

    private static PreparedStatement q(Connection db,String sql,Object... args) throws SQLException {
        var result=db.prepareStatement(sql);
        try { for(int i=0;i<args.length;i++) result.setObject(i+1,args[i]); return result; }
        catch(SQLException failure) { result.close(); throw failure; }
    }

    private static void validate(Command command) {
        if(command.action()==null || command.expectedGeneration()<1 || command.destination()==null
                || command.observedAt()==null || command.observedAt().getNano()%1000!=0
                || command.observedAt().isAfter(Instant.now().plusSeconds(300)) || !Recovery.digest(command.evidenceSha())
                || (command.action()==Action.ACCEPT_TRANSFER)!=(command.requestId()!=null)
                || (command.action()==Action.REQUEST_TRANSFER)!=(command.recipientRef()!=null)
                || (command.recipientRef()!=null && !Recovery.digest(command.recipientRef()))) throw new IllegalArgumentException("Invalid custody command");
    }
}
