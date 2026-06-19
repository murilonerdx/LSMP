import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

const TYPES = [
  { key: 'talk', label: '💬 Talk', desc: 'Balão simples' },
  { key: 'comic', label: '🗯 Comic', desc: 'Estilo quadrinho' },
  { key: 'shout', label: '📢 Shout', desc: 'Grito (com !!)' },
  { key: 'thought', label: '💭 Thought', desc: 'Pensamento (italic)' },
]

export function BalloonsDirectorPage() {
  const qc = useQueryClient()
  const [target, setTarget] = useState('')
  const [text, setText] = useState('')
  const [type, setType] = useState<'talk' | 'comic' | 'shout' | 'thought'>('talk')
  const [filterPlayer, setFilterPlayer] = useState('')
  const [filterSource, setFilterSource] = useState('')
  const [selected, setSelected] = useState<Set<number>>(new Set())

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 10_000 })
  const statsQ = useQuery({ queryKey: ['balloons-stats'], queryFn: api.balloonsStats, refetchInterval: 15_000 })
  const listQ = useQuery({
    queryKey: ['balloons', filterPlayer, filterSource],
    queryFn: () => api.balloonsList({ playerUuid: filterPlayer || undefined, source: filterSource || undefined, size: 200 }),
    refetchInterval: 8_000,
  })

  const sayMut = useMutation({
    mutationFn: api.balloonsSay,
    onSuccess: () => { toast.ok('💬 dito'); setText(''); qc.invalidateQueries({ queryKey: ['balloons'] }); qc.invalidateQueries({ queryKey: ['balloons-stats'] }) },
    onError: (e: any) => toast.err(e.message),
  })
  const bulkDelMut = useMutation({
    mutationFn: api.balloonsBulkDelete,
    onSuccess: (r) => { toast.ok(`🗑 ${r.deleted} apagados`); setSelected(new Set()); qc.invalidateQueries({ queryKey: ['balloons'] }) },
    onError: (e: any) => toast.err(e.message),
  })

  const players = Array.isArray(playersQ.data) ? playersQ.data : []
  const balloons = listQ.data?.content ?? []
  const stats = statsQ.data

  function toggleSel(id: number) {
    setSelected(s => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n })
  }

  return (
    <div className="route-fade max-w-[1500px] space-y-4">
      <header>
        <h1 className="page-title">💬 Balloons Director</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Faz um player "dizer" o que tu quiser — o TalkBalloons/ComicsBubbles renderiza acima da
          cabeça dele. Tudo fica salvo no banco pra histórico.
        </p>
      </header>

      {/* Make player say something */}
      <div className="card-glow space-y-2">
        <h3 className="font-bold">🎤 Forçar fala</h3>
        <div className="grid grid-cols-1 md:grid-cols-[200px_180px_1fr_auto] gap-2">
          <select className="input" value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="">Player…</option>
            {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
          </select>
          <select className="input" value={type} onChange={(e) => setType(e.target.value as any)}>
            {TYPES.map(t => <option key={t.key} value={t.key}>{t.label}</option>)}
          </select>
          <input className="input" placeholder="Texto que ele vai 'falar'…"
            value={text} onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && target && text) sayMut.mutate({ playerName: target, text, type }) }} />
          <button className="btn" disabled={!target || !text}
            onClick={() => sayMut.mutate({ playerName: target, text, type })}>
            💬 Dizer
          </button>
        </div>
        <p className="text-[10px] text-liberthia-300/50 italic">
          {TYPES.find(t => t.key === type)?.desc}
        </p>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
          <div className="card text-center">
            <div className="text-2xl font-bold">{stats.total}</div>
            <div className="text-liberthia-300/60">Total</div>
          </div>
          <div className="card text-center">
            <div className="text-2xl font-bold text-emerald-300">{stats.captured}</div>
            <div className="text-liberthia-300/60">Capturados (chat)</div>
          </div>
          <div className="card text-center">
            <div className="text-2xl font-bold text-purple-300">{stats.admin}</div>
            <div className="text-liberthia-300/60">Forçados admin</div>
          </div>
          <div className="card text-center">
            <div className="text-2xl font-bold">{stats.topPlayers.length}</div>
            <div className="text-liberthia-300/60">Players únicos</div>
          </div>
        </div>
      )}

      {/* Top players */}
      {stats && stats.topPlayers.length > 0 && (
        <div className="card-glow">
          <h3 className="font-bold mb-2 text-sm">🏆 Top falantes</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-1.5">
            {stats.topPlayers.slice(0, 12).map(p => (
              <button key={p.playerUuid}
                onClick={() => setFilterPlayer(p.playerUuid === filterPlayer ? '' : p.playerUuid)}
                className={`rounded p-2 text-left text-xs ${
                  filterPlayer === p.playerUuid
                    ? 'bg-purple-500/30 ring-1 ring-purple-400'
                    : 'bg-liberthia-900/60 hover:bg-liberthia-900/80'
                }`}>
                <div className="font-bold truncate">{p.playerName}</div>
                <div className="text-[10px] text-liberthia-300/60">{p.count} balão{p.count === 1 ? '' : 'ões'}</div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex items-center gap-2 flex-wrap text-xs">
        <button className={`btn-ghost btn-sm ${filterSource === '' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilterSource('')}>Todos</button>
        <button className={`btn-ghost btn-sm ${filterSource === 'captured' ? 'ring-1 ring-emerald-400' : ''}`}
          onClick={() => setFilterSource('captured')}>📥 Capturados</button>
        <button className={`btn-ghost btn-sm ${filterSource === 'admin' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilterSource('admin')}>🎭 Admin</button>
        {filterPlayer && (
          <button className="btn-ghost btn-sm bg-amber-500/20" onClick={() => setFilterPlayer('')}>
            ✕ Limpar filtro player
          </button>
        )}
        {selected.size > 0 && (
          <button className="btn-danger btn-sm ml-auto"
            onClick={() => { if (confirm(`Apagar ${selected.size} balões?`)) bulkDelMut.mutate(Array.from(selected)) }}>
            🗑 Apagar {selected.size}
          </button>
        )}
      </div>

      {/* Lista */}
      <div className="space-y-1.5">
        {balloons.map(b => (
          <div key={b.id} className={`card flex items-start gap-2 ${selected.has(b.id) ? 'ring-1 ring-amber-400' : ''}`}>
            <input type="checkbox" checked={selected.has(b.id)} onChange={() => toggleSel(b.id)} className="mt-1" />
            <span className="text-base">{TYPES.find(t => t.key === b.type)?.label.split(' ')[0] ?? '💬'}</span>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-bold text-sm">{b.playerName}</span>
                <span className="text-xs italic">"{b.text}"</span>
                {b.source === 'admin' && <span className="badge badge-purple text-[9px]">admin</span>}
              </div>
              <div className="text-[10px] text-liberthia-300/40">
                {new Date(b.ts).toLocaleString()}
                {b.dimension && ` · ${b.dimension}`}
              </div>
            </div>
          </div>
        ))}
        {!listQ.isLoading && balloons.length === 0 && (
          <div className="card text-center py-12 text-liberthia-300/60">
            Sem balões ainda. Use o painel acima pra criar.
          </div>
        )}
      </div>

      <div className="card text-[11px] text-liberthia-300/60 italic">
        <strong>📥 Captura automática:</strong> requer hook <code>ServerChatEvent</code> no mod
        Liberthia que faz POST em <code>/api/balloons/capture</code> (passa playerUuid, playerName, text, ts, pos).
        Sem isso, só os criados pelo painel (source=admin) aparecem aqui.
      </div>
    </div>
  )
}
