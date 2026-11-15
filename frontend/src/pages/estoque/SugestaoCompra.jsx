import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Loading, Failure, Empty } from '../../components/State';
import { money } from '../../utils/format';

// Lista do que comprar de matéria-prima: ordens abertas + estoque mínimo de produção − saldo.
export default function SugestaoCompra({ exportar }) {
  const q = useQuery({ queryKey: ['sugestao-compra'], queryFn: () => api.get('/estoque/sugestao-compra') });
  const rows = q.data?.data ?? [];
  const total = rows.reduce((s, r) => s + Number(r.custoEstimado), 0);
  return <>
    <section className="panel filters">
      <p className="hint grow">Soma o que as ordens de produção abertas vão consumir com o estoque mínimo de produção e desconta o que já existe. Só aparece o que falta.</p>
      {exportar && <button type="button" className="secondary" onClick={() => exportar('/estoque/sugestao-compra/exportar', {}, 'sugestao-compra')}>Exportar Excel</button>}
    </section>
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <>
      <div className="table-wrap"><table><thead><tr><th>Matéria-prima</th><th>Em estoque</th><th>Ordens abertas</th><th>Mínimo</th><th>Comprar</th><th>Fornecedor</th><th>Custo estimado</th></tr></thead><tbody>
        {rows.map((r) => <tr key={r.produtoId}><td>{r.nome}<small>{r.unidade}</small></td><td>{r.saldo}</td><td>{r.comprometido}</td><td>{r.minimo}</td>
          <td className="qtd-saida">{Number(Number(r.falta).toFixed(3))} {r.unidade}</td><td>{r.fornecedor ?? '—'}</td><td>{money(r.custoEstimado)}</td></tr>)}
      </tbody></table></div>
      <p className="total-line"><strong>Total estimado: {money(total)}</strong></p>
    </> : <Empty>Nada para comprar agora: as matérias-primas cobrem as ordens abertas e o estoque mínimo.</Empty>}</section>
  </>;
}
