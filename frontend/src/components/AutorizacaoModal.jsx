import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api } from '../services/api';
import Modal from './Modal';

// Registra que o cliente autorizou receber mensagens da loja pelo WhatsApp (LGPD). Quem registra descreve COMO foi a autorização:
// o sistema nunca preenche isso sozinho, e sem autorização o cliente nunca recebe campanhas.
export default function AutorizacaoModal({ cliente, onClose, onDone }) {
  const [evidencia, setEvidencia] = useState('');
  const [confirmo, setConfirmo] = useState(false);
  const salvar = useMutation({ mutationFn: () => api.post(`/clientes/${cliente.id}/consentimento`, { evidencia: evidencia.trim() }), onSuccess: () => { onDone(); onClose(); } });
  const curta = evidencia.trim().length < 10;
  return <Modal title={`Autorização de WhatsApp — ${cliente.nome}`} onClose={onClose}>
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); salvar.mutate(); }}>
      <p className="hint full">Só registre se o cliente <strong>realmente autorizou</strong> receber mensagens da loja pelo WhatsApp (pessoalmente, por mensagem ou por escrito). Ele pode pedir para sair a qualquer momento respondendo SAIR.</p>
      <label className="full">Como o cliente autorizou?
        <textarea required rows={3} maxLength={2000} value={evidencia} onChange={(e) => setEvidencia(e.target.value)} placeholder='Ex.: "Autorizou no balcão em 21/11/2026, falando com a vendedora Ana"' /></label>
      <small className={`full ${curta && evidencia ? 'error' : 'hint'}`}>{evidencia.trim().length} caracteres (mínimo 10)</small>
      <label className="check full"><input type="checkbox" checked={confirmo} onChange={(e) => setConfirmo(e.target.checked)} /> Confirmo que o cliente autorizou receber mensagens por WhatsApp</label>
      {salvar.isError && <p className="error full">{salvar.error.message}</p>}
      <footer><button type="button" className="secondary" onClick={onClose}>Cancelar</button><button disabled={salvar.isPending || curta || !confirmo}>{salvar.isPending ? 'Salvando…' : 'Registrar autorização'}</button></footer>
    </form>
  </Modal>;
}
