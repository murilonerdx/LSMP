# -*- coding: utf-8 -*-
"""
Textura dos Punhos Celestiais (celestial_fists.png) — uma manopla/punho
dourado celestial com energia ciano nas juntas, bracelete com gema e
estrelinhas. Renderiza 4x (supersampling).
"""
import os
import math
from PIL import Image, ImageDraw

OUT_ITEM = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                        "assets", "liberthia", "textures", "item")
os.makedirs(OUT_ITEM, exist_ok=True)

SS = 4
SIZE = 64
W = SIZE * SS

GOLD_D = (150, 104, 30, 255)
GOLD = (214, 168, 64, 255)
GOLD_HI = (255, 230, 140, 255)
PLATE_D = (120, 84, 24, 255)
CYAN = (90, 220, 245, 255)
CYAN_HI = (200, 250, 255, 255)
WHITE = (255, 255, 255, 255)
OUTLINE = (54, 34, 10, 255)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def rrect(d, x0, y0, x1, y1, r, fill):
    d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=fill)


def star(d, cx, cy, r, col):
    pts = []
    for i in range(8):
        ang = math.pi / 4 * i
        rr = r if i % 2 == 0 else r * 0.4
        pts.append((cx + math.cos(ang) * rr, cy + math.sin(ang) * rr))
    d.polygon(pts, fill=col)


def main():
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = SS

    # --- estrelas celestiais de fundo ---
    for (sx, sy, sr) in [(10, 12, 1.6), (54, 16, 2.0), (50, 50, 1.4), (12, 50, 1.7), (44, 9, 1.1)]:
        star(d, sx * s, sy * s, sr * s, lerp(CYAN, WHITE, 0.4))

    # --- bracelete (cuff) embaixo ---
    rrect(d, 18 * s, 44 * s, 46 * s, 56 * s, 4 * s, OUTLINE)
    rrect(d, 19 * s, 45 * s, 45 * s, 55 * s, 3 * s, GOLD_D)
    rrect(d, 20 * s, 46 * s, 44 * s, 51 * s, 2 * s, GOLD)
    # gema ciano no centro do bracelete
    d.ellipse([29 * s, 47 * s, 35 * s, 53 * s], fill=OUTLINE)
    d.ellipse([30 * s, 48 * s, 34 * s, 52 * s], fill=CYAN)
    d.ellipse([30.6 * s, 48.4 * s, 32.4 * s, 50.2 * s], fill=CYAN_HI)

    # --- bloco do punho (mão fechada) ---
    fx0, fy0, fx1, fy1 = 17 * s, 20 * s, 47 * s, 46 * s
    rrect(d, fx0 - 1 * s, fy0 - 1 * s, fx1 + 1 * s, fy1 + 1 * s, 7 * s, OUTLINE)
    # gradiente vertical dourado
    for i in range(int(fy1 - fy0)):
        y = fy0 + i
        tt = i / (fy1 - fy0)
        col = lerp(GOLD_D, GOLD, 1 - abs(tt - 0.4) * 1.3)
        d.line([(fx0, y), (fx1, y)], fill=col)
    rrect(d, fx0, fy0, fx1, fy1, 6 * s, None)
    # brilho diagonal
    d.line([(fx0 + 4 * s, fy1 - 3 * s), (fx1 - 5 * s, fy0 + 3 * s)],
           fill=GOLD_HI, width=int(1.4 * s))

    # --- 4 juntas (knuckles) no topo ---
    kc = []
    for k in range(4):
        kx = fx0 + 5 * s + k * 6.5 * s
        ky = fy0 + 1 * s
        kc.append((kx, ky))
        d.ellipse([kx - 3.2 * s, ky - 3.2 * s, kx + 3.2 * s, ky + 3.2 * s], fill=OUTLINE)
        d.ellipse([kx - 2.6 * s, ky - 2.6 * s, kx + 2.6 * s, ky + 2.6 * s], fill=GOLD)
        d.ellipse([kx - 2.6 * s, ky - 2.6 * s, kx + 1.0 * s, ky + 1.0 * s], fill=GOLD_HI)
    # energia ciano nas frestas entre as juntas
    for a in range(3):
        gx = (kc[a][0] + kc[a + 1][0]) / 2
        d.line([(gx, fy0 + 2 * s), (gx, fy0 + 14 * s)], fill=CYAN, width=int(1.3 * s))
        d.line([(gx, fy0 + 3 * s), (gx, fy0 + 11 * s)], fill=CYAN_HI, width=max(1, int(0.5 * s)))

    # --- polegar (lado esquerdo) ---
    rrect(d, 14 * s, 30 * s, 21 * s, 41 * s, 3 * s, OUTLINE)
    rrect(d, 15 * s, 31 * s, 20 * s, 40 * s, 2 * s, GOLD)
    d.line([(16 * s, 39 * s), (19 * s, 33 * s)], fill=GOLD_HI, width=max(1, int(0.9 * s)))

    # --- seam de energia central no dorso ---
    d.line([(32 * s, fy0 + 8 * s), (32 * s, fy1 - 4 * s)], fill=CYAN, width=int(1.2 * s))
    d.ellipse([30 * s, 34 * s, 34 * s, 38 * s], fill=CYAN)
    d.ellipse([30.6 * s, 34.6 * s, 32.6 * s, 36.6 * s], fill=CYAN_HI)

    img = img.resize((SIZE, SIZE), Image.LANCZOS)
    img.save(os.path.join(OUT_ITEM, "celestial_fists.png"))
    print("ok: celestial_fists")


if __name__ == "__main__":
    main()
