#!/usr/bin/env python3
"""r159: Bulk-generate recipes + loot tables para todos os blocos magicos que
faltam, mais recipes para componentes (Arcane Workbench, threads, modifiers).

NÃO cria recipes para feitiços (esses só dropam ou rituais).
"""
import json
from pathlib import Path

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
RECIPES = ROOT / "data" / "liberthia" / "recipes"
LOOT_BLOCKS = ROOT / "data" / "liberthia" / "loot_tables" / "blocks"
RECIPES.mkdir(parents=True, exist_ok=True)
LOOT_BLOCKS.mkdir(parents=True, exist_ok=True)


def shaped(name, pattern, key, result, count=1):
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": key,
        "result": {"item": result, "count": count}
    }


def shapeless(name, ingredients, result, count=1):
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ingredients,
        "result": {"item": result, "count": count}
    }


def self_drop_loot(block_id):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": block_id}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]
        }]
    }


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


# ═══════════════════════════════════════════════════════════════════════════
# BLOCK RECIPES (crafted from gold/iron/diamond + magic materials)
# ═══════════════════════════════════════════════════════════════════════════
BLOCK_RECIPES = [
    # (block_id, pattern, key)
    ("arcane_workbench", ["GAG", "STS", "QQQ"], {
        "G": {"item": "minecraft:gold_ingot"},
        "A": {"item": "liberthia:purified_essence"},
        "S": {"item": "liberthia:dark_matter_shard"},
        "T": {"item": "liberthia:spell_weaver"},
        "Q": {"item": "minecraft:quartz_block"}
    }),
    ("spell_weaver", ["LSL", "ASA", "SSS"], {
        "L": {"item": "minecraft:lapis_lazuli"},
        "S": {"item": "minecraft:smooth_stone"},
        "A": {"item": "liberthia:purified_essence"}
    }),
    ("spell_mutator", ["ESE", "SAS", "ESE"], {
        "E": {"item": "minecraft:ender_pearl"},
        "S": {"item": "liberthia:purified_essence"},
        "A": {"item": "liberthia:dark_matter_shard"}
    }),
    ("glyph_inscriber", ["FQF", "QPQ", "FQF"], {
        "F": {"item": "minecraft:feather"},
        "Q": {"item": "minecraft:quartz"},
        "P": {"item": "minecraft:paper"}
    }),
    ("source_jar", [" G ", "QSQ", "QQQ"], {
        "G": {"item": "minecraft:glass"},
        "Q": {"item": "minecraft:quartz"},
        "S": {"item": "liberthia:purified_essence"}
    }),
    ("scribes_table", ["PQP", "WIW", "WIW"], {
        "P": {"item": "minecraft:paper"},
        "Q": {"item": "minecraft:quartz"},
        "W": {"item": "minecraft:oak_planks"},
        "I": {"item": "minecraft:iron_ingot"}
    }),
    ("imbuement_table", ["LDL", "QPQ", "SSS"], {
        "L": {"item": "minecraft:lapis_lazuli"},
        "D": {"item": "minecraft:diamond"},
        "Q": {"item": "minecraft:quartz_block"},
        "P": {"item": "liberthia:purified_essence"},
        "S": {"item": "minecraft:smooth_stone"}
    }),
    ("spell_binding_pedestal", [" P ", "LBL", "SSS"], {
        "P": {"item": "minecraft:paper"},
        "L": {"item": "minecraft:lapis_lazuli"},
        "B": {"item": "minecraft:book"},
        "S": {"item": "minecraft:smooth_stone"}
    }),
    ("ritual_brazier", ["I I", "ICI", " I "], {
        "I": {"item": "minecraft:iron_ingot"},
        "C": {"item": "minecraft:coal"}
    }),
    ("spell_prism", [" Q ", "QGQ", " Q "], {
        "Q": {"item": "minecraft:quartz"},
        "G": {"item": "minecraft:glass"}
    }),
    ("spell_sensor", ["RQR", "QGQ", "RQR"], {
        "R": {"item": "minecraft:redstone"},
        "Q": {"item": "minecraft:quartz"},
        "G": {"item": "minecraft:glass"}
    }),
    ("spell_turret", [" T ", "TQT", "RRR"], {
        "T": {"item": "minecraft:iron_block"},
        "Q": {"item": "minecraft:quartz_block"},
        "R": {"item": "minecraft:redstone_block"}
    }),
    ("inscription_table", ["LIL", "QPQ", "WWW"], {
        "L": {"item": "minecraft:lapis_lazuli"},
        "I": {"item": "minecraft:ink_sac"},
        "Q": {"item": "minecraft:quartz"},
        "P": {"item": "minecraft:paper"},
        "W": {"item": "minecraft:oak_planks"}
    }),
    ("scroll_forge", ["PFP", "QSQ", "BBB"], {
        "P": {"item": "minecraft:paper"},
        "F": {"item": "minecraft:fire_charge"},
        "Q": {"item": "minecraft:quartz_block"},
        "S": {"item": "liberthia:purified_essence"},
        "B": {"item": "minecraft:blackstone"}
    }),
    # Weaves: 4 thread → 1 weave block
    ("mirror_weave", ["GTG", "TST", "GTG"], {
        "G": {"item": "minecraft:glass"},
        "T": {"item": "liberthia:pale_thread"},
        "S": {"item": "liberthia:purified_essence"}
    }),
    ("sky_weave", ["FTF", "TLT", "FTF"], {
        "F": {"item": "minecraft:feather"},
        "T": {"item": "liberthia:pale_thread"},
        "L": {"item": "minecraft:lapis_lazuli"}
    }),
    ("ghost_weave", ["WTW", "TSE", "WTW"], {
        "W": {"item": "minecraft:white_dye"},
        "T": {"item": "liberthia:pale_thread"},
        "S": {"item": "liberthia:purified_essence"},
        "E": {"item": "minecraft:ender_pearl"}
    }),
    ("false_weave", ["RTR", "TBT", "RTR"], {
        "R": {"item": "minecraft:redstone"},
        "T": {"item": "liberthia:pale_thread"},
        "B": {"item": "minecraft:blaze_powder"}
    }),
    # Walls of magic
    ("fire_wall", ["BFB", "FQF", "BFB"], {
        "B": {"item": "minecraft:blaze_powder"},
        "F": {"item": "minecraft:fire_charge"},
        "Q": {"item": "minecraft:quartz_block"}
    }),
    ("ice_wall", ["IPI", "PLP", "IPI"], {
        "I": {"item": "minecraft:ice"},
        "P": {"item": "minecraft:packed_ice"},
        "L": {"item": "minecraft:lapis_lazuli"}
    }),
    ("lightning_wall", ["RGR", "GQG", "RGR"], {
        "R": {"item": "minecraft:redstone"},
        "G": {"item": "minecraft:glowstone"},
        "Q": {"item": "minecraft:quartz_block"}
    }),
    ("holy_wall", ["GQG", "QDQ", "GQG"], {
        "G": {"item": "minecraft:glowstone"},
        "Q": {"item": "minecraft:quartz_block"},
        "D": {"item": "minecraft:diamond"}
    }),
    # Sourcelinks
    ("volcanic_sourcelink", ["NBN", "BLB", "NBN"], {
        "N": {"item": "minecraft:netherrack"},
        "B": {"item": "minecraft:blaze_powder"},
        "L": {"item": "liberthia:purified_essence"}
    }),
    ("mycelial_sourcelink", ["MBM", "BLB", "MBM"], {
        "M": {"item": "minecraft:mycelium"},
        "B": {"item": "minecraft:bone_meal"},
        "L": {"item": "liberthia:purified_essence"}
    }),
    ("vitalic_sourcelink", ["FBF", "BLB", "FBF"], {
        "F": {"item": "minecraft:rotten_flesh"},
        "B": {"item": "minecraft:bone"},
        "L": {"item": "liberthia:purified_essence"}
    }),
    ("alchemical_sourcelink", ["GBG", "BLB", "GBG"], {
        "G": {"item": "minecraft:glass_bottle"},
        "B": {"item": "minecraft:brewing_stand"},
        "L": {"item": "liberthia:purified_essence"}
    }),
    ("agronomic_sourcelink", ["GBG", "BLB", "GBG"], {
        "G": {"item": "minecraft:wheat"},
        "B": {"item": "minecraft:hay_block"},
        "L": {"item": "liberthia:purified_essence"}
    }),
    # Auto blocks
    ("auto_miner", ["IRI", "RPR", "ICI"], {
        "I": {"item": "minecraft:iron_block"},
        "R": {"item": "minecraft:redstone_block"},
        "P": {"item": "minecraft:diamond_pickaxe"},
        "C": {"item": "minecraft:chest"}
    }),
    ("mage_cauldron", ["I I", "IQI", "ICI"], {
        "I": {"item": "minecraft:iron_ingot"},
        "Q": {"item": "minecraft:cauldron"},
        "C": {"item": "liberthia:purified_essence"}
    }),
    ("whirlwind", ["FRF", "RQR", "FRF"], {
        "F": {"item": "minecraft:feather"},
        "R": {"item": "minecraft:redstone"},
        "Q": {"item": "minecraft:quartz_block"}
    }),
    # Repository
    ("repository", ["CBC", "BQB", "CBC"], {
        "C": {"item": "minecraft:chest"},
        "B": {"item": "minecraft:bookshelf"},
        "Q": {"item": "minecraft:quartz_block"}
    }),
    # Source Relay
    ("source_relay", [" Q ", "QSQ", " Q "], {
        "Q": {"item": "minecraft:quartz_block"},
        "S": {"item": "liberthia:purified_essence"}
    }),
    # Spell Mutator already added; Lay Line is worldgen-only no recipe
    # Spirit Conduit, Source Transmuter
    ("spirit_conduit", ["EQE", "QPQ", "EQE"], {
        "E": {"item": "minecraft:end_stone"},
        "Q": {"item": "minecraft:quartz_block"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    ("source_transmuter", ["NQN", "QPQ", "NQN"], {
        "N": {"item": "minecraft:netherrack"},
        "Q": {"item": "minecraft:quartz_block"},
        "P": {"item": "liberthia:purified_essence"}
    }),
]


# ═══════════════════════════════════════════════════════════════════════════
# COMPONENT RECIPES (tablets, orbs, school runes, threads, modifier glyphs)
# ═══════════════════════════════════════════════════════════════════════════
COMPONENT_RECIPES = [
    # Magic Tablet — stone + arcane essence
    ("magic_tablet", ["SSS", "SPS", "SSS"], {
        "S": {"item": "minecraft:smooth_stone"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    # Arcane Orb — diamond + ender pearl + essence
    ("arcane_orb", [" P ", "DED", " P "], {
        "P": {"item": "liberthia:purified_essence"},
        "D": {"item": "minecraft:diamond"},
        "E": {"item": "minecraft:ender_pearl"}
    }),
    # Pale Thread — string + ghost essence (cheap)
    ("pale_thread", ["S", "S", "G"], {
        "S": {"item": "minecraft:string"},
        "G": {"item": "minecraft:ghast_tear"}
    }),
    # Flesh Thread — string + rotten flesh
    ("flesh_thread", ["S", "F", "S"], {
        "S": {"item": "minecraft:string"},
        "F": {"item": "minecraft:rotten_flesh"}
    }),
    # Thread of Distance — string + ender eye (rare)
    ("thread_of_distance", ["E", "S", "E"], {
        "E": {"item": "minecraft:ender_eye"},
        "S": {"item": "minecraft:string"}
    }),
    # Focus by school — gold + dye + essence
    ("focus_fire", [" R ", "GPG", " R "], {
        "R": {"item": "minecraft:red_dye"},
        "G": {"item": "minecraft:gold_ingot"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    ("focus_ice", [" L ", "GPG", " L "], {
        "L": {"item": "minecraft:light_blue_dye"},
        "G": {"item": "minecraft:gold_ingot"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    ("focus_lightning", [" Y ", "GPG", " Y "], {
        "Y": {"item": "minecraft:yellow_dye"},
        "G": {"item": "minecraft:gold_ingot"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    ("focus_blood", [" R ", "GBG", " R "], {
        "R": {"item": "minecraft:redstone"},
        "G": {"item": "minecraft:gold_ingot"},
        "B": {"item": "minecraft:rotten_flesh"}
    }),
    ("focus_eldritch", [" U ", "GPG", " U "], {
        "U": {"item": "minecraft:purple_dye"},
        "G": {"item": "minecraft:gold_ingot"},
        "P": {"item": "minecraft:ender_eye"}
    }),
    ("focus_holy", [" W ", "GPG", " W "], {
        "W": {"item": "minecraft:white_dye"},
        "G": {"item": "minecraft:gold_ingot"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    ("focus_nature", [" G ", "GPG", " G "], {
        "G": {"item": "minecraft:green_dye"},
        "P": {"item": "liberthia:purified_essence"}
    }),
    # School Runes — chiseled stone + school-specific reagent
    ("school_rune_fire", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:blaze_powder"}
    }),
    ("school_rune_ice", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:packed_ice"}
    }),
    ("school_rune_lightning", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:lightning_rod"}
    }),
    ("school_rune_blood", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:rotten_flesh"}
    }),
    ("school_rune_eldritch", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:ender_eye"}
    }),
    ("school_rune_holy", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:glowstone_dust"}
    }),
    ("school_rune_nature", ["BBB", "BPB", "BBB"], {
        "B": {"item": "minecraft:blackstone"},
        "P": {"item": "minecraft:oak_sapling"}
    }),
    # Modifier Glyphs — paper + essence + specific reagent per modifier
    ("modifier_amplify", [" P ", "PEP", " P "], {
        "P": {"item": "minecraft:paper"},
        "E": {"item": "minecraft:redstone"}
    }),
    ("modifier_aoe", ["P P", " E ", "P P"], {
        "P": {"item": "minecraft:paper"},
        "E": {"item": "minecraft:gunpowder"}
    }),
    ("modifier_pierce", [" P ", "PSP", " P "], {
        "P": {"item": "minecraft:paper"},
        "S": {"item": "minecraft:arrow"}
    }),
    ("modifier_speed", [" P ", "PFP", " P "], {
        "P": {"item": "minecraft:paper"},
        "F": {"item": "minecraft:feather"}
    }),
    # CDR Glyph — paper + ender + clock
    ("glyph_cooldown_reduction", ["PCP", "PEP", "PRP"], {
        "P": {"item": "minecraft:paper"},
        "C": {"item": "minecraft:clock"},
        "E": {"item": "minecraft:ender_eye"},
        "R": {"item": "minecraft:redstone"}
    }),
]

# ═══════════════════════════════════════════════════════════════════════════
# LOOT TABLES — self drop pra todos os blocks acima
# ═══════════════════════════════════════════════════════════════════════════
ALL_BLOCK_IDS = [b[0] for b in BLOCK_RECIPES]


def main():
    written_r = 0
    written_l = 0

    # Block recipes + loot tables
    for entry in BLOCK_RECIPES:
        name, pattern, key = entry
        recipe = shaped(name, pattern, key, f"liberthia:{name}")
        path = RECIPES / f"{name}.json"
        if not path.exists():
            write(path, recipe)
            written_r += 1
        # Self-drop loot table
        loot_path = LOOT_BLOCKS / f"{name}.json"
        if not loot_path.exists():
            write(loot_path, self_drop_loot(f"liberthia:{name}"))
            written_l += 1

    # Component recipes
    for entry in COMPONENT_RECIPES:
        name, pattern, key = entry
        recipe = shaped(name, pattern, key, f"liberthia:{name}")
        path = RECIPES / f"{name}.json"
        if not path.exists():
            write(path, recipe)
            written_r += 1

    print(f"OK — wrote {written_r} new recipes, {written_l} new loot tables")


if __name__ == "__main__":
    main()
