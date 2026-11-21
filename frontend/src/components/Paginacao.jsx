// Controle simples de páginas ("Anterior / Próxima / página X de Y"); some quando há uma página só.
// `page` é o número da página começando em 0; `data` é a resposta paginada do servidor (content, totalPages, totalElements).
export default function Paginacao({ page, data, onChange }) {
  const total = data?.totalPages ?? 0;
  if (total <= 1) return null;
  return <nav className="paginacao" aria-label="Páginas">
    <button type="button" className="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>Anterior</button>
    <span>Página {page + 1} de {total} · {data.totalElements} registros</span>
    <button type="button" className="secondary" disabled={page + 1 >= total} onClick={() => onChange(page + 1)}>Próxima</button>
  </nav>;
}
