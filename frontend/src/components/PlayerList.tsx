import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, Player } from '../lib/api'

export function PlayerList() {
  const q = useQuery({ queryKey: ['players'], queryFn: api.players })

  if (q.isLoading) return <div className="text-liberthia-300">Carregando players...</div>
  if (q.error) return <div className="text-red-400">Erro: {(q.error as Error).message}</div>

  const players = q.data ?? []

  return (
    <div className="card">
      <h2 className="text-lg font-bold mb-3">👥 Players Online ({players.length})</h2>
      {players.length === 0 ? (
        <p className="text-liberthia-300/60 italic">Nenhum player conectado</p>
      ) : (
        <div className="space-y-2">
          {players.map((p) => <PlayerRow key={p.uuid} p={p} />)}
        </div>
      )}
    </div>
  )
}

function PlayerRow({ p }: { p: Player }) {
  const hpColor = p.health < p.maxHealth * 0.3 ? 'text-red-400'
                : p.health < p.maxHealth * 0.6 ? 'text-yellow-400' : 'text-green-400'
  return (
    <Link
      to={`/player/${p.uuid}`}
      className="flex items-center gap-3 p-2 rounded hover:bg-liberthia-700 transition"
    >
      <img
        src={`https://mc-heads.net/avatar/${p.uuid}/32`}
        alt={p.name}
        className="w-8 h-8 rounded"
        onError={(e) => (e.currentTarget.style.display = 'none')}
      />
      <div className="flex-1 min-w-0">
        <div className="font-semibold truncate">{p.name}</div>
        <div className="text-xs text-liberthia-300/60 truncate">
          {p.dimension.replace('minecraft:', '')} · ({p.position.x.toFixed(0)}, {p.position.y.toFixed(0)}, {p.position.z.toFixed(0)})
        </div>
      </div>
      <div className={`text-sm font-mono ${hpColor}`}>
        ❤ {p.health.toFixed(0)}/{p.maxHealth.toFixed(0)}
      </div>
      <div className="text-xs text-liberthia-300/80">L{p.level}</div>
    </Link>
  )
}
