CREATE FUNCTION dzoka.match_report_current(target uuid, revision_no integer) RETURNS boolean
LANGUAGE sql STABLE SET search_path=pg_catalog AS $$
  SELECT EXISTS (SELECT 1 FROM dzoka.reports r
    WHERE r.tenant_id=dzoka.current_tenant() AND r.id=target AND r.current_revision=revision_no
      AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
      AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any')))
$$;
REVOKE ALL ON FUNCTION dzoka.match_report_current(uuid,integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.match_report_current(uuid,integer) TO dzoka_app;

CREATE TABLE dzoka.match_search_runs (
  tenant_id uuid NOT NULL, id uuid NOT NULL, actor_ref text NOT NULL CHECK(length(actor_ref)=64),
  report_id uuid NOT NULL, revision integer NOT NULL,
  space_id uuid, requested_limit integer NOT NULL CHECK(requested_limit BETWEEN 1 AND 20),
  candidate_count integer NOT NULL CHECK(candidate_count BETWEEN 0 AND 20),
  summary jsonb NOT NULL CHECK(jsonb_typeof(summary)='object' AND NOT summary ? 'candidates' AND octet_length(summary::text)<=4096),
  created_at timestamptz NOT NULL DEFAULT statement_timestamp(),
  PRIMARY KEY(tenant_id,id),
  FOREIGN KEY(tenant_id,report_id,revision) REFERENCES dzoka.report_revisions(tenant_id,report_id,revision) ON DELETE CASCADE
);
CREATE INDEX match_run_actor_age ON dzoka.match_search_runs (tenant_id,actor_ref,created_at);
CREATE INDEX match_run_expiry ON dzoka.match_search_runs(created_at);
CREATE INDEX match_verified_serial_route ON dzoka.match_identifiers(tenant_id,key_version,scope_digest,digest,report_id);
CREATE TABLE dzoka.match_search_candidates (
  tenant_id uuid NOT NULL, run_id uuid NOT NULL, ordinal integer NOT NULL CHECK(ordinal BETWEEN 0 AND 19),
  report_id uuid NOT NULL, revision integer NOT NULL,
  evidence jsonb NOT NULL CHECK(jsonb_typeof(evidence)='object' AND octet_length(evidence::text)<=8192),
  PRIMARY KEY(tenant_id,run_id,ordinal),
  UNIQUE(tenant_id,run_id,report_id),
  FOREIGN KEY(tenant_id,run_id) REFERENCES dzoka.match_search_runs(tenant_id,id) ON DELETE CASCADE,
  FOREIGN KEY(tenant_id,report_id,revision) REFERENCES dzoka.report_revisions(tenant_id,report_id,revision) ON DELETE CASCADE
);
CREATE INDEX match_candidate_revision ON dzoka.match_search_candidates(tenant_id,report_id,revision);
ALTER TABLE dzoka.match_search_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.match_search_runs FORCE ROW LEVEL SECURITY;
ALTER TABLE dzoka.match_search_candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.match_search_candidates FORCE ROW LEVEL SECURITY;
CREATE POLICY matching_run_read ON dzoka.match_search_runs FOR SELECT TO dzoka_app USING(
  tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
  AND dzoka.permitted('matches:read') AND dzoka.permitted('reports:read')
  AND (created_at<=statement_timestamp()-interval '24 hours' OR dzoka.match_report_current(report_id,revision)));
CREATE POLICY matching_run_insert ON dzoka.match_search_runs FOR INSERT TO dzoka_app WITH CHECK(
  tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
  AND dzoka.permitted('matches:read') AND dzoka.permitted('reports:read') AND dzoka.match_report_current(report_id,revision));
CREATE POLICY matching_run_delete ON dzoka.match_search_runs FOR DELETE TO dzoka_app USING(
  tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
  AND dzoka.permitted('matches:read') AND dzoka.permitted('reports:read')
  AND created_at<=statement_timestamp()-interval '24 hours');
CREATE POLICY matching_run_expired_maintenance_read ON dzoka.match_search_runs FOR SELECT TO dzoka_app USING(
  tenant_id=dzoka.current_tenant() AND dzoka.permitted('matches:maintain-history')
  AND created_at<=statement_timestamp()-interval '24 hours');
CREATE POLICY matching_run_expired_maintenance_delete ON dzoka.match_search_runs FOR DELETE TO dzoka_app USING(
  tenant_id=dzoka.current_tenant() AND dzoka.permitted('matches:maintain-history')
  AND created_at<=statement_timestamp()-interval '24 hours');
CREATE POLICY matching_candidate_read ON dzoka.match_search_candidates FOR SELECT TO dzoka_app USING(
  tenant_id=dzoka.current_tenant() AND dzoka.match_report_current(report_id,revision)
  AND EXISTS(SELECT 1 FROM dzoka.match_search_runs r WHERE r.tenant_id=match_search_candidates.tenant_id
    AND r.id=run_id AND r.created_at>statement_timestamp()-interval '24 hours'));
CREATE POLICY matching_candidate_insert ON dzoka.match_search_candidates FOR INSERT TO dzoka_app WITH CHECK(
  tenant_id=dzoka.current_tenant() AND dzoka.match_report_current(report_id,revision)
  AND EXISTS(SELECT 1 FROM dzoka.match_search_runs r WHERE r.tenant_id=match_search_candidates.tenant_id
    AND r.id=run_id AND r.created_at>statement_timestamp()-interval '24 hours'));
GRANT SELECT, DELETE ON dzoka.match_search_runs TO dzoka_app;
GRANT INSERT (tenant_id,id,actor_ref,report_id,revision,space_id,requested_limit,candidate_count,summary) ON dzoka.match_search_runs TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.match_search_candidates TO dzoka_app;
