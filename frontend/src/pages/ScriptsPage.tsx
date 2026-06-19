import { useRef, useState } from 'react'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Script runner com mini-DSL:
 *   /comando-do-mc args...     -> roda como /comando
 *   wait 5s                    -> espera N segundos (s/ms suportados)
 *   say <texto>                -> broadcast
 *   #comentário                -> ignorado
 *
 * Scripts persistem em localStorage. Pode rodar tudo, parar, e ver log live.
 */

type Saved = { name: string; body: string; updated: number }

const PRESETS: Record<string, string> = {
  'Boas-vindas': [
    '# Boas-vindas com title + som',
    'say §a✦ Bem-vindos ao Liberthia!',
    'wait 1s',
    'execute as @a run title @s title {"text":"§dLiberthia","bold":true}',
    'execute as @a run title @s subtitle {"text":"§7Servidor Premium"}',
    'execute as @a run playsound minecraft:ui.toast.challenge_complete master @s',
  ].join('\n'),
  'Apocalipse': [
    '# 30s de caos',
    'weather thunder 600',
    'say §c⚠ APOCALIPSE EM 5...',
    'wait 5s',
    'execute as @a at @s run summon minecraft:lightning_bolt',
    'wait 2s',
    'execute as @a at @s run summon minecraft:lightning_bolt',
    'wait 2s',
    'effect give @a minecraft:slowness 10 2',
  ].join('\n'),
  'Reset diário': [
    '# Reseta condições do server',
    'weather clear 60000',
    'time set day',
    'difficulty normal',
    'save-all',
    'say §a✓ Servidor resetado',
  ].join('\n'),
  'Festa': [
    '# Fogos e música',
    'execute as @a at @s run summon minecraft:firework_rocket ~ ~5 ~ {LifeTime:30,FireworksItem:{id:"firework_rocket",Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16711680,65280,255]}]}}}}',
    'execute as @a run playsound minecraft:entity.player.levelup master @s',
    'wait 2s',
    'execute as @a at @s run summon minecraft:firework_rocket ~ ~5 ~ {LifeTime:30,FireworksItem:{id:"firework_rocket",Count:1,tag:{Fireworks:{Explosions:[{Type:2,Colors:[I;16776960,16711935]}]}}}}',
  ].join('\n'),
}

export function ScriptsPage() {
  const [body, setBody] = useState<string>(PRESETS['Boas-vindas'])
  const [name, setName] = useState('untitled')
  const [running, setRunning] = useState(false)
  const [logs, setLogs] = useState<{ ts: number; line: string; ok?: boolean }[]>([])
  const [saved, setSaved] = useKvState<Saved[]>('scripts', [])
  const cancelRef = useRef(false)

  function persist(next: Saved[]) {
    setSaved(next)
  }

  function save() {
    const item: Saved = { name: name || 'untitled', body, updated: Date.now() }
    const next = [item, ...saved.filter((s) => s.name !== item.name)]
    persist(next)
    toast.ok(`Salvo: ${item.name}`)
  }

  function load(s: Saved) {
    setName(s.name)
    setBody(s.body)
    toast.info(`Carregado: ${s.name}`)
  }

  function del(s: Saved) {
    persist(saved.filter((x) => x.name !== s.name))
  }

  function log(line: string, ok?: boolean) {
    setLogs((l) => [{ ts: Date.now(), line, ok }, ...l].slice(0, 200))
  }

  async function run() {
    if (running) return
    cancelRef.current = false
    setRunning(true)
    setLogs([])
    const lines = body.split('\n').map((l) => l.trim()).filter((l) => l && !l.startsWith('#'))
    log(`▶ iniciando ${lines.length} linhas`)
    for (const line of lines) {
      if (cancelRef.current) { log('⏹ cancelado pelo usuário'); break }
      try {
        const m = line.match(/^wait\s+(\d+(?:\.\d+)?)(s|ms)?$/i)
        if (m) {
          const n = parseFloat(m[1])
          const ms = (m[2] === 'ms') ? n : n * 1000
          log(`⏳ wait ${ms}ms`)
          await new Promise((r) => setTimeout(r, ms))
          continue
        }
        // strip leading '/'
        const cmd = line.startsWith('/') ? line.slice(1) : line
        await api.command(cmd, 'scripts')
        log(`✓ ${cmd}`, true)
      } catch (e: any) {
        log(`✗ ${line} — ${e.message ?? e}`, false)
      }
    }
    log('■ fim')
    setRunning(false)
  }

  function stop() {
    cancelRef.current = true
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="page-title">📜 Scripts</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Sequência de comandos com delays. Linhas começando com <span className="chip">#</span> são comentários.
            Use <span className="chip">wait 5s</span> pra delay.
          </p>
        </div>
        <div className="flex gap-2">
          {!running && <button className="btn-success" onClick={run}>▶ Run</button>}
          {running && <button className="btn-danger" onClick={stop}>⏹ Stop</button>}
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        {/* editor + log */}
        <div className="space-y-4">
          <div className="card-glow">
            <div className="flex gap-2 mb-3">
              <input className="input flex-1" placeholder="Nome do script" value={name} onChange={(e) => setName(e.target.value)} />
              <button className="btn-cyan" onClick={save}>💾 Save</button>
            </div>
            <textarea
              className="code-editor"
              rows={16}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              spellCheck={false}
            />
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-bold">Output</h3>
              <button className="btn-ghost btn-sm" onClick={() => setLogs([])}>Limpar</button>
            </div>
            <div className="font-mono text-xs space-y-0.5 max-h-72 overflow-y-auto">
              {logs.length === 0 && <div className="text-liberthia-300/50 italic">— sem output —</div>}
              {logs.map((l, i) => (
                <div key={i} className={`flex gap-2 ${l.ok === false ? 'text-red-300' : l.ok === true ? 'text-emerald-300' : 'text-liberthia-300/80'}`}>
                  <span className="text-liberthia-300/40">{new Date(l.ts).toLocaleTimeString()}</span>
                  <span className="flex-1 break-all">{l.line}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* sidebar */}
        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">📋 Presets</h3>
            <div className="space-y-1.5">
              {Object.entries(PRESETS).map(([k, v]) => (
                <button key={k} className="btn-ghost w-full text-left" onClick={() => { setBody(v); setName(k); }}>
                  {k}
                </button>
              ))}
            </div>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2">💾 Salvos ({saved.length})</h3>
            {saved.length === 0 && <p className="text-xs text-liberthia-300/50">Nenhum script salvo ainda.</p>}
            <div className="space-y-1.5">
              {saved.map((s) => (
                <div key={s.name} className="flex items-center gap-1">
                  <button className="btn-ghost flex-1 text-left text-xs" onClick={() => load(s)}>
                    {s.name}
                    <span className="block text-[10px] opacity-50">{new Date(s.updated).toLocaleString()}</span>
                  </button>
                  <button className="btn-ghost btn-sm" onClick={() => del(s)}>🗑</button>
                </div>
              ))}
            </div>
          </div>

          <div className="card text-xs space-y-1.5 text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">Sintaxe</div>
            <div><span className="chip">/say hello</span> ou <span className="chip">say hello</span> — comando vanilla</div>
            <div><span className="chip">wait 3s</span> ou <span className="chip">wait 500ms</span> — delay</div>
            <div><span className="chip"># comentário</span> — ignorado</div>
            <div><span className="chip">execute as @a ...</span> — vanilla executors</div>
          </div>
        </div>
      </div>
    </div>
  )
}
