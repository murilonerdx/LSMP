"""Bulk generator for Spirit World blocks + cosmic horror + angel items.
Generates textures, models, blockstates, loot tables in batch.
"""
import os, json, random
from PIL import Image, ImageDraw

random.seed(2026)
BASE = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources"

SPIRIT_BLOCKS = [
    ("spirit_grass_block", (140, 90, 200), (180, 130, 240)),
    ("spirit_dirt",        (75, 50, 100),  (95, 65, 125)),
    ("ethereal_stone",     (130, 110, 170), (160, 140, 200)),
    ("ethereal_stone_bricks", (130, 110, 170), (160, 140, 200)),
    ("soul_brick",         (40, 70, 120),  (60, 95, 150)),
    ("halo_marble",        (230, 220, 180), (245, 235, 200)),
    ("halo_marble_bricks", (230, 220, 180), (245, 235, 200)),
    ("dream_glass",        (180, 140, 220), (220, 180, 255)),
    ("whisperwood_log",    (90, 70, 110),  (120, 95, 140)),
    ("whisperwood_planks", (140, 120, 165), (170, 145, 195)),
    ("whisperwood_leaves", (90, 140, 180), (120, 180, 220)),
    ("astral_lantern",     (255, 240, 180), (255, 255, 255)),
    ("crystal_spirit_ore", (180, 110, 220), (255, 200, 255)),
    ("sanctum_ward",       (255, 250, 220), (255, 255, 255)),
    ("holy_censer",        (220, 180, 80),  (255, 230, 130)),
]

HORROR_ITEMS = [
    ("whispering_veil",     (60, 30, 80),   (150, 100, 180)),
    ("eyes_of_abyss",       (20, 20, 40),   (255, 200, 50)),
    ("cursed_cradle",       (50, 20, 30),   (160, 60, 80)),
    ("pendulum_of_dread",   (40, 30, 60),   (180, 150, 220)),
    ("lantern_of_false_memory", (40, 30, 60), (200, 180, 255)),
    ("tongue_of_old_ones",  (110, 30, 50),  (200, 100, 130)),
    ("hourglass_of_regression", (180, 150, 100), (240, 220, 160)),
    ("void_seer_orb",       (20, 10, 40),   (140, 100, 220)),
]

ANGEL_ITEMS = [
    ("halo_of_light",       (255, 240, 180), (255, 255, 230)),
    ("wings_of_ascension",  (240, 240, 255), (255, 255, 255)),
    ("angel_tear_amulet",   (200, 220, 255), (255, 255, 255)),
    ("spirit_anchor",       (180, 200, 230), (220, 240, 255)),
    ("choir_bell",          (220, 190, 100), (255, 230, 150)),
    ("divine_smite_rod",    (250, 240, 180), (255, 255, 220)),
    ("soul_mirror",         (220, 230, 255), (255, 255, 255)),
    ("spirit_compass",      (190, 200, 220), (240, 245, 255)),
    ("holy_water_bucket",   (180, 200, 230), (220, 240, 255)),
    ("angel_wing_feather",  (240, 240, 255), (255, 255, 255)),
    ("seraph_blade",        (255, 240, 200), (255, 255, 240)),
    ("prayer_book",         (220, 180, 100), (255, 215, 130)),
]

def add_noise(img, intensity=12):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            n = random.randint(-intensity, intensity)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)),
                        max(0, min(255, b + n)), a)
    return img

def gen_block_texture(name, base, accent, path):
    img = Image.new('RGBA', (16, 16), (base[0], base[1], base[2], 255))
    d = ImageDraw.Draw(img)
    if 'bricks' in name:
        for y in range(0, 16, 4):
            offset = 4 if (y // 4) % 2 == 0 else 0
            d.line([(0, y), (16, y)], fill=(accent[0], accent[1], accent[2], 255))
            for x in range(offset, 16, 8):
                d.line([(x, y), (x, y + 4)], fill=(accent[0], accent[1], accent[2], 255))
    elif 'log' in name:
        for r in [3, 5]:
            d.ellipse([8 - r, 8 - r, 8 + r, 8 + r], outline=(accent[0], accent[1], accent[2], 255))
        for x in [3, 7, 11]:
            d.line([(x, 0), (x, 15)], fill=(accent[0], accent[1], accent[2], 200))
    elif 'planks' in name:
        for y in [4, 9, 14]:
            d.line([(0, y), (16, y)], fill=(accent[0], accent[1], accent[2], 200))
        for x in [3, 11]:
            d.line([(x, 0), (x, 4)], fill=(accent[0], accent[1], accent[2], 150))
            d.line([(x, 5), (x, 9)], fill=(accent[0], accent[1], accent[2], 150))
    elif 'leaves' in name:
        for _ in range(40):
            x, y = random.randint(0, 15), random.randint(0, 15)
            d.point((x, y), fill=(accent[0], accent[1], accent[2], 255))
    elif 'glass' in name:
        img2 = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        d2 = ImageDraw.Draw(img2)
        for y in range(16):
            for x in range(16):
                a = 120 if (x in [0, 15] or y in [0, 15]) else 80
                d2.point((x, y), fill=(base[0], base[1], base[2], a))
        d2.rectangle((0, 0, 15, 15), outline=(accent[0], accent[1], accent[2], 200))
        img = img2
    elif 'lantern' in name or 'censer' in name:
        d.rectangle((0, 0, 15, 15), fill=(base[0], base[1], base[2], 255))
        d.rectangle((3, 3, 12, 12), fill=(accent[0], accent[1], accent[2], 255))
        d.rectangle((5, 5, 10, 10), fill=(255, 255, 255, 255))
    elif 'ore' in name:
        for _ in range(15):
            x, y = random.randint(0, 14), random.randint(0, 14)
            d.rectangle((x, y, x + 1, y + 1), fill=(accent[0], accent[1], accent[2], 255))
    elif 'grass' in name:
        d.rectangle((0, 0, 15, 2), fill=(accent[0], accent[1], accent[2], 255))
        d.rectangle((0, 3, 15, 15), fill=(75, 50, 100, 255))
    elif 'sanctum' in name:
        d.rectangle((7, 2, 9, 14), fill=(accent[0], accent[1], accent[2], 255))
        d.rectangle((2, 7, 14, 9), fill=(accent[0], accent[1], accent[2], 255))
    elif 'marble' in name:
        for _ in range(3):
            y1 = random.randint(2, 13)
            d.line([(random.randint(0, 5), y1), (random.randint(10, 15), y1 + random.randint(-2, 2))],
                   fill=(accent[0], accent[1], accent[2], 200))
    add_noise(img, 12)
    img.save(path)

def gen_item_texture(name, base, accent, path):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    base_c = (base[0], base[1], base[2], 255)
    accent_c = (accent[0], accent[1], accent[2], 255)
    if 'veil' in name:
        for y in range(2, 14):
            offset = (y - 8) // 2
            d.line([(3 + offset, y), (13 + offset, y)], fill=base_c)
    elif 'eye' in name or 'orb' in name:
        d.ellipse((1, 1, 14, 14), fill=base_c)
        d.ellipse((4, 4, 11, 11), fill=accent_c)
        d.ellipse((6, 6, 9, 9), fill=(0, 0, 0, 255))
    elif 'cradle' in name:
        d.rectangle((2, 6, 13, 13), fill=base_c)
        d.rectangle((2, 4, 13, 6), fill=accent_c)
        for x in [3, 6, 9, 12]:
            d.line([(x, 4), (x, 13)], fill=accent_c)
    elif 'pendulum' in name:
        d.line([(8, 1), (8, 9)], fill=(150, 150, 150, 255))
        d.ellipse((4, 9, 12, 14), fill=base_c)
        d.ellipse((6, 10, 9, 12), fill=accent_c)
    elif 'lantern' in name and 'astral' not in name:
        d.rectangle((4, 5, 12, 13), fill=base_c)
        d.rectangle((6, 7, 10, 11), fill=accent_c)
        d.line([(8, 1), (8, 5)], fill=(100, 100, 100, 255))
    elif 'tongue' in name:
        d.line([(8, 14), (8, 7)], fill=base_c)
        d.line([(8, 7), (5, 3)], fill=base_c)
        d.line([(8, 7), (11, 3)], fill=base_c)
    elif 'hourglass' in name:
        d.line([(3, 2), (13, 2)], fill=base_c)
        d.line([(3, 13), (13, 13)], fill=base_c)
        d.polygon([(3, 2), (13, 2), (8, 7)], fill=accent_c)
        d.polygon([(8, 7), (3, 13), (13, 13)], fill=accent_c)
    elif 'halo' in name:
        d.ellipse((2, 5, 14, 11), outline=accent_c, width=2)
        d.ellipse((4, 6, 12, 10), fill=base_c)
    elif 'wings' in name:
        d.polygon([(3, 4), (8, 8), (3, 14)], fill=base_c)
        d.polygon([(13, 4), (8, 8), (13, 14)], fill=base_c)
        d.polygon([(4, 6), (7, 8), (4, 12)], fill=accent_c)
        d.polygon([(12, 6), (9, 8), (12, 12)], fill=accent_c)
    elif 'tear' in name or 'amulet' in name:
        d.line([(8, 1), (8, 4)], fill=(150, 150, 150, 255))
        d.polygon([(8, 4), (4, 9), (8, 14), (12, 9)], fill=base_c)
        d.polygon([(8, 6), (6, 9), (8, 12), (10, 9)], fill=accent_c)
    elif 'anchor' in name:
        d.line([(8, 1), (8, 11)], fill=base_c)
        d.line([(6, 3), (10, 3)], fill=base_c)
        d.arc([3, 6, 13, 14], 0, 180, fill=accent_c, width=2)
    elif 'bell' in name:
        d.polygon([(5, 3), (11, 3), (13, 11), (3, 11)], fill=base_c)
        d.rectangle((5, 11, 11, 13), fill=accent_c)
    elif 'rod' in name or 'blade' in name:
        if 'blade' in name:
            d.line([(2, 14), (14, 2)], fill=accent_c, width=2)
            d.line([(2, 12), (12, 2)], fill=(255, 255, 255, 255))
            d.rectangle((1, 13, 4, 16), fill=(180, 100, 50, 255))
        else:
            d.line([(2, 14), (14, 2)], fill=accent_c, width=2)
            d.ellipse((1, 13, 4, 16), fill=base_c)
    elif 'mirror' in name:
        d.ellipse((3, 1, 13, 11), fill=(180, 180, 180, 255))
        d.ellipse((4, 2, 12, 10), fill=accent_c)
        d.line([(8, 11), (8, 14)], fill=base_c)
        d.line([(6, 14), (10, 14)], fill=base_c)
    elif 'compass' in name:
        d.ellipse((1, 1, 14, 14), fill=(180, 180, 180, 255))
        d.ellipse((2, 2, 13, 13), fill=base_c)
        d.polygon([(8, 3), (10, 8), (8, 13), (6, 8)], fill=accent_c)
    elif 'water_bucket' in name:
        d.rectangle((3, 4, 13, 14), fill=(150, 150, 150, 255))
        d.rectangle((4, 5, 12, 13), fill=base_c)
        d.line([(4, 3), (12, 3)], fill=(180, 180, 180, 255))
    elif 'feather' in name:
        d.line([(4, 2), (4, 14)], fill=(150, 150, 150, 255))
        for y in range(3, 13):
            length = 6 - abs(y - 8) // 2
            d.line([(5, y), (5 + length, y)], fill=base_c)
    elif 'book' in name:
        d.rectangle((2, 2, 14, 14), fill=base_c)
        d.rectangle((3, 3, 13, 13), fill=accent_c)
        d.line([(8, 3), (8, 13)], fill=base_c)
        d.line([(5, 6), (11, 6)], fill=base_c)
        d.line([(5, 9), (11, 9)], fill=base_c)
    else:
        d.ellipse((3, 3, 13, 13), fill=base_c)
        d.ellipse((5, 5, 11, 11), fill=accent_c)
    add_noise(img, 8)
    img.save(path)

def write_json(path, data):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

def main():
    tex_block = os.path.join(BASE, 'assets/liberthia/textures/block')
    tex_item = os.path.join(BASE, 'assets/liberthia/textures/item')
    model_block = os.path.join(BASE, 'assets/liberthia/models/block')
    model_item = os.path.join(BASE, 'assets/liberthia/models/item')
    blockstate = os.path.join(BASE, 'assets/liberthia/blockstates')
    loot = os.path.join(BASE, 'data/liberthia/loot_tables/blocks')
    for d in [tex_block, tex_item, model_block, model_item, blockstate, loot]:
        os.makedirs(d, exist_ok=True)

    for name, base, accent in SPIRIT_BLOCKS:
        gen_block_texture(name, base, accent, os.path.join(tex_block, name + '.png'))
    print("generated", len(SPIRIT_BLOCKS), "block textures")

    for name, base, accent in HORROR_ITEMS + ANGEL_ITEMS:
        gen_item_texture(name, base, accent, os.path.join(tex_item, name + '.png'))
    print("generated", len(HORROR_ITEMS) + len(ANGEL_ITEMS), "item textures")

    for name, _, _ in SPIRIT_BLOCKS:
        if name == 'whisperwood_log':
            gen_block_texture(name + '_top', (140, 120, 165), (170, 145, 195),
                              os.path.join(tex_block, name + '_top.png'))
            write_json(os.path.join(model_block, name + '.json'), {
                "parent": "minecraft:block/cube_column",
                "textures": {
                    "end": "liberthia:block/" + name + "_top",
                    "side": "liberthia:block/" + name
                }
            })
            write_json(os.path.join(model_block, name + '_horizontal.json'), {
                "parent": "minecraft:block/cube_column_horizontal",
                "textures": {
                    "end": "liberthia:block/" + name + "_top",
                    "side": "liberthia:block/" + name
                }
            })
            write_json(os.path.join(blockstate, name + '.json'), {
                "variants": {
                    "axis=x": {"model": "liberthia:block/" + name + "_horizontal", "x": 90, "y": 90},
                    "axis=y": {"model": "liberthia:block/" + name},
                    "axis=z": {"model": "liberthia:block/" + name + "_horizontal", "x": 90}
                }
            })
        elif 'leaves' in name:
            write_json(os.path.join(model_block, name + '.json'), {
                "parent": "minecraft:block/leaves",
                "textures": {"all": "liberthia:block/" + name}
            })
            write_json(os.path.join(blockstate, name + '.json'), {
                "variants": {"": {"model": "liberthia:block/" + name}}
            })
        else:
            write_json(os.path.join(model_block, name + '.json'), {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": "liberthia:block/" + name}
            })
            write_json(os.path.join(blockstate, name + '.json'), {
                "variants": {"": {"model": "liberthia:block/" + name}}
            })

        write_json(os.path.join(model_item, name + '.json'), {
            "parent": "liberthia:block/" + name
        })
        write_json(os.path.join(loot, name + '.json'), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": "liberthia:" + name}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]
            }]
        })

    for name, _, _ in HORROR_ITEMS + ANGEL_ITEMS:
        write_json(os.path.join(model_item, name + '.json'), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "liberthia:item/" + name}
        })

    print("DONE — all models, blockstates, loot tables generated")

if __name__ == "__main__":
    main()
