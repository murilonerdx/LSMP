import { useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Voice Modulator — grava microfone no browser, aplica efeitos (pitch shift,
 * distortion, reverb, lowpass) via WebAudio OfflineContext, e exporta como
 * WAV/OGG pra colocar como custom sound.
 *
 * Pipeline:
 *   1. MediaRecorder grava do mic → blob WebM/Opus
 *   2. Decodifica via AudioContext.decodeAudioData
 *   3. OfflineAudioContext aplica chain de efeitos
 *   4. Render → AudioBuffer
 *   5. Encode pra WAV (rapido, lossless)
 *   6. User baixa WAV + instruções pra converter pra OGG, OU
 *      tenta upload direto se backend aceitar WAV (a gente vai aceitar)
 *
 * NOTA sobre formato: MC vanilla EXIGE OGG Vorbis. Browser não codifica OGG
 * nativamente. Solução: deixamos o user fazer upload do WAV gerado, e na
 * página de Sounds avisamos pra converter (ffmpeg em 1 comando).
 *
 * Web Audio efeitos in-place:
 *   - playbackRate (pitch + speed combined)
 *   - WaveShaper (distortion)
 *   - ConvolverNode com IR fake (reverb simples)
 *   - BiquadFilter (lowpass / highpass)
 */

type EffectChain = {
  pitch: number          // 0.5 = grave, 2 = agudo
  distortion: number     // 0..100
  reverb: number         // 0..1 wet/dry
  lowpass: number        // Hz, 22050 = sem filtro
  highpass: number       // Hz, 0 = sem filtro
  echoDelay: number      // ms, 0 = sem echo
  echoFeedback: number   // 0..0.9
}

const PRESETS: { name: string; emoji: string; chain: EffectChain }[] = [
  { name: 'Original', emoji: '🎙', chain: { pitch: 1, distortion: 0, reverb: 0, lowpass: 22050, highpass: 0, echoDelay: 0, echoFeedback: 0 } },
  { name: 'Demônio', emoji: '👹', chain: { pitch: 0.6, distortion: 40, reverb: 0.3, lowpass: 4000, highpass: 80, echoDelay: 0, echoFeedback: 0 } },
  { name: 'Fantasma', emoji: '👻', chain: { pitch: 0.85, distortion: 5, reverb: 0.7, lowpass: 6000, highpass: 200, echoDelay: 250, echoFeedback: 0.4 } },
  { name: 'Rádio antigo', emoji: '📻', chain: { pitch: 1, distortion: 15, reverb: 0, lowpass: 3500, highpass: 400, echoDelay: 0, echoFeedback: 0 } },
  { name: 'Robô', emoji: '🤖', chain: { pitch: 1.2, distortion: 30, reverb: 0, lowpass: 8000, highpass: 100, echoDelay: 60, echoFeedback: 0.3 } },
  { name: 'Caverna', emoji: '🕳', chain: { pitch: 0.95, distortion: 0, reverb: 0.85, lowpass: 5000, highpass: 100, echoDelay: 400, echoFeedback: 0.5 } },
  { name: 'Helio', emoji: '🎈', chain: { pitch: 1.6, distortion: 0, reverb: 0, lowpass: 22050, highpass: 0, echoDelay: 0, echoFeedback: 0 } },
  { name: 'Sussurro', emoji: '🤫', chain: { pitch: 0.92, distortion: 0, reverb: 0.6, lowpass: 4000, highpass: 300, echoDelay: 0, echoFeedback: 0 } },
]

export function VoiceModulatorPage() {
  const qc = useQueryClient()
  const [recording, setRecording] = useState(false)
  const [rawBuffer, setRawBuffer] = useState<AudioBuffer | null>(null)
  const [processed, setProcessed] = useState<AudioBuffer | null>(null)
  const [chain, setChain] = useState<EffectChain>(PRESETS[0].chain)
  const [processing, setProcessing] = useState(false)
  const [namespace, setNamespace] = useState('liberthia')
  const [key, setKey] = useState('voice_' + Date.now().toString(36))
  const [uploading, setUploading] = useState(false)
  const mediaRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const procAudioRef = useRef<HTMLAudioElement | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string>('')
  const [procUrl, setProcUrl] = useState<string>('')
  const [permError, setPermError] = useState<string>('')
  const [uploadedPlayId, setUploadedPlayId] = useState<string>('')
  // Voice Modulator → Play in-game (3 modos: player específico / coord fixa / todos)
  const [playMode, setPlayMode] = useState<'player' | 'coord' | 'all'>('player')
  const [targetUuid, setTargetUuid] = useState<string>('')
  // Default 0.8× (era 1.5× que ficou MUITO alto) — escala mais razoável
  // pra Simple Voice Chat positional. Subir manualmente se precisar.
  const [playVolume, setPlayVolume] = useState<number>(0.8)
  const [playingInGame, setPlayingInGame] = useState(false)
  const [coordX, setCoordX] = useState<number>(0)
  const [coordY, setCoordY] = useState<number>(80)
  const [coordZ, setCoordZ] = useState<number>(0)
  const [coordDim, setCoordDim] = useState<string>('minecraft:overworld')
  // Usa MESMA queryKey ['players'] que outras páginas → cache compartilhado +
  // refresh automático em paralelo. Antes era ['mod-players-online'] isolado,
  // que ficava stale facilmente.
  const playersQ = useQuery({
    queryKey: ['players'],
    queryFn: api.players,
    refetchInterval: 5_000,
    staleTime: 2_000,
  })

  // Detecta se contexto é seguro pro getUserMedia
  const isSecure = typeof window !== 'undefined' &&
    (window.isSecureContext || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')

  async function startRecord() {
    setPermError('')
    // Verifica contexto seguro ANTES de pedir permissão
    if (!isSecure) {
      setPermError('insecure-context')
      return
    }
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setPermError('no-api')
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      chunksRef.current = []
      const mr = new MediaRecorder(stream)
      mr.ondataavailable = (e) => { if (e.data.size > 0) chunksRef.current.push(e.data) }
      mr.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop())
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' })
        const arrayBuffer = await blob.arrayBuffer()
        const ctx = new AudioContext()
        try {
          const buf = await ctx.decodeAudioData(arrayBuffer)
          setRawBuffer(buf)
          if (previewUrl) URL.revokeObjectURL(previewUrl)
          setPreviewUrl(URL.createObjectURL(blob))
          toast.ok(`🎙 Gravação: ${buf.duration.toFixed(1)}s`)
        } catch (e: any) { toast.err('Falha decodificar: ' + e.message) }
        await ctx.close()
      }
      mr.start()
      mediaRef.current = mr
      setRecording(true)
      toast.info('🔴 Gravando — click pra parar')
    } catch (e: any) {
      const msg = e?.message || String(e)
      if (e?.name === 'NotAllowedError' || /permission/i.test(msg) || /denied/i.test(msg)) {
        setPermError('denied')
      } else if (e?.name === 'NotFoundError') {
        setPermError('no-mic')
      } else {
        setPermError('other:' + msg)
      }
    }
  }

  async function uploadFile(file: File) {
    setRawBuffer(null); setProcessed(null); setPermError('')
    try {
      const arrayBuffer = await file.arrayBuffer()
      const ctx = new AudioContext()
      const buf = await ctx.decodeAudioData(arrayBuffer)
      await ctx.close()
      setRawBuffer(buf)
      if (previewUrl) URL.revokeObjectURL(previewUrl)
      setPreviewUrl(URL.createObjectURL(file))
      // Auto-fill key com nome do arquivo
      const base = file.name.replace(/\.[^.]+$/, '').toLowerCase().replace(/[^a-z0-9_]/g, '_')
      if (base) setKey(base)
      toast.ok(`📁 Arquivo carregado: ${buf.duration.toFixed(1)}s`)
    } catch (e: any) {
      toast.err('Falha ao ler áudio: ' + e.message)
    }
  }

  function stopRecord() {
    mediaRef.current?.stop()
    setRecording(false)
  }

  async function applyEffects() {
    if (!rawBuffer) { toast.err('Grave primeiro'); return }
    setProcessing(true)
    try {
      const out = await processBuffer(rawBuffer, chain)
      setProcessed(out)
      // Convert pra blob WAV e gerar preview URL
      const wavBlob = bufferToWav(out)
      if (procUrl) URL.revokeObjectURL(procUrl)
      setProcUrl(URL.createObjectURL(wavBlob))
      toast.ok(`✓ Processado — ${out.duration.toFixed(1)}s`)
    } catch (e: any) { toast.err(e.message) }
    setProcessing(false)
  }

  async function upload() {
    if (!processed) { toast.err('Aplique efeitos primeiro'); return }
    setUploading(true)
    try {
      const wavBlob = bufferToWav(processed)
      const file = new File([wavBlob], `${key}.ogg`, { type: 'audio/ogg' })
      await api.soundUpload(file, namespace, key)
      qc.invalidateQueries({ queryKey: ['sounds'] })
      const playId = `${namespace}:${key}`
      setUploadedPlayId(playId)
      toast.ok(`✓ Enviado como ${playId}`)
    } catch (e: any) { toast.err(e.message) }
    setUploading(false)
  }

  function downloadWav() {
    if (!processed) return
    const wav = bufferToWav(processed)
    const a = document.createElement('a')
    a.href = URL.createObjectURL(wav)
    a.download = `${key}.wav`
    a.click()
  }

  /**
   * Toca o áudio modulado no jogo. Modo selecionado decide:
   *  - 'player': posição do player escolhido
   *  - 'coord' : X/Y/Z fixos
   *  - 'all'   : toca pra cada player online em sua posição
   */
  async function playInGameDispatch() {
    if (!processed) { toast.err('Aplique efeitos primeiro'); return }
    setPlayingInGame(true)
    try {
      const wavBlob = bufferToWav(processed)
      let r: any
      if (playMode === 'player') {
        if (!targetUuid) { toast.err('Escolha um player'); setPlayingInGame(false); return }
        r = await api.voiceModulatePlay(wavBlob, targetUuid, playVolume, true)
        if (r.ok) toast.ok(`🎮 Tocando pra ${r.playerName}`)
        else toast.err(r.error || 'Falha')
      } else if (playMode === 'coord') {
        r = await api.voiceModulatePlayAt(wavBlob, coordX, coordY, coordZ, coordDim, playVolume, true)
        if (r.ok) toast.ok(`📍 Tocando em (${Math.round(coordX)}, ${Math.round(coordY)}, ${Math.round(coordZ)})`)
        else toast.err(r.error || 'Falha')
      } else if (playMode === 'all') {
        r = await api.voiceModulatePlayAll(wavBlob, playVolume, true)
        if (r.ok) toast.ok(`🌐 Tocando pra ${r.playedCount}/${r.totalPlayers} players`)
        else toast.err(r.error || 'Nenhum player online')
      }
    } catch (e: any) {
      toast.err(e?.message || String(e))
    }
    setPlayingInGame(false)
  }

  function applyPreset(name: string) {
    const p = PRESETS.find((x) => x.name === name); if (!p) return
    setChain(p.chain)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">🎙 Voice Modulator</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Grava sua voz, aplica efeitos (pitch, distortion, reverb, echo), exporta como sound custom pro MC.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Recording */}
        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3">🎤 1. Gravar ou carregar áudio</h3>

            {!recording && !rawBuffer && (
              <>
                <button className="btn w-full mb-2 pulse-glow" onClick={startRecord}>🔴 Gravar do microfone</button>
                <label className="btn-ghost w-full text-center cursor-pointer block">
                  📁 Carregar arquivo (.ogg / .mp3 / .wav / .webm)
                  <input type="file" accept="audio/*,.ogg,.mp3,.wav,.webm,.m4a" className="hidden"
                    onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadFile(f); (e.target as HTMLInputElement).value = '' }} />
                </label>
              </>
            )}
            {recording && (
              <button className="btn-danger w-full pulse-glow" onClick={stopRecord}>⏹ Parar gravação</button>
            )}
            {rawBuffer && !recording && (
              <div>
                <audio ref={audioRef} src={previewUrl} controls className="w-full mb-2" />
                <div className="grid grid-cols-2 gap-2">
                  <button className="btn-ghost btn-sm" onClick={startRecord}>🔁 Re-gravar</button>
                  <label className="btn-ghost btn-sm text-center cursor-pointer">
                    📁 Outro arquivo
                    <input type="file" accept="audio/*,.ogg,.mp3,.wav,.webm,.m4a" className="hidden"
                      onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadFile(f); (e.target as HTMLInputElement).value = '' }} />
                  </label>
                </div>
              </div>
            )}

            {/* Mensagens de erro/permissão */}
            {permError && <PermissionHelp error={permError} isSecure={isSecure} hostname={window.location.hostname} onRetry={startRecord} />}
            {!permError && !isSecure && !rawBuffer && (
              <div className="mt-3 text-[11px] leading-relaxed p-2 rounded-lg bg-amber-500/10 border border-amber-400/30 text-amber-200">
                ⚠ <b>Microfone bloqueado:</b> esta página tá rodando em <code className="font-mono">{window.location.hostname}</code> (não-HTTPS).
                Browsers só liberam mic em <b>HTTPS</b> ou <b>localhost</b>. Use upload de arquivo, ou configure HTTPS.
              </div>
            )}
          </div>

          {/* Effects */}
          {rawBuffer && (
            <div className="card-glow">
              <h3 className="font-bold mb-3">⚗ 2. Efeitos</h3>

              <label className="label block mb-1">Preset</label>
              <div className="grid grid-cols-4 gap-1 mb-3">
                {PRESETS.map((p) => (
                  <button key={p.name} className="btn-ghost btn-sm" onClick={() => applyPreset(p.name)}>
                    {p.emoji} {p.name}
                  </button>
                ))}
              </div>

              <div className="space-y-2 text-xs">
                <Slider label="Pitch" value={chain.pitch} min={0.4} max={2} step={0.05}
                  onChange={(v) => setChain({ ...chain, pitch: v })} format={(v) => v.toFixed(2) + 'x'} />
                <Slider label="Distortion" value={chain.distortion} min={0} max={100} step={1}
                  onChange={(v) => setChain({ ...chain, distortion: v })} format={(v) => v.toFixed(0) + '%'} />
                <Slider label="Reverb" value={chain.reverb} min={0} max={1} step={0.05}
                  onChange={(v) => setChain({ ...chain, reverb: v })} format={(v) => (v * 100).toFixed(0) + '%'} />
                <Slider label="Lowpass" value={chain.lowpass} min={500} max={22050} step={100}
                  onChange={(v) => setChain({ ...chain, lowpass: v })} format={(v) => (v / 1000).toFixed(1) + 'kHz'} />
                <Slider label="Highpass" value={chain.highpass} min={0} max={2000} step={20}
                  onChange={(v) => setChain({ ...chain, highpass: v })} format={(v) => v.toFixed(0) + 'Hz'} />
                <Slider label="Echo delay" value={chain.echoDelay} min={0} max={800} step={20}
                  onChange={(v) => setChain({ ...chain, echoDelay: v })} format={(v) => v.toFixed(0) + 'ms'} />
                <Slider label="Echo feedback" value={chain.echoFeedback} min={0} max={0.85} step={0.05}
                  onChange={(v) => setChain({ ...chain, echoFeedback: v })} format={(v) => (v * 100).toFixed(0) + '%'} />
              </div>

              <button className="btn w-full mt-3" onClick={applyEffects} disabled={processing}>
                {processing ? '⏳ Processando...' : '✨ Aplicar efeitos'}
              </button>
            </div>
          )}
        </div>

        {/* Processed + upload */}
        <div className="space-y-4">
          {processed && (
            <div className="card-glow">
              <h3 className="font-bold mb-3">🎧 3. Resultado</h3>
              <audio ref={procAudioRef} src={procUrl} controls className="w-full mb-3" />
              <div className="flex gap-2">
                <button className="btn-ghost flex-1" onClick={downloadWav}>⬇ Download .wav</button>
                <button className="btn-cyan flex-1" onClick={applyEffects}>🔁 Re-processar</button>
              </div>
            </div>
          )}

          {/* ===== 4. Tocar in-game (3 modos) ===== */}
          {processed && (
            <div className="card-glow">
              <h3 className="font-bold mb-2">🎮 4. Tocar AGORA no jogo</h3>
              <p className="text-[11px] text-liberthia-300/60 mb-3">
                Reproduz o áudio modulado via Simple Voice Chat positional — soa
                <b> como uma voz humana</b>, não como playsound. <b>Sem resource pack</b>.
              </p>

              {/* Mode tabs */}
              <div className="flex gap-1 mb-3 text-xs">
                <button
                  className={`btn-ghost btn-sm flex-1 ${playMode === 'player' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
                  onClick={() => setPlayMode('player')}>
                  👤 Player
                </button>
                <button
                  className={`btn-ghost btn-sm flex-1 ${playMode === 'coord' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
                  onClick={() => setPlayMode('coord')}>
                  📍 Coordenada
                </button>
                <button
                  className={`btn-ghost btn-sm flex-1 ${playMode === 'all' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
                  onClick={() => setPlayMode('all')}>
                  🌐 Todos
                </button>
              </div>

              {/* Volume comum */}
              <div className="mb-3">
                <label className="label block mb-1">Volume ({playVolume.toFixed(1)}×)</label>
                <input type="range" min={0.1} max={2} step={0.05}
                  value={playVolume}
                  onChange={(e) => setPlayVolume(Number(e.target.value))}
                  className="w-full" />
                <div className="flex justify-between text-[9px] text-liberthia-300/40">
                  <span>sussurro</span>
                  <span>normal</span>
                  <span>alto</span>
                </div>
                <div className="text-[10px] text-liberthia-300/50">
                  {playVolume < 0.4 ? '🤫 quase inaudível — só pra quem está bem perto' :
                   playVolume < 0.7 ? '🔉 sussurro — escuta de ~5 blocos' :
                   playVolume < 1.1 ? '🔊 volume normal — voz natural' :
                   playVolume < 1.6 ? '📢 alto — escuta de longe' :
                   '💥 MUITO alto — pode incomodar'}
                </div>
              </div>

              {/* Mode-specific inputs */}
              {playMode === 'player' && (
                <div className="mb-3">
                  <div className="flex items-baseline justify-between mb-1">
                    <label className="label">
                      Player alvo
                      {playersQ.isLoading && <span className="ml-2 text-[10px] text-liberthia-300/50 animate-pulse">carregando...</span>}
                      {playersQ.isError && <span className="ml-2 text-[10px] text-red-400">erro ao buscar</span>}
                      {!playersQ.isLoading && !playersQ.isError && (
                        <span className="ml-2 text-[10px] text-emerald-300/70">
                          ({playersQ.data?.length ?? 0} online)
                        </span>
                      )}
                    </label>
                    <button type="button"
                      className="btn-ghost btn-sm text-[10px]"
                      onClick={() => playersQ.refetch()}
                      disabled={playersQ.isFetching}
                      title="Atualizar lista de players">
                      {playersQ.isFetching ? '⏳' : '↻'} refresh
                    </button>
                  </div>
                  <select className="input" value={targetUuid}
                    onChange={(e) => setTargetUuid(e.target.value)}
                    disabled={playersQ.isLoading}>
                    <option value="">— Selecione um player —</option>
                    {(playersQ.data ?? []).map((p: any) => (
                      <option key={p.uuid} value={p.uuid}>
                        {p.name} ({p.dimension?.replace('minecraft:', '') ?? '?'}) · L{p.level}
                      </option>
                    ))}
                  </select>
                  {playersQ.isError && (
                    <div className="text-[10px] text-red-400 mt-1">
                      ⚠ Falha ao buscar players: {(playersQ.error as any)?.message ?? 'erro desconhecido'}
                    </div>
                  )}
                  {!playersQ.isLoading && !playersQ.isError && playersQ.data && playersQ.data.length === 0 && (
                    <div className="text-[10px] text-amber-300 mt-1">⚠ Nenhum player online no momento.</div>
                  )}
                  <div className="text-[10px] text-liberthia-300/50 mt-1">
                    Som toca na posição do jogador. Outros próximos podem escutar (positional).
                  </div>
                </div>
              )}

              {playMode === 'coord' && (
                <div className="mb-3 space-y-2">
                  <div className="grid grid-cols-3 gap-2 text-xs">
                    <label>
                      <span className="text-liberthia-300/70">X</span>
                      <input type="number" className="input" value={coordX}
                        onChange={(e) => setCoordX(Number(e.target.value) || 0)} />
                    </label>
                    <label>
                      <span className="text-liberthia-300/70">Y</span>
                      <input type="number" className="input" value={coordY}
                        onChange={(e) => setCoordY(Number(e.target.value) || 0)} />
                    </label>
                    <label>
                      <span className="text-liberthia-300/70">Z</span>
                      <input type="number" className="input" value={coordZ}
                        onChange={(e) => setCoordZ(Number(e.target.value) || 0)} />
                    </label>
                  </div>
                  <select className="input text-xs w-full" value={coordDim}
                    onChange={(e) => setCoordDim(e.target.value)}>
                    <option value="minecraft:overworld">Overworld</option>
                    <option value="minecraft:the_nether">Nether</option>
                    <option value="minecraft:the_end">End</option>
                  </select>
                  <div className="text-[10px] text-liberthia-300/50">
                    Som toca em coords fixas — players ao redor escutam. Tipo um "speaker" no mundo.
                  </div>
                </div>
              )}

              {playMode === 'all' && (
                <div className="mb-3 p-2 rounded bg-amber-900/20 border border-amber-400/30 text-[11px] text-amber-200/90">
                  📢 <b>Modo broadcast</b>: toca o áudio na posição de CADA player online
                  ({playersQ.data?.length ?? 0} online agora). Cada um escuta como se fosse na própria localização.
                  Útil pra announcements lore-friendly (entidades cósmicas, NPCs onipresentes).
                </div>
              )}

              <button className="btn w-full bg-purple-500/30 hover:bg-purple-500/50 ring-1 ring-purple-400/60"
                onClick={playInGameDispatch}
                disabled={playingInGame || (playMode === 'player' && !targetUuid)}>
                {playingInGame ? '⏳ Enviando…' :
                  playMode === 'player' ? `🎙 Tocar pra ${targetUuid ? (playersQ.data?.find((p: any) => p.uuid === targetUuid)?.name ?? 'player') : 'player'}` :
                  playMode === 'coord' ? `📍 Tocar em (${Math.round(coordX)}, ${Math.round(coordY)}, ${Math.round(coordZ)})` :
                  `🌐 Broadcast pra ${playersQ.data?.length ?? 0} player(s)`}
              </button>
              <div className="text-[10px] text-liberthia-300/50 mt-2 italic">
                💡 Clipe é auto-deletado após 60s. Não polui Voice Library.
              </div>
            </div>
          )}

          {processed && (
            <div className="card-glow">
              <h3 className="font-bold mb-3">📤 5. Upload como sound permanente</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-3">
                <div>
                  <label className="label block mb-1">Namespace</label>
                  <input className="input font-mono text-xs" value={namespace}
                    onChange={(e) => setNamespace(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_'))} />
                </div>
                <div>
                  <label className="label block mb-1">Key</label>
                  <input className="input font-mono text-xs" value={key}
                    onChange={(e) => setKey(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_'))} />
                </div>
              </div>
              <button className="btn w-full" onClick={upload} disabled={uploading}>
                {uploading ? '⏳' : '📤 Upload como ' + namespace + ':' + key}
              </button>

              {/* Comando pós-upload */}
              {uploadedPlayId && (
                <div className="mt-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-400/40">
                  <div className="text-xs font-bold text-emerald-200 mb-2">✅ Som disponível como <span className="font-mono">{uploadedPlayId}</span></div>
                  <CommandLine label="Tocar pra todos" cmd={`/playsound ${uploadedPlayId} master @a`} />
                  <CommandLine label="Tocar num player" cmd={`/playsound ${uploadedPlayId} master <player>`} />
                  <CommandLine label="Tocar em coord" cmd={`/playsound ${uploadedPlayId} master @a ~ ~ ~ 1 1`} />
                  <CommandLine label="Pitch grave (medo)" cmd={`/playsound ${uploadedPlayId} master @a ~ ~ ~ 1 0.5`} />
                  <CommandLine label="Volume baixo (ambient)" cmd={`/playsound ${uploadedPlayId} ambient @a ~ ~ ~ 0.4 1`} />
                  <div className="text-[10px] text-emerald-200/70 mt-2 leading-relaxed">
                    💡 Cole esses comandos em <b>Console</b>, <b>Scripts</b>, ou use o playId no editor de <b>Story Beats</b>,{' '}
                    <b>Music Director</b>, <b>Dialog</b>, <b>Echo Whispers</b>.
                  </div>
                </div>
              )}

              <div className="mt-3 text-[10px] text-amber-300/80 leading-relaxed bg-amber-500/10 border border-amber-400/30 rounded-lg p-2">
                ⚠ <b>Importante:</b> WAV é enviado mas MC exige OGG Vorbis pra render fiel.
                Recomenda:
                <ol className="list-decimal list-inside mt-1 space-y-0.5">
                  <li>Download .wav acima</li>
                  <li>Converte: <code className="font-mono">ffmpeg -i {key}.wav -c:a libvorbis -q:a 5 {key}.ogg</code></li>
                  <li>Sobe o .ogg na página <b>Custom Sounds</b></li>
                </ol>
                Players precisam ter aceito o resource pack do servidor.
              </div>
            </div>
          )}

          {!rawBuffer && (
            <div className="card text-xs text-liberthia-300/70 leading-relaxed">
              <div className="font-bold text-liberthia-200 mb-2">💡 Como usar</div>
              <p>• Click <b>Começar gravação</b> e fale no microfone</p>
              <p>• Pare a gravação, escolha um preset ou ajuste manualmente</p>
              <p>• <b>Aplicar efeitos</b> processa em background (offline render)</p>
              <p>• Download como .wav OU upload direto</p>
              <p>• Use o som via <span className="chip">/playsound {namespace}:{key}</span> ou nas Story Beats</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function CommandLine({ label, cmd }: { label: string; cmd: string }) {
  return (
    <div className="flex items-center gap-2 mb-1.5 text-xs">
      <span className="text-emerald-200/60 w-28 shrink-0 hidden sm:inline">{label}:</span>
      <code className="flex-1 bg-black/40 px-2 py-1 rounded font-mono text-[11px] truncate text-emerald-100 select-all">{cmd}</code>
      <button className="btn-ghost btn-sm shrink-0"
        onClick={() => { navigator.clipboard.writeText(cmd); toast.ok('Copiado') }}
        title="Copiar">📋</button>
    </div>
  )
}

function PermissionHelp({ error, isSecure, hostname, onRetry }: {
  error: string; isSecure: boolean; hostname: string; onRetry: () => void
}) {
  const isLan = !isSecure && !!hostname && hostname !== 'localhost' && hostname !== '127.0.0.1'
  const httpsUrl = `https://${hostname}${window.location.port ? ':' + window.location.port : ''}${window.location.pathname}`
  const localhostUrl = `http://localhost${window.location.port ? ':' + window.location.port : ''}${window.location.pathname}`

  return (
    <div className="mt-3 p-3 rounded-lg bg-red-500/15 border border-red-400/40 text-sm">
      <div className="font-bold text-red-200 mb-1.5 flex items-center gap-2">
        <span className="text-xl">🎤</span>
        Mic bloqueado
      </div>

      {error === 'insecure-context' && (
        <div className="text-xs text-red-100/80 space-y-2">
          <p>O browser <b>só permite acesso ao microfone em HTTPS ou localhost</b>. Você tá em <code className="font-mono">{hostname}</code> (HTTP).</p>
          <p className="font-bold text-red-100">3 alternativas:</p>
          <ol className="list-decimal list-inside space-y-1 ml-1">
            <li>Use o <b>upload de arquivo</b> abaixo — funciona em qualquer contexto</li>
            {isLan && (
              <li>Abra o painel em <code className="font-mono text-emerald-200">{localhostUrl}</code> (na máquina onde o frontend roda)</li>
            )}
            <li>Configure HTTPS via Cloudflare Tunnel, Caddy ou Nginx + Let's Encrypt em <code className="font-mono">{hostname}</code></li>
            <li>Use <b>Brave/Chrome flags</b>: <code className="font-mono">chrome://flags/#unsafely-treat-insecure-origin-as-secure</code> → adiciona <code className="font-mono">{httpsUrl.replace('https://', 'http://')}</code></li>
          </ol>
        </div>
      )}

      {error === 'denied' && (
        <div className="text-xs text-red-100/80 space-y-2">
          <p>Permissão negada pelo browser. Pra liberar:</p>
          <ol className="list-decimal list-inside space-y-1 ml-1">
            <li>Click no <b>🔒 ícone de cadeado</b> na barra de endereço</li>
            <li>Mude "Microfone" pra <b>Permitir</b></li>
            <li>Recarregue a página</li>
          </ol>
          <button className="btn-ghost btn-sm mt-2" onClick={onRetry}>🔁 Tentar de novo</button>
        </div>
      )}

      {error === 'no-mic' && (
        <div className="text-xs text-red-100/80">
          <p>Nenhum microfone detectado no sistema. Conecta um e tenta novamente, ou use upload de arquivo abaixo.</p>
          <button className="btn-ghost btn-sm mt-2" onClick={onRetry}>🔁 Tentar de novo</button>
        </div>
      )}

      {error === 'no-api' && (
        <div className="text-xs text-red-100/80">
          <p>Este browser não suporta MediaRecorder. Use Chrome/Firefox/Edge atualizados, ou faça upload de arquivo.</p>
        </div>
      )}

      {error.startsWith('other:') && (
        <div className="text-xs text-red-100/80">
          <p>Erro inesperado:</p>
          <code className="block bg-black/30 p-1.5 rounded mt-1 break-all">{error.slice(6)}</code>
          <button className="btn-ghost btn-sm mt-2" onClick={onRetry}>🔁 Tentar de novo</button>
        </div>
      )}
    </div>
  )
}

function Slider({ label, value, min, max, step, onChange, format }: {
  label: string; value: number; min: number; max: number; step: number; onChange: (v: number) => void
  format?: (v: number) => string
}) {
  return (
    <div className="flex items-center gap-2">
      <span className="label w-20 shrink-0">{label}</span>
      <input type="range" min={min} max={max} step={step} value={value}
        onChange={(e) => onChange(Number(e.target.value))} className="flex-1" />
      <span className="font-mono w-16 text-right">{format ? format(value) : value.toFixed(2)}</span>
    </div>
  )
}

// ============ Audio processing ============

async function processBuffer(input: AudioBuffer, chain: EffectChain): Promise<AudioBuffer> {
  // FORÇA output em 48kHz MONO porque Simple Voice Chat só toca nessa
  // config — qualquer outro sample rate/channels o mod fica resampleando
  // frame a frame e dá travada brutal no jogo (sintoma "travadão").
  //
  // 48000 = sample rate padrão do SVC (e da WebRTC moderno).
  // 1 channel  = SVC é mono (positional audio = posição é a fonte).
  const sampleRate = 48000
  const outDuration = input.duration / chain.pitch + (chain.echoDelay > 0 ? 2 : 0.5)
  const offline = new OfflineAudioContext(1, Math.ceil(outDuration * sampleRate), sampleRate)
  const src = offline.createBufferSource()
  src.buffer = input
  src.playbackRate.value = chain.pitch

  // Chain: src -> highpass -> lowpass -> distortion -> [echo] -> [reverb] -> dst
  let node: AudioNode = src

  if (chain.highpass > 0) {
    const hp = offline.createBiquadFilter()
    hp.type = 'highpass'; hp.frequency.value = chain.highpass
    node.connect(hp); node = hp
  }

  if (chain.lowpass < 22050) {
    const lp = offline.createBiquadFilter()
    lp.type = 'lowpass'; lp.frequency.value = chain.lowpass
    node.connect(lp); node = lp
  }

  if (chain.distortion > 0) {
    const ws = offline.createWaveShaper()
    ws.curve = makeDistortionCurve(chain.distortion) as any
    ws.oversample = '2x'
    node.connect(ws); node = ws
  }

  if (chain.echoDelay > 0) {
    const delay = offline.createDelay(2)
    delay.delayTime.value = chain.echoDelay / 1000
    const fb = offline.createGain(); fb.gain.value = chain.echoFeedback
    const wet = offline.createGain(); wet.gain.value = 0.7
    const dry = offline.createGain(); dry.gain.value = 1.0
    node.connect(dry); dry.connect(offline.destination)
    node.connect(delay)
    delay.connect(fb); fb.connect(delay)
    delay.connect(wet); wet.connect(offline.destination)
  } else if (chain.reverb > 0) {
    const conv = offline.createConvolver()
    conv.buffer = makeImpulseResponse(offline, 1.5, 2)
    const wet = offline.createGain(); wet.gain.value = chain.reverb
    const dry = offline.createGain(); dry.gain.value = 1 - chain.reverb * 0.7
    node.connect(dry); dry.connect(offline.destination)
    node.connect(conv); conv.connect(wet); wet.connect(offline.destination)
  } else {
    node.connect(offline.destination)
  }

  src.start(0)
  return await offline.startRendering()
}

function makeDistortionCurve(amount: number): Float32Array {
  const k = amount / 100 * 100
  const n = 44100
  const curve = new Float32Array(n)
  const deg = Math.PI / 180
  for (let i = 0; i < n; i++) {
    const x = (i * 2) / n - 1
    curve[i] = (3 + k) * x * 20 * deg / (Math.PI + k * Math.abs(x))
  }
  return curve
}

function makeImpulseResponse(ctx: BaseAudioContext, durationSec: number, decay: number): AudioBuffer {
  const rate = ctx.sampleRate
  const length = rate * durationSec
  // MONO impulse — antes criava stereo (2 canais) que falhava de conectar
  // num offline context mono. Agora respeita o destination context.
  const channels = ctx.destination.channelCount || 1
  const impulse = ctx.createBuffer(channels, length, rate)
  for (let c = 0; c < channels; c++) {
    const data = impulse.getChannelData(c)
    for (let i = 0; i < length; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / length, decay)
    }
  }
  return impulse
}

function bufferToWav(buffer: AudioBuffer): Blob {
  const numCh = buffer.numberOfChannels
  const sampleRate = buffer.sampleRate
  const format = 1 // PCM
  const bitDepth = 16

  const channels: Float32Array[] = []
  for (let c = 0; c < numCh; c++) channels.push(buffer.getChannelData(c))
  const length = buffer.length
  const bytesPerSample = bitDepth / 8
  const blockAlign = numCh * bytesPerSample
  const byteRate = sampleRate * blockAlign
  const dataSize = length * blockAlign
  const bufferOut = new ArrayBuffer(44 + dataSize)
  const view = new DataView(bufferOut)

  writeStr(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataSize, true)
  writeStr(view, 8, 'WAVE')
  writeStr(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, format, true)
  view.setUint16(22, numCh, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, byteRate, true)
  view.setUint16(32, blockAlign, true)
  view.setUint16(34, bitDepth, true)
  writeStr(view, 36, 'data')
  view.setUint32(40, dataSize, true)

  let offset = 44
  for (let i = 0; i < length; i++) {
    for (let c = 0; c < numCh; c++) {
      const s = Math.max(-1, Math.min(1, channels[c][i]))
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
      offset += 2
    }
  }
  return new Blob([bufferOut], { type: 'audio/wav' })
}

function writeStr(view: DataView, offset: number, str: string) {
  for (let i = 0; i < str.length; i++) view.setUint8(offset + i, str.charCodeAt(i))
}
