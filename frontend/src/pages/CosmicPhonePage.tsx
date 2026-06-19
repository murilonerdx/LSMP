import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'
import { useKvState } from '../lib/kvState'

/**
 * Cosmic Phone Call — simula telefonema de NPC pro player. Mensagens chegam
 * no chat espaçadas com sons de digitação (typing dots) entre cada uma.
 * Igual SMS recebendo — vibe horror-cosmico de "alguém te chamando do nada".
 */

type Message = {
  text: string
  delay: number      // ms antes de enviar
  typingMs: number   // ms mostrando "..." antes
  speaker: string    // §-codes incluídos
  withSound?: boolean
}

type Call = {
  id: string
  name: string
  emoji: string
  callerName: string  // ex: "§5§l? ? ?"
  ringtone: string    // sound id
  messages: Message[]
  updated: number
}

const DEFAULT_CALLS: Call[] = [
  {
    id: 'c_unknown',
    name: 'Chamada Desconhecida',
    emoji: '📞',
    callerName: '§5§l? ? ?',
    ringtone: 'minecraft:block.bell.use',
    messages: [
      { text: '...você está aí?', delay: 2000, typingMs: 1500, speaker: '§5§l[? ? ?]§r ' },
      { text: '§eEu sei.§r', delay: 4000, typingMs: 2000, speaker: '§5§l[? ? ?]§r ', withSound: true },
      { text: 'Eu sei o que você fez.', delay: 3500, typingMs: 1800, speaker: '§5§l[? ? ?]§r ' },
      { text: '...', delay: 5000, typingMs: 3000, speaker: '§5§l[? ? ?]§r ' },
      { text: '§lOLHE PRA TRÁS.§r', delay: 3000, typingMs: 1000, speaker: '§5§l[? ? ?]§r ', withSound: true },
    ],
    updated: Date.now(),
  },
]

export function CosmicPhonePage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [calls, setCalls] = useKvState<Call[]>('cosmic_calls', DEFAULT_CALLS)
  const [active, setActive] = useState<{ callId: string; player: string; idx: number } | null>(null)
  const [editing, setEditing] = useState<Call | null>(null)
  const cancelRef = useRef(false)

  async function ring(call: Call, playerUuid: string) {
    const player = players.find((p) => p.uuid === playerUuid); if (!player) return
    setActive({ callId: call.id, player: playerUuid, idx: 0 })
    cancelRef.current = false

    try {
      // Toca ringtone + título tipo "incoming call"
      for (let r = 0; r < 3; r++) {
        if (cancelRef.current) break
        await api.sound(playerUuid, call.ringtone, 1, 1)
        await api.title(playerUuid, `${call.emoji} §e§lINCOMING`, call.callerName, 5, 30, 5)
        await new Promise((r) => setTimeout(r, 700))
      }
      // Anuncia chamada conectada
      await api.command(`tellraw ${player.name} ${JSON.stringify({ text: '\n§7§o— chamada conectada —§r\n' })}`, 'phone')
      await new Promise((r) => setTimeout(r, 1500))

      // Manda mensagens
      for (let i = 0; i < call.messages.length; i++) {
        if (cancelRef.current) break
        setActive((cur) => cur ? { ...cur, idx: i } : null)
        const m = call.messages[i]
        // Delay entre mensagens
        await new Promise((r) => setTimeout(r, m.delay))
        if (cancelRef.current) break
        // Typing indicator (subtitle "...")
        if (m.typingMs > 0) {
          await api.title(playerUuid, ' ', `${m.speaker}§7§o...digitando...`, 0, Math.floor(m.typingMs / 50), 0)
          // Som leve de keyboard
          for (let t = 0; t < Math.min(4, Math.floor(m.typingMs / 250)); t++) {
            if (cancelRef.current) break
            await api.sound(playerUuid, 'minecraft:block.note_block.hat', 0.3, 1.5 + Math.random() * 0.3)
            await new Promise((r) => setTimeout(r, 250))
          }
        }
        if (cancelRef.current) break
        // Manda a mensagem no chat
        await api.command(`tellraw ${player.name} ${JSON.stringify({ text: m.speaker + m.text })}`, 'phone')
        if (m.withSound) {
          await api.sound(playerUuid, 'minecraft:entity.warden.heartbeat', 0.7, 0.6)
        }
      }

      // Encerramento
      if (!cancelRef.current) {
        await new Promise((r) => setTimeout(r, 2000))
        await api.command(`tellraw ${player.name} ${JSON.stringify({ text: '\n§7§o— chamada encerrada —§r\n' })}`, 'phone')
        await api.sound(playerUuid, 'minecraft:block.note_block.bit', 0.5, 0.5)
      }
    } catch (e: any) { toast.err(e.message) }
    setActive(null)
  }

  function newCall() {
    setEditing({
      id: `call_${Date.now().toString(36)}`,
      name: 'Nova Chamada',
      emoji: '📞',
      callerName: '§5§l? ? ?',
      ringtone: 'minecraft:block.bell.use',
      messages: [{ text: 'olá?', delay: 1500, typingMs: 1500, speaker: '§5§l[? ? ?]§r ' }],
      updated: Date.now(),
    })
  }

  function commit(c: Call) {
    setCalls((cur) => {
      const i = cur.findIndex((x) => x.id === c.id)
      if (i >= 0) { const n = [...cur]; n[i] = c; return n }
      return [...cur, c]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">📞 Cosmic Phone Call</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Simula telefonema/SMS de NPC: ringtone → typing indicator → mensagens espaçadas no chat.
            Vibe horror-cosmico de "alguém te chamando do nada".
          </p>
        </div>
        <div className="flex gap-2">
          {active && <button className="btn-danger btn-sm" onClick={() => { cancelRef.current = true }}>📵 Desligar</button>}
          <button className="btn" onClick={newCall}>+ Nova Chamada</button>
        </div>
      </header>

      {active && (
        <div className="card-glow mb-4 !border-emerald-400/40 !bg-emerald-500/5">
          <div className="flex items-center gap-3">
            <span className="text-3xl animate-pulse">📞</span>
            <div className="flex-1">
              <div className="font-bold">{calls.find((c) => c.id === active.callId)?.name}</div>
              <div className="text-xs">Mensagem {active.idx + 1} / {calls.find((c) => c.id === active.callId)?.messages.length}</div>
            </div>
            <span className="live-dot" />
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {calls.map((c) => (
          <div key={c.id} className="card-glow">
            <div className="text-5xl text-center mb-2">{c.emoji}</div>
            <div className="font-bold text-center mb-1">{c.name}</div>
            <div className="text-xs text-center text-liberthia-300/60 mb-3">{renderMcText(c.callerName)}</div>
            <div className="text-[10px] text-liberthia-300/50 mb-3 text-center">
              {c.messages.length} mensagens
            </div>
            <select className="input text-xs mb-2"
              onChange={(e) => { if (e.target.value) { ring(c, e.target.value); e.target.value = '' } }}
              disabled={!!active}>
              <option value="">📞 Ligar pra...</option>
              {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
            </select>
            <div className="grid grid-cols-2 gap-1">
              <button className="btn-ghost btn-sm" onClick={() => setEditing(c)}>✎</button>
              <button className="btn-ghost btn-sm" onClick={() => setCalls((cur) => cur.filter((x) => x.id !== c.id))}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {editing && <CallEditor call={editing} onSave={commit} onCancel={() => setEditing(null)} />}
    </div>
  )
}

function CallEditor({ call, onSave, onCancel }: { call: Call; onSave: (c: Call) => void; onCancel: () => void }) {
  const [c, setC] = useState<Call>(call)
  function updateMsg(i: number, p: Partial<Message>) {
    setC({ ...c, messages: c.messages.map((m, j) => j === i ? { ...m, ...p } : m) })
  }
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">📞 Chamada</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <input className="input text-2xl text-center" value={c.emoji} onChange={(e) => setC({ ...c, emoji: e.target.value })} />
          <input className="input font-bold" value={c.name} onChange={(e) => setC({ ...c, name: e.target.value })} />
        </div>
        <input className="input mb-2 text-sm font-mono" value={c.callerName} onChange={(e) => setC({ ...c, callerName: e.target.value })} placeholder="§5§l[? ? ?]" />
        <input className="input mb-4 text-xs font-mono" value={c.ringtone} onChange={(e) => setC({ ...c, ringtone: e.target.value })} placeholder="sound do ringtone" />

        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">💬 Mensagens ({c.messages.length})</h4>
          <button className="btn-ghost btn-sm" onClick={() => setC({ ...c, messages: [...c.messages, { text: '...', delay: 2000, typingMs: 1500, speaker: c.messages[0]?.speaker ?? '§5§l[? ? ?]§r ' }] })}>+ Msg</button>
        </div>

        <div className="space-y-2 mb-4">
          {c.messages.map((m, i) => (
            <div key={i} className="border border-liberthia-500/20 rounded-xl p-2 bg-liberthia-900/40">
              <div className="flex items-center gap-2 mb-2">
                <span className="badge badge-purple">#{i + 1}</span>
                <input className="input text-xs font-mono flex-1" value={m.speaker} onChange={(e) => updateMsg(i, { speaker: e.target.value })} placeholder="speaker prefix" />
                <button className="btn-ghost btn-sm" onClick={() => setC({ ...c, messages: c.messages.filter((_, j) => j !== i) })}>🗑</button>
              </div>
              <MinecraftFormatter value={m.text} onChange={(v) => updateMsg(i, { text: v })} rows={1} maxChars={150} showCounter={false} />
              <div className="grid grid-cols-3 gap-2 mt-2 text-xs">
                <div><label className="label">Delay (ms)</label><input type="number" className="input" value={m.delay} step={500} onChange={(e) => updateMsg(i, { delay: Number(e.target.value) })} /></div>
                <div><label className="label">Typing (ms)</label><input type="number" className="input" value={m.typingMs} step={500} onChange={(e) => updateMsg(i, { typingMs: Number(e.target.value) })} /></div>
                <div className="flex items-end">
                  <label className="flex items-center gap-1"><input type="checkbox" checked={m.withSound ?? false} onChange={(e) => updateMsg(i, { withSound: e.target.checked })} /> 💔 heartbeat</label>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(c)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
