import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, PlayerVisitDto } from '../lib/api'

/**
 * Player Visits — quem entrou no servidor e quantas vezes/horas.
 *
 * Dados vêm de snapshots HORÁRIOS (tabela player_snapshots), então cada
 * "snapshot" ≈ 1h de tempo online. Margem de erro de ±30min por sessão.
 *
 * Tiers (categorias visuais):
 *  - rare      → 1-3 horas total (visitou poucas vezes)
 *  - casual    → 4-12 horas
 *  - regular   → 13-50 horas
 *  - veteran   → 50h+
 */

type SortMode = 'rare' | 'active' | 'recent' | 'inactive' | 'newest'

const SORT_LABELS: Record<SortMode, string> = {
  rare: '🌒 Mais raros (poucas visitas)',
  active: '🔥 Mais ativos',
  recent: '🕒 Vistos recentemente',
  inactive: '💤 Sumidos há mais tempo',
  newest: '✨ Novatos (primeira visita recente)',
}

const TIER_COLORS: Record<string, string> = {
  rare: 'bg-amber-500/30 text-amber-100 border-amber-400/40',
  casual: 'bg-blue-500/30 text-blue-100 border-blue-400/40',
  regular: 'bg-purple-500/30 text-purple-100 border-purple-400/40',
  veteran: 'bg-emerald-500/30 text-emerald-100 border-emerald-400/40',
}
const TIER_LABELS: Record<string, string> = {
  rare: '🌒 Raro',
  casual: '👤 Casual',
  regular: '👥 Regular',
  veteran: '⭐ Veterano',
}

function fmtDate(ts: number): string {
  return new Date(ts).toLocaleDateString('pt-BR')
}
function fmtDays(days: number): string {
  if (days === 0) return 'hoje'
  if (days === 1) return 'ontem'
  if (days < 7) return `${days}d atrás`
  if (days < 30) return `${Math.floor(days / 7)}sem atrás`
  if (days < 365) return `${Math.floor(days / 30)}m atrás`
  return `${Math.floor(days / 365)}a atrás`
}

export function PlayerVisitsPage() {
  const [sort, setSort] = useState<SortMode>('rare')
  const [search, setSearch] = useState('')
  const [filterTier, setFilterTier] = useState<'all' | 'rare' | 'casual' | 'regular' | 'veteran'>('all')

  const visitsQ = useQuery({
    queryKey: ['player-visits', sort],
    queryFn: () => api.playerVisitsList(sort, 500),
    refetchInterval: 60_000,
  })

  const allPlayers = visitsQ.data?.players ?? []
  const stats = visitsQ.data?.stats ?? { rare: 0, casual: 0, regular: 0, veteran: 0 }

  const filtered = allPlayers
    .filter(p => filterTier === 'all' || p.tier === filterTier)
    .filter(p => !search.trim() || p.name?.toLowerCase().includes(search.toLowerCase()))

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4">
        <h1 className="page-title">👥 Histórico de Visitas</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Quem entrou no servidor + quanto tempo ficou. Calculado a partir dos snapshots horários
          (margem ±30min por sessão). Cada hora online = 1 ponto no contador.
        </p>
      </header>

      {/* ============ Stats por tier ============ */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <TierCard tier="rare" count={stats.rare}
          desc="1-3h total — visitaram poucas vezes" onClick={() => setFilterTier('rare')}
          active={filterTier === 'rare'} />
        <TierCard tier="casual" count={stats.casual}
          desc="4-12h — passam pra brincar" onClick={() => setFilterTier('casual')}
          active={filterTier === 'casual'} />
        <TierCard tier="regular" count={stats.regular}
          desc="13-50h — players assíduos" onClick={() => setFilterTier('regular')}
          active={filterTier === 'regular'} />
        <TierCard tier="veteran" count={stats.veteran}
          desc="50h+ — moram no servidor" onClick={() => setFilterTier('veteran')}
          active={filterTier === 'veteran'} />
      </div>

      {/* ============ Filtros ============ */}
      <div className="flex gap-2 items-center mb-4 flex-wrap">
        <label className="text-xs text-liberthia-300/70 font-bold">Ordenar:</label>
        <select className="input" value={sort} onChange={(e) => setSort(e.target.value as SortMode)}>
          {Object.entries(SORT_LABELS).map(([k, v]) =>
            <option key={k} value={k}>{v}</option>)}
        </select>

        <input type="text" placeholder="🔍 Buscar player por nome…"
          value={search} onChange={(e) => setSearch(e.target.value)}
          className="input flex-1 min-w-[200px]" />

        {filterTier !== 'all' && (
          <button className="btn-ghost btn-sm" onClick={() => setFilterTier('all')}>
            ✕ Tier: {TIER_LABELS[filterTier]}
          </button>
        )}

        {visitsQ.data && (
          <span className="badge badge-purple">
            {filtered.length} de {visitsQ.data.count} player{visitsQ.data.count === 1 ? '' : 's'}
          </span>
        )}
      </div>

      {/* ============ Lista ============ */}
      {visitsQ.isLoading && <div className="card text-center py-12">Carregando…</div>}

      {!visitsQ.isLoading && filtered.length === 0 && (
        <div className="card text-center py-12 text-liberthia-300/60">
          <div className="text-4xl mb-2 opacity-50">👤</div>
          <p>Nenhum player encontrado com esse filtro.</p>
          <p className="text-[11px] mt-2 italic">
            Snapshots começam após o backend rodar 1h. Aguarde se acabou de deployar.
          </p>
        </div>
      )}

      <div className="space-y-2">
        {filtered.map(p => <PlayerRow key={p.uuid} p={p} />)}
      </div>
    </div>
  )
}

function TierCard({ tier, count, desc, onClick, active }: {
  tier: string; count: number; desc: string; onClick: () => void; active: boolean
}) {
  return (
    <button onClick={onClick}
      className={`card text-left border ${TIER_COLORS[tier]} hover:scale-[1.02] transition cursor-pointer
        ${active ? 'ring-2 ring-white/50' : ''}`}>
      <div className="text-[10px] uppercase tracking-wider opacity-70">{TIER_LABELS[tier]}</div>
      <div className="text-3xl font-bold mt-1">{count}</div>
      <div className="text-[10px] mt-1 opacity-70">{desc}</div>
    </button>
  )
}

function PlayerRow({ p }: { p: PlayerVisitDto }) {
  return (
    <div className={`card flex items-center gap-3 p-3 border-l-4 ${
      p.tier === 'rare' ? 'border-l-amber-400' :
      p.tier === 'casual' ? 'border-l-blue-400' :
      p.tier === 'regular' ? 'border-l-purple-400' :
      'border-l-emerald-400'
    }`}>
      <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.name || '?')}/40`}
        className="rounded shrink-0" />
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="font-bold truncate">{p.name}</span>
          <span className={`text-[10px] px-1.5 py-0.5 rounded ${TIER_COLORS[p.tier]}`}>
            {TIER_LABELS[p.tier]}
          </span>
        </div>
        <div className="text-[10px] text-liberthia-300/50 font-mono truncate">{p.uuid}</div>
      </div>

      <Stat label="Horas online" value={`~${p.total_snapshots}h`}
        big={p.tier === 'veteran' || p.tier === 'regular'} />
      <Stat label="Dias distintos" value={p.distinct_days} />
      <Stat label="Sessões" value={p.estimated_sessions} />
      <Stat label="Primeira visita" value={fmtDate(p.first_seen_ts)} small />
      <Stat label="Última visita" value={fmtDays(p.days_since_last_seen)}
        warning={p.days_since_last_seen > 14} small />
    </div>
  )
}

function Stat({ label, value, big, small, warning }: {
  label: string; value: string | number; big?: boolean; small?: boolean; warning?: boolean
}) {
  return (
    <div className="text-center min-w-[80px]">
      <div className="text-[9px] text-liberthia-300/50 uppercase tracking-wider">{label}</div>
      <div className={`font-bold ${big ? 'text-amber-300 text-lg' : small ? 'text-[11px]' : 'text-sm'}
        ${warning ? 'text-amber-400' : ''}`}>
        {value}
      </div>
    </div>
  )
}
