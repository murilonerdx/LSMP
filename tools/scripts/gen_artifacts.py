"""Textures for the 3 boss artifacts."""
from PIL import Image, ImageDraw
import os

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures", "item")
os.makedirs(BASE, exist_ok=True)
S = 16

def img(): return Image.new("RGBA", (S, S), (0, 0, 0, 0))
def save(n, im): im.save(os.path.join(BASE, n + ".png"))

# --- cursed_idol: black stone idol with red eyes ---
im = img(); d = ImageDraw.Draw(im)
# body
d.polygon([(5, 14), (5, 6), (7, 4), (9, 4), (11, 6), (11, 14)],
          fill=(40, 30, 35, 255), outline=(15, 10, 15, 255))
# face
d.point((6, 7), fill=(0, 0, 0, 255))
d.point((10, 7), fill=(0, 0, 0, 255))
# glowing red eyes
d.point((6, 7), fill=(220, 40, 50, 255))
d.point((10, 7), fill=(220, 40, 50, 255))
# mouth (black slit)
d.line([7, 9, 9, 9], fill=(15, 5, 10, 255))
# base
d.rectangle([4, 14, 12, 15], fill=(70, 50, 55, 255))
# rune on chest
d.point((8, 11), fill=(180, 30, 40, 255))
d.point((7, 12), fill=(180, 30, 40, 255))
d.point((9, 12), fill=(180, 30, 40, 255))
save("cursed_idol", im)

# --- veiled_lantern: cracked black lantern with sickly green light ---
im = img(); d = ImageDraw.Draw(im)
# top hook
d.line([7, 1, 7, 3], fill=(80, 80, 80, 255))
d.line([8, 1, 8, 3], fill=(60, 60, 60, 255))
# body frame
d.rectangle([4, 4, 11, 13], fill=(40, 40, 45, 255), outline=(15, 15, 20, 255))
# inner glass with sickly green flame
d.rectangle([5, 5, 10, 12], fill=(20, 35, 25, 255))
d.point((7, 8), fill=(150, 220, 130, 255))
d.point((8, 8), fill=(180, 240, 150, 255))
d.point((7, 9), fill=(120, 180, 110, 255))
d.point((8, 9), fill=(120, 180, 110, 255))
# bars
d.line([7, 5, 7, 12], fill=(30, 30, 35, 255))
d.line([8, 5, 8, 12], fill=(30, 30, 35, 255))
# foot
d.rectangle([3, 13, 12, 14], fill=(60, 60, 65, 255))
save("veiled_lantern", im)

# --- pulsing_heart: dark red beating heart with veins ---
im = img(); d = ImageDraw.Draw(im)
# heart silhouette
d.ellipse([3, 4, 8, 9], fill=(140, 20, 30, 255), outline=(50, 5, 10, 255))
d.ellipse([8, 4, 13, 9], fill=(140, 20, 30, 255), outline=(50, 5, 10, 255))
d.polygon([(3, 7), (13, 7), (8, 14)], fill=(140, 20, 30, 255), outline=(50, 5, 10, 255))
# pulsing core (brighter center)
d.point((8, 9), fill=(220, 50, 60, 255))
d.point((7, 9), fill=(200, 40, 50, 255))
d.point((9, 9), fill=(200, 40, 50, 255))
# bright highlight (heartbeat)
d.point((6, 6), fill=(255, 100, 120, 255))
d.point((10, 6), fill=(255, 100, 120, 255))
# veins
d.point((4, 8), fill=(80, 10, 15, 255))
d.point((12, 8), fill=(80, 10, 15, 255))
d.point((6, 12), fill=(80, 10, 15, 255))
d.point((10, 12), fill=(80, 10, 15, 255))
save("pulsing_heart", im)

print("OK 3 boss artifact textures")
