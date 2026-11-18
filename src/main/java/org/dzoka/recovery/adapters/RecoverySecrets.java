package org.dzoka.recovery.adapters;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.dzoka.foundation.application.Failures;
import org.dzoka.recovery.domain.Recovery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class RecoverySecrets {
    private final SecretKeySpec key;
    private final SecureRandom random=new SecureRandom();

    RecoverySecrets(@Value("${dzoka.recovery.hmac-key:}") String secret) {
        if (!secret.isEmpty() && !Recovery.digest(secret)) throw new IllegalArgumentException("Invalid recovery key configuration");
        key=secret.isEmpty()?null:new SecretKeySpec(HexFormat.of().parseHex(secret),"HmacSHA256");
    }

    String nonce() {
        byte[] nonce=new byte[32];
        random.nextBytes(nonce);
        return HexFormat.of().formatHex(nonce);
    }

    String token(UUID tenant,UUID grant,String nonce) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac("pickup-token",tenant,grant,nonce));
    }

    String digest(String purpose,UUID tenant,UUID id,String value) {
        return HexFormat.of().formatHex(mac(purpose,tenant,id,value));
    }

    static boolean equal(String first,String second) {
        return MessageDigest.isEqual(HexFormat.of().parseHex(first),HexFormat.of().parseHex(second));
    }

    private byte[] mac(String purpose,UUID tenant,UUID id,String value) {
        if (key==null) throw new Failures.Unavailable(new IllegalStateException("Recovery key unavailable"));
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal((purpose+"\n"+tenant+"\n"+id+"\n"+value).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
