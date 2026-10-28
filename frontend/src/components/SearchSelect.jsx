import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';

/**
 * Select com busca no servidor (endpoints paginados com ?search=): não fica limitado aos primeiros registros.
 * onChange recebe o registro inteiro (ou null), para a tela usar preço, saldo etc. sem nova consulta.
 */
export default function SearchSelect({ label, endpoint, value, onChange, filter = () => true, describe = (o) => o.nome, placeholder = 'Digite para buscar…', required = false }) {
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(null);
  const q = useQuery({ queryKey: [endpoint, 'busca', search], queryFn: () => api.get(endpoint, { params: { search, size: 20 } }) });
  const found = (q.data?.data?.content ?? []).filter(filter);
  const options = selected && selected.id === value && !found.some((o) => o.id === value) ? [selected, ...found] : found;
  const choose = (id) => { const option = options.find((o) => o.id === id) ?? null; setSelected(option); onChange(option); };
  return <label className="search-select">{label}
    <input type="search" placeholder={placeholder} value={search} onChange={(e) => setSearch(e.target.value)} aria-label={`Buscar ${label}`} />
    <select required={required} value={value ?? ''} onChange={(e) => choose(e.target.value)}>
      <option value="">{q.isPending ? 'Carregando…' : options.length ? 'Selecione' : 'Nenhum resultado'}</option>
      {options.map((o) => <option key={o.id} value={o.id}>{describe(o)}</option>)}
    </select>
  </label>;
}
