import { describe, expect, it } from 'vitest';
import { nivelCota, resumoAprovacao, taxa } from './reativacao';

describe('nivelCota', () => {
  it('colore o contador da cota mensal', () => {
    expect(nivelCota(100, 500, 450)).toBe('ok');
    expect(nivelCota(350, 500, 450)).toBe('atencao');
    expect(nivelCota(450, 500, 450)).toBe('bloqueada');
  });
});

describe('taxa', () => {
  it('calcula a porcentagem e não divide por zero', () => {
    expect(taxa(1, 4)).toBe(25);
    expect(taxa(0, 0)).toBe(0);
    expect(taxa(2, 3)).toBe(67);
  });
});

describe('resumoAprovacao', () => {
  it('descreve o que será enviado, no singular e no plural', () => {
    expect(resumoAprovacao({ destinatarios: 12, intervaloDias: 5, limiteDiario: 20 })).toContain('12 clientes');
    expect(resumoAprovacao({ destinatarios: 1, intervaloDias: 7, limiteDiario: 10 })).toContain('1 cliente,');
    expect(resumoAprovacao({ destinatarios: 1, intervaloDias: 7, limiteDiario: 10 })).toContain('7 dias');
  });
});
