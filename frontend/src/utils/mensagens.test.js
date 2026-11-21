import { describe, expect, it } from 'vitest';
import { camposDesconhecidos, camposFaltando, inserirCampo, previa, problemas, TAMANHO_MAXIMO } from './mensagens';

const modelo = {
  variaveis: [{ nome: 'nome', exemplo: 'Maria' }, { nome: 'loja', exemplo: 'Tapetes Kalke' }, { nome: 'lista_tapetes', exemplo: '1) Tapete A' }],
  obrigatorias: ['lista_tapetes']
};

describe('campos', () => {
  it('acha campos desconhecidos e obrigatórios que sumiram', () => {
    expect(camposDesconhecidos('Oi {nme}, {nome}', modelo.variaveis)).toEqual(['nme']);
    expect(camposFaltando('Oi {nome}', modelo.obrigatorias)).toEqual(['lista_tapetes']);
    expect(camposFaltando('Veja {lista_tapetes}', modelo.obrigatorias)).toEqual([]);
  });
});

describe('previa', () => {
  it('troca pelos exemplos e mantém o que não conhece', () => {
    expect(previa('Oi {nome}, aqui é a {loja}. {x}', modelo.variaveis)).toBe('Oi Maria, aqui é a Tapetes Kalke. {x}');
  });
  it('campanha mostra a frase de saída uma única vez', () => {
    const saida = 'Responda SAIR para não receber mais mensagens.';
    expect(previa('Oi {nome}', modelo.variaveis, saida)).toBe(`Oi Maria\n\n${saida}`);
    expect(previa(`Oi. ${saida}`, modelo.variaveis, saida).split('SAIR').length - 1).toBe(1);
  });
});

describe('problemas', () => {
  it('texto correto não tem problemas', () => {
    expect(problemas('Oi {nome}: {lista_tapetes}', modelo)).toEqual([]);
  });
  it('aponta vazio, campo errado, chave solta, obrigatório, tamanho e link encurtado', () => {
    expect(problemas('  ', modelo).length).toBeGreaterThan(0);
    expect(problemas('Oi {nme} {lista_tapetes}', modelo).join(' ')).toContain('{nme}');
    expect(problemas('Oi {nome {lista_tapetes}', modelo).join(' ')).toContain('solta');
    expect(problemas('Oi {nome}', modelo).join(' ')).toContain('{lista_tapetes}');
    expect(problemas(`{lista_tapetes} ${'a'.repeat(TAMANHO_MAXIMO)}`, modelo).join(' ')).toContain('passou de');
    expect(problemas('{lista_tapetes} https://bit.ly/x', modelo).join(' ')).toContain('encurtados');
  });
});

describe('inserirCampo', () => {
  it('insere no cursor e troca a seleção', () => {
    expect(inserirCampo('Olá, !', 5, 5, 'nome')).toEqual({ texto: 'Olá, {nome}!', cursor: 11 });
    expect(inserirCampo('Olá, XXXX!', 5, 9, 'nome').texto).toBe('Olá, {nome}!');
  });
});
