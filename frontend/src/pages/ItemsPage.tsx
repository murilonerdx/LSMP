import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { api } from '../lib/api'
import { PageHeader } from '../components/PageHeader'

export function ItemsPage() {
  const itemsQ = useQuery({ queryKey: ['items'], queryFn: api.items, staleTime: 60_000 })
  const enchsQ = useQuery({ queryKey: ['enchantments'], queryFn: api.enchantments, staleTime: 60_000 })
  const [tab, setTab] = useState<'items' | 'enchantments'>('items')
  const [search, setSearch] = useState('')

  const items = itemsQ.data ?? []
  const enchs = enchsQ.data ?? []

  const itemsFiltered = useMemo(() => {
    const t = search.toLowerCase()
    return items.filter(i => !t || i.id.toLowerCase().includes(t) || i.name.toLowerCase().includes(t))
  }, [items, search])

  const enchsFiltered = useMemo(() => {
    const t = search.toLowerCase()
    return enchs.filter(e => !t || e.id.toLowerCase().includes(t) || e.name.toLowerCase().includes(t))
  }, [enchs, search])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Catálogo Global"
        subtitle="Todos os items registrados + encantamentos disponíveis"
        icon="📦"
      >
        <input className="input max-w-xs" placeholder="🔍 Pesquisar..." value={search} onChange={e => setSearch(e.target.value)} />
      </PageHeader>

      <div className="flex gap-2 border-b border-liberthia-600/30">
        <TabBtn active={tab === 'items'} onClick={() => setTab('items')}>📦 Items ({items.length})</TabBtn>
        <TabBtn active={tab === 'enchantments'} onClick={() => setTab('enchantments')}>✨ Encantamentos ({enchs.length})</TabBtn>
      </div>

      {tab === 'items' && (
        <div className="card">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2 max-h-[60vh] overflow-y-auto">
            {itemsFiltered.slice(0, 500).map(it => (
              <div key={it.id} className="px-3 py-2 rounded-lg bg-liberthia-900/40 hover:bg-liberthia-700/40 transition">
                <div className="text-sm font-medium">{it.name}</div>
                <div className="text-[10px] font-mono text-liberthia-300/60 truncate">{it.id}</div>
              </div>
            ))}
          </div>
          {itemsFiltered.length > 500 && (
            <p className="text-xs text-liberthia-300/60 mt-3 text-center">
              Mostrando 500 de {itemsFiltered.length} — refine a pesquisa
            </p>
          )}
        </div>
      )}

      {tab === 'enchantments' && (
        <div className="card">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2 max-h-[60vh] overflow-y-auto">
            {enchsFiltered.map(e => (
              <div key={e.id} className="px-3 py-2 rounded-lg bg-liberthia-900/40 hover:bg-liberthia-700/40 transition">
                <div className="flex items-center justify-between">
                  <div className="text-sm font-medium">{e.name}</div>
                  {e.isCurse && <span className="badge badge-red">curse</span>}
                  {e.isTreasure && !e.isCurse && <span className="badge badge-yellow">treasure</span>}
                </div>
                <div className="text-[10px] font-mono text-liberthia-300/60 truncate">{e.id}</div>
                <div className="text-[10px] text-liberthia-300/70 mt-1">max level: {e.maxLevel}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="text-xs text-liberthia-300/60 italic">
        Pra dar items pra players com encantos custom, abre um player → aba "Dar Item".
      </div>
    </div>
  )
}

function TabBtn({ active, onClick, children }: any) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 rounded-t-lg text-sm transition ${
        active ? 'bg-liberthia-500/30 text-white border-b-2 border-liberthia-400' : 'hover:bg-liberthia-700/30 text-liberthia-300/80'
      }`}
    >{children}</button>
  )
}
