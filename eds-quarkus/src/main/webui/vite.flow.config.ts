import { defineConfig } from 'vite';

// Builds one user of the flow test (scripts/flow) for Node: Rune's modules
// without a page, in one file, so several users can run as workers.
export default defineConfig({
  build: {
    ssr: 'scripts/flow/user.ts',
    copyPublicDir: false,
    outDir: 'scripts/flow/dist',
    emptyOutDir: true,
    minify: false,
    sourcemap: false,
    rollupOptions: { output: { entryFileNames: 'user.js', format: 'es' } },
  },
  ssr: { noExternal: true },
});
