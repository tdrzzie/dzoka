CREATE FUNCTION dzoka.lock_recovery_report(tenant uuid, report uuid) RETURNS void
LANGUAGE sql SET search_path=pg_catalog AS $$
  SELECT pg_advisory_xact_lock(hashtextextended('recovery-report:'||tenant||':'||report,17))
$$;
REVOKE ALL ON FUNCTION dzoka.lock_recovery_report(uuid,uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.lock_recovery_report(uuid,uuid) TO dzoka_app;
CREATE FUNCTION dzoka.guard_recovery_report_change() RETURNS trigger LANGUAGE plpgsql
SET search_path=pg_catalog AS $$
BEGIN
  PERFORM dzoka.lock_recovery_report(OLD.tenant_id,OLD.id);
  IF NEW.version=OLD.version AND (NEW.current_revision,NEW.legal_hold,NEW.deleted_at,NEW.expires_at)
       IS DISTINCT FROM (OLD.current_revision,OLD.legal_hold,OLD.deleted_at,OLD.expires_at) THEN
    NEW.version:=OLD.version+1;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER guard_recovery_report_change BEFORE UPDATE OF current_revision,version,legal_hold,deleted_at,expires_at
ON dzoka.reports FOR EACH ROW EXECUTE FUNCTION dzoka.guard_recovery_report_change();

CREATE TABLE dzoka.recovery_policies (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), version integer NOT NULL CHECK(version>0),
 dual_control boolean NOT NULL, allow_representatives boolean NOT NULL,
 approved_by text NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,version)
);
CREATE TABLE dzoka.recovery_intakes (
 tenant_id uuid NOT NULL, id uuid NOT NULL, item_id uuid NOT NULL, report_id uuid NOT NULL,
 site_id uuid NOT NULL, custodian_ref text NOT NULL,
 generation integer NOT NULL DEFAULT 1, blocked boolean NOT NULL DEFAULT false, returned boolean NOT NULL DEFAULT false,
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,item_id), UNIQUE(tenant_id,report_id),
 FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.items(tenant_id,id),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id)
);
CREATE TABLE dzoka.ownership_claims (
 tenant_id uuid NOT NULL, id uuid NOT NULL, intake_id uuid NOT NULL, actor_ref text NOT NULL,
 capacity text NOT NULL CHECK(capacity IN ('OWNER','REPRESENTATIVE')),
 state text NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','APPROVED','REJECTED','WITHDRAWN','REVOKED','RETURNED')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,intake_id) REFERENCES dzoka.recovery_intakes(tenant_id,id)
);
CREATE UNIQUE INDEX one_claimant_per_intake ON dzoka.ownership_claims(tenant_id,intake_id,actor_ref)
 WHERE state IN ('PENDING','APPROVED');
CREATE UNIQUE INDEX one_recovery_entitlement ON dzoka.ownership_claims(tenant_id,intake_id) WHERE state='APPROVED';
CREATE INDEX recovery_claim_actor ON dzoka.ownership_claims(tenant_id,actor_ref,created_at);

CREATE TABLE dzoka.private_recovery_facts (
 tenant_id uuid NOT NULL, id uuid NOT NULL, intake_id uuid NOT NULL, report_revision integer NOT NULL,
 kind text NOT NULL CHECK(kind IN ('HIDDEN_MARK','PRIVATE_ACCESSORY','PRIVATE_CONTENT')),
 source_sha text NOT NULL CHECK(source_sha ~ '^[0-9a-f]{64}$'), answer_mac text NOT NULL CHECK(answer_mac ~ '^[0-9a-f]{64}$'),
 observer_ref text NOT NULL,
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,intake_id) REFERENCES dzoka.recovery_intakes(tenant_id,id)
);
CREATE INDEX recovery_fact_item ON dzoka.private_recovery_facts(tenant_id,intake_id,report_revision,kind);
CREATE TABLE dzoka.recovery_fact_withdrawals (
 tenant_id uuid NOT NULL, fact_id uuid NOT NULL, actor_ref text NOT NULL,
 reason text NOT NULL CHECK(reason IN ('DISCLOSED','WITHDRAWN')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,fact_id),
 FOREIGN KEY(tenant_id,fact_id) REFERENCES dzoka.private_recovery_facts(tenant_id,id)
);
CREATE TABLE dzoka.ownership_sessions (
 tenant_id uuid NOT NULL, id uuid NOT NULL,intake_id uuid NOT NULL, claim_id uuid NOT NULL, fact_id uuid NOT NULL,
 actor_ref text NOT NULL, policy_version integer NOT NULL, report_version bigint NOT NULL, generation integer NOT NULL,
 expires_at timestamptz NOT NULL DEFAULT (clock_timestamp()+interval '10 minutes'),
 outcome text CHECK(outcome IN ('PASSED','FAILED')),
 PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,fact_id,intake_id) REFERENCES dzoka.private_recovery_facts(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,policy_version) REFERENCES dzoka.recovery_policies(tenant_id,version)
);
CREATE INDEX recovery_session_item ON dzoka.ownership_sessions(tenant_id,intake_id,expires_at);
CREATE TABLE dzoka.recovery_reviews (
 tenant_id uuid NOT NULL, id uuid NOT NULL, intake_id uuid NOT NULL, claim_id uuid NOT NULL, session_id uuid,
 actor_ref text NOT NULL, policy_version integer NOT NULL, report_version bigint NOT NULL,
 generation integer NOT NULL, decision text NOT NULL CHECK(decision IN ('APPROVE','REJECT')),
 reason text NOT NULL CHECK(reason IN ('INDEPENDENT_EVIDENCE','INSUFFICIENT_EVIDENCE','CONFLICT','FRAUD')),
 evidence_sha text NOT NULL CHECK(evidence_sha ~ '^[0-9a-f]{64}$'),
 entitlement_sha text CHECK(entitlement_sha ~ '^[0-9a-f]{64}$'),
 CHECK(decision='REJECT' OR session_id IS NOT NULL),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,session_id) REFERENCES dzoka.ownership_sessions(tenant_id,id),
 FOREIGN KEY(tenant_id,policy_version) REFERENCES dzoka.recovery_policies(tenant_id,version)
);
CREATE INDEX recovery_review_claim ON dzoka.recovery_reviews(tenant_id,claim_id,created_at);
CREATE TABLE dzoka.handover_grants (
 tenant_id uuid NOT NULL,id uuid NOT NULL,intake_id uuid NOT NULL,claim_id uuid NOT NULL,review_id uuid NOT NULL,
 audience_ref text NOT NULL, issuer_ref text NOT NULL, nonce text NOT NULL,
 token_mac text NOT NULL CHECK(token_mac ~ '^[0-9a-f]{64}$'),
 expires_at timestamptz NOT NULL DEFAULT (clock_timestamp()+interval '5 minutes'), used_at timestamptz,
 PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,review_id) REFERENCES dzoka.recovery_reviews(tenant_id,id)
);
CREATE INDEX recovery_grant_item ON dzoka.handover_grants(tenant_id,intake_id,expires_at);
CREATE TABLE dzoka.recovery_activity (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL, intake_id uuid,
 claim_id uuid, actor_ref text NOT NULL, event text NOT NULL, resource_id uuid, reason text,
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,intake_id) REFERENCES dzoka.recovery_intakes(tenant_id,id)
);
CREATE INDEX recovery_activity_scope ON dzoka.recovery_activity(tenant_id,intake_id,event,occurred_at);
CREATE INDEX recovery_activity_actor ON dzoka.recovery_activity(tenant_id,actor_ref,event,occurred_at);
CREATE TABLE dzoka.recovery_returns (
 tenant_id uuid NOT NULL, intake_id uuid NOT NULL, id uuid NOT NULL, claim_id uuid NOT NULL, grant_id uuid NOT NULL,
 actor_ref text NOT NULL, from_custodian text NOT NULL, recipient_ref text NOT NULL, site_id uuid NOT NULL,
 confirmation_sha text NOT NULL CHECK(confirmation_sha ~ '^[0-9a-f]{64}$'),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,intake_id), UNIQUE(tenant_id,actor_ref,id),
 FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id),
 FOREIGN KEY(tenant_id,grant_id) REFERENCES dzoka.handover_grants(tenant_id,id)
);

CREATE FUNCTION dzoka.recovery_staff() RETURNS boolean LANGUAGE sql STABLE SET search_path=pg_catalog AS $$
 SELECT dzoka.permitted('recovery:manage') OR dzoka.permitted('recovery:verify')
     OR dzoka.permitted('recovery:review') OR dzoka.permitted('recovery:handover')
$$;
REVOKE ALL ON FUNCTION dzoka.recovery_staff() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.recovery_staff() TO dzoka_app;
DO $$
DECLARE t text;
BEGIN
 FOREACH t IN ARRAY ARRAY['recovery_policies','recovery_intakes','ownership_claims','private_recovery_facts',
    'recovery_fact_withdrawals','ownership_sessions','recovery_reviews','handover_grants','recovery_activity','recovery_returns'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY recovery_tenant ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING (tenant_id=dzoka.current_tenant()) WITH CHECK (tenant_id=dzoka.current_tenant())',t);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',t);
 END LOOP;
END $$;
CREATE POLICY policy_read ON dzoka.recovery_policies FOR SELECT TO dzoka_app USING(dzoka.recovery_staff() OR dzoka.permitted('claims:submit'));
CREATE POLICY policy_write ON dzoka.recovery_policies FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('recovery:manage') AND approved_by=dzoka.current_actor_ref());
CREATE POLICY intake_recovery_read ON dzoka.recovery_intakes FOR SELECT TO dzoka_app USING(dzoka.recovery_staff() OR dzoka.permitted('claims:submit'));
CREATE POLICY intake_recovery_insert ON dzoka.recovery_intakes FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('recovery:manage') AND custodian_ref=dzoka.current_actor_ref());
CREATE POLICY intake_recovery_change ON dzoka.recovery_intakes FOR UPDATE TO dzoka_app USING(dzoka.permitted('recovery:manage') OR dzoka.permitted('recovery:handover'));
GRANT UPDATE(generation,blocked,returned) ON dzoka.recovery_intakes TO dzoka_app;
CREATE POLICY claims_read ON dzoka.ownership_claims FOR SELECT TO dzoka_app USING(dzoka.recovery_staff() OR (dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref()));
CREATE POLICY claims_insert ON dzoka.ownership_claims FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref() AND state='PENDING');
CREATE POLICY claims_change ON dzoka.ownership_claims FOR UPDATE TO dzoka_app
 USING(dzoka.permitted('recovery:review') OR dzoka.permitted('recovery:handover') OR dzoka.permitted('recovery:manage')
       OR (dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref() AND state='PENDING'))
 WITH CHECK(dzoka.permitted('recovery:review') OR dzoka.permitted('recovery:handover') OR dzoka.permitted('recovery:manage')
       OR (dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref() AND state='WITHDRAWN'));
GRANT UPDATE(state) ON dzoka.ownership_claims TO dzoka_app;
CREATE POLICY facts_read ON dzoka.private_recovery_facts FOR SELECT TO dzoka_app USING(dzoka.recovery_staff()
 OR (dzoka.permitted('claims:submit') AND EXISTS(SELECT 1 FROM dzoka.ownership_claims c
  WHERE c.tenant_id=private_recovery_facts.tenant_id AND c.intake_id=private_recovery_facts.intake_id
    AND c.actor_ref=dzoka.current_actor_ref() AND c.state='PENDING')));
CREATE POLICY facts_append ON dzoka.private_recovery_facts FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('recovery:verify') AND observer_ref=dzoka.current_actor_ref());
CREATE POLICY fact_withdrawals_read ON dzoka.recovery_fact_withdrawals FOR SELECT TO dzoka_app USING(
 EXISTS(SELECT 1 FROM dzoka.private_recovery_facts f WHERE f.tenant_id=recovery_fact_withdrawals.tenant_id AND f.id=fact_id));
CREATE POLICY fact_withdrawals_append ON dzoka.recovery_fact_withdrawals FOR INSERT TO dzoka_app
 WITH CHECK(dzoka.permitted('recovery:verify') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY sessions_read ON dzoka.ownership_sessions FOR SELECT TO dzoka_app USING(dzoka.recovery_staff() OR (dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref()));
CREATE POLICY sessions_append ON dzoka.ownership_sessions FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref() AND outcome IS NULL);
CREATE POLICY sessions_change ON dzoka.ownership_sessions FOR UPDATE TO dzoka_app USING(dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref() AND outcome IS NULL)
 WITH CHECK(dzoka.permitted('claims:submit') AND actor_ref=dzoka.current_actor_ref() AND outcome IS NOT NULL);
GRANT UPDATE(outcome) ON dzoka.ownership_sessions TO dzoka_app;
CREATE POLICY reviews_read ON dzoka.recovery_reviews FOR SELECT TO dzoka_app USING(dzoka.recovery_staff());
CREATE POLICY reviews_append ON dzoka.recovery_reviews FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('recovery:review') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY grant_read ON dzoka.handover_grants FOR SELECT TO dzoka_app USING(dzoka.permitted('recovery:review') OR dzoka.permitted('recovery:handover'));
CREATE POLICY grant_append ON dzoka.handover_grants FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('recovery:review') AND issuer_ref=dzoka.current_actor_ref() AND used_at IS NULL);
CREATE POLICY grant_change ON dzoka.handover_grants FOR UPDATE TO dzoka_app USING(dzoka.permitted('recovery:handover') AND audience_ref=dzoka.current_actor_ref() AND used_at IS NULL)
 WITH CHECK(dzoka.permitted('recovery:handover') AND audience_ref=dzoka.current_actor_ref() AND used_at IS NOT NULL);
GRANT UPDATE(used_at) ON dzoka.handover_grants TO dzoka_app;
CREATE POLICY recovery_activity_read ON dzoka.recovery_activity FOR SELECT TO dzoka_app USING(dzoka.recovery_staff()
 OR (dzoka.permitted('claims:submit') AND event IN ('CHALLENGE_ISSUED','CHALLENGE_FAILED','CLAIM_OPENED')));
CREATE POLICY recovery_activity_append ON dzoka.recovery_activity FOR INSERT TO dzoka_app WITH CHECK((dzoka.recovery_staff() OR dzoka.permitted('claims:submit')) AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY returns_read ON dzoka.recovery_returns FOR SELECT TO dzoka_app USING(dzoka.recovery_staff());
CREATE POLICY returns_append ON dzoka.recovery_returns FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('recovery:handover') AND actor_ref=dzoka.current_actor_ref());

-- The migration owner can read only the current tenant through the fixed, redacted definer function.
CREATE POLICY recovery_report_metadata ON dzoka.reports FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY recovery_intake_metadata ON dzoka.recovery_intakes FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE FUNCTION dzoka.recovery_report_status(intake uuid)
RETURNS TABLE(revision integer, version bigint, available boolean)
LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT r.current_revision,r.version,r.deleted_at IS NULL AND NOT r.legal_hold AND r.expires_at>clock_timestamp()
 FROM dzoka.recovery_intakes i JOIN dzoka.reports r ON (r.tenant_id,r.id)=(i.tenant_id,i.report_id)
 WHERE i.tenant_id=dzoka.current_tenant() AND i.id=intake
$$;
REVOKE ALL ON FUNCTION dzoka.recovery_report_status(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.recovery_report_status(uuid) TO dzoka_app;
CREATE POLICY recovery_return_audit ON dzoka.audit_events FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
 AND action='recovery.returned' AND dzoka.permitted('recovery:handover'));
CREATE POLICY recovery_return_outbox ON dzoka.outbox FOR INSERT TO dzoka_app WITH CHECK(
 tenant_id=dzoka.current_tenant() AND event_type='recovery.returned' AND dzoka.permitted('recovery:handover'));
