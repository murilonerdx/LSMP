"""r72: gera texturas pros 10 utility glyphs + chalk + rune + 8 tomes."""
from PIL import Image, ImageDraw
import os

OUT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item"
BLOCK_OUT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\block"
SIZE = 16
BORDER = (20, 10, 35, 255)

def base(accent):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bg = tuple(int(c * 0.25) for c in accent[:3]) + (220,)
    d.rectangle([1, 1, 14, 14], fill=bg, outline=BORDER)
    d.rectangle([0, 0, 15, 15], outline=BORDER)
    d.point((1, 1), fill=accent); d.point((14, 14), fill=accent)
    d.point((1, 14), fill=accent); d.point((14, 1), fill=accent)
    return img, d

# 10 utility glyphs
def place_block(d, c):
    d.rectangle([4, 6, 11, 13], fill=c, outline=BORDER)
    d.line([4, 4, 11, 4], fill=c)
    d.line([8, 4, 8, 6], fill=c)
def break_block(d, c):
    d.rectangle([4, 6, 11, 13], outline=c, width=1)
    d.line([4, 6, 11, 13], fill=(255, 100, 100, 255), width=2)
    d.line([11, 6, 4, 13], fill=(255, 100, 100, 255), width=2)
def conjure_water(d, c):
    # Water drop
    d.polygon([(8, 3), (5, 8), (5, 12), (8, 14), (11, 12), (11, 8)], fill=c, outline=BORDER)
    d.point((7, 9), fill=(255, 255, 255, 255))
def light_glyph(d, c):
    d.line([8, 3, 8, 11], fill=c, width=2)
    d.rectangle([7, 11, 9, 13], fill=(150, 100, 50, 255))
    # Flame
    d.polygon([(8, 2), (6, 4), (8, 6), (10, 4)], fill=(255, 220, 50, 255))
def snare(d, c):
    d.ellipse([2, 4, 13, 11], outline=c, width=2)
    d.line([3, 7, 12, 7], fill=c)
    d.line([3, 9, 12, 9], fill=c)
def hex_glyph(d, c):
    # Pentagram
    pts = [(8, 2), (10, 7), (14, 8), (10, 11), (12, 14), (8, 12), (4, 14), (6, 11), (2, 8), (6, 7)]
    d.polygon(pts, outline=c)
    d.point((8, 8), fill=(255, 50, 50, 255))
def pickup(d, c):
    # Arrows pointing in
    d.line([2, 8, 6, 8], fill=c, width=2)
    d.line([4, 6, 6, 8], fill=c)
    d.line([4, 10, 6, 8], fill=c)
    d.line([10, 8, 14, 8], fill=c, width=2)
    d.line([10, 8, 12, 6], fill=c)
    d.line([10, 8, 12, 10], fill=c)
    d.point((8, 8), fill=c)
def pierce(d, c):
    # Arrow with multiple targets
    d.line([2, 8, 13, 8], fill=c, width=2)
    d.line([13, 6, 14, 8], fill=c)
    d.line([13, 10, 14, 8], fill=c)
    d.point((6, 8), fill=(255, 100, 100, 255))
    d.point((10, 8), fill=(255, 100, 100, 255))
def split(d, c):
    # Multiple arrows diverging
    d.line([3, 8, 7, 5], fill=c, width=2)
    d.line([3, 8, 7, 11], fill=c, width=2)
    d.line([3, 8, 13, 8], fill=c, width=2)
def aoe(d, c):
    # Expanding circles
    d.ellipse([6, 6, 9, 9], outline=c, width=1)
    d.ellipse([4, 4, 11, 11], outline=c, width=1)
    d.ellipse([1, 1, 14, 14], outline=c, width=1)
    d.point((8, 8), fill=c)

GLYPHS = [
    ("glyph_place_block",   (180, 130, 80, 255),  place_block),
    ("glyph_break_block",   (200, 100, 80, 255),  break_block),
    ("glyph_conjure_water", (80, 150, 220, 255),  conjure_water),
    ("glyph_light",         (255, 230, 100, 255), light_glyph),
    ("glyph_snare",         (180, 150, 50, 255),  snare),
    ("glyph_hex",           (130, 80, 180, 255),  hex_glyph),
    ("glyph_pickup",        (200, 200, 100, 255), pickup),
    ("glyph_pierce",        (220, 150, 80, 255),  pierce),
    ("glyph_split",         (200, 100, 200, 255), split),
    ("glyph_aoe",           (130, 200, 130, 255), aoe),
]

count = 0
for name, accent, drawer in GLYPHS:
    img, draw = base(accent)
    drawer(draw, accent)
    img.save(os.path.join(OUT, f"{name}.png"))
    count += 1

# Chalk item (stick with chalk tip)
img, d = base((220, 220, 230, 255))
d.line([3, 12, 10, 4], fill=(180, 180, 200, 255), width=2)
d.point((10, 4), fill=(255, 255, 255, 255))
d.point((11, 3), fill=(255, 255, 255, 255))
d.rectangle([2, 11, 5, 14], fill=(120, 120, 130, 255), outline=BORDER)
img.save(os.path.join(OUT, "observation_chalk.png"))
count += 1

# Rune block item (circle rune)
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Stone background
for x in range(SIZE):
    for y in range(SIZE):
        n = (x*7+y*3) % 7
        if n < 2:
            img.putpixel((x, y), (60, 50, 70, 255))
        else:
            img.putpixel((x, y), (90, 80, 100, 255))
# Glowing rune circle
d.ellipse([3, 3, 12, 12], outline=(220, 150, 255, 255), width=1)
d.ellipse([5, 5, 10, 10], outline=(180, 100, 220, 255), width=1)
# Inner symbol
d.line([6, 8, 9, 8], fill=(255, 200, 255, 255))
d.line([8, 6, 8, 10], fill=(255, 200, 255, 255))
img.save(os.path.join(OUT, "rune_block.png"))
img.save(os.path.join(BLOCK_OUT, "rune_block.png"))
count += 1

# 8 prebuilt tomes (book covers with element colors)
def book(accent_top, accent_side, accent_pages, symbol_drawer):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Spine (left)
    d.rectangle([2, 2, 4, 13], fill=accent_side, outline=BORDER)
    # Cover
    d.rectangle([4, 2, 13, 13], fill=accent_top, outline=BORDER)
    # Pages (right edge)
    d.line([13, 3, 13, 12], fill=accent_pages)
    # Decoration
    d.point((6, 4), fill=BORDER); d.point((11, 4), fill=BORDER)
    d.point((6, 11), fill=BORDER); d.point((11, 11), fill=BORDER)
    # Symbol on cover
    symbol_drawer(d)
    return img

def pyro_sym(d):
    d.polygon([(9, 5), (7, 8), (8, 10), (10, 8)], fill=(255, 220, 100, 255))
def frost_sym(d):
    d.line([8, 5, 8, 11], fill=(220, 240, 255, 255))
    d.line([6, 8, 11, 8], fill=(220, 240, 255, 255))
    d.point((6, 6), fill=(255, 255, 255, 255))
    d.point((10, 10), fill=(255, 255, 255, 255))
def sky_sym(d):
    d.polygon([(9, 4), (7, 8), (8, 8), (8, 12), (10, 12), (10, 8), (11, 8)], fill=(255, 255, 255, 255))
def web_sym(d):
    d.ellipse([6, 6, 11, 11], outline=(200, 200, 200, 255))
    d.line([5, 5, 12, 12], fill=(200, 200, 200, 255))
    d.line([12, 5, 5, 12], fill=(200, 200, 200, 255))
def death_sym(d):
    d.line([6, 5, 12, 11], fill=(255, 50, 50, 255), width=2)
    d.line([12, 5, 6, 11], fill=(255, 50, 50, 255), width=2)
def heal_sym(d):
    d.line([8, 5, 8, 11], fill=(150, 255, 150, 255), width=2)
    d.line([6, 8, 11, 8], fill=(150, 255, 150, 255), width=2)
def dash_sym(d):
    d.line([5, 8, 11, 8], fill=(255, 255, 100, 255), width=2)
    d.line([11, 6, 12, 8], fill=(255, 255, 100, 255))
    d.line([11, 10, 12, 8], fill=(255, 255, 100, 255))
def sing_sym(d):
    d.ellipse([7, 7, 10, 10], fill=(20, 0, 30, 255))
    d.ellipse([6, 6, 11, 11], outline=(200, 100, 255, 255))

BOOKS = [
    ("tome_pyromancer",    (180, 50, 30, 255),   (130, 20, 10, 255),  (255, 220, 180, 255), pyro_sym),
    ("tome_frostbinder",   (80, 130, 200, 255),  (50, 80, 150, 255),  (220, 240, 255, 255), frost_sym),
    ("tome_skywalker",     (220, 220, 240, 255), (180, 180, 200, 255), (255, 255, 255, 255), sky_sym),
    ("tome_webweaver",     (100, 100, 110, 255), (60, 60, 70, 255),    (180, 180, 180, 255), web_sym),
    ("tome_death_beam",    (90, 20, 20, 255),    (50, 10, 10, 255),    (220, 100, 100, 255), death_sym),
    ("tome_healing_light", (230, 200, 100, 255), (180, 130, 50, 255),  (255, 250, 200, 255), heal_sym),
    ("tome_dash",          (255, 220, 80, 255),  (200, 150, 30, 255),  (255, 255, 200, 255), dash_sym),
    ("tome_singularity",   (60, 20, 80, 255),    (30, 10, 50, 255),    (180, 100, 220, 255), sing_sym),
]
for name, top, side, pages, sym in BOOKS:
    img = book(top, side, pages, sym)
    img.save(os.path.join(OUT, f"{name}.png"))
    count += 1

print(f"r72: Generated {count} textures.")
