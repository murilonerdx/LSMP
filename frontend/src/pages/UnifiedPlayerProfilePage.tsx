import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, VoiceClipDto, ChatEntry } from '../lib/api'
import { InventoryGrid } from '../components/InventoryGrid'
import { MatterEditor } from '../components/MatterEditor'

/**
 * Unified Player Profile — uma tela única com TUDO de um player.
 *
 * Substitui o PlayerDetail antigo (que tinha só inv+matter) e consolida:
 *  - Overview: posição, dimensão, vida, matter (DM/WM/YM), madness, AI profile
 *  - Inventory: grid de inventário
 *  - Photos: galeria de fotos tiradas pelo player
 *  - Voice: voice clips com transcrições
 *  - Chat: histórico de chat
 *  - Activity: snapshots, visits, eventos
 *
 * Rota: /profile/:uuid
 */

type Tab = 'overview' | 'inventory' | 'photos' | 'voice' | 'chat' | 'activity'

function fmtBytes(b: number): string {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  if (b < 1024 * 1024 * 1024) return `${(b / (1024 * 1024)).toFixed(1)}MB`
  return `${(b / (1024 * 1024 * 1024)).toFixed(2)}GB`
}

function fmtMs(ms: number): string {
  const s = ms / 1000
  return s < 1 ? `${ms}ms` : `${s.toFixed(1)}s`
}

function fmtDate(ts: number | string | null | undefined): string {
  if (!ts) return '—'
  const d = typeof ts === 'string' ? new Date(ts) : new Date(ts)
  return d.toLocaleString()
}

export function UnifiedPlayerProfilePage() {
  const { uuid } = useParams<{ uuid: string }>()
  const [tab, setTab] = useState<Tab>('overview')

  if (!uuid) return <p className="text-red-400">UUID missing</p>

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 10000 })
  const player = playersQ.data?.find(p => p.uuid === uuid)

  return (
    <div className="route-fade max-w-[1500px]">
      <Link to="/players" className="btn-ghost text-xs mb-3 inline-block">← Voltar</Link>

      {/* Header fixo com info essencial */}
      <div className="card-glow mb-4">
        <div className="flex items-start gap-4 flex-wrap">
          <img src={`https://mc-heads.net/avatar/${uuid}/96`}
            className="w-24 h-24 rounded-lg shadow-lg"
            alt={player?.name ?? 'player'} />
          <div className="flex-1 min-w-[200px]">
            <h1 className="text-3xl font-bold gradient-text">{player?.name ?? '(offline)'}</h1>
            <p className="text-[10px] text-liberthia-300/50 font-mono">{uuid}</p>
            {player ? (
              <div className="mt-2 flex flex-wrap gap-2 text-xs">
                <span className="badge badge-green">🌍 {player.dimension.replace('minecraft:', '')}</span>
                <span className="badge">❤ {player.health.toFixed(0)}/{player.maxHealth.toFixed(0)}</span>
                <span className="badge">🍗 {player.food}/20</span>
                <span className="badge badge-purple">⭐ L{player.level} ({player.xp.toFixed(0)}xp)</span>
                <span className="badge">🎮 {player.gameMode}</span>
                <span className="badge text-[10px] font-mono">
                  📍 {player.position.x.toFixed(0)}, {player.position.y.toFixed(0)}, {player.position.z.toFixed(0)}
                </span>
              </div>
            ) : (
              <p className="text-xs text-liberthia-300/50 mt-2 italic">Player offline</p>
            )}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-4 border-b border-liberthia-600 pb-1 overflow-x-auto">
        <TabBtn id="overview" current={tab} onClick={setTab}>👁 Overview</TabBtn>
        <TabBtn id="inventory" current={tab} onClick={setTab}>🎒 Inventário</TabBtn>
        <TabBtn id="photos" current={tab} onClick={setTab}>📸 Fotos</TabBtn>
        <TabBtn id="voice" current={tab} onClick={setTab}>🎙 Voz</TabBtn>
        <TabBtn id="chat" current={tab} onClick={setTab}>💬 Chat</TabBtn>
        <TabBtn id="activity" current={tab} onClick={setTab}>📊 Atividade</TabBtn>
      </div>

      {tab === 'overview' && <OverviewTab uuid={uuid} />}
      {tab === 'inventory' && <InventoryGrid uuid={uuid} />}
      {tab === 'photos' && <PhotosTab uuid={uuid} playerName={player?.name ?? ''} />}
      {tab === 'voice' && <VoiceTab uuid={uuid} playerName={player?.name ?? ''} />}
      {tab === 'chat' && <ChatTab uuid={uuid} />}
      {tab === 'activity' && <ActivityTab uuid={uuid} />}
    </div>
  )
}

function TabBtn({ id, current, onClick, children }:
  { id: Tab; current: Tab; onClick: (t: Tab) => void; children: React.ReactNode }) {
  const active = id === current
  return (
    <button
      onClick={() => onClick(id)}
      className={`px-3 py-1.5 text-xs rounded-t whitespace-nowrap transition-colors ${
        active
          ? 'bg-liberthia-500/30 text-white font-bold border-b-2 border-purple-400'
          : 'text-liberthia-300/70 hover:bg-liberthia-700/30'
      }`}>
      {children}
    </button>
  )
}

// ============ Overview Tab ============
function OverviewTab({ uuid }: { uuid: string }) {
  const matterQ = useQuery({
    queryKey: ['matter', uuid],
    queryFn: () => api.matter(uuid),
  })
  const madnessQ = useQuery({
    queryKey: ['madness-state'],
    queryFn: () => api.madnessState(),
  })

  const myMadness = madnessQ.data?.state?.find((s: any) => s.uuid === uuid)

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      {/* Matter */}
      <div className="card-glow">
        <h2 className="text-base font-bold gradient-text mb-2">⚛ Perfil de Matéria</h2>
        {matterQ.isLoading && <div className="text-xs text-liberthia-300/60">Carregando...</div>}
        {matterQ.data && (
          <div className="space-y-1 text-xs">
            <div className="flex justify-between"><span>🌑 Dark Matter (DM)</span><b className="text-red-300">{matterQ.data.dm}%</b></div>
            <div className="flex justify-between"><span>✨ White Matter (WM)</span><b className="text-emerald-300">{matterQ.data.wm}%</b></div>
            <div className="flex justify-between"><span>⚡ Yellow Matter (YM)</span><b className="text-amber-300">{matterQ.data.ym}%</b></div>
            <hr className="border-liberthia-500/20 my-2" />
            <div className="flex justify-between">
              <span className="text-liberthia-300/70">Tipo composto:</span>
              <span className="badge badge-purple">{matterQ.data.type}</span>
            </div>
          </div>
        )}
        <details className="mt-3">
          <summary className="cursor-pointer text-[10px] text-liberthia-300/60">▶ Editar valores</summary>
          <div className="mt-2"><MatterEditor uuid={uuid} /></div>
        </details>
      </div>

      {/* Madness */}
      <div className="card-glow">
        <h2 className="text-base font-bold gradient-text mb-2">🌀 Sanidade / Loucura</h2>
        {madnessQ.isLoading && <div className="text-xs text-liberthia-300/60">Carregando...</div>}
        {myMadness ? (
          <div className="space-y-2 text-xs">
            <div className="flex justify-between items-baseline">
              <span>Sanidade atual</span>
              <b className="text-2xl text-purple-300">{myMadness.sanity}<span className="text-xs text-liberthia-300/50">/100</span></b>
            </div>
            <div className="w-full bg-liberthia-900/60 rounded-full h-2 overflow-hidden">
              <div className="h-full bg-gradient-to-r from-red-500 via-amber-400 to-emerald-400 transition-all"
                style={{ width: `${myMadness.sanity}%` }} />
            </div>
            <div className="text-[10px] text-liberthia-300/50">
              {myMadness.sanity >= 80 ? '😌 estável' :
               myMadness.sanity >= 50 ? '😟 levemente perturbado' :
               myMadness.sanity >= 25 ? '😨 instável' : '💀 colapso iminente'}
            </div>
          </div>
        ) : (
          <div className="text-xs text-liberthia-300/60 italic">Sem dados de loucura pra esse player</div>
        )}
      </div>

    </div>
  )
}

// ============ Photos Tab ============
// HACK Opção B: mod hoje só manda authorName (não authorUuid).
// Buscamos TODAS as fotos e filtramos client-side por nome.
// TODO: quando o mod for atualizado pra mandar authorUuid, trocar
// pra api.photoLibrarySearch({ authorUuid: uuid, ... })
function PhotosTab({ uuid, playerName }: { uuid: string; playerName: string }) {
  const photosQ = useQuery({
    queryKey: ['photos-all', 'for-player', playerName],
    queryFn: () => api.photoLibrarySearch({ limit: 1000 }),
    enabled: !!playerName,
  })

  if (photosQ.isLoading) return <div className="text-xs text-liberthia-300/60">Carregando...</div>

  // Filtra client-side: tenta match por authorUuid (caso mod novo já mande)
  // OU por authorName (fallback pro mod antigo).
  const allPhotos = photosQ.data?.photos ?? []
  const nameLower = playerName.toLowerCase()
  const photos = allPhotos.filter((p: any) => {
    if (p.authorUuid === uuid) return true
    if (p.authorName && p.authorName.toLowerCase() === nameLower) return true
    return false
  })

  if (photos.length === 0) {
    return (
      <div className="card text-center py-12 text-liberthia-300/50">
        <div className="text-4xl mb-2 opacity-50">📸</div>
        <p>Sem fotos pra <b>{playerName || '(player desconhecido)'}</b> ainda.</p>
        <p className="text-[10px] mt-2">
          Mod escaneia <code>world/exposures/</code> a cada 30s e faz upload via POST /api/photos.
          Filename precisa começar com o nome do player (<code>{playerName}_*.png</code>).
        </p>
        <p className="text-[10px] mt-1 text-liberthia-300/40">
          Total no servidor: {allPhotos.length} foto(s).
        </p>
      </div>
    )
  }

  return (
    <div>
      <div className="text-xs text-liberthia-300/60 mb-2">
        {photos.length} foto(s) deste player · clica pra ver maior
        <span className="ml-2 text-[10px] text-liberthia-300/40">
          ({allPhotos.length} totais no servidor, filtradas por nome)
        </span>
      </div>
      <div className="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2">
        {photos.map((p: any) => (
          <a key={p.id} href={`/api/photos/${p.id}/image`} target="_blank" rel="noreferrer"
             className="relative group rounded overflow-hidden bg-liberthia-900/40 aspect-square hover:ring-2 hover:ring-purple-400 transition-all">
            <img src={`/api/photos/${p.id}/image`}
                 className="w-full h-full object-cover"
                 alt={p.title ?? '?'}
                 loading="lazy" />
            {p.cursed && (
              <span className="absolute top-1 right-1 text-base">💀</span>
            )}
            <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-1 text-[9px] opacity-0 group-hover:opacity-100 transition-opacity">
              <div className="truncate">{p.title || `#${p.id}`}</div>
              <div className="text-liberthia-300/60">{fmtDate(p.takenAt)}</div>
            </div>
          </a>
        ))}
      </div>
    </div>
  )
}

// ============ Voice Tab ============
function VoiceTab({ uuid, playerName: _playerName }: { uuid: string; playerName: string }) {
  const qc = useQueryClient()
  const voiceQ = useQuery({
    queryKey: ['voice-by-player', uuid],
    queryFn: () => api.voiceList(uuid, 200),
    refetchInterval: 10000,
  })

  if (voiceQ.isLoading) return <div className="text-xs text-liberthia-300/60">Carregando...</div>
  const clips = voiceQ.data?.clips ?? []

  if (clips.length === 0) {
    return (
      <div className="card text-center py-12 text-liberthia-300/50">
        <div className="text-4xl mb-2 opacity-50">🎙</div>
        <p>Esse player ainda não falou no Voice Chat.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="text-xs text-liberthia-300/60 mb-2 flex items-center gap-2">
        <span>{clips.length} clipe(s)</span>
        <span>·</span>
        <span>{clips.filter((c: VoiceClipDto) => c.transcriptionStatus === 'DONE').length} transcritos</span>
      </div>
      <div className="space-y-1 max-h-[70vh] overflow-y-auto">
        {clips.map((c: VoiceClipDto) => (
          <div key={c.id} className="card text-xs py-2 px-3 hover:bg-liberthia-500/10">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-[10px] text-liberthia-300/50 font-mono">{new Date(c.ts).toLocaleTimeString()}</span>
              <span className="text-[10px] text-liberthia-300/50">{fmtMs(c.durationMs)} · {fmtBytes(c.sizeBytes)}</span>
              {c.transcriptionStatus === 'DONE' && <span className="text-emerald-300 text-[10px]">✓</span>}
              {c.transcriptionStatus === 'PENDING' && <span className="text-amber-300 animate-pulse text-[10px]">⏳</span>}
              <button className="ml-auto btn-ghost btn-sm text-[10px]"
                onClick={() => {
                  if (!confirm('Deletar?')) return
                  api.voiceDelete(c.id).then(() => {
                    qc.invalidateQueries({ queryKey: ['voice-by-player', uuid] })
                    qc.invalidateQueries({ queryKey: ['voice-clips'] })
                    qc.invalidateQueries({ queryKey: ['voice-library-players'] })
                    qc.invalidateQueries({ queryKey: ['voice-library-clips'] })
                  })
                }}>🗑</button>
            </div>
            {c.transcription && (
              <div className="italic text-liberthia-300/80 mb-1">"{c.transcription}"</div>
            )}
            <audio controls className="w-full h-8" preload="none" src={api.voiceAudioUrl(c.id)} />
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ Chat Tab ============
function ChatTab({ uuid }: { uuid: string }) {
  const chatQ = useQuery({
    queryKey: ['chat-by-player', uuid],
    queryFn: () => api.chatHistory(uuid, 0, 500),
    refetchInterval: 5000,
  })

  if (chatQ.isLoading) return <div className="text-xs text-liberthia-300/60">Carregando...</div>
  const entries = chatQ.data ?? []

  if (entries.length === 0) {
    return (
      <div className="card text-center py-12 text-liberthia-300/50">
        <div className="text-4xl mb-2 opacity-50">💬</div>
        <p>Sem mensagens de chat ainda.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="text-xs text-liberthia-300/60 mb-2">{entries.length} mensagem(ns)</div>
      <div className="space-y-1 max-h-[70vh] overflow-y-auto card-glow !p-2">
        {entries.map((e: ChatEntry, i: number) => (
          <div key={i} className="text-xs px-2 py-1 hover:bg-liberthia-500/10 rounded">
            <span className="text-[10px] text-liberthia-300/50 font-mono mr-2">{new Date(e.ts).toLocaleString()}</span>
            <span>{e.message}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ Activity Tab ============
function ActivityTab({ uuid }: { uuid: string }) {
  const visitsQ = useQuery({
    queryKey: ['player-visits', uuid],
    queryFn: () => api.playerVisitsDetail(uuid),
    retry: false,
  })

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div className="card-glow">
        <h3 className="font-bold mb-2">📊 Visitas / Sessões</h3>
        {visitsQ.isLoading && <div className="text-xs text-liberthia-300/60">Carregando...</div>}
        {visitsQ.isError && <div className="text-xs text-liberthia-300/60 italic">Endpoint não disponível</div>}
        {visitsQ.data && (
          <pre className="text-[10px] text-liberthia-300/70 overflow-x-auto">
            {JSON.stringify(visitsQ.data, null, 2)}
          </pre>
        )}
      </div>

      <div className="card-glow">
        <h3 className="font-bold mb-2">🪦 Cemitério / Mortes</h3>
        <p className="text-xs text-liberthia-300/60 italic">
          Histórico de mortes está em Cemitério. Filtra por esse player lá pra ver detalhes.
        </p>
        <Link to="/cemetery" className="btn-ghost text-xs mt-2 inline-block">→ Abrir Cemitério</Link>
      </div>
    </div>
  )
}
