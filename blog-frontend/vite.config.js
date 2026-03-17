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
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          // 核心框架单独chunk，浏览器长期缓存
          if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) {
            return 'vendor'
          }
          if (id.includes('node_modules/element-plus/')) return 'element-plus'
          if (id.includes('node_modules/echarts/') || id.includes('node_modules/zrender/')) return 'echarts'
          if (id.includes('node_modules/md-editor-v3/')) return 'md-editor'
          if (id.includes('node_modules/konva/') || id.includes('node_modules/vue-konva/')) return 'konva'
          if (id.includes('node_modules/xlsx/')) return 'xlsx'
          if (id.includes('node_modules/docx-preview/')) return 'docx-preview'
          // 其余node_modules合并为一个chunk
          if (id.includes('node_modules/')) return 'vendor-misc'
        }
      }
    },
    chunkSizeWarningLimit: 1000,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info'],
      }
    },
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 预加载指令生成
    modulePreload: {
      polyfill: true
    }
  }
})
