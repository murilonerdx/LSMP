import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Eldritch Sigil — desenha sigilos ocultos no chão com partículas em padrões
 * matemáticos (pentagrama, hexagrama, olho, espiral, círculo de runas).
 *
 * Anima do centro pra fora, com sound buildup. Permanece visível ~10s.
 */

type SigilType = 'pentagram' | 'hexagram' | 'eye' | 'spiral' | 'circle' | 'flower_of_life' | 'triquetra' | 'enneagram'

type Sigil = {
  type: SigilType
  emoji: string
  label: string
  desc: string
  draw: (cx: number, cy: number, cz: number, t: number) => { x: number; y: number; z: number }[]
}

const SIGIL_TYPES: Sigil[] = [
  {
    type: 'pentagram', emoji: '⛤', label: 'Pentagrama', desc: '5 pontas conectadas — invocação',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      const r = 4
      const phase = (t / 100) * Math.PI * 0.2  // anima leve rotação
      // 5 vertices
      const verts: [number, number][] = []
      for (let i = 0; i < 5; i++) {
        const ang = -Math.PI / 2 + (i * Math.PI * 2) / 5 + phase
        verts.push([Math.cos(ang) * r, Math.sin(ang) * r])
      }
      // Linhas: 0→2→4→1→3→0
      const order = [0, 2, 4, 1, 3, 0]
      for (let i = 0; i < order.length - 1; i++) {
        const a = verts[order[i]], b = verts[order[i + 1]]
        const STEPS = 14
        for (let s = 0; s <= STEPS; s++) {
          const k = s / STEPS
          pts.push({ x: cx + a[0] + (b[0] - a[0]) * k, y: cy + 0.1, z: cz + a[1] + (b[1] - a[1]) * k })
        }
      }
      // Círculo externo
      for (let i = 0; i < 64; i++) {
        const ang = (i / 64) * Math.PI * 2
        pts.push({ x: cx + Math.cos(ang) * (r + 0.3), y: cy + 0.1, z: cz + Math.sin(ang) * (r + 0.3) })
      }
      return pts
    },
  },
  {
    type: 'hexagram', emoji: '✡', label: 'Hexagrama', desc: 'Estrela de 6 pontas — proteção',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      const r = 4
      const phase = (t / 120) * Math.PI * 0.15
      // 2 triângulos sobrepostos
      for (let tri = 0; tri < 2; tri++) {
        const off = tri === 0 ? phase : phase + Math.PI
        const verts: [number, number][] = []
        for (let i = 0; i < 3; i++) {
          const ang = -Math.PI / 2 + (i * Math.PI * 2) / 3 + off
          verts.push([Math.cos(ang) * r, Math.sin(ang) * r])
        }
        for (let i = 0; i < 3; i++) {
          const a = verts[i], b = verts[(i + 1) % 3]
          for (let s = 0; s <= 12; s++) {
            const k = s / 12
            pts.push({ x: cx + a[0] + (b[0] - a[0]) * k, y: cy + 0.1, z: cz + a[1] + (b[1] - a[1]) * k })
          }
        }
      }
      // Círculo
      for (let i = 0; i < 60; i++) {
        const ang = (i / 60) * Math.PI * 2
        pts.push({ x: cx + Math.cos(ang) * (r + 0.3), y: cy + 0.1, z: cz + Math.sin(ang) * (r + 0.3) })
      }
      return pts
    },
  },
  {
    type: 'eye', emoji: '👁', label: 'Olho', desc: 'Olho que vê — atenção do além',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      // Pálpebra superior + inferior (arcos elípticos)
      for (let i = 0; i <= 30; i++) {
        const k = i / 30
        const x = -4 + k * 8
        const y = Math.sin(k * Math.PI) * 2.2
        pts.push({ x: cx + x, y: cy + 0.1, z: cz + y })
        pts.push({ x: cx + x, y: cy + 0.1, z: cz - y })
      }
      // Íris (círculo central)
      const phase = (t / 80) * Math.PI * 0.5
      for (let i = 0; i < 30; i++) {
        const ang = (i / 30) * Math.PI * 2 + phase
        pts.push({ x: cx + Math.cos(ang) * 1.2, y: cy + 0.15, z: cz + Math.sin(ang) * 1.2 })
      }
      // Pupila (ponto central pulsante)
      const pulse = 0.4 + Math.sin(t / 30) * 0.2
      for (let i = 0; i < 12; i++) {
        const ang = (i / 12) * Math.PI * 2
        pts.push({ x: cx + Math.cos(ang) * pulse, y: cy + 0.2, z: cz + Math.sin(ang) * pulse })
      }
      return pts
    },
  },
  {
    type: 'spiral', emoji: '🌀', label: 'Espiral', desc: 'Espiral arquimediana — vórtice',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      const phase = (t / 50) * Math.PI * 0.3
      for (let i = 0; i < 120; i++) {
        const k = i / 120
        const ang = k * Math.PI * 8 + phase
        const r = k * 5
        pts.push({ x: cx + Math.cos(ang) * r, y: cy + 0.1 + k * 0.3, z: cz + Math.sin(ang) * r })
      }
      return pts
    },
  },
  {
    type: 'circle', emoji: '⊙', label: 'Círculo de Runas', desc: 'Anel com 12 marcadores',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      const r = 5
      const phase = (t / 100) * Math.PI * 0.1
      // Círculo externo
      for (let i = 0; i < 80; i++) {
        const ang = (i / 80) * Math.PI * 2
        pts.push({ x: cx + Math.cos(ang) * r, y: cy + 0.1, z: cz + Math.sin(ang) * r })
      }
      // Círculo interno
      for (let i = 0; i < 60; i++) {
        const ang = (i / 60) * Math.PI * 2
        pts.push({ x: cx + Math.cos(ang) * (r * 0.7), y: cy + 0.1, z: cz + Math.sin(ang) * (r * 0.7) })
      }
      // 12 marcadores radiais
      for (let i = 0; i < 12; i++) {
        const ang = (i / 12) * Math.PI * 2 + phase
        for (let s = 0; s <= 8; s++) {
          const k = (s / 8) * 0.3
          pts.push({ x: cx + Math.cos(ang) * (r + k), y: cy + 0.15, z: cz + Math.sin(ang) * (r + k) })
        }
      }
      return pts
    },
  },
  {
    type: 'flower_of_life', emoji: '⚛', label: 'Flor da Vida', desc: '7 círculos sobrepostos',
    draw: (cx, cy, cz) => {
      const pts: { x: number; y: number; z: number }[] = []
      const r = 1.5
      const centers: [number, number][] = [[0, 0]]
      for (let i = 0; i < 6; i++) {
        const ang = (i / 6) * Math.PI * 2
        centers.push([Math.cos(ang) * r * 1.732, Math.sin(ang) * r * 1.732])
      }
      for (const [ox, oy] of centers) {
        for (let i = 0; i < 36; i++) {
          const ang = (i / 36) * Math.PI * 2
          pts.push({ x: cx + ox + Math.cos(ang) * r, y: cy + 0.1, z: cz + oy + Math.sin(ang) * r })
        }
      }
      return pts
    },
  },
  {
    type: 'triquetra', emoji: '☘', label: 'Triquetra', desc: '3 arcos entrelaçados — trindade',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      const r = 2.5
      const phase = (t / 200) * Math.PI * 0.1
      // 3 círculos posicionados em 120°
      for (let i = 0; i < 3; i++) {
        const ang = -Math.PI / 2 + (i * Math.PI * 2) / 3 + phase
        const ox = Math.cos(ang) * r * 0.8
        const oy = Math.sin(ang) * r * 0.8
        for (let j = 0; j < 40; j++) {
          const a2 = (j / 40) * Math.PI * 2
          pts.push({ x: cx + ox + Math.cos(a2) * r, y: cy + 0.1, z: cz + oy + Math.sin(a2) * r })
        }
      }
      return pts
    },
  },
  {
    type: 'enneagram', emoji: '✴', label: 'Eneagrama', desc: '9 pontas — cosmic horror máximo',
    draw: (cx, cy, cz, t) => {
      const pts: { x: number; y: number; z: number }[] = []
      const r = 4.5
      const phase = (t / 80) * Math.PI * 0.2
      // 9 vertices conectados num padrão estrela
      const verts: [number, number][] = []
      for (let i = 0; i < 9; i++) {
        const ang = -Math.PI / 2 + (i * Math.PI * 2) / 9 + phase
        verts.push([Math.cos(ang) * r, Math.sin(ang) * r])
      }
      // Conecta cada vertex com o vertex +4 (pula 3)
      for (let i = 0; i < 9; i++) {
        const a = verts[i], b = verts[(i + 4) % 9]
        for (let s = 0; s <= 14; s++) {
          const k = s / 14
          pts.push({ x: cx + a[0] + (b[0] - a[0]) * k, y: cy + 0.1, z: cz + a[1] + (b[1] - a[1]) * k })
        }
      }
      // Círculo externo
      for (let i = 0; i < 80; i++) {
        const ang = (i / 80) * Math.PI * 2
        pts.push({ x: cx + Math.cos(ang) * (r + 0.3), y: cy + 0.1, z: cz + Math.sin(ang) * (r + 0.3) })
      }
      return pts
    },
  },
]

const PARTICLES = [
  'minecraft:soul_fire_flame', 'minecraft:flame', 'minecraft:end_rod',
  'minecraft:dragon_breath', 'minecraft:portal', 'minecraft:enchant',
  'minecraft:sculk_soul', 'minecraft:reverse_portal',
]

export function EldritchSigilPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [sigilType, setSigilType] = useState<SigilType>('pentagram')
  const [particle, setParticle] = useState('minecraft:soul_fire_flame')
  const [running, setRunning] = useState(false)
  const cancelRef = useRef(false)

  async function summon() {
    if (!target) { toast.err('Selecione player'); return }
    const sigil = SIGIL_TYPES.find((s) => s.type === sigilType); if (!sigil) return
    const list = await api.players()
    const p = list.find((x) => x.uuid === target); if (!p) return
    setRunning(true); cancelRef.current = false

    // Dramatização: title + som de buildup
    try {
      await api.title(target, `§5§l${sigil.emoji} ${sigil.label}`, '§7§oalgo está sendo invocado...', 10, 60, 15)
      await api.sound(target, 'minecraft:entity.warden.sonic_charge', 1, 0.5)
    } catch {}

    // Anima por ~8s, partículas a cada 100ms
    const STEPS = 60
    const cx = p.position.x, cy = p.position.y, cz = p.position.z
    for (let t = 0; t < STEPS; t++) {
      if (cancelRef.current) break
      const pts = sigil.draw(cx, cy, cz, t)
      // Dispara em chunks pra não saturar
      const CHUNK = 40
      for (let i = 0; i < pts.length; i += CHUNK) {
        const slice = pts.slice(i, i + CHUNK)
        // Pra cada ponto, lança particle simples (count=1, sem spread)
        for (const pt of slice) {
          api.particle(particle, pt.x, pt.y, pt.z, 1).catch(() => {})
        }
      }
      // Som ambient a cada 10 frames
      if (t % 10 === 0) {
        try { await api.sound(target, 'minecraft:block.amethyst_block.chime', 0.5, 0.5 + t / 100) } catch {}
      }
      await new Promise((r) => setTimeout(r, 130))
    }

    // Climax
    try {
      await api.command(`playsound minecraft:entity.warden.sonic_boom hostile @a ~ ~ ~ 1 0.6`, 'sigil')
      await api.title(target, `${sigil.emoji}§l ⊗ ${sigil.label.toUpperCase()} ⊗`, '§4§oalgo respondeu...', 5, 80, 20)
    } catch {}
    setRunning(false)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⛤ Eldritch Sigil</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Desenha sigilos ocultos no chão ao redor do player com partículas. Anima ~8s + sons de buildup → climax.
          </p>
        </div>
        <div className="flex gap-2 items-center">
          <select className="input text-sm max-w-xs" value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="">— player (centro do sigilo) —</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
          </select>
          {running && <button className="btn-danger btn-sm" onClick={() => { cancelRef.current = true }}>⏹</button>}
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {SIGIL_TYPES.map((s) => (
          <button key={s.type}
            className={`card-glow text-center transition ${sigilType === s.type ? '!border-liberthia-400/60 !bg-liberthia-500/20' : ''}`}
            onClick={() => setSigilType(s.type)}>
            <div className="text-5xl mb-2">{s.emoji}</div>
            <div className="font-bold">{s.label}</div>
            <div className="text-xs text-liberthia-300/60 mt-1 min-h-[32px]">{s.desc}</div>
          </button>
        ))}
      </div>

      <div className="card-glow">
        <label className="label block mb-1">Partícula</label>
        <select className="input mb-3 font-mono text-xs" value={particle} onChange={(e) => setParticle(e.target.value)}>
          {PARTICLES.map((p) => <option key={p} value={p}>{p.replace('minecraft:', '')}</option>)}
        </select>
        <button className={running ? 'btn-amber w-full pulse-glow' : 'btn w-full'}
          onClick={summon} disabled={!target || running}>
          {running ? '⏳ Invocando...' : `⛤ Invocar ${SIGIL_TYPES.find((s) => s.type === sigilType)?.label}`}
        </button>
      </div>
    </div>
  )
}
