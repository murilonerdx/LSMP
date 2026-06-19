import { useEffect, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'
import { NumInput } from '../components/NumInput'

/**
 * Treasure Hunt — caça ao tesouro com pistas em sequência.
 *
 * Cada Hunt tem:
 *  - Pistas em ordem (texto + coord de destino + raio de "achou")
 *  - Tesouro final (lista de items dados via /give)
 *
 * Frontend monitora a posição do player escolhido e:
 *  1. Dá um book/clue ao iniciar (com a pista atual)
 *  2. Quando player chega no raio da pista, marca como achada, dá próxima pista
 *  3. Na última, libera o loot
 *  4. Pode renderizar partículas guia no local da pista atual
 */

type Clue = {
  text: string         // texto da pista (mostrado no book/title)
  x: number; y: number; z: number
  radius: number       // raio de detecção (blocos)
  reward?: string      // item opcional ao achar (id only, count 1)
}

type Loot = {
  item: string
  count: number
  nbt?: string         // SNBT extra (encantamentos, etc)
}

type Hunt = {
  id: string
  name: string
  emoji: string
  story: string        // contexto/intro
  clues: Clue[]
  loot: Loot[]
  particle: string     // partícula guia no local da pista
  updated: number
}

const ICONS = ['🏴‍☠', '💎', '🗝', '📜', '⚱', '🔮', '⚔', '🏆', '🌟', '🜲']

/** Presets pré-fabricados de caça ao tesouro. */
const TREASURE_PRESETS: Omit<Hunt, 'id' | 'updated'>[] = [
  {
    name: '🏴‍☠ O Tesouro Perdido do Pirata',
    emoji: '🏴‍☠',
    story: 'Há séculos, um pirata enterrou seu maior tesouro. Diziam que ele deixou pistas pelos cantos do reino. Os mais corajosos podem encontrá-lo.',
    clues: [
      { text: '§7§oPista 1:\n§r§e"Onde o sol nasce sobre as águas, lá começa minha jornada."', x: 200, y: 64, z: 0, radius: 8 },
      { text: '§7§oPista 2:\n§r§e"Procura na sombra da árvore mais alta — algo brilha sob a raiz."', x: 100, y: 70, z: 300, radius: 8 },
      { text: '§7§oPista 3:\n§r§e"O X final está onde os ossos repousam — entre os mortos, mas não com eles."', x: -50, y: 50, z: 400, radius: 6 },
    ],
    loot: [
      { item: 'minecraft:diamond', count: 16 },
      { item: 'minecraft:gold_ingot', count: 32 },
      { item: 'minecraft:emerald', count: 24 },
      { item: 'minecraft:enchanted_book', count: 1,
        nbt: '{StoredEnchantments:[{id:"minecraft:sharpness",lvl:5s}]}' },
    ],
    particle: 'minecraft:soul_fire_flame',
  },
  {
    name: '🔮 Caça ao Cristal Místico',
    emoji: '🔮',
    story: 'Um cristal antigo foi quebrado em 4 fragmentos espalhados pelo reino. Reúna todos pra desbloquear a magia perdida.',
    clues: [
      { text: '§5§oFragmento Norte:\n§r§d"O frio guarda o primeiro pedaço."', x: 0, y: 80, z: -500, radius: 10 },
      { text: '§5§oFragmento Sul:\n§r§d"O calor protege o segundo."', x: 0, y: 80, z: 500, radius: 10 },
      { text: '§5§oFragmento Leste:\n§r§d"A floresta esconde o terceiro."', x: 500, y: 80, z: 0, radius: 10 },
      { text: '§5§oFragmento Oeste:\n§r§d"O mar guarda o último."', x: -500, y: 70, z: 0, radius: 10 },
    ],
    loot: [
      { item: 'minecraft:end_crystal', count: 4 },
      { item: 'minecraft:beacon', count: 1 },
      { item: 'minecraft:nether_star', count: 1 },
    ],
    particle: 'minecraft:end_rod',
  },
  {
    name: '⚔ Trilha do Guerreiro',
    emoji: '⚔',
    story: 'Um cavaleiro lendário deixou pistas pra que apenas os fortes alcancem sua espada. Cada pista exige superar um perigo.',
    clues: [
      { text: '§4§oPista 1:\n§r§c"O início é onde derrotaste teu primeiro inimigo."', x: 50, y: 64, z: 50, radius: 5,
        reward: 'minecraft:iron_sword' },
      { text: '§4§oPista 2:\n§r§c"Sobreviva ao fogo do inferno — lá encontrarás o caminho."', x: 100, y: 50, z: 0, radius: 8,
        reward: 'minecraft:fire_resistance_potion' },
      { text: '§4§oPista 3:\n§r§c"O ápice fica onde a respiração é difícil. Olhe pra cima."', x: 200, y: 200, z: 100, radius: 8 },
    ],
    loot: [
      { item: 'minecraft:netherite_sword', count: 1,
        nbt: '{Enchantments:[{id:"minecraft:sharpness",lvl:5s},{id:"minecraft:unbreaking",lvl:3s},{id:"minecraft:mending",lvl:1s}],display:{Name:\'{"text":"Espada do Cavaleiro","color":"red","italic":false,"bold":true}\'}}' },
      { item: 'minecraft:netherite_chestplate', count: 1 },
      { item: 'minecraft:totem_of_undying', count: 3 },
    ],
    particle: 'minecraft:flame',
  },
  {
    name: '🌟 Estrelas do Céu',
    emoji: '🌟',
    story: 'Sete estrelas caíram do céu numa noite estrelada. Cada uma guarda uma bênção. Apenas quem coleta todas torna-se digno do prêmio celeste.',
    clues: [
      { text: '§e§oEstrela 1:\n§r§6"Norte distante, alta nas montanhas."', x: 0, y: 150, z: -300, radius: 10 },
      { text: '§e§oEstrela 2:\n§r§6"Profunda como a vergonha, no fundo do oceano."', x: 200, y: 30, z: 0, radius: 12 },
      { text: '§e§oEstrela 3:\n§r§6"Numa caverna esquecida, brilha solitária."', x: -100, y: 40, z: 100, radius: 8 },
      { text: '§e§oEstrela 4:\n§r§6"Onde o vento gira, ela paira."', x: 300, y: 100, z: 300, radius: 10 },
      { text: '§e§oEstrela 5:\n§r§6"No vazio do End, dança eterna."', x: 100, y: 60, z: 100, radius: 15 },
    ],
    loot: [
      { item: 'minecraft:nether_star', count: 1 },
      { item: 'minecraft:elytra', count: 1 },
      { item: 'minecraft:diamond_block', count: 7 },
      { item: 'minecraft:experience_bottle', count: 64 },
    ],
    particle: 'minecraft:end_rod',
  },
  {
    name: '📜 Os 3 Livros Antigos',
    emoji: '📜',
    story: 'Os antigos sábios deixaram 3 livros escondidos pelo mundo. Cada um ensina um segredo do universo.',
    clues: [
      { text: '§3§oLivro do Mar:\n§r§b"Bate-papo das ondas."', x: 100, y: 62, z: -200, radius: 12 },
      { text: '§3§oLivro da Terra:\n§r§b"Coração da montanha."', x: -300, y: 80, z: 100, radius: 10 },
      { text: '§3§oLivro do Céu:\n§r§b"Acima de tudo."', x: 0, y: 250, z: 0, radius: 15 },
    ],
    loot: [
      { item: 'minecraft:enchanted_book', count: 1,
        nbt: '{StoredEnchantments:[{id:"minecraft:mending",lvl:1s}],display:{Name:\'{"text":"Livro do Mar","color":"aqua","italic":false}\'}}' },
      { item: 'minecraft:enchanted_book', count: 1,
        nbt: '{StoredEnchantments:[{id:"minecraft:fortune",lvl:3s}],display:{Name:\'{"text":"Livro da Terra","color":"gold","italic":false}\'}}' },
      { item: 'minecraft:enchanted_book', count: 1,
        nbt: '{StoredEnchantments:[{id:"minecraft:feather_falling",lvl:4s}],display:{Name:\'{"text":"Livro do Céu","color":"white","italic":false}\'}}' },
    ],
    particle: 'minecraft:enchant',
  },
]

export function TreasureHuntPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 1500 })
  const players = playersQ.data ?? []

  const [hunts, setHunts] = useKvState<Hunt[]>('treasure_hunts', [])
  const [editing, setEditing] = useState<Hunt | null>(null)
  const [active, setActive] = useState<{ hunt: Hunt; player: string; clueIdx: number } | null>(null)

  // Detector: quando hunt ativa, monitora player e detecta proximity
  useEffect(() => {
    if (!active) return
    const player = players.find((p) => p.uuid === active.player)
    if (!player) return
    const clue = active.hunt.clues[active.clueIdx]
    if (!clue) return
    const dx = player.position.x - clue.x
    const dy = player.position.y - clue.y
    const dz = player.position.z - clue.z
    const dist = Math.sqrt(dx * dx + dy * dy + dz * dz)
    if (dist <= clue.radius) {
      onClueReached(active.hunt, active.clueIdx)
    } else {
      // Particle guide a cada ~2s seria pesado — façamos a cada poll (1.5s)
      // só se player estiver dentro de "warm range" (raio*4)
      if (dist <= clue.radius * 4) {
        api.particle(active.hunt.particle, clue.x, clue.y + 1, clue.z, 8).catch(() => {})
      }
    }
  }, [players, active])

  async function onClueReached(hunt: Hunt, clueIdx: number) {
    const clue = hunt.clues[clueIdx]
    const isLast = clueIdx === hunt.clues.length - 1
    const player = players.find((p) => p.uuid === active!.player)
    if (!player) return

    // Som de descoberta + título
    try {
      await api.sound(player.uuid, 'minecraft:entity.experience_orb.pickup', 1, 1.5)
      await api.title(player.uuid, '§a§l✓ Pista encontrada!', isLast ? '§eO tesouro está aqui...' : '§7Próxima pista chegando', 10, 60, 20)
    } catch {}
    // Reward intermediário (se tiver)
    if (clue.reward) {
      try { await api.give(player.uuid, clue.reward, 1) } catch {}
    }
    // Partícula explosão de celebração
    try { await api.particle('minecraft:firework', clue.x, clue.y + 1, clue.z, 40) } catch {}

    if (isLast) {
      // Entrega o loot final
      for (const l of hunt.loot) {
        try { await api.give(player.uuid, l.item, l.count) } catch {}
      }
      try {
        await api.title(player.uuid, '§6§l🏆 TESOURO!', `§e${hunt.loot.length} items recebidos`, 20, 100, 30)
        await api.sound(player.uuid, 'minecraft:ui.toast.challenge_complete', 1, 1)
      } catch {}
      toast.ok(`🏆 ${player.name} completou: ${hunt.name}`)
      setActive(null)
    } else {
      // Dá próxima pista
      const nextIdx = clueIdx + 1
      try {
        await api.title(player.uuid, '§e§l📜 Nova Pista', hunt.clues[nextIdx].text, 10, 100, 20)
      } catch {}
      setActive({ ...active!, clueIdx: nextIdx })
    }
  }

  async function start(hunt: Hunt, playerUuid: string) {
    if (!playerUuid) { toast.err('Selecione player'); return }
    if (hunt.clues.length === 0) { toast.err('Hunt sem pistas'); return }
    const player = players.find((p) => p.uuid === playerUuid)
    if (!player) return
    setActive({ hunt, player: playerUuid, clueIdx: 0 })
    try {
      await api.title(playerUuid, `§6§l${hunt.emoji} ${hunt.name}`, hunt.story.slice(0, 80), 20, 120, 30)
      await api.sound(playerUuid, 'minecraft:item.book.page_turn', 1, 0.8)
      await new Promise((r) => setTimeout(r, 4000))
      await api.title(playerUuid, '§e§l📜 Primeira Pista', hunt.clues[0].text, 10, 120, 20)
    } catch {}
    toast.ok(`🏴‍☠ Hunt iniciada pra ${player.name}`)
  }

  function newHunt() {
    setEditing({
      id: `hunt_${Date.now().toString(36)}`,
      name: 'A Relíquia Perdida',
      emoji: '🏴‍☠',
      story: '§7Dizem que numa caverna a leste, sob a árvore tombada, está enterrada uma chave...',
      clues: [{ text: '§7Procure a árvore tombada perto da água', x: 0, y: 64, z: 0, radius: 8 }],
      loot: [{ item: 'minecraft:diamond', count: 3 }],
      particle: 'minecraft:end_rod',
      updated: Date.now(),
    })
  }

  function commit(h: Hunt) {
    setHunts((cur) => {
      const i = cur.findIndex((x) => x.id === h.id)
      if (i >= 0) { const n = [...cur]; n[i] = h; return n }
      return [...cur, h]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🏴‍☠ Treasure Hunts</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Caça ao tesouro com pistas geográficas. Player se aproxima da coord → próxima pista. Final libera loot.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => {
            const newHunts: Hunt[] = TREASURE_PRESETS.map((p, i) => ({
              ...p,
              id: 'preset-' + Date.now() + '-' + i,
              updated: Date.now(),
            }))
            setHunts((cur) => [...cur, ...newHunts])
            toast.ok(`📥 ${newHunts.length} caças importadas (ajuste coordenadas antes de iniciar)`)
          }}>📥 Importar {TREASURE_PRESETS.length} presets</button>
          <button className="btn" onClick={newHunt}>+ Nova Hunt</button>
        </div>
      </header>

      {active && (
        <div className="card-glow mb-4 !border-emerald-400/40 !bg-emerald-500/5">
          <div className="flex items-center gap-3 flex-wrap">
            <div className="text-3xl">{active.hunt.emoji}</div>
            <div className="flex-1">
              <div className="font-bold text-lg">{active.hunt.name}</div>
              <div className="text-xs text-liberthia-300/70">
                Player: {players.find((p) => p.uuid === active.player)?.name} ·
                Pista <b>{active.clueIdx + 1}/{active.hunt.clues.length}</b>
              </div>
              <div className="text-xs mt-1">📜 {renderMcText(active.hunt.clues[active.clueIdx]?.text ?? '')}</div>
              <div className="text-[10px] text-liberthia-300/50 font-mono mt-1">
                destino: {active.hunt.clues[active.clueIdx]?.x}, {active.hunt.clues[active.clueIdx]?.y}, {active.hunt.clues[active.clueIdx]?.z}
                · raio {active.hunt.clues[active.clueIdx]?.radius}b
              </div>
            </div>
            <span className="live-dot" />
            <button className="btn-danger btn-sm" onClick={() => setActive(null)}>⏹ Cancelar</button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {hunts.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">🗺</div>
            <p className="text-liberthia-300/70">Nenhuma hunt criada. Bora?</p>
          </div>
        )}
        {hunts.map((h) => (
          <div key={h.id} className="card-glow">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">{h.emoji}</span>
              <div className="flex-1 min-w-0">
                <div className="font-bold truncate">{h.name}</div>
                <div className="text-[10px] text-liberthia-300/50">
                  {h.clues.length} pistas · {h.loot.length} items de loot
                </div>
              </div>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(h)}>✎</button>
              <button className="btn-ghost btn-sm" onClick={() => setHunts((cur) => cur.filter((x) => x.id !== h.id))}>🗑</button>
            </div>
            <div className="text-xs text-liberthia-300/70 mb-3 line-clamp-2 min-h-[32px]">
              {renderMcText(h.story)}
            </div>
            <div className="grid grid-cols-[1fr_auto] gap-2">
              <select className="input text-xs" disabled={!!active}
                onChange={(e) => { if (e.target.value) { start(h, e.target.value); e.target.value = '' } }}>
                <option value="">▶ Iniciar pra...</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
            </div>
          </div>
        ))}
      </div>

      {editing && <HuntEditor hunt={editing} players={players} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function HuntEditor({ hunt, players, onSave, onCancel }: {
  hunt: Hunt; players: any[]; onSave: (h: Hunt) => void; onCancel: () => void
}) {
  const [h, setH] = useState<Hunt>(hunt)

  function updateClue(i: number, patch: Partial<Clue>) {
    setH({ ...h, clues: h.clues.map((c, j) => j === i ? { ...c, ...patch } : c) })
  }
  function updateLoot(i: number, patch: Partial<Loot>) {
    setH({ ...h, loot: h.loot.map((l, j) => j === i ? { ...l, ...patch } : l) })
  }
  function captureFromPlayer(uuid: string, clueIdx: number) {
    const p = players.find((pl) => pl.uuid === uuid); if (!p) return
    updateClue(clueIdx, { x: Math.round(p.position.x), y: Math.round(p.position.y), z: Math.round(p.position.z) })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🏴‍☠ Hunt</h3>

        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <select className="input text-2xl text-center" value={h.emoji} onChange={(e) => setH({ ...h, emoji: e.target.value })}>
            {ICONS.map((i) => <option key={i} value={i}>{i}</option>)}
          </select>
          <input className="input" value={h.name} onChange={(e) => setH({ ...h, name: e.target.value })} />
        </div>

        <label className="label block mb-1">História/contexto (intro)</label>
        <MinecraftFormatter value={h.story} onChange={(v) => setH({ ...h, story: v })} rows={3} maxChars={300} />

        <label className="label block mb-1 mt-3">Partícula guia</label>
        <select className="input mb-4 text-xs font-mono" value={h.particle} onChange={(e) => setH({ ...h, particle: e.target.value })}>
          <option value="minecraft:end_rod">end_rod (branco)</option>
          <option value="minecraft:flame">flame (laranja)</option>
          <option value="minecraft:soul_fire_flame">soul_flame (ciano)</option>
          <option value="minecraft:dragon_breath">dragon_breath (roxo)</option>
          <option value="minecraft:totem_of_undying">totem (verde/laranja)</option>
          <option value="minecraft:heart">heart</option>
        </select>

        {/* Clues */}
        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">📜 Pistas ({h.clues.length})</h4>
          <button className="btn-ghost btn-sm" onClick={() => setH({ ...h, clues: [...h.clues, { text: '§7Próxima pista...', x: 0, y: 64, z: 0, radius: 8 }] })}>+ Pista</button>
        </div>
        <div className="space-y-2 mb-4">
          {h.clues.map((c, i) => (
            <div key={i} className="border border-liberthia-500/20 rounded-xl p-3 bg-liberthia-900/40">
              <div className="flex items-center gap-2 mb-2">
                <span className="badge badge-purple">#{i + 1}</span>
                <select className="input text-xs flex-1"
                  onChange={(e) => { if (e.target.value) { captureFromPlayer(e.target.value, i); e.target.value = '' } }}>
                  <option value="">📍 Coord de player...</option>
                  {players.map((p: any) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
                </select>
                <button className="btn-ghost btn-sm" onClick={() => setH({ ...h, clues: h.clues.filter((_, j) => j !== i) })}>🗑</button>
              </div>
              <MinecraftFormatter value={c.text} onChange={(v) => updateClue(i, { text: v })} rows={2} maxChars={120} showCounter={false} />
              <div className="grid grid-cols-4 gap-2 mt-2">
                <div>
                  <label className="label text-[10px]">X</label>
                  <NumInput className="input text-xs" value={c.x} onChange={(v) => updateClue(i, { x: v })} />
                </div>
                <div>
                  <label className="label text-[10px]">Y</label>
                  <NumInput className="input text-xs" value={c.y} onChange={(v) => updateClue(i, { y: v })} />
                </div>
                <div>
                  <label className="label text-[10px]">Z</label>
                  <NumInput className="input text-xs" value={c.z} onChange={(v) => updateClue(i, { z: v })} />
                </div>
                <div>
                  <label className="label text-[10px]">Raio</label>
                  <input type="number" className="input text-xs" value={c.radius} min={1} onChange={(e) => updateClue(i, { radius: Number(e.target.value) })} />
                </div>
              </div>
              <input className="input mt-2 text-xs font-mono" placeholder="reward intermediário opcional (ex: minecraft:emerald)"
                value={c.reward ?? ''} onChange={(e) => updateClue(i, { reward: e.target.value || undefined })} />
            </div>
          ))}
        </div>

        {/* Loot */}
        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">🏆 Loot final</h4>
          <button className="btn-ghost btn-sm" onClick={() => setH({ ...h, loot: [...h.loot, { item: 'minecraft:diamond', count: 1 }] })}>+ Item</button>
        </div>
        <div className="space-y-1 mb-4">
          {h.loot.map((l, i) => (
            <div key={i} className="flex items-center gap-2">
              <input className="input flex-1 text-xs font-mono" value={l.item} placeholder="minecraft:diamond"
                onChange={(e) => updateLoot(i, { item: e.target.value })} />
              <input type="number" className="input w-20 text-xs" value={l.count} min={1} max={64}
                onChange={(e) => updateLoot(i, { count: Math.max(1, Math.min(64, Number(e.target.value))) })} />
              <button className="btn-ghost btn-sm" onClick={() => setH({ ...h, loot: h.loot.filter((_, j) => j !== i) })}>🗑</button>
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave({ ...h, updated: Date.now() })}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
