import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ChoiceDecisionDto } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'

/**
 * Decisões interativas V2 — backend-driven.
 *
 * O backend (ChoiceEngine) subscreve eventos de chat do mod. Quando o player
 * clica numa opção no chat, o mod publica um chat event com [LIB:id:idx], o
 * backend casa contra decisões ativas e aplica a consequência configurada.
 *
 * Vantagens da V2:
 *  - Funciona MESMO com a página fechada (engine roda no backend)
 *  - Estado ativo persiste no PostgreSQL — sobrevive a restart
 *  - Histórico fica no DB e não em memória local
 */

type Option = {
  text: string
  color: string
  action: 'title' | 'sound' | 'command' | 'effect'
  payload: string
  resultMessage: string
}

export function ChoicePage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['choice'], queryFn: api.choiceList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const decisions = q.data?.decisions ?? []
  const history = q.data?.history ?? []
  const [editing, setEditing] = useState<ChoiceDecisionDto | null>(null)

  const active = decisions.find((d) => d.activeTarget != null)

  const save = useMutation({
    mutationFn: api.choiceSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['choice'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({
    mutationFn: api.choiceDelete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['choice'] }),
  })
  const present = useMutation({
    mutationFn: (v: { id: string; target: string }) => api.choicePresent(v.id, v.target),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['choice'] }); toast.ok('🎴 apresentado') },
    onError: (e: any) => toast.err(e.message),
  })
  const cancel = useMutation({
    mutationFn: api.choiceCancel,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['choice'] }),
  })

  function newDecision() {
    setEditing({
      title: 'O Cruzamento',
      question: 'Você chega numa encruzilhada. A esquerda leva à montanha. A direita, à floresta. Qual caminho?',
      multiResponse: false,
      optionsJson: JSON.stringify([
        { text: 'Esquerda — Montanha', color: '§b', action: 'title', payload: '', resultMessage: '§b§l⛰ Você sobe...' },
        { text: 'Direita — Floresta', color: '§a', action: 'title', payload: '', resultMessage: '§a§l🌲 Você entra na mata...' },
      ] as Option[]),
    })
  }

  function parseOptions(d: ChoiceDecisionDto): Option[] {
    try { const a = JSON.parse(d.optionsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  }
  function parseResponses(d: ChoiceDecisionDto): string[] {
    if (!d.activeResponsesJson) return []
    try { const a = JSON.parse(d.activeResponsesJson); return Array.isArray(a) ? a : [] } catch { return [] }
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">⚖ Decisões Interativas</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            <span className="text-emerald-300 font-bold">Engine no backend</span> — clicks de chat processados 24/7.
          </p>
        </div>
        <button className="btn" onClick={newDecision}>+ Nova</button>
      </header>

      {active && (
        <div className="card-glow mb-4 !border-emerald-400/40">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="live-dot" />
            <span className="text-sm">Decisão ativa: <b>{active.title}</b></span>
            <span className="chip">target: {active.activeTarget === 'all' ? 'todos' : (players.find((p) => p.uuid === active.activeTarget)?.name ?? active.activeTarget)}</span>
            <span className="chip">{parseResponses(active).length} respostas</span>
            <button className="btn-danger btn-sm ml-auto" onClick={() => cancel.mutate(active.id!)}>⏹ Cancelar</button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {decisions.length === 0 && (
            <div className="card text-center py-12 col-span-full">
              <div className="text-5xl mb-3 opacity-50">⚖</div>
              <p className="text-liberthia-300/70">Nenhuma decisão ainda.</p>
            </div>
          )}
          {decisions.map((d) => {
            const opts = parseOptions(d)
            const isActive = d.activeTarget != null
            return (
              <div key={d.id} className={`card-glow ${isActive ? '!border-emerald-400/40' : ''}`}>
                <h3 className="font-bold mb-1">{d.title}</h3>
                <p className="text-xs text-liberthia-300/70 mb-2 line-clamp-2 min-h-[32px]">{renderMcText(d.question)}</p>
                <div className="space-y-1 mb-3">
                  {opts.map((o, i) => (
                    <div key={i} className="text-xs bg-liberthia-900/40 rounded p-1.5 truncate">
                      {renderMcText(`${o.color}» §l[${o.text}]`)} <span className="text-liberthia-300/50">→ {o.action}</span>
                    </div>
                  ))}
                </div>
                <div className="grid grid-cols-2 gap-1.5">
                  <select className="input text-xs"
                    value=""
                    onChange={(e) => { if (e.target.value) present.mutate({ id: d.id!, target: e.target.value }) }}
                    disabled={!!active}>
                    <option value="">📤 Apresentar pra...</option>
                    <option value="all">Todos</option>
                    {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
                  </select>
                  <div className="flex gap-1">
                    <button className="btn-ghost btn-sm flex-1" onClick={() => setEditing(d)}>✎</button>
                    <button className="btn-ghost btn-sm" onClick={() => del.mutate(d.id!)}>🗑</button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>

        <div className="card">
          <h3 className="font-bold mb-3">📜 Histórico (DB)</h3>
          <div className="space-y-1 max-h-96 overflow-y-auto text-xs">
            {history.length === 0 && <p className="italic text-liberthia-300/50">— sem escolhas —</p>}
            {history.map((h) => (
              <div key={h.id} className="p-1.5 rounded bg-liberthia-900/40">
                <div className="flex gap-2">
                  <span className="text-liberthia-300/40">{new Date(h.ts).toLocaleTimeString()}</span>
                  <b>{h.playerName}</b>
                </div>
                <div className="text-liberthia-300/70 text-[10px]">
                  {h.decisionTitle} → <span className="text-emerald-300">{h.optionText}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {editing && <DecisionEditor d={editing} onSave={(d) => save.mutate(d, { onSuccess: () => setEditing(null) })} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function DecisionEditor({ d, onSave, onCancel }: { d: ChoiceDecisionDto; onSave: (d: ChoiceDecisionDto) => void; onCancel: () => void }) {
  const [dec, setDec] = useState<ChoiceDecisionDto>(d)
  const opts: Option[] = (() => {
    try { const a = JSON.parse(dec.optionsJson); return Array.isArray(a) ? a : [] } catch { return [] }
  })()
  function setOpts(o: Option[]) { setDec({ ...dec, optionsJson: JSON.stringify(o) }) }
  function updateOpt(i: number, p: Partial<Option>) {
    setOpts(opts.map((o, j) => j === i ? { ...o, ...p } : o))
  }
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">⚖ Decisão</h3>

        <input className="input mb-2 font-bold" value={dec.title} onChange={(e) => setDec({ ...dec, title: e.target.value })} />
        <MinecraftFormatter value={dec.question} onChange={(v) => setDec({ ...dec, question: v })} rows={3} maxChars={300} />

        <label className="flex items-center gap-2 text-xs mt-3 mb-3">
          <input type="checkbox" checked={dec.multiResponse} onChange={(e) => setDec({ ...dec, multiResponse: e.target.checked })} />
          Permitir múltiplas respostas
        </label>

        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">📋 Opções ({opts.length})</h4>
          {opts.length < 6 && (
            <button className="btn-ghost btn-sm" onClick={() => setOpts([...opts, { text: 'Opção', color: '§f', action: 'title', payload: '', resultMessage: '§7...' }])}>+ Opção</button>
          )}
        </div>

        <div className="space-y-2">
          {opts.map((o, i) => (
            <div key={i} className="border border-liberthia-500/20 rounded-xl p-2 bg-liberthia-900/40">
              <div className="flex gap-2 mb-2">
                <input className="input w-16 font-mono text-xs" value={o.color} onChange={(e) => updateOpt(i, { color: e.target.value })} />
                <input className="input flex-1 text-xs" value={o.text} onChange={(e) => updateOpt(i, { text: e.target.value })} />
                <button className="btn-ghost btn-sm" onClick={() => setOpts(opts.filter((_, j) => j !== i))}>🗑</button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <select className="input text-xs" value={o.action} onChange={(e) => updateOpt(i, { action: e.target.value as any })}>
                  <option value="title">📺 Title</option>
                  <option value="sound">🔊 Sound</option>
                  <option value="command">⌨ Comando</option>
                  <option value="effect">⚗ Effect (id dur amp)</option>
                </select>
                <input className="input text-xs font-mono" value={o.payload} onChange={(e) => updateOpt(i, { payload: e.target.value })}
                  placeholder={o.action === 'sound' ? 'minecraft:...' : o.action === 'command' ? 'cmd ({player})' : o.action === 'effect' ? 'minecraft:speed 60 1' : ''} />
              </div>
              <input className="input text-xs font-mono mt-2" value={o.resultMessage}
                onChange={(e) => updateOpt(i, { resultMessage: e.target.value })} placeholder="Title quando escolhem" />
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end mt-4">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(dec)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
