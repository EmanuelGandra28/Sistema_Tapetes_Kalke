import { describe, expect, it } from 'vitest';
import { atalhosPeriodo, faixaMargem, resumirAbc, variacao } from './relatorios';

describe('atalhosPeriodo', () => {
  const porId = Object.fromEntries(atalhosPeriodo('2026-11-21').map((a) => [a.id, a]));
  it('hoje, 7 dias, mês e ano', () => {
    expect(porId.hoje).toMatchObject({ inicio: '2026-11-21', fim: '2026-11-21' });
    expect(porId['7dias']).toMatchObject({ inicio: '2026-11-15', fim: '2026-11-21' });
    expect(porId.mes).toMatchObject({ inicio: '2026-11-01', fim: '2026-11-21' });
    expect(porId.ano).toMatchObject({ inicio: '2026-01-01', fim: '2026-11-21' });
  });
  it('mês passado vai do dia 1 ao último dia', () => {
    expect(porId.mespassado).toMatchObject({ inicio: '2026-10-01', fim: '2026-10-31' });
  });
  it('mês passado em janeiro volta para dezembro do ano anterior', () => {
    const jan = atalhosPeriodo('2027-01-10').find((a) => a.id === 'mespassado');
    expect(jan).toMatchObject({ inicio: '2026-12-01', fim: '2026-12-31' });
  });
  it('7 dias atravessa a virada de mês', () => {
    expect(atalhosPeriodo('2026-12-03').find((a) => a.id === '7dias').inicio).toBe('2026-11-27');
  });
});

describe('variacao', () => {
  it('sobe, cai e fica igual', () => {
    expect(variacao(150, 100)).toMatchObject({ sentido: 'subiu', pct: 50 });
    expect(variacao(50, 100)).toMatchObject({ sentido: 'caiu', pct: -50 });
    expect(variacao(100, 100)).toMatchObject({ sentido: 'igual' });
  });
  it('sem período anterior não divide por zero', () => {
    expect(variacao(10, 0)).toEqual({ sentido: 'novo', pct: null });
    expect(variacao(0, 0)).toEqual({ sentido: 'igual', pct: 0 });
  });
});

describe('resumirAbc', () => {
  const itens = [
    { classe: 'A', receita: '600' }, { classe: 'A', receita: '200' },
    { classe: 'B', receita: '120' }, { classe: 'C', receita: '80' }
  ];
  it('conta produtos e participação de cada grupo', () => {
    const [a, b, c] = resumirAbc(itens);
    expect(a).toMatchObject({ classe: 'A', nome: 'Campeões', produtos: 2, percentual: 80 });
    expect(b).toMatchObject({ produtos: 1, percentual: 12 });
    expect(c).toMatchObject({ produtos: 1, percentual: 8 });
  });
  it('sem vendas não quebra', () => {
    expect(resumirAbc([]).every((g) => g.produtos === 0 && g.percentual === 0)).toBe(true);
  });
});

describe('faixaMargem', () => {
  it('classifica pela porcentagem', () => {
    expect(faixaMargem(45).id).toBe('boa');
    expect(faixaMargem(30).id).toBe('boa');
    expect(faixaMargem(20).id).toBe('atencao');
    expect(faixaMargem(5).id).toBe('baixa');
    expect(faixaMargem(0).id).toBe('baixa');
    expect(faixaMargem(-3).id).toBe('prejuizo');
  });
});
