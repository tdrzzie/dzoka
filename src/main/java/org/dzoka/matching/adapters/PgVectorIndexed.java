package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.dzoka.matching.domain.Matching;
import org.dzoka.matching.domain.Matching.Observation;
import org.dzoka.matching.domain.Matching.VectorRoute;

final class PgVectorIndexed {
    static final String BACKEND="PGVECTOR_HNSW";
    private PgVectorIndexed() {}

    static VectorRoute rank(Connection db, UUID tenant, Observation source, UUID space,long activation,String category) throws SQLException {
        if(source.vector().values().length!=384) return failure();
        var saved=db.setSavepoint();
        try {
            PgVectorRanking.verifyExtension(db);
            try(var policy=db.prepareStatement("""
                    SELECT set_config('hnsw.iterative_scan','strict_order',true),
                           set_config('hnsw.max_scan_tuples','10000',true), EXISTS(
                        SELECT 1 FROM pg_catalog.pg_index ix JOIN pg_catalog.pg_class c ON c.oid=ix.indexrelid
                        JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
                        JOIN pg_catalog.pg_am am ON am.oid=c.relam
                        WHERE n.nspname='dzoka' AND c.relname='matching_text_hnsw_384' AND am.amname='hnsw'
                          AND ix.indisvalid AND ix.indisready AND ix.indrelid='dzoka.fingerprint_components'::regclass)
                    """ );var row=policy.executeQuery()) {
                if(!row.next() || !row.getBoolean(3)) throw new SQLException("Reviewed index unavailable");
            }
            var found=new LinkedHashMap<UUID,Double>();
            try(var sql=db.prepareStatement("""
                  SELECT c.report_id,1-(c.vector::public.vector(384) OPERATOR(public.<=>) q.v)
                  FROM dzoka.fingerprint_components c CROSS JOIN (SELECT CAST(? AS public.vector(384)) AS v,?::text AS category) q
                  WHERE c.tenant_id=? AND c.space_id=? AND cardinality(c.vector)=384 AND NOT c.truncated
                    AND EXISTS (
                      SELECT 1 FROM dzoka.fingerprint_active a
                      JOIN dzoka.fingerprint_snapshot_slots sl ON (sl.tenant_id,sl.rollout_id)=(a.tenant_id,a.rollout_id)
                      JOIN dzoka.reports r ON (r.tenant_id,r.id,r.current_revision)=(c.tenant_id,c.report_id,c.source_revision)
                      JOIN dzoka.reports s ON s.tenant_id=r.tenant_id AND s.id=? AND s.current_revision=? AND s.kind<>r.kind
                      JOIN dzoka.embedding_spaces e ON (e.tenant_id,e.id)=(c.tenant_id,c.space_id)
                      WHERE a.tenant_id=c.tenant_id AND a.version=? AND sl.component_id=c.id
                        AND sl.space_id=c.space_id AND sl.report_id=c.report_id AND sl.state='AVAILABLE'
                        AND e.enabled AND e.dimensions=384 AND e.metric='COSINE'
                        AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
                        AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                        AND (q.category IS NULL OR EXISTS (
                          SELECT 1 FROM dzoka.report_revisions cv WHERE
                            (cv.tenant_id,cv.report_id,cv.revision)=(r.tenant_id,r.id,r.current_revision)
                            AND cv.category=q.category))
                        AND s.deleted_at IS NULL AND s.expires_at>statement_timestamp()
                        AND (s.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                    )
                  ORDER BY c.vector::public.vector(384) OPERATOR(public.<=>) q.v LIMIT ?
                  """)) {
                sql.setQueryTimeout(3);
                sql.setString(1,Arrays.toString(source.vector().values()));sql.setString(2,category);sql.setObject(3,tenant);
                sql.setObject(4,space);sql.setObject(5,source.report());sql.setInt(6,source.revision());
                sql.setLong(7,activation);sql.setInt(8,Matching.ROUTE_LIMIT+1);
                try(var rows=sql.executeQuery()) {
                    while(rows.next()) {
                        double similarity=rows.getDouble(2);
                        if(rows.wasNull() || !Double.isFinite(similarity)) throw new SQLException("Invalid indexed distance");
                        found.put(rows.getObject(1,UUID.class),similarity);
                    }
                }
            }
            db.releaseSavepoint(saved);
            return new VectorRoute(BACKEND,found,false);
        } catch(SQLException failure) {
            db.rollback(saved);db.releaseSavepoint(saved);return failure();
        }
    }

    private static VectorRoute failure() { return new VectorRoute(BACKEND,Map.of(),true); }
}
