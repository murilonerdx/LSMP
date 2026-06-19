# Spell Factory — Reference Completa (r151)

Cada spell é um JSON em `data/liberthia/spells/<id>.json`. **Todos os campos são opcionais** exceto `id`, `name`, `school`. Cada campo tem um default sensato.

---

## 📋 Campos de TOPO (root level)

| Campo | Tipo | Default | Descrição |
|---|---|---|---|
| `id` | string | **OBRIGATÓRIO** | ID único (ex: `"my_fireball"`) |
| `name` | string | **OBRIGATÓRIO** | Nome no jogo (ex: `"Bola de Fogo"`) |
| `lore` | string | `""` | Descrição lore no tooltip |
| `school` | enum | **OBRIGATÓRIO** | Ver tabela [Schools](#schools) |
| `rarity` | enum | `COMMON` | `COMMON`, `UNCOMMON`, `RARE`, `EPIC` |
| `category` | enum | `PROJECTILE` | Ver tabela [Categories](#categories) (12 tipos) |
| `element` | enum | `NONE` | Ver tabela [Elements](#elements) |
| `secondary_element` | enum | `NONE` | Combinação automática (FIRE+WATER=STEAM, etc) |
| `mana` | int | `10` | Custo em Source |
| `cooldown` | int | `40` | Ticks até poder usar de novo (20 = 1s) |
| `damage` | float | `4` | Dano base (multiplicado pelo element damage multiplier) |
| `range` | float | `16` | Alcance em blocos |
| `type` | enum | `PROJECTILE` | Ver tabela [Types](#spell-types) (13 tipos) |
| `behavior` | object | `{}` | [Behavior fields](#behavior-fields) |
| `vfx` | object | `{}` | [VFX fields](#vfx-fields) |
| `effects` | array | `[]` | [Status effects](#status-effects) |

---

## 🎯 Schools (7)

| Valor | Cor | Damage type |
|---|---|---|
| `FIRE` | laranja | incendia |
| `ICE` | ciano | congela |
| `LIGHTNING` | amarelo | chain |
| `BLOOD` | vermelho escuro | lifesteal |
| `ELDRITCH` | roxo | nausea/blindness |
| `HOLY` | dourado | undead 2× |
| `NATURE` | verde | poison/slowness |

---

## 🌍 Elements (10 — 4 base + 6 duais)

**Base** (use em `element`):
| Valor | Multiplicador dano |
|---|---|
| `NONE` | 1.0× |
| `WATER` | 1.0× |
| `FIRE` | 1.2× |
| `AIR` | 0.8× |
| `EARTH` | 1.3× |

**Duais** (combinações automáticas com `secondary_element`):
| Combo | Resultado | Multiplicador |
|---|---|---|
| FIRE + WATER | `STEAM` | 1.4× |
| FIRE + EARTH | `LAVA` | 1.6× |
| FIRE + AIR | `LIGHTNING` | 1.5× |
| WATER + AIR | `ICE` | 1.3× |
| WATER + EARTH | `MUD` | 1.1× |
| EARTH + AIR | `SAND` | 1.2× |

Se setar `element` + `secondary_element` de combos válidos, o **damage é automaticamente multiplicado**.

---

## 🎬 Categories (12)

Filtros temáticos (não afeta comportamento direto, só categorização):

`EXPLOSION`, `DESTRUCTION`, `DASH`, `RAY`, `CRITICAL`, `UTILITY`, `SUMMON`, `AOE`, `CHANNELED`, `INSTANT`, `PROJECTILE`, `MIXED`

---

## 🎮 Spell Types (13)

| Valor | Comportamento | Campos relevantes |
|---|---|---|
| `PROJECTILE` | Linha reta, hit-target | `speed`, `lifetime_ticks`, `pierce`, `aoe_radius` |
| `HOMING` | Trava no alvo + persegue | `speed`, `homing`, `lifetime_ticks` |
| `MULTI_SHOT` | N projéteis em cone | `projectile_count`, `spread_degrees` |
| `ARC` | Projétil com gravidade | `speed`, `lifetime_ticks` |
| `BEAM` | Feixe contínuo | `beam_max_ticks`, `beam_range` |
| `EXPLOSION` | AOE instantâneo no crosshair | `radius`, `height` |
| `NOVA` | Onda do caster | `radius` |
| `CONE` | Cone frontal | `radius`, `cone_angle` |
| `RAIN` | Strikes do céu | `radius`, `rain_strikes`, `rain_duration_ticks` |
| `AURA` | Pulsa em volta do caster | `radius`, `aura_duration_ticks`, `aura_tick_interval` |
| `TOUCH` | Single melee | `touch_range` |
| `SELF` | Buff no caster | (só effects) |
| `DASH` | Move caster | `dash_distance` |

---

## ⚙️ Behavior fields

### Projectile-like
| Campo | Tipo | Default | Range |
|---|---|---|---|
| `speed` | float | `1.4` | 0.5-3.0 |
| `lifetime_ticks` | int | `80` | 20-200 |
| `pierce` | bool | `false` | passa por entidades |
| `aoe_radius` | float | `0` | splash damage no impacto |
| `ignite_blocks` | bool | `false` | põe fogo no chão (FIRE only) |
| `projectile_count` | int | `1` | pra MULTI_SHOT |
| `spread_degrees` | float | `0` | ângulo do cone |
| `homing` | bool | `false` | só faz sentido se type=HOMING |

### Area effects
| Campo | Tipo | Default |
|---|---|---|
| `radius` | float | `3.0` |
| `cone_angle` | float | `60.0` |
| `rain_strikes` | int | `5` |
| `rain_duration_ticks` | int | `60` |

### Aura
| Campo | Tipo | Default |
|---|---|---|
| `aura_duration_ticks` | int | `60` |
| `aura_tick_interval` | int | `10` |

### Self/Touch/Dash
| Campo | Tipo | Default |
|---|---|---|
| `dash_distance` | float | `4.0` |
| `touch_range` | float | `4.0` |

### Beam
| Campo | Tipo | Default |
|---|---|---|
| `beam_max_ticks` | int | `40` |
| `beam_range` | float | `16.0` |

### **r151 NEW — Advanced**
| Campo | Tipo | Default | Descrição |
|---|---|---|---|
| `crit_chance` | float | `0` | 0..1 chance de crítico |
| `crit_multiplier` | float | `1.5` | multiplicador no crit |
| `knockback_strength` | float | `0` | empurrão no impacto |
| `height` | float | `3.0` | altura vertical de AOE |
| `self_shake` | bool | `false` | shake só no caster |
| `penetrate_blocks` | bool | `false` | projétil atravessa paredes |

---

## 🎨 VFX fields

### Cores
| Campo | Tipo | Default | Exemplo |
|---|---|---|---|
| `color_primary` | string/int | `-1` (usa school) | `"#ff5500"` |
| `color_secondary` | string/int | `-1` | `"#ffaa00"` |

### Trail (em vôo)
| Campo | Tipo | Default | Range |
|---|---|---|---|
| `trail_density` | int | `8` | 1-30 partículas/tick |
| `trail_size` | float | `1.2` | 0.3-3.0 escala |
| `trail_lifetime` | int | `18` | 10-30 ticks |

### Sprite (visual do projetil/trail)
| Valor `sprite` | Significado |
|---|---|
| `"auto"` | (default) Usa spritesheet da school — Fire=chama, Ice=floco, etc |
| `"orb"` | Orbe genérica |
| `"star"` | Estrela brilhante |
| `"hex"` | Hexágono arcano |
| `"arc"` | Raio elétrico |
| `"shard"` | Cristal afiado |

### Charging (mão)
| Campo | Tipo | Default |
|---|---|---|
| `charge_intensity` | float | `0.6` (0-1) |

### Impact burst
| Campo | Tipo | Default | Range |
|---|---|---|---|
| `impact_scale` | float | `1.0` | 0.3-4.0 |
| `impact_particles` | int | `80` | 5-500 |
| `impact_light` | int | `10` | 0-15 |

### Screen shake
| Campo | Tipo | Default | Range |
|---|---|---|---|
| `screen_shake` | float | `0.6` | 0-2.5 |
| `screen_shake_ticks` | int | `8` | 1-40 |

### Sons (resource locations OU "auto")
| Campo | Tipo | Default | Exemplo |
|---|---|---|---|
| `sound_cast_start` | string | `"auto"` | `"minecraft:entity.blaze.shoot"` |
| `sound_cast_finish` | string | `"auto"` | `"minecraft:entity.firework_rocket.launch"` |
| `sound_impact` | string | `"auto"` | `"minecraft:entity.generic.explode"` |

**`"auto"`** = usa default da school (FIRE=blaze sound, ICE=glass break, etc — ver `MagicSounds.java`).

**Exemplos de resource locations úteis:**
```
minecraft:entity.lightning_bolt.thunder    (raio)
minecraft:entity.enderman.teleport         (teleport)
minecraft:entity.wither.shoot              (sombrio)
minecraft:entity.dragon_fireball.explode   (épico)
minecraft:block.amethyst_block.chime       (mágico)
minecraft:entity.firework_rocket.large_blast (explosão)
liberthia:any.custom.sound                 (custom — adicione em sounds.json)
```

---

## 💉 Status Effects

Cada item em `effects: []` aplica um efeito no alvo no impacto.

```json
{ "type": "IGNITE", "duration": 120, "amplifier": 0, "magnitude": 1.0 }
```

| Campo | Default | Uso |
|---|---|---|
| `type` | obrigatório | nome do effect (ver tabela abaixo) |
| `duration` | `60` | ticks (20 = 1s) |
| `amplifier` | `0` | level (0 = level I) |
| `magnitude` | `1.0` | pra HEAL/LIFESTEAL/KNOCKBACK = força/% |

### Tipos disponíveis (21)

| Tipo | O que faz |
|---|---|
| `IGNITE` | Pega fogo (duration / 20 segs) |
| `FREEZE` | Frozen ticks |
| `KNOCKBACK` | Empurra (magnitude = força) |
| `HEAL` | Cura (magnitude = HP) |
| `LIFESTEAL` | Caster cura % do dano (magnitude = %) |
| `LEVITATE` | Sobe |
| `POISON` | Veneno |
| `WITHER` | Wither (dano contínuo + sem HP regen) |
| `SLOWNESS` | Lento |
| `WEAKNESS` | Ataque fraco |
| `BLINDNESS` | Cego |
| `NAUSEA` | Confusão |
| `REGENERATION` | Cura contínua |
| `ABSORPTION` | HP extra dourado |
| `RESISTANCE` | Reduz dano |
| `STRENGTH` | Aumenta dano |
| `SPEED` | Velocidade |
| `GLOWING` | Brilho vísivel pelas paredes |
| `LUCK` | Sorte (loot) |
| `FIRE_RESISTANCE` | Imune fogo |
| `NIGHT_VISION` | Vê no escuro |

---

## 📦 Exemplo MÍNIMO

Quase tudo opcional:
```json
{
  "id": "my_spell",
  "name": "Minha Spell",
  "school": "FIRE"
}
```
Cria fireball básico (PROJECTILE, COMMON, 4 dmg, 10 mana, etc — tudo default).

---

## 🎯 Exemplo COMPLETO

Veja `data/liberthia/spells/factory_KITCHEN_SINK_example.json` — todos os ~40 campos preenchidos com valores de exemplo.

---

## ⚡ Workflow

1. Cria JSON em `data/liberthia/spells/meu_id.json` (do mod ou de um datapack)
2. `/reload` no jogo
3. `/liberthia spell info meu_id` — vê detalhes
4. `/liberthia spell give meu_id` — recebe scroll
5. Right-click hold → cast carregando → solta → cast acontece com tudo configurado
