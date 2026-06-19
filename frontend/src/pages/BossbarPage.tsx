import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, MC_COLORS, renderMcText } from '../components/MinecraftFormatter'
import { useKvState } from '../lib/kvState'

/**
 * Bossbar Director — cria, atualiza e remove bossbars custom no servidor.
 *
 * Vanilla syntax:
 *   /bossbar add <id> <name>             cria
 *   /bossbar set <id> name <name>        renomeia
 *   /bossbar set <id> color <color>      cor (pink, blue, red, green, yellow, purple, white)
 *   /bossbar set <id> style <style>      progress|notched_6|notched_10|notched_12|notched_20
 *   /bossbar set <id> max <int>          valor máximo (default 100)
 *   /bossbar set <id> value <int>        valor atual (0..max)
 *   /bossbar set <id> players <selector> quem vê
 *   /bossbar set <id> visible true|false
 *   /bossbar remove <id>
 *
 * Bossbars persistem entre restarts no scoreboards.dat, então o frontend
 * só guarda a definição visual em localStorage pra reuso.
 */

type BarColor = 'pink' | 'blue' | 'red' | 'green' | 'yellow' | 'purple' | 'white'
type BarStyle = 'progress' | 'notched_6' | 'notched_10' | 'notched_12' | 'notched_20'

type Bar = {
  id: string
  name: string
  nameColor: string // §-code
  color: BarColor
  style: BarStyle
  max: number
  value: number
  targets: 'all' | string[] // uuids
  updated: number
}

const COLORS: { c: BarColor; emoji: string; hex: string }[] = [
  { c: 'pink',   emoji: '🌸', hex: '#ff55ff' },
  { c: 'blue',   emoji: '🟦', hex: '#5555ff' },
  { c: 'red',    emoji: '🟥', hex: '#ff5555' },
  { c: 'green',  emoji: '🟩', hex: '#55ff55' },
  { c: 'yellow', emoji: '🟨', hex: '#ffff55' },
  { c: 'purple', emoji: '🟪', hex: '#aa00aa' },
  { c: 'white',  emoji: '⬜', hex: '#ffffff' },
]
const STYLES: { s: BarStyle; label: string }[] = [
  { s: 'progress',    label: 'Contínua' },
  { s: 'notched_6',   label: '6 segmentos' },
  { s: 'notched_10',  label: '10 segmentos' },
  { s: 'notched_12',  label: '12 segmentos' },
  { s: 'notched_20',  label: '20 segmentos' },
]

export function BossbarPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []

  const [bars, setBars] = useKvState<Bar[]>('bossbars', [])
  const [editing, setEditing] = useState<Bar | null>(null)
  // Set de IDs sendo drenados (pra mostrar "⏹ Cancelar" no botão)
  const drainingRef = useRef<Set<string>>(new Set())
  const [draining, setDraining] = useState<Set<string>>(new Set())
  // Config local de dano por card
  const [dmgInput, setDmgInput] = useState<Record<string, { amount: number; duration: number }>>({})

  function newBar() {
    setEditing({
      id: `liberthia_${Date.now().toString(36)}`,
      name: 'Vida do Boss',
      nameColor: '§c§l',
      color: 'red',
      style: 'notched_10',
      max: 100,
      value: 100,
      targets: 'all',
      updated: Date.now(),
    })
  }

  /** Cria a bossbar no MC (idempotente: remove anterior se mesmo id).
   *  /bossbar add aceita JSON CRU como componente — single-stringify só. */
  async function deploy(b: Bar) {
    const nameJson = JSON.stringify({ text: b.nameColor + b.name })
    const cmds: string[] = [
      `bossbar remove minecraft:${b.id}`,
      `bossbar add minecraft:${b.id} ${nameJson}`,
      `bossbar set minecraft:${b.id} color ${b.color}`,
      `bossbar set minecraft:${b.id} style ${b.style}`,
      `bossbar set minecraft:${b.id} max ${b.max}`,
      `bossbar set minecraft:${b.id} value ${b.value}`,
      `bossbar set minecraft:${b.id} visible true`,
    ]
    const sel = b.targets === 'all' ? '@a' : (b.targets as string[]).map((u) => nameByUuid(u, players)).join(' ')
    if (sel) cmds.push(`bossbar set minecraft:${b.id} players ${sel}`)
    for (const c of cmds) {
      try {
        const r: any = await api.command(c, 'bossbar')
        // /bossbar remove em id inexistente retorna result=0; tudo bem
        // Mas se for o add e result=0, algo deu errado.
        if (c.includes('bossbar add') && r?.result === 0) {
          toast.err(`Falha em: ${c}`)
        }
      } catch (e: any) { toast.err(`${c} → ${e.message}`); return }
    }
    toast.ok(`✓ Bossbar "${b.name}" ativa`)
  }

  async function updateValue(b: Bar, v: number) {
    setBars((bs) => bs.map((x) => x.id === b.id ? { ...x, value: v } : x))
    try { await api.command(`bossbar set minecraft:${b.id} value ${v}`, 'bossbar') } catch {}
  }

  /**
   * Drena a vida do boss: anima value -= step a cada interval ms, até zero
   * ou até cancelar. A cada tick: atualiza estado + envia /bossbar set value
   * + toca som de "hit" pros recipients.
   */
  async function drainDamage(b: Bar, totalDmg: number, durationMs: number) {
    if (drainingRef.current.has(b.id)) {
      // Já está drenando — cancela
      drainingRef.current.delete(b.id)
      setDraining(new Set(drainingRef.current))
      return
    }
    drainingRef.current.add(b.id)
    setDraining(new Set(drainingRef.current))

    const steps = Math.max(2, Math.floor(durationMs / 150))
    const stepMs = durationMs / steps
    const dmgPerStep = totalDmg / steps
    const startVal = b.value
    const targetVal = Math.max(0, startVal - totalDmg)
    const recipients = b.targets === 'all' ? '@a' : (b.targets as string[]).map((u) => nameByUuid(u, players)).join(' ')

    for (let i = 1; i <= steps; i++) {
      if (!drainingRef.current.has(b.id)) break
      const newVal = Math.max(0, Math.round(startVal - dmgPerStep * i))
      try {
        await api.command(`bossbar set minecraft:${b.id} value ${newVal}`, 'bossbar-drain')
        if (recipients) {
          api.command(`playsound minecraft:entity.iron_golem.hurt master ${recipients} ~ ~ ~ 0.4 1.6`, 'bossbar-drain').catch(() => {})
        }
      } catch {}
      // Atualiza state local
      setBars((bs) => bs.map((x) => x.id === b.id ? { ...x, value: newVal } : x))
      await new Promise((r) => setTimeout(r, stepMs))
    }
    drainingRef.current.delete(b.id)
    setDraining(new Set(drainingRef.current))

    if (targetVal === 0) {
      // Death rattle
      if (recipients) {
        try {
          await api.command(`playsound minecraft:entity.wither.death master ${recipients} ~ ~ ~ 1 0.8`, 'bossbar-drain')
        } catch {}
      }
      toast.ok(`💀 ${b.name} caiu`)
    }
  }

  async function quickHit(b: Bar, dmg: number) {
    const newVal = Math.max(0, b.value - dmg)
    await updateValue(b, newVal)
    const recipients = b.targets === 'all' ? '@a' : (b.targets as string[]).map((u) => nameByUuid(u, players)).join(' ')
    if (recipients) {
      try { await api.command(`playsound minecraft:entity.iron_golem.hurt master ${recipients} ~ ~ ~ 0.6 1.2`, 'bossbar') } catch {}
    }
  }

  async function fullHeal(b: Bar) {
    await updateValue(b, b.max)
    const recipients = b.targets === 'all' ? '@a' : (b.targets as string[]).map((u) => nameByUuid(u, players)).join(' ')
    if (recipients) {
      try { await api.command(`playsound minecraft:block.beacon.power_select master ${recipients} ~ ~ ~ 1 1.4`, 'bossbar') } catch {}
    }
    toast.ok(`❤ ${b.name} restaurado`)
  }

  async function remove(b: Bar) {
    try {
      await api.command(`bossbar remove minecraft:${b.id}`, 'bossbar')
      toast.ok(`Bossbar ${b.name} removida`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function hide(b: Bar, visible: boolean) {
    try {
      await api.command(`bossbar set minecraft:${b.id} visible ${visible}`, 'bossbar')
      toast.ok(visible ? '👁 Visível' : '🙈 Escondida')
    } catch (e: any) { toast.err(e.message) }
  }

  function commit(b: Bar) {
    setBars((cur) => {
      const i = cur.findIndex((x) => x.id === b.id)
      if (i >= 0) { const n = [...cur]; n[i] = b; return n }
      return [...cur, b]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📊 Bossbars</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Barras dramáticas no topo da tela do player — perfeitas pra HP de boss, timers de evento, progresso de quest, contador de loot.
          </p>
        </div>
        <button className="btn" onClick={newBar}>+ Nova Bossbar</button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {bars.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">📊</div>
            <p className="text-liberthia-300/70">Nenhuma bossbar criada ainda.</p>
          </div>
        )}
        {bars.map((b) => {
          const colorHex = COLORS.find((c) => c.c === b.color)?.hex ?? '#aa40e8'
          const pct = Math.max(0, Math.min(1, b.value / Math.max(b.max, 1)))
          return (
            <div key={b.id} className="card-glow">
              {/* Preview */}
              <div className="mb-3 p-3 rounded-lg bg-black/60 border border-white/10">
                <div className="text-center mb-1 font-bold" style={{ textShadow: '2px 2px 0 #000' }}>
                  {renderMcText(b.nameColor + b.name)}
                </div>
                <div className="h-2 rounded-sm overflow-hidden bg-black/80 border border-white/20">
                  <div className="h-full transition-all" style={{ width: `${pct * 100}%`, background: colorHex, boxShadow: `0 0 8px ${colorHex}` }} />
                </div>
                <div className="text-[10px] text-liberthia-300/50 text-center mt-1 font-mono">
                  {b.value} / {b.max}
                </div>
              </div>

              <div className="text-[10px] font-mono text-liberthia-300/50 mb-2 truncate">
                id: minecraft:{b.id}
              </div>
              <div className="flex flex-wrap gap-1 mb-3">
                <span className="chip" style={{ background: colorHex + '30', borderColor: colorHex + '50', color: colorHex }}>
                  {COLORS.find((c) => c.c === b.color)?.emoji} {b.color}
                </span>
                <span className="chip">{STYLES.find((s) => s.s === b.style)?.label}</span>
                <span className="chip">
                  {b.targets === 'all' ? '👥 todos' : `👤 ${(b.targets as string[]).length}`}
                </span>
              </div>

              {/* Value slider */}
              <div className="mb-3">
                <label className="label block mb-1">Valor: <span className="font-mono">{b.value}/{b.max}</span></label>
                <input
                  type="range" min={0} max={b.max} value={b.value}
                  onChange={(e) => updateValue(b, Number(e.target.value))}
                  className="w-full"
                />
              </div>

              {/* Damage controls */}
              <div className="mb-3 p-2 rounded-lg bg-red-500/10 border border-red-400/20">
                <div className="text-[10px] uppercase tracking-widest text-red-300/70 font-bold mb-2">💥 Damage</div>
                <div className="flex items-center gap-1.5 mb-2">
                  <label className="text-[10px] text-liberthia-300/60">Dmg:</label>
                  <input type="number" className="input text-xs w-16 !py-1"
                    value={dmgInput[b.id]?.amount ?? Math.max(1, Math.round(b.max * 0.1))}
                    onChange={(e) => setDmgInput((d) => ({ ...d, [b.id]: { amount: Number(e.target.value), duration: d[b.id]?.duration ?? 1500 } }))} />
                  <label className="text-[10px] text-liberthia-300/60">por</label>
                  <input type="number" className="input text-xs w-16 !py-1"
                    value={dmgInput[b.id]?.duration ?? 1500} step={100}
                    onChange={(e) => setDmgInput((d) => ({ ...d, [b.id]: { amount: d[b.id]?.amount ?? Math.max(1, Math.round(b.max * 0.1)), duration: Number(e.target.value) } }))} />
                  <span className="text-[10px] text-liberthia-300/60">ms</span>
                </div>
                <div className="grid grid-cols-3 gap-1">
                  <button className="btn-ghost btn-sm" onClick={() => quickHit(b, dmgInput[b.id]?.amount ?? Math.max(1, Math.round(b.max * 0.1)))}>
                    ⚔ Hit
                  </button>
                  <button
                    className={draining.has(b.id) ? 'btn-danger btn-sm pulse-glow' : 'btn-amber btn-sm'}
                    onClick={() => drainDamage(b, dmgInput[b.id]?.amount ?? Math.max(1, Math.round(b.max * 0.1)), dmgInput[b.id]?.duration ?? 1500)}
                    title={draining.has(b.id) ? 'Cancelar drenagem' : 'Drenar dano ao longo do tempo'}
                  >
                    {draining.has(b.id) ? '⏹ Stop' : '🩸 Drain'}
                  </button>
                  <button className="btn-success btn-sm" onClick={() => fullHeal(b)} title="Restaurar HP">❤ Heal</button>
                </div>
              </div>

              <div className="grid grid-cols-4 gap-1.5">
                <button className="btn-success btn-sm" onClick={() => deploy(b)}>▶ Deploy</button>
                <button className="btn-ghost btn-sm" onClick={() => hide(b, false)}>🙈</button>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(b)}>✎</button>
                <button className="btn-danger btn-sm" onClick={async () => { await remove(b); setBars((bs) => bs.filter((x) => x.id !== b.id)) }}>🗑</button>
              </div>
            </div>
          )
        })}
      </div>

      {editing && <BarEditor bar={editing} players={players} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function BarEditor({ bar, players, onSave, onCancel }: {
  bar: Bar; players: any[]; onSave: (b: Bar) => void; onCancel: () => void
}) {
  const [b, setB] = useState<Bar>(bar)
  const isAll = b.targets === 'all'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">📊 Bossbar</h3>

        <label className="label block mb-1">
          ID interno <span className="text-liberthia-300/40">(sem espaços, lowercase)</span>
        </label>
        <input className="input mb-3 font-mono text-xs" value={b.id}
          onChange={(e) => setB({ ...b, id: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_') })} />

        <label className="label block mb-1">Nome (texto na barra)</label>
        <MinecraftFormatter
          value={b.name}
          onChange={(v) => setB({ ...b, name: v })}
          rows={2}
          maxChars={64}
        />

        <div className="grid grid-cols-2 gap-3 mt-3">
          <div>
            <label className="label block mb-1">Cor da barra</label>
            <div className="grid grid-cols-7 gap-1">
              {COLORS.map((c) => (
                <button key={c.c} type="button" title={c.c}
                  onClick={() => setB({ ...b, color: c.c })}
                  className={`h-9 rounded border transition ${b.color === c.c ? 'ring-2 ring-white scale-110' : 'border-white/20'}`}
                  style={{ background: c.hex }}
                >{c.emoji}</button>
              ))}
            </div>
          </div>
          <div>
            <label className="label block mb-1">Estilo</label>
            <select className="input" value={b.style} onChange={(e) => setB({ ...b, style: e.target.value as BarStyle })}>
              {STYLES.map((s) => <option key={s.s} value={s.s}>{s.label}</option>)}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 mt-3">
          <div>
            <label className="label block mb-1">Max</label>
            <input type="number" className="input" value={b.max} min={1}
              onChange={(e) => setB({ ...b, max: Math.max(1, Number(e.target.value)), value: Math.min(b.value, Number(e.target.value)) })} />
          </div>
          <div>
            <label className="label block mb-1">Valor inicial</label>
            <input type="number" className="input" value={b.value} min={0} max={b.max}
              onChange={(e) => setB({ ...b, value: Math.min(b.max, Math.max(0, Number(e.target.value))) })} />
          </div>
        </div>

        <div className="mt-4">
          <label className="label block mb-1">Quem vê</label>
          <select className="input mb-2" value={isAll ? 'all' : 'custom'}
            onChange={(e) => setB({ ...b, targets: e.target.value === 'all' ? 'all' : [] })}>
            <option value="all">Todos players online</option>
            <option value="custom">Players selecionados</option>
          </select>
          {!isAll && (
            <div className="flex flex-wrap gap-1.5">
              {players.map((p: any) => {
                const on = (b.targets as string[]).includes(p.uuid)
                return (
                  <button key={p.uuid} type="button"
                    className={`btn-ghost btn-sm ${on ? '!bg-liberthia-500/30 !text-white' : ''}`}
                    onClick={() => setB({
                      ...b,
                      targets: on ? (b.targets as string[]).filter((u) => u !== p.uuid) : [...(b.targets as string[]), p.uuid],
                    })}
                  >{p.name}</button>
                )
              })}
            </div>
          )}
        </div>

        <div className="flex gap-2 justify-end mt-6">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave({ ...b, updated: Date.now() })}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

function nameByUuid(uuid: string, players: any[]) {
  return players.find((p) => p.uuid === uuid)?.name ?? '@p'
}
