# -*- coding: utf-8 -*-
"""
Texturas da Lua do Medo:
  - fear_moon_drum.png  (item: tambor ritual com lua de sangue na pele)
  - medo.png            (icone 18x18 do efeito Medo: lua de sangue + face)
Renderiza em 4x (supersampling) pra antialias.
"""
import os
import math
from PIL import Image, ImageDraw

OUT_ITEM = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                        "assets", "liberthia", "textures", "item")
OUT_FX = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                      "assets", "liberthia", "textures", "mob_effect")
os.makedirs(OUT_ITEM, exist_ok=True)
os.makedirs(OUT_FX, exist_ok=True)

SS = 4
SIZE = 64
W = SIZE * SS

WOOD_D = (38, 22, 16, 255)
WOOD = (70, 42, 26, 255)
WOOD_HI = (104, 66, 40, 255)
SKIN = (214, 196, 168, 255)     # pele do tambor
SKIN_SH = (168, 150, 124, 255)
ROPE = (196, 176, 130, 255)
MOON = (170, 18, 24, 255)       # vermelho sangue
MOON_HI = (224, 60, 56, 255)
MOON_SH = (96, 8, 14, 255)
BLACK = (12, 6, 8, 255)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def draw_drum():
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = SS
    cx = 32 * s

    # corpo (barril) — duas bordas elipticas + lados
    top_y = 16 * s
    bot_y = 52 * s
    rx = 20 * s          # raio horizontal
    ry_top = 6 * s       # achatamento da elipse do topo
    ry_bot = 5 * s

    # lados do barril (gradiente madeira)
    for i in range(int(2 * rx)):
        x = cx - rx + i
        tt = i / (2 * rx)
        # leve curvatura do barril
        bulge = math.sin(tt * math.pi) * 2 * s
        col = lerp(WOOD_D, WOOD_HI, math.sin(tt * math.pi) * 0.9 + 0.05)
        d.line([(x, top_y - bulge * 0.2), (x, bot_y + bulge * 0.2)], fill=col, width=s + 1)

    # cordas (lacing) em zig-zag
    n = 8
    for k in range(n):
        x1 = cx - rx + (2 * rx) * (k / n)
        x2 = cx - rx + (2 * rx) * ((k + 0.5) / n)
        d.line([(x1, top_y + 1 * s), (x2, bot_y - 1 * s)], fill=ROPE, width=max(1, int(1.0 * s)))
        d.line([(x2, bot_y - 1 * s), (cx - rx + (2 * rx) * ((k + 1) / n), top_y + 1 * s)],
               fill=ROPE, width=max(1, int(1.0 * s)))

    # aros (madeira escura) topo e base
    d.ellipse([cx - rx, bot_y - ry_bot, cx + rx, bot_y + ry_bot], fill=WOOD_D)
    d.ellipse([cx - rx, top_y - ry_top, cx + rx, top_y + ry_top], outline=WOOD_D, width=int(2.2 * s))

    # pele do tambor (elipse do topo) com sombreado radial
    d.ellipse([cx - rx + 1 * s, top_y - ry_top + 1 * s, cx + rx - 1 * s, top_y + ry_top - 1 * s],
              fill=SKIN)
    d.ellipse([cx - rx + 1 * s, top_y - ry_top + 2.4 * s, cx + rx - 1 * s, top_y + ry_top - 0.5 * s],
              fill=SKIN_SH)
    d.ellipse([cx - rx + 1 * s, top_y - ry_top + 1 * s, cx + rx - 1 * s, top_y + ry_top - 2.2 * s],
              fill=SKIN)

    # lua de sangue pintada na pele (elipse achatada como a perspectiva)
    mr = 11 * s
    mry = 3.4 * s
    d.ellipse([cx - mr, top_y - mry, cx + mr, top_y + mry], fill=MOON)
    d.ellipse([cx - mr, top_y - mry, cx + mr, top_y + mry], outline=MOON_SH, width=int(0.8 * s))
    # crescente escuro (sombra) deslocado
    d.ellipse([cx - mr + 5 * s, top_y - mry, cx + mr + 5 * s, top_y + mry], fill=SKIN_SH)
    # respingo de highlight
    d.ellipse([cx - mr + 2 * s, top_y - mry + 0.8 * s, cx - mr + 6 * s, top_y + mry - 0.8 * s],
              fill=MOON_HI)

    return img.resize((SIZE, SIZE), Image.LANCZOS)


def draw_medo():
    f = 18 * SS
    img = Image.new("RGBA", (f, f), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = f / 2
    # lua de sangue cheia
    r = 7.4 * SS
    d.ellipse([c - r, c - r, c + r, c + r], fill=MOON)
    d.ellipse([c - r, c - r, c + r, c + r], outline=MOON_SH, width=int(1.4 * SS))
    # halo highlight
    d.ellipse([c - r + 1.4 * SS, c - r + 1.4 * SS, c - r + 5.5 * SS, c - r + 5.5 * SS], fill=MOON_HI)
    # face apavorada (dois olhos arregalados + boca aberta) em preto
    eye_r = 1.5 * SS
    for ex in (-2.7, 2.7):
        d.ellipse([c + ex * SS - eye_r, c - 1.6 * SS - eye_r,
                   c + ex * SS + eye_r, c - 1.6 * SS + eye_r], fill=BLACK)
        d.ellipse([c + ex * SS - 0.5 * SS, c - 1.6 * SS - 0.5 * SS,
                   c + ex * SS + 0.5 * SS, c - 1.6 * SS + 0.5 * SS], fill=(255, 255, 255, 255))
    # boca (grito) oval
    d.ellipse([c - 2.2 * SS, c + 1.6 * SS, c + 2.2 * SS, c + 5.2 * SS], fill=BLACK)
    return img.resize((18, 18), Image.LANCZOS)


def main():
    draw_drum().save(os.path.join(OUT_ITEM, "fear_moon_drum.png"))
    print("ok: fear_moon_drum")
    draw_medo().save(os.path.join(OUT_FX, "medo.png"))
    print("ok: medo (effect icon)")


if __name__ == "__main__":
    main()
