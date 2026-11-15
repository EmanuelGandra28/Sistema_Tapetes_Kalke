import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
import process from 'node:process';
// O PostCSS do frontend é explícito para não herdar configurações de pastas-pai.
// VITE_BASE é definido no GitHub Actions como "/<repositório>/", o caminho do site no GitHub Pages; localmente fica "/".
const base = process.env.VITE_BASE ?? '/';

// App instalável (PWA): guarda só os arquivos do próprio site para abrir rápido. Nenhuma chamada da API entra em cache,
// para estoque e preços nunca ficarem desatualizados; sem internet o app abre, mas os dados não carregam.
const pwa = VitePWA({
  registerType: 'autoUpdate',
  includeAssets: ['favicon.svg', 'apple-touch-icon.png'],
  manifest: {
    name: 'Tapetes Kalke',
    short_name: 'Kalke',
    description: 'Gestão da loja Tapetes Kalke: vendas, estoque, produção e relatórios.',
    lang: 'pt-BR',
    theme_color: '#1c5c46',
    background_color: '#f3e9d8',
    display: 'standalone',
    start_url: `${base}dashboard`,
    scope: base,
    icons: [
      { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
      { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
      { src: 'pwa-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
    ]
  },
  workbox: {
    globPatterns: ['**/*.{js,css,html,svg,png,ico,webmanifest}'],
    navigateFallback: `${base}index.html`,
    cleanupOutdatedCaches: true,
    runtimeCaching: []
  }
});

export default defineConfig({ base, plugins: [react(), pwa], css: { postcss: { plugins: [] } }, server: { port: 5173 } });
