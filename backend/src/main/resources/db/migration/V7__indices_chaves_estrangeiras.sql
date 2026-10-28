-- Índices nas chaves estrangeiras usadas em joins e no carregamento de itens.
CREATE INDEX IF NOT EXISTS idx_venda_item_venda ON venda_item(id_venda);
CREATE INDEX IF NOT EXISTS idx_venda_item_produto ON venda_item(id_produto);
CREATE INDEX IF NOT EXISTS idx_compra_item_compra ON compra_item(id_compra);
CREATE INDEX IF NOT EXISTS idx_compra_item_produto ON compra_item(id_produto);
CREATE INDEX IF NOT EXISTS idx_ordem_item_ordem ON ordem_producao_item(id_ordem_producao);
CREATE INDEX IF NOT EXISTS idx_ordem_item_produto ON ordem_producao_item(id_produto_materia_prima);
CREATE INDEX IF NOT EXISTS idx_ordem_produto ON ordem_producao(id_produto_acabado);
CREATE INDEX IF NOT EXISTS idx_ordem_status ON ordem_producao(status);
CREATE INDEX IF NOT EXISTS idx_entrega_cliente ON entrega(id_cliente);
CREATE INDEX IF NOT EXISTS idx_entrega_status ON entrega(status);
CREATE INDEX IF NOT EXISTS idx_despesa_data ON despesa(data_despesa);
