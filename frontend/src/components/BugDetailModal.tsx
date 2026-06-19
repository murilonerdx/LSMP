import { useEffect } from 'react'

/**
 * Modal único de detalhe de bug — usado tanto pela tela do tester (community
 * bugs / meus bugs) quanto pela tela do admin (triagem). Antes cada card
 * mostrava só título + descrição truncados; pra ver steps/repro/triagem o
 * usuário tinha que abrir vários `<details>` e o card crescia muito.
 *
 * Aqui o card mostra resumo + botão "ver detalhes" → abre esse modal com:
 *   - Header com status, severidade, prioridade, frequência
 *   - Item afetado (link pro autocomplete) + screenshot
 *   - Descrição, comportamento esperado, workaround, passos, como encontrou
 *   - Triagem (canReplicate, affectsOthers, tags) + nota do admin
 *   - Confirmações de outros testers (+1)
 *   - Ações admin (confirmar/rejeitar) injetadas via prop `actions`
 *
 * Props.bug é o objeto vindo do backend (todos os campos do BugReport).
 */
export type BugLike = {
  id: number
  testerMcName?: string
  title: string
  description?: string
  stepsToReproduce?: string
  howFound?: string
  itemId?: string
  betaItemId?: number
  modVersion?: string
  mcVersion?: string
  worldContext?: string
  screenshotUrl?: string
  severity?: string
  frequency?: string
  priority?: string
  canReplicate?: boolean
  affectsOthers?: boolean
  expectedBehavior?: string
  workaround?: string
  tags?: string
  replicationCount?: number
  status?: string
  pointsAwarded?: number
  adminNote?: string
  triagedBy?: string
  triagedAt?: string
  createdAt?: string
}

export type BugDetailModalProps = {
  bug: BugLike | null
  onClose: () => void
  /** Ações no rodapé (botões admin de confirmar/rejeitar, etc). */
  actions?: React.ReactNode
  /**
   * Botão "Eu também vi isso (+1)" — só aparece se for da tela community
   * de tester. Admin não vê isso (não faz sentido admin +1-ar bug).
   */
  onConfirmReplication?: () => void
}

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'badge-yellow', CONFIRMED: 'badge-green',
  REJECTED: 'badge-red', DUPLICATE: 'badge-purple',
}

const SEV_COLOR: Record<string, string> = {
  low: 'text-emerald-300', medium: 'text-amber-300',
  high: 'text-orange-300', critical: 'text-red-300',
}

const FREQ_LABEL: Record<string, string> = {
  always: '🔴 Sempre', often: '🟠 Frequente',
  rare: '🟡 Raro', once: '⚪ 1× só',
}

const PRIORITY_LABEL: Record<string, string> = {
  low: '🟢 Baixa', medium: '🟡 Média',
  high: '🟠 Alta', blocker: '🔴 Bloqueia',
}

export function BugDetailModal({ bug, onClose, actions, onConfirmReplication }: BugDetailModalProps) {
  // ESC fecha o modal. UseEffect porque depende de bug !== null.
  useEffect(() => {
    if (!bug) return
    function onKey(e: KeyboardEvent) { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [bug, onClose])

  if (!bug) return null

  const tags = (bug.tags ?? '').split(',').map(t => t.trim()).filter(Boolean)

  return (
    <div className="fixed inset-0 z-[9999] flex items-start justify-center bg-black/70 backdrop-blur-sm p-4 overflow-y-auto"
         onClick={onClose}>
      <div className="card-glow max-w-3xl w-full my-8" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-start justify-between gap-3 mb-3 pb-3 border-b border-purple-500/20">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap text-[10px] mb-1">
              <span className={`badge ${STATUS_COLOR[bug.status ?? 'PENDING']}`}>
                {bug.status ?? 'PENDING'}
              </span>
              {bug.severity && (
                <span className={`badge ${SEV_COLOR[bug.severity] ?? ''}`}>
                  ⚠ {bug.severity}
                </span>
              )}
              {bug.priority && (
                <span className="badge">{PRIORITY_LABEL[bug.priority] ?? bug.priority}</span>
              )}
              {bug.frequency && (
                <span className="badge">{FREQ_LABEL[bug.frequency] ?? bug.frequency}</span>
              )}
              {bug.pointsAwarded != null && bug.pointsAwarded > 0 && (
                <span className="badge badge-purple">⭐ +{bug.pointsAwarded} pts</span>
              )}
              {bug.replicationCount != null && bug.replicationCount > 0 && (
                <span className="badge badge-purple">+{bug.replicationCount} replicou</span>
              )}
            </div>
            <h2 className="text-base font-bold gradient-text break-words">{bug.title}</h2>
            <div className="text-[10px] text-liberthia-300/60 mt-1">
              #{bug.id}
              {bug.testerMcName && <> · por <b>{bug.testerMcName}</b></>}
              {bug.createdAt && <> · {new Date(bug.createdAt).toLocaleString('pt-BR')}</>}
            </div>
          </div>
          <button className="btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>

        {/* Metadata grid (env + item) */}
        {(bug.modVersion || bug.mcVersion || bug.worldContext || bug.itemId) && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2 mb-3 text-[10px]">
            {bug.modVersion && <MetaCard label="Mod" value={bug.modVersion} />}
            {bug.mcVersion && <MetaCard label="MC" value={bug.mcVersion} />}
            {bug.worldContext && <MetaCard label="Dimensão" value={bug.worldContext} />}
            {bug.itemId && <MetaCard label="Item" value={bug.itemId} mono />}
          </div>
        )}

        {/* Flags de triagem */}
        {(bug.canReplicate != null || bug.affectsOthers != null) && (
          <div className="flex flex-wrap gap-3 text-[11px] mb-3">
            {bug.canReplicate != null && (
              <span className={bug.canReplicate ? 'text-emerald-300' : 'text-liberthia-300/50'}>
                {bug.canReplicate ? '✓' : '✗'} Reproduz consistentemente
              </span>
            )}
            {bug.affectsOthers != null && (
              <span className={bug.affectsOthers ? 'text-amber-300' : 'text-liberthia-300/50'}>
                {bug.affectsOthers ? '✓' : '✗'} Afeta outros players
              </span>
            )}
          </div>
        )}

        {/* Tags */}
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {tags.map(t => (
              <span key={t} className="badge text-[9px]">🏷 {t}</span>
            ))}
          </div>
        )}

        {/* Screenshot */}
        {bug.screenshotUrl && (
          <div className="mb-3">
            {/\.(png|jpe?g|gif|webp)(\?|$)/i.test(bug.screenshotUrl) ? (
              <a href={bug.screenshotUrl} target="_blank" rel="noreferrer">
                <img src={bug.screenshotUrl} alt="evidência"
                  className="max-h-64 rounded border border-purple-500/30" />
              </a>
            ) : (
              <a href={bug.screenshotUrl} target="_blank" rel="noreferrer"
                className="text-purple-300 hover:underline text-xs">
                📸 Ver evidência →
              </a>
            )}
          </div>
        )}

        {/* Seções de texto */}
        <Section title="Descrição" body={bug.description} />
        <Section title="✅ Comportamento esperado" body={bug.expectedBehavior} />
        <Section title="🔍 Como encontrou" body={bug.howFound} />
        <Section title="🪜 Passos pra reproduzir" body={bug.stepsToReproduce} preformat />
        <Section title="🩹 Workaround" body={bug.workaround} />

        {/* Triagem do admin */}
        {(bug.adminNote || bug.triagedBy) && (
          <div className="mt-3 p-3 bg-amber-900/20 border border-amber-500/30 rounded text-xs">
            <div className="font-bold text-amber-200 mb-1">📝 Nota do admin</div>
            {bug.adminNote && <p className="text-amber-100 whitespace-pre-wrap">{bug.adminNote}</p>}
            {bug.triagedBy && (
              <div className="text-[9px] text-amber-200/60 mt-1">
                triado por {bug.triagedBy}
                {bug.triagedAt && <> · {new Date(bug.triagedAt).toLocaleString('pt-BR')}</>}
              </div>
            )}
          </div>
        )}

        {/* Footer: ações */}
        <div className="mt-4 pt-3 border-t border-purple-500/20 flex flex-wrap gap-2">
          {onConfirmReplication && bug.status === 'PENDING' && (
            <button onClick={onConfirmReplication}
              className="btn btn-sm bg-emerald-600/80 hover:bg-emerald-600">
              ✋ Eu também vi isso (+1)
            </button>
          )}
          {actions}
          <button onClick={onClose} className="btn-ghost btn-sm ml-auto">Fechar</button>
        </div>
      </div>
    </div>
  )
}

function MetaCard({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="bg-liberthia-900/40 rounded p-2">
      <div className="text-[9px] uppercase text-liberthia-300/50">{label}</div>
      <div className={`mt-0.5 break-words ${mono ? 'font-mono text-[10px]' : 'text-xs'}`}>{value}</div>
    </div>
  )
}

function Section({ title, body, preformat }: { title: string; body?: string; preformat?: boolean }) {
  if (!body || !body.trim()) return null
  return (
    <div className="mt-3">
      <div className="text-[10px] font-bold uppercase text-purple-300/80 mb-1">{title}</div>
      {preformat ? (
        <pre className="text-[11px] whitespace-pre-wrap bg-liberthia-900/40 p-2 rounded border border-purple-500/10">{body}</pre>
      ) : (
        <p className="text-xs text-liberthia-300/90 whitespace-pre-wrap">{body}</p>
      )}
    </div>
  )
}
