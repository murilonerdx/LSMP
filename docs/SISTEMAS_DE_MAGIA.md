# Sistemas de Magia do Liberthia — Visão Geral Completa

> Última atualização: **r144** — Tab `Liberthia: Abbadon` criada.

Liberthia tem **5 sistemas de magia distintos** rodando em paralelo. Eles não competem — cada um atende a um estilo diferente de jogador (caster customizável, mago "off-the-shelf" estilo Iron's Spells, cultista de sangue, ocultista de rituais, e Abbadon/destruidor). Este documento mapeia **quais são** e **quais são baseados em Ars Nouveau (AN)**.

---

## 📖 Resumo Rápido

| Sistema | Tab Criativa | Base AN? | Estilo | Mana |
|---|---|---|---|---|
| **1. Observation Casting** | Liberthia: Magia | ✅ **PRIMÁRIO** AN | Glyphs combináveis (Method+Effect+Augment) | Source |
| **2. Universal Spells (Iron's)** | Liberthia: Magia | ❌ Iron's Spells port | 50 spells prontos (1 item = 1 spell) | Source |
| **3. Custom Spell Crafting** | Liberthia: Magia | ❌ Independente | Spell Crafting Table + Spell Wheel | Source |
| **4. Occult Ritual System** | Liberthia: Horror | ❌ Independente | Brazier + sigils + chalks + velas | Sacrifício / itens |
| **5. Blood Magic (Abbadon)** | Liberthia: Abbadon | ❌ Inspirado EvilCraft/BM2 | Sangue/Carne/Sacrifício | HP do caster |

---

## 1️⃣ Observation Casting — **O sistema PRINCIPAL, baseado em Ars Nouveau**

**Status:** 🌟 Sistema MAIN. Foi desenvolvido nas releases **r60 → r74** com referência direta ao código real do Ars Nouveau pra Forge 1.20.1.

### O que é baseado no AN

Estes elementos são port/adaptação direta do Ars Nouveau:

| AN Original | Liberthia equivalente | Implementação |
|---|---|---|
| `Spell` (Method + Effects + Augments) | `SpellComposition` | `magic/spell/SpellComposition.java` |
| `AbstractCastMethod` | `CastMethod` enum (Projectile/Beam/Touch/Self/Burst/Orbit/Chain/Laser/Wall) | `magic/cast/CastMethod.java` |
| `AbstractEffect` | `SpellEffect` (Damage/Ignite/Heal/Freeze/Knockback/Launch/Levitate/Lightning/Blind/Gravity/Slowfall/Fangs/Explosion/Harm) | `magic/effect/SpellEffect.java` |
| `AbstractAugment` | `Augment` (Amplify/AOE/Pierce/Multishot/Chain/Range/Sustain/Penetrate/Apolão) | `magic/augment/Augment.java` |
| `EntitySpellResolver` + `EntityProjectileSpell` | `SpellProjectileEntity` | `magic/entity/SpellProjectileEntity.java` |
| `Source` capability (mana) | `SourceProvider` capability | `magic/source/SourceProvider.java` |
| `SourceJar` (block de mana) | `SourceJarBlock` + tile entity | `magic/block/SourceJarBlock.java` |
| `Sourcelink` (geradores Source) | `VolcanicSourcelink` / `MycelialSourcelink` / `VitalicSourcelink` / `AlchemicalSourcelink` | r84 — `magic/block/sourcelink/*` |
| `Source Relay` (rede de transferência) | `SourceRelayBlock` | r74 — `magic/block/SourceRelayBlock.java` |
| `ScribesTable` GUI | `ScribesTableBlock` + `ScribesTableScreen` | r69 — composição de spells via drag-drop |
| `Imbuement Chamber` | `ImbuementTableBlock` + `ImbuementTableMenu` | r71 — converte items em reagentes mágicos |
| `Spell Parchment` | `SpellParchmentItem` (transfere recipe entre tomes) | r68 |
| `Spellbook` (book de feitiços salvos) | `ObservationTome` + `GrimoireOfObservation` | r62 / r140 |
| `Spell Binding Pedestal` | `SpellBindingPedestalBlock` (combina Parchment + Tome) | r77 |
| `Spellsword` (espada que custa Source) | `SpellsverdItem` (a.k.a. `SPELLSWORD`) | r61 |
| `Ritual Brazier` (sigil-driven rituals) | `RitualBrazierBlock` + 8 ritual tablets | r86 |
| `Rune Block` (chalk-based step trigger) | `RuneBlock` + `ChalkItem` | r72 |
| `Scrying` (oculus + lens) | `ScryingLens` + `ScryerOculus` | r85 |
| `Familiar mobs` (Wisps, Sprites, etc) | 6 familiars: Wisp/Sprite/Reaper/Whelp/Carbuncle/AmethystGolem | r95 + r109 |
| `Bookwyrm` (catalogador automático) | `BookwyrmEntity` | r74 |
| `Wilden Boss` (boss faseado) | `AbyssalLichEntity` + 2 minions | r98 |
| `Dominion Wand` (link blocks) | `DominionWandItem` | r88 |
| `Spell Prism` (redirect spell) | `SpellPrismBlock` + entity | r88 |
| `Spell Turret` (turret automática) | `SpellTurretBlock` | r88 |
| `Glyph items` (pickup do mundo) | 70+ glyph items via Spirit World rituals | r68/r74/r139 |
| `Magic Level` (XP de magia) | `MagicLevelProvider` capability | r71 |
| `Perk system` (3 slots em wizard armor) | `PerkThreadItem` × 15 + slot UI | r90 |
| `Lay Line` (refill grátis em raio) | `LayLineBlock` | r138 |
| `Wizard Tower` (estrutura procedural) | `WizardTowerFeature` worldgen | r138 |

### Como funciona (ciclo do jogador)

```
1. Achar Source Crystal Ore (Spirit World) → minera, tem Source Catalyst
2. Craft Grimoire of Observation (book + Source Catalyst + Spirit Ink)
3. Craft Scribes Table → drag-drop glyphs pra compor spell
4. Spell = Method (Projectile/Beam/Touch/...) + Effect[] (Damage/Heal/Ignite/...) + Augment[] (AOE/Amplify/Pierce/...)
5. Salva spell em Spell Parchment ou Observation Tome
6. Equipa Spellsword ou Caster Wand → right-click pra castar (custa Source)
7. Source regenera via Source Berry, Mana Berry, Lay Line, ou Sourcelink em rede
8. Sobe Magic Level matando mobs com magia → desbloqueia max Source maior
```

### Arquivos-chave AN-based

- `magic/spell/SpellComposition.java` — DTO da spell (idêntico ao `Spell` do AN)
- `magic/cast/CastResolver.java` — executa Method + Effects (equivalente ao `EntitySpellResolver`)
- `magic/source/SourceProvider.java` — capability do mana
- `magic/source/SourceSyncTicker.java` — sync server→client
- `magic/glyph/GlyphRegistry.java` — registro central de glyphs
- `magic/perk/PerkSlots.java` — sistema de slots em wizard armor
- `magic/level/MagicLevelProvider.java` — XP/level de magia

---

## 2️⃣ Universal Spells (Iron's Spells 'n Spellbooks port) — **NÃO é AN**

**Status:** r111-r112. Port direto do mod **Iron's Spells 'n Spellbooks**.

### Diferença do AN

- **AN:** você COMPÕE a spell juntando glyphs em runtime no Scribes Table.
- **Iron's:** cada spell é um **item pronto, hardcoded**. Você só usa `right-click` no item de spell pra castar.

### 50 spells implementados

Divididos em 8 schools:

| School | Spells |
|---|---|
| Fire (8) | Fireball, Burning Dash, Inferno, Magma Bomb, Sun Beam, Phoenix Reborn, Cauterize, Heat Wave |
| Ice (8) | Frostbolt, Ice Spike, Frost Nova, Glacial Storm, Frost Step, Frozen Ground, Ray of Frost, Ice Lance |
| Lightning (8) | Lightning Bolt, Spark Burst, Chain Lightning, Shock, Thunder Step, Storm Cloud, Static Field, Lightning Lance |
| Blood (8) → **agora em Abbadon tab** | Blood Step, Lifedrain, Heartstop, Blood Spear, Sanguine Bind, Crimson Mist, Vampiric Touch, Blood Pact |
| Eldritch (6) | Void Tentacle, Mind Spike, Eldritch Blast, Soul Tear, Madness Wave, Cosmic Void |
| Holy (8) | Greater Heal, Smite, Divine Light, Sun Strike, Holy Lance, Healing Aura, Sacred Ground, Judgment |
| Nature (6) | Vine Tangle, Earth Wall, Stone Shard, Wisps Heal, Roots, Bramble Storm |
| Evocation (4) | Magic Missile, Bone Spear, Magic Shield, Summon Vex |

### Compartilha mana com o AN

Usa o **mesmo Source mana** do Observation Casting (capability `SourceProvider`). Então um caster de Observation pode usar spells Iron's-style também.

### Arquivos

- `magic/universal/UniversalSpellRegistry.java`
- `magic/universal/SpellItem.java`
- `magic/universal/impl/*.java` (50 implementações)

---

## 3️⃣ Custom Spell Crafting — Independente, **não é AN**

**Status:** r42, depois r119. Sistema próprio nosso pra dar "spells nomeáveis" sem precisar fazer glyph-combination.

### O que é

- Bloco **Spell Crafting Table** — abre GUI onde você compõe spell com:
  - Tipo (projétil/beam/AOE/self)
  - Damage source (fogo/gelo/cosmic/...)
  - Modifiers (lifesteal, knockback, etc)
  - Nome customizado
- Resultado: **Spell Wheel item** que vai pro inventário e tem o nome que você deu.
- Right-click castar; consome Source.

### Diferença do AN

No AN você combina glyphs em ordem específica (Method → Effects → Augments) com regras estritas. Aqui é mais "fluido" — você seta variáveis numa GUI.

### Arquivos

- `block/SpellCraftingTableBlock.java`
- `menu/SpellCraftingMenu.java`
- `client/screen/SpellCraftingScreen.java`
- `item/SpellWheelItem.java`

---

## 4️⃣ Occult Ritual System — Independente, **não é AN**

**Status:** r121 + r139. Sistema de **rituais de invocação** com tema demoníaco/angelical (Bartimaeus/Solomon's Keys).

### Como funciona

1. Player desenha **chalk circle** (Chalk Black/Red/White/Purple/Gold) no chão
2. Coloca **velas** (Candle Black Occult/Red/etc) nos pontos cardeais
3. Coloca **sigils** (10 sigils: Afrit, Bael, Banishing, Dimensional, Djinni, Foliot, Lucifer, Metatron, Necro, Sandalphon) no centro
4. Right-click com **Ritual Dagger** ou **Bound Crystal**
5. Spawn da entidade invocada ou efeito custom

### Diferença do AN

AN tem Ritual Brazier também — mas é tablet-driven (Sunrise, Moonfall, Healing, etc, 8 tablets fixos). O Occult system é **mais customizável** com sigils diferentes e tem **vela colocada espacialmente** importando pro ritual.

### Itens

- 5 chalks (5 cores), 5 candle types (Black/Golden/Purple/Red/White Occult), 10 sigils, 3 bound crystals (Afrit/Djinni/Foliot), 1 ritual dagger, 1 ritual chalice, 1 ritual circle block.

### Arquivos

- `item/ritual/SigilItem.java`
- `block/RitualCircleBlock.java`
- `block/CandleOccultBlock.java`
- `item/ritual/BoundCrystalItem.java`

---

## 5️⃣ Blood Magic — **Tab Abbadon**, inspirado em EvilCraft/Blood Magic 2

**Status:** Fases 1-5, criado bem antes da magic update. **NÃO é AN.**

### O que é

Tema gore/sacrifício. O caster **paga com HP** (não com Source). Tem:

- **Cult of the Mother** — culto de sangue, NPCs Cultist/Priest/Mage/Hound, Tome of the Mother
- **Blood Altar / Blood Cauldron** — máquinas pra processar sangue
- **Sanguine Ward armor** — anti-infection, salva o player
- **Blood Trees / Sanguine Trees** — bioma de árvores de sangue (worldgen)
- **Flesh Mother (boss)** — boss fight com HP, blood pulse, sonic boom, ground pound
- **Order vs Blood** — facção oposta (Order Paladin egg, Desecrated Holy Relic)
- **Hemomancer Staff / Blood Bow / Blood Ritual Dagger / Blood Pact Amulet** — armas que custam HP
- **Blood Syringe** — drena HP de mobs/players pra encher Blood Vials

### Por que tab própria (Abbadon)?

Pediu o usuário em r144 — agrupar TODOS os items de sangue/carne/cult/Order×Blood pra ficar fácil de achar. Total **~110 itens** migrados.

### Compartilhamento com Magic Arsenal

Apesar de NÃO usar Source mana, **os spells `SPELL_BLOOD_STEP`, `SPELL_LIFEDRAIN`, etc.** são Iron's-style — então tecnicamente USAM Source (pq são part do Universal Spell system). Foram migrados pra Abbadon só pra organização visual.

### Arquivos

- `item/BloodArmorItem.java`, `item/SanguineWardArmorItem.java`
- `item/HemomancerStaffItem.java`, `BloodBowItem.java`, `BloodRitualDaggerItem.java`
- `block/BloodAltarBlock.java`, `BloodCauldronBlock.java`
- `entity/FleshMotherBossEntity.java`, `BloodCultistEntity.java`
- `effect/BloodInfectionEffect.java`, `MatterArmorDeathHandler.java`

---

## Resumo: Quais SÃO baseados em Ars Nouveau?

✅ **APENAS o Observation Casting (sistema #1)** é diretamente baseado e portado do Ars Nouveau.

❌ **Os outros 4 (Universal Spells, Custom Crafting, Occult Rituals, Blood Magic)** são sistemas independentes inspirados por outros mods:
- Universal Spells → Iron's Spells 'n Spellbooks
- Custom Crafting → independente (nosso design)
- Occult Rituals → Bartimaeus / Solomon's Keys / Occultism
- Blood Magic → Blood Magic 2 / EvilCraft / Witchery

Todos os 5 compartilham um ponto em comum: **rodam em paralelo, sem conflito**, e o player pode usar combos entre eles (ex: escudo Holy Iron's-style + projétil Observation custom + armor de sangue).

---

## Tabs Criativas Atuais (r144)

```
1. Liberthia              (icon: Dark Matter Bucket)     — main mod + matter + horror leftovers
2. Liberthia: Magia       (icon: Grimoire of Observation) — TODA a magia (5 sistemas)
3. Liberthia: Horror      (icon: Fractured Scripture)    — cosmic horror items + dimensões liminais
4. Liberthia: Abbadon     (icon: Heart of the Mother)    — sangue + carne + culto [r144 NOVO]
```

---

## Diagrama de Dependências

```
                    ┌──────────────────────────────────────┐
                    │   Source Mana (capability único)     │
                    │   Servido por: SourceJar, Sourcelink,│
                    │   Lay Line, Mana/Source Berries      │
                    └──────────────────┬───────────────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        ▼                              ▼                              ▼
┌───────────────┐         ┌────────────────────┐         ┌─────────────────┐
│ 1. Observation │         │ 2. Universal Spells│         │ 3. Custom Spells│
│    (AN port)   │         │   (Iron's port)    │         │   (próprio)     │
│ Glyphs combo   │         │ 50 spells prontos  │         │ Spell Wheel     │
└───────────────┘         └────────────────────┘         └─────────────────┘

         ┌──────────────────────┐         ┌─────────────────────┐
         │ 4. Occult Rituals    │         │ 5. Blood Magic       │
         │  (chalks+sigils+vel) │         │  (Abbadon — HP cost) │
         │  Tema Bartimaeus     │         │  Tema EvilCraft/BM2  │
         └──────────────────────┘         └─────────────────────┘
                  │                                 │
                  └──────  Não usam Source ─────────┘
```

---

## Para o usuário: qual sistema usar?

| Quer... | Use |
|---|---|
| Customizar tudo, criar spell própria do zero | **Observation (AN)** — Scribes Table + glyphs |
| Só queimar coisa rápido | **Universal Spells (Iron's)** — pegou spell, right-click |
| Spell com nome custom estilo MMO | **Custom Spell Crafting** — Spell Wheel |
| Invocar djinn/demônio com chalk circle | **Occult Rituals** |
| Vampirismo, sacrifício, lifesteal, build de necromancer | **Blood Magic (Abbadon tab)** |

Todos rodam ao mesmo tempo no mesmo save. ✅
