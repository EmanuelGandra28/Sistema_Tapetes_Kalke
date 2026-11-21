import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { LogoSimbolo } from './Logo';
import { urlImagem } from '../utils/imagem';

// Mapa produto → foto principal, carregado uma vez e compartilhado pelas listas.
export function useFotosPrincipais() {
  const q = useQuery({ queryKey: ['fotos-principais'], queryFn: () => api.get('/produtos/imagens-principais'), staleTime: 60_000 });
  return q.data?.data ?? {};
}

// Miniatura quadrada da foto principal; sem foto mostra a marca da loja como espaço reservado.
export default function FotoProduto({ imagemId, nome, tamanho = 48 }) {
  const estilo = { width: tamanho, height: tamanho };
  return imagemId
    ? <img className="foto-produto" style={estilo} src={urlImagem(imagemId)} alt={`Foto de ${nome}`} loading="lazy" />
    : <span className="foto-produto vazia" style={estilo} title="Produto sem foto"><LogoSimbolo variante="login" tamanho={Math.round(tamanho * 0.6)} /></span>;
}
