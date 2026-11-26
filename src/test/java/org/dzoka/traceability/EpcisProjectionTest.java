package org.dzoka.traceability;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class EpcisProjectionTest {
    @Test void acceptedTransferAndOwnerReturnKeepTheirDistinctObservations() {
        var json=new ObjectMapper();
        var item=UUID.fromString("00000000-0000-4000-8000-000000000001");
        var id=UUID.fromString("00000000-0000-4000-8000-000000000002");
        var observed=Instant.parse("2026-11-20T14:12:33Z");
        var recorded=Instant.parse("2026-11-21T08:05:00Z");
        var receiving=EpcisProjection.document(json,item,id,observed,EpcisProjection.NativeKind.TRANSFER_ACCEPTED,2,recorded,null);
        var returned=EpcisProjection.document(json,item,id,observed,EpcisProjection.NativeKind.RETURNED,2,recorded,null);

        var transferredEvent=receiving.path("epcisBody").path("eventList").path(0);
        var returnedEvent=returned.path("epcisBody").path("eventList").path(0);
        assertEquals("receiving",transferredEvent.path("bizStep").asString());
        assertFalse(returnedEvent.has("bizStep"));
        assertEquals("OBSERVE",returnedEvent.path("action").asString());
        assertEquals("urn:uuid:"+item,returnedEvent.path("epcList").path(0).asString());
        assertEquals(observed.toString(),returnedEvent.path("eventTime").asString());
        assertEquals(recorded.toString(),returned.path("creationDate").asString());
    }
}
