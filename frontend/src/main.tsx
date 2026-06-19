import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import './index.css'

/**
 * Defaults pensados para "painel admin que fica aberto o dia todo".
 *
 *  • `refetchIntervalInBackground: false` — quando a aba é minimizada,
 *    PARA os polls. Senão browsers throttlam `setTimeout` para ~1Hz e
 *    quando você volta pra aba ele dispara um BURST de refetches
 *    acumulados que trava a UI por 1-2s.
 *  • `refetchOnWindowFocus: false` — mesma razão. Sem isso, toda vez que
 *    você muda de aba, TODAS as queries ativas re-fazem fetch ao mesmo
 *    tempo.
 *  • SEM `refetchInterval` global — cada página declara o seu próprio.
 *    Tinha um default global de 5000ms que silenciosamente fazia poll
 *    em ~40 queries diferentes mesmo quando a página nem precisava.
 *  • `staleTime: 2000` — cache curto, mas evita refetch redundante
 *    quando vários componentes pedem a mesma queryKey em sequência.
 */
/**
 * Retry inteligente:
 *  - 401 → não retry (sessão expirou, frontend trata via redirect)
 *  - 503 mod_offline → retry com backoff (mod pode voltar a qualquer momento)
 *  - Outros 5xx → retry 3x com backoff exponencial
 *  - Network error → retry 3x com backoff exponencial
 *  - 4xx (exceto 401, 408, 429) → não retry (bug do cliente, retry não resolve)
 */
function smartRetry(failureCount: number, error: any): boolean {
  // Erro do react-query vem como Error com message contendo "HTTP NNN: <body>"
  const msg = String(error?.message ?? error ?? '')
  const m = msg.match(/^HTTP (\d{3})/)
  const status = m ? parseInt(m[1], 10) : 0

  if (status === 401 || status === 403) return false
  if (status === 404) return false
  if (status >= 400 && status < 500 && status !== 408 && status !== 429) return false
  // 5xx e network errors retentam até 4x
  return failureCount < 4
}

function smartRetryDelay(attempt: number): number {
  // 500ms, 1s, 2s, 4s — capped em 6s
  return Math.min(500 * Math.pow(2, attempt), 6000)
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 2000,
      refetchIntervalInBackground: false,
      refetchOnWindowFocus: false,
      retry: smartRetry,
      retryDelay: smartRetryDelay,
    },
    mutations: {
      retry: 0, // mutations não retentam — usuário decide reaplicar
    },
  }
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)
