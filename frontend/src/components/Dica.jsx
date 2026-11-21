import { useState } from 'react';

// Pontinho "?" ao lado de um campo difícil: abre uma explicação curta (clique ou teclado; também funciona no celular).
export default function Dica({ texto }) {
  const [aberta, setAberta] = useState(false);
  return <span className="dica">
    <button type="button" className="dica-botao" aria-expanded={aberta} aria-label="O que é isto?" title={texto} onClick={() => setAberta(!aberta)}>?</button>
    {aberta && <span className="dica-texto" role="note">{texto}</span>}
  </span>;
}
