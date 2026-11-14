import { useEffect, useState } from 'react';

// Devolve o valor só depois de `delay` ms sem mudanças: evita uma consulta à API a cada tecla digitada.
export function useDebounce(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}
