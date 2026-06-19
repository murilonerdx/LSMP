"""
r179: Regenera as texturas WORN (vestidas) das armaduras do Culto (blood) e da
Ordem (order) — report #54 (gabisousa02: "armaduras nao tem textura").

As worn-layers antigas (de 20/04) eram esparsas/ruidosas → davam impressão de
"sem textura" no corpo. Aqui pintamos as REGIÕES UV CANÔNICAS do HumanoidArmorModel
(64x32) sólidas, com gradiente vertical + contorno + faixa de trim, forçando
opacidade total. Como OrderArmorItem/BloodArmorItem são ArmorItem vanilla (sem
renderer custom), o layout UV padrão se aplica.

Layout 64x32 (mesma malha do skin clássico, lado esquerdo = espelho do direito):
  HEAD (capacete)  : x0-31  y0-15
  BODY (peito)     : x16-39 y16-31
  ARM  (braço dir) : x40-55 y16-31   (espelha pro esquerdo)
  LEG  (perna dir) : x0-15  y16-31   (espelha pra esquerda)

  layer_1 = capacete + peito + braços + botas (parte baixa da perna)
  layer_2 = leggings  = cintura (body) + pernas inteiras
"""
import os
from PIL import Image

BASE = os.path.join(os.path.dirname(__file__), "..",
                    "src", "main", "resources", "assets", "liberthia",
                    "textures", "models", "armor")

HEAD = (0, 0, 31, 15)
BODY = (16, 16, 39, 31)
ARM  = (40, 16, 55, 31)
LEG  = (0, 16, 15, 31)

def clamp(c): return max(0, min(255, int(c)))

def shade(rgb, t, lighten=0.28, darken=0.42):
    """t=0 topo(claro) .. 1 base(escuro)."""
    if t < 0.5:
        f = 1.0 + lighten * (1 - 2 * t)
    else:
        f = 1.0 - darken * (2 * t - 1)
    return tuple(clamp(c * f) for c in rgb)

def brushed(rgb, x):
    """Leve modulação por coluna pra dar textura de metal escovado."""
    m = 1.0 + (0.06 if (x % 2 == 0) else -0.06)
    return tuple(clamp(c * m) for c in rgb)

def fill_box(px, box, base, trim, y0=None, y1=None, band=None, band_color=None):
    bx0, by0, bx1, by1 = box
    if y0 is None: y0 = by0
    if y1 is None: y1 = by1
    H = max(1, y1 - y0)
    for y in range(y0, y1 + 1):
        t = (y - y0) / H
        for x in range(bx0, bx1 + 1):
            col = brushed(shade(base, t), x)
            px[x, y] = (col[0], col[1], col[2], 255)
    # contorno (definição das peças)
    for x in range(bx0, bx1 + 1):
        px[x, y0] = (*trim, 255)
        px[x, y1] = (*trim, 255)
    for y in range(y0, y1 + 1):
        px[bx0, y] = (*trim, 255)
        px[bx1, y] = (*trim, 255)
    # faixa de trim (cinto / detalhe)
    if band is not None and y0 <= band <= y1:
        bc = band_color if band_color else trim
        for x in range(bx0, bx1 + 1):
            px[x, band] = (*bc, 255)

def make_layer1(path, base, trim, band_color):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    fill_box(px, HEAD, base, trim, band=10, band_color=band_color)   # visor do capacete
    fill_box(px, BODY, base, trim, band=27, band_color=band_color)   # cinto no peito
    fill_box(px, ARM,  base, trim)                                   # braços
    fill_box(px, LEG,  base, trim, y0=24, y1=31, band=25, band_color=band_color)  # botas
    im.save(path)
    return path

def make_layer2(path, base, trim, band_color):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    fill_box(px, BODY, base, trim, y0=22, y1=31, band=23, band_color=band_color)  # cintura/leggings
    fill_box(px, LEG,  base, trim, band=20, band_color=band_color)                # pernas inteiras
    im.save(path)
    return path

# ── Paletas ──
# Ordem: paladino sagrado — branco-prata + ouro
ORDER_BASE = (214, 211, 198)
ORDER_TRIM = (198, 154, 60)
ORDER_BAND = (236, 200, 110)
# Culto: sangue — carmesim escuro + preto + sangue vivo
BLOOD_BASE = (122, 26, 30)
BLOOD_TRIM = (30, 9, 11)
BLOOD_BAND = (190, 44, 44)

def main():
    out = []
    out.append(make_layer1(os.path.join(BASE, "order_layer_1.png"), ORDER_BASE, ORDER_TRIM, ORDER_BAND))
    out.append(make_layer2(os.path.join(BASE, "order_layer_2.png"), ORDER_BASE, ORDER_TRIM, ORDER_BAND))
    out.append(make_layer1(os.path.join(BASE, "blood_layer_1.png"), BLOOD_BASE, BLOOD_TRIM, BLOOD_BAND))
    out.append(make_layer2(os.path.join(BASE, "blood_layer_2.png"), BLOOD_BASE, BLOOD_TRIM, BLOOD_BAND))
    for p in out:
        im = Image.open(p).convert("RGBA")
        opaque = sum(1 for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0)
        print("OK", os.path.basename(p), im.size, "opacos=%d" % opaque)

if __name__ == "__main__":
    main()
