package org.dzoka.fulfillment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController
public final class FulfillmentWebhook {
    private final byte[] key;
    private final UUID tenant;
    private final VerifiedIdentity worker;
    private final FulfillmentOperations operations;
    private final ObjectMapper json;
    public FulfillmentWebhook(Environment env,FulfillmentOperations operations,ObjectMapper json) {
        String secret=env.getProperty("DZOKA_FULFILLMENT_WEBHOOK_KEY","");
        if(!secret.isEmpty() && !secret.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Webhook key must be 32 bytes");
        key=secret.isEmpty()?null:HexFormat.of().parseHex(secret);
        tenant=key==null?null:UUID.fromString(env.getRequiredProperty("DZOKA_FULFILLMENT_TENANT"));
        worker=key==null?null:new VerifiedIdentity(env.getRequiredProperty("DZOKA_FULFILLMENT_ISSUER"),env.getRequiredProperty("DZOKA_FULFILLMENT_SUBJECT"));
        this.operations=operations;this.json=json;
    }
    public static boolean verify(byte[] key,String timestamp,String signature,byte[] raw,Instant now) {
        if(key==null || timestamp==null || !timestamp.matches("[0-9]{10,12}") || signature==null || !signature.matches("[0-9a-f]{64}") || raw.length>16384)return false;
        try {
            long seconds=Long.parseLong(timestamp);
            if(Math.abs(now.getEpochSecond()-seconds)>300)return false;
            var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
            mac.update((timestamp+"\n").getBytes(StandardCharsets.US_ASCII));
            return MessageDigest.isEqual(mac.doFinal(raw),HexFormat.of().parseHex(signature));
        } catch(Exception invalid) { return false; }
    }
    @PostMapping(value="/api/v1/hooks/fulfillment",consumes="application/json")
    public ResponseEntity<Void> observe(@RequestHeader(value="X-Dzoka-Timestamp",required=false) String timestamp,
                                      @RequestHeader(value="X-Dzoka-Signature",required=false) String signature,@RequestBody byte[] raw) {
        if(!verify(key,timestamp,signature,raw,Instant.now()))throw new Failures.Forbidden();
        FulfillmentOperations.Callback callback;
        try {callback=json.readValue(raw,FulfillmentOperations.Callback.class);}
        catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid fulfillment callback");}
        if(callback==null)throw new IllegalArgumentException("Callback required");
        if(!tenant.equals(callback.tenant()))throw new Failures.Forbidden();
        operations.callback(worker,tenant,callback,ContentDigest.sha256(raw));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }
}
