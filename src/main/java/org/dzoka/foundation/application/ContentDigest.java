package org.dzoka.foundation.application;
import java.security.*;
import java.util.HexFormat;
public final class ContentDigest {
    private ContentDigest(){}
    public static String sha256(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
