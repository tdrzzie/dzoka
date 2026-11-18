ALTER TABLE dzoka.report_revisions ADD COLUMN category text
  CHECK(category ~ '^[A-Z][A-Z0-9_]{0,47}$');
CREATE INDEX match_report_category ON dzoka.report_revisions(tenant_id,category,report_id,revision)
  WHERE category IS NOT NULL;
GRANT INSERT(category) ON dzoka.report_revisions TO dzoka_app;

ALTER TABLE dzoka.match_search_runs ADD COLUMN requested_category text
  CHECK(requested_category ~ '^[A-Z][A-Z0-9_]{0,47}$');
GRANT INSERT(requested_category) ON dzoka.match_search_runs TO dzoka_app;
