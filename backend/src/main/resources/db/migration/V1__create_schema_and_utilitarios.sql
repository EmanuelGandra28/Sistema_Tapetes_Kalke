CREATE SCHEMA IF NOT EXISTS utilitarios;

CREATE TABLE utilitarios.unidade_medida (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.tipo_produto (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.forma_pagamento (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.tipo_despesa (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.grupo_estoque (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.status_cliente (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.status_funcionario (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.status_venda (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.status_entrega (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.tipo_ordem_producao (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.status_ordem_producao (id SERIAL PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);
CREATE TABLE utilitarios.tipo_movimento_estoque (id SERIAL PRIMARY KEY, codigo VARCHAR(40) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL);

INSERT INTO utilitarios.unidade_medida(codigo,nome) VALUES ('UNIDADE','Unidade'),('METRO','Metro'),('KG','Quilograma');
INSERT INTO utilitarios.tipo_produto(codigo,nome) VALUES ('TAPETE','Tapete'),('MALHA','Malha');
INSERT INTO utilitarios.forma_pagamento(codigo,nome) VALUES ('PIX','PIX'),('DINHEIRO','Dinheiro'),('CARTAO_CREDITO','Cartão de crédito'),('CARTAO_DEBITO','Cartão de débito'),('CHEQUE','Cheque');
INSERT INTO utilitarios.tipo_despesa(codigo,nome) VALUES ('SALARIOS','Salários'),('FRETES','Fretes'),('MANUTENCAO','Manutenção'),('OUTROS','Outros');
INSERT INTO utilitarios.grupo_estoque(codigo,nome) VALUES ('COMERCIAL','Comercial'),('PRODUCAO','Produção');
INSERT INTO utilitarios.status_cliente(codigo,nome) VALUES ('ATIVO','Ativo'),('INATIVO','Inativo');
INSERT INTO utilitarios.status_funcionario(codigo,nome) VALUES ('ATIVO','Ativo'),('INATIVO','Inativo'),('FERIAS','Férias');
INSERT INTO utilitarios.status_venda(codigo,nome) VALUES ('FINALIZADA','Finalizada'),('CANCELADA','Cancelada');
INSERT INTO utilitarios.status_entrega(codigo,nome) VALUES ('AGENDADA','Agendada'),('EM_ROTA','Em rota'),('ENTREGUE','Entregue'),('NAO_ENTREGUE','Não entregue');
INSERT INTO utilitarios.tipo_ordem_producao(codigo,nome) VALUES ('ESTOQUE','Estoque'),('ENCOMENDA','Encomenda');
INSERT INTO utilitarios.status_ordem_producao(codigo,nome) VALUES ('ABERTA','Aberta'),('FINALIZADA','Finalizada'),('CANCELADA','Cancelada');
INSERT INTO utilitarios.tipo_movimento_estoque(codigo,nome) VALUES ('ENTRADA_COMPRA','Entrada de compra'),('SAIDA_VENDA','Saída de venda'),('CONSUMO_PRODUCAO','Consumo de produção'),('ENTRADA_PRODUCAO','Entrada de produção'),('AJUSTE_MANUAL','Ajuste manual'),('ESTORNO_VENDA','Estorno de venda'),('ESTORNO_COMPRA','Estorno de compra');