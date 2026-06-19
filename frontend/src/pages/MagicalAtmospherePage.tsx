import { Atmosphere } from '../lib/atmosphere'
import { AtmosphereGrid } from '../components/AtmosphereGrid'

const PRESETS: Atmosphere[] = [
  {
    id: 'm_enchant', emoji: '✨', name: 'Encantamento',
    description: 'Enchant particles + portal sound + texto twinkle',
    category: 'magical',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.enchantment_table.use', volume: 1.2 },
      { kind: 'loop', iterations: 5, gapMs: 200, steps: [
        { kind: 'particle', particle: 'minecraft:enchant', relX: 0, relY: 2, relZ: 0, count: 30 },
      ]},
      { kind: 'title', text: '§d§l✨ MAGIA', subtitle: '§5feitiço se forma', fadeIn: 10, stay: 80, fadeOut: 20 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 200, amplifier: 1 },
    ],
  },
  {
    id: 'm_portal', emoji: '🌀', name: 'Portal Místico',
    description: 'Portal sound contínuo + partículas + teleport vibe',
    category: 'magical',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.portal.ambient', volume: 1 },
      { kind: 'wait', ms: 800 },
      { kind: 'sound', sound: 'minecraft:entity.enderman.teleport', volume: 1.2 },
      { kind: 'loop', iterations: 12, gapMs: 100, steps: [
        { kind: 'particle', particle: 'minecraft:portal', relX: 0, relY: 1.5, relZ: 0, count: 20 },
      ]},
      { kind: 'title', text: '§5§l🌀 PORTAL', subtitle: '§docasos se entrelaçam', fadeIn: 15, stay: 100, fadeOut: 25 },
    ],
  },
  {
    id: 'm_amethyst', emoji: '💎', name: 'Cantos de Ametista',
    description: 'Camadas de chimes + ressonância + glow',
    category: 'magical',
    steps: [
      { kind: 'loop', iterations: 8, gapMs: 300, steps: [
        { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 1.2 + Math.random() * 0.6, volume: 0.6 },
        { kind: 'particle', particle: 'minecraft:dust_plume', relX: 0, relY: 1.5, relZ: 0, count: 5 },
      ]},
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.resonate', volume: 1.5 },
      { kind: 'title', text: '§d§l💎 RESSONÂNCIA', subtitle: '§5cristais cantam', fadeIn: 20, stay: 100, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'm_summoning', emoji: '🔮', name: 'Invocação',
    description: 'Evoker spell + dramatic build + dragon_breath final',
    category: 'magical',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.evoker.prepare_summon', volume: 1 },
      { kind: 'title', text: '§5§l🔮 INVOCAÇÃO', subtitle: '§7algo é chamado deste lado...', fadeIn: 15, stay: 60, fadeOut: 10 },
      { kind: 'wait', ms: 2000 },
      { kind: 'sound', sound: 'minecraft:entity.evoker.cast_spell', volume: 1.2 },
      { kind: 'loop', iterations: 6, gapMs: 150, steps: [
        { kind: 'particle', particle: 'minecraft:dragon_breath', relX: 0, relY: 0.5, relZ: 0, count: 25 },
      ]},
      { kind: 'wait', ms: 1500 },
      { kind: 'flash', text: '✦', flashes: 4, intervalMs: 120, colors: ['§d', '§5', '§f'] },
      { kind: 'title', text: '§d§l✦ ELE VEIO ✦', subtitle: '§5do outro lado do véu', fadeIn: 10, stay: 80, fadeOut: 30 },
    ],
  },
  {
    id: 'm_fairy', emoji: '🧚', name: 'Trilha das Fadas',
    description: 'Allay sounds + heart particles + cherry leaves',
    category: 'magical',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.allay.ambient_with_item', volume: 1 },
      { kind: 'loop', iterations: 10, gapMs: 200, steps: [
        { kind: 'particle', particle: 'minecraft:heart', relX: 0, relY: 2, relZ: 0, count: 4 },
        { kind: 'particle', particle: 'minecraft:cherry_leaves', relX: 0, relY: 3, relZ: 0, count: 10 },
      ]},
      { kind: 'title', text: '§d§l🧚 Fadas Visitam', subtitle: '§7algo gentil passou por aqui', fadeIn: 25, stay: 100, fadeOut: 40 },
      { kind: 'effect', effect: 'minecraft:luck', duration: 600, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 100, amplifier: 0 },
    ],
  },
  {
    id: 'm_starfall', emoji: '🌠', name: 'Chuva de Estrelas',
    description: 'Noite + end_rod particles caindo + chimes',
    category: 'magical',
    steps: [
      { kind: 'time', ticks: 18000 },
      { kind: 'weather', type: 'clear', duration: 12000 },
      { kind: 'loop', iterations: 15, gapMs: 250, steps: [
        { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 10, relZ: 0, count: 15 },
        { kind: 'sound', sound: 'minecraft:block.note_block.chime', pitch: 1.5, volume: 0.4 },
      ]},
      { kind: 'title', text: '§b§l🌠 Chuva de Estrelas', subtitle: '§7olhe pra cima', fadeIn: 30, stay: 120, fadeOut: 40 },
    ],
  },
  // ============ +4 PRESETS NOVOS ============
  {
    id: 'm_fairy_ring', emoji: '🍄', name: 'Anel de Fadas',
    description: 'Particle de happy_villager em círculo + soft chime + speed II + glow',
    category: 'magical',
    steps: [
      { kind: 'time', ticks: 16000 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 300, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 200, amplifier: 0 },
      { kind: 'loop', iterations: 12, gapMs: 200, steps: [
        { kind: 'particle', particle: 'minecraft:happy_villager', relX: 0, relY: 0, relZ: 0, count: 40 },
        { kind: 'particle', particle: 'minecraft:cherry_leaves', relX: 0, relY: 0, relZ: 0, count: 20 },
        { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 1.4, volume: 0.3 },
      ]},
      { kind: 'title', text: '§a§l🍄 Anel das Fadas', subtitle: '§dvocê foi escolhido', fadeIn: 20, stay: 100, fadeOut: 30 },
    ],
  },
  {
    id: 'm_mana_surge', emoji: '💫', name: 'Surto de Mana',
    description: 'Cura instantânea + strength + jump_boost + particles brancas + chime alto',
    category: 'magical',
    steps: [
      { kind: 'effect', effect: 'minecraft:instant_health', duration: 1, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:strength', duration: 600, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:jump_boost', duration: 600, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 600, amplifier: 0 },
      { kind: 'particle', particle: 'minecraft:enchant', relX: 0, relY: 0, relZ: 0, count: 200 },
      { kind: 'sound', sound: 'minecraft:block.beacon.activate', pitch: 1.2, volume: 1 },
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 1.8, volume: 0.8 },
      { kind: 'title', text: '§b§l💫 MANA', subtitle: '§7poder corre nas veias', fadeIn: 5, stay: 80, fadeOut: 30 },
    ],
  },
  {
    id: 'm_summoning_circle', emoji: '⛤', name: 'Círculo de Invocação',
    description: 'Particle ring + soul_fire + portal sound + título — algo é chamado',
    category: 'magical',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.resonate', pitch: 0.6, volume: 1 },
      { kind: 'loop', iterations: 8, gapMs: 250, steps: [
        { kind: 'particle', particle: 'minecraft:soul_fire_flame', relX: 0, relY: 0, relZ: 0, count: 30 },
        { kind: 'particle', particle: 'minecraft:enchant', relX: 0, relY: 0, relZ: 0, count: 50 },
      ]},
      { kind: 'particle', particle: 'minecraft:dragon_breath', relX: 0, relY: 0, relZ: 0, count: 80 },
      { kind: 'sound', sound: 'minecraft:block.portal.trigger', pitch: 0.8, volume: 1 },
      { kind: 'title', text: '§5§l⛤ INVOCAÇÃO', subtitle: '§do círculo se fecha', fadeIn: 10, stay: 80, fadeOut: 20 },
      { kind: 'wait', ms: 1500 },
      { kind: 'sound', sound: 'minecraft:entity.evoker.cast_spell', pitch: 0.7, volume: 1 },
    ],
  },
  {
    id: 'm_blessed', emoji: '✨', name: 'Bênção Antiga',
    description: 'Resistance + regen + fire_resistance + golden particles — você é blindado',
    category: 'magical',
    steps: [
      { kind: 'effect', effect: 'minecraft:resistance', duration: 1200, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:regeneration', duration: 600, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:fire_resistance', duration: 1200, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:saturation', duration: 200, amplifier: 1 },
      { kind: 'particle', particle: 'minecraft:totem_of_undying', relX: 0, relY: 0, relZ: 0, count: 100 },
      { kind: 'sound', sound: 'minecraft:item.totem.use', pitch: 1, volume: 1 },
      { kind: 'title', text: '§6§l✨ BÊNÇÃO', subtitle: '§eos antigos sorriem pra você', fadeIn: 15, stay: 120, fadeOut: 30 },
    ],
  },
]

export function MagicalAtmospherePage() {
  return (
    <AtmosphereGrid
      category="magical"
      title="Magical Atmosphere"
      subtitle=""
      pageEmoji="🔮"
      pageDescription="Encantamento, portais, ametistas, fadas. Mood arcano pra rituais e magia."
      presets={PRESETS}
      storageKey="liberthia.magical.customs"
      defaultEmoji="🔮"
    />
  )
}
