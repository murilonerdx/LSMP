import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, NightmareSequenceDto } from '../lib/api'
import { toast } from '../store/toast'
import { renderMcText, MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useEffectOptions, useParticleOptions, useEntityOptions, useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Nightmare Sequence — V2 backend-driven.
 *
 * Engine roda no backend (Spring Boot @Async). Steps são processados
 * sequencialmente lá no servidor, então a página pode fechar e o pesadelo
 * continua rodando. Active runs aparecem no painel via refetch a cada 2s.
 */

type StepType = 'tp' | 'title' | 'sound' | 'particle' | 'effect' | 'spawn_mob' | 'chat' | 'command' | 'lightning' | 'wait' | 'snapshot_inv' | 'restore_inv'
type Step = { type: StepType; [k: string]: any }

const DEFAULT_STEPS: Step[] = [
  { type: 'title', title: '§5§l...', subtitle: '§8§o— começa —', fadeIn: 10, stay: 50, fadeOut: 10 },
  { type: 'wait', durationSec: 3 },
  { type: 'chat', message: '§5§o<<um pesadelo se inicia>>' },
]

const PRESET_SEEDS: NightmareSequenceDto[] = [
  {
    id: 'nm_awakening',
    name: 'O Despertar',
    emoji: '😱',
    description: 'Player acorda em local escuro, sem inventário. Some lentamente do horror, recupera inv no fim.',
    snapshotBefore: true,
    restoreAfter: true,
    stepsJson: JSON.stringify([
      { type: 'snapshot_inv' },
      { type: 'title', title: '§0', subtitle: '§8§o...', fadeIn: 20, stay: 60, fadeOut: 0 },
      { type: 'sound', sound: 'minecraft:ambient.cave', volume: 1, pitch: 0.4 },
      { type: 'wait', durationSec: 3 },
      { type: 'command', command: 'clear @s' },
      { type: 'effect', effect: 'minecraft:darkness', durationSec: 30, amplifier: 0 },
      { type: 'effect', effect: 'minecraft:blindness', durationSec: 10, amplifier: 0 },
      { type: 'effect', effect: 'minecraft:slowness', durationSec: 20, amplifier: 2 },
      { type: 'title', title: '§5§l? ? ?', subtitle: '§8§o— onde você está? —', fadeIn: 10, stay: 60, fadeOut: 20 },
      { type: 'wait', durationSec: 5 },
      { type: 'chat', message: '§5§o<<você acordou. mas não onde dormiu.>>' },
      { type: 'sound', sound: 'minecraft:entity.warden.heartbeat', volume: 1, pitch: 0.5 },
      { type: 'wait', durationSec: 6 },
      { type: 'spawn_mob', entity: 'minecraft:zombie', count: 2, offsetX: 5, offsetY: 0, offsetZ: 5 },
      { type: 'sound', sound: 'minecraft:entity.zombie.ambient', volume: 0.8, pitch: 0.5 },
      { type: 'chat', message: '§8§o<<não está sozinho.>>' },
      { type: 'wait', durationSec: 10 },
      { type: 'effect', effect: 'minecraft:speed', durationSec: 30, amplifier: 1 },
      { type: 'chat', message: '§e§o<<corra.>>' },
      { type: 'wait', durationSec: 15 },
      { type: 'title', title: '§f', subtitle: '§7§o— você está acordando —', fadeIn: 20, stay: 60, fadeOut: 20 },
      { type: 'wait', durationSec: 4 },
      { type: 'restore_inv' },
      { type: 'sound', sound: 'minecraft:block.bell.use', volume: 1, pitch: 1.2 },
      { type: 'chat', message: '§a§l— era um sonho —' },
    ]),
  },
]

export function NightmareSequencePage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['nightmares'], queryFn: api.nightmareList, refetchInterval: 2000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 4000 })
  const players = playersQ.data ?? []
  const sequences = q.data?.sequences ?? []
  const active = q.data?.active ?? []
  const [editing, setEditing] = useState<NightmareSequenceDto | null>(null)
  const [seedTried, setSeedTried] = useState(false)

  // Seed presets na primeira vez (se DB vazio)
  useEffect(() => {
    if (q.isSuccess && sequences.length === 0 && !seedTried) {
      setSeedTried(true)
      Promise.all(PRESET_SEEDS.map((s) => api.nightmareSave(s).catch(() => null)))
        .then(() => qc.invalidateQueries({ queryKey: ['nightmares'] }))
    }
  }, [q.isSuccess, sequences.length, seedTried, qc])

  const save = useMutation({
    mutationFn: api.nightmareSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['nightmares'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({
    mutationFn: api.nightmareDelete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['nightmares'] }),
  })
  const run = useMutation({
    mutationFn: (v: { seqId: string; playerUuid: string }) => api.nightmareRun(v.seqId, v.playerUuid),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['nightmares'] }); toast.ok('😱 pesadelo iniciado') },
    onError: (e: any) => toast.err(e.message),
  })
  const cancel = useMutation({
    mutationFn: api.nightmareCancel,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['nightmares'] }),
  })

  function newSeq() {
    setEditing({
      emoji: '😱',
      name: 'Novo Pesadelo',
      description: '...',
      stepsJson: JSON.stringify(DEFAULT_STEPS),
      snapshotBefore: true,
      restoreAfter: true,
    })
  }

  function parseSteps(s: NightmareSequenceDto): Step[] {
    try { const a = JSON.parse(s.stepsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">😱 Nightmare Sequence</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — pesadelos rodam mesmo com a página fechada.
          </p>
        </div>
        <button className="btn" onClick={newSeq}>+ Novo Pesadelo</button>
      </header>

      {active.length > 0 && (
        <div className="card-glow mb-4 !border-red-400/40 !bg-red-500/5">
          <h3 className="font-bold mb-3">⚡ Pesadelos em andamento ({active.length})</h3>
          <div className="space-y-2">
            {active.map((r) => {
              const pct = r.totalSteps > 0 ? ((r.currentStep + 1) / r.totalSteps) * 100 : 0
              return (
                <div key={r.runId} className="bg-liberthia-900/40 rounded-xl p-2">
                  <div className="flex items-center gap-3 mb-1">
                    <span className="text-2xl">😱</span>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-bold truncate">{r.seqName} → <span className="text-liberthia-300/60">{r.playerName}</span></div>
                      <div className="text-[10px] text-liberthia-300/50">step {r.currentStep + 1}/{r.totalSteps} · {r.currentStepType}</div>
                    </div>
                    {r.finished
                      ? <span className="badge badge-green">✓ done</span>
                      : <button className="btn-danger btn-sm" onClick={() => cancel.mutate(r.runId)}>✕ Cancelar</button>}
                  </div>
                  <div className="h-2 bg-liberthia-900/70 rounded overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-purple-500 to-red-500 transition-all" style={{ width: `${pct}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {sequences.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">😱</div>
            <p className="text-liberthia-300/70 text-sm">Aguardando presets serem criados no DB... ou clique em "+ Novo Pesadelo".</p>
          </div>
        )}
        {sequences.map((s) => {
          const steps = parseSteps(s)
          return (
            <div key={s.id} className="card-glow">
              <div className="flex items-start gap-3 mb-2">
                <div className="text-5xl">{s.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold">{s.name}</div>
                  <div className="text-[10px] text-liberthia-300/50">{steps.length} steps</div>
                  {s.snapshotBefore && <div className="text-[10px] text-emerald-300/70">💾 snapshot+restore</div>}
                </div>
              </div>
              <p className="text-xs text-liberthia-300/60 mb-3 line-clamp-3">{s.description}</p>
              <div className="bg-liberthia-900/40 rounded p-2 mb-3 max-h-24 overflow-y-auto text-[10px] font-mono space-y-0.5">
                {steps.slice(0, 6).map((step, i) => (
                  <div key={i} className="flex gap-2">
                    <span className="text-liberthia-300/40">{i + 1}.</span>
                    <span className="text-purple-300">{step.type}</span>
                    <span className="text-liberthia-300/50 truncate">
                      {step.title ?? step.message ?? step.command ?? step.sound ?? step.effect ?? step.entity ?? step.particle ?? (step.durationSec ? `${step.durationSec}s` : '')}
                    </span>
                  </div>
                ))}
                {steps.length > 6 && <div className="text-liberthia-300/40">...mais {steps.length - 6}</div>}
              </div>

              <RunForm seq={s} players={players}
                onRun={(uuid) => run.mutate({ seqId: s.id!, playerUuid: uuid })} />
              <div className="grid grid-cols-2 gap-1 mt-2">
                <button className="btn-ghost btn-sm" onClick={() => setEditing(s)}>✎ Editar</button>
                <button className="btn-ghost btn-sm" onClick={() => del.mutate(s.id!)}>🗑</button>
              </div>
            </div>
          )
        })}
      </div>

      {editing && (
        <SeqEditor seq={editing}
          onSave={(s) => save.mutate(s, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function RunForm({ seq, players, onRun }: { seq: NightmareSequenceDto; players: any[]; onRun: (uuid: string) => void }) {
  const [target, setTarget] = useState('')
  return (
    <div className="flex gap-1">
      <select className="input text-xs flex-1" value={target} onChange={(e) => setTarget(e.target.value)}>
        <option value="">— vítima —</option>
        {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
      </select>
      <button className="btn-danger btn-sm" disabled={!target} onClick={() => onRun(target)} title={`Rodar ${seq.name}`}>😱 Rodar</button>
    </div>
  )
}

function SeqEditor({ seq, onSave, onCancel }: { seq: NightmareSequenceDto; onSave: (s: NightmareSequenceDto) => void; onCancel: () => void }) {
  const [s, setS] = useState<NightmareSequenceDto>(seq)
  // Hooks chamados UMA vez no editor (não dentro do StepEditor).
  // Com 25+ steps cada um chamando 4 useQuery, o modal levava ~2s pra
  // abrir e parecia que o botão Editar não funcionava.
  const effects = useEffectOptions()
  const particles = useParticleOptions()
  const entities = useEntityOptions()
  const sounds = useSoundOptions()

  const steps: Step[] = (() => {
    try { const a = JSON.parse(s.stepsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  })()

  function setSteps(arr: Step[]) { setS({ ...s, stepsJson: JSON.stringify(arr) }) }
  function update(i: number, p: Partial<Step>) { setSteps(steps.map((x, j) => j === i ? { ...x, ...p } : x)) }
  function moveStep(i: number, dir: -1 | 1) {
    const j = i + dir; if (j < 0 || j >= steps.length) return
    const a = [...steps]
    ;[a[i], a[j]] = [a[j], a[i]]
    setSteps(a)
  }
  function addStep(t: StepType) {
    const blank: Record<StepType, Step> = {
      tp: { type: 'tp', x: 0, y: 80, z: 0, dim: 'overworld' },
      title: { type: 'title', title: '§5§l...', subtitle: '§8§o...', fadeIn: 10, stay: 50, fadeOut: 10 },
      sound: { type: 'sound', sound: 'minecraft:ambient.cave', volume: 1, pitch: 1 },
      particle: { type: 'particle', particle: 'minecraft:smoke', count: 30, offsetY: 1 },
      effect: { type: 'effect', effect: 'minecraft:slowness', durationSec: 10, amplifier: 0 },
      spawn_mob: { type: 'spawn_mob', entity: 'minecraft:zombie', count: 1, offsetX: 4, offsetY: 0, offsetZ: 4 },
      chat: { type: 'chat', message: '§5§o<<...>>' },
      command: { type: 'command', command: 'say hello' },
      lightning: { type: 'lightning' },
      wait: { type: 'wait', durationSec: 3 },
      snapshot_inv: { type: 'snapshot_inv' },
      restore_inv: { type: 'restore_inv' },
    }
    setSteps([...steps, blank[t]])
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-4xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">😱 Editar Pesadelo</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-2">
          <input className="input text-2xl text-center" value={s.emoji} onChange={(e) => setS({ ...s, emoji: e.target.value })} />
          <input className="input font-bold" value={s.name} onChange={(e) => setS({ ...s, name: e.target.value })} />
        </div>
        <textarea className="input mb-3 text-xs" rows={2} value={s.description} onChange={(e) => setS({ ...s, description: e.target.value })} />

        <div className="flex items-center gap-2 mb-3 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => addStep('tp')}>+ tp</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('title')}>+ title</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('sound')}>+ sound</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('particle')}>+ particle</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('effect')}>+ effect</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('spawn_mob')}>+ mob</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('chat')}>+ chat</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('command')}>+ cmd</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('lightning')}>+ ⚡</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('wait')}>+ wait</button>
        </div>

        <div className="space-y-1 mb-4">
          {steps.map((st, i) => (
            <div key={i} className="bg-liberthia-900/40 rounded p-2">
              <div className="flex items-center gap-2 mb-1">
                <span className="badge badge-purple shrink-0">#{i + 1}</span>
                <span className="text-xs font-mono font-bold text-purple-300 shrink-0">{st.type}</span>
                <div className="flex-1" />
                <button className="btn-ghost btn-sm" onClick={() => moveStep(i, -1)}>↑</button>
                <button className="btn-ghost btn-sm" onClick={() => moveStep(i, 1)}>↓</button>
                <button className="btn-ghost btn-sm" onClick={() => setSteps(steps.filter((_, j) => j !== i))}>🗑</button>
              </div>
              <StepEditor step={st} onChange={(p) => update(i, p)}
                effects={effects} particles={particles} entities={entities} sounds={sounds} />
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(s)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

type Opt = { value: string; label?: string }

function StepEditor({ step, onChange, effects, particles, entities, sounds }: {
  step: Step
  onChange: (p: Partial<Step>) => void
  effects: Opt[]
  particles: Opt[]
  entities: Opt[]
  sounds: Opt[]
}) {
  switch (step.type) {
    case 'title':
      return (
        <div className="space-y-1">
          <input className="input text-xs font-mono" placeholder="title" value={step.title ?? ''} onChange={(e) => onChange({ title: e.target.value })} />
          <input className="input text-xs font-mono" placeholder="subtitle" value={step.subtitle ?? ''} onChange={(e) => onChange({ subtitle: e.target.value })} />
          <div className="text-[10px]">{renderMcText(step.title ?? '')} / {renderMcText(step.subtitle ?? '')}</div>
        </div>
      )
    case 'sound':
      return (
        <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
          <Autocomplete value={step.sound ?? ''} onChange={(v) => onChange({ sound: v })} options={sounds} placeholder="minecraft:ambient..." />
          <input type="number" step={0.1} className="input text-xs" placeholder="vol" value={step.volume ?? 1} onChange={(e) => onChange({ volume: Number(e.target.value) })} />
          <input type="number" step={0.1} className="input text-xs" placeholder="pitch" value={step.pitch ?? 1} onChange={(e) => onChange({ pitch: Number(e.target.value) })} />
        </div>
      )
    case 'particle':
      return (
        <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
          <Autocomplete value={step.particle ?? ''} onChange={(v) => onChange({ particle: v })} options={particles} placeholder="minecraft:smoke" />
          <input type="number" className="input text-xs" placeholder="count" value={step.count ?? 30} onChange={(e) => onChange({ count: Number(e.target.value) })} />
          <input type="number" className="input text-xs" placeholder="offY" value={step.offsetY ?? 1} onChange={(e) => onChange({ offsetY: Number(e.target.value) })} />
        </div>
      )
    case 'effect':
      return (
        <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
          <Autocomplete value={step.effect ?? ''} onChange={(v) => onChange({ effect: v })} options={effects} placeholder="minecraft:slowness" />
          <input type="number" className="input text-xs" placeholder="durSec" value={step.durationSec ?? 10} onChange={(e) => onChange({ durationSec: Number(e.target.value) })} />
          <input type="number" className="input text-xs" placeholder="amp" value={step.amplifier ?? 0} onChange={(e) => onChange({ amplifier: Number(e.target.value) })} />
        </div>
      )
    case 'spawn_mob':
      return (
        <div className="space-y-1">
          <Autocomplete value={step.entity ?? ''} onChange={(v) => onChange({ entity: v })} options={entities} placeholder="minecraft:zombie" />
          <div className="grid grid-cols-4 gap-1">
            <input type="number" className="input text-xs" placeholder="count" value={step.count ?? 1} onChange={(e) => onChange({ count: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="offX" value={step.offsetX ?? 0} onChange={(e) => onChange({ offsetX: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="offY" value={step.offsetY ?? 0} onChange={(e) => onChange({ offsetY: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="offZ" value={step.offsetZ ?? 0} onChange={(e) => onChange({ offsetZ: Number(e.target.value) })} />
          </div>
        </div>
      )
    case 'chat':
      return <MinecraftFormatter value={step.message ?? ''} onChange={(v) => onChange({ message: v })} rows={1} maxChars={200} showCounter={false} />
    case 'command':
      return <input className="input text-xs font-mono" value={step.command ?? ''} onChange={(e) => onChange({ command: e.target.value })} placeholder="say hello (sem /)" />
    case 'wait':
      return <input type="number" className="input text-xs" placeholder="durSec" value={step.durationSec ?? 1} onChange={(e) => onChange({ durationSec: Number(e.target.value) })} />
    case 'lightning':
      return <span className="text-xs text-liberthia-300/50">— sem parâmetros —</span>
    case 'tp':
      return (
        <div className="space-y-1">
          <div className="grid grid-cols-4 gap-1">
            <input type="number" className="input text-xs" placeholder="x" value={step.x ?? 0} onChange={(e) => onChange({ x: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="y" value={step.y ?? 0} onChange={(e) => onChange({ y: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="z" value={step.z ?? 0} onChange={(e) => onChange({ z: Number(e.target.value) })} />
            <input className="input text-xs" placeholder="dim" value={step.dim ?? 'overworld'} onChange={(e) => onChange({ dim: e.target.value })} />
          </div>
          <PlayerPosPicker onPick={(p) => onChange({ x: Math.floor(p.x), y: Math.floor(p.y), z: Math.floor(p.z), dim: p.dim })} />
        </div>
      )
    case 'snapshot_inv':
      return <span className="text-xs text-amber-300/70">⚠ snapshot ainda não implementado no backend (skip silencioso)</span>
    case 'restore_inv':
      return <span className="text-xs text-amber-300/70">⚠ restore ainda não implementado no backend (skip silencioso)</span>
  }
  return null
}
