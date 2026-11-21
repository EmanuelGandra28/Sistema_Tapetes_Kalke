import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { useCodigos } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Abas from '../components/Abas';
import Logo from '../components/Logo';
import { GraficoBarras, GraficoLinha, GraficoRosca } from '../components/Graficos';
import { useExportar } from '../hooks/useExportar';
import { date, money, today as hoje } from '../utils/format';
import { atalhosPeriodo, faixaMargem, FAIXAS_MARGEM, resumirAbc, variacao } from '../utils/relatorios';

const ABAS = [
  { id: 'resumo', rotulo: 'Resumo', url: '/relatorios/resumo', exportar: '/relatorios/financeiro' },
  { id: 'financeiro', rotulo: 'Financeiro', url: '/relatorios/financeiro', exportar: '/relatorios/financeiro' },
  { id: 'abc', rotulo: 'Produtos que mais dão dinheiro', url: '/relatorios/curva-abc', exportar: '/relatorios/curva-abc' },
  { id: 'margem', rotulo: 'Quanto sobra em cada tapete', url: '/relatorios/margem-produtos', exportar: '/relatorios/margem-produtos' },
  { id: 'vendedores', rotulo: 'Vendedores', url: '/relatorios/vendas-por-vendedor', exportar: '/relatorios/vendas-por-vendedor' }
];
const LABELS = { receita: 'Receita', custoMercadoriaVendida: 'Custo da mercadoria vendida', lucroBruto: 'Lucro bruto', salarios: 'Salários', outrasDespesas: 'Outras despesas', lucroLiquido: 'Lucro líquido', compras: 'Compras (caixa)', fluxoCaixa: 'Fluxo de caixa' };
const pct = (v) => `${Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;
const diaMes = (d) => `${d.slice(8, 10)}/${d.slice(5, 7)}`;
const nomeProduto = (nome) => (nome.length > 34 ? `${nome.slice(0, 33)}…` : nome);

// "Subiu 12% sobre o período anterior": verde quando melhora e vermelho quando piora (nas despesas é o contrário).
function Variacao({ atual, anterior, menorEMelhor = false }) {
  const v = variacao(atual, anterior);
  if (v.sentido === 'novo') return <small className="variacao neutra">Sem vendas no período anterior</small>;
  if (v.sentido === 'igual') return <small className="variacao neutra">Igual ao período anterior</small>;
  const bom = (v.sentido === 'subiu') !== menorEMelhor;
  return <small className={`variacao ${bom ? 'boa' : 'ruim'}`}>{v.sentido === 'subiu' ? '▲' : '▼'} {pct(Math.abs(v.pct))} {v.sentido === 'subiu' ? 'a mais' : 'a menos'} que o período anterior</small>;
}

function Resumo({ dados, nomeForma }) {
  const { atual, anterior } = dados;
  const periodo = `${date(dados.inicio)} a ${date(dados.fim)}`;
  return <>
    <p className="resumo-frase">De <strong>{periodo}</strong> você vendeu <strong>{money(atual.receita)}</strong> em <strong>{atual.vendas}</strong> {atual.vendas === 1 ? 'venda' : 'vendas'}
      {' '}e, depois de pagar o custo dos tapetes e as despesas, {Number(atual.lucroLiquido) >= 0 ? <>sobraram <strong>{money(atual.lucroLiquido)}</strong> de lucro.</> : <>faltaram <strong className="qtd-saida">{money(Math.abs(atual.lucroLiquido))}</strong> (prejuízo).</>}</p>
    <section className="cards">
      <article><span>Você vendeu</span><strong>{money(atual.receita)}</strong><Variacao atual={atual.receita} anterior={anterior.receita} /></article>
      <article><span>Sobrou de lucro</span><strong className={Number(atual.lucroLiquido) < 0 ? 'qtd-saida' : undefined}>{money(atual.lucroLiquido)}</strong><Variacao atual={atual.lucroLiquido} anterior={anterior.lucroLiquido} /></article>
      <article><span>Vendas realizadas</span><strong>{atual.vendas}</strong><Variacao atual={atual.vendas} anterior={anterior.vendas} /></article>
      <article><span>Valor médio por venda</span><strong>{money(atual.ticketMedio)}</strong><Variacao atual={atual.ticketMedio} anterior={anterior.ticketMedio} /></article>
    </section>
    <section className="panel"><h2>Vendas dia a dia</h2>
      <GraficoLinha titulo="Vendas por dia no período" dados={dados.porDia.map((d) => ({ rotulo: diaMes(d.data), valor: d.total }))} formato={money} /></section>
    <div className="grid-2">
      <section className="panel"><h2>Para onde foi o dinheiro</h2>
        <GraficoBarras formato={money} dados={[
          { rotulo: 'Vendas', valor: atual.receita, cor: 'var(--primary)' },
          { rotulo: 'Despesas', valor: atual.despesas, cor: 'var(--warning)' },
          { rotulo: Number(atual.lucroLiquido) < 0 ? 'Prejuízo' : 'Lucro', valor: atual.lucroLiquido, cor: Number(atual.lucroLiquido) < 0 ? 'var(--danger)' : 'var(--success)' }]} />
        <p className="hint">Despesas = salários e outras despesas lançadas. O lucro já desconta também o custo dos tapetes vendidos.</p></section>
      <section className="panel"><h2>Como os clientes pagaram</h2>
        <GraficoRosca formato={money} dados={dados.porForma.map((f) => ({ rotulo: nomeForma(f.forma), valor: f.total }))} /></section>
    </div>
    <section className="panel"><h2>Os 5 tapetes que mais venderam</h2>
      <GraficoBarras formato={money} dados={dados.maisVendidos.map((p) => ({ rotulo: nomeProduto(p.produto), valor: p.receita }))} vazio="Nenhuma venda finalizada no período." /></section>
  </>;
}

function Financeiro({ dados }) {
  return <>
    <section className="cards">{Object.entries(dados).map(([key, value]) => <article key={key}><span>{LABELS[key] ?? key}</span><strong className={Number(value) < 0 ? 'qtd-saida' : undefined}>{money(value)}</strong></article>)}</section>
    <section className="panel"><h2>Do que entrou ao que sobrou</h2>
      <GraficoBarras formato={money} dados={[
        { rotulo: 'Receita', valor: dados.receita, cor: 'var(--primary)' },
        { rotulo: 'Custo dos tapetes vendidos', valor: dados.custoMercadoriaVendida, cor: 'var(--warning)' },
        { rotulo: 'Salários', valor: dados.salarios, cor: 'var(--accent)' },
        { rotulo: 'Outras despesas', valor: dados.outrasDespesas, cor: 'var(--accent)' },
        { rotulo: 'Lucro líquido', valor: dados.lucroLiquido, cor: Number(dados.lucroLiquido) < 0 ? 'var(--danger)' : 'var(--success)' }]} />
      <p className="hint">Compras e fluxo de caixa ficam à parte: compra de mercadoria não é custo até o tapete ser vendido.</p></section>
  </>;
}

function CurvaAbc({ dados }) {
  if (!dados.length) return <Empty>Nenhuma venda finalizada no período.</Empty>;
  const grupos = resumirAbc(dados);
  const topo = dados.slice(0, 10);
  return <>
    <details className="ajuda-inline"><summary>O que é isso?</summary>
      <p>Mostra quais tapetes sustentam as suas vendas. Em quase toda loja, poucos produtos trazem a maior parte do dinheiro. Os <strong>Campeões</strong> merecem estoque sempre cheio; a <strong>Cauda</strong> vende pouco e pode ser revista (promoção, menos estoque ou sair de linha).</p></details>
    <section className="cards">{grupos.map((g) => <article key={g.classe} className={`grupo-abc abc-${g.classe.toLowerCase()}`}>
      <span><b className="selo">{g.classe}</b> {g.nome}</span>
      <strong>{g.produtos} {g.produtos === 1 ? 'produto' : 'produtos'}</strong>
      <small>{pct(g.percentual)} do que você vendeu — {g.explicacao}</small></article>)}</section>
    <section className="panel"><h2>Os 10 que mais trazem dinheiro</h2>
      <GraficoBarras formato={money} dados={topo.map((d) => ({ rotulo: nomeProduto(d.produto), valor: d.receita, cor: d.classe === 'A' ? 'var(--success)' : d.classe === 'B' ? 'var(--accent)' : 'var(--text-muted)' }))} />
      <p className="hint"><span className="cor-legenda" style={{ background: 'var(--success)' }} /> Campeões <span className="cor-legenda" style={{ background: 'var(--accent)' }} /> Intermediários <span className="cor-legenda" style={{ background: 'var(--text-muted)' }} /> Cauda</p></section>
    <section className="panel"><h2>Lista completa</h2>
      <div className="table-wrap"><table><thead><tr><th>Grupo</th><th>Produto</th><th>Quantidade vendida</th><th>Valor vendido</th><th>Parte das vendas</th><th>Soma até aqui</th></tr></thead><tbody>
        {dados.map((d) => <tr key={d.produto}><td><span className={`badge abc-${d.classe.toLowerCase()}`}>{d.classe} · {resumirAbc([d])[['A', 'B', 'C'].indexOf(d.classe)].nome}</span></td><td>{d.produto}</td><td>{d.quantidade}</td><td>{money(d.receita)}</td><td>{pct(d.percentual)}</td><td>{pct(d.acumulado)}</td></tr>)}
      </tbody></table></div></section>
  </>;
}

function Margem({ dados }) {
  const [ordem, setOrdem] = useState({ chave: 'margemPercentual', crescente: false });
  const ordenados = useMemo(() => [...dados].sort((a, b) => (Number(a[ordem.chave]) - Number(b[ordem.chave])) * (ordem.crescente ? 1 : -1)), [dados, ordem]);
  if (!dados.length) return <Empty>Nenhuma venda finalizada no período.</Empty>;
  const estimado = dados.some((d) => d.custoEstimado);
  const piores = [...dados].sort((a, b) => Number(a.margemPercentual) - Number(b.margemPercentual)).slice(0, 3);
  const th = (chave, rotulo) => <th><button type="button" className="link ordenar" onClick={() => setOrdem({ chave, crescente: ordem.chave === chave ? !ordem.crescente : false })}>{rotulo}{ordem.chave === chave ? (ordem.crescente ? ' ▲' : ' ▼') : ''}</button></th>;
  return <>
    <details className="ajuda-inline"><summary>O que é isso?</summary>
      <p>Para cada tapete, mostra quanto sobra depois de descontar o que ele custou. <strong>Sobra em cada R$ 100</strong> é a margem: {FAIXAS_MARGEM.boa}% ou mais é <strong>boa</strong>, de {FAIXAS_MARGEM.atencao}% a {FAIXAS_MARGEM.boa}% pede <strong>atenção</strong>, abaixo disso é <strong>baixa</strong> e negativa é <strong>prejuízo</strong>.</p></details>
    <p className="resumo-frase">Os que menos deixam lucro: {piores.map((p, i) => <span key={p.produto}>{i > 0 && ', '}<strong>{p.produto}</strong> ({pct(p.margemPercentual)})</span>)}.</p>
    <section className="panel">{estimado && <p className="hint">* Custo estimado com o preço de custo atual: vendas antigas não guardavam o custo do item.</p>}
      <div className="table-wrap"><table><thead><tr>{th('produto', 'Produto')}{th('quantidade', 'Qtd.')}{th('receita', 'Vendido')}{th('custo', 'Custo')}{th('margem', 'Sobrou (R$)')}{th('margemPercentual', 'Sobra em cada R$ 100')}<th>Situação</th></tr></thead><tbody>
        {ordenados.map((d) => { const f = faixaMargem(d.margemPercentual); return <tr key={d.produto}><td>{d.produto}</td><td>{d.quantidade}</td><td>{money(d.receita)}</td><td>{money(d.custo)}{d.custoEstimado && ' *'}</td>
          <td className={Number(d.margem) < 0 ? 'qtd-saida' : undefined}>{money(d.margem)}</td><td>{money(d.margemPercentual)}</td><td><span className={`badge faixa-${f.id}`} title={f.dica}>{f.rotulo}</span></td></tr>; })}
      </tbody></table></div></section>
  </>;
}

function Vendedores({ dados }) {
  if (!dados.length) return <Empty>Nenhuma venda finalizada no período.</Empty>;
  return <>
    <section className="panel"><h2>Quanto cada vendedor vendeu</h2><GraficoBarras formato={money} dados={dados.map((d) => ({ rotulo: d.vendedor, valor: d.total }))} /></section>
    <section className="panel"><p className="hint">O vendedor é o funcionário com o mesmo e-mail do usuário que registrou a venda. Sem esse vínculo, a venda aparece como "Sem vendedor".</p>
      <div className="table-wrap"><table><thead><tr><th>Vendedor</th><th>Vendas</th><th>Total vendido</th><th>Valor médio por venda</th><th>Descontos dados</th></tr></thead><tbody>
        {dados.map((d) => <tr key={d.vendedor}><td>{d.vendedor}</td><td>{d.vendas}</td><td>{money(d.total)}</td><td>{money(d.ticketMedio)}</td><td>{money(d.descontos)}</td></tr>)}
      </tbody></table></div></section>
  </>;
}

const CONTEUDO = { resumo: Resumo, financeiro: Financeiro, abc: CurvaAbc, margem: Margem, vendedores: Vendedores };

export default function Relatorios() {
  const exportar = useExportar();
  const { formasPagamento } = useCodigos();
  const hojeData = hoje();
  const atalhos = useMemo(() => atalhosPeriodo(hojeData), [hojeData]);
  const [aba, setAba] = useState('resumo');
  const [inicio, setInicio] = useState(atalhos[2].inicio);
  const [fim, setFim] = useState(hojeData);
  const atual = ABAS.find((a) => a.id === aba);
  const periodoValido = Boolean(inicio && fim) && inicio <= fim;
  const q = useQuery({ queryKey: ['relatorio', aba, inicio, fim], queryFn: () => api.get(atual.url, { params: { inicio, fim } }), enabled: periodoValido });
  const nomeForma = (codigo) => formasPagamento.find((f) => f.codigo === codigo)?.nome ?? codigo;
  const Conteudo = CONTEUDO[aba];
  return <>
    <header className="print-header"><Logo variante="login" tamanho={48} /><div><h1>{atual.rotulo}</h1><p>Período: {date(inicio)} a {date(fim)} · emitido em {date(hojeData)}</p></div></header>
    <section className="page-heading no-print"><div><h1>Relatórios</h1><p>Escolha o período e veja como a loja está indo. Receitas e despesas usam datas de negócio no fuso America/Sao_Paulo.</p></div>
      <button type="button" className="secondary" disabled={!periodoValido || q.isPending} onClick={() => window.print()}>Imprimir / salvar em PDF</button></section>
    <div className="no-print"><Abas abas={ABAS} ativa={aba} onChange={setAba} /></div>
    <section className="panel filters no-print">
      <div className="atalhos" role="group" aria-label="Períodos prontos">{atalhos.map((a) => <button type="button" key={a.id} className={`chip ${a.inicio === inicio && a.fim === fim ? 'ativo' : ''}`} onClick={() => { setInicio(a.inicio); setFim(a.fim); }}>{a.rotulo}</button>)}</div>
      <label>Início<input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} /></label>
      <label>Fim<input type="date" value={fim} onChange={(e) => setFim(e.target.value)} /></label>
      {exportar && <button type="button" className="secondary" disabled={!periodoValido} onClick={() => exportar(`${atual.exportar}/exportar`, { inicio, fim }, `relatorio-${aba === 'resumo' ? 'financeiro' : aba}`)}>Exportar Excel</button>}
    </section>
    {!periodoValido ? <p className="state error">A data inicial deve ser anterior ou igual à data final.</p> : q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : <Conteudo dados={q.data.data} nomeForma={nomeForma} />}
  </>;
}
