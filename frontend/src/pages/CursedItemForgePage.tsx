import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, CurseDto, CurseBindingDto } from '../lib/api'
import { toast } from '../store/toast'
import { renderMcText, MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { useItemOptions, useEnchantmentOptions, useEffectOptions, useParticleOptions, useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Cursed Item Forge V2 — backend-driven.
 * Engine aplica curse effects no backend mesmo com página fechada.
 */

export function CursedItemForgePage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['cursed'], queryFn: api.cursedList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const curses = q.data?.curses ?? []
  const bindings = q.data?.bindings ?? []
  const [editing, setEditing] = useState<CurseDto | null>(null)

  const save = useMutation({
    mutationFn: api.cursedSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cursed'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({
    mutationFn: api.cursedDelete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cursed'] }),
  })
  const forge = useMutation({
    mutationFn: (v: { curseId: string; playerUuid: string }) => api.cursedForge(v.curseId, v.playerUuid),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cursed'] }); toast.ok('🩸 forjado') },
    onError: (e: any) => toast.err(e.message),
  })
  const unbind = useMutation({
    mutationFn: api.cursedUnbind,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cursed'] }),
  })

  function newCurse() {
    setEditing({
      emoji: '🩸', itemId: 'minecraft:bone', displayName: '§5§lNova Maldição§r',
      loreJson: '["§7§oalgo pulsa dentro."]',
      enchantmentsJson: '[{"id":"minecraft:vanishing_curse","level":1}]',
      curseEffectsJson: '[{"effect":"minecraft:nausea","durationSec":6,"amplifier":0}]',
      whispersJson: '["§4§oele te ouve."]',
      particle: 'minecraft:smoke', sound: 'minecraft:ambient.cave',
      tickIntervalSec: 30,
    })
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🩸 Cursed Item Forge</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — aplica curse effects em portadores 24/7.
          </p>
        </div>
        <button className="btn" onClick={newCurse}>+ Nova Maldição</button>
      </header>

      {bindings.length > 0 && (
        <div className="card-glow mb-4 !border-red-400/40 !bg-red-500/5">
          <h3 className="font-bold mb-3">🩸 Portadores amaldiçoados ({bindings.length})</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {bindings.map((b) => {
              const c = curses.find((x) => x.id === b.curseId)
              return (
                <div key={b.id} className="flex items-center gap-3 bg-liberthia-900/40 rounded-xl p-2">
                  <span className="text-2xl">{c?.emoji ?? '🩸'}</span>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-bold truncate">{b.playerName}</div>
                    <div className="text-[10px] text-liberthia-300/60 truncate">{c ? renderMcText(c.displayName) : b.curseId}</div>
                    <div className="text-[10px] text-liberthia-300/40">há {Math.floor((Date.now() - new Date(b.startedAt).getTime()) / 60000)}m</div>
                  </div>
                  <button className="btn-ghost btn-sm" onClick={() => unbind.mutate(b.id)}>✕</button>
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {curses.map((c) => (
          <CurseCard key={c.id} curse={c} players={players}
            onForge={(uuid) => forge.mutate({ curseId: c.id!, playerUuid: uuid })}
            onEdit={() => setEditing(c)}
            onDelete={() => del.mutate(c.id!)} />
        ))}
      </div>

      {editing && (
        <CurseEditor curse={editing}
          onSave={(c) => save.mutate(c, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function CurseCard({ curse, players, onForge, onEdit, onDelete }: {
  curse: CurseDto; players: any[]
  onForge: (uuid: string) => void
  onEdit: () => void
  onDelete: () => void
}) {
  const [target, setTarget] = useState('')
  const lore = parseArr(curse.loreJson)
  const enchants = parseArr<{id: string; level: number}>(curse.enchantmentsJson)
  const effects = parseArr<{effect: string; durationSec: number; amplifier: number}>(curse.curseEffectsJson)
  return (
    <div className="card-glow border-red-500/20">
      <div className="flex items-start gap-2 mb-2">
        <div className="text-4xl drop-shadow-[0_0_8px_rgba(239,68,68,0.5)]">{curse.emoji}</div>
        <div className="flex-1 min-w-0">
          <div className="font-bold text-sm">{renderMcText(curse.displayName)}</div>
          <div className="text-[10px] text-liberthia-300/50 font-mono truncate">{curse.itemId}</div>
        </div>
      </div>
      <div className="bg-black/40 rounded p-2 mb-2 text-[10px] space-y-0.5 max-h-20 overflow-y-auto">
        {lore.map((l, i) => <div key={i} className="italic">{renderMcText(l as string)}</div>)}
      </div>
      <div className="flex flex-wrap gap-1 mb-2 text-[10px]">
        {enchants.slice(0, 3).map((e, i) => (
          <span key={i} className="badge badge-purple">{e.id.replace('minecraft:', '')} {e.level}</span>
        ))}
        {effects.slice(0, 2).map((e, i) => (
          <span key={`f${i}`} className="badge badge-red">{e.effect.replace('minecraft:', '')} {e.amplifier}</span>
        ))}
      </div>
      <div className="flex gap-1">
        <select className="input text-xs flex-1" value={target} onChange={(e) => setTarget(e.target.value)}>
          <option value="">— player —</option>
          {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
        </select>
        <button className="btn-danger btn-sm" disabled={!target} onClick={() => onForge(target)}>🩸 Forjar</button>
      </div>
      <div className="grid grid-cols-2 gap-1 mt-2">
        <button className="btn-ghost btn-sm" onClick={onEdit}>✎ Editar</button>
        <button className="btn-ghost btn-sm" onClick={onDelete}>🗑</button>
      </div>
    </div>
  )
}

function parseArr<T = any>(json: string): T[] {
  try { const a = JSON.parse(json); return Array.isArray(a) ? a : [] } catch { return [] }
}

function CurseEditor({ curse, onSave, onCancel }: { curse: CurseDto; onSave: (c: CurseDto) => void; onCancel: () => void }) {
  const [c, setC] = useState<CurseDto>(curse)
  const items = useItemOptions()
  const enchants = useEnchantmentOptions()
  const effects = useEffectOptions()
  const particles = useParticleOptions()
  const sounds = useSoundOptions()

  const enchantList = parseArr<{ id: string; level: number }>(c.enchantmentsJson)
  const effectList = parseArr<{ effect: string; durationSec: number; amplifier: number }>(c.curseEffectsJson)

  function setEnchants(arr: { id: string; level: number }[]) { setC({ ...c, enchantmentsJson: JSON.stringify(arr) }) }
  function setEffects(arr: { effect: string; durationSec: number; amplifier: number }[]) { setC({ ...c, curseEffectsJson: JSON.stringify(arr) }) }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🩸 Editar Maldição</h3>
        <div className="grid grid-cols-[60px_2fr_2fr] gap-2 mb-3 items-end">
          <div>
            <label className="label">Emoji</label>
            <input className="input text-2xl text-center" value={c.emoji} onChange={(e) => setC({ ...c, emoji: e.target.value })} />
          </div>
          <div>
            <label className="label">Item</label>
            <Autocomplete value={c.itemId} onChange={(v) => setC({ ...c, itemId: v })} options={items} placeholder="minecraft:bone" />
          </div>
          <div>
            <label className="label">Nome (display)</label>
            <input className="input text-sm font-mono" value={c.displayName} onChange={(e) => setC({ ...c, displayName: e.target.value })} />
          </div>
        </div>
        <div className="bg-liberthia-900/40 px-2 py-1 rounded mb-3 text-xs">preview: {renderMcText(c.displayName)}</div>

        <label className="label">Lore JSON (array de strings)</label>
        <textarea className="input mb-3 text-xs font-mono" rows={3} value={c.loreJson} onChange={(e) => setC({ ...c, loreJson: e.target.value })} />

        <div className="flex items-center justify-between mb-1">
          <label className="label">Enchantments ({enchantList.length})</label>
          <button className="btn-ghost btn-sm" onClick={() => setEnchants([...enchantList, { id: 'minecraft:protection', level: 1 }])}>+ Encantamento</button>
        </div>
        <div className="space-y-1 mb-3">
          {enchantList.map((en, i) => (
            <div key={i} className="grid grid-cols-[1fr_70px_30px] gap-1">
              <Autocomplete value={en.id} onChange={(v) => setEnchants(enchantList.map((x, j) => j === i ? { ...x, id: v } : x))} options={enchants} placeholder="minecraft:..." />
              <input type="number" className="input text-xs" value={en.level} min={1} max={10} onChange={(e) => setEnchants(enchantList.map((x, j) => j === i ? { ...x, level: Number(e.target.value) } : x))} />
              <button className="btn-ghost btn-sm" onClick={() => setEnchants(enchantList.filter((_, j) => j !== i))}>🗑</button>
            </div>
          ))}
        </div>

        <div className="flex items-center justify-between mb-1">
          <label className="label">Curse Effects ({effectList.length})</label>
          <button className="btn-ghost btn-sm" onClick={() => setEffects([...effectList, { effect: 'minecraft:nausea', durationSec: 8, amplifier: 0 }])}>+ Effect</button>
        </div>
        <div className="space-y-1 mb-3">
          {effectList.map((ef, i) => (
            <div key={i} className="grid grid-cols-[1fr_60px_50px_30px] gap-1">
              <Autocomplete value={ef.effect} onChange={(v) => setEffects(effectList.map((x, j) => j === i ? { ...x, effect: v } : x))} options={effects} placeholder="minecraft:..." />
              <input type="number" className="input text-xs" placeholder="durSec" value={ef.durationSec} onChange={(e) => setEffects(effectList.map((x, j) => j === i ? { ...x, durationSec: Number(e.target.value) } : x))} />
              <input type="number" className="input text-xs" placeholder="amp" value={ef.amplifier} onChange={(e) => setEffects(effectList.map((x, j) => j === i ? { ...x, amplifier: Number(e.target.value) } : x))} />
              <button className="btn-ghost btn-sm" onClick={() => setEffects(effectList.filter((_, j) => j !== i))}>🗑</button>
            </div>
          ))}
        </div>

        <label className="label">Whispers (1 por linha §-codes)</label>
        <MinecraftFormatter
          value={parseArr<string>(c.whispersJson).join('\n')}
          onChange={(v) => setC({ ...c, whispersJson: JSON.stringify(v.split('\n').filter(Boolean)) })}
          rows={3} maxChars={500} showCounter={false} />

        <div className="grid grid-cols-3 gap-2 mt-3">
          <div><label className="label">Particle</label><Autocomplete value={c.particle} onChange={(v) => setC({ ...c, particle: v })} options={particles} placeholder="minecraft:smoke" /></div>
          <div><label className="label">Sound</label><Autocomplete value={c.sound} onChange={(v) => setC({ ...c, sound: v })} options={sounds} placeholder="minecraft:ambient.cave" /></div>
          <div><label className="label">Tick (s)</label><input type="number" className="input text-xs" value={c.tickIntervalSec} min={5} onChange={(e) => setC({ ...c, tickIntervalSec: Number(e.target.value) })} /></div>
        </div>

        <div className="flex gap-2 justify-end mt-4">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(c)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
