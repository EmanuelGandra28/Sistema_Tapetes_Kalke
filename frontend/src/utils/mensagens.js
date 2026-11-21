// Apoio da aba "Mensagens": o servidor é quem decide o que vale; aqui só se ajuda quem escreve a ver o resultado na hora.

export const TIPOS_MENSAGEM = {
  AVISO_PEDIDO: { rotulo: 'Aviso de pedido', dica: 'Vai para quem fez a compra' },
  CAMPANHA: { rotulo: 'Campanha', dica: 'Só vai para clientes que aceitaram receber; leva a frase de saída' },
  INTERNA: { rotulo: 'Interna', dica: 'Mensagem para a equipe ou para a dona' },
  ATENDIMENTO: { rotulo: 'Atendimento', dica: 'Já vem escrita quando um funcionário abre a conversa' }
};

export const TAMANHO_MAXIMO = 700;

const CAMPO = /\{([^{}]*)\}/g;

// Campos {nome} que aparecem no texto, sem repetir.
export const camposUsados = (texto) => [...new Set([...texto.matchAll(CAMPO)].map((m) => m[1].trim()))];

// Campos escritos que o gatilho não conhece (ex.: {nme}).
export const camposDesconhecidos = (texto, variaveis) => camposUsados(texto).filter((c) => !variaveis.some((v) => v.nome === c));

// Campos obrigatórios que sumiram do texto.
export const camposFaltando = (texto, obrigatorias) => obrigatorias.filter((o) => !camposUsados(texto).includes(o));

// Mostra a mensagem como o cliente vai ver, usando os exemplos de cada campo (e a frase de saída, nas campanhas).
export function previa(texto, variaveis, rodapeSaida) {
  const exemplos = Object.fromEntries(variaveis.map((v) => [v.nome, v.exemplo]));
  const corpo = texto.replace(CAMPO, (m, c) => exemplos[c.trim()] ?? m);
  return rodapeSaida && !corpo.includes(rodapeSaida) ? `${corpo.trimEnd()}\n\n${rodapeSaida}` : corpo;
}

// Problemas que o servidor também vai recusar: mostrados enquanto a pessoa digita.
export function problemas(texto, modelo) {
  const lista = [];
  if (!texto.trim()) lista.push('O texto não pode ficar vazio.');
  if (texto.length > TAMANHO_MAXIMO) lista.push(`A mensagem passou de ${TAMANHO_MAXIMO} caracteres (tem ${texto.length}).`);
  const desconhecidos = camposDesconhecidos(texto, modelo.variaveis);
  if (desconhecidos.length) lista.push(`Campo desconhecido: ${desconhecidos.map((c) => `{${c}}`).join(', ')}. Use os botões acima para inserir os campos.`);
  if (texto.replace(CAMPO, '').match(/[{}]/)) lista.push('Há uma chave { ou } solta. Os campos devem ficar assim: {nome}.');
  const faltando = camposFaltando(texto, modelo.obrigatorias);
  if (faltando.length) lista.push(`Esta mensagem precisa conter: ${faltando.map((c) => `{${c}}`).join(', ')}.`);
  if (/(?:https?:\/\/|www\.)?(?:bit\.ly|tinyurl\.com|t\.co|goo\.gl|cutt\.ly|is\.gd|ow\.ly|rb\.gy|shorturl\.at|tiny\.cc|buff\.ly)\b/i.test(texto)) lista.push('Não use links encurtados: o WhatsApp costuma bloquear.');
  return lista;
}

// Insere um campo na posição do cursor (ou troca o trecho selecionado) e devolve o novo texto e onde o cursor deve ficar.
export function inserirCampo(texto, inicio, fim, campo) {
  const marca = `{${campo}}`;
  return { texto: texto.slice(0, inicio) + marca + texto.slice(fim), cursor: inicio + marca.length };
}
