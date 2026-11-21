import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CircleHelp, X } from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { useNovidadesPendentes } from '../hooks/useNovidadesPendentes';
import { ajudaDaRota, chaveVisto, passosDoUsuario, primeiraVisita } from '../utils/ajuda';

// Ajuda da tela atual: botão "?" sempre à mão. Na primeira visita de cada pessoa a uma tela, abre sozinha como um passeio
// passo a passo (com "Pular"); depois fica só no botão. O que já foi visto fica guardado na conta do usuário.
export default function Ajuda() {
  const { pathname } = useLocation();
  const { canPage, canAction, user } = useAuth();
  const queryClient = useQueryClient();
  const ajuda = ajudaDaRota(pathname);
  const [aberta, setAberta] = useState(false);
  const [modo, setModo] = useState('guia'); // 'passeio' (um passo por vez) ou 'guia' (tudo de uma vez)
  const [passo, setPasso] = useState(0);
  const vistos = useQuery({ queryKey: ['vistos'], queryFn: () => api.get('/vistos'), enabled: Boolean(user), staleTime: Infinity });
  const marcar = useMutation({
    mutationFn: (id) => api.post('/vistos', { chave: chaveVisto(id) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['vistos'] })
  });
  const novidades = useNovidadesPendentes();
  const aguardaNovidades = novidades.carregando || novidades.pendentes.length > 0; // a janela de Novidades vem primeiro
  const permitida = ajuda && canPage(ajuda.pagina);
  const idAjuda = permitida ? ajuda.id : null;
  const jaVisto = vistos.data?.data;

  // Mudou de tela: fecha o painel; se for a primeira visita, abre o passeio.
  useEffect(() => {
    setPasso(0);
    if (idAjuda && !aguardaNovidades && primeiraVisita(idAjuda, jaVisto)) { setModo('passeio'); setAberta(true); } else { setAberta(false); setModo('guia'); }
    // só reage à troca de tela e à chegada da lista de vistos
  }, [idAjuda, Boolean(jaVisto), aguardaNovidades]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!permitida) return null;
  const passos = passosDoUsuario(ajuda, canAction);
  const fechar = () => { setAberta(false); if (primeiraVisita(ajuda.id, jaVisto)) marcar.mutate(ajuda.id); };
  const ultimo = passo >= passos.length - 1;
  const emPasseio = modo === 'passeio' && passos.length > 0; // sem passos liberados para o cargo, mostra a ajuda completa
  return <>
    <button type="button" className="ajuda-botao no-print" onClick={() => { setModo('guia'); setAberta(true); }} aria-label={`Ajuda da tela ${ajuda.titulo}`} title="Ajuda desta tela"><CircleHelp size={22} /><span>Ajuda</span></button>
    {aberta && <div className="ajuda-fundo no-print" onClick={fechar}>
      <aside className="ajuda-painel" role="dialog" aria-modal="true" aria-label={`Ajuda: ${ajuda.titulo}`} onClick={(e) => e.stopPropagation()} onKeyDown={(e) => e.key === 'Escape' && fechar()}>
        <header><h2>{emPasseio ? `Conhecendo: ${ajuda.titulo}` : `Ajuda — ${ajuda.titulo}`}</h2>
          <button type="button" className="ajuda-fechar" onClick={fechar} aria-label="Fechar ajuda" autoFocus><X size={20} /></button></header>
        <p className="ajuda-resumo">{ajuda.resumo}</p>
        {emPasseio ? <section aria-live="polite">
          <p className="hint">Passo {passo + 1} de {passos.length}</p>
          <p className="ajuda-passo">{passos[passo].texto}</p>
          <div className="ajuda-acoes">
            <button type="button" className="secondary" onClick={fechar}>Pular</button>
            <button type="button" className="secondary" disabled={passo === 0} onClick={() => setPasso(passo - 1)}>Voltar</button>
            <button type="button" onClick={() => (ultimo ? fechar() : setPasso(passo + 1))}>{ultimo ? 'Concluir' : 'Próximo'}</button>
          </div>
          <p><button type="button" className="link" onClick={() => setModo('guia')}>Ver a ajuda completa</button></p>
        </section> : <>
          <h3>Passo a passo</h3>
          <ol>{passos.map((p) => <li key={p.texto}>{p.texto}</li>)}</ol>
          {ajuda.exemplos.length > 0 && <><h3>Exemplo</h3>{ajuda.exemplos.map((e) => <p key={e} className="ajuda-exemplo">{e}</p>)}</>}
          {ajuda.duvidas.length > 0 && <><h3>Dúvidas comuns</h3><dl>{ajuda.duvidas.map((d) => <div key={d.p}><dt>{d.p}</dt><dd>{d.r}</dd></div>)}</dl></>}
        </>}
      </aside>
    </div>}
  </>;
}
