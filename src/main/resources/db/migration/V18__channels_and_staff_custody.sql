CREATE TABLE dzoka.staff_custody_events (
 tenant_id uuid NOT NULL, intake_id uuid NOT NULL, id uuid NOT NULL,
 actor_ref text NOT NULL, recipient_ref text NOT NULL, source_site uuid NOT NULL, destination uuid NOT NULL,
 expected_generation int NOT NULL CHECK(expected_generation>0),
 observed_at timestamptz NOT NULL, recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 kind text NOT NULL CHECK(kind IN ('STORED','INCIDENT','TRANSFER_REQUESTED','TRANSFER_ACCEPTED','OFFLINE_CONFLICT')),
 command text NOT NULL CHECK(command IN ('STORE','INCIDENT','REQUEST_TRANSFER','ACCEPT_TRANSFER')),
 request_id uuid, evidence_sha text NOT NULL CHECK(evidence_sha ~ '^[0-9a-f]{64}$'),
 PRIMARY KEY(tenant_id,intake_id,id),
 FOREIGN KEY(tenant_id,intake_id) REFERENCES dzoka.recovery_intakes(tenant_id,id),
 FOREIGN KEY(tenant_id,intake_id,request_id) REFERENCES dzoka.staff_custody_events(tenant_id,intake_id,id)
);
CREATE UNIQUE INDEX staff_custody_one_acceptance ON dzoka.staff_custody_events(tenant_id,intake_id,request_id)
 WHERE kind='TRANSFER_ACCEPTED';
CREATE POLICY staff_custody_intake ON dzoka.recovery_intakes FOR SELECT TO dzoka_app
 USING (tenant_id=dzoka.current_tenant() AND dzoka.permitted('custody:operate'));
CREATE POLICY staff_custody_update ON dzoka.recovery_intakes FOR UPDATE TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('custody:operate'))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('custody:operate'));
GRANT UPDATE(site_id,custodian_ref) ON dzoka.recovery_intakes TO dzoka_app;
CREATE FUNCTION dzoka.guard_custody_columns() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF current_user='dzoka_app' AND (
    ((NEW.site_id,NEW.custodian_ref) IS DISTINCT FROM (OLD.site_id,OLD.custodian_ref) AND NOT dzoka.permitted('custody:operate'))
    OR (NEW.returned IS DISTINCT FROM OLD.returned AND NOT dzoka.permitted('recovery:handover'))
    OR (NEW.blocked IS DISTINCT FROM OLD.blocked AND NOT (dzoka.permitted('recovery:manage')
        OR (NEW.blocked AND dzoka.permitted('custody:operate'))))) THEN
   RAISE EXCEPTION 'Disallowed custody mutation' USING ERRCODE='42501';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_custody_columns BEFORE UPDATE ON dzoka.recovery_intakes
 FOR EACH ROW EXECUTE FUNCTION dzoka.guard_custody_columns();

CREATE TABLE dzoka.channel_contacts (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
 member_ref text NOT NULL, address_key text NOT NULL, route text NOT NULL,
 enabled boolean NOT NULL DEFAULT false, consented_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,member_ref), UNIQUE(tenant_id,address_key)
);
CREATE TABLE dzoka.channel_events (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), event_key text NOT NULL,
 contact_id uuid, operation_id uuid, report_id uuid, report_revision int, kind text NOT NULL,
 input_key text, processed boolean NOT NULL DEFAULT true,
 received_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,event_key),
 FOREIGN KEY(tenant_id,contact_id) REFERENCES dzoka.channel_contacts(tenant_id,id),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id),
 CHECK ((report_id IS NULL AND report_revision IS NULL) OR
        (report_id IS NOT NULL AND report_revision IS NOT NULL AND report_revision>0))
);
CREATE INDEX channel_inbound_quota ON dzoka.channel_events(tenant_id,contact_id,received_at);
CREATE TABLE dzoka.channel_operations (
 tenant_id uuid NOT NULL, id uuid NOT NULL, contact_id uuid NOT NULL, relay_thread uuid,
 template text NOT NULL CHECK(template IN ('HELP','REPORT_SAVED','RELAY_UPDATE','RECOVERY_UPDATE','UNSUPPORTED')),
 state text NOT NULL DEFAULT 'QUEUED' CHECK(state IN ('QUEUED','SENDING','UNKNOWN','SENT','FAILED','DELIVERED','READ','CANCELLED')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), sending_at timestamptz,
 ready_at timestamptz NOT NULL DEFAULT clock_timestamp(), attempts integer NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 3),
 provider_key text, PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,provider_key),
 FOREIGN KEY(tenant_id,contact_id) REFERENCES dzoka.channel_contacts(tenant_id,id)
);
CREATE INDEX channel_dispatch_queue ON dzoka.channel_operations(tenant_id,created_at,id)
 WHERE state='QUEUED';
CREATE TABLE dzoka.relay_threads (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL, intake_id uuid NOT NULL, claim_id uuid NOT NULL,
 claimant_ref text NOT NULL, finder_ref text NOT NULL,
 blocked boolean NOT NULL DEFAULT false, flagged boolean NOT NULL DEFAULT false,
 expires_at timestamptz NOT NULL DEFAULT (clock_timestamp()+interval '7 days'),
 CHECK (claimant_ref<>finder_ref), PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,intake_id,claimant_ref,finder_ref),
 FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id)
);
CREATE TABLE dzoka.relay_entries (
 tenant_id uuid NOT NULL, thread_id uuid NOT NULL, id uuid NOT NULL, sender_ref text NOT NULL,
 phrase text NOT NULL CHECK(phrase IN ('REQUEST_UPDATE','READY_TO_ARRANGE_PICKUP','THANK_YOU')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,thread_id,id),
 FOREIGN KEY(tenant_id,thread_id) REFERENCES dzoka.relay_threads(tenant_id,id) ON DELETE CASCADE
);
CREATE INDEX relay_entry_window ON dzoka.relay_entries(tenant_id,thread_id,created_at);
CREATE TABLE dzoka.channel_alerts (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL DEFAULT gen_random_uuid(),
 member_ref text NOT NULL, relay_thread uuid, considered boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id)
);
CREATE INDEX channel_pending_alert ON dzoka.channel_alerts(tenant_id,created_at,id) WHERE NOT considered;

DO $$
DECLARE name text;
BEGIN
 FOREACH name IN ARRAY ARRAY['staff_custody_events','channel_contacts','channel_events','channel_operations','relay_threads','relay_entries','channel_alerts'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',name);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',name);
  EXECUTE format('CREATE POLICY tenant_boundary ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',name);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',name);
 END LOOP;
END $$;
CREATE POLICY custody_events_read ON dzoka.staff_custody_events FOR SELECT TO dzoka_app USING(dzoka.permitted('custody:operate'));
CREATE POLICY custody_events_insert ON dzoka.staff_custody_events FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('custody:operate') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY custody_audit ON dzoka.audit_events FOR INSERT TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
 AND dzoka.permitted('custody:operate') AND action IN ('custody.stored','custody.transferred','custody.incident'));
CREATE POLICY custody_outbox ON dzoka.outbox FOR INSERT TO dzoka_app WITH CHECK (
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('custody:operate')
 AND event_type IN ('custody.stored','custody.transferred','custody.incident'));
CREATE POLICY contacts_read ON dzoka.channel_contacts FOR SELECT TO dzoka_app USING(
 dzoka.permitted('channels:admin') OR dzoka.permitted('channels:service')
 OR (dzoka.permitted('channels:use') AND member_ref=dzoka.current_actor_ref()));
CREATE POLICY contacts_insert ON dzoka.channel_contacts FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('channels:admin'));
CREATE POLICY contacts_change ON dzoka.channel_contacts FOR UPDATE TO dzoka_app USING(
 dzoka.permitted('channels:admin') OR dzoka.permitted('channels:service')
 OR (dzoka.permitted('channels:use') AND member_ref=dzoka.current_actor_ref()));
GRANT UPDATE(enabled) ON dzoka.channel_contacts TO dzoka_app;
CREATE POLICY inbox_read ON dzoka.channel_events FOR SELECT TO dzoka_app USING(dzoka.permitted('channels:service'));
CREATE POLICY inbox_insert ON dzoka.channel_events FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('channels:service'));
CREATE POLICY inbox_change ON dzoka.channel_events FOR UPDATE TO dzoka_app USING(dzoka.permitted('channels:service'));
CREATE POLICY inbox_expiry ON dzoka.channel_events FOR DELETE TO dzoka_app USING(
 dzoka.permitted('channels:service') AND received_at<clock_timestamp()-interval '8 days');
GRANT UPDATE(processed,report_id,report_revision) ON dzoka.channel_events TO dzoka_app;
GRANT DELETE ON dzoka.channel_events TO dzoka_app;
CREATE POLICY operation_read ON dzoka.channel_operations FOR SELECT TO dzoka_app USING(
 dzoka.permitted('channels:service') OR dzoka.permitted('channels:admin') OR
 (dzoka.permitted('channels:use') AND EXISTS(SELECT 1 FROM dzoka.channel_contacts c
  WHERE c.tenant_id=channel_operations.tenant_id AND c.id=channel_operations.contact_id AND c.member_ref=dzoka.current_actor_ref())));
CREATE POLICY operation_insert ON dzoka.channel_operations FOR INSERT TO dzoka_app WITH CHECK(
 dzoka.permitted('channels:service') OR dzoka.permitted('channels:admin'));
CREATE POLICY operation_change ON dzoka.channel_operations FOR UPDATE TO dzoka_app USING(dzoka.permitted('channels:service'));
GRANT UPDATE(state,sending_at,provider_key,ready_at,attempts,relay_thread) ON dzoka.channel_operations TO dzoka_app;
CREATE POLICY relay_read ON dzoka.relay_threads FOR SELECT TO dzoka_app USING(dzoka.permitted('channels:moderate')
 OR dzoka.permitted('channels:service') OR (dzoka.permitted('channels:use') AND dzoka.current_actor_ref() IN (claimant_ref,finder_ref)));
CREATE POLICY relay_insert ON dzoka.relay_threads FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('channels:moderate'));
CREATE POLICY relay_change ON dzoka.relay_threads FOR UPDATE TO dzoka_app USING(dzoka.permitted('channels:moderate')
 OR (dzoka.permitted('channels:use') AND dzoka.current_actor_ref() IN (claimant_ref,finder_ref)));
GRANT UPDATE(blocked,flagged) ON dzoka.relay_threads TO dzoka_app;
CREATE POLICY relay_expiry ON dzoka.relay_threads FOR DELETE TO dzoka_app USING(
 dzoka.permitted('channels:moderate')
 AND expires_at+CASE WHEN flagged THEN interval '30 days' ELSE interval '0 days' END<clock_timestamp());
GRANT DELETE ON dzoka.relay_threads TO dzoka_app;
CREATE INDEX relay_expiry_order ON dzoka.relay_threads(tenant_id,expires_at);

CREATE POLICY relay_metadata ON dzoka.relay_threads FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY relay_claim_metadata ON dzoka.ownership_claims FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE FUNCTION dzoka.relay_available(thread uuid) RETURNS boolean
LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM dzoka.relay_threads t
  JOIN dzoka.ownership_claims c ON (c.tenant_id,c.id,c.intake_id)=(t.tenant_id,t.claim_id,t.intake_id)
  JOIN dzoka.recovery_intakes i ON (i.tenant_id,i.id)=(t.tenant_id,t.intake_id)
  JOIN dzoka.reports r ON (r.tenant_id,r.id)=(i.tenant_id,i.report_id)
  WHERE t.tenant_id=dzoka.current_tenant() AND t.id=thread AND NOT t.blocked AND t.expires_at>clock_timestamp()
    AND c.state IN ('PENDING','APPROVED') AND NOT i.blocked AND NOT i.returned
    AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp() AND NOT r.legal_hold)
$$;
REVOKE ALL ON FUNCTION dzoka.relay_available(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.relay_available(uuid) TO dzoka_app;
CREATE POLICY relay_entry_read ON dzoka.relay_entries FOR SELECT TO dzoka_app USING(EXISTS(
 SELECT 1 FROM dzoka.relay_threads t WHERE t.tenant_id=relay_entries.tenant_id AND t.id=relay_entries.thread_id
 AND (dzoka.permitted('channels:moderate') OR dzoka.relay_available(t.id))));
CREATE POLICY relay_entry_insert ON dzoka.relay_entries FOR INSERT TO dzoka_app WITH CHECK(
 (dzoka.permitted('channels:service') OR (dzoka.permitted('channels:use') AND sender_ref=dzoka.current_actor_ref()))
 AND EXISTS(SELECT 1 FROM dzoka.relay_threads t WHERE t.tenant_id=relay_entries.tenant_id AND t.id=relay_entries.thread_id
 AND sender_ref IN (t.claimant_ref,t.finder_ref) AND dzoka.relay_available(t.id)));

CREATE POLICY alerts_read ON dzoka.channel_alerts FOR SELECT TO dzoka_app USING(dzoka.permitted('channels:service'));
CREATE POLICY alerts_update ON dzoka.channel_alerts FOR UPDATE TO dzoka_app USING(dzoka.permitted('channels:service'));
CREATE POLICY alerts_expiry ON dzoka.channel_alerts FOR DELETE TO dzoka_app USING(
 dzoka.permitted('channels:service') AND considered AND created_at<clock_timestamp()-interval '30 days');
CREATE POLICY alerts_capture ON dzoka.channel_alerts FOR INSERT TO CURRENT_USER WITH CHECK(tenant_id=dzoka.current_tenant());
GRANT UPDATE(considered) ON dzoka.channel_alerts TO dzoka_app;
GRANT DELETE ON dzoka.channel_alerts TO dzoka_app;
CREATE FUNCTION dzoka.notify_recovery_return() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 INSERT INTO dzoka.channel_alerts(tenant_id,member_ref) VALUES(NEW.tenant_id,NEW.recipient_ref);
 RETURN NEW;
END $$;
CREATE TRIGGER notify_recovery_return AFTER INSERT ON dzoka.recovery_returns
 FOR EACH ROW EXECUTE FUNCTION dzoka.notify_recovery_return();
CREATE FUNCTION dzoka.notify_relay_entry() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 INSERT INTO dzoka.channel_alerts(tenant_id,member_ref,relay_thread)
 SELECT tenant_id,CASE WHEN claimant_ref=NEW.sender_ref THEN finder_ref ELSE claimant_ref END,id
 FROM dzoka.relay_threads WHERE tenant_id=NEW.tenant_id AND id=NEW.thread_id;
 RETURN NEW;
END $$;
CREATE TRIGGER notify_relay_entry AFTER INSERT ON dzoka.relay_entries
 FOR EACH ROW EXECUTE FUNCTION dzoka.notify_relay_entry();
REVOKE ALL ON FUNCTION dzoka.notify_recovery_return(),dzoka.notify_relay_entry() FROM PUBLIC;

CREATE POLICY consent_operations_read ON dzoka.channel_operations FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY consent_operations_cancel ON dzoka.channel_operations FOR UPDATE TO CURRENT_USER
 USING(tenant_id=dzoka.current_tenant() AND state='QUEUED')
 WITH CHECK(tenant_id=dzoka.current_tenant() AND state='CANCELLED');
CREATE FUNCTION dzoka.apply_channel_consent() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF OLD.enabled AND NOT NEW.enabled THEN
   UPDATE dzoka.channel_operations SET state='CANCELLED'
    WHERE tenant_id=NEW.tenant_id AND contact_id=NEW.id AND state='QUEUED';
 ELSIF NEW.enabled AND NOT OLD.enabled THEN
   NEW.consented_at:=clock_timestamp();
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER apply_channel_consent BEFORE UPDATE OF enabled ON dzoka.channel_contacts
 FOR EACH ROW EXECUTE FUNCTION dzoka.apply_channel_consent();
REVOKE ALL ON FUNCTION dzoka.apply_channel_consent() FROM PUBLIC;
