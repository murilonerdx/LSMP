"""Textures for Occultism-port additions: Blood Chalk + 4 chalk glyphs +
Blood Fire + Blood Torch."""
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

# --- blood_chalk: a small reddish chalk stick ---
im = img(); d = ImageDraw.Draw(im)
# stick body diagonal
d.line([4, 12, 11, 5], fill=(180, 30, 40, 255), width=2)
d.line([5, 12, 12, 5], fill=(120, 18, 25, 255))
# tip dust
d.point((11, 4), fill=(220, 60, 80, 255))
d.point((10, 4), fill=(220, 60, 80, 255))
d.point((12, 5), fill=(220, 60, 80, 255))
# end nub
d.point((4, 13), fill=(80, 10, 18, 255))
d.point((5, 13), fill=(60, 5, 10, 255))
save_item("blood_chalk", im)

# --- chalk_glyph_0..3: red glyphs on transparent bg ---
def glyph(seed, kind):
    """One of 4 distinct glyph shapes — pentagram, ouroboros, cross, sigil."""
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    color = (170, 25, 35, 230)
    accent = (220, 50, 60, 255)
    if kind == 0:
        # 5-pointed star
        cx, cy, r = 8, 8, 5.5
        pts = []
        for i in range(10):
            a = math.pi/2 + i * math.pi/5
            rr = r if i % 2 == 0 else r * 0.45
            pts.append((cx + math.cos(a) * rr, cy - math.sin(a) * rr))
        d.line(pts + [pts[0]], fill=color, width=1)
        d.ellipse([cx-1, cy-1, cx+1, cy+1], fill=accent)
    elif kind == 1:
        # circle with cross inside
        d.ellipse([2, 2, 13, 13], outline=color)
        d.line([8, 3, 8, 12], fill=color)
        d.line([3, 8, 12, 8], fill=color)
        d.point((8, 8), fill=accent)
    elif kind == 2:
        # triangle with eye
        d.polygon([(8, 2), (13, 13), (3, 13)], outline=color)
        d.ellipse([6, 8, 9, 11], outline=color)
        d.point((7, 9), fill=accent)
        d.point((8, 9), fill=accent)
    else:
        # spiral runes
        for i in range(20):
            a = i * 0.5
            rr = i * 0.3
            x = 8 + math.cos(a) * rr
            y = 8 + math.sin(a) * rr
            if 1 <= x < 15 and 1 <= y < 15:
                d.point((int(x), int(y)), fill=color)
        d.point((8, 8), fill=accent)
    return im

for i in range(4):
    save_block(f"chalk_glyph_{i}", glyph(i, i))

# --- blood_fire: red flame texture (16x16, will tile on cube_all) ---
im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
random.seed(13)
# vertical flame stripes
for x in range(0, 16, 2):
    h = random.randint(8, 14)
    for y in range(16 - h, 16):
        # gradient: dark red at bottom, bright orange-red at top
        t = (y - (16 - h)) / max(1, h)
        r = int(140 + t * 100)
        g = int(20 + t * 60)
        b = int(20 + t * 30)
        d.point((x, y), fill=(r, g, b, 255))
        if x + 1 < 16:
            d.point((x + 1, y), fill=(r - 20, g - 10, b, 255))
# embers at top
for _ in range(8):
    x, y = random.randint(0, 15), random.randint(0, 5)
    d.point((x, y), fill=(255, 200, 80, 255))
save_block("blood_fire", im)

# --- blood_torch: torch with red flame on top ---
im = img(); d = ImageDraw.Draw(im)
# stick (vertical, lower portion) — vanilla torch model expects column at x=7..8 across full height
d.rectangle([7, 8, 8, 13], fill=(110, 70, 30, 255))
d.rectangle([7, 13, 8, 15], fill=(80, 50, 20, 255))
# flame at top
d.point((7, 6), fill=(180, 30, 40, 255))
d.point((8, 6), fill=(220, 50, 60, 255))
d.point((7, 5), fill=(220, 50, 60, 255))
d.point((8, 5), fill=(255, 100, 80, 255))
d.point((7, 4), fill=(255, 80, 60, 255))
d.point((8, 4), fill=(255, 200, 100, 255))
d.point((7, 7), fill=(140, 20, 30, 255))
d.point((8, 7), fill=(140, 20, 30, 255))
save_block("blood_torch", im)

print("OK occultism port textures")
