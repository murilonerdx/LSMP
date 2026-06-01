"""Textures for the FE energy system blocks."""
from PIL import Image, ImageDraw
import os, random

OUT = os.path.join(os.path.dirname(__file__), "..",
                   "src", "main", "resources", "assets", "liberthia", "textures", "block")
os.makedirs(OUT, exist_ok=True)
S = 16

# --- dark_matter_generator: black metallic plate with a glowing purple core ---
img = Image.new("RGBA", (S, S), (20, 14, 32, 255))
d = ImageDraw.Draw(img)
random.seed(57)
# noise plate
for _ in range(110):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(20, 70)
    d.point((x, y), fill=(v, v // 2, v + 10, 255))
# inset frame
d.rectangle([1, 1, 14, 14], outline=(50, 30, 80, 255))
d.rectangle([2, 2, 13, 13], outline=(15, 8, 25, 255))
# glowing core
d.ellipse([5, 5, 10, 10], fill=(150, 60, 220, 255), outline=(80, 30, 130, 255))
d.point((7, 7), fill=(220, 180, 255, 255))
d.point((8, 8), fill=(220, 180, 255, 255))
# corner studs
for cx, cy in [(2, 2), (13, 2), (2, 13), (13, 13)]:
    d.point((cx, cy), fill=(140, 100, 60, 255))
img.save(os.path.join(OUT, "dark_matter_generator.png"))

# --- energy_cable: dark grey insulator with cyan glowing core ---
img = Image.new("RGBA", (S, S), (35, 35, 42, 255))
d = ImageDraw.Draw(img)
random.seed(73)
for _ in range(80):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(30, 60)
    d.point((x, y), fill=(v, v, v + 5, 255))
# central glowing strip
d.rectangle([0, 6, 15, 9], fill=(40, 120, 180, 255))
d.line([0, 7, 15, 7], fill=(120, 220, 255, 255))
d.line([0, 8, 15, 8], fill=(80, 180, 220, 255))
img.save(os.path.join(OUT, "energy_cable.png"))

print("OK energy textures")
