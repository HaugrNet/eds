import { defineConfig } from 'vite';

// The PWA is served by Quinoa at the root of the stone, in dev mode through
// the Vite dev server. Asset paths are relative, so it also works below a
// path prefix of a reverse proxy.
export default defineConfig({
  base: './',
  build: {
    outDir: 'dist',
    // No inline scripts or styles, so the CSP needs no 'unsafe-inline'.
    assetsInlineLimit: 0,
    sourcemap: false,
  },
  server: {
    port: 5173,
    strictPort: true,
  },
});
