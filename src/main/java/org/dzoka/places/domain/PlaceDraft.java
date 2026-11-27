package org.dzoka.places.domain;

import java.text.Normalizer;
import java.time.ZoneId;
import java.util.*;

public record PlaceDraft(String name,String kind,UUID parentId,Double latitude,Double longitude,Double uncertaintyMeters,
        String timezone,List<String> aliases,Boolean active,Long expectedVersion) {
    public PlaceDraft {
        label(name);
        if(kind==null || !Set.of("COUNTRY","REGION","CITY","FACILITY","FLOOR","ROOM","ENTRANCE","STATION","TERMINAL","PLATFORM","ROUTE","VEHICLE","DEPOT","OTHER").contains(kind)
                || active==null || expectedVersion==null || expectedVersion<0 || expectedVersion==Long.MAX_VALUE || aliases==null || aliases.size()>32)throw new IllegalArgumentException("Invalid place");
        aliases=List.copyOf(aliases);var normalized=new HashSet<String>();
        for(String alias:aliases){label(alias);if(!normalized.add(normalize(alias)))throw new IllegalArgumentException("Duplicate alias");}
        if(timezone!=null)try{ZoneId.of(timezone);}catch(java.time.DateTimeException invalid){throw new IllegalArgumentException("Invalid place timezone");}
        if((latitude==null || longitude==null || uncertaintyMeters==null) && !(latitude==null && longitude==null && uncertaintyMeters==null))throw new IllegalArgumentException("Incomplete location");
        if(latitude!=null && (!Double.isFinite(latitude)||!Double.isFinite(longitude)||!Double.isFinite(uncertaintyMeters)||Math.abs(latitude)>90||Math.abs(longitude)>180||uncertaintyMeters<0||uncertaintyMeters>200000))throw new IllegalArgumentException("Invalid location");
    }
    private static void label(String value){
        // codePoints combines valid pairs; a remaining surrogate is malformed UTF-16.
        if(value==null || value.isBlank() || value.length()>160 || value.codePoints().anyMatch(cp->Character.isISOControl(cp)||(cp>=0xD800 && cp<=0xDFFF)))
            throw new IllegalArgumentException("Invalid place label");
    }
    public static String normalize(String value){label(value);return Normalizer.normalize(value,Normalizer.Form.NFC).replaceAll("(?U)\\s+"," ").strip().toLowerCase(Locale.ROOT);}
}
