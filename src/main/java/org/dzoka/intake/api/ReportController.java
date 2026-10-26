package org.dzoka.intake.api;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}/reports")
public final class ReportController {
    private final IntakeService service;private final ObjectMapper mapper;
    public ReportController(IntakeService service,ObjectMapper mapper){this.service=service;this.mapper=mapper;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    private ReportDraft parse(byte[] raw){try{return mapper.readValue(raw,ReportDraft.class);}catch(Exception error){throw new IllegalArgumentException("Invalid report");}}
    @PostMapping(consumes="application/json") public ReportReceipt create(@PathVariable UUID tenant,@AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String key,@RequestBody byte[] raw){return service.submit(actor(jwt),tenant,null,null,parse(raw),raw,key);}
    @PostMapping(value="/{report}/revisions",consumes="application/json") public ReportReceipt revise(@PathVariable UUID tenant,@PathVariable UUID report,
            @AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") String key,@RequestHeader("If-Match") int revision,@RequestBody byte[] raw){
        return service.submit(actor(jwt),tenant,report,revision,parse(raw),raw,key);
    }
    @GetMapping("/{report}") public ReportView get(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt){return service.get(actor(jwt),tenant,report,null);}
    @GetMapping("/{report}/revisions/{revision}") public ReportView revision(@PathVariable UUID tenant,@PathVariable UUID report,@PathVariable int revision,@AuthenticationPrincipal Jwt jwt){return service.get(actor(jwt),tenant,report,revision);}
    @DeleteMapping("/{report}") public void delete(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt){service.delete(actor(jwt),tenant,report);}
    public record Hold(boolean held){}
    @PutMapping("/{report}/hold") public void hold(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt,@RequestBody Hold body){service.hold(actor(jwt),tenant,report,body.held());}
}
