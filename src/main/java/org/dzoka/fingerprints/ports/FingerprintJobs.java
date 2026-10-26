package org.dzoka.fingerprints.ports;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.fingerprints.domain.*;
public interface FingerprintJobs {
    InferenceJob enqueue(VerifiedIdentity actor,UUID tenant,UUID report,UUID space);
    InferenceJob read(VerifiedIdentity actor,UUID tenant,UUID job);
    List<FingerprintComponent> current(VerifiedIdentity actor,UUID tenant,UUID report);
    ItemFingerprint snapshot(VerifiedIdentity actor,UUID tenant,UUID report);
    Optional<LeasedInput> claim(VerifiedIdentity worker,UUID tenant,UUID space);
    boolean complete(VerifiedIdentity worker,UUID tenant,UUID job,EmbeddingResult result);
    boolean fail(VerifiedIdentity worker,UUID tenant,UUID job,UUID lease,String reason);
    int purge(VerifiedIdentity worker,UUID tenant);
}
