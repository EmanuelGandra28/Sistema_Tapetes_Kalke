import { describe, expect, it } from 'vitest';
import { linkWhatsapp, preencher, telefoneWhatsapp } from './whatsapp';

describe('telefoneWhatsapp', () => {
  it('acrescenta o 55 em celular e fixo', () => {
    expect(telefoneWhatsapp('(11) 98765-4321')).toBe('5511987654321');
    expect(telefoneWhatsapp('11 3456-7890')).toBe('551134567890');
  });
  it('mantém quando já tem 55 e ignora zero à esquerda', () => {
    expect(telefoneWhatsapp('+55 11 98765-4321')).toBe('5511987654321');
    expect(telefoneWhatsapp('011987654321')).toBe('5511987654321');
  });
  it('recusa vazio ou curto demais', () => {
    expect(telefoneWhatsapp('')).toBeNull();
    expect(telefoneWhatsapp(null)).toBeNull();
    expect(telefoneWhatsapp('1234')).toBeNull();
  });
});

describe('linkWhatsapp e preencher', () => {
  it('monta o link com a mensagem codificada', () => {
    const msg = preencher('Olá, {nome}!', { nome: 'Ana' });
    expect(linkWhatsapp('11987654321', msg)).toBe('https://wa.me/5511987654321?text=Ol%C3%A1%2C%20Ana!');
  });
  it('mantém a variável quando falta o dado', () => {
    expect(preencher('Oi {nome}', {})).toBe('Oi {nome}');
  });
  it('sem telefone válido não gera link', () => {
    expect(linkWhatsapp('abc', 'oi')).toBeNull();
  });
});
