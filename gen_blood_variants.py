"""Generate blockstates, models, item models, loot tables, and textures
for the blood-themed terrain/ore variants.

Blocks: blood_dirt, blood_sand, blood_stone,
        blood_coal_ore, blood_iron_ore, blood_gold_ore, blood_diamond_ore,
        blood_redstone_ore, blood_lapis_ore, blood_emerald_ore.
"""
import json, os, random
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
BLOCKSTATES = os.path.join(ROOT, "src/main/resources/assets/liberthia/blockstates")
MODELS_BLOCK = os.path.join(ROOT, "src/main/resources/assets/liberthia/models/block")
MODELS_ITEM = os.path.join(ROOT, "src/main/resources/assets/liberthia/models/item")
TEXTURES = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/block")
LOOT = os.path.join(ROOT, "src/main/resources/data/liberthia/loot_tables/blocks")

for d in (BLOCKSTATES, MODELS_BLOCK, MODELS_ITEM, TEXTURES, LOOT):
    os.makedirs(d, exist_ok=True)


def write_json(path, obj):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def simple_block(name):
    write_json(os.path.join(BLOCKSTATES, f"{name}.json"),
               {"variants": {"": {"model": f"liberthia:block/{name}"}}})
    write_json(os.path.join(MODELS_BLOCK, f"{name}.json"),
               {"parent": "minecraft:block/cube_all",
                "textures": {"all": f"liberthia:block/{name}"}})
    write_json(os.path.join(MODELS_ITEM, f"{name}.json"),
               {"parent": f"liberthia:block/{name}"})
    write_json(os.path.join(LOOT, f"{name}.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"liberthia:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]
        }]
    })


def noise_base(size, base, variance, veins_color, vein_count, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), base + (255,))
    px = img.load()
    # speckle
    for y in range(size):
        for x in range(size):
            v = rng.randint(-variance, variance)
            r = max(0, min(255, base[0] + v))
            g = max(0, min(255, base[1] + v // 2))
            b = max(0, min(255, base[2] + v // 2))
            px[x, y] = (r, g, b, 255)
    # draw blood veins
    d = ImageDraw.Draw(img)
    for _ in range(vein_count):
        x0 = rng.randint(0, size)
        y0 = rng.randint(0, size)
        for _ in range(rng.randint(3, 6)):
            x1 = x0 + rng.randint(-4, 4)
            y1 = y0 + rng.randint(-4, 4)
            d.line([(x0, y0), (x1, y1)], fill=veins_color + (255,), width=1)
            x0, y0 = x1, y1
    # slight blur
    return img.filter(ImageFilter.SMOOTH)


def ore_overlay(base_img, spot_color, count, seed, size=16):
    rng = random.Random(seed)
    img = base_img.copy()
    d = ImageDraw.Draw(img)
    for _ in range(count):
        cx = rng.randint(1, size - 2)
        cy = rng.randint(1, size - 2)
        rad = rng.randint(1, 2)
        d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad],
                  fill=spot_color + (255,))
        # dark edge
        d.point([(cx - rad, cy), (cx + rad, cy), (cx, cy - rad), (cx, cy + rad)],
                fill=(30, 0, 0, 255))
    return img


def save(img, name):
    path = os.path.join(TEXTURES, f"{name}.png")
    img.save(path)
    print(f"wrote {path}")


# Base terrain textures
dirt = noise_base(16, (95, 25, 25), 18, (50, 8, 8), vein_count=8, seed=1)
save(dirt, "blood_dirt")

sand = noise_base(16, (165, 70, 70), 22, (90, 20, 20), vein_count=5, seed=2)
save(sand, "blood_sand")

stone = noise_base(16, (80, 30, 30), 14, (40, 10, 10), vein_count=6, seed=3)
save(stone, "blood_stone")

# Ores — stone base + colored spots
ore_specs = [
    ("blood_coal_ore",     (25, 10, 10), 7, 100),
    ("blood_iron_ore",     (220, 180, 150), 6, 101),
    ("blood_gold_ore",     (255, 215, 0), 6, 102),
    ("blood_diamond_ore",  (140, 235, 235), 6, 103),
    ("blood_redstone_ore", (255, 20, 20), 8, 104),
    ("blood_lapis_ore",    (30, 60, 200), 7, 105),
    ("blood_emerald_ore",  (50, 220, 100), 6, 106),
]
for name, color, cnt, seed in ore_specs:
    img = ore_overlay(stone.copy(), color, cnt, seed)
    save(img, name)

# JSON assets
for name in ("blood_dirt", "blood_sand", "blood_stone",
             "blood_coal_ore", "blood_iron_ore", "blood_gold_ore",
             "blood_diamond_ore", "blood_redstone_ore",
             "blood_lapis_ore", "blood_emerald_ore"):
    simple_block(name)

print("done.")
