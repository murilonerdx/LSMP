import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Desenho freehand 2D em canvas → reproduz como trilha de partículas no mundo MC.
 * Usuário desenha no canvas, define plano (XZ no Y do player, ou XY) + escala +
 * partícula. Frontend itera os pontos e dispara /api/world/particle.
 */

type Point = { x: number; y: number }

const PARTICLES = [
  'minecraft:end_rod', 'minecraft:flame', 'minecraft:soul_fire_flame',
  'minecraft:dragon_breath', 'minecraft:portal', 'minecraft:totem_of_undying',
  'minecraft:heart', 'minecraft:enchant', 'minecraft:firework',
]

export function ParticlePathPage() {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [strokes, setStrokes] = useState<Point[][]>([])
  const drawingRef = useRef<Point[] | null>(null)
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [particle, setParticle] = useState('minecraft:end_rod')
  const [scale, setScale] = useState(0.2) // 1 px no canvas = 0.2 blocos
  const [stepDelay, setStepDelay] = useState(60) // ms entre partículas
  const [plane, setPlane] = useState<'XZ' | 'XY'>('XZ')
  const [running, setRunning] = useState(false)
  const cancelRef = useRef(false)
  const [size, setSize] = useState({ w: 600, h: 400 })

  useEffect(() => {
    const el = canvasRef.current?.parentElement; if (!el) return
    const ro = new ResizeObserver(() => {
      const r = el.getBoundingClientRect()
      setSize({ w: Math.floor(r.width), h: Math.max(380, Math.floor(window.innerHeight - 350)) })
    })
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  // Render
  useEffect(() => {
    const c = canvasRef.current; if (!c) return
    const ctx = c.getContext('2d')!
    c.width = size.w; c.height = size.h
    ctx.fillStyle = '#070310'; ctx.fillRect(0, 0, c.width, c.height)
    // Grid
    ctx.strokeStyle = 'rgba(170,64,232,0.1)'; ctx.lineWidth = 1
    for (let x = 0; x < c.width; x += 32) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, c.height); ctx.stroke() }
    for (let y = 0; y < c.height; y += 32) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(c.width, y); ctx.stroke() }
    // Origin (canvas center)
    ctx.strokeStyle = '#fff7'; ctx.beginPath()
    ctx.moveTo(c.width / 2, 0); ctx.lineTo(c.width / 2, c.height); ctx.moveTo(0, c.height / 2); ctx.lineTo(c.width, c.height / 2); ctx.stroke()
    // Strokes
    for (const s of strokes) drawStroke(ctx, s)
    if (drawingRef.current) drawStroke(ctx, drawingRef.current)
  }, [size, strokes])

  function drawStroke(ctx: CanvasRenderingContext2D, s: Point[]) {
    if (s.length < 2) return
    ctx.strokeStyle = '#aa40e8'; ctx.lineWidth = 3; ctx.lineCap = 'round'; ctx.lineJoin = 'round'
    ctx.shadowColor = '#aa40e8'; ctx.shadowBlur = 8
    ctx.beginPath()
    ctx.moveTo(s[0].x, s[0].y)
    for (let i = 1; i < s.length; i++) ctx.lineTo(s[i].x, s[i].y)
    ctx.stroke()
    ctx.shadowBlur = 0
  }

  function onDown(e: React.MouseEvent) {
    const r = (e.target as HTMLCanvasElement).getBoundingClientRect()
    drawingRef.current = [{ x: e.clientX - r.left, y: e.clientY - r.top }]
  }
  function onMove(e: React.MouseEvent) {
    if (!drawingRef.current) return
    const r = (e.target as HTMLCanvasElement).getBoundingClientRect()
    drawingRef.current.push({ x: e.clientX - r.left, y: e.clientY - r.top })
    const c = canvasRef.current!; const ctx = c.getContext('2d')!
    drawStroke(ctx, drawingRef.current)
  }
  function onUp() {
    // Snapshot do current ANTES de nullar — senão o setStrokes (assíncrono)
    // captura `drawingRef.current` já null e o reduce explode.
    const current = drawingRef.current
    drawingRef.current = null
    if (current && current.length > 1) {
      setStrokes((s) => [...s, current])
    }
  }

  async function play() {
    if (running) { toast.err('Já tá rodando'); return }
    if (!target) { toast.err('Selecione um player de referência'); return }
    if (strokes.length === 0) { toast.err('Desenhe algo primeiro'); return }
    let p: any
    try {
      const list = await api.players()
      p = list.find((x: any) => x.uuid === target)
    } catch (e: any) {
      toast.err(`Falha ao buscar players: ${e.message}`)
      return
    }
    if (!p) { toast.err('Player não está online'); return }

    setRunning(true); cancelRef.current = false
    const totalPts = strokes.reduce((a, s) => a + s.length, 0)
    toast.info(`▶ Iniciando ${totalPts} partículas em ${plane}`)
    const cx = size.w / 2, cy = size.h / 2
    let sent = 0, errors = 0
    let firstError = ''
    for (const stroke of strokes) {
      if (cancelRef.current) break
      for (const pt of stroke) {
        if (cancelRef.current) break
        const ox = (pt.x - cx) * scale
        const oy = (pt.y - cy) * scale
        let wx = p.position.x, wy = p.position.y + 1, wz = p.position.z
        if (plane === 'XZ') { wx += ox; wz += oy }
        else { wx += ox; wy += -oy } // canvas Y invertido
        try {
          await api.particle(particle, wx, wy, wz, 10)
          sent++
        } catch (e: any) {
          errors++
          if (!firstError) firstError = e.message
        }
        await new Promise((r) => setTimeout(r, stepDelay))
      }
    }
    setRunning(false)
    if (errors > 0) {
      toast.err(`✗ ${errors}/${totalPts} falharam: ${firstError}`)
    } else if (cancelRef.current) {
      toast.info(`⏹ Cancelado — ${sent}/${totalPts} enviados`)
    } else {
      toast.ok(`✓ ${sent} partículas replicadas`)
    }
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎨 Particle Path</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Desenhe à mão livre — server replica como trilha de partículas no mundo.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost btn-sm" onClick={() => setStrokes([])}>🧹 Limpar</button>
          <button className="btn-ghost btn-sm" onClick={() => setStrokes((s) => s.slice(0, -1))}>↶ Undo</button>
          {!running && <button className="btn-success" onClick={play} disabled={!target || strokes.length === 0}>▶ Replicar</button>}
          {running && <button className="btn-danger" onClick={() => { cancelRef.current = true }}>⏹</button>}
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_280px] gap-5">
        <div className="card-glow p-2">
          <canvas
            ref={canvasRef}
            className="rounded-xl cursor-crosshair select-none"
            style={{ display: 'block', width: '100%' }}
            onMouseDown={onDown}
            onMouseMove={onMove}
            onMouseUp={onUp}
            onMouseLeave={onUp}
          />
        </div>

        <div className="space-y-4">
          <div className="card text-sm">
            <h3 className="font-bold mb-2">🎯 Alvo</h3>
            <select className="input mb-3" value={target} onChange={(e) => setTarget(e.target.value)}>
              <option value="">— player de referência —</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>

            <label className="label block mb-1">Plano</label>
            <div className="tab-strip mb-3">
              <div className={`tab-item ${plane === 'XZ' ? 'active' : ''}`} onClick={() => setPlane('XZ')}>XZ (chão)</div>
              <div className={`tab-item ${plane === 'XY' ? 'active' : ''}`} onClick={() => setPlane('XY')}>XY (parede)</div>
            </div>

            <label className="label block mb-1">Partícula</label>
            <select className="input mb-3" value={particle} onChange={(e) => setParticle(e.target.value)}>
              {PARTICLES.map((p) => <option key={p} value={p}>{p.replace('minecraft:', '')}</option>)}
            </select>

            <label className="label block mb-1">Escala (px → blocos): {scale.toFixed(2)}</label>
            <input type="range" min={0.05} max={1} step={0.05} value={scale} onChange={(e) => setScale(Number(e.target.value))} className="w-full mb-3" />

            <label className="label block mb-1">Delay: {stepDelay}ms</label>
            <input type="range" min={10} max={300} step={10} value={stepDelay} onChange={(e) => setStepDelay(Number(e.target.value))} className="w-full" />
          </div>

          <div className="card text-xs text-liberthia-300/70">
            {(() => {
              const totalPts = strokes.reduce((a, s) => a + (s?.length ?? 0), 0)
              return (
                <>
                  <p>• <span className="chip">{strokes.length}</span> traços, total <span className="chip">{totalPts}</span> pontos</p>
                  <p>• Tempo estimado: <span className="chip">{((totalPts * stepDelay) / 1000).toFixed(1)}s</span></p>
                </>
              )
            })()}
          </div>
        </div>
      </div>
    </div>
  )
}
