import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, CutsceneDto } from '../lib/api'
import { toast } from '../store/toast'
import { renderMcText, MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useSoundOptions, useParticleOptions, useEffectOptions, useEntityOptions } from '../lib/mcAutocomplete'

/**
 * Scheduled Cutscenes — backend-driven.
 *
 * Diferente da CinematicPage (que roda on-demand quando você clica), esta página
 * AGENDA cutscenes pra rodar SEM o painel estar aberto.
 *
 * Modos:
 *  - once: roda 1 vez no horário específico
 *  - daily: roda todo dia em HH:MM
 *  - interval: roda a cada N segundos
 *
 * Steps disponíveis: tp, title, sound, particle, effect, spawn_mob, chat, command, lightning, wait
 * Target selector: uuid do player, "@a" (todos online), "@first" (primeiro online)
 */

type StepType = 'tp' | 'title' | 'sound' | 'particle' | 'effect' | 'spawn_mob' | 'chat' | 'command' | 'lightning' | 'wait'
type Step = { type: StepType; [k: string]: any }

function localInput(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function ScheduledCutscenesPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['cutscenes'], queryFn: api.cutscenesList, refetchInterval: 5000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const cutscenes = q.data?.cutscenes ?? []
  const [editing, setEditing] = useState<CutsceneDto | null>(null)
  const [now, setNow] = useState(Date.now())

  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 10000); return () => clearInterval(t) }, [])

  const save = useMutation({
    mutationFn: api.cutscenesSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cutscenes'] }); toast.ok('✓ salvo') },
  })
  const del = useMutation({ mutationFn: api.cutscenesDelete, onSuccess: () => qc.invalidateQueries({ queryKey: ['cutscenes'] }) })
  const toggle = useMutation({ mutationFn: api.cutscenesToggle, onSuccess: () => qc.invalidateQueries({ queryKey: ['cutscenes'] }) })
  const runNow = useMutation({ mutationFn: api.cutscenesRunNow, onSuccess: () => toast.ok('▶ executando') })

  function newCutscene() {
    setEditing({
      emoji: '🎬', name: 'Nova Cutscene', description: '...',
      stepsJson: JSON.stringify([
        { type: 'title', title: '§5§l...', subtitle: '§8§o— começa —', fadeIn: 10, stay: 50, fadeOut: 10 },
        { type: 'wait', durationSec: 3 },
        { type: 'chat', message: '§5§o<<um pesadelo se inicia>>' },
      ]),
      targetSelector: '@a',
      scheduledAt: null,
      scheduleMode: 'once',
      enabled: false,
    })
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⏰ Scheduled Cutscenes</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Agenda cutscenes pra rodar em data/hora, diariamente, ou em intervalo. <span className="text-emerald-300 font-bold">Roda no backend mesmo com painel fechado.</span>
          </p>
        </div>
        <button className="btn" onClick={newCutscene}>+ Nova Cutscene</button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {cutscenes.map((c) => {
          const steps = parseSteps(c.stepsJson)
          const nextRun = computeNextRun(c, now)
          return (
            <div key={c.id} className={`card-glow ${c.enabled ? '!border-emerald-400/40' : ''}`}>
              <div className="flex items-start gap-3 mb-2">
                <div className="text-4xl">{c.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold">{c.name}</div>
                  <div className="text-[10px] text-liberthia-300/60">{steps.length} steps · target: {c.targetSelector}</div>
                  <div className="text-[10px] text-liberthia-300/40">
                    {c.scheduleMode === 'once' && (c.scheduledAt ? new Date(c.scheduledAt).toLocaleString() : 'sem horário')}
                    {c.scheduleMode === 'daily' && `diário ${String(c.dailyHour ?? 0).padStart(2, '0')}:${String(c.dailyMinute ?? 0).padStart(2, '0')}`}
                    {c.scheduleMode === 'interval' && `a cada ${c.intervalSec}s`}
                  </div>
                  {nextRun && c.enabled && <div className="text-[10px] text-amber-300 font-bold">⏰ próx: {nextRun}</div>}
                  {c.runCount != null && c.runCount > 0 && <div className="text-[10px] text-liberthia-300/40">▶ {c.runCount}x · último: {c.lastRunAt ? new Date(c.lastRunAt).toLocaleString() : '-'}</div>}
                </div>
              </div>
              <p className="text-xs text-liberthia-300/60 mb-3 line-clamp-2">{c.description}</p>
              <div className="grid grid-cols-4 gap-1">
                <button className={c.enabled ? 'btn-success btn-sm' : 'btn-amber btn-sm'} onClick={() => toggle.mutate(c.id!)}>{c.enabled ? '⏸' : '▶'}</button>
                <button className="btn-ghost btn-sm" title="Run agora" onClick={() => runNow.mutate(c.id!)}>⚡</button>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(c)}>✎</button>
                <button className="btn-ghost btn-sm" onClick={() => del.mutate(c.id!)}>🗑</button>
              </div>
            </div>
          )
        })}
        {cutscenes.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">⏰</div>
            <p className="text-liberthia-300/70 text-sm">Nenhuma cutscene agendada.</p>
          </div>
        )}
      </div>

      {editing && (
        <CutsceneEditor cs={editing} players={players}
          onSave={(c) => save.mutate(c, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function parseSteps(json: string): Step[] {
  try { const a = JSON.parse(json); return Array.isArray(a) ? a : [] } catch { return [] }
}

function computeNextRun(c: CutsceneDto, now: number): string | null {
  if (c.scheduleMode === 'once' && c.scheduledAt) {
    const t = new Date(c.scheduledAt).getTime()
    if (t > now) return new Date(t).toLocaleString()
    return null
  }
  if (c.scheduleMode === 'daily' && c.dailyHour != null && c.dailyMinute != null) {
    const d = new Date()
    d.setHours(c.dailyHour, c.dailyMinute, 0, 0)
    if (d.getTime() <= now) d.setDate(d.getDate() + 1)
    return d.toLocaleString()
  }
  if (c.scheduleMode === 'interval' && c.intervalSec) {
    const last = c.lastRunAt ? new Date(c.lastRunAt).getTime() : now
    const next = last + c.intervalSec * 1000
    return next > now ? new Date(next).toLocaleString() : 'agora'
  }
  return null
}

function CutsceneEditor({ cs, players, onSave, onCancel }: { cs: CutsceneDto; players: any[]; onSave: (c: CutsceneDto) => void; onCancel: () => void }) {
  const [c, setC] = useState<CutsceneDto>(cs)
  // Hooks aqui em cima — não dentro do StepEditor — pra evitar N×4 calls de
  // useQuery quando a cutscene tem muitos steps (cada chamada gera subscription
  // e re-render). Aberto com 25 steps subia de ~2s pra instantâneo.
  const sounds = useSoundOptions()
  const particles = useParticleOptions()
  const effects = useEffectOptions()
  const entities = useEntityOptions()
  const steps: Step[] = parseSteps(c.stepsJson)

  function setSteps(s: Step[]) {
    setC({ ...c, stepsJson: JSON.stringify(s) })
  }
  function updateStep(i: number, p: Partial<Step>) {
    setSteps(steps.map((x, j) => j === i ? { ...x, ...p } : x))
  }
  function move(i: number, dir: -1 | 1) {
    const j = i + dir; if (j < 0 || j >= steps.length) return
    const arr = [...steps]
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
    setSteps(arr)
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
    }
    setSteps([...steps, blank[t]])
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-4xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⏰ Editar Cutscene Agendada</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-2">
          <input className="input text-2xl text-center" value={c.emoji} onChange={(e) => setC({ ...c, emoji: e.target.value })} />
          <input className="input font-bold" value={c.name} onChange={(e) => setC({ ...c, name: e.target.value })} />
        </div>
        <textarea className="input mb-3 text-xs" rows={2} value={c.description} onChange={(e) => setC({ ...c, description: e.target.value })} />

        <h4 className="font-bold text-sm mb-1">🎯 Target</h4>
        <select className="input mb-3" value={c.targetSelector} onChange={(e) => setC({ ...c, targetSelector: e.target.value })}>
          <option value="@a">@a (todos online)</option>
          <option value="@first">@first (primeiro online)</option>
          {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
        </select>

        <h4 className="font-bold text-sm mb-1">⏰ Schedule</h4>
        <select className="input mb-3" value={c.scheduleMode} onChange={(e) => setC({ ...c, scheduleMode: e.target.value as any })}>
          <option value="once">once (uma vez em data/hora)</option>
          <option value="daily">daily (todo dia HH:MM)</option>
          <option value="interval">interval (a cada N segundos)</option>
        </select>

        {c.scheduleMode === 'once' && (
          <div className="mb-3">
            <label className="label">Data/Hora</label>
            <input type="datetime-local" className="input" value={localInput(c.scheduledAt)}
              onChange={(e) => setC({ ...c, scheduledAt: e.target.value ? new Date(e.target.value).toISOString() : null })} />
          </div>
        )}
        {c.scheduleMode === 'daily' && (
          <div className="grid grid-cols-2 gap-2 mb-3">
            <div><label className="label">Hora (0-23)</label><input type="number" className="input" value={c.dailyHour ?? 0} min={0} max={23} onChange={(e) => setC({ ...c, dailyHour: Number(e.target.value) })} /></div>
            <div><label className="label">Minuto (0-59)</label><input type="number" className="input" value={c.dailyMinute ?? 0} min={0} max={59} onChange={(e) => setC({ ...c, dailyMinute: Number(e.target.value) })} /></div>
          </div>
        )}
        {c.scheduleMode === 'interval' && (
          <div className="mb-3">
            <label className="label">A cada (segundos)</label>
            <input type="number" className="input" value={c.intervalSec ?? 60} min={5} onChange={(e) => setC({ ...c, intervalSec: Number(e.target.value) })} />
          </div>
        )}

        <h4 className="font-bold text-sm mb-1 mt-4">🎬 Steps ({steps.length})</h4>
        <div className="flex flex-wrap gap-1 mb-2">
          <button className="btn-ghost btn-sm" onClick={() => addStep('title')}>+ title</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('sound')}>+ sound</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('particle')}>+ particle</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('effect')}>+ effect</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('spawn_mob')}>+ mob</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('chat')}>+ chat</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('command')}>+ cmd</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('lightning')}>+ ⚡</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('wait')}>+ wait</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('tp')}>+ tp</button>
        </div>

        <div className="space-y-1 mb-4">
          {steps.map((st, i) => (
            <div key={i} className="bg-liberthia-900/40 rounded p-2">
              <div className="flex items-center gap-2 mb-1">
                <span className="badge badge-purple shrink-0">#{i + 1}</span>
                <span className="text-xs font-mono font-bold text-purple-300 shrink-0">{st.type}</span>
                <div className="flex-1" />
                <button className="btn-ghost btn-sm" onClick={() => move(i, -1)}>↑</button>
                <button className="btn-ghost btn-sm" onClick={() => move(i, 1)}>↓</button>
                <button className="btn-ghost btn-sm" onClick={() => setSteps(steps.filter((_, j) => j !== i))}>🗑</button>
              </div>
              <StepEditor step={st} onChange={(p) => updateStep(i, p)}
                sounds={sounds} particles={particles} effects={effects} entities={entities} />
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(c)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

type Opt = { value: string; label?: string }

function StepEditor({ step, onChange, sounds, particles, effects, entities }: {
  step: Step
  onChange: (p: Partial<Step>) => void
  sounds: Opt[]
  particles: Opt[]
  effects: Opt[]
  entities: Opt[]
}) {
  switch (step.type) {
    case 'tp':
      return (
        <div className="space-y-1">
          <div className="grid grid-cols-4 gap-1">
            <input type="number" className="input text-xs" placeholder="x" value={step.x ?? 0} onChange={(e) => onChange({ x: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="y" value={step.y ?? 0} onChange={(e) => onChange({ y: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="z" value={step.z ?? 0} onChange={(e) => onChange({ z: Number(e.target.value) })} />
            <select className="input text-xs" value={step.dim ?? 'overworld'} onChange={(e) => onChange({ dim: e.target.value })}>
              <option value="overworld">overworld</option><option value="the_nether">nether</option><option value="the_end">end</option>
            </select>
          </div>
          <PlayerPosPicker onPick={(p) => onChange({ x: Math.floor(p.x), y: Math.floor(p.y), z: Math.floor(p.z), dim: p.dim })} label="📍 usar pos do player" />
        </div>
      )
    case 'title':
      return (
        <div className="space-y-1">
          <input className="input text-xs font-mono" placeholder="title" value={step.title ?? ''} onChange={(e) => onChange({ title: e.target.value })} />
          <input className="input text-xs font-mono" placeholder="subtitle" value={step.subtitle ?? ''} onChange={(e) => onChange({ subtitle: e.target.value })} />
          <div className="grid grid-cols-3 gap-1">
            <input type="number" className="input text-xs" placeholder="fadeIn" value={step.fadeIn ?? 10} onChange={(e) => onChange({ fadeIn: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="stay" value={step.stay ?? 50} onChange={(e) => onChange({ stay: Number(e.target.value) })} />
            <input type="number" className="input text-xs" placeholder="fadeOut" value={step.fadeOut ?? 10} onChange={(e) => onChange({ fadeOut: Number(e.target.value) })} />
          </div>
          <div className="text-[10px] mt-1">{renderMcText(step.title ?? '')} / {renderMcText(step.subtitle ?? '')}</div>
        </div>
      )
    case 'sound':
      return (
        <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
          <Autocomplete value={step.sound ?? ''} onChange={(v) => onChange({ sound: v })} options={sounds} placeholder="minecraft:ambient.cave" />
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
      return <input className="input text-xs font-mono" placeholder="say hello (sem /)" value={step.command ?? ''} onChange={(e) => onChange({ command: e.target.value })} />
    case 'lightning':
      return <span className="text-xs text-liberthia-300/50">— sem parâmetros —</span>
    case 'wait':
      return <input type="number" className="input text-xs" placeholder="durSec" value={step.durationSec ?? 1} onChange={(e) => onChange({ durationSec: Number(e.target.value) })} />
  }
  return null
}
