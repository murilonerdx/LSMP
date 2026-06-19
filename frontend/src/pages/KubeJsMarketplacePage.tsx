import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const headers = (): Record<string, string> => ({
  'Content-Type': 'application/json; charset=utf-8',
  Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}`,
})

type Script = {
  id: number; name: string; description: string; scope: string;
  status: string; authorName: string; downloads: number;
  sourceCode: string; updatedAt: string; reviewNote: string;
}

/** Marketplace de scripts KubeJS — submit + approve flow. */
export function KubeJsMarketplacePage() {
  const [filter, setFilter] = useState<string>('all')
  const [editing, setEditing] = useState<Script | null>(null)
  const [draft, setDraft] = useState<Partial<Script>>({})
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['kubejs', filter],
    queryFn: async () => {
      const url = filter === 'all' ? '/api/kubejs' : `/api/kubejs?status=${filter}`
      const r = await fetch(url, { headers: headers() })
      return r.json()
    },
  })

  const save = useMutation({
    mutationFn: async (body: Partial<Script>) => {
      const url = body.id ? `/api/kubejs/${body.id}` : '/api/kubejs'
      const r = await fetch(url, { method: body.id ? 'PUT' : 'POST', headers: headers(), body: JSON.stringify(body) })
      return r.json()
    },
    onSuccess: () => { setEditing(null); setDraft({}); qc.invalidateQueries({ queryKey: ['kubejs'] }) },
  })

  const approve = useMutation({
    mutationFn: async (id: number) => fetch(`/api/kubejs/${id}/approve`, { method: 'POST', headers: headers(), body: '{}' }).then(r => r.json()),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['kubejs'] }),
  })

  const reject = useMutation({
    mutationFn: async (id: number) => fetch(`/api/kubejs/${id}/reject`, { method: 'POST', headers: headers(), body: '{}' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['kubejs'] }),
  })

  const items: Script[] = Array.isArray(q.data?.content) ? q.data.content : []

  const TEMPLATES: Array<{ name: string; description: string; scope: string; sourceCode: string }> = [
    {
      name: 'Hello world tick',
      description: 'Loga "tick" no console a cada 20s. Pra testar setup.',
      scope: 'server',
      sourceCode: `// Server tick básico
ServerEvents.tick(event => {
  if (event.server.tickCount % 400 === 0) {
    console.log('[LSMP] tick ' + event.server.tickCount)
  }
})`
    },
    {
      name: 'Receita custom — Espada Divina',
      description: 'Recipe shaped pra espada OP usando matter do mod Liberthia.',
      scope: 'server',
      sourceCode: `ServerEvents.recipes(event => {
  event.shaped(
    Item.of('minecraft:netherite_sword', '{display:{Name:\\'{"text":"Espada Divina","color":"gold","italic":false}\\'}}'),
    ['X', 'X', 'Y'],
    { X: 'liberthia:active_dark_matter', Y: 'minecraft:stick' }
  )
})`
    },
    {
      name: 'Welcome message',
      description: 'Saudação personalizada quando player entra.',
      scope: 'server',
      sourceCode: `PlayerEvents.loggedIn(event => {
  const p = event.player
  p.tell([
    { text: 'Bem-vindo, ', color: 'gray' },
    { text: p.name.string, color: 'gold', bold: true },
    { text: ', a Liberthia!', color: 'gray' }
  ])
  p.server.runCommandSilent(\`playsound minecraft:ui.toast.challenge_complete master \${p.name.string}\`)
})`
    },
    {
      name: 'Boss kill broadcast',
      description: 'Quando matam Frostmaw (mowziesmobs), avisa o server.',
      scope: 'server',
      sourceCode: `EntityEvents.death(event => {
  const id = event.entity.type
  if (id === 'mowziesmobs:frostmaw') {
    event.server.runCommandSilent(\`say §6§l⚔ \${event.source.player?.name.string || 'Alguém'} derrotou Frostmaw!\`)
  }
})`
    },
    {
      name: 'Lightning ao colocar beacon',
      description: 'Efeito dramático em construções importantes.',
      scope: 'server',
      sourceCode: `BlockEvents.placed(event => {
  if (event.block.id === 'minecraft:beacon') {
    event.level.runCommandSilent(\`summon lightning_bolt \${event.block.x} \${event.block.y} \${event.block.z}\`)
  }
})`
    },
    {
      name: 'Anti-grief — TNT desativada',
      description: 'TNT não explode (player só griefa o próprio inv).',
      scope: 'server',
      sourceCode: `BlockEvents.placed(event => {
  if (event.block.id === 'minecraft:tnt') {
    event.cancel()
    event.player?.tell({ text: '✗ TNT desativada no servidor', color: 'red' })
  }
})`
    },
  ]
  function loadTemplate(t: typeof TEMPLATES[0]) {
    setEditing({ id: 0 } as Script)
    setDraft({ name: t.name, description: t.description, scope: t.scope, sourceCode: t.sourceCode, authorName: 'admin' })
  }

  return (
    <div className="p-6 space-y-6 text-white">
      <div className="flex justify-between">
        <h1 className="text-3xl font-bold">⚗️ KubeJS Scripts</h1>
        <div className="flex gap-2 text-sm">
          {['all', 'pending', 'approved', 'rejected'].map((s) => (
            <button key={s} onClick={() => setFilter(s)}
              className={`px-3 py-1 rounded ${filter === s ? 'bg-purple-700' : 'bg-zinc-800'}`}>{s}</button>
          ))}
          <button onClick={() => { setEditing({ id: 0 } as Script); setDraft({ scope: 'server', authorName: 'admin' }) }}
            className="bg-emerald-700 hover:bg-emerald-600 px-3 py-1 rounded">+ Novo script</button>
        </div>
      </div>

      {/* Templates — Examples */}
      <div className="bg-purple-950/30 border border-purple-800 rounded-lg p-4 space-y-3">
        <h3 className="font-bold flex items-center gap-2">📚 Templates prontos <span className="text-xs text-zinc-400 font-normal">— click pra começar com exemplo</span></h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
          {TEMPLATES.map((t, i) => (
            <button key={i} onClick={() => loadTemplate(t)}
              className="text-left bg-zinc-900 hover:bg-zinc-800 border border-zinc-700 rounded p-3">
              <div className="font-semibold">{t.name}</div>
              <p className="text-xs text-zinc-400 mt-1">{t.description}</p>
              <span className="text-xs text-purple-400 mt-2 inline-block">{t.scope}_scripts</span>
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-2">
        {items.map((s) => (
          <div key={s.id} className="bg-zinc-900 border border-zinc-800 rounded p-4">
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <h3 className="font-bold">{s.name}</h3>
                <p className="text-xs text-zinc-500">{s.scope}_scripts · {s.authorName} · ↓ {s.downloads}</p>
                <p className="text-sm text-zinc-400 mt-1">{s.description}</p>
              </div>
              <div className="flex flex-col items-end gap-1">
                <span className={`text-xs px-2 py-0.5 rounded ${
                  s.status === 'approved' ? 'bg-green-900 text-green-300' :
                  s.status === 'pending' ? 'bg-yellow-900 text-yellow-300' :
                  'bg-red-900 text-red-300'
                }`}>{s.status}</span>
              </div>
            </div>
            <div className="flex gap-2 mt-3 text-xs">
              <button onClick={() => { setEditing(s); setDraft({ ...s }) }} className="bg-zinc-800 hover:bg-zinc-700 px-2 py-1 rounded">Ver código</button>
              <a href={`/api/kubejs/${s.id}/download`} className="bg-zinc-800 hover:bg-zinc-700 px-2 py-1 rounded">↓ Download</a>
              {s.status === 'pending' && (
                <>
                  <button onClick={() => approve.mutate(s.id)} className="bg-green-800 hover:bg-green-700 px-2 py-1 rounded">✓ Aprovar</button>
                  <button onClick={() => reject.mutate(s.id)} className="bg-red-800 hover:bg-red-700 px-2 py-1 rounded">✗ Rejeitar</button>
                </>
              )}
            </div>
          </div>
        ))}
        {items.length === 0 && <div className="text-center text-zinc-500 py-12">Nenhum script.</div>}
      </div>

      {editing && (
        <div onClick={() => setEditing(null)} className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-4xl w-full p-6 space-y-3">
            <h2 className="text-xl font-bold">{draft.id ? 'Script' : 'Novo Script'}</h2>
            <input value={draft.name ?? ''} onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              placeholder="Nome" className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2" />
            <textarea value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              placeholder="Descrição (o que faz)" rows={2}
              className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm" />
            <select value={draft.scope ?? 'server'} onChange={(e) => setDraft({ ...draft, scope: e.target.value })}
              className="bg-zinc-950 border border-zinc-700 rounded px-3 py-2 text-sm">
              <option value="server">server_scripts</option>
              <option value="client">client_scripts</option>
              <option value="startup">startup_scripts</option>
            </select>
            <textarea value={draft.sourceCode ?? ''} onChange={(e) => setDraft({ ...draft, sourceCode: e.target.value })}
              placeholder="// Cole o código JS aqui" rows={18}
              className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 font-mono text-xs" />
            <div className="flex gap-2">
              <button onClick={() => save.mutate(draft)} className="ml-auto bg-emerald-700 hover:bg-emerald-600 px-4 py-2 rounded">Salvar</button>
              <button onClick={() => setEditing(null)} className="bg-zinc-700 px-4 py-2 rounded">Fechar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
