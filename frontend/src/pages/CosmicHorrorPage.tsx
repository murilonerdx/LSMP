import { Atmosphere } from '../lib/atmosphere'
import { AtmosphereGrid } from '../components/AtmosphereGrid'

/**
 * Cosmic Horror — terror cósmico, non-sense, quebra de realidade.
 * Diferente do Horror normal (jumpscares, sustos físicos), aqui é horror
 * de percepção: nausea, darkness, distorção, gravidade errada, déjà vu.
 *
 * Vanilla MC não tem "inverter mouse" — usamos nausea (que distorce a
 * câmera de forma similar). Pra "imagem na tela" usamos chars de bloco
 * unicode em titles fullscreen.
 */

const PRESETS: Atmosphere[] = [
  // ============== Quebra de realidade ==============
  {
    id: 'c_reality_break', emoji: '🌀', name: 'Quebra de Realidade',
    description: 'Dia/noite alternando + nausea + thunder súbitos — o tempo quebra',
    category: 'cosmic',
    steps: [
      { kind: 'effect', effect: 'minecraft:nausea', duration: 300, amplifier: 1 },
      { kind: 'loop', iterations: 6, gapMs: 800, steps: [
        { kind: 'random', count: 1, choices: [
          { kind: 'time', ticks: 0 },
          { kind: 'time', ticks: 6000 },
          { kind: 'time', ticks: 13000 },
          { kind: 'time', ticks: 18000 },
        ]},
        { kind: 'sound', sound: 'minecraft:block.portal.trigger', pitch: 0.4, volume: 0.7 },
      ]},
      { kind: 'title', text: '§5§l⊗ A REALIDADE QUEBRA', subtitle: '§7tempo já não obedece', fadeIn: 5, stay: 80, fadeOut: 20 },
      { kind: 'sound', sound: 'minecraft:entity.elder_guardian.curse', pitch: 0.3 },
    ],
  },

  // ============== Olhos no escuro ==============
  {
    id: 'c_eyes', emoji: '👁', name: 'Olhos no Escuro',
    description: 'Darkness pulsante + warden heartbeat + sussurros aleatórios',
    category: 'cosmic',
    steps: [
      { kind: 'effect', effect: 'minecraft:darkness', duration: 400, amplifier: 0 },
      { kind: 'loop', iterations: 8, gapMs: 700, steps: [
        { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.7, volume: 0.8 },
        { kind: 'random', count: 1, choices: [
          { kind: 'title', mode: 'subtitle', text: '§8§o*algo te observa*', stay: 30 },
          { kind: 'title', mode: 'subtitle', text: '§0§o*eles sabem seu nome*', stay: 30 },
          { kind: 'title', mode: 'subtitle', text: '§7§o*você não tá sozinho aqui*', stay: 30 },
          { kind: 'title', mode: 'subtitle', text: '§4§o*olhe pra trás. devagar*', stay: 30 },
        ]},
      ]},
    ],
  },

  // ============== Gravidade errada ==============
  {
    id: 'c_wrong_gravity', emoji: '🪐', name: 'Gravidade Errada',
    description: 'Levitation curto + slow_falling alternando — você não tem peso',
    category: 'cosmic',
    steps: [
      { kind: 'title', text: '§5§l🪐 §dPESO PERDIDO', subtitle: '§7em qual direção é baixo?', fadeIn: 10, stay: 60, fadeOut: 15 },
      { kind: 'sound', sound: 'minecraft:entity.enderman.teleport', pitch: 0.5 },
      { kind: 'effect', effect: 'minecraft:levitation', duration: 40, amplifier: 1 },
      { kind: 'wait', ms: 2000 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 80, amplifier: 0 },
      { kind: 'wait', ms: 2000 },
      { kind: 'effect', effect: 'minecraft:levitation', duration: 30, amplifier: 0 },
      { kind: 'wait', ms: 1500 },
      { kind: 'effect', effect: 'minecraft:slow_falling', duration: 100, amplifier: 0 },
    ],
  },

  // ============== Mente invertida ==============
  {
    id: 'c_inverted', emoji: '🔄', name: 'Mente Invertida',
    description: 'Nausea forte + slowness + sons em pitch invertido — o cérebro reseta',
    category: 'cosmic',
    steps: [
      { kind: 'effect', effect: 'minecraft:nausea', duration: 400, amplifier: 2 },
      { kind: 'effect', effect: 'minecraft:slowness', duration: 200, amplifier: 3 },
      { kind: 'effect', effect: 'minecraft:weakness', duration: 200, amplifier: 1 },
      { kind: 'title', text: '§5§l🔄 INVERSÃO', subtitle: '§dseus sentidos mentem', fadeIn: 5, stay: 100, fadeOut: 20 },
      { kind: 'loop', iterations: 6, gapMs: 250, steps: [
        { kind: 'sound', sound: 'minecraft:block.note_block.didgeridoo', pitch: 0.5 },
        { kind: 'sound', sound: 'minecraft:block.note_block.didgeridoo', pitch: 0.6 },
      ]},
    ],
  },

  // ============== Saturação cósmica ==============
  {
    id: 'c_saturation', emoji: '🌈', name: 'Saturação Insana',
    description: 'Glow em tudo + speed III + nausea — overload de percepção',
    category: 'cosmic',
    steps: [
      { kind: 'effect', effect: 'minecraft:glowing', duration: 400, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:speed', duration: 300, amplifier: 2 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 200, amplifier: 0 },
      { kind: 'flash', text: '§l✦●✦●✦●', flashes: 15, intervalMs: 80,
        colors: ['§a', '§b', '§c', '§d', '§e', '§6'], sound: 'minecraft:block.amethyst_block.chime' },
      { kind: 'title', text: '§d§l🌈 EXCESSO', subtitle: '§7sentir tudo de uma vez é demais', fadeIn: 5, stay: 80, fadeOut: 20 },
    ],
  },

  // ============== Déjà vu / Loop temporal ==============
  {
    id: 'c_deja_vu', emoji: '⟲', name: 'Déjà Vu',
    description: 'Mesma sequência se repete 3× idêntica — você já viu isso',
    category: 'cosmic',
    steps: [
      { kind: 'loop', iterations: 3, gapMs: 1500, steps: [
        { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 0.8 },
        { kind: 'title', text: '§7§l⟲ §dvocê já viu isso', subtitle: '§8§ohá quanto tempo?', fadeIn: 8, stay: 40, fadeOut: 8 },
        { kind: 'effect', effect: 'minecraft:nausea', duration: 30, amplifier: 0 },
      ]},
      { kind: 'wait', ms: 1500 },
      { kind: 'title', text: '§5§l⟲', subtitle: '§5§oou é a primeira vez?', fadeIn: 10, stay: 80, fadeOut: 30 },
    ],
  },

  // ============== Voz na cabeça ==============
  {
    id: 'c_voice_in_head', emoji: '🗣', name: 'Voz na Cabeça',
    description: 'Sussurros estranhos em loop — vozes que não deveriam existir',
    category: 'cosmic',
    steps: [
      { kind: 'loop', iterations: 10, gapMs: 700, steps: [
        { kind: 'random', count: 1, choices: [
          { kind: 'title', mode: 'actionbar', text: '§5§oeu sei o que você fez' },
          { kind: 'title', mode: 'actionbar', text: '§d§ovocê me ouve não me ouve' },
          { kind: 'title', mode: 'actionbar', text: '§8§oolhe pra dentro' },
          { kind: 'title', mode: 'actionbar', text: '§4§onão sou seu pensamento' },
          { kind: 'title', mode: 'actionbar', text: '§5§ovocê é a casca' },
        ]},
        { kind: 'sound', sound: 'minecraft:entity.warden.heartbeat', pitch: 0.5, volume: 0.3 },
      ]},
    ],
  },

  // ============== Bloco branco fullscreen (imagem na tela) ==============
  {
    id: 'c_white_void', emoji: '⬜', name: 'Vazio Branco',
    description: 'Title fullscreen branco + silêncio + texto na tela — sem mundo',
    category: 'cosmic',
    steps: [
      { kind: 'title', text: '§f█████████████████████', subtitle: '§f█████████████████████', fadeIn: 30, stay: 150, fadeOut: 30 },
      { kind: 'effect', effect: 'minecraft:blindness', duration: 200, amplifier: 0 },
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.resonate', pitch: 0.3 },
      { kind: 'wait', ms: 3000 },
      { kind: 'title', text: '§8§l⊘', subtitle: '§7§oo nada também é um lugar', fadeIn: 30, stay: 100, fadeOut: 30 },
    ],
  },

  // ============== Doppelganger ==============
  {
    id: 'c_doppel', emoji: '👥', name: 'Doppelgänger',
    description: 'Spawn de armor stand invisível com seu nome ao lado + sons',
    category: 'cosmic',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.enderman.teleport', pitch: 0.6 },
      { kind: 'command', cmd: 'execute as @a at @s run summon armor_stand ~2 ~ ~ {Invisible:1b,CustomName:\'{"text":"§7§o???","color":"gray"}\',CustomNameVisible:1b,Tags:["liberthia_doppel"]}' },
      { kind: 'wait', ms: 1500 },
      { kind: 'title', text: '§8§l👥', subtitle: '§7§oalguém com seu rosto está perto', fadeIn: 15, stay: 80, fadeOut: 20 },
      { kind: 'wait', ms: 4000 },
      { kind: 'command', cmd: 'kill @e[tag=liberthia_doppel]' },
    ],
  },

  // ============== Sussurro do próprio nome ==============
  {
    id: 'c_my_name', emoji: '📛', name: 'Sussurro do Nome',
    description: 'Random sound + título com nome do player letra por letra',
    category: 'cosmic',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.allay.ambient_without_item', pitch: 0.3, volume: 0.6 },
      { kind: 'title', text: ' ', subtitle: '§8§oeles estão dizendo...', fadeIn: 10, stay: 60, fadeOut: 10 },
      { kind: 'wait', ms: 2000 },
      { kind: 'garble', text: 'eu sei seu nome', iterations: 10, intervalMs: 100, color: '§8',
        finalText: '§5§lE U   S E I   S E U   N O M E' },
    ],
  },

  // ============== A escolha errada ==============
  {
    id: 'c_wrong_choice', emoji: '🜲', name: 'A Escolha Errada',
    description: 'Glow + darkness alternando + sussurros — você tomou a decisão errada',
    category: 'cosmic',
    steps: [
      { kind: 'effect', effect: 'minecraft:glowing', duration: 100, amplifier: 0 },
      { kind: 'wait', ms: 800 },
      { kind: 'effect', effect: 'minecraft:darkness', duration: 60, amplifier: 0 },
      { kind: 'sound', sound: 'minecraft:entity.warden.sonic_charge', pitch: 0.7 },
      { kind: 'title', text: '§4§l🜲 ERRADA', subtitle: '§c§oa escolha foi errada', fadeIn: 5, stay: 60, fadeOut: 15 },
      { kind: 'effect', effect: 'minecraft:wither', duration: 40, amplifier: 0 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 120, amplifier: 0 },
    ],
  },

  // ============== Tempo parado ==============
  {
    id: 'c_frozen_time', emoji: '⌛', name: 'Tempo Parado',
    description: 'Slowness V + título fullscreen — você está congelado no tempo',
    category: 'cosmic',
    steps: [
      { kind: 'effect', effect: 'minecraft:slowness', duration: 300, amplifier: 4 },
      { kind: 'effect', effect: 'minecraft:mining_fatigue', duration: 300, amplifier: 2 },
      { kind: 'title', text: '§b§l⌛ ESTANCADO', subtitle: '§7§oo mundo continua. você não.', fadeIn: 20, stay: 150, fadeOut: 30 },
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 0.4 },
    ],
  },
  // ============ +4 PRESETS NOVOS ============
  {
    id: 'c_geometry_break', emoji: '◐', name: 'Geometria Quebrada',
    description: 'Glow alternando + particle de portal em si próprio — o espaço deforma',
    category: 'cosmic',
    steps: [
      { kind: 'particle', particle: 'minecraft:portal', relX: 0, relY: 0, relZ: 0, count: 200 },
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.resonate', pitch: 0.4, volume: 0.8 },
      { kind: 'effect', effect: 'minecraft:glowing', duration: 80, amplifier: 0 },
      { kind: 'wait', ms: 1500 },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 200, amplifier: 1 },
      { kind: 'title', text: '§5§l◐ §dGEOMETRIA', subtitle: '§7as linhas não fazem mais sentido', fadeIn: 5, stay: 80, fadeOut: 15 },
      { kind: 'loop', iterations: 4, gapMs: 800, steps: [
        { kind: 'particle', particle: 'minecraft:reverse_portal', relX: 0, relY: 0, relZ: 0, count: 50 },
        { kind: 'sound', sound: 'minecraft:block.portal.ambient', pitch: 0.5 },
      ]},
    ],
  },
  {
    id: 'c_alien_language', emoji: '🜲', name: 'Língua Alienígena',
    description: 'Texto com caracteres impossíveis no chat + comando para outro player',
    category: 'cosmic',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.elder_guardian.curse', pitch: 0.6, volume: 0.7 },
      { kind: 'title', mode: 'subtitle', text: '§5§oalguém está te chamando', stay: 50 },
      { kind: 'wait', ms: 1200 },
      { kind: 'chat', message: '§5§l⌬ ⩜ ⌬ ⩘ ⌬ ⪧ ⌬' },
      { kind: 'wait', ms: 800 },
      { kind: 'chat', message: '§5§l◈ ⨁ ◈ ⫷ ◈ ⫸ ◈' },
      { kind: 'wait', ms: 800 },
      { kind: 'chat', message: '§5§l⩘ ⪧ ⌬ ⪧ ⩜ ⪧ ⌬' },
      { kind: 'sound', sound: 'minecraft:block.amethyst_block.chime', pitch: 0.2, volume: 1 },
      { kind: 'title', text: '§5§l🜲', subtitle: '§5§ovocê quase entendeu', fadeIn: 10, stay: 80, fadeOut: 20 },
    ],
  },
  {
    id: 'c_void_pull', emoji: '⊗', name: 'Atração do Vazio',
    description: 'Levitation + sound de vento + chama de morte — algo te puxa pra cima',
    category: 'cosmic',
    steps: [
      { kind: 'sound', sound: 'minecraft:entity.ender_dragon.growl', pitch: 0.3, volume: 0.6 },
      { kind: 'title', text: '§0§l⊗ §5§l...', subtitle: '§5§oalgo te chama de cima', fadeIn: 10, stay: 60, fadeOut: 15 },
      { kind: 'wait', ms: 1000 },
      { kind: 'effect', effect: 'minecraft:levitation', duration: 100, amplifier: 0 },
      { kind: 'sound', sound: 'minecraft:item.elytra.flying', pitch: 0.4, volume: 1 },
      { kind: 'loop', iterations: 5, gapMs: 400, steps: [
        { kind: 'particle', particle: 'minecraft:end_rod', relX: 0, relY: 0, relZ: 0, count: 30 },
        { kind: 'sound', sound: 'minecraft:entity.phantom.flap', pitch: 0.5 },
      ]},
      { kind: 'title', text: '§5§l⊗', subtitle: '§8§onão olhe pra baixo', fadeIn: 5, stay: 80, fadeOut: 30 },
    ],
  },
  {
    id: 'c_mirror_self', emoji: '🪞', name: 'Espelho do Eu',
    description: 'Spawn de armor stand com o nome do player + sons espelhados',
    category: 'cosmic',
    steps: [
      { kind: 'sound', sound: 'minecraft:block.glass.break', pitch: 0.3, volume: 1 },
      { kind: 'particle', particle: 'minecraft:glow', relX: 0, relY: 0, relZ: 0, count: 60 },
      { kind: 'title', text: '§7§l🪞', subtitle: '§dvocê está se vendo de fora', fadeIn: 10, stay: 60, fadeOut: 15 },
      { kind: 'command', cmd: 'execute as @s at @s run summon armor_stand ~ ~ ~3 {Invisible:0b,ShowArms:1b,CustomName:\'{"text":"§5§ovocê","color":"dark_purple"}\',CustomNameVisible:1b,Tags:["liberthia_mirror"]}' },
      { kind: 'effect', effect: 'minecraft:nausea', duration: 120, amplifier: 1 },
      { kind: 'wait', ms: 4000 },
      { kind: 'sound', sound: 'minecraft:entity.glass_break', pitch: 0.5, volume: 1 },
      { kind: 'command', cmd: 'kill @e[tag=liberthia_mirror]' },
      { kind: 'title', text: '§4§l⨯', subtitle: '§c§oele se foi. ou foi você?', fadeIn: 10, stay: 80, fadeOut: 30 },
    ],
  },
]

export function CosmicHorrorPage() {
  return (
    <AtmosphereGrid
      category="cosmic"
      title="Cosmic Horror"
      subtitle=""
      pageEmoji="🜲"
      pageDescription="Horror non-sense. Quebra de percepção, nausea, déjà vu, gravidade errada. Pra romper a sanidade do player sem matar."
      presets={PRESETS}
      storageKey="liberthia.cosmic.customs"
      defaultEmoji="🜲"
    />
  )
}
