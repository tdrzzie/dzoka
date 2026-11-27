package org.dzoka.places.api;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.places.application.Places;
import org.dzoka.places.domain.*;
import org.dzoka.places.ports.PlaceCatalog;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/tenants/{tenant}/places")
public final class PlaceController {
    private final Places places;public PlaceController(Places places){this.places=places;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    @PostMapping public PlaceView create(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") String key,@RequestBody PlaceDraft body){return places.save(actor(jwt),tenant,null,body,key);}
    @PutMapping("/{id}") public PlaceView update(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") String key,@RequestBody PlaceDraft body){return places.save(actor(jwt),tenant,id,body,key);}
    @GetMapping public PlaceCatalog.Page page(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID after,@RequestParam(defaultValue="50") int limit,@RequestParam(required=false) String label,@RequestParam(defaultValue="false") boolean includeInactive){return places.page(actor(jwt),tenant,after,limit,label,includeInactive);}
    @GetMapping("/{id}") public PlaceView get(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return places.get(actor(jwt),tenant,id,null);}
    @GetMapping("/{id}/revisions/{version}") public PlaceView revision(@PathVariable UUID tenant,@PathVariable UUID id,@PathVariable long version,@AuthenticationPrincipal Jwt jwt){return places.get(actor(jwt),tenant,id,version);}
    @GetMapping("/{id}/ancestors") public List<PlaceView> ancestors(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) Long version){return places.ancestors(actor(jwt),tenant,id,version);}
}
