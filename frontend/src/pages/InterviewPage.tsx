import { useRef, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { NumInput } from '../components/NumInput'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Interview / Roteirizador — NPC fala em proximidade, espera resposta do player
 * mais próximo, captura, anexa no roteiro. Iterativo: a cada turn o operador
 * compõe a próxima fala olhando a resposta capturada.
 *
 * Uso típico:
 *  1. Define origem (xyz) + raio = onde o NPC tá / até onde escuta
 *  2. Escreve fala (speaker + texto)
 *  3. "Send + Wait" → whisper aos vizinhos, polling em /api/history/chat até
 *     vir uma mensagem nova de algum recipient (timeout configurável)
 *  4. Resposta vai pro transcript com timestamp; reabre input pra próxima fala
 *  5. Salva transcript em localStorage e exporta CSV/JSON
 */

type Turn =
  | { kind: 'npc'; speaker: string; speakerColor: string; text: string; ts: number; recipients: number; sound?: string }
  | { kind: 'player'; name: string; uuid: string; text: string; ts: number }

type Session = { name: string; turns: Turn[]; updated: number; origin: { x: number; y: number; z: number; radius: number } }

export function InterviewPage() {
  const sounds = useSoundOptions()

  const [origin, setOrigin] = useState({ x: 0, y: 80, z: 0, radius: 12 })
  const [turns, setTurns] = useState<Turn[]>([])
  const [name, setName] = useState('interview-1')
  const [speaker, setSpeaker] = useState('Mira')
  const [speakerColor, setSpeakerColor] = useState('§e')
  const [text, setText] = useState('')
  const [sound, setSound] = useState('minecraft:block.note_block.bit')
  const [waitForResponse, setWaitForResponse] = useState(true)
  const [timeoutSec, setTimeoutSec] = useState(60)
  const [waiting, setWaiting] = useState(false)
  const cancelRef = useRef(false)
  const [saved, setSaved] = useKvState<Session[]>('interviews', [])

  function persist(list: Session[]) {
    setSaved(list)
  }

  function saveSession() {
    const item: Session = { name, turns, updated: Date.now(), origin }
    persist([item, ...saved.filter((s) => s.name !== name)])
    toast.ok(`Saved: ${name}`)
  }

  function loadSession(s: Session) {
    setName(s.name); setTurns(s.turns); setOrigin(s.origin)
  }


  async function send() {
    if (!text.trim()) { toast.err('Escreva a fala'); return }
    cancelRef.current = false
    const startTs = Date.now() - 100 // overlap pra não perder
    try {
      const res = await api.whisper({
        ...origin,
        message: text,
        speaker: speakerColor + speaker,
        sound,
      })
      const newTurn: Turn = {
        kind: 'npc', speaker, speakerColor, text, ts: Date.now(),
        recipients: res.recipients?.length ?? 0, sound,
      }
      setTurns((t) => [...t, newTurn])
      setText('')

      if (!waitForResponse) {
        toast.ok(`Whisper para ${res.recipients?.length ?? 0} player(s)`)
        return
      }
      if (res.recipients?.length === 0) {
        toast.err('Ninguém perto pra responder')
        return
      }
      // Captura próxima resposta de qualquer recipient
      setWaiting(true)
      const recipUuids = new Set(res.recipients.map((r) => r.uuid))
      const deadline = Date.now() + timeoutSec * 1000
      let captured: { uuid: string; name: string; text: string; ts: number } | null = null
      while (Date.now() < deadline && !cancelRef.current) {
        await new Promise((r) => setTimeout(r, 1500))
        try {
          const entries = await api.chatHistory('', startTs, 100)
          for (const e of entries) {
            if (e.ts > startTs && recipUuids.has(e.uuid)) {
              captured = { uuid: e.uuid, name: e.name, text: e.message, ts: e.ts }
              break
            }
          }
        } catch {}
        if (captured) break
      }
      if (captured) {
        setTurns((t) => [...t, { kind: 'player', uuid: captured!.uuid, name: captured!.name, text: captured!.text, ts: captured!.ts }])
        toast.ok(`Capturado: ${captured.name}`)
      } else if (cancelRef.current) {
        toast.info('Cancelado')
      } else {
        toast.err(`Sem resposta em ${timeoutSec}s`)
      }
    } catch (e: any) {
      toast.err(e.message)
    } finally {
      setWaiting(false)
    }
  }

  function exportTranscript() {
    const txt = turns.map((t) => {
      const dt = new Date(t.ts).toLocaleString()
      if (t.kind === 'npc') return `[${dt}] ${t.speakerColor}${t.speaker}§r: ${t.text}`
      return `[${dt}] ${t.name} (player): ${t.text}`
    }).join('\n')
    const blob = new Blob([txt], { type: 'text/plain' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${name}.txt`
    a.click()
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🗣 Interview / Roteirizador</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            NPC fala só pra quem tá perto, captura a resposta do player do chat, monta um roteiro turn-by-turn.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost btn-sm" onClick={exportTranscript} disabled={turns.length === 0}>⬇ TXT</button>
          <button className="btn-cyan btn-sm" onClick={saveSession} disabled={turns.length === 0}>💾 Save</button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-4">
          {/* Config origem */}
          <div className="card-glow">
            <h3 className="font-bold mb-2 text-sm">📍 Origem do NPC + alcance</h3>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 mb-2">
              <div>
                <label className="label block">X</label>
                <NumInput value={origin.x} onChange={(v) => setOrigin({ ...origin, x: v })} />
              </div>
              <div>
                <label className="label block">Y</label>
                <NumInput value={origin.y} onChange={(v) => setOrigin({ ...origin, y: v })} />
              </div>
              <div>
                <label className="label block">Z</label>
                <NumInput value={origin.z} onChange={(v) => setOrigin({ ...origin, z: v })} />
              </div>
              <div>
                <label className="label block">Raio</label>
                <input type="number" className="input" value={origin.radius} onChange={(e) => setOrigin({ ...origin, radius: Number(e.target.value) })} />
              </div>
              <div className="col-span-2 sm:col-span-1 flex items-end">
                <PlayerPosPicker label="📍 De player..."
                  onPick={(p) => {
                    setOrigin({ ...origin, x: Math.round(p.x), y: Math.round(p.y), z: Math.round(p.z) })
                    toast.info(`Origem em ${p.name}`)
                  }} />
              </div>
            </div>
          </div>

          {/* Transcript */}
          <div className="card-glow">
            <h3 className="font-bold mb-3 flex items-center gap-2">
              📜 Roteiro
              <span className="badge badge-purple">{turns.length} turns</span>
              {waiting && <span className="ml-auto flex items-center gap-2 text-emerald-300 text-xs"><span className="live-dot" /> aguardando resposta...</span>}
            </h3>
            <div className="space-y-2 max-h-[55vh] overflow-y-auto">
              {turns.length === 0 && (
                <div className="text-center py-12 text-liberthia-300/50 text-sm">
                  Roteiro vazio. Escreva uma fala abaixo e envie.
                </div>
              )}
              {turns.map((t, i) => t.kind === 'npc' ? (
                <div key={i} className="bg-liberthia-500/10 border border-liberthia-400/30 rounded-xl p-3">
                  <div className="flex items-center gap-2 mb-1 text-xs">
                    <span className="font-bold" style={{ color: hexFromColor(t.speakerColor) }}>{t.speaker}</span>
                    <span className="chip">📡 {t.recipients} ouviram</span>
                    <span className="text-liberthia-300/50 ml-auto">{new Date(t.ts).toLocaleTimeString()}</span>
                    <button className="btn-ghost btn-sm" onClick={() => setTurns((tt) => tt.filter((_, j) => j !== i))}>🗑</button>
                  </div>
                  <div className="text-sm">{t.text}</div>
                </div>
              ) : (
                <div key={i} className="bg-cyan-500/10 border border-cyan-400/30 rounded-xl p-3 ml-8">
                  <div className="flex items-center gap-2 mb-1 text-xs">
                    <img src={`https://mc-heads.net/avatar/${t.uuid}/16`} className="rounded" />
                    <span className="font-bold text-cyan-300">{t.name}</span>
                    <span className="chip">👤 player</span>
                    <span className="text-liberthia-300/50 ml-auto">{new Date(t.ts).toLocaleTimeString()}</span>
                    <button className="btn-ghost btn-sm" onClick={() => setTurns((tt) => tt.filter((_, j) => j !== i))}>🗑</button>
                  </div>
                  <div className="text-sm">{t.text}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Input nova fala */}
          <div className="card-glow">
            <h3 className="font-bold mb-2 text-sm">✏ Próxima fala do NPC</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mb-2">
              <input className="input" placeholder="Speaker" value={speaker} onChange={(e) => setSpeaker(e.target.value)} />
              <input className="input font-mono" placeholder="§e" value={speakerColor} onChange={(e) => setSpeakerColor(e.target.value)} />
              <Autocomplete value={sound} onChange={setSound} options={sounds} placeholder="minecraft:block.note_block.bit" />
            </div>
            <textarea
              className="code-editor mb-2"
              rows={3}
              placeholder="O que o NPC vai dizer (use §a §b § cores)..."
              value={text}
              onChange={(e) => setText(e.target.value)}
              disabled={waiting}
              onKeyDown={(e) => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send() }}
            />
            <div className="flex items-center gap-3 flex-wrap text-xs mb-2">
              <label className="flex items-center gap-1">
                <input type="checkbox" checked={waitForResponse} onChange={(e) => setWaitForResponse(e.target.checked)} disabled={waiting} />
                Esperar resposta
              </label>
              {waitForResponse && (
                <>
                  <span className="label">Timeout:</span>
                  <input type="range" min={10} max={300} value={timeoutSec} onChange={(e) => setTimeoutSec(Number(e.target.value))} className="w-32" disabled={waiting} />
                  <span className="font-mono">{timeoutSec}s</span>
                </>
              )}
              <span className="text-liberthia-300/40 ml-auto">Ctrl+Enter pra enviar</span>
            </div>
            <div className="flex gap-2">
              {!waiting && <button className="btn flex-1" onClick={send} disabled={!text.trim()}>📡 Send {waitForResponse ? '+ Wait' : ''}</button>}
              {waiting && <button className="btn-danger flex-1" onClick={() => { cancelRef.current = true }}>⏹ Cancelar espera</button>}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">📋 Sessions</h3>
            <input className="input mb-2 text-xs" value={name} onChange={(e) => setName(e.target.value)} placeholder="nome da sessão" />
            <div className="space-y-1">
              {saved.map((s) => (
                <div key={s.name} className="flex items-center gap-1">
                  <button className="btn-ghost flex-1 text-left text-xs" onClick={() => loadSession(s)}>
                    <div className="font-bold">{s.name}</div>
                    <div className="text-[10px] opacity-50">{s.turns.length} turns · {new Date(s.updated).toLocaleString()}</div>
                  </button>
                  <button className="btn-ghost btn-sm" onClick={() => persist(saved.filter((x) => x.name !== s.name))}>🗑</button>
                </div>
              ))}
            </div>
          </div>

          <div className="card text-xs space-y-1.5 text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-1">💡 Como usar</div>
            <p>1. Posicione o NPC e capture a coord do player que vai interpretar</p>
            <p>2. Escreva a fala — ela aparece no chat APENAS dos players dentro do raio</p>
            <p>3. Marque <span className="chip">Esperar resposta</span> e o roteirizador captura a próxima mensagem que algum vizinho enviar no chat</p>
            <p>4. Continue compondo turns — cada um fica salvo no transcript</p>
            <p>5. Save + Export → você tem o roteiro pronto pra reusar como Storyboard / Cutscene</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function hexFromColor(code: string): string {
  const m: Record<string, string> = {
    '§0': '#000', '§1': '#0000aa', '§2': '#00aa00', '§3': '#00aaaa',
    '§4': '#aa0000', '§5': '#aa00aa', '§6': '#ffaa00', '§7': '#aaaaaa',
    '§8': '#555555', '§9': '#5555ff', '§a': '#55ff55', '§b': '#55ffff',
    '§c': '#ff5555', '§d': '#ff55ff', '§e': '#ffff55', '§f': '#fff',
  }
  return m[code] ?? '#fff'
}
