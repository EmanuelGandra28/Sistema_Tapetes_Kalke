-- Ficha técnica: quanto de cada matéria-prima vai em UMA unidade do produto acabado.
CREATE TABLE produto_componente (
  id VARCHAR(38) PRIMARY KEY,
  id_produto VARCHAR(38) NOT NULL,
  id_materia_prima VARCHAR(38) NOT NULL,
  quantidade NUMERIC(15,3) NOT NULL CHECK (quantidade > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_componente_X_produto FOREIGN KEY (id_produto) REFERENCES produto(id),
  CONSTRAINT fk_componente_X_materia FOREIGN KEY (id_materia_prima) REFERENCES produto(id),
  CONSTRAINT uq_componente UNIQUE (id_produto, id_materia_prima),
  CONSTRAINT ck_componente_nao_ele_mesmo CHECK (id_produto <> id_materia_prima)
);
CREATE INDEX idx_componente_produto ON produto_componente(id_produto);
CREATE INDEX idx_componente_materia ON produto_componente(id_materia_prima);
-- Mesmo bloqueio da V8: no Supabase, a API REST pública não enxerga a tabela.
ALTER TABLE produto_componente ENABLE ROW LEVEL SECURITY;

-- Ordens criadas pelo sistema quando o estoque comercial fica baixo ou esgota.
ALTER TABLE ordem_producao ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (origem IN ('MANUAL','AUTOMATICA'));
-- Acelera a checagem "já existe ordem aberta para este produto?".
CREATE INDEX idx_ordem_produto_status ON ordem_producao(id_produto_acabado, status);
