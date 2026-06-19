import { useEffect, useState } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { useEvents } from '../store/events'
import { api } from '../lib/api'

const auth = () => ({ Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` })
const jsonHeaders = () => ({ ...auth(), 'Content-Type': 'application/json; charset=utf-8' })

type SecurityEvent = {
  id: number; kind: string; intruderName: string; ownerName: string;
  dimension: string; x: number; y: number; z: number;
  blockTypeId: string; detail: string; occurredAt: string;
}

type Stat = { ownerUuid: string; ownerName: string; count: number; uniqueIntruders: number }

const kindColors: Record<string, string> = {
  password_fail: 'bg-yellow-900 text-yellow-300',
  block_break_protected: 'bg-red-900 text-red-300',
  chest_lock_break: 'bg-orange-900 text-orange-300',
  camera_motion: 'bg-blue-900 text-blue-300',
  unknown: 'bg-zinc-800 text-zinc-400',
}
const kindIcons: Record<string, string> = {
  password_fail: '🔒',
  block_break_protected: '⛏',
  chest_lock_break: '📦',
  camera_motion: '📹',
  unknown: '❓',
}

export function SecurityAuditPage() {
  const [kind, setKind] = useState('all')
  const subscribe = useEvents((s) => s.subscribe)
  const qc = useQueryClient()
  const [pulse, setPulse] = useState<number | null>(null)
  const [tpPicker, setTpPicker] = useState<number | null>(null)
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })

  const tpAction = useMutation({
    mutationFn: async ({ id, playerUuid }: { id: number; playerUuid: string }) =>
      fetch(`/api/security/${id}/tp`, { method: 'POST', headers: jsonHeaders(), body: JSON.stringify({ playerUuid }) }),
    onSuccess: () => alert('🌀 TP!'),
  })
  const notifyAction = useMutation({
    mutationFn: async (id: number) =>
      fetch(`/api/security/${id}/notify-owner`, { method: 'POST', headers: auth() }),
    onSuccess: () => alert('🔔 Dono notificado in-game'),
  })
  const kickAction = useMutation({
    mutationFn: async (id: number) =>
      fetch(`/api/security/${id}/kick`, { method: 'POST', headers: auth() }),
    onSuccess: () => alert('👢 Intruso kickado'),
  })

  // Real-time: invalida queries quando chega security_event via WS
  useEffect(() => subscribe((ev) => {
    if (ev.type !== 'security_event') return
    setPulse(ev.data?.id ?? null)
    qc.invalidateQueries({ queryKey: ['sec-feed'] })
    qc.invalidateQueries({ queryKey: ['sec-stats'] })
    setTimeout(() => setPulse(null), 1500)
  }), [subscribe])

  const feed = useQuery({
    queryKey: ['sec-feed', kind],
    queryFn: async () => {
      const url = kind === 'all' ? '/api/security?size=100' : `/api/security?kind=${kind}&size=100`
      const r = await fetch(url, { headers: auth() })
      return r.json()
    },
    refetchInterval: 15_000,
  })

  const stats = useQuery({
    queryKey: ['sec-stats'],
    queryFn: async () => (await fetch('/api/security/stats/last-24h', { headers: auth() })).json(),
    refetchInterval: 30_000,
  })

  const events: SecurityEvent[] = Array.isArray(feed.data?.content) ? feed.data.content : []
  const top: Stat[] = Array.isArray(stats.data) ? stats.data : []

  return (
    <div className="p-6 space-y-6 text-white">
      <h1 className="text-3xl font-bold">🛡 Security Audit</h1>

      <section>
        <h2 className="text-xl font-semibold mb-3">🔥 Mais atacados nas últimas 24h</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
          {top.slice(0, 6).map((s) => (
            <div key={s.ownerUuid} className="bg-red-950/30 border border-red-800 rounded p-3">
              <div className="font-bold text-red-400">{s.ownerName || '?'}</div>
              <div className="text-2xl font-mono text-red-300">{s.count}</div>
              <div className="text-xs text-zinc-500">{s.uniqueIntruders} invasores únicos</div>
            </div>
          ))}
          {top.length === 0 && <div className="col-span-full text-zinc-500 italic text-sm">Sem eventos nas últimas 24h.</div>}
        </div>
      </section>

      <section>
        <div className="flex justify-between items-center mb-3">
          <h2 className="text-xl font-semibold">📜 Activity Wall</h2>
          <div className="flex gap-2 text-sm">
            {['all', 'password_fail', 'block_break_protected', 'chest_lock_break', 'camera_motion'].map((k) => (
              <button key={k} onClick={() => setKind(k)} className={`px-2 py-1 rounded text-xs ${kind === k ? 'bg-purple-700' : 'bg-zinc-800'}`}>{k}</button>
            ))}
          </div>
        </div>
        <div className="space-y-1">
          {events.map((e) => (
            <div key={e.id} className={`bg-zinc-900 border rounded p-3 flex items-center gap-3 text-sm transition ${pulse === e.id ? 'border-red-400 ring-2 ring-red-500/40 scale-[1.01]' : 'border-zinc-800'}`}>
              <div className="text-2xl">{kindIcons[e.kind] || '❓'}</div>
              <div className="flex-1">
                <div>
                  <span className="text-red-400 font-semibold">{e.intruderName}</span>
                  {' → '}
                  <span className="text-purple-400 font-semibold">{e.ownerName}</span>
                  {' · '}
                  <span className={`text-xs px-2 py-0.5 rounded ${kindColors[e.kind] || kindColors.unknown}`}>{e.kind}</span>
                </div>
                <div className="text-xs text-zinc-500 font-mono">
                  {e.dimension} X:{e.x} Y:{e.y} Z:{e.z}
                  {e.blockTypeId && <span> · {e.blockTypeId}</span>}
                  {e.detail && <span> · {e.detail}</span>}
                </div>
              </div>
              <div className="text-xs text-zinc-600">{new Date(e.occurredAt).toLocaleString()}</div>
              <div className="flex gap-1">
                <button onClick={() => setTpPicker(e.id)} className="bg-emerald-900 hover:bg-emerald-700 px-2 py-0.5 rounded text-xs" title="TP até o local">🌀</button>
                {e.ownerName && <button onClick={() => notifyAction.mutate(e.id)} className="bg-amber-900 hover:bg-amber-700 px-2 py-0.5 rounded text-xs" title="Notificar dono">🔔</button>}
                {e.intruderName && <button onClick={() => kickAction.mutate(e.id)} className="bg-red-900 hover:bg-red-700 px-2 py-0.5 rounded text-xs" title="Kickar intruso">👢</button>}
              </div>
            </div>
          ))}
          {events.length === 0 && <div className="text-zinc-500 text-center py-8">Sem eventos.</div>}
        </div>
      </section>

      {tpPicker && (
        <div onClick={() => setTpPicker(null)} className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-md w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">🌀 Quem investiga?</h2>
            <select onChange={(e) => { if (e.target.value) { tpAction.mutate({ id: tpPicker, playerUuid: e.target.value }); setTpPicker(null) } }}
              className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2">
              <option value="">Selecione player...</option>
              {(playersQ.data ?? []).map(p => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
            <button onClick={() => setTpPicker(null)} className="w-full bg-zinc-700 px-4 py-2 rounded">Cancelar</button>
          </div>
        </div>
      )}
    </div>
  )
}
