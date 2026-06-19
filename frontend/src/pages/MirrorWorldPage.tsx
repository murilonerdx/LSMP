import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Mirror World — clone visualmente idêntico ao player.
 *
 * Vanilla MC não permite spawnar entity tipo `player`. Aproximação fiel:
 *  1. Spawn `zombie` (humanoid body) com:
 *     - Invulnerable, NoAI, Silent, PersistenceRequired
 *     - Invisibility 99999s → corpo zombie some
 *     - player_head{SkullOwner:"<name>"} no helmet → face do player real
 *     - Armadura + mainhand + offhand copiados via `item replace entity from entity`
 *  2. Resultado: só armadura + cabeça visíveis flutuando — visualmente é o player.
 *  3. Update posição via /tp em loop com yaw e pitch corretos.
 *
 * 3 modos: Follow / Mirror / Stalker
 */

type Mode = 'follow' | 'mirror' | 'stalker'

export function MirrorWorldPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 2000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState('')
  const [mode, setMode] = useState<Mode>('follow')
  const [distance, setDistance] = useState(4)
  const [updateSpeed, setUpdateSpeed] = useState(300)
  const [active, setActive] = useState(false)
  const [tick, setTick] = useState(0)
  const intervalRef = useRef<number | null>(null)
  const idRef = useRef<string>('')

  useEffect(() => () => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    if (idRef.current) api.command(`kill @e[tag=liberthia_mirror_${idRef.current}]`, 'mirror').catch(() => {})
  }, [])

  async function start() {
    if (!target) { toast.err('Selecione player'); return }
    const player = players.find((p) => p.uuid === target); if (!player) return
    const playerName = player.name

    idRef.current = `m${Date.now().toString(36)}`
    const tag = `liberthia_mirror_${idRef.current}`

    const yawRad = (player.position.yaw * Math.PI) / 180
    const dx = Math.sin(yawRad)
    const dz = -Math.cos(yawRad)
    const sx = player.position.x - dx * distance
    const sz = player.position.z - dz * distance

    const cloneName = `§7§o${playerName}`
    const nameComp = JSON.stringify({ text: cloneName }).replace(/'/g, "\\'")

    // NBT do zombie clone:
    //  - Invulnerable + NoAI + Silent + PersistenceRequired = não morre / não se mexe / não despawn
    //  - Invisibility infinita (effect id 14, MC 1.20) — esconde corpo zombie
    //  - HandDropChances + ArmorDropChances zerados (não dropa itens ao morrer)
    //  - Helmet = player_head com SkullOwner = playerName (pega skin do Mojang)
    const nbt = [
      `Invulnerable:1b`,
      `NoAI:1b`,
      `Silent:1b`,
      `PersistenceRequired:1b`,
      `CustomName:'${nameComp}'`,
      `CustomNameVisible:0b`,
      `Tags:["liberthia_mirror","${tag}"]`,
      `ActiveEffects:[{Id:14,Amplifier:0,Duration:2147483647,ShowParticles:0b}]`,
      `HandDropChances:[0f,0f]`,
      `ArmorDropChances:[0f,0f,0f,0f]`,
      // Slot 3 do ArmorItems = helmet (head). Outros vazios pra serem preenchidos pelo item replace.
      `ArmorItems:[{},{},{},{id:"minecraft:player_head",Count:1b,tag:{SkullOwner:"${playerName}"}}]`,
    ].join(',')

    try {
      // Limpa instâncias antigas com o mesmo tag (idempotente)
      await api.command(`kill @e[tag=${tag}]`, 'mirror').catch(() => {})
      // Summon zombie
      await api.command(`summon minecraft:zombie ${sx.toFixed(2)} ${player.position.y.toFixed(2)} ${sz.toFixed(2)} {${nbt}}`, 'mirror')
      // Espera ele existir antes do item replace
      await new Promise((r) => setTimeout(r, 200))

      const selClone = `@e[tag=${tag},limit=1]`
      // Copia armadura + arma do player real (head já tem player_head)
      await api.command(`item replace entity ${selClone} armor.chest from entity ${playerName} armor.chest`, 'mirror').catch(() => {})
      await api.command(`item replace entity ${selClone} armor.legs from entity ${playerName} armor.legs`, 'mirror').catch(() => {})
      await api.command(`item replace entity ${selClone} armor.feet from entity ${playerName} armor.feet`, 'mirror').catch(() => {})
      await api.command(`item replace entity ${selClone} weapon.mainhand from entity ${playerName} weapon.mainhand`, 'mirror').catch(() => {})
      await api.command(`item replace entity ${selClone} weapon.offhand from entity ${playerName} weapon.offhand`, 'mirror').catch(() => {})

      // Feedback dramático
      await api.title(target, '§8§l⊗ ELE TE COPIOU', '§7§oalguém com seu rosto te observa', 10, 80, 20)
      await api.sound(target, 'minecraft:entity.enderman.stare', 1, 0.5)
    } catch (e: any) {
      toast.err(e.message); return
    }

    setActive(true); setTick(0)

    intervalRef.current = window.setInterval(async () => {
      try {
        const live = await api.players()
        const p = live.find((x: any) => x.uuid === target)
        if (!p) return
        let nx: number, nz: number
        const yaw = (p.position.yaw * Math.PI) / 180
        const fdx = Math.sin(yaw), fdz = -Math.cos(yaw)
        if (mode === 'follow') {
          nx = p.position.x - fdx * distance
          nz = p.position.z - fdz * distance
        } else if (mode === 'mirror') {
          nx = p.position.x + fdx * distance
          nz = p.position.z + fdz * distance
        } else {
          const ang = Math.random() * Math.PI * 2
          nx = p.position.x + Math.cos(ang) * distance
          nz = p.position.z + Math.sin(ang) * distance
        }
        // Olhar pro player
        const lookDx = p.position.x - nx
        const lookDz = p.position.z - nz
        const lookYaw = Math.atan2(-lookDx, lookDz) * 180 / Math.PI

        await api.command(`tp @e[tag=${tag},limit=1] ${nx.toFixed(2)} ${p.position.y.toFixed(2)} ${nz.toFixed(2)} ${lookYaw.toFixed(1)} 0`, 'mirror')
        setTick((t) => t + 1)

        // Som de tensão ocasional
        if (Math.random() < 0.015) {
          api.command(`playsound minecraft:entity.warden.heartbeat hostile ${p.name} ${nx.toFixed(0)} ${p.position.y.toFixed(0)} ${nz.toFixed(0)} 1 0.6`, 'mirror').catch(() => {})
        }
        // Re-aplica invisibility ocasionalmente (paranoia)
        if (Math.random() < 0.005) {
          api.command(`effect give @e[tag=${tag},limit=1] minecraft:invisibility 99999 0 true`, 'mirror').catch(() => {})
        }
      } catch {}
    }, updateSpeed)
  }

  async function stop() {
    if (intervalRef.current) clearInterval(intervalRef.current)
    intervalRef.current = null
    try {
      if (idRef.current) {
        await api.command(`kill @e[tag=liberthia_mirror_${idRef.current}]`, 'mirror')
      }
      if (target) {
        await api.title(target, '§7§o⊗ ele se foi...', '', 5, 40, 10)
        await api.sound(target, 'minecraft:entity.enderman.teleport', 1, 0.6)
      }
    } catch {}
    setActive(false); idRef.current = ''
  }

  async function killAllMirrors() {
    try {
      await api.command(`kill @e[tag=liberthia_mirror]`, 'mirror')
      toast.ok('Todos espelhos eliminados')
    } catch (e: any) { toast.err(e.message) }
  }

  /** Re-equipa armadura do player real (caso ele tenha trocado) */
  async function refreshGear() {
    if (!idRef.current || !target) return
    const playerName = players.find((p) => p.uuid === target)?.name
    if (!playerName) return
    const tag = `liberthia_mirror_${idRef.current}`
    const selClone = `@e[tag=${tag},limit=1]`
    try {
      await api.command(`item replace entity ${selClone} armor.chest from entity ${playerName} armor.chest`, 'mirror')
      await api.command(`item replace entity ${selClone} armor.legs from entity ${playerName} armor.legs`, 'mirror')
      await api.command(`item replace entity ${selClone} armor.feet from entity ${playerName} armor.feet`, 'mirror')
      await api.command(`item replace entity ${selClone} weapon.mainhand from entity ${playerName} weapon.mainhand`, 'mirror')
      await api.command(`item replace entity ${selClone} weapon.offhand from entity ${playerName} weapon.offhand`, 'mirror')
      toast.ok('🛡 Equipamento re-sincronizado')
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🪞 Mirror World</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Clone <b>visualmente idêntico</b> ao player — zombie invisível com{' '}
            <code className="text-xs">player_head</code> e armadura copiada do player real.
          </p>
        </div>
        <button className="btn-danger btn-sm" onClick={killAllMirrors}>💀 Kill all mirrors</button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="card-glow">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-3">
            <div>
              <label className="label block mb-1">Player a copiar</label>
              <select className="input" value={target} onChange={(e) => setTarget(e.target.value)} disabled={active}>
                <option value="">— escolher —</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              {target && (
                <div className="mt-2 flex items-center gap-2 text-xs text-liberthia-300/70">
                  <img src={`https://mc-heads.net/avatar/${target}/24`} className="rounded" />
                  <span>skin será espelhada do Mojang</span>
                </div>
              )}
            </div>
            <div>
              <label className="label block mb-1">Update: {updateSpeed}ms</label>
              <input type="range" min={100} max={1000} step={50} value={updateSpeed}
                     onChange={(e) => setUpdateSpeed(Number(e.target.value))} className="w-full" />
              <div className="text-[10px] text-liberthia-300/50">menor = mais reativo, mais request</div>
            </div>
          </div>

          <label className="label block mb-1">Modo</label>
          <div className="grid grid-cols-3 gap-2 mb-3">
            <ModeBtn active={mode === 'follow'} onClick={() => setMode('follow')} emoji="🚶" label="Follow" desc="Anda atrás" />
            <ModeBtn active={mode === 'mirror'} onClick={() => setMode('mirror')} emoji="🪞" label="Mirror" desc="Lado oposto" />
            <ModeBtn active={mode === 'stalker'} onClick={() => setMode('stalker')} emoji="👁" label="Stalker" desc="Random ao redor" />
          </div>

          <label className="label block mb-1">Distância: {distance} blocos</label>
          <input type="range" min={2} max={15} value={distance} onChange={(e) => setDistance(Number(e.target.value))} className="w-full mb-3" />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {!active ? (
              <button className="btn md:col-span-2" onClick={start} disabled={!target}>🪞 Invocar Clone</button>
            ) : (
              <>
                <button className="btn-cyan" onClick={refreshGear} title="Re-copia equipamento atual">🛡 Re-equipar</button>
                <button className="btn-danger pulse-glow" onClick={stop}>✂ Dissolver</button>
              </>
            )}
          </div>
        </div>

        <div className="space-y-3">
          <div className="card">
            <h3 className="font-bold mb-2">📊 Status</h3>
            <div className="text-xs space-y-1">
              <Row k="Estado" v={active ? <span className="badge badge-green">ativo</span> : <span className="badge badge-purple">parado</span>} />
              <Row k="Ticks" v={tick} />
              <Row k="Modo" v={<span className="chip">{mode}</span>} />
              <Row k="Distância" v={<span className="chip">{distance}b</span>} />
              {active && idRef.current && <Row k="tag" v={<span className="font-mono text-[9px]">{idRef.current}</span>} />}
            </div>
          </div>

          <div className="card text-xs text-liberthia-300/70 leading-relaxed">
            <div className="font-bold text-liberthia-200 mb-2">🧬 Como o clone é feito</div>
            <p>• Vanilla MC <b>não permite</b> spawnar entity tipo <code>player</code></p>
            <p>• Aproximação fiel: <b>zombie</b> humanoid + <b>invisibility infinita</b> esconde o corpo</p>
            <p>• <b>player_head</b> com <code>SkullOwner:"{'<name>'}"</code> puxa a skin real do Mojang automaticamente</p>
            <p>• <code>item replace entity from entity</code> copia armadura + mainhand + offhand</p>
            <p>• Resultado: <b>cabeça + armadura</b> visíveis = visualmente o player</p>
            <p>• <b>Re-equipar</b> sincroniza se o player trocar gear</p>
            <p className="mt-2 text-amber-300/80">⚠ Skin offline/cracked pode não funcionar — depende do Mojang reconhecer o username.</p>
          </div>

          <div className="card text-xs text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Modos</div>
            <p>• <b>Follow</b>: atrás do player, sempre olhando</p>
            <p>• <b>Mirror</b>: lado oposto, espelha</p>
            <p>• <b>Stalker</b>: random ao redor — diferente posição cada tick</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function Row({ k, v }: { k: string; v: any }) {
  return (
    <div className="flex justify-between py-0.5">
      <span className="text-liberthia-300/70">{k}</span>
      <span>{v}</span>
    </div>
  )
}

function ModeBtn({ active, emoji, label, desc, onClick }: { active: boolean; emoji: string; label: string; desc: string; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick}
      className={`p-2 rounded-lg border text-center transition ${
        active ? 'bg-liberthia-500/20 border-liberthia-400/60 ring-2 ring-liberthia-400/30'
               : 'bg-liberthia-900/40 border-liberthia-500/20 hover:bg-liberthia-700/30'
      }`}>
      <div className="text-2xl">{emoji}</div>
      <div className="font-bold text-xs">{label}</div>
      <div className="text-[9px] text-liberthia-300/70">{desc}</div>
    </button>
  )
}
