# CUSTOM particle spritesheets for the Void Manifestation (NOT vanilla):
#   particle/void_flying_eye_0..3.png  — REALISTIC red almond eye, 4 look/blink frames
#   particle/void_shadow_wisp_0..3.png — DENSE black smoke puffs, 4 shapes (32x32)
#
# REWORK: eyes are almond-shaped real eyes (iris + vertical pupil + eyelid), NOT gears.
# Smoke is darker and denser so it reads as stable smoke, not scattered specks.
from PIL import Image
import math, os

PART = "src/main/resources/assets/liberthia/textures/particle"
os.makedirs(PART, exist_ok=True)

SCLERA = (60, 6, 6)
SCLERA_HI = (115, 14, 14)
IRIS = (235, 32, 28)
IRIS_RING = (150, 12, 12)
IRIS_HOT = (255, 120, 80)
PUPIL = (4, 0, 0)
LID = (90, 10, 10)


def eye_frame(idx, size=32):
    """Almond eye; frames: 0 center, 1 look-right, 2 look-left, 3 half-blink."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    half_w = size * 0.46
    base_open = size * 0.30
    blink = (idx == 3)
    open_h = base_open * (0.4 if blink else 1.0)
    iris_r = size * 0.19
    pupil_w = size * 0.05
    pupil_h = size * 0.15
    look = [(0, 0), (0.16, 0), (-0.16, 0), (0, 0)][idx]
    irx = c + look[0] * size
    iry = c + look[1] * size

    for y in range(size):
        for x in range(size):
            nx, ny = x + 0.5 - c, y + 0.5 - c
            if abs(nx) > half_w:
                continue
            t = nx / half_w
            lidH = open_h * (1.0 - t * t)        # almond: tall center, sharp corners
            if lidH < 0.5 or abs(ny) > lidH:
                continue
            col = SCLERA if abs(ny) > lidH * 0.6 else SCLERA_HI
            # iris (follows look offset)
            d_iris = math.hypot(x + 0.5 - irx, y + 0.5 - iry)
            if d_iris <= iris_r:
                if d_iris >= iris_r - 2.0:
                    col = IRIS_RING
                elif d_iris <= iris_r * 0.42:
                    col = IRIS_HOT
                else:
                    col = IRIS
                # vertical slit pupil inside iris
                if abs(x + 0.5 - irx) <= pupil_w and abs(y + 0.5 - iry) <= pupil_h:
                    col = PUPIL
            # eyelid outline near the almond boundary
            if abs(ny) >= lidH - 1.5:
                col = LID
            px[x, y] = (col[0], col[1], col[2], 255)
    # glint
    if not blink:
        gx, gy = int(irx - iris_r * 0.4), int(iry - iris_r * 0.4)
        if 0 <= gx < size and 0 <= gy < size and px[gx, gy][3] > 0:
            px[gx, gy] = (255, 230, 210, 255)
    img.save(f"{PART}/void_flying_eye_{idx}.png")
    print(f"wrote void_flying_eye_{idx}.png (almond)")


def wisp_frame(idx, size=32):
    """Dense black smoke puff; shape varies per frame (stable smoke, not specks)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    seed = idx * 1.7 + 0.5
    for y in range(size):
        for x in range(size):
            dx, dy = (x + 0.5 - c) / c, (y + 0.5 - c) / c
            d = math.hypot(dx, dy)
            if d >= 1.0:
                continue
            # dense flat core, soft but quick edge
            if d < 0.55:
                a = 1.0
            else:
                a = max(0.0, 1.0 - (d - 0.55) / 0.45) ** 1.2
            ang = math.atan2(dy, dx)
            wob = 0.12 * math.sin(ang * 5 + seed) + 0.07 * math.sin(ang * 11 - seed * 2)
            edge = 0.78 + wob
            if d > edge:
                a *= max(0.0, 1.0 - (d - edge) / (1.0 - edge))
            alpha = int(250 * a)             # DENSE
            base = 5 + int(10 * (1.0 - d))   # near-black
            px[x, y] = (base, base, base + 3, alpha)
    img.save(f"{PART}/void_shadow_wisp_{idx}.png")
    print(f"wrote void_shadow_wisp_{idx}.png (dense)")


for i in range(4):
    eye_frame(i)
for i in range(4):
    wisp_frame(i)
print("done: void particles rework (almond eyes + dense smoke)")
