#!/usr/bin/env python3
"""Gera texturas placeholder para as novas features v1.

Items:
- dark_matter_pendant.png  (moldura dourada + cristal violeta escuro)
- clear_matter_pendant.png (moldura dourada + cristal branco perolado)
- yellow_matter_pendant.png (moldura dourada + cristal dourado)
- reliquia_protecao_astaron.png (relíquia roxa em cinto)
- astaron_access_key.png   (chave roxa brilhante)
- pes_queimantes_astaron.png (bota com magma)
- flame_key.png            (chave dourada / vermelha)
- botas_mercuriais.png     (bota azul com pluma alquimica)
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).parent.parent
OUT = ROOT / "src/main/resources/assets/liberthia/textures/item"
OUT.mkdir(parents=True, exist_ok=True)


def img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def px(im, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), color)


def rect(im, x1, y1, x2, y2, color):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            px(im, x, y, color)


def make_pendant(name, cl, cm, cd, sparkle):
    """Cria pendant: corrente, anel dourado, cristal pontiagudo cor cl/cm/cd."""
    GOLD = (220, 180, 60, 255)
    GOLD_DARK = (160, 120, 30, 255)
    CHAIN = (180, 150, 60, 255)
    im = img()
    # Corrente em arco
    chain_pts = [(2, 3), (3, 2), (4, 1), (5, 1), (6, 0), (9, 0), (10, 1), (11, 1), (12, 2), (13, 3)]
    for x, y in chain_pts:
        px(im, x, y, CHAIN)
    # Anel dourado no topo do pingente
    for dx in range(6, 10):
        px(im, dx, 4, GOLD)
    px(im, 5, 4, GOLD_DARK)
    px(im, 10, 4, GOLD_DARK)
    px(im, 6, 5, GOLD_DARK)
    px(im, 9, 5, GOLD_DARK)
    px(im, 7, 5, GOLD)
    px(im, 8, 5, GOLD)
    # Cristal
    for x in range(5, 11):
        px(im, x, 6, cm)
    px(im, 4, 6, cd)
    px(im, 11, 6, cd)
    for x in range(4, 12):
        px(im, x, 7, cl)
    for x in range(4, 12):
        px(im, x, 8, cm)
    px(im, 6, 7, cl)
    px(im, 7, 7, sparkle)
    px(im, 8, 7, cl)
    px(im, 5, 8, cd)
    px(im, 10, 8, cd)
    for x in range(5, 11):
        px(im, x, 9, cm)
    for x in range(5, 11):
        px(im, x, 10, cd)
    for x in range(6, 10):
        px(im, x, 11, cd)
    for x in range(7, 9):
        px(im, x, 12, cm)
    px(im, 7, 13, cd)
    px(im, 4, 7, GOLD_DARK)
    px(im, 11, 7, GOLD_DARK)
    out = OUT / name
    im.save(out)
    print(f"OK -> {name}")


# Dark matter pendant — violeta escuro
make_pendant(
    "dark_matter_pendant.png",
    cl=(140, 80, 200, 255),
    cm=(90, 40, 140, 255),
    cd=(50, 20, 90, 255),
    sparkle=(220, 180, 255, 255),
)

# Clear matter pendant — branco perolado
make_pendant(
    "clear_matter_pendant.png",
    cl=(240, 245, 255, 255),
    cm=(200, 220, 240, 255),
    cd=(140, 170, 200, 255),
    sparkle=(255, 255, 255, 255),
)

# Yellow matter pendant — dourado intenso
make_pendant(
    "yellow_matter_pendant.png",
    cl=(255, 230, 90, 255),
    cm=(220, 180, 40, 255),
    cd=(150, 110, 20, 255),
    sparkle=(255, 250, 200, 255),
)

# ── Reliquia Astaron (cinto com pingente roxo) ──
im = img()
# Cinto de couro
BELT = (90, 50, 30, 255)
BELT_LIGHT = (130, 80, 50, 255)
BELT_DARK = (50, 25, 12, 255)
RELIC = (170, 50, 200, 255)
RELIC_LIGHT = (220, 130, 240, 255)
RELIC_DARK = (90, 20, 110, 255)
GOLD = (220, 180, 60, 255)
rect(im, 1, 6, 14, 9, BELT)
rect(im, 1, 6, 14, 6, BELT_LIGHT)
rect(im, 1, 9, 14, 9, BELT_DARK)
# Fivela dourada
rect(im, 6, 5, 9, 10, GOLD)
rect(im, 7, 7, 8, 8, BELT_DARK)
# Relíquia (cristal embaixo)
rect(im, 5, 10, 10, 13, RELIC)
rect(im, 6, 10, 9, 10, RELIC_LIGHT)
rect(im, 5, 13, 10, 13, RELIC_DARK)
px(im, 7, 11, RELIC_LIGHT)
px(im, 8, 11, RELIC_LIGHT)
px(im, 6, 14, RELIC_DARK)
px(im, 9, 14, RELIC_DARK)
px(im, 7, 14, RELIC)
px(im, 8, 14, RELIC)
im.save(OUT / "reliquia_protecao_astaron.png")
print("OK -> reliquia_protecao_astaron.png")

# ── Astaron Access Key — chave roxa brilhante ──
im = img()
KEY_GOLD = (220, 180, 60, 255)
KEY_GLOW = (200, 100, 240, 255)
KEY_DARK = (60, 30, 80, 255)
# Cabo (loop) no topo
rect(im, 3, 1, 6, 4, KEY_GOLD)
rect(im, 4, 2, 5, 3, KEY_GLOW)
# Haste
rect(im, 4, 5, 5, 12, KEY_GOLD)
# Dentes
rect(im, 6, 10, 7, 11, KEY_GOLD)
rect(im, 6, 12, 8, 13, KEY_GOLD)
# Glow
px(im, 4, 6, KEY_GLOW)
px(im, 5, 9, KEY_GLOW)
# Outline
for y in range(0, 15):
    if im.getpixel((3, y))[3] > 0:
        px(im, 2, y, KEY_DARK)
im.save(OUT / "astaron_access_key.png")
print("OK -> astaron_access_key.png")

# ── Pes Queimantes — bota com magma ──
im = img()
LAVA = (220, 60, 20, 255)
LAVA_LIGHT = (255, 160, 40, 255)
LAVA_DARK = (130, 30, 10, 255)
BOOT = (60, 30, 20, 255)
BOOT_DARK = (30, 15, 10, 255)
# Sola
rect(im, 2, 12, 13, 14, BOOT_DARK)
# Corpo da bota
rect(im, 3, 5, 12, 11, BOOT)
rect(im, 3, 5, 12, 5, LAVA)
# Magma core glowing
rect(im, 5, 7, 10, 10, LAVA)
rect(im, 6, 7, 9, 8, LAVA_LIGHT)
rect(im, 5, 10, 10, 10, LAVA_DARK)
px(im, 7, 8, LAVA_LIGHT)
px(im, 8, 8, LAVA_LIGHT)
# Bocal
rect(im, 3, 3, 6, 4, BOOT)
rect(im, 3, 3, 6, 3, LAVA)
im.save(OUT / "pes_queimantes_astaron.png")
print("OK -> pes_queimantes_astaron.png")

# ── Flame Key — chave laranja/vermelha com flame embaixo ──
im = img()
FK_GOLD = (220, 180, 60, 255)
FK_RED = (220, 60, 20, 255)
FK_ORANGE = (255, 140, 40, 255)
FK_DARK = (80, 30, 10, 255)
rect(im, 3, 1, 6, 4, FK_GOLD)
rect(im, 4, 2, 5, 3, FK_ORANGE)
rect(im, 4, 5, 5, 11, FK_GOLD)
# Flame teeth
rect(im, 6, 9, 7, 10, FK_RED)
rect(im, 6, 11, 8, 12, FK_RED)
px(im, 5, 13, FK_ORANGE)
px(im, 4, 14, FK_ORANGE)
px(im, 6, 14, FK_ORANGE)
px(im, 5, 14, FK_RED)
px(im, 5, 15, FK_RED)
# Outline
for y in range(0, 16):
    if im.getpixel((3, y))[3] > 0:
        px(im, 2, y, FK_DARK)
im.save(OUT / "flame_key.png")
print("OK -> flame_key.png")

# ── Botas Mercuriais — bota azul com pluma ──
im = img()
MERC_BLUE = (60, 140, 220, 255)
MERC_LIGHT = (140, 200, 250, 255)
MERC_DARK = (20, 60, 130, 255)
MERC_SILVER = (220, 230, 240, 255)
BOOT = (40, 70, 100, 255)
BOOT_DARK = (20, 35, 50, 255)
# Sola
rect(im, 2, 12, 13, 14, BOOT_DARK)
# Corpo bota
rect(im, 3, 5, 12, 11, BOOT)
rect(im, 3, 5, 12, 5, MERC_BLUE)
# Detalhes mercuriais
rect(im, 5, 7, 10, 10, MERC_BLUE)
rect(im, 6, 7, 9, 8, MERC_LIGHT)
rect(im, 5, 10, 10, 10, MERC_DARK)
px(im, 7, 8, MERC_SILVER)
px(im, 8, 8, MERC_SILVER)
# Pluma — fios pra cima
for y in range(0, 5):
    px(im, 13, y, MERC_LIGHT)
    px(im, 12, y, MERC_SILVER)
px(im, 14, 0, MERC_LIGHT)
px(im, 14, 2, MERC_LIGHT)
px(im, 11, 1, MERC_LIGHT)
px(im, 11, 3, MERC_LIGHT)
im.save(OUT / "botas_mercuriais.png")
print("OK -> botas_mercuriais.png")
