import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useEvents } from '../store/events'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Guardian — proteção forte contra morte pra player(s) selecionado(s).
 *
 * Estratégia em camadas:
 *  1. Loop a cada 2s: Resistance 5 + Regeneration X + FireRes + Saturation
 *     (5 levels de resistance = 100% damage absorb em PvE; em PvP com items
 *      criativos ainda pode passar, daí as próximas camadas)
 *  2. Monitoramento contínuo da posição → /spawnpoint segue o player
 *  3. gamerule keepInventory true
 *  4. Listener de player_death SSE: se o player guardado morrer, instant heal
 *     + tp pra coord da morte (executa AFTER respawn dele, que é automático
 *     pelo gamerule + spawnpoint)
 *  5. Listener de HP low (poll a cada 1s): se HP < 6, dispara instant_health 50
 *
 * Tudo só funciona enquanto a aba estiver aberta.
 */

type Guard = {
  uuid: string
  name: string
  enabled: boolean
  trackSpawnpoint: boolean
  emergencyHeal: boolean
  lastDeathPos?: { x: number; y: number; z: number; dim?: string }
}

const TICK_MS = 2000

export function GuardianPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 1000 })
  const subscribe = useEvents((s) => s.subscribe)
  const players = playersQ.data ?? []

  const [guards, setGuards] = useKvState<Guard[]>('guardian', [])
  const [keepInv, setKeepInv] = useState(false)
  const [log, setLog] = useState<{ ts: number; text: string }[]>([])
  const tickRef = useRef<number | null>(null)

  function pushLog(text: string) {
    setLog((l) => [{ ts: Date.now(), text }, ...l].slice(0, 30))
  }

  // Sincroniza com players online: adiciona faltantes (desligados)
  useEffect(() => {
    setGuards((cur) => {
      const next = [...cur]
      for (const p of players) {
        if (!next.find((g) => g.uuid === p.uuid)) {
          next.push({ uuid: p.uuid, name: p.name, enabled: false, trackSpawnpoint: true, emergencyHeal: true })
        }
      }
      // atualiza names
      return next.map((g) => ({ ...g, name: players.find((p) => p.uuid === g.uuid)?.name ?? g.name }))
    })
  }, [players.length])

  // Loop de aplicação de efeitos + spawnpoint tracking
  useEffect(() => {
    if (tickRef.current) clearInterval(tickRef.current)
    const active = guards.filter((g) => g.enabled)
    if (active.length === 0) return

    async function tick() {
      const livePlayers = await api.players().catch(() => [])
      for (const g of active) {
        const p = livePlayers.find((x: any) => x.uuid === g.uuid)
        if (!p) continue
        const name = p.name
        // Efeitos
        try { await api.command(`effect give ${name} minecraft:resistance 60 4 true`, 'guardian') } catch {}
        try { await api.command(`effect give ${name} minecraft:regeneration 60 9 true`, 'guardian') } catch {}
        try { await api.command(`effect give ${name} minecraft:fire_resistance 60 0 true`, 'guardian') } catch {}
        try { await api.command(`effect give ${name} minecraft:saturation 60 5 true`, 'guardian') } catch {}
        // Spawnpoint tracking
        if (g.trackSpawnpoint) {
          const x = Math.round(p.position.x), y = Math.round(p.position.y), z = Math.round(p.position.z)
          try { await api.command(`spawnpoint ${name} ${x} ${y} ${z}`, 'guardian') } catch {}
        }
        // Emergency heal se HP baixo
        if (g.emergencyHeal && p.health < 6 && p.maxHealth > 0) {
          try {
            await api.command(`effect give ${name} minecraft:instant_health 1 49 true`, 'guardian')
            await api.heal(g.uuid)
            pushLog(`❤ ${name} resgatado (HP estava ${p.health.toFixed(1)})`)
          } catch {}
        }
      }
    }
    tick()
    tickRef.current = window.setInterval(tick, TICK_MS)
    return () => { if (tickRef.current) clearInterval(tickRef.current) }
  }, [guards])

  // Listener de morte: se player guardado morreu, tp + heal ao "respawnar"
  useEffect(() => {
    return subscribe(async (ev) => {
      if (ev.type !== 'player_death') return
      const uuid = ev.data?.uuid
      const g = guards.find((x) => x.uuid === uuid && x.enabled)
      if (!g) return
      const dx = ev.data?.x, dy = ev.data?.y, dz = ev.data?.z
      pushLog(`💀 ${g.name} morreu (${ev.data?.source ?? '?'}) — auto-respawn em ${dx?.toFixed(0)},${dy?.toFixed(0)},${dz?.toFixed(0)}`)
      // Salva death pos pro state local
      setGuards((cur) => cur.map((x) => x.uuid === uuid ? { ...x, lastDeathPos: { x: dx, y: dy, z: dz } } : x))
      // Espera o player respawnar (gamerule keepInventory + spawnpoint setado)
      // Depois força HP cheia e tp ao local da morte
      setTimeout(async () => {
        try {
          await api.heal(uuid)
          if (dx !== undefined) await api.teleport(uuid, dx, dy, dz)
          await api.title(uuid, '§a§l✦ PROTEGIDO ✦', '§7você não pode ser morto', 8, 60, 12)
          await api.sound(uuid, 'minecraft:block.beacon.activate', 1, 1.5)
          pushLog(`✓ ${g.name} ressuscitado no local`)
        } catch {}
      }, 800)
    })
  }, [guards, subscribe])

  function toggle(uuid: string, patch?: Partial<Guard>) {
    setGuards((cur) => cur.map((g) => g.uuid === uuid ? { ...g, ...(patch ?? { enabled: !g.enabled }) } : g))
  }

  async function setKeepInventory(v: boolean) {
    try {
      await api.command(`gamerule keepInventory ${v}`, 'guardian')
      setKeepInv(v)
      toast.ok(`gamerule keepInventory = ${v}`)
    } catch (e: any) { toast.err(e.message) }
  }

  const activeCount = guards.filter((g) => g.enabled).length

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🛡 Guardian</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Imortalidade pra players selecionados. Resistance V + Regen X + spawnpoint que segue
            + auto-respawn no local da morte. Sobrevive até a sword sharpness 1000 do creative.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`badge ${activeCount > 0 ? 'badge-green' : 'badge-purple'}`}>
            {activeCount > 0 && <span className="live-dot" />}
            {activeCount} ativos
          </span>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        <div className="space-y-3">
          {/* Global */}
          <div className="card-glow">
            <h3 className="font-bold mb-2">🌐 Global</h3>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={keepInv} onChange={(e) => setKeepInventory(e.target.checked)} />
              <span className="text-sm"><b>gamerule keepInventory</b> = true (não perde items ao morrer)</span>
            </label>
          </div>

          {/* Per-player */}
          <div className="card-glow">
            <h3 className="font-bold mb-3">👤 Por player</h3>
            {guards.length === 0 && (
              <p className="text-xs text-liberthia-300/50 italic py-4 text-center">
                Nenhum player online ainda. Quando alguém entrar, aparece aqui.
              </p>
            )}
            <div className="space-y-2">
              {guards.map((g) => {
                const online = players.some((p) => p.uuid === g.uuid)
                return (
                  <div key={g.uuid} className={`flex items-center gap-3 p-3 rounded-xl border transition ${
                    g.enabled ? 'bg-emerald-500/10 border-emerald-400/40' : 'bg-liberthia-900/40 border-liberthia-500/20'
                  }`}>
                    <button
                      className={`w-12 h-7 rounded-full relative transition shrink-0 ${g.enabled ? 'bg-emerald-500/70' : 'bg-liberthia-700'}`}
                      onClick={() => toggle(g.uuid)}
                    >
                      <span className={`absolute top-1 ${g.enabled ? 'right-1' : 'left-1'} w-5 h-5 rounded-full bg-white transition-all`} />
                    </button>
                    <img src={`https://mc-heads.net/avatar/${g.uuid}/32`} className="rounded shrink-0" />
                    <div className="flex-1 min-w-0">
                      <div className="font-bold truncate flex items-center gap-2">
                        {g.name}
                        {!online && <span className="badge badge-red text-[10px]">offline</span>}
                      </div>
                      <div className="text-xs text-liberthia-300/60 flex gap-3 mt-0.5 flex-wrap">
                        <label className="flex items-center gap-1 cursor-pointer">
                          <input type="checkbox" checked={g.trackSpawnpoint} onChange={(e) => toggle(g.uuid, { trackSpawnpoint: e.target.checked })} />
                          spawnpoint segue
                        </label>
                        <label className="flex items-center gap-1 cursor-pointer">
                          <input type="checkbox" checked={g.emergencyHeal} onChange={(e) => toggle(g.uuid, { emergencyHeal: e.target.checked })} />
                          emergency heal
                        </label>
                      </div>
                    </div>
                    {g.lastDeathPos && (
                      <div className="text-[10px] text-liberthia-300/40 font-mono shrink-0">
                        💀 {g.lastDeathPos.x.toFixed(0)},{g.lastDeathPos.y.toFixed(0)},{g.lastDeathPos.z.toFixed(0)}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">📜 Log</h3>
            <div className="space-y-0.5 max-h-96 overflow-y-auto text-xs font-mono">
              {log.length === 0 && <p className="text-liberthia-300/50 italic">— sem eventos ainda —</p>}
              {log.map((l, i) => (
                <div key={i} className="flex gap-2">
                  <span className="text-liberthia-300/40 shrink-0">{new Date(l.ts).toLocaleTimeString()}</span>
                  <span className="flex-1 break-words">{l.text}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card text-xs text-liberthia-300/70 leading-relaxed">
            <div className="font-bold text-liberthia-200 mb-2">🛡 Camadas de proteção</div>
            <p>• <b>Resistance V</b> a cada 2s — absorve 100% do dano em PvE</p>
            <p>• <b>Regen X</b> — cura 10× por segundo</p>
            <p>• <b>FireRes + Saturation</b> — sem queimar nem morrer de fome</p>
            <p>• <b>Spawnpoint dinâmico</b> — atualiza pra coord atual a cada 2s</p>
            <p>• <b>Emergency heal</b> — quando HP &lt; 6, força instant_health 50</p>
            <p>• <b>Auto-respawn</b> — se morrer (creative kill), heal+tp pro local da morte</p>
            <p className="mt-2 text-amber-300">⚠ Só funciona com aba aberta</p>
          </div>
        </div>
      </div>
    </div>
  )
}
