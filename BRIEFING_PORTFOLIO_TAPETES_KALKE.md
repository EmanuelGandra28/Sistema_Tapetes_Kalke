# Briefing — Site de Portfólio Tapetes Kalke

Documento para iniciar, em outro chat, a criação do site de portfólio (catálogo online com link de WhatsApp). Reúne tudo o que existe hoje no Sistema Tapetes Kalke e o que o site deve aproveitar. Gerado em 21/11/2026.

> **O que NÃO está pronto:** o site, o endpoint público de catálogo e o campo "exibir no portfólio" nos produtos **ainda não existem**. Este documento é o ponto de partida. Os números de telefone, textos institucionais e fotos reais precisam ser fornecidos pelo dono.

---

## 1. Sobre a empresa

- **Nome:** Tapetes Kalke.
- **Negócio:** loja que **vende tapetes prontos e também fabrica tapetes** a partir de matérias-primas (malha, fios etc.). Aceita encomendas (tapete sob medida está nos planos do sistema, ainda não implementado).
- **Quem usa o sistema interno:** dono(a), gerente, vendedores, estoquista e entregadores. O dono não é programador.
- **A preencher pelo dono (não estão no sistema):** história da loja ("quem somos"), endereço da loja física, horário de atendimento, cidade/região atendida, redes sociais (Instagram etc.), **número de WhatsApp da loja** (o sistema ainda não tem esse campo; será criado em Configurações), formas de pagamento aceitas para divulgar, política de entrega/frete.
- Formas de pagamento que o sistema já registra: PIX, dinheiro, cartão de crédito, cartão de débito, cheque. Entregas têm status e frete próprio; envio por Correios está em planejamento (`PLANEJAMENTO_RASTREAMENTO_ENVIOS.md`).

## 2. Identidade visual (o site deve combinar com o sistema)

- **Logomarca:** losango estilo kilim (padrão geométrico de tapete) com monograma **TK** em serifa, mais o nome "Tapetes **Kalke**" (Georgia/serifa; "Tapetes" fino e "Kalke" em negrito). O SVG está em `frontend/src/components/Logo.jsx` (viewBox 120×120: losango externo, losango interno com contorno dourado, 4 losangos pequenos nas pontas, texto TK). Ícones do app: `frontend/public/favicon.svg`, `pwa-192.png`, `pwa-512.png`, `pwa-maskable-512.png`, `apple-touch-icon.png`.
- **Paleta padrão "Verde Kalke"** (a identidade principal): verde profundo `#1c5c46` (primário), `#163b30` (verde escuro), `#e2ece7` (verde suave), **dourado `#c8a24a`** (acento), fundo `#f4f7f4`, texto `#18332b`, texto secundário `#647b72`, bordas `#dce7e0`, hover `#154737`.
- **Paleta alternativa "Areia e terracota"** (tons quentes de tapete, boa para um site de catálogo): terracota `#a5512f`, marrom escuro `#4a2c1d`, areia `#f7f1e8`, superfície `#fffdf9`, texto `#3b2a1e`, acento dourado `#b8892e`.
- Outros temas existentes: Azul-marinho (`#1f4e8c`, `#132743`), Escuro, Alto contraste. Todos respeitam contraste mínimo de acessibilidade — o site deve fazer o mesmo.
- **Tipografia:** corpo em Inter / sistema; títulos e marca em Georgia (serifa). Os arquivos de estilo estão em `frontend/src/styles.css` (variáveis CSS `--primary`, `--accent`, `--bg` etc.).
- **Tom de voz:** simples, acolhedor, em português do Brasil, sem jargão técnico. Botões dizem a ação ("Quero este", "Pedir orçamento").

## 3. Dados de produto disponíveis hoje

Cada produto no sistema (tabela `produto`) tem:

| Campo | Observação para o portfólio |
|---|---|
| `nome` | Nome do tapete (até 180 caracteres) |
| `tipo` | `TAPETE` ou `MALHA` (só mostrar TAPETE; malha é matéria-prima) |
| `categoria`, `cor`, `tamanho` | Texto livre; úteis para filtros |
| `unidadeMedida` | unidade, metro ou kg |
| `precoVenda` | Preço de venda. **Nunca expor `precoCusto`** |
| `podeSerVendido`, `ativo` | Só publicar produtos ativos e vendáveis |
| `estoqueComercial` | Saldo pronto para venda. **Não expor o número exato**; no máximo "Disponível / Sob encomenda" |
| `observacoes` | Uso interno; não publicar sem revisão |
| `codigoBarras`, `fornecedorPrincipalId`, estoques mínimos | Uso interno; não publicar |

**Ainda não existe e será criado no sistema** (ver `PROMPT_Melhorias_Fase2.md`):
- **Fotos do produto** (várias, uma principal) com upload no cadastro do produto. As fotos ficarão acessíveis por URL pública estável.
- Campo **"exibir no portfólio"**, **descrição curta** para o site e **ordem de destaque**, controlados pelo dono: só aparece no site o que ele marcar.
- Campo **WhatsApp da loja** em Configurações.

## 4. Arquitetura recomendada para o site

**Restrição principal: tudo gratuito.** O sistema roda assim:

| Parte | Onde |
|---|---|
| Telas do sistema | GitHub Pages: `https://emanuelgandra28.github.io/Sistema_Tapetes_Kalke/` |
| API (Java 21 + Spring Boot) | Render, plano grátis. **Dorme após 15 min sem uso; o primeiro acesso leva ~1 minuto** |
| Banco | Supabase (PostgreSQL), plano grátis |
| Código | GitHub (repositório **público**: nunca colocar chaves, senhas ou telefones reais no código) |

**Decisão central — o cold start.** Um visitante de portfólio não pode esperar 1 minuto. Duas saídas gratuitas:

1. **Recomendada: catálogo estático.** O site (HTML/JS no GitHub Pages, repositório próprio) lê um arquivo `catalogo.json` + imagens publicados estaticamente. Quando o dono alterar produtos marcados para o portfólio, uma rotina (GitHub Actions) regenera o JSON consultando a API/banco. O visitante nunca depende do Render acordado.
2. **Alternativa: consultar a API ao vivo** (`GET /api/v1/publico/catalogo`), com tela de carregamento amigável e cache no navegador. Mais simples, mas o primeiro acesso do dia sofre com o cold start.

**Segurança do endpoint público (se for usado):** liberar só essa rota no `SecurityConfig`, devolver apenas campos seguros (nome, descrição, categoria, cor, tamanho, preço de venda, fotos), com limite de requisições por IP (modelo: `LoginAttemptService`). CORS hoje só aceita a origem do sistema (`FRONTEND_URL`); o domínio do portfólio precisará ser adicionado.

**Tecnologia sugerida:** a mesma do sistema para reaproveitar o conhecimento — React 19 + Vite (JavaScript), React Router, `vite-plugin-pwa` já usado; ou um site estático mais leve (Astro/HTML puro) se a prioridade for velocidade e SEO. Hospedagem: GitHub Pages (grátis). Domínio próprio é opcional (~R$ 40/ano para `.com.br`).

## 5. Funcionalidades pedidas

- Vitrine em grade, bonita e leve, pensada para **celular** (a maioria dos visitantes virá de link no WhatsApp/Instagram).
- Filtros por categoria, cor e tamanho.
- Página de detalhe do tapete com várias fotos, tamanho, cor, descrição e preço (ou "sob consulta" — decisão do dono).
- **Botão "Quero este" / "Pedir orçamento"** que abre `https://wa.me/55<DDD><número>?text=<mensagem>` com texto pronto citando o tapete. Exemplo: "Olá! Tenho interesse no tapete {nome} ({tamanho}, {cor})." Formato do número: só dígitos, com 55 do Brasil (já existe utilitário em `frontend/src/utils/whatsapp.js`: `telefoneWhatsapp` e `linkWhatsapp`).
- Páginas institucionais: quem somos, como comprar, contato, localização com mapa, horário.
- **SEO e compartilhamento:** título, descrição, Open Graph com imagem (o link fica bonito quando colado no WhatsApp), `sitemap.xml`.
- Acessibilidade: contraste mínimo, texto alternativo nas fotos, navegação por teclado.
- **LGPD:** se houver formulário de contato, aviso de uso dos dados; sem cookies de rastreamento desnecessários.
- Opcional futuro: instalar como PWA, galeria de "tapetes sob medida / projetos feitos", depoimentos de clientes (com autorização).

## 6. Fotos — orientação para o dono

- Fotos na vertical e na horizontal, fundo neutro, boa luz natural, tapete inteiro + um detalhe da trama.
- O sistema reduzirá as imagens automaticamente (largura máx. ~1280 px, WEBP/JPEG ~80%) para economizar espaço gratuito e carregar rápido no celular.
- Nome e descrição de cada tapete devem ser pensados para quem vê o site (diferente do nome interno do estoque, se necessário).

## 7. Decisões em aberto (o dono precisa responder)

1. Nome/endereço do site: subpasta do GitHub Pages, domínio próprio ou outro?
2. Mostrar **preço** no site ou "sob consulta"?
3. Qual **número de WhatsApp** da loja e qual mensagem padrão do botão?
4. Textos institucionais, endereço, horário e redes sociais.
5. Quais produtos entram no portfólio e quem cuida de fotos/descrições (dono ou gerente)?
6. Catálogo estático (recomendado) ou consulta à API ao vivo?
7. O portfólio deve ter formulário de orçamento além do WhatsApp?

## 8. Arquivos de referência no repositório

- `SISTEMA_TAPETES_KALKE_VISAO_GERAL.md` — o que o sistema faz hoje.
- `PROMPT_Melhorias_Fase2.md` — melhorias em andamento (fotos dos produtos, modelos de mensagens, relatórios etc.).
- `frontend/src/components/Logo.jsx`, `frontend/src/styles.css`, `frontend/src/utils/tema.js` — marca e cores.
- `frontend/src/utils/whatsapp.js` — telefone e link de WhatsApp.
- `backend/src/main/java/br/com/tapeteskalke/dto/ProdutoDtos.java` — campos do produto.
- `backend/src/main/java/br/com/tapeteskalke/config/SecurityConfig.java` — regras de acesso e CORS.
- `docs/guia-do-sistema.html` — guia para o dono, útil para o tom de voz.
