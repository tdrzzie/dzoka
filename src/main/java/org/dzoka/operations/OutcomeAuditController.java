package org.dzoka.operations;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/operations/outcomes/{claim}")
public final class OutcomeAuditController {
    private final OutcomeAudits audits;
    public OutcomeAuditController(OutcomeAudits audits){this.audits=audits;}
    @GetMapping public ResponseEntity<JsonNode> context(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID claim){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audits.context(actor(jwt),tenant,claim));}
    @PutMapping("/audits/{id}") public ResponseEntity<Void> record(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID claim,@PathVariable UUID id,@RequestBody OutcomeAudits.Audit body){audits.record(actor(jwt),tenant,claim,id,body);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
}
