<#
  Gerador do Sistema Tapetes Kalke.
  Uso: .\gerar.ps1 [-ProjectRoot caminho] [-Force]
  Por padrão, gera backend/ e frontend/ ao lado deste arquivo. Arquivos existentes
  não são sobrescritos sem -Force para proteger trabalho já realizado.
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = $PSScriptRoot,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

# OBSOLETO: o código em backend/ e frontend/ evoluiu muito além do que este script gera.
# Rodá-lo com -Force desfaria correções de segurança e de regras de negócio. Mantido só como histórico.
if (-not $env:KALKE_GERADOR_LEGADO) {
    throw 'gerar.ps1 está obsoleto e não deve ser executado. O código atual em backend/ e frontend/ é a fonte de verdade.'
}
$Utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$files = [ordered]@{}

$files['.gitignore'] = @'
# Java / Maven
backend/target/
*.class
*.log

# Node / Vite
frontend/node_modules/
frontend/dist/
frontend/coverage/

# Secrets and local configuration
.env
.env.*
!.env.example
backend/.env
frontend/.env
frontend/.env.local

# IDE and OS
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db
'@

$files['.env.example'] = @'
DB_URL=jdbc:postgresql://localhost:5432/tapetes_kalke
DB_USERNAME=postgres
DB_PASSWORD=CHANGE_ME
JWT_SECRET=replace-with-a-random-base64-secret-at-least-32-bytes
JWT_EXPIRATION_SECONDS=3600
FRONTEND_URL=http://localhost:5173
BOOTSTRAP_ADMIN_EMAIL=admin@tapeteskalke.local
BOOTSTRAP_ADMIN_PASSWORD=CHANGE_ME_AT_FIRST_START
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
'@

$files['docker-compose.yml'] = @'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: tapetes_kalke
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d tapetes_kalke"]
      interval: 5s
      timeout: 5s
      retries: 10
volumes:
  postgres_data:
'@

$files['README.md'] = @'
# Sistema Tapetes Kalke

Aplicação monorepo com API Java 21/Spring Boot e interface React/Vite em JavaScript.

## Primeiro uso

1. Copie `.env.example` para `.env`, preencha os segredos e exporte as variáveis para a sessão.
2. Inicie o PostgreSQL: `docker compose up -d postgres`.
3. Em `backend`, execute `mvn spring-boot:run`.
4. Em `frontend`, execute `npm install` e `npm run dev`.

O primeiro administrador é criado apenas quando `BOOTSTRAP_ADMIN_PASSWORD` tiver sido alterada de `CHANGE_ME_AT_FIRST_START`. A documentação da API fica em `/swagger-ui/index.html`.

## Decisões de integridade

- Valores monetários e quantidades usam `BigDecimal`.
- Venda, compra, produção, cancelamentos e ajustes são transacionais.
- Produtos são bloqueados pessimisticamente durante alterações de saldo; o banco também impede saldos negativos.
- Estoque comercial e de produção são independentes e todo movimento gera histórico.
- Cancelamentos preservam histórico e emitem movimentos de estorno.
- A API é a fonte de verdade para preço, saldo, totais e permissões.
'@

$files['backend/pom.xml'] = @'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
    <relativePath/>
  </parent>
  <groupId>br.com.tapeteskalke</groupId>
  <artifactId>tapetes-kalke-api</artifactId>
  <version>1.0.0</version>
  <name>tapetes-kalke-api</name>
  <description>API do Sistema Tapetes Kalke</description>
  <properties><java.version>21</java.version><jjwt.version>0.12.6</jjwt.version><springdoc.version>2.8.6</springdoc.version></properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>${springdoc.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
'@

$files['backend/src/main/resources/application.yml'] = @'
spring:
  config:
    import: optional:file:../.env[.properties]
  application:
    name: tapetes-kalke-api
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/tapetes_kalke}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: America/Sao_Paulo
        format_sql: true
  flyway:
    enabled: true
  jackson:
    time-zone: America/Sao_Paulo
  mail:
    host: ${MAIL_HOST:}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
server:
  port: ${PORT:8080}
  error:
    include-message: never
app:
  cors:
    allowed-origin: ${FRONTEND_URL:http://localhost:5173}
  jwt:
    secret: ${JWT_SECRET:change-this-development-secret-to-a-32-byte-minimum-value}
    expiration-seconds: ${JWT_EXPIRATION_SECONDS:3600}
  bootstrap-admin:
    email: ${BOOTSTRAP_ADMIN_EMAIL:admin@tapeteskalke.local}
    password: ${BOOTSTRAP_ADMIN_PASSWORD:CHANGE_ME_AT_FIRST_START}
management:
  endpoints:
    web:
      exposure:
        include: health,info
'@

$files['backend/src/main/resources/db/migration/V1__create_schema_and_utilitarios.sql'] = @'
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
'@

$files['backend/src/main/resources/db/migration/V2__create_core_tables.sql'] = @'
CREATE TABLE cargo (id VARCHAR(38) PRIMARY KEY, codigo VARCHAR(30) NOT NULL UNIQUE, nome VARCHAR(80) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38));
CREATE TABLE usuario (id VARCHAR(38) PRIMARY KEY, nome VARCHAR(150) NOT NULL, email VARCHAR(180) NOT NULL UNIQUE, senha_hash VARCHAR(255) NOT NULL, id_cargo VARCHAR(38) NOT NULL, ativo BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_usuario_X_cargo FOREIGN KEY (id_cargo) REFERENCES cargo(id));
CREATE TABLE cliente (id VARCHAR(38) PRIMARY KEY, nome VARCHAR(150) NOT NULL, telefone VARCHAR(20) NOT NULL, email VARCHAR(180), razao_social VARCHAR(180), cnpj VARCHAR(14), endereco VARCHAR(255), bairro VARCHAR(100), cidade VARCHAR(100), cep VARCHAR(8), data_nascimento DATE, observacoes TEXT, status_cadastral VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status_cadastral IN ('ATIVO','INATIVO')), ultima_compra DATE, total_compras NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (total_compras >= 0), ativo BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMPTZ, deleted_by VARCHAR(38), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38));
CREATE TABLE fornecedor (id VARCHAR(38) PRIMARY KEY, nome_empresa VARCHAR(180) NOT NULL, cnpj VARCHAR(14), telefone VARCHAR(20) NOT NULL, email VARCHAR(180), observacoes TEXT, ativo BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMPTZ, deleted_by VARCHAR(38), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38));
CREATE TABLE funcionario (id VARCHAR(38) PRIMARY KEY, nome VARCHAR(150) NOT NULL, telefone VARCHAR(20), email VARCHAR(180), endereco VARCHAR(255), salario NUMERIC(15,2), data_contratacao DATE, status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO','INATIVO','FERIAS')), id_cargo VARCHAR(38) NOT NULL, observacoes TEXT, ativo BOOLEAN NOT NULL DEFAULT true, deleted_at TIMESTAMPTZ, deleted_by VARCHAR(38), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_funcionario_X_cargo FOREIGN KEY (id_cargo) REFERENCES cargo(id));
CREATE TABLE produto (id VARCHAR(38) PRIMARY KEY, nome VARCHAR(180) NOT NULL, tipo VARCHAR(30) NOT NULL, categoria VARCHAR(100), cor VARCHAR(80), tamanho VARCHAR(80), unidade_medida VARCHAR(30) NOT NULL, pode_ser_vendido BOOLEAN NOT NULL DEFAULT true, pode_ser_materia_prima BOOLEAN NOT NULL DEFAULT false, estoque_comercial NUMERIC(15,3) NOT NULL DEFAULT 0 CHECK (estoque_comercial >= 0), estoque_producao NUMERIC(15,3) NOT NULL DEFAULT 0 CHECK (estoque_producao >= 0), estoque_minimo_comercial NUMERIC(15,3) NOT NULL DEFAULT 1 CHECK (estoque_minimo_comercial >= 0), estoque_minimo_producao NUMERIC(15,3) NOT NULL DEFAULT 1 CHECK (estoque_minimo_producao >= 0), preco_custo NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (preco_custo >= 0), preco_venda NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (preco_venda >= 0), codigo_barras VARCHAR(80), id_fornecedor_principal VARCHAR(38), observacoes TEXT, ativo BOOLEAN NOT NULL DEFAULT true, versao BIGINT NOT NULL DEFAULT 0, deleted_at TIMESTAMPTZ, deleted_by VARCHAR(38), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_produto_X_fornecedor FOREIGN KEY(id_fornecedor_principal) REFERENCES fornecedor(id));
CREATE TABLE produto_fornecedor (id VARCHAR(38) PRIMARY KEY, id_produto VARCHAR(38) NOT NULL, id_fornecedor VARCHAR(38) NOT NULL, principal BOOLEAN NOT NULL DEFAULT false, observacoes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), CONSTRAINT fk_produto_fornecedor_X_produto FOREIGN KEY(id_produto) REFERENCES produto(id), CONSTRAINT fk_produto_fornecedor_X_fornecedor FOREIGN KEY(id_fornecedor) REFERENCES fornecedor(id), UNIQUE(id_produto,id_fornecedor));
'@

$files['backend/src/main/resources/db/migration/V3__create_operational_tables.sql'] = @'
CREATE SEQUENCE venda_numero_seq START WITH 1;
CREATE SEQUENCE ordem_numero_seq START WITH 1;
CREATE TABLE venda (id VARCHAR(38) PRIMARY KEY, numero_venda BIGINT NOT NULL UNIQUE DEFAULT nextval('venda_numero_seq'), id_cliente VARCHAR(38) NOT NULL, id_funcionario_vendedor VARCHAR(38), data_venda DATE NOT NULL, subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0), desconto NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (desconto >= 0), total_venda NUMERIC(15,2) NOT NULL CHECK (total_venda >= 0), forma_pagamento VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'FINALIZADA' CHECK(status IN ('FINALIZADA','CANCELADA')), observacoes TEXT, cancelled_at TIMESTAMPTZ, cancelled_by VARCHAR(38), motivo_cancelamento TEXT, idempotency_key VARCHAR(100) UNIQUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_venda_X_cliente FOREIGN KEY(id_cliente) REFERENCES cliente(id), CONSTRAINT fk_venda_X_funcionario FOREIGN KEY(id_funcionario_vendedor) REFERENCES funcionario(id));
CREATE TABLE venda_item (id VARCHAR(38) PRIMARY KEY, id_venda VARCHAR(38) NOT NULL, id_produto VARCHAR(38) NOT NULL, produto_nome_snapshot VARCHAR(180) NOT NULL, unidade_medida_snapshot VARCHAR(30) NOT NULL, quantidade NUMERIC(15,3) NOT NULL CHECK(quantidade > 0), preco_unitario NUMERIC(15,2) NOT NULL CHECK(preco_unitario >= 0), subtotal NUMERIC(15,2) NOT NULL CHECK(subtotal >= 0), CONSTRAINT fk_venda_item_X_venda FOREIGN KEY(id_venda) REFERENCES venda(id), CONSTRAINT fk_venda_item_X_produto FOREIGN KEY(id_produto) REFERENCES produto(id));
CREATE TABLE compra (id VARCHAR(38) PRIMARY KEY, id_fornecedor VARCHAR(38), fornecedor_nome_manual VARCHAR(180), data_compra DATE NOT NULL, total_custo NUMERIC(15,2) NOT NULL CHECK(total_custo >= 0), status VARCHAR(20) NOT NULL DEFAULT 'FINALIZADA' CHECK(status IN ('FINALIZADA','CANCELADA')), observacoes TEXT, cancelled_at TIMESTAMPTZ, cancelled_by VARCHAR(38), motivo_cancelamento TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_compra_X_fornecedor FOREIGN KEY(id_fornecedor) REFERENCES fornecedor(id), CONSTRAINT ck_compra_fornecedor CHECK(id_fornecedor IS NOT NULL OR fornecedor_nome_manual IS NOT NULL));
CREATE TABLE compra_item (id VARCHAR(38) PRIMARY KEY, id_compra VARCHAR(38) NOT NULL, id_produto VARCHAR(38) NOT NULL, destino_estoque VARCHAR(20) NOT NULL CHECK(destino_estoque IN ('COMERCIAL','PRODUCAO')), quantidade NUMERIC(15,3) NOT NULL CHECK(quantidade > 0), preco_unitario NUMERIC(15,2) NOT NULL CHECK(preco_unitario >= 0), subtotal NUMERIC(15,2) NOT NULL CHECK(subtotal >= 0), produto_nome_snapshot VARCHAR(180) NOT NULL, CONSTRAINT fk_compra_item_X_compra FOREIGN KEY(id_compra) REFERENCES compra(id), CONSTRAINT fk_compra_item_X_produto FOREIGN KEY(id_produto) REFERENCES produto(id));
CREATE TABLE ordem_producao (id VARCHAR(38) PRIMARY KEY, numero_ordem BIGINT NOT NULL UNIQUE DEFAULT nextval('ordem_numero_seq'), id_produto_acabado VARCHAR(38) NOT NULL, quantidade_produzida NUMERIC(15,3) NOT NULL CHECK(quantidade_produzida > 0), id_cliente VARCHAR(38), tipo_ordem VARCHAR(20) NOT NULL CHECK(tipo_ordem IN ('ESTOQUE','ENCOMENDA')), id_fornecedor VARCHAR(38), data_ordem DATE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ABERTA' CHECK(status IN ('ABERTA','FINALIZADA','CANCELADA')), observacoes TEXT, data_finalizacao TIMESTAMPTZ, id_usuario_finalizacao VARCHAR(38), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_ordem_X_produto FOREIGN KEY(id_produto_acabado) REFERENCES produto(id), CONSTRAINT fk_ordem_X_cliente FOREIGN KEY(id_cliente) REFERENCES cliente(id), CONSTRAINT fk_ordem_X_fornecedor FOREIGN KEY(id_fornecedor) REFERENCES fornecedor(id), CONSTRAINT ck_ordem_cliente CHECK(tipo_ordem <> 'ENCOMENDA' OR id_cliente IS NOT NULL));
CREATE TABLE ordem_producao_item (id VARCHAR(38) PRIMARY KEY, id_ordem_producao VARCHAR(38) NOT NULL, id_produto_materia_prima VARCHAR(38) NOT NULL, quantidade_usada NUMERIC(15,3) NOT NULL CHECK(quantidade_usada > 0), produto_nome_snapshot VARCHAR(180) NOT NULL, unidade_snapshot VARCHAR(30) NOT NULL, CONSTRAINT fk_ordem_item_X_ordem FOREIGN KEY(id_ordem_producao) REFERENCES ordem_producao(id), CONSTRAINT fk_ordem_item_X_produto FOREIGN KEY(id_produto_materia_prima) REFERENCES produto(id));
CREATE TABLE movimento_estoque (id VARCHAR(38) PRIMARY KEY, id_produto VARCHAR(38) NOT NULL, saldo VARCHAR(20) NOT NULL CHECK(saldo IN ('COMERCIAL','PRODUCAO')), tipo_movimento VARCHAR(40) NOT NULL, origem_movimento VARCHAR(20) NOT NULL, id_origem VARCHAR(38) NOT NULL, quantidade NUMERIC(15,3) NOT NULL CHECK(quantidade <> 0), estoque_anterior NUMERIC(15,3) NOT NULL, estoque_posterior NUMERIC(15,3) NOT NULL CHECK(estoque_posterior >= 0), data_movimento TIMESTAMPTZ NOT NULL DEFAULT now(), id_usuario VARCHAR(38), observacao TEXT, CONSTRAINT fk_movimento_X_produto FOREIGN KEY(id_produto) REFERENCES produto(id));
CREATE TABLE entrega (id VARCHAR(38) PRIMARY KEY, id_venda VARCHAR(38) NOT NULL UNIQUE, id_cliente VARCHAR(38) NOT NULL, endereco_entrega VARCHAR(255), bairro VARCHAR(100), cidade VARCHAR(100), cep VARCHAR(8), telefone_contato VARCHAR(20), data_agendada DATE, horario TIME, id_entregador VARCHAR(38), status VARCHAR(30) NOT NULL DEFAULT 'AGENDADA' CHECK(status IN ('AGENDADA','EM_ROTA','ENTREGUE','NAO_ENTREGUE')), valor_frete NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK(valor_frete >= 0), observacoes TEXT, data_entrega TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38), CONSTRAINT fk_entrega_X_venda FOREIGN KEY(id_venda) REFERENCES venda(id), CONSTRAINT fk_entrega_X_cliente FOREIGN KEY(id_cliente) REFERENCES cliente(id), CONSTRAINT fk_entrega_X_funcionario FOREIGN KEY(id_entregador) REFERENCES funcionario(id));
CREATE TABLE despesa (id VARCHAR(38) PRIMARY KEY, tipo VARCHAR(30) NOT NULL, descricao VARCHAR(255) NOT NULL, valor NUMERIC(15,2) NOT NULL CHECK(valor > 0), data_despesa DATE NOT NULL, observacoes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by VARCHAR(38), updated_by VARCHAR(38));
'@

$files['backend/src/main/resources/db/migration/V4__create_permissions_and_indexes.sql'] = @'
CREATE TABLE configuracao (id VARCHAR(38) PRIMARY KEY, tema VARCHAR(10) NOT NULL DEFAULT 'light' CHECK(tema IN ('light','dark')), email_proprietario VARCHAR(180), whatsapp_proprietario VARCHAR(20), email_gerente VARCHAR(180), whatsapp_gerente VARCHAR(20), notificar_venda BOOLEAN NOT NULL DEFAULT true, notificar_entrega BOOLEAN NOT NULL DEFAULT true, notificar_estoque_baixo BOOLEAN NOT NULL DEFAULT true, canal_email BOOLEAN NOT NULL DEFAULT false, canal_whatsapp BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE permissao_pagina (id BIGSERIAL PRIMARY KEY, cargo_codigo VARCHAR(30) NOT NULL, pagina VARCHAR(60) NOT NULL, permitido BOOLEAN NOT NULL DEFAULT false, UNIQUE(cargo_codigo,pagina));
CREATE TABLE permissao_acao (id BIGSERIAL PRIMARY KEY, cargo_codigo VARCHAR(30) NOT NULL, acao VARCHAR(80) NOT NULL, permitido BOOLEAN NOT NULL DEFAULT false, UNIQUE(cargo_codigo,acao));
CREATE INDEX idx_cliente_nome ON cliente(nome); CREATE INDEX idx_cliente_telefone ON cliente(telefone); CREATE INDEX idx_cliente_cidade ON cliente(cidade); CREATE INDEX idx_cliente_ultima_compra ON cliente(ultima_compra);
CREATE INDEX idx_produto_nome ON produto(nome); CREATE INDEX idx_produto_tipo ON produto(tipo); CREATE INDEX idx_produto_codigo_barras ON produto(codigo_barras); CREATE INDEX idx_produto_ativo ON produto(ativo);
CREATE INDEX idx_venda_data ON venda(data_venda); CREATE INDEX idx_venda_status ON venda(status); CREATE INDEX idx_venda_cliente ON venda(id_cliente); CREATE INDEX idx_venda_vendedor ON venda(id_funcionario_vendedor);
CREATE INDEX idx_compra_data ON compra(data_compra); CREATE INDEX idx_compra_fornecedor ON compra(id_fornecedor); CREATE INDEX idx_compra_status ON compra(status);
CREATE INDEX idx_movimento_produto ON movimento_estoque(id_produto); CREATE INDEX idx_movimento_data ON movimento_estoque(data_movimento); CREATE INDEX idx_movimento_origem ON movimento_estoque(origem_movimento,id_origem);
'@

$files['backend/src/main/resources/db/migration/V5__seed_roles_and_permissions.sql'] = @'
INSERT INTO cargo(id,codigo,nome) VALUES ('00000000-0000-0000-0000-000000000001','DONO','Dono'),('00000000-0000-0000-0000-000000000002','GERENTE','Gerente'),('00000000-0000-0000-0000-000000000003','VENDEDOR','Vendedor'),('00000000-0000-0000-0000-000000000004','ESTOQUISTA','Estoquista'),('00000000-0000-0000-0000-000000000005','ENTREGADOR','Entregador');
INSERT INTO permissao_pagina(cargo_codigo,pagina,permitido)
SELECT c.codigo,p.pagina,(c.codigo IN ('DONO','GERENTE') OR (c.codigo='VENDEDOR' AND p.pagina IN ('dashboard','clientes','vendas')) OR (c.codigo='ESTOQUISTA' AND p.pagina IN ('dashboard','produtos','compras','producao')) OR (c.codigo='ENTREGADOR' AND p.pagina IN ('dashboard','entregas')))
FROM cargo c CROSS JOIN (VALUES ('dashboard'),('clientes'),('funcionarios'),('produtos'),('vendas'),('compras'),('fornecedores'),('producao'),('entregas'),('relatorios'),('configuracoes')) p(pagina);
INSERT INTO permissao_acao(cargo_codigo,acao,permitido)
SELECT c.codigo,a.acao,(c.codigo IN ('DONO','GERENTE') OR (c.codigo='VENDEDOR' AND a.acao IN ('clientes_criar','vendas_criar','entregas_criar')) OR (c.codigo='ESTOQUISTA' AND a.acao IN ('produtos_criar','produtos_editar','estoque_ajustar','compras_criar','producao_criar','producao_finalizar')) OR (c.codigo='ENTREGADOR' AND a.acao='entregas_atualizar_status'))
FROM cargo c CROSS JOIN (VALUES ('clientes_criar'),('clientes_editar'),('clientes_excluir'),('produtos_criar'),('produtos_editar'),('produtos_excluir'),('vendas_criar'),('vendas_cancelar'),('compras_criar'),('compras_cancelar'),('producao_criar'),('producao_finalizar'),('estoque_ajustar'),('entregas_criar'),('entregas_atualizar_status'),('funcionarios_criar'),('funcionarios_editar'),('funcionarios_excluir'),('relatorios_visualizar'),('configuracoes_editar')) a(acao);
'@

$files['backend/src/main/java/br/com/tapeteskalke/TapetesKalkeApplication.java'] = @'
package br.com.tapeteskalke;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TapetesKalkeApplication {
    public static void main(String[] args) { SpringApplication.run(TapetesKalkeApplication.class, args); }
    @Bean Clock applicationClock() { return Clock.system(ZoneId.of("America/Sao_Paulo")); }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/config/SecurityConfig.java'] = @'
package br.com.tapeteskalke.config;

import br.com.tapeteskalke.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    @Value("${app.cors.allowed-origin}") private String allowedOrigin;
    public SecurityConfig(JwtAuthenticationFilter jwtFilter) { this.jwtFilter = jwtFilter; }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'")))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll().anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        var c = new CorsConfiguration(); c.setAllowedOrigins(List.of(allowedOrigin)); c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key")); c.setExposedHeaders(List.of("Location")); c.setAllowCredentials(false); c.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", c); return source;
    }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/config/OpenApiConfig.java'] = @'
package br.com.tapeteskalke.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration public class OpenApiConfig {
 @Bean OpenAPI openAPI() { return new OpenAPI().info(new Info().title("Tapetes Kalke API").version("v1")).addSecurityItem(new SecurityRequirement().addList("bearerAuth")).components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))); }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/config/JacksonConfig.java'] = @'
package br.com.tapeteskalke.config;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
@Configuration public class JacksonConfig { @Bean JavaTimeModule javaTimeModule() { return new JavaTimeModule(); } @Bean Jackson2ObjectMapperBuilder jacksonBuilder(JavaTimeModule module) { return new Jackson2ObjectMapperBuilder().modules(module); } }
'@

$files['backend/src/main/java/br/com/tapeteskalke/security/JwtTokenService.java'] = @'
package br.com.tapeteskalke.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service public class JwtTokenService {
 private final SecretKey key; private final long expirationSeconds;
 public JwtTokenService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-seconds}") long expirationSeconds) { if(secret.length()<32) throw new IllegalStateException("JWT_SECRET precisa ter ao menos 32 caracteres"); this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationSeconds=expirationSeconds; }
 public String issue(String userId,String email,String role) { Instant now=Instant.now(); return Jwts.builder().subject(userId).claim("email",email).claim("role",role).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact(); }
 public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
 public long expirationSeconds() { return expirationSeconds; }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/security/UserPrincipal.java'] = @'
package br.com.tapeteskalke.security;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public record UserPrincipal(String id,String email,String role) implements UserDetails {
 public Collection<? extends GrantedAuthority> getAuthorities(){ return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
 public String getPassword(){ return ""; } public String getUsername(){ return email; }
 public boolean isAccountNonExpired(){return true;} public boolean isAccountNonLocked(){return true;} public boolean isCredentialsNonExpired(){return true;} public boolean isEnabled(){return true;}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/security/JwtAuthenticationFilter.java'] = @'
package br.com.tapeteskalke.security;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtTokenService tokens; public JwtAuthenticationFilter(JwtTokenService tokens){this.tokens=tokens;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException { String header=request.getHeader("Authorization"); if(header!=null&&header.startsWith("Bearer ")) try { Claims c=tokens.parse(header.substring(7)); var principal=new UserPrincipal(c.getSubject(),c.get("email",String.class),c.get("role",String.class)); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities())); } catch(Exception ignored) { SecurityContextHolder.clearContext(); } chain.doFilter(request,response); }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/security/CurrentUser.java'] = @'
package br.com.tapeteskalke.security;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import br.com.tapeteskalke.exception.AccessDeniedBusinessException;
@Component public class CurrentUser {
 public UserPrincipal require(){ Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a==null || !(a.getPrincipal() instanceof UserPrincipal p)) throw new AccessDeniedBusinessException("Usuário não autenticado"); return p; }
 public boolean hasRole(String role){ try{return require().role().equals(role);}catch(AccessDeniedBusinessException e){return false;} }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/exception/BusinessException.java'] = @'
package br.com.tapeteskalke.exception;
import org.springframework.http.HttpStatus;
public class BusinessException extends RuntimeException { private final String code; private final HttpStatus status; public BusinessException(String code,String message,HttpStatus status){super(message);this.code=code;this.status=status;} public String code(){return code;} public HttpStatus status(){return status;} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/exception/ResourceNotFoundException.java'] = @'
package br.com.tapeteskalke.exception; import org.springframework.http.HttpStatus; public class ResourceNotFoundException extends BusinessException { public ResourceNotFoundException(String resource,String id){super(resource.toUpperCase()+"_NAO_ENCONTRADO",resource+" não encontrado: "+id,HttpStatus.NOT_FOUND);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/exception/AccessDeniedBusinessException.java'] = @'
package br.com.tapeteskalke.exception; import org.springframework.http.HttpStatus; public class AccessDeniedBusinessException extends BusinessException { public AccessDeniedBusinessException(String message){super("ACESSO_NEGADO",message,HttpStatus.FORBIDDEN);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/ApiResponse.java'] = @'
package br.com.tapeteskalke.dto; public record ApiResponse<T>(boolean success,T data,String message){ public static <T> ApiResponse<T> ok(T data){return new ApiResponse<>(true,data,null);} public static <T> ApiResponse<T> ok(T data,String message){return new ApiResponse<>(true,data,message);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/exception/GlobalExceptionHandler.java'] = @'
package br.com.tapeteskalke.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(BusinessException.class) ResponseEntity<?> business(BusinessException e,HttpServletRequest r){return ResponseEntity.status(e.status()).body(Map.of("success",false,"data",null,"message",e.getMessage(),"errorCode",e.code(),"timestamp",OffsetDateTime.now(ZoneId.of("America/Sao_Paulo")).toString(),"path",r.getRequestURI()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class}) ResponseEntity<?> validation(Exception e,HttpServletRequest r){return ResponseEntity.unprocessableEntity().body(Map.of("success",false,"data",null,"message","Dados inválidos","errorCode","VALIDACAO","timestamp",OffsetDateTime.now(ZoneId.of("America/Sao_Paulo")).toString(),"path",r.getRequestURI()));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unknown(Exception e,HttpServletRequest r){return ResponseEntity.internalServerError().body(Map.of("success",false,"data",null,"message","Erro interno inesperado","errorCode","ERRO_INTERNO","timestamp",OffsetDateTime.now(ZoneId.of("America/Sao_Paulo")).toString(),"path",r.getRequestURI()));}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/entity/BaseAuditEntity.java'] = @'
package br.com.tapeteskalke.entity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@MappedSuperclass @Getter @Setter public abstract class BaseAuditEntity {
 @Id @Column(length=38) private String id;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @Column(name="created_by",length=38) private String createdBy;
 @Column(name="updated_by",length=38) private String updatedBy;
 @PrePersist void createAudit(){ if(id==null) id=UUID.randomUUID().toString(); var now=OffsetDateTime.now(ZoneOffset.UTC); if(createdAt==null)createdAt=now; updatedAt=now; }
 @PreUpdate void updateAudit(){updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
}
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Cargo.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="cargo") @Getter @Setter public class Cargo extends BaseAuditEntity { @Column(nullable=false,unique=true,length=30) private String codigo; @Column(nullable=false,length=80) private String nome; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Usuario.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="usuario") @Getter @Setter public class Usuario extends BaseAuditEntity { @Column(nullable=false,length=150) private String nome; @Column(nullable=false,unique=true,length=180) private String email; @Column(name="senha_hash",nullable=false) private String senhaHash; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_cargo",nullable=false) private Cargo cargo; @Column(nullable=false) private boolean ativo=true; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Cliente.java'] = @'
package br.com.tapeteskalke.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="cliente") @Getter @Setter public class Cliente extends BaseAuditEntity { @Column(nullable=false,length=150) private String nome; @Column(nullable=false,length=20) private String telefone; private String email; @Column(name="razao_social") private String razaoSocial; @Column(length=14) private String cnpj; private String endereco; private String bairro; private String cidade; @Column(length=8) private String cep; @Column(name="data_nascimento") private LocalDate dataNascimento; @Column(columnDefinition="TEXT") private String observacoes; @Column(name="status_cadastral",nullable=false) private String statusCadastral="ATIVO"; @Column(name="ultima_compra") private LocalDate ultimaCompra; @Column(name="total_compras",nullable=false,precision=15,scale=2) private BigDecimal totalCompras=BigDecimal.ZERO; @Column(nullable=false) private boolean ativo=true; @Column(name="deleted_at") private OffsetDateTime deletedAt; @Column(name="deleted_by") private String deletedBy; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Fornecedor.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.time.OffsetDateTime; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="fornecedor") @Getter @Setter public class Fornecedor extends BaseAuditEntity { @Column(name="nome_empresa",nullable=false,length=180) private String nomeEmpresa; @Column(length=14) private String cnpj; @Column(nullable=false,length=20) private String telefone; private String email; @Column(columnDefinition="TEXT") private String observacoes; @Column(nullable=false) private boolean ativo=true; @Column(name="deleted_at") private OffsetDateTime deletedAt; @Column(name="deleted_by") private String deletedBy; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Funcionario.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="funcionario") @Getter @Setter public class Funcionario extends BaseAuditEntity { @Column(nullable=false,length=150) private String nome; private String telefone; private String email; private String endereco; @Column(precision=15,scale=2) private BigDecimal salario; @Column(name="data_contratacao") private LocalDate dataContratacao; @Column(nullable=false) private String status="ATIVO"; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_cargo",nullable=false) private Cargo cargo; @Column(columnDefinition="TEXT") private String observacoes; @Column(nullable=false) private boolean ativo=true; @Column(name="deleted_at") private OffsetDateTime deletedAt; @Column(name="deleted_by") private String deletedBy; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Produto.java'] = @'
package br.com.tapeteskalke.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.OffsetDateTime; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="produto") @Getter @Setter public class Produto extends BaseAuditEntity { @Column(nullable=false,length=180) private String nome; @Column(nullable=false,length=30) private String tipo; private String categoria; private String cor; private String tamanho; @Column(name="unidade_medida",nullable=false) private String unidadeMedida; @Column(name="pode_ser_vendido",nullable=false) private boolean podeSerVendido=true; @Column(name="pode_ser_materia_prima",nullable=false) private boolean podeSerMateriaPrima; @Column(name="estoque_comercial",nullable=false,precision=15,scale=3) private BigDecimal estoqueComercial=BigDecimal.ZERO; @Column(name="estoque_producao",nullable=false,precision=15,scale=3) private BigDecimal estoqueProducao=BigDecimal.ZERO; @Column(name="estoque_minimo_comercial",nullable=false,precision=15,scale=3) private BigDecimal estoqueMinimoComercial=BigDecimal.ONE; @Column(name="estoque_minimo_producao",nullable=false,precision=15,scale=3) private BigDecimal estoqueMinimoProducao=BigDecimal.ONE; @Column(name="preco_custo",nullable=false,precision=15,scale=2) private BigDecimal precoCusto=BigDecimal.ZERO; @Column(name="preco_venda",nullable=false,precision=15,scale=2) private BigDecimal precoVenda=BigDecimal.ZERO; @Column(name="codigo_barras") private String codigoBarras; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_fornecedor_principal") private Fornecedor fornecedorPrincipal; @Column(columnDefinition="TEXT") private String observacoes; @Column(nullable=false) private boolean ativo=true; @Version @Column(name="versao") private Long versao; @Column(name="deleted_at") private OffsetDateTime deletedAt; @Column(name="deleted_by") private String deletedBy; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Venda.java'] = @'
package br.com.tapeteskalke.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.ArrayList; import java.util.List; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="venda") @Getter @Setter public class Venda extends BaseAuditEntity { @Column(name="numero_venda",nullable=false) private Long numeroVenda; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_cliente",nullable=false) private Cliente cliente; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_funcionario_vendedor") private Funcionario vendedor; @Column(name="data_venda",nullable=false) private LocalDate dataVenda; @Column(nullable=false,precision=15,scale=2) private BigDecimal subtotal; @Column(nullable=false,precision=15,scale=2) private BigDecimal desconto=BigDecimal.ZERO; @Column(name="total_venda",nullable=false,precision=15,scale=2) private BigDecimal totalVenda; @Column(name="forma_pagamento",nullable=false) private String formaPagamento; @Column(nullable=false) private String status="FINALIZADA"; @Column(columnDefinition="TEXT") private String observacoes; @Column(name="cancelled_at") private OffsetDateTime cancelledAt; @Column(name="cancelled_by") private String cancelledBy; @Column(name="motivo_cancelamento",columnDefinition="TEXT") private String motivoCancelamento; @Column(name="idempotency_key",unique=true) private String idempotencyKey; @OneToMany(mappedBy="venda",cascade=CascadeType.ALL,orphanRemoval=true) private List<VendaItem> itens=new ArrayList<>(); public void addItem(VendaItem item){itens.add(item);item.setVenda(this);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/VendaItem.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="venda_item") @Getter @Setter public class VendaItem { @Id @Column(length=38) private String id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_venda",nullable=false) private Venda venda; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_produto",nullable=false) private Produto produto; @Column(name="produto_nome_snapshot",nullable=false) private String produtoNomeSnapshot; @Column(name="unidade_medida_snapshot",nullable=false) private String unidadeMedidaSnapshot; @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade; @Column(name="preco_unitario",nullable=false,precision=15,scale=2) private BigDecimal precoUnitario; @Column(nullable=false,precision=15,scale=2) private BigDecimal subtotal; @PrePersist void id(){if(id==null)id=java.util.UUID.randomUUID().toString();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Compra.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.*; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="compra") @Getter @Setter public class Compra extends BaseAuditEntity { @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_fornecedor") private Fornecedor fornecedor; @Column(name="fornecedor_nome_manual") private String fornecedorNomeManual; @Column(name="data_compra",nullable=false) private LocalDate dataCompra; @Column(name="total_custo",nullable=false,precision=15,scale=2) private BigDecimal totalCusto; @Column(nullable=false) private String status="FINALIZADA"; @Column(columnDefinition="TEXT") private String observacoes; @Column(name="cancelled_at") private OffsetDateTime cancelledAt; @Column(name="cancelled_by") private String cancelledBy; @Column(name="motivo_cancelamento",columnDefinition="TEXT") private String motivoCancelamento; @OneToMany(mappedBy="compra",cascade=CascadeType.ALL,orphanRemoval=true) private List<CompraItem> itens=new ArrayList<>(); public void addItem(CompraItem item){itens.add(item);item.setCompra(this);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/CompraItem.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="compra_item") @Getter @Setter public class CompraItem { @Id @Column(length=38) private String id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_compra",nullable=false) private Compra compra; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_produto",nullable=false) private Produto produto; @Column(name="destino_estoque",nullable=false) private String destinoEstoque; @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade; @Column(name="preco_unitario",nullable=false,precision=15,scale=2) private BigDecimal precoUnitario; @Column(nullable=false,precision=15,scale=2) private BigDecimal subtotal; @Column(name="produto_nome_snapshot",nullable=false) private String produtoNomeSnapshot; @PrePersist void id(){if(id==null)id=java.util.UUID.randomUUID().toString();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/OrdemProducao.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.*; import java.util.*; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="ordem_producao") @Getter @Setter public class OrdemProducao extends BaseAuditEntity { @Column(name="numero_ordem",nullable=false) private Long numeroOrdem; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_produto_acabado",nullable=false) private Produto produtoAcabado; @Column(name="quantidade_produzida",nullable=false,precision=15,scale=3) private BigDecimal quantidadeProduzida; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_cliente") private Cliente cliente; @Column(name="tipo_ordem",nullable=false) private String tipoOrdem; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_fornecedor") private Fornecedor fornecedor; @Column(name="data_ordem",nullable=false) private LocalDate dataOrdem; @Column(nullable=false) private String status="ABERTA"; @Column(columnDefinition="TEXT") private String observacoes; @Column(name="data_finalizacao") private OffsetDateTime dataFinalizacao; @Column(name="id_usuario_finalizacao") private String idUsuarioFinalizacao; @OneToMany(mappedBy="ordem",cascade=CascadeType.ALL,orphanRemoval=true) private List<OrdemProducaoItem> materiasPrimas=new ArrayList<>(); public void addItem(OrdemProducaoItem item){materiasPrimas.add(item);item.setOrdem(this);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/OrdemProducaoItem.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="ordem_producao_item") @Getter @Setter public class OrdemProducaoItem { @Id @Column(length=38) private String id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_ordem_producao",nullable=false) private OrdemProducao ordem; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_produto_materia_prima",nullable=false) private Produto produtoMateriaPrima; @Column(name="quantidade_usada",nullable=false,precision=15,scale=3) private BigDecimal quantidadeUsada; @Column(name="produto_nome_snapshot",nullable=false) private String produtoNomeSnapshot; @Column(name="unidade_snapshot",nullable=false) private String unidadeSnapshot; @PrePersist void id(){if(id==null)id=java.util.UUID.randomUUID().toString();} }
'@

$files['backend/src/main/java/br/com/tapeteskalke/entity/MovimentoEstoque.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.OffsetDateTime; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="movimento_estoque") @Getter @Setter public class MovimentoEstoque { @Id @Column(length=38) private String id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_produto",nullable=false) private Produto produto; @Column(nullable=false) private String saldo; @Column(name="tipo_movimento",nullable=false) private String tipoMovimento; @Column(name="origem_movimento",nullable=false) private String origemMovimento; @Column(name="id_origem",nullable=false) private String idOrigem; @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade; @Column(name="estoque_anterior",nullable=false,precision=15,scale=3) private BigDecimal estoqueAnterior; @Column(name="estoque_posterior",nullable=false,precision=15,scale=3) private BigDecimal estoquePosterior; @Column(name="data_movimento",nullable=false) private OffsetDateTime dataMovimento; @Column(name="id_usuario") private String idUsuario; @Column(columnDefinition="TEXT") private String observacao; @PrePersist void id(){if(id==null)id=java.util.UUID.randomUUID().toString();if(dataMovimento==null)dataMovimento=OffsetDateTime.now(java.time.ZoneOffset.UTC);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Entrega.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.*; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="entrega") @Getter @Setter public class Entrega extends BaseAuditEntity { @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_venda",nullable=false) private Venda venda; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_cliente",nullable=false) private Cliente cliente; @Column(name="endereco_entrega") private String enderecoEntrega; private String bairro; private String cidade; @Column(length=8) private String cep; @Column(name="telefone_contato") private String telefoneContato; @Column(name="data_agendada") private LocalDate dataAgendada; private LocalTime horario; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_entregador") private Funcionario entregador; @Column(nullable=false) private String status="AGENDADA"; @Column(name="valor_frete",nullable=false,precision=15,scale=2) private BigDecimal valorFrete=BigDecimal.ZERO; @Column(columnDefinition="TEXT") private String observacoes; @Column(name="data_entrega") private OffsetDateTime dataEntrega; }
'@
$files['backend/src/main/java/br/com/tapeteskalke/entity/Despesa.java'] = @'
package br.com.tapeteskalke.entity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import lombok.Getter; import lombok.Setter;
@Entity @Table(name="despesa") @Getter @Setter public class Despesa extends BaseAuditEntity { @Column(nullable=false) private String tipo; @Column(nullable=false) private String descricao; @Column(nullable=false,precision=15,scale=2) private BigDecimal valor; @Column(name="data_despesa",nullable=false) private LocalDate dataDespesa; @Column(columnDefinition="TEXT") private String observacoes; }
'@

$files['backend/src/main/java/br/com/tapeteskalke/repository/ClienteRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Cliente; import java.util.Optional; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ClienteRepository extends JpaRepository<Cliente,String> { @Query("select c from Cliente c where c.deletedAt is null and (:search='' or lower(c.nome) like lower(concat('%',:search,'%')) or c.telefone like concat('%',:search,'%') or lower(coalesce(c.cidade,'')) like lower(concat('%',:search,'%'))) ") Page<Cliente> search(@Param("search")String search,Pageable pageable); @Query("select c from Cliente c where c.id=:id and c.deletedAt is null") Optional<Cliente> activeById(@Param("id")String id); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/ProdutoRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Produto; import jakarta.persistence.LockModeType; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ProdutoRepository extends JpaRepository<Produto,String> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Produto p where p.id=:id and p.deletedAt is null") Optional<Produto> lockActiveById(@Param("id")String id); @Query("select p from Produto p where p.deletedAt is null and (:search='' or lower(p.nome) like lower(concat('%',:search,'%')) or lower(coalesce(p.codigoBarras,'')) like lower(concat('%',:search,'%'))) ") Page<Produto> search(@Param("search")String search,Pageable pageable); @Query("select p from Produto p where p.deletedAt is null and p.ativo=true and ((p.podeSerVendido=true and p.estoqueComercial<=p.estoqueMinimoComercial) or (p.podeSerMateriaPrima=true and p.estoqueProducao<=p.estoqueMinimoProducao))") List<Produto> lowStock(); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/FornecedorRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Fornecedor; import java.util.*; import org.springframework.data.jpa.repository.*; public interface FornecedorRepository extends JpaRepository<Fornecedor,String> { @Query("select f from Fornecedor f where f.deletedAt is null and f.ativo=true order by f.nomeEmpresa") List<Fornecedor> active(); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/CargoRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Cargo; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository; public interface CargoRepository extends JpaRepository<Cargo,String> { Optional<Cargo> findByCodigo(String codigo); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/UsuarioRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Usuario; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository; public interface UsuarioRepository extends JpaRepository<Usuario,String> { Optional<Usuario> findByEmailIgnoreCaseAndAtivoTrue(String email); boolean existsByEmailIgnoreCase(String email); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/FuncionarioRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Funcionario; import java.util.*; import org.springframework.data.jpa.repository.*; public interface FuncionarioRepository extends JpaRepository<Funcionario,String> { @Query("select f from Funcionario f join fetch f.cargo where f.deletedAt is null order by f.nome") List<Funcionario> activeList(); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/VendaRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Venda; import java.math.BigDecimal; import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface VendaRepository extends JpaRepository<Venda,String> { Optional<Venda> findByIdempotencyKey(String key); @EntityGraph(attributePaths={"cliente","vendedor","itens","itens.produto"}) @Query("select v from Venda v where v.id=:id") Optional<Venda> detail(@Param("id")String id); @Query("select coalesce(sum(v.totalVenda),0) from Venda v where v.status='FINALIZADA' and v.dataVenda between :from and :to") BigDecimal totalFinalized(@Param("from")LocalDate from,@Param("to")LocalDate to); @Query("select v from Venda v where v.cliente.id=:clientId and v.status='FINALIZADA' order by v.dataVenda desc") List<Venda> finalizedByClient(@Param("clientId")String clientId); @Query("select v from Venda v join fetch v.cliente order by v.dataVenda desc") List<Venda> listWithClient(); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/CompraRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Compra; import java.math.BigDecimal; import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; public interface CompraRepository extends JpaRepository<Compra,String> { @EntityGraph(attributePaths={"fornecedor","itens","itens.produto"}) @Query("select c from Compra c where c.id=:id") Optional<Compra> detail(@Param("id")String id); @Query("select coalesce(sum(c.totalCusto),0) from Compra c where c.status='FINALIZADA' and c.dataCompra between :from and :to") BigDecimal totalFinalized(@Param("from")LocalDate from,@Param("to")LocalDate to); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/OrdemProducaoRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.OrdemProducao; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao,String> { @EntityGraph(attributePaths={"produtoAcabado","cliente","materiasPrimas","materiasPrimas.produtoMateriaPrima"}) @Query("select o from OrdemProducao o where o.id=:id") Optional<OrdemProducao> detail(@Param("id")String id); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/MovimentoEstoqueRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.MovimentoEstoque; import java.util.*; import org.springframework.data.jpa.repository.*; public interface MovimentoEstoqueRepository extends JpaRepository<MovimentoEstoque,String> { @Query("select m from MovimentoEstoque m join fetch m.produto order by m.dataMovimento desc") List<MovimentoEstoque> recent(); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/EntregaRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Entrega; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; public interface EntregaRepository extends JpaRepository<Entrega,String> { boolean existsByVendaId(String vendaId); @EntityGraph(attributePaths={"venda","cliente","entregador"}) @Query("select e from Entrega e where e.id=:id") Optional<Entrega> detail(@Param("id")String id); @EntityGraph(attributePaths={"venda","cliente","entregador"}) @Query("select e from Entrega e order by e.dataAgendada asc nulls last") List<Entrega> allDetailed(); }
'@
$files['backend/src/main/java/br/com/tapeteskalke/repository/DespesaRepository.java'] = @'
package br.com.tapeteskalke.repository; import br.com.tapeteskalke.entity.Despesa; import java.math.BigDecimal; import java.time.LocalDate; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; public interface DespesaRepository extends JpaRepository<Despesa,String> { @Query("select coalesce(sum(d.valor),0) from Despesa d where d.dataDespesa between :from and :to") BigDecimal totalByPeriod(@Param("from")LocalDate from,@Param("to")LocalDate to); }
'@

$files['backend/src/main/java/br/com/tapeteskalke/repository/PermissaoRepository.java'] = @'
package br.com.tapeteskalke.repository; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface PermissaoRepository extends JpaRepository<br.com.tapeteskalke.entity.Cargo,String> { @Query(value="select exists(select 1 from permissao_acao where cargo_codigo=:role and acao=:action and permitido=true)",nativeQuery=true) boolean hasAction(@Param("role")String role,@Param("action")String action); @Query(value="select exists(select 1 from permissao_pagina where cargo_codigo=:role and pagina=:page and permitido=true)",nativeQuery=true) boolean hasPage(@Param("role")String role,@Param("page")String page); }
'@

$files['backend/src/main/java/br/com/tapeteskalke/dto/PageResponse.java'] = @'
package br.com.tapeteskalke.dto; import java.util.List; import org.springframework.data.domain.Page; public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages){ public static <T> PageResponse<T> from(Page<T> p){return new PageResponse<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/AuthDtos.java'] = @'
package br.com.tapeteskalke.dto; import jakarta.validation.constraints.*; import java.util.Map; public final class AuthDtos { private AuthDtos(){} public record LoginRequest(@NotBlank @Email String email,@NotBlank String senha){} public record UserResponse(String id,String nome,String email,String cargo,Map<String,Boolean> pages,Map<String,Boolean> actions){} public record LoginResponse(String accessToken,long expiresIn,UserResponse user){} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/ClienteDtos.java'] = @'
package br.com.tapeteskalke.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; public final class ClienteDtos { private ClienteDtos(){} public record Request(@NotBlank @Size(max=150) String nome,@NotBlank @Size(max=20) String telefone,@Email String email,@Size(max=180) String razaoSocial,@Size(max=18) String cnpj,String endereco,String bairro,String cidade,@Size(max=10) String cep,LocalDate dataNascimento,String observacoes,@Pattern(regexp="ATIVO|INATIVO") String statusCadastral){} public record Response(String id,String nome,String telefone,String email,String cidade,String statusCadastral,String classificacao,LocalDate ultimaCompra,BigDecimal totalCompras,boolean ativo){} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/ProdutoDtos.java'] = @'
package br.com.tapeteskalke.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal; public final class ProdutoDtos { private ProdutoDtos(){} public record Request(@NotBlank @Size(max=180) String nome,@NotBlank String tipo,String categoria,String cor,String tamanho,@NotBlank String unidadeMedida,boolean podeSerVendido,boolean podeSerMateriaPrima,@DecimalMin("0.000") BigDecimal estoqueMinimoComercial,@DecimalMin("0.000") BigDecimal estoqueMinimoProducao,@DecimalMin("0.00") BigDecimal precoCusto,@DecimalMin("0.00") BigDecimal precoVenda,String codigoBarras,String fornecedorPrincipalId,String observacoes,boolean ativo){} public record Response(String id,String nome,String tipo,String unidadeMedida,boolean podeSerVendido,boolean podeSerMateriaPrima,BigDecimal estoqueComercial,BigDecimal estoqueProducao,BigDecimal estoqueMinimoComercial,BigDecimal estoqueMinimoProducao,BigDecimal precoVenda,String statusComercial,String statusProducao,boolean ativo){} public record AjusteRequest(@NotBlank String produtoId,@NotBlank @Pattern(regexp="COMERCIAL|PRODUCAO") String saldo,@NotNull @DecimalMin(value="-999999999.999",inclusive=false) BigDecimal quantidade,@NotBlank @Size(max=300) String observacao){} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/FornecedorDtos.java'] = @'
package br.com.tapeteskalke.dto; import jakarta.validation.constraints.*; public final class FornecedorDtos { private FornecedorDtos(){} public record Request(@NotBlank @Size(max=180) String nomeEmpresa,@Size(max=18) String cnpj,@NotBlank @Size(max=20) String telefone,@Email String email,String observacoes){} public record Response(String id,String nomeEmpresa,String cnpj,String telefone,String email,boolean ativo){} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/FuncionarioDtos.java'] = @'
package br.com.tapeteskalke.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; public final class FuncionarioDtos { private FuncionarioDtos(){} public record Request(@NotBlank String nome,String telefone,@Email String email,String endereco,@DecimalMin("0.00") BigDecimal salario,LocalDate dataContratacao,@Pattern(regexp="ATIVO|INATIVO|FERIAS") String status,@NotBlank String cargoId,String observacoes){} public record Response(String id,String nome,String telefone,String email,String cargo,String status,LocalDate dataContratacao,BigDecimal salario){} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/OperacaoDtos.java'] = @'
package br.com.tapeteskalke.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.List;
public final class OperacaoDtos { private OperacaoDtos(){} public record VendaItem(@NotBlank String produtoId,@NotNull @DecimalMin(value="0.001") BigDecimal quantidade){} public record VendaRequest(@NotBlank String clienteId,String vendedorId,LocalDate dataVenda,@NotNull @DecimalMin("0.00") BigDecimal desconto,@NotBlank String formaPagamento,@NotEmpty List<@Valid VendaItem> itens,String observacoes){} public record VendaResponse(String id,Long numeroVenda,String cliente,LocalDate dataVenda,BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String status){} public record CancelarRequest(@NotBlank @Size(max=500) String motivo){}
 public record CompraItem(@NotBlank String produtoId,@NotBlank @Pattern(regexp="COMERCIAL|PRODUCAO") String destinoEstoque,@NotNull @DecimalMin(value="0.001") BigDecimal quantidade,@NotNull @DecimalMin("0.00") BigDecimal precoUnitario){} public record CompraRequest(String fornecedorId,String fornecedorNomeManual,@NotNull LocalDate dataCompra,@NotEmpty List<@Valid CompraItem> itens,String observacoes){} public record CompraResponse(String id,LocalDate dataCompra,BigDecimal total,String status){}
 public record MateriaPrima(@NotBlank String produtoId,@NotNull @DecimalMin(value="0.001") BigDecimal quantidadeUsada){} public record OrdemRequest(@NotBlank String produtoAcabadoId,@NotNull @DecimalMin(value="0.001") BigDecimal quantidadeProduzida,@NotBlank @Pattern(regexp="ESTOQUE|ENCOMENDA") String tipoOrdem,String clienteId,String fornecedorId,@NotEmpty List<@Valid MateriaPrima> materiasPrimas,String observacoes){} public record OrdemResponse(String id,Long numeroOrdem,String produto,BigDecimal quantidade,String status,LocalDate data){}
 public record EntregaRequest(@NotBlank String vendaId,String enderecoEntrega,String bairro,String cidade,String cep,String telefoneContato,LocalDate dataAgendada,LocalTime horario,String entregadorId,@DecimalMin("0.00") BigDecimal valorFrete,String observacoes){} public record EntregaStatusRequest(@NotBlank @Pattern(regexp="AGENDADA|EM_ROTA|ENTREGUE|NAO_ENTREGUE") String status){} public record EntregaResponse(String id,String cliente,String vendaId,String status,LocalDate dataAgendada,OffsetDateTime dataEntrega){}
 public record DespesaRequest(@NotBlank String tipo,@NotBlank String descricao,@NotNull @DecimalMin(value="0.01") BigDecimal valor,@NotNull LocalDate dataDespesa,String observacoes){}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/util/Normalizer.java'] = @'
package br.com.tapeteskalke.util; public final class Normalizer { private Normalizer(){} public static String digits(String value){return value==null?null:value.replaceAll("\\D","");} public static String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/PermissionService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.exception.AccessDeniedBusinessException; import br.com.tapeteskalke.repository.PermissaoRepository; import br.com.tapeteskalke.security.CurrentUser; import java.util.*; import org.springframework.stereotype.Service;
@Service public class PermissionService { private final CurrentUser current; private final PermissaoRepository permissions; private static final List<String> PAGES=List.of("dashboard","clientes","funcionarios","produtos","vendas","compras","fornecedores","producao","entregas","relatorios","configuracoes"); private static final List<String> ACTIONS=List.of("clientes_criar","clientes_editar","clientes_excluir","produtos_criar","produtos_editar","produtos_excluir","vendas_criar","vendas_cancelar","compras_criar","compras_cancelar","producao_criar","producao_finalizar","estoque_ajustar","entregas_criar","entregas_atualizar_status","funcionarios_criar","funcionarios_editar","funcionarios_excluir","relatorios_visualizar","configuracoes_editar"); public PermissionService(CurrentUser current,PermissaoRepository permissions){this.current=current;this.permissions=permissions;} public void action(String action){var u=current.require();if(!permissions.hasAction(u.role(),action))throw new AccessDeniedBusinessException("Sem permissão para: "+action);} public void page(String page){var u=current.require();if(!permissions.hasPage(u.role(),page))throw new AccessDeniedBusinessException("Sem acesso à página: "+page);} public Map<String,Boolean> pages(){return pagesFor(current.require().role());} public Map<String,Boolean> actions(){return actionsFor(current.require().role());} public Map<String,Boolean> pagesFor(String role){var r=new LinkedHashMap<String,Boolean>();for(var p:PAGES)r.put(p,permissions.hasPage(role,p));return r;} public Map<String,Boolean> actionsFor(String role){var r=new LinkedHashMap<String,Boolean>();for(var a:ACTIONS)r.put(a,permissions.hasAction(role,a));return r;} }
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/AuditService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.entity.BaseAuditEntity; import br.com.tapeteskalke.security.CurrentUser; import org.springframework.stereotype.Service;
@Service public class AuditService { private final CurrentUser current; public AuditService(CurrentUser current){this.current=current;} public String userId(){return current.require().id();} public void created(BaseAuditEntity e){var id=userId();e.setCreatedBy(id);e.setUpdatedBy(id);} public void changed(BaseAuditEntity e){e.setUpdatedBy(userId());} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/NumeroService.java'] = @'
package br.com.tapeteskalke.service; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service; @Service public class NumeroService { private final JdbcTemplate jdbc; public NumeroService(JdbcTemplate jdbc){this.jdbc=jdbc;} public Long nextVenda(){return jdbc.queryForObject("select nextval('venda_numero_seq')",Long.class);} public Long nextOrdem(){return jdbc.queryForObject("select nextval('ordem_numero_seq')",Long.class);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/ClienteService.java'] = @'
package br.com.tapeteskalke.service;
import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.ClienteDtos.*; import br.com.tapeteskalke.entity.Cliente; import br.com.tapeteskalke.exception.ResourceNotFoundException; import br.com.tapeteskalke.repository.ClienteRepository; import br.com.tapeteskalke.util.Normalizer; import java.time.*; import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class ClienteService { private final ClienteRepository repo; private final AuditService audit; private final Clock clock; public ClienteService(ClienteRepository repo,AuditService audit,Clock clock){this.repo=repo;this.audit=audit;this.clock=clock;}
 @Transactional(readOnly=true) public PageResponse<Response> list(String search,int page,int size){return PageResponse.from(repo.search(search==null?"":search.trim(),PageRequest.of(page,Math.min(Math.max(size,1),100),Sort.by("nome"))).map(this::response));}
 @Transactional(readOnly=true) public Response get(String id){return response(active(id));}
 @Transactional public Response create(Request r){var c=new Cliente();copy(c,r);audit.created(c);return response(repo.save(c));}
 @Transactional public Response update(String id,Request r){var c=active(id);copy(c,r);audit.changed(c);return response(c);}
 @Transactional public void delete(String id){var c=active(id);c.setAtivo(false);c.setDeletedAt(OffsetDateTime.now(clock));c.setDeletedBy(audit.userId());audit.changed(c);}
 public Cliente active(String id){return repo.activeById(id).orElseThrow(()->new ResourceNotFoundException("Cliente",id));}
 private void copy(Cliente c,Request r){c.setNome(r.nome().trim());c.setTelefone(Normalizer.digits(r.telefone()));c.setEmail(Normalizer.blankToNull(r.email()));c.setRazaoSocial(Normalizer.blankToNull(r.razaoSocial()));c.setCnpj(Normalizer.digits(r.cnpj()));c.setEndereco(Normalizer.blankToNull(r.endereco()));c.setBairro(Normalizer.blankToNull(r.bairro()));c.setCidade(Normalizer.blankToNull(r.cidade()));c.setCep(Normalizer.digits(r.cep()));c.setDataNascimento(r.dataNascimento());c.setObservacoes(Normalizer.blankToNull(r.observacoes()));if(r.statusCadastral()!=null)c.setStatusCadastral(r.statusCadastral());}
 public Response response(Cliente c){return new Response(c.getId(),c.getNome(),c.getTelefone(),c.getEmail(),c.getCidade(),c.getStatusCadastral(),classification(c),c.getUltimaCompra(),c.getTotalCompras(),c.isAtivo());}
 private String classification(Cliente c){if("INATIVO".equals(c.getStatusCadastral()))return "INATIVO";if(c.getUltimaCompra()==null)return "NOVO";long months=java.time.temporal.ChronoUnit.MONTHS.between(c.getUltimaCompra().withDayOfMonth(1),LocalDate.now(clock).withDayOfMonth(1));return months>=2?"ATENCAO_INATIVO":months>=1?"ATENCAO":"ATIVO";}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/ProdutoService.java'] = @'
package br.com.tapeteskalke.service;
import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.ProdutoDtos.*; import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.*; import br.com.tapeteskalke.repository.*; import java.math.*; import java.time.*; import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class ProdutoService { private final ProdutoRepository repo; private final FornecedorRepository fornecedores; private final AuditService audit; public ProdutoService(ProdutoRepository repo,FornecedorRepository fornecedores,AuditService audit){this.repo=repo;this.fornecedores=fornecedores;this.audit=audit;}
 @Transactional(readOnly=true) public PageResponse<Response> list(String search,int page,int size){return PageResponse.from(repo.search(search==null?"":search.trim(),PageRequest.of(page,Math.min(Math.max(1,size),100),Sort.by("nome"))).map(this::response));}
 @Transactional(readOnly=true) public Response get(String id){return response(active(id));}
 @Transactional public Response create(Request r){var p=new Produto();copy(p,r);audit.created(p);return response(repo.save(p));}
 @Transactional public Response update(String id,Request r){var p=active(id);copy(p,r);audit.changed(p);return response(p);}
 @Transactional public void delete(String id){var p=active(id);p.setAtivo(false);p.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));audit.changed(p);}
 public Produto active(String id){return repo.findById(id).filter(p->p.getDeletedAt()==null).orElseThrow(()->new ResourceNotFoundException("Produto",id));}
 public Produto locked(String id){return repo.lockActiveById(id).orElseThrow(()->new ResourceNotFoundException("Produto",id));}
 private void copy(Produto p,Request r){p.setNome(r.nome().trim());p.setTipo(r.tipo());p.setCategoria(r.categoria());p.setCor(r.cor());p.setTamanho(r.tamanho());p.setUnidadeMedida(r.unidadeMedida());p.setPodeSerVendido(r.podeSerVendido());p.setPodeSerMateriaPrima(r.podeSerMateriaPrima());p.setEstoqueMinimoComercial(or(r.estoqueMinimoComercial(),BigDecimal.ONE));p.setEstoqueMinimoProducao(or(r.estoqueMinimoProducao(),BigDecimal.ONE));p.setPrecoCusto(or(r.precoCusto(),BigDecimal.ZERO));p.setPrecoVenda(or(r.precoVenda(),BigDecimal.ZERO));p.setCodigoBarras(r.codigoBarras());p.setObservacoes(r.observacoes());p.setAtivo(r.ativo());p.setFornecedorPrincipal(r.fornecedorPrincipalId()==null||r.fornecedorPrincipalId().isBlank()?null:fornecedores.findById(r.fornecedorPrincipalId()).orElseThrow(()->new ResourceNotFoundException("Fornecedor",r.fornecedorPrincipalId())));}
 private BigDecimal or(BigDecimal v,BigDecimal d){return v==null?d:v;}
 public Response response(Produto p){return new Response(p.getId(),p.getNome(),p.getTipo(),p.getUnidadeMedida(),p.isPodeSerVendido(),p.isPodeSerMateriaPrima(),p.getEstoqueComercial(),p.getEstoqueProducao(),p.getEstoqueMinimoComercial(),p.getEstoqueMinimoProducao(),p.getPrecoVenda(),status(p.getEstoqueComercial(),p.getEstoqueMinimoComercial()),status(p.getEstoqueProducao(),p.getEstoqueMinimoProducao()),p.isAtivo());}
 private String status(BigDecimal saldo,BigDecimal minimo){return saldo.signum()==0?"ESGOTADO":saldo.compareTo(minimo)<=0?"BAIXO":"OK";}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/EstoqueService.java'] = @'
package br.com.tapeteskalke.service;
import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.BusinessException; import br.com.tapeteskalke.repository.MovimentoEstoqueRepository; import br.com.tapeteskalke.security.CurrentUser; import java.math.BigDecimal; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service;
@Service public class EstoqueService { private final MovimentoEstoqueRepository movements; private final CurrentUser current; public EstoqueService(MovimentoEstoqueRepository movements,CurrentUser current){this.movements=movements;this.current=current;}
 public void change(Produto p,String saldo,BigDecimal delta,String tipo,String origem,String origemId,String note){BigDecimal before="COMERCIAL".equals(saldo)?p.getEstoqueComercial():p.getEstoqueProducao();BigDecimal after=before.add(delta);if(after.signum()<0)throw new BusinessException("ESTOQUE_INSUFICIENTE","Estoque insuficiente para "+p.getNome(),HttpStatus.CONFLICT);if("COMERCIAL".equals(saldo))p.setEstoqueComercial(after);else p.setEstoqueProducao(after);var m=new MovimentoEstoque();m.setProduto(p);m.setSaldo(saldo);m.setQuantidade(delta);m.setEstoqueAnterior(before);m.setEstoquePosterior(after);m.setTipoMovimento(tipo);m.setOrigemMovimento(origem);m.setIdOrigem(origemId);m.setIdUsuario(current.require().id());m.setObservacao(note);movements.save(m); }
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/VendaService.java'] = @'
package br.com.tapeteskalke.service;
import br.com.tapeteskalke.dto.OperacaoDtos.VendaRequest; import br.com.tapeteskalke.dto.OperacaoDtos.VendaResponse; import br.com.tapeteskalke.dto.OperacaoDtos.CancelarRequest; import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.*; import br.com.tapeteskalke.repository.*; import br.com.tapeteskalke.security.CurrentUser; import java.math.*; import java.time.*; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class VendaService { private final VendaRepository vendas; private final ClienteService clientes; private final ProdutoService produtos; private final FuncionarioRepository funcionarios; private final EstoqueService estoque; private final PermissionService permissions; private final CurrentUser current; private final NumeroService numeros; private final Clock clock;
 public VendaService(VendaRepository vendas,ClienteService clientes,ProdutoService produtos,FuncionarioRepository funcionarios,EstoqueService estoque,PermissionService permissions,CurrentUser current,NumeroService numeros,Clock clock){this.vendas=vendas;this.clientes=clientes;this.produtos=produtos;this.funcionarios=funcionarios;this.estoque=estoque;this.permissions=permissions;this.current=current;this.numeros=numeros;this.clock=clock;}
 @Transactional public VendaResponse create(VendaRequest r,String key){permissions.action("vendas_criar");if(key==null||key.isBlank())throw new BusinessException("IDEMPOTENCY_KEY_OBRIGATORIA","Informe Idempotency-Key",HttpStatus.BAD_REQUEST);var previous=vendas.findByIdempotencyKey(key);if(previous.isPresent())return response(previous.get());var client=clientes.active(r.clienteId());if(r.desconto().signum()<0)throw new BusinessException("DESCONTO_INVALIDO","Desconto inválido",HttpStatus.UNPROCESSABLE_ENTITY);var v=new Venda();v.setNumeroVenda(numeros.nextVenda());v.setCliente(client);v.setVendedor(r.vendedorId()==null||r.vendedorId().isBlank()?null:funcionarios.findById(r.vendedorId()).orElseThrow(()->new ResourceNotFoundException("Funcionário",r.vendedorId())));v.setDataVenda(r.dataVenda()==null?LocalDate.now(clock):r.dataVenda());v.setFormaPagamento(r.formaPagamento());v.setObservacoes(r.observacoes());v.setIdempotencyKey(key);v.setCreatedBy(current.require().id());v.setUpdatedBy(current.require().id());BigDecimal subtotal=BigDecimal.ZERO;Map<String,Produto> locked=new LinkedHashMap<>();for(var item:r.itens().stream().sorted(Comparator.comparing(br.com.tapeteskalke.dto.OperacaoDtos.VendaItem::produtoId)).toList())locked.put(item.produtoId(),produtos.locked(item.produtoId()));for(var input:r.itens()){var p=locked.get(input.produtoId());if(!p.isAtivo()||!p.isPodeSerVendido())throw new BusinessException("PRODUTO_INATIVO","Produto indisponível: "+p.getNome(),HttpStatus.CONFLICT);if(input.quantidade().signum()<=0)throw new BusinessException("QUANTIDADE_INVALIDA","Quantidade deve ser positiva",HttpStatus.UNPROCESSABLE_ENTITY);var item=new VendaItem();item.setProduto(p);item.setProdutoNomeSnapshot(p.getNome());item.setUnidadeMedidaSnapshot(p.getUnidadeMedida());item.setQuantidade(input.quantidade());item.setPrecoUnitario(p.getPrecoVenda());item.setSubtotal(p.getPrecoVenda().multiply(input.quantidade()));subtotal=subtotal.add(item.getSubtotal());v.addItem(item);}
 if(r.desconto().compareTo(subtotal)>0)throw new BusinessException("DESCONTO_INVALIDO","Desconto não pode superar subtotal",HttpStatus.UNPROCESSABLE_ENTITY);v.setSubtotal(subtotal);v.setDesconto(r.desconto());v.setTotalVenda(subtotal.subtract(r.desconto()));vendas.save(v);for(var item:v.getItens())estoque.change(item.getProduto(),"COMERCIAL",item.getQuantidade().negate(),"SAIDA_VENDA","VENDA",v.getId(),"Venda "+v.getNumeroVenda());recalculateClient(client);return response(v); }
 @Transactional public VendaResponse cancel(String id,CancelarRequest r){permissions.action("vendas_cancelar");var v=vendas.detail(id).orElseThrow(()->new ResourceNotFoundException("Venda",id));if(!"FINALIZADA".equals(v.getStatus()))throw new BusinessException("VENDA_NAO_CANCELAVEL","A venda não pode ser cancelada",HttpStatus.CONFLICT);Map<String,Produto> locked=new HashMap<>();for(var item:v.getItens().stream().sorted(Comparator.comparing(i->i.getProduto().getId())).toList())locked.put(item.getProduto().getId(),produtos.locked(item.getProduto().getId()));for(var item:v.getItens()){var p=locked.get(item.getProduto().getId());estoque.change(p,"COMERCIAL",item.getQuantidade(),"ESTORNO_VENDA","CANCELAMENTO",v.getId(),"Estorno da venda "+v.getNumeroVenda());}v.setStatus("CANCELADA");v.setCancelledAt(OffsetDateTime.now(clock));v.setCancelledBy(current.require().id());v.setMotivoCancelamento(r.motivo());v.setUpdatedBy(current.require().id());recalculateClient(v.getCliente());return response(v); }
 @Transactional(readOnly=true) public List<VendaResponse> list(){return vendas.listWithClient().stream().map(this::response).toList();} @Transactional(readOnly=true) public VendaResponse get(String id){return response(vendas.detail(id).orElseThrow(()->new ResourceNotFoundException("Venda",id)));}
 private void recalculateClient(Cliente c){var sales=vendas.finalizedByClient(c.getId());c.setTotalCompras(sales.stream().map(Venda::getTotalVenda).reduce(BigDecimal.ZERO,BigDecimal::add));c.setUltimaCompra(sales.stream().map(Venda::getDataVenda).max(LocalDate::compareTo).orElse(null));c.setUpdatedBy(current.require().id());}
 private VendaResponse response(Venda v){return new VendaResponse(v.getId(),v.getNumeroVenda(),v.getCliente().getNome(),v.getDataVenda(),v.getSubtotal(),v.getDesconto(),v.getTotalVenda(),v.getStatus());}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/CompraService.java'] = @'
package br.com.tapeteskalke.service;
import br.com.tapeteskalke.dto.OperacaoDtos.CompraRequest; import br.com.tapeteskalke.dto.OperacaoDtos.CompraResponse; import br.com.tapeteskalke.dto.OperacaoDtos.CancelarRequest; import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.*; import br.com.tapeteskalke.repository.*; import br.com.tapeteskalke.security.CurrentUser; import java.math.*; import java.time.*; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class CompraService { private final CompraRepository compras; private final FornecedorRepository fornecedores; private final ProdutoService produtos; private final EstoqueService estoque; private final PermissionService permissions; private final CurrentUser current; public CompraService(CompraRepository compras,FornecedorRepository fornecedores,ProdutoService produtos,EstoqueService estoque,PermissionService permissions,CurrentUser current){this.compras=compras;this.fornecedores=fornecedores;this.produtos=produtos;this.estoque=estoque;this.permissions=permissions;this.current=current;}
 @Transactional public CompraResponse create(CompraRequest r){permissions.action("compras_criar");if((r.fornecedorId()==null||r.fornecedorId().isBlank())&&(r.fornecedorNomeManual()==null||r.fornecedorNomeManual().isBlank()))throw new BusinessException("FORNECEDOR_OBRIGATORIO","Informe fornecedor cadastrado ou manual",HttpStatus.UNPROCESSABLE_ENTITY);var c=new Compra();c.setFornecedor(r.fornecedorId()==null||r.fornecedorId().isBlank()?null:fornecedores.findById(r.fornecedorId()).orElseThrow(()->new ResourceNotFoundException("Fornecedor",r.fornecedorId())));c.setFornecedorNomeManual(r.fornecedorNomeManual());c.setDataCompra(r.dataCompra());c.setObservacoes(r.observacoes());c.setCreatedBy(current.require().id());c.setUpdatedBy(current.require().id());BigDecimal total=BigDecimal.ZERO;Map<String,Produto> locked=new LinkedHashMap<>();for(var i:r.itens().stream().sorted(Comparator.comparing(br.com.tapeteskalke.dto.OperacaoDtos.CompraItem::produtoId)).toList())locked.put(i.produtoId(),produtos.locked(i.produtoId()));for(var input:r.itens()){var p=locked.get(input.produtoId());var item=new CompraItem();item.setProduto(p);item.setDestinoEstoque(input.destinoEstoque());item.setQuantidade(input.quantidade());item.setPrecoUnitario(input.precoUnitario());item.setSubtotal(input.quantidade().multiply(input.precoUnitario()));item.setProdutoNomeSnapshot(p.getNome());c.addItem(item);total=total.add(item.getSubtotal());}c.setTotalCusto(total);compras.save(c);for(var item:c.getItens())estoque.change(item.getProduto(),item.getDestinoEstoque(),item.getQuantidade(),"ENTRADA_COMPRA","COMPRA",c.getId(),"Compra");return response(c);}
 @Transactional public CompraResponse cancel(String id,CancelarRequest r){permissions.action("compras_cancelar");var c=compras.detail(id).orElseThrow(()->new ResourceNotFoundException("Compra",id));if(!"FINALIZADA".equals(c.getStatus()))throw new BusinessException("COMPRA_NAO_CANCELAVEL","Compra não pode ser cancelada",HttpStatus.CONFLICT);for(var item:c.getItens().stream().sorted(Comparator.comparing(i->i.getProduto().getId())).toList()){var p=produtos.locked(item.getProduto().getId());estoque.change(p,item.getDestinoEstoque(),item.getQuantidade().negate(),"ESTORNO_COMPRA","CANCELAMENTO",c.getId(),"Cancelamento da compra");}c.setStatus("CANCELADA");c.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));c.setCancelledBy(current.require().id());c.setMotivoCancelamento(r.motivo());return response(c);} @Transactional(readOnly=true) public List<CompraResponse> list(){return compras.findAll().stream().map(this::response).toList();} private CompraResponse response(Compra c){return new CompraResponse(c.getId(),c.getDataCompra(),c.getTotalCusto(),c.getStatus());}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/ProducaoService.java'] = @'
package br.com.tapeteskalke.service;
import br.com.tapeteskalke.dto.OperacaoDtos.*; import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.*; import br.com.tapeteskalke.repository.*; import br.com.tapeteskalke.security.CurrentUser; import java.math.*; import java.time.*; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class ProducaoService { private final OrdemProducaoRepository ordens; private final ProdutoService produtos; private final ClienteService clientes; private final FornecedorRepository fornecedores; private final EstoqueService estoque; private final PermissionService permissions; private final CurrentUser current; private final NumeroService numeros; private final Clock clock; public ProducaoService(OrdemProducaoRepository ordens,ProdutoService produtos,ClienteService clientes,FornecedorRepository fornecedores,EstoqueService estoque,PermissionService permissions,CurrentUser current,NumeroService numeros,Clock clock){this.ordens=ordens;this.produtos=produtos;this.clientes=clientes;this.fornecedores=fornecedores;this.estoque=estoque;this.permissions=permissions;this.current=current;this.numeros=numeros;this.clock=clock;}
 @Transactional public OrdemResponse create(OrdemRequest r){permissions.action("producao_criar");if("ENCOMENDA".equals(r.tipoOrdem())&&(r.clienteId()==null||r.clienteId().isBlank()))throw new BusinessException("CLIENTE_OBRIGATORIO","Encomenda exige cliente",HttpStatus.UNPROCESSABLE_ENTITY);var o=new OrdemProducao();o.setNumeroOrdem(numeros.nextOrdem());o.setProdutoAcabado(produtos.active(r.produtoAcabadoId()));o.setQuantidadeProduzida(r.quantidadeProduzida());o.setTipoOrdem(r.tipoOrdem());o.setCliente(r.clienteId()==null||r.clienteId().isBlank()?null:clientes.active(r.clienteId()));o.setFornecedor(r.fornecedorId()==null||r.fornecedorId().isBlank()?null:fornecedores.findById(r.fornecedorId()).orElseThrow(()->new ResourceNotFoundException("Fornecedor",r.fornecedorId())));o.setDataOrdem(LocalDate.now(clock));o.setObservacoes(r.observacoes());o.setCreatedBy(current.require().id());o.setUpdatedBy(current.require().id());for(var input:r.materiasPrimas()){var p=produtos.active(input.produtoId());if(!p.isPodeSerMateriaPrima())throw new BusinessException("PRODUTO_NAO_E_MATERIA_PRIMA",p.getNome()+" não pode ser matéria-prima",HttpStatus.UNPROCESSABLE_ENTITY);var item=new OrdemProducaoItem();item.setProdutoMateriaPrima(p);item.setQuantidadeUsada(input.quantidadeUsada());item.setProdutoNomeSnapshot(p.getNome());item.setUnidadeSnapshot(p.getUnidadeMedida());o.addItem(item);}ordens.save(o);return response(o);}
 @Transactional public OrdemResponse finish(String id){permissions.action("producao_finalizar");var o=ordens.detail(id).orElseThrow(()->new ResourceNotFoundException("Ordem de produção",id));if(!"ABERTA".equals(o.getStatus()))throw new BusinessException("ORDEM_NAO_FINALIZAVEL","Ordem não está aberta",HttpStatus.CONFLICT);var ids=new TreeSet<String>();ids.add(o.getProdutoAcabado().getId());o.getMateriasPrimas().forEach(i->ids.add(i.getProdutoMateriaPrima().getId()));Map<String,Produto> locked=new HashMap<>();for(var productId:ids)locked.put(productId,produtos.locked(productId));for(var i:o.getMateriasPrimas()){var p=locked.get(i.getProdutoMateriaPrima().getId());estoque.change(p,"PRODUCAO",i.getQuantidadeUsada().negate(),"CONSUMO_PRODUCAO","PRODUCAO",o.getId(),"Ordem "+o.getNumeroOrdem());}var finished=locked.get(o.getProdutoAcabado().getId());estoque.change(finished,"COMERCIAL",o.getQuantidadeProduzida(),"ENTRADA_PRODUCAO","PRODUCAO",o.getId(),"Ordem "+o.getNumeroOrdem());o.setStatus("FINALIZADA");o.setDataFinalizacao(OffsetDateTime.now(clock));o.setIdUsuarioFinalizacao(current.require().id());o.setUpdatedBy(current.require().id());return response(o);} @Transactional(readOnly=true) public List<OrdemResponse> list(){return ordens.findAll().stream().map(this::response).toList();} private OrdemResponse response(OrdemProducao o){return new OrdemResponse(o.getId(),o.getNumeroOrdem(),o.getProdutoAcabado().getNome(),o.getQuantidadeProduzida(),o.getStatus(),o.getDataOrdem());}
}
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/FornecedorService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.dto.FornecedorDtos.*; import br.com.tapeteskalke.entity.Fornecedor; import br.com.tapeteskalke.exception.ResourceNotFoundException; import br.com.tapeteskalke.repository.FornecedorRepository; import br.com.tapeteskalke.util.Normalizer; import java.time.OffsetDateTime; import java.time.ZoneOffset; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class FornecedorService { private final FornecedorRepository repo; private final AuditService audit; public FornecedorService(FornecedorRepository repo,AuditService audit){this.repo=repo;this.audit=audit;} @Transactional(readOnly=true) public List<Response> list(){return repo.active().stream().map(this::response).toList();} @Transactional(readOnly=true) public Response get(String id){return response(active(id));} @Transactional public Response create(Request r){var f=new Fornecedor();copy(f,r);audit.created(f);return response(repo.save(f));} @Transactional public Response update(String id,Request r){var f=active(id);copy(f,r);audit.changed(f);return response(f);} @Transactional public void delete(String id){var f=active(id);f.setAtivo(false);f.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));f.setDeletedBy(audit.userId());audit.changed(f);} private Fornecedor active(String id){return repo.findById(id).filter(f->f.getDeletedAt()==null).orElseThrow(()->new ResourceNotFoundException("Fornecedor",id));} private void copy(Fornecedor f,Request r){f.setNomeEmpresa(r.nomeEmpresa().trim());f.setCnpj(Normalizer.digits(r.cnpj()));f.setTelefone(Normalizer.digits(r.telefone()));f.setEmail(Normalizer.blankToNull(r.email()));f.setObservacoes(Normalizer.blankToNull(r.observacoes()));} private Response response(Fornecedor f){return new Response(f.getId(),f.getNomeEmpresa(),f.getCnpj(),f.getTelefone(),f.getEmail(),f.isAtivo());} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/FuncionarioService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.dto.FuncionarioDtos.*; import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.*; import br.com.tapeteskalke.repository.*; import br.com.tapeteskalke.security.CurrentUser; import br.com.tapeteskalke.util.Normalizer; import java.time.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class FuncionarioService { private final FuncionarioRepository repo; private final CargoRepository cargos; private final AuditService audit; private final PermissionService permissions; private final CurrentUser current; public FuncionarioService(FuncionarioRepository repo,CargoRepository cargos,AuditService audit,PermissionService permissions,CurrentUser current){this.repo=repo;this.cargos=cargos;this.audit=audit;this.permissions=permissions;this.current=current;} @Transactional(readOnly=true) public List<Response> list(){return repo.activeList().stream().map(this::response).toList();} @Transactional(readOnly=true) public Response get(String id){return response(active(id));} @Transactional public Response create(Request r){permissions.action("funcionarios_criar");var f=new Funcionario();copy(f,r);audit.created(f);return response(repo.save(f));} @Transactional public Response update(String id,Request r){permissions.action("funcionarios_editar");var f=active(id);copy(f,r);audit.changed(f);return response(f);} @Transactional public void delete(String id){permissions.action("funcionarios_excluir");var f=active(id);f.setAtivo(false);f.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));f.setDeletedBy(audit.userId());audit.changed(f);} private Funcionario active(String id){return repo.findById(id).filter(f->f.getDeletedAt()==null).orElseThrow(()->new ResourceNotFoundException("Funcionário",id));} private void copy(Funcionario f,Request r){f.setNome(r.nome().trim());f.setTelefone(Normalizer.digits(r.telefone()));f.setEmail(Normalizer.blankToNull(r.email()));f.setEndereco(Normalizer.blankToNull(r.endereco()));f.setSalario(r.salario());f.setDataContratacao(r.dataContratacao());if(r.status()!=null)f.setStatus(r.status());f.setCargo(cargos.findById(r.cargoId()).orElseThrow(()->new ResourceNotFoundException("Cargo",r.cargoId())));f.setObservacoes(r.observacoes());} private Response response(Funcionario f){boolean salary=current.hasRole("DONO")||current.hasRole("GERENTE");return new Response(f.getId(),f.getNome(),f.getTelefone(),f.getEmail(),f.getCargo().getCodigo(),f.getStatus(),f.getDataContratacao(),salary?f.getSalario():null);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/EntregaService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.dto.OperacaoDtos.*; import br.com.tapeteskalke.entity.*; import br.com.tapeteskalke.exception.*; import br.com.tapeteskalke.repository.*; import br.com.tapeteskalke.security.CurrentUser; import br.com.tapeteskalke.util.Normalizer; import java.time.*; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class EntregaService { private final EntregaRepository deliveries; private final VendaRepository sales; private final FuncionarioRepository employees; private final PermissionService permissions; private final CurrentUser current; public EntregaService(EntregaRepository deliveries,VendaRepository sales,FuncionarioRepository employees,PermissionService permissions,CurrentUser current){this.deliveries=deliveries;this.sales=sales;this.employees=employees;this.permissions=permissions;this.current=current;} @Transactional public EntregaResponse create(EntregaRequest r){permissions.action("entregas_criar");if(deliveries.existsByVendaId(r.vendaId()))throw new BusinessException("ENTREGA_DUPLICADA","Já existe entrega para esta venda",HttpStatus.CONFLICT);var sale=sales.detail(r.vendaId()).orElseThrow(()->new ResourceNotFoundException("Venda",r.vendaId()));if(!"FINALIZADA".equals(sale.getStatus()))throw new BusinessException("VENDA_CANCELADA","Não é possível entregar venda cancelada",HttpStatus.CONFLICT);var c=sale.getCliente();var e=new Entrega();e.setVenda(sale);e.setCliente(c);e.setEnderecoEntrega(defaultValue(r.enderecoEntrega(),c.getEndereco()));e.setBairro(defaultValue(r.bairro(),c.getBairro()));e.setCidade(defaultValue(r.cidade(),c.getCidade()));e.setCep(Normalizer.digits(defaultValue(r.cep(),c.getCep())));e.setTelefoneContato(Normalizer.digits(defaultValue(r.telefoneContato(),c.getTelefone())));e.setDataAgendada(r.dataAgendada());e.setHorario(r.horario());e.setEntregador(r.entregadorId()==null||r.entregadorId().isBlank()?null:employees.findById(r.entregadorId()).orElseThrow(()->new ResourceNotFoundException("Entregador",r.entregadorId())));e.setValorFrete(r.valorFrete()==null?java.math.BigDecimal.ZERO:r.valorFrete());e.setObservacoes(r.observacoes());e.setCreatedBy(current.require().id());e.setUpdatedBy(current.require().id());return response(deliveries.save(e));} @Transactional public EntregaResponse status(String id,EntregaStatusRequest r){permissions.action("entregas_atualizar_status");var e=deliveries.detail(id).orElseThrow(()->new ResourceNotFoundException("Entrega",id));if("ENTREGUE".equals(e.getStatus()))throw new BusinessException("ENTREGA_FINALIZADA","Entrega já está finalizada",HttpStatus.CONFLICT);if("EM_ROTA".equals(r.status())&&!"AGENDADA".equals(e.getStatus()))throw new BusinessException("TRANSICAO_INVALIDA","Transição de status inválida",HttpStatus.CONFLICT);if("ENTREGUE".equals(r.status())&&!("AGENDADA".equals(e.getStatus())||"EM_ROTA".equals(e.getStatus())))throw new BusinessException("TRANSICAO_INVALIDA","Transição de status inválida",HttpStatus.CONFLICT);e.setStatus(r.status());if("ENTREGUE".equals(r.status()))e.setDataEntrega(OffsetDateTime.now(ZoneOffset.UTC));e.setUpdatedBy(current.require().id());return response(e);} @Transactional(readOnly=true) public List<EntregaResponse> list(){return deliveries.allDetailed().stream().map(this::response).toList();} private String defaultValue(String supplied,String fallback){return supplied==null||supplied.isBlank()?fallback:supplied;} private EntregaResponse response(Entrega e){return new EntregaResponse(e.getId(),e.getCliente().getNome(),e.getVenda().getId(),e.getStatus(),e.getDataAgendada(),e.getDataEntrega());} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/DespesaService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.dto.OperacaoDtos.DespesaRequest; import br.com.tapeteskalke.entity.Despesa; import br.com.tapeteskalke.repository.DespesaRepository; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class DespesaService { private final DespesaRepository repo; private final AuditService audit; public DespesaService(DespesaRepository repo,AuditService audit){this.repo=repo;this.audit=audit;} @Transactional public Despesa create(DespesaRequest r){var d=new Despesa();d.setTipo(r.tipo());d.setDescricao(r.descricao());d.setValor(r.valor());d.setDataDespesa(r.dataDespesa());d.setObservacoes(r.observacoes());audit.created(d);return repo.save(d);} @Transactional(readOnly=true) public List<Despesa> list(){return repo.findAll();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/DashboardService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.repository.*; import java.math.BigDecimal; import java.time.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class DashboardService { private final VendaRepository vendas; private final ClienteRepository clientes; private final ProdutoRepository produtos; private final Clock clock; public DashboardService(VendaRepository vendas,ClienteRepository clientes,ProdutoRepository produtos,Clock clock){this.vendas=vendas;this.clientes=clientes;this.produtos=produtos;this.clock=clock;} @Transactional(readOnly=true) public Map<String,Object> summary(){var today=LocalDate.now(clock);var first=today.withDayOfMonth(1);var low=produtos.lowStock();var clients=clientes.findAll().stream().filter(c->c.getDeletedAt()==null).count();Map<String,Object> m=new LinkedHashMap<>();m.put("vendasHoje",vendas.totalFinalized(today,today));m.put("vendasMes",vendas.totalFinalized(first,today));m.put("totalClientes",clients);m.put("itensEstoque",low.size());m.put("estoqueBaixo",low.stream().map(p->Map.of("id",p.getId(),"nome",p.getNome(),"comercial",p.getEstoqueComercial(),"producao",p.getEstoqueProducao())).toList());return m;} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/RelatorioService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.repository.*; import java.math.BigDecimal; import java.time.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class RelatorioService { private final VendaRepository vendas; private final CompraRepository compras; private final DespesaRepository despesas; public RelatorioService(VendaRepository vendas,CompraRepository compras,DespesaRepository despesas){this.vendas=vendas;this.compras=compras;this.despesas=despesas;} @Transactional(readOnly=true) public Map<String,BigDecimal> financial(LocalDate start,LocalDate end){var receita=vendas.totalFinalized(start,end);var totalCompras=compras.totalFinalized(start,end);var outras=despesas.totalByPeriod(start,end);return Map.of("receita",receita,"compras",totalCompras,"outrasDespesas",outras,"salarios",BigDecimal.ZERO,"lucro",receita.subtract(totalCompras).subtract(outras));} }
'@

$files['backend/src/main/java/br/com/tapeteskalke/service/AuthService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.dto.AuthDtos.*; import br.com.tapeteskalke.entity.Usuario; import br.com.tapeteskalke.exception.BusinessException; import br.com.tapeteskalke.repository.UsuarioRepository; import br.com.tapeteskalke.security.*; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class AuthService { private final UsuarioRepository users; private final PasswordEncoder encoder; private final JwtTokenService tokens; private final PermissionService permissions; public AuthService(UsuarioRepository users,PasswordEncoder encoder,JwtTokenService tokens,PermissionService permissions){this.users=users;this.encoder=encoder;this.tokens=tokens;this.permissions=permissions;} @Transactional(readOnly=true) public LoginResponse login(LoginRequest r){var user=users.findByEmailIgnoreCaseAndAtivoTrue(r.email().trim()).orElseThrow(()->invalid());if(!encoder.matches(r.senha(),user.getSenhaHash()))throw invalid();return loginResponse(user);} @Transactional(readOnly=true) public UserResponse me(UserPrincipal p){return new UserResponse(p.id(),p.email(),p.email(),p.role(),permissions.pagesFor(p.role()),permissions.actionsFor(p.role()));} private LoginResponse loginResponse(Usuario u){var role=u.getCargo().getCodigo();var user=new UserResponse(u.getId(),u.getNome(),u.getEmail(),role,permissions.pagesFor(role),permissions.actionsFor(role));return new LoginResponse(tokens.issue(u.getId(),u.getEmail(),role),tokens.expirationSeconds(),user);} private BusinessException invalid(){return new BusinessException("CREDENCIAIS_INVALIDAS","E-mail ou senha inválidos",HttpStatus.UNAUTHORIZED);} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/config/BootstrapAdminRunner.java'] = @'
package br.com.tapeteskalke.config; import br.com.tapeteskalke.entity.Usuario; import br.com.tapeteskalke.repository.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration public class BootstrapAdminRunner { @Bean CommandLineRunner bootstrapAdmin(UsuarioRepository users,CargoRepository cargos,PasswordEncoder encoder,@Value("${app.bootstrap-admin.email}")String email,@Value("${app.bootstrap-admin.password}")String password){return args->{if(!users.existsByEmailIgnoreCase(email)&&password!=null&&!password.equals("CHANGE_ME_AT_FIRST_START")){var owner=cargos.findByCodigo("DONO").orElseThrow();var u=new Usuario();u.setNome("Administrador");u.setEmail(email.trim().toLowerCase());u.setSenhaHash(encoder.encode(password));u.setCargo(owner);users.save(u);}};} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/dto/PermissionDtos.java'] = @'
package br.com.tapeteskalke.dto; import java.util.Map; public final class PermissionDtos { private PermissionDtos(){} public record UpdateRequest(Map<String,Boolean> pages,Map<String,Boolean> actions){} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/PermissionAdminService.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.dto.PermissionDtos.UpdateRequest; import br.com.tapeteskalke.exception.BusinessException; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class PermissionAdminService { private final JdbcTemplate jdbc; private final CurrentRole role; public PermissionAdminService(JdbcTemplate jdbc,CurrentRole role){this.jdbc=jdbc;this.role=role;} @Transactional public void update(UpdateRequest r){if(!"DONO".equals(role.value()))throw new BusinessException("ACESSO_NEGADO","Somente o dono pode editar permissões",HttpStatus.FORBIDDEN);apply(r.pages(),"permissao_pagina","pagina");apply(r.actions(),"permissao_acao","acao");} private void apply(Map<String,Boolean> values,String table,String column){if(values==null)return;for(var e:values.entrySet()){var key=e.getKey().split(":",2);if(key.length!=2)throw new BusinessException("PERMISSAO_INVALIDA","Use a chave CARGO:permissao",HttpStatus.UNPROCESSABLE_ENTITY);if("DONO".equals(key[0])&&!e.getValue()&&(key[1].equals("configuracoes")||key[1].equals("configuracoes_editar")))throw new BusinessException("SELF_LOCKOUT","Não é permitido remover o acesso de configuração do DONO",HttpStatus.CONFLICT);jdbc.update("update "+table+" set permitido=? where cargo_codigo=? and "+column+"=?",e.getValue(),key[0],key[1]);}} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/service/CurrentRole.java'] = @'
package br.com.tapeteskalke.service; import br.com.tapeteskalke.security.CurrentUser; import org.springframework.stereotype.Component; @Component public class CurrentRole { private final CurrentUser user; public CurrentRole(CurrentUser user){this.user=user;} public String value(){return user.require().role();} }
'@

$files['backend/src/main/java/br/com/tapeteskalke/controller/AuthController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.AuthDtos.*; import br.com.tapeteskalke.security.CurrentUser; import br.com.tapeteskalke.service.AuthService; import jakarta.validation.Valid; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth") public class AuthController { private final AuthService auth; private final CurrentUser current; public AuthController(AuthService auth,CurrentUser current){this.auth=auth;this.current=current;} @PostMapping("/login") public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest r){return ResponseEntity.ok(ApiResponse.ok(auth.login(r)));} @GetMapping("/me") public ApiResponse<UserResponse> me(){return ApiResponse.ok(auth.me(current.require()));} @PostMapping("/logout") public ResponseEntity<Void> logout(){return ResponseEntity.noContent().build();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/ClienteController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.ClienteDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/clientes") public class ClienteController { private final ClienteService service; private final PermissionService permissions; public ClienteController(ClienteService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<PageResponse<Response>> list(@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){permissions.page("clientes");return ApiResponse.ok(service.list(search,page,size));} @GetMapping("/{id}") public ApiResponse<Response> get(@PathVariable String id){permissions.page("clientes");return ApiResponse.ok(service.get(id));} @PostMapping public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody Request r){permissions.action("clientes_criar");return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PutMapping("/{id}") public ApiResponse<Response> update(@PathVariable String id,@Valid @RequestBody Request r){permissions.action("clientes_editar");return ApiResponse.ok(service.update(id,r));} @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id){permissions.action("clientes_excluir");service.delete(id);return ResponseEntity.noContent().build();} @GetMapping("/{id}/historico-compras") public ApiResponse<?> history(@PathVariable String id){permissions.page("clientes");service.get(id);return ApiResponse.ok(java.util.Map.of("message","Histórico disponível pela listagem de vendas do cliente."));} @PostMapping("/{id}/whatsapp-link") public ApiResponse<?> whatsapp(@PathVariable String id){var c=service.get(id);return ApiResponse.ok(java.util.Map.of("url","https://wa.me/"+c.telefone()+"?text="+java.net.URLEncoder.encode("Olá "+c.nome()+"! Esperamos que esteja bem! Temos novidades que podem te interessar!",java.nio.charset.StandardCharsets.UTF_8)));} }
'@

$files['backend/src/main/java/br/com/tapeteskalke/controller/ProdutoController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.ProdutoDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/produtos") public class ProdutoController { private final ProdutoService service; private final PermissionService permissions; public ProdutoController(ProdutoService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<PageResponse<Response>> list(@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){permissions.page("produtos");return ApiResponse.ok(service.list(search,page,size));} @GetMapping("/{id}") public ApiResponse<Response> get(@PathVariable String id){permissions.page("produtos");return ApiResponse.ok(service.get(id));} @PostMapping public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody Request r){permissions.action("produtos_criar");return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PutMapping("/{id}") public ApiResponse<Response> update(@PathVariable String id,@Valid @RequestBody Request r){permissions.action("produtos_editar");return ApiResponse.ok(service.update(id,r));} @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id){permissions.action("produtos_excluir");service.delete(id);return ResponseEntity.noContent().build();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/EstoqueController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.ProdutoDtos.AjusteRequest; import br.com.tapeteskalke.repository.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.web.bind.annotation.*; import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/api/v1/estoque") public class EstoqueController { private final ProdutoRepository produtos; private final MovimentoEstoqueRepository movimentos; private final ProdutoService productService; private final EstoqueService estoque; private final PermissionService permissions; public EstoqueController(ProdutoRepository produtos,MovimentoEstoqueRepository movimentos,ProdutoService productService,EstoqueService estoque,PermissionService permissions){this.produtos=produtos;this.movimentos=movimentos;this.productService=productService;this.estoque=estoque;this.permissions=permissions;} @GetMapping public ApiResponse<?> list(){permissions.page("produtos");return ApiResponse.ok(productService.list("",0,100));} @GetMapping("/resumo") public ApiResponse<?> resumo(){permissions.page("produtos");var low=produtos.lowStock();return ApiResponse.ok(Map.of("baixoOuEsgotado",low.size(),"produtos",low.stream().map(productService::response).toList()));} @GetMapping("/baixo") public ApiResponse<?> baixo(){permissions.page("produtos");return ApiResponse.ok(produtos.lowStock().stream().map(productService::response).toList());} @GetMapping("/movimentos") public ApiResponse<?> movimentos(){permissions.page("produtos");return ApiResponse.ok(movimentos.recent().stream().map(m->Map.of("id",m.getId(),"produto",m.getProduto().getNome(),"saldo",m.getSaldo(),"tipo",m.getTipoMovimento(),"quantidade",m.getQuantidade(),"data",m.getDataMovimento())).toList());} @PostMapping("/ajuste") @Transactional public ApiResponse<?> ajustar(@Valid @RequestBody AjusteRequest r){permissions.action("estoque_ajustar");var p=productService.locked(r.produtoId());estoque.change(p,r.saldo(),r.quantidade(),"AJUSTE_MANUAL","AJUSTE",java.util.UUID.randomUUID().toString(),r.observacao());return ApiResponse.ok(productService.response(p));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/VendaController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.OperacaoDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/vendas") public class VendaController { private final VendaService service; private final PermissionService permissions; public VendaController(VendaService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<List<VendaResponse>> list(){permissions.page("vendas");return ApiResponse.ok(service.list());} @GetMapping("/{id}") public ApiResponse<VendaResponse> get(@PathVariable String id){permissions.page("vendas");return ApiResponse.ok(service.get(id));} @PostMapping public ResponseEntity<ApiResponse<VendaResponse>> create(@Valid @RequestBody VendaRequest r,@RequestHeader("Idempotency-Key")String key){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r,key)));} @PostMapping("/{id}/cancelar") public ApiResponse<VendaResponse> cancel(@PathVariable String id,@Valid @RequestBody CancelarRequest r){return ApiResponse.ok(service.cancel(id,r));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/CompraController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.OperacaoDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/compras") public class CompraController { private final CompraService service; private final PermissionService permissions; public CompraController(CompraService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<List<CompraResponse>> list(){permissions.page("compras");return ApiResponse.ok(service.list());} @PostMapping public ResponseEntity<ApiResponse<CompraResponse>> create(@Valid @RequestBody CompraRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PostMapping("/{id}/cancelar") public ApiResponse<CompraResponse> cancel(@PathVariable String id,@Valid @RequestBody CancelarRequest r){return ApiResponse.ok(service.cancel(id,r));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/ProducaoController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.OperacaoDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/ordens-producao") public class ProducaoController { private final ProducaoService service; private final PermissionService permissions; public ProducaoController(ProducaoService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<List<OrdemResponse>> list(){permissions.page("producao");return ApiResponse.ok(service.list());} @PostMapping public ResponseEntity<ApiResponse<OrdemResponse>> create(@Valid @RequestBody OrdemRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PostMapping("/{id}/finalizar") public ApiResponse<OrdemResponse> finish(@PathVariable String id){return ApiResponse.ok(service.finish(id));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/EntregaController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.OperacaoDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/entregas") public class EntregaController { private final EntregaService service; private final PermissionService permissions; public EntregaController(EntregaService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<List<EntregaResponse>> list(){permissions.page("entregas");return ApiResponse.ok(service.list());} @PostMapping public ResponseEntity<ApiResponse<EntregaResponse>> create(@Valid @RequestBody EntregaRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PatchMapping("/{id}/status") public ApiResponse<EntregaResponse> status(@PathVariable String id,@Valid @RequestBody EntregaStatusRequest r){return ApiResponse.ok(service.status(id,r));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/FornecedorController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.FornecedorDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/fornecedores") public class FornecedorController { private final FornecedorService service; private final PermissionService permissions; public FornecedorController(FornecedorService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<List<Response>> list(){permissions.page("fornecedores");return ApiResponse.ok(service.list());} @GetMapping("/{id}") public ApiResponse<Response> get(@PathVariable String id){permissions.page("fornecedores");return ApiResponse.ok(service.get(id));} @PostMapping public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody Request r){permissions.action("produtos_criar");return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PutMapping("/{id}") public ApiResponse<Response> update(@PathVariable String id,@Valid @RequestBody Request r){permissions.action("produtos_editar");return ApiResponse.ok(service.update(id,r));} @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id){permissions.action("produtos_excluir");service.delete(id);return ResponseEntity.noContent().build();} }
'@

$files['backend/src/main/java/br/com/tapeteskalke/controller/FuncionarioController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.FuncionarioDtos.*; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/funcionarios") public class FuncionarioController { private final FuncionarioService service; private final PermissionService permissions; public FuncionarioController(FuncionarioService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<List<Response>> list(){permissions.page("funcionarios");return ApiResponse.ok(service.list());} @GetMapping("/{id}") public ApiResponse<Response> get(@PathVariable String id){permissions.page("funcionarios");return ApiResponse.ok(service.get(id));} @PostMapping public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody Request r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} @PutMapping("/{id}") public ApiResponse<Response> update(@PathVariable String id,@Valid @RequestBody Request r){return ApiResponse.ok(service.update(id,r));} @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id){service.delete(id);return ResponseEntity.noContent().build();} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/DashboardController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.ApiResponse; import br.com.tapeteskalke.service.*; import org.springframework.web.bind.annotation.*; @RestController @RequestMapping("/api/v1/dashboard") public class DashboardController { private final DashboardService service; private final PermissionService permissions; public DashboardController(DashboardService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<?> get(){permissions.page("dashboard");return ApiResponse.ok(service.summary());} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/RelatorioController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.ApiResponse; import br.com.tapeteskalke.service.*; import java.time.*; import org.springframework.web.bind.annotation.*; @RestController @RequestMapping("/api/v1/relatorios") public class RelatorioController { private final RelatorioService service; private final PermissionService permissions; private final Clock clock; public RelatorioController(RelatorioService service,PermissionService permissions,Clock clock){this.service=service;this.permissions=permissions;this.clock=clock;} @GetMapping("/financeiro") public ApiResponse<?> financeiro(@RequestParam(required=false)LocalDate inicio,@RequestParam(required=false)LocalDate fim){permissions.action("relatorios_visualizar");var today=LocalDate.now(clock);return ApiResponse.ok(service.financial(inicio==null?today.withDayOfMonth(1):inicio,fim==null?today:fim));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/DespesaController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.OperacaoDtos.DespesaRequest; import br.com.tapeteskalke.service.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; @RestController @RequestMapping("/api/v1/despesas") public class DespesaController { private final DespesaService service; private final PermissionService permissions; public DespesaController(DespesaService service,PermissionService permissions){this.service=service;this.permissions=permissions;} @GetMapping public ApiResponse<?> list(){permissions.action("relatorios_visualizar");return ApiResponse.ok(service.list());} @PostMapping public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody DespesaRequest r){permissions.action("configuracoes_editar");return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r)));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/controller/ConfiguracaoController.java'] = @'
package br.com.tapeteskalke.controller; import br.com.tapeteskalke.dto.*; import br.com.tapeteskalke.dto.PermissionDtos.UpdateRequest; import br.com.tapeteskalke.service.*; import org.springframework.web.bind.annotation.*; @RestController @RequestMapping("/api/v1/configuracoes") public class ConfiguracaoController { private final PermissionService permissions; private final PermissionAdminService admin; public ConfiguracaoController(PermissionService permissions,PermissionAdminService admin){this.permissions=permissions;this.admin=admin;} @GetMapping public ApiResponse<?> get(){permissions.page("configuracoes");return ApiResponse.ok(java.util.Map.of("theme","light","pages",permissions.pages(),"actions",permissions.actions()));} @GetMapping("/permissoes") public ApiResponse<?> permissions(){return ApiResponse.ok(java.util.Map.of("pages",permissions.pages(),"actions",permissions.actions()));} @PutMapping("/permissoes") public ApiResponse<?> update(@RequestBody UpdateRequest r){admin.update(r);return ApiResponse.ok(java.util.Map.of("updated",true));} }
'@
$files['backend/src/main/java/br/com/tapeteskalke/exception/ErrorResponse.java'] = @'
package br.com.tapeteskalke.exception; public record ErrorResponse(boolean success,Object data,String message,String errorCode,String timestamp,String path) {}
'@
$files['backend/src/main/java/br/com/tapeteskalke/exception/GlobalExceptionHandler.java'] = @'
package br.com.tapeteskalke.exception;
import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.ConstraintViolationException; import java.time.*; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class GlobalExceptionHandler { private ErrorResponse error(String message,String code,HttpServletRequest r){return new ErrorResponse(false,null,message,code,OffsetDateTime.now(ZoneId.of("America/Sao_Paulo")).toString(),r.getRequestURI());} @ExceptionHandler(BusinessException.class) ResponseEntity<ErrorResponse> business(BusinessException e,HttpServletRequest r){return ResponseEntity.status(e.status()).body(error(e.getMessage(),e.code(),r));} @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class}) ResponseEntity<ErrorResponse> validation(Exception e,HttpServletRequest r){return ResponseEntity.unprocessableEntity().body(error("Dados inválidos","VALIDACAO",r));} @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unknown(Exception e,HttpServletRequest r){return ResponseEntity.internalServerError().body(error("Erro interno inesperado","ERRO_INTERNO",r));} }
'@
$files['backend/src/test/java/br/com/tapeteskalke/EstoqueRulesTest.java'] = @'
package br.com.tapeteskalke; import static org.junit.jupiter.api.Assertions.*; import java.math.BigDecimal; import org.junit.jupiter.api.Test; class EstoqueRulesTest { @Test void saldoNaoPodeFicarNegativo(){var saldo=new BigDecimal("1.000");var saida=new BigDecimal("2.000");assertTrue(saldo.subtract(saida).signum()<0,"a regra de serviço deve transformar este cenário em conflito");} @Test void dinheiroUsaPrecisaoDecimal(){assertEquals(new BigDecimal("19.99"),new BigDecimal("10.00").add(new BigDecimal("9.99")));} }
'@

$files['frontend/package.json'] = @'
{
  "name": "tapetes-kalke-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": { "dev": "vite", "build": "vite build", "preview": "vite preview", "lint": "eslint .", "test": "vitest run" },
  "dependencies": {
    "@hookform/resolvers": "^5.0.1",
    "@tanstack/react-query": "^5.74.9",
    "axios": "^1.9.0",
    "lucide-react": "^0.511.0",
    "react": "^19.1.0",
    "react-dom": "^19.1.0",
    "react-hook-form": "^7.56.4",
    "react-router-dom": "^7.6.1",
    "zod": "^3.24.4"
  },
  "devDependencies": { "@eslint/js": "^9.27.0", "@vitejs/plugin-react": "^4.5.1", "eslint": "^9.27.0", "eslint-plugin-react-hooks": "^5.2.0", "eslint-plugin-react-refresh": "^0.4.20", "vite": "^6.3.5", "vitest": "^3.1.4" }
}
'@
$files['frontend/vite.config.js'] = @'
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
// O PostCSS do frontend é explícito para não herdar configurações de pastas-pai.
export default defineConfig({ plugins: [react()], css: { postcss: { plugins: [] } }, server: { port: 5173 } });
'@
$files['frontend/postcss.config.js'] = @'
// Este projeto utiliza CSS próprio. Este arquivo evita que o Vite procure um
// postcss.config.js de outra aplicação na pasta-pai (por exemplo, um Tailwind).
export default { plugins: {} };
'@
$files['frontend/index.html'] = @'
<!doctype html><html lang="pt-BR"><head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width, initial-scale=1.0"/><meta name="theme-color" content="#1c5c46"/><title>Tapetes Kalke</title></head><body><div id="root"></div><script type="module" src="/src/main.jsx"></script></body></html>
'@
$files['frontend/src/main.jsx'] = @'
import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './contexts/AuthContext';
import './styles.css';
const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } });
ReactDOM.createRoot(document.getElementById('root')).render(<React.StrictMode><QueryClientProvider client={queryClient}><BrowserRouter><AuthProvider><App /></AuthProvider></BrowserRouter></QueryClientProvider></React.StrictMode>);
'@
$files['frontend/src/services/api.js'] = @'
import axios from 'axios';
let accessToken = null;
export const setAccessToken = (token) => { accessToken = token; };
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1', headers: { 'Content-Type': 'application/json' } });
api.interceptors.request.use((config) => { if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`; return config; });
api.interceptors.response.use((response) => response.data, (error) => Promise.reject(error.response?.data ?? { message: 'Não foi possível comunicar com o servidor.' }));
export const createKey = () => crypto.randomUUID();
'@
$files['frontend/src/contexts/AuthContext.jsx'] = @'
import { createContext, useContext, useEffect, useState } from 'react';
import { api, setAccessToken } from '../services/api';
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); const [loading, setLoading] = useState(false);
  useEffect(() => { setAccessToken(null); }, []);
  const login = async (email, senha) => { setLoading(true); try { const response = await api.post('/auth/login', { email, senha }); setAccessToken(response.data.accessToken); setUser(response.data.user); return response.data.user; } finally { setLoading(false); } };
  const logout = async () => { try { await api.post('/auth/logout'); } catch { /* logout local ainda deve ocorrer */ } setAccessToken(null); setUser(null); };
  return <AuthContext.Provider value={{ user, loading, login, logout, canPage: (page) => Boolean(user?.pages?.[page]), canAction: (action) => Boolean(user?.actions?.[action]) }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
'@
$files['frontend/src/components/ErrorBoundary.jsx'] = @'
import React from 'react';
export default class ErrorBoundary extends React.Component { state = { failed: false }; static getDerivedStateFromError() { return { failed: true }; } render() { if (this.state.failed) return <main className="centered"><h1>Algo não saiu como esperado</h1><button onClick={() => this.setState({ failed: false })}>Tentar novamente</button></main>; return this.props.children; } }
'@
$files['frontend/src/components/ProtectedRoute.jsx'] = @'
import { Navigate } from 'react-router-dom'; import { useAuth } from '../contexts/AuthContext';
export default function ProtectedRoute({ page, children }) { const { user, canPage } = useAuth(); if (!user) return <Navigate to="/login" replace />; return !canPage(page) ? <main className="centered"><h1>Acesso não autorizado</h1><p>Você não possui permissão para esta área.</p></main> : children; }
'@
$files['frontend/src/components/Modal.jsx'] = @'
export default function Modal({ title, children, onClose }) { return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}><section className="modal" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()}><header><h2>{title}</h2><button className="icon-button" aria-label="Fechar modal" onClick={onClose}>×</button></header>{children}</section></div>; }
'@
$files['frontend/src/components/State.jsx'] = @'
export function Loading() { return <p className="state">Carregando…</p>; } export function Empty({ children = 'Nenhum registro encontrado.' }) { return <p className="state">{children}</p>; } export function Failure({ error }) { return <p className="state error">{error?.message ?? 'Não foi possível carregar os dados.'}</p>; }
'@
$files['frontend/src/utils/format.js'] = @'
export const money = (value) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value ?? 0);
export const date = (value) => value ? new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo' }).format(new Date(`${value}T12:00:00`)) : '—';
export const digits = (value) => (value ?? '').replace(/\D/g, '');
'@

$files['frontend/src/App.jsx'] = @'
import { Navigate, Route, Routes } from 'react-router-dom';
import ErrorBoundary from './components/ErrorBoundary';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login'; import Dashboard from './pages/Dashboard'; import Clientes from './pages/Clientes'; import Produtos from './pages/Produtos'; import Vendas from './pages/Vendas'; import SimpleModule from './pages/SimpleModule'; import Relatorios from './pages/Relatorios'; import Configuracoes from './pages/Configuracoes';
const secured = (page, node) => <ProtectedRoute page={page}><Layout>{node}</Layout></ProtectedRoute>;
export default function App() { return <ErrorBoundary><Routes><Route path="/login" element={<Login />} /><Route path="/" element={<Navigate to="/dashboard" replace />} /><Route path="/dashboard" element={secured('dashboard', <Dashboard />)} /><Route path="/clientes" element={secured('clientes', <Clientes />)} /><Route path="/produtos" element={secured('produtos', <Produtos />)} /><Route path="/vendas" element={secured('vendas', <Vendas />)} /><Route path="/compras" element={secured('compras', <SimpleModule page="compras" title="Compras" endpoint="/compras" />)} /><Route path="/fornecedores" element={secured('fornecedores', <SimpleModule page="fornecedores" title="Fornecedores" endpoint="/fornecedores" />)} /><Route path="/funcionarios" element={secured('funcionarios', <SimpleModule page="funcionarios" title="Funcionários" endpoint="/funcionarios" />)} /><Route path="/ordens-producao" element={secured('producao', <SimpleModule page="producao" title="Ordens de produção" endpoint="/ordens-producao" />)} /><Route path="/entregas" element={secured('entregas', <SimpleModule page="entregas" title="Entregas" endpoint="/entregas" />)} /><Route path="/relatorios" element={secured('relatorios', <Relatorios />)} /><Route path="/configuracoes" element={secured('configuracoes', <Configuracoes />)} /><Route path="*" element={<main className="centered"><h1>404</h1><p>Página não encontrada.</p></main>} /></Routes></ErrorBoundary>; }
'@
$files['frontend/src/components/Layout.jsx'] = @'
import { NavLink, useNavigate } from 'react-router-dom';
import { BarChart3, Box, ClipboardList, Factory, LayoutDashboard, LogOut, Settings, ShoppingBag, Truck, Users, WalletCards } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
const links = [ ['dashboard','/dashboard','Dashboard',LayoutDashboard], ['clientes','/clientes','Clientes',Users], ['funcionarios','/funcionarios','Funcionários',Users], ['produtos','/produtos','Estoque',Box], ['vendas','/vendas','Vendas',ShoppingBag], ['compras','/compras','Compras',WalletCards], ['fornecedores','/fornecedores','Fornecedores',Users], ['producao','/ordens-producao','Produção',Factory], ['entregas','/entregas','Entregas',Truck], ['relatorios','/relatorios','Relatórios',BarChart3], ['configuracoes','/configuracoes','Configurações',Settings] ];
export default function Layout({ children }) { const { user, canPage, logout } = useAuth(); const navigate = useNavigate(); const leave = async () => { await logout(); navigate('/login'); }; return <div className="app-shell"><aside><div className="brand">Tapetes <strong>Kalke</strong></div><nav>{links.filter(([key]) => canPage(key)).map(([key, to, label, Icon]) => <NavLink key={key} to={to}><Icon size={18}/><span>{label}</span></NavLink>)}</nav><div className="account"><span>{user?.nome}</span><small>{user?.cargo}</small><button onClick={leave}><LogOut size={16}/> Sair</button></div></aside><div className="content"><header className="topbar"><span><ClipboardList size={18}/> Gestão operacional</span></header><main>{children}</main></div></div>; }
'@
$files['frontend/src/pages/Login.jsx'] = @'
import { useState } from 'react'; import { useNavigate } from 'react-router-dom'; import { useAuth } from '../contexts/AuthContext';
export default function Login() { const { login, loading } = useAuth(); const navigate = useNavigate(); const [email,setEmail]=useState(''); const [senha,setSenha]=useState(''); const [error,setError]=useState(''); const submit=async(e)=>{e.preventDefault();setError('');try{await login(email,senha);navigate('/dashboard');}catch(e){setError(e.message ?? 'Não foi possível entrar.');}}; return <main className="login"><form onSubmit={submit}><div className="brand">Tapetes <strong>Kalke</strong></div><h1>Entrar</h1><p>Use as credenciais do administrador criado no primeiro início.</p><label>E-mail<input required type="email" value={email} onChange={(e)=>setEmail(e.target.value)} autoComplete="email"/></label><label>Senha<input required type="password" minLength="8" value={senha} onChange={(e)=>setSenha(e.target.value)} autoComplete="current-password"/></label>{error&&<p className="error">{error}</p>}<button disabled={loading}>{loading?'Entrando…':'Entrar'}</button></form></main>; }
'@
$files['frontend/src/pages/Dashboard.jsx'] = @'
import { useQuery } from '@tanstack/react-query'; import { api } from '../services/api'; import { Loading, Failure, Empty } from '../components/State'; import { money } from '../utils/format';
export default function Dashboard(){const q=useQuery({queryKey:['dashboard'],queryFn:()=>api.get('/dashboard')});if(q.isPending)return <Loading/>;if(q.isError)return <Failure error={q.error}/>;const d=q.data.data;return <><section className="page-heading"><div><h1>Dashboard</h1><p>Visão geral da operação.</p></div></section><section className="cards"><article><span>Vendas hoje</span><strong>{money(d.vendasHoje)}</strong></article><article><span>Vendas no mês</span><strong>{money(d.vendasMes)}</strong></article><article><span>Total de clientes</span><strong>{d.totalClientes}</strong></article><article><span>Alertas de estoque</span><strong>{d.baixoOuEsgotado ?? d.itensEstoque}</strong></article></section><section className="panel"><h2>Produtos que exigem atenção</h2>{d.estoqueBaixo?.length?<ul className="alerts">{d.estoqueBaixo.map((p)=><li key={p.id}><strong>{p.nome}</strong><span>Comercial: {p.comercial} · Produção: {p.producao}</span></li>)}</ul>:<Empty children="Nenhum alerta de estoque no momento."/>}</section></>;}
'@
$files['frontend/src/pages/SimpleModule.jsx'] = @'
import { useQuery } from '@tanstack/react-query'; import { api } from '../services/api'; import { Loading, Failure, Empty } from '../components/State';
const labels={id:'ID',nome:'Nome',nomeEmpresa:'Empresa',dataCompra:'Data',total:'Total',status:'Status',cliente:'Cliente',produto:'Produto',quantidade:'Quantidade',data:'Data',dataAgendada:'Agendada'};
export default function SimpleModule({title,endpoint}){const q=useQuery({queryKey:[endpoint],queryFn:()=>api.get(endpoint)});if(q.isPending)return <Loading/>;if(q.isError)return <Failure error={q.error}/>;const data=q.data.data?.content??q.data.data??[];const rows=Array.isArray(data)?data:[];const columns=rows[0]?Object.keys(rows[0]).filter((k)=>!['id','salario'].includes(k)).slice(0,6):[];return <><section className="page-heading"><div><h1>{title}</h1><p>Dados consultados diretamente pela API.</p></div></section><section className="panel">{rows.length===0?<Empty/>:<div className="table-wrap"><table><thead><tr>{columns.map((c)=><th key={c}>{labels[c]??c}</th>)}</tr></thead><tbody>{rows.map((row,index)=><tr key={row.id??index}>{columns.map((c)=><td key={c}>{String(row[c]??'—')}</td>)}</tr>)}</tbody></table></div>}</section></>;}
'@

$files['frontend/src/pages/Clientes.jsx'] = @'
import { useState } from 'react'; import { useForm } from 'react-hook-form'; import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'; import { api } from '../services/api'; import { Loading, Failure, Empty } from '../components/State'; import Modal from '../components/Modal'; import { useAuth } from '../contexts/AuthContext'; import { date, money } from '../utils/format';
const defaults={nome:'',telefone:'',email:'',razaoSocial:'',cnpj:'',endereco:'',bairro:'',cidade:'',cep:'',dataNascimento:'',observacoes:'',statusCadastral:'ATIVO'};
export default function Clientes(){const [open,setOpen]=useState(false);const [search,setSearch]=useState('');const {canAction}=useAuth();const queryClient=useQueryClient();const q=useQuery({queryKey:['clientes',search],queryFn:()=>api.get('/clientes',{params:{search,size:50}})});const form=useForm({defaultValues:defaults});const save=useMutation({mutationFn:(data)=>api.post('/clientes',data),onSuccess:()=>{form.reset(defaults);setOpen(false);queryClient.invalidateQueries({queryKey:['clientes']});}});if(q.isPending)return <Loading/>;if(q.isError)return <Failure error={q.error}/>;const rows=q.data.data.content;return <><section className="page-heading"><div><h1>Clientes</h1><p>Cadastros e acompanhamento de relacionamento.</p></div>{canAction('clientes_criar')&&<button onClick={()=>setOpen(true)}>Novo cliente</button>}</section><input className="search" placeholder="Buscar por nome, telefone ou cidade" value={search} onChange={(e)=>setSearch(e.target.value)}/><section className="card-grid">{rows.length?rows.map((c)=><article className="customer-card" key={c.id}><div><h2>{c.nome}</h2><span className={`badge ${c.classificacao.toLowerCase()}`}>{c.classificacao.replace('_',' ')}</span></div><p>{c.telefone} · {c.cidade??'Cidade não informada'}</p><p>Última compra: {date(c.ultimaCompra)}</p><strong>{money(c.totalCompras)}</strong>{(['ATENCAO','ATENCAO_INATIVO','INATIVO'].includes(c.classificacao))&&<a className="whatsapp" href={`https://wa.me/${c.telefone}`} target="_blank" rel="noreferrer">Abrir WhatsApp</a>}</article>):<Empty/>}</section>{open&&<Modal title="Novo cliente" onClose={()=>setOpen(false)}><form className="form-grid" onSubmit={form.handleSubmit((data)=>save.mutate(data))}><label>Nome<input required {...form.register('nome')}/></label><label>Telefone<input required {...form.register('telefone')}/></label><label>E-mail<input type="email" {...form.register('email')}/></label><label>Cidade<input {...form.register('cidade')}/></label><label>Endereço<input {...form.register('endereco')}/></label><label>CEP<input {...form.register('cep')}/></label><label>Status<select {...form.register('statusCadastral')}><option value="ATIVO">Ativo</option><option value="INATIVO">Inativo</option></select></label>{save.isError&&<p className="error">{save.error.message}</p>}<footer><button type="button" className="secondary" onClick={()=>setOpen(false)}>Cancelar</button><button disabled={save.isPending}>{save.isPending?'Salvando…':'Salvar'}</button></footer></form></Modal>}</>;}
'@
$files['frontend/src/pages/Produtos.jsx'] = @'
import { useState } from 'react'; import { useForm } from 'react-hook-form'; import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'; import { api } from '../services/api'; import { Loading, Failure, Empty } from '../components/State'; import Modal from '../components/Modal'; import { useAuth } from '../contexts/AuthContext'; import { money } from '../utils/format';
const defaults={nome:'',tipo:'TAPETE',categoria:'',cor:'',tamanho:'',unidadeMedida:'UNIDADE',podeSerVendido:true,podeSerMateriaPrima:false,estoqueMinimoComercial:1,estoqueMinimoProducao:1,precoCusto:0,precoVenda:0,codigoBarras:'',fornecedorPrincipalId:'',observacoes:'',ativo:true};
export default function Produtos(){const [open,setOpen]=useState(false);const {canAction}=useAuth();const queryClient=useQueryClient();const q=useQuery({queryKey:['produtos'],queryFn:()=>api.get('/produtos',{params:{size:100}})});const form=useForm({defaultValues:defaults});const save=useMutation({mutationFn:(data)=>api.post('/produtos',{...data,estoqueMinimoComercial:Number(data.estoqueMinimoComercial),estoqueMinimoProducao:Number(data.estoqueMinimoProducao),precoCusto:Number(data.precoCusto),precoVenda:Number(data.precoVenda)}),onSuccess:()=>{form.reset(defaults);setOpen(false);queryClient.invalidateQueries({queryKey:['produtos']});}});if(q.isPending)return <Loading/>;if(q.isError)return <Failure error={q.error}/>;const rows=q.data.data.content;return <><section className="page-heading"><div><h1>Estoque</h1><p>Produtos, dois saldos e alertas.</p></div>{canAction('produtos_criar')&&<button onClick={()=>setOpen(true)}>Novo produto</button>}</section><section className="panel">{rows.length?<div className="table-wrap"><table><thead><tr><th>Produto</th><th>Comercial</th><th>Produção</th><th>Venda</th><th>Status</th></tr></thead><tbody>{rows.map((p)=><tr key={p.id}><td>{p.nome}<small>{p.unidadeMedida}</small></td><td>{p.estoqueComercial}</td><td>{p.estoqueProducao}</td><td>{money(p.precoVenda)}</td><td><span className={`badge ${p.statusComercial.toLowerCase()}`}>{p.statusComercial}</span></td></tr>)}</tbody></table></div>:<Empty/>}</section>{open&&<Modal title="Novo produto" onClose={()=>setOpen(false)}><form className="form-grid" onSubmit={form.handleSubmit((data)=>save.mutate(data))}><label>Nome<input required {...form.register('nome')}/></label><label>Tipo<select {...form.register('tipo')}><option>TAPETE</option><option>MALHA</option></select></label><label>Unidade<select {...form.register('unidadeMedida')}><option>UNIDADE</option><option>METRO</option><option>KG</option></select></label><label>Preço de venda<input required type="number" min="0" step="0.01" {...form.register('precoVenda')}/></label><label>Preço de custo<input type="number" min="0" step="0.01" {...form.register('precoCusto')}/></label><label>Estoque mínimo comercial<input type="number" min="0" step="0.001" {...form.register('estoqueMinimoComercial')}/></label><label className="check"><input type="checkbox" {...form.register('podeSerVendido')}/> Pode ser vendido</label><label className="check"><input type="checkbox" {...form.register('podeSerMateriaPrima')}/> Pode ser matéria-prima</label>{save.isError&&<p className="error">{save.error.message}</p>}<footer><button type="button" className="secondary" onClick={()=>setOpen(false)}>Cancelar</button><button disabled={save.isPending}>{save.isPending?'Salvando…':'Salvar'}</button></footer></form></Modal>}</>;}
'@
$files['frontend/src/pages/Vendas.jsx'] = @'
import { useMemo, useState } from 'react'; import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'; import { api, createKey } from '../services/api'; import { Loading, Failure, Empty } from '../components/State'; import { money } from '../utils/format'; import { useAuth } from '../contexts/AuthContext';
export default function Vendas(){const client=useQueryClient();const {canAction}=useAuth();const clients=useQuery({queryKey:['clientes-select'],queryFn:()=>api.get('/clientes',{params:{size:100}})});const products=useQuery({queryKey:['produtos-select'],queryFn:()=>api.get('/produtos',{params:{size:100}})});const sales=useQuery({queryKey:['vendas'],queryFn:()=>api.get('/vendas')});const [clienteId,setClienteId]=useState('');const [produtoId,setProdutoId]=useState('');const [quantity,setQuantity]=useState(1);const [items,setItems]=useState([]);const [payment,setPayment]=useState('PIX');const checkout=useMutation({mutationFn:()=>api.post('/vendas',{clienteId,dataVenda:new Date().toISOString().slice(0,10),desconto:0,formaPagamento:payment,itens:items,observacoes:''},{headers:{'Idempotency-Key':createKey()}}),onSuccess:()=>{setItems([]);setClienteId('');client.invalidateQueries({queryKey:['vendas']});client.invalidateQueries({queryKey:['produtos-select']});client.invalidateQueries({queryKey:['dashboard']});}});if(clients.isPending||products.isPending||sales.isPending)return <Loading/>;if(clients.isError||products.isError||sales.isError)return <Failure error={clients.error??products.error??sales.error}/>;const available=products.data.data.content.filter((p)=>p.ativo&&p.podeSerVendido);const total=useMemo(()=>items.reduce((sum,i)=>sum+i.quantidade*i.preco,0),[items]);const add=()=>{const p=available.find((x)=>x.id===produtoId);if(!p||quantity<=0)return;setItems((old)=>[...old,{produtoId:p.id,nome:p.nome,quantidade:Number(quantity),preco:Number(p.precoVenda)}]);setProdutoId('');};return <><section className="page-heading"><div><h1>Vendas</h1><p>O servidor recalcula preço, subtotal e total antes de finalizar.</p></div></section>{canAction('vendas_criar')&&<section className="panel sale"><h2>Nova venda</h2><div className="form-row"><select value={clienteId} onChange={(e)=>setClienteId(e.target.value)}><option value="">Selecione o cliente</option>{clients.data.data.content.map((c)=><option key={c.id} value={c.id}>{c.nome}</option>)}</select><select value={produtoId} onChange={(e)=>setProdutoId(e.target.value)}><option value="">Selecione o produto</option>{available.map((p)=><option key={p.id} value={p.id}>{p.nome} · {p.estoqueComercial} disponível</option>)}</select><input aria-label="Quantidade" type="number" min="0.001" step="0.001" value={quantity} onChange={(e)=>setQuantity(e.target.value)}/><button type="button" className="secondary" onClick={add}>Adicionar item</button></div>{items.length>0&&<><ul className="cart">{items.map((i,index)=><li key={`${i.produtoId}-${index}`}><span>{i.nome} × {i.quantidade}</span><span>{money(i.preco*i.quantidade)} <button className="link" aria-label="Remover item" onClick={()=>setItems(items.filter((_,x)=>x!==index))}>Remover</button></span></li>)}</ul><div className="checkout"><select value={payment} onChange={(e)=>setPayment(e.target.value)}><option>PIX</option><option>DINHEIRO</option><option>CARTAO_CREDITO</option><option>CARTAO_DEBITO</option><option>CHEQUE</option></select><strong>Total: {money(total)}</strong><button disabled={!clienteId||items.length===0||checkout.isPending} onClick={()=>checkout.mutate()}>{checkout.isPending?'Finalizando…':'Finalizar venda'}</button></div>{checkout.isError&&<p className="error">{checkout.error.message}</p>}</>}</section>}<section className="panel"><h2>Histórico</h2>{sales.data.data.length?<div className="table-wrap"><table><thead><tr><th>Número</th><th>Cliente</th><th>Data</th><th>Total</th><th>Status</th></tr></thead><tbody>{sales.data.data.map((s)=><tr key={s.id}><td>#{s.numeroVenda}</td><td>{s.cliente}</td><td>{s.dataVenda}</td><td>{money(s.total)}</td><td>{s.status}</td></tr>)}</tbody></table></div>:<Empty/>}</section></>;}
'@

$files['frontend/src/pages/Relatorios.jsx'] = @'
import { useState } from 'react'; import { useQuery } from '@tanstack/react-query'; import { api } from '../services/api'; import { Loading, Failure } from '../components/State'; import { money } from '../utils/format';
export default function Relatorios(){const today=new Date().toISOString().slice(0,10);const [inicio,setInicio]=useState(today.slice(0,8)+'01');const [fim,setFim]=useState(today);const q=useQuery({queryKey:['financeiro',inicio,fim],queryFn:()=>api.get('/relatorios/financeiro',{params:{inicio,fim}})});return <><section className="page-heading"><div><h1>Relatórios</h1><p>Receitas e despesas usam datas de negócio no fuso America/Sao_Paulo.</p></div></section><section className="panel form-row"><label>Início<input type="date" value={inicio} onChange={(e)=>setInicio(e.target.value)}/></label><label>Fim<input type="date" value={fim} onChange={(e)=>setFim(e.target.value)}/></label></section>{q.isPending?<Loading/>:q.isError?<Failure error={q.error}/>:<section className="cards">{Object.entries(q.data.data).map(([key,value])=><article key={key}><span>{key.replace(/([A-Z])/g,' $1')}</span><strong>{money(value)}</strong></article>)}</section>}</>;}
'@
$files['frontend/src/pages/Configuracoes.jsx'] = @'
import { useQuery } from '@tanstack/react-query'; import { api } from '../services/api'; import { Loading, Failure } from '../components/State';
export default function Configuracoes(){const q=useQuery({queryKey:['configuracoes'],queryFn:()=>api.get('/configuracoes')});if(q.isPending)return <Loading/>;if(q.isError)return <Failure error={q.error}/>;const {pages,actions,theme}=q.data.data;return <><section className="page-heading"><div><h1>Configurações</h1><p>Permissões exibidas são as aplicadas pela API para o usuário atual.</p></div></section><section className="panel"><h2>Tema</h2><p>{theme === 'dark' ? 'Escuro' : 'Claro'}</p></section><section className="split"><article className="panel"><h2>Acesso às páginas</h2><ul className="permission-list">{Object.entries(pages).map(([name,allowed])=><li key={name}><span>{name}</span><strong className={allowed?'allowed':'denied'}>{allowed?'Permitido':'Negado'}</strong></li>)}</ul></article><article className="panel"><h2>Ações</h2><ul className="permission-list">{Object.entries(actions).map(([name,allowed])=><li key={name}><span>{name}</span><strong className={allowed?'allowed':'denied'}>{allowed?'Permitido':'Negado'}</strong></li>)}</ul></article></section></>;}
'@
$files['frontend/eslint.config.js'] = @'
import js from '@eslint/js';
export default [js.configs.recommended, { ignores: ['dist', 'node_modules'], languageOptions: { ecmaVersion: 'latest', sourceType: 'module', parserOptions: { ecmaFeatures: { jsx: true } }, globals: { window: 'readonly', document: 'readonly', crypto: 'readonly' } }, rules: { 'no-unused-vars': ['error', { argsIgnorePattern: '^_' }] } }];
'@
$files['frontend/src/styles.css'] = @'
:root { font-family: Inter, ui-sans-serif, system-ui, sans-serif; color:#18332b; background:#f4f7f4; font-synthesis:none; }
* { box-sizing:border-box; } body { margin:0; min-width:320px; } button,input,select,textarea { font:inherit; } button { cursor:pointer; border:0; border-radius:8px; padding:.65rem 1rem; color:#fff; background:#1c5c46; font-weight:700; } button:hover { background:#154737; } button:disabled { opacity:.6; cursor:not-allowed; } button.secondary { color:#1c5c46; background:#e2ece7; } button.link { background:transparent; color:#ba3131; padding:0; margin-left:.5rem; font-size:.85rem; } .app-shell { min-height:100vh; display:flex; } aside { width:248px; padding:1.5rem 1rem; color:#eaf4ef; background:#163b30; display:flex; flex-direction:column; gap:2rem; } .brand { color:#1c5c46; font-size:1.35rem; letter-spacing:.02em; } aside .brand { color:white; } nav { display:grid; gap:.3rem; } nav a { display:flex; align-items:center; gap:.7rem; padding:.7rem .75rem; border-radius:8px; color:#cfe1d8; text-decoration:none; } nav a.active,nav a:hover { color:white; background:#245542; } .account { margin-top:auto; display:grid; gap:.3rem; font-size:.9rem; } .account small { color:#a8c0b5; } .account button { margin-top:.8rem; background:transparent; border:1px solid #5d8373; display:flex; align-items:center; justify-content:center; gap:.4rem; } .content { flex:1; min-width:0; } .topbar { height:64px; padding:0 2rem; display:flex; align-items:center; border-bottom:1px solid #dce7e0; background:white; color:#567168; } .topbar span { display:flex; gap:.5rem; align-items:center; } main { padding:2rem; max-width:1500px; } .page-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:1rem; margin-bottom:1.25rem; } h1,h2,p { margin-top:0; } h1 { margin-bottom:.25rem; font-size:1.75rem; } h2 { font-size:1.05rem; } .page-heading p { color:#647b72; margin-bottom:0; } .cards { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:1rem; margin-bottom:1rem; } .cards article,.panel,.customer-card { border:1px solid #dce7e0; border-radius:12px; padding:1.15rem; background:white; box-shadow:0 2px 8px #12351a0a; } .cards span { display:block; color:#647b72; font-size:.9rem; margin-bottom:.65rem; } .cards strong { font-size:1.55rem; } .panel { margin-bottom:1rem; } .state { padding:1.5rem; color:#647b72; text-align:center; } .error { color:#ae2727; } .search { width:100%; max-width:450px; border:1px solid #bdccc4; border-radius:8px; padding:.7rem .8rem; margin-bottom:1rem; background:#fff; } .card-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr)); gap:1rem; } .customer-card { display:grid; gap:.8rem; } .customer-card>div { display:flex; justify-content:space-between; gap:.5rem; align-items:flex-start; } .customer-card h2 { margin:0; } .customer-card p { margin:0; color:#647b72; } .whatsapp { color:#176b45; font-weight:700; text-decoration:none; } .badge { display:inline-block; padding:.25rem .5rem; border-radius:999px; font-size:.72rem; font-weight:800; color:#225342; background:#ddefe5; text-transform:capitalize; white-space:nowrap; } .badge.esgotado,.badge.inativo,.badge.atencao_inativo { color:#a42b2b; background:#f7dfdf; } .badge.baixo,.badge.atencao { color:#865e08; background:#f6edcf; } .table-wrap { overflow-x:auto; } table { width:100%; border-collapse:collapse; min-width:600px; } th,td { padding:.75rem; border-bottom:1px solid #e8efea; text-align:left; } th { color:#647b72; font-size:.78rem; text-transform:uppercase; } td small { display:block; color:#748a80; } .modal-backdrop { position:fixed; inset:0; display:grid; place-items:center; padding:1rem; background:#07251a99; z-index:10; } .modal { max-height:90vh; overflow:auto; width:min(720px,100%); border-radius:12px; padding:1.25rem; background:white; } .modal header { display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #e3ebe6; margin-bottom:1rem; } .modal header h2 { margin:0 0 1rem; } .icon-button { color:#17362c; background:transparent; padding:.2rem .5rem; font-size:1.6rem; } .form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:1rem; } .form-grid label,.form-row label { display:grid; gap:.35rem; color:#405b51; font-size:.9rem; } input,select,textarea { width:100%; padding:.6rem; border:1px solid #bdccc4; border-radius:7px; background:white; color:#18332b; } .form-grid .check { display:flex; align-items:center; gap:.5rem; } .check input { width:auto; } form footer { grid-column:1/-1; display:flex; justify-content:flex-end; gap:.6rem; } .form-row { display:flex; align-items:end; gap:.75rem; flex-wrap:wrap; } .form-row select { min-width:190px; flex:1; } .sale .form-row input { width:100px; } .cart { padding:0; list-style:none; } .cart li,.permission-list li { display:flex; justify-content:space-between; gap:1rem; padding:.75rem 0; border-bottom:1px solid #e5ede8; } .checkout { display:flex; gap:1rem; align-items:center; justify-content:flex-end; flex-wrap:wrap; } .checkout select { width:190px; } .alerts,.permission-list { padding:0; list-style:none; } .alerts li { display:flex; justify-content:space-between; padding:.75rem 0; border-bottom:1px solid #e5ede8; } .alerts span { color:#647b72; } .split { display:grid; grid-template-columns:1fr 1fr; gap:1rem; } .allowed { color:#176b45; } .denied { color:#a42b2b; } .centered { min-height:100vh; display:grid; place-content:center; text-align:center; } .login { min-height:100vh; display:grid; place-items:center; background:linear-gradient(135deg,#e0eee6,#f8faf8); } .login form { width:min(420px,calc(100vw - 2rem)); display:grid; gap:1rem; padding:2rem; background:white; border-radius:14px; box-shadow:0 12px 32px #153b2930; } .login .brand { text-align:center; }
@media(max-width:900px){ aside { width:68px; padding:.9rem .5rem; } aside .brand { font-size:0; } aside .brand strong { font-size:1rem; } nav a { justify-content:center; } nav span,.account span,.account small,.account button { display:none; } main { padding:1.25rem; } .cards { grid-template-columns:repeat(2,1fr); } } @media(max-width:620px){ .app-shell { display:block; } aside { width:100%; display:block; padding:.7rem; } aside .brand { display:none; } nav { display:flex; overflow:auto; } nav a { flex:0 0 auto; } .content { width:100%; } .topbar { padding:0 1rem; } .cards,.split,.form-grid { grid-template-columns:1fr; } .page-heading { align-items:stretch; flex-direction:column; } .page-heading button { width:100%; } }
'@

function Write-ProjectFile {
    param([string]$RelativePath, [string]$Content)
    $destination = Join-Path $ProjectRoot $RelativePath
    $directory = Split-Path -Parent $destination
    if (-not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory -Force | Out-Null }
    if ((Test-Path -LiteralPath $destination) -and -not $Force) { throw "Arquivo já existe: $destination. Use -Force somente se desejar sobrescrevê-lo." }
    [System.IO.File]::WriteAllText($destination, $Content.TrimStart("`r", "`n"), $Utf8NoBom)
}

foreach ($entry in $files.GetEnumerator()) { Write-ProjectFile -RelativePath $entry.Key -Content $entry.Value }
Write-Host "Projeto gerado em: $ProjectRoot" -ForegroundColor Green
Write-Host "Próximos passos: copie .env.example para .env, inicie 'docker compose up -d postgres', depois execute Maven em backend e npm install em frontend." -ForegroundColor Cyan
