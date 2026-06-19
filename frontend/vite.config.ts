import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0', // permite acessar pelo IP local (não só localhost)
    // HMR via WS no mesmo host:porta da página — evita conflito com nosso /ws
    hmr: {
      clientPort: 5173,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            if ((req.headers.accept || '').includes('text/event-stream')) {
              proxyReq.setHeader('Cache-Control', 'no-cache')
            }
          })
          proxy.on('error', (err, req) => {
            console.error('[vite-proxy /api] ERRO:', err.message, 'url=', req.url)
          })
        },
      },
      // /ws — proxy WebSocket pro backend
      '/ws': {
        target: 'ws://localhost:8090',
        ws: true,
        changeOrigin: true,
        rewriteWsOrigin: true,
        configure: (proxy) => {
          proxy.on('error', (err, req) => {
            console.error('[vite-proxy /ws] ERRO:', err.message, 'url=', req.url)
          })
          // Estes eventos só disparam se proxy.ws=true e a request for upgrade
          proxy.on('proxyReqWs', (_proxyReq, req) => {
            console.info('[vite-proxy /ws] upgrade request →', req.url, 'origin=', req.headers.origin)
          })
          proxy.on('open', () => {
            console.info('[vite-proxy /ws] ✓ socket aberto pra backend')
          })
          proxy.on('close', () => {
            console.warn('[vite-proxy /ws] ✗ socket fechado pra backend')
          })
        },
      },
    },
  },
})
