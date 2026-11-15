// Abas simples dentro de uma página; `abas` = [{ id, rotulo }].
export default function Abas({ abas, ativa, onChange }) {
  return <nav className="tabs" role="tablist">{abas.map((a) => <button key={a.id} type="button" role="tab" aria-selected={ativa === a.id}
    className={ativa === a.id ? 'tab active' : 'tab'} onClick={() => onChange(a.id)}>{a.rotulo}</button>)}</nav>;
}
