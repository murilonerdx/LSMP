import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'
import { Autocomplete } from '../components/Autocomplete'
import { useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Loop broadcaster — define mensagens que rotacionam num intervalo, escolhe modo
 * (chat / title / actionbar) e alvo (todos / específicos). Frontend mantém o
 * timer; quando aba fecha, loop para. Não persiste no backend (decisão MVP).
 */

type Mode = 'chat' | 'title' | 'subtitle' | 'actionbar' | 'sound'

type LoopConfig = {
  mode: Mode
  intervalSec: number
  messages: string[]
  targets: 'all' | string[]
  soundId?: string
}

const DEFAULT_BROADCASTER: LoopConfig = {
  mode: 'title',
  intervalSec: 30,
  messages: ['§a✦ §dBem-vindo §a✦', '§7Liberthia Server', '§e🌌 Matter awaits...'],
  targets: 'all',
  soundId: 'minecraft:block.note_block.pling',
}

export function BroadcasterPage() {
  const [config, setConfig] = useKvState<LoopConfig>('broadcaster', DEFAULT_BROADCASTER)

  const [running, setRunning] = useState(false)
  const [tick, setTick] = useState(0)
  const [history, setHistory] = useState<{ ts: number; idx: number; ok: boolean; err?: string }[]>([])
  const idxRef = useRef(0)
  const timerRef = useRef<number | null>(null)

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []

  useEffect(() => () => { if (timerRef.current) clearInterval(timerRef.current) }, [])

  function patch(p: Partial<LoopConfig>) { setConfig((c) => ({ ...c, ...p })) }

  async function fireOnce() {
    const msg = config.messages[idxRef.current % Math.max(config.messages.length, 1)]
    idxRef.current++
    setTick((t) => t + 1)
    if (!msg && config.mode !== 'sound') return
    const targets = config.targets === 'all' ? players.map((p) => p.uuid) : config.targets
    const promises: Promise<any>[] = []
    for (const uuid of targets) {
      switch (config.mode) {
        case 'chat':
          promises.push(api.command(`tellraw ${nameByUuid(players, uuid)} {"text":${JSON.stringify(msg)}}`, 'broadcaster'))
          break
        case 'title':
          promises.push(api.title(uuid, msg, '', 8, 50, 12))
          break
        case 'subtitle':
          promises.push(api.title(uuid, ' ', msg, 8, 50, 12))
          break
        case 'actionbar':
          promises.push(api.command(`title ${nameByUuid(players, uuid)} actionbar {"text":${JSON.stringify(msg)}}`, 'broadcaster'))
          break
        case 'sound':
          if (config.soundId) promises.push(api.sound(uuid, config.soundId))
          break
      }
    }
    try {
      await Promise.allSettled(promises)
      setHistory((h) => [{ ts: Date.now(), idx: idxRef.current - 1, ok: true }, ...h].slice(0, 50))
    } catch (e: any) {
      setHistory((h) => [{ ts: Date.now(), idx: idxRef.current - 1, ok: false, err: e.message }, ...h].slice(0, 50))
    }
  }

  function start() {
    if (running) return
    if (config.messages.length === 0 && config.mode !== 'sound') {
      toast.err('Adicione pelo menos uma mensagem')
      return
    }
    setRunning(true)
    fireOnce()
    timerRef.current = window.setInterval(fireOnce, Math.max(config.intervalSec, 3) * 1000)
    toast.ok('Broadcaster iniciado')
  }

  function stop() {
    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = null
    setRunning(false)
    toast.info('Broadcaster parado')
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="page-title">📡 Broadcaster</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Mensagens rotativas que aparecem pra todos no servidor a cada N segundos.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {running && <div className="flex items-center gap-2"><span className="live-dot" /><span className="text-xs">tick #{tick}</span></div>}
          {!running && <button className="btn-success" onClick={start}>▶ Start Loop</button>}
          {running && <button className="btn-danger" onClick={stop}>⏹ Stop</button>}
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_380px] gap-5">
        <div className="space-y-4">
          {/* Config */}
          <div className="card-glow">
            <h3 className="font-bold mb-3">⚙ Configuração</h3>
            <div className="grid grid-cols-2 gap-3 mb-4">
              <div>
                <label className="label block mb-1">Modo</label>
                <div className="tab-strip flex-wrap">
                  {(['chat', 'title', 'subtitle', 'actionbar', 'sound'] as Mode[]).map((m) => (
                    <div
                      key={m}
                      className={`tab-item ${config.mode === m ? 'active' : ''}`}
                      onClick={() => patch({ mode: m })}
                    >{m}</div>
                  ))}
                </div>
              </div>
              <div>
                <label className="label block mb-1">Intervalo: {config.intervalSec}s</label>
                <input
                  type="range" min={3} max={600} step={1}
                  value={config.intervalSec}
                  onChange={(e) => patch({ intervalSec: Number(e.target.value) })}
                  className="w-full"
                />
              </div>
            </div>
            <div>
              <label className="label block mb-1">Alvo</label>
              <select
                className="input"
                value={config.targets === 'all' ? 'all' : 'custom'}
                onChange={(e) => patch({ targets: e.target.value === 'all' ? 'all' : [] })}
              >
                <option value="all">Todos players online</option>
                <option value="custom">Selecionar específicos</option>
              </select>
              {config.targets !== 'all' && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {players.map((p) => {
                    const on = (config.targets as string[]).includes(p.uuid)
                    return (
                      <button
                        key={p.uuid}
                        className={`btn-ghost btn-sm ${on ? '!bg-liberthia-500/30 !text-white' : ''}`}
                        onClick={() => patch({
                          targets: on
                            ? (config.targets as string[]).filter((u) => u !== p.uuid)
                            : [...(config.targets as string[]), p.uuid]
                        })}
                      >{p.name}</button>
                    )
                  })}
                </div>
              )}
            </div>
            {config.mode === 'sound' && (
              <div className="mt-3">
                <label className="label block mb-1">Sound ID</label>
                <SoundField value={config.soundId ?? ''} onChange={(v) => patch({ soundId: v })} />
              </div>
            )}
          </div>

          {/* Mensagens */}
          {config.mode !== 'sound' && (
            <div className="card-glow">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-bold">💬 Mensagens ({config.messages.length})</h3>
                <button className="btn-ghost btn-sm" onClick={() => patch({ messages: [...config.messages, ''] })}>+ Add</button>
              </div>
              <div className="space-y-2">
                {config.messages.map((m, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <span className="text-liberthia-300/40 font-mono text-xs w-6">{i + 1}</span>
                    <input
                      className="input flex-1 font-mono text-xs"
                      value={m}
                      placeholder="§a Sua mensagem aqui (use §a §b §c pra cores)"
                      onChange={(e) => {
                        const next = [...config.messages]; next[i] = e.target.value
                        patch({ messages: next })
                      }}
                    />
                    <button className="btn-ghost btn-sm" onClick={() => patch({ messages: config.messages.filter((_, j) => j !== i) })}>🗑</button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar history */}
        <div className="space-y-4">
          <div className="card text-sm">
            <div className="font-bold mb-2">📊 Status</div>
            <Row k="Loop" v={running ? <span className="badge badge-green">running</span> : <span className="badge badge-red">parado</span>} />
            <Row k="Próximo" v={running ? `${config.intervalSec}s` : '—'} />
            <Row k="Mensagens" v={`${config.messages.length}`} />
            <Row k="Alvos" v={config.targets === 'all' ? `todos (${players.length})` : `${(config.targets as string[]).length}`} />
            <Row k="Disparos" v={`${tick}`} />
          </div>

          <div className="card">
            <h3 className="font-bold mb-2">🕒 Histórico</h3>
            <div className="font-mono text-xs space-y-0.5 max-h-72 overflow-y-auto">
              {history.length === 0 && <div className="text-liberthia-300/50 italic">— vazio —</div>}
              {history.map((h, i) => (
                <div key={i} className={h.ok ? 'text-emerald-300' : 'text-red-300'}>
                  {new Date(h.ts).toLocaleTimeString()} #{h.idx}
                  {h.err && <span className="text-red-200"> — {h.err}</span>}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function Row({ k, v }: { k: string; v: any }) {
  return (
    <div className="flex justify-between py-0.5">
      <span className="text-liberthia-300/70 text-xs">{k}</span>
      <span className="text-xs">{v}</span>
    </div>
  )
}

function nameByUuid(players: Player[], uuid: string) {
  return players.find((p) => p.uuid === uuid)?.name ?? `@a[uuid=${uuid}]`
}

function SoundField({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const opts = useSoundOptions()
  return <Autocomplete value={value} onChange={onChange} options={opts}
    placeholder="minecraft:block.note_block.pling" />
}
