import { useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'

/**
 * Editor de written_book. Limites vanilla aplicados:
 *  - Título: 16 chars
 *  - Página: 256 chars (incluindo §-codes)
 *  - Livro: 100 páginas max
 *  - Autor: 16 chars
 */

// Limites vanilla (Minecraft Java 1.20.1):
//   - Título do livro: 16 chars
//   - Autor: 16 chars
//   - Por página: 239 chars (255 vanilla - 16 reservados pra overhead NBT) — hard cap
//   - Livro: 100 páginas
//
// IMPORTANTE: cada página tem seu PRÓPRIO contador — não soma do livro inteiro.
const MAX_TITLE = 16
const MAX_AUTHOR = 16
const MAX_PAGE_CHARS = 239
const MAX_PAGES = 100

type Book = { name: string; title: string; author: string; pages: string[]; updated: number }

export function LoreBookPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [name, setName] = useState('lore-book-1')
  const [title, setTitle] = useState('§5Despertar')
  const [author, setAuthor] = useState('Mira')
  const [pages, setPages] = useState<string[]>([
    '§5§lO Despertar§r\n\nAs sombras voltaram a sussurrar.\n\nVi os olhos de §cdragões§r dormindo.',
    '\n§7Capítulo I§r\n\nFoi numa noite de tempestade que o pó da escuridão começou a cair.',
    '\n§eFinal§r\n\nLuz e treva §lcaminham juntas§r.',
  ])
  const [page, setPage] = useState(0)
  const [saved, setSaved] = useKvState<Book[]>('lore_books', [])

  function save() {
    const next = [{ name, title, author, pages, updated: Date.now() }, ...saved.filter((b) => b.name !== name)]
    setSaved(next)
    toast.ok('Livro salvo')
  }

  function load(b: Book) {
    setName(b.name); setTitle(b.title); setAuthor(b.author); setPages(b.pages); setPage(0)
  }

  function addPage() {
    if (pages.length >= MAX_PAGES) { toast.err(`Máximo ${MAX_PAGES} páginas`); return }
    setPages([...pages, '']); setPage(pages.length)
  }

  function deletePage() {
    if (pages.length <= 1) return
    setPages(pages.filter((_, i) => i !== page))
    setPage(Math.max(0, page - 1))
  }

  function updatePage(text: string) {
    setPages(pages.map((p, i) => i === page ? text : p))
  }

  async function give() {
    if (!target) { toast.err('Selecione um player'); return }
    // Validação de limites
    const overflows = pages.filter((p) => p.length > MAX_PAGE_CHARS)
    if (overflows.length) { toast.err(`${overflows.length} página(s) acima de ${MAX_PAGE_CHARS} chars`); return }
    if (title.length > MAX_TITLE) { toast.err(`Título acima de ${MAX_TITLE} chars`); return }
    if (author.length > MAX_AUTHOR) { toast.err(`Autor acima de ${MAX_AUTHOR} chars`); return }

    const escapedPages = pages.map((p) => {
      const json = JSON.stringify({ text: p })
      return JSON.stringify(json)
    }).join(',')
    const cmd = `give ${nameByUuid(target)} written_book{title:"${title.replace(/"/g, '\\"')}",author:"${author.replace(/"/g, '\\"')}",pages:[${escapedPages}]} 1`
    try {
      await api.command(cmd, 'books')
      toast.ok('📖 Livro entregue')
    } catch (e: any) { toast.err(e.message) }
  }

  function nameByUuid(uuid: string) { return players.find((p) => p.uuid === uuid)?.name ?? '@p' }

  const currentPageChars = (pages[page] ?? '').length

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📖 Lore Books</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Livros vanilla com cores e formatação. <b>Cada página tem seu próprio limite</b> —
            não é total acumulado.
          </p>
          <div className="flex flex-wrap gap-2 mt-2">
            <span className="chip">Título: {MAX_TITLE} chars</span>
            <span className="chip">Máx {MAX_PAGE_CHARS} chars por página (inclui espaços e §-codes)</span>
            <span className="chip">Até {MAX_PAGES} páginas</span>
          </div>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-5">
        <div className="space-y-4">
          {/* Metadata */}
          <div className="card-glow">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2 mb-3">
              <div>
                <label className="label block mb-1">Nome interno</label>
                <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
              </div>
              <div>
                <label className="label block mb-1">
                  Título <span className="text-liberthia-300/40">({title.length}/{MAX_TITLE})</span>
                </label>
                <input
                  className={`input ${title.length > MAX_TITLE ? 'border-red-400/60' : ''}`}
                  value={title} maxLength={MAX_TITLE}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>
              <div className="md:col-span-2">
                <label className="label block mb-1">
                  Autor <span className="text-liberthia-300/40">({author.length}/{MAX_AUTHOR})</span>
                </label>
                <input
                  className={`input ${author.length > MAX_AUTHOR ? 'border-red-400/60' : ''}`}
                  value={author} maxLength={MAX_AUTHOR}
                  onChange={(e) => setAuthor(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Page navigator */}
          <div className="card-glow">
            <div className="flex items-center gap-2 mb-3 flex-wrap">
              <span className="label">Páginas:</span>
              <div className="flex gap-1 flex-wrap">
                {pages.map((p, i) => {
                  const isOver = p.length > MAX_PAGE_CHARS
                  return (
                    <button key={i}
                      className={`btn-ghost btn-sm ${page === i ? '!bg-liberthia-500/40 !text-white' : ''} ${isOver ? '!border-red-400/60' : ''}`}
                      onClick={() => setPage(i)}
                    >
                      {i + 1}
                      {isOver && <span className="ml-1 text-red-300">!</span>}
                    </button>
                  )
                })}
                <button className="btn-ghost btn-sm" onClick={addPage} disabled={pages.length >= MAX_PAGES}>+ pág</button>
                {pages.length > 1 && (
                  <button className="btn-ghost btn-sm" onClick={deletePage}>🗑 atual</button>
                )}
              </div>
              <div className="ml-auto text-xs text-liberthia-300/60 font-mono">
                {pages.length}/{MAX_PAGES} págs
              </div>
            </div>

            <MinecraftFormatter
              value={pages[page] ?? ''}
              onChange={updatePage}
              rows={10}
              maxChars={MAX_PAGE_CHARS}
              placeholder="§fDigite a página... use os botões pra cor e formatação"
            />
            <div className="text-[10px] text-liberthia-300/50 mt-2 text-center">
              Página {page + 1} de {pages.length} · {currentPageChars}/{MAX_PAGE_CHARS} chars
            </div>
          </div>

          {/* Preview */}
          <div className="card-glow">
            <h3 className="font-bold mb-2 flex items-center gap-2">📄 Preview no livro</h3>
            <div className="bg-amber-50 text-black rounded-lg p-6 font-serif min-h-[280px] shadow-inner whitespace-pre-wrap break-words leading-relaxed text-base overflow-hidden">
              {renderMcText(pages[page] ?? '')}
            </div>
            <div className="text-xs text-liberthia-300/50 text-center mt-2">
              Página {page + 1} / {pages.length}
            </div>
          </div>

          {/* Actions */}
          <div className="card flex items-center gap-2 flex-wrap">
            <select className="input flex-1 min-w-[200px]" value={target} onChange={(e) => setTarget(e.target.value)}>
              <option value="">— player que vai receber —</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
            <button className="btn-cyan" onClick={save}>💾 Save</button>
            <button className="btn" onClick={give} disabled={!target}>📖 Entregar</button>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">📚 Biblioteca</h3>
            {saved.length === 0 && <p className="text-xs text-liberthia-300/50 italic">Nenhum livro salvo ainda.</p>}
            <div className="space-y-1">
              {saved.map((b) => (
                <div key={b.name} className="flex items-center gap-1">
                  <button className="btn-ghost flex-1 text-left text-xs" onClick={() => load(b)}>
                    <div className="font-bold">{renderMcText(b.title)}</div>
                    <div className="text-[10px] opacity-60">{b.author} · {b.pages.length} pgs</div>
                  </button>
                  <button className="btn-ghost btn-sm" onClick={() => {
                    const next = saved.filter((x) => x.name !== b.name)
                    setSaved(next)
                  }}>🗑</button>
                </div>
              ))}
            </div>
          </div>

          <div className="card text-xs space-y-1.5 text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Dicas</div>
            <p>• Selecione texto e clique <span className="chip">B</span> / <span className="chip">I</span> pra envolver com formato</p>
            <p>• Click numa cor → injeta no cursor</p>
            <p>• <span className="chip">§r</span> reseta tudo (cor + bold + italic)</p>
            <p>• Contador fica <span className="text-amber-300">amarelo</span> em 85% e <span className="text-red-300">vermelho</span> ao exceder</p>
            <p>• Páginas acima do limite mostram <span className="text-red-300">!</span> no tab</p>
          </div>
        </div>
      </div>
    </div>
  )
}
