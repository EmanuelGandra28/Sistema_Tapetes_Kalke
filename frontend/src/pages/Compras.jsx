import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { ask } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import SearchSelect from '../components/SearchSelect';
import { useAuth } from '../contexts/AuthContext';
import { date, money, today } from '../utils/format';

function NovaCompra({ onClose, onDone }) {
  const fornecedores = useQuery({ queryKey: ['fornecedores'], queryFn: () => api.get('/fornecedores') });
  const [fornecedorId, setFornecedorId] = useState('');
  const [manual, setManual] = useState('');
  const [dataCompra, setDataCompra] = useState(today());
  const [observacoes, setObservacoes] = useState('');
  const [produto, setProduto] = useState(null);
  const [item, setItem] = useState({ destinoEstoque: 'COMERCIAL', quantidade: 1, precoUnitario: 0 });
  const [itens, setItens] = useState([]);
  const total = itens.reduce((s, i) => s + i.quantidade * i.precoUnitario, 0);
  const save = useMutation({ mutationFn: () => api.post('/compras', { fornecedorId: fornecedorId || null, fornecedorNomeManual: fornecedorId ? null : manual, dataCompra, observacoes, itens: itens.map(({ produtoId, destinoEstoque, quantidade, precoUnitario }) => ({ produtoId, destinoEstoque, quantidade, precoUnitario })) }), onSuccess: () => { onDone(); onClose(); } });
  const add = () => {
    if (!produto || Number(item.quantidade) <= 0) return;
    setItens((old) => [...old, { produtoId: produto.id, nome: produto.nome, destinoEstoque: item.destinoEstoque, quantidade: Number(item.quantidade), precoUnitario: Number(item.precoUnitario) }]);
    setProduto(null);
  };
  return <Modal title="Nova compra" onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <label>Fornecedor cadastrado<select value={fornecedorId} onChange={(e) => setFornecedorId(e.target.value)}><option value="">Informar manualmente</option>{(fornecedores.data?.data ?? []).map((f) => <option key={f.id} value={f.id}>{f.nomeEmpresa}</option>)}</select></label>
      {!fornecedorId && <label>Nome do fornecedor<input required maxLength="180" value={manual} onChange={(e) => setManual(e.target.value)} /></label>}
      <label>Data da compra<input required type="date" value={dataCompra} onChange={(e) => setDataCompra(e.target.value)} /></label>
      <label>Observações<input value={observacoes} onChange={(e) => setObservacoes(e.target.value)} /></label>
      <fieldset className="full items-box"><legend>Itens</legend>
        <div className="form-row">
          <SearchSelect label="Produto" endpoint="/produtos" value={produto?.id} onChange={(p) => { setProduto(p); if (p) setItem((i) => ({ ...i, precoUnitario: p.precoCusto ?? 0, destinoEstoque: p.podeSerMateriaPrima && !p.podeSerVendido ? 'PRODUCAO' : 'COMERCIAL' })); }} filter={(p) => p.ativo} />
          <label>Destino<select value={item.destinoEstoque} onChange={(e) => setItem({ ...item, destinoEstoque: e.target.value })}><option value="COMERCIAL">Comercial</option><option value="PRODUCAO">Produção</option></select></label>
          <label>Qtd.<input type="number" min="0.001" step="0.001" value={item.quantidade} onChange={(e) => setItem({ ...item, quantidade: e.target.value })} /></label>
          <label>Preço unit.<input type="number" min="0" step="0.01" value={item.precoUnitario} onChange={(e) => setItem({ ...item, precoUnitario: e.target.value })} /></label>
          <button type="button" className="secondary" onClick={add} disabled={!produto}>Adicionar</button>
        </div>
        {itens.length > 0 && <ul className="cart">{itens.map((i, idx) => <li key={idx}><span>{i.nome} × {i.quantidade} ({i.destinoEstoque === 'PRODUCAO' ? 'produção' : 'comercial'})</span><span>{money(i.quantidade * i.precoUnitario)} <button type="button" className="link" onClick={() => setItens(itens.filter((_, x) => x !== idx))}>Remover</button></span></li>)}</ul>}
        <strong>Total: {money(total)}</strong>
      </fieldset>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Cancelar</button><button disabled={save.isPending || itens.length === 0}>{save.isPending ? 'Salvando…' : 'Registrar compra'}</button></footer>
    </form>
  </Modal>;
}

export default function Compras() {
  const [open, setOpen] = useState(false);
  const { canAction } = useAuth();
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['compras'], queryFn: () => api.get('/compras') });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['compras'] }); queryClient.invalidateQueries({ queryKey: ['/produtos'] }); };
  const cancel = useMutation({ mutationFn: ({ id, motivo }) => api.post(`/compras/${id}/cancelar`, { motivo }), onSuccess: refresh });
  const cancelar = (c) => { const motivo = ask('Motivo do cancelamento da compra:'); if (motivo) cancel.mutate({ id: c.id, motivo }); };
  const rows = q.data?.data ?? [];
  return <>
    <section className="page-heading"><div><h1>Compras</h1><p>Entradas de mercadoria e matéria-prima no estoque.</p></div>{canAction('compras_criar') && <button onClick={() => setOpen(true)}>Nova compra</button>}</section>
    {cancel.isError && <p className="error">{cancel.error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Data</th><th>Fornecedor</th><th>Total</th><th>Status</th><th></th></tr></thead><tbody>
      {rows.map((c) => <tr key={c.id}><td>{date(c.dataCompra)}</td><td>{c.fornecedor ?? '—'}</td><td>{money(c.total)}</td><td><span className={`badge ${c.status === 'CANCELADA' ? 'inativo' : ''}`}>{c.status}</span></td>
        <td>{canAction('compras_cancelar') && c.status === 'FINALIZADA' && <button className="link" onClick={() => cancelar(c)}>Cancelar</button>}</td></tr>)}
    </tbody></table></div> : <Empty />}</section>
    {open && <NovaCompra onClose={() => setOpen(false)} onDone={refresh} />}
  </>;
}
