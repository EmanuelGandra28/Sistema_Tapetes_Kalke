import { describe, expect, it } from 'vitest';
import { AJUDA } from '../content/ajuda';
import { ajudaDaRota, chaveVisto, passosDoUsuario, primeiraVisita } from './ajuda';

describe('ajudaDaRota', () => {
  it('acha a ajuda pelo endereço, com ou sem barra final', () => {
    expect(ajudaDaRota('/vendas').id).toBe('vendas');
    expect(ajudaDaRota('/ordens-producao/').id).toBe('ordens-producao');
  });
  it('não há ajuda no login nem em endereços desconhecidos', () => {
    expect(ajudaDaRota('/login')).toBeNull();
    expect(ajudaDaRota('/qualquer-coisa')).toBeNull();
  });
  it('todas as telas do menu têm ajuda', () => {
    for (const rota of ['/dashboard', '/clientes', '/funcionarios', '/produtos', '/vendas', '/compras', '/fornecedores', '/ordens-producao', '/entregas', '/despesas', '/relatorios', '/configuracoes'])
      expect(ajudaDaRota(rota), rota).not.toBeNull();
  });
});

describe('passosDoUsuario', () => {
  const vendas = AJUDA.vendas;
  it('esconde os passos de ações que o cargo não tem', () => {
    const soLeitura = passosDoUsuario(vendas, () => false);
    expect(soLeitura.every((p) => !p.acao)).toBe(true);
    const completo = passosDoUsuario(vendas, () => true);
    expect(completo.length).toBeGreaterThan(soLeitura.length);
  });
  it('mostra só o passo da ação liberada', () => {
    const passos = passosDoUsuario(vendas, (a) => a === 'vendas_cancelar');
    expect(passos.some((p) => p.acao === 'vendas_cancelar')).toBe(true);
    expect(passos.some((p) => p.acao === 'vendas_criar')).toBe(false);
  });
});

describe('conteúdo', () => {
  it('toda tela tem resumo e ao menos um passo sem exigir permissão', () => {
    for (const [id, a] of Object.entries(AJUDA)) {
      expect(a.resumo.length, id).toBeGreaterThan(20);
      expect(a.passos.some((p) => !p.acao), id).toBe(true);
    }
  });
});

describe('primeiraVisita', () => {
  it('só é primeira visita quando a lista chegou e não tem a tela', () => {
    expect(primeiraVisita('vendas', undefined)).toBe(false);
    expect(primeiraVisita('vendas', [])).toBe(true);
    expect(primeiraVisita('vendas', [chaveVisto('vendas')])).toBe(false);
  });
});
