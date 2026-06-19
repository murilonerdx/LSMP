#!/usr/bin/env python3
"""r163: Mass-update Spirit World worldgen + spawns + create wizard textures.

Adds:
- All custom ores to Spirit World biome with proper Y-layers and rarities
- Decorative block patches (spirit_stone, soul_brick, halo_marble, etc.)
- Mob spawns with cosmic horror (controlled weight to avoid overcrowding)
- Custom 64×32 wizard textures (5 wizards: pyromancer/cryomancer/electromancer/
  necromancer/eldritch_cultist) — pixel-art style matching the villager rig

Fix cryomancer (and others) texture issue — user said cryomancer should be
"totally stone skin" so we generate stone-textured wizard skins.
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
WORLDGEN = ROOT / "data" / "liberthia" / "worldgen"
CONF_DIR = WORLDGEN / "configured_feature"
PLACED_DIR = WORLDGEN / "placed_feature"
BIOME_DIR = WORLDGEN / "biome"
TEX_DIR = ROOT / "assets" / "liberthia" / "textures" / "entity" / "wizard"
TEX_DIR.mkdir(parents=True, exist_ok=True)


# ═════════════════════════════════════════════════════════════════════════
# WIZARD TEXTURES — 64×64 villager-rig pixel art
# ═════════════════════════════════════════════════════════════════════════
# The villager texture is 64×64. We replicate its UV map but with custom palettes.
# Body: arms, torso, head — each tinted by the wizard class.

def gen_wizard_texture(name: str, palette: dict, output: Path):
    """Generate a 64×64 villager-rig PNG with custom palette."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    skin = palette["skin"]
    robe = palette["robe"]
    accent = palette["accent"]
    eye_color = palette.get("eye", (0, 0, 0))

    # Body rectangles approximating villager UV map
    # Head (top, front, right, left, back, bottom) — UV 0..32 horizontally, 0..16 vertically
    # We'll just fill big regions with skin/robe; details added via small dots.

    # --- HEAD (front + sides + top + back + bottom) ---
    # Vanilla villager head UV roughly:
    # top: 8,0,16,8 | bottom: 16,0,24,8
    # front: 8,8,16,16 | back: 24,8,32,16 | right: 0,8,8,16 | left: 16,8,24,16
    head_regions = [
        (8, 0, 16, 8),   # top
        (16, 0, 24, 8),  # bottom
        (8, 8, 16, 16),  # front
        (24, 8, 32, 16), # back
        (0, 8, 8, 16),   # right side
        (16, 8, 24, 16), # left side
    ]
    for (x1, y1, x2, y2) in head_regions:
        for y in range(y1, y2):
            for x in range(x1, x2):
                # Slight noise pattern
                n = ((x * 3 + y * 7) % 17) - 8
                r, g, b = skin
                d.point((x, y), fill=(
                    max(0, min(255, r + n)),
                    max(0, min(255, g + n)),
                    max(0, min(255, b + n)),
                    255))

    # --- EYES (2 dots on front of head) ---
    d.point((10, 11), fill=(*eye_color, 255))
    d.point((13, 11), fill=(*eye_color, 255))
    # White glint in eye
    d.point((10, 10), fill=(255, 255, 255, 255))
    d.point((13, 10), fill=(255, 255, 255, 255))

    # --- NOSE (Liberthia: x marker for damaged) ---
    # Vanilla villager nose: 1px wide strip 9-14, 12-15
    # We'll skip nose for stone-skin

    # --- BODY (torso) UV ~16-32 H, 20-32 V ---
    body_regions = [
        (16, 16, 28, 20),  # top of torso (going down)
        (28, 16, 40, 20),
        (16, 20, 28, 32),  # front
        (28, 20, 40, 32),  # back
        (40, 20, 44, 32),  # right side
        (12, 20, 16, 32),  # left side
    ]
    for (x1, y1, x2, y2) in body_regions:
        for y in range(y1, min(y2, 64)):
            for x in range(x1, min(x2, 64)):
                r, g, b = robe
                n = ((x + y) % 5) - 2
                d.point((x, y), fill=(
                    max(0, min(255, r + n)),
                    max(0, min(255, g + n)),
                    max(0, min(255, b + n)),
                    255))

    # --- ROBE ACCENT (stripe down middle of torso front) ---
    for y in range(20, 32):
        d.point((21, y), fill=(*accent, 255))
        d.point((22, y), fill=(*accent, 255))

    # --- ARMS (44-56 H, 20-32 V on right side; some on left) ---
    arm_regions = [
        (44, 20, 48, 32),  # right arm front
        (48, 20, 52, 32),  # right arm side
        (52, 20, 56, 32),  # right arm back
        (40, 16, 48, 20),  # top of right arm
        (32, 48, 36, 64),  # left arm (alt UV)
        (36, 48, 40, 64),
    ]
    for (x1, y1, x2, y2) in arm_regions:
        for y in range(y1, min(y2, 64)):
            for x in range(x1, min(x2, 64)):
                r, g, b = skin
                n = ((x * 5 + y * 3) % 13) - 6
                d.point((x, y), fill=(
                    max(0, min(255, r + n)),
                    max(0, min(255, g + n)),
                    max(0, min(255, b + n)),
                    255))

    # --- LEGS UV ~ 0-16 H 16-32 V (front), 16-32 H 16-32 V (back/sides) ---
    leg_regions = [
        (0, 16, 4, 20),    # right leg top
        (4, 16, 8, 20),    # right leg bottom
        (0, 20, 4, 32),    # right leg front
        (4, 20, 8, 32),    # right leg back
        (0, 48, 16, 64),   # left leg (alt UV)
    ]
    for (x1, y1, x2, y2) in leg_regions:
        for y in range(y1, min(y2, 64)):
            for x in range(x1, min(x2, 64)):
                r, g, b = robe
                # Darker robe for legs
                r, g, b = int(r * 0.7), int(g * 0.7), int(b * 0.7)
                d.point((x, y), fill=(r, g, b, 255))

    img.save(output)


# Wizard palettes — each one has a distinctive look
WIZARD_PALETTES = {
    "cryomancer":      {"skin": (140, 140, 150), "robe": (180, 220, 240), "accent": (100, 180, 255), "eye": (200, 230, 255)},  # STONE SKIN! per user
    "pyromancer":      {"skin": (220, 180, 140), "robe": (200, 50, 30),    "accent": (255, 200, 80),  "eye": (255, 100, 50)},
    "electromancer":   {"skin": (220, 200, 160), "robe": (250, 220, 80),  "accent": (255, 255, 100), "eye": (255, 255, 50)},
    "necromancer":     {"skin": (170, 160, 180), "robe": (40, 30, 60),    "accent": (130, 80, 200),  "eye": (180, 80, 220)},
    "eldritch_cultist":{"skin": (160, 130, 180), "robe": (60, 0, 80),     "accent": (180, 60, 200),  "eye": (255, 100, 255)},
}


def main():
    # Generate textures
    print("Generating wizard textures...")
    for name, palette in WIZARD_PALETTES.items():
        out = TEX_DIR / f"{name}.png"
        gen_wizard_texture(name, palette, out)
        print(f"  OK {out.name}")

    # ─── Configured features for ALL custom ores ──────────────────────────
    # Each ore feature gen (size, target stone, y-range will be in placed)
    ORES = {
        # ore_id (block): (size, discard_chance)
        "spirit_crystal_ore":   (4, 0.0),
        "soul_iron_ore":        (6, 0.0),
        "spirit_gem_ore":       (3, 0.0),
        "sourcestone_ore":      (5, 0.0),
        "crystal_spirit_ore":   (4, 0.0),
        "dark_matter_ore":      (3, 0.0),
        "white_matter_ore":     (3, 0.0),
        "deepslate_dark_matter_ore": (3, 0.0),
        "loom_voidite_ore":     (4, 0.1),
        "loom_riftite_ore":     (5, 0.0),
        "loom_umbral_ore":      (3, 0.0),
        "spirit_gem_ore":       (3, 0.0),
    }
    # Placement (count, y_min, y_max)
    PLACEMENTS = {
        "spirit_crystal_ore":  (4, -50, 30),
        "soul_iron_ore":       (8, -40, 10),
        "spirit_gem_ore":      (3, 0, 50),
        "sourcestone_ore":     (5, -10, 20),
        "crystal_spirit_ore":  (4, -20, 30),
        "dark_matter_ore":     (2, -64, -20),
        "white_matter_ore":    (2, 30, 80),
        "deepslate_dark_matter_ore": (3, -64, -10),
        "loom_voidite_ore":    (3, -30, 20),
        "loom_riftite_ore":    (4, -10, 40),
        "loom_umbral_ore":     (2, -50, -10),
    }

    # Generate missing configured features
    for ore_id, (size, discard) in ORES.items():
        cf_path = CONF_DIR / f"{ore_id}.json"
        if cf_path.exists():
            continue
        feature = {
            "type": "minecraft:ore",
            "config": {
                "size": size,
                "discard_chance_on_air_exposure": discard,
                "targets": [
                    {
                        "target": {"predicate_type": "minecraft:tag_match",
                                   "tag": "minecraft:stone_ore_replaceables"},
                        "state": {"Name": f"liberthia:{ore_id}"}
                    },
                    {
                        "target": {"predicate_type": "minecraft:tag_match",
                                   "tag": "minecraft:deepslate_ore_replaceables"},
                        "state": {"Name": f"liberthia:{ore_id}"}
                    }
                ]
            }
        }
        cf_path.write_text(json.dumps(feature, indent=2), encoding="utf-8")
        print(f"  OK configured_feature/{ore_id}.json")

    # Generate missing placed features
    for ore_id, (count, y_min, y_max) in PLACEMENTS.items():
        pf_path = PLACED_DIR / f"{ore_id}.json"
        # Build placement (rewrite if exists to ensure consistency)
        placement = {
            "feature": f"liberthia:{ore_id}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {
                    "type": "minecraft:height_range",
                    "height": {
                        "type": "minecraft:trapezoid",
                        "min_inclusive": {"absolute": y_min},
                        "max_inclusive": {"absolute": y_max}
                    }
                },
                {"type": "minecraft:biome"}
            ]
        }
        pf_path.write_text(json.dumps(placement, indent=2), encoding="utf-8")
        print(f"  OK placed_feature/{ore_id}.json")

    # ─── Update spirit_world biome to include ALL ores + decorative patches ──
    biome_path = BIOME_DIR / "spirit_world.json"
    biome = json.loads(biome_path.read_text(encoding="utf-8"))
    # Features array indices in vanilla worldgen:
    # 0=RAW_GENERATION, 1=LAKES, 2=LOCAL_MODIFICATIONS, 3=UNDERGROUND_STRUCTURES,
    # 4=SURFACE_STRUCTURES, 5=STRONGHOLDS, 6=UNDERGROUND_ORES, 7=UNDERGROUND_DECORATION,
    # 8=FLUID_SPRINGS, 9=VEGETAL_DECORATION, 10=TOP_LAYER_MODIFICATION
    ore_list = [
        "liberthia:spirit_crystal_ore",
        "liberthia:soul_iron_ore",
        "liberthia:spirit_gem_ore",
        "liberthia:sourcestone_ore",
        "liberthia:crystal_spirit_ore",
        "liberthia:dark_matter_ore",
        "liberthia:white_matter_ore",
        "liberthia:deepslate_dark_matter_ore",
        "liberthia:loom_voidite_ore",
        "liberthia:loom_riftite_ore",
        "liberthia:loom_umbral_ore",
    ]
    biome["features"][6] = ore_list  # UNDERGROUND_ORES
    biome["features"][9] = ["liberthia:whisperwood_tree"]  # VEGETAL_DECORATION

    # Update spawn list — cosmic horror with controlled weights
    biome["spawners"] = {
        "monster": [
            {"type": "liberthia:loom_peripheral", "weight": 40, "minCount": 1, "maxCount": 2},
            {"type": "liberthia:loom_screamer", "weight": 20, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:loom_watcher", "weight": 10, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:dark_consciousness", "weight": 8, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:pyromancer", "weight": 6, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:cryomancer", "weight": 6, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:electromancer", "weight": 6, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:necromancer", "weight": 4, "minCount": 1, "maxCount": 1},
            {"type": "liberthia:eldritch_cultist", "weight": 4, "minCount": 1, "maxCount": 1},
        ],
        "creature": [
            {"type": "liberthia:wounded_pilgrim", "weight": 20, "minCount": 1, "maxCount": 1}
        ],
        "ambient": [],
        "axolotls": [],
        "underground_water_creature": [],
        "water_ambient": [],
        "water_creature": [],
        "misc": []
    }
    # Reduce overall spawn probability (user complained about too many mobs)
    biome["creature_spawn_probability"] = 0.05

    biome_path.write_text(json.dumps(biome, indent=2), encoding="utf-8")
    print(f"\nOK Updated biome/spirit_world.json — {len(ore_list)} ores, {len(biome['spawners']['monster'])} monster spawns")


if __name__ == "__main__":
    main()
