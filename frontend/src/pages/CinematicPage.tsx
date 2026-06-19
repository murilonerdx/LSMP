import { useRef, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { NumInput } from '../components/NumInput'
import { toast } from '../store/toast'
import { Autocomplete } from '../components/Autocomplete'
import { useSoundOptions, useParticleOptions } from '../lib/mcAutocomplete'

/**
 * Cutscene editor: lista de waypoints {x,y,z, durationMs, title?}.
 * Ao tocar:
 *   1. Coloca player em spectator
 *   2. Para cada waypoint, anima posição via TP em 60fps (interpolação linear)
 *   3. Mostra title se definido
 *   4. No final volta pra survival
 */

type Waypoint = {
  x: number; y: number; z: number;
  yaw?: number; pitch?: number;
  durationMs: number;
  title?: string;
  subtitle?: string;
  sound?: string;
  particle?: string;
}


type Cutscene = { name: string; waypoints: Waypoint[]; updated: number }

export function CinematicPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const sounds = useSoundOptions()
  const particles = useParticleOptions()
  const [target, setTarget] = useState<string>('')
  const [name, setName] = useState('cutscene1')
  const [waypoints, setWaypoints] = useState<Waypoint[]>([])
  const [running, setRunning] = useState(false)
  const [progress, setProgress] = useState(0)
  const cancelRef = useRef(false)
  const [saved, setSaved] = useKvState<Cutscene[]>('cinematics', [])

  function persist(list: Cutscene[]) {
    setSaved(list)
  }

  function save() {
    const item: Cutscene = { name, waypoints, updated: Date.now() }
    const next = [item, ...saved.filter((s) => s.name !== item.name)]
    persist(next)
    toast.ok(`Saved: ${name}`)
  }

  function load(c: Cutscene) {
    setName(c.name); setWaypoints(c.waypoints); toast.info(`Loaded ${c.name}`)
  }

  async function captureCurrent() {
    if (!target) { toast.err('Selecione um player'); return }
    const list = await api.players()
    const p = list.find((x) => x.uuid === target)
    if (!p) return
    setWaypoints((w) => [...w, {
      x: Math.round(p.position.x * 10) / 10,
      y: Math.round(p.position.y * 10) / 10,
      z: Math.round(p.position.z * 10) / 10,
      yaw: Math.round(p.position.yaw),
      pitch: Math.round(p.position.pitch),
      durationMs: 3000,
    }])
    toast.ok(`Captured waypoint #${waypoints.length + 1}`)
  }

  async function play() {
    if (!target || waypoints.length < 1 || running) return
    setRunning(true); cancelRef.current = false; setProgress(0)
    const players0 = await api.players()
    const start = players0.find((p) => p.uuid === target)
    if (!start) { setRunning(false); return }
    try {
      await api.gamemode(target, 'spectator')
      const path = [{
        x: start.position.x, y: start.position.y, z: start.position.z,
        yaw: start.position.yaw, pitch: start.position.pitch,
        durationMs: 0,
      } as Waypoint, ...waypoints]
      const total = path.reduce((a, w) => a + w.durationMs, 0) || 1
      let elapsed = 0
      for (let i = 1; i < path.length; i++) {
        if (cancelRef.current) break
        const a = path[i - 1], b = path[i]
        const seg = Math.max(b.durationMs, 50)
        const steps = Math.max(2, Math.floor(seg / 100)) // ~10Hz
        for (let s = 1; s <= steps; s++) {
          if (cancelRef.current) break
          const t = s / steps
          const x = a.x + (b.x - a.x) * t
          const y = a.y + (b.y - a.y) * t
          const z = a.z + (b.z - a.z) * t
          await api.teleport(target, x, y, z)
          elapsed += seg / steps
          setProgress(elapsed / total)
          await new Promise((r) => setTimeout(r, seg / steps))
        }
        if (b.title || b.subtitle) await api.title(target, b.title ?? '', b.subtitle ?? '', 8, 50, 8)
        if (b.sound) await api.sound(target, b.sound)
        if (b.particle) await api.particle(b.particle, b.x, b.y + 1, b.z, 40)
      }
    } catch (e: any) {
      toast.err(e.message)
    } finally {
      try { await api.gamemode(target, 'survival') } catch {}
      setRunning(false); setProgress(0)
    }
  }

  function update(i: number, p: Partial<Waypoint>) {
    setWaypoints((w) => w.map((wp, j) => j === i ? { ...wp, ...p } : wp))
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎬 Cutscene Director</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Cinematic camera com waypoints. Player vira spectator, voa pelo path, volta ao normal.
          </p>
        </div>
        <div className="flex gap-2">
          {!running && <button className="btn-success" onClick={play} disabled={!target || waypoints.length === 0}>▶ Play Cutscene</button>}
          {running && <button className="btn-danger" onClick={() => { cancelRef.current = true }}>⏹ Stop</button>}
        </div>
      </header>

      {running && (
        <div className="card mb-4">
          <div className="flex items-center justify-between mb-2 text-xs">
            <span>🎥 Recording...</span>
            <span>{(progress * 100).toFixed(1)}%</span>
          </div>
          <div className="h-2 rounded-full bg-liberthia-900 overflow-hidden">
            <div className="h-full bg-gradient-to-r from-liberthia-400 to-liberthia-600 transition-all" style={{ width: `${progress * 100}%` }} />
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-4">
          {/* Header config */}
          <div className="card-glow">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
              <select className="input md:col-span-1" value={target} onChange={(e) => setTarget(e.target.value)}>
                <option value="">— player alvo —</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <input className="input md:col-span-1" value={name} onChange={(e) => setName(e.target.value)} placeholder="nome do cutscene" />
              <div className="flex gap-2">
                <button className="btn-cyan flex-1" onClick={captureCurrent} disabled={!target}>📍 + Waypoint aqui</button>
                <button className="btn-ghost" onClick={save}>💾</button>
              </div>
            </div>
          </div>

          {/* Waypoints */}
          <div className="card-glow">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold">📍 Waypoints ({waypoints.length})</h3>
              <button className="btn-ghost btn-sm" onClick={() => setWaypoints((w) => [...w, { x: 0, y: 80, z: 0, durationMs: 3000 }])}>+ Add manual</button>
            </div>
            {waypoints.length === 0 && (
              <div className="text-center py-6 text-liberthia-300/50 text-sm">
                Posicione o player no MC e clique <span className="chip">+ Waypoint aqui</span> pra capturar a coordenada.
              </div>
            )}
            <div className="space-y-2">
              {waypoints.map((w, i) => (
                <div key={i} className="border border-liberthia-500/20 rounded-xl p-3 bg-liberthia-900/40">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="badge badge-purple">#{i + 1}</span>
                    <span className="text-xs text-liberthia-300/70 font-mono flex-1">
                      {w.x.toFixed(0)}, {w.y.toFixed(0)}, {w.z.toFixed(0)}
                    </span>
                    <span className="chip">{(w.durationMs / 1000).toFixed(1)}s</span>
                    <button className="btn-ghost btn-sm" onClick={() => setWaypoints((wp) => wp.filter((_, j) => j !== i))}>🗑</button>
                  </div>
                  <div className="grid grid-cols-3 gap-1.5 mb-2 text-xs">
                    <NumInput value={w.x} onChange={(v) => update(i, { x: v })} />
                    <NumInput value={w.y} onChange={(v) => update(i, { y: v })} />
                    <NumInput value={w.z} onChange={(v) => update(i, { z: v })} />
                  </div>
                  <div className="flex gap-2 mb-2 text-xs items-center">
                    <span className="label">Duração</span>
                    <input type="range" min={500} max={20000} step={100} value={w.durationMs}
                           onChange={(e) => update(i, { durationMs: Number(e.target.value) })} className="flex-1" />
                  </div>
                  <input className="input mb-1 text-xs font-mono" placeholder="title (opcional)" value={w.title ?? ''} onChange={(e) => update(i, { title: e.target.value })} />
                  <input className="input mb-1 text-xs font-mono" placeholder="subtitle (opcional)" value={w.subtitle ?? ''} onChange={(e) => update(i, { subtitle: e.target.value })} />
                  <div className="grid grid-cols-2 gap-1.5">
                    <Autocomplete value={w.sound ?? ''} onChange={(v) => update(i, { sound: v })} options={sounds} placeholder="sound (minecraft:...)" />
                    <Autocomplete value={w.particle ?? ''} onChange={(v) => update(i, { particle: v })} options={particles} placeholder="particle (minecraft:...)" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">💾 Salvos ({saved.length})</h3>
            <div className="space-y-1">
              {saved.map((s) => (
                <div key={s.name} className="flex items-center gap-1">
                  <button className="btn-ghost flex-1 text-left text-xs" onClick={() => load(s)}>
                    {s.name}
                    <span className="block text-[10px] opacity-50">{s.waypoints.length} pts · {new Date(s.updated).toLocaleString()}</span>
                  </button>
                  <button className="btn-ghost btn-sm" onClick={() => persist(saved.filter((x) => x.name !== s.name))}>🗑</button>
                </div>
              ))}
            </div>
          </div>

          <div className="card text-xs space-y-1.5 text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Dicas</div>
            <p>• Posicione o player no MC, depois clique <span className="chip">+ Waypoint</span> várias vezes pra capturar uma trajetória.</p>
            <p>• Duração = tempo de voo até o próximo waypoint.</p>
            <p>• Title aparece quando chega no waypoint.</p>
            <p>• Player não vai poder se mexer durante o playback (spectator + auto-tp).</p>
          </div>
        </div>
      </div>
    </div>
  )
}
