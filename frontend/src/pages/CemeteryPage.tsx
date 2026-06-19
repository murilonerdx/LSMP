import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useEvents } from '../store/events'
import { api } from '../lib/api'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Memorial = {
  id: number; playerName: string; playerUuid: string;
  causeOfDeath: string; epitaph: string;
  dimension: string; x: number; y: number; z: number;
  permanent: boolean; reactions: number; diedAt: string;
}

/** Cemitério — lápides automáticas, separadas dos Memoriais físicos in-world. */
export function CemeteryPage() {
  const [filter, setFilter] = useState<'all' | 'perma' | 'top'>('all')
  const [editing, setEditing] = useState<Memorial | null>(null)
  const subscribe = useEvents((s) => s.subscribe)
  const qc = useQueryClient()
  const [highlight, setHighlight] = useState<number | null>(null)

  useEffect(() => subscribe((ev) => {
    if (ev.type !== 'memorial_created') return
    setHighlight(ev.data?.id ?? null)
    qc.invalidateQueries({ queryKey: ['cemetery'] })
    setTimeout(() => setHighlight(null), 3000)
  }), [subscribe])

  const q = useQuery({
    queryKey: ['cemetery', filter],
    queryFn: async () => {
      let url = '/api/memorials'
      if (filter === 'perma') url += '?permanent=true'
      else if (filter === 'top') url += '?sort=top'
      return (await fetch(url, { headers: headers() })).json()
    },
  })

  const react = useMutation({
    mutationFn: async (id: number) => fetch(`/api/memorials/${id}/react`, { method: 'POST', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cemetery'] }),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/memorials/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cemetery'] }),
  })

  const saveEpitaph = useMutation({
    mutationFn: async ({ id, epitaph }: { id: number; epitaph: string }) =>
      fetch(`/api/memorials/${id}/epitaph`, { method: 'POST', headers: headers(), body: JSON.stringify({ epitaph }) }),
    onSuccess: () => { setEditing(null); qc.invalidateQueries({ queryKey: ['cemetery'] }) },
  })

  const visit = useMutation({
    mutationFn: async ({ id, playerUuid }: { id: number; playerUuid: string }) =>
      fetch(`/api/memorials/${id}/visit`, { method: 'POST', headers: headers(), body: JSON.stringify({ playerUuid }) }),
    onSuccess: () => alert('🌀 Teleportado!'),
  })

  const materialize = useMutation({
    mutationFn: async (id: number) =>
      (await fetch(`/api/memorials/${id}/materialize`, { method: 'POST', headers: headers() })).json(),
    onSuccess: (data) => alert('🪦 Lápide construída em ' + data.at),
  })

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = Array.isArray(playersQ.data) ? playersQ.data : []
  const [tpPicker, setTpPicker] = useState<number | null>(null)

  const items: Memorial[] = Array.isArray(q.data?.content) ? q.data.content : []

  return (
    <div className="p-6 space-y-6 text-white">
      <div className="flex justify-between">
        <h1 className="text-3xl font-bold">⚰️ Cemitério</h1>
        <div className="flex gap-2 text-sm">
          <button onClick={() => setFilter('all')} className={`px-3 py-1 rounded ${filter === 'all' ? 'bg-purple-700' : 'bg-zinc-800'}`}>Todas</button>
          <button onClick={() => setFilter('perma')} className={`px-3 py-1 rounded ${filter === 'perma' ? 'bg-purple-700' : 'bg-zinc-800'}`}>💀 Permanentes</button>
          <button onClick={() => setFilter('top')} className={`px-3 py-1 rounded ${filter === 'top' ? 'bg-purple-700' : 'bg-zinc-800'}`}>🔥 Mais lembradas</button>
        </div>
      </div>

      <p className="text-sm text-zinc-500 italic">
        Lápides automáticas criadas a partir de mortes detectadas pelo mod. Players escrevem epitáfio
        e outros reagem com ❤ — lembrança coletiva.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {items.map((m) => (
          <div key={m.id} className={`rounded p-4 border transition ${
            highlight === m.id ? 'border-red-500 ring-4 ring-red-500/30 bg-red-950/40' :
            m.permanent ? 'bg-zinc-950 border-zinc-700' : 'bg-zinc-900 border-zinc-800'
          }`}>
            <div className="text-center">
              <div className="text-4xl mb-2">{m.permanent ? '⚰️' : '🪦'}</div>
              <div className="text-xs text-zinc-500 uppercase tracking-widest">Aqui jaz</div>
              <div className="text-xl font-bold mt-1">{m.playerName}</div>
              <div className="text-xs text-zinc-500 mt-1">{new Date(m.diedAt).toLocaleDateString()}</div>
              <div className="text-xs text-zinc-400 mt-2">
                Causa: <span className="text-amber-400">{m.causeOfDeath}</span>
              </div>
              {m.epitaph ? (
                <p className="mt-3 italic text-zinc-300 text-sm border-t border-zinc-700 pt-3">"{m.epitaph}"</p>
              ) : (
                <button onClick={() => setEditing(m)} className="mt-3 text-xs text-zinc-500 hover:text-zinc-300 italic">
                  (sem epitáfio — clique pra escrever)
                </button>
              )}
              <div className="text-xs text-zinc-500 mt-2 font-mono">
                {m.dimension} {m.x},{m.y},{m.z}
              </div>
            </div>
            <div className="flex items-center gap-1 mt-3 justify-center flex-wrap">
              <button onClick={() => react.mutate(m.id)} className="bg-red-900 hover:bg-red-700 px-3 py-1 rounded text-sm">
                ❤ {m.reactions}
              </button>
              <button onClick={() => setTpPicker(m.id)} className="bg-emerald-900 hover:bg-emerald-700 px-2 py-1 rounded text-xs" title="TP até a lápide">🌀</button>
              <button onClick={() => materialize.mutate(m.id)} className="bg-amber-900 hover:bg-amber-700 px-2 py-1 rounded text-xs" title="Construir cruz no local">🪦</button>
              <button onClick={() => setEditing(m)} className="bg-zinc-800 hover:bg-zinc-700 px-2 py-1 rounded text-xs">✍</button>
              <button onClick={() => remove.mutate(m.id)} className="text-zinc-500 hover:text-red-400 text-xs">🗑</button>
            </div>
          </div>
        ))}
        {items.length === 0 && <div className="col-span-full text-center text-zinc-500 py-12 italic">Que nenhum tenha tombado ainda.</div>}
      </div>

      {tpPicker && (
        <div onClick={() => setTpPicker(null)} className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-md w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">🌀 Quem visita a lápide?</h2>
            <select onChange={(e) => { if (e.target.value) { visit.mutate({ id: tpPicker, playerUuid: e.target.value }); setTpPicker(null) } }}
              className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2">
              <option value="">Selecione player...</option>
              {players.map(p => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
            <button onClick={() => setTpPicker(null)} className="w-full bg-zinc-700 px-4 py-2 rounded">Cancelar</button>
          </div>
        </div>
      )}

      {editing && (
        <div onClick={() => setEditing(null)} className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-6">
          <form onSubmit={(e) => {
            e.preventDefault()
            const fd = new FormData(e.currentTarget)
            saveEpitaph.mutate({ id: editing.id, epitaph: fd.get('epitaph') as string })
          }} onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-xl w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">Epitáfio — {editing.playerName}</h2>
            <p className="text-xs text-zinc-500">Morte em {new Date(editing.diedAt).toLocaleString()} por {editing.causeOfDeath}</p>
            <textarea name="epitaph" defaultValue={editing.epitaph || ''} rows={5} placeholder="Aqui jaz alguém que..."
              className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 italic" />
            <div className="flex gap-2">
              <button type="submit" className="ml-auto bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">Salvar</button>
              <button type="button" onClick={() => setEditing(null)} className="bg-zinc-700 px-4 py-2 rounded">Cancelar</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
