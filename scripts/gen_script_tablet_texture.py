"""Texture for the LiberScript tablet — purple slate with code-style glyphs."""
from PIL import Image, ImageDraw
import os

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
os.makedirs(ITEM, exist_ok=True)
S = 16

img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# frame
d.rectangle([2, 2, 13, 14], fill=(40, 25, 55, 255), outline=(15, 8, 25, 255))
d.rectangle([3, 3, 12, 13], fill=(60, 40, 90, 255))
# screen
d.rectangle([4, 4, 11, 12], fill=(20, 10, 35, 255), outline=(140, 80, 220, 255))
# magenta code lines
d.line([5, 6, 9, 6], fill=(220, 130, 255, 255))
d.line([6, 8, 10, 8], fill=(220, 130, 255, 255))
d.line([5, 10, 8, 10], fill=(220, 130, 255, 255))
# blink dot (orange = "running")
d.point((10, 10), fill=(255, 180, 80, 255))
# corner stud
d.point((13, 13), fill=(255, 200, 100, 255))
img.save(os.path.join(ITEM, "script_tablet.png"))
print("OK script_tablet")
