import { Atmosphere } from '../lib/atmosphere'
import { AtmosphereGrid } from '../components/AtmosphereGrid'

const PRESETS: Atmosphere[] = [
  {
    id: 'h_tv', emoji: '📺', name: 'TV Sem Sinal',
    description: '30 frames de chars corrompidos + ruído branco',
    category: 'horror',
    steps: [
      { kind: 'loop', iterations: 30, gapMs: 80, steps: [
        { kind: 'garble', text: 'SINAL PERDIDO', iterations: 1, intervalMs: 50, color: '§8' },
        { kind: 'sound', sound: 'minecraft:block.note_block.snare', pitch: 1, volume: 0.3 },
      ]},
    ],
  },
  {
    id: 'h_glitch', emoji: '🩸', name: 'Screen Glitch',
    description: 'Frases corrompidas piscando em vermelho/preto',
    category: 'horror',
    steps: [
      { kind: 'loop', iterations: 18, gapMs: 150, steps: [
        { kind: 'random', count: 1, choices: [
          { kind: 'flash', text: 'ELES VEEM', flashes: 1, intervalMs: 100, colors: ['§4', '§0'] },
          { kind: 'flash', text: 'ATRÁS DE VOCÊ', flashes: 1, intervalMs: 100, colors: ['§c', '§4'] },
          { kind: 'flash', text: 'NÃO OLHE', flashes: 1, intervalMs: 100, colors: ['§8', '§0'] },
          { kind: 'flash', text: 'CORRA', flashes: 1, intervalMs: 100, colors: ['§4', '§c'] },
          { kind: 'flash', text: 'TARDE DEMAIS', flashes: 1, intervalMs: 100, colors: ['§0', '§4'] },
        ]},
        { kind: 'sound', sound: 'minecraft:entity.elder_guardian.curse', pitch: 0.5, volume: 0.4 },
      ]},
    ],
  },
  {
    id: 'h_heart', emoji: '💔', name: 'Batimento Cardíaco',
    description: 'Bombo acelerando até flatline',
    category: 'horror',
    steps: [
      ...Array.from({ length: 20 }, (_, i) => {
        const delay = Math.max(180, 800 - i * 30)
        return [
          { kind: 'sound', sound: 'minecraft:block.note_block.basedrum', pitch: 0.6, volume: 0.8 },
          { kind: 'wait', ms: delay / 2 },
          { kind: 'sound', sound: 'minecraft:block.note_block.basedrum', pitch: 0.5, volume: 0.6 },
          { kind: 'wait', ms: delay / 2 },
        ] as any
      }).flat(),
      { kind: 'title', text: '§4§l💔 . . . . . .', stay: 60 },
      { kind: 'sound', sound: 'minecraft:entity.player.death', pitch: 0.6, volume: 1 },
    ],
  },
  {
    id: 'h_scare', emoji: '⚡', name: 'JUMPSCARE',
    description: 'Flash branco + ruído alto + nausea — intenso',
    category: 'horror',
    steps: [
      { kind: 'flash', text: '█████████████', flashes: 3, intervalMs: 50, colors: ['§f', '§4'] },
      { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.5, volume: 1 },
      { kind: 'sound', sound: 'minecraft:entity.elder_guardian.curse', pitch: 0.3, volume: 1 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 60, amplifier: 1 },
      { kind: 'title', text: '§4§l!!!', subtitle: '§c§oalgo te encontrou', stay: 30 },
    ],
  },
  {
    id: 'h_darkness', emoji: '🌑', name: 'Véu da Escuridão',
    description: 'Darkness 15s + noite + tempestade + ambient cave',
    category: 'horror',
    steps: [
      { kind: 'time', ticks: 18000 },
      { kind: 'weather', type: 'thunder', duration: 1200 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 300, amplifier: 0 },
      { kind: 'sound', sound: 'minecraft:ambient.cave', pitch: 0.5, volume: 0.6 },
      { kind: 'title', text: '§0§l. . .', subtitle: '§8§oa luz se foi', fadeIn: 20, stay: 100, fadeOut: 30 },
    ],
  },
  {
    id: 'h_presence', emoji: '👤', name: 'Presença Atrás',
    description: 'Warden heartbeat + darkness + sonic boom',
    category: 'horror',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.8, volume: 1 },
      { kind: 'title', text: ' ', subtitle: '§8§o*algo respira atrás de você*', stay: 60 },
      { kind: 'wait', ms: 1500 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 80, amplifier: 0 },
      { kind: 'sound', sound: 'minecraft:entity.warden.sonic_boom', volume: 1 },
    ],
  },
  {
    id: 'h_scream', emoji: '🗣', name: 'Grito Distante',
    description: 'Ghast scream + subtitle',
    category: 'horror',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.ghast.scream', pitch: 0.3, volume: 2 },
      { kind: 'title', text: ' ', subtitle: '§8§oum grito distante...', stay: 60 },
    ],
  },
  {
    id: 'h_whisper', emoji: '🌫', name: 'Sussurros',
    description: 'Fragmentos em subtitles + allay death',
    category: 'horror',
    loop: false,
    steps: [
      { kind: 'loop', iterations: 8, gapMs: 600, steps: [
        { kind: 'random', count: 1, choices: [
          { kind: 'title', mode: 'subtitle', text: '§8§oeu vejo você', stay: 30 },
          { kind: 'title', mode: 'subtitle', text: '§7§oeles estão chegando', stay: 30 },
          { kind: 'title', mode: 'subtitle', text: '§0§ovocê não tem saída', stay: 30 },
          { kind: 'title', mode: 'subtitle', text: '§4§oolhe pra trás', stay: 30 },
        ]},
        { kind: 'sound', sound: 'minecraft:entity.allay.death', pitch: 0.5, volume: 0.3 },
      ]},
    ],
  },
  {
    id: 'h_chat', emoji: '💬', name: 'Chat Corrompido',
    description: 'Mensagem fica progressivamente garbled',
    category: 'horror',
    steps: [
      { kind: 'garble', text: 'eles estão chegando', iterations: 5, intervalMs: 400, color: '§8' },
      { kind: 'chat', message: '§4§leles estão chegando' },
    ],
  },
  {
    id: 'h_anatomy', emoji: '🩻', name: 'Anatomia do Pânico',
    description: 'Combina heartbeat + flash + whisper + glitch (insano)',
    category: 'horror',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.6, volume: 1 },
      { kind: 'wait', ms: 800 },
      { kind: 'flash', text: 'AQUI', flashes: 4, intervalMs: 80, colors: ['§4', '§c', '§0', '§f'] },
      { kind: 'sound', sound: 'minecraft:entity.elder_guardian.curse', pitch: 0.4 },
      { kind: 'wait', ms: 600 },
      { kind: 'garble', text: 'voce nao deveria estar aqui', iterations: 10, intervalMs: 80, color: '§8' },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 80, amplifier: 1 },
      { kind: 'wait', ms: 1500 },
      { kind: 'flash', text: '█████████', flashes: 2, intervalMs: 60, colors: ['§f', '§0'] },
      { kind: 'sound', sound: 'minecraft:entity.warden.sonic_boom', volume: 1 },
      { kind: 'title', text: '§4§l!!!', subtitle: '§c§oacordou?', stay: 60 },
    ],
  },
  // ============ +4 PRESETS NOVOS ============
  {
    id: 'h_static_voice', emoji: '📻', name: 'Rádio Quebrado',
    description: 'Estática + frases truncadas como se a frequência captasse algo',
    category: 'horror',
    steps: [
      { kind: 'loop', iterations: 12, gapMs: 300, steps: [
        { kind: 'sound', sound: 'minecraft:block.note_block.snare', pitch: 1.8, volume: 0.2 },
        { kind: 'sound', sound: 'minecraft:block.note_block.hat', pitch: 2, volume: 0.15 },
        { kind: 'random', count: 1, choices: [
          { kind: 'title', mode: 'actionbar', text: '§7▒▒▒...§celes vêm...§7▒▒▒' },
          { kind: 'title', mode: 'actionbar', text: '§7▒▒...§4por traz da po...§7▒▒▒' },
          { kind: 'title', mode: 'actionbar', text: '§7▒...§cnao saia do qu...§7▒▒▒' },
          { kind: 'title', mode: 'actionbar', text: '§7...§4SE OUVE§7▒▒▒' },
        ]},
      ]},
      { kind: 'sound', sound: 'minecraft:block.note_block.bit', pitch: 0.3, volume: 1 },
      { kind: 'title', text: '§4§l📻', subtitle: '§c§ofim de transmissão', stay: 60 },
    ],
  },
  {
    id: 'h_corpse_breath', emoji: '💀', name: 'Respiração Próxima',
    description: 'Respirações ofegantes + atraso entre cada uma + sussurros entre fôlegos',
    category: 'horror',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.3, volume: 0.7 },
      { kind: 'wait', ms: 1200 },
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.4, volume: 0.8 },
      { kind: 'title', mode: 'subtitle', text: '§8§o*inspira*', stay: 30 },
      { kind: 'wait', ms: 1500 },
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.5, volume: 0.9 },
      { kind: 'title', mode: 'subtitle', text: '§7§o*expira lentamente*', stay: 30 },
      { kind: 'wait', ms: 1800 },
      { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.6, volume: 1 },
      { kind: 'title', mode: 'subtitle', text: '§4§o*muito perto da sua nuca*', stay: 50 },
      { kind: 'effect', effect: 'minecraft:slowness', duration: 60, amplifier: 0 },
    ],
  },
  {
    id: 'h_bleeding_walls', emoji: '🩸', name: 'Paredes Sangrando',
    description: 'Particle de gota + filtro vermelho via title + chat de aviso',
    category: 'horror',
    steps: [
      { kind: 'effect', effect: 'minecraft:nausea', duration: 120, amplifier: 0 },
      { kind: 'loop', iterations: 8, gapMs: 250, steps: [
        { kind: 'particle', particle: 'minecraft:dust 0.6 0.0 0.0 1', count: 30 },
        { kind: 'sound', sound: 'minecraft:entity.player.hurt', pitch: 0.5, volume: 0.3 },
      ]},
      { kind: 'title', text: '§4§l🩸', subtitle: '§c§oas paredes choram vermelho', fadeIn: 10, stay: 80, fadeOut: 20 },
      { kind: 'chat', message: '§4§oo cheiro não sai...' },
    ],
  },
  {
    id: 'h_door_closing', emoji: '🚪', name: 'Porta Fechando',
    description: 'Sequência de portas fechando atrás do player — sensação de prisão',
    category: 'horror',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.iron_door.close', pitch: 0.7, volume: 1 },
      { kind: 'title', mode: 'subtitle', text: '§8§ouma porta se fechou', stay: 40 },
      { kind: 'wait', ms: 1500 },
      { kind: 'sound', sound: 'minecraft:block.iron_door.close', pitch: 0.6, volume: 1 },
      { kind: 'title', mode: 'subtitle', text: '§7§ooutra porta', stay: 40 },
      { kind: 'wait', ms: 1200 },
      { kind: 'sound', sound: 'minecraft:block.iron_door.close', pitch: 0.5, volume: 1 },
      { kind: 'title', mode: 'subtitle', text: '§4§ovocê não pode mais voltar', stay: 50 },
      { kind: 'wait', ms: 1000 },
      { kind: 'sound', sound: 'minecraft:block.netherite_block.break', pitch: 0.4, volume: 1 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 100, amplifier: 0 },
      { kind: 'title', text: '§0§l🔒', subtitle: '§4§ofechado', fadeIn: 10, stay: 80, fadeOut: 20 },
    ],
  },
]

export function HorrorPage() {
  return (
    <AtmosphereGrid
      category="horror"
      title="Horror Atmosphere"
      subtitle=""
      pageEmoji="👻"
      pageDescription="Glitches, sustos, vozes, sangramento de tela. Use no SMP de terror — alguns presets são intensos."
      presets={PRESETS}
      storageKey="liberthia.horror.customs"
      defaultEmoji="👻"
    />
  )
}
