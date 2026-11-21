-- Portfólio (site público): o dono escolhe quais tapetes aparecem no site e como.
--   NENHUM  = fora do site (padrão); VENDA = aparece com preço e botão "adicionar ao pedido";
--   GALERIA = aparece só como foto na seção "Feitos com carinho" (sem preço e sem botão).
ALTER TABLE produto
  ADD COLUMN portfolio_modo VARCHAR(10) NOT NULL DEFAULT 'NENHUM' CHECK (portfolio_modo IN ('NENHUM','VENDA','GALERIA')),
  ADD COLUMN portfolio_linha VARCHAR(10) CHECK (portfolio_linha IN ('BANHEIRO','COZINHA','SALA','OUTRA')),
  ADD COLUMN portfolio_descricao VARCHAR(500),
  ADD COLUMN portfolio_ordem INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN portfolio_selo VARCHAR(20);

CREATE INDEX idx_produto_portfolio ON produto(portfolio_modo) WHERE portfolio_modo <> 'NENHUM';
