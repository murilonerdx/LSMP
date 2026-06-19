"""r116: textures pros 6 Spirit Reagents (drops do Spirit World pra craft glyphs)."""
from PIL import Image, ImageDraw
import os, math, json

ROOT = os.path.dirname(__file__)
TEX = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                     "assets", "liberthia", "textures", "item"))
MODEL = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                       "assets", "liberthia", "models", "item"))
os.makedirs(TEX, exist_ok=True)
os.makedirs(MODEL, exist_ok=True)


def alpha_circle(d, img, cx, cy, radius, color, alpha=255, falloff=1.0):
    r = max(1, int(radius))
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if not (0 <= x < 16 and 0 <= y < 16):
                continue
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            if dist > radius:
                continue
            f = 1.0 - (dist / radius)
            f = math.pow(f, falloff)
            a = int(alpha * f)
            if a <= 0:
                continue
            existing = img.getpixel((x, y))
            nr = min(255, existing[0] + int(color[0] * a / 255))
            ng = min(255, existing[1] + int(color[1] * a / 255))
            nb = min(255, existing[2] + int(color[2] * a / 255))
            na = min(255, existing[3] + a)
            img.putpixel((x, y), (nr, ng, nb, na))


def make_wisp_essence():
    """Esferinha branca-amarelada flutuante com glow."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Aura
    alpha_circle(d, img, 8, 8, 7, (200, 200, 100), 80, 1.5)
    # Core
    alpha_circle(d, img, 8, 8, 4, (255, 255, 180), 230, 0.6)
    # Highlight
    d.point((7, 6), fill=(255, 255, 255, 255))
    d.point((6, 7), fill=(255, 255, 255, 255))
    return img


def make_astral_dust():
    """Frasco pequeno com 4-5 pixels de poeira azul-roxa."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Bottle outline
    d.rectangle([5, 4, 10, 14], fill=(40, 30, 50, 200), outline=(80, 60, 100, 255))
    d.rectangle([6, 2, 9, 4], fill=(120, 100, 50, 255))  # cap
    # Dust particles inside
    dust_pos = [(6, 8), (8, 9), (7, 11), (9, 12), (6, 12)]
    for px, py in dust_pos:
        d.point((px, py), fill=(180, 140, 230, 255))
        d.point((px, py-1), fill=(150, 100, 200, 200))
    return img


def make_whisperwood_resin():
    """Gota viscosa marrom-dourada."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Tear-drop shape
    d.polygon([(8, 2), (5, 6), (4, 11), (6, 14), (10, 14), (12, 11), (11, 6)],
              fill=(150, 100, 40, 230), outline=(80, 50, 20, 255))
    # Highlight (wet shine)
    d.line([(7, 5), (7, 9)], fill=(220, 180, 80, 200))
    d.point((6, 6), fill=(255, 220, 120, 255))
    return img


def make_memory_shard():
    """Cristal triangular azul-roxo."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Crystal triangle
    d.polygon([(8, 2), (3, 13), (13, 13)],
              fill=(80, 100, 200, 230), outline=(40, 60, 140, 255))
    # Inner facet
    d.polygon([(8, 5), (6, 11), (10, 11)], fill=(140, 170, 240, 200))
    # Highlight
    d.line([(8, 4), (8, 10)], fill=(220, 230, 255, 255))
    return img


def make_ectoplasm_strand():
    """Fios verdes-azul espirralados estilo plasma."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Halo
    alpha_circle(d, img, 8, 8, 6, (40, 200, 180), 60, 1.5)
    # Spiral strands
    for t in range(0, 12):
        angle = t * 0.7
        r = 2 + t * 0.4
        x = int(8 + math.cos(angle) * r)
        y = int(8 + math.sin(angle) * r)
        if 0 <= x < 16 and 0 <= y < 16:
            d.point((x, y), fill=(120, 240, 200, 255))
            if x+1 < 16:
                d.point((x+1, y), fill=(60, 180, 160, 180))
    return img


def make_phantom_ink():
    """Frasco de tinta preta-vermelha goteja."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Bottle
    d.rectangle([4, 4, 11, 13], fill=(15, 5, 20, 240), outline=(50, 30, 60, 255))
    d.rectangle([5, 2, 10, 4], fill=(60, 30, 30, 255))  # cap
    # Ink surface inside
    d.rectangle([5, 9, 10, 12], fill=(100, 15, 30, 255))
    # Drip
    d.line([(8, 13), (8, 15)], fill=(80, 10, 25, 220))
    d.point((8, 15), fill=(140, 30, 50, 255))
    return img


def make_veil_fragment():
    """Pedaço rasgado de pano roxo-prateado, fim de portal."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Cloth piece (ragged shape)
    d.polygon([(4, 3), (12, 4), (13, 8), (11, 13), (4, 12), (3, 7)],
              fill=(70, 30, 100, 220), outline=(40, 15, 60, 255))
    # Silver shimmer threads
    d.line([(5, 6), (12, 7)], fill=(200, 180, 230, 200))
    d.line([(6, 9), (11, 10)], fill=(180, 160, 220, 180))
    # Tear marks
    d.point((10, 4), fill=(40, 15, 60, 255))
    d.point((4, 11), fill=(40, 15, 60, 255))
    return img


REAGENTS = [
    ("wisp_essence", make_wisp_essence),
    ("astral_dust", make_astral_dust),
    ("whisperwood_resin", make_whisperwood_resin),
    ("memory_shard", make_memory_shard),
    ("ectoplasm_strand", make_ectoplasm_strand),
    ("phantom_ink", make_phantom_ink),
    ("veil_fragment", make_veil_fragment),
]

count = 0
for name, fn in REAGENTS:
    img = fn()
    img.save(os.path.join(TEX, f"{name}.png"))
    # Item model JSON
    model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{name}"}
    }
    with open(os.path.join(MODEL, f"{name}.json"), "w") as f:
        json.dump(model, f)
    count += 1

print(f"Gerados {count} Spirit Reagent textures + models")
