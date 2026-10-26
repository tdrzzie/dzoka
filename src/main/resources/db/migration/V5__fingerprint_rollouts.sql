CREATE TABLE dzoka.fingerprint_rollouts (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
 plan_sha256 text NOT NULL CHECK(plan_sha256~'^[0-9a-f]{64}$'),
 evaluation_sha256 text NOT NULL CHECK(evaluation_sha256~'^[0-9a-f]{64}$'),
 minimum_coverage_bps integer NOT NULL CHECK(minimum_coverage_bps BETWEEN 0 AND 10000),
 state text NOT NULL DEFAULT 'DRAFT' CHECK(state IN ('DRAFT','SEALED')),
 created_at timestamptz NOT NULL DEFAULT now(),sealed_at timestamptz,
 PRIMARY KEY(tenant_id,id)
);
CREATE TABLE dzoka.fingerprint_rollout_sources (
 tenant_id uuid NOT NULL,rollout_id uuid NOT NULL,report_id uuid NOT NULL,source_revision integer NOT NULL CHECK(source_revision>0),
 PRIMARY KEY(tenant_id,rollout_id,report_id),
 FOREIGN KEY(tenant_id,rollout_id) REFERENCES dzoka.fingerprint_rollouts(tenant_id,id),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id)
);
CREATE TABLE dzoka.fingerprint_rollout_spaces (
 tenant_id uuid NOT NULL,rollout_id uuid NOT NULL,space_id uuid NOT NULL,
 PRIMARY KEY(tenant_id,rollout_id,space_id),
 FOREIGN KEY(tenant_id,rollout_id) REFERENCES dzoka.fingerprint_rollouts(tenant_id,id),
 FOREIGN KEY(tenant_id,space_id) REFERENCES dzoka.embedding_spaces(tenant_id,id)
);
CREATE TABLE dzoka.fingerprint_snapshots (
 tenant_id uuid NOT NULL,rollout_id uuid NOT NULL,report_id uuid NOT NULL,
 manifest_sha256 text NOT NULL CHECK(manifest_sha256~'^[0-9a-f]{64}$'),created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,rollout_id,report_id),
 FOREIGN KEY(tenant_id,rollout_id,report_id) REFERENCES dzoka.fingerprint_rollout_sources(tenant_id,rollout_id,report_id)
);
ALTER TABLE dzoka.fingerprint_components ADD UNIQUE(tenant_id,id,report_id,space_id);
CREATE TABLE dzoka.fingerprint_snapshot_slots (
 tenant_id uuid NOT NULL,rollout_id uuid NOT NULL,report_id uuid NOT NULL,space_id uuid NOT NULL,
 component_id uuid,state text NOT NULL CHECK(state IN ('AVAILABLE','UNAVAILABLE')),
 CHECK((state='AVAILABLE')=(component_id IS NOT NULL)),
 PRIMARY KEY(tenant_id,rollout_id,report_id,space_id),
 FOREIGN KEY(tenant_id,rollout_id,report_id) REFERENCES dzoka.fingerprint_snapshots(tenant_id,rollout_id,report_id),
 FOREIGN KEY(tenant_id,rollout_id,space_id) REFERENCES dzoka.fingerprint_rollout_spaces(tenant_id,rollout_id,space_id),
 FOREIGN KEY(tenant_id,component_id,report_id,space_id) REFERENCES dzoka.fingerprint_components(tenant_id,id,report_id,space_id) ON DELETE CASCADE
);
CREATE TABLE dzoka.fingerprint_active (
 tenant_id uuid PRIMARY KEY REFERENCES dzoka.tenants(id),rollout_id uuid NOT NULL,version bigint NOT NULL CHECK(version>0),
 FOREIGN KEY(tenant_id,rollout_id) REFERENCES dzoka.fingerprint_rollouts(tenant_id,id)
);
CREATE TABLE dzoka.fingerprint_activations (
 tenant_id uuid NOT NULL,version bigint NOT NULL,rollout_id uuid NOT NULL,previous_rollout_id uuid,
 actor_ref text NOT NULL,action text NOT NULL CHECK(action IN ('PROMOTE','ROLLBACK')),recorded_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,version),
 FOREIGN KEY(tenant_id,rollout_id) REFERENCES dzoka.fingerprint_rollouts(tenant_id,id),
 FOREIGN KEY(tenant_id,previous_rollout_id) REFERENCES dzoka.fingerprint_rollouts(tenant_id,id)
);
DO $$ DECLARE name text; BEGIN
 FOREACH name IN ARRAY ARRAY['fingerprint_rollouts','fingerprint_rollout_sources','fingerprint_rollout_spaces',
  'fingerprint_snapshots','fingerprint_snapshot_slots','fingerprint_active','fingerprint_activations'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',name);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',name);
  EXECUTE format('CREATE POLICY rollout_read ON dzoka.%I FOR SELECT TO dzoka_app USING
    (tenant_id=dzoka.current_tenant() AND (dzoka.permitted(''fingerprints:manage'') OR dzoka.permitted(''reports:read'')))',name);
  EXECUTE format('CREATE POLICY rollout_append ON dzoka.%I FOR INSERT TO dzoka_app WITH CHECK
    (tenant_id=dzoka.current_tenant() AND dzoka.permitted(''fingerprints:manage''))',name);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',name);
 END LOOP;
END $$;
DROP POLICY rollout_read ON dzoka.fingerprint_snapshot_slots;
CREATE POLICY rollout_read ON dzoka.fingerprint_snapshot_slots FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('fingerprints:manage') OR
 (dzoka.permitted('reports:read') AND dzoka.report_visible(report_id))));
-- Ordinary readers only need pointer/slots used by the core query. Plans and
-- history remain administrator-only, including report references in cohorts.
DROP POLICY rollout_read ON dzoka.fingerprint_rollouts;
DROP POLICY rollout_read ON dzoka.fingerprint_rollout_sources;
DROP POLICY rollout_read ON dzoka.fingerprint_rollout_spaces;
DROP POLICY rollout_read ON dzoka.fingerprint_snapshots;
DROP POLICY rollout_read ON dzoka.fingerprint_activations;
DO $$ DECLARE name text; BEGIN
 FOREACH name IN ARRAY ARRAY['fingerprint_rollouts','fingerprint_rollout_sources','fingerprint_rollout_spaces','fingerprint_snapshots','fingerprint_activations'] LOOP
  EXECUTE format('CREATE POLICY rollout_read ON dzoka.%I FOR SELECT TO dzoka_app USING
    (tenant_id=dzoka.current_tenant() AND dzoka.permitted(''fingerprints:manage''))',name);
 END LOOP;
END $$;
CREATE POLICY rollout_state ON dzoka.fingerprint_rollouts FOR UPDATE TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage')) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY active_change ON dzoka.fingerprint_active FOR UPDATE TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage')) WITH CHECK(tenant_id=dzoka.current_tenant());
GRANT UPDATE(state,sealed_at) ON dzoka.fingerprint_rollouts TO dzoka_app;
GRANT UPDATE(rollout_id,version) ON dzoka.fingerprint_active TO dzoka_app;
CREATE POLICY rollout_report_metadata ON dzoka.reports FOR SELECT TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
CREATE POLICY rollout_report_version ON dzoka.reports FOR UPDATE TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage')) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY rollout_components ON dzoka.fingerprint_components FOR SELECT TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
CREATE POLICY rollout_jobs ON dzoka.ai_jobs FOR SELECT TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
CREATE POLICY rollout_spaces ON dzoka.embedding_spaces FOR SELECT TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
-- PostgreSQL row SHARE locks require an UPDATE privilege. Permit the lock on
-- enabled only, but reject every actual UPDATE with an unconditional check.
GRANT UPDATE(enabled) ON dzoka.embedding_spaces TO dzoka_app;
CREATE POLICY rollout_space_lock ON dzoka.embedding_spaces FOR UPDATE TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage')) WITH CHECK(false);
CREATE POLICY space_values_immutable ON dzoka.embedding_spaces AS RESTRICTIVE FOR UPDATE TO dzoka_app
 USING(true) WITH CHECK(false);
CREATE POLICY rollout_audit ON dzoka.audit_events FOR INSERT TO dzoka_app
 WITH CHECK(tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('fingerprints:manage'));
CREATE POLICY rollout_outbox ON dzoka.outbox FOR INSERT TO dzoka_app
 WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
