import { useState } from 'react'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Macro Recorder — registra comandos manualmente em sequência. Replay roda
 * todos sequencialmente. Cada comando pode ter delay próprio.
 * Persiste em localStorage.
 */

type MacroStep = { cmd: string; delayMs: number }
type Macro = { id: string; name: string; emoji: string; steps: MacroStep[]; updated: number }

export function MacroRecorderPage() {
  const [macros, setMacros] = useKvState<Macro[]>('macros', [])
  const [editing, setEditing] = useState<Macro | null>(null)
  const [running, setRunning] = useState<string | null>(null)

  function newMacro() {
    setEditing({
      id: `m${Date.now().toString(36)}`,
      name: 'Nova Macro',
      emoji: '⚙',
      steps: [{ cmd: 'say hello', delayMs: 500 }],
      updated: Date.now(),
    })
  }

  function commit(m: Macro) {
    setMacros((cur) => {
      const i = cur.findIndex((x) => x.id === m.id)
      if (i >= 0) { const n = [...cur]; n[i] = m; return n }
      return [...cur, m]
    })
    setEditing(null)
  }

  async function run(m: Macro) {
    if (running) return
    setRunning(m.id)
    let ok = 0, fail = 0
    for (const s of m.steps) {
      try { await api.command(s.cmd, 'macro'); ok++ } catch { fail++ }
      await new Promise((r) => setTimeout(r, s.delayMs))
    }
    setRunning(null)
    toast.ok(`✓ ${m.name}: ${ok}/${ok + fail} ok`)
  }

  return (
    <div className="route-fade max-w-[1300px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⚙ Macro Recorder</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Salva sequências de comandos como macros nomeadas. Replay com 1 click.
          </p>
        </div>
        <button className="btn" onClick={newMacro}>+ Nova Macro</button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {macros.length === 0 && (
          <div className="card text-center py-12 col-span-full">
            <div className="text-5xl mb-3 opacity-50">⚙</div>
            <p className="text-liberthia-300/70 text-sm">Nenhuma macro criada.</p>
          </div>
        )}
        {macros.map((m) => (
          <div key={m.id} className="card-glow">
            <div className="text-4xl text-center mb-2">{m.emoji}</div>
            <div className="font-bold text-center mb-1">{m.name}</div>
            <div className="text-xs text-liberthia-300/60 text-center mb-3">{m.steps.length} comandos</div>
            <div className="space-y-0.5 mb-3 max-h-32 overflow-y-auto text-[10px] font-mono">
              {m.steps.slice(0, 5).map((s, i) => (
                <div key={i} className="bg-liberthia-900/40 px-2 py-0.5 rounded truncate">
                  /{s.cmd}
                </div>
              ))}
              {m.steps.length > 5 && <div className="text-liberthia-300/50">+{m.steps.length - 5}...</div>}
            </div>
            <div className="grid grid-cols-3 gap-1">
              <button className={running === m.id ? 'btn-amber btn-sm pulse-glow' : 'btn-success btn-sm'} onClick={() => run(m)} disabled={!!running}>
                {running === m.id ? '⏳' : '▶'}
              </button>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(m)}>✎</button>
              <button className="btn-ghost btn-sm" onClick={() => setMacros((cur) => cur.filter((x) => x.id !== m.id))}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {editing && <MacroEditor macro={editing} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function MacroEditor({ macro, onSave, onCancel }: { macro: Macro; onSave: (m: Macro) => void; onCancel: () => void }) {
  const [m, setM] = useState<Macro>(macro)
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⚙ Macro</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-4">
          <input className="input text-2xl text-center" value={m.emoji} onChange={(e) => setM({ ...m, emoji: e.target.value })} />
          <input className="input font-bold" value={m.name} onChange={(e) => setM({ ...m, name: e.target.value })} />
        </div>
        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">📋 Comandos ({m.steps.length})</h4>
          <button className="btn-ghost btn-sm" onClick={() => setM({ ...m, steps: [...m.steps, { cmd: '', delayMs: 500 }] })}>+ Comando</button>
        </div>
        <div className="space-y-2 mb-4">
          {m.steps.map((s, i) => (
            <div key={i} className="flex gap-2 items-center">
              <span className="text-xs text-liberthia-300/40 w-6">#{i + 1}</span>
              <input className="input font-mono text-xs flex-1" value={s.cmd} onChange={(e) => setM({ ...m, steps: m.steps.map((x, j) => j === i ? { ...x, cmd: e.target.value } : x) })} placeholder="say hello" />
              <input type="number" className="input text-xs w-20" value={s.delayMs} step={100} onChange={(e) => setM({ ...m, steps: m.steps.map((x, j) => j === i ? { ...x, delayMs: Number(e.target.value) } : x) })} />
              <span className="text-[10px] text-liberthia-300/50">ms</span>
              <button className="btn-ghost btn-sm" onClick={() => setM({ ...m, steps: m.steps.filter((_, j) => j !== i) })}>🗑</button>
            </div>
          ))}
        </div>
        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(m)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
