package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;
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
    private final boolean indexed;
    private final QdrantRanking qdrant;
    private final String qdrantWriteKey;

    public JdbcMatching(TenantTransactions transactions,@Value("${dzoka.matching.vector-backend:exact}") String backend,
                        @Value("${dzoka.matching.qdrant-origin:}") String qdrantOrigin,
                        @Value("${dzoka.matching.qdrant-api-key:}") String qdrantApiKey,
                        @Value("${dzoka.matching.qdrant-write-key:}") String qdrantWriteKey) {
        if (!java.util.Set.of("exact","pgvector","pgvector-indexed","qdrant").contains(backend)) {
            throw new IllegalArgumentException("Invalid vector backend");
        }
        this.transactions=transactions;
        this.pgvector=backend.equals("pgvector");
        this.indexed=backend.equals("pgvector-indexed");
        this.qdrant=backend.equals("qdrant") ? new QdrantRanking(qdrantOrigin,qdrantApiKey) : null;
        this.qdrantWriteKey=qdrantWriteKey;
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
        checkLimit(limit);
        return transactions.execute(actor,tenant,"matches:read",connection -> {
            requireReportRead(connection);
            return search(connection,tenant,report,space,limit);
        });
    }

    @Override public Search recordSearch(VerifiedIdentity actor, UUID tenant, UUID report, UUID space, int limit, UUID run) {
        checkLimit(limit);
        return transactions.execute(actor,tenant,"matches:read",connection -> {
            requireReportRead(connection);
            MatchingHistory.lockAndClean(connection,tenant,actor.reference());
            var previous=MatchingHistory.read(connection,tenant,report,run);
            if (previous!=null) {
                if (previous.limit()!=limit || !Objects.equals(previous.space(),space)) throw new Failures.Conflict();
                return previous.result();
            }
            var result=search(connection,tenant,report,space,limit);
            MatchingHistory.write(connection,tenant,actor.reference(),space,limit,run,result);
            return result;
        });
    }

    @Override public Search readSearch(VerifiedIdentity actor, UUID tenant, UUID report, UUID run) {
        return transactions.execute(actor,tenant,"matches:read",connection -> {
            requireReportRead(connection);
            var record=MatchingHistory.read(connection,tenant,report,run);
            if (record==null) throw new Failures.NotFound();
            return record.result();
        });
    }

    @Override public int pruneHistory(VerifiedIdentity actor, UUID tenant) {
        return transactions.execute(actor,tenant,"matches:maintain-history",connection -> MatchingHistory.prune(connection,tenant));
    }

    private static void checkLimit(int limit) {
        if (limit<1 || limit>Matching.ROUTE_LIMIT) throw new IllegalArgumentException("Matching limit out of range");
    }

    @Override public Projection reconcile(VerifiedIdentity actor, UUID tenant, UUID space) {
        return transactions.execute(actor,tenant,"matches:maintain-projection",connection -> {
            requireReportRead(connection);
            try(var permissions=connection.prepareStatement("SELECT dzoka.permitted('reports:read:any')");
                var result=permissions.executeQuery()) {
                if(!result.next() || !result.getBoolean(1)) throw new Failures.Forbidden();
            }
            if(qdrant==null || qdrantWriteKey.isBlank()) throw new Failures.Unavailable(new IllegalStateException("Projection management disabled"));
            try(var lock=connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) {
                lock.setString(1,"ai:"+tenant);lock.execute();
            }
            String spaceKey=null;
            Long activation=null;
            var vectors=new ArrayList<Vector>();
            try(var query=connection.prepareStatement("""
                    SELECT e.space_key,a.version,c.id AS component_id,c.vector
                    FROM dzoka.embedding_spaces e
                    LEFT JOIN dzoka.fingerprint_active a ON a.tenant_id=e.tenant_id
                    LEFT JOIN dzoka.fingerprint_snapshot_slots slot
                      ON (slot.tenant_id,slot.rollout_id,slot.space_id)=(a.tenant_id,a.rollout_id,e.id) AND slot.state='AVAILABLE'
                    LEFT JOIN dzoka.reports r ON (r.tenant_id,r.id)=(slot.tenant_id,slot.report_id)
                      AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
                    LEFT JOIN dzoka.fingerprint_components c
                      ON (c.tenant_id,c.id,c.report_id,c.source_revision)=(r.tenant_id,slot.component_id,r.id,r.current_revision)
                      AND c.space_id=e.id AND e.enabled AND NOT c.truncated AND cardinality(c.vector)=e.dimensions
                    WHERE e.tenant_id=? AND e.id=?
                    LIMIT 513
                    """)) {
                query.setObject(1,tenant);query.setObject(2,space);query.setQueryTimeout(3);
                int observed=0;
                try(var rows=query.executeQuery()) {
                    while(rows.next()) {
                        if(++observed>Matching.POOL_LIMIT) throw new Failures.Conflict();
                        spaceKey=rows.getString("space_key");activation=rows.getObject("version",Long.class);
                        Vector vector=readVector(rows);
                        if(vector!=null) vectors.add(vector);
                    }
                }
            }
            if(spaceKey==null) throw new Failures.NotFound();
            try { qdrant.reconcile(tenant,spaceKey,vectors,qdrantWriteKey); }
            catch(java.io.IOException | tools.jackson.core.JacksonException error) {
                throw new Failures.Unavailable(new IllegalStateException("Projection reconciliation incomplete"));
            }
            return new Projection(space,activation,vectors.size());
        });
    }

    private static void requireReportRead(Connection connection) throws SQLException {
        try (var scope=connection.prepareStatement("SELECT dzoka.permitted('reports:read')");
             var result=scope.executeQuery()) {
            if (!result.next() || !result.getBoolean(1)) throw new Failures.Forbidden();
        }
    }

    private Search search(Connection connection, UUID tenant, UUID report, UUID space, int limit) throws SQLException {
        boolean indexOnly=indexed && largePool(connection,tenant,report);
        Snapshot snapshot=snapshot(connection,tenant,report,space,indexOnly?java.util.Set.of():null);
        Observation source=snapshot.source();
        Long activation=snapshot.activation();
        var pool=snapshot.pool();
        VectorRoute ranking=null;
        boolean overflow=indexOnly || pool.size()>Matching.POOL_LIMIT;
        if (indexed && source.vector()!=null && activation!=null) {
            ranking=PgVectorIndexed.rank(connection,tenant,source,space,activation);
        } else if (source.vector()!=null && !overflow) {
            if (pgvector) ranking=PgVectorRanking.rank(connection,source.vector(),pool);
            if (qdrant!=null) ranking=qdrant.rank(tenant,source.vector(),pool);
        }
        if(indexed && overflow) {
            var selected=new java.util.HashSet<>(identifierCandidates(connection,tenant,source));
            if(ranking!=null) selected.addAll(ranking.similarities().keySet());
            Snapshot indexedSnapshot=snapshot(connection,tenant,report,space,selected);
            Observation current=indexedSnapshot.source();
            if(Objects.equals(activation,indexedSnapshot.activation()) && current.revision()==source.revision()
                    && Objects.equals(component(source),component(current)) && Objects.equals(current.identifier(),source.identifier())) {
                return Matching.search(current,indexedSnapshot.pool(),activation,limit,ranking,true);
            }
        }
        return overflow?Matching.overCapacity(source,activation):Matching.search(source,pool,activation,limit,ranking);
    }

    private static UUID component(Observation source) {
        return source.vector()==null?null:source.vector().component();
    }

    private static java.util.List<UUID> identifierCandidates(Connection db, UUID tenant, Observation source) throws SQLException {
        Identifier identifier=source.identifier();
        if(identifier==null) return java.util.List.of();
        try(var query=db.prepareStatement("""
                SELECT i.report_id FROM dzoka.match_identifiers i
                JOIN dzoka.reports r ON (r.tenant_id,r.id,r.current_revision)=(i.tenant_id,i.report_id,i.revision)
                JOIN dzoka.reports s ON s.tenant_id=i.tenant_id AND s.id=? AND s.kind<>r.kind
                WHERE i.tenant_id=? AND i.key_version=? AND i.scope_digest=? AND i.digest=?
                ORDER BY i.report_id LIMIT 21
                """)) {
            query.setObject(1,source.report());query.setObject(2,tenant);query.setInt(3,identifier.keyVersion());
            query.setString(4,identifier.scopeDigest());query.setString(5,identifier.digest());query.setQueryTimeout(2);
            var found=new ArrayList<UUID>();
            try(var rows=query.executeQuery()) { while(rows.next()) found.add(rows.getObject(1,UUID.class)); }
            return found;
        }
    }

    private static boolean largePool(Connection db, UUID tenant, UUID source) throws SQLException {
        try(var query=db.prepareStatement("""
                SELECT count(*)>512 FROM (
                  SELECT c.id FROM dzoka.reports s JOIN dzoka.reports c ON c.tenant_id=s.tenant_id AND c.kind<>s.kind
                  WHERE s.tenant_id=? AND s.id=? AND c.deleted_at IS NULL AND c.expires_at>statement_timestamp()
                    AND (c.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))
                  LIMIT 513
                ) bounded
                """)) {
            query.setObject(1,tenant);query.setObject(2,source);query.setQueryTimeout(3);
            try(var row=query.executeQuery()) { return row.next() && row.getBoolean(1); }
        }
    }

    private record Snapshot(Observation source, Long activation, java.util.List<Observation> pool) {}

    private static Snapshot snapshot(Connection connection, UUID tenant, UUID report, UUID space,
                                     java.util.Set<UUID> selection) throws SQLException {
        Observation source=null;
        Long activation=null;
        var pool=new ArrayList<Observation>();
        String sql=selection==null?SEARCH:SEARCH.replace("LIMIT ?","AND r.id=ANY(?) LIMIT ?");
        var ids=selection==null?null:connection.createArrayOf("uuid",selection.toArray());
        try (var query=connection.prepareStatement(sql)) {
            query.setObject(1,tenant);query.setObject(2,report);
            if(ids==null) {query.setInt(3,Matching.POOL_LIMIT+1);query.setObject(4,space,java.sql.Types.OTHER);}
            else {
                query.setArray(3,ids);query.setInt(4,Matching.POOL_LIMIT+1);query.setObject(5,space,java.sql.Types.OTHER);
            }
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
        } finally { if(ids!=null) ids.free(); }
        if (source==null) throw new Failures.NotFound();
        return new Snapshot(source,activation,pool);
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
        return new Observation(row.getObject("id",UUID.class),row.getInt("current_revision"),row.getString("normalized_text"),
                position,instant(row,"occurred_after"),instant(row,"occurred_before"),identifier,readVector(row));
    }

    private static Vector readVector(ResultSet row) throws SQLException {
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
        return vector;
    }

    private static Instant instant(ResultSet row, String name) throws SQLException {
        OffsetDateTime value=row.getObject(name,OffsetDateTime.class);
        return value==null?null:value.toInstant();
    }
}
