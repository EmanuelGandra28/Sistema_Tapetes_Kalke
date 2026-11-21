-- Reativação de clientes pelo WhatsApp: consentimento (LGPD), campanhas com até 3 mensagens, mensagens enviadas e eventos do gateway.

-- Consentimento por cliente e finalidade. A evidência é sempre escrita por uma pessoa (como o cliente autorizou); o sistema nunca a inventa.
CREATE TABLE cliente_consentimento (
  id VARCHAR(38) PRIMARY KEY,
  id_cliente VARCHAR(38) NOT NULL,
  finalidade VARCHAR(60) NOT NULL,
  evidencia TEXT NOT NULL CHECK (char_length(evidencia) >= 10),
  origem VARCHAR(40) NOT NULL,
  concedido_em TIMESTAMPTZ NOT NULL,
  revogado_em TIMESTAMPTZ,
  motivo_revogacao VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT fk_consentimento_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id)
);
-- Um consentimento vigente por cliente e finalidade (os revogados ficam como histórico).
CREATE UNIQUE INDEX uq_consentimento_vigente ON cliente_consentimento(id_cliente, finalidade) WHERE revogado_em IS NULL;

CREATE TABLE campanha_reativacao (
  id VARCHAR(38) PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO' CHECK (status IN ('RASCUNHO','APROVADA','PAUSADA','CONCLUIDA','CANCELADA')),
  intervalo_dias INTEGER NOT NULL DEFAULT 5 CHECK (intervalo_dias BETWEEN 1 AND 60),
  limite_diario INTEGER NOT NULL DEFAULT 20 CHECK (limite_diario BETWEEN 1 AND 200),
  aprovada_por VARCHAR(38),
  aprovada_em TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38)
);

CREATE TABLE campanha_destinatario (
  id VARCHAR(38) PRIMARY KEY,
  id_campanha VARCHAR(38) NOT NULL,
  id_cliente VARCHAR(38) NOT NULL,
  etapa_atual INTEGER NOT NULL DEFAULT 0 CHECK (etapa_atual BETWEEN 0 AND 3),
  status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO','CONCLUIDO','RESPONDEU','SAIU','FALHOU','CANCELADO','RECOMPROU')),
  proxima_etapa_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  primeiro_envio_em TIMESTAMPTZ,
  ultimo_envio_em TIMESTAMPTZ,
  resposta_classe VARCHAR(20) CHECK (resposta_classe IN ('INTERESSADO','SEM_INTERESSE','PEDIU_SAIR','OUTRO')),
  resposta_texto VARCHAR(500),
  resposta_em TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT fk_destinatario_campanha FOREIGN KEY (id_campanha) REFERENCES campanha_reativacao(id),
  CONSTRAINT fk_destinatario_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id),
  CONSTRAINT uq_destinatario UNIQUE (id_campanha, id_cliente)
);
CREATE INDEX idx_destinatario_fila ON campanha_destinatario(status, proxima_etapa_em);

-- Cada mensagem enviada ao gateway: a chave de idempotência impede envio em duplicidade e a contagem do mês controla a cota.
CREATE TABLE whatsapp_mensagem (
  id VARCHAR(38) PRIMARY KEY,
  id_destinatario VARCHAR(38),
  telefone VARCHAR(20) NOT NULL,
  etapa INTEGER NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL UNIQUE,
  mensagem_gateway VARCHAR(80),
  situacao VARCHAR(20) NOT NULL DEFAULT 'na_fila',
  erro_codigo VARCHAR(60),
  enviado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT fk_whatsapp_mensagem_destinatario FOREIGN KEY (id_destinatario) REFERENCES campanha_destinatario(id)
);
CREATE INDEX idx_whatsapp_mensagem_enviado ON whatsapp_mensagem(enviado_em);
CREATE INDEX idx_whatsapp_mensagem_gateway ON whatsapp_mensagem(mensagem_gateway);

-- Eventos recebidos por webhook; o evento_id único faz a entrega "ao menos uma vez" do gateway não repetir efeitos.
CREATE TABLE whatsapp_evento (
  id VARCHAR(38) PRIMARY KEY,
  evento_id VARCHAR(80) NOT NULL UNIQUE,
  evento VARCHAR(40) NOT NULL,
  recebido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38)
);

-- Tabelas novas precisam do RLS ligado (a V8 só cobriu as antigas).
ALTER TABLE cliente_consentimento ENABLE ROW LEVEL SECURITY;
ALTER TABLE campanha_reativacao ENABLE ROW LEVEL SECURITY;
ALTER TABLE campanha_destinatario ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_mensagem ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_evento ENABLE ROW LEVEL SECURITY;

INSERT INTO permissao_pagina(cargo_codigo,pagina,permitido)
SELECT c.codigo,'reativacao',c.codigo IN ('DONO','GERENTE') FROM cargo c
ON CONFLICT (cargo_codigo,pagina) DO NOTHING;

INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo,a.acao,CASE WHEN a.acao='clientes_consentimento' THEN c.codigo IN ('DONO','GERENTE','VENDEDOR') ELSE c.codigo IN ('DONO','GERENTE') END
FROM cargo c CROSS JOIN (VALUES ('reativacao_gerenciar'),('clientes_consentimento')) a(acao)
ON CONFLICT (cargo_codigo,acao) DO NOTHING;
