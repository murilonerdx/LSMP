import { Atmosphere } from '../lib/atmosphere'
import { AtmosphereGrid } from '../components/AtmosphereGrid'

const PRESETS: Atmosphere[] = [
  {
    id: 'd_dawn', emoji: '☀', name: 'Amanhecer Sagrado',
    description: 'Manhã serena, sino + bênção dourada + partículas brancas',
    category: 'divine',
    steps: [
      { kind: 'time', ticks: 23000 },
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'wait', ms: 1000 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.8, volume: 1 },
      { kind: 'title', text: '§e§l☀ Amanhecer', subtitle: '§7a luz retorna ao mundo', fadeIn: 20, stay: 80, fadeOut: 30 },
      { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 3, relZ: 0, count: 40 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:saturation', duration: 200, amplifier: 1 },
    ],
  },
  {
    id: 'd_beacon', emoji: '✨', name: 'Pilar de Luz',
    description: 'Beacon activate + partículas verticais + título dourado',
    category: 'divine',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.beacon.activate', volume: 1.2 },
      { kind: 'title', text: '§6§l✦ ASCENSÃO ✦', subtitle: '§eUm caminho se abre', fadeIn: 15, stay: 100, fadeOut: 30 },
      { kind: 'loop', iterations: 10, gapMs: 100, steps: [
        { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 5, relZ: 0, count: 8 },
      ]},
      { kind: 'effect', effect: 'minecraft:levitation', duration: 60, amplifier: 1 },
      { kind: 'wait', ms: 3500 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 100, amplifier: 0 },
    ],
  },
  {
    id: 'd_blessing', emoji: '🕊', name: 'Bênção',
    description: 'Chime + texto dourado + heart particles + speed',
    category: 'divine',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 1.4, volume: 1 },
      { kind: 'title', text: '§6§l🕊 Bênção', subtitle: '§eos céus te observam', fadeIn: 15, stay: 80, fadeOut: 20 },
      { kind: 'particle', particle: 'minecraft:heart', relX: 0, relY: 2, relZ: 0, count: 15 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 2 },
      { kind: 'effect', effect: 'minecraft:resistance', duration: 400, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:strength', duration: 400, amplifier: 0 },
    ],
  },
  {
    id: 'd_revelation', emoji: '👁', name: 'Revelação',
    description: 'Texto pulsante dourado + chime + glow',
    category: 'divine',
    steps: [
      { kind: 'time', ticks: 6000 },
      { kind: 'flash', text: '✦', flashes: 6, intervalMs: 200, colors: ['§e', '§6', '§f'], sound: 'minecraft:block.amethyst_block.chime' },
      { kind: 'title', text: '§e§l A VERDADE ', subtitle: '§7se revela diante de você', fadeIn: 20, stay: 100, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'd_holy_choir', emoji: '⛪', name: 'Coro Celestial',
    description: 'Camada de chimes + bells + título azul-claro',
    category: 'divine',
    steps: [
      { kind: 'loop', iterations: 6, gapMs: 250, steps: [
        { kind: 'sound', sound: 'minecraft:block.note_block.chime', pitch: 1.5, volume: 0.6 },
        { kind: 'sound', sound: 'minecraft:block.note_block.bell', pitch: 1.2, volume: 0.4 },
      ]},
      { kind: 'title', text: '§b§l⛪ Coro Celestial', subtitle: '§7vozes além do véu cantam', fadeIn: 25, stay: 120, fadeOut: 40 },
      { kind: 'particle', particle: 'minecraft:enchant', relX: 0, relY: 3, relZ: 0, count: 30 },
    ],
  },
  {
    id: 'd_sanctuary', emoji: '⛪', name: 'Santuário',
    description: 'Dia + clear + resistance + título azul',
    category: 'divine',
    steps: [
      { kind: 'time', ticks: 6000 },
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'sound', sound: 'minecraft:block.beacon.activate', volume: 1 },
      { kind: 'title', text: '§b§l✦ Santuário', subtitle: '§7você está seguro aqui', fadeIn: 20, stay: 80, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:resistance', duration: 600, amplifier: 1 },
    ],
  },
  // ============ +4 PRESETS NOVOS ============
  {
    id: 'd_ascension', emoji: '🕊', name: 'Ascensão',
    description: 'Slow_falling + glowing + chimes celestiais — você flutua suave pra cima',
    category: 'divine',
    steps: [
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 200, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:levitation', duration: 80, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 200, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 1 },
      { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 0, relZ: 0, count: 100 },
      { kind: 'particle', particle: 'minecraft:enchant', relX: 0, relY: 0, relZ: 0, count: 100 },
      { kind: 'sound', sound: 'minecraft:block.beacon.power_select', pitch: 1.2, volume: 1 },
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 2, volume: 0.8 },
      { kind: 'title', text: '§e§l🕊 ASCENSÃO', subtitle: '§7suba — eles te esperam', fadeIn: 20, stay: 120, fadeOut: 30 },
    ],
  },
  {
    id: 'd_holy_judgement', emoji: '⚖', name: 'Julgamento Divino',
    description: 'Thunder do céu claro + título dourado + slowness — você está sendo pesado',
    category: 'divine',
    steps: [
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'time', ticks: 6000 },
      { kind: 'sound', sound: 'minecraft:item.trident.thunder', pitch: 0.6, volume: 2 },
      { kind: 'wait', ms: 800 },
      { kind: 'flash', text: '§e§l⚖', flashes: 3, intervalMs: 200, colors: ['§e', '§6', '§f'] },
      { kind: 'effect', effect: 'minecraft:slowness', duration: 100, amplifier: 1 },
      { kind: 'title', text: '§e§l⚖ JULGAMENTO', subtitle: '§6sua alma está sendo medida', fadeIn: 10, stay: 100, fadeOut: 20 },
      { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.5, volume: 2 },
      { kind: 'wait', ms: 1500 },
      { kind: 'sound', sound: 'minecraft:block.beacon.deactivate', pitch: 1, volume: 1 },
    ],
  },
  {
    id: 'd_seraph_song', emoji: '🎶', name: 'Canto dos Serafins',
    description: 'Bell + chime + harp + regen — música celestial em camadas',
    category: 'divine',
    steps: [
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 1 },
      { kind: 'loop', iterations: 6, gapMs: 600, steps: [
        { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 1.5, volume: 0.6 },
        { kind: 'sound', sound: 'minecraft:block.note_block.harp', pitch: 1.8, volume: 0.5 },
        { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 2, volume: 0.4 },
        { kind: 'particle', particle: 'minecraft:note', relX: 0, relY: 0, relZ: 0, count: 30 },
      ]},
      { kind: 'title', text: '§e§l🎶', subtitle: '§7a canção que cura', fadeIn: 30, stay: 100, fadeOut: 40 },
      { kind: 'effect', effect: 'minecraft:saturation', duration: 100, amplifier: 0 },
    ],
  },
  {
    id: 'd_sunrise_blessing', emoji: '🌅', name: 'Aurora Sagrada',
    description: 'Set time pra alvorecer + weather clear + speed + regen — novo começo',
    category: 'divine',
    steps: [
      { kind: 'time', ticks: 0 },
      { kind: 'weather', type: 'clear', duration: 24000 },
      { kind: 'sound', sound: 'minecraft:entity.cat.purreow', pitch: 1.5, volume: 0.8 },
      { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 5, relZ: 0, count: 60 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 600, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 200, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:saturation', duration: 100, amplifier: 0 },
      { kind: 'title', text: '§e§l🌅 AURORA', subtitle: '§6um novo dia te perdoa', fadeIn: 30, stay: 120, fadeOut: 30 },
      { kind: 'sound', sound: 'minecraft:block.beacon.activate', pitch: 1.5, volume: 1 },
    ],
  },
]

export function DivineAtmospherePage() {
  return (
    <AtmosphereGrid
      category="divine"
      title="Divine Atmosphere"
      subtitle=""
      pageEmoji="✨"
      pageDescription="Mood sagrado, bênção, ascensão. Luz dourada, sinos, regen e resistance."
      presets={PRESETS}
      storageKey="liberthia.divine.customs"
      defaultEmoji="✨"
    />
  )
}
