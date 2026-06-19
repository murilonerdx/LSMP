#!/usr/bin/env python3
"""r164: Adicionar TODOS os mobs custom (cosmic horror, wooden horrors, blood,
infectados, possessed) nas dimensões Spirit World + Loom + outras, com pesos
balanceados — uns mais raros, outros mais comuns.
"""
import json
from pathlib import Path

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
BIOME_DIR = ROOT / "data" / "liberthia" / "worldgen" / "biome"


def spawn(eid, weight, mn=1, mx=1):
    return {"type": f"liberthia:{eid}", "weight": weight, "minCount": mn, "maxCount": mx}


# ═══════════════════════════════════════════════════════════════════════
# Spirit World — main horror dim. Mix amplo de tudo.
# ═══════════════════════════════════════════════════════════════════════
SPIRIT_WORLD_MONSTERS = [
    # Loom horrors (most common) — they're the iconic spirit world residents
    spawn("loom_peripheral",   40, 1, 2),
    spawn("loom_screamer",     20),
    spawn("loom_watcher",      10),
    spawn("loom_worm",          8),
    # Cosmic horror (rare, scary)
    spawn("dark_consciousness", 8),
    spawn("empty_man",          4),
    spawn("observer",           3),     # MUITO raro — só "do nada"
    spawn("absence",            3),
    spawn("remembered",         3),
    # Wizards — balanced
    spawn("pyromancer",         6),
    spawn("cryomancer",         6),
    spawn("electromancer",      6),
    spawn("necromancer",        4),
    spawn("eldritch_cultist",   4),
    # Wooden horrors — 1-2 chance cada (8 variantes)
    spawn("wooden_charcoal",        4),
    spawn("wooden_pale_oak",        4),
    spawn("wooden_rotted_birch",    4),
    spawn("wooden_bleeding_maple",  3),
    spawn("wooden_mossy",           4),
    spawn("wooden_frozen_pine",     3),
    spawn("wooden_burning_acacia",  3),
    spawn("wooden_cursed_mahogany", 2),  # mais raro
    # Blood-infected (Spirit World tem influência sombria)
    spawn("blood_cultist",      6),
    spawn("blood_mage",         4),
    spawn("blood_priest",       2),  # raro (mini-boss)
    spawn("flesh_crawler",      8, 1, 2),
    spawn("gore_worm",          5),
    spawn("blood_worm",         4),
    # Possessed corpses
    spawn("possessed_zombie",   10),
    spawn("possessed_skeleton", 8),
    spawn("corrupted_zombie",   6),
    spawn("spore_spitter",      5),
    # Cosmic shadow
    spawn("disarmer",           3),
    spawn("weaving_shade",      3),
]
SPIRIT_WORLD_CREATURES = [
    spawn("wounded_pilgrim", 20),
    spawn("bookwyrm",         5),
    spawn("wisp_picker",      8),
    spawn("grove_sprite",     6),
    spawn("whelp",            3),
    spawn("carbuncle",        2),
    spawn("drygmy",           3),
]

# ═══════════════════════════════════════════════════════════════════════
# Loom Dimension — pure horror, no wizards/familiars. Dense + dark.
# ═══════════════════════════════════════════════════════════════════════
LOOM_MONSTERS = [
    spawn("loom_watcher",      60, 1, 2),
    spawn("loom_peripheral",   45, 1, 2),
    spawn("loom_screamer",     30),
    spawn("loom_worm",         20),
    spawn("dark_consciousness", 15),
    # Cosmic horror MUITO presente no Loom
    spawn("empty_man",         10),
    spawn("observer",           8),
    spawn("absence",            8),
    spawn("remembered",         6),
    # Few wooden horrors (Loom é mais "etéreo")
    spawn("wooden_pale_oak",    4),
    spawn("wooden_cursed_mahogany", 3),
    spawn("wooden_charcoal",    4),
    # Eldritch
    spawn("eldritch_cultist",   8),
    spawn("disarmer",           5),
    spawn("weaving_shade",      8),
]

# ═══════════════════════════════════════════════════════════════════════
# Wooden Place — WOODEN HORROR HEAVEN (all 8 variants)
# ═══════════════════════════════════════════════════════════════════════
WOODEN_PLACE_MONSTERS = [
    spawn("wooden_charcoal",        20, 1, 2),
    spawn("wooden_pale_oak",        20, 1, 2),
    spawn("wooden_rotted_birch",    18),
    spawn("wooden_bleeding_maple",  15),
    spawn("wooden_mossy",           18),
    spawn("wooden_frozen_pine",     15),
    spawn("wooden_burning_acacia",  15),
    spawn("wooden_cursed_mahogany", 10),
    spawn("dark_consciousness",      8),
    spawn("loom_peripheral",        15),
    spawn("loom_screamer",          10),
]

# ═══════════════════════════════════════════════════════════════════════
# Upside Sea — water + few horrors (mostly aquatic vibes)
# ═══════════════════════════════════════════════════════════════════════
UPSIDE_SEA_MONSTERS = [
    spawn("blood_worm",         12, 1, 2),
    spawn("gore_worm",          10),
    spawn("flesh_crawler",       8),
    spawn("loom_peripheral",     6),
    spawn("dark_consciousness",  4),
    spawn("observer",            2),  # raro mas existe
]

# ═══════════════════════════════════════════════════════════════════════
# Folded City — urban horror
# ═══════════════════════════════════════════════════════════════════════
FOLDED_CITY_MONSTERS = [
    spawn("possessed_zombie",   25, 1, 2),
    spawn("possessed_skeleton", 20),
    spawn("blood_cultist",      18),
    spawn("blood_mage",          8),
    spawn("loom_watcher",       12),
    spawn("empty_man",           6),
    spawn("observer",            4),  # cities = creepy
    spawn("absence",             5),
    spawn("remembered",          6),
    spawn("disarmer",            4),
    spawn("weaving_shade",       4),
    # Wizards podem aparecer em ruínas urbanas
    spawn("pyromancer",          4),
    spawn("cryomancer",          4),
    spawn("necromancer",         3),
    spawn("eldritch_cultist",    5),
]


def update_biome(name, monsters, creatures=None, spawn_prob=0.05):
    path = BIOME_DIR / f"{name}.json"
    if not path.exists():
        print(f"  SKIP {name} (no file)")
        return
    bio = json.loads(path.read_text(encoding="utf-8"))
    bio["spawners"]["monster"] = monsters
    if creatures is not None:
        bio["spawners"]["creature"] = creatures
    bio["creature_spawn_probability"] = spawn_prob
    path.write_text(json.dumps(bio, indent=2), encoding="utf-8")
    print(f"  OK {name}: {len(monsters)} monsters, "
          f"{len(creatures) if creatures else len(bio['spawners'].get('creature', []))} creatures")


def main():
    print("Updating biome spawn lists...")
    update_biome("spirit_world", SPIRIT_WORLD_MONSTERS, SPIRIT_WORLD_CREATURES, 0.05)
    update_biome("loom",         LOOM_MONSTERS,         [], 0.04)
    update_biome("wooden_place", WOODEN_PLACE_MONSTERS, [], 0.06)
    update_biome("upside_sea",   UPSIDE_SEA_MONSTERS,   [], 0.03)
    update_biome("folded_city",  FOLDED_CITY_MONSTERS,  [], 0.05)
    print("\nDONE — all biomes updated with comprehensive horror spawns.")


if __name__ == "__main__":
    main()
