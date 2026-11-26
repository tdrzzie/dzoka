package org.dzoka.rewards;

import java.sql.*;
import java.util.*;
import org.dzoka.fulfillment.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public final class Rewards implements RewardEscrowProvider {
    private final TenantTransactions tx;
    private final ObjectMapper json;
    public Rewards(TenantTransactions tx,ObjectMapper json) { this.tx=tx;this.json=json; }
    public record Terms(UUID intake,UUID claim,String beneficiaryRef,String currency,long minorUnits,long feeMinor,String termsSha,int policyVersion) {}
    public record Reward(UUID id,Terms terms,String state,boolean disputed,boolean ownerConfirmed) {}
    public record Entry(UUID operation,String account,long amount,String currency) {}
    private record Stored(Reward view,String payer) {}
    public Reward offer(VerifiedIdentity actor,UUID tenant,UUID id,Terms terms) {
        if(terms==null || terms.intake()==null || terms.claim()==null || terms.policyVersion()<1) throw new IllegalArgumentException();
        new PaymentProvider.Money(terms.currency(),terms.minorUnits());Sql.sha(terms.beneficiaryRef());Sql.sha(terms.termsSha());
        if(terms.feeMinor()<0 || terms.feeMinor()>=terms.minorUnits() || actor.reference().equals(terms.beneficiaryRef())) throw new IllegalArgumentException();
        return tx.execute(actor,tenant,"rewards:use",db -> {
            Sql.lock(db,tenant);var old=stored(db,id);
            if(old!=null) {
                if(!old.payer().equals(actor.reference()) || !old.view().terms().equals(terms)) throw new Failures.Conflict();
                return old.view();
            }
            var c=RecoveryContext.read(db,terms.intake(),terms.claim());
            if(!actor.reference().equals(c.owner()) || c.policy()!=terms.policyVersion() || c.returned() || !c.available()) throw new Failures.Conflict();
            Sql.write(db,"INSERT INTO dzoka.rewards(tenant_id,id,intake_id,claim_id,payer_ref,beneficiary_ref,currency,minor_units,fee_minor,terms_sha,policy_version) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    tenant,id,terms.intake(),terms.claim(),actor.reference(),terms.beneficiaryRef(),terms.currency(),terms.minorUnits(),terms.feeMinor(),terms.termsSha(),terms.policyVersion());
            OperationJournal.enqueue(db,tenant,actor,id,"PAYMENT_RESERVE",payload(id,terms));
            OperationJournal.event(db,tenant,actor,id,"REWARD_OFFERED",terms.termsSha());
            return required(db,id).view();
        });
    }
    public Reward read(VerifiedIdentity actor,UUID tenant,UUID id) { return tx.execute(actor,tenant,"rewards:use",db -> required(db,id).view()); }
    public List<Entry> ledger(VerifiedIdentity actor,UUID tenant,UUID id) {
        return tx.execute(actor,tenant,"rewards:use",db -> {
            required(db,id);var entries=new ArrayList<Entry>();
            try(var query=Sql.q(db,"SELECT operation_id,account,amount,currency FROM dzoka.reward_ledger WHERE reward_id=? ORDER BY created_at,operation_id,account",id);var row=query.executeQuery()) {
                while(row.next()) entries.add(new Entry(row.getObject(1,UUID.class),row.getString(2),row.getLong(3),row.getString(4)));
            }
            return List.copyOf(entries);
        });
    }
    public Reward confirm(VerifiedIdentity actor,UUID tenant,UUID id,String evidence) {
        Sql.sha(evidence);
        return tx.execute(actor,tenant,"rewards:use",db -> {
            Sql.lock(db,tenant);var r=required(db,id);payer(actor,r);
            var c=RecoveryContext.read(db,r.view().terms().intake(),r.view().terms().claim());c.requireReward();
            if(!r.view().ownerConfirmed()) {
                Sql.write(db,"UPDATE dzoka.rewards SET owner_confirmed=true WHERE id=?",id);
                OperationJournal.event(db,tenant,actor,id,"REWARD_OWNER_CONFIRMED",evidence);
            }
            return required(db,id).view();
        });
    }
    public Reward settle(VerifiedIdentity actor,UUID tenant,UUID id,boolean refund) {
        return tx.execute(actor,tenant,"rewards:use",db -> {
            Sql.lock(db,tenant);var r=required(db,id);payer(actor,r);
            String pending=refund?"REFUND_PENDING":"RELEASE_PENDING",terminal=refund?"REFUNDED":"RELEASED";
            if(r.view().state().equals(pending) || r.view().state().equals(terminal)) return r.view();
            if(!r.view().state().equals("RESERVED") || r.view().disputed()) throw new Failures.Conflict();
            if(!refund) eligibility(db,r);
            Sql.write(db,"UPDATE dzoka.rewards SET state=? WHERE id=?",pending,id);
            OperationJournal.enqueue(db,tenant,actor,id,refund?"PAYMENT_REFUND":"PAYMENT_RELEASE",payload(id,r.view().terms()));
            return required(db,id).view();
        });
    }
    public Reward dispute(VerifiedIdentity actor,UUID tenant,UUID id,String evidence,boolean resolve) {
        Sql.sha(evidence);
        return tx.execute(actor,tenant,resolve?"rewards:review":"rewards:use",db -> {
            Sql.lock(db,tenant);var r=required(db,id);
            if(resolve && (actor.reference().equals(r.payer()) || actor.reference().equals(r.view().terms().beneficiaryRef()))) throw new Failures.Forbidden();
            if(resolve && r.view().state().equals("REVERSE_PENDING"))throw new Failures.Conflict();
            if(resolve && Set.of("RELEASE_PENDING","REFUND_PENDING","RESERVE_PENDING").contains(r.view().state())) {
                String kind=r.view().state().equals("RELEASE_PENDING")?"PAYMENT_RELEASE":r.view().state().equals("REFUND_PENDING")?"PAYMENT_REFUND":"PAYMENT_RESERVE";
                try(var query=Sql.q(db,"SELECT state FROM dzoka.fulfillment_operations WHERE entity_id=? AND kind=?",id,kind);var row=query.executeQuery()) {
                    if(!row.next() || !Set.of("READY","BLOCKED").contains(row.getString(1)))throw new Failures.Conflict();
                }
            }
            if(r.view().disputed()!=!resolve) {
                Sql.write(db,"UPDATE dzoka.rewards SET disputed=? WHERE id=?",!resolve,id);
                OperationJournal.event(db,tenant,actor,id,resolve?"DISPUTE_RESOLVED":"DISPUTE_OPENED",evidence);
            }
            return required(db,id).view();
        });
    }
    public Reward cancelIntent(VerifiedIdentity actor,UUID tenant,UUID id,String evidence) {
        Sql.sha(evidence);
        return tx.execute(actor,tenant,"rewards:use",db -> {
            Sql.lock(db,tenant);var r=required(db,id);payer(actor,r);
            String kind=switch(r.view().state()){case "RELEASE_PENDING"->"PAYMENT_RELEASE";case "REFUND_PENDING"->"PAYMENT_REFUND";case "RESERVE_PENDING"->"PAYMENT_RESERVE";default->throw new Failures.Conflict();};
            if(Sql.write(db,"UPDATE dzoka.fulfillment_operations SET state='CANCELLED' WHERE entity_id=? AND kind=? AND state IN ('READY','BLOCKED')",id,kind)!=1)throw new Failures.Conflict();
            Sql.write(db,"UPDATE dzoka.rewards SET state=? WHERE id=?",kind.equals("PAYMENT_RESERVE")?"FAILED":"RESERVED",id);
            OperationJournal.event(db,tenant,actor,id,"UNSENT_PAYMENT_CANCELLED",evidence);return required(db,id).view();
        });
    }
    public void validateOperation(Connection db,UUID id,String kind) throws SQLException {
        var r=required(db,id);
        if(kind.equals("PAYMENT_REVERSAL")) {
            if(!r.view().state().equals("REVERSE_PENDING") || !r.view().disputed())throw new Failures.Conflict();return;
        }
        if(kind.equals("PAYMENT_RESERVE")) {
            if(r.view().disputed() || !r.view().state().equals("RESERVE_PENDING")) throw new Failures.Conflict();
        } else {
            if(r.view().disputed() || !r.view().state().equals(kind.equals("PAYMENT_RELEASE")?"RELEASE_PENDING":"REFUND_PENDING")) throw new Failures.Conflict();
            if(kind.equals("PAYMENT_RELEASE")) eligibility(db,r);
        }
    }
    public Reward reverse(VerifiedIdentity actor,UUID tenant,UUID id,String evidence) {
        Sql.sha(evidence);
        return tx.execute(actor,tenant,"rewards:review",db -> {
            Sql.lock(db,tenant);var r=required(db,id);
            if(actor.reference().equals(r.payer()) || actor.reference().equals(r.view().terms().beneficiaryRef()))throw new Failures.Forbidden();
            if(Set.of("REVERSE_PENDING","REVERSED").contains(r.view().state()))return r.view();
            if(!r.view().state().equals("RELEASED") || !r.view().disputed())throw new Failures.Conflict();
            Sql.write(db,"UPDATE dzoka.rewards SET state='REVERSE_PENDING' WHERE id=?",id);
            OperationJournal.enqueue(db,tenant,actor,id,"PAYMENT_REVERSAL",payload(id,r.view().terms()));
            OperationJournal.event(db,tenant,actor,id,"REVERSAL_APPROVED",evidence);return required(db,id).view();
        });
    }
    private static void eligibility(Connection db,Stored r) throws SQLException {
        var c=RecoveryContext.read(db,r.view().terms().intake(),r.view().terms().claim());c.requireReward();
        if(!r.view().ownerConfirmed() || c.policy()!=r.view().terms().policyVersion()) throw new Failures.Conflict();
    }
    public void complete(Connection db,UUID tenant,UUID operation,UUID id,String kind,boolean confirmed) throws SQLException {
        var r=required(db,id);var terms=r.view().terms();
        if(kind.equals("PAYMENT_REVERSAL")) {
            if(confirmed) {
                transfer(db,tenant,operation,id,"PAYOUT_REVERSAL","BENEFICIARY","PAYER",terms.minorUnits()-terms.feeMinor(),terms.currency());
                if(terms.feeMinor()>0)transfer(db,tenant,operation,id,"FEE_REVERSAL","FEE","PAYER",terms.feeMinor(),terms.currency());
            }
            Sql.write(db,"UPDATE dzoka.rewards SET state=? WHERE id=?",confirmed?"REVERSED":"RELEASED",id);return;
        }
        if(confirmed) {
            if(kind.equals("PAYMENT_RESERVE")) transfer(db,tenant,operation,id,"RESERVE","FUNDING","ESCROW",terms.minorUnits(),terms.currency());
            else if(kind.equals("PAYMENT_REFUND")) transfer(db,tenant,operation,id,"REFUND","ESCROW","PAYER",terms.minorUnits(),terms.currency());
            else {
                transfer(db,tenant,operation,id,"PAYOUT","ESCROW","BENEFICIARY",terms.minorUnits()-terms.feeMinor(),terms.currency());
                if(terms.feeMinor()>0) transfer(db,tenant,operation,id,"FEE","ESCROW","FEE",terms.feeMinor(),terms.currency());
            }
        }
        String state=kind.equals("PAYMENT_RESERVE")?(confirmed?"RESERVED":"FAILED"):
                (confirmed?(kind.equals("PAYMENT_RELEASE")?"RELEASED":"REFUNDED"):"RESERVED");
        Sql.write(db,"UPDATE dzoka.rewards SET state=? WHERE id=?",state,id);
    }
    private static void transfer(Connection db,UUID tenant,UUID operation,UUID reward,String leg,String debit,String credit,long amount,String currency) throws SQLException {
        Sql.write(db,"INSERT INTO dzoka.reward_transfers(tenant_id,reward_id,operation_id,leg,debit,credit,minor_units,currency) VALUES(?,?,?,?,?,?,?,?)",
                tenant,reward,operation,leg,debit,credit,amount,currency);
    }
    private String payload(UUID id,Terms terms) { return json.writeValueAsString(new TreeMap<>(Map.of("reward",id.toString(),"beneficiaryReference",terms.beneficiaryRef(),"currency",terms.currency(),"minorUnits",terms.minorUnits(),"feeMinor",terms.feeMinor(),"termsSha",terms.termsSha()))); }
    private static void payer(VerifiedIdentity actor,Stored r) { if(!actor.reference().equals(r.payer())) throw new Failures.Forbidden(); }
    private static Stored required(Connection db,UUID id) throws SQLException { var value=stored(db,id);if(value==null) throw new Failures.NotFound();return value; }
    private static Stored stored(Connection db,UUID id) throws SQLException {
        try(var query=Sql.q(db,"SELECT * FROM dzoka.rewards WHERE id=?",id);var row=query.executeQuery()) {
            if(!row.next())return null;
            var terms=new Terms(row.getObject("intake_id",UUID.class),row.getObject("claim_id",UUID.class),row.getString("beneficiary_ref"),row.getString("currency"),row.getLong("minor_units"),row.getLong("fee_minor"),row.getString("terms_sha"),row.getInt("policy_version"));
            return new Stored(new Reward(id,terms,row.getString("state"),row.getBoolean("disputed"),row.getBoolean("owner_confirmed")),row.getString("payer_ref"));
        }
    }
    @Override public String settlementState(VerifiedIdentity actor,UUID tenant,UUID reward) {
        return read(actor,tenant,reward).state();
    }
}
