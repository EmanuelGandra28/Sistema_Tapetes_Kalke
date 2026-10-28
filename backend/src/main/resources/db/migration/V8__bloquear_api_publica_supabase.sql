-- No Supabase, as tabelas do schema public também ficam expostas pela API REST automática (PostgREST).
-- Ligar o RLS sem nenhuma policy bloqueia esse acesso; a API Java conecta como dona das tabelas e não é afetada.
-- Em um PostgreSQL comum este script não muda nada no funcionamento do sistema.
DO $$
DECLARE t record;
BEGIN
  -- flyway_schema_history fica de fora: o Flyway a mantém bloqueada enquanto esta migração roda.
  FOR t IN SELECT schemaname, tablename FROM pg_tables
           WHERE schemaname IN ('public', 'utilitarios') AND tablename <> 'flyway_schema_history' LOOP
    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', t.schemaname, t.tablename);
  END LOOP;
  -- Os papéis anon/authenticated só existem no Supabase; ali o histórico de migrações deixa de ser legível pela API REST.
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    EXECUTE 'REVOKE ALL ON public.flyway_schema_history FROM anon, authenticated';
  END IF;
END $$;
