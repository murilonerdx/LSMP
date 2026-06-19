# Generates complete 64x64 PLAYER-layout (Steve) skins for the 6 caster mobs, so the
# HumanoidModel<PLAYER> renders with NO missing/garbled parts. Each region (head/body/
# arms/legs, all 6 faces of every box) is filled — robe body, hood/face on the head,
# sleeve arms, boot legs — themed per caster.
#
# PLAYER 64x64 UV map (the standard Steve layout) regions we fill:
#   HEAD  : faces around (8..24 x, 0..16 y)   right/front/left/back/top/bottom
#   BODY  : (16..40 x, 16..32 y)
#   R-ARM : (40..56 x, 16..32) ; L-ARM (32..48, 48..64)
#   R-LEG : (0..16 x, 16..32)  ; L-LEG (16..32, 48..64)
# We don't need pixel-perfect cube unwrap — we fill whole rectangular regions with the
# right base/shade colors so every visible face shows the robe (no transparent = no
# "missing part"). Overlay simple face/hood/trim details on the front-facing areas.
from PIL import Image
import os

OUT = "src/main/resources/assets/liberthia/textures/entity/wizard"
os.makedirs(OUT, exist_ok=True)


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < 64 and 0 <= y < 64:
                px[x, y] = c


def shade(c, f):
    return (int(c[0]*f), int(c[1]*f), int(c[2]*f), 255)


def make_skin(name, robe, robe_dk, trim, skin, hood, accent, eyes=(220,40,40,255)):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    # ---------------- HEAD (hooded) ----------------
    # head faces block: x 0..32, y 0..16 (right 0-8, front 8-16, left 16-24, back 24-32 ; top/bottom 8-24 y0-8)
    # top + all sides = hood color
    rect(px, 0, 0, 32, 16, hood)
    rect(px, 8, 0, 24, 8, shade(hood, 1.1))      # top of head (hood crown)
    # FRONT face of head = x 8..16, y 8..16 → dark face cavity + glowing eyes
    rect(px, 8, 8, 16, 16, shade(hood, 0.45))    # shadowed face
    px[10, 11] = eyes; px[10, 12] = eyes         # left eye
    px[13, 11] = eyes; px[13, 12] = eyes         # right eye
    # hood brim shading on sides
    rect(px, 0, 8, 8, 16, shade(hood, 0.8))      # right side
    rect(px, 16, 8, 24, 16, shade(hood, 0.8))    # left side
    rect(px, 24, 8, 32, 16, shade(hood, 0.7))    # back

    # ---------------- BODY (robe) ----------------  x 16..40, y 16..32 (wrap)
    rect(px, 16, 16, 40, 20, shade(robe, 1.05))  # top rim (shoulders)
    rect(px, 16, 20, 40, 32, robe)
    # front of body = x 20..28 → robe with a vertical trim stripe + accent
    rect(px, 20, 20, 28, 32, robe)
    rect(px, 23, 20, 25, 32, trim)               # central trim
    rect(px, 21, 22, 22, 24, accent)             # clasp/gem
    rect(px, 26, 22, 27, 24, accent)
    # back of body x 32..40 slightly darker
    rect(px, 32, 20, 40, 32, robe_dk)

    # ---------------- RIGHT ARM ---------------- x 40..56, y 16..32 (sleeve)
    rect(px, 40, 16, 56, 20, shade(robe, 1.05))
    rect(px, 40, 20, 56, 32, robe)
    rect(px, 44, 20, 48, 22, skin)               # hand peeking (front-bottom approx)
    rect(px, 47, 28, 51, 32, skin)               # hand cuff
    rect(px, 44, 30, 56, 32, trim)               # sleeve trim

    # ---------------- LEFT ARM ---------------- x 32..48, y 48..64
    rect(px, 32, 48, 48, 52, shade(robe, 1.05))
    rect(px, 32, 52, 48, 64, robe)
    rect(px, 36, 62, 48, 64, trim)
    rect(px, 39, 60, 43, 64, skin)

    # ---------------- RIGHT LEG ---------------- x 0..16, y 16..32 (robe hem + boot)
    rect(px, 0, 16, 16, 28, robe_dk)
    rect(px, 0, 28, 16, 32, shade(accent, 0.6))  # boots
    # ---------------- LEFT LEG ---------------- x 16..32, y 48..64
    rect(px, 16, 48, 32, 60, robe_dk)
    rect(px, 16, 60, 32, 64, shade(accent, 0.6))

    img.save(f"{OUT}/{name}.png")
    print("wrote", name + ".png")


# palettes per caster (robe, robe_dark, trim, skin, hood, accent[, eyes])
make_skin("electromancer",
          robe=(40, 70, 120, 255), robe_dk=(26, 46, 80, 255), trim=(120, 200, 255, 255),
          skin=(214, 180, 140, 255), hood=(30, 52, 92, 255), accent=(180, 230, 255, 255),
          eyes=(140, 220, 255, 255))
make_skin("pyromancer",
          robe=(150, 40, 24, 255), robe_dk=(104, 26, 16, 255), trim=(255, 170, 50, 255),
          skin=(214, 180, 140, 255), hood=(120, 30, 18, 255), accent=(255, 140, 40, 255),
          eyes=(255, 170, 60, 255))
make_skin("cryomancer",
          robe=(120, 170, 210, 255), robe_dk=(80, 124, 170, 255), trim=(220, 245, 255, 255),
          skin=(214, 188, 165, 255), hood=(96, 146, 190, 255), accent=(200, 240, 255, 255),
          eyes=(180, 235, 255, 255))
make_skin("necromancer",
          robe=(40, 44, 50, 255), robe_dk=(24, 26, 30, 255), trim=(120, 200, 120, 255),
          skin=(180, 188, 170, 255), hood=(20, 22, 26, 255), accent=(90, 220, 120, 255),
          eyes=(120, 240, 130, 255))
make_skin("eldritch_cultist",
          robe=(70, 40, 96, 255), robe_dk=(46, 26, 64, 255), trim=(190, 120, 230, 255),
          skin=(150, 140, 160, 255), hood=(46, 24, 64, 255), accent=(220, 140, 255, 255),
          eyes=(230, 120, 255, 255))
make_skin("apothecarist",
          robe=(120, 90, 50, 255), robe_dk=(86, 64, 36, 255), trim=(210, 180, 110, 255),
          skin=(214, 180, 140, 255), hood=(150, 120, 70, 255), accent=(120, 220, 140, 255),
          eyes=(90, 200, 120, 255))
make_skin("keeper",
          robe=(180, 180, 190, 255), robe_dk=(130, 130, 142, 255), trim=(230, 220, 140, 255),
          skin=(214, 180, 140, 255), hood=(160, 162, 172, 255), accent=(240, 230, 150, 255),
          eyes=(250, 240, 180, 255))
make_skin("archevoker",
          robe=(60, 30, 36, 255), robe_dk=(40, 18, 22, 255), trim=(230, 80, 70, 255),
          skin=(210, 175, 150, 255), hood=(44, 20, 24, 255), accent=(255, 90, 70, 255),
          eyes=(255, 120, 90, 255))

print("done: 6 wizard skins (full 64x64 player layout)")
