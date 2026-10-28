import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure, Empty } from './State';
import Modal from './Modal';
import { useAuth } from '../contexts/AuthContext';

/**
 * Cadastro simples (listar, criar, editar, excluir) a partir de uma configuração de campos.
 * fields: [{ name, label, type?, required?, options?: [{value,label}], full? }]
 * columns: [{ key, label, render? }]
 * permissions: { create, edit, remove } com os nomes das ações do servidor.
 */
export default function CrudPage({ title, subtitle, endpoint, fields, columns, permissions, singular, toForm = (row) => row, toPayload = (data) => data }) {
  const [editing, setEditing] = useState(null);
  const { canAction } = useAuth();
  const queryClient = useQueryClient();
  const defaults = Object.fromEntries(fields.map((f) => [f.name, f.default ?? '']));
  const form = useForm({ defaultValues: defaults });
  const q = useQuery({ queryKey: [endpoint], queryFn: () => api.get(endpoint) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: [endpoint] });
  const open = (row) => { form.reset(row ? { ...defaults, ...Object.fromEntries(Object.entries(toForm(row)).map(([k, v]) => [k, v ?? ''])) } : defaults); setEditing(row ?? {}); };
  const close = () => setEditing(null);
  const save = useMutation({ mutationFn: (data) => editing?.id ? api.put(`${endpoint}/${editing.id}`, toPayload(data)) : api.post(endpoint, toPayload(data)), onSuccess: () => { close(); refresh(); } });
  const remove = useMutation({ mutationFn: (id) => api.delete(`${endpoint}/${id}`), onSuccess: refresh });
  const excluir = (row) => { if (window.confirm(`Excluir ${singular} "${row[columns[0].key]}"?`)) remove.mutate(row.id); };
  const rows = q.data?.data ?? [];
  return <>
    <section className="page-heading"><div><h1>{title}</h1><p>{subtitle}</p></div>{canAction(permissions.create) && <button onClick={() => open(null)}>Novo {singular}</button>}</section>
    {remove.isError && <p className="error">{remove.error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr>{columns.map((c) => <th key={c.key}>{c.label}</th>)}<th></th></tr></thead><tbody>
      {rows.map((row) => <tr key={row.id}>{columns.map((c) => <td key={c.key}>{c.render ? c.render(row) : (row[c.key] ?? '—')}</td>)}
        <td className="row-actions">{canAction(permissions.edit) && <button className="link" onClick={() => open(row)}>Editar</button>}{canAction(permissions.remove) && <button className="link" onClick={() => excluir(row)}>Excluir</button>}</td></tr>)}
    </tbody></table></div> : <Empty />}</section>
    {editing !== null && <Modal title={`${editing.id ? 'Editar' : 'Novo'} ${singular}`} onClose={close}>
      <form className="form-grid" onSubmit={form.handleSubmit((data) => save.mutate(data))}>
        {fields.map((f) => <label key={f.name} className={f.full ? 'full' : undefined}>{f.label}
          {f.options ? <select required={f.required} {...form.register(f.name)}>{f.options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select>
            : <input type={f.type ?? 'text'} required={f.required} step={f.type === 'number' ? '0.01' : undefined} min={f.type === 'number' ? '0' : undefined} {...form.register(f.name)} />}
        </label>)}
        {save.isError && <p className="error full">{save.error.message}</p>}
        <footer><button type="button" className="secondary" onClick={close}>Cancelar</button><button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar'}</button></footer>
      </form>
    </Modal>}
  </>;
}
