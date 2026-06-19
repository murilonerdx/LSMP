import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, PilgrimageDto, PilgrimageStep, GlyphDto, SaveAnchorDto } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Sacred Pilgrimage — sequência ordenada de estações que o player precisa
 * visitar pra completar. Engine roda no backend (polling 2s).
 *
 * Cada estação aceita 4 tipos:
 *  - memorial (refId vindo do kv 'memorials')
 *  - glyph (refId do banco)
 *  - anchor (refId do banco)
 *  - coords (x,y,z,dim,radius livres)
 */

/** Templates pré-fabricados — substituem stepsJson por exemplos coerentes. */
const PILGRIMAGE_PRESETS: Omit<PilgrimageDto, 'id'>[] = [
  {
    emoji: '🌅',
    name: 'A Trilha do Aurora',
    description: 'Visite 3 pontos cardeais ao nascer do sol pra ganhar bênção celeste.',
    stepsJson: JSON.stringify([
      { type: 'coords', x: 100, y: 80, z: 0, dim: 'minecraft:overworld', radius: 8, label: 'Pedra do Leste' },
      { type: 'coords', x: 0, y: 80, z: 100, dim: 'minecraft:overworld', radius: 8, label: 'Pedra do Sul' },
      { type: 'coords', x: -100, y: 80, z: 0, dim: 'minecraft:overworld', radius: 8, label: 'Pedra do Oeste' },
    ]),
    timeLimitSec: 1800,
    rewardCmd: 'effect give {player} minecraft:hero_of_the_village 86400 1',
    startMsg: '§e§l🌅 {player} caminha sob o céu do amanhecer...',
    stepMsg: '§6⛧ Estação §e{step}§6/§e{total} §6alcançada',
    completeMsg: '§e§l☀ AURORA COMPLETA ☀ §r§eque a luz te acompanhe sempre, {player}',
    stepSound: 'minecraft:block.bell.use',
    completeSound: 'minecraft:ui.toast.challenge_complete',
    enabled: false,
  },
  {
    emoji: '🩸',
    name: 'Procissão dos Mortos',
    description: 'Passe por todos os memoriais dos heróis caídos pra honrá-los.',
    stepsJson: JSON.stringify([
      { type: 'memorial', refId: 'MEMORIAL_ID_AQUI', label: 'Memorial do Fundador' },
      { type: 'memorial', refId: 'MEMORIAL_ID_AQUI', label: 'Memorial do Guerreiro' },
      { type: 'memorial', refId: 'MEMORIAL_ID_AQUI', label: 'Memorial do Cronista' },
    ]),
    timeLimitSec: 0,
    rewardCmd: 'give {player} minecraft:totem_of_undying 1',
    startMsg: '§4§l🩸 {player} carrega tochas pra honrar os caídos...',
    stepMsg: '§c⚱ §e{step}§c/§e{total} §clembrado',
    completeMsg: '§4§l⚱ HONRA CONSAGRADA ⚱ §r§4os mortos não esquecem, {player}',
    stepSound: 'minecraft:entity.elder_guardian.curse',
    completeSound: 'minecraft:block.bell.use',
    enabled: false,
  },
  {
    emoji: '🜲',
    name: 'O Caminho do Vazio',
    description: 'Atravesse Overworld → Nether → End rumo ao silêncio final.',
    stepsJson: JSON.stringify([
      { type: 'coords', x: 0, y: 64, z: 0, dim: 'minecraft:overworld', radius: 10, label: 'Spawn — início' },
      { type: 'coords', x: 0, y: 64, z: 0, dim: 'minecraft:the_nether', radius: 10, label: 'Portal do Inferno' },
      { type: 'coords', x: 100, y: 50, z: 0, dim: 'minecraft:the_end', radius: 15, label: 'Ilhas do Vazio' },
    ]),
    timeLimitSec: 7200,
    rewardCmd: 'give {player} minecraft:dragon_egg 1',
    startMsg: '§5§l🜲 {player} caminha entre os mundos...',
    stepMsg: '§d⛧ §5{step}§d/§5{total} §datravessado',
    completeMsg: '§0§l🜲 O VAZIO TE CONHECE 🜲 §r§5{player} tocou o nada',
    stepSound: 'minecraft:portal.travel',
    completeSound: 'minecraft:entity.ender_dragon.death',
    enabled: false,
  },
  {
    emoji: '🌳',
    name: 'Trilha das 4 Estações',
    description: 'Visite biomas opostos: deserto, taiga, jungle, mushroom — símbolo de adaptação.',
    stepsJson: JSON.stringify([
      { type: 'coords', x: 500, y: 70, z: 0, dim: 'minecraft:overworld', radius: 20, label: '🏜 Deserto' },
      { type: 'coords', x: 0, y: 70, z: 500, dim: 'minecraft:overworld', radius: 20, label: '🌲 Taiga' },
      { type: 'coords', x: -500, y: 70, z: 0, dim: 'minecraft:overworld', radius: 20, label: '🌴 Jungle' },
      { type: 'coords', x: 0, y: 70, z: -500, dim: 'minecraft:overworld', radius: 20, label: '🍄 Mushroom Island' },
    ]),
    timeLimitSec: 10800,
    rewardCmd: 'give {player} minecraft:elytra 1',
    startMsg: '§a§l🌍 {player} caminha pelas 4 estações...',
    stepMsg: '§2⛧ §a{step}§2/§a{total} §abioma alcançado',
    completeMsg: '§a§l🌍 ANDARILHO MUNDIAL 🌍 §r§a{player} provou ser de qualquer terra',
    stepSound: 'minecraft:entity.experience_orb.pickup',
    completeSound: 'minecraft:entity.player.levelup',
    enabled: false,
  },
  {
    emoji: '⚔',
    name: 'Caminho do Sangue',
    description: 'Confronto direto: 3 estações onde o player luta contra mobs cada vez mais fortes.',
    stepsJson: JSON.stringify([
      { type: 'coords', x: 200, y: 50, z: 0, dim: 'minecraft:overworld', radius: 5, label: '⚔ Arena Zumbi' },
      { type: 'coords', x: 0, y: 50, z: 200, dim: 'minecraft:overworld', radius: 5, label: '⚔ Arena Esqueleto' },
      { type: 'coords', x: 0, y: 50, z: 0, dim: 'minecraft:the_nether', radius: 5, label: '⚔ Arena Blaze' },
    ]),
    timeLimitSec: 3600,
    rewardCmd: 'give {player} minecraft:netherite_sword{Enchantments:[{id:"minecraft:sharpness",lvl:5s}]} 1',
    startMsg: '§4§l⚔ {player} desembainha a espada do sangue...',
    stepMsg: '§c⚔ §4{step}§c/§4{total} §carena conquistada',
    completeMsg: '§4§l⚔ CAMINHO COMPLETO ⚔ §r§c{player} provou em sangue',
    stepSound: 'minecraft:entity.ravager.roar',
    completeSound: 'minecraft:entity.wither.death',
    enabled: false,
  },
]

export function PilgrimagePage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['pilgrimages'], queryFn: api.pilgrimagesList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const pilgrimages = q.data?.pilgrimages ?? []
  const progress = q.data?.progress ?? []
  const [editing, setEditing] = useState<PilgrimageDto | null>(null)

  const save = useMutation({
    mutationFn: api.pilgrimageSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pilgrimages'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({ mutationFn: api.pilgrimageDelete, onSuccess: () => qc.invalidateQueries({ queryKey: ['pilgrimages'] }) })
  const toggle = useMutation({ mutationFn: api.pilgrimageToggle, onSuccess: () => qc.invalidateQueries({ queryKey: ['pilgrimages'] }) })

  function newPil() {
    setEditing({
      emoji: '⚔',
      name: 'Nova Peregrinação',
      description: 'Visite cada estação em ordem.',
      stepsJson: '[]',
      timeLimitSec: 0,
      rewardCmd: 'give {player} minecraft:nether_star 1',
      startMsg: '§5§l⚔ {player} iniciou a peregrinação §r§7— que os deuses guiem seus passos',
      stepMsg: '§5§l⛧ Estação §6{step}§5/§6{total} §5§lalcançada',
      completeMsg: '§a§l⛧ PEREGRINAÇÃO CONCLUÍDA ⛧ §r§7{player} provou seu valor',
      stepSound: 'minecraft:block.amethyst_block.chime',
      completeSound: 'minecraft:ui.toast.challenge_complete',
      enabled: false,
    })
  }

  function parseSteps(p: PilgrimageDto): PilgrimageStep[] {
    try { const a = JSON.parse(p.stepsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⚔ Sacred Pilgrimage</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — costura Memorial / Glyph / Anchor / coords numa peregrinação ordenada.
            Auto-inicia quando o player chega na 1ª estação.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={async () => {
            if (!confirm(`Importar ${PILGRIMAGE_PRESETS.length} peregrinações de exemplo?`)) return
            for (const preset of PILGRIMAGE_PRESETS) {
              try { await save.mutateAsync(preset as PilgrimageDto) } catch {}
            }
            toast.ok(`📥 ${PILGRIMAGE_PRESETS.length} peregrinações importadas`)
          }}>📥 Importar {PILGRIMAGE_PRESETS.length} presets</button>
          <button className="btn" onClick={newPil}>+ Nova Peregrinação</button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {pilgrimages.length === 0 && (
            <div className="card text-center py-12 col-span-full">
              <div className="text-5xl mb-3 opacity-50">⚔</div>
              <p className="text-liberthia-300/70">Nenhuma peregrinação ainda.</p>
            </div>
          )}
          {pilgrimages.map((p) => {
            const steps = parseSteps(p)
            const playersOn = progress.filter((pr) => pr.pilgrimageId === p.id && !pr.completedAt)
            const completers = progress.filter((pr) => pr.pilgrimageId === p.id && pr.completedAt)
            return (
              <div key={p.id} className={`card-glow ${p.enabled ? '!border-purple-400/40' : ''}`}>
                <div className="flex items-start gap-3 mb-2">
                  <div className="text-4xl">{p.emoji}</div>
                  <div className="flex-1 min-w-0">
                    <div className="font-bold">{p.name}</div>
                    <div className="text-[10px] text-liberthia-300/50">
                      {steps.length} estações · {p.timeLimitSec > 0 ? `${p.timeLimitSec}s limite` : 'sem limite'}
                    </div>
                  </div>
                </div>
                <p className="text-xs text-liberthia-300/70 italic mb-2 line-clamp-2">{p.description}</p>

                {playersOn.length > 0 && (
                  <div className="bg-amber-500/10 border border-amber-400/30 rounded-lg p-2 mb-2">
                    <div className="text-[10px] text-amber-300 font-bold mb-1">⏳ Em andamento ({playersOn.length})</div>
                    {playersOn.map((pr) => (
                      <div key={pr.id} className="text-[10px] flex justify-between">
                        <span>{pr.playerName}</span>
                        <span>{pr.currentStep}/{steps.length}</span>
                      </div>
                    ))}
                  </div>
                )}

                {completers.length > 0 && (
                  <div className="text-[10px] text-emerald-300/70 mb-2">
                    ✓ {completers.length} concluíram
                  </div>
                )}

                <div className="grid grid-cols-3 gap-1">
                  <button className={p.enabled ? 'btn-success btn-sm' : 'btn-ghost btn-sm'} onClick={() => toggle.mutate(p.id!)}>
                    {p.enabled ? '⏸' : '▶'}
                  </button>
                  <button className="btn-ghost btn-sm" onClick={() => setEditing(p)}>✎</button>
                  <button className="btn-ghost btn-sm" onClick={() => del.mutate(p.id!)}>🗑</button>
                </div>
              </div>
            )
          })}
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">📜 Progresso por player</h3>
          <div className="space-y-1 max-h-[60vh] overflow-y-auto text-xs">
            {progress.length === 0 && <p className="italic text-liberthia-300/50">— ninguém peregrinando —</p>}
            {progress.map((pr) => {
              const pil = pilgrimages.find((x) => x.id === pr.pilgrimageId)
              if (!pil) return null
              const total = parseSteps(pil).length
              const pct = total > 0 ? (pr.currentStep / total) * 100 : 0
              return (
                <div key={pr.id} className="p-1.5 rounded bg-liberthia-900/40">
                  <div className="flex justify-between mb-1">
                    <span><b>{pr.playerName}</b> · {pil.emoji} {pil.name}</span>
                    <span className={pr.completedAt ? 'text-emerald-300' : 'text-amber-300'}>
                      {pr.completedAt ? '✓' : `${pr.currentStep}/${total}`}
                    </span>
                  </div>
                  <div className="h-1 bg-liberthia-900 rounded overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-purple-500 to-amber-400" style={{ width: `${pct}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {editing && (
        <PilEditor pil={editing} players={players}
          onSave={(p) => save.mutate(p, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function PilEditor({ pil, onSave, onCancel }: { pil: PilgrimageDto; players: any[]; onSave: (p: PilgrimageDto) => void; onCancel: () => void }) {
  const [p, setP] = useState<PilgrimageDto>(pil)
  const sounds = useSoundOptions()
  const glyphsQ = useQuery({ queryKey: ['glyphs'], queryFn: api.glyphsList })
  const anchorsQ = useQuery({ queryKey: ['anchors'], queryFn: api.anchorsList })
  const glyphs: GlyphDto[] = glyphsQ.data?.glyphs ?? []
  const anchors: SaveAnchorDto[] = anchorsQ.data?.anchors ?? []
  // Memorial lives in kv — read from KV
  const memorialsQ = useQuery({
    queryKey: ['kv', 'memorials'],
    queryFn: async () => (await api.kvGet<any[]>('memorials')).data ?? [],
  })
  const memorials = memorialsQ.data ?? []

  const steps: PilgrimageStep[] = (() => {
    try { const a = JSON.parse(p.stepsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  })()

  function setSteps(arr: PilgrimageStep[]) { setP({ ...p, stepsJson: JSON.stringify(arr) }) }
  function move(i: number, dir: -1 | 1) {
    const j = i + dir; if (j < 0 || j >= steps.length) return
    const a = [...steps]
    ;[a[i], a[j]] = [a[j], a[i]]
    setSteps(a)
  }
  function update(i: number, partial: any) {
    setSteps(steps.map((s, j) => j === i ? { ...s, ...partial } as PilgrimageStep : s))
  }

  function addStep(type: PilgrimageStep['type']) {
    let next: PilgrimageStep
    if (type === 'coords') next = { type: 'coords', x: 0, y: 80, z: 0, dim: 'overworld', radius: 5, label: '' }
    else next = { type, refId: '' } as any
    setSteps([...steps, next])
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⚔ Editar Peregrinação</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-2">
          <input className="input text-2xl text-center" value={p.emoji} onChange={(e) => setP({ ...p, emoji: e.target.value })} />
          <input className="input font-bold" value={p.name} onChange={(e) => setP({ ...p, name: e.target.value })} />
        </div>
        <textarea className="input mb-3 text-xs" rows={2} value={p.description} onChange={(e) => setP({ ...p, description: e.target.value })} />

        <div className="grid grid-cols-2 gap-2 mb-3">
          <div>
            <label className="label">Tempo limite (s, 0 = sem)</label>
            <input type="number" className="input" min={0} value={p.timeLimitSec} onChange={(e) => setP({ ...p, timeLimitSec: Number(e.target.value) })} />
          </div>
          <div>
            <label className="label">Som a cada estação</label>
            <Autocomplete value={p.stepSound} onChange={(v) => setP({ ...p, stepSound: v })} options={sounds} />
          </div>
        </div>

        <label className="label">Som ao completar</label>
        <div className="mb-3"><Autocomplete value={p.completeSound} onChange={(v) => setP({ ...p, completeSound: v })} options={sounds} /></div>

        <label className="label">Comando de recompensa (suporta {'{player}'})</label>
        <input className="input mb-3 text-xs font-mono" value={p.rewardCmd} onChange={(e) => setP({ ...p, rewardCmd: e.target.value })} placeholder="give {player} minecraft:nether_star 1" />

        <details className="mb-3">
          <summary className="cursor-pointer label">📢 Mensagens (start / step / complete)</summary>
          <div className="mt-2 space-y-2">
            <div><label className="label">Start (auto-inicio na 1ª estação)</label>
              <MinecraftFormatter value={p.startMsg} onChange={(v) => setP({ ...p, startMsg: v })} rows={1} maxChars={300} showCounter={false} /></div>
            <div><label className="label">Step ({'{step}'} / {'{total}'} disponíveis)</label>
              <MinecraftFormatter value={p.stepMsg} onChange={(v) => setP({ ...p, stepMsg: v })} rows={1} maxChars={300} showCounter={false} /></div>
            <div><label className="label">Complete</label>
              <MinecraftFormatter value={p.completeMsg} onChange={(v) => setP({ ...p, completeMsg: v })} rows={1} maxChars={300} showCounter={false} /></div>
          </div>
        </details>

        <h4 className="font-bold text-sm mb-1 mt-3">⛧ Estações ({steps.length})</h4>
        <div className="flex gap-1 mb-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => addStep('memorial')}>+ Memorial</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('glyph')}>+ Glyph</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('anchor')}>+ Anchor</button>
          <button className="btn-ghost btn-sm" onClick={() => addStep('coords')}>+ Coords</button>
        </div>

        <div className="space-y-1 mb-4">
          {steps.map((st, i) => (
            <div key={i} className="bg-liberthia-900/40 rounded p-2">
              <div className="flex items-center gap-2 mb-1">
                <span className="badge badge-purple shrink-0">#{i + 1}</span>
                <span className="text-xs font-mono font-bold text-purple-300 shrink-0">{st.type}</span>
                <div className="flex-1" />
                <button className="btn-ghost btn-sm" onClick={() => move(i, -1)}>↑</button>
                <button className="btn-ghost btn-sm" onClick={() => move(i, 1)}>↓</button>
                <button className="btn-ghost btn-sm" onClick={() => setSteps(steps.filter((_, j) => j !== i))}>🗑</button>
              </div>
              {st.type === 'memorial' && (
                <select className="input text-xs" value={st.refId} onChange={(e) => update(i, { refId: e.target.value })}>
                  <option value="">— selecione memorial —</option>
                  {memorials.map((m: any) => <option key={m.id} value={m.id}>{m.emoji} {m.title} ({m.x},{m.y},{m.z})</option>)}
                </select>
              )}
              {st.type === 'glyph' && (
                <select className="input text-xs" value={st.refId} onChange={(e) => update(i, { refId: e.target.value })}>
                  <option value="">— selecione glyph —</option>
                  {glyphs.map((g) => <option key={g.id} value={g.id}>{g.symbol} {renderMcText(g.name)} ({g.posX},{g.posY},{g.posZ})</option>)}
                </select>
              )}
              {st.type === 'anchor' && (
                <select className="input text-xs" value={st.refId} onChange={(e) => update(i, { refId: e.target.value })}>
                  <option value="">— selecione anchor —</option>
                  {anchors.map((a) => <option key={a.id} value={a.id}>{a.emoji} {a.name} ({a.posX},{a.posY},{a.posZ})</option>)}
                </select>
              )}
              {st.type === 'coords' && (
                <div className="space-y-1">
                  <input className="input text-xs" placeholder="rótulo (opcional)" value={st.label ?? ''} onChange={(e) => update(i, { label: e.target.value })} />
                  <div className="grid grid-cols-5 gap-1">
                    <input type="number" className="input text-xs" placeholder="x" value={st.x} onChange={(e) => update(i, { x: Number(e.target.value) })} />
                    <input type="number" className="input text-xs" placeholder="y" value={st.y} onChange={(e) => update(i, { y: Number(e.target.value) })} />
                    <input type="number" className="input text-xs" placeholder="z" value={st.z} onChange={(e) => update(i, { z: Number(e.target.value) })} />
                    <select className="input text-xs" value={st.dim} onChange={(e) => update(i, { dim: e.target.value })}>
                      <option value="overworld">overworld</option><option value="the_nether">nether</option><option value="the_end">end</option>
                    </select>
                    <input type="number" className="input text-xs" placeholder="raio" value={st.radius ?? 5} onChange={(e) => update(i, { radius: Number(e.target.value) })} />
                  </div>
                  <PlayerPosPicker label="📍 pegar do player" onPick={(pp) => update(i, { x: Math.floor(pp.x), y: Math.floor(pp.y), z: Math.floor(pp.z), dim: pp.dim })} />
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(p)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
