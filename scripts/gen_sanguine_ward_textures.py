"""Generate inventory icons + armor body layers for the Sanguine Ward set."""
from PIL import Image, ImageDraw
import os, random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia", "textures")
ITEM = os.path.join(BASE, "item")
ARMOR = os.path.join(BASE, "models", "armor")
os.makedirs(ITEM, exist_ok=True)
os.makedirs(ARMOR, exist_ok=True)

# Palette: silver/cyan with red trim — "warding against blood".
SILVER = (180, 200, 220, 255)
SILVER_DARK = (90, 110, 140, 255)
CYAN = (140, 220, 240, 255)
RED = (200, 60, 80, 255)
DARK = (40, 50, 70, 255)

S = 16

def new_img(): return Image.new("RGBA", (S, S), (0, 0, 0, 0))
def save_item(name, img): img.save(os.path.join(ITEM, name + ".png"))

# --- helmet: domed head with red sigil ---
img = new_img(); d = ImageDraw.Draw(img)
d.rectangle([3, 4, 12, 12], fill=SILVER, outline=DARK)
d.rectangle([3, 12, 12, 13], fill=SILVER_DARK)
d.line([4, 5, 11, 5], fill=CYAN)
d.line([5, 7, 10, 7], fill=DARK)
d.point((7, 8), fill=RED); d.point((8, 8), fill=RED)
d.point((6, 4), fill=CYAN); d.point((9, 4), fill=CYAN)
save_item("sanguine_ward_helmet", img)

# --- chestplate ---
img = new_img(); d = ImageDraw.Draw(img)
d.rectangle([3, 3, 12, 14], fill=SILVER, outline=DARK)
d.rectangle([2, 4, 3, 10], fill=SILVER_DARK)
d.rectangle([12, 4, 13, 10], fill=SILVER_DARK)
# chest sigil
d.rectangle([6, 6, 9, 11], fill=DARK)
d.line([7, 7, 8, 7], fill=RED)
d.line([7, 9, 8, 9], fill=RED)
d.point((7, 8), fill=CYAN); d.point((8, 8), fill=CYAN)
# trim
d.line([3, 3, 12, 3], fill=CYAN)
save_item("sanguine_ward_chestplate", img)

# --- leggings ---
img = new_img(); d = ImageDraw.Draw(img)
d.rectangle([4, 2, 11, 14], fill=SILVER, outline=DARK)
d.line([7, 3, 7, 13], fill=DARK)
d.line([8, 3, 8, 13], fill=DARK)
d.point((5, 5), fill=RED); d.point((10, 5), fill=RED)
d.point((5, 11), fill=RED); d.point((10, 11), fill=RED)
save_item("sanguine_ward_leggings", img)

# --- boots ---
img = new_img(); d = ImageDraw.Draw(img)
d.rectangle([3, 9, 12, 14], fill=SILVER, outline=DARK)
d.line([3, 9, 12, 9], fill=CYAN)
d.point((5, 11), fill=RED); d.point((10, 11), fill=RED)
save_item("sanguine_ward_boots", img)

# --- sword: silver blade with red core ---
img = new_img(); d = ImageDraw.Draw(img)
# blade
d.line([2, 13, 12, 3], fill=SILVER, width=1)
d.line([3, 13, 13, 3], fill=SILVER_DARK)
d.line([2, 12, 11, 3], fill=CYAN)
# red core down center
d.point((7, 8), fill=RED); d.point((8, 7), fill=RED); d.point((6, 9), fill=RED)
# guard
d.rectangle([1, 13, 4, 14], fill=DARK)
# hilt
d.rectangle([2, 14, 3, 15], fill=(140, 60, 30, 255))
# pommel highlight
d.point((1, 14), fill=CYAN)
save_item("sanguine_ward_sword", img)

# --- pickaxe ---
img = new_img(); d = ImageDraw.Draw(img)
# head
d.polygon([(2, 5), (13, 2), (14, 5), (3, 8)], fill=SILVER, outline=DARK)
d.line([4, 4, 12, 3], fill=CYAN)
d.point((7, 4), fill=RED); d.point((10, 3), fill=RED)
# handle
d.line([6, 7, 13, 14], fill=(140, 80, 40, 255), width=1)
d.line([7, 7, 14, 14], fill=(90, 50, 20, 255))
save_item("sanguine_ward_pickaxe", img)

# --- blood_ward_charm: silver pendant with red gem ---
img = new_img(); d = ImageDraw.Draw(img)
for i, x in enumerate([5, 7, 9, 11]):
    d.point((x, 2 + (i % 2)), fill=SILVER)
# pendant body (octagon-ish)
d.polygon([(8, 5), (12, 9), (8, 13), (4, 9)], fill=SILVER, outline=DARK)
d.polygon([(8, 7), (10, 9), (8, 11), (6, 9)], fill=RED)
d.point((8, 9), fill=(255, 200, 220, 255))
d.point((4, 9), fill=CYAN); d.point((12, 9), fill=CYAN)
save_item("blood_ward_charm", img)

# ---------------------------------------------------------------- armor body layers
def base_layer(width, name):
    """Generic procedural armor layer mimicking vanilla layout (64xH)."""
    h = 32
    img = Image.new("RGBA", (width, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    random.seed(91)
    for x in range(width):
        for y in range(h):
            # paint with silver base + cyan highlights + red specks; 90% silver
            r = random.random()
            if r < 0.05:
                col = RED
            elif r < 0.18:
                col = CYAN
            elif r < 0.45:
                col = SILVER_DARK
            else:
                col = SILVER
            d.point((x, y), fill=col)
    return img

base_layer(64, "sanguine_ward_layer_1").save(os.path.join(ARMOR, "sanguine_ward_layer_1.png"))
base_layer(64, "sanguine_ward_layer_2").save(os.path.join(ARMOR, "sanguine_ward_layer_2.png"))

print("OK Sanguine Ward")
