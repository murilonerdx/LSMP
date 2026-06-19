import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Live world map. Renderiza chunks reais (PNG top-down enviado pelo mod) +
 * overlay de players (dot, yaw arrow, HP bar, label).
 *
 * Pan (drag), zoom (scroll), context menu (click direito) com ações.
 * Chunks são cacheados como Image() local; refresh a cada 30s.
 */

type Camera = { cx: number; cz: number; zoom: number }
type ChunkCache = Map<string, { img: HTMLImageElement; loadedAt: number }>

const CHUNK_PX = 16
const REFRESH_MS = 30_000

export function MapPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 1500 })
  const players = playersQ.data ?? []
  const [dim, setDim] = useState('')
  const chunksQ = useQuery({
    queryKey: ['map-chunks', dim],
    queryFn: () => api.mapChunks(dim),
    refetchInterval: 5000,
  })

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const cacheRef = useRef<ChunkCache>(new Map())
  const [cam, setCam] = useState<Camera>({ cx: 0, cz: 0, zoom: 2 })
  const camRef = useRef(cam); camRef.current = cam
  const [menu, setMenu] = useState<{ x: number; y: number; worldX: number; worldZ: number } | null>(null)
  const [size, setSize] = useState({ w: 800, h: 600 })

  // Auto-center
  useEffect(() => {
    if (players.length && cam.cx === 0 && cam.cz === 0) {
      const sx = players.reduce((a, p) => a + p.position.x, 0) / players.length
      const sz = players.reduce((a, p) => a + p.position.z, 0) / players.length
      setCam((c) => ({ ...c, cx: sx, cz: sz }))
    }
  }, [players.length])

  // Resize
  useEffect(() => {
    const el = canvasRef.current?.parentElement
    if (!el) return
    const ro = new ResizeObserver(() => {
      const r = el.getBoundingClientRect()
      setSize({ w: Math.floor(r.width), h: Math.max(450, Math.floor(window.innerHeight - 220)) })
    })
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  // Pre-load chunks
  useEffect(() => {
    const list = chunksQ.data?.chunks ?? []
    const cache = cacheRef.current
    const now = Date.now()
    for (const c of list) {
      const key = `${dim}:${c.x},${c.z}`
      const cur = cache.get(key)
      if (cur && now - cur.loadedAt < REFRESH_MS) continue
      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.src = api.mapChunkPngUrl(c.x, c.z, dim) + (cur ? `&_t=${now}` : '')
      img.onload = () => {
        cache.set(key, { img, loadedAt: now })
        // Trigger redraw
        setCam((cm) => ({ ...cm }))
      }
      img.onerror = () => {
        // Mantém antigo se falhou
      }
    }
  }, [chunksQ.data, dim])

  // Render loop (re-runs on cam, players, cache changes)
  useEffect(() => {
    const c = canvasRef.current
    if (!c) return
    const ctx = c.getContext('2d')!
    c.width = size.w
    c.height = size.h
    const { cx, cz, zoom } = cam
    const pxPerBlock = zoom

    ctx.fillStyle = '#070310'
    ctx.fillRect(0, 0, c.width, c.height)

    // Chunks
    const chunks = chunksQ.data?.chunks ?? []
    for (const ch of chunks) {
      const key = `${dim}:${ch.x},${ch.z}`
      const entry = cacheRef.current.get(key)
      if (!entry) continue
      // World pos do chunk (block coords)
      const wx = ch.x * 16
      const wz = ch.z * 16
      const px = c.width / 2 + (wx - cx) * pxPerBlock
      const py = c.height / 2 + (wz - cz) * pxPerBlock
      const sz = 16 * pxPerBlock
      // Off-screen culling
      if (px + sz < 0 || py + sz < 0 || px > c.width || py > c.height) continue
      ctx.imageSmoothingEnabled = false
      ctx.drawImage(entry.img, px, py, sz, sz)
    }

    // Loaded chunks border (thin)
    if (chunksQ.data?.bbox && chunks.length > 0) {
      ctx.strokeStyle = 'rgba(170,64,232,0.15)'
      ctx.lineWidth = 1
      for (const ch of chunks) {
        const wx = ch.x * 16, wz = ch.z * 16
        const px = c.width / 2 + (wx - cx) * pxPerBlock
        const py = c.height / 2 + (wz - cz) * pxPerBlock
        const sz = 16 * pxPerBlock
        ctx.strokeRect(px, py, sz, sz)
      }
    }

    // Origin
    const ox = c.width / 2 - cx * pxPerBlock
    const oy = c.height / 2 - cz * pxPerBlock
    ctx.strokeStyle = 'rgba(255,255,0,0.5)'
    ctx.lineWidth = 1
    ctx.beginPath(); ctx.moveTo(ox - 8, oy); ctx.lineTo(ox + 8, oy); ctx.moveTo(ox, oy - 8); ctx.lineTo(ox, oy + 8); ctx.stroke()

    // Players
    for (const p of players) {
      if (p.dimension !== chunksQ.data?.dimension && dim) continue
      const px = c.width / 2 + (p.position.x - cx) * pxPerBlock
      const py = c.height / 2 + (p.position.z - cz) * pxPerBlock

      const grad = ctx.createRadialGradient(px, py, 0, px, py, 22)
      grad.addColorStop(0, 'rgba(170,64,232,0.6)')
      grad.addColorStop(1, 'rgba(170,64,232,0)')
      ctx.fillStyle = grad
      ctx.beginPath(); ctx.arc(px, py, 22, 0, Math.PI * 2); ctx.fill()

      const yawRad = (p.position.yaw + 180) * Math.PI / 180
      ctx.strokeStyle = '#aa40e8'
      ctx.lineWidth = 2.5
      ctx.beginPath(); ctx.moveTo(px, py)
      ctx.lineTo(px + Math.sin(yawRad) * 16, py - Math.cos(yawRad) * 16)
      ctx.stroke()

      ctx.fillStyle = '#fff'
      ctx.beginPath(); ctx.arc(px, py, 6, 0, Math.PI * 2); ctx.fill()
      ctx.strokeStyle = '#aa40e8'; ctx.lineWidth = 2; ctx.stroke()

      ctx.fillStyle = '#fff'
      ctx.font = 'bold 12px Inter, sans-serif'
      ctx.fillText(p.name, px + 12, py - 8)
      ctx.font = '10px monospace'
      ctx.fillStyle = 'rgba(216,194,255,0.7)'
      ctx.fillText(`${p.position.x.toFixed(0)}, ${p.position.y.toFixed(0)}, ${p.position.z.toFixed(0)}`, px + 12, py + 10)

      const w = 36
      const hp = Math.max(0, Math.min(1, p.health / Math.max(p.maxHealth, 1)))
      ctx.fillStyle = 'rgba(0,0,0,0.6)'
      ctx.fillRect(px - w / 2, py - 22, w, 4)
      ctx.fillStyle = hp > 0.5 ? '#34d399' : hp > 0.25 ? '#fbbf24' : '#ef4444'
      ctx.fillRect(px - w / 2, py - 22, w * hp, 4)
    }
  }, [size, players, cam, chunksQ.data])

  // Pan
  const dragRef = useRef<{ x: number; y: number; cx: number; cz: number } | null>(null)
  function onMouseDown(e: React.MouseEvent) { setMenu(null); dragRef.current = { x: e.clientX, y: e.clientY, cx: cam.cx, cz: cam.cz } }
  function onMouseMove(e: React.MouseEvent) {
    if (!dragRef.current) return
    const dx = (e.clientX - dragRef.current.x) / cam.zoom
    const dy = (e.clientY - dragRef.current.y) / cam.zoom
    setCam({ ...cam, cx: dragRef.current.cx - dx, cz: dragRef.current.cz - dy })
  }
  function onMouseUp() { dragRef.current = null }
  function onWheel(e: React.WheelEvent) {
    const factor = e.deltaY < 0 ? 1.25 : 0.8
    setCam((c) => ({ ...c, zoom: Math.max(0.25, Math.min(20, c.zoom * factor)) }))
  }
  function onContextMenu(e: React.MouseEvent) {
    e.preventDefault()
    const rect = (e.target as HTMLCanvasElement).getBoundingClientRect()
    const px = e.clientX - rect.left
    const py = e.clientY - rect.top
    const worldX = cam.cx + (px - rect.width / 2) / cam.zoom
    const worldZ = cam.cz + (py - rect.height / 2) / cam.zoom
    setMenu({ x: e.clientX, y: e.clientY, worldX, worldZ })
  }

  function recenter() {
    if (!players.length) return
    const sx = players.reduce((a, p) => a + p.position.x, 0) / players.length
    const sz = players.reduce((a, p) => a + p.position.z, 0) / players.length
    setCam({ cx: sx, cz: sz, zoom: 2 })
  }

  function followPlayer(p: Player) {
    setCam({ cx: p.position.x, cz: p.position.z, zoom: 4 })
  }

  return (
    <div className="route-fade max-w-[1700px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🗺 Live World Map</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Render real do mundo (top-down). <span className="chip">drag</span> pan,
            <span className="chip">scroll</span> zoom,
            <span className="chip">click direito</span> menu de ações.
          </p>
        </div>
        <div className="flex gap-2 items-center flex-wrap">
          <select className="input max-w-xs text-xs" value={dim} onChange={(e) => setDim(e.target.value)}>
            <option value="">overworld (default)</option>
            <option value="minecraft:overworld">overworld</option>
            <option value="minecraft:the_nether">nether</option>
            <option value="minecraft:the_end">end</option>
          </select>
          <span className="badge badge-purple">{chunksQ.data?.chunks?.length ?? 0} chunks</span>
          <span className="badge badge-purple">zoom {cam.zoom.toFixed(2)}x</span>
          <button className="btn-ghost btn-sm" onClick={recenter}>📍 Center</button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_220px] gap-4">
        <div className="card-glow p-2">
          <canvas
            ref={canvasRef}
            className="rounded-xl cursor-grab active:cursor-grabbing select-none"
            style={{ display: 'block', width: '100%' }}
            onMouseDown={onMouseDown}
            onMouseMove={onMouseMove}
            onMouseUp={onMouseUp}
            onMouseLeave={onMouseUp}
            onWheel={onWheel}
            onContextMenu={onContextMenu}
          />
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">👥 Players</h3>
          <div className="space-y-1 max-h-96 overflow-y-auto">
            {players.length === 0 && <p className="text-xs text-liberthia-300/50 italic">Ninguém online</p>}
            {players.map((p) => (
              <button key={p.uuid} onClick={() => followPlayer(p)}
                className="w-full text-left px-2 py-1.5 rounded-lg hover:bg-liberthia-700/30 transition flex items-center gap-2 text-xs">
                <img src={`https://mc-heads.net/avatar/${p.uuid}/20`} className="w-5 h-5 rounded" />
                <span className="flex-1">{p.name}</span>
                <span className="text-liberthia-300/50">📍</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {menu && <ContextMenu menu={menu} players={players} onClose={() => setMenu(null)} />}
    </div>
  )
}

function ContextMenu({ menu, players, onClose }: {
  menu: { x: number; y: number; worldX: number; worldZ: number }
  players: Player[]
  onClose: () => void
}) {
  const [selectedPlayer, setSelectedPlayer] = useState(players[0]?.uuid ?? '')
  const x = Math.round(menu.worldX), z = Math.round(menu.worldZ), y = 80

  async function tpHere() {
    if (!selectedPlayer) return
    try { await api.teleport(selectedPlayer, x, y, z); toast.ok(`TP → ${x}, ${y}, ${z}`) }
    catch (e: any) { toast.err(e.message) }
    onClose()
  }
  async function spawn(entity: string) {
    try { await api.spawnEntity(entity, x, y, z, 1); toast.ok(`spawned ${entity}`) }
    catch (e: any) { toast.err(e.message) }
    onClose()
  }
  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} />
      <div
        className="fixed z-50 card !p-2 min-w-[260px]"
        style={{ left: Math.min(menu.x, window.innerWidth - 280), top: Math.min(menu.y, window.innerHeight - 380) }}
      >
        <div className="px-2 py-1 text-xs font-mono text-liberthia-300/70 border-b border-liberthia-500/20 mb-2">
          📍 X={x} Y={y} Z={z}
        </div>
        <select className="input mb-2 text-xs" value={selectedPlayer} onChange={(e) => setSelectedPlayer(e.target.value)}>
          <option value="">— player —</option>
          {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
        </select>
        <button className="btn-ghost w-full text-left text-xs mb-1" onClick={tpHere} disabled={!selectedPlayer}>🌀 TP player aqui</button>
        <div className="text-[10px] uppercase text-liberthia-300/40 px-1 mt-2 mb-1">Spawn</div>
        <div className="grid grid-cols-2 gap-1">
          <button className="btn-ghost btn-sm" onClick={() => spawn('minecraft:zombie')}>🧟 Zombie</button>
          <button className="btn-ghost btn-sm" onClick={() => spawn('minecraft:creeper')}>💚 Creeper</button>
          <button className="btn-ghost btn-sm" onClick={() => spawn('minecraft:lightning_bolt')}>⚡ Lightning</button>
          <button className="btn-ghost btn-sm" onClick={() => spawn('minecraft:tnt')}>💣 TNT</button>
        </div>
        <button className="btn-ghost w-full text-left text-xs mt-2 mb-1" onClick={() => { api.particle('minecraft:end_rod', x, y, z, 60); onClose(); toast.ok('✨') }}>
          ✨ Partículas
        </button>
        <button className="btn-danger btn-sm w-full" onClick={() => { api.explosion(x, y, z, 4); onClose(); toast.ok('💥') }}>💥 Explosão</button>
      </div>
    </>
  )
}
