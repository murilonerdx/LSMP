import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Track = {
  type: 'title' | 'sound' | 'particle' | 'weather' | 'broadcast' | 'spawnEntity' | 'spawnPlayerClone' | 'teleport' | 'lightning' | 'freeze' | 'unfreeze' | 'voice' | 'explosion' | 'time' | 'command'
  atMs: number
  payload: any
}

type Cutscene = {
  id: number; name: string; description: string;
  tracksJson: string; durationMs: number; tags: string;
  plays: number; updatedAt: string;
}

const TRACK_TYPES: Array<{ type: Track['type']; label: string; emoji: string; color: string }> = [
  { type: 'title', label: 'Title (texto na tela)', emoji: '📝', color: 'bg-blue-700' },
  { type: 'sound', label: 'Sound (toca som)', emoji: '🔊', color: 'bg-amber-700' },
  { type: 'particle', label: 'Particle (efeito visual)', emoji: '✨', color: 'bg-fuchsia-700' },
  { type: 'weather', label: 'Weather (muda clima)', emoji: '⛈', color: 'bg-cyan-700' },
  { type: 'time', label: 'Time (muda hora)', emoji: '🌙', color: 'bg-indigo-700' },
  { type: 'broadcast', label: 'Broadcast (msg pra todos)', emoji: '📢', color: 'bg-rose-700' },
  { type: 'spawnEntity', label: 'Spawn Mob', emoji: '🐺', color: 'bg-red-700' },
  { type: 'spawnPlayerClone', label: 'Spawn NPC (player clone)', emoji: '🎭', color: 'bg-purple-700' },
  { type: 'teleport', label: 'Teleport player', emoji: '🌀', color: 'bg-emerald-700' },
  { type: 'lightning', label: 'Lightning (raio)', emoji: '⚡', color: 'bg-yellow-700' },
  { type: 'freeze', label: 'Freeze player', emoji: '🧊', color: 'bg-sky-700' },
  { type: 'unfreeze', label: 'Unfreeze player', emoji: '🌡', color: 'bg-orange-700' },
  { type: 'voice', label: 'Voice clip (áudio)', emoji: '🎤', color: 'bg-pink-700' },
  { type: 'explosion', label: 'Explosion', emoji: '💥', color: 'bg-red-900' },
  { type: 'command', label: 'Comando raw', emoji: '⌨️', color: 'bg-zinc-700' },
]

const TIMELINE_DURATION = 30_000  // 30s visual default
const PX_PER_MS = 60 / 1000        // 60px = 1s

export function CutsceneDirectorPage() {
  const [selected, setSelected] = useState<Cutscene | null>(null)
  const [editing, setEditing] = useState<Cutscene | null>(null)
  const [draft, setDraft] = useState<Partial<Cutscene>>({})
  const [tracks, setTracks] = useState<Track[]>([])
  const [editingTrack, setEditingTrack] = useState<number | null>(null)
  const [timelineDuration, setTimelineDuration] = useState(TIMELINE_DURATION)
  const [preview, setPreview] = useState<Cutscene | null>(null)
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['cutscenes-director'],
    queryFn: async () => (await fetch('/api/cutscenes-director', { headers: headers() })).json(),
  })

  const save = useMutation({
    mutationFn: async (body: any) => {
      const url = body.id ? `/api/cutscenes-director/${body.id}` : '/api/cutscenes-director'
      const r = await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(), body: JSON.stringify({ ...body, tracksJson: JSON.stringify(tracks), durationMs: timelineDuration }) })
      return r.json()
    },
    onSuccess: () => { setEditing(null); setDraft({}); setTracks([]); qc.invalidateQueries({ queryKey: ['cutscenes-director'] }) },
  })

  const play = useMutation({
    mutationFn: async (id: number) => fetch(`/api/cutscenes-director/${id}/play`, { method: 'POST', headers: headers() }),
  })

  const stop = useMutation({
    mutationFn: async (id: number) => fetch(`/api/cutscenes-director/${id}/stop`, { method: 'POST', headers: headers() }),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/cutscenes-director/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cutscenes-director'] }),
  })

  const items: Cutscene[] = Array.isArray(q.data?.content) ? q.data.content : []

  function openEditor(c?: Cutscene) {
    if (c) {
      setEditing(c)
      setDraft({ ...c })
      try { setTracks(JSON.parse(c.tracksJson || '[]')) } catch { setTracks([]) }
      setTimelineDuration(c.durationMs || TIMELINE_DURATION)
    } else {
      setEditing({ id: 0 } as Cutscene)
      setDraft({ name: 'Nova Cutscene' })
      setTracks([])
      setTimelineDuration(TIMELINE_DURATION)
    }
  }

  function addTrack(type: Track['type']) {
    const newTrack: Track = {
      type, atMs: 0,
      payload: defaultPayload(type),
    }
    setTracks(t => [...t, newTrack])
    setEditingTrack(tracks.length)
  }

  function updateTrack(idx: number, patch: Partial<Track>) {
    setTracks(t => t.map((tr, i) => i === idx ? { ...tr, ...patch } : tr))
  }

  function removeTrack(idx: number) {
    setTracks(t => t.filter((_, i) => i !== idx))
    setEditingTrack(null)
  }

  function defaultPayload(type: Track['type']): any {
    switch (type) {
      case 'title': return { title: 'Bem-vindo', subtitle: '', fadeIn: 10, stay: 60, fadeOut: 10 }
      case 'sound': return { sound: 'minecraft:ambient.cave', volume: 1.0, pitch: 1.0 }
      case 'particle': return { particle: 'minecraft:flame', x: 0, y: 64, z: 0, count: 50 }
      case 'weather': return { weather: 'thunder', duration: 6000 }
      case 'time': return { time: 'night' }
      case 'broadcast': return { message: '§5§lOlá viajantes!' }
      case 'spawnEntity': return { entity: 'minecraft:zombie', x: 0, y: 64, z: 0, dimension: 'minecraft:overworld' }
      case 'spawnPlayerClone': return { playerName: 'Steve', x: 0, y: 64, z: 0, rotation: 0 }
      case 'teleport': return { playerUuid: '', x: 0, y: 64, z: 0, dimension: 'minecraft:overworld' }
      case 'lightning': return { playerUuid: '' }
      case 'voice': return { clipId: 1, x: 0, y: 64, z: 0, dimension: 'minecraft:overworld' }
      case 'explosion': return { x: 0, y: 64, z: 0, power: 4 }
      case 'command': return { command: '/say hello' }
      default: return {}
    }
  }

  const sortedTracks = useMemo(() => [...tracks].sort((a, b) => a.atMs - b.atMs), [tracks])
  const trackDefs = (type: Track['type']) => TRACK_TYPES.find(t => t.type === type) ?? TRACK_TYPES[0]

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">🎞 Cutscene Director</h1>
          <p className="text-sm text-zinc-400">Editor de timeline visual pra cinemáticas no servidor</p>
        </div>
        <button onClick={() => openEditor()} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded font-semibold">
          + Nova Cutscene
        </button>
      </div>

      {!editing && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((c) => (
            <div key={c.id} className="bg-gradient-to-br from-zinc-900 to-zinc-950 border border-zinc-800 rounded-lg overflow-hidden hover:border-purple-700 transition">
              <div className="bg-gradient-to-r from-purple-900 to-purple-700 h-2"></div>
              <div className="p-4 space-y-2">
                <div className="flex justify-between">
                  <h3 className="font-bold text-lg">{c.name}</h3>
                  <span className="text-xs bg-zinc-800 px-2 py-0.5 rounded">{c.plays}x</span>
                </div>
                <p className="text-xs text-zinc-500">{c.description || '(sem descrição)'}</p>
                <div className="text-xs text-zinc-600 font-mono">{((c.durationMs ?? 0) / 1000).toFixed(1)}s</div>
                <div className="flex gap-2 pt-2">
                  <button onClick={() => play.mutate(c.id)} className="flex-1 bg-emerald-700 hover:bg-emerald-600 py-1.5 rounded text-sm font-semibold">▶ Play</button>
                  <button onClick={() => setPreview(c)} className="bg-amber-700 hover:bg-amber-600 px-2 py-1.5 rounded text-sm" title="Preview animado">👁</button>
                  <button onClick={() => stop.mutate(c.id)} className="bg-red-900 hover:bg-red-700 px-3 py-1.5 rounded text-sm">⏹</button>
                  <button onClick={() => openEditor(c)} className="bg-zinc-800 hover:bg-zinc-700 px-3 py-1.5 rounded text-sm">✎</button>
                  <button onClick={() => confirm('Apagar?') && remove.mutate(c.id)} className="bg-red-950 hover:bg-red-900 px-3 py-1.5 rounded text-sm">🗑</button>
                </div>
              </div>
            </div>
          ))}
          {items.length === 0 && (
            <div className="col-span-full text-center py-16 text-zinc-500">
              <div className="text-6xl mb-3">🎬</div>
              <p>Nenhuma cutscene ainda. Crie a primeira!</p>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="bg-zinc-950 border border-zinc-800 rounded-lg p-4 space-y-4">
          <div className="flex justify-between items-start">
            <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xl font-bold flex-1 mr-2" placeholder="Nome da cutscene" />
            <div className="flex gap-2">
              <button onClick={() => save.mutate({ ...draft, id: editing.id || undefined })}
                className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">💾 Salvar</button>
              <button onClick={() => { setEditing(null); setTracks([]); setDraft({}) }}
                className="bg-zinc-700 hover:bg-zinc-600 px-4 py-2 rounded">✕</button>
            </div>
          </div>
          <textarea value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value })}
            rows={2} placeholder="Descrição..."
            className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />

          {/* Track palette */}
          <div className="space-y-2">
            <div className="text-sm font-semibold text-zinc-400">+ Adicionar track:</div>
            <div className="flex flex-wrap gap-2">
              {TRACK_TYPES.map((t) => (
                <button key={t.type} onClick={() => addTrack(t.type)}
                  className={`${t.color} hover:brightness-110 px-3 py-1.5 rounded text-xs font-semibold flex items-center gap-1`}>
                  <span>{t.emoji}</span><span>{t.label.split(' ')[0]}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Timeline */}
          <div className="space-y-2">
            <div className="flex justify-between items-center">
              <div className="text-sm font-semibold text-zinc-400">Timeline ({(timelineDuration / 1000).toFixed(0)}s)</div>
              <input type="range" min={5000} max={120000} step={5000}
                value={timelineDuration} onChange={(e) => setTimelineDuration(parseInt(e.target.value))}
                className="w-48" />
            </div>
            <div className="relative bg-zinc-900 rounded border border-zinc-800 overflow-x-auto">
              {/* Ruler */}
              <div className="flex border-b border-zinc-800 sticky top-0 bg-zinc-900 z-10" style={{ minWidth: timelineDuration * PX_PER_MS + 100 }}>
                {Array.from({ length: Math.ceil(timelineDuration / 1000) + 1 }).map((_, s) => (
                  <div key={s} className="flex-shrink-0 border-r border-zinc-800 text-xs text-zinc-500 px-1" style={{ width: 1000 * PX_PER_MS }}>
                    {s}s
                  </div>
                ))}
              </div>
              {/* Tracks lanes */}
              <div className="relative" style={{ minWidth: timelineDuration * PX_PER_MS + 100, minHeight: 60 + sortedTracks.length * 40 }}>
                {sortedTracks.map((tr, originalIdx) => {
                  const idx = tracks.findIndex(t => t === tr)
                  const td = trackDefs(tr.type)
                  return (
                    <div key={idx} onClick={() => setEditingTrack(idx)}
                      className={`absolute ${td.color} ${editingTrack === idx ? 'ring-2 ring-white' : ''} hover:brightness-110 cursor-pointer rounded px-2 py-1 text-xs flex items-center gap-1 shadow-lg`}
                      style={{ left: tr.atMs * PX_PER_MS, top: originalIdx * 40 + 8, minWidth: 80 }}>
                      <span>{td.emoji}</span>
                      <span className="font-semibold truncate">{tr.type}</span>
                      <button onClick={(e) => { e.stopPropagation(); removeTrack(idx) }} className="ml-1 opacity-70 hover:opacity-100">✕</button>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>

          {/* Preview button no editor */}
          <div className="flex gap-2">
            <button onClick={() => setPreview({ ...editing, tracksJson: JSON.stringify(tracks), durationMs: timelineDuration } as Cutscene)}
              className="bg-amber-700 hover:bg-amber-600 px-3 py-2 rounded text-sm">👁 Preview animado</button>
          </div>

          {/* Track editor */}
          {editingTrack !== null && tracks[editingTrack] && (
            <div className="bg-zinc-900 border border-zinc-700 rounded p-4 space-y-3">
              <div className="flex justify-between items-center">
                <h3 className="font-bold flex items-center gap-2">
                  {trackDefs(tracks[editingTrack].type).emoji} {trackDefs(tracks[editingTrack].type).label}
                </h3>
                <button onClick={() => setEditingTrack(null)} className="text-zinc-500 hover:text-zinc-300">✕</button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <label className="text-sm">
                  <div className="text-xs text-zinc-500 mb-1">Tempo (ms)</div>
                  <input type="number" min={0} max={timelineDuration} step={100}
                    value={tracks[editingTrack].atMs}
                    onChange={(e) => updateTrack(editingTrack, { atMs: parseInt(e.target.value) || 0 })}
                    className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5" />
                </label>
              </div>
              <div>
                <div className="text-xs text-zinc-500 mb-1">Payload (JSON editável)</div>
                <textarea value={JSON.stringify(tracks[editingTrack].payload, null, 2)}
                  onChange={(e) => {
                    try { updateTrack(editingTrack, { payload: JSON.parse(e.target.value) }) } catch {}
                  }}
                  rows={8}
                  className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
              </div>
            </div>
          )}
        </div>
      )}

      {preview && <PreviewPlayer cutscene={preview} onClose={() => setPreview(null)} trackDefs={trackDefs} />}
    </div>
  )
}

function PreviewPlayer({ cutscene, onClose, trackDefs }: {
  cutscene: Cutscene; onClose: () => void; trackDefs: (t: Track['type']) => any
}) {
  const tracks: Track[] = (() => { try { return JSON.parse(cutscene.tracksJson || '[]') } catch { return [] } })()
  const duration = Math.max(cutscene.durationMs, ...tracks.map(t => t.atMs)) || 10000
  const [elapsed, setElapsed] = useState(0)
  const [playing, setPlaying] = useState(true)

  useEffect(() => {
    if (!playing) return
    const start = Date.now() - elapsed
    const tick = setInterval(() => {
      const e = Date.now() - start
      if (e > duration + 1000) { setPlaying(false); return }
      setElapsed(e)
    }, 30)
    return () => clearInterval(tick)
  }, [playing, duration])

  const firedNow = tracks.filter(t => Math.abs(t.atMs - elapsed) < 200)

  return (
    <div onClick={onClose} className="fixed inset-0 bg-black/95 z-[100] flex items-center justify-center p-6">
      <div onClick={(e) => e.stopPropagation()} className="bg-zinc-950 rounded-lg max-w-5xl w-full p-6 space-y-4 border border-purple-700">
        <div className="flex justify-between">
          <h2 className="text-xl font-bold">👁 Preview: {cutscene.name}</h2>
          <button onClick={onClose} className="text-zinc-400 hover:text-white">✕</button>
        </div>

        {/* Stage — mock TV screen */}
        <div className="bg-black border-2 border-zinc-800 rounded-lg aspect-video relative overflow-hidden">
          {firedNow.map((t, i) => {
            const td = trackDefs(t.type)
            return (
              <div key={i} className="absolute inset-0 flex items-center justify-center animate-pulse">
                <div className={`${td.color} px-6 py-3 rounded-lg text-2xl font-bold shadow-2xl`}>
                  {td.emoji} {t.type}
                </div>
              </div>
            )
          })}
          {/* Mock title display */}
          {firedNow.filter(t => t.type === 'title').map((t, i) => (
            <div key={`title-${i}`} className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
              <div className="text-5xl font-bold mb-2 drop-shadow-lg">{t.payload?.title || ''}</div>
              <div className="text-2xl text-zinc-300">{t.payload?.subtitle || ''}</div>
            </div>
          ))}
          {/* Mock weather */}
          {firedNow.find(t => t.type === 'weather') && (
            <div className="absolute inset-0 bg-blue-950/40 animate-pulse" />
          )}
          {/* Time indicator */}
          <div className="absolute bottom-2 left-2 text-xs font-mono bg-black/60 text-white px-2 py-1 rounded">
            {(elapsed / 1000).toFixed(2)}s / {(duration / 1000).toFixed(2)}s
          </div>
        </div>

        {/* Progress bar */}
        <div className="space-y-1">
          <div className="h-2 bg-zinc-800 rounded overflow-hidden">
            <div className="h-full bg-gradient-to-r from-purple-500 to-fuchsia-500 transition-all"
              style={{ width: `${Math.min(100, (elapsed / duration) * 100)}%` }} />
          </div>
          <div className="flex gap-2 text-xs text-zinc-500 relative h-6">
            {tracks.map((t, i) => {
              const td = trackDefs(t.type)
              const left = (t.atMs / duration) * 100
              const fired = elapsed >= t.atMs
              return (
                <div key={i} className={`absolute top-0 ${td.color} ${fired ? 'opacity-100' : 'opacity-40'} px-1.5 py-0.5 rounded text-xs transition`}
                  style={{ left: `${Math.min(95, left)}%` }} title={`${t.type} @ ${t.atMs}ms`}>
                  {td.emoji}
                </div>
              )
            })}
          </div>
        </div>

        <div className="flex gap-2 justify-center">
          <button onClick={() => { setElapsed(0); setPlaying(true) }} className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded font-semibold">▶ Replay</button>
          <button onClick={() => setPlaying(!playing)} className="bg-zinc-700 hover:bg-zinc-600 px-4 py-2 rounded">
            {playing ? '⏸ Pause' : '▶ Resume'}
          </button>
          <button onClick={onClose} className="bg-zinc-800 hover:bg-zinc-700 px-4 py-2 rounded">Fechar</button>
        </div>

        <p className="text-xs text-zinc-500 text-center italic">
          Preview simula timing das tracks. Para ver no jogo de verdade, use ▶ Play.
        </p>
      </div>
    </div>
  )
}
