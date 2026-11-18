package org.dzoka.matching.api;

import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Calibration.Approval;
import org.dzoka.matching.domain.Calibration.Draft;
import org.dzoka.matching.ports.MatchingCalibration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/internal/matching/calibrations/{artifact}")
public final class CalibrationController {
    private final MatchingCalibration calibration;
    public CalibrationController(MatchingCalibration calibration) { this.calibration=calibration; }
    public record Revocation(String reviewSha256) {}

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void propose(@PathVariable UUID tenant,@PathVariable UUID artifact,@RequestBody Draft draft,
                        @AuthenticationPrincipal Jwt jwt) {
        calibration.propose(identity(jwt),tenant,artifact,draft);
    }
    @GetMapping
    public Draft draft(@PathVariable UUID tenant,@PathVariable UUID artifact,@AuthenticationPrincipal Jwt jwt) {
        return calibration.draft(identity(jwt),tenant,artifact);
    }
    @PutMapping("/approval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID tenant,@PathVariable UUID artifact,@RequestBody Approval approval,
                        @AuthenticationPrincipal Jwt jwt) {
        calibration.approve(identity(jwt),tenant,artifact,approval);
    }
    @PutMapping("/revocation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID tenant,@PathVariable UUID artifact,@RequestBody Revocation revocation,
                       @AuthenticationPrincipal Jwt jwt) {
        calibration.revoke(identity(jwt),tenant,artifact,revocation.reviewSha256());
    }
    private static VerifiedIdentity identity(Jwt jwt) {
        return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());
    }
}
