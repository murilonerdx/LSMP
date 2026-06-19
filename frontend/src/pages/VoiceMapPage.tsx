import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api, VoiceClipDto } from '../lib/api'
import { toast } from '../store/toast'
import { useEvents } from '../store/events'

/**
 * Voice Map — clipes plotados num mapa 2D (XZ do mundo) com filtros temporais
 * + ferramenta "encontrar conversas" (clipes próximos no espaço/tempo).
 *
 * Fluxos:
 *  - Filtra por dimensão + janela de tempo + player opcional
 *  - Cada clipe vira pin no mapa (XZ). Hover mostra player + transcrição
 *  - Click no pin → painel direito mostra detalhes + botão "🔍 Achar próximos"
 *  - "Próximos" mostra outros clipes em ±30 blocos e ±5 min — provável conversa
 *  - Seleção múltipla → enviar pra Video Editor OU Voice Conversations
 */

const PRESETS = [
  { label: '15min', minutes: 15 },
  { label: '30min', minutes: 30 },
  { label: '1h', minutes: 60 },
  { label: '6h', minutes: 360 },
  { label: '24h', minutes: 1440 },
  { label: '7d', minutes: 10080 },
  { label: 'tudo', minutes: 0 },
]

const DIMENSION_COLORS: Record<string, string> = {
  'minecraft:overworld': '#22c55e',
  'minecraft:the_nether': '#dc2626',
  'minecraft:the_end': '#a855f7',
}
function dimColor(dim: string | null): string {
  if (!dim) return '#94a3b8'
  return DIMENSION_COLORS[dim] || '#3b82f6'
}

function fmtClock(ts: number): string {
  return new Date(ts).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
}

export function VoiceMapPage() {
  const nav = useNavigate()
  const [lastMinutes, setLastMinutes] = useState(60)
  const [dimension, setDimension] = useState<string>('minecraft:overworld')
  const [selectedClip, setSelectedClip] = useState<VoiceClipDto | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [searchRadius, setSearchRadius] = useState(30)
  const [searchTimeMin, setSearchTimeMin] = useState(5)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  // Pan/zoom do mapa
  const [zoom, setZoom] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  // Live mode — auto-play clipes que chegam via WebSocket
  const [liveMode, setLiveMode] = useState(false)
  const liveAudioRef = useRef<HTMLAudioElement | null>(null)
  // Filtro de live mode: só clipes dentro de N blocos de algum player selecionado
  const [liveAnchorUuid, setLiveAnchorUuid] = useState<string>('')
  const [liveRadius, setLiveRadius] = useState(50)
  // Subscribe WS pra capturar voice_clip_done events
  const events = useEvents((s) => s.events)

  // World map iframe (BlueMap / Dynmap / Squaremap) — opcional.
  // User salva URL no localStorage; se setada, mostra iframe como background.
  const [worldMapUrl, setWorldMapUrl] = useState<string>(
    typeof window !== 'undefined' ? (localStorage.getItem('voice-map-world-url') || '') : ''
  )
  const [showWorldMap, setShowWorldMap] = useState<boolean>(
    typeof window !== 'undefined' ? localStorage.getItem('voice-map-show-world') === 'true' : false
  )
  function saveWorldMapUrl(url: string) {
    setWorldMapUrl(url)
    localStorage.setItem('voice-map-world-url', url)
  }
  function toggleWorldMap(on: boolean) {
    setShowWorldMap(on)
    localStorage.setItem('voice-map-show-world', String(on))
  }

  /** Focar mapa num player específico — zoom + centra offset. */
  function focusPlayer(uuid: string) {
    const p = onlinePlayers.find(p => p.uuid === uuid)
    if (!p || !bounds) return
    // Calcula offset pra centralizar player no canvas
    const canvas = canvasRef.current
    if (!canvas) return
    const W = canvas.width, H = canvas.height, padding = 40
    const rangeX = Math.max(50, bounds.maxX - bounds.minX)
    const rangeZ = Math.max(50, bounds.maxZ - bounds.minZ)
    const newZoom = 2.5
    const scale = Math.min((W - padding * 2) / rangeX, (H - padding * 2) / rangeZ) * newZoom
    const targetX = padding + (p.posX - bounds.minX) * scale
    const targetY = padding + (p.posZ - bounds.minZ) * scale
    setZoom(newZoom)
    setOffset({ x: W / 2 - targetX, y: H / 2 - targetY })
    setLiveAnchorUuid(uuid)
    toast.ok(`📍 Focando em ${p.name}`)
  }

  const fromTs = useMemo(() => {
    if (lastMinutes === 0) return 0
    return Date.now() - lastMinutes * 60_000
  }, [lastMinutes])

  const mapQ = useQuery({
    queryKey: ['voice-map', fromTs, dimension],
    queryFn: () => api.voiceMap({ fromTs, dimension, limit: 1000 }),
    // Refresh agressivo se Live mode tá ON (pra players moverem no mapa em tempo real)
    refetchInterval: liveMode ? 3_000 : 15_000,
  })

  const onlinePlayers = mapQ.data?.onlinePlayers ?? []
  const clipsWithoutPos = mapQ.data?.clipsWithoutPos ?? []

  // ===== Live mode — quando WS emite voice_clip_done, decide se toca =====
  useEffect(() => {
    if (!liveMode || events.length === 0) return
    const lastEvent = events[events.length - 1]
    if (lastEvent.type !== 'voice_clip_done') return
    const clipId = (lastEvent.data as any)?.clipId
    const clipPos = (lastEvent.data as any)?.position
    if (!clipId) return

    // Se anchor selecionado, só toca se clipe está dentro do raio
    if (liveAnchorUuid) {
      const anchor = onlinePlayers.find(p => p.uuid === liveAnchorUuid)
      if (anchor && clipPos) {
        const dx = clipPos.x - anchor.posX
        const dz = clipPos.z - anchor.posZ
        const dist = Math.sqrt(dx * dx + dz * dz)
        if (dist > liveRadius) return
      }
    }

    // Toca o clipe
    if (liveAudioRef.current) {
      try { liveAudioRef.current.pause() } catch {}
    }
    const audio = new Audio(api.voiceAudioUrl(clipId))
    audio.volume = 0.9
    liveAudioRef.current = audio
    audio.play().catch(() => {})
    toast.ok(`🔴 LIVE: clipe #${clipId} tocando`)
    // Força refresh do mapa
    mapQ.refetch()
  }, [events, liveMode, liveAnchorUuid, liveRadius, onlinePlayers])

  useEffect(() => () => {
    if (liveAudioRef.current) try { liveAudioRef.current.pause() } catch {}
  }, [])

  const nearbyQ = useQuery({
    enabled: !!selectedClip,
    queryKey: ['voice-nearby', selectedClip?.id, searchRadius, searchTimeMin],
    queryFn: () => api.voiceNearby(selectedClip!.id, searchRadius, searchTimeMin),
  })

  // Clipes ao redor do PLAYER (não do clipe) — atualizados em tempo real
  const aroundPlayerQ = useQuery({
    enabled: !!liveAnchorUuid,
    queryKey: ['voice-around-player', liveAnchorUuid, liveRadius],
    queryFn: () => api.voiceAroundPlayer(liveAnchorUuid, liveRadius, 30),
    refetchInterval: 5_000, // refresh agressivo - player se move
  })

  // Auto-play do último clipe que apareceu (se diferente do anterior)
  const lastAutoPlayedIdRef = useRef<number | null>(null)
  useEffect(() => {
    if (!liveMode || !aroundPlayerQ.data?.clips || aroundPlayerQ.data.clips.length === 0) return
    const newest = aroundPlayerQ.data.clips[0]?.clip
    if (!newest) return
    // Se é um clipe que ainda não tocamos, toca
    if (lastAutoPlayedIdRef.current === newest.id) return
    // Só toca se for recente (< 2 min)
    if (Date.now() - newest.ts > 120_000) {
      lastAutoPlayedIdRef.current = newest.id
      return
    }
    lastAutoPlayedIdRef.current = newest.id
    if (liveAudioRef.current) try { liveAudioRef.current.pause() } catch {}
    const audio = new Audio(api.voiceAudioUrl(newest.id))
    audio.volume = 0.9
    liveAudioRef.current = audio
    audio.play().catch(() => {})
    toast.ok(`🔴 ${newest.playerName}: ${(aroundPlayerQ.data.clips[0].distance).toFixed(0)}b`)
  }, [aroundPlayerQ.data, liveMode])

  const clips = mapQ.data?.clips ?? []
  const bounds = mapQ.data?.bounds

  // ===== Canvas draw =====
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const W = canvas.width = canvas.offsetWidth
    const H = canvas.height = canvas.offsetHeight
    // Se world map iframe está ativo, NÃO pinta fundo (deixa iframe aparecer)
    if (!showWorldMap || !worldMapUrl) {
      ctx.fillStyle = '#0a0a1a'
      ctx.fillRect(0, 0, W, H)
    } else {
      ctx.clearRect(0, 0, W, H)
    }

    if ((clips.length === 0 && onlinePlayers.length === 0) || !bounds) {
      ctx.fillStyle = '#475569'
      ctx.font = '14px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText('Sem clipes nem players online na dimensão selecionada.', W / 2, H / 2 - 10)
      ctx.fillStyle = '#64748b'
      ctx.font = '11px sans-serif'
      ctx.fillText('Aguarde alguém entrar ou falar, ou troque a dimensão acima.', W / 2, H / 2 + 12)
      return
    }

    const padding = 40
    const rangeX = Math.max(50, bounds.maxX - bounds.minX)
    const rangeZ = Math.max(50, bounds.maxZ - bounds.minZ)
    const scale = Math.min(
      (W - padding * 2) / rangeX,
      (H - padding * 2) / rangeZ
    ) * zoom

    function toCanvas(x: number, z: number) {
      const cx = padding + (x - bounds!.minX) * scale + offset.x
      const cy = padding + (z - bounds!.minZ) * scale + offset.y
      return { x: cx, y: cy }
    }

    // Grid (cada 100 blocos)
    ctx.strokeStyle = '#1e293b'
    ctx.lineWidth = 1
    const gridStep = 100
    const gx0 = Math.floor(bounds.minX / gridStep) * gridStep
    const gx1 = Math.ceil(bounds.maxX / gridStep) * gridStep
    const gz0 = Math.floor(bounds.minZ / gridStep) * gridStep
    const gz1 = Math.ceil(bounds.maxZ / gridStep) * gridStep
    for (let gx = gx0; gx <= gx1; gx += gridStep) {
      const p1 = toCanvas(gx, bounds.minZ)
      const p2 = toCanvas(gx, bounds.maxZ)
      ctx.beginPath(); ctx.moveTo(p1.x, p1.y); ctx.lineTo(p2.x, p2.y); ctx.stroke()
    }
    for (let gz = gz0; gz <= gz1; gz += gridStep) {
      const p1 = toCanvas(bounds.minX, gz)
      const p2 = toCanvas(bounds.maxX, gz)
      ctx.beginPath(); ctx.moveTo(p1.x, p1.y); ctx.lineTo(p2.x, p2.y); ctx.stroke()
    }

    // Voice clip pins
    const now = Date.now()
    for (const c of clips) {
      if (c.posX == null || c.posZ == null) continue
      const p = toCanvas(c.posX, c.posZ)
      const isSelected = selectedClip?.id === c.id
      const isMulti = selectedIds.has(c.id)
      const isNearby = nearbyQ.data?.nearby?.some(n => n.clip.id === c.id)
      // Decay temporal — clipes mais antigos ficam mais transparentes
      const ageMs = now - c.ts
      const opacity = Math.max(0.3, Math.min(1, 1 - ageMs / (4 * 60 * 60 * 1000)))

      ctx.globalAlpha = opacity
      ctx.beginPath()
      ctx.arc(p.x, p.y, isSelected ? 8 : (isNearby ? 6 : 4), 0, Math.PI * 2)
      ctx.fillStyle = isSelected ? '#fbbf24' : (isNearby ? '#a855f7' : dimColor(c.dimension))
      ctx.fill()
      if (isMulti) {
        ctx.strokeStyle = '#22c55e'
        ctx.lineWidth = 2
        ctx.stroke()
      }
      ctx.globalAlpha = 1
    }

    // ONLINE PLAYERS — laranja pulsante, MAIOR
    const pulse = (Math.sin(now / 300) + 1) / 2 // 0..1
    for (const p of onlinePlayers) {
      const pt = toCanvas(p.posX, p.posZ)
      const isAnchor = liveAnchorUuid === p.uuid
      // Pulse ring
      ctx.beginPath()
      ctx.arc(pt.x, pt.y, 14 + pulse * 6, 0, Math.PI * 2)
      ctx.fillStyle = isAnchor ? `rgba(34, 197, 94, ${0.15 + pulse * 0.15})` : `rgba(251, 146, 60, ${0.15 + pulse * 0.15})`
      ctx.fill()
      // Solid center
      ctx.beginPath()
      ctx.arc(pt.x, pt.y, 6, 0, Math.PI * 2)
      ctx.fillStyle = isAnchor ? '#22c55e' : '#fb923c'
      ctx.fill()
      // Name label
      ctx.fillStyle = '#fef3c7'
      ctx.font = 'bold 11px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(p.name, pt.x, pt.y - 14)
      // Live mode radius around anchor
      if (isAnchor && liveMode) {
        ctx.beginPath()
        ctx.arc(pt.x, pt.y, liveRadius * scale, 0, Math.PI * 2)
        ctx.strokeStyle = 'rgba(239, 68, 68, 0.6)'
        ctx.lineWidth = 2
        ctx.setLineDash([6, 4])
        ctx.stroke()
        ctx.setLineDash([])
      }
    }

    // Anchor radius circle
    if (selectedClip && selectedClip.posX != null && selectedClip.posZ != null && nearbyQ.data) {
      const center = toCanvas(selectedClip.posX, selectedClip.posZ)
      ctx.beginPath()
      ctx.arc(center.x, center.y, searchRadius * scale, 0, Math.PI * 2)
      ctx.strokeStyle = 'rgba(251, 191, 36, 0.4)'
      ctx.lineWidth = 1
      ctx.setLineDash([5, 3])
      ctx.stroke()
      ctx.setLineDash([])
    }
  }, [clips, onlinePlayers, bounds, selectedClip, selectedIds, nearbyQ.data, zoom, offset, searchRadius,
      liveMode, liveAnchorUuid, liveRadius])

  // Force redraw every 200ms quando live mode (pra pulse animation)
  useEffect(() => {
    if (!liveMode) return
    const t = setInterval(() => {
      // Trigger re-render via dummy state? Actually, just dispatch a force-redraw
      // simulado: setOffset força re-render do useEffect acima
      setOffset(o => ({ ...o }))
    }, 200)
    return () => clearInterval(t)
  }, [liveMode])

  // Click handling — converte coord canvas pra clip mais próximo
  function handleCanvasClick(e: React.MouseEvent<HTMLCanvasElement>) {
    if (!bounds || clips.length === 0) return
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const cx = e.clientX - rect.left
    const cy = e.clientY - rect.top

    const W = canvas.width, H = canvas.height, padding = 40
    const rangeX = Math.max(50, bounds.maxX - bounds.minX)
    const rangeZ = Math.max(50, bounds.maxZ - bounds.minZ)
    const scale = Math.min((W - padding * 2) / rangeX, (H - padding * 2) / rangeZ) * zoom

    let closest: VoiceClipDto | null = null
    let bestDist = Infinity
    for (const c of clips) {
      if (c.posX == null || c.posZ == null) continue
      const px = padding + (c.posX - bounds.minX) * scale + offset.x
      const py = padding + (c.posZ - bounds.minZ) * scale + offset.y
      const d = Math.hypot(px - cx, py - cy)
      if (d < 12 && d < bestDist) { bestDist = d; closest = c }
    }
    if (closest) {
      setSelectedClip(closest)
      toast.ok(`📍 ${closest.playerName} (${Math.round(closest.posX!)}, ${Math.round(closest.posZ!)})`)
    }
  }

  function toggleMulti(id: number) {
    setSelectedIds(prev => {
      const n = new Set(prev)
      if (n.has(id)) n.delete(id); else n.add(id)
      return n
    })
  }

  function selectNearbyForBatch() {
    if (!nearbyQ.data) return
    const newSet = new Set(selectedIds)
    if (selectedClip) newSet.add(selectedClip.id)
    for (const n of nearbyQ.data.nearby) newSet.add(n.clip.id)
    setSelectedIds(newSet)
    toast.ok(`+ ${newSet.size} clipes selecionados`)
  }

  function sendToConversations() {
    if (selectedIds.size === 0) { toast.err('Selecione clipes primeiro'); return }
    // Pega players únicos dos clipes selecionados
    const selectedClips = clips.filter(c => selectedIds.has(c.id))
    const uuids = Array.from(new Set(selectedClips.map(c => c.playerUuid)))
    const params = new URLSearchParams()
    params.set('players', uuids.join(','))
    params.set('mode', 'precision')
    // Range de tempo = entre primeiro e último clipe
    const ts = selectedClips.map(c => c.ts).sort((a, b) => a - b)
    if (ts.length > 0) {
      params.set('from', String(ts[0]))
      params.set('to', String(ts[ts.length - 1]))
    }
    nav(`/voice-conversations?${params.toString()}`)
  }

  function sendToVideoEditor() {
    if (selectedIds.size === 0) { toast.err('Selecione clipes primeiro'); return }
    // Persiste no sessionStorage pra video editor pegar
    const selectedClips = clips
      .filter(c => selectedIds.has(c.id))
      .sort((a, b) => a.ts - b.ts)
    sessionStorage.setItem('videoEditorPrefill', JSON.stringify({
      clips: selectedClips.map(c => ({
        clipId: c.id,
        playerName: c.playerName,
        playerUuid: c.playerUuid,
        durationMs: c.durationMs,
        transcription: c.transcription,
        realTs: c.ts,
      })),
    }))
    toast.ok(`${selectedIds.size} clipes prontos pra Video Editor`)
    nav('/video-editor')
  }

  return (
    <div className="route-fade max-w-[1800px]">
      <header className="mb-4">
        <h1 className="page-title">🗺 Voice Map</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Onde os players estavam quando falaram. Click num pin pra ver detalhes; "🔍 Próximos"
          acha conversas que aconteceram perto (espaço + tempo). Selecione múltiplos pra mandar
          pro Video Editor ou Voice Conversations.
        </p>
      </header>

      {/* World Map config */}
      <div className="card-glow mb-3">
        <div className="flex items-center gap-2 flex-wrap text-xs">
          <span className="font-bold">🗺 Mapa do mundo (BlueMap/Dynmap/Squaremap):</span>
          <input type="text"
            value={worldMapUrl}
            onChange={(e) => saveWorldMapUrl(e.target.value)}
            placeholder="https://map.seu-servidor.com (opcional)"
            className="input flex-1 min-w-[300px]" />
          {worldMapUrl && (
            <label className="flex items-center gap-1 text-xs cursor-pointer">
              <input type="checkbox" checked={showWorldMap}
                onChange={(e) => toggleWorldMap(e.target.checked)} />
              <span>Mostrar como fundo</span>
            </label>
          )}
        </div>
        <p className="text-[10px] text-liberthia-300/50 mt-1">
          Se você tem BlueMap ou Dynmap no servidor, cole a URL aqui — vai renderizar o mapa real
          do mundo com os pins de voice sobrepostos. Sem URL, usa fundo escuro com grid.
        </p>
      </div>

      {/* Filtros */}
      <div className="card-glow mb-4">
        <div className="flex items-center gap-3 flex-wrap text-xs">
          <span className="font-bold text-liberthia-300/70">⏱ Janela:</span>
          {PRESETS.map(p => (
            <button key={p.minutes}
              className={`btn-ghost btn-sm ${lastMinutes === p.minutes ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
              onClick={() => setLastMinutes(p.minutes)}>
              {p.label}
            </button>
          ))}
          <span className="text-liberthia-300/40">|</span>
          <span className="font-bold text-liberthia-300/70">🌍 Dimensão:</span>
          <select className="input text-xs" value={dimension} onChange={(e) => setDimension(e.target.value)}>
            <option value="minecraft:overworld">Overworld 🟢</option>
            <option value="minecraft:the_nether">Nether 🔴</option>
            <option value="minecraft:the_end">End 🟣</option>
            {(mapQ.data?.dimensions ?? []).filter(d => !['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'].includes(d)).map(d => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
          <span className="badge badge-purple">{clips.length} clipes</span>
          <span className="badge badge-purple">{mapQ.data?.distinctPlayers ?? 0} players</span>
          <span className="badge" style={{background:'rgba(251,146,60,0.3)',color:'#fed7aa'}}>
            {onlinePlayers.length} 🟠 online
          </span>
          <button className="btn-ghost btn-sm ml-auto"
            onClick={() => { setZoom(1); setOffset({ x: 0, y: 0 }) }}>
            🔄 Reset view
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-3">
        {/* ============ Map canvas ============ */}
        <div className="card-glow">
          <div className="flex items-center justify-between mb-2 text-xs">
            <div className="flex items-center gap-2">
              <span>Zoom:</span>
              <input type="range" min={0.5} max={5} step={0.1}
                value={zoom} onChange={(e) => setZoom(Number(e.target.value))}
                className="w-32" />
              <span className="font-mono">{zoom.toFixed(1)}×</span>
            </div>
            <div className="text-liberthia-300/50">
              {bounds && `X: ${Math.round(bounds.minX)}…${Math.round(bounds.maxX)} · Z: ${Math.round(bounds.minZ)}…${Math.round(bounds.maxZ)}`}
            </div>
          </div>
          <div className="relative w-full h-[600px] rounded overflow-hidden bg-liberthia-900/60">
            {showWorldMap && worldMapUrl && (
              <iframe src={worldMapUrl}
                className="absolute inset-0 w-full h-full border-0 opacity-60"
                title="World map" />
            )}
            <canvas ref={canvasRef}
              onClick={handleCanvasClick}
              className="absolute inset-0 w-full h-full cursor-crosshair"
              style={{ background: showWorldMap && worldMapUrl ? 'transparent' : undefined }} />
          </div>
          <div className="text-[10px] text-liberthia-300/50 mt-1">
            🟠 player online (pulse) · 🟢 anchor live · 🟡 selecionado · 🟣 próximos · pequenos = clipes (opacidade = idade)
          </div>
        </div>

        {/* ============ Sidebar: detalhes + actions ============ */}
        <aside className="space-y-3 max-h-[80vh] overflow-y-auto">

          {/* ====== LIVE MODE — escuta tempo real ====== */}
          <div className={`card-glow ${liveMode ? 'ring-2 ring-red-400 bg-red-900/10' : ''}`}>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-bold">
                {liveMode && <span className="inline-block w-2 h-2 rounded-full bg-red-500 animate-pulse mr-2" />}
                🔴 Live Mode
              </h3>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" checked={liveMode}
                  onChange={(e) => setLiveMode(e.target.checked)} className="sr-only peer" />
                <div className="w-11 h-6 bg-liberthia-700 rounded-full peer-checked:bg-red-500 transition relative">
                  <div className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform ${liveMode ? 'translate-x-5' : ''}`} />
                </div>
              </label>
            </div>
            <p className="text-[10px] text-liberthia-300/60 mb-2">
              {liveMode
                ? 'Escutando WebSocket — novos clipes tocam automaticamente.'
                : 'Ative pra ouvir conversas conforme acontecem.'}
            </p>
            {liveMode && (
              <>
                <label className="block text-xs mb-1">
                  <span className="text-liberthia-300/70">Filtrar por player online (opcional):</span>
                  <select className="input w-full text-xs mt-1"
                    value={liveAnchorUuid}
                    onChange={(e) => setLiveAnchorUuid(e.target.value)}>
                    <option value="">📡 Qualquer clipe novo</option>
                    {onlinePlayers.map(p => (
                      <option key={p.uuid} value={p.uuid}>
                        📍 {p.name} — escutar quem fala perto
                      </option>
                    ))}
                  </select>
                </label>
                {liveAnchorUuid && (
                  <label className="block text-xs mt-2">
                    <span className="text-liberthia-300/70">
                      Raio: {liveRadius} blocos
                    </span>
                    <input type="range" min={10} max={200} step={5}
                      value={liveRadius}
                      onChange={(e) => setLiveRadius(Number(e.target.value))}
                      className="w-full" />
                  </label>
                )}
              </>
            )}
          </div>

          {/* ====== Clipes ao redor do anchor (atualiza em tempo real) ====== */}
          {liveAnchorUuid && (
            <div className="card-glow">
              <h3 className="text-sm font-bold mb-1">
                📻 Clipes a {liveRadius}b de{' '}
                <span className="text-amber-300">{aroundPlayerQ.data?.playerName ?? '?'}</span>
              </h3>
              <p className="text-[10px] text-liberthia-300/60 mb-2">
                Atualiza a cada 5s. Click no clipe pra tocar manualmente.
                {liveMode && ' Live Mode toca o mais recente automaticamente.'}
              </p>
              {aroundPlayerQ.data?.error && (
                <div className="text-xs text-red-300">{aroundPlayerQ.data.error}</div>
              )}
              {aroundPlayerQ.data && (aroundPlayerQ.data.count ?? 0) === 0 && (
                <div className="text-xs text-liberthia-300/60 italic">
                  Nenhum clipe nos últimos 30min no raio de {liveRadius} blocos.
                </div>
              )}
              {aroundPlayerQ.data?.clips && aroundPlayerQ.data.clips.length > 0 && (
                <div className="space-y-1 max-h-96 overflow-y-auto">
                  {aroundPlayerQ.data.clips.map(({ clip, distance }) => (
                    <div key={clip.id} className="p-1.5 rounded bg-liberthia-900/40 hover:bg-liberthia-900/70 transition">
                      <div className="flex items-center gap-2">
                        <img src={`https://mc-heads.net/avatar/${encodeURIComponent(clip.playerName)}/20`}
                          className="rounded shrink-0" />
                        <div className="flex-1 min-w-0">
                          <div className="text-xs font-bold truncate">{clip.playerName}</div>
                          <div className="text-[10px] text-liberthia-300/50">
                            {Math.round(distance)}b · {new Date(clip.ts).toLocaleTimeString('pt-BR')}
                          </div>
                        </div>
                        <button className="btn-ghost btn-sm text-[10px] py-0.5 px-1.5"
                          onClick={() => {
                            const audio = new Audio(api.voiceAudioUrl(clip.id))
                            audio.volume = 0.9
                            if (liveAudioRef.current) try { liveAudioRef.current.pause() } catch {}
                            liveAudioRef.current = audio
                            audio.play().catch(() => {})
                          }}>
                          ▶
                        </button>
                      </div>
                      {clip.transcription && (
                        <div className="text-[10px] italic text-liberthia-300/60 line-clamp-2 mt-1 ml-7">
                          "{clip.transcription}"
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ====== Online Players ====== */}
          {onlinePlayers.length > 0 && (
            <div className="card-glow">
              <h3 className="text-sm font-bold mb-2">🟠 Players Online ({onlinePlayers.length})</h3>
              <div className="space-y-1">
                {onlinePlayers.map(p => (
                  <div key={p.uuid} className="rounded bg-liberthia-900/40 p-2 text-xs">
                    <div className="flex items-center gap-2">
                      <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.name)}/28`}
                        className="rounded shrink-0" />
                      <div className="flex-1 min-w-0">
                        <div className="font-bold truncate">{p.name}</div>
                        <div className="text-[10px] text-liberthia-300/50 font-mono">
                          ({Math.round(p.posX)}, {Math.round(p.posY)}, {Math.round(p.posZ)})
                        </div>
                      </div>
                      <span className="text-[10px] text-red-300">❤ {Math.round(p.health)}</span>
                    </div>
                    {(p.nearbyCount ?? 0) > 0 && (
                      <div className="mt-1.5 text-[10px] text-amber-300 flex items-center gap-1">
                        <span className="px-1.5 py-0.5 rounded bg-amber-500/20">
                          👥 +{p.nearbyCount} a 50 blocos
                        </span>
                        <span className="text-liberthia-300/60 truncate">
                          {(p.nearbyNames ?? []).join(', ')}
                        </span>
                      </div>
                    )}
                    <div className="flex gap-1 mt-1.5">
                      <button className="btn-ghost btn-sm flex-1 text-[10px] py-0.5"
                        onClick={() => focusPlayer(p.uuid)}>
                        🔍 Focar
                      </button>
                      <button className="btn-ghost btn-sm flex-1 text-[10px] py-0.5"
                        onClick={async () => {
                          // v92: BUG FIX — antes só ativava liveMode e ficava esperando
                          // um clipe novo aparecer pra tocar (auto-play só aceita clipes
                          // < 2min de idade). Resultado: clicar e nada acontecia.
                          // Agora: busca IMEDIATAMENTE o último clipe do player e toca.
                          setLiveAnchorUuid(p.uuid)
                          setLiveMode(true)
                          try {
                            const r = await api.voiceLibrarySearch({
                              playerUuid: p.uuid,
                              sort: 'recent',
                              limit: 1,
                            })
                            const last = r?.clips?.[0]
                            if (!last) {
                              toast.err(`${p.name} não tem clipes ainda`)
                              return
                            }
                            // Pausa qualquer audio anterior
                            if (liveAudioRef.current) try { liveAudioRef.current.pause() } catch {}
                            const audio = new Audio(api.voiceAudioUrl(last.id))
                            audio.volume = 0.9
                            liveAudioRef.current = audio
                            audio.play().catch((e) => {
                              toast.err('Falha ao tocar — talvez precise interagir com a página primeiro')
                              console.error(e)
                            })
                            // Marca como já tocado pro auto-play não repetir
                            lastAutoPlayedIdRef.current = last.id
                            toast.ok(`🔴 Escutando ${p.name} (último: ${new Date(last.ts).toLocaleTimeString('pt-BR')})`)
                          } catch (e: any) {
                            toast.err(e.message ?? 'erro')
                          }
                        }}>
                        🔴 Escutar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {selectedClip ? (
            <>
              <div className="card-glow">
                <div className="flex items-center gap-2 mb-2">
                  <img src={`https://mc-heads.net/avatar/${encodeURIComponent(selectedClip.playerName)}/32`}
                    className="rounded" />
                  <div className="flex-1 min-w-0">
                    <div className="font-bold truncate">{selectedClip.playerName}</div>
                    <div className="text-[10px] font-mono text-liberthia-300/50">
                      ({Math.round(selectedClip.posX!)}, {Math.round(selectedClip.posY ?? 64)}, {Math.round(selectedClip.posZ!)})
                    </div>
                  </div>
                </div>
                <div className="text-[10px] text-liberthia-300/60">{fmtClock(selectedClip.ts)}</div>
                <audio src={api.voiceAudioUrl(selectedClip.id)} controls
                  className="w-full mt-2 h-8" />
                {selectedClip.transcription && (
                  <div className="mt-2 text-[11px] italic text-liberthia-300/70 whitespace-pre-wrap">
                    "{selectedClip.transcription}"
                  </div>
                )}
                <button className="btn-ghost btn-sm w-full mt-2"
                  onClick={() => toggleMulti(selectedClip.id)}>
                  {selectedIds.has(selectedClip.id) ? '✓ Remover do batch' : '+ Adicionar ao batch'}
                </button>
              </div>

              <div className="card-glow space-y-2">
                <h3 className="text-sm font-bold">🔍 Próximos no espaço-tempo</h3>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <label>
                    <span className="text-liberthia-300/60">Raio (blocos)</span>
                    <input type="number" min={1} max={500} className="input"
                      value={searchRadius} onChange={(e) => setSearchRadius(Math.max(1, Number(e.target.value)))} />
                  </label>
                  <label>
                    <span className="text-liberthia-300/60">Janela (min)</span>
                    <input type="number" min={1} max={60} className="input"
                      value={searchTimeMin} onChange={(e) => setSearchTimeMin(Math.max(1, Number(e.target.value)))} />
                  </label>
                </div>
                {nearbyQ.data?.error && (
                  <div className="text-xs text-red-300">{nearbyQ.data.error}</div>
                )}
                {nearbyQ.data && nearbyQ.data.nearby && nearbyQ.data.nearby.length === 0 && (
                  <div className="text-xs text-liberthia-300/60 italic">
                    Nenhum clipe dentro de {searchRadius}b e ±{searchTimeMin}min.
                  </div>
                )}
                {nearbyQ.data && nearbyQ.data.nearby && nearbyQ.data.nearby.length > 0 && (
                  <>
                    <div className="text-[10px] text-liberthia-300/60">
                      {nearbyQ.data.nearby.length} clipe(s) na vizinhança:
                    </div>
                    <div className="space-y-1 max-h-60 overflow-y-auto">
                      {nearbyQ.data.nearby.map(n => (
                        <button key={n.clip.id}
                          onClick={() => toggleMulti(n.clip.id)}
                          className={`w-full text-left p-1.5 rounded text-[11px] ${
                            selectedIds.has(n.clip.id) ? 'bg-emerald-500/30 ring-1 ring-emerald-400' : 'bg-liberthia-900/40 hover:bg-liberthia-900/70'
                          }`}>
                          <div className="flex justify-between">
                            <span className="font-bold">{n.clip.playerName}</span>
                            <span className="text-liberthia-300/50">
                              {Math.round(n.distance)}b · {n.timeDeltaMs > 0 ? '+' : ''}{Math.round(n.timeDeltaMs / 1000)}s
                            </span>
                          </div>
                          {n.clip.transcription && (
                            <div className="italic text-liberthia-300/60 line-clamp-1 mt-0.5">
                              "{n.clip.transcription}"
                            </div>
                          )}
                        </button>
                      ))}
                    </div>
                    <button className="btn btn-sm w-full bg-purple-500/30 hover:bg-purple-500/50"
                      onClick={selectNearbyForBatch}>
                      + Selecionar TODOS os {nearbyQ.data.nearby.length + 1} pro batch
                    </button>
                  </>
                )}
              </div>
            </>
          ) : (
            <div className="card-glow text-center py-8 text-xs text-liberthia-300/60">
              <div className="text-3xl mb-2 opacity-50">📍</div>
              Click num pin do mapa pra ver detalhes.
            </div>
          )}

          {clipsWithoutPos.length > 0 && (
            <div className="card-glow">
              <h3 className="text-xs font-bold mb-1">⚠ {clipsWithoutPos.length} clipes sem coordenadas</h3>
              <p className="text-[10px] text-liberthia-300/60">
                Esses clipes foram gravados ANTES do mod salvar posição. Aparecem
                normalmente na Voice Library mas não no mapa.
              </p>
            </div>
          )}

          {/* Batch actions */}
          <div className="card-glow space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold">📦 Batch ({selectedIds.size})</span>
              {selectedIds.size > 0 && (
                <button className="btn-ghost btn-sm text-[10px]"
                  onClick={() => setSelectedIds(new Set())}>✕ limpar</button>
              )}
            </div>
            <button className="btn btn-sm w-full bg-blue-500/30 hover:bg-blue-500/50"
              disabled={selectedIds.size === 0}
              onClick={sendToConversations}>
              💬 Mandar pra Voice Conversations
            </button>
            <button className="btn btn-sm w-full bg-purple-500/30 hover:bg-purple-500/50"
              disabled={selectedIds.size === 0}
              onClick={sendToVideoEditor}>
              🎬 Mandar pro Video Editor
            </button>
          </div>
        </aside>
      </div>
    </div>
  )
}
