# Generates the 13 missing mob_effect icons (18x18, Minecraft effect-icon size) for
# Liberthia synergy/status effects that showed up as "no texture" + bugged name.
# Each icon = a themed glyph on a soft rounded background tinted to the effect's color.
from PIL import Image
import math, os

OUT = "src/main/resources/assets/liberthia/textures/mob_effect"
os.makedirs(OUT, exist_ok=True)
S = 18  # vanilla mob effect icons are 18x18


def base(bg):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = img.load()
    # soft rounded square background
    for y in range(S):
        for x in range(S):
            # rounded corners
            cx = min(x, S - 1 - x); cy = min(y, S - 1 - y)
            if cx + cy < 2:
                continue
            edge = (x == 0 or x == S - 1 or y == 0 or y == S - 1)
            px[x, y] = tuple(int(c * 0.55) for c in bg[:3]) + (255,) if edge else bg
    return img, px


def put(px, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        px[x, y] = c


def line(px, x0, y0, x1, y1, c):
    # simple Bresenham
    dx = abs(x1 - x0); dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        put(px, x0, y0, c)
        if x0 == x1 and y0 == y1: break
        e2 = 2 * err
        if e2 > -dy: err -= dy; x0 += sx
        if e2 < dx: err += dx; y0 += sy


def droplet(px, cx, cy, c, hi):
    # teardrop blood/water shape
    for y in range(-4, 5):
        w = int(2.6 * (1 - abs(y) / 6.0)) if y >= -2 else int((y + 4) * 1.2)
        for x in range(-w, w + 1):
            put(px, cx + x, cy + y, c)
    put(px, cx - 1, cy - 1, hi)


def save(img, name):
    img.save(f"{OUT}/{name}.png")
    print("wrote", name)


W = (245, 245, 250, 255)   # white glyph
K = (20, 12, 16, 255)      # dark glyph

# ---- CHILLED: light-blue, snowflake ----
img, px = base((90, 170, 220, 255))
cx = cy = 9
for ang in range(0, 360, 60):
    a = math.radians(ang)
    line(px, cx, cy, cx + int(math.cos(a) * 5), cy + int(math.sin(a) * 5), W)
put(px, cx, cy, W)
save(img, "chilled")

# ---- FROSTBITE: deeper ice-blue, jagged shard ----
img, px = base((70, 140, 200, 255))
for (x, y) in [(9,3),(8,5),(10,5),(7,7),(9,7),(11,7),(8,9),(10,9),(9,11),(9,13)]:
    put(px, x, y, W)
line(px, 6, 9, 12, 9, (200, 235, 255, 255))
save(img, "frostbite")

# ---- BURNING_MARK: orange, small flame ----
img, px = base((200, 90, 40, 255))
for y in range(4, 14):
    w = int((y - 4) * 0.6) + 1
    for x in range(-w, w + 1):
        put(px, 9 + x, y, (255, 180, 60, 255))
for y in range(7, 14):
    put(px, 9, y, (255, 240, 150, 255))
put(px, 9, 4, (255, 230, 120, 255))
save(img, "burning_mark")

# ---- IMMOLATE: hot red-orange, big flame ----
img, px = base((200, 50, 20, 255))
for y in range(3, 15):
    w = int((y - 3) * 0.7) + 1
    for x in range(-w, w + 1):
        put(px, 9 + x, y, (255, 120, 30, 255))
for y in range(6, 15):
    put(px, 9, y, (255, 220, 90, 255))
    put(px, 8, y + 1 if y < 14 else y, (255, 160, 40, 255))
save(img, "immolate")

# ---- BLEED: dark red, blood droplet ----
img, px = base((150, 30, 50, 255))
droplet(px, 9, 8, (200, 20, 40, 255), (255, 120, 130, 255))
save(img, "bleed")

# ---- HEMORRHAGE: crimson, double droplet ----
img, px = base((170, 20, 50, 255))
droplet(px, 6, 7, (210, 20, 40, 255), (255, 130, 140, 255))
droplet(px, 12, 9, (180, 15, 35, 255), (255, 110, 120, 255))
save(img, "hemorrhage")

# ---- STATIC_CHARGE: yellow, lightning bolt ----
img, px = base((220, 200, 60, 255))
for (x, y) in [(11,3),(10,4),(9,5),(8,6),(10,6),(7,7),(9,7),(8,8),(7,9),(6,10),(8,9)]:
    put(px, x, y, (255, 255, 150, 255))
line(px, 11, 3, 7, 8, (255, 255, 180, 255))
line(px, 9, 7, 6, 13, (255, 255, 180, 255))
save(img, "static_charge")

# ---- STORM_MARK: pale-blue, lightning in cloud ----
img, px = base((150, 160, 210, 255))
# cloud
for x in range(5, 13):
    put(px, x, 6, (220, 225, 235, 255)); put(px, x, 7, (200, 205, 220, 255))
put(px, 4, 7, (210, 215, 230, 255)); put(px, 13, 7, (210, 215, 230, 255))
# bolt
for (x, y) in [(9,8),(8,10),(10,10),(9,12),(8,13)]:
    put(px, x, y, (255, 255, 120, 255))
save(img, "storm_mark")

# ---- HEARTSTOP: dark purple, cracked heart ----
img, px = base((60, 20, 60, 255))
heart = [(7,5),(8,4),(11,4),(12,5),(6,6),(13,6),(6,7),(13,7),(7,8),(12,8),(8,9),(11,9),(9,10),(10,10),(9,11)]
for (x, y) in heart:
    put(px, x, y, (200, 40, 90, 255))
# crack
line(px, 9, 5, 10, 11, K)
save(img, "heartstop")

# ---- ASCENSION: gold/white, upward wings/arrow ----
img, px = base((210, 180, 90, 255))
for i in range(5):
    put(px, 9, 4 + i, (255, 240, 170, 255))
put(px, 8, 5, (255, 240, 170, 255)); put(px, 10, 5, (255, 240, 170, 255))
put(px, 7, 6, (255, 240, 170, 255)); put(px, 11, 6, (255, 240, 170, 255))
# wings
line(px, 6, 9, 3, 12, W); line(px, 12, 9, 15, 12, W)
line(px, 6, 11, 4, 13, (255,240,200,255)); line(px, 12, 11, 14, 13, (255,240,200,255))
save(img, "ascension")

# ---- MADNESS: dark purple, spiral ----
img, px = base((70, 20, 80, 255))
for t in range(0, 540, 12):
    a = math.radians(t); r = t / 90.0
    put(px, int(9 + math.cos(a) * r), int(9 + math.sin(a) * r), (210, 120, 230, 255))
save(img, "madness")

# ---- OBSESSION: dark violet, eye ----
img, px = base((50, 16, 60, 255))
for x in range(4, 14):
    dy = int(2.4 * (1 - ((x - 9) / 5.0) ** 2))
    for y in range(-dy, dy + 1):
        put(px, x, 9 + y, (220, 210, 230, 255))
for x in range(7, 12):
    for y in range(-2, 3):
        if (x-9)**2 + y*y <= 4: put(px, x, 9 + y, (180, 40, 60, 255))
put(px, 9, 9, K)
save(img, "obsession")

# ---- DIMENSIONAL_INFECTION: void purple, portal swirl ----
img, px = base((40, 16, 70, 255))
for t in range(0, 360, 10):
    a = math.radians(t); r = 5 - (t / 120.0)
    put(px, int(9 + math.cos(a) * r), int(9 + math.sin(a) * r), (160, 90, 230, 255))
put(px, 9, 9, (220, 180, 255, 255))
save(img, "dimensional_infection")

print("done: 13 effect icons")
