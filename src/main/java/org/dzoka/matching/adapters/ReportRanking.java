package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.dzoka.matching.domain.Matching;
import org.dzoka.matching.domain.Matching.IndexedRoute;
import org.dzoka.matching.domain.Matching.Observation;

final class ReportRanking {
    private ReportRanking() {}

    private static final String SOURCE = """
        WITH source AS (
          SELECT r.kind,v.*,?::text AS filter_category FROM dzoka.reports r JOIN dzoka.report_revisions v
            ON (v.tenant_id,v.report_id,v.revision)=(r.tenant_id,r.id,r.current_revision)
          WHERE r.tenant_id=? AND r.id=? AND r.current_revision=?
            AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
            AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
        )
        """;
    private static final String VISIBLE = """
        JOIN dzoka.reports r ON (r.tenant_id,r.id,r.current_revision)=(v.tenant_id,v.report_id,v.revision)
        WHERE r.kind<>s.kind AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
          AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
          AND (s.filter_category IS NULL OR EXISTS (
            SELECT 1 FROM dzoka.report_revisions cv WHERE
              (cv.tenant_id,cv.report_id,cv.revision)=(r.tenant_id,r.id,r.current_revision)
              AND cv.category=s.filter_category))
        """;

    private static final String LEXICAL = SOURCE + """
        SELECT v.report_id,v.revision FROM source s
        CROSS JOIN to_tsquery('simple'::regconfig,?) q
        JOIN dzoka.report_revisions v ON v.tenant_id=s.tenant_id
          AND to_tsvector('simple'::regconfig,coalesce(v.normalized_text,'')) @@ q
        """ + VISIBLE + """
        ORDER BY ts_rank(to_tsvector('simple'::regconfig,coalesce(v.normalized_text,'')),q) DESC,v.report_id
        LIMIT 21
        """;

    private static final String CONTEXT = SOURCE + """
        , candidates AS (
          SELECT v.tenant_id,v.report_id,v.revision FROM source s JOIN dzoka.report_revisions v ON v.tenant_id=s.tenant_id
            AND v.latitude BETWEEN s.latitude-(205000+s.uncertainty_meters)/111000
                               AND s.latitude+(205000+s.uncertainty_meters)/111000
          UNION
          SELECT v.tenant_id,v.report_id,v.revision FROM source s JOIN dzoka.report_revisions v ON v.tenant_id=s.tenant_id
            AND v.occurred_after IS NOT NULL AND v.occurred_before IS NOT NULL
            AND tstzrange(v.occurred_after,v.occurred_before,'[]') &&
                tstzrange(s.occurred_after-interval '86401 seconds',s.occurred_before+interval '86401 seconds','[]')
            AND s.occurred_after IS NOT NULL AND s.occurred_before IS NOT NULL
        ), ranked AS (
          SELECT v.report_id,v.revision,greatest(
            CASE WHEN s.latitude IS NOT NULL AND v.latitude IS NOT NULL THEN
              greatest(0,1-greatest(0,12742000*asin(sqrt(least(1,
                power(sin(radians(v.latitude-s.latitude)/2),2)+cos(radians(s.latitude))*
                cos(radians(v.latitude))*power(sin(radians(v.longitude-s.longitude)/2),2))))
                -s.uncertainty_meters-v.uncertainty_meters)/5000)
                /(1+(s.uncertainty_meters+v.uncertainty_meters)/1000)
            END,
            CASE WHEN s.occurred_after IS NOT NULL AND s.occurred_before IS NOT NULL
                   AND v.occurred_after IS NOT NULL AND v.occurred_before IS NOT NULL THEN
              0.4*(1-greatest(0,
                floor(extract(epoch from s.occurred_after))-floor(extract(epoch from v.occurred_before)),
                floor(extract(epoch from v.occurred_after))-floor(extract(epoch from s.occurred_before)))/86400)
            END) AS relevance
          FROM source s JOIN candidates c ON c.tenant_id=s.tenant_id
          JOIN dzoka.report_revisions v ON (v.tenant_id,v.report_id,v.revision)=(c.tenant_id,c.report_id,c.revision)
        """ + VISIBLE + """
        )
        SELECT report_id,revision FROM ranked WHERE relevance>0 ORDER BY relevance DESC,report_id LIMIT 21
        """;

    static IndexedRoute lexical(Connection db, UUID tenant, Observation source,String category) throws SQLException {
        String query=Matching.tokens(source.text()).stream().sorted().limit(64)
                .map(word -> "'" + word + "'").collect(Collectors.joining(" | "));
        if (query.isEmpty()) return new IndexedRoute("LEXICAL_GIN",Map.of(),true,false);
        return rank(db,tenant,source,category,"LEXICAL_GIN",true,LEXICAL,query);
    }

    static IndexedRoute context(Connection db, UUID tenant, Observation source,String category) throws SQLException {
        if (source.position()==null && (source.after()==null || source.before()==null)) {
            return new IndexedRoute("SPATIAL_TIME_INDEX",Map.of(),false,false);
        }
        return rank(db,tenant,source,category,"SPATIAL_TIME_INDEX",false,CONTEXT,null);
    }

    static IndexedRoute identifiers(Connection db, UUID tenant, Observation source,String category) throws SQLException {
        if (source.identifier()==null) return new IndexedRoute("IDENTIFIER_INDEX",Map.of(),false,false);
        return rank(db,tenant,source,category,"IDENTIFIER_INDEX",false,SOURCE + """
            SELECT v.report_id,v.revision FROM source s
            JOIN dzoka.match_identifiers i ON (i.tenant_id,i.report_id,i.revision)=(s.tenant_id,s.report_id,s.revision)
            JOIN dzoka.match_identifiers v ON (v.tenant_id,v.key_version,v.scope_digest,v.digest)=
                                              (i.tenant_id,i.key_version,i.scope_digest,i.digest)
            """ + VISIBLE + " ORDER BY v.report_id LIMIT 21",null);
    }

    static IndexedRoute categories(Connection db, UUID tenant, Observation source,String category) throws SQLException {
        if (source.category()==null || (category!=null && !category.equals(source.category()))) {
            return new IndexedRoute("CATEGORY_INDEX",Map.of(),false,false);
        }
        return rank(db,tenant,source,category,"CATEGORY_INDEX",false,SOURCE + """
            SELECT v.report_id,v.revision FROM source s JOIN dzoka.report_revisions v
              ON v.tenant_id=s.tenant_id AND v.category=s.category AND v.category IS NOT NULL
            """ + VISIBLE + " ORDER BY v.report_id LIMIT 21",null);
    }

    private static IndexedRoute rank(Connection db, UUID tenant, Observation source,String category,String backend,
                                     boolean approximate, String sql, String terms) throws SQLException {
        var saved=db.setSavepoint();
        Map<UUID,Integer> found=new HashMap<>();
        try (PreparedStatement query=db.prepareStatement(sql)) {
            query.setString(1,category);
            query.setObject(2,tenant);
            query.setObject(3,source.report());
            query.setInt(4,source.revision());
            if (terms!=null) query.setString(5,terms);
            query.setQueryTimeout(2);
            try (var rows=query.executeQuery()) {
                while (rows.next()) found.put(rows.getObject(1,UUID.class),rows.getInt(2));
            }
        } catch (SQLException failure) {
            db.rollback(saved);
            db.releaseSavepoint(saved);
            return new IndexedRoute(backend,Map.of(),approximate,true);
        }
        db.releaseSavepoint(saved);
        return new IndexedRoute(backend,found,approximate,false);
    }
}
