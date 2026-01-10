import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/music-api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/music-api/, '')
      },
      // 酷狗搜索API代理
      '/kugou-api': {
        target: 'http://mobilecdn.kugou.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/kugou-api/, ''),
        headers: {
          referer: 'http://m.kugou.com/',
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15'
        }
      },
      // 酷狗播放URL获取接口
      '/kugou-play': {
        target: 'http://m.kugou.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/kugou-play/, ''),
        headers: {
          referer: 'http://m.kugou.com/',
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148'
        }
      },
      // QQ音乐API代理
      '/qq-api': {
        target: 'https://c.y.qq.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/qq-api/, ''),
        headers: { referer: 'https://y.qq.com/', origin: 'https://y.qq.com' }
      },
      // QQ音乐播放URL代理
      '/qq-play': {
        target: 'https://u.y.qq.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/qq-play/, ''),
        headers: { referer: 'https://y.qq.com/', origin: 'https://y.qq.com' }
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'echarts': ['echarts'],
          'md-editor': ['md-editor-v3'],
        }
      }
    }
  }
})
