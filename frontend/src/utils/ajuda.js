import { AJUDA } from '../content/ajuda';

// Ajuda da tela que corresponde ao endereço atual (ou null, como no login e na tela 404).
export function ajudaDaRota(pathname) {
  const limpo = pathname.replace(/\/+$/, '') || '/';
  const id = Object.keys(AJUDA).find((k) => AJUDA[k].rota === limpo);
  return id ? { id, ...AJUDA[id] } : null;
}

// Só os passos que o usuário pode usar: um passo com `acao` exige essa permissão.
export const passosDoUsuario = (ajuda, canAction) => ajuda.passos.filter((p) => !p.acao || canAction(p.acao));

export const chaveVisto = (id) => `ajuda:${id}`;

// Primeira visita a uma tela: ainda não consta entre os "vistos" da conta (e a lista já foi carregada).
export const primeiraVisita = (id, vistos) => Array.isArray(vistos) && !vistos.includes(chaveVisto(id));
