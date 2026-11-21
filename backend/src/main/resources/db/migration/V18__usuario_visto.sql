-- O que cada usuário já viu no sistema (tutoriais por tela, novidades de cada versão...). Fica na conta, não só no navegador:
-- quem troca de computador ou celular não vê o mesmo tutorial de novo. A chave tem o formato "ajuda:vendas" ou "novidades:1.2".
CREATE TABLE usuario_visto (
  id VARCHAR(38) PRIMARY KEY,
  id_usuario VARCHAR(38) NOT NULL,
  chave VARCHAR(60) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(38), updated_by VARCHAR(38),
  CONSTRAINT fk_usuario_visto_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id) ON DELETE CASCADE,
  CONSTRAINT uq_usuario_visto UNIQUE (id_usuario, chave)
);
-- Tabelas novas precisam do RLS ligado (a V8 só cobriu as antigas).
ALTER TABLE usuario_visto ENABLE ROW LEVEL SECURITY;
