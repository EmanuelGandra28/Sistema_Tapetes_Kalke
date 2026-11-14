import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { useCodigos } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { date, money, today } from '../utils/format';

// Mesmos códigos aceitos pelo servidor; usados enquanto a lista do banco não chega.
const TIPOS_PADRAO = [{ codigo: 'SALARIOS', nome: 'Salários' }, { codigo: 'FRETES', nome: 'Fretes' }, { codigo: 'MANUTENCAO', nome: 'Manutenção' }, { codigo: 'OUTROS', nome: 'Outros' }];

function NovaDespesa({ tipos, onClose, onDone }) {
  const [form, setForm] = useState({ tipo: 'OUTROS', descricao: '', valor: '', dataDespesa: today(), observacoes: '' });
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const save = useMutation({ mutationFn: () => api.post('/despesas', { ...form, valor: Number(form.valor), observacoes: form.observacoes.trim() || null }), onSuccess: () => { onDone(); onClose(); } });
  return <Modal title="Nova despesa" onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <label>Tipo<select value={form.tipo} onChange={set('tipo')}>{tipos.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}</select></label>
      <label>Data<input required type="date" value={form.dataDespesa} onChange={set('dataDespesa')} /></label>
      <label className="full">Descrição<input required maxLength="255" value={form.descricao} onChange={set('descricao')} /></label>
      <label>Valor (R$)<input required type="number" min="0.01" step="0.01" value={form.valor} onChange={set('valor')} /></label>
      <label className="full">Observações<input value={form.observacoes} onChange={set('observacoes')} /></label>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Fechar</button><button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Registrar despesa'}</button></footer>
    </form>
  </Modal>;
}

export default function Despesas() {
  const [open, setOpen] = useState(false);
  const { canAction } = useAuth();
  const { tiposDespesa } = useCodigos();
  const tipos = tiposDespesa.length ? tiposDespesa : TIPOS_PADRAO;
  const nomeTipo = (codigo) => tipos.find((t) => t.codigo === codigo)?.nome ?? codigo;
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['despesas'], queryFn: () => api.get('/despesas') });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['despesas'] }); queryClient.invalidateQueries({ queryKey: ['financeiro'] }); };
  const rows = q.data?.data ?? [];
  return <>
    <section className="page-heading"><div><h1>Despesas</h1><p>Salários, fretes, manutenção e outros gastos. Entram em "Salários" e "Outras despesas" nos relatórios.</p></div>{canAction('despesas_criar') && <button onClick={() => setOpen(true)}>Nova despesa</button>}</section>
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Data</th><th>Tipo</th><th>Descrição</th><th>Valor</th></tr></thead><tbody>
      {rows.map((d) => <tr key={d.id}><td>{date(d.dataDespesa)}</td><td>{nomeTipo(d.tipo)}</td><td>{d.descricao}{d.observacoes && <small>{d.observacoes}</small>}</td><td>{money(d.valor)}</td></tr>)}
    </tbody></table></div> : <Empty />}</section>
    {open && <NovaDespesa tipos={tipos} onClose={() => setOpen(false)} onDone={refresh} />}
  </>;
}
