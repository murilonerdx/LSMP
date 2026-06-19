import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'
import { EventLog } from '../components/EventLog'
import { PageHeader } from '../components/PageHeader'

export function Dashboard() {
  const serverQ = useQuery({
    queryKey: ['serverInfo'],
    queryFn: api.serverInfo,
    retry: false,
    refetchInterval: 5000,
  })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, retry: false, refetchInterval: 4000 })

  const errMsg = (serverQ.error as Error | undefined)?.message ?? ''
  const offline = !!serverQ.error && (errMsg.includes('mod_offline') || errMsg.includes('503'))

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" subtitle="Visão geral do servidor e atividade" icon="📊">
        <span className={`badge ${offline ? 'badge-red' : 'badge-green'}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${offline ? 'bg-red-400' : 'bg-emerald-400'} animate-pulse`} />
          {offline ? 'Offline' : 'Online'}
        </span>
      </PageHeader>

      {offline && <OfflineBanner />}

      {!offline && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Stat
            icon="👥"
            label="Players online"
            value={`${serverQ.data?.playerCount ?? 0}/${serverQ.data?.maxPlayers ?? 0}`}
            color="from-purple-500/20 to-purple-700/10"
          />
          <Stat
            icon="⚡"
            label="TPS"
            value={serverQ.data?.tps?.toFixed(1) ?? '?'}
            color={
              (serverQ.data?.tps ?? 0) > 19 ? 'from-emerald-500/20 to-emerald-700/10' :
              (serverQ.data?.tps ?? 0) > 15 ? 'from-yellow-500/20 to-yellow-700/10' :
              'from-red-500/20 to-red-700/10'
            }
          />
          <Stat
            icon="🌍"
            label="Dimensões"
            value={serverQ.data?.dimensions?.length ?? 0}
            color="from-cyan-500/20 to-cyan-700/10"
          />
          <Stat
            icon="🕐"
            label="Tick"
            value={(serverQ.data?.tickCount ?? 0).toLocaleString()}
            color="from-pink-500/20 to-pink-700/10"
          />
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="card-glow">
          <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
            <span>⚡</span> Acesso rápido
          </h2>
          <div className="grid grid-cols-2 gap-2">
            <QuickLink to="/map" icon="🗺" label="Live Map" desc="Radar 2D + ações" />
            <QuickLink to="/scripts" icon="📜" label="Scripts" desc="Sequência de comandos" />
            <QuickLink to="/broadcaster" icon="📡" label="Broadcaster" desc="Loop de mensagens" />
            <QuickLink to="/automation" icon="⚙" label="Automation" desc="Triggers reativos" />
            <QuickLink to="/tools" icon="⚡" label="Power Tools" desc="11 ferramentas" />
            <QuickLink to="/players" icon="👥" label="Players" desc="Administração" />
            <QuickLink to="/console" icon="⌨" label="Console" desc="Executar comandos" />
            <QuickLink to="/items" icon="📦" label="Catálogo" desc="Items + Encantos" />
          </div>
        </div>

        <div className="card-glow">
          <h2 className="text-lg font-bold mb-3 flex items-center gap-2">
            <span>👥</span> Players Online
            <span className="badge badge-purple ml-auto">{playersQ.data?.length ?? 0}</span>
          </h2>
          {(playersQ.data ?? []).length === 0 ? (
            <p className="text-liberthia-300/60 italic text-sm py-4 text-center">Ninguém conectado</p>
          ) : (
            <ul className="space-y-2">
              {(playersQ.data ?? []).slice(0, 8).map(p => (
                <li key={p.uuid}>
                  <Link to={`/player/${p.uuid}`} className="flex items-center gap-2 p-2 rounded-lg hover:bg-liberthia-700/30 transition group">
                    <img src={`https://mc-heads.net/avatar/${p.uuid}/32`} className="w-8 h-8 rounded" />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium truncate group-hover:text-liberthia-300 transition">{p.name}</div>
                      <div className="text-[10px] text-liberthia-300/60 truncate">L{p.level} · {p.dimension.replace('minecraft:', '')}</div>
                    </div>
                    <span className="text-xs text-liberthia-300/40 group-hover:text-liberthia-300 transition">→</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>

        <EventLog />
      </div>
    </div>
  )
}

function Stat({ icon, label, value, color }: { icon: string; label: string; value: any; color: string }) {
  return (
    <div className={`stat-card bg-gradient-to-br ${color}`}>
      <div className="flex items-center justify-between">
        <span className="label">{label}</span>
        <span className="text-2xl opacity-60">{icon}</span>
      </div>
      <div className="text-3xl font-black gradient-text mt-1">{value}</div>
    </div>
  )
}

function QuickLink({ to, icon, label, desc }: { to: string; icon: string; label: string; desc: string }) {
  return (
    <Link to={to} className="p-3 rounded-lg bg-liberthia-900/40 hover:bg-liberthia-700/40 hover:border-liberthia-400/40 border border-transparent transition group">
      <div className="text-2xl mb-1 group-hover:scale-110 transition origin-left">{icon}</div>
      <div className="font-bold text-sm">{label}</div>
      <div className="text-[10px] text-liberthia-300/60">{desc}</div>
    </Link>
  )
}

function OfflineBanner() {
  return (
    <div className="card border-yellow-500/40 bg-gradient-to-br from-yellow-500/10 to-yellow-600/5">
      <div className="flex items-start gap-3">
        <span className="text-3xl">⚠</span>
        <div>
          <h3 className="font-bold text-yellow-300 text-lg">Servidor MC offline</h3>
          <p className="text-sm text-yellow-200/80 mt-1">
            Backend não consegue se conectar ao mod. Verifica:
          </p>
          <ul className="text-sm text-yellow-200/80 mt-2 space-y-1 ml-4 list-disc">
            <li>O servidor MC tá rodando com o mod Liberthia?</li>
            <li>O token no <code className="bg-black/40 px-1 rounded">backend/application.yml</code> bate com o do mod?</li>
            <li>No chat do server: <code className="bg-black/40 px-1 rounded">/liberthia admin status</code></li>
          </ul>
        </div>
      </div>
    </div>
  )
}
