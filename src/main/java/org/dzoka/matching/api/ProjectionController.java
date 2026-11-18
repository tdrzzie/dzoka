package org.dzoka.matching.api;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.ports.MatchingUseCases;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/internal/matching")
public final class ProjectionController {
    private final MatchingUseCases matching;
    public ProjectionController(MatchingUseCases matching) { this.matching=matching; }

    @PutMapping("/projections/{space}")
    public MatchingUseCases.Projection reconcile(@PathVariable UUID tenant,@PathVariable UUID space,
            @AuthenticationPrincipal Jwt jwt) {
        return matching.reconcile(new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()),tenant,space);
    }

    @PostMapping("/history/prune")
    public java.util.Map<String,Integer> prune(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt) {
        int removed=matching.pruneHistory(new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()),tenant);
        return java.util.Map.of("removed",removed);
    }
}
