package org.dzoka.fulfillment;

import java.sql.*;
import java.util.UUID;

public final class Sql {
    public static PreparedStatement q(Connection db,String sql,Object... values) throws SQLException {
        var query=db.prepareStatement(sql);
        for(int i=0;i<values.length;i++) query.setObject(i+1,values[i]);
        return query;
    }
    public static int write(Connection db,String sql,Object... values) throws SQLException {
        try(var query=q(db,sql,values)) { return query.executeUpdate(); }
    }
    public static void lock(Connection db,UUID tenant) throws SQLException {
        try(var query=q(db,"SELECT pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||?::text,17))",tenant)) { query.execute(); }
    }
    public static void sha(String value) {
        if(value==null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Evidence digest required");
    }
    public static String opaque(String value) {
        if(value==null || !value.matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Opaque reference required");
        return value;
    }
    private Sql() {}
}
