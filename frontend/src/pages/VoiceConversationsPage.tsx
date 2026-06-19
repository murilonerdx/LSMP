import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, VoicePlayerSummary, VoiceClipDto } from '../lib/api'

/**
 * Voice Conversations — selecione 1+ players e um range de tempo, e ouça os
 * clipes em ordem cronológica como se fosse uma "conversa".
 *
 * UX:
 *  - Sidebar: lista de players com checkbox (multi-select)
 *  - Range picker: "últimos N minutos" (presets ou custom)
 *  - Player principal: auto-play sequencial, controles, velocidade
 *  - Lista cronológica com highlight no clipe atual
 */

const TIME_PRESETS = [
  { label: '15min', minutes: 15 },
  { label: '30min', minutes: 30 },
  { label: '1h', minutes: 60 },
  { label: '2h', minutes: 120 },
  { label: '4h', minutes: 240 },
  { label: '12h', minutes: 720 },
  { label: '24h', minutes: 1440 },
]

const SPEED_OPTIONS = [0.75, 1.0, 1.25, 1.5, 2.0]

function fmtMs(ms: number): string {
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}h ${m}m ${sec}s`
  if (m > 0) return `${m}m ${sec}s`
  return `${sec}s`
}
function fmtBytes(b: number): string {
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  return `${(b / (1024 * 1024)).toFixed(1)}MB`
}

export function VoiceConversationsPage() {
  const [selectedUuids, setSelectedUuids] = useState<Set<string>>(new Set())
  const [lastMinutes, setLastMinutes] = useState<number>(60)
  const [customMinutes, setCustomMinutes] = useState<number>(60)
  const [useCustom, setUseCustom] = useState(false)
  const [currentIdx, setCurrentIdx] = useState<number>(-1)
  const [autoPlay, setAutoPlay] = useState(true)
  const [playbackRate, setPlaybackRate] = useState(1.0)
  const [paused, setPaused] = useState(true)
  const audioRef = useRef<HTMLAudioElement>(null)
  const listRef = useRef<HTMLDivElement>(null)
  // Sinal explícito de "queremos tocar ao mudar de clipe". Resolve race
  // condition: antes a useEffect lia `paused` da closure, mas como o handler
  // de onEnded só chamava setCurrentIdx (sem mexer em paused), tudo dependia
  // do paused estar exatamente "false" no momento do re-render. Eventos de
  // 'pause' disparados pelo browser ao trocar `src` zoavam isso. Ref bypassa
  // o ciclo de render do React.
  const playIntent = useRef(false)

  const playersQ = useQuery({
    queryKey: ['voice-library-players-for-conv'],
    queryFn: api.voiceLibraryPlayers,
    refetchInterval: 30_000,
  })
  const minutes = useCustom ? customMinutes : lastMinutes
  const convQ = useQuery({
    enabled: selectedUuids.size > 0,
    queryKey: ['voice-conversation', Array.from(selectedUuids).sort().join(','), minutes],
    queryFn: () => api.voiceConversation({
      playerUuids: Array.from(selectedUuids),
      lastMinutes: minutes,
      limit: 1000,
    }),
    refetchInterval: 30_000,
  })

  const players = Array.isArray(playersQ.data?.players) ? playersQ.data!.players : []
  const clips = useMemo<VoiceClipDto[]>(() => convQ.data?.clips ?? [], [convQ.data])
  const currentClip = currentIdx >= 0 && currentIdx < clips.length ? clips[currentIdx] : null

  // Player name lookup
  const nameByUuid = useMemo(() => {
    const m: Record<string, string> = {}
    for (const p of players) m[p.playerUuid] = p.playerName
    return m
  }, [players])

  // Toggle player
  function togglePlayer(uuid: string) {
    setSelectedUuids(prev => {
      const n = new Set(prev)
      if (n.has(uuid)) n.delete(uuid)
      else n.add(uuid)
      return n
    })
    setCurrentIdx(-1)
  }
  function selectAll() {
    setSelectedUuids(new Set(players.map(p => p.playerUuid)))
    setCurrentIdx(-1)
  }
  function clearAll() {
    setSelectedUuids(new Set())
    setCurrentIdx(-1)
  }

  // Quando a lista de clipes muda, reset cursor pro último (mais recente)
  useEffect(() => {
    if (clips.length > 0 && currentIdx === -1) {
      // começa no PRIMEIRO clipe da janela (mais antigo) — a conversa toca
      // do passado pro presente. Se quiser inverter, mudar pra clips.length-1.
      setCurrentIdx(0)
    }
  }, [clips.length, currentIdx])

  // Quando o clipe muda, carrega o áudio. Se playIntent estiver true, toca
  // automaticamente assim que load + canplay disparam. Não lemos `paused`
  // aqui — antes era a fonte do bug do autoplay (a closure ficava stale ou
  // o browser disparava 'pause' ao trocar src, derrubando paused→true).
  useEffect(() => {
    const a = audioRef.current
    if (!a || !currentClip) return
    const newSrc = api.voiceAudioUrl(currentClip.id)
    if (a.src !== newSrc) {
      a.src = newSrc
      a.load() // força re-load — sem isso, audio pode ficar em estado transitivo entre clipes
    }
    a.playbackRate = playbackRate
    if (playIntent.current) {
      // play() retorna promise. Chamamos depois do tick atual pra dar tempo
      // do browser começar o load. .catch() captura autoplay-policy block.
      const tryPlay = () => a.play()
        .then(() => { playIntent.current = false })
        .catch(err => {
          console.warn('[voice-conv] play() falhou:', err)
          playIntent.current = false
          setPaused(true)
        })
      // Aguarda canplay se ainda não pronto, senão toca imediato
      if (a.readyState >= 2) {
        tryPlay()
      } else {
        const onReady = () => {
          a.removeEventListener('canplay', onReady)
          tryPlay()
        }
        a.addEventListener('canplay', onReady, { once: true })
      }
    }
  }, [currentClip?.id])

  useEffect(() => {
    const a = audioRef.current
    if (a) a.playbackRate = playbackRate
  }, [playbackRate])

  // Scroll automático pra deixar o clipe atual visível
  useEffect(() => {
    if (currentIdx < 0 || !listRef.current) return
    const el = listRef.current.querySelector(`[data-idx="${currentIdx}"]`) as HTMLElement | null
    if (el) el.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [currentIdx])

  function handleAudioEnded() {
    if (autoPlay && currentIdx + 1 < clips.length) {
      // Sinaliza intent ANTES de mudar idx — useEffect captura ao re-renderizar
      playIntent.current = true
      setCurrentIdx(currentIdx + 1)
    } else {
      setPaused(true) // chegou no fim ou autoplay off
    }
  }
  function playClip(idx: number) {
    playIntent.current = true
    setCurrentIdx(idx)
    setPaused(false)
  }
  function skipNext() {
    if (currentIdx + 1 < clips.length) {
      playIntent.current = !paused // mantém o "tocando" se já estava
      setCurrentIdx(currentIdx + 1)
    }
  }
  function skipPrev() {
    if (currentIdx > 0) {
      playIntent.current = !paused
      setCurrentIdx(currentIdx - 1)
    }
  }

  const totalDuration = convQ.data?.totalDurationMs ?? 0
  const totalBytes = convQ.data?.totalBytes ?? 0

  return (
    <div className="route-fade max-w-[1600px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎙 Voice Conversations</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Selecione players + tempo e ouça a "conversa" deles em ordem cronológica.
            Auto-play sequencial — sente, ouve, espia.
          </p>
        </div>
        <div className="flex gap-2 text-xs items-center">
          {selectedUuids.size > 0 && (
            <>
              <span className="badge badge-purple">{selectedUuids.size} player{selectedUuids.size === 1 ? '' : 's'}</span>
              <span className="badge badge-purple">{clips.length} clipes</span>
              <span className="badge badge-purple">{fmtMs(totalDuration)} total</span>
              <span className="badge">{fmtBytes(totalBytes)}</span>
            </>
          )}
        </div>
      </header>

      <div className="grid grid-cols-[260px_1fr] gap-3">
        {/* ============ SIDEBAR: Players ============ */}
        <aside className="card-glow p-2 space-y-2 max-h-[80vh] overflow-y-auto">
          <div className="flex items-center justify-between text-[10px]">
            <span className="uppercase tracking-widest text-liberthia-300/60">Players</span>
            <div className="flex gap-1">
              <button className="btn-ghost btn-sm text-[10px] py-0.5 px-1.5" onClick={selectAll}>Todos</button>
              <button className="btn-ghost btn-sm text-[10px] py-0.5 px-1.5" onClick={clearAll}>×</button>
            </div>
          </div>
          {players.map(p => {
            const checked = selectedUuids.has(p.playerUuid)
            return (
              <button key={p.playerUuid}
                onClick={() => togglePlayer(p.playerUuid)}
                className={`w-full text-left rounded p-2 flex items-center gap-2 transition ${
                  checked ? 'bg-purple-500/30 ring-1 ring-purple-400' : 'bg-liberthia-900/40 hover:bg-liberthia-900/70'
                }`}>
                <input type="checkbox" checked={checked} readOnly className="shrink-0" />
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.playerName)}/24`}
                  className="rounded" />
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-bold truncate">{p.playerName}</div>
                  <div className="text-[10px] text-liberthia-300/50">{p.clipCount} clipes · {fmtMs(p.totalDurationMs)}</div>
                </div>
              </button>
            )
          })}
          {!playersQ.isLoading && players.length === 0 && (
            <div className="text-center text-[11px] text-liberthia-300/60 py-4">Sem players.</div>
          )}
        </aside>

        {/* ============ MAIN: Player + timeline ============ */}
        <main className="space-y-3">
          {/* Time range */}
          <div className="card-glow space-y-2">
            <div className="flex items-center gap-2 text-xs flex-wrap">
              <span className="text-liberthia-300/70 font-bold">⏱ Janela:</span>
              {TIME_PRESETS.map(p => (
                <button key={p.minutes}
                  className={`btn-ghost btn-sm ${!useCustom && lastMinutes === p.minutes ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
                  onClick={() => { setUseCustom(false); setLastMinutes(p.minutes); setCurrentIdx(-1) }}>
                  {p.label}
                </button>
              ))}
              <span className="text-liberthia-300/40">|</span>
              <button className={`btn-ghost btn-sm ${useCustom ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
                onClick={() => { setUseCustom(true); setCurrentIdx(-1) }}>
                ⚙ Custom
              </button>
              {useCustom && (
                <div className="flex items-center gap-1 text-xs">
                  <input type="number" min={1} max={10080} className="input w-20"
                    value={customMinutes}
                    onChange={(e) => { setCustomMinutes(Number(e.target.value) || 1); setCurrentIdx(-1) }} />
                  <span className="text-liberthia-300/50">minutos</span>
                </div>
              )}
            </div>
            {convQ.data && (
              <div className="flex items-center justify-between gap-2">
                <div className="text-[10px] text-liberthia-300/50">
                  {new Date(convQ.data.fromTs).toLocaleString()} → {new Date(convQ.data.toTs).toLocaleString()}
                </div>
                {clips.length > 0 && (
                  <button className="btn btn-sm bg-purple-500/30 hover:bg-purple-500/50 ring-1 ring-purple-400/60"
                    onClick={() => playClip(0)}
                    title="Inicia a conversa do clipe mais antigo">
                    ▶ Iniciar conversa (do início)
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Now playing */}
          {currentClip && (
            <div className="card-glow border-l-4 border-l-purple-400">
              <div className="flex items-center gap-3">
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(currentClip.playerName || nameByUuid[currentClip.playerUuid] || '?')}/48`}
                  className="rounded" />
                <div className="flex-1 min-w-0">
                  <div className="text-xs text-liberthia-300/60">▶ Tocando #{currentIdx + 1}/{clips.length}</div>
                  <div className="font-bold">{currentClip.playerName || nameByUuid[currentClip.playerUuid]}</div>
                  <div className="text-[10px] text-liberthia-300/50">
                    {new Date(currentClip.ts).toLocaleString()} · {fmtMs(currentClip.durationMs)}
                  </div>
                </div>
                {currentClip.transcription && (
                  // Now-playing — transcrição completa, quebra em múltiplas linhas
                  <div className="text-xs italic text-liberthia-300/80 max-w-2xl whitespace-pre-wrap break-words">
                    "{currentClip.transcription}"
                  </div>
                )}
              </div>
              <audio ref={audioRef}
                controls
                className="w-full h-10 mt-2"
                onEnded={handleAudioEnded}
                onPlay={() => setPaused(false)}
                onPause={() => setPaused(true)} />
              <div className="flex items-center gap-2 mt-2 text-xs">
                <button className="btn-ghost btn-sm" onClick={skipPrev} disabled={currentIdx <= 0}>⏮ Anterior</button>
                <button className="btn-ghost btn-sm" onClick={skipNext} disabled={currentIdx + 1 >= clips.length}>Próximo ⏭</button>
                <label className="flex items-center gap-1 ml-2">
                  <input type="checkbox" checked={autoPlay} onChange={(e) => setAutoPlay(e.target.checked)} />
                  <span>Auto-play sequencial</span>
                </label>
                <div className="ml-auto flex items-center gap-1">
                  <span className="text-liberthia-300/60">Velocidade:</span>
                  {SPEED_OPTIONS.map(s => (
                    <button key={s}
                      className={`btn-ghost btn-sm text-[10px] py-0 px-1.5 ${playbackRate === s ? 'ring-1 ring-purple-400' : ''}`}
                      onClick={() => setPlaybackRate(s)}>{s}×</button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Lista cronológica */}
          {selectedUuids.size === 0 ? (
            <div className="card text-center py-12 text-liberthia-300/60">
              <div className="text-4xl mb-2 opacity-50">🎙</div>
              <p>Selecione 1+ players na sidebar pra começar a conversa.</p>
            </div>
          ) : (
            <div ref={listRef} className="card max-h-[55vh] overflow-y-auto space-y-1">
              {convQ.isLoading && <div className="text-xs text-liberthia-300/60 py-2 text-center">Carregando…</div>}
              {!convQ.isLoading && clips.length === 0 && (
                <div className="text-xs text-liberthia-300/60 py-6 text-center italic">
                  Nenhum áudio na janela escolhida. Aumenta o tempo ou adiciona mais players.
                </div>
              )}
              {clips.map((c, idx) => {
                const isCurrent = idx === currentIdx
                const prevTs = idx > 0 ? clips[idx - 1].ts : c.ts
                const gapMs = c.ts - prevTs
                return (
                  <div key={c.id}>
                    {gapMs > 30_000 && idx > 0 && (
                      <div className="text-[10px] text-liberthia-300/40 italic text-center py-1">
                        ⋯ gap {fmtMs(gapMs)} ⋯
                      </div>
                    )}
                    <button data-idx={idx}
                      onClick={() => playClip(idx)}
                      className={`w-full text-left rounded p-2 flex items-center gap-2 transition ${
                        isCurrent
                          ? 'bg-purple-500/30 ring-2 ring-purple-400 pulse-glow'
                          : 'bg-liberthia-900/40 hover:bg-liberthia-900/70'
                      }`}>
                      <span className="text-[10px] text-liberthia-300/40 w-6 text-right">#{idx + 1}</span>
                      <img src={`https://mc-heads.net/avatar/${encodeURIComponent(c.playerName || nameByUuid[c.playerUuid] || '?')}/24`}
                        className="rounded shrink-0" />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm truncate">{c.playerName || nameByUuid[c.playerUuid]}</span>
                          {isCurrent && <span className="text-[10px] text-purple-300">▶ now</span>}
                        </div>
                        {c.transcription && (
                          // Linha do clipe — mantém UMA linha truncada na lista
                          // (clicar mostra completo no now-playing acima)
                          <div className="text-[11px] text-liberthia-300/60 italic line-clamp-2">"{c.transcription}"</div>
                        )}
                      </div>
                      <div className="text-[10px] text-liberthia-300/50 text-right shrink-0">
                        <div>{new Date(c.ts).toLocaleTimeString()}</div>
                        <div className="font-mono">{fmtMs(c.durationMs)}</div>
                      </div>
                    </button>
                  </div>
                )
              })}
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
