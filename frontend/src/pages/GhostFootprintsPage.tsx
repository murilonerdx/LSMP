import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Ghost Footprints — toda vez que o player se move, deixa pegadas (partículas)
 * que ficam pulsando no chão por X segundos. Outros players ao redor podem
 * ver o rastro.
 */

type Footprint = { x: number; y: number; z: number; ts: number; player: string }

export function GhostFootprintsPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 1500 })
  const players = playersQ.data ?? []
  const [selectedUuids, setSelectedUuids] = useState<Set<string>>(new Set())
  const [active, setActive] = useState(false)
  const [particle, setParticle] = useState('minecraft:sculk_soul')
  const [intensity, setIntensity] = useState(8)
  const [stepDist, setStepDist] = useState(1.5)
  const [lifeTime, setLifeTime] = useState(15)
  const lastPosRef = useRef<Record<string, { x: number; y: number; z: number }>>({})
  const footprintsRef = useRef<Footprint[]>([])
  const intervalRef = useRef<number | null>(null)
  const [stats, setStats] = useState({ active: 0, dropped: 0 })

  useEffect(() => () => { if (intervalRef.current) clearInterval(intervalRef.current) }, [])

  function toggle(uuid: string) {
    setSelectedUuids((cur) => {
      const n = new Set(cur)
      if (n.has(uuid)) n.delete(uuid); else n.add(uuid)
      return n
    })
  }

  async function start() {
    if (selectedUuids.size === 0) { toast.err('Selecione pelo menos 1 player'); return }
    setActive(true)
    footprintsRef.current = []

    intervalRef.current = window.setInterval(async () => {
      const live = await api.players().catch(() => [] as Player[])
      let dropped = 0
      for (const p of live) {
        if (!selectedUuids.has(p.uuid)) continue
        const last = lastPosRef.current[p.uuid]
        const dx = last ? p.position.x - last.x : 99
        const dy = last ? p.position.y - last.y : 0
        const dz = last ? p.position.z - last.z : 99
        const moved = Math.sqrt(dx * dx + dz * dz)
        if (moved >= stepDist) {
          footprintsRef.current.push({ x: p.position.x, y: p.position.y, z: p.position.z, ts: Date.now(), player: p.name })
          lastPosRef.current[p.uuid] = { ...p.position }
          dropped++
        }
      }
      // Limpa expirados
      const cutoff = Date.now() - lifeTime * 1000
      footprintsRef.current = footprintsRef.current.filter((f) => f.ts > cutoff)
      // Renderiza pulse pra cada footprint vivo
      for (const fp of footprintsRef.current) {
        const age = (Date.now() - fp.ts) / 1000
        const alpha = 1 - age / lifeTime
        const count = Math.max(1, Math.floor(intensity * alpha))
        api.particle(particle, fp.x, fp.y + 0.1, fp.z, count).catch(() => {})
      }
      setStats({ active: footprintsRef.current.length, dropped: stats.dropped + dropped })
    }, 700)
  }

  function stop() {
    if (intervalRef.current) clearInterval(intervalRef.current)
    intervalRef.current = null
    footprintsRef.current = []
    lastPosRef.current = {}
    setActive(false)
    toast.ok('Pegadas paradas')
  }

  return (
    <div className="route-fade max-w-[1300px]">
      <header className="mb-6">
        <h1 className="page-title">👣 Ghost Footprints</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Quando o player anda, deixa pegadas que pulsam por N segundos. Outros players próximos vêem.
          Cria efeito de "rastro fantasma" — útil pra ambientação de horror.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="card-glow">
          <h3 className="font-bold mb-3">🎯 Players a rastrear</h3>
          {players.length === 0 && <p className="text-xs text-liberthia-300/50 italic">Ninguém online</p>}
          <div className="flex flex-wrap gap-2 mb-4">
            {players.map((p) => {
              const on = selectedUuids.has(p.uuid)
              return (
                <button key={p.uuid} type="button"
                  onClick={() => toggle(p.uuid)}
                  disabled={active}
                  className={`px-3 py-2 rounded-lg border transition flex items-center gap-2 text-sm ${
                    on ? 'bg-liberthia-500/30 border-liberthia-400/60 text-white'
                       : 'bg-liberthia-900/40 border-liberthia-500/20'
                  }`}>
                  <img src={`https://mc-heads.net/avatar/${p.uuid}/24`} className="rounded" />
                  {p.name}
                </button>
              )
            })}
          </div>

          <label className="label block mb-1">Partícula</label>
          <select className="input mb-3 font-mono text-xs" value={particle} onChange={(e) => setParticle(e.target.value)}>
            <option value="minecraft:sculk_soul">sculk_soul (verde sombrio)</option>
            <option value="minecraft:soul_fire_flame">soul_fire_flame (azul)</option>
            <option value="minecraft:flame">flame (laranja)</option>
            <option value="minecraft:heart">heart</option>
            <option value="minecraft:ash">ash (cinza)</option>
            <option value="minecraft:dragon_breath">dragon_breath (roxo)</option>
            <option value="minecraft:end_rod">end_rod (branco)</option>
          </select>

          <div className="grid grid-cols-3 gap-3 text-xs mb-3">
            <div>
              <label className="label">Intensidade: {intensity}</label>
              <input type="range" min={2} max={20} value={intensity} onChange={(e) => setIntensity(Number(e.target.value))} className="w-full" />
            </div>
            <div>
              <label className="label">Espaço entre passos: {stepDist}b</label>
              <input type="range" min={0.5} max={4} step={0.1} value={stepDist} onChange={(e) => setStepDist(Number(e.target.value))} className="w-full" />
            </div>
            <div>
              <label className="label">Vida: {lifeTime}s</label>
              <input type="range" min={3} max={60} value={lifeTime} onChange={(e) => setLifeTime(Number(e.target.value))} className="w-full" />
            </div>
          </div>

          {!active ? (
            <button className="btn w-full" onClick={start} disabled={selectedUuids.size === 0}>👣 Iniciar Rastro</button>
          ) : (
            <button className="btn-danger w-full pulse-glow" onClick={stop}>⏹ Parar</button>
          )}
        </div>

        <div className="card text-xs">
          <h3 className="font-bold mb-2">📊 Stats</h3>
          <div className="space-y-1">
            <Row k="Players rastreados" v={selectedUuids.size} />
            <Row k="Pegadas ativas" v={stats.active} />
            <Row k="Total dropadas" v={stats.dropped} />
            <Row k="Vida individual" v={`${lifeTime}s`} />
          </div>

          <div className="font-bold text-liberthia-200 mt-3 mb-1">💡 Combina com:</div>
          <p className="text-liberthia-300/70">• <span className="chip">Shadow Walker</span> pra player fantasma deixando rastro</p>
          <p className="text-liberthia-300/70">• <span className="chip">Echo Whispers</span> nas pegadas pra criar caminho de lore</p>
        </div>
      </div>
    </div>
  )
}

function Row({ k, v }: { k: string; v: any }) {
  return <div className="flex justify-between"><span className="text-liberthia-300/70">{k}</span><span className="font-mono">{v}</span></div>
}
