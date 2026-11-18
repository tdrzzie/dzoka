package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.dzoka.matching.domain.Matching.Observation;
import org.dzoka.matching.domain.Matching.Vector;
import org.dzoka.matching.domain.Matching.VectorRoute;

/** Exact extension ranking over an already authorized, single-space statement snapshot. */
final class PgVectorRanking {
    private static final String BACKEND = "PGVECTOR_EXACT";
    private static final String VERSION = "0.8.7";
    private PgVectorRanking() {}

    static VectorRoute rank(Connection connection, Vector source, List<Observation> pool) throws SQLException {
        var checkpoint = connection.setSavepoint();
        try {
            verifyExtension(connection);
            var candidates = pool.stream().filter(row -> row.vector()!=null
                    && source.spaceKey().equals(row.vector().spaceKey())
                    && row.vector().values().length==source.values().length).toList();
            var ids = connection.createArrayOf("uuid",candidates.stream().map(Observation::report).toArray());
            var values = connection.createArrayOf("text",candidates.stream().map(row -> serialize(row.vector())).toArray());
            Map<UUID,Double> scores = new HashMap<>();
            try (var query=connection.prepareStatement("""
                    SELECT id,1-(CAST(? AS public.vector) OPERATOR(public.<=>) CAST(coordinates AS public.vector))
                    FROM unnest(CAST(? AS uuid[]),CAST(? AS text[])) AS candidate(id,coordinates)
                    """)) {
                query.setQueryTimeout(2);
                query.setString(1,serialize(source));query.setArray(2,ids);query.setArray(3,values);
                try (var rows=query.executeQuery()) {
                    while (rows.next()) {
                        double similarity=rows.getDouble(2);
                        if (rows.wasNull() || !Double.isFinite(similarity)) throw new SQLException("Invalid vector result");
                        scores.put(rows.getObject(1,UUID.class),similarity);
                    }
                }
            } finally { ids.free(); values.free(); }
            if (scores.size()!=candidates.size()) throw new SQLException("Incomplete vector results");
            connection.releaseSavepoint(checkpoint);
            return new VectorRoute(BACKEND,scores,false);
        } catch (SQLException failure) {
            connection.rollback(checkpoint);
            connection.releaseSavepoint(checkpoint);
            return new VectorRoute(BACKEND,Map.of(),true);
        }
    }

    private static void verifyExtension(Connection connection) throws SQLException {
        try (var query=connection.prepareStatement("""
                SELECT extversion FROM pg_catalog.pg_extension e JOIN pg_catalog.pg_namespace n ON e.extnamespace=n.oid
                WHERE extname='vector' AND n.nspname='public'
                """)) {
            query.setQueryTimeout(2);
            try (var rows=query.executeQuery()) {
                if (!rows.next() || !VERSION.equals(rows.getString(1))) throw new SQLException("Vector extension unavailable");
            }
        }
    }

    private static String serialize(Vector vector) { return Arrays.toString(vector.values()); }
}
