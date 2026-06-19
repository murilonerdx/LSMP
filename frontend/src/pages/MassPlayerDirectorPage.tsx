import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { api } from '../lib/api'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

async function director(op: string, body: any) {
  const r = await fetch(`/api/director/${op}`, { method: 'POST', headers: headers(), body: JSON.stringify(body) })
  return r.json()
}

export function MassPlayerDirectorPage() {
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [lastResult, setLastResult] = useState<any>(null)

  // Forms
  const [tpForm, setTpForm] = useState({ x: 0, y: 64, z: 0, dimension: 'minecraft:overworld' })
  const [titleForm, setTitleForm] = useState({ title: '§5§lBem-vindo', subtitle: '', fadeIn: 10, stay: 60, fadeOut: 10 })
  const [soundForm, setSoundForm] = useState({ sound: 'minecraft:event.raid.horn', volume: 1.0, pitch: 1.0 })
  const [effectForm, setEffectForm] = useState({ effect: 'minecraft:slowness', duration: 60, amplifier: 1 })
  const [giveForm, setGiveForm] = useState({ item: 'minecraft:apple', count: 1 })
  const [fadeForm, setFadeForm] = useState({ durationSec: 5 })

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = Array.isArray(playersQ.data) ? playersQ.data : []

  function toggle(uuid: string) {
    setSelected(s => {
      const n = new Set(s); n.has(uuid) ? n.delete(uuid) : n.add(uuid); return n
    })
  }

  function selectAll() { setSelected(new Set(players.map(p => p.uuid))) }
  function selectNone() { setSelected(new Set()) }

  const playerUuids = Array.from(selected)

  async function run(op: string, extra: any) {
    if (playerUuids.length === 0) { alert('Selecione players primeiro'); return }
    setLastResult({ op, running: true })
    const result = await director(op, { ...extra, playerUuids })
    setLastResult(result)
  }

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div>
        <h1 className="text-3xl font-bold">🎬 Mass Player Director</h1>
        <p className="text-sm text-zinc-400">Controla múltiplos players ao mesmo tempo — cutscenes coletivas, eventos sincronizados</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[400px_1fr] gap-4">
        {/* Player selector */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 space-y-3">
          <div className="flex justify-between items-center">
            <h3 className="font-bold">🎯 Selecionados ({selected.size}/{players.length})</h3>
            <div className="flex gap-1">
              <button onClick={selectAll} className="text-xs bg-emerald-800 hover:bg-emerald-700 px-2 py-1 rounded">Todos</button>
              <button onClick={selectNone} className="text-xs bg-zinc-700 hover:bg-zinc-600 px-2 py-1 rounded">Nenhum</button>
            </div>
          </div>
          <div className="space-y-1 max-h-[600px] overflow-y-auto">
            {players.map(p => (
              <label key={p.uuid} className={`flex items-center gap-2 p-2 rounded cursor-pointer ${selected.has(p.uuid) ? 'bg-purple-900/50 border border-purple-500' : 'bg-zinc-950 border border-zinc-800 hover:border-zinc-600'}`}>
                <input type="checkbox" checked={selected.has(p.uuid)} onChange={() => toggle(p.uuid)} />
                <div className="flex-1 min-w-0">
                  <div className="font-semibold truncate">{p.name}</div>
                  <div className="text-xs text-zinc-500">{p.dimension} · ❤ {p.health.toFixed(0)}/{p.maxHealth}</div>
                </div>
              </label>
            ))}
            {players.length === 0 && <p className="text-zinc-500 text-center text-sm py-4">Nenhum player online.</p>}
          </div>
        </div>

        {/* Actions */}
        <div className="space-y-3">
          {/* Quick actions */}
          <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 space-y-3">
            <h3 className="font-bold">⚡ Ações rápidas</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              <button onClick={() => run('lightning', {})} className="bg-yellow-700 hover:bg-yellow-600 py-2 rounded text-sm font-semibold">⚡ Raio</button>
              <button onClick={() => run('freeze', {})} className="bg-sky-700 hover:bg-sky-600 py-2 rounded text-sm font-semibold">🧊 Freeze</button>
              <button onClick={() => run('unfreeze', {})} className="bg-orange-700 hover:bg-orange-600 py-2 rounded text-sm font-semibold">🌡 Unfreeze</button>
              <button onClick={() => run('heal', {})} className="bg-pink-700 hover:bg-pink-600 py-2 rounded text-sm font-semibold">💖 Heal</button>
              <button onClick={() => run('feed', {})} className="bg-amber-700 hover:bg-amber-600 py-2 rounded text-sm font-semibold">🍗 Feed</button>
              <button onClick={() => run('fade-black', fadeForm)} className="bg-zinc-700 hover:bg-zinc-600 py-2 rounded text-sm font-semibold">⚫ Fade Black</button>
            </div>
          </div>

          {/* TP mass */}
          <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 space-y-2">
            <h3 className="font-bold">🌀 Teleport em massa</h3>
            <div className="flex items-center gap-2 text-sm">
              <label className="text-xs text-zinc-500 whitespace-nowrap">📍 Pegar de:</label>
              <select onChange={(e) => {
                const uuid = e.target.value
                if (!uuid) return
                const p = players.find(pl => pl.uuid === uuid)
                if (p) setTpForm({
                  x: Math.round(p.position.x),
                  y: Math.round(p.position.y),
                  z: Math.round(p.position.z),
                  dimension: p.dimension
                })
                e.target.value = ''
              }} className="flex-1 bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5 text-sm">
                <option value="">(player...)</option>
                {players.map(p => <option key={p.uuid} value={p.uuid}>{p.name} ({Math.round(p.position.x)},{Math.round(p.position.y)},{Math.round(p.position.z)})</option>)}
              </select>
            </div>
            <div className="grid grid-cols-4 gap-2">
              <input type="number" placeholder="X" value={tpForm.x} onChange={(e) => setTpForm({ ...tpForm, x: parseFloat(e.target.value) || 0 })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5 text-sm" />
              <input type="number" placeholder="Y" value={tpForm.y} onChange={(e) => setTpForm({ ...tpForm, y: parseFloat(e.target.value) || 0 })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5 text-sm" />
              <input type="number" placeholder="Z" value={tpForm.z} onChange={(e) => setTpForm({ ...tpForm, z: parseFloat(e.target.value) || 0 })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5 text-sm" />
              <input placeholder="Dimension" value={tpForm.dimension} onChange={(e) => setTpForm({ ...tpForm, dimension: e.target.value })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5 text-sm font-mono" />
            </div>
            <button onClick={() => run('teleport', tpForm)} className="w-full bg-emerald-700 hover:bg-emerald-600 py-2 rounded font-semibold">🌀 Teleportar todos</button>
          </div>

          {/* Title mass */}
          <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 space-y-2">
            <h3 className="font-bold">📝 Title em massa</h3>
            <input value={titleForm.title} onChange={(e) => setTitleForm({ ...titleForm, title: e.target.value })} placeholder="Título principal" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm" />
            <input value={titleForm.subtitle} onChange={(e) => setTitleForm({ ...titleForm, subtitle: e.target.value })} placeholder="Subtítulo" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm" />
            <div className="grid grid-cols-3 gap-2 text-xs">
              <input type="number" placeholder="fade in" value={titleForm.fadeIn} onChange={(e) => setTitleForm({ ...titleForm, fadeIn: parseInt(e.target.value) || 0 })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1" />
              <input type="number" placeholder="stay" value={titleForm.stay} onChange={(e) => setTitleForm({ ...titleForm, stay: parseInt(e.target.value) || 0 })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1" />
              <input type="number" placeholder="fade out" value={titleForm.fadeOut} onChange={(e) => setTitleForm({ ...titleForm, fadeOut: parseInt(e.target.value) || 0 })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1" />
            </div>
            <button onClick={() => run('title', titleForm)} className="w-full bg-blue-700 hover:bg-blue-600 py-2 rounded font-semibold">📝 Mostrar title</button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 space-y-2">
              <h3 className="font-bold">🔊 Som</h3>
              <input value={soundForm.sound} onChange={(e) => setSoundForm({ ...soundForm, sound: e.target.value })} placeholder="minecraft:event.raid.horn" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm font-mono text-xs" />
              <button onClick={() => run('sound', soundForm)} className="w-full bg-amber-700 hover:bg-amber-600 py-2 rounded text-sm font-semibold">🔊 Tocar</button>
            </div>
            <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 space-y-2">
              <h3 className="font-bold">✨ Effect</h3>
              <input value={effectForm.effect} onChange={(e) => setEffectForm({ ...effectForm, effect: e.target.value })} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm font-mono text-xs" />
              <div className="grid grid-cols-2 gap-1">
                <input type="number" placeholder="duration" value={effectForm.duration} onChange={(e) => setEffectForm({ ...effectForm, duration: parseInt(e.target.value) })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-xs" />
                <input type="number" placeholder="amp" value={effectForm.amplifier} onChange={(e) => setEffectForm({ ...effectForm, amplifier: parseInt(e.target.value) })} className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-xs" />
              </div>
              <button onClick={() => run('effect', effectForm)} className="w-full bg-fuchsia-700 hover:bg-fuchsia-600 py-2 rounded text-sm font-semibold">✨ Aplicar</button>
            </div>
          </div>

          {/* Result */}
          {lastResult && (
            <div className="bg-zinc-950 border border-zinc-700 rounded p-3 text-xs font-mono">
              <pre className="text-zinc-300 whitespace-pre-wrap">{JSON.stringify(lastResult, null, 2)}</pre>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
