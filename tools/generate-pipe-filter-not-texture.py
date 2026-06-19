#!/usr/bin/env python3
"""
Gera src/main/resources/assets/liberthia/textures/item/pipe_filter_not.png

Pixel-art 16x16:
- Quadro de filtro (ícone tipo "funil/peneira" cinza)
- Atravessado por um X vermelho indicando NEGATIVE
- Sem antialias (estilo Minecraft)
"""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/liberthia/textures/item/pipe_filter_not.png")
OUT.parent.mkdir(parents=True, exist_ok=True)

TR = (0, 0, 0, 0)
BL = (0, 0, 0, 255)
GR_DARK = (60, 60, 70, 255)
GR_MID  = (110, 110, 130, 255)
GR_LIT  = (170, 170, 190, 255)
RED_DARK = (140, 20, 20, 255)
RED_LIT  = (230, 50, 50, 255)
RED_HI   = (255, 120, 120, 255)

# Layout: 16x16 grid. cada char = 1 pixel:
# . = transparente
# B = preto (outline)
# d = cinza escuro
# m = cinza médio
# l = cinza claro
# R = vermelho escuro
# r = vermelho claro
# h = vermelho highlight (centro do X)
GRID = [
    "................",
    "..BBBBBBBBBBBB..",
    ".BllllllllllllB.",
    ".BlmmmmmmmmmmlB.",
    ".BlmRdddddddmlB.",
    ".BlmddRddddRlB.",
    ".BlmdddRddRdmlB",
    ".BlmddddRrddmlB",
    ".BlmddddhRddmlB",
    ".BlmddddRRddmlB",
    ".BlmdddRddRdmlB",
    ".BlmddRddddRlB.",
    ".BlmRdddddddmlB.",
    ".BlmmmmmmmmmmlB.",
    ".BllllllllllllB.",
    "..BBBBBBBBBBBB..",
]

CMAP = {
    '.': TR,
    'B': BL,
    'd': GR_DARK,
    'm': GR_MID,
    'l': GR_LIT,
    'R': RED_DARK,
    'r': RED_LIT,
    'h': RED_HI,
}

img = Image.new("RGBA", (16, 16), TR)
for y, row in enumerate(GRID):
    # Pad ou trunca pra exatamente 16 colunas
    row = (row + ".." * 16)[:16]
    for x, ch in enumerate(row):
        img.putpixel((x, y), CMAP.get(ch, TR))

img.save(OUT, "PNG")
print(f"[texture] {OUT}  16x16 PNG saved")
