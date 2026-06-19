"""
r70: Generate 35 glyph textures (16x16 PNG) procedurally.

Each glyph has:
- Base color by type (Method=blue, Effect/Manifestation=magenta/red, Distortion=gold)
- Unique geometric symbol
- Dark border + subtle inner glow
"""
from PIL import Image, ImageDraw
import os

OUT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item"
os.makedirs(OUT, exist_ok=True)
SIZE = 16

# Color scheme
BORDER = (20, 10, 35, 255)        # Dark purple border (cosmic horror)
GLOW_METHOD = (90, 180, 230, 255)   # cyan-blue
GLOW_MANIF = (230, 70, 180, 255)    # hot pink/magenta
GLOW_DISTORT = (240, 200, 60, 255)  # gold/yellow

# Per-glyph (name, type, accent color, symbol drawer fn)

def base_canvas(accent):
    """Create base square with border."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Inner fill (slightly dimmer than accent)
    bg = tuple(int(c * 0.25) for c in accent[:3]) + (220,)
    d.rectangle([1, 1, 14, 14], fill=bg, outline=BORDER)
    # Outer dark border
    d.rectangle([0, 0, 15, 15], outline=BORDER)
    # Corner highlights
    d.point((1, 1), fill=accent)
    d.point((14, 14), fill=accent)
    d.point((1, 14), fill=accent)
    d.point((14, 1), fill=accent)
    return img, d

# ───────────── SYMBOL DRAWERS ─────────────

def draw_eye(d, accent, w=2):
    """Eye symbol — for direct gaze, peripheral, secret."""
    d.ellipse([3, 5, 12, 10], outline=accent, width=1)
    d.ellipse([6, 6, 9, 9], fill=accent)

def draw_eye_closed(d, accent):
    """Closed eye / sleeping — secret."""
    d.line([3, 8, 12, 8], fill=accent, width=2)
    d.line([4, 7, 6, 6], fill=accent)
    d.line([9, 6, 11, 7], fill=accent)

def draw_eye_side(d, accent):
    """Peripheral — eye looking sideways."""
    d.ellipse([3, 5, 12, 10], outline=accent, width=1)
    d.ellipse([3, 6, 6, 9], fill=accent)

def draw_brain(d, accent):
    """Brain / memory."""
    d.ellipse([3, 4, 12, 11], outline=accent, width=1)
    d.line([5, 6, 8, 6], fill=accent)
    d.line([5, 8, 10, 8], fill=accent)
    d.line([7, 10, 10, 10], fill=accent)

def draw_shush(d, accent):
    """Finger to lips / silence."""
    d.line([7, 4, 7, 11], fill=accent, width=2)
    d.line([5, 4, 9, 4], fill=accent)
    d.point((6, 12), fill=accent)
    d.point((8, 12), fill=accent)

def draw_mirror(d, accent):
    """Mirror / reflection — diagonal split."""
    d.polygon([(2, 2), (13, 2), (2, 13)], outline=accent)
    d.polygon([(13, 2), (13, 13), (2, 13)], outline=accent, fill=tuple(int(c*0.5) for c in accent[:3]) + (180,))

def draw_tendril(d, accent):
    """Wavy tendril / tentacle."""
    pts = [(3,3),(5,5),(4,7),(6,9),(5,11),(7,13)]
    for i in range(len(pts)-1):
        d.line([pts[i], pts[i+1]], fill=accent, width=2)
    # Suckers
    d.point((9, 6), fill=accent)
    d.point((10, 9), fill=accent)
    d.point((11, 11), fill=accent)

def draw_mute(d, accent):
    """Muted speech / silence manifestation."""
    d.ellipse([3, 4, 12, 11], outline=accent, width=1)
    d.line([4, 4, 12, 12], fill=accent, width=2)

def draw_whisper(d, accent):
    """Whisper / 3 dots small."""
    d.point((4, 6), fill=accent)
    d.point((5, 8), fill=accent)
    d.point((7, 5), fill=accent)
    d.point((8, 7), fill=accent)
    d.point((9, 9), fill=accent)
    d.point((11, 6), fill=accent)
    d.point((12, 8), fill=accent)

def draw_decay(d, accent):
    """Decay — broken/cracked square."""
    d.rectangle([3, 3, 12, 12], outline=accent)
    d.line([4, 5, 8, 9], fill=accent)
    d.line([10, 4, 7, 11], fill=accent)
    d.line([12, 8, 5, 12], fill=accent)

def draw_glimpse(d, accent):
    """Brief glimpse — flash/spark."""
    d.line([8, 2, 8, 14], fill=accent, width=2)
    d.line([2, 8, 14, 8], fill=accent, width=2)
    d.line([4, 4, 12, 12], fill=accent)
    d.line([12, 4, 4, 12], fill=accent)

def draw_amplify(d, accent):
    """Plus / amplify."""
    d.rectangle([6, 2, 9, 13], fill=accent)
    d.rectangle([2, 6, 13, 9], fill=accent)

def draw_linger(d, accent):
    """Hourglass — linger / duration."""
    d.polygon([(3, 3), (12, 3), (8, 8)], outline=accent, fill=tuple(int(c*0.5) for c in accent[:3]) + (180,))
    d.polygon([(3, 13), (12, 13), (7, 8)], outline=accent, fill=tuple(int(c*0.5) for c in accent[:3]) + (180,))

def draw_echo(d, accent):
    """Concentric ripples."""
    d.ellipse([6, 6, 9, 9], outline=accent, width=1)
    d.ellipse([4, 4, 11, 11], outline=accent, width=1)
    d.ellipse([2, 2, 13, 13], outline=accent, width=1)

def draw_secret(d, accent):
    """Sealed eye / cross."""
    d.ellipse([3, 5, 12, 10], outline=accent, width=1)
    d.line([2, 12, 13, 3], fill=accent, width=2)

def draw_hand(d, accent):
    """Touch — hand."""
    d.rectangle([5, 7, 10, 13], outline=accent, fill=tuple(int(c*0.4) for c in accent[:3]) + (200,))
    d.line([6, 7, 6, 3], fill=accent)
    d.line([7, 7, 7, 2], fill=accent)
    d.line([8, 7, 8, 2], fill=accent)
    d.line([9, 7, 9, 4], fill=accent)

def draw_self(d, accent):
    """Self — figure / circle around figure."""
    d.ellipse([2, 2, 13, 13], outline=accent, width=1)
    d.point((7, 7), fill=accent)
    d.line([7, 5, 8, 5], fill=accent)  # head
    d.line([7, 7, 8, 7], fill=accent)
    d.line([7, 9, 8, 9], fill=accent)

def draw_fire(d, accent):
    """Fire / ignite — flame."""
    d.polygon([(8, 2), (5, 6), (4, 9), (6, 12), (8, 14), (10, 12), (12, 9), (11, 6)],
              outline=accent, fill=tuple(int(c*0.6) for c in accent[:3]) + (220,))
    d.point((8, 6), fill=(255, 255, 200, 255))
    d.point((8, 10), fill=(255, 255, 200, 255))

def draw_sword(d, accent):
    """Harm — sword."""
    d.line([8, 2, 8, 11], fill=accent, width=2)
    d.line([6, 11, 10, 11], fill=accent, width=2)
    d.line([7, 12, 9, 12], fill=accent)
    d.line([7, 13, 9, 13], fill=accent)

def draw_heart(d, accent):
    """Heart — heal."""
    d.ellipse([3, 4, 8, 9], fill=accent)
    d.ellipse([8, 4, 13, 9], fill=accent)
    d.polygon([(3, 7), (13, 7), (8, 14)], fill=accent)

def draw_snowflake(d, accent):
    """Freeze — snowflake."""
    d.line([8, 2, 8, 14], fill=accent, width=2)
    d.line([2, 8, 14, 8], fill=accent, width=2)
    d.line([4, 4, 12, 12], fill=accent)
    d.line([12, 4, 4, 12], fill=accent)
    d.point((6, 8), fill=(255, 255, 255, 255))
    d.point((10, 8), fill=(255, 255, 255, 255))
    d.point((8, 6), fill=(255, 255, 255, 255))
    d.point((8, 10), fill=(255, 255, 255, 255))

def draw_arrow_up(d, accent):
    """Launch — arrow pointing up."""
    d.polygon([(8, 2), (4, 7), (6, 7), (6, 14), (10, 14), (10, 7), (12, 7)], fill=accent, outline=BORDER)

def draw_feather(d, accent):
    """Slowfall — feather curve."""
    d.line([4, 3, 12, 12], fill=accent, width=2)
    for i in range(5):
        d.line([5 + i, 4 + i, 8 + i, 4 + i], fill=accent)

def draw_laser(d, accent):
    """Laser — horizontal beam."""
    d.line([2, 8, 13, 8], fill=accent, width=3)
    d.line([13, 7, 14, 8], fill=accent)
    d.line([13, 9, 14, 8], fill=accent)
    d.point((1, 8), fill=accent)

def draw_burst(d, accent):
    """Burst — 8-pointed star."""
    cx, cy = 8, 8
    pts = [(cx, 2), (10, 6), (14, 8), (10, 10), (cx, 14), (6, 10), (2, 8), (6, 6)]
    for p in pts:
        d.line([(cx, cy), p], fill=accent, width=2)
    d.point((cx, cy), fill=(255, 255, 255, 255))

def draw_orbit(d, accent):
    """Orbit — circle around point."""
    d.ellipse([2, 2, 13, 13], outline=accent, width=1)
    d.ellipse([7, 7, 9, 9], fill=accent)
    d.point((2, 8), fill=accent)
    d.point((14, 8), fill=accent)
    d.point((8, 2), fill=accent)
    d.point((8, 14), fill=accent)

def draw_wall(d, accent):
    """Wall — vertical bars."""
    d.rectangle([3, 4, 5, 13], fill=accent, outline=BORDER)
    d.rectangle([7, 2, 9, 13], fill=accent, outline=BORDER)
    d.rectangle([11, 4, 13, 13], fill=accent, outline=BORDER)

def draw_chain(d, accent):
    """Chain — linked rings."""
    d.ellipse([2, 5, 7, 10], outline=accent, width=1)
    d.ellipse([8, 5, 13, 10], outline=accent, width=1)
    d.line([6, 7, 9, 7], fill=accent)

def draw_lightning(d, accent):
    """Lightning bolt."""
    d.polygon([(9, 2), (5, 8), (7, 8), (5, 14), (12, 7), (9, 7)],
              fill=accent, outline=BORDER)

def draw_gravity(d, accent):
    """Gravity well — concentric arrows in."""
    d.point((8, 8), fill=accent)
    d.line([2, 8, 6, 8], fill=accent)
    d.line([6, 7, 5, 8], fill=accent)
    d.line([6, 9, 5, 8], fill=accent)
    d.line([10, 8, 14, 8], fill=accent)
    d.line([10, 7, 11, 8], fill=accent)
    d.line([10, 9, 11, 8], fill=accent)
    d.line([8, 2, 8, 6], fill=accent)
    d.line([7, 6, 8, 5], fill=accent)
    d.line([9, 6, 8, 5], fill=accent)
    d.line([8, 10, 8, 14], fill=accent)
    d.line([7, 10, 8, 11], fill=accent)
    d.line([9, 10, 8, 11], fill=accent)

def draw_blind(d, accent):
    """Blind — eye with slash."""
    d.ellipse([3, 5, 12, 10], outline=accent, width=1)
    d.ellipse([6, 6, 9, 9], fill=accent)
    d.line([2, 12, 13, 3], fill=(255, 50, 50, 255), width=2)

def draw_levitate(d, accent):
    """Levitate — floating circle."""
    d.ellipse([5, 3, 10, 8], outline=accent, fill=tuple(int(c*0.5) for c in accent[:3]) + (180,))
    d.line([4, 10, 11, 10], fill=accent)
    d.line([5, 12, 10, 12], fill=accent)
    d.line([6, 14, 9, 14], fill=accent)

def draw_knockback(d, accent):
    """Knockback — radial arrows."""
    d.point((8, 8), fill=accent)
    # 4 arrows out
    d.line([8, 8, 2, 2], fill=accent, width=2)
    d.line([8, 8, 14, 2], fill=accent, width=2)
    d.line([8, 8, 2, 14], fill=accent, width=2)
    d.line([8, 8, 14, 14], fill=accent, width=2)

def draw_explosion(d, accent):
    """Explosion — jagged star."""
    pts = [(8, 1), (10, 5), (14, 5), (11, 8), (14, 12), (10, 11), (8, 14),
           (6, 11), (2, 12), (5, 8), (2, 5), (6, 5)]
    d.polygon(pts, fill=accent, outline=BORDER)

def draw_fangs(d, accent):
    """Fangs — two pointed teeth."""
    d.polygon([(3, 3), (5, 3), (4, 12)], fill=accent, outline=BORDER)
    d.polygon([(7, 3), (9, 3), (8, 12)], fill=accent, outline=BORDER)
    d.polygon([(11, 3), (13, 3), (12, 12)], fill=accent, outline=BORDER)

# ───────────── REGISTRY ─────────────
# (filename, type, accent_color, drawer)

# Custom accent colors per glyph for variety
GLYPHS = [
    # Methods (cool blues/cyans)
    ("glyph_direct_gaze",       1, (130, 80, 230, 255), draw_eye),
    ("glyph_watch_peripheral",  1, (100, 180, 230, 255), draw_eye_side),
    ("glyph_watch_memory",      1, (180, 130, 230, 255), draw_brain),
    ("glyph_watch_silence",     1, (200, 200, 220, 255), draw_shush),
    ("glyph_watch_reflection",  1, (150, 200, 230, 255), draw_mirror),
    ("glyph_method_touch",      1, (200, 230, 180, 255), draw_hand),
    ("glyph_method_self",       1, (230, 220, 130, 255), draw_self),
    ("glyph_method_laser",      1, (230, 100, 100, 255), draw_laser),
    ("glyph_method_burst",      1, (255, 140, 50, 255),  draw_burst),
    ("glyph_method_orbit",      1, (130, 130, 230, 255), draw_orbit),
    ("glyph_method_wall",       1, (160, 160, 200, 255), draw_wall),
    ("glyph_method_chain",      1, (210, 180, 130, 255), draw_chain),

    # Manifestations (hot magenta/red/orange)
    ("glyph_tendril",           5, (157, 77, 214, 255), draw_tendril),
    ("glyph_manifest_silence",  5, (180, 180, 200, 255), draw_mute),
    ("glyph_manifest_mirror",   5, (153, 136, 204, 255), draw_mirror),
    ("glyph_manifest_whisper",  5, (85, 68, 170, 255),   draw_whisper),
    ("glyph_manifest_decay",    5, (61, 96, 16, 255),    draw_decay),
    ("glyph_manifest_glimpse",  5, (235, 235, 100, 255), draw_glimpse),
    ("glyph_effect_ignite",     5, (255, 119, 51, 255),  draw_fire),
    ("glyph_effect_harm",       5, (187, 0, 0, 255),     draw_sword),
    ("glyph_effect_heal",       5, (85, 238, 85, 255),   draw_heart),
    ("glyph_effect_freeze",     5, (136, 221, 255, 255), draw_snowflake),
    ("glyph_effect_launch",     5, (255, 255, 170, 255), draw_arrow_up),
    ("glyph_effect_slowfall",   5, (238, 221, 255, 255), draw_feather),
    ("glyph_effect_lightning",  5, (255, 255, 100, 255), draw_lightning),
    ("glyph_effect_gravity",    5, (100, 50, 130, 255),  draw_gravity),
    ("glyph_effect_blind",      5, (40, 40, 50, 255),    draw_blind),
    ("glyph_effect_levitate",   5, (200, 200, 255, 255), draw_levitate),
    ("glyph_effect_knockback",  5, (255, 180, 100, 255), draw_knockback),
    ("glyph_effect_explosion",  5, (255, 80, 30, 255),   draw_explosion),
    ("glyph_effect_fangs",      5, (180, 50, 50, 255),   draw_fangs),

    # Distortions (gold/yellow)
    ("glyph_amplify",           10, (240, 200, 60, 255),  draw_amplify),
    ("glyph_linger",            10, (220, 180, 90, 255),  draw_linger),
    ("glyph_echo",              10, (200, 160, 120, 255), draw_echo),
    ("glyph_secret",            10, (130, 110, 60, 255),  draw_secret),
]

count = 0
for name, type_idx, accent, drawer in GLYPHS:
    img, draw = base_canvas(accent)
    drawer(draw, accent)
    out_path = os.path.join(OUT, f"{name}.png")
    img.save(out_path)
    count += 1

# Also generate a Spell Parchment texture
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
# Parchment scroll
draw.rectangle([2, 3, 13, 12], fill=(230, 210, 170, 255), outline=(120, 90, 50, 255))
# Top/bottom rolls
draw.rectangle([1, 2, 14, 4], fill=(180, 140, 80, 255), outline=(80, 50, 30, 255))
draw.rectangle([1, 11, 14, 13], fill=(180, 140, 80, 255), outline=(80, 50, 30, 255))
# Ink runes
draw.line([4, 6, 11, 6], fill=(60, 30, 100, 255))
draw.line([4, 8, 9, 8], fill=(60, 30, 100, 255))
draw.line([4, 10, 11, 10], fill=(60, 30, 100, 255))
img.save(os.path.join(OUT, "spell_parchment.png"))
count += 1

# Scribes Table block top texture
img = Image.new("RGBA", (SIZE, SIZE), (60, 40, 30, 255))
draw = ImageDraw.Draw(img)
# Wood grain
for x in range(SIZE):
    for y in range(SIZE):
        n = (x * 31 + y * 7) % 16
        if n > 12:
            img.putpixel((x, y), (90, 60, 40, 255))
        elif n < 3:
            img.putpixel((x, y), (40, 25, 20, 255))
# Engraved circle (rune)
draw.ellipse([4, 4, 11, 11], outline=(180, 150, 80, 255), width=1)
draw.point((7, 7), fill=(220, 200, 100, 255))
img.save(os.path.join(OUT, "scribes_table.png"))
count += 1

print(f"Generated {count} textures in {OUT}")
