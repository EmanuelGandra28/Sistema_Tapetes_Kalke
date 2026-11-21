import { useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import { reduzirImagem, urlImagem } from '../utils/imagem';
import Modal from './Modal';
import { Loading, Failure } from './State';

// Fotos de um produto: enviar (reduzidas no navegador), escolher a principal e remover.
export default function FotosProdutoModal({ produto, podeEditar, onClose }) {
  const queryClient = useQueryClient();
  const entrada = useRef(null);
  const q = useQuery({ queryKey: ['fotos', produto.id], queryFn: () => api.get(`/produtos/${produto.id}/imagens`) });
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['fotos', produto.id] }); queryClient.invalidateQueries({ queryKey: ['fotos-principais'] }); };
  const enviar = useMutation({
    mutationFn: async (arquivos) => {
      for (const arquivo of arquivos) {
        const blob = await reduzirImagem(arquivo);
        const dados = new FormData();
        dados.append('arquivo', blob, 'foto');
        await api.post(`/produtos/${produto.id}/imagens`, dados, { headers: { 'Content-Type': 'multipart/form-data' } });
      }
    },
    onSettled: () => { refresh(); if (entrada.current) entrada.current.value = ''; }
  });
  const principal = useMutation({ mutationFn: (id) => api.put(`/produtos/${produto.id}/imagens/${id}/principal`), onSuccess: refresh });
  const remover = useMutation({ mutationFn: (id) => api.delete(`/produtos/${produto.id}/imagens/${id}`), onSuccess: refresh });
  const fotos = q.data?.data ?? [];
  const erro = enviar.error ?? principal.error ?? remover.error;
  return <Modal title={`Fotos — ${produto.nome}`} onClose={onClose}>
    {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : <>
      {fotos.length === 0 && <p>Este produto ainda não tem fotos.</p>}
      <div className="galeria">{fotos.map((f) => <figure key={f.id} className={f.principal ? 'principal' : ''}>
        <img src={urlImagem(f.id)} alt={`Foto de ${produto.nome}`} loading="lazy" />
        <figcaption>{f.principal ? <strong>Foto principal</strong> : podeEditar && <button type="button" className="link" onClick={() => principal.mutate(f.id)}>Tornar principal</button>}
          {podeEditar && <button type="button" className="link danger" onClick={() => window.confirm('Remover esta foto?') && remover.mutate(f.id)}>Remover foto</button>}</figcaption>
      </figure>)}</div>
      {podeEditar && <p><label className="secondary botao-arquivo">{enviar.isPending ? 'Enviando…' : 'Anexar fotos'}
        <input ref={entrada} type="file" accept="image/jpeg,image/png,image/webp" multiple hidden disabled={enviar.isPending}
               onChange={(e) => e.target.files.length && enviar.mutate([...e.target.files])} /></label>
        <small> Até 8 fotos. Elas são reduzidas automaticamente antes de enviar.</small></p>}
      {erro && <p className="error">{erro.message}</p>}
    </>}
    <footer><button type="button" className="secondary" onClick={onClose}>Fechar</button></footer>
  </Modal>;
}
