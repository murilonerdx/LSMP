import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, TimeBoxDto } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { useItemOptions, useEffectOptions, useParticleOptions, useSoundOptions } from '../lib/mcAutocomplete'

/** TimeLockedBoxes V2 — backend-driven. Entrega agendada via @Scheduled no Spring Boot. */

function fmtTime(ms: number) {
  if (ms <= 0) return 'AGORA'
  const d = Math.floor(ms / 86400000)
  const h = Math.floor((ms % 86400000) / 3600000)
  const m = Math.floor((ms % 3600000) / 60000)
  const s = Math.floor((ms % 60000) / 1000)
  if (d > 0) return `${d}d ${h}h ${m}m`
  if (h > 0) return `${h}h ${m}m ${s}s`
  if (m > 0) return `${m}m ${s}s`
  return `${s}s`
}

function localInput(iso: string | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function TimeLockedBoxesPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['boxes'], queryFn: api.boxesList, refetchInterval: 5000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const boxes = q.data?.boxes ?? []
  const [editing, setEditing] = useState<TimeBoxDto | null>(null)
  const [now, setNow] = useState(Date.now())

  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(t) }, [])

  const save = useMutation({
    mutationFn: api.boxesSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['boxes'] }); toast.ok('✓ salvo') },
  })
  const del = useMutation({ mutationFn: api.boxesDelete, onSuccess: () => qc.invalidateQueries({ queryKey: ['boxes'] }) })
  const force = useMutation({ mutationFn: api.boxesForceOpen, onSuccess: () => { qc.invalidateQueries({ queryKey: ['boxes'] }); toast.ok('⚡ unlock forçado') } })
  const reset = useMutation({ mutationFn: api.boxesReset, onSuccess: () => qc.invalidateQueries({ queryKey: ['boxes'] }) })

  function newBox() {
    setEditing({
      emoji: '🎁', name: 'Nova Caixa', description: '...',
      unlockAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      recipient: '@a',
      itemsJson: '[{"itemId":"minecraft:diamond","count":1}]',
      effectsJson: '[]',
      unlockMessage: '§e§l▣ A CAIXA SE ABRE§r',
      unlockSound: 'minecraft:block.bell.use',
      unlockParticle: 'minecraft:end_rod',
      countdownEnabled: true,
      countdownEveryMin: 30,
      countdownMsg: '§e§o[Caixa] abre em §l{time}',
      delivered: false,
    })
  }

  const sorted = [...boxes].sort((a, b) => {
    if (!!a.delivered !== !!b.delivered) return a.delivered ? 1 : -1
    return new Date(a.unlockAt).getTime() - new Date(b.unlockAt).getTime()
  })

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎁 Time-Locked Boxes</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — entrega na hora marcada, mesmo com painel fechado.
          </p>
        </div>
        <button className="btn" onClick={newBox}>+ Nova Caixa</button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {sorted.map((b) => {
          const ms = new Date(b.unlockAt).getTime() - now
          const dueIn = ms > 0
          const recName = b.recipient === '@a' ? 'Todos online' : b.recipient === '@first' ? 'Primeiro online' : (players.find((p) => p.uuid === b.recipient)?.name ?? b.recipient.slice(0, 8))
          return (
            <div key={b.id} className={`card-glow ${b.delivered ? 'opacity-60' : ''} ${dueIn && ms < 60 * 60 * 1000 ? '!border-amber-400/60 pulse-glow' : ''}`}>
              <div className="flex items-start gap-3 mb-2">
                <div className="text-5xl">{b.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold">{b.name}</div>
                  <div className="text-[10px] text-liberthia-300/60">📥 {recName}</div>
                  <div className="text-[10px] text-liberthia-300/40">{new Date(b.unlockAt).toLocaleString()}</div>
                </div>
              </div>
              {b.delivered ? (
                <div className="bg-emerald-900/30 border border-emerald-500/40 rounded p-2 text-center mb-2 text-emerald-300 font-bold">✓ ENTREGUE</div>
              ) : dueIn ? (
                <div className="bg-amber-900/30 border border-amber-500/40 rounded p-2 text-center mb-2 text-amber-300 font-bold tabular-nums">⏰ {fmtTime(ms)}</div>
              ) : (
                <div className="bg-purple-900/30 border border-purple-500/40 rounded p-2 text-center mb-2 animate-pulse text-purple-300 font-bold">⚡ PRONTA PRA ABRIR</div>
              )}
              <div className="grid grid-cols-4 gap-1">
                <button className="btn-amber btn-sm" title="Forçar" disabled={b.delivered} onClick={() => force.mutate(b.id!)}>⚡</button>
                <button className="btn-ghost btn-sm" title="Reset" onClick={() => reset.mutate(b.id!)}>↺</button>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(b)}>✎</button>
                <button className="btn-ghost btn-sm" onClick={() => del.mutate(b.id!)}>🗑</button>
              </div>
            </div>
          )
        })}
      </div>

      {editing && <BoxEditor box={editing} players={players} onSave={(b) => save.mutate(b, { onSuccess: () => setEditing(null) })} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function BoxEditor({ box, players, onSave, onCancel }: { box: TimeBoxDto; players: any[]; onSave: (b: TimeBoxDto) => void; onCancel: () => void }) {
  const [b, setB] = useState<TimeBoxDto>(box)
  const items = useItemOptions()
  const effects = useEffectOptions()
  const particles = useParticleOptions()
  const sounds = useSoundOptions()

  type Item = { itemId: string; count: number }
  type Eff = { effect: string; durationSec: number; amplifier: number }
  function parseItems(): Item[] {
    try { const r = JSON.parse(b.itemsJson); return Array.isArray(r) ? r : [] } catch { return [] }
  }
  function setItems(list: Item[]) { setB({ ...b, itemsJson: JSON.stringify(list) }) }
  function parseEffects(): Eff[] {
    try { const r = JSON.parse(b.effectsJson); return Array.isArray(r) ? r : [] } catch { return [] }
  }
  function setEffects(list: Eff[]) { setB({ ...b, effectsJson: JSON.stringify(list) }) }
  const itemList = parseItems()
  const effList = parseEffects()
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🎁 Editar Caixa</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-2">
          <input className="input text-2xl text-center" value={b.emoji} onChange={(e) => setB({ ...b, emoji: e.target.value })} />
          <input className="input font-bold" value={b.name} onChange={(e) => setB({ ...b, name: e.target.value })} />
        </div>
        <textarea className="input mb-3 text-xs" rows={2} value={b.description} onChange={(e) => setB({ ...b, description: e.target.value })} />
        <div className="grid grid-cols-2 gap-2 mb-3">
          <div>
            <label className="label">Unlock em</label>
            <input type="datetime-local" className="input" value={localInput(b.unlockAt)}
              onChange={(e) => setB({ ...b, unlockAt: new Date(e.target.value).toISOString() })} />
          </div>
          <div>
            <label className="label">Destinatário</label>
            <select className="input" value={b.recipient} onChange={(e) => setB({ ...b, recipient: e.target.value })}>
              <option value="@a">@a (todos)</option>
              <option value="@first">@first</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
          </div>
        </div>
        <label className="label">📦 Items</label>
        <div className="space-y-1 mb-3">
          {itemList.map((it, i) => (
            <div key={i} className="grid grid-cols-[1fr_70px_36px] gap-1">
              <Autocomplete value={it.itemId} onChange={(v) => setItems(itemList.map((x, j) => j === i ? { ...x, itemId: v } : x))} options={items} placeholder="minecraft:diamond" />
              <input type="number" className="input text-xs" placeholder="count" value={it.count} onChange={(e) => setItems(itemList.map((x, j) => j === i ? { ...x, count: Number(e.target.value) } : x))} />
              <button className="btn-ghost btn-sm" onClick={() => setItems(itemList.filter((_, j) => j !== i))}>🗑</button>
            </div>
          ))}
          <button className="btn-ghost btn-sm" onClick={() => setItems([...itemList, { itemId: 'minecraft:diamond', count: 1 }])}>+ Item</button>
        </div>

        <label className="label">✨ Efeitos aplicados</label>
        <div className="space-y-1 mb-3">
          {effList.map((ef, i) => (
            <div key={i} className="grid grid-cols-[1fr_70px_50px_36px] gap-1">
              <Autocomplete value={ef.effect} onChange={(v) => setEffects(effList.map((x, j) => j === i ? { ...x, effect: v } : x))} options={effects} placeholder="minecraft:luck" />
              <input type="number" className="input text-xs" placeholder="dur s" value={ef.durationSec} onChange={(e) => setEffects(effList.map((x, j) => j === i ? { ...x, durationSec: Number(e.target.value) } : x))} />
              <input type="number" className="input text-xs" placeholder="amp" value={ef.amplifier} onChange={(e) => setEffects(effList.map((x, j) => j === i ? { ...x, amplifier: Number(e.target.value) } : x))} />
              <button className="btn-ghost btn-sm" onClick={() => setEffects(effList.filter((_, j) => j !== i))}>🗑</button>
            </div>
          ))}
          <button className="btn-ghost btn-sm" onClick={() => setEffects([...effList, { effect: 'minecraft:luck', durationSec: 600, amplifier: 0 }])}>+ Efeito</button>
        </div>

        <label className="label">Mensagem ao desbloquear</label>
        <MinecraftFormatter value={b.unlockMessage} onChange={(v) => setB({ ...b, unlockMessage: v })} rows={1} maxChars={200} showCounter={false} />
        <div className="grid grid-cols-2 gap-2 mt-3 mb-3">
          <div><label className="label">Sound</label><Autocomplete value={b.unlockSound} onChange={(v) => setB({ ...b, unlockSound: v })} options={sounds} placeholder="minecraft:block.bell.use" /></div>
          <div><label className="label">Particle</label><Autocomplete value={b.unlockParticle} onChange={(v) => setB({ ...b, unlockParticle: v })} options={particles} placeholder="minecraft:end_rod" /></div>
        </div>
        <label className="flex items-center gap-2 text-xs mb-2">
          <input type="checkbox" checked={b.countdownEnabled} onChange={(e) => setB({ ...b, countdownEnabled: e.target.checked })} />
          Avisar players periodicamente
        </label>
        {b.countdownEnabled && (
          <>
            <div className="grid grid-cols-2 gap-2 mb-2">
              <div><label className="label">A cada (min)</label><input type="number" className="input" value={b.countdownEveryMin} min={5} onChange={(e) => setB({ ...b, countdownEveryMin: Number(e.target.value) })} /></div>
            </div>
            <label className="label">Mensagem ({'{time}'} = tempo restante)</label>
            <input className="input mb-3 text-xs font-mono" value={b.countdownMsg} onChange={(e) => setB({ ...b, countdownMsg: e.target.value })} />
          </>
        )}
        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(b)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
