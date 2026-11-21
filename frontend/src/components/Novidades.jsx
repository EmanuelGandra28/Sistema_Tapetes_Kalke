import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Sparkles } from 'lucide-react';
import { api } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import Modal from './Modal';
import { useNovidadesPendentes } from '../hooks/useNovidadesPendentes';
import { chaveNovidade, jaVista } from '../utils/novidades';

const dataBr = (d) => `${d.slice(8, 10)}/${d.slice(5, 7)}/${d.slice(0, 4)}`;

// "O que há de novo": abre sozinho no primeiro login depois de cada atualização (uma vez por pessoa, guardado na conta)
// e fica disponível pelo botão "Novidades" para rever o histórico. Mostra só o que a pessoa tem permissão de usar.
export default function Novidades() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [aberta, setAberta] = useState(false);
  const [historico, setHistorico] = useState(false); // false = só as novidades novas; true = tudo
  const { vistos: lista, permitidas, pendentes } = useNovidadesPendentes();
  const marcar = useMutation({
    mutationFn: (ids) => Promise.all(ids.map((id) => api.post('/vistos', { chave: chaveNovidade(id) }))),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['vistos'] })
  });
  const chavePendentes = pendentes.map((v) => v.id).join(',');

  // Primeiro login depois da atualização: abre sozinho.
  useEffect(() => { if (chavePendentes) { setHistorico(false); setAberta(true); } }, [chavePendentes]);

  if (!user) return null;
  const mostrar = historico || pendentes.length === 0 ? permitidas : pendentes;
  const fechar = () => {
    setAberta(false);
    // Tudo o que estava na lista (e ainda não visto) passa a constar como visto.
    const novos = mostrar.filter((v) => !jaVista(v, lista)).map((v) => v.id);
    if (novos.length) marcar.mutate(novos);
  };
  return <>
    <button type="button" className="novidades-botao no-print" onClick={() => { setHistorico(true); setAberta(true); }} aria-label="Novidades do sistema" title="O que há de novo">
      <Sparkles size={16} /><span>Novidades</span>{pendentes.length > 0 && <b className="novidades-ponto" aria-label="Há novidades novas" />}</button>
    {aberta && <Modal title={historico || pendentes.length === 0 ? 'Novidades do sistema' : 'O sistema foi atualizado!'} onClose={fechar}>
      {mostrar.length === 0 ? <p>Não há novidades para o seu cargo.</p> : mostrar.map((v) => <section key={v.id} className="novidade-versao">
        <h3>{v.titulo} {!jaVista(v, lista) && <span className="badge faixa-boa">Novo</span>}</h3>
        <p className="hint">Atualização de {dataBr(v.data)}</p>
        <ul className="novidade-itens">{v.itens.map((i) => <li key={i.titulo}><strong>{i.titulo}</strong><p>{i.texto}</p>
          {i.rota && <Link to={i.rota} onClick={fechar}>Ir para esta tela</Link>}</li>)}</ul>
      </section>)}
      <footer><button type="button" onClick={fechar} autoFocus>Entendi</button></footer>
    </Modal>}
  </>;
}
