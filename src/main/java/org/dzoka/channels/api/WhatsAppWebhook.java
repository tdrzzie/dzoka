package org.dzoka.channels.api;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.dzoka.channels.adapters.ChannelOperations;
import org.dzoka.channels.adapters.WhatsApp;
import org.dzoka.foundation.api.BoundedJsonInput;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hooks/whatsapp")
public final class WhatsAppWebhook {
    private final WhatsApp whatsapp;
    private final ChannelOperations operations;
    public WhatsAppWebhook(WhatsApp whatsapp,ChannelOperations operations) { this.whatsapp=whatsapp;this.operations=operations; }

    @GetMapping(produces="text/plain")
    public ResponseEntity<String> verify(@RequestParam(name="hub.mode",required=false) String mode,
                                         @RequestParam(name="hub.verify_token",required=false) String token,
                                         @RequestParam(name="hub.challenge",required=false) String challenge) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(whatsapp.challenge(mode,token,challenge));
    }

    @PostMapping(consumes="application/json") @ResponseStatus(HttpStatus.OK)
    public void observe(@RequestHeader(value="X-Hub-Signature-256",required=false) String signature,HttpServletRequest request)
            throws IOException {
        whatsapp.tenant();
        int limit=131072;
        if(request.getContentLengthLong()>limit) throw new BoundedJsonInput.TooLarge();
        byte[] bytes=request.getInputStream().readNBytes(limit+1);
        if(bytes.length>limit) throw new BoundedJsonInput.TooLarge();
        operations.observe(whatsapp.parse(signature,bytes));
    }
}
