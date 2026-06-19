import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, MC_COLORS } from '../components/MinecraftFormatter'
import { useKvState } from '../lib/kvState'

/**
 * Dialog system: NPC fala em sequência via subtitle/actionbar.
 * Suporta efeito typewriter (caractere por caractere) e voz com pitch dependendo do speaker.
 */

type Line = { speaker: string; text: string; speakerColor?: string; soundPitch?: number; pauseMs?: number; mode?: 'subtitle' | 'actionbar' | 'chat' }
type Dialog = { name: string; lines: Line[]; updated: number }

const VOICE_SOUND = 'minecraft:block.note_block.bit'

export function DialogPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState<string>('all')
  const [name, setName] = useState('intro')
  const [lines, setLines] = useState<Line[]>(defaultLines())
  const [running, setRunning] = useState(false)
  const cancelRef = useRef(false)
  const [saved, setSaved] = useKvState<Dialog[]>('dialogs', [])
  const [typewriter, setTypewriter] = useState(true)
  const [speakerVoice, setSpeakerVoice] = useState(true)

  function persist(list: Dialog[]) {
    setSaved(list)
  }

  function save() {
    persist([{ name, lines, updated: Date.now() }, ...saved.filter((s) => s.name !== name)])
    toast.ok('Saved')
  }

  function load(d: Dialog) { setName(d.name); setLines(d.lines) }

  async function play() {
    if (running || lines.length === 0) return
    setRunning(true); cancelRef.current = false
    try {
      const targets = target === 'all' ? players.map((p) => p.uuid) : [target]
      for (const line of lines) {
        if (cancelRef.current) break
        const color = line.speakerColor ?? '§e'
        const speakerLabel = `${color}[${line.speaker}]§r `
        if (typewriter) {
          for (let i = 1; i <= line.text.length; i++) {
            if (cancelRef.current) break
            const partial = speakerLabel + '§f' + line.text.slice(0, i)
            for (const u of targets) {
              if (line.mode === 'chat') {
                // Chat só rola no final pra evitar spam
              } else if (line.mode === 'subtitle') {
                api.title(u, ' ', partial, 0, 80, 0).catch(() => {})
              } else {
                // actionbar via title cmd vanilla
                api.command(`title ${nameOf(u, players)} actionbar {"text":${JSON.stringify(partial)}}`).catch(() => {})
              }
            }
            if (speakerVoice && i % 2 === 0) {
              for (const u of targets) {
                api.sound(u, VOICE_SOUND, 0.35, line.soundPitch ?? 1).catch(() => {})
              }
            }
            await new Promise((r) => setTimeout(r, 35))
          }
          if (line.mode === 'chat') {
            for (const u of targets) {
              await api.command(`tellraw ${nameOf(u, players)} {"text":${JSON.stringify(speakerLabel + line.text)}}`)
            }
          }
        } else {
          const full = speakerLabel + '§f' + line.text
          for (const u of targets) {
            if (line.mode === 'chat') await api.command(`tellraw ${nameOf(u, players)} {"text":${JSON.stringify(full)}}`)
            else if (line.mode === 'subtitle') await api.title(u, ' ', full, 4, 50, 6)
            else await api.command(`title ${nameOf(u, players)} actionbar {"text":${JSON.stringify(full)}}`)
          }
        }
        await new Promise((r) => setTimeout(r, line.pauseMs ?? 1500))
      }
    } finally {
      setRunning(false)
    }
  }

  function update(i: number, p: Partial<Line>) {
    setLines((l) => l.map((x, j) => j === i ? { ...x, ...p } : x))
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">💭 Dialog Director</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Diálogos de NPC com efeito typewriter + voz (block.note_block.bit). Subtitle, actionbar ou chat.
          </p>
        </div>
        <div className="flex gap-2">
          {!running && <button className="btn-success" onClick={play}>▶ Play</button>}
          {running && <button className="btn-danger" onClick={() => { cancelRef.current = true }}>⏹ Stop</button>}
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-4">
          <div className="card-glow">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mb-3">
              <select className="input" value={target} onChange={(e) => setTarget(e.target.value)}>
                <option value="all">Todos players</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
              <div className="flex gap-2">
                <button className="btn-cyan flex-1" onClick={save}>💾 Save</button>
                <button className="btn-ghost" onClick={() => setLines([...lines, { speaker: 'NPC', text: 'novo' }])}>+ Linha</button>
              </div>
            </div>
            <div className="flex gap-3 text-xs">
              <label className="flex items-center gap-1"><input type="checkbox" checked={typewriter} onChange={(e) => setTypewriter(e.target.checked)} /> Typewriter</label>
              <label className="flex items-center gap-1"><input type="checkbox" checked={speakerVoice} onChange={(e) => setSpeakerVoice(e.target.checked)} /> Voz (pitch)</label>
            </div>
          </div>

          <div className="card-glow">
            <h3 className="font-bold mb-3">📜 Roteiro</h3>
            <div className="space-y-2">
              {lines.map((l, i) => (
                <div key={i} className="border border-liberthia-500/20 rounded-xl p-3 bg-liberthia-900/40">
                  <div className="flex gap-2 items-center mb-2 flex-wrap">
                    <input className="input text-sm w-40" placeholder="speaker" value={l.speaker}
                           onChange={(e) => update(i, { speaker: e.target.value })} />
                    {/* Color picker — clicar no swatch troca a cor do speaker */}
                    <div className="flex gap-0.5 p-1 rounded-lg bg-liberthia-900/60 border border-liberthia-500/20">
                      {MC_COLORS.map((c) => (
                        <button key={c.code} type="button" title={c.label}
                          onClick={() => update(i, { speakerColor: c.code })}
                          className={`w-5 h-5 rounded border transition ${l.speakerColor === c.code ? 'ring-2 ring-white scale-110' : 'border-white/10'}`}
                          style={{ background: c.hex }}
                        />
                      ))}
                    </div>
                    <select className="input text-xs w-32" value={l.mode ?? 'subtitle'} onChange={(e) => update(i, { mode: e.target.value as any })}>
                      <option value="subtitle">subtitle</option>
                      <option value="actionbar">actionbar</option>
                      <option value="chat">chat</option>
                    </select>
                    <input type="number" className="input w-20 text-xs" placeholder="pitch" step={0.1} min={0.5} max={2}
                           value={l.soundPitch ?? 1} onChange={(e) => update(i, { soundPitch: Number(e.target.value) })} />
                    <button className="btn-ghost btn-sm" onClick={() => setLines((ls) => ls.filter((_, j) => j !== i))}>🗑</button>
                  </div>
                  <MinecraftFormatter
                    value={l.text}
                    onChange={(v) => update(i, { text: v })}
                    rows={2}
                    placeholder="Olá viajante..."
                    showCounter={false}
                  />
                  <div className="flex items-center gap-2 mt-2">
                    <span className="label text-xs">Pausa após:</span>
                    <input type="range" min={0} max={5000} step={100} className="flex-1"
                           value={l.pauseMs ?? 1500} onChange={(e) => update(i, { pauseMs: Number(e.target.value) })} />
                    <span className="text-xs font-mono w-16 text-right">{((l.pauseMs ?? 1500) / 1000).toFixed(1)}s</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">💾 Salvos</h3>
            {saved.map((s) => (
              <div key={s.name} className="flex items-center gap-1 mb-1">
                <button className="btn-ghost flex-1 text-left text-xs" onClick={() => load(s)}>
                  {s.name} <span className="opacity-50">({s.lines.length})</span>
                </button>
                <button className="btn-ghost btn-sm" onClick={() => persist(saved.filter((x) => x.name !== s.name))}>🗑</button>
              </div>
            ))}
          </div>
          <div className="card text-xs text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Dicas</div>
            <p>• <b>Swatch</b> ao lado do speaker → cor do nome</p>
            <p>• <b>Toolbar do texto</b> → cor + B/I/U/S/M/R</p>
            <p>• Selecione texto + click <b>B</b> → envolve <span className="chip">§l...§r</span></p>
          </div>
        </div>
      </div>
    </div>
  )
}

function defaultLines(): Line[] {
  return [
    { speaker: 'Mira', speakerColor: '§e', text: 'Olá viajante... Você sente isso também?', mode: 'subtitle', pauseMs: 2500, soundPitch: 1.2 },
    { speaker: 'Mira', speakerColor: '§e', text: 'A matéria escura está despertando outra vez.', mode: 'subtitle', pauseMs: 2500, soundPitch: 1.2 },
    { speaker: 'Você', speakerColor: '§b', text: '...', mode: 'subtitle', pauseMs: 1500, soundPitch: 0.9 },
    { speaker: 'Mira', speakerColor: '§e', text: 'Vai precisar de força. E sorte.', mode: 'chat', pauseMs: 2000, soundPitch: 1.2 },
  ]
}

function nameOf(uuid: string, list: { uuid: string; name: string }[]) {
  return list.find((p) => p.uuid === uuid)?.name ?? `@a[uuid=${uuid}]`
}
