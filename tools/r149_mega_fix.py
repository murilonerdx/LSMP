"""r149: MEGA-FIX em batch — lang entries, armor layers procedurais, block models faltando.

Gera:
1. Lang entries pra TODOS os items registrados mas sem nome humano
2. 14 armor layer PNGs procedurais (7 wizard schools x 2 layers)
3. Block models faltando pra magic fire + weaves + crops
"""
import json
import re
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(".")
LANG_EN = ROOT / "src/main/resources/assets/liberthia/lang/en_us.json"
LANG_PT = ROOT / "src/main/resources/assets/liberthia/lang/pt_br.json"
MODELS_DIR = ROOT / "src/main/resources/assets/liberthia/models"
BLOCKSTATES_DIR = ROOT / "src/main/resources/assets/liberthia/blockstates"
ARMOR_DIR = ROOT / "src/main/resources/assets/liberthia/textures/models/armor"
TEX_BLOCK_DIR = ROOT / "src/main/resources/assets/liberthia/textures/block"

# ════════════════════════════════════════════════════════════════════════
# Part 1: LANG ENTRIES
# ════════════════════════════════════════════════════════════════════════

def title_case(s):
    """ice_lance → Ice Lance, magic_fire_fire → Magic Fire (Fire)."""
    parts = s.split('_')
    # Special handling: magic_fire_X → Magic Fire (X)
    if len(parts) >= 3 and parts[0] == 'magic' and parts[1] == 'fire':
        suffix = ' '.join(p.capitalize() for p in parts[2:])
        return f"Magic Fire ({suffix})"
    # wizard_helm_fire → Wizard Helm (Fire)
    if len(parts) >= 3 and parts[0] == 'wizard':
        piece = parts[1].capitalize()
        school = ' '.join(p.capitalize() for p in parts[2:])
        return f"Wizard {piece} ({school})"
    return ' '.join(p.capitalize() for p in parts)

# Items que precisam de lang — lista do agent diagnose
MISSING_ITEMS = [
    # Sourcelinks
    "agronomic_sourcelink", "alchemical_sourcelink", "mycelial_sourcelink",
    "vitalic_sourcelink", "volcanic_sourcelink",
    # Blood blocks
    "attacking_flesh", "blood_altar", "blood_fountain", "blood_infection_block",
    "blood_infestation_block", "blood_volcano", "flesh_mother", "living_flesh",
    "infection_growth", "infection_heart", "infection_vein", "chalk_symbol",
    # Matter
    "clear_matter_block", "dark_matter_block", "yellow_matter_block",
    "containment_chamber", "corrupted_log", "corrupted_soil", "corrupted_stone",
    "dark_blood_test_item", "dark_matter_forge", "dark_matter_ore",
    "deepslate_dark_matter_ore", "matter_infuser", "matter_transmuter",
    "dm_infected_dirt", "dm_infected_grass", "dm_infected_sand", "dm_infected_stone",
    "wm_bleached_dirt", "wm_bleached_grass", "wm_bleached_sand", "wm_bleached_stone",
    "ym_unstable_dirt", "ym_unstable_grass", "ym_unstable_sand", "ym_unstable_stone",
    # Walls
    "fire_wall", "ice_wall", "holy_wall", "lightning_wall",
    # Auto blocks
    "auto_miner", "mage_cauldron", "whirlwind",
    "purification_bench", "purity_beacon", "quarantine_ward",
    # Magic fire
    "magelight_torch",
    "magic_fire_fire", "magic_fire_ice", "magic_fire_lightning",
    "magic_fire_blood", "magic_fire_eldritch", "magic_fire_holy", "magic_fire_nature",
    # Drygmy, factory
    "drygmy_egg", "factory_spell_scroll",
    # Tables / pedestals
    "inscription_table", "research_table", "ritual_brazier", "scroll_forge",
    "scryer_oculus", "spell_binding_pedestal", "spell_mirror",
    # Spell automation
    "spell_prism", "spell_sensor", "spell_turret", "spell_mutator",
    # Ores
    "soul_iron_ore", "spirit_crystal_ore", "sourcestone_ore_v2", "white_matter_ore",
    # Storage / utility
    "mob_jar", "potion_jar",
    # Crops
    "mage_bloom_crop", "source_berry_bush", "spore_bloom",
    # Misc
    "scarred_earth", "scarred_stone", "unstable_matter", "white_matter_tnt",
    "wormhole_block", "whisper_petal_bush",
    # Weaves
    "false_weave", "ghost_weave", "mirror_weave", "sky_weave",
    # Lay line
    "lay_line",
]

# Wizard armor (7 schools × 4 pieces = 28)
SCHOOLS = ["fire", "ice", "lightning", "blood", "eldritch", "holy", "nature"]
for school in SCHOOLS:
    for piece in ["helm", "chest", "legs", "boots"]:
        MISSING_ITEMS.append(f"wizard_{piece}_{school}")

def update_lang_file(path, items):
    if not path.exists():
        print(f"  ! {path} not found")
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    added = 0
    for item in items:
        # Try as block first
        block_key = f"block.liberthia.{item}"
        item_key = f"item.liberthia.{item}"
        # Prefer item key for items, block key for blocks
        # Most items are blocks here, but doesn't hurt to add both
        if block_key not in data:
            data[block_key] = title_case(item)
            added += 1
        if item_key not in data:
            data[item_key] = title_case(item)
            added += 1
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"  + {path.name}: added {added} entries")

print("=== Part 1: LANG ===")
update_lang_file(LANG_EN, MISSING_ITEMS)
update_lang_file(LANG_PT, MISSING_ITEMS)

# ════════════════════════════════════════════════════════════════════════
# Part 2: ARMOR LAYERS (procedural PNG)
# ════════════════════════════════════════════════════════════════════════

print("\n=== Part 2: WIZARD ARMOR LAYERS ===")
ARMOR_DIR.mkdir(parents=True, exist_ok=True)

# Cores temáticas por escola
SCHOOL_COLORS = {
    "fire":      ((255, 85, 0),   (180, 30, 0),   (255, 200, 50)),    # primary/dark/highlight
    "ice":       ((150, 220, 255),(60, 130, 200), (240, 250, 255)),
    "lightning": ((255, 255, 100),(220, 180, 50), (255, 255, 220)),
    "blood":     ((180, 30, 30),  (90, 10, 10),   (255, 80, 80)),
    "eldritch":  ((150, 60, 220), (80, 20, 130),  (220, 150, 255)),
    "holy":      ((255, 230, 130),(200, 150, 50), (255, 255, 220)),
    "nature":    ((80, 180, 60),  (40, 100, 30),  (180, 240, 100)),
}

def make_armor_layer(school, layer_num):
    """Gera textura de armor layer 64x32 (vanilla armor format)."""
    primary, dark, highlight = SCHOOL_COLORS[school]
    W, H = 64, 32
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Layer 1: helmet (UV 0,0-32,16) + chestplate (UV 16,16-40,32) + boots (UV 0,16-16,32)
    # Layer 2: leggings (UV 0,0-32,16) + boots (UV 0,16-16,32)
    # All overlapping in single image — paint with school color

    # Fill all opaque regions with primary
    if layer_num == 1:
        # Helmet area: 0,0 to 32,16
        d.rectangle([0, 0, 31, 15], fill=primary)
        # Chestplate area: 16,16 to 40,32
        d.rectangle([16, 16, 39, 31], fill=primary)
        # Right arm: 40,16 to 56,32
        d.rectangle([40, 16, 55, 31], fill=primary)
        # Left arm: 32,48 sair-> mas é layer 1, fica em 0,16-16,32 (boots area)
        d.rectangle([0, 16, 15, 31], fill=primary)
    else:
        # Layer 2 = leggings (chestplate area + legs)
        d.rectangle([0, 0, 16, 16], fill=primary)
        d.rectangle([16, 16, 40, 32], fill=primary)

    # Add detail/pattern — diagonal stripe + spots of highlight
    import random
    random.seed(hash(school + str(layer_num)))
    for _ in range(40):
        x = random.randint(0, W - 1)
        y = random.randint(0, H - 1)
        p = img.getpixel((x, y))
        if p[3] > 0:  # has alpha (not transparent)
            img.putpixel((x, y), dark if random.random() < 0.6 else highlight)

    # Add school sigil mark on chest (center of chestplate area)
    if layer_num == 1:
        cx, cy = 28, 23
        d.rectangle([cx - 2, cy - 2, cx + 2, cy + 2], fill=highlight)
        img.putpixel((cx, cy), (255, 255, 255, 255))

    return img

for school in SCHOOLS:
    for layer in [1, 2]:
        path = ARMOR_DIR / f"wizard_{school}_layer_{layer}.png"
        img = make_armor_layer(school, layer)
        img.save(path)
        print(f"  + {path.name}")

# ════════════════════════════════════════════════════════════════════════
# Part 3: MISSING BLOCK MODELS (weaves, crops, lay_line)
# ════════════════════════════════════════════════════════════════════════

print("\n=== Part 3: MISSING BLOCK MODELS ===")
MISSING_BLOCKS = {
    "false_weave":   {"color": "#9966cc", "transparent": True},
    "ghost_weave":   {"color": "#ccccff", "transparent": True},
    "mirror_weave":  {"color": "#aaeeff", "transparent": True},
    "sky_weave":     {"color": "#88ccff", "transparent": True},
    "lay_line":      {"color": "#ddaaff", "transparent": True},
    "mage_bloom_crop":   {"color": "#ff66cc", "transparent": False},
    "source_berry_bush": {"color": "#9966ff", "transparent": False},
    "spell_mutator":     {"color": "#aa88ff", "transparent": False},
}

def hex_to_rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)

def make_block_texture(name, color_hex, transparent=False):
    """Procedural 16x16 — solid color + noise."""
    base = hex_to_rgb(color_hex)
    img = Image.new("RGBA", (16, 16), (0,0,0,0) if transparent else base)
    if transparent:
        # Center pattern (X shape)
        d = ImageDraw.Draw(img)
        for i in range(16):
            img.putpixel((i, i), base)
            img.putpixel((15 - i, i), base)
        # Center square brighter
        bright = tuple(min(255, c + 50) if i < 3 else c for i, c in enumerate(base))
        d.rectangle([6, 6, 9, 9], fill=bright)
    else:
        import random
        random.seed(hash(name))
        dark = tuple(max(0, c - 40) if i < 3 else c for i, c in enumerate(base))
        light = tuple(min(255, c + 30) if i < 3 else c for i, c in enumerate(base))
        for y in range(16):
            for x in range(16):
                r = random.random()
                if r < 0.2:
                    img.putpixel((x, y), dark)
                elif r > 0.8:
                    img.putpixel((x, y), light)
    return img

(MODELS_DIR / "block").mkdir(parents=True, exist_ok=True)
(MODELS_DIR / "item").mkdir(parents=True, exist_ok=True)
BLOCKSTATES_DIR.mkdir(parents=True, exist_ok=True)
TEX_BLOCK_DIR.mkdir(parents=True, exist_ok=True)

for name, spec in MISSING_BLOCKS.items():
    # 1. Texture PNG
    tex_path = TEX_BLOCK_DIR / f"{name}.png"
    if not tex_path.exists():
        make_block_texture(name, spec["color"], spec.get("transparent", False)).save(tex_path)
        print(f"  + texture/block/{name}.png")

    # 2. Block model JSON
    model_path = MODELS_DIR / "block" / f"{name}.json"
    if not model_path.exists():
        if spec.get("transparent"):
            # Cross-shape (plant-like)
            model = {
                "parent": "minecraft:block/cross",
                "textures": {"cross": f"liberthia:block/{name}"}
            }
        else:
            model = {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"liberthia:block/{name}"}
            }
        model_path.write_text(json.dumps(model, indent=2), encoding="utf-8")
        print(f"  + models/block/{name}.json")

    # 3. Blockstate JSON
    bs_path = BLOCKSTATES_DIR / f"{name}.json"
    if not bs_path.exists():
        bs = {"variants": {"": {"model": f"liberthia:block/{name}"}}}
        bs_path.write_text(json.dumps(bs, indent=2), encoding="utf-8")
        print(f"  + blockstates/{name}.json")

    # 4. Item model (refs block model)
    item_path = MODELS_DIR / "item" / f"{name}.json"
    if not item_path.exists():
        item = {"parent": f"liberthia:block/{name}"}
        item_path.write_text(json.dumps(item, indent=2), encoding="utf-8")
        print(f"  + models/item/{name}.json")

print("\n✅ DONE")
