"""Textures for 4 staves/swords + 4 attacking blood blocks."""
from PIL import Image, ImageDraw
import os, math, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
BLOCK = os.path.join(BASE, "block")
os.makedirs(ITEM, exist_ok=True)
os.makedirs(BLOCK, exist_ok=True)
S = 16

def img(): return Image.new("RGBA", (S, S), (0, 0, 0, 0))
def save_item(n, im): im.save(os.path.join(ITEM, n + ".png"))
def save_block(n, im): im.save(os.path.join(BLOCK, n + ".png"))

# ============================================================ STAVES (handheld) — diagonal layout

# --- thorn_staff: brown wood with red thorn cluster top ---
im = img(); d = ImageDraw.Draw(im)
# shaft (handheld diagonal: bottom-left to top-right)
d.line([2, 14, 11, 5], fill=(80, 50, 25, 255))
d.line([3, 14, 12, 5], fill=(50, 30, 15, 255))
# thorn cluster
d.ellipse([9, 2, 15, 8], fill=(60, 30, 10, 255))
d.point((11, 3), fill=(180, 40, 40, 255))
d.point((13, 4), fill=(180, 40, 40, 255))
d.point((10, 6), fill=(180, 40, 40, 255))
d.point((14, 6), fill=(180, 40, 40, 255))
d.point((12, 7), fill=(220, 60, 80, 255))
# leaf
d.point((4, 13), fill=(40, 80, 30, 255))
save_item("thorn_staff", im)

# --- lightning_staff: copper rod with yellow tip and arc ---
im = img(); d = ImageDraw.Draw(im)
d.line([2, 14, 11, 5], fill=(170, 100, 40, 255))
d.line([3, 14, 12, 5], fill=(110, 65, 25, 255))
# top orb
d.ellipse([10, 2, 15, 7], fill=(255, 220, 80, 255), outline=(120, 80, 20, 255))
d.point((12, 4), fill=(255, 255, 200, 255))
# arcs
d.line([11, 7, 13, 9], fill=(220, 240, 255, 255))
d.line([14, 5, 15, 7], fill=(220, 240, 255, 255))
d.line([9, 3, 11, 5], fill=(220, 240, 255, 255))
save_item("lightning_staff", im)

# --- soul_scream_sword: dark blade with cyan core ---
im = img(); d = ImageDraw.Draw(im)
# blade
d.line([2, 13, 12, 3], fill=(60, 60, 80, 255), width=1)
d.line([3, 13, 13, 3], fill=(30, 30, 50, 255))
d.line([2, 12, 11, 3], fill=(120, 200, 220, 255))
# soul flame on blade
d.point((6, 9), fill=(255, 255, 220, 255))
d.point((9, 6), fill=(255, 255, 220, 255))
d.point((7, 7), fill=(180, 240, 255, 255))
# hilt
d.rectangle([1, 13, 4, 14], fill=(20, 20, 30, 255))
d.rectangle([2, 14, 3, 15], fill=(60, 30, 10, 255))
save_item("soul_scream_sword", im)

# --- magnetic_wand: purple rod with twin-pole tip ---
im = img(); d = ImageDraw.Draw(im)
d.line([2, 14, 11, 5], fill=(70, 40, 90, 255))
d.line([3, 14, 12, 5], fill=(40, 20, 60, 255))
# fork
d.line([10, 5, 13, 2], fill=(160, 80, 220, 255))
d.line([12, 5, 14, 6], fill=(160, 80, 220, 255))
# poles (red and blue)
d.ellipse([12, 1, 14, 3], fill=(220, 60, 80, 255))
d.ellipse([13, 5, 15, 7], fill=(80, 140, 240, 255))
# spark
d.point((11, 3), fill=(255, 240, 220, 255))
save_item("magnetic_wand", im)

# ============================================================ BLOCKS

# --- hemorrhage_spike: dark red with bone shards ---
im = Image.new("RGBA", (S, S), (60, 10, 18, 255))
d = ImageDraw.Draw(im)
random.seed(101)
for _ in range(80):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(40, 100)
    d.point((x, y), fill=(v + 30, v // 4, v // 4, 255))
# bone shards (white spikes pointing up)
for x in [3, 7, 11]:
    d.line([x, 14, x, 9], fill=(220, 210, 190, 255))
    d.point((x, 8), fill=(255, 240, 220, 255))
# blood pools
d.point((4, 4), fill=(180, 30, 40, 255))
d.point((10, 5), fill=(180, 30, 40, 255))
d.point((6, 11), fill=(220, 50, 60, 255))
save_block("hemorrhage_spike", im)

# --- sanguine_snare: dark red flesh with sticky tendrils ---
im = Image.new("RGBA", (S, S), (90, 25, 30, 255))
d = ImageDraw.Draw(im)
random.seed(103)
for _ in range(70):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(30, 80)
    d.point((x, y), fill=(v + 60, v // 3, v // 3, 255))
# tendrils (curving lines)
for sx, sy in [(2, 2), (13, 3), (4, 13), (12, 12)]:
    d.line([sx, sy, 8, 8], fill=(40, 8, 12, 255))
    d.point((sx, sy), fill=(180, 40, 50, 255))
# central pit
d.ellipse([6, 6, 9, 9], fill=(20, 5, 8, 255))
d.point((7, 7), fill=(220, 60, 80, 255))
save_block("sanguine_snare", im)

# --- veil_of_madness: deep purple with cyan eye runes ---
im = Image.new("RGBA", (S, S), (35, 15, 60, 255))
d = ImageDraw.Draw(im)
random.seed(107)
for _ in range(60):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(20, 60)
    d.point((x, y), fill=(v + 10, v // 3, v + 30, 255))
# floating eye runes
for cx, cy in [(5, 5), (10, 5), (5, 10), (10, 10)]:
    d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(180, 240, 255, 255))
    d.point((cx, cy), fill=(0, 0, 0, 255))
# central rune
d.line([7, 8, 9, 8], fill=(200, 100, 240, 255))
d.line([8, 7, 8, 9], fill=(200, 100, 240, 255))
# soul wisps
for _ in range(6):
    x, y = random.randint(0, 15), random.randint(0, 15)
    d.point((x, y), fill=(220, 240, 255, 200))
save_block("veil_of_madness", im)

# --- phantom_portal: black with swirling purple/cyan ---
im = Image.new("RGBA", (S, S), (8, 4, 18, 255))
d = ImageDraw.Draw(im)
# spiral
for i in range(28):
    a = i * 0.4
    r = i * 0.25
    x = int(8 + math.cos(a) * r)
    y = int(8 + math.sin(a) * r)
    if 0 <= x < 16 and 0 <= y < 16:
        c = (140 + (i % 3) * 30, 80, 220 - (i % 3) * 20, 255)
        d.point((x, y), fill=c)
# bright core
d.ellipse([7, 7, 9, 9], fill=(220, 200, 255, 255))
d.point((8, 8), fill=(255, 255, 255, 255))
# corner sparkles
d.point((1, 1), fill=(180, 100, 220, 255))
d.point((14, 2), fill=(180, 100, 220, 255))
d.point((2, 14), fill=(180, 100, 220, 255))
d.point((14, 13), fill=(180, 100, 220, 255))
save_block("phantom_portal", im)

print("OK attack pack textures")
