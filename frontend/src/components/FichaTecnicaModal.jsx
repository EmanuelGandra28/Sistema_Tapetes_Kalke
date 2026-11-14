import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { money } from '../utils/format';
import Modal from './Modal';
import SearchSelect from './SearchSelect';
import { Loading, Failure } from './State';

/**
 * Ficha técnica de um produto acabado: matérias-primas e quantidade por UNIDADE produzida.
 * É dela que a ordem de produção automática tira as matérias-primas quando o estoque fica baixo.
 */
export default function FichaTecnicaModal({ produto, podeEditar, onClose }) {
  const q = useQuery({ queryKey: ['ficha-tecnica', produto.id], queryFn: () => api.get(`/produtos/${produto.id}/ficha-tecnica`) });
  const [linhas, setLinhas] = useState(null);
  const [materia, setMateria] = useState(null);
  const [quantidade, setQuantidade] = useState('');
  useEffect(() => { if (q.data && linhas === null) setLinhas(q.data.data.componentes); }, [q.data, linhas]);
  const save = useMutation({
    mutationFn: () => api.put(`/produtos/${produto.id}/ficha-tecnica`, { componentes: linhas.map((l) => ({ materiaPrimaId: l.materiaPrimaId, quantidade: Number(l.quantidade) })) }),
    onSuccess: onClose
  });
  const adicionar = () => {
    if (!materia || Number(quantidade) <= 0 || linhas.some((l) => l.materiaPrimaId === materia.id)) return;
    setLinhas([...linhas, { materiaPrimaId: materia.id, nome: materia.nome, unidadeMedida: materia.unidadeMedida, quantidade: Number(quantidade), estoqueProducao: materia.estoqueProducao, precoCusto: materia.precoCusto }]);
    setMateria(null); setQuantidade('');
  };
  const custo = (linhas ?? []).reduce((s, l) => s + Number(l.quantidade) * Number(l.precoCusto ?? 0), 0);
  return <Modal title={`Ficha técnica · ${produto.nome}`} onClose={onClose}>
    {q.isPending || linhas === null ? <Loading /> : q.isError ? <Failure error={q.error} /> : <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <p className="muted full">Quanto de cada matéria-prima vai em <strong>1 {produto.unidadeMedida.toLowerCase()}</strong> deste produto. Quando o estoque comercial ficar baixo ou esgotar, o sistema abre sozinho uma ordem de produção usando esta receita.</p>
      {linhas.length === 0 ? <p className="state full">Nenhuma matéria-prima cadastrada. Sem ficha técnica, a ordem automática não é criada.</p> :
        <ul className="cart full">{linhas.map((l, idx) => <li key={l.materiaPrimaId}>
          <span>{l.nome}<small className="muted"> · saldo de produção {l.estoqueProducao} {l.unidadeMedida}</small></span>
          <span className="row-actions">{podeEditar ? <input aria-label={`Quantidade de ${l.nome}`} type="number" min="0.001" step="0.001" style={{ width: '110px' }} value={l.quantidade}
            onChange={(e) => setLinhas(linhas.map((x, i) => (i === idx ? { ...x, quantidade: e.target.value } : x)))} /> : <strong>{l.quantidade}</strong>} {l.unidadeMedida}
            {podeEditar && <button type="button" className="link danger" onClick={() => setLinhas(linhas.filter((_, i) => i !== idx))}>Remover</button>}</span>
        </li>)}</ul>}
      {podeEditar && <fieldset className="full items-box"><legend>Adicionar matéria-prima</legend>
        <div className="form-row">
          <SearchSelect label="Matéria-prima" endpoint="/produtos" value={materia?.id} onChange={setMateria} filter={(p) => p.ativo && p.podeSerMateriaPrima && p.id !== produto.id && !linhas.some((l) => l.materiaPrimaId === p.id)} describe={(p) => `${p.nome} (${p.unidadeMedida})`} />
          <label>Qtd. por unidade<input type="number" min="0.001" step="0.001" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} /></label>
          <button type="button" className="secondary" onClick={adicionar} disabled={!materia || Number(quantidade) <= 0}>Adicionar matéria-prima</button>
        </div>
      </fieldset>}
      <p className="full"><strong>Custo de matéria-prima por unidade: {money(custo)}</strong> <small className="muted">(pelo preço de custo cadastrado de cada matéria-prima)</small></p>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Fechar</button>{podeEditar && <button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar ficha técnica'}</button>}</footer>
    </form>}
  </Modal>;
}
