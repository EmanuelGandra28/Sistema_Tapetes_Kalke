// Valores iniciais e conversões do formulário de produto (inclui os campos do portfólio/site).
export const produtoDefaults = {
  nome: '', tipo: 'TAPETE', categoria: '', cor: '', tamanho: '', unidadeMedida: 'UNIDADE', podeSerVendido: true, podeSerMateriaPrima: false,
  estoqueMinimoComercial: 1, estoqueMinimoProducao: 1, precoCusto: 0, precoVenda: 0, codigoBarras: '', fornecedorPrincipalId: '', observacoes: '', ativo: true,
  portfolioModo: 'NENHUM', portfolioLinha: '', portfolioDescricao: '', portfolioOrdem: 0, portfolioSelo: '',
};

// Produtos antigos não têm os campos novos: caem nos valores padrão (fora do site).
export const produtoParaForm = (p) => Object.fromEntries(Object.keys(produtoDefaults).map((k) => [k, p[k] ?? produtoDefaults[k]]));

const nuloSeVazio = (v) => (String(v ?? '').trim() === '' ? null : String(v).trim());

export const produtoParaPayload = (d) => ({
  ...d,
  estoqueMinimoComercial: Number(d.estoqueMinimoComercial),
  estoqueMinimoProducao: Number(d.estoqueMinimoProducao),
  precoCusto: Number(d.precoCusto),
  precoVenda: Number(d.precoVenda),
  portfolioOrdem: Number(d.portfolioOrdem) || 0,
  portfolioLinha: nuloSeVazio(d.portfolioLinha),
  portfolioDescricao: nuloSeVazio(d.portfolioDescricao),
  portfolioSelo: nuloSeVazio(d.portfolioSelo),
});
