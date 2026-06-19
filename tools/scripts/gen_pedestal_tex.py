"""r77: Spell Binding Pedestal texture."""
from PIL import Image, ImageDraw
import os

BLOCK = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\block"
SIZE = 16

# Top texture — pedestal com pentáculo dourado
def top():
    img = Image.new("RGBA", (SIZE, SIZE), (40, 25, 60, 255))
    d = ImageDraw.Draw(img)
    # Stone-ish base
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*11+y*7) % 13
            if n > 10:
                img.putpixel((x, y), (60, 40, 80, 255))
            elif n < 3:
                img.putpixel((x, y), (25, 15, 40, 255))
    # Golden pentacle circle
    d.ellipse([2, 2, 13, 13], outline=(255, 220, 100, 255), width=1)
    d.ellipse([4, 4, 11, 11], outline=(220, 180, 80, 255), width=1)
    # Center sparkle
    d.point((8, 8), fill=(255, 255, 200, 255))
    d.point((7, 8), fill=(255, 230, 180, 255))
    d.point((9, 8), fill=(255, 230, 180, 255))
    d.point((8, 7), fill=(255, 230, 180, 255))
    d.point((8, 9), fill=(255, 230, 180, 255))
    return img

def side():
    img = Image.new("RGBA", (SIZE, SIZE), (35, 20, 50, 255))
    d = ImageDraw.Draw(img)
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*7+y*13) % 14
            if n > 12:
                img.putpixel((x, y), (55, 35, 80, 255))
            elif n < 2:
                img.putpixel((x, y), (20, 10, 30, 255))
    # Decorative rune vertical
    d.line([8, 3, 8, 12], fill=(180, 130, 220, 200))
    d.line([6, 6, 10, 6], fill=(200, 150, 240, 200))
    d.line([6, 10, 10, 10], fill=(200, 150, 240, 200))
    return img

def bottom():
    img = Image.new("RGBA", (SIZE, SIZE), (30, 18, 45, 255))
    return img

top().save(os.path.join(BLOCK, "spell_binding_pedestal_top.png"))
side().save(os.path.join(BLOCK, "spell_binding_pedestal_side.png"))
bottom().save(os.path.join(BLOCK, "spell_binding_pedestal_bottom.png"))
print("Generated 3 textures.")
