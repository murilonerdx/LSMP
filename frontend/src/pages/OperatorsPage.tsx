import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../lib/api'
import { PageHeader } from '../components/PageHeader'

export function OperatorsPage() {
  const qc = useQueryClient()
  const opsQ = useQuery({ queryKey: ['operators'], queryFn: api.operators })
  const bansQ = useQuery({ queryKey: ['bans'], queryFn: api.bans })
  const wlQ = useQuery({ queryKey: ['whitelist'], queryFn: api.whitelist })
  const [opName, setOpName] = useState('')
  const [banName, setBanName] = useState('')
  const [wlName, setWlName] = useState('')

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['operators'] })
    qc.invalidateQueries({ queryKey: ['bans'] })
    qc.invalidateQueries({ queryKey: ['whitelist'] })
  }

  const cmd = async (c: string) => {
    try { await api.command(c); refresh() } catch (e: any) { alert(`Erro: ${e.message}`) }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Operadores & Permissões" subtitle="OPs, Bans, Whitelist" icon="🛡">
        <button className="btn-ghost" onClick={refresh}>🔄 Refresh</button>
      </PageHeader>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* OPs */}
        <div className="card-glow">
          <h3 className="font-bold mb-3 flex items-center gap-2">
            <span>👑</span> Operadores ({opsQ.data?.operators.length ?? 0})
          </h3>
          <div className="flex gap-2 mb-3">
            <input className="input" placeholder="player_name" value={opName} onChange={e => setOpName(e.target.value)} />
            <button className="btn-success btn-sm" onClick={() => { if (opName.trim()) { cmd(`op ${opName.trim()}`); setOpName('') } }}>+ OP</button>
          </div>
          <ul className="space-y-1 max-h-64 overflow-y-auto">
            {(opsQ.data?.operators ?? []).map(o => (
              <li key={o.name} className="flex items-center justify-between bg-liberthia-900/40 rounded px-3 py-1.5">
                <span className="text-sm font-medium">{o.name}</span>
                <button className="btn-ghost btn-sm text-red-400" onClick={() => cmd(`deop ${o.name}`)}>Remover</button>
              </li>
            ))}
            {(opsQ.data?.operators ?? []).length === 0 && (
              <p className="text-xs text-liberthia-300/60 italic text-center py-4">Nenhum OP</p>
            )}
          </ul>
        </div>

        {/* Whitelist */}
        <div className="card-glow">
          <h3 className="font-bold mb-1 flex items-center gap-2">
            <span>📋</span> Whitelist ({wlQ.data?.whitelist.length ?? 0})
          </h3>
          <p className="text-[10px] mb-3">
            Status: {wlQ.data?.enabled
              ? <span className="badge badge-green">ativada</span>
              : <span className="badge badge-yellow">desativada</span>}
          </p>
          <div className="flex gap-2 mb-3">
            <input className="input" placeholder="player_name" value={wlName} onChange={e => setWlName(e.target.value)} />
            <button className="btn-success btn-sm" onClick={() => { if (wlName.trim()) { cmd(`whitelist add ${wlName.trim()}`); setWlName('') } }}>+ Add</button>
          </div>
          <div className="flex gap-2 mb-3">
            <button className="btn-ghost btn-sm flex-1" onClick={() => cmd('whitelist on')}>Ligar</button>
            <button className="btn-ghost btn-sm flex-1" onClick={() => cmd('whitelist off')}>Desligar</button>
            <button className="btn-ghost btn-sm flex-1" onClick={() => cmd('whitelist reload')}>Reload</button>
          </div>
          <ul className="space-y-1 max-h-48 overflow-y-auto">
            {(wlQ.data?.whitelist ?? []).map(w => (
              <li key={w.name} className="flex items-center justify-between bg-liberthia-900/40 rounded px-3 py-1.5">
                <span className="text-sm font-medium">{w.name}</span>
                <button className="btn-ghost btn-sm text-red-400" onClick={() => cmd(`whitelist remove ${w.name}`)}>Remover</button>
              </li>
            ))}
            {(wlQ.data?.whitelist ?? []).length === 0 && (
              <p className="text-xs text-liberthia-300/60 italic text-center py-4">Whitelist vazia</p>
            )}
          </ul>
        </div>

        {/* Bans */}
        <div className="card-glow">
          <h3 className="font-bold mb-3 flex items-center gap-2">
            <span>🚫</span> Bans ({(bansQ.data?.playerBans.length ?? 0) + (bansQ.data?.ipBans.length ?? 0)})
          </h3>
          <div className="flex gap-2 mb-3">
            <input className="input" placeholder="player_name" value={banName} onChange={e => setBanName(e.target.value)} />
            <button className="btn-danger btn-sm" onClick={() => { if (banName.trim()) { cmd(`ban ${banName.trim()}`); setBanName('') } }}>BAN</button>
          </div>
          <div className="text-xs label mb-1">Players banidos</div>
          <ul className="space-y-1 max-h-32 overflow-y-auto mb-3">
            {(bansQ.data?.playerBans ?? []).map(b => (
              <li key={b.name} className="flex items-center justify-between bg-red-900/20 rounded px-3 py-1.5">
                <span className="text-sm font-medium">{b.name}</span>
                <button className="btn-ghost btn-sm text-emerald-400" onClick={() => cmd(`pardon ${b.name}`)}>Unban</button>
              </li>
            ))}
            {(bansQ.data?.playerBans ?? []).length === 0 && (
              <p className="text-xs text-liberthia-300/60 italic text-center py-2">Nenhum ban</p>
            )}
          </ul>
          {(bansQ.data?.ipBans ?? []).length > 0 && (
            <>
              <div className="text-xs label mb-1">IPs banidos</div>
              <ul className="space-y-1 max-h-24 overflow-y-auto">
                {(bansQ.data?.ipBans ?? []).map(b => (
                  <li key={b.ip} className="flex items-center justify-between bg-red-900/20 rounded px-3 py-1.5">
                    <span className="text-xs font-mono">{b.ip}</span>
                    <button className="btn-ghost btn-sm text-emerald-400" onClick={() => cmd(`pardon-ip ${b.ip}`)}>Unban</button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </div>

      <div className="card text-xs text-liberthia-300/60 italic">
        💡 Listas atualizam automaticamente quando você adiciona/remove. Ações usam comandos vanilla
        (op/deop/ban/pardon/whitelist) executados em op level 4.
      </div>
    </div>
  )
}
