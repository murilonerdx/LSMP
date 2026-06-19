"""r134: gera BiomeModifier JSONs pra spawning natural de mobs.

Forge 1.20.1 usa neoforge:add_spawns / forge:add_spawns format.
Coloca em data/liberthia/forge/biome_modifier/<nome>.json e referencia
em data/liberthia/neoforge/biome_modifier/<nome>.json (Forge 1.20+).

Categorias:
- Cosmic horror mobs: spawning em dark biomes (dark forest, deep dark)
- Spirit world mobs: spawning na dim spirit_world
- Blood mobs: spawning em swamps escuras / nether
- Wizard mobs: spawning raros em florestas
"""
import os, json

ROOT = os.path.dirname(__file__)
OUT_DIR = os.path.normpath(os.path.join(ROOT, "..",
    "src/main/resources/data/liberthia/forge/biome_modifier"))
os.makedirs(OUT_DIR, exist_ok=True)


def make_spawn_modifier(name, biomes, mob_type, weight, minCount=1, maxCount=3,
                         category="MONSTER"):
    """Gera 1 JSON biome modifier add_spawns."""
    j = {
        "type": "forge:add_spawns",
        "biomes": biomes,  # list ou tag #namespace:path
        "spawners": [{
            "type": mob_type,
            "weight": weight,
            "minCount": minCount,
            "maxCount": maxCount
        }]
    }
    path = os.path.join(OUT_DIR, f"{name}.json")
    with open(path, "w") as f:
        json.dump(j, f, indent=2)
    return path


# === COSMIC HORROR mobs em dark biomes ===
make_spawn_modifier("empty_man_dark", "#minecraft:is_forest",
                     "liberthia:empty_man", weight=2, minCount=1, maxCount=1)
make_spawn_modifier("observer_dark", "#minecraft:is_taiga",
                     "liberthia:observer", weight=3, minCount=1, maxCount=2)
make_spawn_modifier("absence_void", "#minecraft:is_end",
                     "liberthia:absence", weight=5, minCount=1, maxCount=2)
make_spawn_modifier("remembered_taiga", "#minecraft:is_taiga",
                     "liberthia:remembered", weight=2, minCount=1, maxCount=1)

# === BLOOD mobs em swamps + nether ===
make_spawn_modifier("blood_hound_swamp", "minecraft:swamp",
                     "liberthia:blood_hound", weight=8, minCount=2, maxCount=4)
make_spawn_modifier("blood_cultist_swamp", "minecraft:swamp",
                     "liberthia:blood_cultist", weight=5, minCount=1, maxCount=3)
make_spawn_modifier("blood_priest_dark", "#minecraft:is_forest",
                     "liberthia:blood_priest", weight=2, minCount=1, maxCount=1)
make_spawn_modifier("wounded_pilgrim_savanna", "minecraft:savanna",
                     "liberthia:wounded_pilgrim", weight=4, minCount=1, maxCount=2)
make_spawn_modifier("flesh_crawler_nether", "minecraft:nether_wastes",
                     "liberthia:flesh_crawler", weight=6, minCount=2, maxCount=5)
make_spawn_modifier("gore_worm_nether", "minecraft:soul_sand_valley",
                     "liberthia:gore_worm", weight=5, minCount=1, maxCount=3)

# === WIZARD mobs raros em florestas / planícies ===
make_spawn_modifier("apothecarist_forest", "#minecraft:is_forest",
                     "liberthia:apothecarist", weight=1, minCount=1, maxCount=1)
make_spawn_modifier("keeper_taiga", "#minecraft:is_taiga",
                     "liberthia:keeper", weight=1, minCount=1, maxCount=1)
make_spawn_modifier("archevoker_dark", "minecraft:dark_forest",
                     "liberthia:archevoker", weight=1, minCount=1, maxCount=1)
make_spawn_modifier("blood_mage_swamp", "minecraft:swamp",
                     "liberthia:blood_mage", weight=2, minCount=1, maxCount=2)
make_spawn_modifier("order_paladin_plains", "#minecraft:is_overworld",
                     "liberthia:order_paladin", weight=1, minCount=1, maxCount=2,
                     category="CREATURE")

# === FAMILIARS (não hostis, rare encounters) ===
# Carbuncle/Whelp/AmethystGolem aparecem como passivos
make_spawn_modifier("carbuncle_lush", "minecraft:lush_caves",
                     "liberthia:carbuncle", weight=3, minCount=1, maxCount=2,
                     category="CREATURE")
make_spawn_modifier("amethyst_golem_geode", "#minecraft:is_overworld",
                     "liberthia:amethyst_golem", weight=1, minCount=1, maxCount=1,
                     category="CREATURE")

# === DARK MATTER spore (raro overworld) ===
make_spawn_modifier("corrupted_zombie_nether", "minecraft:nether_wastes",
                     "liberthia:corrupted_zombie", weight=4, minCount=1, maxCount=3)
make_spawn_modifier("spore_spitter_jungle", "minecraft:jungle",
                     "liberthia:spore_spitter", weight=2, minCount=1, maxCount=2)

# === DISARMER (raro, nighttime) ===
make_spawn_modifier("disarmer_plains", "minecraft:plains",
                     "liberthia:disarmer", weight=1, minCount=1, maxCount=1)

# === LOOM dimension mobs — colocadas via worldgen feature do spirit world,
# nao via biome modifier. Skip.

n_files = len([f for f in os.listdir(OUT_DIR) if f.endswith('.json')])
print(f"[OK] Gerados {n_files} biome modifier JSONs em:")
print(f"     {OUT_DIR}")
