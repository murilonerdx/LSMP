import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, Snapshot, SnapshotItem } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Snapshots horários de cada player. Mostra timeline e diff inventory contents.
 * Items dentro de bolsas/shulker boxes/bundles aparecem como sub-itens recursivos.
 */
export function SnapshotsPage() {
  const qc = useQueryClient()
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [selectedUuid, setSelectedUuid] = useState<string>('')
  const [selectedTs, setSelectedTs] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)

  const listQ = useQuery({
    queryKey: ['snap-list', selectedUuid],
    queryFn: () => selectedUuid ? api.snapshotList(selectedUuid) : Promise.resolve([]),
    enabled: !!selectedUuid,
  })
  const snapQ = useQuery({
    queryKey: ['snap-get', selectedUuid, selectedTs],
    queryFn: () => (selectedUuid && selectedTs) ? api.snapshotGet(selectedUuid, selectedTs) : null,
    enabled: !!selectedUuid && !!selectedTs,
  })

  useEffect(() => {
    if (!selectedTs && (listQ.data ?? []).length) setSelectedTs(listQ.data![listQ.data!.length - 1])
  }, [listQ.data])

  async function runNow() {
    setBusy(true)
    try {
      const r = await api.snapshotRunNow()
      toast.ok(`Snapshot capturou ${(r as any).captured} players`)
      qc.invalidateQueries({ queryKey: ['snap-list'] })
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }

  return (
    <div className="route-fade max-w-[1600px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📸 Snapshots</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Backup automático de inventário, posição, stats — toda hora. Inclui itens dentro de shulker boxes / bundles / bolsas.
          </p>
        </div>
        <button className="btn" onClick={runNow} disabled={busy}>📸 Snapshot Now</button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[280px_240px_1fr] gap-4">
        {/* Players list */}
        <div className="card">
          <h3 className="font-bold mb-2">👥 Players</h3>
          <div className="space-y-1 max-h-96 overflow-y-auto">
            {players.length === 0 && <div className="text-xs text-liberthia-300/50 italic py-4 text-center">Ninguém online</div>}
            {players.map(p => (
              <button
                key={p.uuid}
                className={`w-full text-left px-2 py-1.5 rounded-lg transition flex items-center gap-2 ${
                  selectedUuid === p.uuid ? 'bg-liberthia-500/30 text-white' : 'hover:bg-liberthia-700/30'
                }`}
                onClick={() => { setSelectedUuid(p.uuid); setSelectedTs(null) }}
              >
                <img src={`https://mc-heads.net/avatar/${p.uuid}/24`} className="w-6 h-6 rounded" />
                <span className="text-sm flex-1">{p.name}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Timeline */}
        <div className="card">
          <h3 className="font-bold mb-2">📅 Timeline</h3>
          {!selectedUuid && <div className="text-xs text-liberthia-300/50 italic py-4 text-center">— escolha um player —</div>}
          {selectedUuid && (
            <div className="space-y-1 max-h-96 overflow-y-auto">
              {(listQ.data ?? []).length === 0 && (
                <div className="text-xs text-liberthia-300/50 italic py-4 text-center">
                  Sem snapshots ainda. Clique "Snapshot Now".
                </div>
              )}
              {(listQ.data ?? []).slice().reverse().map((ts) => (
                <button
                  key={ts}
                  onClick={() => setSelectedTs(ts)}
                  className={`w-full text-left px-2 py-1 rounded-md transition text-xs font-mono ${
                    selectedTs === ts ? 'bg-liberthia-500/30 text-white' : 'hover:bg-liberthia-700/30 text-liberthia-200'
                  }`}
                >
                  {new Date(ts).toLocaleString()}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Detail */}
        <div className="space-y-4">
          {snapQ.data && <SnapshotDetail snap={snapQ.data} />}
          {selectedUuid && !selectedTs && (listQ.data ?? []).length === 0 && (
            <div className="card text-center py-12">
              <div className="text-5xl mb-3 opacity-50">📸</div>
              <p className="text-liberthia-300/70 text-sm">Sem snapshots pra esse player ainda.</p>
            </div>
          )}
          {!selectedUuid && (
            <div className="card text-center py-12">
              <div className="text-5xl mb-3 opacity-50">👈</div>
              <p className="text-liberthia-300/70 text-sm">Escolha um player na lista.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function SnapshotDetail({ snap }: { snap: Snapshot }) {
  const totalItems = countAll(snap.main) + countAll(snap.armor) + countAll([snap.offhand]) + countAll(snap.ender)
  const [showRestore, setShowRestore] = useState(false)
  return (
    <>
      <div className="card-glow">
        <div className="flex items-center gap-3 mb-3">
          <img src={`https://mc-heads.net/avatar/${snap.uuid}/40`} className="rounded" />
          <div>
            <div className="font-bold text-lg">{snap.name}</div>
            <div className="text-xs text-liberthia-300/60 font-mono">{new Date(snap.ts).toLocaleString()}</div>
          </div>
          <span className="badge badge-purple">{snap.gameMode}</span>
          <button className="btn-amber btn-sm ml-auto" onClick={() => setShowRestore(true)}>
            ⟲ Restore
          </button>
        </div>
        {showRestore && <RestoreModal snap={snap} onClose={() => setShowRestore(false)} />}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
          <Stat label="HP" v={`${snap.health.toFixed(0)} / ${snap.maxHealth.toFixed(0)}`} />
          <Stat label="Food" v={`${snap.food}/20`} />
          <Stat label="XP" v={`L${snap.xpLevel} (${snap.xpTotal})`} />
          <Stat label="Total items" v={`${totalItems}`} />
          <Stat label="Dimension" v={snap.dimension.replace('minecraft:', '')} />
          <Stat label="Position" v={`${snap.position.x.toFixed(0)}, ${snap.position.y.toFixed(0)}, ${snap.position.z.toFixed(0)}`} />
          {snap.matter && <Stat label="Matter" v={`${snap.matter.type}`} />}
          {snap.matter && <Stat label="DM/WM/YM" v={`${snap.matter.dm.toFixed(1)}/${snap.matter.wm.toFixed(1)}/${snap.matter.ym.toFixed(1)}`} />}
        </div>
      </div>

      <Section title="🎒 Inventário Principal" items={snap.main} />
      <Section title="🛡 Armadura + Offhand" items={[...snap.armor, snap.offhand]} />
      <Section title="📦 Ender Chest" items={snap.ender} />
    </>
  )
}

function Section({ title, items }: { title: string; items: SnapshotItem[] }) {
  const nonEmpty = items.filter((i) => !i.empty)
  return (
    <div className="card">
      <div className="flex items-center justify-between mb-2">
        <h3 className="font-bold">{title}</h3>
        <span className="badge badge-purple">{nonEmpty.length}</span>
      </div>
      {nonEmpty.length === 0 && <div className="text-xs text-liberthia-300/50 italic">vazio</div>}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-1.5">
        {nonEmpty.map((it, i) => <ItemRow key={i} item={it} />)}
      </div>
    </div>
  )
}

function ItemRow({ item, depth = 0 }: { item: SnapshotItem; depth?: number }) {
  const [open, setOpen] = useState(false)
  const hasContents = (item.contents ?? []).length > 0
  const hasNbt = !!item.nbt
  const expand = hasContents || hasNbt
  return (
    <div style={{ marginLeft: depth * 16 }}>
      <div
        className={`flex items-center gap-2 px-2 py-1 rounded-md text-xs ${expand ? 'cursor-pointer hover:bg-liberthia-700/30' : ''}`}
        onClick={() => expand && setOpen((o) => !o)}
      >
        {expand && <span className="text-liberthia-300/60 w-3">{open ? '▼' : '▶'}</span>}
        {!expand && <span className="w-3" />}
        <span className="text-liberthia-300/40 w-8 font-mono shrink-0">#{item.slot}</span>
        <span className="font-bold shrink-0">×{item.count}</span>
        <span className="font-mono text-liberthia-200/90 truncate">{item.id}</span>
        {(item.enchantments ?? []).length > 0 && (
          <span className="badge badge-purple">⚡ {(item.enchantments ?? []).length}</span>
        )}
        {hasContents && <span className="badge badge-cyan">📦 {item.contents!.length}</span>}
      </div>
      {open && hasContents && (
        <div className="mt-1 space-y-0.5 border-l border-liberthia-500/20 pl-2 ml-3">
          {item.contents!.map((c, i) => <ItemRow key={i} item={c} depth={depth + 1} />)}
        </div>
      )}
      {open && hasNbt && (
        <pre className="text-[10px] font-mono bg-black/40 p-2 rounded mt-1 ml-6 text-liberthia-300/70 overflow-x-auto max-h-32">
          {item.nbt}
        </pre>
      )}
    </div>
  )
}

function Stat({ label, v }: { label: string; v: any }) {
  return (
    <div className="px-3 py-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
      <div className="label">{label}</div>
      <div className="font-mono">{v}</div>
    </div>
  )
}

type Mode = 'replace' | 'merge' | 'chest'

function RestoreModal({ snap, onClose }: { snap: Snapshot; onClose: () => void }) {
  const [mode, setMode] = useState<Mode>('merge')
  const [restoreStats, setRestoreStats] = useState(false)
  const [restorePosition, setRestorePosition] = useState(false)
  const [busy, setBusy] = useState(false)
  const [confirm, setConfirm] = useState(false)

  // Coleta TODOS items não vazios em ordem (main + armor + offhand + ender)
  function collectItems(): SnapshotItem[] {
    const out: SnapshotItem[] = []
    for (const arr of [snap.main, snap.armor, [snap.offhand], snap.ender]) {
      for (const it of arr) if (!it.empty && it.id) out.push(it)
    }
    return out
  }

  async function runReplace() {
    const r: any = await api.snapshotRestore(snap.uuid, snap.ts, {
      restoreInventory: true, restoreStats, restorePosition,
    })
    toast.ok(`✓ Restaurados ${r.restoredItems ?? 0} items (replace)`)
  }

  async function runMerge() {
    // Pure vanilla: /give pra cada item — adiciona sem apagar inventário atual
    const players = await api.players()
    const p = players.find((x) => x.uuid === snap.uuid)
    if (!p) throw new Error('Player offline')
    const items = collectItems()
    let given = 0
    for (const it of items) {
      let cmd = `give ${p.name} ${it.id}`
      if (it.nbt) cmd += it.nbt           // nbt já é SNBT do tag
      cmd += ` ${it.count ?? 1}`
      try { await api.command(cmd, 'snapshot-merge'); given++ } catch {}
    }
    if (restoreStats) {
      await api.heal(snap.uuid).catch(() => {})
      await api.command(`xp set ${p.name} ${snap.xpLevel} levels`, 'snapshot-merge').catch(() => {})
    }
    if (restorePosition) {
      await api.teleport(snap.uuid, snap.position.x, snap.position.y, snap.position.z).catch(() => {})
    }
    toast.ok(`✓ ${given}/${items.length} items adicionados (merge)`)
  }

  async function runChest() {
    // Cria N barrels na frente do player com items do snapshot
    const players = await api.players()
    const p = players.find((x) => x.uuid === snap.uuid)
    if (!p) throw new Error('Player offline')
    const items = collectItems()
    if (items.length === 0) { toast.err('Snapshot sem items'); return }

    // Direção à frente baseada no yaw atual do player
    const yawRad = ((p.position.yaw + 180) * Math.PI) / 180
    const dx = Math.round(Math.sin(yawRad))
    const dz = Math.round(-Math.cos(yawRad))
    // Perpendicular pra alinhar múltiplos barrels lado a lado
    const perpX = -dz, perpZ = dx
    const baseX = Math.floor(p.position.x + dx * 2)
    const baseY = Math.floor(p.position.y)
    const baseZ = Math.floor(p.position.z + dz * 2)

    const PER_CHEST = 27 // barrel = 27 slots
    const chunks: SnapshotItem[][] = []
    for (let i = 0; i < items.length; i += PER_CHEST) chunks.push(items.slice(i, i + PER_CHEST))

    let placed = 0
    for (let i = 0; i < chunks.length; i++) {
      const cx = baseX + perpX * i
      const cz = baseZ + perpZ * i
      const slots: string[] = []
      chunks[i].forEach((it, slot) => {
        let entry = `{Slot:${slot}b,id:"${it.id}",Count:${it.count ?? 1}b`
        if (it.nbt) entry += `,tag:${it.nbt}`
        entry += '}'
        slots.push(entry)
      })
      const nbt = `{Items:[${slots.join(',')}]}`
      try {
        await api.command(`setblock ${cx} ${baseY} ${cz} minecraft:barrel${nbt} replace`, 'snapshot-chest')
        placed++
      } catch (e) { /* ignore individual failures */ }
    }
    // Som de chest + título pro player
    await api.sound(snap.uuid, 'minecraft:block.chest.open', 1, 1).catch(() => {})
    await api.title(snap.uuid, '§6§l📦 Snapshot Items', `§e${items.length} items em ${chunks.length} barrel${chunks.length > 1 ? 's' : ''}`, 10, 80, 20).catch(() => {})
    toast.ok(`📦 ${placed}/${chunks.length} barrel(s) com ${items.length} items @ ${baseX},${baseY},${baseZ}`)
  }

  async function go() {
    if (mode === 'replace' && !confirm) { toast.err('Confirme antes do replace'); return }
    setBusy(true)
    try {
      if (mode === 'replace') await runReplace()
      else if (mode === 'merge') await runMerge()
      else await runChest()
      onClose()
    } catch (e: any) { toast.err(e.message) }
    finally { setBusy(false) }
  }

  const totalItems = collectItems().length

  // Renderiza via Portal pra escapar de qualquer stacking context do parent
  const modal = (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
         onClick={onClose}>
      <div className="card max-w-xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-2 flex items-center gap-2">
          <span className="text-2xl">⟲</span> Restore Snapshot
        </h3>
        <div className="text-sm text-liberthia-300/80 mb-4">
          <span className="font-bold gradient-text">{snap.name}</span> ·{' '}
          <span className="chip">{new Date(snap.ts).toLocaleString()}</span> ·{' '}
          <span className="chip">{totalItems} items</span>
        </div>

        <label className="label block mb-2">Modo</label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mb-4">
          <ModeCard active={mode === 'merge'} onClick={() => setMode('merge')}
            emoji="🔀" title="Mesclar"
            desc="Adiciona items ao inventário ATUAL sem apagar nada. Items que não couberem dropam no chão." />
          <ModeCard active={mode === 'chest'} onClick={() => setMode('chest')}
            emoji="📦" title="Baú na frente"
            desc="Cria barrel(s) 2 blocos à frente do player com TODOS os items do snapshot. Não toca o inventário." />
          <ModeCard active={mode === 'replace'} onClick={() => setMode('replace')}
            emoji="💥" title="Substituir"
            desc="APAGA o inventário atual e cola o do snapshot exatamente. Slots preservados. Precisa mod recente." />
        </div>

        {mode !== 'chest' && (
          <div className="space-y-2 mb-4">
            <label className="flex items-center gap-2 p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20 cursor-pointer">
              <input type="checkbox" checked={restoreStats} onChange={(e) => setRestoreStats(e.target.checked)} />
              <span className="text-sm">❤ <b>Stats</b> (HP, fome, XP level)</span>
            </label>
            <label className="flex items-center gap-2 p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20 cursor-pointer">
              <input type="checkbox" checked={restorePosition} onChange={(e) => setRestorePosition(e.target.checked)} />
              <span className="text-sm">📍 <b>Posição</b> ({snap.position.x.toFixed(0)}, {snap.position.y.toFixed(0)}, {snap.position.z.toFixed(0)})</span>
            </label>
          </div>
        )}

        {mode === 'replace' && (
          <>
            <div className="mb-3 px-3 py-2 rounded-lg bg-amber-500/10 border border-amber-400/30 text-amber-200 text-xs">
              ⚠ <b>O inventário ATUAL vai ser apagado</b> e substituído pelo do snapshot.
              Items dentro de shulkers/bolsas são preservados via NBT.
            </div>
            <label className="flex items-start gap-2 mb-4 p-2 rounded-lg bg-red-500/10 border border-red-400/30 cursor-pointer">
              <input type="checkbox" checked={confirm} onChange={(e) => setConfirm(e.target.checked)} className="mt-1" />
              <span className="text-xs text-red-200">
                Confirmo que entendo que <b>essa ação não pode ser desfeita</b> e o player precisa estar online.
              </span>
            </label>
          </>
        )}

        {mode === 'merge' && (
          <div className="mb-4 px-3 py-2 rounded-lg bg-cyan-500/10 border border-cyan-400/30 text-cyan-200 text-xs">
            💡 Usa <code>/give</code> vanilla — funciona mesmo com mod antigo. Não apaga nada do que o player tem.
          </div>
        )}

        {mode === 'chest' && (
          <div className="mb-4 px-3 py-2 rounded-lg bg-emerald-500/10 border border-emerald-400/30 text-emerald-200 text-xs">
            🪵 Cria <b>{Math.ceil(totalItems / 27)} barrel(s)</b> 2 blocos à frente do player.
            Cada barrel comporta 27 items.
            Não toca o inventário — player abre e pega o que quiser.
          </div>
        )}

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onClose} disabled={busy}>Cancelar</button>
          <button className="btn-amber" onClick={go} disabled={busy || (mode === 'replace' && !confirm)}>
            {busy ? '⏳ Executando...' :
             mode === 'merge' ? '🔀 Mesclar' :
             mode === 'chest' ? '📦 Criar Baú' :
             '💥 Substituir'}
          </button>
        </div>
      </div>
    </div>
  )

  return createPortal(modal, document.body)
}

function ModeCard({ active, emoji, title, desc, onClick }: {
  active: boolean; emoji: string; title: string; desc: string; onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`p-3 rounded-xl border text-left transition ${
        active ? 'bg-liberthia-500/20 border-liberthia-400/60 ring-2 ring-liberthia-400/30'
               : 'bg-liberthia-900/40 border-liberthia-500/20 hover:bg-liberthia-700/30'
      }`}
    >
      <div className="text-2xl mb-1">{emoji}</div>
      <div className="font-bold text-sm mb-1">{title}</div>
      <div className="text-[10px] text-liberthia-300/70 leading-tight">{desc}</div>
    </button>
  )
}

function countAll(items: SnapshotItem[]): number {
  let n = 0
  for (const it of items) {
    if (!it.empty) n++
    if (it.contents) n += countAll(it.contents)
  }
  return n
}
