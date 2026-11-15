// BR Code do PIX estático ("copia e cola"), no padrão EMV do Banco Central. Não confirma pagamento: só monta o código.

// Campo EMV: id (2 dígitos) + tamanho (2 dígitos) + valor.
const campo = (id, valor) => `${id}${String(valor.length).padStart(2, '0')}${valor}`;

// CRC16-CCITT: polinômio 0x1021, valor inicial 0xFFFF; resultado em 4 dígitos hexadecimais maiúsculos.
export function crc16(texto) {
  let crc = 0xffff;
  for (let i = 0; i < texto.length; i++) {
    crc ^= texto.charCodeAt(i) << 8;
    for (let b = 0; b < 8; b++) crc = crc & 0x8000 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff;
  }
  return crc.toString(16).toUpperCase().padStart(4, '0');
}

// Nome e cidade do recebedor: sem acento, em maiúsculas e no tamanho máximo do padrão.
export function normalizar(texto, max) {
  return (texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase().trim().slice(0, max).trim();
}

export function payloadPix({ chave, nome, cidade, valor, txid }) {
  const identificador = (txid ?? '').replace(/[^A-Za-z0-9]/g, '').slice(0, 25) || '***';
  const conta = campo('00', 'br.gov.bcb.pix') + campo('01', chave.trim());
  const semCrc = campo('00', '01')
    + campo('26', conta)
    + campo('52', '0000')
    + campo('53', '986')
    + (Number(valor) > 0 ? campo('54', Number(valor).toFixed(2)) : '')
    + campo('58', 'BR')
    + campo('59', normalizar(nome, 25))
    + campo('60', normalizar(cidade, 15))
    + campo('62', campo('05', identificador))
    + '6304';
  return semCrc + crc16(semCrc);
}
