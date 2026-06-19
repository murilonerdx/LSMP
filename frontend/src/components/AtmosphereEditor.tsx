import { useState } from 'react'
import { Atmosphere, AtmosphereStep, defaultStep, stepLabel } from '../lib/atmosphere'
import { MinecraftFormatter, MC_COLORS } from './MinecraftFormatter'
import { NumInput } from './NumInput'
import { Autocomplete } from './Autocomplete'
import { useSoundOptions, useParticleOptions, useEffectOptions } from '../lib/mcAutocomplete'

/**
 * Editor de Atmosphere — modal completo pra criar/editar sequências complexas.
 * Suporta todos os step kinds incluindo loop e random (compostos).
 */

const EMOJIS = ['🎬', '👻', '🜨', '🩸', '⚡', '🌙', '☀', '✨', '🔮', '⚖', '⛪', '🦠', '🌪', '🌌', '🩻', '🜲', '🕯', '🪐', '🌋', '🌊', '🪶', '⚱', '🎭', '👁', '💀', '🪨', '🌳', '🎉']

const SOUND_PRESETS_BY_CAT: Record<string, [string, string][]> = {
  horror: [
    ['minecraft:entity.ghast.warn', '👻 Ghast warn'],
    ['minecraft:entity.elder_guardian.curse', '👁 Elder curse'],
    ['minecraft:entity.warden.heartbeat', '💓 Warden heart'],
    ['minecraft:entity.warden.sonic_boom', '💥 Sonic boom'],
    ['minecraft:block.sculk.charge', '🜨 Sculk charge'],
    ['minecraft:entity.player.death', '💀 Death'],
    ['minecraft:ambient.cave', '🕳 Cave'],
  ],
  divine: [
    ['minecraft:block.bell.use', '🔔 Bell'],
    ['minecraft:block.amethyst_block.chime', '💎 Chime'],
    ['minecraft:block.beacon.activate', '✨ Beacon'],
    ['minecraft:entity.allay.ambient_with_item', '🧚 Allay'],
    ['minecraft:entity.player.levelup', '⭐ LevelUp'],
  ],
  magical: [
    ['minecraft:block.amethyst_block.resonate', '💎 Resonate'],
    ['minecraft:entity.enderman.teleport', '🌀 Teleport'],
    ['minecraft:block.portal.ambient', '🌀 Portal'],
    ['minecraft:block.respawn_anchor.charge', '🔥 Charge'],
    ['minecraft:entity.evoker.cast_spell', '🧙 Spell'],
  ],
  apocalyptic: [
    ['minecraft:entity.wither.spawn', '☠ Wither'],
    ['minecraft:entity.ender_dragon.growl', '🐉 Dragon'],
    ['minecraft:entity.lightning_bolt.thunder', '⛈ Thunder'],
    ['minecraft:entity.tnt.primed', '💣 TNT'],
    ['minecraft:entity.generic.explode', '💥 Explode'],
  ],
}

const PARTICLE_PRESETS = [
  'minecraft:soul_fire_flame', 'minecraft:flame', 'minecraft:end_rod',
  'minecraft:dragon_breath', 'minecraft:portal', 'minecraft:enchant',
  'minecraft:totem_of_undying', 'minecraft:heart', 'minecraft:firework',
  'minecraft:sculk_soul', 'minecraft:reverse_portal', 'minecraft:cherry_leaves',
  'minecraft:white_smoke', 'minecraft:large_smoke', 'minecraft:ash',
  'minecraft:dust_plume', 'minecraft:rain',
]

const EFFECT_PRESETS = [
  'minecraft:blindness', 'minecraft:darkness', 'minecraft:nausea',
  'minecraft:slowness', 'minecraft:weakness', 'minecraft:glowing',
  'minecraft:levitation', 'minecraft:speed', 'minecraft:strength',
  'minecraft:resistance', 'minecraft:regeneration', 'minecraft:fire_resistance',
  'minecraft:slow_falling', 'minecraft:wither', 'minecraft:poison',
  'minecraft:invisibility',
]

type Props = {
  atm: Atmosphere
  category?: string
  onSave: (a: Atmosphere) => void
  onCancel: () => void
}

export function AtmosphereEditor({ atm, category = 'horror', onSave, onCancel }: Props) {
  const [a, setA] = useState<Atmosphere>(atm)

  function update(p: Partial<Atmosphere>) { setA({ ...a, ...p }) }
  function updateStep(i: number, p: any) {
    setA({ ...a, steps: a.steps.map((s, j) => j === i ? { ...s, ...p } as AtmosphereStep : s) })
  }
  function addStep(kind: AtmosphereStep['kind']) {
    setA({ ...a, steps: [...a.steps, defaultStep(kind)] })
  }
  function removeStep(i: number) { setA({ ...a, steps: a.steps.filter((_, j) => j !== i) }) }
  function moveStep(i: number, dir: -1 | 1) {
    const j = i + dir; if (j < 0 || j >= a.steps.length) return
    const next = [...a.steps]; [next[i], next[j]] = [next[j], next[i]]; setA({ ...a, steps: next })
  }
  function duplicateStep(i: number) {
    const copy = JSON.parse(JSON.stringify(a.steps[i]))
    setA({ ...a, steps: [...a.steps.slice(0, i + 1), copy, ...a.steps.slice(i + 1)] })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 md:p-4 bg-black/80 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-4xl w-full max-h-[95vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4 sticky top-0 bg-liberthia-900/90 backdrop-blur-md -mx-4 px-4 py-2 -mt-4 pt-4 z-10 rounded-t-2xl">
          <h3 className="font-bold text-lg flex items-center gap-2">
            🎬 Atmosfera Custom
          </h3>
          <div className="flex gap-2">
            <button className="btn-ghost btn-sm" onClick={onCancel}>✕ Cancelar</button>
            <button className="btn btn-sm" onClick={() => onSave(a)} disabled={!a.name || a.steps.length === 0}>💾 Salvar</button>
          </div>
        </div>

        {/* Header */}
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <select className="input text-2xl text-center" value={a.emoji} onChange={(e) => update({ emoji: e.target.value })}>
            {EMOJIS.map((e) => <option key={e} value={e}>{e}</option>)}
          </select>
          <input className="input font-bold" value={a.name} onChange={(e) => update({ name: e.target.value })} placeholder="Nome" />
        </div>
        <input className="input text-xs mb-3" value={a.description} onChange={(e) => update({ description: e.target.value })} placeholder="Descrição curta" />

        <label className="flex items-center gap-2 cursor-pointer mb-4 p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
          <input type="checkbox" checked={a.loop ?? false} onChange={(e) => update({ loop: e.target.checked })} />
          <span className="text-sm"><b>🔁 Loop infinito</b> — repete a sequência inteira até clicar Stop</span>
        </label>

        {/* Steps */}
        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">📋 Passos ({a.steps.length})</h4>
        </div>

        <div className="space-y-2 mb-4">
          {a.steps.map((s, i) => (
            <div key={i} className="border border-liberthia-500/20 rounded-xl p-2.5 bg-liberthia-900/40">
              <div className="flex items-center gap-2 mb-2 flex-wrap">
                <span className="badge badge-purple">#{i + 1}</span>
                <span className="chip">{stepLabel(s)}</span>
                <div className="ml-auto flex gap-0.5">
                  <button className="btn-ghost btn-sm" disabled={i === 0} onClick={() => moveStep(i, -1)} title="↑">↑</button>
                  <button className="btn-ghost btn-sm" disabled={i === a.steps.length - 1} onClick={() => moveStep(i, 1)} title="↓">↓</button>
                  <button className="btn-ghost btn-sm" onClick={() => duplicateStep(i)} title="Duplicar">⎘</button>
                  <button className="btn-ghost btn-sm" onClick={() => removeStep(i)}>🗑</button>
                </div>
              </div>
              <StepEditor step={s} category={category} onChange={(p) => updateStep(i, p)} />
            </div>
          ))}
        </div>

        {/* Add step buttons */}
        <div className="card !p-2 mb-4">
          <div className="text-[10px] uppercase tracking-widest text-liberthia-300/50 mb-2">+ Adicionar passo</div>
          <div className="grid grid-cols-3 sm:grid-cols-5 lg:grid-cols-7 gap-1.5">
            <AddBtn label="📺 title" onClick={() => addStep('title')} />
            <AddBtn label="⚡ flash" onClick={() => addStep('flash')} title="Texto piscando rápido" />
            <AddBtn label="🜨 garble" onClick={() => addStep('garble')} title="Texto progressivamente corrompido" />
            <AddBtn label="🔊 sound" onClick={() => addStep('sound')} />
            <AddBtn label="💬 chat" onClick={() => addStep('chat')} />
            <AddBtn label="⚗ effect" onClick={() => addStep('effect')} />
            <AddBtn label="✨ particle" onClick={() => addStep('particle')} />
            <AddBtn label="⛅ weather" onClick={() => addStep('weather')} />
            <AddBtn label="🕐 time" onClick={() => addStep('time')} />
            <AddBtn label="⌨ cmd" onClick={() => addStep('command')} />
            <AddBtn label="⏳ wait" onClick={() => addStep('wait')} />
            <AddBtn label="🔁 loop" onClick={() => addStep('loop')} title="Repete um grupo N vezes" />
            <AddBtn label="🎲 random" onClick={() => addStep('random')} title="Sorteia entre opções" />
          </div>
        </div>
      </div>
    </div>
  )
}

function AddBtn({ label, onClick, title }: { label: string; onClick: () => void; title?: string }) {
  return <button className="btn-ghost btn-sm" onClick={onClick} title={title}>{label}</button>
}

// ============= Step-specific editors =============

function StepEditor({ step, category, onChange }: { step: AtmosphereStep; category: string; onChange: (p: any) => void }) {
  switch (step.kind) {
    case 'title': return <TitleStep s={step} onChange={onChange} />
    case 'flash': return <FlashStep s={step} onChange={onChange} />
    case 'garble': return <GarbleStep s={step} onChange={onChange} />
    case 'sound': return <SoundStep s={step} category={category} onChange={onChange} />
    case 'chat': return <ChatStep s={step} onChange={onChange} />
    case 'effect': return <EffectStep s={step} onChange={onChange} />
    case 'particle': return <ParticleStep s={step} onChange={onChange} />
    case 'weather': return <WeatherStep s={step} onChange={onChange} />
    case 'time': return <TimeStep s={step} onChange={onChange} />
    case 'command': return <CommandStep s={step} onChange={onChange} />
    case 'wait': return <WaitStep s={step} onChange={onChange} />
    case 'loop': return <LoopStep s={step} category={category} onChange={onChange} />
    case 'random': return <RandomStep s={step} category={category} onChange={onChange} />
  }
}

function TitleStep({ s, onChange }: any) {
  return (
    <div className="space-y-2">
      <div className="grid grid-cols-3 gap-1.5 text-xs">
        <select className="input text-xs col-span-3 sm:col-span-1" value={s.mode ?? 'title'} onChange={(e) => onChange({ mode: e.target.value })}>
          <option value="title">📺 Title (grande)</option>
          <option value="subtitle">📺 Subtitle</option>
          <option value="actionbar">📺 Actionbar</option>
        </select>
        <div><label className="label">Fade in</label><input type="number" className="input" value={s.fadeIn ?? 10} onChange={(e) => onChange({ fadeIn: Number(e.target.value) })} /></div>
        <div><label className="label">Stay</label><input type="number" className="input" value={s.stay ?? 60} onChange={(e) => onChange({ stay: Number(e.target.value) })} /></div>
      </div>
      <MinecraftFormatter value={s.text} onChange={(v) => onChange({ text: v })} rows={1} maxChars={120} showCounter={false} placeholder="Texto" />
      {s.mode !== 'subtitle' && s.mode !== 'actionbar' && (
        <MinecraftFormatter value={s.subtitle ?? ''} onChange={(v) => onChange({ subtitle: v })} rows={1} maxChars={120} showCounter={false} placeholder="Subtitle (opcional)" />
      )}
    </div>
  )
}

function FlashStep({ s, onChange }: any) {
  return (
    <div className="space-y-2">
      <input className="input text-sm" value={s.text} onChange={(e) => onChange({ text: e.target.value })} placeholder="Texto piscante" />
      <div className="grid grid-cols-3 gap-1.5 text-xs">
        <div><label className="label">Piscadas</label><input type="number" className="input" value={s.flashes} min={1} max={50} onChange={(e) => onChange({ flashes: Number(e.target.value) })} /></div>
        <div><label className="label">Intervalo (ms)</label><input type="number" className="input" value={s.intervalMs} min={30} step={10} onChange={(e) => onChange({ intervalMs: Number(e.target.value) })} /></div>
        <div className="flex items-end">
          <FlashSoundField sound={s.sound ?? ''} onChange={(v) => onChange({ sound: v || undefined })} />
        </div>
      </div>
      <div>
        <label className="label block mb-1">Cores (alterna)</label>
        <div className="flex flex-wrap gap-1">
          {MC_COLORS.map((c) => {
            const arr: string[] = s.colors ?? ['§c', '§4', '§f']
            const on = arr.includes(c.code)
            return (
              <button key={c.code} type="button" title={c.label}
                onClick={() => onChange({ colors: on ? arr.filter((x) => x !== c.code) : [...arr, c.code] })}
                className={`w-6 h-6 rounded border transition ${on ? 'ring-2 ring-white scale-110' : 'border-white/10'}`}
                style={{ background: c.hex }}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}

function GarbleStep({ s, onChange }: any) {
  return (
    <div className="space-y-2">
      <input className="input text-sm" value={s.text} onChange={(e) => onChange({ text: e.target.value })} placeholder="Texto inicial (vai corrompendo)" />
      <input className="input text-sm" value={s.finalText ?? ''} onChange={(e) => onChange({ finalText: e.target.value || undefined })} placeholder="Texto final (opcional)" />
      <div className="grid grid-cols-3 gap-1.5 text-xs">
        <div><label className="label">Iterações</label><input type="number" className="input" value={s.iterations} min={2} max={50} onChange={(e) => onChange({ iterations: Number(e.target.value) })} /></div>
        <div><label className="label">Intervalo (ms)</label><input type="number" className="input" value={s.intervalMs} min={30} step={10} onChange={(e) => onChange({ intervalMs: Number(e.target.value) })} /></div>
        <div><label className="label">Cor</label><input className="input font-mono" value={s.color ?? '§8'} onChange={(e) => onChange({ color: e.target.value })} /></div>
      </div>
    </div>
  )
}

function SoundStep({ s, category, onChange }: any) {
  const presets = SOUND_PRESETS_BY_CAT[category] ?? SOUND_PRESETS_BY_CAT.horror
  const soundOpts = useSoundOptions()
  return (
    <div className="space-y-2">
      <Autocomplete value={s.sound ?? ''} onChange={(v) => onChange({ sound: v })}
        options={soundOpts} placeholder="minecraft:entity..." />
      <div className="flex flex-wrap gap-1">
        {presets.map(([id, lbl]: [string, string]) => (
          <button key={id} className="btn-ghost btn-sm" onClick={() => onChange({ sound: id })}>{lbl}</button>
        ))}
      </div>
      <div className="grid grid-cols-2 gap-2 text-xs">
        <div><label className="label">Pitch ({(s.pitch ?? 1).toFixed(2)})</label><input type="range" min={0.5} max={2} step={0.05} value={s.pitch ?? 1} onChange={(e) => onChange({ pitch: Number(e.target.value) })} className="w-full" /></div>
        <div><label className="label">Volume ({(s.volume ?? 1).toFixed(2)})</label><input type="range" min={0} max={1} step={0.05} value={s.volume ?? 1} onChange={(e) => onChange({ volume: Number(e.target.value) })} className="w-full" /></div>
      </div>
    </div>
  )
}

function ChatStep({ s, onChange }: any) {
  return <MinecraftFormatter value={s.message} onChange={(v) => onChange({ message: v })} rows={2} maxChars={200} showCounter={false} placeholder="Mensagem no chat" />
}

function EffectStep({ s, onChange }: any) {
  const effectOpts = useEffectOptions()
  return (
    <div className="space-y-2">
      <Autocomplete value={s.effect ?? ''} onChange={(v) => onChange({ effect: v })}
        options={effectOpts} placeholder="minecraft:nausea" />
      <div className="flex flex-wrap gap-1">
        {EFFECT_PRESETS.map((p) => (
          <button key={p} className="btn-ghost btn-sm" onClick={() => onChange({ effect: 'minecraft:' + p })}>{p.slice(0, 8)}</button>
        ))}
      </div>
      <div className="grid grid-cols-2 gap-2 text-xs">
        <div><label className="label">Duração (t)</label><input type="number" className="input" value={s.duration} onChange={(e) => onChange({ duration: Number(e.target.value) })} /></div>
        <div><label className="label">Amplificador</label><input type="number" className="input" value={s.amplifier} onChange={(e) => onChange({ amplifier: Number(e.target.value) })} /></div>
      </div>
    </div>
  )
}

function ParticleStep({ s, onChange }: any) {
  const particleOpts = useParticleOptions()
  return (
    <div className="space-y-2">
      <Autocomplete value={s.particle ?? ''} onChange={(v) => onChange({ particle: v })}
        options={particleOpts} placeholder="minecraft:smoke" />
      <div className="flex flex-wrap gap-1">
        {PARTICLE_PRESETS.slice(0, 12).map((p) => (
          <button key={p} className="btn-ghost btn-sm text-[10px]"
            onClick={() => onChange({ particle: p })}>{p.replace('minecraft:', '')}</button>
        ))}
      </div>
      <div className="grid grid-cols-4 gap-2 text-xs">
        <div><label className="label">+X</label><NumInput value={s.relX} step={0.5} onChange={(v) => onChange({ relX: v })} /></div>
        <div><label className="label">+Y</label><NumInput value={s.relY} step={0.5} onChange={(v) => onChange({ relY: v })} /></div>
        <div><label className="label">+Z</label><NumInput value={s.relZ} step={0.5} onChange={(v) => onChange({ relZ: v })} /></div>
        <div><label className="label">Count</label><input type="number" className="input" value={s.count} onChange={(e) => onChange({ count: Number(e.target.value) })} /></div>
      </div>
    </div>
  )
}

function WeatherStep({ s, onChange }: any) {
  return (
    <div className="grid grid-cols-2 gap-2 text-xs">
      <select className="input" value={s.type} onChange={(e) => onChange({ type: e.target.value })}>
        <option value="clear">☀ clear</option>
        <option value="rain">🌧 rain</option>
        <option value="thunder">⛈ thunder</option>
      </select>
      <input type="number" className="input" value={s.duration} onChange={(e) => onChange({ duration: Number(e.target.value) })} placeholder="ticks" />
    </div>
  )
}

function TimeStep({ s, onChange }: any) {
  return (
    <div className="flex gap-2 items-center text-xs">
      <input type="number" className="input w-28" value={s.ticks} onChange={(e) => onChange({ ticks: Number(e.target.value) })} />
      <div className="flex gap-1">
        <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 0 })}>☀</button>
        <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 13000 })}>🌅</button>
        <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 18000 })}>🌙</button>
        <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 23000 })}>🌌</button>
      </div>
    </div>
  )
}

function CommandStep({ s, onChange }: any) {
  return <input className="input font-mono text-xs" value={s.cmd} onChange={(e) => onChange({ cmd: e.target.value })} placeholder="say hello" />
}

function WaitStep({ s, onChange }: any) {
  return (
    <div className="flex items-center gap-2 text-xs">
      <input type="range" min={50} max={10000} step={50} value={s.ms} onChange={(e) => onChange({ ms: Number(e.target.value) })} className="flex-1" />
      <span className="font-mono w-20 text-right">{(s.ms / 1000).toFixed(2)}s</span>
    </div>
  )
}

function LoopStep({ s, category, onChange }: any) {
  function addInner(kind: AtmosphereStep['kind']) {
    onChange({ steps: [...s.steps, defaultStep(kind)] })
  }
  return (
    <div className="space-y-2">
      <div className="grid grid-cols-2 gap-2 text-xs">
        <div><label className="label">Iterações</label><input type="number" className="input" value={s.iterations} min={1} max={50} onChange={(e) => onChange({ iterations: Number(e.target.value) })} /></div>
        <div><label className="label">Gap (ms)</label><input type="number" className="input" value={s.gapMs ?? 0} onChange={(e) => onChange({ gapMs: Number(e.target.value) })} /></div>
      </div>
      <div className="border-l-2 border-liberthia-400/30 pl-2 space-y-1">
        {s.steps.map((sub: AtmosphereStep, i: number) => (
          <div key={i} className="bg-liberthia-900/60 rounded p-1.5">
            <div className="flex items-center gap-1.5 mb-1">
              <span className="chip text-[10px]">{stepLabel(sub)}</span>
              <button className="btn-ghost btn-sm ml-auto" onClick={() => onChange({ steps: s.steps.filter((_: any, j: number) => j !== i) })}>🗑</button>
            </div>
            <StepEditor step={sub} category={category} onChange={(p: any) => onChange({ steps: s.steps.map((x: any, j: number) => j === i ? { ...x, ...p } : x) })} />
          </div>
        ))}
        <div className="flex flex-wrap gap-1">
          {['title', 'flash', 'sound', 'particle', 'effect', 'wait', 'chat'].map((k) => (
            <button key={k} className="btn-ghost btn-sm" onClick={() => addInner(k as any)}>+ {k}</button>
          ))}
        </div>
      </div>
    </div>
  )
}

function RandomStep({ s, category, onChange }: any) {
  function addChoice(kind: AtmosphereStep['kind']) {
    onChange({ choices: [...s.choices, defaultStep(kind)] })
  }
  return (
    <div className="space-y-2">
      <div className="text-xs">
        <label className="label">Sortear</label>
        <input type="number" className="input w-20 inline-block ml-2" value={s.count ?? 1} min={1} max={s.choices.length || 1}
          onChange={(e) => onChange({ count: Number(e.target.value) })} />
        <span className="ml-2 text-liberthia-300/60">de {s.choices.length} opções</span>
      </div>
      <div className="border-l-2 border-amber-400/30 pl-2 space-y-1">
        {s.choices.map((sub: AtmosphereStep, i: number) => (
          <div key={i} className="bg-liberthia-900/60 rounded p-1.5">
            <div className="flex items-center gap-1.5 mb-1">
              <span className="badge badge-yellow text-[10px]">opt {i + 1}</span>
              <span className="chip text-[10px]">{stepLabel(sub)}</span>
              <button className="btn-ghost btn-sm ml-auto" onClick={() => onChange({ choices: s.choices.filter((_: any, j: number) => j !== i) })}>🗑</button>
            </div>
            <StepEditor step={sub} category={category} onChange={(p: any) => onChange({ choices: s.choices.map((x: any, j: number) => j === i ? { ...x, ...p } : x) })} />
          </div>
        ))}
        <div className="flex flex-wrap gap-1">
          {['title', 'flash', 'sound', 'particle', 'chat'].map((k) => (
            <button key={k} className="btn-ghost btn-sm" onClick={() => addChoice(k as any)}>+ {k}</button>
          ))}
        </div>
      </div>
    </div>
  )
}

// Sub-componente isolado pra que o hook useSoundOptions funcione no FlashStep.
// (não pode chamar hook dentro de uma expressão JSX em outro lugar)
function FlashSoundField({ sound, onChange }: { sound: string; onChange: (v: string) => void }) {
  const opts = useSoundOptions()
  return (
    <Autocomplete value={sound} onChange={onChange}
      options={opts} placeholder="sound (opcional)" />
  )
}
