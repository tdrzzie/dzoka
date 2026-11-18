CREATE TABLE dzoka.match_calibration_drafts (
  tenant_id uuid NOT NULL, id uuid NOT NULL, space_id uuid NOT NULL, activation_version bigint NOT NULL,
  source_kind text NOT NULL CHECK(source_kind IN ('LOST','FOUND')), scoring_version text NOT NULL, vector_backend text NOT NULL,
  proposer_ref text NOT NULL, artifact jsonb NOT NULL CHECK(jsonb_typeof(artifact)='object'),
  created_at timestamptz NOT NULL DEFAULT statement_timestamp(),
  PRIMARY KEY(tenant_id,id),
  FOREIGN KEY(tenant_id,space_id) REFERENCES dzoka.embedding_spaces(tenant_id,id)
);
CREATE INDEX match_calibration_scope ON dzoka.match_calibration_drafts
  (tenant_id,space_id,activation_version,source_kind,scoring_version,vector_backend);
CREATE TABLE dzoka.match_calibration_approvals (
  tenant_id uuid NOT NULL, draft_id uuid NOT NULL, reviewer_ref text NOT NULL,
  review_sha256 text NOT NULL CHECK(review_sha256 ~ '^[0-9a-f]{64}$'),
  maximum_brier double precision NOT NULL CHECK(maximum_brier BETWEEN 0 AND 1),
  minimum_bin_support integer NOT NULL CHECK(minimum_bin_support BETWEEN 2 AND 2048),
  valid_hours integer NOT NULL CHECK(valid_hours BETWEEN 1 AND 720),
  approved_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY(tenant_id,draft_id),
  FOREIGN KEY(tenant_id,draft_id) REFERENCES dzoka.match_calibration_drafts(tenant_id,id)
);
CREATE TABLE dzoka.match_calibration_revocations (
  tenant_id uuid NOT NULL, draft_id uuid NOT NULL, revoked_by text NOT NULL,
  review_sha256 text NOT NULL CHECK(review_sha256 ~ '^[0-9a-f]{64}$'),
  revoked_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY(tenant_id,draft_id),
  FOREIGN KEY(tenant_id,draft_id) REFERENCES dzoka.match_calibration_approvals(tenant_id,draft_id)
);
DO $$
DECLARE registry text;
BEGIN
  FOREACH registry IN ARRAY ARRAY['match_calibration_drafts','match_calibration_approvals','match_calibration_revocations']
  LOOP
    EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',registry);
    EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',registry);
    EXECUTE format('CREATE POLICY calibration_read ON dzoka.%I FOR SELECT TO dzoka_app USING (
      tenant_id=dzoka.current_tenant() AND
      (dzoka.permitted(''matches:propose-calibration'') OR dzoka.permitted(''matches:review-calibration'') OR
       (dzoka.permitted(''matches:read'') AND dzoka.permitted(''reports:read'') AND dzoka.permitted(''reports:read:any''))))',
      registry);
    EXECUTE format('GRANT SELECT ON dzoka.%I TO dzoka_app',registry);
  END LOOP;
END $$;
CREATE POLICY calibration_draft_insert ON dzoka.match_calibration_drafts FOR INSERT TO dzoka_app WITH CHECK (
  tenant_id=dzoka.current_tenant() AND proposer_ref=dzoka.current_actor_ref() AND dzoka.permitted('matches:propose-calibration'));
CREATE POLICY calibration_approval_insert ON dzoka.match_calibration_approvals FOR INSERT TO dzoka_app WITH CHECK (
  tenant_id=dzoka.current_tenant() AND reviewer_ref=dzoka.current_actor_ref() AND dzoka.permitted('matches:review-calibration')
  AND EXISTS (SELECT 1 FROM dzoka.match_calibration_drafts d WHERE
    d.tenant_id=match_calibration_approvals.tenant_id AND d.id=draft_id AND d.proposer_ref<>reviewer_ref));
CREATE POLICY calibration_revoke_insert ON dzoka.match_calibration_revocations FOR INSERT TO dzoka_app WITH CHECK (
  tenant_id=dzoka.current_tenant() AND revoked_by=dzoka.current_actor_ref() AND dzoka.permitted('matches:review-calibration'));
GRANT INSERT (tenant_id,id,space_id,activation_version,source_kind,scoring_version,vector_backend,proposer_ref,artifact)
  ON dzoka.match_calibration_drafts TO dzoka_app;
GRANT INSERT (tenant_id,draft_id,reviewer_ref,review_sha256,maximum_brier,minimum_bin_support,valid_hours)
  ON dzoka.match_calibration_approvals TO dzoka_app;
GRANT INSERT (tenant_id,draft_id,revoked_by,review_sha256) ON dzoka.match_calibration_revocations TO dzoka_app;
