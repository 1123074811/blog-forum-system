import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import ElementPlus from 'unplugin-element-plus/vite'
import viteCompression from 'vite-plugin-compression'
import { visualizer } from 'rollup-plugin-visualizer'

const isAnalyze = process.env.ANALYZE === 'true'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
    // Transform Element Plus API imports and inject component-level styles.
    ElementPlus({
      useSource: false,
    }),
    // Generate gzip files.
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 1024,
      deleteOriginFile: false,
    }),
    // Generate brotli files.
    viteCompression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 1024,
      deleteOriginFile: false,
    }),
    isAnalyze && visualizer({
      filename: 'dist/stats.html',
      open: true,
      gzipSize: true,
      brotliSize: true,
      template: 'treemap',
    }),
  ].filter(Boolean),
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
      '/blog': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      '/media': {
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
        // Hashed filenames for long-term immutable caching.
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
        manualChunks(id) {
          if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) {
            return 'vendor'
          }
          // Keep Element Plus modules free for Rollup to split by usage
          // instead of forcing a single large shared chunk.
          if (id.includes('node_modules/echarts/') || id.includes('node_modules/zrender/')) return 'echarts'
          if (id.includes('node_modules/konva/') || id.includes('node_modules/vue-konva/')) return 'konva'
          if (id.includes('node_modules/xlsx/')) return 'xlsx'
          if (id.includes('node_modules/docx-preview/')) return 'docx-preview'
          // Split large runtime libraries separately.
          if (id.includes('node_modules/pdfjs-dist/')) return 'pdfjs'
          if (id.includes('node_modules/lottie-web/')) return 'lottie'
        }
      }
    },
    chunkSizeWarningLimit: 1000,
    assetsInlineLimit: 4096,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info'],
      }
    },
    cssCodeSplit: true,
    // Allow browser preload for critical chunks.
    modulePreload: { polyfill: true },
    // Disable source maps in production bundle.
    sourcemap: false,
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios']
  }
})
