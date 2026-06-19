"""r121 v3: Refazer chalks com cor MUITO mais visível.

Abordagem:
- Stick "espesso" diagonal de 4-5 pixels de largura usando função draw.line
- Cor central PURA (sem highlight cobrindo)
- Edge escuro nas bordas
- Tip claro na ponta
"""
from PIL import Image, ImageDraw
import os, math

ROOT = os.path.dirname(__file__)
TEX_ITEM = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/item"))


def make_chalk_v3(filepath, color):
    """16×16 chalk com stick diagonal grosso (5 pixels perpendicular)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = color + (255,)
    cshadow = (max(0, c[0] - 70), max(0, c[1] - 70), max(0, c[2] - 70), 255)
    cdark = (max(0, c[0] - 110), max(0, c[1] - 110), max(0, c[2] - 110), 255)

    # Pinta o stick em ETAPAS, da edge escura pra dentro
    # Direção do stick: de (2,13) a (13,2) — diagonal canto inf-esq a sup-dir
    # Width perpendicular: 5 pixels (offset -2 a +2 em direção (1,1))

    # FIRST pass: outer dark edge (offset -2)
    for t in range(13):
        cx, cy = 1 + t, 14 - t
        for offset in [-2, 2]:
            px, py = cx + offset, cy + offset
            if 0 <= px < 16 and 0 <= py < 16:
                d.point((px, py), fill=cdark)

    # SECOND pass: medium shadow (offset -1, +1)
    for t in range(13):
        cx, cy = 1 + t, 14 - t
        for offset in [-1, 1]:
            px, py = cx + offset, cy + offset
            if 0 <= px < 16 and 0 <= py < 16:
                d.point((px, py), fill=cshadow)

    # THIRD pass (LAST): pure color core (offset 0)
    for t in range(13):
        cx, cy = 1 + t, 14 - t
        if 0 <= cx < 16 and 0 <= cy < 16:
            d.point((cx, cy), fill=c)

    # PARALLEL extra core line for thicker visible core (offset along perpendicular)
    # Pinta uma 2ª linha de core deslocada em (-1, +1) — perpendicular axis
    for t in range(13):
        cx, cy = 1 + t, 14 - t
        px, py = cx - 1, cy + 1
        if 0 <= px < 16 and 0 <= py < 16:
            d.point((px, py), fill=c)
    # E outra deslocada (+1, -1)
    for t in range(13):
        cx, cy = 1 + t, 14 - t
        px, py = cx + 1, cy - 1
        if 0 <= px < 16 and 0 <= py < 16:
            d.point((px, py), fill=c)

    # TOP TIP (ponta usada) — branco com dust da cor
    d.point((13, 2), fill=(255, 255, 255, 255))
    d.point((14, 2), fill=(255, 255, 255, 220))
    d.point((14, 3), fill=(c[0], c[1], c[2], 200))
    d.point((13, 1), fill=(255, 255, 255, 180))

    # BOTTOM TIP (base segurada)
    d.point((1, 14), fill=cdark)
    d.point((2, 13), fill=cshadow)
    d.point((1, 15), fill=cdark)
    d.point((0, 14), fill=cdark)

    img.save(filepath)


def make_chalk_symbol_v3():
    """16×16 chalk symbol — sigilo do olho de providência mais claro."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    # Anel externo
    d.ellipse([cx - 7, cy - 7, cx + 6, cy + 6], outline=(245, 230, 200, 255))
    # Triângulo
    d.polygon([(cx, cy - 5), (cx - 5, cy + 4), (cx + 5, cy + 4)],
              outline=(245, 230, 200, 255))
    # Olho dentro
    d.ellipse([cx - 2, cy - 1, cx + 2, cy + 2], fill=(40, 25, 70, 255))
    # Iris
    d.point((cx, cy), fill=(255, 215, 90, 255))
    d.point((cx, cy + 1), fill=(255, 180, 50, 255))
    # Sparkles
    d.point((cx - 6, cy), fill=(255, 220, 100, 220))
    d.point((cx + 6, cy), fill=(255, 220, 100, 220))
    d.point((cx, cy - 7), fill=(255, 220, 100, 220))
    d.point((cx, cy + 6), fill=(255, 220, 100, 220))
    img.save(os.path.join(TEX_ITEM, "chalk_symbol.png"))


CHALK_PALETTE = {
    "chalk":        (250, 248, 240),
    "chalk_white":  (250, 248, 240),
    "chalk_black":  (40, 38, 38),
    "chalk_red":    (220, 40, 40),
    "chalk_purple": (175, 75, 230),
    "chalk_golden": (245, 195, 60),
    "blood_chalk":  (165, 25, 30),
    "observation_chalk": (130, 105, 235),
}

for name, color in CHALK_PALETTE.items():
    make_chalk_v3(os.path.join(TEX_ITEM, f"{name}.png"), color)

make_chalk_symbol_v3()

print(f"r121 v3: refined {len(CHALK_PALETTE)} chalks with thick saturated cores + chalk_symbol sigil")
