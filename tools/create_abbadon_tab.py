"""r144: Cria nova tab ABBADON (Liberthia: Abbadon) e move TODOS items
de sangue/cult/flesh/order-vs-blood pra ela.

Estratégia:
1. Lê ModCreativeTabs.java
2. Define lista de blood items que devem migrar
3. Remove cada linha output.accept(...) / out.accept(...) que referencia esses items
4. Adiciona um novo bloco ABBADON antes do `private ModCreativeTabs()`
"""

from pathlib import Path
import re

FILE = Path("src/main/java/br/com/murilo/liberthia/registry/ModCreativeTabs.java")

# Items "de sangue" (Abbadon = lugar de destruição/aniquilação, perfeito pro tema)
ABBADON_ITEMS = [
    # --- Blood Fountain / Cure / Altar / Cauldron ---
    "BLOOD_FOUNTAIN_ITEM",
    "BLOOD_CURE_PILL",
    "BLOOD_ALTAR_ITEM",
    "BLOOD_CAULDRON_ITEM",
    # --- Flesh / Mother ---
    "LIVING_FLESH_ITEM",
    "FLESH_MOTHER_ITEM",
    "ATTACKING_FLESH_ITEM",
    "HEART_OF_FLESH_BLOCK_ITEM",
    "HEART_OF_FLESH_ITEM",
    "HEART_OF_THE_MOTHER",
    "FLESH_MOTHER_BOSS_EGG",
    "FLESH_THREAD",
    "FLESH_CRAWLER_EGG",
    # --- Blood infection / volcano / spike ---
    "BLOOD_INFECTION_BLOCK_ITEM",
    "BLOOD_INFESTATION_BLOCK_ITEM",
    "BLOOD_VOLCANO_ITEM",
    "BLOOD_SPIKE_ITEM",
    "BLOOD_BUCKET",
    # --- Blood terrain ---
    "BLOOD_DIRT_ITEM",
    "BLOOD_SAND_ITEM",
    "BLOOD_STONE_ITEM",
    "BLOOD_COAL_ORE_ITEM",
    "BLOOD_IRON_ORE_ITEM",
    "BLOOD_GOLD_ORE_ITEM",
    "BLOOD_DIAMOND_ORE_ITEM",
    "BLOOD_REDSTONE_ORE_ITEM",
    "BLOOD_LAPIS_ORE_ITEM",
    "BLOOD_EMERALD_ORE_ITEM",
    # --- Blood Armor ---
    "BLOOD_HELMET",
    "BLOOD_CHESTPLATE",
    "BLOOD_LEGGINGS",
    "BLOOD_BOOTS",
    # --- Blood Scythe + Sanguine ---
    "BLOOD_SCYTHE",
    "SANGUINE_CORE",
    "SANGUINE_ESSENCE",
    # --- Order x Sangue (Fase 5) ---
    "ORDER_SHRINE_ITEM",
    "DESECRATED_HOLY_RELIC",
    "ORDER_PALADIN_EGG",
    # --- Weapons / Magic (Fase 4) ---
    "HEMOMANCER_STAFF",
    "BLOOD_BOW",
    "BLOOD_RITUAL_DAGGER",
    "BLOOD_PACT_AMULET",
    # --- Alquimia (Fase 3) ---
    "BLOOD_VIAL",
    "BLOOD_VIAL_FILLED",
    "CONGEALED_BLOOD",
    # --- Culto do Sangue (Fase 1) ---
    "BLOODY_RAG",
    "RUSTED_DAGGER",
    "PRIEST_SIGIL",
    "TOME_OF_THE_MOTHER",
    "TOME_OF_THE_PILGRIM",
    # --- Spawn Eggs ---
    "GORE_WORM_EGG",
    "BLOOD_CULTIST_EGG",
    "BLOOD_PRIEST_EGG",
    "WOUNDED_PILGRIM_EGG",
    "BLOOD_MAGE_EGG",
    "BLOOD_HOUND_EGG",
    "BLOOD_WARDEN_EGG",
    # --- Seringa (T5b) ---
    "BLOOD_SYRINGE",
    # --- T6: EvilCraft ports ---
    "BLOOD_TELEPORT_PEARL",
    "TAINTED_APPLE",
    # --- Sanguine Ward ---
    "SANGUINE_WARD_HELMET",
    "SANGUINE_WARD_CHESTPLATE",
    "SANGUINE_WARD_LEGGINGS",
    "SANGUINE_WARD_BOOTS",
    "SANGUINE_WARD_SWORD",
    "SANGUINE_WARD_PICKAXE",
    "BLOOD_WARD_CHARM",
    "TAINTED_ESSENCE",
    "CLEANSING_SALT",
    "PURIFYING_FLASK",
    # --- Ritual blocks ---
    "BLOOD_SACRIFICIAL_BOWL_ITEM",
    "GOLDEN_BLOOD_BOWL_ITEM",
    "CRYSTALLIZED_BLOOD_SOUL",
    # --- Blood attacking blocks ---
    "HEMORRHAGE_SPIKE_ITEM",
    "SANGUINE_SNARE_ITEM",
    # --- Occultism blood ports ---
    "BLOOD_CHALK",
    "BLOOD_TORCH_ITEM",
    # --- Sanguine Wood set ---
    "SANGUINE_LOG_ITEM",
    "SANGUINE_WOOD_ITEM",
    "STRIPPED_SANGUINE_LOG_ITEM",
    "STRIPPED_SANGUINE_WOOD_ITEM",
    "SANGUINE_PLANKS_ITEM",
    "SANGUINE_LEAVES_ITEM",
    "SANGUINE_SAPLING_ITEM",
    "SANGUINE_STAIRS_ITEM",
    "SANGUINE_SLAB_ITEM",
    "SANGUINE_FENCE_ITEM",
    "SANGUINE_FENCE_GATE_ITEM",
    "SANGUINE_BUTTON_ITEM",
    "SANGUINE_PRESSURE_PLATE_ITEM",
    "SANGUINE_DOOR_ITEM",
    "SANGUINE_TRAPDOOR_ITEM",
    # --- Blood Tree set ---
    "BLOOD_LOG_ITEM",
    "STRIPPED_BLOOD_LOG_ITEM",
    "BLOOD_PLANKS_ITEM",
    "BLOOD_LEAVES_ITEM",
    "BLOOD_SAPLING_ITEM",
    "BLOOD_STAIRS_ITEM",
    "BLOOD_SLAB_ITEM",
    "BLOOD_FENCE_ITEM",
    "BLOOD_FENCE_GATE_ITEM",
    "BLOOD_DOOR_ITEM",
    "BLOOD_TRAPDOOR_ITEM",
    # --- Magic_Arsenal blood items ---
    "MAGIC_FIRE_BLOOD_ITEM",
    "PERK_VAMPIRIC",
    "SCROLL_BLOOD",
    "RING_BLOODBORN",
    "STAFF_BLOOD",
    "FOCUS_BLOOD",
    "WIZARD_CHEST_BLOOD",
    "WIZARD_HELM_BLOOD",
    "WIZARD_LEGS_BLOOD",
    "WIZARD_BOOTS_BLOOD",
    # --- Spells de sangue (50 Iron's Spells) ---
    "SPELL_BLOOD_STEP",
    "SPELL_LIFEDRAIN",
    "SPELL_HEARTSTOP",
    "SPELL_BLOOD_SPEAR",
    "SPELL_SANGUINE_BIND",
    "SPELL_CRIMSON_MIST",
    "SPELL_VAMPIRIC_TOUCH",
    "SPELL_BLOOD_PACT",
]

src = FILE.read_text(encoding="utf-8")
original_len = len(src)

# Remove cada linha de output.accept(ModItems.<ITEM>.get())
# Padrão captura possíveis variantes: "output.accept" ou "out.accept", indentação preservada
removed_count = 0
for item in ABBADON_ITEMS:
    # Match linha inteira (com terminador) que contém .accept(ModItems.ITEM.get())
    pattern = r"^[ \t]*(?:output|out)\.accept\(ModItems\." + re.escape(item) + r"\.get\(\)\);\s*\n"
    new_src, n = re.subn(pattern, "", src, flags=re.MULTILINE)
    if n > 0:
        removed_count += n
        src = new_src
        print(f"  - removed {n}x {item}")
    else:
        print(f"  ! {item}: NOT FOUND")

print(f"\nTotal lines removed from MAIN+MAGIC_ARSENAL: {removed_count}")

# Agora insere a nova ABBADON tab ANTES de `private ModCreativeTabs()`
ABBADON_BLOCK = '''
    // ════════════════════════════════════════════════════════════════════════
    // r144: ABBADON TAB — Sangue, Carne, Culto, Sangue×Ordem (lugar de aniquilação)
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<CreativeModeTab> ABBADON = CREATIVE_MODE_TABS.register("abbadon",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Liberthia: Abbadon"))
                    .icon(() -> ModItems.HEART_OF_THE_MOTHER.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        // ─── Blood Fountain / Cure / Altar / Cauldron ───
                        out.accept(ModItems.BLOOD_FOUNTAIN_ITEM.get());
                        out.accept(ModItems.BLOOD_CURE_PILL.get());
                        out.accept(ModItems.BLOOD_ALTAR_ITEM.get());
                        out.accept(ModItems.BLOOD_CAULDRON_ITEM.get());
                        // ─── Flesh / Mother (Fase 2) ───
                        out.accept(ModItems.LIVING_FLESH_ITEM.get());
                        out.accept(ModItems.FLESH_MOTHER_ITEM.get());
                        out.accept(ModItems.ATTACKING_FLESH_ITEM.get());
                        out.accept(ModItems.HEART_OF_FLESH_BLOCK_ITEM.get());
                        out.accept(ModItems.HEART_OF_FLESH_ITEM.get());
                        out.accept(ModItems.HEART_OF_THE_MOTHER.get());
                        out.accept(ModItems.FLESH_MOTHER_BOSS_EGG.get());
                        out.accept(ModItems.FLESH_THREAD.get());
                        out.accept(ModItems.FLESH_CRAWLER_EGG.get());
                        // ─── Blood Infection / Volcano / Spike ───
                        out.accept(ModItems.BLOOD_INFECTION_BLOCK_ITEM.get());
                        out.accept(ModItems.BLOOD_INFESTATION_BLOCK_ITEM.get());
                        out.accept(ModItems.BLOOD_VOLCANO_ITEM.get());
                        out.accept(ModItems.BLOOD_SPIKE_ITEM.get());
                        out.accept(ModItems.BLOOD_BUCKET.get());
                        // ─── Blood Terrain variants ───
                        out.accept(ModItems.BLOOD_DIRT_ITEM.get());
                        out.accept(ModItems.BLOOD_SAND_ITEM.get());
                        out.accept(ModItems.BLOOD_STONE_ITEM.get());
                        out.accept(ModItems.BLOOD_COAL_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_IRON_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_GOLD_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_DIAMOND_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_REDSTONE_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_LAPIS_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_EMERALD_ORE_ITEM.get());
                        // ─── Blood Armor ───
                        out.accept(ModItems.BLOOD_HELMET.get());
                        out.accept(ModItems.BLOOD_CHESTPLATE.get());
                        out.accept(ModItems.BLOOD_LEGGINGS.get());
                        out.accept(ModItems.BLOOD_BOOTS.get());
                        // ─── Blood Scythe + Sanguine Core/Essence ───
                        out.accept(ModItems.BLOOD_SCYTHE.get());
                        out.accept(ModItems.SANGUINE_CORE.get());
                        out.accept(ModItems.SANGUINE_ESSENCE.get());
                        // ─── Order x Sangue (Fase 5) ───
                        out.accept(ModItems.ORDER_SHRINE_ITEM.get());
                        out.accept(ModItems.DESECRATED_HOLY_RELIC.get());
                        out.accept(ModItems.ORDER_PALADIN_EGG.get());
                        // ─── Weapons / Magic (Fase 4) ───
                        out.accept(ModItems.HEMOMANCER_STAFF.get());
                        out.accept(ModItems.BLOOD_BOW.get());
                        out.accept(ModItems.BLOOD_RITUAL_DAGGER.get());
                        out.accept(ModItems.BLOOD_PACT_AMULET.get());
                        // ─── Alquimia de Sangue (Fase 3) ───
                        out.accept(ModItems.BLOOD_VIAL.get());
                        out.accept(ModItems.BLOOD_VIAL_FILLED.get());
                        out.accept(ModItems.CONGEALED_BLOOD.get());
                        // ─── Culto do Sangue (Fase 1) ───
                        out.accept(ModItems.BLOODY_RAG.get());
                        out.accept(ModItems.RUSTED_DAGGER.get());
                        out.accept(ModItems.PRIEST_SIGIL.get());
                        out.accept(ModItems.TOME_OF_THE_MOTHER.get());
                        out.accept(ModItems.TOME_OF_THE_PILGRIM.get());
                        // ─── Spawn Eggs ───
                        out.accept(ModItems.GORE_WORM_EGG.get());
                        out.accept(ModItems.BLOOD_CULTIST_EGG.get());
                        out.accept(ModItems.BLOOD_PRIEST_EGG.get());
                        out.accept(ModItems.WOUNDED_PILGRIM_EGG.get());
                        out.accept(ModItems.BLOOD_MAGE_EGG.get());
                        out.accept(ModItems.BLOOD_HOUND_EGG.get());
                        out.accept(ModItems.BLOOD_WARDEN_EGG.get());
                        // ─── Seringa + Tainted ───
                        out.accept(ModItems.BLOOD_SYRINGE.get());
                        out.accept(ModItems.BLOOD_TELEPORT_PEARL.get());
                        out.accept(ModItems.TAINTED_APPLE.get());
                        // ─── Sanguine Ward (anti Blood Infection) ───
                        out.accept(ModItems.SANGUINE_WARD_HELMET.get());
                        out.accept(ModItems.SANGUINE_WARD_CHESTPLATE.get());
                        out.accept(ModItems.SANGUINE_WARD_LEGGINGS.get());
                        out.accept(ModItems.SANGUINE_WARD_BOOTS.get());
                        out.accept(ModItems.SANGUINE_WARD_SWORD.get());
                        out.accept(ModItems.SANGUINE_WARD_PICKAXE.get());
                        out.accept(ModItems.BLOOD_WARD_CHARM.get());
                        out.accept(ModItems.TAINTED_ESSENCE.get());
                        out.accept(ModItems.CLEANSING_SALT.get());
                        out.accept(ModItems.PURIFYING_FLASK.get());
                        // ─── Ritual blocks ───
                        out.accept(ModItems.BLOOD_SACRIFICIAL_BOWL_ITEM.get());
                        out.accept(ModItems.GOLDEN_BLOOD_BOWL_ITEM.get());
                        out.accept(ModItems.CRYSTALLIZED_BLOOD_SOUL.get());
                        // ─── Blood Attacking blocks ───
                        out.accept(ModItems.HEMORRHAGE_SPIKE_ITEM.get());
                        out.accept(ModItems.SANGUINE_SNARE_ITEM.get());
                        // ─── Occultism blood ───
                        out.accept(ModItems.BLOOD_CHALK.get());
                        out.accept(ModItems.BLOOD_TORCH_ITEM.get());
                        // ─── Sanguine Wood set ───
                        out.accept(ModItems.SANGUINE_LOG_ITEM.get());
                        out.accept(ModItems.SANGUINE_WOOD_ITEM.get());
                        out.accept(ModItems.STRIPPED_SANGUINE_LOG_ITEM.get());
                        out.accept(ModItems.STRIPPED_SANGUINE_WOOD_ITEM.get());
                        out.accept(ModItems.SANGUINE_PLANKS_ITEM.get());
                        out.accept(ModItems.SANGUINE_LEAVES_ITEM.get());
                        out.accept(ModItems.SANGUINE_SAPLING_ITEM.get());
                        out.accept(ModItems.SANGUINE_STAIRS_ITEM.get());
                        out.accept(ModItems.SANGUINE_SLAB_ITEM.get());
                        out.accept(ModItems.SANGUINE_FENCE_ITEM.get());
                        out.accept(ModItems.SANGUINE_FENCE_GATE_ITEM.get());
                        out.accept(ModItems.SANGUINE_BUTTON_ITEM.get());
                        out.accept(ModItems.SANGUINE_PRESSURE_PLATE_ITEM.get());
                        out.accept(ModItems.SANGUINE_DOOR_ITEM.get());
                        out.accept(ModItems.SANGUINE_TRAPDOOR_ITEM.get());
                        // ─── Blood Tree set ───
                        out.accept(ModItems.BLOOD_LOG_ITEM.get());
                        out.accept(ModItems.STRIPPED_BLOOD_LOG_ITEM.get());
                        out.accept(ModItems.BLOOD_PLANKS_ITEM.get());
                        out.accept(ModItems.BLOOD_LEAVES_ITEM.get());
                        out.accept(ModItems.BLOOD_SAPLING_ITEM.get());
                        out.accept(ModItems.BLOOD_STAIRS_ITEM.get());
                        out.accept(ModItems.BLOOD_SLAB_ITEM.get());
                        out.accept(ModItems.BLOOD_FENCE_ITEM.get());
                        out.accept(ModItems.BLOOD_FENCE_GATE_ITEM.get());
                        out.accept(ModItems.BLOOD_DOOR_ITEM.get());
                        out.accept(ModItems.BLOOD_TRAPDOOR_ITEM.get());
                        // ─── Magic-school Blood ───
                        out.accept(ModItems.MAGIC_FIRE_BLOOD_ITEM.get());
                        out.accept(ModItems.PERK_VAMPIRIC.get());
                        out.accept(ModItems.SCROLL_BLOOD.get());
                        out.accept(ModItems.RING_BLOODBORN.get());
                        out.accept(ModItems.STAFF_BLOOD.get());
                        out.accept(ModItems.FOCUS_BLOOD.get());
                        out.accept(ModItems.WIZARD_CHEST_BLOOD.get());
                        out.accept(ModItems.WIZARD_HELM_BLOOD.get());
                        out.accept(ModItems.WIZARD_LEGS_BLOOD.get());
                        out.accept(ModItems.WIZARD_BOOTS_BLOOD.get());
                        // ─── Spells de sangue (Iron's Spells) ───
                        out.accept(ModItems.SPELL_BLOOD_STEP.get());
                        out.accept(ModItems.SPELL_LIFEDRAIN.get());
                        out.accept(ModItems.SPELL_HEARTSTOP.get());
                        out.accept(ModItems.SPELL_BLOOD_SPEAR.get());
                        out.accept(ModItems.SPELL_SANGUINE_BIND.get());
                        out.accept(ModItems.SPELL_CRIMSON_MIST.get());
                        out.accept(ModItems.SPELL_VAMPIRIC_TOUCH.get());
                        out.accept(ModItems.SPELL_BLOOD_PACT.get());
                    })
                    .build());

'''

# Insere antes de "private ModCreativeTabs()"
marker = "    private ModCreativeTabs() {"
if marker not in src:
    raise SystemExit("ERRO: marker `private ModCreativeTabs()` não encontrado")
src = src.replace(marker, ABBADON_BLOCK + marker, 1)

FILE.write_text(src, encoding="utf-8")
print(f"\n✅ Arquivo escrito. {original_len} → {len(src)} bytes (diff {len(src)-original_len:+})")
print(f"   Tab ABBADON criada com {sum(1 for line in ABBADON_BLOCK.split(chr(10)) if 'out.accept' in line)} items.")
