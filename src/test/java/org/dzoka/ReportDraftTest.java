package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
class ReportDraftTest {
    @Test void unknownEvidenceIsNullableAndZeroCoordinatesRemainObserved() {
        var empty=new ReportDraft(ReportDraft.Kind.LOST,null,"sn","Africa/Harare",null,null,null,null,null,null,null);
        assertNull(empty.normalizedText());assertNull(empty.latitude());
        var equator=new ReportDraft(ReportDraft.Kind.FOUND," raw\ninput ","en","UTC",null,null,0d,0d,0d,null,null);
        assertEquals(0d,equator.latitude());assertEquals("raw input",equator.normalizedText());assertEquals(" raw\ninput ",equator.rawText());
        assertThrows(IllegalArgumentException.class,()->new ReportDraft(ReportDraft.Kind.LOST,"text","en","UTC",null,null,0d,null,null,null,null));
        assertThrows(IllegalArgumentException.class,()->new ReportDraft(ReportDraft.Kind.LOST,"text","en","UTC",null,null,Double.NaN,0d,1d,null,null));
        assertThrows(IllegalArgumentException.class,()->new ReportDraft(ReportDraft.Kind.LOST,"text","en","UTC",null,null,null,null,null,Instant.parse("2026-10-26T00:00:00Z"),Instant.parse("2026-10-25T00:00:00Z")));
    }
}
