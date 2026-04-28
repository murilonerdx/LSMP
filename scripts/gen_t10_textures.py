"""Generate textures for T10 (op tools: scale rods, command tablet, command pylon)."""
from PIL import Image, ImageDraw
import os, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
BLOCK = os.path.join(BASE, "block")
os.makedirs(ITEM, exist_ok=True)
os.makedirs(BLOCK, exist_ok=True)
S = 16

def new_img(bg=(0, 0, 0, 0)): return Image.new("RGBA", (S, S), bg)
def save_item(name, img): img.save(os.path.join(ITEM, name + ".png"))
def save_block(name, img): img.save(os.path.join(BLOCK, name + ".png"))

# --- growth_rod: gold-green wand with up arrow ---
img = new_img(); d = ImageDraw.Draw(img)
# rod
d.line([3, 13, 11, 5], fill=(180, 140, 50, 255), width=1)
d.line([4, 13, 12, 5], fill=(120, 90, 25, 255))
# orb
d.ellipse([10, 2, 15, 7], fill=(80, 220, 100, 255), outline=(20, 100, 30, 255))
d.point((12, 4), fill=(220, 255, 200, 255))
# up arrow
d.line([7, 14, 7, 11], fill=(80, 220, 100, 255))
d.point((6, 12), fill=(80, 220, 100, 255))
d.point((8, 12), fill=(80, 220, 100, 255))
save_item("growth_rod", img)

# --- shrink_rod: dark purple rod with down arrow ---
img = new_img(); d = ImageDraw.Draw(img)
d.line([3, 13, 11, 5], fill=(120, 60, 160, 255), width=1)
d.line([4, 13, 12, 5], fill=(60, 25, 90, 255))
d.ellipse([10, 2, 15, 7], fill=(160, 80, 220, 255), outline=(60, 20, 90, 255))
d.point((12, 4), fill=(230, 200, 255, 255))
# down arrow
d.line([7, 11, 7, 14], fill=(200, 120, 240, 255))
d.point((6, 13), fill=(200, 120, 240, 255))
d.point((8, 13), fill=(200, 120, 240, 255))
save_item("shrink_rod", img)

# --- command_tablet: dark slate with cyan glyphs ---
img = new_img(); d = ImageDraw.Draw(img)
d.rectangle([2, 2, 13, 14], fill=(30, 30, 45, 255), outline=(15, 15, 25, 255))
d.rectangle([3, 3, 12, 13], fill=(45, 50, 70, 255))
# screen
d.rectangle([4, 4, 11, 12], fill=(15, 25, 40, 255), outline=(10, 80, 130, 255))
# code lines (cyan)
d.line([5, 6, 9, 6], fill=(80, 220, 240, 255))
d.line([5, 8, 10, 8], fill=(80, 220, 240, 255))
d.line([5, 10, 7, 10], fill=(80, 220, 240, 255))
# blink dot
d.point((9, 10), fill=(255, 255, 100, 255))
# corner
d.point((13, 13), fill=(220, 220, 100, 255))
save_item("command_tablet", img)

# --- command_pylon: purple metallic block with command icon ---
img = Image.new("RGBA", (S, S), (50, 35, 80, 255))
d = ImageDraw.Draw(img)
random.seed(31)
for _ in range(70):
    x, y = random.randint(0, 15), random.randint(0, 15)
    v = random.randint(40, 100)
    d.point((x, y), fill=(v + 30, v // 2, v + 50, 255))
# frame
d.rectangle([0, 0, 15, 15], outline=(20, 10, 35, 255))
d.rectangle([1, 1, 14, 14], outline=(120, 80, 180, 255))
# central command symbol ">_"
d.line([5, 6, 7, 8], fill=(200, 230, 255, 255))
d.line([5, 10, 7, 8], fill=(200, 230, 255, 255))
d.line([8, 11, 11, 11], fill=(200, 230, 255, 255))
# glow corners
d.point((2, 2), fill=(220, 180, 255, 255))
d.point((13, 2), fill=(220, 180, 255, 255))
d.point((2, 13), fill=(220, 180, 255, 255))
d.point((13, 13), fill=(220, 180, 255, 255))
save_block("command_pylon", img)

print("OK T10")
