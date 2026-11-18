package org.dzoka.matching.ports;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Matching.Search;

public interface MatchingUseCases {
    Search search(VerifiedIdentity actor, UUID tenant, UUID report, UUID space, int limit);
    void attest(VerifiedIdentity actor, UUID tenant, UUID report, int revision, int keyVersion,
                String scopeDigest, String digest, UUID attestation);
}
