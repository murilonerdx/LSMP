import { useState, useEffect, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type ReplayEvent = { atMs: number; type: string; payload: any }
type Replay = {
  id: number; name: string; description: string;
  eventsJson: string; durationMs: number; eventCount: number;
  plays: number; recordedAt: string; updatedAt: string;
}

export function ReplayStudioPage() {
  const [editing, setEditing] = useState<Replay | null>(null)
  const [draft, setDraft] = useState<Partial<Replay>>({})
  const [events, setEvents] = useState<ReplayEvent[]>([])
  const [recording, setRecording] = useState(false)
  const recordStartRef = useRef<number>(0)
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['replays'],
    queryFn: async () => (await fetch('/api/replays', { headers: headers() })).json(),
  })

  const save = useMutation({
    mutationFn: async (body: any) => {
      const url = body.id ? `/api/replays/${body.id}` : '/api/replays'
      const dur = events.length ? Math.max(...events.map(e => e.atMs)) : 0
      return (await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(),
        body: JSON.stringify({ ...body, eventsJson: JSON.stringify(events), eventCount: events.length, durationMs: dur }) })).json()
    },
    onSuccess: () => { setEditing(null); setEvents([]); setDraft({}); qc.invalidateQueries({ queryKey: ['replays'] }) },
  })

  const play = useMutation({
    mutationFn: async (id: number) => (await fetch(`/api/replays/${id}/play`, { method: 'POST', headers: headers() })).json(),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/replays/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['replays'] }),
  })

  // Auto-record server-side via WS/SSE
  const recordStatus = useQuery({
    queryKey: ['record-status'],
    queryFn: async () => (await fetch('/api/replays/record/status', { headers: headers() })).json(),
    refetchInterval: 2000,
  })
  const recordStart = useMutation({
    mutationFn: async (name: string) =>
      (await fetch('/api/replays/record/start', { method: 'POST', headers: headers(), body: JSON.stringify({ name }) })).json(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['record-status'] }),
  })
  const recordStop = useMutation({
    mutationFn: async (description: string) =>
      (await fetch('/api/replays/record/stop', { method: 'POST', headers: headers(), body: JSON.stringify({ description }) })).json(),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['record-status'] }); qc.invalidateQueries({ queryKey: ['replays'] }) },
  })

  const items: Replay[] = Array.isArray(q.data?.content) ? q.data.content : []
  const isAutoRecording = recordStatus.data?.recording === true

  function openEditor(r?: Replay) {
    if (r) {
      setEditing(r); setDraft({ ...r })
      try { setEvents(JSON.parse(r.eventsJson || '[]')) } catch { setEvents([]) }
    } else {
      setEditing({ id: 0 } as Replay)
      setDraft({ name: 'Novo Replay' })
      setEvents([])
    }
  }

  function startRecording() {
    recordStartRef.current = Date.now()
    setRecording(true)
  }

  function stopRecording() { setRecording(false) }

  function addEvent(type: string, payload: any) {
    const atMs = recording ? Date.now() - recordStartRef.current : (events.length ? Math.max(...events.map(e => e.atMs)) + 1000 : 0)
    setEvents(e => [...e, { atMs, type, payload }])
  }

  function updateEvent(idx: number, patch: Partial<ReplayEvent>) {
    setEvents(e => e.map((ev, i) => i === idx ? { ...ev, ...patch } : ev))
  }

  function removeEvent(idx: number) {
    setEvents(e => e.filter((_, i) => i !== idx))
  }

  const sortedEvents = [...events].sort((a, b) => a.atMs - b.atMs)

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div className="flex justify-between">
        <div>
          <h1 className="text-3xl font-bold">🎬 Replay Studio</h1>
          <p className="text-sm text-zinc-400">Grava sequências de eventos pra reproduzir como cinematics</p>
        </div>
        <button onClick={() => openEditor()} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded font-semibold">+ Novo Replay manual</button>
      </div>

      {/* Auto-record server-side */}
      <div className={`rounded-lg p-4 border ${isAutoRecording ? 'bg-red-950/40 border-red-700 animate-pulse' : 'bg-zinc-900 border-zinc-800'}`}>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-3">
            <div className={`w-3 h-3 rounded-full ${isAutoRecording ? 'bg-red-500 animate-pulse' : 'bg-zinc-700'}`}></div>
            <div>
              <h3 className="font-bold flex items-center gap-2">
                🔴 Auto-Record <span className="text-xs text-zinc-500 font-normal">— captura TODOS os eventos do mod via WS</span>
              </h3>
              {isAutoRecording ? (
                <p className="text-sm text-red-300 font-mono mt-1">
                  Gravando "{recordStatus.data?.name}" · {((recordStatus.data?.elapsedMs ?? 0) / 1000).toFixed(1)}s · {recordStatus.data?.events} eventos capturados
                </p>
              ) : (
                <p className="text-sm text-zinc-500 mt-1">Pronto pra gravar — clique no botão pra começar.</p>
              )}
            </div>
          </div>
          <div className="flex gap-2">
            {!isAutoRecording ? (
              <button onClick={() => {
                const name = prompt('Nome da gravação:', 'Gravação ' + new Date().toLocaleString())
                if (name) recordStart.mutate(name)
              }} className="bg-red-700 hover:bg-red-600 px-4 py-2 rounded font-semibold flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-white"></span> Iniciar
              </button>
            ) : (
              <button onClick={() => {
                const desc = prompt('Descrição (opcional):', '') ?? ''
                recordStop.mutate(desc)
              }} className="bg-zinc-700 hover:bg-zinc-600 px-4 py-2 rounded font-semibold">⏹ Parar & Salvar</button>
            )}
          </div>
        </div>
      </div>

      {!editing && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {items.map((r) => (
            <div key={r.id} className="bg-gradient-to-br from-zinc-900 to-zinc-950 border border-zinc-800 rounded-lg overflow-hidden hover:border-purple-700">
              <div className="bg-gradient-to-r from-fuchsia-700 to-purple-700 h-1"></div>
              <div className="p-4">
                <div className="flex justify-between">
                  <h3 className="font-bold">{r.name}</h3>
                  <span className="text-xs text-zinc-500">{r.plays}x</span>
                </div>
                <p className="text-xs text-zinc-500 mt-1">{r.description}</p>
                <div className="grid grid-cols-2 gap-2 mt-3 text-xs">
                  <div className="bg-zinc-950 rounded p-2 text-center">📜 <b>{r.eventCount}</b> eventos</div>
                  <div className="bg-zinc-950 rounded p-2 text-center">⏱ <b>{(r.durationMs/1000).toFixed(1)}s</b></div>
                </div>
                <div className="flex gap-1 mt-3">
                  <button onClick={() => play.mutate(r.id)} className="flex-1 bg-emerald-700 hover:bg-emerald-600 py-1.5 rounded text-sm font-semibold">▶ Play</button>
                  <button onClick={() => openEditor(r)} className="bg-zinc-800 hover:bg-zinc-700 px-2 py-1.5 rounded text-sm">✎</button>
                  <button onClick={() => confirm('Apagar?') && remove.mutate(r.id)} className="bg-red-900 hover:bg-red-700 px-2 py-1.5 rounded text-sm">🗑</button>
                </div>
              </div>
            </div>
          ))}
          {items.length === 0 && (
            <div className="col-span-full text-center py-16 text-zinc-500">
              <div className="text-6xl mb-3">🎬</div>
              <p>Nenhum replay gravado.</p>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="bg-zinc-950 border border-zinc-800 rounded-lg p-4 space-y-4">
          <div className="flex justify-between">
            <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xl font-bold flex-1 mr-2" placeholder="Nome do replay" />
            <div className="flex gap-2">
              {!recording ? (
                <button onClick={startRecording} className="bg-red-700 hover:bg-red-600 px-4 py-2 rounded font-semibold flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-white animate-pulse"></span> Gravar
                </button>
              ) : (
                <button onClick={stopRecording} className="bg-zinc-700 hover:bg-zinc-600 px-4 py-2 rounded">⏹ Parar</button>
              )}
              <button onClick={() => save.mutate({ ...draft, id: editing.id || undefined })} className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">💾</button>
              <button onClick={() => { setEditing(null); setEvents([]); setDraft({}) }} className="bg-zinc-700 px-4 py-2 rounded">✕</button>
            </div>
          </div>
          <textarea value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value })}
            placeholder="Descrição" rows={2}
            className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />

          {recording && (
            <div className="bg-red-950 border border-red-700 rounded p-3 flex items-center gap-2 animate-pulse">
              <span className="w-3 h-3 rounded-full bg-red-500"></span>
              <span className="font-semibold">GRAVANDO</span>
              <span className="ml-auto text-xs text-zinc-400">Os eventos abaixo serão timestampados a partir do momento que você clicou Gravar.</span>
            </div>
          )}

          <div className="space-y-2">
            <h3 className="font-bold flex justify-between items-center">
              Eventos ({events.length})
              <div className="flex gap-1 text-xs">
                <button onClick={() => addEvent('command', { command: '/say hello' })} className="bg-zinc-700 hover:bg-zinc-600 px-2 py-1 rounded">+ Command</button>
                <button onClick={() => addEvent('broadcast', { message: '§5§lÉvento!' })} className="bg-rose-700 hover:bg-rose-600 px-2 py-1 rounded">+ Broadcast</button>
                <button onClick={() => addEvent('spawn', { entity: 'minecraft:zombie', x: 0, y: 64, z: 0, dimension: 'minecraft:overworld' })} className="bg-red-700 hover:bg-red-600 px-2 py-1 rounded">+ Spawn</button>
                <button onClick={() => addEvent('sound', { playerUuid: '', sound: 'minecraft:ambient.cave' })} className="bg-amber-700 hover:bg-amber-600 px-2 py-1 rounded">+ Sound</button>
                <button onClick={() => addEvent('particle', { particle: 'minecraft:flame', x: 0, y: 64, z: 0 })} className="bg-fuchsia-700 hover:bg-fuchsia-600 px-2 py-1 rounded">+ Particle</button>
              </div>
            </h3>

            <div className="space-y-1 max-h-[500px] overflow-y-auto">
              {sortedEvents.map((ev, i) => {
                const originalIdx = events.findIndex(e => e === ev)
                return (
                  <div key={originalIdx} className="bg-zinc-900 border border-zinc-800 rounded p-2 grid grid-cols-[80px_100px_1fr_30px] gap-2 items-center text-xs">
                    <input type="number" value={ev.atMs} onChange={(e) => updateEvent(originalIdx, { atMs: parseInt(e.target.value) || 0 })}
                      className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 font-mono" />
                    <span className="bg-purple-900 px-2 py-1 rounded font-semibold text-center">{ev.type}</span>
                    <textarea value={JSON.stringify(ev.payload)}
                      onChange={(e) => { try { updateEvent(originalIdx, { payload: JSON.parse(e.target.value) }) } catch {} }}
                      rows={1}
                      className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 font-mono text-xs resize-none" />
                    <button onClick={() => removeEvent(originalIdx)} className="text-red-400">✕</button>
                  </div>
                )
              })}
              {events.length === 0 && <p className="text-zinc-500 text-sm italic text-center py-4">Sem eventos. Clique nos botões acima.</p>}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
