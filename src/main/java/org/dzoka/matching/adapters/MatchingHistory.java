package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.matching.domain.Matching.Search;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

final class MatchingHistory {
    private static final ObjectMapper JSON=new ObjectMapper();
    record Stored(UUID space, int limit,String category, Search result) {}
    private MatchingHistory() {}

    static void lockAndClean(Connection connection, UUID tenant, String actor) throws SQLException {
        try(var lock=connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,12))")) {
            lock.setString(1,"match-run:"+tenant+":"+actor);
            lock.execute();
        }
        try(var cleanup=connection.prepareStatement("""
                DELETE FROM dzoka.match_search_runs WHERE tenant_id=? AND actor_ref=?
                  AND created_at<=statement_timestamp()-interval '24 hours'
                """)) {
            cleanup.setObject(1,tenant);cleanup.setString(2,actor);cleanup.executeUpdate();
        }
    }

    static int prune(Connection connection, UUID tenant) throws SQLException {
        try(var query=connection.prepareStatement("""
                DELETE FROM dzoka.match_search_runs WHERE tenant_id=? AND id IN (
                  SELECT id FROM dzoka.match_search_runs WHERE tenant_id=?
                    AND created_at<=statement_timestamp()-interval '24 hours'
                  ORDER BY created_at,id LIMIT 200)
                """)) {
            query.setObject(1,tenant);query.setObject(2,tenant);return query.executeUpdate();
        }
    }

    static Stored read(Connection connection, UUID tenant, UUID report, UUID run) throws SQLException {
        try(var query=connection.prepareStatement("""
                SELECT r.space_id,r.requested_limit,r.candidate_count,r.summary::text,
                       COALESCE(jsonb_agg(c.evidence ORDER BY c.ordinal)
                           FILTER (WHERE c.ordinal IS NOT NULL),'[]'::jsonb)::text,r.requested_category
                FROM dzoka.match_search_runs r LEFT JOIN dzoka.match_search_candidates c
                  ON (c.tenant_id,c.run_id)=(r.tenant_id,r.id)
                WHERE r.tenant_id=? AND r.report_id=? AND r.client_id=? AND r.actor_ref=dzoka.current_actor_ref()
                  AND r.created_at>statement_timestamp()-interval '24 hours'
                GROUP BY r.tenant_id,r.id
                """)) {
            query.setObject(1,tenant);query.setObject(2,report);query.setObject(3,run);
            try(var row=query.executeQuery()) {
                if(!row.next()) return null;
                var candidates=JSON.readTree(row.getString(5));
                if(candidates.size()!=row.getInt(3)) throw new Failures.NotFound();
                var summary=(ObjectNode)JSON.readTree(row.getString(4));
                summary.set("candidates",candidates);
                return new Stored(row.getObject(1,UUID.class),row.getInt(2),row.getString(6),JSON.treeToValue(summary,Search.class));
            }
        }
    }

    static void write(Connection connection, UUID tenant, String actor, UUID space, int limit,String category,
                      UUID clientId, Search result) throws SQLException {
        UUID run=UUID.randomUUID();
        var summary=(ObjectNode)JSON.valueToTree(result);
        summary.remove("candidates");
        try(var insert=connection.prepareStatement("""
                INSERT INTO dzoka.match_search_runs
                  (tenant_id,id,actor_ref,report_id,revision,space_id,requested_limit,candidate_count,summary,requested_category,client_id)
                SELECT ?,?,?,?,?,?,?,?,CAST(? AS jsonb),?,?
                WHERE (SELECT count(*) FROM dzoka.match_search_quota WHERE tenant_id=? AND actor_ref=?)<100
                ON CONFLICT DO NOTHING
                """)) {
            insert.setObject(1,tenant);insert.setObject(2,run);insert.setString(3,actor);
            insert.setObject(4,result.report());insert.setInt(5,result.revision());insert.setObject(6,space,java.sql.Types.OTHER);
            insert.setInt(7,limit);insert.setInt(8,result.candidates().size());insert.setString(9,summary.toString());
            insert.setString(10,category);insert.setObject(11,clientId);insert.setObject(12,tenant);insert.setString(13,actor);
            if(insert.executeUpdate()!=1) throw new Failures.Conflict();
        }
        try(var quota=connection.prepareStatement(
                "INSERT INTO dzoka.match_search_quota(tenant_id,id,actor_ref) VALUES(?,?,?)")) {
            quota.setObject(1,tenant);quota.setObject(2,run);quota.setString(3,actor);
            quota.executeUpdate();
        }
        try(var insert=connection.prepareStatement("""
                INSERT INTO dzoka.match_search_candidates(tenant_id,run_id,ordinal,report_id,revision,evidence)
                VALUES(?,?,?,?,?,CAST(? AS jsonb))
                """)) {
            for(int i=0;i<result.candidates().size();i++) {
                var candidate=result.candidates().get(i);
                insert.setObject(1,tenant);insert.setObject(2,run);insert.setInt(3,i);
                insert.setObject(4,candidate.report());insert.setInt(5,candidate.revision());
                insert.setString(6,JSON.writeValueAsString(candidate));insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
