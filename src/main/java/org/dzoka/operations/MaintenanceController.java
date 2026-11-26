package org.dzoka.operations;

import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.ports.Reports;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.matching.ports.MatchingUseCases;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Bounded canonical purges. Object deletion remains in the separately isolated media worker. */
@RestController
@RequestMapping("/api/v1/tenants/{tenant}/operations/maintenance")
public final class MaintenanceController {
    private final Reports reports;private final FingerprintJobs fingerprints;private final PerceptionJobs perceptions;private final MatchingUseCases matching;
    public MaintenanceController(Reports reports,FingerprintJobs fingerprints,PerceptionJobs perceptions,MatchingUseCases matching){this.reports=reports;this.fingerprints=fingerprints;this.perceptions=perceptions;this.matching=matching;}
    @PostMapping("/{kind}") public ResponseEntity<Map<String,Integer>> purge(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID tenant,@PathVariable String kind){
        var actor=new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());
        int count=switch(kind){case "reports"->reports.expire(actor,tenant);case "fingerprints"->fingerprints.purge(actor,tenant);case "perceptions"->perceptions.purge(actor,tenant);case "history"->matching.pruneHistory(actor,tenant);default->throw new IllegalArgumentException();};
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("purged",count));
    }
}
