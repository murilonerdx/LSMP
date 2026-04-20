"""Procedural textures for A Mãe de Carne (Fase 2).

Generates:
- entity/flesh_mother.png            (64x32, player UV, bloody humanoid)
- block/heart_of_flesh.png           (16x16, pulsing flesh block)
- item/heart_of_flesh.png            (16x16, block item)
- item/heart_of_flesh_item.png       (16x16, organ drop)
- item/heart_of_the_mother.png       (16x16, glowing core)
- item/sanguine_core.png             (16x16, crystalline core)
- item/sanguine_essence.png          (16x16, vial of dark blood)
- item/flesh_mother_boss_spawn_egg.png (16x16)
"""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
ENTITY = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/entity")
ITEM = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/item")
BLOCK = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/block")
for d in (ENTITY, ITEM, BLOCK):
    os.makedirs(d, exist_ok=True)


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


def fill(img, x, y, w, h, color):
    ImageDraw.Draw(img).rectangle([x, y, x + w - 1, y + h - 1], fill=color)


# ---------- Flesh Mother entity (player UV 64x32) ----------
def flesh_mother():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    skin = (140, 40, 40)
    dark = (80, 15, 15)
    # full body paint
    fill(img, 0, 0, 64, 32, skin)
    speckle(img, 0, 0, 64, 32, skin, 18, 111)
    # head darker
    fill(img, 0, 0, 32, 16, dark)
    speckle(img, 0, 0, 32, 16, dark, 14, 222)
    # face: two hollow red eyes + gash mouth
    d = ImageDraw.Draw(img)
    d.rectangle([10, 11, 11, 12], fill=(255, 60, 60, 255))
    d.rectangle([12, 11, 13, 12], fill=(255, 60, 60, 255))
    d.line([(9, 14), (14, 14)], fill=(30, 0, 0, 255))
    d.point([(10, 15), (12, 15), (14, 14)], fill=(200, 30, 30, 255))
    # veins across body
    rng = random.Random(333)
    for _ in range(40):
        x = rng.randint(16, 39)
        y = rng.randint(16, 31)
        d.point([(x, y)], fill=(40, 5, 5, 255))
    for _ in range(20):
        x = rng.randint(40, 55)
        y = rng.randint(16, 31)
        d.point([(x, y)], fill=(40, 5, 5, 255))
    img.save(os.path.join(ENTITY, "flesh_mother.png"))
    print("wrote flesh_mother.png")


def heart_of_flesh_block():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    fill(img, 0, 0, 16, 16, (110, 25, 25))
    speckle(img, 0, 0, 16, 16, (110, 25, 25), 18, 1)
    # pulse core
    d.ellipse([4, 4, 11, 11], fill=(180, 40, 40, 255))
    d.ellipse([6, 6, 9, 9], fill=(240, 80, 80, 255))
    # dark veins
    for (x, y) in [(2,2),(3,3),(13,2),(12,3),(2,13),(3,12),(13,13),(12,12)]:
        d.point([(x, y)], fill=(40, 5, 5, 255))
    img.save(os.path.join(BLOCK, "heart_of_flesh.png"))
    img.save(os.path.join(ITEM, "heart_of_flesh.png"))  # block item reuses


def heart_of_flesh_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # organ shape (two lobes)
    d.ellipse([3, 4, 8, 11], fill=(140, 25, 25, 255))
    d.ellipse([7, 4, 12, 11], fill=(140, 25, 25, 255))
    d.rectangle([5, 9, 10, 13], fill=(120, 20, 20, 255))
    speckle(img, 3, 4, 10, 10, (140, 25, 25), 12, 5)
    # highlight
    d.point([(5, 6), (9, 6)], fill=(220, 70, 70, 255))
    # stem
    d.line([(8, 2), (8, 5)], fill=(90, 15, 15, 255))
    img.save(os.path.join(ITEM, "heart_of_flesh_item.png"))


def heart_of_the_mother():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # glowing core organ
    d.ellipse([2, 3, 13, 13], fill=(60, 5, 5, 255))
    d.ellipse([4, 5, 11, 11], fill=(200, 30, 30, 255))
    d.ellipse([6, 7, 9, 9], fill=(255, 150, 150, 255))
    # radial vein accents
    for (x, y) in [(2,8),(13,8),(7,2),(8,14)]:
        d.point([(x, y)], fill=(255, 80, 80, 255))
    img.save(os.path.join(ITEM, "heart_of_the_mother.png"))


def sanguine_core():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # crystalline diamond shape
    pts = [(8, 2), (13, 8), (8, 14), (3, 8)]
    d.polygon(pts, fill=(120, 10, 10, 255), outline=(230, 60, 60, 255))
    # inner facets
    d.line([(8, 4), (8, 12)], fill=(200, 40, 40, 255))
    d.line([(5, 8), (11, 8)], fill=(200, 40, 40, 255))
    d.point([(8, 8)], fill=(255, 180, 180, 255))
    img.save(os.path.join(ITEM, "sanguine_core.png"))


def sanguine_essence():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # vial body
    d.rectangle([5, 6, 10, 13], fill=(60, 5, 5, 255), outline=(180, 180, 180, 255))
    # liquid inside
    d.rectangle([6, 8, 9, 12], fill=(180, 20, 20, 255))
    d.point([(7, 9)], fill=(255, 100, 100, 255))
    # neck + cork
    d.rectangle([6, 4, 9, 5], fill=(180, 180, 180, 255))
    d.rectangle([6, 2, 9, 3], fill=(120, 80, 50, 255))
    img.save(os.path.join(ITEM, "sanguine_essence.png"))


def spawn_egg(path, base, spots):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(2, 14):
        w = 1 + min(y - 2, 13 - y)
        d.line([(8 - w // 2 - 1, y), (8 + w // 2, y)], fill=base + (255,))
    rng = random.Random(sum(base) + sum(spots))
    for _ in range(14):
        x = rng.randint(5, 10)
        y = rng.randint(3, 12)
        d.point([(x, y)], fill=spots + (255,))
    img.save(path)


flesh_mother()
heart_of_flesh_block()
heart_of_flesh_item()
heart_of_the_mother()
sanguine_core()
sanguine_essence()
spawn_egg(os.path.join(ITEM, "flesh_mother_boss_spawn_egg.png"),
          (110, 20, 20), (60, 0, 0))

print("mother textures done.")
