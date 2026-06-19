import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Choice = { label: string; nextId?: string }
type DialogNode = { id: string; text: string; speaker?: string; choices: Choice[] }

type DialogTree = {
  id: number; name: string; npcTag: string; rootNodeId: string;
  nodesJson: string; balloonStyle: 'talk' | 'comic' | 'title';
  active: boolean; updatedAt: string;
}

export function DialogTreeBuilderPage() {
  const [editing, setEditing] = useState<DialogTree | null>(null)
  const [draft, setDraft] = useState<Partial<DialogTree>>({})
  const [nodes, setNodes] = useState<Record<string, DialogNode>>({})
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [testing, setTesting] = useState<{ treeId: number; nodeId: string } | null>(null)
  const [testPlayer, setTestPlayer] = useState('')
  const qc = useQueryClient()

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000, enabled: testing !== null })

  const trigger = useMutation({
    mutationFn: async (body: { treeId: number; playerUuid: string; nodeId: string }) =>
      (await fetch(`/api/dialog-trees/${body.treeId}/trigger`, {
        method: 'POST', headers: headers(),
        body: JSON.stringify({ playerUuid: body.playerUuid, nodeId: body.nodeId })
      })).json(),
  })

  const q = useQuery({
    queryKey: ['dialog-trees'],
    queryFn: async () => (await fetch('/api/dialog-trees', { headers: headers() })).json(),
  })

  const save = useMutation({
    mutationFn: async (body: any) => {
      const url = body.id ? `/api/dialog-trees/${body.id}` : '/api/dialog-trees'
      return (await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(), body: JSON.stringify({ ...body, nodesJson: JSON.stringify(nodes) }) })).json()
    },
    onSuccess: () => { setEditing(null); setNodes({}); setDraft({}); qc.invalidateQueries({ queryKey: ['dialog-trees'] }) },
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/dialog-trees/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['dialog-trees'] }),
  })

  const items: DialogTree[] = Array.isArray(q.data?.content) ? q.data.content : []

  function openEditor(d?: DialogTree) {
    if (d) {
      setEditing(d); setDraft({ ...d })
      try { setNodes(JSON.parse(d.nodesJson || '{}')) } catch { setNodes({}) }
    } else {
      setEditing({ id: 0 } as DialogTree)
      const start: DialogNode = { id: 'start', text: 'Olá, viajante. O que te trouxe aqui?', choices: [] }
      setNodes({ start })
      setDraft({ name: 'Novo Diálogo', npcTag: 'npc_default', rootNodeId: 'start', balloonStyle: 'talk', active: true })
      setSelectedNodeId('start')
    }
  }

  function addNode() {
    const id = 'n_' + Math.random().toString(36).slice(2, 8)
    const node: DialogNode = { id, text: 'Nova fala...', choices: [] }
    setNodes(n => ({ ...n, [id]: node }))
    setSelectedNodeId(id)
  }

  function updateNode(id: string, patch: Partial<DialogNode>) {
    setNodes(n => ({ ...n, [id]: { ...n[id], ...patch } }))
  }

  function removeNode(id: string) {
    setNodes(n => {
      const next = { ...n }
      delete next[id]
      return next
    })
    setSelectedNodeId(null)
  }

  function addChoice(nodeId: string) {
    const n = nodes[nodeId]
    updateNode(nodeId, { choices: [...n.choices, { label: 'Resposta', nextId: '' }] })
  }

  function updateChoice(nodeId: string, idx: number, patch: Partial<Choice>) {
    const n = nodes[nodeId]
    updateNode(nodeId, { choices: n.choices.map((c, i) => i === idx ? { ...c, ...patch } : c) })
  }

  function removeChoice(nodeId: string, idx: number) {
    const n = nodes[nodeId]
    updateNode(nodeId, { choices: n.choices.filter((_, i) => i !== idx) })
  }

  const selectedNode = selectedNodeId ? nodes[selectedNodeId] : null

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">💬 Dialog Tree Builder</h1>
          <p className="text-sm text-zinc-400">Editor de árvore de diálogo pra NPCs (TalkBalloons / Comics Bubbles)</p>
        </div>
        <button onClick={() => openEditor()} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded font-semibold">+ Nova Árvore</button>
      </div>

      {!editing && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {items.map((d) => {
            let nodeCount = 0
            try { nodeCount = Object.keys(JSON.parse(d.nodesJson || '{}')).length } catch {}
            return (
              <div key={d.id} className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 hover:border-purple-700 transition">
                <div className="flex justify-between mb-2">
                  <h3 className="font-bold">{d.name}</h3>
                  <span className={`text-xs px-2 py-0.5 rounded ${d.active ? 'bg-green-900 text-green-300' : 'bg-zinc-800 text-zinc-500'}`}>
                    {d.active ? 'ativo' : 'pausado'}
                  </span>
                </div>
                <p className="text-xs text-zinc-500">Tag: <code className="text-purple-400">{d.npcTag}</code></p>
                <p className="text-xs text-zinc-500 mt-1">{nodeCount} nós · estilo {d.balloonStyle}</p>
                <div className="flex gap-2 mt-3">
                  <button onClick={() => openEditor(d)} className="flex-1 bg-zinc-800 hover:bg-zinc-700 py-1.5 rounded text-sm">✎ Editar</button>
                  <button onClick={() => confirm('Apagar?') && remove.mutate(d.id)} className="bg-red-900 hover:bg-red-700 px-3 py-1.5 rounded text-sm">🗑</button>
                </div>
              </div>
            )
          })}
          {items.length === 0 && (
            <div className="col-span-full text-center py-16 text-zinc-500">
              <div className="text-6xl mb-3">💬</div>
              <p>Nenhuma árvore de diálogo. Crie um NPC falante!</p>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-4">
          <div className="bg-zinc-950 border border-zinc-800 rounded-lg p-4 space-y-3">
            <div className="flex justify-between">
              <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xl font-bold flex-1 mr-2" />
              <div className="flex gap-2">
                <button onClick={() => save.mutate({ ...draft, id: editing.id || undefined })} className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">💾 Salvar</button>
                <button onClick={() => { setEditing(null); setNodes({}); setDraft({}) }} className="bg-zinc-700 px-4 py-2 rounded">✕</button>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <input value={draft.npcTag ?? ''} onChange={(e) => setDraft({ ...draft, npcTag: e.target.value })}
                placeholder="NPC tag" className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />
              <select value={draft.balloonStyle ?? 'talk'} onChange={(e) => setDraft({ ...draft, balloonStyle: e.target.value as any })}
                className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm">
                <option value="talk">TalkBalloons</option>
                <option value="comic">Comics Bubbles</option>
                <option value="title">Title screen</option>
              </select>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={draft.active ?? true} onChange={(e) => setDraft({ ...draft, active: e.target.checked })} />
                Ativo
              </label>
            </div>

            <div className="flex justify-between items-center">
              <h3 className="font-semibold">Nós da árvore ({Object.keys(nodes).length})</h3>
              <button onClick={addNode} className="bg-emerald-700 hover:bg-emerald-600 px-3 py-1 rounded text-sm">+ Adicionar nó</button>
            </div>

            <div className="space-y-2 max-h-[60vh] overflow-y-auto">
              {Object.values(nodes).map((n) => (
                <div key={n.id} onClick={() => setSelectedNodeId(n.id)}
                  className={`p-3 rounded border cursor-pointer transition ${
                    selectedNodeId === n.id ? 'border-purple-500 bg-purple-950/30' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
                  }`}>
                  <div className="flex justify-between items-start">
                    <div className="flex-1 min-w-0">
                      <code className="text-xs text-purple-400">{n.id}</code>
                      {n.id === draft.rootNodeId && <span className="ml-2 text-xs bg-emerald-900 text-emerald-300 px-1 py-0.5 rounded">START</span>}
                      <p className="text-sm mt-1 truncate text-zinc-200">"{n.text}"</p>
                    </div>
                    <span className="text-xs text-zinc-500 ml-2">{n.choices.length} 🌿</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {selectedNode && (
            <div className="bg-zinc-900 border border-zinc-700 rounded-lg p-4 space-y-3">
              <div className="flex justify-between">
                <h3 className="font-bold">Nó: <code className="text-purple-400">{selectedNode.id}</code></h3>
                <div className="flex gap-2">
                  <button onClick={() => editing.id && setTesting({ treeId: editing.id, nodeId: selectedNode.id })}
                    disabled={!editing.id}
                    className="text-xs bg-amber-700 hover:bg-amber-600 disabled:bg-zinc-800 disabled:text-zinc-600 px-2 py-1 rounded"
                    title="Testar diálogo num player real">▶ Testar</button>
                  <button onClick={() => setDraft({ ...draft, rootNodeId: selectedNode.id })}
                    className="text-xs bg-emerald-900 hover:bg-emerald-700 px-2 py-1 rounded">⭐ Start</button>
                  <button onClick={() => removeNode(selectedNode.id)}
                    className="text-xs bg-red-900 hover:bg-red-700 px-2 py-1 rounded">🗑</button>
                </div>
              </div>
              <div>
                <label className="text-xs text-zinc-500">Speaker (opcional)</label>
                <input value={selectedNode.speaker ?? ''} onChange={(e) => updateNode(selectedNode.id, { speaker: e.target.value })}
                  placeholder="Nome do NPC"
                  className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm" />
              </div>
              <div>
                <label className="text-xs text-zinc-500">Texto da fala</label>
                <textarea value={selectedNode.text} onChange={(e) => updateNode(selectedNode.id, { text: e.target.value })}
                  rows={4}
                  className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
              </div>
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-xs text-zinc-500">Escolhas</label>
                  <button onClick={() => addChoice(selectedNode.id)} className="text-xs bg-zinc-800 hover:bg-zinc-700 px-2 py-0.5 rounded">+</button>
                </div>
                <div className="space-y-1">
                  {selectedNode.choices.map((c, i) => (
                    <div key={i} className="flex gap-1 items-center">
                      <input value={c.label} onChange={(e) => updateChoice(selectedNode.id, i, { label: e.target.value })}
                        placeholder="Texto da escolha" className="flex-1 bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-xs" />
                      <span className="text-zinc-500 text-xs">→</span>
                      <select value={c.nextId ?? ''} onChange={(e) => updateChoice(selectedNode.id, i, { nextId: e.target.value })}
                        className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-xs">
                        <option value="">(fim)</option>
                        {Object.keys(nodes).filter(id => id !== selectedNode.id).map(id => <option key={id} value={id}>{id}</option>)}
                      </select>
                      <button onClick={() => removeChoice(selectedNode.id, i)} className="text-red-400 text-xs px-1">✕</button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {testing && (
        <div onClick={() => setTesting(null)} className="fixed inset-0 bg-black/80 z-[100] flex items-center justify-center p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded-lg max-w-md w-full p-6 space-y-3 border border-amber-700">
            <h2 className="text-xl font-bold">▶ Testar diálogo</h2>
            <p className="text-sm text-zinc-400">Triggera o nó <code className="text-purple-400">{testing.nodeId}</code> num player online.</p>

            <div>
              <label className="text-xs text-zinc-500">Player alvo</label>
              <select value={testPlayer} onChange={(e) => setTestPlayer(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2">
                <option value="">Selecione...</option>
                {(playersQ.data ?? []).map(p => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <p className="text-xs text-zinc-500 mt-1">⚠ Salve a árvore antes de testar pra triggerar a versão atual.</p>
            </div>

            {trigger.data && (
              <div className="bg-emerald-950 border border-emerald-700 rounded p-2 text-xs text-emerald-300">
                ✓ Triggered: {JSON.stringify(trigger.data)}
              </div>
            )}

            <div className="flex gap-2">
              <button onClick={() => trigger.mutate({ treeId: testing.treeId, playerUuid: testPlayer, nodeId: testing.nodeId })}
                disabled={!testPlayer || trigger.isPending}
                className="ml-auto bg-amber-700 hover:bg-amber-600 disabled:bg-zinc-800 px-4 py-2 rounded font-semibold">
                {trigger.isPending ? '⏳ Disparando...' : '▶ Disparar'}
              </button>
              <button onClick={() => { setTesting(null); setTestPlayer(''); trigger.reset() }} className="bg-zinc-700 px-4 py-2 rounded">Fechar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
