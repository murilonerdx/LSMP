#!/usr/bin/env python3
"""
Gera 3 texturas 16x16 do Matter Purifier:
- matter_purifier_top.png  — face de cima, círculo brilhante violeta-cyan
- matter_purifier_bottom.png — face de baixo, escura com runas
- matter_purifier_side.png — laterais, painel de máquina com tubo de matter

Estética: moderna, alquímica, violeta+cyan+dourado.
"""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/liberthia/textures/block")
OUT.mkdir(parents=True, exist_ok=True)

TR = (0, 0, 0, 0)

# Paleta moderna
BL  = (8, 4, 16, 255)     # quase preto violeta
DK  = (20, 10, 38, 255)   # dark violeta
MD  = (50, 25, 80, 255)   # mid violeta
LT  = (110, 55, 160, 255) # light violeta
LL  = (170, 110, 220, 255)# lighter
CY  = (90, 200, 230, 255) # cyan brilho
GO  = (240, 200, 80, 255) # dourado
WH  = (230, 230, 255, 255)# branco-luz

# --------------- TOP — círculo brilhante alquímico
TOP = [
    "BBBBBBBBBBBBBBBB",
    "BDDDDDDDDDDDDDDB",
    "BDDMMMMMMMMMMDDB",
    "BDMM..CC..MMMMDB",
    "BDM.CC..CC.MMMDB",
    "BDM.C....C.MMMDB",
    "BDM.C.WW.C.MMMDB",
    "BDM.C.WW.C.MMMDB",
    "BDM.C....C.MMMDB",
    "BDM.CC..CC.MMMDB",
    "BDMM..CC..MMMMDB",
    "BDDMMMMMMMMMMDDB",
    "BDDDDDDDDDDDDDDB",
    "BDDLDDDDDDDDLDDB",
    "BDDDDDDDDDDDDDDB",
    "BBBBBBBBBBBBBBBB",
]

# --------------- SIDE — painel com tubo central
SIDE = [
    "BBBBBBBBBBBBBBBB",
    "BDDDDDDDDDDDDDDB",
    "BDMMMMMMMMMMMMMD",  # bordas
    "BDM..........MMD",
    "BDM..MMMMMM..MMD",
    "BDM..MLLLLM..MMD",
    "BDM..MLCCLM..MMD",
    "BDM..MLCCLM..MMD",
    "BDM..MLLLLM..MMD",
    "BDM..MMMMMM..MMD",
    "BDM..........MMD",
    "BDM.GG....GG.MMD",
    "BDM.GG....GG.MMD",
    "BDMMMMMMMMMMMMMD",
    "BDDDDDDDDDDDDDDB",
    "BBBBBBBBBBBBBBBB",
]

# --------------- BOTTOM — escuro com runas
BOTTOM = [
    "BBBBBBBBBBBBBBBB",
    "BDDDDDDDDDDDDDDB",
    "BDDDDDDDDDDDDDDB",
    "BDDMDDDDDDDDMDDB",
    "BDDMMDDDDDDDMMDB",
    "BDDDDDDDDDDDDDDB",
    "BDDDDDMMMMDDDDDB",
    "BDDDDMMMMMMDDDDB",
    "BDDDDMMMMMMDDDDB",
    "BDDDDDMMMMDDDDDB",
    "BDDDDDDDDDDDDDDB",
    "BDDMMDDDDDDDMMDB",
    "BDDMDDDDDDDDMDDB",
    "BDDDDDDDDDDDDDDB",
    "BDDDDDDDDDDDDDDB",
    "BBBBBBBBBBBBBBBB",
]

CMAP = {'B': BL, 'D': DK, 'M': MD, 'L': LT, 'l': LL, 'C': CY, 'G': GO, 'W': WH, '.': DK}

def gen(grid, name):
    img = Image.new("RGBA", (16, 16), TR)
    for y, row in enumerate(grid):
        row = (row + ".." * 16)[:16]
        for x, ch in enumerate(row):
            img.putpixel((x, y), CMAP.get(ch, TR))
    out = OUT / f"{name}.png"
    img.save(out, "PNG", optimize=False)
    print(f"  OK  {out}")

gen(TOP, "matter_purifier_top")
gen(SIDE, "matter_purifier_side")
gen(BOTTOM, "matter_purifier_bottom")
print("Done.")
