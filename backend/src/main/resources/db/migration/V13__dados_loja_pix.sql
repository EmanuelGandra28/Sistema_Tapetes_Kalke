-- Dados da loja para gerar o QR Code PIX (estático) nas vendas. A tabela configuracao existia sem nenhuma linha;
-- passa a ter uma linha única, de id fixo LOJA. Os valores são preenchidos pelo dono em Configurações.
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS pix_chave VARCHAR(77);
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS pix_nome_recebedor VARCHAR(25);
ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS pix_cidade VARCHAR(15);
INSERT INTO configuracao(id) VALUES ('LOJA') ON CONFLICT (id) DO NOTHING;
