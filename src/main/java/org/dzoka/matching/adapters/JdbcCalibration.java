package org.dzoka.matching.adapters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.matching.domain.Calibration;
import org.dzoka.matching.domain.Calibration.Approval;
import org.dzoka.matching.domain.Calibration.Draft;
import org.dzoka.matching.domain.Matching.Search;
import org.dzoka.matching.domain.Matching.Route;
import org.dzoka.matching.domain.Matching.RouteState;
import org.dzoka.matching.ports.MatchingCalibration;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class JdbcCalibration implements MatchingCalibration {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String PROPOSE="matches:propose-calibration";
    private static final String REVIEW="matches:review-calibration";
    private final TenantTransactions transactions;

    public JdbcCalibration(TenantTransactions transactions) { this.transactions=transactions; }

    @Override public void propose(VerifiedIdentity actor,UUID tenant,UUID artifact,Draft draft) {
        transactions.execute(actor,tenant,PROPOSE,db -> {
            lock(db,tenant);
            try (var insert=db.prepareStatement("""
                    INSERT INTO dzoka.match_calibration_drafts
                      (tenant_id,id,space_id,activation_version,source_kind,scoring_version,vector_backend,proposer_ref,artifact)
                    VALUES (?,?,?,?,?,?,?,?,?::jsonb) ON CONFLICT DO NOTHING
                    """)) {
                insert.setObject(1,tenant);
                insert.setObject(2,artifact);
                insert.setObject(3,draft.spaceId());
                insert.setLong(4,draft.activationVersion());
                insert.setString(5,draft.sourceKind());
                insert.setString(6,draft.scoringVersion());
                insert.setString(7,draft.vectorBackend());
                insert.setString(8,actor.reference());
                insert.setString(9,JSON.writeValueAsString(draft));
                if (insert.executeUpdate()==0) {
                    Registered existing=registered(db,tenant,artifact);
                    if (!existing.draft().equals(draft) || !existing.proposer().equals(actor.reference())) {
                        throw new Failures.Conflict();
                    }
                }
            } catch (SQLException failure) {
                if ("23503".equals(failure.getSQLState())) throw new Failures.NotFound();
                throw failure;
            }
            return null;
        });
    }

    @Override public Draft draft(VerifiedIdentity actor,UUID tenant,UUID artifact) {
        return transactions.execute(actor,tenant,REVIEW,db -> registered(db,tenant,artifact).draft());
    }

    @Override public void approve(VerifiedIdentity actor,UUID tenant,UUID artifact,Approval approval) {
        transactions.execute(actor,tenant,REVIEW,db -> {
            lock(db,tenant);
            var entry=registered(db,tenant,artifact);
            if (actor.reference().equals(entry.proposer())) throw new Failures.Forbidden();
            if (!approval.accepts(entry.draft())) throw new Failures.Conflict();
            try (var existing=db.prepareStatement("""
                    SELECT reviewer_ref,review_sha256,maximum_brier,minimum_bin_support,valid_hours
                    FROM dzoka.match_calibration_approvals WHERE tenant_id=? AND draft_id=?
                    """)) {
                scope(existing,tenant,artifact);
                try (var rows=existing.executeQuery()) {
                    if (rows.next()) {
                        if (!actor.reference().equals(rows.getString(1)) ||
                                !approval.equals(new Approval(rows.getString(2),rows.getDouble(3),rows.getInt(4),rows.getInt(5)))) {
                            throw new Failures.Conflict();
                        }
                        return null;
                    }
                }
            }
            requireCurrentScope(db,tenant,entry.draft());
            try (var insert=db.prepareStatement("""
                    INSERT INTO dzoka.match_calibration_approvals
                      (tenant_id,draft_id,reviewer_ref,review_sha256,maximum_brier,minimum_bin_support,valid_hours)
                    VALUES (?,?,?,?,?,?,?)
                    """)) {
                scope(insert,tenant,artifact);
                insert.setString(3,actor.reference());
                insert.setString(4,approval.reviewSha256());
                insert.setDouble(5,approval.maximumBrier());
                insert.setInt(6,approval.minimumBinSupport());
                insert.setInt(7,approval.validHours());
                insert.executeUpdate();
            }
            return null;
        });
    }

    @Override public void revoke(VerifiedIdentity actor,UUID tenant,UUID artifact,String reviewSha256) {
        if (!Calibration.digest(reviewSha256)) throw new IllegalArgumentException("Revocation review required");
        transactions.execute(actor,tenant,REVIEW,db -> {
            lock(db,tenant);
            try (var insert=db.prepareStatement("""
                    INSERT INTO dzoka.match_calibration_revocations(tenant_id,draft_id,revoked_by,review_sha256)
                    SELECT tenant_id,draft_id,?,? FROM dzoka.match_calibration_approvals
                    WHERE tenant_id=? AND draft_id=? ON CONFLICT DO NOTHING
                    """)) {
                insert.setString(1,actor.reference());
                insert.setString(2,reviewSha256);
                insert.setObject(3,tenant);
                insert.setObject(4,artifact);
                if (insert.executeUpdate()==1) return null;
            }
            try (var existing=db.prepareStatement("""
                    SELECT revoked_by,review_sha256 FROM dzoka.match_calibration_revocations
                    WHERE tenant_id=? AND draft_id=?
                    """)) {
                scope(existing,tenant,artifact);
                try (var rows=existing.executeQuery()) {
                    if (!rows.next()) throw new Failures.NotFound();
                    if (!actor.reference().equals(rows.getString(1)) || !reviewSha256.equals(rows.getString(2))) {
                        throw new Failures.Conflict();
                    }
                }
            }
            return null;
        });
    }

    private record Registered(Draft draft,String proposer) {}
    private record Active(UUID id,Draft draft,int minimumSupport) {}

    static Search assess(Connection db,UUID tenant,UUID space,Search result,boolean historical) throws SQLException {
        if (space==null || result.activationVersion()==null || result.candidates().stream().noneMatch(candidate ->
                historical?candidate.confidence()!=null:candidate.vectorEvidence()!=null)) return result;
        Active active=active(db,tenant,space,result);
        return result.withCandidates(result.candidates().stream().map(candidate -> {
            if (active==null) return candidate.confidence()==null?candidate:candidate.withConfidence(null);
            if (historical) return candidate.confidence()==null || active.id().equals(candidate.confidence().artifact())
                    ?candidate:candidate.withConfidence(null);
            if (candidate.jointEvidence()!=null) return candidate;
            return candidate.withConfidence(Calibration.estimate(active.id(),active.draft(),active.minimumSupport(),
                    candidate.score(),candidate.vectorEvidence()));
        }).toList());
    }

    private static Active active(Connection db,UUID tenant,UUID space,Search result) throws SQLException {
        if (result.routes().stream().anyMatch(status ->
                status.route()==Route.JOINT_VECTOR && status.state()!=RouteState.UNAVAILABLE)) return null;
        String backend=result.routes().stream().filter(status -> status.route()==Route.EXACT_VECTOR)
                .map(status -> status.reason().replaceFirst("_ROUTE_LIMIT$","")).findFirst().orElse("");
        try (var sql=db.prepareStatement("""
                SELECT d.id,d.artifact,a.minimum_bin_support,
                    v.draft_id IS NULL AND a.approved_at+make_interval(hours=>a.valid_hours)>statement_timestamp()
                FROM dzoka.match_calibration_drafts d
                JOIN dzoka.match_calibration_approvals a ON (a.tenant_id,a.draft_id)=(d.tenant_id,d.id)
                LEFT JOIN dzoka.match_calibration_revocations v ON (v.tenant_id,v.draft_id)=(d.tenant_id,d.id)
                JOIN dzoka.fingerprint_active f ON (f.tenant_id,f.version)=(d.tenant_id,d.activation_version)
                JOIN dzoka.embedding_spaces e ON (e.tenant_id,e.id)=(d.tenant_id,d.space_id)
                JOIN dzoka.reports r ON r.tenant_id=d.tenant_id AND r.kind=d.source_kind
                WHERE d.tenant_id=? AND d.space_id=? AND d.activation_version=? AND d.scoring_version=? AND d.vector_backend=?
                  AND r.id=? AND r.current_revision=? AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
                  AND e.space_key=d.artifact->>'spaceKey' AND e.enabled AND dzoka.permitted('reports:read:any')
                ORDER BY a.approved_at DESC,d.id DESC LIMIT 1
                """)) {
            scope(sql,tenant,space);
            sql.setLong(3,result.activationVersion());
            sql.setString(4,result.scoringVersion());
            sql.setString(5,backend);
            sql.setObject(6,result.report());
            sql.setInt(7,result.revision());
            try (var rows=sql.executeQuery()) {
                if (!rows.next() || !rows.getBoolean(4)) return null;
                return new Active(rows.getObject(1,UUID.class),JSON.readValue(rows.getString(2),Draft.class),rows.getInt(3));
            }
        }
    }

    private static void requireCurrentScope(Connection db,UUID tenant,Draft draft) throws SQLException {
        try (var sql=db.prepareStatement("""
                SELECT 1 FROM dzoka.embedding_spaces e JOIN dzoka.fingerprint_active a ON a.tenant_id=e.tenant_id
                WHERE e.tenant_id=? AND e.id=? AND e.space_key=? AND a.version=? AND e.enabled AND e.metric='COSINE'
                  AND EXISTS(SELECT 1 FROM dzoka.fingerprint_snapshot_slots s WHERE s.tenant_id=a.tenant_id
                    AND s.rollout_id=a.rollout_id AND s.space_id=e.id AND s.state='AVAILABLE')
                """)) {
            scope(sql,tenant,draft.spaceId());
            sql.setString(3,draft.spaceKey());
            sql.setLong(4,draft.activationVersion());
            try (var rows=sql.executeQuery()) { if (!rows.next()) throw new Failures.Conflict(); }
        }
    }

    private static Registered registered(Connection db,UUID tenant,UUID artifact) throws SQLException {
        try (var sql=db.prepareStatement("""
                SELECT artifact,proposer_ref FROM dzoka.match_calibration_drafts WHERE tenant_id=? AND id=?
                """)) {
            scope(sql,tenant,artifact);
            try (var rows=sql.executeQuery()) {
                if (!rows.next()) throw new Failures.NotFound();
                return new Registered(JSON.readValue(rows.getString(1),Draft.class),rows.getString(2));
            }
        }
    }

    private static void lock(Connection db,UUID tenant) throws SQLException {
        try (var sql=db.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) {
            sql.setString(1,"ai:"+tenant);
            sql.execute();
        }
    }

    private static void scope(java.sql.PreparedStatement sql,UUID tenant,UUID resource) throws SQLException {
        sql.setObject(1,tenant);
        sql.setObject(2,resource);
    }
}
