import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ScPlayerInfo } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Painel SecurityCraft — controle remoto dos blocos protegidos e gerenciamento
 * de items (keycards, monitors, RATs) por player.
 *
 * Reusa /api/player/{uuid}/inventory pra contar items "securitycraft:*" no
 * inventário de cada player online — assim sabe quem tem o quê SEM precisar
 * mexer no mod SecurityCraft.
 */

const KEYCARDS = [
  { id: 'securitycraft:keycard_lv1', label: 'Keycard Lv1', emoji: '🔑' },
  { id: 'securitycraft:keycard_lv2', label: 'Keycard Lv2', emoji: '🔑' },
  { id: 'securitycraft:keycard_lv3', label: 'Keycard Lv3', emoji: '🔑' },
  { id: 'securitycraft:keycard_lv4', label: 'Keycard Lv4', emoji: '🔑' },
  { id: 'securitycraft:keycard_lv5', label: 'Keycard Lv5', emoji: '🔑' },
  { id: 'securitycraft:limited_use_keycard', label: 'Limited Keycard', emoji: '🎫' },
]
const TOOLS = [
  { id: 'securitycraft:universal_key_changer', label: 'Key Changer', emoji: '🔧' },
  { id: 'securitycraft:camera_monitor', label: 'Camera Monitor', emoji: '📺' },
  { id: 'securitycraft:remote_access_tool', label: 'Remote Access (geral)', emoji: '📡' },
  { id: 'securitycraft:remote_access_sentry', label: 'Remote Sentry', emoji: '🎯' },
  { id: 'securitycraft:remote_access_mine', label: 'Remote Mine', emoji: '💣' },
  { id: 'securitycraft:briefcase', label: 'Briefcase', emoji: '💼' },
]

export function SecurityCraftPage() {
  const qc = useQueryClient()
  const [selected, setSelected] = useState<ScPlayerInfo | null>(null)

  const playersQ = useQuery({
    queryKey: ['sc-players'],
    queryFn: api.scPlayers,
    refetchInterval: 8000,
  })

  const listMut = useMutation({
    mutationFn: api.scListBlocks,
    onSuccess: (r) => toast.ok('📋 ' + (r.note ?? 'comando enviado')),
    onError: (e: any) => toast.err(e.message),
  })
  const resetMut = useMutation({
    mutationFn: api.scResetBlocks,
    onSuccess: () => toast.ok('↺ blocos resetados'),
    onError: (e: any) => toast.err(e.message),
  })
  const giveMut = useMutation({
    mutationFn: ({ playerName, itemId, count }: { playerName: string; itemId: string; count: number }) =>
      api.scGiveItem(playerName, itemId, count),
    onSuccess: () => { toast.ok('🎁 entregue'); qc.invalidateQueries({ queryKey: ['sc-players'] }) },
    onError: (e: any) => toast.err(e.message),
  })

  const players = Array.isArray(playersQ.data?.players) ? playersQ.data!.players : []

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4">
        <h1 className="page-title">🔐 SecurityCraft</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Painel de controle dos blocos protegidos (câmeras, keypads, mines, sentries) e items
          (keycards, monitors, RATs). Lista quem tem o quê — cruza inventário em tempo real.
        </p>
      </header>

      <div className="grid grid-cols-[260px_1fr] gap-3">

        {/* Sidebar: players online ordenados por quantos items SC têm */}
        <aside className="card-glow p-2 space-y-1 max-h-[80vh] overflow-y-auto">
          <div className="text-[10px] uppercase tracking-widest text-liberthia-300/50 mb-1 px-1">
            {players.length} online · ordenado por items SC
          </div>
          {players.map(p => (
            <button key={p.uuid}
              onClick={() => setSelected(p)}
              className={`w-full text-left rounded p-2 flex items-center gap-2 transition ${
                selected?.uuid === p.uuid
                  ? 'bg-purple-500/30 ring-2 ring-purple-400'
                  : 'bg-liberthia-900/40 hover:bg-liberthia-900/70'
              }`}>
              <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.name)}/28`}
                className="rounded shrink-0"
                onError={(ev) => { (ev.target as HTMLImageElement).style.display = 'none' }} />
              <div className="flex-1 min-w-0">
                <div className="text-sm font-bold truncate">{p.name}</div>
                <div className="text-[10px] text-liberthia-300/60">
                  {p.scItemCount > 0 ? `🔑 ${p.scItemCount} item${p.scItemCount === 1 ? '' : 's'} SC` : 'sem items SC'}
                </div>
              </div>
            </button>
          ))}
          {!playersQ.isLoading && players.length === 0 && (
            <div className="text-center text-xs text-liberthia-300/60 py-4">Nenhum player online.</div>
          )}
        </aside>

        {/* Detalhes do player selecionado */}
        <main>
          {!selected ? (
            <div className="card text-center py-16">
              <div className="text-5xl mb-3 opacity-50">🔐</div>
              <p className="text-liberthia-300/70">Selecione um player na sidebar.</p>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="card-glow flex items-center gap-3">
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(selected.name)}/64`} className="rounded" />
                <div className="flex-1">
                  <div className="text-2xl font-bold">{selected.name}</div>
                  <div className="text-xs text-liberthia-300/60 mt-1">{selected.dimension}</div>
                </div>
                <div className="flex flex-col gap-1">
                  <button className="btn-ghost btn-sm" onClick={() => listMut.mutate(selected.name)}>
                    📋 /sc list_owned_blocks
                  </button>
                  <button className="btn-danger btn-sm"
                    onClick={() => {
                      if (confirm(`Resetar TODOS os blocos SC de ${selected.name} pro dono "Server"?`))
                        resetMut.mutate(selected.name)
                    }}>↺ Reset all blocks</button>
                </div>
              </div>

              {/* Items SC que ele tem */}
              <div className="card">
                <h3 className="font-bold mb-2 text-sm">🎒 Items SecurityCraft no inventário</h3>
                {Object.keys(selected.scItems).length === 0 ? (
                  <div className="text-xs text-liberthia-300/50">Sem items SC.</div>
                ) : (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-1.5 text-xs">
                    {Object.entries(selected.scItems).map(([id, count]) => (
                      <div key={id} className="rounded bg-liberthia-900/60 p-2 flex items-center gap-2">
                        <span className="text-lg">🔑</span>
                        <div className="flex-1 min-w-0">
                          <div className="font-bold truncate">{id.replace('securitycraft:', '')}</div>
                          <div className="text-[10px] text-liberthia-300/50">{count}x</div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Dar items SC */}
              <div className="card">
                <h3 className="font-bold mb-2 text-sm">🎁 Dar item SecurityCraft</h3>
                <div className="text-[10px] text-liberthia-300/50 uppercase tracking-widest mb-1">Keycards</div>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-1.5 mb-3">
                  {KEYCARDS.map(k => (
                    <button key={k.id} className="rounded bg-liberthia-900/60 hover:bg-purple-500/20 p-2 text-xs flex items-center gap-2"
                      onClick={() => giveMut.mutate({ playerName: selected.name, itemId: k.id, count: 1 })}>
                      <span className="text-base">{k.emoji}</span>
                      <span className="font-bold flex-1 text-left">{k.label}</span>
                    </button>
                  ))}
                </div>
                <div className="text-[10px] text-liberthia-300/50 uppercase tracking-widest mb-1">Tools</div>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-1.5">
                  {TOOLS.map(k => (
                    <button key={k.id} className="rounded bg-liberthia-900/60 hover:bg-purple-500/20 p-2 text-xs flex items-center gap-2"
                      onClick={() => giveMut.mutate({ playerName: selected.name, itemId: k.id, count: 1 })}>
                      <span className="text-base">{k.emoji}</span>
                      <span className="font-bold flex-1 text-left">{k.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="card text-[11px] text-liberthia-300/60 italic">
                <p className="mb-2"><strong>Notas:</strong></p>
                <p>· O <code>/sc list_owned_blocks</code> faz output no console do servidor + chat dos ops online.</p>
                <p>· Pra <code>/sc set_owner</code> e <code>/sc unlock</code> é necessário raycast — admin precisa estar olhando pro bloco. Use o botão de TP pra coord se souber onde está.</p>
                <p>· Camera Monitor + Remote Access Tool funcionam mesmo dados via painel — só precisa o player abrir e configurar in-game.</p>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
