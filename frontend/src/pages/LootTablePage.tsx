import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, LootItemDto } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Loot Table — sistema de prêmios aleatórios com raridade ponderada.
 * Quanto maior o weight, maior a chance de sair no draw. Rarities têm
 * weights default sensíveis mas tudo é customizável.
 */

const RARITIES = [
  { id: 'common', label: 'Comum', color: 'text-zinc-300', bg: 'bg-zinc-500/20', defaultWeight: 50 },
  { id: 'uncommon', label: 'Incomum', color: 'text-green-300', bg: 'bg-green-500/20', defaultWeight: 25 },
  { id: 'rare', label: 'Raro', color: 'text-cyan-300', bg: 'bg-cyan-500/20', defaultWeight: 10 },
  { id: 'epic', label: 'Épico', color: 'text-purple-300', bg: 'bg-purple-500/20', defaultWeight: 4 },
  { id: 'legendary', label: 'Lendário', color: 'text-amber-300', bg: 'bg-amber-500/20', defaultWeight: 1 },
  { id: 'mythic', label: 'Mítico', color: 'text-red-300', bg: 'bg-red-500/20', defaultWeight: 1 },
] as const

const CATEGORIES = [
  { id: 'misc', label: '📦 Diversos' },
  { id: 'gear', label: '⚔ Equipamento' },
  { id: 'tool', label: '🛠 Ferramentas' },
  { id: 'food', label: '🍎 Comida' },
  { id: 'magic', label: '🔮 Mágico' },
  { id: 'currency', label: '💰 Moeda' },
  { id: 'decoration', label: '🎨 Decoração' },
  { id: 'special', label: '✨ Especial' },
]

export function LootTablePage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<LootItemDto | null>(null)
  const [filterRarity, setFilterRarity] = useState<string>('')
  const [filterCategory, setFilterCategory] = useState<string>('')
  const [search, setSearch] = useState('')
  const [giveTo, setGiveTo] = useState('')

  const itemsQ = useQuery({ queryKey: ['loot-items'], queryFn: () => api.lootItems() })
  const statsQ = useQuery({ queryKey: ['loot-stats'], queryFn: api.lootStats })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 8000 })

  const createMut = useMutation({
    mutationFn: api.lootCreate,
    onSuccess: () => { toast.ok('✓ item criado'); qc.invalidateQueries({ queryKey: ['loot-items'] }); setEditing(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const updateMut = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<LootItemDto> }) => api.lootUpdate(id, body),
    onSuccess: () => { toast.ok('✓ atualizado'); qc.invalidateQueries({ queryKey: ['loot-items'] }); setEditing(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const deleteMut = useMutation({
    mutationFn: api.lootDelete,
    onSuccess: () => { toast.ok('🗑 deletado'); qc.invalidateQueries({ queryKey: ['loot-items'] }) },
  })
  const drawMut = useMutation({
    mutationFn: api.lootDraw,
    onSuccess: (r) => {
      if (r.ok && r.item) toast.ok(`🎲 Sorteado: ${r.item.name} (${r.item.rarity})`)
      else toast.err(r.error ?? 'falhou')
    },
  })
  const giveMut = useMutation({
    mutationFn: api.lootGive,
    onSuccess: (r) => {
      if (r.ok && r.item) toast.ok(`🎁 ${giveTo} recebeu: ${r.item.name}`)
      else toast.err(r.error ?? 'falhou')
      qc.invalidateQueries({ queryKey: ['loot-items'] })
    },
  })
  const importMut = useMutation({
    mutationFn: api.lootImportPresets,
    onSuccess: (r) => {
      toast.ok(`📥 ${r.added} items importados${r.skipped > 0 ? ` (${r.skipped} já existiam)` : ''}`)
      qc.invalidateQueries({ queryKey: ['loot-items'] })
    },
  })

  const items = Array.isArray(itemsQ.data) ? itemsQ.data : []
  const players = Array.isArray(playersQ.data) ? playersQ.data : []

  const filtered = useMemo(() => items.filter(it => {
    if (filterRarity && it.rarity !== filterRarity) return false
    if (filterCategory && it.category !== filterCategory) return false
    if (search) {
      const q = search.toLowerCase()
      if (!it.name.toLowerCase().includes(q) && !it.itemId.toLowerCase().includes(q)) return false
    }
    return true
  }), [items, filterRarity, filterCategory, search])

  return (
    <div className="route-fade max-w-[1600px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎲 Loot Table</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Itens aleatórios com raridade ponderada. Quanto maior o weight, maior a chance de cair.
            Pode ser usado em Auto-Gifts ou disparado manualmente pra qualquer player.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost" onClick={() => importMut.mutate()}
            disabled={importMut.isPending}
            title="Importa ~30 items famosos do MC com raridades sensatas">
            📥 Importar presets
          </button>
          <button className="btn" onClick={() => setEditing({
            name: '', itemId: '', count: 1, rarity: 'common', weight: 50,
            category: 'misc', enabled: true,
          } as LootItemDto)}>+ Novo</button>
        </div>
      </header>

      {/* Stats */}
      {statsQ.data && (
        <div className="grid grid-cols-2 md:grid-cols-6 gap-2 text-xs mb-4">
          <div className="card text-center">
            <div className="text-2xl font-bold">{statsQ.data.total}</div>
            <div className="text-liberthia-300/60">Total items</div>
          </div>
          {RARITIES.map(r => (
            <div key={r.id} className={`card text-center ${r.bg}`}>
              <div className={`text-2xl font-bold ${r.color}`}>
                {statsQ.data.byRarity?.[r.id] ?? 0}
              </div>
              <div className="text-liberthia-300/60">{r.label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Action bar — draw/give */}
      <div className="card-glow mb-4 flex flex-wrap gap-2 items-end">
        <button className="btn"
          onClick={() => drawMut.mutate({ category: filterCategory || undefined, rarity: filterRarity || undefined })}>
          🎲 Sortear 1 (preview)
        </button>
        <div className="flex-1 min-w-[200px]">
          <label className="label">Player pra dar:</label>
          <select className="input" value={giveTo} onChange={(e) => setGiveTo(e.target.value)}>
            <option value="">Selecione…</option>
            {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
          </select>
        </div>
        <button className="btn" disabled={!giveTo}
          onClick={() => giveMut.mutate({
            playerName: giveTo,
            category: filterCategory || undefined,
            rarity: filterRarity || undefined,
          })}>
          🎁 Sortear + dar
        </button>
      </div>

      {/* Filtros */}
      <div className="flex items-center gap-2 flex-wrap mb-3 text-xs">
        <input className="input flex-1" placeholder="Buscar nome/id…"
          value={search} onChange={(e) => setSearch(e.target.value)} />
        <button className={`btn-ghost btn-sm ${filterRarity === '' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilterRarity('')}>Todas raridades</button>
        {RARITIES.map(r => (
          <button key={r.id}
            className={`btn-ghost btn-sm ${filterRarity === r.id ? 'ring-1 ring-purple-400' : ''} ${r.color}`}
            onClick={() => setFilterRarity(r.id === filterRarity ? '' : r.id)}>
            {r.label}
          </button>
        ))}
      </div>
      <div className="flex items-center gap-1 flex-wrap mb-3 text-xs">
        <button className={`btn-ghost btn-sm ${filterCategory === '' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilterCategory('')}>Todas</button>
        {CATEGORIES.map(c => (
          <button key={c.id}
            className={`btn-ghost btn-sm ${filterCategory === c.id ? 'ring-1 ring-purple-400' : ''}`}
            onClick={() => setFilterCategory(c.id === filterCategory ? '' : c.id)}>
            {c.label}
          </button>
        ))}
      </div>

      {/* Lista de items */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
        {filtered.map(it => {
          const rarity = RARITIES.find(r => r.id === it.rarity) ?? RARITIES[0]
          return (
            <div key={it.id} className={`card-glow ${!it.enabled ? 'opacity-50' : ''} ${rarity.bg}`}>
              <div className="flex items-start gap-2">
                <div className="flex-1 min-w-0">
                  <div className={`font-bold ${rarity.color}`}>{it.name}</div>
                  <code className="text-[10px] text-liberthia-300/40 truncate block">{it.itemId} ×{it.count}</code>
                </div>
                <span className={`badge ${rarity.color}`}>{rarity.label}</span>
              </div>
              {it.description && (
                <p className="text-[11px] text-liberthia-300/70 italic mt-1">"{it.description}"</p>
              )}
              {it.nbt && (
                <details className="mt-1">
                  <summary className="text-[9px] text-liberthia-300/50 cursor-pointer">NBT</summary>
                  <code className="text-[9px] text-liberthia-300/60 block break-all">{it.nbt}</code>
                </details>
              )}
              <div className="flex items-center gap-2 text-[10px] text-liberthia-300/50 mt-1">
                <span>⚖ weight: <strong className="text-purple-300">{it.weight}</strong></span>
                <span>·</span>
                <span>{it.category}</span>
                {(it.drawCount ?? 0) > 0 && (
                  <>
                    <span>·</span>
                    <span>🎲 {it.drawCount}</span>
                  </>
                )}
              </div>
              <div className="flex gap-1 mt-2">
                <button className="btn-ghost btn-sm flex-1" onClick={() => setEditing(it)}>✏ Editar</button>
                <button className="btn-danger btn-sm"
                  onClick={() => { if (it.id && confirm(`Deletar "${it.name}"?`)) deleteMut.mutate(it.id) }}>🗑</button>
              </div>
            </div>
          )
        })}
        {!itemsQ.isLoading && filtered.length === 0 && (
          <div className="card col-span-full text-center py-12 text-liberthia-300/60">
            {items.length === 0
              ? 'Loot table vazia. Click "📥 Importar presets" pra começar com ~30 items.'
              : 'Nenhum item bate com os filtros.'}
          </div>
        )}
      </div>

      {editing && (
        <ItemEditor item={editing}
          onCancel={() => setEditing(null)}
          onSave={(it) => {
            if (it.id) updateMut.mutate({ id: it.id, body: it })
            else createMut.mutate(it)
          }} />
      )}
    </div>
  )
}

function ItemEditor({ item, onSave, onCancel }: {
  item: LootItemDto
  onSave: (it: LootItemDto) => void
  onCancel: () => void
}) {
  const [it, setIt] = useState(item)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onCancel}>
      <div className="card max-w-xl w-full max-h-[90vh] overflow-y-auto space-y-2" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold">{it.id ? '✏ Editar' : '+ Novo'} item</h3>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="label">Nome</label>
            <input className="input" value={it.name} onChange={(e) => setIt({ ...it, name: e.target.value })} />
          </div>
          <div>
            <label className="label">Item ID</label>
            <input className="input" placeholder="minecraft:diamond" value={it.itemId}
              onChange={(e) => setIt({ ...it, itemId: e.target.value })} />
          </div>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <div>
            <label className="label">Count</label>
            <input type="number" min={1} className="input" value={it.count}
              onChange={(e) => setIt({ ...it, count: Number(e.target.value) || 1 })} />
          </div>
          <div>
            <label className="label">Raridade</label>
            <select className="input" value={it.rarity}
              onChange={(e) => {
                const r = RARITIES.find(x => x.id === e.target.value)
                setIt({ ...it, rarity: e.target.value as any, weight: r?.defaultWeight ?? it.weight })
              }}>
              {RARITIES.map(r => <option key={r.id} value={r.id}>{r.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Weight (drop chance)</label>
            <input type="number" min={1} className="input" value={it.weight}
              onChange={(e) => setIt({ ...it, weight: Number(e.target.value) || 1 })} />
          </div>
        </div>
        <div>
          <label className="label">Categoria</label>
          <select className="input" value={it.category}
            onChange={(e) => setIt({ ...it, category: e.target.value })}>
            {CATEGORIES.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Descrição (opcional)</label>
          <input className="input" value={it.description ?? ''}
            onChange={(e) => setIt({ ...it, description: e.target.value })} />
        </div>
        <div>
          <label className="label">NBT (SNBT, opcional — pra encantos, custom name, etc)</label>
          <textarea className="input text-[11px] font-mono" rows={3}
            placeholder='{Enchantments:[{id:"minecraft:sharpness",lvl:3s}]}'
            value={it.nbt ?? ''}
            onChange={(e) => setIt({ ...it, nbt: e.target.value })} />
          <p className="text-[10px] text-liberthia-300/50 mt-1">
            Cola SNBT igual no /give. Ex: <code>{'{Enchantments:[{id:"sharpness",lvl:5s}]}'}</code>
          </p>
        </div>
        <label className="flex items-center gap-2 text-xs">
          <input type="checkbox" checked={it.enabled}
            onChange={(e) => setIt({ ...it, enabled: e.target.checked })} />
          <span>Habilitado (entra nos sorteios)</span>
        </label>
        <div className="flex gap-2 justify-end pt-2">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" disabled={!it.name || !it.itemId} onClick={() => onSave(it)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
