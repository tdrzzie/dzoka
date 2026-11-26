package org.dzoka.passports;

import java.net.URI;
import java.util.UUID;
import org.dzoka.fulfillment.Sql;
import org.springframework.stereotype.Component;

@Component
public final class LocalItemIdentity implements ItemIdentityProvider {
    @Override public Assertion identify(UUID item,String provider,String reference) {
        if(provider==null || reference==null || reference.length()>256 || reference.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
        switch(provider) {
            case "INTERNAL_UUID" -> { if(!item.toString().equals(reference)) throw new IllegalArgumentException(); }
            case "ENTERPRISE_ASSET" -> Sql.opaque(reference);
            case "QR", "NFC" -> UUID.fromString(reference);
            case "GS1_DIGITAL_LINK" -> {
                URI uri=URI.create(reference);
                if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null
                        || !uri.getPath().matches("/01/[0-9]{14}(?:/21/[A-Za-z0-9_-]{1,40})?")) throw new IllegalArgumentException();
            }
            default -> throw new IllegalArgumentException("Identity provider not adopted");
        }
        return new Assertion(provider,"dzoka-item-reference-v1",reference,provider.equals("INTERNAL_UUID"));
    }
}
