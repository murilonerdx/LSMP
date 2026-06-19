#!/usr/bin/env python3
"""Gera texturas dos items novos v1:
   - vision_swap_lens.png — lente com cristal branco no meio + brilho
   - possession_amulet.png — amuleto roxo escuro com olho central
Ambas 16×16 PNG.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).parent.parent
OUT_DIR = ROOT / "src/main/resources/assets/liberthia/textures/item"
OUT_DIR.mkdir(parents=True, exist_ok=True)


# ─────────────────────────────────────────────────────────────────────────
# 1) Vision Swap Lens — lente circular (anel dourado), cristal branco central,
#    brilho leve. Estilo "espia óptica mágica".
# ─────────────────────────────────────────────────────────────────────────
def gen_vision_swap_lens():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # paleta
    FRAME = (210, 175, 50, 255)        # dourado
    FRAME_DARK = (140, 110, 25, 255)   # dourado sombra
    GLASS_OUTER = (220, 240, 255, 255) # vidro azul-claro
    GLASS_INNER = (245, 250, 255, 255) # vidro mais claro
    CRYSTAL = (255, 255, 255, 255)     # branco puro do cristal
    GLOW = (200, 220, 255, 200)        # brilho semi-transparente

    # Construímos um círculo aproximado em uma grade 16x16
    # ring shape (centro 8,8 raio ~6)
    ring_outer = [
        (4,3),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,3),
        (12,4),(12,5),(13,6),(13,7),(13,8),(13,9),(12,10),(12,11),
        (11,12),(10,13),(9,13),(8,13),(7,13),(6,13),(5,13),(4,12),
        (3,11),(3,10),(2,9),(2,8),(2,7),(2,6),(3,5),(3,4),
    ]
    for x, y in ring_outer:
        px[x, y] = FRAME

    # ring shadow
    ring_shadow = [(4,4),(11,4),(4,11),(11,11),(2,5),(13,5),(2,10),(13,10)]
    for x, y in ring_shadow:
        px[x, y] = FRAME_DARK

    # glass body
    for y in range(4, 12):
        for x in range(4, 12):
            # skip already-set pixels (frame)
            if px[x, y] != (0, 0, 0, 0):
                continue
            # check distance from center
            dx = x - 7.5
            dy = y - 7.5
            d2 = dx * dx + dy * dy
            if d2 <= 14:
                if d2 <= 4:
                    px[x, y] = GLASS_INNER
                else:
                    px[x, y] = GLASS_OUTER

    # central crystal (3x3 pixel dot)
    px[7, 7] = CRYSTAL
    px[8, 7] = CRYSTAL
    px[7, 8] = CRYSTAL
    px[8, 8] = CRYSTAL
    # tiny diagonal sparkle highlights
    px[6, 6] = GLOW
    px[9, 9] = GLOW
    # tiny glint top-left
    px[6, 8] = GLOW

    out = OUT_DIR / "vision_swap_lens.png"
    img.save(out)
    print(f"OK -> {out.name} ({out.stat().st_size}B)")


# ─────────────────────────────────────────────────────────────────────────
# 2) Possession Amulet — amuleto pendente roxo-escuro c/ olho central
#    (esclera branca, íris roxa). Chain pequena em cima.
# ─────────────────────────────────────────────────────────────────────────
def gen_possession_amulet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    CHAIN = (110, 90, 130, 255)
    PURPLE_DARK = (40, 15, 65, 255)
    PURPLE_MID = (70, 30, 110, 255)
    PURPLE_LIGHT = (100, 55, 150, 255)
    EYE_WHITE = (235, 220, 240, 255)
    IRIS = (165, 75, 220, 255)
    PUPIL = (15, 5, 25, 255)

    # Chain on top — 4 pixel chain in V shape
    for x, y in [(5, 1), (10, 1), (6, 2), (9, 2), (7, 3), (8, 3)]:
        px[x, y] = CHAIN

    # Amulet body — oval/teardrop ~ 7 wide x 10 tall, centered at (7.5, 9)
    body = [
        # row 4 (top widest just under chain)
        (5,4),(6,4),(7,4),(8,4),(9,4),(10,4),
        # rows 5-12 forming an oval
        (4,5),(5,5),(6,5),(7,5),(8,5),(9,5),(10,5),(11,5),
        (4,6),(11,6),
        (4,7),(11,7),
        (4,8),(11,8),
        (4,9),(11,9),
        (4,10),(11,10),
        (5,11),(10,11),
        (5,12),(6,12),(7,12),(8,12),(9,12),(10,12),
        (6,13),(7,13),(8,13),(9,13),
        (7,14),(8,14),
    ]
    for x, y in body:
        px[x, y] = PURPLE_DARK

    # Fill the inside with mid/light purple gradient
    inner = [
        (5,6),(6,6),(7,6),(8,6),(9,6),(10,6),
        (5,7),(6,7),(7,7),(8,7),(9,7),(10,7),
        (5,8),(6,8),(7,8),(8,8),(9,8),(10,8),
        (5,9),(6,9),(7,9),(8,9),(9,9),(10,9),
        (5,10),(6,10),(7,10),(8,10),(9,10),(10,10),
        (6,11),(7,11),(8,11),(9,11),
    ]
    for x, y in inner:
        # default mid
        px[x, y] = PURPLE_MID

    # Highlight strip top-left
    for x, y in [(5,6),(6,6),(5,7)]:
        px[x, y] = PURPLE_LIGHT
    # subtle shadow on bottom-right
    for x, y in [(10, 10), (9, 11)]:
        px[x, y] = PURPLE_DARK

    # Central eye — large eye in middle
    # esclera (white)
    eye_w = [(6,7),(7,7),(8,7),(9,7),(6,8),(9,8),(6,9),(7,9),(8,9),(9,9)]
    for x, y in eye_w:
        px[x, y] = EYE_WHITE
    # iris (purple)
    for x, y in [(7,8),(8,8)]:
        px[x, y] = IRIS
    # pupil (single dot center-ish)
    px[7, 8] = PUPIL

    # Eye outline (slight dark above and below)
    for x in range(5, 11):
        # already outline by body, skip
        pass

    out = OUT_DIR / "possession_amulet.png"
    img.save(out)
    print(f"OK -> {out.name} ({out.stat().st_size}B)")


if __name__ == "__main__":
    gen_vision_swap_lens()
    gen_possession_amulet()
