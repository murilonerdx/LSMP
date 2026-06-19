#!/usr/bin/env python3
"""
Gera texturas 16x16 dos 3 purified matter ingots.

Visual: lingote estilo Minecraft (forma de ingot) com tons da matéria
correspondente + brilho/highlight central pra indicar "purificado".
"""
from pathlib import Path
from PIL import Image

OUT_DIR = Path("src/main/resources/assets/liberthia/textures/item")
OUT_DIR.mkdir(parents=True, exist_ok=True)

TR = (0, 0, 0, 0)

# Paletas
PALETTES = {
    "purified_dark_matter_ingot": {
        # Roxo profundo + violeta brilhante + branco-luz no centro
        'B': (15, 5, 25, 255),       # outline
        'd': (40, 15, 60, 255),      # dark base
        'm': (75, 30, 110, 255),     # mid
        'l': (130, 70, 180, 255),    # light
        'h': (210, 170, 255, 255),   # highlight cyan-violet
        's': (255, 240, 255, 255),   # shine
    },
    "purified_clear_matter_ingot": {
        # Azul gelo + cyan + branco brilhante
        'B': (10, 20, 40, 255),
        'd': (60, 110, 150, 255),
        'm': (130, 180, 220, 255),
        'l': (200, 230, 245, 255),
        'h': (240, 250, 255, 255),
        's': (255, 255, 255, 255),
    },
    "purified_yellow_matter_ingot": {
        # Dourado + amarelo + branco-quente
        'B': (40, 20, 0, 255),
        'd': (140, 90, 10, 255),
        'm': (220, 170, 30, 255),
        'l': (255, 220, 80, 255),
        'h': (255, 245, 180, 255),
        's': (255, 255, 220, 255),
    },
}

# Forma de ingot — pixel art horizontal com bordas chanfradas e brilho central
GRID = [
    "................",
    "................",
    "................",
    "...BBBBBBBBBB...",
    "..BdmmllllmmdB..",
    ".BdmllhhsshlmdB.",
    ".BdmlhshshslmdB.",
    ".BdmllhhsshlmdB.",
    "..BdmmllllmmdB..",
    "...BBBBBBBBBB...",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]

for name, palette in PALETTES.items():
    img = Image.new("RGBA", (16, 16), TR)
    for y, row in enumerate(GRID):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            col = palette.get(ch, TR)
            img.putpixel((x, y), col)
    out_path = OUT_DIR / f"{name}.png"
    img.save(out_path, "PNG", optimize=False)
    print(f"  OK  {out_path}")
print("Done.")
