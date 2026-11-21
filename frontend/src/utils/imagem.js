import { api } from '../services/api';

// Endereço público da foto (o <img> não envia o token de login, por isso a leitura no servidor é aberta).
export const urlImagem = (id) => `${api.defaults.baseURL}/imagens/${id}`;

export const LADO_MAXIMO = 1280;

// Escala que mantém a proporção e nunca amplia a foto.
export function escalaReducao(largura, altura, lado = LADO_MAXIMO) {
  const maior = Math.max(largura, altura);
  return maior > lado ? lado / maior : 1;
}

// Reduz a foto no navegador (lado maior até 1280 px, WEBP ou JPEG ~80%) antes de enviar: carrega rápido e poupa o espaço grátis.
/* global createImageBitmap */
export async function reduzirImagem(arquivo) {
  if (!arquivo.type.startsWith('image/')) throw new Error('Escolha um arquivo de imagem (JPG, PNG ou WEBP).');
  const bitmap = await createImageBitmap(arquivo);
  const escala = escalaReducao(bitmap.width, bitmap.height);
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * escala);
  canvas.height = Math.round(bitmap.height * escala);
  canvas.getContext('2d').drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close?.();
  const gerar = (tipo) => new Promise((resolve) => canvas.toBlob(resolve, tipo, 0.8));
  let blob = await gerar('image/webp');
  if (!blob || blob.type !== 'image/webp') blob = await gerar('image/jpeg');
  if (!blob) throw new Error('Não foi possível preparar a foto.');
  return blob;
}
