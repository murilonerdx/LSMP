import { useState } from 'react'
import { api } from '../lib/api'
import { PageHeader } from '../components/PageHeader'
import { Autocomplete } from '../components/Autocomplete'
import { useCommandOptions } from '../lib/mcAutocomplete'

const SUGGESTIONS = [
  '/give @a minecraft:diamond 64',
  '/time set day',
  '/weather clear',
  '/gamemode creative @a',
  '/effect give @a minecraft:speed 60 5',
  '/say Hello server!',
  '/kill @e[type=!player]',
  '/difficulty peaceful',
  '/tp @s 0 100 0',
]

export function ConsolePage() {
  const [cmd, setCmd] = useState('')
  const [history, setHistory] = useState<{ cmd: string; result: any; ts: number }[]>([])
  const [busy, setBusy] = useState(false)
  const [broadcast, setBroadcast] = useState('')
  const commandOpts = useCommandOptions()

  const send = async (c: string) => {
    if (!c.trim()) return
    setBusy(true)
    try {
      const res = await api.command(c.startsWith('/') ? c.substring(1) : c)
      setHistory(h => [{ cmd: c, result: res, ts: Date.now() }, ...h].slice(0, 100))
    } catch (e: any) {
      setHistory(h => [{ cmd: c, result: { error: e.message }, ts: Date.now() }, ...h].slice(0, 100))
    }
    setBusy(false)
    setCmd('')
  }

  const sendBroadcast = async () => {
    if (!broadcast.trim()) return
    try {
      await api.broadcast(broadcast)
      setHistory(h => [{ cmd: `[BROADCAST] ${broadcast}`, result: { ok: true }, ts: Date.now() }, ...h])
      setBroadcast('')
    } catch (e: any) {
      setHistory(h => [{ cmd: `[BROADCAST] ${broadcast}`, result: { error: e.message }, ts: Date.now() }, ...h])
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Console" subtitle="Executa comandos /op-level direto no servidor" icon="⌨" />

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2 space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3 flex items-center gap-2">
              <span>⚡</span> Executar comando
            </h3>
            <form onSubmit={e => { e.preventDefault(); send(cmd) }} className="flex gap-2">
              <span className="self-center text-liberthia-400 font-mono">/</span>
              <div className="flex-1">
                <Autocomplete value={cmd} onChange={setCmd}
                  options={commandOpts} placeholder="give @a minecraft:diamond 64"
                  className="font-mono" />
              </div>
              <button type="submit" className="btn" disabled={busy}>{busy ? '...' : 'Run'}</button>
            </form>
            <div className="mt-3 flex flex-wrap gap-1">
              {SUGGESTIONS.map(s => (
                <button key={s} className="btn-ghost btn-sm font-mono" onClick={() => setCmd(s.substring(1))}>
                  {s}
                </button>
              ))}
            </div>
          </div>

          <div className="card">
            <h3 className="font-bold mb-3 flex items-center gap-2">
              <span>📢</span> Broadcast — manda mensagem pra todos
            </h3>
            <div className="flex gap-2">
              <input
                className="input"
                placeholder="Mensagem que aparece no chat de todos"
                value={broadcast}
                onChange={e => setBroadcast(e.target.value)}
              />
              <button className="btn-success" onClick={sendBroadcast}>Enviar</button>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold">📜 Histórico ({history.length})</h3>
              {history.length > 0 && <button className="btn-ghost btn-sm" onClick={() => setHistory([])}>Limpar</button>}
            </div>
            <div className="space-y-1 max-h-[50vh] overflow-y-auto font-mono text-xs">
              {history.length === 0 ? (
                <p className="text-liberthia-300/40 italic text-center py-8">
                  Nenhum comando ainda. Tenta um dos sugeridos.
                </p>
              ) : history.map(h => {
                const t = new Date(h.ts).toLocaleTimeString()
                const err = h.result?.error
                return (
                  <div key={h.ts} className={`p-2 rounded border-l-2 ${err ? 'border-red-500 bg-red-900/10' : 'border-emerald-500 bg-emerald-900/10'}`}>
                    <div className="flex justify-between text-liberthia-300/60 mb-0.5">
                      <span>$ {h.cmd}</span>
                      <span>{t}</span>
                    </div>
                    <div className={err ? 'text-red-400' : 'text-emerald-400'}>
                      {err ? `✗ ${err}` : `✓ ok ${h.result?.result !== undefined ? `result=${h.result.result}` : ''}`}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-2">💡 Dica</h3>
            <p className="text-sm text-liberthia-300/80">
              Comandos rodam em <span className="badge badge-purple">op level 4</span> (max).
              Sintaxe vanilla. Selectors <code className="bg-black/30 px-1 rounded">@a</code> <code className="bg-black/30 px-1 rounded">@p</code> <code className="bg-black/30 px-1 rounded">@e</code> funcionam.
            </p>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2">⚡ Atalhos</h3>
            <div className="text-xs space-y-2 text-liberthia-300/70">
              <div><span className="badge badge-purple">/give</span> dar item pra player</div>
              <div><span className="badge badge-purple">/effect</span> aplicar effect</div>
              <div><span className="badge badge-purple">/tp</span> teleport</div>
              <div><span className="badge badge-purple">/time set</span> setar hora</div>
              <div><span className="badge badge-purple">/weather</span> clima</div>
              <div><span className="badge badge-purple">/gamemode</span> gamemode</div>
              <div><span className="badge badge-purple">/op /deop</span> permissões</div>
              <div><span className="badge badge-purple">/ban /pardon</span> banir</div>
              <div><span className="badge badge-purple">/whitelist</span> whitelist</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
