# Generates the missing textures that caused "missing model"/purple-black items:
#   item/caster_wand.png          — a wand (handle + glowing tip)
#   item/dark_blood_test_item.png — dark blood test vial
#   block/bone_rune.png, gold_rune.png, diamond_rune.png, netherite_rune.png — rune tablets
#   block/ritual_pedestal.png, ritual_pedestal_top.png — stone ritual pedestal
from PIL import Image
import os

BI = "src/main/resources/assets/liberthia/textures/item"
BB = "src/main/resources/assets/liberthia/textures/block"
os.makedirs(BI, exist_ok=True)
os.makedirs(BB, exist_ok=True)


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = c


def fill(px, c):
    for y in range(16):
        for x in range(16):
            px[x, y] = c


def bevel(px, hi, lo):
    for i in range(16):
        put(px, i, 0, hi); put(px, 0, i, hi)
        put(px, i, 15, lo); put(px, 15, i, lo)


# ---- caster_wand: diagonal handle + glowing cyan tip ----
img = new(); px = img.load()
wood = (110, 78, 46, 255); wood_d = (74, 50, 28, 255)
tip = (90, 220, 245, 255); tip_hot = (220, 255, 255, 255)
for i in range(3, 13):           # diagonal shaft bottom-left → top-right
    put(px, i, 15 - i, wood)
    put(px, i, 16 - i, wood_d)
# tip glow at top-right
put(px, 12, 3, tip); put(px, 13, 2, tip_hot); put(px, 13, 3, tip)
put(px, 12, 2, tip); put(px, 11, 3, tip)
put(px, 14, 2, (150, 240, 255, 200))
img.save(f"{BI}/caster_wand.png"); print("caster_wand")

# ---- dark_blood_test_item: a test vial with dark blood ----
img = new(); px = img.load()
glass = (200, 215, 225, 90); glass_e = (150, 165, 180, 255)
blood = (90, 8, 14, 255); blood_hi = (140, 18, 24, 255)
cork = (150, 110, 60, 255)
for y in range(3, 14):           # vial body
    for x in range(6, 10):
        put(px, x, y, glass)
for y in range(3, 14):
    put(px, 6, y, glass_e); put(px, 9, y, glass_e)
for y in range(7, 13):           # dark blood fill
    for x in range(7, 9):
        put(px, x, y, blood)
put(px, 7, 7, blood_hi)
for x in range(6, 10):
    put(px, x, 13, glass_e)
put(px, 7, 2, cork); put(px, 8, 2, cork)  # cork
img.save(f"{BI}/dark_blood_test_item.png"); print("dark_blood_test_item")


# ---- runes: tablet with a glyph, themed color ----
def rune(name, base, base_hi, glyph):
    img = new(); px = img.load()
    fill(px, base); bevel(px, base_hi, tuple(max(0, c - 30) for c in base[:3]) + (255,))
    # glyph (a stylized eye/cross) in a contrasting glow
    for (gx, gy) in glyph:
        put(px, gx, gy, (235, 235, 255, 255))
    img.save(f"{BB}/{name}.png"); print(name)


GLYPH = [(8, 4), (8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (8, 7), (10, 7),
         (7, 8), (8, 8), (9, 8), (8, 9), (8, 10), (8, 11)]
rune("bone_rune", (210, 205, 185, 255), (235, 230, 212, 255), GLYPH)
rune("gold_rune", (200, 165, 60, 255), (235, 205, 90, 255), GLYPH)
rune("diamond_rune", (110, 200, 210, 255), (160, 235, 240, 255), GLYPH)
rune("netherite_rune", (60, 54, 58, 255), (92, 84, 90, 255), GLYPH)

# ---- ritual_pedestal: stone column side + top ----
stone = (96, 96, 104, 255); stone_hi = (124, 124, 132, 255); stone_lo = (66, 66, 74, 255)
rune_c = (150, 40, 160, 255)
# side
img = new(); px = img.load(); fill(px, stone); bevel(px, stone_hi, stone_lo)
for y in (4, 8, 12):             # carved bands
    for x in range(2, 14):
        put(px, x, y, stone_lo)
put(px, 8, 6, rune_c); put(px, 7, 7, rune_c); put(px, 9, 7, rune_c); put(px, 8, 8, rune_c)  # rune mark
img.save(f"{BB}/ritual_pedestal.png"); print("ritual_pedestal")
# top: a bowl/socket
img = new(); px = img.load(); fill(px, stone); bevel(px, stone_hi, stone_lo)
for y in range(4, 12):
    for x in range(4, 12):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d <= 3.5:
            put(px, x, y, stone_lo)
        if d <= 2.0:
            put(px, x, y, (40, 12, 48, 255))   # dark socket
img.save(f"{BB}/ritual_pedestal_top.png"); print("ritual_pedestal_top")

print("done: missing textures")
