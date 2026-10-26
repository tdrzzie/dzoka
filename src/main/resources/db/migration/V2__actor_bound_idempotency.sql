CREATE FUNCTION dzoka.current_actor_ref() RETURNS text LANGUAGE sql STABLE
  SET search_path = pg_catalog
  AS $$ SELECT encode(sha256(convert_to(dzoka.current_issuer(), 'UTF8')
    || decode('00','hex') || convert_to(dzoka.current_subject(), 'UTF8')), 'hex') $$;
REVOKE ALL ON FUNCTION dzoka.current_actor_ref() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dzoka.current_actor_ref() TO dzoka_app;

ALTER POLICY idempotency_scope ON dzoka.idempotency TO dzoka_app
  USING (tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
    AND dzoka.permitted('organizations:write'))
  WITH CHECK (tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
    AND dzoka.permitted('organizations:write'));
ALTER POLICY audit_append ON dzoka.audit_events TO dzoka_app
  WITH CHECK (tenant_id=dzoka.current_tenant() AND actor_ref=dzoka.current_actor_ref()
    AND dzoka.permitted('organizations:write'));
