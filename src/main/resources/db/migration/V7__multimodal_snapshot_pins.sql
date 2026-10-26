ALTER TABLE dzoka.perception_components ADD UNIQUE(tenant_id,id,report_id);
CREATE TABLE dzoka.fingerprint_rollout_perception_pins (
 tenant_id uuid NOT NULL,rollout_id uuid NOT NULL,report_id uuid NOT NULL,component_id uuid NOT NULL,
 PRIMARY KEY(tenant_id,rollout_id,component_id),
 FOREIGN KEY(tenant_id,rollout_id,report_id) REFERENCES dzoka.fingerprint_rollout_sources(tenant_id,rollout_id,report_id)
 -- The declared UUID remains a minimal tombstone after governed component erasure.
);
CREATE TABLE dzoka.fingerprint_perception_slots (
 tenant_id uuid NOT NULL,rollout_id uuid NOT NULL,report_id uuid NOT NULL,component_id uuid NOT NULL,
 PRIMARY KEY(tenant_id,rollout_id,component_id),
 FOREIGN KEY(tenant_id,rollout_id,report_id) REFERENCES dzoka.fingerprint_snapshots(tenant_id,rollout_id,report_id),
 FOREIGN KEY(tenant_id,component_id,report_id) REFERENCES dzoka.perception_components(tenant_id,id,report_id) ON DELETE CASCADE
);
DO $$ DECLARE name text; BEGIN
 FOREACH name IN ARRAY ARRAY['fingerprint_rollout_perception_pins','fingerprint_perception_slots'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',name);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',name);
  EXECUTE format('CREATE POLICY multimodal_read ON dzoka.%I FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND
    (dzoka.permitted(''fingerprints:manage'') OR (dzoka.permitted(''reports:read'') AND dzoka.report_visible(report_id))))',name);
  EXECUTE format('CREATE POLICY multimodal_append ON dzoka.%I FOR INSERT TO dzoka_app WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted(''fingerprints:manage''))',name);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',name);
 END LOOP;
END $$;
CREATE POLICY perception_curator ON dzoka.perception_components FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
CREATE POLICY perception_curator_jobs ON dzoka.perception_jobs FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
CREATE POLICY perception_curator_media ON dzoka.media_assets FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('fingerprints:manage'));
