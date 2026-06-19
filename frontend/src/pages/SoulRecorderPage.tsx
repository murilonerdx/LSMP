import { useEffect, useRef, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Soul Recorder — grava posição do player em intervalos por N segundos.
 * Replay: cria armor stand fantasma que percorre o path gravado.
 */

type Frame = { x: number; y: number; z: number; yaw: number; t: number }
type Recording = { name: string; player: string; frames: Frame[]; updated: number }


export function SoulRecorderPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 2000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [recording, setRecording] = useState(false)
  const [playing, setPlaying] = useState(false)
  const [frames, setFrames] = useState<Frame[]>([])
  const [recDuration, setRecDuration] = useState(30)
  const [interval, setIntervalMs] = useState(200)
  const [startTs, setStartTs] = useState(0)
  const [saved, setSaved] = useKvState<Recording[]>('souls', [])
  const [recName, setRecName] = useState('soul_1')
  const recIntervalRef = useRef<number | null>(null)
  const playIntervalRef = useRef<number | null>(null)
  const playIdRef = useRef('')

  useEffect(() => () => {
    if (recIntervalRef.current) clearInterval(recIntervalRef.current)
    if (playIntervalRef.current) clearInterval(playIntervalRef.current)
    if (playIdRef.current) api.command(`kill @e[tag=liberthia_soul_${playIdRef.current}]`, 'soul').catch(() => {})
  }, [])

  async function startRec() {
    if (!target) { toast.err('Selecione player'); return }
    setRecording(true); setFrames([]); setStartTs(Date.now())
    recIntervalRef.current = window.setInterval(async () => {
      const live = await api.players().catch(() => [])
      const p = live.find((x: any) => x.uuid === target)
      if (!p) return
      setFrames((cur) => {
        const t = Date.now() - startTs
        if (t > recDuration * 1000) {
          if (recIntervalRef.current) clearInterval(recIntervalRef.current)
          recIntervalRef.current = null
          setRecording(false)
          toast.ok(`✓ Gravado: ${cur.length + 1} frames`)
          return [...cur, { x: p.position.x, y: p.position.y, z: p.position.z, yaw: p.position.yaw, t }]
        }
        return [...cur, { x: p.position.x, y: p.position.y, z: p.position.z, yaw: p.position.yaw, t }]
      })
    }, interval)
  }

  function stopRec() {
    if (recIntervalRef.current) clearInterval(recIntervalRef.current)
    recIntervalRef.current = null
    setRecording(false)
    toast.ok(`✓ Gravado: ${frames.length} frames`)
  }

  async function play() {
    if (frames.length === 0) { toast.err('Sem gravação'); return }
    setPlaying(true)
    playIdRef.current = `r${Date.now().toString(36)}`
    const tag = `liberthia_soul_${playIdRef.current}`
    const player = players.find((p) => p.uuid === target)
    const playerName = player?.name ?? '???'

    try {
      const nameComp = JSON.stringify({ text: `§7§o${playerName} (fantasma)` }).replace(/'/g, "\\'")
      // Armor stand simples com player_head
      const headTag = `tag:{SkullOwner:"${playerName}"}`
      const nbt = [
        `Invulnerable:1b`, `NoGravity:1b`, `Marker:1b`,
        `CustomName:'${nameComp}'`,
        `CustomNameVisible:0b`,
        `Tags:["liberthia_soul","${tag}"]`,
        `ArmorItems:[{},{},{},{id:"minecraft:player_head",Count:1b,${headTag}}]`,
      ].join(',')
      const first = frames[0]
      await api.command(`summon armor_stand ${first.x.toFixed(2)} ${first.y.toFixed(2)} ${first.z.toFixed(2)} {${nbt}}`, 'soul')
    } catch (e: any) {
      toast.err(e.message); setPlaying(false); return
    }

    let idx = 0
    playIntervalRef.current = window.setInterval(async () => {
      if (idx >= frames.length) {
        if (playIntervalRef.current) clearInterval(playIntervalRef.current)
        playIntervalRef.current = null
        // Despawn no fim
        if (playIdRef.current) {
          await api.command(`kill @e[tag=liberthia_soul_${playIdRef.current}]`, 'soul').catch(() => {})
        }
        setPlaying(false)
        toast.ok('✓ Replay terminou')
        return
      }
      const f = frames[idx]
      await api.command(`tp @e[tag=liberthia_soul_${playIdRef.current},limit=1] ${f.x.toFixed(2)} ${f.y.toFixed(2)} ${f.z.toFixed(2)} ${f.yaw.toFixed(1)} 0`, 'soul').catch(() => {})
      // Trail de partículas
      api.particle('minecraft:soul_fire_flame', f.x, f.y + 0.5, f.z, 2).catch(() => {})
      idx++
    }, interval)
  }

  function stopPlay() {
    if (playIntervalRef.current) clearInterval(playIntervalRef.current)
    playIntervalRef.current = null
    if (playIdRef.current) {
      api.command(`kill @e[tag=liberthia_soul_${playIdRef.current}]`, 'soul').catch(() => {})
    }
    setPlaying(false)
  }

  function save() {
    if (frames.length === 0 || !recName) return
    const player = players.find((p) => p.uuid === target)
    const playerName = player?.name ?? '???'
    setSaved((cur) => [...cur, { name: recName, player: playerName, frames: [...frames], updated: Date.now() }])
    toast.ok(`💾 Salvo: ${recName}`)
  }

  function load(r: Recording) {
    setFrames(r.frames)
    setRecName(r.name)
    toast.info(`Carregado: ${r.name} (${r.frames.length} frames)`)
  }

  return (
    <div className="route-fade max-w-[1300px]">
      <header className="mb-6">
        <h1 className="page-title">👻 Soul Recorder</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Grava movimento do player. Replay como armor stand fantasma com a face do player original.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3">🔴 Gravar</h3>
            <select className="input mb-3" value={target} onChange={(e) => setTarget(e.target.value)} disabled={recording || playing}>
              <option value="">— player —</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
            <div className="grid grid-cols-2 gap-3 mb-3 text-xs">
              <div>
                <label className="label">Duração: {recDuration}s</label>
                <input type="range" min={5} max={120} value={recDuration} onChange={(e) => setRecDuration(Number(e.target.value))} disabled={recording} className="w-full" />
              </div>
              <div>
                <label className="label">Interval: {interval}ms</label>
                <input type="range" min={100} max={500} step={50} value={interval} onChange={(e) => setIntervalMs(Number(e.target.value))} disabled={recording} className="w-full" />
              </div>
            </div>
            {!recording ? (
              <button className="btn-danger w-full" onClick={startRec} disabled={!target || playing}>🔴 Iniciar gravação</button>
            ) : (
              <button className="btn-danger w-full pulse-glow" onClick={stopRec}>⏹ Parar ({frames.length} frames)</button>
            )}
          </div>

          {frames.length > 0 && !recording && (
            <div className="card-glow">
              <h3 className="font-bold mb-3">▶ Replay</h3>
              <div className="text-xs text-liberthia-300/70 mb-3">
                <span className="chip">{frames.length} frames</span>{' '}
                <span className="chip">{(frames[frames.length - 1]?.t / 1000).toFixed(1)}s</span>{' '}
                <span className="chip">interval {interval}ms</span>
              </div>
              {!playing ? (
                <button className="btn w-full mb-2" onClick={play}>▶ Replay com fantasma</button>
              ) : (
                <button className="btn-danger w-full pulse-glow mb-2" onClick={stopPlay}>⏹ Parar replay</button>
              )}
              <div className="grid grid-cols-2 gap-2">
                <input className="input text-xs" value={recName} onChange={(e) => setRecName(e.target.value)} placeholder="Nome" />
                <button className="btn-cyan btn-sm" onClick={save}>💾 Salvar</button>
              </div>
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">💾 Gravações ({saved.length})</h3>
          {saved.length === 0 && <p className="text-xs italic text-liberthia-300/50">Nenhuma</p>}
          <div className="space-y-1 max-h-96 overflow-y-auto">
            {saved.map((r, i) => (
              <div key={i} className="flex items-center gap-1 p-1 rounded bg-liberthia-900/40">
                <button className="btn-ghost btn-sm flex-1 text-left text-xs" onClick={() => load(r)}>
                  <div className="font-bold">{r.name}</div>
                  <div className="text-[10px] opacity-60">{r.player} · {r.frames.length} fr</div>
                </button>
                <button className="btn-ghost btn-sm" onClick={() => setSaved((cur) => cur.filter((_, j) => j !== i))}>🗑</button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
