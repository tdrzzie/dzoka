package org.dzoka.federation;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.dzoka.foundation.api.BoundedJsonInput;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public final class FederationController {
    private static final String ROOT="/api/v1/tenants/{tenant}/federation";
    private final Federation federation;

    public FederationController(Federation federation) { this.federation=federation; }

    @PostMapping(value=Federation.PEER_ROUTE,consumes="application/json",produces="application/json")
    public ResponseEntity<byte[]> peerExchange(HttpServletRequest request) throws IOException {
        int limit=FederationWire.MAX_BYTES;
        if(request.getContentLengthLong()>limit) throw new BoundedJsonInput.TooLarge();
        byte[] body=request.getInputStream().readNBytes(limit+1);
        if(body.length>limit) throw new BoundedJsonInput.TooLarge();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
                .body(federation.receive(body));
    }

    @GetMapping(ROOT+"/peers")
    public List<Federation.Health> peers(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return federation.health(actor(jwt),tenant);
    }

    @PutMapping(ROOT+"/peers/{peer}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void peer(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable String peer,@RequestBody Federation.Peer body) {
        federation.peer(actor(jwt),tenant,peer,body);
    }

    @PutMapping(ROOT+"/peers/{peer}/keys/{key}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void key(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable String peer,@PathVariable String key,
                    @RequestBody Federation.Key body) {
        federation.key(actor(jwt),tenant,peer,key,body);
    }

    @DeleteMapping(ROOT+"/peers/{peer}/keys/{key}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable String peer,@PathVariable String key) {
        federation.revoke(actor(jwt),tenant,peer,key);
    }

    @PutMapping(ROOT+"/offers/{report}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void offer(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID report,@RequestBody Federation.Offer body) {
        federation.offer(actor(jwt),tenant,report,body);
    }

    @PostMapping(ROOT+"/search")
    public ResponseEntity<Federation.Result> search(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody Federation.Search body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(federation.search(actor(jwt),tenant,body));
    }

    @PostMapping(ROOT+"/peers/{peer}/capabilities")
    public Federation.Capabilities capabilities(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable String peer) {
        return federation.negotiate(actor(jwt),tenant,peer);
    }

    @PostMapping(ROOT+"/prune")
    public Map<String,Integer> prune(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return Map.of("removed",federation.prune(actor(jwt),tenant));
    }

    private static VerifiedIdentity actor(Jwt jwt) { return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()); }
}
