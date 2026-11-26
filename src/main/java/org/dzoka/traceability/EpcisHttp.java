package org.dzoka.traceability;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.dzoka.foundation.http.BoundedPeerHttp;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
final class EpcisHttp implements TraceabilityProvider {
    private final URI origin;
    private final String token;
    private final ObjectMapper json;

    EpcisHttp(Environment env,ObjectMapper json) {
        String configured=env.getProperty("DZOKA_EPCIS_ORIGIN","");
        boolean fixture=env.getProperty("DZOKA_EPCIS_TEST_LOOPBACK",Boolean.class,false);
        origin=configured.isBlank()?null:BoundedPeerHttp.origin(configured,fixture);
        token=env.getProperty("DZOKA_EPCIS_TOKEN","");
        if(origin!=null && (token.isBlank() || token.chars().anyMatch(Character::isISOControl)))
            throw new IllegalArgumentException("A repository token is required");
        this.json=json;
    }

    @Override public boolean enabled() { return origin!=null; }
    @Override public String repository() {
        return org.dzoka.foundation.application.ContentDigest.sha256(origin.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Override public Result capture(UUID id,byte[] document) {
        try {
            var response=BoundedPeerHttp.exchange(origin,"/capture",document,Map.of("Authorization","Bearer "+token,
                    "Content-Type","application/json","GS1-EPCIS-Version","2.0.0",
                    "GS1-Capture-Error-Behaviour","rollback","Idempotency-Key",id.toString()),16384);
            if(response.status()==202) {
                String receipt=receipt(response.location());
                return receipt==null?new Result(State.UNKNOWN,null):new Result(State.SUBMITTED,receipt);
            }
            if(response.status()==429) return new Result(State.RETRY,null);
            if(response.status()>=400 && response.status()<500 && response.status()!=408 && response.status()!=409)
                return new Result(State.REJECTED,null);
            return new Result(State.UNKNOWN,null);
        } catch(IOException | RuntimeException uncertain) { return new Result(State.UNKNOWN,null); }
    }

    @Override public Result status(String receipt) {
        if(!validReceipt(receipt)) return new Result(State.UNKNOWN,null);
        try {
            var response=BoundedPeerHttp.exchange(origin,"/capture/"+receipt,null,Map.of("Authorization","Bearer "+token,
                    "Accept","application/json","GS1-EPCIS-Version","2.0.0"),16384);
            if(response.status()!=200) return new Result(State.RETRY,receipt);
            var job=json.readTree(response.body());
            if(job.path("running").isBoolean() && job.path("running").asBoolean()) return new Result(State.RUNNING,receipt);
            if(!job.path("running").isBoolean() || !job.path("success").isBoolean()
                    || !"rollback".equals(job.path("captureErrorBehaviour").asString()))
                return new Result(State.RETRY,receipt);
            return new Result(job.path("success").asBoolean()?State.COMPLETE:State.REJECTED,receipt);
        } catch(IOException | RuntimeException unavailable) { return new Result(State.RETRY,receipt); }
    }

    private String receipt(String location) {
        if(location==null) return null;
        URI resolved=origin.resolve(location);
        if(!origin.getScheme().equals(resolved.getScheme()) || !origin.getRawAuthority().equals(resolved.getRawAuthority())
                || resolved.getRawFragment()!=null || resolved.getRawQuery()!=null) return null;
        String path=resolved.getRawPath();
        if(!path.startsWith("/capture/")) return null;
        String id=path.substring("/capture/".length());
        return validReceipt(id)?id:null;
    }

    static boolean validReceipt(String id) { return id!=null && id.matches("[A-Za-z0-9_-]{1,80}"); }
}
