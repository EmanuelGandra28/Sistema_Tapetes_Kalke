import { describe, expect, it } from 'vitest';
import { NOVIDADES } from '../content/novidades';
import { chaveNovidade, jaVista, novidadesPendentes, versoesParaUsuario } from './novidades';

const versoes = [
  { id: '2', data: '2026-12-01', titulo: 'B', itens: [{ titulo: 'x', texto: 'y' }] },
  { id: '1', data: '2026-11-21', titulo: 'A', itens: [{ titulo: 'x', texto: 'y' }] }
];

describe('novidadesPendentes', () => {
  it('mostra só o que ainda não foi visto', () => {
    expect(novidadesPendentes(versoes, [chaveNovidade('1')], '2026-01-01T10:00:00Z').map((v) => v.id)).toEqual(['2']);
    expect(novidadesPendentes(versoes, [chaveNovidade('1'), chaveNovidade('2')], '2026-01-01T10:00:00Z')).toEqual([]);
  });
  it('não mostra nada enquanto a lista de vistos não chegou', () => {
    expect(novidadesPendentes(versoes, undefined, '2026-01-01T10:00:00Z')).toEqual([]);
  });
  it('conta criada depois da atualização não vê aquela novidade', () => {
    expect(novidadesPendentes(versoes, [], '2026-11-25T10:00:00Z').map((v) => v.id)).toEqual(['2']);
    expect(novidadesPendentes(versoes, [], '2026-12-05T10:00:00Z')).toEqual([]);
  });
  it('sem data de criação mostra tudo que não foi visto', () => {
    expect(novidadesPendentes(versoes, [], null)).toHaveLength(2);
  });
  it('conta criada no mesmo dia da atualização ainda vê', () => {
    expect(novidadesPendentes(versoes, [], '2026-11-21T08:00:00Z').map((v) => v.id)).toEqual(['2', '1']);
  });
});

describe('versoesParaUsuario', () => {
  const lista = [{ id: '1', data: '2026-11-21', titulo: 'A', itens: [
    { titulo: 'geral', texto: '' }, { titulo: 'tela', texto: '', pagina: 'relatorios' }, { titulo: 'acao', texto: '', acao: 'mensagens_gerenciar' }] }];
  it('filtra por permissão de página e de ação', () => {
    expect(versoesParaUsuario(lista, () => false, () => false)[0].itens.map((i) => i.titulo)).toEqual(['geral']);
    expect(versoesParaUsuario(lista, (p) => p === 'relatorios', () => false)[0].itens.map((i) => i.titulo)).toEqual(['geral', 'tela']);
    expect(versoesParaUsuario(lista, () => true, () => true)[0].itens).toHaveLength(3);
  });
  it('versão sem nenhum item para a pessoa some', () => {
    const so = [{ id: '1', data: '2026-11-21', titulo: 'A', itens: [{ titulo: 'tela', texto: '', pagina: 'relatorios' }] }];
    expect(versoesParaUsuario(so, () => false, () => false)).toEqual([]);
  });
});

describe('jaVista e conteúdo', () => {
  it('reconhece a marca na conta', () => {
    expect(jaVista(versoes[0], [chaveNovidade('2')])).toBe(true);
    expect(jaVista(versoes[0], [])).toBe(false);
    expect(jaVista(versoes[0], undefined)).toBe(false);
  });
  it('todo id de versão cabe no formato aceito pelo servidor e a lista está da mais nova para a mais antiga', () => {
    for (const v of NOVIDADES) expect(chaveNovidade(v.id)).toMatch(/^[a-z0-9]+:[a-z0-9._-]{1,50}$/);
    const datas = NOVIDADES.map((v) => v.data);
    expect([...datas].sort().reverse()).toEqual(datas);
    expect(new Set(NOVIDADES.map((v) => v.id)).size).toBe(NOVIDADES.length);
  });
});
