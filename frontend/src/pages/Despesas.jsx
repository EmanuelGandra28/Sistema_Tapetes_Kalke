import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { useCodigos } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import Paginacao from '../components/Paginacao';
import { useAuth } from '../contexts/AuthContext';
import { date, money, today } from '../utils/format';

// Mesmos códigos aceitos pelo servidor; usados enquanto a lista do banco não chega.
const TIPOS_PADRAO = [{ codigo: 'SALARIOS', nome: 'Salários' }, { codigo: 'FRETES', nome: 'Fretes' }, { codigo: 'MANUTENCAO', nome: 'Manutenção' }, { codigo: 'OUTROS', nome: 'Outros' }];

// Serve para lançar uma despesa nova (despesa = null) e para editar uma existente.
function FormDespesa({ despesa, tipos, onClose, onDone }) {
  const [form, setForm] = useState({
    tipo: despesa?.tipo ?? 'OUTROS', descricao: despesa?.descricao ?? '', valor: despesa?.valor ?? '',
    dataDespesa: despesa?.dataDespesa ?? today(), observacoes: despesa?.observacoes ?? ''
  });
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const save = useMutation({
    mutationFn: () => {
      const body = { ...form, valor: Number(form.valor), observacoes: form.observacoes.trim() || null };
      return despesa ? api.put(`/despesas/${despesa.id}`, body) : api.post('/despesas', body);
    },
    onSuccess: () => { onDone(); onClose(); }
  });
  return <Modal title={despesa ? 'Editar despesa' : 'Nova despesa'} onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <label>Tipo<select value={form.tipo} onChange={set('tipo')}>{tipos.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}</select></label>
      <label>Data<input required type="date" value={form.dataDespesa} onChange={set('dataDespesa')} /></label>
      <label className="full">Descrição<input required maxLength="255" value={form.descricao} onChange={set('descricao')} /></label>
      <label>Valor (R$)<input required type="number" min="0.01" step="0.01" value={form.valor} onChange={set('valor')} /></label>
      <label className="full">Observações<input value={form.observacoes} onChange={set('observacoes')} /></label>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Fechar</button><button disabled={save.isPending}>{save.isPending ? 'Salvando…' : despesa ? 'Salvar alterações' : 'Registrar despesa'}</button></footer>
    </form>
  </Modal>;
}

export default function Despesas() {
  const [editing, setEditing] = useState(null); // null = fechado, {} = nova, despesa = edição
  const { canAction } = useAuth();
  const { tiposDespesa } = useCodigos();
  const tipos = tiposDespesa.length ? tiposDespesa : TIPOS_PADRAO;
  const nomeTipo = (codigo) => tipos.find((t) => t.codigo === codigo)?.nome ?? codigo;
  const queryClient = useQueryClient();
  const [pagina, setPagina] = useState(0);
  const q = useQuery({ queryKey: ['despesas', 'pagina', pagina], queryFn: () => api.get('/despesas', { params: { page: pagina, size: 20 } }), placeholderData: keepPreviousData });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['despesas'] }); queryClient.invalidateQueries({ queryKey: ['relatorio'] }); };
  const remove = useMutation({ mutationFn: ({ id, motivo }) => api.delete(`/despesas/${id}`, { params: { motivo } }), onSuccess: refresh });
  const excluir = (d) => {
    const motivo = window.prompt(`Excluir a despesa "${d.descricao}" (${money(d.valor)})?\nInforme o motivo da exclusão:`);
    if (motivo && motivo.trim()) remove.mutate({ id: d.id, motivo: motivo.trim() });
  };
  const rows = q.data?.data?.content ?? [];
  const acoes = canAction('despesas_editar') || canAction('despesas_excluir');
  return <>
    <section className="page-heading"><div><h1>Despesas</h1><p>Salários, fretes, manutenção e outros gastos. Entram em "Salários" e "Outras despesas" nos relatórios.</p></div>{canAction('despesas_criar') && <button onClick={() => setEditing({})}>Nova despesa</button>}</section>
    {remove.isError && <p className="error">{remove.error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Data</th><th>Tipo</th><th>Descrição</th><th>Valor</th>{acoes && <th>Ações</th>}</tr></thead><tbody>
      {rows.map((d) => <tr key={d.id}><td>{date(d.dataDespesa)}</td><td>{nomeTipo(d.tipo)}</td><td>{d.descricao}{d.observacoes && <small>{d.observacoes}</small>}</td><td>{money(d.valor)}</td>
        {acoes && <td><div className="row-actions">
          {canAction('despesas_editar') && <button className="link" onClick={() => setEditing(d)}>Editar despesa</button>}
          {canAction('despesas_excluir') && <button className="link danger" onClick={() => excluir(d)} disabled={remove.isPending}>Excluir despesa</button>}
        </div></td>}</tr>)}
    </tbody></table></div> : <Empty />}<Paginacao page={pagina} data={q.data?.data} onChange={setPagina} /></section>
    {editing !== null && <FormDespesa despesa={editing.id ? editing : null} tipos={tipos} onClose={() => setEditing(null)} onDone={refresh} />}
  </>;
}
