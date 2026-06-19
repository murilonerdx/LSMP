import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, RiftDto } from '../lib/api'
import { toast } from '../store/toast'
import { NumInput } from '../components/NumInput'
import { renderMcText } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useParticleOptions, useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Dimensional Rift V2 — backend-driven.
 * Engine detecta players e teleporta mesmo com página fechada.
 */

export function DimensionalRiftPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['rifts'], queryFn: api.riftsList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const rifts = q.data?.rifts ?? []
  const [editing, setEditing] = useState<RiftDto | null>(null)

  const save = useMutation({
    mutationFn: api.riftsSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['rifts'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({
    mutationFn: api.riftsDelete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rifts'] }),
  })
  const toggle = useMutation({
    mutationFn: api.riftsToggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rifts'] }),
  })

  function newRift() {
    setEditing({
      emoji: '🌀', name: 'Nova Fenda',
      posX: 0, posY: 80, posZ: 0, posDim: 'overworld', radius: 2,
      destX: 0, destY: 80, destZ: 100, destDim: 'overworld',
      randomDestRadius: 1000,
      particle: 'minecraft:portal', particleCount: 60, visualEvery: 4, ambientSound: 'minecraft:block.portal.ambient',
      preMsg: '§5§o— você sente um puxão —', postMsg: '§5§l— você não está mais onde estava —',
      preEffectsJson: '[{"effect":"minecraft:blindness","durationSec":3,"amplifier":0}]',
      postEffectsJson: '[{"effect":"minecraft:nausea","durationSec":8,"amplifier":0}]',
      cooldownSec: 30, enabled: false, visualize: true,
    })
  }

  async function testRift(r: RiftDto) {
    const p = playersQ.data?.[0]; if (!p) return toast.err('Sem player')
    try {
      await api.teleport(p.uuid, r.posX, r.posY + 1, r.posZ, r.posDim)
      toast.ok(`📍 ${p.name} tp pra rift`)
    } catch (e: any) { toast.err(e.message) }
  }

  const activeCount = rifts.filter((r) => r.enabled).length

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🌀 Dimensional Rift</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — detecta players e teleporta 24/7.
          </p>
        </div>
        <div className="flex gap-2 items-center">
          <span className="badge badge-purple">{activeCount} ativas / {rifts.length}</span>
          <button className="btn" onClick={newRift}>+ Nova Fenda</button>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {rifts.map((r) => (
          <div key={r.id} className={`card-glow ${r.enabled ? '!border-purple-400/40 !bg-purple-500/5' : ''}`}>
            <div className="flex items-start gap-2 mb-2">
              <div className={`text-4xl ${r.enabled ? 'animate-spin-slow drop-shadow-[0_0_12px_rgba(168,85,247,0.6)]' : 'opacity-40'}`}>{r.emoji}</div>
              <div className="flex-1 min-w-0">
                <div className="font-bold truncate">{r.name}</div>
                <div className="text-[10px] text-liberthia-300/60 font-mono truncate">
                  {r.posX.toFixed(0)},{r.posY.toFixed(0)},{r.posZ.toFixed(0)} ({r.posDim})
                </div>
                <div className="text-[10px] text-liberthia-300/40">
                  raio: {r.radius}b · cd: {r.cooldownSec}s · {r.triggers ?? 0} triggers
                </div>
              </div>
            </div>
            <div className="text-[10px] mb-2">
              🚀 dest: {r.destX != null && r.destY != null && r.destZ != null
                ? `${r.destX.toFixed(0)},${r.destY.toFixed(0)},${r.destZ.toFixed(0)} (${r.destDim})`
                : `🎲 random ${r.randomDestRadius}b`}
            </div>
            <div className="text-[10px] italic text-purple-300 mb-2 line-clamp-2">{renderMcText(r.preMsg)}</div>
            <div className="grid grid-cols-4 gap-1">
              <button className={r.enabled ? 'btn-success btn-sm' : 'btn-ghost btn-sm'}
                onClick={() => toggle.mutate(r.id!)}>
                {r.enabled ? '⏸' : '▶'}
              </button>
              <button className="btn-ghost btn-sm" title="TP me pra cá" onClick={() => testRift(r)}>📍</button>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(r)}>✎</button>
              <button className="btn-ghost btn-sm" onClick={() => del.mutate(r.id!)}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <RiftEditor rift={editing}
          onSave={(r) => save.mutate(r, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function RiftEditor({ rift, onSave, onCancel }: { rift: RiftDto; onSave: (r: RiftDto) => void; onCancel: () => void }) {
  const [r, setR] = useState<RiftDto>(rift)
  const [randomDest, setRandomDest] = useState(rift.destX == null)
  const particles = useParticleOptions()
  const sounds = useSoundOptions()
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🌀 Editar Fenda</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-4">
          <input className="input text-2xl text-center" value={r.emoji} onChange={(e) => setR({ ...r, emoji: e.target.value })} />
          <input className="input font-bold" value={r.name} onChange={(e) => setR({ ...r, name: e.target.value })} />
        </div>

        <h4 className="font-bold text-sm mb-1">📍 Posição</h4>
        <div className="grid grid-cols-4 gap-2 mb-2">
          <div><label className="label">X</label><NumInput value={r.posX} onChange={(v) => setR({ ...r, posX: v })} /></div>
          <div><label className="label">Y</label><NumInput value={r.posY} onChange={(v) => setR({ ...r, posY: v })} /></div>
          <div><label className="label">Z</label><NumInput value={r.posZ} onChange={(v) => setR({ ...r, posZ: v })} /></div>
          <div><label className="label">Dim</label>
            <select className="input text-xs" value={r.posDim} onChange={(e) => setR({ ...r, posDim: e.target.value })}>
              <option value="overworld">overworld</option>
              <option value="the_nether">nether</option>
              <option value="the_end">end</option>
            </select>
          </div>
        </div>
        <div className="mb-3"><PlayerPosPicker onPick={(p) => setR({ ...r, posX: Math.floor(p.x), posY: Math.floor(p.y), posZ: Math.floor(p.z), posDim: p.dim })} /></div>

        <div className="grid grid-cols-2 gap-2 mb-3">
          <div><label className="label">Raio</label><NumInput value={r.radius} onChange={(v) => setR({ ...r, radius: v })} /></div>
          <div><label className="label">Cooldown (s)</label><NumInput value={r.cooldownSec} onChange={(v) => setR({ ...r, cooldownSec: v })} /></div>
        </div>

        <h4 className="font-bold text-sm mb-1">🚀 Destino</h4>
        <label className="flex items-center gap-2 text-xs mb-2">
          <input type="checkbox" checked={randomDest}
            onChange={(e) => {
              setRandomDest(e.target.checked)
              if (e.target.checked) setR({ ...r, destX: null, destY: null, destZ: null, destDim: null })
              else setR({ ...r, destX: r.posX + 100, destY: r.posY, destZ: r.posZ, destDim: r.posDim })
            }} />
          🎲 Destino aleatório em raio
        </label>
        {randomDest ? (
          <div className="mb-3"><label className="label">Raio aleatório</label><NumInput value={r.randomDestRadius} onChange={(v) => setR({ ...r, randomDestRadius: v })} /></div>
        ) : (
          <>
            <div className="grid grid-cols-4 gap-2 mb-2">
              <div><label className="label">X</label><NumInput value={r.destX ?? 0} onChange={(v) => setR({ ...r, destX: v })} /></div>
              <div><label className="label">Y</label><NumInput value={r.destY ?? 80} onChange={(v) => setR({ ...r, destY: v })} /></div>
              <div><label className="label">Z</label><NumInput value={r.destZ ?? 0} onChange={(v) => setR({ ...r, destZ: v })} /></div>
              <div><label className="label">Dim</label>
                <select className="input text-xs" value={r.destDim ?? 'overworld'} onChange={(e) => setR({ ...r, destDim: e.target.value })}>
                  <option value="overworld">overworld</option>
                  <option value="the_nether">nether</option>
                  <option value="the_end">end</option>
                </select>
              </div>
            </div>
            <div className="mb-3"><PlayerPosPicker label="📍 Destino do player..." onPick={(p) => setR({ ...r, destX: Math.floor(p.x), destY: Math.floor(p.y), destZ: Math.floor(p.z), destDim: p.dim })} /></div>
          </>
        )}

        <h4 className="font-bold text-sm mb-1">🎨 Visual</h4>
        <div className="grid grid-cols-3 gap-2 mb-3">
          <div><label className="label">Particle</label><Autocomplete value={r.particle} onChange={(v) => setR({ ...r, particle: v })} options={particles} placeholder="minecraft:portal" /></div>
          <div><label className="label">Count</label><NumInput value={r.particleCount} onChange={(v) => setR({ ...r, particleCount: v })} /></div>
          <div><label className="label">Visual (s)</label><NumInput value={r.visualEvery} onChange={(v) => setR({ ...r, visualEvery: v })} /></div>
        </div>
        <label className="label">Som ambient</label>
        <div className="mb-3"><Autocomplete value={r.ambientSound} onChange={(v) => setR({ ...r, ambientSound: v })} options={sounds} placeholder="minecraft:block.portal.ambient" /></div>
        <label className="flex items-center gap-2 text-xs mb-3">
          <input type="checkbox" checked={r.visualize} onChange={(e) => setR({ ...r, visualize: e.target.checked })} /> Renderizar vortex visível
        </label>

        <div className="grid grid-cols-2 gap-2 mb-3">
          <div><label className="label">Pré-tp msg</label><input className="input text-xs font-mono" value={r.preMsg} onChange={(e) => setR({ ...r, preMsg: e.target.value })} /></div>
          <div><label className="label">Pós-tp msg</label><input className="input text-xs font-mono" value={r.postMsg} onChange={(e) => setR({ ...r, postMsg: e.target.value })} /></div>
        </div>

        <label className="label">Pre-effects JSON</label>
        <textarea className="input mb-3 text-xs font-mono" rows={2} value={r.preEffectsJson} onChange={(e) => setR({ ...r, preEffectsJson: e.target.value })} />
        <label className="label">Post-effects JSON</label>
        <textarea className="input mb-3 text-xs font-mono" rows={2} value={r.postEffectsJson} onChange={(e) => setR({ ...r, postEffectsJson: e.target.value })} />

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(r)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
