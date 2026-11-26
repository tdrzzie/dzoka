package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import org.dzoka.edge.EdgeInvestigations;
import org.junit.jupiter.api.Test;

class EdgeWindowTest {
    private EdgeInvestigations.Observation observation(int second,String status,String processor) {
        var time=Instant.parse("2026-11-26T12:00:00Z").plusSeconds(second);
        return new EdgeInvestigations.Observation(time,time,"UNKNOWN".equals(status),"a".repeat(64),processor,status);
    }
    @Test void gapsAndChangedProvidersCannotBecomeDisappearanceEvidence() {
        var present=observation(0,"PRESENT","b".repeat(64));
        var missing=observation(2,"NOT_OBSERVED","b".repeat(64));
        assertEquals(1,EdgeInvestigations.windows(List.of(present,missing)).size());
        assertTrue(EdgeInvestigations.windows(List.of(present,observation(1,"UNKNOWN","b".repeat(64)),missing)).isEmpty());
        assertTrue(EdgeInvestigations.windows(List.of(present,observation(2,"NOT_OBSERVED","c".repeat(64)))).isEmpty());
        assertTrue(EdgeInvestigations.windows(List.of(missing,present)).isEmpty());
    }
    @Test void intervalObservationsAndPresentToPresentHaveNoRemovalHypothesis() {
        var present=observation(0,"PRESENT","b".repeat(64));
        var interval=new EdgeInvestigations.Observation(present.after(),present.before().plusSeconds(1),false,
                present.sourceSha(),present.processorSha(),"PRESENT");
        assertTrue(EdgeInvestigations.windows(List.of(interval,observation(2,"NOT_OBSERVED",present.processorSha()))).isEmpty());
        assertTrue(EdgeInvestigations.windows(List.of(present,observation(2,"PRESENT",present.processorSha()))).isEmpty());
    }
}
