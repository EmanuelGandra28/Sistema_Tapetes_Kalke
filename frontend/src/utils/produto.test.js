import { describe, expect, it } from 'vitest';
import { produtoDefaults, produtoParaForm, produtoParaPayload } from './produto';

describe('produtoParaPayload', () => {
  it('converte números, inclusive a ordem do portfólio', () => {
    const p = produtoParaPayload({ ...produtoDefaults, estoqueMinimoComercial: '2', precoVenda: '119.9', portfolioOrdem: '3' });
    expect(p.estoqueMinimoComercial).toBe(2);
    expect(p.precoVenda).toBe(119.9);
    expect(p.portfolioOrdem).toBe(3);
  });
  it('envia null nos campos de portfólio em branco', () => {
    const p = produtoParaPayload({ ...produtoDefaults, portfolioLinha: '', portfolioSelo: '', portfolioDescricao: '   ' });
    expect(p.portfolioLinha).toBeNull();
    expect(p.portfolioSelo).toBeNull();
    expect(p.portfolioDescricao).toBeNull();
  });
  it('mantém os campos de portfólio preenchidos', () => {
    const p = produtoParaPayload({ ...produtoDefaults, portfolioModo: 'VENDA', portfolioLinha: 'COZINHA', portfolioSelo: 'Novo', portfolioDescricao: 'Tulipas' });
    expect(p).toMatchObject({ portfolioModo: 'VENDA', portfolioLinha: 'COZINHA', portfolioSelo: 'Novo', portfolioDescricao: 'Tulipas' });
  });
  it('ordem em branco vira 0', () => {
    expect(produtoParaPayload({ ...produtoDefaults, portfolioOrdem: '' }).portfolioOrdem).toBe(0);
  });
});

describe('produtoParaForm', () => {
  it('produto antigo, sem campos de portfólio, abre como "fora do site"', () => {
    const f = produtoParaForm({ id: 'x', nome: 'Tapete' });
    expect(f.portfolioModo).toBe('NENHUM');
    expect(f.portfolioLinha).toBe('');
    expect(f.portfolioOrdem).toBe(0);
  });
  it('traz os valores salvos', () => {
    const f = produtoParaForm({ id: 'x', nome: 'Tapete', portfolioModo: 'GALERIA', portfolioLinha: 'SALA', portfolioSelo: null });
    expect(f.portfolioModo).toBe('GALERIA');
    expect(f.portfolioLinha).toBe('SALA');
    expect(f.portfolioSelo).toBe('');
  });
});
