# Site de portfólio Tapetes Kalke — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publicar um site de portfólio estático (loja de tapetes) com pedido por WhatsApp, "Permita-se Criar" e rotas, atualizado automaticamente e por botão a partir do Sistema Tapetes Kalke.

**Architecture:** Repositório novo `Portfolio_Tapetes_Kalke` (Astro + ilhas React) gerado no GitHub Actions. No build, um script acorda a API no Render, lê `GET /api/v1/publico/catalogo`, baixa as fotos de `GET /api/v1/imagens/{id}` (já existe e é público) e gera versões reduzidas. O sistema ganha campos de portfólio no produto, o endpoint público e o botão que dispara o workflow pela API do GitHub.

**Tech Stack:** Astro 5 + `@astrojs/react` + `@astrojs/sitemap`, JavaScript, Vitest, `sharp`; backend Java 21 / Spring Boot / Flyway / JUnit; frontend do sistema React 19 + Vite.

**Spec:** `docs/superpowers/specs/2026-11-21-portfolio-site-design.md`

## Global Constraints

- Tudo gratuito: GitHub Pages + Actions (repositório **público**), Render e Supabase grátis. **Nenhuma chave, senha, token ou telefone fora da configuração pública** no código; token do GitHub só como variável de ambiente no Render.
- Cores Verde Kalke: `#1c5c46` (primário), `#163b30` (escuro), `#e2ece7` (suave), `#c8a24a` (dourado), fundo `#f4f7f4`, texto `#18332b`, secundário `#647b72`, bordas `#dce7e0`. Títulos em Georgia; corpo em Inter/sistema.
- WhatsApp da loja: `5527998042658` (exibido como (27) 99804-2658). Endereço: "Rua Bernardino Monteiro, Domingos Martins – ES", complemento "Em frente à biquinha". Slogan: "Tapetes Fru Fru". Frase do topo: "Detalhes que dão charme à sua casa".
- Texto do site em português do Brasil, simples e acolhedor, sem jargão.
- Nunca expor custo, estoque, código de barras, fornecedor ou observações. Preço só quando `portfolio_modo = VENDA` e `precoVenda > 0`; senão "Sob consulta".
- Se a API não responder no build, o workflow **falha** e o site anterior fica no ar.
- **O repositório do sistema tem alterações não commitadas de outra frente (fotos dos produtos: `SecurityConfig.java`, `PermissionService.java`, `OperacaoDtos.java`, `ProducaoService.java`, `application.yml`, `ProdutoImagem*`, `V16`).** Não reverter nem sobrescrever. Nos commits de `SecurityConfig.java` e `application.yml`, usar `git add -p` e incluir só as linhas desta frente.
- Migrations: a última é `V19__reativacao_whatsapp.sql`; as novas são **V20** e **V21**. Nunca alterar `flyway_schema_history`.
- Ambiente: Node só existe em `~/AppData/Local/JetBrains/IntelliJIdea2026.2/acp-agents/.runtimes/node/24.19.0` (npm: `node_modules/npm/bin/npm-cli.js`). Maven/JDK: receita da memória "Publicação Tapetes Kalke" (JAVA_HOME=`~/.jdks/temurin-21.0.12.1`, settings apontando ao Maven Central). Criar o repositório no GitHub, ativar o Pages e criar segredos/variáveis são ações **externas: pedir confirmação ao dono antes**.
- O servidor do companheiro visual escreve em `.superpowers/`; adicionar `.superpowers/` ao `.gitignore` do sistema.

## Review Focus

1. Tapete sem nenhuma foto → card e página mostram um placeholder da marca, sem imagem quebrada (Task 5, 6).
2. Catálogo vazio ou sem produtos em uma linha → a seção some em vez de aparecer vazia (Task 5).
3. Produto em modo `VENDA` com preço 0 ou nulo → "Sob consulta" e fora do total do pedido (Task 2, 4).
4. Nome/tema com aspas, `&`, `#`, acentos ou emoji → mensagem do WhatsApp corretamente codificada e legível (Task 3).
5. Pedido guardado no navegador com tapete que já saiu do catálogo, ou com muitos itens → item inexistente é ignorado; mensagem limitada a ~1.500 caracteres, com "e mais N itens" (Task 4).

---

# ETAPA 1 — Site (repositório novo)

Pasta local: `C:\Users\emanuel.gandra\Documents\APPs\Portfolio_Tapetes_Kalke` (irmã do sistema). Todos os caminhos da Etapa 1 são relativos a ela.

### Task 1: Esqueleto do site, tema e dados de exemplo

**Files:**
- Create: `package.json`, `astro.config.mjs`, `vitest.config.js`, `.gitignore`, `README.md`
- Create: `site.config.json`, `src/data/catalogo.exemplo.json`, `src/styles/tema.css`, `src/layouts/Base.astro`, `src/pages/index.astro` (provisório)
- Test: `src/lib/config.test.js`

**Interfaces:**
- Produces: `site.config.json` com `{ nome, slogan, fraseTopo, whatsapp, endereco: { linha1, cidade, complemento, lat, lng }, horario, instagram, mensagemPedido, urlSite }` (`lat`/`lng`/`horario`/`instagram` podem ser `null`); `catalogo.exemplo.json` no formato final: `{ geradoEm, produtos: [{ id, nome, linha, descricao, cor, tamanho, preco, selo, ordem, modo, fotos: [{ id, principal, ordem }] }] }` com 8 produtos (4 `VENDA`, 2 `GALERIA`, linhas `BANHEIRO`/`COZINHA`/`SALA`); `Base.astro` recebe `{ titulo, descricao, imagem }`.

- [ ] **Step 1: Criar o projeto** — `git init`, `package.json` (scripts `dev`, `build`, `test`, `preview`), dependências Astro 5, `@astrojs/react`, `react`, `react-dom`, `@astrojs/sitemap`, `sharp`, `vitest`. Rodar `npm install` com o Node do IntelliJ.
- [ ] **Step 2: Test que falha** — `config.test.js`: `site.config.json` tem `whatsapp === '5527998042658'` e só dígitos; `catalogo.exemplo.json` tem produtos com `modo` em `VENDA|GALERIA`; nenhum produto expõe as chaves `precoCusto`, `estoque`, `observacoes`.
- [ ] **Step 3: Implementar** `site.config.json`, `catalogo.exemplo.json` e `tema.css` (variáveis CSS com as cores da seção Global Constraints, fonte Georgia nos títulos, botões, cartões); `Base.astro` com `<title>`, descrição, Open Graph, favicon `favicon.svg` (copiar de `frontend/public/favicon.svg` do sistema).
- [ ] **Step 4: Rodar** `npm test` e `npm run build` → PASS e build sem erro.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: esqueleto do site, tema Verde Kalke e catálogo de exemplo"`

### Task 2: Biblioteca do catálogo

**Files:**
- Create: `src/lib/catalogo.js`
- Test: `src/lib/catalogo.test.js`

**Interfaces:**
- Consumes: formato do `catalogo.exemplo.json` (Task 1).
- Produces: `carregarCatalogo(): Promise<{ vendas, galeria, linhas }>` (lê `src/data/catalogo.json` se existir, senão o exemplo); `formatarPreco(preco: number|null): string` → `"R$ 89,90"` ou `"Sob consulta"`; `temPreco(p): boolean`; `fotoPrincipal(p): {id}|null`; `urlFoto(id, tamanho: 'p'|'g'): string` → `/fotos/{id}-{tamanho}.webp`; `agruparPorLinha(produtos): Record<string, Produto[]>`; `ordenar(produtos): Produto[]` (por `ordem`, depois nome).

- [ ] **Step 1: Testes** — `formatarPreco(89.9) === 'R$ 89,90'`; `formatarPreco(0)`, `formatarPreco(null)` → `'Sob consulta'`; `temPreco` falso para 0/null; `ordenar` respeita `ordem` e depois nome; `agruparPorLinha` não cria chaves vazias; `fotoPrincipal` devolve a marcada `principal`, senão a de menor `ordem`, senão `null`.
- [ ] **Step 2:** Rodar e ver falhar. **Step 3:** Implementar as funções acima. **Step 4:** Rodar e ver passar.
- [ ] **Step 5: Commit** — `feat: biblioteca do catálogo`

### Task 3: Mensagens e links do WhatsApp

**Files:**
- Create: `src/lib/whatsapp.js`
- Test: `src/lib/whatsapp.test.js`

**Interfaces:**
- Produces: `linkWhatsapp(numero: string, mensagem?: string): string` → `https://wa.me/{numero}?text=...`; `montarMensagemPedido(itens: {nome,tamanho,quantidade}[], opcoes?: {restantes?: number}): string`; `montarMensagemCriar(f: {peca,tema,cores,tamanho,quantidade,observacoes}): string`. Ambas sem codificação (o `linkWhatsapp` codifica).

- [ ] **Step 1: Testes** — pedido com `[{nome:'Conjunto Tulipas',tamanho:'70 × 45 cm',quantidade:1},{nome:'Conjunto Cogumelos',tamanho:'60 × 40 cm',quantidade:2}]` → texto começa com `"Olá! Gostaria de encomendar:"`, contém `"• 1× Conjunto Tulipas (70 × 45 cm)"` e `"• 2× Conjunto Cogumelos (60 × 40 cm)"`, termina com `"Pode me ajudar?"`. `linkWhatsapp('5527998042658','Olá & #1 "teste" 😀')` → a URL contém `%26`, `%23`, `%22` e decodifica de volta ao texto original. Mensagem do Permita-se Criar inclui só os campos preenchidos. Mais de ~1.500 caracteres → corta a lista e termina com `"… e mais N item(ns)"`.
- [ ] **Step 2–4:** falhar, implementar (`encodeURIComponent`), passar. **Step 5: Commit** — `feat: mensagens e links do WhatsApp`

### Task 4: Pedido (carrinho)

**Files:**
- Create: `src/lib/carrinho.js`, `src/components/Pedido.jsx`, `src/components/BotaoAdicionar.jsx`
- Test: `src/lib/carrinho.test.js`

**Interfaces:**
- Consumes: `temPreco` (Task 2), `montarMensagemPedido`/`linkWhatsapp` (Task 3).
- Produces: `criarCarrinho(storage = localStorage)` → `{ itens(): {id,quantidade}[], adicionar(id), remover(id), definir(id, q), limpar(), assinar(fn): () => void }` (grava em `localStorage['kalke.pedido']` dentro de `try/catch`; funciona sem storage); `resumir(itens, catalogo): { linhas: {produto,quantidade,subtotal|null}[], total: number, temSemPreco: boolean, restantes }` (ignora ids que não estão no catálogo); ilha `Pedido.jsx` (painel lateral com contador no menu) e `BotaoAdicionar.jsx` (props `{ id }`, mostra "+ Adicionar" / "✓ No pedido").

- [ ] **Step 1: Testes** — adicionar o mesmo id duas vezes → quantidade 2; `definir(id, 0)` remove; `resumir` com um produto inexistente ignora o item; total soma só itens com preço e `temSemPreco` fica `true` quando há item "Sob consulta"; storage que lança erro não quebra (`criarCarrinho({ getItem(){throw 0}, setItem(){throw 0} })`); assinantes são avisados a cada mudança.
- [ ] **Step 2–4:** falhar, implementar, passar. Em `Pedido.jsx`: lista com quantidades (− / +), mensagem de prévia, total ("frete e peças personalizadas combinados no WhatsApp"), botão "Encomendar pelo WhatsApp" (verde `#25a244`, abre `linkWhatsapp` em nova aba); vazio → "Seu pedido está vazio" e botão "Ver catálogo".
- [ ] **Step 5: Commit** — `feat: pedido (carrinho) com envio ao WhatsApp`

### Task 5: Página inicial

**Files:**
- Create: `src/components/Nav.astro`, `Hero.astro`, `Linhas.astro`, `Produtos.jsx` (ilha com filtro por linha), `CardTapete.astro`, `CriarBanner.astro`, `Galeria.astro`, `ComoFunciona.astro`, `Rodape.astro`, `FotoTapete.astro` (placeholder quando não há foto)
- Create: `src/lib/rotas.js`
- Modify: `src/pages/index.astro`
- Test: `src/lib/rotas.test.js`

**Interfaces:**
- Consumes: `carregarCatalogo`, `formatarPreco`, `urlFoto`, `fotoPrincipal` (Task 2); `Pedido`, `BotaoAdicionar` (Task 4); `site.config.json`.
- Produces: `googleMapsUrl(end)`, `wazeUrl(end)`, `appleMapsUrl(end)`: com `lat`/`lng` usam as coordenadas; sem elas, buscam pelo texto "linha1, cidade". Seções seguem o mockup `home-estilo-loja.html` (menu → topo com cartão → "Escolha a sua linha" (Banheiro, Cozinha, Permita-se Criar) → "Nossos tapetes" → faixa Permita-se Criar → "Feitos com carinho" → "Como funciona" (4 passos) → rodapé com "Encontre-nos").

- [ ] **Step 1: Testes** — sem coordenadas: `googleMapsUrl` contém `Rua%20Bernardino%20Monteiro` e `Domingos%20Martins`; com `lat/lng` usa `destination=lat,lng` (Google), `ll=lat,lng&navigate=yes` (Waze) e `daddr=lat,lng` (Apple).
- [ ] **Step 2–4:** falhar, implementar rotas e componentes. Regras: seção sem produtos não é renderizada; produto sem foto usa `FotoTapete` com o losango da marca; grade com `loading="lazy"`, `width`/`height` e `alt` descritivo; menu vira ícone no celular; contraste mínimo AA; botões de rota + "Copiar endereço" (ilha pequena) + "Falar no WhatsApp".
- [ ] **Step 5: Verificar** — `npm run build`; abrir `npm run preview` e conferir a página em largura 375 px e 1280 px (sem rolagem horizontal).
- [ ] **Step 6: Commit** — `feat: página inicial no estilo de loja`

### Task 6: Página de cada tapete, 404 e SEO

**Files:**
- Create: `src/pages/tapete/[id].astro`, `src/pages/404.astro`, `src/components/GaleriaProduto.jsx`
- Modify: `astro.config.mjs` (sitemap, `site: urlSite`, `base`)

**Interfaces:**
- Consumes: `carregarCatalogo`, `urlFoto`, `fotoPrincipal`, `formatarPreco`; `Base.astro` com Open Graph (`imagem` = foto principal grande em URL absoluta).
- Produces: uma página estática por produto de modo `VENDA` (`getStaticPaths`); `sitemap-index.xml`.

- [ ] **Step 1:** Implementar a página: galeria com miniaturas clicáveis e teclado, nome, linha, tamanho, cor, descrição, preço, `BotaoAdicionar`, link "Voltar ao catálogo", "Quero um parecido" apontando para o Permita-se Criar.
- [ ] **Step 2: Verificar** — `npm run build`; conferir em `dist/tapete/<id>/index.html` as tags `og:title`, `og:image`; `dist/sitemap-index.xml` existe; produto sem foto renderiza placeholder.
- [ ] **Step 3: Commit** — `feat: página de cada tapete, 404 e sitemap`

### Task 7: Permita-se Criar

**Files:**
- Create: `src/pages/permita-se-criar.astro`, `src/components/FormCriar.jsx`
- Test: `src/lib/criar.test.js`

**Interfaces:**
- Consumes: `montarMensagemCriar`, `linkWhatsapp` (Task 3).
- Produces: `validarCriar(f): { ok: boolean, erros: Record<string,string> }` em `src/lib/criar.js` (obrigatórios: `peca`, `tema`; `quantidade` inteira ≥ 1, padrão 1).

- [ ] **Step 1: Testes** — sem `tema` → erro "Conte o tema ou desenho"; `quantidade: 0` → erro; formulário completo → `ok: true`.
- [ ] **Step 2–4:** falhar, implementar `criar.js` e `FormCriar.jsx` (peça em botões: Banheiro, Cozinha, Sala, Outro; tema, cores, tamanho, quantidade, observações; aviso "Tem foto de referência? Envie na conversa do WhatsApp"; botão "Pedir orçamento no WhatsApp"). Nenhum dado sai do navegador.
- [ ] **Step 5: Commit** — `feat: Permita-se Criar`

### Task 8: Script que baixa o catálogo e as fotos

**Files:**
- Create: `scripts/baixar-catalogo.mjs`, `scripts/lib/api.mjs`, `scripts/lib/fotos.mjs`
- Modify: `package.json` (`"prebuild": "node scripts/baixar-catalogo.mjs"`)
- Test: `scripts/lib/api.test.js`

**Interfaces:**
- Produces: `buscarComRetentativa(url, { tentativas = 12, espera = 15000, fetchFn = fetch }): Promise<Response>` (acorda o Render; tenta de novo em erro de rede ou status ≥ 500); `normalizarCatalogo(resposta): Catalogo` (descarta produto sem `id`/`nome`, força `modo ∈ VENDA|GALERIA`); `reduzirFoto(buffer, id)` grava `public/fotos/{id}-p.webp` (largura 480) e `{id}-g.webp` (largura 1280) com `sharp`.
- Comportamento: sem `API_URL` (desenvolvimento) → usa o catálogo de exemplo e não baixa nada; com `API_URL` e falha final → sai com código 1 (o workflow falha).

- [ ] **Step 1: Testes** — `buscarComRetentativa` com `fetchFn` que falha 2 vezes e responde 200 na 3ª → resolve e chamou 3 vezes; falha em todas → rejeita; status 404 não repete; `normalizarCatalogo` descarta item sem `nome` e converte modo desconhecido para fora.
- [ ] **Step 2–4:** falhar, implementar, passar. Escrever `src/data/catalogo.json` (ignorado no git) e `public/fotos/*` (ignorado no git).
- [ ] **Step 5: Commit** — `feat: baixar catálogo e fotos da API no build`

### Task 9: Workflow de publicação

**Files:**
- Create: `.github/workflows/publicar.yml`

**Interfaces:**
- Produces: workflow `Publicar site` com `on: schedule` (`0 */6 * * *`), `workflow_dispatch` e `push` em `main`; `permissions: contents: read, pages: write, id-token: write`; `concurrency: pages`; variável de repositório `API_URL` (ex.: `https://tapetes-kalke-api.onrender.com`); passos: checkout, setup-node 22 com cache, `npm ci`, `npm test`, `npm run build` com `API_URL`, `touch dist/.nojekyll`, `configure-pages`, `upload-pages-artifact`, `deploy-pages` (mesmo modelo de `.github/workflows/deploy-pages.yml` do sistema).

- [ ] **Step 1:** Escrever o workflow. **Step 2: Verificação local** — `npm test && npm run build` (sem `API_URL`, usa o exemplo) → sucesso.
- [ ] **Step 3: Ação externa (confirmar com o dono antes):** criar o repositório público `Portfolio_Tapetes_Kalke` no GitHub, enviar o código, ativar Pages (fonte: GitHub Actions), criar a variável `API_URL`. Primeira publicação com o catálogo de exemplo (`API_URL` ainda vazia usa o exemplo).
- [ ] **Step 4:** Abrir o endereço publicado e conferir página inicial, pedido, Permita-se Criar e rotas no celular. **Step 5: Commit** — `ci: publicação no GitHub Pages`

---

# ETAPA 2 — Sistema (repositório `Sistema_Tapetes_Kalke`)

### Task 10: Campos de portfólio no produto

**Files:**
- Create: `backend/src/main/resources/db/migration/V20__produto_portfolio.sql`
- Modify: `backend/src/main/java/br/com/tapeteskalke/entity/Produto.java`, `dto/ProdutoDtos.java`, `service/ProdutoService.java`
- Test: `backend/src/test/java/br/com/tapeteskalke/service/ProdutoPortfolioTest.java`

**Interfaces:**
- Produces: colunas em `produto`: `portfolio_modo VARCHAR(10) NOT NULL DEFAULT 'NENHUM' CHECK IN ('NENHUM','VENDA','GALERIA')`, `portfolio_linha VARCHAR(10) NULL CHECK IN ('BANHEIRO','COZINHA','SALA','OUTRA')`, `portfolio_descricao VARCHAR(500) NULL`, `portfolio_ordem INTEGER NOT NULL DEFAULT 0`, `portfolio_selo VARCHAR(20) NULL`. `ProdutoDtos.Request` e `Response` ganham `portfolioModo, portfolioLinha, portfolioDescricao, portfolioOrdem, portfolioSelo` (validação `@Pattern` nos valores aceitos, `@Size`).

- [ ] **Step 1: Teste que falha** — `ProdutoPortfolioTest`: `create` com `portfolioModo='VENDA'` e `portfolioLinha='COZINHA'` devolve os valores; omitidos → `NENHUM`/`null`/`0`; `portfolioModo='XYZ'` → violação de validação.
- [ ] **Step 2:** Rodar `mvn -q -Dtest=ProdutoPortfolioTest test` (receita do ambiente) → FALHA. **Step 3:** Implementar migration, entidade, DTOs e `copy`/`response` do `ProdutoService`. **Step 4:** Rodar → PASSA, e `mvn -q test` completo sem regressão.
- [ ] **Step 5: Commit** — `feat: campos de portfólio no produto`

### Task 11: Catálogo público

**Files:**
- Create: `service/PortfolioService.java`, `controller/PortfolioController.java`, `security/PublicRateLimiter.java`
- Modify: `config/SecurityConfig.java` (liberar `GET /api/v1/publico/catalogo`), `repository/ProdutoRepository.java` (`List<Produto> portfolio()`)
- Test: `service/PortfolioServiceTest.java`, `security/PublicRateLimiterTest.java`

**Interfaces:**
- Consumes: `ProdutoImagemService.listar(produtoId)` (existente, devolve `ImagemResponse(id, principal, ordem)`).
- Produces: `PortfolioService.catalogo(): CatalogoPublico` com `record CatalogoPublico(OffsetDateTime geradoEm, List<ItemPublico> produtos)` e `record ItemPublico(String id, String nome, String linha, String descricao, String cor, String tamanho, BigDecimal preco, String selo, int ordem, String modo, List<ImagemResponse> fotos)` (`preco` só se `modo=VENDA` e `precoVenda>0`, senão `null`); `PublicRateLimiter.permitir(String ip): void` (lança `BusinessException` 429; 30 requisições por minuto por IP, em memória, mesmo estilo de `LoginAttemptService`).
- Regra: só `ativo`, não excluído, `tipo='TAPETE'` e `portfolio_modo <> 'NENHUM'`; ordenar por `portfolio_ordem`, depois nome.

- [ ] **Step 1: Testes** — produtos `NENHUM`, inativos, excluídos e tipo `MALHA` ficam de fora; `GALERIA` vem sem preço; `VENDA` com preço 0 vem com `preco = null`; o JSON serializado **não contém** `precoCusto`, `estoqueComercial`, `observacoes`, `codigoBarras`, `fornecedor`; rate limiter bloqueia a 31ª chamada do mesmo IP e não afeta outro IP.
- [ ] **Step 2–4:** falhar, implementar, passar. O controller lê o IP de `X-Forwarded-For` (primeiro valor) ou `getRemoteAddr()`. Resposta com `Cache-Control: public, max-age=60`.
- [ ] **Step 5: Commit** — `feat: endpoint público do catálogo de portfólio`

### Task 12: Tela de produto — seção "Portfólio"

**Files:**
- Modify: `frontend/src/pages/Produtos.jsx` (defaults, `toPayload`, formulário), `frontend/src/content/ajuda.js`
- Test: `frontend/src/pages/produtosPortfolio.test.js` (se houver padrão de teste de utilidades; senão testar `toPayload` extraído para `frontend/src/utils/produto.js`)

**Interfaces:**
- Consumes: `ProdutoDtos.Request` (Task 10).
- Produces: campos no formulário: "Aparece no site?" (Não / Vender no site / Só na galeria), "Linha" (Banheiro, Cozinha, Sala, Outra), "Descrição para o site" (500), "Destaque (ordem)" e "Selo" (Novo / Mais pedido / nenhum); aviso "Aparece no site na próxima atualização". `defaults` ganha `portfolioModo: 'NENHUM'`, etc.

- [ ] **Step 1: Teste** — `toPayload` converte `portfolioOrdem` para número e envia `portfolioLinha: null` quando vazio. **Step 2–4:** falhar, implementar, passar; rodar `npm run lint` e `npm test` no frontend. **Step 5: Commit** — `feat: seção Portfólio no cadastro de produtos`

### Task 13: Botão "Atualizar site"

**Files:**
- Create: `db/migration/V21__permissao_portfolio_publicar.sql`, `service/PublicacaoSiteService.java`
- Modify: `controller/PortfolioController.java` (`POST /api/v1/portfolio/publicar`), `application.yml` (`app.portfolio.*`), `frontend/src/services/permissoes.js`, `frontend/src/pages/Produtos.jsx` (botão), `render.yaml` (declarar variáveis sem valor)
- Test: `service/PublicacaoSiteServiceTest.java`

**Interfaces:**
- Produces: `PublicacaoSiteService.publicar(): void` — `POST https://api.github.com/repos/{repo}/actions/workflows/{workflow}/dispatches` com corpo `{"ref":"main"}`, cabeçalhos `Authorization: Bearer {token}`, `Accept: application/vnd.github+json`; sucesso é status 204. Configuração: `app.portfolio.github-token=${PORTFOLIO_GITHUB_TOKEN:}`, `app.portfolio.repo=${PORTFOLIO_REPO:EmanuelGandra28/Portfolio_Tapetes_Kalke}`, `app.portfolio.workflow=${PORTFOLIO_WORKFLOW:publicar.yml}`. Token vazio → `BusinessException("SITE_NAO_CONFIGURADO", "A atualização do site ainda não foi configurada", 400)`. Falha do GitHub → `BusinessException("SITE_FALHOU", "Não foi possível atualizar o site agora. Tente de novo em alguns minutos", 502)`. Permissão `portfolio_publicar` (padrão DONO e GERENTE). Limite: no máximo 1 disparo a cada 2 minutos (resposta 429 com mensagem amigável).
- Cliente HTTP injetável (`RestClient`/`HttpClient` atrás de uma interface) para testar sem rede.

- [ ] **Step 1: Testes** — token vazio → `SITE_NAO_CONFIGURADO`; cliente fake devolve 204 → sem erro e a requisição tem URL, `Authorization` e corpo esperados; cliente devolve 404/500 → `SITE_FALHOU`; segundo disparo em menos de 2 minutos → 429.
- [ ] **Step 2–4:** falhar, implementar, passar. No frontend, botão "Atualizar site" visível só com `canAction('portfolio_publicar')`, confirmação "O site será atualizado em 2 a 5 minutos" e mensagem de sucesso/erro.
- [ ] **Step 5: Commit** — `feat: botão para atualizar o site do portfólio`

---

# ETAPA 3 — Ligar tudo

### Task 14: Conexão real, documentação e conferência final

**Files:**
- Modify: `frontend/src/content/ajuda.js`, `docs/guia-do-sistema.html`, `SISTEMA_TAPETES_KALKE_VISAO_GERAL.md`, `.gitignore`, `docs/superpowers/specs/2026-11-21-portfolio-site-design.md` (V17→V20; foto pública já existe em `/api/v1/imagens/{id}`)

- [ ] **Step 1: Ações externas (confirmar com o dono antes de cada uma):** no Render, definir `PORTFOLIO_GITHUB_TOKEN` (token do GitHub restrito ao repositório do site, com permissão de Actions: escrita) e, se preciso, `FRONTEND_URL`; no repositório do site, definir `API_URL`. O dono cria o token (não digitar nem registrar o valor em arquivos).
- [ ] **Step 2:** Cadastrar no sistema (ou o dono cadastra) 2 produtos reais com foto, `Vender no site`, e rodar o workflow manualmente: confirmar no site publicado que o catálogo real substituiu o exemplo, com fotos e preços.
- [ ] **Step 3:** Clicar em "Atualizar site" no sistema; conferir que o workflow dispara e termina com sucesso. Desligar a API (ou usar `API_URL` inválida em uma execução manual de teste) e confirmar que a execução falha e o site anterior continua no ar.
- [ ] **Step 4:** Atualizar a ajuda e o guia do dono (como marcar um tapete para o site, o que significa cada modo, o botão e o prazo de 2 a 5 minutos); adicionar `.superpowers/` ao `.gitignore`.
- [ ] **Step 5: Commit** — `docs: guia e ajuda do portfólio`

---

## Self-Review

- **Cobertura da spec:** página inicial (T5), carrinho (T4), Permita-se Criar (T7), rotas (T5), preço só nos padrão (T2/T10/T11), página do tapete + OG + sitemap (T6), atualização automática e por botão (T9/T13), segurança do endpoint (T11), site anterior permanece se falhar (T8/T9/T14), campos de portfólio e galeria (T10/T12), documentação (T14). Pendências do dono (pin, horário, fotos, descrições) viram campos opcionais em `site.config.json` e dados do cadastro, sem bloquear.
- **Tipos:** `Catalogo`/`ItemPublico` (T11) coincidem com o JSON de exemplo (T1) e com `normalizarCatalogo` (T8); `temPreco`/`formatarPreco` (T2) usados em T4 e T5; `montarMensagemPedido`/`linkWhatsapp` (T3) usados em T4 e T7.
- **Proporção:** passos trazem assinaturas, nomes de teste e valores da spec; corpos de função ficam para quem implementa.
