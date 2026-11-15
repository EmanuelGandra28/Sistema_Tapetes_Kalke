import { useCallback } from 'react';
import { baixarArquivo } from '../services/download';

// Função de exportação para os botões "Exportar Excel": baixa o arquivo e avisa se der erro.
export function useExportar() {
  return useCallback(async (url, params, nome) => {
    try {
      await baixarArquivo(url, params, nome);
    } catch (e) {
      window.alert(e.message);
    }
  }, []);
}
