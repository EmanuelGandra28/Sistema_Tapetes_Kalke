// Regras de apresentação dos relatórios, separadas da tela para poderem ser testadas.

const pad = (n) => String(n).padStart(2, '0');
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const dataLocal = (texto) => { const [a, m, d] = texto.split('-').map(Number); return new Date(a, m - 1, d); };
const somarDias = (texto, dias) => { const d = dataLocal(texto); d.setDate(d.getDate() + dias); return iso(d); };

// Atalhos de período a partir de "hoje" (aaaa-mm-dd): devolvem { id, rotulo, inicio, fim }.
export function atalhosPeriodo(hoje) {
  const [ano, mes] = hoje.split('-').map(Number);
  const mesPassado = new Date(ano, mes - 2, 1);
  const fimMesPassado = new Date(ano, mes - 1, 0);
  return [
    { id: 'hoje', rotulo: 'Hoje', inicio: hoje, fim: hoje },
    { id: '7dias', rotulo: 'Últimos 7 dias', inicio: somarDias(hoje, -6), fim: hoje },
    { id: 'mes', rotulo: 'Este mês', inicio: `${ano}-${pad(mes)}-01`, fim: hoje },
    { id: 'mespassado', rotulo: 'Mês passado', inicio: iso(mesPassado), fim: iso(fimMesPassado) },
    { id: 'ano', rotulo: 'Este ano', inicio: `${ano}-01-01`, fim: hoje }
  ];
}

// Compara o valor atual com o do período anterior: { sentido: 'subiu' | 'caiu' | 'igual' | 'novo', pct }.
export function variacao(atual, anterior) {
  const a = Number(atual) || 0;
  const b = Number(anterior) || 0;
  if (b === 0) return a === 0 ? { sentido: 'igual', pct: 0 } : { sentido: 'novo', pct: null };
  const pct = ((a - b) / Math.abs(b)) * 100;
  return { sentido: Math.abs(pct) < 0.05 ? 'igual' : pct > 0 ? 'subiu' : 'caiu', pct };
}

// Curva ABC em linguagem simples: quantos produtos há em cada grupo e quanto da receita cada grupo representa.
export const GRUPOS_ABC = {
  A: { nome: 'Campeões', explicacao: 'poucos produtos que trazem a maior parte do dinheiro' },
  B: { nome: 'Intermediários', explicacao: 'produtos de venda regular' },
  C: { nome: 'Cauda', explicacao: 'muitos produtos que vendem pouco' }
};

export function resumirAbc(itens) {
  const total = itens.reduce((s, i) => s + Number(i.receita), 0);
  return ['A', 'B', 'C'].map((classe) => {
    const doGrupo = itens.filter((i) => i.classe === classe);
    const receita = doGrupo.reduce((s, i) => s + Number(i.receita), 0);
    return { classe, ...GRUPOS_ABC[classe], produtos: doGrupo.length, receita, percentual: total === 0 ? 0 : (receita / total) * 100 };
  });
}

// Faixas de margem: quanto sobra de cada R$ 100 vendidos.
export const FAIXAS_MARGEM = { boa: 30, atencao: 15 };
export function faixaMargem(percentual) {
  const p = Number(percentual);
  if (p < 0) return { id: 'prejuizo', rotulo: 'Prejuízo', dica: 'Está vendendo abaixo do custo' };
  if (p >= FAIXAS_MARGEM.boa) return { id: 'boa', rotulo: 'Boa', dica: 'Sobra bem em cada venda' };
  if (p >= FAIXAS_MARGEM.atencao) return { id: 'atencao', rotulo: 'Atenção', dica: 'Sobra pouco; vale rever o preço ou o custo' };
  return { id: 'baixa', rotulo: 'Baixa', dica: 'Sobra muito pouco em cada venda' };
}
