# Sistema Tapetes Kalke — Novidades da atualização de 21/11/2026

Este documento reúne **tudo o que foi implementado de novo** na Fase 2, o que precisa ser configurado antes de usar, **o que foi e o que não foi testado** e o que ficou de fora.

> **Situação do código:** tudo está commitado no computador (branch `main`) e **nada foi enviado ao GitHub** ainda. Enquanto não houver o envio, o site e a API publicados continuam como antes.

---

## 1. Resumo

| # | Novidade | Onde fica | Quem vê (padrão) |
|---|---|---|---|
| 1 | Botão "Chamar no WhatsApp" corrigido | Clientes | quem acessa Clientes |
| 2 | Editar e excluir despesas | Despesas | Dono e Gerente |
| 3 | Listas em páginas | Vendas, Compras, Ordens, Despesas, Clientes, Estoque | todos |
| 4 | Fotos dos produtos | Estoque e Ordens de produção | Dono, Gerente, Estoquista (anexar) |
| 5 | Relatórios com gráficos, ABC e margem simples, Excel e PDF | Relatórios | quem tem a ação de relatórios |
| 6 | Aba "Mensagens" (textos editáveis) | Configurações | Dono e Gerente (Administrador sempre) |
| 7 | Ajuda em cada tela + passeio na primeira visita | todas as telas | todos |
| 8 | Reativação de clientes pelo WhatsApp | Reativação (menu) | Dono e Gerente |
| 9 | Autorização de WhatsApp do cliente (LGPD) | Clientes | Dono, Gerente, Vendedor |
| 10 | Alerta de produção para a dona pelo WhatsApp | Configurações | Dono |
| 11 | Tela "Novidades" no primeiro login após cada atualização | topo de todas as telas | todos |

O **Administrador** sempre tem acesso a tudo. Tudo isso pode ser ajustado em Configurações → Permissões.

---

## 2. O que mudou, em detalhe

### 2.1 Botão "Chamar no WhatsApp" do cliente
- O link agora sai com o código do Brasil (`55`) e uma **mensagem pronta** com o primeiro nome do cliente.
- Só aparece com **2 meses ou mais** sem comprar (antes aparecia com 1 mês).
- Telefone inválido: o botão fica desabilitado com a dica "Cliente sem telefone válido".
- O texto vem do modelo editável **"Botão Chamar no WhatsApp"** (aba Mensagens). Se a mensagem for desativada lá, o botão abre a conversa em branco.

### 2.2 Despesas: editar e excluir
- Botões **"Editar despesa"** e **"Excluir despesa"**.
- A exclusão é **lógica**: pede o motivo, some das listas e dos relatórios, mas fica registrada (quem excluiu, quando e por quê).
- Duas permissões novas: `despesas_editar` e `despesas_excluir`.

### 2.3 Listas em páginas
- Vendas, compras, ordens de produção, despesas, clientes e produtos passam a ser carregados em páginas (controle "Anterior / Próxima"), para o sistema não ficar lento com o tempo.
- Quem não pede página continua recebendo a lista completa (a tela de Entregas depende disso).
- A tela de **Movimentações de estoque** já existia, com filtros e páginas.

### 2.4 Fotos dos produtos
- Em **Estoque → Fotos**: anexar até **8 fotos** por produto, escolher a **principal** e remover.
- O navegador reduz a foto antes de enviar (lado maior até 1280 px, WEBP/JPEG ~80%): carrega rápido e poupa o espaço gratuito.
- A foto principal aparece em **Estoque** e em **Ordens de produção** (para saber qual modelo produzir). Produto sem foto mostra a marca TK.
- O servidor confere o tipo pelo **conteúdo** do arquivo (JPG, PNG ou WEBP), máximo 2 MB.
- As fotos ficam **no próprio banco** (não no Supabase Storage): não precisa de chave extra. Com ~150 KB por foto, 300 fotos ocupam cerca de 45 MB.
- A foto abre por um endereço público com identificador aleatório (necessário para o WhatsApp e para o futuro portfólio).
- Permissão nova: `produtos_imagens`.

### 2.5 Relatórios
- **Atalhos de período:** Hoje, Últimos 7 dias, Este mês, Mês passado, Este ano.
- **Aba Resumo (nova):** frase em linguagem simples ("você vendeu R$ X e sobraram R$ Y"), quatro cartões comparados ao período anterior (setas verde/vermelha) e gráficos: vendas dia a dia, para onde foi o dinheiro, forma de pagamento e os 5 tapetes que mais venderam.
- **"Produtos que mais dão dinheiro"** (antiga Curva ABC): grupos **Campeões, Intermediários e Cauda**, com quantidade de produtos e participação nas vendas; botão "O que é isso?".
- **"Quanto sobra em cada tapete"** (antiga Margem): lucro de cada produto, com etiqueta **Boa** (30% ou mais), **Atenção** (15 a 30%), **Baixa** e **Prejuízo**; ordenação por clique.
- **Excel:** título com a marca, período e data de geração, cabeçalho colorido, bordas, filtro, linha de títulos congelada, totais e configuração para imprimir.
- **PDF:** botão **"Imprimir / salvar em PDF"** (abre a impressão do navegador com cabeçalho da loja; escolha "Salvar como PDF"). *Não gera um arquivo PDF por conta própria.*
- Gráficos desenhados na própria tela (sem biblioteca nova), seguindo o tema escolhido.

### 2.6 Aba "Mensagens" (Configurações)
- **10 gatilhos** com textos padrão: 3 mensagens de reativação, envio de encomendas, saiu para entrega, comprovante de venda, pós-venda, aniversário, botão de WhatsApp do cliente e alerta de produção para a dona.
- Editor com **botões de campos** (`{nome}`, `{loja}`...), contador (limite de 700 caracteres) e **prévia ao vivo**.
- Proteções: campo escrito errado impede de salvar; links encurtados são bloqueados; campos obrigatórios não podem sumir; em **campanhas**, a frase "Responda SAIR para não receber mais mensagens." é acrescentada pelo sistema e não pode ser apagada.
- Interruptor Ativa/Desativada, **Restaurar texto padrão** e **histórico** (autor, data e texto anterior, com opção de voltar).
- Permissão nova: `mensagens_gerenciar`.
- *Ainda não existe o botão "Enviar teste para o meu número".*

### 2.7 Ajuda contextual
- Botão **"Ajuda"** em cada tela: para que serve, passo a passo, exemplo e dúvidas comuns. Os passos respeitam o cargo.
- **Passeio na primeira visita** a cada tela (um passo por vez, com "Pular"). O que cada pessoa já viu fica **guardado na conta**.
- **Dicas "?"** nos campos "Estoque mínimo".
- Em Configurações → **"Rever os tutoriais das telas"**.
- *O passeio é um passo a passo no painel lateral; não há setas apontando para cada botão.*

### 2.8 Reativação de clientes pelo WhatsApp
- Nova tela **Reativação** (menu), com painel de status: envio ligado/desligado, cota do mês (X de 500), horário e recebimento de respostas.
- **Quem pode receber:** clientes ativos, **2+ meses sem comprar**, telefone válido, **autorização registrada** e fora de outra campanha. Classificados por valor (**Ouro** = 20% que mais compraram, **Prata** = próximos 30%, **Bronze**) e por tempo sem comprar.
- **Campanhas:** nome, dias entre as mensagens (padrão 5) e máximo por dia (padrão 20). Nasce como **rascunho**; só depois de **"Aprovar e iniciar"** as mensagens saem. Dá para pausar e cancelar.
- **Envio:** até **3 mensagens** por cliente (textos da aba Mensagens), **só entre 8h e 20h**, respeitando o limite diário. Para em **450 mensagens/mês** (90% da cota de 500). Sem saldo (402) ou sem permissão (403), tudo para.
- Sai da campanha: quem **voltou a comprar**, quem **perdeu a autorização**, quem **respondeu** e quem **pediu para sair**.
- **Respostas** (via webhook assinado) são classificadas em *interessado, sem interesse, pediu para sair, outra*. "SAIR" revoga a autorização na hora.
- **Resultados** por campanha: enviadas, responderam, o que disseram e **quantos voltaram a comprar em até 30 dias**.
- Permissões novas: página `reativacao`, ações `reativacao_gerenciar` e `clientes_consentimento`.

### 2.9 Autorização de WhatsApp do cliente (LGPD)
- Na tela de Clientes: **"Registrar autorização de WhatsApp"**. Quem registra **descreve como o cliente autorizou** (mínimo 10 caracteres) e confirma. O sistema nunca inventa essa evidência.
- Etiqueta **"Autorizou mensagens"** e botão **"Revogar autorização"**.
- Sem autorização, o cliente **nunca** recebe campanha.

### 2.10 Alerta de produção para a dona
- Quando o sistema abre uma **ordem de produção automática**, o aviso entra numa fila. Tudo o que está pendente sai em **uma única mensagem** com a lista: *"1) Tapete Floral (Azul, 2x3) — saldo 2, mínimo 5 → produzir 8"*, o link para as ordens e **até 3 fotos**.
- Só sai entre 8h e 20h; fora do horário espera até as 8h. Respeita a cota do mês (cada foto conta). Cada ordem avisa **uma vez**. Falhou 5 vezes? Aparece o botão **"Reenviar avisos que falharam"**.
- Em Configurações → **"Alerta de produção por WhatsApp"**: número da dona e interruptor. Ao ligar, fica registrado quem ativou, quando e para qual número (é a autorização exigida pelo WhatsApp).
- Uma falha no aviso **nunca** atrapalha a venda nem a criação da ordem.
- O texto é o modelo "Alerta de produção para a dona" (a lista de tapetes é obrigatória).

### 2.11 Tela "Novidades"
- No **primeiro login depois de cada atualização**, abre sozinha uma janela **"O sistema foi atualizado!"** com o que mudou, **só o que o cargo da pessoa pode usar**, com link para cada tela. Aparece uma vez por pessoa (guardado na conta).
- O botão **"Novidades"** no topo reabre o histórico a qualquer momento. Um pontinho dourado indica novidade ainda não vista.
- Quem foi cadastrado **depois** da atualização não vê aquela versão (já entra na versão nova e tem o passeio da Ajuda).
- **Como adicionar novidades em atualizações futuras:** acrescente uma versão **no topo** da lista em `frontend/src/content/novidades.js` (e atualize este documento). Cada item pode exigir uma página (`pagina`) ou ação (`acao`) para aparecer.

---

## 3. O que precisa ser configurado antes de usar

### 3.1 Obrigatório para o sistema subir
1. **Enviar o código ao GitHub** (`git push`): o envio publica as telas e a API sozinho.
2. As **migrações de banco V15 a V22** rodam sozinhas no deploy (a V20 e a V21 são do portfólio, feitas em outro chat).

### 3.2 Só para o WhatsApp funcionar (sem isso tudo funciona, menos o envio)
| Onde | O quê |
|---|---|
| Render (variáveis) | `GATEWAY_API_KEY` (chave do Orquestra), `GATEWAY_WEBHOOK_SECRET` (segredo do webhook), `CRON_SECRET` (um segredo qualquer, longo e aleatório) |
| GitHub → Settings → Secrets → Actions | `CRON_SECRET` (**igual** ao do Render). A variável `API_URL` já existe. |
| Painel do gateway → Webhooks | URL `https://tapetes-kalke-api.onrender.com/api/v1/webhooks/orquestra` com os eventos `mensagem.situacao`, `mensagem.recebida` e `contato.descadastrado` |
| Sistema | Configurações → Alerta de produção: número da dona. Clientes: registrar autorizações. |

- A rotina `reativacao.yml` (GitHub Actions) roda **a cada 30 minutos, das 8h às 19:30**, acorda a API e envia a fila das campanhas aprovadas e os alertas de produção. **Sem o segredo, não faz nada.**
- **Antes da primeira campanha real, teste com um número seu e poucas mensagens.**
- Risco conhecido: o número do gateway usa a biblioteca **baileys** (não oficial); há risco de bloqueio do número. Mitigações já no sistema: aprovação manual, limite diário, horário comercial, autorização do cliente e opção de sair.

---

## 4. Banco de dados (novas migrações)

| Migração | Conteúdo |
|---|---|
| V15 | Despesas: exclusão lógica + permissões `despesas_editar` e `despesas_excluir` |
| V16 | Fotos dos produtos (`produto_imagem`) + permissão `produtos_imagens` |
| V17 | Mensagens (`mensagem_modelo`, `mensagem_modelo_historico`) + permissão `mensagens_gerenciar` |
| V18 | O que cada usuário já viu (`usuario_visto`) — usado pela Ajuda e pelas Novidades |
| V19 | Reativação: consentimentos, campanhas, destinatários, mensagens e eventos do WhatsApp + permissões |
| V22 | Alerta de produção: campos na configuração da loja e fila `alerta_producao` |

Todas as tabelas novas já nascem com **RLS ligado** (bloqueia o acesso pela API pública do Supabase).

---

## 5. Testes

### 5.1 Testes automáticos
| Área | Resultado |
|---|---|
| Backend | **139 passam**, 0 falhas |
| Frontend | **58 passam**, 0 falhas; lint e build sem erros |

Cobrem as regras de despesas, fotos, relatórios, Excel, mensagens, ajuda, novidades, reativação (horário, limite diário, cota, consentimento, recompra, erros do gateway, idempotência, respostas), cliente do gateway (token, renovação, repetição, upload de foto), webhook (assinatura, janela de tempo, evento repetido) e alerta de produção.

### 5.2 Teste com banco de dados de verdade (PostgreSQL 17 local) — feito
Com a sua autorização, subi um PostgreSQL 17 portátil nesta máquina, rodei a API real contra ele e testei pelas chamadas da API e pelo navegador. **Nenhuma mensagem foi enviada a ninguém** (chave falsa, gateway inexistente; a rotina de envio só rodou fora do horário comercial e se recusou a enviar). Tudo foi apagado depois; **nada foi criado no banco do Supabase nem no sistema publicado**.

**Migrações:** as **22 migrações aplicaram sem erro** num banco limpo e o Hibernate validou todas as entidades (a API subiu).

**Chamadas da API: 96 verificações, todas corretas**, entre elas:
- despesas: criar, editar, excluir com motivo (some das listas e do relatório);
- páginas nas listas (e lista completa quando não se pede página);
- fotos: primeira vira principal, arquivo falso recusado, leitura pública sem login, trocar/remover a principal;
- relatórios: resumo, período anterior, ABC, margem e os 4 Excel gerados (arquivos .xlsx válidos);
- mensagens: 10 gatilhos, campo errado recusado, histórico, restaurar padrão, desativar;
- ajuda/novidades: marcar visto, repetir, limpar;
- autorização do cliente (LGPD), candidatos (só quem autorizou e está há 2+ meses sem comprar), campanha: criar, aprovar, pausar, cancelar;
- rotina agendada: sem segredo e com segredo errado = proibido; fora do horário não envia nada;
- alerta de produção: venda derrubou o estoque, ordem automática criada, aviso entrou na fila e esperou (fora do horário);
- webhook assinado: sem assinatura/segredo errado/tempo velho = recusado, válido aceito, repetido ignorado, descadastro revogou a autorização;
- permissões: o vendedor não vê Reativação, não edita mensagens, não mexe no alerta nem cria campanha, mas lê o texto do botão e registra autorização.

**Telas abertas no navegador com dados reais:** Dashboard, Relatórios (Resumo com gráficos, Produtos que mais dão dinheiro, Quanto sobra em cada tapete), Estoque (miniaturas e botão Fotos), Reativação, Configurações → Mensagens (editor com prévia), Clientes (botão de WhatsApp com `55` e texto do modelo, autorização), Ajuda (passeio) e Novidades.

### 5.3 Defeitos encontrados nesse teste (já corrigidos)
1. **Duas migrações com a versão 20** (a minha e a do portfólio, criadas ao mesmo tempo por chats diferentes): a API **não subiria**. A minha foi renumerada para **V22**.
2. **No primeiro login abriam duas janelas juntas** (Novidades e o passeio da Ajuda). Agora a Ajuda espera as Novidades terminarem.
3. "1 clientes" na lista de campanhas (faltava o singular).
4. (Antes, nos testes automáticos) o passeio da Ajuda quebraria sem permissões em Compras/Despesas, e o envio de foto ao gateway usaria uma biblioteca que o projeto não tem.

### 5.4 O que NÃO foi testado (de propósito ou por limite)
| Item | Motivo |
|---|---|
| **Envio real de WhatsApp** (campanhas e alerta) e **webhook real do gateway** | Por decisão sua: nenhum teste que envie mensagem a clientes. Os envios foram testados só com gateway simulado nos testes automáticos. |
| **Envio dentro do horário comercial** | O teste rodou de madrugada; dentro do horário só há cobertura dos testes automáticos. |
| **Rotina do GitHub Actions** | Só roda depois do envio ao GitHub e com o segredo cadastrado. |
| **Impressão em PDF** e **abrir o Excel** no programa | Conferi que os arquivos são válidos, mas ninguém imprimiu/abriu. |
| **Celular / outros navegadores** | Testado só no navegador do computador. |

**Recomendação antes de usar em produção:** enviar ao GitHub, conferir no Render (logs) que as migrações rodaram e, antes de qualquer campanha real, testar com um número seu e poucas mensagens.

---

## 6. O que NÃO foi implementado
- **Portfólio / catálogo online:** tratado em outro chat. Os dados necessários estão em `BRIEFING_PORTFOLIO_TAPETES_KALKE.md`; as fotos já ficam acessíveis por endereço público.
- **PDF gerado pelo servidor:** hoje é a impressão do navegador ("Salvar como PDF").
- **Balões apontando para os botões** no passeio da Ajuda.
- **Tela para conectar o número por QR Code** e **página de monitoramento** do gateway de WhatsApp.
- **Botão "Enviar teste para o meu número"** na aba Mensagens.
- **Aviso automático "saiu para entrega"**, comprovante pelo WhatsApp, aniversário e pós-venda automáticos (os **textos** já existem na aba Mensagens; falta o disparo).
- **Rastreamento de envios (Correios)**, tapete sob medida, orçamentos, vendas a prazo (continuam nas ideias futuras).

---

## 7. Arquivos novos de referência
- `PROMPT_Melhorias_Fase2.md` — o plano desta fase.
- `BRIEFING_PORTFOLIO_TAPETES_KALKE.md` — informações para o site de portfólio.
- `frontend/src/content/ajuda.js` — textos da Ajuda de cada tela.
- `frontend/src/content/novidades.js` — novidades mostradas aos usuários.
- `.github/workflows/reativacao.yml` — rotina que envia a fila de WhatsApp.
