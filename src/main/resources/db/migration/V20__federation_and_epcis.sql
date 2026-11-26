CREATE TABLE dzoka.federation_peers (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), peer text NOT NULL,
 inbound boolean NOT NULL, outbound boolean NOT NULL, active boolean NOT NULL,
 min_version int NOT NULL CHECK(min_version BETWEEN 1 AND 100),
 max_version int NOT NULL CHECK(max_version BETWEEN min_version AND 100),
 failures int NOT NULL DEFAULT 0 CHECK(failures BETWEEN 0 AND 3),
 retry_at timestamptz,
 PRIMARY KEY(tenant_id,peer)
);
CREATE TABLE dzoka.federation_keys (
 tenant_id uuid NOT NULL, peer text NOT NULL, key_id text NOT NULL, public_key text NOT NULL,
 revoked_at timestamptz,
 PRIMARY KEY(tenant_id,peer,key_id),
 FOREIGN KEY(tenant_id,peer) REFERENCES dzoka.federation_peers(tenant_id,peer)
);
CREATE TABLE dzoka.federation_offers (
 tenant_id uuid NOT NULL, report_id uuid NOT NULL, revision int NOT NULL, offer_id uuid NOT NULL DEFAULT gen_random_uuid(),
 category text NOT NULL, country text NOT NULL, observed_month date NOT NULL, published_month date NOT NULL DEFAULT date_trunc('month',now() AT TIME ZONE 'UTC')::date,
 active boolean NOT NULL,
 PRIMARY KEY(tenant_id,report_id), UNIQUE(tenant_id,offer_id),
 FOREIGN KEY(tenant_id,report_id,revision) REFERENCES dzoka.report_revisions(tenant_id,report_id,revision) ON DELETE CASCADE
);
CREATE INDEX federation_offer_scope ON dzoka.federation_offers(tenant_id,category,country,observed_month,offer_id) WHERE active;
CREATE TABLE dzoka.federation_traffic (
 tenant_id uuid NOT NULL, peer text NOT NULL, request_id uuid NOT NULL, direction text NOT NULL CHECK(direction IN ('IN','OUT')),
 operation text NOT NULL, outcome text NOT NULL, recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,peer,request_id,direction),
 FOREIGN KEY(tenant_id,peer) REFERENCES dzoka.federation_peers(tenant_id,peer)
);
CREATE INDEX federation_traffic_budget ON dzoka.federation_traffic(tenant_id,peer,direction,recorded_at);

DO $$
DECLARE relation text;
BEGIN
 FOREACH relation IN ARRAY ARRAY['federation_peers','federation_keys','federation_offers','federation_traffic'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',relation);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',relation);
  EXECUTE format('CREATE POLICY tenant_boundary ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',relation);
  EXECUTE format('CREATE POLICY service_read ON dzoka.%I FOR SELECT TO dzoka_app USING(dzoka.permitted(''federation:service'') OR dzoka.permitted(''federation:admin''))',relation);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',relation);
 END LOOP;
END $$;
CREATE POLICY peer_admin ON dzoka.federation_peers FOR ALL TO dzoka_app USING(dzoka.permitted('federation:admin'));
CREATE POLICY peer_health ON dzoka.federation_peers FOR UPDATE TO dzoka_app USING(dzoka.permitted('federation:service'));
GRANT UPDATE(inbound,outbound,active,min_version,max_version,failures,retry_at) ON dzoka.federation_peers TO dzoka_app;
CREATE POLICY key_admin ON dzoka.federation_keys FOR ALL TO dzoka_app USING(dzoka.permitted('federation:admin'));
GRANT UPDATE(revoked_at) ON dzoka.federation_keys TO dzoka_app;
CREATE POLICY offer_admin ON dzoka.federation_offers FOR ALL TO dzoka_app USING(dzoka.permitted('federation:admin'));
GRANT UPDATE(revision,offer_id,category,country,observed_month,published_month,active) ON dzoka.federation_offers TO dzoka_app;
CREATE POLICY traffic_log ON dzoka.federation_traffic FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('federation:service'));
CREATE POLICY traffic_outcome ON dzoka.federation_traffic FOR UPDATE TO dzoka_app
 USING(dzoka.permitted('federation:service') AND direction='OUT');
GRANT UPDATE(outcome) ON dzoka.federation_traffic TO dzoka_app;
CREATE POLICY traffic_prune ON dzoka.federation_traffic FOR DELETE TO dzoka_app
 USING(dzoka.permitted('federation:service') AND recorded_at<clock_timestamp()-interval '30 days');
GRANT DELETE ON dzoka.federation_traffic TO dzoka_app;
CREATE POLICY federation_report_read ON dzoka.reports FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('federation:service')
 AND EXISTS(SELECT 1 FROM dzoka.federation_offers o WHERE o.tenant_id=reports.tenant_id AND o.report_id=reports.id AND o.active));

CREATE TABLE dzoka.traceability_objects (
 tenant_id uuid NOT NULL, intake_id uuid NOT NULL, object_id uuid NOT NULL DEFAULT gen_random_uuid(), enabled boolean NOT NULL,
 enabled_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,intake_id), UNIQUE(tenant_id,object_id),
 FOREIGN KEY(tenant_id,intake_id) REFERENCES dzoka.recovery_intakes(tenant_id,id)
);
CREATE TABLE dzoka.epcis_projections (
 tenant_id uuid NOT NULL, intake_id uuid NOT NULL, native_id uuid NOT NULL,
 native_kind text NOT NULL CHECK(native_kind IN ('STORED','TRANSFER_ACCEPTED','RETURNED')),
 mapping int NOT NULL CHECK(mapping IN (1,2)), id uuid NOT NULL DEFAULT gen_random_uuid(), predecessor uuid,
 document jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 state text NOT NULL DEFAULT 'QUEUED' CHECK(state IN ('QUEUED','SENDING','SUBMITTED','COMPLETE','REJECTED','UNKNOWN','CANCELLED')),
 ready_at timestamptz NOT NULL DEFAULT clock_timestamp(), lease uuid, lease_until timestamptz,
 attempts integer NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 3), receipt text, repository text, reconciliation_sha text,
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,intake_id,native_id,native_kind,mapping),
 UNIQUE(tenant_id,repository,receipt),
 FOREIGN KEY(tenant_id,intake_id) REFERENCES dzoka.traceability_objects(tenant_id,intake_id),
 FOREIGN KEY(tenant_id,predecessor) REFERENCES dzoka.epcis_projections(tenant_id,id)
);
CREATE INDEX epcis_dispatch ON dzoka.epcis_projections(tenant_id,ready_at,id) WHERE state IN ('QUEUED','SUBMITTED','SENDING');
DO $$
DECLARE relation text;
BEGIN
 FOREACH relation IN ARRAY ARRAY['traceability_objects','epcis_projections'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',relation);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',relation);
  EXECUTE format('CREATE POLICY trace_boundary ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',relation);
  EXECUTE format('CREATE POLICY trace_read ON dzoka.%I FOR SELECT TO dzoka_app USING(dzoka.permitted(''trace:manage'') OR dzoka.permitted(''trace:dispatch'') OR dzoka.permitted(''trace:read''))',relation);
  EXECUTE format('CREATE POLICY trace_capture ON dzoka.%I FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted(''trace:manage''))',relation);
  EXECUTE format('GRANT SELECT, INSERT ON dzoka.%I TO dzoka_app',relation);
 END LOOP;
END $$;
CREATE POLICY trace_opt_in ON dzoka.traceability_objects FOR UPDATE TO dzoka_app USING(dzoka.permitted('trace:manage'));
GRANT UPDATE(enabled,enabled_at) ON dzoka.traceability_objects TO dzoka_app;
CREATE POLICY trace_execute ON dzoka.epcis_projections FOR UPDATE TO dzoka_app USING(dzoka.permitted('trace:dispatch'));
CREATE POLICY trace_cancel ON dzoka.epcis_projections FOR UPDATE TO dzoka_app
 USING(dzoka.permitted('trace:manage') AND state='QUEUED') WITH CHECK(dzoka.permitted('trace:manage') AND state='CANCELLED');
GRANT UPDATE(state,ready_at,lease,lease_until,attempts,receipt,repository,reconciliation_sha) ON dzoka.epcis_projections TO dzoka_app;
