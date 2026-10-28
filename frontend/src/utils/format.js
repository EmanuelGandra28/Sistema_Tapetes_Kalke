export const money = (value) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value ?? 0);
export const date = (value) => value ? new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo' }).format(new Date(`${value}T12:00:00`)) : '—';
export const digits = (value) => (value ?? '').replace(/\D/g, '');
// Data de negócio (yyyy-mm-dd) no fuso da loja; toISOString() usaria UTC e viraria o dia após as 21h.
export const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date());
