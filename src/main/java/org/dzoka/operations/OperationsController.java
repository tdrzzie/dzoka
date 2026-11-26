package org.dzoka.operations;

import java.time.Instant;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/operations")
public final class OperationsController {
    private final Operations operations;private final JobDiagnostics diagnostics;
    public OperationsController(Operations operations,JobDiagnostics diagnostics){this.operations=operations;this.diagnostics=diagnostics;}
    public record Evidence(String evidenceSha) {}
    @PostMapping("/telemetry/export") public ResponseEntity<Map<String,Integer>> export(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant){return ok(Map.of("exported",diagnostics.export(actor(jwt),tenant)));}
    @PutMapping("/policy") public ResponseEntity<Void> policy(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody Operations.Policy body){operations.policy(actor(jwt),tenant,body);return ok(null);}
    @GetMapping("/erasures") public ResponseEntity<List<Operations.Erasure>> erasures(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestParam(required=false) UUID after){return ok(operations.erasures(actor(jwt),tenant,after));}
    @GetMapping("/status") public ResponseEntity<JsonNode> status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant){return ok(operations.status(actor(jwt),tenant));}
    @GetMapping("/analytics") public ResponseEntity<JsonNode> analytics(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestParam Instant since){return ok(operations.analytics(actor(jwt),tenant,since));}
    @PostMapping("/restore/close") public ResponseEntity<Void> close(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody Evidence body){operations.close(actor(jwt),tenant,body.evidenceSha());return ok(null);}
    @PostMapping("/restore/replay") public ResponseEntity<Map<String,String>> replay(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody Operations.Replay body){return ok(Map.of("manifestSha",operations.replay(actor(jwt),tenant,body)));}
    @PostMapping("/restore/open") public ResponseEntity<Void> open(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody Operations.Open body){operations.open(actor(jwt),tenant,body);return ok(null);}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    private static <T> ResponseEntity<T> ok(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
