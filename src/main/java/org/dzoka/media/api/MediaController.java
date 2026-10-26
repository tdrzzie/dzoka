package org.dzoka.media.api;
import java.io.IOException;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.dzoka.foundation.api.BoundedJsonInput;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.media.application.MediaService;
import org.dzoka.media.domain.MediaView;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenant}")
public final class MediaController {
    private final MediaService service;public MediaController(MediaService service){this.service=service;}
    private static VerifiedIdentity actor(Jwt jwt){return new VerifiedIdentity(jwt.getIssuer().toString(),jwt.getSubject());}
    @PostMapping(value="/reports/{report}/media",consumes={"image/png","image/jpeg"})
    public MediaView upload(@PathVariable UUID tenant,@PathVariable UUID report,@AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String key,HttpServletRequest request) throws IOException {
        var actor=actor(jwt);service.authorizeUpload(actor,tenant,report,key);
        int max=5*1024*1024;if(request.getContentLengthLong()>max)throw new BoundedJsonInput.TooLarge();
        byte[] bytes=request.getInputStream().readNBytes(max+1);if(bytes.length>max)throw new BoundedJsonInput.TooLarge();
        String type=MediaType.parseMediaType(request.getContentType()).getType()+"/"+MediaType.parseMediaType(request.getContentType()).getSubtype();
        return service.upload(actor,tenant,report,key,bytes,type);
    }
    @GetMapping("/media/{media}") public MediaView state(@PathVariable UUID tenant,@PathVariable UUID media,@AuthenticationPrincipal Jwt jwt){return service.read(actor(jwt),tenant,media);}
    @PostMapping("/media/{media}/retry") public MediaView retry(@PathVariable UUID tenant,@PathVariable UUID media,@AuthenticationPrincipal Jwt jwt){return service.retry(actor(jwt),tenant,media);}
    @GetMapping("/media/{media}/preview") public ResponseEntity<byte[]> preview(@PathVariable UUID tenant,@PathVariable UUID media,@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'")
            .body(service.preview(actor(jwt),tenant,media));
    }
}
