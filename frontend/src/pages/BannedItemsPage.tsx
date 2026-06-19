import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, BannedItemDto } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Banlist de items gerenciada 100% pelo painel web.
 *
 * Fluxo:
 *   1. Admin pesquisa item via autocomplete (busca em /api/banned-items/search-items,
 *      que proxia /api/items do mod — 1800+ items vanilla + de mods).
 *   2. Escolhe modo: auto_clear (default), silent (sem chat), broadcast (só anuncia).
 *   3. Backend salva no banco e dispara /clear @a {item} imediato + a cada 30s
 *      (via scheduler).
 *
 * Sem mexer no mod do MC — só usa /clear vanilla via ModBridgeClient.
 */

const MODE_LABELS: Record<string, { label: string; emoji: string; desc: string }> = {
  auto_clear: { label: 'Auto-clear', emoji: '🗑',
    desc: '/clear a cada 30s + broadcast no ban' },
  silent:     { label: 'Silent',    emoji: '🤫',
    desc: '/clear sem anunciar no chat' },
  broadcast:  { label: 'Broadcast', emoji: '📢',
    desc: 'Só anuncia, não remove (placebo)' },
}

/** Items que costumam ser problemáticos — quick-ban com 1 click. */
const QUICK_BANS: Array<{ id: string; label: string; emoji: string; reason: string }> = [
  { id: 'minecraft:tnt', label: 'TNT', emoji: '💥', reason: 'grief: dano em chunk loaded' },
  { id: 'minecraft:bedrock', label: 'Bedrock', emoji: '🪨', reason: 'creative item indevido' },
  { id: 'minecraft:command_block', label: 'Command Block', emoji: '⌨', reason: 'só admins' },
  { id: 'minecraft:debug_stick', label: 'Debug Stick', emoji: '🪄', reason: 'cheat' },
  { id: 'minecraft:structure_block', label: 'Structure Block', emoji: '🏗', reason: 'só admins' },
  { id: 'minecraft:jigsaw', label: 'Jigsaw', emoji: '🧩', reason: 'só admins' },
  { id: 'minecraft:barrier', label: 'Barrier', emoji: '🚧', reason: 'só admins' },
  { id: 'minecraft:end_portal_frame', label: 'End Portal Frame', emoji: '🌌', reason: 'item indevido' },
  { id: 'minecraft:dragon_egg', label: 'Dragon Egg', emoji: '🥚', reason: 'duplicação' },
  { id: 'minecraft:netherite_block', label: 'Netherite Block', emoji: '⬛', reason: 'troll banlist' },
]

export function BannedItemsPage() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<'all' | 'active' | 'inactive'>('all')
  const [showPresets, setShowPresets] = useState(false)

  const listQ = useQuery({
    queryKey: ['banned-items'],
    queryFn: api.bannedItemsList,
    refetchInterval: 10_000,
  })

  const unbanMut = useMutation({
    mutationFn: api.bannedItemUnban,
    onSuccess: () => { toast.ok('✓ desbanido'); qc.invalidateQueries({ queryKey: ['banned-items'] }) },
    onError: (e: any) => toast.err(e.message),
  })
  const banMut = useMutation({
    mutationFn: api.bannedItemBan,
    onSuccess: () => { toast.ok('⛔ banido'); qc.invalidateQueries({ queryKey: ['banned-items'] }) },
    onError: (e: any) => toast.err(e.message),
  })
  const toggleMut = useMutation({
    mutationFn: api.bannedItemToggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['banned-items'] }),
    onError: (e: any) => toast.err(e.message),
  })
  const enforceMut = useMutation({
    mutationFn: api.bannedItemEnforce,
    onSuccess: () => toast.ok('⚡ /clear disparado'),
    onError: (e: any) => toast.err(e.message),
  })

  const all = Array.isArray(listQ.data) ? listQ.data : []
  const filtered = useMemo(() => {
    let list = all
    if (filter === 'active') list = list.filter(b => b.active)
    if (filter === 'inactive') list = list.filter(b => !b.active)
    if (search.trim()) {
      const q = search.trim().toLowerCase()
      list = list.filter(b =>
        b.itemId.toLowerCase().includes(q) ||
        (b.displayName ?? '').toLowerCase().includes(q) ||
        (b.reason ?? '').toLowerCase().includes(q))
    }
    return list
  }, [all, filter, search])

  const activeCount = all.filter(b => b.active).length
  const totalClears = all.reduce((acc, b) => acc + (b.clearCount || 0), 0)

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⛔ Banimento de Items</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Banlist via painel, sem precisar SSH ou entrar no servidor. Backend roda <code>/clear @a {'<item>'}</code> a
            cada 30s + broadcast no chat ao banir.
          </p>
        </div>
        <div className="flex gap-2 text-xs">
          <span className="badge badge-red">{activeCount} ativos</span>
          <span className="badge">{all.length} total</span>
          <span className="badge badge-purple">{totalClears} /clear disparados</span>
        </div>
      </header>

      {/* ============ Form com Autocomplete ============ */}
      <BanItemForm />

      {/* ============ Quick-ban presets (items vanilla problemáticos comuns) ============ */}
      <div className="card mt-3">
        <button onClick={() => setShowPresets(s => !s)}
          className="flex items-center gap-2 w-full text-left text-xs">
          <span className="text-base">⚡</span>
          <span className="font-bold">Quick-ban presets — items vanilla problemáticos</span>
          <span className="text-liberthia-300/40 ml-auto">{showPresets ? '▼' : '▶'}</span>
        </button>
        {showPresets && (
          <div className="mt-2 grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-1.5 text-xs">
            {QUICK_BANS.map(q => {
              const already = all.find(b => b.itemId === q.id)
              return (
                <button key={q.id}
                  disabled={!!already}
                  onClick={() => banMut.mutate({
                    itemId: q.id, displayName: q.label, reason: q.reason, mode: 'auto_clear'
                  })}
                  className={`rounded p-2 text-left transition ${
                    already
                      ? 'bg-zinc-800/40 opacity-60 cursor-not-allowed'
                      : 'bg-liberthia-900/60 hover:bg-red-500/20'
                  }`}
                  title={already ? 'Já banido' : q.reason}>
                  <div className="font-bold">{q.emoji} {q.label}</div>
                  <code className="text-[9px] text-liberthia-300/50 truncate block">{q.id}</code>
                  {already && <div className="text-[9px] text-emerald-300 mt-0.5">✓ já banido</div>}
                </button>
              )
            })}
          </div>
        )}
      </div>

      {/* ============ Filtros + Lista ============ */}
      <div className="mt-6 flex items-center gap-2 flex-wrap text-xs">
        <input className="input flex-1" placeholder="Buscar id/nome/motivo…"
          value={search} onChange={(e) => setSearch(e.target.value)} />
        <button className={`btn-ghost btn-sm ${filter === 'all' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilter('all')}>Todos</button>
        <button className={`btn-ghost btn-sm ${filter === 'active' ? 'ring-1 ring-red-400' : ''}`}
          onClick={() => setFilter('active')}>Só ativos</button>
        <button className={`btn-ghost btn-sm ${filter === 'inactive' ? 'ring-1 ring-zinc-400' : ''}`}
          onClick={() => setFilter('inactive')}>Pausados</button>
      </div>

      {listQ.isLoading && (
        <div className="card text-center py-12 mt-4">Carregando…</div>
      )}

      <div className="mt-3 space-y-2">
        {filtered.map((b) => (
          <div key={b.id} className={`card-glow flex items-center gap-3 ${
            !b.active ? 'opacity-50' : ''
          }`}>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-base font-bold">{b.displayName || b.itemId}</span>
                <code className="text-[10px] text-liberthia-300/50">{b.itemId}</code>
                <span className={`badge ${b.active ? 'badge-red' : 'badge'}`}>
                  {MODE_LABELS[b.mode]?.emoji} {MODE_LABELS[b.mode]?.label}
                </span>
                {!b.active && <span className="badge">⏸ pausado</span>}
              </div>
              {b.reason && (
                <div className="text-xs text-liberthia-300/70 italic mt-0.5">"{b.reason}"</div>
              )}
              <div className="text-[10px] text-liberthia-300/40 mt-0.5">
                Banido por {b.bannedBy || '?'} · {new Date(b.bannedAt).toLocaleString()}
                {b.clearCount > 0 && <> · {b.clearCount} disparos de /clear</>}
              </div>
            </div>
            <button className="btn-ghost btn-sm" title="Disparar /clear agora"
              onClick={() => b.id && enforceMut.mutate(b.id)}>⚡</button>
            <button className="btn-ghost btn-sm" title={b.active ? 'Pausar (mantém na lista, para /clear)' : 'Reativar ban'}
              onClick={() => b.id && toggleMut.mutate(b.id)}>
              {b.active ? '⏸' : '▶'}
            </button>
            <button className="btn-danger btn-sm font-bold" title="Desbanir definitivamente (remove do banlist)"
              onClick={() => {
                if (!b.id) return
                if (confirm(`Desbanir "${b.displayName || b.itemId}"?\n\nO item volta a poder ser usado no servidor.`)) unbanMut.mutate(b.id)
              }}>🔓 Desbanir</button>
          </div>
        ))}
        {!listQ.isLoading && filtered.length === 0 && (
          <div className="card text-center py-12 text-liberthia-300/60">
            {all.length === 0
              ? 'Nenhum item banido ainda. Use a busca acima pra começar.'
              : 'Nenhum item bate com os filtros.'}
          </div>
        )}
      </div>
    </div>
  )
}

// ============================================================================
function BanItemForm() {
  const qc = useQueryClient()
  const [query, setQuery] = useState('')
  const [showResults, setShowResults] = useState(false)
  const [selected, setSelected] = useState<{ id: string; displayName: string } | null>(null)
  const [reason, setReason] = useState('')
  const [mode, setMode] = useState<'auto_clear' | 'broadcast' | 'silent'>('auto_clear')
  const dropdownRef = useRef<HTMLDivElement>(null)

  // Debounce do search pra não hammerar /api/items
  const [debouncedQ, setDebouncedQ] = useState('')
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(query), 200)
    return () => clearTimeout(t)
  }, [query])

  const searchQ = useQuery({
    enabled: showResults && debouncedQ.length > 0,
    queryKey: ['ban-item-search', debouncedQ],
    queryFn: () => api.bannedItemSearch(debouncedQ, 30),
    staleTime: 60_000,
  })

  // Fecha dropdown ao clicar fora
  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (!dropdownRef.current?.contains(e.target as Node)) setShowResults(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  const banMut = useMutation({
    mutationFn: api.bannedItemBan,
    onSuccess: () => {
      toast.ok(`⛔ ${selected?.displayName ?? query} banido`)
      qc.invalidateQueries({ queryKey: ['banned-items'] })
      setQuery(''); setSelected(null); setReason('')
    },
    onError: (e: any) => toast.err(e.message),
  })

  function pick(item: { id: string; displayName: string }) {
    setSelected(item)
    setQuery(item.displayName)
    setShowResults(false)
  }

  function submit() {
    const itemId = selected?.id ?? query.trim()
    if (!itemId) { toast.err('escolhe um item'); return }
    banMut.mutate({
      itemId,
      displayName: selected?.displayName ?? itemId,
      reason: reason.trim(),
      mode,
    })
  }

  const items = Array.isArray(searchQ.data?.items) ? searchQ.data!.items : []

  return (
    <div className="card-glow space-y-2 relative" ref={dropdownRef}>
      <div className="text-xs uppercase tracking-widest text-liberthia-300/60">
        🔍 Banir novo item
      </div>

      <div className="grid grid-cols-1 md:grid-cols-[1fr_180px_120px] gap-2">
        <div className="relative">
          <input className="input"
            placeholder="Digita pra buscar (ex: diamond_sword, create:, alexsmobs:warped)…"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setSelected(null)
              setShowResults(true)
            }}
            onFocus={() => setShowResults(true)} />
          {selected && (
            <div className="absolute right-2 top-1/2 -translate-y-1/2 text-xs">
              <span className="badge badge-green text-[10px]">✓ {selected.id}</span>
            </div>
          )}
          {showResults && query.length > 0 && (
            <div className="absolute z-30 left-0 right-0 mt-1 max-h-72 overflow-y-auto rounded bg-liberthia-950 border border-liberthia-700 shadow-2xl">
              {searchQ.isLoading && (
                <div className="p-2 text-xs text-liberthia-300/60">Buscando…</div>
              )}
              {!searchQ.isLoading && items.length === 0 && (
                <div className="p-2 text-xs text-liberthia-300/60">Nenhum resultado.</div>
              )}
              {items.map((it) => (
                <button key={it.id} onClick={() => pick(it)}
                  className="w-full text-left p-2 hover:bg-purple-500/20 border-b border-liberthia-800/60 last:border-0 flex items-center gap-2">
                  <span className="text-sm font-bold">{it.displayName}</span>
                  <code className="text-[10px] text-liberthia-300/40 ml-auto">{it.id}</code>
                </button>
              ))}
            </div>
          )}
        </div>

        <select className="input" value={mode} onChange={(e) => setMode(e.target.value as any)}
          title={MODE_LABELS[mode].desc}>
          {Object.entries(MODE_LABELS).map(([k, v]) => (
            <option key={k} value={k}>{v.emoji} {v.label}</option>
          ))}
        </select>

        <button className="btn" disabled={!query.trim() || banMut.isPending} onClick={submit}>
          ⛔ Banir
        </button>
      </div>

      <input className="input text-xs" placeholder="Motivo (opcional, vai pro chat)"
        value={reason} onChange={(e) => setReason(e.target.value)} />

      <div className="text-[10px] text-liberthia-300/50 italic">
        {MODE_LABELS[mode].emoji} {MODE_LABELS[mode].desc}
      </div>
    </div>
  )
}
