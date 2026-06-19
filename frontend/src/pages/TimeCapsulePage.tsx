import { useEffect, useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'

/**
 * Time Capsule — agenda entregas futuras pra um player.
 * Frontend monitora data atual; quando deliverAt <= now, dispara a entrega.
 *
 * Cada cápsula pode conter:
 *  - mensagem (chat broadcast pro player)
 *  - title cinematográfico
 *  - livro com texto
 *  - items (lista de give)
 *  - som
 */

/** Presets prontos — só precisa preencher playerUuid/Name + deliverAt. */
type CapsulePreset = Omit<Capsule, 'id' | 'playerUuid' | 'playerName' | 'deliverAt' | 'createdAt' | 'delivered'>
const CAPSULE_PRESETS: { label: string; daysLater: number; preset: CapsulePreset }[] = [
  {
    label: '🎂 Aniversário de 1 ano de servidor',
    daysLater: 365,
    preset: {
      title: '§6§l🎂 1 ANO de Liberthia',
      subtitle: '§eparabéns por chegar até aqui',
      message: '§6Faz exatamente §e1 ano§6 que você entrou no servidor. Olha quanto você cresceu...',
      sound: 'minecraft:entity.firework_rocket.launch',
      items: [
        { item: 'minecraft:cake', count: 1 },
        { item: 'minecraft:firework_rocket', count: 16 },
        { item: 'minecraft:netherite_ingot', count: 1 },
      ],
      bookTitle: 'Diário 1 Ano',
      bookAuthor: 'Você do Passado',
      bookPages: [
        'Olá, eu de 1 ano atrás.\n\nSe você ta lendo isso, significa que durou.\n\nLembra de tudo que você passou...',
        'Pra: §6você\nDe: §epassado\n\nNão desista. As coisas melhoram.',
      ],
    },
  },
  {
    label: '⏳ Lembrete em 7 dias',
    daysLater: 7,
    preset: {
      title: '§b§l⏳ Lembrete',
      subtitle: '§7algo importante',
      message: '§7Faz §b1 semana§7 que essa nota foi escrita. Lembra do que prometeu fazer?',
      sound: 'minecraft:block.bell.use',
    },
  },
  {
    label: '🌌 Profecia em 30 dias',
    daysLater: 30,
    preset: {
      title: '§5§l🌌 A profecia chegou',
      subtitle: '§do tempo se cumpriu',
      message: '§5§oAs estrelas previam isso há um mês...',
      sound: 'minecraft:entity.elder_guardian.curse',
      items: [
        { item: 'minecraft:ender_eye', count: 4 },
        { item: 'minecraft:experience_bottle', count: 16 },
      ],
      bookTitle: 'Profecia Cumprida',
      bookAuthor: 'O Oráculo',
      bookPages: [
        '§oNo trigésimo dia,\nquando a lua estiver cheia\ne a memória vacilar...',
        '§5tu receberás esta visão.',
      ],
    },
  },
  {
    label: '🎁 Presente de boas-vindas (1 dia)',
    daysLater: 1,
    preset: {
      title: '§a§l🎁 Bem-vindo de volta',
      subtitle: '§2sentimos sua falta',
      message: '§aFaz um dia que você não aparece. Aqui tem um presente.',
      sound: 'minecraft:entity.player.levelup',
      items: [
        { item: 'minecraft:diamond', count: 5 },
        { item: 'minecraft:bread', count: 16 },
        { item: 'minecraft:golden_apple', count: 3 },
      ],
    },
  },
  {
    label: '💀 Mensagem do além-túmulo (90 dias)',
    daysLater: 90,
    preset: {
      title: '§8§l💀 Uma carta esquecida',
      subtitle: '§7chega no momento certo',
      message: '§8§oUm envelope amarelado aparece do nada...',
      sound: 'minecraft:entity.allay.death',
      bookTitle: 'Carta Esquecida',
      bookAuthor: 'Anônimo',
      bookPages: [
        'Se você está lendo isto,\né porque sobreviveu.\n\nNão é todo dia que se chega aos 90 dias.',
        'Agora é hora de\ndecidir o que fazer\ncom o tempo que sobra.',
      ],
    },
  },
]

type Item = { item: string; count: number }
type Capsule = {
  id: string
  playerUuid: string
  playerName: string
  deliverAt: number      // timestamp ms
  message?: string
  title?: string
  subtitle?: string
  bookTitle?: string
  bookAuthor?: string
  bookPages?: string[]
  items?: Item[]
  sound?: string
  delivered?: boolean
  createdAt: number
}

export function TimeCapsulePage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [capsules, setCapsules] = useKvState<Capsule[]>('time_capsules', [])
  const [editing, setEditing] = useState<Capsule | null>(null)

  // Tick checker — a cada 10s verifica se alguma cápsula está vencida
  useEffect(() => {
    const t = setInterval(async () => {
      const now = Date.now()
      const pending = capsules.filter((c) => !c.delivered && c.deliverAt <= now)
      if (pending.length === 0) return
      const livePlayers = await api.players().catch(() => [] as any[])
      for (const c of pending) {
        const p = livePlayers.find((x: any) => x.uuid === c.playerUuid)
        if (!p) continue // player offline, tenta depois
        await deliver(c, p.name)
        setCapsules((cur) => cur.map((x) => x.id === c.id ? { ...x, delivered: true } : x))
      }
    }, 10000)
    return () => clearInterval(t)
  }, [capsules])

  async function deliver(c: Capsule, name: string) {
    try {
      // Som introduz a cápsula
      await api.sound(c.playerUuid, 'minecraft:block.amethyst_block.chime', 1, 0.5)
      await new Promise((r) => setTimeout(r, 500))
      await api.title(c.playerUuid, '§e§l⏳ Cápsula do Tempo', '§7algo chegou pra você...', 15, 80, 20)
      await new Promise((r) => setTimeout(r, 3500))

      if (c.title) {
        await api.title(c.playerUuid, c.title, c.subtitle ?? '', 10, 80, 20)
      }
      if (c.message) {
        await api.command(`tellraw ${name} ${JSON.stringify({ text: '§7§o[Cápsula] §r' + c.message })}`, 'capsule')
      }
      if (c.sound) {
        await api.sound(c.playerUuid, c.sound)
      }
      if (c.bookPages && c.bookPages.length > 0) {
        const escapedPages = c.bookPages.map((p) => JSON.stringify(JSON.stringify({ text: p }))).join(',')
        await api.command(`give ${name} written_book{title:"${(c.bookTitle ?? 'Cápsula').replace(/"/g, '\\"')}",author:"${(c.bookAuthor ?? 'Você do passado').replace(/"/g, '\\"')}",pages:[${escapedPages}]} 1`, 'capsule')
      }
      if (c.items) {
        for (const it of c.items) {
          await api.give(c.playerUuid, it.item, it.count)
        }
      }
      toast.ok(`📦 Cápsula entregue: ${name}`)
    } catch (e: any) { toast.err(`Falha cápsula: ${e.message}`) }
  }

  function newCapsule() {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    setEditing({
      id: `cap_${Date.now().toString(36)}`,
      playerUuid: players[0]?.uuid ?? '',
      playerName: players[0]?.name ?? '',
      deliverAt: tomorrow.getTime(),
      message: '§7Não esqueça do que prometi.',
      bookTitle: '§5Para o futuro',
      bookAuthor: 'Você (antes)',
      bookPages: ['§7Quando você ler isto...\n§lvai lembrar de tudo.'],
      createdAt: Date.now(),
    })
  }

  function commit(c: Capsule) {
    const p = players.find((x) => x.uuid === c.playerUuid)
    if (p) c.playerName = p.name
    setCapsules((cur) => {
      const i = cur.findIndex((x) => x.id === c.id)
      if (i >= 0) { const n = [...cur]; n[i] = c; return n }
      return [...cur, c]
    })
    setEditing(null)
  }

  async function deliverNow(c: Capsule) {
    if (!c.playerName) return
    await deliver(c, c.playerName)
    setCapsules((cur) => cur.map((x) => x.id === c.id ? { ...x, delivered: true } : x))
  }

  const pending = capsules.filter((c) => !c.delivered).sort((a, b) => a.deliverAt - b.deliverAt)
  const delivered = capsules.filter((c) => c.delivered).sort((a, b) => b.deliverAt - a.deliverAt)

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⏳ Cápsulas do Tempo</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Agenda entregas futuras pra players: títulos, livros, items, sons. Frontend monitora e entrega quando vence.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <select className="input text-xs" disabled={players.length === 0}
            onChange={(e) => {
              const idx = Number(e.target.value)
              if (Number.isNaN(idx) || idx < 0) return
              const tpl = CAPSULE_PRESETS[idx]
              const p = players[0]
              const capsule: Capsule = {
                ...tpl.preset,
                id: 'preset-' + Date.now(),
                playerUuid: p.uuid,
                playerName: p.name,
                deliverAt: Date.now() + tpl.daysLater * 24 * 60 * 60 * 1000,
                createdAt: Date.now(),
              }
              setCapsules((cur) => [...cur, capsule])
              toast.ok(`📥 "${tpl.label}" agendada pra ${p.name} em ${tpl.daysLater}d`)
              e.target.value = '-1'
            }}>
            <option value="-1">📥 Importar preset…</option>
            {CAPSULE_PRESETS.map((t, i) => (
              <option key={i} value={i}>{t.label} (+{t.daysLater}d)</option>
            ))}
          </select>
          <button className="btn" onClick={newCapsule} disabled={players.length === 0}>+ Nova Cápsula</button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div>
          <h2 className="text-lg font-bold gradient-text mb-3">📦 Pendentes ({pending.length})</h2>
          <div className="space-y-2">
            {pending.length === 0 && (
              <div className="card text-center py-8 text-liberthia-300/50">— nada agendado —</div>
            )}
            {pending.map((c) => {
              const dt = new Date(c.deliverAt)
              const diff = c.deliverAt - Date.now()
              const days = Math.floor(diff / 86400000)
              const hours = Math.floor((diff % 86400000) / 3600000)
              const mins = Math.floor((diff % 3600000) / 60000)
              return (
                <div key={c.id} className="card-glow">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-2xl">⏳</span>
                    <div className="flex-1 min-w-0">
                      <div className="font-bold">{c.playerName}</div>
                      <div className="text-xs text-liberthia-300/60">{dt.toLocaleString()}</div>
                    </div>
                    <span className={`badge ${diff < 0 ? 'badge-red' : diff < 3600000 ? 'badge-yellow' : 'badge-purple'}`}>
                      {diff < 0 ? 'atrasada' : days > 0 ? `${days}d ${hours}h` : hours > 0 ? `${hours}h ${mins}m` : `${mins}m`}
                    </span>
                    <button className="btn-ghost btn-sm" onClick={() => setEditing(c)}>✎</button>
                  </div>
                  {c.title && <div className="text-sm">📺 {renderMcText(c.title)}</div>}
                  {c.message && <div className="text-xs text-liberthia-300/70 truncate">💬 {renderMcText(c.message)}</div>}
                  {(c.bookPages?.length ?? 0) > 0 && <span className="chip">📖 livro</span>}
                  {(c.items?.length ?? 0) > 0 && <span className="chip">📦 {c.items!.length} items</span>}
                  <div className="flex gap-1 mt-2">
                    <button className="btn-amber btn-sm flex-1" onClick={() => deliverNow(c)}>⚡ Entregar agora</button>
                    <button className="btn-ghost btn-sm" onClick={() => setCapsules((cur) => cur.filter((x) => x.id !== c.id))}>🗑</button>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div>
          <h2 className="text-lg font-bold gradient-text mb-3">✓ Entregues ({delivered.length})</h2>
          <div className="space-y-1 max-h-[80vh] overflow-y-auto">
            {delivered.map((c) => (
              <div key={c.id} className="p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20 text-xs">
                <div className="flex items-center gap-2">
                  <span className="badge badge-green">✓</span>
                  <span>{c.playerName}</span>
                  <span className="text-liberthia-300/40 ml-auto">{new Date(c.deliverAt).toLocaleString()}</span>
                  <button className="btn-ghost btn-sm" onClick={() => setCapsules((cur) => cur.filter((x) => x.id !== c.id))}>🗑</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {editing && <CapsuleEditor capsule={editing} players={players} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function CapsuleEditor({ capsule, players, onSave, onCancel }: {
  capsule: Capsule; players: any[]; onSave: (c: Capsule) => void; onCancel: () => void
}) {
  const [c, setC] = useState<Capsule>(capsule)
  const dtStr = new Date(c.deliverAt).toISOString().slice(0, 16) // YYYY-MM-DDTHH:MM

  function updateItem(i: number, p: Partial<Item>) {
    setC({ ...c, items: (c.items ?? []).map((x, j) => j === i ? { ...x, ...p } : x) })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⏳ Cápsula do Tempo</h3>

        <div className="grid grid-cols-2 gap-2 mb-3">
          <div>
            <label className="label block mb-1">Player destinatário</label>
            <select className="input" value={c.playerUuid} onChange={(e) => setC({ ...c, playerUuid: e.target.value })}>
              <option value="">— escolher —</option>
              {players.map((p: any) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
          </div>
          <div>
            <label className="label block mb-1">Entregar em</label>
            <input type="datetime-local" className="input" value={dtStr}
              onChange={(e) => setC({ ...c, deliverAt: new Date(e.target.value).getTime() })} />
          </div>
        </div>

        <label className="label block mb-1 mt-2">Title (opcional)</label>
        <MinecraftFormatter value={c.title ?? ''} onChange={(v) => setC({ ...c, title: v })} rows={1} maxChars={64} showCounter={false} />
        <label className="label block mb-1 mt-2">Subtitle</label>
        <MinecraftFormatter value={c.subtitle ?? ''} onChange={(v) => setC({ ...c, subtitle: v })} rows={1} maxChars={64} showCounter={false} />

        <label className="label block mb-1 mt-3">Mensagem no chat (opcional)</label>
        <MinecraftFormatter value={c.message ?? ''} onChange={(v) => setC({ ...c, message: v })} rows={2} maxChars={200} showCounter={false} />

        <label className="label block mb-1 mt-3">Som (opcional)</label>
        <input className="input font-mono text-xs" value={c.sound ?? ''} placeholder="minecraft:block.bell.use ou liberthia:meu_som"
          onChange={(e) => setC({ ...c, sound: e.target.value || undefined })} />

        <details className="mt-4">
          <summary className="cursor-pointer text-sm font-bold">📖 Livro (opcional)</summary>
          <div className="mt-2 space-y-2">
            <input className="input text-xs" placeholder="Título do livro" value={c.bookTitle ?? ''}
              onChange={(e) => setC({ ...c, bookTitle: e.target.value })} />
            <input className="input text-xs" placeholder="Autor" value={c.bookAuthor ?? ''}
              onChange={(e) => setC({ ...c, bookAuthor: e.target.value })} />
            {(c.bookPages ?? []).map((p, i) => (
              <div key={i} className="flex gap-2 items-start">
                <span className="text-xs text-liberthia-300/50 mt-2 shrink-0">p{i + 1}</span>
                <MinecraftFormatter value={p} onChange={(v) => setC({ ...c, bookPages: (c.bookPages ?? []).map((x, j) => j === i ? v : x) })} rows={2} maxChars={285} showCounter={false} />
                <button className="btn-ghost btn-sm" onClick={() => setC({ ...c, bookPages: (c.bookPages ?? []).filter((_, j) => j !== i) })}>🗑</button>
              </div>
            ))}
            <button className="btn-ghost btn-sm" onClick={() => setC({ ...c, bookPages: [...(c.bookPages ?? []), ''] })}>+ Página</button>
          </div>
        </details>

        <details className="mt-3">
          <summary className="cursor-pointer text-sm font-bold">📦 Items (opcional)</summary>
          <div className="mt-2 space-y-1">
            {(c.items ?? []).map((it, i) => (
              <div key={i} className="flex gap-2">
                <input className="input flex-1 font-mono text-xs" value={it.item} onChange={(e) => updateItem(i, { item: e.target.value })} placeholder="minecraft:diamond" />
                <input type="number" className="input w-20 text-xs" value={it.count} min={1} max={64}
                  onChange={(e) => updateItem(i, { count: Math.max(1, Math.min(64, Number(e.target.value))) })} />
                <button className="btn-ghost btn-sm" onClick={() => setC({ ...c, items: (c.items ?? []).filter((_, j) => j !== i) })}>🗑</button>
              </div>
            ))}
            <button className="btn-ghost btn-sm" onClick={() => setC({ ...c, items: [...(c.items ?? []), { item: 'minecraft:diamond', count: 1 }] })}>+ Item</button>
          </div>
        </details>

        <div className="flex gap-2 justify-end mt-4">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(c)} disabled={!c.playerUuid}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
