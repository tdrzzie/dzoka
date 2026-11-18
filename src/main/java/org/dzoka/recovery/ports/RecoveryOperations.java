package org.dzoka.recovery.ports;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.recovery.domain.Recovery.*;

public interface RecoveryOperations {
    Policy policy(VerifiedIdentity actor,UUID tenant,int version,boolean dualControl,boolean representatives);
    void enroll(VerifiedIdentity actor,UUID tenant,UUID intake,UUID foundReport,UUID site);
    void hold(VerifiedIdentity actor,UUID tenant,UUID intake,boolean blocked);
    void fact(VerifiedIdentity actor,UUID tenant,UUID intake,UUID fact,FactKind kind,String sourceSha,String answer,boolean privateConfirmed);
    void withdrawFact(VerifiedIdentity actor,UUID tenant,UUID intake,UUID fact,FactWithdrawal reason);
    Claim submit(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim,Capacity capacity);
    Claim read(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim);
    Claim withdraw(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim);
    Challenge challenge(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim,UUID session);
    Assessment answer(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim,UUID session,String answer);
    ReviewContext reviewContext(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim);
    Claim review(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim,UUID review,Review decision);
    Claim revoke(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim,Reason reason);
    Grant issue(VerifiedIdentity actor,UUID tenant,UUID intake,UUID claim,UUID grant,String audienceRef);
    PickupContext pickupContext(VerifiedIdentity actor,UUID tenant,UUID intake,UUID grant,String token);
    Redemption redeem(VerifiedIdentity actor,UUID tenant,UUID intake,UUID grant,UUID request,String token,String recipientRef,String confirmationSha);
}
