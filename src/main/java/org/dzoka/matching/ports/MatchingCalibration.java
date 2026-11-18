package org.dzoka.matching.ports;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Calibration.Approval;
import org.dzoka.matching.domain.Calibration.Draft;

public interface MatchingCalibration {
    void propose(VerifiedIdentity actor,UUID tenant,UUID artifact,Draft draft);
    Draft draft(VerifiedIdentity actor,UUID tenant,UUID artifact);
    void approve(VerifiedIdentity actor,UUID tenant,UUID artifact,Approval approval);
    void revoke(VerifiedIdentity actor,UUID tenant,UUID artifact,String reviewSha256);
}
