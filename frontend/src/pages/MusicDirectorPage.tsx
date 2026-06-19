import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Music Director multi-track — tipo um pequeno DAW.
 * Múltiplas TRACKS independentes, cada uma com seus eventos.
 * Tracks têm mute/solo/volume independentes.
 * Playhead único toca tudo em sincronia.
 */

type TrackEvent = { id: number; tMs: number; sound: string; pitch: number; volume: number }
type Track = {
  id: string
  name: string
  color: string
  events: TrackEvent[]
  mute: boolean
  solo: boolean
  volume: number   // master volume multiplier 0..1
}


const SOUND_BANK = [
  // Note blocks (melodia)
  ['minecraft:block.note_block.harp', '🎵 Harp'],
  ['minecraft:block.note_block.bell', '🔔 Bell'],
  ['minecraft:block.note_block.flute', '🪈 Flute'],
  ['minecraft:block.note_block.chime', '✨ Chime'],
  ['minecraft:block.note_block.bit', '🤖 Bit'],
  ['minecraft:block.note_block.banjo', '🪕 Banjo'],
  ['minecraft:block.note_block.pling', '🎹 Pling'],
  ['minecraft:block.note_block.guitar', '🎸 Guitar'],
  ['minecraft:block.note_block.didgeridoo', '🪗 Didge'],
  ['minecraft:block.note_block.cow_bell', '🐄 Cow Bell'],
  // Percussion
  ['minecraft:block.note_block.bass', '🪘 Bass'],
  ['minecraft:block.note_block.basedrum', '🥁 Bass Drum'],
  ['minecraft:block.note_block.snare', '🥁 Snare'],
  ['minecraft:block.note_block.hat', '🥁 Hi-hat'],
  // Misc
  ['minecraft:music_disc.cat.wait', '💿 Cat'],
  ['minecraft:music_disc.13.wait', '💿 13'],
  ['minecraft:entity.experience_orb.pickup', '✨ XP'],
  ['minecraft:entity.player.levelup', '⭐ LevelUp'],
  ['minecraft:ui.toast.challenge_complete', '🏆 Achievement'],
  ['minecraft:block.bell.use', '🔔 Big Bell'],
  ['minecraft:block.amethyst_block.chime', '💎 Amethyst'],
  ['minecraft:entity.wither.spawn', '☠ Wither'],
  ['minecraft:entity.lightning_bolt.thunder', '⛈ Thunder'],
] as const

const TRACK_COLORS = ['#aa40e8', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#8b5cf6', '#22d3ee']

export function MusicDirectorPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const soundsQ = useQuery({ queryKey: ['sounds'], queryFn: api.soundsList, retry: false })
  const players = playersQ.data ?? []
  const customSounds = soundsQ.data?.sounds ?? []
  const [target, setTarget] = useState<'all' | string>('all')

  const [tracks, setTracks] = useKvState<Track[]>('music_project',
    [{ id: 'melody', name: 'Melodia', color: TRACK_COLORS[0], events: [], mute: false, solo: false, volume: 1 }])

  const [running, setRunning] = useState(false)
  const [progressMs, setProgressMs] = useState(0)
  const playRef = useRef<{ cancel: boolean; timers: number[] } | null>(null)

  // Totais
  const allEvents = tracks.flatMap((t) => t.events.map((e) => ({ ...e, trackId: t.id, color: t.color })))
  const totalMs = allEvents.length > 0 ? Math.max(...allEvents.map((e) => e.tMs)) + 500 : 0
  const timelineWidth = Math.max(900, totalMs / 18)
  function tToPx(t: number) { return (t / Math.max(totalMs || 1, 1)) * timelineWidth }

  // Effective tracks: se algum solo, só toca solos; senão respeita mute
  const anySolo = tracks.some((t) => t.solo)
  const effective = tracks.filter((t) => anySolo ? t.solo : !t.mute)

  function newTrack() {
    setTracks((cur) => [...cur, {
      id: `t${Date.now().toString(36)}`,
      name: 'Track ' + (cur.length + 1),
      color: TRACK_COLORS[cur.length % TRACK_COLORS.length],
      events: [],
      mute: false, solo: false, volume: 1,
    }])
  }

  function updateTrack(id: string, p: Partial<Track>) {
    setTracks((cur) => cur.map((t) => t.id === id ? { ...t, ...p } : t))
  }

  function removeTrack(id: string) {
    setTracks((cur) => cur.filter((t) => t.id !== id))
  }

  function addEvent(trackId: string, tMs: number) {
    setTracks((cur) => cur.map((t) => t.id === trackId
      ? { ...t, events: [...t.events, { id: Date.now() + Math.random(), tMs, sound: SOUND_BANK[0][0], pitch: 1, volume: 1 }] }
      : t))
  }

  function updateEvent(trackId: string, evId: number, p: Partial<TrackEvent>) {
    setTracks((cur) => cur.map((t) => t.id === trackId
      ? { ...t, events: t.events.map((e) => e.id === evId ? { ...e, ...p } : e) }
      : t))
  }

  function removeEvent(trackId: string, evId: number) {
    setTracks((cur) => cur.map((t) => t.id === trackId ? { ...t, events: t.events.filter((e) => e.id !== evId) } : t))
  }

  async function play() {
    if (running || effective.length === 0) return
    const totalEvents = effective.reduce((a, t) => a + t.events.length, 0)
    if (totalEvents === 0) { toast.err('Sem eventos pra tocar'); return }
    setRunning(true); setProgressMs(0)
    const targetUuids = target === 'all' ? players.map((p) => p.uuid) : [target]
    const ctl = { cancel: false, timers: [] as number[] }
    playRef.current = ctl
    const start = Date.now()
    for (const track of effective) {
      for (const e of track.events) {
        const id = window.setTimeout(() => {
          if (ctl.cancel) return
          const finalVol = (e.volume * track.volume) || 0
          if (finalVol > 0) {
            for (const u of targetUuids) api.sound(u, e.sound, finalVol, e.pitch).catch(() => {})
          }
        }, e.tMs)
        ctl.timers.push(id)
      }
    }
    const progressTimer = window.setInterval(() => {
      if (ctl.cancel) { clearInterval(progressTimer); return }
      const e = Date.now() - start
      setProgressMs(e)
      if (e > totalMs) { clearInterval(progressTimer); setRunning(false) }
    }, 100)
  }

  function stop() {
    if (playRef.current) {
      playRef.current.cancel = true
      playRef.current.timers.forEach((t) => clearTimeout(t))
    }
    setRunning(false); setProgressMs(0)
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎼 Music Director</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            DAW multi-track. Crie melodia + percussão + harmonia em camadas. Mute/solo/volume por track.
          </p>
        </div>
        <div className="flex gap-2 items-center">
          <select className="input text-xs w-40" value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="all">🌐 Todos</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
          </select>
          <button className="btn-ghost btn-sm" onClick={newTrack}>+ Track</button>
          {!running && <button className="btn-success" onClick={play} disabled={effective.length === 0}>▶ Play</button>}
          {running && <button className="btn-danger" onClick={stop}>⏹ Stop</button>}
        </div>
      </header>

      {/* Big timeline preview (todas tracks stacked) */}
      <div className="card-glow mb-4 overflow-x-auto">
        <div className="relative bg-liberthia-900/40 rounded-lg" style={{ minWidth: timelineWidth + 60, height: 24 + tracks.length * 20 }}>
          {/* Time grid */}
          {Array.from({ length: Math.ceil(Math.max(totalMs, 5000) / 1000) + 1 }, (_, i) => (
            <div key={i} className="absolute top-0 bottom-0 border-l border-liberthia-500/15" style={{ left: tToPx(i * 1000) }}>
              <div className="text-[10px] text-liberthia-300/50 px-1">{i}s</div>
            </div>
          ))}
          {/* Events stacked por track */}
          {tracks.map((t, ti) => (
            <div key={t.id} className="absolute" style={{ top: 22 + ti * 20, height: 16, left: 0, right: 0 }}>
              {t.events.map((e) => (
                <div key={e.id}
                  className="absolute h-3.5 w-2 rounded-sm shadow"
                  style={{ left: tToPx(e.tMs) - 4, background: t.color, opacity: t.mute && !t.solo ? 0.3 : 1 }}
                  title={`${t.name} @ ${e.tMs}ms`}
                />
              ))}
            </div>
          ))}
          {/* Playhead */}
          {running && (
            <div className="absolute top-0 bottom-0 w-0.5 bg-emerald-400 shadow-[0_0_8px_#34d399]" style={{ left: tToPx(progressMs) }} />
          )}
        </div>
      </div>

      {/* Tracks */}
      <div className="space-y-4">
        {tracks.map((t) => (
          <div key={t.id} className="card-glow" style={{ borderColor: t.color + '40' }}>
            {/* Header */}
            <div className="flex items-center gap-3 mb-3 flex-wrap">
              <div className="w-3 h-12 rounded shrink-0" style={{ background: t.color }} />
              <input className="input font-bold w-40" value={t.name} onChange={(e) => updateTrack(t.id, { name: e.target.value })} />
              <input type="color" className="input h-10 w-14" value={t.color} onChange={(e) => updateTrack(t.id, { color: e.target.value })} />
              <button
                className={`btn-ghost btn-sm ${t.mute ? '!bg-red-500/30 !text-red-200' : ''}`}
                onClick={() => updateTrack(t.id, { mute: !t.mute })}
                title="Mute"
              >🔇 M</button>
              <button
                className={`btn-ghost btn-sm ${t.solo ? '!bg-amber-500/30 !text-amber-200' : ''}`}
                onClick={() => updateTrack(t.id, { solo: !t.solo })}
                title="Solo"
              >S</button>
              <div className="flex items-center gap-1">
                <span className="text-[10px] text-liberthia-300/50">Vol</span>
                <input type="range" min={0} max={1} step={0.05} value={t.volume}
                  onChange={(e) => updateTrack(t.id, { volume: Number(e.target.value) })} className="w-24" />
              </div>
              <span className="text-[10px] text-liberthia-300/50 ml-auto">{t.events.length} eventos</span>
              <button className="btn-ghost btn-sm" onClick={() => removeTrack(t.id)}>🗑</button>
            </div>

            {/* Mini timeline da track */}
            <div className="relative h-16 bg-liberthia-900/50 rounded-lg cursor-pointer mb-3"
              onClick={(e) => {
                const r = e.currentTarget.getBoundingClientRect()
                const px = e.clientX - r.left
                const tMs = (px / r.width) * Math.max(totalMs || 5000, 5000)
                addEvent(t.id, Math.round(tMs / 100) * 100)
              }}>
              {Array.from({ length: Math.ceil(Math.max(totalMs, 5000) / 1000) + 1 }, (_, i) => (
                <div key={i} className="absolute top-0 bottom-0 border-l border-liberthia-500/15" style={{ left: `${(i * 1000 / Math.max(totalMs || 5000, 5000)) * 100}%` }} />
              ))}
              {t.events.map((e) => (
                <div key={e.id}
                  className="absolute top-1 h-14 w-2 rounded shadow cursor-grab"
                  style={{ left: `calc(${(e.tMs / Math.max(totalMs || 5000, 5000)) * 100}% - 4px)`, background: t.color, boxShadow: `0 0 6px ${t.color}` }}
                  onClick={(ev) => ev.stopPropagation()}
                  title={`${e.tMs}ms · ${e.sound}`}
                />
              ))}
            </div>

            {/* Event list (compact) */}
            <details>
              <summary className="cursor-pointer text-xs text-liberthia-300/70 mb-2">📋 Eventos ({t.events.length})</summary>
              <div className="space-y-1 mt-2">
                {[...t.events].sort((a, b) => a.tMs - b.tMs).map((e) => (
                  <div key={e.id} className="grid grid-cols-[80px_1fr_90px_90px_30px] gap-1.5 items-center bg-liberthia-900/40 p-1.5 rounded">
                    <input type="number" className="input text-xs !py-1" value={e.tMs} step={50}
                      onChange={(ev) => updateEvent(t.id, e.id, { tMs: Number(ev.target.value) })} />
                    <select className="input text-xs !py-1 font-mono" value={e.sound}
                      onChange={(ev) => updateEvent(t.id, e.id, { sound: ev.target.value })}>
                      <optgroup label="Vanilla">
                        {SOUND_BANK.map(([id, lbl]) => <option key={id} value={id}>{lbl}</option>)}
                      </optgroup>
                      {customSounds.length > 0 && (
                        <optgroup label="Custom (uploaded)">
                          {customSounds.map((cs) => <option key={cs.playId} value={cs.playId}>🎵 {cs.playId}</option>)}
                        </optgroup>
                      )}
                    </select>
                    <div>
                      <input type="range" min={0.5} max={2} step={0.05} value={e.pitch}
                        onChange={(ev) => updateEvent(t.id, e.id, { pitch: Number(ev.target.value) })} className="w-full" />
                      <div className="text-[9px] text-center text-liberthia-300/50">p{e.pitch.toFixed(2)}</div>
                    </div>
                    <div>
                      <input type="range" min={0} max={1} step={0.05} value={e.volume}
                        onChange={(ev) => updateEvent(t.id, e.id, { volume: Number(ev.target.value) })} className="w-full" />
                      <div className="text-[9px] text-center text-liberthia-300/50">v{e.volume.toFixed(2)}</div>
                    </div>
                    <button className="btn-ghost btn-sm" onClick={() => removeEvent(t.id, e.id)}>🗑</button>
                  </div>
                ))}
                <button className="btn-ghost btn-sm w-full" onClick={() => addEvent(t.id, totalMs || 0)}>+ Evento no final</button>
              </div>
            </details>
          </div>
        ))}
      </div>

      <div className="card mt-4 text-xs text-liberthia-300/70">
        <p>• Click na mini-timeline de uma track pra adicionar evento naquele ponto temporal</p>
        <p>• <b>Mute (M)</b> silencia a track · <b>Solo (S)</b> toca só as marcadas como solo</p>
        <p>• <b>Volume da track</b> multiplica o volume de cada evento</p>
        <p>• Sons custom (uploadados) aparecem automaticamente no dropdown</p>
      </div>
    </div>
  )
}
