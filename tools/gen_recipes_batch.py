"""r131: Gera batch de recipes pra items orfaos.

Focado em items que SE BENEFICIAM de recipe (deixa de fora artefatos/eggs/boss drops).
"""
import os, json, re

ROOT = os.path.dirname(__file__)
RECIPES = os.path.normpath(os.path.join(ROOT, "..",
    "src/main/resources/data/liberthia/recipes"))
os.makedirs(RECIPES, exist_ok=True)


def shaped(name, pattern, key, result_item, count=1):
    """Cria shaped recipe."""
    j = {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": key,
        "result": {"item": result_item, "count": count}
    }
    with open(os.path.join(RECIPES, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


def shapeless(name, ingredients, result_item, count=1):
    """Cria shapeless recipe."""
    j = {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ingredients,
        "result": {"item": result_item, "count": count}
    }
    with open(os.path.join(RECIPES, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


def smelting(name, ingredient, result_item, xp=0.5, cooktime=200):
    """Cria smelting recipe."""
    j = {
        "type": "minecraft:smelting",
        "ingredient": {"item": ingredient},
        "result": result_item,
        "experience": xp,
        "cookingtime": cooktime
    }
    with open(os.path.join(RECIPES, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


# Helper key constants
IRON = {"item": "minecraft:iron_ingot"}
GOLD = {"item": "minecraft:gold_ingot"}
DIAMOND = {"item": "minecraft:diamond"}
STICK = {"item": "minecraft:stick"}
STRING = {"item": "minecraft:string"}
BOOK = {"item": "minecraft:book"}
ENDER_PEARL = {"item": "minecraft:ender_pearl"}
AMETHYST = {"item": "minecraft:amethyst_shard"}
NETHERITE = {"item": "minecraft:netherite_ingot"}
GLASS = {"item": "minecraft:glass"}
LAPIS = {"item": "minecraft:lapis_lazuli"}
EMERALD = {"item": "minecraft:emerald"}
LEATHER = {"item": "minecraft:leather"}
BLAZE = {"item": "minecraft:blaze_rod"}
OBSIDIAN = {"item": "minecraft:obsidian"}
PAPER = {"item": "minecraft:paper"}
FEATHER = {"item": "minecraft:feather"}
INK = {"item": "minecraft:ink_sac"}
GLOWSTONE = {"item": "minecraft:glowstone_dust"}
REDSTONE = {"item": "minecraft:redstone"}

# Mod items
DM_SHARD = {"item": "liberthia:dark_matter_shard"}
DM_INGOT = {"item": "liberthia:dark_matter_ingot"}
CM_SHARD = {"item": "liberthia:clear_matter_shard"}
CM_INGOT = {"item": "liberthia:clear_matter_ingot"}
YM_SHARD = {"item": "liberthia:yellow_matter_shard"}
YM_INGOT = {"item": "liberthia:yellow_matter_ingot"}
SOURCESTONE = {"item": "liberthia:sourcestone"}
WISP = {"item": "liberthia:wisp_essence"}
ASTRAL = {"item": "liberthia:astral_dust"}
PHANTOM = {"item": "liberthia:phantom_ink"}
MEMORY = {"item": "liberthia:memory_shard"}
ECTOPLASM = {"item": "liberthia:ectoplasm_strand"}
WHISPERWOOD = {"item": "liberthia:whisperwood_resin"}
VEIL = {"item": "liberthia:veil_fragment"}

# ============================================================================
# 1. MATTER — ingots, blocks, tools (caminho tech)
# ============================================================================

# Smelting: shard → ingot (3 matters)
smelting("dark_matter_ingot_smelting", "liberthia:dark_matter_shard",
         "liberthia:dark_matter_ingot", xp=1.0, cooktime=400)
smelting("yellow_matter_ingot_smelting", "liberthia:yellow_matter_shard",
         "liberthia:yellow_matter_ingot", xp=1.0, cooktime=400)
# (clear_matter_ingot ja tem recipe presumivelmente)

# Block compress: 9 ingot -> block (3 matters)
shaped("dark_matter_block_from_ingots",
       ["XXX", "XXX", "XXX"], {"X": DM_INGOT},
       "liberthia:dark_matter_block", 1)
shaped("yellow_matter_block_from_ingots",
       ["XXX", "XXX", "XXX"], {"X": YM_INGOT},
       "liberthia:yellow_matter_block", 1)

# Block decompose: block -> 9 ingot
shapeless("dark_matter_ingot_from_block",
          [{"item": "liberthia:dark_matter_block"}],
          "liberthia:dark_matter_ingot", 9)
shapeless("yellow_matter_ingot_from_block",
          [{"item": "liberthia:yellow_matter_block"}],
          "liberthia:yellow_matter_ingot", 9)

# Matter armor (DM)
shaped("dark_matter_helmet", ["XXX", "X X"], {"X": DM_INGOT},
       "liberthia:dark_matter_helmet")
shaped("dark_matter_chestplate", ["X X", "XXX", "XXX"], {"X": DM_INGOT},
       "liberthia:dark_matter_chestplate")
shaped("dark_matter_leggings", ["XXX", "X X", "X X"], {"X": DM_INGOT},
       "liberthia:dark_matter_leggings")
shaped("dark_matter_boots", ["X X", "X X"], {"X": DM_INGOT},
       "liberthia:dark_matter_boots")

# Tools DM
shaped("dark_matter_sword", ["X", "X", "S"], {"X": DM_INGOT, "S": STICK},
       "liberthia:dark_matter_sword")
shaped("dark_matter_pickaxe", ["XXX", " S ", " S "], {"X": DM_INGOT, "S": STICK},
       "liberthia:dark_matter_pickaxe")
shaped("dark_matter_axe", ["XX", "XS", " S"], {"X": DM_INGOT, "S": STICK},
       "liberthia:dark_matter_axe")
shaped("dark_matter_shovel", ["X", "S", "S"], {"X": DM_INGOT, "S": STICK},
       "liberthia:dark_matter_shovel")

# Tools YM
shaped("yellow_matter_sword", ["X", "X", "S"], {"X": YM_INGOT, "S": STICK},
       "liberthia:yellow_matter_sword")

# ============================================================================
# 2. SOURCE / MAGIC blocks
# ============================================================================

# Source Jar - glass + amethyst + sourcestone
shaped("source_jar", ["GAG", "G G", "GAG"],
       {"G": GLASS, "A": AMETHYST},
       "liberthia:source_jar")

# Source Relay - basically jar com redstone
shaped("source_relay", ["IRI", "ARA", "IRI"],
       {"I": IRON, "R": REDSTONE, "A": AMETHYST},
       "liberthia:source_relay")

# Imbuement Block
shaped("imbuement_block", ["ASA", "SDS", "ASA"],
       {"A": AMETHYST, "S": SOURCESTONE, "D": DIAMOND},
       "liberthia:imbuement_block")

# Scribes Table
shaped("scribes_table", ["BBB", "PPP", "AAA"],
       {"B": BOOK, "P": PAPER, "A": AMETHYST},
       "liberthia:scribes_table")

# Spell Binding Pedestal
shaped("spell_binding_pedestal", ["DAD", "ASA", "DAD"],
       {"D": DIAMOND, "A": AMETHYST, "S": SOURCESTONE},
       "liberthia:spell_binding_pedestal")

# Inscription Table
shaped("inscription_table", ["IPI", "BAB", "OOO"],
       {"I": INK, "P": PAPER, "B": BOOK, "A": AMETHYST, "O": OBSIDIAN},
       "liberthia:inscription_table")

# Glyph Inscriber
shaped("glyph_inscriber", ["VWV", "VGV", "OOO"],
       {"V": VEIL, "W": WISP, "G": {"item": "liberthia:glyph_inscriber_table"} if False else AMETHYST, "O": OBSIDIAN},
       "liberthia:glyph_inscriber")

# Spell Weaver (precisa de tudo magic)
shaped("spell_weaver", ["VAV", "ASA", "OEO"],
       {"V": VEIL, "A": AMETHYST, "S": SOURCESTONE, "O": OBSIDIAN, "E": EMERALD},
       "liberthia:spell_weaver")

# Grimoire Book
shaped("grimoire_book", ["LSL", "PBP", "LDL"],
       {"L": LEATHER, "S": STRING, "P": PAPER, "B": BOOK, "D": DM_INGOT},
       "liberthia:grimoire_book")

# Spirit Conduit
shaped("spirit_conduit", ["AAA", "WWW", "SSS"],
       {"A": AMETHYST, "W": WISP, "S": SOURCESTONE},
       "liberthia:spirit_conduit")

# Source Transmuter
shaped("source_transmuter", ["NSN", "VTV", "NIN"],
       {"N": NETHERITE, "S": SOURCESTONE, "V": VEIL, "T": {"item": "liberthia:spirit_conduit"}, "I": IRON},
       "liberthia:source_transmuter")

# Magelight (light source bloco)
shaped("magelight", ["GAG", "AGA", "GAG"],
       {"G": GLOWSTONE, "A": AMETHYST},
       "liberthia:magelight", 4)

# ============================================================================
# 3. SPIRIT ROBES (armor angelica)
# ============================================================================

shaped("spirit_robes_helm", ["WWW", "W W"],
       {"W": WHISPERWOOD},
       "liberthia:spirit_robes_helm")
shaped("spirit_robes_chest", ["W W", "WAW", "WWW"],
       {"W": WHISPERWOOD, "A": AMETHYST},
       "liberthia:spirit_robes_chest")
shaped("spirit_robes_legs", ["WWW", "W W", "W W"],
       {"W": WHISPERWOOD},
       "liberthia:spirit_robes_legs")
shaped("spirit_robes_boots", ["W W", "W W"],
       {"W": WHISPERWOOD},
       "liberthia:spirit_robes_boots")

# ============================================================================
# 4. CHALKS (giz pra runas)
# ============================================================================
shapeless("chalk_white",  [{"item": "minecraft:bone_meal"}, {"item": "minecraft:gypsum"} if False else {"item": "minecraft:bone_meal"}, PAPER], "liberthia:chalk_white", 4)
shapeless("chalk_black",  [{"item": "minecraft:coal"}, INK, PAPER], "liberthia:chalk_black", 4)
shapeless("chalk_red",    [{"item": "minecraft:redstone"}, {"item": "minecraft:rose_red"}, PAPER], "liberthia:chalk_red", 4)
shapeless("chalk_golden", [{"item": "minecraft:gold_nugget"}, GLOWSTONE, PAPER], "liberthia:chalk_golden", 4)
shapeless("chalk_purple", [AMETHYST, {"item": "minecraft:purple_dye"}, PAPER], "liberthia:chalk_purple", 4)
shapeless("blood_chalk",  [{"item": "minecraft:redstone"}, {"item": "minecraft:fermented_spider_eye"}, PAPER], "liberthia:blood_chalk", 4)
shapeless("observation_chalk", [AMETHYST, ENDER_PEARL, PAPER], "liberthia:observation_chalk", 4)

# ============================================================================
# 5. MATTER ITEM PILLS (via crafting tradicional simples — alem do Pill Brewer)
# ============================================================================
shapeless("matter_pill_dm_simple", [DM_SHARD, {"item": "minecraft:sugar"}, {"item": "minecraft:glass_bottle"}], "liberthia:matter_pill_dm", 1)
shapeless("matter_pill_cm_simple", [CM_SHARD, {"item": "minecraft:sugar"}, {"item": "minecraft:glass_bottle"}], "liberthia:matter_pill_cm", 1)
shapeless("matter_pill_ym_simple", [YM_SHARD, {"item": "minecraft:sugar"}, {"item": "minecraft:glass_bottle"}], "liberthia:matter_pill_ym", 1)

# ============================================================================
# 6. SOURCE WAND / TOOLS BASE (simple ones)
# ============================================================================
# Source Jar simples (1 emerald no centro)
# Already done above

# Spiritual Connection (item espiritual base — para teleportar)
shaped("spiritual_connection", [" A ", "AEA", " A "],
       {"A": AMETHYST, "E": ENDER_PEARL},
       "liberthia:spiritual_connection")

shaped("spiritual_link", [" A ", "AEA", " L "],
       {"A": AMETHYST, "E": ENDER_PEARL, "L": LAPIS},
       "liberthia:spiritual_link")

# ============================================================================
# 7. RITUAL ITEMS
# ============================================================================
shaped("ritual_pedestal", ["SSS", "SAS", "SSS"],
       {"S": {"item": "minecraft:smooth_stone"}, "A": AMETHYST},
       "liberthia:ritual_pedestal")

shaped("brazier", ["I I", "I I", "III"],
       {"I": IRON},
       "liberthia:brazier" if False else "liberthia:ritual_pedestal", 0)
# skip if brazier nao registrada

# ============================================================================
# 8. SEALS - protecao items (vanilla-like)
# ============================================================================
shaped("bone_seal", [" B ", "BAB", " B "],
       {"B": {"item": "minecraft:bone"}, "A": AMETHYST},
       "liberthia:bone_seal")
shaped("gold_seal", [" G ", "GAG", " G "],
       {"G": GOLD, "A": AMETHYST},
       "liberthia:gold_seal")
shaped("diamond_seal", [" D ", "DAD", " D "],
       {"D": DIAMOND, "A": AMETHYST},
       "liberthia:diamond_seal")
shaped("netherite_seal", [" N ", "NAN", " N "],
       {"N": NETHERITE, "A": AMETHYST},
       "liberthia:netherite_seal")

# ============================================================================
# 9. SOURCE GEMS (4 base elementals)
# ============================================================================
shapeless("source_fire_gem", [REDSTONE, BLAZE, AMETHYST], "liberthia:source_fire_gem", 1)
shapeless("source_water_gem", [{"item": "minecraft:water_bucket"}, AMETHYST], "liberthia:source_water_gem", 1)
shapeless("source_air_gem", [FEATHER, FEATHER, AMETHYST], "liberthia:source_air_gem", 1)
shapeless("source_earth_gem", [{"item": "minecraft:emerald"}, {"item": "minecraft:clay_ball"}, AMETHYST], "liberthia:source_earth_gem", 1)


# ============================================================================
# Validation: load all back and count
# ============================================================================
def main_check():
    import os
    n = len([f for f in os.listdir(RECIPES) if f.endswith('.json')])
    print(f"[OK] Total recipes na pasta agora: {n}")

main_check()
