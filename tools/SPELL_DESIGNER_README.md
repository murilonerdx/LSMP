# Liberthia Spell Designer (r158)

GUI standalone para criação visual de feitiços — **sem código, sem digitação**, só click, arrastar e ajustar sliders.

## Requisitos

- **Python 3.8+** (Windows: instale do [python.org](https://www.python.org/downloads/) marcando "Add to PATH")
- **Pillow**: `pip install pillow`

## Como rodar

### Windows
Duplo-click em `tools/spell_designer.bat`.

### Linux/Mac
```bash
cd liberthia_mod
python3 tools/spell_designer.py
```

## Interface

```
┌─────────────────────────────────────────────────────────────────┐
│            ✦ Liberthia Spell Designer ✦                          │
├──────────────┬────────────────────────┬────────────────────────┤
│ FORM         │   SPRITE COMPOSER      │   SPRITE GALLERY       │
│ ────         │   ──────────────       │   ──────────────       │
│ ● Identity   │                        │   [Schools] [Runes]    │
│   ID         │   ┌────────────────┐   │   [Elements][Threads]  │
│   Name       │   │                │   │   [Focuses]            │
│   Lore       │   │   PREVIEW      │   │                        │
│   Rarity     │   │   16×16 → 256  │   │   ╔═╗ ╔═╗ ╔═╗ ╔═╗      │
│              │   │                │   │   ║◊║ ║△║ ║○║ ║✶║      │
│ ● School     │   └────────────────┘   │   ╚═╝ ╚═╝ ╚═╝ ╚═╝      │
│ [F][I][L][B] │                        │   fire ice  bolt drop  │
│ [E][H][N]    │   ● Layers (▲▼✖▶)      │                        │
│              │   ┌────────────────┐   │   (galeria scrollable, │
│ ● Type       │   │ schools/fire   │   │    duplo-click adiciona│
│ ● Category   │   │ runes/pentagram│   │     ao composer)       │
│              │   │ ...            │   │                        │
│ ● Stats      │   └────────────────┘   │                        │
│ Mana   ▓▓▓░░ │                        │                        │
│ CD     ▓▓░░░ │                        │                        │
│ Damage ▓▓▓▓░ │                        │                        │
│ Range  ▓▓░░░ │                        │                        │
│              │                        │                        │
│ ● VFX        │                        │                        │
│ Primary █████│                        │                        │
│ Secondary    │                        │                        │
│ Particles ▼  │                        │                        │
│              │                        │                        │
│ ● Effects    │                        │                        │
│ [+ Add Effect]                         │                        │
│  • IGNITE    │                        │                        │
│  • DAMAGE    │                        │                        │
├──────────────┴────────────────────────┴────────────────────────┤
│ Status: Pronto                  [📂 Load] [🔄 Reset] [💾 SAVE]  │
└─────────────────────────────────────────────────────────────────┘
```

## Workflow

1. **Identity** — Digite **Nome** (ID é auto-derivado); ajuste **Lore** e **Rarity**.
2. **School** — Click num dos 7 botões coloridos. Cor primária do VFX é auto-setada.
3. **Type & Category** — Escolha o comportamento (PROJECTILE, BEAM, AOE, DASH, ...) e categoria temática.
4. **Stats** — Mexa os sliders de Mana, Cooldown, Damage, Range.
5. **VFX** — Pick cores via color picker; selecione preset de partículas (minimal/standard/intense/devastation/subtle_aura).
6. **Effects** — Click "+ Add Effect" para adicionar IGNITE, KNOCKBACK, CUSTOM_EFFECT (mod-native), DAMAGE, HEAL, etc.
7. **Sprite Composer** — Da galeria à direita, duplo-click numa sprite para adicionar ao composer. Use ▲▼ para reordenar (top = renderizado primeiro). ▶ toca a sequência. ✖ remove.
8. **Save Spell** — Click no botão dourado. O tool **automaticamente**:
   - Cria `data/liberthia/spells/factory_<id>.json`
   - Compõe textura 16×16 com layers + cor primária + parchment → `textures/item/factory_spell_scroll_<id>.png`
   - Cria model JSON `models/item/factory_spell_scroll_<id>.json`
   - Atualiza `FactorySpellIndex.java` (Java map) ordenado alfabeticamente
   - Atualiza overrides em `factory_spell_scroll.json` master
   - Adiciona lang entry em `en_us.json` + `pt_br.json`
9. **Rebuild do mod** — `./gradlew build` para ver in-game.

## Galerias disponíveis

### Schools (`data/liberthia/schools/*.png`)
- fire, ice, lightning, blood, eldritch, holy, nature
- Glifos 32×32 transparent com símbolo único por escola.

### Runes (`data/liberthia/runes/*.png`)
- pentagram, hexagram, circle, triangle, square, cross, spiral, infinity, arrow, wave
- Símbolos geométricos genéricos para layering.

### Elements (`data/liberthia/elements/*.png`)
- earth, water, wind, void, steam, mud, lava, frost, spark, thorn
- Hexágonos com pattern de cor por elemento.

### Threads (`data/liberthia/threads/*.png`)
- silver, golden, crimson, azure, shadow, ethereal
- Padrões de tecelagem trançada.

### Focuses (`data/liberthia/focuses/*.png`)
- concentration, destruction, protection, manipulation, transmutation
- Lentes concêntricas com cristal central.

## Atalhos

| Ação | Atalho |
|---|---|
| Adicionar sprite ao composer | Duplo-click na galeria |
| Remover effect | Duplo-click no list de effects |
| Reset form | Botão 🔄 |
| Carregar feitiço existente | Botão 📂 → escolhe `factory_*.json` |

## Estrutura de saída

Quando você salva um feitiço chamado **`vortex_blade`** (auto-prefixed → `factory_vortex_blade`):

```
src/main/resources/
├── data/liberthia/spells/factory_vortex_blade.json
└── assets/liberthia/
    ├── models/item/factory_spell_scroll_vortex_blade.json
    ├── textures/item/factory_spell_scroll_vortex_blade.png  (16×16)
    └── lang/{en_us.json,pt_br.json}  (+1 entry)

src/main/java/.../magic/factory/FactorySpellIndex.java  (+1 entry, ordenado)
```

## Adicionar suas próprias sprites

Coloque PNGs 32×32 transparent em qualquer pasta default. O tool re-escaneia ao abrir, então:
1. Salve seu PNG em `data/liberthia/schools/minha_escola.png`
2. Re-abra o designer
3. Aparece na aba "Schools" pronta pra usar

## Dúvidas / bugs

- Sprite não aparece na galeria → confira se é PNG 32×32 RGBA na pasta certa
- Texture não atualiza in-game → rebuild com `./gradlew build` + reinicie cliente
- "ModuleNotFoundError: pillow" → `pip install pillow`
