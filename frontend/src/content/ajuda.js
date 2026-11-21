// Ajuda de cada tela. Um passo com `acao` só aparece para quem tem aquela permissão (o vendedor não vê "Cancelar venda" se não pode cancelar).
// `rota` liga a ajuda ao endereço da tela; `pagina` é a permissão de página exigida.
export const AJUDA = {
  dashboard: {
    rota: '/dashboard', pagina: 'dashboard', titulo: 'Dashboard',
    resumo: 'A visão do dia: quanto a loja vendeu, o que precisa de atenção no estoque e na produção, e as entregas de hoje. Cada pessoa vê só os blocos do seu cargo.',
    passos: [
      { texto: 'Olhe primeiro os cartões do topo: vendas de hoje, vendas do mês (com a comparação com o mês passado), valor médio por venda e lucro bruto.' },
      { texto: 'Em "Estoque que exige atenção", veja os tapetes baixos ou esgotados. Um aviso de "sem ficha técnica" significa que a produção automática não consegue abrir ordem para aquele tapete.' },
      { texto: 'Em "Entregas de hoje", confira horário, cliente e bairro de cada entrega pendente.' },
      { texto: 'Em "Clientes", veja quantos estão há mais de 2 meses sem comprar: são os candidatos a uma mensagem de volta.' }
    ],
    exemplos: ['Terça de manhã: o dashboard mostra 3 tapetes "Baixo". Você abre Ordens de produção e confere se já existe uma ordem automática para eles.'],
    duvidas: [
      { p: 'Por que não vejo alguns blocos?', r: 'Cada cargo vê só o que precisa. O Dono ajusta isso em Configurações → Permissões.' },
      { p: 'Os números estão zerados.', r: 'Os valores contam vendas finalizadas. Venda cancelada não entra.' }
    ]
  },
  clientes: {
    rota: '/clientes', pagina: 'clientes', titulo: 'Clientes',
    resumo: 'Cadastro de clientes e acompanhamento de quem está sumido. O sistema calcula a situação de cada um pela data da última compra.',
    passos: [
      { texto: 'Use a busca para achar por nome, telefone ou cidade. Ela espera você parar de digitar para consultar.' },
      { texto: 'Clique em "Novo cliente" e preencha ao menos nome e telefone (com DDD).', acao: 'clientes_criar' },
      { texto: 'Veja a etiqueta de cada cliente: Novo (nunca comprou), Ativo, Atenção (1 mês sem comprar) ou Atenção (Inativo) (2 meses ou mais).' },
      { texto: 'Nos clientes com 2 meses ou mais sem comprar, "Chamar no WhatsApp" abre a conversa com a mensagem pronta. O texto é editado em Configurações → Mensagens.' },
      { texto: '"Histórico" mostra todas as compras do cliente.' },
      { texto: 'Use "Editar" para corrigir dados e "Excluir" para tirar da lista; o histórico de vendas é mantido.', acao: 'clientes_editar' }
    ],
    exemplos: ['A Maria não compra há 3 meses: o cartão dela mostra "Atenção (Inativo)". Você clica em "Chamar no WhatsApp", confere o texto e envia.'],
    duvidas: [
      { p: 'O botão de WhatsApp está cinza.', r: 'O cliente está sem telefone válido. Edite o cadastro e informe DDD + número.' },
      { p: 'O botão não aparece.', r: 'Ele só aparece com 2 meses ou mais sem comprar.' }
    ]
  },
  reativacao: {
    rota: '/reativacao', pagina: 'reativacao', titulo: 'Reativação de clientes',
    resumo: 'Traz de volta pelo WhatsApp quem está há 2 meses ou mais sem comprar. Você monta a campanha, aprova, e o sistema envia até 3 mensagens a cada cliente, no horário comercial.',
    passos: [
      { texto: 'Confira o painel do topo: se o envio está ligado, quantas mensagens do plano já foram usadas no mês e se está dentro do horário (8h às 20h).' },
      { texto: 'Em "Quem pode receber" estão os clientes com 2 meses ou mais sem comprar, telefone válido e autorização registrada. Para aparecer, registre a autorização na tela de Clientes.' },
      { texto: 'Clique em "Nova campanha", dê um nome, marque os clientes e escolha os dias entre as mensagens e o máximo por dia.', acao: 'reativacao_gerenciar' },
      { texto: 'A campanha nasce como rascunho. Só depois de "Aprovar e iniciar" as mensagens começam a sair.', acao: 'reativacao_gerenciar' },
      { texto: 'Use "Pausar" ou "Cancelar campanha" a qualquer momento. Quem responder, ou pedir para sair, para de receber.', acao: 'reativacao_gerenciar' },
      { texto: '"Ver resultados" mostra quantos receberam, quantos responderam, o que disseram e quantos voltaram a comprar em até 30 dias.' }
    ],
    exemplos: ['Em novembro há 30 clientes autorizados sem comprar. Você cria a campanha "Volta de novembro", marca os 30, aprova e acompanha os resultados nos dias seguintes.'],
    duvidas: [
      { p: 'Posso mandar para qualquer cliente?', r: 'Não. Só para quem autorizou. A autorização é registrada na tela de Clientes, e o cliente pode pedir para sair respondendo SAIR.' },
      { p: 'Os textos podem ser mudados?', r: 'Sim, em Configurações → Mensagens (as 3 mensagens da reativação).' },
      { p: 'Aprovei e nada foi enviado.', r: 'As mensagens só saem entre 8h e 20h, respeitando o limite por dia e a cota do mês. O painel do topo mostra o motivo.' }
    ]
  },
  produtos: {
    rota: '/produtos', pagina: 'produtos', titulo: 'Estoque e produtos',
    resumo: 'Os tapetes e as matérias-primas. Cada produto tem dois saldos separados: o Comercial (pronto para vender) e o de Produção (matéria-prima para fabricar).',
    passos: [
      { texto: 'Em "Produtos", busque pelo nome ou código de barras. A coluna Status mostra OK, Baixo ou Esgotado.' },
      { texto: 'Cadastre com "Novo produto": nome, tipo, preço de custo e de venda, e o estoque mínimo. O mínimo é a quantidade que dispara o alerta.', acao: 'produtos_criar' },
      { texto: '"Fotos" permite anexar até 8 imagens por produto; a primeira vira a principal e aparece nas listas e nas ordens de produção.', acao: 'produtos_imagens' },
      { texto: 'Para um tapete aparecer no site da loja, edite o produto e, em "Site (portfólio)", escolha "Sim, à venda" (com preço) ou "Só como foto na galeria". Escolha também a linha e coloque fotos no botão "Fotos".', acao: 'produtos_editar' },
      { texto: 'O site se atualiza sozinho a cada 6 horas. Para ver a mudança antes, use "Atualizar site" no topo da tela; ela aparece em 2 a 5 minutos.', acao: 'portfolio_publicar' },
      { texto: '"Ficha técnica" é a receita: quanto de cada matéria-prima vai em uma unidade do tapete. Sem ela, a produção automática não abre ordem.', acao: 'produtos_editar' },
      { texto: '"Ajustar estoque" corrige o saldo (positivo ou negativo) e exige um motivo, que fica registrado.', acao: 'estoque_ajustar' },
      { texto: 'Na aba "Movimentações" veja toda entrada e saída, com saldo antes e depois e quem fez. "Inventário" compara a contagem física com o sistema; "Sugestão de compra" lista o que falta de matéria-prima.' }
    ],
    exemplos: ['Você contou 12 tapetes na prateleira e o sistema diz 14. Em "Ajustar estoque" lance -2 com o motivo "contagem física".'],
    duvidas: [
      { p: 'O saldo pode ficar negativo?', r: 'Não. O sistema recusa a operação que deixaria o saldo abaixo de zero.' },
      { p: 'Qual a diferença entre os dois estoques?', r: 'Comercial é o que está pronto para vender. Produção é a matéria-prima guardada para fabricar.' }
    ]
  },
  vendas: {
    rota: '/vendas', pagina: 'vendas', titulo: 'Vendas',
    resumo: 'Registro das vendas. O servidor recalcula preços e totais, baixa o estoque e atualiza o histórico do cliente sozinho.',
    passos: [
      { texto: 'Escolha o cliente e o produto, informe a quantidade e clique em "Adicionar item". Repita para cada tapete.', acao: 'vendas_criar' },
      { texto: 'Escolha a forma de pagamento, dê desconto se for o caso e confira o total.', acao: 'vendas_criar' },
      { texto: 'Clique em "Finalizar venda". Pagando por PIX, o QR Code aparece na hora.', acao: 'vendas_criar' },
      { texto: 'No histórico, filtre por período e use "Exportar Excel". O histórico é dividido em páginas.' },
      { texto: '"Cancelar venda" devolve o estoque e pede o motivo. Se a entrega já saiu para a rota, o cancelamento é bloqueado.', acao: 'vendas_cancelar' }
    ],
    exemplos: ['Cliente leva 2 tapetes e ganha R$ 20 de desconto: adicione os 2 itens, digite 20 em Desconto e finalize.'],
    duvidas: [
      { p: 'Cliquei duas vezes em Finalizar. Duplicou?', r: 'Não. A mesma tentativa nunca gera duas vendas.' },
      { p: 'Não acho o produto.', r: 'Só aparecem produtos ativos e marcados como "pode ser vendido".' }
    ]
  },
  compras: {
    rota: '/compras', pagina: 'compras', titulo: 'Compras',
    resumo: 'Entrada de mercadoria e matéria-prima. Ao registrar, o estoque aumenta.',
    passos: [
      { texto: 'A lista mostra cada compra com data, fornecedor, total e situação (Finalizada ou Cancelada). Ela é dividida em páginas.' },
      { texto: 'Clique em "Nova compra", escolha o fornecedor (ou digite um nome) e a data.', acao: 'compras_criar' },
      { texto: 'Adicione os itens, o destino do estoque (Comercial ou Produção), a quantidade e o preço unitário.', acao: 'compras_criar' },
      { texto: 'Para desfazer, use "Cancelar compra" com o motivo. Se o estoque já foi usado, o cancelamento é bloqueado.', acao: 'compras_cancelar' }
    ],
    exemplos: ['Chegaram 50 kg de malha: registre o item com destino "Produção" para o saldo de matéria-prima subir.'],
    duvidas: [{ p: 'A compra entra no custo do tapete?', r: 'O custo usado nos relatórios vem do preço de custo do produto. Atualize o custo quando o fornecedor mudar o preço.' }]
  },
  fornecedores: {
    rota: '/fornecedores', pagina: 'fornecedores', titulo: 'Fornecedores',
    resumo: 'Cadastro das empresas de quem a loja compra.',
    passos: [
      { texto: 'Cadastre empresa, CNPJ, telefone e e-mail.' },
      { texto: 'O fornecedor cadastrado pode ser escolhido na tela de Compras e no cadastro do produto.' }
    ],
    exemplos: [], duvidas: []
  },
  funcionarios: {
    rota: '/funcionarios', pagina: 'funcionarios', titulo: 'Funcionários',
    resumo: 'Cadastro da equipe: cargo, situação, contato e salário (o salário só aparece para Dono e Gerente).',
    passos: [
      { texto: 'Cadastre cada pessoa com cargo e data de contratação.', acao: 'funcionarios_criar' },
      { texto: 'Para o entregador ver as próprias entregas, o e-mail do cadastro de funcionário deve ser o mesmo do usuário dele.' },
      { texto: 'Marque Férias ou Inativo em vez de excluir, para manter o histórico.', acao: 'funcionarios_editar' }
    ],
    exemplos: [], duvidas: [{ p: 'Funcionário é a mesma coisa que usuário?', r: 'Não. Usuário é quem entra no sistema (Configurações → Usuários); funcionário é o cadastro da pessoa na loja. Ligam-se pelo e-mail.' }]
  },
  'ordens-producao': {
    rota: '/ordens-producao', pagina: 'producao', titulo: 'Ordens de produção',
    resumo: 'Controle do que está sendo fabricado. Ao finalizar uma ordem, a matéria-prima sai do estoque de produção e o tapete pronto entra no estoque comercial.',
    passos: [
      { texto: 'Clique em "Nova ordem", escolha o tapete e a quantidade. As matérias-primas vêm da ficha técnica.', acao: 'producao_criar' },
      { texto: 'Cada ordem mostra a foto do tapete para a equipe saber qual modelo produzir.' },
      { texto: 'Ordens com a etiqueta "Automática" foram abertas pelo sistema quando o estoque chegou ao mínimo.' },
      { texto: 'Quando o tapete ficar pronto, clique em "Finalizar".', acao: 'producao_finalizar' },
      { texto: '"Cancelar ordem" só funciona em ordens ainda abertas.', acao: 'producao_cancelar' }
    ],
    exemplos: ['O estoque do "Tapete Floral" caiu ao mínimo: surge uma ordem automática. Quando a produção terminar, finalize para o saldo voltar.'],
    duvidas: [{ p: 'Por que não abriu ordem automática?', r: 'Ou o produto está sem ficha técnica, ou já existe uma ordem aberta para ele.' }]
  },
  entregas: {
    rota: '/entregas', pagina: 'entregas', titulo: 'Entregas',
    resumo: 'Agendamento e acompanhamento das entregas. Cada venda finalizada pode ter uma entrega.',
    passos: [
      { texto: 'Clique em "Agendar entrega", escolha a venda, a data, o horário, o entregador e o frete.', acao: 'entregas_criar' },
      { texto: 'Se deixar o endereço em branco, vale o do cliente.' },
      { texto: 'Atualize o andamento: Agendada → Em rota → Entregue, ou "Não entregue" para reagendar.', acao: 'entregas_atualizar_status' },
      { texto: 'O entregador vê apenas as entregas dele.' }
    ],
    exemplos: [], duvidas: [{ p: 'Cancelei a venda. E a entrega?', r: 'A entrega pendente é cancelada junto. Se já estiver em rota, o cancelamento da venda é bloqueado.' }]
  },
  despesas: {
    rota: '/despesas', pagina: 'despesas', titulo: 'Despesas',
    resumo: 'Salários, fretes, manutenção e outros gastos. Entram nas linhas de despesa dos relatórios.',
    passos: [
      { texto: 'A lista mostra as despesas lançadas, da mais recente para a mais antiga, com tipo, descrição e valor.' },
      { texto: 'Clique em "Nova despesa", escolha o tipo, a data e o valor.', acao: 'despesas_criar' },
      { texto: 'Errou? "Editar despesa" corrige os dados.', acao: 'despesas_editar' },
      { texto: '"Excluir despesa" pede o motivo: ela some das listas e dos relatórios, mas fica registrada com quem excluiu.', acao: 'despesas_excluir' }
    ],
    exemplos: ['Lançou R$ 1.200 em vez de R$ 120: use "Editar despesa" e corrija o valor; o relatório se ajusta sozinho.'],
    duvidas: []
  },
  relatorios: {
    rota: '/relatorios', pagina: 'relatorios', titulo: 'Relatórios',
    resumo: 'Como a loja está indo, em gráficos e em frases simples, para o período que você escolher.',
    passos: [
      { texto: 'Escolha o período pelos atalhos (Hoje, 7 dias, Este mês...) ou pelas datas.' },
      { texto: 'A aba "Resumo" compara o período com o anterior: setas verdes melhoram, vermelhas pioram.' },
      { texto: '"Produtos que mais dão dinheiro" separa os Campeões, os Intermediários e a Cauda.' },
      { texto: '"Quanto sobra em cada tapete" mostra o lucro de cada produto; Boa = 30% ou mais, Atenção = 15 a 30%, Baixa = abaixo, Prejuízo = negativa.' },
      { texto: '"Exportar Excel" baixa a planilha; "Imprimir / salvar em PDF" abre a impressão com o cabeçalho da loja (escolha "Salvar como PDF").' }
    ],
    exemplos: [], duvidas: [{ p: 'O que é o período anterior?', r: 'O mesmo número de dias logo antes do período escolhido. Se você olha 10 dias, compara com os 10 anteriores.' }]
  },
  configuracoes: {
    rota: '/configuracoes', pagina: 'configuracoes', titulo: 'Configurações',
    resumo: 'Sua senha, o tema visual e, para quem tem permissão, usuários, permissões, dados da loja e mensagens automáticas.',
    passos: [
      { texto: 'Em "Minha senha" você troca a sua senha. Os outros aparelhos conectados saem da conta.' },
      { texto: 'Escolha o tema que for mais confortável; ele fica salvo na sua conta.' },
      { texto: 'Na aba "Mensagens", edite os textos enviados aos clientes. Use os botões de campos e confira a prévia antes de salvar.', acao: 'mensagens_gerenciar' },
      { texto: 'Em "Alerta de produção por WhatsApp", informe o número da dona e ligue o aviso: quando o sistema abrir uma ordem automática, ela recebe a lista dos tapetes e as fotos.', acao: 'configuracoes_editar' },
      { texto: 'Em "Permissões", o Dono define o que cada cargo pode ver e fazer.', acao: 'configuracoes_editar' }
    ],
    exemplos: [], duvidas: [{ p: 'Como rever os tutoriais?', r: 'Em Configurações → Geral, use o botão "Rever os tutoriais das telas".' }]
  }
};

// Dicas curtas ao lado de campos difíceis.
export const DICAS = {
  estoqueMinimo: 'Quando o saldo chegar a este número (ou menos), o produto aparece como "Baixo" e, se tiver ficha técnica, o sistema abre uma ordem de produção.',
  fichaTecnica: 'É a receita do tapete: quanto de cada matéria-prima entra em 1 unidade. Com ela, a ordem de produção já vem preenchida.'
};
