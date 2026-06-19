import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ForbiddenRule, ForbiddenInvocation } from '../lib/api'
import { toast } from '../store/toast'
import { renderMcText, MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { useEffectOptions, useParticleOptions, useEntityOptions, useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Forbidden Words — V2 (backend-driven).
 *
 * Toda a engine roda no backend (Spring Boot + PostgreSQL). Frontend é só
 * editor de regras. A engine continua processando chat events do mod
 * MESMO COM A PÁGINA FECHADA enquanto o backend estiver rodando.
 *
 * Endpoints usados:
 *  GET    /api/forbidden/rules
 *  POST   /api/forbidden/rules        (cria/atualiza)
 *  DELETE /api/forbidden/rules/{id}
 *  POST   /api/forbidden/rules/{id}/toggle
 *  POST   /api/forbidden/rules/{id}/simulate {playerUuid}
 *  GET    /api/forbidden/invocations?limit=50
 */

type ConsType = 'sound' | 'effect' | 'particle' | 'title' | 'lightning' | 'tellraw' | 'spawn_mob' | 'command'
type Consequence = { type: ConsType; [k: string]: any }

const COMMON_WORDS = [
  'vorashen', 'yhkuath', 'morghaur', 'isthar', 'eldritch', 'cosmico',
  'corra', 'fuja', 'socorro', 'ajuda', 'morte', 'morrer',
  'deus', 'demônio', 'maldição', 'medo', 'silêncio',
  'olho', 'sangue', 'porta', 'véu', 'sussurro', 'eco', 'sombra',
]


export function ForbiddenWordsPage() {
  const qc = useQueryClient()
  const rulesQ = useQuery({ queryKey: ['forbidden'], queryFn: api.forbiddenRules, refetchInterval: 3000 })
  const invQ = useQuery({ queryKey: ['forbidden-inv'], queryFn: () => api.forbiddenInvocations(50), refetchInterval: 4000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })

  const rules = rulesQ.data?.rules ?? []
  const invocations = invQ.data?.invocations ?? []
  const players = playersQ.data ?? []

  const [editing, setEditing] = useState<ForbiddenRule | null>(null)
  const [tab, setTab] = useState<'rules' | 'hist'>('rules')

  const saveMut = useMutation({
    mutationFn: api.forbiddenSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['forbidden'] }); toast.ok('✓ regra salva') },
    onError: (e: any) => toast.err(e.message ?? 'erro ao salvar'),
  })
  const deleteMut = useMutation({
    mutationFn: api.forbiddenDelete,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['forbidden'] }) },
  })
  const toggleMut = useMutation({
    mutationFn: api.forbiddenToggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forbidden'] }),
  })

  function newRule() {
    setEditing({
      emoji: '🩸',
      name: 'Nova Palavra Proibida',
      pattern: 'palavra',
      matchMode: 'contains',
      caseSensitive: false,
      target: 'speaker',
      consequencesJson: JSON.stringify([{ type: 'sound', sound: 'minecraft:ambient.cave', volume: 1, pitch: 1 }]),
      cooldownSec: 30,
      enabled: false,
    })
  }

  async function simulate(rule: ForbiddenRule, playerUuid: string) {
    try {
      await api.forbiddenSimulate(rule.id!, playerUuid)
      const p = players.find((x) => x.uuid === playerUuid)
      toast.ok(`${rule.emoji} simulado em ${p?.name ?? '?'}`)
    } catch (e: any) { toast.err(e.message ?? 'erro') }
  }

  const activeCount = rules.filter((r) => r.enabled).length

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🔮 Forbidden Words</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Engine de backend. Roda em PostgreSQL + Spring Boot, processa chat events do mod
            <span className="text-emerald-300 font-bold"> mesmo com a página fechada.</span>
          </p>
        </div>
        <div className="flex gap-2 items-center">
          <span className="badge badge-purple">{activeCount} ativas / {rules.length}</span>
          <button className="btn" onClick={newRule}>+ Nova Regra</button>
        </div>
      </header>

      {/* Status backend */}
      <div className="card-glow mb-3">
        <div className="flex items-center gap-3 flex-wrap text-sm">
          <span className={`badge ${rulesQ.error ? 'badge-red' : 'badge-green'}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${rulesQ.error ? 'bg-red-400' : 'bg-emerald-400'} animate-pulse`} />
            Backend {rulesQ.error ? 'OFFLINE' : 'conectado'}
          </span>
          <span className={`badge ${activeCount > 0 ? 'badge-green' : 'badge-red'}`}>🎯 {activeCount} regra(s) ativa(s)</span>
          <span className="badge badge-purple">⚡ {invocations.length} invocações no histórico</span>
        </div>
      </div>

      <div className="flex gap-2 mb-3">
        <button className={tab === 'rules' ? 'tab-item active' : 'tab-item'} onClick={() => setTab('rules')}>📜 Regras ({rules.length})</button>
        <button className={tab === 'hist' ? 'tab-item active' : 'tab-item'} onClick={() => setTab('hist')}>📋 Invocações ({invocations.length})</button>
      </div>

      {tab === 'rules' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {rules.length === 0 && (
            <div className="card text-center py-12 col-span-full">
              <div className="text-5xl mb-3 opacity-50">🔮</div>
              <p className="text-liberthia-300/70 text-sm">Nenhuma regra ainda. Clica em "+ Nova Regra".</p>
            </div>
          )}
          {rules.map((r) => (
            <div key={r.id} className={`card-glow ${r.enabled ? '!border-red-400/40 !bg-red-500/5' : ''}`}>
              <div className="flex items-start gap-3 mb-2">
                <div className="text-4xl drop-shadow-[0_0_8px_rgba(239,68,68,0.5)]">{r.emoji}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold truncate">{r.name}</div>
                  <div className="text-[10px] text-liberthia-300/60">
                    <code className="bg-liberthia-900/60 px-1 rounded">"{r.pattern}"</code> · {r.matchMode}{r.caseSensitive ? ' Aa' : ''}
                  </div>
                  <div className="text-[10px] text-liberthia-300/40">
                    alvo: {r.target} · cd: {r.cooldownSec}s · ⚡ {r.triggered ?? 0}
                  </div>
                </div>
              </div>
              <ConsBadges json={r.consequencesJson} />
              <SimForm rule={r} players={players} onSim={simulate} />
              <div className="grid grid-cols-3 gap-1 mt-2">
                <button className={r.enabled ? 'btn-success btn-sm' : 'btn-amber btn-sm pulse-glow'}
                  onClick={() => toggleMut.mutate(r.id!)}>
                  {r.enabled ? '⏸ ATIVO' : '▶ ATIVAR'}
                </button>
                <button className="btn-ghost btn-sm" onClick={() => setEditing(r)}>✎</button>
                <button className="btn-ghost btn-sm" onClick={() => {
                  if (confirm(`Excluir "${r.name}"?`)) deleteMut.mutate(r.id!)
                }}>🗑</button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card-glow">
          <h3 className="font-bold mb-2">📋 Últimas invocações (persistidas no PostgreSQL)</h3>
          {invocations.length === 0 ? <p className="text-xs italic text-liberthia-300/50">Sem invocações ainda.</p> : (
            <div className="space-y-1 max-h-[60vh] overflow-y-auto">
              {invocations.map((h) => (
                <div key={h.id} className="flex items-center gap-2 bg-liberthia-900/40 rounded px-2 py-1 text-xs">
                  <span className="text-[10px] text-liberthia-300/40 font-mono w-32 shrink-0">{new Date(h.ts).toLocaleString()}</span>
                  <span className="font-bold text-red-300 shrink-0">{h.speakerName}</span>
                  <span className="text-liberthia-300/60">disse</span>
                  <code className="bg-liberthia-900/60 px-1 rounded shrink-0">"{h.word}"</code>
                  <span className="text-liberthia-300/40">→</span>
                  <span className="truncate">{h.ruleName}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {editing && <RuleEditor rule={editing} onSave={(r) => saveMut.mutate(r, { onSuccess: () => setEditing(null) })} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function ConsBadges({ json }: { json: string }) {
  try {
    const list = JSON.parse(json) as Consequence[]
    return (
      <div className="flex flex-wrap gap-1 mb-2">
        {list.slice(0, 5).map((c, i) => (
          <span key={i} className="badge badge-red text-[10px]">{consBadge(c)}</span>
        ))}
        {list.length > 5 && <span className="badge badge-purple text-[10px]">+{list.length - 5}</span>}
      </div>
    )
  } catch { return <div className="text-[10px] text-red-300">json inválido</div> }
}

function consBadge(c: Consequence): string {
  switch (c.type) {
    case 'sound': return '🔊 ' + (c.sound ?? '').replace('minecraft:', '').slice(0, 14)
    case 'effect': return '⚗ ' + (c.effect ?? '').replace('minecraft:', '').slice(0, 14)
    case 'particle': return '✨ ' + (c.particle ?? '').replace('minecraft:', '').slice(0, 14)
    case 'title': return '🅰 ' + (c.title ?? '').replace(/§./g, '').slice(0, 14)
    case 'lightning': return '⚡'
    case 'tellraw': return '💬 ' + (c.message ?? '').replace(/§./g, '').slice(0, 14)
    case 'spawn_mob': return '👹 ' + (c.entity ?? '').replace('minecraft:', '')
    case 'command': return '⌨ ' + (c.command ?? '').slice(0, 12)
  }
  return c.type
}

function SimForm({ rule, players, onSim }: { rule: ForbiddenRule; players: any[]; onSim: (r: ForbiddenRule, uuid: string) => void }) {
  const [target, setTarget] = useState('')
  return (
    <div className="flex gap-1">
      <select className="input text-xs flex-1" value={target} onChange={(e) => setTarget(e.target.value)}>
        <option value="">— simular em —</option>
        {players.map((p: any) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
      </select>
      <button className="btn-amber btn-sm" disabled={!target} onClick={() => onSim(rule, target)}>🧪</button>
    </div>
  )
}

function RuleEditor({ rule, onSave, onCancel }: { rule: ForbiddenRule; onSave: (r: ForbiddenRule) => void; onCancel: () => void }) {
  const [r, setR] = useState<ForbiddenRule>(rule)
  const [consequences, setConsequences] = useState<Consequence[]>(() => {
    try { return JSON.parse(rule.consequencesJson) as Consequence[] }
    catch { return [] }
  })

  function update(c: Partial<ForbiddenRule>) {
    setR({ ...r, ...c })
  }

  function addCons(t: ConsType) {
    const blank: Record<ConsType, Consequence> = {
      sound: { type: 'sound', sound: 'minecraft:ambient.cave', volume: 1, pitch: 1 },
      effect: { type: 'effect', effect: 'minecraft:slowness', durationSec: 10, amplifier: 0 },
      particle: { type: 'particle', particle: 'minecraft:smoke', count: 30, offsetY: 1 },
      title: { type: 'title', title: '§4§l...', subtitle: '§c§o...', fadeIn: 10, stay: 50, fadeOut: 10 },
      lightning: { type: 'lightning' },
      tellraw: { type: 'tellraw', target: 'speaker', message: '§4§o— ela ouviu —' },
      spawn_mob: { type: 'spawn_mob', entity: 'minecraft:zombie', count: 1, offsetX: 4, offsetZ: 4 },
      command: { type: 'command', command: 'say —' },
    }
    setConsequences([...consequences, blank[t]])
  }

  function updateCons(i: number, p: Partial<Consequence>) {
    setConsequences(consequences.map((c, j) => j === i ? { ...c, ...p } : c))
  }
  function moveCons(i: number, dir: -1 | 1) {
    const j = i + dir; if (j < 0 || j >= consequences.length) return
    const arr = [...consequences]
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
    setConsequences(arr)
  }

  function save() {
    onSave({ ...r, consequencesJson: JSON.stringify(consequences) })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🔮 Editar Regra</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <input className="input text-2xl text-center" value={r.emoji} onChange={(e) => update({ emoji: e.target.value })} />
          <input className="input font-bold" value={r.name} onChange={(e) => update({ name: e.target.value })} />
        </div>

        <h4 className="font-bold text-sm mb-1">📜 Palavra/Pattern</h4>
        <div className="grid grid-cols-[2fr_1fr_auto] gap-2 mb-3">
          <input className="input font-mono" value={r.pattern} onChange={(e) => update({ pattern: e.target.value })} />
          <select className="input text-xs" value={r.matchMode} onChange={(e) => update({ matchMode: e.target.value as any })}>
            <option value="exact">exato</option>
            <option value="contains">contém</option>
            <option value="regex">regex</option>
          </select>
          <label className="flex items-center gap-1 text-xs px-2">
            <input type="checkbox" checked={r.caseSensitive} onChange={(e) => update({ caseSensitive: e.target.checked })} /> Aa
          </label>
        </div>

        <details className="mb-3">
          <summary className="text-xs cursor-pointer text-liberthia-300/60">💡 Palavras sugeridas</summary>
          <div className="flex flex-wrap gap-1 mt-2">
            {COMMON_WORDS.map((w) => (
              <button key={w} className="badge badge-purple text-[10px] hover:bg-purple-500/30" onClick={() => update({ pattern: w })}>{w}</button>
            ))}
          </div>
        </details>

        <div className="grid grid-cols-2 gap-3 mb-3">
          <div>
            <label className="label">Alvo</label>
            <select className="input text-xs" value={r.target} onChange={(e) => update({ target: e.target.value as any })}>
              <option value="speaker">só quem falou</option>
              <option value="everyone">todos online</option>
              <option value="both">falou + outros</option>
            </select>
          </div>
          <div>
            <label className="label">Cooldown por player (s)</label>
            <input type="number" className="input" value={r.cooldownSec} min={0}
              onChange={(e) => update({ cooldownSec: Number(e.target.value) })} />
          </div>
        </div>

        <h4 className="font-bold text-sm mb-1 mt-4">⚡ Consequências ({consequences.length})</h4>
        <div className="flex flex-wrap gap-1 mb-2">
          <button className="btn-ghost btn-sm" onClick={() => addCons('sound')}>+ 🔊 sound</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('effect')}>+ ⚗ effect</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('particle')}>+ ✨ particle</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('title')}>+ 🅰 title</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('lightning')}>+ ⚡</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('tellraw')}>+ 💬 tellraw</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('spawn_mob')}>+ 👹 mob</button>
          <button className="btn-ghost btn-sm" onClick={() => addCons('command')}>+ ⌨ cmd</button>
        </div>

        <div className="space-y-2 mb-4">
          {consequences.map((c, i) => (
            <div key={i} className="bg-liberthia-900/40 rounded p-2">
              <div className="flex items-center gap-2 mb-1">
                <span className="badge badge-red shrink-0">#{i + 1}</span>
                <span className="text-xs font-mono font-bold text-red-300 shrink-0">{c.type}</span>
                <div className="flex-1" />
                <button className="btn-ghost btn-sm" onClick={() => moveCons(i, -1)}>↑</button>
                <button className="btn-ghost btn-sm" onClick={() => moveCons(i, 1)}>↓</button>
                <button className="btn-ghost btn-sm" onClick={() => setConsequences(consequences.filter((_, j) => j !== i))}>🗑</button>
              </div>
              <ConsEditor c={c} onChange={(p) => updateCons(i, p)} />
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={save}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

function ConsEditor({ c, onChange }: { c: Consequence; onChange: (p: Partial<Consequence>) => void }) {
  const effects = useEffectOptions()
  const particles = useParticleOptions()
  const entities = useEntityOptions()
  const sounds = useSoundOptions()
  switch (c.type) {
    case 'sound': return (
      <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
        <Autocomplete value={c.sound ?? ''} onChange={(v) => onChange({ sound: v })} options={sounds} placeholder="minecraft:ambient.cave" />
        <input type="number" step={0.1} className="input text-xs" placeholder="vol" value={c.volume ?? 1} onChange={(e) => onChange({ volume: Number(e.target.value) })} />
        <input type="number" step={0.1} className="input text-xs" placeholder="pitch" value={c.pitch ?? 1} onChange={(e) => onChange({ pitch: Number(e.target.value) })} />
      </div>
    )
    case 'effect': return (
      <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
        <Autocomplete value={c.effect ?? ''} onChange={(v) => onChange({ effect: v })} options={effects} placeholder="minecraft:slowness" />
        <input type="number" className="input text-xs" placeholder="durSec" value={c.durationSec ?? 10} onChange={(e) => onChange({ durationSec: Number(e.target.value) })} />
        <input type="number" className="input text-xs" placeholder="amp" value={c.amplifier ?? 0} onChange={(e) => onChange({ amplifier: Number(e.target.value) })} />
      </div>
    )
    case 'particle': return (
      <div className="grid grid-cols-[2fr_1fr_1fr] gap-1">
        <Autocomplete value={c.particle ?? ''} onChange={(v) => onChange({ particle: v })} options={particles} placeholder="minecraft:smoke" />
        <input type="number" className="input text-xs" placeholder="count" value={c.count ?? 30} onChange={(e) => onChange({ count: Number(e.target.value) })} />
        <input type="number" className="input text-xs" placeholder="offY" value={c.offsetY ?? 1} onChange={(e) => onChange({ offsetY: Number(e.target.value) })} />
      </div>
    )
    case 'title': return (
      <div className="space-y-1">
        <input className="input text-xs font-mono" placeholder="title §..." value={c.title ?? ''} onChange={(e) => onChange({ title: e.target.value })} />
        <input className="input text-xs font-mono" placeholder="subtitle §..." value={c.subtitle ?? ''} onChange={(e) => onChange({ subtitle: e.target.value })} />
        <div className="grid grid-cols-3 gap-1">
          <input type="number" className="input text-xs" placeholder="fadeIn" value={c.fadeIn ?? 10} onChange={(e) => onChange({ fadeIn: Number(e.target.value) })} />
          <input type="number" className="input text-xs" placeholder="stay" value={c.stay ?? 50} onChange={(e) => onChange({ stay: Number(e.target.value) })} />
          <input type="number" className="input text-xs" placeholder="fadeOut" value={c.fadeOut ?? 10} onChange={(e) => onChange({ fadeOut: Number(e.target.value) })} />
        </div>
        <div className="text-[10px] mt-1">preview: {renderMcText(c.title ?? '')} / {renderMcText(c.subtitle ?? '')}</div>
      </div>
    )
    case 'lightning': return <span className="text-xs text-liberthia-300/50">— sem parâmetros (cai em cima do alvo) —</span>
    case 'tellraw': return (
      <div className="space-y-1">
        <select className="input text-xs" value={c.target ?? 'speaker'} onChange={(e) => onChange({ target: e.target.value })}>
          <option value="speaker">só pro alvo</option>
          <option value="broadcast">broadcast @a</option>
        </select>
        <MinecraftFormatter value={c.message ?? ''} onChange={(v) => onChange({ message: v })} rows={1} maxChars={200} showCounter={false} />
      </div>
    )
    case 'spawn_mob': return (
      <div className="space-y-1">
        <div className="grid grid-cols-[2fr_1fr] gap-1">
          <Autocomplete value={c.entity ?? ''} onChange={(v) => onChange({ entity: v })} options={entities} placeholder="minecraft:zombie" />
          <input type="number" className="input text-xs" placeholder="count" value={c.count ?? 1} onChange={(e) => onChange({ count: Number(e.target.value) })} />
        </div>
        <div className="grid grid-cols-2 gap-1">
          <input type="number" className="input text-xs" placeholder="offX" value={c.offsetX ?? 0} onChange={(e) => onChange({ offsetX: Number(e.target.value) })} />
          <input type="number" className="input text-xs" placeholder="offZ" value={c.offsetZ ?? 0} onChange={(e) => onChange({ offsetZ: Number(e.target.value) })} />
        </div>
      </div>
    )
    case 'command': return <input className="input text-xs font-mono" placeholder="say hello (sem /)" value={c.command ?? ''} onChange={(e) => onChange({ command: e.target.value })} />
  }
  return null
}
