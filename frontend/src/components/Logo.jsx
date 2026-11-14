/**
 * Logomarca Tapetes Kalke: monograma TK num losango em estilo kilim.
 * As cores vêm do tema ativo, para a marca acompanhar Verde, Escuro, Areia, Marinho e Alto contraste.
 * - variante "login": losango na cor principal, letras claras.
 * - variante "menu": losango claro sobre a barra lateral, letras na cor da barra.
 */
export function LogoSimbolo({ variante = 'login', tamanho = 56 }) {
  const fundo = variante === 'menu' ? 'var(--logo-menu-fill)' : 'var(--primary)';
  const letras = variante === 'menu' ? 'var(--sidebar-bg)' : 'var(--on-primary)';
  return <svg className="logo-simbolo" width={tamanho} height={tamanho} viewBox="0 0 120 120" aria-hidden="true" focusable="false">
    <polygon points="60,6 114,60 60,114 6,60" fill={fundo} />
    <polygon points="60,20 100,60 60,100 20,60" fill="none" stroke="var(--accent)" strokeWidth="3" />
    <polygon points="60,6 66,12 60,18 54,12" fill="var(--accent)" />
    <polygon points="60,102 66,108 60,114 54,108" fill="var(--accent)" />
    <polygon points="6,60 12,54 18,60 12,66" fill="var(--accent)" />
    <polygon points="114,60 108,54 102,60 108,66" fill="var(--accent)" />
    <text x="60" y="71" textAnchor="middle" fontFamily="Georgia, 'Times New Roman', serif" fontSize="31" fontWeight="600" fill={letras}>TK</text>
  </svg>;
}

export default function Logo({ variante = 'login', tamanho }) {
  return <div className={`logo logo-${variante}`} role="img" aria-label="Tapetes Kalke">
    <LogoSimbolo variante={variante} tamanho={tamanho ?? (variante === 'menu' ? 40 : 64)} />
    <span className="logo-nome"><span>Tapetes</span><strong>Kalke</strong></span>
  </div>;
}
