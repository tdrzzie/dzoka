CREATE TABLE dzoka.investigation_grants (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL, report_id uuid NOT NULL,
 camera uuid NOT NULL, investigator text NOT NULL, reviewer text NOT NULL,
 starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL, expires_at timestamptz NOT NULL,
 evidence_sha text NOT NULL CHECK(evidence_sha ~ '^[0-9a-f]{64}$'),
 budget int NOT NULL CHECK(budget BETWEEN 1 AND 100), revoked boolean NOT NULL DEFAULT false,
 CHECK(investigator<>reviewer), CHECK(starts_at<ends_at),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id)
);
CREATE TABLE dzoka.edge_jobs (
 tenant_id uuid NOT NULL, id uuid NOT NULL, grant_id uuid NOT NULL,
 actor_ref text NOT NULL, state text NOT NULL DEFAULT 'QUEUED' CHECK(state IN ('QUEUED','RUNNING','COMPLETE','CANCELLED')),
 lease uuid, worker text, lease_until timestamptz, attempts int NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 3),
 result jsonb, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,grant_id),
 FOREIGN KEY(tenant_id,grant_id) REFERENCES dzoka.investigation_grants(tenant_id,id)
);
DO $$
DECLARE name text;
BEGIN
 FOREACH name IN ARRAY ARRAY['investigation_grants','edge_jobs'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',name);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',name);
  EXECUTE format('CREATE POLICY tenant_scope ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',name);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',name);
 END LOOP;
END $$;
CREATE POLICY grant_read ON dzoka.investigation_grants FOR SELECT TO dzoka_app USING(
 dzoka.permitted('edge:review') OR dzoka.permitted('edge:process') OR investigator=dzoka.current_actor_ref());
CREATE POLICY grant_append ON dzoka.investigation_grants FOR INSERT TO dzoka_app WITH CHECK(
 dzoka.permitted('edge:review') AND reviewer=dzoka.current_actor_ref() AND investigator<>reviewer);
CREATE POLICY grant_revoke ON dzoka.investigation_grants FOR UPDATE TO dzoka_app USING(dzoka.permitted('edge:review'));
GRANT UPDATE(revoked) ON dzoka.investigation_grants TO dzoka_app;
CREATE POLICY job_read ON dzoka.edge_jobs FOR SELECT TO dzoka_app USING(
 dzoka.permitted('edge:process') OR (dzoka.permitted('edge:investigate') AND actor_ref=dzoka.current_actor_ref()));
CREATE POLICY job_submit ON dzoka.edge_jobs FOR INSERT TO dzoka_app WITH CHECK(
 dzoka.permitted('edge:investigate') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY job_work ON dzoka.edge_jobs FOR UPDATE TO dzoka_app USING(dzoka.permitted('edge:process'));
GRANT UPDATE(state,lease,worker,lease_until,attempts,result) ON dzoka.edge_jobs TO dzoka_app;
CREATE INDEX edge_queue ON dzoka.edge_jobs(tenant_id,created_at,id) WHERE state IN ('QUEUED','RUNNING');
