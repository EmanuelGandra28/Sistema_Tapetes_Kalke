import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { Loading, Failure } from './State';

// Configurações → alerta de produção: a dona recebe no WhatsApp a lista dos tapetes (com fotos) quando o sistema abre uma ordem automática.
export default function AlertaProducaoConfig() {
  const queryClient = useQueryClient();
  const q = useQuery({ queryKey: ['alerta-producao'], queryFn: () => api.get('/alertas-producao') });
  const [form, setForm] = useState(null);
  const refresh = () => { setForm(null); queryClient.invalidateQueries({ queryKey: ['alerta-producao'] }); };
  const salvar = useMutation({ mutationFn: (dados) => api.put('/alertas-producao', dados), onSuccess: refresh });
  const reenviar = useMutation({ mutationFn: () => api.post('/alertas-producao/reenviar'), onSuccess: refresh });
  if (q.isPending) return <Loading />;
  if (q.isError) return <Failure error={q.error} />;
  const c = q.data.data;
  const atual = form ?? { whatsapp: c.whatsapp ?? '', ativo: c.ativo };
  const set = (k, v) => setForm({ ...atual, [k]: v });
  const resultado = reenviar.data?.data;
  return <section className="panel">
    <h2>Alerta de produção por WhatsApp</h2>
    <p className="hint">Quando o estoque de um tapete chega ao mínimo e o sistema abre uma ordem de produção automática, a dona recebe uma mensagem com a lista dos tapetes (saldo, mínimo e quanto produzir) e até 3 fotos. Os avisos são agrupados numa só mensagem e saem entre 8h e 20h, em até 30 minutos. O texto é editado em Mensagens.</p>
    {!c.gatewayConfigurado && <p className="aviso">O envio de WhatsApp ainda não está ligado no servidor (falta a chave do gateway). Você já pode salvar o número; os avisos esperam na fila.</p>}
    <form className="form-grid" onSubmit={(e) => { e.preventDefault(); salvar.mutate(atual); }}>
      <label>WhatsApp da dona (com DDD)<input inputMode="tel" maxLength="20" value={atual.whatsapp} onChange={(e) => set('whatsapp', e.target.value)} placeholder="(11) 98765-4321" /></label>
      <label className="check full"><input type="checkbox" checked={atual.ativo} onChange={(e) => set('ativo', e.target.checked)} /> Avisar a dona quando uma ordem de produção automática for criada</label>
      <p className="hint full">Ao ligar, fica registrado que você cadastrou este número e ativou os avisos, com seu nome e a data (é a autorização exigida pelo WhatsApp). Só ligue para um número que seja da dona e que ela queira usar.</p>
      <p className="hint full">Na fila agora: <strong>{c.pendentes}</strong> {c.pendentes === 1 ? 'aviso' : 'avisos'} · Com falha: <strong>{c.falhas}</strong></p>
      {(salvar.isError || reenviar.isError) && <p className="error full">{(salvar.error ?? reenviar.error).message}</p>}
      {resultado?.motivo && <p className="hint full" role="status">{resultado.motivo}</p>}
      {resultado && !resultado.motivo && <p className="hint full" role="status">Enviado: {resultado.alertas} {resultado.alertas === 1 ? 'aviso' : 'avisos'} em {resultado.mensagens} {resultado.mensagens === 1 ? 'mensagem' : 'mensagens'}.</p>}
      <footer>
        {c.falhas > 0 && <button type="button" className="secondary" disabled={reenviar.isPending} onClick={() => reenviar.mutate()}>{reenviar.isPending ? 'Reenviando…' : 'Reenviar avisos que falharam'}</button>}
        <button disabled={salvar.isPending || !form}>{salvar.isPending ? 'Salvando…' : 'Salvar'}</button>
      </footer>
    </form>
  </section>;
}
