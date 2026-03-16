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
      // 音乐爬虫服务（网易云、酷狗，port 3000）
      '/music-api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/music-api/, '')
      },
      // NeteaseCloudMusicApi 降级备用（port 3001）
      '/netease-api': {
        target: 'http://localhost:3001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/netease-api/, '')
      },
      // 酷狗搜索 API 降级备用
      '/kugou-api': {
        target: 'http://mobilecdn.kugou.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/kugou-api/, ''),
        headers: {
          referer: 'http://m.kugou.com/',
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15'
        }
      },
      // 酷狗播放 URL 降级备用
      '/kugou-play': {
        target: 'http://m.kugou.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/kugou-play/, ''),
        headers: {
          referer: 'http://m.kugou.com/',
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148'
        }
      },
      // QQ 音乐搜索
      '/qq-api': {
        target: 'https://c.y.qq.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/qq-api/, ''),
        headers: { referer: 'https://y.qq.com/', origin: 'https://y.qq.com' }
      },
      // QQ 音乐播放 URL
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
          'konva': ['konva', 'vue-konva'],
          'xlsx': ['xlsx'],
          'docx-preview': ['docx-preview'],
        }
      }
    },
    chunkSizeWarningLimit: 1000,
  }
})
