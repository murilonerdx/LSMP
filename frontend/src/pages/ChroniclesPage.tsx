import { useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'

/**
 * Chronicles — registro público da história do servidor.
 *
 * Cada entrada tem:
 *  - data (in-game ou real)
 *  - emoji/ícone
 *  - título
 *  - texto/descrição
 *  - tipo (lore, war, treaty, death, discovery, ascension, etc)
 *
 * Persiste em localStorage. Pode ser:
 *  - exportado como livro vanilla (entrega ao player)
 *  - mostrado em chat (broadcast formatado)
 *  - servido em tela cheia pra um player (sequência de titles)
 */

type ChronicleKind =
  | 'lore'      // 📜 evento de lore
  | 'war'       // ⚔ guerra/conflito
  | 'treaty'    // 🤝 tratado/aliança
  | 'death'     // 💀 morte importante
  | 'discovery' // 🔮 descoberta
  | 'ascension' // ✨ ascensão
  | 'omen'      // 🜨 presságio
  | 'festival'  // 🎉 festival

type Entry = {
  id: string
  kind: ChronicleKind
  title: string
  body: string
  era: string         // "Era da Sombra", "Ano 3 da Liberthia", etc
  realDate: number    // timestamp
  pinned?: boolean
}

const KIND_META: Record<ChronicleKind, { emoji: string; label: string; color: string }> = {
  lore:      { emoji: '📜', label: 'Lore',       color: '#d8b4fe' },
  war:       { emoji: '⚔', label: 'Guerra',     color: '#ff5555' },
  treaty:    { emoji: '🤝', label: 'Tratado',    color: '#55ff55' },
  death:     { emoji: '💀', label: 'Morte',      color: '#aaaaaa' },
  discovery: { emoji: '🔮', label: 'Descoberta', color: '#55ffff' },
  ascension: { emoji: '✨', label: 'Ascensão',   color: '#ffff55' },
  omen:      { emoji: '🜨', label: 'Presságio',  color: '#aa00aa' },
  festival:  { emoji: '🎉', label: 'Festival',   color: '#ffaa00' },
}

/** Templates de entradas para arrancar a história do servidor. */
const CHRONICLE_PRESETS: Omit<Entry, 'id' | 'realDate'>[] = [
  {
    kind: 'lore',
    title: '§5§lO Despertar das Sombras',
    body: '§7No princípio do terceiro ciclo, as sombras voltaram a sussurrar dos cantos antigos. Os primeiros a sentir foram os mineiros — relataram vozes vindas do escuro.\n\n§8§oO que veio depois disso ninguém pode contar inteiro.',
    era: 'Era da Sombra · Ano 1',
    pinned: true,
  },
  {
    kind: 'war',
    title: '§4§lA Guerra dos Cristais',
    body: '§cDois clãs disputaram por gerações o controle da Mina do Cristal Negro. A guerra durou §o3 luas§r§c, deixou §o47 caídos§r§c e um pacto frágil.\n\n§7Hoje, a mina é território neutro — mas a tensão nunca desapareceu.',
    era: 'Era da Sombra · Ano 2',
  },
  {
    kind: 'treaty',
    title: '§a§lO Pacto da Aurora',
    body: '§2Após a Guerra dos Cristais, os líderes assinaram §lo Pacto da Aurora§r§2:\n\n§7§o"Que nenhuma alma derrame sangue por matéria escura. Que o que está enterrado, permaneça enterrado."',
    era: 'Era da Sombra · Ano 2',
    pinned: true,
  },
  {
    kind: 'death',
    title: '§8§lA Queda do Primeiro Cronista',
    body: '§7O Cronista §o[NOME_AQUI]§r§7 caiu na §o Caverna do Vazio§r§7. Seus escritos foram recuperados, mas seu corpo nunca encontrado.\n\n§8§oAlguns juram ainda ouvir sua pena rabiscando à noite.',
    era: 'Era da Sombra · Ano 3',
  },
  {
    kind: 'discovery',
    title: '§b§lA Descoberta do Portal Sem Volta',
    body: '§3Em §o[DATA]§r§3, exploradores encontraram um portal antigo em coords §o[X,Y,Z]§r§3. Ninguém que entrou voltou.\n\n§7§oOu pelo menos, voltou como antes.',
    era: 'Era da Sombra · Ano 3',
  },
  {
    kind: 'ascension',
    title: '§6§l✦ A Ascensão de §e[NOME] §6§l✦',
    body: '§eApós cumprir todos os 7 ritos sagrados, §o[NOME]§r§e foi tocado pela Aurora.\n\n§6§o"Não morreu. Apenas deixou de ser daqui."',
    era: 'Era da Aurora · Ano 1',
    pinned: true,
  },
  {
    kind: 'omen',
    title: '§5§l🜨 O Presságio da Lua Vermelha',
    body: '§5Em §o[DATA]§r§5, a lua se tingiu de sangue por 3 noites seguidas. Os Oráculos previam catástrofe.\n\n§dE a catástrofe veio.',
    era: 'Era da Sombra · Ano 4',
  },
  {
    kind: 'festival',
    title: '§e§l🎉 O Primeiro Festival da Colheita',
    body: '§6Em §o[DATA]§r§6, todos os clãs depuseram suas armas pra comemorar a colheita. Houve §o vinho, dança e canto§r§6 sob o céu estrelado.\n\n§eFoi o último ano em que isso aconteceu.',
    era: 'Era da Sombra · Ano 2',
  },
]

export function ChroniclesPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [entries, setEntries] = useKvState<Entry[]>('chronicles', [])
  const [editing, setEditing] = useState<Entry | null>(null)
  const [filter, setFilter] = useState<ChronicleKind | 'all'>('all')

  const filtered = entries
    .filter((e) => filter === 'all' || e.kind === filter)
    .sort((a, b) => {
      if (a.pinned && !b.pinned) return -1
      if (!a.pinned && b.pinned) return 1
      return b.realDate - a.realDate
    })

  function newEntry() {
    setEditing({
      id: `chr_${Date.now().toString(36)}`,
      kind: 'lore',
      title: 'O Despertar das Sombras',
      body: '§7No início do terceiro ciclo, as sombras voltaram a sussurrar...',
      era: 'Era da Sombra',
      realDate: Date.now(),
    })
  }

  function commit(e: Entry) {
    setEntries((cur) => {
      const i = cur.findIndex((x) => x.id === e.id)
      if (i >= 0) { const n = [...cur]; n[i] = e; return n }
      return [...cur, e]
    })
    setEditing(null)
    toast.ok('Crônica registrada')
  }

  /** Anuncia crônica no chat com formatação grande. */
  async function announce(e: Entry) {
    const meta = KIND_META[e.kind]
    try {
      // Header em chat
      await api.command(`tellraw @a ${JSON.stringify({ text: '\n§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' })}`, 'chronicle')
      await api.command(`tellraw @a ${JSON.stringify({ text: `${meta.emoji} §lCRÔNICA — ${meta.label.toUpperCase()}§r` })}`, 'chronicle')
      await api.command(`tellraw @a ${JSON.stringify({ text: `§o${e.era}§r` })}`, 'chronicle')
      await api.command(`tellraw @a ${JSON.stringify({ text: `§l${e.title}` })}`, 'chronicle')
      // Body em chunks de 80 chars pra não quebrar
      const lines = e.body.split('\n')
      for (const l of lines) {
        if (l.trim()) await api.command(`tellraw @a ${JSON.stringify({ text: l })}`, 'chronicle')
      }
      await api.command(`tellraw @a ${JSON.stringify({ text: '§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n' })}`, 'chronicle')
      // Som dramático
      await api.command(`playsound minecraft:item.book.page_turn master @a ~ ~ ~ 1 0.7`, 'chronicle')
      toast.ok('📢 Crônica anunciada')
    } catch (err: any) { toast.err(err.message) }
  }

  /** Mostra crônica como title cinematográfico (todos players). */
  async function presentAsTitle(e: Entry) {
    const meta = KIND_META[e.kind]
    const uuids = players.map((p) => p.uuid)
    try {
      for (const u of uuids) {
        await api.title(u, `§7${e.era}`, ' ', 10, 60, 10)
      }
      await new Promise((r) => setTimeout(r, 2500))
      for (const u of uuids) {
        await api.title(u, `${meta.emoji} §l${e.title}`, '§o' + e.body.slice(0, 60), 15, 120, 30)
        api.sound(u, 'minecraft:block.amethyst_block.chime', 1, 0.6).catch(() => {})
      }
      toast.ok('🎬 Crônica apresentada')
    } catch (err: any) { toast.err(err.message) }
  }

  /** Exporta crônica como written_book pro player. */
  async function giveBook(e: Entry, playerUuid: string) {
    if (!playerUuid) return
    const player = players.find((p) => p.uuid === playerUuid); if (!player) return
    const meta = KIND_META[e.kind]
    const pages: string[] = []
    pages.push(`§7§o${e.era}§r\n\n${meta.emoji} §l${e.title}§r\n\n§8${new Date(e.realDate).toLocaleDateString()}§r`)
    // Quebra body em páginas de ~260 chars
    const body = e.body
    let pos = 0
    while (pos < body.length) {
      pages.push(body.slice(pos, pos + 260))
      pos += 260
    }
    const escapedPages = pages.map((p) => JSON.stringify(JSON.stringify({ text: p }))).join(',')
    const cmd = `give ${player.name} written_book{title:"§${e.kind === 'death' ? '8' : '5'}${e.title.replace(/"/g, '\\"').slice(0, 16)}",author:"Crônicas",pages:[${escapedPages}]} 1`
    try {
      await api.command(cmd, 'chronicle')
      toast.ok(`📖 Livro entregue a ${player.name}`)
    } catch (err: any) { toast.err(err.message) }
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📚 Chronicles</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Registro permanente da história do servidor. Anuncie, transforme em livro, ou apresente como cutscene.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => {
            const newOnes: Entry[] = CHRONICLE_PRESETS.map((p, i) => ({
              ...p,
              id: 'chr_preset_' + Date.now().toString(36) + '_' + i,
              realDate: Date.now() - i * 86400000,  // espalha pra trás 1 dia cada
            }))
            setEntries((cur) => [...cur, ...newOnes])
            toast.ok(`📥 ${newOnes.length} crônicas importadas`)
          }}>📥 Importar {CHRONICLE_PRESETS.length} presets</button>
          <button className="btn" onClick={newEntry}>+ Nova Crônica</button>
        </div>
      </header>

      {/* Filtros */}
      <div className="flex items-center gap-2 mb-4 flex-wrap">
        <button className={`btn-ghost btn-sm ${filter === 'all' ? '!bg-liberthia-500/30 !text-white' : ''}`}
                onClick={() => setFilter('all')}>Todas ({entries.length})</button>
        {Object.entries(KIND_META).map(([k, m]) => {
          const n = entries.filter((e) => e.kind === k).length
          if (n === 0) return null
          return (
            <button key={k}
              className={`btn-ghost btn-sm ${filter === k ? '!bg-liberthia-500/30 !text-white' : ''}`}
              onClick={() => setFilter(k as ChronicleKind)}
              style={{ borderColor: m.color + '60' }}
            >
              {m.emoji} {m.label} ({n})
            </button>
          )
        })}
      </div>

      {/* Timeline */}
      <div className="space-y-3">
        {filtered.length === 0 && (
          <div className="card text-center py-12">
            <div className="text-5xl mb-3 opacity-50">📜</div>
            <p className="text-liberthia-300/70">Nenhuma crônica registrada ainda. Comece a história.</p>
          </div>
        )}
        {filtered.map((e) => {
          const meta = KIND_META[e.kind]
          return (
            <div key={e.id} className="card-glow"
              style={{ borderColor: meta.color + '40' }}>
              <div className="flex items-start gap-3">
                <div className="text-4xl">{meta.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span className="badge text-[10px]" style={{ background: meta.color + '20', borderColor: meta.color + '50', color: meta.color }}>
                      {meta.label}
                    </span>
                    <span className="text-xs text-liberthia-300/70 italic">{e.era}</span>
                    <span className="text-[10px] text-liberthia-300/40 ml-auto">{new Date(e.realDate).toLocaleString()}</span>
                  </div>
                  <h3 className="font-bold text-lg mb-1">{renderMcText(e.title)}</h3>
                  <div className="text-sm text-liberthia-300/80 whitespace-pre-wrap break-words mb-3">
                    {renderMcText(e.body)}
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    <button className="btn-cyan btn-sm" onClick={() => announce(e)}>📢 Anunciar</button>
                    <button className="btn-ghost btn-sm" onClick={() => presentAsTitle(e)}>🎬 Apresentar</button>
                    <select className="input text-xs max-w-xs"
                      onChange={(ev) => { if (ev.target.value) { giveBook(e, ev.target.value); ev.target.value = '' } }}>
                      <option value="">📖 Dar livro a...</option>
                      {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
                    </select>
                    <button className="btn-ghost btn-sm" onClick={() => setEntries((cur) => cur.map((x) => x.id === e.id ? { ...x, pinned: !x.pinned } : x))} title="Fixar">
                      {e.pinned ? '📌' : '☆'}
                    </button>
                    <button className="btn-ghost btn-sm ml-auto" onClick={() => setEditing(e)}>✎</button>
                    <button className="btn-ghost btn-sm" onClick={() => setEntries((cur) => cur.filter((x) => x.id !== e.id))}>🗑</button>
                  </div>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {editing && <EntryEditor entry={editing} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function EntryEditor({ entry, onSave, onCancel }: {
  entry: Entry; onSave: (e: Entry) => void; onCancel: () => void
}) {
  const [e, setE] = useState<Entry>(entry)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(ev) => ev.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">📜 Crônica</h3>

        <label className="label block mb-1">Tipo</label>
        <div className="grid grid-cols-4 gap-1.5 mb-3">
          {Object.entries(KIND_META).map(([k, m]) => (
            <button key={k} type="button"
              className={`btn-ghost btn-sm ${e.kind === k ? '!bg-liberthia-500/30 !text-white' : ''}`}
              onClick={() => setE({ ...e, kind: k as ChronicleKind })}
              style={{ borderColor: m.color + '60' }}
            >
              {m.emoji} {m.label}
            </button>
          ))}
        </div>

        <label className="label block mb-1">Era / período</label>
        <input className="input mb-3" value={e.era} onChange={(ev) => setE({ ...e, era: ev.target.value })} placeholder="Era da Sombra, Ano 3, etc" />

        <label className="label block mb-1">Título</label>
        <MinecraftFormatter value={e.title} onChange={(v) => setE({ ...e, title: v })} rows={1} maxChars={80} showCounter={false} />

        <label className="label block mb-1 mt-3">Corpo / descrição</label>
        <MinecraftFormatter value={e.body} onChange={(v) => setE({ ...e, body: v })} rows={6} maxChars={1500} />

        <div className="flex gap-2 justify-end mt-6">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(e)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
