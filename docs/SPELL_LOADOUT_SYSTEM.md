# Sistema Unificado de Magia — Loadout + Modificadores

## Status atual (r154)

### ✅ Já implementado

1. **Spell Hotbar (Z/B/N)** — r138
   - 3 slots fixos (Z, B, N keybinds)
   - Cast direto sem precisar segurar item na mão
   - Shift+key = bind scroll da main hand
   - Ctrl+key = clear slot
   - Sincronização server-client via `SpellHotbarData` capability

2. **Cooldown Reduction Glyph** — r154 ⭐ NOVO
   - Item `liberthia:glyph_cooldown_reduction`
   - **Cada glyph no inventário** aplica multiplicativo:
     - CD: ×0.90 (10% menos)
     - Mana: ×1.15 (15% mais)
   - Stackable mas com diminishing returns:
     | Glyphs | CD reduce | Mana increase |
     |---|---|---|
     | 1 | -10% | +15% |
     | 2 | -19% | +30% |
     | 3 | -27% | +45% |
     | 5 | -41% | +75% |
     | 10 | -65% | +150% |
   - Hook em **TODAS** as spell items (Universal + Factory)
   - Tooltip mostra status atual baseado no inventário

3. **Spell Levels** — r112
   - XP por kill com spell → levels (1-10)
   - Cada level: -5% mana, -10% CD, +10% dmg/range
   - Persistente no NBT do scroll

4. **Magic Level (player XP)** — r71
   - XP global de magia
   - Aumenta max Source/mana

## 📋 Spell Loadout Slot System (Próxima iteração — r155)

**Conceito:** Player tem 8 slots permanentes (não apenas 3 Z/B/N). UI radial circular ao apertar `R`.

### Design proposto

```
       slot 0
   slot 7  slot 1
slot 6  ●cursor   slot 2
   slot 5  slot 3
       slot 4
```

**Mecânicas:**
- Player abre Grimoire of Observation → menu de 8 slots
- Drag-drop de scrolls Universal/Factory pra qualquer slot
- Apertar `R` (hold) → wheel aparece sobre o crosshair
- Mover mouse pra direção → seleciona slot
- Soltar `R` → cast
- Custom CD shown on each slot (-15% por slot ocupado, max 60%)

### Arquivos a criar (para implementar):
- `SpellLoadoutData.java` — capability com 8 ItemStacks
- `SpellLoadoutScreen.java` — GUI de drag-drop
- `SpellWheelOverlay.java` — radial menu in-game
- `LoadoutCastC2SPacket.java` — packet
- `SpellWheelKeyHandler.java` — hold R detection

## 🔮 Unified Spell Crafting Block (r156+)

**Conceito:** Bloco `Arcane Workbench` com slots pra:

```
[base scroll] [school] [element]
[modifier 1]  [output]  [orb 1]
[modifier 2]  [glyph]   [focus]
[modifier 3]            [tablet]
```

**Tudo combina:**
- Base spell scroll → spell base
- School slot → muda school (override)
- Element slot → adiciona element (dual element se 2 slots)
- 3 modifier glyphs → adicionam modifiers
- Orb → buff a um stat específico
- Focus → adiciona efeito secundário
- Tablet → adiciona um trigger (passive cast on hit, etc)

**Output:** spell scroll customizado com TUDO baked em NBT, mostrando todos os modifiers no tooltip.

### Arquivos a criar:
- `ArcaneWorkbenchBlock` + `BlockEntity`
- `ArcaneWorkbenchMenu` + `Screen`
- `SpellMutationSystem.java` — lógica de combinação
- Recipe JSONs para definir combinações válidas

## 🎨 Auto-Asset System

### JSON spell texture/sound (já suportado em r151)

```json
{
  "id": "minha_super",
  "vfx": {
    "sound_cast_start": "minecraft:entity.blaze.shoot",
    "sound_cast_finish": "minecraft:entity.lightning_bolt.thunder",
    "sound_impact": "minecraft:entity.generic.explode",
    "sprite": "orb"
  }
}
```

### Para texture per-spell (precisaria de Item Model overrides — r156+)

Plano:
1. Adicionar campo `texture` no JSON → `"texture": "liberthia:item/factory_fireball"`
2. Item Model JSON do `factory_spell_scroll` teria 42 overrides baseados em `custom_property:school_hash`
3. `ItemPropertyFunction` registrado retorna o hash → MC seleciona o override correto
4. Cada texture seria gerada procedural via script

**Atualmente:** apenas tint por school (ItemColor) — todas as 42 spells usam o mesmo PNG base com cor diferente.

## 🎯 Plano de execução

| r | Feature | Tempo estimado |
|---|---|---|
| r154 ✅ | CDR Glyph + cost penalty | feito |
| r155 | Spell Loadout (8 slots + wheel) | grande |
| r156 | Arcane Workbench (unified crafting) | enorme |
| r157 | Per-spell custom textures (Item Model overrides) | médio |
| r158 | Spell auto-resource resolver (assets/) | médio |

## Como usar AGORA (r154)

```
/give @s liberthia:glyph_cooldown_reduction 5
```

Coloca os 5 glyphs no inventário (qualquer slot). Verifica:
- Cast qualquer spell → veja no log:
  ```
  [DynamicSpell] factory_fireball by Steve — mana 38 (×1.75), cd 36t (×0.59)
  ```
  (com 5 glyphs: mana 25→44, cd 60t→35t)
- Tooltip do glyph mostra status ativo

Se quiser que eu implemente o **Loadout System (8 slots + wheel R)** ou o **Arcane Workbench**, me fala qual prioriza. Cada um é trabalho de 2-3 horas.
