import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, VoiceClipDto, VoicePlayerSummary } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Video Editor — compõe um MP4 misturando voice clips do servidor, música
 * de fundo, imagem de fundo, e timing.
 *
 * UX:
 *  1. Upload background image + (opcional) música
 *  2. Browse voice clips por player → adiciona ao timeline com 1 click
 *  3. Ajusta start time + volume de cada clipe
 *  4. Preview tocando os áudios em ordem (com offsets, browser-side)
 *  5. "Renderizar" → manda projeto pro backend → ffmpeg roda → MP4 download
 *
 * Render é assíncrono: jobId + polling pra status.
 */

type Track = {
  clipId: number
  playerName: string
  playerUuid: string
  startMs: number       // posição no vídeo (timeline)
  durationMs: number
  volume: number
  realTs: number        // timestamp REAL do clipe original (pra ordenar cronologicamente)
  transcription?: string | null
}

function fmtTime(ms: number): string {
  const s = Math.floor(ms / 1000)
  const min = Math.floor(s / 60)
  const sec = s % 60
  const milli = ms % 1000
  return `${min}:${String(sec).padStart(2, '0')}.${String(Math.floor(milli / 10)).padStart(2, '0')}`
}
function fmtSec(ms: number): string {
  return (ms / 1000).toFixed(1) + 's'
}
function fmtClock(ts: number): string {
  return new Date(ts).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
}

export function VideoEditorPage() {
  // Assets uploaded
  const [bgImageId, setBgImageId] = useState<string | null>(null)
  const [bgImageName, setBgImageName] = useState<string>('')
  const [bgImagePreview, setBgImagePreview] = useState<string>('')
  const [musicId, setMusicId] = useState<string | null>(null)
  const [musicName, setMusicName] = useState<string>('')
  const [musicLocalUrl, setMusicLocalUrl] = useState<string>('') // blob URL pra preview no browser
  const [musicVolume, setMusicVolume] = useState<number>(0.3)
  const [musicLoop, setMusicLoop] = useState<boolean>(true)
  const [title, setTitle] = useState<string>('')

  // Timeline tracks
  const [tracks, setTracks] = useState<Track[]>([])

  // Render job
  const [activeJobId, setActiveJobId] = useState<string | null>(null)
  const [rendering, setRendering] = useState(false)
  const [uploadingBg, setUploadingBg] = useState(false)
  const [uploadingMusic, setUploadingMusic] = useState(false)

  // Voice library — players + clipes
  const playersQ = useQuery({
    queryKey: ['voice-library-players-for-editor'],
    queryFn: api.voiceLibraryPlayers,
    refetchInterval: 30_000,
  })
  const [selectedPlayer, setSelectedPlayer] = useState<VoicePlayerSummary | null>(null)
  // Sidebar tem 2 modos: "byPlayer" (escolhe player → vê clipes dele) ou
  // "allMixed" (todos clipes do servidor ordenados por timestamp).
  const [sidebarMode, setSidebarMode] = useState<'byPlayer' | 'allMixed'>('byPlayer')
  const [sidebarSearch, setSidebarSearch] = useState('')

  const clipsQ = useQuery({
    enabled: !!selectedPlayer && sidebarMode === 'byPlayer',
    queryKey: ['voice-library-clips-editor', selectedPlayer?.playerUuid],
    queryFn: () => api.voiceLibrarySearch({
      playerUuid: selectedPlayer!.playerUuid,
      sort: 'recent',
      limit: 500,
    }),
  })
  // All clips mode — busca TODOS os clipes do servidor ordenados por ts desc
  const allClipsQ = useQuery({
    enabled: sidebarMode === 'allMixed',
    queryKey: ['voice-library-clips-all-editor'],
    queryFn: () => api.voiceLibrarySearch({
      playerUuid: '',
      sort: 'recent',
      limit: 500,
    }),
  })

  // Preview (browser-side audio playback com offsets)
  // Acumula TODOS os elementos de audio criados em algum momento (não só os do
  // clipId atual). Garante stop completo mesmo se mesmo clip foi adicionado N×.
  const allAudioElementsRef = useRef<HTMLAudioElement[]>([])
  const musicAudioRef = useRef<HTMLAudioElement | null>(null)
  const previewStartTsRef = useRef<number>(0)
  const previewTimerRef = useRef<number | null>(null)
  // Lista de timeouts pendentes — limpa em stopPreview pra evitar áudios
  // disparando depois que user clicou stop ou começou novo preview.
  const previewTimeoutsRef = useRef<number[]>([])
  // Session id — incrementa a cada start; setTimeout verifica antes de tocar
  // pra não disparar áudio de sessão antiga já cancelada.
  const previewSessionRef = useRef<number>(0)
  const [previewing, setPreviewing] = useState(false)
  const [previewMs, setPreviewMs] = useState(0)

  // Job status polling
  const statusQ = useQuery({
    enabled: !!activeJobId,
    queryKey: ['video-job', activeJobId],
    queryFn: () => api.videoStatus(activeJobId!),
    refetchInterval: (q) => {
      const s = (q.state.data as any)?.status
      return s === 'DONE' || s === 'FAILED' ? false : 2_000
    },
  })

  const totalDurationMs = useMemo(() => {
    if (tracks.length === 0) return 5000
    return Math.max(...tracks.map(t => t.startMs + t.durationMs)) + 1000
  }, [tracks])

  // Detecta se backend tem os endpoints do video editor (HTTP 404 = não)
  const [backendOutdated, setBackendOutdated] = useState(false)

  // Save-to-server modal
  const [showSaveModal, setShowSaveModal] = useState(false)
  const [saveTitle, setSaveTitle] = useState('')
  const [saveDescription, setSaveDescription] = useState('')
  const [saveIsPublic, setSaveIsPublic] = useState(true)
  const [savingToServer, setSavingToServer] = useState(false)

  // ===== Asset uploads =====
  function handleUploadError(e: any) {
    const msg = e?.message || String(e)
    if (msg.includes('HTTP 404') || msg.includes('endpoint_not_found')) {
      setBackendOutdated(true)
      toast.err('Backend desatualizado — atualize a imagem pra v51+')
    } else {
      toast.err(msg)
    }
  }
  async function uploadBg(file: File) {
    setUploadingBg(true)
    try {
      const r = await api.videoUploadAsset(file, 'image')
      if (r.ok && r.assetId) {
        setBgImageId(r.assetId)
        setBgImageName(file.name)
        setBgImagePreview(URL.createObjectURL(file))
        setBackendOutdated(false)
        toast.ok('🖼 Imagem carregada')
      } else {
        toast.err(r.error || 'Falha upload')
      }
    } catch (e: any) { handleUploadError(e) }
    setUploadingBg(false)
  }
  async function uploadMusic(file: File) {
    setUploadingMusic(true)
    try {
      const r = await api.videoUploadAsset(file, 'music')
      if (r.ok && r.assetId) {
        setMusicId(r.assetId)
        setMusicName(file.name)
        // Guarda blob URL local pra tocar durante preview no browser
        if (musicLocalUrl) URL.revokeObjectURL(musicLocalUrl)
        setMusicLocalUrl(URL.createObjectURL(file))
        setBackendOutdated(false)
        toast.ok('🎵 Música carregada (vai tocar no preview também)')
      } else {
        toast.err(r.error || 'Falha upload')
      }
    } catch (e: any) { handleUploadError(e) }
    setUploadingMusic(false)
  }

  // ===== Timeline ops =====
  function addTrack(clip: VoiceClipDto) {
    // Auto-position: after last track + 200ms gap
    const lastEnd = tracks.length > 0
      ? Math.max(...tracks.map(t => t.startMs + t.durationMs)) + 200
      : 0
    setTracks(prev => [...prev, {
      clipId: clip.id,
      playerName: clip.playerName,
      playerUuid: clip.playerUuid,
      startMs: lastEnd,
      durationMs: clip.durationMs,
      volume: 1.0,
      realTs: clip.ts,
      transcription: clip.transcription,
    }])
    toast.ok(`+ "${clip.playerName}" no timeline`)
  }
  function updateTrack(idx: number, patch: Partial<Track>) {
    setTracks(prev => prev.map((t, i) => i === idx ? { ...t, ...patch } : t))
  }
  function removeTrack(idx: number) {
    setTracks(prev => prev.filter((_, i) => i !== idx))
  }
  function autoArrange() {
    // Coloca tracks em sequência sem overlap (200ms gap), mantendo ordem atual.
    let cursor = 0
    setTracks(prev => prev.map(t => {
      const newStart = cursor
      cursor = newStart + t.durationMs + 200
      return { ...t, startMs: newStart }
    }))
    toast.ok('⏩ Tracks alinhadas em sequência')
  }
  /**
   * Reordena cronologicamente: pega o realTs de cada clipe e:
   *  - Ordena tracks ascendente por realTs (conversa do passado pro presente)
   *  - Preserva os GAPS reais entre falas (proporcionais), mas capa gap máximo
   *    em 1500ms pra evitar vídeos com horas de silêncio.
   *
   * Exemplo: se Steve falou 10:00:00 e Maria falou 10:00:05 e Pedro falou
   * 11:30:00 (1h30 depois), o gap Steve→Maria fica 5s no video, e Maria→Pedro
   * fica capado em 1.5s.
   */
  function sortChronological() {
    if (tracks.length === 0) return
    const sorted = [...tracks].sort((a, b) => a.realTs - b.realTs)
    const anchorTs = sorted[0].realTs
    let cursor = 0
    let prevRealEnd = anchorTs
    const rearranged = sorted.map((t, idx) => {
      if (idx === 0) {
        cursor = 0
        prevRealEnd = t.realTs + t.durationMs
        return { ...t, startMs: 0 }
      }
      // Gap real entre fim do anterior e start desse clipe
      const realGap = Math.max(0, t.realTs - prevRealEnd)
      // Capa em 1500ms — gap grandes não ajudam no vídeo
      const cappedGap = Math.min(1500, realGap)
      cursor = cursor + (sorted[idx - 1].durationMs) + cappedGap
      prevRealEnd = t.realTs + t.durationMs
      return { ...t, startMs: cursor }
    })
    setTracks(rearranged)
    toast.ok('🕒 Tracks ordenadas cronologicamente (gaps reais até 1.5s)')
  }

  // ===== Preview (browser-side mix) =====
  function startPreview() {
    if (tracks.length === 0) { toast.err('Sem tracks'); return }
    // Para tudo que estava rodando ANTES de incrementar session
    stopPreview()
    const mySession = ++previewSessionRef.current
    setPreviewing(true)
    previewStartTsRef.current = Date.now()
    setPreviewMs(0)

    // Música de fundo — toca imediatamente, com volume + loop do slider
    if (musicLocalUrl) {
      const music = new Audio(musicLocalUrl)
      music.volume = Math.min(1, musicVolume)
      music.loop = musicLoop
      musicAudioRef.current = music
      allAudioElementsRef.current.push(music)
      music.play().catch(err => console.warn('music preview blocked:', err))
    }

    // Schedule each voice track. Cria audio JÁ NA HORA (não dentro do
    // setTimeout) — assim stopPreview consegue pausá-lo mesmo se ainda
    // não tocou. E só dispara play se a sessão ainda for válida.
    for (const t of tracks) {
      const audio = new Audio(api.voiceAudioUrl(t.clipId))
      audio.volume = Math.min(1, t.volume)
      allAudioElementsRef.current.push(audio)
      const timerId = window.setTimeout(() => {
        // Checa session — se user já clicou stop ou re-startou, descarta
        if (previewSessionRef.current !== mySession) return
        audio.play().catch(() => {})
      }, t.startMs)
      previewTimeoutsRef.current.push(timerId)
    }

    // Auto-stop after total duration
    previewTimerRef.current = window.setInterval(() => {
      if (previewSessionRef.current !== mySession) return
      const elapsed = Date.now() - previewStartTsRef.current
      setPreviewMs(elapsed)
      if (elapsed >= totalDurationMs) stopPreview()
    }, 100)
  }
  function stopPreview() {
    // Invalida sessão — qualquer setTimeout/setInterval pendente fica no-op
    previewSessionRef.current++
    setPreviewing(false)

    // Cancela timer de progresso
    if (previewTimerRef.current) {
      clearInterval(previewTimerRef.current)
      previewTimerRef.current = null
    }
    // Cancela TODOS os setTimeouts agendados pra tocar audios
    for (const t of previewTimeoutsRef.current) {
      clearTimeout(t)
    }
    previewTimeoutsRef.current = []

    // Para TODOS os audio elements (não só os "ativos") — inclui os que
    // o setTimeout ainda nem disparou, e os de previews antigos que ainda
    // tinham referência em memória.
    for (const a of allAudioElementsRef.current) {
      try { a.pause(); a.currentTime = 0; a.src = '' } catch {}
    }
    allAudioElementsRef.current = []
    musicAudioRef.current = null
  }

  // Atualiza volume da música em tempo real durante preview
  useEffect(() => {
    if (musicAudioRef.current) {
      musicAudioRef.current.volume = Math.min(1, musicVolume)
    }
  }, [musicVolume])

  useEffect(() => () => {
    stopPreview()
    if (musicLocalUrl) URL.revokeObjectURL(musicLocalUrl)
    if (bgImagePreview) URL.revokeObjectURL(bgImagePreview)
  }, [])

  async function saveToServer() {
    if (!job?.id || !jobDone) return
    if (!saveTitle.trim()) { toast.err('Título obrigatório'); return }
    setSavingToServer(true)
    try {
      const v = await api.savedVideoFromRender(job.id, {
        title: saveTitle.trim(),
        description: saveDescription,
        isPublic: saveIsPublic,
      })
      toast.ok(`💾 Salvo como "${v.title}" (id ${v.id})`)
      setShowSaveModal(false)
      setSaveTitle(''); setSaveDescription('')
    } catch (e: any) { toast.err(e?.message ?? 'Falha ao salvar') }
    setSavingToServer(false)
  }

  /**
   * Download autenticado do MP4. `<a href download>` não manda header
   * Authorization, então o backend retornava 401. Solução: fetch com token,
   * cria blob URL temporário, dispara click sintético no link.
   */
  async function downloadMp4(jobId: string, filename: string) {
    try {
      const token = localStorage.getItem('liberthia.token')
      const r = await fetch(api.videoDownloadUrl(jobId), {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      if (!r.ok) {
        toast.err(`Download falhou: HTTP ${r.status}`)
        return
      }
      const blob = await r.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      setTimeout(() => URL.revokeObjectURL(url), 5_000)
      toast.ok('⬇ Download iniciado')
    } catch (e: any) {
      toast.err(`Erro: ${e?.message ?? e}`)
    }
  }

  // ===== Render =====
  async function startRender() {
    if (tracks.length === 0) { toast.err('Adicione pelo menos 1 voice clip'); return }
    setRendering(true)
    try {
      const r = await api.videoRender({
        backgroundImageId: bgImageId,
        musicId: musicId,
        musicVolume,
        musicLoop,
        voiceClips: tracks.map(t => ({
          clipId: t.clipId,
          startMs: t.startMs,
          volume: t.volume,
        })),
        title,
      })
      if (r.ok && r.jobId) {
        setActiveJobId(r.jobId)
        toast.ok(`🎬 Render iniciado: job ${r.jobId}`)
      } else {
        toast.err(r.error || 'Falha ao iniciar render')
      }
    } catch (e: any) {
      toast.err(e?.message || 'Falha render')
    }
    setRendering(false)
  }

  // ===== UI =====
  const job = statusQ.data
  const jobDone = job?.status === 'DONE'
  const jobFailed = job?.status === 'FAILED'

  return (
    <div className="route-fade max-w-[1600px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎬 Video Editor</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Compõe um MP4 misturando voice clips dos players, música de fundo + imagem.
            Render server-side via ffmpeg, download direto pelo navegador.
          </p>
        </div>
        <div className="flex gap-2 text-xs items-center">
          <span className="badge badge-purple">{tracks.length} voice track{tracks.length === 1 ? '' : 's'}</span>
          <span className="badge">{fmtTime(totalDurationMs)} duração</span>
        </div>
      </header>

      {backendOutdated && (
        <div className="mb-4 p-3 rounded bg-red-900/30 border border-red-400/40 text-sm">
          <div className="font-bold text-red-300 mb-1">⚠ Backend desatualizado</div>
          <div className="text-xs text-red-200/80">
            O backend rodando não tem os endpoints do Video Editor (404). Atualize pra{' '}
            <code className="text-red-100">murilonerdx/lsmp-backend:v51</code> ou mais recente,
            re-deploy o stack com "Re-pull image" marcado, e adicione o volume{' '}
            <code className="text-red-100">lsmp_video_data:/app/data/videos</code>.
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[300px_1fr] gap-3">
        {/* ============ Sidebar: Voice Library picker ============ */}
        <aside className="card-glow space-y-2 max-h-[80vh] overflow-y-auto">
          {/* Mode switcher */}
          <div className="flex gap-1">
            <button
              className={`btn-ghost btn-sm flex-1 text-[10px] ${sidebarMode === 'byPlayer' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
              onClick={() => setSidebarMode('byPlayer')}>
              👤 Por player
            </button>
            <button
              className={`btn-ghost btn-sm flex-1 text-[10px] ${sidebarMode === 'allMixed' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
              onClick={() => { setSidebarMode('allMixed'); setSelectedPlayer(null) }}>
              🌐 Todos
            </button>
          </div>

          <input className="input w-full text-xs"
            placeholder="🔍 Filtrar por nome/texto…"
            value={sidebarSearch}
            onChange={(e) => setSidebarSearch(e.target.value)} />

          {/* ====== Mode: byPlayer ====== */}
          {sidebarMode === 'byPlayer' && !selectedPlayer && (
            <>
              <div className="text-[11px] text-liberthia-300/60 italic">
                Escolha um player pra ver os clipes dele:
              </div>
              {(playersQ.data?.players ?? [])
                .filter(p => !sidebarSearch.trim() || p.playerName?.toLowerCase().includes(sidebarSearch.toLowerCase()))
                .map(p => (
                <button key={p.playerUuid}
                  onClick={() => setSelectedPlayer(p)}
                  className="w-full text-left rounded p-2 bg-liberthia-900/40 hover:bg-liberthia-900/70 flex items-center gap-2">
                  <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.playerName)}/24`}
                    className="rounded shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-bold truncate">{p.playerName}</div>
                    <div className="text-[10px] text-liberthia-300/50">
                      {p.clipCount} clipes · {fmtSec(p.totalDurationMs)}
                    </div>
                    <div className="text-[10px] text-liberthia-300/40">
                      {new Date(p.firstClipTs).toLocaleDateString('pt-BR')} → {new Date(p.lastClipTs).toLocaleDateString('pt-BR')}
                    </div>
                  </div>
                </button>
              ))}
            </>
          )}

          {sidebarMode === 'byPlayer' && selectedPlayer && (
            <>
              <div className="flex items-center justify-between sticky top-0 bg-liberthia-900/70 backdrop-blur p-1 rounded">
                <span className="text-xs font-bold truncate">{selectedPlayer.playerName}</span>
                <button className="btn-ghost btn-sm text-[10px] shrink-0"
                  onClick={() => setSelectedPlayer(null)}>← players</button>
              </div>
              {clipsQ.isLoading && <div className="text-xs text-liberthia-300/60">Carregando…</div>}
              {(clipsQ.data?.clips ?? [])
                .filter(c => !sidebarSearch.trim() || c.transcription?.toLowerCase().includes(sidebarSearch.toLowerCase()))
                .map(c => (
                <button key={c.id}
                  onClick={() => addTrack(c)}
                  className="w-full text-left rounded p-2 bg-liberthia-900/40 hover:bg-purple-500/30 transition">
                  <div className="text-[10px] font-mono text-liberthia-300/60 flex justify-between">
                    <span>#{c.id} · {fmtSec(c.durationMs)}</span>
                    <span>{fmtClock(c.ts)}</span>
                  </div>
                  {c.transcription && (
                    <div className="text-[11px] italic text-liberthia-300/70 line-clamp-2 mt-0.5">
                      "{c.transcription}"
                    </div>
                  )}
                </button>
              ))}
            </>
          )}

          {/* ====== Mode: allMixed ====== */}
          {sidebarMode === 'allMixed' && (
            <>
              <div className="text-[11px] text-liberthia-300/60 italic">
                Todos os clipes do servidor (mais recentes primeiro):
              </div>
              {allClipsQ.isLoading && <div className="text-xs text-liberthia-300/60">Carregando…</div>}
              {(allClipsQ.data?.clips ?? [])
                .filter(c => !sidebarSearch.trim()
                  || c.playerName?.toLowerCase().includes(sidebarSearch.toLowerCase())
                  || c.transcription?.toLowerCase().includes(sidebarSearch.toLowerCase()))
                .map(c => (
                <button key={c.id}
                  onClick={() => addTrack(c)}
                  className="w-full text-left rounded p-2 bg-liberthia-900/40 hover:bg-purple-500/30 transition flex gap-2">
                  <img src={`https://mc-heads.net/avatar/${encodeURIComponent(c.playerName)}/24`}
                    className="rounded shrink-0 self-start" />
                  <div className="flex-1 min-w-0">
                    <div className="text-[11px] flex justify-between">
                      <span className="font-bold truncate">{c.playerName}</span>
                      <span className="text-liberthia-300/50 font-mono shrink-0 ml-1">
                        {fmtSec(c.durationMs)}
                      </span>
                    </div>
                    <div className="text-[10px] text-liberthia-300/50">
                      {fmtClock(c.ts)}
                    </div>
                    {c.transcription && (
                      <div className="text-[11px] italic text-liberthia-300/70 line-clamp-2 mt-0.5">
                        "{c.transcription}"
                      </div>
                    )}
                  </div>
                </button>
              ))}
            </>
          )}
        </aside>

        {/* ============ Main: Editor ============ */}
        <main className="space-y-3">
          {/* Assets row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="card-glow">
              <h3 className="text-sm font-bold mb-2">🖼 Imagem de fundo</h3>
              {!bgImageId ? (
                <label className="btn-ghost w-full text-center cursor-pointer block">
                  {uploadingBg ? '⏳ enviando…' : '📁 Upload imagem'}
                  <input type="file" accept="image/*" className="hidden"
                    onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadBg(f); }} />
                </label>
              ) : (
                <div className="flex items-center gap-2">
                  {bgImagePreview && <img src={bgImagePreview} className="w-20 h-12 object-cover rounded" />}
                  <div className="flex-1 min-w-0">
                    <div className="text-xs font-mono truncate">{bgImageName}</div>
                    <div className="text-[10px] text-liberthia-300/50">{bgImageId}</div>
                  </div>
                  <button className="btn-ghost btn-sm" onClick={() => { setBgImageId(null); setBgImageName(''); setBgImagePreview('') }}>✕</button>
                </div>
              )}
            </div>

            <div className="card-glow">
              <h3 className="text-sm font-bold mb-2">🎵 Música de fundo</h3>
              {!musicId ? (
                <label className="btn-ghost w-full text-center cursor-pointer block">
                  {uploadingMusic ? '⏳ enviando…' : '📁 Upload mp3/wav'}
                  <input type="file" accept="audio/*" className="hidden"
                    onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadMusic(f); }} />
                </label>
              ) : (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl">🎵</span>
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-mono truncate">{musicName}</div>
                    </div>
                    <button className="btn-ghost btn-sm" onClick={() => {
                      setMusicId(null); setMusicName('')
                      if (musicLocalUrl) { URL.revokeObjectURL(musicLocalUrl); setMusicLocalUrl('') }
                    }}>✕</button>
                  </div>
                  <div>
                    <label className="text-[10px] text-liberthia-300/60">
                      Volume da música: {musicVolume.toFixed(2)}
                    </label>
                    <input type="range" min={0} max={1} step={0.05}
                      value={musicVolume}
                      onChange={(e) => setMusicVolume(Number(e.target.value))}
                      className="w-full" />
                  </div>
                  <label className="flex items-center gap-2 text-[11px] mt-1 cursor-pointer">
                    <input type="checkbox" checked={musicLoop}
                      onChange={(e) => setMusicLoop(e.target.checked)} />
                    <span>🔁 Repetir música até o fim do vídeo</span>
                  </label>
                  <div className="text-[10px] text-liberthia-300/50">
                    {musicLoop
                      ? `Loop ON — música toca em repeat até cobrir os ${(totalDurationMs / 1000).toFixed(0)}s do vídeo.`
                      : 'Loop OFF — música toca 1× e silencia depois.'}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Timeline */}
          <div className="card-glow">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold">⏱ Timeline de voices</h3>
              <div className="flex gap-2 flex-wrap">
                <button className="btn-ghost btn-sm" onClick={sortChronological} disabled={tracks.length === 0}
                  title="Reordena pela hora REAL em que as falas aconteceram no servidor">
                  🕒 Cronológico
                </button>
                <button className="btn-ghost btn-sm" onClick={autoArrange} disabled={tracks.length === 0}>
                  ⏩ Sequencial
                </button>
                <button className="btn-ghost btn-sm" onClick={() => setTracks([])} disabled={tracks.length === 0}>
                  ✕ Limpar
                </button>
                {previewing ? (
                  <button className="btn btn-sm bg-red-500/30" onClick={stopPreview}>
                    ⏹ Stop preview ({(previewMs / 1000).toFixed(1)}s)
                  </button>
                ) : (
                  <button className="btn btn-sm bg-purple-500/30" onClick={startPreview}
                    disabled={tracks.length === 0}>
                    ▶ Preview (browser)
                  </button>
                )}
              </div>
            </div>

            {tracks.length === 0 && (
              <div className="text-center py-12 text-liberthia-300/60 text-xs italic">
                Selecione um player na sidebar e clique nos clipes pra adicionar ao timeline.
              </div>
            )}

            {tracks.map((t, idx) => (
              <div key={idx} className="bg-liberthia-900/40 rounded p-2 mb-2 flex items-center gap-2">
                <span className="text-[10px] text-liberthia-300/40 w-6 text-right">#{idx + 1}</span>
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(t.playerName)}/24`}
                  className="rounded shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-bold truncate">{t.playerName}</div>
                  <div className="text-[10px] text-liberthia-300/40">
                    real: {fmtClock(t.realTs)}
                  </div>
                  {t.transcription && (
                    <div className="text-[10px] italic text-liberthia-300/60 line-clamp-1">
                      "{t.transcription}"
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-1 text-[10px]">
                  <label className="text-liberthia-300/60">Start:</label>
                  <input type="number" min={0} value={t.startMs}
                    onChange={(e) => updateTrack(idx, { startMs: Math.max(0, Number(e.target.value) || 0) })}
                    className="input w-20 text-[10px] py-1" />
                  <span className="text-liberthia-300/40">ms</span>
                </div>
                <div className="flex items-center gap-1 text-[10px] min-w-[100px]">
                  <label className="text-liberthia-300/60">Vol:</label>
                  <input type="range" min={0} max={2} step={0.1}
                    value={t.volume}
                    onChange={(e) => updateTrack(idx, { volume: Number(e.target.value) })}
                    className="flex-1" />
                  <span className="font-mono w-8 text-right">{t.volume.toFixed(1)}</span>
                </div>
                <span className="text-[10px] font-mono text-liberthia-300/50 w-14 text-right">
                  +{fmtSec(t.durationMs)}
                </span>
                <button className="btn-ghost btn-sm text-red-300" onClick={() => removeTrack(idx)}>✕</button>
              </div>
            ))}
          </div>

          {/* Title (opcional) */}
          <div className="card-glow">
            <label className="text-xs text-liberthia-300/70 block mb-1">
              📝 Título (opcional — overlay no vídeo, ainda WIP)
            </label>
            <input className="input" value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="ex: Crônicas de Liberthia — Capítulo 1" />
          </div>

          {/* Render button + status */}
          <div className="card-glow space-y-2">
            <button className="btn w-full bg-purple-500/30 hover:bg-purple-500/50 ring-1 ring-purple-400/60"
              onClick={startRender}
              disabled={rendering || tracks.length === 0 || (job?.status === 'RENDERING')}>
              {rendering ? '⏳ enviando…' :
                job?.status === 'RENDERING' ? `🎬 renderizando ${job.progress}%…` :
                  '🎬 Renderizar MP4'}
            </button>

            {job && (
              <div className={`p-3 rounded text-xs ${
                jobDone ? 'bg-emerald-900/30 border border-emerald-400/40' :
                jobFailed ? 'bg-red-900/30 border border-red-400/40' :
                'bg-amber-900/20 border border-amber-400/40'
              }`}>
                <div className="flex items-center justify-between mb-1">
                  <span className="font-bold">Job {job.id}</span>
                  <span>{job.status} · {job.progress}%</span>
                </div>
                <div className="w-full bg-liberthia-900/60 h-1.5 rounded overflow-hidden">
                  <div className={`h-full transition-all ${
                    jobFailed ? 'bg-red-400' : jobDone ? 'bg-emerald-400' : 'bg-amber-400'
                  }`}
                    style={{ width: `${job.progress}%` }} />
                </div>
                {job.message && <div className="mt-1 text-liberthia-300/70 italic">{job.message}</div>}
                {jobFailed && job.error && (
                  <div className="mt-1 text-red-300 text-[10px] font-mono whitespace-pre-wrap break-all">
                    {job.error}
                  </div>
                )}
                {jobDone && (
                  <div className="flex gap-2 mt-2">
                    <button className="btn flex-1 bg-emerald-500/30 hover:bg-emerald-500/50"
                      onClick={() => downloadMp4(job.id, job.outputPath || 'video.mp4')}>
                      ⬇ Baixar MP4
                    </button>
                    <button className="btn flex-1 bg-purple-500/30 hover:bg-purple-500/50"
                      onClick={() => setShowSaveModal(true)}>
                      💾 Salvar no servidor
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </main>
      </div>

      {/* Save-to-server modal */}
      {showSaveModal && job?.id && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4"
          onClick={() => setShowSaveModal(false)}>
          <div className="card-glow max-w-2xl w-full" onClick={e => e.stopPropagation()}>
            <h3 className="font-bold mb-1">💾 Salvar vídeo no servidor</h3>
            <p className="text-[11px] text-liberthia-300/60 mb-3">
              O vídeo fica persistido em <code className="text-amber-300">/videos</code> e,
              se público, pode ser visto sem login em <code className="text-amber-300">/watch/&#123;id&#125;</code>.
            </p>
            <label className="block mb-2">
              <span className="text-xs text-liberthia-300/70">Título</span>
              <input className="input w-full" value={saveTitle}
                onChange={e => setSaveTitle(e.target.value)}
                placeholder="ex: Crônicas — Capítulo 1" />
            </label>
            <label className="block mb-2">
              <span className="text-xs text-liberthia-300/70">Descrição</span>
              <textarea className="input w-full min-h-32" value={saveDescription}
                onChange={e => setSaveDescription(e.target.value)}
                placeholder="contexto, players envolvidos, data dos eventos…" />
            </label>
            <label className="flex items-center gap-2 mb-3 text-xs">
              <input type="checkbox" checked={saveIsPublic}
                onChange={e => setSaveIsPublic(e.target.checked)} />
              <span>Público (qualquer um com o link pode ver, sem login)</span>
            </label>
            <div className="flex gap-2">
              <button className="btn btn-primary flex-1" onClick={saveToServer}
                disabled={savingToServer || !saveTitle.trim()}>
                {savingToServer ? '⏳ Salvando…' : '💾 Salvar'}
              </button>
              <button className="btn-ghost" onClick={() => setShowSaveModal(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
