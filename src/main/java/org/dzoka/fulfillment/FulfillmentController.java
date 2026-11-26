package org.dzoka.fulfillment;

import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.logistics.Logistics;
import org.dzoka.rewards.Rewards;
import org.dzoka.passports.Passports;
import org.dzoka.passports.ItemIdentityProvider;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}")
public final class FulfillmentController {
    private final Logistics logistics;
    private final Rewards rewards;
    private final Passports passports;
    private final FulfillmentOperations operations;
    private final tools.jackson.databind.ObjectMapper json;
    public FulfillmentController(Logistics logistics,Rewards rewards,Passports passports,FulfillmentOperations operations,tools.jackson.databind.ObjectMapper json) {
        this.logistics=logistics;this.rewards=rewards;this.passports=passports;this.operations=operations;
        this.json=json;
    }
    public record QuoteRequest(String deliveryReference) {}
    public record Evidence(String evidenceSha) {}
    public record Handoff(String courierRef,String evidenceSha) {}
    public record Registration(String description) {}
    public record Scan(String token) { @Override public String toString(){return "PrivateTagScan";} }
    public record Phrase(String phrase) {}
    @PostMapping("/logistics/quotes") public ResponseEntity<Logistics.Quote> quote(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@RequestBody QuoteRequest body) {
        return ok(logistics.quote(actor(jwt),tenant,body.deliveryReference()));
    }
    @PutMapping("/logistics/shipments/{id}") public ResponseEntity<Logistics.Shipment> shipment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Logistics.Request body) {
        return ok(logistics.request(actor(jwt),tenant,id,body));
    }
    @GetMapping("/logistics/shipments/{id}") public ResponseEntity<Logistics.Shipment> shipment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(logistics.read(actor(jwt),tenant,id));}
    @GetMapping("/logistics/shipments/{id}/tracking") public ResponseEntity<List<Logistics.Tracking>> tracking(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(logistics.tracking(actor(jwt),tenant,id));}
    @PostMapping("/logistics/shipments/{id}/consent-withdrawal") public ResponseEntity<Void> consent(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {logistics.withdrawConsent(actor(jwt),tenant,id,body.evidenceSha());return ok(null);}
    @PostMapping("/logistics/shipments/{id}/booking") public ResponseEntity<Logistics.Shipment> book(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(logistics.book(actor(jwt),tenant,id));}
    @PostMapping("/logistics/shipments/{id}/cancellation") public ResponseEntity<Logistics.Shipment> cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(logistics.cancel(actor(jwt),tenant,id));}
    @PostMapping("/logistics/shipments/{id}/handoff") public ResponseEntity<Logistics.Shipment> handoff(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Handoff body) {return ok(logistics.handoff(actor(jwt),tenant,id,body.courierRef(),body.evidenceSha()));}
    @PostMapping("/logistics/shipments/{id}/receipt") public ResponseEntity<Logistics.Shipment> receipt(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(logistics.confirm(actor(jwt),tenant,id,body.evidenceSha()));}
    @PostMapping("/logistics/shipments/{id}/staff-receipt") public ResponseEntity<Logistics.Shipment> staffReceipt(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(logistics.staffReceipt(actor(jwt),tenant,id,body.evidenceSha()));}
    @PutMapping("/logistics/shipments/{id}/receipt-reviews/{review}") public ResponseEntity<Void> receiptReview(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@PathVariable UUID review,@RequestBody Evidence body) {logistics.reviewReceipt(actor(jwt),tenant,id,review,body.evidenceSha());return ok(null);}
    @PutMapping("/rewards/{id}") public ResponseEntity<Rewards.Reward> reward(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody tools.jackson.databind.JsonNode body) {
        if(!body.path("minorUnits").isIntegralNumber() || !body.path("minorUnits").canConvertToLong() || !body.path("feeMinor").isIntegralNumber() || !body.path("feeMinor").canConvertToLong() || !body.path("policyVersion").isIntegralNumber() || !body.path("policyVersion").canConvertToInt()) throw new IllegalArgumentException("Integer minor units and policy required");
        Rewards.Terms terms;
        try{terms=json.treeToValue(body,Rewards.Terms.class);}catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid reward terms");}
        return ok(rewards.offer(actor(jwt),tenant,id,terms));
    }
    @GetMapping("/rewards/{id}") public ResponseEntity<Rewards.Reward> reward(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(rewards.read(actor(jwt),tenant,id));}
    @GetMapping("/rewards/{id}/ledger") public ResponseEntity<List<Rewards.Entry>> ledger(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(rewards.ledger(actor(jwt),tenant,id));}
    @PostMapping("/rewards/{id}/receipt") public ResponseEntity<Rewards.Reward> rewardReceipt(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(rewards.confirm(actor(jwt),tenant,id,body.evidenceSha()));}
    @PostMapping("/rewards/{id}/release") public ResponseEntity<Rewards.Reward> release(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(rewards.settle(actor(jwt),tenant,id,false));}
    @PostMapping("/rewards/{id}/refund") public ResponseEntity<Rewards.Reward> refund(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(rewards.settle(actor(jwt),tenant,id,true));}
    @PostMapping("/rewards/{id}/dispute") public ResponseEntity<Rewards.Reward> dispute(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(rewards.dispute(actor(jwt),tenant,id,body.evidenceSha(),false));}
    @PostMapping("/rewards/{id}/resolution") public ResponseEntity<Rewards.Reward> resolve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(rewards.dispute(actor(jwt),tenant,id,body.evidenceSha(),true));}
    @PostMapping("/rewards/{id}/reversal") public ResponseEntity<Rewards.Reward> reverse(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(rewards.reverse(actor(jwt),tenant,id,body.evidenceSha()));}
    @PostMapping("/rewards/{id}/intent-cancellation") public ResponseEntity<Rewards.Reward> cancelIntent(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Evidence body) {return ok(rewards.cancelIntent(actor(jwt),tenant,id,body.evidenceSha()));}
    @PutMapping("/passports/items/{id}") public ResponseEntity<Passports.Item> register(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Registration body) {return ok(passports.register(actor(jwt),tenant,id,body.description()));}
    @GetMapping("/passports/items") public ResponseEntity<List<Passports.Item>> items(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {return ok(passports.items(actor(jwt),tenant));}
    @DeleteMapping("/passports/items/{id}") public ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {passports.deactivate(actor(jwt),tenant,id);return ok(null);}
    @PutMapping("/passports/items/{item}/tags/{id}") public ResponseEntity<Map<String,String>> tag(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID item,@PathVariable UUID id,@RequestBody Passports.Tag body) {passports.tag(actor(jwt),tenant,item,id,body);return ok(Map.of("locator",Passports.locator(tenant,body.token()),"kind",body.kind()));}
    @DeleteMapping("/passports/items/{item}/tags/{id}") public ResponseEntity<Void> revokeTag(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID item,@PathVariable UUID id) {passports.revokeTag(actor(jwt),tenant,item,id);return ok(null);}
    @PutMapping("/passports/items/{item}/identities/{id}") public ResponseEntity<ItemIdentityProvider.Assertion> identity(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID item,@PathVariable UUID id,@RequestBody Passports.Identity body) {return ok(passports.passport(actor(jwt),tenant,item,id,body));}
    @GetMapping("/passports/items/{item}/identities") public ResponseEntity<List<Passports.StoredIdentity>> identities(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID item) {return ok(passports.identities(actor(jwt),tenant,item));}
    @GetMapping("/passports/items/{item}/tags") public ResponseEntity<List<Passports.TagView>> tags(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID item) {return ok(passports.tags(actor(jwt),tenant,item));}
    @PostMapping("/passports/threads/{id}/scan") public ResponseEntity<Passports.ThreadView> scan(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Scan body) {return ok(passports.scan(actor(jwt),tenant,id,body.token()));}
    @GetMapping("/passports/threads/{id}") public ResponseEntity<Passports.ThreadView> thread(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {return ok(passports.thread(actor(jwt),tenant,id));}
    @GetMapping("/passports/threads") public ResponseEntity<List<Passports.ThreadView>> threads(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {return ok(passports.threads(actor(jwt),tenant));}
    @PutMapping("/passports/threads/{thread}/messages/{id}") public ResponseEntity<Passports.ThreadView> message(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID thread,@PathVariable UUID id,@RequestBody Phrase body) {return ok(passports.message(actor(jwt),tenant,thread,id,body.phrase()));}
    @PostMapping("/passports/threads/{id}/block") public ResponseEntity<Void> block(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {passports.moderate(actor(jwt),tenant,id,false);return ok(null);}
    @PostMapping("/passports/threads/{id}/flag") public ResponseEntity<Void> flag(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {passports.moderate(actor(jwt),tenant,id,true);return ok(null);}
    @PostMapping("/passports/purge") public ResponseEntity<Map<String,Integer>> purge(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {return ok(Map.of("purged",passports.purge(actor(jwt),tenant)));}
    @PostMapping("/fulfillment/dispatch") public ResponseEntity<Map<String,Boolean>> dispatch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {return ok(Map.of("worked",operations.dispatch(actor(jwt),tenant)));}
    private static VerifiedIdentity actor(Jwt jwt) {return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    private static <T> ResponseEntity<T> ok(T value) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
