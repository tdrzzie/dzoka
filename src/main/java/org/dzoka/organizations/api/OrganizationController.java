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
    @GetMapping("/page")
    public org.dzoka.organizations.ports.OrganizationCommands.Page page(@PathVariable UUID tenant, @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) UUID after, @RequestParam(defaultValue="50") int limit,
            @RequestParam(defaultValue="false") boolean includeInactive) {
        return service.page(actor(jwt),tenant,after,limit,includeInactive);
    }
    public record UpdateOrganization(String name, Boolean active, Long expectedVersion) {}
    @GetMapping("/{id}")
    public org.dzoka.organizations.domain.OrganizationState get(@PathVariable UUID tenant,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        return service.get(actor(jwt),tenant,id);
    }
    @PutMapping("/{id}")
    public org.dzoka.organizations.domain.OrganizationState update(@PathVariable UUID tenant,@PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") String key,@RequestBody UpdateOrganization body) {
        if(body.active()==null || body.expectedVersion()==null)throw new IllegalArgumentException("Required update fields");
        return service.update(actor(jwt),tenant,id,body.name(),body.active(),body.expectedVersion(),key);
    }
}
