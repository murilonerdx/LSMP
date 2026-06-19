import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import { useEvents } from '../store/events'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Automation — regras "quando X acontece, faz Y".
 *
 * Events suportados (vindos do mod via SSE/WS): player_login, player_logout,
 * player_death, server_started, server_stopping.
 *
 * Actions: command, title, sound, lightning, heal — disparam via API.
 *
 * Regras vivem em localStorage. Engine roda enquanto a aba estiver aberta.
 */

type EventKind = 'player_login' | 'player_logout' | 'player_death' | 'server_started' | 'server_stopping'

type Action =
  | { type: 'command'; command: string }
  | { type: 'title'; title: string; subtitle?: string }
  | { type: 'sound'; sound: string }
  | { type: 'lightning' }
  | { type: 'heal' }

type Rule = {
  id: number
  enabled: boolean
  name: string
  on: EventKind
  matchPlayer?: string // optional player name filter
  action: Action
  delaySec?: number
}

const EVENT_LABELS: Record<EventKind, string> = {
  player_login: '🟢 Player entra',
  player_logout: '🔴 Player sai',
  player_death: '☠ Player morre',
  server_started: '⚡ Server iniciou',
  server_stopping: '🛑 Server desligando',
}

export function AutomationPage() {
  const subscribe = useEvents((s) => s.subscribe)
  const [rules, setRules] = useKvState<Rule[]>('automation_rules', [])
  const [fired, setFired] = useState<{ ts: number; ruleName: string; player?: string }[]>([])
  const [editing, setEditing] = useState<Rule | null>(null)

  // Subscribe to live events and trigger matching rules
  useEffect(() => {
    return subscribe(async (ev) => {
      const matchType = (ev.type || '').toLowerCase()
      const playerName = ev.data?.name ?? ev.data?.playerName ?? ''
      for (const r of rules) {
        if (!r.enabled) continue
        if (r.on !== matchType) continue
        if (r.matchPlayer && r.matchPlayer.toLowerCase() !== playerName.toLowerCase()) continue
        setFired((f) => [{ ts: Date.now(), ruleName: r.name, player: playerName }, ...f].slice(0, 50))
        const fire = async () => {
          try {
            const players = await api.players()
            const target = players.find((p) => p.name.toLowerCase() === playerName.toLowerCase())
            switch (r.action.type) {
              case 'command':
                await api.command(r.action.command.split('{player}').join(playerName || ''))
                break
              case 'title':
                if (target) await api.title(target.uuid, r.action.title, r.action.subtitle ?? '')
                break
              case 'sound':
                if (target) await api.sound(target.uuid, r.action.sound)
                break
              case 'lightning':
                if (target) await api.lightning(target.uuid)
                break
              case 'heal':
                if (target) await api.heal(target.uuid)
                break
            }
          } catch (e: any) { toast.err(`Rule ${r.name}: ${e.message}`) }
        }
        if (r.delaySec && r.delaySec > 0) setTimeout(fire, r.delaySec * 1000)
        else fire()
      }
    })
  }, [rules, subscribe])

  function addNew() {
    const r: Rule = {
      id: Date.now(),
      enabled: true,
      name: 'Nova regra',
      on: 'player_login',
      action: { type: 'title', title: '§aBem-vindo §d{player}!', subtitle: '§7Liberthia' },
      delaySec: 1,
    }
    setEditing(r)
  }

  function commit(r: Rule) {
    setRules((rs) => {
      const idx = rs.findIndex((x) => x.id === r.id)
      if (idx >= 0) { const n = [...rs]; n[idx] = r; return n }
      return [...rs, r]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="page-title">⚙ Automation</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Quando algo acontece no servidor, executa uma ação. Regras rodam enquanto a aba estiver aberta.
          </p>
        </div>
        <button className="btn" onClick={addNew}>+ Nova Regra</button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        <div className="space-y-3">
          {rules.length === 0 && (
            <div className="card text-center py-12">
              <div className="text-5xl mb-3 opacity-50">⚙</div>
              <p className="text-liberthia-300/70">Nenhuma regra criada ainda. Crie a primeira pra automatizar reações.</p>
            </div>
          )}
          {rules.map((r) => (
            <div key={r.id} className="card-glow flex items-center gap-4">
              <button
                className={`w-10 h-6 rounded-full relative transition ${r.enabled ? 'bg-emerald-500/60' : 'bg-liberthia-700'}`}
                onClick={() => setRules(rs => rs.map(x => x.id === r.id ? { ...x, enabled: !x.enabled } : x))}
              >
                <span className={`absolute top-1 ${r.enabled ? 'right-1' : 'left-1'} w-4 h-4 rounded-full bg-white transition-all`} />
              </button>
              <div className="flex-1 min-w-0">
                <div className="font-bold">{r.name}</div>
                <div className="text-xs text-liberthia-300/70 mt-0.5 flex flex-wrap items-center gap-2">
                  <span className="chip">{EVENT_LABELS[r.on]}</span>
                  {r.matchPlayer && <span className="chip">player = {r.matchPlayer}</span>}
                  <span className="text-liberthia-300/40">→</span>
                  <span className="chip">{r.action.type}</span>
                  {r.delaySec ? <span className="chip">delay {r.delaySec}s</span> : null}
                </div>
              </div>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(r)}>edit</button>
              <button className="btn-ghost btn-sm" onClick={() => setRules(rs => rs.filter(x => x.id !== r.id))}>🗑</button>
            </div>
          ))}
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">📝 Disparos recentes</h3>
          <div className="font-mono text-xs space-y-0.5 max-h-96 overflow-y-auto">
            {fired.length === 0 && <div className="text-liberthia-300/50 italic">— ainda nada —</div>}
            {fired.map((f, i) => (
              <div key={i} className="text-emerald-300">
                {new Date(f.ts).toLocaleTimeString()} · {f.ruleName}
                {f.player && <span className="text-liberthia-300/60"> · {f.player}</span>}
              </div>
            ))}
          </div>
        </div>
      </div>

      {editing && (
        <RuleEditor
          rule={editing}
          onSave={commit}
          onCancel={() => setEditing(null)}
        />
      )}
    </div>
  )
}

function RuleEditor({ rule, onSave, onCancel }: { rule: Rule; onSave: (r: Rule) => void; onCancel: () => void }) {
  const [r, setR] = useState<Rule>(rule)
  const a = r.action
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-lg w-full" onClick={e => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">Regra</h3>
        <label className="label block mb-1">Nome</label>
        <input className="input mb-3" value={r.name} onChange={e => setR({ ...r, name: e.target.value })} />

        <label className="label block mb-1">Quando (evento)</label>
        <select className="input mb-3" value={r.on} onChange={e => setR({ ...r, on: e.target.value as EventKind })}>
          {Object.entries(EVENT_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>

        <label className="label block mb-1">Filtrar por player (opcional)</label>
        <input className="input mb-3" placeholder="ex: Steve (vazio = qualquer)" value={r.matchPlayer ?? ''}
          onChange={e => setR({ ...r, matchPlayer: e.target.value })} />

        <label className="label block mb-1">Delay (segundos)</label>
        <input type="number" className="input mb-3" value={r.delaySec ?? 0} min={0}
          onChange={e => setR({ ...r, delaySec: Number(e.target.value) })} />

        <label className="label block mb-1">Ação</label>
        <select className="input mb-3" value={a.type} onChange={e => {
          const t = e.target.value as Action['type']
          if (t === 'command') setR({ ...r, action: { type: 'command', command: 'say {player} chegou!' } })
          if (t === 'title') setR({ ...r, action: { type: 'title', title: '§aOlá {player}', subtitle: '' } })
          if (t === 'sound') setR({ ...r, action: { type: 'sound', sound: 'minecraft:entity.player.levelup' } })
          if (t === 'lightning') setR({ ...r, action: { type: 'lightning' } })
          if (t === 'heal') setR({ ...r, action: { type: 'heal' } })
        }}>
          <option value="command">command</option>
          <option value="title">title</option>
          <option value="sound">sound</option>
          <option value="lightning">lightning</option>
          <option value="heal">heal</option>
        </select>

        {a.type === 'command' && (
          <input className="input mb-3 font-mono text-xs" value={a.command}
            onChange={e => setR({ ...r, action: { type: 'command', command: e.target.value } })}
            placeholder="say {player} chegou!" />
        )}
        {a.type === 'title' && (
          <>
            <input className="input mb-2 font-mono text-xs" value={a.title}
              onChange={e => setR({ ...r, action: { ...a, title: e.target.value } })}
              placeholder="§aBem-vindo {player}" />
            <input className="input mb-3 font-mono text-xs" value={a.subtitle ?? ''}
              onChange={e => setR({ ...r, action: { ...a, subtitle: e.target.value } })}
              placeholder="subtítulo" />
          </>
        )}
        {a.type === 'sound' && (
          <input className="input mb-3 font-mono text-xs" value={a.sound}
            onChange={e => setR({ ...r, action: { type: 'sound', sound: e.target.value } })}
            placeholder="minecraft:entity.player.levelup" />
        )}

        <div className="text-xs text-liberthia-300/60 mb-4">
          Use <span className="chip">{'{player}'}</span> pra inserir o nome do player que disparou.
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(r)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
