package org.dzoka.places.domain;
import java.util.*;
public record PlaceView(UUID id,long version,String name,String kind,UUID parentId,Long parentVersion,boolean active,
        Double latitude,Double longitude,Double uncertaintyMeters,String timezone,List<String> aliases) {}
