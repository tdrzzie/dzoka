package org.dzoka.logistics;

import java.sql.*;
import java.util.*;
import org.dzoka.fulfillment.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public final class Logistics {
    private final TenantTransactions tx;
    private final CourierProvider courier;
    private final ObjectMapper json;
    public Logistics(TenantTransactions tx,CourierProvider courier,ObjectMapper json) { this.tx=tx;this.courier=courier;this.json=json; }
    public record Quote(UUID id,String currency,long minorUnits) {}
    public record Request(UUID intake,UUID claim,UUID quote,String deliveryReference,String consentSha) {}
    public record Shipment(UUID id,UUID intake,UUID claim,String state,boolean dispatched,boolean ownerConfirmed,boolean consentWithdrawn) {}
    public record Tracking(String status,String evidenceSha,java.time.Instant observedAt) {}
    private record Stored(Shipment view,String claimant,String delivery,String consent,String quote,int generation,int policy) {}
    public Quote quote(VerifiedIdentity actor,UUID tenant,String delivery) {
        Sql.opaque(delivery);
        tx.execute(actor,tenant,"logistics:use",db -> {
            Sql.lock(db,tenant);
            if(Sql.write(db,"INSERT INTO dzoka.courier_quote_budgets(tenant_id,actor_ref,bucket,used) VALUES(?,?,date_trunc('hour',clock_timestamp()),1) ON CONFLICT(tenant_id,actor_ref,bucket) DO UPDATE SET used=courier_quote_budgets.used+1 WHERE courier_quote_budgets.used<30",tenant,actor.reference())!=1)throw new Failures.Conflict();
            return null;
        });
        var value=courier.quote(tenant,delivery);
        return tx.execute(actor,tenant,"logistics:use",db -> {
            UUID id=UUID.randomUUID();
            Sql.write(db,"INSERT INTO dzoka.courier_quotes(tenant_id,id,actor_ref,delivery_ref,provider_ref,currency,minor_units) VALUES(?,?,?,?,?,?,?)",
                    tenant,id,actor.reference(),delivery,value.reference(),value.currency(),value.minorUnits());
            return new Quote(id,value.currency(),value.minorUnits());
        });
    }
    public Shipment request(VerifiedIdentity actor,UUID tenant,UUID id,Request request) {
        if(request==null || request.intake()==null || request.claim()==null || request.quote()==null) throw new IllegalArgumentException();
        Sql.opaque(request.deliveryReference());Sql.sha(request.consentSha());
        return tx.execute(actor,tenant,"logistics:use",db -> {
            Sql.lock(db,tenant);
            Stored prior=stored(db,id);
            if(prior!=null) {
                if(!prior.claimant().equals(actor.reference()) || !prior.view().intake().equals(request.intake()) || !prior.view().claim().equals(request.claim())
                        || !prior.delivery().equals(request.deliveryReference()) || !prior.consent().equals(request.consentSha())
                        || !prior.quote().equals(request.quote().toString())) throw new Failures.Conflict();
                return prior.view();
            }
            var context=RecoveryContext.read(db,request.intake(),request.claim());
            if(!context.owner().equals(actor.reference()) || context.blocked() || context.returned() || !context.available()) throw new Failures.Conflict();
            try(var query=Sql.q(db,"SELECT currency,minor_units FROM dzoka.courier_quotes WHERE id=? AND actor_ref=? AND delivery_ref=? AND expires_at>clock_timestamp()",
                    request.quote(),actor.reference(),request.deliveryReference());var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.Conflict();
                Sql.write(db,"INSERT INTO dzoka.shipments(tenant_id,id,intake_id,claim_id,claimant_ref,delivery_ref,consent_sha,quote_ref,currency,quote_minor) VALUES(?,?,?,?,?,?,?,?,?,?)",
                        tenant,id,request.intake(),request.claim(),actor.reference(),request.deliveryReference(),request.consentSha(),request.quote().toString(),row.getString(1),row.getLong(2));
            }
            OperationJournal.event(db,tenant,actor,id,"SHIPMENT_REQUESTED",request.consentSha());
            return stored(db,id).view();
        });
    }
    public Shipment read(VerifiedIdentity actor,UUID tenant,UUID id) {
        return tx.execute(actor,tenant,"logistics:use",db -> required(db,id).view());
    }
    public Shipment book(VerifiedIdentity actor,UUID tenant,UUID id) {
        return tx.execute(actor,tenant,"logistics:manage",db -> {
            Sql.lock(db,tenant);var s=required(db,id);
            if(Set.of("BOOKING","BOOKED").contains(s.view().state())) return s.view();
            if(!s.view().state().equals("REQUESTED") || s.view().consentWithdrawn()) throw new Failures.Conflict();
            var c=RecoveryContext.read(db,s.view().intake(),s.view().claim());c.requireRelease();
            String quote;
            try(var query=Sql.q(db,"SELECT provider_ref FROM dzoka.courier_quotes WHERE id=? AND expires_at>clock_timestamp()",UUID.fromString(s.quote()));var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.Conflict();quote=row.getString(1);
            }
            Sql.write(db,"UPDATE dzoka.shipments SET state='BOOKING',generation=?,policy_version=? WHERE id=?",c.generation(),c.policy(),id);
            OperationJournal.enqueue(db,tenant,actor,id,"COURIER_BOOK",json.writeValueAsString(new TreeMap<>(Map.of("shipment",id.toString(),"pickupIntakeReference",s.view().intake().toString(),"deliveryReference",s.delivery(),"quote",quote))));
            return required(db,id).view();
        });
    }
    public Shipment cancel(VerifiedIdentity actor,UUID tenant,UUID id) {
        return tx.execute(actor,tenant,"logistics:manage",db -> {
            Sql.lock(db,tenant);var s=required(db,id);
            if(s.view().dispatched()) throw new Failures.Conflict();
            if(Set.of("CANCELLED","CANCEL_PENDING").contains(s.view().state())) return s.view();
            if(s.view().state().equals("REQUESTED")) Sql.write(db,"UPDATE dzoka.shipments SET state='CANCELLED' WHERE id=?",id);
            else {
                if(!s.view().state().equals("BOOKED")) throw new Failures.Conflict();
                Sql.write(db,"UPDATE dzoka.shipments SET state='CANCEL_PENDING' WHERE id=?",id);
                OperationJournal.enqueue(db,tenant,actor,id,"COURIER_CANCEL",json.writeValueAsString(Map.of("shipment",id.toString())));
            }
            return required(db,id).view();
        });
    }
    public Shipment handoff(VerifiedIdentity actor,UUID tenant,UUID id,String courierRef,String evidence) {
        Sql.sha(courierRef);Sql.sha(evidence);
        return tx.execute(actor,tenant,"logistics:manage",db -> {
            Sql.lock(db,tenant);var s=required(db,id);
            if(!s.view().dispatched()) {
                var c=RecoveryContext.read(db,s.view().intake(),s.view().claim());c.requireRelease();
                if(s.view().consentWithdrawn() || !s.view().state().equals("BOOKED") || c.generation()!=s.generation() || c.policy()!=s.policy() || !c.custodian().equals(actor.reference())) throw new Failures.Conflict();
            } else {
                try(var query=Sql.q(db,"SELECT 1 FROM dzoka.courier_custody WHERE shipment_id=? AND courier_ref=? AND dispatch_sha=?",id,courierRef,evidence);var row=query.executeQuery()) {
                    if(!row.next()) throw new Failures.Conflict();return s.view();
                }
            }
            try(var query=Sql.q(db,"SELECT dzoka.courier_handoff(?,?,?)",id,courierRef,evidence)) { query.execute(); }
            projectTracking(db,id);
            OperationJournal.event(db,tenant,actor,id,"COURIER_HANDOFF",evidence);
            return required(db,id).view();
        });
    }
    public Shipment confirm(VerifiedIdentity actor,UUID tenant,UUID id,String evidence) {
        Sql.sha(evidence);
        return tx.execute(actor,tenant,"logistics:use",db -> {
            Sql.lock(db,tenant);var s=required(db,id);
            if(!s.claimant().equals(actor.reference())) throw new Failures.NotFound();
            if(s.view().ownerConfirmed()) {
                try(var query=Sql.q(db,"SELECT 1 FROM dzoka.courier_custody WHERE shipment_id=? AND receipt_sha=?",id,evidence);var row=query.executeQuery()) {
                    if(!row.next()) throw new Failures.Conflict();return s.view();
                }
            }
            var c=RecoveryContext.read(db,s.view().intake(),s.view().claim());
            if(!s.view().state().equals("DELIVERED") || c.blocked() || !c.available() || c.returned() || !c.entitled()) throw new Failures.Conflict();
            long reportVersion;
            try(var query=Sql.q(db,"SELECT report_version FROM dzoka.courier_custody WHERE shipment_id=?",id);var row=query.executeQuery()) {
                if(!row.next())throw new Failures.Conflict();reportVersion=row.getLong(1);
            }
            try(var query=Sql.q(db,"SELECT generation,policy_version,report_version FROM dzoka.courier_receipt_reviews WHERE shipment_id=? ORDER BY created_at DESC,id DESC LIMIT 1",id);var row=query.executeQuery()) {
                int generation=s.generation()+1,policy=s.policy();
                if(row.next()){generation=row.getInt(1);policy=row.getInt(2);reportVersion=row.getLong(3);}
                if(generation!=c.generation() || policy!=c.policy() || reportVersion!=c.reportVersion())throw new Failures.Conflict();
            }
            try(var query=Sql.q(db,"SELECT dzoka.courier_receipt(?,?)",id,evidence)) { query.execute(); }
            OperationJournal.event(db,tenant,actor,id,"OWNER_RECEIPT",evidence);
            return required(db,id).view();
        });
    }
    public void validateOperation(Connection db,UUID id,String kind) throws SQLException {
        var s=required(db,id);
        if(kind.equals("COURIER_BOOK")) {
            var c=RecoveryContext.read(db,s.view().intake(),s.view().claim());c.requireRelease();
            if(s.view().consentWithdrawn() || !s.view().state().equals("BOOKING") || c.generation()!=s.generation() || c.policy()!=s.policy()) throw new Failures.Conflict();
        } else if(s.view().dispatched() || !s.view().state().equals("CANCEL_PENDING")) throw new Failures.Conflict();
    }
    public void complete(Connection db,UUID id,String kind,boolean confirmed) throws SQLException {
        Sql.write(db,"UPDATE dzoka.shipments SET state=? WHERE id=?",kind.equals("COURIER_BOOK")?(confirmed?"BOOKED":"FAILED"):(confirmed?"CANCELLED":"BOOKED"),id);
    }
    public void withdrawConsent(VerifiedIdentity actor,UUID tenant,UUID id,String proof) {
        Sql.sha(proof);
        tx.execute(actor,tenant,"logistics:use",db -> {
            Sql.lock(db,tenant);var s=required(db,id);
            if(!actor.reference().equals(s.claimant()))throw new Failures.NotFound();
            if(!s.view().consentWithdrawn()) {
                Sql.write(db,"UPDATE dzoka.shipments SET consent_withdrawn=true WHERE id=?",id);
                OperationJournal.event(db,tenant,actor,id,"DELIVERY_CONSENT_WITHDRAWN",proof);
            }
            return null;
        });
    }
    public List<Tracking> tracking(VerifiedIdentity actor,UUID tenant,UUID id) {
        return tx.execute(actor,tenant,"logistics:use",db -> {
            required(db,id);var events=new ArrayList<Tracking>();
            try(var query=Sql.q(db,"SELECT status,evidence_sha,observed_at FROM dzoka.shipment_tracking WHERE shipment_id=? ORDER BY observed_at,event_id LIMIT 100",id);var row=query.executeQuery()) {
                while(row.next())events.add(new Tracking(row.getString(1),row.getString(2),row.getTimestamp(3).toInstant()));
            }
            return List.copyOf(events);
        });
    }
    public void track(Connection db,UUID tenant,UUID event,UUID id,String status,String proof,java.time.Instant observed) throws SQLException {
        if(!Set.of("IN_TRANSIT","DELIVERED","LOST","RETURNED_TO_SENDER").contains(status))throw new IllegalArgumentException();
        Sql.write(db,"INSERT INTO dzoka.shipment_tracking(tenant_id,event_id,shipment_id,status,evidence_sha,observed_at) VALUES(?,?,?,?,?,?)",tenant,event,id,status,proof,Timestamp.from(observed));
        projectTracking(db,id);
    }
    private void projectTracking(Connection db,UUID id) throws SQLException {
        var s=required(db,id);
        if(!s.view().dispatched() || s.view().ownerConfirmed() || s.view().state().equals("CANCELLED")) return;
        try(var query=Sql.q(db,"SELECT status FROM dzoka.shipment_tracking WHERE shipment_id=? ORDER BY (status<>'IN_TRANSIT') DESC,observed_at DESC,recorded_at DESC,event_id DESC LIMIT 1",id);var row=query.executeQuery()) {
            if(row.next())Sql.write(db,"UPDATE dzoka.shipments SET state=? WHERE id=?",row.getString(1),id);
        }
    }
    public Shipment staffReceipt(VerifiedIdentity actor,UUID tenant,UUID id,String evidence) {
        Sql.sha(evidence);
        return tx.execute(actor,tenant,"logistics:manage",db -> {
            Sql.lock(db,tenant);var s=required(db,id);
            try(var query=Sql.q(db,"SELECT previous_custodian,closed,receipt_sha,receipt_kind FROM dzoka.courier_custody WHERE shipment_id=?",id);var row=query.executeQuery()) {
                if(!row.next() || !actor.reference().equals(row.getString(1)))throw new Failures.Forbidden();
                if(row.getBoolean(2)) {
                    if(!evidence.equals(row.getString(3)) || !"STAFF_RETURN".equals(row.getString(4)))throw new Failures.Conflict();return s.view();
                }
            }
            if(!Set.of("LOST","RETURNED_TO_SENDER").contains(s.view().state()))throw new Failures.Conflict();
            try(var query=Sql.q(db,"SELECT dzoka.courier_staff_receipt(?,?)",id,evidence)){query.execute();}
            OperationJournal.event(db,tenant,actor,id,"STAFF_COURIER_RECEIPT",evidence);return required(db,id).view();
        });
    }
    public void reviewReceipt(VerifiedIdentity actor,UUID tenant,UUID id,UUID review,String evidence) {
        Sql.sha(evidence);
        tx.execute(actor,tenant,"recovery:review",db -> {
            Sql.lock(db,tenant);
            try(var query=Sql.q(db,"SELECT dzoka.permitted('logistics:manage')");var row=query.executeQuery()){row.next();if(!row.getBoolean(1))throw new Failures.Forbidden();}
            var s=required(db,id);var c=RecoveryContext.read(db,s.view().intake(),s.view().claim());
            if(!s.view().state().equals("DELIVERED") || c.blocked() || c.returned() || !c.available() || !c.entitled())throw new Failures.Conflict();
            try(var query=Sql.q(db,"SELECT previous_custodian FROM dzoka.courier_custody WHERE shipment_id=? AND NOT closed",id);var row=query.executeQuery()) {
                if(!row.next())throw new Failures.NotFound();
                if(actor.reference().equals(c.owner()) || actor.reference().equals(c.custodian()) || actor.reference().equals(row.getString(1)))throw new Failures.Forbidden();
            }
            try(var query=Sql.q(db,"SELECT generation,policy_version,actor_ref,evidence_sha,shipment_id,report_version FROM dzoka.courier_receipt_reviews WHERE id=?",review);var row=query.executeQuery()) {
                if(row.next()) {
                    if(row.getInt(1)!=c.generation() || row.getInt(2)!=c.policy() || !actor.reference().equals(row.getString(3)) || !evidence.equals(row.getString(4)) || !id.equals(row.getObject(5,UUID.class)) || row.getLong(6)!=c.reportVersion())throw new Failures.Conflict();return null;
                }
            }
            Sql.write(db,"INSERT INTO dzoka.courier_receipt_reviews(tenant_id,id,shipment_id,generation,policy_version,report_version,actor_ref,evidence_sha) VALUES(?,?,?,?,?,?,?,?)",tenant,review,id,c.generation(),c.policy(),c.reportVersion(),actor.reference(),evidence);return null;
        });
    }
    private static Stored required(Connection db,UUID id) throws SQLException { var value=stored(db,id);if(value==null)throw new Failures.NotFound();return value; }
    private static Stored stored(Connection db,UUID id) throws SQLException {
        try(var query=Sql.q(db,"SELECT * FROM dzoka.shipments WHERE id=?",id);var row=query.executeQuery()) {
            if(!row.next())return null;
            return new Stored(new Shipment(id,row.getObject("intake_id",UUID.class),row.getObject("claim_id",UUID.class),row.getString("state"),row.getBoolean("dispatched"),row.getBoolean("owner_confirmed"),row.getBoolean("consent_withdrawn")),
                    row.getString("claimant_ref"),row.getString("delivery_ref"),row.getString("consent_sha"),row.getString("quote_ref"),row.getInt("generation"),row.getInt("policy_version"));
        }
    }
}
