-- A privileged, separate migration role owns schema/tables. Provision dzoka_app externally.
CREATE SCHEMA dzoka;
REVOKE ALL ON SCHEMA dzoka FROM PUBLIC;
GRANT USAGE ON SCHEMA dzoka TO dzoka_app;

CREATE FUNCTION dzoka.current_tenant() RETURNS uuid LANGUAGE sql STABLE
  SET search_path = pg_catalog
  AS $$ SELECT nullif(current_setting('dzoka.tenant_id', true), '')::uuid $$;
CREATE FUNCTION dzoka.current_issuer() RETURNS text LANGUAGE sql STABLE
  SET search_path = pg_catalog
  AS $$ SELECT nullif(current_setting('dzoka.issuer', true), '') $$;
CREATE FUNCTION dzoka.current_subject() RETURNS text LANGUAGE sql STABLE
  SET search_path = pg_catalog
  AS $$ SELECT nullif(current_setting('dzoka.subject', true), '') $$;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA dzoka FROM PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA dzoka TO dzoka_app;

CREATE TABLE dzoka.tenants (
  id uuid PRIMARY KEY,
  name text NOT NULL CHECK (length(name) BETWEEN 1 AND 160),
  active boolean NOT NULL DEFAULT true
);
CREATE TABLE dzoka.memberships (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),
  issuer text NOT NULL,
  subject text NOT NULL,
  permissions text[] NOT NULL DEFAULT '{}',
  active boolean NOT NULL DEFAULT true,
  PRIMARY KEY (tenant_id, issuer, subject)
);
CREATE TABLE dzoka.organizations (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),
  id uuid NOT NULL,
  name text NOT NULL CHECK (length(name) BETWEEN 1 AND 160),
  version bigint NOT NULL CHECK (version > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, id)
);
CREATE TABLE dzoka.idempotency (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),
  actor_ref text NOT NULL CHECK (length(actor_ref)=64),
  operation text NOT NULL,
  key_hash text NOT NULL CHECK (length(key_hash)=64),
  request_hash text NOT NULL CHECK (length(request_hash)=64),
  result_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, actor_ref, operation, key_hash),
  FOREIGN KEY (tenant_id, result_id) REFERENCES dzoka.organizations(tenant_id,id)
);
CREATE TABLE dzoka.audit_events (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),
  id uuid NOT NULL,
  actor_ref text NOT NULL CHECK (length(actor_ref)=64),
  action text NOT NULL,
  entity_id uuid NOT NULL,
  recorded_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,id)
);
CREATE TABLE dzoka.outbox (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),
  id uuid NOT NULL,
  event_type text NOT NULL,
  schema_version integer NOT NULL DEFAULT 1 CHECK (schema_version > 0),
  aggregate_id uuid NOT NULL,
  aggregate_version bigint NOT NULL CHECK (aggregate_version > 0),
  correlation_id uuid NOT NULL DEFAULT gen_random_uuid(),
  causation_id uuid,
  payload jsonb NOT NULL,
  occurred_at timestamptz NOT NULL DEFAULT now(),
  lease_token uuid,
  lease_until timestamptz,
  attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  published_at timestamptz,
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,event_type,aggregate_id,aggregate_version),
  FOREIGN KEY (tenant_id,aggregate_id) REFERENCES dzoka.organizations(tenant_id,id)
);
CREATE INDEX outbox_pending ON dzoka.outbox (tenant_id,occurred_at,id) WHERE published_at IS NULL;
CREATE TABLE dzoka.inbox (
  tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),
  consumer text NOT NULL,
  event_id uuid NOT NULL,
  payload_hash text NOT NULL CHECK (length(payload_hash)=64),
  processed_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,consumer,event_id)
);

-- Membership lookup is self-only, including for workers represented by local service grants.
ALTER TABLE dzoka.memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.memberships FORCE ROW LEVEL SECURITY;
CREATE POLICY own_membership ON dzoka.memberships FOR SELECT TO dzoka_app
  USING (tenant_id=dzoka.current_tenant() AND issuer=dzoka.current_issuer()
    AND subject=dzoka.current_subject());

CREATE FUNCTION dzoka.permitted(required_permission text) RETURNS boolean LANGUAGE sql STABLE
  SET search_path = pg_catalog
  AS $$ SELECT EXISTS (
    SELECT 1 FROM dzoka.memberships m JOIN dzoka.tenants t ON t.id=m.tenant_id
    WHERE m.tenant_id=dzoka.current_tenant() AND m.issuer=dzoka.current_issuer()
      AND m.subject=dzoka.current_subject() AND m.active AND t.active
      AND required_permission=ANY(m.permissions)) $$;
REVOKE ALL ON FUNCTION dzoka.permitted(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.permitted(text) TO dzoka_app;

ALTER TABLE dzoka.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.tenants FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_self ON dzoka.tenants FOR SELECT TO dzoka_app
  USING (id=dzoka.current_tenant());

ALTER TABLE dzoka.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.organizations FORCE ROW LEVEL SECURITY;
CREATE POLICY organizations_read ON dzoka.organizations FOR SELECT TO dzoka_app
  USING (tenant_id=dzoka.current_tenant()
    AND (dzoka.permitted('organizations:read') OR dzoka.permitted('organizations:write')));
CREATE POLICY organizations_insert ON dzoka.organizations FOR INSERT TO dzoka_app
  WITH CHECK (tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'));

ALTER TABLE dzoka.idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.idempotency FORCE ROW LEVEL SECURITY;
CREATE POLICY idempotency_scope ON dzoka.idempotency TO dzoka_app
  USING (tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'))
  WITH CHECK (tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'));
ALTER TABLE dzoka.audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_append ON dzoka.audit_events FOR INSERT TO dzoka_app
  WITH CHECK (tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'));

ALTER TABLE dzoka.outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY outbox_read ON dzoka.outbox FOR SELECT TO dzoka_app
  USING (tenant_id=dzoka.current_tenant()
    AND (dzoka.permitted('events:publish') OR dzoka.permitted('organizations:write')));
CREATE POLICY outbox_append ON dzoka.outbox FOR INSERT TO dzoka_app
  WITH CHECK (tenant_id=dzoka.current_tenant() AND dzoka.permitted('organizations:write'));
CREATE POLICY outbox_publish ON dzoka.outbox FOR UPDATE TO dzoka_app
  USING (tenant_id=dzoka.current_tenant() AND dzoka.permitted('events:publish'))
  WITH CHECK (tenant_id=dzoka.current_tenant() AND dzoka.permitted('events:publish'));

ALTER TABLE dzoka.inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY inbox_scope ON dzoka.inbox TO dzoka_app
  USING (tenant_id=dzoka.current_tenant() AND dzoka.permitted('events:consume'))
  WITH CHECK (tenant_id=dzoka.current_tenant() AND dzoka.permitted('events:consume'));

REVOKE ALL ON ALL TABLES IN SCHEMA dzoka FROM PUBLIC;
GRANT SELECT ON dzoka.tenants,dzoka.memberships,dzoka.organizations TO dzoka_app;
GRANT INSERT ON dzoka.organizations,dzoka.audit_events TO dzoka_app;
GRANT SELECT,INSERT,UPDATE ON dzoka.idempotency TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.inbox TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.outbox TO dzoka_app;
GRANT UPDATE (lease_token,lease_until,attempts,published_at) ON dzoka.outbox TO dzoka_app;
