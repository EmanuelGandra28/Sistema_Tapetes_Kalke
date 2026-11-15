import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure, Empty } from '../components/State';
import Abas from '../components/Abas';
import { money, today as hoje } from '../utils/format';

const LABELS = { receita: 'Receita', custoMercadoriaVendida: 'Custo da mercadoria vendida', lucroBruto: 'Lucro bruto', salarios: 'Salários', outrasDespesas: 'Outras despesas', lucroLiquido: 'Lucro líquido', compras: 'Compras (caixa)', fluxoCaixa: 'Fluxo de caixa' };
const ABAS = [
  { id: 'financeiro', rotulo: 'Financeiro', url: '/relatorios/financeiro' },
  { id: 'abc', rotulo: 'Curva ABC', url: '/relatorios/curva-abc' },
  { id: 'margem', rotulo: 'Margem por produto', url: '/relatorios/margem-produtos' },
  { id: 'vendedores', rotulo: 'Vendedores', url: '/relatorios/vendas-por-vendedor' }
];
const pct = (v) => `${Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;

function Financeiro({ dados }) {
  return <section className="cards">{Object.entries(dados).map(([key, value]) => <article key={key}><span>{LABELS[key] ?? key}</span><strong>{money(value)}</strong></article>)}</section>;
}

function CurvaAbc({ dados }) {
  if (!dados.length) return <Empty>Nenhuma venda finalizada no período.</Empty>;
  return <section className="panel"><p className="hint">A: produtos que somam até 80% da receita · B: até 95% · C: o restante.</p>
    <div className="table-wrap"><table><thead><tr><th>Classe</th><th>Produto</th><th>Qtd.</th><th>Receita</th><th>% da receita</th><th>% acumulado</th></tr></thead><tbody>
      {dados.map((d) => <tr key={d.produto}><td><span className={`badge abc-${d.classe.toLowerCase()}`}>{d.classe}</span></td><td>{d.produto}</td><td>{d.quantidade}</td><td>{money(d.receita)}</td><td>{pct(d.percentual)}</td><td>{pct(d.acumulado)}</td></tr>)}
    </tbody></table></div></section>;
}

function Margem({ dados }) {
  if (!dados.length) return <Empty>Nenhuma venda finalizada no período.</Empty>;
  const estimado = dados.some((d) => d.custoEstimado);
  return <section className="panel">{estimado && <p className="hint">* Custo estimado com o preço de custo atual: vendas antigas não guardavam o custo do item.</p>}
    <div className="table-wrap"><table><thead><tr><th>Produto</th><th>Qtd.</th><th>Receita</th><th>Custo</th><th>Margem</th><th>Margem %</th></tr></thead><tbody>
      {dados.map((d) => <tr key={d.produto}><td>{d.produto}</td><td>{d.quantidade}</td><td>{money(d.receita)}</td><td>{money(d.custo)}{d.custoEstimado && ' *'}</td>
        <td className={Number(d.margem) < 0 ? 'qtd-saida' : undefined}>{money(d.margem)}</td><td>{pct(d.margemPercentual)}</td></tr>)}
    </tbody></table></div></section>;
}

function Vendedores({ dados }) {
  if (!dados.length) return <Empty>Nenhuma venda finalizada no período.</Empty>;
  return <section className="panel"><p className="hint">O vendedor é o funcionário com o mesmo e-mail do usuário que registrou a venda. Sem esse vínculo, a venda aparece como "Sem vendedor".</p>
    <div className="table-wrap"><table><thead><tr><th>Vendedor</th><th>Vendas</th><th>Total vendido</th><th>Ticket médio</th><th>Descontos dados</th></tr></thead><tbody>
      {dados.map((d) => <tr key={d.vendedor}><td>{d.vendedor}</td><td>{d.vendas}</td><td>{money(d.total)}</td><td>{money(d.ticketMedio)}</td><td>{money(d.descontos)}</td></tr>)}
    </tbody></table></div></section>;
}

const CONTEUDO = { financeiro: Financeiro, abc: CurvaAbc, margem: Margem, vendedores: Vendedores };

export default function Relatorios({ exportar }) {
  const today = hoje();
  const [aba, setAba] = useState('financeiro');
  const [inicio, setInicio] = useState(today.slice(0, 8) + '01');
  const [fim, setFim] = useState(today);
  const atual = ABAS.find((a) => a.id === aba);
  const periodoValido = Boolean(inicio && fim) && inicio <= fim;
  const q = useQuery({ queryKey: ['relatorio', aba, inicio, fim], queryFn: () => api.get(atual.url, { params: { inicio, fim } }), enabled: periodoValido });
  const Conteudo = CONTEUDO[aba];
  return <>
    <section className="page-heading"><div><h1>Relatórios</h1><p>Receitas e despesas usam datas de negócio no fuso America/Sao_Paulo.</p></div></section>
    <Abas abas={ABAS} ativa={aba} onChange={setAba} />
    <section className="panel filters">
      <label>Início<input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} /></label>
      <label>Fim<input type="date" value={fim} onChange={(e) => setFim(e.target.value)} /></label>
      {exportar && <button type="button" className="secondary" disabled={!periodoValido} onClick={() => exportar(`${atual.url}/exportar`, { inicio, fim }, `relatorio-${aba}`)}>Exportar Excel</button>}
    </section>
    {!periodoValido ? <p className="state error">A data inicial deve ser anterior ou igual à data final.</p> : q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : <Conteudo dados={q.data.data} />}
  </>;
}
