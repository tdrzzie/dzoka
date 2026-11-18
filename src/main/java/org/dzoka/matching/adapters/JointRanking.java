package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.dzoka.matching.domain.Matching;
import org.dzoka.matching.domain.Matching.*;
import tools.jackson.databind.ObjectMapper;

/** Reads only current joint-space pins from one authorized active rollout. */
final class JointRanking {
    private static final int PIN_LIMIT=64;
    private static final ObjectMapper JSON=new ObjectMapper();
    private JointRanking() {}

    private record Pin(UUID report,int revision,boolean source,String task,Vector vector) {}

    static JointRoute rank(Connection db,UUID tenant,Observation source,Long activation,String category) throws SQLException {
        if (activation==null) return JointRoute.UNAVAILABLE;
        var saved=db.setSavepoint();
        var pins=new ArrayList<Pin>();
        try (var query=db.prepareStatement("""
                WITH source AS (
                  SELECT tenant_id,id,kind FROM dzoka.reports
                  WHERE tenant_id=? AND id=? AND current_revision=? AND deleted_at IS NULL
                    AND expires_at>statement_timestamp()
                    AND (owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                )
                SELECT r.id,r.current_revision,r.id=s.id,p.task,c.id,c.evidence->>'spaceKey',c.evidence->'vector'
                FROM source s JOIN dzoka.fingerprint_active a ON a.tenant_id=s.tenant_id AND a.version=?
                JOIN dzoka.fingerprint_perception_slots sl ON (sl.tenant_id,sl.rollout_id)=(a.tenant_id,a.rollout_id)
                JOIN dzoka.perception_components c ON (c.tenant_id,c.id,c.report_id)=(sl.tenant_id,sl.component_id,sl.report_id)
                JOIN dzoka.reports r ON (r.tenant_id,r.id,r.current_revision)=(c.tenant_id,c.report_id,c.source_revision)
                JOIN dzoka.perception_profiles p ON (p.tenant_id,p.id)=(c.tenant_id,c.profile_id)
                JOIN dzoka.perception_jobs j ON (j.tenant_id,j.id)=(c.tenant_id,c.job_id)
                CROSS JOIN (SELECT ?::text AS category) filter
                WHERE p.enabled AND p.task IN ('JOINT_IMAGE','JOINT_TEXT')
                  AND p.manifest->'sharedSpace'->>'key'=c.evidence->>'spaceKey'
                  AND c.evidence->>'spaceKey' ~ '^[0-9a-f]{64}$'
                  AND c.evidence->>'representation'='joint-embedding-v1'
                  AND j.state='SUCCEEDED' AND c.input_sha256=j.input_sha256
                  AND ((p.task='JOINT_IMAGE' AND EXISTS(SELECT 1 FROM dzoka.media_assets m
                       WHERE (m.tenant_id,m.id,m.report_id)=(j.tenant_id,j.media_id,r.id)
                         AND m.state='SANITIZED' AND NOT m.purge_pending AND m.expires_at>statement_timestamp()
                         AND m.derivative_sha256=c.input_sha256))
                    OR (p.task='JOINT_TEXT' AND j.media_id IS NULL AND c.evidence->'truncated'='false'::jsonb))
                  AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
                  AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                  AND (r.id=s.id OR (r.kind<>s.kind AND (filter.category IS NULL OR EXISTS (
                    SELECT 1 FROM dzoka.report_revisions cv WHERE
                     (cv.tenant_id,cv.report_id,cv.revision)=(r.tenant_id,r.id,r.current_revision)
                     AND cv.category=filter.category))))
                ORDER BY c.id LIMIT ?
                """)) {
            query.setObject(1,tenant);
            query.setObject(2,source.report());
            query.setInt(3,source.revision());
            query.setLong(4,activation);
            query.setString(5,category);
            query.setInt(6,PIN_LIMIT+1);
            query.setQueryTimeout(2);
            try (var rows=query.executeQuery()) {
                while (rows.next()) {
                    var values=JSON.readTree(rows.getString(7));
                    if (values==null || !values.isArray() || values.size()!=768) throw new IllegalArgumentException("Invalid pinned joint vector");
                    double[] vector=new double[768];
                    for (int i=0;i<vector.length;i++) {
                        if (!values.get(i).isNumber()) throw new IllegalArgumentException("Invalid joint vector coordinate");
                        vector[i]=values.get(i).asDouble();
                    }
                    pins.add(new Pin(rows.getObject(1,UUID.class),rows.getInt(2),rows.getBoolean(3),rows.getString(4),
                            new Vector(rows.getObject(5,UUID.class),rows.getString(6),vector)));
                }
            }
        } catch (SQLException | IllegalArgumentException | tools.jackson.core.JacksonException failure) {
            db.rollback(saved);
            db.releaseSavepoint(saved);
            return new JointRoute(false,true,Map.of());
        }
        db.releaseSavepoint(saved);
        if (pins.size()>PIN_LIMIT) return new JointRoute(false,true,Map.of());

        var sourcePins=pins.stream().filter(Pin::source).toList();
        if (sourcePins.isEmpty()) return JointRoute.UNAVAILABLE;
        Map<UUID,JointMatch> matches=new HashMap<>();
        for (Pin candidate:pins) {
            if (candidate.source()) continue;
            for (Pin left:sourcePins) {
                if (left.task().equals("JOINT_TEXT") && candidate.task().equals("JOINT_TEXT")) continue;
                Double score=Matching.cosine(left.vector(),candidate.vector());
                if (score==null) continue;
                var provenance=new JointEvidence(left.vector().component(),candidate.vector().component(),
                        left.vector().spaceKey(),left.task(),candidate.task());
                var comparison=new JointMatch(candidate.revision(),score,provenance);
                matches.merge(candidate.report(),comparison,(first,second) ->
                        second.similarity()>first.similarity()?second:first);
            }
        }
        return new JointRoute(true,false,matches);
    }
}
