"""
Gera 4 texturas de Sample Vial em 16x16:
 - sample_vial.png            → vazio (frasco transparente)
 - sample_vial_filled_dark.png   → roxo (matéria escura)
 - sample_vial_filled_white.png  → branco (matéria branca)
 - sample_vial_filled_yellow.png → amarelo (matéria amarela)

E também redesenha o Dark Matter Shard pra algo mais bonito (cristal facetado).

Usa só PIL — sem deps externas.
"""
from PIL import Image, ImageDraw
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "liberthia", "textures", "item")
os.makedirs(OUT, exist_ok=True)

# ----------- SAMPLE VIAL -----------
# Forma do frasco (compartilhada). Coords em pixels (16x16).
# - Rolha em cima (rgba marrom)
# - Pescoço fino
# - Corpo arredondado com líquido dentro
# - Brilho à esquerda

VIAL_OUTLINE = (40, 30, 60, 255)       # roxo bem escuro pra contorno do vidro
VIAL_GLASS   = (180, 170, 220, 90)     # vidro semi-transparente
VIAL_HIGHLIGHT = (255, 255, 255, 200)
CORK         = (90, 55, 30, 255)
CORK_DARK    = (60, 35, 15, 255)

def make_vial(fluid_color, empty=False):
    """fluid_color = (r,g,b,a) ou None pra vazio."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # Layout (y de cima → baixo):
    # 0-1: rolha (cork) 3 pixels de largura
    # 2-3: pescoço estreito
    # 4-13: corpo do frasco (com líquido se filled)
    # 14: base

    # Rolha (cork) — 3 pixels centrais
    for x in range(6, 10):
        px[x, 0] = CORK_DARK
        px[x, 1] = CORK
    px[6, 1] = CORK_DARK  # contorno
    px[9, 1] = CORK_DARK

    # Pescoço (2 colunas centrais) y=2,3
    for y in range(2, 4):
        px[6, y] = VIAL_OUTLINE
        px[9, y] = VIAL_OUTLINE
        if not empty:
            # Líquido sobe um pouquinho pelo pescoço pra dar charme
            if y == 3:
                px[7, y] = fluid_color
                px[8, y] = fluid_color
            else:
                px[7, y] = VIAL_GLASS
                px[8, y] = VIAL_GLASS
        else:
            px[7, y] = VIAL_GLASS
            px[8, y] = VIAL_GLASS

    # Ombros (y=4) — alarga
    for x in range(5, 11):
        if x in (5, 10):
            px[x, 4] = VIAL_OUTLINE
        else:
            if empty:
                px[x, 4] = VIAL_GLASS
            else:
                px[x, 4] = fluid_color

    # Corpo (y=5..13) — 6 cols largura
    for y in range(5, 14):
        # Bordas
        px[4, y] = VIAL_OUTLINE
        px[11, y] = VIAL_OUTLINE
        for x in range(5, 11):
            if empty:
                px[x, y] = VIAL_GLASS
            else:
                # Nível do líquido sobe de baixo pra cima. Topo do líquido em y=5.
                px[x, y] = fluid_color
        # Brilho na esquerda
        if not empty and y in (6, 7, 8):
            r, g, b, a = fluid_color
            highlight = (min(r + 60, 255), min(g + 60, 255), min(b + 60, 255), a)
            px[5, y] = highlight

    # Base (y=14)
    for x in range(4, 12):
        px[x, 14] = VIAL_OUTLINE
    # Sombra do fundo
    if not empty:
        for x in range(5, 11):
            r, g, b, a = fluid_color
            shadow = (max(r - 40, 0), max(g - 40, 0), max(b - 40, 0), a)
            px[x, 13] = shadow

    return img


# Texturas
empty_img    = make_vial(None, empty=True)
dark_img     = make_vial((130, 50, 200, 255))   # roxo viv
white_img    = make_vial((230, 230, 255, 255))  # branco azulado leve
yellow_img   = make_vial((255, 220, 60, 255))   # amarelo dourado

empty_img.save(os.path.join(OUT, "sample_vial.png"))
dark_img.save(os.path.join(OUT, "sample_vial_filled_dark.png"))
white_img.save(os.path.join(OUT, "sample_vial_filled_white.png"))
yellow_img.save(os.path.join(OUT, "sample_vial_filled_yellow.png"))

# Mantém sample_vial_filled.png como fallback = dark (compatibilidade pra
# clientes em cache antigo, e pro caso de cargas mistas).
dark_img.save(os.path.join(OUT, "sample_vial_filled.png"))

print("[OK]Sample vial textures: empty, dark, white, yellow")


# ----------- DARK MATTER SHARD -----------
# Cristal facetado roxo com brilho — mais bonito que a textura antiga.

def make_shard():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # Paleta roxa
    OUTLINE   = (15, 0, 30, 255)
    DEEP      = (45, 5, 80, 255)
    BASE      = (90, 25, 150, 255)
    MID       = (140, 50, 210, 255)
    LIGHT     = (200, 130, 255, 255)
    BRIGHT    = (240, 200, 255, 255)
    GLOW      = (255, 255, 255, 200)

    # Shape: cristal alongado vertical com facetas
    # Topo afunilado, meio largo, base afunilada.
    #
    #         x
    #        / \
    #       / | \
    #      /  |  \
    #      \  |  /
    #       \ | /
    #        \|/
    #         x

    # Define mapa do contorno (1=outline, 2=deep, 3=base, 4=mid, 5=light, 6=bright, 7=glow)
    shape = [
        "................",
        "................",
        ".......11.......",
        "......1221......",
        ".....122231.....",
        "....1223341.....",
        "....1233451.....",
        "...12345561.....",
        "...12345561.....",
        "....1234451.....",
        "....1223341.....",
        ".....122331.....",
        "......1221......",
        ".......11.......",
        "................",
        "................",
    ]
    palette = {
        '.': (0, 0, 0, 0),
        '1': OUTLINE,
        '2': DEEP,
        '3': BASE,
        '4': MID,
        '5': LIGHT,
        '6': BRIGHT,
        '7': GLOW,
    }
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            px[x, y] = palette[ch]

    # Glow points (pontos brilhantes)
    px[7, 6] = GLOW
    px[8, 8] = (255, 255, 255, 150)

    return img

shard = make_shard()
shard.save(os.path.join(OUT, "dark_matter_shard.png"))
print("[OK]Dark Matter Shard texture redesenhado")

print("\nDone!")
