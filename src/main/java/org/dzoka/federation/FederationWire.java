package org.dzoka.federation;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.foundation.application.Failures;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class FederationWire {
    static final int VERSION=1;
    static final int MAX_BYTES=16*1024;
    static final int MAX_PAYLOAD=8*1024;
    static final long VALIDITY=30;
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    record Envelope(int version,String operation,String issuer,String audience,String keyId,UUID request,
                    long issued,long expires,String payload,String signature) {
        Envelope {
            identifier(issuer);identifier(audience);identifier(keyId);
            if(version!=VERSION || request==null || operation==null
                    || !operation.matches("(SEARCH|CAPABILITIES)_(REQUEST|RESPONSE)")
                    || payload==null || signature==null || signature.length()>96)
                throw new IllegalArgumentException("Invalid federation envelope");
        }
    }

    static byte[] encode(Object value) { return JSON.writeValueAsBytes(value); }
    static <T> T decode(byte[] bytes,Class<T> type) {
        try { return JSON.readValue(bytes,type); }
        catch(tools.jackson.core.JacksonException invalid) { throw new IllegalArgumentException("Invalid federation document"); }
    }

    static void identifier(String value) {
        if(value==null || !value.matches("[a-z0-9][a-z0-9-]{0,47}"))
            throw new IllegalArgumentException("Invalid federation identifier");
    }

    static PublicKey publicKey(String value) {
        try { return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(decode64(value,128))); }
        catch(GeneralSecurityException invalid) { throw new IllegalArgumentException("Invalid federation public key"); }
    }

    static PrivateKey privateKey(String value) {
        try { return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(decode64(value,256))); }
        catch(GeneralSecurityException invalid) { throw new IllegalArgumentException("Invalid federation private key"); }
    }

    static Envelope sign(String operation,String issuer,String audience,String key,UUID request,Object body,PrivateKey secret) {
        long time=Instant.now().getEpochSecond();
        byte[] bytes=encode(body);
        if(bytes.length>MAX_PAYLOAD) throw new IllegalArgumentException("Oversized federation payload");
        var header=new Envelope(VERSION,operation,issuer,audience,key,request,time,time+VALIDITY,base64(bytes),"");
        try {
            var signer=Signature.getInstance("Ed25519");
            signer.initSign(secret);
            signer.update(canonical(header,bytes));
            return new Envelope(VERSION,operation,issuer,audience,key,request,time,time+VALIDITY,header.payload(),base64(signer.sign()));
        } catch(GeneralSecurityException failure) { throw new IllegalStateException("Cannot sign federation message",failure); }
    }

    static byte[] verify(Envelope envelope,PublicKey key) {
        long now=Instant.now().getEpochSecond();
        if(envelope.issued()<now-VALIDITY || envelope.issued()>now+5 || envelope.expires()<=now
                || envelope.expires()<=envelope.issued() || envelope.expires()>envelope.issued()+VALIDITY)
            throw new Failures.Forbidden();
        byte[] body=decode64(envelope.payload(),MAX_PAYLOAD);
        byte[] signature=decode64(envelope.signature(),64);
        try {
            var verifier=Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(canonical(envelope,body));
            if(signature.length!=64 || !verifier.verify(signature)) throw new Failures.Forbidden();
            return body;
        } catch(GeneralSecurityException invalid) { throw new Failures.Forbidden(); }
    }

    private static byte[] canonical(Envelope header,byte[] body) {
        return String.join("\n","dzoka-federation",Integer.toString(header.version()),header.operation(),header.issuer(),
                header.audience(),header.keyId(),header.request().toString(),Long.toString(header.issued()),
                Long.toString(header.expires()),ContentDigest.sha256(body)).getBytes(StandardCharsets.US_ASCII);
    }

    private static String base64(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }

    private static byte[] decode64(String value,int max) {
        if(value==null || value.length()>((max+2)/3)*4) throw new IllegalArgumentException("Invalid federation encoding");
        byte[] bytes=Base64.getUrlDecoder().decode(value);
        if(bytes.length>max || !base64(bytes).equals(value)) throw new IllegalArgumentException("Invalid federation encoding");
        return bytes;
    }

    private FederationWire() {}
}
