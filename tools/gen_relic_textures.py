# Generates 16x16 item icons for the r178 horror relics:
# black_mirror, sanity_candle, broken_radio, cursed_doll.
from PIL import Image

OUT = "src/main/resources/assets/liberthia/textures/item"


def new():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, img.load()


def save(img, name):
    img.save(f"{OUT}/{name}.png")
    print("wrote", name)


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = c


# ---------- Black Mirror ----------
img, px = new()
frame = (90, 92, 100, 255)
frame_d = (50, 52, 60, 255)
glass = (10, 10, 16, 255)
glass_h = (40, 44, 60, 255)
face = (120, 120, 140, 90)  # faint pale hint
cx, cy = 7.5, 7.5
for y in range(16):
    for x in range(16):
        dx, dy = (x + 0.5 - cx) / 6.0, (y + 0.5 - cy) / 7.0
        d = (dx * dx + dy * dy) ** 0.5
        if d <= 1.0:
            put(px, x, y, glass)
        if 1.0 < d <= 1.18:
            put(px, x, y, frame)
        if 1.18 < d <= 1.30:
            put(px, x, y, frame_d)
# glass glint + faint face
put(px, 5, 4, glass_h); put(px, 6, 4, glass_h); put(px, 5, 5, glass_h)
put(px, 8, 8, face); put(px, 7, 9, face); put(px, 9, 9, face); put(px, 8, 10, face)
save(img, "black_mirror")

# ---------- Sanity Candle ----------
img, px = new()
wax = (235, 228, 205, 255)
wax_d = (200, 192, 168, 255)
flame_o = (255, 140, 30, 255)
flame_i = (255, 230, 120, 255)
flame_w = (255, 255, 230, 255)
holder = (120, 90, 50, 255)
# candle body cols 6..9 rows 6..13
for y in range(6, 14):
    for x in range(6, 10):
        put(px, x, y, wax)
for y in range(6, 14):
    put(px, 9, y, wax_d)
# holder base
for x in range(5, 11):
    put(px, x, 14, holder)
put(px, 6, 13, holder); put(px, 9, 13, holder)
# wick
put(px, 7, 5, (40, 30, 20, 255)); put(px, 8, 5, (40, 30, 20, 255))
# flame
put(px, 7, 4, flame_o); put(px, 8, 4, flame_o)
put(px, 7, 3, flame_i); put(px, 8, 3, flame_i)
put(px, 7, 2, flame_w)
put(px, 7, 1, flame_o)
save(img, "sanity_candle")

# ---------- Broken Radio ----------
img, px = new()
body = (70, 64, 58, 255)
body_l = (96, 88, 80, 255)
body_d = (44, 40, 36, 255)
mesh = (30, 28, 26, 255)
dial = (200, 180, 120, 255)
red = (200, 50, 40, 255)
metal = (150, 150, 158, 255)
# body rows 5..14 cols 2..14
for y in range(5, 15):
    for x in range(2, 15):
        put(px, x, y, body)
for x in range(2, 15):
    put(px, x, 5, body_l); put(px, x, 14, body_d)
for y in range(5, 15):
    put(px, 2, y, body_l); put(px, 14, y, body_d)
# speaker mesh (left)
for y in range(7, 13):
    for x in range(4, 8):
        if (x + y) % 2 == 0:
            put(px, x, y, mesh)
# dial (right) + needle
for y in range(7, 9):
    for x in range(9, 13):
        put(px, x, y, dial)
put(px, 11, 7, red)  # tuning needle
# knobs
put(px, 10, 11, metal); put(px, 12, 11, metal)
# antenna (broken, bent)
put(px, 12, 4, metal); put(px, 13, 3, metal); put(px, 13, 2, metal); put(px, 14, 1, metal)
# crack
put(px, 6, 6, body_d); put(px, 7, 7, body_d); put(px, 6, 8, body_d); put(px, 7, 9, body_d)
save(img, "broken_radio")

# ---------- Cursed Doll ----------
img, px = new()
cloth = (140, 70, 70, 255)      # reddish body
cloth_d = (104, 48, 48, 255)
skin = (210, 188, 160, 255)     # pale head
skin_d = (170, 150, 126, 255)
hair = (60, 44, 34, 255)
button = (20, 20, 24, 255)      # button eyes
stitch = (40, 30, 30, 255)
# head rows 2..7 cols 5..10
for y in range(2, 8):
    for x in range(5, 11):
        put(px, x, y, skin)
for y in range(2, 8):
    put(px, 10, y, skin_d)
# hair top
for x in range(5, 11):
    put(px, x, 2, hair)
put(px, 5, 3, hair); put(px, 10, 3, hair)
# button eyes + stitched mouth
put(px, 6, 5, button); put(px, 9, 5, button)
put(px, 7, 7, stitch); put(px, 8, 7, stitch)
# body rows 8..14 cols 4..11
for y in range(8, 15):
    for x in range(4, 12):
        put(px, x, y, cloth)
for y in range(8, 15):
    put(px, 11, y, cloth_d)
for x in range(4, 12):
    put(px, x, 14, cloth_d)
# stitch line on body
for x in range(5, 11):
    if x % 2 == 0:
        put(px, x, 11, stitch)
# little arms
put(px, 3, 9, cloth); put(px, 3, 10, cloth_d)
put(px, 12, 9, cloth); put(px, 12, 10, cloth_d)
save(img, "cursed_doll")

print("done: 4 relic textures")
