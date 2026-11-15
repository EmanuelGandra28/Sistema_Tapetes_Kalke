import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import SearchSelect from '../../components/SearchSelect';
import { Loading, Failure, Empty } from '../../components/State';

const TIPOS = { SAIDA_VENDA: 'Venda', ESTORNO_VENDA: 'Venda cancelada', ENTRADA_COMPRA: 'Compra', ESTORNO_COMPRA: 'Compra cancelada', CONSUMO_PRODUCAO: 'Consumo na produção', ENTRADA_PRODUCAO: 'Produção concluída', AJUSTE_MANUAL: 'Ajuste manual', INVENTARIO: 'Inventário' };
const tipoLegivel = (t) => TIPOS[t] ?? t.replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase());
const dataHora = (v) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(v));

// Entradas e saídas de estoque com filtros; a exportação usa os mesmos filtros.
export default function Movimentacoes({ exportar }) {
  const [produto, setProduto] = useState(null);
  const [filtro, setFiltro] = useState({ saldo: '', inicio: '', fim: '' });
  const [page, setPage] = useState(0);
  const params = { produtoId: produto?.id, saldo: filtro.saldo || undefined, inicio: filtro.inicio || undefined, fim: filtro.fim || undefined };
  const q = useQuery({ queryKey: ['movimentos', params, page], queryFn: () => api.get('/estoque/movimentos', { params: { ...params, page, size: 50 } }), placeholderData: keepPreviousData });
  const set = (k) => (e) => { setFiltro({ ...filtro, [k]: e.target.value }); setPage(0); };
  const pagina = q.data?.data;
  const rows = pagina?.content ?? [];
  return <>
    <section className="panel filters">
      <SearchSelect label="Produto" endpoint="/produtos" value={produto?.id} onChange={(p) => { setProduto(p); setPage(0); }} />
      <label>Saldo<select value={filtro.saldo} onChange={set('saldo')}><option value="">Todos</option><option value="COMERCIAL">Comercial</option><option value="PRODUCAO">Produção</option></select></label>
      <label>De<input type="date" value={filtro.inicio} onChange={set('inicio')} /></label>
      <label>Até<input type="date" value={filtro.fim} onChange={set('fim')} /></label>
      {exportar && <button type="button" className="secondary" onClick={() => exportar('/estoque/movimentos/exportar', params, 'movimentacoes')}>Exportar Excel</button>}
    </section>
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <>
      <div className="table-wrap"><table><thead><tr><th>Data</th><th>Produto</th><th>Saldo</th><th>Tipo</th><th>Quantidade</th><th>Antes → depois</th><th>Usuário</th></tr></thead><tbody>
        {rows.map((m) => <tr key={m.id}><td>{dataHora(m.data)}</td><td>{m.produto}{m.observacao && <small>{m.observacao}</small>}</td>
          <td>{m.saldo === 'COMERCIAL' ? 'Comercial' : 'Produção'}</td><td>{tipoLegivel(m.tipo)}</td>
          <td className={Number(m.quantidade) > 0 ? 'qtd-entrada' : 'qtd-saida'}>{Number(m.quantidade) > 0 ? '+' : ''}{m.quantidade}</td>
          <td>{m.anterior} → {m.posterior}</td><td>{m.usuario ?? '—'}</td></tr>)}
      </tbody></table></div>
      {pagina.totalPages > 1 && <div className="row-actions"><button type="button" className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button>
        <span>Página {page + 1} de {pagina.totalPages}</span>
        <button type="button" className="secondary" disabled={page + 1 >= pagina.totalPages} onClick={() => setPage(page + 1)}>Próxima</button></div>}
    </> : <Empty>Nenhuma movimentação com esses filtros.</Empty>}</section>
  </>;
}
