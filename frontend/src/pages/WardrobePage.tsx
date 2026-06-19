import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const auth = () => ({ Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` })

type Item = {
  id: number; name: string; description: string; kind: string;
  uploaderName: string; downloads: number; upvotes: number; priceMatter: number;
  sizeBytes: number; createdAt: string;
}

export function WardrobePage() {
  const [kind, setKind] = useState('all')
  const [sort, setSort] = useState<'recent' | 'top'>('recent')
  const [showNew, setShowNew] = useState(false)
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['wardrobe', kind, sort],
    queryFn: async () => {
      const params = new URLSearchParams()
      if (kind !== 'all') params.set('kind', kind)
      params.set('sort', sort)
      const r = await fetch(`/api/wardrobe?${params}`, { headers: auth() })
      return r.json()
    },
  })

  const upload = useMutation({
    mutationFn: async (fd: FormData) => {
      const r = await fetch('/api/wardrobe', { method: 'POST', headers: auth(), body: fd })
      if (!r.ok) throw new Error(`${r.status}`)
      return r.json()
    },
    onSuccess: () => { setShowNew(false); qc.invalidateQueries({ queryKey: ['wardrobe'] }) },
  })

  const upvote = useMutation({
    mutationFn: async (id: number) => fetch(`/api/wardrobe/${id}/upvote`, { method: 'POST', headers: auth() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wardrobe'] }),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/wardrobe/${id}`, { method: 'DELETE', headers: auth() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wardrobe'] }),
  })

  const items: Item[] = Array.isArray(q.data?.content) ? q.data.content : []

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const fd = new FormData(e.currentTarget)
    upload.mutate(fd)
  }

  return (
    <div className="p-6 space-y-6 text-white">
      <div className="flex justify-between">
        <h1 className="text-3xl font-bold">👕 Wardrobe</h1>
        <div className="flex gap-2 text-sm">
          {['all', 'armor', 'cpm', 'skin', 'cosmetic'].map((k) => (
            <button key={k} onClick={() => setKind(k)} className={`px-3 py-1 rounded ${kind === k ? 'bg-purple-700' : 'bg-zinc-800'}`}>{k}</button>
          ))}
          <button onClick={() => setSort(sort === 'top' ? 'recent' : 'top')} className="px-3 py-1 rounded bg-zinc-800">{sort === 'top' ? '🔥 Top' : '🕐 Recentes'}</button>
          <button onClick={() => setShowNew(true)} className="bg-emerald-700 hover:bg-emerald-600 px-3 py-1 rounded">+ Upload</button>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
        {items.map((it) => (
          <div key={it.id} className="bg-zinc-900 border border-zinc-800 rounded p-3">
            <div className="text-xs text-purple-400 uppercase">{it.kind}</div>
            <h3 className="font-bold mt-1">{it.name}</h3>
            <p className="text-xs text-zinc-500">por {it.uploaderName}</p>
            <p className="text-sm text-zinc-400 mt-1 line-clamp-2">{it.description}</p>
            <div className="flex items-center gap-2 text-xs mt-3">
              <button onClick={() => upvote.mutate(it.id)} className="bg-green-900 hover:bg-green-700 px-2 py-1 rounded">▲ {it.upvotes}</button>
              <a href={`/api/wardrobe/${it.id}/file`} className="bg-zinc-700 hover:bg-zinc-600 px-2 py-1 rounded">↓ {it.downloads}</a>
              {it.priceMatter > 0 && <span className="bg-purple-900 px-2 py-1 rounded">💎 {it.priceMatter}</span>}
              <button onClick={() => remove.mutate(it.id)} className="ml-auto text-zinc-500 hover:text-red-400">🗑</button>
            </div>
          </div>
        ))}
        {items.length === 0 && <div className="col-span-full text-center text-zinc-500 py-12">Sem itens nessa categoria.</div>}
      </div>

      {showNew && (
        <div onClick={() => setShowNew(false)} className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-6">
          <form onSubmit={handleSubmit} onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-xl w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">+ Upload cosmético</h2>
            <input name="name" required placeholder="Nome" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2" />
            <textarea name="description" rows={2} placeholder="Descrição" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <select name="kind" defaultValue="armor" className="bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm">
              <option value="armor">Armor (ArmourersWorkshop)</option>
              <option value="cpm">CPM Project</option>
              <option value="skin">Skin</option>
              <option value="cosmetic">Cosmetic (CakeCosmetics)</option>
            </select>
            <input name="uploaderName" defaultValue="admin" className="bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input name="priceMatter" type="number" defaultValue={0} className="bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input name="file" type="file" required className="text-sm" />
            <div className="flex gap-2">
              <button type="submit" className="ml-auto bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">Subir</button>
              <button type="button" onClick={() => setShowNew(false)} className="bg-zinc-700 px-4 py-2 rounded">Cancelar</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
