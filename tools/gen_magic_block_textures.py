"""r112: Generate textures + models + blockstates pros blocos mágicos
que estão usando textura missing (purple/black checkerboard).
"""

from PIL import Image, ImageDraw
import os
import json

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX_BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets",
                         "liberthia", "textures", "block")
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "liberthia", "textures", "item")
MODEL_BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets",
                           "liberthia", "models", "block")
MODEL_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets",
                          "liberthia", "models", "item")
BLOCKSTATE = os.path.join(ROOT, "src", "main", "resources", "assets",
                          "liberthia", "blockstates")

for d in [TEX_BLOCK, TEX_ITEM, MODEL_BLOCK, MODEL_ITEM, BLOCKSTATE]:
    os.makedirs(d, exist_ok=True)

# School colors
SCHOOL_COLORS = {
    "fire":      ((255, 102, 51),  (160, 60, 20),  (255, 200, 50)),
    "ice":       ((102, 204, 255), (40, 120, 200), (200, 240, 255)),
    "lightning": ((255, 255, 68),  (200, 180, 30), (255, 255, 200)),
    "blood":     ((153, 0, 51),    (90, 0, 30),    (200, 30, 80)),
    "eldritch":  ((102, 51, 204),  (50, 20, 120),  (180, 100, 255)),
    "holy":      ((255, 238, 170), (220, 190, 80), (255, 255, 220)),
    "nature":    ((51, 170, 51),   (20, 110, 20),  (120, 230, 120)),
    "neutral":   ((140, 120, 100), (90, 70, 50),   (200, 180, 160)),
}

# (block_name, school, kind)
# kind: cube=cube_all, fire=flat-ish, jar=glass-like, wall=tile, torch=tall, ore=ore_pattern
BLOCKS = [
    ("magic_fire_fire", "fire", "fire"),
    ("magic_fire_ice", "ice", "fire"),
    ("magic_fire_lightning", "lightning", "fire"),
    ("magic_fire_blood", "blood", "fire"),
    ("magic_fire_eldritch", "eldritch", "fire"),
    ("magic_fire_holy", "holy", "fire"),
    ("magic_fire_nature", "nature", "fire"),
    ("magelight_torch", "holy", "torch"),
    ("mob_jar", "neutral", "jar"),
    ("potion_jar", "eldritch", "jar"),
    ("scryer_oculus", "eldritch", "cube"),
    ("repository", "neutral", "cube"),
    ("fire_wall", "fire", "wall"),
    ("ice_wall", "ice", "wall"),
    ("lightning_wall", "lightning", "wall"),
    ("holy_wall", "holy", "wall"),
    ("whirlwind", "lightning", "cube"),
    ("auto_miner", "neutral", "cube"),
    ("mage_cauldron", "eldritch", "cube"),
    ("inscription_table", "neutral", "cube"),
    ("scroll_forge", "fire", "cube"),
    ("spell_prism", "eldritch", "cube"),
    ("spell_turret", "fire", "cube"),
    ("spell_sensor", "eldritch", "cube"),
    ("ritual_brazier", "fire", "cube"),
    ("volcanic_sourcelink", "fire", "cube"),
    ("mycelial_sourcelink", "nature", "cube"),
    ("vitalic_sourcelink", "blood", "cube"),
    ("alchemical_sourcelink", "eldritch", "cube"),
    ("weave_basic", "eldritch", "cube"),
    ("weave_chest", "eldritch", "cube"),
    ("weave_holy", "holy", "cube"),
]


def draw_cube_pattern(d, light, dark, accent):
    """16x16 brick-ish pattern with school color."""
    d.rectangle([0, 0, 16, 16], fill=dark)
    # Brick pattern
    for y in range(0, 16, 4):
        offset = 2 if (y // 4) % 2 else 0
        for x in range(-offset, 16, 8):
            d.rectangle([x, y, x + 7, y + 3], fill=light)
    # Random accent points
    pts = [(2, 2), (13, 5), (5, 10), (10, 13), (8, 8)]
    for px, py in pts:
        d.point((px, py), fill=accent)


def draw_fire_pattern(d, light, dark, accent):
    """Flame-like swirl."""
    d.rectangle([0, 0, 16, 16], fill=(20, 10, 5))
    # Bottom hot core
    for y in range(8, 16):
        for x in range(0, 16):
            dist = ((x - 7.5) ** 2 + (y - 14) ** 2) ** 0.5
            if dist < 8:
                d.point((x, y), fill=dark)
    # Flame tongues
    flames = [(4, 6), (8, 4), (12, 6), (6, 8), (10, 8), (8, 2)]
    for fx, fy in flames:
        for dy in range(4):
            w = 2 - dy // 2
            for dx in range(-w, w + 1):
                if 0 <= fx + dx < 16 and 0 <= fy + dy < 16:
                    color = accent if dy < 2 else light
                    d.point((fx + dx, fy + dy), fill=color)


def draw_jar_pattern(d, light, dark, accent):
    """Glass jar transparent-ish."""
    d.rectangle([0, 0, 16, 16], fill=(0, 0, 0, 0))
    # Body
    d.rectangle([3, 4, 12, 14], fill=light + (180,) if len(light) == 3 else light)
    # Neck
    d.rectangle([5, 1, 10, 4], fill=dark)
    # Cap
    d.rectangle([4, 0, 11, 2], fill=accent)
    # Highlights
    d.line([(4, 5), (4, 13)], fill=accent)
    d.point((11, 6), fill=accent)


def draw_wall_pattern(d, light, dark, accent):
    """Tile wall pattern."""
    d.rectangle([0, 0, 16, 16], fill=dark)
    # Lattice
    for y in range(1, 16, 5):
        d.line([(0, y), (15, y)], fill=light)
    for x in range(1, 16, 5):
        d.line([(x, 0), (x, 15)], fill=light)
    # Center diamond
    d.point((7, 7), fill=accent)
    d.point((8, 7), fill=accent)
    d.point((7, 8), fill=accent)
    d.point((8, 8), fill=accent)


def draw_torch_pattern(d, light, dark, accent):
    """Vertical torch."""
    d.rectangle([0, 0, 16, 16], fill=(0, 0, 0, 0))
    # Stick
    d.rectangle([7, 8, 9, 16], fill=dark)
    # Flame
    d.rectangle([6, 5, 10, 8], fill=light)
    d.rectangle([7, 3, 9, 5], fill=accent)
    d.point((8, 1), fill=accent)


KIND_FNS = {
    "cube": draw_cube_pattern,
    "fire": draw_fire_pattern,
    "jar": draw_jar_pattern,
    "wall": draw_wall_pattern,
    "torch": draw_torch_pattern,
}


def gen_texture(name, school, kind):
    light, dark, accent = SCHOOL_COLORS[school]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0) if kind in ("jar", "torch") else light)
    d = ImageDraw.Draw(img)
    KIND_FNS.get(kind, draw_cube_pattern)(d, light, dark, accent)
    img.save(os.path.join(TEX_BLOCK, f"{name}.png"))


def gen_block_model(name, kind):
    """Generate models/block/{name}.json — cube_all most blocks."""
    if kind == "torch":
        data = {
            "parent": "minecraft:block/torch",
            "textures": {"torch": f"liberthia:block/{name}"}
        }
    elif kind == "fire":
        # Cross model (X pattern)
        data = {
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"liberthia:block/{name}"}
        }
    else:
        data = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"liberthia:block/{name}"}
        }
    with open(os.path.join(MODEL_BLOCK, f"{name}.json"), "w") as f:
        json.dump(data, f)


def gen_blockstate(name, kind):
    """Generate blockstates/{name}.json."""
    if kind == "torch":
        # Skip — torch uses vanilla wall_torch state
        data = {
            "variants": {
                "": {"model": f"liberthia:block/{name}"}
            }
        }
    else:
        data = {
            "variants": {
                "": {"model": f"liberthia:block/{name}"}
            }
        }
    with open(os.path.join(BLOCKSTATE, f"{name}.json"), "w") as f:
        json.dump(data, f)


def gen_item_model(name, kind):
    """Generate models/item/{name}.json that points to block model."""
    if kind in ("torch", "fire"):
        # Use generated 2D model from texture
        data = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"liberthia:block/{name}"}
        }
    else:
        data = {"parent": f"liberthia:block/{name}"}
    with open(os.path.join(MODEL_ITEM, f"{name}.json"), "w") as f:
        json.dump(data, f)


count = 0
for name, school, kind in BLOCKS:
    gen_texture(name, school, kind)
    gen_block_model(name, kind)
    gen_blockstate(name, kind)
    gen_item_model(name, kind)
    count += 1

print(f"Generated {count} block assets (texture+model+blockstate+itemmodel)")
