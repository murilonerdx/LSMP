import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Roleta de Eventos — segmentos com peso, gira, sorteia, dispara comando.
 *
 * IMPORTANTE: angle vive em useRef (mutável) e drawWheel() é chamado
 * direto pelo requestAnimationFrame — sem passar por React state durante
 * animação. Isso evita race condition que fazia o canvas sumir.
 */

type Segment = {
  id: number
  label: string
  color: string
  weight: number
  command?: string
  emoji?: string
}

const PALETTE = ['#aa40e8', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#8b5cf6', '#22d3ee', '#f97316', '#84cc16']

const DEFAULT_SEGMENTS: Segment[] = [
  { id: 1, label: 'Diamantes', emoji: '💎', color: PALETTE[1], weight: 1, command: 'give {player} minecraft:diamond 5' },
  { id: 2, label: 'Raio na cabeça', emoji: '⚡', color: PALETTE[4], weight: 1, command: 'execute as {player} at @s run summon lightning_bolt ~ ~ ~' },
  { id: 3, label: 'Speed II', emoji: '💨', color: PALETTE[0], weight: 2, command: 'effect give {player} minecraft:speed 600 1' },
  { id: 4, label: 'Slowness', emoji: '🐌', color: PALETTE[6], weight: 1, command: 'effect give {player} minecraft:slowness 200 2' },
  { id: 5, label: 'Foguete', emoji: '🎆', color: PALETTE[5], weight: 1, command: 'execute as {player} at @s run summon firework_rocket ~ ~3 ~ {LifeTime:30,FireworksItem:{id:"firework_rocket",Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16711680,65280]}]}}}}' },
  { id: 6, label: 'Cavalo', emoji: '🐴', color: PALETTE[3], weight: 0.5, command: 'execute as {player} at @s run summon horse ~ ~ ~ {Tame:1b}' },
  { id: 7, label: 'Nada (azar)', emoji: '🌑', color: PALETTE[7], weight: 3, command: 'tellraw {player} {"text":"§7§oa roleta gira mas... nada acontece"}' },
  { id: 8, label: 'Vida cheia', emoji: '❤', color: PALETTE[2], weight: 1, command: 'effect give {player} minecraft:instant_health 1 4' },
]

export function RandomWheelPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState<string>('')
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [segments, setSegments] = useKvState<Segment[]>('wheel', DEFAULT_SEGMENTS)
  const segmentsRef = useRef(segments)
  segmentsRef.current = segments

  // angle vive em ref pra não passar por state durante anim
  const angleRef = useRef(0)
  const animRef = useRef<number | null>(null)

  const [spinning, setSpinning] = useState(false)
  const [winner, setWinner] = useState<Segment | null>(null)
  const [history, setHistory] = useState<{ ts: number; player: string; segment: string }[]>([])
  const [editing, setEditing] = useState<Segment | null>(null)
  const [renderTrigger, setRenderTrigger] = useState(0)  // pra forçar redraw quando segments mudam

  // Draw function — pura, sempre desenha o que tem em segmentsRef + angleRef
  // Tamanho adaptativo: usa width do container, cap em 380px pra não dominar a tela
  function drawWheel() {
    const c = canvasRef.current; if (!c) return
    const ctx = c.getContext('2d')
    if (!ctx) return
    const container = c.parentElement
    const available = container ? container.clientWidth - 16 : 320
    const size = Math.max(220, Math.min(available, 380))
    if (c.width !== size) { c.width = size; c.height = size }
    const cx = size / 2, cy = size / 2, r = size / 2 - 10
    ctx.clearRect(0, 0, size, size)

    const segs = segmentsRef.current
    if (segs.length === 0) return

    const angle = angleRef.current
    const total = segs.reduce((a, s) => a + s.weight, 0)
    let a = angle
    for (const s of segs) {
      const span = (s.weight / total) * Math.PI * 2
      ctx.fillStyle = s.color
      ctx.beginPath()
      ctx.moveTo(cx, cy)
      ctx.arc(cx, cy, r, a, a + span)
      ctx.closePath()
      ctx.fill()
      ctx.strokeStyle = 'rgba(0,0,0,0.5)'; ctx.lineWidth = 2
      ctx.stroke()
      // label
      ctx.save()
      ctx.translate(cx, cy)
      ctx.rotate(a + span / 2)
      ctx.textAlign = 'right'
      ctx.fillStyle = '#fff'
      ctx.font = 'bold 14px Inter, sans-serif'
      ctx.shadowColor = 'rgba(0,0,0,0.8)'; ctx.shadowBlur = 4
      ctx.fillText(`${s.emoji ?? ''} ${s.label}`, r - 14, 6)
      ctx.shadowBlur = 0
      ctx.restore()
      a += span
    }
    // Hub
    ctx.fillStyle = '#0a0410'
    ctx.beginPath(); ctx.arc(cx, cy, 30, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#aa40e8'; ctx.font = 'bold 22px Inter'
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText('🎰', cx, cy)
    // Pointer
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.moveTo(cx - 12, 4); ctx.lineTo(cx + 12, 4); ctx.lineTo(cx, 32); ctx.closePath()
    ctx.fill()
    ctx.strokeStyle = '#aa40e8'; ctx.lineWidth = 2
    ctx.stroke()
  }

  // Redraw quando dados mudam (mas NÃO durante anim — anim chama draw direto via rAF)
  useEffect(() => { drawWheel() }, [renderTrigger, segments])

  // Cleanup
  useEffect(() => () => { if (animRef.current) cancelAnimationFrame(animRef.current) }, [])

  function pickWinner(): Segment | null {
    const total = segments.reduce((a, s) => a + s.weight, 0)
    let r = Math.random() * total
    for (const s of segments) { r -= s.weight; if (r <= 0) return s }
    return segments[segments.length - 1] ?? null
  }

  async function spin() {
    if (!target) { toast.err('Selecione player'); return }
    if (spinning) return
    if (segments.length === 0) return

    const player = players.find((p) => p.uuid === target)
    if (!player) return

    setSpinning(true); setWinner(null)
    const picked = pickWinner()
    if (!picked) { setSpinning(false); return }
    const winSafe: Segment = picked

    // Calcula ângulo final
    const total = segments.reduce((a, s) => a + s.weight, 0)
    let cumul = 0
    for (const s of segments) { if (s.id === winSafe.id) break; cumul += s.weight }
    const winStart = (cumul / total) * Math.PI * 2
    const winSpan = (winSafe.weight / total) * Math.PI * 2
    const winMid = winStart + winSpan / 2
    const extraSpins = 6
    const startAngle = angleRef.current % (Math.PI * 2)
    const targetAngle = startAngle + (-Math.PI / 2 - winMid - startAngle) + extraSpins * Math.PI * 2

    // Broadcast in-game: aviso que a roleta tá girando pra esse player
    try {
      await api.command(`tellraw @a [{"text":"\\n§5§l═══ ROLETA ═══§r\\n"},{"text":"§e🎰 "},{"text":"${player.name}","color":"yellow","bold":true},{"text":" §7está girando a roleta...\\n§7Aguarde o resultado!\\n"}]`, 'wheel')
      await api.title(target, '§e§l🎰 ROLETA', '§7girando o seu destino...', 5, 80, 10)
      // Som de roleta acelerando começa
      await api.command(`playsound minecraft:block.note_block.hat master @a ~ ~ ~ 0.4 1.5`, 'wheel')
    } catch {}

    const start = performance.now()
    const duration = 4500

    function step() {
      const t = Math.min(1, (performance.now() - start) / duration)
      const eased = 1 - Math.pow(1 - t, 3)
      angleRef.current = startAngle + (targetAngle - startAngle) * eased
      drawWheel()  // direto, sem setState
      // Som de tick periódico
      if (Math.floor(t * 20) !== Math.floor(((performance.now() - 50 - start) / duration) * 20)) {
        api.sound(target, 'minecraft:block.note_block.hat', 0.3, 1.2 - t * 0.6).catch(() => {})
      }
      if (t < 1) {
        animRef.current = requestAnimationFrame(step)
      } else {
        // Snap final + finish
        animRef.current = null
        finish(winSafe)
      }
    }
    animRef.current = requestAnimationFrame(step)
  }

  async function finish(win: Segment) {
    setSpinning(false)
    setWinner(win)
    setRenderTrigger((v) => v + 1) // garante redraw final
    const player = players.find((p) => p.uuid === target); if (!player) return
    setHistory((h) => [{ ts: Date.now(), player: player.name, segment: win.label }, ...h].slice(0, 30))
    try {
      // Title pro player que tirou
      await api.title(target, `${win.emoji ?? '🎰'} §l${win.label}`, '§7sorteado pra você!', 10, 80, 20)
      await api.sound(target, 'minecraft:entity.player.levelup', 1, 1.2)
      // Broadcast result pra todos
      await api.command(`tellraw @a [{"text":"\\n§5§l🎰 RESULTADO §r\\n"},{"text":"${player.name}","color":"yellow","bold":true},{"text":" §7tirou: "},{"text":"${win.emoji ?? '🎰'} ${win.label.replace(/"/g, '')}","color":"gold","bold":true},{"text":"\\n"}]`, 'wheel')
      await api.command(`playsound minecraft:entity.player.levelup master @a ~ ~ ~ 0.7 1.2`, 'wheel')
      // Roda comando
      if (win.command) {
        const cmd = win.command.split('{player}').join(player.name)
        await api.command(cmd, 'wheel')
      }
    } catch (e: any) { toast.err(e.message) }
  }

  function newSegment() {
    setEditing({
      id: Date.now(),
      label: 'Novo segmento',
      color: PALETTE[segments.length % PALETTE.length],
      weight: 1,
      emoji: '🎁',
      command: 'give {player} minecraft:diamond 1',
    })
  }

  function commit(s: Segment) {
    setSegments((cur) => {
      const i = cur.findIndex((x) => x.id === s.id)
      if (i >= 0) { const n = [...cur]; n[i] = s; return n }
      return [...cur, s]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎰 Roleta de Eventos</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Cria segmentos com peso. Gira, sorteia, executa comando + broadcasta o resultado pra todos in-game.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <select className="input text-sm max-w-xs" value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="">— player —</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
          </select>
          <button className={spinning ? 'btn-amber pulse-glow' : 'btn'} onClick={spin} disabled={!target || spinning || segments.length === 0}>
            {spinning ? '🎰 Girando...' : '🎰 GIRAR'}
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,400px)_minmax(0,1fr)] gap-6">
        <div className="card-glow flex flex-col items-center min-w-0" style={{ maxWidth: 400 }}>
          <canvas ref={canvasRef} className="rounded-lg block" style={{ width: '100%', maxWidth: 380, height: 'auto', aspectRatio: '1 / 1' }} />
          {winner && !spinning && (
            <div className="mt-3 text-center animate-in fade-in">
              <div className="text-3xl mb-1">{winner.emoji}</div>
              <div className="font-bold text-lg" style={{ color: winner.color }}>{winner.label}</div>
            </div>
          )}
          {spinning && (
            <div className="mt-3 text-center">
              <div className="text-sm text-amber-300 font-bold animate-pulse">🎰 Girando...</div>
              <div className="text-xs text-liberthia-300/60">Aguarde o resultado</div>
            </div>
          )}
        </div>

        <div className="space-y-3 min-w-0">
          <div className="card-glow">
            <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
              <h3 className="font-bold">🎯 Segmentos ({segments.length})</h3>
              <div className="flex gap-1">
                <button className="btn-ghost btn-sm" onClick={() => {
                  if (!confirm('Resetar pro deck padrão? Vai apagar seus segmentos custom.')) return
                  // 1) Cópia profunda pra garantir nova referência
                  const fresh = DEFAULT_SEGMENTS.map((s) => ({ ...s }))
                  // 2) Atualiza state (useKvState faz persistência debounced)
                  setSegments(fresh)
                  segmentsRef.current = fresh
                  // 3) Reseta ângulo e força redraw
                  angleRef.current = 0
                  setWinner(null)
                  setRenderTrigger((v) => v + 1)
                  // 4) Chama drawWheel direto no próximo frame
                  requestAnimationFrame(() => drawWheel())
                  toast.ok('↺ Deck padrão restaurado')
                }}>↺ Reset</button>
                <button className="btn-ghost btn-sm" onClick={newSegment}>+ Segmento</button>
              </div>
            </div>
            <div className="space-y-1.5 max-h-[500px] overflow-y-auto">
              {segments.map((s) => (
                <div key={s.id} className="flex items-center gap-2 p-2 rounded-lg bg-liberthia-900/40 border-l-4 min-w-0"
                     style={{ borderLeftColor: s.color }}>
                  <span className="text-2xl shrink-0">{s.emoji ?? '🎁'}</span>
                  <div className="flex-1 min-w-0 overflow-hidden">
                    <div className="font-bold text-sm truncate">{s.label}</div>
                    <div className="text-[10px] text-liberthia-300/50 font-mono truncate">{s.command ?? '—'}</div>
                  </div>
                  <span className="chip shrink-0 text-[10px]">×{s.weight}</span>
                  <button className="btn-ghost btn-sm shrink-0" onClick={() => setEditing(s)}>✎</button>
                  <button className="btn-ghost btn-sm shrink-0" onClick={() => setSegments((cur) => cur.filter((x) => x.id !== s.id))}>🗑</button>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2">📜 Histórico</h3>
            <div className="text-xs space-y-0.5 max-h-48 overflow-y-auto">
              {history.length === 0 && <p className="italic text-liberthia-300/50">— sem giros —</p>}
              {history.map((h, i) => (
                <div key={i}><span className="text-liberthia-300/40">{new Date(h.ts).toLocaleTimeString()}</span> · {h.player} → <b>{h.segment}</b></div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {editing && <SegEditor seg={editing} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function SegEditor({ seg, onSave, onCancel }: { seg: Segment; onSave: (s: Segment) => void; onCancel: () => void }) {
  const [s, setS] = useState<Segment>(seg)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-lg w-full" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🎯 Segmento</h3>
        <div className="grid grid-cols-[60px_1fr_80px] gap-2 mb-3">
          <input className="input text-2xl text-center" value={s.emoji ?? ''} onChange={(e) => setS({ ...s, emoji: e.target.value })} />
          <input className="input" value={s.label} onChange={(e) => setS({ ...s, label: e.target.value })} />
          <input type="color" className="input h-10" value={s.color} onChange={(e) => setS({ ...s, color: e.target.value })} />
        </div>
        <label className="label block mb-1">Peso (probabilidade relativa)</label>
        <input type="number" step={0.1} min={0.1} className="input mb-3" value={s.weight} onChange={(e) => setS({ ...s, weight: Number(e.target.value) })} />
        <label className="label block mb-1">Comando ao ser sorteado ({'{player}'} é substituído)</label>
        <input className="input font-mono text-xs" value={s.command ?? ''} onChange={(e) => setS({ ...s, command: e.target.value || undefined })}
          placeholder="give {player} minecraft:diamond 1" />
        <div className="flex gap-2 justify-end mt-4">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(s)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
