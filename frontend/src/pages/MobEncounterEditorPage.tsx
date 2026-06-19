import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Mob = { id: string; count: number; dx: number; dy: number; dz: number; tag?: string; nbt?: string }
type Wave = { delayMs: number; mobs: Mob[]; announce: string }
type Encounter = {
  id: number; name: string; description: string; wavesJson: string;
  dimension: string; centerX: number; centerY: number; centerZ: number;
  totalDifficulty: number; timesRun: number; updatedAt: string;
}

const MOB_PRESETS = [
  'minecraft:zombie', 'minecraft:skeleton', 'minecraft:creeper', 'minecraft:enderman',
  'minecraft:wither_skeleton', 'minecraft:blaze', 'minecraft:phantom',
  'alexsmobs:cachalot_whale', 'alexsmobs:rocky_roller', 'alexsmobs:bone_serpent',
  'mowziesmobs:frostmaw', 'mowziesmobs:ferrous_wroughtnaut', 'mowziesmobs:foliaath',
  'born_in_chaos_v1:cursed_phantom', 'born_in_chaos_v1:tribal',
  'endermanoverhaul:bone_enderman', 'twilightforest:naga',
  'untamedwilds:bear', 'liberthia:flesh_mother',
]

export function MobEncounterEditorPage() {
  const [editing, setEditing] = useState<Encounter | null>(null)
  const [draft, setDraft] = useState<Partial<Encounter>>({})
  const [waves, setWaves] = useState<Wave[]>([])
  const [selectedWaveIdx, setSelectedWaveIdx] = useState<number | null>(null)
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['encounters'],
    queryFn: async () => (await fetch('/api/encounters', { headers: headers() })).json(),
  })

  const save = useMutation({
    mutationFn: async (body: any) => {
      const url = body.id ? `/api/encounters/${body.id}` : '/api/encounters'
      return (await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(), body: JSON.stringify({ ...body, wavesJson: JSON.stringify(waves) }) })).json()
    },
    onSuccess: () => { setEditing(null); setWaves([]); setDraft({}); qc.invalidateQueries({ queryKey: ['encounters'] }) },
  })

  const start = useMutation({
    mutationFn: async (id: number) => (await fetch(`/api/encounters/${id}/start`, { method: 'POST', headers: headers(), body: '{}' })).json(),
  })

  const clear = useMutation({
    mutationFn: async (id: number) => fetch(`/api/encounters/${id}/clear`, { method: 'POST', headers: headers() }),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/encounters/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['encounters'] }),
  })

  const items: Encounter[] = Array.isArray(q.data?.content) ? q.data.content : []

  function openEditor(e?: Encounter) {
    if (e) {
      setEditing(e); setDraft({ ...e })
      try { setWaves(JSON.parse(e.wavesJson || '[]')) } catch { setWaves([]) }
    } else {
      setEditing({ id: 0 } as Encounter)
      setDraft({ name: 'Novo Encontro', dimension: 'minecraft:overworld', totalDifficulty: 3 })
      setWaves([{ delayMs: 0, mobs: [], announce: 'Wave 1' }])
      setSelectedWaveIdx(0)
    }
  }

  function addWave() {
    setWaves(w => [...w, { delayMs: 10000, mobs: [], announce: `Wave ${w.length + 1}` }])
    setSelectedWaveIdx(waves.length)
  }

  function updateWave(idx: number, patch: Partial<Wave>) {
    setWaves(w => w.map((wv, i) => i === idx ? { ...wv, ...patch } : wv))
  }

  function removeWave(idx: number) {
    setWaves(w => w.filter((_, i) => i !== idx))
    setSelectedWaveIdx(null)
  }

  function addMobToWave(waveIdx: number, mobId: string) {
    const wave = waves[waveIdx]
    updateWave(waveIdx, { mobs: [...wave.mobs, { id: mobId, count: 1, dx: 0, dy: 0, dz: 0 }] })
  }

  function updateMob(waveIdx: number, mobIdx: number, patch: Partial<Mob>) {
    const wave = waves[waveIdx]
    updateWave(waveIdx, { mobs: wave.mobs.map((m, i) => i === mobIdx ? { ...m, ...patch } : m) })
  }

  function removeMob(waveIdx: number, mobIdx: number) {
    const wave = waves[waveIdx]
    updateWave(waveIdx, { mobs: wave.mobs.filter((_, i) => i !== mobIdx) })
  }

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div className="flex justify-between">
        <div>
          <h1 className="text-3xl font-bold">🐺 Mob Encounter Editor</h1>
          <p className="text-sm text-zinc-400">Designer de waves — define quem spawna, quando, e onde</p>
        </div>
        <button onClick={() => openEditor()} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded font-semibold">+ Novo Encontro</button>
      </div>

      {!editing && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {items.map((e) => {
            let waveCount = 0; let totalMobs = 0
            try {
              const ws: Wave[] = JSON.parse(e.wavesJson || '[]')
              waveCount = ws.length
              totalMobs = ws.reduce((acc, w) => acc + w.mobs.reduce((a, m) => a + m.count, 0), 0)
            } catch {}
            return (
              <div key={e.id} className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 hover:border-purple-700">
                <div className="flex justify-between mb-2">
                  <h3 className="font-bold">{e.name}</h3>
                  <span className="text-xs bg-red-900 px-2 py-0.5 rounded">Lv {e.totalDifficulty}</span>
                </div>
                <p className="text-xs text-zinc-500">{e.description}</p>
                <div className="grid grid-cols-3 gap-1 mt-2 text-xs text-center">
                  <div className="bg-zinc-950 rounded py-1">🌊 <b>{waveCount}</b><br />waves</div>
                  <div className="bg-zinc-950 rounded py-1">🐺 <b>{totalMobs}</b><br />mobs</div>
                  <div className="bg-zinc-950 rounded py-1">🎯 <b>{e.timesRun}</b><br />runs</div>
                </div>
                <div className="text-xs text-zinc-600 font-mono mt-2">
                  📍 {e.centerX},{e.centerY},{e.centerZ}
                </div>
                <div className="flex gap-1 mt-3">
                  <button onClick={() => start.mutate(e.id)} className="flex-1 bg-red-700 hover:bg-red-600 py-1.5 rounded text-sm font-semibold">⚔ Start</button>
                  <button onClick={() => clear.mutate(e.id)} className="bg-orange-900 hover:bg-orange-800 px-2 py-1.5 rounded text-sm">🧹</button>
                  <button onClick={() => openEditor(e)} className="bg-zinc-800 hover:bg-zinc-700 px-2 py-1.5 rounded text-sm">✎</button>
                  <button onClick={() => confirm('Apagar?') && remove.mutate(e.id)} className="bg-red-950 hover:bg-red-900 px-2 py-1.5 rounded text-sm">🗑</button>
                </div>
              </div>
            )
          })}
          {items.length === 0 && (
            <div className="col-span-full text-center py-16 text-zinc-500">
              <div className="text-6xl mb-3">🐺</div>
              <p>Nenhum encontro criado.</p>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="bg-zinc-950 border border-zinc-800 rounded-lg p-4 space-y-4">
          <div className="flex justify-between">
            <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xl font-bold flex-1 mr-2" placeholder="Nome do encontro" />
            <div className="flex gap-2">
              <button onClick={() => save.mutate({ ...draft, id: editing.id || undefined })} className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">💾 Salvar</button>
              <button onClick={() => { setEditing(null); setWaves([]); setDraft({}) }} className="bg-zinc-700 px-4 py-2 rounded">✕</button>
            </div>
          </div>

          <textarea value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value })}
            placeholder="Descrição (lore)" rows={2}
            className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />

          <div className="grid grid-cols-5 gap-2">
            <input value={draft.dimension ?? 'minecraft:overworld'} onChange={(e) => setDraft({ ...draft, dimension: e.target.value })} className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm font-mono" />
            <input type="number" placeholder="X" value={draft.centerX ?? 0} onChange={(e) => setDraft({ ...draft, centerX: parseFloat(e.target.value) || 0 })} className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input type="number" placeholder="Y" value={draft.centerY ?? 0} onChange={(e) => setDraft({ ...draft, centerY: parseFloat(e.target.value) || 0 })} className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input type="number" placeholder="Z" value={draft.centerZ ?? 0} onChange={(e) => setDraft({ ...draft, centerZ: parseFloat(e.target.value) || 0 })} className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <input type="number" placeholder="Dificuldade 1-10" min={1} max={10} value={draft.totalDifficulty ?? 3} onChange={(e) => setDraft({ ...draft, totalDifficulty: parseInt(e.target.value) || 1 })} className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />
          </div>

          {/* Waves */}
          <div className="grid grid-cols-1 lg:grid-cols-[200px_1fr] gap-4">
            <div className="space-y-2">
              <h3 className="font-bold flex justify-between items-center">Waves
                <button onClick={addWave} className="text-xs bg-emerald-700 hover:bg-emerald-600 px-2 py-1 rounded">+</button>
              </h3>
              {waves.map((w, i) => (
                <div key={i} onClick={() => setSelectedWaveIdx(i)}
                  className={`p-2 rounded cursor-pointer text-sm ${selectedWaveIdx === i ? 'bg-red-900 border border-red-500' : 'bg-zinc-900 border border-zinc-800 hover:border-zinc-600'}`}>
                  <div className="flex justify-between">
                    <span className="font-semibold">Wave {i + 1}</span>
                    <span className="text-xs text-zinc-400">+{w.delayMs / 1000}s</span>
                  </div>
                  <div className="text-xs text-zinc-500">{w.mobs.reduce((a, m) => a + m.count, 0)} mobs</div>
                </div>
              ))}
            </div>

            {selectedWaveIdx !== null && waves[selectedWaveIdx] && (
              <div className="bg-zinc-900 border border-zinc-700 rounded p-4 space-y-3">
                <div className="flex justify-between">
                  <h3 className="font-bold">Wave {selectedWaveIdx + 1}</h3>
                  <button onClick={() => removeWave(selectedWaveIdx)} className="text-red-400 text-sm">🗑 Remover wave</button>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <label className="text-sm">
                    <div className="text-xs text-zinc-500">Delay desde wave anterior (ms)</div>
                    <input type="number" value={waves[selectedWaveIdx].delayMs} onChange={(e) => updateWave(selectedWaveIdx, { delayMs: parseInt(e.target.value) || 0 })} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5" />
                  </label>
                  <label className="text-sm">
                    <div className="text-xs text-zinc-500">Anúncio in-game</div>
                    <input value={waves[selectedWaveIdx].announce} onChange={(e) => updateWave(selectedWaveIdx, { announce: e.target.value })} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5" />
                  </label>
                </div>

                <div>
                  <h4 className="text-sm font-semibold mb-1">Adicionar mob:</h4>
                  <div className="flex flex-wrap gap-1">
                    {MOB_PRESETS.map(id => (
                      <button key={id} onClick={() => addMobToWave(selectedWaveIdx, id)}
                        className="text-xs bg-zinc-800 hover:bg-zinc-700 px-2 py-1 rounded font-mono">{id.split(':')[1]}</button>
                    ))}
                  </div>
                </div>

                <div className="space-y-2">
                  {waves[selectedWaveIdx].mobs.map((m, mi) => (
                    <div key={mi} className="bg-zinc-950 border border-zinc-800 rounded p-2 grid grid-cols-[1fr_60px_50px_50px_50px_30px] gap-2 items-center text-xs">
                      <input value={m.id} onChange={(e) => updateMob(selectedWaveIdx, mi, { id: e.target.value })} className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1 font-mono" />
                      <input type="number" min={1} value={m.count} onChange={(e) => updateMob(selectedWaveIdx, mi, { count: parseInt(e.target.value) || 1 })} className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1" title="count" />
                      <input type="number" placeholder="dx" value={m.dx} onChange={(e) => updateMob(selectedWaveIdx, mi, { dx: parseFloat(e.target.value) || 0 })} className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1" title="dx" />
                      <input type="number" placeholder="dy" value={m.dy} onChange={(e) => updateMob(selectedWaveIdx, mi, { dy: parseFloat(e.target.value) || 0 })} className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1" title="dy" />
                      <input type="number" placeholder="dz" value={m.dz} onChange={(e) => updateMob(selectedWaveIdx, mi, { dz: parseFloat(e.target.value) || 0 })} className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1" title="dz" />
                      <button onClick={() => removeMob(selectedWaveIdx, mi)} className="text-red-400">✕</button>
                    </div>
                  ))}
                  {waves[selectedWaveIdx].mobs.length === 0 && <p className="text-zinc-500 text-sm italic">Nenhum mob nessa wave</p>}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
