"""Gera 5 texturas de background pros tabs de advancement.

Cada background eh um PNG tileable 16x16 ou 256x256 com tema da categoria.
"""
import os
from PIL import Image, ImageDraw
import random

OUT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..",
    "src/main/resources/assets/liberthia/textures/gui/advancements"))
os.makedirs(OUT, exist_ok=True)


def noise_bg(color_base, color_dark, color_accent, name, size=16):
    """Cria PNG 16x16 com noise stone-like."""
    img = Image.new("RGBA", (size, size), color_base + (255,))
    d = ImageDraw.Draw(img)
    random.seed(hash(name) % 10000)
    for _ in range(size * size // 3):
        x = random.randint(0, size - 1)
        y = random.randint(0, size - 1)
        c = random.choice([color_dark, color_accent, color_base])
        d.point((x, y), fill=c + (255,))
    # Add some "crack" diagonals
    for _ in range(2):
        x1 = random.randint(0, size - 1)
        y1 = random.randint(0, size - 1)
        x2 = x1 + random.randint(-4, 4)
        y2 = y1 + random.randint(-4, 4)
        d.line([(x1, y1), (x2, y2)], fill=color_dark + (255,))
    img.save(os.path.join(OUT, f"bg_{name}.png"))


# MATTER — dark purple stone tile
noise_bg((40, 25, 60), (20, 10, 35), (90, 60, 130), "matter")
# MAGIC — blue arcane
noise_bg((30, 50, 100), (10, 25, 55), (100, 130, 220), "magic")
# SPIRIT — soft pale lavender
noise_bg((60, 50, 90), (35, 25, 60), (140, 130, 200), "spirit")
# COSMIC — black/red void
noise_bg((25, 15, 25), (5, 0, 10), (90, 30, 50), "cosmic")
# BOSSES — gold/red royal
noise_bg((70, 40, 30), (40, 20, 15), (180, 120, 60), "bosses")

print(f"[OK] 5 advancement backgrounds gerados em {OUT}")
