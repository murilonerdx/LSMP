"""Texture for crystallized_blood_soul — red faceted crystal with soul wisp."""
from PIL import Image, ImageDraw
import os

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures", "item")
os.makedirs(BASE, exist_ok=True)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# crystal outer (deep red)
d.polygon([(8, 1), (13, 5), (13, 11), (8, 15), (3, 11), (3, 5)], fill=(120, 20, 30, 255), outline=(40, 5, 10, 255))
# inner facet (pink)
d.polygon([(8, 4), (11, 6), (11, 10), (8, 12), (5, 10), (5, 6)], fill=(220, 60, 80, 255))
# soul wisp (pale blue)
d.point((8, 7), fill=(220, 240, 255, 255))
d.point((8, 8), fill=(180, 220, 255, 255))
d.point((8, 9), fill=(150, 200, 240, 255))
# highlight
d.point((6, 5), fill=(255, 200, 220, 255))
d.point((11, 11), fill=(255, 180, 200, 255))
img.save(os.path.join(BASE, "crystallized_blood_soul.png"))
print("OK soul shard")
