import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { api, ItemDef } from '../lib/api'

type Props = {
  onSelect: (item: ItemDef) => void
  selected?: string
}

export function ItemPicker({ onSelect, selected }: Props) {
  const q = useQuery({
    queryKey: ['items'],
    queryFn: api.items,
    staleTime: 60_000,
    refetchInterval: false,
  })
  const [search, setSearch] = useState('')
  const [filterSource, setFilterSource] = useState<'all' | 'minecraft' | 'liberthia' | 'other'>('all')

  const items = q.data ?? []
  const sources = useMemo(() => {
    const s = new Set<string>()
    items.forEach(i => s.add(i.id.split(':')[0]))
    return Array.from(s).sort()
  }, [items])

  const filtered = useMemo(() => {
    const t = search.toLowerCase()
    return items.filter(i => {
      if (filterSource === 'minecraft' && !i.id.startsWith('minecraft:')) return false
      if (filterSource === 'liberthia' && !i.id.startsWith('liberthia:')) return false
      if (filterSource === 'other' && (i.id.startsWith('minecraft:') || i.id.startsWith('liberthia:'))) return false
      if (!t) return true
      return i.id.toLowerCase().includes(t) || i.name.toLowerCase().includes(t)
    }).slice(0, 200)
  }, [items, search, filterSource])

  return (
    <div className="card">
      <h3 className="font-bold mb-2">📦 Catálogo de Items ({items.length})</h3>
      <div className="flex gap-2 mb-2">
        <input
          className="input flex-1"
          placeholder="Pesquisar (ex: diamond, sword, dark_matter)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select className="input w-32" value={filterSource} onChange={e => setFilterSource(e.target.value as any)}>
          <option value="all">todos ({sources.length})</option>
          <option value="minecraft">vanilla</option>
          <option value="liberthia">liberthia</option>
          <option value="other">outros mods</option>
        </select>
      </div>
      <div className="max-h-96 overflow-y-auto space-y-0.5">
        {filtered.map(it => (
          <button
            key={it.id}
            onClick={() => onSelect(it)}
            className={`w-full text-left px-2 py-1 rounded text-sm flex justify-between gap-2 ${
              selected === it.id ? 'bg-liberthia-500 text-white' : 'hover:bg-liberthia-700'
            }`}
          >
            <span className="truncate">{it.name}</span>
            <span className="text-xs opacity-50 truncate">{it.id}</span>
          </button>
        ))}
        {filtered.length === 0 && search && (
          <p className="text-liberthia-300/60 italic text-center py-4">nenhum match pra "{search}"</p>
        )}
      </div>
    </div>
  )
}
