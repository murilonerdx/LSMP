import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'
import { NumInput } from '../components/NumInput'
import { useKvState } from '../lib/kvState'
import { Autocomplete } from '../components/Autocomplete'
import { PlayerPosPicker } from '../components/PlayerPosPicker'
import { useSoundOptions } from '../lib/mcAutocomplete'

/**
 * Echo Whispers — sussurros ambientes vinculados a coordenadas.
 *
 * Diferente de Region Markers (zona = box 3D com title forte), Echo Whispers
 * são "balizas de lore" pontuais que sussurram quando o player chega perto,
 * sem disruptar o gameplay:
 *  - actionbar discreto (não title fullscreen)
 *  - som ambient suave (bell pitch baixo, amethyst chime)
 *  - cooldown longo por player+whisper (default 5min) pra não spammar
 *
 * Use cases: ruínas que sussurram fragmentos de história, árvores antigas,
 * monumentos perdidos, lugares-chave do worldbuilding.
 */

type Whisper = {
  id: string
  name: string         // nome interno do whisper
  emoji: string
  x: number; y: number; z: number
  radius: number       // raio de ativação
  text: string         // mensagem sussurrada
  sound: string        // som ambient
  cooldownMs: number   // cooldown por player
  enabled: boolean
}

/** Presets pré-fabricados — ajustar coords antes de habilitar. */
const ECHO_PRESETS: Omit<Whisper, 'id'>[] = [
  {
    name: 'Ruínas dos Antigos',
    emoji: '🜨',
    x: 100, y: 64, z: 100, radius: 12,
    text: '§7§o...pedras gastas pelo tempo sussurram nomes esquecidos...',
    sound: 'minecraft:ambient.cave',
    cooldownMs: 5 * 60_000,
    enabled: false,
  },
  {
    name: 'Árvore Anciã',
    emoji: '🌳',
    x: -50, y: 80, z: 200, radius: 8,
    text: '§2§o*a casca da árvore vibra como se estivesse viva*',
    sound: 'minecraft:block.azalea.step',
    cooldownMs: 10 * 60_000,
    enabled: false,
  },
  {
    name: 'Bússola sem Norte',
    emoji: '🧭',
    x: 0, y: 64, z: 0, radius: 5,
    text: '§5§o"você não está perdido. apenas em outro lugar."',
    sound: 'minecraft:block.amethyst_block.chime',
    cooldownMs: 3 * 60_000,
    enabled: false,
  },
  {
    name: 'Crepúsculo do Naufrágio',
    emoji: '⚓',
    x: 200, y: 50, z: -100, radius: 15,
    text: '§3§o*ondas levam ecos de canções marinheiras antigas*',
    sound: 'minecraft:weather.rain',
    cooldownMs: 5 * 60_000,
    enabled: false,
  },
  {
    name: 'Sussurro da Cratera',
    emoji: '🌋',
    x: 300, y: 50, z: 300, radius: 10,
    text: '§4§o*o solo aqui ainda lembra do calor que o queimou*',
    sound: 'minecraft:block.lava.ambient',
    cooldownMs: 8 * 60_000,
    enabled: false,
  },
  {
    name: 'Memória do Cemitério',
    emoji: '⚰',
    x: -100, y: 65, z: -100, radius: 8,
    text: '§8§o*passos sobre solo sagrado fazem os mortos se moverem...*',
    sound: 'minecraft:entity.elder_guardian.curse',
    cooldownMs: 10 * 60_000,
    enabled: false,
  },
  {
    name: 'Fonte da Verdade',
    emoji: '💧',
    x: 50, y: 62, z: -50, radius: 6,
    text: '§b§o"olhe seu reflexo. é quem você está se tornando."',
    sound: 'minecraft:block.water.ambient',
    cooldownMs: 5 * 60_000,
    enabled: false,
  },
  {
    name: 'Portal Quebrado',
    emoji: '🌀',
    x: 100, y: 70, z: 0, radius: 4,
    text: '§5§o*o ar tremula. algo quase atravessou*',
    sound: 'minecraft:block.portal.ambient',
    cooldownMs: 7 * 60_000,
    enabled: false,
  },
]


export function EchoWhispersPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 2000 })
  const players = playersQ.data ?? []
  const [echoes, setEchoes] = useKvState<Whisper[]>('echo_whispers', [])
  const [editing, setEditing] = useState<Whisper | null>(null)
  const [fired, setFired] = useState<{ ts: number; whisper: string; player: string }[]>([])
  // Cooldowns: key = "whisperId:playerUuid" -> ts last fired
  const [cooldowns] = useState<Map<string, number>>(() => new Map())

  // Detector
  useEffect(() => {
    if (echoes.length === 0 || players.length === 0) return
    for (const w of echoes) {
      if (!w.enabled) continue
      for (const p of players) {
        const dx = p.position.x - w.x
        const dy = p.position.y - w.y
        const dz = p.position.z - w.z
        const dist = Math.sqrt(dx * dx + dy * dy + dz * dz)
        if (dist > w.radius) continue
        const key = `${w.id}:${p.uuid}`
        const last = cooldowns.get(key) ?? 0
        if (Date.now() - last < w.cooldownMs) continue
        cooldowns.set(key, Date.now())
        whisperTo(w, p)
      }
    }
  }, [players, echoes])

  async function whisperTo(w: Whisper, p: { uuid: string; name: string }) {
    setFired((f) => [{ ts: Date.now(), whisper: w.name, player: p.name }, ...f].slice(0, 30))
    try {
      // Actionbar (discreto, não fullscreen)
      await api.command(`title ${p.name} actionbar ${JSON.stringify({ text: '§7§o' + w.text })}`, 'echo')
      // Som ambient quieto e pitch baixo
      await api.sound(p.uuid, w.sound, 0.4, 0.7)
    } catch {}
  }

  function newWhisper() {
    setEditing({
      id: `echo_${Date.now().toString(36)}`,
      name: 'ruina_antiga',
      emoji: '🪨',
      x: 0, y: 70, z: 0,
      radius: 8,
      text: '§7§o...este lugar lembra de algo antigo...',
      sound: 'minecraft:block.sculk.charge',
      cooldownMs: 300000,
      enabled: true,
    })
  }

  function commit(w: Whisper) {
    setEchoes((cur) => {
      const i = cur.findIndex((x) => x.id === w.id)
      if (i >= 0) { const n = [...cur]; n[i] = w; return n }
      return [...cur, w]
    })
    setEditing(null)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🕯 Echo Whispers</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Balizas de lore ambientes. Player se aproxima do ponto → sussurro discreto no actionbar + som suave.
            Cooldown por player evita spam.
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <button className="btn-ghost btn-sm" onClick={() => {
            const newOnes: Whisper[] = ECHO_PRESETS.map((p, i) => ({
              ...p,
              id: 'preset-' + Date.now() + '-' + i,
            }))
            setEchoes((cur) => [...cur, ...newOnes])
          }}>📥 Importar {ECHO_PRESETS.length} presets</button>
          <button className="btn" onClick={newWhisper}>+ Novo Eco</button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="space-y-3">
          {echoes.length === 0 && (
            <div className="card text-center py-12">
              <div className="text-5xl mb-3 opacity-50">🕯</div>
              <p className="text-liberthia-300/70">Nenhum eco criado ainda. Plante a lore no mapa.</p>
            </div>
          )}
          {echoes.map((w) => (
            <div key={w.id} className="card-glow flex items-center gap-3">
              <button
                className={`w-12 h-7 rounded-full relative transition shrink-0 ${w.enabled ? 'bg-emerald-500/70' : 'bg-liberthia-700'}`}
                onClick={() => setEchoes((cur) => cur.map((x) => x.id === w.id ? { ...x, enabled: !x.enabled } : x))}
              >
                <span className={`absolute top-1 ${w.enabled ? 'right-1' : 'left-1'} w-5 h-5 rounded-full bg-white transition-all`} />
              </button>
              <span className="text-3xl">{w.emoji}</span>
              <div className="flex-1 min-w-0">
                <div className="font-bold">{w.name}</div>
                <div className="text-xs text-liberthia-300/70 mt-0.5 italic">{renderMcText(w.text)}</div>
                <div className="text-[10px] text-liberthia-300/50 font-mono mt-1">
                  📍 {w.x},{w.y},{w.z} · raio {w.radius}b · cooldown {Math.round(w.cooldownMs / 1000 / 60)}min
                </div>
              </div>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(w)}>✎</button>
              <button className="btn-ghost btn-sm" onClick={() => setEchoes((cur) => cur.filter((x) => x.id !== w.id))}>🗑</button>
            </div>
          ))}
        </div>

        <div className="card">
          <h3 className="font-bold mb-2">🔔 Sussurros recentes</h3>
          <div className="space-y-1 max-h-96 overflow-y-auto text-xs">
            {fired.length === 0 && <p className="italic text-liberthia-300/50">— silêncio —</p>}
            {fired.map((f, i) => (
              <div key={i} className="text-emerald-300/80">
                {new Date(f.ts).toLocaleTimeString()} · §7{f.whisper}§r → {f.player}
              </div>
            ))}
          </div>
        </div>
      </div>

      {editing && (
        <EchoEditor echo={editing} onSave={commit} onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function EchoEditor({ echo, onSave, onCancel }: {
  echo: Whisper; onSave: (w: Whisper) => void; onCancel: () => void
}) {
  const [w, setW] = useState<Whisper>(echo)
  const sounds = useSoundOptions()
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🕯 Echo Whisper</h3>

        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <input className="input text-2xl text-center" value={w.emoji} onChange={(e) => setW({ ...w, emoji: e.target.value })} />
          <input className="input" value={w.name} onChange={(e) => setW({ ...w, name: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_') })} placeholder="ruina_antiga" />
        </div>

        <label className="label block mb-1">Texto do sussurro</label>
        <MinecraftFormatter value={w.text} onChange={(v) => setW({ ...w, text: v })} rows={2} maxChars={120} />

        <div className="grid grid-cols-3 gap-2 mt-3 mb-2">
          <div><label className="label">X</label><NumInput value={w.x} onChange={(v) => setW({ ...w, x: v })} /></div>
          <div><label className="label">Y</label><NumInput value={w.y} onChange={(v) => setW({ ...w, y: v })} /></div>
          <div><label className="label">Z</label><NumInput value={w.z} onChange={(v) => setW({ ...w, z: v })} /></div>
        </div>
        <div className="mb-3">
          <label className="label">📍 Quick: pos do player</label>
          <PlayerPosPicker onPick={(p) => setW({ ...w, x: Math.round(p.x), y: Math.round(p.y), z: Math.round(p.z) })} />
        </div>

        <div className="grid grid-cols-2 gap-2 mb-3">
          <div>
            <label className="label block mb-1">Raio: {w.radius}b</label>
            <input type="range" min={3} max={32} value={w.radius} onChange={(e) => setW({ ...w, radius: Number(e.target.value) })} className="w-full" />
          </div>
          <div>
            <label className="label block mb-1">Cooldown: {(w.cooldownMs / 60000).toFixed(0)}min</label>
            <input type="range" min={30000} max={3600000} step={30000} value={w.cooldownMs} onChange={(e) => setW({ ...w, cooldownMs: Number(e.target.value) })} className="w-full" />
          </div>
        </div>

        <label className="label block mb-1">Som ambient</label>
        <div className="mb-4">
          <Autocomplete value={w.sound} onChange={(v) => setW({ ...w, sound: v })} options={sounds} placeholder="minecraft:block.sculk.charge" />
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(w)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
