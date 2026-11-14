import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {keepPreviousData, useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {useDebounce} from '../hooks/useDebounce';
import {api} from '../services/api';
import {Loading, Failure, Empty} from '../components/State';
import Modal from '../components/Modal';
import {useAuth} from '../contexts/AuthContext';
import {date, money} from '../utils/format';

const defaults = {
    nome: '',
    telefone: '',
    email: '',
    razaoSocial: '',
    cnpj: '',
    endereco: '',
    bairro: '',
    cidade: '',
    cep: '',
    dataNascimento: '',
    observacoes: '',
    statusCadastral: 'ATIVO'
};
const toForm = (c) => Object.fromEntries(Object.keys(defaults).map((k) => [k, c[k] ?? defaults[k]]));
// Campos vazios vão como null (data vazia não é uma data válida para o servidor).
const toPayload = (d) => Object.fromEntries(Object.entries(d).map(([k, v]) => [k, typeof v === 'string' && !v.trim() ? null : v]));

export default function Clientes() {
    const [editing, setEditing] = useState(null); // null = fechado, {} = novo, cliente = edição
    const [search, setSearch] = useState('');
    const termo = useDebounce(search.trim());
    const {canAction} = useAuth();
    const queryClient = useQueryClient();
    const q = useQuery({
        queryKey: ['clientes', termo],
        queryFn: () => api.get('/clientes', {params: {search: termo, size: 50}}),
        placeholderData: keepPreviousData
    });
    const form = useForm({defaultValues: defaults});
    const refresh = () => {
        queryClient.invalidateQueries({queryKey: ['clientes']});
        queryClient.invalidateQueries({queryKey: ['/clientes']});
        queryClient.invalidateQueries({queryKey: ['dashboard']});
    };
    const open = (c) => {
        form.reset(c?.id ? toForm(c) : defaults);
        setEditing(c ?? {});
    };
    const close = () => setEditing(null);
    const save = useMutation({
        mutationFn: (data) => editing?.id ? api.put(`/clientes/${editing.id}`, toPayload(data)) : api.post('/clientes', toPayload(data)),
        onSuccess: () => {
            close();
            refresh();
        }
    });
    const remove = useMutation({mutationFn: (id) => api.delete(`/clientes/${id}`), onSuccess: refresh});
    const excluir = (c) => {
        if (window.confirm(`Excluir o cliente "${c.nome}"? O histórico de vendas é mantido.`)) remove.mutate(c.id);
    };
    // Cabeçalho e busca ficam sempre montados; só a lista mostra carregando/erro (senão o campo perde o foco a cada letra).
    const rows = q.data?.data?.content ?? [];
    return <>
        <section className="page-heading">
            <div><h1>Clientes</h1><p>Cadastros e acompanhamento de relacionamento.</p></div>
            {canAction('clientes_criar') && <button onClick={() => open(null)}>Novo cliente</button>}</section>
        <input className="search" placeholder="Buscar por nome, telefone ou cidade" value={search}
               onChange={(e) => setSearch(e.target.value)}/>
        {remove.isError && <p className="error">{remove.error.message}</p>}
        {q.isPending ? <Loading/> : q.isError ? <Failure error={q.error}/> :
        <section className="card-grid">{rows.length ? rows.map((c) => <article className="customer-card" key={c.id}>
            <div><h2>{c.nome}</h2><span
                className={`badge ${c.classificacao.toLowerCase()}`}>{c.classificacao.replace('_', ' ')}</span></div>
            <p>{c.telefone} · {c.cidade ?? 'Cidade não informada'}</p><p>Última compra: {date(c.ultimaCompra)}</p>
            <strong>{money(c.totalCompras)}</strong>{(['ATENCAO', 'ATENCAO_INATIVO', 'INATIVO'].includes(c.classificacao)) &&
            <a className="whatsapp" href={`https://wa.me/${c.telefone}`} target="_blank" rel="noreferrer">Chamar no WhatsApp</a>}
            {(canAction('clientes_editar') || canAction('clientes_excluir')) && <div className="row-actions">
                {canAction('clientes_editar') && <button className="link" onClick={() => open(c)}>Editar</button>}
                {canAction('clientes_excluir') && <button className="link danger" onClick={() => excluir(c)} disabled={remove.isPending}>Excluir</button>}
            </div>}
        </article>) : <Empty/>}</section>}
        {editing !== null && <Modal title={editing.id ? 'Editar cliente' : 'Novo cliente'} onClose={close}>
            <form className="form-grid" onSubmit={form.handleSubmit((data) => save.mutate(data))}>
                <label>Nome<input required maxLength="150" {...form.register('nome')}/></label>
                <label>Telefone<input required maxLength="20" {...form.register('telefone')}/></label>
                <label>E-mail<input type="email" {...form.register('email')}/></label>
                <label>Data de nascimento<input type="date" {...form.register('dataNascimento')}/></label>
                <label className="full">Endereço<input {...form.register('endereco')}/></label>
                <label>Bairro<input {...form.register('bairro')}/></label>
                <label>Cidade<input {...form.register('cidade')}/></label>
                <label>CEP<input maxLength="10" {...form.register('cep')}/></label>
                <label>Status<select {...form.register('statusCadastral')}>
                    <option value="ATIVO">Ativo</option>
                    <option value="INATIVO">Inativo</option>
                </select></label>
                <label>Razão social<input maxLength="180" {...form.register('razaoSocial')}/></label>
                <label>CNPJ<input maxLength="18" {...form.register('cnpj')}/></label>
                <label className="full">Observações<input {...form.register('observacoes')}/></label>
                {save.isError && <p className="error full">{save.error.message}</p>}
                <footer>
                    <button type="button" className="secondary" onClick={close}>Fechar</button>
                    <button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar'}</button>
                </footer>
            </form>
        </Modal>}</>;
}
