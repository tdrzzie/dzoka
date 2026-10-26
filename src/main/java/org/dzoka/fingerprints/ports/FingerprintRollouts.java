package org.dzoka.fingerprints.ports;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
public interface FingerprintRollouts {
    Activation active(VerifiedIdentity actor,UUID tenant);
    Status create(VerifiedIdentity actor,UUID tenant,Plan plan);
    Status read(VerifiedIdentity actor,UUID tenant,UUID id);
    Status seal(VerifiedIdentity actor,UUID tenant,UUID id);
    Activation activate(VerifiedIdentity actor,UUID tenant,UUID id,long expectedVersion,boolean rollback);
}
