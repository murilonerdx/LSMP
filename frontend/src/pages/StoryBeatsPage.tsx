import { useState } from 'react'
import { useKvState } from '../lib/kvState'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { MinecraftFormatter, renderMcText } from '../components/MinecraftFormatter'

/**
 * Story Beats — cenas atmosféricas.
 *
 * 2 fontes:
 *  1. PRESETS (built-in, 8 cenas)
 *  2. CUSTOM (criados pelo usuário, persiste em localStorage)
 *
 * Cada beat é uma sequência de Steps:
 *   time, weather, title, sound, broadcast, effect, wait, command, particle
 */

type Step =
  | { kind: 'time'; ticks: number }
  | { kind: 'weather'; type: 'clear' | 'rain' | 'thunder'; duration: number }
  | { kind: 'title'; title: string; subtitle?: string; fadeIn?: number; stay?: number; fadeOut?: number }
  | { kind: 'sound'; sound: string; pitch?: number; volume?: number }
  | { kind: 'broadcast'; message: string }
  | { kind: 'effect'; effect: string; duration: number; amplifier: number }
  | { kind: 'particle'; particle: string; relX: number; relY: number; relZ: number; count: number }
  | { kind: 'wait'; ms: number }
  | { kind: 'command'; cmd: string }

type Beat = {
  id: string
  emoji: string
  name: string
  description: string
  steps: Step[]
  custom?: boolean
  updated?: number
}

const PRESETS: Beat[] = [
  {
    id: 'aurora',
    emoji: '🌌',
    name: 'Aurora dos Antigos',
    description: 'Manhã serena, sino distante e bênção branca pra todos.',
    steps: [
      { kind: 'time', ticks: 23000 },
      { kind: 'weather', type: 'clear', duration: 12000 },
      { kind: 'wait', ms: 1500 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.7 },
      { kind: 'title', title: '§e§l☀ Aurora', subtitle: '§7A luz retorna...', fadeIn: 20, stay: 60, fadeOut: 30 },
      { kind: 'wait', ms: 3000 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 100, amplifier: 0 },
    ],
  },
  {
    id: 'void', emoji: '🜨', name: 'Noite do Vazio',
    description: 'Meia-noite, tempestade, blindness curto + sussurro distante.',
    steps: [
      { kind: 'time', ticks: 18000 },
      { kind: 'weather', type: 'thunder', duration: 8000 },
      { kind: 'wait', ms: 800 },
      { kind: 'broadcast', message: '§8§o[uma presença observa do escuro]' },
      { kind: 'sound', sound: 'minecraft:entity.ghast.warn', pitch: 0.4 },
      { kind: 'title', title: '§4§l🜨 Noite do Vazio', subtitle: '§8algo te observa', fadeIn: 10, stay: 80, fadeOut: 20 },
      { kind: 'wait', ms: 1500 },
      { kind: 'effect', effect: 'minecraft:blindness', duration: 60, amplifier: 0 },
    ],
  },
  {
    id: 'ascension', emoji: '🪶', name: 'Ascensão',
    description: 'Levitação curta + sino agudo + título dourado.',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.note_block.chime', pitch: 1.5 },
      { kind: 'title', title: '§6§l✦ ASCENSÃO ✦', subtitle: '§eAlce-se', fadeIn: 5, stay: 50, fadeOut: 15 },
      { kind: 'effect', effect: 'minecraft:levitation', duration: 60, amplifier: 2 },
      { kind: 'wait', ms: 3500 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 100, amplifier: 0 },
    ],
  },
  {
    id: 'judgment', emoji: '⚖', name: 'Julgamento',
    description: 'Glow em todos + sino grave + título vermelho.',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.5 },
      { kind: 'title', title: '§4§l⚖ JULGAMENTO', subtitle: '§cseus atos ecoam', fadeIn: 15, stay: 70, fadeOut: 20 },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 300, amplifier: 0 },
    ],
  },
  {
    id: 'sanctuary', emoji: '⛪', name: 'Santuário',
    description: 'Dia + clear + resistance + título azul.',
    steps: [
      { kind: 'time', ticks: 6000 },
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'sound', sound: 'minecraft:block.beacon.activate', pitch: 1 },
      { kind: 'title', title: '§b§l✦ Santuário', subtitle: '§7você está seguro aqui', fadeIn: 20, stay: 80, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:resistance', duration: 600, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'corruption', emoji: '🦠', name: 'Corrupção',
    description: 'Tempestade súbita + wither + título roxo.',
    steps: [
      { kind: 'weather', type: 'thunder', duration: 6000 },
      { kind: 'sound', sound: 'minecraft:entity.wither.spawn', pitch: 1 },
      { kind: 'title', title: '§5§l🦠 CORRUPÇÃO', subtitle: '§8a matéria escura desperta', fadeIn: 10, stay: 80, fadeOut: 20 },
      { kind: 'wait', ms: 2000 },
      { kind: 'effect', effect: 'minecraft:wither', duration: 60, amplifier: 0 },
    ],
  },
  {
    id: 'tempest', emoji: '🌪', name: 'Tempestade',
    description: 'Thunder + raios espalhados + nausea.',
    steps: [
      { kind: 'weather', type: 'thunder', duration: 600 },
      { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.8 },
      { kind: 'title', title: '§9§l🌪 TEMPESTADE', subtitle: '§3os céus se rasgam', fadeIn: 5, stay: 60, fadeOut: 20 },
      { kind: 'wait', ms: 800 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 80, amplifier: 0 },
    ],
  },
  {
    id: 'oracle', emoji: '🔮', name: 'Oráculo',
    description: 'Som místico + título alongado + slow_falling.',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 0.7 },
      { kind: 'title', title: '§d§l🔮 §dO Oráculo Fala', subtitle: '§7ouça em silêncio...', fadeIn: 30, stay: 100, fadeOut: 40 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 200, amplifier: 0 },
    ],
  },
  // ===== Novos presets (v95) =====
  {
    id: 'first_blood', emoji: '🩸', name: 'Primeira Morte',
    description: 'Sino + tempo congela + título vermelho + glowing 10s pra marcar quem morreu.',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.elder_guardian.curse', pitch: 0.4 },
      { kind: 'title', title: '§4§l🩸 PRIMEIRA MORTE', subtitle: '§co solo bebeu sangue', fadeIn: 5, stay: 100, fadeOut: 30 },
      { kind: 'broadcast', message: '§4§oA terra registra cada queda...' },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 200, amplifier: 0 },
      { kind: 'wait', ms: 4000 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.3 },
    ],
  },
  {
    id: 'awakening', emoji: '☀', name: 'O Despertar',
    description: 'Manhã clara + sino agudo + speed por 30s pra todos acordarem juntos.',
    steps: [
      { kind: 'time', ticks: 0 },
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 1.2 },
      { kind: 'title', title: '§e§l☀ O DESPERTAR', subtitle: '§7um novo capítulo começa', fadeIn: 30, stay: 80, fadeOut: 40 },
      { kind: 'wait', ms: 2000 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 600, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:saturation', duration: 100, amplifier: 1 },
      { kind: 'broadcast', message: '§e§l✦ §rA aurora chama os corajosos.' },
    ],
  },
  {
    id: 'eclipse', emoji: '🌑', name: 'Eclipse Total',
    description: 'Meio-dia → noite súbita + darkness 20s + cave ambient.',
    steps: [
      { kind: 'time', ticks: 6000 },
      { kind: 'wait', ms: 1000 },
      { kind: 'broadcast', message: '§8§oo céu escurece subitamente...' },
      { kind: 'sound', sound: 'minecraft:ambient.cave', pitch: 0.5 },
      { kind: 'time', ticks: 18000 },
      { kind: 'weather', type: 'thunder', duration: 4000 },
      { kind: 'title', title: '§0§l🌑 ECLIPSE', subtitle: '§8o sol foi devorado', fadeIn: 10, stay: 120, fadeOut: 60 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 400, amplifier: 0 },
      { kind: 'wait', ms: 8000 },
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.8 },
    ],
  },
  {
    id: 'covenant', emoji: '🤝', name: 'Pacto Selado',
    description: 'Sino duplo + hero_of_village pra todos + broadcast solene — pra eventos de aliança/casamento.',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 1 },
      { kind: 'wait', ms: 500 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.8 },
      { kind: 'title', title: '§6§l🤝 PACTO SELADO', subtitle: '§eum laço foi firmado', fadeIn: 20, stay: 100, fadeOut: 40 },
      { kind: 'effect', effect: 'minecraft:hero_of_the_village', duration: 6000, amplifier: 1 },
      { kind: 'broadcast', message: '§6§l⚖ §rTestemunhas, o pacto não pode ser desfeito.' },
    ],
  },
  {
    id: 'plague', emoji: '☣', name: 'A Praga Chega',
    description: 'Hunger + weakness + poison curto + thunder + título verde-doente.',
    steps: [
      { kind: 'weather', type: 'thunder', duration: 6000 },
      { kind: 'sound', sound: 'minecraft:entity.husk.ambient', pitch: 0.5 },
      { kind: 'title', title: '§2§l☣ A PRAGA', subtitle: '§ao ar fica denso...', fadeIn: 10, stay: 100, fadeOut: 40 },
      { kind: 'broadcast', message: '§2§oum miasma se espalha pelo mundo...' },
      { kind: 'effect', effect: 'minecraft:hunger', duration: 200, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:weakness', duration: 200, amplifier: 0 },
      { kind: 'wait', ms: 2000 },
      { kind: 'effect', effect: 'minecraft:poison', duration: 80, amplifier: 0 },
    ],
  },
  {
    id: 'apocalypse', emoji: '🔥', name: 'Apocalipse',
    description: 'Trovões + raios + fire resistance pra dar chance + lava grunts.',
    steps: [
      { kind: 'time', ticks: 18000 },
      { kind: 'weather', type: 'thunder', duration: 12000 },
      { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.5 },
      { kind: 'title', title: '§c§l🔥 APOCALIPSE', subtitle: '§4O fim dos tempos', fadeIn: 5, stay: 140, fadeOut: 50 },
      { kind: 'broadcast', message: '§c§l⚠ §rO mundo está sendo julgado.' },
      { kind: 'effect', effect: 'minecraft:fire_resistance', duration: 400, amplifier: 0 },
      { kind: 'wait', ms: 3000 },
      { kind: 'sound', sound: 'minecraft:entity.ghast.warn', pitch: 0.3 },
      { kind: 'wait', ms: 2000 },
      { kind: 'sound', sound: 'minecraft:block.lava.ambient', pitch: 0.5 },
    ],
  },
  {
    id: 'rebirth', emoji: '🌱', name: 'Renascimento',
    description: 'Após algo terrível: clear + regeneration alta + título verde + sino suave.',
    steps: [
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'time', ticks: 1000 },
      { kind: 'sound', sound: 'minecraft:entity.experience_orb.pickup', pitch: 1 },
      { kind: 'wait', ms: 1500 },
      { kind: 'title', title: '§a§l🌱 RENASCIMENTO', subtitle: '§2a terra cura suas feridas', fadeIn: 30, stay: 100, fadeOut: 40 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 2 },
      { kind: 'effect', effect: 'minecraft:saturation', duration: 100, amplifier: 1 },
      { kind: 'broadcast', message: '§a§oa vida sempre encontra um caminho...' },
    ],
  },
  {
    id: 'whispers_of_void', emoji: '🌫', name: 'Sussurros do Vazio',
    description: 'Allay death em loop + broadcast eerie + slow_falling — clima oníricо.',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.allay.death', pitch: 0.5 },
      { kind: 'broadcast', message: '§8§o...você ouve algo?' },
      { kind: 'wait', ms: 2000 },
      { kind: 'sound', sound: 'minecraft:entity.allay.death', pitch: 0.4 },
      { kind: 'broadcast', message: '§8§oa voz vem de dentro...' },
      { kind: 'wait', ms: 2000 },
      { kind: 'sound', sound: 'minecraft:entity.allay.death', pitch: 0.3 },
      { kind: 'title', title: '§8§l🌫 §7sussurros', subtitle: '§8nada está como parece', fadeIn: 30, stay: 100, fadeOut: 50 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'last_stand', emoji: '⚔', name: 'Última Resistência',
    description: 'Battle music vibe: strength + speed + resistance — buff de guerra coletivo.',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.ravager.roar', pitch: 0.7 },
      { kind: 'title', title: '§c§l⚔ ÚLTIMA RESISTÊNCIA', subtitle: '§4juntos ou nada', fadeIn: 10, stay: 100, fadeOut: 30 },
      { kind: 'broadcast', message: '§c§l⚔ §rEles vêm. Levantem suas espadas.' },
      { kind: 'effect', effect: 'minecraft:strength', duration: 600, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 600, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:resistance', duration: 600, amplifier: 0 },
      { kind: 'wait', ms: 2000 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 1.5 },
    ],
  },
  {
    id: 'first_snow', emoji: '❄', name: 'Primeira Nevasca',
    description: 'Mudança de estação simbólica: rain + slow_falling + título azul.',
    steps: [
      { kind: 'weather', type: 'rain', duration: 24000 },
      { kind: 'time', ticks: 12000 },
      { kind: 'sound', sound: 'minecraft:block.snow.place', pitch: 1.5 },
      { kind: 'title', title: '§b§l❄ A NEVE CHEGA', subtitle: '§3o inverno desperta', fadeIn: 30, stay: 100, fadeOut: 40 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 100, amplifier: 0 },
      { kind: 'wait', ms: 2000 },
      { kind: 'broadcast', message: '§b§oa primeira neve cobre tudo de branco...' },
    ],
  },
]

const EMOJIS = ['🎬', '🌌', '🜨', '🪶', '⚖', '⛪', '🦠', '🌪', '🔮', '✨', '⚡', '🌙', '☀', '🩸', '👁', '🜲', '🕯', '🪐', '🌋', '🌊', '🪶', '⚱', '🎭', '🎪', '🩻']

export function StoryBeatsPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const soundsQ = useQuery({ queryKey: ['sounds'], queryFn: api.soundsList, retry: false })
  const players = playersQ.data ?? []
  const customSounds = soundsQ.data?.sounds ?? []
  const [running, setRunning] = useState<string | null>(null)
  const [target, setTarget] = useState<'all' | string>('all')
  const [custom, setCustom] = useKvState<Beat[]>('storybeats', [])
  const [editing, setEditing] = useState<Beat | null>(null)

  async function fire(beat: Beat) {
    setRunning(beat.id)
    const playerSelector = target === 'all' ? '@a' : nameOf(target, players)
    const playerUuids = target === 'all' ? players.map((p) => p.uuid) : [target]
    const player = players.find((p) => p.uuid === target)
    toast.info(`🎬 ${beat.emoji} ${beat.name}`)

    for (const s of beat.steps) {
      try {
        switch (s.kind) {
          case 'time': await api.worldTime(s.ticks); break
          case 'weather': await api.worldWeather(s.type, s.duration); break
          case 'title':
            for (const u of playerUuids) {
              api.title(u, s.title, s.subtitle ?? '', s.fadeIn ?? 10, s.stay ?? 60, s.fadeOut ?? 20).catch(() => {})
            }
            break
          case 'sound':
            for (const u of playerUuids) {
              api.sound(u, s.sound, s.volume ?? 1, s.pitch ?? 1).catch(() => {})
            }
            break
          case 'broadcast':
            await api.command(`tellraw ${playerSelector} ${JSON.stringify({ text: s.message })}`, 'story')
            break
          case 'effect':
            await api.command(`effect give ${playerSelector} ${s.effect} ${s.duration} ${s.amplifier} true`, 'story')
            break
          case 'particle':
            // Relativo à posição do player (se target=all, usa primeiro online)
            const ref = player ?? players[0]
            if (ref) {
              await api.particle(s.particle, ref.position.x + s.relX, ref.position.y + s.relY, ref.position.z + s.relZ, s.count)
            }
            break
          case 'command': await api.command(s.cmd, 'story'); break
          case 'wait': await new Promise((r) => setTimeout(r, s.ms)); break
        }
      } catch (e: any) { console.warn('step failed:', s.kind, e.message) }
    }
    toast.ok(`✓ ${beat.name} encerrado`)
    setRunning(null)
  }

  function newBeat() {
    setEditing({
      id: `custom_${Date.now().toString(36)}`,
      emoji: '🎬',
      name: 'Novo Beat',
      description: 'Descreva o mood...',
      steps: [
        { kind: 'title', title: '§5§lTítulo', subtitle: '§7subtítulo', fadeIn: 10, stay: 60, fadeOut: 20 },
      ],
      custom: true,
      updated: Date.now(),
    })
  }

  function saveBeat(b: Beat) {
    setCustom((cur) => {
      const i = cur.findIndex((x) => x.id === b.id)
      if (i >= 0) { const n = [...cur]; n[i] = { ...b, updated: Date.now() }; return n }
      return [...cur, { ...b, updated: Date.now() }]
    })
    setEditing(null)
    toast.ok(`Beat "${b.name}" salvo`)
  }

  function duplicate(b: Beat) {
    setEditing({
      ...b,
      id: `custom_${Date.now().toString(36)}`,
      name: b.name + ' (cópia)',
      custom: true,
    })
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎬 Story Beats</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Cenas atmosféricas instantâneas — clima, tempo, som, título, efeito, partículas, comandos. Crie os seus.
          </p>
        </div>
        <div className="flex gap-2 items-center flex-wrap">
          <select className="input max-w-xs text-sm" value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="all">🌐 Todos players ({players.length})</option>
            {players.map((p) => <option key={p.uuid} value={p.uuid}>👤 {p.name}</option>)}
          </select>
          <button className="btn" onClick={newBeat}>+ Criar Beat</button>
        </div>
      </header>

      {/* CUSTOM BEATS primeiro */}
      {custom.length > 0 && (
        <>
          <h2 className="text-lg font-bold gradient-text mb-3 flex items-center gap-2">
            ✨ Seus Beats <span className="badge badge-purple">{custom.length}</span>
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mb-8">
            {custom.map((b) => (
              <BeatCard key={b.id}
                beat={b}
                running={running === b.id}
                disabled={!!running}
                onFire={() => fire(b)}
                onEdit={() => setEditing(b)}
                onDuplicate={() => duplicate(b)}
                onDelete={() => setCustom((c) => c.filter((x) => x.id !== b.id))}
              />
            ))}
          </div>
        </>
      )}

      <h2 className="text-lg font-bold gradient-text mb-3 flex items-center gap-2">
        📚 Presets <span className="badge badge-purple">{PRESETS.length}</span>
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {PRESETS.map((b) => (
          <BeatCard key={b.id}
            beat={b}
            running={running === b.id}
            disabled={!!running}
            onFire={() => fire(b)}
            onDuplicate={() => duplicate(b)}
          />
        ))}
      </div>

      {editing && (
        <BeatEditor
          beat={editing}
          customSounds={customSounds}
          onSave={saveBeat}
          onCancel={() => setEditing(null)}
        />
      )}
    </div>
  )
}

// ============ Card ============

function BeatCard({ beat, running, disabled, onFire, onEdit, onDuplicate, onDelete }: {
  beat: Beat; running: boolean; disabled: boolean
  onFire: () => void
  onEdit?: () => void
  onDuplicate?: () => void
  onDelete?: () => void
}) {
  return (
    <div className={`card-glow transition-all ${running ? '!border-emerald-400/60 !bg-emerald-500/10' : ''}`}>
      <div className="text-5xl text-center mb-2">{beat.emoji}</div>
      <div className="font-bold text-lg text-center mb-1">{beat.name}</div>
      <div className="text-xs text-liberthia-300/70 text-center mb-3 min-h-[40px]">{beat.description}</div>
      <div className="flex flex-wrap gap-1 mb-3 justify-center">
        {beat.steps.slice(0, 8).map((s, i) => (
          <span key={i} className="chip text-[10px]">{stepLabel(s)}</span>
        ))}
        {beat.steps.length > 8 && <span className="chip text-[10px]">+{beat.steps.length - 8}</span>}
      </div>
      <button
        className={running ? 'btn-success w-full pulse-glow mb-2' : 'btn w-full mb-2'}
        disabled={disabled}
        onClick={onFire}
      >
        {running ? '⏳ Rodando...' : '▶ Disparar'}
      </button>
      <div className="grid grid-cols-3 gap-1">
        {onEdit && <button className="btn-ghost btn-sm" onClick={onEdit}>✎</button>}
        {!onEdit && <span />}
        {onDuplicate && <button className="btn-ghost btn-sm" onClick={onDuplicate} title="Duplicar pra editar">⎘</button>}
        {!onDuplicate && <span />}
        {onDelete && <button className="btn-ghost btn-sm" onClick={onDelete}>🗑</button>}
        {!onDelete && <span />}
      </div>
    </div>
  )
}

// ============ Editor ============

function BeatEditor({ beat, customSounds, onSave, onCancel }: {
  beat: Beat
  customSounds: import('../lib/api').SoundEntry[]
  onSave: (b: Beat) => void
  onCancel: () => void
}) {
  const [b, setB] = useState<Beat>(beat)

  function update(patch: Partial<Beat>) { setB({ ...b, ...patch }) }
  function updateStep(i: number, patch: any) {
    setB({ ...b, steps: b.steps.map((s, j) => j === i ? { ...s, ...patch } : s) })
  }
  function addStep(kind: Step['kind']) {
    let s: Step
    switch (kind) {
      case 'time': s = { kind: 'time', ticks: 12000 }; break
      case 'weather': s = { kind: 'weather', type: 'rain', duration: 6000 }; break
      case 'title': s = { kind: 'title', title: '§dTítulo', subtitle: '', fadeIn: 10, stay: 60, fadeOut: 20 }; break
      case 'sound': s = { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 1, volume: 1 }; break
      case 'broadcast': s = { kind: 'broadcast', message: '§7...' }; break
      case 'effect': s = { kind: 'effect', effect: 'minecraft:glowing', duration: 200, amplifier: 0 }; break
      case 'particle': s = { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 2, relZ: 0, count: 40 }; break
      case 'wait': s = { kind: 'wait', ms: 1000 }; break
      case 'command': s = { kind: 'command', cmd: 'say hello' }; break
    }
    setB({ ...b, steps: [...b.steps, s] })
  }
  function removeStep(i: number) {
    setB({ ...b, steps: b.steps.filter((_, j) => j !== i) })
  }
  function moveStep(i: number, dir: -1 | 1) {
    const j = i + dir
    if (j < 0 || j >= b.steps.length) return
    const next = [...b.steps]
    ;[next[i], next[j]] = [next[j], next[i]]
    setB({ ...b, steps: next })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">🎬 Story Beat</h3>

        <div className="grid grid-cols-[60px_1fr] gap-2 mb-3">
          <select className="input text-2xl text-center" value={b.emoji} onChange={(e) => update({ emoji: e.target.value })}>
            {EMOJIS.map((e) => <option key={e} value={e}>{e}</option>)}
          </select>
          <input className="input font-bold" value={b.name} onChange={(e) => update({ name: e.target.value })} />
        </div>

        <input className="input text-xs mb-4" placeholder="Descrição curta"
          value={b.description} onChange={(e) => update({ description: e.target.value })} />

        <div className="flex items-center justify-between mb-2">
          <h4 className="font-bold">📋 Passos ({b.steps.length})</h4>
        </div>

        {/* Step list */}
        <div className="space-y-2 mb-4">
          {b.steps.map((s, i) => (
            <div key={i} className="border border-liberthia-500/20 rounded-xl p-2.5 bg-liberthia-900/40">
              <div className="flex items-center gap-2 mb-2">
                <span className="badge badge-purple">#{i + 1}</span>
                <span className="chip">{s.kind}</span>
                <div className="ml-auto flex gap-0.5">
                  <button className="btn-ghost btn-sm" disabled={i === 0} onClick={() => moveStep(i, -1)}>↑</button>
                  <button className="btn-ghost btn-sm" disabled={i === b.steps.length - 1} onClick={() => moveStep(i, 1)}>↓</button>
                  <button className="btn-ghost btn-sm" onClick={() => removeStep(i)}>🗑</button>
                </div>
              </div>
              <StepEditor step={s} customSounds={customSounds} onChange={(p) => updateStep(i, p)} />
            </div>
          ))}
        </div>

        {/* Add step */}
        <div className="card !p-2 mb-4">
          <div className="text-[10px] uppercase tracking-widest text-liberthia-300/50 mb-2">+ Adicionar passo</div>
          <div className="grid grid-cols-3 sm:grid-cols-5 gap-1.5">
            <button className="btn-ghost btn-sm" onClick={() => addStep('title')}>📺 title</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('sound')}>🔊 sound</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('weather')}>⛅ weather</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('time')}>🕐 time</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('effect')}>⚗ effect</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('particle')}>✨ particle</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('broadcast')}>💬 chat</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('wait')}>⏳ wait</button>
            <button className="btn-ghost btn-sm" onClick={() => addStep('command')}>⌨ cmd</button>
          </div>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => onSave(b)} disabled={!b.name || b.steps.length === 0}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

// ============ Step Editor ============

function StepEditor({ step, customSounds, onChange }: {
  step: Step
  customSounds: import('../lib/api').SoundEntry[]
  onChange: (patch: any) => void
}) {
  switch (step.kind) {
    case 'time':
      return (
        <div className="flex items-center gap-2 text-xs">
          <label className="label">Ticks (0=manhã, 6000=meio-dia, 13000=anoitecer, 18000=meia-noite):</label>
          <input type="number" className="input w-28" value={step.ticks} onChange={(e) => onChange({ ticks: Number(e.target.value) })} />
          <div className="flex gap-1">
            <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 0 })}>☀ dia</button>
            <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 13000 })}>🌅 noite</button>
            <button className="btn-ghost btn-sm" onClick={() => onChange({ ticks: 18000 })}>🌙 meia-noite</button>
          </div>
        </div>
      )
    case 'weather':
      return (
        <div className="grid grid-cols-3 gap-2 text-xs">
          <select className="input" value={step.type} onChange={(e) => onChange({ type: e.target.value })}>
            <option value="clear">☀ clear</option>
            <option value="rain">🌧 rain</option>
            <option value="thunder">⛈ thunder</option>
          </select>
          <input type="number" className="input" value={step.duration} onChange={(e) => onChange({ duration: Number(e.target.value) })} />
          <span className="text-liberthia-300/50 text-xs flex items-center">ticks ({(step.duration / 20).toFixed(0)}s)</span>
        </div>
      )
    case 'title':
      return (
        <div className="space-y-2 text-xs">
          <MinecraftFormatter value={step.title} onChange={(v) => onChange({ title: v })} rows={2} maxChars={120} showCounter={false} placeholder="Title" />
          <MinecraftFormatter value={step.subtitle ?? ''} onChange={(v) => onChange({ subtitle: v })} rows={1} maxChars={120} showCounter={false} placeholder="Subtitle (opcional)" />
          <div className="grid grid-cols-3 gap-2">
            <div><label className="label">Fade In</label><input type="number" className="input" value={step.fadeIn ?? 10} onChange={(e) => onChange({ fadeIn: Number(e.target.value) })} /></div>
            <div><label className="label">Stay</label><input type="number" className="input" value={step.stay ?? 60} onChange={(e) => onChange({ stay: Number(e.target.value) })} /></div>
            <div><label className="label">Fade Out</label><input type="number" className="input" value={step.fadeOut ?? 20} onChange={(e) => onChange({ fadeOut: Number(e.target.value) })} /></div>
          </div>
        </div>
      )
    case 'sound':
      return (
        <div className="space-y-2 text-xs">
          <input className="input font-mono text-xs" value={step.sound} onChange={(e) => onChange({ sound: e.target.value })} placeholder="minecraft:block.bell.use ou liberthia:meu_som" />
          {customSounds.length > 0 && (
            <div>
              <div className="text-[10px] text-liberthia-300/60 mb-1">Sons custom (click pra colar):</div>
              <div className="flex flex-wrap gap-1">
                {customSounds.map((cs) => (
                  <button key={cs.playId} className="btn-ghost btn-sm" onClick={() => onChange({ sound: cs.playId })}>🎵 {cs.playId}</button>
                ))}
              </div>
            </div>
          )}
          <div className="grid grid-cols-2 gap-2">
            <div><label className="label">Pitch</label><input type="number" className="input" step={0.1} min={0.5} max={2} value={step.pitch ?? 1} onChange={(e) => onChange({ pitch: Number(e.target.value) })} /></div>
            <div><label className="label">Volume</label><input type="number" className="input" step={0.1} min={0} max={1} value={step.volume ?? 1} onChange={(e) => onChange({ volume: Number(e.target.value) })} /></div>
          </div>
        </div>
      )
    case 'broadcast':
      return (
        <MinecraftFormatter value={step.message} onChange={(v) => onChange({ message: v })} rows={2} maxChars={200} showCounter={false} placeholder="Mensagem no chat" />
      )
    case 'effect':
      return (
        <div className="grid grid-cols-3 gap-2 text-xs">
          <input className="input font-mono col-span-3" value={step.effect} onChange={(e) => onChange({ effect: e.target.value })} placeholder="minecraft:glowing" />
          <div><label className="label">Duração</label><input type="number" className="input" value={step.duration} onChange={(e) => onChange({ duration: Number(e.target.value) })} /></div>
          <div><label className="label">Amplificador</label><input type="number" className="input" value={step.amplifier} onChange={(e) => onChange({ amplifier: Number(e.target.value) })} /></div>
          <div className="flex gap-1 flex-wrap items-end">
            {['glowing', 'levitation', 'speed', 'slowness', 'blindness', 'regeneration', 'wither', 'nausea'].map((p) => (
              <button key={p} className="btn-ghost btn-sm" onClick={() => onChange({ effect: 'minecraft:' + p })}>{p.slice(0, 6)}</button>
            ))}
          </div>
        </div>
      )
    case 'particle':
      return (
        <div className="space-y-2 text-xs">
          <select className="input" value={step.particle} onChange={(e) => onChange({ particle: e.target.value })}>
            {['end_rod', 'flame', 'soul_fire_flame', 'dragon_breath', 'portal', 'totem_of_undying', 'heart', 'firework', 'enchant', 'reverse_portal', 'sculk_soul'].map((p) => (
              <option key={p} value={`minecraft:${p}`}>{p}</option>
            ))}
          </select>
          <div className="grid grid-cols-4 gap-2">
            <div><label className="label">+X</label><input type="number" className="input" value={step.relX} onChange={(e) => onChange({ relX: Number(e.target.value) })} /></div>
            <div><label className="label">+Y</label><input type="number" className="input" value={step.relY} onChange={(e) => onChange({ relY: Number(e.target.value) })} /></div>
            <div><label className="label">+Z</label><input type="number" className="input" value={step.relZ} onChange={(e) => onChange({ relZ: Number(e.target.value) })} /></div>
            <div><label className="label">Count</label><input type="number" className="input" value={step.count} onChange={(e) => onChange({ count: Number(e.target.value) })} /></div>
          </div>
          <div className="text-[10px] text-liberthia-300/50">Coords relativas à posição do player target</div>
        </div>
      )
    case 'wait':
      return (
        <div className="flex items-center gap-2 text-xs">
          <input type="range" min={100} max={10000} step={100} value={step.ms} onChange={(e) => onChange({ ms: Number(e.target.value) })} className="flex-1" />
          <span className="font-mono w-20 text-right">{(step.ms / 1000).toFixed(1)}s</span>
        </div>
      )
    case 'command':
      return (
        <input className="input font-mono text-xs" value={step.cmd} onChange={(e) => onChange({ cmd: e.target.value })} placeholder="ex: summon zombie ~ ~ ~" />
      )
  }
}

function stepLabel(s: Step): string {
  switch (s.kind) {
    case 'time': return `🕐 t=${s.ticks}`
    case 'weather': return `⛅ ${s.type}`
    case 'title': return `📺 title`
    case 'sound': return `🔊 ${s.sound.split(':')[1]?.split('.').pop() ?? 'sound'}`
    case 'broadcast': return `💬 chat`
    case 'effect': return `⚗ ${s.effect.replace('minecraft:', '')}`
    case 'particle': return `✨ ${s.particle.replace('minecraft:', '')}`
    case 'command': return `⌨ cmd`
    case 'wait': return `⏳ ${s.ms}ms`
  }
}

function nameOf(uuid: string, players: any[]): string {
  return players.find((p) => p.uuid === uuid)?.name ?? '@p'
}
