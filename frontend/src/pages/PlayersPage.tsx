import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { api, Player } from '../lib/api'
import { PageHeader } from '../components/PageHeader'

export function PlayersPage() {
  const q = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 3000 })
  const [search, setSearch] = useState('')

  const players = (q.data ?? []).filter(p => p.name.toLowerCase().includes(search.toLowerCase()))

  return (
    <div className="space-y-6">
      <PageHeader title="Players" subtitle={`${q.data?.length ?? 0} jogadores online`} icon="👥">
        <input
          className="input max-w-xs"
          placeholder="🔍 Pesquisar..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
      </PageHeader>

      {q.isLoading ? (
        <div className="card text-center py-12">Carregando...</div>
      ) : (q.data ?? []).length === 0 ? (
        <div className="card text-center py-12 text-liberthia-300/60 italic">
          Nenhum player conectado no servidor agora.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {players.map(p => <PlayerCard key={p.uuid} p={p} />)}
        </div>
      )}
    </div>
  )
}

function PlayerCard({ p }: { p: Player }) {
  const hpPct = (p.health / p.maxHealth) * 100
  const hpColor = hpPct < 30 ? 'bg-red-500' : hpPct < 60 ? 'bg-yellow-500' : 'bg-emerald-500'
  return (
    <Link to={`/profile/${p.uuid}`} className="card-glow hover:border-liberthia-400/60 transition group">
      <div className="flex items-start gap-3">
        <img
          src={`https://mc-heads.net/avatar/${p.uuid}/64`}
          alt={p.name}
          className="w-14 h-14 rounded-lg shadow-lg ring-1 ring-liberthia-500/30 group-hover:ring-liberthia-400 transition"
          onError={(e) => (e.currentTarget.style.display = 'none')}
        />
        <div className="flex-1 min-w-0">
          <h3 className="font-bold text-lg truncate group-hover:gradient-text transition">{p.name}</h3>
          <div className="text-xs text-liberthia-300/60 truncate font-mono">
            {p.dimension.replace('minecraft:', '')}
          </div>
          <div className="mt-2 flex gap-2 flex-wrap">
            <span className="badge badge-purple">⭐ Lvl {p.level}</span>
            <span className="badge badge-yellow">{p.gameMode}</span>
          </div>
        </div>
      </div>

      <div className="mt-3 space-y-1.5">
        <Bar label="❤️ HP" value={p.health} max={p.maxHealth} color={hpColor} />
        <Bar label="🍗 Food" value={p.food} max={20} color="bg-orange-500" />
      </div>

      <div className="mt-3 grid grid-cols-3 gap-1 text-center text-xs text-liberthia-300/60">
        <div>X<div className="font-mono text-liberthia-300">{p.position.x.toFixed(0)}</div></div>
        <div>Y<div className="font-mono text-liberthia-300">{p.position.y.toFixed(0)}</div></div>
        <div>Z<div className="font-mono text-liberthia-300">{p.position.z.toFixed(0)}</div></div>
      </div>
    </Link>
  )
}

function Bar({ label, value, max, color }: { label: string; value: number; max: number; color: string }) {
  const pct = Math.min(100, (value / max) * 100)
  return (
    <div>
      <div className="flex justify-between text-[10px] text-liberthia-300/70 mb-0.5">
        <span>{label}</span>
        <span className="font-mono">{value.toFixed(0)}/{max.toFixed(0)}</span>
      </div>
      <div className="h-1.5 rounded-full bg-black/40 overflow-hidden">
        <div className={`h-full ${color} transition-all`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}
