# PROMPT MASTER DEFINITIVO — SISTEMA TAPETES KALKE
## Especificação funcional, técnica e arquitetural para implementação independente do Base44

> **Objetivo deste documento:** servir como especificação mestre e prompt-base para uma IA de desenvolvimento construir o Sistema Tapetes Kalke do zero, sem depender do Base44, utilizando **Java 21 + Spring Boot** no back-end, **PostgreSQL** como banco de dados e **JavaScript** no front-end.
>
> Este documento consolida o conteúdo dos arquivos de arquitetura/orientações fornecidos e as características visuais e funcionais observadas na demonstração do sistema existente. Onde houver decisão ainda não definida nos materiais, isso deve ser tratado explicitamente como **PENDÊNCIA DE DEFINIÇÃO**, e não como regra inventada.

---

# 1. REGRA PRINCIPAL PARA A IA DE DESENVOLVIMENTO

Você está implementando um sistema administrativo chamado **Tapetes Kalke**.

A aplicação existente no Base44 deve ser tratada como **referência funcional e visual**, mas **não deve ser utilizada como dependência de runtime**.

A nova aplicação deverá:

- ser totalmente independente do Base44;
- possuir back-end próprio;
- possuir banco de dados PostgreSQL próprio;
- expor API REST;
- possuir autenticação e autorização próprias;
- possuir regras de negócio no back-end;
- utilizar transações para operações que alterem estoque, vendas, compras e produção;
- possuir front-end em JavaScript;
- ser preparada para futuras integrações com WhatsApp, e-mail, Pix, Mercado Livre e automações com IA;
- preservar os fluxos de negócio descritos neste documento;
- possuir estrutura profissional, testável, versionável e preparada para manutenção.

**Não copiar código do Base44 automaticamente.**

O Base44 serve como referência de:

- telas;
- nomenclaturas;
- fluxos;
- regras já implementadas;
- campos;
- permissões;
- comportamentos;
- mensagens;
- componentes visuais;
- validações;
- relações entre módulos.

Os fontes do Base44 foram fornecidos e analisados nesta revisão. Eles são utilizados como **fonte de levantamento funcional e de identificação de defeitos**, e não como dependência ou código que deva permanecer no projeto final.

---

# 2. DECISÕES TECNOLÓGICAS OBRIGATÓRIAS

## 2.1 Back-end

- Java 21.
- Spring Boot.
- Maven.
- Spring Web.
- Spring Data JPA.
- Hibernate.
- Spring Security.
- Bean Validation.
- PostgreSQL.
- Flyway para versionamento do banco.
- OpenAPI/Swagger.
- JUnit 5.
- Mockito.
- Testcontainers para testes de integração, quando aplicável.
- Jackson para JSON.
- BCrypt ou outro encoder seguro suportado pelo Spring Security para senhas.
- JWT para autenticação stateless, salvo decisão posterior por sessão tradicional.

## 2.2 Front-end

O front-end deverá ser escrito em **JavaScript**, sem TypeScript.

Arquitetura recomendada:

- React;
- Vite;
- CSS próprio ou biblioteca de componentes leve;
- React Router;
- Axios;
- biblioteca de ícones;
- biblioteca de gráficos;
- biblioteca para notificações/toasts;
- biblioteca de drag-and-drop para o Kanban de entregas;
- biblioteca de máscaras de campos, se necessário;
- gerenciamento de estado local/global apenas quando realmente necessário.

A referência existente utiliza React + Vite + Tailwind + shadcn/Radix + lucide-react + react-router-dom + TanStack Query + recharts + date-fns. Isso pode ser reproduzido como referência técnica do front-end, porém toda a comunicação de dados deverá ser refeita para a API própria.

---

# 3. PRINCÍPIOS DE IMPLEMENTAÇÃO

## 3.1 Regra crítica

**Não colocar regra de negócio exclusivamente no JavaScript.**

O front-end poderá:

- validar formato;
- validar campos obrigatórios antes do envio;
- impedir ações obviamente inválidas;
- exibir mensagens;
- controlar UX;
- calcular valores preliminares para exibição.

Porém, o back-end deverá validar novamente:

- permissões;
- estoque disponível;
- existência de entidades;
- valores;
- relacionamentos;
- alterações de saldo;
- regras de venda;
- regras de produção;
- regras de cancelamento;
- regras financeiras.

Nunca confiar em dados enviados pelo navegador.

## 3.2 Transações

Operações compostas deverão ser transacionais.

Exemplo:

**Finalização da venda**

1. validar usuário;
2. validar permissão;
3. validar cliente;
4. validar vendedor;
5. validar produtos;
6. validar estoque;
7. calcular total novamente no servidor;
8. gravar venda;
9. gravar itens;
10. gerar movimentos de estoque;
11. atualizar cliente;
12. registrar notificações;
13. confirmar transação.

Se qualquer etapa crítica falhar, a operação deverá sofrer rollback.

O mesmo princípio vale para:

- compras;
- finalização de ordem de produção;
- cancelamentos;
- ajustes de estoque.

---

# 4. NOMENCLATURA

## 4.1 Banco de dados

As regras fornecidas para o banco são obrigatórias.

### IDs

Todos os campos de ID das tabelas de negócio deverão ser:

```sql
varchar(38)
```

A aplicação deverá gerar IDs únicos de forma segura.

Uma opção recomendada é UUID armazenado como texto com tamanho compatível ao padrão definido.

### Foreign Keys

Toda FK deverá seguir:

```text
id_(nome_tabela_referencia)
```

Exemplos:

```text
id_usuario
id_funcionario
id_cargo
id_cliente
id_produto
id_fornecedor
```

### Índices

Padrão:

```text
idx_(nome)
```

Exemplos:

```text
idx_usuario_email
idx_id_cargo_usuario
```

Os índices deverão ser criados conforme os filtros e joins reais da aplicação.

Não criar índice indiscriminadamente.

### Foreign Keys

Padrão:

```text
fk_(tabela_origem)_X_(tabela_destino)
```

Exemplo:

```text
fk_funcionario_X_cargo
```

---

# 5. TABELAS DE VALORES FIXOS / UTILITÁRIOS

Valores que poderiam ser tratados como Enum fixo no sistema deverão ser modelados de acordo com a orientação fornecida, utilizando tabelas no schema:

```text
utilitarios
```

Padrão:

```text
utilitarios_(nome_tabela)
```

Exemplos:

```text
utilitarios_unidade_medida
utilitarios_tipo_produto
```

As tabelas utilitárias deverão possuir ID:

```text
serial
```

com valores sequenciais:

```text
1
2
3
4
5
...
```

A aplicação deverá consultar esses valores e não espalhar números mágicos pelo código.

Exemplo:

```text
utilitarios_forma_pagamento
utilitarios_status_cliente
utilitarios_status_produto
utilitarios_tipo_estoque
utilitarios_status_venda
utilitarios_status_entrega
utilitarios_tipo_ordem_producao
utilitarios_status_ordem_producao
utilitarios_status_funcionario
utilitarios_tipo_despesa
```

Essas tabelas são uma **proposta de expansão da regra existente**. O nome exato e os valores finais deverão ser conferidos com as classes/código do Base44 quando forem disponibilizados.

---

# 6. ARQUITETURA DO BACK-END

Utilizar arquitetura em camadas, com clara separação de responsabilidades.

Estrutura recomendada:

```text
src/main/java/br/com/tapeteskalke/
├── TapetesKalkeApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   ├── JacksonConfig.java
│   └── TransactionConfig.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenService.java
│   ├── CustomUserDetailsService.java
│   ├── AuthenticatedUser.java
│   └── SecurityConstants.java
│
├── controller/
│   ├── auth/
│   ├── dashboard/
│   ├── clientes/
│   ├── funcionarios/
│   ├── produtos/
│   ├── estoque/
│   ├── vendas/
│   ├── entregas/
│   ├── compras/
│   ├── fornecedores/
│   ├── producao/
│   ├── relatorios/
│   ├── integracoes/
│   ├── configuracoes/
│   └── utilitarios/
│
├── service/
│   ├── auth/
│   ├── dashboard/
│   ├── clientes/
│   ├── funcionarios/
│   ├── produtos/
│   ├── estoque/
│   ├── vendas/
│   ├── entregas/
│   ├── compras/
│   ├── fornecedores/
│   ├── producao/
│   ├── relatorios/
│   ├── notificacoes/
│   └── configuracoes/
│
├── repository/
│   ├── ...
│
├── entity/
│   ├── ...
│
├── dto/
│   ├── request/
│   └── response/
│
├── mapper/
│   └── ...
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── AccessDeniedBusinessException.java
│   ├── ValidationException.java
│   └── ErrorResponse.java
│
├── enums/
│   └── ...
│
├── specification/
│   └── ...
│
├── notification/
│   ├── EmailNotificationService.java
│   ├── WhatsAppNotificationService.java
│   └── NotificationService.java
│
└── util/
    ├── DateUtils.java
    ├── MoneyUtils.java
    └── PhoneUtils.java
```

---

# 7. PADRÃO ENTITY / DTO / SESSION / CONTROLLER

Foi definida explicitamente a necessidade de distinguir operações de salvar, alterar e excluir.

Para cada tela/tabela relevante, deverá existir:

- Entity;
- Repository;
- Service/Session de consulta;
- Service/Session de inclusão;
- Service/Session de alteração;
- Service/Session de exclusão;
- DTO de inclusão;
- DTO de alteração;
- DTO de resposta;
- Controller REST;
- validações correspondentes.

Exemplo:

```text
Cliente
├── Cliente.java
├── ClienteRepository.java
├── ClienteConsultaService.java
├── ClienteSalvarService.java
├── ClienteAlterarService.java
├── ClienteExcluirService.java
├── ClienteConsultaController.java
├── ClienteSalvarController.java
├── ClienteAlterarController.java
├── ClienteExcluirController.java
├── ClienteSalvarRequest.java
├── ClienteAlterarRequest.java
└── ClienteResponse.java
```

**Observação:** caso seja tecnicamente mais limpo utilizar um único `ClienteController` e um único `ClienteService` internamente organizado, isso deverá ser permitido apenas se o prompt/implementação continuar preservando a separação clara das operações e endpoints. A exigência original de classes distintas deve ser tratada como requisito arquitetural de referência.

---

# 8. API REST

Base sugerida:

```text
/api/v1
```

Todos os endpoints deverão:

- possuir HTTP method correto;
- retornar status HTTP correto;
- validar entrada;
- respeitar autorização;
- retornar JSON padronizado;
- registrar logs;
- utilizar paginação em listas grandes;
- evitar retornar entidades JPA diretamente.

---

# 9. PADRÃO DE RESPOSTA DA API

### Sucesso de consulta

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

### Erro

```json
{
  "success": false,
  "data": null,
  "message": "Mensagem amigável",
  "errorCode": "CLIENTE_NAO_ENCONTRADO",
  "timestamp": "2026-01-01T10:00:00-03:00",
  "path": "/api/v1/clientes/123"
}
```

Para listas paginadas:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "message": null
}
```

---

# 10. AUTENTICAÇÃO

Implementar:

- login;
- logout no cliente;
- expiração de token;
- refresh token, se adotado;
- senha armazenada com hash;
- bloqueio de rotas protegidas;
- usuário autenticado disponível no contexto do front-end;
- autorização por cargo;
- autorização por ação.

Endpoint sugerido:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "usuario@empresa.com",
  "senha": "********"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 3600,
    "user": {
      "id": "...",
      "nome": "...",
      "email": "...",
      "cargo": "GERENTE"
    }
  }
}
```

---

# 11. CARGOS

Cargos identificados na aplicação:

```text
Dono
Gerente
Vendedor
Estoquista
Entregador
```

A capitalização de exibição poderá ser amigável, mas o código deverá utilizar identificadores estáveis.

Sugestão:

```text
DONO
GERENTE
VENDEDOR
ESTOQUISTA
ENTREGADOR
```

---

# 12. AUTORIZAÇÃO POR PÁGINA

Permissões padrão observadas:

| Página | Dono | Gerente | Vendedor | Estoquista | Entregador |
|---|---:|---:|---:|---:|---:|
| Dashboard | SIM | SIM | NÃO | NÃO | NÃO |
| Clientes | SIM | SIM | SIM | NÃO | NÃO |
| Funcionários | SIM | SIM | NÃO | NÃO | NÃO |
| Estoque | SIM | SIM | SIM | SIM | NÃO |
| Vendas | SIM | SIM | SIM | NÃO | NÃO |
| Relatórios | SIM | SIM | NÃO | NÃO | NÃO |
| Compras | SIM | SIM | NÃO | NÃO | NÃO |
| Fornecedores | SIM | SIM | NÃO | NÃO | NÃO |
| Ordem de Produção | SIM | SIM | NÃO | SIM | NÃO |
| Integrações | SIM | SIM | NÃO | NÃO | NÃO |
| Configurações | SIM | SIM | NÃO | NÃO | NÃO |
| Entregas | SIM | SIM | NÃO | NÃO | SIM |

**Observação:** o documento original menciona Entregas na arquitetura/telas, embora a lista textual inicial de módulos não tenha separado essa tela como item próprio. Ela deverá ser implementada.

---

# 13. AUTORIZAÇÃO POR AÇÃO

Deverá existir uma estrutura configurável de permissões por ação.

Exemplos já identificados:

```text
clientes_criar
clientes_editar
clientes_excluir

produtos_criar
produtos_editar
produtos_excluir

vendas_criar
vendas_cancelar

funcionarios_criar
funcionarios_editar
funcionarios_excluir

entregas_criar
entregas_editar
```

A estrutura deve ser extensível para todos os módulos.

A autorização deve ser aplicada:

1. no menu;
2. no front-end;
3. no endpoint do back-end.

**Nunca utilizar somente ocultação de botão como segurança.**

---

# 14. DASHBOARD

## Objetivo

Demonstrar rapidamente as principais informações gerenciais da empresa.

## Cards

Deve apresentar:

- Vendas Hoje;
- Vendas do Mês;
- Total de Clientes;
- Itens em Estoque.

## Alertas

Deve apresentar:

- produtos com estoque baixo;
- produtos esgotados;
- clientes sem compra há mais de dois meses.

## Regras

Vendas do dia:

```text
status = FINALIZADA
data_venda dentro do dia atual
```

Vendas do mês:

```text
status = FINALIZADA
data_venda dentro do mês atual
```

Clientes:

```text
quantidade total de clientes
```

Estoque:

O conceito de "itens em estoque" deverá ser definido no código conforme a necessidade final: quantidade de produtos cadastrados ou soma das quantidades. A implementação não deve assumir silenciosamente qual das duas interpretações é correta.

---

# 15. CLIENTES

## Objetivo

Gerenciar a base de clientes e identificar clientes que precisam de reativação.

## Campos identificados

- nome;
- telefone;
- e-mail;
- razão social;
- CNPJ;
- endereço;
- bairro;
- cidade;
- CEP;
- data de nascimento;
- observações;
- última compra;
- total de compras;
- status.

## Listagem

A interface existente utiliza cards.

Cada card apresenta:

- nome;
- status;
- telefone;
- cidade;
- última compra;
- ações;
- botão de WhatsApp quando aplicável.

## Busca

Pesquisar por:

- nome;
- telefone;
- cidade;
- status.

## Status dinâmico

Com base no comportamento existente:

```text
status cadastral = INATIVO
    -> Inativo

sem última compra
    -> Novo

mais de 2 meses sem compra
    -> Atenção (Inativo)

mais de 1 mês sem compra
    -> Atenção

caso contrário
    -> Ativo
```

A regra de prioridade deverá seguir essa ordem para evitar ambiguidades.

## WhatsApp

Quando:

```text
mesesSemComprar >= 2
OU
status cadastral = inativo
```

mostrar o botão de WhatsApp.

Mensagem de referência:

```text
Olá {NOME DO CLIENTE}! Esperamos que esteja bem! Notamos que faz um tempo que não vem aqui na loja. Está precisando de novos tapetes ou malhas? Temos várias novidades que podem te interessar!
```

No estágio inicial:

- abrir WhatsApp com mensagem pré-preenchida.

No estágio futuro:

- automação real;
- envio programado;
- portfólio;
- controle de campanhas;
- IA para sugestão de mensagem;
- análise de retorno.

---

# 16. CLIENTE — MELHORIAS NECESSÁRIAS

A nova implementação deverá prever, mas não obrigatoriamente ativar na primeira versão:

- histórico de compras;
- valor total já comprado;
- quantidade de compras;
- data da primeira compra;
- data da última compra;
- ticket médio;
- produtos comprados anteriormente;
- observações internas;
- contatos;
- endereço completo estruturado;
- histórico de mensagens/campanhas;
- controle de consentimento para contatos, se necessário;
- identificação de cliente ativo/inativo de forma objetiva.

---

# 17. FUNCIONÁRIOS

## Campos

- nome;
- cargo;
- telefone;
- e-mail;
- endereço;
- salário;
- data de contratação;
- status;
- observações.

## Status

- ativo;
- inativo;
- férias.

## Cargo

- vendedor;
- gerente;
- entregador;
- estoquista.

O dono é um perfil administrativo do sistema e não deve ser automaticamente tratado como funcionário comum sem uma regra explícita.

## Permissões

- dono: criar, consultar, alterar e excluir;
- gerente: criar, consultar e alterar;
- outros: conforme configuração.

O salário é informação sensível e deve ser protegido por autorização.

---

# 18. PRODUTOS / ESTOQUE

## Tipos

- tapete;
- malha.

O sistema precisa distinguir:

### Matéria-prima

Exemplos:

- fios;
- malhas;
- bases.

### Produto acabado

Exemplos:

- tapetes prontos;
- produtos preparados para venda.

## Campos

- nome;
- tipo;
- grupo de estoque;
- categoria;
- cor;
- tamanho;
- unidade de medida;
- estoque atual;
- estoque mínimo;
- preço de custo;
- preço de venda;
- fornecedor;
- código de barras;
- observações;
- ativo.

## Unidade

Valores identificados:

- unidade;
- metro;
- kg.

A tabela de utilitário deverá permitir expansão futura.

---

# 19. STATUS DO ESTOQUE

## Esgotado

```text
estoque_atual = 0
```

## Baixo

```text
estoque_atual <= estoque_minimo
```

## OK

```text
estoque_atual > estoque_minimo
```

**Ordem de avaliação obrigatória:**

1. esgotado;
2. baixo;
3. OK.

Isso evita classificar um produto com estoque 0 apenas como "baixo".

---

# 20. MOVIMENTAÇÃO DE ESTOQUE

A nova aplicação deve possuir histórico de movimentação.

Isso é uma melhoria arquitetural essencial para garantir rastreabilidade.

Criar conceito de:

```text
movimento_estoque
```

Sugestão de campos:

```text
id
id_produto
tipo_movimento
origem_movimento
id_origem
quantidade
estoque_anterior
estoque_posterior
data_movimento
id_usuario
observacao
```

Tipos possíveis:

```text
ENTRADA_COMPRA
SAIDA_VENDA
CONSUMO_PRODUCAO
ENTRADA_PRODUCAO
AJUSTE_MANUAL
ESTORNO_VENDA
ESTORNO_PRODUCAO
```

Os nomes finais devem ser revisados antes de congelar o banco.

---

# 21. VENDAS

## Campos

- número da venda;
- cliente;
- vendedor;
- data;
- total;
- desconto;
- forma de pagamento;
- status;
- observações;
- itens.

## Item

- produto;
- quantidade;
- preço unitário;
- subtotal.

## Formas de pagamento identificadas

- PIX;
- dinheiro;
- cartão de crédito;
- cartão de débito;
- cheque (existente no modelo original, embora a interface observada liste quatro opções).

**Não remover cheque silenciosamente sem decisão funcional.**

## Regras

Ao finalizar:

1. validar cliente;
2. validar vendedor, quando obrigatório;
3. validar itens;
4. garantir quantidade > 0;
5. validar estoque;
6. calcular subtotal no servidor;
7. calcular desconto;
8. calcular total;
9. gravar venda;
10. gravar itens;
11. baixar estoque;
12. criar movimentos de estoque;
13. atualizar dados de compra do cliente;
14. verificar alertas;
15. disparar notificações configuradas.

---

# 22. NÚMERO DA VENDA

O sistema deverá gerar número de venda seguro e sequencial ou conforme regra definida.

Não gerar número confiando no JavaScript.

Recomendação:

- sequence própria no PostgreSQL;
- ou mecanismo transacional equivalente.

O número deverá ser único.

---

# 23. CANCELAMENTO DE VENDA

Cancelar venda não é simplesmente excluir o registro.

Recomendação:

```text
status = CANCELADA
```

e:

- gerar estorno de estoque;
- registrar usuário responsável;
- registrar data/hora;
- registrar motivo;
- impedir alteração indevida de uma venda já cancelada.

A exclusão física da venda deverá ser evitada para preservar auditoria, salvo uma decisão expressa de negócio.

---

# 24. PIX

## Primeira versão

Ao finalizar venda com PIX:

1. abrir modal;
2. exibir QR Code estático;
3. exibir informações do PIX da loja;
4. permitir concluir o fluxo.

## Segunda etapa

Implementar PIX dinâmico:

- cobrança;
- valor exato da venda;
- identificação da venda;
- retorno da confirmação;
- conciliação;
- status do pagamento.

**Não implementar PIX dinâmico sem definir previamente o provedor de pagamento.**

---

# 25. NOTIFICAÇÃO DE VENDA

Depois de uma venda:

- notificar proprietário;
- notificar gerente, se configurado;
- enviar e-mail quando habilitado;
- enviar WhatsApp quando habilitado;
- incluir alerta de estoque se houver estoque baixo.

A notificação deverá ser desacoplada da transação principal sempre que possível.

Recomendação futura:

```text
evento = VENDA_FINALIZADA
```

consumido por serviços de notificação.

---

# 26. ENTREGAS

## Acesso

- dono;
- gerente;
- entregador.

## Kanban

Colunas:

```text
AGENDADAS
EM ROTA
ENTREGUES
NÃO ENTREGUE
```

O arrastar de um card deverá alterar o status no servidor.

## Ao entregar

Preencher:

```text
data_entrega = data atual
status = ENTREGUE
```

e, se configurado, notificar responsável.

## Campos

- venda;
- cliente;
- endereço de entrega;
- telefone;
- data agendada;
- horário;
- entregador;
- status;
- valor do frete;
- observações;
- data da entrega.

## Melhoria importante

Ao selecionar a venda, carregar automaticamente os dados do cliente e seu endereço.

A implementação existente apenas traz cliente_nome; isso é um ponto de melhoria documentado.

---

# 27. COMPRAS

## Objetivo

Registrar gastos e entradas de produtos/matérias-primas.

## Campos

- fornecedor;
- data;
- itens;
- custo;
- observações;
- total.

## Itens

- produto;
- quantidade;
- preço unitário;
- subtotal.

## Regra de estoque

Ao registrar compra:

```text
se produto = matéria-prima
    incrementar estoque de produção
```

Porém, a regra completa precisa evoluir para permitir também produtos comprados para venda.

Não assumir que toda compra sempre entra no mesmo grupo.

---

# 28. SEGREGAÇÃO DE ESTOQUE

Esta é uma regra de negócio crítica.

Uma mesma categoria de material pode assumir papéis distintos:

```text
malha para venda
malha utilizada na produção
```

O sistema deverá tratar o destino de estoque de maneira clara.

Recomendação:

não depender apenas de `tipo = malha`.

Utilizar explicitamente:

```text
grupo_estoque
```

com valores:

```text
MATERIA_PRIMA
PRODUTO_ACABADO
```

E, se necessário no futuro:

```text
MERCADORIA_PARA_REVENDA
```

---

# 29. ORDEM DE PRODUÇÃO

## Objetivo

Controlar transformação de matéria-prima em produto acabado.

## Campos

- número da ordem;
- produto acabado;
- quantidade produzida;
- cliente opcional;
- tipo de ordem;
- fornecedor, quando aplicável;
- matérias-primas;
- data;
- status;
- observações.

## Tipos

```text
ESTOQUE
ENCOMENDA
```

## Status

```text
ABERTA
FINALIZADA
```

---

# 30. PRIORIZAÇÃO DA PRODUÇÃO

A seleção de produto acabado deverá priorizar:

1. esgotado;
2. estoque baixo;
3. OK.

Dentro da mesma prioridade, utilizar critério determinístico, por exemplo:

```text
nome
```

ou:

```text
maior déficit
```

A estratégia final pode ser evoluída.

---

# 31. QUANTIDADE SUGERIDA

Quando o usuário abrir produção a partir de um produto em estoque baixo, o sistema deverá:

- receber produto;
- receber quantidade sugerida;
- receber fornecedor, se houver;
- deixar todos os campos editáveis.

O atalho deverá funcionar por query parameters ou mecanismo equivalente.

Exemplo:

```text
/ordens-producao?produto_id=...&quantidade=...&fornecedor=...
```

O back-end não deve aceitar qualquer valor sem validar novamente.

---

# 32. FINALIZAÇÃO DA ORDEM DE PRODUÇÃO

Ao finalizar:

1. validar ordem aberta;
2. validar produto acabado;
3. validar quantidade;
4. validar matérias-primas;
5. conferir estoque das matérias-primas;
6. descontar matéria-prima;
7. gerar movimentos de consumo;
8. incrementar produto acabado;
9. gerar movimento de produção;
10. marcar ordem como finalizada;
11. registrar data/hora e usuário;
12. impedir finalização duplicada.

A operação deve ser transacional.

---

# 33. FORNECEDORES

## Campos

- empresa/nome;
- CNPJ;
- telefone;
- e-mail;
- produto fornecido;
- observações.

## Tela

A referência demonstra cards com:

- nome;
- produto fornecido;
- CNPJ;
- telefone;
- e-mail;
- quantidade de compras;
- valor total comprado.

Esses indicadores devem ser calculados no back-end quando a base crescer.

Evitar carregar todas as compras para o navegador apenas para somar.

---

# 34. RELATÓRIOS

## Acesso

- dono;
- gerente.

## Períodos

Existentes na referência:

- dia;
- semana;
- quinzena;
- mês atual;
- mês anterior.

## Indicadores

- receita;
- compras;
- salários;
- lucro.

## Fórmula

```text
LUCRO = RECEITA - (COMPRAS + SALÁRIOS + OUTRAS DESPESAS)
```

## Gráficos

### Receita x Despesas por dia

Linha temporal.

### Despesas por categoria

Gráfico por categoria.

## Categorias futuras

- compras;
- salários;
- fretes;
- manutenção;
- outros.

## Regras

Datas deverão ser tratadas em timezone da aplicação/empresa.

Não misturar:

- data local;
- UTC;
- created_at.

O relatório deve utilizar o campo de negócio correto:

```text
data_venda
data_compra
data_despesa
```

---

# 35. DESPESAS

Campos identificados:

- tipo;
- descrição;
- valor;
- data.

Tipos existentes:

```text
SALARIOS
FRETES
MANUTENCAO
OUTROS
```

A tabela de despesas deve fazer parte do cálculo de lucro.

---

# 36. CONFIGURAÇÕES

## Notificações

Configurar:

- e-mail do proprietário;
- WhatsApp do proprietário;
- e-mail do gerente;
- WhatsApp do gerente;
- notificar venda;
- notificar entrega;
- notificar estoque baixo;
- canal e-mail;
- canal WhatsApp.

## Tema

- claro;
- escuro.

## Permissões

### Página

Matriz:

```text
página x cargo
```

### Ações

Matriz:

```text
ação x cargo
```

A configuração deverá ser armazenada no banco.

---

# 37. INTEGRAÇÕES

## Mercado Livre

A tela existente é apenas planejamento.

A integração não está implementada.

Futuro:

- autenticação OAuth;
- produtos;
- anúncios;
- pedidos;
- estoque;
- sincronização;
- frete;
- status;
- conciliação.

Não simular integração funcional sem credenciais e contrato de integração.

---

# 38. WHATSAPP

## Fase 1

Abrir:

```text
https://wa.me/<numero>?text=<mensagem>
```

## Fase 2

Integração real com provedor/API oficial ou serviço escolhido.

O código deve ser desenhado com interface:

```java
public interface WhatsAppNotificationService {
    void enviarMensagem(...);
}
```

Implementação inicial:

```text
WhatsAppLinkService
```

Implementação futura:

```text
WhatsAppApiService
```

---

# 39. E-MAIL

Criar abstração:

```java
public interface EmailNotificationService {
    void enviar(...);
}
```

Configurar SMTP por variáveis de ambiente.

Nunca colocar:

- senha SMTP;
- token;
- API key;

no Git.

---

# 40. NOTIFICAÇÕES INTERNAS

Criar uma camada central:

```text
NotificationService
```

Exemplo conceitual:

```text
Venda finalizada
      |
      +--> e-mail
      |
      +--> WhatsApp
      |
      +--> alerta estoque
```

O módulo não deverá espalhar código de envio de e-mail/WhatsApp dentro dos Controllers.

---

# 41. FRONT-END — ARQUITETURA

Estrutura sugerida:

```text
src/
├── app/
│   ├── App.jsx
│   ├── router.jsx
│   ├── providers.jsx
│   └── routes.js
│
├── assets/
│
├── components/
│   ├── common/
│   ├── layout/
│   ├── auth/
│   ├── dashboard/
│   ├── clientes/
│   ├── funcionarios/
│   ├── estoque/
│   ├── vendas/
│   ├── entregas/
│   ├── compras/
│   ├── fornecedores/
│   ├── producao/
│   ├── relatorios/
│   └── configuracoes/
│
├── pages/
│   ├── Login.jsx
│   ├── Dashboard.jsx
│   ├── Clientes.jsx
│   ├── Funcionarios.jsx
│   ├── Estoque.jsx
│   ├── Vendas.jsx
│   ├── Entregas.jsx
│   ├── Compras.jsx
│   ├── Fornecedores.jsx
│   ├── OrdensProducao.jsx
│   ├── Relatorios.jsx
│   ├── Integracoes.jsx
│   └── Configuracoes.jsx
│
├── services/
│   ├── api.js
│   ├── authService.js
│   ├── clienteService.js
│   ├── funcionarioService.js
│   ├── produtoService.js
│   ├── estoqueService.js
│   ├── vendaService.js
│   ├── entregaService.js
│   ├── compraService.js
│   ├── fornecedorService.js
│   ├── producaoService.js
│   ├── relatorioService.js
│   └── configuracaoService.js
│
├── contexts/
│   ├── AuthContext.jsx
│   ├── ThemeContext.jsx
│   └── PermissionContext.jsx
│
├── hooks/
│   ├── useAuth.js
│   ├── usePermissions.js
│   └── ...
│
├── utils/
│   ├── formatters.js
│   ├── validators.js
│   ├── dates.js
│   └── money.js
│
├── styles/
│   ├── globals.css
│   ├── variables.css
│   └── ...
│
└── main.jsx
```

Embora o projeto utilize JavaScript, arquivos `.jsx` são aceitáveis caso React seja adotado.

Não utilizar TypeScript.

---

# 42. FRONT-END — LAYOUT

O layout deverá preservar a ideia visual observada:

- sidebar;
- cabeçalho;
- conteúdo central;
- navegação por módulos;
- identificação do usuário;
- cargo;
- botão sair;
- responsividade;
- tema claro/escuro.

Na demonstração, o sistema possui:

- marca "Tapetes Kalke";
- subtítulo "Gestão Inteligente";
- menu lateral;
- item ativo destacado;
- badge vermelho no Estoque;
- avatar/inicial do usuário;
- cargo exibido;
- botão "Sair".

---

# 43. NAVEGAÇÃO

Menu:

```text
Dashboard
Clientes
Funcionários
Estoque
Vendas
Entregas
Relatórios
Compras
Fornecedores
Ordem de Produção
Integrações
Configurações
```

A renderização do menu deve depender das permissões recebidas da API.

Não hardcodar somente por cargo.

---

# 44. BADGE DE ESTOQUE

No menu Estoque deverá existir badge quando houver produtos:

```text
estoque_atual <= estoque_minimo
```

O número exibido deverá ser a quantidade de produtos em estado de alerta.

A API pode fornecer diretamente:

```http
GET /api/v1/estoque/resumo
```

Evitar listar todos os produtos no front-end apenas para contar.

---

# 45. PADRÃO VISUAL

A referência demonstra uma interface administrativa limpa, com:

- fundo claro;
- cards;
- bordas suaves;
- sombras discretas;
- botões destacados;
- modais;
- tabelas;
- badges de status;
- ícones;
- espaçamento consistente.

Cores semânticas:

```text
Verde  = sucesso / OK / ativo
Laranja/amarelo = atenção / baixo
Vermelho = erro / exclusão / esgotado
Azul = ação primária / informação
Cinza = neutro
```

A implementação visual final poderá melhorar a estética, mas não deve destruir a linguagem visual já validada.

---

# 46. MODAIS

O sistema precisa de modais reutilizáveis para:

- criar;
- editar;
- visualizar;
- excluir;
- confirmar;
- alertar;
- pagamento PIX;
- detalhes.

Modal de exclusão:

Título:

```text
Confirmar Exclusão
```

Mensagem:

```text
Você tem certeza de que deseja excluir este registro?
Esta ação não pode ser desfeita.
```

O texto e o comportamento deverão ser centralizados em componente reutilizável.

---

# 47. TOASTS

Criar sistema de notificações:

```text
success
warning
error
info
```

Exemplos:

```text
Cliente cadastrado com sucesso.
Produto atualizado com sucesso.
Venda finalizada com sucesso.
Não há estoque suficiente.
Registro excluído com sucesso.
```

As mensagens finais devem ser definidas em uma camada consistente.

---

# 48. FORMULÁRIOS

Todo formulário deverá possuir:

- valor inicial;
- validação;
- mensagens de erro;
- estado de carregamento;
- botão desabilitado durante envio;
- prevenção de duplo clique;
- tratamento de erro da API;
- limpeza após sucesso quando aplicável;
- foco adequado;
- acessibilidade básica.

Campos monetários deverão utilizar máscara/apresentação BRL sem enviar texto formatado inadequadamente ao back-end.

---

# 49. FORMATADORES

Criar:

```text
formatPhone()
formatCNPJ()
formatCEP()
formatCurrency()
formatDate()
formatDateTime()
```

Exemplos:

```text
(27) 99999-9999
00.000.000/0000-00
00000-000
R$ 43,00
28/04/2026
28/04/2026 14:35
```

O back-end deve receber valores normalizados.

---

# 50. CLIENTES — FRONT-END

Tela:

```text
Clientes
Gerencie sua base de clientes
```

Possuir:

- busca;
- filtros;
- Novo Cliente;
- cards;
- ações;
- WhatsApp;
- visualização;
- edição;
- exclusão.

Filtro deverá poder combinar:

- nome;
- telefone;
- cidade;
- status.

---

# 51. ESTOQUE — FRONT-END

Tela:

```text
Estoque
Controle seus produtos
```

Possuir:

- alerta de estoque;
- busca;
- tabela;
- status;
- produto;
- tipo;
- grupo;
- estoque atual;
- estoque mínimo;
- movimentação;
- preço de venda;
- ações.

O formulário de produto deverá conter os campos observados na gravação:

```text
Nome do Produto *
Tipo *
Grupo de Estoque *
Categoria
Unidade Medida *
Estoque Atual *
Estoque Mínimo *
Preço de Custo
Preço de Venda *
Produto Ativo
```

A regra de estoque mínimo deverá possuir informação contextual:

```text
Você será notificado quando o estoque atingir este valor.
```

---

# 52. VENDAS — FRONT-END

Tabela:

```text
Nº Venda
Cliente
Data
Total
Ações
```

No formulário:

```text
Cliente *
Vendedor
Produto
Quantidade
Preço Unitário
Subtotal
Forma de Pagamento
Desconto
Total
```

Ao adicionar produto:

- mostrar estoque atual;
- bloquear produto sem estoque;
- impedir quantidade superior ao disponível;
- atualizar subtotal;
- atualizar total.

---

# 53. COMPRAS — FRONT-END

Tabela:

```text
Data
Fornecedor
Total
Itens
Ações
```

Formulário:

```text
Fornecedor *
Data *
Observações
Produto
Quantidade
Preço Unitário
Subtotal
Total
```

Permitir:

```text
Informar manualmente
```

como fornecedor, conforme referência.

---

# 54. ORDEM DE PRODUÇÃO — FRONT-END

Tela deverá apresentar:

```text
Ordens cadastradas
```

Tabela:

```text
Ordem
Produto
Quantidade
Status
Tipo
Cliente
Ação
```

Modal:

```text
Produto Acabado *
Quantidade Produzida *
Fornecedor
Tipo de Ordem
Vincular Cliente/Encomenda
Matéria-Prima
Quantidade Usada
Adicionar
Observações
```

A lista de produto acabado deverá respeitar prioridade de estoque.

---

# 55. RELATÓRIOS — FRONT-END

Na tela observada:

```text
Relatórios Financeiros
```

Cards:

```text
Receita
Compras (Malhas/Produtos)
Salários
Lucro
```

Filtro:

```text
Dia
Semana
Quinzena
Mês Atual
Mês Anterior
```

Gráficos:

```text
Receita x Despesas por dia
Despesas por Categoria
```

A tela deverá lidar corretamente com zero dados.

Não deixar gráfico visualmente quebrado quando não existirem registros.

---

# 56. CONFIGURAÇÕES — FRONT-END

Deverá possuir:

## Notificações

- e-mail proprietário;
- WhatsApp proprietário;
- e-mail gerente;
- WhatsApp gerente;
- disparar quando venda finalizada;
- entrega concluída;
- estoque baixo;
- canal e-mail;
- canal WhatsApp.

## Tema

- claro;
- escuro.

## Permissões por página

Matriz:

```text
Página x Cargo
```

## Permissões por ação

Matriz:

```text
Ação x Cargo
```

Possuir botão:

```text
Salvar alterações
```

---

# 57. RESPONSIVIDADE

O sistema deve funcionar em:

- desktop;
- notebook;
- tablet;
- celular.

A sidebar deverá:

- permanecer lateral em telas grandes;
- ser recolhível ou drawer em telas pequenas.

Tabelas grandes deverão possuir:

- scroll horizontal;
- colunas prioritárias;
- comportamento mobile adequado.

Não esconder ações importantes apenas porque a tela ficou estreita.

---

# 58. PAGINAÇÃO

Listas com potencial de crescimento deverão ser paginadas no servidor:

- clientes;
- funcionários;
- produtos;
- vendas;
- compras;
- fornecedores;
- entregas;
- ordens de produção.

Filtros e ordenações deverão acompanhar a paginação.

Evitar endpoints que retornem toda a base indefinidamente.

---

# 59. BANCO DE DADOS — MODELO INICIAL

Abaixo está um modelo de domínio recomendado a partir dos materiais fornecidos.

## Tabelas principais

```text
usuario
cargo
cliente
funcionario
produto
fornecedor
venda
venda_item
entrega
compra
compra_item
despesa
ordem_producao
ordem_producao_materia_prima
movimento_estoque
configuracao
permissao_pagina
permissao_acao
```

## Tabelas utilitárias

```text
utilitarios_unidade_medida
utilitarios_tipo_produto
utilitarios_grupo_estoque
utilitarios_forma_pagamento
utilitarios_status_cliente
utilitarios_status_funcionario
utilitarios_status_venda
utilitarios_status_entrega
utilitarios_tipo_ordem_producao
utilitarios_status_ordem_producao
utilitarios_tipo_despesa
utilitarios_tipo_movimento_estoque
```

Os nomes finais deverão ser validados contra o modelo definitivo.

---

# 60. CAMPOS DE AUDITORIA

Todas as principais tabelas de negócio deverão possuir, no mínimo:

```text
created_at
updated_at
created_by
updated_by
```

Quando fizer sentido:

```text
deleted_at
deleted_by
```

Para operações de negócio críticas, registrar:

```text
data_cancelamento
id_usuario_cancelamento
motivo_cancelamento
```

---

# 61. SOFT DELETE

Preferir soft delete para:

- clientes;
- funcionários;
- produtos;
- fornecedores.

Não usar soft delete de forma automática em todos os registros.

Vendas e movimentos financeiros/estoque não devem desaparecer.

---

# 62. RELACIONAMENTOS PRINCIPAIS

```text
Usuario
  |
  +--> Cargo

Funcionario
  |
  +--> Cargo/Perfil

Cliente
  |
  +--> Venda
  |
  +--> Entrega
  |
  +--> OrdemProducao (opcional)

Fornecedor
  |
  +--> Compra

Produto
  |
  +--> VendaItem
  |
  +--> CompraItem
  |
  +--> MovimentoEstoque
  |
  +--> OrdemProducao
  |
  +--> OrdemProducaoMateriaPrima

Venda
  |
  +--> VendaItem
  |
  +--> Cliente
  |
  +--> Funcionario
  |
  +--> Entrega

Compra
  |
  +--> CompraItem

OrdemProducao
  |
  +--> ProdutoAcabado
  |
  +--> Cliente opcional
  |
  +--> MateriasPrimas
```

---

# 63. SPRING DATA JPA

Não expor Entity diretamente pela API.

Usar DTOs.

Evitar:

```java
@Entity
class Cliente { ... }
```

sendo serializado diretamente.

Preferir:

```text
Entity -> Mapper -> Response DTO
```

e:

```text
Request DTO -> Service -> Entity
```

---

# 64. PROBLEMA CRÍTICO: N+1

Ao criar consultas para:

- vendas;
- fornecedores;
- compras;
- itens;
- relatórios;

evitar N+1 queries.

Utilizar:

- `JOIN FETCH`;
- EntityGraph;
- projeções;
- consultas específicas;
- aggregates SQL.

Não fazer dezenas ou centenas de chamadas internas desnecessárias.

---

# 65. CONSULTAS DE DASHBOARD

O dashboard não deverá depender de:

```text
listar todas as vendas
listar todos os clientes
listar todos os produtos
```

e somar no Java.

Preferir agregações SQL.

Exemplo conceitual:

```sql
SELECT COUNT(*)
FROM venda
WHERE status = 'FINALIZADA'
  AND data_venda >= :inicio
  AND data_venda < :fim;
```

---

# 66. VALORES MONETÁRIOS

No Java:

```java
BigDecimal
```

Nunca utilizar:

```java
double
float
```

para valores financeiros.

No PostgreSQL:

```sql
numeric(15,2)
```

ou precisão equivalente definida no projeto.

---

# 67. QUANTIDADES

Para produtos com:

- unidade;
- metro;
- kg;

poderá ser necessário utilizar casas decimais.

Não restringir todos os estoques a inteiro sem validar a necessidade de negócio.

Uma estrutura como:

```text
numeric(15,3)
```

pode ser mais adequada para unidades fracionadas.

A precisão final deverá ser decidida antes da criação definitiva do banco.

---

# 68. DATAS

Usar no Java 21:

```java
LocalDate
LocalDateTime
OffsetDateTime
```

Evitar:

```java
java.util.Date
```

sem necessidade.

Para eventos auditáveis, preferir timestamp com offset/UTC internamente e apresentar em horário local no front-end.

---

# 69. TRANSAÇÕES DE ESTOQUE

Nunca fazer:

```text
produto.setEstoqueAtual(produto.getEstoqueAtual() - quantidade)
produtoRepository.save(produto)
```

sem proteção contra concorrência.

Considerar:

- optimistic locking com `@Version`;
- atualização atômica;
- bloqueio pessimista quando necessário;
- validação transacional.

A estratégia escolhida deve ser documentada.

---

# 70. CONCORRÊNCIA DE ESTOQUE

Cenário:

```text
Dois usuários vendem o mesmo produto simultaneamente.
```

O sistema não pode permitir:

```text
estoque 1
usuário A vende 1
usuário B vende 1
resultado = -1
```

ou venda de item inexistente.

A regra precisa ser garantida no banco/transação.

---

# 71. CÓDIGO DE PRODUTO

A implementação observada possui código de barras como campo.

Prever:

```text
codigo_barras
```

com índice quando for utilizado em busca.

Idealmente:

```text
unique
```

somente se o negócio confirmar que cada código deve ser único.

---

# 72. CNPJ / TELEFONE

Normalizar no armazenamento quando possível.

Exemplo:

```text
telefone = apenas dígitos
cnpj = apenas dígitos
cep = apenas dígitos
```

A máscara existe apenas para apresentação.

Isso evita dificuldades de busca e integração.

---

# 73. SEGURANÇA

Obrigatório:

- CSRF conforme arquitetura escolhida;
- CORS configurado;
- JWT seguro;
- senha com hash;
- validação de payload;
- rate limit no login, se aplicável;
- logs sem senhas/tokens;
- secrets via environment variables;
- tratamento de exceções;
- headers de segurança;
- autorização no servidor.

Nunca:

```text
senha no Git
JWT secreto no Git
senha banco no Git
chave WhatsApp no Git
SMTP_PASSWORD no Git
```

---

# 74. CORS

Permitir apenas origens configuradas.

Exemplo:

```env
FRONTEND_URL=http://localhost:5173
```

Produção:

```env
FRONTEND_URL=https://...
```

Não utilizar:

```text
*
```

em produção sem justificativa.

---

# 75. ARQUIVO `.env`

Nunca versionar `.env`.

Criar:

```text
.env
.env.local
```

e adicionar ao `.gitignore`.

Criar também:

```text
.env.example
```

com somente placeholders.

Exemplo:

```env
DB_URL=jdbc:postgresql://localhost:5432/tapetes_kalke
DB_USERNAME=postgres
DB_PASSWORD=CHANGE_ME

JWT_SECRET=CHANGE_ME
JWT_EXPIRATION=3600

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

FRONTEND_URL=http://localhost:5173
```

---

# 76. `pom.xml` — DEPENDÊNCIAS RECOMENDADAS

A versão exata do Spring Boot deverá ser escolhida de forma estável e compatível com Java 21 no momento da criação do projeto.

Dependências:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Para JWT, selecionar uma biblioteca atual e compatível com Java 21/Spring Security vigente no momento da implementação e fixar a versão explicitamente.

---

# 77. FRONT-END — `package.json`

Estrutura recomendada:

```json
{
  "name": "tapetes-kalke-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "lint": "eslint .",
    "lint:fix": "eslint . --fix",
    "test": "vitest",
    "test:ui": "vitest --ui"
  },
  "dependencies": {
    "axios": "^...",
    "date-fns": "^...",
    "lucide-react": "^...",
    "react": "^...",
    "react-dom": "^...",
    "react-router-dom": "^...",
    "@tanstack/react-query": "^...",
    "recharts": "^..."
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^...",
    "eslint": "^...",
    "eslint-plugin-react-hooks": "^...",
    "eslint-plugin-react-refresh": "^...",
    "vite": "^...",
    "vitest": "^..."
  }
}
```

As versões devem ser fechadas no momento da criação do projeto.

Não utilizar `latest` em produção ou em arquivos de documentação de build.

---

# 78. TAILWIND / SHADCN

O sistema existente utiliza:

- Tailwind CSS;
- shadcn/ui;
- Radix primitives;
- lucide-react.

Esses componentes podem ser adotados na nova implementação para aproximar a aparência da referência, desde que:

- não exista dependência do Base44;
- os componentes estejam sob controle do novo repositório;
- o código possa ser mantido pela equipe;
- não seja introduzido acoplamento desnecessário.

---

# 79. `.gitignore` — BACK-END

Criar `.gitignore` contendo pelo menos:

```gitignore
# =========================
# Java / Maven
# =========================
target/
!.mvn/wrapper/maven-wrapper.jar

# =========================
# IntelliJ
# =========================
.idea/
*.iws
*.iml
*.ipr

# =========================
# Eclipse
# =========================
.classpath
.project
.settings/

# =========================
# VS Code
# =========================
.vscode/

# =========================
# OS
# =========================
.DS_Store
Thumbs.db
Desktop.ini

# =========================
# Logs
# =========================
*.log
logs/

# =========================
# Environment
# =========================
.env
.env.*
!.env.example

# =========================
# Temporary
# =========================
tmp/
temp/

# =========================
# Reports
# =========================
jacoco.exec
```

---

# 80. `.gitignore` — FRONT-END

```gitignore
# dependencies
node_modules/

# build
dist/
build/

# environment
.env
.env.*
!.env.example

# logs
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*

# tooling
.vite/
.cache/
coverage/

# IDE
.idea/
.vscode/

# OS
.DS_Store
Thumbs.db
Desktop.ini
```

---

# 81. `.gitignore` — PROJETO MONOREPO

Caso back-end e front-end estejam no mesmo Git:

```gitignore
# Java / Maven
backend/target/
backend/.mvn/wrapper/*.jar

# Node
frontend/node_modules/
frontend/dist/

# Environments
**/.env
**/.env.*
!**/.env.example

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db
Desktop.ini

# logs
**/*.log

# coverage
**/coverage/
```

---

# 82. ESTRUTURA DO REPOSITÓRIO

Recomendação:

```text
tapetes-kalke/
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── src/
│
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│
├── database/
│   ├── README.md
│   └── seeds/
│
├── docs/
│   ├── arquitetura.md
│   ├── api.md
│   ├── regras-negocio.md
│   └── banco.md
│
├── .gitignore
├── .env.example
├── docker-compose.yml
└── README.md
```

---

# 83. DOCKER

Opcional para desenvolvimento, mas recomendado para padronização.

Serviço inicial:

```text
postgres
```

Opcional:

```text
pgadmin
```

Não é necessário containerizar o frontend/backend em primeira etapa para o desenvolvimento local.

---

# 84. FLYWAY

Migrações:

```text
backend/src/main/resources/db/migration/
```

Exemplo:

```text
V1__create_schema.sql
V2__create_utilitarios.sql
V3__create_usuario.sql
V4__create_cliente.sql
V5__create_funcionario.sql
V6__create_produto.sql
V7__create_fornecedor.sql
V8__create_venda.sql
V9__create_compra.sql
V10__create_entrega.sql
V11__create_ordem_producao.sql
V12__create_movimento_estoque.sql
V13__create_despesa.sql
V14__create_configuracoes.sql
V15__create_permissions.sql
V16__seed_utilitarios.sql
V17__seed_admin.sql
```

As migrações deverão ser pequenas e rastreáveis.

Nunca editar uma migration já aplicada em ambiente compartilhado.

---

# 85. SEEDS

O banco deverá possuir dados iniciais suficientes para desenvolvimento.

Criar seeds de:

- cargos;
- unidades;
- tipos de produto;
- grupos de estoque;
- formas de pagamento;
- status;
- tipos de despesas;
- permissões.

Não colocar senha real de usuário no script.

Criar mecanismo seguro de bootstrap do primeiro administrador.

---

# 86. LOGGING

Utilizar logs estruturados ou padronizados.

Registrar:

- requisição;
- usuário;
- operação;
- erro;
- duração, quando relevante.

Nunca registrar:

- senha;
- token completo;
- segredo de API;
- dados financeiros desnecessários;
- mensagens privadas completas se não forem necessárias.

---

# 87. OBSERVABILIDADE FUTURA

Preparar:

- Actuator;
- métricas;
- health check;
- readiness;
- liveness.

Endpoints como:

```text
/actuator/health
```

devem ser protegidos adequadamente em produção.

---

# 88. TESTES

Criar testes unitários para regras críticas.

Prioridade:

```text
Venda
Estoque
Produção
Permissões
Relatórios
Cliente
```

## Casos mínimos de venda

- venda com estoque suficiente;
- venda sem estoque;
- quantidade maior que estoque;
- desconto válido;
- venda com PIX;
- venda cancelada;
- atualização do cliente;
- criação de movimento de estoque.

## Casos mínimos de produção

- matéria-prima suficiente;
- matéria-prima insuficiente;
- finalização duplicada;
- aumento do produto acabado;
- baixa da matéria-prima.

## Casos mínimos de permissão

- acesso autorizado;
- acesso negado;
- ação autorizada;
- ação negada.

---

# 89. TESTES DE INTEGRAÇÃO

Utilizar PostgreSQL real via Testcontainers para validar:

- FK;
- índices;
- queries;
- transações;
- concorrência;
- relatórios;
- estoque.

Evitar confiar exclusivamente em banco H2 para testar comportamento específico do PostgreSQL.

---

# 90. API — ENDPOINTS SUGERIDOS

## Auth

```text
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
```

## Clientes

```text
GET    /api/v1/clientes
GET    /api/v1/clientes/{id}
POST   /api/v1/clientes
PUT    /api/v1/clientes/{id}
DELETE /api/v1/clientes/{id}
GET    /api/v1/clientes/{id}/historico-compras
POST   /api/v1/clientes/{id}/whatsapp-link
```

## Funcionários

```text
GET    /api/v1/funcionarios
GET    /api/v1/funcionarios/{id}
POST   /api/v1/funcionarios
PUT    /api/v1/funcionarios/{id}
DELETE /api/v1/funcionarios/{id}
```

## Produtos

```text
GET    /api/v1/produtos
GET    /api/v1/produtos/{id}
POST   /api/v1/produtos
PUT    /api/v1/produtos/{id}
DELETE /api/v1/produtos/{id}
```

## Estoque

```text
GET    /api/v1/estoque
GET    /api/v1/estoque/resumo
GET    /api/v1/estoque/baixo
GET    /api/v1/estoque/movimentos
POST   /api/v1/estoque/ajuste
```

## Vendas

```text
GET    /api/v1/vendas
GET    /api/v1/vendas/{id}
POST   /api/v1/vendas
POST   /api/v1/vendas/{id}/cancelar
```

## Entregas

```text
GET    /api/v1/entregas
GET    /api/v1/entregas/{id}
POST   /api/v1/entregas
PUT    /api/v1/entregas/{id}
PATCH  /api/v1/entregas/{id}/status
```

## Compras

```text
GET    /api/v1/compras
GET    /api/v1/compras/{id}
POST   /api/v1/compras
DELETE /api/v1/compras/{id}
```

## Fornecedores

```text
GET    /api/v1/fornecedores
GET    /api/v1/fornecedores/{id}
POST   /api/v1/fornecedores
PUT    /api/v1/fornecedores/{id}
DELETE /api/v1/fornecedores/{id}
```

## Produção

```text
GET    /api/v1/ordens-producao
GET    /api/v1/ordens-producao/{id}
POST   /api/v1/ordens-producao
POST   /api/v1/ordens-producao/{id}/finalizar
```

## Relatórios

```text
GET /api/v1/relatorios/financeiro
GET /api/v1/relatorios/vendas
GET /api/v1/relatorios/estoque
```

## Configurações

```text
GET /api/v1/configuracoes
PUT /api/v1/configuracoes
GET /api/v1/configuracoes/permissoes
PUT /api/v1/configuracoes/permissoes
```

---

# 91. FRONT-END — SERVIÇO HTTP

Criar cliente Axios centralizado.

Responsabilidades:

- baseURL;
- token;
- refresh, se utilizado;
- interceptor de erro;
- logout automático em 401;
- tratamento de erros;
- headers.

Exemplo conceitual:

```text
src/services/api.js
```

Não criar uma instância Axios diferente para cada tela.

---

# 92. REACT QUERY

Utilizar TanStack Query para:

- cache;
- loading;
- refetch;
- mutations;
- invalidação.

Exemplo conceitual:

```text
clientes
estoque
vendas
compras
fornecedores
ordensProducao
```

Ao criar/editar:

```text
invalidateQueries(...)
```

Evitar duplicar manualmente toda a base no estado global.

---

# 93. ESTADO LOCAL

Usar `useState` quando o estado for:

- modal aberto;
- filtro local;
- formulário;
- item selecionado.

Não colocar tudo em Context.

---

# 94. CONTEXTO DE AUTENTICAÇÃO

`AuthContext` deve fornecer:

```text
user
isAuthenticated
isLoading
login()
logout()
```

E informações de autorização:

```text
cargo
permissions
```

---

# 95. ROTAS PROTEGIDAS

Criar:

```text
ProtectedRoute
PermissionRoute
```

Exemplo:

```text
/relatorios
```

somente se o usuário possuir permissão da página.

Mesmo bloqueando no front-end, a API deverá negar acesso não autorizado.

---

# 96. FORMULÁRIOS E DUPLO ENVIO

Todos os submits deverão:

```text
isSubmitting = true
```

e:

- desabilitar botão;
- impedir múltiplas requisições;
- restaurar estado após resposta.

No back-end, operações críticas também deverão ser resistentes a duplicidade quando necessário.

---

# 97. IDempotência

Considerar idempotency key para:

- vendas;
- pagamentos;
- integrações futuras.

Isso é especialmente importante quando houver:

- PIX;
- Mercado Livre;
- webhooks;
- retries automáticos.

---

# 98. RELACIONAMENTOS DE CLIENTE

Quando uma venda for finalizada:

```text
cliente.ultima_compra = data_venda
cliente.total_compras += total_venda
```

Não fazer isso no front-end.

O back-end deve controlar.

Idealmente, `total_compras` poderá ser considerado um campo materializado/derivado conforme a necessidade de performance, mas a consistência deverá ser garantida.

---

# 99. RELATÓRIOS E SALÁRIOS

A regra original cita salários como componente do lucro.

A primeira versão poderá utilizar uma despesa de salário ou cálculo derivado dos funcionários, porém é necessário definir:

- salário mensal;
- proporcionalidade;
- férias;
- encargos;
- funcionário ativo;
- competência.

**Não inventar cálculo trabalhista.**

Para a versão inicial, utilizar apenas a regra financeira explicitamente definida no sistema.

---

# 100. PORTFÓLIO DE PRODUTOS

Melhoria futura prevista:

- página pública;
- catálogo;
- imagens;
- produtos disponíveis;
- kits;
- QR Code;
- compartilhamento;
- link enviado pelo WhatsApp.

Deve existir separação entre:

```text
painel administrativo
```

e:

```text
site público/portfólio
```

O catálogo público não deve expor informações internas como:

- custo;
- salário;
- fornecedores;
- estoque exato;
- dados privados de clientes.

---

# 101. IA — PREPARAÇÃO

A arquitetura deverá permitir futuramente:

- mensagens personalizadas para clientes;
- previsão de estoque;
- recomendação de compras;
- análise de vendas;
- identificação de clientes em risco;
- recomendação de produtos;
- resumo gerencial;
- análise de margem.

Não acoplar IA diretamente aos Controllers.

Criar camada futura:

```text
AiRecommendationService
```

ou equivalente.

---

# 102. AUTOMATIZAÇÕES FUTURAS

Eventos possíveis:

```text
CLIENTE_INATIVO
VENDA_FINALIZADA
ESTOQUE_BAIXO
ESTOQUE_ESGOTADO
ENTREGA_CONCLUIDA
COMPRA_REGISTRADA
ORDEM_PRODUCAO_FINALIZADA
```

Esses eventos poderão disparar:

- notificações;
- relatórios;
- automações;
- IA.

---

# 103. LGPD / DADOS PESSOAIS

O sistema manipula:

- nome;
- telefone;
- e-mail;
- endereço;
- data de nascimento;
- CNPJ;
- salário.

Portanto, deverá tratar dados com segurança.

Recomendações:

- mínimo privilégio;
- logs controlados;
- controle de acesso;
- não expor dados sem necessidade;
- HTTPS em produção;
- secrets seguros;
- backups protegidos;
- política de retenção.

O escopo jurídico da LGPD deverá ser validado separadamente.

---

# 104. BACKUP

O projeto deverá ter estratégia de backup do PostgreSQL.

No mínimo:

```text
backup diário
retenção configurável
teste de restauração
```

Não considerar backup concluído apenas porque existe um arquivo `.sql`.

---

# 105. README

O repositório deverá possuir README explicando:

- objetivo;
- stack;
- requisitos;
- Java;
- Node;
- PostgreSQL;
- configuração;
- variáveis de ambiente;
- execução;
- testes;
- migrations;
- Swagger;
- estrutura;
- deploy.

---

# 106. COMMANDOS DE DESENVOLVIMENTO

## Back-end

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

## Testes

```bash
./mvnw test
```

## Front-end

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
```

---

# 107. SWAGGER

Disponibilizar documentação OpenAPI.

Documentar:

- endpoints;
- parâmetros;
- requests;
- responses;
- erros;
- autenticação;
- permissões.

Não deixar Swagger sem exemplos.

---

# 108. PAGINAÇÃO E FILTRO PADRÃO

Criar padrão:

```text
page
size
sort
direction
search
```

Exemplo:

```text
GET /api/v1/clientes?page=0&size=20&search=Laura&sort=nome&direction=asc
```

---

# 109. ORDENAÇÃO

Cada tabela deverá definir colunas permitidas para ordenação.

Não concatenar diretamente a coluna recebida pelo usuário no SQL.

Criar whitelist.

---

# 110. SQL / JDBC

Quando JPA não for suficiente para relatórios complexos:

- utilizar consulta nativa;
- projection;
- JdbcTemplate;
- QueryDSL ou outra solução, se necessário.

Não forçar JPA para toda agregação financeira.

---

# 111. ERROS DE NEGÓCIO

Criar códigos previsíveis.

Exemplos:

```text
CLIENTE_NAO_ENCONTRADO
PRODUTO_NAO_ENCONTRADO
ESTOQUE_INSUFICIENTE
PRODUTO_INATIVO
VENDA_NAO_ENCONTRADA
VENDA_JA_CANCELADA
ORDEM_PRODUCAO_JA_FINALIZADA
MATERIA_PRIMA_INSUFICIENTE
ACESSO_NEGADO
DADOS_INVALIDOS
```

---

# 112. REGRA DE EXCLUSÃO

Antes de excluir uma entidade, verificar dependências.

Exemplo:

Não permitir excluir produto que possui:

- vendas;
- compras;
- movimentos de estoque;
- produção.

Em vez disso:

```text
ativo = false
```

quando adequado.

---

# 113. HISTÓRICO

Módulos que impactam dinheiro ou estoque devem preservar histórico.

Não apagar:

- venda;
- venda_item;
- compra;
- compra_item;
- movimento_estoque;
- ordem de produção finalizada.

---

# 114. REGRAS DE PRODUTO

Produto inativo:

- não pode aparecer em nova venda;
- não deve aparecer como opção padrão de compra, conforme regra definida;
- pode aparecer em histórico;
- pode continuar disponível para relatórios.

---

# 115. REGRAS DE VENDA

Um produto:

```text
ativo = false
```

não pode ser vendido.

Um produto:

```text
estoque_atual <= 0
```

não pode ser vendido.

A quantidade não pode superar o estoque disponível.

---

# 116. REGRAS DE PRODUÇÃO

Produto acabado deve ser:

```text
grupo_estoque = PRODUTO_ACABADO
```

Matéria-prima deve ser:

```text
grupo_estoque = MATERIA_PRIMA
```

Não aceitar matéria-prima como produto acabado por engano.

---

# 117. COMPRA DE MATÉRIA-PRIMA

Ao comprar matéria-prima:

```text
estoque_materia_prima += quantidade
```

Gerar:

```text
movimento = ENTRADA_COMPRA
```

---

# 118. VENDA DE PRODUTO

Ao finalizar venda:

```text
estoque_produto -= quantidade
```

Gerar:

```text
movimento = SAIDA_VENDA
```

---

# 119. PRODUÇÃO

Ao finalizar produção:

```text
materia_prima -= utilizada
produto_acabado += produzido
```

Gerar movimentos separados.

---

# 120. ALERTA DE ESTOQUE

Considerar alerta quando:

```text
estoque_atual <= estoque_minimo
```

Diferenciar:

```text
0 = ESGOTADO
1..estoque_minimo = BAIXO
> estoque_minimo = OK
```

---

# 121. CLIENTES INATIVOS

A referência identifica:

```text
>= 2 meses
```

como "Atenção (Inativo)".

A aplicação deverá utilizar cálculo por data real.

Evitar:

```text
meses = dias / 30
```

quando for possível comparar datas com API de datas.

Exemplo conceitual:

```text
ultimaCompra < hoje.minusMonths(2)
```

---

# 122. FILTRO DE CLIENTES

Filtros possíveis:

```text
Todos
Ativos
Atenção
Atenção (Inativo)
Inativos
Novos
```

Essa expansão é recomendada para a nova versão, mas deverá ser validada antes de ser considerada requisito obrigatório.

---

# 123. WHATSAPP — NÃO AUTOMATIZAR PELO NAVEGADOR SEM DEFINIÇÃO

Não usar automações frágeis baseadas em:

- abrir WhatsApp Web;
- clicar em elementos;
- simular teclado;
- scraping.

Para envio real, utilizar API/provedor apropriado.

O estágio inicial pode utilizar `wa.me`.

---

# 124. PLAYWRIGHT / COPYRIGHT / REPLICAÇÃO

O projeto deve ser construído com implementação própria.

Não:

- copiar código proprietário do Base44;
- duplicar bundles;
- incorporar arquivos compilados;
- reutilizar dependências privadas;
- reproduzir identificadores internos desnecessários.

A referência deve ser convertida em:

```text
requisito funcional
```

e:

```text
requisito visual
```

---

# 125. COMPATIBILIDADE COM TESTES E2E

Preparar o front-end para testes:

- Playwright;
- Cypress;
- Selenium, se necessário.

Adicionar `data-testid` em elementos críticos quando realmente necessário.

Exemplos:

```text
data-testid="btn-novo-cliente"
data-testid="btn-salvar-venda"
data-testid="estoque-alerta"
```

---

# 126. ACESSIBILIDADE

Formulários deverão possuir:

- label associado;
- foco;
- navegação por teclado;
- `aria-label` quando necessário;
- contraste adequado;
- mensagens de erro acessíveis.

Modais devem:

- prender foco;
- permitir ESC;
- devolver foco ao elemento anterior.

---

# 127. PERFORMANCE FRONT-END

Evitar:

- renderizações desnecessárias;
- consultas repetidas;
- carregar imagens gigantes;
- listas sem paginação;
- cálculos pesados em render.

Usar:

- lazy loading de páginas;
- cache;
- debounce em buscas;
- memoização apenas quando justificável.

---

# 128. PESQUISA / DEBOUNCE

Busca de clientes/produtos deverá utilizar debounce quando a pesquisa for no servidor.

Exemplo:

```text
300–500 ms
```

O valor exato pode ser ajustado.

---

# 129. FORMULÁRIO DE VENDA — UX

Fluxo recomendado:

```text
Selecionar cliente
        ↓
Selecionar vendedor
        ↓
Pesquisar produto
        ↓
Visualizar estoque
        ↓
Definir quantidade
        ↓
Adicionar item
        ↓
Conferir itens
        ↓
Escolher pagamento
        ↓
Aplicar desconto
        ↓
Visualizar total
        ↓
Finalizar
```

Se PIX:

```text
Finalizar
   ↓
Modal PIX
   ↓
QR Code
   ↓
Confirmar
```

---

# 130. ORDEM DE PRODUÇÃO — UX

Fluxo:

```text
Abrir nova ordem
      ↓
selecionar produto acabado
      ↓
sistema sugere quantidade
      ↓
fornecedor sugerido
      ↓
selecionar tipo
      ↓
opcionalmente vincular cliente
      ↓
adicionar matérias-primas
      ↓
informar quantidades
      ↓
salvar
      ↓
finalizar posteriormente
```

---

# 131. ENTREGA — UX

Fluxo:

```text
Venda
  ↓
Criar entrega
  ↓
Selecionar data/horário
  ↓
Selecionar entregador
  ↓
Agendada
  ↓
Em rota
  ↓
Entregue / Não entregue
```

Ao arrastar no Kanban:

```text
frontend
   ↓
API
   ↓
persistência
```

Nunca considerar o drag-and-drop concluído só no front-end.

---

# 132. RELATÓRIO — PERÍODOS

Definir claramente intervalos:

### Dia

```text
00:00:00 <= data < próximo dia 00:00:00
```

### Semana

Definir início da semana como segunda-feira, salvo decisão contrária.

### Quinzena

```text
01–15
16–fim do mês
```

### Mês atual

```text
primeiro dia do mês
até primeiro dia do próximo mês
```

### Mês anterior

```text
primeiro dia mês anterior
até primeiro dia mês atual
```

---

# 133. FUSO HORÁRIO

Definir:

```text
America/Sao_Paulo
```

ou configuração equivalente para o ambiente da empresa.

Evitar divergência entre:

- banco;
- back-end;
- browser.

---

# 134. ARQUITETURA DE EVENTOS — PREPARAÇÃO

Mesmo que inicialmente síncrona, organizar o código de maneira que seja possível evoluir para eventos.

Exemplo:

```text
VendaFinalizadaEvent
EstoqueBaixoEvent
EntregaConcluidaEvent
OrdemProducaoFinalizadaEvent
```

---

# 135. FILA / ASYNC

No início pode ser síncrono.

Futuramente:

```text
RabbitMQ
Kafka
SQS
```

não deve ser introduzido prematuramente.

Para notificações simples, uma fila pode ser adicionada quando houver necessidade real.

---

# 136. WEBHOOKS FUTUROS

Preparar endpoints para:

```text
POST /api/v1/webhooks/pix
POST /api/v1/webhooks/mercadolivre
POST /api/v1/webhooks/whatsapp
```

Somente quando as integrações forem realmente desenvolvidas.

Validar assinatura/autenticidade.

---

# 137. CONFIGURAÇÃO POR AMBIENTE

Ambientes:

```text
dev
test
prod
```

ou:

```text
local
staging
production
```

Cada ambiente deve possuir configuração própria.

---

# 138. PROFILE SPRING

Sugestão:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Não colocar secrets reais.

---

# 139. DEPLOY

A arquitetura deverá permitir:

```text
Frontend -> servidor/CDN
Backend -> servidor/container
PostgreSQL -> banco gerenciado ou servidor
```

HTTPS obrigatório em produção.

---

# 140. CI/CD

Preparar pipeline:

```text
checkout
↓
build
↓
test
↓
lint
↓
package
↓
deploy
```

No GitHub Actions ou outra ferramenta.

---

# 141. CHECKLIST DE ENTREGA DO BACK-END

Antes de considerar o back-end concluído:

```text
[ ] Java 21
[ ] Spring Boot
[ ] Maven
[ ] PostgreSQL
[ ] Flyway
[ ] Security
[ ] JWT
[ ] Validation
[ ] OpenAPI
[ ] DTOs
[ ] Controllers
[ ] Services
[ ] Repositories
[ ] Exceptions
[ ] Logs
[ ] Transactions
[ ] Pagination
[ ] Tests
[ ] Testcontainers
[ ] CORS
[ ] Environment variables
[ ] README
```

---

# 142. CHECKLIST DE ENTREGA DO FRONT-END

```text
[ ] JavaScript
[ ] React
[ ] Vite
[ ] Router
[ ] API client
[ ] Auth context
[ ] Permissions
[ ] Protected routes
[ ] Layout
[ ] Sidebar
[ ] Dashboard
[ ] Clientes
[ ] Funcionários
[ ] Estoque
[ ] Vendas
[ ] Entregas
[ ] Compras
[ ] Fornecedores
[ ] Ordem de Produção
[ ] Relatórios
[ ] Integrações
[ ] Configurações
[ ] Tema claro/escuro
[ ] Toasts
[ ] Modais
[ ] Máscaras
[ ] Responsividade
[ ] Loading states
[ ] Empty states
[ ] Error states
[ ] Testes
```

---

# 143. CHECKLIST DE BANCO

```text
[ ] IDs varchar(38)
[ ] FKs id_nome_tabela
[ ] Índices idx_nome
[ ] FKs fk_origem_X_destino
[ ] Utilitários em schema próprio
[ ] IDs utilitários serial
[ ] Auditoria
[ ] Constraints
[ ] Unique quando necessário
[ ] Índices de busca
[ ] Índices de FK
[ ] Flyway
[ ] Seeds
[ ] Backup
```

---

# 144. PONTOS DE ATENÇÃO IDENTIFICADOS NO SISTEMA ORIGINAL

Foram observados explicitamente os seguintes pontos:

1. `tipo_estoque` é utilizado em diversas telas, mas não estava declarado no schema original do Produto.
2. `OrdemProducao` e `Fornecedor` não tinham RLS declarado na referência.
3. O endereço da entrega não era automaticamente carregado a partir do cliente.
4. Integrações eram apenas planejamento.
5. A tela de produção utilizava acesso direto ao objeto Base44 devido a workaround.
6. Permissões de criar/editar/excluir ainda precisavam ser completadas.
7. WhatsApp atualmente era apenas abertura de link/mensagem.
8. PIX dinâmico ainda não estava implementado.
9. Separação de matéria-prima e produto acabado é uma necessidade crítica.
10. O dashboard e relatórios possuem forte dependência de regras corretas de datas/status.

Na nova arquitetura, esses pontos devem ser tratados como oportunidades para eliminar dívidas técnicas, não como comportamentos a reproduzir literalmente.

---

# 145. OBSERVAÇÕES DA DEMONSTRAÇÃO EM VÍDEO

A demonstração reforça visualmente:

- Dashboard com cards de indicadores;
- alertas de estoque e clientes;
- tela Clientes em cards;
- filtros de clientes;
- modal de criação de cliente;
- confirmação de exclusão;
- tela Funcionários em cards;
- tela Estoque em tabela;
- alerta de estoque baixo;
- modal de produto;
- tela Vendas em tabela;
- tela Relatórios com filtros de período;
- tela Compras com modal;
- tela Fornecedores;
- tela Ordem de Produção;
- tela Configurações com matriz de permissões.

O vídeo também confirma que a interface existente é predominantemente administrativa, baseada em sidebar lateral, cards, tabelas, modais e badges.

A gravação é uma referência visual/funcional complementar e não substitui o levantamento do código-fonte.

---

# 146. O QUE PRECISA SER DECIDIDO ANTES DA IMPLEMENTAÇÃO DEFINITIVA

As informações fornecidas são suficientes para iniciar a arquitetura, mas ainda existem decisões que não devem ser inventadas.

## Autenticação

- login por e-mail?
- login por telefone?
- recuperação de senha?
- convite de usuário?
- refresh token?
- validade dos tokens?

## Funcionários

- cargo é separado de usuário?
- todo funcionário possui usuário?
- salário mensal?
- histórico de reajustes?

## Estoque

- mesma malha pode existir como venda e matéria-prima?
- precisa controlar lotes?
- precisa controlar cor/lote?
- unidades fracionadas?
- ajustes manuais exigem motivo?

## Venda

- cheque continuará disponível?
- venda pode ser parcelada?
- existe fiado/crediário?
- desconto por item ou total?
- frete entra no total?

## Compra

- toda compra entra no estoque?
- compra de material indireto?
- compra para revenda?
- custo médio?
- custo por lote?

## Produção

- uma receita/BOM ficará cadastrada?
- ou as matérias-primas serão informadas manualmente em cada ordem?
- perda de matéria-prima?
- produção parcial?
- cancelamento de ordem?

## Entregas

- uma venda pode possuir várias entregas?
- pagamento de frete?
- comprovante de entrega?
- assinatura/foto?

## Relatórios

- salário entra por competência?
- compras entram pelo valor pago ou pelo valor lançado?
- lucro bruto ou líquido?

## Integrações

- provedor de PIX;
- SMTP;
- WhatsApp;
- Mercado Livre.

---

# 147. CRITÉRIO DE IMPLEMENTAÇÃO PARA A IA

A IA responsável pelo desenvolvimento deverá seguir esta ordem:

## Fase 1 — Fundação

1. criar repositório;
2. criar backend;
3. criar frontend;
4. criar PostgreSQL;
5. criar Flyway;
6. criar autenticação;
7. criar cargos/permissões;
8. criar layout base.

## Fase 2 — Cadastros

1. Clientes;
2. Funcionários;
3. Fornecedores;
4. Produtos.

## Fase 3 — Estoque

1. estoque;
2. movimentos;
3. alertas;
4. ajustes.

## Fase 4 — Operações

1. Compras;
2. Vendas;
3. Entregas;
4. Produção.

## Fase 5 — Gestão

1. Dashboard;
2. Relatórios;
3. Configurações.

## Fase 6 — Integrações

1. e-mail;
2. WhatsApp;
3. PIX;
4. Mercado Livre.

## Fase 7 — Inteligência

1. automações;
2. IA;
3. recomendações;
4. previsão.

---

# 148. REGRA PARA NÃO GERAR CÓDIGO INCOMPLETO

Ao implementar cada módulo, entregar:

```text
Entity
Repository
Request DTO
Response DTO
Service
Controller
Validation
Migration
Test
Frontend page
Frontend service
Frontend components
Loading state
Empty state
Error state
Permission check
Documentation
```

Não criar uma tela "bonita" sem API.

Não criar API sem validações.

Não criar tabela sem migration.

Não criar regra somente no front-end.

---

# 149. REGRA PARA ALTERAÇÕES FUTURAS

Sempre que uma regra de negócio for alterada:

1. atualizar documentação;
2. atualizar migration se houver mudança no banco;
3. atualizar back-end;
4. atualizar testes;
5. atualizar front-end;
6. atualizar README/documentação da API.

---

# 150. REGRA SOBRE CÓDIGO LEGADO DO BASE44

Se código do Base44 for fornecido:

- não copiar cegamente;
- analisar cada componente;
- extrair regras;
- identificar validações;
- identificar filtros;
- identificar permissões;
- identificar endpoints implícitos;
- identificar cálculos;
- identificar comportamento visual;
- identificar campos ocultos;
- identificar defaults;
- identificar mensagens;
- identificar edge cases.

O objetivo é reconstruir o sistema com arquitetura própria.

---

# 151. ARQUIVOS DO BASE44 QUE DEVEM SER SOLICITADOS

Para uma reconstrução realmente fiel, solicitar preferencialmente:

```text
src/pages/*
src/components/*
src/components/auth/*
src/components/common/*
src/components/dashboard/*
src/components/clientes/*
src/components/estoque/*
src/components/vendas/*
src/components/entregas/*
src/components/compras/*
src/components/fornecedores/*
src/components/producao/*
src/components/configuracoes/*
src/lib/AuthContext.*
src/lib/query-client.*
src/entities/*
src/api/*
src/utils/*
```

Também solicitar:

```text
base44/entities/*.jsonc
```

ou equivalente.

---

# 152. POR QUE AS CLASSES DO BASE44 SÃO IMPORTANTES

A documentação mostra **o que existe**, mas o código poderá revelar **como funciona**.

Exemplos de informações que podem estar escondidas:

- validações não documentadas;
- tratamento de valores nulos;
- campos calculados;
- regras condicionais;
- ordenações específicas;
- mensagens;
- query params;
- filtros;
- defaults;
- tratamento de erro;
- relações;
- comportamento do formulário;
- comportamento de exclusão;
- permissões granulares;
- regras especiais de estoque;
- detalhes do cálculo de status do cliente;
- comportamento do Kanban;
- geração do número da venda;
- atualização automática de dados;
- nomes exatos de campos;
- valores reais dos enums;
- regras de notificações.

Portanto, para **replicar fielmente**, é recomendável fornecer as classes.

---

# 153. O QUE É SUFICIENTE PARA COMEÇAR

Com os materiais atuais já é possível:

- definir arquitetura;
- criar banco inicial;
- criar backend;
- criar frontend base;
- criar layout;
- criar contratos da API;
- implementar a maior parte das telas;
- estruturar permissões;
- implementar estoque;
- implementar vendas;
- implementar compras;
- implementar produção;
- implementar relatórios.

Porém, antes de considerar a reconstrução **100% fiel**, analisar os fontes do Base44.

---

# 154. ORDEM RECOMENDADA DOS FONTES A SEREM ENVIADOS

Prioridade alta:

```text
1. entidades/schemas
2. Vendas.jsx + VendaForm
3. Estoque.jsx + ProdutoForm
4. OrdensProducao.jsx + OrdemProducaoForm
5. Clientes.jsx + ClienteForm
6. Configuracoes.jsx
7. AuthContext
8. Entregas.jsx + EntregaForm
9. Compras.jsx + CompraForm
10. Relatorios.jsx
11. Dashboard.jsx
12. Funcionarios.jsx
13. Fornecedores.jsx
```

Depois:

```text
utils
helpers
componentes comuns
serviços
configurações
```

---

# 155. PROMPT FINAL PARA EXECUÇÃO POR OUTRA IA

Use o conteúdo deste documento como especificação principal.

Você deverá agir como arquiteto de software e desenvolvedor full-stack sênior.

## Regras obrigatórias

- Java 21;
- Spring Boot;
- Maven;
- PostgreSQL;
- Flyway;
- Spring Security;
- API REST;
- JavaScript no front-end;
- React + Vite, salvo decisão posterior;
- sem TypeScript;
- sem dependência do Base44;
- arquitetura em camadas;
- DTOs;
- validação;
- tratamento de exceções;
- autorização por página;
- autorização por ação;
- transações;
- testes;
- documentação;
- `.gitignore`;
- `.env.example`;
- README;
- OpenAPI;
- migrações;
- seeds.

## Não faça

- não criar regra de negócio somente no frontend;
- não expor Entity diretamente;
- não colocar secrets no código;
- não usar `double` para dinheiro;
- não permitir estoque negativo por corrida de concorrência;
- não excluir histórico financeiro/estoque sem regra;
- não confiar em permissões vindas apenas do navegador;
- não usar Base44 como backend;
- não copiar código proprietário desnecessariamente.

## Sempre entregar

Ao criar um módulo:

1. migration;
2. entity;
3. repository;
4. DTOs;
5. service;
6. controller;
7. validações;
8. exceptions;
9. testes;
10. frontend service;
11. página;
12. componentes;
13. estados de loading/erro/vazio;
14. permissões;
15. documentação.

---

# 156. CRITÉRIO FINAL DE QUALIDADE

Antes de afirmar que o sistema está pronto, validar:

### Segurança

```text
[ ] usuário não autenticado não acessa páginas protegidas
[ ] usuário não autorizado recebe 403
[ ] API valida permissões
[ ] secrets não estão no Git
[ ] senhas são hash
[ ] tokens não são logados
```

### Dados

```text
[ ] FKs funcionando
[ ] índices funcionando
[ ] migrations reproduzíveis
[ ] auditoria
[ ] histórico
```

### Estoque

```text
[ ] venda baixa estoque
[ ] compra entrada estoque
[ ] produção consome matéria-prima
[ ] produção adiciona produto acabado
[ ] cancelamento estorna
[ ] não existe estoque negativo
```

### Vendas

```text
[ ] total correto
[ ] desconto correto
[ ] estoque validado
[ ] cliente atualizado
[ ] número único
[ ] notificações respeitam configuração
```

### Produção

```text
[ ] ordem criada
[ ] matérias-primas validadas
[ ] finalização transacional
[ ] estoque atualizado
[ ] não finaliza duas vezes
```

### Relatórios

```text
[ ] períodos corretos
[ ] timezone correto
[ ] receitas corretas
[ ] compras corretas
[ ] despesas corretas
[ ] lucro correto
```

### Front-end

```text
[ ] responsivo
[ ] acessível
[ ] loading
[ ] empty state
[ ] error state
[ ] confirmação de exclusão
[ ] toast
[ ] modais
[ ] permissões
[ ] tema
```

---

# 157. RESULTADO ESPERADO

O resultado final deverá ser uma aplicação empresarial completa:

```text
USUÁRIO
   ↓
LOGIN
   ↓
PERMISSÕES
   ↓
LAYOUT
   ↓
MÓDULOS
   ↓
API REST
   ↓
SERVICES
   ↓
REPOSITORIES
   ↓
POSTGRESQL
```

Com os seguintes módulos:

```text
Dashboard
Clientes
Funcionários
Estoque
Vendas
Entregas
Compras
Fornecedores
Ordem de Produção
Relatórios
Integrações
Configurações
```

E os seguintes fluxos:

```text
Cliente
   ↓
Venda
   ↓
Estoque
   ↓
Notificação

Compra
   ↓
Estoque de Matéria-Prima

Estoque Baixo
   ↓
Ordem de Produção
   ↓
Consumo de Matéria-Prima
   ↓
Produto Acabado

Venda
   ↓
Entrega
   ↓
Conclusão
   ↓
Notificação

Todas as operações
   ↓
Auditoria
```

---

# 158. DECISÃO TÉCNICA FINAL RECOMENDADA

Não tentar "converter o Base44 para Java" literalmente.

Fazer:

```text
Base44
   ↓
Levantamento funcional
   ↓
Modelo de domínio
   ↓
Modelo relacional
   ↓
API
   ↓
Back-end próprio
   ↓
Front-end próprio
```

Isso reduz acoplamento, elimina as limitações do BaaS e deixa o projeto pronto para:

- Mercado Livre;
- PIX;
- WhatsApp;
- e-commerce;
- automações;
- IA;
- relatórios mais avançados;
- escalabilidade.

---

# 159. NOTA SOBRE O ESCOPO DESTE DOCUMENTO

Este documento foi **revisado após a análise dos códigos-fontes do Base44**. Ele deve ser tratado como a especificação consolidada para a implementação independente. Os fontes analisados permanecem como material de referência e auditoria.

- código das páginas;
- componentes;
- schemas;
- helpers;
- regras ocultas;
- valores exatos;
- comportamentos dos formulários;
- validações;
- decisões ainda pendentes.

---

# 160. STATUS DA ANÁLISE DOS FONTES DO BASE44

## Os fontes já foram recebidos e analisados.

O ZIP foi inspecionado módulo por módulo e os achados foram incorporados nas seções de auditoria e correções obrigatórias adicionadas ao final deste documento.

---

# FIM DA ESPECIFICAÇÃO


---

# 161. EMENDA FINAL — AUDITORIA DOS FONTES DO BASE44

**ATENÇÃO:** esta seção e as seguintes são parte da versão final e, em caso de conflito com qualquer seção anterior deste arquivo, **esta emenda prevalece**.

A especificação anterior foi produzida antes da análise do ZIP. Depois disso, os fontes foram confrontados com os dois Markdown e com os quadros da demonstração.

Esta revisão transforma os achados em requisitos obrigatórios para o novo projeto.

---

# 162. INVENTÁRIO DOS FONTES ANALISADOS

O ZIP contém aproximadamente:

```text
124 arquivos
```

Incluindo:

```text
React/JSX
JavaScript
um arquivo TypeScript
schemas JSON
componentes UI
configuração Vite
cliente Base44
integrações Base44
autenticação Base44
```

Não existe backend Java no ZIP.

Logo, as regras de backend abaixo foram reconstruídas a partir de:

```text
Markdown de orientações
Markdown de arquitetura
código React/JSX
schemas JSON disponíveis
fluxos observados
```

---

# 163. COBERTURA DOS SCHEMAS BASE44

Schemas localizados:

```text
Cliente.json
Compra.json
Configuracao.json
Fornecedor.json
OrdemProducao.json
Produto.json
```

Ausentes no ZIP:

```text
Venda.json
Funcionario.json
Entrega.json
Despesa.json
```

Portanto, a estrutura final dessas quatro entidades deve ser baseada na documentação e no uso real encontrado nos componentes.

A IA não deve afirmar que os schemas ausentes possuem campos que não foram efetivamente sustentados por código/documentação.

---

# 164. ARQUIVOS QUE NÃO PERTENCEM AO NOVO PROJETO

O ZIP contém arquivos externos ao sistema:

```text
App Controle Financeiro - 2.0.html
App Controle Financeiro.html
leitor_markdown_v2.html
```

Eles devem ser ignorados.

Não copiar para o repositório do Tapetes Kalke.

---

# 165. BASE44 — DEPENDÊNCIAS A REMOVER

Remover completamente:

```text
@base44/sdk
@base44/vite-plugin
base44Client
Base44 entity wrappers
Base44 auth
Base44 app logs
Base44 VisualEditAgent
Base44 Core integrations
```

Buscar ao final do projeto por:

```text
@base44
base44
Base44
legacySDKImports
base44Client
```

Resultado esperado:

```text
zero ocorrências em código executável
```

---

# 166. AUDITORIA — PATHS E IMPORTS

O código atual mistura:

```text
src/Componentes
src/Componentes/Paginas
@/components
@/pages
@/lib
@/entities
```

e utiliza aliases herdados do Base44.

Existe ainda inconsistência de maiúsculas/minúsculas.

O novo projeto deverá usar:

```text
src/components
src/pages
src/services
src/contexts
src/hooks
src/utils
src/styles
src/api
```

Tudo em lowercase nos diretórios.

---

# 167. AUDITORIA — `pages.config.js`

O arquivo atual referencia:

```text
./pages/Dashboard
./pages/Clientes
...
```

mas a árvore fornecida possui páginas em:

```text
src/Componentes/Paginas
```

Isso é um problema de portabilidade/compilação quando os aliases mágicos do ambiente Base44 deixam de existir.

No novo sistema, as rotas deverão ser imports reais e explícitos.

---

# 168. AUDITORIA — `App.css`

O `App.jsx` atual importa:

```text
./App.css
```

mas o arquivo não foi localizado no ZIP.

A nova aplicação não deve possuir imports de arquivos inexistentes.

---

# 169. AUDITORIA — `index.ts`

Existe:

```text
src/Componentes/Utilitarios/index.ts
```

com sintaxe TypeScript:

```text
(pageName: string)
```

Isto é incompatível com a exigência de frontend em JavaScript.

Substituir por:

```text
src/utils/routes.js
```

---

# 170. AUDITORIA — ROTAS E `createPageUrl`

O utilitário atual transforma:

```text
Dashboard -> /dashboard
Clientes -> /clientes
OrdensProducao -> /ordensproducao
```

Enquanto `App.jsx` usa:

```text
/Dashboard
/Clientes
/OrdensProducao
```

Isso cria uma inconsistência real de rota.

Rotas definitivas:

```text
/dashboard
/clientes
/funcionarios
/estoque
/vendas
/entregas
/relatorios
/compras
/fornecedores
/ordens-producao
/integracoes
/configuracoes
```

---

# 171. AUDITORIA — CLIENTE: ERRO DE `ultima_compra`

Ao criar cliente, o código atual define a data do dia como:

```text
ultima_compra
```

Isso é incorreto.

Regra final:

```text
Novo cliente
ultima_compra = NULL
total_compras = 0
```

---

# 172. AUDITORIA — CLIENTE: STATUS INCONSISTENTE

Lista e detalhes calculam status de maneiras diferentes.

A lista respeita o status cadastral `inativo`.

O detalhe pode recalcular somente pela data de compra.

Correção definitiva:

```text
status_cadastral
```

separado de:

```text
classificacao_relacionamento
```

A regra deve existir no backend e ser reutilizada por toda a interface.

---

# 173. AUDITORIA — CLIENTE: WHATSAPP

O código atual concatena `55` diretamente.

Isso pode duplicar código de país.

Nova regra:

```text
normalizar número
identificar DDI existente
gerar URL canônica
```

Preferência: backend retorna `whatsappUrl`.

---

# 174. AUDITORIA — CLIENTE: EXCLUSÃO

Cliente pode possuir vendas históricas.

Não excluir fisicamente cliente com histórico.

Usar:

```text
ativo = false
```

ou mecanismo de soft delete.

---

# 175. AUDITORIA — DASHBOARD: DATA ERRADA

O código filtra vendas do dia/mês pela:

```text
created_date
```

O campo de negócio correto é:

```text
data_venda
```

sempre considerando:

```text
status = FINALIZADA
```

---

# 176. AUDITORIA — DASHBOARD: QUARTO CARD

O código chama o indicador de:

```text
Itens em Estoque
```

mas calcula:

```text
quantidade de produtos ativos com estoque > 0
```

Portanto o novo label deve ser:

```text
Produtos com Estoque
```

Se desejado, criar outro card:

```text
Quantidade em Estoque
```

---

# 177. AUDITORIA — DASHBOARD: CLIENTES

O dashboard atual conta todos os clientes retornados.

A versão nova deve definir a métrica:

```text
clientes ativos
```

ou manter explicitamente:

```text
total de clientes cadastrados
```

A opção escolhida deve ser refletida no nome do card.

---

# 178. AUDITORIA — VENDAS: PERMISSÃO DE EXCLUSÃO

O código usa uma variável equivalente a:

```text
canCreateEdit
```

que inclui:

```text
dono
gerente
vendedor
```

para mostrar o botão de excluir.

Porém a configuração define cancelamento somente para:

```text
dono
gerente
```

Correção:

```text
VENDEDOR -> criar venda
VENDEDOR -> visualizar venda
VENDEDOR -> não cancelar venda
```

---

# 179. AUDITORIA — VENDAS: EXCLUSÃO SEM ESTORNO

O código faz:

```text
Venda.delete(id)
```

Sem devolver estoque.

Sem corrigir cliente.

Isso é uma falha grave de integridade.

Nova regra:

```text
CANCELAR
```

não:

```text
DELETE
```

---

# 180. AUDITORIA — VENDAS: CLIENTE

Ao cancelar uma venda que foi a última compra do cliente, o sistema deve recalcular a última compra válida.

Não simplesmente subtrair uma data sem consultar histórico.

Recomendação:

```text
SELECT MAX(data_venda)
FROM venda
WHERE cliente = ?
  AND status = FINALIZADA
```

E recalcular o total quando necessário.

---

# 181. AUDITORIA — VENDAS: TRANSAÇÃO

Hoje existe sequência:

```text
cria venda
atualiza estoque
atualiza cliente
manda notificação
```

Isso não é seguro.

Nova regra:

```text
venda + itens + estoque + cliente + movimentos
```

na mesma transação de negócio.

Notificação externa pode ocorrer após commit.

---

# 182. AUDITORIA — VENDAS: DADOS DO FRONTEND

O código envia:

```text
estoque_anterior
```

e usa esse número para atualizar o banco.

Isso é proibido na nova aplicação.

O servidor deve consultar o saldo real.

---

# 183. AUDITORIA — VENDAS: CONCORRÊNCIA

Dois usuários podem trabalhar sobre o mesmo estoque.

Implementar:

```text
SELECT FOR UPDATE
```

ou operação SQL atômica equivalente.

---

# 184. AUDITORIA — VENDAS: NÚMERO

`Date.now().slice(...)` não garante unicidade.

Usar:

```text
sequence PostgreSQL
```

---

# 185. AUDITORIA — VENDAS: DESCONTO

A entidade possui desconto, mas o formulário não o trata de forma completa.

A nova venda deve possuir:

```text
subtotal
- desconto
= total
```

Servidor recalcula tudo.

---

# 186. AUDITORIA — VENDAS: PRODUTOS

Produto vendido deve:

```text
ativo = true
pode_ser_vendido = true
estoque_comercial suficiente
```

Não utilizar estoque de produção para venda.

---

# 187. AUDITORIA — COMPRAS: PRODUTO LIVRE

O formulário atual permite item apenas com nome.

Isso cria compra sem vínculo com estoque.

Nova regra:

```text
compra que movimenta estoque exige produto cadastrado
```

Compra sem produto deve ser tratada como despesa, se esse fluxo for realmente necessário.

---

# 188. AUDITORIA — COMPRAS: ESTOQUE

O código atual somente aumenta estoque quando:

```text
tipo_estoque = materia_prima
```

Portanto não atende corretamente compras destinadas à venda.

Nova regra por item:

```text
destino = COMERCIAL
ou
DESTINO = PRODUCAO
```

---

# 189. AUDITORIA — COMPRAS: CANCELAMENTO

Excluir compra não estorna estoque.

Correção:

```text
cancelar compra
estornar movimento
```

Somente se a integridade do saldo permitir.

---

# 190. AUDITORIA — ESTOQUE: DOIS SALDOS

A necessidade de negócio exige distinguir:

```text
estoque comercial
estoque de produção
```

A mesma malha pode ter ambos.

Este é um requisito obrigatório da nova modelagem.

---

# 191. AUDITORIA — ESTOQUE: EDIÇÃO DIRETA

O ProdutoForm atual permite alterar:

```text
estoque_atual
```

diretamente.

Isso deve ser removido.

Toda alteração após criação deve ocorrer através de:

```text
ajuste de estoque
```

com movimento e auditoria.

---

# 192. AUDITORIA — ESTOQUE: EXCLUSÃO

O código usa permissão ampla para ações da tabela.

Exclusão deve ser separada.

Default:

```text
DONO
GERENTE
```

---

# 193. AUDITORIA — ESTOQUE: MOVIMENTAÇÃO

A coluna `Movimentação` atual não representa histórico.

Implementar:

```text
Histórico de Movimentações
```

com:

```text
data
produto
tipo
origem
destino
quantidade
antes
depois
usuário
observação
```

---

# 194. AUDITORIA — PRODUÇÃO: ESTOQUE NEGATIVO

O código atual pode subtrair matéria-prima abaixo de zero.

Nova regra:

```text
saldo produção >= quantidade usada
```

Caso contrário:

```text
409 MATERIA_PRIMA_INSUFICIENTE
```

---

# 195. AUDITORIA — PRODUÇÃO: DUPLA FINALIZAÇÃO

O código não garante transição única.

Nova regra:

```text
ABERTA -> FINALIZADA
```

uma única vez.

---

# 196. AUDITORIA — PRODUÇÃO: TRANSAÇÃO

Consumir todas as matérias-primas e adicionar produto acabado deve ser uma única transação.

---

# 197. AUDITORIA — PRODUÇÃO: MOVIMENTOS

Toda finalização deve gerar:

```text
CONSUMO_PRODUCAO
```

para cada matéria-prima e:

```text
ENTRADA_PRODUCAO
```

para o produto acabado.

---

# 198. AUDITORIA — PRODUÇÃO: ENCOMENDA

Se:

```text
tipo = ENCOMENDA
```

então:

```text
cliente obrigatório
```

---

# 199. AUDITORIA — PRODUÇÃO: FORNECEDOR

Trocar:

```text
fornecedor texto livre
```

por:

```text
id_fornecedor
```

quando houver fornecedor cadastrado.

---

# 200. AUDITORIA — PRODUÇÃO: MATÉRIA-PRIMA

Somente produtos com:

```text
pode_ser_materia_prima = true
```

podem ser selecionados.

---

# 201. AUDITORIA — PRODUÇÃO: QUANTIDADE

Quantidade produzida deve ser:

```text
> 0
```

Quantidades de matéria-prima também:

```text
> 0
```

---

# 202. AUDITORIA — ENTREGA: PERMISSÕES

A configuração original define:

```text
criar = dono, gerente, vendedor
editar = dono, gerente, entregador
```

O código atual usa lógica simplificada de `canManage` e permite divergências.

Nova autorização deve usar ação por ação.

---

# 203. AUDITORIA — ENTREGA: DRAG

Não efetivar mudança de coluna somente no estado local.

API confirma.

Se falhar:

```text
rollback / refetch
```

---

# 204. AUDITORIA — ENTREGA: ENDEREÇO

Formulário atual carrega nome do cliente, mas pede endereço manual.

Nova regra:

```text
selecionar venda
-> carregar cliente
-> carregar endereço
-> permitir ajuste específico da entrega
```

---

# 205. AUDITORIA — ENTREGA: DUPLICIDADE

Primeira versão:

```text
uma entrega por venda
```

Não permitir duplicação acidental.

Arquitetura poderá evoluir para múltiplas entregas.

---

# 206. AUDITORIA — ENTREGA: VENDA CANCELADA

Não criar entrega para:

```text
venda cancelada
```

ou:

```text
venda pendente
```

---

# 207. AUDITORIA — ENTREGA: ATRASO

Não comparar somente data de agendamento com `now`.

Usar:

```text
data + horário
```

quando o horário existir.

---

# 208. AUDITORIA — ENTREGA: WHATSAPP AUTOMÁTICO

Abrir nova janela após evento assíncrono pode ser bloqueado pelo browser.

Na primeira versão:

```text
gerar link
```

ou permitir ação explícita.

Automação real ficará para integração oficial.

---

# 209. AUDITORIA — RELATÓRIO DIA

Código atual usa últimas 24 horas.

Nova regra:

```text
dia do calendário
```

---

# 210. AUDITORIA — RELATÓRIO QUINZENA

Código atual usa últimos 15 dias.

Nova regra:

```text
1–15
16–fim do mês
```

---

# 211. AUDITORIA — RELATÓRIO STATUS

Código atual não filtra vendas por status.

Nova regra:

```text
somente FINALIZADA
```

---

# 212. AUDITORIA — RELATÓRIO PERFORMANCE

Não carregar todas as entidades no browser.

Criar agregações SQL.

---

# 213. AUDITORIA — RELATÓRIO SEMANA

Não transformar a semana inteira em um único ponto se o título do gráfico disser "por dia".

Manter série diária.

---

# 214. AUDITORIA — CONFIGURAÇÕES

O código pode deixar o administrador sem acesso à própria configuração.

Nova regra de proteção:

```text
pelo menos um DONO deve permanecer com acesso às configurações.
```

---

# 215. AUDITORIA — CONFIGURAÇÕES: ERROS

O fluxo de salvar deve usar:

```text
try/catch/finally
```

Nunca deixar:

```text
saving = true
```

preso em erro.

---

# 216. AUDITORIA — TOAST

Existe mistura de sistemas de toast.

Adotar uma única solução.

---

# 217. AUDITORIA — THEME

Não usar seletores genéricos como:

```text
[class*="bg-white"]
[class*="border"]
```

para hackear dark mode.

Usar:

```text
CSS variables
[data-theme]
```

---

# 218. AUDITORIA — AUTHENTICATION

O `AuthContext` atual é todo baseado em Base44.

Nova autenticação será própria:

```text
Spring Security
JWT
access token
refresh token
```

---

# 219. AUDITORIA — NAVIGATION TRACKER

`NavigationTracker` atualmente registra atividade em Base44.

Substituir por:

```text
auditoria própria
```

ou remover se não houver necessidade.

---

# 220. AUDITORIA — VISUAL EDIT AGENT

`VisualEditAgent` é específico do ambiente Base44.

Remover.

---

# 221. AUDITORIA — DUPLICATE PROTECTED ROUTE

Existem mais de uma implementação de `ProtectedRoute`.

Nova aplicação terá uma única implementação.

---

# 222. AUDITORIA — PÁGINAS FORNECEDORES/PRODUÇÃO

O código atual possui inclusão manual fora do `PAGES` principal.

Nova aplicação terá única tabela de rotas.

---

# 223. AUDITORIA — DEPENDÊNCIAS EXCESSIVAS

Não transportar automaticamente todas as dependências atuais.

Remover o que não for usado.

Especialmente:

```text
three
react-leaflet
react-markdown
react-quill
html2canvas
jspdf
moment
canvas-confetti
```

salvo uso futuro claramente definido.

---

# 224. MODELAGEM FINAL — PRODUTO

A entidade final deve possuir pelo menos:

```text
id
nome
id_tipo_produto
categoria
cor
tamanho
id_unidade_medida
pode_ser_vendido
pode_ser_materia_prima
estoque_comercial
estoque_producao
estoque_minimo_comercial
estoque_minimo_producao
preco_custo
preco_venda
codigo_barras
id_fornecedor_principal
observacoes
ativo
```

---

# 225. MODELAGEM FINAL — CLIENTE

```text
id
nome
telefone
email
razao_social
cnpj
endereco
bairro
cidade
cep
data_nascimento
observacoes
status_cadastral
ultima_compra
total_compras
ativo
auditoria
```

---

# 226. MODELAGEM FINAL — VENDA

```text
id
numero_venda
id_cliente
id_funcionario_vendedor
data_venda
subtotal
desconto
total_venda
id_forma_pagamento
id_status_venda
observacoes
cancelled_at
cancelled_by
motivo_cancelamento
auditoria
```

---

# 227. MODELAGEM FINAL — VENDA ITEM

```text
id
id_venda
id_produto
produto_nome_snapshot
unidade_medida_snapshot
quantidade
preco_unitario
subtotal
auditoria
```

---

# 228. MODELAGEM FINAL — COMPRA

```text
id
id_fornecedor
fornecedor_nome_manual
data_compra
total_custo
status
observacoes
cancelled_at
cancelled_by
motivo_cancelamento
auditoria
```

---

# 229. MODELAGEM FINAL — COMPRA ITEM

```text
id
id_compra
id_produto
destino_estoque
quantidade
preco_unitario
subtotal
produto_nome_snapshot
auditoria
```

---

# 230. MODELAGEM FINAL — ENTREGA

```text
id
id_venda
id_cliente
endereco_entrega
bairro
cidade
cep
telefone_contato
data_agendada
horario
id_entregador
id_status_entrega
valor_frete
observacoes
data_entrega
auditoria
```

---

# 231. MODELAGEM FINAL — ORDEM PRODUÇÃO

```text
id
numero_ordem
id_produto_acabado
quantidade_produzida
id_cliente
tipo_ordem
id_fornecedor
data_ordem
status
observacoes
data_finalizacao
id_usuario_finalizacao
auditoria
```

---

# 232. MODELAGEM FINAL — PRODUÇÃO ITEM

```text
id
id_ordem_producao
id_produto_materia_prima
quantidade_usada
produto_nome_snapshot
unidade_snapshot
auditoria
```

---

# 233. MODELAGEM FINAL — DESPESA

```text
id
id_tipo_despesa
descricao
valor
data_despesa
observacoes
auditoria
```

---

# 234. MODELAGEM FINAL — FORNECEDOR

```text
id
nome_empresa
cnpj
telefone
email
observacoes
ativo
auditoria
```

---

# 235. MODELAGEM FINAL — PRODUTO/FORNECEDOR

```text
id
id_produto
id_fornecedor
principal
observacoes
auditoria
```

Permitir vários fornecedores por produto.

---

# 236. MOVIMENTOS DE ESTOQUE — ORIGEM

Toda movimentação deve identificar a origem:

```text
VENDA
COMPRA
PRODUCAO
AJUSTE
CANCELAMENTO
```

---

# 237. MOVIMENTOS DE ESTOQUE — AUDITORIA

Registrar:

```text
saldo comercial antes/depois
saldo produção antes/depois
usuário
momento
origem
```

---

# 238. REGRAS DE ESTOQUE — RESUMO

```text
Compra COMERCIAL -> estoque_comercial + quantidade
Compra PRODUCAO -> estoque_producao + quantidade
Venda -> estoque_comercial - quantidade
Produção consumo -> estoque_producao - quantidade
Produção saída -> estoque_comercial + quantidade
Estorno venda -> estoque_comercial + quantidade
Estorno compra -> saldo correspondente - quantidade
Ajuste -> saldo correspondente alterado com movimento
```

---

# 239. REGRA DE NÃO NEGATIVO

Nenhum saldo físico deverá ser negativo.

Se operação resultaria em saldo negativo:

```text
REJEITAR
```

não:

```text
salvar negativo
```

---

# 240. DEDUÇÃO DE ESTOQUE COM LOCK

Para saída:

```sql
SELECT ... FOR UPDATE
```

ou equivalente atômico.

---

# 241. TRANSAÇÕES EXTERNAS

Não chamar serviços externos críticos antes do commit da transação principal.

Fluxo:

```text
DB commit
   ↓
evento
   ↓
notificação
```

Se e-mail falhar:

```text
venda continua válida
```

registrando falha da notificação.

---

# 242. NOTIFICAÇÕES — RETRY

Falha de e-mail/integração deve permitir retry.

Não fazer rollback da venda por falha de SMTP.

---

# 243. TESTE DE NOTIFICAÇÃO

Testar:

```text
notificação habilitada
notificação desabilitada
canal email
canal whatsapp
falha externa
retry
```

---

# 244. ORDEM DE PRODUÇÃO — PRIORIDADE

Prioridade:

```text
esgotado
baixo/no mínimo
normal
```

Depois:

```text
maior déficit
nome
```

---

# 245. ESTOQUE — ALERTA DUPLICADO

Não mandar nova notificação simplesmente porque uma tela consultou o banco.

Registrar evento/estado para deduplicação.

---

# 246. CÁLCULO DE `total_compras`

Preferir manter como campo de leitura rápida, mas sua atualização precisa ocorrer transacionalmente.

Disponibilizar rotina administrativa de reconciliação:

```text
recalcular total do cliente a partir de vendas finalizadas
```

---

# 247. CÁLCULO DE `ultima_compra`

Sempre baseado na maior `data_venda` de venda:

```text
FINALIZADA
```

---

# 248. VENDA — CANCELAMENTO E CLIENTE

Ao cancelar:

```text
recalcular última compra
recalcular total comprado
```

Isso evita erros quando a venda cancelada era a última compra.

---

# 249. COMPRA — CANCELAMENTO E ESTOQUE

Ao cancelar:

```text
reverter somente o movimento daquela compra
```

Não alterar historicamente outros movimentos.

---

# 250. PRODUÇÃO — CANCELAMENTO

Primeira versão:

```text
cancelar somente ordens ABERTAS
```

Não excluir.

Estorno de ordem finalizada fica para operação administrativa específica.

---

# 251. SEGURANÇA — DONO

O DONO possui maior privilégio, mas ainda deve passar por autenticação.

Não hardcodar senha.

---

# 252. SEGURANÇA — GERENTE

Gerente possui acesso administrativo configurável, mas não deve receber automaticamente:

```text
tudo que DONO recebe
```

---

# 253. SEGURANÇA — VENDEDOR

Vendedor não deve:

```text
cancelar venda
ver salário
ver relatório financeiro
editar funcionário
editar produto
```

salvo permissão explícita em configuração para ações não críticas.

---

# 254. SEGURANÇA — ESTOQUISTA

Estoquista pode:

```text
visualizar estoque
ajustar estoque
produzir
```

mas não deve:

```text
ver salários
ver relatórios financeiros
cancelar vendas
```

por padrão.

---

# 255. SEGURANÇA — ENTREGADOR

Entregador deve ver somente informações necessárias para entrega.

Não deve ter acesso a:

```text
salários
compras
relatórios
configurações
```

---

# 256. DADOS SENSÍVEIS DO FUNCIONÁRIO

Salário deve ser retornado somente para cargos/permissões autorizadas.

Mesmo que o endpoint de funcionários seja acessível, campos sensíveis devem ser controlados.

---

# 257. DTO DIFERENCIADO PARA FUNCIONÁRIO

Preferir:

```text
FuncionarioListResponse
FuncionarioDetailsResponse
```

para evitar exposição desnecessária.

---

# 258. DTO DIFERENCIADO PARA DASHBOARD

Dashboard deve retornar somente informações necessárias.

---

# 259. API — PRINCÍPIO MINIMUM DATA

Não retornar entidade inteira quando a tela precisa de três campos.

---

# 260. FRONTEND — CÓDIGO

Não utilizar:

```text
any
```

como solução para estruturar dados no JavaScript.

Criar objetos coerentes e validação com Zod quando útil.

---

# 261. ZOD

Utilizar Zod para validação do formulário no frontend quando isso melhorar consistência.

Ainda assim:

```text
backend valida novamente
```

---

# 262. REACT HOOK FORM

Utilizar React Hook Form nos formulários complexos:

```text
Venda
Compra
Produto
OrdemProducao
Cliente
Funcionario
Entrega
Configuracao
```

---

# 263. FORMULÁRIOS — DEFAULTS

Criar `defaultValues` explícitos.

Não depender de `undefined`.

---

# 264. FORMULÁRIOS — RESET

Após salvar:

```text
fechar
reset
invalidate query
```

---

# 265. FORMULÁRIOS — ERRO

Exibir erro do backend no campo quando o erro for específico.

---

# 266. FRONTEND — ACCESSIBILITY

Ícones de ação devem ter `aria-label`.

Exemplo:

```text
aria-label="Editar cliente"
aria-label="Excluir cliente"
aria-label="Visualizar cliente"
```

---

# 267. FRONTEND — CONFIRMAÇÃO

Ações destrutivas sempre usam confirmação:

```text
cancelar venda
cancelar compra
ajustar estoque
cancelar ordem
```

---

# 268. FRONTEND — SEARCH

Busca remota com debounce.

---

# 269. FRONTEND — QUERY CACHE

Após alteração:

```text
invalidate
```

em vez de recarregar a página inteira.

---

# 270. FRONTEND — RELOAD

Não usar:

```text
window.location.reload()
```

como mecanismo normal de atualização.

---

# 271. FRONTEND — ERROR BOUNDARY

Criar Error Boundary para falhas inesperadas do React.

---

# 272. FRONTEND — 404

Página própria:

```text
404
Página não encontrada
Voltar ao Dashboard
```

Sem dependência Base44.

---

# 273. BACKEND — PACKAGE `security`

Criar:

```text
SecurityConfig
JwtAuthenticationFilter
JwtTokenService
RefreshTokenService
AuthenticatedUser
```

---

# 274. REFRESH TOKEN

Não armazenar refresh token em localStorage.

Preferir cookie HttpOnly.

---

# 275. ACCESS TOKEN

Pode permanecer em memória no frontend.

---

# 276. LOGOUT

Revogar/invalidar refresh token conforme estratégia escolhida.

---

# 277. PASSWORD

Usar encoder forte compatível com Spring Security.

---

# 278. PASSWORD POLICY

No mínimo:

```text
8 caracteres
```

mais regras podem ser acrescentadas.

---

# 279. ACCOUNT LOCK

Preparar para:

```text
tentativas
bloqueio temporário
```

sem obrigar implementação complexa na primeira versão.

---

# 280. LOGIN RATE LIMIT

Aplicar proteção contra brute force.

---

# 281. CORS

Origem configurável.

---

# 282. CSRF

Se refresh token for cookie:

```text
avaliar CSRF adequadamente
```

Não ignorar por padrão.

---

# 283. HTTPS

Produção obrigatoriamente HTTPS.

---

# 284. OPENAPI

Documentar autorização:

```text
Bearer token
```

se access token for bearer.

---

# 285. AUDITORIA — IP

Registrar IP somente quando tecnicamente disponível e necessário.

---

# 286. AUDITORIA — USER AGENT

Registrar para operações administrativas quando útil.

---

# 287. AUDITORIA — DADOS

Não gravar senha, token ou segredo em `dados_anteriores/dados_novos`.

---

# 288. MIGRATION — NOMES

Toda migration deve seguir:

```text
Vn__descricao.sql
```

---

# 289. MIGRATION — IDEMPOTÊNCIA

Migration Flyway não precisa ser idempotente manualmente; deve ser versionada corretamente.

---

# 290. SEED UTILITÁRIO

Seeds de utilitários devem ser determinísticos.

---

# 291. SEED CARGOS

Criar exatamente:

```text
DONO
GERENTE
VENDEDOR
ESTOQUISTA
ENTREGADOR
```

---

# 292. SEED PERMISSÕES

Criar defaults correspondentes às regras desta especificação.

---

# 293. SQL DE ÍNDICES

Criar índices em:

```text
FKs de consulta
status
datas
buscas
```

---

# 294. INDEX — CLIENTE

```text
idx_cliente_nome
idx_cliente_telefone
idx_cliente_cidade
idx_cliente_ultima_compra
```

---

# 295. INDEX — PRODUTO

```text
idx_produto_nome
idx_produto_tipo
idx_produto_codigo_barras
idx_produto_ativo
```

---

# 296. INDEX — VENDA

```text
idx_venda_data
idx_venda_status
idx_venda_cliente
idx_venda_vendedor
```

---

# 297. INDEX — COMPRA

```text
idx_compra_data
idx_compra_fornecedor
idx_compra_status
```

---

# 298. INDEX — MOVIMENTO

```text
idx_movimento_produto
idx_movimento_data
idx_movimento_origem
```

---

# 299. FOREIGN KEYS

Padrão:

```text
fk_cliente_X_status_cliente
fk_funcionario_X_cargo
fk_produto_X_tipo_produto
fk_venda_X_cliente
fk_venda_X_funcionario
fk_venda_X_status_venda
fk_venda_item_X_venda
fk_venda_item_X_produto
```

e equivalentes para as demais entidades.

---

# 300. BANCO — INTEGRIDADE

Criar constraints para:

```text
quantidade > 0
preço >= 0
saldo >= 0
```

conforme o papel do campo.

---

# 301. API — TRANSAÇÃO DE VENDA

O endpoint `POST /api/v1/vendas` deve aceitar somente dados necessários:

```json
{
  "clienteId": "...",
  "vendedorId": "...",
  "dataVenda": "2026-09-09",
  "desconto": 0,
  "formaPagamento": "PIX",
  "itens": [
    {
      "produtoId": "...",
      "quantidade": 2
    }
  ],
  "observacoes": ""
}
```

O backend busca:

```text
nome
preço
estoque
unidade
```

do banco.

---

# 302. API — VENDA NÃO ACEITA

Não aceitar como autoridade:

```text
subtotal
preçoUnitário
subtotalItem
total
estoqueAnterior
estoqueDisponivel
produtoNome
```

Esses valores podem ser tratados como somente apresentação, nunca como fonte de verdade.

---

# 303. API — COMPRA

Request semelhante:

```json
{
  "fornecedorId": "...",
  "fornecedorNomeManual": null,
  "dataCompra": "2026-09-09",
  "observacoes": "",
  "itens": [
    {
      "produtoId": "...",
      "destinoEstoque": "PRODUCAO",
      "quantidade": 20,
      "precoUnitario": 10.50
    }
  ]
}
```

Servidor calcula subtotal/total.

---

# 304. API — ORDEM PRODUÇÃO

```json
{
  "produtoAcabadoId": "...",
  "quantidadeProduzida": 10,
  "tipoOrdem": "ESTOQUE",
  "clienteId": null,
  "fornecedorId": null,
  "materiasPrimas": [
    {
      "produtoId": "...",
      "quantidadeUsada": 20
    }
  ],
  "observacoes": ""
}
```

---

# 305. API — ENTREGA

Ao criar entrega:

```text
vendaId
```

é suficiente para localizar cliente/endereço.

Ainda permitir override explícito do endereço de entrega.

---

# 306. API — STATUS DE ENTREGA

```http
PATCH /api/v1/entregas/{id}/status
```

Request:

```json
{
  "status": "SAIU_PARA_ENTREGA"
}
```

Backend valida transição.

---

# 307. API — DASHBOARD

Retornar dados agregados.

---

# 308. API — RELATÓRIOS

Parâmetros:

```text
periodo
inicio
fim
```

---

# 309. API — PERMISSÕES

Frontend deve receber autorização pronta:

```json
{
  "pages": {
    "dashboard": true,
    "clientes": true
  },
  "actions": {
    "clientes_criar": true,
    "clientes_editar": false
  }
}
```

---

# 310. FRONTEND — SIDEBAR

Sidebar deve consumir permissões da API.

---

# 311. FRONTEND — ESTOQUE BADGE

Usar:

```text
GET /estoque/resumo
```

não:

```text
Produto.list() inteiro
```

---

# 312. FRONTEND — CLIENTES

Não calcular status de relacionamento novamente.

---

# 313. FRONTEND — ESTOQUE

Não calcular saldo.

Somente exibir o saldo enviado pela API.

---

# 314. FRONTEND — RELATÓRIOS

Não calcular lucro final.

Backend retorna:

```text
receita
compras
salários
outras despesas
lucro
```

---

# 315. FRONTEND — VENDA

Total visual é apenas preview.

O retorno da API é a fonte final.

---

# 316. FRONTEND — FORMATAÇÃO

Moeda:

```text
Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
```

Datas:

```text
DD/MM/YYYY
```

---

# 317. FRONTEND — TELEFONE

Mostrar máscara.

Armazenar normalizado.

---

# 318. FRONTEND — CNPJ

Mostrar máscara.

Armazenar 14 dígitos.

---

# 319. FRONTEND — CEP

Mostrar máscara.

Armazenar 8 dígitos.

---

# 320. FRONTEND — DARK MODE

Persistir preferência do usuário/configuração.

Não usar reload completo.

---

# 321. UI — CLIENTES

Manter cards como referência visual.

---

# 322. UI — FUNCIONÁRIOS

Manter cards como referência visual.

---

# 323. UI — ESTOQUE

Tabela administrativa com status bem visível.

---

# 324. UI — VENDAS

Tabela administrativa + modal/form de venda.

---

# 325. UI — RELATÓRIOS

Cards financeiros + gráfico diário.

---

# 326. UI — ORDEM PRODUÇÃO

Tabela + modal de criação/finalização.

---

# 327. UI — CONFIGURAÇÕES

Matriz visual de permissões.

---

# 328. UI — ALERTAS

Alertas devem mostrar texto, não somente cor.

---

# 329. UI — DELEÇÃO

Usar vermelho e confirmação.

---

# 330. UI — STATUS

Não depender somente de cor.

---

# 331. BUILD — VALIDAÇÃO

O Gemini deve executar:

```bash
./mvnw clean verify
npm ci
npm run lint
npm run test
npm run build
```

---

# 332. BUILD — BASE44

Depois do build, verificar:

```bash
rg -n "@base44|base44|Base44|legacySDKImports" .
```

Nenhum resultado em runtime source.

---

# 333. BUILD — TYPESCRIPT

Verificar:

```bash
find frontend/src -type f \( -name '*.ts' -o -name '*.tsx' \)
```

Resultado:

```text
vazio
```

---

# 334. BUILD — CASE SENSITIVITY

O projeto deve compilar em ambiente Linux.

---

# 335. TESTE — NOVO CLIENTE

Criar cliente.

Verificar:

```text
ultima_compra = NULL
total_compras = 0
classificação = NOVO
```

---

# 336. TESTE — VENDA

Criar venda.

Verificar:

```text
venda finalizada
estoque comercial baixado
movimentos gerados
cliente atualizado
```

---

# 337. TESTE — CANCELAMENTO VENDA

Verificar:

```text
status CANCELADA
estoque restaurado
movimento de estorno
cliente recalculado
```

---

# 338. TESTE — COMPRA COMERCIAL

Verificar:

```text
estoque comercial += quantidade
```

---

# 339. TESTE — COMPRA PRODUÇÃO

Verificar:

```text
estoque produção += quantidade
```

---

# 340. TESTE — PRODUÇÃO

Verificar:

```text
estoque produção -= matéria-prima
estoque comercial += produto acabado
```

---

# 341. TESTE — MATÉRIA-PRIMA INSUFICIENTE

Esperado:

```text
409
rollback completo
```

---

# 342. TESTE — VENDA CONCORRENTE

Esperado:

```text
apenas uma operação aprovada quando saldo = 1
```

---

# 343. TESTE — PERMISSÃO

Vendedor tenta cancelar venda.

Esperado:

```text
403
```

---

# 344. TESTE — SALÁRIO

Vendedor tenta consultar salário.

Esperado:

```text
403
```

ou DTO sem campo sensível conforme endpoint.

---

# 345. TESTE — PRODUÇÃO ENCOMENDA

Tentar criar sem cliente.

Esperado:

```text
422
```

---

# 346. TESTE — PRODUTO INATIVO

Tentar vender.

Esperado:

```text
409/422 PRODUTO_INATIVO
```

---

# 347. TESTE — COMPRA SEM PRODUTO

Esperado:

```text
422
```

para linha de estoque.

---

# 348. TESTE — ESTOQUE DIRETO

Não deve existir endpoint genérico:

```text
PUT /produtos/{id} -> estoque
```

sem mecanismo de movimento.

---

# 349. TESTE — AJUSTE DE ESTOQUE

Deve gerar:

```text
movimento
auditoria
```

---

# 350. TESTE — RELATÓRIO DO DIA

Venda ontem não aparece.

Venda hoje aparece.

---

# 351. TESTE — RELATÓRIO QUINZENA

Data 15 e data 16 devem pertencer a quinzenas diferentes.

---

# 352. TESTE — RELATÓRIO VENDA CANCELADA

Venda cancelada não entra em receita.

---

# 353. TESTE — CLIENTE INATIVO

Cliente cadastralmente INATIVO permanece INATIVO independentemente de data da última compra.

---

# 354. TESTE — CLIENTE NOVO

Cliente sem compra aparece:

```text
NOVO
```

---

# 355. TESTE — CLIENTE REATIVAÇÃO

Ao comprar novamente:

```text
ultima_compra = data da venda
status comportamento = ATIVO
```

---

# 356. TESTE — ENTREGA

Entrega para venda cancelada deve falhar.

---

# 357. TESTE — ENTREGA STATUS

Transição inválida deve retornar conflito.

---

# 358. TESTE — CONFIGURAÇÃO SELF-LOCKOUT

Impedir remoção do último DONO das configurações.

---

# 359. TESTE — NOTIFICAÇÃO

Falha de e-mail não deve cancelar venda.

---

# 360. CRITÉRIO ABSOLUTO — DADOS FINANCEIROS

Nunca confiar em dados financeiros calculados pelo navegador.

---

# 361. CRITÉRIO ABSOLUTO — ESTOQUE

Nunca confiar em saldo recebido do navegador.

---

# 362. CRITÉRIO ABSOLUTO — PERMISSÃO

Nunca confiar em cargo/permissão enviado do navegador.

---

# 363. CRITÉRIO ABSOLUTO — HISTÓRICO

Nunca apagar histórico de estoque/venda/compra sem mecanismo explícito de auditoria.

---

# 364. CRITÉRIO ABSOLUTO — IDEMPOTÊNCIA

Toda operação crítica deve possuir proteção contra repetição.

---

# 365. CRITÉRIO ABSOLUTO — TRANSAÇÃO

Nenhum fluxo poderá deixar dados parcialmente alterados.

---

# 366. CRITÉRIO ABSOLUTO — DEPENDÊNCIAS

Nenhuma dependência Base44 em produção.

---

# 367. CRITÉRIO ABSOLUTO — FRONTEND

JavaScript puro, React/JSX, sem TypeScript.

---

# 368. CRITÉRIO ABSOLUTO — BANCO

Respeitar:

```text
varchar(38)
id_nome
idx_nome
fk_origem_X_destino
schema utilitarios
serial utilitários
```

---

# 369. CRITÉRIO ABSOLUTO — VISUAL

Preservar a experiência demonstrada:

```text
sidebar
cards
tabelas
modais
badges
alertas
```

mas com implementação própria.

---

# 370. PROMPT FINAL — COMEÇO DA EXECUÇÃO PELO GEMINI

> **IMPORTANTE: todo o conteúdo desta especificação deve ser tratado como requisito de implementação.**
>
> Você recebeu uma especificação consolidada após análise dos documentos funcionais, documentação da arquitetura, fontes do Base44 e demonstração visual.
>
> Não faça uma conversão literal do Base44.
>
> Reconstrua a aplicação usando arquitetura própria.
>
> A aplicação deverá utilizar Java 21 + Spring Boot 4.1.1 + PostgreSQL + Flyway + Spring Security + API REST no backend e JavaScript + React + Vite no frontend.
>
> Não utilize TypeScript.
>
> Não utilize Base44.
>
> Não copie código Base44 como dependência.
>
> Preserve os fluxos de negócio existentes, mas corrija os defeitos explicitamente indicados nesta especificação.
>
> O sistema deve possuir integridade transacional, controle de concorrência, auditoria, permissões reais no servidor e frontend responsivo.
>
> Utilize estoque comercial e estoque de produção separados.
>
> Permita que uma mesma malha possa ser vendida e utilizada como matéria-prima.
>
> Nunca aceite do frontend estoque anterior, total financeiro, preço final ou permissão como fonte de verdade.
>
> Gere o número da venda no servidor.
>
> Nunca exclua fisicamente venda que já impactou estoque.
>
> Nunca permita estoque negativo.
>
> Nunca finalize uma ordem de produção duas vezes.
>
> Todas as operações críticas devem ser transacionais.
>
> Entregue banco, backend, frontend, testes, documentação e configuração de execução.

---

# 371. ORDEM EXATA DE EXECUÇÃO PELO GEMINI

```text
PASSO 1 — criar estrutura monorepo
PASSO 2 — criar backend
PASSO 3 — criar frontend
PASSO 4 — configurar PostgreSQL/Docker
PASSO 5 — configurar Flyway
PASSO 6 — criar utilitários
PASSO 7 — criar cargos/usuários
PASSO 8 — criar autenticação
PASSO 9 — criar permissões
PASSO 10 — criar clientes
PASSO 11 — criar funcionários
PASSO 12 — criar fornecedores
PASSO 13 — criar produtos
PASSO 14 — criar estoque/movimentos
PASSO 15 — criar compras
PASSO 16 — criar vendas
PASSO 17 — criar entregas
PASSO 18 — criar produção
PASSO 19 — criar despesas
PASSO 20 — criar relatórios
PASSO 21 — criar dashboard
PASSO 22 — criar configurações
PASSO 23 — criar notificações
PASSO 24 — criar integrações preparadas
PASSO 25 — testes unitários
PASSO 26 — testes integração
PASSO 27 — lint/build
PASSO 28 — documentação final
```

---

# 372. NÃO TERMINAR COM MOCK

O projeto somente poderá ser considerado concluído quando o frontend estiver ligado ao backend real.

---

# 373. NÃO TERMINAR COM TODO

Não deixar:

```text
TODO
FIXME
IMPLEMENTAR
A FAZER
MOCK
PLACEHOLDER
```

nos fluxos principais.

---

# 374. NÃO TERMINAR COM IMPORT QUEBRADO

Build deve passar.

---

# 375. NÃO TERMINAR COM REGRA NO FRONTEND

Regras de negócio devem estar no backend.

---

# 376. NÃO TERMINAR SEM TESTE DE ESTOQUE

Estoque é o ponto mais crítico do sistema.

---

# 377. NÃO TERMINAR SEM TESTE DE CANCELAMENTO

Venda e compra precisam de estorno seguro.

---

# 378. NÃO TERMINAR SEM TESTE DE PERMISSÃO

Cada papel deve ser validado.

---

# 379. NÃO TERMINAR SEM TESTE DE CONCORRÊNCIA

Pelo menos venda concorrente e produção concorrente devem ser considerados.

---

# 380. NÃO TERMINAR SEM DOCUMENTAÇÃO

Criar:

```text
README.md
docs/arquitetura.md
docs/regras-negocio.md
docs/banco.md
docs/api.md
docs/permissoes.md
```

---

# 381. CHECKLIST FINAL DO GEMINI — IMPLEMENTAÇÃO

```text
[ ] Git inicializado
[ ] .gitignore
[ ] .env.example
[ ] README
[ ] Docker Compose
[ ] Backend Java 21
[ ] Spring Boot 4.1.1
[ ] Maven wrapper
[ ] Frontend JavaScript
[ ] React
[ ] Vite
[ ] package-lock
[ ] ESLint
[ ] Vitest
[ ] PostgreSQL
[ ] Flyway
[ ] Security
[ ] JWT
[ ] OpenAPI
[ ] Actuator
[ ] DTOs
[ ] Services
[ ] Repositories
[ ] Exceptions
[ ] Transactions
[ ] Locks
[ ] Audit
[ ] Notifications
[ ] Permissions
[ ] Dashboard
[ ] Clientes
[ ] Funcionários
[ ] Estoque
[ ] Vendas
[ ] Entregas
[ ] Compras
[ ] Fornecedores
[ ] Produção
[ ] Relatórios
[ ] Integrações
[ ] Configurações
[ ] Tema claro/escuro
[ ] Responsividade
[ ] Testes
[ ] Build
[ ] Sem Base44
[ ] Sem TypeScript
```

---

# 382. RESULTADO FINAL DESEJADO

A aplicação deverá funcionar como um sistema empresarial real, e não como uma réplica superficial da interface.

A prioridade será:

```text
1. integridade dos dados
2. integridade do estoque
3. segurança
4. permissões
5. fidelidade funcional
6. fidelidade visual
7. performance
8. extensibilidade
```

---

# 383. DECISÃO DE ARQUITETURA MAIS IMPORTANTE DESTE PROJETO

A modelagem não deve reproduzir a limitação do Base44 que tratava uma entidade Produto com um único `tipo_estoque` como se isso fosse suficiente.

O novo sistema deve representar o problema físico real da empresa:

```text
um mesmo material pode possuir saldo destinado à venda
E
saldo destinado à produção.
```

Portanto:

```text
PRODUTO
   |
   +--> estoque_comercial
   |
   +--> estoque_producao
```

Esse desenho é parte central do novo domínio.

---

# 384. DECISÃO DE ARQUITETURA — HISTÓRICO

O saldo atual é somente o estado atual.

O histórico de como chegou a esse saldo deve existir em:

```text
movimento_estoque
```

Assim será possível auditar:

```text
quem
quando
por quê
quanto entrou
quanto saiu
quanto restou
```

---

# 385. DECISÃO DE ARQUITETURA — FINANCEIRO

Vendas e compras representam documentos de negócio.

Despesas representam gastos.

Cancelamentos representam reversões.

Relatórios representam agregações.

Não alterar diretamente um valor histórico para "consertar" um relatório.

---

# 386. DECISÃO DE ARQUITETURA — PERMISSÕES

Permissão é regra de segurança, não componente visual.

Portanto:

```text
frontend = UX
backend = segurança real
```

---

# 387. DECISÃO DE ARQUITETURA — INTEGRAÇÕES

Integrações externas são auxiliares.

Falha de integração não deve destruir a operação principal já confirmada.

---

# 388. DECISÃO DE ARQUITETURA — FUTURO

O sistema deverá ser preparado para:

```text
PIX dinâmico
WhatsApp API
Mercado Livre
e-commerce
catálogo público
IA
previsão de estoque
automação de clientes
```

Sem implementar complexidade prematura.

---

# 389. REFERÊNCIAS TÉCNICAS ATUAIS UTILIZADAS NESTA REVISÃO

Na revisão técnica foram conferidas as versões atuais das documentações oficiais em 2026-09-09.

```text
Spring Boot estável: 4.1.1
Springdoc: 3.1.1
Testcontainers PostgreSQL: 2.0.1
JJWT: 0.13.0
```

Spring Boot 4.1.1 requer Java 17+ e é compatível com Java 21. O starter MVC atualmente recomendado é `spring-boot-starter-webmvc`. citeturn294347search0turn278926search0

Springdoc documenta `springdoc-openapi-starter-webmvc-ui` 3.1.1 para a integração atual. citeturn520253search0

Testcontainers documenta `testcontainers-postgresql` 2.0.1. citeturn294347search1

JJWT 0.13.0 utiliza `jjwt-api` e os módulos `jjwt-impl`/`jjwt-jackson` em runtime. citeturn294347search12

---

# 390. FIM — ESPECIFICAÇÃO DEFINITIVA

**Este arquivo agora representa a versão revisada após a análise dos documentos, código-fonte e demonstração.**

Não voltar a tratar o Base44 como backend.

Não transportar bugs identificados.

Não relaxar as regras de transação, estoque, segurança e permissões para "ficar igual ao Base44".

A meta é:

```text
MESMA EXPERIÊNCIA DE NEGÓCIO
+
INTERFACE EQUIVALENTE/MELHORADA
+
DADOS MAIS ÍNTEGROS
+
SEGURANÇA REAL
+
ARQUITETURA PRÓPRIA
+
PREPARAÇÃO PARA CRESCIMENTO
```



# 401. ANEXO — SCHEMAS JSON ORIGINAIS DO BASE44

Este anexo incorpora os arquivos presentes em `Entidades.zip` para que o prompt de implementação seja autossuficiente.

**Importante:** este ZIP contém oficialmente seis schemas JSON: `Cliente`, `Compra`, `Configuracao`, `Fornecedor`, `OrdemProducao` e `Produto`. Ele **não** contém JSONs separados para `Venda`, `Funcionario`, `Entrega` e `Despesa`; essas entidades devem continuar sendo derivadas do código-fonte e da documentação, e não devem ser tratadas como se possuíssem um schema JSON oficial neste pacote.

## 401.1 Regra de leitura dos JSONs

Para cada entidade, distinguir sempre:

1. **Schema original Base44** — o que o JSON declara.
2. **Modelo relacional novo** — como a informação será normalizada no PostgreSQL.
3. **Correções de negócio** — mudanças necessárias para eliminar bugs/inconsistências.
4. **Regras de implementação** — validações, constraints, transações e permissões.

O JSON original **não** deve ser transformado mecanicamente em uma tabela SQL. Ele é a fonte estrutural do modelo legado; as regras desta especificação determinam a arquitetura nova.

## 401.2 Matriz de entidades encontradas

| Entidade | JSON oficial no ZIP | Código/documentação | Modelagem nova |
|---|---:|---:|---:|
| Cliente | SIM | SIM | SIM |
| Compra | SIM | SIM | SIM |
| Configuracao | SIM | SIM | SIM |
| Fornecedor | SIM | SIM | SIM |
| OrdemProducao | SIM | SIM | SIM |
| Produto | SIM | SIM | SIM |
| Venda | NÃO | SIM | SIM |
| Funcionario | NÃO | SIM | SIM |
| Entrega | NÃO | SIM | SIM |
| Despesa | NÃO | SIM | SIM |

---

## 401.3 — Cliente

| Campo | Tipo JSON | Obrigatório | Formato/Enum | Default | Descrição |
|---|---|---:|---|---|---|
| `nome` | `string` | SIM | — | `—` | Nome completo do cliente |
| `razao_social` | `string` | NÃO | — | `—` | Razão social do cliente |
| `cnpj` | `string` | NÃO | — | `—` | CNPJ do cliente |
| `telefone` | `string` | SIM | — | `—` | Telefone com WhatsApp |
| `email` | `string` | NÃO | email | `—` | Email do cliente |
| `endereco` | `string` | NÃO | — | `—` | Endereço completo |
| `bairro` | `string` | NÃO | — | `—` | Bairro do cliente |
| `cidade` | `string` | NÃO | — | `—` | Cidade |
| `cep` | `string` | NÃO | — | `—` | CEP |
| `data_nascimento` | `string` | NÃO | date | `—` | Data de nascimento |
| `observacoes` | `string` | NÃO | — | `—` | Observações sobre o cliente |
| `ultima_compra` | `string` | NÃO | date | `—` | Data da última compra |
| `total_compras` | `number` | NÃO | — | `0` | Valor total gasto pelo cliente |
| `status` | `string` | NÃO | ativo, inativo | `"ativo"` | Status do cliente |

### Regras do schema original

- Obrigatórios declarados: `nome`, `telefone`.

### JSON original completo

```json
{
  "name": "Cliente",
  "type": "object",
  "properties": {
    "nome": {
      "type": "string",
      "description": "Nome completo do cliente"
    },
    "razao_social": {
      "type": "string",
      "description": "Razão social do cliente"
    },
    "cnpj": {
      "type": "string",
      "description": "CNPJ do cliente"
    },
    "telefone": {
      "type": "string",
      "description": "Telefone com WhatsApp"
    },
    "email": {
      "type": "string",
      "format": "email",
      "description": "Email do cliente"
    },
    "endereco": {
      "type": "string",
      "description": "Endereço completo"
    },
    "bairro": {
      "type": "string",
      "description": "Bairro do cliente"
    },
    "cidade": {
      "type": "string",
      "description": "Cidade"
    },
    "cep": {
      "type": "string",
      "description": "CEP"
    },
    "data_nascimento": {
      "type": "string",
      "format": "date",
      "description": "Data de nascimento"
    },
    "observacoes": {
      "type": "string",
      "description": "Observações sobre o cliente"
    },
    "ultima_compra": {
      "type": "string",
      "format": "date",
      "description": "Data da última compra"
    },
    "total_compras": {
      "type": "number",
      "default": 0,
      "description": "Valor total gasto pelo cliente"
    },
    "status": {
      "type": "string",
      "enum": [
        "ativo",
        "inativo"
      ],
      "default": "ativo",
      "description": "Status do cliente"
    }
  },
  "required": [
    "nome",
    "telefone"
  ]
}
```

## 401.4 — Compra

| Campo | Tipo JSON | Obrigatório | Formato/Enum | Default | Descrição |
|---|---|---:|---|---|---|
| `fornecedor` | `string` | SIM | — | `—` | Fornecedor da compra |
| `fornecedor_id` | `string` | NÃO | — | `—` | ID do fornecedor |
| `fornecedor_nome` | `string` | NÃO | — | `—` | Nome do fornecedor vinculado |
| `data_compra` | `string` | SIM | date | `—` | Data da compra |
| `itens` | `array<object>` | NÃO | — | `—` | Itens comprados |
| `total_custo` | `number` | SIM | — | `—` | Custo total da compra |
| `observacoes` | `string` | NÃO | — | `—` | Observações |

### Regras do schema original

- Obrigatórios declarados: `fornecedor`, `data_compra`, `total_custo`.

### JSON original completo

```json
{
  "name": "Compra",
  "type": "object",
  "properties": {
    "fornecedor": {
      "type": "string",
      "description": "Fornecedor da compra"
    },
    "fornecedor_id": {
      "type": "string",
      "description": "ID do fornecedor"
    },
    "fornecedor_nome": {
      "type": "string",
      "description": "Nome do fornecedor vinculado"
    },
    "data_compra": {
      "type": "string",
      "format": "date",
      "description": "Data da compra"
    },
    "itens": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "produto_id": {
            "type": "string"
          },
          "produto_nome": {
            "type": "string"
          },
          "quantidade": {
            "type": "number"
          },
          "preco_unitario": {
            "type": "number"
          },
          "subtotal": {
            "type": "number"
          }
        }
      },
      "description": "Itens comprados"
    },
    "total_custo": {
      "type": "number",
      "description": "Custo total da compra"
    },
    "observacoes": {
      "type": "string",
      "description": "Observações"
    }
  },
  "required": [
    "fornecedor",
    "data_compra",
    "total_custo"
  ]
}
```

## 401.5 — Configuracao

| Campo | Tipo JSON | Obrigatório | Formato/Enum | Default | Descrição |
|---|---|---:|---|---|---|
| `owner_email` | `string` | NÃO | email | `—` | Email do dono/proprietário para notificações |
| `owner_whatsapp` | `string` | NÃO | — | `—` | WhatsApp do dono/proprietário |
| `manager_email` | `string` | NÃO | email | `—` | Email da gerente para notificações |
| `manager_whatsapp` | `string` | NÃO | — | `—` | WhatsApp da gerente |
| `notifications` | `object` | NÃO | — | `—` |  |
| `roles_permissions` | `object` | NÃO | — | `—` |  |
| `actions_permissions` | `object` | NÃO | — | `—` |  |
| `theme` | `string` | NÃO | light, dark | `"light"` | Tema do sistema |

### Regras do schema original

- Nenhum campo obrigatório foi declarado no JSON.

### JSON original completo

```json
{
  "name": "Configuracao",
  "type": "object",
  "properties": {
    "owner_email": {
      "type": "string",
      "format": "email",
      "description": "Email do dono/proprietário para notificações"
    },
    "owner_whatsapp": {
      "type": "string",
      "description": "WhatsApp do dono/proprietário"
    },
    "manager_email": {
      "type": "string",
      "format": "email",
      "description": "Email da gerente para notificações"
    },
    "manager_whatsapp": {
      "type": "string",
      "description": "WhatsApp da gerente"
    },
    "notifications": {
      "type": "object",
      "properties": {
        "notify_on_sale": {
          "type": "boolean",
          "default": true
        },
        "notify_on_delivery": {
          "type": "boolean",
          "default": true
        },
        "notify_on_stock_low": {
          "type": "boolean",
          "default": true
        },
        "channel_email": {
          "type": "boolean",
          "default": true
        },
        "channel_whatsapp": {
          "type": "boolean",
          "default": false
        }
      }
    },
    "roles_permissions": {
      "type": "object",
      "properties": {
        "Dashboard": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "Clientes": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "vendedor"
          ]
        },
        "Funcionarios": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "Estoque": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "vendedor",
            "estoquista"
          ]
        },
        "Vendas": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "vendedor"
          ]
        },
        "Entregas": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "entregador"
          ]
        },
        "Relatorios": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "Configuracoes": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "Integracoes": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "Compras": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "Fornecedores": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "OrdensProducao": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "estoquista"
          ]
        }
      }
    },
    "actions_permissions": {
      "type": "object",
      "properties": {
        "clientes_criar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "vendedor"
          ]
        },
        "clientes_editar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "clientes_excluir": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "produtos_criar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "estoquista"
          ]
        },
        "produtos_editar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "estoquista"
          ]
        },
        "produtos_excluir": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "vendas_criar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "vendedor"
          ]
        },
        "vendas_cancelar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "funcionarios_criar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "funcionarios_editar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente"
          ]
        },
        "funcionarios_excluir": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono"
          ]
        },
        "entregas_criar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "vendedor"
          ]
        },
        "entregas_editar": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": [
            "dono",
            "gerente",
            "entregador"
          ]
        }
      }
    },
    "theme": {
      "type": "string",
      "enum": [
        "light",
        "dark"
      ],
      "default": "light",
      "description": "Tema do sistema"
    }
  },
  "required": []
}
```

## 401.6 — Fornecedor

| Campo | Tipo JSON | Obrigatório | Formato/Enum | Default | Descrição |
|---|---|---:|---|---|---|
| `nome_empresa` | `string` | SIM | — | `—` | Nome da empresa ou fornecedor |
| `cnpj` | `string` | NÃO | — | `—` | CNPJ do fornecedor |
| `telefone` | `string` | SIM | — | `—` | Telefone do fornecedor |
| `email` | `string` | NÃO | email | `—` | Email do fornecedor |
| `produto_fornecido` | `string` | NÃO | — | `—` | Produto principal fornecido |
| `observacoes` | `string` | NÃO | — | `—` | Observações |

### Regras do schema original

- Obrigatórios declarados: `nome_empresa`, `telefone`.

### JSON original completo

```json
{
  "name": "Fornecedor",
  "type": "object",
  "properties": {
    "nome_empresa": {
      "type": "string",
      "description": "Nome da empresa ou fornecedor"
    },
    "cnpj": {
      "type": "string",
      "description": "CNPJ do fornecedor"
    },
    "telefone": {
      "type": "string",
      "description": "Telefone do fornecedor"
    },
    "email": {
      "type": "string",
      "format": "email",
      "description": "Email do fornecedor"
    },
    "produto_fornecido": {
      "type": "string",
      "description": "Produto principal fornecido"
    },
    "observacoes": {
      "type": "string",
      "description": "Observações"
    }
  },
  "required": [
    "nome_empresa",
    "telefone"
  ]
}
```

## 401.7 — OrdemProducao

| Campo | Tipo JSON | Obrigatório | Formato/Enum | Default | Descrição |
|---|---|---:|---|---|---|
| `numero_ordem` | `string` | NÃO | — | `—` | Número da ordem de produção |
| `produto_acabado_id` | `string` | SIM | — | `—` | ID do produto acabado |
| `produto_acabado_nome` | `string` | NÃO | — | `—` | Nome do produto acabado |
| `quantidade_produzida` | `number` | SIM | — | `—` | Quantidade produzida |
| `cliente_id` | `string` | NÃO | — | `—` | ID do cliente vinculado à encomenda |
| `cliente_nome` | `string` | NÃO | — | `—` | Nome do cliente vinculado à encomenda |
| `tipo_ordem` | `string` | NÃO | estoque, encomenda | `"estoque"` | Define se a ordem é para estoque ou encomenda |
| `fornecedor` | `string` | NÃO | — | `—` | Fornecedor vinculado ao produto |
| `materias_primas` | `array<object>` | NÃO | — | `—` | Lista de matérias-primas consumidas |
| `data_ordem` | `string` | SIM | date | `—` | Data da ordem |
| `status` | `string` | NÃO | aberta, finalizada | `"aberta"` | Status da ordem |
| `observacoes` | `string` | NÃO | — | `—` | Observações |

### Regras do schema original

- Obrigatórios declarados: `produto_acabado_id`, `quantidade_produzida`, `data_ordem`.

### JSON original completo

```json
{
  "name": "OrdemProducao",
  "type": "object",
  "properties": {
    "numero_ordem": {
      "type": "string",
      "description": "Número da ordem de produção"
    },
    "produto_acabado_id": {
      "type": "string",
      "description": "ID do produto acabado"
    },
    "produto_acabado_nome": {
      "type": "string",
      "description": "Nome do produto acabado"
    },
    "quantidade_produzida": {
      "type": "number",
      "description": "Quantidade produzida"
    },
    "cliente_id": {
      "type": "string",
      "description": "ID do cliente vinculado à encomenda"
    },
    "cliente_nome": {
      "type": "string",
      "description": "Nome do cliente vinculado à encomenda"
    },
    "tipo_ordem": {
      "type": "string",
      "enum": [
        "estoque",
        "encomenda"
      ],
      "default": "estoque",
      "description": "Define se a ordem é para estoque ou encomenda"
    },
    "fornecedor": {
      "type": "string",
      "description": "Fornecedor vinculado ao produto"
    },
    "materias_primas": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "produto_id": {
            "type": "string"
          },
          "produto_nome": {
            "type": "string"
          },
          "quantidade_usada": {
            "type": "number"
          }
        }
      },
      "description": "Lista de matérias-primas consumidas"
    },
    "data_ordem": {
      "type": "string",
      "format": "date",
      "description": "Data da ordem"
    },
    "status": {
      "type": "string",
      "enum": [
        "aberta",
        "finalizada"
      ],
      "default": "aberta",
      "description": "Status da ordem"
    },
    "observacoes": {
      "type": "string",
      "description": "Observações"
    }
  },
  "required": [
    "produto_acabado_id",
    "quantidade_produzida",
    "data_ordem"
  ]
}
```

## 401.8 — Produto

| Campo | Tipo JSON | Obrigatório | Formato/Enum | Default | Descrição |
|---|---|---:|---|---|---|
| `nome` | `string` | SIM | — | `—` | Nome do produto |
| `tipo` | `string` | SIM | tapete, malha | `—` | Tipo do produto |
| `tipo_estoque` | `string` | NÃO | materia_prima, produto_acabado | `"produto_acabado"` | Define se o item é matéria-prima ou produto acabado |
| `categoria` | `string` | NÃO | — | `—` | Categoria do produto (ex: girassol, listrado, etc) |
| `cor` | `string` | NÃO | — | `—` | Cor do produto |
| `tamanho` | `string` | NÃO | — | `—` | Tamanho do tapete ou largura da malha |
| `unidade_medida` | `string` | SIM | unidade, metro, kg | `—` | Unidade de medida |
| `estoque_atual` | `number` | NÃO | — | `0` | Quantidade atual em estoque |
| `estoque_minimo` | `number` | NÃO | — | `1` | Quantidade mínima em estoque |
| `preco_custo` | `number` | NÃO | — | `—` | Preço de custo |
| `preco_venda` | `number` | SIM | — | `—` | Preço de venda |
| `fornecedor` | `string` | NÃO | — | `—` | Nome do fornecedor |
| `codigo_barras` | `string` | NÃO | — | `—` | Código de barras do produto |
| `observacoes` | `string` | NÃO | — | `—` | Observações sobre o produto |
| `ativo` | `boolean` | NÃO | — | `true` | Produto ativo ou inativo |

### Regras do schema original

- Obrigatórios declarados: `nome`, `tipo`, `unidade_medida`, `preco_venda`.

### JSON original completo

```json
{
  "name": "Produto",
  "type": "object",
  "properties": {
    "nome": {
      "type": "string",
      "description": "Nome do produto"
    },
    "tipo": {
      "type": "string",
      "enum": [
        "tapete",
        "malha"
      ],
      "description": "Tipo do produto"
    },
    "tipo_estoque": {
      "type": "string",
      "enum": [
        "materia_prima",
        "produto_acabado"
      ],
      "default": "produto_acabado",
      "description": "Define se o item é matéria-prima ou produto acabado"
    },
    "categoria": {
      "type": "string",
      "description": "Categoria do produto (ex: girassol, listrado, etc)"
    },
    "cor": {
      "type": "string",
      "description": "Cor do produto"
    },
    "tamanho": {
      "type": "string",
      "description": "Tamanho do tapete ou largura da malha"
    },
    "unidade_medida": {
      "type": "string",
      "enum": [
        "unidade",
        "metro",
        "kg"
      ],
      "description": "Unidade de medida"
    },
    "estoque_atual": {
      "type": "number",
      "default": 0,
      "description": "Quantidade atual em estoque"
    },
    "estoque_minimo": {
      "type": "number",
      "default": 1,
      "description": "Quantidade mínima em estoque"
    },
    "preco_custo": {
      "type": "number",
      "description": "Preço de custo"
    },
    "preco_venda": {
      "type": "number",
      "description": "Preço de venda"
    },
    "fornecedor": {
      "type": "string",
      "description": "Nome do fornecedor"
    },
    "codigo_barras": {
      "type": "string",
      "description": "Código de barras do produto"
    },
    "observacoes": {
      "type": "string",
      "description": "Observações sobre o produto"
    },
    "ativo": {
      "type": "boolean",
      "default": true,
      "description": "Produto ativo ou inativo"
    }
  },
  "required": [
    "nome",
    "tipo",
    "unidade_medida",
    "preco_venda"
  ]
}
```

## 401.9 — Observações derivadas dos schemas

### Cliente

O JSON declara como obrigatórios somente `nome` e `telefone`, possui `total_compras` com default `0` e `status` com `ativo/inativo`. A nova aplicação deve preservar essas informações, mas separar **status cadastral** da **classificação comportamental** calculada pela última compra.

### Compra

O JSON permite `fornecedor`, `fornecedor_id` e `fornecedor_nome`, além de um array `itens`. Na nova aplicação, não manter três fontes paralelas de verdade. Utilizar `id_fornecedor` quando houver fornecedor cadastrado e `fornecedor_nome_manual` somente para fornecedor não cadastrado. `itens` deve virar tabela `compra_item`.

### Configuracao

O JSON concentra notificações, permissões de páginas, permissões de ações e tema em um único objeto. Na nova aplicação, essas estruturas devem ser normalizadas em tabelas relacionadas para permitir autorização real no backend.

### Fornecedor

O campo `produto_fornecido` representa somente uma informação descritiva no JSON. Na nova aplicação, relacionamentos reais entre fornecedor e produto devem usar `produto_fornecedor`.

### OrdemProducao

O JSON possui `materias_primas` como array aninhado e apenas `aberta/finalizada`. A nova aplicação normaliza para `ordem_producao_item` e acrescenta mecanismos de cancelamento, auditoria, lock e movimentação de estoque.

### Produto

O JSON declara `tipo_estoque` como `materia_prima/produto_acabado` e um único `estoque_atual`. Essa estrutura não é suficiente para o requisito de uma mesma malha poder ser vendida e também utilizada na produção. Na nova arquitetura, utilizar os campos e dois saldos definidos na especificação: `pode_ser_vendido`, `pode_ser_materia_prima`, `estoque_comercial` e `estoque_producao`.

## 401.10 — Regra para geração das migrations a partir dos JSONs

A IA **não deve simplesmente converter cada propriedade JSON em uma coluna com o mesmo nome**. Deve seguir esta ordem:

```text
JSON original
    ↓
identificação de entidades e agregados
    ↓
normalização relacional
    ↓
FKs
    ↓
constraints
    ↓
índices
    ↓
auditoria
    ↓
regras de estoque/financeiro
    ↓
Flyway
```

Exemplo:

```text
Compra.itens[]
    ↓
compra
compra_item
```

```text
OrdemProducao.materias_primas[]
    ↓
ordem_producao
ordem_producao_item
```

```text
Configuracao.actions_permissions
    ↓
acao_permissao
cargo_acao_permissao
```

## 401.11 — Regra sobre os JSONs que não existem

Para `Venda`, `Funcionario`, `Entrega` e `Despesa`, não inventar um JSON legado. A estrutura deverá ser obtida de:

```text
código-fonte existente
+
documentação
+
regras deste prompt
```

Quando houver divergência entre código e documentação, aplicar a hierarquia de autoridade definida na seção 3.

## 401.12 — Arquivos-fonte do anexo

Os JSONs incorporados neste documento vieram dos arquivos:

```text
Entidades/Cliente.json
Entidades/Compra.json
Entidades/Configuracao.json
Entidades/Fornecedor.json
Entidades/OrdemProducao.json
Entidades/Produto.json
```

Nenhuma alteração foi aplicada aos JSONs originais; este anexo os reproduz como referência documental.

---

# 402. INSTRUÇÃO FINAL SOBRE OS JSONs

Ao gerar o PostgreSQL, o Gemini deverá usar os JSONs deste anexo como **fonte de campos e significado do legado**, mas deverá obedecer às correções e ao modelo relacional deste prompt.

A regra é:

> **Preservar a intenção do campo; não preservar defeitos estruturais do Base44.**

Consequentemente:

- `Compra.itens[]` deve ser normalizado;
- `OrdemProducao.materias_primas[]` deve ser normalizado;
- permissões JSON devem virar modelo relacional;
- fornecedor textual deve virar relacionamento;
- estoque único deve ser substituído pelos dois saldos definidos;
- cancelamentos devem ser transacionais;
- dados históricos devem permanecer rastreáveis.

# 403. FINAL DO ANEXO DE ENTIDADES
