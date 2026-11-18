-- Apply after Flyway migrations with the approved database owner. Run with ON_ERROR_STOP.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace
                 WHERE e.extname='vector' AND e.extversion='0.8.7' AND n.nspname='public') THEN
    RAISE EXCEPTION 'Expected reviewed public.vector 0.8.7';
  END IF;
END $$;
-- An expression index is maintained by PostgreSQL in the same transaction as the authoritative components.
CREATE INDEX CONCURRENTLY matching_text_hnsw_384 ON dzoka.fingerprint_components
USING hnsw ((vector::public.vector(384)) public.vector_cosine_ops)
WHERE cardinality(vector)=384 AND NOT truncated;
