import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { date, today } from '../utils/format';

const LABEL = { AGENDADA: 'Agendada', EM_ROTA: 'Em rota', ENTREGUE: 'Entregue', NAO_ENTREGUE: 'Não entregue', CANCELADA: 'Cancelada' };
const BADGE = { ENTREGUE: '', EM_ROTA: 'baixo', AGENDADA: 'atencao', NAO_ENTREGUE: 'inativo', CANCELADA: 'inativo' };
// Próximos status permitidos pelo servidor a partir de cada status.
const NEXT = { AGENDADA: ['EM_ROTA', 'ENTREGUE', 'NAO_ENTREGUE'], EM_ROTA: ['ENTREGUE', 'NAO_ENTREGUE'], NAO_ENTREGUE: ['AGENDADA'] };

function NovaEntrega({ ocupadas, onClose, onDone }) {
  const vendas = useQuery({ queryKey: ['vendas'], queryFn: () => api.get('/vendas') });
  const funcionarios = useQuery({ queryKey: ['funcionarios'], queryFn: () => api.get('/funcionarios'), retry: false });
  const [form, setForm] = useState({ vendaId: '', dataAgendada: today(), horario: '', entregadorId: '', valorFrete: 0, enderecoEntrega: '', observacoes: '' });
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const disponiveis = (vendas.data?.data ?? []).filter((v) => v.status === 'FINALIZADA' && !ocupadas.has(v.id));
  const entregadores = (funcionarios.data?.data ?? []).filter((f) => f.status === 'ATIVO');
  const save = useMutation({ mutationFn: () => api.post('/entregas', { ...form, horario: form.horario || null, entregadorId: form.entregadorId || null, valorFrete: Number(form.valorFrete) || 0 }), onSuccess: () => { onDone(); onClose(); } });
  return <Modal title="Nova entrega" onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <label className="full">Venda<select required value={form.vendaId} onChange={set('vendaId')}><option value="">{vendas.isPending ? 'Carregando…' : 'Selecione a venda'}</option>{disponiveis.map((v) => <option key={v.id} value={v.id}>#{v.numeroVenda} · {v.cliente} · {date(v.dataVenda)}</option>)}</select></label>
      <label>Data agendada<input type="date" value={form.dataAgendada} onChange={set('dataAgendada')} /></label>
      <label>Horário<input type="time" value={form.horario} onChange={set('horario')} /></label>
      <label>Entregador<select value={form.entregadorId} onChange={set('entregadorId')}><option value="">A definir</option>{entregadores.map((f) => <option key={f.id} value={f.id}>{f.nome} ({f.cargo})</option>)}</select></label>
      <label>Frete (R$)<input type="number" min="0" step="0.01" value={form.valorFrete} onChange={set('valorFrete')} /></label>
      <label className="full">Endereço (vazio = endereço do cliente)<input value={form.enderecoEntrega} onChange={set('enderecoEntrega')} /></label>
      <label className="full">Observações<input value={form.observacoes} onChange={set('observacoes')} /></label>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Cancelar</button><button disabled={save.isPending || !form.vendaId}>{save.isPending ? 'Salvando…' : 'Agendar'}</button></footer>
    </form>
  </Modal>;
}

export default function Entregas() {
  const [open, setOpen] = useState(false);
  const { canAction } = useAuth();
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['entregas'], queryFn: () => api.get('/entregas') });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['entregas'] });
  const status = useMutation({ mutationFn: ({ id, novo }) => api.patch(`/entregas/${id}/status`, { status: novo }), onSuccess: refresh });
  const rows = q.data?.data ?? [];
  return <>
    <section className="page-heading"><div><h1>Entregas</h1><p>Agendamento e acompanhamento das entregas de vendas.</p></div>{canAction('entregas_criar') && <button onClick={() => setOpen(true)}>Nova entrega</button>}</section>
    {status.isError && <p className="error">{status.error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Venda</th><th>Cliente</th><th>Agendada</th><th>Entregador</th><th>Status</th><th></th></tr></thead><tbody>
      {rows.map((e) => <tr key={e.id}><td>#{e.numeroVenda}</td><td>{e.cliente}</td><td>{date(e.dataAgendada)}{e.horario ? ` ${e.horario.slice(0, 5)}` : ''}</td><td>{e.entregador ?? '—'}</td><td><span className={`badge ${BADGE[e.status] ?? ''}`}>{LABEL[e.status] ?? e.status}</span></td>
        <td className="row-actions">{canAction('entregas_atualizar_status') && (NEXT[e.status] ?? []).map((novo) => <button key={novo} className="link" disabled={status.isPending} onClick={() => status.mutate({ id: e.id, novo })}>{novo === 'AGENDADA' ? 'Reagendar' : LABEL[novo]}</button>)}</td></tr>)}
    </tbody></table></div> : <Empty />}</section>
    {open && <NovaEntrega ocupadas={new Set(rows.map((e) => e.vendaId))} onClose={() => setOpen(false)} onDone={refresh} />}
  </>;
}
