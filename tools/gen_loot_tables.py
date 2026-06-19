"""r132: gera loot tables para entities sem drops.

Tematica por categoria:
- Cosmic horror: drops insanity items
- Blood mobs: blood vials, fragments
- Wizard mobs: scrolls, source items
- Familiars: raras essences
- Bosses: drops unicos exclusivos
- Projectiles/orbs: SKIP
"""
import os, json

ROOT = os.path.dirname(__file__)
OUT_DIR = os.path.normpath(os.path.join(ROOT, "..",
    "src/main/resources/data/liberthia/loot_tables/entities"))
os.makedirs(OUT_DIR, exist_ok=True)


def loot(name, pools):
    """Cria 1 loot table JSON com lista de pools."""
    j = {
        "type": "minecraft:entity",
        "pools": pools
    }
    with open(os.path.join(OUT_DIR, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


def item_pool(item_id, min_count=1, max_count=1, chance=1.0, rolls=1):
    """Pool com 1 item, optional chance + count range."""
    entry = {
        "type": "minecraft:item",
        "name": item_id
    }
    funcs = []
    if max_count > 1 or min_count != 1:
        funcs.append({
            "function": "minecraft:set_count",
            "count": {"min": min_count, "max": max_count}
        })
    if funcs:
        entry["functions"] = funcs

    pool = {
        "rolls": rolls,
        "entries": [entry]
    }
    if chance < 1.0:
        pool["conditions"] = [{
            "condition": "minecraft:random_chance",
            "chance": chance
        }]
    return pool


def multi_pool(items_with_weights, rolls=1):
    """Pool com lista de items ponderados (weighted random)."""
    entries = []
    for item_id, weight in items_with_weights:
        entries.append({
            "type": "minecraft:item",
            "name": item_id,
            "weight": weight
        })
    return {"rolls": rolls, "entries": entries}


# SKIPS: projectiles/orbs/entities tecnicas (no drop)
SKIPS = {
    'bleeding_arrow', 'blood_orb', 'blood_pearl', 'burning_gem',
    'frost_flask', 'hemo_bolt', 'lightning_grenade', 'mind_splinter_dart',
    'observation_projectile', 'purifying_flask', 'spell_projectile',
    'white_matter_explosion', 'mini_black_hole', 'black_hole',
    'clone_player', 'reflection_entity', 'soul_body',
    'veiling_orb',  # orbital
}

# === COSMIC HORROR mobs ===
loot("empty_man", [
    item_pool("liberthia:phantom_ink", 1, 3, chance=0.8),
    item_pool("liberthia:ectoplasm_strand", 1, 1, chance=0.3),
])
loot("observer", [
    item_pool("liberthia:memory_shard", 1, 2, chance=0.7),
    item_pool("minecraft:ender_pearl", 0, 1, chance=0.2),
])
loot("absence", [
    item_pool("liberthia:ectoplasm_strand", 1, 3, chance=0.9),
    item_pool("liberthia:veil_fragment", 0, 1, chance=0.15),
])
loot("remembered", [
    item_pool("liberthia:phantom_ink", 1, 2, chance=0.7),
    item_pool("liberthia:memory_shard", 0, 1, chance=0.3),
])
loot("eldritch_cultist", [
    item_pool("minecraft:black_dye", 1, 2),
    item_pool("liberthia:ectoplasm_strand", 0, 1, chance=0.2),
])
loot("eye_of_horus", [
    item_pool("liberthia:phantom_ink", 1, 1, chance=0.5),
    item_pool("minecraft:ender_eye", 0, 1, chance=0.1),
])
loot("dark_consciousness", [
    item_pool("liberthia:ectoplasm_strand", 2, 4),
    item_pool("liberthia:veil_fragment", 1, 1, chance=0.4),
])

# === BLOOD mobs ===
loot("blood_hound", [
    item_pool("minecraft:rotten_flesh", 1, 2),
    item_pool("minecraft:redstone", 0, 1, chance=0.3),
])
loot("blood_cultist", [
    item_pool("minecraft:redstone", 1, 2),
    item_pool("minecraft:rotten_flesh", 0, 1, chance=0.4),
])
loot("blood_priest", [
    item_pool("minecraft:redstone", 2, 4),
    multi_pool([
        ("minecraft:gold_nugget", 5),
        ("minecraft:emerald", 1),
    ]),
])
loot("blood_mage", [
    item_pool("minecraft:redstone", 1, 3),
    item_pool("minecraft:book", 0, 1, chance=0.3),
])
loot("blood_worm", [
    item_pool("minecraft:string", 0, 1, chance=0.6),
])
loot("flesh_crawler", [
    item_pool("minecraft:rotten_flesh", 1, 3),
    item_pool("minecraft:bone", 0, 2, chance=0.4),
])
loot("gore_worm", [
    item_pool("minecraft:rotten_flesh", 1, 2),
    item_pool("minecraft:string", 0, 1, chance=0.4),
])
loot("wounded_pilgrim", [
    item_pool("minecraft:rotten_flesh", 1, 2),
    item_pool("minecraft:bone", 0, 1, chance=0.3),
])
loot("possessed_zombie", [
    item_pool("minecraft:rotten_flesh", 1, 2),
    item_pool("minecraft:iron_ingot", 0, 1, chance=0.15),
])
loot("possessed_skeleton", [
    item_pool("minecraft:bone", 1, 2),
    item_pool("minecraft:arrow", 0, 2, chance=0.5),
])

# === WIZARD mobs (drop scrolls + source items) ===
loot("apothecarist", [
    item_pool("minecraft:potion", 0, 1, chance=0.3),
    multi_pool([
        ("liberthia:wisp_essence", 4),
        ("liberthia:astral_dust", 3),
    ], rolls=1),
])
loot("keeper", [
    item_pool("minecraft:book", 0, 1, chance=0.4),
    item_pool("liberthia:memory_shard", 0, 1, chance=0.3),
])
loot("archevoker", [
    item_pool("minecraft:emerald", 1, 2),
    item_pool("liberthia:phantom_ink", 0, 1, chance=0.4),
])
loot("cryomancer", [
    item_pool("minecraft:ice", 1, 2),
    item_pool("liberthia:spell_frostbolt", 0, 1, chance=0.1),
])
loot("pyromancer", [
    item_pool("minecraft:blaze_powder", 1, 2),
    item_pool("liberthia:spell_fireball", 0, 1, chance=0.1),
])
loot("electromancer", [
    item_pool("minecraft:copper_ingot", 1, 2),
    item_pool("liberthia:spell_lightning_bolt", 0, 1, chance=0.1),
])
loot("necromancer", [
    item_pool("minecraft:bone", 2, 4),
    item_pool("minecraft:soul_sand", 0, 1, chance=0.3),
])
loot("order_paladin", [
    item_pool("minecraft:gold_ingot", 1, 2),
    item_pool("minecraft:emerald", 0, 1, chance=0.4),
])

# === FAMILIARS (drops raros) ===
loot("carbuncle", [
    item_pool("minecraft:amethyst_shard", 1, 3, chance=0.7),
    multi_pool([("minecraft:emerald", 3), ("minecraft:diamond", 1)], rolls=1),
])
loot("amethyst_golem", [
    item_pool("minecraft:amethyst_shard", 2, 5),
    item_pool("minecraft:amethyst_block", 0, 1, chance=0.3),
])
loot("whelp", [
    item_pool("liberthia:astral_dust", 1, 2, chance=0.6),
])
loot("bookwyrm", [
    item_pool("minecraft:book", 0, 1, chance=0.4),
    item_pool("minecraft:paper", 1, 3),
])
loot("grove_sprite", [
    item_pool("minecraft:sapling", 0, 2, chance=0.5),
    item_pool("minecraft:bone_meal", 1, 2),
])
loot("wisp_picker", [
    item_pool("liberthia:wisp_essence", 1, 2, chance=0.8),
])

# === DARK MATTER / CORRUPTION mobs ===
loot("corrupted_zombie", [
    item_pool("liberthia:dark_matter_shard", 0, 1, chance=0.4),
    item_pool("minecraft:rotten_flesh", 1, 2),
])
loot("dark_matter_spore", [
    item_pool("liberthia:dark_matter_shard", 0, 1, chance=0.5),
])
loot("spore_spitter", [
    item_pool("liberthia:dark_matter_shard", 0, 1, chance=0.3),
    item_pool("minecraft:slime_ball", 0, 1, chance=0.4),
])

# === DISARMER / OUTROS ===
loot("disarmer", [
    item_pool("minecraft:iron_ingot", 0, 1, chance=0.3),
    item_pool("minecraft:redstone", 1, 2),
])
loot("frozen_humanoid", [
    item_pool("minecraft:ice", 1, 2),
    item_pool("liberthia:clear_matter_shard", 0, 1, chance=0.3),
])
loot("weaving_shade", [
    item_pool("minecraft:string", 1, 3),
    item_pool("liberthia:ectoplasm_strand", 0, 1, chance=0.4),
])
loot("soul_reaper", [
    item_pool("minecraft:bone", 1, 3),
    item_pool("liberthia:ectoplasm_strand", 0, 1, chance=0.5),
])

# === LICH BOSSES + minions ===
loot("abyssal_lich", [
    item_pool("liberthia:netherite_seal", 1, 1),
    item_pool("liberthia:diamond_seal", 1, 1),
    item_pool("minecraft:diamond", 3, 8),
    item_pool("liberthia:veil_fragment", 2, 4),
    item_pool("liberthia:spell_eldritch_meteor", 0, 1, chance=0.5),
])
loot("lich_stalker", [
    item_pool("minecraft:bone", 1, 3),
    item_pool("liberthia:phantom_ink", 0, 1, chance=0.4),
])
loot("lich_hunter", [
    item_pool("minecraft:bone", 2, 4),
    item_pool("liberthia:ectoplasm_strand", 0, 1, chance=0.5),
])

# === LOOM mobs ===
loot("loom_watcher", [
    item_pool("liberthia:veil_fragment", 0, 1, chance=0.4),
    item_pool("liberthia:phantom_ink", 1, 2, chance=0.6),
])
loot("loom_peripheral", [
    item_pool("liberthia:ectoplasm_strand", 1, 2, chance=0.6),
])
loot("loom_screamer", [
    item_pool("minecraft:bone", 0, 2, chance=0.5),
    item_pool("liberthia:phantom_ink", 0, 1, chance=0.3),
])
loot("loom_worm", [
    item_pool("minecraft:string", 1, 2),
    item_pool("liberthia:veil_fragment", 0, 1, chance=0.2),
])

# === FLESH MOTHER BOSS ===
loot("flesh_mother_boss", [
    item_pool("liberthia:bone_seal", 1, 1),
    item_pool("liberthia:diamond_seal", 0, 1, chance=0.5),
    item_pool("minecraft:rotten_flesh", 16, 32),
    item_pool("minecraft:bone", 8, 16),
    item_pool("liberthia:dark_matter_shard", 2, 5),
])
loot("blood_warden", [
    item_pool("liberthia:gold_seal", 1, 1),
    item_pool("liberthia:diamond_seal", 0, 1, chance=0.5),
    item_pool("minecraft:redstone", 8, 16),
    item_pool("liberthia:dark_matter_shard", 1, 3),
])

# === VOID LARVA (drop infection items) ===
loot("void_larva", [
    item_pool("liberthia:phantom_ink", 0, 1, chance=0.3),
    item_pool("liberthia:void_reagent", 0, 1, chance=0.1),
])

n = len([f for f in os.listdir(OUT_DIR) if f.endswith('.json')])
print(f"[OK] Gerados {n} loot tables em data/liberthia/loot_tables/entities/")
