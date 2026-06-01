"""Fase 5 textures."""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
ENTITY = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/entity")
ITEM = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/item")
BLOCK = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/block")
for d in (ENTITY, ITEM, BLOCK):
    os.makedirs(d, exist_ok=True)


def fill(img, x, y, w, h, c):
    ImageDraw.Draw(img).rectangle([x, y, x + w - 1, y + h - 1], fill=c)


def speckle(img, x, y, w, h, base, var, seed):
    rng = random.Random(seed)
    px = img.load()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if px[xx, yy][3] == 0:
                continue
            dv = rng.randint(-var, var)
            r = max(0, min(255, base[0] + dv))
            g = max(0, min(255, base[1] + dv))
            b = max(0, min(255, base[2] + dv // 2))
            px[xx, yy] = (r, g, b, 255)


def order_paladin():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    armor = (200, 200, 210)
    skin = (230, 210, 180)
    gold = (220, 180, 50)
    # skin everywhere first
    fill(img, 0, 0, 64, 32, skin)
    speckle(img, 0, 0, 64, 32, skin, 6, 42)
    # armored head (helmet), face cutout
    fill(img, 0, 0, 32, 16, armor)
    speckle(img, 0, 0, 32, 16, armor, 8, 43)
    fill(img, 8, 10, 8, 6, skin)
    speckle(img, 8, 10, 8, 6, skin, 5, 44)
    # eye line
    d = ImageDraw.Draw(img)
    d.line([(8, 11), (15, 11)], fill=(40, 40, 80))
    d.point([(10, 11), (13, 11)], fill=(50, 150, 230))
    # chestplate + arms
    fill(img, 16, 16, 24, 16, armor)
    speckle(img, 16, 16, 24, 16, armor, 7, 45)
    fill(img, 40, 16, 16, 16, armor)
    speckle(img, 40, 16, 16, 16, armor, 7, 46)
    # legs slightly darker
    fill(img, 0, 16, 16, 16, (160, 160, 170))
    speckle(img, 0, 16, 16, 16, (160, 160, 170), 8, 47)
    # gold belt
    d.line([(16, 22), (40, 22)], fill=gold)
    # cross on chest
    d.line([(27, 18), (27, 24)], fill=gold)
    d.line([(25, 20), (29, 20)], fill=gold)
    img.save(os.path.join(ENTITY, "order_paladin.png"))


def order_shrine():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    fill(img, 0, 0, 16, 16, (230, 230, 235))
    speckle(img, 0, 0, 16, 16, (230, 230, 235), 8, 99)
    # gold cross
    d.line([(7, 2), (7, 13)], fill=(230, 190, 60))
    d.line([(8, 2), (8, 13)], fill=(230, 190, 60))
    d.line([(4, 6), (11, 6)], fill=(230, 190, 60))
    d.line([(4, 7), (11, 7)], fill=(230, 190, 60))
    # glow dot
    d.point([(7, 7), (8, 7)], fill=(255, 255, 200))
    img.save(os.path.join(BLOCK, "order_shrine.png"))


def desecrated_relic():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # broken cross shape
    d.line([(7, 2), (7, 13)], fill=(190, 180, 120))
    d.line([(8, 2), (8, 13)], fill=(190, 180, 120))
    d.line([(4, 6), (11, 6)], fill=(190, 180, 120))
    d.line([(4, 7), (11, 7)], fill=(190, 180, 120))
    # crack
    d.line([(8, 8), (6, 12)], fill=(40, 5, 5))
    # blood stains
    d.point([(7, 4), (8, 9), (5, 6), (11, 7), (9, 11)], fill=(140, 20, 20))
    img.save(os.path.join(ITEM, "desecrated_holy_relic.png"))


def spawn_egg(path, base, spots):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(2, 14):
        w = 1 + min(y - 2, 13 - y)
        d.line([(8 - w // 2 - 1, y), (8 + w // 2, y)], fill=base + (255,))
    rng = random.Random(sum(base) + sum(spots))
    for _ in range(14):
        x = rng.randint(5, 10); y = rng.randint(3, 12)
        d.point([(x, y)], fill=spots + (255,))
    img.save(path)


order_paladin()
order_shrine()
desecrated_relic()
spawn_egg(os.path.join(ITEM, "order_paladin_spawn_egg.png"), (230, 230, 235), (220, 180, 50))
print("order textures done.")
