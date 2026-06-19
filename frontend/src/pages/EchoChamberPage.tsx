import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Echo Chamber — pega as últimas N mensagens de chat de um player e usa
 * elas como sussurros fantasmas (subtitles distorcidas) que aparecem
 * pra ELE em loop. As palavras dele mesmo, vindas de fora.
 *
 * Buscar últimas mensagens via /api/history/chat?uuid=X&limit=N
 */

const GLITCH_PROBABILITY = 0.15  // chance de cada char virar glitch
const GLITCH_CHARS = ['#', '@', '?', '!', '$', '%', '*']

function garble(s: string, intensity: number): string {
  return s.split('').map((c) => {
    if (c === ' ') return c
    return Math.random() < intensity ? GLITCH_CHARS[Math.floor(Math.random() * GLITCH_CHARS.length)] : c
  }).join('')
}

export function EchoChamberPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [running, setRunning] = useState(false)
  const [messages, setMessages] = useState<string[]>([])
  const [loops, setLoops] = useState(8)
  const [intervalMs, setIntervalMs] = useState(1200)
  const [glitchIntensity, setGlitchIntensity] = useState(0.2)
  const [reversed, setReversed] = useState(true)
  const [shuffled, setShuffled] = useState(false)
  const [progress, setProgress] = useState(0)
  const [cancelFlag, setCancelFlag] = useState(false)

  async function loadMessages() {
    if (!target) return
    try {
      const entries = await api.chatHistory(target, 0, 30)
      const msgs = entries.map((e) => e.message).filter((m) => m && !m.startsWith('[LIB:'))
      setMessages(msgs)
      toast.ok(`✓ ${msgs.length} mensagens carregadas`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function summon() {
    if (!target || messages.length === 0) {
      toast.err('Carregue mensagens primeiro')
      return
    }
    setRunning(true); setProgress(0); setCancelFlag(false)
    const player = players.find((p) => p.uuid === target)
    if (!player) return

    try {
      await api.title(target, '§5§l🌀 ECHO CHAMBER', '§7§oeles repetem o que você disse...', 15, 80, 20)
      await api.sound(target, 'minecraft:entity.warden.heartbeat', 1, 0.5)
    } catch {}
    await new Promise((r) => setTimeout(r, 3000))

    let pool = [...messages]
    if (shuffled) pool.sort(() => Math.random() - 0.5)
    if (reversed) pool = pool.map((m) => m.split('').reverse().join(''))

    for (let loop = 0; loop < loops; loop++) {
      if (cancelFlag) break
      const msg = pool[loop % pool.length]
      const intensity = glitchIntensity + (loop / loops) * 0.3  // garble crescendo
      const garbled = garble(msg, intensity)
      const color = ['§8', '§7', '§5', '§4', '§0'][Math.floor(Math.random() * 5)]
      try {
        await api.title(target, ' ', `${color}§o${garbled}`, 5, 50, 10)
        await api.sound(target, 'minecraft:entity.allay.death', 0.3, 0.4 + Math.random() * 0.3)
      } catch {}
      setProgress((loop + 1) / loops)
      await new Promise((r) => setTimeout(r, intervalMs))
    }

    try {
      await api.sound(target, 'minecraft:entity.warden.sonic_boom', 0.5, 0.5)
      await api.title(target, '§4§l. . .', '§8§oeles ouviram tudo', 10, 80, 30)
    } catch {}

    setRunning(false)
    setProgress(0)
  }

  function cancel() {
    setCancelFlag(true)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">🌀 Echo Chamber</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Captura últimas mensagens de chat do player e usa como sussurros fantasmas em loop.
          As palavras dele, distorcidas, repetidas — vindas do nada.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3">🎯 Setup</h3>
            <label className="label block mb-1">Player</label>
            <select className="input mb-3" value={target} onChange={(e) => { setTarget(e.target.value); setMessages([]) }}>
              <option value="">— escolher —</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>

            <button className="btn-ghost w-full mb-3" onClick={loadMessages} disabled={!target || running}>
              📥 Carregar últimas mensagens
            </button>

            {messages.length > 0 && (
              <div className="bg-liberthia-900/40 rounded-lg p-2 max-h-40 overflow-y-auto text-xs space-y-1 mb-3">
                {messages.map((m, i) => (
                  <div key={i} className="text-liberthia-300/80 truncate">
                    <span className="text-liberthia-300/40 mr-1">#{i + 1}</span>
                    {m}
                  </div>
                ))}
              </div>
            )}

            <div className="grid grid-cols-2 gap-3 mb-3 text-xs">
              <div>
                <label className="label">Loops: {loops}</label>
                <input type="range" min={3} max={30} value={loops} onChange={(e) => setLoops(Number(e.target.value))} className="w-full" />
              </div>
              <div>
                <label className="label">Intervalo: {intervalMs}ms</label>
                <input type="range" min={400} max={3000} step={100} value={intervalMs} onChange={(e) => setIntervalMs(Number(e.target.value))} className="w-full" />
              </div>
              <div className="col-span-2">
                <label className="label">Glitch inicial: {(glitchIntensity * 100).toFixed(0)}%</label>
                <input type="range" min={0} max={0.5} step={0.05} value={glitchIntensity} onChange={(e) => setGlitchIntensity(Number(e.target.value))} className="w-full" />
                <div className="text-[10px] text-liberthia-300/50">cresce até 50% no último loop</div>
              </div>
            </div>

            <div className="flex gap-3 mb-3 text-xs flex-wrap">
              <label className="flex items-center gap-1 cursor-pointer">
                <input type="checkbox" checked={reversed} onChange={(e) => setReversed(e.target.checked)} /> 🔄 Reverter
              </label>
              <label className="flex items-center gap-1 cursor-pointer">
                <input type="checkbox" checked={shuffled} onChange={(e) => setShuffled(e.target.checked)} /> 🔀 Embaralhar
              </label>
            </div>

            {!running ? (
              <button className="btn w-full" onClick={summon} disabled={messages.length === 0}>
                🌀 Iniciar Eco
              </button>
            ) : (
              <button className="btn-danger w-full pulse-glow" onClick={cancel}>⏹ Parar</button>
            )}

            {running && (
              <div className="mt-2 h-2 rounded-full bg-liberthia-900 overflow-hidden">
                <div className="h-full bg-gradient-to-r from-liberthia-400 to-liberthia-600 transition-all" style={{ width: `${progress * 100}%` }} />
              </div>
            )}
          </div>
        </div>

        <div className="card text-xs text-liberthia-300/70">
          <div className="font-bold text-liberthia-200 mb-2">💡 Como funciona</div>
          <p>• Busca via <span className="chip">/api/history/chat</span> as últimas 30 mensagens do player</p>
          <p>• A cada loop, pega uma mensagem e aplica garble (substitui chars aleatórios)</p>
          <p>• Garble cresce: começa em X% e termina em X+30%</p>
          <p>• <b>Reverter</b>: lê a mensagem de trás pra frente — assustador</p>
          <p>• <b>Embaralhar</b>: pega mensagens em ordem aleatória</p>
          <p className="mt-2 italic">Funciona melhor com players que conversam bastante — sem chat history, sem material.</p>
        </div>
      </div>
    </div>
  )
}
