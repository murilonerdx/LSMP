import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

type Quote = {
  id: number
  playerName: string
  playerUuid: string
  text: string
  capturedAt: string
  upvotes: number
  downvotes: number
  score: number
}

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

async function get(path: string) {
  const r = await fetch(path, { headers: headers() })
  if (!r.ok) throw new Error(`${r.status}`)
  return r.json()
}
async function post(path: string, body?: any) {
  const r = await fetch(path, { method: 'POST', headers: headers(), body: body ? JSON.stringify(body) : undefined })
  if (!r.ok) throw new Error(`${r.status}`)
  return r.json()
}
async function del(path: string) {
  const r = await fetch(path, { method: 'DELETE', headers: headers() })
  if (!r.ok) throw new Error(`${r.status}`)
  return r.json()
}

/** Mural de citações capturadas in-game (balões, chat). */
export function QuotesWallPage() {
  const [sort, setSort] = useState<'recent' | 'top'>('recent')
  const [search, setSearch] = useState('')
  const [newText, setNewText] = useState('')
  const [newPlayer, setNewPlayer] = useState('')
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['quotes', sort, search],
    queryFn: () =>
      get(`/api/quotes?sort=${sort}${search ? `&q=${encodeURIComponent(search)}` : ''}&size=100`),
    refetchInterval: 10_000,
  })

  const create = useMutation({
    mutationFn: (body: any) => post('/api/quotes', body),
    onSuccess: () => { setNewText(''); setNewPlayer(''); qc.invalidateQueries({ queryKey: ['quotes'] }) },
  })
  const upvote = useMutation({
    mutationFn: (id: number) => post(`/api/quotes/${id}/upvote`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['quotes'] }),
  })
  const downvote = useMutation({
    mutationFn: (id: number) => post(`/api/quotes/${id}/downvote`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['quotes'] }),
  })
  const remove = useMutation({
    mutationFn: (id: number) => del(`/api/quotes/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['quotes'] }),
  })

  const broadcast = useMutation({
    mutationFn: (id: number) => post(`/api/quotes/${id}/broadcast`),
    onSuccess: () => alert('📢 Broadcasted no chat in-game!'),
    onError: (e: any) => alert('✗ ' + e.message),
  })

  const items: Quote[] = Array.isArray(q.data?.content) ? q.data.content : []

  return (
    <div className="p-6 space-y-6 text-white">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">💬 Mural de Citações</h1>
        <div className="flex gap-2 text-sm">
          <button
            onClick={() => setSort('recent')}
            className={`px-3 py-1 rounded ${sort === 'recent' ? 'bg-purple-700' : 'bg-zinc-800'}`}
          >Recentes</button>
          <button
            onClick={() => setSort('top')}
            className={`px-3 py-1 rounded ${sort === 'top' ? 'bg-purple-700' : 'bg-zinc-800'}`}
          >🔥 Top</button>
        </div>
      </div>

      <input
        value={search} onChange={(e) => setSearch(e.target.value)}
        placeholder="Buscar texto ou jogador…"
        className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm"
      />

      <div className="bg-zinc-900 border border-zinc-700 rounded p-4 space-y-2">
        <h3 className="font-semibold">+ Adicionar Citação</h3>
        <div className="flex gap-2">
          <input value={newPlayer} onChange={(e) => setNewPlayer(e.target.value)}
            placeholder="Player name" className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-sm w-40" />
          <input value={newText} onChange={(e) => setNewText(e.target.value)}
            placeholder="Citação capturada…" className="flex-1 bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-sm" />
          <button
            disabled={!newText || !newPlayer}
            onClick={() => create.mutate({ playerName: newPlayer, text: newText })}
            className="bg-purple-700 hover:bg-purple-600 disabled:bg-zinc-800 px-3 py-1 rounded text-sm">
            Salvar
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {items.map((q) => (
          <div key={q.id} className="bg-zinc-900 border border-zinc-800 rounded p-4 flex flex-col gap-2">
            <p className="text-zinc-200 italic">"{q.text}"</p>
            <div className="text-xs text-zinc-500">— {q.playerName} · {new Date(q.capturedAt).toLocaleString()}</div>
            <div className="flex items-center gap-2 text-sm mt-1">
              <button onClick={() => upvote.mutate(q.id)} className="px-2 py-0.5 rounded bg-green-900 hover:bg-green-700">▲ {q.upvotes}</button>
              <button onClick={() => downvote.mutate(q.id)} className="px-2 py-0.5 rounded bg-red-900 hover:bg-red-700">▼ {q.downvotes}</button>
              <span className={`ml-2 font-mono ${q.score > 0 ? 'text-green-400' : q.score < 0 ? 'text-red-400' : 'text-zinc-500'}`}>
                {q.score > 0 ? '+' : ''}{q.score}
              </span>
              <button onClick={() => broadcast.mutate(q.id)} className="ml-auto px-2 py-0.5 rounded bg-purple-900 hover:bg-purple-700 text-xs" title="Broadcastar no chat do servidor">📢</button>
              <button onClick={() => remove.mutate(q.id)} className="text-xs text-zinc-500 hover:text-red-400">🗑</button>
            </div>
          </div>
        ))}
        {items.length === 0 && (
          <div className="col-span-full text-center text-zinc-500 py-12">
            Nenhuma citação ainda. Cole as melhores frases do chat aqui!
          </div>
        )}
      </div>
    </div>
  )
}
