import { useState, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftTitlePreview } from '../components/MinecraftTitlePreview'
import { useKvState } from '../lib/kvState'

/**
 * Dice Roller — rolagens de dado pra mecânicas de RPG/decisão.
 *
 * - Suporta dN (4, 6, 8, 10, 12, 20, 100) + modificador
 * - Múltiplos dados de uma vez (ex: 3d6+2)
 * - Resultado vai como title cinematográfico pro player + chat opcional
 * - Modo dano: se rolar pra causar dano, aplica /damage no player
 * - Histórico das últimas 20 rolagens
 */

type Roll = {
  id: number
  player: string
  expr: string         // "3d6+2"
  rolls: number[]      // valores individuais
  modifier: number
  total: number
  critical?: 'success' | 'fail'
  ts: number
  kind: 'normal' | 'damage' | 'decision'
}

const DICE = [4, 6, 8, 10, 12, 20, 100] as const

export function DiceRollerPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [selected, setSelected] = useState<string>('')
  const [d, setD] = useState<number>(20)
  const [count, setCount] = useState(1)
  const [mod, setMod] = useState(0)
  const [kind, setKind] = useState<'normal' | 'damage' | 'decision'>('normal')
  const [decisionThreshold, setDecisionThreshold] = useState(10)
  const [rolling, setRolling] = useState(false)
  const [history, setHistory] = useKvState<Roll[]>('dice_history', [])
  const [lastResult, setLastResult] = useState<Roll | null>(null)
  const idCounter = useRef(0)

  function dice(n: number): number {
    return 1 + Math.floor(Math.random() * n)
  }

  async function roll() {
    if (!selected) { toast.err('Selecione player'); return }
    const player = players.find((p) => p.uuid === selected); if (!player) return
    setRolling(true)
    // Animação: várias rolagens fake antes do final
    for (let i = 0; i < 6; i++) {
      const fake = Array.from({ length: count }, () => dice(d))
      const fakeTotal = fake.reduce((a, v) => a + v, 0) + mod
      try {
        await api.title(selected, `§e§l🎲 ${count}d${d}${mod > 0 ? `+${mod}` : mod < 0 ? mod : ''}`, `§7§o...rolando... §f${fakeTotal}`, 2, 8, 2)
        await api.sound(selected, 'minecraft:block.note_block.hat', 0.6, 1 + i * 0.1)
      } catch {}
      await new Promise((r) => setTimeout(r, 200))
    }
    // Final
    const rolls = Array.from({ length: count }, () => dice(d))
    const total = rolls.reduce((a, v) => a + v, 0) + mod
    let critical: 'success' | 'fail' | undefined
    if (count === 1 && d === 20) {
      if (rolls[0] === 20) critical = 'success'
      else if (rolls[0] === 1) critical = 'fail'
    }
    const expr = `${count}d${d}${mod !== 0 ? (mod > 0 ? `+${mod}` : mod) : ''}`
    const result: Roll = {
      id: ++idCounter.current,
      player: player.name, expr, rolls, modifier: mod, total, critical,
      ts: Date.now(), kind,
    }
    setLastResult(result)
    setHistory((h) => [result, ...h].slice(0, 20))

    // Apresentação do resultado
    try {
      let titleText: string, subtitleText: string, sound: string, pitch: number
      if (critical === 'success') {
        titleText = '§6§l✦ CRITICAL ✦'
        subtitleText = `§a${total}`
        sound = 'minecraft:entity.player.levelup'; pitch = 1.5
      } else if (critical === 'fail') {
        titleText = '§4§l✗ FALHA CRÍTICA'
        subtitleText = `§c${total}`
        sound = 'minecraft:entity.villager.no'; pitch = 0.6
      } else if (kind === 'decision') {
        const success = total >= decisionThreshold
        titleText = success ? '§a§l✓ SUCESSO' : '§c§l✗ FALHA'
        subtitleText = `§7${rolls.join('+')}${mod ? (mod > 0 ? '+' + mod : mod) : ''} §f= §l${total} §7vs ${decisionThreshold}`
        sound = success ? 'minecraft:block.amethyst_block.chime' : 'minecraft:block.note_block.bass'
        pitch = success ? 1.4 : 0.5
      } else if (kind === 'damage') {
        titleText = `§c§l💥 ${total} dano`
        subtitleText = `§7${rolls.join('+')}${mod ? (mod > 0 ? '+' + mod : mod) : ''}`
        sound = 'minecraft:entity.iron_golem.hurt'; pitch = 1
        // Aplica dano
        try { await api.command(`damage ${player.name} ${total}`, 'dice') } catch {}
      } else {
        titleText = `§e§l🎲 ${total}`
        subtitleText = `§7${expr} → ${rolls.join('+')}${mod ? (mod > 0 ? '+' + mod : mod) : ''}`
        sound = 'minecraft:block.amethyst_block.chime'; pitch = 1.2
      }
      await api.title(selected, titleText, subtitleText, 5, 80, 20)
      await api.sound(selected, sound, 1, pitch)
      // Broadcast pra todos (clipboard-friendly chat log)
      await api.command(`tellraw @a ${JSON.stringify({ text: `§7🎲 §f${player.name} §7rolou §e${expr} §7→ §l§f${total}${critical ? (critical === 'success' ? ' §6CRIT!' : ' §4CRIT FAIL') : ''}` })}`, 'dice')
    } catch (e: any) { toast.err(e.message) }
    setRolling(false)
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6">
        <h1 className="page-title">🎲 Dice Roller</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Rolagens de RPG com apresentação dramática. Crítico em d20, modo dano (aplica /damage),
          modo decisão (sucesso/falha vs threshold).
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-5">
        <div className="space-y-4">
          {/* Builder */}
          <div className="card-glow">
            <h3 className="font-bold mb-3">⚙ Configuração</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-3">
              <div>
                <label className="label block mb-1">Player</label>
                <select className="input" value={selected} onChange={(e) => setSelected(e.target.value)}>
                  <option value="">— escolher —</option>
                  {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
                </select>
              </div>
              <div>
                <label className="label block mb-1">Tipo</label>
                <div className="tab-strip">
                  <div className={`tab-item ${kind === 'normal' ? 'active' : ''}`} onClick={() => setKind('normal')}>🎲 Normal</div>
                  <div className={`tab-item ${kind === 'damage' ? 'active' : ''}`} onClick={() => setKind('damage')}>💥 Dano</div>
                  <div className={`tab-item ${kind === 'decision' ? 'active' : ''}`} onClick={() => setKind('decision')}>⚖ Decisão</div>
                </div>
              </div>
            </div>

            <label className="label block mb-1">Dado</label>
            <div className="grid grid-cols-7 gap-1 mb-3">
              {DICE.map((n) => (
                <button key={n}
                  className={`btn-ghost btn-sm ${d === n ? '!bg-liberthia-500/30 !text-white' : ''}`}
                  onClick={() => setD(n)}
                >d{n}</button>
              ))}
            </div>

            <div className="grid grid-cols-2 gap-3 mb-3">
              <div>
                <label className="label block mb-1">Quantidade: {count}</label>
                <input type="range" min={1} max={10} value={count} onChange={(e) => setCount(Number(e.target.value))} className="w-full" />
              </div>
              <div>
                <label className="label block mb-1">Modificador: {mod >= 0 ? `+${mod}` : mod}</label>
                <input type="range" min={-20} max={20} value={mod} onChange={(e) => setMod(Number(e.target.value))} className="w-full" />
              </div>
            </div>

            {kind === 'decision' && (
              <div className="mb-3">
                <label className="label block mb-1">Threshold de sucesso: {decisionThreshold}</label>
                <input type="range" min={1} max={count * d + Math.abs(mod)} value={decisionThreshold}
                  onChange={(e) => setDecisionThreshold(Number(e.target.value))} className="w-full" />
                <div className="text-[10px] text-liberthia-300/50">
                  Min={count} · Max={count * d + mod} · Sucesso se total ≥ {decisionThreshold}
                </div>
              </div>
            )}

            <div className="text-center text-2xl font-mono mb-3 p-3 bg-liberthia-900/40 rounded-lg">
              <span className="text-liberthia-300/50">{count}</span>
              <span className="text-liberthia-300">d</span>
              <span className="text-liberthia-200">{d}</span>
              {mod !== 0 && (
                <span className={mod > 0 ? 'text-emerald-300' : 'text-red-300'}>
                  {mod > 0 ? ' + ' : ' '}{mod}
                </span>
              )}
            </div>

            <button className={rolling ? 'btn-amber w-full pulse-glow' : 'btn w-full'} onClick={roll}
              disabled={!selected || rolling}>
              {rolling ? '🎲 Rolando...' : '🎲 ROLAR'}
            </button>
          </div>

          {/* Preview do último */}
          {lastResult && (
            <div className="card-glow">
              <h3 className="font-bold mb-2">📊 Último resultado</h3>
              <MinecraftTitlePreview
                title={lastResult.critical === 'success' ? '§6§l✦ CRITICAL ✦' :
                       lastResult.critical === 'fail' ? '§4§l✗ FALHA CRÍTICA' :
                       lastResult.kind === 'damage' ? `§c§l💥 ${lastResult.total} dano` :
                       lastResult.kind === 'decision' ? (lastResult.total >= decisionThreshold ? '§a§l✓ SUCESSO' : '§c§l✗ FALHA') :
                       `§e§l🎲 ${lastResult.total}`}
                subtitle={`§7${lastResult.rolls.join(' + ')}${lastResult.modifier !== 0 ? (lastResult.modifier > 0 ? ' + ' + lastResult.modifier : ' ' + lastResult.modifier) : ''} = §l§f${lastResult.total}`}
                mode="title"
              />
            </div>
          )}
        </div>

        {/* Histórico */}
        <div className="card">
          <h3 className="font-bold mb-3">📜 Histórico</h3>
          {history.length === 0 && <p className="text-xs text-liberthia-300/50 italic">— sem rolagens —</p>}
          <div className="space-y-1.5 max-h-[80vh] overflow-y-auto">
            {history.map((r) => (
              <div key={r.id} className={`p-2 rounded-lg border text-xs ${
                r.critical === 'success' ? 'bg-amber-500/10 border-amber-400/30' :
                r.critical === 'fail' ? 'bg-red-500/10 border-red-400/30' :
                'bg-liberthia-900/40 border-liberthia-500/20'
              }`}>
                <div className="flex items-center gap-2">
                  <span className="font-bold">{r.player}</span>
                  <span className="font-mono text-liberthia-300/70">{r.expr}</span>
                  <span className="ml-auto text-2xl font-bold">{r.total}</span>
                </div>
                <div className="text-[10px] text-liberthia-300/50 font-mono">
                  [{r.rolls.join(', ')}]{r.modifier !== 0 ? (r.modifier > 0 ? ` + ${r.modifier}` : ` ${r.modifier}`) : ''} ·{' '}
                  {new Date(r.ts).toLocaleTimeString()}
                  {r.critical === 'success' && <span className="text-amber-300 font-bold ml-1">CRIT</span>}
                  {r.critical === 'fail' && <span className="text-red-300 font-bold ml-1">FAIL</span>}
                  {r.kind !== 'normal' && <span className="ml-1 chip">{r.kind}</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
