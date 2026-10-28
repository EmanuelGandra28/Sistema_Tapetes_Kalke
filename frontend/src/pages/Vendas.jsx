import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, createKey } from '../services/api';
import { ask, useCodigos } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import SearchSelect from '../components/SearchSelect';
import { date, money, today } from '../utils/format';
import { useAuth } from '../contexts/AuthContext';

export default function Vendas() {
  const queryClient = useQueryClient();
  const { canAction } = useAuth();
  const { formasPagamento } = useCodigos();
  const sales = useQuery({ queryKey: ['vendas'], queryFn: () => api.get('/vendas') });
  const [cliente, setCliente] = useState(null);
  const [produto, setProduto] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [items, setItems] = useState([]);
  const [payment, setPayment] = useState('PIX');
  const [desconto, setDesconto] = useState(0);
  // Mesma chave enquanto a venda não for concluída: repetir o envio não duplica a venda.
  const [saleKey, setSaleKey] = useState(createKey);
  const subtotal = useMemo(() => items.reduce((sum, i) => sum + i.quantidade * i.preco, 0), [items]);
  const refresh = () => ['vendas', 'dashboard', '/produtos', '/clientes'].forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
  const checkout = useMutation({
    mutationFn: () => api.post('/vendas', { clienteId: cliente.id, dataVenda: today(), desconto: Number(desconto) || 0, formaPagamento: payment, itens: items.map(({ produtoId, quantidade }) => ({ produtoId, quantidade })), observacoes: '' }, { headers: { 'Idempotency-Key': saleKey } }),
    onSuccess: () => { setSaleKey(createKey()); setItems([]); setCliente(null); setDesconto(0); refresh(); }
  });
  const cancel = useMutation({ mutationFn: ({ id, motivo }) => api.post(`/vendas/${id}/cancelar`, { motivo }), onSuccess: refresh });
  const add = () => {
    if (!produto || Number(quantity) <= 0) return;
    setItems((old) => [...old, { produtoId: produto.id, nome: produto.nome, quantidade: Number(quantity), preco: Number(produto.precoVenda) }]);
    setProduto(null); setQuantity(1);
  };
  const cancelar = (s) => { const motivo = ask(`Motivo do cancelamento da venda #${s.numeroVenda}:`); if (motivo) cancel.mutate({ id: s.id, motivo }); };
  const total = Math.max(0, subtotal - (Number(desconto) || 0));
  return <>
    <section className="page-heading"><div><h1>Vendas</h1><p>O servidor recalcula preço, subtotal e total antes de finalizar.</p></div></section>
    {canAction('vendas_criar') && <section className="panel sale"><h2>Nova venda</h2>
      <div className="form-row">
        <SearchSelect label="Cliente" endpoint="/clientes" value={cliente?.id} onChange={setCliente} describe={(c) => `${c.nome} · ${c.telefone}`} />
        <SearchSelect label="Produto" endpoint="/produtos" value={produto?.id} onChange={setProduto} filter={(p) => p.ativo && p.podeSerVendido} describe={(p) => `${p.nome} · ${money(p.precoVenda)} · ${p.estoqueComercial} disponível`} />
        <label>Quantidade<input type="number" min="0.001" step="0.001" value={quantity} onChange={(e) => setQuantity(e.target.value)} /></label>
        <button type="button" className="secondary" onClick={add} disabled={!produto}>Adicionar item</button>
      </div>
      {items.length > 0 && <>
        <ul className="cart">{items.map((i, index) => <li key={`${i.produtoId}-${index}`}><span>{i.nome} × {i.quantidade}</span><span>{money(i.preco * i.quantidade)} <button className="link" aria-label="Remover item" onClick={() => setItems(items.filter((_, x) => x !== index))}>Remover</button></span></li>)}</ul>
        <div className="checkout">
          <label>Pagamento<select value={payment} onChange={(e) => setPayment(e.target.value)}>{(formasPagamento.length ? formasPagamento : [{ codigo: 'PIX', nome: 'PIX' }]).map((f) => <option key={f.codigo} value={f.codigo}>{f.nome}</option>)}</select></label>
          <label>Desconto (R$)<input type="number" min="0" step="0.01" value={desconto} onChange={(e) => setDesconto(e.target.value)} /></label>
          <span>Subtotal: {money(subtotal)}</span>
          <strong>Total: {money(total)}</strong>
          <button disabled={!cliente || items.length === 0 || checkout.isPending} onClick={() => checkout.mutate()}>{checkout.isPending ? 'Finalizando…' : 'Finalizar venda'}</button>
        </div>
        {checkout.isError && <p className="error">{checkout.error.message}</p>}
      </>}
    </section>}
    <section className="panel"><h2>Histórico</h2>
      {cancel.isError && <p className="error">{cancel.error.message}</p>}
      {sales.isPending ? <Loading /> : sales.isError ? <Failure error={sales.error} /> : sales.data.data.length ? <div className="table-wrap"><table><thead><tr><th>Número</th><th>Cliente</th><th>Data</th><th>Desconto</th><th>Total</th><th>Status</th><th></th></tr></thead><tbody>
        {sales.data.data.map((s) => <tr key={s.id}><td>#{s.numeroVenda}</td><td>{s.cliente}</td><td>{date(s.dataVenda)}</td><td>{money(s.desconto)}</td><td>{money(s.total)}</td><td><span className={`badge ${s.status === 'CANCELADA' ? 'inativo' : ''}`}>{s.status}</span></td>
          <td>{canAction('vendas_cancelar') && s.status === 'FINALIZADA' && <button className="link" onClick={() => cancelar(s)} disabled={cancel.isPending}>Cancelar</button>}</td></tr>)}
      </tbody></table></div> : <Empty />}
    </section>
  </>;
}
