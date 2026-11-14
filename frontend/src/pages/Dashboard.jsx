import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure, Empty } from '../components/State';
import { useAuth } from '../contexts/AuthContext';
import { date, money } from '../utils/format';

const STATUS_ENTREGA = { AGENDADA: 'Agendada', EM_ROTA: 'Em rota' };
const diaCurto = (iso) => `${iso.slice(8, 10)}/${iso.slice(5, 7)}`;

function Variacao({ valor }) {
  if (valor === null || valor === undefined) return <small className="muted">sem dados do mês anterior</small>;
  const n = Number(valor);
  return <small className={n >= 0 ? 'trend up' : 'trend down'}>{n >= 0 ? '▲' : '▼'} {Math.abs(n).toLocaleString('pt-BR')}% vs. mesmo período do mês anterior</small>;
}

// Gráfico de barras simples (sem biblioteca): altura proporcional ao maior dia, total no title de cada barra.
function GraficoVendas({ dias }) {
  const maior = Math.max(...dias.map((d) => Number(d.total)), 0);
  const total = dias.reduce((s, d) => s + Number(d.total), 0);
  return <section className="panel"><h2>Vendas dos últimos 30 dias</h2>
    <p className="muted">Total no período: <strong>{money(total)}</strong></p>
    {maior === 0 ? <Empty children="Nenhuma venda nos últimos 30 dias." /> :
      <div className="bar-chart" role="img" aria-label={`Vendas por dia nos últimos 30 dias, total ${money(total)}`}>
        {dias.map((d) => <div key={d.data} className="bar-col" title={`${date(d.data)}: ${money(d.total)}`}>
          <div className="bar" style={{ height: `${Math.max(Number(d.total) / maior * 100, Number(d.total) > 0 ? 3 : 0)}%` }} />
        </div>)}
      </div>}
    {maior > 0 && <div className="bar-axis"><span>{diaCurto(dias[0].data)}</span><span>{diaCurto(dias[Math.floor(dias.length / 2)].data)}</span><span>Hoje</span></div>}
  </section>;
}

function MaisVendidos({ itens }) {
  const maior = Math.max(...itens.map((i) => Number(i.total)), 0);
  return <section className="panel"><h2>Mais vendidos no mês</h2>
    {itens.length === 0 ? <Empty children="Nenhuma venda neste mês." /> :
      <ul className="rank">{itens.map((i, idx) => <li key={i.produto}>
        <div><strong>{idx + 1}. {i.produto}</strong><span className="muted">{Number(i.quantidade).toLocaleString('pt-BR')} un. · {money(i.total)}</span></div>
        <div className="rank-bar"><span style={{ width: `${maior ? Number(i.total) / maior * 100 : 0}%` }} /></div>
      </li>)}</ul>}
  </section>;
}

export default function Dashboard() {
  const { user } = useAuth();
  const q = useQuery({ queryKey: ['dashboard'], queryFn: () => api.get('/dashboard') });
  if (q.isPending) return <Loading />;
  if (q.isError) return <Failure error={q.error} />;
  const d = q.data.data;
  const v = d.vendas;
  const baixos = d.estoqueBaixo ?? [];
  const semFicha = baixos.filter((p) => p.semFichaTecnica);
  return <>
    <section className="page-heading">
      <div><h1>Olá, {user?.nome?.split(' ')[0] ?? 'bem-vindo'}</h1><p>Resumo de hoje, {date(d.hoje)}.</p></div>
    </section>

    <section className="cards">
      {v && <>
        <article><span>Vendas hoje</span><strong>{money(v.vendasHoje)}</strong><small className="muted">{v.qtdVendasHoje} venda(s)</small></article>
        <article><span>Vendas no mês</span><strong>{money(v.vendasMes)}</strong><Variacao valor={v.variacaoMes} /></article>
        <article><span>Ticket médio do mês</span><strong>{money(v.ticketMedioMes)}</strong><small className="muted">{v.qtdVendasMes} venda(s) no mês</small></article>
        <article><span>Lucro bruto do mês</span><strong>{money(v.lucroBrutoMes)}</strong><small className="muted">vendas menos custo dos produtos</small></article>
      </>}
      {!v && d.clientes && <article><span>Clientes</span><strong>{d.clientes.total}</strong></article>}
      {!v && d.producao && <article><span>Ordens abertas</span><strong>{d.producao.abertas}</strong></article>}
      {!v && d.entregasHoje && <article><span>Entregas hoje</span><strong>{d.entregasHoje.length}</strong></article>}
    </section>

    {v && <div className="dash-grid wide-left"><GraficoVendas dias={v.vendasPorDia} /><MaisVendidos itens={v.maisVendidos} /></div>}

    <div className="dash-grid">
      {d.entregasHoje && <section className="panel"><h2>Entregas de hoje</h2>
        {d.entregasHoje.length === 0 ? <Empty children="Nenhuma entrega pendente para hoje." /> :
          <ul className="alerts">{d.entregasHoje.map((e) => <li key={e.id}>
            <span><strong>{e.horario ? e.horario.slice(0, 5) : '--:--'}</strong> · #{e.numeroVenda} {e.cliente}{e.bairro ? ` (${e.bairro})` : ''}</span>
            <span className={`badge ${e.status === 'EM_ROTA' ? 'baixo' : 'atencao'}`}>{STATUS_ENTREGA[e.status] ?? e.status}</span>
          </li>)}</ul>}
        <Link to="/entregas" className="see-more">Ver todas as entregas</Link>
      </section>}

      {d.producao && <section className="panel"><h2>Ordens de produção abertas</h2>
        <p className="muted">{d.producao.abertas} aberta(s){d.producao.automaticas ? `, ${d.producao.automaticas} criada(s) automaticamente` : ''}.</p>
        {d.producao.ultimas.length === 0 ? <Empty children="Nenhuma ordem aberta." /> :
          <ul className="alerts">{d.producao.ultimas.map((o) => <li key={o.id}>
            <span>#{o.numero} · {Number(o.quantidade).toLocaleString('pt-BR')} × {o.produto}</span>
            {o.origem === 'AUTOMATICA' ? <span className="badge auto">Automática</span> : <span className="muted">{date(o.data)}</span>}
          </li>)}</ul>}
        <Link to="/ordens-producao" className="see-more">Ver produção</Link>
      </section>}

      {d.estoqueBaixo && <section className="panel"><h2>Estoque que exige atenção</h2>
        {semFicha.length > 0 && <p className="notice">{semFicha.length} produto(s) com estoque baixo sem ficha técnica: a ordem automática não é criada para eles. Cadastre a ficha em Estoque.</p>}
        {baixos.length === 0 ? <Empty children="Nenhum alerta de estoque no momento." /> :
          <ul className="alerts">{baixos.slice(0, 8).map((p) => <li key={p.id}>
            <span><strong>{p.nome}</strong><small className="muted"> · comercial {p.comercial} (mín. {p.minimoComercial}) · produção {p.producao}</small></span>
            <span className="row-actions">{p.semFichaTecnica && <span className="badge atencao">Sem ficha</span>}<span className={`badge ${p.esgotado ? 'esgotado' : 'baixo'}`}>{p.esgotado ? 'Esgotado' : 'Baixo'}</span></span>
          </li>)}</ul>}
        <Link to="/produtos" className="see-more">Ver estoque</Link>
      </section>}

      {d.clientes && <section className="panel"><h2>Clientes</h2>
        <p><strong>{d.clientes.total}</strong> cliente(s) cadastrados.</p>
        <p>{d.clientes.paraReativar > 0 ? <><strong>{d.clientes.paraReativar}</strong> cliente(s) sem comprar há mais de 2 meses: bom momento para chamar no WhatsApp.</> : 'Nenhum cliente parado há mais de 2 meses.'}</p>
        <Link to="/clientes" className="see-more">Ver clientes</Link>
      </section>}
    </div>
  </>;
}
