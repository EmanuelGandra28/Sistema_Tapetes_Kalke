import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import process from 'node:process';
// O PostCSS do frontend é explícito para não herdar configurações de pastas-pai.
// VITE_BASE é definido no GitHub Actions como "/<repositório>/", o caminho do site no GitHub Pages; localmente fica "/".
export default defineConfig({ base: process.env.VITE_BASE ?? '/', plugins: [react()], css: { postcss: { plugins: [] } }, server: { port: 5173 } });
