import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useEvents } from '../store/events'

/**
 * Activity Timeline — feed unificado e cronológico de TUDO que acontece:
 *  - chat, comandos, deaths, login/logout
 *  - filtros por tipo + player + busca textual
 *  - exporta JSON
 *  - auto-scroll opcional
 */

type Event = { ts: number; kind: 'chat' | 'cmd' | 'death' | 'login' | 'logout' | 'event'; player?: string; text: string }

const KIND_META: Record<string, { emoji: string; color: string; label: string }> = {
  chat: { emoji: '💬', color: 'badge-purple', label: 'Chat' },
  cmd: { emoji: '⌨', color: 'badge-cyan', label: 'Comando' },
  death: { emoji: '💀', color: 'badge-red', label: 'Morte' },
  login: { emoji: '➡', color: 'badge-green', label: 'Login' },
  logout: { emoji: '⬅', color: 'badge-red', label: 'Logout' },
  event: { emoji: '⚡', color: 'badge-cyan', label: 'Evento' },
}

export function ActivityTimelinePage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const liveEvents = useEvents((s) => s.events)
  const [historyEvents, setHistoryEvents] = useState<Event[]>([])
  const [playerFilter, setPlayerFilter] = useState('')
  const [kindFilters, setKindFilters] = useState<Record<string, boolean>>({
    chat: true, cmd: true, death: true, login: true, logout: true, event: true,
  })
  const [search, setSearch] = useState('')
  const [autoScroll, setAutoScroll] = useState(true)
  const scrollRef = useRef<HTMLDivElement>(null)

  // Fetch history snapshots
  useEffect(() => {
    let active = true
    Promise.all([
      api.chatHistory('', 0, 200),
      api.cmdHistory('', 0, 200),
      api.kvGet<any[]>('death_log').then((r) => r.data ?? []).catch(() => [] as any[]),
    ]).then(([chats, cmds, deaths]) => {
      if (!active) return
      const merged: Event[] = []
      for (const c of chats) merged.push({ ts: c.ts, kind: 'chat', player: c.name, text: c.message })
      for (const c of cmds) merged.push({ ts: c.ts, kind: 'cmd', player: c.name, text: `/${c.command.replace(/^\//, '')}` })
      for (const d of deaths) merged.push({ ts: d.ts, kind: 'death', player: d.name, text: d.cause ?? d.source ?? 'morreu' })
      setHistoryEvents(merged.sort((a, b) => a.ts - b.ts))
    })
    return () => { active = false }
  }, [])

  // Combina live + history
  const merged = useMemo(() => {
    const live: Event[] = liveEvents.map((e) => {
      const t = e.type
      const d = e.data ?? {}
      const ts = e.ts ?? Date.now()
      const player = d.player ?? d.playerName ?? d.name
      if (t === 'chat') return { ts, kind: 'chat' as const, player, text: d.message ?? '' }
      if (t === 'command') return { ts, kind: 'cmd' as const, player, text: `/${(d.command ?? '').replace(/^\//, '')}` }
      if (t === 'death') return { ts, kind: 'death' as const, player, text: d.cause ?? 'morreu' }
      if (t === 'login') return { ts, kind: 'login' as const, player, text: 'entrou no servidor' }
      if (t === 'logout') return { ts, kind: 'logout' as const, player, text: 'saiu do servidor' }
      return { ts, kind: 'event' as const, player, text: `${t}: ${JSON.stringify(d).slice(0, 120)}` }
    })
    const all = [...historyEvents, ...live]
    // dedup grosso por ts+player+text
    const seen = new Set<string>()
    const out: Event[] = []
    for (const e of all) {
      const k = `${e.ts}|${e.player}|${e.kind}|${e.text}`
      if (seen.has(k)) continue
      seen.add(k)
      out.push(e)
    }
    return out.sort((a, b) => a.ts - b.ts)
  }, [historyEvents, liveEvents])

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim()
    return merged.filter((e) => {
      if (!kindFilters[e.kind]) return false
      if (playerFilter && e.player !== playerFilter) return false
      if (q && !(e.text.toLowerCase().includes(q) || (e.player ?? '').toLowerCase().includes(q))) return false
      return true
    })
  }, [merged, kindFilters, playerFilter, search])

  // Auto scroll quando chega evento novo
  useEffect(() => {
    if (autoScroll && scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [filtered.length, autoScroll])

  function exportJSON() {
    const blob = new Blob([JSON.stringify(filtered, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `liberthia-timeline-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  // Day grouping
  const grouped = useMemo(() => {
    const map = new Map<string, Event[]>()
    for (const e of filtered) {
      const d = new Date(e.ts).toLocaleDateString()
      if (!map.has(d)) map.set(d, [])
      map.get(d)!.push(e)
    }
    return Array.from(map.entries())
  }, [filtered])

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4">
        <h1 className="page-title">📅 Activity Timeline</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Feed unificado: chat + comandos + mortes + login/logout, agrupado por dia.
        </p>
      </header>

      {/* Controls */}
      <div className="card-glow mb-3">
        <div className="grid grid-cols-1 md:grid-cols-[1fr_1fr_auto] gap-2 mb-3">
          <select className="input text-xs" value={playerFilter} onChange={(e) => setPlayerFilter(e.target.value)}>
            <option value="">— todos players —</option>
            {players.map((p) => <option key={p.uuid} value={p.name}>{p.name}</option>)}
          </select>
          <input className="input text-xs" placeholder="🔍 buscar texto..." value={search} onChange={(e) => setSearch(e.target.value)} />
          <div className="flex gap-2">
            <label className="flex items-center gap-1 text-xs px-2">
              <input type="checkbox" checked={autoScroll} onChange={(e) => setAutoScroll(e.target.checked)} /> Auto-scroll
            </label>
            <button className="btn-ghost btn-sm" onClick={exportJSON}>📥 JSON</button>
          </div>
        </div>
        <div className="flex flex-wrap gap-1">
          {Object.entries(KIND_META).map(([k, m]) => (
            <button key={k}
              className={kindFilters[k] ? `badge ${m.color}` : 'badge badge-red opacity-30'}
              onClick={() => setKindFilters({ ...kindFilters, [k]: !kindFilters[k] })}>
              {m.emoji} {m.label}
            </button>
          ))}
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 md:grid-cols-6 gap-2 mb-3">
        {Object.entries(KIND_META).map(([k, m]) => {
          const c = filtered.filter((e) => e.kind === k).length
          return (
            <div key={k} className="card text-center py-2">
              <div className="text-lg">{m.emoji}</div>
              <div className="font-mono font-bold text-lg">{c}</div>
              <div className="text-[10px] text-liberthia-300/60">{m.label}</div>
            </div>
          )
        })}
      </div>

      {/* Timeline */}
      <div ref={scrollRef} className="card-glow max-h-[60vh] overflow-y-auto">
        {grouped.length === 0 && (
          <div className="text-center py-12 text-liberthia-300/50 italic">Sem eventos com esses filtros</div>
        )}
        {grouped.map(([day, evs]) => (
          <div key={day} className="mb-4">
            <div className="sticky top-0 bg-liberthia-900/80 backdrop-blur-sm border-b border-liberthia-500/20 py-1 px-2 mb-2 -mx-3 z-10">
              <span className="font-bold text-sm">📅 {day}</span>
              <span className="text-[10px] text-liberthia-300/50 ml-2">{evs.length} eventos</span>
            </div>
            <div className="space-y-1 px-1">
              {evs.map((e, i) => {
                const m = KIND_META[e.kind]
                return (
                  <div key={i} className="flex gap-2 items-start text-xs hover:bg-liberthia-800/30 rounded px-2 py-1">
                    <span className="text-[10px] text-liberthia-300/40 shrink-0 font-mono w-12 pt-0.5">
                      {new Date(e.ts).toLocaleTimeString()}
                    </span>
                    <span className="shrink-0 pt-0.5">{m.emoji}</span>
                    {e.player && <span className="font-bold text-liberthia-300 shrink-0">{e.player}</span>}
                    <span className="break-words flex-1">{e.text}</span>
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
