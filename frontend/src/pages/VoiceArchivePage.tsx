import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, VoiceClipDto } from '../lib/api'
import { toast } from '../store/toast'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { NumInput } from '../components/NumInput'

/**
 * Voice Archive — banco de tudo que os players já falaram no Voice Chat.
 *
 * Mostra cada clipe com player de áudio inline, transcrição editável, tags,
 * e botão pra DISPARAR o clipe DENTRO DO JOGO em coordenadas escolhidas.
 *
 * Use cases (assustadores):
 *  - Pegar o grito de morte de um player → tocar 3 dias depois no memorial dele
 *  - Memory Echoes com a voz REAL do player (não texto)
 *  - The Listener NPC que cospe uma frase salva por hora
 */

function fmtDur(ms: number) {
  const s = ms / 1000
  return s < 1 ? `${ms}ms` : `${s.toFixed(1)}s`
}
function fmtSize(b: number) {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  return `${(b / (1024 * 1024)).toFixed(1)}MB`
}

type SortMode = 'recent' | 'longest' | 'largest'

export function VoiceArchivePage() {
  const qc = useQueryClient()
  const [filterUuid, setFilterUuid] = useState('')
  const [sortMode, setSortMode] = useState<SortMode>('recent')
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  // ANTES: api.players só trazia jogadores ONLINE — dropdown ficava vazio quando
  // ninguém estava conectado, e quem tinha clipes mas tinha saído sumia da lista.
  // AGORA: usa voiceLibraryPlayers (agregação por UUID de quem TEM clipes salvos
  // no disco), garantido pelo cleanup automático no backend.
  const playersQ = useQuery({
    queryKey: ['voice-library-players'],
    queryFn: api.voiceLibraryPlayers,
    refetchInterval: 10000,
  })
  const players = useMemo(() => {
    const list = playersQ.data?.players ?? []
    // Ordena por última atividade desc, depois por nome
    return [...list].sort((a, b) => {
      const ts = b.lastClipTs - a.lastClipTs
      if (ts !== 0) return ts
      return (a.playerName ?? '').localeCompare(b.playerName ?? '')
    }).map(p => ({ uuid: p.playerUuid, name: p.playerName, clipCount: p.clipCount }))
  }, [playersQ.data])
  const q = useQuery({
    queryKey: ['voice-clips', filterUuid],
    queryFn: () => api.voiceList(filterUuid || undefined, 200),
    refetchInterval: 5000,
  })
  const rawClips = q.data?.clips ?? []
  const clips = [...rawClips].sort((a, b) => {
    if (sortMode === 'longest') return b.durationMs - a.durationMs
    if (sortMode === 'largest') return b.sizeBytes - a.sizeBytes
    return b.ts - a.ts
  })
  const [selected, setSelected] = useState<VoiceClipDto | null>(null)

  function toggleSel(id: number) {
    setSelectedIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }
  function toggleAllVisible() {
    if (selectedIds.size >= clips.length) setSelectedIds(new Set())
    else setSelectedIds(new Set(clips.map(c => c.id)))
  }
  function clearSel() { setSelectedIds(new Set()) }

  // Invalida TODOS os caches de voz quando algo é deletado — sem isso,
  // a Voice Library / Rankings ficam mostrando totais (clipes/tempo/tamanho)
  // defasados. v92: também invalida voice-rankings explicitamente.
  function invalidateAllVoiceCaches() {
    qc.invalidateQueries({ queryKey: ['voice-clips'] })
    qc.invalidateQueries({ queryKey: ['voice-library-players'] })
    qc.invalidateQueries({ queryKey: ['voice-library-clips'] })
    qc.invalidateQueries({ queryKey: ['voice-by-player'] })
    qc.invalidateQueries({ queryKey: ['voice-rankings'] })
  }

  async function deleteSelected() {
    const ids = Array.from(selectedIds)
    if (ids.length === 0) return
    if (!confirm(`Deletar ${ids.length} clipe(s)?`)) return
    try {
      const r = await api.voiceBulkDelete(ids)
      toast.ok(`🗑 ${r.deleted} deletado(s)`)
      clearSel()
      invalidateAllVoiceCaches()
    } catch (e: any) { toast.err(e.message) }
  }
  async function deleteAllFromPlayer() {
    if (!filterUuid) return
    const p = players.find(x => x.uuid === filterUuid)
    const name = p?.name ?? filterUuid.slice(0, 8)
    if (!confirm(`Deletar TODOS os clipes de "${name}"? Isso é irreversível.`)) return
    try {
      const r = await api.voiceDeleteByPlayer(filterUuid)
      toast.ok(`🗑 ${r.deleted} clipe(s) de ${name} deletado(s)`)
      clearSel()
      invalidateAllVoiceCaches()
    } catch (e: any) { toast.err(e.message) }
  }
  async function deleteEverything() {
    // Confirmação dupla: confirm normal + prompt pedindo a palavra-chave.
    // Operação destrutiva — limpa O ARQUIVO INTEIRO de todos os players.
    if (!confirm(`💣 ATENÇÃO\n\nIsso vai deletar TODOS os clipes de TODOS os players do servidor.\n\nIrreversível. Continuar?`)) return
    const typed = prompt(`Pra confirmar, digite DELETAR TUDO (em maiúsculas):`)
    if (typed !== 'DELETAR TUDO') {
      toast.err('Cancelado — palavra-chave não bate')
      return
    }
    try {
      const r = await api.voiceDeleteAll()
      if (!r.ok) {
        toast.err(r.error ?? 'falhou')
        return
      }
      toast.ok(`💣 ${r.deleted} clipe(s) removidos do arquivo`)
      clearSel()
      invalidateAllVoiceCaches()
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎙 Voice Archive</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Captura em tempo real via Simple Voice Chat</span> — tudo que os players falam fica salvo aqui.
          </p>
        </div>
        <div className="flex gap-2 items-center text-xs flex-wrap">
          <span className="badge badge-purple">{clips.length} clipes</span>
          <div className="flex gap-1">
            <button className={sortMode === 'recent' ? 'btn btn-sm' : 'btn-ghost btn-sm'} onClick={() => setSortMode('recent')}>📅 Recentes</button>
            <button className={sortMode === 'longest' ? 'btn btn-sm' : 'btn-ghost btn-sm'} onClick={() => setSortMode('longest')}>⏱ Mais longos</button>
            <button className={sortMode === 'largest' ? 'btn btn-sm' : 'btn-ghost btn-sm'} onClick={() => setSortMode('largest')}>📦 Mais pesados</button>
          </div>
          <select className="input text-xs" value={filterUuid} onChange={(e) => { setFilterUuid(e.target.value); clearSel() }}>
            <option value="">Todos os players ({players.length})</option>
            {players.map((p) => (
              <option key={p.uuid} value={p.uuid}>
                {p.name} ({p.clipCount})
              </option>
            ))}
          </select>
        </div>
      </header>

      {/* Bulk actions bar — sempre visível quando há clipes */}
      {clips.length > 0 && (
        <div className="card-glow !bg-amber-500/10 !border-amber-400/40 mb-3 flex items-center gap-2 flex-wrap text-xs py-2">
          {selectedIds.size > 0 ? (
            <>
              <span className="text-amber-300 font-bold">{selectedIds.size} selecionado(s)</span>
              <button className="btn-danger btn-sm" onClick={deleteSelected}>🗑 Deletar selecionados</button>
              <button className="btn-ghost btn-sm" onClick={clearSel}>✕ Limpar seleção</button>
            </>
          ) : (
            <>
              <span className="text-liberthia-300/60">Nenhum clipe selecionado</span>
              <button className="btn-ghost btn-sm" onClick={toggleAllVisible}>☑ Selecionar todos visíveis ({clips.length})</button>
            </>
          )}
          <div className="flex-1" />
          {filterUuid && (
            <button className="btn-danger btn-sm" onClick={deleteAllFromPlayer}>
              💣 Deletar TODOS de {players.find(p => p.uuid === filterUuid)?.name ?? 'player'}
            </button>
          )}
          {!filterUuid && (
            <button className="btn-danger btn-sm" onClick={deleteEverything} title="Deleta TODOS os clipes do servidor (purge global)">
              ☠ Limpar arquivo inteiro
            </button>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-4">
        <div className="card-glow !p-0 overflow-hidden">
          <div className="grid grid-cols-[24px_70px_1fr_60px_60px_50px_30px] text-[10px] text-liberthia-300/50 px-2 py-1 border-b border-liberthia-500/20 bg-liberthia-900/40 items-center">
            <input type="checkbox"
              checked={clips.length > 0 && selectedIds.size >= clips.length}
              onChange={toggleAllVisible}
              title="Selecionar todos visíveis" />
            <span>quando</span>
            <span>player · transcrição</span>
            <span className="text-right">dur</span>
            <span className="text-right">tam</span>
            <span className="text-center">STT</span>
            <span></span>
          </div>
          <div className="max-h-[70vh] overflow-y-auto">
            {clips.length === 0 && (
              <div className="text-center py-12 text-liberthia-300/50 text-sm">
                <div className="text-4xl mb-2 opacity-50">🎙</div>
                <p>Nenhum clipe ainda. Quando algum player falar no Voice Chat, aparece aqui.</p>
              </div>
            )}
            {clips.map((c) => (
              <div key={c.id}
                className={`grid grid-cols-[24px_70px_1fr_60px_60px_50px_30px] text-xs px-2 py-1.5 border-b border-liberthia-700/30 hover:bg-liberthia-500/10 cursor-pointer items-center
                  ${selected?.id === c.id ? 'bg-liberthia-500/20' : ''}
                  ${selectedIds.has(c.id) ? '!bg-amber-500/15' : ''}`}
                onClick={() => setSelected(c)}>
                <input type="checkbox"
                  checked={selectedIds.has(c.id)}
                  onChange={(e) => { e.stopPropagation(); toggleSel(c.id) }}
                  onClick={(e) => e.stopPropagation()} />
                <span className="text-[10px] text-liberthia-300/50 font-mono">
                  {new Date(c.ts).toLocaleTimeString()}
                </span>
                <span className="truncate">
                  <b className="text-liberthia-100">{c.playerName}</b>
                  {c.transcription && (
                    <span className="italic text-liberthia-300/70 ml-2">"{c.transcription}"</span>
                  )}
                  {!c.transcription && <span className="text-liberthia-300/40 ml-2 italic">(sem transcrição)</span>}
                </span>
                <span className="text-right text-liberthia-300/60 font-mono">{fmtDur(c.durationMs)}</span>
                <span className="text-right text-liberthia-300/60 font-mono">{fmtSize(c.sizeBytes)}</span>
                <span className="text-center">
                  {c.transcriptionStatus === 'DONE' && <span className="text-emerald-300">✓</span>}
                  {c.transcriptionStatus === 'PENDING' && <span className="text-amber-300 animate-pulse">⏳</span>}
                  {c.transcriptionStatus === 'FAILED' && <span className="text-red-400">✗</span>}
                </span>
                <span className="text-center">
                  <button className="btn-ghost btn-sm" onClick={(e) => {
                    e.stopPropagation()
                    if (confirm(`Deletar clipe #${c.id}?`)) {
                      api.voiceDelete(c.id).then(() => {
                        invalidateAllVoiceCaches()
                        toast.ok('🗑')
                      })
                    }
                  }}>🗑</button>
                </span>
              </div>
            ))}
          </div>
        </div>

        <div>
          {selected ? <ClipDetail clip={selected} onChange={() => qc.invalidateQueries({ queryKey: ['voice-clips'] })} />
            : <div className="card text-center py-12 text-liberthia-300/50">
              <div className="text-4xl mb-2 opacity-50">👈</div>
              <p className="text-xs">Selecione um clipe pra ouvir, editar e tocar in-game</p>
            </div>}
        </div>
      </div>
    </div>
  )
}

function ClipDetail({ clip, onChange }: { clip: VoiceClipDto; onChange: () => void }) {
  const [trans, setTrans] = useState(clip.transcription ?? '')
  const [tags, setTags] = useState<string[]>(() => {
    try { return JSON.parse(clip.tagsJson || '[]') } catch { return [] }
  })
  const [newTag, setNewTag] = useState('')

  // reseta state quando muda de clipe
  useEffect(() => {
    setTrans(clip.transcription ?? '')
    try { setTags(JSON.parse(clip.tagsJson || '[]')) } catch { setTags([]) }
  }, [clip.id])

  const save = useMutation({
    mutationFn: () => api.voicePatch(clip.id, { transcription: trans, tagsJson: JSON.stringify(tags) }),
    onSuccess: () => { toast.ok('✓ salvo'); onChange() },
    onError: (e: any) => toast.err(e.message),
  })

  // Playback in-game
  const [playX, setPlayX] = useState(clip.posX ?? 0)
  const [playY, setPlayY] = useState(clip.posY ?? 64)
  const [playZ, setPlayZ] = useState(clip.posZ ?? 0)
  const [playDim, setPlayDim] = useState(clip.dimension ?? 'minecraft:overworld')
  const [volume, setVolume] = useState(1.0)

  const playMut = useMutation({
    mutationFn: () => api.voicePlay(clip.id, { x: playX, y: playY, z: playZ, dimension: playDim, volume, category: 'liberthia_voice' }),
    onSuccess: () => toast.ok('▶ tocando in-game'),
    onError: (e: any) => toast.err(e.message),
  })

  return (
    <div className="card-glow space-y-3">
      <h3 className="font-bold flex items-center gap-2">
        🎙 Clipe #{clip.id}
        <span className="text-[10px] text-liberthia-300/50 font-normal">{new Date(clip.ts).toLocaleString()}</span>
      </h3>

      <div className="text-xs">
        <b className="text-liberthia-100">{clip.playerName}</b>
        {clip.posX != null && (
          <span className="text-liberthia-300/60 font-mono ml-2">
            📍 {clip.posX.toFixed(0)},{clip.posY?.toFixed(0)},{clip.posZ?.toFixed(0)} ({(clip.dimension ?? '').replace('minecraft:', '')})
          </span>
        )}
        <span className="block text-[10px] text-liberthia-300/40">
          {fmtDur(clip.durationMs)} · {fmtSize(clip.sizeBytes)} · {clip.uploadStatus}
        </span>
      </div>

      <audio controls className="w-full" preload="metadata" src={api.voiceAudioUrl(clip.id)}
        onError={() => onChange()} />

      <div>
        <label className="label">Transcrição</label>
        <textarea className="input text-xs" rows={3}
          placeholder="Cole aqui o que o player falou (manual ou via Whisper)..."
          value={trans} onChange={(e) => setTrans(e.target.value)} />
      </div>

      <div>
        <label className="label">Tags ({tags.length})</label>
        <div className="flex flex-wrap gap-1 mb-1">
          {tags.map((t) => (
            <span key={t} className="badge badge-purple flex items-center gap-1">
              {t}
              <button className="text-red-400 ml-1" onClick={() => setTags(tags.filter((x) => x !== t))}>×</button>
            </span>
          ))}
        </div>
        <div className="flex gap-1">
          <input className="input text-xs flex-1" placeholder="ex: death, scream, memorial:abc"
            value={newTag} onChange={(e) => setNewTag(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && newTag.trim()) { setTags([...tags, newTag.trim()]); setNewTag('') } }} />
          <button className="btn-ghost btn-sm" onClick={() => { if (newTag.trim()) { setTags([...tags, newTag.trim()]); setNewTag('') } }}>+</button>
        </div>
      </div>

      <button className="btn w-full" onClick={() => save.mutate()} disabled={save.isPending}>💾 Salvar metadata</button>

      <hr className="border-liberthia-500/20" />

      <details>
        <summary className="cursor-pointer font-bold text-amber-300">▶ Tocar in-game</summary>
        <div className="mt-2 space-y-2 text-xs">
          <p className="text-[10px] text-liberthia-300/60">
            Reproduz o clipe via Simple Voice Chat na coord escolhida. Players a até 48b ouvem.
          </p>
          <div className="grid grid-cols-3 gap-1">
            <NumInput value={playX} onChange={setPlayX} />
            <NumInput value={playY} onChange={setPlayY} />
            <NumInput value={playZ} onChange={setPlayZ} />
          </div>
          <select className="input text-xs" value={playDim} onChange={(e) => setPlayDim(e.target.value)}>
            <option value="minecraft:overworld">overworld</option>
            <option value="minecraft:the_nether">nether</option>
            <option value="minecraft:the_end">end</option>
          </select>
          <PlayerPosPicker label="📍 Tocar onde o player está..." onPick={(p) => {
            setPlayX(Math.floor(p.x)); setPlayY(Math.floor(p.y)); setPlayZ(Math.floor(p.z))
            setPlayDim('minecraft:' + p.dim)
          }} />
          <div className="flex items-center gap-2">
            <span className="label">Volume:</span>
            <input type="range" min={0} max={2} step={0.1} value={volume} onChange={(e) => setVolume(Number(e.target.value))} className="flex-1" />
            <span className="font-mono w-8 text-right">{volume.toFixed(1)}</span>
          </div>
          <button className="btn-amber w-full" onClick={() => playMut.mutate()} disabled={playMut.isPending}>
            ▶ Tocar agora
          </button>
        </div>
      </details>
    </div>
  )
}
