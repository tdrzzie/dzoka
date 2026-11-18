package org.dzoka.matching.api;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Matching.Search;
import org.dzoka.matching.ports.MatchingUseCases;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/internal/matching/reports/{report}")
public final class MatchingController {
    private final MatchingUseCases matching;

    public MatchingController(MatchingUseCases matching) { this.matching=matching; }

    @GetMapping("/candidates")
    public Search candidates(@PathVariable UUID tenant,@PathVariable UUID report,
            @RequestParam(required=false) UUID spaceId,@RequestParam(defaultValue="10") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return matching.search(identity(jwt),tenant,report,spaceId,limit);
    }

    public record Attestation(int revision,int keyVersion,String scopeDigest,String digest,UUID attestationId) {}

    @PostMapping("/identifier-attestations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attest(@PathVariable UUID tenant,@PathVariable UUID report,@RequestBody Attestation body,
            @AuthenticationPrincipal Jwt jwt) {
        matching.attest(identity(jwt),tenant,report,body.revision(),body.keyVersion(),body.scopeDigest(),body.digest(),body.attestationId());
    }

    private static VerifiedIdentity identity(Jwt jwt) { return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()); }
}
