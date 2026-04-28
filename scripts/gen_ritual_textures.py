"""Textures for the ritual bowls."""
from PIL import Image, ImageDraw
import os, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures", "block")
os.makedirs(BASE, exist_ok=True)
S = 16

def save(name, img): img.save(os.path.join(BASE, name + ".png"))

# --- blood_sacrificial_bowl: gray stone with red rim ---
img = Image.new("RGBA", (S, S), (90, 90, 95, 255))
d = ImageDraw.Draw(img)
random.seed(41)
for _ in range(80):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(60, 120)
    d.point((x, y), fill=(v, v, v + 5, 255))
# rim — red ring on top
d.rectangle([0, 0, 15, 1], fill=(120, 25, 30, 255))
d.rectangle([0, 14, 15, 15], fill=(60, 60, 65, 255))
# inner well
d.rectangle([3, 3, 12, 12], fill=(40, 40, 45, 255), outline=(20, 20, 25, 255))
# blood pool
d.rectangle([4, 4, 11, 11], fill=(110, 20, 28, 255))
d.point((6, 6), fill=(200, 60, 80, 255))
d.point((9, 8), fill=(180, 40, 60, 255))
save("blood_sacrificial_bowl", img)

# --- golden_blood_bowl: gold with red runes ---
img = Image.new("RGBA", (S, S), (180, 145, 50, 255))
d = ImageDraw.Draw(img)
random.seed(43)
for _ in range(60):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(140, 220)
    d.point((x, y), fill=(v + 10, v - 30, v // 3, 255))
# top rim
d.rectangle([0, 0, 15, 1], fill=(240, 200, 80, 255))
# inner well — dark blood
d.rectangle([3, 3, 12, 12], fill=(80, 15, 22, 255), outline=(220, 180, 60, 255))
d.rectangle([4, 4, 11, 11], fill=(140, 25, 35, 255))
# rune (cross)
d.line([6, 7, 9, 7], fill=(255, 220, 100, 255))
d.line([7, 5, 7, 9], fill=(255, 220, 100, 255))
# corner studs
for cx, cy in [(1, 1), (14, 1), (1, 14), (14, 14)]:
    d.point((cx, cy), fill=(255, 220, 80, 255))
save("golden_blood_bowl", img)

print("OK ritual bowl textures")
