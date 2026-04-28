"""Redraw blood_torch as a proper full 16x16 cross-style sprite."""
from PIL import Image, ImageDraw
import os

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures", "block")
S = 16
im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(im)

# Stick body — taller, fatter, more visible as a "torch column".
# Wood column from y=6 down to y=15, x=7..8.
for y in range(6, 16):
    d.point((7, y), fill=(120, 70, 30, 255))
    d.point((8, y), fill=(140, 85, 35, 255))
# Outer wood shadow
for y in range(6, 16):
    d.point((6, y), fill=(80, 50, 20, 255))
    d.point((9, y), fill=(80, 50, 20, 255))

# Flame above — tall red flame from y=1 to y=5
# Outer red glow
d.point((6, 5), fill=(180, 30, 45, 255))
d.point((7, 5), fill=(220, 50, 65, 255))
d.point((8, 5), fill=(220, 50, 65, 255))
d.point((9, 5), fill=(180, 30, 45, 255))

d.point((6, 4), fill=(220, 50, 65, 255))
d.point((7, 4), fill=(255, 80, 90, 255))
d.point((8, 4), fill=(255, 80, 90, 255))
d.point((9, 4), fill=(220, 50, 65, 255))

d.point((7, 3), fill=(255, 100, 110, 255))
d.point((8, 3), fill=(255, 100, 110, 255))
d.point((6, 3), fill=(180, 30, 45, 255))
d.point((9, 3), fill=(180, 30, 45, 255))

d.point((7, 2), fill=(255, 130, 140, 255))
d.point((8, 2), fill=(255, 130, 140, 255))

d.point((7, 1), fill=(255, 200, 180, 255))
d.point((8, 1), fill=(255, 200, 180, 255))

# Glowing core at base of flame (where it meets the wood)
d.point((7, 6), fill=(255, 200, 100, 255))
d.point((8, 6), fill=(255, 200, 100, 255))

# Save
im.save(os.path.join(BASE, "blood_torch.png"))
print("OK blood_torch redrawn")
