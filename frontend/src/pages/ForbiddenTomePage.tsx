import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { renderMcText } from '../components/MinecraftFormatter'

/**
 * Forbidden Tome — pega histórico de chat do player + garble crescente,
 * pacote como written_book e entrega. O livro contém as próprias palavras
 * do player, distorcidas, encadernadas como uma profecia eldritch.
 */

const GLITCH = ['#', '@', '?', '!', '$', '%', '*', '~', '`', '<', '>']

function garble(s: string, intensity: number): string {
  return s.split('').map((c) => {
    if (c === ' ' || c === '\n') return c
    return Math.random() < intensity ? GLITCH[Math.floor(Math.random() * GLITCH.length)] : c
  }).join('')
}

export function ForbiddenTomePage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [recipient, setRecipient] = useState('')
  const [pages, setPages] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState(0)
  const [title, setTitle] = useState('§5O Livro Vedado')
  const [author, setAuthor] = useState('§8Desconhecido')

  async function generate() {
    if (!target) { toast.err('Selecione o player-fonte'); return }
    setBusy(true); setProgress(0); setPages([])
    try {
      const entries = await api.chatHistory(target, 0, 50)
      const msgs = entries.map((e) => e.message).filter((m) => m && !m.startsWith('[LIB:'))
      if (msgs.length === 0) { toast.err('Player sem chat history'); setBusy(false); return }
      const player = players.find((p) => p.uuid === target)
      const playerName = player?.name ?? 'Desconhecido'

      // Compõe páginas — agrupa mensagens em chunks que cabem em 255 chars
      const pgs: string[] = []
      // Primeira página: introdução
      pgs.push(
        `§5§lO Livro Vedado§r\n\n` +
        `§7§oRecolhi as palavras\nde §f${playerName}§7§o.\n\n` +
        `§8Ouça-as de novo.§r\n\n` +
        `§7§o— anônimo —`
      )

      let acc = ''
      let pageIdx = 1
      for (const m of msgs) {
        // Intensity cresce com cada página
        const intensity = Math.min(0.45, 0.05 + pageIdx * 0.03)
        const garbled = garble(m, intensity)
        const entry = `§7§o— "${garbled}"§r\n\n`
        if (acc.length + entry.length > 240) {
          pgs.push(acc.trim())
          acc = entry
          pageIdx++
        } else {
          acc += entry
        }
      }
      if (acc) pgs.push(acc.trim())

      // Última página
      pgs.push(`§4§lFIM§r\n\n§8§oEles ouviram tudo.\nTudo.\nTudo.\n\n§7${msgs.length} fragmentos\nrecolhidos.`)

      setPages(pgs)
      toast.ok(`📖 ${pgs.length} páginas geradas`)
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }

  async function deliver() {
    if (!recipient) { toast.err('Selecione destinatário'); return }
    if (pages.length === 0) { toast.err('Gere o livro primeiro'); return }
    const player = players.find((p) => p.uuid === recipient); if (!player) return
    setBusy(true)
    try {
      const escapedPages = pages.map((p) => JSON.stringify(JSON.stringify({ text: p }))).join(',')
      const cmd = `give ${player.name} written_book{title:"${title.replace(/"/g, '\\"').slice(0, 32)}",author:"${author.replace(/"/g, '\\"').slice(0, 32)}",pages:[${escapedPages}]} 1`
      await api.command(cmd, 'forbidden-tome')
      // Cinema
      await api.sound(recipient, 'minecraft:item.book.page_turn', 1, 0.5)
      await api.title(recipient, '§5§l📖 §dUm livro chegou', '§7§ovocê não pediu por isso', 15, 100, 30)
      toast.ok(`📖 Tome entregue a ${player.name}`)
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">📖 Forbidden Tome</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Coleta o chat history do player, distorce progressivamente, encaderna como written_book.
          O livro contém as próprias palavras dele — corrompidas, repetidas, em ordem alheia.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-5">
        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3">🎯 Setup</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-3">
              <div>
                <label className="label block mb-1">Player-fonte (chat history)</label>
                <select className="input" value={target} onChange={(e) => setTarget(e.target.value)}>
                  <option value="">— quem vai ser corrompido —</option>
                  {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
                </select>
              </div>
              <div>
                <label className="label block mb-1">Destinatário (vai receber o livro)</label>
                <select className="input" value={recipient} onChange={(e) => setRecipient(e.target.value)}>
                  <option value="">— quem recebe —</option>
                  {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
                </select>
              </div>
              <div>
                <label className="label block mb-1">Título</label>
                <input className="input text-sm font-mono" value={title} onChange={(e) => setTitle(e.target.value.slice(0, 32))} maxLength={32} />
              </div>
              <div>
                <label className="label block mb-1">Autor</label>
                <input className="input text-sm font-mono" value={author} onChange={(e) => setAuthor(e.target.value.slice(0, 32))} maxLength={32} />
              </div>
            </div>
            <div className="flex gap-2">
              <button className="btn-cyan flex-1" onClick={generate} disabled={!target || busy}>
                {busy ? '⏳ Gerando...' : '✨ Gerar livro do chat history'}
              </button>
              <button className="btn flex-1" onClick={deliver} disabled={!recipient || pages.length === 0 || busy}>
                📖 Entregar
              </button>
            </div>
          </div>

          {pages.length > 0 && (
            <div className="card-glow">
              <h3 className="font-bold mb-3">📄 Preview ({pages.length} páginas)</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-[60vh] overflow-y-auto">
                {pages.map((p, i) => (
                  <div key={i} className="bg-amber-50 text-black rounded-lg p-3 font-serif text-xs whitespace-pre-wrap break-words border border-amber-300/30 min-h-[120px]">
                    <div className="text-amber-700 text-[10px] font-bold mb-1">— pg {i + 1} —</div>
                    {renderMcText(p)}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="card text-xs text-liberthia-300/70">
          <div className="font-bold text-liberthia-200 mb-2">💡 Como funciona</div>
          <p>• Busca <span className="chip">50 últimas mensagens</span> via /api/history/chat</p>
          <p>• Filtra markers internos do painel ([LIB:..])</p>
          <p>• Aplica garble crescente: <span className="chip">5%</span> na pg1 → até <span className="chip">45%</span></p>
          <p>• Cada página tem ~240 chars (cabe no limite 255 vanilla)</p>
          <p>• Página final dramática com contador de fragmentos</p>
          <p>• Quando entrega: book.page_turn sound + title cinematográfico</p>
          <p className="mt-2 italic">Funciona melhor com 20+ mensagens de chat acumuladas.</p>
        </div>
      </div>
    </div>
  )
}
