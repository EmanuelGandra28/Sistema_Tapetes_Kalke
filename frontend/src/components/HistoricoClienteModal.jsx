import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure, Empty } from './State';
import Modal from './Modal';
import { date, money } from '../utils/format';

// Compras do cliente: totais (só vendas finalizadas) e a lista com os itens de cada venda.
export default function HistoricoClienteModal({ cliente, onClose }) {
  const [aberta, setAberta] = useState(null);
  const q = useQuery({ queryKey: ['clientes', 'historico', cliente.id], queryFn: () => api.get(`/clientes/${cliente.id}/historico-compras`) });
  const h = q.data?.data;
  return <Modal title={`Histórico · ${cliente.nome}`} onClose={onClose}>
    {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : <>
      <section className="cards compact">
        <article><span>Total comprado</span><strong>{money(h.totalFinalizado)}</strong></article>
        <article><span>Compras</span><strong>{h.compras}</strong></article>
        <article><span>Ticket médio</span><strong>{money(h.ticketMedio)}</strong></article>
        <article><span>Última compra</span><strong>{date(h.ultimaCompra)}</strong></article>
      </section>
      {h.vendas.length ? <ul className="history-list">{h.vendas.map((v) => <li key={v.id} className={v.status === 'CANCELADA' ? 'cancelada' : undefined}>
        <button type="button" className="history-row" aria-expanded={aberta === v.id} onClick={() => setAberta(aberta === v.id ? null : v.id)}>
          <span>#{v.numero} · {date(v.data)}</span>
          <span>{v.status === 'CANCELADA' && <span className="badge inativo">Cancelada</span>} {money(v.total)}</span>
        </button>
        {aberta === v.id && <table><thead><tr><th>Produto</th><th>Qtd.</th><th>Preço</th><th>Subtotal</th></tr></thead><tbody>
          {v.itens.map((i, idx) => <tr key={idx}><td>{i.produto}</td><td>{i.quantidade}</td><td>{money(i.precoUnitario)}</td><td>{money(i.subtotal)}</td></tr>)}
          {Number(v.desconto) > 0 && <tr><td colSpan={3}>Desconto</td><td>− {money(v.desconto)}</td></tr>}
        </tbody></table>}
      </li>)}</ul> : <Empty>Este cliente ainda não comprou.</Empty>}
    </>}
  </Modal>;
}
