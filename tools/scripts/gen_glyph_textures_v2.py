"""r71: 25 elemental glyphs + 4 source items + Imbuement table top."""
from PIL import Image, ImageDraw
import os

OUT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item"
SIZE = 16
BORDER = (20, 10, 35, 255)

def base_canvas(accent):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bg = tuple(int(c * 0.25) for c in accent[:3]) + (220,)
    d.rectangle([1, 1, 14, 14], fill=bg, outline=BORDER)
    d.rectangle([0, 0, 15, 15], outline=BORDER)
    d.point((1, 1), fill=accent); d.point((14, 14), fill=accent)
    d.point((1, 14), fill=accent); d.point((14, 1), fill=accent)
    return img, d

# FIRE 5
def fireball(d, c):
    d.ellipse([3, 5, 12, 13], fill=c, outline=BORDER)
    d.line([8, 2, 7, 5], fill=c, width=2)
    d.point((8, 6), fill=(255, 255, 200, 255))
    d.point((6, 8), fill=(255, 255, 100, 255))
def inferno(d, c):
    for x in range(3, 13, 2):
        d.line([x, 14, x, 5+(x%3)], fill=c, width=2)
    d.line([3, 8, 12, 8], fill=(255, 200, 0, 255))
def cleansing_flame(d, c):
    d.polygon([(8,2),(5,7),(7,9),(5,13),(8,11),(11,13),(9,9),(11,7)], fill=c, outline=(255, 255, 200, 255))
def solar_pulse(d, c):
    cx, cy = 8, 8
    d.ellipse([5, 5, 11, 11], fill=c, outline=(255, 255, 200, 255))
    for a in [0, 1, 2, 3]:
        d.line([cx + a, 1, cx, 4], fill=c)
        d.line([cx - a, 15, cx, 12], fill=c)
        d.line([1, cy + a, 4, cy], fill=c)
        d.line([15, cy - a, 12, cy], fill=c)
def burning_aura(d, c):
    d.ellipse([2, 2, 13, 13], outline=c, width=1)
    d.ellipse([5, 5, 10, 10], fill=c)
    d.line([1, 8, 3, 8], fill=c)
    d.line([12, 8, 14, 8], fill=c)
    d.line([8, 1, 8, 3], fill=c)
    d.line([8, 12, 8, 14], fill=c)

# WATER 5
def bubble_shield(d, c):
    d.ellipse([2, 2, 13, 13], outline=c, width=2)
    d.ellipse([5, 5, 8, 8], fill=(220, 240, 255, 200))
def tidal_wave(d, c):
    d.line([2, 10, 4, 7], fill=c, width=2)
    d.line([4, 7, 7, 10], fill=c, width=2)
    d.line([7, 10, 9, 7], fill=c, width=2)
    d.line([9, 7, 12, 10], fill=c, width=2)
    d.line([2, 13, 13, 13], fill=c, width=2)
def frost_lance(d, c):
    d.line([8, 1, 8, 13], fill=c, width=2)
    d.line([6, 4, 10, 4], fill=c)
    d.line([5, 8, 11, 8], fill=c)
    d.line([6, 11, 10, 11], fill=c)
    d.line([8, 13, 6, 14], fill=c)
    d.line([8, 13, 10, 14], fill=c)
def mist_veil(d, c):
    for y in range(3, 13, 2):
        d.line([2, y, 13, y], fill=c)
        for x in range(3, 14, 3):
            d.point((x, y), fill=(255, 255, 255, 220))
def healing_rain(d, c):
    # Cloud top
    d.ellipse([2, 2, 13, 7], fill=c, outline=BORDER)
    # Hearts dripping
    d.line([4, 9, 4, 13], fill=(255, 100, 100, 255))
    d.line([8, 9, 8, 13], fill=(255, 100, 100, 255))
    d.line([12, 9, 12, 13], fill=(255, 100, 100, 255))

# EARTH 5
def stone_spikes(d, c):
    d.polygon([(2, 13), (4, 5), (6, 13)], fill=c, outline=BORDER)
    d.polygon([(6, 13), (8, 3), (10, 13)], fill=c, outline=BORDER)
    d.polygon([(10, 13), (12, 6), (14, 13)], fill=c, outline=BORDER)
def quake_step(d, c):
    d.line([2, 12, 13, 12], fill=c, width=3)
    d.line([4, 9, 4, 12], fill=c)
    d.line([8, 8, 8, 12], fill=c)
    d.line([12, 10, 12, 12], fill=c)
    d.point((5, 8), fill=c); d.point((9, 7), fill=c); d.point((11, 9), fill=c)
def vein_sight(d, c):
    d.ellipse([3, 5, 12, 10], outline=c, width=1)
    d.ellipse([6, 6, 9, 9], fill=c)
    d.line([1, 13, 4, 11], fill=c)
    d.line([15, 13, 11, 11], fill=c)
def earthen_wall(d, c):
    for x in [2, 6, 10]:
        d.rectangle([x, 4, x+3, 13], fill=c, outline=BORDER)
def roots(d, c):
    d.line([8, 1, 8, 14], fill=c, width=2)
    d.line([4, 5, 8, 7], fill=c)
    d.line([12, 5, 8, 7], fill=c)
    d.line([3, 9, 8, 11], fill=c)
    d.line([13, 9, 8, 11], fill=c)
    d.line([5, 13, 8, 14], fill=c)
    d.line([11, 13, 8, 14], fill=c)

# AIR 5
def gust(d, c):
    d.line([2, 4, 13, 4], fill=c, width=2)
    d.line([2, 8, 11, 8], fill=c, width=2)
    d.line([2, 12, 13, 12], fill=c, width=2)
    d.line([12, 3, 14, 5], fill=c)
    d.line([10, 7, 12, 9], fill=c)
def tornado(d, c):
    d.polygon([(3, 2), (12, 2), (10, 4), (5, 4)], fill=c)
    d.polygon([(4, 5), (11, 5), (10, 7), (5, 7)], fill=c)
    d.polygon([(5, 8), (10, 8), (9, 10), (6, 10)], fill=c)
    d.polygon([(6, 11), (9, 11), (8, 14), (7, 14)], fill=c)
def sky_step(d, c):
    d.polygon([(8, 1), (5, 5), (7, 5), (7, 14), (9, 14), (9, 5), (11, 5)], fill=c, outline=BORDER)
def velocity(d, c):
    d.polygon([(2, 8), (12, 5), (10, 8), (12, 11)], fill=c, outline=BORDER)
    d.polygon([(5, 8), (15, 5), (13, 8), (15, 11)], fill=c, outline=BORDER)
def wind_cutter(d, c):
    d.line([2, 13, 13, 2], fill=c, width=3)
    d.line([4, 5, 11, 12], fill=c, width=1)

# COSMIC 5
def void_pull(d, c):
    d.point((8, 8), fill=(255, 255, 255, 255))
    d.ellipse([6, 6, 9, 9], outline=c, width=1)
    d.ellipse([4, 4, 11, 11], outline=c, width=1)
    d.ellipse([1, 1, 14, 14], outline=c, width=1)
def dread_stare(d, c):
    d.ellipse([3, 5, 12, 10], outline=c, width=1)
    d.ellipse([6, 6, 9, 9], fill=c)
    d.point((7, 7), fill=(255, 50, 50, 255))
    d.line([5, 12, 11, 13], fill=c)
def mind_spike(d, c):
    d.polygon([(8, 1), (6, 8), (8, 14), (10, 8)], fill=c, outline=BORDER)
    d.point((8, 7), fill=(255, 255, 255, 255))
def reality_tear(d, c):
    d.polygon([(7, 1), (5, 5), (8, 7), (4, 10), (7, 14)], fill=c)
    d.polygon([(9, 1), (12, 5), (8, 7), (13, 10), (9, 14)], fill=c)
def singularity(d, c):
    d.ellipse([1, 1, 14, 14], outline=c, width=1)
    d.ellipse([4, 4, 11, 11], outline=(80, 80, 80, 255), width=1)
    d.ellipse([6, 6, 9, 9], fill=(0, 0, 0, 255))

# Source items
def source_crystal(d, c):
    d.polygon([(8, 2), (3, 6), (5, 13), (11, 13), (13, 6)], fill=c, outline=(255, 255, 255, 255))
    d.line([8, 2, 8, 13], fill=(255, 255, 255, 200))
    d.line([3, 6, 13, 6], fill=(255, 255, 255, 200))
def source_catalyst(d, c):
    d.rectangle([5, 3, 10, 13], fill=c, outline=BORDER)
    d.rectangle([6, 2, 9, 3], fill=(180, 100, 50, 255))
    d.line([6, 6, 9, 6], fill=(255, 255, 255, 200))
    d.line([6, 9, 9, 9], fill=(255, 255, 255, 200))
def source_lens(d, c):
    d.ellipse([2, 4, 13, 11], outline=c, width=2)
    d.ellipse([5, 6, 10, 9], fill=(255, 255, 200, 200))
    d.line([4, 12, 11, 12], fill=(180, 130, 80, 255))
def soul_fragment(d, c):
    d.polygon([(8, 2), (4, 7), (5, 13), (11, 13), (12, 7)], fill=c, outline=BORDER)
    d.point((7, 7), fill=(255, 255, 255, 255))
    d.point((9, 9), fill=(255, 255, 255, 255))

# Imbuement table top
def imbuement_table_top(img, draw):
    # Solid base
    for x in range(SIZE):
        for y in range(SIZE):
            img.putpixel((x, y), (40, 30, 50, 255))
    # Wood grain
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x * 17 + y * 5) % 16
            if n > 12:
                img.putpixel((x, y), (70, 50, 80, 255))
            elif n < 3:
                img.putpixel((x, y), (20, 15, 25, 255))
    # Magical rune circle
    draw.ellipse([3, 3, 12, 12], outline=(220, 180, 255, 255), width=1)
    draw.ellipse([5, 5, 10, 10], outline=(180, 130, 220, 255), width=1)
    # Star inside
    cx, cy = 8, 8
    for a in [0, 1, 2, 3, 4]:
        import math
        ang = a * math.pi * 2 / 5 - math.pi / 2
        x = cx + math.cos(ang) * 2.5
        y = cy + math.sin(ang) * 2.5
        draw.point((int(x), int(y)), fill=(255, 220, 255, 255))

ELEM = [
    ("glyph_fireball",        (255, 80, 30, 255),  fireball),
    ("glyph_inferno",         (255, 50, 20, 255),  inferno),
    ("glyph_cleansing_flame", (255, 200, 100, 255), cleansing_flame),
    ("glyph_solar_pulse",     (255, 240, 100, 255), solar_pulse),
    ("glyph_burning_aura",    (255, 150, 50, 255), burning_aura),
    ("glyph_bubble_shield",   (130, 200, 255, 255), bubble_shield),
    ("glyph_tidal_wave",      (60, 130, 220, 255), tidal_wave),
    ("glyph_frost_lance",     (180, 230, 255, 255), frost_lance),
    ("glyph_mist_veil",       (200, 220, 255, 255), mist_veil),
    ("glyph_healing_rain",    (130, 230, 170, 255), healing_rain),
    ("glyph_stone_spikes",    (160, 130, 100, 255), stone_spikes),
    ("glyph_quake_step",      (180, 150, 100, 255), quake_step),
    ("glyph_vein_sight",      (150, 200, 100, 255), vein_sight),
    ("glyph_earthen_wall",    (130, 90, 60, 255),  earthen_wall),
    ("glyph_roots",           (100, 130, 50, 255), roots),
    ("glyph_gust",            (230, 230, 255, 255), gust),
    ("glyph_tornado",         (200, 200, 230, 255), tornado),
    ("glyph_sky_step",        (220, 230, 255, 255), sky_step),
    ("glyph_velocity",        (255, 255, 200, 255), velocity),
    ("glyph_wind_cutter",     (200, 255, 200, 255), wind_cutter),
    ("glyph_void_pull",       (80, 50, 130, 255),  void_pull),
    ("glyph_dread_stare",     (50, 30, 70, 255),   dread_stare),
    ("glyph_mind_spike",      (160, 50, 200, 255), mind_spike),
    ("glyph_reality_tear",    (50, 20, 70, 255),   reality_tear),
    ("glyph_singularity",     (20, 10, 40, 255),   singularity),
]

count = 0
for name, accent, drawer in ELEM:
    img, draw = base_canvas(accent)
    drawer(draw, accent)
    img.save(os.path.join(OUT, f"{name}.png"))
    count += 1

# Source items
ITEMS = [
    ("source_crystal", (130, 180, 255, 255), source_crystal),
    ("source_catalyst", (100, 200, 220, 255), source_catalyst),
    ("source_lens", (240, 200, 100, 255), source_lens),
    ("soul_fragment", (200, 150, 255, 255), soul_fragment),
]
for name, accent, drawer in ITEMS:
    img, draw = base_canvas(accent)
    drawer(draw, accent)
    img.save(os.path.join(OUT, f"{name}.png"))
    count += 1

# Imbuement table block top
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
imbuement_table_top(img, draw)
img.save(os.path.join(OUT, "imbuement_table.png"))
count += 1

print(f"Generated {count} new textures.")
