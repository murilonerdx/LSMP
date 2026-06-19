import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Shadow Walker — modo "sombra" pro player. Aplica:
 *  - Invisibility (corpo some)
 *  - Glowing com team color preto (silhueta apenas)
 *  - Slow falling (anda em silêncio)
 *  - Blindness leve em outros players ao redor (atmosfera)
 *  - Trail de partículas dark seguindo
 *
 * Diferente de invisibility pura: cria mood de "sombra que anda entre eles".
 */

export function ShadowWalkerPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 2000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [trail, setTrail] = useState(true)
  const [darkenNearby, setDarkenNearby] = useState(false)
  const [active, setActive] = useState(false)
  const intervalRef = useRef<number | null>(null)
  const teamRef = useRef<string>('')

  useEffect(() => () => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    if (teamRef.current) api.command(`team remove ${teamRef.current}`, 'shadow').catch(() => {})
  }, [])

  async function start() {
    if (!target) { toast.err('Selecione player'); return }
    const player = players.find((p) => p.uuid === target); if (!player) return
    setActive(true)

    teamRef.current = `liberthia_shadow_${Date.now().toString(36)}`

    try {
      // Cria team com glow preto
      await api.command(`team add ${teamRef.current}`, 'shadow')
      await api.command(`team modify ${teamRef.current} color black`, 'shadow')
      await api.command(`team join ${teamRef.current} ${player.name}`, 'shadow')

      // Efeitos
      await api.command(`effect give ${player.name} minecraft:invisibility 999999 0 true`, 'shadow')
      await api.command(`effect give ${player.name} minecraft:glowing 999999 0 true`, 'shadow')
      await api.command(`effect give ${player.name} minecraft:slow_falling 999999 0 true`, 'shadow')
      await api.command(`effect give ${player.name} minecraft:night_vision 999999 0 true`, 'shadow')

      await api.title(target, '§8§l⊗ SOMBRA', '§7§ovocê não está mais aqui...', 15, 100, 30)
      await api.sound(target, 'minecraft:entity.warden.heartbeat', 1, 0.5)
      toast.ok(`🌑 ${player.name} agora é uma sombra`)
    } catch (e: any) {
      toast.err(e.message); setActive(false); return
    }

    // Loop de trail + darken nearby
    intervalRef.current = window.setInterval(async () => {
      try {
        const live = await api.players()
        const p = live.find((x: any) => x.uuid === target); if (!p) return
        // Trail de partículas atrás
        if (trail) {
          api.particle('minecraft:sculk_soul', p.position.x, p.position.y + 0.5, p.position.z, 3).catch(() => {})
          api.particle('minecraft:ash', p.position.x, p.position.y + 0.5, p.position.z, 5).catch(() => {})
        }
        // Darkness leve em players ao redor (não o próprio shadow)
        if (darkenNearby) {
          for (const other of live) {
            if (other.uuid === target) continue
            const dx = other.position.x - p.position.x
            const dz = other.position.z - p.position.z
            const dist = Math.sqrt(dx * dx + dz * dz)
            if (dist > 8) continue
            api.command(`effect give ${other.name} minecraft:darkness 40 0 true`, 'shadow').catch(() => {})
          }
        }
      } catch {}
    }, 500)
  }

  async function stop() {
    if (intervalRef.current) clearInterval(intervalRef.current)
    intervalRef.current = null
    const player = players.find((p) => p.uuid === target); if (!player) { setActive(false); return }
    try {
      await api.command(`effect clear ${player.name} minecraft:invisibility`, 'shadow')
      await api.command(`effect clear ${player.name} minecraft:glowing`, 'shadow')
      await api.command(`effect clear ${player.name} minecraft:slow_falling`, 'shadow')
      await api.command(`effect clear ${player.name} minecraft:night_vision`, 'shadow')
      if (teamRef.current) {
        await api.command(`team remove ${teamRef.current}`, 'shadow')
        teamRef.current = ''
      }
      await api.title(target, '§7§o🌑 Você retorna...', '', 5, 40, 10)
    } catch {}
    setActive(false)
  }

  return (
    <div className="route-fade max-w-[1200px]">
      <header className="mb-6">
        <h1 className="page-title">🌑 Shadow Walker</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Player vira sombra: invisible + glow preto (silhueta) + slow_falling + night_vision.
          Trail de sculk_soul + ash. Pode escurecer players ao redor.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="card-glow">
          <label className="label block mb-1">Player</label>
          <select className="input mb-3" value={target} onChange={(e) => setTarget(e.target.value)} disabled={active}>
            <option value="">— escolher —</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
          </select>

          <div className="space-y-2 mb-3">
            <label className="flex items-center gap-2 cursor-pointer p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
              <input type="checkbox" checked={trail} onChange={(e) => setTrail(e.target.checked)} />
              <span className="text-sm">💨 <b>Trail</b> — partículas sculk_soul + ash atrás</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
              <input type="checkbox" checked={darkenNearby} onChange={(e) => setDarkenNearby(e.target.checked)} />
              <span className="text-sm">🌑 <b>Escurecer</b> players ao redor (raio 8b)</span>
            </label>
          </div>

          {!active ? (
            <button className="btn w-full" onClick={start} disabled={!target}>🌑 Virar Sombra</button>
          ) : (
            <button className="btn-danger w-full pulse-glow" onClick={stop}>☀ Retornar</button>
          )}
        </div>

        <div className="card text-xs text-liberthia-300/70">
          <div className="font-bold text-liberthia-200 mb-2">🌑 Efeitos aplicados</div>
          <p>• <b>Invisibility</b> infinita — corpo some</p>
          <p>• <b>Glowing</b> com team color preto — silhueta visível</p>
          <p>• <b>Slow Falling</b> — anda silenciosamente</p>
          <p>• <b>Night Vision</b> — você enxerga no escuro</p>
          <p>• <b>Trail</b>: sculk_soul + ash a cada 500ms</p>
          <p>• <b>Darken nearby</b>: aplica darkness 40 ticks em players num raio de 8b</p>
        </div>
      </div>
    </div>
  )
}
