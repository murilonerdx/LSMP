"""Generate textures for T9 (extra attacking blocks + items)."""
from PIL import Image, ImageDraw
import os, math, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
BLOCK = os.path.join(BASE, "block")
os.makedirs(ITEM, exist_ok=True)
os.makedirs(BLOCK, exist_ok=True)

S = 16

def new_img(bg=(0, 0, 0, 0)):
    return Image.new("RGBA", (S, S), bg)

def save_item(name, img):
    img.save(os.path.join(ITEM, name + ".png"))

def save_block(name, img):
    img.save(os.path.join(BLOCK, name + ".png"))

def circle(d, cx, cy, r, fill, outline=None):
    d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=fill, outline=outline)

# ============================================================
# ITEMS
# ============================================================

# --- lightning_grenade: dark sphere with yellow arcs ---
img = new_img(); d = ImageDraw.Draw(img)
circle(d, 8, 9, 6, (40, 40, 60, 255), outline=(15, 15, 25, 255))
circle(d, 6, 7, 2, (90, 90, 120, 255))
# arcs
for (x1, y1, x2, y2) in [(2, 4, 5, 7), (11, 5, 14, 8), (3, 12, 6, 14), (10, 13, 13, 11)]:
    d.line([x1, y1, x2, y2], fill=(255, 240, 120, 255))
d.point((4, 5), fill=(255, 255, 200, 255))
d.point((13, 7), fill=(255, 255, 200, 255))
# fuse
d.line([8, 1, 8, 3], fill=(180, 120, 40, 255))
d.point((8, 0), fill=(255, 200, 60, 255))
save_item("lightning_grenade", img)

# --- burning_gem: red/orange faceted gem ---
img = new_img(); d = ImageDraw.Draw(img)
d.polygon([(8, 1), (13, 6), (11, 14), (5, 14), (3, 6)], fill=(220, 60, 30, 255), outline=(80, 10, 5, 255))
d.polygon([(8, 3), (10, 6), (8, 9), (6, 6)], fill=(255, 160, 60, 255))
d.line([8, 4, 8, 8], fill=(255, 240, 180, 255))
d.point((6, 11), fill=(255, 100, 40, 255))
d.point((10, 11), fill=(255, 100, 40, 255))
# embers
d.point((2, 3), fill=(255, 180, 80, 200))
d.point((14, 4), fill=(255, 180, 80, 200))
save_item("burning_gem", img)

# --- frost_flask: blue vial ---
img = new_img(); d = ImageDraw.Draw(img)
# neck
d.rectangle([6, 1, 9, 3], fill=(180, 220, 240, 255), outline=(80, 120, 160, 255))
# body
d.polygon([(4, 4), (11, 4), (12, 14), (3, 14)], fill=(120, 200, 240, 255), outline=(40, 80, 140, 255))
# liquid
d.rectangle([5, 7, 10, 13], fill=(80, 160, 230, 255))
# highlight
d.line([5, 8, 5, 12], fill=(220, 240, 255, 255))
# snowflake
d.point((8, 10), fill=(255, 255, 255, 255))
d.point((7, 10), fill=(220, 240, 255, 255))
d.point((9, 10), fill=(220, 240, 255, 255))
d.point((8, 9), fill=(220, 240, 255, 255))
d.point((8, 11), fill=(220, 240, 255, 255))
save_item("frost_flask", img)

# --- eye_of_decay: sickly green eye ---
img = new_img(); d = ImageDraw.Draw(img)
# eye sclera (sickly yellow-green)
circle(d, 8, 8, 6, (180, 180, 60, 255), outline=(60, 50, 10, 255))
# veins
d.line([3, 6, 7, 8], fill=(140, 30, 30, 255))
d.line([13, 6, 9, 8], fill=(140, 30, 30, 255))
d.line([4, 11, 7, 9], fill=(140, 30, 30, 255))
# iris
circle(d, 8, 8, 3, (40, 100, 30, 255))
circle(d, 8, 8, 2, (20, 60, 15, 255))
circle(d, 8, 8, 1, (0, 0, 0, 255))
# glint
d.point((9, 7), fill=(220, 240, 180, 255))
save_item("eye_of_decay", img)

# --- withered_totem: dark totem with skull ---
img = new_img(); d = ImageDraw.Draw(img)
# body
d.rectangle([4, 6, 11, 15], fill=(40, 30, 25, 255), outline=(15, 10, 8, 255))
# head
d.rectangle([4, 1, 11, 7], fill=(60, 50, 40, 255), outline=(15, 10, 8, 255))
# eye sockets
d.rectangle([5, 3, 6, 4], fill=(180, 30, 30, 255))
d.rectangle([9, 3, 10, 4], fill=(180, 30, 30, 255))
# mouth
d.line([6, 6, 9, 6], fill=(0, 0, 0, 255))
d.point((7, 5), fill=(0, 0, 0, 255))
d.point((8, 5), fill=(0, 0, 0, 255))
# body cracks
d.line([7, 8, 7, 13], fill=(20, 5, 5, 255))
d.line([6, 10, 9, 10], fill=(20, 5, 5, 255))
# wither aura
d.point((2, 2), fill=(40, 40, 40, 200))
d.point((14, 2), fill=(40, 40, 40, 200))
d.point((13, 13), fill=(40, 40, 40, 200))
save_item("withered_totem", img)

# ============================================================
# BLOCKS
# ============================================================

# --- thorn_briar: cross-style brown spike bush ---
img = new_img((0, 0, 0, 0)); d = ImageDraw.Draw(img)
random.seed(3)
# main stalks
for x in [3, 6, 9, 12]:
    base_y = 15
    top_y = random.randint(4, 8)
    d.line([x, base_y, x, top_y], fill=(80, 50, 25, 255))
    d.line([x + 1, base_y, x + 1, top_y + 1], fill=(50, 30, 15, 255))
    # thorns
    for ty in range(top_y, base_y, 2):
        side = -1 if random.random() < 0.5 else 1
        d.point((x + side, ty), fill=(180, 40, 40, 255))
        d.point((x + 1 - side, ty + 1), fill=(180, 40, 40, 255))
# base leaves
d.point((4, 14), fill=(40, 80, 30, 255))
d.point((10, 13), fill=(40, 80, 30, 255))
save_block("thorn_briar", img)

# --- lightning_node: copper-yellow with electric arcs ---
img = Image.new("RGBA", (S, S), (90, 60, 25, 255))
d = ImageDraw.Draw(img)
random.seed(13)
# copper noise
for _ in range(70):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(60, 130)
    d.point((x, y), fill=(v + 30, v, v // 2, 255))
# coil rings
for y in [3, 7, 11]:
    d.line([2, y, 13, y], fill=(200, 140, 50, 255))
    d.line([2, y + 1, 13, y + 1], fill=(140, 90, 30, 255))
# central core
circle(d, 8, 8, 2, (255, 240, 120, 255), outline=(180, 100, 30, 255))
d.point((8, 8), fill=(255, 255, 220, 255))
# arcs
for (x, y) in [(2, 1), (14, 2), (1, 14), (15, 13), (5, 5), (11, 11)]:
    d.point((x, y), fill=(200, 230, 255, 255))
d.line([3, 1, 5, 3], fill=(180, 220, 255, 255))
d.line([13, 14, 11, 12], fill=(180, 220, 255, 255))
save_block("lightning_node", img)

# --- screaming_soul: black with screaming face ---
img = Image.new("RGBA", (S, S), (10, 8, 12, 255))
d = ImageDraw.Draw(img)
random.seed(17)
for _ in range(50):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(15, 35)
    d.point((x, y), fill=(v, v // 2, v + 5, 255))
# eyes (hollow glowing)
circle(d, 5, 6, 1, (220, 220, 240, 255))
circle(d, 10, 6, 1, (220, 220, 240, 255))
d.point((5, 6), fill=(50, 100, 180, 255))
d.point((10, 6), fill=(50, 100, 180, 255))
# screaming mouth
d.ellipse([6, 9, 9, 14], fill=(0, 0, 0, 255), outline=(120, 120, 160, 255))
d.line([7, 11, 8, 11], fill=(40, 40, 60, 255))
# wisps
d.point((2, 2), fill=(120, 120, 180, 200))
d.point((14, 3), fill=(120, 120, 180, 200))
d.point((13, 14), fill=(120, 120, 180, 200))
save_block("screaming_soul", img)

# --- magnetic_pylon: purple metallic with rings ---
img = Image.new("RGBA", (S, S), (60, 30, 80, 255))
d = ImageDraw.Draw(img)
random.seed(23)
for _ in range(60):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(40, 90)
    d.point((x, y), fill=(v + 20, v // 2, v + 30, 255))
# rings
for y in [2, 7, 13]:
    d.line([1, y, 14, y], fill=(160, 80, 220, 255))
    d.line([1, y + 1, 14, y + 1], fill=(100, 50, 160, 255))
# central column
d.rectangle([7, 0, 8, 15], fill=(40, 20, 60, 255))
# pole highlight
d.line([7, 0, 7, 15], fill=(120, 70, 180, 255))
# magnetic glow points
d.point((3, 4), fill=(220, 180, 255, 255))
d.point((12, 4), fill=(220, 180, 255, 255))
d.point((3, 9), fill=(220, 180, 255, 255))
d.point((12, 9), fill=(220, 180, 255, 255))
save_block("magnetic_pylon", img)

print("OK T9")
