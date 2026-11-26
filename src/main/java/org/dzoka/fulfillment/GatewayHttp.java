package org.dzoka.fulfillment;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.http.BoundedPeerHttp;
import org.dzoka.logistics.CourierProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class GatewayHttp implements CourierProvider,org.dzoka.rewards.PaymentProvider {
    private final URI origin;
    private final String token;
    private final ObjectMapper json;
    public GatewayHttp(Environment env,ObjectMapper json) {
        String configured=env.getProperty("DZOKA_FULFILLMENT_ORIGIN","");
        origin=configured.isBlank()?null:BoundedPeerHttp.origin(configured,env.getProperty("DZOKA_FULFILLMENT_TEST_LOOPBACK",Boolean.class,false));
        token=env.getProperty("DZOKA_FULFILLMENT_TOKEN","");
        if(origin!=null && (token.isBlank() || token.length()>4096 || token.chars().anyMatch(Character::isISOControl)))
            throw new IllegalArgumentException("Fulfillment gateway credential required");
        this.json=json;
    }
    @Override public boolean enabled() { return origin!=null; }
    @Override public Result send(UUID tenant,UUID operation,String kind,String payload) {
        if(!enabled()) throw new Failures.Unavailable(new IllegalStateException("Fulfillment gateway disabled"));
        try {
            byte[] body=json.writeValueAsBytes(Map.of("schema",1,"tenant",tenant,"operation",operation,"kind",kind,"payload",json.readTree(payload)));
            var response=BoundedPeerHttp.exchange(origin,"/v1/operations",body,headers(tenant,operation),16384);
            return response.status()==200?result(response.body(),tenant,operation):new Result(State.UNKNOWN,null);
        } catch(Exception failure) { return new Result(State.UNKNOWN,null); }
    }
    @Override public Result reconcile(UUID tenant,UUID operation) {
        if(!enabled()) throw new Failures.Unavailable(new IllegalStateException("Fulfillment gateway disabled"));
        try {
            var response=BoundedPeerHttp.exchange(origin,"/v1/operations/"+operation,null,headers(tenant,operation),16384);
            return response.status()==200?result(response.body(),tenant,operation):new Result(State.UNKNOWN,null);
        } catch(Exception failure) { return new Result(State.UNKNOWN,null); }
    }
    private Result result(byte[] raw,UUID tenant,UUID operation) {
        var node=json.readTree(raw);
        if(node.size()!=5 || node.path("schema").asInt()!=1 || !tenant.toString().equals(node.path("tenant").asString())
                || !operation.toString().equals(node.path("operation").asString())) return new Result(State.UNKNOWN,null);
        State state=State.valueOf(node.path("state").asString());
        String reference=node.path("reference").isNull()?null:Sql.opaque(node.path("reference").asString());
        if(state==State.CONFIRMED && reference==null) return new Result(State.UNKNOWN,null);
        return new Result(state,reference);
    }
    private Map<String,String> headers(UUID tenant,UUID operation) {
        return Map.of("Authorization","Bearer "+token,"Content-Type","application/json","Accept","application/json",
                "Idempotency-Key",operation.toString(),"Dzoka-Fulfillment-Version","1","Dzoka-Tenant",tenant.toString());
    }
    @Override public Quote quote(UUID tenant,String deliveryReference) {
        if(!enabled()) throw new Failures.Unavailable(new IllegalStateException("Fulfillment gateway disabled"));
        Sql.opaque(deliveryReference);
        try {
            byte[] request=json.writeValueAsBytes(Map.of("schema",1,"tenant",tenant,"deliveryReference",deliveryReference));
            var response=BoundedPeerHttp.exchange(origin,"/v1/quotes",request,headers(tenant,UUID.randomUUID()),16384);
            if(response.status()!=200) throw new IllegalStateException();
            var node=json.readTree(response.body());
            long amount=node.path("minorUnits").asLong(-1);
            String currency=node.path("currency").asString();
            if(!node.path("minorUnits").isIntegralNumber() || amount<0 || amount>1_000_000_000_000L || currency==null || !currency.matches("[A-Z]{3}")) throw new IllegalStateException();
            return new Quote(Sql.opaque(node.path("reference").asString()),currency,amount);
        } catch(Exception failure) { throw new Failures.Unavailable(new IllegalStateException("Quote unavailable")); }
    }
}
