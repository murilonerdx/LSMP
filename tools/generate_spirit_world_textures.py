"""Gera 4 texturas pro Spirit World: soul_sever item + spirit_altar (top+side)
+ spirit_stone block."""
from PIL import Image, ImageDraw
import os
import math

ITEM_OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                       'assets', 'liberthia', 'textures', 'item')
BLOCK_OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                        'assets', 'liberthia', 'textures', 'block')


def soul_sever():
    """16x16 — adaga ritualística com gem ciano/violeta no cabo."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bone = (220, 215, 195, 255)
    bone_dark = (130, 125, 110, 255)
    metal = (140, 145, 165, 255)
    metal_dark = (60, 65, 80, 255)
    cyan = (100, 220, 255, 255)
    cyan_glow = (200, 245, 255, 255)
    soul = (180, 100, 255, 255)
    soul_dark = (90, 40, 150, 255)
    # Lâmina (vertical, diagonal sharpener)
    d.polygon([(7, 1), (9, 1), (10, 11), (8, 13), (6, 11)], fill=metal)
    d.line([(7, 1), (6, 11)], fill=metal_dark)
    d.line([(9, 1), (10, 11)], fill=metal_dark)
    d.line([(6, 11), (8, 13)], fill=metal_dark)
    d.line([(10, 11), (8, 13)], fill=metal_dark)
    d.point((8, 2), fill=cyan_glow)  # tip glow
    # Guarda horizontal (cross)
    d.rectangle([4, 10, 11, 11], fill=bone)
    d.line([(4, 10), (11, 10)], fill=bone_dark)
    d.line([(4, 11), (11, 11)], fill=bone_dark)
    # Cabo
    d.rectangle([7, 12, 9, 15], fill=bone)
    d.line([(7, 12), (7, 15)], fill=bone_dark)
    d.line([(9, 12), (9, 15)], fill=bone_dark)
    # Gem central na cross (soul cyan)
    d.point((7, 10), fill=cyan)
    d.point((8, 10), fill=cyan_glow)
    # Tendrils de alma saindo do cabo
    d.point((5, 14), fill=soul)
    d.point((4, 15), fill=soul_dark)
    d.point((10, 14), fill=soul)
    d.point((11, 15), fill=soul_dark)
    d.point((8, 15), fill=cyan)
    os.makedirs(ITEM_OUT, exist_ok=True)
    img.save(os.path.join(ITEM_OUT, 'soul_sever.png'))


def spirit_altar_top():
    """16x16 — top do altar: pentagrama gravado em pedra escura."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    stone = (50, 45, 65, 255)
    stone_dark = (30, 25, 40, 255)
    stone_light = (75, 65, 95, 255)
    glow = (180, 100, 255, 255)
    soul = (100, 220, 255, 255)
    # Base stone
    for y in range(16):
        for x in range(16):
            base = stone
            if (x + y) % 7 == 0: base = stone_dark
            elif (x * y) % 11 == 0: base = stone_light
            img.putpixel((x, y), base)
    # Pentagrama (5-pointed star inscrito)
    # Centro 8,8 raio 5
    points = []
    for i in range(5):
        angle = -math.pi / 2 + i * (2 * math.pi / 5)
        x = 8 + math.cos(angle) * 5
        y = 8 + math.sin(angle) * 5
        points.append((x, y))
    # Linhas conectando vertices em estrela (each-other-two)
    for i in range(5):
        a = points[i]
        b = points[(i + 2) % 5]
        d.line([(int(a[0]), int(a[1])), (int(b[0]), int(b[1]))], fill=glow)
    # Centro
    d.point((8, 8), fill=soul)
    img.save(os.path.join(BLOCK_OUT, 'spirit_altar_top.png'))


def spirit_altar_side():
    """16x16 — lateral do altar: pedra com runas glow."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    stone = (45, 40, 60, 255)
    stone_dark = (25, 20, 35, 255)
    stone_light = (70, 60, 90, 255)
    glow = (180, 100, 255, 255)
    # Base
    for y in range(16):
        for x in range(16):
            base = stone
            if (x + 2 * y) % 5 == 0: base = stone_dark
            elif (x * 3 + y) % 13 == 0: base = stone_light
            img.putpixel((x, y), base)
    # Runa central (símbolo simples)
    d.line([(5, 6), (10, 10)], fill=glow)
    d.line([(10, 6), (5, 10)], fill=glow)
    d.point((7, 4), fill=glow)
    d.point((8, 4), fill=glow)
    d.point((4, 12), fill=glow)
    d.point((11, 12), fill=glow)
    # Topo brilho (borda superior)
    d.line([(0, 0), (15, 0)], fill=(90, 80, 110, 255))
    img.save(os.path.join(BLOCK_OUT, 'spirit_altar_side.png'))


def spirit_stone():
    """16x16 — pedra espiritual: deepslate com manchas violeta sutil."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    stone = (50, 45, 60, 255)
    stone_dark = (30, 25, 38, 255)
    stone_mid = (40, 35, 50, 255)
    stone_light = (70, 60, 85, 255)
    violet = (100, 60, 140, 255)
    for y in range(16):
        for x in range(16):
            base = stone
            if (x * 7 + y * 13) % 11 == 0: base = stone_dark
            elif (x * 5 + y * 3) % 17 == 0: base = stone_mid
            elif (x * 11 + y * 7) % 19 == 0: base = stone_light
            img.putpixel((x, y), base)
    # Manchas violeta sutil (3 spots)
    d = ImageDraw.Draw(img)
    d.point((4, 5), fill=violet)
    d.point((11, 9), fill=violet)
    d.point((7, 12), fill=violet)
    os.makedirs(BLOCK_OUT, exist_ok=True)
    img.save(os.path.join(BLOCK_OUT, 'spirit_stone.png'))


if __name__ == '__main__':
    soul_sever()
    spirit_altar_top()
    spirit_altar_side()
    spirit_stone()
    print('Generated 4 spirit world textures')
