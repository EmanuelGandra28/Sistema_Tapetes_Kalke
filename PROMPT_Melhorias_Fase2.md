# Prompt — Melhorias Fase 2 do Sistema Tapetes Kalke (21/11/2026)

Você vai evoluir o Sistema Tapetes Kalke (frontend React 19 + Vite no GitHub Pages, API Java 21/Spring Boot 3.4 no Render grátis, PostgreSQL no Supabase grátis, migrações Flyway em `backend/src/main/resources/db/migration`).

Antes de começar, leia: `README.md`, `SISTEMA_TAPETES_KALKE_VISAO_GERAL.md`, `PLANEJAMENTO_RASTREAMENTO_ENVIOS.md` e os arquivos citados em cada item. Siga os padrões que já existem (camadas controller → service → repository → entity, DTOs em `record`, `ApiResponse.ok`, `BaseAuditEntity`, `PermissionService`, React Query, `Modal`, `CrudPage`, `SearchSelect`, `Loading`/`Failure`/`Empty`).

## Regras de trabalho (valem para tudo)

- **Antes de codar cada bloco, mostre um plano curto** (arquivos que vai mexer, migração nova, riscos) e espere meu "ok". Os blocos B, C, D e F têm decisões em aberto, listadas no fim de cada um: pergunte antes.
- Um commit por item, mensagem em português. **Não faça push** sem eu pedir (push no `main` publica no GitHub Pages e no Render).
- Nunca altere migração Flyway já aplicada. Use a próxima livre (confira a pasta; hoje a última é V14).
- **Toda tabela nova deve ter `ENABLE ROW LEVEL SECURITY` na própria migração** (a V8 só cobriu as tabelas antigas).
- Todo endpoint novo confere permissão no servidor (`permissions.page/action`). Páginas e ações novas entram em `PermissionService` e numa migração que popula `permissao_pagina`/`permissao_acao`.
- O repositório é **público**: nenhum segredo, chave ou telefone real no código. Tudo por variável de ambiente (`render.yaml` com `sync: false`).
- Hospedagem continua **gratuita**: não adicionar serviço pago. O Render dorme após 15 min, então rotinas agendadas rodam **de fora** (GitHub Actions chamando endpoint interno protegido por segredo), nunca com `@Scheduled`.
- Não quebrar regras já validadas (estoque, venda, compra, produção, relatório financeiro). Acrescente testes automáticos para regra nova no backend.
- Rode `npm run lint`, `npm run build` e `mvn -q test`; informe o que não deu para rodar.
- Textos da tela sempre em português do Brasil, botões dizem a ação, linguagem simples (o dono não é técnico).
- Ao final de cada bloco: lista do que mudou por arquivo e como validar na tela.

---

## BLOCO A — Ajustes rápidos

### A1. Botão "Chamar no WhatsApp" do cliente (`Clientes.jsx`)
- O link deve sair com o código do Brasil: normalize o telefone (só dígitos; se tiver 10 ou 11 dígitos, prefixe `55`) e use `https://wa.me/55DDDNUMERO?text=...`.
- Mensagem pronta, editável e com o nome do cliente, por exemplo: "Olá, {nome}! Aqui é da Tapetes Kalke. Faz um tempinho que não nos vemos. Posso te ajudar com algum tapete?". Esse texto é o padrão do modelo `BOTAO_WHATSAPP_CLIENTE` do Bloco C; até o Bloco C existir, use uma constante única que depois vira o texto padrão gravado na migração.
- O botão só aparece com **2 meses ou mais** sem compra (situação "Atenção (Inativo)" ou "Inativo"), não com 1 mês. Confira se a regra do backend e a do frontend dizem a mesma coisa.
- Telefone vazio ou inválido: botão desabilitado com dica "Cliente sem telefone válido".

### A2. Editar e excluir despesas
- Backend: `PUT` e `DELETE` de despesa, com ações de permissão próprias (`despesas_editar`, `despesas_excluir`), só DONO e GERENTE por padrão.
- Exclusão **lógica** com motivo (como já se faz em vendas/compras), para o relatório financeiro refletir na hora e a auditoria ficar.
- Frontend: botões "Editar despesa" e "Excluir despesa" (vermelho) na lista, com confirmação.
- Teste: editar valor muda o lucro líquido do relatório; excluir remove do período.

### A3. Tela de movimentações de estoque
- A API já guarda o histórico (saldo anterior e posterior, usuário, origem). Crie a tela **"Movimentações"** dentro de Produtos/Estoque, com filtros por produto, período, tipo (entrada/saída/ajuste) e origem (venda, compra, produção, ajuste manual).
- Mostrar: data/hora, produto, quantidade (verde entrada, vermelho saída), saldo antes → depois, quem fez, motivo. Paginado.
- Atalho no cadastro do produto: "Ver movimentações deste produto".
- Se faltar endpoint de consulta com filtros, crie, com permissão por página.

### A4. Paginação das listas
- Backend: `Pageable` (página e tamanho, padrão 20) em vendas, compras, ordens de produção, clientes, produtos, despesas e movimentações, com ordenação por data decrescente. Devolver total de itens e de páginas.
- Frontend: componente único `Paginacao` ("Anterior / Próxima / página X de Y") reaproveitado nas telas, com `keepPreviousData` para não piscar. Combinar com a busca por debounce que já existe.
- Não pode quebrar o dashboard nem os relatórios, que usam consultas próprias.

---

## BLOCO B — Reativação de clientes pelo WhatsApp

Base: seção 4 do `SISTEMA_TAPETES_KALKE_VISAO_GERAL.md` (gateway "Orquestra WhatsApp Gateway" já contratado e validado: 500 mensagens/mês, janela 08h–20h, consentimento LGPD por finalidade, webhooks assinados). **A integração ainda não foi programada.** Leia também a memória do projeto sobre o gateway.

**Escopo:**
1. **Cliente de integração** do gateway com `RestClient` (sem biblioteca nova), chave em variável de ambiente, e uma interface `WhatsAppProvider` para podermos trocar de provedor depois.
2. **Consentimento (LGPD):** campo e histórico de consentimento por finalidade ("marketing/reativação", "avisos de pedido"), com data, origem e botão de revogar. **Cliente sem consentimento nunca recebe mensagem de campanha.** Resposta "SAIR/PARAR" revoga sozinho.
3. **Classificação** dos clientes por ciclo de recompra e valor (ex.: Ouro/Prata/Bronze × Em dia/Atenção/Inativo), calculada das vendas.
4. **Campanhas:** até 3 mensagens com intervalo entre elas, **textos vindos dos modelos `REATIVACAO_1/2/3` do Bloco C** (nada de texto fixo no código; todo envio passa por `MensagemService.renderizar`), **aprovação manual** do dono antes de cada disparo, e pré-visualização de quem vai receber.
5. **Fila diária** com limite diário e **só entre 08h e 20h**; respeitar a cota de 500 mensagens/mês, com contador visível e bloqueio ao atingir 90%.
6. **Respostas:** receber webhook (validar assinatura), registrar e classificar (interessado, sem interesse, pediu para sair, outro) e mostrar para a equipe responder.
7. **Resultados:** quantos receberam, responderam e **voltaram a comprar** em até 30 dias.
8. Tela **"Reativação"** (página e ações novas na matriz de permissões; padrão: DONO e GERENTE). Disparo da fila por GitHub Actions chamando endpoint interno com segredo.

**Decisões em aberto (pergunte):**
- O gateway usa a biblioteca **baileys**, não oficial; há risco de bloqueio do número. Mantemos assim ou pesquisamos a API oficial da Meta? Como mitigar: volume baixo, aprovação manual, opt-out simples.
- O intervalo entre as 3 mensagens (os textos são definidos no Bloco C).
- Como registrar o consentimento dos clientes que já existem (campo no cadastro? importação?).

---

## BLOCO C — Modelos de mensagens (aba "Mensagens" em Configurações)

**Objetivo:** nenhum texto de mensagem automática fica no código. A dona e a gerente criam e ajustam os textos numa aba simples, sem precisar de programador. **Este bloco vem antes de B e F**, que usam os modelos.

**Permissão**
- Nova ação `mensagens_gerenciar` na matriz de permissões (`PermissionService` + migração que popula `permissao_acao`). Padrão: ADMINISTRADOR, DONO e GERENTE. O ADMINISTRADOR liga ou desliga por cargo, como nas demais ações.
- O servidor confere a permissão em todo endpoint, não só a tela. Nova aba "Mensagens" em `Configuracoes.jsx`, visível só com a ação liberada.

**Banco** (migração nova, com RLS ligado)
- Tabela `mensagem_modelo`: `chave` única (gatilho, ex.: `REATIVACAO_1`, `REATIVACAO_2`, `REATIVACAO_3`, `ENVIO_ENCOMENDA`, `SAIU_PARA_ENTREGA`, `COMPROVANTE_VENDA`, `ANIVERSARIO`, `POS_VENDA`, `ALERTA_PRODUCAO_DONA`, `BOTAO_WHATSAPP_CLIENTE`), `nome` amigável, `tipo` (`AVISO_PEDIDO`, `CAMPANHA` ou `INTERNA`), `texto`, `ativo`, mais os campos de auditoria do `BaseAuditEntity`.
- Tabela `mensagem_modelo_historico`: versão anterior do texto, quem alterou e quando (para ver o que mudou e restaurar).
- A migração já grava os **textos padrão** de cada gatilho, para a aba nunca nascer vazia, e o código guarda o texto padrão para o "Restaurar padrão".

**Variáveis**
- Cada gatilho declara as variáveis que aceita (`{nome}`, `{ultimo_produto}`, `{dias_sem_comprar}`, `{codigo_rastreio}`, `{link_rastreio}`, `{data_entrega}`, `{loja}` etc.). Um serviço único `MensagemService.renderizar(chave, dados)` troca as variáveis. **Todo envio de mensagem do sistema passa por ele.**
- Variável inexistente ou mal escrita (`{nme}`) **bloqueia o salvamento** com mensagem clara. Cliente nunca recebe texto com campo quebrado: se faltar dado na hora do envio (ex.: sem rastreio), usar um texto alternativo ou não enviar e registrar o motivo.

**Tela (para quem não é técnico)**
- Lista de gatilhos com nomes em português, no estilo "Clientes sem compras nos últimos 2 meses" e "Envio de encomendas", cada um com etiqueta do tipo (Aviso de pedido, Campanha, Interna) e interruptor Ativo/Desativado.
- Ao abrir um modelo: caixa de texto, **botões com as variáveis** (clicar insere o campo no cursor), contador de caracteres e **pré-visualização ao vivo** com um cliente de exemplo.
- Botões: "Salvar", "Restaurar texto padrão" e "Ver histórico" (com opção de voltar a uma versão).
- Botão "Enviar teste para o meu número", com limite diário baixo, avisando que gasta a cota. Só depois do cliente do gateway (Bloco B) existir.
- Mostrar o consumo da cota mensal de 500 mensagens e, ao editar uma campanha, a estimativa de quantas mensagens ela gasta.

**Regras de segurança dos textos**
- Nos modelos do tipo **CAMPANHA**, a frase de saída ("Responda SAIR para não receber mais mensagens") é **fixa**: aparece na pré-visualização, é anexada pelo sistema e não pode ser apagada. Isso protege a LGPD e o número (o gateway usa baileys, não oficial).
- Limite de tamanho (sugestão: 700 caracteres) e aviso quando houver link; bloquear encurtadores suspeitos.
- Modelo `ALERTA_PRODUCAO_DONA`: a lista de tapetes é obrigatória e não pode ser removida do texto.
- **Aviso de pedido** (encomenda, entrega, comprovante) pode ir a quem fez a compra. **Campanha** só vai a quem tem consentimento (Bloco B). A tela deixa essa diferença visível.
- Mudança vale na hora. O histórico mostra autor, data e texto anterior, e dá para restaurar.

**Integração com os outros blocos**
- A1 (botão de WhatsApp do cliente) usa o modelo `BOTAO_WHATSAPP_CLIENTE`.
- B (reativação) usa `REATIVACAO_1/2/3`. F (alerta de produção) usa `ALERTA_PRODUCAO_DONA`. Rastreamento de envios e "saiu para entrega" usam os respectivos modelos.
- Se algum bloco anterior precisar de texto antes deste existir, usar o padrão gravado na migração.

**Testes:** renderização com e sem dados faltando, variável inválida bloqueada, restaurar padrão, histórico, frase de saída sempre presente em campanha, permissão negada para cargo sem a ação.

**Decisões em aberto (pergunte):** os textos padrão de cada gatilho (posso sugerir uma primeira versão para você aprovar), o limite de caracteres e se o ADMINISTRADOR quer ser avisado quando um modelo for alterado.

---

## BLOCO D — Fotos no cadastro do produto (e na Ordem de Produção)

- **Anexar imagens ao produto** (várias, com uma marcada como principal): botão "Anexar fotos" no cadastro do produto em Estoque, com pré-visualização, trocar a principal e remover.
- **Armazenamento gratuito:** Supabase Storage (bucket para fotos de produto). Proponha e confirme comigo o desenho antes: o backend recebe o arquivo (ou gera URL assinada de upload), grava só o caminho/URL numa tabela nova `produto_imagem` (`id_produto`, `url`, `principal`, `ordem`) e **valida** tipo (JPG/PNG/WEBP) e tamanho (máx. ~5 MB).
- **Redução no navegador** antes de enviar (largura máxima ~1280 px, formato WEBP/JPEG com qualidade ~80), para economizar o 1 GB do plano grátis e carregar rápido no celular. Gerar também miniatura para listas.
- Chave de serviço do storage só em variável de ambiente do Render. O bucket deve permitir **leitura pública** (para o WhatsApp e para outros usos futuros, como um site de portfólio que será feito à parte) e **escrita só pelo backend**.
- **Onde a foto aparece:** miniatura na lista de produtos, no cadastro, na **venda** (busca de produto), na **Ordem de Produção** (lista e detalhe, para a equipe saber qual modelo produzir, inclusive nas matérias-primas), e no alerta de WhatsApp (Bloco F). Deixe as fotos acessíveis por endpoint/URL estável para um futuro site de portfólio (feito em outro projeto; **não crie nada de portfólio aqui**).
- Permissão nova: ação `produtos_imagens` (anexar/remover), padrão DONO, GERENTE e ESTOQUISTA.
- Produto sem foto mostra um espaço reservado com a marca.

**Decisão em aberto (pergunte):** confirmar Supabase Storage como destino das fotos (alternativa: guardar no banco, o que não recomendo pelo limite de espaço).

---

## BLOCO E — Relatórios: gráficos, Excel/PDF e leitura fácil

Hoje a tela de Relatórios é simples (só o financeiro por período). Refaça com foco em **entendimento fácil** para quem não é técnico.

1. **Gráficos** (use uma biblioteca leve já compatível com React; informe qual e o peso; evite as pesadas):
   - Vendas por dia/mês (linha ou barras), com comparação com o período anterior.
   - Receita × despesas × lucro (barras agrupadas).
   - Vendas por forma de pagamento (pizza/rosca).
   - Vendas por vendedor.
   - Produtos mais vendidos.
   - Os gráficos acompanham o **tema** escolhido (5 temas) e têm contraste adequado.
2. **Resumo em linguagem simples** no topo de cada relatório (cartões grandes: "Você vendeu R$ X", "Sobrou de lucro R$ Y", "O que mais vendeu: Z"), com seta verde/vermelha comparando com o período anterior.
3. **Curva ABC — redesenhar para ficar óbvia.** O formato atual está confuso. Proposta:
   - Trocar o nome técnico por **"Produtos que mais dão dinheiro"**, com uma frase explicativa ("Poucos produtos geram a maior parte da sua venda").
   - Três grupos com **cores e rótulos claros**: **Campeões** (A, ~80% da venda), **Intermediários** (B) e **Cauda** (C). Mostrar um gráfico de barras ordenado + a lista, e uma frase por grupo ("12 produtos geram 80% do seu faturamento").
   - Ícone de ajuda "O que é isso?" abrindo explicação curta com exemplo.
4. **Margem — redesenhar:** tabela com **lucro por produto em R$ e em %**, com marcadores de cor (verde: boa; amarelo: atenção; vermelho: baixa/prejuízo) e ordenação por clique. Mostrar custo × preço lado a lado. Frase no topo: "Estes são os produtos que menos lucro deixam". Definir e explicar as faixas de cor (configuráveis).
5. **Exportar para Excel e PDF** (hoje muito simples):
   - **Excel (.xlsx)** com Apache POI (já está no backend): cabeçalho com a marca e o período, colunas formatadas (moeda, data), linha de totais, congelar cabeçalho, uma aba por relatório, largura de coluna ajustada.
   - **PDF** bonito e imprimível (com marca, período, resumo e tabelas; gráficos como imagem se viável). Pesquise uma biblioteca gratuita adequada (backend, ex.: OpenPDF, ou geração no navegador) e justifique a escolha, avisando se adicionar dependência.
   - Botões "Baixar Excel" e "Baixar PDF" em **todos** os relatórios, respeitando a permissão de ver relatórios.
6. Filtros claros: atalhos "Hoje, 7 dias, Este mês, Mês passado, Este ano" e período personalizado com validação de datas.
7. Reaproveite o mesmo PDF para o **comprovante de venda** se ficar simples (pergunte antes de incluir).

---

## BLOCO F — Alerta de WhatsApp para a dona ao gerar Ordem de Produção

Já existe o ponto de integração (`ReposicaoAutomaticaService`, evento `AFTER_COMMIT`, comentário "avisar pelo WhatsApp"). **Depende do cliente de integração do Bloco B (item 1) e das fotos do Bloco D.**

- Quando uma **Ordem de Produção automática** for criada (estoque mínimo/esgotado), enviar mensagem ao WhatsApp da dona descrevendo **cada tapete** com estoque baixo ou esgotado: nome, cor, tamanho, saldo atual × mínimo, quantidade sugerida a produzir e **a foto** de cada um.
- **Agrupar** em uma única mensagem quando várias ordens forem geradas em sequência (janela de alguns minutos), para não gastar a cota de 500/mês nem encher o celular da dona. Uma foto por produto (principal); se o gateway limitar mídia, enviar as fotos em sequência ou uma colagem.
- Respeitar a janela 08h–20h: se a ordem nascer fora do horário, **enfileirar** e enviar às 08h.
- Telefone da dona e liga/desliga do alerta em Configurações (tabela `configuracao`); consentimento registrado para a finalidade "avisos operacionais".
- **Idempotência:** nunca mandar a mesma ordem duas vezes (guardar status do aviso na ordem: pendente/enviado/falhou, com tentativas).
- **Falha no envio não pode afetar** a venda nem a criação da ordem (já é o padrão do evento). Registrar o erro e mostrar na ordem "Aviso não enviado — reenviar".
- Link na mensagem para abrir a ordem no sistema.
- O texto vem do modelo `ALERTA_PRODUCAO_DONA` do Bloco C (editável pela dona e pela gerente; a lista de tapetes é obrigatória). Mensagem curta e clara, no estilo do texto padrão:
  "Atenção, Kalke! Estes tapetes precisam ser produzidos: 1) Tapete X (cor, tamanho) — saldo 2, mínimo 5 → produzir 8. [foto] ... Abra: {link}".

---

## BLOCO G — Tutorial dentro do sistema (ajuda contextual por tela)

Já existem o guia do sistema (cada tela com exemplos) e o guia de instalação. Transforme em **ajuda contextual**:

- Botão fixo **"?" Ajuda** em cada tela que abre um painel lateral com: para que serve a tela, o passo a passo das ações principais, exemplos e dúvidas comuns. Reaproveite o texto dos guias existentes (uma fonte só, sem duplicar).
- **Passeio guiado no primeiro acesso** de cada tela (balões apontando os botões principais), com "Pular" e "Rever tutorial" nas Configurações. Guardar o que o usuário já viu na conta dele (não só no navegador).
- Conteúdo **por cargo**: o vendedor só vê ajuda das telas que ele acessa; o entregador, só entregas.
- Dica rápida ao lado de campos difíceis (ex.: "Estoque mínimo", "Ficha técnica").
- Sem biblioteca pesada: prefira componente próprio ou uma biblioteca pequena (informe qual).
- Texto simples, sem jargão, com imagens/ícones onde ajudar.
- Acessibilidade: usável por teclado e com contraste nos 5 temas.

---

## Ordem de execução sugerida

1. **A** (rápido, sem dependências).
2. **D** (fotos), pois alimenta o F.
3. **E** (relatórios).
4. **G** (ajuda contextual).
5. **C** (modelos de mensagens), pois B e F dependem dele.
6. **B** (cliente do gateway + reativação) e em seguida **F** (alerta da ordem), que reaproveita o cliente do gateway e os modelos.

O site de portfólio **não faz parte** deste prompt (será tratado em outro chat).

Pergunte-me as decisões em aberto de cada bloco antes de começar o bloco.
