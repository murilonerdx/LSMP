# -*- coding: utf-8 -*-
"""
Gera as texturas do Arco da Cacadora (Huntress's Bow):
  - huntress_bow.png            (relaxado)
  - huntress_bow_pulling_0..2   (tracao crescente, com flecha nockada)
  - marked.png                  (icone 18x18 do efeito Marcado)

Tema: madeira escura + limbos esmeralda com pontas douradas + corda marfim +
detalhe rúnico dourado no grip. Renderiza em 4x (supersampling) pra antialias.
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

SIZE = 64
SS = 4                      # supersampling
W = SIZE * SS

# paleta
WOOD_DARK = (40, 26, 18, 255)
WOOD = (74, 48, 30, 255)
EMER_DARK = (16, 78, 60, 255)
EMER = (38, 150, 110, 255)
EMER_HI = (120, 224, 175, 255)
GOLD = (214, 170, 70, 255)
GOLD_HI = (255, 226, 140, 255)
STRING = (232, 224, 200, 255)
STRING_HI = (255, 255, 240, 255)
ARROW_SHAFT = (150, 110, 70, 255)
ARROW_HEAD = (210, 220, 230, 255)
FLETCH = (200, 70, 70, 255)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def qbez(p0, p1, p2, t):
    u = 1 - t
    x = u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0]
    y = u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1]
    return (x, y)


def disc(d, cx, cy, r, col):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)


def thick_curve(d, p0, p1, p2, width_fn, color_fn, n=160):
    """Desenha curva quadratica como sequencia de discos (espessura/cor variaveis)."""
    for i in range(n + 1):
        t = i / n
        x, y = qbez(p0, p1, p2, t)
        disc(d, x, y, width_fn(t), color_fn(t))


def draw_bow(draw_pct, with_arrow):
    """draw_pct: 0=relaxado .. 1=tracao maxima. Retorna Image 64x64."""
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = SS

    # tips e bulge — arco em "C" fundo abrindo pra DIREITA (corda quase
    # vertical na direita, limbo bojudo na esquerda) = leitura clara de arco.
    top = (46 * s, 7 * s)
    bot = (42 * s, 57 * s)
    bulge = (5 * s, 32 * s)          # controle: empurra o arco bem pra esquerda

    # --- sombra/outline do limbo ---
    thick_curve(d, top, bulge, bot,
                lambda t: 4.6 * s, lambda t: WOOD_DARK, n=180)
    # --- corpo do limbo (gradiente madeira->esmeralda com pontas douradas) ---
    def limb_w(t):
        # mais grosso no meio (grip), afina nas pontas
        return (2.0 + 2.6 * math.sin(math.pi * t)) * s

    def limb_c(t):
        # pontas douradas, meio esmeralda, base madeira sutil
        edge = min(t, 1 - t) * 2          # 0 nas pontas, 1 no meio
        if edge < 0.22:
            return lerp(GOLD, GOLD_HI, 0.3)
        base = lerp(EMER_DARK, EMER, 0.5 + 0.5 * math.sin(math.pi * t))
        return base

    thick_curve(d, top, bulge, bot, limb_w, limb_c, n=200)

    # highlight (sheen) deslocado pra dentro do arco
    def hi_w(t):
        return max(0.5, (0.8 + 0.9 * math.sin(math.pi * t))) * s
    thick_curve(d,
                (top[0] + 2 * s, top[1] + 1 * s),
                (bulge[0] + 4 * s, bulge[1]),
                (bot[0] + 2 * s, bot[1] - 1 * s),
                hi_w, lambda t: EMER_HI if 0.15 < t < 0.85 else GOLD_HI, n=160)

    # nocks dourados nas pontas
    disc(d, top[0], top[1], 2.6 * s, GOLD)
    disc(d, top[0], top[1], 1.3 * s, GOLD_HI)
    disc(d, bot[0], bot[1], 2.6 * s, GOLD)
    disc(d, bot[0], bot[1], 1.3 * s, GOLD_HI)

    # grip enrolado no meio
    gx, gy = qbez(top, bulge, bot, 0.5)
    for k in range(-3, 4):
        ang = math.atan2(bot[1] - top[1], bot[0] - top[0]) + math.pi / 2
        ox = math.cos(ang) * k * 1.7 * s
        oy = math.sin(ang) * k * 1.7 * s
        disc(d, gx + ox, gy + oy, 3.0 * s, WOOD)
    for k in range(-3, 4, 2):
        ang = math.atan2(bot[1] - top[1], bot[0] - top[0]) + math.pi / 2
        ox = math.cos(ang) * k * 1.7 * s
        oy = math.sin(ang) * k * 1.7 * s
        disc(d, gx + ox, gy + oy, 1.1 * s, GOLD)

    # --- corda ---
    # ponto de puxada: do chord (relaxado) ate bem a direita (tracao)
    chord_mid = ((top[0] + bot[0]) / 2, (top[1] + bot[1]) / 2)
    pull_target = (chord_mid[0] + 13 * s, chord_mid[1])
    draw_pt = (chord_mid[0] + (pull_target[0] - chord_mid[0]) * draw_pct,
               chord_mid[1])
    if draw_pct <= 0.02:
        d.line([top, bot], fill=STRING, width=max(1, int(1.2 * s)))
        d.line([top, bot], fill=STRING_HI, width=max(1, int(0.5 * s)))
    else:
        for col, wd in ((STRING, 1.3), (STRING_HI, 0.5)):
            d.line([top, draw_pt], fill=col, width=max(1, int(wd * s)))
            d.line([draw_pt, bot], fill=col, width=max(1, int(wd * s)))

    # --- flecha nockada ---
    if with_arrow:
        tip = (draw_pt[0] - 42 * s, draw_pt[1])   # ponta aponta pra esquerda/frente
        # haste
        d.line([draw_pt, tip], fill=ARROW_SHAFT, width=int(1.8 * s))
        d.line([draw_pt, tip], fill=lerp(ARROW_SHAFT, (210, 180, 140, 255), 0.4),
               width=max(1, int(0.7 * s)))
        # cabeca
        hx, hy = tip
        d.polygon([(hx - 4 * s, hy), (hx + 3 * s, hy - 2.4 * s),
                   (hx + 3 * s, hy + 2.4 * s)], fill=ARROW_HEAD)
        d.polygon([(hx - 4 * s, hy), (hx + 1 * s, hy - 1.0 * s),
                   (hx + 1 * s, hy + 1.0 * s)], fill=(245, 250, 255, 255))
        # fletching (penas) junto ao nock
        fx, fy = draw_pt
        for dy in (-1, 1):
            d.polygon([(fx + 1 * s, fy), (fx + 6 * s, fy + dy * 3.2 * s),
                       (fx + 3 * s, fy + dy * 0.8 * s)], fill=FLETCH)

    return img.resize((SIZE, SIZE), Image.LANCZOS)


def main():
    frames = {
        "huntress_bow": (0.0, False),
        "huntress_bow_pulling_0": (0.45, True),
        "huntress_bow_pulling_1": (0.72, True),
        "huntress_bow_pulling_2": (1.0, True),
    }
    for name, (pct, arrow) in frames.items():
        img = draw_bow(pct, arrow)
        img.save(os.path.join(OUT_ITEM, name + ".png"))
        print("ok:", name)

    # icone do efeito Marcado (18x18) — reticulo carmesim
    fx = 18 * SS
    e = Image.new("RGBA", (fx, fx), (0, 0, 0, 0))
    ed = ImageDraw.Draw(e)
    c = fx / 2
    ed.ellipse([c - 7 * SS, c - 7 * SS, c + 7 * SS, c + 7 * SS],
               outline=(230, 30, 60, 255), width=int(1.6 * SS))
    ed.ellipse([c - 3.2 * SS, c - 3.2 * SS, c + 3.2 * SS, c + 3.2 * SS],
               outline=(255, 110, 130, 255), width=int(1.2 * SS))
    for ang in (0, 90, 180, 270):
        rad = math.radians(ang)
        x1 = c + math.cos(rad) * 4 * SS
        y1 = c + math.sin(rad) * 4 * SS
        x2 = c + math.cos(rad) * 8.5 * SS
        y2 = c + math.sin(rad) * 8.5 * SS
        ed.line([(x1, y1), (x2, y2)], fill=(255, 60, 80, 255), width=int(1.4 * SS))
    ed.ellipse([c - 1.2 * SS, c - 1.2 * SS, c + 1.2 * SS, c + 1.2 * SS],
               fill=(255, 230, 120, 255))
    e = e.resize((18, 18), Image.LANCZOS)
    e.save(os.path.join(OUT_FX, "marked.png"))
    print("ok: marked (effect icon)")


if __name__ == "__main__":
    main()
