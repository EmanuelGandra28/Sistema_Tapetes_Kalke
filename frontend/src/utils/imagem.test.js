import { describe, expect, it } from 'vitest';
import { escalaReducao } from './imagem';

describe('escalaReducao', () => {
  it('reduz fotos grandes mantendo a proporção', () => {
    expect(escalaReducao(4000, 3000)).toBeCloseTo(0.32);
    expect(escalaReducao(3000, 4000)).toBeCloseTo(0.32);
  });
  it('não amplia foto pequena', () => {
    expect(escalaReducao(800, 600)).toBe(1);
    expect(escalaReducao(1280, 1000)).toBe(1);
  });
});
