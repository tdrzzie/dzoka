package org.dzoka.traceability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class EpcisProjection {
    private static final String CONTEXT="https://ref.gs1.org/standards/epcis/2.0.0/epcis-context.jsonld";
    enum NativeKind { STORED, TRANSFER_ACCEPTED, RETURNED }

    static JsonNode document(ObjectMapper json,UUID object,UUID id,Instant eventTime,NativeKind kind,int mapping,
                             Instant created,JsonNode predecessor) {
        var event=new LinkedHashMap<String,Object>();
        event.put("type","ObjectEvent");
        event.put("eventID",uri(id));
        event.put("eventTime",eventTime.toString());
        event.put("eventTimeZoneOffset","+00:00");
        event.put("epcList",List.of(uri(object)));
        event.put("action","OBSERVE");
        if(mapping==2 && kind!=NativeKind.RETURNED)
            event.put("bizStep",kind==NativeKind.STORED?"storing":"receiving");
        var events=new ArrayList<JsonNode>();
        if(predecessor!=null) {
            var oldEvents=predecessor.path("epcisBody").path("eventList");
            var declaration=(tools.jackson.databind.node.ObjectNode)oldEvents.path(oldEvents.size()-1).deepCopy();
            declaration.set("errorDeclaration",json.valueToTree(Map.of("declarationTime",created.toString(),
                    "reason","incorrect_data","correctiveEventIDs",List.of(uri(id)))));
            events.add(declaration);
        }
        events.add(json.valueToTree(event));
        return json.valueToTree(Map.of("@context",List.of(CONTEXT),"type","EPCISDocument","schemaVersion","2.0",
                "creationDate",created.toString(),"epcisBody",Map.of("eventList",events)));
    }

    private static String uri(UUID id) { return "urn:uuid:"+id; }

    private EpcisProjection() {}
}
