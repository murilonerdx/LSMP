import { useMemo, useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Catálogo de estruturas dos mods. Click → /locate via runCommand. Output
 * do /locate aparece no console/chat do server admin (não temos return
 * estruturado), então mostramos toast confirmando + nota.
 */
export function StructuresPage() {
  const [search, setSearch] = useState('')
  const [origin, setOrigin] = useState('')
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({})

  const catalogQ = useQuery({ queryKey: ['structures-catalog'], queryFn: api.structuresCatalog })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 10_000 })
  const players = Array.isArray(playersQ.data) ? playersQ.data : []

  const locateMut = useMutation({
    mutationFn: ({ id, origin }: { id: string; origin?: string }) =>
      api.structuresLocate(id, origin),
    onSuccess: (r) => toast.ok('📍 ' + (r.note ?? r.cmd ?? 'comando enviado')),
    onError: (e: any) => toast.err(e.message),
  })

  const groups = catalogQ.data?.groups ?? []

  const filtered = useMemo(() => {
    if (!search.trim()) return groups
    const q = search.trim().toLowerCase()
    return groups.map(g => ({
      ...g,
      structures: g.structures.filter(s => s.toLowerCase().includes(q)),
    })).filter(g => g.structures.length > 0)
  }, [groups, search])

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🗺 Structures Catalog</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            {catalogQ.data?.total ?? '?'} estruturas dos mods catalogadas. Click no nome dispara <code>/locate structure</code> —
            output vai pro chat do executor.
          </p>
        </div>
        <div className="flex gap-2 items-center text-xs">
          <label>Origem do locate:</label>
          <select className="input" value={origin} onChange={(e) => setOrigin(e.target.value)}>
            <option value="">Spawn (0 0 0)</option>
            {players.map(p => <option key={p.uuid} value={p.name}>📍 {p.name}</option>)}
          </select>
        </div>
      </header>

      <input className="input mb-4" placeholder="Filtrar (ex: village, twilight, naga)…"
        value={search} onChange={(e) => setSearch(e.target.value)} />

      <div className="space-y-3">
        {filtered.map(g => (
          <div key={g.name} className="card-glow">
            <button onClick={() => setCollapsed(c => ({ ...c, [g.name]: !c[g.name] }))}
              className="flex items-center gap-2 w-full text-left">
              <span className="text-2xl">{g.emoji}</span>
              <span className="font-bold text-lg">{g.name}</span>
              <span className="badge ml-auto">{g.structures.length}</span>
              <span className="text-xs">{collapsed[g.name] ? '▶' : '▼'}</span>
            </button>
            {!collapsed[g.name] && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-1.5 mt-2">
                {g.structures.map(s => (
                  <button key={s}
                    onClick={() => locateMut.mutate({ id: s, origin })}
                    className="text-left rounded bg-liberthia-900/60 hover:bg-purple-500/20 p-2 text-xs">
                    <div className="font-bold truncate">{s.split(':')[1] ?? s}</div>
                    <code className="text-[10px] text-liberthia-300/40">{s}</code>
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
