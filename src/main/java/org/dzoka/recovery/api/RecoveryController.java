package org.dzoka.recovery.api;

import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.recovery.domain.Recovery.*;
import org.dzoka.recovery.ports.RecoveryOperations;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/recovery")
public final class RecoveryController {
    private static final String ITEM="/intakes/{intake}", CLAIM=ITEM+"/claims/{claim}";
    private final RecoveryOperations recovery;

    public RecoveryController(RecoveryOperations recovery) { this.recovery=recovery; }

    public record PolicyInput(Boolean dualControl,Boolean allowRepresentatives) {}
    public record Enrollment(UUID foundReport,UUID site) {}
    public record Hold(Boolean blocked) {}
    public record Fact(FactKind kind,String sourceSha,String answer,Boolean privateConfirmed) {
        @Override public String toString() { return "PrivateFact"; }
    }
    public record ClaimInput(Capacity capacity) {}
    public record Withdrawal(FactWithdrawal reason) {}
    public record Answer(String answer) {
        @Override public String toString() { return "PrivateAnswer"; }
    }
    public record Revocation(Reason reason) {}
    public record Pickup(String audienceRef) {}
    public record PickupToken(String token) {
        @Override public String toString() { return "PickupToken"; }
    }
    public record Presentation(UUID grant,String token,String recipientRef,String confirmationSha) {
        @Override public String toString() { return "PickupPresentation"; }
    }

    @PutMapping("/policies/{version}")
    public Policy policy(@PathVariable UUID tenant,@PathVariable int version,@AuthenticationPrincipal Jwt jwt,
                         @RequestBody PolicyInput body) {
        return recovery.policy(identity(jwt),tenant,version,flag(body.dualControl()),flag(body.allowRepresentatives()));
    }

    @PutMapping(ITEM)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enroll(@PathVariable UUID tenant,@PathVariable UUID intake,@AuthenticationPrincipal Jwt jwt,@RequestBody Enrollment body) {
        recovery.enroll(identity(jwt),tenant,intake,body.foundReport(),body.site());
    }

    @PutMapping(ITEM+"/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hold(@PathVariable UUID tenant,@PathVariable UUID intake,@AuthenticationPrincipal Jwt jwt,@RequestBody Hold body) {
        recovery.hold(identity(jwt),tenant,intake,flag(body.blocked()));
    }

    @PutMapping(ITEM+"/facts/{fact}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fact(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID fact,
                     @AuthenticationPrincipal Jwt jwt,@RequestBody Fact body) {
        recovery.fact(identity(jwt),tenant,intake,fact,body.kind(),body.sourceSha(),body.answer(),flag(body.privateConfirmed()));
    }

    @PutMapping(ITEM+"/facts/{fact}/withdrawal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawFact(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID fact,
                             @AuthenticationPrincipal Jwt jwt,@RequestBody Withdrawal body) {
        recovery.withdrawFact(identity(jwt),tenant,intake,fact,body.reason());
    }

    @PutMapping(CLAIM)
    public Claim submit(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,
                        @AuthenticationPrincipal Jwt jwt,@RequestBody ClaimInput body) {
        return recovery.submit(identity(jwt),tenant,intake,claim,body.capacity());
    }

    @GetMapping(CLAIM)
    public Claim read(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,@AuthenticationPrincipal Jwt jwt) {
        return recovery.read(identity(jwt),tenant,intake,claim);
    }

    @PutMapping(CLAIM+"/withdrawal")
    public Claim withdraw(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,@AuthenticationPrincipal Jwt jwt) {
        return recovery.withdraw(identity(jwt),tenant,intake,claim);
    }

    @PutMapping(CLAIM+"/sessions/{session}")
    public ResponseEntity<Challenge> challenge(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,
                                               @PathVariable UUID session,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recovery.challenge(identity(jwt),tenant,intake,claim,session));
    }

    @PutMapping(CLAIM+"/sessions/{session}/answer")
    public Assessment answer(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,@PathVariable UUID session,
                             @AuthenticationPrincipal Jwt jwt,@RequestBody Answer body) {
        return recovery.answer(identity(jwt),tenant,intake,claim,session,body.answer());
    }

    @PutMapping(CLAIM+"/reviews/{review}")
    public Claim review(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,@PathVariable UUID review,
                        @AuthenticationPrincipal Jwt jwt,@RequestBody Review body) {
        return recovery.review(identity(jwt),tenant,intake,claim,review,body);
    }

    @GetMapping(CLAIM+"/review-context")
    public ResponseEntity<ReviewContext> reviewContext(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recovery.reviewContext(identity(jwt),tenant,intake,claim));
    }

    @PutMapping(CLAIM+"/revocation")
    public Claim revoke(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,
                        @AuthenticationPrincipal Jwt jwt,@RequestBody Revocation body) {
        return recovery.revoke(identity(jwt),tenant,intake,claim,body.reason());
    }

    @PutMapping(CLAIM+"/grants/{grant}")
    public ResponseEntity<Grant> issue(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID claim,
                                      @PathVariable UUID grant,@AuthenticationPrincipal Jwt jwt,@RequestBody Pickup body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recovery.issue(identity(jwt),tenant,intake,claim,grant,body.audienceRef()));
    }

    @PostMapping(ITEM+"/grants/{grant}/pickup-context")
    public ResponseEntity<PickupContext> pickupContext(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID grant,
                                                       @AuthenticationPrincipal Jwt jwt,@RequestBody PickupToken body) {
        var result=recovery.pickupContext(identity(jwt),tenant,intake,grant,body.token());
        if (result==null) throw new Failures.Conflict();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    @PutMapping(ITEM+"/returns/{request}")
    public ResponseEntity<Receipt> redeem(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID request,
                                           @AuthenticationPrincipal Jwt jwt,@RequestBody Presentation body) {
        if (body.grant()==null) throw new IllegalArgumentException("Pickup grant required");
        var result=recovery.redeem(identity(jwt),tenant,intake,body.grant(),request,body.token(),body.recipientRef(),body.confirmationSha());
        if (!result.accepted()) throw new Failures.Conflict();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result.receipt());
    }

    @GetMapping(ITEM+"/pickup-messages")
    public ResponseEntity<java.util.List<PickupMessage>> pickupMessages(@PathVariable UUID tenant,@PathVariable UUID intake,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recovery.pickupMessages(identity(jwt),tenant,intake));
    }

    private static boolean flag(Boolean value) {
        if (value==null) throw new IllegalArgumentException("Required recovery choice");
        return value;
    }
    private static VerifiedIdentity identity(Jwt jwt) { return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()); }
}
