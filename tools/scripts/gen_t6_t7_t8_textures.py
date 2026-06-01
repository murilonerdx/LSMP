"""Generate textures for T6 (EvilCraft ports), T7 (attacking blocks) and T8 (vanilla effect throwables)."""
from PIL import Image, ImageDraw
import os, math, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
BLOCK = os.path.join(BASE, "block")
os.makedirs(ITEM, exist_ok=True)
os.makedirs(BLOCK, exist_ok=True)

S = 16

def new_img():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))

def save_item(name, img):
    img.save(os.path.join(ITEM, name + ".png"))

def save_block(name, img):
    img.save(os.path.join(BLOCK, name + ".png"))

def circle(d, cx, cy, r, fill, outline=None):
    d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=fill, outline=outline)

# --- blood_teleport_pearl: dark red pearl with purple swirl ---
img = new_img(); d = ImageDraw.Draw(img)
circle(d, 8, 8, 6, (90, 0, 10, 255), outline=(40, 0, 5, 255))
circle(d, 6, 6, 3, (180, 30, 50, 255))
circle(d, 10, 10, 2, (140, 30, 200, 255))
circle(d, 5, 11, 1, (255, 200, 220, 255))
save_item("blood_teleport_pearl", img)

# --- tainted_apple: green-purple corrupted apple ---
img = new_img(); d = ImageDraw.Draw(img)
d.ellipse([3, 4, 13, 14], fill=(80, 20, 90, 255), outline=(30, 5, 40, 255))
d.ellipse([5, 6, 9, 10], fill=(140, 50, 160, 255))
d.rectangle([8, 2, 9, 5], fill=(60, 30, 10, 255))
d.line([7, 3, 11, 2], fill=(40, 100, 30, 255))
d.point((6, 8), fill=(220, 200, 230, 255))
save_item("tainted_apple", img)

# --- purging_pendant: gold chain with white gem ---
img = new_img(); d = ImageDraw.Draw(img)
for i, x in enumerate([4, 6, 8, 10, 12]):
    d.point((x, 2 + (i % 2)), fill=(220, 180, 60, 255))
d.polygon([(8, 5), (12, 9), (8, 13), (4, 9)], fill=(240, 220, 100, 255), outline=(120, 90, 20, 255))
d.polygon([(8, 7), (10, 9), (8, 11), (6, 9)], fill=(255, 255, 255, 255))
d.point((8, 9), fill=(180, 220, 255, 255))
save_item("purging_pendant", img)

# --- veiling_orb: black orb with purple smoke aura ---
img = new_img(); d = ImageDraw.Draw(img)
for r in range(7, 4, -1):
    a = 80 + (8 - r) * 30
    d.ellipse([8 - r, 8 - r, 8 + r, 8 + r], fill=(40, 10, 60, a))
circle(d, 8, 8, 4, (10, 0, 20, 255), outline=(60, 30, 90, 255))
d.point((6, 6), fill=(180, 100, 220, 255))
save_item("veiling_orb", img)

# --- mind_splinter_dart: cyan crystalline dart ---
img = new_img(); d = ImageDraw.Draw(img)
d.polygon([(8, 1), (11, 7), (9, 14), (7, 14), (5, 7)], fill=(120, 220, 240, 255), outline=(40, 100, 140, 255))
d.line([8, 3, 8, 12], fill=(220, 250, 255, 255))
d.point((6, 8), fill=(180, 240, 255, 255))
d.point((10, 9), fill=(180, 240, 255, 255))
save_item("mind_splinter_dart", img)

# --- WITHERING_EYE block: dark with glowing eye in centre ---
def withering_eye(age):
    img = Image.new("RGBA", (S, S), (15, 8, 20, 255))
    d = ImageDraw.Draw(img)
    # base noise
    random.seed(7 + age)
    for _ in range(40):
        x, y = random.randint(0, 15), random.randint(0, 15)
        v = random.randint(20, 50)
        d.point((x, y), fill=(v, v // 2, v + 10, 255))
    # eye
    glow = (220, 60, 80) if age >= 2 else (140, 30, 50)
    circle(d, 8, 8, 5, (5, 0, 10, 255))
    circle(d, 8, 8, 4, glow + (255,))
    circle(d, 8, 8, 2, (255, 255, 220, 255))
    circle(d, 8, 8, 1, (0, 0, 0, 255))
    if age >= 1:
        for ang in range(0, 360, 45):
            rx = 8 + int(math.cos(math.radians(ang)) * 6)
            ry = 8 + int(math.sin(math.radians(ang)) * 6)
            d.point((rx, ry), fill=(180, 50, 70, 255))
    return img

# Use age=2 as the canonical block texture
save_block("withering_eye", withering_eye(2))

# --- VENOM_GEYSER block: green slimy with bubbling pores ---
img = Image.new("RGBA", (S, S), (40, 70, 30, 255))
d = ImageDraw.Draw(img)
random.seed(11)
for _ in range(60):
    x, y = random.randint(0, 15), random.randint(0, 15)
    g = random.randint(50, 110)
    d.point((x, y), fill=(20, g, 30, 255))
for cx, cy, r in [(4, 4, 2), (11, 5, 1), (5, 11, 1), (12, 12, 2), (8, 8, 1)]:
    circle(d, cx, cy, r, (140, 200, 60, 255), outline=(20, 60, 10, 255))
    d.point((cx, cy), fill=(220, 255, 180, 255))
save_block("venom_geyser", img)

# --- LIGHTNING_COIL block: copper coil with cyan electric arcs ---
img = Image.new("RGBA", (S, S), (60, 35, 20, 255))
d = ImageDraw.Draw(img)
# coil rings
for y in [2, 5, 8, 11, 14]:
    d.line([2, y, 13, y], fill=(180, 110, 50, 255))
    d.line([2, y + 1, 13, y + 1], fill=(120, 70, 30, 255))
# vertical band
d.line([7, 1, 7, 14], fill=(40, 20, 10, 255))
d.line([8, 1, 8, 14], fill=(40, 20, 10, 255))
# electric arcs
for (x, y) in [(3, 3), (12, 6), (4, 9), (11, 12), (8, 0), (8, 15)]:
    d.point((x, y), fill=(160, 230, 255, 255))
d.point((6, 7), fill=(220, 250, 255, 255))
d.point((9, 10), fill=(220, 250, 255, 255))
save_block("lightning_coil", img)

print("OK")
