import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  let apiTarget = 'http://localhost:8123'
  // let apiTarget = 'https://www.yuemutuku.com'
  if (mode === 'production') {
    apiTarget = 'https://www.yuemutuku.com'
  } else if (mode === 'staging') {
    apiTarget = 'https://lumenglover.com'
  }

  const isProd = mode === 'production'
  if (isProd) {
    console.log = () => { }
    console.error = () => { }
    console.warn = () => { }
    console.info = () => { }
    console.debug = () => { }
  }

  return {
    plugins: [
      vue(),
      !isProd && vueDevTools(),
      AutoImport({
        resolvers: [VantResolver()],
      }),
      Components({
        resolvers: [VantResolver()],
      }),
    ].filter(Boolean),
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          ws: true,
          timeout: 600000,
          proxyTimeout: 600000 ,
          buffer: false,
        }
      },
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Origin, X-Requested-With, Content-Type, Accept, Authorization',
        'Referrer-Policy': 'origin-when-cross-origin'
      }
    },
    build: {
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: isProd,
          drop_debugger: isProd,
          pure_funcs: ['console.log', 'console.info', 'console.warn', 'console.error', 'console.debug']
        },
        format: {
          comments: false,
        }
      },
    },
  }
})
