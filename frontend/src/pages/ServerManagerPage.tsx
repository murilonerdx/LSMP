import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Server Manager — controle central do servidor:
 *  - Live status (TPS, uptime, players, dimensões)
 *  - Save world manual
 *  - Backup manual via mod
 *  - Broadcast custom message
 *  - Difficulty + time + weather rápido
 *  - Performance graph (TPS sparkline ao longo do tempo)
 *  - Server stop/restart (via comando)
 */

const KEY = 'liberthia.servermanager.tps'

type TpsSample = { ts: number; tps: number; players: number }

export function ServerManagerPage() {
  const serverQ = useQuery({ queryKey: ['server-info'], queryFn: api.serverInfo, refetchInterval: 2000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 3000 })
  const opsQ = useQuery({ queryKey: ['operators'], queryFn: api.operators, refetchInterval: 30_000 })
  const players = playersQ.data ?? []
  const info = serverQ.data
  const ops = opsQ.data?.operators ?? []

  const [tpsLog, setTpsLog] = useState<TpsSample[]>(() => {
    try { return JSON.parse(sessionStorage.getItem(KEY) ?? '[]') } catch { return [] }
  })
  const [broadcastMsg, setBroadcastMsg] = useState('')
  const [confirmStop, setConfirmStop] = useState(false)
  const canvasRef = useRef<HTMLCanvasElement>(null)

  // Acumula TPS samples
  useEffect(() => {
    if (!info?.tps) return
    setTpsLog((cur) => {
      const next = [...cur, { ts: Date.now(), tps: info.tps, players: info.playerCount }]
      const cutoff = Date.now() - 30 * 60 * 1000  // 30 min retention
      const trimmed = next.filter((s) => s.ts >= cutoff)
      sessionStorage.setItem(KEY, JSON.stringify(trimmed))
      return trimmed
    })
  }, [info?.tps, info?.playerCount])

  // Render sparkline
  useEffect(() => {
    const c = canvasRef.current; if (!c) return
    const ctx = c.getContext('2d'); if (!ctx) return
    const W = c.clientWidth, H = 140
    c.width = W; c.height = H
    ctx.fillStyle = '#05010c'; ctx.fillRect(0, 0, W, H)
    if (tpsLog.length < 2) {
      ctx.fillStyle = 'rgba(216,194,255,0.5)'
      ctx.font = '11px monospace'
      ctx.textAlign = 'center'
      ctx.fillText('coletando dados...', W / 2, H / 2)
      return
    }
    // Eixos
    const minTs = tpsLog[0].ts, maxTs = tpsLog[tpsLog.length - 1].ts
    const tRange = Math.max(1, maxTs - minTs)
    // TPS scale 0-20
    for (let v = 0; v <= 20; v += 5) {
      const y = H - 10 - (v / 20) * (H - 20)
      ctx.strokeStyle = v === 20 ? 'rgba(16,185,129,0.3)' : 'rgba(216,194,255,0.08)'
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke()
      ctx.fillStyle = 'rgba(216,194,255,0.4)'
      ctx.font = '9px monospace'
      ctx.textAlign = 'left'
      ctx.fillText(`${v}`, 2, y - 1)
    }
    // TPS line
    ctx.strokeStyle = '#a78bfa'
    ctx.lineWidth = 2
    ctx.beginPath()
    tpsLog.forEach((s, i) => {
      const x = ((s.ts - minTs) / tRange) * W
      const y = H - 10 - (Math.min(20, s.tps) / 20) * (H - 20)
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
    })
    ctx.stroke()
    // Fill below
    ctx.fillStyle = 'rgba(167,139,250,0.15)'
    ctx.lineTo(W, H); ctx.lineTo(0, H); ctx.closePath(); ctx.fill()
    // Players bar at bottom
    ctx.strokeStyle = '#06b6d4'
    ctx.lineWidth = 1
    ctx.beginPath()
    const maxP = Math.max(1, ...tpsLog.map((s) => s.players))
    tpsLog.forEach((s, i) => {
      const x = ((s.ts - minTs) / tRange) * W
      const y = H - 4 - (s.players / maxP) * 25
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
    })
    ctx.stroke()
  }, [tpsLog])

  async function broadcast() {
    if (!broadcastMsg.trim()) return
    try { await api.broadcast(broadcastMsg); toast.ok('📡 broadcast enviado'); setBroadcastMsg('') }
    catch (e: any) { toast.err(e.message) }
  }

  async function saveWorld() {
    try { await api.saveAll(); toast.ok('💾 mundo salvo') }
    catch (e: any) { toast.err(e.message) }
  }

  async function backupNow() {
    try { await api.backup(); toast.ok('💾 backup iniciado') }
    catch (e: any) { toast.err(e.message) }
  }

  async function healAll() {
    try { await api.healAll(); toast.ok('❤ todos curados') }
    catch (e: any) { toast.err(e.message) }
  }

  async function setTime(t: number) {
    try { await api.worldTime(t); toast.ok('🕐 tempo ajustado') }
    catch (e: any) { toast.err(e.message) }
  }

  async function setWeather(w: 'clear' | 'rain' | 'thunder') {
    try { await api.worldWeather(w, 6000); toast.ok(`🌤 ${w}`) }
    catch (e: any) { toast.err(e.message) }
  }

  async function setDifficulty(d: 'peaceful' | 'easy' | 'normal' | 'hard') {
    try { await api.worldDifficulty(d); toast.ok(`⚔ ${d}`) }
    catch (e: any) { toast.err(e.message) }
  }

  async function snapshotAll() {
    try { await api.snapshotRunNow(); toast.ok('📸 snapshot global') }
    catch (e: any) { toast.err(e.message) }
  }

  async function stopServer() {
    if (!confirmStop) return setConfirmStop(true)
    try {
      await api.broadcast('§4§l[ADMIN]§r §c· O servidor vai parar em §l5 segundos§r §c·')
      setTimeout(async () => {
        try { await api.command('stop', 'server-mgr') } catch {}
      }, 5000)
      setConfirmStop(false)
      toast.ok('🛑 stop em 5s')
    } catch (e: any) { toast.err(e.message); setConfirmStop(false) }
  }

  const avgTps = tpsLog.length > 0 ? tpsLog.reduce((a, v) => a + v.tps, 0) / tpsLog.length : 0
  const tpsHealth = info?.tps ?? 20

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🖥 Server Manager</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Controle central: status, save, backup, broadcast, time/weather, performance, stop.
          </p>
        </div>
      </header>

      {/* Top stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <Stat emoji="⚡" label="TPS" value={info?.tps?.toFixed(1) ?? '—'} sub={`avg ${avgTps.toFixed(1)}`} color={tpsHealth >= 19 ? 'green' : tpsHealth >= 15 ? 'amber' : 'red'} />
        <Stat emoji="👥" label="Players" value={`${info?.playerCount ?? 0}/${info?.maxPlayers ?? 0}`} sub={`${ops.length} OPs`} />
        <Stat emoji="🕐" label="Tick" value={info?.tickCount?.toString() ?? '—'} sub="server tick" />
        <Stat emoji="🌍" label="Dims" value={`${info?.dimensions?.length ?? 0}`} sub="carregadas" />
      </div>

      {/* TPS graph */}
      <div className="card-glow mb-4">
        <h3 className="font-bold mb-2 flex items-center gap-2">📈 Performance (últimos 30min)</h3>
        <canvas ref={canvasRef} style={{ width: '100%', height: 140 }} />
        <div className="grid grid-cols-3 text-[10px] text-liberthia-300/60 mt-1">
          <span><span className="inline-block w-3 h-1 bg-purple-400 align-middle mr-1" /> TPS (0-20)</span>
          <span className="text-center"><span className="inline-block w-3 h-1 bg-cyan-400 align-middle mr-1" /> Players</span>
          <span className="text-right">amostras: {tpsLog.length}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Save & backup */}
        <div className="card-glow">
          <h3 className="font-bold mb-2">💾 Persistência</h3>
          <div className="space-y-2">
            <button className="btn w-full" onClick={saveWorld}>💾 Save World</button>
            <button className="btn-ghost w-full" onClick={backupNow}>📦 Backup zip</button>
            <button className="btn-ghost w-full" onClick={snapshotAll}>📸 Snapshot global</button>
          </div>
        </div>

        {/* Broadcast */}
        <div className="card-glow">
          <h3 className="font-bold mb-2">📡 Broadcast</h3>
          <textarea className="input text-sm mb-2" rows={2} value={broadcastMsg} onChange={(e) => setBroadcastMsg(e.target.value)} placeholder="§5§lMensagem épica..." />
          <button className="btn w-full" onClick={broadcast} disabled={!broadcastMsg.trim()}>📡 Enviar</button>
        </div>

        {/* World controls */}
        <div className="card-glow">
          <h3 className="font-bold mb-2">🌍 Mundo</h3>
          <label className="label">Tempo</label>
          <div className="grid grid-cols-4 gap-1 mb-3">
            <button className="btn-ghost btn-sm" onClick={() => setTime(0)}>☀ 0</button>
            <button className="btn-ghost btn-sm" onClick={() => setTime(6000)}>🌞 6k</button>
            <button className="btn-ghost btn-sm" onClick={() => setTime(13000)}>🌙 13k</button>
            <button className="btn-ghost btn-sm" onClick={() => setTime(18000)}>🌌 18k</button>
          </div>
          <label className="label">Clima</label>
          <div className="grid grid-cols-3 gap-1 mb-3">
            <button className="btn-ghost btn-sm" onClick={() => setWeather('clear')}>☀</button>
            <button className="btn-ghost btn-sm" onClick={() => setWeather('rain')}>🌧</button>
            <button className="btn-ghost btn-sm" onClick={() => setWeather('thunder')}>⛈</button>
          </div>
          <label className="label">Dificuldade</label>
          <div className="grid grid-cols-4 gap-1">
            <button className="btn-ghost btn-sm" onClick={() => setDifficulty('peaceful')}>P</button>
            <button className="btn-ghost btn-sm" onClick={() => setDifficulty('easy')}>E</button>
            <button className="btn-ghost btn-sm" onClick={() => setDifficulty('normal')}>N</button>
            <button className="btn-ghost btn-sm" onClick={() => setDifficulty('hard')}>H</button>
          </div>
        </div>

        {/* Players bulk */}
        <div className="card-glow">
          <h3 className="font-bold mb-2">👥 Bulk players ({players.length})</h3>
          <div className="space-y-2">
            <button className="btn w-full" onClick={healAll}>❤ Heal All</button>
            <button className="btn-ghost w-full" onClick={() => api.command('xp set @a 0 levels', 'srv-mgr').then(() => toast.ok('XP zerado'))}>0 XP All</button>
            <button className="btn-ghost w-full" onClick={() => api.command('clear @a', 'srv-mgr').then(() => toast.ok('inventários limpos'))}>🗑 Clear All Inv</button>
            <button className="btn-ghost w-full" onClick={() => api.command('tp @a @r', 'srv-mgr').then(() => toast.ok('shuffled'))}>🔀 TP shuffle</button>
          </div>
        </div>

        {/* Dimensions */}
        <div className="card-glow">
          <h3 className="font-bold mb-2">🌍 Dimensões</h3>
          {info?.dimensions?.map((d) => (
            <div key={d.id} className="flex items-center justify-between text-xs mb-1 bg-liberthia-900/40 px-2 py-1 rounded">
              <span className="font-mono">{d.id.replace('minecraft:', '')}</span>
              <span className="text-liberthia-300/50">{d.loadedChunks} chunks</span>
            </div>
          ))}
          {(info?.dimensions?.length ?? 0) === 0 && <p className="text-xs italic text-liberthia-300/50">—</p>}
        </div>

        {/* Danger zone */}
        <div className="card-glow !border-red-500/40 !bg-red-500/5">
          <h3 className="font-bold mb-2 text-red-300">⚠ Zona perigosa</h3>
          <p className="text-xs text-liberthia-300/60 mb-3">
            Parar o servidor exige confirmação. Backup é feito antes.
          </p>
          <button className={confirmStop ? 'btn-danger w-full pulse-glow' : 'btn-ghost w-full'} onClick={stopServer}>
            {confirmStop ? '⚠ CLIQUE DE NOVO PRA CONFIRMAR' : '🛑 Stop Server'}
          </button>
          {confirmStop && <button className="btn-ghost w-full mt-2 btn-sm" onClick={() => setConfirmStop(false)}>cancelar</button>}
        </div>
      </div>
    </div>
  )
}

function Stat({ emoji, label, value, sub, color }: { emoji: string; label: string; value: string; sub?: string; color?: 'green' | 'amber' | 'red' }) {
  const tone = color === 'green' ? 'text-emerald-300' : color === 'amber' ? 'text-amber-300' : color === 'red' ? 'text-red-300' : 'gradient-text'
  return (
    <div className="card-glow text-center">
      <div className="text-3xl mb-1">{emoji}</div>
      <div className={`text-2xl font-black ${tone}`}>{value}</div>
      <div className="text-xs text-liberthia-300/70">{label}</div>
      {sub && <div className="text-[10px] text-liberthia-300/40">{sub}</div>}
    </div>
  )
}
