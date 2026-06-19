import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'

/**
 * Player Analytics — dashboard agregado por player com:
 *  - Chat volume (gráfico de barras por hora últimas 24h)
 *  - Comandos executados
 *  - Mortes (do localStorage do DeathLog)
 *  - Snapshots disponíveis
 *  - Top palavras mais ditas
 */

type WordFreq = { word: string; count: number }

const STOPWORDS = new Set(['o', 'a', 'os', 'as', 'um', 'uma', 'de', 'do', 'da', 'que', 'eu', 'tu', 'ele', 'ela',
  'pra', 'pro', 'com', 'em', 'no', 'na', 'e', 'é', 'um', 'um', 'mas', 'se', 'não', 'sim', 'ja', 'já', 'so', 'só',
  'the', 'is', 'a', 'an', 'and', 'or', 'but', 'i', 'you', 'he', 'she', 'it', 'to', 'of', 'in', 'on', 'at'])

export function PlayerAnalyticsPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [selected, setSelected] = useState('')
  const chatBarsRef = useRef<HTMLCanvasElement>(null)

  // Fetch chat + commands + snapshots quando seleciona player
  const chatQ = useQuery({
    queryKey: ['analytics-chat', selected],
    queryFn: () => selected ? api.chatHistory(selected, 0, 500) : Promise.resolve([]),
    enabled: !!selected,
  })
  const cmdQ = useQuery({
    queryKey: ['analytics-cmd', selected],
    queryFn: () => selected ? api.cmdHistory(selected, 0, 500) : Promise.resolve([]),
    enabled: !!selected,
  })
  const snapQ = useQuery({
    queryKey: ['analytics-snap', selected],
    queryFn: () => selected ? api.snapshotList(selected) : Promise.resolve([]),
    enabled: !!selected,
  })

  const player = players.find((p) => p.uuid === selected)
  const chats = chatQ.data ?? []
  const cmds = cmdQ.data ?? []
  const snaps = snapQ.data ?? []

  // Death count do banco (KV: death_log)
  const deathLogQ = useQuery({
    queryKey: ['death_log'],
    queryFn: () => api.kvGet<any[]>('death_log').then((r) => r.data ?? []).catch(() => []),
    refetchInterval: 10000,
  })
  const deathCount = useMemo(() => {
    return (deathLogQ.data ?? []).filter((d: any) => d.uuid === selected).length
  }, [deathLogQ.data, selected])

  // Chat por hora últimas 24h
  const hourBuckets = useMemo(() => {
    const buckets = new Array(24).fill(0)
    const cutoff = Date.now() - 24 * 3600 * 1000
    for (const c of chats) {
      if (c.ts < cutoff) continue
      const hoursAgo = Math.floor((Date.now() - c.ts) / 3600000)
      const idx = 23 - hoursAgo
      if (idx >= 0 && idx < 24) buckets[idx]++
    }
    return buckets
  }, [chats])

  // Top palavras
  const topWords = useMemo<WordFreq[]>(() => {
    const counts: Record<string, number> = {}
    for (const c of chats) {
      const words = (c.message || '').toLowerCase()
        .replace(/[^a-z0-9áéíóúâêôãõç\s]/g, ' ').split(/\s+/)
      for (const w of words) {
        if (w.length < 3 || STOPWORDS.has(w)) continue
        counts[w] = (counts[w] ?? 0) + 1
      }
    }
    return Object.entries(counts).map(([word, count]) => ({ word, count }))
      .sort((a, b) => b.count - a.count).slice(0, 15)
  }, [chats])

  // Comandos mais usados
  const topCmds = useMemo<WordFreq[]>(() => {
    const counts: Record<string, number> = {}
    for (const c of cmds) {
      const firstWord = (c.command || '').replace(/^\/+/, '').split(/\s+/)[0]
      if (!firstWord) continue
      counts[firstWord] = (counts[firstWord] ?? 0) + 1
    }
    return Object.entries(counts).map(([word, count]) => ({ word, count }))
      .sort((a, b) => b.count - a.count).slice(0, 10)
  }, [cmds])

  // Renderiza barras de chat
  useEffect(() => {
    const c = chatBarsRef.current; if (!c) return
    const ctx = c.getContext('2d'); if (!ctx) return
    const W = c.clientWidth, H = 120
    c.width = W; c.height = H
    ctx.fillStyle = '#0a0410'; ctx.fillRect(0, 0, W, H)
    const max = Math.max(1, ...hourBuckets)
    const barW = W / 24
    for (let i = 0; i < 24; i++) {
      const v = hourBuckets[i]
      const barH = (v / max) * (H - 20)
      const x = i * barW
      const y = H - barH - 4
      ctx.fillStyle = `hsl(${280 - (v / max) * 100}, 70%, ${50 + (v / max) * 20}%)`
      ctx.fillRect(x + 2, y, barW - 4, barH)
      if (v > 0) {
        ctx.fillStyle = '#fff'
        ctx.font = '9px monospace'
        ctx.textAlign = 'center'
        ctx.fillText(String(v), x + barW / 2, y - 2)
      }
    }
    // Eixo
    ctx.fillStyle = 'rgba(216,194,255,0.4)'
    ctx.font = '8px monospace'
    ctx.textAlign = 'left'
    ctx.fillText('24h atrás', 4, H - 1)
    ctx.textAlign = 'right'
    ctx.fillText('agora', W - 4, H - 1)
  }, [hourBuckets])

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📊 Player Analytics</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Dashboard agregado: chat, comandos, mortes, snapshots, top palavras.
          </p>
        </div>
        <select className="input max-w-xs" value={selected} onChange={(e) => setSelected(e.target.value)}>
          <option value="">— escolha player —</option>
          {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
        </select>
      </header>

      {!selected && (
        <div className="card text-center py-16">
          <div className="text-6xl mb-3 opacity-40">📊</div>
          <p className="text-liberthia-300/60">Escolha um player no topo pra ver as estatísticas.</p>
        </div>
      )}

      {selected && player && (
        <div className="space-y-4">
          {/* Header card */}
          <div className="card-glow flex items-center gap-3 flex-wrap">
            <img src={`https://mc-heads.net/avatar/${selected}/48`} className="rounded" />
            <div className="flex-1 min-w-0">
              <div className="font-bold text-lg">{player.name}</div>
              <div className="text-xs text-liberthia-300/60">{player.uuid}</div>
              <div className="text-xs text-liberthia-300/60 mt-0.5">
                📍 {player.position.x.toFixed(0)}, {player.position.y.toFixed(0)}, {player.position.z.toFixed(0)} · {player.dimension.replace('minecraft:', '')}
              </div>
            </div>
            <div className="flex gap-2">
              <Badge label="HP" v={`${player.health.toFixed(0)}/${player.maxHealth.toFixed(0)}`} />
              <Badge label="Food" v={`${player.food}/20`} />
              <Badge label="Level" v={`${player.level}`} />
            </div>
          </div>

          {/* Stats grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <StatCard emoji="💬" label="Mensagens" v={chats.length} sub="últimas 500" />
            <StatCard emoji="⌨" label="Comandos" v={cmds.length} sub="últimos 500" />
            <StatCard emoji="💀" label="Mortes" v={deathCount} sub="histórico local" />
            <StatCard emoji="📸" label="Snapshots" v={snaps.length} sub="backups inv" />
          </div>

          {/* Chat por hora */}
          <div className="card-glow">
            <h3 className="font-bold mb-2 flex items-center gap-2">💬 Atividade de chat (últimas 24h)</h3>
            <canvas ref={chatBarsRef} style={{ width: '100%', height: 120 }} />
            <div className="text-[10px] text-liberthia-300/50 text-center mt-1">
              Mensagens por hora · total: {hourBuckets.reduce((a, v) => a + v, 0)}
            </div>
          </div>

          {/* 2 colunas: top palavras + top comandos */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="card-glow">
              <h3 className="font-bold mb-2">📝 Top palavras (chat)</h3>
              {topWords.length === 0 ? (
                <p className="text-xs italic text-liberthia-300/50">Sem chat suficiente</p>
              ) : (
                <div className="space-y-1.5">
                  {topWords.map((w, i) => {
                    const pct = (w.count / topWords[0].count) * 100
                    return (
                      <div key={w.word} className="flex items-center gap-2 text-xs">
                        <span className="w-6 text-right text-liberthia-300/40 font-mono">#{i + 1}</span>
                        <span className="w-32 truncate">{w.word}</span>
                        <div className="flex-1 h-3 bg-liberthia-900/60 rounded overflow-hidden">
                          <div className="h-full bg-gradient-to-r from-liberthia-400 to-liberthia-600" style={{ width: `${pct}%` }} />
                        </div>
                        <span className="w-8 text-right font-mono text-liberthia-300/70">{w.count}</span>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>

            <div className="card-glow">
              <h3 className="font-bold mb-2">⌨ Top comandos</h3>
              {topCmds.length === 0 ? (
                <p className="text-xs italic text-liberthia-300/50">Sem comandos suficientes</p>
              ) : (
                <div className="space-y-1.5">
                  {topCmds.map((c, i) => {
                    const pct = (c.count / topCmds[0].count) * 100
                    return (
                      <div key={c.word} className="flex items-center gap-2 text-xs">
                        <span className="w-6 text-right text-liberthia-300/40 font-mono">#{i + 1}</span>
                        <span className="w-32 truncate font-mono">/{c.word}</span>
                        <div className="flex-1 h-3 bg-liberthia-900/60 rounded overflow-hidden">
                          <div className="h-full bg-gradient-to-r from-cyan-400 to-cyan-600" style={{ width: `${pct}%` }} />
                        </div>
                        <span className="w-8 text-right font-mono text-liberthia-300/70">{c.count}</span>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Últimas mensagens */}
          <div className="card-glow">
            <h3 className="font-bold mb-2">💬 Últimas mensagens</h3>
            <div className="space-y-1 max-h-60 overflow-y-auto text-xs font-mono">
              {chats.slice(0, 20).map((c, i) => (
                <div key={i} className="flex gap-2">
                  <span className="text-liberthia-300/40 shrink-0">{new Date(c.ts).toLocaleTimeString()}</span>
                  <span className="break-words">{c.message}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ emoji, label, v, sub }: { emoji: string; label: string; v: any; sub?: string }) {
  return (
    <div className="card-glow text-center">
      <div className="text-3xl mb-1">{emoji}</div>
      <div className="text-2xl font-black gradient-text">{v}</div>
      <div className="text-xs text-liberthia-300/70">{label}</div>
      {sub && <div className="text-[10px] text-liberthia-300/40">{sub}</div>}
    </div>
  )
}

function Badge({ label, v }: { label: string; v: string }) {
  return (
    <div className="px-3 py-1 rounded-lg bg-liberthia-900/50 border border-liberthia-500/30 text-xs">
      <div className="label">{label}</div>
      <div className="font-mono font-bold">{v}</div>
    </div>
  )
}
