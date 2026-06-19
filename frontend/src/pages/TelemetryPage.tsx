import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { telemetryApi, TelemetrySnapshot, TelemetryFeatures, TelemetryInference } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Telemetry Page — Sistema nervoso do servidor.
 *
 * Mostra em tempo real (refresh 5s) features extraídas + inferências de cada
 * player online. Permite admin desligar/ligar a captura global pra reduzir
 * carga no servidor MC.
 *
 * Fonte de dados: mod faz push de snapshot a cada ~10s pro backend, backend
 * cacheia em memória, esta página polla o cache.
 */

const GOAL_META: Record<string, { emoji: string; label: string; color: string }> = {
  mining: { emoji: '⛏', label: 'Mineração', color: 'amber' },
  exploring: { emoji: '🗺', label: 'Explorando', color: 'emerald' },
  fighting: { emoji: '⚔', label: 'Combate', color: 'red' },
  building: { emoji: '🏗', label: 'Construindo', color: 'purple' },
  socializing: { emoji: '💬', label: 'Socializando', color: 'blue' },
  idle: { emoji: '💤', label: 'Inativo', color: 'gray' },
  unknown: { emoji: '❓', label: 'Indefinido', color: 'gray' },
}

const MOOD_META: Record<string, { emoji: string; color: string }> = {
  focused: { emoji: '🎯', color: '#34d399' },
  frustrated: { emoji: '😤', color: '#f87171' },
  confused: { emoji: '🤔', color: '#fbbf24' },
  satisfied: { emoji: '😎', color: '#a78bfa' },
  neutral: { emoji: '😐', color: '#9ca3af' },
}

function fmtDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}m ${s % 60}s`
  const h = Math.floor(m / 60)
  return `${h}h ${m % 60}m`
}

export function TelemetryPage() {
  const qc = useQueryClient()
  const [selectedUuid, setSelectedUuid] = useState<string | null>(null)

  const statusQ = useQuery({
    queryKey: ['telemetry-status'],
    queryFn: telemetryApi.status,
    refetchInterval: 5000,
  })
  const playersQ = useQuery({
    queryKey: ['telemetry-players'],
    queryFn: telemetryApi.players,
    refetchInterval: 5000,
  })

  const toggleMut = useMutation({
    mutationFn: telemetryApi.setConfig,
    onSuccess: (r) => {
      toast.ok(`Telemetria ${r.enabled ? 'LIGADA' : 'DESLIGADA'}`)
      qc.invalidateQueries({ queryKey: ['telemetry-status'] })
      qc.invalidateQueries({ queryKey: ['telemetry-players'] })
    },
    onError: (e: any) => toast.err(e.message || 'erro'),
  })

  const players = playersQ.data?.players ?? []
  const enabled = playersQ.data?.enabled ?? true
  const selected = selectedUuid ? players.find(p => p.uuid === selectedUuid) : null

  return (
    <div className="route-fade max-w-[1600px]">
      <header className="mb-4">
        <h1 className="page-title">🧠 Sistema Nervoso — Telemetria</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Captura comportamental em tempo real: o mod observa cada player e infere o que ele tá fazendo
          (goal), como tá se sentindo (mood) e o que o servidor pode fazer pra ajudar/desafiar.
        </p>
      </header>

      {/* Status banner + toggle */}
      <div className="card-glow mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className={`w-3 h-3 rounded-full animate-pulse ${enabled ? 'bg-emerald-400' : 'bg-red-400'}`} />
          <div>
            <div className="font-bold">
              {enabled ? '✓ Telemetria ATIVA' : '✗ Telemetria DESLIGADA'}
            </div>
            {statusQ.data && (
              <div className="text-[11px] text-liberthia-300/60">
                {players.length} player(s) com dados ·
                último batch há {fmtDuration(statusQ.data.ageOfLastBatchMs)} ·
                {statusQ.data.lastBatchSize} snapshots por batch
              </div>
            )}
          </div>
        </div>
        <label className="relative inline-flex items-center cursor-pointer">
          <input
            type="checkbox"
            checked={enabled}
            disabled={toggleMut.isPending}
            onChange={(e) => toggleMut.mutate(e.target.checked)}
            className="sr-only peer"
          />
          <div className="w-14 h-7 bg-liberthia-700 rounded-full peer-checked:bg-emerald-500 transition-colors relative">
            <div className={`absolute top-0.5 left-0.5 w-6 h-6 rounded-full bg-white transition-transform ${enabled ? 'translate-x-7' : ''}`} />
          </div>
        </label>
      </div>

      {!enabled && (
        <div className="card !bg-amber-900/20 !border-amber-500/40 mb-4 text-xs">
          ⚠ Backend está descartando snapshots recebidos do mod. Pra desligar TAMBÉM no
          mod (e parar de gastar CPU/RAM no servidor MC), editar{' '}
          <code className="text-amber-300">liberthia-server.toml</code> →{' '}
          <code className="text-amber-300">[telemetry] enabled = false</code> + reiniciar o server.
        </div>
      )}

      {playersQ.isLoading && (
        <div className="text-center py-8 text-liberthia-300/60">Carregando snapshots...</div>
      )}

      {!playersQ.isLoading && players.length === 0 && (
        <div className="card text-center py-12 text-liberthia-300/50">
          <div className="text-4xl mb-2 opacity-50">🌐</div>
          <p>Sem snapshots ainda.</p>
          <p className="text-[11px] mt-2">
            Verifica se o mod tá rodando + telemetria habilitada no toml,
            e se algum player tá online no servidor MC.
          </p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Lista de players */}
        <div className="lg:col-span-2 space-y-2">
          {players.map(p => (
            <PlayerCard key={p.uuid} player={p}
              selected={p.uuid === selectedUuid}
              onClick={() => setSelectedUuid(p.uuid)} />
          ))}
        </div>

        {/* Detalhe */}
        <div className="lg:sticky lg:top-4 self-start">
          {selected ? <PlayerDetail player={selected} /> : (
            <div className="card text-center py-12 text-liberthia-300/50 text-xs">
              ← Click num player pra ver detalhes
            </div>
          )}
        </div>
      </div>

      {/* Tipos de dados — referência rápida */}
      <details className="card mt-6 text-xs">
        <summary className="cursor-pointer font-bold text-liberthia-200">
          📋 JSON disponibilizado por player
        </summary>
        <pre className="mt-2 p-3 rounded bg-liberthia-900/60 overflow-x-auto text-[10px] text-liberthia-300/80">
{`{
  "uuid": "550e8400-...",
  "name": "Steve",
  "online": true,
  "stale": false,
  "ageMs": 1234,
  "session": {
    "durationMs": 7890000,
    "idleMs": 45000,
    "dimension": "minecraft:overworld",
    "pos": { "x": 124.5, "y": 64, "z": -312.8 }
  },
  "counters": {
    "deaths": 2, "kills": 5,
    "blocksBroken": 47,
    "totalDistanceXZ": 312
  },
  "features": {
    "exploration": 0.62, "aggression": 0.4,
    "efficiency": 0.31, "confusion": 0.15,
    "frustration": 0.18, "social": 0.05,
    "risk": 0.22
  },
  "inference": {
    "goal": "mining", "goalConfidence": 0.82,
    "mood": "focused", "moodConfidence": 0.75,
    "suggestedAction": "cooldown"
  },
  "timeline": { "size": 1247, "capacity": 2048 }
}`}
        </pre>
      </details>
    </div>
  )
}

function PlayerCard({ player: p, selected, onClick }: {
  player: TelemetrySnapshot; selected: boolean; onClick: () => void
}) {
  const goal = GOAL_META[p.inference?.goal ?? 'unknown'] ?? GOAL_META.unknown
  const mood = MOOD_META[p.inference?.mood ?? 'neutral'] ?? MOOD_META.neutral
  return (
    <div onClick={onClick}
      className={`card cursor-pointer transition-all ${
        selected ? 'ring-2 ring-purple-400 bg-purple-500/10' : 'hover:bg-liberthia-500/10'
      } ${p.stale ? 'opacity-60' : ''}`}>
      <div className="flex items-center gap-3">
        <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.name || p.uuid.slice(0, 8))}/48`}
          className="w-12 h-12 rounded shrink-0" alt={p.name} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-bold">{p.name || p.uuid.slice(0, 8)}</span>
            {p.stale ? (
              <span className="text-[10px] badge badge-red">offline</span>
            ) : (
              <span className="text-[10px] badge badge-green">online</span>
            )}
            <span className="text-[10px] text-liberthia-300/60">
              {p.session?.dimension?.replace('minecraft:', '') ?? '?'}
            </span>
          </div>
          <div className="flex items-center gap-2 mt-1 text-xs">
            <span className="text-lg" style={{ color: mood.color }}>{mood.emoji}</span>
            <span className="font-mono text-[11px] text-liberthia-300/80">
              {goal.emoji} {goal.label}
            </span>
            {p.inference?.goalConfidence !== undefined && (
              <span className="text-[10px] text-liberthia-300/50">
                ({Math.round(p.inference.goalConfidence * 100)}%)
              </span>
            )}
          </div>
        </div>
        <div className="text-right text-[10px] text-liberthia-300/60 shrink-0">
          <div>{fmtDuration(p.session?.durationMs ?? 0)}</div>
          <div>{p.counters?.deaths ?? 0}💀 · {p.counters?.kills ?? 0}⚔</div>
        </div>
      </div>
      {p.features && (
        <div className="mt-3 grid grid-cols-7 gap-1">
          <FeatureBar label="Expl" value={p.features.exploration} color="#34d399" />
          <FeatureBar label="Agg" value={p.features.aggression} color="#f87171" />
          <FeatureBar label="Effc" value={p.features.efficiency} color="#fbbf24" />
          <FeatureBar label="Conf" value={p.features.confusion} color="#a78bfa" />
          <FeatureBar label="Frus" value={p.features.frustration} color="#fb923c" />
          <FeatureBar label="Soc" value={p.features.social} color="#60a5fa" />
          <FeatureBar label="Risk" value={p.features.risk} color="#ef4444" />
        </div>
      )}
    </div>
  )
}

function FeatureBar({ label, value, color }: { label: string; value: number; color: string }) {
  const pct = Math.round(value * 100)
  return (
    <div className="text-center" title={`${label}: ${pct}%`}>
      <div className="text-[9px] text-liberthia-300/60 uppercase">{label}</div>
      <div className="h-1.5 rounded bg-liberthia-900/40 overflow-hidden">
        <div className="h-full transition-all" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
      <div className="text-[9px] font-mono mt-0.5" style={{ color }}>{pct}</div>
    </div>
  )
}

function PlayerDetail({ player: p }: { player: TelemetrySnapshot }) {
  const goal = GOAL_META[p.inference?.goal ?? 'unknown'] ?? GOAL_META.unknown
  const mood = MOOD_META[p.inference?.mood ?? 'neutral'] ?? MOOD_META.neutral
  return (
    <div className="card-glow space-y-3 text-xs">
      <div className="flex items-center gap-3">
        <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.name || p.uuid.slice(0, 8))}/64`}
          className="w-16 h-16 rounded shadow-lg" alt={p.name} />
        <div className="flex-1 min-w-0">
          <h2 className="font-bold text-base gradient-text truncate">{p.name}</h2>
          <p className="text-[9px] text-liberthia-300/50 font-mono truncate">{p.uuid}</p>
          <Link to={`/profile/${p.uuid}`} className="text-[10px] text-purple-300 hover:underline">
            → ver perfil completo
          </Link>
        </div>
      </div>

      {/* Inferência destaque */}
      <div className="p-3 rounded bg-liberthia-900/40 space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-liberthia-300/60">Goal</span>
          <span className="font-bold">
            {goal.emoji} {goal.label}{' '}
            <span className="text-liberthia-300/50">
              ({Math.round((p.inference?.goalConfidence ?? 0) * 100)}%)
            </span>
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-liberthia-300/60">Mood</span>
          <span className="font-bold" style={{ color: mood.color }}>
            {mood.emoji} {p.inference?.mood ?? '—'}{' '}
            <span className="text-liberthia-300/50">
              ({Math.round((p.inference?.moodConfidence ?? 0) * 100)}%)
            </span>
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-liberthia-300/60">Ação sugerida</span>
          <span className="font-mono text-purple-300">
            {p.inference?.suggestedAction ?? '—'}
          </span>
        </div>
      </div>

      {/* Sessão */}
      <div>
        <div className="text-[10px] text-liberthia-300/50 uppercase mb-1">Sessão</div>
        <div className="grid grid-cols-2 gap-1">
          <Row label="Duração" value={fmtDuration(p.session?.durationMs ?? 0)} />
          <Row label="Inativo" value={fmtDuration(p.session?.idleMs ?? 0)} />
          <Row label="Dimensão" value={p.session?.dimension?.replace('minecraft:', '') ?? '—'} />
          <Row label="Y" value={`${Math.round(p.session?.pos?.y ?? 0)}`} />
        </div>
        <div className="text-[10px] text-liberthia-300/50 mt-1 font-mono">
          📍 {Math.round(p.session?.pos?.x ?? 0)}, {Math.round(p.session?.pos?.y ?? 0)}, {Math.round(p.session?.pos?.z ?? 0)}
        </div>
      </div>

      {/* Contadores */}
      <div>
        <div className="text-[10px] text-liberthia-300/50 uppercase mb-1">Contadores (sessão)</div>
        <div className="grid grid-cols-2 gap-1">
          <Row label="💀 Mortes" value={p.counters?.deaths ?? 0} />
          <Row label="⚔ Kills" value={p.counters?.kills ?? 0} />
          <Row label="Dano dado" value={p.counters?.damageDealt ?? 0} />
          <Row label="Dano tomado" value={p.counters?.damageTaken ?? 0} />
          <Row label="⛏ Quebrou" value={p.counters?.blocksBroken ?? 0} />
          <Row label="🧱 Colocou" value={p.counters?.blocksPlaced ?? 0} />
          <Row label="💬 Chat" value={p.counters?.chatMessages ?? 0} />
          <Row label="🎒 Inv opens" value={p.counters?.inventoryOpens ?? 0} />
          <Row label="🔨 Craft" value={p.counters?.craftCount ?? 0} />
          <Row label="🚶 Dist (m)" value={p.counters?.totalDistanceXZ ?? 0} />
        </div>
      </div>

      <div className="text-[9px] text-liberthia-300/40">
        Última atualização: {fmtDuration(p.ageMs)} atrás · timeline {p.timeline?.size ?? 0}/{p.timeline?.capacity ?? 0}
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: any }) {
  return (
    <div className="flex justify-between p-1 rounded hover:bg-liberthia-500/10">
      <span className="text-liberthia-300/60">{label}</span>
      <span className="font-mono">{value}</span>
    </div>
  )
}
