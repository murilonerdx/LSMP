import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, PehkuiPlayerInfo, PehkuiPresetDto } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Pehkui — modificador de escalas dos players.
 *
 * Workflow:
 *   1. Sidebar mostra TODOS os players online, ordenados por DM (matéria escura) desc.
 *   2. Click num player → vira "alvo". Tabs no centro pra ajustar escalas categoria por categoria.
 *   3. Toggle "live preview" envia comandos /scale set instantâneo ao mover slider.
 *      Senão acumula e dispara em batch quando clicar "Aplicar".
 *   4. Presets na lateral direita — botão único pra aplicar combinação famosa.
 *
 * Toda escala vai pro mod via /api/pehkui/scale (que chama /scale set <type> <amt> <player>).
 * Reset rápido: botão "Zerar tudo" → /api/pehkui/reset all.
 */

type CategoryKey = 'size' | 'combat' | 'movement' | 'physics' | 'visual' | 'items' | 'resources'

type ScaleSpec = {
  type: string
  label: string
  min: number
  max: number
  step: number
  default: number
  hint?: string
}

const CATEGORIES: Record<CategoryKey, { label: string; emoji: string; scales: ScaleSpec[] }> = {
  size: {
    label: 'Tamanho', emoji: '📏',
    scales: [
      { type: 'pehkui:base', label: 'Base (geral)', min: 0.1, max: 10, step: 0.1, default: 1, hint: 'Multiplica TUDO uniformemente' },
      { type: 'pehkui:width', label: 'Largura', min: 0.1, max: 10, step: 0.1, default: 1, hint: 'Achata ou alarga' },
      { type: 'pehkui:height', label: 'Altura', min: 0.1, max: 10, step: 0.1, default: 1, hint: 'Estica pra cima' },
      { type: 'pehkui:eye_height', label: 'Altura dos olhos', min: 0.1, max: 5, step: 0.1, default: 1 },
      { type: 'pehkui:hitbox_width', label: 'Hitbox largura', min: 0.1, max: 5, step: 0.1, default: 1 },
      { type: 'pehkui:hitbox_height', label: 'Hitbox altura', min: 0.1, max: 5, step: 0.1, default: 1 },
    ],
  },
  combat: {
    label: 'Combate', emoji: '⚔',
    scales: [
      { type: 'pehkui:attack', label: 'Dano', min: 0, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:reach', label: 'Alcance', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:defense', label: 'Defesa', min: 0, max: 5, step: 0.1, default: 1 },
      { type: 'pehkui:knockback', label: 'Knockback', min: 0, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:projectile_attack', label: 'Dano de projétil', min: 0, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:projectile_knockback', label: 'KB de projétil', min: 0, max: 10, step: 0.1, default: 1 },
    ],
  },
  movement: {
    label: 'Movimento', emoji: '🏃',
    scales: [
      { type: 'pehkui:motion', label: 'Velocidade', min: 0, max: 5, step: 0.1, default: 1 },
      { type: 'pehkui:jump', label: 'Pulo', min: 0, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:step_height', label: 'Altura do passo', min: 0, max: 5, step: 0.1, default: 1, hint: 'Sobe blocos sem pular' },
      { type: 'pehkui:flight', label: 'Voo (creative)', min: 0, max: 5, step: 0.1, default: 1 },
      { type: 'pehkui:swim_speed', label: 'Velocidade nadando', min: 0, max: 5, step: 0.1, default: 1 },
    ],
  },
  physics: {
    label: 'Física', emoji: '🌍',
    scales: [
      { type: 'pehkui:gravity', label: 'Gravidade', min: 0, max: 5, step: 0.1, default: 1, hint: '0 = flutua, 2 = pesado' },
      { type: 'pehkui:fall_damage', label: 'Dano de queda', min: 0, max: 5, step: 0.1, default: 1 },
    ],
  },
  visual: {
    label: 'Visual', emoji: '👁',
    scales: [
      { type: 'pehkui:visibility', label: 'Visibilidade', min: 0, max: 5, step: 0.1, default: 1, hint: '0 = invisível' },
      { type: 'pehkui:hostile_visibility', label: 'Visível pra mobs hostis', min: 0, max: 5, step: 0.1, default: 1 },
      { type: 'pehkui:third_person', label: 'Distância 3ª pessoa', min: 0, max: 10, step: 0.1, default: 1 },
    ],
  },
  items: {
    label: 'Itens / Projéteis', emoji: '🗡',
    scales: [
      { type: 'pehkui:held_item', label: 'Item na mão (geral)', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:held_item_x', label: 'Item — eixo X', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:held_item_y', label: 'Item — eixo Y', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:held_item_z', label: 'Item — eixo Z', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:projectile', label: 'Projétil (geral)', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:projectile_x', label: 'Projétil — X', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:projectile_y', label: 'Projétil — Y', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:projectile_z', label: 'Projétil — Z', min: 0.1, max: 10, step: 0.1, default: 1 },
    ],
  },
  resources: {
    label: 'Recursos', emoji: '💎',
    scales: [
      { type: 'pehkui:health', label: 'Vida máxima', min: 0.1, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:drops', label: 'Drops', min: 0, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:experience_dropped', label: 'XP dropado', min: 0, max: 10, step: 0.1, default: 1 },
      { type: 'pehkui:held_item_use_duration', label: 'Tempo de uso do item', min: 0.1, max: 5, step: 0.1, default: 1 },
    ],
  },
}

/** Presets prontos — combinações engraçadas/criativas pra aplicar com 1 click. */
const BUILTIN_PRESETS: Array<{ name: string; emoji: string; description: string; scales: Record<string, number> }> = [
  { name: 'Reset', emoji: '🧍', description: 'Tudo no padrão', scales: {} /* especial: usa /reset */ },
  { name: 'Formiguinha', emoji: '🐜', description: 'Mini-mini', scales: { 'pehkui:base': 0.2, 'pehkui:motion': 0.5 } },
  { name: 'Bebê', emoji: '👶', description: 'Pequenininho', scales: { 'pehkui:base': 0.5, 'pehkui:jump': 1.5 } },
  { name: 'Gigante', emoji: '🗿', description: 'Pé-grande', scales: { 'pehkui:base': 2.5, 'pehkui:attack': 2.0, 'pehkui:reach': 1.8, 'pehkui:health': 2 } },
  { name: 'Titã', emoji: '👹', description: 'Modo Attack on Titan', scales: { 'pehkui:base': 5, 'pehkui:attack': 5, 'pehkui:reach': 3, 'pehkui:health': 5, 'pehkui:motion': 1.5 } },
  { name: 'Pizza', emoji: '🍕', description: 'Achatado horizontal', scales: { 'pehkui:width': 3, 'pehkui:height': 0.3 } },
  { name: 'Baguete', emoji: '🥖', description: 'Esticadão pra cima', scales: { 'pehkui:height': 4, 'pehkui:width': 0.4 } },
  { name: 'Balão', emoji: '🎈', description: 'Bola gorda', scales: { 'pehkui:width': 2, 'pehkui:height': 2, 'pehkui:gravity': 0.3, 'pehkui:motion': 0.5 } },
  { name: 'Furtivo', emoji: '🥷', description: 'Mini + invisível', scales: { 'pehkui:base': 0.4, 'pehkui:visibility': 0.1, 'pehkui:motion': 1.3 } },
  { name: 'Sniper', emoji: '🎯', description: 'Reach gigante', scales: { 'pehkui:reach': 8, 'pehkui:projectile_attack': 3 } },
  { name: 'Flash', emoji: '⚡', description: 'Velocíssimo', scales: { 'pehkui:motion': 3, 'pehkui:jump': 2.5, 'pehkui:gravity': 0.7 } },
  { name: 'Lua', emoji: '🌙', description: 'Gravidade lunar', scales: { 'pehkui:gravity': 0.2, 'pehkui:jump': 3, 'pehkui:fall_damage': 0.2 } },
  { name: 'Boss', emoji: '👑', description: 'Forte e durão', scales: { 'pehkui:base': 1.5, 'pehkui:attack': 3, 'pehkui:defense': 2, 'pehkui:knockback': 2.5, 'pehkui:health': 3 } },
  { name: 'Cabeção', emoji: '🤡', description: 'Hitbox amplificado', scales: { 'pehkui:hitbox_width': 3, 'pehkui:hitbox_height': 1.5 } },
  { name: 'Fantasma', emoji: '👻', description: 'Quase invisível, voa', scales: { 'pehkui:visibility': 0.2, 'pehkui:hostile_visibility': 0, 'pehkui:gravity': 0.4, 'pehkui:knockback': 0 } },
  { name: 'Stretch X', emoji: '🤺', description: 'Largo horizontal', scales: { 'pehkui:width': 4, 'pehkui:reach': 1.5 } },
  { name: 'Olho gigante', emoji: '👁', description: 'POV elevado', scales: { 'pehkui:eye_height': 5 } },
]

function fmtScale(v: number): string {
  return v.toFixed(2) + 'x'
}

export function PehkuiPage() {
  const qc = useQueryClient()
  const [selectedUuid, setSelectedUuid] = useState<string>('')
  const [selectedTab, setSelectedTab] = useState<CategoryKey>('size')
  const [livePreview, setLivePreview] = useState(true)
  const [persistent, setPersistent] = useState(true)
  const [search, setSearch] = useState('')
  /** valores que mudaram mas ainda não foram enviados pro backend. */
  const [pending, setPending] = useState<Record<string, number>>({})
  /**
   * valores que JÁ foram aplicados (last known). Slider lê daqui se não tiver
   * pending — assim NÃO volta pro default após apply. Bug histórico: o
   * onSuccess do scaleMut removia do pending, o `current` caía pro
   * s.default (1.0), e o slider voltava — usuário pensava que não aplicou.
   */
  const [appliedValues, setAppliedValues] = useState<Record<string, number>>({})
  const [showCustomPreset, setShowCustomPreset] = useState(false)

  const eligibleQ = useQuery({
    queryKey: ['pehkui-eligible'],
    queryFn: () => api.pehkuiEligible(0),
    refetchInterval: 5000,
  })
  const presetsQ = useQuery({
    queryKey: ['pehkui-presets'],
    queryFn: api.pehkuiPresets,
  })

  const players = useMemo(() =>
    Array.isArray(eligibleQ.data?.players) ? eligibleQ.data!.players : [],
    [eligibleQ.data])
  const selected = players.find(p => p.uuid === selectedUuid) ?? null

  // Quando players mudam e não tem seleção, seleciona o de mais DM
  useEffect(() => {
    if (!selectedUuid && players.length > 0) setSelectedUuid(players[0].uuid)
  }, [players, selectedUuid])

  // Quando troca de player, limpa pending/applied (cada player tem suas escalas)
  useEffect(() => {
    setPending({})
    setAppliedValues({})
  }, [selectedUuid])

  const filteredPlayers = useMemo(() => {
    if (!search.trim()) return players
    const q = search.trim().toLowerCase()
    return players.filter(p => p.name.toLowerCase().includes(q))
  }, [players, search])

  const scaleMut = useMutation({
    mutationFn: api.pehkuiScale,
    onError: (e: any) => toast.err(e.message),
  })
  const bulkMut = useMutation({
    mutationFn: api.pehkuiBulk,
    onSuccess: (r) => { toast.ok(`✓ aplicado ${r.count} escala${(r.count ?? 0) === 1 ? '' : 's'}`); setPending({}) },
    onError: (e: any) => toast.err(e.message),
  })
  const resetMut = useMutation({
    mutationFn: api.pehkuiReset,
    onSuccess: () => { toast.ok('↺ reset'); setPending({}); setAppliedValues({}) },
    onError: (e: any) => toast.err(e.message),
  })
  const presetApplyMut = useMutation({
    mutationFn: ({ id, playerName }: { id: number; playerName: string }) =>
      api.pehkuiPresetApply(id, playerName),
    onSuccess: (r) => {
      if (r.ok) toast.ok(`✓ preset aplicado (${r.applied} escalas${r.duration ? `, ${r.duration}s` : ''})`)
      else toast.err(r.error ?? 'falhou')
    },
    onError: (e: any) => toast.err(e.message),
  })

  function applyOne(type: string, value: number) {
    if (!selected) return
    scaleMut.mutate(
      { playerName: selected.name, scaleType: type, scale: value, persistent },
      { onSuccess: (r: any) => {
        // Move o valor de pending pra applied — slider continua mostrando o valor.
        setAppliedValues(a => ({ ...a, [type]: value }))
        setPending(p => { const n = { ...p }; delete n[type]; return n })
        if (r?.cmd) console.log('[Pehkui] cmd:', r.cmd, '| modResult:', r.modResult)
      }})
  }
  function flushPending() {
    if (!selected || Object.keys(pending).length === 0) return
    const toApply = { ...pending }
    bulkMut.mutate({ playerName: selected.name, scales: toApply, persistent },
      { onSuccess: () => {
        // Move todas as escalas aplicadas pro state applied
        setAppliedValues(a => ({ ...a, ...toApply }))
      }})
  }
  /** Aplica o valor atual de UM slider imediatamente (botão "Aplicar agora" embaixo do slider). */
  function applyNow(type: string, value: number) {
    applyOne(type, value)
  }
  function resetAll() {
    if (!selected) return
    if (!confirm(`Resetar TODAS as escalas de ${selected.name}?`)) return
    resetMut.mutate({ playerName: selected.name, scaleType: 'all' })
  }
  function applyBuiltin(preset: typeof BUILTIN_PRESETS[number]) {
    if (!selected) return
    if (preset.name === 'Reset') { resetAll(); return }
    bulkMut.mutate({ playerName: selected.name, scales: preset.scales, persistent: true })
  }

  return (
    <div className="route-fade max-w-[1700px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📏 Pehkui — Controle de Escalas</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Modifica o tamanho/atributos físicos de qualquer player online. Players com mais DM
            (matéria escura) aparecem no topo — eles são mais "moldáveis" pelo void.
          </p>
        </div>
        <div className="flex gap-2 items-center text-xs">
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={livePreview} onChange={(e) => setLivePreview(e.target.checked)} />
            <span>Live preview (dispara ao soltar slider)</span>
          </label>
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={persistent} onChange={(e) => setPersistent(e.target.checked)} />
            <span>Persistente (sobrevive logoff)</span>
          </label>
        </div>
      </header>

      <div className="grid grid-cols-[260px_1fr_320px] gap-3">

        {/* ============ SIDEBAR: Players online com DM ============ */}
        <aside className="card-glow p-2 space-y-2 max-h-[80vh] overflow-y-auto">
          <input className="input text-xs" placeholder="Buscar player…"
            value={search} onChange={(e) => setSearch(e.target.value)} />
          <div className="text-[10px] text-liberthia-300/50 uppercase tracking-widest mt-2 mb-1">
            {filteredPlayers.length} online {eligibleQ.isLoading && '· carregando…'}
          </div>
          {filteredPlayers.map((p) => (
            <button key={p.uuid}
              onClick={() => setSelectedUuid(p.uuid)}
              className={`w-full text-left rounded p-2 transition flex items-center gap-2 ${
                selectedUuid === p.uuid
                  ? 'bg-purple-500/30 ring-2 ring-purple-400/60'
                  : 'bg-liberthia-900/40 hover:bg-liberthia-900/70'
              }`}>
              <img src={`https://mc-heads.net/avatar/${encodeURIComponent(p.name)}/32`}
                className="rounded shrink-0"
                onError={(ev) => { (ev.target as HTMLImageElement).style.display = 'none' }} />
              <div className="flex-1 min-w-0">
                <div className="text-sm font-bold truncate">{p.name}</div>
                <div className="text-[10px] text-liberthia-300/60 flex gap-1.5">
                  <span className="text-purple-300" title="Matéria Escura">DM {p.dm.toFixed(1)}</span>
                  <span className="text-cyan-300" title="Matéria de Wisdom">WM {p.wm.toFixed(1)}</span>
                  <span className="text-yellow-300" title="Matéria Amarela">YM {p.ym.toFixed(1)}</span>
                </div>
              </div>
            </button>
          ))}
          {!eligibleQ.isLoading && filteredPlayers.length === 0 && (
            <div className="text-center text-xs text-liberthia-300/60 py-4">
              Nenhum player online.
            </div>
          )}
        </aside>

        {/* ============ CENTRO: Sliders por categoria ============ */}
        <main className="space-y-3">
          {!selected ? (
            <div className="card text-center py-16">
              <div className="text-5xl mb-3 opacity-50">📏</div>
              <p className="text-liberthia-300/70">Seleciona um player na sidebar pra começar.</p>
            </div>
          ) : (
            <>
              {/* Header do alvo */}
              <div className="card-glow flex items-center gap-3">
                <img src={`https://mc-heads.net/avatar/${encodeURIComponent(selected.name)}/64`}
                  className="rounded" />
                <div className="flex-1">
                  <div className="text-2xl font-bold">{selected.name}</div>
                  <div className="text-xs text-liberthia-300/70 mt-1 flex gap-2 flex-wrap">
                    <span className="badge badge-purple">DM {selected.dm.toFixed(2)}</span>
                    <span className="badge badge-purple">WM {selected.wm.toFixed(2)}</span>
                    <span className="badge badge-purple">YM {selected.ym.toFixed(2)}</span>
                    <span className="badge">❤ {selected.health.toFixed(1)}</span>
                    <span className="badge">{selected.dimension}</span>
                    <span className="badge font-mono">{Math.round(selected.posX)},{Math.round(selected.posY)},{Math.round(selected.posZ)}</span>
                  </div>
                </div>
                <div className="flex flex-col gap-1">
                  {/* Botão sempre visível quando tem mudanças pendentes — dispara TODAS de uma vez */}
                  {Object.keys(pending).length > 0 && (
                    <button className="btn pulse-glow" onClick={flushPending}>
                      🚀 Aplicar {Object.keys(pending).length} mudança{Object.keys(pending).length === 1 ? '' : 's'} agora
                    </button>
                  )}
                  <button className="btn-danger btn-sm" onClick={resetAll}>↺ Zerar tudo</button>
                </div>
              </div>

              {/* Tabs */}
              <div className="flex gap-1 overflow-x-auto pb-1">
                {(Object.entries(CATEGORIES) as [CategoryKey, typeof CATEGORIES.size][]).map(([k, c]) => (
                  <button key={k} onClick={() => setSelectedTab(k)}
                    className={`px-3 py-1.5 rounded text-xs whitespace-nowrap ${
                      selectedTab === k ? 'bg-purple-500/40 ring-1 ring-purple-400' : 'bg-liberthia-900/50'
                    }`}>
                    {c.emoji} {c.label}
                  </button>
                ))}
              </div>

              {/* Sliders */}
              <div className="card space-y-3">
                {CATEGORIES[selectedTab].scales.map((s) => {
                  const pendingVal = pending[s.type]
                  const appliedVal = appliedValues[s.type]
                  // Prioridade: pending (não aplicado ainda) > applied (último aplicado) > default
                  const current = pendingVal ?? appliedVal ?? s.default
                  const isDirty = pendingVal !== undefined
                  const wasApplied = appliedVal !== undefined && pendingVal === undefined
                  return (
                    <div key={s.type} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <label className="font-semibold">
                          {s.label}
                          {isDirty && <span className="text-amber-300 ml-1" title="modificado, não aplicado">●</span>}
                          {wasApplied && <span className="text-emerald-300 ml-1" title="aplicado in-game">✓</span>}
                        </label>
                        <div className="flex items-center gap-2">
                          <span className={`font-mono ${
                            isDirty ? 'text-amber-300' :
                            wasApplied ? 'text-emerald-300' : 'text-liberthia-300/60'
                          }`}>
                            {fmtScale(current)}
                          </span>
                          <button className="btn-ghost btn-sm text-[10px] py-0.5 px-1.5"
                            onClick={() => {
                              setPending(p => { const n = { ...p }; delete n[s.type]; return n })
                              setAppliedValues(a => { const n = { ...a }; delete n[s.type]; return n })
                              if (selected) resetMut.mutate({ playerName: selected.name, scaleType: s.type })
                            }}
                            title="Resetar essa escala">↺</button>
                        </div>
                      </div>
                      {s.hint && (
                        <div className="text-[10px] text-liberthia-300/40 italic">{s.hint}</div>
                      )}
                      <div className="flex items-center gap-2">
                        <input type="range"
                          min={s.min} max={s.max} step={s.step}
                          value={current}
                          onChange={(e) => {
                            const v = parseFloat(e.target.value)
                            setPending(p => ({ ...p, [s.type]: v }))
                          }}
                          onMouseUp={(e) => {
                            const v = parseFloat((e.target as HTMLInputElement).value)
                            if (livePreview) applyOne(s.type, v)
                          }}
                          onTouchEnd={(e) => {
                            const v = parseFloat((e.target as HTMLInputElement).value)
                            if (livePreview) applyOne(s.type, v)
                          }}
                          className="flex-1" />
                        {/* Botão pra aplicar SÓ essa escala — útil quando livePreview off */}
                        <button className={`btn-sm text-[10px] ${isDirty ? 'btn' : 'btn-ghost opacity-60'}`}
                          disabled={!isDirty}
                          onClick={() => applyNow(s.type, current)}
                          title="Aplicar essa escala agora">
                          🚀 Aplicar
                        </button>
                      </div>
                      <div className="flex justify-between text-[9px] text-liberthia-300/40 font-mono">
                        <span>{s.min}x</span>
                        <span>1x</span>
                        <span>{s.max}x</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            </>
          )}
        </main>

        {/* ============ DIREITA: Presets ============ */}
        <aside className="space-y-2 max-h-[80vh] overflow-y-auto">
          <div className="card-glow p-3">
            <div className="text-xs uppercase tracking-widest text-liberthia-300/60 mb-2">
              🎭 Presets prontos
            </div>
            <div className="grid grid-cols-2 gap-1.5">
              {BUILTIN_PRESETS.map((p) => (
                <button key={p.name}
                  disabled={!selected}
                  onClick={() => applyBuiltin(p)}
                  className="text-left rounded bg-liberthia-900/60 hover:bg-purple-500/30 disabled:opacity-40 p-2 transition"
                  title={p.description}>
                  <div className="text-lg leading-none">{p.emoji}</div>
                  <div className="text-[11px] font-bold mt-1">{p.name}</div>
                  <div className="text-[9px] text-liberthia-300/50 leading-tight">{p.description}</div>
                </button>
              ))}
            </div>
          </div>

          <div className="card p-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs uppercase tracking-widest text-liberthia-300/60">💾 Custom</span>
              <button className="btn-ghost btn-sm" onClick={() => setShowCustomPreset(s => !s)}>
                {showCustomPreset ? '×' : '+'}
              </button>
            </div>
            {showCustomPreset && selected && (
              <CustomPresetCreator
                playerName={selected.name}
                onSave={async (p) => {
                  await api.pehkuiPresetCreate(p)
                  toast.ok('✓ preset salvo')
                  qc.invalidateQueries({ queryKey: ['pehkui-presets'] })
                  setShowCustomPreset(false)
                }} />
            )}
            <div className="space-y-1 mt-2">
              {(presetsQ.data ?? []).map((p) => (
                <div key={p.id} className="flex items-center gap-1 rounded bg-liberthia-900/60 p-1.5">
                  <button className="flex-1 text-left text-xs flex items-center gap-1.5 truncate"
                    disabled={!selected}
                    onClick={() => selected && p.id && presetApplyMut.mutate({ id: p.id, playerName: selected.name })}
                    title={p.description}>
                    <span className="text-base">{p.emoji}</span>
                    <span className="font-bold truncate">{p.name}</span>
                    {p.durationSec > 0 && <span className="text-[9px] text-amber-300">{p.durationSec}s</span>}
                  </button>
                  <button className="btn-ghost btn-sm text-[10px] py-0 px-1"
                    onClick={async () => {
                      if (!p.id || !confirm(`Deletar preset "${p.name}"?`)) return
                      await api.pehkuiPresetDelete(p.id)
                      qc.invalidateQueries({ queryKey: ['pehkui-presets'] })
                    }}>🗑</button>
                </div>
              ))}
              {(presetsQ.data ?? []).length === 0 && (
                <div className="text-[11px] text-liberthia-300/50 italic text-center py-2">
                  Sem presets custom ainda
                </div>
              )}
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}

// ============================================================================
function CustomPresetCreator({ playerName, onSave }: {
  playerName: string
  onSave: (p: Partial<PehkuiPresetDto>) => void
}) {
  const [name, setName] = useState('')
  const [emoji, setEmoji] = useState('🧍')
  const [description, setDescription] = useState('')
  const [duration, setDuration] = useState(0)
  // Coleta as escalas pelo input simples (textarea JSON)
  const [scalesText, setScalesText] = useState(JSON.stringify({
    'pehkui:base': 1.5,
    'pehkui:motion': 1.2,
  }, null, 2))

  function tryParse(): Record<string, number> | null {
    try { return JSON.parse(scalesText) } catch { return null }
  }
  const parsed = tryParse()

  return (
    <div className="space-y-2">
      <input className="input text-xs" placeholder="Nome do preset (ex: Modo Hulk)"
        value={name} onChange={(e) => setName(e.target.value)} />
      <div className="flex gap-1">
        <input className="input text-xs w-16" placeholder="🧍"
          value={emoji} onChange={(e) => setEmoji(e.target.value)} maxLength={4} />
        <input className="input text-xs flex-1" placeholder="Descrição curta"
          value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <div>
        <label className="text-[10px] text-liberthia-300/60">Escalas (JSON — type → multiplicador)</label>
        <textarea className="input text-xs font-mono" rows={5}
          value={scalesText} onChange={(e) => setScalesText(e.target.value)} />
        {!parsed && <div className="text-[10px] text-red-400 mt-1">JSON inválido</div>}
      </div>
      <div className="flex items-center gap-2">
        <label className="text-[10px] text-liberthia-300/60">Duração (s, 0=permanente)</label>
        <input type="number" min={0} className="input text-xs w-24"
          value={duration} onChange={(e) => setDuration(Number(e.target.value) || 0)} />
      </div>
      <button className="btn w-full text-xs"
        disabled={!name || !parsed}
        onClick={() => onSave({
          name, emoji, description, scalesJson: scalesText, durationSec: duration,
          dmCost: 0, createdBy: playerName,
        })}>
        💾 Salvar preset
      </button>
    </div>
  )
}
