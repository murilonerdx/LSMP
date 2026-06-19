import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Player Aura — partículas em padrões geométricos em volta do player, em LOOP
 * até desativar. Diferente do Particle Path (single-shot drawing), aqui o
 * frontend roda um interval que recomputa as posições a cada tick baseado
 * na coord LIVE do player.
 *
 * 8 padrões: circle, spiral, helix, halo, rain, shield, vortex, runic
 */

type Pattern = 'circle' | 'spiral' | 'helix' | 'halo' | 'rain' | 'shield' | 'vortex' | 'runic'

type Aura = {
  uuid: string
  name: string
  enabled: boolean
  pattern: Pattern
  particle: string
  radius: number
  density: number
  speed: number
  height: number  // altura em relação ao player
}

const PATTERNS: { p: Pattern; emoji: string; label: string; desc: string }[] = [
  { p: 'circle', emoji: '⭕', label: 'Círculo', desc: 'Anel horizontal ao redor' },
  { p: 'spiral', emoji: '🌀', label: 'Espiral', desc: 'Sobe enquanto gira' },
  { p: 'helix',  emoji: '🧬', label: 'Hélice', desc: 'DNA duplo, 2 hélices em sincronia' },
  { p: 'halo',   emoji: '😇', label: 'Auréola', desc: 'Anel sobre a cabeça' },
  { p: 'rain',   emoji: '🌧', label: 'Chuva', desc: 'Cai de cima do player' },
  { p: 'shield', emoji: '🛡', label: 'Escudo', desc: 'Esfera ao redor' },
  { p: 'vortex', emoji: '🌪', label: 'Vórtice', desc: 'Espiral invertida descendo' },
  { p: 'runic',  emoji: '🜲', label: 'Runas', desc: '6 pontos hexagonais pulsantes' },
]

const PARTICLES = [
  'minecraft:end_rod', 'minecraft:flame', 'minecraft:soul_fire_flame',
  'minecraft:dragon_breath', 'minecraft:portal', 'minecraft:totem_of_undying',
  'minecraft:heart', 'minecraft:firework', 'minecraft:enchant',
  'minecraft:reverse_portal', 'minecraft:sculk_soul', 'minecraft:cherry_leaves',
  'minecraft:wax_on', 'minecraft:electric_spark', 'minecraft:dust_plume',
]

const TICK_MS = 150

export function PlayerAuraPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 1000 })
  const players = playersQ.data ?? []

  const [auras, setAuras] = useKvState<Aura[]>('auras', [])
  const tickCount = useRef(0)
  const intervalRef = useRef<number | null>(null)

  // Sincroniza players
  useEffect(() => {
    setAuras((cur) => {
      const next = [...cur]
      for (const p of players) {
        if (!next.find((a) => a.uuid === p.uuid)) {
          next.push({
            uuid: p.uuid, name: p.name, enabled: false,
            pattern: 'circle', particle: 'minecraft:end_rod',
            radius: 1.5, density: 8, speed: 1, height: 1,
          })
        }
      }
      return next.map((a) => ({ ...a, name: players.find((p) => p.uuid === a.uuid)?.name ?? a.name }))
    })
  }, [players.length])

  // Tick — para cada aura ativa, calcula pontos e dispara partículas
  useEffect(() => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    const active = auras.filter((a) => a.enabled)
    if (active.length === 0) return

    async function tick() {
      tickCount.current++
      const t = tickCount.current
      const live = await api.players().catch(() => [] as any[])
      for (const a of active) {
        const p = live.find((x: any) => x.uuid === a.uuid)
        if (!p) continue
        const pts = computePattern(a, t, p.position)
        for (const pt of pts) {
          api.particle(a.particle, pt.x, pt.y, pt.z, 1).catch(() => {})
        }
      }
    }
    tick()
    intervalRef.current = window.setInterval(tick, TICK_MS)
    return () => { if (intervalRef.current) clearInterval(intervalRef.current) }
  }, [auras])

  function toggle(uuid: string, patch?: Partial<Aura>) {
    setAuras((cur) => cur.map((a) => a.uuid === uuid ? { ...a, ...(patch ?? { enabled: !a.enabled }) } : a))
  }

  async function stopAll() {
    setAuras((cur) => cur.map((a) => ({ ...a, enabled: false })))
    toast.info('Todas as auras paradas')
  }

  const activeCount = auras.filter((a) => a.enabled).length

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">✨ Player Aura</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Padrões de partícula em volta de cada player, em loop. Atualiza posição em tempo real.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`badge ${activeCount > 0 ? 'badge-green' : 'badge-purple'}`}>
            {activeCount > 0 && <span className="live-dot" />}
            {activeCount} ativas
          </span>
          {activeCount > 0 && <button className="btn-danger btn-sm" onClick={stopAll}>⏹ Parar todas</button>}
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {auras.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">✨</div>
            <p className="text-liberthia-300/70">Aguardando players online...</p>
          </div>
        )}
        {auras.map((a) => {
          const online = players.some((p) => p.uuid === a.uuid)
          const pat = PATTERNS.find((x) => x.p === a.pattern)!
          return (
            <div key={a.uuid} className={`card-glow transition ${a.enabled ? '!border-emerald-400/40' : ''}`}>
              <div className="flex items-center gap-2 mb-3">
                <img src={`https://mc-heads.net/avatar/${a.uuid}/32`} className="rounded" />
                <div className="flex-1 min-w-0">
                  <div className="font-bold truncate">{a.name}</div>
                  {!online && <span className="badge badge-red text-[10px]">offline</span>}
                </div>
                <button
                  className={`w-12 h-7 rounded-full relative transition ${a.enabled ? 'bg-emerald-500/70' : 'bg-liberthia-700'}`}
                  onClick={() => toggle(a.uuid)}
                >
                  <span className={`absolute top-1 ${a.enabled ? 'right-1' : 'left-1'} w-5 h-5 rounded-full bg-white transition-all`} />
                </button>
              </div>

              {/* Pattern */}
              <label className="label block mb-1">Padrão: {pat.emoji} {pat.label}</label>
              <div className="grid grid-cols-4 gap-1 mb-2">
                {PATTERNS.map((p) => (
                  <button key={p.p}
                    title={p.desc}
                    className={`btn-ghost btn-sm ${a.pattern === p.p ? '!bg-liberthia-500/30 !text-white' : ''}`}
                    onClick={() => toggle(a.uuid, { pattern: p.p })}
                  >{p.emoji}</button>
                ))}
              </div>

              {/* Particle */}
              <label className="label block mb-1">Partícula</label>
              <select className="input text-xs mb-2 font-mono" value={a.particle}
                onChange={(e) => toggle(a.uuid, { particle: e.target.value })}>
                {PARTICLES.map((p) => <option key={p} value={p}>{p.replace('minecraft:', '')}</option>)}
              </select>

              {/* Sliders */}
              <div className="space-y-1.5 text-xs">
                <Slider label="Raio" value={a.radius} min={0.3} max={5} step={0.1} onChange={(v) => toggle(a.uuid, { radius: v })} />
                <Slider label="Densidade" value={a.density} min={2} max={32} step={1} onChange={(v) => toggle(a.uuid, { density: v })} />
                <Slider label="Velocidade" value={a.speed} min={0.1} max={3} step={0.1} onChange={(v) => toggle(a.uuid, { speed: v })} />
                <Slider label="Altura" value={a.height} min={0} max={4} step={0.1} onChange={(v) => toggle(a.uuid, { height: v })} />
              </div>
            </div>
          )
        })}
      </div>

      <div className="card mt-6 text-xs text-liberthia-300/70">
        <div className="font-bold text-liberthia-200 mb-2">💡 Notas</div>
        <p>• Loop roda a cada {TICK_MS}ms enquanto a aba estiver aberta</p>
        <p>• Posição é re-fetched a cada tick (segue player se mover)</p>
        <p>• "Densidade" alta + muitos players ativos = muito tráfego HTTP — modera</p>
        <p>• Padrões usam <code>tickCount</code> pra animar — o mesmo player com mesmo pattern fica sincronizado</p>
      </div>
    </div>
  )
}

function Slider({ label, value, min, max, step, onChange }: {
  label: string; value: number; min: number; max: number; step: number; onChange: (v: number) => void
}) {
  return (
    <div className="flex items-center gap-2">
      <span className="label w-16 shrink-0">{label}</span>
      <input type="range" min={min} max={max} step={step} value={value}
        onChange={(e) => onChange(Number(e.target.value))} className="flex-1" />
      <span className="font-mono w-10 text-right text-[10px]">{value.toFixed(1)}</span>
    </div>
  )
}

// ============= Pattern math =============

function computePattern(a: Aura, t: number, pos: { x: number; y: number; z: number }): { x: number; y: number; z: number }[] {
  const pts: { x: number; y: number; z: number }[] = []
  const phase = t * a.speed * 0.15
  const py = pos.y + a.height

  switch (a.pattern) {
    case 'circle': {
      for (let i = 0; i < a.density; i++) {
        const ang = (i / a.density) * Math.PI * 2 + phase
        pts.push({ x: pos.x + Math.cos(ang) * a.radius, y: py, z: pos.z + Math.sin(ang) * a.radius })
      }
      break
    }
    case 'spiral': {
      for (let i = 0; i < a.density; i++) {
        const k = i / a.density
        const ang = k * Math.PI * 4 + phase
        const r = a.radius * (0.3 + k * 0.7)
        pts.push({ x: pos.x + Math.cos(ang) * r, y: py + k * 2.5, z: pos.z + Math.sin(ang) * r })
      }
      break
    }
    case 'helix': {
      const half = Math.max(2, Math.floor(a.density / 2))
      for (let i = 0; i < half; i++) {
        const k = i / half
        const ang = k * Math.PI * 4 + phase
        pts.push({ x: pos.x + Math.cos(ang) * a.radius, y: py + k * 2.2, z: pos.z + Math.sin(ang) * a.radius })
        pts.push({ x: pos.x + Math.cos(ang + Math.PI) * a.radius, y: py + k * 2.2, z: pos.z + Math.sin(ang + Math.PI) * a.radius })
      }
      break
    }
    case 'halo': {
      for (let i = 0; i < a.density; i++) {
        const ang = (i / a.density) * Math.PI * 2 + phase * 0.4
        pts.push({ x: pos.x + Math.cos(ang) * a.radius, y: py + 2.3, z: pos.z + Math.sin(ang) * a.radius })
      }
      break
    }
    case 'rain': {
      for (let i = 0; i < a.density; i++) {
        // Pontos aleatórios em coluna acima do player, "caindo" baseado no tick
        const seed = (i * 73 + t * 17) % 1000
        const ang = (seed / 1000) * Math.PI * 2
        const r = (((seed * 31) % 100) / 100) * a.radius
        const yOffset = ((seed + t * 3) % 100) / 100 * 4
        pts.push({ x: pos.x + Math.cos(ang) * r, y: py + 4 - yOffset, z: pos.z + Math.sin(ang) * r })
      }
      break
    }
    case 'shield': {
      // Esfera de pontos pseudo-aleatórios mas estáveis
      for (let i = 0; i < a.density; i++) {
        const k = i / a.density
        const phi = Math.acos(2 * k - 1)
        const theta = i * Math.PI * (3 - Math.sqrt(5)) + phase
        pts.push({
          x: pos.x + Math.sin(phi) * Math.cos(theta) * a.radius,
          y: py + 1 + Math.cos(phi) * a.radius,
          z: pos.z + Math.sin(phi) * Math.sin(theta) * a.radius,
        })
      }
      break
    }
    case 'vortex': {
      for (let i = 0; i < a.density; i++) {
        const k = i / a.density
        const ang = k * Math.PI * 6 + phase
        const r = a.radius * (1 - k * 0.8)
        pts.push({ x: pos.x + Math.cos(ang) * r, y: py + 3 - k * 3, z: pos.z + Math.sin(ang) * r })
      }
      break
    }
    case 'runic': {
      // 6 pontos hexagonais que pulsam
      const pulse = (Math.sin(phase * 2) + 1) / 2
      const r = a.radius * (0.7 + 0.3 * pulse)
      for (let i = 0; i < 6; i++) {
        const ang = (i / 6) * Math.PI * 2 + phase * 0.2
        pts.push({ x: pos.x + Math.cos(ang) * r, y: py, z: pos.z + Math.sin(ang) * r })
      }
      // pontos intermediários conectando os hexágonos
      const extra = Math.max(0, a.density - 6)
      for (let i = 0; i < extra; i++) {
        const ang = (i / extra) * Math.PI * 2 + phase * 0.5
        pts.push({ x: pos.x + Math.cos(ang) * r * 0.5, y: py + 0.5, z: pos.z + Math.sin(ang) * r * 0.5 })
      }
      break
    }
  }
  return pts
}
