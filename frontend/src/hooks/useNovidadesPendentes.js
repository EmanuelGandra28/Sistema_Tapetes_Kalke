import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { NOVIDADES } from '../content/novidades';
import { novidadesPendentes, versoesParaUsuario } from '../utils/novidades';

// O que o usuário ainda não viu das novidades (e a lista de "vistos" da conta). Usado pela janela de Novidades e pela Ajuda:
// a Ajuda espera as Novidades terminarem para não abrirem duas janelas uma sobre a outra no primeiro login.
export function useNovidadesPendentes() {
  const { user, canPage, canAction } = useAuth();
  const vistos = useQuery({ queryKey: ['vistos'], queryFn: () => api.get('/vistos'), enabled: Boolean(user), staleTime: Infinity });
  const conta = useQuery({ queryKey: ['vistos', 'conta'], queryFn: () => api.get('/vistos/conta'), enabled: Boolean(user), staleTime: Infinity });
  const lista = vistos.data?.data;
  const permitidas = versoesParaUsuario(NOVIDADES, canPage, canAction);
  const pendentes = conta.isSuccess ? novidadesPendentes(permitidas, lista, conta.data.data.criadaEm) : [];
  // Em erro de rede não fica "carregando" para sempre: a Ajuda segue o seu caminho normal.
  return { vistos: lista, permitidas, pendentes, carregando: Boolean(user) && (vistos.isPending || conta.isPending) };
}
