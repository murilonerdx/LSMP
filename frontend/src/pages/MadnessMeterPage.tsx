import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, MadnessConfigDto } from '../lib/api'
import { toast } from '../store/toast'
import { Autocomplete } from '../components/Autocomplete'
import { useSoundOptions, useCommandOptions } from '../lib/mcAutocomplete'

/**
 * Madness Meter — V2 (backend-driven).
 *
 * Sanidade vive no PostgreSQL. Engine roda no Spring Boot via @Scheduled
 * continuamente — mesmo com a página fechada.
 *
 * Features:
 *  - Whisper mensagens: "player" (só pro afetado) ou "everyone" (broadcast)
 *  - Rotação random nas linhas (vem do zoneMessagesJson)
 *  - FX no mundo: random_particle | ghost_armor_stand | random_sound | falling_block | random_lightning
 *  - Decay automático + zonas: Lúcido / Inquieto / Perturbado / Histérico / Consumido
 */

const ZONE_META = [
  { key: 'lucido', from: 80, to: 100, label: 'Lúcido', color: 'bg-emerald-400', glow: 'shadow-emerald-500/30', text: 'text-emerald-300' },
  { key: 'inquieto', from: 60, to: 79, label: 'Inquieto', color: 'bg-amber-400', glow: 'shadow-amber-500/40', text: 'text-amber-300' },
  { key: 'perturbado', from: 40, to: 59, label: 'Perturbado', color: 'bg-orange-500', glow: 'shadow-orange-500/40', text: 'text-orange-300' },
  { key: 'histerico', from: 20, to: 39, label: 'Histérico', color: 'bg-red-500', glow: 'shadow-red-500/50', text: 'text-red-300' },
  { key: 'consumido', from: 0, to: 19, label: 'Consumido', color: 'bg-purple-700', glow: 'shadow-purple-500/60', text: 'text-purple-300' },
]

const WORLD_FX_OPTIONS = [
  { key: 'random_particle', label: '✨ Partículas aleatórias', desc: 'Smoke/soul/end_rod em pos aleatória perto do player' },
  { key: 'ghost_armor_stand', label: '👤 Fantasma armor_stand', desc: 'Spawn de armor_stand com player_head; despawna em 20s' },
  { key: 'random_sound', label: '🔊 Sons aleatórios', desc: 'Heartbeat/ghast/ambient em pitch baixo' },
  { key: 'falling_block', label: '🟫 Bloco caindo', desc: 'Soul sand/obsidiana caindo do céu' },
  { key: 'random_lightning', label: '⚡ Lightning aleatório', desc: 'Raio em pos aleatória perto (chance maior em Consumido)' },
]

function zoneFor(s: number) {
  return ZONE_META.find((z) => s >= z.from && s <= z.to) ?? ZONE_META[0]
}

export function MadnessMeterPage() {
  const qc = useQueryClient()
  const stateQ = useQuery({ queryKey: ['madness'], queryFn: api.madnessState, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 3000 })

  const state = stateQ.data?.state ?? []
  const config = stateQ.data?.config
  const players = playersQ.data ?? []

  const [editingCfg, setEditingCfg] = useState(false)

  const saveCfg = useMutation({
    mutationFn: api.madnessSaveConfig,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['madness'] }); toast.ok('✓ config salva') },
    onError: (e: any) => toast.err(e.message),
  })
  const toggleMut = useMutation({
    mutationFn: api.madnessToggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['madness'] }),
  })
  // Optimistic update — mexe no cache local NA HORA pra o slider não "voltar
  // atrás" enquanto o backend confirma. Antes, ao arrastar o slider de 1 player,
  // o cache só atualizava após o roundtrip; isso fazia o valor exibido oscilar
  // e (combinado com sort por sanidade que existia) parecia que vários cards
  // mudavam ao mesmo tempo.
  function patchLocalSanity(uuid: string, v: number) {
    qc.setQueryData(['madness'], (old: any) => {
      if (!old?.state) return old
      const clamped = Math.max(0, Math.min(100, v))
      return { ...old, state: old.state.map((s: any) => s.uuid === uuid ? { ...s, sanity: clamped } : s) }
    })
  }
  const setSanityMut = useMutation({
    mutationFn: ({ uuid, v }: { uuid: string; v: number }) => api.madnessSetSanity(uuid, v),
    onMutate: ({ uuid, v }) => patchLocalSanity(uuid, v),
    onSettled: () => qc.invalidateQueries({ queryKey: ['madness'] }),
  })
  const deltaMut = useMutation({
    mutationFn: ({ uuid, d }: { uuid: string; d: number }) => api.madnessDelta(uuid, d),
    onMutate: ({ uuid, d }) => {
      const cur = (qc.getQueryData(['madness']) as any)?.state?.find((s: any) => s.uuid === uuid)?.sanity ?? 100
      patchLocalSanity(uuid, cur + d)
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ['madness'] }),
  })
  const resetMut = useMutation({
    mutationFn: api.madnessResetAll,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['madness'] }),
  })
  const manifestMut = useMutation({
    mutationFn: api.madnessManifest,
    onSuccess: () => toast.ok('⚡ manifestado'),
  })

  function applyAll(d: number) {
    for (const s of state) deltaMut.mutate({ uuid: s.uuid, d })
  }

  function eldritchSurge() {
    for (const s of state) setSanityMut.mutate({ uuid: s.uuid, v: Math.random() * 18 })
    toast.ok('🌑 Eldritch Surge')
  }

  // Combina state DB + players online.
  // IMPORTANTE: sort é estável (online + nome). NÃO ordenar por sanidade —
  // se ordenasse, mexer no slider de um player faria o card pular de posição
  // e os outros deslizariam pra preencher, dando a sensação visual de que
  // VÁRIOS cards mudaram ao mesmo tempo. Ordem fixa por nome resolve.
  const rows = useMemo(() => {
    const allUuids = new Set<string>([...state.map((s) => s.uuid), ...players.map((p) => p.uuid)])
    return Array.from(allUuids).map((uuid) => {
      const dbRow = state.find((s) => s.uuid === uuid)
      const p = players.find((x) => x.uuid === uuid)
      return {
        uuid,
        name: p?.name ?? dbRow?.name ?? uuid.slice(0, 8),
        sanity: dbRow?.sanity ?? 100,
        online: !!p,
      }
    }).sort((a, b) => Number(b.online) - Number(a.online) || a.name.localeCompare(b.name))
  }, [state, players])

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🧠 Madness Meter</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Engine de backend em PostgreSQL. Decay + zonas + whispers + FX no mundo.
            <span className="text-emerald-300 font-bold"> Roda continuamente, mesmo com a página fechada.</span>
          </p>
        </div>
        <div className="flex gap-2">
          <button className={config?.enabled ? 'btn-danger pulse-glow' : 'btn-success'} onClick={() => toggleMut.mutate()}>
            {config?.enabled ? '⏸ Pausar Engine' : '▶ Iniciar Engine'}
          </button>
          <button className="btn-ghost btn-sm" onClick={() => setEditingCfg(true)}>⚙ Config</button>
        </div>
      </header>

      {/* Status */}
      <div className="card-glow mb-3">
        <div className="flex items-center gap-3 flex-wrap text-sm">
          <span className={`badge ${stateQ.error ? 'badge-red' : 'badge-green'}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${stateQ.error ? 'bg-red-400' : 'bg-emerald-400'} animate-pulse`} />
            Backend {stateQ.error ? 'OFFLINE' : 'OK'}
          </span>
          <span className={`badge ${config?.enabled ? 'badge-green' : 'badge-red'}`}>
            {config?.enabled ? '🟢 Engine RODANDO' : '🔴 Engine PARADA'}
          </span>
          {config && (
            <>
              <span className="badge badge-purple">tick: {config.tickIntervalSec}s</span>
              <span className="badge badge-purple">decay: {config.decayPerMinute}/min</span>
              <span className="badge badge-purple">whisper: {config.whisperBroadcastMode === 'everyone' ? '@a broadcast' : '@speaker'}</span>
              <span className="badge badge-purple">FX mundo: {config.worldFxEnabled ? `✓ (${(config.worldFxChance * 100).toFixed(0)}%)` : '✗'}</span>
            </>
          )}
        </div>
      </div>

      {/* Mass actions */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-2 mb-4">
        <button className="btn-ghost" onClick={() => applyAll(+25)}>+25 ALL</button>
        <button className="btn-ghost" onClick={() => applyAll(-25)}>-25 ALL</button>
        <button className="btn-ghost" onClick={() => applyAll(-50)}>-50 ALL</button>
        <button className="btn-ghost" onClick={() => resetMut.mutate(100)}>💎 Restore</button>
        <button className="btn-danger" onClick={eldritchSurge}>🌑 Surge</button>
      </div>

      {/* Zone legend */}
      <div className="flex flex-wrap gap-2 mb-3 text-[10px]">
        {ZONE_META.map((z) => (
          <span key={z.key} className={`px-2 py-0.5 rounded border ${z.text}`} style={{ borderColor: 'currentColor', opacity: 0.7 }}>
            {z.from}-{z.to}: {z.label}
          </span>
        ))}
      </div>

      {/* Player grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {rows.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">🧠</div>
            <p className="text-liberthia-300/70 text-sm">Sem players. Quando alguém entrar, aparece aqui.</p>
          </div>
        )}
        {rows.map((row) => {
          const z = zoneFor(row.sanity)
          const pct = row.sanity
          return (
            <div key={row.uuid} className={`card-glow shadow-lg ${z.glow} ${!row.online ? 'opacity-60' : ''}`}>
              <div className="flex items-center gap-2 mb-2">
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(row.name)}/32`} className="rounded"
                  onError={(ev) => { (ev.target as HTMLImageElement).style.display = 'none' }} />
                <div className="flex-1 min-w-0">
                  <div className="font-bold truncate">{row.name}</div>
                  <div className={`text-xs ${z.text} font-bold uppercase tracking-wider`}>
                    {z.label} {!row.online && <span className="text-liberthia-300/40 normal-case font-normal">(offline)</span>}
                  </div>
                </div>
                <div className={`text-3xl font-black tabular-nums ${z.text}`}>{Math.floor(pct)}</div>
              </div>
              <div className="h-3 bg-liberthia-900/70 rounded overflow-hidden mb-2 relative">
                <div className={`h-full ${z.color} transition-all`} style={{ width: `${pct}%` }} />
                {pct < 30 && <div className="absolute inset-0 animate-pulse bg-red-500/20" />}
              </div>
              <div className="grid grid-cols-5 gap-1 mb-1">
                <button className="btn-ghost btn-sm" onClick={() => deltaMut.mutate({ uuid: row.uuid, d: +10 })}>+10</button>
                <button className="btn-ghost btn-sm" onClick={() => deltaMut.mutate({ uuid: row.uuid, d: -10 })}>-10</button>
                <button className="btn-ghost btn-sm" onClick={() => deltaMut.mutate({ uuid: row.uuid, d: -25 })}>-25</button>
                <button className="btn-ghost btn-sm" onClick={() => setSanityMut.mutate({ uuid: row.uuid, v: 100 })}>💎</button>
                <button className="btn-ghost btn-sm" onClick={() => setSanityMut.mutate({ uuid: row.uuid, v: 0 })}>🌑</button>
              </div>
              <div className="grid grid-cols-[1fr_auto] gap-1 items-center">
                <input type="range" min={0} max={100} value={Math.floor(pct)}
                  onChange={(e) => setSanityMut.mutate({ uuid: row.uuid, v: Number(e.target.value) })}
                  className="w-full" />
                <button className="btn-amber btn-sm" title="Forçar manifestação atual" disabled={!row.online} onClick={() => manifestMut.mutate(row.uuid)}>⚡</button>
              </div>
            </div>
          )
        })}
      </div>

      {editingCfg && config && (
        <ConfigEditor
          cfg={config}
          onSave={(c) => saveCfg.mutate(c, { onSuccess: () => setEditingCfg(false) })}
          onCancel={() => setEditingCfg(false)}
        />
      )}
    </div>
  )
}

function ConfigEditor({ cfg, onSave, onCancel }: { cfg: MadnessConfigDto; onSave: (c: MadnessConfigDto) => void; onCancel: () => void }) {
  const [c, setC] = useState<MadnessConfigDto>(cfg)
  const sounds = useSoundOptions()
  const commands = useCommandOptions()

  // Parse zone messages from JSON
  const zoneMessages = (() => {
    try { return JSON.parse(c.zoneMessagesJson ?? '{}') as Record<string, string[]> }
    catch { return {} as Record<string, string[]> }
  })()
  function setZoneMessages(zone: string, lines: string[]) {
    const next = { ...zoneMessages, [zone]: lines }
    setC({ ...c, zoneMessagesJson: JSON.stringify(next) })
  }

  const zoneSounds = (() => {
    try { return JSON.parse(c.zoneSoundsJson ?? '{}') as Record<string, string[]> }
    catch { return {} as Record<string, string[]> }
  })()
  function setZoneSounds(zone: string, items: string[]) {
    const next = { ...zoneSounds, [zone]: items }
    setC({ ...c, zoneSoundsJson: JSON.stringify(next) })
  }

  const zoneCommands = (() => {
    try { return JSON.parse(c.zoneCommandsJson ?? '{}') as Record<string, string[]> }
    catch { return {} as Record<string, string[]> }
  })()
  function setZoneCommands(zone: string, items: string[]) {
    const next = { ...zoneCommands, [zone]: items }
    setC({ ...c, zoneCommandsJson: JSON.stringify(next) })
  }

  const fxTypes = (() => {
    try { return JSON.parse(c.worldFxTypesJson ?? '[]') as string[] }
    catch { return [] as string[] }
  })()
  function toggleFx(key: string) {
    const next = fxTypes.includes(key) ? fxTypes.filter((x) => x !== key) : [...fxTypes, key]
    setC({ ...c, worldFxTypesJson: JSON.stringify(next) })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⚙ Config Madness Meter</h3>

        <div className="grid grid-cols-2 gap-3 mb-4">
          <div>
            <label className="label">Tick interval (s)</label>
            <input type="number" className="input" value={c.tickIntervalSec} min={3} max={120}
              onChange={(e) => setC({ ...c, tickIntervalSec: Number(e.target.value) })} />
          </div>
          <div>
            <label className="label">Decay/min</label>
            <input type="number" className="input" value={c.decayPerMinute} step={0.1} min={0}
              onChange={(e) => setC({ ...c, decayPerMinute: Number(e.target.value) })} />
          </div>
        </div>

        <h4 className="font-bold mb-2">💬 Whispers</h4>
        <div className="card mb-4">
          <label className="label">Modo de broadcast das mensagens</label>
          <div className="grid grid-cols-2 gap-2 mb-3">
            <button className={c.whisperBroadcastMode === 'player' ? 'btn btn-sm' : 'btn-ghost btn-sm'}
              onClick={() => setC({ ...c, whisperBroadcastMode: 'player' })}>
              👤 Só pro player afetado
            </button>
            <button className={c.whisperBroadcastMode === 'everyone' ? 'btn btn-sm' : 'btn-ghost btn-sm'}
              onClick={() => setC({ ...c, whisperBroadcastMode: 'everyone' })}>
              📡 Broadcast pra TODOS
            </button>
          </div>
          <p className="text-[10px] text-liberthia-300/60 mb-3">
            "Broadcast" prefixa a mensagem com [nome] e manda no chat geral — todos veem.
            "Player" usa tellraw privado.
          </p>

          {ZONE_META.filter((z) => z.key !== 'lucido').map((z) => (
            <div key={z.key} className="mb-3">
              <label className={`label ${z.text}`}>📝 Mensagens — {z.label} (1 por linha, §-codes)</label>
              <textarea className="input text-xs font-mono" rows={3}
                value={(zoneMessages[z.key] ?? []).join('\n')}
                onChange={(e) => setZoneMessages(z.key, e.target.value.split('\n').filter(Boolean))} />
            </div>
          ))}
        </div>

        <h4 className="font-bold mb-2">🔊 Sons por zona (com autocomplete)</h4>
        <div className="card mb-4">
          {ZONE_META.filter((z) => z.key !== 'lucido').map((z) => {
            const list = zoneSounds[z.key] ?? []
            return (
              <div key={z.key} className="mb-3">
                <label className={`label ${z.text}`}>🔊 {z.label}</label>
                <div className="space-y-1">
                  {list.map((s, i) => (
                    <div key={i} className="grid grid-cols-[1fr_36px] gap-1">
                      <Autocomplete value={s} options={sounds} placeholder="minecraft:..."
                        onChange={(v) => setZoneSounds(z.key, list.map((x, j) => j === i ? v : x))} />
                      <button className="btn-ghost btn-sm" onClick={() => setZoneSounds(z.key, list.filter((_, j) => j !== i))}>🗑</button>
                    </div>
                  ))}
                  <button className="btn-ghost btn-sm" onClick={() => setZoneSounds(z.key, [...list, ''])}>+ Som</button>
                </div>
              </div>
            )
          })}
        </div>

        <h4 className="font-bold mb-2">⌨ Comandos por zona (com autocomplete)</h4>
        <p className="text-[10px] text-liberthia-300/60 mb-2">
          Executados pelo backend quando o player entra na zona. Use <code>{'{player}'}</code> pra
          o nome dele e <code>{'{target}'}</code> em templates.
        </p>
        <div className="card mb-4">
          {ZONE_META.filter((z) => z.key !== 'lucido').map((z) => {
            const list = zoneCommands[z.key] ?? []
            return (
              <div key={z.key} className="mb-3">
                <label className={`label ${z.text}`}>⌨ {z.label}</label>
                <div className="space-y-1">
                  {list.map((cmd, i) => (
                    <div key={i} className="grid grid-cols-[1fr_36px] gap-1">
                      <Autocomplete value={cmd} options={commands} placeholder="effect give {player} minecraft:darkness 60 0"
                        onChange={(v) => setZoneCommands(z.key, list.map((x, j) => j === i ? v : x))} />
                      <button className="btn-ghost btn-sm" onClick={() => setZoneCommands(z.key, list.filter((_, j) => j !== i))}>🗑</button>
                    </div>
                  ))}
                  <button className="btn-ghost btn-sm" onClick={() => setZoneCommands(z.key, [...list, ''])}>+ Comando</button>
                </div>
              </div>
            )
          })}
        </div>

        <h4 className="font-bold mb-2">🌍 FX no mundo</h4>
        <div className="card mb-4">
          <label className="flex items-center gap-2 mb-2">
            <input type="checkbox" checked={c.worldFxEnabled} onChange={(e) => setC({ ...c, worldFxEnabled: e.target.checked })} />
            <span className="font-bold">Habilitar efeitos visuais aleatórios no mundo</span>
          </label>
          <div className="mb-3">
            <label className="label">Chance por tick (0-1): {(c.worldFxChance * 100).toFixed(0)}%</label>
            <input type="range" min={0} max={1} step={0.05} className="w-full"
              value={c.worldFxChance} onChange={(e) => setC({ ...c, worldFxChance: Number(e.target.value) })} />
          </div>
          <label className="label mt-3">Tipos habilitados (clique pra alternar)</label>
          <div className="space-y-1">
            {WORLD_FX_OPTIONS.map((opt) => (
              <label key={opt.key} className={`flex items-center gap-2 p-2 rounded cursor-pointer ${fxTypes.includes(opt.key) ? 'bg-purple-500/20 border border-purple-400/40' : 'bg-liberthia-900/40 border border-transparent'}`}>
                <input type="checkbox" checked={fxTypes.includes(opt.key)} onChange={() => toggleFx(opt.key)} />
                <div className="flex-1">
                  <div className="text-sm font-bold">{opt.label}</div>
                  <div className="text-[10px] text-liberthia-300/60">{opt.desc}</div>
                </div>
              </label>
            ))}
          </div>
          <p className="text-[10px] text-liberthia-300/50 mt-2">
            FX só dispara em zonas Inquieto+ e só quando o player está online.
          </p>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(c)}>Salvar Config</button>
        </div>
      </div>
    </div>
  )
}
