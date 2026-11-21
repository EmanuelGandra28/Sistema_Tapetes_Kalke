// Novidades de cada atualização, da mais nova para a mais antiga. Ao publicar uma atualização, acrescente uma versão NO TOPO desta lista
// (e atualize o NOVIDADES_DO_SISTEMA.md): no primeiro login depois dela, cada pessoa vê o que mudou. `id` forma a marca "novidades:<id>" na conta do usuário.
// Um item com `pagina` ou `acao` só aparece para quem tem essa permissão (ninguém vê novidade de uma tela que não pode abrir).
export const NOVIDADES = [
  {
    id: '2026.11.21', data: '2026-11-21', titulo: 'Relatórios, fotos, mensagens e reativação de clientes',
    itens: [
      { titulo: 'Relatórios com gráficos e frases simples', texto: 'A aba Resumo mostra quanto você vendeu e quanto sobrou, comparando com o período anterior, com gráficos de vendas por dia, forma de pagamento e produtos mais vendidos. Há atalhos de período (hoje, 7 dias, este mês...).', pagina: 'relatorios', rota: '/relatorios' },
      { titulo: 'Curva ABC e margem mais fáceis de entender', texto: '"Produtos que mais dão dinheiro" separa Campeões, Intermediários e Cauda. "Quanto sobra em cada tapete" mostra o lucro de cada produto com cores: Boa, Atenção, Baixa e Prejuízo.', pagina: 'relatorios', rota: '/relatorios' },
      { titulo: 'Excel melhor e impressão em PDF', texto: 'As planilhas saem com a marca da loja, período, filtros e totais. O botão "Imprimir / salvar em PDF" gera um relatório com cabeçalho da loja.', pagina: 'relatorios', rota: '/relatorios' },
      { titulo: 'Fotos nos produtos', texto: 'Em Estoque, o botão "Fotos" anexa até 8 imagens por produto. A foto principal aparece na lista de produtos e nas ordens de produção, para saber qual modelo produzir.', pagina: 'produtos', rota: '/produtos' },
      { titulo: 'Reativação de clientes pelo WhatsApp', texto: 'Monte campanhas para quem está há 2 meses ou mais sem comprar. Você aprova antes de qualquer envio, cada cliente recebe até 3 mensagens e quem responde ou pede para sair para de receber. Veja os resultados e quantos voltaram a comprar.', pagina: 'reativacao', rota: '/reativacao' },
      { titulo: 'Autorização de WhatsApp do cliente', texto: 'Na tela de Clientes, registre que o cliente autorizou receber mensagens. Sem essa autorização, ele nunca recebe campanhas.', acao: 'clientes_consentimento', rota: '/clientes' },
      { titulo: 'Botão de WhatsApp do cliente melhorado', texto: 'Agora o link já vai com o código do Brasil e uma mensagem pronta com o nome do cliente. O botão aparece com 2 meses ou mais sem comprar.', pagina: 'clientes', rota: '/clientes' },
      { titulo: 'Aba Mensagens nas Configurações', texto: 'Edite os textos que o sistema envia aos clientes, com campos automáticos, prévia, histórico e a opção de restaurar o texto padrão.', acao: 'mensagens_gerenciar', rota: '/configuracoes' },
      { titulo: 'Alerta de produção para a dona', texto: 'Quando o estoque de um tapete chega ao mínimo e o sistema abre uma ordem automática, a dona recebe no WhatsApp a lista dos tapetes e até 3 fotos. Ative em Configurações.', acao: 'configuracoes_editar', rota: '/configuracoes' },
      { titulo: 'Editar e excluir despesas', texto: 'Errou um valor? Agora dá para corrigir uma despesa ou excluí-la (com o motivo registrado).', acao: 'despesas_editar', rota: '/despesas' },
      { titulo: 'Ajuda em todas as telas', texto: 'O botão "Ajuda" explica cada tela passo a passo. Na primeira vez que você abre uma tela, um passeio rápido aparece sozinho. Para ver de novo: Configurações → Rever os tutoriais.' },
      { titulo: 'Listas em páginas', texto: 'Vendas, compras, ordens de produção, despesas, clientes e produtos agora aparecem em páginas, para o sistema continuar rápido com o tempo.' }
    ]
  }
];
