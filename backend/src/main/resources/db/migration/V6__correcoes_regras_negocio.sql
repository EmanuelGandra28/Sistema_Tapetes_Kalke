-- Entrega de venda cancelada passa a ter status próprio em vez de continuar AGENDADA.
ALTER TABLE entrega DROP CONSTRAINT IF EXISTS entrega_status_check;
ALTER TABLE entrega ADD CONSTRAINT entrega_status_check CHECK (status IN ('AGENDADA','EM_ROTA','ENTREGUE','NAO_ENTREGUE','CANCELADA'));
INSERT INTO utilitarios.status_entrega(codigo,nome) VALUES ('CANCELADA','Cancelada') ON CONFLICT (codigo) DO NOTHING;

-- Incrementada no logout e na troca de senha: tokens emitidos antes deixam de valer.
ALTER TABLE usuario ADD COLUMN sessao_versao BIGINT NOT NULL DEFAULT 0;

-- Custo do produto no momento da venda, para o relatório calcular o custo da mercadoria vendida (CMV).
ALTER TABLE venda_item ADD COLUMN custo_unitario NUMERIC(15,2) CHECK (custo_unitario >= 0);
UPDATE venda_item vi SET custo_unitario = p.preco_custo FROM produto p WHERE p.id = vi.id_produto AND vi.custo_unitario IS NULL;

-- Novas ações: lançar despesas e cancelar ordem de produção.
INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo, a.acao, c.codigo IN ('DONO','GERENTE')
FROM cargo c CROSS JOIN (VALUES ('despesas_criar'),('producao_cancelar')) a(acao)
ON CONFLICT (cargo_codigo,acao) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_entrega_entregador ON entrega(id_entregador);
