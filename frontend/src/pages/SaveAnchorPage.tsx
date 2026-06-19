import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, SaveAnchorDto } from '../lib/api'
import { toast } from '../store/toast'
import { NumInput } from '../components/NumInput'
import { PlayerPosPicker } from '../components/PlayerPosPicker'

/** Save Anchors V2 — backend-driven. */

export function SaveAnchorPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['anchors'], queryFn: api.anchorsList, refetchInterval: 5000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 2000 })
  const players = playersQ.data ?? []
  const anchors = q.data?.anchors ?? []
  const [editing, setEditing] = useState<SaveAnchorDto | null>(null)

  const save = useMutation({
    mutationFn: api.anchorsSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['anchors'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({ mutationFn: api.anchorsDelete, onSuccess: () => qc.invalidateQueries({ queryKey: ['anchors'] }) })
  const toggle = useMutation({ mutationFn: api.anchorsToggle, onSuccess: () => qc.invalidateQueries({ queryKey: ['anchors'] }) })
  const resetUses = useMutation({ mutationFn: api.anchorsResetUses, onSuccess: () => qc.invalidateQueries({ queryKey: ['anchors'] }) })

  function newAnchor() {
    setEditing({
      emoji: '⚓',
      name: 'Novo Anchor',
      description: '',
      posX: 0, posY: 64, posZ: 0, posDim: 'overworld',
      radius: 3,
      usesJson: '[]',
      enabled: false,
    })
  }

  function parseUses(a: SaveAnchorDto): string[] {
    try { const r = JSON.parse(a.usesJson); return Array.isArray(r) ? r : [] } catch { return [] }
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⚓ Save Anchors</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Checkpoints estilo Dark Souls. Player chega → spawnpoint + cura + cena.
            <span className="text-emerald-300 font-bold"> Engine no backend, persistido em PostgreSQL.</span>
          </p>
        </div>
        <button className="btn" onClick={newAnchor}>+ Novo Anchor</button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {anchors.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">⚓</div>
            <p className="text-liberthia-300/70 text-sm">Nenhum anchor ainda.</p>
          </div>
        )}
        {anchors.map((a) => {
          const uses = parseUses(a)
          return (
            <div key={a.id} className={`card-glow ${a.enabled ? '!border-amber-400/40' : ''}`}>
              <div className="flex items-start gap-3 mb-2">
                <div className="text-5xl">{a.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold">{a.name}</div>
                  <div className="text-[10px] text-liberthia-300/60 font-mono">
                    {a.posX.toFixed(0)},{a.posY.toFixed(0)},{a.posZ.toFixed(0)} ({a.posDim})
                  </div>
                  <div className="text-[10px] text-liberthia-300/40">raio {a.radius}b · {uses.length} usos</div>
                </div>
              </div>
              {a.description && <p className="text-xs text-liberthia-300/60 mb-2 line-clamp-2">{a.description}</p>}
              {uses.length > 0 && (
                <div className="text-[10px] text-emerald-300/70 mb-2 truncate">
                  ✓ {uses.map((u) => players.find((p) => p.uuid === u)?.name ?? u.slice(0, 6)).join(', ')}
                </div>
              )}
              <div className="grid grid-cols-4 gap-1">
                <button className={a.enabled ? 'btn-success btn-sm' : 'btn-amber btn-sm pulse-glow'}
                  onClick={() => toggle.mutate(a.id!)}>
                  {a.enabled ? '⏸ ATIVA' : '▶ ATIVAR'}
                </button>
                <button className="btn-ghost btn-sm" title="Reset usos" onClick={() => { if (confirm('Reset usos?')) resetUses.mutate(a.id!) }}>↺</button>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(a)}>✎</button>
                <button className="btn-ghost btn-sm" onClick={() => del.mutate(a.id!)}>🗑</button>
              </div>
            </div>
          )
        })}
      </div>

      {editing && (
        <AnchorEditor anchor={editing} players={players}
          onSave={(a) => save.mutate(a, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function AnchorEditor({ anchor, players, onSave, onCancel }: { anchor: SaveAnchorDto; players: any[]; onSave: (a: SaveAnchorDto) => void; onCancel: () => void }) {
  const [a, setA] = useState<SaveAnchorDto>(anchor)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⚓ Editar Save Anchor</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-2">
          <input className="input text-2xl text-center" value={a.emoji} onChange={(e) => setA({ ...a, emoji: e.target.value })} />
          <input className="input font-bold" value={a.name} onChange={(e) => setA({ ...a, name: e.target.value })} />
        </div>
        <textarea className="input mb-3 text-xs" rows={2} value={a.description} onChange={(e) => setA({ ...a, description: e.target.value })} placeholder="descrição" />

        <h4 className="font-bold text-sm mb-1">📍 Posição</h4>
        <div className="grid grid-cols-4 gap-2 mb-2">
          <div><label className="label">X</label><NumInput value={a.posX} onChange={(v) => setA({ ...a, posX: v })} /></div>
          <div><label className="label">Y</label><NumInput value={a.posY} onChange={(v) => setA({ ...a, posY: v })} /></div>
          <div><label className="label">Z</label><NumInput value={a.posZ} onChange={(v) => setA({ ...a, posZ: v })} /></div>
          <div><label className="label">Dim</label>
            <select className="input text-xs" value={a.posDim} onChange={(e) => setA({ ...a, posDim: e.target.value })}>
              <option value="overworld">overworld</option>
              <option value="the_nether">nether</option>
              <option value="the_end">end</option>
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 mb-3">
          <div><label className="label">Raio</label><NumInput value={a.radius} onChange={(v) => setA({ ...a, radius: v })} /></div>
          <div>
            <label className="label">Quick: pos do player</label>
            <PlayerPosPicker onPick={(p) => setA({ ...a, posX: Math.floor(p.x), posY: Math.floor(p.y), posZ: Math.floor(p.z), posDim: p.dim })} />
          </div>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(a)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
