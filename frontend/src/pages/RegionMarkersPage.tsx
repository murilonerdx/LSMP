import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { NumInput } from '../components/NumInput'
import { useKvState } from '../lib/kvState'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Regions: bounding boxes 3D nomeados. Frontend monitora players e quando entra
 * num região executa actions configuradas (title, sound, command).
 *
 * Note: regions vivem no localStorage do operador. O monitoring roda enquanto
 * a aba está aberta — boa pra construir cenas/quest moments.
 */

type Region = {
  id: number
  name: string
  color: string // hex
  min: { x: number; y: number; z: number }
  max: { x: number; y: number; z: number }
  enabled: boolean
  cooldownSec: number
  onEnter: { command?: string; title?: string; subtitle?: string; sound?: string }
}

export function RegionMarkersPage() {
  const [regions, setRegions] = useKvState<Region[]>('regions', [])
  const [editing, setEditing] = useState<Region | null>(null)
  const [fired, setFired] = useState<{ ts: number; region: string; player: string }[]>([])
  const lastFiredRef = useRef<Map<string, number>>(new Map()) // "regionId:playerUuid" → ts
  const [insidePlayers, setInsidePlayers] = useState<Map<number, string[]>>(new Map())

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 1500 })
  const players = playersQ.data ?? []

  // Detector de entrada
  useEffect(() => {
    if (!players || players.length === 0) return
    const inside = new Map<number, string[]>()
    for (const r of regions) {
      const list: string[] = []
      for (const p of players) {
        if (isInside(p, r)) list.push(p.uuid)
      }
      inside.set(r.id, list)
    }
    // Detectar entradas
    setInsidePlayers((prev) => {
      for (const r of regions) {
        if (!r.enabled) continue
        const now = inside.get(r.id) ?? []
        const before = prev.get(r.id) ?? []
        for (const u of now) {
          if (!before.includes(u)) {
            // Entrou
            const key = `${r.id}:${u}`
            const last = lastFiredRef.current.get(key) ?? 0
            const cooldownMs = (r.cooldownSec || 30) * 1000
            if (Date.now() - last < cooldownMs) continue
            lastFiredRef.current.set(key, Date.now())
            const player = players.find((x) => x.uuid === u)
            if (!player) continue
            triggerEnter(r, player)
            setFired((f) => [{ ts: Date.now(), region: r.name, player: player.name }, ...f].slice(0, 50))
          }
        }
      }
      return inside
    })
  }, [players, regions])

  async function triggerEnter(r: Region, p: Player) {
    try {
      if (r.onEnter.title || r.onEnter.subtitle) {
        await api.title(p.uuid, r.onEnter.title ?? '', r.onEnter.subtitle ?? '')
      }
      if (r.onEnter.sound) await api.sound(p.uuid, r.onEnter.sound)
      if (r.onEnter.command) {
        await api.command(r.onEnter.command.split('{player}').join(p.name))
      }
    } catch {}
  }

  function add() {
    setEditing({
      id: Date.now(),
      name: 'Templo da Matéria',
      color: '#aa40e8',
      min: { x: 0, y: 60, z: 0 },
      max: { x: 30, y: 90, z: 30 },
      enabled: true,
      cooldownSec: 30,
      onEnter: {
        title: '§5§l⛧ Templo da Matéria ⛧',
        subtitle: '§7Você sente uma presença...',
        sound: 'minecraft:block.bell.use',
      },
    })
  }

  function commit(r: Region) {
    setRegions((cur) => {
      const i = cur.findIndex((x) => x.id === r.id)
      if (i >= 0) { const n = [...cur]; n[i] = r; return n }
      return [...cur, r]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📍 Region Markers</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Zonas 3D com triggers. Quando player entra, dispara title/som/comando. Cooldown configurável.
          </p>
        </div>
        <button className="btn" onClick={add}>+ Nova Região</button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-3">
          {regions.length === 0 && (
            <div className="card text-center py-12">
              <div className="text-5xl mb-3 opacity-50">📍</div>
              <p className="text-liberthia-300/70 text-sm">Nenhuma região definida ainda.</p>
            </div>
          )}
          {regions.map((r) => {
            const inside = insidePlayers.get(r.id) ?? []
            const insideNames = players.filter((p) => inside.includes(p.uuid)).map((p) => p.name)
            return (
              <div key={r.id} className="card-glow flex items-center gap-3" style={{ borderColor: r.color + '50' }}>
                <button
                  className={`w-10 h-6 rounded-full relative transition ${r.enabled ? 'bg-emerald-500/60' : 'bg-liberthia-700'}`}
                  onClick={() => setRegions((rs) => rs.map((x) => x.id === r.id ? { ...x, enabled: !x.enabled } : x))}
                >
                  <span className={`absolute top-1 ${r.enabled ? 'right-1' : 'left-1'} w-4 h-4 rounded-full bg-white transition-all`} />
                </button>
                <div className="w-3 h-3 rounded-full shrink-0" style={{ background: r.color, boxShadow: `0 0 12px ${r.color}` }} />
                <div className="flex-1 min-w-0">
                  <div className="font-bold">{r.name}</div>
                  <div className="text-xs text-liberthia-300/60 font-mono">
                    [{r.min.x},{r.min.y},{r.min.z}] → [{r.max.x},{r.max.y},{r.max.z}]
                    <span className="ml-2">{(r.max.x - r.min.x) * (r.max.y - r.min.y) * (r.max.z - r.min.z)} m³</span>
                  </div>
                  {insideNames.length > 0 && (
                    <div className="mt-1 flex items-center gap-1">
                      <span className="live-dot" />
                      <span className="text-xs text-emerald-300">dentro: {insideNames.join(', ')}</span>
                    </div>
                  )}
                </div>
                <span className="chip">{r.cooldownSec}s</span>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(r)}>edit</button>
                <button className="btn-ghost btn-sm" onClick={() => setRegions((rs) => rs.filter((x) => x.id !== r.id))}>🗑</button>
              </div>
            )
          })}
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">📝 Disparos recentes</h3>
          <div className="font-mono text-xs space-y-0.5 max-h-[60vh] overflow-y-auto">
            {fired.length === 0 && <div className="italic text-liberthia-300/50">— nenhum trigger ainda —</div>}
            {fired.map((f, i) => (
              <div key={i} className="text-emerald-300">
                {new Date(f.ts).toLocaleTimeString()} · §5{f.region}§r ← {f.player}
              </div>
            ))}
          </div>
        </div>
      </div>

      {editing && <RegionEditor region={editing} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function RegionEditor({ region, onSave, onCancel }: {
  region: Region;
  onSave: (r: Region) => void; onCancel: () => void;
}) {
  const [r, setR] = useState<Region>(region)
  const sounds = useSoundOptions()
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">📍 Region</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-3">
          <input className="input" placeholder="Nome" value={r.name} onChange={(e) => setR({ ...r, name: e.target.value })} />
          <input type="color" className="input h-10" value={r.color} onChange={(e) => setR({ ...r, color: e.target.value })} />
        </div>

        <div className="grid grid-cols-3 gap-2 mb-2">
          <div>
            <label className="label">min X</label>
            <NumInput value={r.min.x} onChange={(v) => setR({ ...r, min: { ...r.min, x: v } })} />
          </div>
          <div>
            <label className="label">min Y</label>
            <NumInput value={r.min.y} onChange={(v) => setR({ ...r, min: { ...r.min, y: v } })} />
          </div>
          <div>
            <label className="label">min Z</label>
            <NumInput value={r.min.z} onChange={(v) => setR({ ...r, min: { ...r.min, z: v } })} />
          </div>
        </div>
        <div className="grid grid-cols-3 gap-2 mb-1">
          <div>
            <label className="label">max X</label>
            <NumInput value={r.max.x} onChange={(v) => setR({ ...r, max: { ...r.max, x: v } })} />
          </div>
          <div>
            <label className="label">max Y</label>
            <NumInput value={r.max.y} onChange={(v) => setR({ ...r, max: { ...r.max, y: v } })} />
          </div>
          <div>
            <label className="label">max Z</label>
            <NumInput value={r.max.z} onChange={(v) => setR({ ...r, max: { ...r.max, z: v } })} />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2 mb-4 text-xs">
          <PlayerPosPicker label="📍 Capturar MIN do player" onPick={(p) => setR({ ...r, min: { x: Math.round(p.x), y: Math.round(p.y), z: Math.round(p.z) } })} />
          <PlayerPosPicker label="📍 Capturar MAX do player" onPick={(p) => setR({ ...r, max: { x: Math.round(p.x), y: Math.round(p.y), z: Math.round(p.z) } })} />
        </div>

        <div className="grid grid-cols-2 gap-2 mb-4">
          <div>
            <label className="label">Cooldown (s)</label>
            <input type="number" className="input" value={r.cooldownSec} onChange={(e) => setR({ ...r, cooldownSec: Number(e.target.value) })} />
          </div>
        </div>

        <h4 className="font-bold mb-2 mt-4">Ao entrar:</h4>
        <input className="input mb-2 text-xs font-mono" placeholder="title (opcional, suporta §)" value={r.onEnter.title ?? ''} onChange={(e) => setR({ ...r, onEnter: { ...r.onEnter, title: e.target.value } })} />
        <input className="input mb-2 text-xs font-mono" placeholder="subtitle" value={r.onEnter.subtitle ?? ''} onChange={(e) => setR({ ...r, onEnter: { ...r.onEnter, subtitle: e.target.value } })} />
        <div className="mb-2"><Autocomplete value={r.onEnter.sound ?? ''} onChange={(v) => setR({ ...r, onEnter: { ...r.onEnter, sound: v } })} options={sounds} placeholder="sound (ex: minecraft:block.bell.use)" /></div>
        <input className="input mb-4 text-xs font-mono" placeholder="comando (use {player} pro nome)" value={r.onEnter.command ?? ''} onChange={(e) => setR({ ...r, onEnter: { ...r.onEnter, command: e.target.value } })} />

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(r)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

function isInside(p: Player, r: Region): boolean {
  const x = p.position.x, y = p.position.y, z = p.position.z
  return x >= Math.min(r.min.x, r.max.x) && x <= Math.max(r.min.x, r.max.x) &&
         y >= Math.min(r.min.y, r.max.y) && y <= Math.max(r.min.y, r.max.y) &&
         z >= Math.min(r.min.z, r.max.z) && z <= Math.max(r.min.z, r.max.z)
}
