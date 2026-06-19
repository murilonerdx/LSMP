import { useQuery, useMutation } from '@tanstack/react-query'

const auth = () => ({ Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` })
const jsonHeaders = () => ({ ...auth(), 'Content-Type': 'application/json; charset=utf-8' })

async function get(path: string) {
  const r = await fetch(path, { headers: auth() })
  if (!r.ok) throw new Error(`${r.status}`)
  return r.json()
}

type Stat = { levelId: string; enters: number; exits: number; currently: number }
type Lost = { playerName: string; playerUuid: string; levelId: string; enteredAt: string; hoursLost: number }
type Entry = { id: number; playerName: string; levelId: string; eventType: string; occurredAt: string }

/** Rastreador de jogadores perdidos nas Backrooms. */
export function BackroomsTrackerPage() {
  const stats = useQuery({ queryKey: ['back-stats'], queryFn: () => get('/api/backrooms/stats'), refetchInterval: 15_000 })
  const lost = useQuery({ queryKey: ['back-lost'], queryFn: () => get('/api/backrooms/lost'), refetchInterval: 15_000 })
  const feed = useQuery({ queryKey: ['back-feed'], queryFn: () => get('/api/backrooms?size=50'), refetchInterval: 10_000 })

  const rescue = useMutation({
    mutationFn: async (playerUuid: string) => fetch('/api/backrooms/rescue', { method: 'POST', headers: jsonHeaders(), body: JSON.stringify({ playerUuid }) }),
    onSuccess: () => alert('🚪 Resgatado! Player teleportado pro spawn'),
  })

  const lostList: Lost[] = Array.isArray(lost.data) ? lost.data : []
  const statsList: Stat[] = Array.isArray(stats.data) ? stats.data : []
  const feedList: Entry[] = Array.isArray(feed.data?.content) ? feed.data.content : []

  return (
    <div className="p-6 space-y-6 text-white">
      <h1 className="text-3xl font-bold">🕳 Backrooms Tracker</h1>

      <section>
        <h2 className="text-xl font-semibold mb-2 text-amber-400">⚠ Perdidos agora</h2>
        {lostList.length === 0 ? (
          <p className="text-zinc-500 italic">Nenhum jogador atualmente perdido.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {lostList.map((l) => (
              <div key={l.playerUuid} className="bg-amber-950/30 border border-amber-800 rounded p-3 flex items-center gap-3">
                <div className="text-2xl">😱</div>
                <div className="flex-1">
                  <div className="font-semibold text-amber-300">{l.playerName}</div>
                  <div className="text-xs text-zinc-400">No {l.levelId} há <span className="text-amber-400 font-bold">{l.hoursLost.toFixed(1)}h</span></div>
                </div>
                <button onClick={() => confirm(`Resgatar ${l.playerName}?`) && rescue.mutate(l.playerUuid)}
                  className="bg-emerald-700 hover:bg-emerald-600 px-3 py-1.5 rounded text-sm font-semibold">🚪 Resgatar</button>
              </div>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-2">📊 Stats por level</h2>
        <table className="w-full text-sm bg-zinc-900 border border-zinc-700 rounded overflow-hidden">
          <thead className="bg-zinc-800">
            <tr><th className="text-left p-2">Level</th><th>Entradas</th><th>Saídas</th><th>Dentro agora</th></tr>
          </thead>
          <tbody>
            {statsList.map((s) => (
              <tr key={s.levelId} className="border-t border-zinc-800">
                <td className="p-2 font-mono text-xs">{s.levelId}</td>
                <td className="text-center text-green-400">{s.enters}</td>
                <td className="text-center text-red-400">{s.exits}</td>
                <td className="text-center font-bold">{s.currently}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-2">📜 Feed recente</h2>
        <div className="space-y-1 text-sm font-mono">
          {feedList.map((e) => (
            <div key={e.id} className="text-zinc-400">
              <span className="text-zinc-600">{new Date(e.occurredAt).toLocaleTimeString()}</span>{' '}
              <span className={e.eventType === 'enter' ? 'text-amber-400' : 'text-green-400'}>
                {e.eventType === 'enter' ? '→' : '←'}
              </span>{' '}
              <span className="text-white">{e.playerName}</span> em <span className="text-purple-400">{e.levelId}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
