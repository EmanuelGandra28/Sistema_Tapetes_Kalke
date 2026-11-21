# Site de portfólio Tapetes Kalke — Design

Data: 21/11/2026. Status: **aguardando aprovação do dono**. Substitui, no que for diferente, o `BRIEFING_PORTFOLIO_TAPETES_KALKE.md`.

## 1. Intenção e porquê

A Tapetes Kalke ("Tapetes Fru Fru", proprietária Tereza Kalke, Domingos Martins – ES) vende tapetes feitos à mão e recebe encomendas pelo WhatsApp. O dono quer um site de portfólio **profissional, com fotos bonitas**, em que quem gostar de um tapete consiga **chamar no WhatsApp com o pedido já montado**.

Quem visita chega, na maior parte, por link no WhatsApp ou Instagram, pelo celular. A primeira impressão precisa ser rápida e bonita.

## 2. Objetivos, não objetivos e anti-objetivos

**Objetivos**
- Página inicial no estilo de loja, com foto grande e cartão de destaque, linhas em fotos altas, grade de produtos com preço, faixa "Permita-se Criar", galeria, "como funciona" e rodapé completo (ver mockup `home-estilo-loja.html`).
- **Pedido (carrinho)** no próprio navegador: o visitante adiciona vários tapetes e toca em "Encomendar pelo WhatsApp"; abre `wa.me` com mensagem listando os itens e quantidades.
- **Permita-se Criar:** formulário simples (peça, tema, cores, tamanho, quantidade, observações) que vira mensagem de orçamento no WhatsApp.
- **Encontre-nos:** endereço, mapa e botões de rota (Google Maps, Waze, Apple Maps), copiar endereço, WhatsApp.
- Preço visível nos produtos padrão (Conjunto de Banheiro, Conjunto de Cozinha); o personalizado é "orçamento pelo WhatsApp".
- Dono atualiza o site **pelo sistema**, sem programador: automático e por botão.

**Não objetivos (agora)**
- Pagamento online, frete calculado, cadastro de cliente, login.
- Formulário que envia e-mail ou guarda dados do visitante.
- Anexar foto no formulário (o link do WhatsApp não permite; o site orienta a enviar a foto na conversa).
- Seções "Quem faz", perguntas frequentes e depoimentos (ficam para depois, quando houver conteúdo).
- Campo "WhatsApp da loja" no sistema (o número fica na configuração do site).

**Anti-objetivos (seria fracasso mesmo funcionando)**
- O visitante esperar o Render acordar ou ver tela em branco.
- Parecer um modelo genérico, com fotos pequenas ou borradas.
- Expor custo, estoque exato, fornecedor ou observações internas.
- Para mudar um tapete ou uma foto, precisar de um programador.

## 3. Restrições

- **Tudo gratuito:** GitHub Pages e GitHub Actions (repositório **público**), Render e Supabase no plano grátis. Nada de chave, senha ou token no código.
- Render dorme após 15 min; o primeiro acesso leva cerca de 1 minuto.
- Neste PC o Node só existe dentro do IntelliJ (`.../acp-agents/.runtimes/node/24.19.0`); o build final roda no GitHub Actions.
- Fotos já ficam no banco (`produto_imagem`, até 8 por produto, 2 MB cada, JPG/PNG/WEBP, uma principal).
- Identidade: **Verde Kalke** (`#1c5c46`, `#163b30`, `#e2ece7`, dourado `#c8a24a`, fundo `#f4f7f4`), títulos em Georgia, corpo em Inter/sistema. Logo em `frontend/src/components/Logo.jsx`.
- Tom de voz: simples, acolhedor, pt-BR, sem jargão.

## 4. Abordagens consideradas

1. **Catálogo publicado (estático)** — escolhida. Uma rotina gera o site a partir do sistema e publica no GitHub Pages. Abre na hora, não depende do Render; custo: a mudança aparece depois da próxima publicação.
2. **Ao vivo pela API.** Sempre atualizado, mas o primeiro visitante espera ~1 min e as fotos saem do banco pelo Render. Descartada.
3. **Misto** (publicado + conferência ao vivo). Mais código para manter; só vale se atualização imediata for essencial. Descartada por ora.

O dono escolheu a 1, **com atualização automática e por botão**.

## 5. Decisões de projeto

**Do dono:** abordagem 1 com atualização automática e por botão; identidade Verde Kalke; estilo de loja da página inicial; preço nos conjuntos padrão e "Permita-se Criar" para o personalizado; carrinho com envio ao WhatsApp; rodapé "Encontre-nos" com rotas.

**Minhas escolhas (o dono pode corrigir):**
- **Repositório próprio** para o site, hospedado no GitHub Pages (endereço `github.io` por enquanto; domínio próprio é opcional depois).
- **Astro com ilhas React** (carrinho, filtros, formulário), gerando uma página estática por tapete com título e imagem para o link ficar bonito no WhatsApp (Open Graph) e `sitemap.xml`. Alternativa aceitável: Vite + React com pré-renderização, se o Astro der problema.
- **Gerar o site a partir de um endpoint público do sistema**, e não com acesso direto ao banco. Assim nenhuma senha do banco fica no GitHub.
- **Carrinho só no navegador** (`localStorage`), sem cookies de rastreamento.
- **Galeria "Feitos com carinho":** sai de produtos marcados "Só na galeria" no sistema (sem preço e sem botão de adicionar), então a Tereza gerencia tudo no mesmo lugar.
- **Número do WhatsApp na configuração do site**, não no sistema: (27) 99804-2658 (do cartão de visita). O número é público de propósito.

## 6. Como funciona

### 6.1 Sistema (backend + telas)
- **Migration V20** em `produto`: `portfolio_modo` (`NENHUM` | `VENDA` | `GALERIA`, padrão `NENHUM`), `portfolio_linha` (`BANHEIRO` | `COZINHA` | `SALA` | `OUTRA`), `portfolio_descricao` (até 500), `portfolio_ordem` (destaque), `portfolio_selo` (opcional: "Novo", "Mais pedido").
- **Tela de produto:** seção "Portfólio" com esses campos, o botão de fotos já existente e o aviso "aparece no site na próxima atualização".
- **Endpoint público** `GET /api/v1/publico/catalogo`: só produtos `ativo`, tipo `TAPETE` e `portfolio_modo` ≠ `NENHUM`. Devolve: id, nome, linha, descrição, cor, tamanho, preço (só no modo `VENDA`), selo, ordem, lista de fotos (id, principal, ordem). As fotos já têm leitura pública em `GET /api/v1/imagens/{id}` (existente), então não há endpoint novo de imagem. Nunca devolve custo, estoque, código de barras, fornecedor ou observações. Rota liberada no `SecurityConfig`, com limite de requisições por IP (modelo `LoginAttemptService`).
- **Botão "Atualizar site"** (nova permissão, por padrão DONO e GERENTE): o backend chama a API do GitHub para disparar o workflow de publicação, usando um token restrito ao repositório do site e guardado só como variável de ambiente no Render.

### 6.2 Publicação (repositório do site)
- Workflow no GitHub Actions com três gatilhos: **agendado** (ex.: a cada 6 horas), **manual** e **disparo pelo botão**.
- Passos: acordar o Render e tentar de novo por até alguns minutos → baixar `catalogo` e fotos → gerar versões reduzidas das fotos (miniatura para a grade, grande para o detalhe, WEBP) → montar o site → publicar no Pages.
- **Se o Render não responder**, o workflow falha e **o site anterior continua no ar**. Nada é publicado pela metade.
- As fotos não são gravadas no repositório (entram só no build), para não encher o limite do GitHub.

### 6.3 Site
- Página inicial: menu (com "Meu pedido" e contador), topo com foto grande e cartão "Detalhes que dão charme à sua casa", três linhas em fotos altas (Banheiro, Cozinha, Permita-se Criar), grade "Nossos tapetes" com filtro por linha, faixa Permita-se Criar, galeria, "Como funciona" (4 passos), rodapé.
- Página de cada tapete: várias fotos, nome, tamanho, cor, descrição, preço, "Adicionar ao pedido".
- **Pedido:** painel com itens e quantidades, total só dos itens com preço, aviso de que frete e personalizados são combinados no WhatsApp. Mensagem de exemplo: "Olá! Gostaria de encomendar: • 1× Conjunto Tulipas • 2× Conjunto Cogumelos. Pode me ajudar?" (texto editável na configuração).
- **Rotas:** links diretos (Google Maps, Waze, Apple Maps) a partir do endereço; mapa incorporado sem chave. Com a localização exata (pin) as rotas ficam precisas.
- Celular em primeiro lugar (a mesma página em uma coluna), contraste mínimo, texto alternativo nas fotos, navegação por teclado, sem rastreamento.

## 7. Ordem de construção
1. **Site** com um `catalogo` de exemplo no mesmo formato final, publicado no Pages, para o dono ver e aprovar o visual de verdade.
2. **Sistema:** migration, tela de portfólio, endpoint público, botão "Atualizar site".
3. **Publicação:** workflow que liga os dois e troca o exemplo pelos dados reais.

## 8. Pendências do dono (não bloqueiam o início)
- Localização exata da loja (pin do Google Maps) e horário de atendimento.
- Fotos boas dos tapetes (inteiro, detalhe e ambiente) e das encomendas já feitas.
- Preços dos conjuntos padrão (vêm do cadastro) e descrição de cada tapete para o site.
- Texto final da mensagem do WhatsApp e do Permita-se Criar (há uma proposta padrão).
- Endereço do site (`github.io` por enquanto).

## 9. O que fica para quem constrói
Ferramenta exata de redução de fotos, nomes dos campos do JSON, textos curtos do site, estados vazios (sem produtos, produto sem foto), tratamento de erro do botão, agenda do workflow, testes.
