CREATE INDEX match_report_lexical ON dzoka.report_revisions
  USING gin (to_tsvector('simple'::regconfig,coalesce(normalized_text,'')));
CREATE INDEX match_report_latitude ON dzoka.report_revisions (tenant_id,latitude)
  WHERE latitude IS NOT NULL;
CREATE INDEX match_report_time ON dzoka.report_revisions
  USING gist (tstzrange(occurred_after,occurred_before,'[]'))
  WHERE occurred_after IS NOT NULL AND occurred_before IS NOT NULL;
