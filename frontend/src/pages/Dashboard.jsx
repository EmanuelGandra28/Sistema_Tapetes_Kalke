import {useQuery} from '@tanstack/react-query';
import {api} from '../services/api';
import {Loading, Failure, Empty} from '../components/State';
import {money} from '../utils/format';

export default function Dashboard() {
    const q = useQuery({queryKey: ['dashboard'], queryFn: () => api.get('/dashboard')});
    if (q.isPending) return <Loading/>;
    if (q.isError) return <Failure error={q.error}/>;
    const d = q.data.data;
    return <>
        <section className="page-heading">
            <div><h1>Dashboard</h1><p>Visão geral da operação.</p></div>
        </section>
        <section className="cards">
            <article><span>Vendas hoje</span><strong>{money(d.vendasHoje)}</strong></article>
            <article><span>Vendas no mês</span><strong>{money(d.vendasMes)}</strong></article>
            <article><span>Total de clientes</span><strong>{d.totalClientes}</strong></article>
            <article><span>Alertas de estoque</span><strong>{d.baixoOuEsgotado ?? d.itensEstoque}</strong></article>
        </section>
        <section className="panel"><h2>Produtos que exigem atenção</h2>{d.estoqueBaixo?.length ?
            <ul className="alerts">{d.estoqueBaixo.map((p) => <li key={p.id}>
                <strong>{p.nome}</strong><span>Comercial: {p.comercial} · Produção: {p.producao}</span></li>)}</ul> :
            <Empty children="Nenhum alerta de estoque no momento."/>}</section>
    </>;
}