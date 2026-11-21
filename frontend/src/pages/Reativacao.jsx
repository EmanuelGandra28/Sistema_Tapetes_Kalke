import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Loading, Failure, Empty } from '../components/State';
import Modal from '../components/Modal';
import Abas from '../components/Abas';
import { date, money } from '../utils/format';
import { CICLO, nivelCota, RESPOSTA, resumoAprovacao, STATUS_CAMPANHA, STATUS_DESTINATARIO, taxa, VALOR } from '../utils/reativacao';

function PainelStatus() {
  const q = useQuery({ queryKey: ['reativacao', 'status'], queryFn: () => api.get('/reativacao/status'), refetchInterval: 60000 });
  if (!q.data) return null;
  const s = q.data.data;
  const nivel = nivelCota(s.enviadasNoMes, s.cotaMensal, s.bloqueioNaCota);
  return <section className="cards">
    <article><span>Envio de mensagens</span><strong className={s.configurado ? '' : 'qtd-saida'}>{s.configurado ? 'Ligado' : 'Desligado'}</strong>
      <small>{s.configurado ? 'O gateway de WhatsApp está configurado.' : 'Falta a chave do gateway no servidor: nada será enviado.'}</small></article>
    <article><span>Cota do mês</span><strong className={nivel === 'bloqueada' ? 'qtd-saida' : undefined}>{s.enviadasNoMes} de {s.cotaMensal}</strong>
      <small>{nivel === 'bloqueada' ? 'Envios pausados para não estourar o plano.' : `Os envios param em ${s.bloqueioNaCota} mensagens.`}</small></article>
    <article><span>Horário de envio</span><strong>{s.dentroDoHorario ? 'Agora pode enviar' : 'Fora do horário'}</strong><small>Das 8h às 20h, todos os dias.</small></article>
    <article><span>Respostas dos clientes</span><strong>{s.webhookConfigurado ? 'Recebendo' : 'Não configurado'}</strong>
      <small>{s.webhookConfigurado ? 'Respostas e pedidos de saída são registrados.' : 'Falta o segredo do webhook no servidor.'}</small></article>
  </section>;
}

function Resultados({ campanha, onClose }) {
  const q = useQuery({ queryKey: ['reativacao', 'resultados', campanha.id], queryFn: () => api.get(`/reativacao/campanhas/${campanha.id}/resultados`) });
  return <Modal title={`Resultados — ${campanha.nome}`} onClose={onClose}>
    {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : (() => {
      const r = q.data.data;
      return <>
        <section className="cards compact">
          <article><span>Clientes</span><strong>{r.campanha.destinatarios}</strong></article>
          <article><span>Mensagens enviadas</span><strong>{r.enviadas}</strong></article>
          <article><span>Responderam</span><strong>{r.responderam}</strong><small>{taxa(r.responderam, r.campanha.destinatarios)}% dos clientes</small></article>
          <article><span>Voltaram a comprar</span><strong>{r.recompraram}</strong><small>{taxa(r.recompraram, r.campanha.destinatarios)}% (em até 30 dias)</small></article>
        </section>
        <p className="hint">Interessados: {r.interessados} · Sem interesse: {r.semInteresse} · Pediram para sair: {r.pediramSair}</p>
        {r.destinatarios.length ? <div className="table-wrap"><table><thead><tr><th>Cliente</th><th>Situação</th><th>Mensagem</th><th>Resposta</th></tr></thead><tbody>
          {r.destinatarios.map((d) => <tr key={d.id}><td>{d.cliente}<small>{d.telefone}</small></td>
            <td>{STATUS_DESTINATARIO[d.status] ?? d.status}{d.recomprou && <span className="badge faixa-boa"> Comprou de novo</span>}</td><td>{d.etapa} de 3</td>
            <td>{d.resposta ? <><span className="badge tipo-atendimento">{RESPOSTA[d.resposta]}</span><small>{d.respostaTexto}</small>{d.resposta !== 'PEDIU_SAIR' && d.telefone && <small>Responda pelo WhatsApp da loja.</small>}</> : '—'}</td></tr>)}
        </tbody></table></div> : <Empty />}
      </>;
    })()}
    <footer><button type="button" className="secondary" onClick={onClose}>Fechar</button></footer>
  </Modal>;
}

function NovaCampanha({ onClose, onDone }) {
  const [nome, setNome] = useState('');
  const [intervaloDias, setIntervalo] = useState(5);
  const [limiteDiario, setLimite] = useState(20);
  const [escolhidos, setEscolhidos] = useState(new Set());
  const q = useQuery({ queryKey: ['reativacao', 'candidatos'], queryFn: () => api.get('/reativacao/candidatos') });
  const criar = useMutation({
    mutationFn: () => api.post('/reativacao/campanhas', { nome, clienteIds: [...escolhidos], intervaloDias: Number(intervaloDias), limiteDiario: Number(limiteDiario) }),
    onSuccess: () => { onDone(); onClose(); }
  });
  const lista = q.data?.data ?? [];
  const alternar = (id) => setEscolhidos((s) => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n; });
  return <Modal title="Nova campanha de reativação" onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); criar.mutate(); }}>
      <label className="full">Nome da campanha<input required maxLength="120" value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Ex.: Clientes de novembro" /></label>
      <label>Dias entre as mensagens<input type="number" min="1" max="60" value={intervaloDias} onChange={(e) => setIntervalo(e.target.value)} /></label>
      <label>Máximo de mensagens por dia<input type="number" min="1" max="200" value={limiteDiario} onChange={(e) => setLimite(e.target.value)} /></label>
      <div className="full">
        <p className="hint">Só aparecem clientes com 2 meses ou mais sem comprar, telefone válido e <strong>autorização para receber mensagens</strong> registrada. Os textos das 3 mensagens são editados em Configurações → Mensagens.</p>
        {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : lista.length === 0 ? <Empty>Nenhum cliente pode receber agora. Registre a autorização na tela de Clientes.</Empty> : <>
          <p><button type="button" className="link" onClick={() => setEscolhidos(escolhidos.size === lista.length ? new Set() : new Set(lista.map((c) => c.id)))}>{escolhidos.size === lista.length ? 'Desmarcar todos' : 'Marcar todos'}</button> · {escolhidos.size} de {lista.length} escolhidos</p>
          <div className="table-wrap"><table><thead><tr><th></th><th>Cliente</th><th>Sem comprar</th><th>Valor</th><th>Já comprou</th></tr></thead><tbody>
            {lista.map((c) => <tr key={c.id}><td><input type="checkbox" aria-label={`Escolher ${c.nome}`} checked={escolhidos.has(c.id)} onChange={() => alternar(c.id)} /></td>
              <td>{c.nome}<small>{c.telefone}</small></td><td><span className="badge" title={CICLO[c.ciclo].dica}>{CICLO[c.ciclo].rotulo}</span><small>desde {date(c.ultimaCompra)}</small></td>
              <td><span className="badge auto" title={VALOR[c.valor].dica}>{VALOR[c.valor].rotulo}</span></td><td>{money(c.totalCompras)}</td></tr>)}
          </tbody></table></div></>}
      </div>
      {criar.isError && <p className="error full">{criar.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Cancelar</button>
        <button disabled={criar.isPending || escolhidos.size === 0 || !nome.trim()}>{criar.isPending ? 'Criando…' : 'Criar campanha (rascunho)'}</button></footer>
    </form>
  </Modal>;
}

function Campanhas() {
  const queryClient = useQueryClient();
  const { canAction } = useAuth();
  const [nova, setNova] = useState(false);
  const [verResultados, setVerResultados] = useState(null);
  const q = useQuery({ queryKey: ['reativacao', 'campanhas'], queryFn: () => api.get('/reativacao/campanhas') });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['reativacao'] });
  const acao = useMutation({ mutationFn: ({ id, nome }) => api.post(`/reativacao/campanhas/${id}/${nome}`), onSuccess: refresh });
  const gerenciar = canAction('reativacao_gerenciar');
  const aprovar = (c) => { if (window.confirm(`Aprovar a campanha "${c.nome}"?\n\n${resumoAprovacao(c)}`)) acao.mutate({ id: c.id, nome: 'aprovar' }); };
  const lista = q.data?.data ?? [];
  return <>
    <section className="page-heading"><div><h2>Campanhas</h2><p className="hint">Nada é enviado antes de você aprovar. Cada cliente recebe até 3 mensagens e, se responder, para de receber.</p></div>
      {gerenciar && <button onClick={() => setNova(true)}>Nova campanha</button>}</section>
    {acao.isError && <p className="error">{acao.error.message}</p>}
    {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : lista.length === 0 ? <Empty>Nenhuma campanha ainda.</Empty> :
      <ul className="lista-mensagens">{lista.map((c) => <li key={c.id}>
        <div><strong>{c.nome}</strong> <span className={`badge ${c.status === 'APROVADA' ? 'faixa-boa' : c.status === 'CANCELADA' ? 'inativo' : 'auto'}`}>{STATUS_CAMPANHA[c.status].rotulo}</span>
          <p className="trecho">{c.destinatarios} clientes · {c.enviadas} mensagens enviadas · {c.intervaloDias} dias entre mensagens · até {c.limiteDiario} por dia</p>
          <small className="hint">{STATUS_CAMPANHA[c.status].ajuda}</small></div>
        <div className="row-actions">
          <button type="button" className="secondary" onClick={() => setVerResultados(c)}>Ver resultados</button>
          {gerenciar && (c.status === 'RASCUNHO' || c.status === 'PAUSADA') && <button type="button" onClick={() => aprovar(c)} disabled={acao.isPending}>Aprovar e iniciar</button>}
          {gerenciar && c.status === 'APROVADA' && <button type="button" className="secondary" onClick={() => acao.mutate({ id: c.id, nome: 'pausar' })} disabled={acao.isPending}>Pausar</button>}
          {gerenciar && !['CONCLUIDA', 'CANCELADA'].includes(c.status) && <button type="button" className="link danger" onClick={() => window.confirm(`Cancelar a campanha "${c.nome}"? Nada mais será enviado.`) && acao.mutate({ id: c.id, nome: 'cancelar' })}>Cancelar campanha</button>}
        </div></li>)}</ul>}
    {nova && <NovaCampanha onClose={() => setNova(false)} onDone={refresh} />}
    {verResultados && <Resultados campanha={verResultados} onClose={() => setVerResultados(null)} />}
  </>;
}

function QuemPodeReceber() {
  const q = useQuery({ queryKey: ['reativacao', 'candidatos'], queryFn: () => api.get('/reativacao/candidatos') });
  const lista = q.data?.data ?? [];
  return <section className="panel"><h2>Quem pode receber agora</h2>
    <p className="hint">Clientes com 2 meses ou mais sem comprar, telefone válido e autorização registrada, que não estão em outra campanha. Para um cliente aparecer aqui, registre a autorização dele na tela de Clientes.</p>
    {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : lista.length === 0 ? <Empty>Ninguém no momento.</Empty> : <div className="table-wrap"><table><thead><tr><th>Cliente</th><th>Tempo sem comprar</th><th>Valor</th><th>Total comprado</th></tr></thead><tbody>
      {lista.map((c) => <tr key={c.id}><td>{c.nome}<small>{c.telefone}</small></td><td><span className="badge" title={CICLO[c.ciclo].dica}>{CICLO[c.ciclo].rotulo}</span><small>{c.diasSemComprar} dias (desde {date(c.ultimaCompra)})</small></td>
        <td><span className="badge auto" title={VALOR[c.valor].dica}>{VALOR[c.valor].rotulo}</span></td><td>{money(c.totalCompras)}</td></tr>)}
    </tbody></table></div>}</section>;
}

const ABAS = [{ id: 'campanhas', rotulo: 'Campanhas' }, { id: 'candidatos', rotulo: 'Quem pode receber' }];

export default function Reativacao() {
  const [aba, setAba] = useState('campanhas');
  return <>
    <section className="page-heading"><div><h1>Reativação de clientes</h1><p>Traga de volta quem parou de comprar, pelo WhatsApp, com mensagens aprovadas por você.</p></div></section>
    <PainelStatus />
    <Abas abas={ABAS} ativa={aba} onChange={setAba} />
    {aba === 'campanhas' ? <Campanhas /> : <QuemPodeReceber />}
  </>;
}
