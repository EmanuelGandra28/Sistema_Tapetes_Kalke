// Telefone brasileiro só com dígitos e com o código do país (55). Devolve null se não parecer um telefone válido.
export function telefoneWhatsapp(telefone) {
  let d = String(telefone ?? '').replace(/\D/g, '');
  if (d.startsWith('0')) d = d.replace(/^0+/, '');
  if (d.length === 10 || d.length === 11) d = `55${d}`;
  return d.startsWith('55') && (d.length === 12 || d.length === 13) ? d : null;
}

// Texto padrão do botão "Chamar no WhatsApp" (depois passa a vir do modelo editável em Configurações → Mensagens).
export const MENSAGEM_REATIVACAO_PADRAO = 'Olá, {nome}! Aqui é da Tapetes Kalke. Faz um tempinho que não nos vemos. Posso te ajudar com algum tapete?';

export const preencher = (texto, dados) => texto.replace(/\{(\w+)\}/g, (m, chave) => dados[chave] ?? m);

export function linkWhatsapp(telefone, mensagem) {
  const numero = telefoneWhatsapp(telefone);
  return numero ? `https://wa.me/${numero}${mensagem ? `?text=${encodeURIComponent(mensagem)}` : ''}` : null;
}
