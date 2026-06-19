import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Costume = {
  id: number; name: string; archetype: string; description: string;
  fakeName: string; chatTitle: string; cpmProjectPath: string;
  skinUrl: string; armourerSkinId: string; scale: number;
  equipmentJson: string; effectsJson: string;
  timesUsed: number; updatedAt: string;
}

const ARCHETYPES = [
  { id: 'hero', label: 'Herói', emoji: '🦸', color: 'bg-amber-700' },
  { id: 'villain', label: 'Vilão', emoji: '👹', color: 'bg-red-900' },
  { id: 'npc', label: 'NPC', emoji: '🧑', color: 'bg-zinc-700' },
  { id: 'spirit', label: 'Espírito', emoji: '👻', color: 'bg-purple-900' },
  { id: 'monster', label: 'Monstro', emoji: '👺', color: 'bg-emerald-900' },
  { id: 'god', label: 'Divindade', emoji: '⚜️', color: 'bg-yellow-700' },
]

export function NpcCostumeLibraryPage() {
  const [filter, setFilter] = useState<string>('all')
  const [editing, setEditing] = useState<Costume | null>(null)
  const [draft, setDraft] = useState<Partial<Costume>>({})
  const [applyTarget, setApplyTarget] = useState<{ costume: Costume; playerName: string } | null>(null)
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['costumes', filter],
    queryFn: async () => {
      const url = filter === 'all' ? '/api/costumes' : `/api/costumes?archetype=${filter}`
      return (await fetch(url, { headers: headers() })).json()
    },
  })

  const save = useMutation({
    mutationFn: async (body: any) => {
      const url = body.id ? `/api/costumes/${body.id}` : '/api/costumes'
      return (await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(), body: JSON.stringify(body) })).json()
    },
    onSuccess: () => { setEditing(null); setDraft({}); qc.invalidateQueries({ queryKey: ['costumes'] }) },
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/costumes/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['costumes'] }),
  })

  const apply = useMutation({
    mutationFn: async ({ id, playerName }: { id: number; playerName: string }) =>
      (await fetch(`/api/costumes/${id}/apply`, { method: 'POST', headers: headers(), body: JSON.stringify({ playerName }) })).json(),
    onSuccess: (data) => { setApplyTarget(null); qc.invalidateQueries({ queryKey: ['costumes'] }); alert('✓ Aplicado: ' + JSON.stringify(data)) },
  })

  const restore = useMutation({
    mutationFn: async (playerName: string) =>
      fetch(`/api/costumes/restore`, { method: 'POST', headers: headers(), body: JSON.stringify({ playerName }) }),
  })

  const items: Costume[] = Array.isArray(q.data?.content) ? q.data.content : []

  function archetypeOf(id: string) { return ARCHETYPES.find(a => a.id === id) ?? ARCHETYPES[2] }

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div className="flex justify-between">
        <div>
          <h1 className="text-3xl font-bold">🎭 NPC Costume Library</h1>
          <p className="text-sm text-zinc-400">Personagens RP prontos pra vestir — nome, skin, escala, armadura, efeitos</p>
        </div>
        <button onClick={() => { setEditing({ id: 0 } as Costume); setDraft({ archetype: 'npc', scale: 1.0 }) }} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded font-semibold">+ Novo Costume</button>
      </div>

      <div className="flex gap-2 flex-wrap">
        <button onClick={() => setFilter('all')} className={`px-3 py-1 rounded ${filter === 'all' ? 'bg-purple-700' : 'bg-zinc-800'}`}>Todos</button>
        {ARCHETYPES.map(a => (
          <button key={a.id} onClick={() => setFilter(a.id)} className={`px-3 py-1 rounded ${filter === a.id ? a.color : 'bg-zinc-800'} text-sm`}>
            {a.emoji} {a.label}
          </button>
        ))}
      </div>

      {!editing && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
          {items.map((c) => {
            const a = archetypeOf(c.archetype || 'npc')
            return (
              <div key={c.id} className="bg-gradient-to-b from-zinc-900 to-zinc-950 border border-zinc-800 rounded-lg overflow-hidden hover:border-purple-700 transition">
                <div className={`${a.color} h-1`}></div>
                <div className="p-4">
                  <div className="flex justify-between items-start">
                    <div className="text-3xl">{a.emoji}</div>
                    <span className="text-xs text-zinc-500">{c.timesUsed}x usado</span>
                  </div>
                  <h3 className="font-bold mt-2">{c.name}</h3>
                  {c.fakeName && <div className="text-xs text-purple-400 mt-0.5" dangerouslySetInnerHTML={{__html: c.fakeName}} />}
                  <p className="text-xs text-zinc-500 mt-1 line-clamp-2">{c.description || '(sem descrição)'}</p>
                  <div className="flex flex-wrap gap-1 mt-2 text-xs">
                    {c.scale && c.scale !== 1.0 && <span className="bg-cyan-900 px-1.5 py-0.5 rounded">{c.scale}x</span>}
                    {c.cpmProjectPath && <span className="bg-fuchsia-900 px-1.5 py-0.5 rounded">CPM</span>}
                    {c.armourerSkinId && <span className="bg-amber-900 px-1.5 py-0.5 rounded">Armourers</span>}
                  </div>
                  <div className="flex gap-1 mt-3">
                    <button onClick={() => setApplyTarget({ costume: c, playerName: '' })} className="flex-1 bg-emerald-700 hover:bg-emerald-600 py-1.5 rounded text-sm">👤 Vestir</button>
                    <button onClick={() => { setEditing(c); setDraft({ ...c }) }} className="bg-zinc-800 hover:bg-zinc-700 px-2 py-1.5 rounded text-sm">✎</button>
                    <button onClick={() => confirm('Apagar?') && remove.mutate(c.id)} className="bg-red-900 hover:bg-red-700 px-2 py-1.5 rounded text-sm">🗑</button>
                  </div>
                </div>
              </div>
            )
          })}
          {items.length === 0 && (
            <div className="col-span-full text-center py-16 text-zinc-500">
              <div className="text-6xl mb-3">🎭</div>
              <p>Nenhum costume criado.</p>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="bg-zinc-950 border border-zinc-800 rounded-lg p-6 space-y-4 max-w-3xl mx-auto">
          <div className="flex justify-between items-start">
            <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xl font-bold flex-1 mr-2" placeholder="Nome do personagem" />
            <div className="flex gap-2">
              <button onClick={() => save.mutate({ ...draft, id: editing.id || undefined })} className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">💾 Salvar</button>
              <button onClick={() => { setEditing(null); setDraft({}) }} className="bg-zinc-700 px-4 py-2 rounded">✕</button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-zinc-500">Arquétipo</label>
              <select value={draft.archetype ?? 'npc'} onChange={(e) => setDraft({ ...draft, archetype: e.target.value })}
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2">
                {ARCHETYPES.map(a => <option key={a.id} value={a.id}>{a.emoji} {a.label}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-zinc-500">Escala (Pehkui)</label>
              <input type="number" step={0.1} min={0.1} max={10} value={draft.scale ?? 1.0}
                onChange={(e) => setDraft({ ...draft, scale: parseFloat(e.target.value) })}
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2" />
            </div>
          </div>

          <textarea value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value })}
            placeholder="Descrição do personagem"
            rows={2} className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-zinc-500">Nome falso (§e§lFake Name) — fakename mod</label>
              <input value={draft.fakeName ?? ''} onChange={(e) => setDraft({ ...draft, fakeName: e.target.value })}
                placeholder="§ePassageiro Solitário"
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 font-mono text-sm" />
            </div>
            <div>
              <label className="text-xs text-zinc-500">Chat title</label>
              <input value={draft.chatTitle ?? ''} onChange={(e) => setDraft({ ...draft, chatTitle: e.target.value })}
                placeholder="[PRESO]"
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />
            </div>
          </div>

          <div className="space-y-3 border-t border-zinc-800 pt-4">
            <h3 className="text-sm font-semibold text-zinc-400">🎨 Modelo & Skin</h3>
            <div>
              <label className="text-xs text-zinc-500">CPM Project Path (CustomPlayerModels mod)</label>
              <input value={draft.cpmProjectPath ?? ''} onChange={(e) => setDraft({ ...draft, cpmProjectPath: e.target.value })}
                placeholder="cpm-models/anfitriao.cpmproject"
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
              <p className="text-xs text-zinc-500 mt-1">Arquivo .cpmproject acessível pelo server</p>
            </div>
            <div>
              <label className="text-xs text-zinc-500">ArmourersWorkshop Skin ID</label>
              <input value={draft.armourerSkinId ?? ''} onChange={(e) => setDraft({ ...draft, armourerSkinId: e.target.value })}
                placeholder="db:1234 ou path do arquivo .armour"
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
            </div>
            <div>
              <label className="text-xs text-zinc-500">Skin URL fallback (PNG 64x64)</label>
              <input value={draft.skinUrl ?? ''} onChange={(e) => setDraft({ ...draft, skinUrl: e.target.value })}
                placeholder="https://..."
                className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xs" />
            </div>
          </div>

          <div className="space-y-3 border-t border-zinc-800 pt-4">
            <h3 className="text-sm font-semibold text-zinc-400">⚔️ Equipamento (JSON)</h3>
            <textarea value={draft.equipmentJson ?? ''} onChange={(e) => setDraft({ ...draft, equipmentJson: e.target.value })}
              placeholder='{"mainhand":{"id":"minecraft:netherite_sword"},"helmet":{"id":"minecraft:netherite_helmet"}}'
              rows={4}
              className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
          </div>

          <div className="space-y-3 border-t border-zinc-800 pt-4">
            <h3 className="text-sm font-semibold text-zinc-400">✨ Efeitos ao vestir (JSON array)</h3>
            <textarea value={draft.effectsJson ?? ''} onChange={(e) => setDraft({ ...draft, effectsJson: e.target.value })}
              placeholder='[{"effect":"minecraft:speed","durationSec":3600,"amplifier":1}]'
              rows={3}
              className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
          </div>
        </div>
      )}

      {applyTarget && (
        <div onClick={() => setApplyTarget(null)} className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-md w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">Vestir: {applyTarget.costume.name}</h2>
            <p className="text-sm text-zinc-400">Em qual player aplicar?</p>
            <input value={applyTarget.playerName} onChange={(e) => setApplyTarget({ ...applyTarget, playerName: e.target.value })}
              placeholder="Nome do player" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2" />
            <div className="flex gap-2">
              <button onClick={() => restore.mutate(applyTarget.playerName)} className="bg-amber-700 hover:bg-amber-600 px-3 py-2 rounded text-sm">↶ Restaurar antes</button>
              <button onClick={() => apply.mutate({ id: applyTarget.costume.id, playerName: applyTarget.playerName })}
                disabled={!applyTarget.playerName}
                className="ml-auto bg-emerald-700 hover:bg-emerald-600 disabled:bg-zinc-800 px-4 py-2 rounded">👤 Aplicar</button>
              <button onClick={() => setApplyTarget(null)} className="bg-zinc-700 px-4 py-2 rounded">Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
