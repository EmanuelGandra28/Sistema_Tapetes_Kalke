import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useDebounce } from '../hooks/useDebounce';
import { api } from '../services/api';
import { useCodigos } from '../services/codigos';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import FichaTecnicaModal from '../components/FichaTecnicaModal';
import Abas from '../components/Abas';
import { useExportar } from '../hooks/useExportar';
import Movimentacoes from './estoque/Movimentacoes';
import Inventario from './estoque/Inventario';
import SugestaoCompra from './estoque/SugestaoCompra';
import { useAuth } from '../contexts/AuthContext';
import { money } from '../utils/format';
import Paginacao from '../components/Paginacao';
import Dica from '../components/Dica';
import { DICAS } from '../content/ajuda';
import FotoProduto, { useFotosPrincipais } from '../components/FotoProduto';
import FotosProdutoModal from '../components/FotosProdutoModal';

import { produtoDefaults as defaults, produtoParaForm as toForm, produtoParaPayload as toPayload } from '../utils/produto';

function AjusteModal({ produto, onClose, onDone }) {
  const [saldo, setSaldo] = useState('COMERCIAL'); const [quantidade, setQuantidade] = useState(''); const [observacao, setObservacao] = useState('');
  const save = useMutation({ mutationFn: () => api.post('/estoque/ajuste', { produtoId: produto.id, saldo, quantidade: Number(quantidade), observacao }), onSuccess: () => { onDone(); onClose(); } });
  return <Modal title={`Ajustar estoque · ${produto.nome}`} onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <label>Saldo<select value={saldo} onChange={(e) => setSaldo(e.target.value)}><option value="COMERCIAL">Comercial ({produto.estoqueComercial})</option><option value="PRODUCAO">Produção ({produto.estoqueProducao})</option></select></label>
      <label>Quantidade (use negativo para baixa)<input required type="number" step="0.001" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} /></label>
      <label className="full">Motivo<input required maxLength="300" value={observacao} onChange={(e) => setObservacao(e.target.value)} /></label>
      {save.isError && <p className="error full">{save.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Fechar</button><button disabled={save.isPending || !Number(quantidade)}>{save.isPending ? 'Salvando…' : 'Salvar ajuste'}</button></footer>
    </form>
  </Modal>;
}

export default function Produtos() {
  const [aba, setAba] = useState('produtos');
  const exportar = useExportar();
  const [editing, setEditing] = useState(null); // null = fechado, {} = novo, produto = edição
  const [ajuste, setAjuste] = useState(null);
  const [ficha, setFicha] = useState(null);
  const [fotos, setFotos] = useState(null);
  const fotosPrincipais = useFotosPrincipais();
  const [search, setSearch] = useState('');
  const [pagina, setPagina] = useState(0);
  const termo = useDebounce(search.trim());
  const { canAction } = useAuth();
  const { tiposProduto, unidadesMedida } = useCodigos();
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['/produtos', 'lista', termo, pagina], queryFn: () => api.get('/produtos', { params: { search: termo, page: pagina, size: 25 } }), placeholderData: keepPreviousData });
  const fornecedores = useQuery({ queryKey: ['fornecedores'], queryFn: () => api.get('/fornecedores'), enabled: editing !== null });
  const form = useForm({ defaultValues: defaults });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['/produtos'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); };
  const open = (p) => { form.reset(p?.id ? toForm(p) : defaults); setEditing(p ?? {}); };
  const close = () => setEditing(null);
  const save = useMutation({ mutationFn: (data) => editing?.id ? api.put(`/produtos/${editing.id}`, toPayload(data)) : api.post('/produtos', toPayload(data)), onSuccess: () => { close(); refresh(); } });
  const remove = useMutation({ mutationFn: (id) => api.delete(`/produtos/${id}`), onSuccess: refresh });
  const atualizarSite = useMutation({ mutationFn: () => api.post('/portfolio/publicar') });
  const modoSite = form.watch('portfolioModo');
  const excluir = (p) => { if (window.confirm(`Excluir o produto "${p.nome}"? O histórico de vendas é mantido.`)) remove.mutate(p.id); };
  const options = (list, fallback) => (list.length ? list : fallback.map((codigo) => ({ codigo, nome: codigo }))).map((o) => <option key={o.codigo} value={o.codigo}>{o.nome}</option>);
  const rows = q.data?.data?.content ?? [];
  const abas = [{ id: 'produtos', rotulo: 'Produtos' }, { id: 'movimentacoes', rotulo: 'Movimentações' }, ...(canAction('estoque_ajustar') ? [{ id: 'inventario', rotulo: 'Inventário' }] : []), { id: 'sugestao', rotulo: 'Sugestão de compra' }];
  return <>
    <section className="page-heading"><div><h1>Estoque</h1><p>Produtos, dois saldos e alertas.</p></div>{aba === 'produtos' && <div className="row-actions"><button type="button" className="secondary" onClick={() => exportar('/estoque/exportar', {}, 'estoque')}>Exportar Excel</button>{canAction('portfolio_publicar') && <button type="button" className="secondary" disabled={atualizarSite.isPending} onClick={() => { if (window.confirm('Atualizar o site agora? As mudanças aparecem em 2 a 5 minutos.')) atualizarSite.mutate(); }}>{atualizarSite.isPending ? 'Pedindo…' : 'Atualizar site'}</button>}{canAction('produtos_criar') && <button onClick={() => open(null)}>Novo produto</button>}</div>}</section>
    {atualizarSite.isSuccess && <p className="hint" role="status">Pedido enviado! O site é atualizado em 2 a 5 minutos.</p>}
    {atualizarSite.isError && <p className="error" role="alert">{atualizarSite.error.message}</p>}
    <Abas abas={abas} ativa={aba} onChange={setAba} />
    {aba === 'movimentacoes' && <Movimentacoes exportar={exportar} />}
    {aba === 'inventario' && <Inventario />}
    {aba === 'sugestao' && <SugestaoCompra exportar={exportar} />}
    {aba === 'produtos' && <>
    <input className="search" placeholder="Buscar por nome ou código de barras" value={search} onChange={(e) => { setSearch(e.target.value); setPagina(0); }} />
    {remove.isError && <p className="error">{remove.error.message}</p>}
    <section className="panel">{q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : rows.length ? <div className="table-wrap"><table><thead><tr><th>Produto</th><th>Comercial</th><th>Produção</th><th>Custo</th><th>Venda</th><th>Status</th><th></th></tr></thead><tbody>
      {rows.map((p) => <tr key={p.id}><td><div className="celula-produto"><FotoProduto imagemId={fotosPrincipais[p.id]} nome={p.nome} /><div>{p.nome}<small>{p.unidadeMedida}{p.ativo ? '' : ' · inativo'}</small></div></div></td><td>{p.estoqueComercial}</td><td>{p.estoqueProducao}</td><td>{money(p.precoCusto)}</td><td>{money(p.precoVenda)}</td>
        <td><span className={`badge ${p.statusComercial.toLowerCase()}`}>{p.statusComercial}</span></td>
        <td className="row-actions">
          {canAction('produtos_editar') && <button className="link" onClick={() => open(p)}>Editar</button>}
          {canAction('estoque_ajustar') && <button className="link" onClick={() => setAjuste(p)}>Ajustar estoque</button>}
          <button className="link" onClick={() => setFotos(p)}>Fotos</button>
          {p.podeSerVendido && <button className="link" onClick={() => setFicha(p)}>Ficha técnica</button>}
          {canAction('produtos_excluir') && <button className="link danger" onClick={() => excluir(p)}>Excluir</button>}
        </td></tr>)}
    </tbody></table></div> : <Empty />}<Paginacao page={pagina} data={q.data?.data} onChange={setPagina} /></section>
    </>}
    {ajuste && <AjusteModal produto={ajuste} onClose={() => setAjuste(null)} onDone={refresh} />}
    {fotos && <FotosProdutoModal produto={fotos} podeEditar={canAction('produtos_imagens')} onClose={() => setFotos(null)} />}
    {ficha && <FichaTecnicaModal produto={ficha} podeEditar={canAction('produtos_editar')} onClose={() => setFicha(null)} />}
    {editing !== null && <Modal title={editing.id ? 'Editar produto' : 'Novo produto'} onClose={close}>
      <form className="form-grid" onSubmit={form.handleSubmit((data) => save.mutate(data))}>
        <label className="full">Nome<input required maxLength="180" {...form.register('nome')} /></label>
        <label>Tipo<select {...form.register('tipo')}>{options(tiposProduto, ['TAPETE', 'MALHA'])}</select></label>
        <label>Unidade<select {...form.register('unidadeMedida')}>{options(unidadesMedida, ['UNIDADE', 'METRO', 'KG'])}</select></label>
        <label>Categoria<input {...form.register('categoria')} /></label>
        <label>Cor<input {...form.register('cor')} /></label>
        <label>Tamanho<input {...form.register('tamanho')} /></label>
        <label>Código de barras<input {...form.register('codigoBarras')} /></label>
        <label>Preço de venda<input required type="number" min="0" step="0.01" {...form.register('precoVenda')} /></label>
        <label>Preço de custo<input type="number" min="0" step="0.01" {...form.register('precoCusto')} /></label>
        <label>Estoque mínimo comercial <Dica texto={DICAS.estoqueMinimo} /><input type="number" min="0" step="0.001" {...form.register('estoqueMinimoComercial')} /></label>
        <label>Estoque mínimo produção <Dica texto={DICAS.estoqueMinimo} /><input type="number" min="0" step="0.001" {...form.register('estoqueMinimoProducao')} /></label>
        <label className="full">Fornecedor principal<select {...form.register('fornecedorPrincipalId')}><option value="">Nenhum</option>{(fornecedores.data?.data ?? []).map((f) => <option key={f.id} value={f.id}>{f.nomeEmpresa}</option>)}</select></label>
        <label className="full">Observações<input {...form.register('observacoes')} /></label>
        <h3 className="full">Site (portfólio)</h3>
        <label>Aparece no site?<select {...form.register('portfolioModo')}><option value="NENHUM">Não aparece</option><option value="VENDA">Sim, à venda (com preço)</option><option value="GALERIA">Só como foto na galeria</option></select></label>
        {modoSite !== 'NENHUM' && <>
          <label>Linha<select {...form.register('portfolioLinha')} required={modoSite === 'VENDA'}><option value="">Escolha…</option><option value="BANHEIRO">Banheiro</option><option value="COZINHA">Cozinha</option><option value="SALA">Sala</option><option value="OUTRA">Outra</option></select></label>
          <label className="full">Descrição para o site<input maxLength="500" placeholder="Ex.: Tulipas vermelhas e amarelas, feitas à mão" {...form.register('portfolioDescricao')} /></label>
          <label>Ordem de destaque<input type="number" min="0" step="1" {...form.register('portfolioOrdem')} /></label>
          <label>Selo<select {...form.register('portfolioSelo')}><option value="">Nenhum</option><option value="Novo">Novo</option><option value="Mais pedido">Mais pedido</option></select></label>
          <p className="hint full">O preço no site é o preço de venda. As fotos vêm do botão "Fotos". Mudanças aparecem no site na próxima atualização (automática a cada 6 horas ou pelo botão "Atualizar site").</p>
        </>}
        <label className="check"><input type="checkbox" {...form.register('podeSerVendido')} /> Pode ser vendido</label>
        <label className="check"><input type="checkbox" {...form.register('podeSerMateriaPrima')} /> Pode ser matéria-prima</label>
        <label className="check"><input type="checkbox" {...form.register('ativo')} /> Ativo</label>
        {save.isError && <p className="error full">{save.error.message}</p>}
        <footer><button type="button" className="secondary" onClick={close}>Fechar</button><button disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar produto'}</button></footer>
      </form>
    </Modal>}
  </>;
}
