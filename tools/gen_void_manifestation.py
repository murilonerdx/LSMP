# VFX textures for the Void Manifestation entity (Alex's Caves Underzealot shadow).
# REWORK: eyes look like REAL eyes (almond shape, iris, pupil — NOT gears), smoke is
# DARK and DENSE (stable mass), tentacles are a 4-frame animation reaching out.
#
#   entity/void_manifestation/smoke.png        — dense black smoke puff (64x64)
#   entity/void_manifestation/eye.png          — realistic red almond eye (64x64)
#   entity/void_manifestation/tentacle_0..3.png— animated black tendril (32x64)
#   item/void_eye.png                          — item icon: red eye in dark orb (16x16)
from PIL import Image
import math, os

ENT = "src/main/resources/assets/liberthia/textures/entity/void_manifestation"
ITEM = "src/main/resources/assets/liberthia/textures/item"
os.makedirs(ENT, exist_ok=True)


# ---------------- SMOKE: dense black puff (stable mass, not a wispy particle) ------
def gen_smoke(size=64):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    for y in range(size):
        for x in range(size):
            dx, dy = (x + 0.5 - c) / c, (y + 0.5 - c) / c
            d = math.sqrt(dx * dx + dy * dy)
            if d >= 1.0:
                continue
            # flat-ish dense core (stays opaque most of the radius), quick soft edge
            if d < 0.62:
                a = 1.0
            else:
                a = max(0.0, 1.0 - (d - 0.62) / 0.38)
                a = a ** 1.3
            # ragged billowing rim
            ang = math.atan2(dy, dx)
            wob = 0.10 * math.sin(ang * 6) + 0.06 * math.sin(ang * 11 + 2.0)
            edge = 0.80 + wob
            if d > edge:
                a *= max(0.0, 1.0 - (d - edge) / (1.0 - edge))
            alpha = int(248 * a)            # DENSE
            # truly dark — near black, barely-there cool tint, slightly lighter core for volume
            base = 6 + int(14 * (1.0 - d))  # 6..20
            px[x, y] = (base, base, base + 4, alpha)
    img.save(f"{ENT}/smoke.png")
    print("wrote smoke.png (dense)")


# ---------------- EYE: realistic almond eye, red iris, vertical pupil --------------
def gen_eye(size=64):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    # almond shape: |x| up to half-width; lid curves meet at corners.
    half_w = size * 0.46          # horizontal half-extent (corners)
    open_h = size * 0.30          # vertical half-extent at center (eye openness)
    iris_r = size * 0.20          # iris radius
    pupil_w = size * 0.055        # pupil half-width (vertical slit)
    pupil_h = size * 0.16
    SCLERA = (60, 6, 6)           # dark red-black "white" of the eye
    SCLERA_HI = (120, 16, 16)
    IRIS = (235, 32, 28)          # bright red iris
    IRIS_RING = (150, 12, 12)     # darker iris rim
    IRIS_HOT = (255, 120, 80)     # inner glow
    PUPIL = (4, 0, 0)
    LID = (90, 10, 10)            # eyelid line

    for y in range(size):
        for x in range(size):
            nx = (x + 0.5 - c)
            ny = (y + 0.5 - c)
            # almond boundary: at horizontal position nx, the lid half-height shrinks
            # toward the corners (parabola). |ny| must be <= lidH to be inside the eye.
            if abs(nx) > half_w:
                continue
            t = nx / half_w                       # -1..1
            lidH = open_h * (1.0 - t * t)          # 0 at corners, max at center
            if lidH < 0.5:
                continue
            if abs(ny) <= lidH:
                d_iris = math.hypot(nx, ny)
                # sclera base with subtle vertical shading
                col = SCLERA if abs(ny) > lidH * 0.6 else SCLERA_HI
                # iris
                if d_iris <= iris_r:
                    if d_iris >= iris_r - 2.5:
                        col = IRIS_RING
                    elif d_iris <= iris_r * 0.45:
                        col = IRIS_HOT
                    else:
                        col = IRIS
                # vertical slit pupil (almond too)
                if abs(nx) <= pupil_w * (1.0 - (abs(ny) / (pupil_h + 0.001)) ** 2) and abs(ny) <= pupil_h:
                    col = PUPIL
                # lid edge (near the almond boundary) → dark outline
                if abs(ny) >= lidH - 2.0:
                    col = LID
                px[x, y] = (col[0], col[1], col[2], 255)
    # tiny specular glint on the iris (upper-left)
    gx, gy = int(c - iris_r * 0.4), int(c - iris_r * 0.4)
    for oy in range(2):
        for ox in range(2):
            if 0 <= gx + ox < size and 0 <= gy + oy < size and px[gx + ox, gy + oy][3] > 0:
                px[gx + ox, gy + oy] = (255, 230, 210, 255)
    img.save(f"{ENT}/eye.png")
    print("wrote eye.png (almond)")


# ---------------- TENTACLE: 4-frame animation reaching out -------------------------
def gen_tentacle_frame(idx, w=32, h=64):
    """idx 0..3 = growth/wiggle phase. Black tendril, thick root -> tapered tip."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    cx = w / 2.0
    grow = 0.55 + 0.15 * idx           # how far up it has extended (0.55..1.0)
    phase = idx * 1.4
    for y in range(h):
        t = y / h                       # 0 top(tip) .. 1 bottom(root)
        if (1.0 - t) > grow:            # above the current tip → not drawn yet
            continue
        # taper: thin near tip, thick at root
        local = (1.0 - t) / grow        # 0 at tip .. 1 at root
        half = (w * 0.06) + (w * 0.34) * (1.0 - local)
        # serpentine wiggle, animated by frame
        wob = math.sin(t * 7 + phase) * (w * 0.14) * (0.4 + 0.6 * t)
        for x in range(w):
            dx = x + 0.5 - (cx + wob)
            if abs(dx) <= half:
                edge = 1.0 - (abs(dx) / max(0.5, half))
                a = int(245 * (edge ** 0.7))
                shade = 6 + int(10 * (1 - edge))   # dark, slightly lit rim
                px[x, y] = (shade, shade, shade + 3, a)
    img.save(f"{ENT}/tentacle_{idx}.png")
    print(f"wrote tentacle_{idx}.png")


# ---------------- ITEM ICON: red almond eye in dark orb (16x16) --------------------
def gen_item(size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    R = size * 0.47
    half_w = size * 0.46
    open_h = size * 0.26
    iris_r = size * 0.17
    # dark orb backing
    for y in range(size):
        for x in range(size):
            if math.hypot(x + 0.5 - c, y + 0.5 - c) <= R:
                px[x, y] = (12, 4, 8, 255)
    # almond eye
    for y in range(size):
        for x in range(size):
            nx, ny = x + 0.5 - c, y + 0.5 - c
            if abs(nx) > half_w:
                continue
            t = nx / half_w
            lidH = open_h * (1.0 - t * t)
            if lidH < 0.5 or abs(ny) > lidH:
                continue
            d = math.hypot(nx, ny)
            if d <= iris_r:
                px[x, y] = (235, 32, 28, 255)
            else:
                px[x, y] = (70, 8, 8, 255)
    # pupil (center column)
    px[int(c), int(c)] = (0, 0, 0, 255)
    px[int(c), int(c) - 1] = (0, 0, 0, 255)
    px[int(c), int(c) + 1] = (0, 0, 0, 255)
    img.save(f"{ITEM}/void_eye.png")
    print("wrote void_eye.png")


gen_smoke()
gen_eye()
for i in range(4):
    gen_tentacle_frame(i)
gen_item()
print("done: void manifestation VFX rework (real eyes + dense smoke + animated tentacles)")
