package org.dzoka.fulfillment;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.foundation.application.Failures;

public final class OperationJournal {
    public static UUID enqueue(Connection db,UUID tenant,VerifiedIdentity actor,UUID entity,String kind,String payload) throws SQLException {
        UUID id=UUID.nameUUIDFromBytes((tenant+":"+entity+":"+kind).getBytes(StandardCharsets.US_ASCII));
        String sha=ContentDigest.sha256(payload.getBytes(StandardCharsets.UTF_8));
        try(var q=Sql.q(db,"SELECT request_sha,state FROM dzoka.fulfillment_operations WHERE id=?",id);var row=q.executeQuery()) {
            if(row.next()) {
                if(!sha.equals(row.getString(1))) throw new Failures.Conflict();
                if(java.util.Set.of("REJECTED","CANCELLED").contains(row.getString(2))) throw new Failures.Conflict();
                return id;
            }
        }
        Sql.write(db,"INSERT INTO dzoka.fulfillment_operations(tenant_id,id,entity_id,kind,payload,actor_ref,request_sha) VALUES(?,?,?,?,?,?,?)",
                tenant,id,entity,kind,payload,actor.reference(),sha);
        return id;
    }
    public static void event(Connection db,UUID tenant,VerifiedIdentity actor,UUID entity,String kind,String evidence) throws SQLException {
        event(db,tenant,actor,entity,kind,evidence,null);
    }
    public static void event(Connection db,UUID tenant,VerifiedIdentity actor,UUID entity,String kind,String evidence,java.time.Instant observed) throws SQLException {
        Sql.sha(evidence);
        Sql.write(db,"INSERT INTO dzoka.fulfillment_events(tenant_id,id,entity_id,kind,evidence_sha,actor_ref,observed_at) VALUES(?,?,?,?,?,?,?)",
                tenant,UUID.randomUUID(),entity,kind,evidence,actor.reference(),observed==null?null:java.sql.Timestamp.from(observed));
    }
    private OperationJournal() {}
}
