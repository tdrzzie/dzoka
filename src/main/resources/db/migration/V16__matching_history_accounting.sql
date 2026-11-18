CREATE TABLE dzoka.match_search_quota (
  tenant_id uuid NOT NULL, id uuid NOT NULL, actor_ref text NOT NULL,
  PRIMARY KEY(tenant_id,id),
  FOREIGN KEY(tenant_id,id) REFERENCES dzoka.match_search_runs(tenant_id,id) ON DELETE CASCADE
);
CREATE INDEX match_search_quota_actor ON dzoka.match_search_quota(tenant_id,actor_ref);

-- Backfill existing ledgers in this migration transaction under the table owner.
ALTER TABLE dzoka.match_search_runs ADD COLUMN client_id uuid;
ALTER TABLE dzoka.match_search_runs NO FORCE ROW LEVEL SECURITY;
UPDATE dzoka.match_search_runs SET client_id=id;
INSERT INTO dzoka.match_search_quota(tenant_id,id,actor_ref)
  SELECT tenant_id,id,actor_ref FROM dzoka.match_search_runs;
ALTER TABLE dzoka.match_search_runs FORCE ROW LEVEL SECURITY;
ALTER TABLE dzoka.match_search_runs ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE dzoka.match_search_runs ADD UNIQUE(tenant_id,actor_ref,client_id);
GRANT INSERT(client_id) ON dzoka.match_search_runs TO dzoka_app;

ALTER TABLE dzoka.match_search_quota ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.match_search_quota FORCE ROW LEVEL SECURITY;
CREATE POLICY match_quota_read ON dzoka.match_search_quota FOR SELECT TO dzoka_app USING(
  tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
  AND dzoka.permitted('matches:read') AND dzoka.permitted('reports:read'));
CREATE POLICY match_quota_insert ON dzoka.match_search_quota FOR INSERT TO dzoka_app WITH CHECK(
  tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
  AND dzoka.permitted('matches:read') AND dzoka.permitted('reports:read')
  AND EXISTS(SELECT 1 FROM dzoka.match_search_runs r WHERE (r.tenant_id,r.id,r.actor_ref)=
    (match_search_quota.tenant_id,match_search_quota.id,match_search_quota.actor_ref)));
GRANT SELECT,INSERT ON dzoka.match_search_quota TO dzoka_app;
