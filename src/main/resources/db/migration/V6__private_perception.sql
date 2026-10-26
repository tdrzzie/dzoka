CREATE TABLE dzoka.perception_profiles (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),id uuid NOT NULL,task text NOT NULL CHECK(task IN ('VISION','LOCAL_FEATURES','OCR','SPEECH')),
 profile_key text NOT NULL CHECK(profile_key~'^[0-9a-f]{64}$'),manifest jsonb NOT NULL CHECK(octet_length(manifest::text)<=32768),enabled boolean NOT NULL DEFAULT false,
 PRIMARY KEY(tenant_id,id),UNIQUE(tenant_id,profile_key)
);
CREATE TABLE dzoka.perception_jobs (
 tenant_id uuid NOT NULL,id uuid NOT NULL,report_id uuid NOT NULL,source_revision integer NOT NULL,profile_id uuid NOT NULL,media_id uuid,
 input_sha256 text NOT NULL CHECK(input_sha256~'^[0-9a-f]{64}$'),input_bytes integer NOT NULL CHECK(input_bytes BETWEEN 1 AND 20971520),
 input_type text NOT NULL CHECK(input_type IN ('IMAGE_PNG','AUDIO_WAV')),width integer,height integer,samples integer,
 state text NOT NULL CHECK(state IN ('QUEUED','PROCESSING','SUCCEEDED','FAILED','OBSOLETE','CANCELLED')),
 attempts integer NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 3),lease_token uuid,lease_owner text,lease_until timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),deadline timestamptz NOT NULL DEFAULT now()+interval '1 day',
 error_code text,completion_sha256 text,reservation_released boolean NOT NULL DEFAULT false,
 PRIMARY KEY(tenant_id,id),UNIQUE NULLS NOT DISTINCT(tenant_id,report_id,source_revision,profile_id,media_id,input_sha256),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
 FOREIGN KEY(tenant_id,profile_id) REFERENCES dzoka.perception_profiles(tenant_id,id),
 FOREIGN KEY(tenant_id,media_id) REFERENCES dzoka.media_assets(tenant_id,id),
 CHECK((input_type='IMAGE_PNG' AND media_id IS NOT NULL AND width>0 AND height>0 AND samples IS NULL)
    OR (input_type='AUDIO_WAV' AND media_id IS NULL AND width IS NULL AND height IS NULL AND samples BETWEEN 400 AND 160000))
);
CREATE INDEX perception_pending ON dzoka.perception_jobs(tenant_id,profile_id,state,created_at);
CREATE TABLE dzoka.perception_audio (
 tenant_id uuid NOT NULL,job_id uuid NOT NULL,raw_wav bytea NOT NULL CHECK(octet_length(raw_wav) BETWEEN 844 AND 320044),
 PRIMARY KEY(tenant_id,job_id),FOREIGN KEY(tenant_id,job_id) REFERENCES dzoka.perception_jobs(tenant_id,id) ON DELETE CASCADE
);
CREATE TABLE dzoka.perception_components (
 tenant_id uuid NOT NULL,id uuid NOT NULL,job_id uuid NOT NULL,report_id uuid NOT NULL,source_revision integer NOT NULL,
 profile_id uuid NOT NULL,input_sha256 text NOT NULL,evidence jsonb NOT NULL CHECK(octet_length(evidence::text)<=65536),
 generated_at timestamptz NOT NULL,received_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,id),UNIQUE(tenant_id,job_id),
 FOREIGN KEY(tenant_id,job_id) REFERENCES dzoka.perception_jobs(tenant_id,id),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
 FOREIGN KEY(tenant_id,profile_id) REFERENCES dzoka.perception_profiles(tenant_id,id)
);
CREATE TABLE dzoka.perception_audio_usage (
 tenant_id uuid PRIMARY KEY REFERENCES dzoka.tenants(id),raw_bytes bigint NOT NULL DEFAULT 0 CHECK(raw_bytes BETWEEN 0 AND 33554432)
);
DO $$ DECLARE name text; BEGIN
 FOREACH name IN ARRAY ARRAY['perception_profiles','perception_jobs','perception_audio','perception_components','perception_audio_usage'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',name);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',name);
  EXECUTE format('GRANT SELECT ON dzoka.%I TO dzoka_app',name);
 END LOOP;
END $$;
CREATE POLICY perception_audio_quota ON dzoka.perception_audio_usage TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:write') OR dzoka.permitted('reports:purge')))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:write') OR dzoka.permitted('reports:purge')));
GRANT INSERT ON dzoka.perception_audio_usage TO dzoka_app;
GRANT UPDATE(raw_bytes) ON dzoka.perception_audio_usage TO dzoka_app;
CREATE POLICY perception_profiles_read ON dzoka.perception_profiles FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('reports:read') OR dzoka.permitted('reports:write') OR dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge') OR dzoka.permitted('fingerprints:manage')));
GRANT UPDATE(enabled) ON dzoka.perception_profiles TO dzoka_app;
CREATE POLICY profile_lock ON dzoka.perception_profiles FOR UPDATE TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(false);
CREATE POLICY profile_immutable ON dzoka.perception_profiles AS RESTRICTIVE FOR UPDATE TO dzoka_app USING(true) WITH CHECK(false);
CREATE POLICY perception_jobs_read ON dzoka.perception_jobs FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.report_visible(report_id) OR dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge')));
CREATE POLICY perception_enqueue ON dzoka.perception_jobs FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:write') AND dzoka.report_visible(report_id));
CREATE POLICY perception_progress ON dzoka.perception_jobs FOR UPDATE TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND (dzoka.permitted('ai:work') OR dzoka.permitted('reports:purge'))) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY perception_audio_append ON dzoka.perception_audio FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:write') AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j WHERE j.tenant_id=perception_audio.tenant_id AND j.id=job_id AND dzoka.report_visible(j.report_id)));
CREATE POLICY perception_audio_lease ON dzoka.perception_audio FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work') AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
 WHERE j.tenant_id=perception_audio.tenant_id AND j.id=job_id AND j.state='PROCESSING' AND j.lease_owner=dzoka.current_actor_ref() AND j.lease_until>clock_timestamp()
 AND r.current_revision=j.source_revision AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()));
CREATE POLICY perception_read ON dzoka.perception_components FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND ((dzoka.permitted('reports:read') AND dzoka.report_visible(report_id)) OR dzoka.permitted('reports:purge')));
CREATE POLICY perception_audio_maintenance ON dzoka.perception_audio FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge') AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
 WHERE j.tenant_id=perception_audio.tenant_id AND j.id=job_id AND NOT r.legal_hold AND (r.deleted_at IS NOT NULL OR r.expires_at<=clock_timestamp())));
CREATE POLICY perception_append ON dzoka.perception_components FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work') AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j WHERE j.tenant_id=perception_components.tenant_id AND j.id=job_id
 AND j.state='PROCESSING' AND j.lease_owner=dzoka.current_actor_ref() AND j.lease_until>clock_timestamp()));
CREATE POLICY perception_erase ON dzoka.perception_components FOR DELETE TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge') AND EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=perception_components.tenant_id AND r.id=report_id
 AND NOT r.legal_hold AND (r.deleted_at IS NOT NULL OR r.expires_at<=clock_timestamp())));
CREATE POLICY perception_audio_erase ON dzoka.perception_audio FOR DELETE TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('reports:purge') AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
 WHERE j.tenant_id=perception_audio.tenant_id AND j.id=job_id AND NOT r.legal_hold AND (r.deleted_at IS NOT NULL OR r.expires_at<=clock_timestamp())));
CREATE POLICY perception_leased_media ON dzoka.media_assets FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work') AND EXISTS(SELECT 1 FROM dzoka.perception_jobs j WHERE j.tenant_id=media_assets.tenant_id AND j.media_id=media_assets.id
 AND j.state='PROCESSING' AND j.lease_owner=dzoka.current_actor_ref() AND j.lease_until>clock_timestamp()));
GRANT INSERT ON dzoka.perception_jobs,dzoka.perception_audio,dzoka.perception_components TO dzoka_app;
GRANT UPDATE(state,attempts,lease_token,lease_owner,lease_until,error_code,completion_sha256,reservation_released) ON dzoka.perception_jobs TO dzoka_app;
GRANT DELETE ON dzoka.perception_components,dzoka.perception_audio TO dzoka_app;
