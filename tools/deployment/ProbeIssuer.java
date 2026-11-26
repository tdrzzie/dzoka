import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Disposable deployment test issuer. Never included in the Core image or production configuration. */
public final class ProbeIssuer {
    public static void main(String[] args)throws Exception {
        Path folder=Path.of(args[1]);
        if(args[0].equals("init")){
            var key=new RSAKeyGenerator(2048).keyID("ephemeral-deployment").generate();Files.writeString(folder.resolve("private-test-jwk.json"),key.toJSONString());
            var claims=new JWTClaimsSet.Builder().issuer("http://127.0.0.1:19090").subject("deployment").audience("dzoka-core").issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(1800))).build();
            var token=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);token.sign(new RSASSASigner(key));Files.writeString(folder.resolve("test-token"),token.serialize());return;
        }
        RSAKey key=RSAKey.parse(Files.readString(folder.resolve("private-test-jwk.json")));byte[] keys=new JWKSet(key.toPublicJWK()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",19090),0);server.createContext("/jwks",e->{e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,keys.length);try(var out=e.getResponseBody()){out.write(keys);}});server.start();
    }
}
