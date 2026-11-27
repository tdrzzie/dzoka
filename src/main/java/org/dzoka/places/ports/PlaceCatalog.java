package org.dzoka.places.ports;
import java.sql.*;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.places.domain.*;
public interface PlaceCatalog {
    record Page(List<PlaceView> items,UUID nextCursor) {}
    PlaceView save(VerifiedIdentity actor,UUID tenant,UUID id,PlaceDraft draft,String key);
    PlaceView get(VerifiedIdentity actor,UUID tenant,UUID id,Long version);
    Page page(VerifiedIdentity actor,UUID tenant,UUID after,int limit,String label,boolean includeInactive);
    List<PlaceView> ancestors(VerifiedIdentity actor,UUID tenant,UUID id,Long version);
    void requireReference(Connection connection,UUID tenant,UUID id,Long version) throws SQLException;
}
