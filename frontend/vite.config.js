import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 构建配置
 * - 开发端口固定 5173
 * - /api 代理到后端 http://localhost:8080（后端接口本身即带 /api 前缀，因此不做 rewrite）
 * - 使用 @ 作为 src 目录别名
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 注意：不做 rewrite，后端接口路径本身就带 /api 前缀
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 2048
  }
})
