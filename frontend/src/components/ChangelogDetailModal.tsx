import { useEffect, useMemo } from 'react'
import { ChangelogEntryDto, ChangelogBugDto, ChangelogSuggestionDto } from '../lib/api'

/**
 * Modal de detalhe de uma entrada de changelog — usado pela página pública
 * `/changelog` e pelo card de "📋 Changelog" no overview do dashboard tester.
 * Extraído pra componente compartilhado pra não duplicar a renderização das
 * sections + lógica de ESC + split de itens.
 *
 * Mostra TODOS os campos com conteúdo (esconde sections vazias):
 *   - summary (destaque com borda lateral)
 *   - itemsAdded, bugsFixed, buffs, debuffs, integrations, credits
 *   - notes (formato preformatted)
 *
 * Cada section quebra a string por separador (vírgula, ponto-e-vírgula) e
 * renderiza em lista — mais legível que a string crua de 2000 chars.
 */
export function ChangelogDetailModal({ entry, onClose }: {
  entry: ChangelogEntryDto | null;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!entry) return
    function onKey(e: KeyboardEvent) { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [entry, onClose])

  // Parse defensivo dos JSON-strings de bugs e suggestions. Se inválido, fica
  // vazio e o modal cai no render legacy (Sections de string puro).
  const parsedBugs = useMemo<ChangelogBugDto[]>(() => {
    if (!entry?.bugsJson) return []
    try {
      const arr = JSON.parse(entry.bugsJson)
      return Array.isArray(arr) ? arr : []
    } catch { return [] }
  }, [entry?.bugsJson])
  const parsedSuggestions = useMemo<ChangelogSuggestionDto[]>(() => {
    if (!entry?.suggestionsJson) return []
    try {
      const arr = JSON.parse(entry.suggestionsJson)
      return Array.isArray(arr) ? arr : []
    } catch { return [] }
  }, [entry?.suggestionsJson])

  if (!entry) return null

  const date = new Date(entry.releaseDate).toLocaleDateString('pt-BR', {
    day: '2-digit', month: 'long', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })

  return (
    <div className="fixed inset-0 z-[9999] flex items-start justify-center bg-black/80 backdrop-blur-sm p-4 overflow-y-auto"
         onClick={onClose}>
      <div className={`card-glow max-w-3xl w-full my-8 ${
        entry.highlighted ? '!bg-amber-500/10 !border-amber-400/40' : ''
      }`} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-start justify-between gap-3 mb-3 pb-3 border-b border-purple-500/20">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap mb-1">
              <span className="text-xs font-mono badge badge-purple">{entry.version}</span>
              {entry.highlighted && <span className="text-xs">⭐ destaque</span>}
              <span className="text-[10px] text-liberthia-300/60">{date}</span>
            </div>
            <h2 className="text-2xl font-bold gradient-text">{entry.title}</h2>
          </div>
          <button onClick={onClose} className="btn-ghost btn-sm shrink-0" title="Fechar (ESC)">
            ✕
          </button>
        </div>

        {entry.summary && (
          <div className="mb-4 p-3 rounded bg-purple-500/10 border-l-4 border-purple-400/60">
            <p className="text-sm text-liberthia-100/90 italic">{entry.summary}</p>
          </div>
        )}

        <DetailSection emoji="🆕" title="Items adicionados" content={entry.itemsAdded} color="emerald" />

        {/* Bugs: schema v2 (parsedBugs) vence o legacy string. Se tem dados
            estruturados, renderiza cards com tudo (reporter, severidade,
            screenshot, etc). Senão cai pro DetailSection antigo de string. */}
        {parsedBugs.length > 0 ? (
          <BugsSection bugs={parsedBugs} />
        ) : (
          <DetailSection emoji="🐛" title="Bugs corrigidos" content={entry.bugsFixed} color="amber" />
        )}

        {parsedSuggestions.length > 0 && (
          <SuggestionsSection suggestions={parsedSuggestions} />
        )}

        <DetailSection emoji="⬆️" title="Buffs" content={entry.buffs} color="green" />
        <DetailSection emoji="⬇️" title="Debuffs / Nerfs" content={entry.debuffs} color="red" />
        <DetailSection emoji="🔌" title="Integrações com outros mods" content={entry.integrations} color="purple" />
        <DetailSection emoji="🏆" title="Créditos" content={entry.credits} color="amber" />

        {entry.notes && (
          <div className="mt-4 p-3 rounded bg-liberthia-900/40 border border-liberthia-600/30">
            <div className="text-xs font-bold text-liberthia-300/70 mb-1">ℹ Notas</div>
            <div className="text-xs text-liberthia-300/80 italic whitespace-pre-wrap">{entry.notes}</div>
          </div>
        )}

        <div className="mt-4 pt-3 border-t border-purple-500/20 flex justify-end">
          <button onClick={onClose} className="btn-ghost btn-sm">Fechar</button>
        </div>
      </div>
    </div>
  )
}

/**
 * Section especial pra bugs estruturados (schema v2). Cada bug vira um card
 * com header (id, severity, priority, status), descrição, screenshot (se
 * houver — imagem clicável que abre em tab nova) e badge do reporter +
 * pontos. Replace do DetailSection antigo de string quando o backend tem
 * bugsJson populado.
 */
function BugsSection({ bugs }: { bugs: ChangelogBugDto[] }) {
  const sevColor: Record<string, string> = {
    low: 'text-emerald-300 bg-emerald-500/10',
    medium: 'text-amber-300 bg-amber-500/10',
    high: 'text-orange-300 bg-orange-500/10',
    critical: 'text-red-300 bg-red-500/10',
  }
  const fixed = bugs.filter(b => (b.status ?? 'fixed') === 'fixed')
  const pending = bugs.filter(b => b.status === 'pending')

  return (
    <div className="mt-3">
      <h4 className="text-sm font-bold mb-2 text-amber-300">
        🐛 Bugs corrigidos
        <span className="ml-2 text-[10px] text-liberthia-300/40">
          ({fixed.length} resolvidos{pending.length > 0 ? ` · ${pending.length} pendentes` : ''})
        </span>
      </h4>
      <div className="space-y-2">
        {bugs.map((b, idx) => {
          const sevClass = sevColor[b.severity ?? ''] || 'text-liberthia-300/60 bg-liberthia-900/40'
          const status = b.status ?? 'fixed'
          return (
            <div key={b.id ?? idx}
              className={`rounded p-2.5 border ${
                status === 'fixed'
                  ? 'bg-emerald-500/5 border-emerald-400/20'
                  : 'bg-amber-500/5 border-amber-400/20'
              }`}>
              <div className="flex items-baseline gap-1.5 flex-wrap mb-1">
                {b.id != null && (
                  <span className="font-mono text-[9px] text-liberthia-300/50">#{b.id}</span>
                )}
                <span className={`badge text-[9px] ${status === 'fixed' ? 'badge-green' : 'badge-yellow'}`}>
                  {status === 'fixed' ? '✅ corrigido' : '⏳ pendente'}
                </span>
                {b.severity && (
                  <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold ${sevClass}`}>
                    {b.severity}
                  </span>
                )}
                {b.priority && (
                  <span className="text-[9px] px-1.5 py-0.5 rounded bg-purple-500/15 text-purple-300">
                    prio: {b.priority}
                  </span>
                )}
                {b.fixedIn && (
                  <span className="text-[9px] text-liberthia-300/50">→ {b.fixedIn}</span>
                )}
              </div>
              <div className="font-bold text-sm mb-1">{b.title}</div>
              {b.itemId && (
                <div className="text-[10px] font-mono text-liberthia-300/50 mb-1">{b.itemId}</div>
              )}
              {b.description && (
                <p className="text-xs text-liberthia-300/80 mb-1.5 line-clamp-3">{b.description}</p>
              )}
              {b.fixDetails && (
                <details className="mt-1">
                  <summary className="cursor-pointer text-[10px] text-purple-300/70 hover:text-purple-300">
                    🔧 Como foi corrigido
                  </summary>
                  <div className="text-[11px] text-liberthia-300/80 mt-1 bg-liberthia-900/40 p-2 rounded">
                    {b.fixDetails}
                  </div>
                </details>
              )}
              {/* Screenshot inline (image) ou link */}
              {b.screenshotUrl && (
                <div className="mt-1.5">
                  {/\.(png|jpe?g|gif|webp)(\?|$)/i.test(b.screenshotUrl) ? (
                    <a href={b.screenshotUrl} target="_blank" rel="noreferrer">
                      <img src={b.screenshotUrl} alt="evidência"
                        className="max-h-32 rounded border border-purple-500/30" />
                    </a>
                  ) : (
                    <a href={b.screenshotUrl} target="_blank" rel="noreferrer"
                      className="text-[10px] text-purple-300 hover:underline">
                      📸 Ver evidência →
                    </a>
                  )}
                </div>
              )}
              {/* Footer: reporter + pontos + data */}
              <div className="flex items-baseline gap-2 mt-1.5 pt-1.5 border-t border-liberthia-700/30 text-[10px] text-liberthia-300/60 flex-wrap">
                {b.reporterMcName && (
                  <span className="inline-flex items-center gap-1">
                    <img src={`https://mc-heads.net/avatar/${b.reporterMcName}/16`}
                         className="w-3.5 h-3.5 rounded" alt="" />
                    por <b className="text-purple-200">{b.reporterMcName}</b>
                  </span>
                )}
                {b.pointsAwarded != null && b.pointsAwarded > 0 && (
                  <span className="text-purple-300 font-bold">⭐ +{b.pointsAwarded}</span>
                )}
                {b.triagedAt && (
                  <span className="ml-auto">triado em {new Date(b.triagedAt).toLocaleDateString('pt-BR')}</span>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

/**
 * Section pra sugestões aprovadas/implementadas incluídas nesta versão.
 * Cada uma vira um card com autor, score, status e descrição.
 */
function SuggestionsSection({ suggestions }: { suggestions: ChangelogSuggestionDto[] }) {
  const typeIcon: Record<string, string> = {
    ITEM: '🗡', BLOCK: '🧱', ARTIFACT: '🏺', MECHANIC: '⚙', MOB: '👾', OTHER: '✨',
  }
  return (
    <div className="mt-3">
      <h4 className="text-sm font-bold mb-2 text-purple-300">
        💡 Sugestões aceitas da comunidade
        <span className="ml-2 text-[10px] text-liberthia-300/40">
          ({suggestions.length})
        </span>
      </h4>
      <div className="space-y-2">
        {suggestions.map((s, idx) => (
          <div key={s.id ?? idx}
            className="rounded p-2.5 border bg-purple-500/5 border-purple-400/20">
            <div className="flex items-baseline gap-1.5 flex-wrap mb-1">
              <span className="text-lg">{typeIcon[s.type ?? ''] ?? '✨'}</span>
              <span className={`badge text-[9px] ${s.status === 'IMPLEMENTED' ? 'badge-green' : 'badge-purple'}`}>
                {s.status === 'IMPLEMENTED' ? '🎉 implementada' : '✅ aceita'}
              </span>
              {s.score != null && (
                <span className={`text-[9px] font-bold ${
                  s.score > 0 ? 'text-emerald-300' : s.score < 0 ? 'text-red-300' : 'text-liberthia-300'
                }`}>
                  {s.score > 0 ? '+' : ''}{s.score} score
                </span>
              )}
            </div>
            <div className="font-bold text-sm mb-1">{s.title}</div>
            {s.suggestedItemId && (
              <div className="text-[10px] font-mono text-liberthia-300/50 mb-1">{s.suggestedItemId}</div>
            )}
            {s.iconUrl && (
              <img src={s.iconUrl} alt="" className="max-h-24 rounded border border-purple-500/30 mb-1" />
            )}
            {s.description && (
              <p className="text-xs text-liberthia-300/80 mb-1.5 line-clamp-3">{s.description}</p>
            )}
            {s.adminNote && (
              <div className="mt-1 p-1.5 text-[10px] bg-amber-900/15 border border-amber-500/20 rounded text-amber-200/90">
                <b>Admin:</b> {s.adminNote}
              </div>
            )}
            <div className="flex items-baseline gap-2 mt-1.5 pt-1.5 border-t border-liberthia-700/30 text-[10px] text-liberthia-300/60 flex-wrap">
              {s.authorMcName && (
                <span className="inline-flex items-center gap-1">
                  <img src={`https://mc-heads.net/avatar/${s.authorMcName}/16`}
                       className="w-3.5 h-3.5 rounded" alt="" />
                  por <b className="text-purple-200">{s.authorMcName}</b>
                </span>
              )}
              {s.upvotes != null && (
                <span>👍 {s.upvotes} 👎 {s.downvotes ?? 0}</span>
              )}
              {s.createdAt && (
                <span className="ml-auto">{new Date(s.createdAt).toLocaleDateString('pt-BR')}</span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function DetailSection({ emoji, title, content, color }: {
  emoji: string; title: string; content: string | null | undefined; color: string;
}) {
  if (!content || content.trim() === '') return null
  // Split por vírgula/ponto-e-vírgula seguidos de uma "palavra que parece
  // ser início de novo item" (maiúscula, emoji, número#, etc) — evita split
  // dentro de descrições longas onde a vírgula é só pontuação natural.
  const lines = content
    .split(/[,;]\s*(?=[A-Z🆕🐛⬆⬇🔌🏆🪞⚠✅❌📦🧪⚙🏺]|#\d|\d+\.\d|[a-z]+:)/)
    .flatMap(s => s.split('\n'))
    .map(l => l.trim())
    .filter(Boolean)

  if (lines.length === 0) return null

  const colorMap: Record<string, string> = {
    emerald: 'text-emerald-300',
    amber: 'text-amber-300',
    green: 'text-green-300',
    red: 'text-red-300',
    purple: 'text-purple-300',
  }

  return (
    <div className="mt-3">
      <h4 className={`text-sm font-bold mb-2 ${colorMap[color] ?? 'text-liberthia-300'}`}>
        {emoji} {title}
        <span className="ml-2 text-[10px] text-liberthia-300/40">({lines.length})</span>
      </h4>
      <ul className="text-xs space-y-1 ml-2">
        {lines.map((l, i) => (
          <li key={i} className="text-liberthia-300/85 leading-relaxed">
            <span className="text-liberthia-300/40 mr-1.5">•</span>
            {l.replace(/^[-•*]\s*/, '')}
          </li>
        ))}
      </ul>
    </div>
  )
}
