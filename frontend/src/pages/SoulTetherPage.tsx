import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Soul Tether — desenha linha de partículas conectando 2 players, em loop.
 * Atualiza posição a cada tick — segue eles em tempo real.
 * Quando um machuca o outro, os dois sentem (sync HP via /effect).
 */

const PARTICLES = [
  ['minecraft:soul_fire_flame', '🔵 Soul Flame'],
  ['minecraft:end_rod', '⚪ End Rod'],
  ['minecraft:heart', '❤ Heart'],
  ['minecraft:enchant', '✨ Enchant'],
  ['minecraft:portal', '🟣 Portal'],
  ['minecraft:dragon_breath', '💜 Dragon Breath'],
] as const

export function SoulTetherPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 2000 })
  const players = playersQ.data ?? []
  const [a, setA] = useState('')
  const [b, setB] = useState('')
  const [particle, setParticle] = useState<string>('minecraft:soul_fire_flame')
  const [density, setDensity] = useState(20)
  const [syncHp, setSyncHp] = useState(true)
  const [active, setActive] = useState(false)
  const intervalRef = useRef<number | null>(null)
  const [stats, setStats] = useState({ ticks: 0, broken: false })

  useEffect(() => () => { if (intervalRef.current) clearInterval(intervalRef.current) }, [])

  async function start() {
    if (!a || !b || a === b) { toast.err('Selecione 2 players diferentes'); return }
    setActive(true); setStats({ ticks: 0, broken: false })

    const pA = players.find((p) => p.uuid === a)
    const pB = players.find((p) => p.uuid === b)
    if (!pA || !pB) return

    try {
      await api.title(a, '§5§l⛓ TETHER', `§7você foi vinculado a §f${pB.name}`, 10, 80, 20)
      await api.title(b, '§5§l⛓ TETHER', `§7você foi vinculado a §f${pA.name}`, 10, 80, 20)
      await api.sound(a, 'minecraft:entity.elder_guardian.curse', 1, 0.7)
      await api.sound(b, 'minecraft:entity.elder_guardian.curse', 1, 0.7)
    } catch {}

    intervalRef.current = window.setInterval(async () => {
      try {
        const live = await api.players()
        const lA = live.find((x: any) => x.uuid === a)
        const lB = live.find((x: any) => x.uuid === b)
        if (!lA || !lB) {
          setStats((s) => ({ ...s, broken: true }))
          return
        }
        // Desenha linha N pontos
        for (let i = 0; i <= density; i++) {
          const k = i / density
          const x = lA.position.x + (lB.position.x - lA.position.x) * k
          const y = lA.position.y + 1 + (lB.position.y + 1 - lA.position.y - 1) * k
          const z = lA.position.z + (lB.position.z - lA.position.z) * k
          api.particle(particle, x, y, z, 1).catch(() => {})
        }
        // Sync HP — se um tá com HP baixo, dano leve no outro
        if (syncHp && stats.ticks % 5 === 0) {
          const diff = Math.abs(lA.health - lB.health)
          if (diff > 4) {
            const weak = lA.health < lB.health ? lB : lA
            api.command(`damage ${weak.name} 1`, 'tether').catch(() => {})
          }
        }
        setStats((s) => ({ ...s, ticks: s.ticks + 1 }))
      } catch {}
    }, 200)
  }

  async function stop() {
    if (intervalRef.current) clearInterval(intervalRef.current)
    intervalRef.current = null
    setActive(false)
    try {
      if (a) await api.title(a, '§7§o⛓ Tether quebrado', '', 5, 40, 10)
      if (b) await api.title(b, '§7§o⛓ Tether quebrado', '', 5, 40, 10)
    } catch {}
  }

  return (
    <div className="route-fade max-w-[1300px]">
      <header className="mb-6">
        <h1 className="page-title">⛓ Soul Tether</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Vincula 2 players com uma linha de partículas. Updates em tempo real seguindo os dois.
          Modo HP sync: se HP difere muito, o mais forte toma dano pra equalizar.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-5">
        <div className="card-glow">
          <div className="grid grid-cols-2 gap-3 mb-3">
            <div>
              <label className="label block mb-1">Player A</label>
              <select className="input" value={a} onChange={(e) => setA(e.target.value)} disabled={active}>
                <option value="">— escolher —</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
            </div>
            <div>
              <label className="label block mb-1">Player B</label>
              <select className="input" value={b} onChange={(e) => setB(e.target.value)} disabled={active}>
                <option value="">— escolher —</option>
                {players.filter((p) => p.uuid !== a).map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
            </div>
          </div>

          <label className="label block mb-1">Partícula</label>
          <select className="input mb-3 font-mono text-xs" value={particle} onChange={(e) => setParticle(e.target.value)}>
            {PARTICLES.map(([id, lbl]) => <option key={id} value={id}>{lbl}</option>)}
          </select>

          <label className="label block mb-1">Densidade: {density} pontos</label>
          <input type="range" min={5} max={40} value={density} onChange={(e) => setDensity(Number(e.target.value))} className="w-full mb-3" />

          <label className="flex items-center gap-2 cursor-pointer mb-3">
            <input type="checkbox" checked={syncHp} onChange={(e) => setSyncHp(e.target.checked)} />
            <span className="text-sm">💔 <b>Sync HP</b> — dano leve no mais forte se diferença &gt; 4</span>
          </label>

          {!active ? (
            <button className="btn w-full" onClick={start} disabled={!a || !b || a === b}>⛓ Vincular</button>
          ) : (
            <button className="btn-danger w-full pulse-glow" onClick={stop}>✂ Quebrar Vínculo</button>
          )}
        </div>

        <div className="card text-sm">
          <h3 className="font-bold mb-2">📊 Status</h3>
          <Row k="Estado" v={active ? <span className="badge badge-green">ativo</span> : <span className="badge badge-purple">parado</span>} />
          <Row k="Ticks" v={stats.ticks} />
          <Row k="Sync HP" v={syncHp ? 'on' : 'off'} />
          <Row k="Partícula" v={particle.replace('minecraft:', '')} />
          {stats.broken && <div className="mt-2 text-red-300 text-xs">⚠ Player saiu — tether quebrado</div>}
        </div>
      </div>
    </div>
  )
}

function Row({ k, v }: { k: string; v: any }) {
  return (
    <div className="flex justify-between py-0.5 text-xs">
      <span className="text-liberthia-300/70">{k}</span>
      <span>{v}</span>
    </div>
  )
}
