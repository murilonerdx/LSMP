import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ItemDef } from '../lib/api'
import { InventoryGrid } from '../components/InventoryGrid'
import { ItemPicker } from '../components/ItemPicker'
import { EnchantPicker, SelectedEnch } from '../components/EnchantPicker'
import { MatterEditor } from '../components/MatterEditor'

export function PlayerDetail() {
  const { uuid } = useParams<{ uuid: string }>()
  if (!uuid) return <p>UUID missing</p>

  const qc = useQueryClient()
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players })
  const player = playersQ.data?.find(p => p.uuid === uuid)

  const [tab, setTab] = useState<'inv' | 'give' | 'matter' | 'actions'>('inv')

  return (
    <div>
      <Link to="/" className="btn-ghost text-xs mb-3 inline-block">← Voltar</Link>
      <div className="card mb-4">
        <div className="flex items-center gap-4">
          <img src={`https://mc-heads.net/avatar/${uuid}/64`} className="w-16 h-16 rounded" />
          <div>
            <h1 className="text-2xl font-bold">{player?.name ?? 'offline'}</h1>
            <p className="text-sm text-liberthia-300/80 font-mono">{uuid}</p>
            {player && (
              <p className="text-sm">
                {player.dimension.replace('minecraft:', '')} · ❤ {player.health.toFixed(0)}/{player.maxHealth.toFixed(0)} · L{player.level}
              </p>
            )}
          </div>
        </div>
      </div>

      <div className="flex gap-2 mb-4 border-b border-liberthia-600 pb-1">
        <Tab id="inv" tab={tab} setTab={setTab}>🎒 Inventário</Tab>
        <Tab id="give" tab={tab} setTab={setTab}>🎁 Dar Item</Tab>
        <Tab id="matter" tab={tab} setTab={setTab}>⚛ Matéria</Tab>
        <Tab id="actions" tab={tab} setTab={setTab}>⚙ Ações</Tab>
      </div>

      {tab === 'inv' && <InventoryGrid uuid={uuid} />}
      {tab === 'give' && <GiveItemPanel uuid={uuid} onGiven={() => qc.invalidateQueries({ queryKey: ['inventory', uuid] })} />}
      {tab === 'matter' && <MatterEditor uuid={uuid} />}
      {tab === 'actions' && <ActionsPanel uuid={uuid} />}
    </div>
  )
}

function Tab({ id, tab, setTab, children }: any) {
  return (
    <button
      className={`px-3 py-1.5 rounded-t text-sm transition ${
        tab === id ? 'bg-liberthia-500 text-white' : 'hover:bg-liberthia-700'
      }`}
      onClick={() => setTab(id)}
    >
      {children}
    </button>
  )
}

function GiveItemPanel({ uuid, onGiven }: { uuid: string; onGiven: () => void }) {
  const [item, setItem] = useState<ItemDef | null>(null)
  const [count, setCount] = useState(1)
  const [enchs, setEnchs] = useState<SelectedEnch[]>([])
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)

  const send = async () => {
    if (!item) { setMsg('selecione um item'); return }
    setBusy(true); setMsg(null)
    try {
      await api.give(uuid, item.id, count, enchs)
      setMsg(`✓ Enviado: ${count}× ${item.name}${enchs.length ? ` com ${enchs.length} encantamento(s)` : ''}`)
      onGiven()
    } catch (e: any) {
      setMsg(`✗ ${e.message}`)
    }
    setBusy(false)
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="space-y-3">
        <ItemPicker selected={item?.id} onSelect={setItem} />
        <div className="card flex items-end gap-2">
          <div className="flex-1">
            <div className="label mb-1">Quantidade</div>
            <input type="number" className="input" value={count} min={1} max={64} onChange={e => setCount(parseInt(e.target.value) || 1)} />
          </div>
          <button className="btn flex-1" onClick={send} disabled={busy || !item}>
            {busy ? '...' : `Dar ${count}× ${item?.name ?? '???'}`}
          </button>
        </div>
        {msg && <div className={`card ${msg.startsWith('✓') ? 'text-green-400' : 'text-red-400'}`}>{msg}</div>}
      </div>
      <EnchantPicker selected={enchs} onChange={setEnchs} />
    </div>
  )
}

function ActionsPanel({ uuid }: { uuid: string }) {
  const [tpX, setTpX] = useState('0'); const [tpY, setTpY] = useState('100'); const [tpZ, setTpZ] = useState('0')
  const [tpDim, setTpDim] = useState('minecraft:overworld')
  const [reason, setReason] = useState('Banido pelo admin')
  const [effect, setEffect] = useState('minecraft:speed')
  const [duration, setDuration] = useState(600)
  const [amplifier, setAmplifier] = useState(1)
  const [log, setLog] = useState<string[]>([])
  const append = (s: string) => setLog(L => [s, ...L].slice(0, 20))

  const tp = async () => {
    try { await api.teleport(uuid, +tpX, +tpY, +tpZ, tpDim); append(`✓ TP pra ${tpX},${tpY},${tpZ} (${tpDim})`) }
    catch (e: any) { append(`✗ ${e.message}`) }
  }
  const kick = async () => {
    try { await api.kick(uuid, reason); append(`✓ Kickado: "${reason}"`) }
    catch (e: any) { append(`✗ ${e.message}`) }
  }
  const eff = async () => {
    try { await api.effect(uuid, effect, duration, amplifier); append(`✓ Effect ${effect} ${duration}t lvl${amplifier+1}`) }
    catch (e: any) { append(`✗ ${e.message}`) }
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="card space-y-2">
        <h3 className="font-bold">🚀 Teleport</h3>
        <div className="grid grid-cols-3 gap-2">
          <div><div className="label">X</div><input className="input" value={tpX} onChange={e=>setTpX(e.target.value)} /></div>
          <div><div className="label">Y</div><input className="input" value={tpY} onChange={e=>setTpY(e.target.value)} /></div>
          <div><div className="label">Z</div><input className="input" value={tpZ} onChange={e=>setTpZ(e.target.value)} /></div>
        </div>
        <div><div className="label">Dimensão</div><input className="input" value={tpDim} onChange={e=>setTpDim(e.target.value)} /></div>
        <button className="btn w-full" onClick={tp}>Teleportar</button>
      </div>

      <div className="card space-y-2">
        <h3 className="font-bold">⚡ Effect</h3>
        <input className="input" placeholder="minecraft:speed" value={effect} onChange={e=>setEffect(e.target.value)} />
        <div className="grid grid-cols-2 gap-2">
          <div><div className="label">Duração (ticks)</div><input type="number" className="input" value={duration} onChange={e=>setDuration(+e.target.value)} /></div>
          <div><div className="label">Amplifier</div><input type="number" className="input" value={amplifier} onChange={e=>setAmplifier(+e.target.value)} /></div>
        </div>
        <button className="btn w-full" onClick={eff}>Aplicar Effect</button>
      </div>

      <div className="card space-y-2">
        <h3 className="font-bold text-red-400">🚪 Kick</h3>
        <input className="input" value={reason} onChange={e=>setReason(e.target.value)} />
        <button className="btn-danger w-full" onClick={kick}>Kickar</button>
      </div>

      <FreezePanel uuid={uuid} append={append} />

      <div className="card">
        <h3 className="font-bold mb-1">📜 Log</h3>
        <div className="text-xs font-mono space-y-0.5 max-h-40 overflow-y-auto">
          {log.length === 0 ? <p className="opacity-50 italic">vazio</p> : log.map((l, i) => <div key={i} className={l.startsWith('✓') ? 'text-green-400' : 'text-red-400'}>{l}</div>)}
        </div>
      </div>
    </div>
  )
}

function FreezePanel({ uuid, append }: { uuid: string; append: (s: string) => void }) {
  const [status, setStatus] = useState<{ frozen: boolean; moveAttempts?: number } | null>(null)
  // poll status cada 2s pra ver se congelou + ataque counter
  useEffect(() => {
    let alive = true
    const tick = async () => {
      try {
        const s = await api.freezeStatus(uuid)
        if (alive) setStatus(s)
      } catch { /* offline */ }
    }
    tick()
    const id = setInterval(tick, 2000)
    return () => { alive = false; clearInterval(id) }
  }, [uuid])

  const freeze = async () => {
    try { await api.freeze(uuid); append('❄ Congelado'); setStatus({ frozen: true }) }
    catch (e: any) { append(`✗ ${e.message}`) }
  }
  const unfreeze = async () => {
    try { await api.unfreeze(uuid); append('🔥 Descongelado'); setStatus({ frozen: false }) }
    catch (e: any) { append(`✗ ${e.message}`) }
  }

  return (
    <div className={`card space-y-2 ${status?.frozen ? '!border-cyan-400/60 !bg-cyan-500/10 pulse-glow' : ''}`}>
      <h3 className="font-bold text-cyan-300">❄ Freeze</h3>
      <p className="text-[10px] text-liberthia-300/70">
        Trava o player no lugar. <b>Não consegue se mover, atacar, abrir inventário</b>.
        Cada tentativa de movimento causa dano crescente (até morte).
      </p>
      {status?.frozen ? (
        <>
          <div className="text-xs text-cyan-300 font-mono">
            🟢 CONGELADO · tentativas: <b>{status.moveAttempts ?? 0}</b>
          </div>
          <button className="btn-success w-full" onClick={unfreeze}>🔥 Descongelar</button>
        </>
      ) : (
        <button className="btn w-full" onClick={freeze}>❄ Congelar</button>
      )}
    </div>
  )
}
