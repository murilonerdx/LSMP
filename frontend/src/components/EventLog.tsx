import { useEffect } from 'react'
import { useEvents } from '../store/events'

export function EventLog() {
  const { events, connected, connect, clear } = useEvents()

  useEffect(() => { connect() }, [])

  return (
    <div className="card">
      <div className="flex justify-between items-center mb-2">
        <h3 className="font-bold">📡 Eventos Live</h3>
        <div className="flex items-center gap-2">
          <span className={`w-2 h-2 rounded-full ${connected ? 'bg-green-400' : 'bg-red-400'}`} />
          <span className="text-xs">{connected ? 'conectado' : 'reconectando'}</span>
          <button onClick={clear} className="btn-ghost ml-2 text-xs">Clear</button>
        </div>
      </div>
      <div className="space-y-1 max-h-72 overflow-y-auto text-sm">
        {events.length === 0 ? (
          <p className="text-liberthia-300/40 italic">Sem eventos ainda...</p>
        ) : events.map(ev => (
          <EventRow key={ev.ts + ev.type} ev={ev} />
        ))}
      </div>
    </div>
  )
}

function EventRow({ ev }: { ev: any }) {
  const time = new Date(ev.ts).toLocaleTimeString()
  const icons: Record<string, string> = {
    player_login: '🟢',
    player_logout: '🔴',
    player_death: '💀',
    server_started: '🚀',
    server_stopping: '🛑',
    hello: '👋',
    inventory_changed: '🎒',
  }
  const labels: Record<string, string> = {
    player_login: 'entrou',
    player_logout: 'saiu',
    player_death: 'morreu',
    server_started: 'server iniciado',
    server_stopping: 'server parando',
  }
  const name = ev.data?.name ?? ev.data?.uuid?.substring(0, 8) ?? '?'
  const label = labels[ev.type] ?? ev.type
  return (
    <div className="px-2 py-1 rounded hover:bg-liberthia-700 flex gap-2 items-baseline">
      <span>{icons[ev.type] ?? '·'}</span>
      <span className="text-xs text-liberthia-300/60 font-mono">{time}</span>
      <span className="font-semibold">{name}</span>
      <span className="opacity-60">{label}</span>
      {ev.type === 'player_death' && ev.data?.source && <span className="text-red-400 text-xs">({ev.data.source})</span>}
    </div>
  )
}
