ALTER TABLE dzoka.channel_operations DROP CONSTRAINT channel_operations_template_check;
ALTER TABLE dzoka.channel_operations ADD CHECK(template IN
 ('HELP','REPORT_SAVED','RELAY_UPDATE','RECOVERY_UPDATE','UNSUPPORTED','MATCH_ALERT','SESSION_REPLY'));
ALTER TABLE dzoka.channel_operations ADD COLUMN protected_reply text;
ALTER TABLE dzoka.channel_operations ADD COLUMN reply_until timestamptz;
ALTER TABLE dzoka.channel_operations ADD CHECK(
 (template='SESSION_REPLY' AND reply_until IS NOT NULL)
 OR (template<>'SESSION_REPLY' AND protected_reply IS NULL AND reply_until IS NULL));
GRANT UPDATE(protected_reply) ON dzoka.channel_operations TO dzoka_app;
ALTER TABLE dzoka.channel_events ADD COLUMN attachment_payload text;
ALTER TABLE dzoka.channel_events ADD COLUMN attachment_until timestamptz;
ALTER TABLE dzoka.channel_events ADD COLUMN attachment_due timestamptz;
ALTER TABLE dzoka.channel_events ADD COLUMN attachment_attempts integer NOT NULL DEFAULT 0 CHECK(attachment_attempts BETWEEN 0 AND 3);
GRANT UPDATE(attachment_payload,attachment_until,attachment_due,attachment_attempts) ON dzoka.channel_events TO dzoka_app;
CREATE INDEX channel_attachment_due ON dzoka.channel_events(tenant_id,attachment_due,event_key) WHERE attachment_payload IS NOT NULL;

CREATE TABLE dzoka.channel_match_watches (
 tenant_id uuid NOT NULL, contact_id uuid NOT NULL, report_id uuid NOT NULL, original_event text NOT NULL,
 due_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,contact_id,report_id), UNIQUE(tenant_id,contact_id,original_event),
 FOREIGN KEY(tenant_id,contact_id) REFERENCES dzoka.channel_contacts(tenant_id,id) ON DELETE CASCADE,
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id) ON DELETE CASCADE
);
CREATE INDEX channel_watch_due ON dzoka.channel_match_watches(tenant_id,due_at,contact_id,report_id);
CREATE TABLE dzoka.channel_match_notifications (
 tenant_id uuid NOT NULL, contact_id uuid NOT NULL, report_id uuid NOT NULL, candidate_id uuid NOT NULL,
 PRIMARY KEY(tenant_id,contact_id,report_id,candidate_id),
 FOREIGN KEY(tenant_id,contact_id,report_id) REFERENCES dzoka.channel_match_watches(tenant_id,contact_id,report_id)
 ON DELETE CASCADE,
 FOREIGN KEY(tenant_id,candidate_id) REFERENCES dzoka.reports(tenant_id,id) ON DELETE CASCADE
);
DO $$
DECLARE name text;
BEGIN
 FOREACH name IN ARRAY ARRAY['channel_match_watches','channel_match_notifications'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',name);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',name);
  EXECUTE format('CREATE POLICY service_scope ON dzoka.%I FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted(''channels:service'')) WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted(''channels:service''))',name);
  EXECUTE format('GRANT SELECT, INSERT, DELETE ON dzoka.%I TO dzoka_app',name);
 END LOOP;
END $$;
GRANT UPDATE(due_at) ON dzoka.channel_match_watches TO dzoka_app;

CREATE TABLE dzoka.channel_match_offers (
 tenant_id uuid NOT NULL, report_id uuid NOT NULL, revision int NOT NULL CHECK(revision>0),
 summary text NOT NULL CHECK(length(summary) BETWEEN 1 AND 320), enabled boolean NOT NULL,
 PRIMARY KEY(tenant_id,report_id),
 FOREIGN KEY(tenant_id,report_id,revision) REFERENCES dzoka.report_revisions(tenant_id,report_id,revision) ON DELETE CASCADE
);
ALTER TABLE dzoka.channel_match_offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.channel_match_offers FORCE ROW LEVEL SECURITY;
CREATE POLICY channel_offer_boundary ON dzoka.channel_match_offers AS RESTRICTIVE FOR ALL TO dzoka_app
 USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY channel_offer_read ON dzoka.channel_match_offers FOR SELECT TO dzoka_app
 USING(dzoka.permitted('channels:service') OR (dzoka.permitted('channels:admin') AND dzoka.permitted('recovery:manage')));
CREATE POLICY channel_offer_staff ON dzoka.channel_match_offers FOR ALL TO dzoka_app
 USING(dzoka.permitted('channels:admin') AND dzoka.permitted('recovery:manage'));
GRANT SELECT, INSERT, UPDATE(revision,summary,enabled) ON dzoka.channel_match_offers TO dzoka_app;

CREATE TABLE dzoka.channel_match_runs (
 tenant_id uuid NOT NULL, contact_id uuid NOT NULL, report_id uuid NOT NULL, id uuid NOT NULL,
 revision int NOT NULL, candidates jsonb NOT NULL CHECK(jsonb_typeof(candidates)='array' AND jsonb_array_length(candidates)<=5),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,contact_id,report_id,id),
 FOREIGN KEY(tenant_id,contact_id,report_id) REFERENCES dzoka.channel_match_watches(tenant_id,contact_id,report_id)
 ON DELETE CASCADE
);
ALTER TABLE dzoka.channel_match_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.channel_match_runs FORCE ROW LEVEL SECURITY;
CREATE POLICY channel_run_scope ON dzoka.channel_match_runs FOR ALL TO dzoka_app
 USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('channels:service'))
 WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('channels:service'));
GRANT SELECT, INSERT, DELETE ON dzoka.channel_match_runs TO dzoka_app;

CREATE FUNCTION dzoka.channel_candidate_intake(candidate uuid, revision int) RETURNS uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT i.id FROM dzoka.recovery_intakes i
 JOIN dzoka.reports r ON r.tenant_id=i.tenant_id AND r.id=i.report_id
 WHERE i.tenant_id=dzoka.current_tenant()
 AND dzoka.current_issuer()='urn:dzoka:channel' AND dzoka.current_subject()='whatsapp'
 AND r.id=candidate AND r.current_revision=revision AND r.deleted_at IS NULL
 AND NOT r.legal_hold AND r.expires_at>statement_timestamp() AND NOT i.blocked AND NOT i.returned
$$;
REVOKE ALL ON FUNCTION dzoka.channel_candidate_intake(uuid,int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.channel_candidate_intake(uuid,int) TO dzoka_app;

CREATE POLICY channel_return_lookup ON dzoka.recovery_returns FOR SELECT TO CURRENT_USER
 USING(tenant_id=dzoka.current_tenant());
CREATE FUNCTION dzoka.channel_return_confirmed(intake uuid, claim uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM dzoka.recovery_returns r
 WHERE r.tenant_id=dzoka.current_tenant()
 AND r.intake_id=intake AND r.claim_id=claim AND r.recipient_ref=dzoka.current_actor_ref())
$$;
REVOKE ALL ON FUNCTION dzoka.channel_return_confirmed(uuid,uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.channel_return_confirmed(uuid,uuid) TO dzoka_app;

CREATE TABLE dzoka.pickup_messages (
 tenant_id uuid NOT NULL, intake_id uuid NOT NULL, claim_id uuid NOT NULL,
 kind text NOT NULL CHECK(kind IN ('REQUEST','ACKNOWLEDGMENT')),
 actor_ref text NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,intake_id,claim_id,kind),
 FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id)
);
ALTER TABLE dzoka.pickup_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.pickup_messages FORCE ROW LEVEL SECURITY;
CREATE POLICY pickup_message_scope ON dzoka.pickup_messages AS RESTRICTIVE FOR ALL TO dzoka_app
 USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY pickup_message_read ON dzoka.pickup_messages FOR SELECT TO dzoka_app
 USING(dzoka.permitted('recovery:handover') OR (dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref()));
CREATE POLICY pickup_message_append ON dzoka.pickup_messages FOR INSERT TO dzoka_app
 WITH CHECK(dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref()
 AND EXISTS (SELECT 1 FROM dzoka.ownership_claims c
     WHERE c.tenant_id=pickup_messages.tenant_id AND c.id=pickup_messages.claim_id
       AND c.intake_id=pickup_messages.intake_id AND c.actor_ref=dzoka.current_actor_ref()
       AND ((kind='REQUEST' AND c.state='APPROVED') OR (kind='ACKNOWLEDGMENT' AND c.state='RETURNED')))
 AND (kind='REQUEST' OR dzoka.channel_return_confirmed(intake_id,claim_id)));
GRANT SELECT, INSERT ON dzoka.pickup_messages TO dzoka_app;
