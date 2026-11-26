package org.dzoka.learning;

import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/learning")
public final class LearningController {
    private final Learning learning;private final Mlflow mlflow;
    public LearningController(Learning learning,Mlflow mlflow){this.learning=learning;this.mlflow=mlflow;}
    public record Evidence(String evidenceSha) {}
    @PostMapping("/mlflow/export") public ResponseEntity<Map<String,Boolean>> export(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant){return ok(Map.of("exported",mlflow.export(actor(jwt),tenant)));}
    @PostMapping("/mlflow/sync-activation") public ResponseEntity<Map<String,Boolean>> sync(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant){return ok(Map.of("synced",mlflow.syncActivation(actor(jwt),tenant)));}
    @PutMapping("/policy") public ResponseEntity<Void> policy(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody tools.jackson.databind.JsonNode body){
        if(!body.isObject()||body.size()!=5||!body.path("version").isIntegralNumber()||!body.path("retentionDays").isIntegralNumber()||!body.path("optIn").isBoolean()||!body.path("requireGate").isBoolean()||!body.path("purposeSha").isString())throw new IllegalArgumentException();
        learning.policy(actor(jwt),tenant,new Learning.Policy(body.get("version").asInt(),body.get("optIn").asBoolean(),body.get("requireGate").asBoolean(),body.get("retentionDays").asInt(),body.get("purposeSha").asString()));return ok(null);
    }
    @PutMapping("/feedback/{id}") public ResponseEntity<Learning.Feedback> declare(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Learning.Declaration body){return ok(learning.declare(actor(jwt),tenant,id,body));}
    @GetMapping("/feedback") public ResponseEntity<List<Learning.Feedback>> feedback(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant){return ok(learning.feedback(actor(jwt),tenant));}
    @PutMapping("/adjudications/{id}") public ResponseEntity<Void> adjudicate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Learning.Adjudication body){learning.adjudicate(actor(jwt),tenant,id,body);return ok(null);}
    @PutMapping("/datasets/{id}") public ResponseEntity<Learning.DatasetView> dataset(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Learning.Dataset body){return ok(learning.dataset(actor(jwt),tenant,id,body));}
    @PutMapping("/experiments/{id}") public ResponseEntity<Learning.ExperimentView> experiment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Learning.Experiment body){return ok(learning.experiment(actor(jwt),tenant,id,body));}
    @GetMapping("/experiments/{id}") public ResponseEntity<Learning.ExperimentView> experiment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id){return ok(learning.experiment(actor(jwt),tenant,id));}
    @PutMapping("/experiments/{id}/approval") public ResponseEntity<Void> approve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Learning.Approval body){learning.approve(actor(jwt),tenant,id,body);return ok(null);}
    @PostMapping("/revocations/{kind}/{id}") public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable String kind,@PathVariable UUID id,@RequestBody Evidence body){learning.revoke(actor(jwt),tenant,kind,id,body.evidenceSha());return ok(null);}
    @PostMapping("/purge") public ResponseEntity<Map<String,Integer>> purge(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant){return ok(Map.of("purged",learning.purge(actor(jwt),tenant)));}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    private static <T> ResponseEntity<T> ok(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
