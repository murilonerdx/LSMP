# Block textures (16x16) for two machines, themed to what they do:
#   THREAD LOOM (Tear de Threads) — weaves magic threads into spell modifiers.
#     top  = loom frame with colored warp threads + a shuttle
#     side = dark wood frame with a thread spool + hanging threads
#     bottom = plain dark wood
#   ORB INFUSER — infuses arcane orbs with energy.
#     top  = metal ring socket with a glowing cyan orb in the center + runes
#     side = arcane dark-metal panel with vertical energy conduit + rune
#     bottom = plain dark metal
from PIL import Image
import math, os

OUT = "src/main/resources/assets/liberthia/textures/block"
os.makedirs(OUT, exist_ok=True)


def img():
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


# ============ THREAD LOOM ============
WOOD = (92, 64, 38, 255)
WOOD_HI = (120, 88, 54, 255)
WOOD_LO = (60, 40, 24, 255)
FRAME = (140, 104, 60, 255)
THREADS = [(210, 70, 70, 255), (70, 150, 210, 255), (210, 200, 80, 255),
           (150, 90, 200, 255), (90, 200, 130, 255)]
SHUTTLE = (200, 180, 140, 255)
SPOOL = (160, 120, 70, 255)

# -- top: warp threads stretched across a frame + shuttle --
im = img(); px = im.load(); fill(px, WOOD); bevel(px, WOOD_HI, WOOD_LO)
# top & bottom beams (frame bars)
for x in range(2, 14):
    put(px, x, 2, FRAME); put(px, x, 13, FRAME)
# vertical warp threads (colored), columns 3..12 spaced 2
ci = 0
for x in range(3, 13, 2):
    col = THREADS[ci % len(THREADS)]; ci += 1
    for y in range(3, 13):
        put(px, x, y, col)
# shuttle (horizontal weaving tool) crossing the middle
for x in range(3, 13):
    put(px, x, 8, SHUTTLE)
put(px, 2, 8, WOOD_LO); put(px, 13, 8, WOOD_LO)
im.save(f"{OUT}/thread_loom_top.png"); print("thread_loom_top")

# -- side: wood panel + a thread spool + hanging threads --
im = img(); px = im.load(); fill(px, WOOD); bevel(px, WOOD_HI, WOOD_LO)
# wood grain lines
for y in (4, 7, 11):
    for x in range(2, 14):
        if (x + y) % 3 != 0:
            put(px, x, y, WOOD_LO)
# spool (top-left): a wound bobbin
for y in range(3, 7):
    for x in range(3, 7):
        put(px, x, y, SPOOL)
put(px, 3, 3, WOOD_LO); put(px, 6, 6, WOOD_LO)
for y in range(3, 7):
    put(px, 4, y, THREADS[0]); put(px, 5, y, THREADS[1])
# hanging threads from a top bar
for x in range(2, 14):
    put(px, x, 2, FRAME)
ci = 0
for x in range(8, 14, 2):
    col = THREADS[ci % len(THREADS)]; ci += 1
    for y in range(3, 13):
        put(px, x, y, col)
im.save(f"{OUT}/thread_loom_side.png"); print("thread_loom_side")

# -- bottom: plain dark wood --
im = img(); px = im.load(); fill(px, WOOD_LO); bevel(px, WOOD, (44, 28, 16, 255))
for y in (5, 10):
    for x in range(2, 14):
        put(px, x, y, (50, 32, 18, 255))
im.save(f"{OUT}/thread_loom_bottom.png"); print("thread_loom_bottom")


# ============ ORB INFUSER ============
METAL = (54, 58, 70, 255)
METAL_HI = (86, 92, 108, 255)
METAL_LO = (32, 34, 44, 255)
RING = (150, 158, 178, 255)
RUNE = (90, 210, 230, 255)
ORB_DK = (20, 70, 90, 255)
ORB = (70, 200, 235, 255)
ORB_HOT = (200, 255, 255, 255)
CONDUIT = (80, 200, 230, 255)


def disc(px, cx, cy, r, c):
    for y in range(16):
        for x in range(16):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                put(px, x, y, c)


def ring(px, cx, cy, r, c):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r - 1 <= d <= r:
                put(px, x, y, c)


# -- top: metal ring socket + glowing orb + 4 corner runes --
im = img(); px = im.load(); fill(px, METAL); bevel(px, METAL_HI, METAL_LO)
ring(px, 8, 8, 6.0, RING)
ring(px, 8, 8, 5.0, METAL_LO)
disc(px, 8, 8, 3.4, ORB_DK)
disc(px, 8, 8, 2.4, ORB)
disc(px, 8, 8, 1.0, ORB_HOT)
put(px, 7, 7, ORB_HOT)
# corner runes (cyan dots)
for (rx, ry) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
    put(px, rx, ry, RUNE)
im.save(f"{OUT}/orb_infuser_top.png"); print("orb_infuser_top")

# -- side: dark metal panel + vertical energy conduit + rune --
im = img(); px = im.load(); fill(px, METAL); bevel(px, METAL_HI, METAL_LO)
# rivets
for (rx, ry) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
    put(px, rx, ry, RING)
# central vertical conduit glowing
for y in range(3, 13):
    put(px, 7, y, CONDUIT); put(px, 8, y, ORB_HOT if y % 2 == 0 else CONDUIT)
# small rune mark
put(px, 4, 6, RUNE); put(px, 4, 7, RUNE); put(px, 5, 7, RUNE)
put(px, 11, 9, RUNE); put(px, 11, 10, RUNE); put(px, 10, 10, RUNE)
im.save(f"{OUT}/orb_infuser_side.png"); print("orb_infuser_side")

# -- bottom: plain dark metal --
im = img(); px = im.load(); fill(px, METAL_LO); bevel(px, METAL, (22, 24, 30, 255))
for (rx, ry) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
    put(px, rx, ry, (40, 42, 52, 255))
im.save(f"{OUT}/orb_infuser_bottom.png"); print("orb_infuser_bottom")

print("done: thread_loom + orb_infuser block textures")
