import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true,
    proxy: {
      // 开发环境通过代理转发到后端，避免跨域问题
      '/user': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/registration': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
