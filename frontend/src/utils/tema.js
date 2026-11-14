// Temas disponíveis. As cores de cada um ficam em styles.css ([data-theme="..."]); aqui só o que a tela de escolha precisa.
export const TEMAS = [
  { id: 'verde', nome: 'Verde Kalke', descricao: 'Padrão da loja', cores: { side: '#163b30', bg: '#f4f7f4', line: '#dce7e0', pill: '#1c5c46' } },
  { id: 'escuro', nome: 'Escuro', descricao: 'Descansa a vista à noite', cores: { side: '#0a110e', bg: '#17221e', line: '#2a3b35', pill: '#3fa37c' } },
  { id: 'areia', nome: 'Areia e terracota', descricao: 'Tons quentes de tapete', cores: { side: '#4a2c1d', bg: '#f7f1e8', line: '#eadccb', pill: '#a5512f' } },
  { id: 'marinho', nome: 'Azul-marinho', descricao: 'Sóbrio e profissional', cores: { side: '#132743', bg: '#f2f5f9', line: '#d9e1ec', pill: '#1f4e8c' } },
  { id: 'contraste', nome: 'Alto contraste', descricao: 'Mais fácil de ler', cores: { side: '#000000', bg: '#ffffff', line: '#000000', pill: '#002b80' } }
];
const PADRAO = 'verde';
const CHAVE = 'tk_tema';
const valido = (tema) => TEMAS.some((t) => t.id === tema);

// Aplica o tema na página e guarda neste navegador, para a tela de login já abrir com a última escolha.
export function aplicarTema(tema) {
  const escolhido = valido(tema) ? tema : PADRAO;
  document.documentElement.dataset.theme = escolhido;
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute('content', TEMAS.find((t) => t.id === escolhido).cores.side);
  try { window.localStorage.setItem(CHAVE, escolhido); } catch { /* armazenamento indisponível: vale só nesta visita */ }
  return escolhido;
}

export function temaGuardado() {
  try { return window.localStorage.getItem(CHAVE) ?? PADRAO; } catch { return PADRAO; }
}
