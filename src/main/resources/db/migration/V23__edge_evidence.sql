CREATE TABLE dzoka.edge_evidence (
 tenant_id uuid NOT NULL, job_id uuid NOT NULL, id uuid NOT NULL, width int NOT NULL CHECK(width BETWEEN 1 AND 64),
 height int NOT NULL CHECK(height BETWEEN 1 AND 64), pixels bytea NOT NULL,
 sha text NOT NULL CHECK(sha ~ '^[0-9a-f]{64}$'), approved boolean NOT NULL DEFAULT false,
 reviewer text, approval_sha text, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,job_id,id), FOREIGN KEY(tenant_id,job_id) REFERENCES dzoka.edge_jobs(tenant_id,id),
 CHECK(octet_length(pixels)=width*height*3)
);
ALTER TABLE dzoka.edge_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.edge_evidence FORCE ROW LEVEL SECURITY;
CREATE POLICY evidence_tenant ON dzoka.edge_evidence AS RESTRICTIVE FOR ALL TO dzoka_app
 USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY evidence_read ON dzoka.edge_evidence FOR SELECT TO dzoka_app USING(
 dzoka.permitted('edge:process') OR dzoka.permitted('edge:review') OR
 (approved AND dzoka.permitted('edge:investigate') AND EXISTS(
   SELECT 1 FROM dzoka.edge_jobs j WHERE j.tenant_id=edge_evidence.tenant_id AND j.id=edge_evidence.job_id AND j.actor_ref=dzoka.current_actor_ref())));
CREATE POLICY evidence_insert ON dzoka.edge_evidence FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('edge:process') AND NOT approved);
CREATE POLICY evidence_review ON dzoka.edge_evidence FOR UPDATE TO dzoka_app USING(dzoka.permitted('edge:review'));
CREATE POLICY evidence_expire ON dzoka.edge_evidence FOR DELETE TO dzoka_app USING(dzoka.permitted('edge:process') AND EXISTS(
 SELECT 1 FROM dzoka.edge_jobs j JOIN dzoka.investigation_grants g ON g.tenant_id=j.tenant_id AND g.id=j.grant_id
 WHERE j.tenant_id=edge_evidence.tenant_id AND j.id=edge_evidence.job_id AND (g.revoked OR g.expires_at<=clock_timestamp())));
GRANT SELECT,INSERT,DELETE,UPDATE(approved,reviewer,approval_sha) ON dzoka.edge_evidence TO dzoka_app;
CREATE POLICY job_review ON dzoka.edge_jobs FOR SELECT TO dzoka_app USING(dzoka.permitted('edge:review'));
