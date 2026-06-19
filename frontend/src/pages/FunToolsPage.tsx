import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Tools mais criativas/divertidas. Tudo via API existente — orquestração no front.
 *
 * - Disco Mode: cycle de cores em title + sons
 * - Random TP: teleporta player pra coords aleatórias
 * - Storyboard: sequência de cenas (title → wait → som → particle) play como cutscene
 * - Freeze: efeito de slowness contínuo
 * - Roleta da Morte: 1 player aleatório recebe lightning
 * - Confetti: chuva de fogos no local do player
 * - Swap Inventory: troca via /clear + replay (simulado com /give)
 * - Boss Mode: efeitos múltiplos pra fazer player virar "boss"
 */
export function FunToolsPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [target, setTarget] = useState<string>('')
  const tp = players.find((p) => p.uuid === target) ?? players[0]

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">🎉 Fun Zone</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Integrações divertidas — orquestram comandos pra criar efeitos ao vivo.
        </p>
      </header>

      <div className="card mb-6 flex items-center gap-3 flex-wrap">
        <span className="label">Alvo (quando aplicável)</span>
        <select className="input flex-1 max-w-md" value={target} onChange={(e) => setTarget(e.target.value)}>
          <option value="">— player —</option>
          {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
        </select>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        <DiscoMode players={players} />
        <RandomTp tp={tp} />
        <Storyboard tp={tp} />
        <BossMode tp={tp} />
        <RoletaDaMorte players={players} />
        <Confetti tp={tp} />
        <RainbowChat />
        <Earthquake />
        <SkyShow />
      </div>
    </div>
  )
}

// ============== Cards ==============

function DiscoMode({ players }: { players: Player[] }) {
  const [running, setRunning] = useState(false)
  const timerRef = useRef<number | null>(null)
  const COLORS = ['§a', '§b', '§c', '§d', '§e', '§6', '§5', '§9']
  const SONGS = [
    'minecraft:block.note_block.pling',
    'minecraft:block.note_block.bell',
    'minecraft:block.note_block.harp',
  ]
  function start() {
    if (running) return
    setRunning(true)
    let i = 0
    timerRef.current = window.setInterval(() => {
      const text = COLORS[i % COLORS.length] + '§l✦ DISCO ✦'
      const sub = COLORS[(i + 3) % COLORS.length] + 'PARTY MODE'
      players.forEach((p) => {
        api.title(p.uuid, text, sub, 2, 12, 2).catch(() => {})
        api.sound(p.uuid, SONGS[i % SONGS.length], 0.6, 1 + (i % 5) * 0.2).catch(() => {})
        api.particle('minecraft:firework', p.position.x, p.position.y + 2, p.position.z, 15).catch(() => {})
      })
      i++
    }, 800)
  }
  function stop() {
    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = null
    setRunning(false)
  }
  useEffect(() => () => { if (timerRef.current) clearInterval(timerRef.current) }, [])
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🪩 Disco Mode</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Cores piscando + sons + fogos pra todo mundo.</p>
      {!running && <button className="btn w-full" onClick={start}>▶ Start Party</button>}
      {running && <button className="btn-danger w-full" onClick={stop}>⏹ Stop</button>}
    </div>
  )
}

function RandomTp({ tp }: { tp?: Player }) {
  const [radius, setRadius] = useState(500)
  const [busy, setBusy] = useState(false)
  async function go() {
    if (!tp) return
    setBusy(true)
    const x = Math.floor(tp.position.x + (Math.random() - 0.5) * radius * 2)
    const z = Math.floor(tp.position.z + (Math.random() - 0.5) * radius * 2)
    try {
      await api.teleport(tp.uuid, x, 200, z)
      await api.title(tp.uuid, '§a✦ §dRandom TP §a✦', `§7${x}, ${z}`, 8, 60, 8)
      toast.ok(`${tp.name} → ${x}, ?, ${z}`)
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🎲 Random TP</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Teleporta player pra coordenada aleatória.</p>
      <label className="label">Raio: ±{radius} blocos</label>
      <input type="range" min={100} max={5000} step={100} value={radius} onChange={(e) => setRadius(Number(e.target.value))} className="w-full mb-3" />
      <button className="btn-cyan w-full" onClick={go} disabled={!tp || busy}>🌀 Randomize</button>
    </div>
  )
}

type Scene = { title?: string; subtitle?: string; sound?: string; particle?: string; command?: string; waitMs: number }

function Storyboard({ tp }: { tp?: Player }) {
  const [scenes, setScenes] = useKvState<Scene[]>('fun_storyboard', defaultStory())
  const [running, setRunning] = useState(false)
  const cancelRef = useRef(false)

  async function play() {
    if (!tp || running) return
    setRunning(true); cancelRef.current = false
    for (const s of scenes) {
      if (cancelRef.current) break
      try {
        if (s.title || s.subtitle) await api.title(tp.uuid, s.title ?? '', s.subtitle ?? '')
        if (s.sound) await api.sound(tp.uuid, s.sound)
        if (s.particle) await api.particle(s.particle, tp.position.x, tp.position.y + 1, tp.position.z, 50)
        if (s.command) await api.command(s.command.split('{player}').join(tp.name))
      } catch {}
      await new Promise((r) => setTimeout(r, s.waitMs))
    }
    setRunning(false)
  }
  return (
    <div className="card-glow lg:col-span-2 xl:col-span-1">
      <h3 className="font-bold text-lg mb-1">🎬 Storyboard</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Cenas em sequência — title, som, partícula, comando.</p>
      <div className="space-y-1.5 max-h-48 overflow-y-auto mb-3">
        {scenes.map((s, i) => (
          <div key={i} className="text-xs p-2 bg-liberthia-900/40 rounded border border-liberthia-500/20 space-y-1">
            <input className="input text-xs" placeholder="title" value={s.title ?? ''} onChange={(e) => updateScene(i, { title: e.target.value })} />
            <input className="input text-xs" placeholder="subtitle" value={s.subtitle ?? ''} onChange={(e) => updateScene(i, { subtitle: e.target.value })} />
            <div className="flex gap-1">
              <input className="input text-xs flex-1" placeholder="sound" value={s.sound ?? ''} onChange={(e) => updateScene(i, { sound: e.target.value })} />
              <input className="input text-xs w-24" type="number" placeholder="ms" value={s.waitMs} onChange={(e) => updateScene(i, { waitMs: Number(e.target.value) })} />
              <button className="btn-ghost btn-sm" onClick={() => setScenes((sc) => sc.filter((_, j) => j !== i))}>🗑</button>
            </div>
          </div>
        ))}
      </div>
      <div className="flex gap-2">
        <button className="btn-ghost btn-sm" onClick={() => setScenes((s) => [...s, { title: '', waitMs: 1500 }])}>+ Cena</button>
        {!running && <button className="btn flex-1" onClick={play} disabled={!tp}>▶ Play</button>}
        {running && <button className="btn-danger flex-1" onClick={() => { cancelRef.current = true }}>⏹ Stop</button>}
      </div>
    </div>
  )
  function updateScene(i: number, p: Partial<Scene>) {
    setScenes((sc) => sc.map((s, j) => j === i ? { ...s, ...p } : s))
  }
}

function defaultStory(): Scene[] {
  return [
    { title: '§5§l~ Apresentando ~', subtitle: '§7Você está prestes a ver...', waitMs: 2500, sound: 'minecraft:block.note_block.bell' },
    { title: '§d§l{player}', subtitle: '§7em ação', waitMs: 2500 },
    { title: '§6§l✦', subtitle: '§eGood luck', waitMs: 1500, particle: 'minecraft:firework', sound: 'minecraft:entity.player.levelup' },
  ]
}

function BossMode({ tp }: { tp?: Player }) {
  const [busy, setBusy] = useState(false)
  async function go() {
    if (!tp) return
    setBusy(true)
    try {
      await Promise.all([
        api.effect(tp.uuid, 'minecraft:strength', 6000, 4),
        api.effect(tp.uuid, 'minecraft:resistance', 6000, 3),
        api.effect(tp.uuid, 'minecraft:speed', 6000, 2),
        api.effect(tp.uuid, 'minecraft:fire_resistance', 6000, 0),
        api.effect(tp.uuid, 'minecraft:regeneration', 6000, 1),
      ])
      await api.title(tp.uuid, '§4§l⚔ BOSS MODE ⚔', '§7Você é a ameaça agora', 10, 80, 20)
      await api.sound(tp.uuid, 'minecraft:entity.ender_dragon.growl', 1, 0.8)
      toast.ok(`${tp.name} virou BOSS por 5min`)
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">⚔ Boss Mode</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Strength V + Resistance IV + Speed III + FireRes + Regen II por 5min.</p>
      <button className="btn-amber w-full" onClick={go} disabled={!tp || busy}>👑 Ativar</button>
    </div>
  )
}

function RoletaDaMorte({ players }: { players: Player[] }) {
  const [busy, setBusy] = useState(false)
  async function go() {
    if (players.length === 0) return
    setBusy(true)
    try {
      // 3-2-1 countdown
      for (const p of players) await api.title(p.uuid, '§c§l☠ ROLETA ☠', '§7Quem será?', 5, 25, 5)
      await new Promise((r) => setTimeout(r, 1500))
      const victim = players[Math.floor(Math.random() * players.length)]
      for (const p of players) {
        await api.title(p.uuid, '§c§l☠ VÍTIMA: §f' + victim.name, '§4Lightning incoming', 5, 40, 10)
      }
      await new Promise((r) => setTimeout(r, 1500))
      await api.lightning(victim.uuid)
      toast.ok(`${victim.name} foi sorteado(a) pela roleta`)
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">☠ Roleta da Morte</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Sorteia 1 player aleatório → recebe lightning.</p>
      <button className="btn-danger w-full" onClick={go} disabled={busy || players.length === 0}>🎯 Sortear</button>
    </div>
  )
}

function Confetti({ tp }: { tp?: Player }) {
  async function go() {
    if (!tp) return
    for (let i = 0; i < 8; i++) {
      api.command(`summon minecraft:firework_rocket ${Math.floor(tp.position.x + (Math.random() - 0.5) * 6)} ${Math.floor(tp.position.y + 3)} ${Math.floor(tp.position.z + (Math.random() - 0.5) * 6)} {LifeTime:30,FireworksItem:{id:"firework_rocket",Count:1,tag:{Fireworks:{Explosions:[{Type:${Math.floor(Math.random() * 4)},Colors:[I;${randomColor()},${randomColor()}]}]}}}}`).catch(() => {})
      await new Promise((r) => setTimeout(r, 250))
    }
    api.title(tp.uuid, '§6§l🎆 §dCONFETTI §6§l🎆', '§7parabéns!').catch(() => {})
    toast.ok('🎆 confetti')
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🎆 Confetti</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">8 fogos de artifício no local do player.</p>
      <button className="btn w-full" onClick={go} disabled={!tp}>Soltar Foguete</button>
    </div>
  )
}

function RainbowChat() {
  const [text, setText] = useState('LIBERTHIA')
  const [busy, setBusy] = useState(false)
  async function go() {
    setBusy(true)
    const colors = ['§c', '§6', '§e', '§a', '§b', '§9', '§5', '§d']
    let out = ''
    for (let i = 0; i < text.length; i++) out += colors[i % colors.length] + text[i]
    try { await api.broadcast(out + '§r §7← rainbow') } finally { setBusy(false) }
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🌈 Rainbow Chat</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Manda texto colorido caractere por caractere.</p>
      <input className="input mb-2" value={text} onChange={(e) => setText(e.target.value.toUpperCase())} />
      <button className="btn-cyan w-full" onClick={go} disabled={busy}>Enviar</button>
    </div>
  )
}

function Earthquake() {
  const [busy, setBusy] = useState(false)
  async function go() {
    setBusy(true)
    try {
      const list = await api.players()
      for (let i = 0; i < 12; i++) {
        await Promise.all(list.map((p) => api.effect(p.uuid, 'minecraft:nausea', 60, 0)))
        await new Promise((r) => setTimeout(r, 200))
      }
      for (const p of list) await api.title(p.uuid, '§4§l🜨 EARTHQUAKE 🜨', '§7~~~ shaking ~~~', 10, 40, 10)
    } finally { setBusy(false) }
    toast.ok('🜨 earthquake!')
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🜨 Earthquake</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Nausea pulsante em todos por 12 ticks + title.</p>
      <button className="btn-amber w-full" onClick={go} disabled={busy}>Tremer</button>
    </div>
  )
}

function SkyShow() {
  const [busy, setBusy] = useState(false)
  async function go() {
    setBusy(true)
    try {
      const list = await api.players()
      const phases = [
        { time: 18000, msg: '§9§l🌙 NOITE' },
        { time: 0, msg: '§e§l☀ DIA' },
        { time: 13000, msg: '§6§l🌅 PÔR DO SOL' },
      ]
      for (const ph of phases) {
        await api.worldTime(ph.time)
        for (const p of list) await api.title(p.uuid, ph.msg, '§7sky show', 10, 30, 10)
        await new Promise((r) => setTimeout(r, 2500))
      }
      await api.worldWeather('thunder', 200)
      for (const p of list) await api.title(p.uuid, '§5§l⚡ TEMPESTADE', '§7final', 10, 40, 20)
    } finally { setBusy(false) }
    toast.ok('☁ sky show done')
  }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">☁ Sky Show</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Cicla noite → dia → pôr-do-sol → tempestade. ~10s.</p>
      <button className="btn w-full" onClick={go} disabled={busy}>▶ Play</button>
    </div>
  )
}

function randomColor() {
  return Math.floor(Math.random() * 0xffffff)
}
