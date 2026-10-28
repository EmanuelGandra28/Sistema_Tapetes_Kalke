import { createContext, useContext, useEffect, useState } from 'react';
import { api, setAccessToken, setSessionLostHandler } from '../services/api';
const AuthContext = createContext(null);
// O token fica no sessionStorage: sobrevive ao F5, mas some ao fechar a aba. O servidor ainda valida sessão e usuário ativo.
const STORAGE_KEY = 'tk_session';
const readToken = () => { try { return sessionStorage.getItem(STORAGE_KEY); } catch { return null; } };
const writeToken = (token) => { try { if (token) sessionStorage.setItem(STORAGE_KEY, token); else sessionStorage.removeItem(STORAGE_KEY); } catch { /* armazenamento indisponível: sessão só em memória */ } };
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); const [loading, setLoading] = useState(false); const [restoring, setRestoring] = useState(true);
  const endSession = () => { setAccessToken(null); writeToken(null); setUser(null); };
  useEffect(() => {
    setSessionLostHandler(() => { writeToken(null); setUser(null); });
    const token = readToken();
    if (!token) { setRestoring(false); return; }
    setAccessToken(token);
    api.get('/auth/me').then((r) => setUser(r.data)).catch(() => endSession()).finally(() => setRestoring(false));
  }, []);
  const login = async (email, senha) => { setLoading(true); try { const response = await api.post('/auth/login', { email, senha }); setAccessToken(response.data.accessToken); writeToken(response.data.accessToken); setUser(response.data.user); return response.data.user; } finally { setLoading(false); } };
  const logout = async () => { try { await api.post('/auth/logout'); } catch { /* logout local ainda deve ocorrer */ } endSession(); };
  return <AuthContext.Provider value={{ user, loading, restoring, login, logout, endSession, canPage: (page) => Boolean(user?.pages?.[page]), canAction: (action) => Boolean(user?.actions?.[action]) }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
