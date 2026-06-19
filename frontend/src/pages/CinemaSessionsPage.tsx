import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Session = {
  id: number; title: string; description: string; mediaUrl: string;
  theaterLocation: string; startsAt: string; status: string; createdBy: string;
  rsvpsJson: string;
}

export function CinemaSessionsPage() {
  const [showNew, setShowNew] = useState(false)
  const [draft, setDraft] = useState<Partial<Session>>({})
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['cinema'],
    queryFn: async () => (await fetch('/api/cinema', { headers: headers() })).json(),
    refetchInterval: 30_000,
  })

  const save = useMutation({
    mutationFn: async (body: Partial<Session>) => {
      const r = await fetch('/api/cinema', { method: 'POST', headers: headers(), body: JSON.stringify(body) })
      return r.json()
    },
    onSuccess: () => { setShowNew(false); setDraft({}); qc.invalidateQueries({ queryKey: ['cinema'] }) },
  })

  const start = useMutation({
    mutationFn: async (id: number) => fetch(`/api/cinema/${id}/start`, { method: 'POST', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cinema'] }),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/cinema/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cinema'] }),
  })

  const items: Session[] = Array.isArray(q.data?.content) ? q.data.content : []
  const upcoming = items.filter(i => i.status === 'scheduled').sort((a, b) => a.startsAt.localeCompare(b.startsAt))
  const past = items.filter(i => i.status !== 'scheduled')

  return (
    <div className="p-6 space-y-6 text-white">
      <div className="flex justify-between">
        <h1 className="text-3xl font-bold">🎥 Cinema Coletivo</h1>
        <button onClick={() => { setShowNew(true); setDraft({ status: 'scheduled', createdBy: 'admin' }) }}
          className="bg-emerald-700 hover:bg-emerald-600 px-3 py-1 rounded">+ Agendar sessão</button>
      </div>

      <section>
        <h2 className="text-xl font-semibold mb-3">📅 Próximas sessões</h2>
        <div className="space-y-2">
          {upcoming.map((s) => {
            const rsvps = JSON.parse(s.rsvpsJson || '[]')
            return (
              <div key={s.id} className="bg-zinc-900 border border-emerald-800 rounded p-4">
                <div className="flex justify-between">
                  <div className="flex-1">
                    <h3 className="font-bold text-emerald-400">{s.title}</h3>
                    <p className="text-xs text-zinc-500">{new Date(s.startsAt).toLocaleString()} · {rsvps.length} RSVPs</p>
                    <p className="text-sm text-zinc-400 mt-1">{s.description}</p>
                    <a href={s.mediaUrl} target="_blank" className="text-xs text-blue-400 hover:underline">{s.mediaUrl}</a>
                  </div>
                  <div className="flex flex-col gap-1">
                    <button onClick={() => start.mutate(s.id)} className="bg-emerald-800 hover:bg-emerald-700 px-3 py-1 rounded text-xs">▶ Iniciar agora</button>
                    <button onClick={() => remove.mutate(s.id)} className="bg-red-900 hover:bg-red-700 px-3 py-1 rounded text-xs">🗑</button>
                  </div>
                </div>
              </div>
            )
          })}
          {upcoming.length === 0 && <div className="text-zinc-500 italic text-sm">Nenhuma sessão agendada.</div>}
        </div>
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-3">📜 Histórico</h2>
        <div className="space-y-1 text-sm">
          {past.slice(0, 20).map((s) => (
            <div key={s.id} className="bg-zinc-900/50 px-3 py-2 rounded flex justify-between">
              <span className="text-zinc-400">{s.title}</span>
              <span className="text-xs text-zinc-600">{new Date(s.startsAt).toLocaleString()} · {s.status}</span>
            </div>
          ))}
        </div>
      </section>

      {showNew && (
        <div onClick={() => setShowNew(false)} className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-2xl w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">Agendar sessão</h2>
            <input value={draft.title ?? ''} onChange={(e) => setDraft({ ...draft, title: e.target.value })}
              placeholder="Título do filme" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2" />
            <textarea value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              placeholder="Descrição" rows={2} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input value={draft.mediaUrl ?? ''} onChange={(e) => setDraft({ ...draft, mediaUrl: e.target.value })}
              placeholder="URL do vídeo (YouTube/MP4)" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input value={draft.theaterLocation ?? ''} onChange={(e) => setDraft({ ...draft, theaterLocation: e.target.value })}
              placeholder="Local do cinema (overworld:100,64,200)" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input type="datetime-local"
              onChange={(e) => setDraft({ ...draft, startsAt: new Date(e.target.value).toISOString() })}
              className="bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <div className="flex gap-2">
              <button onClick={() => save.mutate(draft)} className="ml-auto bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">Agendar</button>
              <button onClick={() => setShowNew(false)} className="bg-zinc-700 px-4 py-2 rounded">Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
