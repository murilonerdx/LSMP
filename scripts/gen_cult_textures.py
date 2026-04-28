"""Procedural textures for Culto do Sangue (Fase 1).

Generates:
- entity/blood_cultist.png     (64x32 zombie layout)
- entity/blood_priest.png      (64x32)
- entity/wounded_pilgrim.png   (64x32)
- item/bloody_rag.png          (16x16)
- item/rusted_dagger.png       (16x16)
- item/priest_sigil.png        (16x16)
- item/tome_of_the_mother.png  (16x16)
- item/tome_of_the_pilgrim.png (16x16)
- item/blood_cultist_spawn_egg.png (16x16 overlay-free, uses vanilla egg template tint)
- item/blood_priest_spawn_egg.png  (idem)
- item/wounded_pilgrim_spawn_egg.png (idem)
"""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
ENTITY = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/entity")
ITEM = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/item")
os.makedirs(ENTITY, exist_ok=True)
os.makedirs(ITEM, exist_ok=True)


def fill_rect(img, x, y, w, h, color):
    d = ImageDraw.Draw(img)
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def speckle(img, x, y, w, h, base, variance, seed):
    rng = random.Random(seed)
    px = img.load()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if px[xx, yy][3] == 0:
                continue
            dv = rng.randint(-variance, variance)
            r = max(0, min(255, base[0] + dv))
            g = max(0, min(255, base[1] + dv // 2))
            b = max(0, min(255, base[2] + dv // 2))
            px[xx, yy] = (r, g, b, 255)


# ---------- Entity textures (simple robe overlay on zombie UV) ----------
def entity_robe(path, robe_color, skin_color, accent_color, seed, hooded=True):
    """Paint a robed humanoid on the 64x32 zombie layout.
    The standard zombie UV uses:
      head: (0..32, 0..16)   - 8x8 face on (8..16, 8..16)
      body: (16..40, 16..32) - 8x12 front
      arm:  (40..56, 16..32)
      leg:  (0..16, 16..32)
    Good-enough: fill whole 64x32 with robe color, speckle, add hood shadow on head.
    """
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Paint all pixels as skin tone; then overlay robe over body+arms+legs.
    fill_rect(img, 0, 0, 64, 32, skin_color)
    speckle(img, 0, 0, 64, 32, skin_color, 6, seed)
    # Head area darker (hood cast shadow) if hooded
    if hooded:
        fill_rect(img, 0, 0, 32, 16, robe_color)
        speckle(img, 0, 0, 32, 16, robe_color, 8, seed + 1)
        # Face cutout: show skin on (8..16, 8..16)
        fill_rect(img, 8, 8, 8, 8, skin_color)
        speckle(img, 8, 8, 8, 8, skin_color, 5, seed + 2)
        # Eyes
        d = ImageDraw.Draw(img)
        d.point([(10, 12), (13, 12)], fill=accent_color)
    # Body/legs/arms: robe color
    fill_rect(img, 16, 16, 24, 16, robe_color)
    speckle(img, 16, 16, 24, 16, robe_color, 10, seed + 3)
    fill_rect(img, 40, 16, 16, 16, robe_color)
    speckle(img, 40, 16, 16, 16, robe_color, 10, seed + 4)
    fill_rect(img, 0, 16, 16, 16, robe_color)
    speckle(img, 0, 16, 16, 16, robe_color, 10, seed + 5)
    # Accent band around waist
    d = ImageDraw.Draw(img)
    d.line([(16, 22), (40, 22)], fill=accent_color)
    img.save(path)
    print("wrote", path)


entity_robe(os.path.join(ENTITY, "blood_cultist.png"),
            robe_color=(90, 18, 18), skin_color=(180, 140, 120),
            accent_color=(200, 30, 30), seed=11)

entity_robe(os.path.join(ENTITY, "blood_priest.png"),
            robe_color=(60, 8, 8), skin_color=(200, 180, 160),
            accent_color=(220, 180, 40), seed=22)

entity_robe(os.path.join(ENTITY, "wounded_pilgrim.png"),
            robe_color=(120, 100, 80), skin_color=(210, 180, 150),
            accent_color=(180, 40, 40), seed=33, hooded=False)


# ---------- Item textures (16x16) ----------
def bloody_rag():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # cloth lump
    d.rectangle([3, 5, 12, 12], fill=(180, 160, 130, 255))
    speckle(img, 3, 5, 10, 8, (180, 160, 130), 10, 1)
    # blood stains
    d.rectangle([5, 7, 8, 10], fill=(120, 15, 15, 255))
    d.rectangle([9, 6, 11, 8], fill=(90, 10, 10, 255))
    d.point([(4, 11), (12, 5)], fill=(60, 0, 0, 255))
    img.save(os.path.join(ITEM, "bloody_rag.png"))


def rusted_dagger():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # handle
    d.rectangle([2, 10, 5, 13], fill=(80, 55, 35, 255))
    # blade - jagged rusted
    for i in range(8):
        d.point([(5 + i, 9 - i)], fill=(130, 110, 90))
        d.point([(5 + i, 10 - i)], fill=(90, 60, 40))
    # rust specks
    rng = random.Random(7)
    for _ in range(10):
        d.point([(rng.randint(5, 13), rng.randint(1, 10))], fill=(140, 50, 10, 255))
    img.save(os.path.join(ITEM, "rusted_dagger.png"))


def priest_sigil():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # dark circular medallion
    d.ellipse([2, 2, 13, 13], fill=(30, 5, 5, 255), outline=(180, 30, 30, 255))
    # inner rune (inverted triangle + dot)
    d.polygon([(8, 5), (5, 11), (11, 11)], outline=(220, 40, 40, 255))
    d.point([(8, 9)], fill=(255, 80, 80, 255))
    img.save(os.path.join(ITEM, "priest_sigil.png"))


def tome(path, cover_color, accent):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 2, 12, 13], fill=cover_color + (255,))
    d.rectangle([3, 2, 3, 13], fill=(20, 10, 10, 255))
    # spine
    d.line([(4, 2), (4, 13)], fill=(60, 20, 20, 255))
    # cross/symbol
    d.line([(8, 5), (8, 10)], fill=accent + (255,))
    d.line([(6, 7), (10, 7)], fill=accent + (255,))
    # page edge
    d.line([(12, 3), (12, 12)], fill=(230, 210, 170, 255))
    img.save(path)


def spawn_egg(path, base, spots):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # egg shape
    for y in range(2, 14):
        w = 1 + min(y - 2, 13 - y)
        d.line([(8 - w // 2 - 1, y), (8 + w // 2, y)], fill=base + (255,))
    rng = random.Random(sum(base) + sum(spots))
    for _ in range(14):
        x = rng.randint(5, 10)
        y = rng.randint(3, 12)
        d.point([(x, y)], fill=spots + (255,))
    img.save(path)


bloody_rag()
rusted_dagger()
priest_sigil()
tome(os.path.join(ITEM, "tome_of_the_mother.png"), (70, 10, 10), (220, 40, 40))
tome(os.path.join(ITEM, "tome_of_the_pilgrim.png"), (110, 90, 60), (180, 40, 40))
spawn_egg(os.path.join(ITEM, "blood_cultist_spawn_egg.png"), (90, 13, 13), (43, 0, 0))
spawn_egg(os.path.join(ITEM, "blood_priest_spawn_egg.png"), (58, 0, 0), (122, 18, 18))
spawn_egg(os.path.join(ITEM, "wounded_pilgrim_spawn_egg.png"), (107, 90, 74), (154, 58, 58))

print("cult textures done.")
