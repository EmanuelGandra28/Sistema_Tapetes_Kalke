import { describe, expect, it } from 'vitest';
import { agruparPermissoes, rotuloPermissao } from './permissoes';

describe('rotuloPermissao', () => {
  it('traduz uma ação conhecida', () => {
    expect(rotuloPermissao('clientes_criar')).toEqual({ rotulo: 'Cadastrar clientes', grupo: 'Clientes' });
  });
  it('traduz uma página conhecida', () => {
    expect(rotuloPermissao('produtos')).toEqual({ rotulo: 'Tela de Estoque', grupo: 'Estoque' });
  });
  it('mostra a chave crua quando não conhece', () => {
    expect(rotuloPermissao('xyz')).toEqual({ rotulo: 'xyz', grupo: 'Outros' });
  });
});

describe('agruparPermissoes', () => {
  it('agrupa na ordem fixa dos grupos e mantém a ordem dos itens', () => {
    expect(agruparPermissoes(['configuracoes', 'xyz', 'vendas_criar', 'clientes_editar', 'clientes_criar'])).toEqual([
      { grupo: 'Clientes', itens: ['clientes_editar', 'clientes_criar'] },
      { grupo: 'Vendas', itens: ['vendas_criar'] },
      { grupo: 'Sistema', itens: ['configuracoes'] },
      { grupo: 'Outros', itens: ['xyz'] }
    ]);
  });
});
