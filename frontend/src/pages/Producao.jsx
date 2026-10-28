import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { ask } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import SearchSelect from '../components/SearchSelect';
import { useAuth } from '../contexts/AuthContext';
import { date } from '../utils/format';

function NovaOrdem({ onClose, onDone }) {
  const [acabado, setAcabado] = useState(null);
  const [quantidadeProduzida, setQuantidadeProduzida] = useState(1);
  const [tipoOrdem, setTipoOrdem] = useState('ESTOQUE');
  const [cliente, setCliente] = useState(null);
  const [observacoes, setObservacoes] = useState('');
  const [materia, setMateria] = useState(null);
  const [quantidadeUsada, setQuantidadeUsada] = useState(1);
  const [materias, setMaterias] = useState([]);
  const save = useMutation({ mutationFn: () => api.post('/ordens-producao', { produtoAcabadoId: acabado.id, quantidadeProduzida: Number(quantidadeProduzida), tipoOrdem, clienteId: tipoOrdem === 'ENCOMENDA' ? cliente?.id : null, observacoes, materiasPrimas: materias.map(({ produtoId, quantidadeUsada: q }) => ({ produtoId, quantidadeUsada: q })) }), onSuccess: () => { onDone(); onClose(); } });
  const add = () => {
    if (!materia || Number(quantidadeUsada) <= 0) return;
    setMaterias((old) => [...old, { produtoId: materia.id, nome: materia.nome, unidade: materia.unidadeMedida, saldo: materia.estoqueProducao, quantidadeUsada: Number(quantidadeUsada) }]);
    setMateria(null);
  };
  return <Modal title="Nova ordem de produção" onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <div className="full"><SearchSelect label="Produto acabado" endpoint="/produtos" value={acabado?.id} onChange={setAcabado} filter={(p) => p.ativo} required /></div>
      <label>Quantidade a produzir<input required type="number" min="0.001" step="0.001" value={quantidadeProduzida} onChange={(e) => setQuantidadeProduzida(e.target.value)} /></label>
      <label>Tipo<select value={tipoOrdem} onChange={(e) => setTipoOrdem(e.target.value)}><option value="ESTOQUE">Para estoque</option><option value="ENCOMENDA">Encomenda</option></select></label>
      {tipoOrdem === 'ENCOMENDA' && <div className="full"><SearchSelect label="Cliente da encomenda" endpoint="/clientes" value={cliente?.id} onChange={setCliente} required /></div>}
      <label className="full">Observações<input value={observacoes} onChange={(e) => setObservacoes(e.target.value)} /></label>
      <fieldset className="full items-box"><legend>Matérias-primas (saem do estoque de produção ao finalizar)</legend>
        <div className="form-row">
          <SearchSelect label="Matéria-prima" endpoint="/produtos" value={materia?.id} onChange={setMateria} filter={(p) => p.ativo && p.podeSerMateriaPrima && p.id !== acabado?.id} describe={(p) => `${p.nome} · ${p.estoqueProducao} ${p.unidadeMedida}`} />
          <label>Qtd.<input type="number" min="0.001" step="0.001" value={quantidadeUsada} onChange={(e) => setQuantidadeUsada(e.target.value)} /></label>
          <button type="button" className="secondary" onClick={add} disabled={!materia}>Adicionar</button>
        </div>
        {materias.length > 0 && <ul className="cart">{materias.map((m, idx) => <li key={idx}><span>{m.nome}: {m.quantidadeUsada} {m.unidade} (saldo {m.saldo})</span><button type="button" className="link" onClick={() => setMaterias(materias.filter((_, x) => x !== idx))}>Remover</button></li>)}</ul>}
      </fieldset>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Cancelar</button><button disabled={save.isPending || !acabado || materias.length === 0 || (tipoOrdem === 'ENCOMENDA' && !cliente)}>{save.isPending ? 'Salvando…' : 'Abrir ordem'}</button></footer>
    </form>
  </Modal>;
}

export default function Producao() {
  const [open, setOpen] = useState(false);
  const { canAction } = useAuth();
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['ordens'], queryFn: () => api.get('/ordens-producao') });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['ordens'] }); queryClient.invalidateQueries({ queryKey: ['/produtos'] }); };
  const finish = useMutation({ mutationFn: (id) => api.post(`/ordens-producao/${id}/finalizar`), onSuccess: refresh });
  const cancel = useMutation({ mutationFn: ({ id, motivo }) => api.post(`/ordens-producao/${id}/cancelar`, { motivo }), onSuccess: refresh });
  const finalizar = (o) => { if (window.confirm(`Finalizar a ordem #${o.numeroOrdem}? As matérias-primas serão baixadas e ${o.quantidade} unidade(s) entrarão no estoque comercial.`)) finish.mutate(o.id); };
  const cancelar = (o) => { const motivo = ask(`Motivo do cancelamento da ordem #${o.numeroOrdem}:`); if (motivo) cancel.mutate({ id: o.id, motivo }); };
  const error = finish.error ?? cancel.error;
  const rows = q.data?.data ?? [];
  return <>
    <section className="page-heading"><div><h1>Ordens de produção</h1><p>Consumo de matéria-prima e entrada do produto acabado.</p></div>{canAction('producao_criar') && <button onClick={() => setOpen(true)}>Nova ordem</button>}</section>
    {error && <p className="error">{error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Nº</th><th>Produto</th><th>Quantidade</th><th>Data</th><th>Status</th><th></th></tr></thead><tbody>
      {rows.map((o) => <tr key={o.id}><td>#{o.numeroOrdem}</td><td>{o.produto}</td><td>{o.quantidade}</td><td>{date(o.data)}</td><td><span className={`badge ${o.status === 'CANCELADA' ? 'inativo' : o.status === 'ABERTA' ? 'atencao' : ''}`}>{o.status}</span></td>
        <td className="row-actions">{o.status === 'ABERTA' && <>
          {canAction('producao_finalizar') && <button className="link" onClick={() => finalizar(o)} disabled={finish.isPending}>Finalizar</button>}
          {canAction('producao_cancelar') && <button className="link" onClick={() => cancelar(o)}>Cancelar</button>}
        </>}</td></tr>)}
    </tbody></table></div> : <Empty />}</section>
    {open && <NovaOrdem onClose={() => setOpen(false)} onDone={refresh} />}
  </>;
}
