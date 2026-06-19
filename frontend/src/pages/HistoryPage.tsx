import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useEvents } from '../store/events'

/**
 * Histórico de chat + comandos do servidor. Persistido em disco do server,
 * RAM ring de 5000 entradas, lê via /api/history/{chat,commands}.
 *
 * Atualização em tempo real:
 *  - WebSocket subscribe: ao receber evento de chat/command, invalida o
 *    cache da React Query imediatamente → refetch instantâneo
 *  - Polling de fallback a cada 1.5s (era 3s, mais sensível agora)
 */
export function HistoryPage() {
  const qc = useQueryClient()
  const subscribe = useEvents((s) => s.subscribe)
  const wsConnected = useEvents((s) => s.connected)
  const [tab, setTab] = useState<'chat' | 'commands' | 'site'>('chat')
  const [search, setSearch] = useState('')
  const [playerFilter, setPlayerFilter] = useState('')
  const [auto, setAuto] = useState(true)
  const [autoScroll, setAutoScroll] = useState(true)
  const listRef = useRef<HTMLDivElement>(null)

  // Subscribe nos eventos do WebSocket — chat/command vindos do mod em tempo
  // real. Quando o evento chega, invalida o cache da React Query daquele tab
  // → próximo render usa dados frescos (refetch dispara automaticamente).
  useEffect(() => {
    return subscribe((ev) => {
      const t = ev.type
      if (t === 'chat' || t === 'player_chat' || t === 'chat_message') {
        qc.invalidateQueries({ queryKey: ['history-chat'] })
      } else if (t === 'command' || t === 'player_command' || t === 'console_command') {
        qc.invalidateQueries({ queryKey: ['history-cmd'] })
      } else if (t === 'site_command' || t === 'admin_command') {
        qc.invalidateQueries({ queryKey: ['history-site'] })
      }
    })
  }, [subscribe, qc])

  const chatQ = useQuery({
    queryKey: ['history-chat'],
    queryFn: () => api.chatHistory('', 0, 500),
    refetchInterval: auto ? 1500 : false,
    refetchOnWindowFocus: auto,
  })
  const cmdQ = useQuery({
    queryKey: ['history-cmd'],
    queryFn: () => api.cmdHistory('', 0, 500),
    refetchInterval: auto ? 1500 : false,
    refetchOnWindowFocus: auto,
  })
  const siteQ = useQuery({
    queryKey: ['history-site'],
    queryFn: () => api.siteCmdHistory(0, 500),
    refetchInterval: auto ? 1500 : false,
    refetchOnWindowFocus: auto,
  })

  const data: any[] = tab === 'chat' ? (chatQ.data ?? []) : tab === 'commands' ? (cmdQ.data ?? []) : (siteQ.data ?? [])

  // Auto-scroll pro fim quando vem mensagem nova (se ligado e usuário tá perto do fim)
  useEffect(() => {
    if (!autoScroll || !listRef.current) return
    const el = listRef.current
    // só rola se já está perto do fim (não atrapalha quem tá lendo histórico)
    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 150
    if (isNearBottom) el.scrollTop = el.scrollHeight
  }, [data.length, autoScroll])
  const players = useMemo(() => {
    if (tab === 'site') return [] // não tem player em site cmds
    return Array.from(new Set(data.map((e: any) => e.name).filter(Boolean))).sort()
  }, [data, tab])
  const filtered = data.filter((e: any) => {
    if (tab !== 'site' && playerFilter && e.name !== playerFilter) return false
    if (search) {
      const v = (tab === 'chat' ? e.message : e.command).toLowerCase()
      if (!v.includes(search.toLowerCase())) return false
    }
    return true
  })

  function exportCsv() {
    const lines = filtered.map((e: any) => {
      const dt = new Date(e.ts).toISOString()
      const text = ((tab === 'chat' ? e.message : e.command) as string).split('"').join('""')
      const who = tab === 'site' ? (e.origin ?? 'site') : (e.name ?? '')
      return `"${dt}","${who}","${text}"`
    })
    const blob = new Blob([`"timestamp","who","${tab}"\n` + lines.join('\n')], { type: 'text/csv' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `liberthia-${tab}-${Date.now()}.csv`
    a.click()
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📚 Histórico</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Chat e comandos do servidor (últimas 5000). Persistidos em disco do server.
          </p>
        </div>
        <div className="flex gap-2 items-center">
          <span className={`badge ${wsConnected ? 'badge-green' : 'badge-red'}`}
            title={wsConnected ? 'WebSocket ON — atualiza instantâneo no evento' : 'WS offline — só polling'}>
            {wsConnected ? '🔌 WS' : '⚠ WS off'}
          </span>
          <span className={`badge ${auto ? 'badge-green' : 'badge-purple'}`}>
            {auto && <span className="live-dot" />}
            {auto ? 'live 1.5s' : 'pausado'}
          </span>
          <button className="btn-ghost btn-sm" onClick={() => setAuto((a) => !a)}>{auto ? '⏸ Pause' : '▶ Resume'}</button>
          <button className={`btn-ghost btn-sm ${autoScroll ? 'ring-1 ring-purple-400' : ''}`}
            onClick={() => setAutoScroll((s) => !s)}
            title="Auto-scroll pro fim quando vem mensagem nova">
            {autoScroll ? '⬇ Scroll' : '↕ Manual'}
          </button>
          <button className="btn-ghost btn-sm" onClick={exportCsv}>⬇ CSV</button>
        </div>
      </header>

      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="tab-strip">
          <div className={`tab-item ${tab === 'chat' ? 'active' : ''}`} onClick={() => setTab('chat')}>💬 Chat ({chatQ.data?.length ?? 0})</div>
          <div className={`tab-item ${tab === 'commands' ? 'active' : ''}`} onClick={() => setTab('commands')}>⌨ In-game ({cmdQ.data?.length ?? 0})</div>
          <div className={`tab-item ${tab === 'site' ? 'active' : ''}`} onClick={() => setTab('site')}>🌐 Site ({siteQ.data?.length ?? 0})</div>
        </div>
        <input
          className="input flex-1 max-w-md"
          placeholder={tab === 'chat' ? 'Buscar mensagem...' : 'Buscar comando...'}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {tab !== 'site' && (
          <select className="input max-w-xs" value={playerFilter} onChange={(e) => setPlayerFilter(e.target.value)}>
            <option value="">Todos players</option>
            {players.map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        )}
      </div>

      <div className="card-glow">
        <div ref={listRef} className="font-mono text-xs space-y-1 max-h-[70vh] overflow-y-auto">
          {filtered.length === 0 && <div className="italic text-liberthia-300/50 py-6 text-center">— vazio —</div>}
          {filtered.map((e: any, i: number) => (
            <div key={i} className="flex gap-3 px-2 py-1 rounded hover:bg-liberthia-700/20">
              <span className="text-liberthia-300/40 shrink-0">{new Date(e.ts).toLocaleTimeString()}</span>
              <span className="text-liberthia-300/50 text-[10px] shrink-0">{new Date(e.ts).toLocaleDateString()}</span>
              {tab === 'site' ? (
                <>
                  <span className="shrink-0 font-bold text-amber-300">🌐 {e.origin}</span>
                  <span className={`shrink-0 text-[10px] ${e.result >= 1 ? 'text-emerald-300' : 'text-red-300'}`}>
                    [r={e.result}]
                  </span>
                </>
              ) : (
                <span className={`shrink-0 font-bold ${tab === 'commands' && !e.isPlayer ? 'text-cyan-300' : 'text-liberthia-200'}`}>
                  {e.name}
                </span>
              )}
              <span className="flex-1 break-all text-liberthia-100/90">
                {tab === 'chat' ? e.message : e.command}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="text-xs text-liberthia-300/50 mt-3 text-center">
        Mostrando {filtered.length} de {data.length}.
        {wsConnected
          ? ' ⚡ WebSocket atualiza instantâneo + polling 1.5s.'
          : ' ⚠ WS offline — só polling 1.5s.'}
      </div>
    </div>
  )
}
