import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { publicApi, BugLeaderboardEntry } from '../../lib/api'

/**
 * Ranking público de caçadores de bugs — qualquer um vê.
 * Mostra top testers ordenados por pontos + bugs confirmados, com tiers
 * (Lendário, Diamond, Gold, Silver, Bronze, Iniciante).
 */
export function BugLeaderboardPublicPage() {
  const q = useQuery({
    queryKey: ['public-leaderboard'],
    queryFn: publicApi.leaderboard,
    refetchInterval: 30000,
  })

  const data = q.data
  const ranking: BugLeaderboardEntry[] = data?.ranking ?? []
  const podium = ranking.slice(0, 3)
  const rest = ranking.slice(3)

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 py-8 px-4">
      <div className="w-full max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="text-6xl mb-2">🏆</div>
          <h1 className="text-4xl font-bold gradient-text">Caçadores de Bugs</h1>
          <p className="text-sm text-liberthia-300/70 mt-2">
            Ranking dos testers que mais ajudaram a melhorar o Liberthia
          </p>
        </div>

        {/* Nav pública */}
        <div className="flex justify-center gap-2 mb-6 text-xs flex-wrap">
          <Link to="/changelog" className="btn-ghost btn-sm">📋 Changelog</Link>
          <Link to="/roadmap" className="btn-ghost btn-sm">🗺 Roadmap</Link>
          <Link to="/leaderboard" className="btn btn-sm">🏆 Caçadores de Bugs</Link>
          <Link to="/tester/apply" className="btn-ghost btn-sm">📝 Virar Tester</Link>
        </div>

        {q.isLoading && <div className="text-center text-liberthia-300/60">Carregando ranking...</div>}
        {q.isError && (
          <div className="card-glow text-center py-10 text-red-400">
            Erro ao carregar ranking.
          </div>
        )}

        {/* Stats agregadas */}
        {data && (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mb-6">
            <StatCard label="Testers ativos" value={data.totalTesters} emoji="👥" />
            <StatCard label="Bugs confirmados" value={data.totalBugsConfirmed} emoji="🐛" />
            <StatCard label="Top tester" value={podium[0]?.mcName ?? '—'} emoji="👑" small />
          </div>
        )}

        {/* Podium */}
        {podium.length >= 1 && (
          <div className="mb-8">
            <h2 className="text-sm uppercase tracking-widest text-amber-300/70 font-bold mb-4 text-center">
              🥇 Pódio
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {podium[1] && <PodiumCard entry={podium[1]} medalEmoji="🥈" rank={2} height="md:order-first md:mt-8" />}
              {podium[0] && <PodiumCard entry={podium[0]} medalEmoji="🥇" rank={1} highlight />}
              {podium[2] && <PodiumCard entry={podium[2]} medalEmoji="🥉" rank={3} height="md:order-last md:mt-12" />}
            </div>
          </div>
        )}

        {/* Lista completa */}
        {rest.length > 0 && (
          <div className="card-glow">
            <h3 className="font-bold text-sm mb-3 text-liberthia-200">📊 Ranking completo</h3>
            <div className="space-y-1">
              {rest.map(e => <RankRow key={e.mcName} entry={e} />)}
            </div>
          </div>
        )}

        {ranking.length === 0 && !q.isLoading && (
          <div className="card-glow text-center py-12">
            <div className="text-4xl mb-2 opacity-50">🐛</div>
            <p className="text-liberthia-300/60">
              Ninguém reportou bug ainda. Seja o primeiro!
            </p>
            <Link to="/tester/apply" className="btn mt-4 inline-block">Virar tester</Link>
          </div>
        )}

        {/* Tiers explicação */}
        {data?.tiers && (
          <div className="card !bg-purple-500/5 !border-purple-400/20 mt-6 text-xs">
            <h3 className="font-bold mb-2 text-liberthia-200">🏅 Tiers</h3>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-2 text-[11px]">
              {data.tiers.map((t: any) => (
                <div key={t.name} className="flex items-center gap-2 p-1.5 rounded bg-liberthia-900/40">
                  <span className="text-lg">{t.emoji}</span>
                  <div>
                    <div className="font-bold" style={{ color: t.color }}>{t.name}</div>
                    <div className="text-liberthia-300/60">{t.minBugs}+ bugs</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="text-center mt-6 text-[10px] text-liberthia-300/40">
          Atualiza a cada 30s · pontos são dados pelo admin ao confirmar cada bug
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value, emoji, small = false }: { label: string; value: any; emoji: string; small?: boolean }) {
  return (
    <div className="card text-center py-3">
      <div className="text-2xl mb-1">{emoji}</div>
      <div className={`font-bold gradient-text ${small ? 'text-base truncate' : 'text-2xl'}`}>{value}</div>
      <div className="text-[10px] text-liberthia-300/60 uppercase tracking-wider mt-0.5">{label}</div>
    </div>
  )
}

function PodiumCard({ entry, medalEmoji, rank, highlight = false, height = '' }: {
  entry: BugLeaderboardEntry
  medalEmoji: string
  rank: number
  highlight?: boolean
  height?: string
}) {
  return (
    <div className={`card-glow text-center ${height} ${highlight ? '!bg-amber-500/15 !border-amber-400/50 shadow-2xl shadow-amber-500/20 ring-2 ring-amber-300/40' : ''}`}>
      <div className="text-5xl mb-2">{medalEmoji}</div>
      <div className="text-[10px] text-liberthia-300/50 uppercase tracking-wider mb-1">{rank}º lugar</div>
      <img src={`https://mc-heads.net/avatar/${entry.mcName}/64`}
        className="w-16 h-16 mx-auto rounded-lg shadow-lg mb-2"
        alt={entry.mcName} />
      <div className="font-bold text-lg gradient-text truncate">{entry.mcName}</div>
      <div className="flex items-center justify-center gap-2 text-[11px] mt-2">
        <span className="badge badge-purple">⭐ {entry.points} pts</span>
        <span className="badge">🐛 {entry.bugsConfirmed}</span>
      </div>
      <div className="text-[10px] text-amber-300/80 mt-1">{entry.tier}</div>
    </div>
  )
}

function RankRow({ entry }: { entry: BugLeaderboardEntry }) {
  return (
    <div className="flex items-center gap-3 p-2 rounded hover:bg-liberthia-500/10 transition-colors">
      <div className="w-8 text-center text-xs font-mono text-liberthia-300/60">#{entry.rank}</div>
      <img src={`https://mc-heads.net/avatar/${entry.mcName}/32`}
        className="w-8 h-8 rounded shadow shrink-0"
        alt={entry.mcName} />
      <div className="flex-1 min-w-0">
        <div className="font-bold text-sm truncate">{entry.mcName}</div>
        <div className="text-[10px] text-liberthia-300/60">{entry.tier}</div>
      </div>
      <div className="flex items-center gap-2 text-xs">
        <span className="badge badge-purple text-[10px]">⭐ {entry.points}</span>
        <span className="badge text-[10px]">🐛 {entry.bugsConfirmed}</span>
      </div>
    </div>
  )
}
