package org.dzoka.fingerprints.domain;
import java.util.*;

/** Current authorized view over immutable components; coverage is separate from similarity or identity. */
public record ItemFingerprint(int schemaVersion,UUID tenantId,UUID itemId,UUID reportId,int sourceRevision,
        List<FingerprintComponent> components,Map<String,EvidenceState> coverage) {
    public enum EvidenceState { AVAILABLE,PENDING,UNAVAILABLE,FAILED }
    public ItemFingerprint {
        components=List.copyOf(components);coverage=Map.copyOf(coverage);
        for(var component:components)if(!component.itemId().equals(itemId) || !component.reportId().equals(reportId) || component.sourceRevision()!=sourceRevision)
            throw new IllegalArgumentException("Mixed fingerprint sources");
    }
}
