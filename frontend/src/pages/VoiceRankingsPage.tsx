import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, VoicePlayerSummary } from '../lib/api'

/**
 * Ranking dos players que mais falaram no servidor. Reusa /api/voice/library/players
 * que já tem agregação por player. Permite trocar critério de ordenação.
 */

type SortKey = 'duration' | 'clips' | 'bytes' | 'recent'

function fmtMs(ms: number): string {
  if (ms <= 0) return '0s'
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}h ${m}m`
  if (m > 0) return `${m}m ${sec}s`
  return `${sec}s`
}
function fmtBytes(b: number): string {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  if (b < 1024 * 1024 * 1024) return `${(b / (1024 * 1024)).toFixed(1)}MB`
  return `${(b / (1024 * 1024 * 1024)).toFixed(2)}GB`
}

export function VoiceRankingsPage() {
  const [sortKey, setSortKey] = useState<SortKey>('duration')
  const q = useQuery({
    queryKey: ['voice-rankings'],
    queryFn: api.voiceLibraryPlayers,
    refetchInterval: 30_000,
  })

  const players = useMemo<VoicePlayerSummary[]>(() => {
    const arr = Array.isArray(q.data?.players) ? q.data!.players.slice() : []
    arr.sort((a, b) => {
      switch (sortKey) {
        case 'clips': return b.clipCount - a.clipCount
        case 'bytes': return b.totalBytes - a.totalBytes
        case 'recent': return b.lastClipTs - a.lastClipTs
        default: return b.totalDurationMs - a.totalDurationMs
      }
    })
    return arr
  }, [q.data, sortKey])

  const top = players[0]
  const totalDur = players.reduce((acc, p) => acc + p.totalDurationMs, 0)
  const totalClips = players.reduce((acc, p) => acc + p.clipCount, 0)
  const totalBytes = players.reduce((acc, p) => acc + p.totalBytes, 0)

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🏆 Voice Rankings</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Ranking dos players que mais falaram. Atualiza a cada 30s.
          </p>
        </div>
        <div className="flex gap-2 text-xs items-center">
          <span className="badge badge-purple">{players.length} players</span>
          <span className="badge badge-purple">{fmtMs(totalDur)} total</span>
          <span className="badge badge-purple">{totalClips} clipes</span>
          <span className="badge badge-purple">{fmtBytes(totalBytes)}</span>
        </div>
      </header>

      {/* Sort selector */}
      <div className="flex gap-2 text-xs mb-4 flex-wrap">
        <button className={`btn-ghost btn-sm ${sortKey === 'duration' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setSortKey('duration')}>⏱ Tempo falado</button>
        <button className={`btn-ghost btn-sm ${sortKey === 'clips' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setSortKey('clips')}>📁 Qtd de clipes</button>
        <button className={`btn-ghost btn-sm ${sortKey === 'bytes' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setSortKey('bytes')}>📦 Bytes</button>
        <button className={`btn-ghost btn-sm ${sortKey === 'recent' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setSortKey('recent')}>🕒 Mais recentes</button>
      </div>

      {/* Pódio top 3 */}
      {top && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-6">
          {players.slice(0, 3).map((p, i) => {
            const medal = i === 0 ? '🥇' : i === 1 ? '🥈' : '🥉'
            const bg = i === 0 ? 'border-yellow-400/60 bg-yellow-500/10'
              : i === 1 ? 'border-zinc-300/40 bg-zinc-500/10'
              : 'border-orange-400/40 bg-orange-500/10'
            return (
              <div key={p.playerUuid} className={`card-glow border-2 ${bg} text-center`}>
                <div className="text-4xl mb-2">{medal}</div>
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.playerName)}/96`}
                  className="rounded mx-auto" />
                <div className="font-bold text-xl mt-2">{p.playerName}</div>
                <div className="text-2xl text-purple-300 mt-2 font-mono">
                  {sortKey === 'duration' ? fmtMs(p.totalDurationMs) :
                   sortKey === 'clips' ? `${p.clipCount} clipes` :
                   sortKey === 'bytes' ? fmtBytes(p.totalBytes) :
                   new Date(p.lastClipTs).toLocaleDateString()}
                </div>
                <div className="text-[11px] text-liberthia-300/60 mt-2 space-y-0.5">
                  <div>{fmtMs(p.totalDurationMs)} · {p.clipCount} clipes · {fmtBytes(p.totalBytes)}</div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Lista 4+ */}
      <div className="space-y-2">
        {players.slice(3).map((p, i) => {
          const rank = i + 4
          const score = sortKey === 'duration' ? p.totalDurationMs :
                        sortKey === 'clips' ? p.clipCount :
                        sortKey === 'bytes' ? p.totalBytes : p.lastClipTs
          const topScore = sortKey === 'duration' ? (top?.totalDurationMs ?? 1) :
                           sortKey === 'clips' ? (top?.clipCount ?? 1) :
                           sortKey === 'bytes' ? (top?.totalBytes ?? 1) : Date.now()
          const pct = topScore > 0 ? (score / topScore) * 100 : 0
          return (
            <div key={p.playerUuid} className="card-glow flex items-center gap-3">
              <div className="text-xl font-bold w-10 text-center text-liberthia-300/60">#{rank}</div>
              <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.playerName)}/40`}
                className="rounded" />
              <div className="flex-1 min-w-0">
                <div className="font-bold truncate">{p.playerName}</div>
                <div className="h-1.5 bg-liberthia-900/70 rounded mt-1 overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-purple-500 to-amber-400" style={{ width: pct + '%' }} />
                </div>
              </div>
              <div className="text-right text-xs">
                <div className="font-mono text-purple-300">
                  {sortKey === 'duration' ? fmtMs(p.totalDurationMs) :
                   sortKey === 'clips' ? `${p.clipCount}` :
                   sortKey === 'bytes' ? fmtBytes(p.totalBytes) :
                   new Date(p.lastClipTs).toLocaleDateString()}
                </div>
                <div className="text-[10px] text-liberthia-300/50">
                  {p.clipCount} clipes · {fmtBytes(p.totalBytes)}
                </div>
              </div>
            </div>
          )
        })}
        {!q.isLoading && players.length === 0 && (
          <div className="card text-center py-12 text-liberthia-300/60">
            Nenhum áudio capturado ainda. Aguardando players falarem no voice chat.
          </div>
        )}
      </div>
    </div>
  )
}
