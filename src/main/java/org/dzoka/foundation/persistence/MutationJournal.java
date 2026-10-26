package org.dzoka.foundation.persistence;
import java.sql.*;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;

/** Minimal transactional facts; never place report text, contacts or object keys in the outbox. */
public final class MutationJournal {
    private MutationJournal() {}
    public static void append(Connection c, UUID tenant, VerifiedIdentity actor, String type,
            UUID entity, long version, String referenceColumn) throws SQLException {
        if(!referenceColumn.equals("report_id") && !referenceColumn.equals("media_id")) throw new IllegalArgumentException();
        try(var s=c.prepareStatement("INSERT INTO dzoka.audit_events(tenant_id,id,actor_ref,action,entity_id) VALUES (?,?,?,?,?)")){
            s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setString(3,actor.reference());s.setString(4,type);s.setObject(5,entity);s.executeUpdate();
        }
        try(var s=c.prepareStatement("INSERT INTO dzoka.outbox(tenant_id,id,event_type,aggregate_id,aggregate_version,"+referenceColumn+",payload) VALUES (?,?,?,?,?,?,jsonb_build_object('entityId',CAST(? AS text)))")){
            s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setString(3,type);s.setObject(4,entity);s.setLong(5,version);
            s.setObject(6,entity);s.setString(7,entity.toString());s.executeUpdate();
        }
    }
}
