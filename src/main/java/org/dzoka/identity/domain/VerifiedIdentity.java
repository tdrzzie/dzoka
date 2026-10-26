package org.dzoka.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Created at the authenticated boundary; never deserialize this type from a request body. */
public record VerifiedIdentity(String issuer, String subject) {
    public VerifiedIdentity {
        if (issuer == null || issuer.isBlank() || issuer.length() > 2048
                || subject == null || subject.isBlank() || subject.length() > 512
                || issuer.indexOf('\0') >= 0 || subject.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid verified identity");
        }
    }
    public String reference() { return digest(issuer + "\0" + subject); }
    public static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
