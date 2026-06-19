#!/usr/bin/env python3
"""
Gera src/main/resources/assets/liberthia/textures/item/pipe_filter.png

Pixel-art 16x16 — irmão do pipe_filter_not:
- Mesmo quadro de filtro cinza
- Atravessado por um ✓ verde (em vez de X vermelho)
"""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/liberthia/textures/item/pipe_filter.png")
OUT.parent.mkdir(parents=True, exist_ok=True)

TR = (0, 0, 0, 0)
BL = (0, 0, 0, 255)
GR_DARK = (60, 60, 70, 255)
GR_MID  = (110, 110, 130, 255)
GR_LIT  = (170, 170, 190, 255)
GN_DARK = (20, 100, 30, 255)
GN_LIT  = (60, 200, 80, 255)
GN_HI   = (140, 255, 150, 255)

# Layout: 16x16. .=transparente, B=preto outline, d/m/l=cinza, G/g/h=verde tons.
# Desenho de check-mark verde sobre frame de filtro cinza.
GRID = [
    "................",
    "..BBBBBBBBBBBB..",
    ".BllllllllllllB.",
    ".BlmmmmmmmmmmlB.",
    ".BlmddddddddmlB.",
    ".BlmdddddddGdmB.",
    ".BlmddddddGGdmB.",
    ".BlmGddddGggddB.",
    ".BlmGGdGgggdddB.",
    ".BlmdGGgggdddmB.",
    ".BlmddGgddddmlB.",
    ".BlmdddGdddmllB.",
    ".BlmmmmmmmmmmlB.",
    ".BllllllllllllB.",
    "..BBBBBBBBBBBB..",
    "................",
]

CMAP = {
    '.': TR, 'B': BL,
    'd': GR_DARK, 'm': GR_MID, 'l': GR_LIT,
    'G': GN_DARK, 'g': GN_LIT, 'h': GN_HI,
}

img = Image.new("RGBA", (16, 16), TR)
for y, row in enumerate(GRID):
    row = (row + ".." * 16)[:16]
    for x, ch in enumerate(row):
        img.putpixel((x, y), CMAP.get(ch, TR))

img.save(OUT, "PNG")
print(f"[texture] {OUT}  16x16 PNG saved")
