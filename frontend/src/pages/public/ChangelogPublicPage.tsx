import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { publicApi, ChangelogEntryDto } from '../../lib/api'
import { ChangelogDetailModal } from '../../components/ChangelogDetailModal'

/**
 * Changelog público — qualquer um vê (sem login).
 * Mostra cards COMPACTOS em ordem cronológica reversa. Click → modal de
 * detalhe com tudo (items, bugs, buffs, debuffs, integrações, créditos, notas).
 *
 * Antes: cards renderizavam TUDO inline, página virava uma parede de texto
 * com 800+ linhas — usuário tinha que rolar muito pra achar uma release
 * específica. Agora cada card mostra só preview (título, versão, data,
 * summary, contadores) e o detalhe abre num overlay.
 */
export function ChangelogPublicPage() {
  const q = useQuery({
    queryKey: ['public-changelog'],
    queryFn: publicApi.changelog,
    refetchInterval: 30000,
  })
  // Entry aberta no modal de detalhe (null = nenhuma)
  const [selected, setSelected] = useState<ChangelogEntryDto | null>(null)

  const entries = q.data?.entries ?? []
  const highlighted = entries.filter(e => e.highlighted)
  const rest = entries.filter(e => !e.highlighted)

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 py-8 px-4">
      <div className="w-full max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="text-6xl mb-2">📋</div>
          <h1 className="text-4xl font-bold gradient-text">Changelog</h1>
          <p className="text-sm text-liberthia-300/70 mt-2">
            Tudo que mudou no mod Liberthia ao longo das versões
          </p>
        </div>

        {/* Navegação pública */}
        <div className="flex justify-center gap-2 mb-6 text-xs flex-wrap">
          <Link to="/changelog" className="btn btn-sm">📋 Changelog</Link>
          <Link to="/roadmap" className="btn-ghost btn-sm">🗺 Roadmap</Link>
          <Link to="/leaderboard" className="btn-ghost btn-sm">🏆 Caçadores de Bugs</Link>
          <Link to="/tester/apply" className="btn-ghost btn-sm">📝 Virar Tester</Link>
        </div>

        {q.isLoading && <div className="text-center text-liberthia-300/60">Carregando...</div>}
        {q.isError && (
          <div className="card-glow text-center py-10 text-red-400">
            Erro ao carregar changelog. Tenta de novo daqui a pouco.
          </div>
        )}

        {entries.length === 0 && !q.isLoading && (
          <div className="card-glow text-center py-12">
            <div className="text-4xl mb-2 opacity-50">📄</div>
            <p className="text-liberthia-300/60">Sem releases publicadas ainda.</p>
          </div>
        )}

        {/* Highlighted (releases importantes em destaque) */}
        {highlighted.length > 0 && (
          <div className="mb-8">
            <h2 className="text-sm uppercase tracking-widest text-amber-300/70 font-bold mb-3 px-2">
              ⭐ Em destaque
            </h2>
            <div className="space-y-3">
              {highlighted.map(e => (
                <ChangelogCompactCard key={e.id} entry={e} highlighted onClick={() => setSelected(e)} />
              ))}
            </div>
          </div>
        )}

        {/* Restante */}
        {rest.length > 0 && (
          <div>
            {highlighted.length > 0 && (
              <h2 className="text-sm uppercase tracking-widest text-liberthia-300/50 font-bold mb-3 px-2">
                📚 Histórico
              </h2>
            )}
            <div className="space-y-3">
              {rest.map(e => (
                <ChangelogCompactCard key={e.id} entry={e} onClick={() => setSelected(e)} />
              ))}
            </div>
          </div>
        )}

        {/* Footer público */}
        <div className="text-center mt-8 text-[10px] text-liberthia-300/40">
          Liberthia mod · {new Date().getFullYear()}
        </div>
      </div>

      {/* Modal de detalhe — abre quando user clica num card */}
      <ChangelogDetailModal entry={selected} onClose={() => setSelected(null)} />
    </div>
  )
}

/** Conta items separados por vírgula ou ponto-e-vírgula na string. */
function countItems(content: string | null | undefined): number {
  if (!content || content.trim() === '') return 0
  // Aceita ", " ou "; " como separador. Filtra entries vazias.
  return content
    .split(/[,;]\s*/)
    .map(s => s.trim())
    .filter(Boolean)
    .length
}

/**
 * Card compacto — só preview. Click abre o modal de detalhe.
 * Mostra contadores agregados pra dar uma noção do tamanho da release sem
 * precisar abrir (X items adicionados, Y bugs corrigidos, etc).
 */
function ChangelogCompactCard({ entry: e, highlighted = false, onClick }: {
  entry: ChangelogEntryDto;
  highlighted?: boolean;
  onClick: () => void;
}) {
  const date = new Date(e.releaseDate).toLocaleDateString('pt-BR', {
    day: '2-digit', month: 'short', year: 'numeric',
  })
  const counts = {
    items: countItems(e.itemsAdded),
    bugs: countItems(e.bugsFixed),
    buffs: countItems(e.buffs),
    debuffs: countItems(e.debuffs),
    integrations: countItems(e.integrations),
  }
  return (
    <button onClick={onClick}
      className={`card-glow w-full text-left hover:!border-purple-400/70 transition-all cursor-pointer ${
        highlighted ? '!bg-amber-500/10 !border-amber-400/40 shadow-lg shadow-amber-500/20' : ''
      }`}>
      <div className="flex items-start justify-between gap-3 mb-2 flex-wrap">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="text-xs font-mono badge badge-purple">{e.version}</span>
            {highlighted && <span className="text-xs">⭐ destaque</span>}
            <span className="text-[10px] text-liberthia-300/60">{date}</span>
          </div>
          <h3 className="text-lg font-bold gradient-text">{e.title}</h3>
        </div>
      </div>

      {e.summary && (
        <p className="text-xs text-liberthia-300/80 italic border-l-2 border-purple-400/40 pl-2 line-clamp-2 mb-3">
          {e.summary}
        </p>
      )}

      {/* Contadores agregados — dá noção da release sem abrir o detalhe */}
      <div className="flex flex-wrap gap-1.5 text-[10px]">
        {counts.items > 0 && (
          <span className="badge !bg-emerald-500/15 !border-emerald-400/40 text-emerald-300">
            🆕 {counts.items} {counts.items === 1 ? 'item' : 'items'}
          </span>
        )}
        {counts.bugs > 0 && (
          <span className="badge !bg-amber-500/15 !border-amber-400/40 text-amber-300">
            🐛 {counts.bugs} bug{counts.bugs > 1 ? 's' : ''}
          </span>
        )}
        {counts.buffs > 0 && (
          <span className="badge !bg-green-500/15 !border-green-400/40 text-green-300">
            ⬆ {counts.buffs} buff{counts.buffs > 1 ? 's' : ''}
          </span>
        )}
        {counts.debuffs > 0 && (
          <span className="badge !bg-red-500/15 !border-red-400/40 text-red-300">
            ⬇ {counts.debuffs} nerf{counts.debuffs > 1 ? 's' : ''}
          </span>
        )}
        {counts.integrations > 0 && (
          <span className="badge badge-purple">
            🔌 {counts.integrations} integ
          </span>
        )}
        <span className="ml-auto text-[10px] text-purple-300/70">ver detalhes →</span>
      </div>
    </button>
  )
}

/* Modal e DetailSection movidos pra components/ChangelogDetailModal.tsx pra
 * serem reaproveitados também pelo card de Changelog no overview do tester. */
