import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, GlyphDto } from '../lib/api'
import { toast } from '../store/toast'
import { NumInput } from '../components/NumInput'
import { renderMcText, MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useParticleOptions, useSoundOptions } from '../lib/mcAutocomplete'

/** Glyph Discovery V2 — backend-driven. */
export function GlyphDiscoveryPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['glyphs'], queryFn: api.glyphsList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const glyphs = q.data?.glyphs ?? []
  const [editing, setEditing] = useState<GlyphDto | null>(null)
  const [tab, setTab] = useState<'glyphs' | 'leaderboard'>('glyphs')

  const save = useMutation({
    mutationFn: api.glyphsSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['glyphs'] }); toast.ok('✓ salvo') },
  })
  const del = useMutation({ mutationFn: api.glyphsDelete, onSuccess: () => qc.invalidateQueries({ queryKey: ['glyphs'] }) })
  const toggle = useMutation({ mutationFn: api.glyphsToggle, onSuccess: () => qc.invalidateQueries({ queryKey: ['glyphs'] }) })
  const resetDisc = useMutation({ mutationFn: api.glyphsResetDiscoveries, onSuccess: () => qc.invalidateQueries({ queryKey: ['glyphs'] }) })

  function newGlyph() {
    setEditing({
      symbol: '✦', emoji: '✦', name: '§5§lNovo Glifo§r',
      posX: 0, posY: 80, posZ: 0, posDim: 'overworld', radius: 2,
      loreFragment: '§7§o"..."§r',
      rewardsJson: '[{"type":"effect","effect":"minecraft:luck","durationSec":600,"amplifier":0}]',
      discoverSound: 'minecraft:block.bell.use', discoverParticle: 'minecraft:end_rod',
      hintParticle: 'minecraft:soul_fire_flame', hintEvery: 5, hintCount: 6,
      discoveredByJson: '[]', enabled: false,
    })
  }

  function parseUuids(g: GlyphDto): string[] {
    try { return JSON.parse(g.discoveredByJson) as string[] } catch { return [] }
  }

  const lb = (() => {
    const counts: Record<string, { name: string; count: number }> = {}
    for (const g of glyphs) for (const uuid of parseUuids(g)) {
      if (!counts[uuid]) {
        const p = players.find((x) => x.uuid === uuid)
        counts[uuid] = { name: p?.name ?? uuid.slice(0, 8), count: 0 }
      }
      counts[uuid].count++
    }
    return Object.entries(counts).map(([uuid, v]) => ({ uuid, ...v })).sort((a, b) => b.count - a.count)
  })()

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">✦ Glyph Discovery</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — detecta descobertas 24/7.
          </p>
        </div>
        <button className="btn" onClick={newGlyph}>+ Novo Glifo</button>
      </header>

      <div className="flex gap-2 mb-3">
        <button className={tab === 'glyphs' ? 'tab-item active' : 'tab-item'} onClick={() => setTab('glyphs')}>✦ Glifos ({glyphs.length})</button>
        <button className={tab === 'leaderboard' ? 'tab-item active' : 'tab-item'} onClick={() => setTab('leaderboard')}>🏆 Leaderboard</button>
      </div>

      {tab === 'glyphs' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {glyphs.map((g) => {
            const discovered = parseUuids(g)
            return (
              <div key={g.id} className={`card-glow ${g.enabled ? '!border-purple-400/40 !bg-purple-500/5' : ''}`}>
                <div className="flex items-start gap-3 mb-2">
                  <div className={`text-5xl ${g.enabled ? 'animate-pulse' : 'opacity-50'}`}>{g.symbol}</div>
                  <div className="flex-1 min-w-0">
                    <div className="font-bold">{renderMcText(g.name)}</div>
                    <div className="text-[10px] text-liberthia-300/60 font-mono">{g.posX},{g.posY},{g.posZ} ({g.posDim})</div>
                    <div className="text-[10px] text-liberthia-300/40">raio {g.radius}b · {discovered.length} desc.</div>
                  </div>
                </div>
                <div className="text-[11px] italic text-purple-300 mb-2 line-clamp-2">{renderMcText(g.loreFragment)}</div>
                <div className="grid grid-cols-4 gap-1">
                  <button className={g.enabled ? 'btn-success btn-sm' : 'btn-ghost btn-sm'} onClick={() => toggle.mutate(g.id!)}>{g.enabled ? '⏸' : '▶'}</button>
                  <button className="btn-ghost btn-sm" title="Reset descobertas" onClick={() => { if (confirm('Reset?')) resetDisc.mutate(g.id!) }}>↺</button>
                  <button className="btn-ghost btn-sm" onClick={() => setEditing(g)}>✎</button>
                  <button className="btn-ghost btn-sm" onClick={() => del.mutate(g.id!)}>🗑</button>
                </div>
              </div>
            )
          })}
        </div>
      ) : (
        <div className="card-glow">
          <h3 className="font-bold mb-2">🏆 Descobridores</h3>
          {lb.length === 0 ? <p className="text-xs italic text-liberthia-300/50">Ninguém descobriu glifos.</p> : (
            <div className="space-y-1">
              {lb.map((row, i) => (
                <div key={row.uuid} className="flex items-center gap-2 bg-liberthia-900/40 rounded px-2 py-1.5">
                  <span className="text-xl w-8 text-center">{i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : `#${i + 1}`}</span>
                  <span className="font-bold flex-1">{row.name}</span>
                  <span className="text-2xl font-black gradient-text">{row.count}</span>
                  <span className="text-[10px] text-liberthia-300/50">/ {glyphs.length}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {editing && <GlyphEditor glyph={editing} players={players} onSave={(g) => save.mutate(g, { onSuccess: () => setEditing(null) })} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function GlyphEditor({ glyph, onSave, onCancel }: { glyph: GlyphDto; players: any[]; onSave: (g: GlyphDto) => void; onCancel: () => void }) {
  const [g, setG] = useState<GlyphDto>(glyph)
  const particles = useParticleOptions()
  const sounds = useSoundOptions()
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">✦ Editar Glifo</h3>
        <div className="grid grid-cols-[60px_60px_1fr] gap-2 mb-3">
          <input className="input text-2xl text-center" value={g.symbol} onChange={(e) => setG({ ...g, symbol: e.target.value })} />
          <input className="input text-2xl text-center" value={g.emoji} onChange={(e) => setG({ ...g, emoji: e.target.value })} />
          <input className="input font-mono" value={g.name} onChange={(e) => setG({ ...g, name: e.target.value })} />
        </div>
        <div className="grid grid-cols-4 gap-2 mb-2">
          <div><label className="label">X</label><NumInput value={g.posX} onChange={(v) => setG({ ...g, posX: v })} /></div>
          <div><label className="label">Y</label><NumInput value={g.posY} onChange={(v) => setG({ ...g, posY: v })} /></div>
          <div><label className="label">Z</label><NumInput value={g.posZ} onChange={(v) => setG({ ...g, posZ: v })} /></div>
          <div><label className="label">Dim</label>
            <select className="input text-xs" value={g.posDim} onChange={(e) => setG({ ...g, posDim: e.target.value })}>
              <option value="overworld">overworld</option><option value="the_nether">nether</option><option value="the_end">end</option>
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 mb-3">
          <div><label className="label">Raio</label><NumInput value={g.radius} onChange={(v) => setG({ ...g, radius: v })} /></div>
          <div>
            <label className="label">Quick: pos do player</label>
            <PlayerPosPicker onPick={(p) => setG({ ...g, posX: Math.floor(p.x), posY: Math.floor(p.y), posZ: Math.floor(p.z), posDim: p.dim })} />
          </div>
        </div>

        <label className="label">Fragmento de lore</label>
        <MinecraftFormatter value={g.loreFragment} onChange={(v) => setG({ ...g, loreFragment: v })} rows={2} maxChars={300} showCounter={false} />

        <div className="grid grid-cols-2 gap-2 mt-3 mb-3">
          <div><label className="label">Hint particle</label><Autocomplete value={g.hintParticle} onChange={(v) => setG({ ...g, hintParticle: v })} options={particles} placeholder="minecraft:soul_fire_flame" /></div>
          <div><label className="label">Discover particle</label><Autocomplete value={g.discoverParticle} onChange={(v) => setG({ ...g, discoverParticle: v })} options={particles} placeholder="minecraft:end_rod" /></div>
          <div><label className="label">Hint a cada (s)</label><NumInput value={g.hintEvery} onChange={(v) => setG({ ...g, hintEvery: v })} /></div>
          <div><label className="label">Hint count</label><NumInput value={g.hintCount} onChange={(v) => setG({ ...g, hintCount: v })} /></div>
          <div className="col-span-2"><label className="label">Som descoberta</label><Autocomplete value={g.discoverSound} onChange={(v) => setG({ ...g, discoverSound: v })} options={sounds} placeholder="minecraft:block.bell.use" /></div>
        </div>

        <label className="label">Rewards JSON (array de {`{type, ...}`})</label>
        <textarea className="input mb-4 text-xs font-mono" rows={4} value={g.rewardsJson} onChange={(e) => setG({ ...g, rewardsJson: e.target.value })} />
        <div className="text-[10px] text-liberthia-300/50 mb-3">
          Tipos: <code>item: {`{type:"item",itemId,count}`}</code> · <code>effect: {`{type:"effect",effect,durationSec,amplifier}`}</code> · <code>title: {`{type:"title",title,subtitle}`}</code> · <code>command: {`{type:"command",command}`}</code>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(g)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
