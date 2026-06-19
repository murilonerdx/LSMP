# Generates a 16x16 "old camera" item icon for Liberthia (item/camera.png).
# Boxy black body, lens ring, viewfinder, shutter button, tiny red REC dot.
from PIL import Image

W = H = 16
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = c

# palette
body_d = (28, 28, 32, 255)     # dark body
body   = (48, 48, 54, 255)     # body
body_l = (70, 70, 78, 255)     # highlight edge
metal  = (120, 122, 130, 255)  # lens ring
glass  = (40, 70, 110, 255)    # lens glass
glass_h= (110, 150, 200, 255)  # lens glint
red    = (200, 40, 40, 255)    # rec dot
black  = (12, 12, 14, 255)

# camera body: rows 4..13, cols 1..14
for y in range(4, 14):
    for x in range(1, 15):
        put(x, y, body)
# top hump (viewfinder/flash housing): cols 3..8 row 2..3
for y in range(2, 4):
    for x in range(3, 9):
        put(x, y, body_d)
# outline / shading
for x in range(1, 15):
    put(x, 4, body_l); put(x, 13, body_d)
for y in range(4, 14):
    put(1, y, body_l); put(14, y, body_d)
for x in range(3, 9):
    put(x, 2, body_l)

# lens: centered circle around (9,9) radius ~3.5
cx, cy = 9.5, 9.0
for y in range(4, 14):
    for x in range(5, 15):
        d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
        if d <= 3.6:
            put(x, y, metal)
        if d <= 2.6:
            put(x, y, glass)
        if d <= 1.2:
            put(x, y, glass_h)
# lens glint
put(8, 8, glass_h)

# shutter button (top-right) + flash window (top-left hump)
put(12, 3, (180, 180, 60, 255)); put(13, 3, (140, 140, 40, 255))
for x in range(4, 7):
    put(x, 3, (150, 170, 200, 255))  # flash glass

# red REC dot (bottom-left)
put(3, 11, red); put(3, 12, (140, 24, 24, 255))

# darken very corners to keep it readable
put(1, 4, black); put(14, 4, black); put(1, 13, black); put(14, 13, black)

img.save(r"src/main/resources/assets/liberthia/textures/item/camera.png")
print("wrote src/main/resources/assets/liberthia/textures/item/camera.png")
