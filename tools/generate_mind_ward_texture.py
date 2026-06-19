"""Gera textura 16x16 do Mind Ward — amuleto anti-possessão.

Visual: amuleto dourado circular com olho fechado central + glow ciano
(simbolo: mente protegida, olho que não vê = mente que não pode ser invadida).
"""
from PIL import Image, ImageDraw
import os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'item')
SIZE = 16


def mind_ward():
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Paleta: amuleto dourado com glow ciano
    gold_dark = (110, 80, 20, 255)
    gold = (200, 160, 60, 255)
    gold_light = (240, 210, 110, 255)
    cyan = (100, 220, 255, 255)
    cyan_bright = (180, 245, 255, 255)
    cyan_dark = (40, 130, 180, 255)
    dark = (30, 20, 10, 255)

    # Anel externo do amuleto (8px de raio, círculo)
    # Centro do círculo: (8, 8)
    # Raio externo: 7, interno: 4
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - 7.5
            dy = y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            if 5.5 <= r <= 7.5:
                # Anel — gradient gold
                if r >= 7.0:
                    img.putpixel((x, y), gold_dark)  # borda escura
                elif r >= 6.3:
                    img.putpixel((x, y), gold)
                else:
                    img.putpixel((x, y), gold_light)
            elif r < 5.5:
                # Interior: glow ciano background
                if r < 2.5:
                    img.putpixel((x, y), cyan_bright)
                elif r < 4.0:
                    img.putpixel((x, y), cyan)
                else:
                    img.putpixel((x, y), cyan_dark)

    # OLHO FECHADO no centro — linha horizontal curva
    # Pálpebra superior
    d.line([(5, 7), (10, 7)], fill=dark, width=1)
    d.line([(4, 8), (5, 7)], fill=dark, width=1)
    d.line([(10, 7), (11, 8)], fill=dark, width=1)
    # Cílios (3 pequenos)
    d.point((5, 6), fill=dark)
    d.point((7, 5), fill=dark)
    d.point((9, 6), fill=dark)
    # Pálpebra inferior (cobre o "olho fechado")
    d.line([(5, 9), (10, 9)], fill=cyan_bright)

    # Glow corners — pontinhos ciano fora do amuleto pra dar vida
    d.point((2, 2), fill=cyan_bright)
    d.point((13, 2), fill=cyan_bright)
    d.point((2, 13), fill=cyan_bright)
    d.point((13, 13), fill=cyan_bright)

    # Highlight superior esquerdo do amuleto (gold)
    d.point((4, 4), fill=(255, 255, 200, 255))
    d.point((5, 3), fill=(255, 255, 200, 255))

    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, 'mind_ward.png'))
    print(f'Saved {OUT}/mind_ward.png')


if __name__ == '__main__':
    mind_ward()
