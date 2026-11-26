package org.dzoka.recovery.adapters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.MutationJournal;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.recovery.domain.Recovery;
import org.dzoka.recovery.domain.Recovery.*;
import org.dzoka.recovery.ports.RecoveryOperations;
import org.springframework.stereotype.Component;

@Component
final class JdbcRecovery implements RecoveryOperations {
    private static final String MANAGE="recovery:manage", VERIFY="recovery:verify", REVIEW="recovery:review",
            HANDOVER="recovery:handover", SUBMIT="claims:submit";
    private final TenantTransactions transactions;
    private final RecoverySecrets secrets;

    JdbcRecovery(TenantTransactions transactions,RecoverySecrets secrets) {
        this.transactions=transactions;
        this.secrets=secrets;
    }

    private record Intake(UUID report,UUID site,String custodian,int generation,boolean blocked,boolean returned) {}
    private record Report(int revision,long version,boolean available) {}
    private record StoredClaim(Claim view,String actor) {}
    private record Authorization(UUID review,Instant verificationExpires) {}
    private record Session(FactKind kind,int revision,String observer,int generation,int policy,long reportVersion,Instant expires,
                           boolean active,String outcome,String mac) {}
    private record Presented(UUID claim,UUID review,boolean active) {}
    private record RecordedReturn(UUID intake,UUID grant,String confirmation,Instant at) {}

    @Override public Policy policy(VerifiedIdentity actor,UUID tenant,int version,boolean dual,boolean representatives) {
        if (version<1) throw new IllegalArgumentException("Invalid policy version");
        return transactions.execute(actor,tenant,MANAGE,db -> {
            lockTenant(db,tenant);
            var requested=new Policy(version,dual,representatives);
            var current=currentPolicy(db,tenant);
            if (requested.equals(current)) return current;
            if (version!=(current==null?1:current.version()+1)) throw new Failures.Conflict();
            execute(db,"INSERT INTO dzoka.recovery_policies(tenant_id,version,dual_control,allow_representatives,approved_by) VALUES(?,?,?,?,?)",
                    tenant,version,dual,representatives,actor.reference());
            activity(db,tenant,null,null,actor,"POLICY_UPDATED",null,null);
            return requested;
        });
    }

    @Override public void enroll(VerifiedIdentity actor,UUID tenant,UUID intake,UUID found,UUID site) {
        required(found,site);
        transactions.execute(actor,tenant,MANAGE,db -> {
            lockTenant(db,tenant);
            lockReport(db,tenant,found);
            try (var sql=q(db,"""
                    SELECT item_id FROM dzoka.reports WHERE tenant_id=? AND id=? AND kind='FOUND'
                      AND deleted_at IS NULL AND expires_at>clock_timestamp() AND NOT legal_hold
                      AND (owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                    """,tenant,found);var rows=sql.executeQuery()) {
                if (!rows.next()) throw new Failures.NotFound();
                int inserted=execute(db,"""
                        INSERT INTO dzoka.recovery_intakes(tenant_id,id,item_id,report_id,site_id,custodian_ref)
                        VALUES(?,?,?,?,?,?) ON CONFLICT DO NOTHING
                        """,tenant,intake,rows.getObject(1,UUID.class),found,site,actor.reference());
                if (inserted==1) activity(db,tenant,intake,null,actor,"ENROLLED",intake,null);
                else {
                    Intake existing=intake(db,tenant,intake);
                    if (!existing.report().equals(found) || !existing.site().equals(site)
                            || !existing.custodian().equals(actor.reference())) throw new Failures.Conflict();
                }
            }
            return null;
        });
    }

    @Override public void hold(VerifiedIdentity actor,UUID tenant,UUID id,boolean blocked) {
        transactions.execute(actor,tenant,MANAGE,db -> {
            Intake item=locked(db,tenant,id);
            if (item.returned()) throw new Failures.Conflict();
            if (item.blocked()!=blocked) {
                execute(db,"UPDATE dzoka.recovery_intakes SET blocked=?,generation=generation+1 WHERE tenant_id=? AND id=?",
                        blocked,tenant,id);
                activity(db,tenant,id,null,actor,blocked?"HOLD_PLACED":"HOLD_LIFTED",null,null);
            }
            return null;
        });
    }

    @Override public void fact(VerifiedIdentity actor,UUID tenant,UUID id,UUID fact,FactKind kind,
                               String source,String answer,boolean privateConfirmed) {
        required(kind);
        checkDigest(source);
        String normalized=Recovery.answer(answer);
        if (!privateConfirmed || !Recovery.privateEnough(normalized)) throw new IllegalArgumentException("Unsafe private fact");
        transactions.execute(actor,tenant,VERIFY,db -> {
            Intake item=locked(db,tenant,id);
            Report report=available(db,id,item);
            try (var query=q(db,"""
                    SELECT raw_text,normalized_text,raw_location,raw_time,category FROM dzoka.report_revisions
                    WHERE tenant_id=? AND report_id=?
                    """,tenant,item.report());var rows=query.executeQuery()) {
                boolean found=false;
                while (rows.next()) {
                    found=true;
                    for (int i=1;i<=5;i++) if (Recovery.normalize(rows.getString(i)).contains(normalized))
                        throw new IllegalArgumentException("Unsafe private fact");
                }
                if (!found) throw new Failures.NotFound();
            }
            String mac=secrets.digest("ownership-answer",tenant,id,normalized);
            int added=execute(db,"""
                    INSERT INTO dzoka.private_recovery_facts(tenant_id,id,intake_id,report_revision,kind,source_sha,answer_mac,observer_ref)
                    SELECT ?,?,?,?,?,?,?,? WHERE NOT EXISTS (
                      SELECT 1 FROM dzoka.private_recovery_facts f LEFT JOIN dzoka.recovery_fact_withdrawals w
                        ON (w.tenant_id,w.fact_id)=(f.tenant_id,f.id)
                      WHERE f.tenant_id=? AND f.intake_id=? AND (
                        (f.report_revision=? AND f.kind=? AND w.fact_id IS NULL)
                        OR (f.answer_mac=? AND w.fact_id IS NOT NULL)
                      )
                    ) ON CONFLICT DO NOTHING
                    """,tenant,fact,id,report.revision(),kind.name(),source,mac,actor.reference(),
                    tenant,id,report.revision(),kind.name(),mac);
            if (added==1) activity(db,tenant,id,null,actor,"FACT_ATTESTED",fact,null);
            else try (var query=q(db,"""
                    SELECT 1 FROM dzoka.private_recovery_facts
                    WHERE tenant_id=? AND id=? AND intake_id=? AND report_revision=? AND kind=?
                      AND source_sha=? AND answer_mac=? AND observer_ref=?
                    """,tenant,fact,id,report.revision(),kind.name(),source,mac,actor.reference())) {
                if (!exists(query)) throw new Failures.Conflict();
            }
            return null;
        });
    }

    @Override public void withdrawFact(VerifiedIdentity actor,UUID tenant,UUID intake,UUID fact,FactWithdrawal reason) {
        required(reason);
        transactions.execute(actor,tenant,VERIFY,db -> {
            locked(db,tenant,intake);
            try (var select=q(db,"SELECT 1 FROM dzoka.private_recovery_facts WHERE tenant_id=? AND intake_id=? AND id=?",
                    tenant,intake,fact)) {
                if (!exists(select)) throw new Failures.NotFound();
            }
            int added=execute(db,"""
                    INSERT INTO dzoka.recovery_fact_withdrawals(tenant_id,fact_id,actor_ref,reason)
                    VALUES(?,?,?,?) ON CONFLICT DO NOTHING
                    """,tenant,fact,actor.reference(),reason.name());
            if (added==1) activity(db,tenant,intake,null,actor,"FACT_WITHDRAWN",fact,reason.name());
            else try (var select=q(db,"""
                    SELECT 1 FROM dzoka.recovery_fact_withdrawals WHERE tenant_id=? AND fact_id=? AND reason=?
                    """,tenant,fact,reason.name())) {
                if (!exists(select)) throw new Failures.Conflict();
            }
            return null;
        });
    }

    @Override public Claim submit(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,Capacity capacity) {
        return submitMatch(actor,tenant,id,claim,capacity,null,0);
    }

    @Override public Claim submitMatch(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,Capacity capacity,
                                      UUID candidate,int revision) {
        required(capacity);
        return transactions.execute(actor,tenant,SUBMIT,db -> {
            Intake item=locked(db,tenant,id);
            StoredClaim existing=claim(db,tenant,id,claim);
            if (existing!=null) {
                if (!existing.actor().equals(actor.reference()) || existing.view().capacity()!=capacity) throw new Failures.Conflict();
                return existing.view();
            }
            Report report=available(db,id,item);
            if(candidate!=null && (!item.report().equals(candidate) || report.revision()!=revision))
                throw new Failures.Conflict();
            Policy policy=requirePolicy(db,tenant);
            if ((capacity==Capacity.REPRESENTATIVE && !policy.allowRepresentatives())
                    || limited(db,tenant,null,actor.reference(),"CLAIM_OPENED",10)
                    || limited(db,tenant,id,null,"CLAIM_OPENED",24)) throw new Failures.Conflict();
            int inserted=execute(db,"""
                    INSERT INTO dzoka.ownership_claims(tenant_id,id,intake_id,actor_ref,capacity)
                    VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING
                    """,tenant,claim,id,actor.reference(),capacity.name());
            if (inserted!=1) throw new Failures.Conflict();
            activity(db,tenant,id,claim,actor,"CLAIM_OPENED",claim,null);
            return new Claim(claim,id,capacity,"PENDING");
        });
    }

    @Override public Claim read(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim) {
        return transactions.execute(actor,tenant,SUBMIT,db -> owned(db,tenant,id,claim,actor).view());
    }

    @Override public String pickupMessage(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,boolean acknowledge) {
        return transactions.execute(actor,tenant,SUBMIT,db -> {
            Intake item=locked(db,tenant,id);
            StoredClaim subject=owned(db,tenant,id,claim,actor);
            if(item.returned()) {
                try(var query=q(db,"SELECT 1 WHERE dzoka.channel_return_confirmed(?,?)",id,claim)) {
                    if(!exists(query)) return "CLOSED";
                    if(acknowledge) appendPickup(db,tenant,id,claim,actor,"ACKNOWLEDGMENT");
                    return acknowledge?"ACKNOWLEDGED":"RETURNED";
                }
            }
            if(acknowledge) return "UNAVAILABLE";
            if(!"APPROVED".equals(subject.view().state())) return subject.view().state();
            try { available(db,id,item); }
            catch(Failures.Conflict denied) { return "UNAVAILABLE"; }
            appendPickup(db,tenant,id,claim,actor,"REQUEST");
            return "CONTACT_DESK";
        });
    }

    private static void appendPickup(Connection db,UUID tenant,UUID intake,UUID claim,VerifiedIdentity actor,String kind)
            throws SQLException {
        execute(db,"""
                INSERT INTO dzoka.pickup_messages(tenant_id,intake_id,claim_id,actor_ref,kind)
                VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING
                """,tenant,intake,claim,actor.reference(),kind);
    }

    @Override public java.util.List<PickupMessage> pickupMessages(VerifiedIdentity actor,UUID tenant,UUID intake) {
        return transactions.execute(actor,tenant,HANDOVER,db -> {
            intake(db,tenant,intake);
            var messages=new java.util.ArrayList<PickupMessage>();
            try(var query=q(db,"""
                    SELECT claim_id,kind,created_at FROM dzoka.pickup_messages
                    WHERE tenant_id=? AND intake_id=? ORDER BY created_at DESC,claim_id,kind LIMIT 50
                    """,tenant,intake);var rows=query.executeQuery()) {
                while(rows.next()) messages.add(new PickupMessage(rows.getObject(1,UUID.class),rows.getString(2),instant(rows,3)));
            }
            return java.util.List.copyOf(messages);
        });
    }

    @Override public Claim withdraw(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim) {
        return transactions.execute(actor,tenant,SUBMIT,db -> {
            locked(db,tenant,id);
            var own=owned(db,tenant,id,claim,actor);
            if (own.view().state().equals("WITHDRAWN")) return own.view();
            pending(own);
            return changeClaim(db,tenant,id,own,actor,"WITHDRAWN",null);
        });
    }

    @Override public Challenge challenge(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,UUID session) {
        return transactions.execute(actor,tenant,SUBMIT,db -> {
            Intake item=locked(db,tenant,id);
            pending(owned(db,tenant,id,claim,actor));
            Report report=available(db,id,item);
            Policy policy=requirePolicy(db,tenant);
            Session existing=session(db,tenant,id,claim,session);
            if (existing!=null) {
                currentSession(existing,report,item,policy);
                if (existing.outcome()!=null) throw new Failures.Conflict();
                return new Challenge(session,"READY",existing.kind().prompt(),existing.expires());
            }
            if (challengeLocked(db,tenant,id,actor)
                    || limited(db,tenant,id,null,"CHALLENGE_ISSUED",12)
                    || limited(db,tenant,null,actor.reference(),"CHALLENGE_ISSUED",5)) throw new Failures.Conflict();
            try (var query=q(db,"""
                    SELECT f.id,f.kind FROM dzoka.private_recovery_facts f LEFT JOIN dzoka.recovery_fact_withdrawals w
                        ON (w.tenant_id,w.fact_id)=(f.tenant_id,f.id)
                    WHERE f.tenant_id=? AND f.intake_id=? AND f.report_revision=? AND f.observer_ref<>?
                      AND w.fact_id IS NULL
                    ORDER BY f.kind,f.id LIMIT 1
                    """,tenant,id,report.revision(),actor.reference());var rows=query.executeQuery()) {
                if (!rows.next()) return new Challenge(null,"INCONCLUSIVE",null,null);
                try (var insert=q(db,"""
                        INSERT INTO dzoka.ownership_sessions(tenant_id,id,intake_id,claim_id,fact_id,actor_ref,policy_version,report_version,generation)
                        VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING RETURNING expires_at
                        """,tenant,session,id,claim,rows.getObject(1,UUID.class),actor.reference(),policy.version(),report.version(),item.generation());
                     var newSession=insert.executeQuery()) {
                    if (!newSession.next()) throw new Failures.Conflict();
                    activity(db,tenant,id,claim,actor,"CHALLENGE_ISSUED",session,null);
                    return new Challenge(session,"READY",FactKind.valueOf(rows.getString(2)).prompt(),instant(newSession,1));
                }
            }
        });
    }

    @Override public Assessment answer(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,UUID session,String answer) {
        String normalized=Recovery.answer(answer);
        return transactions.execute(actor,tenant,SUBMIT,db -> {
            Intake item=locked(db,tenant,id);
            pending(owned(db,tenant,id,claim,actor));
            Report report=available(db,id,item);
            Session challenge=session(db,tenant,id,claim,session);
            if (challenge==null) throw new Failures.NotFound();
            currentSession(challenge,report,item,requirePolicy(db,tenant));
            if (challenge.outcome()!=null) return new Assessment(challenge.outcome());
            if (challengeLocked(db,tenant,id,actor)) throw new Failures.Conflict();
            String supplied=secrets.digest("ownership-answer",tenant,id,normalized);
            String outcome=RecoverySecrets.equal(supplied,challenge.mac())?"PASSED":"FAILED";
            execute(db,"UPDATE dzoka.ownership_sessions SET outcome=? WHERE tenant_id=? AND id=?",outcome,tenant,session);
            activity(db,tenant,id,claim,actor,"CHALLENGE_"+outcome,session,null);
            return new Assessment(outcome);
        });
    }

    @Override public ReviewContext reviewContext(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim) {
        return transactions.execute(actor,tenant,REVIEW,db -> {
            Intake item=intake(db,tenant,intake);
            StoredClaim subject=requiredClaim(db,tenant,intake,claim);
            independentReviewer(actor,item,subject);
            return new ReviewContext(subject.view(),subject.actor(),risk(db,tenant,intake,subject,
                    item.blocked() || !report(db,intake).available()));
        });
    }

    @Override public Claim review(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,UUID review,Review input) {
        required(input.decision(),input.reason());
        checkDigest(input.evidenceSha());
        if (input.entitlementSha()!=null) checkDigest(input.entitlementSha());
        boolean approval=input.decision()==Decision.APPROVE;
        if (approval!=(input.reason()==Reason.INDEPENDENT_EVIDENCE) || (approval && input.session()==null))
            throw new IllegalArgumentException("Invalid review");
        return transactions.execute(actor,tenant,REVIEW,db -> {
            Intake item=locked(db,tenant,id);
            StoredClaim subject=requiredClaim(db,tenant,id,claim);
            independentReviewer(actor,item,subject);
            try (var query=q(db,"""
                    SELECT intake_id,claim_id,session_id,actor_ref,decision,reason,evidence_sha,entitlement_sha
                    FROM dzoka.recovery_reviews WHERE tenant_id=? AND id=?
                    """,tenant,review);var rows=query.executeQuery()) {
                if (rows.next()) {
                    if (!id.equals(rows.getObject(1,UUID.class)) || !claim.equals(rows.getObject(2,UUID.class))
                            || !Objects.equals(input.session(),rows.getObject(3,UUID.class))
                            || !actor.reference().equals(rows.getString(4)) || !input.decision().name().equals(rows.getString(5))
                            || !input.reason().name().equals(rows.getString(6)) || !input.evidenceSha().equals(rows.getString(7))
                            || !Objects.equals(input.entitlementSha(),rows.getString(8))) throw new Failures.Conflict();
                    return subject.view();
                }
            }
            pending(subject);
            Report report=report(db,id);
            Policy policy=requirePolicy(db,tenant);
            Session assessment=input.session()==null?null:session(db,tenant,id,claim,input.session());
            if (input.session()!=null && assessment==null) throw new Failures.NotFound();
            if (approval) {
                requireAvailable(report,item);
                currentSession(assessment,report,item,policy);
                if (actor.reference().equals(assessment.observer())) throw new Failures.Forbidden();
                if (!"PASSED".equals(assessment.outcome()) || competing(db,tenant,id,claim)) throw new Failures.Conflict();
                if (subject.view().capacity()==Capacity.REPRESENTATIVE
                        && (!policy.allowRepresentatives() || input.entitlementSha()==null)) throw new Failures.Conflict();
            }
            execute(db,"""
                    INSERT INTO dzoka.recovery_reviews(tenant_id,id,intake_id,claim_id,session_id,actor_ref,
                      policy_version,report_version,generation,decision,reason,evidence_sha,entitlement_sha)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,tenant,review,id,claim,input.session(),actor.reference(),policy.version(),report.version(),
                    item.generation(),input.decision().name(),input.reason().name(),input.evidenceSha(),input.entitlementSha());
            activity(db,tenant,id,claim,actor,"REVIEW_RECORDED",review,input.reason().name());
            if (!approval) return changeClaim(db,tenant,id,subject,actor,"REJECTED",input.reason());
            if (!policy.dualControl() || secondReview(db,tenant,claim,review,actor,policy,item,report,input))
                return changeClaim(db,tenant,id,subject,actor,"APPROVED",input.reason());
            return subject.view();
        });
    }

    private static boolean secondReview(Connection db,UUID tenant,UUID claim,UUID review,VerifiedIdentity actor,
                                        Policy policy,Intake item,Report report,Review input) throws SQLException {
        try (var select=q(db,"""
                SELECT 1 FROM dzoka.recovery_reviews WHERE tenant_id=? AND claim_id=? AND id<>?
                  AND session_id=? AND decision='APPROVE' AND policy_version=? AND report_version=? AND generation=?
                  AND actor_ref<>? AND evidence_sha<>? AND entitlement_sha IS NOT DISTINCT FROM ?::text LIMIT 1
                """,tenant,claim,review,input.session(),policy.version(),report.version(),item.generation(),
                actor.reference(),input.evidenceSha(),input.entitlementSha())) { return exists(select); }
    }

    @Override public Claim revoke(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,Reason reason) {
        if (reason==null || reason==Reason.INDEPENDENT_EVIDENCE) throw new IllegalArgumentException("Invalid revocation");
        return transactions.execute(actor,tenant,MANAGE,db -> {
            locked(db,tenant,id);
            var subject=requiredClaim(db,tenant,id,claim);
            if (subject.view().state().equals("REVOKED")) return subject.view();
            if (!subject.view().state().equals("APPROVED")) throw new Failures.Conflict();
            return changeClaim(db,tenant,id,subject,actor,"REVOKED",reason);
        });
    }

    @Override public Grant issue(VerifiedIdentity actor,UUID tenant,UUID id,UUID claim,UUID grant,String audience) {
        checkDigest(audience);
        return transactions.execute(actor,tenant,REVIEW,db -> {
            Intake item=locked(db,tenant,id);
            var subject=requiredClaim(db,tenant,id,claim);
            if (audience.equals(subject.actor()) || audience.equals(actor.reference())
                    || actor.reference().equals(subject.actor()) || actor.reference().equals(item.custodian()))
                throw new Failures.Forbidden();
            Authorization authorization=releaseReview(db,tenant,id,item,subject,null);
            try (var sql=q(db,"""
                    SELECT id,review_id,claim_id,audience_ref,issuer_ref,nonce,expires_at,used_at IS NULL AND expires_at>clock_timestamp(),token_mac
                    FROM dzoka.handover_grants WHERE tenant_id=? AND id=?
                    """,tenant,grant);var rows=sql.executeQuery()) {
                if (rows.next()) {
                    if (!rows.getBoolean(8) || !authorization.review().equals(rows.getObject(2,UUID.class))
                            || !claim.equals(rows.getObject(3,UUID.class)) || !audience.equals(rows.getString(4))
                            || !actor.reference().equals(rows.getString(5))) throw new Failures.Conflict();
                    String token=secrets.token(tenant,grant,rows.getString(6));
                    if (!RecoverySecrets.equal(secrets.digest("pickup-verifier",tenant,grant,token),rows.getString(9)))
                        throw new Failures.Conflict();
                    return new Grant(grant,token,instant(rows,7));
                }
            }
            if (limited(db,tenant,id,null,"GRANT_ISSUED",10)) throw new Failures.Conflict();
            try (var prior=q(db,"""
                    SELECT 1 FROM dzoka.handover_grants WHERE tenant_id=? AND intake_id=? AND claim_id=?
                      AND used_at IS NULL AND expires_at>clock_timestamp()
                    """,tenant,id,claim)) {
                if (exists(prior)) throw new Failures.Conflict();
            }
            String nonce=secrets.nonce();
            String token=secrets.token(tenant,grant,nonce);
            String mac=secrets.digest("pickup-verifier",tenant,grant,token);
            try (var sql=q(db,"""
                    INSERT INTO dzoka.handover_grants(tenant_id,id,intake_id,claim_id,review_id,audience_ref,issuer_ref,nonce,token_mac,expires_at)
                    VALUES(?,?,?,?,?,?,?,?,?,least(clock_timestamp()+interval '5 minutes',?::timestamptz)) RETURNING expires_at
                    """,tenant,grant,id,claim,authorization.review(),audience,actor.reference(),nonce,mac,
                    java.time.OffsetDateTime.ofInstant(authorization.verificationExpires(),java.time.ZoneOffset.UTC));
                 var rows=sql.executeQuery()) {
                rows.next();
                activity(db,tenant,id,claim,actor,"GRANT_ISSUED",grant,null);
                return new Grant(grant,token,instant(rows,1));
            }
        });
    }

    @Override public PickupContext pickupContext(VerifiedIdentity actor,UUID tenant,UUID id,UUID grant,String token) {
        return transactions.execute(actor,tenant,HANDOVER,db -> {
            lockTenant(db,tenant);
            Intake item=maybeIntake(db,tenant,id);
            if (item==null) {
                denied(db,tenant,null,actor);
                return null;
            }
            lockReport(db,tenant,item.report());
            if (redemptionLimited(db,tenant,id,actor)
                    || limited(db,tenant,id,actor.reference(),"PICKUP_CONTEXT_READ",20)) return null;
            Presented presentation=presented(db,tenant,id,grant,actor,token);
            if (presentation==null || !presentation.active()) {
                denied(db,tenant,id,actor);
                return null;
            }
            var subject=requiredClaim(db,tenant,id,presentation.claim());
            try { releaseReview(db,tenant,id,item,subject,presentation.review()); }
            catch (Failures.Conflict failure) {
                denied(db,tenant,id,actor);
                return null;
            }
            activity(db,tenant,id,presentation.claim(),actor,"PICKUP_CONTEXT_READ",grant,null);
            return new PickupContext(item.site(),subject.actor(),subject.view().capacity());
        });
    }

    @Override public Redemption redeem(VerifiedIdentity actor,UUID tenant,UUID id,UUID grant,UUID request,String token,
                                      String recipient,String proof) {
        checkDigest(recipient);
        checkDigest(proof);
        return transactions.execute(actor,tenant,HANDOVER,db -> {
            lockTenant(db,tenant);
            Intake item=maybeIntake(db,tenant,id);
            if (item==null) return denied(db,tenant,null,actor);
            lockReport(db,tenant,item.report());
            RecordedReturn previous=recordedReturn(db,tenant,actor,request);
            if (previous==null && redemptionLimited(db,tenant,id,actor)) return denied(db,tenant,id,actor);
            var presentation=presented(db,tenant,id,grant,actor,token);
            if (presentation==null) return denied(db,tenant,id,actor);
            UUID claim=presentation.claim();
            StoredClaim subject=requiredClaim(db,tenant,id,claim);
            if (!recipient.equals(subject.actor())) return denied(db,tenant,id,actor);
            if (previous!=null) {
                if (!id.equals(previous.intake()) || !grant.equals(previous.grant()) || !proof.equals(previous.confirmation()))
                    return denied(db,tenant,id,actor);
                activity(db,tenant,id,claim,actor,"REDEEM_REPLAYED",request,null);
                return new Redemption(true,new Receipt(request,previous.at()));
            }
            if (!presentation.active() || item.returned()) return denied(db,tenant,id,actor);
            try { releaseReview(db,tenant,id,item,subject,presentation.review()); }
            catch (Failures.Conflict failure) { return denied(db,tenant,id,actor); }
            int used=execute(db,"""
                    UPDATE dzoka.handover_grants SET used_at=clock_timestamp()
                    WHERE tenant_id=? AND id=? AND used_at IS NULL AND expires_at>clock_timestamp()
                    """,tenant,grant);
            if (used!=1) return denied(db,tenant,id,actor);
            try (var insert=q(db,"""
                    INSERT INTO dzoka.recovery_returns(tenant_id,intake_id,id,claim_id,grant_id,actor_ref,
                       from_custodian,recipient_ref,site_id,confirmation_sha) VALUES(?,?,?,?,?,?,?,?,?,?) RETURNING created_at
                    """,tenant,id,request,claim,grant,actor.reference(),item.custodian(),subject.actor(),item.site(),proof);
                 var rows=insert.executeQuery()) {
                rows.next();
                execute(db,"UPDATE dzoka.recovery_intakes SET returned=true,generation=generation+1 WHERE tenant_id=? AND id=?",tenant,id);
                changeClaim(db,tenant,id,subject,actor,"RETURNED",null);
                MutationJournal.append(db,tenant,actor,"recovery.returned",item.report(),report(db,id).version(),"report_id");
                activity(db,tenant,id,claim,actor,"REDEEMED",request,null);
                return new Redemption(true,new Receipt(request,instant(rows,1)));
            }
        });
    }

    private static RecordedReturn recordedReturn(Connection db,UUID tenant,VerifiedIdentity actor,UUID request) throws SQLException {
        try (var sql=q(db,"""
                SELECT intake_id,grant_id,confirmation_sha,created_at FROM dzoka.recovery_returns
                WHERE tenant_id=? AND actor_ref=? AND id=?
                """,tenant,actor.reference(),request);var rows=sql.executeQuery()) {
            return rows.next()?new RecordedReturn(rows.getObject(1,UUID.class),rows.getObject(2,UUID.class),
                    rows.getString(3),instant(rows,4)):null;
        }
    }

    private Presented presented(Connection db,UUID tenant,UUID id,UUID grant,VerifiedIdentity actor,String token)
            throws SQLException {
        if (token==null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        try (var sql=q(db,"""
                SELECT claim_id,review_id,token_mac,used_at IS NULL AND expires_at>clock_timestamp()
                FROM dzoka.handover_grants WHERE tenant_id=? AND intake_id=? AND id=? AND audience_ref=?
                """,tenant,id,grant,actor.reference());var rows=sql.executeQuery()) {
            if (!rows.next() || !RecoverySecrets.equal(secrets.digest("pickup-verifier",tenant,grant,token),rows.getString(3)))
                return null;
            return new Presented(rows.getObject(1,UUID.class),rows.getObject(2,UUID.class),rows.getBoolean(4));
        }
    }

    private static void independentReviewer(VerifiedIdentity actor,Intake item,StoredClaim subject) {
        if (actor.reference().equals(subject.actor()) || actor.reference().equals(item.custodian()))
            throw new Failures.Forbidden();
    }

    private static Authorization releaseReview(Connection db,UUID tenant,UUID id,Intake item,StoredClaim subject,UUID exact)
            throws SQLException {
        Report report=available(db,id,item);
        Policy policy=requirePolicy(db,tenant);
        UUID claim=subject.view().id();
        if (!subject.view().state().equals("APPROVED") || competing(db,tenant,id,claim)) throw new Failures.Conflict();
        try (var sql=q(db,"""
                SELECT v.id,s.expires_at FROM dzoka.recovery_reviews v
                JOIN dzoka.ownership_sessions s ON (s.tenant_id,s.id)=(v.tenant_id,v.session_id)
                JOIN dzoka.private_recovery_facts f ON (f.tenant_id,f.id)=(s.tenant_id,s.fact_id)
                LEFT JOIN dzoka.recovery_fact_withdrawals w ON (w.tenant_id,w.fact_id)=(f.tenant_id,f.id)
                WHERE v.tenant_id=? AND v.intake_id=? AND v.claim_id=? AND v.decision='APPROVE'
                  AND v.policy_version=? AND v.report_version=? AND v.generation=?
                  AND s.outcome='PASSED' AND s.expires_at>clock_timestamp() AND f.report_revision=?
                  AND s.report_version=v.report_version AND s.generation=v.generation AND s.policy_version=v.policy_version
                  AND w.fact_id IS NULL
                  AND (?::uuid IS NULL OR v.id=?)
                ORDER BY v.created_at DESC,v.id DESC LIMIT 1
                """,tenant,id,claim,policy.version(),report.version(),item.generation(),report.revision(),exact,exact);
             var rows=sql.executeQuery()) {
            if (!rows.next()) throw new Failures.Conflict();
            return new Authorization(rows.getObject(1,UUID.class),instant(rows,2));
        }
    }

    private static Redemption denied(Connection db,UUID tenant,UUID id,VerifiedIdentity actor) throws SQLException {
        String event=redemptionLimited(db,tenant,id,actor)?"REDEEM_THROTTLED":"REDEEM_FAILED";
        activity(db,tenant,id,null,actor,event,null,null);
        return new Redemption(false,null);
    }

    private static boolean redemptionLimited(Connection db,UUID tenant,UUID id,VerifiedIdentity actor) throws SQLException {
        return limited(db,tenant,null,actor.reference(),"REDEEM_FAILED",20)
                || (id!=null && limited(db,tenant,id,null,"REDEEM_FAILED",30));
    }

    private static boolean challengeLocked(Connection db,UUID tenant,UUID intake,VerifiedIdentity actor) throws SQLException {
        return limited(db,tenant,intake,null,"CHALLENGE_FAILED",8)
                || limited(db,tenant,null,actor.reference(),"CHALLENGE_FAILED",3);
    }

    private static boolean limited(Connection db,UUID tenant,UUID intake,String actor,String event,int maximum)
            throws SQLException {
        try (var sql=q(db,"""
                SELECT count(*)>=? FROM (SELECT 1 FROM dzoka.recovery_activity
                  WHERE tenant_id=? AND event=? AND occurred_at>clock_timestamp()-interval '24 hours'
                    AND (?::uuid IS NULL OR intake_id=?) AND (?::text IS NULL OR actor_ref=?) LIMIT ?) recent
                """,maximum,tenant,event,intake,intake,actor,actor,maximum);var rows=sql.executeQuery()) {
            rows.next();
            return rows.getBoolean(1);
        }
    }

    private static Intake locked(Connection db,UUID tenant,UUID id) throws SQLException {
        lockTenant(db,tenant);
        Intake item=intake(db,tenant,id);
        lockReport(db,tenant,item.report());
        return item;
    }

    private static Intake intake(Connection db,UUID tenant,UUID id) throws SQLException {
        Intake item=maybeIntake(db,tenant,id);
        if (item==null) throw new Failures.NotFound();
        return item;
    }

    private static Intake maybeIntake(Connection db,UUID tenant,UUID id) throws SQLException {
        try (var sql=q(db,"""
                SELECT report_id,site_id,custodian_ref,generation,blocked,returned FROM dzoka.recovery_intakes
                WHERE tenant_id=? AND id=?
                """,tenant,id);var row=sql.executeQuery()) {
            if (!row.next()) return null;
            return new Intake(row.getObject(1,UUID.class),row.getObject(2,UUID.class),row.getString(3),
                    row.getInt(4),row.getBoolean(5),row.getBoolean(6));
        }
    }

    private static Report report(Connection db,UUID id) throws SQLException {
        try (var sql=q(db,"SELECT revision,version,available FROM dzoka.recovery_report_status(?)",id);
             var rows=sql.executeQuery()) {
            if (!rows.next()) throw new Failures.NotFound();
            return new Report(rows.getInt(1),rows.getLong(2),rows.getBoolean(3));
        }
    }

    private static Report available(Connection db,UUID id,Intake item) throws SQLException {
        Report report=report(db,id);
        requireAvailable(report,item);
        return report;
    }

    private static void requireAvailable(Report report,Intake item) {
        if (!report.available() || item.blocked() || item.returned()) throw new Failures.Conflict();
    }

    private static StoredClaim requiredClaim(Connection db,UUID tenant,UUID intake,UUID id) throws SQLException {
        StoredClaim result=claim(db,tenant,intake,id);
        if (result==null) throw new Failures.NotFound();
        return result;
    }

    private static StoredClaim owned(Connection db,UUID tenant,UUID intake,UUID id,VerifiedIdentity actor)
            throws SQLException {
        StoredClaim result=requiredClaim(db,tenant,intake,id);
        if (!actor.reference().equals(result.actor())) throw new Failures.NotFound();
        return result;
    }

    private static StoredClaim claim(Connection db,UUID tenant,UUID intake,UUID id) throws SQLException {
        try (var sql=q(db,"""
                SELECT capacity,state,actor_ref FROM dzoka.ownership_claims WHERE tenant_id=? AND id=? AND intake_id=?
                """,tenant,id,intake);var rows=sql.executeQuery()) {
            if (!rows.next()) return null;
            return new StoredClaim(new Claim(id,intake,Capacity.valueOf(rows.getString(1)),rows.getString(2)),rows.getString(3));
        }
    }

    private static Claim changeClaim(Connection db,UUID tenant,UUID intake,StoredClaim claim,VerifiedIdentity actor,
                                     String state,Reason reason) throws SQLException {
        if (execute(db,"UPDATE dzoka.ownership_claims SET state=? WHERE tenant_id=? AND id=? AND state=?",
                state,tenant,claim.view().id(),claim.view().state())!=1) throw new Failures.Conflict();
        activity(db,tenant,intake,claim.view().id(),actor,"CLAIM_"+state,claim.view().id(),reason==null?null:reason.name());
        return new Claim(claim.view().id(),intake,claim.view().capacity(),state);
    }

    private static boolean competing(Connection db,UUID tenant,UUID intake,UUID claim) throws SQLException {
        try (var sql=q(db,"""
                SELECT 1 FROM dzoka.ownership_claims WHERE tenant_id=? AND intake_id=? AND id<>?
                  AND state IN ('APPROVED','PENDING') LIMIT 1
                """,tenant,intake,claim)) { return exists(sql); }
    }

    private static ClaimRiskScore risk(Connection db,UUID tenant,UUID intake,StoredClaim subject,boolean blocked)
            throws SQLException {
        try (var query=q(db,"""
                SELECT
                  (SELECT count(*) FROM dzoka.recovery_activity WHERE tenant_id=? AND actor_ref=?
                    AND occurred_at>clock_timestamp()-interval '24 hours' AND event='CHALLENGE_FAILED'),
                  (SELECT count(*) FROM dzoka.recovery_activity WHERE tenant_id=? AND intake_id=?
                    AND occurred_at>clock_timestamp()-interval '24 hours' AND event='CHALLENGE_FAILED'),
                  (SELECT count(*) FROM dzoka.ownership_claims WHERE tenant_id=? AND intake_id=? AND id<>?
                    AND state IN ('PENDING','APPROVED'))
                """,tenant,subject.actor(),tenant,intake,tenant,intake,subject.view().id());
             var rows=query.executeQuery()) {
            rows.next();
            return ClaimRiskScore.assess(rows.getInt(1),rows.getInt(2),rows.getInt(3),blocked);
        }
    }

    private static void pending(StoredClaim claim) {
        if (!claim.view().state().equals("PENDING")) throw new Failures.Conflict();
    }

    private static Session session(Connection db,UUID tenant,UUID intake,UUID claim,UUID session) throws SQLException {
        try (var sql=q(db,"""
                SELECT f.kind,f.report_revision,f.observer_ref,s.generation,s.policy_version,s.report_version,
                  s.expires_at,s.expires_at>clock_timestamp(),s.outcome,f.answer_mac
                FROM dzoka.ownership_sessions s
                JOIN dzoka.private_recovery_facts f ON (f.tenant_id,f.id)=(s.tenant_id,s.fact_id)
                LEFT JOIN dzoka.recovery_fact_withdrawals w ON (w.tenant_id,w.fact_id)=(f.tenant_id,f.id)
                WHERE s.tenant_id=? AND s.id=? AND s.intake_id=? AND s.claim_id=? AND w.fact_id IS NULL
                """,tenant,session,intake,claim);var rows=sql.executeQuery()) {
            if (!rows.next()) return null;
            return new Session(FactKind.valueOf(rows.getString(1)),rows.getInt(2),rows.getString(3),rows.getInt(4),
                    rows.getInt(5),rows.getLong(6),instant(rows,7),rows.getBoolean(8),rows.getString(9),rows.getString(10));
        }
    }

    private static void currentSession(Session session,Report report,Intake item,Policy policy) {
        if (!session.active() || session.generation()!=item.generation()
                || session.revision()!=report.revision() || session.reportVersion()!=report.version()
                || session.policy()!=policy.version()) throw new Failures.Conflict();
    }

    private static Policy requirePolicy(Connection db,UUID tenant) throws SQLException {
        Policy policy=currentPolicy(db,tenant);
        if (policy==null) throw new Failures.Conflict();
        return policy;
    }

    private static Policy currentPolicy(Connection db,UUID tenant) throws SQLException {
        try (var sql=q(db,"""
                SELECT version,dual_control,allow_representatives FROM dzoka.recovery_policies
                WHERE tenant_id=? ORDER BY version DESC LIMIT 1
                """,tenant);var rows=sql.executeQuery()) {
            return rows.next()?new Policy(rows.getInt(1),rows.getBoolean(2),rows.getBoolean(3)):null;
        }
    }

    private static void lockTenant(Connection db,UUID tenant) throws SQLException {
        try (var sql=q(db,"SELECT pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||?::text,17))",tenant)) { sql.execute(); }
    }

    private static void lockReport(Connection db,UUID tenant,UUID report) throws SQLException {
        try (var sql=q(db,"SELECT dzoka.lock_recovery_report(?,?)",tenant,report)) { sql.execute(); }
    }

    private static void activity(Connection db,UUID tenant,UUID intake,UUID claim,VerifiedIdentity actor,String event,
                                 UUID resource,String reason) throws SQLException {
        execute(db,"""
                INSERT INTO dzoka.recovery_activity(tenant_id,id,intake_id,claim_id,actor_ref,event,resource_id,reason)
                VALUES(?,?,?,?,?,?,?,?)
                """,tenant,UUID.randomUUID(),intake,claim,actor.reference(),event,resource,reason);
    }

    private static PreparedStatement q(Connection db,String text,Object... values) throws SQLException {
        var statement=db.prepareStatement(text);
        try {
            for (int i=0;i<values.length;i++) statement.setObject(i+1,values[i]);
            return statement;
        } catch (SQLException failure) {
            statement.close();
            throw failure;
        }
    }

    private static int execute(Connection db,String text,Object... values) throws SQLException {
        try (var sql=q(db,text,values)) { return sql.executeUpdate(); }
    }

    private static boolean exists(PreparedStatement query) throws SQLException {
        try (var rows=query.executeQuery()) { return rows.next(); }
    }

    private static Instant instant(ResultSet row,int column) throws SQLException { return row.getTimestamp(column).toInstant(); }
    private static void checkDigest(String value) { if (!Recovery.digest(value)) throw new IllegalArgumentException("Invalid digest"); }
    private static void required(Object... values) {
        for (Object value:values) if (value==null) throw new IllegalArgumentException("Required recovery input");
    }
}
