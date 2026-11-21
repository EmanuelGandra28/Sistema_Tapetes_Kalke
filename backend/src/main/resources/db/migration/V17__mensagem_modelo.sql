-- Modelos das mensagens automáticas (WhatsApp). Os textos padrão e as variáveis de cada gatilho ficam no código (MensagemCatalogo);
-- a linha do gatilho é criada na primeira consulta. Aqui só ficam o texto escolhido pela loja, o interruptor e o histórico.
CREATE TABLE mensagem_modelo (
  id VARCHAR(38) PRIMARY KEY,
  chave VARCHAR(60) NOT NULL,
  texto TEXT NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT uq_mensagem_modelo_chave UNIQUE (chave)
);

-- Cada alteração guarda o texto ANTERIOR, quem alterou (created_by) e quando (created_at), para consultar e restaurar.
CREATE TABLE mensagem_modelo_historico (
  id VARCHAR(38) PRIMARY KEY,
  chave VARCHAR(60) NOT NULL,
  texto_anterior TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38)
);
CREATE INDEX idx_mensagem_historico_chave ON mensagem_modelo_historico(chave, created_at DESC);

-- Tabelas novas precisam do RLS ligado (a V8 só cobriu as antigas).
ALTER TABLE mensagem_modelo ENABLE ROW LEVEL SECURITY;
ALTER TABLE mensagem_modelo_historico ENABLE ROW LEVEL SECURITY;

INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo,'mensagens_gerenciar',c.codigo IN ('DONO','GERENTE') FROM cargo c
ON CONFLICT (cargo_codigo,acao) DO NOTHING;
