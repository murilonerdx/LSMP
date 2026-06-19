import { Atmosphere } from '../lib/atmosphere'
import { AtmosphereGrid } from '../components/AtmosphereGrid'

const PRESETS: Atmosphere[] = [
  {
    id: 'a_fall', emoji: '🌋', name: 'A Queda',
    description: 'Tempestade + thunder rolling + título sangrento + nausea',
    category: 'apocalyptic',
    steps: [
      { kind: 'weather', type: 'thunder', duration: 1200 },
      { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.3, volume: 2 },
      { kind: 'wait', ms: 400 },
      { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.5, volume: 1.5 },
      { kind: 'title', text: '§4§l🌋 A QUEDA', subtitle: '§co mundo se rasga', fadeIn: 10, stay: 80, fadeOut: 20 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 100, amplifier: 1 },
    ],
  },
  {
    id: 'a_wither_call', emoji: '☠', name: 'Chamado do Wither',
    description: 'Wither spawn + corrupção + dano gradual',
    category: 'apocalyptic',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.wither.spawn', volume: 1.5 },
      { kind: 'flash', text: '☠', flashes: 5, intervalMs: 150, colors: ['§5', '§8', '§0'] },
      { kind: 'title', text: '§5§l☠ ELE DESPERTA', subtitle: '§8o senhor da morte ouve', fadeIn: 15, stay: 100, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:wither', duration: 80, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:weakness', duration: 200, amplifier: 1 },
    ],
  },
  {
    id: 'a_inferno', emoji: '🔥', name: 'Inferno na Terra',
    description: 'Fogo + chamas + lava sounds + clima de queima',
    category: 'apocalyptic',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.fire.ambient', volume: 1.5 },
      { kind: 'loop', iterations: 8, gapMs: 200, steps: [
        { kind: 'particle', particle: 'minecraft:flame', relX: 0, relY: 1, relZ: 0, count: 30 },
        { kind: 'sound', sound: 'minecraft:block.fire.extinguish', pitch: 0.7, volume: 0.5 },
      ]},
      { kind: 'title', text: '§c§l🔥 INFERNO', subtitle: '§4§oa terra arde', fadeIn: 10, stay: 100, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:fire_resistance', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'a_dragon', emoji: '🐉', name: 'Sopro do Dragão',
    description: 'Dragon growl + dragon_breath particles + título roxo',
    category: 'apocalyptic',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.ender_dragon.growl', pitch: 0.5, volume: 2 },
      { kind: 'wait', ms: 1500 },
      { kind: 'title', text: '§5§l🐉 ELE VEM', subtitle: '§8o sopro de mil mortes', fadeIn: 15, stay: 80, fadeOut: 30 },
      { kind: 'loop', iterations: 6, gapMs: 200, steps: [
        { kind: 'particle', particle: 'minecraft:dragon_breath', relX: 0, relY: 1, relZ: 0, count: 30 },
      ]},
      { kind: 'effect', effect: 'minecraft:slowness', duration: 100, amplifier: 1 },
    ],
  },
  {
    id: 'a_doom', emoji: '🌪', name: 'Tempestade Final',
    description: 'Combo: thunder + raio + nausea + escuridão',
    category: 'apocalyptic',
    steps: [
      { kind: 'time', ticks: 18000 },
      { kind: 'weather', type: 'thunder', duration: 2400 },
      { kind: 'loop', iterations: 5, gapMs: 800, steps: [
        { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.4, volume: 1.5 },
        { kind: 'flash', text: '⚡', flashes: 2, intervalMs: 60, colors: ['§f', '§e'] },
        { kind: 'command', cmd: 'execute as @a at @s run summon lightning_bolt ~10 ~ ~10' },
      ]},
      { kind: 'title', text: '§4§l🌪 TEMPESTADE FINAL', subtitle: '§co fim chegou', fadeIn: 10, stay: 100, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'a_blood_rain', emoji: '🩸', name: 'Chuva de Sangue',
    description: 'Rain + dust_plume vermelha + título sangrento',
    category: 'apocalyptic',
    steps: [
      { kind: 'weather', type: 'rain', duration: 6000 },
      { kind: 'loop', iterations: 30, gapMs: 100, steps: [
        { kind: 'particle', particle: 'minecraft:dust_plume', relX: 0, relY: 8, relZ: 0, count: 12 },
      ]},
      { kind: 'title', text: '§4§l🩸 Chuva de Sangue', subtitle: '§co céu chora vermelho', fadeIn: 20, stay: 100, fadeOut: 30 },
      { kind: 'sound', sound: 'minecraft:entity.ghast.warn', pitch: 0.4, volume: 1 },
    ],
  },
  // ============ +4 PRESETS NOVOS ============
  {
    id: 'a_ash_storm', emoji: '🌫', name: 'Tempestade de Cinzas',
    description: 'Smoke contínuo + fadiga + escuridão progressiva — fim por sufoco',
    category: 'apocalyptic',
    steps: [
      { kind: 'weather', type: 'thunder', duration: 3000 },
      { kind: 'effect', effect: 'minecraft:slowness', duration: 300, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:mining_fatigue', duration: 300, amplifier: 1 },
      { kind: 'loop', iterations: 20, gapMs: 200, steps: [
        { kind: 'particle', particle: 'minecraft:campfire_signal_smoke', relX: 0, relY: 4, relZ: 0, count: 30 },
        { kind: 'particle', particle: 'minecraft:large_smoke', relX: 0, relY: 0, relZ: 0, count: 20 },
      ]},
      { kind: 'sound', sound: 'minecraft:entity.warden.emerge', pitch: 0.4, volume: 1 },
      { kind: 'title', text: '§8§l🌫 CINZAS', subtitle: '§7o ar já não é ar', fadeIn: 15, stay: 100, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 200, amplifier: 0 },
    ],
  },
  {
    id: 'a_meteor_strike', emoji: '☄️', name: 'Impacto de Meteoro',
    description: 'Explosão visual + flash branco + thunder + nausea — algo cai do céu',
    category: 'apocalyptic',
    steps: [
      { kind: 'title', mode: 'subtitle', text: '§e§oalgo brilha no céu...', stay: 40 },
      { kind: 'wait', ms: 1500 },
      { kind: 'particle', particle: 'minecraft:flame', relX: 0, relY: 20, relZ: 0, count: 100 },
      { kind: 'sound', sound: 'minecraft:entity.ender_dragon.flap', pitch: 0.4, volume: 2 },
      { kind: 'wait', ms: 1200 },
      { kind: 'flash', text: '☄', flashes: 3, intervalMs: 80, colors: ['§f', '§e', '§6'] },
      { kind: 'sound', sound: 'minecraft:entity.generic.explode', pitch: 0.3, volume: 2 },
      { kind: 'sound', sound: 'minecraft:entity.lightning_bolt.thunder', pitch: 0.3, volume: 2 },
      { kind: 'particle', particle: 'minecraft:explosion_emitter', relX: 0, relY: 0, relZ: 0, count: 5 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 100, amplifier: 1 },
      { kind: 'title', text: '§4§l☄ IMPACTO', subtitle: '§co céu entregou seu peso', fadeIn: 5, stay: 100, fadeOut: 30 },
    ],
  },
  {
    id: 'a_plague_bell', emoji: '🔔', name: 'Sino da Peste',
    description: 'Bell em loop lento + darkness + slowness — o sino que anuncia mortos',
    category: 'apocalyptic',
    steps: [
      { kind: 'effect', effect: 'minecraft:slowness', duration: 200, amplifier: 1 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 200, amplifier: 0 },
      { kind: 'loop', iterations: 7, gapMs: 1800, steps: [
        { kind: 'sound', sound: 'minecraft:block.bell.use', pitch: 0.5, volume: 1 },
        { kind: 'title', mode: 'subtitle', text: '§7§o*um sino dobra ao longe*', stay: 50 },
      ]},
      { kind: 'title', text: '§8§l🔔 PESTE', subtitle: '§4§oconte os mortos do dia', fadeIn: 10, stay: 100, fadeOut: 30 },
      { kind: 'sound', sound: 'minecraft:entity.zombie_villager.cure', pitch: 0.4, volume: 1 },
    ],
  },
  {
    id: 'a_ground_crack', emoji: '🌍', name: 'O Chão Racha',
    description: 'Tremor (slowness pulsante) + particles de bloco + crack sound',
    category: 'apocalyptic',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.deepslate.break', pitch: 0.3, volume: 2 },
      { kind: 'effect', effect: 'minecraft:slowness', duration: 30, amplifier: 4 },
      { kind: 'particle', particle: 'minecraft:block minecraft:cracked_deepslate_bricks', relX: 0, relY: 0, relZ: 0, count: 80 },
      { kind: 'wait', ms: 600 },
      { kind: 'sound', sound: 'minecraft:block.stone.break', pitch: 0.4, volume: 2 },
      { kind: 'effect', effect: 'minecraft:slowness', duration: 30, amplifier: 4 },
      { kind: 'particle', particle: 'minecraft:block minecraft:gravel', relX: 0, relY: 0, relZ: 0, count: 80 },
      { kind: 'wait', ms: 700 },
      { kind: 'sound', sound: 'minecraft:block.deepslate.break', pitch: 0.2, volume: 2 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 100, amplifier: 1 },
      { kind: 'title', text: '§4§l🌍 RACHADURA', subtitle: '§co solo não te sustenta mais', fadeIn: 10, stay: 100, fadeOut: 20 },
    ],
  },
]

export function ApocalypticAtmospherePage() {
  return (
    <AtmosphereGrid
      category="apocalyptic"
      title="Apocalyptic"
      subtitle=""
      pageEmoji="🌋"
      pageDescription="Fim do mundo, dragões, wither, chuva de sangue. Mood de fim dos tempos pra eventos finais."
      presets={PRESETS}
      storageKey="liberthia.apocalyptic.customs"
      defaultEmoji="🌋"
    />
  )
}
