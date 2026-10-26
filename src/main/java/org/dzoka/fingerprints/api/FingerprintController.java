package org.dzoka.fingerprints.api;
import java.util.*;
import org.dzoka.fingerprints.domain.*;
import org.dzoka.fingerprints.ports.FingerprintJobs;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}")
public final class FingerprintController {
    private final FingerprintJobs jobs;
    public FingerprintController(FingerprintJobs jobs){this.jobs=jobs;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    public record Request(UUID spaceId){public Request{Objects.requireNonNull(spaceId);}}
    public record Failed(UUID lease,String reason){}
    @PostMapping("/reports/{report}/fingerprints") public InferenceJob request(@PathVariable UUID tenant,@PathVariable UUID report,
            @AuthenticationPrincipal Jwt jwt,@RequestBody Request body){return jobs.enqueue(actor(jwt),tenant,report,body.spaceId());}
    @GetMapping("/reports/{report}/fingerprints") public ItemFingerprint current(@PathVariable UUID tenant,@PathVariable UUID report,
            @AuthenticationPrincipal Jwt jwt){return jobs.snapshot(actor(jwt),tenant,report);}
    @GetMapping("/ai/jobs/{job}") public InferenceJob job(@PathVariable UUID tenant,@PathVariable UUID job,@AuthenticationPrincipal Jwt jwt){return jobs.read(actor(jwt),tenant,job);}
    @PostMapping("/internal/ai/jobs/claim") public Optional<LeasedInput> claim(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt,@RequestBody Request body){
        return jobs.claim(actor(jwt),tenant,body.spaceId());}
    @PostMapping("/internal/ai/jobs/{job}/complete") public boolean complete(@PathVariable UUID tenant,@PathVariable UUID job,
            @AuthenticationPrincipal Jwt jwt,@RequestBody EmbeddingResult body){return jobs.complete(actor(jwt),tenant,job,body);}
    @PostMapping("/internal/ai/jobs/{job}/fail") public boolean fail(@PathVariable UUID tenant,@PathVariable UUID job,
            @AuthenticationPrincipal Jwt jwt,@RequestBody Failed body){return jobs.fail(actor(jwt),tenant,job,body.lease(),body.reason());}
}
