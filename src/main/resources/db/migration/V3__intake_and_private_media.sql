ALTER TABLE dzoka.tenants ADD COLUMN report_retention_days integer NOT NULL DEFAULT 30 CHECK(report_retention_days BETWEEN 1 AND 365);
ALTER TABLE dzoka.tenants ADD COLUMN media_byte_limit bigint NOT NULL DEFAULT 524288000 CHECK(media_byte_limit BETWEEN 26214400 AND 1099511627776);
CREATE TABLE dzoka.recovery_cases (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
  state text NOT NULL DEFAULT 'OPEN' CHECK (state IN ('OPEN','ERASED')),
  created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,id)
);
CREATE TABLE dzoka.items (
  tenant_id uuid NOT NULL, id uuid NOT NULL, case_id uuid NOT NULL,
  identity_state text NOT NULL DEFAULT 'UNRESOLVED' CHECK (identity_state='UNRESOLVED'),
  PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,case_id) REFERENCES dzoka.recovery_cases(tenant_id,id)
);
CREATE TABLE dzoka.reports (
  tenant_id uuid NOT NULL, id uuid NOT NULL, case_id uuid NOT NULL, item_id uuid NOT NULL,
  owner_ref text NOT NULL CHECK(length(owner_ref)=64), kind text NOT NULL CHECK(kind IN ('LOST','FOUND')),
  current_revision integer NOT NULL CHECK(current_revision>0), version bigint NOT NULL DEFAULT 1 CHECK(version>0), created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL, deleted_at timestamptz, erased_at timestamptz, legal_hold boolean NOT NULL DEFAULT false,
  PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,case_id) REFERENCES dzoka.recovery_cases(tenant_id,id),
  FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.items(tenant_id,id)
);
CREATE TABLE dzoka.report_revisions (
  tenant_id uuid NOT NULL, report_id uuid NOT NULL, revision integer NOT NULL CHECK(revision>0),
  raw_request bytea NOT NULL CHECK(octet_length(raw_request)<=16384), raw_sha256 text NOT NULL CHECK(length(raw_sha256)=64),
  raw_text text, normalized_text text, normalization_version text NOT NULL,
  language text NOT NULL, timezone text NOT NULL, raw_location text, raw_time text,
  latitude double precision, longitude double precision, uncertainty_meters double precision,
  occurred_after timestamptz, occurred_before timestamptz,
  recorded_at timestamptz NOT NULL DEFAULT now(), actor_ref text NOT NULL CHECK(length(actor_ref)=64),
  PRIMARY KEY(tenant_id,report_id,revision), FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
  CHECK ((latitude IS NULL AND longitude IS NULL AND uncertainty_meters IS NULL)
    OR (latitude IS NOT NULL AND longitude IS NOT NULL AND uncertainty_meters IS NOT NULL
      AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND uncertainty_meters BETWEEN 0 AND 200000)),
  CHECK (occurred_after IS NULL OR occurred_before IS NULL OR occurred_after<=occurred_before)
);
CREATE TABLE dzoka.intake_commands (
  tenant_id uuid NOT NULL, actor_ref text NOT NULL, operation text NOT NULL, key_hash text NOT NULL,
  request_hash text NOT NULL, report_id uuid, case_id uuid, item_id uuid, result_revision integer,
  PRIMARY KEY(tenant_id,actor_ref,operation,key_hash),
  FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id)
);
CREATE TABLE dzoka.media_usage (
  tenant_id uuid PRIMARY KEY REFERENCES dzoka.tenants(id), reserved_bytes bigint NOT NULL DEFAULT 0 CHECK(reserved_bytes>=0)
);
CREATE TABLE dzoka.media_assets (
  tenant_id uuid NOT NULL, id uuid NOT NULL, report_id uuid NOT NULL, owner_ref text NOT NULL,
  upload_key_hash text NOT NULL, source_sha256 text NOT NULL CHECK(length(source_sha256)=64),
  source_bytes bigint NOT NULL CHECK(source_bytes BETWEEN 1 AND 5242880), declared_type text NOT NULL,
  source_key text NOT NULL, derivative_key text, derivative_sha256 text,
  derivative_bytes bigint, width integer, height integer, sanitizer_version text,
  state text NOT NULL CHECK(state IN ('UPLOADING','QUARANTINED','PROCESSING','SANITIZED','REJECTED','FAILED','DELETED')),
  rejection_code text, lease_token uuid, lease_owner text, lease_until timestamptz, attempts integer NOT NULL DEFAULT 0,
  retry_budget_used integer NOT NULL DEFAULT 0 CHECK(retry_budget_used BETWEEN 0 AND 3),
  created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
  purge_pending boolean NOT NULL DEFAULT false, purge_version bigint NOT NULL DEFAULT 0, purge_started_at timestamptz,
  reservation_released boolean NOT NULL DEFAULT false,
  PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,owner_ref,report_id,upload_key_hash),
  FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
  CHECK(source_key=tenant_id::text||'/'||id::text||'/source'),
  CHECK(derivative_key LIKE tenant_id::text||'/'||id::text||'/%'),
  CHECK(state<>'SANITIZED' OR (derivative_sha256 IS NOT NULL AND derivative_bytes IS NOT NULL AND width IS NOT NULL AND height IS NOT NULL
    AND derivative_sha256~'^[0-9a-f]{64}$' AND derivative_bytes BETWEEN 1 AND 20971520
    AND width BETWEEN 1 AND 8192 AND height BETWEEN 1 AND 8192 AND width::bigint*height<=4000000 AND sanitizer_version IS NOT NULL))
);
CREATE INDEX media_work ON dzoka.media_assets(tenant_id,state,created_at);

CREATE FUNCTION dzoka.report_visible(target uuid) RETURNS boolean LANGUAGE sql STABLE SET search_path=pg_catalog
AS $$ SELECT EXISTS(SELECT 1 FROM dzoka.reports WHERE tenant_id=dzoka.current_tenant() AND id=target) $$;
REVOKE ALL ON FUNCTION dzoka.report_visible(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.report_visible(uuid) TO dzoka_app;

ALTER TABLE dzoka.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.reports FORCE ROW LEVEL SECURITY;
CREATE POLICY reports_read ON dzoka.reports FOR SELECT TO dzoka_app USING (
  tenant_id=dzoka.current_tenant() AND (
    (owner_ref=dzoka.current_actor_ref() AND (dzoka.permitted('reports:read') OR dzoka.permitted('reports:write')))
    OR dzoka.permitted('reports:read:any') OR dzoka.permitted('reports:hold')
    OR dzoka.permitted('media:process') OR dzoka.permitted('reports:purge') OR dzoka.permitted('media:purge')));
CREATE POLICY reports_create ON dzoka.reports FOR INSERT TO dzoka_app WITH CHECK (
  tenant_id=dzoka.current_tenant() AND owner_ref=dzoka.current_actor_ref() AND dzoka.permitted('reports:write'));
CREATE POLICY reports_change ON dzoka.reports FOR UPDATE TO dzoka_app USING (
  tenant_id=dzoka.current_tenant() AND ((owner_ref=dzoka.current_actor_ref() AND dzoka.permitted('reports:write'))
    OR dzoka.permitted('reports:write:any') OR dzoka.permitted('reports:hold') OR dzoka.permitted('reports:purge')))
  WITH CHECK(tenant_id=dzoka.current_tenant());

ALTER TABLE dzoka.recovery_cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.recovery_cases FORCE ROW LEVEL SECURITY;
CREATE POLICY cases_read ON dzoka.recovery_cases FOR SELECT TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=recovery_cases.tenant_id AND r.case_id=recovery_cases.id));
CREATE POLICY cases_create ON dzoka.recovery_cases FOR INSERT TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:write'));
CREATE POLICY cases_purge ON dzoka.recovery_cases FOR UPDATE TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge')) WITH CHECK(tenant_id=dzoka.current_tenant());
ALTER TABLE dzoka.items ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.items FORCE ROW LEVEL SECURITY;
CREATE POLICY items_read ON dzoka.items FOR SELECT TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=items.tenant_id AND r.item_id=items.id));
CREATE POLICY items_create ON dzoka.items FOR INSERT TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:write'));

ALTER TABLE dzoka.report_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.report_revisions FORCE ROW LEVEL SECURITY;
CREATE POLICY revisions_read ON dzoka.report_revisions FOR SELECT TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND dzoka.report_visible(report_id)
 AND (dzoka.permitted('reports:purge') OR EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=report_revisions.tenant_id AND r.id=report_revisions.report_id AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()))
 AND (dzoka.permitted('reports:read') OR dzoka.permitted('reports:read:any') OR dzoka.permitted('reports:write') OR dzoka.permitted('reports:purge')));
CREATE POLICY revisions_append ON dzoka.report_revisions FOR INSERT TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('reports:write')
 AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=report_revisions.tenant_id AND r.id=report_revisions.report_id
   AND r.deleted_at IS NULL AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:write:any'))));
CREATE POLICY revisions_erase ON dzoka.report_revisions FOR DELETE TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge')
 AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=report_revisions.tenant_id AND r.id=report_revisions.report_id AND r.deleted_at IS NOT NULL AND NOT r.legal_hold));

ALTER TABLE dzoka.intake_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.intake_commands FORCE ROW LEVEL SECURITY;
CREATE POLICY intake_commands_scope ON dzoka.intake_commands TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('reports:write'))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('reports:write'));
CREATE POLICY intake_commands_erasure ON dzoka.intake_commands TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge')
 AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=intake_commands.tenant_id AND r.id=intake_commands.report_id AND r.deleted_at IS NOT NULL AND NOT r.legal_hold))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge'));
ALTER TABLE dzoka.media_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.media_usage FORCE ROW LEVEL SECURITY;
CREATE POLICY usage_scope ON dzoka.media_usage TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:write') OR dzoka.permitted('media:purge')))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:write') OR dzoka.permitted('media:purge')));
ALTER TABLE dzoka.media_assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.media_assets FORCE ROW LEVEL SECURITY;
CREATE POLICY media_read ON dzoka.media_assets FOR SELECT TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND dzoka.report_visible(report_id));
CREATE POLICY media_create ON dzoka.media_assets FOR INSERT TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND owner_ref=dzoka.current_actor_ref() AND dzoka.permitted('reports:write')
 AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=media_assets.tenant_id AND r.id=media_assets.report_id AND r.deleted_at IS NULL
   AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:write:any'))));
CREATE POLICY media_change ON dzoka.media_assets FOR UPDATE TO dzoka_app USING (
 tenant_id=dzoka.current_tenant() AND dzoka.report_visible(report_id) AND (
  dzoka.permitted('reports:write') OR dzoka.permitted('media:process') OR dzoka.permitted('media:purge') OR dzoka.permitted('reports:purge')))
 WITH CHECK(tenant_id=dzoka.current_tenant());

GRANT SELECT,INSERT ON dzoka.recovery_cases,dzoka.items,dzoka.reports,dzoka.report_revisions TO dzoka_app;
GRANT UPDATE(state) ON dzoka.recovery_cases TO dzoka_app;
GRANT UPDATE(current_revision,version,deleted_at,erased_at,legal_hold) ON dzoka.reports TO dzoka_app;
GRANT DELETE ON dzoka.report_revisions TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.intake_commands,dzoka.media_assets,dzoka.media_usage TO dzoka_app;
GRANT UPDATE(report_id,case_id,item_id,result_revision,request_hash) ON dzoka.intake_commands TO dzoka_app;
GRANT UPDATE(reserved_bytes) ON dzoka.media_usage TO dzoka_app;
GRANT UPDATE(state,derivative_sha256,derivative_bytes,width,height,sanitizer_version,rejection_code,
 lease_token,lease_owner,lease_until,attempts,retry_budget_used,purge_pending,purge_version,purge_started_at,reservation_released)
 ON dzoka.media_assets TO dzoka_app;

-- Preserve tenant-qualified foreign keys while extending the foundation's organization-only outbox.
ALTER TABLE dzoka.outbox DROP CONSTRAINT outbox_tenant_id_aggregate_id_fkey;
ALTER TABLE dzoka.outbox ADD COLUMN organization_id uuid;
ALTER TABLE dzoka.outbox ADD COLUMN report_id uuid;
ALTER TABLE dzoka.outbox ADD COLUMN media_id uuid;
UPDATE dzoka.outbox SET organization_id=aggregate_id;
ALTER TABLE dzoka.outbox ADD FOREIGN KEY(tenant_id,organization_id) REFERENCES dzoka.organizations(tenant_id,id);
ALTER TABLE dzoka.outbox ADD FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id);
ALTER TABLE dzoka.outbox ADD FOREIGN KEY(tenant_id,media_id) REFERENCES dzoka.media_assets(tenant_id,id);
ALTER TABLE dzoka.outbox ADD CHECK (num_nonnulls(organization_id,report_id,media_id)=1);
ALTER TABLE dzoka.outbox ADD CHECK (aggregate_id=coalesce(organization_id,report_id,media_id));
CREATE FUNCTION dzoka.outbox_default_reference() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF NEW.organization_id IS NULL AND NEW.report_id IS NULL AND NEW.media_id IS NULL THEN
   NEW.organization_id=NEW.aggregate_id;
 END IF;
 RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION dzoka.outbox_default_reference() FROM PUBLIC;
CREATE TRIGGER outbox_reference BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.outbox_default_reference();
ALTER POLICY outbox_read ON dzoka.outbox TO dzoka_app USING (tenant_id=dzoka.current_tenant() AND (
 dzoka.permitted('events:publish') OR dzoka.permitted('organizations:write') OR dzoka.permitted('reports:write')
 OR dzoka.permitted('media:process') OR dzoka.permitted('reports:purge') OR dzoka.permitted('media:purge') OR dzoka.permitted('reports:hold')));
ALTER POLICY outbox_append ON dzoka.outbox TO dzoka_app WITH CHECK (tenant_id=dzoka.current_tenant() AND (
 dzoka.permitted('organizations:write') OR dzoka.permitted('reports:write') OR dzoka.permitted('media:process')
 OR dzoka.permitted('reports:purge') OR dzoka.permitted('media:purge') OR dzoka.permitted('reports:hold')));
ALTER POLICY audit_append ON dzoka.audit_events TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND (
 dzoka.permitted('organizations:write') OR dzoka.permitted('reports:write') OR dzoka.permitted('media:process')
 OR dzoka.permitted('reports:purge') OR dzoka.permitted('media:purge') OR dzoka.permitted('reports:hold')));
