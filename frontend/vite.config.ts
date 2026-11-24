/// <reference types="vitest/config" />
import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * The dev tokens of each role (dev-tokens.env at the repository root, US-02), so the dev server
 * can call the API as the persona picked on the sign-in page until real sign-in arrives (US-01).
 * Only the dev server gets them; a production build has none.
 */
function devTokens(): Record<string, string> {
  const file = fileURLToPath(new URL('../dev-tokens.env', import.meta.url));
  if (!existsSync(file)) return {};
  const tokens: Record<string, string> = {};
  for (const line of readFileSync(file, 'utf8').split('\n')) {
    const match = /^TOKEN_([A-Z_]+)=(\S+)$/.exec(line.trim());
    if (match && match[1] !== 'ALL') tokens[match[1]] = match[2];
  }
  return tokens;
}

export default defineConfig(({ command }) => ({
  plugins: [react()],
  define: {
    __DEV_TOKENS__: JSON.stringify(command === 'serve' ? devTokens() : {}),
  },
  server: {
    port: 5173,
    // The Spring Boot backend runs on 8080; /api calls are proxied to it.
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['src/test/setup.ts'],
  },
}));
