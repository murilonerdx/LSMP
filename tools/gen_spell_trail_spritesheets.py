"""r113: Animated spell trail spritesheets (8 frames × 7 schools).

Output: textures/particle/spell_trail_<school>.png  (16x128, 8 vertical frames)
        textures/particle/spell_trail_<school>.png.mcmeta  (animation 2t/frame)

Cada escola tem sua animacao unica desenhada from-scratch — não copiada de
nenhum outro mod. Estilo: orgânico, glow no centro, fade nas pontas.
"""

import os
import math
import random
import json
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(__file__)
OUT = os.path.normpath(os.path.join(
    ROOT, "..", "src", "main", "resources",
    "assets", "liberthia", "textures", "particle"))
os.makedirs(OUT, exist_ok=True)

FRAMES = 8
SIZE = 16


def lerp_rgb(a, b, t):
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
    )


def alpha_circle(img: Image.Image, cx, cy, radius, color, alpha=255, falloff=1.0):
    """Pinta um circle com falloff radial. Falloff 1.0 = linear, 0.5 = brusco."""
    d = ImageDraw.Draw(img)
    r = max(1, int(radius))
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if not (0 <= x < SIZE and 0 <= y < SIZE):
                continue
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            if dist > radius:
                continue
            f = 1.0 - (dist / radius)
            f = math.pow(f, falloff)
            a = int(alpha * f)
            if a <= 0:
                continue
            existing = img.getpixel((x, y))
            # Additive blend
            nr = min(255, existing[0] + int(color[0] * a / 255))
            ng = min(255, existing[1] + int(color[1] * a / 255))
            nb = min(255, existing[2] + int(color[2] * a / 255))
            na = min(255, existing[3] + a)
            img.putpixel((x, y), (nr, ng, nb, na))


def make_fire_frame(t):
    """Flame: pulsing core + flickering tongues."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pulse = 0.5 + 0.5 * math.sin(t * math.pi * 2)
    # Outer glow (red-orange)
    alpha_circle(img, 8, 8, 6 + pulse * 1.5, (180, 70, 20), 150, falloff=1.3)
    # Mid glow (orange)
    alpha_circle(img, 8, 8, 4 + pulse, (255, 130, 40), 200, falloff=1.0)
    # Hot core (yellow-white)
    alpha_circle(img, 8, 8, 2 + pulse * 0.6, (255, 230, 130), 255, falloff=0.7)
    # Flickering tongues (4 directions, alternate frames)
    angle_offset = t * math.pi * 2
    for i in range(4):
        a = angle_offset + i * math.pi / 2
        tx = 8 + int(math.cos(a) * 5)
        ty = 8 + int(math.sin(a) * 5)
        alpha_circle(img, tx, ty, 1.5 + pulse * 0.5, (255, 180, 60), 180, falloff=0.8)
    return img


def make_ice_frame(t):
    """Crystal star — 6-point rotating shard."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rot = t * math.pi / 4
    # Glow halo
    alpha_circle(img, 8, 8, 5, (60, 130, 200), 100, falloff=1.2)
    # Crystal points (6-star)
    for i in range(6):
        a = rot + i * math.pi / 3
        x2 = 8 + math.cos(a) * 6
        y2 = 8 + math.sin(a) * 6
        d.line([(8, 8), (x2, y2)], fill=(200, 240, 255, 230), width=1)
    # Inner glow
    alpha_circle(img, 8, 8, 2.5, (220, 255, 255), 255, falloff=0.6)
    # Sparkle pixels
    for _ in range(3):
        px = 8 + int(math.cos(rot * 3 + _) * 4)
        py = 8 + int(math.sin(rot * 3 + _) * 4)
        if 0 <= px < SIZE and 0 <= py < SIZE:
            img.putpixel((px, py), (255, 255, 255, 255))
    return img


def make_lightning_frame(t):
    """Electric arc — jagged streak with flicker."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    flicker = 1 if int(t * 8) % 2 == 0 else 0.7
    # Halo
    alpha_circle(img, 8, 8, 5, (180, 180, 50), int(80 * flicker), falloff=1.4)
    # Zigzag arc (procedural)
    random.seed(int(t * 7))
    pts = []
    n = 5
    for i in range(n):
        a = i / (n - 1)
        x = int(2 + a * 12)
        y = int(8 + (random.random() - 0.5) * 6)
        pts.append((x, y))
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=(255, 255, 200, int(255 * flicker)), width=1)
    # Bright core
    alpha_circle(img, 8, 8, 1.8, (255, 255, 180), 255, falloff=0.5)
    return img


def make_blood_frame(t):
    """Blood droplet — dark red pulse with dripping."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pulse = 0.5 + 0.5 * math.sin(t * math.pi * 2)
    # Outer dark halo
    alpha_circle(img, 8, 8, 6, (60, 0, 15), 120, falloff=1.4)
    # Mid red
    alpha_circle(img, 8, 8, 4 + pulse * 0.6, (150, 20, 40), 200, falloff=1.0)
    # Bright core
    alpha_circle(img, 8, 8, 2 + pulse * 0.4, (220, 40, 80), 255, falloff=0.6)
    # Drip below
    drip_y = int(10 + t * 4) % 14
    alpha_circle(img, 8, drip_y, 1.2, (180, 30, 60), 200, falloff=0.6)
    return img


def make_eldritch_frame(t):
    """Void ring — purple warping circle with chromatic edge."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    radius = 4 + math.sin(t * math.pi * 2) * 1.5
    # Outer halo
    alpha_circle(img, 8, 8, 6, (60, 0, 100), 100, falloff=1.4)
    # Ring (purple with magenta edge)
    for i in range(24):
        a = (i / 24) * math.pi * 2 + t * math.pi
        x = 8 + math.cos(a) * radius
        y = 8 + math.sin(a) * radius
        ix, iy = int(x), int(y)
        if 0 <= ix < SIZE and 0 <= iy < SIZE:
            img.putpixel((ix, iy), (180, 60, 240, 255))
    # Dark center (void)
    alpha_circle(img, 8, 8, 1.5, (20, 0, 30), 240, falloff=0.5)
    return img


def make_holy_frame(t):
    """Sacred starburst — gold rays + twinkle."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pulse = 0.5 + 0.5 * math.sin(t * math.pi * 2)
    # Halo
    alpha_circle(img, 8, 8, 6 + pulse, (220, 180, 60), 130, falloff=1.3)
    # 4 main rays
    for i in range(4):
        a = i * math.pi / 2 + t * 0.2
        x2 = 8 + math.cos(a) * (5 + pulse)
        y2 = 8 + math.sin(a) * (5 + pulse)
        d.line([(8, 8), (x2, y2)], fill=(255, 240, 180, 240), width=1)
    # 4 diagonal rays (smaller)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2 + t * 0.2
        x2 = 8 + math.cos(a) * (3 + pulse * 0.5)
        y2 = 8 + math.sin(a) * (3 + pulse * 0.5)
        d.line([(8, 8), (x2, y2)], fill=(255, 220, 150, 180), width=1)
    # Bright core
    alpha_circle(img, 8, 8, 2 + pulse * 0.5, (255, 255, 220), 255, falloff=0.6)
    return img


def make_nature_frame(t):
    """Leaf swirl — green vortex with petal motion."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Halo
    alpha_circle(img, 8, 8, 6, (40, 100, 30), 110, falloff=1.4)
    # 5 petals spiraling
    for i in range(5):
        a = t * math.pi * 2 + i * (math.pi * 2 / 5)
        r = 4 + math.sin(t * math.pi * 2 + i) * 1
        px = 8 + math.cos(a) * r
        py = 8 + math.sin(a) * r
        alpha_circle(img, int(px), int(py), 1.5, (100, 200, 80), 220, falloff=0.7)
    # Bright core
    alpha_circle(img, 8, 8, 2, (180, 240, 120), 255, falloff=0.6)
    return img


GENERATORS = {
    "fire":      make_fire_frame,
    "ice":       make_ice_frame,
    "lightning": make_lightning_frame,
    "blood":     make_blood_frame,
    "eldritch":  make_eldritch_frame,
    "holy":      make_holy_frame,
    "nature":    make_nature_frame,
}


def build_strip(name, gen_fn):
    strip = Image.new("RGBA", (SIZE, SIZE * FRAMES), (0, 0, 0, 0))
    for i in range(FRAMES):
        t = i / FRAMES
        frame = gen_fn(t)
        strip.paste(frame, (0, i * SIZE))
    out_png = os.path.join(OUT, f"spell_trail_{name}.png")
    strip.save(out_png)
    # mcmeta
    mcmeta = {
        "animation": {
            "frametime": 2,
            "interpolate": True,
            "frames": list(range(FRAMES))
        }
    }
    with open(out_png + ".mcmeta", "w") as f:
        json.dump(mcmeta, f, indent=2)


for name, fn in GENERATORS.items():
    build_strip(name, fn)
    print(f"  OK spell_trail_{name}.png ({SIZE}x{SIZE*FRAMES}) + mcmeta")

print(f"\nGenerated {len(GENERATORS)} animated spritesheets → {OUT}")
