import crypto from 'node:crypto'
import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

if (typeof (crypto as typeof crypto & { hash?: unknown }).hash !== 'function') {
  ;(crypto as any).hash = (
    algorithm: string,
    data: crypto.BinaryLike,
    outputEncoding: crypto.BinaryToTextEncoding = 'hex',
  ) => crypto.createHash(algorithm).update(data).digest(outputEncoding)
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  base: '/'
})
