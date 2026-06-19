#!/usr/bin/env python3
"""Gera textura do White Matter Pendant — colar com lasca de cristal branca."""
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).parent.parent
OUT = ROOT / "src/main/resources/assets/liberthia/textures/item/white_matter_pendant.png"
OUT.parent.mkdir(parents=True, exist_ok=True)

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = img.load()
GOLD = (220, 180, 60, 255)
GOLD_DARK = (160, 120, 30, 255)
CRYSTAL_LIGHT = (240, 245, 255, 255)
CRYSTAL_MID = (200, 220, 240, 255)
CRYSTAL_DARK = (140, 170, 200, 255)
CHAIN = (180, 150, 60, 255)

# Corrente em arco no topo (linha curva via pontos manuais)
chain_pts = [(2, 3), (3, 2), (4, 1), (5, 1), (6, 0), (9, 0), (10, 1), (11, 1), (12, 2), (13, 3)]
for x, y in chain_pts:
    px[x, y] = CHAIN

# Anel de ouro no topo do pingente
for dx in range(6, 10):
    px[dx, 4] = GOLD
px[5, 4] = GOLD_DARK; px[10, 4] = GOLD_DARK
px[6, 5] = GOLD_DARK; px[9, 5] = GOLD_DARK
px[7, 5] = GOLD; px[8, 5] = GOLD

# Cristal branco (lasca pontiaguda)
# Top wide row
for x in range(5, 11):
    px[x, 6] = CRYSTAL_MID
px[4, 6] = CRYSTAL_DARK; px[11, 6] = CRYSTAL_DARK
# Body
for x in range(4, 12):
    px[x, 7] = CRYSTAL_LIGHT
for x in range(4, 12):
    px[x, 8] = CRYSTAL_MID
# Highlights / facets
px[6, 7] = CRYSTAL_LIGHT
px[7, 7] = (255, 255, 255, 255)  # bright spot
px[8, 7] = CRYSTAL_LIGHT
px[5, 8] = CRYSTAL_DARK
px[10, 8] = CRYSTAL_DARK
# Mid
for x in range(5, 11):
    px[x, 9] = CRYSTAL_MID
# Lower body tapers
for x in range(5, 11):
    px[x, 10] = CRYSTAL_DARK
for x in range(6, 10):
    px[x, 11] = CRYSTAL_DARK
# Tip
for x in range(7, 9):
    px[x, 12] = CRYSTAL_MID
px[7, 13] = CRYSTAL_DARK

# Gold edge on left/right
px[4, 7] = GOLD_DARK
px[11, 7] = GOLD_DARK

img.save(OUT)
print(f"OK -> {OUT.name} ({OUT.stat().st_size}B)")
