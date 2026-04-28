"""Fix the broken/ugly textures: blood_torch, sanguine_sapling,
sanguine_ward_sword, dark_matter_block, clear_matter_block, yellow_matter_block."""
from PIL import Image, ImageDraw
import os, math, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
BLOCK = os.path.join(BASE, "block")
S = 16

def img(): return Image.new("RGBA", (S, S), (0, 0, 0, 0))
def save_block(n, im): im.save(os.path.join(BLOCK, n + ".png"))
def save_item(n, im): im.save(os.path.join(ITEM, n + ".png"))

# ---- blood_torch: vanilla torch shape (column at x=7,8 from y=8 down to y=15) ----
im = img(); d = ImageDraw.Draw(im)
# Vanilla torch texture: torch column at x=7..8, y=8..15. Flame at y=6..7.
# Stick body
d.rectangle([7, 9, 8, 15], fill=(120, 70, 30, 255))      # main wood
d.point((7, 9),  fill=(180, 110, 60, 255))               # highlight top of stick
d.point((8, 14), fill=(80, 50, 20, 255))                 # shadow bottom
# Flame/ember on top: deep red core
d.rectangle([7, 7, 8, 8], fill=(220, 50, 60, 255))       # red flame core
d.point((7, 7), fill=(255, 80, 80, 255))
d.point((8, 8), fill=(180, 30, 40, 255))
save_block("blood_torch", im)

# ---- sanguine_sapling: clear cross sprite of a tiny crimson sapling ----
im = img(); d = ImageDraw.Draw(im)
# Trunk
d.line([8, 15, 8, 8], fill=(70, 30, 25, 255))
d.line([7, 14, 7, 9], fill=(50, 20, 15, 255))
# Leaf cluster (cluster of red leaves)
d.ellipse([4, 2, 12, 9], fill=(150, 30, 50, 255), outline=(60, 5, 15, 255))
d.point((6, 4), fill=(220, 60, 80, 255))
d.point((10, 4), fill=(220, 60, 80, 255))
d.point((8, 2), fill=(220, 60, 80, 255))
d.point((8, 6), fill=(255, 100, 120, 255))
# Branches sticking out
d.point((4, 5), fill=(70, 30, 25, 255))
d.point((11, 6), fill=(70, 30, 25, 255))
save_block("sanguine_sapling", im)

# ---- sanguine_ward_sword: handheld diagonal layout (correct for handheld parent) ----
# Handheld parent expects: pommel at (1,15), blade tip at (15,1)
# Sword sprite goes diagonally bottom-left to top-right.
im = img(); d = ImageDraw.Draw(im)
# Pommel + grip (lower-left corner)
d.point((1, 15), fill=(60, 30, 10, 255))
d.point((2, 14), fill=(80, 40, 15, 255))
d.point((3, 13), fill=(120, 70, 30, 255))
d.point((4, 12), fill=(140, 90, 40, 255))
# Cross-guard
d.point((4, 13), fill=(180, 200, 220, 255))   # silver guard
d.point((5, 12), fill=(180, 200, 220, 255))
d.point((3, 11), fill=(140, 160, 180, 255))
d.point((6, 11), fill=(140, 160, 180, 255))
# Blade — diagonal stripe
for i in range(10):
    x = 5 + i
    y = 11 - i
    if 0 <= x < 16 and 0 <= y < 16:
        d.point((x, y), fill=(220, 230, 240, 255))         # bright blade edge
        if x + 1 < 16 and y + 1 < 16:
            d.point((x + 1, y + 1), fill=(150, 170, 190, 255))   # blade body
        if x - 1 >= 0 and y - 1 >= 0 and i > 0:
            d.point((x - 1, y - 1), fill=(255, 255, 255, 255))   # highlight
# Red core down center of blade (themed)
d.point((8, 8), fill=(200, 50, 60, 255))
d.point((10, 6), fill=(200, 50, 60, 255))
# Tip
d.point((15, 1), fill=(255, 255, 255, 255))
d.point((14, 2), fill=(220, 230, 240, 255))
save_item("sanguine_ward_sword", im)

# ---- dark_matter_block: deeper purple-black with subtle starfield ----
im = Image.new("RGBA", (S, S), (8, 4, 18, 255))
d = ImageDraw.Draw(im)
random.seed(101)
# noise base
for _ in range(120):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(8, 35)
    d.point((x, y), fill=(v, v // 3, v + 10, 255))
# small bright stars
for _ in range(8):
    x, y = random.randint(0, 15), random.randint(0, 15)
    d.point((x, y), fill=(180, 130, 220, 255))
# 3 brighter star points
for x, y in [(3, 4), (12, 9), (7, 13)]:
    d.point((x, y), fill=(220, 200, 255, 255))
    if x + 1 < 16: d.point((x + 1, y), fill=(150, 100, 180, 255))
    if y + 1 < 16: d.point((x, y + 1), fill=(150, 100, 180, 255))
# subtle frame (corners)
for cx, cy in [(0, 0), (15, 0), (0, 15), (15, 15)]:
    d.point((cx, cy), fill=(30, 15, 45, 255))
save_block("dark_matter_block", im)

# ---- clear_matter_block: bright cyan crystal with refraction lines ----
im = Image.new("RGBA", (S, S), (180, 230, 240, 255))
d = ImageDraw.Draw(im)
random.seed(103)
# crystalline noise
for _ in range(80):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(180, 240)
    d.point((x, y), fill=(v - 30, v, v + 5, 255))
# diagonal refraction lines
for off in range(-12, 16, 4):
    for i in range(16):
        x = i
        y = i + off
        if 0 <= y < 16:
            d.point((x, y), fill=(255, 255, 255, 200))
# bright facet centers
for cx, cy in [(3, 3), (11, 6), (5, 11), (12, 12)]:
    d.point((cx, cy), fill=(255, 255, 255, 255))
    if cx + 1 < 16 and cy + 1 < 16:
        d.point((cx + 1, cy + 1), fill=(220, 240, 255, 255))
# corners
for cx, cy in [(0, 0), (15, 0), (0, 15), (15, 15)]:
    d.point((cx, cy), fill=(120, 180, 200, 255))
save_block("clear_matter_block", im)

# ---- yellow_matter_block: golden energy crystal ----
im = Image.new("RGBA", (S, S), (220, 180, 60, 255))
d = ImageDraw.Draw(im)
random.seed(107)
# golden noise
for x in range(S):
    for y in range(S):
        v = random.randint(160, 240)
        r = max(0, min(255, v))
        g = max(0, min(255, v - 40))
        b = max(0, min(255, v // 4))
        d.point((x, y), fill=(r, g, b, 255))
# energy veins
d.line([2, 8, 14, 8], fill=(255, 230, 120, 255))
d.line([8, 2, 8, 14], fill=(255, 230, 120, 255))
d.line([3, 3, 12, 12], fill=(255, 230, 120, 255))
d.line([3, 12, 12, 3], fill=(255, 230, 120, 255))
# bright center
d.ellipse([6, 6, 9, 9], fill=(255, 255, 200, 255), outline=(255, 200, 60, 255))
d.point((8, 8), fill=(255, 255, 255, 255))
# darker frame
for cx, cy in [(0, 0), (15, 0), (0, 15), (15, 15)]:
    d.point((cx, cy), fill=(140, 100, 30, 255))
save_block("yellow_matter_block", im)

print("OK fixed textures: blood_torch, sanguine_sapling, sanguine_ward_sword, dark_matter_block, clear_matter_block, yellow_matter_block")
