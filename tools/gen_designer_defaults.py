#!/usr/bin/env python3
"""r158: Gera assets default usados pelo Spell Designer GUI.

- data/liberthia/schools/*.png       — 7 glifos de escola (transparent bg)
- data/liberthia/runes/*.png          — 10 runas genéricas (transparent bg)
- data/liberthia/particle_config/*.json — 5 perfis de partícula
- data/liberthia/elements/*.png       — 10 elementos
- data/liberthia/threads/*.png        — 6 threads (componentes de tecido mágico)
- data/liberthia/focuses/*.png        — 5 focuses (foci of channel)

Tudo 32×32 transparent PNG para uso flexível no compositor.
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\data\liberthia")
SCHOOLS_DIR = ROOT / "schools"
RUNES_DIR = ROOT / "runes"
PARTICLE_DIR = ROOT / "particle_config"
ELEMENTS_DIR = ROOT / "elements"
THREADS_DIR = ROOT / "threads"
FOCUSES_DIR = ROOT / "focuses"

for d in [SCHOOLS_DIR, RUNES_DIR, PARTICLE_DIR, ELEMENTS_DIR, THREADS_DIR, FOCUSES_DIR]:
    d.mkdir(parents=True, exist_ok=True)

SIZE = 32

# ═════════════════════════════════════════════════════════════════════════
# School glyphs — 7 escolas com símbolo único
# ═════════════════════════════════════════════════════════════════════════
SCHOOLS = {
    "fire":      {"color": (255, 102, 51),  "shape": "flame"},
    "ice":       {"color": (102, 204, 255), "shape": "crystal"},
    "lightning": {"color": (255, 255, 68),  "shape": "bolt"},
    "blood":     {"color": (153, 0, 51),    "shape": "drop"},
    "eldritch":  {"color": (122, 61, 255),  "shape": "eye"},
    "holy":      {"color": (255, 238, 170), "shape": "cross"},
    "nature":    {"color": (51, 170, 51),   "shape": "leaf"},
}


def draw_glow_outline(d, x, y, color, alpha=120):
    """Anel de glow ao redor do ponto."""
    r, g, b = color
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            if dx == 0 and dy == 0:
                continue
            d.point((x + dx, y + dy), fill=(r, g, b, alpha))


def gen_school(name, color, shape):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = SIZE // 2, SIZE // 2
    r, g, b = color
    bright = (min(255, r + 60), min(255, g + 60), min(255, b + 60), 255)
    base = (r, g, b, 255)

    # Anel externo (todas escolas tem isso)
    for ang in range(0, 360, 4):
        rad = math.radians(ang)
        x = int(cx + math.cos(rad) * 14)
        y = int(cy + math.sin(rad) * 14)
        d.point((x, y), fill=base)
    # Anel interno fino
    for ang in range(0, 360, 12):
        rad = math.radians(ang)
        x = int(cx + math.cos(rad) * 10)
        y = int(cy + math.sin(rad) * 10)
        d.point((x, y), fill=(r, g, b, 180))

    # Shape específico ao centro
    if shape == "flame":
        # Chama: triângulo pra cima com ondas
        for i in range(8):
            d.point((cx, cy - 6 + i), fill=base)
            d.point((cx - 1, cy - 4 + i), fill=base)
            d.point((cx + 1, cy - 4 + i), fill=base)
        d.point((cx - 2, cy + 2), fill=base)
        d.point((cx + 2, cy + 2), fill=base)
    elif shape == "crystal":
        # Cristal: losango
        for i in range(7):
            d.point((cx - i, cy + i - 6), fill=base)
            d.point((cx + i, cy + i - 6), fill=base)
        d.point((cx, cy + 1), fill=bright)
    elif shape == "bolt":
        # Raio: linha zig-zag
        pts = [(0, -7), (-2, -3), (1, -1), (-2, 3), (3, 6)]
        for (dx, dy) in pts:
            d.point((cx + dx, cy + dy), fill=base)
            d.point((cx + dx, cy + dy + 1), fill=base)
    elif shape == "drop":
        # Gota de sangue
        d.ellipse((cx - 4, cy - 2, cx + 4, cy + 6), fill=base)
        d.point((cx, cy - 5), fill=base)
        d.point((cx, cy - 4), fill=base)
        d.point((cx - 1, cy - 3), fill=base)
        d.point((cx + 1, cy - 3), fill=base)
    elif shape == "eye":
        # Olho eldritch: elipse com íris
        d.ellipse((cx - 7, cy - 3, cx + 7, cy + 3), outline=base, width=1)
        d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=base)
        d.point((cx, cy), fill=(255, 255, 255, 255))
    elif shape == "cross":
        # Cruz divina
        for i in range(-6, 7):
            d.point((cx, cy + i), fill=base)
        for i in range(-5, 6):
            d.point((cx + i, cy), fill=base)
        # Halo
        d.point((cx, cy - 7), fill=bright)
        d.point((cx, cy + 7), fill=bright)
    elif shape == "leaf":
        # Folha: gota assimétrica com nervura
        d.ellipse((cx - 5, cy - 6, cx + 5, cy + 6), outline=base, width=1)
        for i in range(-5, 6):
            d.point((cx, cy + i), fill=base)
        d.point((cx - 2, cy - 2), fill=base)
        d.point((cx + 2, cy - 2), fill=base)
        d.point((cx - 2, cy + 2), fill=base)
        d.point((cx + 2, cy + 2), fill=base)

    img.save(SCHOOLS_DIR / f"{name}.png")


# ═════════════════════════════════════════════════════════════════════════
# Runes — 10 padrões geométricos genéricos
# ═════════════════════════════════════════════════════════════════════════
RUNES = ["pentagram", "hexagram", "circle", "triangle", "square",
         "cross", "spiral", "infinity", "arrow", "wave"]


def gen_rune(name):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = SIZE // 2, SIZE // 2
    color = (200, 200, 200, 255)
    if name == "pentagram":
        pts = []
        for i in range(5):
            ang = -math.pi / 2 + i * 2 * math.pi / 5
            pts.append((cx + math.cos(ang) * 12, cy + math.sin(ang) * 12))
        order = [0, 2, 4, 1, 3, 0]
        for i in range(len(order) - 1):
            d.line([pts[order[i]], pts[order[i + 1]]], fill=color, width=1)
    elif name == "hexagram":
        pts = []
        for i in range(6):
            ang = i * 2 * math.pi / 6
            pts.append((cx + math.cos(ang) * 12, cy + math.sin(ang) * 12))
        d.polygon([pts[0], pts[2], pts[4]], outline=color, width=1)
        d.polygon([pts[1], pts[3], pts[5]], outline=color, width=1)
    elif name == "circle":
        d.ellipse((cx - 12, cy - 12, cx + 12, cy + 12), outline=color, width=1)
        d.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), outline=color, width=1)
    elif name == "triangle":
        pts = [(cx, cy - 12), (cx - 10, cy + 8), (cx + 10, cy + 8)]
        d.polygon(pts, outline=color, width=1)
        d.ellipse((cx - 3, cy - 1, cx + 3, cy + 5), outline=color, width=1)
    elif name == "square":
        d.rectangle((cx - 10, cy - 10, cx + 10, cy + 10), outline=color, width=1)
        d.line([(cx - 10, cy - 10), (cx + 10, cy + 10)], fill=color, width=1)
        d.line([(cx + 10, cy - 10), (cx - 10, cy + 10)], fill=color, width=1)
    elif name == "cross":
        d.line([(cx, cy - 12), (cx, cy + 12)], fill=color, width=1)
        d.line([(cx - 12, cy), (cx + 12, cy)], fill=color, width=1)
        d.line([(cx - 8, cy - 8), (cx + 8, cy + 8)], fill=color, width=1)
        d.line([(cx + 8, cy - 8), (cx - 8, cy + 8)], fill=color, width=1)
    elif name == "spiral":
        for t in range(80):
            ang = t * 0.3
            r = 0.18 * t
            x = int(cx + math.cos(ang) * r)
            y = int(cy + math.sin(ang) * r)
            if 0 <= x < SIZE and 0 <= y < SIZE:
                d.point((x, y), fill=color)
    elif name == "infinity":
        for t in range(100):
            ang = t * math.pi * 2 / 100
            r = 10
            x = int(cx + r * math.cos(ang))
            y = int(cy + r * math.sin(ang) * math.cos(ang))
            if 0 <= x < SIZE and 0 <= y < SIZE:
                d.point((x, y), fill=color)
    elif name == "arrow":
        d.line([(cx, cy - 10), (cx, cy + 10)], fill=color, width=2)
        d.line([(cx, cy - 10), (cx - 5, cy - 5)], fill=color, width=2)
        d.line([(cx, cy - 10), (cx + 5, cy - 5)], fill=color, width=2)
    elif name == "wave":
        for x in range(4, 28):
            y = int(cy + math.sin(x * 0.5) * 6)
            d.point((x, y), fill=color)
            d.point((x, y + 1), fill=color)
    img.save(RUNES_DIR / f"{name}.png")


# ═════════════════════════════════════════════════════════════════════════
# Elements — 10 elementos (FIRE+ICE=STEAM etc.)
# ═════════════════════════════════════════════════════════════════════════
ELEMENTS = {
    "earth":   (139, 90, 43),
    "water":   (52, 152, 219),
    "wind":    (160, 220, 255),
    "void":    (43, 0, 64),
    "steam":   (200, 220, 240),
    "mud":     (120, 80, 50),
    "lava":    (255, 80, 0),
    "frost":   (180, 230, 255),
    "spark":   (255, 230, 100),
    "thorn":   (50, 120, 30),
}


def gen_element(name, color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = SIZE // 2, SIZE // 2
    # Hex with element initial-letter pattern
    pts = []
    for i in range(6):
        ang = i * math.pi / 3
        pts.append((cx + math.cos(ang) * 12, cy + math.sin(ang) * 12))
    d.polygon(pts, outline=(*color, 255), width=2)
    # Inner dot cluster
    for i in range(3):
        ang = i * math.pi * 2 / 3
        x = int(cx + math.cos(ang) * 5)
        y = int(cy + math.sin(ang) * 5)
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=(*color, 255))
    img.save(ELEMENTS_DIR / f"{name}.png")


# ═════════════════════════════════════════════════════════════════════════
# Threads — 6 thread types (componentes do weave)
# ═════════════════════════════════════════════════════════════════════════
THREADS = {
    "silver":   (220, 220, 240),
    "golden":   (255, 200, 60),
    "crimson":  (200, 30, 60),
    "azure":    (60, 130, 220),
    "shadow":   (40, 40, 60),
    "ethereal": (180, 200, 255),
}


def gen_thread(name, color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Thread: braided weave pattern
    for x in range(2, 30):
        y1 = 16 + int(math.sin(x * 0.5) * 6)
        y2 = 16 + int(math.cos(x * 0.5) * 6)
        d.point((x, y1), fill=(*color, 255))
        d.point((x, y1 + 1), fill=(*color, 255))
        d.point((x, y2), fill=(*color, 200))
    img.save(THREADS_DIR / f"{name}.png")


# ═════════════════════════════════════════════════════════════════════════
# Focuses — 5 channel foci
# ═════════════════════════════════════════════════════════════════════════
FOCUSES = {
    "concentration": (100, 220, 100),
    "destruction":   (220, 80, 80),
    "protection":    (220, 220, 100),
    "manipulation":  (140, 80, 220),
    "transmutation": (220, 140, 80),
}


def gen_focus(name, color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = SIZE // 2, SIZE // 2
    # Concentric octagon-ish lens
    for i, sz in enumerate([13, 10, 6]):
        alpha = 255 - i * 40
        d.ellipse((cx - sz, cy - sz, cx + sz, cy + sz), outline=(*color, alpha), width=1)
    # Center crystal
    d.polygon([(cx, cy - 4), (cx + 4, cy), (cx, cy + 4), (cx - 4, cy)],
              fill=(*color, 255))
    img.save(FOCUSES_DIR / f"{name}.png")


# ═════════════════════════════════════════════════════════════════════════
# Particle config presets
# ═════════════════════════════════════════════════════════════════════════
PARTICLE_PRESETS = {
    "minimal": {
        "name": "Minimal",
        "trail_density": 4, "trail_size": 0.8,
        "impact_scale": 0.7, "impact_particles": 25,
        "screen_shake": 0.1, "impact_light": 8
    },
    "standard": {
        "name": "Standard",
        "trail_density": 10, "trail_size": 1.2,
        "impact_scale": 1.0, "impact_particles": 60,
        "screen_shake": 0.3, "impact_light": 10
    },
    "intense": {
        "name": "Intense",
        "trail_density": 20, "trail_size": 1.6,
        "impact_scale": 1.5, "impact_particles": 120,
        "screen_shake": 0.6, "impact_light": 13
    },
    "devastation": {
        "name": "Devastation",
        "trail_density": 35, "trail_size": 2.2,
        "impact_scale": 2.5, "impact_particles": 250,
        "screen_shake": 1.2, "impact_light": 15
    },
    "subtle_aura": {
        "name": "Subtle Aura",
        "trail_density": 6, "trail_size": 0.9,
        "impact_scale": 0.5, "impact_particles": 15,
        "screen_shake": 0.0, "impact_light": 6
    },
}


def main():
    print("Generating school glyphs...")
    for name, conf in SCHOOLS.items():
        gen_school(name, conf["color"], conf["shape"])
    print(f"  OK {len(SCHOOLS)} schools")

    print("Generating runes...")
    for name in RUNES:
        gen_rune(name)
    print(f"  OK {len(RUNES)} runes")

    print("Generating elements...")
    for name, color in ELEMENTS.items():
        gen_element(name, color)
    print(f"  OK {len(ELEMENTS)} elements")

    print("Generating threads...")
    for name, color in THREADS.items():
        gen_thread(name, color)
    print(f"  OK {len(THREADS)} threads")

    print("Generating focuses...")
    for name, color in FOCUSES.items():
        gen_focus(name, color)
    print(f"  OK {len(FOCUSES)} focuses")

    print("Generating particle presets...")
    for fname, data in PARTICLE_PRESETS.items():
        (PARTICLE_DIR / f"{fname}.json").write_text(
            json.dumps(data, indent=2), encoding="utf-8")
    print(f"  OK {len(PARTICLE_PRESETS)} particle presets")

    print("\nAll default assets generated.")


if __name__ == "__main__":
    main()
