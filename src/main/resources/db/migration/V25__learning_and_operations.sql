CREATE TABLE dzoka.learning_policies (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), version int NOT NULL CHECK(version>0),
 opt_in boolean NOT NULL, require_gate boolean NOT NULL, retention_days int NOT NULL CHECK(retention_days BETWEEN 1 AND 365),
 purpose_sha text NOT NULL CHECK(purpose_sha ~ '^[0-9a-f]{64}$'), actor_ref text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,version)
);
CREATE TABLE dzoka.learning_feedback (
 tenant_id uuid NOT NULL, id uuid NOT NULL, report_id uuid NOT NULL, candidate_id uuid,
 source_id uuid NOT NULL, source_revision int NOT NULL, candidate_revision int, kind text NOT NULL CHECK(kind IN ('DECLARED_MATCH','DECLARED_REJECTION','CLAIM_APPROVED','CLAIM_REJECTED','RECOVERY_VERIFIED')),
 source_sha text NOT NULL, actor_ref text NOT NULL, observed_at timestamptz NOT NULL,
 recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,source_sha),
 FOREIGN KEY(tenant_id,report_id) REFERENCES dzoka.reports(tenant_id,id)
);
CREATE TABLE dzoka.learning_adjudications (
 tenant_id uuid NOT NULL, id uuid NOT NULL, feedback_id uuid NOT NULL, revision int NOT NULL CHECK(revision>0),
 label text NOT NULL CHECK(label IN ('POSITIVE','NEGATIVE','UNAVAILABLE')),
 institution_sha text NOT NULL CHECK(institution_sha ~ '^[0-9a-f]{64}$'), evidence_sha text NOT NULL,
 actor_ref text NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), UNIQUE(tenant_id,feedback_id,revision), FOREIGN KEY(tenant_id,feedback_id) REFERENCES dzoka.learning_feedback(tenant_id,id)
);
CREATE TABLE dzoka.learning_datasets (
 tenant_id uuid NOT NULL,id uuid NOT NULL,policy_version int NOT NULL,manifest_sha text NOT NULL,
 purpose_sha text NOT NULL,expected_rows int NOT NULL CHECK(expected_rows BETWEEN 1 AND 100), author_ref text NOT NULL,
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,id),
 FOREIGN KEY(tenant_id,policy_version) REFERENCES dzoka.learning_policies(tenant_id,version)
);
CREATE TABLE dzoka.learning_cases (
 tenant_id uuid NOT NULL,dataset_id uuid NOT NULL,feedback_id uuid NOT NULL,adjudication_id uuid NOT NULL,
 item_sha text NOT NULL,candidate_item_sha text,split text NOT NULL CHECK(split IN ('TRAIN','VALIDATION','TEST')),
 PRIMARY KEY(tenant_id,dataset_id,feedback_id), FOREIGN KEY(tenant_id,dataset_id) REFERENCES dzoka.learning_datasets(tenant_id,id),
 FOREIGN KEY(tenant_id,adjudication_id) REFERENCES dzoka.learning_adjudications(tenant_id,id),
 FOREIGN KEY(tenant_id,feedback_id) REFERENCES dzoka.learning_feedback(tenant_id,id)
);
CREATE TABLE dzoka.learning_revocations (
 tenant_id uuid NOT NULL,kind text NOT NULL CHECK(kind IN ('DATASET','EXPERIMENT')),entity_id uuid NOT NULL,
 actor_ref text NOT NULL,evidence_sha text NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,kind,entity_id)
);
CREATE TABLE dzoka.learning_experiments (
 tenant_id uuid NOT NULL,id uuid NOT NULL,dataset_id uuid NOT NULL,rollout_id uuid NOT NULL,
 provider text NOT NULL,model_version text NOT NULL,model_sha text NOT NULL,config_sha text NOT NULL,evaluation_sha text NOT NULL,
 artifact jsonb NOT NULL,metrics jsonb NOT NULL,author_ref text NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id), FOREIGN KEY(tenant_id,dataset_id) REFERENCES dzoka.learning_datasets(tenant_id,id),
 FOREIGN KEY(tenant_id,rollout_id) REFERENCES dzoka.fingerprint_rollouts(tenant_id,id)
);
CREATE TABLE dzoka.learning_approvals (
 tenant_id uuid NOT NULL,experiment_id uuid NOT NULL,actor_ref text NOT NULL,evidence_sha text NOT NULL,
 minimum_coverage double precision NOT NULL,minimum_recall double precision NOT NULL,maximum_brier double precision NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(tenant_id,experiment_id),
 FOREIGN KEY(tenant_id,experiment_id) REFERENCES dzoka.learning_experiments(tenant_id,id)
);
CREATE TABLE dzoka.mlflow_exports (
 tenant_id uuid NOT NULL,experiment_id uuid NOT NULL,run_id text,model_version text,backend_sha text CHECK(backend_sha ~ '^[0-9a-f]{64}$'),state text NOT NULL DEFAULT 'PENDING'
 CHECK(state IN ('PENDING','COMPLETE')), PRIMARY KEY(tenant_id,experiment_id),
 FOREIGN KEY(tenant_id,experiment_id) REFERENCES dzoka.learning_experiments(tenant_id,id)
);
CREATE TABLE dzoka.restore_barriers (
 tenant_id uuid PRIMARY KEY REFERENCES dzoka.tenants(id),closed boolean NOT NULL,
 manifest_sha text,actor_ref text NOT NULL,evidence_sha text NOT NULL,updated_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE dzoka.erasure_journal (
 tenant_id uuid NOT NULL,report_id uuid NOT NULL,source_version bigint NOT NULL,recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,report_id)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['learning_policies','learning_feedback','learning_adjudications','learning_datasets','learning_cases',
  'learning_revocations','learning_experiments','learning_approvals','mlflow_exports','restore_barriers','erasure_journal'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY tenant_scope ON dzoka.%I AS RESTRICTIVE FOR ALL TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',t);
  EXECUTE format('CREATE POLICY learning_internal ON dzoka.%I TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',t);
 END LOOP;
 FOREACH t IN ARRAY ARRAY['learning_policies','learning_feedback','learning_adjudications','learning_datasets','learning_cases','learning_revocations','learning_experiments','learning_approvals','mlflow_exports'] LOOP
  EXECUTE format('CREATE POLICY learning_read ON dzoka.%I FOR SELECT TO dzoka_app USING(dzoka.permitted(''learning:manage'') OR dzoka.permitted(''learning:review'') OR dzoka.permitted(''learning:maintain''))',t);
  EXECUTE format('GRANT SELECT,INSERT ON dzoka.%I TO dzoka_app',t);
 END LOOP;
END $$;
CREATE POLICY policy_append ON dzoka.learning_policies FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:review') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY feedback_append ON dzoka.learning_feedback FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('feedback:submit') AND actor_ref=dzoka.current_actor_ref() AND kind IN('DECLARED_MATCH','DECLARED_REJECTION'));
CREATE POLICY feedback_own ON dzoka.learning_feedback FOR SELECT TO dzoka_app USING(dzoka.permitted('feedback:submit') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY adjudication_append ON dzoka.learning_adjudications FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:review') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY dataset_append ON dzoka.learning_datasets FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:manage') AND author_ref=dzoka.current_actor_ref());
CREATE POLICY case_append ON dzoka.learning_cases FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:manage'));
CREATE POLICY experiment_append ON dzoka.learning_experiments FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:manage') AND author_ref=dzoka.current_actor_ref());
CREATE POLICY approval_append ON dzoka.learning_approvals FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:review') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY revoke_append ON dzoka.learning_revocations FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:review') AND actor_ref=dzoka.current_actor_ref());
CREATE POLICY export_append ON dzoka.mlflow_exports FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('learning:manage'));
CREATE POLICY export_update ON dzoka.mlflow_exports FOR UPDATE TO dzoka_app USING(dzoka.permitted('learning:manage'));
GRANT UPDATE(run_id,model_version,backend_sha,state) ON dzoka.mlflow_exports TO dzoka_app;
CREATE POLICY barrier_visible ON dzoka.restore_barriers FOR SELECT TO dzoka_app USING(true);
CREATE POLICY barrier_control ON dzoka.restore_barriers FOR ALL TO dzoka_app USING(dzoka.permitted('retention:recover')) WITH CHECK(dzoka.permitted('retention:recover') AND actor_ref=dzoka.current_actor_ref());
GRANT SELECT,INSERT,UPDATE ON dzoka.restore_barriers TO dzoka_app;
CREATE POLICY erasure_read ON dzoka.erasure_journal FOR SELECT TO dzoka_app USING(dzoka.permitted('retention:recover') OR dzoka.permitted('operations:read'));
GRANT SELECT ON dzoka.erasure_journal TO dzoka_app;
CREATE POLICY erasure_replay ON dzoka.erasure_journal FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('retention:recover'));
GRANT INSERT ON dzoka.erasure_journal TO dzoka_app;
CREATE POLICY case_purge ON dzoka.learning_cases FOR DELETE TO dzoka_app USING(dzoka.permitted('learning:maintain') AND EXISTS(SELECT 1 FROM dzoka.learning_datasets d WHERE d.tenant_id=learning_cases.tenant_id AND d.id=dataset_id AND d.expires_at<=clock_timestamp()));
GRANT DELETE ON dzoka.learning_cases TO dzoka_app;

CREATE FUNCTION dzoka.learning_source_live(report uuid) RETURNS boolean LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=dzoka.current_tenant() AND r.id=report AND r.deleted_at IS NULL AND NOT r.legal_hold AND r.expires_at>clock_timestamp())
$$;
REVOKE ALL ON FUNCTION dzoka.learning_source_live(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_source_live(uuid) TO dzoka_app;
CREATE FUNCTION dzoka.learning_case_metadata(feedback uuid) RETURNS TABLE(item uuid,candidate_item uuid,observed_at timestamptz,recorded_at timestamptz)
LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT r.item_id,c.item_id,f.observed_at,f.recorded_at FROM dzoka.learning_feedback f JOIN dzoka.reports r ON(r.tenant_id,r.id)=(f.tenant_id,f.report_id)
 LEFT JOIN dzoka.reports c ON(c.tenant_id,c.id)=(f.tenant_id,f.candidate_id)
 WHERE f.tenant_id=dzoka.current_tenant() AND f.id=feedback AND (dzoka.permitted('learning:manage') OR dzoka.permitted('learning:review'))
 AND dzoka.learning_source_live(f.report_id) AND r.current_revision=f.source_revision AND(f.candidate_id IS NULL OR (dzoka.learning_source_live(f.candidate_id) AND c.current_revision=f.candidate_revision))
$$;
REVOKE ALL ON FUNCTION dzoka.learning_case_metadata(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_case_metadata(uuid) TO dzoka_app;
CREATE FUNCTION dzoka.capture_claim_feedback() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE report uuid; generation int; report_version bigint; report_revision int; kind text; digest text; previous_tenant text:=current_setting('dzoka.tenant_id',true);
BEGIN
 IF NEW.state NOT IN('APPROVED','REJECTED','RETURNED') OR (TG_OP='UPDATE' AND NEW.state=OLD.state) THEN RETURN NEW; END IF;
 PERFORM set_config('dzoka.tenant_id',NEW.tenant_id::text,true);
 SELECT i.report_id,i.generation,r.version,r.current_revision INTO report,generation,report_version,report_revision FROM dzoka.recovery_intakes i JOIN dzoka.reports r ON(r.tenant_id,r.id)=(i.tenant_id,i.report_id)
 WHERE i.tenant_id=NEW.tenant_id AND i.id=NEW.intake_id;
 kind:=CASE NEW.state WHEN 'APPROVED' THEN 'CLAIM_APPROVED' WHEN 'REJECTED' THEN 'CLAIM_REJECTED' ELSE 'RECOVERY_VERIFIED' END;
 digest:=encode(sha256(convert_to(NEW.id::text||':'||NEW.state||':'||generation||':'||report_version,'UTF8')),'hex');
 INSERT INTO dzoka.learning_feedback(tenant_id,id,report_id,source_id,source_revision,kind,source_sha,actor_ref,observed_at)
 VALUES(NEW.tenant_id,md5('claim-feedback:'||digest)::uuid,report,NEW.id,report_revision,kind,digest,NEW.actor_ref,clock_timestamp()) ON CONFLICT DO NOTHING;
 PERFORM set_config('dzoka.tenant_id',coalesce(previous_tenant,''),true);
 RETURN NEW;
END $$;
CREATE TRIGGER claim_feedback AFTER INSERT OR UPDATE OF state ON dzoka.ownership_claims FOR EACH ROW EXECUTE FUNCTION dzoka.capture_claim_feedback();
CREATE FUNCTION dzoka.capture_erasure() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE previous_tenant text:=current_setting('dzoka.tenant_id',true);
BEGIN
 IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
  PERFORM set_config('dzoka.tenant_id',NEW.tenant_id::text,true);
  INSERT INTO dzoka.erasure_journal(tenant_id,report_id,source_version) VALUES(NEW.tenant_id,NEW.id,NEW.version)
  ON CONFLICT(tenant_id,report_id) DO UPDATE SET source_version=greatest(erasure_journal.source_version,EXCLUDED.source_version);
  PERFORM set_config('dzoka.tenant_id',coalesce(previous_tenant,''),true);
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER report_erasure_journal AFTER UPDATE OF deleted_at ON dzoka.reports FOR EACH ROW EXECUTE FUNCTION dzoka.capture_erasure();
CREATE FUNCTION dzoka.learning_dataset_live(dataset uuid) RETURNS boolean LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM dzoka.learning_datasets d JOIN LATERAL(SELECT * FROM dzoka.learning_policies p WHERE p.tenant_id=d.tenant_id ORDER BY version DESC LIMIT 1) p ON true
 WHERE d.tenant_id=dzoka.current_tenant() AND d.id=dataset AND p.opt_in AND p.version=d.policy_version AND d.expires_at>clock_timestamp()
 AND NOT EXISTS(SELECT 1 FROM dzoka.learning_revocations v WHERE v.tenant_id=d.tenant_id AND v.kind='DATASET' AND v.entity_id=d.id)
 AND d.expected_rows=(SELECT count(*) FROM dzoka.learning_cases c WHERE c.tenant_id=d.tenant_id AND c.dataset_id=d.id)
 AND NOT EXISTS(SELECT 1 FROM dzoka.learning_cases c JOIN dzoka.learning_feedback f ON(f.tenant_id,f.id)=(c.tenant_id,c.feedback_id)
  JOIN dzoka.learning_adjudications a ON(a.tenant_id,a.id)=(c.tenant_id,c.adjudication_id)
  WHERE c.tenant_id=d.tenant_id AND c.dataset_id=d.id AND (NOT dzoka.learning_source_live(f.report_id)
   OR NOT EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=f.tenant_id AND r.id=f.report_id AND r.current_revision=f.source_revision)
   OR (f.candidate_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM dzoka.reports r WHERE r.tenant_id=f.tenant_id AND r.id=f.candidate_id AND r.current_revision=f.candidate_revision))
   OR (f.candidate_id IS NOT NULL AND NOT dzoka.learning_source_live(f.candidate_id))
   OR EXISTS(SELECT 1 FROM dzoka.learning_adjudications newer WHERE newer.tenant_id=a.tenant_id AND newer.feedback_id=a.feedback_id AND newer.revision>a.revision))))
$$;
REVOKE ALL ON FUNCTION dzoka.learning_dataset_live(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_dataset_live(uuid) TO dzoka_app;
CREATE POLICY governance_active_metadata ON dzoka.fingerprint_active FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY governance_rollout_metadata ON dzoka.fingerprint_rollouts FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE FUNCTION dzoka.learning_rollout_allowed(rollout uuid) RETURNS boolean LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT coalesce((SELECT NOT p.require_gate OR EXISTS(SELECT 1 FROM dzoka.learning_experiments e JOIN dzoka.learning_approvals a ON(a.tenant_id,a.experiment_id)=(e.tenant_id,e.id)
  JOIN dzoka.fingerprint_rollouts r ON(r.tenant_id,r.id)=(e.tenant_id,e.rollout_id)
  WHERE e.tenant_id=p.tenant_id AND e.rollout_id=rollout AND e.evaluation_sha=r.evaluation_sha256 AND a.actor_ref<>e.author_ref
   AND dzoka.learning_dataset_live(e.dataset_id) AND NOT EXISTS(SELECT 1 FROM dzoka.learning_revocations v WHERE v.tenant_id=e.tenant_id AND v.kind='EXPERIMENT' AND v.entity_id=e.id))
 FROM (SELECT * FROM dzoka.learning_policies WHERE tenant_id=dzoka.current_tenant() ORDER BY version DESC LIMIT 1) p),true)
$$;
REVOKE ALL ON FUNCTION dzoka.learning_rollout_allowed(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_rollout_allowed(uuid) TO dzoka_app;
CREATE FUNCTION dzoka.guard_governed_activation() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF NOT dzoka.learning_rollout_allowed(NEW.rollout_id) THEN RAISE EXCEPTION 'Learning approval unavailable' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER governed_activation BEFORE INSERT ON dzoka.fingerprint_activations FOR EACH ROW EXECUTE FUNCTION dzoka.guard_governed_activation();
CREATE FUNCTION dzoka.learning_gate_configured() RETURNS boolean LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT coalesce((SELECT require_gate FROM dzoka.learning_policies WHERE tenant_id=dzoka.current_tenant() ORDER BY version DESC LIMIT 1),false)
$$;
REVOKE ALL ON FUNCTION dzoka.learning_gate_configured() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_gate_configured() TO dzoka_app;
CREATE FUNCTION dzoka.learning_active_allowed() RETURNS boolean LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT NOT EXISTS(SELECT 1 FROM dzoka.fingerprint_active a WHERE a.tenant_id=dzoka.current_tenant() AND NOT dzoka.learning_rollout_allowed(a.rollout_id))
$$;
REVOKE ALL ON FUNCTION dzoka.learning_active_allowed() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_active_allowed() TO dzoka_app;
CREATE POLICY restore_tenant_scope ON dzoka.reports AS RESTRICTIVE TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY learning_report_recovery ON dzoka.reports FOR SELECT TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('retention:recover'));
CREATE POLICY learning_report_erase ON dzoka.reports FOR UPDATE TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('retention:recover')) WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('retention:recover'));
CREATE POLICY operational_metadata_ai ON dzoka.ai_jobs FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY operational_metadata_media ON dzoka.media_assets FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY operational_metadata_outbox ON dzoka.outbox FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY operational_metadata_fulfillment ON dzoka.fulfillment_operations FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY operational_metadata_events ON dzoka.fulfillment_events FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY operational_metadata_rewards ON dzoka.rewards FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY operational_metadata_shipments ON dzoka.shipments FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE FUNCTION dzoka.operations_summary() RETURNS jsonb LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT CASE WHEN dzoka.permitted('operations:read') THEN jsonb_build_object(
 'aiPending',(SELECT count(*) FROM dzoka.ai_jobs WHERE tenant_id=dzoka.current_tenant() AND state IN('QUEUED','PROCESSING')),
 'outboxPending',(SELECT count(*) FROM dzoka.outbox WHERE tenant_id=dzoka.current_tenant() AND published_at IS NULL),
 'externalUnknown',(SELECT count(*) FROM dzoka.fulfillment_operations WHERE tenant_id=dzoka.current_tenant() AND state IN('SENDING','UNKNOWN')),
 'externalBlocked',(SELECT count(*) FROM dzoka.fulfillment_operations WHERE tenant_id=dzoka.current_tenant() AND state='BLOCKED'),
 'restoreBlocked',coalesce((SELECT closed FROM dzoka.restore_barriers WHERE tenant_id=dzoka.current_tenant()),false),
 'modelEvidenceAvailable',dzoka.learning_active_allowed() AND (NOT EXISTS(SELECT 1 FROM dzoka.fingerprint_active WHERE tenant_id=dzoka.current_tenant()) OR dzoka.learning_gate_configured())) ELSE NULL END
$$;
REVOKE ALL ON FUNCTION dzoka.operations_summary() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.operations_summary() TO dzoka_app;
CREATE FUNCTION dzoka.institution_analytics(since_at timestamptz) RETURNS jsonb LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 WITH cohort AS (SELECT r.id,r.kind,r.created_at,v.category,v.latitude,v.longitude,v.uncertainty_meters,v.occurred_after,v.occurred_before FROM dzoka.reports r JOIN dzoka.report_revisions v ON(v.tenant_id,v.report_id,v.revision)=(r.tenant_id,r.id,r.current_revision) WHERE r.tenant_id=dzoka.current_tenant() AND r.created_at>=since_at
  AND r.deleted_at IS NULL AND NOT r.legal_hold AND r.expires_at>clock_timestamp()),
 intakes AS(SELECT i.id,i.report_id FROM dzoka.recovery_intakes i JOIN cohort c ON c.id=i.report_id WHERE i.tenant_id=dzoka.current_tenant()),
 returns AS(SELECT r.intake_id,r.created_at FROM dzoka.recovery_returns r JOIN intakes i ON i.id=r.intake_id WHERE r.tenant_id=dzoka.current_tenant()
 UNION SELECT c.intake_id,c.received_at FROM dzoka.courier_custody c JOIN intakes i ON i.id=c.intake_id WHERE c.tenant_id=dzoka.current_tenant() AND c.closed AND c.receipt_kind='OWNER_RETURN'),
 totals AS(SELECT (SELECT count(*) FROM cohort) reports,(SELECT count(*) FROM intakes) enrolled,(SELECT count(DISTINCT intake_id) FROM returns) recovered)
 SELECT CASE WHEN dzoka.permitted('analytics:read') THEN jsonb_build_object(
 'scope','CURRENT_AVAILABLE_REPORTS_IN_CREATED_COHORT','reports',reports,'enrolled',enrolled,'recovered',recovered,'backlog',enrolled-recovered,
 'verifiedRecoveryRate',CASE WHEN enrolled>=5 THEN recovered::numeric/enrolled ELSE NULL END,
 'commonCategories',coalesce((SELECT jsonb_agg(to_jsonb(g)) FROM (SELECT CASE WHEN category IN('BAG','PHONE','KEYS','WALLET','DOCUMENT','ELECTRONICS','CLOTHING') THEN category ELSE 'OTHER' END category,count(*) reports FROM cohort WHERE kind='LOST' GROUP BY 1 HAVING count(*)>=5 ORDER BY count(*) DESC,1 LIMIT 20) g),'[]'::jsonb),
 'commonLossAreas',coalesce((SELECT jsonb_agg(to_jsonb(g)) FROM (SELECT floor(latitude) latitude_degree,floor(longitude) longitude_degree,count(*) reports FROM cohort WHERE kind='LOST' AND latitude IS NOT NULL AND uncertainty_meters<=10000 GROUP BY 1,2 HAVING count(*)>=5 ORDER BY count(*) DESC,1,2 LIMIT 50) g),'[]'::jsonb),
 'commonLossUtcHours',coalesce((SELECT jsonb_agg(to_jsonb(g)) FROM (SELECT floor(extract(hour FROM occurred_after AT TIME ZONE 'UTC')/4)*4 start_hour,count(*) reports FROM cohort WHERE kind='LOST' AND occurred_after IS NOT NULL AND occurred_before IS NOT NULL AND occurred_before>=occurred_after AND floor(extract(epoch FROM occurred_before)/14400)=floor(extract(epoch FROM occurred_after)/14400) GROUP BY 1 HAVING count(*)>=5 ORDER BY 1) g),'[]'::jsonb),
 'completedRecoveryAverageSeconds',(SELECT CASE WHEN count(*)>=5 THEN avg(extract(epoch FROM r.created_at-c.created_at)) ELSE NULL END FROM returns r JOIN intakes i ON i.id=r.intake_id JOIN cohort c ON c.id=i.report_id),
 'federationMatchContribution',NULL,'fraudDetectionRate',NULL,'outcomeEvidenceStatus','ATTRIBUTED_INDEPENDENT_OUTCOMES_REQUIRED',
 'completedRecoveryMedianSeconds',(SELECT CASE WHEN count(*)>=5 THEN percentile_cont(0.5) WITHIN GROUP(ORDER BY extract(epoch FROM r.created_at-c.created_at)) ELSE NULL END FROM returns r JOIN intakes i ON i.id=r.intake_id JOIN cohort c ON c.id=i.report_id),
 'ownershipFalsePositiveRate',NULL,'ownershipSafetyStatus','INDEPENDENT_OUTCOME_AUDIT_REQUIRED',
 'courierUsage',(SELECT count(*) FROM dzoka.shipments s JOIN intakes i ON i.id=s.intake_id WHERE s.tenant_id=dzoka.current_tenant()),
 'rewardUsage',(SELECT count(*) FROM dzoka.rewards r JOIN intakes i ON i.id=r.intake_id WHERE r.tenant_id=dzoka.current_tenant()),'smallGroupMinimum',5)
 ELSE NULL END FROM totals
$$;
REVOKE ALL ON FUNCTION dzoka.institution_analytics(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.institution_analytics(timestamptz) TO dzoka_app;
CREATE POLICY restore_revision_metadata ON dzoka.report_revisions FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY restore_fingerprint_metadata ON dzoka.fingerprint_components FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE POLICY restore_perception_metadata ON dzoka.perception_components FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
CREATE FUNCTION dzoka.restore_erasure_pending() RETURNS bigint LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT CASE WHEN dzoka.permitted('retention:recover') THEN
 (SELECT count(*) FROM dzoka.reports r WHERE r.tenant_id=dzoka.current_tenant() AND r.deleted_at IS NOT NULL AND NOT r.legal_hold AND (
 EXISTS(SELECT 1 FROM dzoka.report_revisions v WHERE v.tenant_id=r.tenant_id AND v.report_id=r.id)
 OR EXISTS(SELECT 1 FROM dzoka.media_assets m WHERE m.tenant_id=r.tenant_id AND m.report_id=r.id AND (m.purge_pending OR NOT m.reservation_released))
 OR EXISTS(SELECT 1 FROM dzoka.fingerprint_components f WHERE f.tenant_id=r.tenant_id AND f.report_id=r.id)
 OR EXISTS(SELECT 1 FROM dzoka.perception_components p WHERE p.tenant_id=r.tenant_id AND p.report_id=r.id))) ELSE NULL END
$$;
REVOKE ALL ON FUNCTION dzoka.restore_erasure_pending() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.restore_erasure_pending() TO dzoka_app;

-- Institution policy is versioned separately from immutable source/history records.
ALTER TABLE dzoka.tenants ADD COLUMN report_daily_actor_limit int NOT NULL DEFAULT 100 CHECK(report_daily_actor_limit BETWEEN 1 AND 10000);
ALTER TABLE dzoka.tenants ADD COLUMN report_daily_tenant_limit int NOT NULL DEFAULT 1000 CHECK(report_daily_tenant_limit BETWEEN 1 AND 100000);
CREATE TABLE dzoka.operational_actions (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id),id uuid NOT NULL DEFAULT gen_random_uuid(),kind text NOT NULL,
 actor_ref text NOT NULL,evidence_sha text NOT NULL,detail jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp(),PRIMARY KEY(tenant_id,id)
);
CREATE TABLE dzoka.report_budgets (
 tenant_id uuid NOT NULL,actor_ref text NOT NULL,day date NOT NULL,used int NOT NULL CHECK(used>0),PRIMARY KEY(tenant_id,actor_ref,day)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['operational_actions','report_budgets'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE dzoka.%I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY tenant_scope ON dzoka.%I AS RESTRICTIVE TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',t);
  EXECUTE format('CREATE POLICY internal_scope ON dzoka.%I TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',t);
 END LOOP;
END $$;
CREATE POLICY operations_audit_read ON dzoka.operational_actions FOR SELECT TO dzoka_app USING(dzoka.permitted('operations:read'));
CREATE POLICY operations_audit_append ON dzoka.operational_actions FOR INSERT TO dzoka_app WITH CHECK(actor_ref=dzoka.current_actor_ref() AND (dzoka.permitted('retention:recover') OR dzoka.permitted('retention:manage')));
GRANT SELECT,INSERT ON dzoka.operational_actions TO dzoka_app;
CREATE POLICY retention_settings ON dzoka.tenants FOR UPDATE TO dzoka_app USING(id=dzoka.current_tenant() AND dzoka.permitted('retention:manage')) WITH CHECK(id=dzoka.current_tenant() AND dzoka.permitted('retention:manage'));
GRANT UPDATE(report_retention_days,report_daily_actor_limit,report_daily_tenant_limit) ON dzoka.tenants TO dzoka_app;
CREATE FUNCTION dzoka.consume_report_budget() RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE actor_limit int;tenant_limit int;used_actor int;used_tenant int;today date:=(clock_timestamp() AT TIME ZONE 'UTC')::date;
BEGIN
 IF NOT dzoka.permitted('reports:write') THEN RETURN false; END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended('report-budget:'||dzoka.current_tenant()::text,25));
 SELECT report_daily_actor_limit,report_daily_tenant_limit INTO actor_limit,tenant_limit FROM dzoka.tenants WHERE id=dzoka.current_tenant();
 SELECT used INTO used_actor FROM dzoka.report_budgets WHERE tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref() AND day=today;
 SELECT coalesce(sum(used),0) INTO used_tenant FROM dzoka.report_budgets WHERE tenant_id=dzoka.current_tenant() AND day=today;
 IF actor_limit IS NULL OR coalesce(used_actor,0)>=actor_limit OR used_tenant>=tenant_limit THEN RETURN false; END IF;
 INSERT INTO dzoka.report_budgets VALUES(dzoka.current_tenant(),dzoka.current_actor_ref(),today,1)
 ON CONFLICT(tenant_id,actor_ref,day) DO UPDATE SET used=report_budgets.used+1;
 DELETE FROM dzoka.report_budgets WHERE tenant_id=dzoka.current_tenant() AND day<today-2;
 RETURN true;
END $$;
REVOKE ALL ON FUNCTION dzoka.consume_report_budget() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.consume_report_budget() TO dzoka_app;
CREATE INDEX learning_feedback_age ON dzoka.learning_feedback(tenant_id,recorded_at DESC,id);
CREATE INDEX learning_latest_adjudication ON dzoka.learning_adjudications(tenant_id,feedback_id,revision DESC);
CREATE INDEX learning_dataset_expiry ON dzoka.learning_datasets(tenant_id,expires_at,id);
CREATE INDEX learning_experiment_rollout ON dzoka.learning_experiments(tenant_id,rollout_id);

ALTER TABLE dzoka.outbox ADD COLUMN traceparent text DEFAULT nullif(current_setting('dzoka.traceparent',true),'') CHECK(traceparent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]$');
ALTER TABLE dzoka.ai_jobs ADD COLUMN traceparent text DEFAULT nullif(current_setting('dzoka.traceparent',true),'') CHECK(traceparent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]$');
ALTER TABLE dzoka.perception_jobs ADD COLUMN traceparent text DEFAULT nullif(current_setting('dzoka.traceparent',true),'') CHECK(traceparent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]$');
ALTER TABLE dzoka.fulfillment_operations ADD COLUMN traceparent text DEFAULT nullif(current_setting('dzoka.traceparent',true),'') CHECK(traceparent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]$');

-- Safe job lifecycle diagnostics reuse canonical timestamps; they never copy job input or result bodies.
CREATE FUNCTION dzoka.finish_job_diagnostic() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF NEW.state IN('SUCCEEDED','FAILED','UNAVAILABLE','OBSOLETE','CANCELLED','CONFIRMED','REJECTED') THEN
  IF TG_OP='INSERT' OR OLD.state IS DISTINCT FROM NEW.state THEN NEW.diagnostic_finished_at:=clock_timestamp();NEW.diagnostic_exported:=false; END IF;
 ELSE NEW.diagnostic_finished_at:=NULL;NEW.diagnostic_exported:=false;
 END IF;
 RETURN NEW;
END $$;
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['ai_jobs','perception_jobs','fulfillment_operations'] LOOP
  EXECUTE format('ALTER TABLE dzoka.%I ADD COLUMN diagnostic_finished_at timestamptz, ADD COLUMN diagnostic_exported boolean NOT NULL DEFAULT false',t);
  EXECUTE format('CREATE TRIGGER diagnostic_finish BEFORE INSERT OR UPDATE OF state ON dzoka.%I FOR EACH ROW EXECUTE FUNCTION dzoka.finish_job_diagnostic()',t);
  EXECUTE format('CREATE POLICY diagnostic_metadata ON dzoka.%I TO CURRENT_USER USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant())',t);
 END LOOP;
END $$;
CREATE FUNCTION dzoka.export_job_diagnostics() RETURNS TABLE(parent text,task text,state text,started timestamptz,finished timestamptz)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF NOT dzoka.permitted('telemetry:export') THEN RETURN; END IF;
 RETURN QUERY WITH next AS (SELECT j.id FROM dzoka.ai_jobs j WHERE j.tenant_id=dzoka.current_tenant() AND NOT j.diagnostic_exported AND j.traceparent IS NOT NULL AND j.diagnostic_finished_at IS NOT NULL ORDER BY j.created_at,j.id LIMIT 50 FOR UPDATE SKIP LOCKED)
 UPDATE dzoka.ai_jobs j SET diagnostic_exported=true FROM next WHERE j.tenant_id=dzoka.current_tenant() AND j.id=next.id RETURNING j.traceparent,'AI_EMBEDDING'::text,j.state,j.created_at,j.diagnostic_finished_at;
 RETURN QUERY WITH next AS (SELECT j.id FROM dzoka.perception_jobs j WHERE j.tenant_id=dzoka.current_tenant() AND NOT j.diagnostic_exported AND j.traceparent IS NOT NULL AND j.diagnostic_finished_at IS NOT NULL ORDER BY j.created_at,j.id LIMIT 50 FOR UPDATE SKIP LOCKED)
 UPDATE dzoka.perception_jobs j SET diagnostic_exported=true FROM next WHERE j.tenant_id=dzoka.current_tenant() AND j.id=next.id RETURNING j.traceparent,'AI_PERCEPTION'::text,j.state,j.created_at,j.diagnostic_finished_at;
 RETURN QUERY WITH next AS (SELECT j.id FROM dzoka.fulfillment_operations j WHERE j.tenant_id=dzoka.current_tenant() AND NOT j.diagnostic_exported AND j.traceparent IS NOT NULL AND j.diagnostic_finished_at IS NOT NULL ORDER BY j.created_at,j.id LIMIT 50 FOR UPDATE SKIP LOCKED)
 UPDATE dzoka.fulfillment_operations j SET diagnostic_exported=true FROM next WHERE j.tenant_id=dzoka.current_tenant() AND j.id=next.id RETURNING j.traceparent,'EXTERNAL_OPERATION'::text,j.state,j.created_at,j.diagnostic_finished_at;
END $$;
REVOKE ALL ON FUNCTION dzoka.export_job_diagnostics() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.export_job_diagnostics() TO dzoka_app;
ALTER TABLE dzoka.recovery_activity ADD COLUMN traceparent text DEFAULT nullif(current_setting('dzoka.traceparent',true),'');
ALTER TABLE dzoka.match_search_runs ADD COLUMN traceparent text DEFAULT nullif(current_setting('dzoka.traceparent',true),'');



CREATE TABLE dzoka.outcome_audits (
 tenant_id uuid NOT NULL,id uuid NOT NULL,claim_id uuid NOT NULL,revision int NOT NULL CHECK(revision>0),source_sha text NOT NULL,
 owner_truth text NOT NULL CHECK(owner_truth IN('OWNER','NOT_OWNER','UNAVAILABLE')),
 fraud_finding text NOT NULL CHECK(fraud_finding IN('CONFIRMED_FRAUD','NO_FRAUD','UNAVAILABLE')),
 contribution text NOT NULL CHECK(contribution IN('LOCAL','FEDERATED','UNAVAILABLE')),
 evidence_sha text NOT NULL,auditor_ref text NOT NULL,artifact jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(tenant_id,id),UNIQUE(tenant_id,claim_id,revision),FOREIGN KEY(tenant_id,claim_id) REFERENCES dzoka.ownership_claims(tenant_id,id)
);
ALTER TABLE dzoka.outcome_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.outcome_audits FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_scope ON dzoka.outcome_audits AS RESTRICTIVE TO dzoka_app USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY outcome_read ON dzoka.outcome_audits FOR SELECT TO dzoka_app USING(dzoka.permitted('analytics:adjudicate'));
CREATE POLICY outcome_append ON dzoka.outcome_audits FOR INSERT TO dzoka_app WITH CHECK(dzoka.permitted('analytics:adjudicate') AND auditor_ref=dzoka.current_actor_ref());
CREATE POLICY outcome_internal ON dzoka.outcome_audits FOR SELECT TO CURRENT_USER USING(tenant_id=dzoka.current_tenant());
GRANT SELECT,INSERT ON dzoka.outcome_audits TO dzoka_app;
CREATE FUNCTION dzoka.outcome_source(claim uuid,reviewer text) RETURNS TABLE(source_sha text,decision text,independent boolean,report_id uuid)
LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT encode(sha256(convert_to(c.id::text||':'||r.version||':'||i.generation||':'||c.state||':'||coalesce((SELECT v.id::text FROM dzoka.recovery_reviews v WHERE v.tenant_id=c.tenant_id AND v.claim_id=c.id ORDER BY v.created_at DESC,v.id DESC LIMIT 1),''),'UTF8')),'hex'),
 c.state,c.actor_ref<>reviewer AND NOT EXISTS(SELECT 1 FROM dzoka.recovery_reviews v WHERE v.tenant_id=c.tenant_id AND v.claim_id=c.id AND v.actor_ref=reviewer),r.id
 FROM dzoka.ownership_claims c JOIN dzoka.recovery_intakes i ON(i.tenant_id,i.id)=(c.tenant_id,c.intake_id) JOIN dzoka.reports r ON(r.tenant_id,r.id)=(i.tenant_id,i.report_id)
 WHERE c.tenant_id=dzoka.current_tenant() AND c.id=claim AND c.state IN('APPROVED','REJECTED','RETURNED') AND r.deleted_at IS NULL AND NOT r.legal_hold AND r.expires_at>clock_timestamp()
$$;
REVOKE ALL ON FUNCTION dzoka.outcome_source(uuid,text) FROM PUBLIC;
CREATE FUNCTION dzoka.outcome_context(claim uuid) RETURNS jsonb LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT CASE WHEN dzoka.permitted('analytics:adjudicate') THEN (SELECT jsonb_build_object('sourceSha',source_sha,'decision',decision,'independent',independent) FROM dzoka.outcome_source(claim,dzoka.current_actor_ref())) ELSE NULL END
$$;
REVOKE ALL ON FUNCTION dzoka.outcome_context(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.outcome_context(uuid) TO dzoka_app;
CREATE FUNCTION dzoka.audited_outcome_metrics(since_at timestamptz) RETURNS jsonb LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 WITH latest AS(SELECT DISTINCT ON(claim_id) * FROM dzoka.outcome_audits WHERE tenant_id=dzoka.current_tenant() ORDER BY claim_id,revision DESC),
 valid AS(SELECT a.*,s.decision FROM latest a CROSS JOIN LATERAL dzoka.outcome_source(a.claim_id,a.auditor_ref) s JOIN dzoka.reports r ON r.tenant_id=a.tenant_id AND r.id=s.report_id
 WHERE a.source_sha=s.source_sha AND s.independent AND r.created_at>=since_at),
 totals AS(SELECT count(*) FILTER(WHERE owner_truth='NOT_OWNER') negatives,count(*) FILTER(WHERE owner_truth='NOT_OWNER' AND decision IN('APPROVED','RETURNED')) false_approvals,
 count(*) FILTER(WHERE fraud_finding<>'UNAVAILABLE') fraud_known,count(*) FILTER(WHERE fraud_finding='CONFIRMED_FRAUD') fraud_confirmed,
 count(*) FILTER(WHERE owner_truth='OWNER' AND decision='RETURNED' AND contribution<>'UNAVAILABLE') attributed_returns,
 count(*) FILTER(WHERE owner_truth='OWNER' AND decision='RETURNED' AND contribution='FEDERATED') federation_returns FROM valid)
 SELECT CASE WHEN dzoka.permitted('analytics:read') THEN jsonb_build_object(
 'independentlyAuditedClaims',(SELECT count(*) FROM valid),'ownershipFalsePositiveRate',CASE WHEN negatives>=5 THEN false_approvals::numeric/negatives ELSE NULL END,
 'adjudicatedFraudRate',CASE WHEN fraud_known>=5 THEN fraud_confirmed::numeric/fraud_known ELSE NULL END,
 'federationMatchContribution',CASE WHEN attributed_returns>=5 THEN federation_returns::numeric/attributed_returns ELSE NULL END,
 'outcomeEvidenceStatus','SOURCE_BOUND_INDEPENDENT_HUMAN_AUDIT','outcomeBasis','LATEST_CURRENT_DECISION_PER_CLAIM; AT_LEAST_FIVE_KNOWN_DENOMINATOR') ELSE NULL END FROM totals
$$;
REVOKE ALL ON FUNCTION dzoka.audited_outcome_metrics(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.audited_outcome_metrics(timestamptz) TO dzoka_app;

CREATE FUNCTION dzoka.learning_dataset_hold(dataset uuid) RETURNS boolean LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM dzoka.learning_cases c JOIN dzoka.learning_feedback f ON(f.tenant_id,f.id)=(c.tenant_id,c.feedback_id) JOIN dzoka.reports r ON r.tenant_id=f.tenant_id AND r.id IN(f.report_id,f.candidate_id)
 WHERE c.tenant_id=dzoka.current_tenant() AND c.dataset_id=dataset AND r.legal_hold)
$$;
REVOKE ALL ON FUNCTION dzoka.learning_dataset_hold(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.learning_dataset_hold(uuid) TO dzoka_app;
DROP POLICY case_purge ON dzoka.learning_cases;
CREATE POLICY case_purge ON dzoka.learning_cases FOR DELETE TO dzoka_app USING(dzoka.permitted('learning:maintain') AND NOT dzoka.learning_dataset_hold(dataset_id) AND EXISTS(SELECT 1 FROM dzoka.learning_datasets d WHERE d.tenant_id=learning_cases.tenant_id AND d.id=dataset_id AND d.expires_at<=clock_timestamp()));
