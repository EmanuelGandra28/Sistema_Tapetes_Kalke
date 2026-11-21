-- Despesas passam a poder ser editadas e excluídas (exclusão lógica, com motivo, para manter a auditoria).
ALTER TABLE despesa ADD COLUMN excluida_em TIMESTAMPTZ;
ALTER TABLE despesa ADD COLUMN excluida_por VARCHAR(38);
ALTER TABLE despesa ADD COLUMN motivo_exclusao VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_despesa_excluida ON despesa(excluida_em);

INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo,a.acao,c.codigo IN ('DONO','GERENTE') FROM cargo c CROSS JOIN (VALUES ('despesas_editar'),('despesas_excluir')) a(acao)
ON CONFLICT (cargo_codigo,acao) DO NOTHING;
