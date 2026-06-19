import { useState, useRef, useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { testerApi, publicApi, contentAdminApi, ChangelogEntryDto, RoadmapItemDto, api } from '../lib/api'
import { toast } from '../store/toast'
import { ItemAutocomplete, ItemIcon } from '../components/ItemAutocomplete'
import { RecipeBuilder } from '../components/RecipeBuilder'
import { BulkImportButton } from '../components/BulkImportButton'
import { BugDetailModal } from '../components/BugDetailModal'
import { SplashesAdminSection, BalanceAdminSection, WikiAdminSection } from './TesterAdminExtra'

/**
 * Admin: gerenciar todo o ciclo de mod testers. 5 sub-abas:
 *  🎫 Invites · 📦 Packages · 🧪 Beta Items · 🐛 Bugs · 🎁 Rewards
 *
 * Cada uma é um CRUD independente. Ranking aparece no topo sempre.
 */

type Section = 'applications' | 'invites' | 'testers' | 'server' | 'packages'
  | 'items' | 'models' | 'audios' | 'bugs' | 'suggestions' | 'splashes' | 'balance'
  | 'wiki' | 'rewards' | 'changelog' | 'roadmap'

// Lista única — fonte da verdade pras tabs. Antes era duplicada (cada
// SectionBtn no JSX + cada `{sec === 'x' && <X />}` no switch). Agora
// edita aqui pra adicionar/remover tab.
const ADMIN_TABS: { id: Section; label: string; icon: string }[] = [
  { id: 'applications', label: 'Inscrições', icon: '📝' },
  { id: 'invites',      label: 'Invites',    icon: '🎫' },
  { id: 'testers',      label: 'Testers',    icon: '👥' },
  { id: 'server',       label: 'Servidor',   icon: '🌐' },
  { id: 'packages',     label: 'Packages',   icon: '📦' },
  { id: 'items',        label: 'Items beta', icon: '🧪' },
  { id: 'models',       label: 'Modelos 3D', icon: '🧊' },
  { id: 'audios',       label: 'Áudios',     icon: '🔊' },
  { id: 'bugs',         label: 'Bugs',       icon: '🐛' },
  { id: 'suggestions',  label: 'Sugestões',  icon: '💡' },
  { id: 'splashes',     label: 'Splashes',   icon: '✨' },
  { id: 'balance',      label: 'Buff/Nerf',  icon: '⚖' },
  { id: 'wiki',         label: 'Wiki',       icon: '📚' },
  { id: 'rewards',      label: 'Rewards',    icon: '🎁' },
  { id: 'changelog',    label: 'Changelog',  icon: '📋' },
  { id: 'roadmap',      label: 'Roadmap',    icon: '🗺' },
]

export function TesterAdminPage() {
  const [sec, setSec] = useState<Section>('applications')
  const rankingQ = useQuery({ queryKey: ['tester-ranking'], queryFn: testerApi.adminRanking, refetchInterval: 30000 })

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4">
        <h1 className="page-title">🧪 Mod Testers</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Gerenciar testers: invites, pacotes ZIP, items beta, bug reports, splashes, balance e wiki.
        </p>
      </header>

      {/* Ranking topo */}
      <div className="card-glow mb-4">
        <h2 className="font-bold gradient-text mb-2 text-sm">🏆 Ranking (top 20)</h2>
        {rankingQ.isLoading ? (
          <div className="text-xs text-liberthia-300/60">Carregando...</div>
        ) : rankingQ.data?.ranking?.length === 0 ? (
          <div className="text-xs text-liberthia-300/50 italic">nenhum tester ainda</div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {rankingQ.data?.ranking?.slice(0, 10).map((t: any) => (
              <div key={t.mcName} className="flex items-center gap-2 bg-liberthia-900/40 rounded px-2 py-1 text-xs">
                <span className="font-bold w-6">
                  {t.rank === 1 ? '🥇' : t.rank === 2 ? '🥈' : t.rank === 3 ? '🥉' : `#${t.rank}`}
                </span>
                <span className="font-bold">{t.mcName}</span>
                <span className="text-purple-300 font-bold">⭐{t.points}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Sub-aba selector — grid responsivo: 2 colunas no mobile, até 8 no desktop.
          Não tem mais scroll lateral (overflow-x-auto removido); cresce em
          altura conforme aumenta de tabs, que é o comportamento esperado. */}
      <nav className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-1 mb-3 pb-1 border-b border-liberthia-600">
        {ADMIN_TABS.map(t => (
          <SectionBtn key={t.id} id={t.id} cur={sec} onClick={setSec}>
            {t.icon} {t.label}
          </SectionBtn>
        ))}
      </nav>

      {sec === 'applications' && <ApplicationsSection />}
      {sec === 'invites' && <InvitesSection />}
      {sec === 'testers' && <TestersSection />}
      {sec === 'server' && <ServerInfoSection />}
      {sec === 'packages' && <PackagesSection />}
      {sec === 'items' && <BetaItemsSection />}
      {sec === 'models' && <ModelsSection />}
      {sec === 'audios' && <AudiosSection />}
      {sec === 'bugs' && <BugsSection />}
      {sec === 'suggestions' && <SuggestionsAdminSection />}
      {sec === 'splashes' && <SplashesAdminSection />}
      {sec === 'balance' && <BalanceAdminSection />}
      {sec === 'wiki' && <WikiAdminSection />}
      {sec === 'rewards' && <RewardsSection />}
      {sec === 'changelog' && <ChangelogAdminSection />}
      {sec === 'roadmap' && <RoadmapAdminSection />}
    </div>
  )
}

function SectionBtn({ id, cur, onClick, children }:
  { id: Section; cur: Section; onClick: (s: Section) => void; children: React.ReactNode }) {
  const active = id === cur
  return (
    <button onClick={() => onClick(id)}
      className={`px-2 py-1.5 text-[11px] rounded text-center truncate transition-colors ${
        active ? 'bg-purple-500/30 text-white font-bold ring-1 ring-purple-400'
               : 'bg-liberthia-900/30 text-liberthia-300/70 hover:bg-liberthia-700/40 hover:text-white'
      }`}
      title={typeof children === 'string' ? children : undefined}>
      {children}
    </button>
  )
}

// ============ Invites ============
function InvitesSection() {
  const qc = useQueryClient()
  const [note, setNote] = useState('')
  const [onlyUnused, setOnlyUnused] = useState(false)
  const [busy, setBusy] = useState(false)
  const invitesQ = useQuery({
    queryKey: ['tester-invites', onlyUnused],
    queryFn: () => testerApi.adminListInvites(onlyUnused),
    refetchInterval: 10_000,
  })

  async function create() {
    setBusy(true)
    try {
      const r = await testerApi.adminCreateInvite(note.trim() || undefined)
      setNote('')
      qc.invalidateQueries({ queryKey: ['tester-invites'] })
      try { await navigator.clipboard.writeText(r.code) } catch {}
      toast.ok(`📋 ${r.code} (copiado)`)
    } catch (e: any) { toast.err(e.message) }
    finally { setBusy(false) }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <div className="space-y-3">
        <ApplicationsToggleSection />
        <div className="card-glow">
          <h3 className="font-bold mb-2">➕ Criar código (livre)</h3>
          <p className="text-[10px] text-liberthia-300/60 mb-2">
            Tester escolhe nome MC e usa esse código no registro
          </p>
          <input className="input text-xs mb-2" placeholder="Nota (opcional)"
            value={note} onChange={(e) => setNote(e.target.value)} maxLength={256} />
          <button className="btn w-full" onClick={create} disabled={busy}>
            {busy ? '...' : '🎫 Gerar'}
          </button>
        </div>
        <AutoCreateSection />
      </div>
      <div className="card-glow lg:col-span-2">
        <div className="flex items-center justify-between mb-2">
          <h3 className="font-bold">Códigos</h3>
          <label className="text-[11px]"><input type="checkbox" checked={onlyUnused}
            onChange={(e) => setOnlyUnused(e.target.checked)} /> só pendentes</label>
        </div>
        <div className="space-y-1 max-h-[500px] overflow-y-auto">
          {invitesQ.data?.invites?.length === 0 && (
            <div className="text-center py-4 text-xs text-liberthia-300/50 italic">nenhum código</div>
          )}
          {invitesQ.data?.invites?.map((inv: any) => (
            <div key={inv.id} className="text-xs p-2 rounded bg-liberthia-900/40 flex items-center gap-2">
              <button onClick={() => { navigator.clipboard.writeText(inv.code); toast.ok('📋') }}
                className="font-mono font-bold text-base tracking-widest text-purple-300">{inv.code}</button>
              <div className="flex-1 min-w-0">
                {inv.usedBy ? <span className="badge badge-green text-[10px]">✓ {inv.usedBy}</span>
                            : <span className="badge badge-yellow text-[10px]">⏳ pendente</span>}
                {inv.note && <div className="text-liberthia-300/60 truncate italic">"{inv.note}"</div>}
              </div>
              <button
                onClick={async () => {
                  if (!confirm(`Deletar invite ${inv.code}?${inv.usedBy ? `\n\n⚠ Já foi usado por ${inv.usedBy}. A conta dele continua, só a auditoria some.` : ''}`)) return
                  try { await testerApi.adminDeleteInvite(inv.id); qc.invalidateQueries({ queryKey: ['tester-invites'] }); toast.ok('🗑 deletado') }
                  catch (e: any) { toast.err(e.message) }
                }}
                className="text-red-300 hover:text-red-200 text-base px-1"
                title="Deletar invite">🗑</button>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ============ Auto-Create (admin gera conta pré-provisionada com claim link) ============
function AutoCreateSection() {
  const [mcName, setMcName] = useState('')
  const [note, setNote] = useState('')
  const [result, setResult] = useState<{ mcName: string; claimUrl: string; claimCode: string } | null>(null)
  const [busy, setBusy] = useState(false)

  async function create() {
    if (!mcName.trim()) return toast.err('mcName obrigatório')
    setBusy(true)
    try {
      const r = await testerApi.adminAutoCreate(mcName.trim(), note.trim() || undefined)
      if (!r.ok) throw new Error('falha')
      setResult({ mcName: r.mcName, claimUrl: r.claimUrl, claimCode: r.claimCode })
      setMcName(''); setNote('')
      try { await navigator.clipboard.writeText(r.claimUrl) } catch {}
      toast.ok(`✓ ${r.mcName} criado · link copiado`)
    } catch (e: any) { toast.err(e.message ?? 'erro') }
    finally { setBusy(false) }
  }

  return (
    <div className="card-glow !border-emerald-500/40 !bg-emerald-500/10">
      <h3 className="font-bold mb-1 text-emerald-300">⚡ Pre-Setup (sem código)</h3>
      <p className="text-[10px] text-liberthia-300/70 mb-2">
        Cria conta completa e gera link. Tester só define senha pra ativar.
      </p>
      <input className="input text-xs mb-2" placeholder="nick MC do tester"
        value={mcName} onChange={(e) => setMcName(e.target.value)} maxLength={64} />
      <input className="input text-xs mb-2" placeholder="Nota interna (opcional)"
        value={note} onChange={(e) => setNote(e.target.value)} maxLength={256} />
      <button className="btn w-full" onClick={create} disabled={busy}>
        {busy ? '...' : '🔗 Gerar conta + link'}
      </button>

      {result && (
        <div className="mt-3 p-2 rounded bg-black/50 border border-emerald-500/30 text-[10px]">
          <div className="text-emerald-300 font-bold">✓ Conta criada · {result.mcName}</div>
          <div className="mt-1 break-all">
            <b className="text-purple-300">URL:</b><br />
            <a href={result.claimUrl} target="_blank" rel="noreferrer"
              className="text-purple-300 hover:underline">{result.claimUrl}</a>
          </div>
          <div className="mt-1">
            <b className="text-purple-300">Código:</b> <span className="font-mono">{result.claimCode}</span>
          </div>
          <button
            className="btn btn-sm mt-2 w-full text-[10px]"
            onClick={async () => {
              try { await navigator.clipboard.writeText(result.claimUrl); toast.ok('📋 link copiado') }
              catch { toast.err('falha') }
            }}>📋 Copiar link de novo</button>
        </div>
      )}
    </div>
  )
}

// ============ Toggle do /tester/apply (inscrições abertas/fechadas) ============
function ApplicationsToggleSection() {
  const qc = useQueryClient()
  const q = useQuery({
    queryKey: ['tester-public-config'],
    queryFn: testerApi.publicConfig,
    refetchInterval: 60_000,
  })
  const enabled = q.data?.applicationsEnabled ?? true

  async function toggle() {
    try {
      await testerApi.adminSetApplicationsEnabled(!enabled)
      qc.invalidateQueries({ queryKey: ['tester-public-config'] })
      toast.ok(!enabled ? '✓ Inscrições abertas' : '⛔ Inscrições fechadas')
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className={`card-glow ${enabled ? '!border-emerald-500/40' : '!border-amber-500/40 !bg-amber-500/5'}`}>
      <h3 className="font-bold mb-1">
        {enabled ? '🟢' : '🟡'} Página de inscrições
      </h3>
      <p className="text-[10px] text-liberthia-300/60 mb-2">
        Liga/desliga a tela <code className="text-purple-300">/tester/apply</code> — quando desligada,
        novos players não conseguem se inscrever (mas testers existentes continuam acessando o painel).
      </p>
      <div className="flex items-center gap-2">
        <button
          onClick={toggle}
          className={`btn flex-1 ${enabled
            ? 'bg-emerald-600 hover:bg-emerald-700'
            : 'bg-amber-600 hover:bg-amber-700'}`}>
          {enabled ? '✓ Aberta — clique pra fechar' : '⛔ Fechada — clique pra abrir'}
        </button>
      </div>
    </div>
  )
}

// ============ Testers (ban, delete, reset password) ============
function TestersSection() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [showBanned, setShowBanned] = useState(true)

  const q = useQuery({
    queryKey: ['admin-testers-all'],
    queryFn: testerApi.adminListTesters,
    refetchInterval: 15_000,
  })

  const testers = (q.data?.testers ?? []).filter((t: any) => {
    if (!showBanned && !t.enabled) return false
    if (search.trim()) return t.mcName.toLowerCase().includes(search.toLowerCase())
    return true
  })

  async function ban(mcName: string) {
    const reason = prompt(`Motivo do ban de ${mcName}?`, '')
    if (reason === null) return
    try {
      await testerApi.adminBanTester(mcName, reason)
      qc.invalidateQueries({ queryKey: ['admin-testers-all'] })
      toast.ok(`🔨 ${mcName} banido`)
    } catch (e: any) { toast.err(e.message) }
  }
  async function unban(mcName: string) {
    if (!confirm(`Desbanir ${mcName}?`)) return
    try {
      await testerApi.adminUnbanTester(mcName)
      qc.invalidateQueries({ queryKey: ['admin-testers-all'] })
      toast.ok(`✅ ${mcName} reativado`)
    } catch (e: any) { toast.err(e.message) }
  }
  async function del(mcName: string) {
    const confirm1 = prompt(`Pra deletar PERMANENTEMENTE ${mcName}, digite o nick:`)
    if (confirm1 !== mcName) return toast.err('cancelado')
    try {
      await testerApi.adminDeleteTester(mcName)
      qc.invalidateQueries({ queryKey: ['admin-testers-all'] })
      qc.invalidateQueries({ queryKey: ['tester-ranking'] })
      toast.ok(`💀 ${mcName} deletado`)
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="space-y-3">
      <div className="card-glow">
        <div className="flex items-center justify-between flex-wrap gap-2 mb-2">
          <h3 className="font-bold">👥 Gerenciar Testers ({testers.length})</h3>
          <div className="flex items-center gap-2 text-[11px]">
            <input className="input text-xs" placeholder="🔍 buscar nick"
              value={search} onChange={(e) => setSearch(e.target.value)} style={{ width: 180 }} />
            <label><input type="checkbox" checked={showBanned}
              onChange={(e) => setShowBanned(e.target.checked)} /> mostrar banidos</label>
          </div>
        </div>

        <div className="text-[10px] text-liberthia-300/60 mb-2">
          <b>Ban</b>: bloqueia login mas mantém histórico (bugs, sugestões, rewards). Reversível.<br />
          <b>Deletar</b>: apaga a conta. Bugs/sugestões ficam órfãos com o nome antigo. Irreversível.
        </div>

        <div className="space-y-1 max-h-[600px] overflow-y-auto">
          {testers.length === 0 && (
            <div className="text-center py-6 text-xs text-liberthia-300/50 italic">nenhum tester</div>
          )}
          {testers.map((t: any) => {
            const banned = !t.enabled
            const banReason = banned && t.inviteCodeUsed?.startsWith('BANNED:')
              ? t.inviteCodeUsed.slice(7).trim() : null
            return (
              <div key={t.mcName}
                className={`p-2 rounded flex items-center gap-2 text-xs ${
                  banned ? 'bg-red-900/20 border border-red-500/30' : 'bg-liberthia-900/40'
                }`}>
                <img src={`https://mc-heads.net/avatar/${t.mcName}/32`}
                  className="w-8 h-8 rounded shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <b>{t.mcName}</b>
                    {banned && <span className="badge badge-red text-[9px]">🔨 BANIDO</span>}
                    <span className="text-purple-300 font-bold">⭐ {t.points}</span>
                  </div>
                  <div className="text-[9px] text-liberthia-300/50">
                    criado {t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '—'} ·
                    último login {t.lastLoginAt ? new Date(t.lastLoginAt).toLocaleString() : 'nunca'}
                  </div>
                  {banReason && (
                    <div className="text-[10px] text-red-200 italic mt-0.5">motivo: {banReason}</div>
                  )}
                </div>
                {banned ? (
                  <button className="btn-ghost btn-sm text-[10px] text-emerald-300"
                    onClick={() => unban(t.mcName)}>✅ Desbanir</button>
                ) : (
                  <button className="btn-ghost btn-sm text-[10px] text-amber-300"
                    onClick={() => ban(t.mcName)}>🔨 Banir</button>
                )}
                <button className="btn-ghost btn-sm text-[10px] text-red-300"
                  onClick={() => del(t.mcName)}>💀 Deletar</button>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

// ============ Packages ============
function PackagesSection() {
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [version, setVersion] = useState('')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)

  const q = useQuery({ queryKey: ['admin-tester-packages'], queryFn: testerApi.adminListPackages, refetchInterval: 15000 })

  async function upload(e: React.FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) return toast.err('selecione um ZIP')
    if (!name.trim()) return toast.err('nome obrigatório')
    setBusy(true)
    try {
      await testerApi.adminUploadPackage(file, name.trim(), version.trim() || undefined, description.trim() || undefined)
      setName(''); setVersion(''); setDescription('')
      if (fileRef.current) fileRef.current.value = ''
      qc.invalidateQueries({ queryKey: ['admin-tester-packages'] })
      toast.ok('📦 enviado')
    } catch (e: any) { toast.err(e.message) }
    finally { setBusy(false) }
  }

  async function del(id: number) {
    if (!confirm('Deletar pacote?')) return
    try {
      await testerApi.adminDeletePackage(id)
      qc.invalidateQueries({ queryKey: ['admin-tester-packages'] })
      toast.ok('🗑')
    } catch (e: any) { toast.err(e.message) }
  }

  async function toggle(id: number) {
    try {
      await testerApi.adminTogglePackage(id)
      qc.invalidateQueries({ queryKey: ['admin-tester-packages'] })
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <form onSubmit={upload} className="card-glow">
        <h3 className="font-bold mb-2">⬆ Upload</h3>
        <input className="input text-xs mb-2" placeholder="Nome (ex: Liberthia Beta)"
          value={name} onChange={(e) => setName(e.target.value)} required />
        <input className="input text-xs mb-2" placeholder="Versão (ex: 0.1.8)"
          value={version} onChange={(e) => setVersion(e.target.value)} />
        <textarea className="input text-xs mb-2" rows={2} placeholder="Descrição"
          value={description} onChange={(e) => setDescription(e.target.value)} />
        <input type="file" accept=".zip" ref={fileRef} className="text-xs mb-2 w-full" required />
        <button type="submit" className="btn w-full" disabled={busy}>{busy ? '...' : '📦 Upload'}</button>
      </form>
      <div className="lg:col-span-2 space-y-2">
        {q.data?.packages?.length === 0 && (
          <div className="card text-center py-6 text-xs text-liberthia-300/50">nenhum pacote</div>
        )}
        {q.data?.packages?.map((p: any) => (
          <div key={p.id} className={`card-glow ${!p.enabled ? 'opacity-50' : ''}`}>
            <div className="flex items-center gap-2">
              <div className="text-2xl">📦</div>
              <div className="flex-1 min-w-0">
                <h4 className="font-bold">{p.name} {p.version && <span className="badge badge-purple text-[10px]">v{p.version}</span>}</h4>
                <div className="text-[10px] text-liberthia-300/60 font-mono">
                  {p.filename} · {(p.sizeBytes / 1024 / 1024).toFixed(1)}MB · ⬇{p.downloadCount}
                </div>
              </div>
              <button className="btn-ghost btn-sm text-[10px]" onClick={() => toggle(p.id)}>
                {p.enabled ? '⏸' : '▶'}
              </button>
              <button className="btn-ghost btn-sm text-[10px] text-red-400" onClick={() => del(p.id)}>🗑</button>
            </div>
            {p.description && <p className="text-xs text-liberthia-300/70 mt-1">{p.description}</p>}
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ Beta Items ============
function BetaItemsSection() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<any | null>(null)
  const [kindFilter, setKindFilter] = useState<string>('ALL')
  const [form, setForm] = useState({
    name: '', itemId: '', kind: 'ITEM' as 'ITEM' | 'BLOCK' | 'FEATURE' | 'ARTIFACT',
    description: '', lore: '', propertiesJson: '', recipeJson: '', effectsJson: '',
    giveCommand: '', imageUrl: '', category: '', enabled: true,
  })
  const q = useQuery({ queryKey: ['admin-tester-beta-items'], queryFn: testerApi.adminListBetaItems, refetchInterval: 15000 })

  function startEdit(item: any | null) {
    if (item) {
      setEditing(item)
      setForm({
        name: item.name ?? '', itemId: item.itemId ?? '',
        kind: item.kind ?? 'ITEM',
        description: item.description ?? '', lore: item.lore ?? '',
        propertiesJson: item.propertiesJson ?? '',
        recipeJson: item.recipeJson ?? '',
        effectsJson: item.effectsJson ?? '',
        giveCommand: item.giveCommand ?? '', imageUrl: item.imageUrl ?? '',
        category: item.category ?? '', enabled: item.enabled,
      })
    } else {
      setEditing({ new: true })
      setForm({ name: '', itemId: '', kind: 'ITEM', description: '', lore: '',
                propertiesJson: '', recipeJson: '', effectsJson: '',
                giveCommand: '', imageUrl: '', category: '', enabled: true })
    }
  }

  async function save(e: React.FormEvent) {
    e.preventDefault()
    try {
      if (editing?.new) await testerApi.adminCreateBetaItem(form)
      else await testerApi.adminUpdateBetaItem(editing.id, form)
      setEditing(null)
      qc.invalidateQueries({ queryKey: ['admin-tester-beta-items'] })
      toast.ok('💾')
    } catch (e: any) { toast.err(e.message) }
  }

  async function del(id: number) {
    if (!confirm('Deletar item?')) return
    await testerApi.adminDeleteBetaItem(id)
    qc.invalidateQueries({ queryKey: ['admin-tester-beta-items'] })
  }

  const KIND_ICONS: Record<string, string> = {
    ITEM: '🗡', BLOCK: '🧱', FEATURE: '⚙', ARTIFACT: '🏺',
  }

  const allItems = q.data?.items ?? []
  const items = kindFilter === 'ALL' ? allItems : allItems.filter((i: any) => (i.kind ?? 'ITEM') === kindFilter)

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-2 flex-wrap">
          <button className="btn" onClick={() => startEdit(null)}>+ Novo</button>
          <BulkImportButton
            importer={testerApi.adminBulkImportBetaItems}
            onSuccess={() => qc.invalidateQueries({ queryKey: ['admin-tester-beta-items'] })}
            hint="beta-items-update.json"
          />
          {/* Recovery button: liga TODOS items com enabled=false. Usar quando
              tester reclamar "tá vazio mas a gente cadastrou". */}
          <button
            className="btn-ghost btn-sm text-[10px]"
            title="Liga todos os items que estão como 'enabled=false' — usado quando tester reclama que não vê items"
            onClick={async () => {
              if (!confirm('Ligar TODOS os items desabilitados? Eles vão ficar visíveis pros testers.')) return
              try {
                const r = await testerApi.adminEnableAllBetaItems()
                toast.ok(`✅ ${r.updated} items ligados`)
                qc.invalidateQueries({ queryKey: ['admin-tester-beta-items'] })
              } catch (e: any) {
                toast.err(`Falha: ${e.message}`)
              }
            }}>
            🟢 Ligar todos desabilitados
          </button>
        </div>
        <div className="flex gap-1 flex-wrap">
          <button className={`btn-ghost btn-sm text-[10px] ${kindFilter === 'ALL' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
            onClick={() => setKindFilter('ALL')}>Todos ({allItems.length})</button>
          {(['ITEM', 'BLOCK', 'FEATURE', 'ARTIFACT'] as const).map(k => {
            const count = allItems.filter((i: any) => (i.kind ?? 'ITEM') === k).length
            return (
              <button key={k}
                className={`btn-ghost btn-sm text-[10px] ${kindFilter === k ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
                onClick={() => setKindFilter(k)}>
                {KIND_ICONS[k]} {k.toLowerCase()} ({count})
              </button>
            )
          })}
        </div>
      </div>

      {editing && (
        <form onSubmit={save} className="card-glow space-y-3">
          <h3 className="font-bold">{editing.new ? '➕ Novo item/bloco/feature/artefato' : `✏ Editar #${editing.id}`}</h3>

          {/* Kind selector — destaque */}
          <div>
            <label className="label text-[10px]">Tipo do conteúdo</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(['ITEM', 'BLOCK', 'FEATURE', 'ARTIFACT'] as const).map(k => (
                <button key={k} type="button"
                  onClick={() => setForm({ ...form, kind: k })}
                  className={`p-3 rounded text-xs border-2 transition ${
                    form.kind === k
                      ? 'border-purple-400 bg-purple-500/20 ring-2 ring-purple-400/30'
                      : 'border-liberthia-700 bg-liberthia-900/40 hover:bg-purple-500/10'
                  }`}>
                  <div className="text-2xl mb-1">{KIND_ICONS[k]}</div>
                  <div className="font-bold">{k}</div>
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <input className="input text-xs" placeholder="Nome (ex: Lâmina do Vazio)"
              value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            <input className="input text-xs" placeholder="Categoria (ex: weapon, decoration)"
              value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
          </div>

          <div>
            <label className="label text-[10px]">ID Minecraft (autocomplete)</label>
            <ItemAutocomplete
              value={form.itemId}
              onChange={(id) => setForm({ ...form, itemId: id })}
              placeholder="liberthia:dark_matter_sword"
              liberthiaOnly={false}
            />
          </div>

          <div>
            <label className="label text-[10px]">Descrição (curta)</label>
            <textarea className="input text-xs" rows={2}
              value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Resumo do que é e o que faz" />
          </div>

          <div>
            <label className="label text-[10px]">Lore / narrativa (markdown)</label>
            <textarea className="input text-xs" rows={3}
              value={form.lore} onChange={(e) => setForm({ ...form, lore: e.target.value })}
              placeholder="Texto lore opcional — aparece no manual in-game" />
          </div>

          {/* Recipe Builder visual — só pra items/blocks (artefatos/features geralmente não têm receita) */}
          {(form.kind === 'ITEM' || form.kind === 'BLOCK') && (
            <div>
              <label className="label text-[10px]">Receita de crafting (opcional)</label>
              <RecipeBuilder
                value={form.recipeJson}
                onChange={(json) => setForm({ ...form, recipeJson: json })}
              />
            </div>
          )}

          <details>
            <summary className="cursor-pointer text-xs text-liberthia-300/70 font-bold">
              ⚙ Propriedades & efeitos (JSON avançado)
            </summary>
            <div className="space-y-2 mt-2">
              <div>
                <label className="label text-[10px]">Propriedades JSON</label>
                <textarea className="input text-xs font-mono" rows={2}
                  placeholder='{"damage": 8, "durability": 1500, "speed": 1.6}'
                  value={form.propertiesJson}
                  onChange={(e) => setForm({ ...form, propertiesJson: e.target.value })} />
              </div>
              <div>
                <label className="label text-[10px]">Efeitos JSON</label>
                <textarea className="input text-xs font-mono" rows={2}
                  placeholder='{"effects":[{"id":"poison","duration":200,"amplifier":1}]}'
                  value={form.effectsJson}
                  onChange={(e) => setForm({ ...form, effectsJson: e.target.value })} />
              </div>
            </div>
          </details>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <input className="input text-xs font-mono" placeholder="Give command (ex: /give @s liberthia:xxx 1)"
              value={form.giveCommand} onChange={(e) => setForm({ ...form, giveCommand: e.target.value })} />
            <input className="input text-xs" placeholder="URL da imagem (opcional)"
              value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
          </div>

          <label className="text-xs flex items-center gap-1">
            <input type="checkbox" checked={form.enabled}
              onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
            visível pros testers
          </label>

          <div className="flex gap-2 sticky bottom-0 bg-liberthia-900/80 pt-2">
            <button type="submit" className="btn flex-1">💾 Salvar</button>
            <button type="button" className="btn-ghost" onClick={() => setEditing(null)}>Cancelar</button>
          </div>
        </form>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
        {items.length === 0 && (
          <div className="card col-span-full text-center py-8 text-xs text-liberthia-300/50">
            {kindFilter === 'ALL' ? 'Nenhum cadastrado' : `Nenhum ${kindFilter.toLowerCase()} cadastrado`}
          </div>
        )}
        {items.map((i: any) => (
          <div key={i.id} className={`card flex items-start gap-2 ${!i.enabled ? 'opacity-50' : ''}`}>
            {i.imageUrl ? (
              <img src={i.imageUrl} alt={i.name} className="w-12 h-12 rounded object-cover" />
            ) : (
              <div className="w-12 h-12 rounded bg-purple-500/20 flex items-center justify-center text-2xl">
                {KIND_ICONS[i.kind ?? 'ITEM']}
              </div>
            )}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-1 flex-wrap">
                <h4 className="font-bold text-sm">{i.name}</h4>
                <span className="badge badge-purple text-[9px]">{i.kind ?? 'ITEM'}</span>
                {i.category && <span className="badge text-[9px]">{i.category}</span>}
                {i.recipeJson && <span className="badge text-[9px]" title="tem receita">🔨</span>}
              </div>
              {i.itemId && <div className="text-[10px] font-mono text-liberthia-300/60">{i.itemId}</div>}
              {i.description && <p className="text-xs text-liberthia-300/80 mt-1 line-clamp-2">{i.description}</p>}
            </div>
            <div className="flex flex-col gap-1">
              <button className="btn-ghost btn-sm text-[10px]" onClick={() => startEdit(i)}>✏</button>
              <button className="btn-ghost btn-sm text-[10px] text-red-400" onClick={() => del(i.id)}>🗑</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ Bugs ============
function BugsSection() {
  const qc = useQueryClient()
  // Filtro por status. Antes era `onlyPending` (boolean) com default true → bug
  // aceito sumia da tela e admin perdia histórico do que já triou. Agora 'all'
  // é default e os outros 3 status são tabs (não checkbox).
  const [statusFilter, setStatusFilter] = useState<'all' | 'PENDING' | 'CONFIRMED' | 'REJECTED'>('all')
  // Modal de detalhe — click no card abre.
  const [selected, setSelected] = useState<any | null>(null)
  // Pra o painel de confirmar/rejeitar — não usa mais prompt() do browser
  // (visual ruim, perde input se trocar de aba).
  const [confirmModal, setConfirmModal] = useState<{ id: number; type: 'confirm' | 'reject' } | null>(null)
  const [confirmPts, setConfirmPts] = useState(10)
  const [confirmNote, setConfirmNote] = useState('')

  const q = useQuery({
    queryKey: ['admin-tester-bugs', 'all'],
    // Sempre puxa TUDO — filtragem é client-side. Backend tem só 2 opções
    // (onlyPending=true|false) então `false` retorna tudo.
    queryFn: () => testerApi.adminListBugs(false),
    refetchInterval: 10000,
  })

  function openConfirm(id: number) {
    setConfirmModal({ id, type: 'confirm' })
    setConfirmPts(10)
    setConfirmNote('')
  }

  function openReject(id: number) {
    setConfirmModal({ id, type: 'reject' })
    setConfirmNote('')
  }

  async function executeConfirm() {
    if (!confirmModal) return
    try {
      if (confirmModal.type === 'confirm') {
        await testerApi.adminConfirmBug(confirmModal.id, confirmPts, confirmNote)
        toast.ok(`✓ confirmado · +${confirmPts} pts`)
      } else {
        await testerApi.adminRejectBug(confirmModal.id, confirmNote)
        toast.ok('✕ rejeitado')
      }
      qc.invalidateQueries({ queryKey: ['admin-tester-bugs'] })
      qc.invalidateQueries({ queryKey: ['tester-ranking'] })
      setConfirmModal(null)
      setSelected(null)
    } catch (e: any) { toast.err(e.message) }
  }

  const allBugs = q.data?.bugs ?? []
  const filtered = statusFilter === 'all' ? allBugs : allBugs.filter((b: any) => b.status === statusFilter)
  const counts: Record<string, number> = {
    all: allBugs.length,
    PENDING: allBugs.filter((b: any) => b.status === 'PENDING').length,
    CONFIRMED: allBugs.filter((b: any) => b.status === 'CONFIRMED').length,
    REJECTED: allBugs.filter((b: any) => b.status === 'REJECTED').length,
  }

  const statusColor: Record<string, string> = {
    PENDING: 'badge-yellow', CONFIRMED: 'badge-green', REJECTED: 'badge-red',
  }

  // Botões de ação injetados no modal — só pra PENDING.
  const actionsForSelected = selected && selected.status === 'PENDING' ? (
    <>
      <button className="btn btn-sm bg-emerald-600/80 hover:bg-emerald-600"
        onClick={() => openConfirm(selected.id)}>✓ Confirmar + dar pontos</button>
      <button className="btn-danger btn-sm" onClick={() => openReject(selected.id)}>✕ Rejeitar</button>
    </>
  ) : null

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-1 items-center">
        <span className="text-[10px] text-liberthia-300/60 mr-1">Status:</span>
        {(['all', 'PENDING', 'CONFIRMED', 'REJECTED'] as const).map(s => (
          <button key={s} onClick={() => setStatusFilter(s)}
            className={`btn-ghost btn-sm text-[10px] ${statusFilter === s ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
            {s === 'all' ? '📋 Todos' : s === 'PENDING' ? '⏳ Pendentes' : s === 'CONFIRMED' ? '✅ Confirmados' : '❌ Rejeitados'}
            {' '}({counts[s]})
          </button>
        ))}
      </div>

      <div className="space-y-2">
        {filtered.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">
            {allBugs.length === 0 ? 'nenhum bug ainda' : `nenhum bug ${statusFilter === 'all' ? '' : `com status "${statusFilter}"`}`}
          </div>
        )}
        {filtered.map((b: any) => (
          <button key={b.id} onClick={() => setSelected(b)}
            className="w-full text-left card-glow hover:!border-purple-400/60 transition-colors block">
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-2 flex-wrap">
                  <h4 className="font-bold truncate">{b.title}</h4>
                  {b.severity && <span className="badge text-[9px]">{b.severity}</span>}
                  {b.priority && <span className="badge text-[9px]">prio: {b.priority}</span>}
                  {b.replicationCount > 0 && (
                    <span className="badge badge-purple text-[9px]">+{b.replicationCount} replicou</span>
                  )}
                </div>
                <div className="text-[10px] text-liberthia-300/60 mt-1">
                  por <b>{b.testerMcName}</b> · #{b.id} · {new Date(b.createdAt).toLocaleString('pt-BR')}
                  {b.triagedAt && b.status !== 'PENDING' && (
                    <> · triado em {new Date(b.triagedAt).toLocaleString('pt-BR')}</>
                  )}
                </div>
              </div>
              <div className="text-right shrink-0">
                <span className={`badge ${statusColor[b.status]} text-[10px]`}>{b.status}</span>
                {b.pointsAwarded > 0 && (
                  <div className="text-purple-300 text-xs font-bold mt-1">⭐ +{b.pointsAwarded}</div>
                )}
              </div>
            </div>
            <p className="text-xs text-liberthia-300/80 line-clamp-2">{b.description}</p>
            <div className="text-[9px] text-purple-300/70 mt-2">ver detalhes →</div>
          </button>
        ))}
      </div>

      {/* Modal de detalhe — usa o BugDetailModal compartilhado com o tester */}
      <BugDetailModal bug={selected} onClose={() => setSelected(null)} actions={actionsForSelected} />

      {/* Sub-modal de confirmar/rejeitar (em cima do modal de detalhe) */}
      {confirmModal && (
        <div className="fixed inset-0 z-[10000] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
             onClick={() => setConfirmModal(null)}>
          <div className="card-glow max-w-md w-full" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-bold mb-3 gradient-text">
              {confirmModal.type === 'confirm' ? '✓ Confirmar bug' : '✕ Rejeitar bug'}
            </h3>
            {confirmModal.type === 'confirm' && (
              <div className="mb-3">
                <label className="label text-[10px]">Pontos a dar pro tester</label>
                <input type="number" className="input text-sm" min={0} max={1000}
                  value={confirmPts} onChange={(e) => setConfirmPts(Number(e.target.value) || 0)} />
              </div>
            )}
            <div className="mb-3">
              <label className="label text-[10px]">Nota {confirmModal.type === 'reject' ? '(motivo)' : '(opcional)'}</label>
              <textarea className="input text-xs" rows={3}
                value={confirmNote} onChange={(e) => setConfirmNote(e.target.value)}
                placeholder={confirmModal.type === 'confirm'
                  ? 'Ex: fix em v107'
                  : 'Ex: não é bug, é design intencional'} />
            </div>
            <div className="flex gap-2">
              <button className={`btn flex-1 ${confirmModal.type === 'confirm' ? 'bg-emerald-600/80 hover:bg-emerald-600' : ''}`}
                onClick={executeConfirm}>
                {confirmModal.type === 'confirm' ? '✓ Confirmar' : '✕ Rejeitar'}
              </button>
              <button className="btn-ghost" onClick={() => setConfirmModal(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ============ Rewards ============
function RewardsSection() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<any | null>(null)
  const [form, setForm] = useState({
    name: '', description: '', costPoints: 10, giveCommand: '',
    imageUrl: '', category: 'item', enabled: true, perTesterLimit: 0,
    // campos auxiliares pro autocomplete (não vão pro backend, só pra montar o give command)
    rewardItemId: '', rewardItemCount: 1,
  })
  const rewardsQ = useQuery({ queryKey: ['admin-tester-rewards'], queryFn: testerApi.adminListRewards })
  const redemptQ = useQuery({ queryKey: ['admin-tester-redemptions'], queryFn: testerApi.adminListRedemptions, refetchInterval: 10000 })

  function startEdit(r: any | null) {
    if (r) {
      setEditing(r)
      // Tenta extrair itemId/count do giveCommand existente (formato /give {player} <id> <count>)
      let rewardItemId = ''
      let rewardItemCount = 1
      const cmd = r.giveCommand ?? ''
      const m = cmd.match(/\/give\s+\S+\s+(\S+)(?:\s+(\d+))?/)
      if (m) { rewardItemId = m[1]; rewardItemCount = Number(m[2]) || 1 }
      setForm({
        name: r.name ?? '', description: r.description ?? '',
        costPoints: r.costPoints, giveCommand: cmd,
        imageUrl: r.imageUrl ?? '', category: r.category ?? 'item',
        enabled: r.enabled, perTesterLimit: r.perTesterLimit ?? 0,
        rewardItemId, rewardItemCount,
      })
    } else {
      setEditing({ new: true })
      setForm({ name: '', description: '', costPoints: 10, giveCommand: '',
                imageUrl: '', category: 'item', enabled: true, perTesterLimit: 0,
                rewardItemId: '', rewardItemCount: 1 })
    }
  }

  async function save(e: React.FormEvent) {
    e.preventDefault()
    try {
      if (editing?.new) await testerApi.adminCreateReward(form)
      else await testerApi.adminUpdateReward(editing.id, form)
      setEditing(null)
      qc.invalidateQueries({ queryKey: ['admin-tester-rewards'] })
      toast.ok('💾')
    } catch (e: any) { toast.err(e.message) }
  }

  async function del(id: number) {
    if (!confirm('Deletar recompensa?')) return
    await testerApi.adminDeleteReward(id)
    qc.invalidateQueries({ queryKey: ['admin-tester-rewards'] })
  }

  async function deliver(id: number) {
    const note = prompt('Nota (opcional):') ?? ''
    await testerApi.adminDeliverRedemption(id, note)
    qc.invalidateQueries({ queryKey: ['admin-tester-redemptions'] })
    toast.ok('✅ entregue')
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
      <div>
        <div className="flex items-center gap-2 mb-2 flex-wrap">
          <button className="btn" onClick={() => startEdit(null)}>+ Nova recompensa</button>
          <BulkImportButton
            importer={testerApi.adminBulkImportRewards}
            onSuccess={() => qc.invalidateQueries({ queryKey: ['admin-tester-rewards'] })}
            hint="rewards-update.json"
          />
        </div>

        {editing && (
          <form onSubmit={save} className="card-glow space-y-3 mb-3">
            <h3 className="font-bold">{editing.new ? '🎁 Nova Recompensa' : `✏ Editar #${editing.id}`}</h3>

            {/* Quick templates */}
            {editing.new && (
              <div>
                <label className="label text-[10px]">⚡ Quick templates</label>
                <div className="flex flex-wrap gap-1">
                  <button type="button" className="btn-ghost btn-sm text-[10px]"
                    onClick={() => setForm({ ...form, name: 'Diamante', costPoints: 50, category: 'item',
                      giveCommand: '/give {player} minecraft:diamond 1' })}>
                    💎 Diamante (50pts)
                  </button>
                  <button type="button" className="btn-ghost btn-sm text-[10px]"
                    onClick={() => setForm({ ...form, name: 'Crown', costPoints: 25, category: 'currency',
                      giveCommand: '/give {player} numismatics:crown 5' })}>
                    👑 5 Crowns (25pts)
                  </button>
                  <button type="button" className="btn-ghost btn-sm text-[10px]"
                    onClick={() => setForm({ ...form, name: 'Netherite Ingot', costPoints: 200, category: 'item',
                      giveCommand: '/give {player} minecraft:netherite_ingot 1' })}>
                    ⬛ Netherite (200pts)
                  </button>
                  <button type="button" className="btn-ghost btn-sm text-[10px]"
                    onClick={() => setForm({ ...form, name: 'Espada Encantada', costPoints: 150, category: 'weapon',
                      giveCommand: '/give {player} minecraft:diamond_sword{Enchantments:[{id:"minecraft:sharpness",lvl:5}]} 1' })}>
                    ⚔ Espada Enc V (150pts)
                  </button>
                </div>
              </div>
            )}

            <div>
              <label className="label text-[10px]">Nome</label>
              <input className="input text-xs" placeholder="ex: Espada Lendária"
                value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            </div>
            <div>
              <label className="label text-[10px]">Descrição (markdown OK)</label>
              <textarea className="input text-xs" rows={2} placeholder="O que o tester ganha + porque é incrível"
                value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>

            {/* Autocomplete + builder rápido do give */}
            <div>
              <label className="label text-[10px]">🎯 Item de recompensa (autocomplete)</label>
              <ItemAutocomplete
                value={form.rewardItemId ?? ''}
                onChange={(id) => {
                  const count = form.rewardItemCount || 1
                  setForm({
                    ...form,
                    rewardItemId: id,
                    giveCommand: id ? `/give {player} ${id} ${count}` : form.giveCommand
                  })
                }}
                placeholder="ex: minecraft:diamond ou liberthia:dark_matter_sword"
              />
              <div className="flex items-center gap-2 mt-1">
                <span className="text-[10px] text-liberthia-300/60">×</span>
                <input type="number" min={1} max={64} value={form.rewardItemCount || 1}
                  onChange={(e) => {
                    const count = Math.max(1, Math.min(64, Number(e.target.value) || 1))
                    setForm({
                      ...form,
                      rewardItemCount: count,
                      giveCommand: form.rewardItemId
                        ? `/give {player} ${form.rewardItemId} ${count}` : form.giveCommand,
                    })
                  }}
                  className="input text-xs w-16" />
                <span className="text-[10px] text-liberthia-300/40">→ atualiza o comando abaixo</span>
              </div>
            </div>

            <div>
              <label className="label text-[10px]">Comando que será executado in-game</label>
              <input className="input text-xs font-mono"
                placeholder="ex: /give {player} minecraft:diamond 1 — {player} vira o nome do tester"
                value={form.giveCommand} onChange={(e) => setForm({ ...form, giveCommand: e.target.value })} />
              <div className="text-[9px] text-liberthia-300/40 mt-0.5">
                Variável {'{player}'} é substituída pelo nick do tester
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <div>
                <label className="label text-[10px]">⭐ Custo</label>
                <input className="input text-xs" type="number" min={1}
                  value={form.costPoints} onChange={(e) => setForm({ ...form, costPoints: Number(e.target.value) })} required />
              </div>
              <div>
                <label className="label text-[10px]">Limite/tester</label>
                <input className="input text-xs" type="number" min={0} placeholder="0 = ∞"
                  value={form.perTesterLimit} onChange={(e) => setForm({ ...form, perTesterLimit: Number(e.target.value) })} />
              </div>
              <div>
                <label className="label text-[10px]">Categoria</label>
                <select className="input text-xs" value={form.category}
                  onChange={(e) => setForm({ ...form, category: e.target.value })}>
                  <option value="item">🗡 item</option>
                  <option value="block">🧱 block</option>
                  <option value="currency">👑 currency</option>
                  <option value="weapon">⚔ weapon</option>
                  <option value="armor">🛡 armor</option>
                  <option value="title">🏷 title</option>
                  <option value="cosmetic">✨ cosmetic</option>
                  <option value="other">❓ outro</option>
                </select>
              </div>
            </div>

            <div>
              <label className="label text-[10px]">URL imagem (opcional)</label>
              <input className="input text-xs" placeholder="https://..."
                value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
            </div>

            <label className="text-xs flex items-center gap-1">
              <input type="checkbox" checked={form.enabled}
                onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
              visível pros testers (publicado)
            </label>

            {/* Preview do que o tester vai ver */}
            <div className="rounded p-2 bg-liberthia-900/60 border border-purple-500/30">
              <div className="text-[9px] text-liberthia-300/50 mb-1">👁 Preview do card pro tester:</div>
              <div className="flex items-center gap-2">
                {form.imageUrl ? (
                  <img src={form.imageUrl} className="w-12 h-12 rounded object-cover" alt="" />
                ) : form.rewardItemId ? (
                  <ItemIcon id={form.rewardItemId} size={48} />
                ) : (
                  <div className="w-12 h-12 rounded bg-purple-500/20 flex items-center justify-center text-2xl">🎁</div>
                )}
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-sm">{form.name || '...'}</div>
                  <div className="text-[10px] text-liberthia-300/70">{form.description || ''}</div>
                  <div className="text-purple-300 font-bold text-xs mt-1">⭐ {form.costPoints}</div>
                </div>
              </div>
            </div>

            <div className="flex gap-2">
              <button type="submit" className="btn flex-1">💾 Salvar</button>
              <button type="button" className="btn-ghost" onClick={() => setEditing(null)}>Cancelar</button>
            </div>
          </form>
        )}

        <div className="space-y-2">
          {rewardsQ.data?.rewards?.map((r: any) => (
            <div key={r.id} className={`card ${!r.enabled ? 'opacity-50' : ''}`}>
              <div className="flex items-start gap-2">
                <div className="flex-1 min-w-0">
                  <div className="flex items-baseline gap-2">
                    <h4 className="font-bold">{r.name}</h4>
                    <span className="text-purple-300 font-bold">⭐{r.costPoints}</span>
                  </div>
                  {r.description && <p className="text-xs text-liberthia-300/80 mt-1">{r.description}</p>}
                  {r.giveCommand && <div className="text-[9px] font-mono text-liberthia-300/50 mt-1 truncate">{r.giveCommand}</div>}
                </div>
                <button className="btn-ghost btn-sm text-[10px]" onClick={() => startEdit(r)}>✏</button>
                <button className="btn-ghost btn-sm text-[10px] text-red-400" onClick={() => del(r.id)}>🗑</button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="font-bold gradient-text mb-2 text-sm">📜 Resgates ({redemptQ.data?.redemptions?.length ?? 0})</h3>
        <div className="space-y-2 max-h-[600px] overflow-y-auto">
          {redemptQ.data?.redemptions?.length === 0 && (
            <div className="card text-center py-6 text-xs text-liberthia-300/50">nenhum resgate</div>
          )}
          {redemptQ.data?.redemptions?.map((rr: any) => (
            <div key={rr.id} className="card-glow text-xs">
              <div className="flex items-start gap-2">
                <div className="text-2xl">{rr.delivered ? '✅' : '⏳'}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold">{rr.rewardSnapshot}</div>
                  <div className="text-[10px] text-liberthia-300/60">
                    por <b>{rr.testerMcName}</b> · {new Date(rr.redeemedAt).toLocaleString()}
                  </div>
                  {rr.commandRun && (
                    <div className="text-[9px] font-mono mt-1 p-1 bg-liberthia-900/60 rounded">
                      {rr.commandRun}
                    </div>
                  )}
                </div>
                {!rr.delivered && (
                  <button className="btn btn-sm text-[10px]" onClick={() => deliver(rr.id)}>✓ entregue</button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ============ Applications (processo seletivo) ============
function ApplicationsSection() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<'all' | 'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING')
  const q = useQuery({
    queryKey: ['admin-applications', filter],
    queryFn: () => testerApi.adminListApplications(filter === 'all' ? undefined : filter),
    refetchInterval: 10000,
  })

  async function approve(id: number, mcName: string) {
    const note = prompt(`Aprovar inscrição de ${mcName}? Nota (opcional):`) ?? ''
    try {
      const r = await testerApi.adminApproveApplication(id, note)
      qc.invalidateQueries({ queryKey: ['admin-applications'] })
      qc.invalidateQueries({ queryKey: ['tester-invites'] })
      try { await navigator.clipboard.writeText(r.generatedInviteCode) } catch {}
      toast.ok(`✓ aprovado · código ${r.generatedInviteCode} copiado`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function reject(id: number, mcName: string) {
    const note = prompt(`Motivo da rejeição de ${mcName}:`) ?? ''
    if (!note.trim()) return
    try {
      await testerApi.adminRejectApplication(id, note)
      qc.invalidateQueries({ queryKey: ['admin-applications'] })
      toast.ok('✕ rejeitado')
    } catch (e: any) { toast.err(e.message) }
  }

  const statusColor: Record<string, string> = {
    PENDING: 'badge-yellow', APPROVED: 'badge-green', REJECTED: 'badge-red',
  }

  return (
    <div className="space-y-3">
      <div className="flex gap-1 flex-wrap">
        <button className={`btn-ghost btn-sm text-[10px] ${filter === 'all' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
          onClick={() => setFilter('all')}>Todas</button>
        <button className={`btn-ghost btn-sm text-[10px] ${filter === 'PENDING' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
          onClick={() => setFilter('PENDING')}>⏳ Pendentes</button>
        <button className={`btn-ghost btn-sm text-[10px] ${filter === 'APPROVED' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
          onClick={() => setFilter('APPROVED')}>✓ Aprovadas</button>
        <button className={`btn-ghost btn-sm text-[10px] ${filter === 'REJECTED' ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}
          onClick={() => setFilter('REJECTED')}>✕ Rejeitadas</button>
      </div>

      <div className="space-y-2">
        {q.data?.applications?.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">nenhuma inscrição</div>
        )}
        {q.data?.applications?.map((a: any) => (
          <div key={a.id} className="card-glow">
            <div className="flex items-start gap-3">
              <img src={`https://mc-heads.net/avatar/${a.mcName}/48`}
                className="w-12 h-12 rounded shadow shrink-0" alt={a.mcName} />
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-2 flex-wrap">
                  <h4 className="font-bold">{a.realName}</h4>
                  <span className="text-purple-300 text-xs">@{a.mcName}</span>
                  <span className={`badge ${statusColor[a.status]} text-[9px]`}>{a.status}</span>
                  {a.weeklyAvailability ? (
                    <span className="badge badge-green text-[9px]">✅ disp semanal</span>
                  ) : (
                    <span className="badge badge-red text-[9px]">❌ sem disp</span>
                  )}
                </div>
                <div className="text-[10px] text-liberthia-300/60 mt-1">
                  inscrito {new Date(a.createdAt).toLocaleString()}
                  {a.contact && <span> · 📞 {a.contact}</span>}
                </div>
                <div className="mt-2 p-2 bg-liberthia-900/40 rounded text-xs whitespace-pre-wrap">
                  <b>Motivação:</b><br />{a.motivation}
                </div>
                {a.adminNote && (
                  <div className="mt-2 text-[11px] text-amber-200/80 italic">Admin: {a.adminNote}</div>
                )}
                {a.generatedInviteCode && (
                  <div className="mt-2 p-2 rounded bg-emerald-900/30 border border-emerald-400/40 flex items-center gap-2">
                    <span className="text-[10px] text-emerald-300/70">Código gerado:</span>
                    <button onClick={() => { navigator.clipboard.writeText(a.generatedInviteCode); toast.ok('📋') }}
                      className="font-mono font-bold tracking-widest text-emerald-300 text-base">
                      {a.generatedInviteCode}
                    </button>
                  </div>
                )}
                {a.reviewedAt && (
                  <div className="text-[9px] text-liberthia-300/50 mt-1">
                    avaliado {new Date(a.reviewedAt).toLocaleString()} por {a.reviewedBy}
                  </div>
                )}
              </div>
              {a.status === 'PENDING' && (
                <div className="flex flex-col gap-1">
                  <button className="btn btn-sm bg-emerald-600/80 hover:bg-emerald-600 text-[10px]"
                    onClick={() => approve(a.id, a.mcName)}>✓ Aprovar</button>
                  <button className="btn-danger btn-sm text-[10px]"
                    onClick={() => reject(a.id, a.mcName)}>✕ Rejeitar</button>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ Server Info ============
function ServerInfoSection() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['admin-server-info'], queryFn: testerApi.adminGetServerInfo })
  const [form, setForm] = useState({
    serverAddress: '', mcVersion: '', modVersion: '', connectionNotes: '',
    ndaText: '', discordLink: '', voiceChatInfo: '', active: false,
  })
  const [loaded, setLoaded] = useState(false)

  if (q.data && !loaded) {
    setForm({
      serverAddress: q.data.serverAddress ?? '',
      mcVersion: q.data.mcVersion ?? '',
      modVersion: q.data.modVersion ?? '',
      connectionNotes: q.data.connectionNotes ?? '',
      ndaText: q.data.ndaText ?? '',
      discordLink: q.data.discordLink ?? '',
      voiceChatInfo: q.data.voiceChatInfo ?? '',
      active: q.data.active ?? false,
    })
    setLoaded(true)
  }

  async function save(e: React.FormEvent) {
    e.preventDefault()
    try {
      await testerApi.adminUpdateServerInfo(form)
      qc.invalidateQueries({ queryKey: ['admin-server-info'] })
      toast.ok('💾 salvo')
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <form onSubmit={save} className="card-glow space-y-3 max-w-3xl">
      <h3 className="font-bold gradient-text">🌐 Configuração do servidor de teste</h3>
      <p className="text-xs text-liberthia-300/70">
        Essas informações aparecem na aba "Servidor" do dashboard dos testers aprovados.
      </p>

      <div className="flex items-center gap-2 p-3 rounded bg-liberthia-900/40">
        <input type="checkbox" id="active" checked={form.active}
          onChange={(e) => setForm({ ...form, active: e.target.checked })} />
        <label htmlFor="active" className="text-xs cursor-pointer">🟢 Servidor ATIVO (online)</label>
      </div>

      <div>
        <label className="label">Endereço do servidor</label>
        <input className="input text-xs font-mono" placeholder="test.liberthia.com:25565"
          value={form.serverAddress} onChange={(e) => setForm({ ...form, serverAddress: e.target.value })} />
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div>
          <label className="label">Versão MC</label>
          <input className="input text-xs" placeholder="1.20.1 Forge 47.4.18"
            value={form.mcVersion} onChange={(e) => setForm({ ...form, mcVersion: e.target.value })} />
        </div>
        <div>
          <label className="label">Versão dos mods</label>
          <input className="input text-xs" placeholder="0.1.8-beta"
            value={form.modVersion} onChange={(e) => setForm({ ...form, modVersion: e.target.value })} />
        </div>
      </div>

      <div>
        <label className="label">Instruções de conexão</label>
        <textarea className="input text-xs" rows={4}
          value={form.connectionNotes} onChange={(e) => setForm({ ...form, connectionNotes: e.target.value })} />
      </div>

      <div>
        <label className="label">Texto NDA / Avisos</label>
        <textarea className="input text-xs" rows={4}
          value={form.ndaText} onChange={(e) => setForm({ ...form, ndaText: e.target.value })} />
      </div>

      <div>
        <label className="label">Link Discord (opcional)</label>
        <input className="input text-xs" placeholder="https://discord.gg/..."
          value={form.discordLink} onChange={(e) => setForm({ ...form, discordLink: e.target.value })} />
      </div>

      <div>
        <label className="label">Voice Chat info</label>
        <input className="input text-xs"
          value={form.voiceChatInfo} onChange={(e) => setForm({ ...form, voiceChatInfo: e.target.value })} />
      </div>

      <button type="submit" className="btn w-full">💾 Salvar</button>
    </form>
  )
}

// ============ Suggestions Admin ============
function SuggestionsAdminSection() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<string>('all')
  const q = useQuery({
    queryKey: ['admin-suggestions', filter],
    queryFn: () => testerApi.adminListSuggestions(filter === 'all' ? undefined : filter),
    refetchInterval: 15000,
  })

  async function changeStatus(id: number, newStatus: string) {
    const note = prompt(`Nota pra "${newStatus}":`) ?? ''
    try {
      await testerApi.adminUpdateSuggestion(id, newStatus, note)
      qc.invalidateQueries({ queryKey: ['admin-suggestions'] })
      toast.ok(`→ ${newStatus}`)
    } catch (e: any) { toast.err(e.message) }
  }

  const statusColor: Record<string, string> = {
    PENDING: 'badge-yellow', UNDER_REVIEW: 'badge-purple', APPROVED: 'badge-green',
    REJECTED: 'badge-red', IMPLEMENTED: 'badge-green',
  }
  const typeIcon: Record<string, string> = {
    ITEM: '🗡', BLOCK: '🧱', ARTIFACT: '🏺', MECHANIC: '⚙', MOB: '👾', OTHER: '✨',
  }

  return (
    <div className="space-y-3">
      <div className="flex gap-1 flex-wrap">
        {['all', 'PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'IMPLEMENTED'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`btn-ghost btn-sm text-[10px] ${filter === s ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
            {s === 'all' ? 'Todas' : s}
          </button>
        ))}
      </div>

      <div className="space-y-2">
        {q.data?.suggestions?.map((s: any) => (
          <div key={s.id} className="card-glow">
            <div className="flex items-start gap-3">
              <div className="text-center shrink-0">
                <div className="text-3xl">{typeIcon[s.type] ?? '✨'}</div>
                <div className={`text-lg font-bold ${
                  s.score > 0 ? 'text-emerald-300' : s.score < 0 ? 'text-red-300' : 'text-liberthia-300'
                }`}>{s.score > 0 ? '+' : ''}{s.score}</div>
                <div className="text-[9px] text-liberthia-300/60">{s.upvotes}↑ / {s.downvotes}↓</div>
                {s.upvotes === s.downvotes && s.upvotes > 0 && (
                  <div className="text-[9px] text-amber-300">⚖ empate</div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-2 flex-wrap mb-1">
                  <h4 className="font-bold">{s.title}</h4>
                  <span className={`badge ${statusColor[s.status]} text-[9px]`}>{s.status}</span>
                  <span className="badge text-[9px]">{s.type}</span>
                </div>
                <div className="text-[10px] text-liberthia-300/60 mb-1">
                  por <b>{s.authorMcName}</b> · {new Date(s.createdAt).toLocaleDateString()}
                </div>
                <p className="text-xs text-liberthia-300/80 whitespace-pre-wrap">{s.description}</p>
                {s.technicalDetails && (
                  <details className="mt-2">
                    <summary className="cursor-pointer text-[10px] text-liberthia-300/60">Detalhes</summary>
                    <pre className="text-[10px] whitespace-pre-wrap mt-1 bg-liberthia-900/40 p-2 rounded">{s.technicalDetails}</pre>
                  </details>
                )}
                {s.adminNote && (
                  <div className="mt-2 text-[11px] text-amber-200/80 italic">Admin: {s.adminNote}</div>
                )}
                <div className="flex gap-1 mt-2 flex-wrap">
                  <button className="btn-ghost btn-sm text-[10px]" onClick={() => changeStatus(s.id, 'UNDER_REVIEW')}>👀 Análise</button>
                  <button className="btn-ghost btn-sm text-[10px] text-emerald-300" onClick={() => changeStatus(s.id, 'APPROVED')}>✓ Aprovar</button>
                  <button className="btn-ghost btn-sm text-[10px] text-red-300" onClick={() => changeStatus(s.id, 'REJECTED')}>✕ Rejeitar</button>
                  <button className="btn-ghost btn-sm text-[10px] text-blue-300" onClick={() => changeStatus(s.id, 'IMPLEMENTED')}>🎉 Implementada</button>
                </div>
              </div>
            </div>
          </div>
        ))}
        {q.data?.suggestions?.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">nenhuma sugestão</div>
        )}
      </div>
    </div>
  )
}

// ============================================================================
// CHANGELOG ADMIN
// ============================================================================

function ChangelogAdminSection() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<Partial<ChangelogEntryDto> | null>(null)
  const q = useQuery({ queryKey: ['admin-changelog'], queryFn: publicApi.changelog })

  function blank(): Partial<ChangelogEntryDto> {
    return {
      version: '', title: '', summary: '',
      itemsAdded: '', bugsFixed: '', buffs: '', debuffs: '',
      integrations: '', credits: '', notes: '',
      highlighted: false,
      releaseDate: new Date().toISOString(),
    }
  }

  async function save() {
    if (!editing) return
    try {
      if (editing.id) {
        await contentAdminApi.updateChangelog(editing.id, editing)
        toast.ok('Changelog atualizado')
      } else {
        await contentAdminApi.createChangelog(editing)
        toast.ok('Changelog criado')
      }
      setEditing(null)
      qc.invalidateQueries({ queryKey: ['admin-changelog'] })
      qc.invalidateQueries({ queryKey: ['public-changelog'] })
    } catch (e: any) {
      toast.err(e.message || 'erro')
    }
  }

  async function del(id: number) {
    if (!confirm('Deletar essa entrada do changelog?')) return
    try {
      await contentAdminApi.deleteChangelog(id)
      toast.ok('Removido')
      qc.invalidateQueries({ queryKey: ['admin-changelog'] })
      qc.invalidateQueries({ queryKey: ['public-changelog'] })
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-3 flex-wrap gap-2">
        <h2 className="font-bold gradient-text">📋 Changelog público</h2>
        <div className="flex items-center gap-2 flex-wrap">
          <BulkImportButton
            importer={testerApi.adminBulkImportChangelog}
            onSuccess={() => {
              qc.invalidateQueries({ queryKey: ['admin-changelog'] })
              qc.invalidateQueries({ queryKey: ['public-changelog'] })
            }}
            hint="changelog-update.json"
          />
          {/* Auto-gera draft de próxima entry com bugs CONFIRMED desde o
              último changelog OU último pacote uploaded (o mais recente).
              Garante que bugs já listados em release anterior NÃO reapareçam. */}
          <button className="btn btn-sm bg-purple-500/30 hover:bg-purple-500/40"
            title="Cria um draft com bugs/sugestões DESDE a última release"
            onClick={async () => {
              try {
                const draft = await contentAdminApi.autoGenerateNextChangelog()
                const meta = draft._meta ?? {}
                const cutoffStr = meta.cutoffAt
                  ? new Date(meta.cutoffAt).toLocaleString('pt-BR')
                  : 'início dos tempos'
                toast.ok(`📋 Draft gerado: ${meta.bugsCount ?? 0} bug(s) + ${meta.suggestionsCount ?? 0} sugestão(ões) desde ${cutoffStr}`)
                // Move o draft pro editor (mantém os arrays estruturados)
                setEditing(draft as any)
              } catch (e: any) {
                toast.err('Falha: ' + (e.message || 'erro'))
              }
            }}>
            ✨ Auto-gerar próxima
          </button>
          <button className="btn btn-sm" onClick={() => setEditing(blank())}>+ Nova entrada</button>
        </div>
      </div>

      {/* Editor */}
      {editing && (
        <div className="card-glow mb-4 space-y-2">
          <h3 className="font-bold text-sm">{editing.id ? '✏️ Editar' : '➕ Nova'}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            <div>
              <label className="label">Versão *</label>
              <input className="input" placeholder="v85" value={editing.version ?? ''}
                onChange={e => setEditing({ ...editing, version: e.target.value })} />
            </div>
            <div className="md:col-span-2">
              <label className="label">Título *</label>
              <input className="input" placeholder="Sistema de Roadmap + Bug Ranking" value={editing.title ?? ''}
                onChange={e => setEditing({ ...editing, title: e.target.value })} />
            </div>
          </div>
          <div>
            <label className="label">Resumo (1-2 linhas)</label>
            <textarea className="input" rows={2} value={editing.summary ?? ''}
              onChange={e => setEditing({ ...editing, summary: e.target.value })} />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            <BlockField label="🆕 Items adicionados" value={editing.itemsAdded ?? ''}
              onChange={v => setEditing({ ...editing, itemsAdded: v })}
              placeholder="- novo item X\n- novo bloco Y" />
            <BlockField label="🐛 Bugs corrigidos" value={editing.bugsFixed ?? ''}
              onChange={v => setEditing({ ...editing, bugsFixed: v })}
              placeholder="- crash no portal (reportado por @Steve)\n- texturas faltando" />
            <BlockField label="⬆ Buffs" value={editing.buffs ?? ''}
              onChange={v => setEditing({ ...editing, buffs: v })} placeholder="- espada agora dá +3 dano" />
            <BlockField label="⬇ Debuffs" value={editing.debuffs ?? ''}
              onChange={v => setEditing({ ...editing, debuffs: v })} placeholder="- machado mais lento" />
            <BlockField label="🔌 Integrações" value={editing.integrations ?? ''}
              onChange={v => setEditing({ ...editing, integrations: v })}
              placeholder="- agora compatível com Create" />
            <BlockField label="🏆 Créditos" value={editing.credits ?? ''}
              onChange={v => setEditing({ ...editing, credits: v })}
              placeholder="- @SteveBug por achar X bugs" />
          </div>
          <BlockField label="ℹ Notas" value={editing.notes ?? ''}
            onChange={v => setEditing({ ...editing, notes: v })}
            placeholder="known issues, warnings, etc" rows={2} />
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 text-xs">
              <input type="checkbox" checked={!!editing.highlighted}
                onChange={e => setEditing({ ...editing, highlighted: e.target.checked })} />
              ⭐ Marcar como destaque (aparece no topo)
            </label>
          </div>
          <div className="flex gap-2 pt-2">
            <button className="btn btn-sm" onClick={save}>💾 Salvar</button>
            <button className="btn-ghost btn-sm" onClick={() => setEditing(null)}>Cancelar</button>
          </div>
        </div>
      )}

      {/* Lista */}
      <div className="space-y-2">
        {q.data?.entries?.map((e: ChangelogEntryDto) => (
          <div key={e.id} className="card flex items-start gap-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs font-mono badge badge-purple">{e.version}</span>
                {e.highlighted && <span className="text-[10px]">⭐</span>}
                <span className="font-bold truncate">{e.title}</span>
              </div>
              {e.summary && <p className="text-xs text-liberthia-300/70 mt-1 truncate">{e.summary}</p>}
              <div className="text-[10px] text-liberthia-300/50 mt-1">
                {new Date(e.releaseDate).toLocaleString()}
              </div>
            </div>
            <div className="flex gap-1">
              <button className="btn-ghost btn-sm text-xs" onClick={() => setEditing(e)}>✏️</button>
              <button className="btn-ghost btn-sm text-xs text-red-300" onClick={() => del(e.id)}>🗑</button>
            </div>
          </div>
        ))}
        {q.data?.entries?.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">
            Sem releases publicadas. Cria a primeira pra começar.
          </div>
        )}
      </div>
    </div>
  )
}

function BlockField({ label, value, onChange, placeholder, rows = 4 }: {
  label: string; value: string; onChange: (v: string) => void; placeholder?: string; rows?: number
}) {
  return (
    <div>
      <label className="label text-[11px]">{label}</label>
      <textarea className="input text-xs font-mono" rows={rows} value={value}
        placeholder={placeholder?.replace(/\\n/g, '\n')}
        onChange={e => onChange(e.target.value)} />
      <div className="text-[10px] text-liberthia-300/50 mt-0.5">Uma linha por item</div>
    </div>
  )
}

// ============================================================================
// ROADMAP ADMIN
// ============================================================================

const ROADMAP_CATEGORIES: { value: string; label: string; emoji: string }[] = [
  { value: 'IDEA',      label: 'Idea',           emoji: '💡' },
  { value: 'PLANNED',   label: 'Planned',        emoji: '📅' },
  { value: 'IN_DEV',    label: 'Em desenvolvimento', emoji: '🔧' },
  { value: 'NEXT',      label: 'Próxima release',    emoji: '⏭' },
  { value: 'DONE',      label: 'Done',           emoji: '✅' },
  { value: 'CANCELLED', label: 'Cancelled',      emoji: '❌' },
]

function RoadmapAdminSection() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<Partial<RoadmapItemDto> | null>(null)
  const q = useQuery({ queryKey: ['admin-roadmap'], queryFn: publicApi.roadmapFlat })

  function blank(): Partial<RoadmapItemDto> {
    return { title: '', description: '', category: 'IDEA', emoji: '', tag: '', votes: 0, priority: 0, targetVersion: '' }
  }

  async function save() {
    if (!editing) return
    try {
      if (editing.id) {
        await contentAdminApi.updateRoadmap(editing.id, editing)
        toast.ok('Item atualizado')
      } else {
        await contentAdminApi.createRoadmap(editing)
        toast.ok('Item criado')
      }
      setEditing(null)
      qc.invalidateQueries({ queryKey: ['admin-roadmap'] })
      qc.invalidateQueries({ queryKey: ['public-roadmap'] })
    } catch (e: any) { toast.err(e.message || 'erro') }
  }

  async function del(id: number) {
    if (!confirm('Deletar esse item do roadmap?')) return
    try {
      await contentAdminApi.deleteRoadmap(id)
      toast.ok('Removido')
      qc.invalidateQueries({ queryKey: ['admin-roadmap'] })
      qc.invalidateQueries({ queryKey: ['public-roadmap'] })
    } catch (e: any) { toast.err(e.message) }
  }

  async function quickMove(id: number, category: string) {
    try {
      await contentAdminApi.updateRoadmap(id, { category: category as any })
      qc.invalidateQueries({ queryKey: ['admin-roadmap'] })
      qc.invalidateQueries({ queryKey: ['public-roadmap'] })
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-3 flex-wrap gap-2">
        <h2 className="font-bold gradient-text">🗺 Roadmap público</h2>
        <div className="flex items-center gap-2 flex-wrap">
          <BulkImportButton
            importer={testerApi.adminBulkImportRoadmap}
            onSuccess={() => {
              qc.invalidateQueries({ queryKey: ['admin-roadmap'] })
              qc.invalidateQueries({ queryKey: ['public-roadmap'] })
            }}
            hint="roadmap-update.json"
          />
          <button className="btn btn-sm" onClick={() => setEditing(blank())}>+ Novo item</button>
        </div>
      </div>

      {editing && (
        <div className="card-glow mb-4 space-y-2">
          <h3 className="font-bold text-sm">{editing.id ? '✏️ Editar' : '➕ Novo'}</h3>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-2">
            <div>
              <label className="label">Emoji</label>
              <input className="input text-center" maxLength={4} placeholder="🌟" value={editing.emoji ?? ''}
                onChange={e => setEditing({ ...editing, emoji: e.target.value })} />
            </div>
            <div className="md:col-span-3">
              <label className="label">Título *</label>
              <input className="input" placeholder="Sistema de quests dinâmicas" value={editing.title ?? ''}
                onChange={e => setEditing({ ...editing, title: e.target.value })} />
            </div>
          </div>
          <div>
            <label className="label">Descrição</label>
            <textarea className="input text-xs" rows={3} value={editing.description ?? ''}
              onChange={e => setEditing({ ...editing, description: e.target.value })}
              placeholder="Explica o que vai ser feito, contexto, etc..." />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-2">
            <div>
              <label className="label">Categoria *</label>
              <select className="input" value={editing.category ?? 'IDEA'}
                onChange={e => setEditing({ ...editing, category: e.target.value as any })}>
                {ROADMAP_CATEGORIES.map(c => (
                  <option key={c.value} value={c.value}>{c.emoji} {c.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Tag</label>
              <input className="input" placeholder="lore, voice, perf" value={editing.tag ?? ''}
                onChange={e => setEditing({ ...editing, tag: e.target.value })} />
            </div>
            <div>
              <label className="label">Versão alvo</label>
              <input className="input" placeholder="v90" value={editing.targetVersion ?? ''}
                onChange={e => setEditing({ ...editing, targetVersion: e.target.value })} />
            </div>
            <div>
              <label className="label">Prioridade</label>
              <input className="input" type="number" value={editing.priority ?? 0}
                onChange={e => setEditing({ ...editing, priority: parseInt(e.target.value || '0') })} />
            </div>
          </div>
          {editing.id !== undefined && (
            <div>
              <label className="label">Votos (manual override)</label>
              <input className="input w-32" type="number" value={editing.votes ?? 0}
                onChange={e => setEditing({ ...editing, votes: parseInt(e.target.value || '0') })} />
              <div className="text-[10px] text-liberthia-300/50">cuidado: sobrescreve contador público</div>
            </div>
          )}
          <div className="flex gap-2 pt-2">
            <button className="btn btn-sm" onClick={save}>💾 Salvar</button>
            <button className="btn-ghost btn-sm" onClick={() => setEditing(null)}>Cancelar</button>
          </div>
        </div>
      )}

      {/* Lista agrupada */}
      <div className="space-y-2">
        {q.data?.items?.map((it: RoadmapItemDto) => {
          const cat = ROADMAP_CATEGORIES.find(c => c.value === it.category)
          return (
            <div key={it.id} className="card flex items-center gap-3">
              <div className="text-2xl shrink-0">{it.emoji || '📌'}</div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-bold text-sm truncate">{it.title}</span>
                  <span className="text-[10px] badge">{cat?.emoji} {cat?.label}</span>
                  {it.tag && <span className="text-[10px] badge badge-purple">#{it.tag}</span>}
                  {it.targetVersion && <span className="text-[10px] badge badge-green">→ {it.targetVersion}</span>}
                  <span className="text-[10px] text-purple-300">▲ {it.votes}</span>
                </div>
                {it.description && (
                  <p className="text-[11px] text-liberthia-300/70 mt-1 line-clamp-2">{it.description}</p>
                )}
              </div>
              <div className="flex flex-col gap-1 shrink-0">
                <select className="input !p-1 text-[10px]" value={it.category}
                  onChange={e => quickMove(it.id, e.target.value)}>
                  {ROADMAP_CATEGORIES.map(c => (
                    <option key={c.value} value={c.value}>{c.emoji}</option>
                  ))}
                </select>
                <button className="btn-ghost btn-sm text-xs" onClick={() => setEditing(it)}>✏️</button>
                <button className="btn-ghost btn-sm text-xs text-red-300" onClick={() => del(it.id)}>🗑</button>
              </div>
            </div>
          )
        })}
        {q.data?.items?.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">
            Sem items no roadmap. Cria pra começar a coletar votos da comunidade.
          </div>
        )}
      </div>
    </div>
  )
}

// ============ 3D Models (BlockBench) ============
// CRUD de modelos .bbmodel com preview 3D inline. Upload via multipart,
// validação básica no backend (precisa ter "elements"). Toggle pra mostrar/
// ocultar dos testers sem deletar do disco.
function ModelsSection() {
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('')
  const [busy, setBusy] = useState(false)
  const [previewId, setPreviewId] = useState<number | null>(null)
  const [editId, setEditId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editCategory, setEditCategory] = useState('')

  const q = useQuery({
    queryKey: ['admin-tester-models'],
    queryFn: testerApi.adminListModels,
    refetchInterval: 30000,
  })

  const ALLOWED_MODEL_EXTS = ['.bbmodel', '.gltf', '.glb', '.obj']

  async function upload(e: React.FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) return toast.err('selecione um arquivo de modelo 3D')
    const low = file.name.toLowerCase()
    const okExt = ALLOWED_MODEL_EXTS.some(ext => low.endsWith(ext))
    if (!okExt) return toast.err('formato inválido (use .bbmodel, .gltf, .glb ou .obj)')
    if (!name.trim()) return toast.err('nome obrigatório')
    const maxMB = low.endsWith('.glb') ? 30 : 15
    if (file.size > maxMB * 1024 * 1024) return toast.err(`arquivo muito grande (máx ${maxMB}MB)`)
    setBusy(true)
    try {
      await testerApi.adminUploadModel(file, name.trim(), description.trim() || undefined, category.trim() || undefined)
      setName(''); setDescription(''); setCategory('')
      if (fileRef.current) fileRef.current.value = ''
      qc.invalidateQueries({ queryKey: ['admin-tester-models'] })
      toast.ok('🧊 modelo enviado')
    } catch (e: any) {
      toast.err(e.message ?? 'erro no upload')
    } finally {
      setBusy(false)
    }
  }

  async function del(id: number) {
    if (!confirm('Deletar modelo? Os testers perderão acesso.')) return
    try {
      await testerApi.adminDeleteModel(id)
      qc.invalidateQueries({ queryKey: ['admin-tester-models'] })
      toast.ok('🗑')
    } catch (e: any) { toast.err(e.message) }
  }

  async function toggle(id: number) {
    try {
      await testerApi.adminToggleModel(id)
      qc.invalidateQueries({ queryKey: ['admin-tester-models'] })
    } catch (e: any) { toast.err(e.message) }
  }

  function startEdit(m: any) {
    setEditId(m.id)
    setEditName(m.name ?? '')
    setEditDescription(m.description ?? '')
    setEditCategory(m.category ?? '')
  }

  async function saveEdit() {
    if (!editId) return
    try {
      await testerApi.adminUpdateModel(editId, {
        name: editName.trim() || undefined,
        description: editDescription,
        category: editCategory,
      })
      setEditId(null)
      qc.invalidateQueries({ queryKey: ['admin-tester-models'] })
      toast.ok('💾')
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <form onSubmit={upload} className="card-glow space-y-2">
        <h3 className="font-bold mb-2">⬆ Upload modelo 3D</h3>
        <input
          className="input text-xs"
          placeholder="Nome (ex: Dragon Sword)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <input
          className="input text-xs"
          placeholder="Categoria (ex: armas, blocos)"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        />
        <textarea
          className="input text-xs"
          rows={3}
          placeholder="Descrição (opcional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <input
          type="file"
          accept=".bbmodel,.gltf,.glb,.obj"
          ref={fileRef}
          className="text-xs w-full file:btn-ghost file:btn-sm file:mr-2"
          required
        />
        <button type="submit" className="btn w-full" disabled={busy}>
          {busy ? '...' : '🧊 Upload modelo'}
        </button>
        <div className="text-[10px] text-liberthia-300/50 italic space-y-1">
          <div><strong>Formatos aceitos:</strong></div>
          <ul className="pl-3 list-disc space-y-0.5">
            <li><strong>.bbmodel</strong> — BlockBench nativo (texturas embutidas)</li>
            <li><strong>.glb</strong> — GLTF binário (1 arquivo, texturas embutidas, recomendado)</li>
            <li><strong>.gltf</strong> — GLTF text JSON (texturas externas NÃO funcionam — embuta com base64)</li>
            <li><strong>.obj</strong> — Wavefront (renderiza cinza, sem texturas/.mtl)</li>
          </ul>
          <div className="opacity-70">Limite: 15MB (.bbmodel/.gltf/.obj) · 30MB (.glb)</div>
        </div>
      </form>

      <div className="lg:col-span-2 space-y-2">
        {q.isLoading && <div className="card text-xs text-liberthia-300/60">Carregando...</div>}
        {q.data?.models?.length === 0 && (
          <div className="card text-center py-6 text-xs text-liberthia-300/50">
            nenhum modelo ainda — suba o primeiro .bbmodel
          </div>
        )}
        {q.data?.models?.map((m: any) => (
          <div key={m.id} className={`card-glow ${!m.enabled ? 'opacity-50' : ''}`}>
            {editId === m.id ? (
              <div className="space-y-2">
                <input className="input text-xs" value={editName} onChange={e => setEditName(e.target.value)} placeholder="Nome" />
                <input className="input text-xs" value={editCategory} onChange={e => setEditCategory(e.target.value)} placeholder="Categoria" />
                <textarea className="input text-xs" rows={2} value={editDescription} onChange={e => setEditDescription(e.target.value)} placeholder="Descrição" />
                <div className="flex gap-1">
                  <button className="btn btn-sm text-xs flex-1" onClick={saveEdit}>💾 Salvar</button>
                  <button className="btn-ghost btn-sm text-xs" onClick={() => setEditId(null)}>✕</button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-start gap-2">
                  <div className="text-2xl">
                    {m.format === 'gltf' || m.format === 'glb' ? '✨' : m.format === 'obj' ? '📐' : '🧊'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="font-bold flex items-center gap-1 flex-wrap">
                      {m.name}
                      <span className="badge bg-blue-500/20 text-blue-300 text-[10px] font-mono uppercase">{m.format ?? 'bbmodel'}</span>
                      {m.category && <span className="badge badge-purple text-[10px]">{m.category}</span>}
                      {!m.enabled && <span className="badge bg-red-500/20 text-red-300 text-[10px]">oculto</span>}
                    </h4>
                    <div className="text-[10px] text-liberthia-300/60 font-mono">
                      {m.filename} · {(m.sizeBytes / 1024).toFixed(1)}KB · 👁{m.viewCount ?? 0}
                    </div>
                    {m.description && (
                      <p className="text-xs text-liberthia-300/70 mt-1">{m.description}</p>
                    )}
                  </div>
                  <div className="flex flex-col gap-1">
                    <button className="btn-ghost btn-sm text-[10px]" title={previewId === m.id ? 'fechar' : 'preview 3D'}
                            onClick={() => setPreviewId(previewId === m.id ? null : m.id)}>
                      {previewId === m.id ? '⊟' : '👁 3D'}
                    </button>
                    <button className="btn-ghost btn-sm text-[10px]" title="editar" onClick={() => startEdit(m)}>✏</button>
                    <button className="btn-ghost btn-sm text-[10px]" onClick={() => toggle(m.id)}>
                      {m.enabled ? '⏸' : '▶'}
                    </button>
                    <button className="btn-ghost btn-sm text-[10px] text-red-400" onClick={() => del(m.id)}>🗑</button>
                  </div>
                </div>

                {previewId === m.id && <AdminModelPreview modelId={m.id} format={m.format ?? 'bbmodel'} />}
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ Beta Audios ============
function AudiosSection() {
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('criatura')
  const [creatureId, setCreatureId] = useState('')
  const [busy, setBusy] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editCategory, setEditCategory] = useState('')
  const [editCreatureId, setEditCreatureId] = useState('')

  const q = useQuery({
    queryKey: ['admin-tester-audios'],
    queryFn: testerApi.adminListAudios,
    refetchInterval: 30000,
  })

  async function upload(e: React.FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) return toast.err('selecione um áudio')
    const allowed = /\.(mp3|wav|ogg|oga|flac|m4a)$/i
    if (!allowed.test(file.name)) return toast.err('formato não suportado (use mp3, wav, ogg, flac ou m4a)')
    if (!name.trim()) return toast.err('nome obrigatório')
    if (file.size > 25 * 1024 * 1024) return toast.err('arquivo muito grande (máx 25MB)')
    setBusy(true)
    try {
      // Tenta extrair duração via HTMLAudioElement antes do upload
      let durationSec: number | undefined
      try {
        const tmpUrl = URL.createObjectURL(file)
        const audio = new Audio(tmpUrl)
        await new Promise<void>((resolve, reject) => {
          audio.addEventListener('loadedmetadata', () => resolve())
          audio.addEventListener('error', () => reject())
          setTimeout(() => reject(), 5000)
        })
        if (isFinite(audio.duration) && audio.duration > 0) {
          durationSec = Math.round(audio.duration)
        }
        URL.revokeObjectURL(tmpUrl)
      } catch {
        // ignora — duração é opcional
      }

      await testerApi.adminUploadAudio(file, name.trim(), {
        description: description.trim() || undefined,
        category: category.trim() || undefined,
        creatureId: creatureId.trim() || undefined,
        durationSec,
      })
      setName(''); setDescription(''); setCreatureId('')
      if (fileRef.current) fileRef.current.value = ''
      qc.invalidateQueries({ queryKey: ['admin-tester-audios'] })
      toast.ok('🔊 áudio enviado')
    } catch (e: any) {
      toast.err(e.message ?? 'erro no upload')
    } finally {
      setBusy(false)
    }
  }

  async function del(id: number) {
    if (!confirm('Deletar áudio? Votos serão preservados (FK ID continua válido).')) return
    try {
      await testerApi.adminDeleteAudio(id)
      qc.invalidateQueries({ queryKey: ['admin-tester-audios'] })
      toast.ok('🗑')
    } catch (e: any) { toast.err(e.message) }
  }

  async function toggle(id: number) {
    try {
      await testerApi.adminToggleAudio(id)
      qc.invalidateQueries({ queryKey: ['admin-tester-audios'] })
    } catch (e: any) { toast.err(e.message) }
  }

  function startEdit(a: any) {
    setEditId(a.id)
    setEditName(a.name ?? '')
    setEditDescription(a.description ?? '')
    setEditCategory(a.category ?? '')
    setEditCreatureId(a.creatureId ?? '')
  }

  async function saveEdit() {
    if (!editId) return
    try {
      await testerApi.adminUpdateAudio(editId, {
        name: editName.trim() || undefined,
        description: editDescription,
        category: editCategory,
        creatureId: editCreatureId,
      })
      setEditId(null)
      qc.invalidateQueries({ queryKey: ['admin-tester-audios'] })
      toast.ok('💾')
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <form onSubmit={upload} className="card-glow space-y-2">
        <h3 className="font-bold mb-2">⬆ Upload áudio</h3>
        <input
          className="input text-xs"
          placeholder="Nome (ex: Grito do Wendigo)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <select
          className="input text-xs"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        >
          <option value="criatura">criatura</option>
          <option value="instrumento">instrumento</option>
          <option value="ambient">ambient</option>
          <option value="UI">UI</option>
          <option value="effect">effect</option>
          <option value="dialogo">diálogo</option>
        </select>
        <input
          className="input text-xs"
          placeholder="Criatura/ItemID (ex: liberthia:wendigo)"
          value={creatureId}
          onChange={(e) => setCreatureId(e.target.value)}
        />
        <textarea
          className="input text-xs"
          rows={3}
          placeholder="Descrição (contexto, ideia, alternativas)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <input
          type="file"
          accept=".mp3,.wav,.ogg,.oga,.flac,.m4a,audio/*"
          ref={fileRef}
          className="text-xs w-full file:btn-ghost file:btn-sm file:mr-2"
          required
        />
        <button type="submit" className="btn w-full" disabled={busy}>
          {busy ? '...' : '🔊 Upload áudio'}
        </button>
        <p className="text-[10px] text-liberthia-300/50 italic">
          Formatos: MP3, WAV, OGG, FLAC, M4A. Limite: 25MB.
          Duração é extraída automaticamente do header.
        </p>
      </form>

      <div className="lg:col-span-2 space-y-2">
        {q.isLoading && <div className="card text-xs text-liberthia-300/60">Carregando...</div>}
        {q.data?.audios?.length === 0 && (
          <div className="card text-center py-6 text-xs text-liberthia-300/50">
            nenhum áudio ainda — suba o primeiro
          </div>
        )}
        {q.data?.audios?.map((a: any) => (
          <div key={a.id} className={`card-glow ${!a.enabled ? 'opacity-50' : ''}`}>
            {editId === a.id ? (
              <div className="space-y-2">
                <input className="input text-xs" value={editName} onChange={e => setEditName(e.target.value)} placeholder="Nome" />
                <input className="input text-xs" value={editCategory} onChange={e => setEditCategory(e.target.value)} placeholder="Categoria" />
                <input className="input text-xs" value={editCreatureId} onChange={e => setEditCreatureId(e.target.value)} placeholder="Criatura ID" />
                <textarea className="input text-xs" rows={2} value={editDescription} onChange={e => setEditDescription(e.target.value)} placeholder="Descrição" />
                <div className="flex gap-1">
                  <button className="btn btn-sm text-xs flex-1" onClick={saveEdit}>💾 Salvar</button>
                  <button className="btn-ghost btn-sm text-xs" onClick={() => setEditId(null)}>✕</button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-start gap-2">
                  <div className="text-2xl">
                    {a.category === 'criatura' ? '👹' :
                      a.category === 'instrumento' ? '🎺' :
                        a.category === 'ambient' ? '🌫' : '🔊'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="font-bold flex items-center gap-1 flex-wrap">
                      {a.name}
                      {a.category && <span className="badge badge-purple text-[10px]">{a.category}</span>}
                      {!a.enabled && <span className="badge bg-red-500/20 text-red-300 text-[10px]">oculto</span>}
                    </h4>
                    <div className="text-[10px] text-liberthia-300/60 font-mono">
                      {a.filename} · {(a.sizeBytes / 1024).toFixed(1)}KB
                      {a.durationSec ? ` · ${a.durationSec}s` : ''}
                      {' · 🔊'}{a.playCount ?? 0}
                    </div>
                    {a.creatureId && (
                      <div className="text-[10px] font-mono text-purple-300/80 mt-0.5">🎯 {a.creatureId}</div>
                    )}
                    {a.description && (
                      <p className="text-xs text-liberthia-300/70 mt-1">{a.description}</p>
                    )}
                  </div>
                  <div className="flex flex-col gap-1">
                    <button className="btn-ghost btn-sm text-[10px]" title="editar" onClick={() => startEdit(a)}>✏</button>
                    <button className="btn-ghost btn-sm text-[10px]" onClick={() => toggle(a.id)}>
                      {a.enabled ? '⏸' : '▶'}
                    </button>
                    <button className="btn-ghost btn-sm text-[10px] text-red-400" onClick={() => del(a.id)}>🗑</button>
                  </div>
                </div>

                <div className="mt-2">
                  <AdminAudioPreview audioId={a.id} mimeType={a.mimeType} title={a.name} />
                </div>
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

function AdminAudioPreview({ audioId, mimeType, title }:
  { audioId: number; mimeType?: string; title?: string }) {
  const [Player, setPlayer] = useState<any>(null)
  useEffect(() => {
    import('../components/AudioPlayer').then(m => setPlayer(() => m.AudioPlayer))
  }, [])
  if (!Player) {
    return <div className="rounded bg-slate-800/50 p-2 text-xs text-slate-400 animate-pulse">carregando player...</div>
  }
  return (
    <Player
      streamUrl={`/api/admin/tester/audios/${audioId}/stream`}
      mimeType={mimeType}
      title={title}
      adminMode={true}
      compact={true}
    />
  )
}

function AdminModelPreview({ modelId, format }: { modelId: number; format: 'bbmodel' | 'gltf' | 'glb' | 'obj' }) {
  const [modelData, setModelData] = useState<any>(null)
  const [error, setError] = useState<string | null>(null)
  const [Viewer, setViewer] = useState<any>(null)

  useEffect(() => {
    setModelData(null); setError(null)
    const token = localStorage.getItem('liberthia.token')
    if (!token) {
      setError('Sessão expirou')
      return
    }
    let cancelled = false
    import('../components/Model3DViewer').then(({ Model3DViewer, fetchModelData }) => {
      setViewer(() => Model3DViewer)
      return fetchModelData(`/api/admin/tester/models/${modelId}/file`, format, token)
    }).then(data => {
      if (!cancelled) setModelData(data)
    }).catch(e => {
      if (!cancelled) setError(e?.message ?? String(e))
    })
    return () => { cancelled = true }
  }, [modelId, format])

  if (error) {
    return <div className="mt-2 rounded border border-red-700 bg-red-950/30 p-2 text-xs text-red-300">⚠️ {error}</div>
  }
  if (!modelData || !Viewer) {
    return (
      <div className="mt-2 rounded border border-slate-700 bg-slate-900 h-48 flex items-center justify-center text-xs text-liberthia-300/60 animate-pulse">
        carregando preview {format}...
      </div>
    )
  }
  return (
    <div className="mt-2">
      <Viewer model={modelData} width={500} height={350} autoRotate={true} />
    </div>
  )
}
