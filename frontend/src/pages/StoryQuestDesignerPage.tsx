import { useState, useEffect, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type QuestNode = {
  id: string; title: string; description: string;
  x: number; y: number;
  tasks: any[];
  rewards: any[];
}

type StoryQuest = {
  id: number; name: string; summary: string; archetype: string;
  nodesJson: string; connectionsJson: string; startNodeId: string;
  published: boolean; playersStarted: number; playersFinished: number;
  updatedAt: string;
}

export function StoryQuestDesignerPage() {
  const [editing, setEditing] = useState<StoryQuest | null>(null)
  const [draft, setDraft] = useState<Partial<StoryQuest>>({})
  const [nodes, setNodes] = useState<QuestNode[]>([])
  const [connections, setConnections] = useState<Array<{ from: string; to: string }>>([])
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [draggingNode, setDraggingNode] = useState<string | null>(null)
  const dragOffsetRef = useRef<{ dx: number; dy: number }>({ dx: 0, dy: 0 })
  const canvasRef = useRef<HTMLDivElement | null>(null)
  const qc = useQueryClient()

  // Global drag handler — captura mousemove no document inteiro
  useEffect(() => {
    if (!draggingNode) return
    function handleMove(e: MouseEvent) {
      const rect = canvasRef.current?.getBoundingClientRect()
      if (!rect) return
      const x = e.clientX - rect.left - dragOffsetRef.current.dx
      const y = e.clientY - rect.top - dragOffsetRef.current.dy
      setNodes(n => n.map(node => node.id === draggingNode ? { ...node, x, y } : node))
    }
    function handleUp() { setDraggingNode(null) }
    document.addEventListener('mousemove', handleMove)
    document.addEventListener('mouseup', handleUp)
    return () => {
      document.removeEventListener('mousemove', handleMove)
      document.removeEventListener('mouseup', handleUp)
    }
  }, [draggingNode])

  const q = useQuery({
    queryKey: ['story-quests'],
    queryFn: async () => (await fetch('/api/story-quests', { headers: headers() })).json(),
  })

  const save = useMutation({
    mutationFn: async (body: any) => {
      const url = body.id ? `/api/story-quests/${body.id}` : '/api/story-quests'
      return (await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(),
        body: JSON.stringify({ ...body, nodesJson: JSON.stringify(nodes), connectionsJson: JSON.stringify(connections) }) })).json()
    },
    onSuccess: () => { setEditing(null); setNodes([]); setConnections([]); setDraft({}); qc.invalidateQueries({ queryKey: ['story-quests'] }) },
  })

  const remove = useMutation({
    mutationFn: async (id: number) => fetch(`/api/story-quests/${id}`, { method: 'DELETE', headers: headers() }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['story-quests'] }),
  })

  const deploy = useMutation({
    mutationFn: async (id: number) => {
      const r = await fetch(`/api/story-quests/${id}/deploy`, { method: 'POST', headers: headers() })
      const data = await r.json()
      if (!r.ok) throw new Error(data.error || data.hint || `HTTP ${r.status}`)
      return data
    },
    onSuccess: (data) => alert('✓ Deployed!\n' + JSON.stringify(data, null, 2)),
    onError: (err: any) => alert('✗ Falhou: ' + err.message),
  })

  const items: StoryQuest[] = Array.isArray(q.data?.content) ? q.data.content : []

  function openEditor(q?: StoryQuest) {
    if (q) {
      setEditing(q); setDraft({ ...q })
      try { setNodes(JSON.parse(q.nodesJson || '[]')) } catch { setNodes([]) }
      try { setConnections(JSON.parse(q.connectionsJson || '[]')) } catch { setConnections([]) }
    } else {
      setEditing({ id: 0 } as StoryQuest)
      setDraft({ name: 'Nova Quest', archetype: 'main', startNodeId: 'n_start', published: false })
      setNodes([{ id: 'n_start', title: 'Início', description: 'Aqui começa sua jornada', x: 100, y: 100, tasks: [], rewards: [] }])
      setConnections([])
    }
  }

  function addNode() {
    const id = 'n_' + Math.random().toString(36).slice(2, 8)
    setNodes(n => [...n, { id, title: 'Novo objetivo', description: '', x: 200 + Math.random() * 400, y: 200 + Math.random() * 200, tasks: [], rewards: [] }])
    setSelectedNodeId(id)
  }

  function updateNode(id: string, patch: Partial<QuestNode>) {
    setNodes(n => n.map(node => node.id === id ? { ...node, ...patch } : node))
  }

  function removeNode(id: string) {
    setNodes(n => n.filter(node => node.id !== id))
    setConnections(c => c.filter(con => con.from !== id && con.to !== id))
    setSelectedNodeId(null)
  }

  function connectNodes(from: string, to: string) {
    if (from === to) return
    if (connections.find(c => c.from === from && c.to === to)) return
    setConnections(c => [...c, { from, to }])
  }

  function removeConnection(from: string, to: string) {
    setConnections(c => c.filter(con => !(con.from === from && con.to === to)))
  }

  const selectedNode = nodes.find(n => n.id === selectedNodeId)

  return (
    <div className="p-6 space-y-6 text-white max-w-[1600px]">
      <div className="flex justify-between">
        <div>
          <h1 className="text-3xl font-bold">📖 Story Quest Designer</h1>
          <p className="text-sm text-zinc-400">Editor visual de questline tipo flowchart — exporta pra FTB Quests</p>
        </div>
        <button onClick={() => openEditor()} className="bg-purple-700 hover:bg-purple-600 px-4 py-2 rounded font-semibold">+ Nova Quest</button>
      </div>

      {!editing && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {items.map((q) => {
            let nodeCount = 0
            try { nodeCount = JSON.parse(q.nodesJson || '[]').length } catch {}
            return (
              <div key={q.id} className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 hover:border-purple-700">
                <div className="flex justify-between">
                  <h3 className="font-bold">{q.name}</h3>
                  <span className={`text-xs px-2 py-0.5 rounded ${q.published ? 'bg-emerald-900 text-emerald-300' : 'bg-zinc-800'}`}>{q.published ? '✓ pub' : 'rascunho'}</span>
                </div>
                <p className="text-xs text-purple-400">{q.archetype}</p>
                <p className="text-sm text-zinc-400 mt-1 line-clamp-2">{q.summary}</p>
                <div className="text-xs text-zinc-500 mt-2">{nodeCount} nós · {q.playersStarted}/{q.playersFinished} played/finished</div>
                <div className="flex gap-1 mt-3">
                  <button onClick={() => openEditor(q)} className="flex-1 bg-zinc-800 hover:bg-zinc-700 py-1.5 rounded text-sm">✎ Editar</button>
                  <a href={`/api/story-quests/${q.id}/export-ftb`} target="_blank" className="bg-purple-900 hover:bg-purple-700 px-2 py-1.5 rounded text-sm" title="Ver JSON">📋</a>
                  <button onClick={() => deploy.mutate(q.id)} className="bg-emerald-800 hover:bg-emerald-700 px-2 py-1.5 rounded text-sm" title="Deploy direto no server">🚀</button>
                  <button onClick={() => confirm('Apagar?') && remove.mutate(q.id)} className="bg-red-900 hover:bg-red-700 px-2 py-1.5 rounded text-sm">🗑</button>
                </div>
              </div>
            )
          })}
          {items.length === 0 && (
            <div className="col-span-full text-center py-16 text-zinc-500">
              <div className="text-6xl mb-3">📖</div>
              <p>Nenhuma quest criada.</p>
            </div>
          )}
        </div>
      )}

      {editing && (
        <div className="space-y-3">
          <div className="bg-zinc-950 border border-zinc-800 rounded-lg p-4 flex flex-wrap items-center gap-2">
            <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-xl font-bold flex-1 min-w-[200px]" placeholder="Nome da quest" />
            <select value={draft.archetype ?? 'main'} onChange={(e) => setDraft({ ...draft, archetype: e.target.value })}
              className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm">
              <option value="main">Main Story</option>
              <option value="side">Side Quest</option>
              <option value="event">Event</option>
              <option value="tutorial">Tutorial</option>
            </select>
            <label className="flex items-center gap-1 text-sm">
              <input type="checkbox" checked={draft.published ?? false} onChange={(e) => setDraft({ ...draft, published: e.target.checked })} />
              Publicado
            </label>
            <button onClick={addNode} className="bg-emerald-700 hover:bg-emerald-600 px-3 py-2 rounded text-sm">+ Nó</button>
            <button onClick={() => save.mutate({ ...draft, id: editing.id || undefined })} className="bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">💾 Salvar</button>
            <button onClick={() => { setEditing(null); setNodes([]); setConnections([]); setDraft({}) }} className="bg-zinc-700 px-4 py-2 rounded">✕</button>
          </div>

          <input value={draft.summary ?? ''} onChange={(e) => setDraft({ ...draft, summary: e.target.value })}
            placeholder="Resumo (mostrado no card)" className="w-full bg-zinc-900 border border-zinc-700 rounded px-3 py-2 text-sm" />

          <div className="grid grid-cols-1 lg:grid-cols-[1fr_350px] gap-4">
            {/* Canvas */}
            <div ref={canvasRef} className="bg-zinc-950 border border-zinc-800 rounded-lg p-2 relative overflow-auto select-none" style={{ minHeight: 500 }}>
              <svg className="absolute inset-0 pointer-events-none" style={{ width: '100%', height: '100%' }}>
                {connections.map((c, i) => {
                  const from = nodes.find(n => n.id === c.from)
                  const to = nodes.find(n => n.id === c.to)
                  if (!from || !to) return null
                  return (
                    <line key={i} x1={from.x + 80} y1={from.y + 30} x2={to.x + 80} y2={to.y + 30}
                      stroke="#a78bfa" strokeWidth={2} markerEnd="url(#arrow)" />
                  )
                })}
                <defs>
                  <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" fill="#a78bfa" />
                  </marker>
                </defs>
              </svg>
              {nodes.map(node => (
                <div key={node.id}
                  onMouseDown={(e) => {
                    const rect = canvasRef.current?.getBoundingClientRect()
                    if (rect) {
                      dragOffsetRef.current = {
                        dx: e.clientX - rect.left - node.x,
                        dy: e.clientY - rect.top - node.y,
                      }
                    }
                    setDraggingNode(node.id)
                    setSelectedNodeId(node.id)
                    e.preventDefault()
                  }}
                  className={`absolute cursor-move bg-gradient-to-br from-purple-900 to-zinc-900 border rounded-lg p-3 shadow-lg select-none ${
                    selectedNodeId === node.id ? 'border-amber-400 ring-2 ring-amber-400/50' : 'border-purple-700'
                  } ${draft.startNodeId === node.id ? 'ring-2 ring-emerald-500' : ''} ${draggingNode === node.id ? 'opacity-80' : ''}`}
                  style={{ left: node.x, top: node.y, width: 160 }}>
                  <div className="flex justify-between items-start">
                    <div className="font-bold text-sm truncate flex-1">{node.title}</div>
                    {draft.startNodeId === node.id && <span className="text-xs">⭐</span>}
                  </div>
                  <p className="text-xs text-zinc-400 mt-1 line-clamp-2 pointer-events-none">{node.description}</p>
                  <div className="text-xs text-zinc-500 mt-1 pointer-events-none">{node.tasks.length} tasks · {node.rewards.length} rewards</div>
                </div>
              ))}
            </div>

            {selectedNode && (
              <div className="bg-zinc-900 border border-zinc-700 rounded-lg p-4 space-y-3 sticky top-4 h-fit">
                <div className="flex justify-between">
                  <h3 className="font-bold">Nó: <code className="text-purple-400 text-xs">{selectedNode.id}</code></h3>
                  <div className="flex gap-1">
                    <button onClick={() => setDraft({ ...draft, startNodeId: selectedNode.id })}
                      className="text-xs bg-emerald-900 hover:bg-emerald-700 px-2 py-0.5 rounded">⭐ Start</button>
                    <button onClick={() => removeNode(selectedNode.id)} className="text-xs bg-red-900 hover:bg-red-700 px-2 py-0.5 rounded">🗑</button>
                  </div>
                </div>
                <input value={selectedNode.title} onChange={(e) => updateNode(selectedNode.id, { title: e.target.value })}
                  className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm" placeholder="Título" />
                <textarea value={selectedNode.description} onChange={(e) => updateNode(selectedNode.id, { description: e.target.value })}
                  placeholder="Descrição / texto narrativo"
                  rows={4} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />

                <div>
                  <div className="text-xs text-zinc-500 mb-1">Conectar a outro nó →</div>
                  <select onChange={(e) => { if (e.target.value) { connectNodes(selectedNode.id, e.target.value); e.target.value = '' } }}
                    className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm">
                    <option value="">Selecione...</option>
                    {nodes.filter(n => n.id !== selectedNode.id).map(n => <option key={n.id} value={n.id}>{n.title}</option>)}
                  </select>
                </div>
                {connections.filter(c => c.from === selectedNode.id).length > 0 && (
                  <div>
                    <div className="text-xs text-zinc-500 mb-1">Conexões saindo:</div>
                    {connections.filter(c => c.from === selectedNode.id).map((c, i) => {
                      const target = nodes.find(n => n.id === c.to)
                      return (
                        <div key={i} className="flex justify-between text-xs bg-zinc-950 px-2 py-1 rounded mt-1">
                          <span>→ {target?.title}</span>
                          <button onClick={() => removeConnection(c.from, c.to)} className="text-red-400">✕</button>
                        </div>
                      )
                    })}
                  </div>
                )}

                <div>
                  <div className="text-xs text-zinc-500 mb-1">Tasks (JSON)</div>
                  <textarea value={JSON.stringify(selectedNode.tasks, null, 2)}
                    onChange={(e) => { try { updateNode(selectedNode.id, { tasks: JSON.parse(e.target.value) }) } catch {} }}
                    rows={3} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
                </div>
                <div>
                  <div className="text-xs text-zinc-500 mb-1">Rewards (JSON)</div>
                  <textarea value={JSON.stringify(selectedNode.rewards, null, 2)}
                    onChange={(e) => { try { updateNode(selectedNode.id, { rewards: JSON.parse(e.target.value) }) } catch {} }}
                    rows={3} className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
