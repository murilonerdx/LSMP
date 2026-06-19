import { useEffect, useMemo, useRef, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Player Heatmap (real-time) — usa o MESMO motor de renderização do Live Map.
 *
 * Diferenças do Live Map:
 *  • Grava amostras de trail por player (kvState localStorage)
 *  • Layer de heatmap agregado (intensidade por região)
 *  • Auto-follow num player específico
 *  • Filtro por player (mostrar/esconder)
 *
 * Igualdades com o Live Map:
 *  • Chunks PNG reais com imageSmoothingEnabled=false (crisp pixel)
 *  • Off-screen culling
 *  • Pan (drag) + zoom (wheel)
 *  • Auto-center na média dos players ao abrir
 *  • Refresh players 1s, chunks 5s
 *  • Cache de chunks com timestamp (re-load a cada 30s)
 */

type Sample = { x: number; z: number; ts: number; dim: string }
type Trail = Record<string /* uuid */, Sample[]>
type ChunkCache = Map<string, { img: HTMLImageElement; loadedAt: number }>

const PALETTE = ['#a78bfa', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#f43f5e', '#84cc16', '#8b5cf6', '#3b82f6']
const REFRESH_MS = 30_000

export function PlayerHeatmapPage() {
  // Igual ao MapPage: refresh 1.5s pra players
  const playersQ = useQuery({
    queryKey: ['players'],
    queryFn: api.players,
    refetchInterval: 1500,
  })
  const players = playersQ.data ?? []

  // dim segue convenção do Live Map: "" = default overworld, ou
  // "minecraft:overworld" / "minecraft:the_nether" / "minecraft:the_end"
  const [dim, setDim] = useState('')
  const chunksQ = useQuery({
    queryKey: ['map-chunks-heat', dim],
    queryFn: () => api.mapChunks(dim),
    refetchInterval: 5_000,
  })

  // Camera (pxPerBlock = zoom, IGUAL Live Map)
  const [cam, setCam] = useState<{ cx: number; cz: number; zoom: number }>({ cx: 0, cz: 0, zoom: 2 })

  // Layers visíveis
  const [showMap, setShowMap] = useState(true)
  const [showHeat, setShowHeat] = useState(true)
  const [showTrails, setShowTrails] = useState(true)

  // Heatmap config
  const [heatRadius, setHeatRadius] = useState(8)
  const [recording, setRecording] = useState(true)
  const [intervalSec, setIntervalSec] = useState(2)
  const [enabledPlayers, setEnabledPlayers] = useState<Record<string, boolean>>({})
  const [followUuid, setFollowUuid] = useState<string | null>(null)

  // Trail storage
  const [trails, setTrails] = useKvState<Trail>('player_heatmap', {})

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const cacheRef = useRef<ChunkCache>(new Map())
  const camRef = useRef(cam); camRef.current = cam
  const [size, setSize] = useState({ w: 1000, h: 700 })
  const [tick, setTick] = useState(0)  // anim pulse

  // ===== Auto-center na média dos players (igual MapPage) =====
  useEffect(() => {
    if (players.length && cam.cx === 0 && cam.cz === 0) {
      const sx = players.reduce((a, p) => a + p.position.x, 0) / players.length
      const sz = players.reduce((a, p) => a + p.position.z, 0) / players.length
      setCam((c) => ({ ...c, cx: sx, cz: sz }))
    }
  }, [players.length])

  // ===== Auto-follow =====
  useEffect(() => {
    if (!followUuid) return
    const p = players.find(x => x.uuid === followUuid)
    if (p) {
      setCam(c => ({ ...c, cx: p.position.x, cz: p.position.z }))
    }
  }, [players, followUuid])

  // ===== Resize observer (igual MapPage) =====
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

  // ===== Pulse animation tick =====
  useEffect(() => {
    const t = setInterval(() => setTick(v => v + 1), 150)
    return () => clearInterval(t)
  }, [])

  // ===== Preload chunks (igual MapPage, com cache-bust após REFRESH_MS) =====
  useEffect(() => {
    if (!showMap) return
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
        setCam((cm) => ({ ...cm }))  // trigger redraw
      }
      img.onerror = () => {}
    }
  }, [chunksQ.data, dim, showMap])

  // ===== Recording de samples =====
  useEffect(() => {
    if (!recording) return
    const t = setInterval(() => {
      setTrails((cur) => {
        const next = { ...cur }
        for (const p of players) {
          const list = next[p.uuid] ?? []
          const last = list[list.length - 1]
          if (last && Date.now() - last.ts < intervalSec * 1000 - 200) continue
          if (last && last.x === Math.floor(p.position.x) && last.z === Math.floor(p.position.z)) continue
          list.push({ x: Math.floor(p.position.x), z: Math.floor(p.position.z), ts: Date.now(), dim: p.dimension })
          if (list.length > 3000) list.shift()
          next[p.uuid] = list
        }
        return next
      })
    }, intervalSec * 1000)
    return () => clearInterval(t)
  }, [recording, intervalSec, players])

  // ===== Derivados =====
  const allUuids = useMemo(() => Object.keys(trails), [trails])
  const playerColor = useMemo(() => {
    const map: Record<string, string> = {}
    allUuids.forEach((u, i) => { map[u] = PALETTE[i % PALETTE.length] })
    players.forEach((p, i) => {
      if (!map[p.uuid]) map[p.uuid] = PALETTE[(allUuids.length + i) % PALETTE.length]
    })
    return map
  }, [allUuids, players])

  // Filtro de dimensão (compatível com vazio = overworld default)
  const currentDim = dim || chunksQ.data?.dimension || 'minecraft:overworld'

  // Avatares (mc-heads)
  const avatarsRef = useRef<Map<string, HTMLImageElement>>(new Map())
  useEffect(() => {
    for (const p of players) {
      if (avatarsRef.current.has(p.uuid)) continue
      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.src = `https://mc-heads.net/avatar/${encodeURIComponent(p.name || p.uuid.slice(0, 8))}/24`
      img.onload = () => { avatarsRef.current.set(p.uuid, img); setCam(cm => ({ ...cm })) }
    }
  }, [players])

  // ===== RENDER LOOP (mesma estrutura do MapPage) =====
  useEffect(() => {
    const c = canvasRef.current
    if (!c) return
    const ctx = c.getContext('2d')!
    c.width = size.w
    c.height = size.h
    const { cx, cz, zoom } = cam
    const pxPerBlock = zoom

    // Fundo
    ctx.fillStyle = '#070310'
    ctx.fillRect(0, 0, c.width, c.height)

    // ===== 1. CHUNKS REAIS (PNG) =====
    if (showMap) {
      const chunks = chunksQ.data?.chunks ?? []
      ctx.imageSmoothingEnabled = false
      for (const ch of chunks) {
        const key = `${dim}:${ch.x},${ch.z}`
        const entry = cacheRef.current.get(key)
        if (!entry) continue
        const wx = ch.x * 16
        const wz = ch.z * 16
        const px = c.width / 2 + (wx - cx) * pxPerBlock
        const py = c.height / 2 + (wz - cz) * pxPerBlock
        const sz = 16 * pxPerBlock
        // Off-screen culling
        if (px + sz < 0 || py + sz < 0 || px > c.width || py > c.height) continue
        ctx.drawImage(entry.img, px, py, sz, sz)
      }
    }

    // ===== 2. ORIGIN MARKER (cruz amarela) =====
    const ox = c.width / 2 - cx * pxPerBlock
    const oy = c.height / 2 - cz * pxPerBlock
    ctx.strokeStyle = 'rgba(255,255,0,0.5)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(ox - 8, oy); ctx.lineTo(ox + 8, oy)
    ctx.moveTo(ox, oy - 8); ctx.lineTo(ox, oy + 8)
    ctx.stroke()

    // ===== 3. HEATMAP AGREGADO =====
    if (showHeat) {
      const grid: Record<string, { cnt: number; color: string }> = {}
      for (const uuid of allUuids) {
        if (enabledPlayers[uuid] === false) continue
        const samples = trails[uuid] ?? []
        const color = playerColor[uuid]
        for (const s of samples) {
          if (s.dim !== currentDim) continue
          const key = `${Math.floor(s.x / heatRadius)}|${Math.floor(s.z / heatRadius)}`
          if (!grid[key]) grid[key] = { cnt: 0, color }
          grid[key].cnt++
        }
      }
      const max = Math.max(1, ...Object.values(grid).map((v) => v.cnt))
      Object.entries(grid).forEach(([k, v]) => {
        const [gx, gz] = k.split('|').map(Number)
        const wx = gx * heatRadius; const wz = gz * heatRadius
        const px = c.width / 2 + (wx - cx) * pxPerBlock
        const py = c.height / 2 + (wz - cz) * pxPerBlock
        const r = Math.max(3, heatRadius * pxPerBlock * 1.2)
        if (px + r < 0 || py + r < 0 || px - r > c.width || py - r > c.height) return
        const intensity = v.cnt / max
        const grad = ctx.createRadialGradient(px, py, 0, px, py, r)
        grad.addColorStop(0, hexAlpha(v.color, 0.45 + intensity * 0.4))
        grad.addColorStop(1, hexAlpha(v.color, 0))
        ctx.fillStyle = grad
        ctx.beginPath(); ctx.arc(px, py, r, 0, Math.PI * 2); ctx.fill()
      })
    }

    // ===== 4. TRILHAS =====
    if (showTrails) {
      for (const uuid of allUuids) {
        if (enabledPlayers[uuid] === false) continue
        const samples = trails[uuid] ?? []
        const color = playerColor[uuid]
        ctx.strokeStyle = hexAlpha(color, 0.6)
        ctx.lineWidth = 2
        ctx.beginPath()
        let started = false
        for (const s of samples) {
          if (s.dim !== currentDim) continue
          const px = c.width / 2 + (s.x - cx) * pxPerBlock
          const py = c.height / 2 + (s.z - cz) * pxPerBlock
          if (!started) { ctx.moveTo(px, py); started = true } else ctx.lineTo(px, py)
        }
        ctx.stroke()
      }
    }

    // ===== 5. PLAYERS AO VIVO (mesma estética do MapPage + pulse + avatar) =====
    const pulse = 1 + Math.sin(tick / 5) * 0.3
    for (const p of players) {
      if (p.dimension !== currentDim) continue
      const px = c.width / 2 + (p.position.x - cx) * pxPerBlock
      const py = c.height / 2 + (p.position.z - cz) * pxPerBlock
      const color = playerColor[p.uuid] ?? '#aa40e8'

      // Glow gradient (igual MapPage)
      const grad = ctx.createRadialGradient(px, py, 0, px, py, 22)
      grad.addColorStop(0, hexAlpha(color, 0.6))
      grad.addColorStop(1, hexAlpha(color, 0))
      ctx.fillStyle = grad
      ctx.beginPath(); ctx.arc(px, py, 22, 0, Math.PI * 2); ctx.fill()

      // Pulse ring extra
      ctx.strokeStyle = hexAlpha(color, 0.7)
      ctx.lineWidth = 2
      ctx.beginPath(); ctx.arc(px, py, 14 * pulse, 0, Math.PI * 2); ctx.stroke()

      // Yaw arrow (igual MapPage)
      const yawRad = (p.position.yaw + 180) * Math.PI / 180
      ctx.strokeStyle = color
      ctx.lineWidth = 2.5
      ctx.beginPath(); ctx.moveTo(px, py)
      ctx.lineTo(px + Math.sin(yawRad) * 16, py - Math.cos(yawRad) * 16)
      ctx.stroke()

      // Avatar circular (sobrepõe o ponto branco)
      const av = avatarsRef.current.get(p.uuid)
      if (av) {
        ctx.save()
        ctx.beginPath(); ctx.arc(px, py, 9, 0, Math.PI * 2); ctx.clip()
        ctx.drawImage(av, px - 9, py - 9, 18, 18)
        ctx.restore()
        ctx.strokeStyle = color; ctx.lineWidth = 2
        ctx.beginPath(); ctx.arc(px, py, 9, 0, Math.PI * 2); ctx.stroke()
      } else {
        ctx.fillStyle = '#fff'
        ctx.beginPath(); ctx.arc(px, py, 6, 0, Math.PI * 2); ctx.fill()
        ctx.strokeStyle = color; ctx.lineWidth = 2; ctx.stroke()
      }

      // Nome + coords (igual MapPage)
      ctx.fillStyle = '#fff'
      ctx.font = 'bold 12px Inter, sans-serif'
      ctx.fillText(p.name, px + 14, py - 8)
      ctx.font = '10px monospace'
      ctx.fillStyle = 'rgba(216,194,255,0.7)'
      ctx.fillText(`${p.position.x.toFixed(0)}, ${p.position.y.toFixed(0)}, ${p.position.z.toFixed(0)}`,
                   px + 14, py + 10)

      // HP bar (igual MapPage)
      const w = 36
      const hp = Math.max(0, Math.min(1, p.health / Math.max(p.maxHealth, 1)))
      ctx.fillStyle = 'rgba(0,0,0,0.6)'
      ctx.fillRect(px - w / 2, py - 22, w, 4)
      ctx.fillStyle = hp > 0.5 ? '#34d399' : hp > 0.25 ? '#fbbf24' : '#ef4444'
      ctx.fillRect(px - w / 2, py - 22, w * hp, 4)

      // Star de follow
      if (followUuid === p.uuid) {
        ctx.fillStyle = '#fbbf24'
        ctx.font = '14px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('★', px, py - 28)
        ctx.textAlign = 'left'
      }
    }
  }, [size, players, cam, chunksQ.data, dim, trails, allUuids, enabledPlayers,
      showMap, showHeat, showTrails, heatRadius, playerColor, followUuid, tick, currentDim])

  // ===== Pan / Zoom / Mouse (igual MapPage) =====
  const dragRef = useRef<{ x: number; y: number; cx: number; cz: number } | null>(null)
  function onMouseDown(e: React.MouseEvent) {
    dragRef.current = { x: e.clientX, y: e.clientY, cx: cam.cx, cz: cam.cz }
    if (followUuid) setFollowUuid(null)  // drag cancela follow
  }
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

  function recenter() {
    if (!players.length) return
    const sx = players.reduce((a, p) => a + p.position.x, 0) / players.length
    const sz = players.reduce((a, p) => a + p.position.z, 0) / players.length
    setCam({ cx: sx, cz: sz, zoom: 2 })
    setFollowUuid(null)
  }
  function followPlayer(p: Player) {
    setFollowUuid(p.uuid)
    setCam({ cx: p.position.x, cz: p.position.z, zoom: 4 })
  }
  function clearAll() {
    if (!confirm('Apagar todas as trilhas?')) return
    setTrails({}); toast.ok('🗑 trilhas apagadas')
  }
  function clearPlayer(uuid: string) {
    setTrails((cur) => { const n = { ...cur }; delete n[uuid]; return n })
  }
  function exportJSON() {
    const blob = new Blob([JSON.stringify(trails, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = `liberthia-heatmap-${Date.now()}.json`; a.click()
    URL.revokeObjectURL(url)
  }

  const onlineHere = players.filter(p => p.dimension === currentDim)

  return (
    <div className="route-fade max-w-[1700px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🗺 Player Heatmap (real-time)</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Render real do mundo + trilhas + heatmap agregado.
            <span className="chip">drag</span> pan ·
            <span className="chip">scroll</span> zoom ·
            click no 🎯 dum player pra <strong>follow</strong>.
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
          <span className="badge badge-purple">{onlineHere.length} online</span>
          <span className="badge badge-purple">zoom {cam.zoom.toFixed(2)}x</span>
          <button className="btn-ghost btn-sm" onClick={recenter}>📍 Center</button>
          <button className={recording ? 'btn-danger btn-sm pulse-glow' : 'btn-success btn-sm'}
            onClick={() => setRecording((v) => !v)}>
            {recording ? '⏺ Gravando' : '⏸ Pausado'}
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_280px] gap-4">
        <div className="card-glow p-2 relative">
          <canvas
            ref={canvasRef}
            className="rounded-xl cursor-grab active:cursor-grabbing select-none"
            style={{ display: 'block', width: '100%' }}
            onMouseDown={onMouseDown}
            onMouseMove={onMouseMove}
            onMouseUp={onMouseUp}
            onMouseLeave={onMouseUp}
            onWheel={onWheel}
          />
          {followUuid && (
            <div className="absolute top-4 left-4 badge badge-purple text-xs animate-pulse">
              🎯 Seguindo {players.find(p => p.uuid === followUuid)?.name ?? '?'}
              <button className="ml-2 hover:text-white" onClick={() => setFollowUuid(null)}>✕</button>
            </div>
          )}
          <div className="absolute bottom-4 right-4 text-[10px] text-liberthia-300/70 font-mono bg-black/60 px-2 py-1 rounded">
            centro: {Math.round(cam.cx)}, {Math.round(cam.cz)}
          </div>
        </div>

        <div className="space-y-3 max-h-[78vh] overflow-y-auto pr-1">
          <div className="card">
            <h3 className="font-bold mb-2 text-sm">🎚 Layers</h3>
            <div className="space-y-1">
              <label className="flex items-center gap-2 text-xs">
                <input type="checkbox" checked={showMap} onChange={(e) => setShowMap(e.target.checked)} />
                🗺 Chunks reais
              </label>
              <label className="flex items-center gap-2 text-xs">
                <input type="checkbox" checked={showHeat} onChange={(e) => setShowHeat(e.target.checked)} />
                🔥 Heatmap
              </label>
              <label className="flex items-center gap-2 text-xs">
                <input type="checkbox" checked={showTrails} onChange={(e) => setShowTrails(e.target.checked)} />
                〰 Trilhas
              </label>
            </div>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2 text-sm">⚙ Heatmap</h3>
            <label className="label">Amostragem: {intervalSec}s</label>
            <input type="range" className="w-full" min={1} max={30}
              value={intervalSec} onChange={(e) => setIntervalSec(Number(e.target.value))} />
            <label className="label mt-2">Resolução heat: {heatRadius}b</label>
            <input type="range" className="w-full" min={2} max={64}
              value={heatRadius} onChange={(e) => setHeatRadius(Number(e.target.value))} />
            <div className="text-[10px] text-liberthia-300/40 mt-2">
              {Object.values(trails).reduce((s, l) => s + l.length, 0)} samples totais
            </div>
            <div className="flex gap-2 mt-2">
              <button className="btn-ghost btn-sm flex-1" onClick={exportJSON}>📥 JSON</button>
              <button className="btn-ghost btn-sm" onClick={clearAll}>🗑</button>
            </div>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2 text-sm">👥 Online ({onlineHere.length})</h3>
            <div className="space-y-1">
              {onlineHere.length === 0 && (
                <p className="text-xs italic text-liberthia-300/50">Ninguém nessa dimensão</p>
              )}
              {onlineHere.map(p => {
                const samples = trails[p.uuid] ?? []
                return (
                  <div key={p.uuid}
                    className={`flex items-center gap-2 rounded px-2 py-1 ${
                      followUuid === p.uuid ? 'bg-amber-500/20 ring-1 ring-amber-400' : 'bg-liberthia-900/40'
                    }`}>
                    <input type="checkbox" checked={enabledPlayers[p.uuid] !== false}
                      onChange={(e) => setEnabledPlayers({ ...enabledPlayers, [p.uuid]: e.target.checked })} />
                    <span style={{ background: playerColor[p.uuid] }} className="w-3 h-3 rounded-full shrink-0" />
                    <img src={`https://mc-heads.net/avatar/${p.uuid}/16`}
                      className="w-4 h-4 rounded shrink-0" alt={p.name} />
                    <span className="text-xs flex-1 truncate font-bold">{p.name}</span>
                    <span className="text-[9px] text-liberthia-300/60 font-mono">{samples.length}</span>
                    <button className="btn-ghost btn-sm" title="Seguir"
                      onClick={() => followPlayer(p)}>🎯</button>
                  </div>
                )
              })}
            </div>
          </div>

          {allUuids.filter(u => !players.some(p => p.uuid === u)).length > 0 && (
            <div className="card">
              <h3 className="font-bold mb-2 text-sm">📦 Trilhas offline</h3>
              <div className="space-y-1 max-h-40 overflow-y-auto">
                {allUuids.filter(u => !players.some(p => p.uuid === u)).map(uuid => {
                  const samples = trails[uuid] ?? []
                  return (
                    <div key={uuid} className="flex items-center gap-2 bg-liberthia-900/40 rounded px-2 py-1">
                      <input type="checkbox" checked={enabledPlayers[uuid] !== false}
                        onChange={(e) => setEnabledPlayers({ ...enabledPlayers, [uuid]: e.target.checked })} />
                      <span style={{ background: playerColor[uuid] }} className="w-3 h-3 rounded-full shrink-0" />
                      <span className="text-[10px] flex-1 truncate font-mono">{uuid.slice(0, 8)}</span>
                      <span className="text-[9px] text-liberthia-300/60 font-mono">{samples.length}</span>
                      <button className="btn-ghost btn-sm" onClick={() => clearPlayer(uuid)}>🗑</button>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function hexAlpha(hex: string, alpha: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r},${g},${b},${alpha})`
}
