import { useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'

/**
 * Prophecy Cards — admin "tira" uma carta aleatória pra um player.
 * Apresentação: virada de carta dramática (title) + som + book entregue.
 *
 * Cada carta tem: emoji, título, descrição curta, profecia longa (vai no book).
 * Lista persistida em localStorage, admin pode customizar todas.
 */

type Card = {
  id: string
  emoji: string
  title: string
  description: string
  prophecy: string
  weight: number    // probabilidade relativa (1 = normal, 5 = comum, 0.1 = rara)
  color: string     // §-code pro título
}

const DEFAULT_CARDS: Card[] = [
  { id: 'c1', emoji: '🌟', title: 'A Estrela', description: 'Esperança renasce', prophecy: '§eVocê é a luz que outros buscam.\nUm caminho se abrirá quando menos esperar.\nNão olhe pra trás — o passado já lhe deu o que tinha pra dar.', weight: 1, color: '§e' },
  { id: 'c2', emoji: '🜨', title: 'O Vazio', description: 'Algo te observa', prophecy: '§5Há olhos nas sombras que conhecem seu nome.\nO que você esconde retornará em forma de pergunta.\nFique alerta nas próximas três noites.', weight: 0.5, color: '§5' },
  { id: 'c3', emoji: '⚔', title: 'A Lâmina', description: 'Conflito é inevitável', prophecy: '§cA batalha que você evita já começou.\nSeu silêncio não é paz — é munição pros outros.\nLevante-se ou seja varrido.', weight: 1, color: '§c' },
  { id: 'c4', emoji: '🌙', title: 'A Lua', description: 'Mistério guia', prophecy: '§9Confie no que sente, não no que vê.\nUm sonho próximo carrega a chave que você busca.\nA noite revela o que o dia esconde.', weight: 1, color: '§9' },
  { id: 'c5', emoji: '🪶', title: 'O Vento', description: 'Mudança vem', prophecy: '§bDeixe o que estava firme cair.\nO que sobrar é o essencial — e o essencial voa.\nUm viajante chegará trazendo notícias.', weight: 1, color: '§b' },
  { id: 'c6', emoji: '🩸', title: 'O Pacto', description: 'Vínculo de sangue', prophecy: '§4Você está mais ligado a alguém do que percebe.\nA dor dessa pessoa será sua dor.\nMas também a glória dela.', weight: 0.3, color: '§4' },
  { id: 'c7', emoji: '🌳', title: 'A Árvore', description: 'Raízes profundas', prophecy: '§aSeu legado não é o que você construiu — é quem você plantou.\nDeixe sementes mesmo quando não verá flores.\nUma geração à frente vai colher.', weight: 1, color: '§a' },
  { id: 'c8', emoji: '👁', title: 'O Olho', description: 'Verdade exposta', prophecy: '§dUma máscara que você usa será removida em público.\nMas a vergonha esperada não virá — virá liberdade.\nDeixe que vejam.', weight: 0.7, color: '§d' },
  { id: 'c9', emoji: '🜲', title: 'O Selo', description: 'Algo selado', prophecy: '§8Uma força adormecida em algum lugar do mundo te espera.\nQuando ouvir um sino sem badalo, vá ao norte.\nLeve o que herdou.', weight: 0.2, color: '§8' },
  { id: 'c10', emoji: '✨', title: 'A Bênção', description: 'Sorte sobe', prophecy: '§6Os próximos sete dias são seus.\nO que tentar prosperará.\nNão desperdice — a maré desce depois.', weight: 0.5, color: '§6' },
]

export function ProphecyPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [cards, setCards] = useKvState<Card[]>('prophecies', DEFAULT_CARDS)
  const [editing, setEditing] = useState<Card | null>(null)
  const [history, setHistory] = useState<{ ts: number; player: string; card: Card }[]>([])

  function pickCard(): Card | null {
    if (cards.length === 0) return null
    const total = cards.reduce((a, c) => a + c.weight, 0)
    let r = Math.random() * total
    for (const c of cards) {
      r -= c.weight
      if (r <= 0) return c
    }
    return cards[cards.length - 1]
  }

  async function drawFor(playerUuid: string) {
    const player = players.find((p) => p.uuid === playerUuid); if (!player) return
    const card = pickCard(); if (!card) { toast.err('Sem cartas no baralho'); return }
    setHistory((h) => [{ ts: Date.now(), player: player.name, card }, ...h].slice(0, 50))
    // Dramatização
    try {
      await api.title(player.uuid, '§7🔮 §oUma carta é virada...', '', 8, 30, 5)
      await api.sound(player.uuid, 'minecraft:block.amethyst_block.chime', 1, 0.6)
      await new Promise((r) => setTimeout(r, 1800))
      await api.title(player.uuid, `${card.emoji} ${card.color}§l${card.title}`, `§7${card.description}`, 15, 120, 30)
      await api.sound(player.uuid, 'minecraft:block.bell.use', 1, 1.2)
      await new Promise((r) => setTimeout(r, 4000))
      // Entrega como livro
      const pages = [
        `§7§oUma carta foi tirada\npara você§r\n\n${card.emoji}\n\n${card.color}§l${card.title}§r\n\n§o${card.description}§r`,
        card.prophecy,
        '§8§o— Liberthia —§r',
      ]
      const escapedPages = pages.map((p) => JSON.stringify(JSON.stringify({ text: p }))).join(',')
      await api.command(`give ${player.name} written_book{title:"§5Profecia",author:"O Oráculo",pages:[${escapedPages}]} 1`, 'prophecy')
      toast.ok(`🔮 ${card.title} → ${player.name}`)
    } catch (e: any) { toast.err(e.message) }
  }

  function newCard() {
    setEditing({
      id: `c${Date.now().toString(36)}`,
      emoji: '🌟', title: 'Nova Carta', description: '...',
      prophecy: '§7Texto da profecia...', weight: 1, color: '§e',
    })
  }

  function commit(c: Card) {
    setCards((cur) => {
      const i = cur.findIndex((x) => x.id === c.id)
      if (i >= 0) { const n = [...cur]; n[i] = c; return n }
      return [...cur, c]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🔮 Profecias</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Baralho de cartas com narrativa. Sorteia uma pra um player — entrega como livro + cena dramática.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost btn-sm" onClick={() => setCards(DEFAULT_CARDS)}>↺ Reset deck padrão</button>
          <button className="btn" onClick={newCard}>+ Nova Carta</button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        <div className="space-y-3">
          {/* Sortear */}
          <div className="card-glow">
            <h3 className="font-bold mb-3">🎴 Sortear pra player</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {players.map((p) => (
                <button key={p.uuid} className="btn" onClick={() => drawFor(p.uuid)}>
                  🔮 {p.name}
                </button>
              ))}
              {players.length === 0 && <p className="col-span-full text-xs text-liberthia-300/50 italic">Ninguém online</p>}
            </div>
          </div>

          {/* Deck */}
          <div className="card-glow">
            <h3 className="font-bold mb-3">🃏 Baralho ({cards.length})</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
              {cards.map((c) => (
                <div key={c.id} className="border border-liberthia-500/20 rounded-xl p-3 bg-liberthia-900/40">
                  <div className="text-3xl text-center mb-1">{c.emoji}</div>
                  <div className="text-center font-bold mb-1">{renderMcText(c.color + c.title)}</div>
                  <div className="text-xs text-liberthia-300/70 text-center mb-2 min-h-[24px]">{c.description}</div>
                  <div className="flex items-center justify-between text-[10px]">
                    <span className="chip">peso {c.weight}</span>
                    <div className="flex gap-1">
                      <button className="btn-ghost btn-sm" onClick={() => setEditing(c)}>✎</button>
                      <button className="btn-ghost btn-sm" onClick={() => setCards((cur) => cur.filter((x) => x.id !== c.id))}>🗑</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">📜 Histórico de sorteios</h3>
          <div className="space-y-1 max-h-96 overflow-y-auto text-xs">
            {history.length === 0 && <p className="italic text-liberthia-300/50">— vazio —</p>}
            {history.map((h, i) => (
              <div key={i} className="p-1.5 rounded bg-liberthia-900/40 border border-liberthia-500/10">
                <div className="flex items-center gap-1">
                  <span>{h.card.emoji}</span>
                  <span className="font-bold">{renderMcText(h.card.color + h.card.title)}</span>
                  <span className="text-liberthia-300/50">→ {h.player}</span>
                  <span className="text-liberthia-300/40 ml-auto text-[10px]">{new Date(h.ts).toLocaleTimeString()}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {editing && <CardEditor card={editing} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function CardEditor({ card, onSave, onCancel }: { card: Card; onSave: (c: Card) => void; onCancel: () => void }) {
  const [c, setC] = useState<Card>(card)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🃏 Carta</h3>
        <div className="grid grid-cols-[60px_60px_1fr] gap-2 mb-3">
          <input className="input text-2xl text-center" value={c.emoji} onChange={(e) => setC({ ...c, emoji: e.target.value })} />
          <input className="input text-center font-mono" value={c.color} onChange={(e) => setC({ ...c, color: e.target.value })} />
          <input className="input font-bold" value={c.title} onChange={(e) => setC({ ...c, title: e.target.value })} placeholder="A Estrela" />
        </div>
        <input className="input mb-3 text-xs" value={c.description} onChange={(e) => setC({ ...c, description: e.target.value })} placeholder="Descrição curta" />
        <label className="label block mb-1">Profecia (entregue no livro)</label>
        <MinecraftFormatter value={c.prophecy} onChange={(v) => setC({ ...c, prophecy: v })} rows={5} maxChars={500} />
        <label className="label block mb-1 mt-3">Peso (probabilidade relativa)</label>
        <input type="number" step={0.1} min={0} className="input mb-4" value={c.weight} onChange={(e) => setC({ ...c, weight: Number(e.target.value) })} />
        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(c)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
