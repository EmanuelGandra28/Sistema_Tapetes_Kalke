import axios from 'axios';
let accessToken = null;
let onSessionLost = () => {};
export const setAccessToken = (token) => { accessToken = token; };
export const setSessionLostHandler = (handler) => { onSessionLost = handler; };
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1', headers: { 'Content-Type': 'application/json' } });
api.interceptors.request.use((config) => { if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`; return config; });
// Token expirado ou revogado: o Spring Security responde 401/403 sem corpo da API; nesse caso a sessão local é encerrada.
api.interceptors.response.use((response) => response.data, (error) => {
  const status = error.response?.status;
  if (accessToken && (status === 401 || status === 403) && !error.response?.data?.errorCode) { accessToken = null; onSessionLost(); return Promise.reject({ message: 'Sua sessão expirou. Entre novamente.' }); }
  return Promise.reject(error.response?.data ?? { message: 'Não foi possível comunicar com o servidor.' });
});
export const createKey = () => crypto.randomUUID();
