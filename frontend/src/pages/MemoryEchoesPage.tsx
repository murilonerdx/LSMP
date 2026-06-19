import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, MemoryEchoDto } from '../lib/api'
import { toast } from '../store/toast'
import { NumInput } from '../components/NumInput'
import { MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useParticleOptions, useSoundOptions } from '../lib/mcAutocomplete'

/** MemoryEchoes V2 — backend-driven. */

type Ghost = { playerName: string; offset: { x: number; y: number; z: number }; rotation: number; showArms: boolean }

export function MemoryEchoesPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['memory-echoes'], queryFn: api.memoryEchoesList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 3000 })
  const players = playersQ.data ?? []
  const echoes = q.data?.echoes ?? []
  const active = q.data?.active ?? []
  const [editing, setEditing] = useState<MemoryEchoDto | null>(null)

  const save = useMutation({
    mutationFn: api.memoryEchoesSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['memory-echoes'] }); toast.ok('✓ salvo') },
  })
  const del = useMutation({ mutationFn: api.memoryEchoesDelete, onSuccess: () => qc.invalidateQueries({ queryKey: ['memory-echoes'] }) })
  const toggle = useMutation({ mutationFn: api.memoryEchoesToggle, onSuccess: () => qc.invalidateQueries({ queryKey: ['memory-echoes'] }) })
  const purge = useMutation({ mutationFn: api.memoryEchoesPurge, onSuccess: () => { qc.invalidateQueries({ queryKey: ['memory-echoes'] }); toast.ok('🧹 limpo') } })

  function parseGhosts(json: string): Ghost[] {
    try { const a = JSON.parse(json); return Array.isArray(a) ? a : [] } catch { return [] }
  }

  function newEcho() {
    setEditing({
      emoji: '🕯', name: 'Nova Memória', color: '#a78bfa',
      posX: 0, posY: 80, posZ: 0, posDim: 'overworld', radius: 5,
      ghostsJson: '[{"playerName":"Steve","offset":{"x":2,"y":0,"z":0},"rotation":180,"showArms":true}]',
      whispersJson: '["§7§o— alguém estava aqui —"]',
      whisperEverySec: 5, ambientSound: 'minecraft:ambient.cave', ambientParticle: 'minecraft:sculk_soul',
      playDurationSec: 30, cooldownSec: 120, enabled: false,
    })
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🕯 Memory Echoes</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — spawn de fantasmas 24/7.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost btn-sm" onClick={() => purge.mutate()}>🧹</button>
          <button className="btn" onClick={newEcho}>+ Nova Echo</button>
        </div>
      </header>

      {active.length > 0 && (
        <div className="card-glow mb-4 !border-purple-400/40 !bg-purple-500/5">
          <h3 className="font-bold mb-2">🎭 Echoes ativas ({active.length})</h3>
          <div className="space-y-1">
            {active.map((a) => {
              const e = echoes.find((x) => x.id === a.echoId)
              const p = players.find((x) => x.uuid === a.playerUuid)
              const remaining = Math.max(0, Math.ceil((new Date(a.endsAt).getTime() - Date.now()) / 1000))
              if (!e) return null
              return (
                <div key={a.id} className="flex items-center gap-3 bg-liberthia-900/40 rounded-xl p-2">
                  <div className="text-2xl">{e.emoji}</div>
                  <div className="flex-1">
                    <div className="text-sm font-bold">{e.name} → {p?.name ?? '???'}</div>
                    <div className="text-[10px] text-liberthia-300/50">restante: {remaining}s</div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {echoes.map((e) => {
          const ghosts = parseGhosts(e.ghostsJson)
          return (
            <div key={e.id} className="card-glow" style={{ borderColor: e.enabled ? e.color : undefined, boxShadow: e.enabled ? `0 0 24px -8px ${e.color}` : undefined }}>
              <div className="flex items-start gap-3 mb-2">
                <div className="text-5xl" style={{ filter: `drop-shadow(0 0 8px ${e.color})` }}>{e.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold" style={{ color: e.color }}>{e.name}</div>
                  <div className="text-[10px] text-liberthia-300/60 font-mono">{e.posX},{e.posY},{e.posZ} ({e.posDim})</div>
                  <div className="text-[10px] text-liberthia-300/40">raio {e.radius}b · {ghosts.length} fantasmas · play {e.playDurationSec}s</div>
                </div>
              </div>
              <div className="flex flex-wrap gap-1 mb-2 text-[10px]">
                {ghosts.slice(0, 5).map((g, i) => (
                  <span key={i} className="badge badge-purple flex items-center gap-1">
                    <img src={`https://mc-heads.net/avatar/${encodeURIComponent(g.playerName)}/12`} className="rounded shrink-0" onError={(ev) => { (ev.target as HTMLImageElement).style.display = 'none' }} />
                    {g.playerName}
                  </span>
                ))}
              </div>
              <div className="grid grid-cols-3 gap-1">
                <button className={e.enabled ? 'btn-success btn-sm' : 'btn-ghost btn-sm'} onClick={() => toggle.mutate(e.id!)}>{e.enabled ? '⏸' : '▶'}</button>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(e)}>✎</button>
                <button className="btn-ghost btn-sm" onClick={() => del.mutate(e.id!)}>🗑</button>
              </div>
            </div>
          )
        })}
      </div>

      {editing && <EchoEditor echo={editing} players={players} onSave={(e) => save.mutate(e, { onSuccess: () => setEditing(null) })} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function EchoEditor({ echo, players, onSave, onCancel }: { echo: MemoryEchoDto; players: any[]; onSave: (e: MemoryEchoDto) => void; onCancel: () => void }) {
  const [e, setE] = useState<MemoryEchoDto>(echo)
  const sounds = useSoundOptions()
  const particles = useParticleOptions()
  function parseGhosts(): Ghost[] {
    try { const a = JSON.parse(e.ghostsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  }
  function parseWhispers(): string[] {
    try { const a = JSON.parse(e.whispersJson); return Array.isArray(a) ? a : [] } catch { return [] }
  }
  function setGhosts(g: Ghost[]) {
    setE({ ...e, ghostsJson: JSON.stringify(g) })
  }
  const ghosts = parseGhosts()
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[92vh] overflow-y-auto" onClick={(ev) => ev.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🕯 Editar Memory Echo</h3>
        <div className="grid grid-cols-[60px_1fr_80px] gap-2 mb-2">
          <input className="input text-2xl text-center" value={e.emoji} onChange={(ev) => setE({ ...e, emoji: ev.target.value })} />
          <input className="input font-bold" value={e.name} onChange={(ev) => setE({ ...e, name: ev.target.value })} />
          <input type="color" className="input h-9" value={e.color} onChange={(ev) => setE({ ...e, color: ev.target.value })} />
        </div>
        <div className="grid grid-cols-4 gap-2 mb-2">
          <div><label className="label">X</label><NumInput value={e.posX} onChange={(v) => setE({ ...e, posX: v })} /></div>
          <div><label className="label">Y</label><NumInput value={e.posY} onChange={(v) => setE({ ...e, posY: v })} /></div>
          <div><label className="label">Z</label><NumInput value={e.posZ} onChange={(v) => setE({ ...e, posZ: v })} /></div>
          <div><label className="label">Dim</label>
            <select className="input text-xs" value={e.posDim} onChange={(ev) => setE({ ...e, posDim: ev.target.value })}>
              <option value="overworld">overworld</option><option value="the_nether">nether</option><option value="the_end">end</option>
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 mb-3">
          <div><label className="label">Raio</label><NumInput value={e.radius} onChange={(v) => setE({ ...e, radius: v })} /></div>
          <div>
            <label className="label">Quick: pos do player</label>
            <PlayerPosPicker onPick={(p) => setE({ ...e, posX: Math.floor(p.x), posY: Math.floor(p.y), posZ: Math.floor(p.z), posDim: p.dim })} />
          </div>
        </div>

        <h4 className="font-bold text-sm mb-1">👻 Fantasmas ({ghosts.length})</h4>
        <div className="flex gap-1 mb-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => setGhosts([...ghosts, { playerName: 'Steve', offset: { x: 0, y: 0, z: 0 }, rotation: 0, showArms: true }])}>+ Steve</button>
          <button className="btn-ghost btn-sm" onClick={() => setGhosts([...ghosts, { playerName: 'Alex', offset: { x: 0, y: 0, z: 0 }, rotation: 0, showArms: true }])}>+ Alex</button>
          <select className="input text-xs flex-1" value="" onChange={(ev) => {
            const p = players.find((x) => x.uuid === ev.target.value); if (!p) return
            const off = { x: Math.floor(p.position.x) - e.posX, y: Math.floor(p.position.y) - e.posY, z: Math.floor(p.position.z) - e.posZ }
            setGhosts([...ghosts, { playerName: p.name, offset: off, rotation: 0, showArms: true }])
          }}>
            <option value="">📸 capturar online...</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
          </select>
        </div>
        <div className="space-y-1 mb-3">
          {ghosts.map((g, i) => (
            <div key={i} className="bg-liberthia-900/40 rounded p-2">
              <div className="flex items-center gap-2 mb-1">
                <input className="input text-xs flex-1" value={g.playerName} onChange={(ev) => setGhosts(ghosts.map((x, j) => j === i ? { ...x, playerName: ev.target.value } : x))} />
                <button className="btn-ghost btn-sm" onClick={() => setGhosts(ghosts.filter((_, j) => j !== i))}>🗑</button>
              </div>
              <div className="grid grid-cols-5 gap-1 text-xs">
                <input type="number" className="input text-xs" placeholder="offX" value={g.offset.x} onChange={(ev) => setGhosts(ghosts.map((x, j) => j === i ? { ...x, offset: { ...x.offset, x: Number(ev.target.value) } } : x))} />
                <input type="number" className="input text-xs" placeholder="offY" value={g.offset.y} onChange={(ev) => setGhosts(ghosts.map((x, j) => j === i ? { ...x, offset: { ...x.offset, y: Number(ev.target.value) } } : x))} />
                <input type="number" className="input text-xs" placeholder="offZ" value={g.offset.z} onChange={(ev) => setGhosts(ghosts.map((x, j) => j === i ? { ...x, offset: { ...x.offset, z: Number(ev.target.value) } } : x))} />
                <input type="number" className="input text-xs" placeholder="rot°" value={g.rotation} onChange={(ev) => setGhosts(ghosts.map((x, j) => j === i ? { ...x, rotation: Number(ev.target.value) } : x))} />
                <label className="flex items-center gap-1 text-[10px]"><input type="checkbox" checked={g.showArms} onChange={(ev) => setGhosts(ghosts.map((x, j) => j === i ? { ...x, showArms: ev.target.checked } : x))} /> arms</label>
              </div>
            </div>
          ))}
        </div>

        <h4 className="font-bold text-sm mb-1">💬 Whispers</h4>
        <MinecraftFormatter value={parseWhispers().join('\n')} onChange={(v) => setE({ ...e, whispersJson: JSON.stringify(v.split('\n').filter(Boolean)) })} rows={4} maxChars={500} showCounter={false} />

        <div className="grid grid-cols-2 gap-2 mt-3 mb-3">
          <div><label className="label">Som ambient</label><Autocomplete value={e.ambientSound} onChange={(v) => setE({ ...e, ambientSound: v })} options={sounds} placeholder="minecraft:ambient.cave" /></div>
          <div><label className="label">Particle</label><Autocomplete value={e.ambientParticle} onChange={(v) => setE({ ...e, ambientParticle: v })} options={particles} placeholder="minecraft:sculk_soul" /></div>
          <div><label className="label">Whisper (s)</label><NumInput value={e.whisperEverySec} onChange={(v) => setE({ ...e, whisperEverySec: v })} /></div>
          <div><label className="label">Duração play (s)</label><NumInput value={e.playDurationSec} onChange={(v) => setE({ ...e, playDurationSec: v })} /></div>
          <div className="col-span-2"><label className="label">Cooldown (s)</label><NumInput value={e.cooldownSec} onChange={(v) => setE({ ...e, cooldownSec: v })} /></div>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(e)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
