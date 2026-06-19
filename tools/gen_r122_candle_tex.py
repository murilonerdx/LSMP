"""r122: gera 2 texturas pra candle_3d_template — wick (pavio) e flame (chama)."""
from PIL import Image, ImageDraw
import os

ROOT = os.path.dirname(__file__)
TEX_BLOCK = os.path.normpath(os.path.join(ROOT, "..",
    "src/main/resources/assets/liberthia/textures/block"))

def make_wick():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Wick — strip vertical preto/marrom no canto
    d.line([(0, 0), (0, 1)], fill=(30, 20, 15, 255))
    d.line([(1, 0), (1, 1)], fill=(60, 40, 25, 255))
    img.save(os.path.join(TEX_BLOCK, "candle_3d_wick.png"))

def make_flame():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Flame — 8x4 strip animated-ready (1 frame for now)
    # Base wide laranja
    for x in range(8):
        if 2 <= x <= 5:
            d.point((x, 3), fill=(255, 130, 30, 255))
            d.point((x, 2), fill=(255, 180, 50, 255))
    # Middle amarelo
    for x in [3, 4]:
        d.point((x, 1), fill=(255, 230, 100, 255))
    # Top branco
    d.point((3, 0), fill=(255, 255, 230, 255))
    d.point((4, 0), fill=(255, 255, 230, 255))
    img.save(os.path.join(TEX_BLOCK, "candle_3d_flame.png"))

make_wick()
make_flame()
print("Generated candle_3d_wick.png + candle_3d_flame.png")
