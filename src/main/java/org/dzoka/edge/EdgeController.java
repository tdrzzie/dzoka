package org.dzoka.edge;

import java.time.Instant;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/edge")
public final class EdgeController {
    private final EdgeInvestigations edge;
    private final RecoveryIntelligence intelligence;
    public EdgeController(EdgeInvestigations edge,RecoveryIntelligence intelligence) { this.edge=edge;this.intelligence=intelligence; }
    public record Request(UUID grant) {}
    public record Camera(String archiveSha,Boolean enabled) {}
    public record Access(UUID token) {}
    public record EvidenceApproval(String evidenceSha) {}
    @PutMapping("/jobs/{job}/evidence/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evidence(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID job,@PathVariable UUID id,
                         @RequestBody EdgeInvestigations.Crop body) { edge.evidence(actor(jwt),tenant,job,id,body); }
    @PutMapping("/jobs/{job}/evidence/{id}/approval") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID job,@PathVariable UUID id,
                        @RequestBody EvidenceApproval body) { edge.approveEvidence(actor(jwt),tenant,job,id,body.evidenceSha()); }
    @GetMapping("/jobs/{job}/evidence/{id}")
    public ResponseEntity<byte[]> evidence(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID job,@PathVariable UUID id) {
        return image(edge.evidence(actor(jwt),tenant,job,id,false));
    }
    @GetMapping("/jobs/{job}/evidence/{id}/review")
    public ResponseEntity<byte[]> reviewEvidence(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID job,@PathVariable UUID id) {
        return image(edge.evidence(actor(jwt),tenant,job,id,true));
    }
    private static ResponseEntity<byte[]> image(byte[] bytes) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'").body(bytes);
    }
    @PutMapping("/cameras/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void camera(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Camera body) {
        if(body.enabled()==null) throw new IllegalArgumentException("Camera activation required");
        edge.camera(actor(jwt),tenant,id,body.archiveSha(),body.enabled());
    }
    @PostMapping("/jobs/{id}/access")
    public ResponseEntity<EdgeInvestigations.Lease> access(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,
            @PathVariable UUID id,@RequestBody Access body) {
        if(body.token()==null) throw new IllegalArgumentException("Lease required");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(edge.access(actor(jwt),tenant,id,body.token()));
    }
    @PostMapping("/purge")
    public Map<String,Integer> purge(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return Map.of("purged",edge.purge(actor(jwt),tenant));
    }
    @PutMapping("/grants/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody EdgeInvestigations.Grant body) {
        edge.grant(actor(jwt),tenant,id,body);
    }
    @DeleteMapping("/grants/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) { edge.revoke(actor(jwt),tenant,id); }
    @PutMapping("/jobs/{id}") @ResponseStatus(HttpStatus.ACCEPTED)
    public void submit(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Request body) {
        if(body.grant()==null) throw new IllegalArgumentException("Grant required");edge.submit(actor(jwt),tenant,id,body.grant());
    }
    @PostMapping("/claim")
    public ResponseEntity<EdgeInvestigations.Lease> claim(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        var lease=edge.claim(actor(jwt),tenant);return lease==null?ResponseEntity.noContent().build():ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(lease);
    }
    @PutMapping("/jobs/{id}/result")
    public Map<String,Boolean> complete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody EdgeInvestigations.Completion body) {
        return Map.of("accepted",edge.complete(actor(jwt),tenant,id,body));
    }
    @GetMapping("/jobs/{id}")
    public ResponseEntity<EdgeInvestigations.Result> read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(edge.read(actor(jwt),tenant,id));
    }
    @GetMapping("/intakes/{intake}/history")
    public ResponseEntity<List<RecoveryIntelligence.Edge>> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,
            @PathVariable UUID intake,@RequestParam Instant cutoff) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(intelligence.history(actor(jwt),tenant,intake,cutoff));
    }
    @GetMapping("/intakes/{intake}/baseline")
    public ResponseEntity<RecoveryIntelligence.Baseline> baseline(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,
            @PathVariable UUID intake,@RequestParam UUID site,@RequestParam Instant cutoff) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(intelligence.baseline(actor(jwt),tenant,intake,site,cutoff));
    }
    private static VerifiedIdentity actor(Jwt jwt) { return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()); }
}
