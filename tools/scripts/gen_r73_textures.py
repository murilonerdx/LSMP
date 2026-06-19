"""r73: Imbuement Table textures + nova Scribes Table textures."""
from PIL import Image, ImageDraw
import os, math

OUT_BLOCK = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\block"
OUT_ITEM = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item"

SIZE = 16

def imbuement_top():
    """Imbuement table top — rune circle + 4 slots."""
    img = Image.new("RGBA", (SIZE, SIZE), (35, 20, 60, 255))
    d = ImageDraw.Draw(img)
    # Wood grain base
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*13+y*7) % 13
            if n > 10:
                img.putpixel((x, y), (60, 35, 90, 255))
            elif n < 2:
                img.putpixel((x, y), (20, 10, 35, 255))
    # Outer rune circle
    d.ellipse([2, 2, 13, 13], outline=(200, 150, 255, 255), width=1)
    d.ellipse([4, 4, 11, 11], outline=(150, 100, 220, 255), width=1)
    # Inner pentagram
    cx, cy = 8, 8
    pts = []
    for a in range(5):
        ang = a * 2*math.pi/5 - math.pi/2
        pts.append((cx + math.cos(ang)*2.5, cy + math.sin(ang)*2.5))
    for i in range(5):
        j = (i+2) % 5
        d.line([pts[i], pts[j]], fill=(255, 220, 255, 255))
    return img

def imbuement_side():
    """Imbuement table side — dark wood with rune engravings."""
    img = Image.new("RGBA", (SIZE, SIZE), (30, 20, 50, 255))
    d = ImageDraw.Draw(img)
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*7+y*11) % 16
            if n > 12:
                img.putpixel((x, y), (55, 35, 80, 255))
            elif n < 3:
                img.putpixel((x, y), (15, 8, 30, 255))
    # Vertical rune engraving
    d.line([8, 2, 8, 14], fill=(150, 100, 220, 200))
    d.line([6, 5, 10, 5], fill=(180, 130, 240, 200))
    d.line([6, 11, 10, 11], fill=(180, 130, 240, 200))
    # Sparkle
    d.point((4, 8), fill=(255, 200, 255, 255))
    d.point((12, 8), fill=(255, 200, 255, 255))
    return img

def imbuement_bottom():
    img = Image.new("RGBA", (SIZE, SIZE), (30, 20, 50, 255))
    d = ImageDraw.Draw(img)
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*5+y*7) % 11
            if n < 2:
                img.putpixel((x, y), (15, 8, 30, 255))
    return img

def scribes_top_new():
    """Scribes Table — better top with quill drawing + ink stains."""
    img = Image.new("RGBA", (SIZE, SIZE), (90, 60, 30, 255))
    d = ImageDraw.Draw(img)
    # Wood
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*11+y*5) % 13
            if n > 10:
                img.putpixel((x, y), (110, 75, 40, 255))
            elif n < 3:
                img.putpixel((x, y), (60, 35, 20, 255))
    # Parchment laid on top
    d.rectangle([3, 4, 12, 12], fill=(230, 210, 170, 255), outline=(120, 90, 50, 255))
    # Ink lines on parchment
    d.line([4, 6, 11, 6], fill=(60, 30, 100, 255))
    d.line([4, 8, 9, 8], fill=(60, 30, 100, 255))
    d.line([4, 10, 11, 10], fill=(60, 30, 100, 255))
    # Quill (diagonal)
    d.line([10, 3, 13, 0], fill=(220, 200, 255, 255))
    d.point((13, 0), fill=(255, 255, 255, 255))
    return img

def scribes_side_new():
    img = Image.new("RGBA", (SIZE, SIZE), (90, 60, 30, 255))
    d = ImageDraw.Draw(img)
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x*5+y*11) % 15
            if n > 12:
                img.putpixel((x, y), (115, 80, 45, 255))
            elif n < 3:
                img.putpixel((x, y), (50, 30, 15, 255))
    # Engraved rune small
    d.ellipse([6, 6, 9, 9], outline=(200, 150, 255, 200), width=1)
    return img

# Generate
img = imbuement_top()
img.save(os.path.join(OUT_BLOCK, "imbuement_table_top.png"))
img = imbuement_side()
img.save(os.path.join(OUT_BLOCK, "imbuement_table_side.png"))
img = imbuement_bottom()
img.save(os.path.join(OUT_BLOCK, "imbuement_table_bottom.png"))
img = imbuement_top()
img.save(os.path.join(OUT_ITEM, "imbuement_table.png"))

# Scribes table new textures
img = scribes_top_new()
img.save(os.path.join(OUT_BLOCK, "scribes_table_top.png"))
img = scribes_side_new()
img.save(os.path.join(OUT_BLOCK, "scribes_table_side.png"))
# Item icon = top view
img = scribes_top_new()
img.save(os.path.join(OUT_ITEM, "scribes_table.png"))

print("Generated 7 new block/item textures.")
