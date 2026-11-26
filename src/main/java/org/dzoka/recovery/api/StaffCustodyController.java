package org.dzoka.recovery.api;

import java.util.Map;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.recovery.adapters.StaffCustody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/recovery/intakes/{intake}/custody")
public final class StaffCustodyController {
    private final StaffCustody custody;
    public StaffCustodyController(StaffCustody custody) { this.custody=custody; }

    @GetMapping
    public StaffCustody.Snapshot snapshot(@PathVariable UUID tenant,@PathVariable UUID intake,@AuthenticationPrincipal Jwt jwt) {
        return custody.read(new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()),tenant,intake);
    }

    @PutMapping("/{event}")
    public Map<String,String> apply(@PathVariable UUID tenant,@PathVariable UUID intake,@PathVariable UUID event,
                                    @AuthenticationPrincipal Jwt jwt,@RequestBody StaffCustody.Command command) {
        return Map.of("outcome",custody.apply(new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject()),
                tenant,intake,event,command));
    }
}
