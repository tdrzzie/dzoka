package org.dzoka.traceability;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/traceability")
public final class TraceabilityController {
    private final Traceability traceability;
    public TraceabilityController(Traceability traceability) { this.traceability=traceability; }
    public record Authorization(Boolean enabled) {}
    public record Capture(String kind,int mapping,UUID predecessor) {}
    public record Reconcile(String receipt,String evidenceSha) {}

    @PutMapping("/intakes/{intake}/authorization") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void authorize(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID intake,
                          @RequestBody Authorization request) {
        if(request.enabled()==null) throw new IllegalArgumentException("Traceability authorization required");
        traceability.authorization(actor(jwt),tenant,intake,request.enabled());
    }

    @PutMapping("/intakes/{intake}/native/{event}") @ResponseStatus(HttpStatus.ACCEPTED)
    public Traceability.View project(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID intake,
                                     @PathVariable UUID event,@RequestBody Capture capture) {
        return traceability.project(actor(jwt),tenant,intake,event,capture.kind(),capture.mapping(),capture.predecessor());
    }

    @GetMapping("/intakes/{intake}/events")
    public ResponseEntity<List<Traceability.View>> query(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,
                                                        @PathVariable UUID intake) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(traceability.query(actor(jwt),tenant,intake));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<Traceability.View> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(traceability.get(actor(jwt),tenant,id));
    }

    @PostMapping("/dispatch")
    public Map<String,String> dispatch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return Map.of("state",traceability.dispatch(actor(jwt),tenant));
    }

    @PostMapping("/project-next")
    public ResponseEntity<Traceability.View> projectNext(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        var view=traceability.projectNext(actor(jwt),tenant);
        return view==null?ResponseEntity.noContent().build():ResponseEntity.accepted().body(view);
    }

    @PutMapping("/events/{id}/receipt") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reconcile(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Reconcile body) {
        traceability.reconcile(actor(jwt),tenant,id,body.receipt(),body.evidenceSha());
    }

    private static VerifiedIdentity actor(Jwt jwt) { return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()); }
}
