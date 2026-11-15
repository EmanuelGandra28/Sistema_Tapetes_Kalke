import {Fragment, useState} from 'react';
import {agruparPermissoes, CARGO_ROTULO, rotuloPermissao} from '../services/permissoes';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {api} from '../services/api';
import {Loading, Failure, Empty} from '../components/State';
import {useAuth} from '../contexts/AuthContext';

import {CARGOS} from '../services/codigos';
import {TEMAS} from '../utils/tema';

const novoUsuario = {nome: '', email: '', senha: '', cargo: 'VENDEDOR'};
// O DONO não pode perder acesso às configurações (o servidor também bloqueia).
const travada = (cargo, chave) => cargo === 'DONO' && (chave === 'configuracoes' || chave === 'configuracoes_editar');

function MatrizPermissoes({titulo, descricao, itens, cargos, valores, tipo, alterar}) {
    return <article className="panel"><h2>{titulo}</h2>{descricao && <p className="hint">{descricao}</p>}
        <div className="table-wrap"><table className="matrix"><thead><tr><th></th>{cargos.map((c) => <th key={c}>{CARGO_ROTULO[c] ?? c}</th>)}</tr></thead>
            <tbody>{agruparPermissoes(itens).map(({grupo, itens: doGrupo}) => <Fragment key={grupo}>
                <tr className="matrix-group"><th colSpan={cargos.length + 1}>{grupo}</th></tr>
                {doGrupo.map((item) => {
                    const {rotulo} = rotuloPermissao(item);
                    return <tr key={item}><td title={`Chave técnica: ${item}`}>{rotulo}</td>{cargos.map((c) => {
                        const chave = `${c}:${item}`;
                        const bloqueada = travada(c, item);
                        return <td key={c} title={bloqueada ? 'O Dono sempre mantém acesso às configurações' : undefined}>
                            <input type="checkbox" aria-label={`${CARGO_ROTULO[c] ?? c}: ${rotulo}`} disabled={bloqueada}
                                   checked={Boolean(valores[chave])} onChange={(e) => alterar(tipo, chave, e.target.checked)}/>{bloqueada && ' 🔒'}</td>;
                    })}</tr>;
                })}
            </Fragment>)}</tbody></table></div>
    </article>;
}

function EditorPermissoes() {
    const queryClient = useQueryClient();
    const q = useQuery({queryKey: ['permissoes-todas'], queryFn: () => api.get('/configuracoes/permissoes/todas')});
    const [mudancas, setMudancas] = useState({pages: {}, actions: {}});
    const save = useMutation({
        mutationFn: () => api.put('/configuracoes/permissoes', mudancas),
        onSuccess: () => { setMudancas({pages: {}, actions: {}}); queryClient.invalidateQueries({queryKey: ['permissoes-todas']}); queryClient.invalidateQueries({queryKey: ['configuracoes']}); }
    });
    if (q.isPending) return <Loading/>;
    if (q.isError) return <Failure error={q.error}/>;
    const {cargos, paginas, acoes, pages, actions} = q.data.data;
    const alterar = (tipo, chave, valor) => setMudancas((m) => ({...m, [tipo]: {...m[tipo], [chave]: valor}}));
    const total = Object.keys(mudancas.pages).length + Object.keys(mudancas.actions).length;
    return <>
        <section className="panel"><h2>Permissões por cargo</h2>
            <p>Marque o que cada cargo pode ver e fazer. As mudanças valem na próxima requisição de cada usuário.</p>
            <div className="row-actions">
                <button disabled={total === 0 || save.isPending} onClick={() => save.mutate()}>{save.isPending ? 'Salvando…' : `Salvar ${total} alteração(ões)`}</button>
                {total > 0 && <button className="secondary" onClick={() => setMudancas({pages: {}, actions: {}})}>Descartar</button>}
            </div>
            {save.isError && <p className="error">{save.error.message}</p>}
            {save.isSuccess && <p>Permissões salvas.</p>}
        </section>
        <section className="stack">
            <MatrizPermissoes titulo="Telas que cada cargo vê" descricao="Sem a tela, o item some do menu." itens={paginas} cargos={cargos} valores={{...pages, ...mudancas.pages}} tipo="pages" alterar={alterar}/>
            <MatrizPermissoes titulo="O que cada cargo pode fazer" descricao="Passe o mouse sobre um item para ver o nome técnico." itens={acoes} cargos={cargos} valores={{...actions, ...mudancas.actions}} tipo="actions" alterar={alterar}/>
        </section>
    </>;
}

function MinhaSenha() {
    const {endSession} = useAuth();
    const [form, setForm] = useState({senhaAtual: '', novaSenha: ''});
    const save = useMutation({
        mutationFn: () => api.put('/auth/senha', form),
        onSuccess: () => { window.alert('Senha alterada. Entre novamente com a nova senha.'); endSession(); }
    });
    return <section className="panel"><h2>Minha senha</h2>
        <form className="form-row" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
            <label>Senha atual<input required type="password" autoComplete="current-password" value={form.senhaAtual}
                                     onChange={(e) => setForm({...form, senhaAtual: e.target.value})}/></label>
            <label>Nova senha<input required type="password" minLength="8" autoComplete="new-password" value={form.novaSenha}
                                    onChange={(e) => setForm({...form, novaSenha: e.target.value})}/></label>
            <button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Alterar senha'}</button>
        </form>
        {save.isSuccess && <p>Senha alterada.</p>}
        {save.isError && <p className="error">{save.error.message}</p>}
    </section>;
}

function Usuarios() {
    const queryClient = useQueryClient();
    const [form, setForm] = useState(novoUsuario);
    const q = useQuery({queryKey: ['usuarios'], queryFn: () => api.get('/usuarios')});
    const refresh = () => queryClient.invalidateQueries({queryKey: ['usuarios']});
    const create = useMutation({mutationFn: () => api.post('/usuarios', form), onSuccess: () => { setForm(novoUsuario); refresh(); }});
    const update = useMutation({mutationFn: (u) => api.put(`/usuarios/${u.id}`, {nome: u.nome, cargo: u.cargo, ativo: u.ativo}), onSuccess: refresh});
    const reset = useMutation({mutationFn: ({id, senha}) => api.put(`/usuarios/${id}/senha`, {senha})});
    const resetSenha = (u) => {
        const senha = window.prompt(`Nova senha para ${u.nome} (mínimo 8 caracteres):`);
        if (senha) reset.mutate({id: u.id, senha});
    };
    const error = create.error ?? update.error ?? reset.error;
    return <section className="panel"><h2>Usuários</h2>
        <form className="form-row" onSubmit={(e) => { e.preventDefault(); create.mutate(); }}>
            <label>Nome<input required value={form.nome} onChange={(e) => setForm({...form, nome: e.target.value})}/></label>
            <label>E-mail<input required type="email" value={form.email} onChange={(e) => setForm({...form, email: e.target.value})}/></label>
            <label>Senha inicial<input required type="password" minLength="8" autoComplete="new-password" value={form.senha}
                                       onChange={(e) => setForm({...form, senha: e.target.value})}/></label>
            <label>Cargo<select value={form.cargo} onChange={(e) => setForm({...form, cargo: e.target.value})}>{CARGOS.map((c) =>
                <option key={c}>{c}</option>)}</select></label>
            <button disabled={create.isPending}>{create.isPending ? 'Criando…' : 'Criar usuário'}</button>
        </form>
        {error && <p className="error">{error.message}</p>}
        {reset.isSuccess && <p>Senha redefinida.</p>}
        {q.isPending ? <Loading/> : q.isError ? <Failure error={q.error}/> : q.data.data.length === 0 ? <Empty/> :
            <div className="table-wrap"><table><thead><tr><th>Nome</th><th>E-mail</th><th>Cargo</th><th>Status</th><th></th></tr></thead>
                <tbody>{q.data.data.map((u) => <tr key={u.id}>
                    <td>{u.nome}</td><td>{u.email}</td>
                    <td><select value={u.cargo} onChange={(e) => update.mutate({...u, cargo: e.target.value})}>{CARGOS.map((c) =>
                        <option key={c}>{c}</option>)}</select></td>
                    <td>{u.ativo ? 'Ativo' : 'Inativo'}</td>
                    <td><button type="button" className="secondary" onClick={() => update.mutate({...u, ativo: !u.ativo})}>{u.ativo ? 'Desativar' : 'Reativar'}</button>{' '}
                        <button type="button" className="secondary" onClick={() => resetSenha(u)}>Redefinir senha</button></td>
                </tr>)}</tbody></table></div>}
    </section>;
}

function EscolhaTema() {
    const {user, alterarTema} = useAuth();
    const [erro, setErro] = useState('');
    const atual = user?.tema ?? 'verde';
    const escolher = async (tema) => {
        setErro('');
        try { await alterarTema(tema); } catch (e) { setErro(e.message ?? 'Não foi possível salvar o tema.'); }
    };
    return <section className="panel"><h2>Tema</h2>
        <p className="muted">Cada pessoa escolhe o seu; a escolha fica salva na sua conta.</p>
        <div className="theme-grid">{TEMAS.map((t) => <button key={t.id} type="button" className="theme-option" aria-pressed={atual === t.id} onClick={() => escolher(t.id)}>
            <span className="theme-preview" aria-hidden="true"><span className="side" style={{background: t.cores.side}}/><span className="body" style={{background: t.cores.bg}}>
                <span className="line" style={{background: t.cores.line}}/><span className="line" style={{background: t.cores.line, width: '70%'}}/><span className="pill" style={{background: t.cores.pill}}/></span></span>
            <span>{t.nome}{atual === t.id ? ' ✓' : ''}</span><small>{t.descricao}</small>
        </button>)}</div>
        {erro && <p className="error">{erro}</p>}
    </section>;
}

function DadosLoja() {
    const queryClient = useQueryClient();
    const q = useQuery({queryKey: ['configuracoes', 'loja'], queryFn: () => api.get('/configuracoes/loja')});
    const [form, setForm] = useState(null);
    const atual = form ?? {pixChave: q.data?.data?.pixChave ?? '', pixNomeRecebedor: q.data?.data?.pixNomeRecebedor ?? '', pixCidade: q.data?.data?.pixCidade ?? ''};
    const set = (k) => (e) => setForm({...atual, [k]: e.target.value});
    const save = useMutation({mutationFn: () => api.put('/configuracoes/loja', atual), onSuccess: () => { setForm(null); queryClient.invalidateQueries({queryKey: ['configuracoes', 'loja']}); }});
    if (q.isPending) return <Loading/>;
    if (q.isError) return <Failure error={q.error}/>;
    return <section className="panel"><h2>Dados da loja (PIX)</h2>
        <p className="hint">Usados para gerar o QR Code PIX nas vendas. O sistema não confirma o pagamento: confira sempre no app do banco.</p>
        <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
            <label className="full">Chave PIX (CPF/CNPJ, e-mail, telefone com +55 ou chave aleatória)<input maxLength="77" value={atual.pixChave} onChange={set('pixChave')}/></label>
            <label>Nome do recebedor (até 25 letras)<input maxLength="25" value={atual.pixNomeRecebedor} onChange={set('pixNomeRecebedor')}/></label>
            <label>Cidade (até 15 letras)<input maxLength="15" value={atual.pixCidade} onChange={set('pixCidade')}/></label>
            {save.isError && <p className="error full">{save.error.message}</p>}
            {save.isSuccess && !form && <p className="full">Dados salvos.</p>}
            <footer><button disabled={save.isPending || !form}>{save.isPending ? 'Salvando…' : 'Salvar dados do PIX'}</button></footer>
        </form>
    </section>;
}

export default function Configuracoes() {
    const {user, canAction} = useAuth();
    const q = useQuery({queryKey: ['configuracoes'], queryFn: () => api.get('/configuracoes')});
    if (q.isPending) return <Loading/>;
    if (q.isError) return <Failure error={q.error}/>;
    const {pages, actions} = q.data.data;
    return <>
        <section className="page-heading">
            <div><h1>Configurações</h1><p>Permissões exibidas são as aplicadas pela API para o usuário atual.</p></div>
        </section>
        <MinhaSenha/>
        {user?.cargo === 'DONO' && <Usuarios/>}
        <EscolhaTema/>
        {canAction("configuracoes_editar") && <DadosLoja/>}
        {user?.cargo === 'DONO' ? <EditorPermissoes/> : <section className="split">
            <article className="panel"><h2>Acesso às páginas</h2>
                <ul className="permission-list">{Object.entries(pages).map(([name, allowed]) => <li key={name}>
                    <span title={name}>{rotuloPermissao(name).rotulo}</span><strong
                    className={allowed ? 'allowed' : 'denied'}>{allowed ? 'Permitido' : 'Negado'}</strong></li>)}</ul>
            </article>
            <article className="panel"><h2>Ações</h2>
                <ul className="permission-list">{Object.entries(actions).map(([name, allowed]) => <li key={name}>
                    <span title={name}>{rotuloPermissao(name).rotulo}</span><strong
                    className={allowed ? 'allowed' : 'denied'}>{allowed ? 'Permitido' : 'Negado'}</strong></li>)}</ul>
            </article>
        </section>}
    </>;
}
