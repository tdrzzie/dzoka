package org.dzoka.fingerprints.api;
import java.io.IOException;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.fingerprints.domain.Perception.*;
import org.dzoka.foundation.api.BoundedJsonInput;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}")
public final class PerceptionController {
    private final PerceptionJobs jobs;
    public PerceptionController(PerceptionJobs jobs){this.jobs=jobs;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    public record Image(UUID profileId,UUID mediaId){public Image{Objects.requireNonNull(profileId);Objects.requireNonNull(mediaId);}}
    public record Profile(UUID profileId){public Profile{Objects.requireNonNull(profileId);}}
    public record LeaseToken(UUID lease){public LeaseToken{Objects.requireNonNull(lease);}}
    public record Failure(UUID lease,String reason){}
    @PostMapping("/reports/{report}/perception/image") public Job image(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt,@RequestBody Image body){return jobs.enqueue(actor(jwt),tenant,report,body.profileId(),body.mediaId(),null);}
    @PostMapping("/reports/{report}/perception/text") public Job text(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt,@RequestBody Profile body){return jobs.enqueue(actor(jwt),tenant,report,body.profileId(),null,null);}
    @PostMapping(value="/reports/{report}/perception/audio/{profile}",consumes="audio/wav") public Job audio(@PathVariable UUID tenant,@PathVariable UUID report,@PathVariable UUID profile,@AuthenticationPrincipal Jwt jwt,HttpServletRequest request)throws IOException{
        if(request.getContentLengthLong()>320044)throw new BoundedJsonInput.TooLarge();byte[] bytes=request.getInputStream().readNBytes(320045);
        if(bytes.length>320044)throw new BoundedJsonInput.TooLarge();return jobs.enqueue(actor(jwt),tenant,report,profile,null,bytes);
    }
    @GetMapping("/reports/{report}/perception/components") public List<Component> components(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt){return jobs.current(actor(jwt),tenant,report);}
    @GetMapping("/perception/jobs/{id}") public Job read(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return jobs.read(actor(jwt),tenant,id);}
    @PostMapping("/internal/perception/jobs/claim") public Optional<Lease> claim(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt,@RequestBody Profile body){return jobs.claim(actor(jwt),tenant,body.profileId());}
    @PostMapping("/internal/perception/jobs/{id}/input") public ResponseEntity<byte[]> input(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestBody LeaseToken body){
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(jobs.input(actor(jwt),tenant,id,body.lease()));
    }
    @PostMapping("/internal/perception/jobs/{id}/complete") public boolean complete(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestBody Result body){return jobs.complete(actor(jwt),tenant,id,body);}
    @PostMapping("/internal/perception/jobs/{id}/fail") public boolean fail(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestBody Failure body){return jobs.fail(actor(jwt),tenant,id,body.lease(),body.reason());}
}
