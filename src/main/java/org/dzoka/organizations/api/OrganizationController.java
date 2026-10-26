package org.dzoka.organizations.api;

import java.util.List;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.application.OrganizationService;
import org.dzoka.organizations.domain.Organization;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/organizations")
public final class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }
    public record CreateOrganization(String name) {}
    private static VerifiedIdentity actor(Jwt jwt) {
        return new VerifiedIdentity(jwt.getIssuer().toString(), jwt.getSubject());
    }
    @PostMapping
    public Organization create(@PathVariable UUID tenant, @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String key, @RequestBody CreateOrganization body) {
        return service.create(actor(jwt), tenant, body.name(), key);
    }
    @GetMapping
    public List<Organization> list(@PathVariable UUID tenant, @AuthenticationPrincipal Jwt jwt) {
        return service.list(actor(jwt), tenant);
    }
}
