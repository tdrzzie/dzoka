package org.dzoka.places.adapters;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.places.domain.PlaceDraft;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Persisted formats: never change an existing version's encoding. */
public final class PlaceCommandHash {
    private PlaceCommandHash() {}
    public static final int CURRENT_VERSION=2;
    private static final JsonMapper LEGACY=JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();

    public static String canonical(UUID target,PlaceDraft d) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            text(out,"dzoka.place-command.v2");text(out,target==null?null:target.toString());
            text(out,d.name());text(out,d.kind());text(out,d.parentId()==null?null:d.parentId().toString());
            number(out,d.latitude());number(out,d.longitude());number(out,d.uncertaintyMeters());text(out,d.timezone());
            out.writeInt(d.aliases().size());for(String alias:d.aliases())text(out,alias);
            out.writeBoolean(d.active());out.writeLong(d.expectedVersion());out.flush();
            return ContentDigest.sha256(bytes.toByteArray());
        } catch(IOException impossible) { throw new IllegalStateException("Place command encoding failed",impossible); }
    }
    private static void text(DataOutputStream out,String value)throws IOException {
        if(value==null){out.writeInt(-1);return;}
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);
    }
    private static void number(DataOutputStream out,Double value)throws IOException {
        out.writeBoolean(value!=null);if(value!=null)out.writeLong(Double.doubleToLongBits(value));
    }
    public static boolean matches(int version,String stored,UUID target,PlaceDraft d) {
        if(version==CURRENT_VERSION)return canonical(target,d).equals(stored);
        if(version!=1)return false;
        return legacy(target,d,false).equals(stored)||legacy(target,d,true).equals(stored);
    }
    static String legacy(UUID target,PlaceDraft d,boolean draftFirst) {
        // Explicit projection freezes V27 field names, order, nulls and values.
        var draft=new LinkedHashMap<String,Object>();
        draft.put("name",d.name());draft.put("kind",d.kind());draft.put("parentId",d.parentId());
        draft.put("latitude",d.latitude());draft.put("longitude",d.longitude());draft.put("uncertaintyMeters",d.uncertaintyMeters());
        draft.put("timezone",d.timezone());draft.put("aliases",d.aliases());draft.put("active",d.active());draft.put("expectedVersion",d.expectedVersion());
        var request=new LinkedHashMap<String,Object>();String id=target==null?"CREATE":target.toString();
        if(draftFirst){request.put("draft",draft);request.put("id",id);}else{request.put("id",id);request.put("draft",draft);}
        return VerifiedIdentity.digest(LEGACY.writeValueAsString(request));
    }
}
