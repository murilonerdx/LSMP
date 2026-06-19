import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { publicApi, RoadmapItemDto } from '../../lib/api'

/**
 * Roadmap público — qualquer um vê e vota.
 *
 * Voto é anônimo, persistido só no localStorage do cliente (chave
 * "liberthia.roadmap.voted"). Cada item recebe +1 quando clicado;
 * cliente bloqueia o botão se já votou nele.
 *
 * Categorias visuais:
 *   IN_DEV    🔧 em desenvolvimento agora
 *   NEXT      ⏭ próxima release
 *   PLANNED   📅 planejado
 *   IDEA      💡 ideia (pendente de aprovação)
 *   DONE      ✅ lançado
 *   CANCELLED ❌ cancelado
 */

const CATEGORY_META: Record<string, { label: string; emoji: string; color: string; description: string }> = {
  IN_DEV:    { label: 'Em Desenvolvimento', emoji: '🔧', color: 'amber',    description: 'Trabalhando nisso agora' },
  NEXT:      { label: 'Próxima Release',    emoji: '⏭', color: 'purple',   description: 'Vai sair na próxima versão' },
  PLANNED:   { label: 'Planejado',          emoji: '📅', color: 'liberthia', description: 'Confirmado pra entrar em alguma release' },
  IDEA:      { label: 'Ideias',             emoji: '💡', color: 'emerald',  description: 'Sugestões da comunidade — vote no que quer ver' },
  DONE:      { label: 'Lançado',            emoji: '✅', color: 'green',    description: 'Já tá no jogo' },
  CANCELLED: { label: 'Cancelado',          emoji: '❌', color: 'red',      description: 'Não vai ser feito' },
}

const ORDERED_CATEGORIES = ['IN_DEV', 'NEXT', 'PLANNED', 'IDEA', 'DONE', 'CANCELLED']

const VOTED_KEY = 'liberthia.roadmap.voted'

function loadVoted(): Set<number> {
  try {
    const raw = localStorage.getItem(VOTED_KEY)
    if (!raw) return new Set()
    return new Set(JSON.parse(raw))
  } catch { return new Set() }
}

function saveVoted(s: Set<number>) {
  localStorage.setItem(VOTED_KEY, JSON.stringify([...s]))
}

export function RoadmapPublicPage() {
  const qc = useQueryClient()
  const [voted, setVoted] = useState<Set<number>>(() => loadVoted())
  const [filter, setFilter] = useState<string>('all')

  const q = useQuery({
    queryKey: ['public-roadmap'],
    queryFn: publicApi.roadmap,
    refetchInterval: 15000,
  })

  const voteMut = useMutation({
    mutationFn: ({ id, undo }: { id: number; undo: boolean }) =>
      undo ? publicApi.roadmapUnvote(id) : publicApi.roadmapVote(id),
    onSuccess: (_, vars) => {
      const next = new Set(voted)
      if (vars.undo) next.delete(vars.id)
      else next.add(vars.id)
      setVoted(next); saveVoted(next)
      qc.invalidateQueries({ queryKey: ['public-roadmap'] })
    },
  })

  // Refresh local de "voted" caso outra aba tenha votado também
  useEffect(() => {
    const handler = () => setVoted(loadVoted())
    window.addEventListener('storage', handler)
    return () => window.removeEventListener('storage', handler)
  }, [])

  const grouped = q.data?.grouped ?? {}
  const totalItems = Object.values(grouped).reduce((acc, arr) => acc + arr.length, 0)

  const visibleCategories = filter === 'all' ? ORDERED_CATEGORIES : [filter]

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 py-8 px-4">
      <div className="w-full max-w-5xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="text-6xl mb-2">🗺</div>
          <h1 className="text-4xl font-bold gradient-text">Roadmap</h1>
          <p className="text-sm text-liberthia-300/70 mt-2">
            O que tá vindo no Liberthia · vota no que você quer ver primeiro
          </p>
        </div>

        {/* Navegação pública */}
        <div className="flex justify-center gap-2 mb-6 text-xs flex-wrap">
          <Link to="/changelog" className="btn-ghost btn-sm">📋 Changelog</Link>
          <Link to="/roadmap" className="btn btn-sm">🗺 Roadmap</Link>
          <Link to="/leaderboard" className="btn-ghost btn-sm">🏆 Caçadores de Bugs</Link>
          <Link to="/tester/apply" className="btn-ghost btn-sm">📝 Virar Tester</Link>
        </div>

        {/* Filtro por categoria */}
        <div className="card !p-2 flex flex-wrap gap-1 mb-6">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1 text-xs rounded ${filter === 'all' ? 'bg-purple-500/30 text-white' : 'text-liberthia-300/60 hover:bg-liberthia-700/30'}`}>
            🌐 Todos ({totalItems})
          </button>
          {ORDERED_CATEGORIES.map(cat => {
            const items = grouped[cat] ?? []
            if (items.length === 0) return null
            const m = CATEGORY_META[cat]
            return (
              <button key={cat}
                onClick={() => setFilter(cat)}
                className={`px-3 py-1 text-xs rounded ${filter === cat ? 'bg-purple-500/30 text-white' : 'text-liberthia-300/60 hover:bg-liberthia-700/30'}`}>
                {m.emoji} {m.label} ({items.length})
              </button>
            )
          })}
        </div>

        {q.isLoading && <div className="text-center text-liberthia-300/60">Carregando...</div>}
        {q.isError && (
          <div className="card-glow text-center py-10 text-red-400">
            Erro ao carregar roadmap.
          </div>
        )}

        {/* Categorias */}
        {visibleCategories.map(cat => {
          const items = grouped[cat] ?? []
          if (items.length === 0) return null
          const m = CATEGORY_META[cat]
          return (
            <section key={cat} className="mb-8">
              <header className="mb-3 px-1">
                <h2 className="text-lg font-bold flex items-center gap-2">
                  <span className="text-2xl">{m.emoji}</span>
                  <span className="gradient-text">{m.label}</span>
                  <span className="text-xs text-liberthia-300/50 font-normal">({items.length})</span>
                </h2>
                <p className="text-[11px] text-liberthia-300/50 ml-9">{m.description}</p>
              </header>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {items.map(it => (
                  <RoadmapCard key={it.id} item={it}
                    voted={voted.has(it.id)}
                    onVote={(undo) => voteMut.mutate({ id: it.id, undo })}
                    busy={voteMut.isPending} />
                ))}
              </div>
            </section>
          )
        })}

        {totalItems === 0 && !q.isLoading && (
          <div className="card-glow text-center py-12">
            <div className="text-4xl mb-2 opacity-50">📋</div>
            <p className="text-liberthia-300/60">Sem items no roadmap ainda.</p>
          </div>
        )}

        {/* Info / Footer */}
        <div className="card !bg-purple-500/5 !border-purple-400/20 mt-8 text-xs text-liberthia-300/70">
          <h3 className="font-bold mb-2">💡 Como funciona?</h3>
          <ul className="space-y-1 list-disc list-inside">
            <li>Qualquer um pode votar — não precisa de conta.</li>
            <li>Seu voto fica no seu navegador (1 voto por item por dispositivo).</li>
            <li>Items mais votados sobem na lista de prioridade.</li>
            <li>Quer sugerir algo novo? <Link to="/tester/apply" className="text-purple-300 underline">vira tester</Link> pra propor items / mecânicas.</li>
          </ul>
        </div>

        <div className="text-center mt-6 text-[10px] text-liberthia-300/40">
          Liberthia mod · {new Date().getFullYear()}
        </div>
      </div>
    </div>
  )
}

function RoadmapCard({ item, voted, onVote, busy }: {
  item: RoadmapItemDto
  voted: boolean
  onVote: (undo: boolean) => void
  busy: boolean
}) {
  const m = CATEGORY_META[item.category]
  const canVote = item.category !== 'CANCELLED' && item.category !== 'DONE'

  return (
    <div className="card-glow flex gap-3">
      {/* Voto */}
      <div className="flex flex-col items-center justify-start gap-1 min-w-[44px]">
        {canVote ? (
          <button
            onClick={() => onVote(voted)}
            disabled={busy}
            title={voted ? 'Remover voto' : 'Votar'}
            className={`w-10 h-10 rounded-lg flex items-center justify-center text-lg transition-all ${
              voted
                ? 'bg-purple-500/40 text-purple-100 ring-2 ring-purple-400'
                : 'bg-liberthia-900/40 text-liberthia-300/70 hover:bg-liberthia-700/40 hover:text-white'
            }`}>
            ▲
          </button>
        ) : (
          <div className="w-10 h-10 rounded-lg flex items-center justify-center text-lg bg-liberthia-900/20 text-liberthia-300/30">
            ▲
          </div>
        )}
        <div className="text-xs font-mono font-bold">{item.votes}</div>
      </div>

      {/* Conteúdo */}
      <div className="flex-1 min-w-0">
        <div className="flex items-start gap-2 mb-1 flex-wrap">
          {item.emoji && <span className="text-2xl shrink-0">{item.emoji}</span>}
          <div className="flex-1 min-w-0">
            <h3 className="font-bold text-sm text-liberthia-100 truncate">{item.title}</h3>
            <div className="flex flex-wrap items-center gap-1 mt-0.5">
              <span className="text-[10px] badge">{m.emoji} {m.label}</span>
              {item.tag && <span className="text-[10px] badge badge-purple">#{item.tag}</span>}
              {item.targetVersion && (
                <span className="text-[10px] badge badge-green">→ {item.targetVersion}</span>
              )}
            </div>
          </div>
        </div>
        {item.description && (
          <p className="text-xs text-liberthia-300/80 mt-1 whitespace-pre-wrap">{item.description}</p>
        )}
      </div>
    </div>
  )
}
