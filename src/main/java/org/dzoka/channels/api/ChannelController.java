package org.dzoka.channels.api;

import java.util.Map;
import java.util.UUID;
import org.dzoka.channels.adapters.ChannelOperations;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/channels")
public final class ChannelController {
    private final ChannelOperations operations;
    public ChannelController(ChannelOperations operations) { this.operations=operations; }
    public record Notice(UUID contact) {}
    public record OpenRelay(UUID claim) {}
    public record RelayMessage(ChannelOperations.Phrase phrase) {}
    public record Block(Boolean abuse) {}
    public record Consent(Boolean enabled) {}
    public record Offer(int revision,String summary,Boolean enabled) {}

    @PutMapping("/match-offers/{report}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID report,
                        @RequestBody Offer body) {
        if(body.enabled()==null) throw new IllegalArgumentException("Required publication choice");
        operations.publishMatchOffer(actor(jwt),tenant,report,body.revision(),body.summary(),body.enabled());
    }

    @PutMapping("/contacts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enroll(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,
                       @RequestBody ChannelOperations.Enrollment body) { operations.enroll(actor(jwt),tenant,id,body); }

    @DeleteMapping("/consent") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) { operations.optOut(actor(jwt),tenant); }

    @PutMapping("/contacts/{id}/consent") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void consent(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Consent body) {
        if(body.enabled()==null) throw new IllegalArgumentException("Required consent choice");
        operations.consent(actor(jwt),tenant,id,body.enabled());
    }

    @PutMapping("/notices/{id}")
    public Map<String,String> queue(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id,@RequestBody Notice body) {
        if(body.contact()==null) throw new IllegalArgumentException("Required contact");
        return Map.of("state",operations.notify(actor(jwt),tenant,id,body.contact()));
    }

    @GetMapping("/notices/{id}")
    public Map<String,String> status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID id) {
        return Map.of("state",operations.status(actor(jwt),tenant,id));
    }

    @GetMapping("/notices")
    public java.util.List<ChannelOperations.Notice> notices(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return operations.notices(actor(jwt),tenant);
    }

    @PostMapping("/dispatch")
    public Map<String,String> dispatch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return Map.of("state",operations.dispatch(actor(jwt),tenant));
    }

    @PutMapping("/relays/{thread}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void open(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID thread,@RequestBody OpenRelay body) {
        if(body.claim()==null) throw new IllegalArgumentException("Required claim");
        operations.openRelay(actor(jwt),tenant,thread,body.claim());
    }

    @GetMapping("/relays/{thread}")
    public ChannelOperations.ThreadView thread(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID thread) {
        return operations.thread(actor(jwt),tenant,thread);
    }

    @GetMapping("/relays/{thread}/moderation")
    public ChannelOperations.ThreadView moderate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID thread) {
        return operations.moderate(actor(jwt),tenant,thread);
    }

    @PostMapping("/relays/prune")
    public Map<String,Integer> prune(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant) {
        return Map.of("removed",operations.pruneRelays(actor(jwt),tenant));
    }

    @PutMapping("/relays/{thread}/messages/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void message(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID thread,@PathVariable UUID id,
                        @RequestBody RelayMessage body) { operations.relay(actor(jwt),tenant,thread,id,body.phrase()); }

    @PutMapping("/relays/{thread}/block") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable UUID thread,@RequestBody Block body) {
        if(body.abuse()==null) throw new IllegalArgumentException("Required block reason");
        operations.block(actor(jwt),tenant,thread,body.abuse());
    }

    private static VerifiedIdentity actor(Jwt jwt) { return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()); }
}
