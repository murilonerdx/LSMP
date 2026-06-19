# Fallback icon for the Photograph item (16x16): a polaroid — white frame, dark
# photo area with a faint horizon, used only until the real captured PNG loads.
from PIL import Image
import os

OUT = "src/main/resources/assets/liberthia/textures/item"
os.makedirs(OUT, exist_ok=True)

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = img.load()

FRAME = (235, 235, 230, 255)
FRAME_SH = (180, 180, 175, 255)
PHOTO_SKY = (70, 90, 130, 255)
PHOTO_GND = (40, 55, 40, 255)
DARK = (20, 22, 28, 255)

# polaroid body 2..14 with a wider bottom border (classic look)
for y in range(2, 15):
    for x in range(2, 14):
        px[x, y] = FRAME
# subtle frame shading
for x in range(2, 14):
    px[x, 14] = FRAME_SH
for y in range(2, 15):
    px[13, y] = FRAME_SH
# photo window 3..12 x, 3..10 y (leaves a thick bottom margin)
for y in range(3, 11):
    for x in range(3, 13):
        px[x, y] = PHOTO_SKY if y < 7 else PHOTO_GND
# faint horizon line
for x in range(3, 13):
    px[x, 7] = (90, 110, 120, 255)
# a tiny dark "something" in the photo (ominous)
px[9, 5] = DARK; px[9, 6] = DARK; px[10, 6] = DARK

img.save(f"{OUT}/photograph.png")
print("wrote photograph.png")
