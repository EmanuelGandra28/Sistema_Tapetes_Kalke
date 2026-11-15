# Melhorias rodada 2 — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar os itens 13, 5, 7, 6, 8, 9, 12 e 11 do design: permissões legíveis, histórico do cliente, movimentações/inventário, sugestão de compra, relatórios novos, Excel, PIX e PWA.

**Architecture:** Endpoints novos nos controllers existentes (`/clientes`, `/estoque`, `/relatorios`, `/vendas`, `/configuracoes`), com a lógica em serviços (`RelatorioAnaliticoService`, `SugestaoCompraService`, `InventarioService`, `ExcelService`). No frontend, abas em Estoque e Relatórios, utilitários puros (`utils/pix.js`, `services/permissoes.js`, `services/download.js`) e o plugin PWA no Vite.

**Tech Stack:** Spring Boot 3.4 / Java 21 / JPA / Flyway / JUnit 5 + Mockito; React 19 / React Query 5 / Vite 6 / vitest; novas dependências `org.apache.poi:poi-ooxml`, `qrcode` e `vite-plugin-pwa`.

**Spec:** `docs/superpowers/specs/2026-11-14-melhorias-rodada-2-design.md`

## Global Constraints
- Nova migração: **V13** (nunca alterar V1 a V12).
- Permissões no servidor:
  - `page("produtos")`: telas de estoque;
  - `action("estoque_ajustar")`: inventário;
  - `action("relatorios_visualizar")`: relatórios e exportações financeiras;
  - `page("clientes")`: histórico;
  - `page("vendas")`: leitura dos dados de PIX;
  - `action("configuracoes_editar")`: gravação dos dados de PIX.
- Relatórios: só vendas `FINALIZADA`; período invertido → 400 `PERIODO_INVALIDO`.
- ABC: A ≤ 80% acumulado, B ≤ 95%, C o resto.
- PIX:
  - campos `pix_chave` (77), `pix_nome_recebedor` (25), `pix_cidade` (15) e linha `LOJA` em `configuracao`;
  - txid `VENDA<numero>`;
  - CRC16-CCITT com polinômio 0x1021 e valor inicial 0xFFFF.
- PWA: `registerType: 'autoUpdate'`; nenhuma chamada da API em cache; escopo `/Sistema_Tapetes_Kalke/`.
- Excel: MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, nome `<nome>-<aaaa-mm-dd>.xlsx`.
- Um commit por tarefa, mensagem em português, terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Push só no fim, com aprovação.
- Verificação local:
  - backend: `mvn -B -ntp -o -Dmaven.legacyLocalRepo=true -gs <empty-global> -s <central-settings> test` (sem `-o` quando houver dependência nova);
  - frontend: eslint, vitest e vite build com o Node do IntelliJ.

## Review Focus
- Exportação feita por um cargo sem a permissão → 403, igual à listagem. O teste do controller chama `permissions`.
- Margem com `custo_unitario` nulo (vendas antigas) → usa o custo atual e marca `custoEstimado=true`. Coberto pelo teste da Task 5.
- Inventário com quantidade contada **negativa** ou vazia → negativo dá 422, vazio é ignorado. Coberto pelo teste da Task 3.
- PIX com nome ou cidade acentuados ou longos → sem acento, em maiúsculas e cortados no limite. Coberto pelo teste da Task 7.
- Usuário sem funcionário ligado pelo e-mail → venda com vendedor nulo, sem erro. Coberto pelo teste da Task 5.

---

### Task 1: Permissões legíveis (item 13)
**Files:**
- Create: `frontend/src/services/permissoes.js`
- Create: `frontend/src/services/permissoes.test.js`
- Modify: `frontend/src/pages/Configuracoes.jsx` (MatrizPermissoes)

**Interfaces:**
- Produces: `rotuloPermissao(chave) -> {rotulo, grupo}` (chave desconhecida → `{rotulo: chave, grupo: 'Outros'}`).
- Produces: `agruparPermissoes(chaves) -> [{grupo, itens: [chave]}]`, na ordem dos grupos: Clientes, Estoque, Vendas, Compras, Produção, Entregas, Financeiro, Equipe, Sistema, Outros.
- Produces: `CARGO_ROTULO`.

- [ ] Teste: `rotuloPermissao('clientes_criar')` → `{rotulo:'Cadastrar clientes', grupo:'Clientes'}`; `rotuloPermissao('xyz')` → `{rotulo:'xyz', grupo:'Outros'}`; `agruparPermissoes` mantém a ordem dos grupos.
- [ ] Implementar e rodar o vitest (PASS).
- [ ] Matriz: linha de cabeçalho por grupo, rótulo com `title` = chave, cargo com nome amigável, item travado com 🔒 e dica.
- [ ] Lint + build → commit.

### Task 2: Histórico do cliente (item 5)
**Files:**
- Modify: `ClienteController.history`
- Modify: `VendaRepository` (`historyByClient`, com fetch de itens, todas as vendas)
- Modify: `ClienteDtos` (`Historico`, `HistoricoVenda`, `HistoricoItem`)
- Create: `frontend/src/components/HistoricoClienteModal.jsx`
- Modify: `Clientes.jsx` (botão "Histórico")
- Test: `ClienteServiceTest`

**Interfaces:**
- Produces: `ClienteService.historico(id) -> Historico(cliente, totalFinalizado, compras, ticketMedio, ultimaCompra, List<HistoricoVenda>)`.
- Produces: `HistoricoVenda(id, numero, data, status, formaPagamento, desconto, total, List<HistoricoItem>)`.
- Produces: `HistoricoItem(produto, quantidade, precoUnitario, subtotal)`.

- [ ] Teste: 2 vendas finalizadas (100 e 50) e 1 cancelada (30) → totalFinalizado 150, compras 2, ticket 75; a lista tem as 3 vendas, a mais recente primeiro.
- [ ] Implementar e rodar o teste (PASS).
- [ ] Modal: totais no topo e vendas expansíveis; canceladas com selo "Cancelada" e sem entrar nos totais.
- [ ] mvn test + lint → commit.

### Task 3: Movimentações e inventário (item 7)
**Files:**
- Modify: `MovimentoEstoqueRepository` (`search(produtoId, saldo, tipo, de, ate, Pageable)` em JPQL com parâmetros nulos opcionais)
- Modify: `EstoqueController` (`GET /movimentos` com filtros e paginação; `POST /inventario`)
- Create: `service/InventarioService.java`
- Modify: `dto/OperacaoDtos` (`InventarioRequest`, `InventarioItem`, `InventarioResumo`)
- Modify: `Produtos.jsx` (abas)
- Create: `frontend/src/pages/estoque/Movimentacoes.jsx`
- Create: `frontend/src/pages/estoque/Inventario.jsx`
- Test: `InventarioServiceTest`

**Interfaces:**
- Produces: `GET /estoque/movimentos?produtoId&saldo&tipo&inicio&fim&page&size` → `PageResponse` de `{id, data, produto, produtoId, saldo, tipo, origem, idOrigem, quantidade, anterior, posterior, observacao}`.
  - Sem parâmetros, devolve os mais recentes (compatível com o uso atual).
- Produces: `InventarioService.aplicar(InventarioRequest) -> InventarioResumo(contados, comDiferenca, List<{produto, sistema, contado, diferenca}>)`.
- Request: `InventarioRequest(@Pattern COMERCIAL|PRODUCAO saldo, String observacao, @NotEmpty List<InventarioItem(@NotBlank produtoId, @NotNull @DecimalMin("0") contado)>)`.

- [ ] Teste: saldo 5 e contado 3 → `change(p, saldo, -2, "INVENTARIO", "INVENTARIO", <mesmo uuid para todos>, obs)`; contado igual ao saldo → nenhuma chamada; resumo contados=2, comDiferenca=1.
- [ ] Implementar e rodar o teste (PASS). Contado negativo → violação de validação (422).
- [ ] Aba Movimentações: filtros (SearchSelect de produto, saldo, período) e tabela com +/− em cor.
- [ ] Aba Inventário: escolhe o saldo, edita o campo "Contado" (vazio = ignora), vê a diferença ao vivo e aplica com confirmação.
- [ ] mvn test + lint → commit.

### Task 4: Sugestão de compra (item 6)
**Files:**
- Create: `service/SugestaoCompraService.java`
- Modify: `OrdemProducaoRepository` ou query nativa (soma de `quantidade_usada` por matéria em ordens `ABERTA`)
- Modify: `EstoqueController` (`GET /sugestao-compra`)
- Create: `frontend/src/pages/estoque/SugestaoCompra.jsx`
- Test: `SugestaoCompraServiceTest`

**Interfaces:**
- Produces: `SugestaoCompraService.calcular() -> List<SugestaoItem(produtoId, nome, unidade, saldo, comprometido, minimo, falta, fornecedor, custoUnitario, custoEstimado)>`, ordenada por custoEstimado decrescente.

- [ ] Teste: malha com saldo 10, comprometido 8 e mínimo 5 → falta 3 e custo 3 × preço; matéria com saldo 20, comprometido 2 e mínimo 5 → fora da lista.
- [ ] Implementar e rodar o teste (PASS).
- [ ] Aba com a tabela e o total estimado. O botão "Gerar compra" fica fora desta rodada (registrar no relatório final).
- [ ] mvn test + lint → commit.

### Task 5: Relatórios novos + vendedor automático (item 8)
**Files:**
- Create: `service/RelatorioAnaliticoService.java`
- Modify: `VendaRepository` (agregações por produto: `id_produto`, nome do snapshot, quantidade, receita, `sum(quantidade*custo_unitario)`, contagem de itens sem custo; por vendedor)
- Modify: `RelatorioController` (3 endpoints e um helper `periodo(inicio, fim)` que reaproveita a validação)
- Modify: `VendaService` (vendedor pelo e-mail)
- Modify: `FuncionarioRepository` (`findFirstByEmailIgnoreCaseAndStatus`)
- Modify: `Relatorios.jsx` (abas)
- Tests: `RelatorioAnaliticoServiceTest`, `VendaServiceTest`

**Interfaces:**
- Produces: `curvaAbc(de, ate) -> List<AbcItem(produto, quantidade, receita, percentual, acumulado, classe)>`.
- Produces: `margem(de, ate) -> List<MargemItem(produto, quantidade, receita, custo, margem, margemPercentual, custoEstimado)>`.
- Produces: `porVendedor(de, ate) -> List<VendedorItem(vendedor, vendas, total, ticketMedio, descontos)>`.

- [ ] Teste ABC: receitas 70/15/10/5 → classes A, B, B, C (acumulado 70, 85, 95, 100).
- [ ] Teste margem: custo nulo usa o `precoCusto` atual e `custoEstimado=true`.
- [ ] Teste vendedor: `vendedorId` nulo + funcionário ativo com o e-mail do usuário → `setVendedor(funcionario)`; sem funcionário → null e venda salva.
- [ ] Implementar e rodar os testes (PASS).
- [ ] Abas Financeiro | Curva ABC | Margem | Vendedores, com o seletor de período compartilhado.
- [ ] mvn test + lint → commit.

### Task 6: Exportar para Excel (item 9)
**Files:**
- Modify: `backend/pom.xml` (`poi-ooxml` 5.4.x)
- Create: `service/ExcelService.java`
- Modify: controllers de Vendas, Clientes, Estoque e Relatórios (`/exportar`)
- Create: `frontend/src/services/download.js`
- Modify: botões nas telas
- Test: `ExcelServiceTest`

**Interfaces:**
- Produces: `ExcelService.planilha(List<Aba(nome, List<Coluna(titulo, tipo TEXTO|NUMERO|MOEDA|DATA)>, List<List<Object>>)>) -> byte[]`.
- Produces: `ExcelService.resposta(nome, byte[]) -> ResponseEntity<byte[]>` (MIME e nome do arquivo).
- Produces: `baixarArquivo(url, params, nomePadrao) -> Promise<void>`. Erro → lê o blob como JSON e lança `{message}`.

- [ ] Teste: uma aba com uma coluna MOEDA → ao reler com POI, a célula é numérica com formato contendo "R$"; o cabeçalho está em negrito.
- [ ] Implementar e rodar o teste (PASS).
- [ ] Endpoints:
  - `/vendas/exportar?inicio&fim` (abas Vendas e Itens);
  - `/clientes/exportar`;
  - `/estoque/exportar`;
  - `/estoque/movimentos/exportar`;
  - `/estoque/sugestao-compra/exportar`;
  - `/relatorios/{financeiro,curva-abc,margem-produtos,vendas-por-vendedor}/exportar`.
  - Cada um chama a mesma permissão da listagem correspondente.
- [ ] Botões "Exportar Excel" nas telas.
- [ ] mvn test (com rede, por causa da dependência nova) + lint + build → commit.

### Task 7: PIX (item 12)
**Files:**
- Create: `db/migration/V13__dados_loja_pix.sql`
- Create: `entity/ConfiguracaoLoja.java` (tabela `configuracao`, id `LOJA`)
- Create: repositório
- Modify: `ConfiguracaoController` (`GET`/`PUT /loja`)
- Create: `frontend/src/utils/pix.js`
- Create: `frontend/src/utils/pix.test.js`
- Create: `frontend/src/components/PixModal.jsx`
- Modify: `Vendas.jsx`
- Modify: `Configuracoes.jsx` (seção "Dados da loja (PIX)")
- Modify: `frontend/package.json` (`qrcode`)

**Interfaces:**
- Produces: `GET /configuracoes/loja -> {pixChave, pixNomeRecebedor, pixCidade}`.
- Produces: `PUT /configuracoes/loja` (mesma estrutura; validação de tamanho 77/25/15).
- Produces: `crc16(str) -> 'XXXX'` (hexadecimal maiúsculo, 4 dígitos).
- Produces: `payloadPix({chave, nome, cidade, valor, txid}) -> string`.
- Produces: `normalizar(texto, max)`.

- [ ] Testes:
  - `crc16('123456789') === '29B1'`;
  - o payload começa com `000201`, contém `5303986`, `5802BR` e `54` + valor com 2 casas, e termina com `6304` + crc16 do restante;
  - `normalizar('São José dos Campos', 15) === 'SAO JOSE DOS CA'`.
- [ ] Implementar e rodar o vitest (PASS).
- [ ] V13: ALTER das 3 colunas e INSERT da linha `LOJA` com `ON CONFLICT DO NOTHING` (as colunas NOT NULL com default existentes ficam no default).
- [ ] Modal PIX depois de finalizar uma venda PIX e pelo botão "QR PIX" no histórico; sem configuração, mostra a mensagem.
- [ ] mvn test + vitest + lint + build → commit.

### Task 8: PWA (item 11)
**Files:**
- Modify: `frontend/vite.config.js`
- Modify: `frontend/package.json` (`vite-plugin-pwa`)
- Create: `frontend/public/pwa-192.png`, `pwa-512.png`, `pwa-maskable-512.png` (gerados da logomarca)
- Modify: `index.html` (`theme-color`, `apple-touch-icon`)

- [ ] Configurar o plugin:
  - manifesto (nome, nome curto, cores, `standalone`, `start_url`/`scope` = base);
  - `workbox.globPatterns` só para os arquivos do build;
  - `navigateFallback: 'index.html'`;
  - sem `runtimeCaching` para a API.
- [ ] Build → `dist/manifest.webmanifest` e `dist/sw.js` existem; `sw.js` não contém `onrender.com`.
- [ ] Commit.

### Task 9: Verificação final
- [ ] mvn test, vitest, lint e build passam.
- [ ] Push (com aprovação) e acompanhar o deploy.
- [ ] Teste no site publicado de cada item, com registros TESTE, e limpeza.
