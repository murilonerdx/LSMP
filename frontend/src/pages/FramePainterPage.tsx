import { useEffect, useRef, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Frame Painter: upload de imagem ou canvas branco, escala pra W×H 128×128 chunks
 * (cada map item é 128x128 px no MC), e gera comandos pra:
 *   1. /give @p filled_map[map=...] — aproximação via /give map
 *   2. Posiciona /summon item_frame nos slots da parede
 *
 * MVP simplificado: mostra grid preview na frente + gera SEQUÊNCIA de comandos
 * que o usuário pode rodar via Console ou Scripts. Persistência local da imagem.
 *
 * NOTE: Vanilla MC não permite write-pixel direto em maps via comando — então
 * usamos o approach de /summon item_frame com /give de mapa zerado (placeholder).
 * Pra poster real precisaria de plugin. Aqui geramos a estrutura + título de cena.
 */

type Painting = { name: string; w: number; h: number; dataUrl: string; updated: number }

export function FramePainterPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [name, setName] = useState('mural-1')
  const [w, setW] = useState(3)
  const [h, setHRows] = useState(2)
  const [dataUrl, setDataUrl] = useState<string>('')
  const [saved, setSaved] = useKvState<Painting[]>('frame_paintings', [])
  const fileRef = useRef<HTMLInputElement>(null)
  const previewRef = useRef<HTMLCanvasElement>(null)

  function persist(list: Painting[]) {
    setSaved(list)
  }

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]; if (!f) return
    const reader = new FileReader()
    reader.onload = () => {
      const url = reader.result as string
      // Resize/crop pra ratio do mural
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        canvas.width = w * 128
        canvas.height = h * 128
        const ctx = canvas.getContext('2d')!
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
        setDataUrl(canvas.toDataURL('image/png'))
      }
      img.src = url
    }
    reader.readAsDataURL(f)
  }

  // Render preview
  useEffect(() => {
    const c = previewRef.current; if (!c) return
    c.width = w * 128; c.height = h * 128
    const ctx = c.getContext('2d')!
    ctx.fillStyle = '#1a1a1a'; ctx.fillRect(0, 0, c.width, c.height)
    if (dataUrl) {
      const img = new Image()
      img.onload = () => {
        ctx.drawImage(img, 0, 0, c.width, c.height)
        // Grade dos frames
        ctx.strokeStyle = 'rgba(170,64,232,0.5)'; ctx.lineWidth = 2
        for (let i = 1; i < w; i++) { ctx.beginPath(); ctx.moveTo(i * 128, 0); ctx.lineTo(i * 128, c.height); ctx.stroke() }
        for (let j = 1; j < h; j++) { ctx.beginPath(); ctx.moveTo(0, j * 128); ctx.lineTo(c.width, j * 128); ctx.stroke() }
      }
      img.src = dataUrl
    } else {
      // Empty grid
      ctx.fillStyle = 'rgba(170,64,232,0.4)'
      ctx.font = '20px Inter'
      ctx.textAlign = 'center'
      ctx.fillText(`${w}×${h} mural · ${w * 128}×${h * 128}px`, c.width / 2, c.height / 2)
      ctx.strokeStyle = 'rgba(170,64,232,0.5)'
      for (let i = 0; i <= w; i++) { ctx.beginPath(); ctx.moveTo(i * 128, 0); ctx.lineTo(i * 128, c.height); ctx.stroke() }
      for (let j = 0; j <= h; j++) { ctx.beginPath(); ctx.moveTo(0, j * 128); ctx.lineTo(c.width, j * 128); ctx.stroke() }
    }
  }, [w, h, dataUrl])

  function save() {
    if (!dataUrl) { toast.err('Faça upload primeiro'); return }
    persist([{ name, w, h, dataUrl, updated: Date.now() }, ...saved.filter((p) => p.name !== name)])
    toast.ok('Salvo')
  }

  /**
   * Constrói mural na frente do player. Math correta:
   *  - look vector (cardinal) = direção que o player encara
   *  - perp = perpendicular horizontal pra crescer as colunas
   *  - Antes de cada frame coloca um bloco "wall" pra a frame ter onde se prender
   *    (item_frame em ar cai mesmo com Fixed=1 em algumas versões)
   *  - Facing do frame = -look (frame encara o player)
   */
  async function generateFrames() {
    if (!target) { toast.err('Selecione player'); return }
    const list = await api.players()
    const p = list.find((x) => x.uuid === target); if (!p) return

    // Yaw: 0=south(+Z), 90=west(-X), 180=north(-Z), 270=east(+X)
    // Calcula direção cardinal mais próxima (NSEW)
    const yawNorm = ((p.position.yaw % 360) + 360) % 360
    // 4 quadrantes de 90° centrados em S/W/N/E
    let lookX = 0, lookZ = 0
    if (yawNorm >= 315 || yawNorm < 45) { lookZ = 1 }       // south
    else if (yawNorm < 135) { lookX = -1 }                   // west
    else if (yawNorm < 225) { lookZ = -1 }                   // north
    else { lookX = 1 }                                       // east

    // Perpendicular (pra colunas): (perpX, perpZ) = rotate 90°
    // Se lookZ=1 (south) → perp = (-1, 0) (vai pra oeste)
    // Se lookX=-1 (west) → perp = (0, -1)
    // Se lookZ=-1 (north) → perp = (1, 0)
    // Se lookX=1 (east) → perp = (0, 1)
    const perpX = -lookZ
    const perpZ = lookX

    // Posição base = 5 blocos na direção do olhar do player
    const baseX = Math.floor(p.position.x + lookX * 5)
    const baseY = Math.floor(p.position.y + 1)
    const baseZ = Math.floor(p.position.z + lookZ * 5)

    // Facing do frame = direção oposta ao look (frame encara o player)
    // 0=down, 1=up, 2=north(-z), 3=south(+z), 4=west(-x), 5=east(+x)
    let facing = 2
    if (lookZ === 1) facing = 2       // player olha south → frame face north
    else if (lookZ === -1) facing = 3 // player olha north → frame face south
    else if (lookX === 1) facing = 4  // player olha east → frame face west
    else if (lookX === -1) facing = 5 // player olha west → frame face east

    // Posição do bloco de suporte (atrás do frame, na direção do look)
    const supportOffX = lookX, supportOffZ = lookZ

    let placed = 0
    let errors = 0
    for (let row = 0; row < h; row++) {
      for (let col = 0; col < w; col++) {
        // Posição do frame
        const fx = baseX + perpX * col
        const fy = baseY + (h - 1 - row)
        const fz = baseZ + perpZ * col
        // Bloco de suporte atrás do frame
        const sx = fx + supportOffX
        const sy = fy
        const sz = fz + supportOffZ
        // 1) garante bloco sólido atrás
        try {
          await api.command(`setblock ${sx} ${sy} ${sz} minecraft:white_concrete keep`, 'frames')
        } catch { errors++ }
        // 2) limpa frame antigo no slot exato (pra reidempotência)
        try {
          await api.command(`kill @e[type=minecraft:glow_item_frame,x=${fx},y=${fy},z=${fz},distance=..1.5]`, 'frames')
        } catch {}
        // 3) summon do frame
        const tagName = JSON.stringify(`${name} ${col + 1},${row + 1}`).replace(/"/g, '\\"')
        const itemTag = `tag:{display:{Name:"${tagName}"}}`
        const nbt = [
          `Facing:${facing}b`,
          `Invulnerable:1b`,
          `Fixed:1b`,
          `Item:{id:"minecraft:filled_map",Count:1b,${itemTag}}`,
          `Tags:["liberthia_frame","liberthia_frame_${name}"]`,
        ].join(',')
        try {
          await api.command(`summon minecraft:glow_item_frame ${fx} ${fy} ${fz} {${nbt}}`, 'frames')
          placed++
        } catch (e: any) {
          errors++
        }
        await new Promise((r) => setTimeout(r, 40))
      }
    }
    if (errors > 0) toast.err(`${placed}/${w * h} frames (${errors} erros)`)
    else toast.ok(`${placed} frames colocados`)
  }

  async function removeFrames() {
    try {
      await api.command(`kill @e[tag=liberthia_frame_${name}]`, 'frames')
      toast.ok(`Frames "${name}" removidos`)
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🖼 Frame Painter</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Carregue imagem, define grid <code className="text-xs">w×h</code> de frames, gera estrutura no mundo.
            Versão MVP — frames vazios nomeados (pixels exigem plugin custom).
          </p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-4">
          <div className="card-glow">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-2 mb-3">
              <input className="input md:col-span-2" placeholder="Nome" value={name} onChange={(e) => setName(e.target.value)} />
              <div className="flex items-center gap-2">
                <label className="label">W</label>
                <input type="number" className="input" min={1} max={16} value={w} onChange={(e) => setW(Math.max(1, Math.min(16, Number(e.target.value))))} />
              </div>
              <div className="flex items-center gap-2">
                <label className="label">H</label>
                <input type="number" className="input" min={1} max={16} value={h} onChange={(e) => setHRows(Math.max(1, Math.min(16, Number(e.target.value))))} />
              </div>
            </div>
            <div className="flex items-center gap-2 mb-3">
              <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={onFile} />
              <button className="btn-ghost flex-1" onClick={() => fileRef.current?.click()}>📁 Upload imagem</button>
              {dataUrl && <button className="btn-ghost" onClick={() => setDataUrl('')}>🗑</button>}
            </div>
            <div className="bg-black/60 rounded-lg p-2 overflow-auto">
              <canvas ref={previewRef} className="border border-liberthia-500/30 rounded mx-auto block" style={{ imageRendering: 'pixelated', maxWidth: '100%' }} />
            </div>
            <div className="text-[10px] text-liberthia-300/50 mt-2 text-center">
              {w * h} frames · {w * 128}×{h * 128}px
            </div>
          </div>

          <div className="card flex items-center gap-2">
            <select className="input flex-1" value={target} onChange={(e) => setTarget(e.target.value)}>
              <option value="">— player de referência —</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
            <button className="btn-cyan" onClick={save}>💾 Save</button>
            <button className="btn-ghost btn-sm" onClick={removeFrames} title="Remove frames com mesma tag">🗑 Remover mural</button>
            <button className="btn" onClick={generateFrames} disabled={!target}>🖼 Construir</button>
          </div>
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">🎨 Galeria</h3>
          {saved.length === 0 && <p className="text-xs text-liberthia-300/50 italic">Vazia</p>}
          <div className="space-y-2">
            {saved.map((p) => (
              <button key={p.name} className="w-full text-left p-2 rounded-lg hover:bg-liberthia-700/30 transition flex items-center gap-2"
                      onClick={() => { setName(p.name); setW(p.w); setHRows(p.h); setDataUrl(p.dataUrl) }}>
                <img src={p.dataUrl} className="w-12 h-12 rounded object-cover border border-liberthia-500/30" style={{ imageRendering: 'pixelated' }} />
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-xs truncate">{p.name}</div>
                  <div className="text-[10px] text-liberthia-300/60">{p.w}×{p.h}</div>
                </div>
                <button className="btn-ghost btn-sm" onClick={(e) => { e.stopPropagation(); persist(saved.filter((x) => x.name !== p.name)) }}>🗑</button>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

