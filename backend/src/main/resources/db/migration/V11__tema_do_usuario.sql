-- Tema visual escolhido por cada usuário em Configurações.
ALTER TABLE usuario ADD COLUMN tema VARCHAR(20) NOT NULL DEFAULT 'verde'
  CHECK (tema IN ('verde','escuro','areia','marinho','contraste'));
