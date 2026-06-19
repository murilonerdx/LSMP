# Generates the 3 block textures for the Matter Tester machine (16x16 each):
#   matter_tester_top    — control panel: two sample wells + a glowing reactor lens
#   matter_tester_side   — chassis with a small DM/WM/YM readout strip
#   matter_tester_bottom — plain dark metal plate
from PIL import Image

OUT = "src/main/resources/assets/liberthia/textures/block"

METAL = (46, 52, 62, 255)
METAL_HI = (74, 84, 98, 255)
METAL_LO = (28, 32, 40, 255)
RIVET = (96, 108, 124, 255)
WELL = (12, 16, 22, 255)
A_RING = (90, 90, 230, 255)   # sample A (bluish)
B_RING = (235, 140, 70, 255)  # sample B (orange)
LENS_DK = (16, 40, 52, 255)
LENS_HOT = (70, 220, 255, 255)
DM = (170, 60, 210, 255)
WM = (235, 235, 245, 255)
YM = (250, 220, 70, 255)


def base(c=METAL):
    img = Image.new("RGBA", (16, 16), c)
    px = img.load()
    # bevel: top/left light, bottom/right dark
    for i in range(16):
        px[i, 0] = METAL_HI
        px[0, i] = METAL_HI
        px[i, 15] = METAL_LO
        px[15, i] = METAL_LO
    return img, px


def disc(px, cx, cy, r, color):
    for y in range(16):
        for x in range(16):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                px[x, y] = color


def ring(px, cx, cy, r, color):
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if r - 1 <= d <= r:
                px[x, y] = color


def rivets(px):
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px[x, y] = RIVET


# ── TOP: two sample wells + reactor lens ──
img, px = base()
rivets(px)
# sample well A (top-left area)
disc(px, 5, 5, 2.4, WELL); ring(px, 5, 5, 2.6, A_RING)
# sample well B (bottom-left area)
disc(px, 5, 11, 2.4, WELL); ring(px, 5, 11, 2.6, B_RING)
# reactor lens (right) — glowing cyan core
disc(px, 11, 8, 3.2, LENS_DK)
disc(px, 11, 8, 1.6, LENS_HOT)
px[10, 7] = (200, 255, 255, 255)
img.save(f"{OUT}/matter_tester_top.png")
print("wrote matter_tester_top.png")

# ── SIDE: chassis + DM/WM/YM readout strip ──
img, px = base()
rivets(px)
# dark display panel
for y in range(4, 12):
    for x in range(3, 13):
        px[x, y] = (10, 14, 20, 255)
for x in range(3, 13):
    px[x, 4] = (40, 48, 60, 255)
# three little bars (DM/WM/YM)
for x in range(4, 12):
    px[x, 6] = DM if x < 10 else (40, 30, 50, 255)
    px[x, 8] = WM if x < 8 else (60, 60, 70, 255)
    px[x, 10] = YM if x < 11 else (60, 56, 30, 255)
img.save(f"{OUT}/matter_tester_side.png")
print("wrote matter_tester_side.png")

# ── BOTTOM: plain plate ──
img, px = base(METAL_LO)
rivets(px)
img.save(f"{OUT}/matter_tester_bottom.png")
print("wrote matter_tester_bottom.png")

print("done: 3 matter tester block textures")
