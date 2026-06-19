import { ReactNode, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, VoiceClipDto, VoicePlayerSummary } from '../lib/api'
import { toast } from '../store/toast'
import { WhisperControlPanel } from '../components/WhisperControlPanel'

/**
 * Voice Library — banco de áudios organizado por player, com modo "biblioteca de pastas".
 *
 * Modo 1 (tela inicial): grid de PLAYERS, cada um é uma "pasta" com avatar,
 * contagem de clipes, duração total e bytes totais. Click → entra na pasta.
 *
 * Modo 2 (selecionou um player): lista de clipes daquele player, com filtros
 * de duração / tamanho / data e ordenação. Botão "← Voltar" retorna ao modo 1.
 *
 * Diferente do VoiceArchivePage (que é foco em edição inline e disparo no jogo),
 * essa página é foco em NAVEGAÇÃO e BIBLIOTECA — ouvir, baixar, descobrir
 * conteúdo de cada player.
 */

function fmtDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  const rs = s % 60
  if (m < 60) return `${m}m ${rs}s`
  const h = Math.floor(m / 60)
  return `${h}h ${m % 60}m`
}

function fmtBytes(b: number): string {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  if (b < 1024 * 1024 * 1024) return `${(b / (1024 * 1024)).toFixed(1)}MB`
  return `${(b / (1024 * 1024 * 1024)).toFixed(2)}GB`
}

function fmtDateShort(ts: number): string {
  if (!ts) return '—'
  return new Date(ts).toLocaleDateString()
}

/**
 * Recorta trecho da transcrição em volta do match. Retorna texto curto
 * com a palavra-chave destacada em negrito amarelo (~80 chars de contexto).
 */
function highlightMatch(text: string, query: string): ReactNode {
  if (!text || !query) return text
  const lower = text.toLowerCase()
  const q = query.toLowerCase()
  const idx = lower.indexOf(q)
  if (idx === -1) {
    // Sem match — devolve texto truncado
    return text.length > 120 ? text.slice(0, 120) + '…' : text
  }
  // Pega ~50 chars antes + match + ~50 chars depois
  const start = Math.max(0, idx - 50)
  const end = Math.min(text.length, idx + query.length + 50)
  const before = (start > 0 ? '…' : '') + text.slice(start, idx)
  const match = text.slice(idx, idx + query.length)
  const after = text.slice(idx + query.length, end) + (end < text.length ? '…' : '')
  return (
    <>
      {before}
      <span className="bg-amber-500/40 text-amber-100 font-bold px-0.5 rounded">{match}</span>
      {after}
    </>
  )
}

type SortMode = 'recent' | 'oldest' | 'longest' | 'shortest' | 'biggest' | 'smallest'

const SORT_LABELS: Record<SortMode, string> = {
  recent: '🕒 Mais recentes',
  oldest: '⏳ Mais antigos',
  longest: '⏱ Mais longos',
  shortest: '⚡ Mais curtos',
  biggest: '📦 Maiores',
  smallest: '🥚 Menores',
}

export function VoiceLibraryPage() {
  const qc = useQueryClient()
  // selectedPlayerBase: snapshot inicial do player quando clicou pra entrar
  //   (preserva uuid + name como fallback caso desapareça do allPlayers, ex:
  //    quando todos os clipes daquele player são deletados).
  // selectedPlayer (derivado abaixo): merge do snapshot com a versão MAIS
  //   RECENTE de allPlayers, garantindo que os badges (clipCount, duração,
  //   tamanho) reflitam o estado atual após deleções.
  const [selectedPlayerBase, setSelectedPlayerBase] = useState<VoicePlayerSummary | null>(null)
  const [sortMode, setSortMode] = useState<SortMode>('recent')
  const [minDurSec, setMinDurSec] = useState(0)
  const [maxDurSec, setMaxDurSec] = useState(0)
  const [minKB, setMinKB] = useState(0)
  const [maxKB, setMaxKB] = useState(0)
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<Set<number>>(new Set())
  // Busca GLOBAL por texto na transcrição — usa endpoint backend que
  // pesquisa em TODOS os clipes de TODOS os players. Diferente do `search`
  // local que só filtra após carregar a lista do player.
  const [transcriptionSearch, setTranscriptionSearch] = useState('')
  const [transcriptionSearchActive, setTranscriptionSearchActive] = useState('')

  const playersQ = useQuery({
    queryKey: ['voice-library-players'],
    queryFn: api.voiceLibraryPlayers,
    // Refetch agressivo: 5s + sempre que a página recebe foco (volta de outra
    // aba) ou o componente remonta. Garante que totais (clipes, tempo, tamanho)
    // refletem deleções feitas em outras telas (Voice Archive, Conversations, etc).
    refetchInterval: 5_000,
    refetchOnWindowFocus: true,
    refetchOnMount: 'always',
    staleTime: 0,
  })

  /**
   * Busca GLOBAL — só roda quando transcriptionSearchActive está setada
   * (botão "Buscar" foi clicado). Retorna players com matches + contagem.
   */
  const transcriptionQ = useQuery({
    enabled: transcriptionSearchActive.trim().length >= 2,
    queryKey: ['voice-transcription-search', transcriptionSearchActive],
    queryFn: () => api.voiceSearchTranscription(transcriptionSearchActive.trim(), 500),
  })

  // Quando entra na pasta, busca clipes com filtros.
  // Usa selectedPlayerBase (não selectedPlayer derivado) pra evitar ordem-de-
  // declaração — selectedPlayer é computado depois (precisa de allPlayers).
  const clipsQ = useQuery({
    enabled: !!selectedPlayerBase,
    queryKey: ['voice-library-clips', selectedPlayerBase?.playerUuid, sortMode,
               minDurSec, maxDurSec, minKB, maxKB, fromDate, toDate],
    queryFn: () => api.voiceLibrarySearch({
      playerUuid: selectedPlayerBase!.playerUuid,
      sort: sortMode,
      minDurationMs: minDurSec * 1000,
      maxDurationMs: maxDurSec * 1000,
      minBytes: minKB * 1024,
      maxBytes: maxKB * 1024,
      fromTs: fromDate ? new Date(fromDate).getTime() : 0,
      toTs: toDate ? new Date(toDate).getTime() + 86_400_000 : 0,  // inclui o dia inteiro
      limit: 500,
    }),
    refetchInterval: 5_000,
  })

  // Filtro de busca textual local (transcrição/nome do arquivo)
  const allPlayers = Array.isArray(playersQ.data?.players) ? playersQ.data!.players : []
  const players = useMemo(() => {
    if (!search.trim()) return allPlayers
    const q = search.trim().toLowerCase()
    return allPlayers.filter(p => p.playerName?.toLowerCase().includes(q))
  }, [allPlayers, search])

  // selectedPlayer = derivado. Sempre usa a versão MAIS RECENTE do player
  // (vinda do refresh automático de allPlayers). Se o player sumiu do
  // allPlayers (todos os clipes foram deletados), retorna selectedPlayerBase
  // mas com clipCount/totalDurationMs/totalBytes zerados — refletindo a
  // realidade atual em vez do snapshot antigo.
  const selectedPlayer: VoicePlayerSummary | null = useMemo(() => {
    if (!selectedPlayerBase) return null
    const fresh = allPlayers.find(p => p.playerUuid === selectedPlayerBase.playerUuid)
    if (fresh) return fresh
    // Player sumiu da lista global → todos os clipes foram deletados.
    // Devolve o snapshot original mas com totais zerados, pra UI refletir.
    return {
      ...selectedPlayerBase,
      clipCount: 0,
      totalDurationMs: 0,
      totalBytes: 0,
    }
  }, [selectedPlayerBase, allPlayers])

  const clipsAll = Array.isArray(clipsQ.data?.clips) ? clipsQ.data!.clips : []
  const clips = useMemo(() => {
    if (!search.trim() || !selectedPlayer) return clipsAll
    const q = search.trim().toLowerCase()
    return clipsAll.filter(c =>
      c.transcription?.toLowerCase().includes(q) ||
      String(c.id).includes(q)
    )
  }, [clipsAll, search, selectedPlayer])

  function resetFilters() {
    setSortMode('recent')
    setMinDurSec(0); setMaxDurSec(0)
    setMinKB(0); setMaxKB(0)
    setFromDate(''); setToDate('')
    setSearch('')
    setSelected(new Set())
  }
  function toggleSel(id: number) {
    setSelected(s => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n })
  }
  function selectAll() {
    setSelected(new Set(clips.map(c => c.id)))
  }
  function clearSel() { setSelected(new Set()) }

  const protectMut = useMutation({
    mutationFn: ({ id, on }: { id: number; on: boolean }) =>
      on ? api.voiceProtect(id) : api.voiceUnprotect(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['voice-library-clips'] }),
  })
  const bulkProtectMut = useMutation({
    mutationFn: ({ ids, protect }: { ids: number[]; protect: boolean }) =>
      api.voiceBulkProtect(ids, protect, protect ? 'bulk-protected' : ''),
    onSuccess: (r) => {
      toast.ok(`🛡 ${r.updated} clipe${r.updated === 1 ? '' : 's'} ${r.updated > 0 ? 'atualizado' : 'sem mudança'}`)
      setSelected(new Set())
      qc.invalidateQueries({ queryKey: ['voice-library-clips'] })
    },
  })

  // -------- TELA 1: grid de players --------
  if (!selectedPlayer) {
    const grandTotal = allPlayers.reduce((acc, p) => ({
      clips: acc.clips + p.clipCount,
      ms: acc.ms + p.totalDurationMs,
      bytes: acc.bytes + p.totalBytes,
    }), { clips: 0, ms: 0, bytes: 0 })

    return (
      <div className="route-fade max-w-[1600px]">
        <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
          <div>
            <h1 className="page-title">🎙 Voice Library</h1>
            <p className="text-sm text-liberthia-300/70 mt-1">
              Biblioteca de áudios capturados via Simple Voice Chat. Cada player é uma "pasta".
              Click pra ver todos os clipes dele com filtros de duração, tamanho e data.
            </p>
          </div>
          <div className="flex gap-2 items-center text-xs">
            <span className="badge badge-purple">{allPlayers.length} players</span>
            <span className="badge badge-purple">{grandTotal.clips} clipes</span>
            <span className="badge badge-purple">{fmtDuration(grandTotal.ms)}</span>
            <span className="badge badge-purple">{fmtBytes(grandTotal.bytes)}</span>
            <button
              onClick={() => {
                qc.invalidateQueries({ queryKey: ['voice-library-players'] })
                qc.invalidateQueries({ queryKey: ['voice-library-clips'] })
                toast.ok('Atualizando…')
              }}
              className="btn-ghost btn-sm text-xs"
              title="Recalcular totais (clipes / tempo / tamanho)">
              🔄 Atualizar
            </button>
          </div>
        </header>

        {/* Linha 1: filtro local por NOME (rápido, sem chamada backend) */}
        <input
          value={search} onChange={(e) => setSearch(e.target.value)}
          placeholder="Filtrar player por nome…"
          className="input mb-2"
        />

        {/* Linha 2: busca GLOBAL por TEXTO em transcrições. Chama backend
            que percorre TODAS as transcrições e retorna só players com hit. */}
        <div className="flex gap-2 mb-4">
          <input
            value={transcriptionSearch}
            onChange={(e) => setTranscriptionSearch(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && transcriptionSearch.trim().length >= 2) {
                setTranscriptionSearchActive(transcriptionSearch.trim())
              }
            }}
            placeholder="🔍 Buscar texto na conversa (ex: 'matéria', 'me ajuda', 'avador')…"
            className="input flex-1"
          />
          <button
            className="btn btn-primary"
            disabled={transcriptionSearch.trim().length < 2}
            onClick={() => setTranscriptionSearchActive(transcriptionSearch.trim())}>
            🔍 Buscar
          </button>
          {transcriptionSearchActive && (
            <button className="btn-ghost"
              onClick={() => { setTranscriptionSearch(''); setTranscriptionSearchActive('') }}>
              ✕ Limpar
            </button>
          )}
        </div>

        {/* Resultados da busca por transcrição — substitui grid quando ativo */}
        {transcriptionSearchActive && (
          <div className="mb-4 card-glow">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold">
                🔍 Resultados pra <span className="text-purple-300">"{transcriptionSearchActive}"</span>
              </h3>
              {transcriptionQ.data && (
                <span className="badge badge-purple">
                  {transcriptionQ.data.players.length} player{transcriptionQ.data.players.length === 1 ? '' : 's'} •{' '}
                  {transcriptionQ.data.count} clipe{transcriptionQ.data.count === 1 ? '' : 's'}
                </span>
              )}
            </div>

            {transcriptionQ.isLoading && (
              <div className="text-xs text-liberthia-300/60 py-4 text-center">Buscando…</div>
            )}

            {transcriptionQ.data && transcriptionQ.data.players.length === 0 && (
              <div className="text-xs text-liberthia-300/60 py-6 text-center italic">
                Nenhuma transcrição contém essa palavra. Tenta sinônimos ou aguarde mais transcrições.
              </div>
            )}

            {transcriptionQ.data && transcriptionQ.data.players.length > 0 && (
              <div className="space-y-2">
                {transcriptionQ.data.players.map(match => {
                  const player = allPlayers.find(p => p.playerUuid === match.playerUuid)
                  const clipsForPlayer = (transcriptionQ.data?.clips ?? [])
                    .filter(c => c.playerUuid === match.playerUuid)
                  return (
                    <div key={match.playerUuid} className="bg-liberthia-900/40 rounded p-2 space-y-1">
                      <button
                        onClick={() => player && setSelectedPlayerBase(player)}
                        className="w-full flex items-center gap-2 hover:bg-liberthia-900/60 rounded p-1">
                        <img
                          src={`https://mc-heads.net/avatar/${encodeURIComponent(match.playerName || '?')}/32`}
                          className="rounded shrink-0" />
                        <div className="flex-1 text-left min-w-0">
                          <div className="font-bold">{match.playerName}</div>
                          <div className="text-[10px] text-liberthia-300/60">
                            {match.matchCount} clipe{match.matchCount === 1 ? '' : 's'} com match •
                            último: {fmtDateShort(match.latestTs)}
                          </div>
                        </div>
                        <span className="text-xs text-purple-300">→ abrir pasta</span>
                      </button>
                      {/* Preview do trecho (primeiros 2 clipes) */}
                      <div className="pl-10 space-y-1">
                        {clipsForPlayer.slice(0, 2).map(c => (
                          <div key={c.id} className="text-[11px] text-liberthia-300/70 italic">
                            <span className="text-amber-300">▸</span>{' '}
                            "{highlightMatch(c.transcription || '', transcriptionSearchActive)}"
                            <span className="text-[9px] text-liberthia-300/40 ml-1">
                              ({fmtDateShort(c.ts)} · {fmtDuration(c.durationMs)})
                            </span>
                          </div>
                        ))}
                        {clipsForPlayer.length > 2 && (
                          <div className="text-[10px] text-liberthia-300/40">
                            … e mais {clipsForPlayer.length - 2} clipe{clipsForPlayer.length - 2 === 1 ? '' : 's'}
                          </div>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}

        {playersQ.isLoading && (
          <div className="card text-center py-12">Carregando…</div>
        )}

        {!playersQ.isLoading && players.length === 0 && (
          <div className="card text-center py-12">
            <div className="text-5xl mb-3 opacity-50">🎙</div>
            <p className="text-liberthia-300/70 text-sm">
              {allPlayers.length === 0
                ? 'Sem clipes ainda. Quando alguém falar no Voice Chat, vai aparecer aqui.'
                : 'Nenhum player bate com a busca.'}
            </p>
          </div>
        )}

        {/* Painel de controle do Whisper — sempre visível na tela de players.
            Permite pausar transcrição quando CPU tá sobrecarregando, agendar
            horário de funcionamento, ou ajustar threads/beam em runtime. */}
        <div className="mb-4">
          <WhisperControlPanel />
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3">
          {players.map((p) => (
            <button key={p.playerUuid}
              onClick={() => setSelectedPlayerBase(p)}
              className="card-glow text-left hover:scale-[1.02] hover:ring-2 hover:ring-purple-400/40 transition cursor-pointer">
              <div className="flex items-center gap-2 mb-2">
                <img
                  src={`https://mc-heads.net/avatar/${encodeURIComponent(p.playerName || p.playerUuid.slice(0, 8))}/40`}
                  className="rounded"
                  onError={(ev) => { (ev.target as HTMLImageElement).style.display = 'none' }} />
                <div className="flex-1 min-w-0">
                  <div className="font-bold truncate">{p.playerName || p.playerUuid.slice(0, 8)}</div>
                  <div className="text-[10px] text-liberthia-300/50 truncate font-mono">{p.playerUuid.slice(0, 8)}…</div>
                </div>
              </div>
              <div className="text-2xl mb-1 text-purple-300">📁 {p.clipCount}</div>
              <div className="text-[11px] text-liberthia-300/70 space-y-0.5">
                <div>🕒 {fmtDuration(p.totalDurationMs)}</div>
                <div>📦 {fmtBytes(p.totalBytes)}</div>
                <div className="text-liberthia-300/50">
                  {fmtDateShort(p.firstClipTs)} → {fmtDateShort(p.lastClipTs)}
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    )
  }

  // -------- TELA 2: clipes do player selecionado --------
  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <button className="btn-ghost" onClick={() => { setSelectedPlayerBase(null); resetFilters() }}>
            ← Voltar
          </button>
          <img
            src={`https://mc-heads.net/avatar/${encodeURIComponent(selectedPlayer.playerName || selectedPlayer.playerUuid.slice(0, 8))}/48`}
            className="rounded" />
          <div>
            <h1 className="page-title">📁 {selectedPlayer.playerName}</h1>
            <p className="text-xs text-liberthia-300/60 font-mono">{selectedPlayer.playerUuid}</p>
          </div>
        </div>
        <div className="flex gap-2 text-xs items-center flex-wrap">
          <span className="badge badge-purple">{selectedPlayer.clipCount} total</span>
          <span className="badge badge-purple">{fmtDuration(selectedPlayer.totalDurationMs)}</span>
          <span className="badge badge-purple">{fmtBytes(selectedPlayer.totalBytes)}</span>
          <a className="btn-ghost btn-sm" href={api.voiceZipUrl(selectedPlayer.playerUuid)}
            download={`voice-clips-${selectedPlayer.playerName || selectedPlayer.playerUuid.slice(0, 8)}.zip`}>
            📦 Baixar ZIP
          </a>
        </div>
      </header>

      {/* Filtros */}
      <div className="card mb-4 space-y-3">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
          <div>
            <label className="label">Ordenar por</label>
            <select className="input" value={sortMode}
              onChange={(e) => setSortMode(e.target.value as SortMode)}>
              {Object.entries(SORT_LABELS).map(([k, v]) =>
                <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Buscar (transcrição/id)</label>
            <input className="input" value={search} onChange={(e) => setSearch(e.target.value)}
              placeholder="ex: morte, oi…" />
          </div>
          <div className="col-span-2 md:col-span-1">
            <label className="label">Duração (segundos)</label>
            <div className="flex gap-1 items-center">
              <input type="number" min={0} className="input" placeholder="min"
                value={minDurSec || ''} onChange={(e) => setMinDurSec(Number(e.target.value) || 0)} />
              <span>→</span>
              <input type="number" min={0} className="input" placeholder="max"
                value={maxDurSec || ''} onChange={(e) => setMaxDurSec(Number(e.target.value) || 0)} />
            </div>
          </div>
          <div className="col-span-2 md:col-span-1">
            <label className="label">Tamanho (KB)</label>
            <div className="flex gap-1 items-center">
              <input type="number" min={0} className="input" placeholder="min"
                value={minKB || ''} onChange={(e) => setMinKB(Number(e.target.value) || 0)} />
              <span>→</span>
              <input type="number" min={0} className="input" placeholder="max"
                value={maxKB || ''} onChange={(e) => setMaxKB(Number(e.target.value) || 0)} />
            </div>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-2 text-xs items-end">
          <div>
            <label className="label">Data — de</label>
            <input type="date" className="input" value={fromDate}
              onChange={(e) => setFromDate(e.target.value)} />
          </div>
          <div>
            <label className="label">Data — até</label>
            <input type="date" className="input" value={toDate}
              onChange={(e) => setToDate(e.target.value)} />
          </div>
          <button className="btn-ghost" onClick={resetFilters}>↺ Resetar filtros</button>
        </div>
      </div>

      <div className="flex items-center justify-between mb-2 flex-wrap gap-2">
        <div className="text-xs text-liberthia-300/60">
          {clipsQ.isLoading
            ? 'Carregando…'
            : `${clips.length} clipe${clips.length === 1 ? '' : 's'} ${clips.length !== clipsAll.length ? `(de ${clipsAll.length})` : ''}`}
        </div>
        <div className="flex gap-1 text-xs items-center">
          <button className="btn-ghost btn-sm" onClick={selectAll}>
            ☑ Selecionar todos visíveis
          </button>
          {selected.size > 0 && (
            <>
              <span className="badge badge-amber">{selected.size} selecionado{selected.size === 1 ? '' : 's'}</span>
              <button className="btn btn-sm"
                onClick={() => bulkProtectMut.mutate({ ids: Array.from(selected), protect: true })}>
                🛡 Proteger
              </button>
              <button className="btn-ghost btn-sm"
                onClick={() => bulkProtectMut.mutate({ ids: Array.from(selected), protect: false })}>
                🔓 Desproteger
              </button>
              <button className="btn-ghost btn-sm" onClick={clearSel}>✕</button>
            </>
          )}
        </div>
      </div>

      <div className="card mb-3 text-[11px] text-liberthia-300/60">
        🛡 Clipes protegidos NÃO são apagados pela auto-cleanup (14 dias). Marque os memoráveis.
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-2">
        {clips.map((c) => <ClipRow key={c.id} clip={c}
          selected={selected.has(c.id)}
          onToggleSel={() => toggleSel(c.id)}
          onToggleProtect={() => protectMut.mutate({ id: c.id, on: !c.protectedFromCleanup })} />)}
        {!clipsQ.isLoading && clips.length === 0 && (
          <div className="card text-center py-12 col-span-full text-liberthia-300/70">
            Nenhum clipe casa com os filtros.
          </div>
        )}
      </div>
    </div>
  )
}

function ClipRow({ clip, selected, onToggleSel, onToggleProtect }: {
  clip: VoiceClipDto
  selected: boolean
  onToggleSel: () => void
  onToggleProtect: () => void
}) {
  const qc = useQueryClient()
  return (
    <div className={`card flex items-center gap-2 p-3 ${selected ? 'ring-1 ring-amber-400' : ''} ${
      clip.protectedFromCleanup ? 'border-l-2 border-l-emerald-400' : ''
    }`}>
      <input type="checkbox" checked={selected} onChange={onToggleSel} className="shrink-0" />
      <div className="text-center min-w-[60px]">
        <div className="text-[10px] text-liberthia-300/50">#{clip.id}</div>
        <div className="text-xs font-mono">{fmtDuration(clip.durationMs)}</div>
        <div className="text-[10px] text-liberthia-300/50">{fmtBytes(clip.sizeBytes)}</div>
      </div>
      <div className="flex-1 min-w-0">
        <audio controls className="w-full h-8" src={api.voiceAudioUrl(clip.id)}
          preload="none"
          onError={() => {
            // 404 = orphan no servidor (DB row sem WAV). Backend já deletou
            // o row no readAudio. Invalida cache pra sumir da lista.
            qc.invalidateQueries({ queryKey: ['voice-library-clips'] })
            qc.invalidateQueries({ queryKey: ['voice-library-players'] })
          }} />
        {clip.transcription && (
          // Transcrição COMPLETA — sem truncate. Quebra em múltiplas linhas
          // pra mostrar tudo que o jogador falou. whitespace-pre-wrap preserva
          // quebras naturais de sentença.
          <div className="text-[11px] text-liberthia-300/80 mt-1 italic whitespace-pre-wrap break-words">
            "{clip.transcription}"
          </div>
        )}
        <div className="text-[10px] text-liberthia-300/40 mt-0.5">
          {new Date(clip.ts).toLocaleString()}
          {clip.dimension && ` · ${clip.dimension}`}
          {clip.posX != null && ` · ${Math.round(clip.posX)},${Math.round(clip.posY || 0)},${Math.round(clip.posZ || 0)}`}
          {clip.protectedFromCleanup && <span className="text-emerald-300 ml-1">· 🛡 protegido</span>}
        </div>
      </div>
      <button className={`btn-ghost btn-sm ${clip.protectedFromCleanup ? 'text-emerald-300' : ''}`}
        onClick={onToggleProtect} title={clip.protectedFromCleanup ? 'Desproteger' : 'Proteger da auto-cleanup'}>
        {clip.protectedFromCleanup ? '🛡' : '🔓'}
      </button>
      <a className="btn-ghost btn-sm" href={api.voiceAudioUrl(clip.id)} download={`clip-${clip.id}.wav`}
        title="Baixar WAV">⬇</a>
    </div>
  )
}
