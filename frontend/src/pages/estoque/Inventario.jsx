import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Loading, Failure, Empty } from '../../components/State';

// Contagem física: a pessoa digita o que contou; só as diferenças viram ajuste do tipo Inventário.
export default function Inventario() {
  const queryClient = useQueryClient();
  const [saldo, setSaldo] = useState('COMERCIAL');
  const [contado, setContado] = useState({});
  const [observacao, setObservacao] = useState('');
  const [resumo, setResumo] = useState(null);
  const q = useQuery({ queryKey: ['/produtos', 'inventario'], queryFn: () => api.get('/produtos', { params: { size: 100 } }) });
  const produtos = (q.data?.data?.content ?? []).filter((p) => p.ativo && (saldo === 'COMERCIAL' ? p.podeSerVendido : p.podeSerMateriaPrima));
  const sistema = (p) => Number(saldo === 'COMERCIAL' ? p.estoqueComercial : p.estoqueProducao);
  const preenchidos = produtos.filter((p) => contado[p.id] !== undefined && contado[p.id] !== '');
  const comDiferenca = preenchidos.filter((p) => Number(contado[p.id]) !== sistema(p));
  const aplicar = useMutation({
    mutationFn: () => api.post('/estoque/inventario', { saldo, observacao: observacao.trim() || null, itens: preenchidos.map((p) => ({ produtoId: p.id, contado: Number(contado[p.id]) })) }),
    onSuccess: (r) => { setResumo(r.data); setContado({}); queryClient.invalidateQueries({ queryKey: ['/produtos'] }); queryClient.invalidateQueries({ queryKey: ['movimentos'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); }
  });
  const confirmar = () => { if (window.confirm(`Aplicar a contagem? ${comDiferenca.length} produto(s) com diferença terão o estoque corrigido.`)) aplicar.mutate(); };
  const trocarSaldo = (e) => { setSaldo(e.target.value); setContado({}); setResumo(null); };
  return <>
    <section className="panel filters">
      <label>Saldo contado<select value={saldo} onChange={trocarSaldo}><option value="COMERCIAL">Comercial (loja)</option><option value="PRODUCAO">Produção (matéria-prima)</option></select></label>
      <label className="grow">Observação<input maxLength="300" placeholder="Ex.: contagem de fim de mês" value={observacao} onChange={(e) => setObservacao(e.target.value)} /></label>
      <button type="button" disabled={!preenchidos.length || aplicar.isPending} onClick={confirmar}>{aplicar.isPending ? 'Aplicando…' : `Aplicar contagem (${comDiferenca.length} diferença(s))`}</button>
    </section>
    {aplicar.isError && <p className="error">{aplicar.error.message}</p>}
    {resumo && <section className="panel"><h2>Contagem aplicada</h2><p>{resumo.contados} produto(s) contados, {resumo.comDiferenca} corrigido(s).</p>
      {resumo.diferencas.length > 0 && <ul>{resumo.diferencas.map((d) => <li key={d.produto}>{d.produto}: {d.sistema} → {d.contado} ({Number(d.diferenca) > 0 ? '+' : ''}{d.diferenca})</li>)}</ul>}</section>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : produtos.length ? <div className="table-wrap"><table className="inventario"><thead><tr><th>Produto</th><th>No sistema</th><th>Contado</th><th>Diferença</th></tr></thead><tbody>
      {produtos.map((p) => { const valor = contado[p.id] ?? ''; const dif = valor === '' ? null : Number(valor) - sistema(p);
        return <tr key={p.id}><td>{p.nome}<small>{p.unidadeMedida}</small></td><td>{sistema(p)}</td>
          <td><input type="number" min="0" step="0.001" aria-label={`Contado de ${p.nome}`} value={valor} onChange={(e) => setContado({ ...contado, [p.id]: e.target.value })} /></td>
          <td className={dif === null || dif === 0 ? undefined : dif > 0 ? 'qtd-entrada' : 'qtd-saida'}>{dif === null ? '—' : dif === 0 ? 'ok' : `${dif > 0 ? '+' : ''}${Number(dif.toFixed(3))}`}</td></tr>; })}
    </tbody></table></div> : <Empty>Nenhum produto ativo para este saldo.</Empty>}</section>
  </>;
}
