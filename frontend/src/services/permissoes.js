// Nomes legíveis para as chaves de permissão da API. As chaves enviadas ao servidor não mudam.
const GRUPOS = ['Clientes', 'Estoque', 'Vendas', 'Compras', 'Produção', 'Entregas', 'Financeiro', 'Equipe', 'Sistema', 'Outros'];

const ROTULOS = {
  // Páginas
  dashboard: ['Tela inicial (Dashboard)', 'Sistema'],
  clientes: ['Tela de Clientes', 'Clientes'],
  funcionarios: ['Tela de Funcionários', 'Equipe'],
  produtos: ['Tela de Estoque', 'Estoque'],
  fornecedores: ['Tela de Fornecedores', 'Compras'],
  vendas: ['Tela de Vendas', 'Vendas'],
  compras: ['Tela de Compras', 'Compras'],
  producao: ['Tela de Produção', 'Produção'],
  entregas: ['Tela de Entregas', 'Entregas'],
  despesas: ['Tela de Despesas', 'Financeiro'],
  relatorios: ['Tela de Relatórios', 'Financeiro'],
  reativacao: ['Tela de Reativação de clientes', 'Clientes'],
  configuracoes: ['Tela de Configurações', 'Sistema'],
  // Ações
  clientes_criar: ['Cadastrar clientes', 'Clientes'],
  clientes_editar: ['Editar clientes', 'Clientes'],
  clientes_excluir: ['Excluir clientes', 'Clientes'],
  clientes_consentimento: ['Registrar a autorização de WhatsApp do cliente', 'Clientes'],
  reativacao_gerenciar: ['Criar, aprovar e cancelar campanhas de reativação', 'Clientes'],
  produtos_criar: ['Cadastrar produtos e fornecedores', 'Estoque'],
  produtos_editar: ['Editar produtos e fornecedores', 'Estoque'],
  produtos_excluir: ['Excluir produtos e fornecedores', 'Estoque'],
  produtos_imagens: ['Anexar e remover fotos dos produtos', 'Estoque'],
  portfolio_publicar: ['Atualizar o site de portfólio', 'Estoque'],
  mensagens_gerenciar: ['Editar as mensagens automáticas para clientes', 'Sistema'],
  estoque_ajustar: ['Ajustar estoque e fazer inventário', 'Estoque'],
  vendas_criar: ['Registrar vendas', 'Vendas'],
  vendas_cancelar: ['Cancelar vendas', 'Vendas'],
  compras_criar: ['Registrar compras', 'Compras'],
  compras_cancelar: ['Cancelar compras', 'Compras'],
  producao_criar: ['Abrir ordens de produção', 'Produção'],
  producao_finalizar: ['Finalizar ordens de produção', 'Produção'],
  producao_cancelar: ['Cancelar ordens de produção', 'Produção'],
  entregas_criar: ['Agendar entregas', 'Entregas'],
  entregas_atualizar_status: ['Atualizar status das entregas', 'Entregas'],
  despesas_criar: ['Lançar despesas', 'Financeiro'],
  despesas_editar: ['Editar despesas', 'Financeiro'],
  despesas_excluir: ['Excluir despesas', 'Financeiro'],
  relatorios_visualizar: ['Ver relatórios e exportar', 'Financeiro'],
  funcionarios_criar: ['Cadastrar funcionários', 'Equipe'],
  funcionarios_editar: ['Editar funcionários', 'Equipe'],
  funcionarios_excluir: ['Excluir funcionários', 'Equipe'],
  configuracoes_editar: ['Alterar configurações e permissões', 'Sistema']
};

export const CARGO_ROTULO = { ADMINISTRADOR: 'Administrador', DONO: 'Dono', GERENTE: 'Gerente', VENDEDOR: 'Vendedor', ESTOQUISTA: 'Estoquista', ENTREGADOR: 'Entregador' };

export function rotuloPermissao(chave) {
  const conhecido = ROTULOS[chave];
  return conhecido ? { rotulo: conhecido[0], grupo: conhecido[1] } : { rotulo: chave, grupo: 'Outros' };
}

export function agruparPermissoes(chaves) {
  const porGrupo = new Map(GRUPOS.map((g) => [g, []]));
  chaves.forEach((chave) => porGrupo.get(rotuloPermissao(chave).grupo).push(chave));
  return GRUPOS.filter((g) => porGrupo.get(g).length).map((grupo) => ({ grupo, itens: porGrupo.get(grupo) }));
}

// Dono e Administrador gerenciam usuários e permissões (o Administrador tem tudo o que o Dono tem).
export const ehGestor = (cargo) => cargo === 'DONO' || cargo === 'ADMINISTRADOR';
