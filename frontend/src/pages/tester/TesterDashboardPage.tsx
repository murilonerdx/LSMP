import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { testerApi } from '../../lib/api'
import { TesterDto, getTesterToken, clearTesterAuth } from '../../store/testerAuth'
import {
  SplashesTab,
  BalanceTab,
  WikiTab,
  NotificationsBell,
  NotificationsPanel,
} from './TesterTabsExtra'
import { Sparkline, BarChart, Donut, StatCard } from '../../components/MiniCharts'
import { RecipeBuilder as RecipeBuilderEmbed } from '../../components/RecipeBuilder'
import { publicApi } from '../../lib/api'
import { BugDetailModal, BugLike } from '../../components/BugDetailModal'
import { ChangelogDetailModal } from '../../components/ChangelogDetailModal'
import type { ChangelogEntryDto } from '../../lib/api'

/**
 * Dashboard do tester logado — 5 abas funcionais:
 *  - 👁 Visão geral (stats + último login)
 *  - 📥 Downloads (lista de ZIPs liberados)
 *  - 🧪 Items beta (catálogo)
 *  - 🐛 Bug reports (criar + ver meus)
 *  - 🎁 Recompensas (catálogo + meus resgates)
 */

type Tab = 'overview' | 'server' | 'downloads' | 'items' | 'models' | 'audios' | 'bugs' | 'suggestions' | 'splashes' | 'balance' | 'wiki' | 'rewards'

// Fonte da verdade pras tabs do dashboard. Antes cada `<TabBtn>` repetia o
// emoji+texto inline; agora vem desse array e o nav é gerado por map(). Pra
// adicionar tab nova: incluir aqui + adicionar o componente no switch.
const TESTER_TABS: { id: Tab; label: string; icon: string }[] = [
  { id: 'overview',    label: 'Visão geral', icon: '👁' },
  { id: 'server',      label: 'Servidor',    icon: '🌐' },
  { id: 'downloads',   label: 'Downloads',   icon: '📥' },
  { id: 'items',       label: 'Items beta',  icon: '🧪' },
  { id: 'models',      label: 'Modelos 3D',  icon: '🧊' },
  { id: 'audios',      label: 'Áudios',      icon: '🔊' },
  { id: 'bugs',        label: 'Bug reports', icon: '🐛' },
  { id: 'suggestions', label: 'Sugestões',   icon: '💡' },
  { id: 'splashes',    label: 'Splashes',    icon: '✨' },
  { id: 'balance',     label: 'Buff/Nerf',   icon: '⚖' },
  { id: 'wiki',        label: 'Wiki',        icon: '📚' },
  { id: 'rewards',     label: 'Recompensas', icon: '🎁' },
]

function fmtBytes(b: number): string {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)}KB`
  if (b < 1024 * 1024 * 1024) return `${(b / (1024 * 1024)).toFixed(1)}MB`
  return `${(b / (1024 * 1024 * 1024)).toFixed(2)}GB`
}

export function TesterDashboardPage() {
  const nav = useNavigate()
  const [tab, setTab] = useState<Tab>('overview')
  const [notifOpen, setNotifOpen] = useState(false)

  useEffect(() => {
    if (!getTesterToken()) nav('/tester/login', { replace: true })
  }, [])

  // Lê ?tab=xxx da URL pra navegar via deep-link de notificação
  useEffect(() => {
    const url = new URL(window.location.href)
    const t = url.searchParams.get('tab')
    if (t && ['overview', 'server', 'downloads', 'items', 'models', 'audios', 'bugs', 'suggestions', 'splashes', 'balance', 'wiki', 'rewards'].includes(t)) {
      setTab(t as Tab)
    }
  }, [])

  const meQ = useQuery({
    queryKey: ['tester-me'],
    queryFn: testerApi.me,
    refetchInterval: 30_000,
  })

  function logout() {
    clearTesterAuth()
    nav('/tester/login', { replace: true })
  }

  const me = meQ.data as TesterDto | undefined

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950">
      <header className="border-b border-purple-500/30 bg-liberthia-900/60 backdrop-blur sticky top-0 z-20">
        <div className="max-w-6xl mx-auto p-2 sm:p-3 flex items-center gap-2 sm:gap-3">
          <img src={`https://mc-heads.net/avatar/${me?.mcName ?? 'Steve'}/40`}
               className="w-8 h-8 sm:w-10 sm:h-10 rounded shadow flex-shrink-0"
               alt={me?.mcName ?? ''} />
          <div className="flex-1 min-w-0">
            <h1 className="font-bold text-sm sm:text-base gradient-text truncate">
              <span className="hidden sm:inline">Mod Tester · </span>{me?.mcName ?? '...'}
            </h1>
            <div className="text-[9px] sm:text-[10px] text-liberthia-300/60">
              <span className="text-purple-300 font-bold">⭐ {me?.points ?? 0} pts</span>
              {me?.lastLoginAt && (
                <span className="ml-2 hidden sm:inline">· último login: {new Date(me.lastLoginAt).toLocaleString()}</span>
              )}
            </div>
          </div>
          <NotificationsBell onOpen={() => setNotifOpen(true)} />
          <button className="btn-ghost btn-sm text-xs" onClick={logout}>↪ Sair</button>
        </div>

        {/* Tab nav responsivo — grid pra TODAS as 12 tabs caberem na tela sem
            scroll lateral. Aumentado em v105: antes era 3 colunas no mobile
            com text-[9px] que estourava em algumas tabs ("Bug reports",
            "Recompensas") sobrepondo o ícone. Agora: 2 cols no mobile (texto
            cabe), 3 sm, 4 md, 6 lg (final em desktop maior). Cada tab tem
            min-height pra ficarem alinhadas independente do tamanho do texto. */}
        <nav className="max-w-6xl mx-auto px-3 pb-2 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-1.5">
          {TESTER_TABS.map(t => (
            <TabBtn key={t.id} id={t.id} cur={tab} onClick={setTab}>
              <span className="text-base leading-none">{t.icon}</span>
              <span className="text-[11px] sm:text-xs font-medium leading-tight">{t.label}</span>
            </TabBtn>
          ))}
        </nav>
      </header>

      <main className="max-w-6xl mx-auto p-3 sm:p-4">
        {tab === 'overview' && <OverviewTab me={me} />}
        {tab === 'server' && <ServerTab />}
        {tab === 'downloads' && <DownloadsTab />}
        {tab === 'items' && <BetaItemsTab onReport={(id) => setTab('bugs')} />}
        {tab === 'models' && <ModelsTab />}
        {tab === 'audios' && <AudiosTab />}
        {tab === 'bugs' && <BugsTab />}
        {tab === 'suggestions' && <SuggestionsTab />}
        {tab === 'splashes' && <SplashesTab />}
        {tab === 'balance' && <BalanceTab />}
        {tab === 'wiki' && <WikiTab />}
        {tab === 'rewards' && <RewardsTab me={me} />}
      </main>

      {notifOpen && <NotificationsPanel onClose={() => setNotifOpen(false)} />}
    </div>
  )
}

function TabBtn({ id, cur, onClick, children }:
  { id: Tab; cur: Tab; onClick: (t: Tab) => void; children: React.ReactNode }) {
  const active = id === cur
  return (
    <button onClick={() => onClick(id)}
      className={`px-2 py-2 rounded text-center transition-colors flex flex-col items-center justify-center gap-0.5 min-h-[44px] min-w-0 ${
        active
          ? 'bg-purple-500/30 text-white font-bold ring-1 ring-purple-400'
          : 'bg-liberthia-900/30 text-liberthia-300/70 hover:bg-liberthia-700/40 hover:text-white'
      }`}>
      {children}
    </button>
  )
}

// ============ Overview (Dashboard rico — v96) ============
function OverviewTab({ me }: { me?: TesterDto }) {
  // Entry de changelog selecionada (abre modal de detalhe)
  const [changelogDetail, setChangelogDetail] = useState<ChangelogEntryDto | null>(null)
  const bugsQ = useQuery({ queryKey: ['tester-bugs-mine'], queryFn: testerApi.myBugs })
  const redemptQ = useQuery({ queryKey: ['tester-redemptions-mine'], queryFn: testerApi.myRedemptions })
  const serverQ = useQuery({ queryKey: ['tester-server-info'], queryFn: testerApi.serverInfo, refetchInterval: 30_000 })
  const pkgQ = useQuery({ queryKey: ['tester-packages-overview'], queryFn: testerApi.packages })
  const betaQ = useQuery({ queryKey: ['tester-beta-overview'], queryFn: testerApi.betaItems })
  const communityQ = useQuery({
    queryKey: ['tester-community-bugs'],
    queryFn: () => testerApi.communityBugs(15),
    refetchInterval: 60_000,
  })
  const changelogQ = useQuery({
    queryKey: ['public-changelog-overview'],
    queryFn: publicApi.changelog,
    staleTime: 5 * 60_000,
  })
  // Bug Hunter Leaderboard público — usa o mesmo endpoint da página
  // /leaderboard (sem auth). Mostra quem pegou mais bugs confirmados.
  // Tester vê onde tá no ranking e os top da casa.
  const leaderboardQ = useQuery({
    queryKey: ['public-leaderboard-overview'],
    queryFn: publicApi.leaderboard,
    staleTime: 2 * 60_000,
    refetchInterval: 5 * 60_000,
  })

  const bugs = bugsQ.data?.bugs ?? []
  const totalBugs = bugs.length
  const confirmedBugs = bugs.filter((b: any) => b.status === 'CONFIRMED').length
  const rejectedBugs = bugs.filter((b: any) => b.status === 'REJECTED').length
  const totalRedemptions = redemptQ.data?.redemptions?.length ?? 0

  // Trend dos últimos 7 dias de bugs reportados pelo tester
  const sevenDayTrend = useMemo(() => {
    const counts = new Array(7).fill(0)
    const dayMs = 86400000
    const now = Date.now()
    for (const b of bugs) {
      const t = new Date(b.createdAt).getTime()
      const daysAgo = Math.floor((now - t) / dayMs)
      if (daysAgo >= 0 && daysAgo < 7) counts[6 - daysAgo]++
    }
    return counts
  }, [bugs])

  const labelsDay = ['7d', '6d', '5d', '4d', '3d', '2d', 'hoje']

  // Bug status donut
  const totalForDonut = totalBugs || 1
  const pendingBugs = totalBugs - confirmedBugs - rejectedBugs

  const serverOnline = serverQ.data?.active === true
  const newestPkg = (pkgQ.data?.packages ?? []).slice(0, 3)
  const newestItems = (betaQ.data?.items ?? []).slice(0, 4)
  const communityBugs = communityQ.data?.bugs ?? []

  return (
    <div className="space-y-3">
      {/* Header de boas vindas + status servidor */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div className="card-glow md:col-span-2 !bg-gradient-to-br from-purple-900/40 to-liberthia-900/40">
          <div className="flex items-start gap-3">
            <img src={`https://mc-heads.net/avatar/${me?.mcName ?? 'Steve'}/64`}
              className="w-16 h-16 rounded shadow-lg" alt={me?.mcName} />
            <div className="flex-1 min-w-0">
              <h2 className="font-bold text-lg gradient-text">Bem-vindo, {me?.mcName} 🧪</h2>
              <div className="flex flex-wrap gap-3 mt-2 text-xs">
                <span className="text-purple-300 font-bold">⭐ {me?.points ?? 0} pts</span>
                <span className="text-amber-300">🐛 {confirmedBugs} confirmados</span>
                <span className="text-emerald-300">🎁 {totalRedemptions} resgates</span>
                {me?.createdAt && (
                  <span className="text-liberthia-300/60">
                    membro há {Math.floor((Date.now() - new Date(me.createdAt).getTime()) / 86400000)} dias
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
        <ServerStatusWidget online={serverOnline} info={serverQ.data} />
      </div>

      {/* Cards de stats com sparklines */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard label="Pontos" value={me?.points ?? 0} icon="⭐" color="purple"
          trend={[2, 4, 3, 6, 5, 8, me?.points ? Math.max(8, me.points / 10) : 9]}
          sub="ganhos ao longo do tempo" />
        <StatCard label="Bugs reportados" value={totalBugs} icon="🐛" color="amber"
          trend={sevenDayTrend}
          sub={`+${sevenDayTrend[6]} hoje`} />
        <StatCard label="Confirmados" value={confirmedBugs} icon="✅" color="emerald"
          sub={`${totalBugs > 0 ? Math.round(confirmedBugs / totalBugs * 100) : 0}% de aprovação`} />
        <StatCard label="Resgates" value={totalRedemptions} icon="🎁" color="pink" />
      </div>

      {/* Linha 2: Bug status donut + 7-day trend + bugs community */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div className="card-glow">
          <h3 className="text-xs font-bold mb-2 gradient-text">🐛 Status dos seus bugs</h3>
          <div className="flex items-center justify-around">
            <Donut value={confirmedBugs} total={totalForDonut} color="#34D399" label="conf." />
            <Donut value={pendingBugs} total={totalForDonut} color="#FCD34D" label="pend." />
            <Donut value={rejectedBugs} total={totalForDonut} color="#F87171" label="rej." />
          </div>
        </div>
        <div className="card-glow">
          <h3 className="text-xs font-bold mb-2 gradient-text">📊 Atividade (7 dias)</h3>
          <div className="flex items-center justify-center pt-2">
            <BarChart data={sevenDayTrend} labels={labelsDay} width={260} height={70} />
          </div>
          <div className="text-[10px] text-liberthia-300/60 text-center mt-1">
            Bugs reportados por dia
          </div>
        </div>
        <div className="card-glow">
          <h3 className="text-xs font-bold mb-2 gradient-text">📦 Downloads</h3>
          {newestPkg.length === 0 ? (
            <div className="text-[10px] text-liberthia-300/50 text-center py-4">nenhum pacote</div>
          ) : (
            <ul className="space-y-1.5 text-xs">
              {/* Cada item agora é um anchor que aponta direto pro download
                  do pacote — clica e baixa, sem precisar ir pra aba Downloads.
                  Usa testerApi.packageDownloadUrl que já anexa ?token= pro
                  backend autenticar via query param (anchor não consegue
                  mandar Authorization header). */}
              {/* v0.1.27: backend filtra packages fantasmas (fileExists=false)
                  pro tester, então aqui só renderizamos os packages que têm
                  arquivo. Removeu o UI feio de "perdido" que aparecia antes. */}
              {newestPkg.map((p: any) => (
                <li key={p.id}>
                  <a
                    href={testerApi.packageDownloadUrl(p.id)}
                    download={p.filename || `pacote-${p.id}.zip`}
                    rel="noopener"
                    className="flex items-center gap-2 hover:bg-purple-500/15 rounded p-1 transition-colors cursor-pointer group">
                    <span>📦</span>
                    <div className="flex-1 min-w-0">
                      <div className="font-bold truncate group-hover:text-purple-200">{p.name}</div>
                      {p.version && (
                        <div className="text-[9px] text-liberthia-300/50">v{p.version}</div>
                      )}
                    </div>
                    <span className="badge text-[9px]">{(p.sizeBytes / 1024 / 1024).toFixed(1)}MB</span>
                    <span className="text-[10px] text-purple-300/70 opacity-0 group-hover:opacity-100 transition-opacity">⬇</span>
                  </a>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Linha 3: community bugs + new items + changelog */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <CommunityBugsCard bugs={communityBugs} myMcName={me?.mcName} />
        <div className="card-glow">
          <h3 className="text-xs font-bold mb-2 gradient-text">🧪 Items novos pra testar</h3>
          {newestItems.length === 0 ? (
            <div className="text-[10px] text-liberthia-300/50 text-center py-4">nenhum item beta</div>
          ) : (
            <ul className="space-y-2 text-xs">
              {newestItems.map((i: any) => (
                <li key={i.id} className="flex items-center gap-2">
                  {i.imageUrl ? (
                    <img src={i.imageUrl} className="w-8 h-8 rounded object-cover" />
                  ) : (
                    <div className="w-8 h-8 rounded bg-purple-500/20 flex items-center justify-center text-base">
                      {i.kind === 'BLOCK' ? '🧱' : i.kind === 'ARTIFACT' ? '🏺' : i.kind === 'FEATURE' ? '⚙' : '🗡'}
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="font-bold truncate">{i.name}</div>
                    {i.itemId && (
                      <div className="text-[9px] font-mono text-liberthia-300/50 truncate">{i.itemId}</div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="card-glow">
          <h3 className="text-xs font-bold mb-2 gradient-text">📋 Changelog</h3>
          {(!changelogQ.data?.entries || changelogQ.data.entries.length === 0) ? (
            <div className="text-[10px] text-liberthia-300/50 text-center py-4">sem novidades</div>
          ) : (
            <ul className="space-y-2 text-xs">
              {/* Cada item é um botão clickável que abre o modal de detalhe
                  com TODOS os campos (items adicionados, bugs corrigidos,
                  buffs, debuffs, créditos, etc). Antes mostrava só título
                  + summary truncado e usuário não tinha como ver o resto
                  sem ir pra página /changelog. */}
              {changelogQ.data.entries.slice(0, 4).map((c: any) => (
                <li key={c.id}>
                  <button
                    onClick={() => setChangelogDetail(c)}
                    className="w-full text-left border-l-2 border-purple-500/40 pl-2 hover:border-purple-300 hover:bg-purple-500/5 rounded-r transition-colors py-0.5">
                    <div className="flex items-baseline gap-1">
                      <span className="badge badge-purple text-[8px]">{c.tag ?? 'update'}</span>
                      {c.version && <span className="text-[9px] text-liberthia-300/60">v{c.version}</span>}
                    </div>
                    <div className="font-bold truncate">{c.title}</div>
                    {c.summary && (
                      <div className="text-[9px] text-liberthia-300/60 line-clamp-2">{c.summary}</div>
                    )}
                    <div className="text-[8px] text-purple-300/70 mt-0.5">ver detalhes →</div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Bug Hunter Leaderboard — quem pegou mais bugs confirmados.
          Destaca a posição do tester atual (highlight no card dele).
          Card único de largura total porque é uma feature destacada. */}
      <BugHunterLeaderboard data={leaderboardQ.data} myMcName={me?.mcName} />

      {/* Banner de regras */}
      <div className="card !bg-amber-900/20 !border-amber-500/30 text-[10px] text-amber-200/90">
        <b>⚠ Lembrete NDA:</b> NÃO compartilhe o endereço do servidor de teste, gravações ou items beta.
        Banimento = perda de pontos, recompensas e acesso pra sempre.
      </div>

      {/* Modal de detalhe do changelog (renderizado fora do flow de cards) */}
      <ChangelogDetailModal entry={changelogDetail} onClose={() => setChangelogDetail(null)} />
    </div>
  )
}

/**
 * Bug Hunter Leaderboard — top caçadores de bug por bugs CONFIRMADOS.
 * Mostra top 10 + se o tester atual não estiver no top 10, mostra a posição
 * dele separada embaixo. Highlight visual no card do tester logado.
 */
function BugHunterLeaderboard({ data, myMcName }: { data: any; myMcName?: string }) {
  const ranking = data?.ranking ?? []
  if (ranking.length === 0) {
    return (
      <div className="card-glow">
        <h3 className="text-sm font-bold gradient-text mb-2">🏆 Bug Hunter Leaderboard</h3>
        <div className="text-[10px] text-liberthia-300/50 text-center py-4">
          Ninguém pegou bug ainda. Seja o primeiro!
        </div>
      </div>
    )
  }
  const top = ranking.slice(0, 10)
  const myEntry = myMcName
    ? ranking.find((r: any) => (r.mcName ?? '').toLowerCase() === myMcName.toLowerCase())
    : null
  const myIndex = myMcName
    ? ranking.findIndex((r: any) => (r.mcName ?? '').toLowerCase() === myMcName.toLowerCase())
    : -1
  const isMyInTop = myIndex >= 0 && myIndex < 10

  const totalBugs = data?.totalBugsConfirmed ?? 0
  const totalTesters = data?.totalTesters ?? ranking.length

  function medal(rank: number) {
    if (rank === 1) return '🥇'
    if (rank === 2) return '🥈'
    if (rank === 3) return '🥉'
    return `#${rank}`
  }

  return (
    <div className="card-glow">
      <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
        <h3 className="text-sm font-bold gradient-text">🏆 Bug Hunter Leaderboard</h3>
        <div className="text-[10px] text-liberthia-300/60">
          {totalBugs} bugs · {totalTesters} caçadores
        </div>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
        {top.map((r: any, idx: number) => {
          const rank = idx + 1
          const isMe = myMcName && (r.mcName ?? '').toLowerCase() === myMcName.toLowerCase()
          return (
            <div key={r.mcName ?? idx}
              className={`flex items-center gap-2 rounded p-1.5 ${
                isMe ? 'bg-purple-500/25 ring-1 ring-purple-400'
                     : 'bg-liberthia-900/30 hover:bg-liberthia-900/50'
              }`}>
              <span className={`font-bold text-xs w-8 text-center ${
                rank <= 3 ? 'text-base' : 'text-liberthia-300/70'
              }`}>{medal(rank)}</span>
              <img src={`https://mc-heads.net/avatar/${r.mcName}/24`}
                   className="w-6 h-6 rounded shadow shrink-0"
                   alt={r.mcName} />
              <div className="flex-1 min-w-0">
                <div className={`font-bold text-xs truncate ${isMe ? 'text-purple-200' : ''}`}>
                  {r.mcName}{isMe && ' (você)'}
                </div>
                {r.tier && (
                  <div className="text-[9px] text-liberthia-300/60">{r.tier}</div>
                )}
              </div>
              <div className="text-right shrink-0">
                <div className="text-xs font-bold text-amber-300">🐛 {r.bugsConfirmed ?? 0}</div>
                <div className="text-[9px] text-purple-300/70">⭐ {r.points ?? 0}</div>
              </div>
            </div>
          )
        })}
      </div>
      {/* Se o tester não tá no top 10, mostra a posição dele separada */}
      {myEntry && !isMyInTop && (
        <>
          <div className="text-center my-2 text-[9px] text-liberthia-300/40">— ⋯ —</div>
          <div className="flex items-center gap-2 rounded p-1.5 bg-purple-500/25 ring-1 ring-purple-400">
            <span className="font-bold text-xs w-8 text-center text-liberthia-300/70">
              #{myIndex + 1}
            </span>
            <img src={`https://mc-heads.net/avatar/${myEntry.mcName}/24`}
                 className="w-6 h-6 rounded shadow shrink-0" alt="" />
            <div className="flex-1 min-w-0">
              <div className="font-bold text-xs truncate text-purple-200">
                {myEntry.mcName} (você)
              </div>
              {myEntry.tier && (
                <div className="text-[9px] text-liberthia-300/60">{myEntry.tier}</div>
              )}
            </div>
            <div className="text-right shrink-0">
              <div className="text-xs font-bold text-amber-300">🐛 {myEntry.bugsConfirmed ?? 0}</div>
              <div className="text-[9px] text-purple-300/70">⭐ {myEntry.points ?? 0}</div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function ServerStatusWidget({ online, info }: { online: boolean; info: any }) {
  return (
    <div className={`card-glow border-l-4 ${online ? 'border-emerald-400' : 'border-red-500'}`}>
      <div className="flex items-center gap-2 mb-2">
        <span className={`w-2.5 h-2.5 rounded-full ${online ? 'bg-emerald-400 animate-pulse' : 'bg-red-500'}`} />
        <span className="font-bold text-xs">
          {online ? '🟢 Servidor ONLINE' : '🔴 Servidor offline'}
        </span>
      </div>
      {online && info?.serverAddress && (
        <div className="text-[10px] font-mono text-liberthia-300/70 truncate">{info.serverAddress}</div>
      )}
      {info?.mcVersion && (
        <div className="text-[10px] text-liberthia-300/60 mt-1">
          MC <b>{info.mcVersion}</b> · mod <b>{info.modVersion ?? '?'}</b>
        </div>
      )}
      {info?.updatedAt && (
        <div className="text-[9px] text-liberthia-300/40 mt-1">
          atualizado {new Date(info.updatedAt).toLocaleTimeString('pt-BR')}
        </div>
      )}
    </div>
  )
}

function CommunityBugsCard({ bugs, myMcName }: { bugs: any[]; myMcName?: string }) {
  const qc = useQueryClient()
  // Bug aberto no modal de detalhe. Click no card abre, ESC ou ✕ fecha.
  const [selected, setSelected] = useState<BugLike | null>(null)

  async function confirmReplication(id: number, mcOfReporter: string) {
    if (mcOfReporter === myMcName) {
      alert('Você mesmo reportou esse bug')
      return
    }
    try {
      await testerApi.confirmBugReplication(id)
      qc.invalidateQueries({ queryKey: ['tester-community-bugs'] })
      // Fecha o modal já que a lista vai refetchar
      setSelected(null)
    } catch (e: any) { alert(e.message) }
  }

  const others = bugs.filter(b => b.testerMcName !== myMcName).slice(0, 5)

  return (
    <>
      <div className="card-glow !border-amber-500/30">
        <h3 className="text-xs font-bold mb-2 gradient-text">👥 Bugs da comunidade</h3>
        <div className="text-[10px] text-liberthia-300/60 mb-2">
          Clica num bug pra ver detalhes e confirmar se você reproduziu
        </div>
        {others.length === 0 ? (
          <div className="text-[10px] text-liberthia-300/50 text-center py-4">
            nenhum bug pendente
          </div>
        ) : (
          <ul className="space-y-2 text-xs">
            {others.map(b => (
              <li key={b.id}>
                <button
                  onClick={() => setSelected(b)}
                  className="w-full text-left rounded bg-liberthia-900/40 hover:bg-purple-500/15 p-2 transition-colors">
                  <div className="flex items-start gap-1 flex-wrap mb-1">
                    <span className="badge text-[8px]">{b.severity ?? 'med'}</span>
                    {b.replicationCount > 0 && (
                      <span className="badge badge-purple text-[8px]">+{b.replicationCount}</span>
                    )}
                    <div className="text-[9px] text-liberthia-300/50 ml-auto">por {b.testerMcName}</div>
                  </div>
                  <div className="font-bold truncate text-[11px]">{b.title}</div>
                  {b.itemId && (
                    <div className="text-[9px] font-mono text-liberthia-300/50 truncate">{b.itemId}</div>
                  )}
                  <div className="text-[9px] text-purple-300/70 mt-1">ver detalhes →</div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
      <BugDetailModal
        bug={selected}
        onClose={() => setSelected(null)}
        onConfirmReplication={selected && selected.testerMcName !== myMcName
          ? () => confirmReplication(selected.id, selected.testerMcName ?? '')
          : undefined}
      />
    </>
  )
}

function Stat({ label, value, color, icon, sub }:
  { label: string; value: number; color: string; icon?: string; sub?: string }) {
  const colorMap: Record<string, string> = {
    purple: 'text-purple-300', amber: 'text-amber-300', emerald: 'text-emerald-300', red: 'text-red-300',
  }
  return (
    <div className="card-glow">
      <div className="text-[10px] uppercase text-liberthia-300/50">{label}</div>
      <div className={`text-3xl font-bold mt-1 ${colorMap[color]}`}>
        {icon && <span className="text-xl mr-1">{icon}</span>}{value}
      </div>
      {sub && <div className="text-[10px] text-liberthia-300/50 mt-1">{sub}</div>}
    </div>
  )
}

// ============ Downloads ============
function DownloadsTab() {
  const q = useQuery({ queryKey: ['tester-packages'], queryFn: testerApi.packages, refetchInterval: 30000 })

  /**
   * Download via anchor direto — browser streama o ZIP pro disco sem alocar
   * buffer/blob em memória. Antes a função usava `fetch() + blob()` que:
   *   1. Quebrava com "Failed to fetch" em packs > ~100MB (browser não
   *      conseguia montar o Blob inteiro na heap).
   *   2. Não mostrava progresso do download — usuário ficava com botão
   *      "⏳ baixando..." travado por minutos sem feedback.
   *   3. Hold do token em URL agora é fácil pq o backend aceita ?token=
   *      como fallback do Bearer (TesterContentController.requireTester).
   */
  function download(id: number, filename: string) {
    const url = testerApi.packageDownloadUrl(id)
    const a = document.createElement('a')
    a.href = url
    a.download = filename || `pacote-${id}.zip`
    a.rel = 'noopener'
    document.body.appendChild(a)
    a.click()
    a.remove()
  }

  if (q.isLoading) return <div className="text-xs text-liberthia-300/60">Carregando...</div>
  const pkgs = q.data?.packages ?? []
  if (pkgs.length === 0) {
    return <div className="card-glow text-center py-12 text-liberthia-300/60">
      <div className="text-5xl mb-2 opacity-50">📦</div>
      <p>Nenhum pacote disponível ainda.</p>
    </div>
  }

  // Agrupa por nome — cada grupo lista todas as versões com a mais recente em destaque
  const groups: Record<string, any[]> = {}
  for (const p of pkgs) {
    const key = (p.name ?? 'sem nome').trim()
    if (!groups[key]) groups[key] = []
    groups[key].push(p)
  }
  // Ordena versões dentro de cada grupo (mais recente primeiro — usa uploadedAt do backend)
  Object.values(groups).forEach(list => list.sort((a, b) => {
    const ta = new Date(a.uploadedAt ?? 0).getTime()
    const tb = new Date(b.uploadedAt ?? 0).getTime()
    return tb - ta
  }))

  const groupList = Object.entries(groups).sort(([a], [b]) => a.localeCompare(b))

  return (
    <div className="space-y-4">
      {groupList.map(([groupName, versions]) => {
        const latest = versions[0]
        return (
          <div key={groupName} className="card-glow">
            <div className="flex items-center gap-2 mb-3 flex-wrap">
              <div className="text-3xl">📦</div>
              <div className="flex-1 min-w-0">
                <h3 className="font-bold gradient-text">{groupName}</h3>
                <div className="text-[10px] text-liberthia-300/60">
                  {versions.length} versão{versions.length > 1 ? 'ões' : ''} disponí{versions.length > 1 ? 'veis' : 'vel'}
                </div>
              </div>
              {versions.length > 1 && (
                <span className="badge badge-purple text-[10px]">⬇ último: v{latest.version ?? '?'}</span>
              )}
            </div>

            <div className="space-y-2">
              {versions.map((p: any, idx: number) => (
                <div key={p.id}
                  className={`rounded p-2 ${idx === 0 ? 'bg-purple-500/15 border border-purple-400/40' : 'bg-liberthia-900/40'}`}>
                  <div className="flex items-center gap-2 flex-wrap">
                    {p.version ? (
                      <span className={`badge text-[10px] ${idx === 0 ? 'badge-green' : ''}`}>
                        v{p.version} {idx === 0 && '· latest'}
                      </span>
                    ) : (
                      <span className="badge text-[10px]">sem versão</span>
                    )}
                    <div className="text-[10px] text-liberthia-300/60 font-mono flex-1 min-w-0 truncate">
                      {p.filename} · {fmtBytes(p.sizeBytes)} · ⬇ {p.downloadCount}×
                    </div>
                    {/* v0.1.27: backend já filtra os packages fantasmas
                        (fileExists=false) pro tester, então sempre que
                        chegar aqui o arquivo está disponível. */}
                    <button
                      className="btn btn-sm text-[10px]"
                      onClick={() => download(p.id, p.filename)}>
                      📥 Download
                    </button>
                  </div>
                  {p.description && (
                    <p className="text-xs text-liberthia-300/80 mt-2">{p.description}</p>
                  )}
                  {p.uploadedAt && (
                    <div className="text-[9px] text-liberthia-300/40 mt-1">
                      enviado em {new Date(p.uploadedAt).toLocaleString('pt-BR')}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ============ Beta Items ============
function BetaItemsTab({ onReport: _onReport }: { onReport: (id: number) => void }) {
  const q = useQuery({ queryKey: ['tester-beta-items'], queryFn: testerApi.betaItems })
  // Search filtra por nome/itemId/descrição/category. Trim + lower no
  // useMemo pra não recomputar a cada render. Com 280+ items beta, scan
  // O(n) na input do search ainda é instantâneo (<1ms), mas o useMemo
  // evita re-render dos cards quando o usuário digita rápido.
  const [search, setSearch] = useState('')
  const [kindFilter, setKindFilter] = useState<'ALL' | 'ITEM' | 'BLOCK' | 'FEATURE' | 'ARTIFACT'>('ALL')

  const allItems = q.data?.items ?? []

  const filtered = useMemo(() => {
    let list = allItems
    if (kindFilter !== 'ALL') {
      list = list.filter((i: any) => (i.kind ?? 'ITEM') === kindFilter)
    }
    const term = search.trim().toLowerCase()
    if (term) {
      list = list.filter((i: any) => {
        // Multi-field search: nome, itemId, descrição, category, giveCommand
        // (giveCommand inclui o ID do item então um search por "sword" também
        // pega items que dão sword via comando)
        const haystack = [
          i.name, i.itemId, i.description, i.category, i.giveCommand,
        ].filter(Boolean).join(' ').toLowerCase()
        return haystack.includes(term)
      })
    }
    return list
  }, [allItems, search, kindFilter])

  const kindCounts = useMemo(() => {
    const c: Record<string, number> = { ALL: allItems.length }
    for (const i of allItems) {
      const k = i.kind ?? 'ITEM'
      c[k] = (c[k] ?? 0) + 1
    }
    return c
  }, [allItems])

  if (q.isLoading) return <div className="text-xs text-liberthia-300/60">Carregando...</div>
  if (q.isError) {
    return <div className="card-glow text-center py-12 text-red-300/80">
      <div className="text-5xl mb-2 opacity-50">⚠</div>
      <p className="font-bold">Falha ao carregar items beta</p>
      <p className="text-xs mt-1">{(q.error as any)?.message ?? 'Erro de rede'}</p>
    </div>
  }
  // Hint do backend (v106): se totalInDb > 0 mas items vazio = items
  // existem no DB mas todos com enabled=false. Usuário precisa pedir ao
  // admin pra usar o "Ligar todos" no painel admin.
  const totalInDb = Number((q.data as any)?.totalInDb ?? allItems.length)
  if (allItems.length === 0) {
    return <div className="card-glow text-center py-12 text-liberthia-300/60">
      <div className="text-5xl mb-2 opacity-50">🧪</div>
      {totalInDb > 0 ? (
        <>
          <p className="font-bold text-amber-300">
            {totalInDb} item{totalInDb > 1 ? 's' : ''} cadastrado{totalInDb > 1 ? 's' : ''} — todos desativados
          </p>
          <p className="text-xs mt-2">O admin precisa ativar os items no painel pra você ver aqui.</p>
        </>
      ) : (
        <p>Nenhum item beta cadastrado ainda.</p>
      )}
    </div>
  }

  const KIND_ICONS: Record<string, string> = {
    ITEM: '🗡', BLOCK: '🧱', FEATURE: '⚙', ARTIFACT: '🏺',
  }

  return (
    <div className="space-y-3">
      {/* Search bar + filtros de kind. Sticky no topo pra ficar acessível
          enquanto o usuário rola a lista (que pode ter 280+ cards). */}
      <div className="sticky top-[68px] z-10 -mx-3 sm:-mx-4 px-3 sm:px-4 py-2 bg-liberthia-900/95 backdrop-blur border-b border-purple-500/20 space-y-2">
        <div className="relative">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="🔍 Buscar por nome, ID, descrição..."
            className="input text-sm pr-8"
            autoComplete="off"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-liberthia-300/60 hover:text-white text-xs"
              title="Limpar busca">
              ✕
            </button>
          )}
        </div>
        <div className="flex items-center gap-1 flex-wrap">
          {(['ALL', 'ITEM', 'BLOCK', 'FEATURE', 'ARTIFACT'] as const).map(k => (
            <button key={k}
              onClick={() => setKindFilter(k)}
              className={`btn-ghost btn-sm text-[10px] ${
                kindFilter === k ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''
              }`}>
              {k === 'ALL' ? '📋' : KIND_ICONS[k]} {k === 'ALL' ? 'Todos' : k.toLowerCase()}{' '}
              <span className="opacity-60">({kindCounts[k] ?? 0})</span>
            </button>
          ))}
          <span className="text-[10px] text-liberthia-300/60 ml-auto">
            {filtered.length} de {allItems.length}
          </span>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="card-glow text-center py-8 text-liberthia-300/60">
          <div className="text-3xl mb-1 opacity-50">🔍</div>
          <p className="text-xs">Nenhum item bate com a busca "{search}"</p>
          {kindFilter !== 'ALL' && (
            <p className="text-[10px] mt-1">Tipo: {kindFilter.toLowerCase()}</p>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {filtered.map((i: any) => {
        let props: any = null
        try { props = i.propertiesJson ? JSON.parse(i.propertiesJson) : null } catch {}
        return (
          <div key={i.id} className="card-glow flex flex-col">
            {i.imageUrl && (
              <img src={i.imageUrl} alt={i.name} className="w-full h-32 object-cover rounded mb-2" />
            )}
            <div className="flex items-start gap-2 mb-2">
              <div className="flex-1 min-w-0">
                <h3 className="font-bold gradient-text truncate">{i.name}</h3>
                {i.category && <span className="badge text-[10px]">{i.category}</span>}
              </div>
            </div>
            {i.itemId && (
              <div className="text-[10px] font-mono text-liberthia-300/50 mb-1">{i.itemId}</div>
            )}
            {i.description && <p className="text-xs text-liberthia-300/80 mb-2">{i.description}</p>}
            {props && (
              <div className="text-[10px] bg-liberthia-900/40 rounded p-2 mb-2 space-y-0.5">
                {Object.entries(props).map(([k, v]) => (
                  <div key={k} className="flex justify-between">
                    <span className="text-liberthia-300/60">{k}</span>
                    <b className="text-purple-300">{String(v)}</b>
                  </div>
                ))}
              </div>
            )}
            {i.giveCommand && (
              <div className="text-[10px] font-mono bg-liberthia-900/60 rounded p-2 mb-2 break-all">
                {i.giveCommand}
              </div>
            )}
            <div className="mt-auto pt-2 flex items-center justify-between gap-2">
              <VoteButtonsLazy
                targetType="BETA_ITEM"
                targetId={i.id}
                counts={{
                  likes: Number(i.votes?.likes ?? 0),
                  dislikes: Number(i.votes?.dislikes ?? 0),
                  myVote: (i.votes?.myVote ?? null) as any,
                }}
                size="sm"
              />
            </div>
          </div>
        )
      })}
        </div>
      )}
    </div>
  )
}

/**
 * Wrapper que lazy-loads VoteButtons. Evita pull do componente em telas
 * que não usam votação. (Pequeno mas vale a granularidade no code-split.)
 */
function VoteButtonsLazy(props: any) {
  const [Cmp, setCmp] = useState<any>(null)
  useEffect(() => {
    import('../../components/VoteButtons').then(m => setCmp(() => m.VoteButtons))
  }, [])
  if (!Cmp) {
    return (
      <div className="flex items-center gap-1 opacity-40 text-xs text-slate-400">
        <span>👍 {props.counts?.likes ?? 0}</span>
        <span>👎 {props.counts?.dislikes ?? 0}</span>
      </div>
    )
  }
  return <Cmp {...props} />
}

// ============ Bugs ============
function BugsTab() {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  // ID do bug em edição. null = criando novo, número = editando existente.
  // Mesmo form é reusado pra create vs edit — diferencia pelo editingId no submit.
  const [editingId, setEditingId] = useState<number | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [steps, setSteps] = useState('')
  const [severity, setSeverity] = useState<'low' | 'medium' | 'high' | 'critical'>('medium')
  const [betaItemId, setBetaItemId] = useState<number | ''>('')
  const [itemId, setItemId] = useState('')
  const [howFound, setHowFound] = useState('')
  const [modVersion, setModVersion] = useState('v96')
  const [mcVersion, setMcVersion] = useState('1.20.1')
  const [worldContext, setWorldContext] = useState('overworld')
  const [screenshotUrl, setScreenshotUrl] = useState('')
  // v96: triage detalhada
  const [frequency, setFrequency] = useState<'always' | 'often' | 'rare' | 'once'>('often')
  const [priority, setPriority] = useState<'low' | 'medium' | 'high' | 'blocker'>('medium')
  const [canReplicate, setCanReplicate] = useState(true)
  const [affectsOthers, setAffectsOthers] = useState(false)
  const [expectedBehavior, setExpectedBehavior] = useState('')
  const [workaround, setWorkaround] = useState('')
  const [tags, setTags] = useState('')
  const [busy, setBusy] = useState(false)

  const myBugsQ = useQuery({ queryKey: ['tester-bugs-mine'], queryFn: testerApi.myBugs, refetchInterval: 10000 })
  const itemsQ = useQuery({ queryKey: ['tester-beta-items'], queryFn: testerApi.betaItems })

  // Modal de detalhe (click no card → abre com tudo do bug)
  const [selected, setSelected] = useState<BugLike | null>(null)
  // Filtro por status — testers querem ver histórico do que foi confirmado.
  // 'all' default mostra tudo, igual ao admin agora.
  const [statusFilter, setStatusFilter] = useState<'all' | 'PENDING' | 'CONFIRMED' | 'REJECTED'>('all')

  function resetForm() {
    setEditingId(null)
    setTitle(''); setDescription(''); setSteps(''); setBetaItemId('')
    setItemId(''); setHowFound(''); setScreenshotUrl('')
    setSeverity('medium')
    setModVersion('v96'); setMcVersion('1.20.1'); setWorldContext('overworld')
    setFrequency('often'); setPriority('medium')
    setCanReplicate(true); setAffectsOthers(false)
    setExpectedBehavior(''); setWorkaround(''); setTags('')
  }

  /**
   * Popula o form com os dados de um bug pra editar. Aceita o objeto bug
   * direto do backend. Só faz sentido chamar isso pra bugs PENDING — o backend
   * recusa edit em bugs já triados (CONFIRMED/REJECTED).
   */
  function openEdit(b: any) {
    setEditingId(b.id)
    setTitle(b.title ?? '')
    setDescription(b.description ?? '')
    setSteps(b.stepsToReproduce ?? '')
    setSeverity((b.severity ?? 'medium') as any)
    setBetaItemId(b.betaItemId ?? '')
    setItemId(b.itemId ?? '')
    setHowFound(b.howFound ?? '')
    setModVersion(b.modVersion ?? 'v96')
    setMcVersion(b.mcVersion ?? '1.20.1')
    setWorldContext(b.worldContext ?? 'overworld')
    setScreenshotUrl(b.screenshotUrl ?? '')
    setFrequency((b.frequency ?? 'often') as any)
    setPriority((b.priority ?? 'medium') as any)
    setCanReplicate(b.canReplicate ?? true)
    setAffectsOthers(b.affectsOthers ?? false)
    setExpectedBehavior(b.expectedBehavior ?? '')
    setWorkaround(b.workaround ?? '')
    setTags(b.tags ?? '')
    setShowForm(true)
    setSelected(null) // fecha modal de detalhe se estava aberto
    // Scroll suave pro topo (form fica no topo da tab)
    if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function deleteBug(id: number) {
    if (!confirm('Deletar esse bug? Não dá pra desfazer.')) return
    try {
      await testerApi.deleteBug(id)
      qc.invalidateQueries({ queryKey: ['tester-bugs-mine'] })
      setSelected(null)
    } catch (e: any) { alert(e.message) }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!title.trim() || !description.trim()) return
    setBusy(true)
    try {
      const payload = {
        betaItemId: betaItemId === '' ? undefined : Number(betaItemId),
        title: title.trim(),
        description: description.trim(),
        stepsToReproduce: steps.trim() || undefined,
        severity,
        itemId: itemId.trim() || undefined,
        howFound: howFound.trim() || undefined,
        modVersion: modVersion.trim() || undefined,
        mcVersion: mcVersion.trim() || undefined,
        worldContext: worldContext.trim() || undefined,
        screenshotUrl: screenshotUrl.trim() || undefined,
        frequency, priority, canReplicate, affectsOthers,
        expectedBehavior: expectedBehavior.trim() || undefined,
        workaround: workaround.trim() || undefined,
        tags: tags.trim() || undefined,
      }
      if (editingId !== null) {
        // PATCH em vez de POST — backend recusa se status != PENDING ou se
        // o bug não pertence ao tester logado.
        await testerApi.updateBug(editingId, payload)
      } else {
        await testerApi.createBug(payload as any)
      }
      resetForm()
      setShowForm(false)
      qc.invalidateQueries({ queryKey: ['tester-bugs-mine'] })
    } catch (e: any) { alert(e.message) }
    finally { setBusy(false) }
  }

  const statusColor: Record<string, string> = {
    PENDING: 'badge-yellow', CONFIRMED: 'badge-green', REJECTED: 'badge-red', DUPLICATE: '',
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-base font-bold gradient-text">🐛 Meus bug reports</h2>
        <button className="btn btn-sm" onClick={() => {
          if (showForm) {
            resetForm()
            setShowForm(false)
          } else {
            resetForm()
            setShowForm(true)
          }
        }}>
          {showForm ? '✕ Cancelar' : '+ Novo bug'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={submit} className="card-glow space-y-2">
          {/* Indicador de modo edição — feedback claro pro user que ele tá
              editando um bug existente, não criando outro novo. */}
          {editingId !== null && (
            <div className="rounded bg-amber-900/30 border border-amber-500/40 px-2 py-1.5 text-[11px] text-amber-200">
              ✏ Editando bug <b>#{editingId}</b> — só funciona enquanto status=PENDING.
              Depois que admin triar, esse form bloqueia.
            </div>
          )}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <div>
              <label className="label">Item beta (opcional)</label>
              <select className="input text-xs" value={betaItemId}
                onChange={(e) => setBetaItemId(e.target.value === '' ? '' : Number(e.target.value))}>
                <option value="">— nenhum / geral —</option>
                {(itemsQ.data?.items ?? []).map((i: any) => (
                  <option key={i.id} value={i.id}>{i.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">ID do item (texto livre)</label>
              <input className="input font-mono text-xs" value={itemId}
                onChange={(e) => setItemId(e.target.value)}
                placeholder="liberthia:dark_matter_sword" maxLength={256} />
            </div>
          </div>
          <div>
            <label className="label">Título</label>
            <input className="input" value={title} onChange={(e) => setTitle(e.target.value)}
              placeholder="ex: Matter Analyzer crasha ao colocar cobblestone"
              required maxLength={256} />
          </div>
          <div>
            <label className="label">Descrição</label>
            <textarea className="input" rows={3} value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Descreva o bug em detalhe..." required />
          </div>
          <div>
            <label className="label">🔍 Como você encontrou? (simulação)</label>
            <textarea className="input text-xs" rows={2} value={howFound}
              onChange={(e) => setHowFound(e.target.value)}
              placeholder="Ex: tava testando combate vs zombie e equipei a espada quando..." />
          </div>
          <div>
            <label className="label">Passos pra reproduzir</label>
            <textarea className="input" rows={3} value={steps}
              onChange={(e) => setSteps(e.target.value)}
              placeholder="1. Coloca o bloco...&#10;2. Insere o item X...&#10;3. Aparece o erro Y" />
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <div>
              <label className="label text-[10px]">Versão mod</label>
              <input className="input text-xs" value={modVersion}
                onChange={(e) => setModVersion(e.target.value)} placeholder="v90" maxLength={32} />
            </div>
            <div>
              <label className="label text-[10px]">Versão MC</label>
              <input className="input text-xs" value={mcVersion}
                onChange={(e) => setMcVersion(e.target.value)} placeholder="1.20.1" maxLength={32} />
            </div>
            <div>
              <label className="label text-[10px]">Dimensão</label>
              <select className="input text-xs" value={worldContext}
                onChange={(e) => setWorldContext(e.target.value)}>
                <option value="overworld">overworld</option>
                <option value="nether">nether</option>
                <option value="end">end</option>
                <option value="custom">custom</option>
              </select>
            </div>
            <div>
              <label className="label text-[10px]">Severidade</label>
              <select className="input text-xs" value={severity} onChange={(e) => setSeverity(e.target.value as any)}>
                <option value="low">🟢 Baixa</option>
                <option value="medium">🟡 Média</option>
                <option value="high">🟠 Alta</option>
                <option value="critical">🔴 Crítica</option>
              </select>
            </div>
          </div>
          <div>
            <label className="label text-[10px]">📸 URL screenshot/vídeo (imgur, youtube, etc)</label>
            <input className="input text-xs" value={screenshotUrl}
              onChange={(e) => setScreenshotUrl(e.target.value)}
              placeholder="https://i.imgur.com/..." />
          </div>

          {/* v96: triage detalhada */}
          <details className="rounded bg-liberthia-900/30 p-2">
            <summary className="cursor-pointer text-xs font-bold text-purple-300">
              🔬 Triagem detalhada (recomendado)
            </summary>
            <div className="space-y-2 mt-2">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="label text-[10px]">Frequência</label>
                  <select className="input text-xs" value={frequency}
                    onChange={(e) => setFrequency(e.target.value as any)}>
                    <option value="always">🔴 Sempre acontece</option>
                    <option value="often">🟠 Frequente</option>
                    <option value="rare">🟡 Raro</option>
                    <option value="once">⚪ Aconteceu 1× só</option>
                  </select>
                </div>
                <div>
                  <label className="label text-[10px]">Prioridade sugerida</label>
                  <select className="input text-xs" value={priority}
                    onChange={(e) => setPriority(e.target.value as any)}>
                    <option value="low">🟢 Baixa</option>
                    <option value="medium">🟡 Média</option>
                    <option value="high">🟠 Alta</option>
                    <option value="blocker">🔴 Bloqueia jogo</option>
                  </select>
                </div>
              </div>
              <div className="flex flex-wrap gap-3">
                <label className="text-xs flex items-center gap-1">
                  <input type="checkbox" checked={canReplicate}
                    onChange={(e) => setCanReplicate(e.target.checked)} />
                  Consigo reproduzir consistentemente
                </label>
                <label className="text-xs flex items-center gap-1">
                  <input type="checkbox" checked={affectsOthers}
                    onChange={(e) => setAffectsOthers(e.target.checked)} />
                  Afeta outros players (não só eu)
                </label>
              </div>
              <div>
                <label className="label text-[10px]">✅ Comportamento esperado</label>
                <textarea className="input text-xs" rows={2}
                  placeholder="O que DEVERIA acontecer em vez do bug"
                  value={expectedBehavior}
                  onChange={(e) => setExpectedBehavior(e.target.value)} />
              </div>
              <div>
                <label className="label text-[10px]">🩹 Workaround (se descobriu)</label>
                <textarea className="input text-xs" rows={2}
                  placeholder="Tem um jeito de contornar enquanto não corrige?"
                  value={workaround}
                  onChange={(e) => setWorkaround(e.target.value)} />
              </div>
              <div>
                <label className="label text-[10px]">🏷 Tags (separadas por vírgula)</label>
                <input className="input text-xs"
                  placeholder="ex: crash, combat, multiplayer, performance"
                  value={tags}
                  onChange={(e) => setTags(e.target.value)} />
              </div>
            </div>
          </details>

          <button type="submit" className="btn w-full" disabled={busy}>
            {busy ? '...' : (editingId !== null ? '💾 Salvar edição' : '📤 Enviar report')}
          </button>
        </form>
      )}

      {/* Filtros — mantém histórico visível por default (incluindo confirmados). */}
      <div className="flex gap-1 flex-wrap items-center">
        <span className="text-[10px] text-liberthia-300/60 mr-1">Status:</span>
        {(['all', 'PENDING', 'CONFIRMED', 'REJECTED'] as const).map(s => {
          const count = s === 'all'
            ? (myBugsQ.data?.bugs?.length ?? 0)
            : (myBugsQ.data?.bugs?.filter((b: any) => b.status === s).length ?? 0)
          return (
            <button key={s} onClick={() => setStatusFilter(s)}
              className={`btn-ghost btn-sm text-[10px] ${statusFilter === s ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
              {s === 'all' ? 'Todos' : s === 'PENDING' ? '⏳ Pend' : s === 'CONFIRMED' ? '✅ Conf' : '❌ Rejeit'}
              {' '}({count})
            </button>
          )
        })}
      </div>

      <div className="space-y-2">
        {(() => {
          const allBugs = myBugsQ.data?.bugs ?? []
          const filtered = statusFilter === 'all' ? allBugs : allBugs.filter((b: any) => b.status === statusFilter)
          if (allBugs.length === 0) {
            return (
              <div className="card text-center py-8 text-liberthia-300/50 text-xs">
                Nenhum bug reportado ainda. Encontrou um? Clique em "+ Novo bug".
              </div>
            )
          }
          if (filtered.length === 0) {
            return (
              <div className="card text-center py-8 text-liberthia-300/50 text-xs">
                Nenhum bug com status "{statusFilter}".
              </div>
            )
          }
          return filtered.map((b: any) => {
            // Bugs PENDING podem ser editados/deletados pelo dono. Depois que
            // admin triou (CONFIRMED/REJECTED), vira read-only — preserva o
            // que ele já julgou + os pontos dados.
            const isEditable = b.status === 'PENDING'
            return (
              <div
                key={b.id}
                onClick={() => setSelected(b)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => { if (e.key === 'Enter') setSelected(b) }}
                className="cursor-pointer card-glow hover:!border-purple-400/60 transition-colors">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-bold text-sm truncate">{b.title}</h3>
                    <div className="text-[10px] text-liberthia-300/50 mt-1">
                      #{b.id} · {new Date(b.createdAt).toLocaleString('pt-BR')}
                      {b.severity && <span className="ml-2 badge text-[9px]">{b.severity}</span>}
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
                <div className="flex items-center justify-between mt-2 gap-2">
                  <div className="text-[9px] text-purple-300/70">ver detalhes →</div>
                  {/* Botões editar/deletar só pra PENDING (próprio bug, ainda não triado).
                      stopPropagation pra não disparar o setSelected do card pai. */}
                  {isEditable && (
                    <div className="flex gap-1">
                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); openEdit(b) }}
                        className="btn-ghost btn-sm text-[10px] py-0.5"
                        title="Editar esse bug (só enquanto PENDING)">
                        ✏ Editar
                      </button>
                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); deleteBug(b.id) }}
                        className="btn-ghost btn-sm text-[10px] py-0.5 text-red-300 hover:bg-red-500/10"
                        title="Deletar (só enquanto PENDING)">
                        🗑
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )
          })
        })()}
      </div>

      <BugDetailModal bug={selected} onClose={() => setSelected(null)} />
    </div>
  )
}

// ============ Rewards ============
function RewardsTab({ me }: { me?: TesterDto }) {
  const qc = useQueryClient()
  const rewardsQ = useQuery({ queryKey: ['tester-rewards'], queryFn: testerApi.rewards })
  const myRedemptQ = useQuery({ queryKey: ['tester-redemptions-mine'], queryFn: testerApi.myRedemptions })

  async function redeem(id: number, name: string, cost: number) {
    if (!confirm(`Resgatar "${name}" por ${cost} pontos?`)) return
    try {
      await testerApi.redeem(id)
      qc.invalidateQueries({ queryKey: ['tester-rewards'] })
      qc.invalidateQueries({ queryKey: ['tester-redemptions-mine'] })
      qc.invalidateQueries({ queryKey: ['tester-me'] })
      alert('🎉 Resgatado! Aguarde o admin entregar in-game.')
    } catch (e: any) { alert(e.message) }
  }

  const points = me?.points ?? 0

  return (
    <div className="space-y-4">
      <section>
        <h2 className="text-base font-bold gradient-text mb-3">🎁 Catálogo</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {rewardsQ.data?.rewards?.length === 0 && (
            <div className="card text-center py-8 text-liberthia-300/50 text-xs col-span-full">
              Nenhuma recompensa cadastrada ainda.
            </div>
          )}
          {rewardsQ.data?.rewards?.map((r: any) => {
            const canAfford = points >= r.costPoints
            return (
              <div key={r.id} className={`card-glow flex flex-col ${!canAfford ? 'opacity-60' : ''}`}>
                {r.imageUrl && <img src={r.imageUrl} alt={r.name} className="w-full h-28 object-cover rounded mb-2" />}
                <h3 className="font-bold gradient-text">{r.name}</h3>
                {r.category && <span className="badge text-[10px] mb-1 inline-block">{r.category}</span>}
                {r.description && <p className="text-xs text-liberthia-300/80 my-2 flex-1">{r.description}</p>}
                <div className="flex items-center justify-between mt-2">
                  <span className="text-purple-300 font-bold">⭐ {r.costPoints}</span>
                  <button className="btn btn-sm" disabled={!canAfford}
                    onClick={() => redeem(r.id, r.name, r.costPoints)}>
                    {canAfford ? 'Resgatar' : 'Insuficiente'}
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      </section>

      <section>
        <h2 className="text-base font-bold gradient-text mb-3">📜 Meus resgates</h2>
        <div className="space-y-2">
          {myRedemptQ.data?.redemptions?.length === 0 && (
            <div className="card text-center py-6 text-liberthia-300/50 text-xs">
              Nenhum resgate ainda.
            </div>
          )}
          {myRedemptQ.data?.redemptions?.map((r: any) => (
            <div key={r.id} className="card flex items-center gap-3 text-xs">
              <div className="text-2xl">{r.delivered ? '✅' : '⏳'}</div>
              <div className="flex-1 min-w-0">
                <div className="font-bold">{r.rewardSnapshot}</div>
                <div className="text-[10px] text-liberthia-300/60">
                  {new Date(r.redeemedAt).toLocaleString()} · −{r.pointsSpent} pts
                  {r.delivered ? ` · entregue ${new Date(r.deliveredAt).toLocaleString()}` : ' · aguardando admin'}
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}

// ============ Server Info ============
function ServerTab() {
  const q = useQuery({ queryKey: ['tester-server-info'], queryFn: testerApi.serverInfo, refetchInterval: 30000 })
  const [copied, setCopied] = useState(false)

  if (q.isLoading) return <div className="text-xs text-liberthia-300/60">Carregando...</div>
  const info = q.data ?? {}

  function copyAddress() {
    if (!info.serverAddress) return
    navigator.clipboard.writeText(info.serverAddress)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="space-y-4 max-w-3xl mx-auto">
      {/* NDA — bem visível */}
      <div className="card !bg-red-900/30 !border-red-500/50">
        <h3 className="font-bold text-red-300 mb-2 text-sm">⚠ NDA / Termos de uso</h3>
        <p className="text-xs text-red-200/90 whitespace-pre-wrap">
          {info.ndaText || 'Carregando...'}
        </p>
      </div>

      {/* Status servidor */}
      <div className="card-glow">
        <div className="flex items-center gap-3 mb-3">
          <span className={`w-3 h-3 rounded-full ${info.active ? 'bg-emerald-400 animate-pulse' : 'bg-red-500'}`} />
          <h2 className="font-bold gradient-text">
            {info.active ? '🟢 Servidor ONLINE' : '🔴 Servidor OFFLINE'}
          </h2>
        </div>

        {info.serverAddress ? (
          <div className="space-y-3 text-xs">
            <div>
              <label className="label text-[10px]">Endereço do servidor</label>
              <div className="flex gap-2">
                <input className="input font-mono text-sm flex-1" value={info.serverAddress} readOnly
                  onClick={(e) => (e.target as HTMLInputElement).select()} />
                <button className="btn btn-sm" onClick={copyAddress}>
                  {copied ? '✓ Copiado' : '📋 Copiar'}
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              {info.mcVersion && (
                <div>
                  <label className="label text-[10px]">Versão MC</label>
                  <div className="font-mono text-sm bg-liberthia-900/40 rounded p-2">{info.mcVersion}</div>
                </div>
              )}
              {info.modVersion && (
                <div>
                  <label className="label text-[10px]">Versão dos mods</label>
                  <div className="font-mono text-sm bg-liberthia-900/40 rounded p-2">{info.modVersion}</div>
                </div>
              )}
            </div>

            {info.connectionNotes && (
              <div>
                <label className="label text-[10px]">Instruções de conexão</label>
                <div className="bg-liberthia-900/40 rounded p-2 whitespace-pre-wrap">{info.connectionNotes}</div>
              </div>
            )}

            {info.voiceChatInfo && (
              <div>
                <label className="label text-[10px]">Voice Chat</label>
                <div className="bg-liberthia-900/40 rounded p-2 text-[11px]">{info.voiceChatInfo}</div>
              </div>
            )}

            {info.discordLink && (
              <a href={info.discordLink} target="_blank" rel="noreferrer"
                className="btn-ghost w-full text-center block">💬 Discord do beta →</a>
            )}
          </div>
        ) : (
          <div className="text-xs text-liberthia-300/60 italic">
            Servidor ainda não configurado. Aguarde o admin.
          </div>
        )}

        {info.updatedAt && (
          <div className="text-[10px] text-liberthia-300/50 mt-3">
            atualizado {new Date(info.updatedAt).toLocaleString()} por {info.updatedBy}
          </div>
        )}
      </div>
    </div>
  )
}

// ============ Suggestions ============
function SuggestionsTab() {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [filter, setFilter] = useState<'all' | 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'IMPLEMENTED'>('all')
  const [form, setForm] = useState({
    type: 'ITEM', title: '', description: '', technicalDetails: '', referenceUrl: '',
    // v96 — campos completos
    suggestedItemId: '', iconUrl: '', recipeJson: '', effectsJson: '',
  })
  const [busy, setBusy] = useState(false)
  /**
   * Sugestão aberta no modal de detalhe. Click no card abre — dentro do modal
   * o user vê descrição completa + detalhes técnicos + recipe (se houver) +
   * effects + referência + nota admin, e tem botões de like/dislike grandes.
   */
  const [selectedSuggestion, setSelectedSuggestion] = useState<any | null>(null)

  const q = useQuery({
    queryKey: ['tester-suggestions', filter],
    queryFn: () => testerApi.listSuggestions(filter === 'all' ? undefined : filter),
    refetchInterval: 15000,
  })

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim() || !form.description.trim()) return
    setBusy(true)
    try {
      await testerApi.createSuggestion(form as any)
      setForm({ type: 'ITEM', title: '', description: '', technicalDetails: '', referenceUrl: '',
                suggestedItemId: '', iconUrl: '', recipeJson: '', effectsJson: '' })
      setShowForm(false)
      qc.invalidateQueries({ queryKey: ['tester-suggestions'] })
    } catch (e: any) { alert(e.message) }
    finally { setBusy(false) }
  }

  async function vote(id: number, currentVote: number, newVote: 1 | -1) {
    const v = (currentVote === newVote ? 0 : newVote) as 1 | 0 | -1
    try {
      const updated = await testerApi.voteSuggestion(id, v)
      qc.invalidateQueries({ queryKey: ['tester-suggestions'] })
      // Se o modal está aberto pra essa sugestão, atualiza o estado local
      // pra refletir o voto novo IMEDIATAMENTE (sem esperar o refetch). O
      // backend retorna a suggestion atualizada com score + myVote novos.
      if (selectedSuggestion && selectedSuggestion.id === id && updated) {
        setSelectedSuggestion(updated)
      }
    } catch (e: any) { alert(e.message) }
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
      <div className="card !bg-purple-500/10 !border-purple-400/40 text-xs">
        <b className="text-purple-300">💡 Como funciona:</b> proponha items/blocos/artefatos novos pro mod. Outros testers votam. Você também pode votar e <b>desempatar</b> sugestões de outros. Quando o admin marcar como <b>APPROVED</b> ou <b>IMPLEMENTED</b>, você ganha crédito no changelog.
      </div>

      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex gap-1 flex-wrap">
          <FilterBtn cur={filter} val="all" onClick={setFilter}>Todas</FilterBtn>
          <FilterBtn cur={filter} val="PENDING" onClick={setFilter}>⏳ Pendentes</FilterBtn>
          <FilterBtn cur={filter} val="UNDER_REVIEW" onClick={setFilter}>👀 Em análise</FilterBtn>
          <FilterBtn cur={filter} val="APPROVED" onClick={setFilter}>✅ Aprovadas</FilterBtn>
          <FilterBtn cur={filter} val="IMPLEMENTED" onClick={setFilter}>🎉 Implementadas</FilterBtn>
        </div>
        <button className="btn btn-sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancelar' : '+ Nova sugestão'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={submit} className="card-glow space-y-3">
          {/* Tipo grande visual */}
          <div>
            <label className="label text-[10px]">Tipo</label>
            <div className="grid grid-cols-3 sm:grid-cols-6 gap-1">
              {([
                { v: 'ITEM', i: '🗡', l: 'Item' },
                { v: 'BLOCK', i: '🧱', l: 'Bloco' },
                { v: 'ARTIFACT', i: '🏺', l: 'Artefato' },
                { v: 'MECHANIC', i: '⚙', l: 'Mecânica' },
                { v: 'MOB', i: '👾', l: 'Mob' },
                { v: 'OTHER', i: '✨', l: 'Outro' },
              ] as const).map(t => (
                <button key={t.v} type="button"
                  onClick={() => setForm({ ...form, type: t.v })}
                  className={`p-2 rounded text-[10px] border ${
                    form.type === t.v
                      ? 'border-purple-400 bg-purple-500/20 ring-1 ring-purple-400/40'
                      : 'border-liberthia-700 bg-liberthia-900/40 hover:bg-purple-500/10'
                  }`}>
                  <div className="text-lg">{t.i}</div>
                  {t.l}
                </button>
              ))}
            </div>
          </div>

          <input className="input text-xs" placeholder="Título (ex: Lâmina do Vazio)"
            value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
            required maxLength={256} />

          <textarea className="input text-xs" rows={3} placeholder="Descrição: o que é e o que faz?"
            value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
            required />

          <textarea className="input text-xs" rows={2}
            placeholder="Detalhes técnicos (propriedades, balanço, comportamento) — opcional"
            value={form.technicalDetails}
            onChange={(e) => setForm({ ...form, technicalDetails: e.target.value })} />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <div>
              <label className="label text-[10px]">ID Minecraft sugerido</label>
              <input className="input text-xs font-mono"
                placeholder="ex: liberthia:void_blade"
                value={form.suggestedItemId}
                onChange={(e) => setForm({ ...form, suggestedItemId: e.target.value })} />
            </div>
            <div>
              <label className="label text-[10px]">🖼 URL da textura/ícone</label>
              <input className="input text-xs"
                placeholder="https://i.imgur.com/textura.png"
                value={form.iconUrl}
                onChange={(e) => setForm({ ...form, iconUrl: e.target.value })} />
            </div>
          </div>

          {form.iconUrl && (
            <div className="text-center">
              <img src={form.iconUrl} alt="preview" className="inline-block w-20 h-20 rounded object-cover"
                onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none' }} />
              <div className="text-[9px] text-liberthia-300/50 mt-1">preview da textura</div>
            </div>
          )}

          <input className="input text-xs"
            placeholder="URL de referência adicional (inspiração) — opcional"
            value={form.referenceUrl}
            onChange={(e) => setForm({ ...form, referenceUrl: e.target.value })} />

          {/* Recipe Builder pra Item/Block */}
          {(form.type === 'ITEM' || form.type === 'BLOCK') && (
            <details className="rounded bg-liberthia-900/30 p-2" open>
              <summary className="cursor-pointer text-xs font-bold text-purple-300">
                🔨 Receita de crafting (opcional)
              </summary>
              <div className="mt-2">
                <RecipeBuilderEmbed
                  value={form.recipeJson}
                  onChange={(json) => setForm({ ...form, recipeJson: json })}
                />
              </div>
            </details>
          )}

          {/* Effects JSON */}
          <details className="rounded bg-liberthia-900/30 p-2">
            <summary className="cursor-pointer text-xs font-bold text-purple-300">
              ⚙ Atributos/efeitos (JSON avançado)
            </summary>
            <textarea className="input text-xs font-mono mt-2" rows={3}
              placeholder='{"damage": 8, "durability": 1500, "effects":[{"id":"poison","duration":200}]}'
              value={form.effectsJson}
              onChange={(e) => setForm({ ...form, effectsJson: e.target.value })} />
          </details>

          <button type="submit" className="btn w-full" disabled={busy}>{busy ? '...' : '💡 Enviar sugestão'}</button>
        </form>
      )}

      <div className="space-y-2">
        {q.data?.suggestions?.length === 0 && (
          <div className="card text-center py-8 text-xs text-liberthia-300/50">
            nenhuma sugestão {filter !== 'all' && `(${filter.toLowerCase()})`} ainda
          </div>
        )}
        {q.data?.suggestions?.map((s: any) => (
          /* Card clicável que abre o modal de detalhe. Antes era um div
             estático com TUDO inline (detalhes técnicos em <details>, recipe
             escondida, etc) — agora preview compacto + modal pra ler tudo. */
          <div
            key={s.id}
            onClick={() => setSelectedSuggestion(s)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => { if (e.key === 'Enter') setSelectedSuggestion(s) }}
            className="card-glow cursor-pointer hover:!border-purple-400/60 transition-colors">
            <div className="flex items-start gap-3">
              {/* Votação à esquerda — stopPropagation pra não abrir modal ao votar */}
              <div className="flex flex-col items-center gap-1 shrink-0"
                   onClick={(e) => e.stopPropagation()}>
                <button onClick={() => vote(s.id, s.myVote, 1)}
                  className={`text-2xl ${s.myVote === 1 ? 'text-emerald-400' : 'text-liberthia-300/40 hover:text-emerald-300'}`}
                  title="Up">▲</button>
                <span className={`text-sm font-bold ${
                  s.score > 0 ? 'text-emerald-300' : s.score < 0 ? 'text-red-300' : 'text-liberthia-300'
                }`}>{s.score}</span>
                <button onClick={() => vote(s.id, s.myVote, -1)}
                  className={`text-2xl ${s.myVote === -1 ? 'text-red-400' : 'text-liberthia-300/40 hover:text-red-300'}`}
                  title="Down">▼</button>
                {s.upvotes === s.downvotes && s.upvotes > 0 && (
                  <span className="text-[9px] text-amber-300 text-center mt-1">⚖ empate</span>
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-2 flex-wrap mb-1">
                  <span className="text-xl">{typeIcon[s.type] ?? '✨'}</span>
                  <h3 className="font-bold truncate">{s.title}</h3>
                  <span className={`badge ${statusColor[s.status]} text-[9px]`}>{s.status}</span>
                </div>
                <div className="text-[10px] text-liberthia-300/50 mb-2">
                  por <b>{s.authorMcName}</b> · {new Date(s.createdAt).toLocaleDateString()}
                  · 👍{s.upvotes} 👎{s.downvotes}
                </div>
                {/* Preview da descrição em 2 linhas (line-clamp). Modal mostra completo. */}
                <p className="text-xs text-liberthia-300/80 line-clamp-2">{s.description}</p>
                <div className="text-[9px] text-purple-300/70 mt-2">ver detalhes →</div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <SuggestionDetailModal
        suggestion={selectedSuggestion}
        onClose={() => setSelectedSuggestion(null)}
        onVote={vote}
      />
    </div>
  )
}

function FilterBtn<T extends string>({ cur, val, onClick, children }:
  { cur: T; val: T; onClick: (v: T) => void; children: React.ReactNode }) {
  const active = cur === val
  return (
    <button onClick={() => onClick(val)}
      className={`btn-ghost btn-sm text-[10px] ${active ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''}`}>
      {children}
    </button>
  )
}

/**
 * Modal de detalhe de uma sugestão da comunidade.
 *
 * Mostra:
 *  - Header com tipo, título, status, autor, data
 *  - Descrição completa (sem line-clamp)
 *  - Detalhes técnicos / Recipe / Effects / Reference URL / Nota admin
 *  - Painel grande de votação no fim (▲ Like / ▼ Dislike + score)
 *
 * ESC ou click no backdrop fecha. Como o vote() do parent atualiza o
 * `suggestion` local quando o user vota, o modal mostra o myVote e score
 * atualizados instantaneamente sem fechar.
 */
function SuggestionDetailModal({ suggestion, onClose, onVote }: {
  suggestion: any | null;
  onClose: () => void;
  onVote: (id: number, currentVote: number, newVote: 1 | -1) => Promise<void>;
}) {
  useEffect(() => {
    if (!suggestion) return
    function onKey(e: KeyboardEvent) { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [suggestion, onClose])

  if (!suggestion) return null
  const s = suggestion

  const typeIcon: Record<string, string> = {
    ITEM: '🗡', BLOCK: '🧱', ARTIFACT: '🏺', MECHANIC: '⚙', MOB: '👾', OTHER: '✨',
  }
  const statusColor: Record<string, string> = {
    PENDING: 'badge-yellow', UNDER_REVIEW: 'badge-purple', APPROVED: 'badge-green',
    REJECTED: 'badge-red', IMPLEMENTED: 'badge-green',
  }

  // Parsing seguro do recipeJson/effectsJson — backend grava como string JSON
  let parsedRecipe: any = null
  try { parsedRecipe = s.recipeJson ? JSON.parse(s.recipeJson) : null } catch {}
  let parsedEffects: any = null
  try { parsedEffects = s.effectsJson ? JSON.parse(s.effectsJson) : null } catch {}

  const date = new Date(s.createdAt).toLocaleDateString('pt-BR', {
    day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })

  return (
    <div className="fixed inset-0 z-[9999] flex items-start justify-center bg-black/80 backdrop-blur-sm p-4 overflow-y-auto"
         onClick={onClose}>
      <div className="card-glow max-w-3xl w-full my-8" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-start justify-between gap-3 mb-3 pb-3 border-b border-purple-500/20">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap mb-1">
              <span className="text-2xl">{typeIcon[s.type] ?? '✨'}</span>
              <span className={`badge ${statusColor[s.status]} text-[10px]`}>{s.status}</span>
              <span className="text-[10px] text-liberthia-300/60">por <b>{s.authorMcName}</b></span>
              <span className="text-[10px] text-liberthia-300/40">· {date}</span>
            </div>
            <h2 className="text-xl font-bold gradient-text">{s.title}</h2>
            {s.suggestedItemId && (
              <div className="text-[10px] font-mono text-liberthia-300/50 mt-1">{s.suggestedItemId}</div>
            )}
          </div>
          <button onClick={onClose} className="btn-ghost btn-sm shrink-0" title="Fechar (ESC)">✕</button>
        </div>

        {/* Ícone grande (se houver) */}
        {s.iconUrl && (
          <div className="mb-3 flex justify-center">
            <img src={s.iconUrl} alt={s.title} className="max-h-32 rounded border border-purple-500/30" />
          </div>
        )}

        {/* Descrição completa */}
        <div className="mb-4">
          <div className="text-[10px] font-bold uppercase text-purple-300/80 mb-1">📝 Descrição</div>
          <p className="text-sm text-liberthia-300/90 whitespace-pre-wrap">{s.description}</p>
        </div>

        {s.technicalDetails && (
          <div className="mb-4">
            <div className="text-[10px] font-bold uppercase text-purple-300/80 mb-1">⚙ Detalhes técnicos</div>
            <pre className="text-xs text-liberthia-300/85 whitespace-pre-wrap bg-liberthia-900/40 p-3 rounded border border-purple-500/10">
              {s.technicalDetails}
            </pre>
          </div>
        )}

        {/* Recipe (se houver) */}
        {parsedRecipe && (
          <div className="mb-4">
            <div className="text-[10px] font-bold uppercase text-purple-300/80 mb-1">🪛 Receita sugerida</div>
            <pre className="text-[10px] font-mono whitespace-pre-wrap bg-liberthia-900/40 p-3 rounded text-purple-200">
              {JSON.stringify(parsedRecipe, null, 2)}
            </pre>
          </div>
        )}

        {/* Effects (se houver) */}
        {parsedEffects && (
          <div className="mb-4">
            <div className="text-[10px] font-bold uppercase text-purple-300/80 mb-1">✨ Efeitos/Atributos</div>
            <pre className="text-[10px] font-mono whitespace-pre-wrap bg-liberthia-900/40 p-3 rounded text-amber-200">
              {JSON.stringify(parsedEffects, null, 2)}
            </pre>
          </div>
        )}

        {s.referenceUrl && (
          <div className="mb-4">
            <a href={s.referenceUrl} target="_blank" rel="noreferrer"
              className="text-xs text-purple-300 hover:underline">
              🔗 {s.referenceUrl}
            </a>
          </div>
        )}

        {s.adminNote && (
          <div className="mb-4 p-3 rounded bg-amber-900/20 border border-amber-500/30">
            <div className="text-[10px] font-bold uppercase text-amber-300 mb-1">📝 Nota do admin</div>
            <p className="text-sm text-amber-100 whitespace-pre-wrap">{s.adminNote}</p>
          </div>
        )}

        {/* Painel de votação destacado no fim */}
        <div className="mt-4 pt-3 border-t border-purple-500/20">
          <div className="flex items-center justify-center gap-6">
            <button
              onClick={() => onVote(s.id, s.myVote, 1)}
              className={`flex flex-col items-center gap-1 px-4 py-2 rounded transition-colors ${
                s.myVote === 1
                  ? 'bg-emerald-500/25 ring-1 ring-emerald-400 text-emerald-300'
                  : 'bg-liberthia-900/40 text-liberthia-300/60 hover:bg-emerald-500/10 hover:text-emerald-300'
              }`}
              title="Curtir essa sugestão">
              <span className="text-3xl leading-none">▲</span>
              <span className="text-[10px] font-bold">{s.upvotes ?? 0} likes</span>
            </button>

            <div className="text-center">
              <div className={`text-3xl font-bold ${
                s.score > 0 ? 'text-emerald-300' : s.score < 0 ? 'text-red-300' : 'text-liberthia-300/60'
              }`}>{s.score > 0 ? '+' : ''}{s.score}</div>
              <div className="text-[9px] text-liberthia-300/50">score</div>
              {s.upvotes === s.downvotes && s.upvotes > 0 && (
                <div className="text-[9px] text-amber-300 mt-0.5">⚖ empate</div>
              )}
            </div>

            <button
              onClick={() => onVote(s.id, s.myVote, -1)}
              className={`flex flex-col items-center gap-1 px-4 py-2 rounded transition-colors ${
                s.myVote === -1
                  ? 'bg-red-500/25 ring-1 ring-red-400 text-red-300'
                  : 'bg-liberthia-900/40 text-liberthia-300/60 hover:bg-red-500/10 hover:text-red-300'
              }`}
              title="Não curti">
              <span className="text-3xl leading-none">▼</span>
              <span className="text-[10px] font-bold">{s.downvotes ?? 0} dislikes</span>
            </button>
          </div>
          <div className="text-[9px] text-liberthia-300/40 text-center mt-2">
            Clica de novo no voto pra remover · Tua decisão: <b>{
              s.myVote === 1 ? 'curtiu 👍' :
              s.myVote === -1 ? 'não curtiu 👎' :
              'sem voto'
            }</b>
          </div>
        </div>

        <div className="mt-4 pt-3 border-t border-purple-500/20 flex justify-end">
          <button onClick={onClose} className="btn-ghost btn-sm">Fechar</button>
        </div>
      </div>
    </div>
  )
}

// ============================================================ //
// MODELS TAB — Galeria 3D de modelos BlockBench
// ============================================================ //
function ModelsTab() {
  const q = useQuery({
    queryKey: ['tester-models'],
    queryFn: testerApi.testerListModels,
    refetchOnWindowFocus: false,
  })
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterCategory, setFilterCategory] = useState<string>('all')

  const models = q.data?.models ?? []
  const categories = useMemo(() => {
    const set = new Set<string>()
    for (const m of models) if (m.category) set.add(m.category)
    return ['all', ...Array.from(set).sort()]
  }, [models])

  const filtered = useMemo(() => {
    return models.filter((m: any) => {
      if (filterCategory !== 'all' && m.category !== filterCategory) return false
      if (searchTerm && !`${m.name} ${m.description ?? ''}`.toLowerCase().includes(searchTerm.toLowerCase())) return false
      return true
    })
  }, [models, searchTerm, filterCategory])

  if (q.isLoading) return <div className="text-center py-10 text-liberthia-300/60">Carregando modelos...</div>
  if (q.error) return <div className="text-red-400 text-sm">Erro: {String(q.error)}</div>

  return (
    <div className="space-y-4">
      <div className="card-tester p-3 flex flex-wrap items-center gap-2">
        <h2 className="text-base sm:text-lg font-bold gradient-text">🧊 Modelos 3D · BlockBench</h2>
        <div className="text-xs text-liberthia-300/60 flex-1">
          Visualize modelos 3D dos itens/blocos do mod. Arraste pra rotacionar, scroll pra zoom.
        </div>
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="🔍 buscar..."
          className="input input-sm text-xs"
        />
      </div>

      {categories.length > 1 && (
        <div className="flex flex-wrap gap-1">
          {categories.map(c => (
            <FilterBtn key={c} cur={filterCategory} val={c} onClick={setFilterCategory}>
              {c === 'all' ? '📦 todos' : c}
            </FilterBtn>
          ))}
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="card-tester p-8 text-center text-liberthia-300/60">
          <div className="text-4xl mb-2">🧊</div>
          <div className="text-sm">Nenhum modelo 3D ainda. Admin pode subir em /tester-admin.</div>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {filtered.map((m: any) => (
            <ModelCard key={m.id} model={m} onOpen={() => setSelectedId(m.id)} />
          ))}
        </div>
      )}

      {selectedId !== null && (
        <ModelViewerModal
          modelId={selectedId}
          model={models.find((m: any) => m.id === selectedId)}
          onClose={() => setSelectedId(null)}
        />
      )}
    </div>
  )
}

function ModelCard({ model, onOpen }: { model: any; onOpen: () => void }) {
  const format = (model.format ?? 'bbmodel') as string
  const formatEmoji = format === 'gltf' || format === 'glb' ? '✨' : format === 'obj' ? '📐' : '🧊'
  return (
    <button
      onClick={onOpen}
      className="card-tester p-3 text-left hover:ring-2 hover:ring-purple-400 transition-all group"
    >
      <div className="aspect-square rounded bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center border border-slate-700/50 group-hover:border-purple-400 transition-colors mb-2 relative">
        <div className="text-5xl opacity-50 group-hover:opacity-100 group-hover:scale-110 transition-all">{formatEmoji}</div>
        <span className="absolute top-1 right-1 px-1.5 py-0.5 rounded bg-blue-500/30 text-blue-200 text-[9px] font-mono uppercase">
          {format}
        </span>
      </div>
      <div className="font-bold text-sm truncate">{model.name}</div>
      {model.description && (
        <div className="text-[10px] text-liberthia-300/60 line-clamp-2 mt-0.5">{model.description}</div>
      )}
      <div className="flex items-center justify-between mt-2 text-[9px] text-liberthia-300/50">
        {model.category && <span className="px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300">{model.category}</span>}
        <span>{model.viewCount ?? 0} 👁</span>
      </div>
    </button>
  )
}

function ModelViewerModal({ modelId, model, onClose }: { modelId: number; model?: any; onClose: () => void }) {
  // Aceita qualquer ModelData (bbmodel | gltf | glb | obj) — viewer interno dispatcha
  const [modelData, setModelData] = useState<any>(null)
  const [error, setError] = useState<string | null>(null)
  const [autoRotate, setAutoRotate] = useState(true)
  const [viewportSize, setViewportSize] = useState({ w: 600, h: 500 })
  const format = (model?.format ?? 'bbmodel') as 'bbmodel' | 'gltf' | 'glb' | 'obj'

  useEffect(() => {
    setModelData(null)
    setError(null)
    const token = getTesterToken()
    if (!token) {
      setError('Sessão expirou')
      return
    }
    let cancelled = false
    // Lazy import + fetch combinados — só baixa o viewer chunk quando precisar
    import('../../components/Model3DViewer').then(({ fetchModelData }) => {
      return fetchModelData(`/api/tester/models/${modelId}/file`, format, token)
    }).then(data => {
      if (!cancelled) setModelData(data)
    }).catch(e => {
      if (!cancelled) setError(e?.message ?? String(e))
    })
    return () => { cancelled = true }
  }, [modelId, format])

  useEffect(() => {
    function updateSize() {
      const maxW = Math.min(window.innerWidth - 40, 900)
      const maxH = Math.min(window.innerHeight - 220, 700)
      setViewportSize({ w: maxW, h: maxH })
    }
    updateSize()
    window.addEventListener('resize', updateSize)
    return () => window.removeEventListener('resize', updateSize)
  }, [])

  // ESC fecha
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div
        className="card-tester max-w-5xl w-full max-h-[95vh] overflow-auto p-4 sm:p-6"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="min-w-0 flex-1">
            <h3 className="text-lg sm:text-xl font-bold gradient-text truncate">{model?.name ?? 'Modelo'}</h3>
            {model?.description && (
              <p className="text-xs sm:text-sm text-liberthia-300/70 mt-0.5">{model.description}</p>
            )}
            <div className="flex flex-wrap gap-2 mt-1 text-[10px] text-liberthia-300/50">
              {model?.category && <span className="px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300">{model.category}</span>}
              <span className="px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-300 font-mono uppercase">{format}</span>
              {model?.filename && <span>📁 {model.filename}</span>}
              {model?.sizeBytes && <span>{fmtBytes(model.sizeBytes)}</span>}
              {model?.uploadedBy && <span>📤 {model.uploadedBy}</span>}
            </div>
          </div>
          <button className="btn-ghost btn-sm text-xs flex-shrink-0" onClick={onClose}>✕ Fechar</button>
        </div>

        <div className="flex flex-wrap gap-2 mb-3">
          <label className="flex items-center gap-1.5 text-xs cursor-pointer">
            <input
              type="checkbox"
              checked={autoRotate}
              onChange={e => setAutoRotate(e.target.checked)}
              className="rounded"
            />
            Auto-rotacionar
          </label>
          <div className="text-[10px] text-liberthia-300/50 italic ml-auto">
            🖱️ arrasta = rotacionar · 🖱️ scroll = zoom
          </div>
        </div>

        {error ? (
          <div className="rounded-lg border border-red-700 bg-red-950/30 p-6 text-center text-red-200">
            <div className="text-3xl mb-2">⚠️</div>
            <div className="font-semibold">Erro ao carregar modelo</div>
            <div className="text-xs mt-1 opacity-80">{error}</div>
          </div>
        ) : !modelData ? (
          <div
            style={{ width: viewportSize.w, height: viewportSize.h }}
            className="flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 mx-auto"
          >
            <div className="text-liberthia-300/60 text-sm animate-pulse">Carregando modelo 3D ({format})...</div>
          </div>
        ) : (
          <div className="mx-auto" style={{ width: viewportSize.w }}>
            <ModelViewerLazy model={modelData} width={viewportSize.w} height={viewportSize.h} autoRotate={autoRotate} />
          </div>
        )}
      </div>
    </div>
  )
}

// Lazy import pra não estourar bundle inicial quando tester não usa modelos
function ModelViewerLazy({ model, width, height, autoRotate }:
  { model: any; width: number; height: number; autoRotate: boolean }) {
  const [Viewer, setViewer] = useState<any>(null)
  useEffect(() => {
    import('../../components/Model3DViewer').then(m => setViewer(() => m.Model3DViewer))
  }, [])
  if (!Viewer) {
    return (
      <div style={{ width, height }} className="flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900">
        <div className="text-liberthia-300/60 text-sm animate-pulse">Inicializando three.js...</div>
      </div>
    )
  }
  return <Viewer model={model} width={width} height={height} autoRotate={autoRotate} />
}

// ============================================================ //
// AUDIOS TAB — Galeria de áudios beta com player + votação
// ============================================================ //
function AudiosTab() {
  const q = useQuery({
    queryKey: ['tester-audios'],
    queryFn: testerApi.testerListAudios,
    refetchOnWindowFocus: false,
  })
  const [searchTerm, setSearchTerm] = useState('')
  const [filterCategory, setFilterCategory] = useState<string>('all')
  const [sortBy, setSortBy] = useState<'newest' | 'mostLiked' | 'mostPlayed'>('newest')

  const audios = q.data?.audios ?? []
  const categories = useMemo(() => {
    const set = new Set<string>()
    for (const a of audios) if (a.category) set.add(a.category)
    return ['all', ...Array.from(set).sort()]
  }, [audios])

  const filtered = useMemo(() => {
    let list = audios.filter((a: any) => {
      if (filterCategory !== 'all' && a.category !== filterCategory) return false
      if (searchTerm && !`${a.name} ${a.description ?? ''} ${a.creatureId ?? ''}`.toLowerCase().includes(searchTerm.toLowerCase())) return false
      return true
    })
    if (sortBy === 'mostLiked') {
      list = list.slice().sort((a: any, b: any) => {
        const sa = (a.votes?.likes ?? 0) - (a.votes?.dislikes ?? 0)
        const sb = (b.votes?.likes ?? 0) - (b.votes?.dislikes ?? 0)
        return sb - sa
      })
    } else if (sortBy === 'mostPlayed') {
      list = list.slice().sort((a: any, b: any) => (b.playCount ?? 0) - (a.playCount ?? 0))
    }
    return list
  }, [audios, searchTerm, filterCategory, sortBy])

  if (q.isLoading) return <div className="text-center py-10 text-liberthia-300/60">Carregando áudios...</div>
  if (q.error) return <div className="text-red-400 text-sm">Erro: {String(q.error)}</div>

  return (
    <div className="space-y-4">
      <div className="card-tester p-3 flex flex-wrap items-center gap-2">
        <h2 className="text-base sm:text-lg font-bold gradient-text">🔊 Áudios beta · criaturas, instrumentos, ambient</h2>
        <div className="text-xs text-liberthia-300/60 flex-1 min-w-0">
          Vote 👍/👎 nos sons de teste — admin usa pra escolher quais entram no mod.
        </div>
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="🔍 buscar..."
          className="input input-sm text-xs"
        />
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {categories.length > 1 && (
          <div className="flex flex-wrap gap-1">
            {categories.map(c => (
              <FilterBtn key={c} cur={filterCategory} val={c} onClick={setFilterCategory}>
                {c === 'all' ? '🎵 todos' : c}
              </FilterBtn>
            ))}
          </div>
        )}
        <div className="flex flex-wrap gap-1 ml-auto">
          <FilterBtn cur={sortBy} val={'newest'} onClick={setSortBy}>🕒 recentes</FilterBtn>
          <FilterBtn cur={sortBy} val={'mostLiked'} onClick={setSortBy}>👍 mais curtidos</FilterBtn>
          <FilterBtn cur={sortBy} val={'mostPlayed'} onClick={setSortBy}>🔥 mais ouvidos</FilterBtn>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="card-tester p-8 text-center text-liberthia-300/60">
          <div className="text-4xl mb-2">🔇</div>
          <div className="text-sm">Nenhum áudio publicado ainda.</div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {filtered.map((a: any) => (
            <AudioCard key={a.id} audio={a} />
          ))}
        </div>
      )}
    </div>
  )
}

function AudioCard({ audio }: { audio: any }) {
  const [Player, setPlayer] = useState<any>(null)
  const [showPlayer, setShowPlayer] = useState(false)

  useEffect(() => {
    if (showPlayer && !Player) {
      import('../../components/AudioPlayer').then(m => setPlayer(() => m.AudioPlayer))
    }
  }, [showPlayer, Player])

  const streamUrl = `/api/tester/audios/${audio.id}/stream`

  return (
    <div className="card-tester p-3 flex flex-col gap-2">
      <div className="flex items-start gap-3">
        <div className="text-3xl flex-shrink-0">
          {audio.category === 'criatura' ? '👹' :
            audio.category === 'instrumento' ? '🎺' :
              audio.category === 'ambient' ? '🌫' : '🎵'}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-bold text-sm truncate">{audio.name}</h3>
          <div className="flex flex-wrap items-center gap-1 text-[10px] text-liberthia-300/60 mt-0.5">
            {audio.category && <span className="px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300">{audio.category}</span>}
            {audio.creatureId && <span className="font-mono">🎯 {audio.creatureId}</span>}
            {audio.durationSec && <span>⏱ {audio.durationSec}s</span>}
            {audio.sizeBytes && <span>📦 {fmtBytes(audio.sizeBytes)}</span>}
            <span>🔊 {audio.playCount ?? 0}</span>
          </div>
          {audio.description && (
            <p className="text-xs text-liberthia-300/80 mt-1 line-clamp-2">{audio.description}</p>
          )}
        </div>
      </div>

      {!showPlayer ? (
        <button
          onClick={() => setShowPlayer(true)}
          className="btn btn-sm text-xs w-full"
        >
          ▶ Carregar e tocar
        </button>
      ) : Player ? (
        <Player
          streamUrl={streamUrl}
          mimeType={audio.mimeType}
          title={audio.name}
          autoPlay={true}
        />
      ) : (
        <div className="rounded bg-slate-800/50 p-2 text-xs text-slate-400 animate-pulse">
          carregando player...
        </div>
      )}

      <div className="flex items-center justify-between gap-2 pt-1 border-t border-slate-700/30">
        <VoteButtonsLazy
          targetType="BETA_AUDIO"
          targetId={audio.id}
          counts={{
            likes: Number(audio.votes?.likes ?? 0),
            dislikes: Number(audio.votes?.dislikes ?? 0),
            myVote: (audio.votes?.myVote ?? null) as any,
          }}
          size="sm"
        />
        <div className="text-[9px] text-liberthia-300/40">
          📤 {audio.uploadedBy ?? '—'}
        </div>
      </div>
    </div>
  )
}
