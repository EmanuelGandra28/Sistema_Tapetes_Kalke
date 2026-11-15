import { api } from './api';

// Baixa um arquivo da API (Excel). O interceptor do `api` devolve só o corpo; aqui pedimos o blob e montamos o download.
// Em erro, o corpo vem como blob com o JSON da API: lemos para mostrar a mensagem certa.
export async function baixarArquivo(url, params = {}, nomePadrao = 'arquivo') {
  let blob;
  try {
    blob = await api.get(url, { params, responseType: 'blob', headers: { Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/json' } });
  } catch (erro) {
    let mensagem = erro?.message;
    if (erro instanceof window.Blob) {
      try { mensagem = JSON.parse(await erro.text()).message; } catch { mensagem = null; }
    }
    throw new Error(mensagem ?? 'Não foi possível exportar.');
  }
  const hoje = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date());
  const link = document.createElement('a');
  link.href = window.URL.createObjectURL(blob);
  link.download = `${nomePadrao}-${hoje}.xlsx`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => window.URL.revokeObjectURL(link.href), 1000);
}
