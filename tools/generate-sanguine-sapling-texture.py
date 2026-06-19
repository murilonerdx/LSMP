#!/usr/bin/env python3
"""
Gera textura 16x16 do sanguine_sapling.

v2 (v0.1.20 fix): garante alpha=0 nos pixels transparentes via
Image.new('RGBA', (16,16), (0,0,0,0)) E putpixel explícito com tupla 4-canais.

Bug reportado: "a sapling está ficando com as laterais pretas" — causado
por pixels com alpha alto mas RGB preto (0,0,0,255) em vez de transparente
(0,0,0,0). O Minecraft renderiza sprite/cross usando alpha channel — se
estiver opaco, vira retângulo preto em vez de transparente.
"""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/liberthia/textures/block/sanguine_sapling.png")
OUT.parent.mkdir(parents=True, exist_ok=True)

# IMPORTANTE: alpha=0 explícito em TR
TR = (0, 0, 0, 0)
# Folhas — gradiente vermelho-sangue, todos com alpha=255
LF_DK = (90, 10, 15, 255)
LF_MD = (160, 20, 30, 255)
LF_LT = (210, 40, 55, 255)
LF_HI = (240, 80, 95, 255)
# Tronco — castanho avermelhado
TR_DK = (60, 30, 25, 255)
TR_LT = (100, 50, 35, 255)

# Layout estilo Minecraft sapling vanilla — copa + tronco fino
# . = transparente (alpha=0)
GRID = [
    "................",
    "......LLLL......",
    ".....LMMLML.....",
    "....LMmMHLML....",
    "...LmMHHMmmML...",
    "..LMHHmMMMHMML..",
    "..LMHmMMMHMmML..",
    "...LMmMHHMmmL...",
    ".....LMMLML.....",
    "......LMTL......",
    ".......T.T......",
    ".......T........",
    "......TtT.......",
    ".....TtTt.......",
    "......TT........",
    "................",
]

CMAP = {
    '.': TR,
    'L': LF_DK,
    'M': LF_MD,
    'm': LF_LT,
    'H': LF_HI,
    'T': TR_DK,
    't': TR_LT,
}

# Cria com fundo totalmente transparente
img = Image.new("RGBA", (16, 16), TR)

for y, row in enumerate(GRID):
    # Garante 16 colunas, padding com transparência
    row = (row + "." * 16)[:16]
    for x, ch in enumerate(row):
        col = CMAP.get(ch, TR)
        # Force tupla de 4 elementos
        if len(col) == 3:
            col = (col[0], col[1], col[2], 255)
        img.putpixel((x, y), col)

# Save sem otimização que pode comer alpha
img.save(OUT, "PNG", optimize=False)

# Verifica que pixels transparentes têm alpha=0 mesmo
verify = Image.open(OUT)
print(f"[texture] {OUT}  mode={verify.mode}")
opaque = sum(1 for p in verify.convert('RGBA').getdata() if p[3] > 0)
transparent = sum(1 for p in verify.convert('RGBA').getdata() if p[3] == 0)
print(f"           opaque={opaque}, transparent={transparent}, total={opaque+transparent}")
