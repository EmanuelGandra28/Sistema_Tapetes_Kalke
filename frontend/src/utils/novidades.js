export const chaveNovidade = (id) => `novidades:${id}`;

// Versões com os itens que a pessoa pode usar (um item com `pagina` ou `acao` exige a permissão). Versão sem nenhum item some.
export function versoesParaUsuario(versoes, canPage, canAction) {
  return versoes
    .map((v) => ({ ...v, itens: v.itens.filter((i) => (!i.pagina || canPage(i.pagina)) && (!i.acao || canAction(i.acao))) }))
    .filter((v) => v.itens.length > 0);
}

// Novidades que devem aparecer sozinhas: ainda não vistas e de uma atualização publicada DEPOIS que a conta existia
// (quem acabou de ser cadastrado já entra na versão nova e tem o passeio da Ajuda; não faz sentido mostrar "o que mudou").
// `vistos` indefinido = ainda carregando: não mostra nada. `criadaEm` ausente: mostra, por segurança.
export function novidadesPendentes(versoes, vistos, criadaEm) {
  if (!Array.isArray(vistos)) return [];
  const criada = criadaEm ? String(criadaEm).slice(0, 10) : null;
  return versoes.filter((v) => !vistos.includes(chaveNovidade(v.id)) && (!criada || criada <= v.data));
}

export const jaVista = (versao, vistos) => Array.isArray(vistos) && vistos.includes(chaveNovidade(versao.id));
