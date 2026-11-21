-- Alerta de produção para a dona pelo WhatsApp: número, interruptor e a evidência do consentimento ficam na linha única da loja.
-- A evidência é montada com o que realmente aconteceu (quem ativou, quando e para qual número), nunca inventada.
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS whatsapp_dona VARCHAR(20);
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS alerta_producao_ativo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS alerta_evidencia TEXT;
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS alerta_ativado_em TIMESTAMPTZ;

-- Fila de avisos: cada ordem automática vira uma linha PENDENTE; a rotina agrupa todas as pendentes numa única mensagem
-- (e envia só no horário comercial). A ordem é única para não avisar duas vezes sobre a mesma produção.
CREATE TABLE alerta_producao (
  id VARCHAR(38) PRIMARY KEY,
  id_ordem VARCHAR(38) NOT NULL,
  id_produto VARCHAR(38) NOT NULL,
  produto_nome VARCHAR(180) NOT NULL,
  quantidade NUMERIC(15,3) NOT NULL,
  saldo NUMERIC(15,3) NOT NULL,
  estoque_minimo NUMERIC(15,3) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','ENVIADO','FALHOU')),
  tentativas INTEGER NOT NULL DEFAULT 0,
  erro_codigo VARCHAR(60),
  lote VARCHAR(38),
  enviado_em TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT fk_alerta_producao_ordem FOREIGN KEY (id_ordem) REFERENCES ordem_producao(id),
  CONSTRAINT uq_alerta_producao_ordem UNIQUE (id_ordem)
);
CREATE INDEX idx_alerta_producao_status ON alerta_producao(status, created_at);
-- Tabelas novas precisam do RLS ligado (a V8 só cobriu as antigas).
ALTER TABLE alerta_producao ENABLE ROW LEVEL SECURITY;
