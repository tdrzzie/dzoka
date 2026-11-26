-- New modules use the existing transaction-local tenant and actor identity.
-- Definer functions still check the invoker's context through narrowly scoped
-- membership/tenant rows, even though their execution role owns the schema.
CREATE POLICY fulfillment_member_context ON dzoka.memberships FOR SELECT TO CURRENT_USER USING(
 tenant_id=dzoka.current_tenant() AND issuer=dzoka.current_issuer() AND subject=dzoka.current_subject());
CREATE POLICY fulfillment_tenant_context ON dzoka.tenants FOR SELECT TO CURRENT_USER USING(id=dzoka.current_tenant());
CREATE TABLE dzoka.fulfillment_operations (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL, entity_id uuid NOT NULL,
 kind text NOT NULL CHECK(kind IN ('COURIER_BOOK','COURIER_CANCEL','PAYMENT_RESERVE','PAYMENT_RELEASE','PAYMENT_REFUND','PAYMENT_REVERSAL')),
 payload text NOT NULL, actor_ref text NOT NULL, request_sha text NOT NULL CHECK(request_sha ~ '^[0-9a-f]{64}$'),
 state text NOT NULL DEFAULT 'READY' CHECK(state IN ('READY','BLOCKED','SENDING','UNKNOWN','CONFIRMED','REJECTED','CANCELLED')),
 attempt int NOT NULL DEFAULT 0, lease uuid, lease_until timestamptz, worker text,
 provider_ref text, next_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,entity_id,kind)
);
CREATE TABLE dzoka.fulfillment_events (
 tenant_id uuid NOT NULL, id uuid NOT NULL, entity_id uuid NOT NULL, kind text NOT NULL,
 evidence_sha text NOT NULL CHECK(evidence_sha ~ '^[0-9a-f]{64}$'), actor_ref text NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(), observed_at timestamptz, PRIMARY KEY(tenant_id,id)
);
CREATE TABLE dzoka.fulfillment_callbacks (
 tenant_id uuid NOT NULL, event_id uuid NOT NULL, request_sha text NOT NULL,
 received_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,event_id)
);
CREATE TABLE dzoka.shipments (
 tenant_id uuid NOT NULL, id uuid NOT NULL, intake_id uuid NOT NULL, claim_id uuid NOT NULL,
 claimant_ref text NOT NULL, delivery_ref text NOT NULL, consent_sha text NOT NULL,
 quote_ref text NOT NULL, currency text NOT NULL, quote_minor bigint NOT NULL CHECK(quote_minor>=0),
 generation int, policy_version int, state text NOT NULL DEFAULT 'REQUESTED'
 CHECK(state IN ('REQUESTED','BOOKING','BOOKED','DISPATCHED','IN_TRANSIT','DELIVERED','OWNER_CONFIRMED','CANCEL_PENDING','CANCELLED','FAILED','LOST','RETURNED_TO_SENDER')),
 dispatched boolean NOT NULL DEFAULT false, owner_confirmed boolean NOT NULL DEFAULT false, consent_withdrawn boolean NOT NULL DEFAULT false,
 courier_ref text, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id)
);
CREATE TABLE dzoka.courier_quotes (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL, actor_ref text NOT NULL,
 delivery_ref text NOT NULL, provider_ref text NOT NULL, currency text NOT NULL, minor_units bigint NOT NULL,
 expires_at timestamptz NOT NULL DEFAULT(clock_timestamp()+interval '5 minutes'), PRIMARY KEY(tenant_id,id)
);
CREATE TABLE dzoka.courier_quote_budgets (
 tenant_id uuid NOT NULL, actor_ref text NOT NULL, bucket timestamptz NOT NULL, used int NOT NULL CHECK(used BETWEEN 1 AND 30),
 PRIMARY KEY(tenant_id,actor_ref,bucket)
);
CREATE TABLE dzoka.courier_custody (
 tenant_id uuid NOT NULL, shipment_id uuid NOT NULL, intake_id uuid NOT NULL, claim_id uuid NOT NULL,
 owner_ref text NOT NULL, previous_custodian text NOT NULL, courier_ref text NOT NULL,
 generation int NOT NULL, policy_version int NOT NULL, report_version bigint NOT NULL, dispatch_sha text NOT NULL,
 dispatched_at timestamptz NOT NULL DEFAULT clock_timestamp(), closed boolean NOT NULL DEFAULT false,
 receipt_sha text, received_at timestamptz, receipt_kind text CHECK(receipt_kind IN ('OWNER_RETURN','STAFF_RETURN')),
 PRIMARY KEY(tenant_id,shipment_id), FOREIGN KEY(tenant_id,shipment_id) REFERENCES dzoka.shipments(tenant_id,id)
);
CREATE TABLE dzoka.shipment_tracking (
 tenant_id uuid NOT NULL, event_id uuid NOT NULL, shipment_id uuid NOT NULL,
 status text NOT NULL CHECK(status IN ('IN_TRANSIT','DELIVERED','LOST','RETURNED_TO_SENDER')),
 evidence_sha text NOT NULL, observed_at timestamptz NOT NULL, recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,event_id), FOREIGN KEY(tenant_id,shipment_id) REFERENCES dzoka.shipments(tenant_id,id)
);
CREATE TABLE dzoka.courier_receipt_reviews (
 tenant_id uuid NOT NULL, id uuid NOT NULL, shipment_id uuid NOT NULL, generation int NOT NULL, policy_version int NOT NULL, report_version bigint NOT NULL,
 actor_ref text NOT NULL, evidence_sha text NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,shipment_id) REFERENCES dzoka.shipments(tenant_id,id)
);
CREATE UNIQUE INDEX one_active_shipment ON dzoka.shipments(tenant_id,intake_id) WHERE state NOT IN ('CANCELLED','FAILED','OWNER_CONFIRMED');
CREATE TABLE dzoka.rewards (
 tenant_id uuid NOT NULL, id uuid NOT NULL, intake_id uuid NOT NULL, claim_id uuid NOT NULL,
 payer_ref text NOT NULL, beneficiary_ref text NOT NULL CHECK(payer_ref<>beneficiary_ref),
 currency text NOT NULL CHECK(currency ~ '^[A-Z]{3}$'), minor_units bigint NOT NULL CHECK(minor_units BETWEEN 1 AND 1000000000000),
 fee_minor bigint NOT NULL CHECK(fee_minor>=0 AND fee_minor<minor_units), terms_sha text NOT NULL,
 policy_version int NOT NULL CHECK(policy_version>0), disputed boolean NOT NULL DEFAULT false,
 owner_confirmed boolean NOT NULL DEFAULT false,
 state text NOT NULL DEFAULT 'RESERVE_PENDING' CHECK(state IN ('RESERVE_PENDING','RESERVED','RELEASE_PENDING','REFUND_PENDING','RELEASED','REFUNDED','FAILED','REVERSE_PENDING','REVERSED')),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,claim_id,intake_id) REFERENCES dzoka.ownership_claims(tenant_id,id,intake_id)
);
CREATE TABLE dzoka.reward_transfers (
 tenant_id uuid NOT NULL, reward_id uuid NOT NULL, operation_id uuid NOT NULL, leg text NOT NULL,
 debit text NOT NULL CHECK(debit IN ('FUNDING','ESCROW','BENEFICIARY','FEE')), credit text NOT NULL CHECK(credit IN ('ESCROW','BENEFICIARY','PAYER','FEE')),
 minor_units bigint NOT NULL CHECK(minor_units>0), currency text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), CHECK(debit<>credit),
 PRIMARY KEY(tenant_id,reward_id,operation_id,leg), FOREIGN KEY(tenant_id,reward_id) REFERENCES dzoka.rewards(tenant_id,id),
 FOREIGN KEY(tenant_id,operation_id) REFERENCES dzoka.fulfillment_operations(tenant_id,id)
);
CREATE VIEW dzoka.reward_ledger WITH(security_invoker=true) AS
 SELECT tenant_id,reward_id,operation_id,leg,debit account,-minor_units amount,currency,created_at FROM dzoka.reward_transfers
 UNION ALL SELECT tenant_id,reward_id,operation_id,leg,credit account,minor_units amount,currency,created_at FROM dzoka.reward_transfers;
GRANT SELECT ON dzoka.reward_ledger TO dzoka_app;
CREATE TABLE dzoka.registered_items (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL, owner_ref text NOT NULL,
 description text NOT NULL CHECK(length(description) BETWEEN 1 AND 160), active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id)
);
CREATE TABLE dzoka.item_tags (
 tenant_id uuid NOT NULL, id uuid NOT NULL, item_id uuid NOT NULL, token_sha text NOT NULL UNIQUE,
 kind text NOT NULL CHECK(kind IN ('QR','NFC')), active boolean NOT NULL DEFAULT true,
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.registered_items(tenant_id,id)
);
CREATE TABLE dzoka.item_passports (
 tenant_id uuid NOT NULL, id uuid NOT NULL, item_id uuid NOT NULL, provider text NOT NULL,
 reference text NOT NULL, version text NOT NULL, evidence_sha text NOT NULL, verified boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.registered_items(tenant_id,id)
);
CREATE TABLE dzoka.tag_scan_budgets (
 tenant_id uuid NOT NULL, actor_ref text NOT NULL, bucket timestamptz NOT NULL, used int NOT NULL CHECK(used BETWEEN 1 AND 60),
 PRIMARY KEY(tenant_id,actor_ref,bucket)
);
CREATE TABLE dzoka.tag_threads (
 tenant_id uuid NOT NULL, id uuid NOT NULL, item_id uuid NOT NULL, tag_id uuid NOT NULL, finder_ref text NOT NULL,
 blocked boolean NOT NULL DEFAULT false, flagged boolean NOT NULL DEFAULT false,
 expires_at timestamptz NOT NULL DEFAULT(clock_timestamp()+interval '7 days'),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,item_id) REFERENCES dzoka.registered_items(tenant_id,id),
 FOREIGN KEY(tenant_id,tag_id) REFERENCES dzoka.item_tags(tenant_id,id)
);
CREATE TABLE dzoka.tag_messages (
 tenant_id uuid NOT NULL, thread_id uuid NOT NULL, id uuid NOT NULL, actor_ref text NOT NULL,
 phrase text NOT NULL CHECK(phrase IN ('FOUND_ITEM','ARRANGE_STAFF_RETURN','THANK_YOU')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,thread_id) REFERENCES dzoka.tag_threads(tenant_id,id)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['fulfillment_operations','fulfillment_events','fulfillment_callbacks','shipments','rewards',
  'reward_transfers','registered_items','item_tags','item_passports','tag_threads','tag_messages','courier_quotes','courier_custody','tag_scan_budgets','shipment_tracking','courier_receipt_reviews','courier_quote_budgets'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY tenant_boundary ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',t);
 END LOOP;
END $$;
CREATE POLICY quote_actor ON dzoka.courier_quotes TO dzoka_app USING(actor_ref=dzoka.current_actor_ref() OR dzoka.permitted('logistics:manage') OR dzoka.permitted('fulfillment:process'));
GRANT SELECT,INSERT ON dzoka.courier_quotes TO dzoka_app;
CREATE POLICY quote_budget_actor ON dzoka.courier_quote_budgets TO dzoka_app USING(actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('logistics:use'));
CREATE POLICY quote_budget_maintenance ON dzoka.courier_quote_budgets FOR SELECT TO dzoka_app USING(dzoka.permitted('fulfillment:process') AND bucket<clock_timestamp()-interval '2 days');
CREATE POLICY quote_budget_purge ON dzoka.courier_quote_budgets FOR DELETE TO dzoka_app USING(dzoka.permitted('fulfillment:process') AND bucket<clock_timestamp()-interval '2 days');
GRANT SELECT,INSERT,UPDATE(used),DELETE ON dzoka.courier_quote_budgets TO dzoka_app;
CREATE POLICY courier_history ON dzoka.courier_custody FOR SELECT TO dzoka_app USING(
 owner_ref=dzoka.current_actor_ref() OR dzoka.recovery_staff() OR dzoka.permitted('custody:operate') OR dzoka.permitted('intelligence:read') OR dzoka.permitted('logistics:manage') OR dzoka.permitted('fulfillment:process'));
GRANT SELECT ON dzoka.courier_custody TO dzoka_app;
CREATE POLICY tracking_read ON dzoka.shipment_tracking FOR SELECT TO dzoka_app USING(dzoka.permitted('fulfillment:process') OR dzoka.permitted('logistics:manage') OR EXISTS(SELECT 1 FROM dzoka.shipments s WHERE s.tenant_id=shipment_tracking.tenant_id AND s.id=shipment_id));
CREATE POLICY tracking_append ON dzoka.shipment_tracking FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('fulfillment:process'));
GRANT SELECT,INSERT ON dzoka.shipment_tracking TO dzoka_app;
CREATE POLICY receipt_review_read ON dzoka.courier_receipt_reviews FOR SELECT TO dzoka_app USING(dzoka.permitted('logistics:manage') OR dzoka.permitted('fulfillment:process') OR EXISTS(SELECT 1 FROM dzoka.shipments s WHERE s.tenant_id=courier_receipt_reviews.tenant_id AND s.id=shipment_id AND s.claimant_ref=dzoka.current_actor_ref()));
CREATE POLICY receipt_review_append ON dzoka.courier_receipt_reviews FOR INSERT TO dzoka_app WITH CHECK(actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('recovery:review') AND dzoka.permitted('logistics:manage'));
CREATE POLICY receipt_review_internal ON dzoka.courier_receipt_reviews FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
GRANT SELECT,INSERT ON dzoka.courier_receipt_reviews TO dzoka_app;
CREATE POLICY courier_custody_internal ON dzoka.courier_custody TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY shipment_internal ON dzoka.shipments TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY fulfillment_intake_internal ON dzoka.recovery_intakes FOR UPDATE TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY fulfillment_claim_internal ON dzoka.ownership_claims FOR UPDATE TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
-- Operation internals are readable only by trusted workers, never by arbitrary reward parties.
CREATE POLICY operation_read ON dzoka.fulfillment_operations FOR SELECT TO dzoka_app
 USING(dzoka.permitted('fulfillment:process') OR dzoka.permitted('logistics:manage') OR (dzoka.permitted('rewards:review') AND kind LIKE 'PAYMENT_%') OR actor_ref=dzoka.current_actor_ref());
CREATE POLICY operation_insert ON dzoka.fulfillment_operations FOR INSERT TO dzoka_app
 WITH CHECK(actor_ref=dzoka.current_actor_ref() AND (dzoka.permitted('logistics:manage') OR dzoka.permitted('rewards:use') OR dzoka.permitted('rewards:review')));
CREATE POLICY operation_update ON dzoka.fulfillment_operations FOR UPDATE TO dzoka_app USING(dzoka.permitted('fulfillment:process'));
CREATE POLICY operation_cancel_unsent ON dzoka.fulfillment_operations FOR UPDATE TO dzoka_app
 USING(actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('rewards:use') AND kind IN('PAYMENT_RESERVE','PAYMENT_RELEASE','PAYMENT_REFUND') AND state IN('READY','BLOCKED'))
 WITH CHECK(actor_ref=dzoka.current_actor_ref() AND state='CANCELLED');
GRANT SELECT,INSERT ON dzoka.fulfillment_operations TO dzoka_app;
GRANT UPDATE(state,attempt,lease,lease_until,worker,provider_ref,next_at) ON dzoka.fulfillment_operations TO dzoka_app;
CREATE POLICY event_read ON dzoka.fulfillment_events FOR SELECT TO dzoka_app USING(
 dzoka.permitted('fulfillment:process') OR dzoka.permitted('logistics:manage') OR dzoka.permitted('rewards:review') OR actor_ref=dzoka.current_actor_ref());
CREATE POLICY event_append ON dzoka.fulfillment_events FOR INSERT TO dzoka_app WITH CHECK(actor_ref=dzoka.current_actor_ref());
GRANT SELECT,INSERT ON dzoka.fulfillment_events TO dzoka_app;
CREATE POLICY callback_worker ON dzoka.fulfillment_callbacks FOR ALL TO dzoka_app USING(dzoka.permitted('fulfillment:process'));
GRANT SELECT,INSERT ON dzoka.fulfillment_callbacks TO dzoka_app;
CREATE POLICY shipment_access ON dzoka.shipments FOR ALL TO dzoka_app USING(dzoka.permitted('logistics:manage') OR dzoka.permitted('fulfillment:process') OR claimant_ref=dzoka.current_actor_ref())
 WITH CHECK(dzoka.permitted('logistics:manage') OR dzoka.permitted('fulfillment:process') OR claimant_ref=dzoka.current_actor_ref());
GRANT SELECT,INSERT ON dzoka.shipments TO dzoka_app;
GRANT UPDATE(state,generation,policy_version,dispatched,owner_confirmed,courier_ref,consent_withdrawn) ON dzoka.shipments TO dzoka_app;
CREATE POLICY reward_access ON dzoka.rewards FOR ALL TO dzoka_app USING(dzoka.permitted('fulfillment:process') OR dzoka.permitted('rewards:review') OR payer_ref=dzoka.current_actor_ref() OR beneficiary_ref=dzoka.current_actor_ref())
 WITH CHECK(dzoka.permitted('fulfillment:process') OR dzoka.permitted('rewards:review') OR payer_ref=dzoka.current_actor_ref() OR beneficiary_ref=dzoka.current_actor_ref());
GRANT SELECT,INSERT ON dzoka.rewards TO dzoka_app;
GRANT UPDATE(state,disputed,owner_confirmed) ON dzoka.rewards TO dzoka_app;
CREATE POLICY ledger_read ON dzoka.reward_transfers FOR SELECT TO dzoka_app USING(EXISTS(SELECT 1 FROM dzoka.rewards r WHERE r.tenant_id=reward_transfers.tenant_id AND r.id=reward_id));
CREATE POLICY ledger_append ON dzoka.reward_transfers FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('fulfillment:process'));
GRANT SELECT,INSERT ON dzoka.reward_transfers TO dzoka_app;
CREATE POLICY item_owner ON dzoka.registered_items FOR ALL TO dzoka_app USING(owner_ref=dzoka.current_actor_ref()) WITH CHECK(owner_ref=dzoka.current_actor_ref());
GRANT SELECT,INSERT ON dzoka.registered_items TO dzoka_app;
GRANT UPDATE(active) ON dzoka.registered_items TO dzoka_app;
CREATE POLICY tag_owner ON dzoka.item_tags FOR ALL TO dzoka_app USING(EXISTS(SELECT 1 FROM dzoka.registered_items i WHERE i.tenant_id=item_tags.tenant_id AND i.id=item_id))
 WITH CHECK(EXISTS(SELECT 1 FROM dzoka.registered_items i WHERE i.tenant_id=item_tags.tenant_id AND i.id=item_id));
GRANT SELECT,INSERT ON dzoka.item_tags TO dzoka_app;
GRANT UPDATE(active) ON dzoka.item_tags TO dzoka_app;
CREATE POLICY passport_owner ON dzoka.item_passports FOR ALL TO dzoka_app USING(EXISTS(SELECT 1 FROM dzoka.registered_items i WHERE i.tenant_id=item_passports.tenant_id AND i.id=item_id));
GRANT SELECT,INSERT ON dzoka.item_passports TO dzoka_app;
-- Restricted security-definer functions below mediate finder contact without exposing owner/tag tables.
CREATE POLICY registration_lookup ON dzoka.registered_items TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY tag_lookup ON dzoka.item_tags TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY thread_access ON dzoka.tag_threads TO dzoka_app USING(finder_ref=dzoka.current_actor_ref() OR EXISTS(SELECT 1 FROM dzoka.registered_items i WHERE i.tenant_id=tag_threads.tenant_id AND i.id=item_id));
CREATE POLICY thread_internal ON dzoka.tag_threads TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY thread_maintenance ON dzoka.tag_threads FOR SELECT TO dzoka_app USING(dzoka.permitted('passports:maintain') AND expires_at<=clock_timestamp());
CREATE POLICY scan_budget_actor ON dzoka.tag_scan_budgets TO dzoka_app USING(actor_ref=dzoka.current_actor_ref() AND dzoka.permitted('passports:use'));
GRANT SELECT,INSERT,UPDATE(used) ON dzoka.tag_scan_budgets TO dzoka_app;
CREATE POLICY scan_budget_retention ON dzoka.tag_scan_budgets FOR SELECT TO dzoka_app USING(dzoka.permitted('passports:maintain') AND bucket<clock_timestamp()-interval '2 days');
CREATE POLICY scan_budget_purge ON dzoka.tag_scan_budgets FOR DELETE TO dzoka_app USING(dzoka.permitted('passports:maintain') AND bucket<clock_timestamp()-interval '2 days');
GRANT DELETE ON dzoka.tag_scan_budgets TO dzoka_app;
GRANT SELECT,INSERT ON dzoka.tag_threads TO dzoka_app;
GRANT UPDATE(blocked,flagged) ON dzoka.tag_threads TO dzoka_app;
CREATE POLICY tag_message_read ON dzoka.tag_messages FOR SELECT TO dzoka_app USING(EXISTS(SELECT 1 FROM dzoka.tag_threads t WHERE t.tenant_id=tag_messages.tenant_id AND t.id=thread_id));
CREATE POLICY tag_message_write ON dzoka.tag_messages FOR INSERT TO dzoka_app WITH CHECK(actor_ref=dzoka.current_actor_ref() AND EXISTS(SELECT 1 FROM dzoka.tag_threads t WHERE t.tenant_id=tag_messages.tenant_id AND t.id=thread_id AND NOT blocked AND expires_at>clock_timestamp()));
CREATE POLICY tag_message_purge ON dzoka.tag_messages FOR DELETE TO dzoka_app USING(dzoka.permitted('passports:maintain') AND EXISTS(SELECT 1 FROM dzoka.tag_threads t WHERE t.tenant_id=tag_messages.tenant_id AND t.id=thread_id AND expires_at<=clock_timestamp()));
GRANT SELECT,INSERT,DELETE ON dzoka.tag_messages TO dzoka_app;
CREATE FUNCTION dzoka.lookup_item_tag(digest text) RETURNS TABLE(item uuid,tag uuid) LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT i.id,t.id FROM dzoka.item_tags t JOIN dzoka.registered_items i ON(i.tenant_id,i.id)=(t.tenant_id,t.item_id)
 WHERE t.tenant_id=dzoka.current_tenant() AND t.token_sha=digest AND t.active AND i.active AND dzoka.permitted('passports:use')
$$;
REVOKE ALL ON FUNCTION dzoka.lookup_item_tag(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.lookup_item_tag(text) TO dzoka_app;
CREATE FUNCTION dzoka.tag_thread_live(thread uuid) RETURNS TABLE(active boolean) LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT true FROM dzoka.tag_threads r JOIN dzoka.registered_items i ON(i.tenant_id,i.id)=(r.tenant_id,r.item_id)
 JOIN dzoka.item_tags t ON(t.tenant_id,t.id)=(r.tenant_id,r.tag_id)
 WHERE r.tenant_id=dzoka.current_tenant() AND r.id=thread AND i.active AND t.active AND r.expires_at>clock_timestamp()
 AND dzoka.permitted('passports:use') AND (r.finder_ref=dzoka.current_actor_ref() OR i.owner_ref=dzoka.current_actor_ref())
$$;
REVOKE ALL ON FUNCTION dzoka.tag_thread_live(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.tag_thread_live(uuid) TO dzoka_app;
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['ownership_claims','recovery_reviews','ownership_sessions','private_recovery_facts','recovery_fact_withdrawals','recovery_policies','recovery_returns'] LOOP
  EXECUTE format('CREATE POLICY fulfillment_metadata ON dzoka.%I TO CURRENT_USER USING(tenant_id=dzoka.current_tenant())',t);
 END LOOP;
END $$;
CREATE FUNCTION dzoka.fulfillment_context(intake uuid,claim uuid)
RETURNS TABLE(report uuid,owner_ref text,custodian text,generation int,blocked boolean,returned boolean,available boolean,authorized boolean,policy int,confirmed_return boolean,entitled boolean,report_version bigint)
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r uuid;
BEGIN
 IF NOT (dzoka.permitted('fulfillment:process') OR dzoka.permitted('logistics:manage') OR dzoka.permitted('logistics:use') OR dzoka.permitted('rewards:use') OR dzoka.permitted('rewards:review')) THEN RETURN; END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||dzoka.current_tenant()::text,17));
 SELECT i.report_id INTO r FROM dzoka.recovery_intakes i WHERE i.tenant_id=dzoka.current_tenant() AND i.id=intake;
 IF r IS NULL THEN RETURN; END IF;
 PERFORM dzoka.lock_recovery_report(dzoka.current_tenant(),r);
 RETURN QUERY SELECT i.report_id,c.actor_ref,i.custodian_ref,i.generation,i.blocked,i.returned,
  p.deleted_at IS NULL AND NOT p.legal_hold AND p.expires_at>clock_timestamp(),
  c.state='APPROVED' AND NOT EXISTS(SELECT 1 FROM dzoka.ownership_claims other WHERE other.tenant_id=i.tenant_id AND other.intake_id=i.id AND other.id<>c.id AND other.state IN ('PENDING','APPROVED')) AND EXISTS(SELECT 1 FROM dzoka.recovery_reviews v
   JOIN dzoka.ownership_sessions s ON(s.tenant_id,s.id)=(v.tenant_id,v.session_id)
   JOIN dzoka.private_recovery_facts f ON(f.tenant_id,f.id)=(s.tenant_id,s.fact_id)
   LEFT JOIN dzoka.recovery_fact_withdrawals w ON(w.tenant_id,w.fact_id)=(f.tenant_id,f.id)
   WHERE v.tenant_id=i.tenant_id AND v.intake_id=i.id AND v.claim_id=c.id AND v.decision='APPROVE'
    AND v.policy_version=rp.version AND v.report_version=p.version AND v.generation=i.generation
    AND s.outcome='PASSED' AND s.expires_at>clock_timestamp() AND f.report_revision=p.current_revision
    AND s.report_version=v.report_version AND s.generation=v.generation AND s.policy_version=v.policy_version AND w.fact_id IS NULL),
  rp.version,(EXISTS(SELECT 1 FROM dzoka.recovery_returns rr WHERE rr.tenant_id=i.tenant_id AND rr.intake_id=i.id AND rr.claim_id=c.id AND rr.recipient_ref=c.actor_ref)
   OR EXISTS(SELECT 1 FROM dzoka.courier_custody cc WHERE cc.tenant_id=i.tenant_id AND cc.intake_id=i.id AND cc.claim_id=c.id AND cc.owner_ref=c.actor_ref AND cc.closed AND cc.receipt_kind='OWNER_RETURN')),c.state='APPROVED',p.version
 FROM dzoka.recovery_intakes i JOIN dzoka.ownership_claims c ON(c.tenant_id,c.intake_id)=(i.tenant_id,i.id) AND c.id=claim
 JOIN dzoka.reports p ON(p.tenant_id,p.id)=(i.tenant_id,i.report_id)
 JOIN LATERAL(SELECT version FROM dzoka.recovery_policies x WHERE x.tenant_id=i.tenant_id ORDER BY version DESC LIMIT 1) rp ON true
 WHERE i.tenant_id=dzoka.current_tenant() AND i.id=intake
  AND (c.actor_ref=dzoka.current_actor_ref() OR dzoka.permitted('logistics:manage') OR dzoka.permitted('fulfillment:process') OR dzoka.permitted('rewards:review'));
END $$;
REVOKE ALL ON FUNCTION dzoka.fulfillment_context(uuid,uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.fulfillment_context(uuid,uuid) TO dzoka_app;
CREATE FUNCTION dzoka.courier_handoff(shipment uuid,courier text,evidence text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE s dzoka.shipments; c record;
BEGIN
 IF NOT dzoka.permitted('logistics:manage') OR courier !~ '^[0-9a-f]{64}$' OR evidence !~ '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'Denied courier handoff' USING ERRCODE='42501'; END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||dzoka.current_tenant()::text,17));
 SELECT * INTO s FROM dzoka.shipments x WHERE x.tenant_id=dzoka.current_tenant() AND x.id=shipment;
 IF NOT FOUND THEN RAISE EXCEPTION 'Missing shipment'; END IF;
 IF s.dispatched THEN
  IF EXISTS(SELECT 1 FROM dzoka.courier_custody cc WHERE cc.tenant_id=s.tenant_id AND cc.shipment_id=s.id AND cc.courier_ref=courier AND cc.dispatch_sha=evidence) THEN RETURN; END IF;
  RAISE EXCEPTION 'Different handoff';
 END IF;
 SELECT * INTO c FROM dzoka.fulfillment_context(s.intake_id,s.claim_id);
 IF NOT FOUND OR s.state<>'BOOKED' OR s.consent_withdrawn OR NOT c.authorized OR c.blocked OR c.returned OR NOT c.available
  OR c.generation<>s.generation OR c.policy<>s.policy_version OR c.custodian<>dzoka.current_actor_ref() THEN RAISE EXCEPTION 'Stale handoff authorization'; END IF;
 UPDATE dzoka.recovery_intakes SET custodian_ref=courier,generation=generation+1 WHERE tenant_id=s.tenant_id AND id=s.intake_id;
 INSERT INTO dzoka.courier_custody(tenant_id,shipment_id,intake_id,claim_id,owner_ref,previous_custodian,courier_ref,generation,policy_version,report_version,dispatch_sha)
 VALUES(s.tenant_id,s.id,s.intake_id,s.claim_id,c.owner_ref,c.custodian,courier,c.generation+1,c.policy,c.report_version,evidence);
 UPDATE dzoka.shipments SET dispatched=true,state='DISPATCHED',courier_ref=courier WHERE tenant_id=s.tenant_id AND id=s.id;
END $$;
REVOKE ALL ON FUNCTION dzoka.courier_handoff(uuid,text,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.courier_handoff(uuid,text,text) TO dzoka_app;
CREATE FUNCTION dzoka.courier_receipt(shipment uuid,evidence text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE s dzoka.shipments; h dzoka.courier_custody; c record; expected_generation int; expected_policy int; expected_report bigint;
BEGIN
 IF NOT dzoka.permitted('logistics:use') OR evidence !~ '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'Denied receipt' USING ERRCODE='42501'; END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||dzoka.current_tenant()::text,17));
 SELECT * INTO s FROM dzoka.shipments x WHERE x.tenant_id=dzoka.current_tenant() AND x.id=shipment AND x.claimant_ref=dzoka.current_actor_ref();
 IF NOT FOUND THEN RAISE EXCEPTION 'Missing shipment'; END IF;
 SELECT * INTO h FROM dzoka.courier_custody x WHERE x.tenant_id=s.tenant_id AND x.shipment_id=s.id;
 IF NOT FOUND THEN RAISE EXCEPTION 'No dispatch'; END IF;
 IF h.closed THEN
  IF h.receipt_sha=evidence THEN RETURN; END IF;
  RAISE EXCEPTION 'Different receipt';
 END IF;
 SELECT * INTO c FROM dzoka.fulfillment_context(s.intake_id,s.claim_id);
 expected_generation:=h.generation; expected_policy:=h.policy_version;
 SELECT v.generation,v.policy_version,v.report_version INTO expected_generation,expected_policy,expected_report FROM dzoka.courier_receipt_reviews v
  WHERE v.tenant_id=s.tenant_id AND v.shipment_id=s.id ORDER BY v.created_at DESC,v.id DESC LIMIT 1;
 IF NOT FOUND THEN expected_generation:=h.generation;expected_policy:=h.policy_version;expected_report:=h.report_version; END IF;
 IF s.state<>'DELIVERED' OR c.blocked OR c.returned OR NOT c.available OR c.generation<>expected_generation
  OR c.policy<>expected_policy OR c.report_version<>expected_report OR c.custodian<>h.courier_ref
  OR NOT EXISTS(SELECT 1 FROM dzoka.ownership_claims x WHERE x.tenant_id=s.tenant_id AND x.id=s.claim_id AND x.state='APPROVED') THEN RAISE EXCEPTION 'Receipt blocked'; END IF;
 UPDATE dzoka.recovery_intakes SET returned=true,generation=generation+1 WHERE tenant_id=s.tenant_id AND id=s.intake_id;
 UPDATE dzoka.ownership_claims SET state='RETURNED' WHERE tenant_id=s.tenant_id AND id=s.claim_id;
 UPDATE dzoka.courier_custody SET closed=true,receipt_sha=evidence,receipt_kind='OWNER_RETURN',received_at=clock_timestamp() WHERE tenant_id=s.tenant_id AND shipment_id=s.id;
 UPDATE dzoka.shipments SET state='OWNER_CONFIRMED',owner_confirmed=true WHERE tenant_id=s.tenant_id AND id=s.id;
END $$;
REVOKE ALL ON FUNCTION dzoka.courier_receipt(uuid,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.courier_receipt(uuid,text) TO dzoka_app;
CREATE FUNCTION dzoka.courier_staff_receipt(shipment uuid,evidence text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE s dzoka.shipments; h dzoka.courier_custody;
BEGIN
 IF NOT dzoka.permitted('logistics:manage') OR evidence !~ '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'Denied staff receipt' USING ERRCODE='42501'; END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended('recovery-tenant:'||dzoka.current_tenant()::text,17));
 SELECT * INTO s FROM dzoka.shipments x WHERE x.tenant_id=dzoka.current_tenant() AND x.id=shipment;
 SELECT * INTO h FROM dzoka.courier_custody x WHERE x.tenant_id=s.tenant_id AND x.shipment_id=s.id AND x.previous_custodian=dzoka.current_actor_ref();
 IF NOT FOUND THEN RAISE EXCEPTION 'No matching staff dispatch'; END IF;
 IF h.closed THEN
  IF h.receipt_kind='STAFF_RETURN' AND h.receipt_sha=evidence THEN RETURN; END IF;
  RAISE EXCEPTION 'Conflicting staff receipt';
 END IF;
 IF s.state NOT IN ('RETURNED_TO_SENDER','LOST') THEN RAISE EXCEPTION 'Shipment not returning'; END IF;
 UPDATE dzoka.recovery_intakes SET custodian_ref=h.previous_custodian,generation=generation+1 WHERE tenant_id=s.tenant_id AND id=s.intake_id AND NOT returned;
 IF NOT FOUND THEN RAISE EXCEPTION 'Return conflict'; END IF;
 UPDATE dzoka.ownership_claims SET state='PENDING' WHERE tenant_id=s.tenant_id AND id=s.claim_id AND state='APPROVED';
 UPDATE dzoka.courier_custody SET closed=true,receipt_sha=evidence,receipt_kind='STAFF_RETURN',received_at=clock_timestamp() WHERE tenant_id=s.tenant_id AND shipment_id=s.id;
 UPDATE dzoka.shipments SET state='CANCELLED' WHERE tenant_id=s.tenant_id AND id=s.id;
END $$;
REVOKE ALL ON FUNCTION dzoka.courier_staff_receipt(uuid,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.courier_staff_receipt(uuid,text) TO dzoka_app;
CREATE FUNCTION dzoka.guard_reward_transfer() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
DECLARE r dzoka.rewards; k text; balance numeric;
BEGIN
 SELECT * INTO r FROM dzoka.rewards WHERE tenant_id=NEW.tenant_id AND id=NEW.reward_id FOR UPDATE;
 SELECT kind INTO k FROM dzoka.fulfillment_operations WHERE tenant_id=NEW.tenant_id AND id=NEW.operation_id AND entity_id=NEW.reward_id AND state='CONFIRMED';
 IF NOT FOUND OR NEW.currency<>r.currency OR NOT(
 (k='PAYMENT_RESERVE' AND NEW.leg='RESERVE' AND NEW.debit='FUNDING' AND NEW.credit='ESCROW' AND NEW.minor_units=r.minor_units)
 OR (k='PAYMENT_REFUND' AND NEW.leg='REFUND' AND NEW.debit='ESCROW' AND NEW.credit='PAYER' AND NEW.minor_units=r.minor_units)
 OR (k='PAYMENT_RELEASE' AND NEW.leg='PAYOUT' AND NEW.debit='ESCROW' AND NEW.credit='BENEFICIARY' AND NEW.minor_units=r.minor_units-r.fee_minor)
 OR (k='PAYMENT_RELEASE' AND NEW.leg='FEE' AND NEW.debit='ESCROW' AND NEW.credit='FEE' AND NEW.minor_units=r.fee_minor)
 OR (k='PAYMENT_REVERSAL' AND NEW.leg='PAYOUT_REVERSAL' AND NEW.debit='BENEFICIARY' AND NEW.credit='PAYER' AND NEW.minor_units=r.minor_units-r.fee_minor)
 OR (k='PAYMENT_REVERSAL' AND NEW.leg='FEE_REVERSAL' AND NEW.debit='FEE' AND NEW.credit='PAYER' AND NEW.minor_units=r.fee_minor)) THEN
 RAISE EXCEPTION 'Invalid ledger transfer' USING ERRCODE='23514'; END IF;
 IF NEW.debit<>'FUNDING' THEN
  SELECT coalesce(sum(CASE WHEN credit=NEW.debit THEN minor_units WHEN debit=NEW.debit THEN -minor_units ELSE 0 END),0) INTO balance
   FROM dzoka.reward_transfers WHERE tenant_id=NEW.tenant_id AND reward_id=NEW.reward_id;
  IF balance<NEW.minor_units THEN RAISE EXCEPTION 'Insufficient escrow' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reward_transfer_integrity BEFORE INSERT ON dzoka.reward_transfers FOR EACH ROW EXECUTE FUNCTION dzoka.guard_reward_transfer();
CREATE FUNCTION dzoka.guard_reward_dispute() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF OLD.disputed AND NOT NEW.disputed AND (NOT dzoka.permitted('rewards:review') OR dzoka.current_actor_ref() IN (OLD.payer_ref,OLD.beneficiary_ref)) THEN
  RAISE EXCEPTION 'Independent dispute review required' USING ERRCODE='42501';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reward_dispute_integrity BEFORE UPDATE OF disputed ON dzoka.rewards FOR EACH ROW EXECUTE FUNCTION dzoka.guard_reward_dispute();
CREATE INDEX fulfillment_dispatch_ready ON dzoka.fulfillment_operations(tenant_id,next_at,created_at,id) WHERE state IN('READY','BLOCKED','SENDING','UNKNOWN');
CREATE INDEX shipment_tracking_order ON dzoka.shipment_tracking(tenant_id,shipment_id,observed_at DESC);
CREATE INDEX registration_owner_order ON dzoka.registered_items(tenant_id,owner_ref,created_at DESC,id);
CREATE INDEX tag_thread_finder_budget ON dzoka.tag_threads(tenant_id,finder_ref,expires_at);
CREATE INDEX tag_thread_item_order ON dzoka.tag_threads(tenant_id,item_id,expires_at);
