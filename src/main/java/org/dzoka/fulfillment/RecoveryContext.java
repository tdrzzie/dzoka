package org.dzoka.fulfillment;

import java.sql.*;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;

public record RecoveryContext(UUID report,String owner,String custodian,int generation,boolean blocked,
                              boolean returned,boolean available,boolean authorized,int policy,boolean confirmedReturn,boolean entitled,long reportVersion) {
    public static RecoveryContext read(Connection db,UUID intake,UUID claim) throws SQLException {
        try(var query=Sql.q(db,"SELECT * FROM dzoka.fulfillment_context(?,?)",intake,claim);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
            return new RecoveryContext(row.getObject(1,UUID.class),row.getString(2),row.getString(3),row.getInt(4),row.getBoolean(5),
                    row.getBoolean(6),row.getBoolean(7),row.getBoolean(8),row.getInt(9),row.getBoolean(10),row.getBoolean(11),row.getLong(12));
        }
    }
    public void requireRelease() {
        if(blocked || returned || !available || !authorized) throw new Failures.Conflict();
    }
    public void requireReward() {
        if(blocked || !available || !confirmedReturn) throw new Failures.Conflict();
    }
}
