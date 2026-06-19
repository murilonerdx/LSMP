# Spell Factory — Liberthia r148

Sistema declarativo de feitiços. Crie spells novos via **JSON** (zero Java) ou via **Builder API** (em código).

## 🎯 Por que existe?

Antes (r112+): pra adicionar uma nova spell, era preciso:
1. Criar `RegistryObject<Item>` em `ModItems`
2. Criar entry em `SpellLibrary`
3. Escrever lambda de cast manualmente
4. Lidar com VFX, particles, sons, screen shake, ground decal manualmente

Agora (r148): **um JSON** define tudo. A factory cuida do resto.

## 📦 Componentes

| Classe | Função |
|---|---|
| `SpellType` (enum, 13 valores) | PROJECTILE, HOMING, MULTI_SHOT, ARC, BEAM, EXPLOSION, NOVA, CONE, RAIN, AURA, TOUCH, SELF, DASH |
| `SpellRecipe` | DTO imutável da spell (lido de JSON) |
| `SpellBehavior` | Params de comportamento (speed, radius, etc) |
| `SpellVfxProfile` | Cores, trail density, screen shake, sons |
| `SpellEffectSpec` | Lista de status effects aplicados (poison, ignite, lifesteal, knockback, etc) |
| `SpellFactory` | Converte Recipe → SpellCast lambda (HOW) |
| `SpellRecipeRegistry` | Registry central |
| `SpellRecipeLoader` | JSON loader (auto-reload via `/reload`) |
| `SpellRecipeBuilder` | Fluent API pra criação em código |
| `DynamicSpellItem` | Item único que aceita NBT de qualquer spell |
| `SpellFactoryCommand` | `/liberthia spell {give,list,info,reload}` |

## 🧪 JSON Format completo

```json
{
  "id": "minha_super_spell",
  "name": "Minha Super Spell",
  "school": "FIRE",              // FIRE, ICE, LIGHTNING, BLOOD, ELDRITCH, HOLY, NATURE
  "rarity": "RARE",              // COMMON, UNCOMMON, RARE, EPIC
  "mana": 30,
  "cooldown": 60,                // ticks
  "damage": 12.0,
  "range": 24.0,                 // blocos
  "type": "PROJECTILE",          // ver SpellType enum
  "lore": "Descrição mística pro tooltip",

  "behavior": {
    "speed": 1.6,                // PROJECTILE: blocks/tick
    "lifetime_ticks": 80,
    "pierce": false,
    "aoe_radius": 2.5,           // splash damage no impacto
    "ignite_blocks": true,
    "projectile_count": 1,       // MULTI_SHOT
    "spread_degrees": 0.0,
    "homing": false,             // HOMING
    "radius": 4.0,               // EXPLOSION/NOVA/CONE/RAIN: raio
    "cone_angle": 60.0,          // CONE: ângulo total em graus
    "rain_strikes": 5,           // RAIN: número de strikes
    "rain_duration_ticks": 60,
    "aura_duration_ticks": 160,  // AURA: tempo total
    "aura_tick_interval": 20,    // AURA: a cada X ticks
    "dash_distance": 12.0,       // DASH
    "touch_range": 4.0,          // TOUCH
    "beam_max_ticks": 40,        // BEAM
    "beam_range": 16.0
  },

  "vfx": {
    "color_primary": "#ff5500",  // -1 ou ausente = usa school
    "color_secondary": "#ffaa00",
    "sprite": "auto",            // "auto" | "orb" | "star" | "hex" | "arc" | "shard"
    "trail_density": 8,          // partículas/tick (1-30)
    "trail_size": 1.3,           // escala (0.3-3.0)
    "trail_lifetime": 18,        // ticks
    "charge_intensity": 0.7,     // densidade aura mão (0-1)
    "impact_scale": 1.3,         // tamanho do burst
    "impact_particles": 100,     // count total
    "screen_shake": 0.7,         // 0-2.5
    "screen_shake_ticks": 8,
    "impact_light": 14,          // 0-15
    "sound_cast_start": "auto",
    "sound_cast_finish": "auto",
    "sound_impact": "auto"
  },

  "effects": [
    { "type": "IGNITE", "duration": 100, "magnitude": 1.0 },
    { "type": "KNOCKBACK", "magnitude": 0.6 },
    { "type": "LIFESTEAL", "magnitude": 0.5 },
    { "type": "POISON", "duration": 80, "amplifier": 1 }
  ]
}
```

### Effect types disponíveis

`IGNITE`, `FREEZE`, `KNOCKBACK`, `HEAL`, `LIFESTEAL`, `LEVITATE`, `POISON`, `WITHER`, `SLOWNESS`, `WEAKNESS`, `BLINDNESS`, `NAUSEA`, `REGENERATION`, `ABSORPTION`, `RESISTANCE`, `STRENGTH`, `SPEED`, `GLOWING`, `LUCK`, `FIRE_RESISTANCE`, `NIGHT_VISION`

## 🎮 Como usar (in-game)

### 1. Comandos

```
/liberthia spell list                      → lista todas as recipes
/liberthia spell give factory_fireball     → dá scroll dessa spell
/liberthia spell info factory_meteor       → mostra detalhes
/liberthia spell reload                    → recarrega JSONs (usa /reload)
```

### 2. Fluxo completo

1. **Cria JSON** em `data/liberthia/spells/<id>.json` (ou em datapack)
2. **`/reload`** → loader detecta e regista
3. **`/liberthia spell give <id>`** → recebe scroll com NBT da spell
4. **Right-click hold** → carrega (channel wind-up)
5. **Solta** → cast acontece com VFX, sons, dano, effects

### 3. 12 spells de exemplo já incluídas

| ID | Tipo | Escola |
|---|---|---|
| `factory_fireball` | PROJECTILE | FIRE |
| `factory_homing_missile` | HOMING | ELDRITCH |
| `factory_triple_shot` | MULTI_SHOT | ICE |
| `factory_death_beam` | BEAM | ELDRITCH |
| `factory_meteor` | EXPLOSION | FIRE |
| `factory_frost_nova` | NOVA | ICE |
| `factory_dragon_breath` | CONE | FIRE |
| `factory_meteor_storm` | RAIN | FIRE |
| `factory_burning_aura` | AURA | FIRE |
| `factory_vampiric_touch` | TOUCH | BLOOD |
| `factory_iron_skin` | SELF | NATURE |
| `factory_phase_dash` | DASH | ELDRITCH |

## 💻 Builder API (em Java)

```java
import br.com.murilo.liberthia.magic.factory.*;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.world.item.Rarity;

SpellRecipe minhaSpell = SpellRecipeBuilder.of("custom_lightning_storm")
    .name("Tempestade Elétrica")
    .school(SpellSchool.LIGHTNING)
    .rarity(Rarity.RARE)
    .mana(50).cooldown(120).damage(15).range(20)
    .type(SpellType.RAIN)
    .lore("Raios caem do céu numa área.")
    .behavior(b -> b
        .radius(8.0F)
        .rainStrikes(8)
        .rainDuration(80))
    .vfx(v -> v
        .colorPrimary(0xFFFF88)
        .impactScale(1.5F)
        .screenShake(0.8F)
        .impactLight(15))
    .effect(SpellEffectSpec.Type.NAUSEA, 60, 0, 0)
    .build();

SpellRecipeRegistry.register(minhaSpell);
```

## 🔧 Arquivos modificados (r148)

- `magic/factory/SpellType.java` — enum 13 tipos
- `magic/factory/SpellRecipe.java` — DTO
- `magic/factory/SpellBehavior.java` — comportamento
- `magic/factory/SpellVfxProfile.java` — VFX config
- `magic/factory/SpellEffectSpec.java` — effects (21 tipos)
- `magic/factory/SpellFactory.java` — dispatch por type
- `magic/factory/SpellRecipeRegistry.java` — registry
- `magic/factory/SpellRecipeLoader.java` — JSON loader (reload listener)
- `magic/factory/SpellRecipeBuilder.java` — fluent API
- `magic/factory/DynamicSpellItem.java` — item NBT-driven
- `magic/spell/vfx/ScheduledVfx.java` — adicionado `spawnAura`, `spawnBeam`, `schedule`
- `magic/spell/SpellLibrary.java` — `registerExternal()` pública
- `command/SpellFactoryCommand.java` — comandos
- `data/liberthia/spells/*.json` — 12 recipes de exemplo

## 🚀 Como adicionar uma spell própria sem mexer em código

1. Crie `data/liberthia/spells/meu_super_spell.json` (no seu mundo: `world/datapacks/seu_pack/data/liberthia/spells/`)
2. Cole o template do JSON acima e ajuste valores
3. `/reload` no jogo
4. `/liberthia spell give meu_super_spell`
5. Right-click hold pra carregar, solta pra castar

Pronto! Spell completa com VFX, sons, particles, screen shake, status effects — **sem uma linha de Java**.
