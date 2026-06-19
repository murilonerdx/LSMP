import { create } from 'zustand'

export type LiveEvent = {
  type: string
  ts: number
  data: any
}

export type WsState = 'idle' | 'connecting' | 'open' | 'closed' | 'error'

type EventStore = {
  events: LiveEvent[]
  ws: WebSocket | null
  connected: boolean
  state: WsState
  attempt: number
  reconnectTimer: number | null
  lastError: string | null
  lastCloseCode: number | null
  lastCloseReason: string | null
  lastAttemptAt: number | null
  nextRetryAt: number | null
  url: string | null
  subs: Set<(ev: LiveEvent) => void>
  connect: () => void
  disconnect: () => void
  reset: () => void
  forceReconnect: () => void
  push: (ev: LiveEvent) => void
  clear: () => void
  subscribe: (fn: (ev: LiveEvent) => void) => () => void
}

export const useEvents = create<EventStore>((set, get) => ({
  events: [],
  ws: null,
  connected: false,
  state: 'idle',
  attempt: 0,
  reconnectTimer: null,
  lastError: null,
  lastCloseCode: null,
  lastCloseReason: null,
  lastAttemptAt: null,
  nextRetryAt: null,
  url: null,
  subs: new Set(),
  connect: () => {
    if (get().ws) return
    if (!localStorage.getItem('liberthia.token')) {
      console.warn('[WS] sem token — não conectando')
      set({ state: 'idle', lastError: 'sem token (login necessário)' })
      return
    }

    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${proto}//${window.location.host}/ws`
    console.info('[WS] conectando em', url, 'attempt=', get().attempt)
    set({ state: 'connecting', url, lastAttemptAt: Date.now(), nextRetryAt: null })

    let ws: WebSocket
    try { ws = new WebSocket(url) }
    catch (e: any) {
      console.error('[WS] new WebSocket() falhou:', e)
      set({ state: 'error', lastError: `new WebSocket() falhou: ${e?.message ?? e}` })
      scheduleReconnect(set, get)
      return
    }

    ws.onopen = () => {
      console.info('[WS] ✓ aberto')
      set({ connected: true, state: 'open', attempt: 0, lastError: null, lastCloseCode: null, lastCloseReason: null })
    }
    ws.onclose = (e) => {
      const reason = explainCloseCode(e.code, e.reason)
      console.warn('[WS] fechado: code', e.code, '—', reason)
      set({
        connected: false,
        ws: null,
        state: 'closed',
        lastCloseCode: e.code,
        lastCloseReason: reason,
      })
      scheduleReconnect(set, get)
    }
    ws.onerror = (ev) => {
      const msg = '(navegador não expõe detalhe — veja Network tab pra status HTTP)'
      console.error('[WS] erro:', ev)
      set({ state: 'error', lastError: msg })
      try { ws.close() } catch {}
    }
    ws.onmessage = (m) => {
      try {
        const ev = JSON.parse(m.data) as LiveEvent
        get().push(ev)
      } catch (e) {
        console.warn('[WS] parse falhou:', e, m.data)
      }
    }
    set({ ws })
  },
  disconnect: () => {
    const t = get().reconnectTimer
    if (t) clearTimeout(t)
    const ws = get().ws
    if (ws) { try { ws.close() } catch {} }
    set({ ws: null, connected: false, state: 'idle', attempt: 0, reconnectTimer: null, nextRetryAt: null })
  },
  reset: () => {
    set({ attempt: 0, lastError: null, lastCloseCode: null, lastCloseReason: null })
  },
  forceReconnect: () => {
    const t = get().reconnectTimer
    if (t) clearTimeout(t)
    const ws = get().ws
    if (ws) { try { ws.close() } catch {} }
    set({ ws: null, connected: false, state: 'idle', attempt: 0, reconnectTimer: null, nextRetryAt: null })
    setTimeout(() => get().connect(), 50)
  },
  push: (ev) => {
    set((s) => ({ events: [ev, ...s.events].slice(0, 200) }))
    get().subs.forEach((fn) => { try { fn(ev) } catch {} })
  },
  clear: () => set({ events: [] }),
  subscribe: (fn) => {
    const subs = get().subs
    subs.add(fn)
    return () => { subs.delete(fn) }
  },
}))

/**
 * Backoff exponencial: 2s, 4s, 8s, 16s, 30s (cap).
 */
function scheduleReconnect(
  set: (s: Partial<ReturnType<typeof useEvents.getState>>) => void,
  get: () => ReturnType<typeof useEvents.getState>,
) {
  const cur = get()
  if (cur.reconnectTimer) return
  const next = Math.min(30000, 2000 * Math.pow(2, cur.attempt))
  const nextRetryAt = Date.now() + next
  console.info('[WS] próxima tentativa em', next, 'ms (attempt=', cur.attempt + 1, ')')
  const timer = window.setTimeout(() => {
    set({ reconnectTimer: null, attempt: cur.attempt + 1, nextRetryAt: null })
    get().connect()
  }, next)
  set({ reconnectTimer: timer, nextRetryAt })
}

/** Tradução de WebSocket close codes (RFC 6455 + Spring). */
function explainCloseCode(code: number, reason: string): string {
  const r = reason ? ` "${reason}"` : ''
  switch (code) {
    case 1000: return 'normal close' + r
    case 1001: return 'going away (servidor desligou)' + r
    case 1002: return 'protocol error' + r
    case 1003: return 'data não suportada' + r
    case 1005: return 'sem código (provavelmente backend OFFLINE ou bloqueado por proxy)' + r
    case 1006: return 'fechamento anormal — backend OFFLINE ou rede caiu' + r
    case 1008: return 'policy violation (auth?)' + r
    case 1011: return 'erro interno do servidor' + r
    case 1015: return 'TLS handshake fail' + r
    case 4401: return 'unauthorized (401 do AuthFilter — restart do backend)' + r
    case 4403: return 'forbidden (403)' + r
    default: return `código ${code}${r}`
  }
}
