"""Textures for the 3 anti-blood cleansing items."""
from PIL import Image, ImageDraw
import os

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures", "item")
os.makedirs(BASE, exist_ok=True)
S = 16

def new_img(): return Image.new("RGBA", (S, S), (0, 0, 0, 0))
def save(name, img): img.save(os.path.join(BASE, name + ".png"))

# --- tainted_essence: dark crimson droplet with green sheen ---
img = new_img(); d = ImageDraw.Draw(img)
# droplet shape
d.polygon([(8, 2), (12, 9), (10, 14), (6, 14), (4, 9)], fill=(110, 20, 30, 255), outline=(40, 5, 10, 255))
# inner glow
d.polygon([(8, 5), (10, 9), (8, 12), (6, 9)], fill=(180, 40, 60, 255))
# greenish corruption sheen
d.point((7, 7), fill=(120, 200, 110, 255))
d.point((9, 11), fill=(120, 200, 110, 255))
d.point((6, 11), fill=(120, 200, 110, 255))
# highlight
d.point((9, 6), fill=(255, 200, 220, 255))
save("tainted_essence", img)

# --- cleansing_salt: small white pouch with crystals ---
img = new_img(); d = ImageDraw.Draw(img)
# pouch (cloth bag)
d.polygon([(4, 6), (12, 6), (13, 14), (3, 14)], fill=(220, 220, 200, 255), outline=(120, 100, 80, 255))
# tied top
d.line([5, 5, 11, 5], fill=(140, 110, 80, 255))
d.line([6, 4, 10, 4], fill=(160, 120, 90, 255))
d.point((6, 3), fill=(180, 140, 100, 255))
d.point((10, 3), fill=(180, 140, 100, 255))
# salt crystals visible
d.point((6, 9), fill=(255, 255, 255, 255))
d.point((9, 10), fill=(255, 255, 255, 255))
d.point((7, 12), fill=(255, 255, 255, 255))
d.point((10, 12), fill=(255, 255, 255, 255))
d.point((5, 11), fill=(220, 240, 255, 255))
# rune mark
d.line([7, 8, 9, 8], fill=(80, 160, 220, 200))
save("cleansing_salt", img)

# --- purifying_flask: silver/cyan flask with white liquid ---
img = new_img(); d = ImageDraw.Draw(img)
# neck
d.rectangle([6, 1, 9, 3], fill=(220, 240, 255, 255), outline=(80, 120, 160, 255))
# cork
d.rectangle([6, 0, 9, 1], fill=(180, 140, 80, 255))
# body
d.polygon([(4, 4), (11, 4), (12, 14), (3, 14)], fill=(200, 240, 255, 255), outline=(40, 80, 140, 255))
# liquid (pure white)
d.rectangle([5, 7, 10, 13], fill=(245, 250, 255, 255))
# silver shine on liquid
d.line([5, 8, 5, 12], fill=(255, 255, 255, 255))
d.point((10, 9), fill=(180, 220, 240, 255))
# cross/purify mark
d.line([7, 9, 8, 9], fill=(140, 200, 240, 255))
d.line([7, 10, 8, 10], fill=(140, 200, 240, 255))
d.point((7, 11), fill=(180, 220, 255, 255))
d.point((8, 11), fill=(180, 220, 255, 255))
# sparkle around
d.point((2, 6), fill=(220, 240, 255, 200))
d.point((13, 5), fill=(220, 240, 255, 200))
d.point((1, 12), fill=(220, 240, 255, 200))
save("purifying_flask", img)

print("OK cleansing textures")
