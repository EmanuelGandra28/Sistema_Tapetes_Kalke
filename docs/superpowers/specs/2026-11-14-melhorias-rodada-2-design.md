# Melhorias — rodada 2 (design)

Data: 14/11/2026 · Sistema Tapetes Kalke (React/Vite no GitHub Pages, Spring Boot 3.4/Java 21 no Render, PostgreSQL no Supabase com Flyway).

## Intenção

A loja Kalke vende quase sempre **tapetes prontos, no balcão**. Pedidos sob medida são raros. Esta rodada dá ao dono e à gerência visão sobre o que já acontece no sistema:
- histórico por cliente;
- movimentações de estoque;
- o que comprar de matéria-prima;
- quais produtos e vendedores rendem mais.

Também facilita o dia a dia: Excel para levar os dados para fora, QR Code PIX no caixa, o sistema instalado no celular e uma tela de permissões que qualquer pessoa entenda.

## Escopo

| Item | Situação |
|---|---|
| 5. Histórico do cliente | **Nesta rodada** |
| 6. Sugestão de compra de matéria-prima | **Nesta rodada** |
| 7. Movimentações de estoque e inventário | **Nesta rodada** |
| 8. Relatórios: curva ABC, margem por produto, vendas por vendedor | **Nesta rodada** |
| 9. Exportar para Excel | **Nesta rodada** |
| 11. Instalar no celular (PWA) | **Nesta rodada** |
| 12. QR Code PIX | **Nesta rodada** |
| 13. Tela de permissões legível | **Nesta rodada** |
| 1. Preço por m² / sob medida | Em espera: falta entender o processo com a loja |
| 2. Orçamentos | Em espera |
| 3. Vendas a prazo | Fora |
| 4. Comprovante / nota e detalhe da venda | Em espera |
| 10. Reativação de clientes e WhatsApp | Em espera |

**Fora desta rodada (não-objetivos):**
- confirmação automática de pagamento PIX;
- nota fiscal;
- cálculo de comissão com percentual (o relatório mostra os totais por vendedor; a comissão fica por conta do dono);
- modo offline para registrar vendas sem internet.

**Anti-objetivos (seria fracasso mesmo funcionando):**
- quebrar ou deixar mais lento o fluxo de venda que já funciona;
- o app instalado no celular mostrar dados velhos (estoque ou preço) por cache;
- uma exportação para Excel entregar dados que o cargo não pode ver na tela (por exemplo, salário);
- números de relatório que não batem com o relatório financeiro existente.

## Restrições

- **Migrações:** nunca alterar V1 a V12. As novas começam em **V13**, valem sozinhas no deploy do Render e não podem mexer em `flyway_schema_history`.
- **Permissões:** checadas no servidor (`PermissionService.page/action`). Reaproveitar as existentes:
  - `produtos`: telas de estoque;
  - `estoque_ajustar`: inventário;
  - `relatorios_visualizar`: relatórios e exportações financeiras;
  - `clientes`: histórico.
  - Não criar permissão nova sem necessidade.
- **Cliente HTTP:** o interceptor de `frontend/src/services/api.js` devolve `response.data` (o envelope `ApiResponse`). Download de arquivo precisa de `responseType: 'blob'` e de tratamento à parte, porque o corpo não é JSON.
- **Ambiente local:** Node só existe no runtime do IntelliJ. O Maven precisa de settings apontando para o Central, sem o proxy corporativo. A CI (GitHub Actions) roda `mvn test`, `npm run lint` e `npm run build`.
- **Padrão de código:**
  - Frontend: React Query, `Modal`, `SearchSelect`, `Loading/Failure/Empty`, `useDebounce`, temas por variáveis CSS (V11).
  - Backend: serviços com `@Transactional` e DTOs em records.

## Abordagens consideradas (decididas)

- **Excel (9):**
  - **Escolhida: gerar `.xlsx` no servidor com Apache POI (`poi-ooxml`).** O servidor já aplica as permissões e exporta o conjunto completo.
  - Descartada: gerar no navegador (só exporta o que está na tela, e cada página monta o seu arquivo).
  - Descartada: CSV (acentos e números quebram no Excel em português).
- **PIX (12):**
  - **Escolhida: gerar no navegador o BR Code estático (padrão EMV do Banco Central, com CRC16) e desenhar o QR com a biblioteca `qrcode`.** Sem custo e sem banco.
  - Descartada por ora: integração com banco/PSP. Confirmaria o pagamento, mas exige conta de API e tarifa.
- **PWA (11):**
  - **Escolhida: `vite-plugin-pwa`**, com `registerType: 'autoUpdate'`.
  - Descartada: manifesto e service worker à mão (mais risco de deixar o celular preso numa versão antiga).

## Design por item

### 5. Histórico do cliente
- **Servidor:** `GET /clientes/{id}/historico-compras`, que hoje devolve um texto fixo, passa a devolver:
  - os dados do cliente;
  - as vendas, mais recentes primeiro: número, data, status, forma de pagamento, desconto, total e itens (nome, quantidade, preço unitário, subtotal), usando os snapshots de `venda_item`;
  - totais: soma das vendas **finalizadas**, quantidade de compras, ticket médio e data da última compra.
  - Permissão: página `clientes`.
- **Tela:** botão "Histórico" no card do cliente abre um modal com os totais no topo e a lista de vendas (expansível para ver os itens). Vendas canceladas aparecem riscadas ou com selo e não entram nos totais.

### 7a. Movimentações de estoque
- **Servidor:** `GET /estoque/movimentos` ganha filtros opcionais e paginação:
  - `produtoId`, `saldo` (COMERCIAL/PRODUCAO), `tipo`, `inicio`, `fim`, `page`, `size` (padrão 50, máximo 200);
  - cada linha: data/hora, produto, saldo, tipo, origem (venda nº, compra, ordem nº, ajuste, inventário), quantidade (+/−), antes → depois, usuário e observação;
  - permissão: página `produtos`;
  - manter compatível quem já usa o endpoint hoje (o dashboard pode usar os "recentes").
- **Tela:** a página Estoque ganha abas: **Produtos | Movimentações | Inventário | Sugestão de compra**. A aba Movimentações tem filtros (produto via `SearchSelect`, saldo, período) e uma tabela com entradas em verde e saídas em vermelho.

### 7b. Inventário (contagem)
- **Servidor:** `POST /estoque/inventario` com `{ saldo, observacao, itens: [{ produtoId, contado }] }`.
  - Para cada item em que `contado` ≠ saldo atual, lança um movimento de ajuste com:
    - tipo `INVENTARIO`;
    - origem `INVENTARIO`;
    - `id_origem` = um UUID da contagem.
  - Tudo numa transação; os itens sem diferença são ignorados.
  - Devolve o resumo: quantos itens foram contados, quantos tinham diferença e as diferenças.
  - Permissão: `estoque_ajustar`.
  - Reaproveitar a mesma rotina do ajuste manual (`EstoqueService`) para manter o histórico consistente.
- **Tela:** aba Inventário.
  1. Escolhe o saldo (Comercial ou Produção).
  2. A tabela lista os produtos ativos com "Sistema" e um campo "Contado" (vazio = não contado).
  3. A coluna "Diferença" é calculada ao vivo.
  4. O botão "Aplicar contagem" mostra a confirmação com o número de diferenças.
  5. Ao final, mostra o resumo.

### 6. Sugestão de compra de matéria-prima
- **Servidor:** `GET /estoque/sugestao-compra`. Para cada produto ativo com `podeSerMateriaPrima`:
  - **comprometido** = soma de `quantidade_usada` dos itens de **ordens de produção ABERTAS** (manuais e automáticas). A matéria-prima só sai do estoque quando a ordem é finalizada;
  - **necessário** = comprometido + estoque mínimo de produção;
  - **falta** = necessário − estoque de produção. A matéria entra na lista só se falta > 0;
  - cada linha traz: matéria, unidade, saldo, comprometido, mínimo, falta, fornecedor principal, preço de custo e custo estimado (falta × custo);
  - permissão: página `produtos`.
- **Tela:** aba Sugestão de compra, com a tabela, o total estimado e o botão Exportar (Excel).
  - O botão "Gerar compra" pré-monta a tela de Nova compra com esses itens, destino Produção e o fornecedor principal quando todos tiverem o mesmo.
  - Se ficar complexo, é opcional para o construtor; o mínimo é a lista com exportação.

### 8. Relatórios novos
Todos recebem `inicio`/`fim`, como o financeiro (mesma validação de período invertido → 400), consideram só vendas **FINALIZADAS** e exigem `relatorios_visualizar`.
- **Curva ABC** (`GET /relatorios/curva-abc`):
  - por produto: quantidade vendida, receita, % da receita, % acumulado e classe;
  - classes: A até 80% acumulado, B até 95%, C o resto, em ordem de receita decrescente.
- **Margem por produto** (`GET /relatorios/margem-produtos`):
  - por produto: quantidade, receita, custo (`custo_unitario` gravado no item; quando nulo, usar o custo atual do produto e marcar a linha como "custo estimado"), margem em R$ e margem em %;
  - a soma da receita e do custo tem que bater com "Receita" e "Custo da mercadoria vendida" do relatório financeiro no mesmo período.
- **Vendas por vendedor** (`GET /relatorios/vendas-por-vendedor`):
  - por vendedor: número de vendas, total vendido, ticket médio e desconto concedido;
  - vendas sem vendedor aparecem como "Sem vendedor".
- **Registrar o vendedor:** em `VendaService`, quando `vendedorId` vier vazio, usar o funcionário **ativo** cujo e-mail é igual ao do usuário logado (mesma regra do entregador em `EntregaService`). Se não houver, fica nulo.
  - Não há mudança de tela na venda.
  - Vendas antigas continuam "Sem vendedor".
- **Tela:** a página Relatórios ganha abas: **Financeiro | Curva ABC | Margem | Vendedores**, todas com o mesmo seletor de período e o botão Exportar.

### 9. Exportar para Excel
- **Servidor:** um serviço de exportação com Apache POI (`poi-ooxml`):
  - cabeçalho em negrito, colunas com largura ajustada, valores em R$ como número com formato de moeda e datas como data;
  - cada endpoint devolve `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` com `Content-Disposition: attachment; filename="<nome>-<data>.xlsx"`.
- **Endpoints:** mesma permissão e mesmos filtros da listagem equivalente, gerando só as colunas que o cargo pode ver.

  | Endpoint | Conteúdo |
  |---|---|
  | `GET /vendas/exportar?inicio&fim` | uma linha por venda, com uma 2ª aba de itens |
  | `GET /clientes/exportar` | clientes |
  | `GET /estoque/exportar` | produtos com os dois saldos, mínimos, custo, venda e status |
  | `GET /estoque/movimentos/exportar` | movimentos, com os mesmos filtros da tela |
  | `GET /estoque/sugestao-compra/exportar` | sugestão de compra |
  | `GET /relatorios/{financeiro,curva-abc,margem-produtos,vendas-por-vendedor}/exportar` | relatórios |

- **Tela:** botão "Exportar Excel" nas telas correspondentes. Um helper `baixarArquivo(url, params)` usa o `api` com `responseType: 'blob'` e evita o interceptor de JSON, trata erro (o blob de erro precisa ser lido como JSON para mostrar a mensagem) e dispara o download.

### 12. QR Code PIX
- **Dados da loja:**
  - **Banco:** a migração V13 adiciona `pix_chave` (até 77), `pix_nome_recebedor` (até 25) e `pix_cidade` (até 15) à tabela `configuracao`, que hoje não tem nenhuma linha nem entidade, e insere uma linha única com id fixo `LOJA`.
  - **Servidor:** `GET /configuracoes/loja` (qualquer usuário logado com a página `vendas`) e `PUT /configuracoes/loja` (`configuracoes_editar`).
  - **Tela:** a seção "Dados da loja (PIX)" em Configurações, visível para quem tem `configuracoes_editar`. A chave e os dados são preenchidos pelo dono; não são informados aqui.
- **Geração:** `frontend/src/utils/pix.js` monta o payload EMV:
  - campos 00, 26 (GUI `br.gov.bcb.pix` + chave), 52 `0000`, 53 `986`, 54 valor, 58 `BR`, 59 nome, 60 cidade, 62 (txid `VENDA<numero>`, só alfanumérico, até 25), 63 CRC16-CCITT (polinômio 0x1021, inicial 0xFFFF);
  - nome e cidade sem acento e em maiúsculas;
  - testes unitários com vitest, usando um payload de referência.
- **Tela de Vendas:** quando a forma de pagamento é PIX e a venda é finalizada, abre um modal com:
  - o QR;
  - o valor;
  - o botão "Copiar código" (copia e cola);
  - o aviso "Confira o recebimento no app do banco antes de entregar".
  - No histórico, as vendas PIX finalizadas ganham o botão "QR PIX" para reabrir o modal.
  - Sem dados de PIX configurados, o modal explica e aponta para Configurações (só para quem pode editar).

### 11. Instalar no celular (PWA)
- `vite-plugin-pwa` com `registerType: 'autoUpdate'`:
  - manifesto: nome "Tapetes Kalke", nome curto "Kalke", cores do tema Verde Kalke, `display: standalone`, `start_url` e `scope` = base do Pages (`/Sistema_Tapetes_Kalke/`);
  - ícones 192, 512 e maskable gerados a partir da logomarca (`components/Logo.jsx`), salvos como PNG em `frontend/public/`.
- **Cache:** só os arquivos do próprio site (JS, CSS, ícones, `index.html`). **Nenhuma chamada da API entra em cache**: o padrão de navegação não pode interceptar o domínio do Render.
- Manter a cópia do `404.html` no workflow. O fallback de navegação do service worker aponta para `index.html` dentro do escopo.
- Opcional: o botão "Instalar app" no menu, quando o navegador oferecer (`beforeinstallprompt`).

### 13. Tela de permissões legível
Mudança só no frontend; as chaves enviadas à API continuam as mesmas.
- Um mapa `services/permissoes.js` traduz cada página e ação para um rótulo e um grupo.
  - Exemplos: `clientes_criar` → "Cadastrar clientes" em **Clientes**; `estoque_ajustar` → "Ajustar estoque" em **Estoque**.
  - Grupos: Clientes, Estoque, Vendas, Compras, Produção, Entregas, Financeiro, Equipe, Sistema.
  - Chave desconhecida: mostra a chave crua, para nunca sumir.
- A matriz mostra:
  - os cargos com nome amigável;
  - um cabeçalho por grupo;
  - o texto da linha com a descrição;
  - uma dica (`title`) com a chave técnica.
- Itens travados (configurações do DONO) aparecem com cadeado e explicação.

## Banco de dados (V13)
- `configuracao`: colunas PIX e a linha `LOJA`.
- Nenhuma tabela nova para inventário: usa `movimento_estoque` com tipo e origem `INVENTARIO`.
- Índices de apoio, se necessários:
  - `venda(id_cliente, data_venda)` para o histórico;
  - `movimento_estoque(id_produto, data_movimento)` para os filtros;
  - `ordem_producao_item(id_ordem)`, se ainda não existir.

## Testes
- **Backend (JUnit + Mockito, como os testes existentes):**
  - classificação ABC (limites de 80% e 95%);
  - margem com custo nulo;
  - sugestão de compra (comprometido + mínimo − saldo, ignorando falta ≤ 0);
  - inventário (só lança as diferenças, numa transação);
  - vendedor automático pelo e-mail;
  - validação de período.
- **Frontend (vitest):** o gerador de BR Code e o CRC16, contra um exemplo conhecido.
- **Depois do deploy:** teste completo no site publicado com registros "TESTE", como na rodada anterior, e limpeza ao final.

## Deixado para o construtor
- Layout das abas e detalhes visuais, seguindo os temas.
- Nome das classes e serviços novos.
- Se "Gerar compra" a partir da sugestão entra agora ou fica como melhoria.
- Ordem de implementação. Sugestão: 13 → 5 → 7 → 6 → 8 → 9 → 12 → 11, um commit por item.
