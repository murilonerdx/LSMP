import { useEffect, useState } from 'react'
import { useEvents } from '../store/events'

/**
 * Widget compacto de status do WebSocket pra sidebar.
 * Click expande pra mostrar diagnóstico completo (close code, last error,
 * próxima tentativa, attempt counter, botão de force reconnect).
 */
export function WsStatus() {
  const state = useEvents((s) => s.state)
  const connected = useEvents((s) => s.connected)
  const attempt = useEvents((s) => s.attempt)
  const lastError = useEvents((s) => s.lastError)
  const lastCloseCode = useEvents((s) => s.lastCloseCode)
  const lastCloseReason = useEvents((s) => s.lastCloseReason)
  const nextRetryAt = useEvents((s) => s.nextRetryAt)
  const url = useEvents((s) => s.url)
  const forceReconnect = useEvents((s) => s.forceReconnect)
  const [open, setOpen] = useState(false)
  const [tick, setTick] = useState(0)

  // Tick a cada 1s pra atualizar countdown do retry
  useEffect(() => {
    const t = setInterval(() => setTick((x) => x + 1), 1000)
    return () => clearInterval(t)
  }, [])

  let label: string, bg: string, dot: string
  switch (state) {
    case 'open': label = 'conectado'; bg = 'badge-green'; dot = 'bg-emerald-400'; break
    case 'connecting': label = 'conectando…'; bg = 'badge-cyan'; dot = 'bg-cyan-400 animate-pulse'; break
    case 'closed': label = 'desconectado'; bg = 'badge-red'; dot = 'bg-red-400 animate-pulse'; break
    case 'error': label = 'erro'; bg = 'badge-red'; dot = 'bg-red-400 animate-pulse'; break
    default: label = 'parado'; bg = 'badge-red'; dot = 'bg-red-400'; break
  }

  const countdown = nextRetryAt ? Math.max(0, Math.ceil((nextRetryAt - Date.now()) / 1000)) : null
  // forcing tick to invalidate cached countdown
  void tick

  return (
    <div className="text-[10px]">
      <button
        className="w-full flex items-center justify-between gap-1 px-1 py-1 rounded hover:bg-liberthia-700/30 transition"
        onClick={() => setOpen((v) => !v)}>
        <span className="text-liberthia-300/70">WebSocket</span>
        <span className={`badge ${bg}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
          {label}
          {!connected && attempt > 0 && ` (try ${attempt})`}
        </span>
      </button>

      {open && (
        <div className="mt-1 px-2 py-2 rounded bg-liberthia-900/60 space-y-1 text-[10px]">
          <div className="flex justify-between gap-2">
            <span className="text-liberthia-300/60">URL</span>
            <span className="font-mono truncate">{url ?? '—'}</span>
          </div>
          <div className="flex justify-between gap-2">
            <span className="text-liberthia-300/60">Tentativas</span>
            <span className="font-mono">{attempt}</span>
          </div>
          {countdown != null && (
            <div className="flex justify-between gap-2">
              <span className="text-liberthia-300/60">Retry em</span>
              <span className="font-mono text-amber-300">{countdown}s</span>
            </div>
          )}
          {lastCloseCode != null && (
            <div className="flex justify-between gap-2">
              <span className="text-liberthia-300/60">Last close</span>
              <span className="font-mono text-red-300">{lastCloseCode}</span>
            </div>
          )}
          {lastCloseReason && (
            <div className="text-red-300/80 break-words">{lastCloseReason}</div>
          )}
          {lastError && (
            <div className="text-red-300/80 break-words">⚠ {lastError}</div>
          )}
          <button
            className="btn-amber btn-sm w-full mt-2"
            onClick={forceReconnect}>
            ↻ Forçar reconexão
          </button>
          <details className="mt-1">
            <summary className="cursor-pointer text-liberthia-300/50">💡 troubleshoot</summary>
            <div className="mt-1 text-liberthia-300/60 space-y-0.5">
              <div>• code 1006 = backend offline</div>
              <div>• code 1008 = bloqueado pelo AuthFilter</div>
              <div>• code 1011 = erro interno do backend</div>
              <div>• DevTools → Network → ws filter pra ver detalhes</div>
            </div>
          </details>
        </div>
      )}
    </div>
  )
}
