"""Gera 12 texturas de Source Jar com nível de fluido visível (16x16)."""
from PIL import Image, ImageDraw
import os

OUT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\block"
os.makedirs(OUT, exist_ok=True)
SIZE = 16

DARK = (20, 10, 35, 255)
GLASS_LIGHT = (90, 60, 130, 200)
GLASS_DARK = (40, 20, 70, 220)
LIQUID_BASE = (180, 100, 230, 255)
LIQUID_GLOW = (220, 160, 255, 255)
LID = (110, 70, 50, 255)

def make_jar(fill_level):
    """fill_level 0-11"""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Background — outline of jar (full transparency around)
    # Jar SIDE view (16x16):
    #   col 0-1: empty (background)
    #   col 2-13: jar body
    #   col 14-15: empty
    #   row 0: lid top (narrow)
    #   row 1: lid bottom (wider)
    #   row 2: neck top
    #   row 3-14: jar body
    #   row 15: jar bottom rim

    # Lid (cork)
    d.rectangle([5, 0, 10, 0], fill=LID, outline=DARK)
    d.rectangle([4, 1, 11, 1], fill=LID, outline=DARK)
    # Neck
    d.rectangle([4, 2, 11, 2], fill=GLASS_DARK)
    # Body outline
    for x in range(2, 14):
        d.point((x, 3), fill=GLASS_DARK)
        d.point((x, 14), fill=GLASS_DARK)
    for y in range(3, 15):
        d.point((2, y), fill=GLASS_DARK)
        d.point((13, y), fill=GLASS_DARK)
    # Body fill (glass with subtle tint)
    for x in range(3, 13):
        for y in range(4, 14):
            img.putpixel((x, y), (50, 30, 80, 80))  # transparent glass
    # Light reflection on glass
    d.line([3, 5, 3, 9], fill=(150, 120, 200, 180))
    d.line([4, 4, 4, 5], fill=(180, 150, 230, 200))

    # FLUID inside (rises with fill level)
    # Max fluid height = 10 rows (y=13 down to y=4)
    # fill 0 = 0 rows, fill 11 = 10 rows
    fluid_height = int(round(fill_level * 10 / 11))
    if fluid_height > 0:
        fluid_top = 14 - fluid_height
        for x in range(3, 13):
            for y in range(fluid_top, 14):
                # Brighter at top (meniscus), darker at bottom
                if y == fluid_top:
                    img.putpixel((x, y), LIQUID_GLOW)
                else:
                    img.putpixel((x, y), LIQUID_BASE)
        # Top sparkle
        d.point((6, fluid_top), fill=(255, 255, 255, 255))
        d.point((10, fluid_top), fill=(255, 255, 255, 255))
    return img

# Generate 12 variants
for i in range(12):
    img = make_jar(i)
    img.save(os.path.join(OUT, f"source_jar_{i}.png"))

# Also write a top texture and bottom (simpler)
top = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(top)
# Lid cork from above
d.rectangle([4, 4, 11, 11], fill=LID, outline=DARK)
d.line([5, 5, 10, 5], fill=(180, 130, 100, 255))
d.line([5, 10, 10, 10], fill=(60, 40, 30, 255))
top.save(os.path.join(OUT, "source_jar_top.png"))

bot = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(bot)
d.rectangle([2, 2, 13, 13], fill=GLASS_DARK, outline=DARK)
d.rectangle([4, 4, 11, 11], fill=(30, 15, 50, 255))
bot.save(os.path.join(OUT, "source_jar_bottom.png"))

print("Generated 12 source_jar fluid levels + top/bottom.")
