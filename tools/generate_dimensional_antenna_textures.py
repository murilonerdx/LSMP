"""Gera 3 texturas pra Dimensional Antenna (top/side/bottom)."""
from PIL import Image, ImageDraw
import os
import math

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'block')


def base():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 255))


def top():
    """Top: cluster de amethyst em pedra escura — antena olha pra cima."""
    img = base()
    d = ImageDraw.Draw(img)
    stone = (35, 30, 50, 255)
    stone_dark = (20, 18, 30, 255)
    stone_light = (55, 50, 75, 255)
    amethyst = (170, 100, 220, 255)
    amethyst_dark = (90, 50, 130, 255)
    amethyst_glow = (230, 180, 255, 255)
    copper = (200, 110, 70, 255)
    # Base stone
    for y in range(16):
        for x in range(16):
            color = stone
            if (x + y) % 7 == 0: color = stone_dark
            elif (x * y) % 9 == 0: color = stone_light
            img.putpixel((x, y), color)
    # Center: cluster de amethyst (4 cristais)
    # Cristal central grande
    d.polygon([(7, 4), (9, 4), (10, 8), (9, 11), (7, 11), (6, 8)], fill=amethyst)
    d.line([(7, 4), (6, 8)], fill=amethyst_dark)
    d.line([(9, 4), (10, 8)], fill=amethyst_dark)
    d.line([(6, 8), (7, 11)], fill=amethyst_dark)
    d.line([(10, 8), (9, 11)], fill=amethyst_dark)
    d.point((8, 6), fill=amethyst_glow)
    # Cristais secundários (4 corners)
    d.point((3, 4), fill=amethyst)
    d.point((4, 3), fill=amethyst_glow)
    d.point((12, 4), fill=amethyst)
    d.point((11, 3), fill=amethyst_glow)
    d.point((3, 12), fill=amethyst)
    d.point((4, 13), fill=amethyst_glow)
    d.point((12, 12), fill=amethyst)
    d.point((11, 13), fill=amethyst_glow)
    # Copper ring (canto)
    d.point((0, 0), fill=copper)
    d.point((15, 0), fill=copper)
    d.point((0, 15), fill=copper)
    d.point((15, 15), fill=copper)
    img.save(os.path.join(OUT, 'dimensional_antenna_top.png'))


def side():
    """Side: pedra escura com runa amethyst no centro + estria de copper."""
    img = base()
    d = ImageDraw.Draw(img)
    stone = (40, 35, 55, 255)
    stone_dark = (22, 20, 35, 255)
    stone_light = (60, 55, 80, 255)
    amethyst = (170, 100, 220, 255)
    amethyst_glow = (230, 180, 255, 255)
    copper = (200, 110, 70, 255)
    copper_dark = (120, 60, 30, 255)
    # Base stone
    for y in range(16):
        for x in range(16):
            color = stone
            if (x * 3 + y) % 7 == 0: color = stone_dark
            elif (x + y * 3) % 11 == 0: color = stone_light
            img.putpixel((x, y), color)
    # Copper banner horizontal no meio
    d.line([(0, 9), (15, 9)], fill=copper_dark)
    d.line([(0, 10), (15, 10)], fill=copper)
    d.line([(0, 11), (15, 11)], fill=copper_dark)
    # Runa amethyst central (Y simbólico)
    d.line([(7, 3), (8, 6)], fill=amethyst)
    d.line([(8, 6), (9, 3)], fill=amethyst)
    d.line([(8, 6), (8, 8)], fill=amethyst)
    d.point((8, 4), fill=amethyst_glow)
    # Diodo LED em baixo (indica ativo) - 1 pixel ciano
    d.point((2, 13), fill=(100, 220, 255, 255))
    d.point((13, 13), fill=(100, 220, 255, 255))
    # Top border
    d.line([(0, 0), (15, 0)], fill=stone_light)
    img.save(os.path.join(OUT, 'dimensional_antenna_side.png'))


def bottom():
    """Bottom: pedra com base de copper polido."""
    img = base()
    d = ImageDraw.Draw(img)
    stone = (30, 25, 45, 255)
    stone_dark = (15, 12, 25, 255)
    copper = (180, 100, 60, 255)
    copper_dark = (110, 55, 25, 255)
    copper_light = (240, 160, 100, 255)
    # Plain dark stone
    for y in range(16):
        for x in range(16):
            color = stone
            if (x + y) % 5 == 0: color = stone_dark
            img.putpixel((x, y), color)
    # Copper ring no centro (anel)
    for y in range(16):
        for x in range(16):
            dx = x - 7.5
            dy = y - 7.5
            r = math.sqrt(dx * dx + dy * dy)
            if 4.5 <= r <= 6.0:
                img.putpixel((x, y), copper_dark if r > 5.5 else copper)
            elif r < 3.0:
                img.putpixel((x, y), copper_light if r < 1.5 else copper)
    img.save(os.path.join(OUT, 'dimensional_antenna_bottom.png'))


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    top()
    side()
    bottom()
    print('Generated 3 antenna textures')
