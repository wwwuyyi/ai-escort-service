import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  cacheDir: '.vite-cache',
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      // 统一代理到后端；改写 Origin 以匹配后端 CORS 白名单（默认 localhost:5173）。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        headers: { Origin: 'http://localhost:5173' }
      },
      '/upload': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        headers: { Origin: 'http://localhost:5173' }
      }
    }
  }
})