CREATE TABLE dzoka.match_identifiers (
  tenant_id uuid NOT NULL, report_id uuid NOT NULL, revision integer NOT NULL,
  namespace text NOT NULL CHECK (namespace = 'manufacturer_serial'),
  key_version integer NOT NULL CHECK (key_version > 0),
  scope_digest text NOT NULL CHECK (scope_digest ~ '^[0-9a-f]{64}$'),
  digest text NOT NULL CHECK (digest ~ '^[0-9a-f]{64}$'),
  attestation_id uuid NOT NULL, actor_ref text NOT NULL CHECK (length(actor_ref)=64),
  recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY (tenant_id,report_id,revision,namespace),
  FOREIGN KEY (tenant_id,report_id,revision) REFERENCES dzoka.report_revisions(tenant_id,report_id,revision) ON DELETE CASCADE
);
ALTER TABLE dzoka.match_identifiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.match_identifiers FORCE ROW LEVEL SECURITY;
CREATE POLICY match_identifier_read ON dzoka.match_identifiers FOR SELECT TO dzoka_app
 USING (tenant_id=dzoka.current_tenant() AND dzoka.permitted('matches:read') AND EXISTS (
   SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=match_identifiers.tenant_id AND r.id=match_identifiers.report_id
     AND r.current_revision=match_identifiers.revision AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
     AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))));
CREATE POLICY match_identifier_append ON dzoka.match_identifiers FOR INSERT TO dzoka_app
 WITH CHECK (tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND
   dzoka.permitted('matches:attest-identifiers') AND EXISTS (
     SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=match_identifiers.tenant_id AND r.id=match_identifiers.report_id
       AND r.current_revision=match_identifiers.revision AND r.deleted_at IS NULL AND r.expires_at>statement_timestamp()
       AND (r.owner_ref=dzoka.current_actor_ref() OR dzoka.permitted('reports:read:any'))));
GRANT SELECT,INSERT ON dzoka.match_identifiers TO dzoka_app;
