package org.dzoka.intake.api;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.IntakeReference;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/tenants/{tenant}")
public final class IntakeReferenceController {
    private final IntakeService service;public IntakeReferenceController(IntakeService service){this.service=service;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    @GetMapping("/cases/{id}") public IntakeReference caseReference(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.reference(actor(jwt),tenant,id,true);}
    @GetMapping("/items/{id}") public IntakeReference itemReference(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.reference(actor(jwt),tenant,id,false);}
}
