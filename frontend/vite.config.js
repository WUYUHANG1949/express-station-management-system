import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 构建配置
 * - 开发端口固定 5173
 * - /api 代理到后端 http://localhost:8080（后端接口本身即带 /api 前缀，因此不做 rewrite）
 * - 使用 @ 作为 src 目录别名
 * - 开发服务器对所有响应加 no-store，避免浏览器缓存旧模块导致"内容区空白"
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
    // 开发服务器重启后，若浏览器仍持有旧模块的缓存，懒加载的路由组件会 404，
    // 表现为"左侧菜单和顶栏正常、内容区一片空白"。这里对入口与模块统一禁用缓存，
    // 配合 router.onError 的自动刷新，形成双重保险。
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0',
      Pragma: 'no-cache',
      Expires: '0'
    },
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
