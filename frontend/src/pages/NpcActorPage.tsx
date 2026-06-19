import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, MC_COLORS } from '../components/MinecraftFormatter'
import { NumInput } from '../components/NumInput'
import { useKvState } from '../lib/kvState'

/**
 * NPC Actor: spawna entidades vanilla com nome custom, NoAI, equipamento.
 *
 * Cada actor recebe Tags:["liberthia_npc","liberthia_npc_<id>"] no NBT.
 * Toda ação (kill, effect, walk-away, falar) usa @e[tag=...] em vez de name
 * — não quebra com cores § no CustomName.
 */

type Actor = {
  id: string
  name: string
  type: string
  x: number; y: number; z: number
  yaw: number
  customNameColor: string
  noAi: boolean
  invulnerable: boolean
  silent: boolean
  glow: boolean
  helmet?: string
  mainHand?: string
}


const PRESET_TYPES = [
  ['minecraft:villager', '🧑 Villager'],
  ['minecraft:zombie', '🧟 Zombie'],
  ['minecraft:skeleton', '💀 Skeleton'],
  ['minecraft:wither_skeleton', '☠ Wither Skeleton'],
  ['minecraft:armor_stand', '🗿 Armor Stand'],
  ['minecraft:iron_golem', '🤖 Iron Golem'],
  ['minecraft:enderman', '👻 Enderman'],
  ['minecraft:vindicator', '⚔ Vindicator'],
  ['minecraft:evoker', '🧙 Evoker'],
  ['minecraft:witch', '🧙‍♀ Witch'],
] as const

const EFFECT_PRESETS = [
  { id: 'minecraft:glowing', emoji: '✨', label: 'Glow', dur: 600, amp: 0 },
  { id: 'minecraft:invisibility', emoji: '👻', label: 'Invisível', dur: 600, amp: 0 },
  { id: 'minecraft:speed', emoji: '💨', label: 'Speed', dur: 300, amp: 2 },
  { id: 'minecraft:slowness', emoji: '🐌', label: 'Slow', dur: 300, amp: 1 },
  { id: 'minecraft:levitation', emoji: '🪶', label: 'Levitar', dur: 120, amp: 1 },
  { id: 'minecraft:fire_resistance', emoji: '🛡', label: 'FireRes', dur: 9999, amp: 0 },
]

export function NpcActorPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []

  const [actors, setActors] = useKvState<Actor[]>('npc_actors', [])
  const [editing, setEditing] = useState<Actor | null>(null)
  const [speaking, setSpeaking] = useState<Actor | null>(null)
  const [acting, setActing] = useState<Actor | null>(null)

  function tagSelector(a: Actor) { return `@e[tag=liberthia_npc_${a.id},limit=1]` }
  function tagSelectorAll(a: Actor) { return `@e[tag=liberthia_npc_${a.id}]` }

  function addNew() {
    setEditing({
      id: `n${Date.now().toString(36)}`,
      name: 'Mira',
      type: 'minecraft:villager',
      x: 0, y: 80, z: 0, yaw: 0,
      customNameColor: '§e',
      noAi: true, invulnerable: true, silent: false, glow: false,
    })
  }

  async function captureFrom(uuid: string) {
    const list = await api.players()
    const p = list.find((x) => x.uuid === uuid); if (!p) return
    setEditing({
      id: `n${Date.now().toString(36)}`,
      name: 'NPC',
      type: 'minecraft:villager',
      x: Math.round(p.position.x), y: Math.round(p.position.y), z: Math.round(p.position.z),
      yaw: Math.round(p.position.yaw),
      customNameColor: '§e',
      noAi: true, invulnerable: true, silent: false, glow: false,
    })
  }

  async function spawn(a: Actor) {
    // Tags identificadoras + universal "liberthia_npc"
    const tag: string[] = []
    tag.push(`CustomName:'{"text":"${a.customNameColor}${a.name.replace(/"/g, '\\"')}"}'`)
    tag.push(`CustomNameVisible:1b`)
    tag.push(`Tags:["liberthia_npc","liberthia_npc_${a.id}"]`)
    if (a.noAi) tag.push('NoAI:1b')
    if (a.invulnerable) tag.push('Invulnerable:1b')
    if (a.silent) tag.push('Silent:1b')
    if (a.glow) tag.push('Glowing:1b')
    if (a.helmet || a.mainHand) {
      const armor = ['{}', '{}', '{}', a.helmet ? `{id:"${a.helmet}",Count:1}` : '{}']
      tag.push(`ArmorItems:[${armor.join(',')}]`)
      const hand = a.mainHand ? `[{id:"${a.mainHand}",Count:1},{}]` : `[{},{}]`
      tag.push(`HandItems:${hand}`)
    }
    tag.push(`Rotation:[${a.yaw}f,0f]`)
    // Mata instâncias antigas do mesmo id (limpo, sem chat)
    try { await api.command(`kill ${tagSelectorAll(a)}`, 'npc') } catch {}
    const cmd = `summon ${a.type} ${a.x} ${a.y} ${a.z} {${tag.join(',')}}`
    try { await api.command(cmd, 'npc'); toast.ok(`${a.name} spawnado`) }
    catch (e: any) { toast.err(e.message) }
  }

  async function killActor(a: Actor) {
    try {
      // Mata por tag (funciona mesmo com armor_stand ou nomes coloridos)
      await api.command(`kill ${tagSelectorAll(a)}`, 'npc')
      toast.ok(`${a.name} morto`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function killAllNpcs() {
    try {
      await api.command(`kill @e[tag=liberthia_npc]`, 'npc')
      toast.ok('Todos NPCs Liberthia mortos')
    } catch (e: any) { toast.err(e.message) }
  }

  async function applyEffect(a: Actor, eff: typeof EFFECT_PRESETS[number]) {
    try {
      await api.command(`effect give ${tagSelector(a)} ${eff.id} ${eff.dur} ${eff.amp} true`, 'npc')
      toast.ok(`${eff.emoji} ${eff.label} em ${a.name}`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function clearEffects(a: Actor) {
    try {
      await api.command(`effect clear ${tagSelector(a)}`, 'npc')
      toast.ok('Efeitos limpos')
    } catch (e: any) { toast.err(e.message) }
  }

  /** TP do operador pra perto do NPC (útil pra encontrar onde foi colocado). */
  async function tpToNpc(a: Actor, playerUuid: string) {
    if (!playerUuid) { toast.err('Selecione um player'); return }
    const list = await api.players()
    const p = list.find((x) => x.uuid === playerUuid); if (!p) return
    try {
      await api.teleport(playerUuid, a.x, a.y + 1, a.z)
      toast.ok(`${p.name} → ${a.name}`)
    } catch (e: any) { toast.err(e.message) }
  }

  /** Rotaciona o NPC pra encarar um player (one-shot). */
  async function lookAt(a: Actor, playerUuid: string) {
    const list = await api.players()
    const p = list.find((x) => x.uuid === playerUuid); if (!p) return
    const dx = p.position.x - a.x
    const dz = p.position.z - a.z
    const yaw = Math.atan2(-dx, dz) * 180 / Math.PI
    try {
      await api.command(`data merge entity ${tagSelector(a)} {Rotation:[${yaw.toFixed(1)}f,0f]}`, 'npc')
      toast.ok(`${a.name} olhando pra ${p.name}`)
    } catch (e: any) { toast.err(e.message) }
  }

  /** Animação de TP do NPC pra uma coordenada (caminha até lá). */
  async function moveTo(a: Actor, tx: number, ty: number, tz: number) {
    const STEPS = 20
    const STEP_MS = 200
    for (let i = 1; i <= STEPS; i++) {
      const t = i / STEPS
      const nx = a.x + (tx - a.x) * t
      const ny = a.y + (ty - a.y) * t
      const nz = a.z + (tz - a.z) * t
      try { await api.command(`tp ${tagSelector(a)} ${nx.toFixed(2)} ${ny.toFixed(2)} ${nz.toFixed(2)}`, 'npc') } catch {}
      await new Promise((r) => setTimeout(r, STEP_MS))
    }
    // Atualiza coord salva
    setActors((cur) => cur.map((x) => x.id === a.id ? { ...x, x: Math.round(tx), y: Math.round(ty), z: Math.round(tz) } : x))
    toast.ok(`${a.name} chegou em ${Math.round(tx)},${Math.round(ty)},${Math.round(tz)}`)
  }

  /** Verifica se NPC está vivo no mundo. data get entity returna result>=1 se existe. */
  async function statusCheck(a: Actor) {
    try {
      const r = await api.command(`data get entity ${tagSelector(a)} UUID`, 'npc-check')
      const alive = (r as any)?.result >= 1
      toast.info(alive ? `✓ ${a.name} está vivo` : `✗ ${a.name} não está no mundo`)
    } catch { toast.info(`✗ ${a.name} não está no mundo`) }
  }

  /**
   * Walk-away: anima o NPC indo embora ~30 blocos na direção do yaw,
   * com som de passos. Depois remove silenciosamente (sem death anim).
   * Funciona com NoAI=1 pq usa /tp.
   */
  async function walkAway(a: Actor) {
    const STEPS = 30
    const STEP_BLOCKS = 1.0
    const STEP_MS = 200
    const yawRad = ((a.yaw + 180) * Math.PI) / 180
    const dx = Math.sin(yawRad)
    const dz = -Math.cos(yawRad)
    toast.info(`🚶 ${a.name} indo embora...`)
    for (let i = 1; i <= STEPS; i++) {
      const nx = a.x + dx * STEP_BLOCKS * i
      const nz = a.z + dz * STEP_BLOCKS * i
      try {
        await api.command(`tp ${tagSelector(a)} ${nx.toFixed(2)} ${a.y} ${nz.toFixed(2)} ${a.yaw} 0`, 'npc')
        if (i % 3 === 0) {
          await api.command(`playsound minecraft:entity.player.attack.weak master @a[distance=..20,x=${nx.toFixed(0)},y=${a.y},z=${nz.toFixed(0)}] ${nx} ${a.y} ${nz} 0.3`, 'npc')
        }
      } catch {}
      await new Promise((r) => setTimeout(r, STEP_MS))
    }
    // Fade out: invisibility 5s, depois despawn silencioso
    try {
      await api.command(`effect give ${tagSelector(a)} minecraft:invisibility 100 0 true`, 'npc')
      await new Promise((r) => setTimeout(r, 2500))
      await api.command(`data merge entity ${tagSelector(a)} {DeathLootTable:"minecraft:empty"}`, 'npc')
      await api.command(`kill ${tagSelectorAll(a)}`, 'npc')
      toast.ok(`${a.name} sumiu sem deixar rastro`)
    } catch {}
  }

  function commit(a: Actor) {
    setActors((cur) => {
      const i = cur.findIndex((x) => x.id === a.id)
      if (i >= 0) { const n = [...cur]; n[i] = a; return n }
      return [...cur, a]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎭 NPC Actors</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Elenco do servidor. Cada actor tem tag única — kill/effect/talk funciona mesmo com nome colorido.
          </p>
        </div>
        <div className="flex gap-2">
          <select className="input text-xs w-48" onChange={(e) => { if (e.target.value) { captureFrom(e.target.value); e.target.value = '' } }}>
            <option value="">📍 Capturar coords...</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
          </select>
          <button className="btn-danger btn-sm" onClick={killAllNpcs} title="Mata todos NPCs Liberthia">💀 Kill all</button>
          <button className="btn" onClick={addNew}>+ Novo Actor</button>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {actors.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">🎭</div>
            <p className="text-liberthia-300/70">Nenhum actor criado.</p>
          </div>
        )}
        {actors.map((a) => (
          <div key={a.id} className="card-glow">
            <div className="flex items-start gap-2 mb-2">
              <span className="text-2xl">{PRESET_TYPES.find((p) => p[0] === a.type)?.[1].split(' ')[0] ?? '🎭'}</span>
              <div className="flex-1 min-w-0">
                <div className="font-bold text-base truncate" style={{ color: hexFromCode(a.customNameColor) }}>{a.name}</div>
                <div className="text-[10px] text-liberthia-300/50 font-mono">{a.type.replace('minecraft:', '')} · id:{a.id}</div>
              </div>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(a)} title="Editar">✎</button>
              <button className="btn-ghost btn-sm" onClick={() => setActors((c) => c.filter((x) => x.id !== a.id))} title="Remover do elenco">🗑</button>
            </div>
            <div className="text-xs font-mono text-liberthia-300/70 mb-3">
              📍 {a.x}, {a.y}, {a.z} · yaw {a.yaw}°
            </div>
            <div className="flex flex-wrap gap-1 mb-3">
              {a.noAi && <span className="chip">NoAI</span>}
              {a.invulnerable && <span className="chip">Invuln</span>}
              {a.silent && <span className="chip">Silent</span>}
              {a.glow && <span className="chip">Glow</span>}
              {a.helmet && <span className="chip">⛑</span>}
              {a.mainHand && <span className="chip">⚔</span>}
            </div>
            <div className="grid grid-cols-2 gap-1.5 mb-2">
              <button className="btn-success btn-sm" onClick={() => spawn(a)} title="Spawna (mata anterior + cria fresh)">▶ Spawn</button>
              <button className="btn-cyan btn-sm" onClick={() => setSpeaking(a)}>💬 Falar</button>
            </div>
            <div className="grid grid-cols-3 gap-1.5 mb-1.5">
              <button className="btn-ghost btn-sm" onClick={() => statusCheck(a)} title="Está vivo?">🔍 Status</button>
              <button className="btn-ghost btn-sm" onClick={() => setActing(a)} title="Ações avançadas">🎬 Ações</button>
              <details className="relative">
                <summary className="btn-ghost btn-sm cursor-pointer list-none text-center">⚗ Efeito ▾</summary>
                <div className="absolute right-0 mt-1 bg-liberthia-900/95 border border-liberthia-500/30 rounded-xl p-2 z-10 shadow-2xl backdrop-blur-md min-w-[160px]">
                  {EFFECT_PRESETS.map((eff) => (
                    <button key={eff.id}
                      className="btn-ghost btn-sm w-full text-left mb-0.5"
                      onClick={() => applyEffect(a, eff)}
                    >{eff.emoji} {eff.label}</button>
                  ))}
                  <button className="btn-ghost btn-sm w-full text-left mb-0.5 mt-1 border-t border-liberthia-500/20 pt-1"
                          onClick={() => clearEffects(a)}>🧹 Limpar</button>
                </div>
              </details>
            </div>
            <div className="grid grid-cols-2 gap-1.5">
              <button className="btn-amber btn-sm" onClick={() => walkAway(a)} title="Anda embora e some sem morrer">🚶 Ir embora</button>
              <button className="btn-danger btn-sm" onClick={() => killActor(a)} title="Mata (via tag)">💀 Kill</button>
            </div>
          </div>
        ))}
      </div>

      {editing && <ActorEditor actor={editing} onSave={commit} onCancel={() => setEditing(null)} />}
      {speaking && <SpeakModal actor={speaking} onClose={() => setSpeaking(null)} />}
      {acting && (
        <ActionsModal
          actor={acting}
          players={players}
          onClose={() => setActing(null)}
          onLookAt={(uuid) => lookAt(acting, uuid)}
          onTpToNpc={(uuid) => tpToNpc(acting, uuid)}
          onMoveTo={(x, y, z) => moveTo(acting, x, y, z)}
        />
      )}
    </div>
  )
}

// ============= Actions Modal =============

function ActionsModal({ actor, players, onClose, onLookAt, onTpToNpc, onMoveTo }: {
  actor: Actor
  players: import('../lib/api').Player[]
  onClose: () => void
  onLookAt: (uuid: string) => void
  onTpToNpc: (uuid: string) => void
  onMoveTo: (x: number, y: number, z: number) => void
}) {
  const [target, setTarget] = useState(players[0]?.uuid ?? '')
  const [destX, setDestX] = useState(actor.x)
  const [destY, setDestY] = useState(actor.y)
  const [destZ, setDestZ] = useState(actor.z)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onClose}>
      <div className="card max-w-lg w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4 flex items-center gap-2">
          🎬 Ações — <span style={{ color: hexFromCode(actor.customNameColor) }}>{actor.name}</span>
        </h3>

        <div className="space-y-3">
          {/* Olhar pra player */}
          <div className="card !p-3">
            <div className="font-bold text-sm mb-2">👁 Olhar pra player</div>
            <div className="flex gap-2">
              <select className="input flex-1 text-xs" value={target} onChange={(e) => setTarget(e.target.value)}>
                <option value="">— player —</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <button className="btn-cyan btn-sm" onClick={() => target && onLookAt(target)} disabled={!target}>Olhar</button>
            </div>
          </div>

          {/* TP player até o NPC */}
          <div className="card !p-3">
            <div className="font-bold text-sm mb-2">🌀 TP player até o NPC</div>
            <div className="flex gap-2">
              <select className="input flex-1 text-xs" value={target} onChange={(e) => setTarget(e.target.value)}>
                <option value="">— player —</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <button className="btn-cyan btn-sm" onClick={() => target && onTpToNpc(target)} disabled={!target}>TP</button>
            </div>
          </div>

          {/* Mover NPC pra coord */}
          <div className="card !p-3">
            <div className="font-bold text-sm mb-2">🚶 Mover NPC pra coordenada</div>
            <div className="grid grid-cols-3 gap-2 mb-2">
              <NumInput className="input text-xs" placeholder="X" value={destX} onChange={setDestX} />
              <NumInput className="input text-xs" placeholder="Y" value={destY} onChange={setDestY} />
              <NumInput className="input text-xs" placeholder="Z" value={destZ} onChange={setDestZ} />
            </div>
            <div className="flex gap-2">
              <select className="input text-xs flex-1" onChange={(e) => {
                if (!e.target.value) return
                const p = players.find((x) => x.uuid === e.target.value)
                if (p) {
                  setDestX(Math.round(p.position.x))
                  setDestY(Math.round(p.position.y))
                  setDestZ(Math.round(p.position.z))
                }
                e.target.value = ''
              }}>
                <option value="">📍 Capturar de player...</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <button className="btn btn-sm" onClick={() => onMoveTo(destX, destY, destZ)}>Andar até lá</button>
            </div>
            <div className="text-[10px] text-liberthia-300/50 mt-1">
              Anima ~4s (20 steps). Atualiza a coord salva ao chegar.
            </div>
          </div>
        </div>

        <div className="flex gap-2 justify-end mt-4">
          <button className="btn-ghost" onClick={onClose}>Fechar</button>
        </div>
      </div>
    </div>
  )
}

// ============= Speak Modal =============

function SpeakModal({ actor, onClose }: { actor: Actor; onClose: () => void }) {
  const [text, setText] = useState('§fOlá viajante...')
  const [mode, setMode] = useState<'chat-near' | 'chat-global' | 'subtitle-near'>('chat-near')
  const [radius, setRadius] = useState(15)
  const [busy, setBusy] = useState(false)
  const [history, setHistory] = useState<{ ts: number; text: string; recipients: number }[]>([])

  async function send() {
    if (!text.trim()) return
    setBusy(true)
    try {
      if (mode === 'chat-near' || mode === 'subtitle-near') {
        const body: any = {
          x: actor.x, y: actor.y, z: actor.z,
          radius,
          speaker: actor.customNameColor + actor.name,
        }
        if (mode === 'chat-near') body.message = text
        else body.subtitle = text
        const r = await api.whisper(body)
        setHistory((h) => [{ ts: Date.now(), text, recipients: r.recipients?.length ?? 0 }, ...h].slice(0, 20))
        toast.ok(`📡 ${r.recipients?.length ?? 0} ouviram`)
      } else {
        // Global tellraw (todos players)
        const tellraw = JSON.stringify([
          { text: `[${actor.customNameColor}${actor.name}§r] `, color: 'white' },
          { text },
        ])
        await api.command(`tellraw @a ${tellraw}`, 'npc')
        setHistory((h) => [{ ts: Date.now(), text, recipients: -1 }, ...h].slice(0, 20))
        toast.ok('📢 Mensagem global enviada')
      }
      setText('')
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onClose}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-3 mb-4">
          <div className="text-3xl">💬</div>
          <div>
            <h3 className="font-bold text-lg" style={{ color: hexFromCode(actor.customNameColor) }}>{actor.name}</h3>
            <div className="text-xs text-liberthia-300/60 font-mono">{actor.x}, {actor.y}, {actor.z}</div>
          </div>
        </div>

        <label className="label block mb-1">Modo</label>
        <div className="tab-strip mb-3 flex-wrap">
          <div className={`tab-item ${mode === 'chat-near' ? 'active' : ''}`} onClick={() => setMode('chat-near')}>💬 Chat (perto)</div>
          <div className={`tab-item ${mode === 'subtitle-near' ? 'active' : ''}`} onClick={() => setMode('subtitle-near')}>📺 Subtitle (perto)</div>
          <div className={`tab-item ${mode === 'chat-global' ? 'active' : ''}`} onClick={() => setMode('chat-global')}>📢 Global</div>
        </div>

        {mode !== 'chat-global' && (
          <div className="flex items-center gap-2 mb-3">
            <span className="label">Raio:</span>
            <input type="range" min={3} max={64} value={radius} onChange={(e) => setRadius(Number(e.target.value))} className="flex-1" />
            <span className="text-xs font-mono w-12 text-right">{radius}b</span>
          </div>
        )}

        <MinecraftFormatter
          value={text}
          onChange={setText}
          rows={3}
          placeholder="O que o NPC vai dizer..."
        />

        <div className="flex items-center gap-2 justify-end mt-4">
          <button className="btn-ghost" onClick={onClose}>Fechar</button>
          <button className="btn" onClick={send} disabled={busy || !text.trim()}>📡 Enviar</button>
        </div>

        {history.length > 0 && (
          <div className="mt-4 pt-3 border-t border-liberthia-500/20">
            <div className="label mb-2">Histórico desta sessão</div>
            <div className="space-y-1 max-h-40 overflow-y-auto text-xs font-mono">
              {history.map((h, i) => (
                <div key={i} className="px-2 py-1 rounded bg-liberthia-900/40 flex gap-2">
                  <span className="text-liberthia-300/40">{new Date(h.ts).toLocaleTimeString()}</span>
                  <span className="text-liberthia-300/60">
                    {h.recipients < 0 ? '@a' : `${h.recipients}p`}
                  </span>
                  <span className="flex-1 truncate">{h.text}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

// ============= Actor Editor =============

function ActorEditor({ actor, onSave, onCancel }: { actor: Actor; onSave: (a: Actor) => void; onCancel: () => void }) {
  const [a, setA] = useState<Actor>(actor)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🎭 Actor</h3>

        <div className="grid grid-cols-2 gap-2 mb-3">
          <div className="col-span-2">
            <label className="label block mb-1">Nome</label>
            <input className="input" value={a.name} onChange={(e) => setA({ ...a, name: e.target.value })} />
          </div>
          <div className="col-span-2">
            <label className="label block mb-1">Cor do nome</label>
            <div className="flex flex-wrap gap-1 p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
              {MC_COLORS.map((c) => (
                <button key={c.code} type="button" title={c.label}
                  onClick={() => setA({ ...a, customNameColor: c.code })}
                  className={`w-7 h-7 rounded border transition ${a.customNameColor === c.code ? 'ring-2 ring-white scale-110' : 'border-white/20'}`}
                  style={{ background: c.hex }}
                />
              ))}
            </div>
            <div className="mt-1 text-xs">
              Preview: <span style={{ color: hexFromCode(a.customNameColor) }} className="font-bold">{a.name}</span>
            </div>
          </div>
          <div className="col-span-2">
            <label className="label block mb-1">Tipo</label>
            <select className="input" value={a.type} onChange={(e) => setA({ ...a, type: e.target.value })}>
              {PRESET_TYPES.map(([id, lbl]) => <option key={id} value={id}>{lbl}</option>)}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-4 gap-2 mb-3">
          <div>
            <label className="label block mb-1">X</label>
            <NumInput value={a.x} onChange={(v) => setA({ ...a, x: v })} />
          </div>
          <div>
            <label className="label block mb-1">Y</label>
            <NumInput value={a.y} onChange={(v) => setA({ ...a, y: v })} />
          </div>
          <div>
            <label className="label block mb-1">Z</label>
            <NumInput value={a.z} onChange={(v) => setA({ ...a, z: v })} />
          </div>
          <div>
            <label className="label block mb-1">Yaw</label>
            <NumInput value={a.yaw} onChange={(v) => setA({ ...a, yaw: v })} />
          </div>
        </div>

        <div className="flex flex-wrap gap-3 mb-3 text-xs">
          <label className="flex items-center gap-1"><input type="checkbox" checked={a.noAi} onChange={(e) => setA({ ...a, noAi: e.target.checked })} /> NoAI (estátua)</label>
          <label className="flex items-center gap-1"><input type="checkbox" checked={a.invulnerable} onChange={(e) => setA({ ...a, invulnerable: e.target.checked })} /> Invulnerável</label>
          <label className="flex items-center gap-1"><input type="checkbox" checked={a.silent} onChange={(e) => setA({ ...a, silent: e.target.checked })} /> Silent</label>
          <label className="flex items-center gap-1"><input type="checkbox" checked={a.glow} onChange={(e) => setA({ ...a, glow: e.target.checked })} /> Glow</label>
        </div>

        <div className="grid grid-cols-2 gap-2 mb-4">
          <input className="input text-xs font-mono" placeholder="helmet (ex: minecraft:diamond_helmet)" value={a.helmet ?? ''} onChange={(e) => setA({ ...a, helmet: e.target.value })} />
          <input className="input text-xs font-mono" placeholder="mainhand (ex: minecraft:netherite_sword)" value={a.mainHand ?? ''} onChange={(e) => setA({ ...a, mainHand: e.target.value })} />
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(a)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

function hexFromCode(code: string): string {
  return MC_COLORS.find((c) => c.code === code)?.hex ?? '#fff'
}
