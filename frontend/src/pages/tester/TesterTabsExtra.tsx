import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { testerApi } from '../../lib/api'

/**
 * Tabs adicionais do dashboard de tester:
 *   - Splashes (sugestões de mensagens de loading screen)
 *   - Balance (pedidos de buff/nerf)
 *   - Notifications (inbox)
 *
 * Componentes exportados são montados pelo TesterDashboardPage via switch case.
 */

// ============ SPLASH ============

export function SplashesTab() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<'all' | 'PENDING' | 'APPROVED' | 'REJECTED'>('all')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ text: '', colorHex: '#A78BFA', category: 'FUNNY' })
  const [busy, setBusy] = useState(false)

  const q = useQuery({
    queryKey: ['tester-splashes', filter],
    queryFn: () => testerApi.listSplashes(filter),
    refetchInterval: 30_000,
  })

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.text.trim()) return
    setBusy(true)
    try {
      await testerApi.createSplash(form)
      setForm({ text: '', colorHex: '#A78BFA', category: 'FUNNY' })
      setShowForm(false)
      qc.invalidateQueries({ queryKey: ['tester-splashes'] })
    } catch (e: any) { alert(e.message) }
    finally { setBusy(false) }
  }

  async function vote(id: number, current: number, n: 1 | -1) {
    const v = (current === n ? 0 : n) as 1 | 0 | -1
    try {
      await testerApi.voteSplash(id, v)
      qc.invalidateQueries({ queryKey: ['tester-splashes'] })
    } catch (e: any) { alert(e.message) }
  }

  const statusBadge: Record<string, string> = {
    PENDING: 'badge-yellow', APPROVED: 'badge-green', REJECTED: 'badge-red', RETIRED: '',
  }

  const catIcon: Record<string, string> = {
    FUNNY: '😄', LORE: '📜', WARNING: '⚠', TECHNICAL: '⚙', EVENT: '🎉', META: '🌀',
  }

  return (
    <div className="space-y-3">
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">🎮 Splashes:</b> mensagens curtas e estilosas que aparecem na tela inicial do mod, igual ao "Also try Minecraft Dungeons!". Propõe a sua, vota nas dos outros — admin escolhe quais entram.
      </div>

      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex gap-1 flex-wrap">
          {(['all', 'PENDING', 'APPROVED', 'REJECTED'] as const).map(s => (
            <button key={s} onClick={() => setFilter(s)}
              className={`btn-ghost btn-sm text-[10px] ${filter === s ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
              {s === 'all' ? 'Todas' : s === 'PENDING' ? '⏳ Pend' : s === 'APPROVED' ? '✅ Aprovadas' : '❌ Rejeitadas'}
            </button>
          ))}
        </div>
        <button className="btn btn-sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancelar' : '+ Nova splash'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={submit} className="card-glow space-y-2">
          <input className="input text-xs" maxLength={200}
            value={form.text} onChange={(e) => setForm({ ...form, text: e.target.value })}
            placeholder="Ex: Forjado nas estrelas! · Cuidado com a matéria escura..."
            required />
          <div className="flex gap-2">
            <select className="input text-xs flex-1" value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}>
              <option value="FUNNY">😄 Engraçada</option>
              <option value="LORE">📜 Lore</option>
              <option value="WARNING">⚠ Aviso</option>
              <option value="TECHNICAL">⚙ Técnica</option>
              <option value="EVENT">🎉 Evento</option>
              <option value="META">🌀 Meta</option>
            </select>
            <input type="color" value={form.colorHex}
              onChange={(e) => setForm({ ...form, colorHex: e.target.value })}
              className="w-12 h-10 rounded cursor-pointer bg-transparent border border-purple-500/30" />
          </div>
          <div className="rounded p-3 bg-black/40 text-center font-bold text-sm"
            style={{ color: form.colorHex }}>
            {form.text || 'Preview do splash aqui'}
          </div>
          <button type="submit" className="btn w-full" disabled={busy}>{busy ? '...' : '✨ Enviar splash'}</button>
        </form>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
        {q.data?.splashes?.length === 0 && (
          <div className="card col-span-full text-center py-6 text-xs text-liberthia-300/50">
            Nenhuma splash {filter !== 'all' && `(${filter.toLowerCase()})`} ainda
          </div>
        )}
        {q.data?.splashes?.map((s: any) => (
          <div key={s.id} className="card-glow">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xl">{catIcon[s.category]}</span>
              <span className={`badge ${statusBadge[s.status]} text-[9px]`}>{s.status}</span>
              <span className="text-[10px] text-liberthia-300/40 ml-auto">por <b>{s.authorMcName}</b></span>
            </div>
            <div className="rounded bg-black/40 p-3 text-center font-bold mb-2 text-sm break-words"
              style={{ color: s.colorHex || '#A78BFA' }}>
              "{s.text}"
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1">
                <button onClick={() => vote(s.id, s.myVote, 1)}
                  className={`text-base px-2 py-1 rounded ${s.myVote === 1 ? 'bg-emerald-500/30 text-emerald-300' : 'text-liberthia-300/40 hover:text-emerald-300'}`}>
                  ▲ {s.upvotes}
                </button>
                <button onClick={() => vote(s.id, s.myVote, -1)}
                  className={`text-base px-2 py-1 rounded ${s.myVote === -1 ? 'bg-red-500/30 text-red-300' : 'text-liberthia-300/40 hover:text-red-300'}`}>
                  ▼ {s.downvotes}
                </button>
              </div>
              <span className={`text-xs font-bold ${s.score > 0 ? 'text-emerald-300' : s.score < 0 ? 'text-red-300' : 'text-liberthia-300/50'}`}>
                {s.score >= 0 ? '+' : ''}{s.score}
              </span>
            </div>
            {s.adminNote && (
              <div className="mt-2 p-2 bg-amber-900/20 border border-amber-500/30 rounded text-[10px] text-amber-100">
                <b>Admin:</b> {s.adminNote}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ BALANCE (BUFF/NERF) ============

export function BalanceTab() {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [tab, setTab] = useState<'mine' | 'all'>('mine')
  const [form, setForm] = useState({
    type: 'BUFF',
    itemId: '',
    itemDisplayName: '',
    title: '',
    description: '',
    currentBehavior: '',
    proposedBehavior: '',
    evidenceUrl: '',
    testContext: 'solo',
  })
  const [busy, setBusy] = useState(false)

  const mineQ = useQuery({
    queryKey: ['tester-balance-mine'],
    queryFn: testerApi.myBalance,
    refetchInterval: 30_000,
    enabled: tab === 'mine',
  })
  const allQ = useQuery({
    queryKey: ['tester-balance-all'],
    queryFn: () => testerApi.listBalance(),
    refetchInterval: 60_000,
    enabled: tab === 'all',
  })

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.itemId.trim() || !form.title.trim() || !form.description.trim()) return
    setBusy(true)
    try {
      await testerApi.createBalance(form)
      setForm({ ...form, itemId: '', itemDisplayName: '', title: '', description: '', currentBehavior: '', proposedBehavior: '', evidenceUrl: '' })
      setShowForm(false)
      qc.invalidateQueries({ queryKey: ['tester-balance-mine'] })
      qc.invalidateQueries({ queryKey: ['tester-balance-all'] })
    } catch (e: any) { alert(e.message) }
    finally { setBusy(false) }
  }

  const list = tab === 'mine' ? (mineQ.data?.requests ?? []) : (allQ.data?.requests ?? [])

  const statusBadge: Record<string, string> = {
    PENDING: 'badge-yellow', UNDER_REVIEW: 'badge-purple', APPROVED: 'badge-green',
    IN_DEVELOPMENT: 'badge-purple', PAUSED: '', IMPLEMENTED: 'badge-green', REJECTED: 'badge-red',
  }
  const typeIcon: Record<string, string> = { BUFF: '💪', NERF: '🪶', REWORK: '🔄', REMOVE: '🗑' }

  return (
    <div className="space-y-3">
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">⚖ Buffs/Nerfs:</b> testou um item in-game e achou que ele tá overpowered ou fraco demais? Manda um pedido de ajuste aqui. Admin avalia e move pro pipeline de desenvolvimento.
      </div>

      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex gap-1">
          <button onClick={() => setTab('mine')}
            className={`btn-ghost btn-sm text-[10px] ${tab === 'mine' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
            🙋 Meus pedidos
          </button>
          <button onClick={() => setTab('all')}
            className={`btn-ghost btn-sm text-[10px] ${tab === 'all' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
            🌐 Comunidade
          </button>
        </div>
        <button className="btn btn-sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancelar' : '+ Novo pedido'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={submit} className="card-glow space-y-2">
          <div className="grid grid-cols-2 gap-2">
            <select className="input text-xs" value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value })}>
              <option value="BUFF">💪 Buff (fortalecer)</option>
              <option value="NERF">🪶 Nerf (enfraquecer)</option>
              <option value="REWORK">🔄 Rework</option>
              <option value="REMOVE">🗑 Remover</option>
            </select>
            <select className="input text-xs" value={form.testContext}
              onChange={(e) => setForm({ ...form, testContext: e.target.value })}>
              <option value="solo">🧍 Solo</option>
              <option value="pvp">⚔ PvP</option>
              <option value="raid">🏰 Raid/Boss</option>
              <option value="dungeon">🗝 Dungeon</option>
              <option value="other">❓ Outro</option>
            </select>
          </div>
          <input className="input text-xs font-mono"
            placeholder="ID do item (ex: liberthia:dark_matter_sword)"
            value={form.itemId} onChange={(e) => setForm({ ...form, itemId: e.target.value })}
            required maxLength={256} />
          <input className="input text-xs"
            placeholder="Nome amigável (ex: Espada de Matéria Escura)"
            value={form.itemDisplayName} onChange={(e) => setForm({ ...form, itemDisplayName: e.target.value })}
            maxLength={256} />
          <input className="input text-xs"
            placeholder="Título resumido (ex: Dano mata mob em 1 hit)"
            value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
            required maxLength={256} />
          <textarea className="input text-xs" rows={2}
            placeholder="Descrição do que você quer mudar e por quê"
            value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
            required />
          <div className="grid md:grid-cols-2 gap-2">
            <textarea className="input text-xs" rows={3}
              placeholder="Como ele funciona HOJE"
              value={form.currentBehavior} onChange={(e) => setForm({ ...form, currentBehavior: e.target.value })} />
            <textarea className="input text-xs" rows={3}
              placeholder="Como você acha que DEVERIA funcionar"
              value={form.proposedBehavior} onChange={(e) => setForm({ ...form, proposedBehavior: e.target.value })} />
          </div>
          <input className="input text-xs"
            placeholder="URL de vídeo/screenshot (opcional)"
            value={form.evidenceUrl} onChange={(e) => setForm({ ...form, evidenceUrl: e.target.value })} />
          <button type="submit" className="btn w-full" disabled={busy}>{busy ? '...' : '⚖ Enviar pedido'}</button>
        </form>
      )}

      <div className="space-y-2">
        {list.length === 0 && (
          <div className="card text-center py-6 text-xs text-liberthia-300/50">
            Nenhum pedido {tab === 'mine' ? 'seu' : 'da comunidade'} ainda
          </div>
        )}
        {list.map((b: any) => (
          <div key={b.id} className="card-glow">
            <div className="flex items-start gap-2 mb-2 flex-wrap">
              <span className="text-xl">{typeIcon[b.type] ?? '⚖'}</span>
              <div className="flex-1 min-w-0">
                <h3 className="font-bold text-sm">{b.title}</h3>
                <div className="text-[10px] text-liberthia-300/50">
                  por <b>{b.testerMcName}</b> · {new Date(b.createdAt).toLocaleDateString()}
                  {b.testContext && <span className="ml-2">📍 {b.testContext}</span>}
                </div>
              </div>
              <div className="text-right">
                <span className={`badge ${statusBadge[b.status]} text-[9px]`}>{b.status}</span>
                {b.pointsAwarded > 0 && (
                  <div className="text-purple-300 text-xs font-bold mt-1">⭐ +{b.pointsAwarded}</div>
                )}
              </div>
            </div>
            <div className="text-[10px] font-mono bg-black/30 rounded p-1 mb-2 break-all">
              {b.itemId} {b.itemDisplayName && <span className="text-liberthia-300/70">— {b.itemDisplayName}</span>}
            </div>
            <p className="text-xs text-liberthia-300/80 mb-2 whitespace-pre-wrap">{b.description}</p>
            {(b.currentBehavior || b.proposedBehavior) && (
              <details className="mb-2">
                <summary className="cursor-pointer text-[10px] text-liberthia-300/60">Comportamento atual × proposto</summary>
                <div className="grid md:grid-cols-2 gap-2 mt-1 text-[10px]">
                  {b.currentBehavior && (
                    <div className="bg-red-900/20 rounded p-2"><b className="text-red-300">Hoje:</b><br />{b.currentBehavior}</div>
                  )}
                  {b.proposedBehavior && (
                    <div className="bg-emerald-900/20 rounded p-2"><b className="text-emerald-300">Proposto:</b><br />{b.proposedBehavior}</div>
                  )}
                </div>
              </details>
            )}
            {b.evidenceUrl && (
              <a href={b.evidenceUrl} target="_blank" rel="noreferrer"
                className="text-[10px] text-purple-300 hover:underline">📎 Evidência →</a>
            )}
            {b.adminNote && (
              <div className="mt-2 p-2 bg-amber-900/20 border border-amber-500/30 rounded text-[10px] text-amber-100">
                <b>Admin:</b> {b.adminNote}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ NOTIFICATIONS ============

export function NotificationsBell({ onOpen }: { onOpen?: () => void }) {
  const q = useQuery({
    queryKey: ['tester-notifications'],
    queryFn: testerApi.notifications,
    refetchInterval: 20_000,
  })
  const unread = q.data?.unread ?? 0
  return (
    <button onClick={onOpen}
      className="relative px-2 py-1 rounded-full hover:bg-purple-500/20 transition"
      title="Notificações">
      🔔
      {unread > 0 && (
        <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[9px] font-bold rounded-full min-w-[16px] h-4 px-1 flex items-center justify-center">
          {unread > 9 ? '9+' : unread}
        </span>
      )}
    </button>
  )
}

export function NotificationsPanel({ onClose }: { onClose: () => void }) {
  const qc = useQueryClient()
  const q = useQuery({
    queryKey: ['tester-notifications'],
    queryFn: testerApi.notifications,
    refetchInterval: 20_000,
  })

  // Auto-mark-all-as-read no PRIMEIRO render desse painel — assim que o user
  // abre as notificações, o badge vermelho de unread some imediatamente. Antes
  // o user tinha que clicar manualmente em "marcar lida" pra cada uma (ou em
  // "Marcar todas") — ruído de UX. Ref pra rodar só 1 vez por mount mesmo se
  // o componente re-render por refetch dos dados.
  const autoMarkDoneRef = useRef(false)
  useEffect(() => {
    if (autoMarkDoneRef.current) return
    if (!q.data) return  // espera carregar antes de marcar
    if ((q.data.unread ?? 0) === 0) {
      autoMarkDoneRef.current = true
      return
    }
    autoMarkDoneRef.current = true
    testerApi.markAllNotifRead().then(() => {
      qc.invalidateQueries({ queryKey: ['tester-notifications'] })
    }).catch(() => {
      // Se falhar (rede offline, etc), reseta a flag pra tentar de novo no
      // próximo render. Sem isso ficaria preso em "unread mas marcado como
      // marcado" se a primeira tentativa falhar.
      autoMarkDoneRef.current = false
    })
  }, [q.data, qc])

  async function markRead(id: number) {
    try { await testerApi.markNotifRead(id); qc.invalidateQueries({ queryKey: ['tester-notifications'] }) }
    catch {}
  }
  async function markAll() {
    try { await testerApi.markAllNotifRead(); qc.invalidateQueries({ queryKey: ['tester-notifications'] }) }
    catch {}
  }
  async function del(id: number) {
    try { await testerApi.deleteNotif(id); qc.invalidateQueries({ queryKey: ['tester-notifications'] }) }
    catch {}
  }
  /**
   * Apaga TODAS as notificações lidas em série. Sem endpoint bulk-delete no
   * backend, faz N requests paralelos via Promise.all. Funciona com 50+
   * notificações sem travar o painel — Promise.all é não-bloqueante.
   */
  async function clearAllRead() {
    const readIds = (q.data?.notifications ?? []).filter((n: any) => n.isRead).map((n: any) => n.id)
    if (readIds.length === 0) return
    if (!confirm(`Apagar ${readIds.length} notificação(ões) lida(s)?`)) return
    try {
      await Promise.all(readIds.map((id: number) => testerApi.deleteNotif(id)))
      qc.invalidateQueries({ queryKey: ['tester-notifications'] })
    } catch (e) {
      // Mesmo se algumas falharem, invalida pra mostrar o estado atual
      qc.invalidateQueries({ queryKey: ['tester-notifications'] })
    }
  }

  const list = q.data?.notifications ?? []
  const unread = q.data?.unread ?? 0
  const readCount = list.filter((n: any) => n.isRead).length

  const typeEmoji: Record<string, string> = {
    BUG_CONFIRMED: '🐛', BUG_REJECTED: '❌',
    SUGGESTION_APPROVED: '✅', SUGGESTION_REJECTED: '❌', SUGGESTION_IMPLEMENTED: '🎉',
    SUGGESTION_STATUS_CHANGED: '📝',
    SPLASH_APPROVED: '✨', SPLASH_REJECTED: '❌',
    REDEMPTION_DELIVERED: '🎁',
    BALANCE_APPROVED: '⚖', BALANCE_REJECTED: '❌', BALANCE_IMPLEMENTED: '🎉',
    WIKI_CREDIT: '📚', ADMIN_MESSAGE: '💬', SYSTEM: '🔔',
  }

  return (
    <div className="fixed inset-0 z-40 flex items-start justify-end p-3 sm:p-6 pointer-events-none">
      <div className="absolute inset-0 bg-black/60 pointer-events-auto" onClick={onClose} />
      <div className="card-glow w-full max-w-sm max-h-[80vh] overflow-auto pointer-events-auto relative">
        <div className="sticky top-0 bg-liberthia-900/95 backdrop-blur p-2 flex items-center justify-between border-b border-purple-500/20">
          <h3 className="font-bold gradient-text text-sm">
            🔔 Notificações
            {unread > 0 && <span className="text-red-300"> ({unread})</span>}
          </h3>
          <div className="flex gap-1">
            {unread > 0 && (
              <button className="btn-ghost btn-sm text-[9px]" onClick={markAll}
                title="Marcar todas como lidas">✓ todas</button>
            )}
            {readCount > 0 && (
              <button className="btn-ghost btn-sm text-[9px] text-red-300 hover:bg-red-500/10"
                onClick={clearAllRead}
                title={`Apagar as ${readCount} notificação(ões) já lidas`}>
                🗑 limpar lidas
              </button>
            )}
            <button className="btn-ghost btn-sm text-[9px]" onClick={onClose}>✕</button>
          </div>
        </div>

        {list.length === 0 ? (
          <div className="text-center text-xs text-liberthia-300/50 py-12">
            <div className="text-3xl mb-2">📭</div>
            Nenhuma notificação
          </div>
        ) : (
          <ul className="divide-y divide-purple-500/10">
            {list.map((n: any) => (
              <li key={n.id} className={`p-3 ${n.isRead ? 'opacity-60' : 'bg-purple-500/5'}`}>
                <div className="flex items-start gap-2">
                  <span className="text-lg">{typeEmoji[n.type] ?? '🔔'}</span>
                  <div className="flex-1 min-w-0">
                    <div className="font-bold text-xs truncate">{n.title}</div>
                    {n.body && <div className="text-[10px] text-liberthia-300/70 mt-0.5 line-clamp-2">{n.body}</div>}
                    <div className="text-[9px] text-liberthia-300/40 mt-1">{new Date(n.createdAt).toLocaleString()}</div>
                  </div>
                  <div className="flex flex-col gap-1">
                    {!n.isRead && (
                      <button className="text-[9px] text-purple-300 hover:underline" onClick={() => markRead(n.id)}>marcar lida</button>
                    )}
                    <button className="text-[9px] text-liberthia-300/40 hover:text-red-300" onClick={() => del(n.id)}>×</button>
                  </div>
                </div>
                {n.link && (
                  <a href={n.link} className="text-[10px] text-purple-300 hover:underline mt-1 inline-block">
                    abrir →
                  </a>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

// ============ WIKI ============

export function WikiTab() {
  const [category, setCategory] = useState<string>('all')
  const [search, setSearch] = useState('')
  const [tagFilter, setTagFilter] = useState<string | null>(null)
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null)
  // Busca client-side: puxa SEMPRE todas as entries (category='all') e filtra
  // localmente por categoria + texto + tag. Assim o search é instantâneo e os
  // contadores por categoria refletem o catálogo inteiro, não só o filtrado.
  const listQ = useQuery({
    queryKey: ['wiki-list', 'all'],
    queryFn: () => import('../../lib/api').then(m => m.wikiApi.list('all')),
    refetchInterval: 60_000,
  })
  const detailQ = useQuery({
    queryKey: ['wiki-detail', selectedSlug],
    queryFn: () => import('../../lib/api').then(m => m.wikiApi.get(selectedSlug!)),
    enabled: !!selectedSlug,
  })

  const catIcon: Record<string, string> = {
    ITEM: '🗡', BLOCK: '🧱', ARTIFACT: '🏺', MECHANIC: '⚙', MOB: '👾',
    RITUAL: '🔮', TOOL: '🔧', ARMOR: '🛡', WEAPON: '⚔', OTHER: '✨',
  }

  const allEntries: any[] = listQ.data?.entries ?? []

  // contador por categoria (catálogo inteiro)
  const catCounts = useMemo(() => {
    const c: Record<string, number> = { all: allEntries.length }
    for (const e of allEntries) c[e.category] = (c[e.category] ?? 0) + 1
    return c
  }, [allEntries])

  // todas as tags presentes (pra chips de filtro), ordenadas por frequência
  const allTags = useMemo(() => {
    const freq: Record<string, number> = {}
    for (const e of allEntries) {
      String(e.tags ?? '').split(',').map((t: string) => t.trim()).filter(Boolean)
        .forEach((t: string) => { freq[t] = (freq[t] ?? 0) + 1 })
    }
    return Object.keys(freq).sort((a, b) => freq[b] - freq[a]).slice(0, 14)
  }, [allEntries])

  // lista filtrada: categoria + texto (title/summary/itemId/tags/versão) + tag
  const filtered = useMemo(() => {
    let list = allEntries
    if (category !== 'all') list = list.filter(e => e.category === category)
    if (tagFilter) {
      list = list.filter(e => String(e.tags ?? '').split(',')
        .map((t: string) => t.trim().toLowerCase()).includes(tagFilter.toLowerCase()))
    }
    const term = search.trim().toLowerCase()
    if (term) {
      list = list.filter(e => [e.title, e.summary, e.itemId, e.category, e.tags, e.addedInVersion]
        .filter(Boolean).join(' ').toLowerCase().includes(term))
    }
    return list
  }, [allEntries, category, tagFilter, search])

  if (selectedSlug && detailQ.data) {
    const d = detailQ.data
    const credits = d.creditsJson ? safeParseArray(d.creditsJson) : []
    return (
      <div className="space-y-3">
        <button className="btn-ghost btn-sm text-xs" onClick={() => setSelectedSlug(null)}>← Voltar</button>
        <article className="card-glow">
          <div className="flex items-start gap-3 mb-3">
            {d.imageUrl && <img src={d.imageUrl} alt={d.title} className="w-20 h-20 object-cover rounded" />}
            <div className="flex-1">
              <div className="flex items-center gap-2 flex-wrap mb-1">
                <span className="text-2xl">{catIcon[d.category] ?? '✨'}</span>
                <h1 className="text-xl font-bold gradient-text">{d.title}</h1>
              </div>
              {d.itemId && <div className="text-[10px] font-mono text-liberthia-300/60">{d.itemId}</div>}
              {d.summary && <p className="text-xs text-liberthia-300/80 mt-1">{d.summary}</p>}
              {d.addedInVersion && <span className="badge badge-purple text-[10px] mt-1 inline-block">+ {d.addedInVersion}</span>}
            </div>
          </div>
          {d.tags && (
            <div className="flex gap-1 flex-wrap mb-3">
              {d.tags.split(',').filter(Boolean).map((t: string) => (
                <span key={t} className="badge text-[9px]">{t.trim()}</span>
              ))}
            </div>
          )}
          <div className="prose prose-invert max-w-none text-sm">
            <pre className="whitespace-pre-wrap font-sans text-xs">{d.contentMd}</pre>
          </div>
          {credits.length > 0 && (
            <div className="mt-4 p-2 rounded bg-emerald-900/10 border border-emerald-500/30 text-[10px]">
              <b className="text-emerald-300">📚 Créditos:</b> {credits.join(', ')}
            </div>
          )}
        </article>
      </div>
    )
  }

  const hasFilters = !!search.trim() || category !== 'all' || !!tagFilter

  return (
    <div className="space-y-3">
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">📚 Wiki:</b> documentação dos items, blocos, mecânicas e ferramentas do mod. Cada entry tem instruções de uso, receita e créditos pra quem contribuiu com bugs/sugestões.
      </div>

      {/* Barra de busca + filtros — sticky no topo enquanto rola o catálogo */}
      <div className="sticky top-[68px] z-10 -mx-3 sm:-mx-4 px-3 sm:px-4 py-2 bg-liberthia-900/95 backdrop-blur border-b border-purple-500/20 space-y-2">
        {/* search input com ícone, glow no focus e botão limpar */}
        <div className="relative group">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-purple-300/70 pointer-events-none text-sm">🔍</span>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar na wiki — nome, ID, tag, versão..."
            className="input text-sm pl-9 pr-8 w-full transition focus:ring-2 focus:ring-purple-400/60 focus:border-purple-400/60"
            autoComplete="off"
          />
          {search && (
            <button type="button" onClick={() => setSearch('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-liberthia-300/60 hover:text-white text-xs"
              title="Limpar busca">✕</button>
          )}
        </div>

        {/* filtros de categoria com contadores */}
        <div className="flex gap-1 flex-wrap items-center">
          <FilterBtn active={category === 'all'} onClick={() => setCategory('all')}>
            📋 Todos <span className="opacity-60">({catCounts.all ?? 0})</span>
          </FilterBtn>
          {Object.keys(catIcon).filter(cat => (catCounts[cat] ?? 0) > 0).map(cat => (
            <FilterBtn key={cat} active={category === cat} onClick={() => setCategory(cat)}>
              {catIcon[cat]} {cat} <span className="opacity-60">({catCounts[cat]})</span>
            </FilterBtn>
          ))}
          <span className="text-[10px] text-liberthia-300/60 ml-auto whitespace-nowrap">
            {filtered.length} de {allEntries.length}
          </span>
        </div>

        {/* chips de tag (filtro adicional) — só aparece se há tags */}
        {allTags.length > 0 && (
          <div className="flex gap-1 flex-wrap items-center">
            <span className="text-[9px] uppercase tracking-wide text-liberthia-300/40 mr-0.5">tags:</span>
            {allTags.map(t => (
              <button key={t}
                onClick={() => setTagFilter(tagFilter === t ? null : t)}
                className={`px-2 py-0.5 rounded-full text-[9px] border transition ${
                  tagFilter === t
                    ? 'bg-purple-500/30 border-purple-400 text-white'
                    : 'bg-liberthia-800/40 border-liberthia-700/50 text-liberthia-300/70 hover:border-purple-400/50'
                }`}>
                #{t}
              </button>
            ))}
            {hasFilters && (
              <button onClick={() => { setSearch(''); setCategory('all'); setTagFilter(null) }}
                className="text-[9px] text-purple-300/70 hover:text-white ml-1 underline decoration-dotted">
                limpar tudo
              </button>
            )}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
        {allEntries.length === 0 && (
          <div className="card col-span-full text-center py-8 text-xs text-liberthia-300/50">
            Nenhuma entry de wiki ainda. Admin: comece a documentar features novas.
          </div>
        )}
        {allEntries.length > 0 && filtered.length === 0 && (
          <div className="card-glow col-span-full text-center py-8 text-liberthia-300/60">
            <div className="text-3xl mb-1 opacity-50">🔍</div>
            <p className="text-xs">Nenhuma entry bate com a busca{search ? ` "${search}"` : ''}.</p>
            <button onClick={() => { setSearch(''); setCategory('all'); setTagFilter(null) }}
              className="btn-ghost btn-sm text-[10px] mt-2">Limpar filtros</button>
          </div>
        )}
        {filtered.map((e: any) => (
          <button key={e.id} onClick={() => setSelectedSlug(e.slug)}
            className="card-glow text-left hover:scale-[1.02] transition cursor-pointer">
            <div className="flex items-start gap-2">
              {e.imageUrl ? (
                <img src={e.imageUrl} alt="" className="w-12 h-12 object-cover rounded" />
              ) : (
                <div className="w-12 h-12 rounded bg-purple-500/20 flex items-center justify-center text-2xl">
                  {catIcon[e.category]}
                </div>
              )}
              <div className="flex-1 min-w-0">
                <h3 className="font-bold text-xs truncate">{e.title}</h3>
                {e.summary && <p className="text-[10px] text-liberthia-300/70 line-clamp-2">{e.summary}</p>}
                {e.addedInVersion && <span className="badge badge-purple text-[9px] mt-1 inline-block">+ {e.addedInVersion}</span>}
              </div>
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}

function FilterBtn({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button onClick={onClick}
      className={`btn-ghost btn-sm text-[10px] ${active ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
      {children}
    </button>
  )
}

function safeParseArray(s: string): string[] {
  try { const x = JSON.parse(s); return Array.isArray(x) ? x : [] }
  catch { return [] }
}
