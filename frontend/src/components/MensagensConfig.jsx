import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure } from './State';
import Modal from './Modal';
import { date } from '../utils/format';
import { inserirCampo, previa, problemas, TAMANHO_MAXIMO, TIPOS_MENSAGEM } from '../utils/mensagens';

function HistoricoModelo({ modelo, onRestaurado }) {
  const q = useQuery({ queryKey: ['mensagens', modelo.chave, 'historico'], queryFn: () => api.get(`/mensagens/${modelo.chave}/historico`) });
  const restaurar = useMutation({ mutationFn: (id) => api.post(`/mensagens/${modelo.chave}/historico/${id}/restaurar`), onSuccess: (r) => onRestaurado(r.data) });
  if (q.isPending) return <Loading />;
  if (q.isError) return <Failure error={q.error} />;
  const lista = q.data.data;
  return <div className="historico-mensagem">
    {lista.length === 0 && <p className="hint">Este texto ainda não foi alterado.</p>}
    {lista.map((h) => <article key={h.id}>
      <small>{date(h.alteradoEm?.slice(0, 10))} · alterado por {h.alteradoPor ?? 'usuário removido'} · texto que valia antes:</small>
      <p>{h.textoAnterior}</p>
      <button type="button" className="link" disabled={restaurar.isPending} onClick={() => window.confirm('Voltar para este texto? O texto atual fica guardado no histórico.') && restaurar.mutate(h.id)}>Voltar para este texto</button>
    </article>)}
    {restaurar.isError && <p className="error">{restaurar.error.message}</p>}
  </div>;
}

function Editor({ modelo, onClose }) {
  const queryClient = useQueryClient();
  const area = useRef(null);
  const [texto, setTexto] = useState(modelo.texto);
  const [historico, setHistorico] = useState(false);
  const [avisos, setAvisos] = useState([]);
  const erros = problemas(texto, modelo);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['mensagens'] });
  const salvar = useMutation({
    mutationFn: () => api.put(`/mensagens/${modelo.chave}`, { texto }),
    onSuccess: (r) => { refresh(); if (r.data.avisos.length) setAvisos(r.data.avisos); else onClose(); }
  });
  const restaurar = useMutation({ mutationFn: () => api.post(`/mensagens/${modelo.chave}/restaurar-padrao`), onSuccess: (r) => { refresh(); setTexto(r.data.texto); } });
  const inserir = (campo) => {
    const el = area.current;
    const { texto: novo, cursor } = inserirCampo(texto, el?.selectionStart ?? texto.length, el?.selectionEnd ?? texto.length, campo);
    setTexto(novo);
    requestAnimationFrame(() => { el?.focus(); el?.setSelectionRange(cursor, cursor); });
  };
  const tipo = TIPOS_MENSAGEM[modelo.tipo];
  return <Modal title={modelo.nome} onClose={onClose}>
    <p className="hint">{modelo.descricao} <span className={`badge tipo-${modelo.tipo.toLowerCase()}`} title={tipo.dica}>{tipo.rotulo}</span></p>
    <div className="campos-mensagem" role="group" aria-label="Inserir campo no texto">
      <span>Clique para inserir no texto:</span>
      {modelo.variaveis.map((v) => <button type="button" key={v.nome} className="chip" title={`${v.descricao} (exemplo: ${v.exemplo})`} onClick={() => inserir(v.nome)}>{`{${v.nome}}`}</button>)}
    </div>
    <label className="full">Texto da mensagem
      <textarea ref={area} rows={6} value={texto} onChange={(e) => setTexto(e.target.value)} aria-invalid={erros.length > 0} /></label>
    <small className={texto.length > TAMANHO_MAXIMO ? 'error' : 'hint'}>{texto.length} de {TAMANHO_MAXIMO} caracteres</small>
    {erros.map((e) => <p key={e} className="error">{e}</p>)}
    <div className="previa-whatsapp"><small>Assim o cliente vai ver (com dados de exemplo):</small><p>{previa(texto, modelo.variaveis, modelo.rodapeSaida)}</p></div>
    {modelo.rodapeSaida && <p className="hint">A frase "{modelo.rodapeSaida}" é acrescentada pelo sistema em todas as campanhas e não pode ser apagada.</p>}
    {avisos.map((a) => <p key={a} className="aviso">{a}</p>)}
    {(salvar.isError || restaurar.isError) && <p className="error">{(salvar.error ?? restaurar.error).message}</p>}
    <p><button type="button" className="link" onClick={() => setHistorico(!historico)}>{historico ? 'Esconder histórico' : 'Ver histórico de alterações'}</button></p>
    {historico && <HistoricoModelo modelo={modelo} onRestaurado={(m) => { refresh(); setTexto(m.texto); }} />}
    <footer>
      <button type="button" className="secondary" onClick={() => window.confirm('Voltar ao texto padrão do sistema?') && restaurar.mutate()} disabled={restaurar.isPending || !modelo.personalizado}>Restaurar texto padrão</button>
      <button type="button" className="secondary" onClick={onClose}>{avisos.length ? 'Fechar' : 'Cancelar'}</button>
      {!avisos.length && <button type="button" disabled={salvar.isPending || erros.length > 0 || texto === modelo.texto} onClick={() => salvar.mutate()}>{salvar.isPending ? 'Salvando…' : 'Salvar mensagem'}</button>}
    </footer>
  </Modal>;
}

// Aba "Mensagens" das Configurações: a dona e a gerente ajustam os textos enviados automaticamente, sem mexer no código.
export default function MensagensConfig() {
  const queryClient = useQueryClient();
  const [editando, setEditando] = useState(null);
  const q = useQuery({ queryKey: ['mensagens'], queryFn: () => api.get('/mensagens') });
  const ativar = useMutation({ mutationFn: ({ chave, ativo }) => api.put(`/mensagens/${chave}/ativo`, { ativo }), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['mensagens'] }) });
  if (q.isPending) return <Loading />;
  if (q.isError) return <Failure error={q.error} />;
  const lista = q.data.data;
  const atual = editando && lista.find((m) => m.chave === editando);
  return <section className="panel">
    <h2>Mensagens automáticas</h2>
    <p className="hint">Escreva aqui os textos que o sistema envia aos clientes. Use os campos entre chaves (como {'{nome}'}) para o sistema preencher sozinho. Alterações valem na hora, e o histórico guarda os textos anteriores.</p>
    {ativar.isError && <p className="error">{ativar.error.message}</p>}
    <ul className="lista-mensagens">{lista.map((m) => <li key={m.chave} className={m.ativo ? '' : 'desativada'}>
      <div><strong>{m.nome}</strong> <span className={`badge tipo-${m.tipo.toLowerCase()}`} title={TIPOS_MENSAGEM[m.tipo].dica}>{TIPOS_MENSAGEM[m.tipo].rotulo}</span>
        {m.personalizado && <span className="badge auto" title="O texto foi alterado em relação ao padrão">Personalizada</span>}
        <p className="trecho">{m.texto}</p>
        {m.atualizadoPor && <small className="hint">Última alteração por {m.atualizadoPor}</small>}</div>
      <div className="row-actions">
        <label className="interruptor" title={m.ativo ? 'Ativa: o sistema usa esta mensagem' : 'Desativada: o sistema não envia esta mensagem'}>
          <input type="checkbox" checked={m.ativo} disabled={ativar.isPending} onChange={(e) => ativar.mutate({ chave: m.chave, ativo: e.target.checked })} /> {m.ativo ? 'Ativa' : 'Desativada'}</label>
        <button type="button" className="secondary" onClick={() => setEditando(m.chave)}>Editar texto</button>
      </div></li>)}</ul>
    {atual && <Editor key={atual.chave} modelo={atual} onClose={() => setEditando(null)} />}
  </section>;
}
