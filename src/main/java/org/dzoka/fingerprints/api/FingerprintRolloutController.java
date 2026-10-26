package org.dzoka.fingerprints.api;
import java.util.UUID;
import org.dzoka.fingerprints.ports.FingerprintRollouts;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/internal/fingerprints/rollouts")
public final class FingerprintRolloutController {
    private final FingerprintRollouts rollouts;
    public FingerprintRolloutController(FingerprintRollouts rollouts){this.rollouts=rollouts;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    public record Transition(Long expectedVersion,Boolean rollback){
        public Transition{if(expectedVersion==null||expectedVersion<0||rollback==null)throw new IllegalArgumentException("Explicit transition fields required");}
    }
    @PostMapping public Status create(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt,@RequestBody Plan body){return rollouts.create(actor(jwt),tenant,body);}
    @GetMapping("/active") public Activation active(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt){return rollouts.active(actor(jwt),tenant);}
    @GetMapping("/{id}") public Status read(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return rollouts.read(actor(jwt),tenant,id);}
    @PostMapping("/{id}/seal") public Status seal(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return rollouts.seal(actor(jwt),tenant,id);}
    @PostMapping("/{id}/activate") public Activation activate(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestBody Transition body){
        return rollouts.activate(actor(jwt),tenant,id,body.expectedVersion(),body.rollback());
    }
}
