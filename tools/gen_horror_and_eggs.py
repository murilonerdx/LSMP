"""Generate textures for horror items + spawn eggs that are missing."""

from PIL import Image, ImageDraw
import os
import json

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "liberthia", "textures", "item")
MODEL_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets",
                          "liberthia", "models", "item")


def egg(name, c1, c2):
    """Vanilla-style spawn egg pattern."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Egg shape
    for y in range(2, 14):
        for x in range(4, 12):
            dx = abs(x - 7.5)
            dy = y - 8
            dist = (dx * 1.2) ** 2 + dy ** 2
            if dist <= 14:
                d.point((x, y), fill=c1)
    # Spots
    spots = [(6, 5), (9, 7), (5, 9), (10, 10), (7, 11), (8, 4)]
    for sx, sy in spots:
        d.point((sx, sy), fill=c2)
        d.point((sx + 1, sy), fill=c2)
        d.point((sx, sy + 1), fill=c2)
    return img


def shard(name, c1, c2):
    """Cosmic shard fragment."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (3, 8), (6, 14), (12, 12), (13, 5)], fill=c1, outline=c2)
    d.line([(8, 3), (9, 12)], fill=c2)
    return img


def mask(name, c1, c2):
    """Mirror Mask"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 2), (3, 6), (3, 11), (8, 14), (13, 11), (13, 6)], fill=c1, outline=c2)
    # Eye slits
    d.line([(5, 8), (7, 8)], fill=c2)
    d.line([(9, 8), (11, 8)], fill=c2)
    # Mirror shine
    d.point((6, 5), fill=(255, 255, 255))
    d.point((10, 5), fill=(255, 255, 255))
    return img


def book(name, c1, c2):
    """Fractured Scripture book."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=c2)
    d.rectangle([3, 3, 12, 12], fill=c1)
    # Cracked pattern
    d.line([(4, 5), (8, 8)], fill=c2)
    d.line([(8, 8), (11, 6)], fill=c2)
    d.line([(8, 8), (7, 11)], fill=c2)
    return img


def halo(name, c1, c2):
    """Halo of Abaddon - circular ring."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], outline=c2)
    d.ellipse([3, 3, 12, 12], outline=c1)
    d.ellipse([4, 4, 11, 11], outline=c1)
    # Eye in center
    d.point((7, 7), fill=c2)
    d.point((8, 7), fill=c2)
    d.point((7, 8), fill=c2)
    d.point((8, 8), fill=c2)
    return img


def chestplate(name, c1, c2):
    """Veinbound Chestplate - flesh armor."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 12, 14], fill=c2)
    d.rectangle([4, 4, 11, 13], fill=c1)
    # Veins
    d.line([(5, 5), (7, 12)], fill=c2)
    d.line([(10, 5), (8, 12)], fill=c2)
    d.line([(6, 8), (9, 8)], fill=c2)
    return img


# (name, kind, color1, color2)
ITEMS = [
    # Cosmic Horror Items
    ("mirror_mask", "mask", (200, 200, 220), (100, 100, 150)),
    ("fractured_scripture", "book", (60, 30, 80), (180, 100, 220)),
    ("halo_of_abaddon", "halo", (255, 220, 100), (200, 100, 50)),
    ("veinbound_chestplate", "chestplate", (140, 30, 60), (60, 10, 20)),
    # Cosmic Horror Spawn Eggs
    ("empty_man_egg", "egg", (40, 40, 40), (200, 200, 200)),
    ("observer_egg", "egg", (50, 50, 100), (180, 180, 220)),
    ("absence_egg", "egg", (10, 10, 10), (50, 50, 50)),
    ("remembered_egg", "egg", (100, 30, 100), (200, 100, 200)),
    # Wizard Eggs
    ("pyromancer_egg", "egg", (255, 100, 50), (255, 200, 100)),
    ("cryomancer_egg", "egg", (100, 200, 255), (200, 230, 255)),
    ("electromancer_egg", "egg", (255, 255, 100), (255, 255, 200)),
    ("necromancer_egg", "egg", (150, 0, 50), (200, 80, 120)),
    ("eldritch_cultist_egg", "egg", (100, 50, 200), (180, 100, 230)),
    ("apothecarist_egg", "egg", (100, 200, 100), (180, 230, 180)),
    ("keeper_egg", "egg", (255, 240, 180), (255, 250, 220)),
    ("archevoker_egg", "egg", (100, 50, 200), (200, 100, 255)),
    # Familiar Eggs
    ("wisp_picker_egg", "egg", (255, 255, 180), (255, 255, 230)),
    ("grove_sprite_egg", "egg", (50, 180, 50), (120, 220, 120)),
    ("soul_reaper_egg", "egg", (70, 0, 70), (140, 0, 140)),
    ("whelp_egg", "egg", (255, 100, 50), (255, 180, 100)),
    ("carbuncle_egg", "egg", (200, 170, 100), (255, 220, 150)),
    ("amethyst_golem_egg", "egg", (150, 100, 200), (200, 150, 255)),
    # Boss Eggs
    ("abyssal_lich_egg", "egg", (50, 0, 70), (140, 0, 140)),
    ("lich_stalker_egg", "egg", (30, 30, 70), (70, 70, 100)),
    ("lich_hunter_egg", "egg", (70, 30, 100), (100, 70, 140)),
]

KIND_FNS = {"mask": mask, "book": book, "halo": halo, "chestplate": chestplate, "egg": egg}

count = 0
for name, kind, c1, c2 in ITEMS:
    fn = KIND_FNS[kind]
    img = fn(name, c1, c2)
    img.save(os.path.join(TEX_ITEM, f"{name}.png"))
    model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{name}"}
    }
    with open(os.path.join(MODEL_ITEM, f"{name}.json"), "w") as f:
        json.dump(model, f)
    count += 1

print(f"Generated {count} horror+egg textures")
