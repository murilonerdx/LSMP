import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { testerApi, wikiApi } from '../lib/api'
import { toast } from '../store/toast'
import { BulkImportButton } from '../components/BulkImportButton'

/**
 * Seções extras do painel admin de testers — paridade com as tabs do
 * dashboard de tester. Antes só o admin tinha invites/packages/items/bugs/
 * rewards/changelog/roadmap mas o tester podia criar splashes, balances e
 * sugestões, sem o admin ter onde triá-las pela UI (precisava chamar a API
 * direto via curl). Aqui tem:
 *
 *  - SplashesAdminSection — lista TODAS splashes (qualquer status), triagem
 *    inline (PENDING → APPROVED/REJECTED/RETIRED + adminNote), botão delete.
 *  - BalanceAdminSection  — lista pedidos de buff/nerf, triagem inline com
 *    nota + pontos de bônus pro tester (se confirmar).
 *  - WikiAdminSection     — CRUD completo de entries de feature wiki (lista
 *    todas, edita inline, publica/despublica, deleta).
 *
 * Cada seção é auto-contida (não compartilha estado com outras). Usa o
 * mesmo padrão visual do resto do painel (card-glow, btn, badge-*).
 */

// ============================================================
// SPLASHES ADMIN
// ============================================================

const SPLASH_STATUS_COLOR: Record<string, string> = {
  PENDING: 'badge-yellow', APPROVED: 'badge-green',
  REJECTED: 'badge-red', RETIRED: '',
}

const SPLASH_CAT_ICON: Record<string, string> = {
  FUNNY: '😄', LORE: '📜', WARNING: '⚠',
  TECHNICAL: '⚙', EVENT: '🎉', META: '🌀',
}

export function SplashesAdminSection() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<string>('PENDING')
  const [triageOpen, setTriageOpen] = useState<any | null>(null)
  const [triageStatus, setTriageStatus] = useState<string>('APPROVED')
  const [triageNote, setTriageNote] = useState('')

  // Endpoint /api/tester/splashes é público (AuthFilter libera /api/tester/*)
  // e aceita ?status= pra filtrar. Funciona sem tester token (extractMc
  // retorna null mas o list ainda funciona, só não inclui myVote).
  const q = useQuery({
    queryKey: ['admin-splashes', filter],
    queryFn: () => testerApi.listSplashes(filter),
    refetchInterval: 15_000,
  })

  function openTriage(s: any) {
    setTriageOpen(s)
    setTriageStatus(s.status === 'PENDING' ? 'APPROVED' : s.status)
    setTriageNote(s.adminNote ?? '')
  }

  async function executeTriage() {
    if (!triageOpen) return
    try {
      await testerApi.adminTriageSplash(triageOpen.id, triageStatus, triageNote)
      qc.invalidateQueries({ queryKey: ['admin-splashes'] })
      toast.ok(`✨ splash ${triageStatus.toLowerCase()}`)
      setTriageOpen(null)
    } catch (e: any) { toast.err(e.message) }
  }

  async function del(id: number) {
    if (!confirm('Deletar splash?')) return
    try {
      await testerApi.adminDeleteSplash(id)
      qc.invalidateQueries({ queryKey: ['admin-splashes'] })
      toast.ok('🗑 deletado')
    } catch (e: any) { toast.err(e.message) }
  }

  const splashes = (q.data as any)?.splashes ?? []
  const allCounts = ['PENDING', 'APPROVED', 'REJECTED', 'RETIRED', 'all']

  return (
    <div className="space-y-3">
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">✨ Splashes:</b> mensagens que os testers propõem pra
        aparecer na tela inicial do mod (estilo "Also try Minecraft Dungeons!"). Aprove as boas,
        rejeite as ruins — só APROVADAS aparecem in-game.
      </div>

      <div className="flex gap-1 flex-wrap">
        {allCounts.map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`btn-ghost btn-sm text-[10px] ${filter === s ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
            {s === 'all' ? '📋 Todas' : s === 'PENDING' ? '⏳ Pend' : s === 'APPROVED' ? '✅ Aprov' : s === 'REJECTED' ? '❌ Rejeit' : '📦 Aposent'}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
        {splashes.length === 0 && (
          <div className="card col-span-full text-center py-8 text-xs text-liberthia-300/50">
            nenhuma splash com filtro "{filter}"
          </div>
        )}
        {splashes.map((s: any) => (
          <div key={s.id} className="card-glow">
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1 mb-1 flex-wrap">
                  <span className="text-base">{SPLASH_CAT_ICON[s.category] ?? '✨'}</span>
                  <span className={`badge ${SPLASH_STATUS_COLOR[s.status]} text-[9px]`}>{s.status}</span>
                  <span className="badge text-[9px]">{s.category}</span>
                </div>
                <div className="text-xs font-bold break-words" style={{ color: s.colorHex ?? '#A78BFA' }}>
                  "{s.text}"
                </div>
                <div className="text-[9px] text-liberthia-300/50 mt-1">
                  por <b>{s.authorMcName ?? s.proposerMcName ?? '?'}</b> · ⭐ {s.score ?? 0}
                </div>
              </div>
            </div>
            {s.adminNote && (
              <div className="text-[10px] text-amber-200/80 italic mt-1">📝 {s.adminNote}</div>
            )}
            <div className="flex gap-1 mt-2">
              <button className="btn-ghost btn-sm text-[10px] flex-1" onClick={() => openTriage(s)}>
                ✏ Triar
              </button>
              <button className="btn-danger btn-sm text-[10px]" onClick={() => del(s.id)}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {triageOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
             onClick={() => setTriageOpen(null)}>
          <div className="card-glow max-w-md w-full" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-bold mb-3 gradient-text">✨ Triar splash</h3>
            <div className="bg-liberthia-900/40 rounded p-3 mb-3 text-xs">
              "{triageOpen.text}"
              <div className="text-[9px] text-liberthia-300/50 mt-1">por {triageOpen.authorMcName ?? triageOpen.proposerMcName}</div>
            </div>
            <div className="mb-3">
              <label className="label text-[10px]">Status</label>
              <select className="input text-sm" value={triageStatus} onChange={(e) => setTriageStatus(e.target.value)}>
                <option value="APPROVED">✅ Aprovada (entra no rotation in-game)</option>
                <option value="REJECTED">❌ Rejeitada</option>
                <option value="RETIRED">📦 Aposentada (já apareceu, agora descansa)</option>
                <option value="PENDING">⏳ Pendente (volta pra fila)</option>
              </select>
            </div>
            <div className="mb-3">
              <label className="label text-[10px]">Nota (opcional)</label>
              <textarea className="input text-xs" rows={2} value={triageNote}
                onChange={(e) => setTriageNote(e.target.value)} />
            </div>
            <div className="flex gap-2">
              <button className="btn flex-1" onClick={executeTriage}>💾 Salvar</button>
              <button className="btn-ghost" onClick={() => setTriageOpen(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ============================================================
// BALANCE ADMIN (buff/nerf requests)
// ============================================================

const BAL_STATUS_COLOR: Record<string, string> = {
  PENDING: 'badge-yellow', UNDER_REVIEW: 'badge-purple',
  ACCEPTED: 'badge-green', REJECTED: 'badge-red', IMPLEMENTED: 'badge-green',
}

export function BalanceAdminSection() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<string>('all')
  const [triageOpen, setTriageOpen] = useState<any | null>(null)
  const [triageStatus, setTriageStatus] = useState<string>('UNDER_REVIEW')
  const [triageNote, setTriageNote] = useState('')
  const [triagePts, setTriagePts] = useState(0)

  const q = useQuery({
    queryKey: ['admin-balance', filter],
    queryFn: () => testerApi.listBalance(filter),
    refetchInterval: 15_000,
  })

  function openTriage(b: any) {
    setTriageOpen(b)
    setTriageStatus(b.status === 'PENDING' ? 'UNDER_REVIEW' : b.status)
    setTriageNote(b.adminNote ?? '')
    setTriagePts(0)
  }

  async function executeTriage() {
    if (!triageOpen) return
    try {
      await testerApi.adminTriageBalance(
        triageOpen.id, triageStatus, triageNote,
        triagePts > 0 ? triagePts : undefined,
      )
      qc.invalidateQueries({ queryKey: ['admin-balance'] })
      qc.invalidateQueries({ queryKey: ['tester-ranking'] })
      toast.ok(`⚖ pedido ${triageStatus.toLowerCase()}${triagePts > 0 ? ` · +${triagePts} pts` : ''}`)
      setTriageOpen(null)
    } catch (e: any) { toast.err(e.message) }
  }

  async function del(id: number) {
    if (!confirm('Deletar pedido?')) return
    try {
      await testerApi.adminDeleteBalance(id)
      qc.invalidateQueries({ queryKey: ['admin-balance'] })
      toast.ok('🗑 deletado')
    } catch (e: any) { toast.err(e.message) }
  }

  const requests = (q.data as any)?.requests ?? []

  return (
    <div className="space-y-3">
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">⚖ Buff/Nerf:</b> testers reportam que algo tá overpowered
        (precisa nerf) ou underpowered (precisa buff). Você triagga, vê evidência, dá pontos se
        for genuíno e marca IMPLEMENTED quando ajustar.
      </div>

      <div className="flex gap-1 flex-wrap">
        {['all', 'PENDING', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'IMPLEMENTED'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`btn-ghost btn-sm text-[10px] ${filter === s ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
            {s === 'all' ? '📋 Todos' : s}
          </button>
        ))}
      </div>

      <div className="space-y-2">
        {requests.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">
            nenhum pedido com filtro "{filter}"
          </div>
        )}
        {requests.map((b: any) => (
          <div key={b.id} className="card-glow">
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-2 flex-wrap">
                  <span className="badge text-[9px]">{b.type ?? '?'}</span>
                  <h4 className="font-bold truncate">{b.title}</h4>
                </div>
                <div className="text-[10px] text-liberthia-300/60 mt-1">
                  por <b>{b.testerMcName}</b> · #{b.id} · {b.createdAt && new Date(b.createdAt).toLocaleString('pt-BR')}
                </div>
                {b.itemId && (
                  <div className="text-[10px] font-mono text-liberthia-300/50 mt-1">{b.itemId}</div>
                )}
              </div>
              <span className={`badge ${BAL_STATUS_COLOR[b.status] ?? ''} text-[10px]`}>{b.status}</span>
            </div>
            <p className="text-xs text-liberthia-300/80 whitespace-pre-wrap line-clamp-3">{b.description}</p>
            {(b.currentBehavior || b.proposedBehavior) && (
              <details className="mt-2">
                <summary className="cursor-pointer text-[10px] text-liberthia-300/60">Atual ↔ Proposto</summary>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-1">
                  {b.currentBehavior && (
                    <div className="bg-liberthia-900/40 rounded p-2 text-[10px]">
                      <b className="text-amber-300">Atual:</b> {b.currentBehavior}
                    </div>
                  )}
                  {b.proposedBehavior && (
                    <div className="bg-liberthia-900/40 rounded p-2 text-[10px]">
                      <b className="text-emerald-300">Proposto:</b> {b.proposedBehavior}
                    </div>
                  )}
                </div>
              </details>
            )}
            {b.evidenceUrl && (
              <a href={b.evidenceUrl} target="_blank" rel="noreferrer"
                className="block mt-2 text-[10px] text-purple-300 hover:underline">📸 Evidência →</a>
            )}
            {b.adminNote && (
              <div className="mt-2 text-[10px] text-amber-200/80 italic">📝 {b.adminNote}</div>
            )}
            <div className="flex gap-1 mt-2">
              <button className="btn-ghost btn-sm text-[10px] flex-1" onClick={() => openTriage(b)}>
                ✏ Triar
              </button>
              <button className="btn-danger btn-sm text-[10px]" onClick={() => del(b.id)}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {triageOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
             onClick={() => setTriageOpen(null)}>
          <div className="card-glow max-w-lg w-full" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-bold mb-3 gradient-text">⚖ Triar pedido de balance</h3>
            <div className="bg-liberthia-900/40 rounded p-3 mb-3 text-xs">
              <b>{triageOpen.title}</b>
              <div className="text-[9px] text-liberthia-300/50 mt-1">por {triageOpen.testerMcName}</div>
            </div>
            <div className="mb-3">
              <label className="label text-[10px]">Status</label>
              <select className="input text-sm" value={triageStatus} onChange={(e) => setTriageStatus(e.target.value)}>
                <option value="UNDER_REVIEW">🔍 Em análise</option>
                <option value="ACCEPTED">✅ Aceito (vai ser ajustado)</option>
                <option value="IMPLEMENTED">✨ Implementado (já foi)</option>
                <option value="REJECTED">❌ Rejeitado</option>
                <option value="PENDING">⏳ Volta pra fila</option>
              </select>
            </div>
            <div className="mb-3">
              <label className="label text-[10px]">Bônus de pontos (opcional)</label>
              <input type="number" className="input text-sm" min={0} max={100}
                value={triagePts} onChange={(e) => setTriagePts(Number(e.target.value) || 0)}
                placeholder="0 = sem bônus" />
              <div className="text-[9px] text-liberthia-300/50 mt-0.5">
                Dá pontos extras se o pedido for útil (mesmo que não implementado ainda).
              </div>
            </div>
            <div className="mb-3">
              <label className="label text-[10px]">Nota do admin</label>
              <textarea className="input text-xs" rows={3} value={triageNote}
                onChange={(e) => setTriageNote(e.target.value)}
                placeholder="Ex: vai pro v110, valor de damage reduzido de 12 → 8" />
            </div>
            <div className="flex gap-2">
              <button className="btn flex-1" onClick={executeTriage}>💾 Salvar</button>
              <button className="btn-ghost" onClick={() => setTriageOpen(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ============================================================
// WIKI ADMIN
// ============================================================

const WIKI_CAT_ICON: Record<string, string> = {
  ITEM: '🗡', BLOCK: '🧱', ARTIFACT: '🏺', MECHANIC: '⚙', MOB: '👾',
  RITUAL: '🔮', TOOL: '🔧', ARMOR: '🛡', WEAPON: '⚔', OTHER: '✨',
}

const EMPTY_WIKI = {
  id: null as number | null,
  slug: '',
  title: '',
  category: 'ITEM' as string,
  itemId: '',
  summary: '',
  contentMd: '',
  imageUrl: '',
  tags: '',
  addedInVersion: '',
  creditsJson: '',
  published: true,
}

export function WikiAdminSection() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<typeof EMPTY_WIKI | null>(null)

  const q = useQuery({
    queryKey: ['admin-feature-wiki'],
    queryFn: wikiApi.adminList,
    refetchInterval: 30_000,
  })

  async function save() {
    if (!editing) return
    if (!editing.slug.trim() || !editing.title.trim()) {
      return toast.err('slug e title obrigatórios')
    }
    try {
      await wikiApi.adminSave(editing)
      qc.invalidateQueries({ queryKey: ['admin-feature-wiki'] })
      setEditing(null)
      toast.ok('💾 wiki salvo')
    } catch (e: any) { toast.err(e.message) }
  }

  async function del(id: number) {
    if (!confirm('Deletar entry da wiki?')) return
    try {
      await wikiApi.adminDelete(id)
      qc.invalidateQueries({ queryKey: ['admin-feature-wiki'] })
      toast.ok('🗑 deletado')
    } catch (e: any) { toast.err(e.message) }
  }

  const entries = (q.data as any)?.entries ?? []

  return (
    <div className="space-y-3">
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">📚 Wiki:</b> documentação dos items/blocos/mecânicas que os
        testers podem consultar in-game. Cada entry tem texto markdown + créditos pra quem ajudou
        no bug ou sugestão. Só PUBLISHED aparece pros testers.
      </div>

      <div className="flex items-center gap-2 flex-wrap">
        <button className="btn" onClick={() => setEditing({ ...EMPTY_WIKI })}>+ Nova entry</button>
        <BulkImportButton
          importer={wikiApi.bulkImport}
          onSuccess={() => qc.invalidateQueries({ queryKey: ['admin-feature-wiki'] })}
          label="Importar wiki JSON"
          hint="wiki-update.json"
        />
        <div className="text-[10px] text-liberthia-300/50">{entries.length} entries</div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
        {entries.length === 0 && (
          <div className="card col-span-full text-center py-8 text-xs text-liberthia-300/50">
            nenhuma entry — comece criando a primeira
          </div>
        )}
        {entries.map((e: any) => (
          <div key={e.id} className={`card-glow ${!e.published ? 'opacity-60' : ''}`}>
            <div className="flex items-start gap-2 mb-2">
              {e.imageUrl ? (
                <img src={e.imageUrl} alt="" className="w-10 h-10 object-cover rounded" />
              ) : (
                <div className="w-10 h-10 rounded bg-purple-500/20 flex items-center justify-center text-xl">
                  {WIKI_CAT_ICON[e.category] ?? '✨'}
                </div>
              )}
              <div className="flex-1 min-w-0">
                <h3 className="font-bold text-xs truncate">{e.title}</h3>
                <div className="text-[9px] font-mono text-liberthia-300/50 truncate">{e.slug}</div>
                <div className="flex gap-1 mt-1 flex-wrap">
                  <span className="badge text-[8px]">{e.category}</span>
                  {!e.published && <span className="badge badge-yellow text-[8px]">DRAFT</span>}
                  {e.addedInVersion && <span className="badge badge-purple text-[8px]">{e.addedInVersion}</span>}
                </div>
              </div>
            </div>
            {e.summary && <p className="text-[10px] text-liberthia-300/70 line-clamp-2">{e.summary}</p>}
            <div className="flex gap-1 mt-2">
              <button className="btn-ghost btn-sm text-[10px] flex-1"
                onClick={() => setEditing({ ...EMPTY_WIKI, ...e })}>✏ Editar</button>
              <button className="btn-danger btn-sm text-[10px]" onClick={() => del(e.id)}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <div className="fixed inset-0 z-[9999] flex items-start justify-center bg-black/80 backdrop-blur-sm p-4 overflow-y-auto"
             onClick={() => setEditing(null)}>
          <div className="card-glow max-w-2xl w-full my-8" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-bold mb-3 gradient-text">
              {editing.id ? `✏ Editar #${editing.id}` : '📚 Nova entry'}
            </h3>
            <div className="space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <div>
                  <label className="label text-[10px]">Slug (URL — único)</label>
                  <input className="input text-xs font-mono"
                    value={editing.slug} onChange={(e) => setEditing({ ...editing, slug: e.target.value })}
                    placeholder="matter-analyzer" disabled={!!editing.id} />
                </div>
                <div>
                  <label className="label text-[10px]">Categoria</label>
                  <select className="input text-sm"
                    value={editing.category} onChange={(e) => setEditing({ ...editing, category: e.target.value })}>
                    {Object.keys(WIKI_CAT_ICON).map(c => (
                      <option key={c} value={c}>{WIKI_CAT_ICON[c]} {c}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="label text-[10px]">Título</label>
                <input className="input text-sm"
                  value={editing.title} onChange={(e) => setEditing({ ...editing, title: e.target.value })} />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <div>
                  <label className="label text-[10px]">Item ID (Minecraft)</label>
                  <input className="input text-xs font-mono"
                    value={editing.itemId ?? ''} onChange={(e) => setEditing({ ...editing, itemId: e.target.value })}
                    placeholder="liberthia:matter_analyzer" />
                </div>
                <div>
                  <label className="label text-[10px]">Versão (added in)</label>
                  <input className="input text-xs"
                    value={editing.addedInVersion ?? ''}
                    onChange={(e) => setEditing({ ...editing, addedInVersion: e.target.value })}
                    placeholder="v0.1.13" />
                </div>
              </div>
              <div>
                <label className="label text-[10px]">Imagem (URL)</label>
                <input className="input text-xs"
                  value={editing.imageUrl ?? ''} onChange={(e) => setEditing({ ...editing, imageUrl: e.target.value })} />
              </div>
              <div>
                <label className="label text-[10px]">Resumo (1-2 frases)</label>
                <textarea className="input text-xs" rows={2}
                  value={editing.summary ?? ''} onChange={(e) => setEditing({ ...editing, summary: e.target.value })} />
              </div>
              <div>
                <label className="label text-[10px]">Conteúdo (Markdown)</label>
                <textarea className="input text-xs font-mono" rows={10}
                  value={editing.contentMd ?? ''} onChange={(e) => setEditing({ ...editing, contentMd: e.target.value })}
                  placeholder="# Como usar&#10;&#10;1. Crafte com 2 ferro + 1 redstone&#10;2. Coloque no chão e..." />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <div>
                  <label className="label text-[10px]">Tags (vírgula)</label>
                  <input className="input text-xs"
                    value={editing.tags ?? ''} onChange={(e) => setEditing({ ...editing, tags: e.target.value })}
                    placeholder="combat, multiplayer" />
                </div>
                <div>
                  <label className="label text-[10px]">Créditos (JSON array)</label>
                  <input className="input text-xs font-mono"
                    value={editing.creditsJson ?? ''}
                    onChange={(e) => setEditing({ ...editing, creditsJson: e.target.value })}
                    placeholder='["Steve","Alex"]' />
                </div>
              </div>
              <label className="text-xs flex items-center gap-1">
                <input type="checkbox" checked={editing.published}
                  onChange={(e) => setEditing({ ...editing, published: e.target.checked })} />
                Publicado (aparece pros testers)
              </label>
            </div>
            <div className="flex gap-2 mt-4">
              <button className="btn flex-1" onClick={save}>💾 Salvar</button>
              <button className="btn-ghost" onClick={() => setEditing(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
