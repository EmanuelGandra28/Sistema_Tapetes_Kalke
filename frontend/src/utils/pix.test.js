import { describe, expect, it } from 'vitest';
import { crc16, normalizar, payloadPix } from './pix';

describe('crc16', () => {
  it('usa CRC16-CCITT (0x1021, inicial 0xFFFF)', () => {
    expect(crc16('123456789')).toBe('29B1');
  });
});

describe('normalizar', () => {
  it('tira acentos, põe em maiúsculas e corta no limite', () => {
    expect(normalizar('São José dos Campos', 15)).toBe('SAO JOSE DOS CA');
    expect(normalizar('  Tapetes Kalke Ltda.  ', 25)).toBe('TAPETES KALKE LTDA.');
  });
});

describe('payloadPix', () => {
  const payload = payloadPix({ chave: 'loja@kalke.com.br', nome: 'Tapetes Kalke', cidade: 'Domingos Martins', valor: 199.9, txid: 'VENDA-12' });

  it('segue o formato EMV do BR Code', () => {
    expect(payload.startsWith('000201')).toBe(true);
    expect(payload).toContain('0014br.gov.bcb.pix0117loja@kalke.com.br');
    expect(payload).toContain('52040000');
    expect(payload).toContain('5303986');
    expect(payload).toContain('5406199.90');
    expect(payload).toContain('5802BR');
    expect(payload).toContain('5913TAPETES KALKE');
    expect(payload).toContain('6015DOMINGOS MARTIN');
    expect(payload).toContain('62110507VENDA12');
  });

  it('termina com o CRC do restante', () => {
    const semCrc = payload.slice(0, -4);
    expect(semCrc.endsWith('6304')).toBe(true);
    expect(payload.slice(-4)).toBe(crc16(semCrc));
  });

  it('usa *** quando não há identificador', () => {
    expect(payloadPix({ chave: 'x', nome: 'A', cidade: 'B', valor: 1 })).toContain('62070503***');
  });
});

describe('exemplo do manual do BR Code (Banco Central)', () => {
  it('reproduz o CRC 1D3D', () => {
    expect(crc16('00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304')).toBe('1D3D');
  });
});
