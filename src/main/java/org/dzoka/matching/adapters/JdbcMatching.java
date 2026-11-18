package org.dzoka.matching.adapters;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Matching;
import org.dzoka.matching.domain.Matching.*;
import org.dzoka.matching.ports.MatchingUseCases;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class JdbcMatching implements MatchingUseCases {
    private final TenantTransactions transactions;
    private final boolean pgvector;
    private final QdrantRanking qdrant;

    public JdbcMatching(TenantTransactions transactions,@Value("${dzoka.matching.vector-backend:exact}") String backend,
                        @Value("${dzoka.matching.qdrant-origin:}") String qdrantOrigin,
                        @Value("${dzoka.matching.qdrant-api-key:}") String qdrantApiKey) {
        if (!backend.equals("exact") && !backend.equals("pgvector") && !backend.equals("qdrant")) {
            throw new IllegalArgumentException("Invalid vector backend");
        }
        this.transactions=transactions;
        this.pgvector=backend.equals("pgvector");
        this.qdrant=backend.equals("qdrant") ? new QdrantRanking(qdrantOrigin,qdrantApiKey) : null;
    }

    /** Load one statement snapshot under existing report visibility and tenant row policies. */
    private static final String SEARCH = """
        WITH selected AS (
          SELECT tenant_id,id,kind,current_revision,true AS is_source
          FROM dzoka.reports WHERE tenant_id=? AND id=?
            AND deleted_at IS NULL AND expires_at>statement_timestamp()
            AND (owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
        ), pool AS (
          SELECT r.tenant_id,r.id,r.kind,r.current_revision,false AS is_source
          FROM dzoka.reports r JOIN selected s ON s.tenant_id=r.tenant_id AND s.kind<>r.kind
          WHERE r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
            AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
          LIMIT ?
        ), visible AS (SELECT * FROM selected UNION ALL SELECT * FROM pool)
        SELECT r.id,r.is_source,r.current_revision,v.normalized_text,v.latitude,v.longitude,v.uncertainty_meters,
               v.occurred_after,v.occurred_before,i.key_version,i.scope_digest,i.digest,i.attestation_id,
               a.version AS activation_version,c.id AS component_id,e.space_key,c.vector
        FROM visible r JOIN dzoka.report_revisions v
          ON (v.tenant_id,v.report_id,v.revision)=(r.tenant_id,r.id,r.current_revision)
        LEFT JOIN dzoka.match_identifiers i
          ON (i.tenant_id,i.report_id,i.revision)=(r.tenant_id,r.id,r.current_revision)
        LEFT JOIN dzoka.fingerprint_active a ON a.tenant_id=r.tenant_id
        LEFT JOIN dzoka.fingerprint_snapshot_slots slot
          ON (slot.tenant_id,slot.rollout_id,slot.report_id)=(r.tenant_id,a.rollout_id,r.id)
          AND slot.space_id=? AND slot.state='AVAILABLE'
        LEFT JOIN dzoka.embedding_spaces e
          ON (e.tenant_id,e.id)=(slot.tenant_id,slot.space_id) AND e.enabled AND e.metric='COSINE'
        LEFT JOIN dzoka.fingerprint_components c
          ON (c.tenant_id,c.id,c.report_id,c.space_id)=(slot.tenant_id,slot.component_id,slot.report_id,e.id)
          AND c.source_revision=r.current_revision AND cardinality(c.vector)=e.dimensions AND NOT c.truncated
        """;

    @Override public Search search(VerifiedIdentity actor, UUID tenant, UUID report, UUID space, int limit) {
        if (limit<1 || limit>Matching.ROUTE_LIMIT) throw new IllegalArgumentException("Matching limit out of range");
        return transactions.execute(actor,tenant,"matches:read",connection -> {
            try (var scope=connection.prepareStatement("SELECT dzoka.permitted('reports:read')")) {
                try (var result=scope.executeQuery()) {
                    if (!result.next() || !result.getBoolean(1)) throw new Failures.Forbidden();
                }
            }
            Observation source=null;
            Long activation=null;
            var pool=new ArrayList<Observation>();
            try (var query=connection.prepareStatement(SEARCH)) {
                query.setObject(1,tenant);query.setObject(2,report);query.setInt(3,Matching.POOL_LIMIT+1);
                query.setObject(4,space,java.sql.Types.OTHER);
                query.setQueryTimeout(3);
                try (var rows=query.executeQuery()) {
                    while (rows.next()) {
                        Observation row=read(rows);
                        if (rows.getBoolean("is_source")) {
                            source=row;
                            activation=rows.getObject("activation_version",Long.class);
                        } else pool.add(row);
                    }
                }
            }
            if (source==null) throw new Failures.NotFound();
            VectorRoute ranking=null;
            if (source.vector()!=null && pool.size()<=Matching.POOL_LIMIT) {
                if (pgvector) ranking=PgVectorRanking.rank(connection,source.vector(),pool);
                if (qdrant!=null) ranking=qdrant.rank(tenant,source.vector(),pool);
            }
            return Matching.search(source,pool,activation,limit,ranking);
        });
    }

    /** Only a separately authorized verifier may attest protected, issuer-keyed serial digests. */
    @Override public void attest(VerifiedIdentity actor, UUID tenant, UUID report, int revision, int keyVersion,
                       String scopeDigest, String digest, UUID attestation) {
        if (revision<1 || keyVersion<1 || attestation==null || !validDigest(scopeDigest) || !validDigest(digest)) {
            throw new IllegalArgumentException("Protected serial attestation required");
        }
        transactions.execute(actor,tenant,"matches:attest-identifiers",connection -> {
            // Evidence remains pinned to the specified revision if a new revision is published.
            try (var source=connection.prepareStatement("""
                    SELECT 1 FROM dzoka.reports WHERE tenant_id=? AND id=? AND current_revision=?
                      AND deleted_at IS NULL AND expires_at>statement_timestamp()
                      AND (owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                    """)) {
                source.setObject(1,tenant);source.setObject(2,report);source.setInt(3,revision);
                try (var row=source.executeQuery()) { if (!row.next()) throw new Failures.NotFound(); }
            }
            try (var insert=connection.prepareStatement("""
                    INSERT INTO dzoka.match_identifiers
                     (tenant_id,report_id,revision,namespace,key_version,scope_digest,digest,attestation_id,actor_ref)
                    VALUES (?,?,?,'manufacturer_serial',?,?,?,?,?) ON CONFLICT DO NOTHING
                    """)) {
                insert.setObject(1,tenant);insert.setObject(2,report);insert.setInt(3,revision);
                insert.setInt(4,keyVersion);insert.setString(5,scopeDigest);insert.setString(6,digest);
                insert.setObject(7,attestation);insert.setString(8,actor.reference());
                if (insert.executeUpdate()!=1) throw new Failures.Conflict();
            }
            return null;
        });
    }

    private static boolean validDigest(String value) { return value!=null && value.matches("[0-9a-f]{64}"); }

    private static Observation read(ResultSet row) throws SQLException {
        Double latitude=row.getObject("latitude",Double.class);
        Position position=latitude==null?null:new Position(latitude,row.getDouble("longitude"),row.getDouble("uncertainty_meters"));
        String digest=row.getString("digest");
        Identifier identifier=digest==null?null:new Identifier(row.getInt("key_version"),row.getString("scope_digest"),digest,
                row.getObject("attestation_id",UUID.class));
        Vector vector=null;
        var array=row.getArray("vector");
        if (array!=null) {
            try {
                Object[] elements=(Object[])array.getArray();
                double[] values=new double[elements.length];
                for (int i=0; i<values.length; i++) values[i]=elements[i] instanceof Number n?n.doubleValue():Double.NaN;
                vector=new Vector(row.getObject("component_id",UUID.class),row.getString("space_key"),values);
            } finally { array.free(); }
        }
        return new Observation(row.getObject("id",UUID.class),row.getInt("current_revision"),row.getString("normalized_text"),
                position,instant(row,"occurred_after"),instant(row,"occurred_before"),identifier,vector);
    }

    private static Instant instant(ResultSet row, String name) throws SQLException {
        OffsetDateTime value=row.getObject(name,OffsetDateTime.class);
        return value==null?null:value.toInstant();
    }
}
