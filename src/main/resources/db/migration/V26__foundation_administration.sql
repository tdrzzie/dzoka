ALTER TABLE dzoka.organizations ADD COLUMN active boolean NOT NULL DEFAULT true;
ALTER TABLE dzoka.tenants ADD COLUMN version bigint NOT NULL DEFAULT 1 CHECK(version>0);
ALTER TABLE dzoka.memberships ADD COLUMN version bigint NOT NULL DEFAULT 1 CHECK(version>0);
ALTER TABLE dzoka.idempotency ADD COLUMN result_name text;
ALTER TABLE dzoka.idempotency ADD COLUMN result_version bigint;
-- Owner maintenance is explicit; FORCE RLS remains enabled throughout the migration.
CREATE POLICY foundation_operator_idempotency ON dzoka.idempotency TO dzoka_migrator USING(true) WITH CHECK(true);
CREATE POLICY foundation_operator_organizations ON dzoka.organizations TO dzoka_migrator USING(true) WITH CHECK(true);
UPDATE dzoka.idempotency i SET result_name=o.name,result_version=o.version FROM dzoka.organizations o
 WHERE i.tenant_id=o.tenant_id AND i.result_id=o.id;
DROP POLICY foundation_operator_idempotency ON dzoka.idempotency;
DROP POLICY foundation_operator_organizations ON dzoka.organizations;
ALTER TABLE dzoka.idempotency ADD CHECK(result_id IS NULL OR (result_name IS NOT NULL AND result_version IS NOT NULL AND result_version>0));

GRANT UPDATE(name,version,active) ON dzoka.organizations TO dzoka_app;
CREATE POLICY organizations_update ON dzoka.organizations FOR UPDATE TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'));

CREATE TABLE dzoka.organization_commands (
 tenant_id uuid NOT NULL, actor_ref text NOT NULL CHECK(actor_ref ~ '^[0-9a-f]{64}$'),
 key_hash text NOT NULL CHECK(key_hash ~ '^[0-9a-f]{64}$'), request_hash text NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
 organization_id uuid NOT NULL, result_name text NOT NULL, result_version bigint NOT NULL CHECK(result_version>0), result_active boolean NOT NULL,
 recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,actor_ref,key_hash),
 FOREIGN KEY(tenant_id,organization_id) REFERENCES dzoka.organizations(tenant_id,id)
);
ALTER TABLE dzoka.organization_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.organization_commands FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_commands_scope ON dzoka.organization_commands TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('organizations:write'))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('organizations:write'));
GRANT SELECT,INSERT ON dzoka.organization_commands TO dzoka_app;

-- This operational journal is inaccessible to API/worker credentials.
CREATE TABLE dzoka.foundation_admin_operations (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
 key_hash text NOT NULL CHECK(key_hash ~ '^[0-9a-f]{64}$'), request_hash text NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
 db_actor text NOT NULL, target_ref text NOT NULL CHECK(target_ref ~ '^[0-9a-f]{64}$'), kind text NOT NULL CHECK(kind IN ('TENANT','MEMBERSHIP')),
 result_version bigint NOT NULL CHECK(result_version>0), result_active boolean NOT NULL,
 recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,key_hash)
);
ALTER TABLE dzoka.foundation_admin_operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.foundation_admin_operations FORCE ROW LEVEL SECURITY;
CREATE POLICY foundation_operator_receipts ON dzoka.foundation_admin_operations TO dzoka_migrator USING(true) WITH CHECK(true);
CREATE POLICY foundation_operator_tenants ON dzoka.tenants TO dzoka_migrator USING(true) WITH CHECK(true);
CREATE POLICY foundation_operator_memberships ON dzoka.memberships TO dzoka_migrator USING(true) WITH CHECK(true);
REVOKE ALL ON dzoka.foundation_admin_operations FROM PUBLIC,dzoka_app;
CREATE FUNCTION dzoka.reject_foundation_receipt_change() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN RAISE EXCEPTION 'Foundation receipts are append-only'; END $$;
REVOKE ALL ON FUNCTION dzoka.reject_foundation_receipt_change() FROM PUBLIC;
CREATE TRIGGER foundation_receipt_immutable BEFORE UPDATE OR DELETE ON dzoka.foundation_admin_operations
 FOR EACH ROW EXECUTE FUNCTION dzoka.reject_foundation_receipt_change();
