-- Fotos dos produtos. Ficam no próprio banco (o navegador já reduz cada foto antes de enviar, ~100-200 KB),
-- sem depender de outro serviço nem de chave extra. A leitura é por link público com id aleatório (o catálogo, o WhatsApp
-- e o futuro portfólio precisam abrir a imagem sem login); a escrita exige permissão.
CREATE TABLE produto_imagem (
  id VARCHAR(38) PRIMARY KEY,
  id_produto VARCHAR(38) NOT NULL,
  content_type VARCHAR(30) NOT NULL CHECK (content_type IN ('image/jpeg','image/png','image/webp')),
  dados BYTEA NOT NULL,
  tamanho INTEGER NOT NULL CHECK (tamanho > 0),
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  ordem INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT fk_produto_imagem_produto FOREIGN KEY (id_produto) REFERENCES produto(id)
);
CREATE INDEX idx_produto_imagem_produto ON produto_imagem(id_produto);
-- Só uma foto principal por produto.
CREATE UNIQUE INDEX uq_produto_imagem_principal ON produto_imagem(id_produto) WHERE principal;
-- Tabelas novas precisam do RLS ligado (a V8 só cobriu as antigas), senão ficam expostas pela API REST do Supabase.
ALTER TABLE produto_imagem ENABLE ROW LEVEL SECURITY;

INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo,'produtos_imagens',c.codigo IN ('DONO','GERENTE','ESTOQUISTA') FROM cargo c
ON CONFLICT (cargo_codigo,acao) DO NOTHING;
