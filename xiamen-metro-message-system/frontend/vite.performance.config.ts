import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { compression } from 'vite-plugin-compression2'
import { visualizer } from 'rollup-plugin-visualizer'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: [
        'vue',
        'vue-router',
        'pinia',
        '@vueuse/core'
      ],
      dts: true,
      eslintrc: {
        enabled: true
      }
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: true
    }),
    // Gzip压缩
    compression({
      algorithm: 'gzip',
      ext: '.gz'
    }),
    // Brotli压缩
    compression({
      algorithm: 'brotliCompress',
      ext: '.br'
    }),
    // 打包分析
    visualizer({
      open: false,
      gzipSize: true,
      brotliSize: true
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    },
    // 启用HTTP/2
    https: false,
    // 启用热更新
    hmr: true,
    // 预构建依赖
    force: true
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    chunkSizeWarningLimit: 1000,
    minify: 'esbuild',
    target: 'es2015',
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 构建优化
    rollupOptions: {
      output: {
        // 代码分割策略
        manualChunks: {
          // Vue核心库
          'vue-core': ['vue', 'vue-router', 'pinia'],
          // UI组件库
          'element-plus': ['element-plus'],
          // 图表库
          'echarts': ['echarts', 'vue-echarts'],
          // 工具库
          'utils': ['axios', 'dayjs', 'lodash-es', '@vueuse/core'],
          // 文件处理
          'file-utils': ['xlsx', 'file-saver', 'html2canvas', 'jspdf'],
          // 样式
          'styles': ['element-plus/dist/index.css']
        },
        // 文件命名策略
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId
            ? chunkInfo.facadeModuleId.split('/').pop().replace(/\.\w+$/, '')
            : 'chunk'
          return `assets/js/${facadeModuleId}-[hash].js`
        },
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.')
          const ext = info[info.length - 1]
          if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(ext)) {
            return `assets/images/[name]-[hash][extname]`
          }
          if (/css/i.test(ext)) {
            return `assets/css/[name]-[hash][extname]`
          }
          return `assets/[ext]/[name]-[hash][extname]`
        }
      },
      // 启用treeshaking
      treeshake: true
    },
    // 构建报告
    reportCompressedSize: false,
    // 启用CSS压缩
    cssMinify: true
  },
  // 依赖优化
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'element-plus',
      'axios',
      'dayjs',
      'lodash-es',
      '@vueuse/core',
      'echarts',
      'vue-echarts',
      'xlsx',
      'file-saver'
    ],
    exclude: [
      // 排除不需要预构建的包
    ]
  },
  // CSS优化
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/assets/styles/variables.scss" as *;`
      }
    },
    // CSS模块化
    modules: {
      localsConvention: 'camelCase'
    }
  },
  // 性能优化
  define: {
    // 环境变量
    __VUE_OPTIONS_API__: false,
    __VUE_PROD_DEVTOOLS__: false,
    __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: false
  },
  // 预渲染配置
  ssr: {
    // 服务端渲染配置（如果需要）
  }
})