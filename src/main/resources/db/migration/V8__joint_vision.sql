ALTER TABLE dzoka.perception_profiles DROP CONSTRAINT perception_profiles_task_check;
ALTER TABLE dzoka.perception_profiles ADD CHECK(task IN ('VISION','LOCAL_FEATURES','OCR','SPEECH','JOINT_IMAGE','JOINT_TEXT','DETECTION','SEGMENTATION'));
ALTER TABLE dzoka.perception_jobs DROP CONSTRAINT perception_jobs_input_type_check;
ALTER TABLE dzoka.perception_jobs DROP CONSTRAINT perception_jobs_check;
ALTER TABLE dzoka.perception_jobs ADD CHECK(input_type IN ('IMAGE_PNG','AUDIO_WAV','TEXT_UTF8'));
ALTER TABLE dzoka.perception_jobs ADD CHECK(
 (input_type='IMAGE_PNG' AND media_id IS NOT NULL AND width IS NOT NULL AND height IS NOT NULL AND width>0 AND height>0 AND samples IS NULL)
 OR (input_type='AUDIO_WAV' AND media_id IS NULL AND width IS NULL AND height IS NULL AND samples IS NOT NULL AND samples BETWEEN 400 AND 160000)
 OR (input_type='TEXT_UTF8' AND media_id IS NULL AND width IS NULL AND height IS NULL AND samples IS NULL AND input_bytes<=32000));
CREATE POLICY perception_leased_text ON dzoka.report_revisions FOR SELECT TO dzoka_app USING(
 tenant_id=dzoka.current_tenant() AND dzoka.permitted('ai:work') AND EXISTS(
 SELECT 1 FROM dzoka.perception_jobs j JOIN dzoka.reports r ON r.tenant_id=j.tenant_id AND r.id=j.report_id
 JOIN dzoka.perception_profiles p ON p.tenant_id=j.tenant_id AND p.id=j.profile_id AND p.enabled AND p.task='JOINT_TEXT'
 WHERE j.tenant_id=report_revisions.tenant_id AND j.report_id=report_revisions.report_id AND j.source_revision=report_revisions.revision
 AND j.input_type='TEXT_UTF8' AND j.state='PROCESSING' AND j.lease_owner=dzoka.current_actor_ref()
 AND j.lease_until>clock_timestamp() AND j.deadline>clock_timestamp() AND r.current_revision=j.source_revision
 AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()));
