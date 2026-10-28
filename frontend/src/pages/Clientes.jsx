import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
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
export default function Clientes() {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const {canAction} = useAuth();
    const queryClient = useQueryClient();
    const q = useQuery({
        queryKey: ['clientes', search],
        queryFn: () => api.get('/clientes', {params: {search, size: 50}})
    });
    const form = useForm({defaultValues: defaults});
    const save = useMutation({
        mutationFn: (data) => api.post('/clientes', data), onSuccess: () => {
            form.reset(defaults);
            setOpen(false);
            queryClient.invalidateQueries({queryKey: ['clientes']});
        }
    });
    if (q.isPending) return <Loading/>;
    if (q.isError) return <Failure error={q.error}/>;
    const rows = q.data.data.content;
    return <>
        <section className="page-heading">
            <div><h1>Clientes</h1><p>Cadastros e acompanhamento de relacionamento.</p></div>
            {canAction('clientes_criar') && <button onClick={() => setOpen(true)}>Novo cliente</button>}</section>
        <input className="search" placeholder="Buscar por nome, telefone ou cidade" value={search}
               onChange={(e) => setSearch(e.target.value)}/>
        <section className="card-grid">{rows.length ? rows.map((c) => <article className="customer-card" key={c.id}>
            <div><h2>{c.nome}</h2><span
                className={`badge ${c.classificacao.toLowerCase()}`}>{c.classificacao.replace('_', ' ')}</span></div>
            <p>{c.telefone} · {c.cidade ?? 'Cidade não informada'}</p><p>Última compra: {date(c.ultimaCompra)}</p>
            <strong>{money(c.totalCompras)}</strong>{(['ATENCAO', 'ATENCAO_INATIVO', 'INATIVO'].includes(c.classificacao)) &&
            <a className="whatsapp" href={`https://wa.me/${c.telefone}`} target="_blank" rel="noreferrer">Abrir
                WhatsApp</a>}</article>) : <Empty/>}</section>
        {open && <Modal title="Novo cliente" onClose={() => setOpen(false)}>
            <form className="form-grid" onSubmit={form.handleSubmit((data) => save.mutate(data))}><label>Nome<input
                required {...form.register('nome')}/></label><label>Telefone<input
                required {...form.register('telefone')}/></label><label>E-mail<input
                type="email" {...form.register('email')}/></label><label>Cidade<input {...form.register('cidade')}/></label><label>Endereço<input {...form.register('endereco')}/></label><label>CEP<input {...form.register('cep')}/></label><label>Status<select {...form.register('statusCadastral')}>
                <option value="ATIVO">Ativo</option>
                <option value="INATIVO">Inativo</option>
            </select></label>{save.isError && <p className="error">{save.error.message}</p>}
                <footer>
                    <button type="button" className="secondary" onClick={() => setOpen(false)}>Cancelar</button>
                    <button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar'}</button>
                </footer>
            </form>
        </Modal>}</>;
}