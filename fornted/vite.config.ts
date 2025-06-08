import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  // 生产环境下禁用 console
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
      // 仅在开发和测试环境启用 vue-devtools
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
          // target: isProd ? 'https://lumenglover.com' : 'http://localhost:8123',
          target: 'https://lumenglover.com' ,
          changeOrigin: true,
          ws: true
        }
      },
      // headers: {
      //   'Cross-Origin-Opener-Policy': 'same-origin',
      //   'Cross-Origin-Embedder-Policy': 'require-corp'
      // }
    },
    // 生产环境特定配置
    build: {
      // 移除 console
      terserOptions: {
        compress: {
          drop_console: isProd,
          drop_debugger: isProd,
        },
      },
    },
  }
})
