package org.dzoka.fulfillment;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.logistics.Logistics;
import org.dzoka.rewards.Rewards;
import org.springframework.stereotype.Service;

@Service
public final class FulfillmentOperations {
    private final TenantTransactions tx;
    private final FulfillmentGateway gateway;
    private final Logistics logistics;
    private final Rewards rewards;
    public FulfillmentOperations(TenantTransactions tx,FulfillmentGateway gateway,Logistics logistics,Rewards rewards) {
        this.tx=tx;this.gateway=gateway;this.logistics=logistics;this.rewards=rewards;
    }
    private record Work(UUID id,UUID entity,String kind,String payload,UUID lease,boolean reconcile) {}
    public boolean dispatch(VerifiedIdentity actor,UUID tenant) {
        if(!gateway.enabled()) {
            tx.execute(actor,tenant,"fulfillment:process",db -> null);
            throw new Failures.Unavailable(new IllegalStateException("Fulfillment gateway disabled"));
        }
        var work=tx.execute(actor,tenant,"fulfillment:process",db -> {
            Sql.lock(db,tenant);
            Sql.write(db,"DELETE FROM dzoka.courier_quote_budgets WHERE (actor_ref,bucket) IN(SELECT actor_ref,bucket FROM dzoka.courier_quote_budgets WHERE bucket<clock_timestamp()-interval '2 days' ORDER BY bucket LIMIT 100)");
            try(var query=Sql.q(db,"SELECT * FROM dzoka.fulfillment_operations WHERE state IN ('READY','BLOCKED','SENDING','UNKNOWN') AND next_at<=clock_timestamp() AND (lease_until IS NULL OR lease_until<=clock_timestamp()) ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED");var row=query.executeQuery()) {
                if(!row.next())return null;
                UUID id=row.getObject("id",UUID.class),entity=row.getObject("entity_id",UUID.class),lease=UUID.randomUUID();
                String kind=row.getString("kind");
                boolean reconcile=!Set.of("READY","BLOCKED").contains(row.getString("state"));
                if(!reconcile) {
                    try { validate(db,entity,kind); }
                    catch(Failures.Conflict | Failures.NotFound stale) {
                        if(kind.startsWith("PAYMENT_")) {
                            Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state='BLOCKED',next_at=clock_timestamp()+interval '30 seconds' WHERE id=?",id);
                            return new Work(id,entity,kind,null,null,false);
                        }
                        Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state='CANCELLED' WHERE id=?",id);
                        complete(db,tenant,id,entity,kind,false);
                        return new Work(id,entity,kind,null,null,false);
                    }
                }
                Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state='SENDING',lease=?,worker=?,lease_until=clock_timestamp()+interval '30 seconds',attempt=attempt+1 WHERE id=?",lease,actor.reference(),id);
                return new Work(id,entity,kind,row.getString("payload"),lease,reconcile);
            }
        });
        if(work==null)return false;
        if(work.lease()==null)return true;
        var result=work.reconcile()?gateway.reconcile(tenant,work.id()):gateway.send(tenant,work.id(),work.kind(),work.payload());
        tx.execute(actor,tenant,"fulfillment:process",db -> {
            Sql.lock(db,tenant);
            try(var query=Sql.q(db,"SELECT state,lease,worker FROM dzoka.fulfillment_operations WHERE id=? FOR UPDATE",work.id());var row=query.executeQuery()) {
                if(!row.next() || !Objects.equals(work.lease(),row.getObject(2,UUID.class)) || !actor.reference().equals(row.getString(3))) return null;
                if(Set.of("CONFIRMED","REJECTED","CANCELLED").contains(row.getString(1))) return null;
                if(result.state()==FulfillmentGateway.State.NOT_FOUND && work.reconcile()) {
                    Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state='READY',lease=NULL,lease_until=NULL,next_at=clock_timestamp()+interval '2 seconds' WHERE id=?",work.id());
                } else apply(db,tenant,actor,work.id(),work.entity(),work.kind(),result);
            }
            return null;
        });
        return true;
    }
    private void validate(Connection db,UUID entity,String kind) throws SQLException {
        if(kind.startsWith("COURIER_")) logistics.validateOperation(db,entity,kind);else rewards.validateOperation(db,entity,kind);
    }
    private void complete(Connection db,UUID tenant,UUID id,UUID entity,String kind,boolean confirmed) throws SQLException {
        if(kind.startsWith("COURIER_"))logistics.complete(db,entity,kind,confirmed);else rewards.complete(db,tenant,id,entity,kind,confirmed);
    }
    private void apply(Connection db,UUID tenant,VerifiedIdentity actor,UUID id,UUID entity,String kind,FulfillmentGateway.Result result) throws SQLException {
        if(result.state()==FulfillmentGateway.State.UNKNOWN || result.state()==FulfillmentGateway.State.NOT_FOUND) {
            Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state='UNKNOWN',lease=NULL,lease_until=NULL,next_at=clock_timestamp()+interval '5 seconds' WHERE id=?",id);
            return;
        }
        if(result.state()==FulfillmentGateway.State.CONFIRMED) Sql.opaque(result.reference());
        Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state=?,provider_ref=?,lease=NULL,lease_until=NULL WHERE id=?",result.state().name(),result.reference(),id);
        complete(db,tenant,id,entity,kind,result.state()==FulfillmentGateway.State.CONFIRMED);
        OperationJournal.event(db,tenant,actor,entity,kind+"_"+result.state().name(),org.dzoka.foundation.application.ContentDigest.sha256((id+":"+result.state()+":"+result.reference()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    public record Callback(UUID tenant,UUID event,UUID operation,String state,String reference,String shipmentStatus,String proofSha,Instant occurredAt) {}
    public void callback(VerifiedIdentity worker,UUID tenant,Callback callback,String bodySha) {
        Sql.sha(bodySha);Sql.sha(callback.proofSha());
        if(callback.event()==null || callback.operation()==null || !tenant.equals(callback.tenant()) || callback.occurredAt()==null
                || callback.occurredAt().isAfter(Instant.now().plusSeconds(60))) throw new IllegalArgumentException();
        var state=FulfillmentGateway.State.valueOf(callback.state());
        if(state==FulfillmentGateway.State.NOT_FOUND) throw new IllegalArgumentException();
        tx.execute(worker,tenant,"fulfillment:process",db -> {
            Sql.lock(db,tenant);
            try(var query=Sql.q(db,"SELECT request_sha FROM dzoka.fulfillment_callbacks WHERE event_id=?",callback.event());var row=query.executeQuery()) {
                if(row.next()) { if(!bodySha.equals(row.getString(1)))throw new Failures.Conflict();return null; }
            }
            UUID entity;String kind,current;
            try(var query=Sql.q(db,"SELECT entity_id,kind,state,provider_ref FROM dzoka.fulfillment_operations WHERE id=? FOR UPDATE",callback.operation());var row=query.executeQuery()) {
                if(!row.next())throw new Failures.NotFound();
                entity=row.getObject(1,UUID.class);kind=row.getString(2);current=row.getString(3);
                if(Set.of("READY","BLOCKED","CANCELLED").contains(current))throw new Failures.Conflict();
                if(current.equals("CONFIRMED") && state==FulfillmentGateway.State.CONFIRMED && !Objects.equals(row.getString(4),callback.reference())) throw new Failures.Conflict();
            }
            Sql.write(db,"INSERT INTO dzoka.fulfillment_callbacks(tenant_id,event_id,request_sha) VALUES(?,?,?)",tenant,callback.event(),bodySha);
            if(!Set.of("CONFIRMED","REJECTED").contains(current)) apply(db,tenant,worker,callback.operation(),entity,kind,new FulfillmentGateway.Result(state,callback.reference()));
            boolean contradiction=(current.equals("CONFIRMED") && state==FulfillmentGateway.State.REJECTED)
                    || (current.equals("REJECTED") && state==FulfillmentGateway.State.CONFIRMED);
            if(contradiction && kind.startsWith("PAYMENT_")) Sql.write(db,"UPDATE dzoka.rewards SET disputed=true WHERE id=?",entity);
            if(callback.shipmentStatus()!=null) {
                if(!kind.equals("COURIER_BOOK") || !state.equals(FulfillmentGateway.State.CONFIRMED))throw new IllegalArgumentException();
                logistics.track(db,tenant,callback.event(),entity,callback.shipmentStatus(),callback.proofSha(),callback.occurredAt());
            }
            OperationJournal.event(db,tenant,worker,entity,contradiction?"PROVIDER_TERMINAL_CONFLICT":"PROVIDER_CALLBACK",callback.proofSha(),callback.occurredAt());
            return null;
        });
    }
}
