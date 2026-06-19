import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { Atmosphere, runAtmosphere, RunContext, stepLabel } from '../lib/atmosphere'
import { toast } from '../store/toast'
import { AtmosphereEditor } from './AtmosphereEditor'
import { useKvState } from '../lib/kvState'

/**
 * Grid completo + builder pra páginas de atmosfera (Horror/Divine/Magical/etc).
 * Reusa pra qualquer categoria; só passa presets e localStorage key.
 */

type Props = {
  category: string
  title: string
  subtitle: string
  presets: Atmosphere[]
  storageKey: string
  defaultEmoji: string
  pageEmoji: string
  pageDescription: string
}

export function AtmosphereGrid({ category, title, presets, storageKey, defaultEmoji, pageEmoji, pageDescription }: Props) {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState<'all' | string>('all')
  const [running, setRunning] = useState<string | null>(null)
  const abortRef = useRef({ cancelled: false })
  // Migrate da key antiga (localStorage) pra key nova (KV no banco)
  const kvKey = storageKey.replace(/^liberthia\./, '').replace(/\./g, '_')  // ex: "atmospheres_horror"
  const [customs, setCustoms] = useKvState<Atmosphere[]>(kvKey, [])
  const [editing, setEditing] = useState<Atmosphere | null>(null)

  async function fire(a: Atmosphere) {
    if (running) { toast.err('Já tá rodando — para a outra primeiro'); return }
    abortRef.current = { cancelled: false }
    setRunning(a.id)
    toast.info(`${a.emoji} ${a.name}`)
    const ctx: RunContext = {
      target,
      playerUuids: target === 'all' ? players.map((p) => p.uuid) : [target],
      playerSelector: target === 'all' ? '@a' : (players.find((p) => p.uuid === target)?.name ?? '@p'),
      origin: category,
      abort: abortRef.current,
    }
    try {
      await runAtmosphere(a, ctx)
      if (!abortRef.current.cancelled) toast.ok(`✓ ${a.name} encerrado`)
    } catch (e: any) { toast.err(e.message) }
    setRunning(null)
  }

  function stopRunning() {
    abortRef.current.cancelled = true
    toast.info('⏹ Parando...')
  }

  /**
   * Panic Stop — para a atmosfera E limpa efeitos negativos dos players.
   * Útil quando levitation/blindness/nausea/wither/darkness fica preso.
   */
  async function panicStop() {
    abortRef.current.cancelled = true
    const sel = target === 'all' ? '@a' : (players.find((p) => p.uuid === target)?.name ?? '@a')
    try {
      await api.command(`effect clear ${sel}`, category)
      await api.command(`effect give ${sel} minecraft:slow_falling 100 0 true`, category)
      await api.command(`title ${sel} clear`, category)
      await api.command(`weather clear 12000`, category)
      toast.ok('🚨 PANIC STOP: efeitos limpos, slow_falling aplicado')
    } catch (e: any) { toast.err(e.message) }
  }

  function newCustom() {
    setEditing({
      id: `custom_${category}_${Date.now().toString(36)}`,
      name: `Minha atmosfera ${customs.length + 1}`,
      emoji: defaultEmoji,
      description: 'Custom — edite os passos abaixo',
      category,
      steps: [],
      custom: true,
      loop: false,
      updated: Date.now(),
    })
  }

  function duplicate(a: Atmosphere) {
    setEditing({
      ...a,
      id: `custom_${category}_${Date.now().toString(36)}`,
      name: a.name + ' (cópia)',
      custom: true,
    })
  }

  function saveCustom(a: Atmosphere) {
    setCustoms((cur) => {
      const i = cur.findIndex((x) => x.id === a.id)
      if (i >= 0) { const n = [...cur]; n[i] = { ...a, updated: Date.now() }; return n }
      return [...cur, { ...a, updated: Date.now() }]
    })
    setEditing(null)
    toast.ok(`Atmosfera "${a.name}" salva`)
  }

  function exportJson(a: Atmosphere) {
    const blob = new Blob([JSON.stringify(a, null, 2)], { type: 'application/json' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `${a.id}.atm.json`
    link.click()
  }

  async function importJson(file: File) {
    try {
      const txt = await file.text()
      const obj = JSON.parse(txt) as Atmosphere
      if (!obj.steps || !obj.name) throw new Error('JSON inválido')
      obj.id = `custom_${category}_${Date.now().toString(36)}`
      obj.custom = true
      setCustoms((cur) => [...cur, obj])
      toast.ok(`Importado: ${obj.name}`)
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title flex items-center gap-2">{pageEmoji} {title}</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">{pageDescription}</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <select className="input text-sm max-w-xs" value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="all">🌐 Todos players</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>👤 {p.name}</option>)}
          </select>
          {running && <button className="btn-danger btn-sm" onClick={stopRunning}>⏹ Stop</button>}
          <button className="btn-amber btn-sm" onClick={panicStop} title="Para tudo + limpa efeitos negativos">🚨 PANIC</button>
          <label className="btn-ghost btn-sm cursor-pointer">
            📥 Importar
            <input type="file" accept=".json" className="hidden"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) importJson(f); (e.target as HTMLInputElement).value = '' }} />
          </label>
          <button className="btn" onClick={newCustom}>+ Criar Atmosfera</button>
        </div>
      </header>

      {/* Banner sempre visível quando algo tá rodando — Stop bem destacado */}
      {running && (
        <div className="card mb-4 !border-emerald-400/60 !bg-emerald-500/10 flex items-center gap-3 flex-wrap">
          <span className="live-dot" />
          <span className="text-sm font-bold flex-1 min-w-0">⚙ Atmosfera rodando...</span>
          <button className="btn-danger btn-sm" onClick={stopRunning}>⏹ Stop</button>
          <button className="btn-amber btn-sm" onClick={panicStop}>🚨 PANIC STOP (limpa efeitos)</button>
        </div>
      )}

      {customs.length > 0 && (
        <>
          <h2 className="text-lg font-bold gradient-text mb-3 flex items-center gap-2">
            ✨ Suas Atmosferas <span className="badge badge-purple">{customs.length}</span>
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 mb-8">
            {customs.map((a) => (
              <AtmosphereCard key={a.id} a={a} running={running === a.id} disabled={!!running}
                onFire={() => fire(a)}
                onEdit={() => setEditing(a)}
                onDuplicate={() => duplicate(a)}
                onExport={() => exportJson(a)}
                onDelete={() => setCustoms((cur) => cur.filter((x) => x.id !== a.id))}
              />
            ))}
          </div>
        </>
      )}

      <h2 className="text-lg font-bold gradient-text mb-3 flex items-center gap-2">
        📚 Presets <span className="badge badge-purple">{presets.length}</span>
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
        {presets.map((a) => (
          <AtmosphereCard key={a.id} a={a} running={running === a.id} disabled={!!running}
            onFire={() => fire(a)}
            onDuplicate={() => duplicate(a)}
          />
        ))}
      </div>

      {editing && (
        <AtmosphereEditor
          atm={editing}
          category={category}
          onSave={saveCustom}
          onCancel={() => setEditing(null)}
        />
      )}
    </div>
  )
}

function AtmosphereCard({ a, running, disabled, onFire, onEdit, onDuplicate, onExport, onDelete }: {
  a: Atmosphere; running: boolean; disabled: boolean
  onFire: () => void
  onEdit?: () => void
  onDuplicate?: () => void
  onExport?: () => void
  onDelete?: () => void
}) {
  return (
    <div className={`card-glow transition ${running ? '!border-emerald-400/60 !bg-emerald-500/10' : ''}`}>
      <div className="text-5xl text-center mb-2">{a.emoji}</div>
      <div className="font-bold text-center mb-1">{a.name}</div>
      <div className="text-xs text-liberthia-300/70 text-center mb-3 min-h-[36px]">{a.description}</div>
      <div className="flex flex-wrap gap-1 mb-3 justify-center max-h-16 overflow-hidden">
        {a.steps.slice(0, 6).map((s, i) => (
          <span key={i} className="chip text-[10px]">{stepLabel(s)}</span>
        ))}
        {a.steps.length > 6 && <span className="chip text-[10px]">+{a.steps.length - 6}</span>}
      </div>
      {a.loop && <div className="text-center text-[10px] text-amber-300 mb-2">🔁 loop infinito</div>}
      <button className={running ? 'btn-success w-full pulse-glow mb-2' : 'btn w-full mb-2'}
        disabled={disabled} onClick={onFire}>
        {running ? (a.loop ? '🔁 Em loop' : '⏳ Rodando...') : '▶ Disparar'}
      </button>
      <div className="grid grid-cols-4 gap-1">
        {onEdit ? <button className="btn-ghost btn-sm" onClick={onEdit} title="Editar">✎</button> : <span />}
        {onDuplicate ? <button className="btn-ghost btn-sm" onClick={onDuplicate} title="Duplicar">⎘</button> : <span />}
        {onExport ? <button className="btn-ghost btn-sm" onClick={onExport} title="Exportar JSON">⬇</button> : <span />}
        {onDelete ? <button className="btn-ghost btn-sm" onClick={onDelete} title="Remover">🗑</button> : <span />}
      </div>
    </div>
  )
}
