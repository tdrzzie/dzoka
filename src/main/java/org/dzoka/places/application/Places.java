package org.dzoka.places.application;
import java.sql.*;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.places.domain.*;
import org.dzoka.places.ports.PlaceCatalog;
import org.springframework.stereotype.Service;
@Service
public final class Places {
    private final PlaceCatalog catalog;
    public Places(PlaceCatalog catalog){this.catalog=catalog;}
    public PlaceView save(VerifiedIdentity actor,UUID tenant,UUID id,PlaceDraft draft,String key){
        if(draft==null || key==null || !key.matches("[A-Za-z0-9._:-]{8,128}") || (id==null)!=(draft.expectedVersion()==0))throw new IllegalArgumentException("Invalid place command");
        return catalog.save(actor,tenant,id,draft,key);
    }
    public PlaceView get(VerifiedIdentity actor,UUID tenant,UUID id,Long version){if(version!=null&&version<1)throw new IllegalArgumentException();return catalog.get(actor,tenant,id,version);}
    public PlaceCatalog.Page page(VerifiedIdentity actor,UUID tenant,UUID after,int limit,String label,boolean includeInactive){if(limit<1||limit>100)throw new IllegalArgumentException();return catalog.page(actor,tenant,after,limit,label==null?null:PlaceDraft.normalize(label),includeInactive);}
    public List<PlaceView> ancestors(VerifiedIdentity actor,UUID tenant,UUID id,Long version){if(version!=null&&version<1)throw new IllegalArgumentException();return catalog.ancestors(actor,tenant,id,version);}
    /** Called inside the report transaction; location module retains validation ownership. */
    public void requireReference(Connection connection,UUID tenant,UUID id,Long version)throws SQLException{catalog.requireReference(connection,tenant,id,version);}
}
