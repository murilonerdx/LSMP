import { useMemo, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, EtchedDiscDto } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Etched Music Disc Library — admin cadastra URLs (YouTube/SoundCloud/mp3)
 * e cria discos físicos do mod Etched in-game via /give com NBT custom.
 */

const CATEGORIES = [
  { key: 'music', label: '🎵 Música', color: 'text-purple-300' },
  { key: 'ambient', label: '🌫 Ambiente', color: 'text-cyan-300' },
  { key: 'sfx', label: '🔊 SFX', color: 'text-amber-300' },
  { key: 'meme', label: '😂 Meme', color: 'text-pink-300' },
  { key: 'event', label: '🎭 Evento', color: 'text-emerald-300' },
]

const DISC_COLORS = ['blank', 'lapis', 'gold', 'diamond', 'emerald', 'nether']

export function EtchedDiscsPage() {
  const qc = useQueryClient()
  const [filterCat, setFilterCat] = useState<string>('')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<EtchedDiscDto | null>(null)
  const [giveTo, setGiveTo] = useState<{ disc: EtchedDiscDto; playerName: string } | null>(null)
  const [showCookies, setShowCookies] = useState(false)

  const playersQ = useQuery({
    queryKey: ['players'], queryFn: api.players, refetchInterval: 8000,
  })
  const listQ = useQuery({
    queryKey: ['etched-discs', filterCat],
    queryFn: () => api.etchedList(filterCat || undefined),
    refetchInterval: 4000,  // pra atualizar status de extração
  })

  const createMut = useMutation({
    mutationFn: api.etchedCreate,
    onSuccess: () => { toast.ok('✓ disco criado'); qc.invalidateQueries({ queryKey: ['etched-discs'] }); setEditing(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const updateMut = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<EtchedDiscDto> }) => api.etchedUpdate(id, body),
    onSuccess: () => { toast.ok('✓ atualizado'); qc.invalidateQueries({ queryKey: ['etched-discs'] }); setEditing(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const deleteMut = useMutation({
    mutationFn: api.etchedDelete,
    onSuccess: () => { toast.ok('🗑 deletado'); qc.invalidateQueries({ queryKey: ['etched-discs'] }) },
    onError: (e: any) => toast.err(e.message),
  })
  const giveMut = useMutation({
    mutationFn: ({ id, playerName }: { id: number; playerName: string }) => api.etchedGive(id, playerName),
    onSuccess: () => { toast.ok('🎵 disco entregue'); setGiveTo(null); qc.invalidateQueries({ queryKey: ['etched-discs'] }) },
    onError: (e: any) => toast.err(e.message),
  })
  const giveLabelMut = useMutation({
    mutationFn: ({ id, playerName }: { id: number; playerName: string }) => api.etchedGiveLabel(id, playerName),
    onSuccess: (r) => { toast.ok(r.note ?? '🛠 kit entregue'); setGiveTo(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const giveKitMut = useMutation({
    mutationFn: ({ id, playerName, includeTable }: { id: number; playerName: string; includeTable: boolean }) =>
      api.etchedGiveKit(id, playerName, includeTable),
    onSuccess: (r) => { toast.ok(r.note ?? '🛠 kit completo entregue'); setGiveTo(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const giveRadioMut = useMutation({
    mutationFn: ({ id, playerName }: { id: number; playerName: string }) => api.etchedGiveRadio(id, playerName),
    onSuccess: (r) => { toast.ok(r.note ?? '📡 portal radio entregue'); setGiveTo(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const [bpDiscType, setBpDiscType] = useState<'vanilla' | 'etched'>('etched')
  const [bpVanillaDisc, setBpVanillaDisc] = useState('minecraft:music_disc_cat')
  const [bpAdvanced, setBpAdvanced] = useState(false)
  const giveBackpackMut = useMutation({
    mutationFn: ({ id, playerName }: { id: number; playerName: string }) =>
      api.etchedGiveBackpack(id, playerName, {
        advanced: bpAdvanced,
        discType: bpDiscType,
        vanillaDisc: bpVanillaDisc,
      }),
    onSuccess: (r) => { toast.ok(r.note ?? '🎒 mochila musical entregue'); setGiveTo(null) },
    onError: (e: any) => toast.err(e.message),
  })

  const VANILLA_DISCS = [
    { id: 'minecraft:music_disc_cat', label: '🐱 Cat (C418)' },
    { id: 'minecraft:music_disc_13', label: '13 (C418, sinistro)' },
    { id: 'minecraft:music_disc_blocks', label: '🧱 Blocks (C418)' },
    { id: 'minecraft:music_disc_chirp', label: '🐦 Chirp (C418)' },
    { id: 'minecraft:music_disc_far', label: '🏞 Far (C418)' },
    { id: 'minecraft:music_disc_mall', label: '🛍 Mall (C418)' },
    { id: 'minecraft:music_disc_mellohi', label: '🌌 Mellohi (C418)' },
    { id: 'minecraft:music_disc_stal', label: '🏛 Stal (C418)' },
    { id: 'minecraft:music_disc_strad', label: '🎻 Strad (C418)' },
    { id: 'minecraft:music_disc_ward', label: '⚠ Ward (C418, ambient)' },
    { id: 'minecraft:music_disc_11', label: '11 (C418, lo-fi)' },
    { id: 'minecraft:music_disc_wait', label: '⏳ Wait (C418)' },
    { id: 'minecraft:music_disc_pigstep', label: '🐷 Pigstep (Lena Raine)' },
    { id: 'minecraft:music_disc_otherside', label: '🌀 Otherside (Lena Raine)' },
    { id: 'minecraft:music_disc_5', label: '5 (Samuel Åberg)' },
    { id: 'minecraft:music_disc_relic', label: '🪦 Relic (Aaron Cherof)' },
  ]
  const extractMut = useMutation({
    mutationFn: api.etchedExtract,
    onSuccess: () => { toast.ok('⏳ extraindo áudio (pode levar 30s-2min)'); qc.invalidateQueries({ queryKey: ['etched-discs'] }) },
    onError: (e: any) => toast.err(e.message),
  })
  const uploadMut = useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => api.etchedUploadAudio(id, file),
    onSuccess: (r) => {
      if (r.ok) toast.ok(`✓ mp3 enviado (${Math.round((r.size ?? 0) / 1024)}KB)`)
      else toast.err(r.error ?? 'falhou')
      qc.invalidateQueries({ queryKey: ['etched-discs'] })
    },
    onError: (e: any) => toast.err(e.message),
  })
  // Hidden file input que dispara o upload pro disco clicado
  const uploadRef = useRef<HTMLInputElement>(null)
  const [uploadFor, setUploadFor] = useState<number | null>(null)
  function pickFileFor(discId: number) {
    setUploadFor(discId)
    uploadRef.current?.click()
  }

  const discs = Array.isArray(listQ.data) ? listQ.data : []
  const players = Array.isArray(playersQ.data) ? playersQ.data : []
  const filtered = useMemo(() => {
    if (!search.trim()) return discs
    const q = search.trim().toLowerCase()
    return discs.filter(d =>
      d.title.toLowerCase().includes(q) ||
      d.author.toLowerCase().includes(q) ||
      d.url.toLowerCase().includes(q))
  }, [discs, search])

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">💿 Etched Music Discs</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Cadastra URL (YouTube, SoundCloud, mp3) e gera disco musical do mod Etched no jogo. Cada
            disco fica salvo no banco — entrega quando quiser pra qualquer player.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost" onClick={() => setShowCookies(true)}
            title="Subir cookies.txt do YouTube pra desbloquear vídeos com bot-check">
            🍪 Cookies YT
          </button>
          <button className="btn" onClick={() => setEditing({
            title: '', author: 'Servidor', url: '', discColor: 'blank',
            durationSec: 0, category: 'music', description: '', addedBy: '',
            playCount: 0,
          })}>+ Novo disco</button>
        </div>
      </header>

      {showCookies && <CookiesDialog onClose={() => setShowCookies(false)} />}

      {/* Box de items do Etched — qual jukebox usar, etc */}
      <EtchedItemsHelpBox />

      {/* Hidden file input — disparado pelo botão "📤 Upload" de qualquer card */}
      <input ref={uploadRef} type="file"
        accept="audio/mpeg,audio/wav,audio/ogg,audio/mp4,audio/flac,audio/x-m4a,.mp3,.wav,.ogg,.m4a,.flac,.opus,.webm"
        style={{ display: 'none' }}
        onChange={(ev) => {
          const f = ev.target.files?.[0]
          if (f && uploadFor) uploadMut.mutate({ id: uploadFor, file: f })
          if (uploadRef.current) uploadRef.current.value = ''
          setUploadFor(null)
        }} />

      <div className="flex items-center gap-2 flex-wrap mb-3">
        <input className="input flex-1" placeholder="Buscar título/autor/url…"
          value={search} onChange={(e) => setSearch(e.target.value)} />
        <button className={`btn-ghost btn-sm ${filterCat === '' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilterCat('')}>Todas</button>
        {CATEGORIES.map(c => (
          <button key={c.key}
            className={`btn-ghost btn-sm ${filterCat === c.key ? 'ring-1 ring-purple-400' : ''}`}
            onClick={() => setFilterCat(c.key)}>{c.label}</button>
        ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {filtered.map(d => {
          const cat = CATEGORIES.find(c => c.key === d.category)
          return (
            <div key={d.id} className="card-glow space-y-2">
              <div className="flex items-start gap-2">
                <div className="text-3xl">💿</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold truncate">{d.title}</div>
                  <div className="text-xs text-liberthia-300/60 truncate">por {d.author}</div>
                  <div className="text-[10px] text-liberthia-300/40 truncate font-mono">{d.url}</div>
                </div>
                <span className={`badge ${cat?.color ?? ''}`}>{cat?.label ?? d.category}</span>
              </div>
              {d.description && (
                <p className="text-xs text-liberthia-300/70 italic">"{d.description}"</p>
              )}
              <div className="flex gap-2 text-[10px] text-liberthia-300/50 flex-wrap items-center">
                {d.durationSec > 0 && <span>⏱ {Math.floor(d.durationSec / 60)}m{d.durationSec % 60}s</span>}
                <span>🎬 {d.playCount} dado{d.playCount === 1 ? '' : 's'}</span>
                {/* Status do áudio extraído */}
                {d.audioStatus === 'READY' && (
                  <span className="badge badge-green text-[9px]">
                    ✓ mp3 local ({d.audioSizeBytes ? Math.round(d.audioSizeBytes / 1024) + 'KB' : 'ok'})
                  </span>
                )}
                {d.audioStatus === 'EXTRACTING' && (
                  <span className="badge badge-amber text-[9px] animate-pulse">⏳ extraindo…</span>
                )}
                {d.audioStatus === 'FAILED' && (
                  <span className="badge badge-red text-[9px]" title={d.audioError ?? ''}>
                    ✗ falhou
                  </span>
                )}
                {(d.audioStatus === 'NONE' || !d.audioStatus) && (
                  <span className="badge text-[9px]">YT direto</span>
                )}
              </div>
              <div className="flex gap-1 flex-wrap">
                <button className="btn btn-sm flex-1"
                  onClick={() => setGiveTo({ disc: d, playerName: players[0]?.name ?? '' })}>
                  🎁 Dar
                </button>
                {d.audioStatus !== 'READY' && d.audioStatus !== 'EXTRACTING' && (
                  <>
                    <button className="btn-ghost btn-sm" title="Extrair via yt-dlp (YouTube/Soundcloud/etc)"
                      onClick={() => d.id && extractMut.mutate(d.id)}>
                      📥 Extrair
                    </button>
                    <button className="btn-ghost btn-sm" title="Upload mp3/wav/ogg do desktop"
                      onClick={() => d.id && pickFileFor(d.id)}>
                      📤 Upload
                    </button>
                  </>
                )}
                {d.audioStatus === 'READY' && (
                  <button className="btn-ghost btn-sm" title="Substituir mp3 atual por upload"
                    onClick={() => d.id && pickFileFor(d.id)}>
                    📤 Trocar
                  </button>
                )}
                {d.audioStatus === 'FAILED' && (
                  <button className="btn-ghost btn-sm" title="Tentar de novo"
                    onClick={() => d.id && extractMut.mutate(d.id)}>↻</button>
                )}
                <button className="btn-ghost btn-sm" onClick={() => setEditing(d)}>✏</button>
                <button className="btn-danger btn-sm"
                  onClick={() => { if (d.id && confirm(`Deletar "${d.title}"?`)) deleteMut.mutate(d.id) }}>🗑</button>
              </div>
              {d.audioStatus === 'FAILED' && d.audioError && (
                <div className="text-[9px] text-red-300/80 font-mono truncate" title={d.audioError}>
                  {d.audioError.slice(-100)}
                </div>
              )}
            </div>
          )
        })}
        {!listQ.isLoading && filtered.length === 0 && (
          <div className="card col-span-full text-center py-12 text-liberthia-300/60">
            Sem discos ainda. Click em "+ Novo disco".
          </div>
        )}
      </div>

      {/* Modal: criar/editar */}
      {editing && (
        <DiscEditor disc={editing}
          onCancel={() => setEditing(null)}
          onSave={(d) => {
            if (d.id) updateMut.mutate({ id: d.id, body: d })
            else createMut.mutate(d)
          }} />
      )}

      {/* Modal: dar disco — 3 opções */}
      {giveTo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={() => setGiveTo(null)}>
          <div className="card max-w-2xl w-full space-y-3 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-bold text-lg">🎁 Como entregar "{giveTo.disc.title}"</h3>
            <select className="input" value={giveTo.playerName}
              onChange={(e) => setGiveTo({ ...giveTo, playerName: e.target.value })}>
              <option value="">Selecione um player online…</option>
              {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
            </select>

            {/* OPÇÃO 1: Kit Etching Table — GARANTIDO */}
            <div className="card text-xs space-y-2 border-l-4 border-l-emerald-400">
              <div className="flex items-center gap-2">
                <span className="badge badge-green">✓ GARANTIDO</span>
                <p className="font-bold">🛠 Kit Etching Table (recomendado)</p>
              </div>
              <p className="text-liberthia-300/70">
                Backend entrega 3 items pro player:
              </p>
              <ul className="pl-4 list-disc text-liberthia-300/70 space-y-0.5">
                <li><code>etched:etching_table</code> — bloco pra gravar disco</li>
                <li><code>etched:music_label</code> — etiqueta JÁ configurada com URL/título/artista</li>
                <li><code>etched:blank_music_disc</code> — disco em branco</li>
              </ul>
              <p className="text-liberthia-300/70">
                Player coloca a table no chão, RMB nela, junta <strong>label+disco</strong> no slot →
                <strong> o próprio mod Etched gera o disco final</strong> com NBT 100% correto. Resultado:
                disco que toca em <strong>minecraft:jukebox</strong> normal, em <strong>etched:album_jukebox</strong>,
                <strong> etched:boombox</strong>, e qualquer outro player. SEM CRASH.
              </p>
              <button className="btn w-full"
                disabled={!giveTo.playerName}
                onClick={() => giveKitMut.mutate({
                  id: giveTo.disc.id!, playerName: giveTo.playerName, includeTable: true
                })}>
                🛠 Entregar Kit Completo (table + label + disco)
              </button>
              <button className="btn-ghost btn-sm w-full text-[10px]"
                disabled={!giveTo.playerName}
                onClick={() => giveKitMut.mutate({
                  id: giveTo.disc.id!, playerName: giveTo.playerName, includeTable: false
                })}>
                ↳ Só label + disco (sem table) — player já tem uma
              </button>
            </div>

            {/* OPÇÃO 2: Portal Radio — também GARANTIDO */}
            <div className="card text-xs space-y-2 border-l-4 border-l-cyan-400">
              <div className="flex items-center gap-2">
                <span className="badge badge-green">✓ GARANTIDO</span>
                <p className="font-bold">📡 Portal Radio (sem disco)</p>
              </div>
              <p className="text-liberthia-300/70">
                Entrega 1× <code>etched:portal_radio</code> (bloco autônomo) +
                manda a URL no chat (clica pra copiar). Player coloca no chão, RMB pra abrir GUI,
                cola a URL → bloco fica streamando direto, sem precisar de disco.
              </p>
              <p className="text-liberthia-300/50 italic">
                Útil pra ambiente (rádio na base, música contínua, lounge). Não move, mas é stable.
              </p>
              <button className="btn w-full"
                disabled={!giveTo.playerName}
                onClick={() => giveRadioMut.mutate({ id: giveTo.disc.id!, playerName: giveTo.playerName })}>
                📡 Entregar Portal Radio + URL no chat
              </button>
            </div>

            {/* OPÇÃO 3: Mochila Musical — GARANTIDO mas com disco VANILLA */}
            <div className="card text-xs space-y-2 border-l-4 border-l-purple-400">
              <div className="flex items-center gap-2">
                <span className="badge badge-green">✓ GARANTIDO</span>
                <p className="font-bold">🎒 Mochila Musical (portátil)</p>
              </div>

              <div className="bg-amber-500/10 border-2 border-amber-500/60 rounded p-3 text-xs text-amber-100 space-y-2">
                <p className="font-bold text-amber-300 text-sm">
                  ⚠ DISCO ETCHED NA MOCHILA REQUER MOD DE COMPAT
                </p>
                <p>
                  O servidor LSMP <strong>ainda não tem</strong> o mod de compat instalado. Sem ele, o slot
                  do Jukebox Upgrade NÃO aceita o disco Etched (confirmado: handler do SB usa
                  <code> instanceof RecordItem</code>, Etched extende só <code>Item</code>).
                </p>
                <p className="font-bold mt-2">📥 Como instalar (3 passos):</p>
                <ol className="pl-4 list-decimal space-y-1">
                  <li>
                    Baixa o jar:{' '}
                    <a href="https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks-etched-integration/files/7932339"
                      target="_blank" rel="noopener noreferrer"
                      className="underline text-cyan-300 font-bold">
                      sophisticatedbackpacksetchedintegration-1.0.0.jar ↗
                    </a>{' '}
                    (~10KB, MIT License)
                  </li>
                  <li>
                    Coloca o jar em <code className="bg-black/30 px-1 rounded">mods/</code> de:
                    <ul className="pl-4 list-disc mt-0.5 space-y-0.5">
                      <li><strong>Servidor MC</strong> (lsmp.ddns.net) — via SSH/SCP/Pterodactyl</li>
                      <li><strong>Cliente de cada player</strong> que vai ouvir (FTP do modpack, CurseForge App, etc)</li>
                    </ul>
                  </li>
                  <li>
                    Restart o <strong>servidor MC</strong> + cada player <strong>reconecta</strong> com o mod
                    no client. Pronto — disco Etched toca na mochila.
                  </li>
                </ol>
                <p className="text-[10px] text-amber-200/80 italic pt-1">
                  ℹ️ Como verifico que o mod tá instalado? No menu do MC, vai em <strong>"Mods"</strong> e
                  procura por "Sophisticated Backpacks Etched Integration". Server side, olha o log de
                  startup procurando por <code>[sophisticatedbackpacksetchedintegration]</code>.
                </p>
                <p className="text-[10px] text-emerald-200 pt-1">
                  ✓ Source code do mod (open source): <a target="_blank" rel="noopener noreferrer" className="underline"
                    href="https://github.com/RootTool0/Sophisticated-Backpacks-Etched-Integration">
                    github.com/RootTool0/Sophisticated-Backpacks-Etched-Integration
                  </a>
                </p>
                <p className="text-[10px] text-emerald-200">
                  ✓ Source confirmado: <code>EtchedDiscHandler.supports()</code> retorna true pra QUALQUER
                  <code> instanceof EtchedMusicDiscItem</code> — NBT não importa pro aceite.
                </p>
              </div>

              <div className="bg-amber-500/10 border border-amber-500/30 rounded p-2 text-[11px] text-amber-200">
                <strong>⚠ Sem o mod de compat acima:</strong> SB Jukebox Upgrade usa{' '}
                <code>instanceof RecordItem</code> e o Etched extende <code>Item</code> — então o disco custom
                <strong> não toca</strong> na mochila por default. Use o disco vanilla abaixo nesse caso.
              </div>

              <p className="text-liberthia-300/70">Entrega:</p>
              <ul className="pl-4 list-disc text-liberthia-300/70 space-y-0.5">
                <li><code>sophisticatedbackpacks:backpack</code></li>
                <li><code>{bpAdvanced ? 'advanced_jukebox_upgrade' : 'jukebox_upgrade'}</code></li>
                <li>
                  1× {bpDiscType === 'etched'
                    ? <><strong className="text-emerald-300">etched_music_disc</strong> JÁ gravado (com URL deste disco)</>
                    : <>disco vanilla escolhido</>}
                </li>
              </ul>

              <div className="grid grid-cols-1 gap-2 pt-1">
                {/* Switch tipo de disco */}
                <div className="flex gap-1">
                  <button className={`btn-sm flex-1 ${bpDiscType === 'etched' ? 'btn' : 'btn-ghost'}`}
                    onClick={() => setBpDiscType('etched')}
                    title="Disco custom já gravado (precisa do mod de compat instalado no server)">
                    💿 Etched (custom)
                  </button>
                  <button className={`btn-sm flex-1 ${bpDiscType === 'vanilla' ? 'btn' : 'btn-ghost'}`}
                    onClick={() => setBpDiscType('vanilla')}
                    title="Disco vanilla — funciona sempre, sem precisar do mod de compat">
                    🐱 Vanilla
                  </button>
                </div>

                {bpDiscType === 'vanilla' && (
                  <div>
                    <label className="label text-[10px]">Qual disco vanilla?</label>
                    <select className="input text-xs" value={bpVanillaDisc}
                      onChange={(e) => setBpVanillaDisc(e.target.value)}>
                      {VANILLA_DISCS.map(v => <option key={v.id} value={v.id}>{v.label}</option>)}
                    </select>
                  </div>
                )}

                {bpDiscType === 'etched' && (
                  <div className="bg-emerald-500/10 border border-emerald-500/30 rounded p-2 text-[10px] text-emerald-200">
                    Vai entregar o disco JÁ GRAVADO com o NBT correto (mesmo formato que o EtchingMenu
                    gera). Player coloca direto na mochila — <strong>requer o mod de compat instalado</strong>.
                  </div>
                )}

                <label className="flex items-center gap-1.5 text-[11px]">
                  <input type="checkbox" checked={bpAdvanced}
                    onChange={(e) => setBpAdvanced(e.target.checked)} />
                  <span>Advanced upgrade (12 slots + playlist)</span>
                </label>
              </div>

              <button className="btn w-full"
                disabled={!giveTo.playerName}
                onClick={() => giveBackpackMut.mutate({
                  id: giveTo.disc.id!, playerName: giveTo.playerName,
                })}>
                🎒 Entregar mochila + jukebox + {
                  bpDiscType === 'etched'
                    ? 'disco Etched custom'
                    : bpVanillaDisc.replace('minecraft:music_disc_', '')
                }
              </button>
            </div>

            {/* OPÇÃO 4: Disco direto — EXPERIMENTAL */}
            <div className="card text-xs space-y-2 border-l-4 border-l-amber-400 opacity-80">
              <div className="flex items-center gap-2">
                <span className="badge badge-amber">⚠ EXPERIMENTAL</span>
                <p className="font-bold">💿 Disco gravado direto via NBT</p>
              </div>
              <p className="text-liberthia-300/70">
                Tenta dar <code>etched:etched_music_disc</code> via <code>/give</code> com NBT injetado
                (Tracks + DiscColor + Title + Author). Em teoria funciona em qualquer jukebox; na prática
                pode crashar o cliente dependendo do mismatch de NBT entre versões.
              </p>
              <p className="text-amber-300/80 italic">
                Só use se realmente precisa do disco físico já gravado. Se der ruim, manda o crash log
                pra eu ajustar o NBT.
              </p>
              <button className="btn-ghost w-full"
                disabled={!giveTo.playerName}
                onClick={() => giveMut.mutate({ id: giveTo.disc.id!, playerName: giveTo.playerName })}>
                💿 Tentar dar disco direto (pode crashar)
              </button>
            </div>

            <div className="flex gap-2 justify-end">
              <button className="btn-ghost" onClick={() => setGiveTo(null)}>Fechar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function DiscEditor({ disc, onSave, onCancel }: {
  disc: EtchedDiscDto
  onSave: (d: EtchedDiscDto) => void
  onCancel: () => void
}) {
  const [d, setD] = useState(disc)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onCancel}>
      <div className="card max-w-xl w-full max-h-[90vh] overflow-y-auto space-y-2" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold">{d.id ? '✏ Editar' : '+ Novo'} disco</h3>
        <div>
          <label className="label">Título</label>
          <input className="input" value={d.title} onChange={(e) => setD({ ...d, title: e.target.value })} />
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="label">Autor</label>
            <input className="input" value={d.author} onChange={(e) => setD({ ...d, author: e.target.value })} />
          </div>
          <div>
            <label className="label">Duração (segundos)</label>
            <input type="number" min={0} className="input" value={d.durationSec}
              onChange={(e) => setD({ ...d, durationSec: Number(e.target.value) || 0 })} />
          </div>
        </div>
        <div>
          <label className="label">URL (YouTube/SoundCloud/Bandcamp/mp3 direto)</label>
          <input className="input" placeholder="https://soundcloud.com/... ou deixa em branco se vai usar upload"
            value={d.url} onChange={(e) => setD({ ...d, url: e.target.value })} />
          <p className="text-[10px] text-liberthia-300/50 mt-1">
            💡 Se a URL não funcionar (YT exigindo login), salva o disco e depois usa o botão{' '}
            <strong>📤 Upload</strong> pra subir mp3 direto do desktop (mp3/wav/ogg/m4a/flac).
          </p>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="label">Categoria</label>
            <select className="input" value={d.category} onChange={(e) => setD({ ...d, category: e.target.value })}>
              {CATEGORIES.map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Cor do disco</label>
            <select className="input" value={d.discColor} onChange={(e) => setD({ ...d, discColor: e.target.value })}>
              {DISC_COLORS.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
        </div>
        <div>
          <label className="label">Descrição (lore)</label>
          <textarea className="input" rows={2}
            value={d.description} onChange={(e) => setD({ ...d, description: e.target.value })} />
        </div>
        <div className="flex gap-2 justify-end pt-2">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" disabled={!d.title} onClick={() => onSave(d)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

/**
 * Help box explicando os items/blocks do Etched mod. CRITICAL: minecraft:jukebox
 * NÃO funciona com discos do Etched 3.x — o mod tem jukeboxes próprias.
 * Usuários ficavam frustrados tentando tocar disco do Etched na jukebox vanilla
 * (que crasha ou apenas não toca).
 */
function EtchedItemsHelpBox() {
  const [open, setOpen] = useState(true) // aberto por default pra galera ver
  const [giveTarget, setGiveTarget] = useState('')
  const playersQ = useQuery({ queryKey: ['players-etched-give'], queryFn: api.players })
  const players = Array.isArray(playersQ.data) ? playersQ.data : []

  const giveMut = useMutation({
    mutationFn: ({ player, item }: { player: string; item: string }) =>
      api.command(`give ${player} ${item} 1`),
    onSuccess: () => toast.ok('🎁 entregue'),
    onError: (e: any) => toast.err(e.message),
  })

  const ITEMS = [
    {
      id: 'etched:album_jukebox', label: '📻 Album Jukebox',
      emoji: '✓', good: true,
      desc: 'Jukebox PRINCIPAL do Etched. ACEITA discos custom (etched_music_disc) + albums.',
    },
    {
      id: 'etched:boombox', label: '📦 Boombox',
      emoji: '✓', good: true,
      desc: 'Item portátil — segura na mão e dá RMB pra tocar disco custom.',
    },
    {
      id: 'etched:jukebox_minecart', label: '🚂 Jukebox Minecart',
      emoji: '✓', good: true,
      desc: 'Minecart com jukebox embutida — toca enquanto se move.',
    },
    {
      id: 'etched:portal_radio', label: '📡 Portal Radio',
      emoji: '✓', good: true,
      desc: 'Bloco que faz streaming de URL direto (sem precisar disco).',
    },
    {
      id: 'etched:etching_table', label: '🛠 Etching Table',
      emoji: '✓', good: true,
      desc: 'Bloco pra player gravar discos manualmente (juntando label + disco branco).',
    },
    {
      id: 'etched:music_label', label: '🏷 Music Label',
      emoji: 'ℹ', good: false,
      desc: 'Etiqueta com URL — vai numa etching table pra gravar disco em branco.',
    },
    {
      id: 'etched:blank_music_disc', label: '💿 Blank Disc',
      emoji: 'ℹ', good: false,
      desc: 'Disco sem gravação — combina com label na etching table.',
    },
    {
      id: 'etched:album_cover', label: '📕 Album Cover',
      emoji: 'ℹ', good: false,
      desc: 'Capa pra fazer álbum com vários discos.',
    },
  ]

  return (
    <div className="card mt-3 border-l-4 border-l-amber-400/60">
      <button onClick={() => setOpen(o => !o)}
        className="flex items-center gap-2 w-full text-left text-xs">
        <span className="text-base">⚠</span>
        <span className="font-bold text-amber-300">
          minecraft:jukebox NÃO funciona — use os items do Etched abaixo
        </span>
        <span className="ml-auto text-liberthia-300/40">{open ? '▼' : '▶'}</span>
      </button>
      {open && (
        <div className="mt-2 space-y-2">
          <p className="text-xs text-liberthia-300/70">
            Os discos do Etched (<code>etched:etched_music_disc</code>) precisam ser tocados nas
            jukeboxes do <strong>próprio mod Etched</strong>. A jukebox vanilla
            (<code>minecraft:jukebox</code>) <strong>NÃO toca</strong> esse tipo de disco —
            quando você tenta, o cliente pode travar ou só não tocar nada.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-1.5 text-xs">
            {ITEMS.map(it => (
              <div key={it.id}
                className={`rounded p-2 ${
                  it.good ? 'bg-emerald-500/10 border border-emerald-500/30' : 'bg-liberthia-900/40'
                }`}>
                <div className="font-bold">
                  <span className={it.good ? 'text-emerald-300' : 'text-liberthia-300/50'}>
                    {it.emoji}
                  </span> {it.label}
                </div>
                <code className="text-[10px] text-liberthia-300/50">{it.id}</code>
                <div className="text-[10px] text-liberthia-300/70 mt-1 mb-1">{it.desc}</div>
                <button className="btn-ghost btn-sm w-full text-[10px]"
                  disabled={!giveTarget}
                  onClick={() => giveMut.mutate({ player: giveTarget, item: it.id })}>
                  🎁 Dar {it.label.split(' ').slice(1).join(' ')}
                </button>
              </div>
            ))}
          </div>
          <div className="flex items-center gap-2 pt-2 border-t border-liberthia-800/60">
            <label className="text-xs whitespace-nowrap">Dar pra player:</label>
            <select className="input flex-1 text-xs" value={giveTarget}
              onChange={(e) => setGiveTarget(e.target.value)}>
              <option value="">Selecione…</option>
              {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
            </select>
          </div>
          <p className="text-[10px] text-liberthia-300/50 italic">
            💡 Fluxo recomendado: dá <strong>Album Jukebox</strong> pro player → ele coloca no chão
            → enfia o disco com RMB. Pra portátil, use <strong>Boombox</strong> (RMB segurando o disco).
          </p>
        </div>
      )}
    </div>
  )
}

function CookiesDialog({ onClose }: { onClose: () => void }) {
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)

  const statusQ = useQuery({
    queryKey: ['etched-cookies-status'],
    queryFn: api.etchedCookiesStatus,
  })
  const uploadMut = useMutation({
    mutationFn: (file: File) => api.etchedUploadCookies(file),
    onSuccess: (r) => {
      toast.ok(`🍪 cookies salvos (${r.size} bytes)`)
      qc.invalidateQueries({ queryKey: ['etched-cookies-status'] })
    },
    onError: (e: any) => toast.err(e.message),
  })
  const deleteMut = useMutation({
    mutationFn: api.etchedDeleteCookies,
    onSuccess: () => {
      toast.ok('🗑 cookies removidos')
      qc.invalidateQueries({ queryKey: ['etched-cookies-status'] })
    },
  })

  function submit() {
    const f = fileRef.current?.files?.[0]
    if (!f) { toast.err('escolhe um cookies.txt'); return }
    uploadMut.mutate(f)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onClose}>
      <div className="card max-w-xl w-full space-y-2 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold">🍪 YouTube Cookies (para bypass do bot-check)</h3>
        <p className="text-xs text-liberthia-300/70">
          O YouTube às vezes bloqueia o yt-dlp com "Sign in to confirm you're not a bot". A solução
          é subir cookies de uma sessão logada do teu navegador.
        </p>
        <div className="card text-xs space-y-1">
          <p className="font-bold">📋 Como exportar:</p>
          <ol className="pl-4 list-decimal space-y-0.5">
            <li>Instala extensão <strong>"Get cookies.txt LOCALLY"</strong> no Chrome/Firefox/Edge</li>
            <li>Vai pra <code>youtube.com</code> logado</li>
            <li>Click no ícone da extensão → "Export As" → cookies.txt</li>
            <li>Sobe esse arquivo aqui ↓</li>
          </ol>
        </div>

        <div className={`card text-xs ${statusQ.data?.present ? 'border-l-2 border-l-emerald-400' : 'border-l-2 border-l-zinc-600'}`}>
          {statusQ.data?.present ? (
            <>
              <p className="font-bold text-emerald-300">✓ cookies.txt já está configurado</p>
              <p className="text-liberthia-300/60">
                Tamanho: {statusQ.data.size} bytes ·
                Subido em: {statusQ.data.lastModified ? new Date(statusQ.data.lastModified).toLocaleString() : '?'}
              </p>
              <button className="btn-danger btn-sm mt-2"
                onClick={() => { if (confirm('Remover cookies?')) deleteMut.mutate() }}>
                🗑 Remover cookies atuais
              </button>
            </>
          ) : (
            <p className="text-liberthia-300/60">⚪ Sem cookies — usando só fallback de player_client</p>
          )}
        </div>

        <input type="file" accept=".txt" ref={fileRef} className="input" />
        <div className="flex gap-2 justify-end pt-2">
          <button className="btn-ghost" onClick={onClose}>Fechar</button>
          <button className="btn" disabled={uploadMut.isPending} onClick={submit}>
            {uploadMut.isPending ? '⌛ Subindo…' : '📤 Subir cookies'}
          </button>
        </div>

        <p className="text-[10px] text-liberthia-300/40 italic">
          ⚠ cookies dão acesso à tua conta YT — não compartilhe com terceiros.
          Ficam em <code>/app/data/etched-audio/yt-cookies.txt</code> no container.
        </p>
      </div>
    </div>
  )
}
