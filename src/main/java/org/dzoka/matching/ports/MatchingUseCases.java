package org.dzoka.matching.ports;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Matching.Search;

public interface MatchingUseCases {
    Search search(VerifiedIdentity actor, UUID tenant, UUID report, UUID space, int limit);
    Search recordSearch(VerifiedIdentity actor, UUID tenant, UUID report, UUID space, int limit, UUID run);
    Search readSearch(VerifiedIdentity actor, UUID tenant, UUID report, UUID run);
    record Projection(UUID space, Long activationVersion, int activeComponents) {}
    Projection reconcile(VerifiedIdentity actor, UUID tenant, UUID space);
    int pruneHistory(VerifiedIdentity actor, UUID tenant);
    void attest(VerifiedIdentity actor, UUID tenant, UUID report, int revision, int keyVersion,
                String scopeDigest, String digest, UUID attestation);
}
