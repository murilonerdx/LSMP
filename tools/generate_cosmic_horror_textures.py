"""Gera 5 texturas 16x16 dos itens de cosmic horror.

1. madness_aura — orbe roxo/preto com olho dentro
2. maddening_gaze — olho aberto vermelho
3. mass_possession_crown — coroa de ouro com correntes
4. mirror_of_insanity — espelho dourado com reflexo distorcido
5. soul_cloner — frasco com silhueta dentro
"""
from PIL import Image, ImageDraw
import os
import math

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'item')
SIZE = 16


def base_img():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


# ============================================================
# 1. MADNESS AURA — orbe roxo escuro com olho central
# ============================================================
def madness_aura():
    img = base_img()
    d = ImageDraw.Draw(img)
    dark_purple = (40, 10, 60, 255)
    mid_purple = (100, 30, 140, 255)
    bright = (180, 80, 220, 255)
    glow = (220, 150, 255, 255)
    iris = (255, 50, 50, 255)
    pupil_black = (15, 0, 20, 255)
    # Orbe circular
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - 7.5
            dy = y - 7.5
            r = math.sqrt(dx * dx + dy * dy)
            if r <= 7:
                if r >= 6:
                    img.putpixel((x, y), dark_purple)
                elif r >= 4.5:
                    img.putpixel((x, y), mid_purple)
                elif r >= 3:
                    img.putpixel((x, y), bright)
                else:
                    img.putpixel((x, y), glow)
    # Olho central (íris vermelho + pupila preta vertical)
    d.ellipse([5, 6, 10, 9], fill=iris)
    d.line([(7, 6), (7, 9)], fill=pupil_black, width=1)
    d.line([(8, 6), (8, 9)], fill=pupil_black, width=1)
    # Sombras "tendrils" saindo do orbe
    d.point((1, 8), fill=mid_purple)
    d.point((0, 7), fill=mid_purple)
    d.point((14, 8), fill=mid_purple)
    d.point((15, 9), fill=mid_purple)
    d.point((7, 0), fill=mid_purple)
    d.point((8, 1), fill=mid_purple)
    d.point((7, 15), fill=mid_purple)
    d.point((6, 14), fill=mid_purple)
    img.save(os.path.join(OUT, 'madness_aura.png'))


# ============================================================
# 2. MADDENING GAZE — olho gigante aberto vermelho
# ============================================================
def maddening_gaze():
    img = base_img()
    d = ImageDraw.Draw(img)
    sclera = (240, 220, 220, 255)
    blood = (220, 30, 30, 255)
    blood_dark = (120, 10, 10, 255)
    iris = (180, 0, 0, 255)
    iris_dark = (90, 0, 0, 255)
    pupil = (10, 0, 0, 255)
    glow = (255, 100, 100, 255)
    # Almond/olho shape (horizontal oval)
    # Esclera
    d.polygon([(1, 8), (4, 4), (12, 4), (15, 8), (12, 12), (4, 12)],
              fill=sclera)
    # Outline blood
    d.line([(1, 8), (4, 4)], fill=blood_dark)
    d.line([(4, 4), (12, 4)], fill=blood_dark)
    d.line([(12, 4), (15, 8)], fill=blood_dark)
    d.line([(15, 8), (12, 12)], fill=blood_dark)
    d.line([(12, 12), (4, 12)], fill=blood_dark)
    d.line([(4, 12), (1, 8)], fill=blood_dark)
    # Íris circular vermelha
    d.ellipse([5, 5, 11, 11], fill=iris)
    d.ellipse([6, 6, 10, 10], fill=iris_dark)
    # Pupila
    d.ellipse([7, 7, 9, 9], fill=pupil)
    # Veias sclera
    d.point((2, 7), fill=blood)
    d.point((3, 9), fill=blood)
    d.point((13, 7), fill=blood)
    d.point((14, 9), fill=blood)
    # Sangue escorrendo
    d.point((6, 13), fill=blood)
    d.point((6, 14), fill=blood_dark)
    d.point((10, 13), fill=blood)
    d.point((10, 14), fill=blood_dark)
    # Glow superior (highlight da íris)
    d.point((6, 6), fill=glow)
    img.save(os.path.join(OUT, 'maddening_gaze.png'))


# ============================================================
# 3. MASS POSSESSION CROWN — coroa de ouro com 3 picos + correntes
# ============================================================
def mass_possession_crown():
    img = base_img()
    d = ImageDraw.Draw(img)
    gold = (210, 170, 50, 255)
    gold_dark = (130, 100, 20, 255)
    gold_light = (255, 220, 100, 255)
    purple = (140, 50, 200, 255)
    chain = (90, 90, 100, 255)
    # Base da coroa (banda horizontal)
    d.rectangle([2, 8, 13, 11], fill=gold)
    # Outline base
    d.line([(2, 8), (13, 8)], fill=gold_dark)
    d.line([(2, 11), (13, 11)], fill=gold_dark)
    # 3 picos triangulares
    # Pico esquerdo
    d.polygon([(2, 8), (4, 4), (5, 8)], fill=gold)
    d.polygon([(2, 8), (4, 4)], fill=gold_dark)
    # Pico central (maior)
    d.polygon([(6, 8), (8, 2), (10, 8)], fill=gold)
    d.line([(6, 8), (8, 2)], fill=gold_dark)
    d.line([(8, 2), (10, 8)], fill=gold_dark)
    # Pico direito
    d.polygon([(11, 8), (13, 4), (13, 8)], fill=gold)
    d.line([(11, 8), (13, 4)], fill=gold_dark)
    # Gemas roxas (1 em cada pico)
    d.point((4, 6), fill=purple)
    d.point((8, 4), fill=purple)
    d.point((8, 5), fill=(180, 100, 230, 255))
    d.point((12, 6), fill=purple)
    # Correntes pendentes (puppet strings)
    d.line([(2, 11), (1, 14)], fill=chain)
    d.line([(7, 11), (7, 15)], fill=chain)
    d.line([(13, 11), (14, 14)], fill=chain)
    # Highlights gold
    d.point((3, 9), fill=gold_light)
    d.point((8, 6), fill=gold_light)
    d.point((12, 9), fill=gold_light)
    img.save(os.path.join(OUT, 'mass_possession_crown.png'))


# ============================================================
# 4. MIRROR OF INSANITY — espelho dourado com reflexo "??"
# ============================================================
def mirror_of_insanity():
    img = base_img()
    d = ImageDraw.Draw(img)
    gold = (200, 170, 70, 255)
    gold_dark = (110, 90, 30, 255)
    glass = (180, 200, 220, 255)
    glass_dark = (90, 110, 140, 255)
    reflection = (255, 255, 255, 255)
    ink = (10, 10, 30, 255)
    # Frame oval dourado
    # Espelho oval (vertical)
    for y in range(SIZE):
        for x in range(SIZE):
            # Elíptica vertical: (x/3.5)² + (y/5.5)² ≤ 1
            ex = (x - 7.5) / 4.0
            ey = (y - 7.5) / 6.0
            v = ex * ex + ey * ey
            if v <= 1.0:
                if v >= 0.85:
                    img.putpixel((x, y), gold_dark)
                elif v >= 0.7:
                    img.putpixel((x, y), gold)
                else:
                    # Glass / reflexo
                    if v < 0.3:
                        img.putpixel((x, y), glass)
                    else:
                        img.putpixel((x, y), glass_dark)
    # "Você?" no centro — punto de interrogação distorcido
    d.point((7, 6), fill=ink)
    d.point((8, 6), fill=ink)
    d.point((9, 7), fill=ink)
    d.point((8, 8), fill=ink)
    d.point((8, 9), fill=ink)
    # Pingo do ?
    d.point((8, 11), fill=ink)
    # Highlight (reflexo)
    d.point((5, 5), fill=reflection)
    d.point((6, 4), fill=reflection)
    # Cabo do espelho (handle)
    d.rectangle([7, 13, 8, 15], fill=gold)
    img.save(os.path.join(OUT, 'mirror_of_insanity.png'))


# ============================================================
# 5. SOUL CLONER — frasco com silhueta de player dentro
# ============================================================
def soul_cloner():
    img = base_img()
    d = ImageDraw.Draw(img)
    glass = (200, 220, 240, 200)  # semi-transparente
    glass_dark = (110, 130, 160, 255)
    cork = (140, 90, 40, 255)
    soul = (200, 255, 255, 220)
    silhouette = (40, 60, 100, 255)
    glow = (255, 255, 255, 255)
    # Frasco — forma de pera/garrafa
    # Bojo (parte de baixo, redonda)
    d.ellipse([3, 6, 12, 14], fill=glass)
    d.ellipse([3, 6, 12, 14], outline=glass_dark)
    # Gargalo (vertical superior)
    d.rectangle([6, 3, 9, 7], fill=glass)
    d.line([(6, 3), (6, 7)], fill=glass_dark)
    d.line([(9, 3), (9, 7)], fill=glass_dark)
    # Cortiça topo
    d.rectangle([5, 1, 10, 3], fill=cork)
    d.line([(5, 1), (10, 1)], fill=(80, 50, 20, 255))
    # Conteúdo soul dentro do bojo
    d.ellipse([5, 8, 10, 13], fill=soul)
    # Silhueta de player MINI dentro (cabeça + corpo)
    d.point((7, 9), fill=silhouette)  # cabeça
    d.point((8, 9), fill=silhouette)
    d.rectangle([7, 10, 8, 12], fill=silhouette)  # tronco
    # Glow no topo (sparkle)
    d.point((5, 7), fill=glow)
    d.point((10, 9), fill=glow)
    img.save(os.path.join(OUT, 'soul_cloner.png'))


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    madness_aura()
    maddening_gaze()
    mass_possession_crown()
    mirror_of_insanity()
    soul_cloner()
    print(f'Generated 5 cosmic horror textures in {OUT}')
