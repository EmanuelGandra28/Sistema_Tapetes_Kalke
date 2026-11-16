import { useQuery } from '@tanstack/react-query';
import { api } from './api';

// Listas de códigos (forma de pagamento, tipo, unidade, tipo de despesa) vindas do banco; mudam raramente.
export const useCodigos = () => {
  const q = useQuery({ queryKey: ['codigos'], queryFn: () => api.get('/codigos'), staleTime: 10 * 60 * 1000 });
  return q.data?.data ?? { formasPagamento: [], tiposProduto: [], unidadesMedida: [], tiposDespesa: [] };
};

export const CARGOS = ['DONO', 'GERENTE', 'VENDEDOR', 'ESTOQUISTA', 'ENTREGADOR'];
// Cargos de login: inclui o ADMINISTRADOR (quem mantém o sistema), que não é cargo de funcionário da loja.
export const CARGOS_USUARIO = ['ADMINISTRADOR', ...CARGOS];

export const ask = (message) => { const value = window.prompt(message); return value && value.trim() ? value.trim() : null; };
