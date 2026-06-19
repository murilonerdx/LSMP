import { useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'
import { NumInput } from '../components/NumInput'
import { PlayerPosPicker } from '../components/PlayerPosPicker'

/**
 * Memorial — pedras/monumentos permanentes com inscrição. Estrutura:
 *  - 1 pilar (4 blocos de altura) do material escolhido
 *  - Sign no topo com texto multilinha
 *  - Armor stand invisível com CustomName em §-codes flutuando acima
 *  - Tochas/glow nos cantos pra destaque
 *
 * Persiste posições pra poder remover/reapply.
 */

type Memorial = {
  id: string
  emoji: string
  title: string        // nome em §-codes (vai no holograma)
  inscription: string  // texto do holograma multi-linha
  x: number; y: number; z: number
  block: string        // bloco do pilar
  hasGlowing: boolean  // tochas com glow
  hasLightning: boolean // pequeno raio cerimonial ao colocar
  /** Player honored — quando setado, spawna um clone-player (corpo + skin) em cima do pilar. */
  honoredPlayer?: string
  /** Rotação do clone player em graus (0 = norte). */
  honoredRotation?: number
  created: number
}

const BLOCK_PRESETS = [
  ['minecraft:deepslate_tile_wall', '🪨 Deepslate Tile Wall'],
  ['minecraft:polished_blackstone_wall', '⬛ Polished Blackstone'],
  ['minecraft:cobblestone_wall', '🪨 Cobblestone Wall'],
  ['minecraft:mossy_cobblestone_wall', '🌿 Mossy Cobble'],
  ['minecraft:end_stone_brick_wall', '🟡 End Stone Brick'],
  ['minecraft:nether_brick_wall', '🟫 Nether Brick'],
  ['minecraft:red_sandstone_wall', '🟥 Red Sandstone'],
  ['minecraft:prismarine_wall', '🌊 Prismarine'],
] as const

/** Presets pré-fabricados de memoriais (templates). Click pra duplicar com seu UUID. */
const MEMORIAL_PRESETS: Omit<Memorial, 'id' | 'created' | 'x' | 'y' | 'z'>[] = [
  {
    emoji: '👑',
    title: '§6§l⚜ Fundador do Reino ⚜',
    inscription: '§e§oAqui jaz aquele\n§eque ergueu as torres\n§eantes do tempo dos tempos.\n\n§6"sua memória ilumina os caminhos."',
    block: 'minecraft:polished_blackstone_wall',
    hasGlowing: true,
    hasLightning: true,
  },
  {
    emoji: '⚔',
    title: '§4§l⚔ Guerreiro Caído ⚔',
    inscription: '§c§oTombou em batalha,\n§cmas não em vão.\n\n§4"o aço lembra do seu nome."',
    block: 'minecraft:nether_brick_wall',
    hasGlowing: false,
    hasLightning: true,
  },
  {
    emoji: '📖',
    title: '§b§l✦ Cronista Eterno ✦',
    inscription: '§3§oCada palavra escrita\n§3é uma alma preservada.\n\n§b"escreva e nunca seja esquecido."',
    block: 'minecraft:prismarine_wall',
    hasGlowing: true,
    hasLightning: false,
  },
  {
    emoji: '🌌',
    title: '§5§l🜲 Tocado pelo Vazio 🜲',
    inscription: '§5§oOlhou o abismo\n§5e o abismo respondeu.\n\n§d"silêncio é resposta também."',
    block: 'minecraft:deepslate_tile_wall',
    hasGlowing: true,
    hasLightning: true,
  },
  {
    emoji: '🌳',
    title: '§a§l🌿 Guardião das Florestas 🌿',
    inscription: '§2§oPlantou árvores\n§2que viverão mais que ele.\n\n§a"a raiz não esquece."',
    block: 'minecraft:mossy_cobblestone_wall',
    hasGlowing: false,
    hasLightning: false,
  },
  {
    emoji: '🩸',
    title: '§c§l⚱ Mártir Sangrento ⚱',
    inscription: '§4§oDeu seu sangue\n§4pelo que acreditava.\n\n§c"o que se sacrifica não se perde."',
    block: 'minecraft:red_sandstone_wall',
    hasGlowing: true,
    hasLightning: true,
  },
  {
    emoji: '🎭',
    title: '§d§l🎭 Bardo das Mil Vozes 🎭',
    inscription: '§5§oContou histórias\n§5que ainda ressoam.\n\n§d"suas canções vivem."',
    block: 'minecraft:end_stone_brick_wall',
    hasGlowing: true,
    hasLightning: false,
  },
  {
    emoji: '⛓',
    title: '§8§l⛓ O Desaparecido ⛓',
    inscription: '§7§oNinguém sabe pra onde foi.\n§7Mas algo ficou.\n\n§8"a ausência também é presença."',
    block: 'minecraft:cobblestone_wall',
    hasGlowing: false,
    hasLightning: false,
  },
]

export function MemorialPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [memorials, setMemorials] = useKvState<Memorial[]>('memorials', [])
  const [editing, setEditing] = useState<Memorial | null>(null)

  async function build(m: Memorial) {
    try {
      // Limpa volume antigo (área 3x8x3 ao redor) + mata armor stands antigos do mesmo memorial
      const tagSelf = `memorial_${m.id}`
      try { await api.killByTag(tagSelf) } catch {} // fallback se backend velho
      await api.command(`kill @e[tag=${tagSelf}]`, 'memorial')
      await api.command(`fill ${m.x - 1} ${m.y} ${m.z - 1} ${m.x + 1} ${m.y + 7} ${m.z + 1} minecraft:air`, 'memorial')
      // Pilar 4 blocos
      await api.command(`fill ${m.x} ${m.y} ${m.z} ${m.x} ${m.y + 3} ${m.z} ${m.block}`, 'memorial')

      // Estátua do honored: ClonePlayerEntity (renderiza como player real, com
      // PlayerModel + skin) em cima do pilar (feet em y+4, cabeça ~y+5.8).
      let titleY = m.y + 5.5
      if (m.honoredPlayer && m.honoredPlayer.trim().length > 0) {
        await api.spawnPlayerClone({
          x: m.x + 0.5,
          y: m.y + 4, // em cima do pilar (que termina em y+3)
          z: m.z + 0.5,
          playerName: m.honoredPlayer.trim(),
          rotation: m.honoredRotation ?? 180,
          tag: `liberthia_memorial,${tagSelf}`,
        })
        // Sobe o holograma pra ficar acima da cabeça do clone
        titleY = m.y + 6.3
      }

      // Hologram: pilha de armor stands invisíveis com CustomName visível.
      // Cada armor stand é uma "linha" flutuando. Espaçamento ~0.28 blocks
      // (~altura do texto MC) pra ficar legível e bem coladinho.
      const TAGS = `["liberthia_memorial","${tagSelf}"]`
      const LINE_GAP = 0.28

      // 1) Título flutuando primeiro (mais alto)
      const titleNameComp = JSON.stringify({ text: m.title }).replace(/'/g, "\\'")
      const titleNbt = `{Invisible:1b,Marker:1b,NoGravity:1b,CustomName:'${titleNameComp}',CustomNameVisible:1b,Tags:${TAGS}}`
      await api.command(`summon armor_stand ${m.x + 0.5} ${titleY.toFixed(2)} ${m.z + 0.5} ${titleNbt}`, 'memorial')

      // 2) Inscrição: cada linha vira um armor stand invisível abaixo do título.
      //    Linhas vazias VIRAM espaçamento (pulam um slot Y).
      const lines = m.inscription.split('\n')
      let slot = 1
      for (const rawLine of lines) {
        const line = rawLine ?? ''
        const lineY = titleY - LINE_GAP * slot
        if (line.trim().length > 0) {
          const nameComp = JSON.stringify({ text: line }).replace(/'/g, "\\'")
          const standNbt = `{Invisible:1b,Marker:1b,NoGravity:1b,CustomName:'${nameComp}',CustomNameVisible:1b,Tags:${TAGS}}`
          await api.command(`summon armor_stand ${m.x + 0.5} ${lineY.toFixed(2)} ${m.z + 0.5} ${standNbt}`, 'memorial')
        }
        slot += 1
      }

      // Tochas nos cantos
      if (m.hasGlowing) {
        for (const [dx, dz] of [[-1, -1], [-1, 1], [1, -1], [1, 1]]) {
          await api.command(`setblock ${m.x + dx} ${m.y} ${m.z + dz} minecraft:soul_torch`, 'memorial')
        }
      }
      // Raio cerimonial
      if (m.hasLightning) {
        await api.command(`summon lightning_bolt ${m.x} ${m.y + 6} ${m.z}`, 'memorial')
      }
      toast.ok(`🗿 Memorial "${m.title}" construído`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function demolish(m: Memorial) {
    try {
      await api.command(`fill ${m.x - 1} ${m.y} ${m.z - 1} ${m.x + 1} ${m.y + 7} ${m.z + 1} minecraft:air`, 'memorial')
      const tag = `memorial_${m.id}`
      try { await api.killByTag(tag) } catch {} // pega ClonePlayerEntity em qualquer dim
      await api.command(`kill @e[tag=${tag}]`, 'memorial')
      toast.ok('🗑 Demolido')
    } catch (e: any) { toast.err(e.message) }
  }

  function newMemorial() {
    setEditing({
      id: `mem_${Date.now().toString(36)}`,
      emoji: '🗿',
      title: '§7§oIn Memoriam',
      inscription: '§lAqui repousa\n§rum herói\nde §dLiberthia',
      x: 0, y: 70, z: 0,
      block: 'minecraft:deepslate_tile_wall',
      hasGlowing: true, hasLightning: true,
      created: Date.now(),
    })
  }

  function commit(m: Memorial) {
    setMemorials((cur) => {
      const i = cur.findIndex((x) => x.id === m.id)
      if (i >= 0) { const n = [...cur]; n[i] = m; return n }
      return [...cur, m]
    })
    setEditing(null)
  }

  async function tpTo(m: Memorial, playerUuid: string) {
    if (!playerUuid) return
    try { await api.teleport(playerUuid, m.x, m.y + 1, m.z); toast.ok('🌀 TP') } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🗿 Memoriais</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Pilares permanentes com inscrição. Marca lugares importantes: mortes, batalhas, descobertas.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => {
            const newOnes: Memorial[] = MEMORIAL_PRESETS.map((p, i) => ({
              ...p,
              id: `preset-${Date.now()}-${i}`,
              x: 0, y: 64, z: 0,
              created: Date.now(),
            }))
            setMemorials((cur) => [...cur, ...newOnes])
            toast.ok(`📥 ${newOnes.length} presets importados (defina posição antes de buildar)`)
          }}>📥 Importar {MEMORIAL_PRESETS.length} presets</button>
          <button className="btn" onClick={newMemorial}>+ Novo Memorial</button>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {memorials.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">🗿</div>
            <p className="text-liberthia-300/70">Nenhum memorial registrado ainda.</p>
          </div>
        )}
        {memorials.map((m) => (
          <div key={m.id} className="card-glow">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-3xl">{m.emoji}</span>
              <div className="flex-1 min-w-0">
                <div className="font-bold truncate">{renderMcText(m.title)}</div>
                <div className="text-[10px] text-liberthia-300/50 font-mono">
                  📍 {m.x}, {m.y}, {m.z} · {new Date(m.created).toLocaleDateString()}
                </div>
              </div>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(m)}>✎</button>
            </div>
            <div className="bg-liberthia-900/40 rounded-lg p-2 mb-3 text-xs whitespace-pre-wrap break-words min-h-[60px] border border-liberthia-500/20">
              {renderMcText(m.inscription)}
            </div>
            <div className="grid grid-cols-3 gap-1">
              <button className="btn-success btn-sm" onClick={() => build(m)}>🏗 Build</button>
              <select className="input text-xs"
                onChange={(e) => { if (e.target.value) { tpTo(m, e.target.value); e.target.value = '' } }}>
                <option value="">🌀 TP</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <button className="btn-danger btn-sm" onClick={() => demolish(m)}>💥</button>
            </div>
            <button className="btn-ghost btn-sm w-full mt-1" onClick={() => setMemorials((cur) => cur.filter((x) => x.id !== m.id))}>🗑 Remover do catálogo</button>
          </div>
        ))}
      </div>

      {editing && (
        <MemEditor mem={editing} onSave={commit} onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function MemEditor({ mem, onSave, onCancel }: {
  mem: Memorial; onSave: (m: Memorial) => void; onCancel: () => void
}) {
  const [m, setM] = useState<Memorial>(mem)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🗿 Memorial</h3>

        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <input className="input text-2xl text-center" value={m.emoji} onChange={(e) => setM({ ...m, emoji: e.target.value })} />
          <div>
            <label className="label block mb-1">Título (flutua sobre o pilar)</label>
            <MinecraftFormatter value={m.title} onChange={(v) => setM({ ...m, title: v })} rows={1} maxChars={80} showCounter={false} />
          </div>
        </div>

        <label className="label block mb-1">Inscrição (cada linha = uma linha do holograma; vazia = espaço)</label>
        <MinecraftFormatter value={m.inscription} onChange={(v) => setM({ ...m, inscription: v })} rows={6} maxChars={400} />

        <div className="grid grid-cols-3 gap-2 mt-3 mb-2">
          <div>
            <label className="label block mb-1">X</label>
            <NumInput value={m.x} onChange={(v) => setM({ ...m, x: v })} />
          </div>
          <div>
            <label className="label block mb-1">Y</label>
            <NumInput value={m.y} onChange={(v) => setM({ ...m, y: v })} />
          </div>
          <div>
            <label className="label block mb-1">Z</label>
            <NumInput value={m.z} onChange={(v) => setM({ ...m, z: v })} />
          </div>
        </div>
        <div className="mb-3">
          <label className="label block mb-1">📍 Usar pos do player</label>
          <PlayerPosPicker onPick={(p) =>
            setM({ ...m, x: Math.round(p.x), y: Math.round(p.y), z: Math.round(p.z) })
          } />
        </div>

        <label className="label block mb-1">Bloco do pilar</label>
        <select className="input mb-3 font-mono text-xs" value={m.block} onChange={(e) => setM({ ...m, block: e.target.value })}>
          {BLOCK_PRESETS.map(([id, lbl]) => <option key={id} value={id}>{lbl}</option>)}
        </select>

        <div className="card !bg-purple-500/5 !border-purple-400/30 mb-3">
          <label className="label block mb-1">👤 Estátua do player honored (opcional)</label>
          <p className="text-[10px] text-liberthia-300/60 mb-2">
            Spawna uma <b>cópia do corpo do player</b> (PlayerModel + skin real) em cima do pilar, em
            vez de armor-stand. Use o nome exato (server resolve via cache de profiles).
          </p>
          <div className="grid grid-cols-[1fr_100px] gap-2">
            <input className="input text-xs" placeholder="ex: Steve (ou deixe vazio pra só holograma)"
              value={m.honoredPlayer ?? ''}
              onChange={(e) => setM({ ...m, honoredPlayer: e.target.value })} />
            <div>
              <input type="number" className="input text-xs" placeholder="rot°" min={0} max={360}
                value={m.honoredRotation ?? 180}
                onChange={(e) => setM({ ...m, honoredRotation: Number(e.target.value) })} />
            </div>
          </div>
        </div>

        <div className="flex gap-3 text-xs mb-4">
          <label className="flex items-center gap-1 cursor-pointer">
            <input type="checkbox" checked={m.hasGlowing} onChange={(e) => setM({ ...m, hasGlowing: e.target.checked })} />
            Tochas de alma nos cantos
          </label>
          <label className="flex items-center gap-1 cursor-pointer">
            <input type="checkbox" checked={m.hasLightning} onChange={(e) => setM({ ...m, hasLightning: e.target.checked })} />
            Raio cerimonial ao construir
          </label>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(m)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
