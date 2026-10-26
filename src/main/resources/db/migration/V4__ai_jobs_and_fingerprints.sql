-- AI workers are HTTP clients; only the trusted core holds these runtime SQL grants.
CREATE TABLE dzoka.embedding_spaces (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
 provider text NOT NULL, model text NOT NULL, artifact_revision text NOT NULL,
 artifact_sha256 text NOT NULL CHECK(artifact_sha256~'^[0-9a-f]{64}$'),
 tokenizer_sha256 text NOT NULL CHECK(tokenizer_sha256~'^[0-9a-f]{64}$'),
 preprocessing text NOT NULL, dimensions integer NOT NULL CHECK(dimensions BETWEEN 1 AND 1024),
 metric text NOT NULL CHECK(metric='COSINE'), normalization text NOT NULL CHECK(normalization='L2'),
 config_sha256 text NOT NULL CHECK(config_sha256~'^[0-9a-f]{64}$'),
 space_key text NOT NULL CHECK(space_key~'^[0-9a-f]{64}$'), enabled boolean NOT NULL DEFAULT false,
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,space_key)
);
CREATE TABLE dzoka.ai_usage (
 tenant_id uuid PRIMARY KEY REFERENCES dzoka.tenants(id),pending integer NOT NULL DEFAULT 0 CHECK(pending BETWEEN 0 AND 32)
);
CREATE TABLE dzoka.ai_jobs (
 tenant_id uuid NOT NULL, id uuid NOT NULL, report_id uuid NOT NULL, item_id uuid NOT NULL,
 source_revision integer NOT NULL, input_sha256 text NOT NULL CHECK(input_sha256~'^[0-9a-f]{64}$'),
 space_id uuid NOT NULL, task text NOT NULL DEFAULT 'TEXT_EMBEDDING' CHECK(task='TEXT_EMBEDDING'),
 state text NOT NULL CHECK(state IN ('QUEUED','PROCESSING','SUCCEEDED','FAILED','UNAVAILABLE','OBSOLETE','CANCELLED')),
 attempts integer NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 3),
 lease_token uuid, lease_owner text, lease_until timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(), deadline timestamptz NOT NULL DEFAULT now()+interval '1 day',
 error_code text, completion_sha256 text,reservation_released boolean NOT NULL DEFAULT false,
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,report_id,source_revision,space_id),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
 FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.items(tenant_id,id),
 FOREIGN KEY(tenant_id,space_id) REFERENCES dzoka.embedding_spaces(tenant_id,id)
);
CREATE INDEX ai_pending ON dzoka.ai_jobs(tenant_id,space_id,state,created_at);
CREATE TABLE dzoka.fingerprint_components (
 tenant_id uuid NOT NULL, id uuid NOT NULL, job_id uuid NOT NULL, report_id uuid NOT NULL, item_id uuid NOT NULL,
 source_revision integer NOT NULL, input_sha256 text NOT NULL, space_id uuid NOT NULL,
 output_schema text NOT NULL CHECK(output_schema='text-embedding-v1'),
 generated_at timestamptz NOT NULL, received_at timestamptz NOT NULL DEFAULT now(),
 vector double precision[] NOT NULL CHECK(cardinality(vector) BETWEEN 1 AND 1024),
 token_count integer NOT NULL CHECK(token_count BETWEEN 1 AND 128), truncated boolean NOT NULL,
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,job_id),
 FOREIGN KEY(tenant_id,job_id) REFERENCES dzoka.ai_jobs(tenant_id,id),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
 FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.items(tenant_id,id),
 FOREIGN KEY(tenant_id,space_id) REFERENCES dzoka.embedding_spaces(tenant_id,id)
);
ALTER TABLE dzoka.embedding_spaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.embedding_spaces FORCE ROW LEVEL SECURITY;
CREATE POLICY spaces_read ON dzoka.embedding_spaces FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:read') OR dzoka.permitted('reports:write') OR dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge')));
ALTER TABLE dzoka.ai_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.ai_jobs FORCE ROW LEVEL SECURITY;
CREATE POLICY jobs_read ON dzoka.ai_jobs FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.report_visible(report_id));
CREATE POLICY jobs_create ON dzoka.ai_jobs FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:write') AND dzoka.report_visible(report_id));
CREATE POLICY jobs_update ON dzoka.ai_jobs FOR UPDATE TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.report_visible(report_id)
 AND (dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge')))
 WITH CHECK(tenant_id=dzoka.current_tenant());
ALTER TABLE dzoka.ai_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.ai_usage FORCE ROW LEVEL SECURITY;
CREATE POLICY ai_usage_scope ON dzoka.ai_usage TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:write') OR dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge')))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:write') OR dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge')));
ALTER TABLE dzoka.fingerprint_components ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.fingerprint_components FORCE ROW LEVEL SECURITY;
CREATE POLICY fingerprints_read ON dzoka.fingerprint_components FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.report_visible(report_id)
 AND (dzoka.permitted('reports:read') OR dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge')));
CREATE POLICY fingerprints_append ON dzoka.fingerprint_components FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work') AND EXISTS(
 SELECT 1 FROM dzoka.ai_jobs j WHERE j.tenant_id=fingerprint_components.tenant_id AND j.id=fingerprint_components.job_id
 AND j.state='PROCESSING' AND j.lease_owner=dzoka.current_actor_ref() AND j.lease_until>clock_timestamp()));
CREATE POLICY fingerprints_erase ON dzoka.fingerprint_components FOR DELETE TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge') AND EXISTS(
 SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=fingerprint_components.tenant_id AND r.id=fingerprint_components.report_id
 AND NOT r.legal_hold AND (r.deleted_at IS NOT NULL OR r.expires_at<=clock_timestamp())));
CREATE POLICY ai_report_metadata ON dzoka.reports FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work'));
CREATE POLICY ai_report_event_version ON dzoka.reports FOR UPDATE TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work')) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY ai_leased_input ON dzoka.report_revisions FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work') AND EXISTS(
 SELECT 1 FROM dzoka.ai_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
 WHERE j.tenant_id=report_revisions.tenant_id AND j.report_id=report_revisions.report_id AND j.source_revision=report_revisions.revision
 AND j.state='PROCESSING' AND j.lease_owner=dzoka.current_actor_ref() AND j.lease_until>clock_timestamp()
 AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() AND r.current_revision=j.source_revision));
CREATE POLICY ai_audit ON dzoka.audit_events FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('ai:work'));
CREATE POLICY ai_outbox ON dzoka.outbox FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work'));
GRANT SELECT ON dzoka.embedding_spaces TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.ai_jobs,dzoka.fingerprint_components TO dzoka_app;
GRANT UPDATE(state,attempts,lease_token,lease_owner,lease_until,error_code,completion_sha256,reservation_released) ON dzoka.ai_jobs TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.ai_usage TO dzoka_app;
GRANT UPDATE(pending) ON dzoka.ai_usage TO dzoka_app;
GRANT DELETE ON dzoka.fingerprint_components TO dzoka_app;
