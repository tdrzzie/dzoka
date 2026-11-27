CREATE TABLE dzoka.places (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
 version bigint NOT NULL CHECK(version>0), parent_id uuid, active boolean NOT NULL,
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,parent_id) REFERENCES dzoka.places(tenant_id,id), CHECK(parent_id IS DISTINCT FROM id)
);
CREATE TABLE dzoka.place_revisions (
 tenant_id uuid NOT NULL, place_id uuid NOT NULL, version bigint NOT NULL CHECK(version>0),
 name text NOT NULL CHECK(length(name) BETWEEN 1 AND 160), normalized_name text NOT NULL, normalization_version text NOT NULL DEFAULT 'unicode-nfc-whitespace-rootcase-v1',kind text NOT NULL,
 parent_id uuid, parent_version bigint, active boolean NOT NULL,
 latitude double precision,longitude double precision,uncertainty_meters double precision,timezone text,
 actor_ref text NOT NULL CHECK(actor_ref ~ '^[0-9a-f]{64}$'), recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,place_id,version), FOREIGN KEY(tenant_id,place_id) REFERENCES dzoka.places(tenant_id,id),
 FOREIGN KEY(tenant_id,parent_id,parent_version) REFERENCES dzoka.place_revisions(tenant_id,place_id,version),
 CHECK((parent_id IS NULL)=(parent_version IS NULL)),
 CHECK((latitude IS NULL AND longitude IS NULL AND uncertainty_meters IS NULL) OR
 (latitude IS NOT NULL AND longitude IS NOT NULL AND uncertainty_meters IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND uncertainty_meters BETWEEN 0 AND 200000))
);
ALTER TABLE dzoka.places ADD FOREIGN KEY(tenant_id,id,version) REFERENCES dzoka.place_revisions(tenant_id,place_id,version) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE dzoka.place_aliases (
 tenant_id uuid NOT NULL,place_id uuid NOT NULL,version bigint NOT NULL,ordinal integer NOT NULL,
 raw_label text NOT NULL CHECK(length(raw_label) BETWEEN 1 AND 160), normalized_label text NOT NULL,
 PRIMARY KEY(tenant_id,place_id,version,ordinal),UNIQUE(tenant_id,place_id,version,normalized_label),
 FOREIGN KEY(tenant_id,place_id,version) REFERENCES dzoka.place_revisions(tenant_id,place_id,version)
);
CREATE INDEX place_alias_lookup ON dzoka.place_aliases(tenant_id,normalized_label,place_id,version);
CREATE INDEX place_name_lookup ON dzoka.place_revisions(tenant_id,normalized_name,place_id,version);
CREATE INDEX place_children ON dzoka.places(tenant_id,parent_id,id);
CREATE TABLE dzoka.place_commands (
 tenant_id uuid NOT NULL,actor_ref text NOT NULL,key_hash text NOT NULL,request_hash text NOT NULL,
 place_id uuid NOT NULL,version bigint NOT NULL,PRIMARY KEY(tenant_id,actor_ref,key_hash),
 FOREIGN KEY(tenant_id,place_id,version) REFERENCES dzoka.place_revisions(tenant_id,place_id,version)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['places','place_revisions','place_aliases'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY place_read ON dzoka.%I FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND (dzoka.permitted(''places:read'') OR dzoka.permitted(''places:manage'') OR dzoka.permitted(''reports:write'') OR dzoka.permitted(''reports:read'')))',t);
  EXECUTE format('CREATE POLICY place_append ON dzoka.%I FOR INSERT TO dzoka_app WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted(''places:manage''))',t);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',t);
 END LOOP;
END $$;
CREATE POLICY place_change ON dzoka.places FOR UPDATE TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('places:manage')) WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('places:manage'));
GRANT UPDATE(version,parent_id,active) ON dzoka.places TO dzoka_app;
ALTER TABLE dzoka.place_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.place_commands FORCE ROW LEVEL SECURITY;
CREATE POLICY place_command_scope ON dzoka.place_commands TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('places:manage')) WITH CHECK(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('places:manage'));
GRANT SELECT,INSERT ON dzoka.place_commands TO dzoka_app;

ALTER TABLE dzoka.report_revisions ADD COLUMN place_id uuid;
ALTER TABLE dzoka.report_revisions ADD COLUMN place_version bigint;
ALTER TABLE dzoka.report_revisions ADD CHECK((place_id IS NULL)=(place_version IS NULL));
ALTER TABLE dzoka.report_revisions ADD FOREIGN KEY(tenant_id,place_id,place_version) REFERENCES dzoka.place_revisions(tenant_id,place_id,version);
ALTER TABLE dzoka.outbox ADD COLUMN place_id uuid;
ALTER TABLE dzoka.outbox ADD FOREIGN KEY(tenant_id,place_id) REFERENCES dzoka.places(tenant_id,id);
DO $$ DECLARE constraint_name text; BEGIN
 FOR constraint_name IN SELECT conname FROM pg_constraint WHERE conrelid='dzoka.outbox'::regclass AND contype='c'
  AND pg_get_constraintdef(oid) LIKE '%organization_id%' AND pg_get_constraintdef(oid) LIKE '%media_id%' LOOP
  EXECUTE format('ALTER TABLE dzoka.outbox DROP CONSTRAINT %I',constraint_name);
 END LOOP;
END $$;
ALTER TABLE dzoka.outbox ADD CHECK(num_nonnulls(organization_id,report_id,media_id,place_id)=1);
ALTER TABLE dzoka.outbox ADD CHECK(aggregate_id=coalesce(organization_id,report_id,media_id,place_id));
CREATE OR REPLACE FUNCTION dzoka.outbox_default_reference() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF NEW.organization_id IS NULL AND NEW.report_id IS NULL AND NEW.media_id IS NULL AND NEW.place_id IS NULL THEN NEW.organization_id=NEW.aggregate_id; END IF;
 RETURN NEW;
END $$;
CREATE POLICY places_outbox_read ON dzoka.outbox FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND place_id IS NOT NULL AND dzoka.permitted('places:manage'));
CREATE POLICY places_outbox_append ON dzoka.outbox FOR INSERT TO dzoka_app WITH CHECK(tenant_id=dzoka.current_tenant() AND place_id=aggregate_id AND event_type IN('place.created','place.updated') AND dzoka.permitted('places:manage'));
CREATE POLICY places_audit_append ON dzoka.audit_events FOR INSERT TO dzoka_app WITH CHECK(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND action IN('place.created','place.updated') AND dzoka.permitted('places:manage'));

ALTER TABLE dzoka.media_assets ADD COLUMN cleanup_checked_at timestamptz;
ALTER TABLE dzoka.media_assets ADD COLUMN cleanup_token uuid;
ALTER TABLE dzoka.media_assets ADD COLUMN cleanup_until timestamptz;
GRANT UPDATE(cleanup_checked_at,cleanup_token,cleanup_until) ON dzoka.media_assets TO dzoka_app;
CREATE INDEX media_cleanup_sweep ON dzoka.media_assets(tenant_id,cleanup_checked_at,id) WHERE reservation_released;
