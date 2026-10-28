import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { useCodigos } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { money } from '../utils/format';

const defaults = { nome: '', tipo: 'TAPETE', categoria: '', cor: '', tamanho: '', unidadeMedida: 'UNIDADE', podeSerVendido: true, podeSerMateriaPrima: false, estoqueMinimoComercial: 1, estoqueMinimoProducao: 1, precoCusto: 0, precoVenda: 0, codigoBarras: '', fornecedorPrincipalId: '', observacoes: '', ativo: true };
const toForm = (p) => Object.fromEntries(Object.keys(defaults).map((k) => [k, p[k] ?? defaults[k]]));
const toPayload = (d) => ({ ...d, estoqueMinimoComercial: Number(d.estoqueMinimoComercial), estoqueMinimoProducao: Number(d.estoqueMinimoProducao), precoCusto: Number(d.precoCusto), precoVenda: Number(d.precoVenda) });

function AjusteModal({ produto, onClose, onDone }) {
  const [saldo, setSaldo] = useState('COMERCIAL'); const [quantidade, setQuantidade] = useState(''); const [observacao, setObservacao] = useState('');
  const save = useMutation({ mutationFn: () => api.post('/estoque/ajuste', { produtoId: produto.id, saldo, quantidade: Number(quantidade), observacao }), onSuccess: () => { onDone(); onClose(); } });
  return <Modal title={`Ajustar estoque · ${produto.nome}`} onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <label>Saldo<select value={saldo} onChange={(e) => setSaldo(e.target.value)}><option value="COMERCIAL">Comercial ({produto.estoqueComercial})</option><option value="PRODUCAO">Produção ({produto.estoqueProducao})</option></select></label>
      <label>Quantidade (use negativo para baixa)<input required type="number" step="0.001" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} /></label>
      <label className="full">Motivo<input required maxLength="300" value={observacao} onChange={(e) => setObservacao(e.target.value)} /></label>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Cancelar</button><button disabled={save.isPending || !Number(quantidade)}>{save.isPending ? 'Salvando…' : 'Ajustar'}</button></footer>
    </form>
  </Modal>;
}

export default function Produtos() {
  const [editing, setEditing] = useState(null); // null = fechado, {} = novo, produto = edição
  const [ajuste, setAjuste] = useState(null);
  const [search, setSearch] = useState('');
  const { canAction } = useAuth();
  const { tiposProduto, unidadesMedida } = useCodigos();
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['/produtos', 'lista', search], queryFn: () => api.get('/produtos', { params: { search, size: 100 } }) });
  const fornecedores = useQuery({ queryKey: ['fornecedores'], queryFn: () => api.get('/fornecedores'), enabled: editing !== null });
  const form = useForm({ defaultValues: defaults });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['/produtos'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); };
  const open = (p) => { form.reset(p?.id ? toForm(p) : defaults); setEditing(p ?? {}); };
  const close = () => setEditing(null);
  const save = useMutation({ mutationFn: (data) => editing?.id ? api.put(`/produtos/${editing.id}`, toPayload(data)) : api.post('/produtos', toPayload(data)), onSuccess: () => { close(); refresh(); } });
  const remove = useMutation({ mutationFn: (id) => api.delete(`/produtos/${id}`), onSuccess: refresh });
  const excluir = (p) => { if (window.confirm(`Excluir o produto "${p.nome}"? O histórico de vendas é mantido.`)) remove.mutate(p.id); };
  const options = (list, fallback) => (list.length ? list : fallback.map((codigo) => ({ codigo, nome: codigo }))).map((o) => <option key={o.codigo} value={o.codigo}>{o.nome}</option>);
  const rows = q.data?.data?.content ?? [];
  return <>
    <section className="page-heading"><div><h1>Estoque</h1><p>Produtos, dois saldos e alertas.</p></div>{canAction('produtos_criar') && <button onClick={() => open(null)}>Novo produto</button>}</section>
    <input className="search" placeholder="Buscar por nome ou código de barras" value={search} onChange={(e) => setSearch(e.target.value)} />
    {remove.isError && <p className="error">{remove.error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Produto</th><th>Comercial</th><th>Produção</th><th>Custo</th><th>Venda</th><th>Status</th><th></th></tr></thead><tbody>
      {rows.map((p) => <tr key={p.id}><td>{p.nome}<small>{p.unidadeMedida}{p.ativo ? '' : ' · inativo'}</small></td><td>{p.estoqueComercial}</td><td>{p.estoqueProducao}</td><td>{money(p.precoCusto)}</td><td>{money(p.precoVenda)}</td>
        <td><span className={`badge ${p.statusComercial.toLowerCase()}`}>{p.statusComercial}</span></td>
        <td className="row-actions">
          {canAction('produtos_editar') && <button className="link" onClick={() => open(p)}>Editar</button>}
          {canAction('estoque_ajustar') && <button className="link" onClick={() => setAjuste(p)}>Ajustar</button>}
          {canAction('produtos_excluir') && <button className="link" onClick={() => excluir(p)}>Excluir</button>}
        </td></tr>)}
    </tbody></table></div> : <Empty />}</section>
    {ajuste && <AjusteModal produto={ajuste} onClose={() => setAjuste(null)} onDone={refresh} />}
    {editing !== null && <Modal title={editing.id ? 'Editar produto' : 'Novo produto'} onClose={close}>
      <form className="form-grid" onSubmit={form.handleSubmit((data) => save.mutate(data))}>
        <label className="full">Nome<input required maxLength="180" {...form.register('nome')} /></label>
        <label>Tipo<select {...form.register('tipo')}>{options(tiposProduto, ['TAPETE', 'MALHA'])}</select></label>
        <label>Unidade<select {...form.register('unidadeMedida')}>{options(unidadesMedida, ['UNIDADE', 'METRO', 'KG'])}</select></label>
        <label>Categoria<input {...form.register('categoria')} /></label>
        <label>Cor<input {...form.register('cor')} /></label>
        <label>Tamanho<input {...form.register('tamanho')} /></label>
        <label>Código de barras<input {...form.register('codigoBarras')} /></label>
        <label>Preço de venda<input required type="number" min="0" step="0.01" {...form.register('precoVenda')} /></label>
        <label>Preço de custo<input type="number" min="0" step="0.01" {...form.register('precoCusto')} /></label>
        <label>Estoque mínimo comercial<input type="number" min="0" step="0.001" {...form.register('estoqueMinimoComercial')} /></label>
        <label>Estoque mínimo produção<input type="number" min="0" step="0.001" {...form.register('estoqueMinimoProducao')} /></label>
        <label className="full">Fornecedor principal<select {...form.register('fornecedorPrincipalId')}><option value="">Nenhum</option>{(fornecedores.data?.data ?? []).map((f) => <option key={f.id} value={f.id}>{f.nomeEmpresa}</option>)}</select></label>
        <label className="full">Observações<input {...form.register('observacoes')} /></label>
        <label className="check"><input type="checkbox" {...form.register('podeSerVendido')} /> Pode ser vendido</label>
        <label className="check"><input type="checkbox" {...form.register('podeSerMateriaPrima')} /> Pode ser matéria-prima</label>
        <label className="check"><input type="checkbox" {...form.register('ativo')} /> Ativo</label>
        {save.isError && <p className="error full">{save.error.message}</p>}
        <footer><button type="button" className="secondary" onClick={close}>Cancelar</button><button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar'}</button></footer>
      </form>
    </Modal>}
  </>;
}
