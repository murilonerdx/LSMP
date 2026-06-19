"""
r46: Gera 10 texturas 16x16 de items eldritch — temáticas:
- alive, organic, cursed, ancient, impossible
- flesh + void fusion
- analog horror

1.  watching_eye         — olho vivo em moldura preta metálica
2.  black_signal_radio   — rádio analog preto com LEDs vermelhos
3.  hollow_mask          — máscara porcelana branca rachada com líquido
4.  flesh_lantern        — lanterna de carne pulsante com olho
5.  false_totem          — ídolo de pedra preta com runas flutuantes
6.  infection_needle     — seringa biomecânica com fluido brilhando
7.  book_impossible      — tome com diagramas eldritch
8.  mimic_heart          — coração vermelho pulsante
9.  red_tape             — VHS vermelha corrupta
10. null_bell            — sino preto com cracks no vazio
"""
from PIL import Image, ImageDraw
import os
import math
import random

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'item')
os.makedirs(OUT, exist_ok=True)
SIZE = 16
random.seed(42)


def blank():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def save(name, img):
    img.save(os.path.join(OUT, f'{name}.png'))
    print(f'  [OK] item/{name}.png')


# ============================================================
# 1. WATCHING EYE — olho vivo em moldura metálica
# ============================================================
def watching_eye():
    img = blank()
    d = ImageDraw.Draw(img)
    # Moldura preta metálica (octógono)
    d.polygon([(4, 1), (11, 1), (15, 5), (15, 10),
               (11, 14), (4, 14), (1, 10), (1, 5)],
              fill=(20, 15, 25, 255), outline=(80, 60, 90, 255))
    # Highlight metálico nas bordas (sheen)
    d.line([(4, 1), (11, 1)], fill=(180, 160, 200, 255), width=1)
    d.line([(1, 5), (1, 10)], fill=(120, 100, 140, 255), width=1)
    # Interior (eye socket cavernoso)
    d.ellipse([3, 4, 12, 11], fill=(50, 5, 5, 255))
    # Eyeball (white com veias)
    d.ellipse([4, 5, 11, 10], fill=(220, 200, 180, 255))
    # Veias vermelhas
    d.line([(5, 6), (7, 6)], fill=(140, 20, 20, 255), width=1)
    d.line([(10, 7), (8, 8)], fill=(140, 20, 20, 255), width=1)
    d.line([(5, 9), (6, 8)], fill=(140, 20, 20, 255), width=1)
    # Iris vermelho-laranja (emissive feel)
    d.ellipse([6, 6, 9, 9], fill=(200, 30, 30, 255))
    # Pupila vertical (gato/réptil)
    d.line([(7, 6), (7, 9)], fill=(5, 0, 0, 255), width=1)
    d.line([(8, 6), (8, 9)], fill=(5, 0, 0, 255), width=1)
    # Brilho da pupila
    img.putpixel((7, 7), (255, 200, 100, 255))
    # Lágrima de sangue
    img.putpixel((5, 11), (120, 10, 10, 255))
    img.putpixel((5, 12), (160, 20, 20, 255))
    save('watching_eye', img)


# ============================================================
# 2. BLACK SIGNAL RADIO — rádio analog preto
# ============================================================
def black_signal_radio():
    img = blank()
    d = ImageDraw.Draw(img)
    # Body (rusted dark metal)
    d.rectangle([1, 4, 14, 14], fill=(40, 30, 35, 255), outline=(15, 10, 12, 255))
    # Top antenna
    d.line([(8, 0), (8, 4)], fill=(60, 50, 55, 255), width=1)
    d.line([(7, 1), (9, 1)], fill=(80, 70, 75, 255), width=1)
    # Speaker mesh (left)
    for y in range(6, 13, 2):
        for x in range(2, 7, 2):
            img.putpixel((x, y), (15, 10, 12, 255))
    # Frequency dial (right top)
    d.ellipse([9, 5, 13, 9], fill=(20, 15, 18, 255), outline=(100, 80, 90, 255))
    d.line([(11, 7), (12, 5)], fill=(200, 30, 30, 255), width=1)  # red needle
    # LED indicators (flickering red dots)
    img.putpixel((10, 11), (220, 30, 30, 255))
    img.putpixel((12, 11), (160, 20, 20, 255))
    img.putpixel((11, 13), (220, 30, 30, 255))
    # Cosmic symbol engraved (small)
    img.putpixel((4, 5), (140, 30, 200, 255))
    img.putpixel((5, 5), (140, 30, 200, 255))
    # Rust patches
    img.putpixel((2, 11), (90, 50, 30, 255))
    img.putpixel((13, 8), (90, 50, 30, 255))
    save('black_signal_radio', img)


# ============================================================
# 3. HOLLOW MASK — máscara porcelana branca rachada
# ============================================================
def hollow_mask():
    img = blank()
    d = ImageDraw.Draw(img)
    # Mask shape (oval pointing slightly forward)
    d.ellipse([2, 1, 13, 15], fill=(230, 225, 215, 255), outline=(80, 75, 70, 255))
    # Eye sockets (dark hollows)
    d.ellipse([4, 5, 7, 8], fill=(5, 0, 5, 255))
    d.ellipse([8, 5, 11, 8], fill=(5, 0, 5, 255))
    # Glowing eye sockets (pequenos pixels brancos no fundo escuro)
    img.putpixel((5, 6), (255, 240, 200, 255))
    img.putpixel((9, 6), (255, 240, 200, 255))
    # Cracks
    d.line([(7, 2), (6, 5)], fill=(60, 55, 50, 255), width=1)
    d.line([(8, 9), (9, 13)], fill=(60, 55, 50, 255), width=1)
    d.line([(4, 10), (5, 12)], fill=(60, 55, 50, 255), width=1)
    d.line([(11, 8), (12, 10)], fill=(60, 55, 50, 255), width=1)
    # Black liquid leaking from eyes
    img.putpixel((5, 9), (10, 5, 10, 255))
    img.putpixel((5, 10), (15, 5, 15, 255))
    img.putpixel((9, 9), (10, 5, 10, 255))
    img.putpixel((9, 10), (15, 5, 15, 255))
    # Subtle face shading (depth)
    d.line([(6, 12), (9, 12)], fill=(180, 175, 165, 255), width=1)  # mouth indent
    # Side shadow
    d.line([(3, 5), (3, 11)], fill=(180, 175, 165, 255), width=1)
    save('hollow_mask', img)


# ============================================================
# 4. FLESH LANTERN — lanterna de carne pulsante
# ============================================================
def flesh_lantern():
    img = blank()
    d = ImageDraw.Draw(img)
    # Top handle (bone-like)
    d.line([(7, 1), (8, 1)], fill=(200, 180, 160, 255), width=2)
    d.line([(6, 2), (9, 2)], fill=(180, 160, 140, 255), width=1)
    # Lantern body — pulsating flesh
    # Outer fleshy shell
    d.ellipse([2, 3, 13, 14], fill=(140, 30, 40, 255), outline=(80, 10, 20, 255))
    # Inner glow (pulsating)
    d.ellipse([3, 5, 12, 12], fill=(200, 50, 60, 255))
    d.ellipse([5, 6, 10, 11], fill=(255, 100, 80, 255))
    # Central eyeball glowing
    d.ellipse([6, 7, 9, 10], fill=(255, 255, 200, 255))
    d.ellipse([7, 8, 8, 9], fill=(50, 5, 5, 255))  # pupil
    # Veins on the outside (organic)
    d.line([(3, 7), (5, 9)], fill=(100, 10, 20, 255), width=1)
    d.line([(11, 7), (13, 9)], fill=(100, 10, 20, 255), width=1)
    d.line([(7, 13), (8, 14)], fill=(100, 10, 20, 255), width=1)
    # Membrane highlights
    img.putpixel((4, 4), (255, 150, 150, 255))
    img.putpixel((12, 4), (255, 150, 150, 255))
    # Drip below
    img.putpixel((7, 15), (160, 20, 30, 200))
    save('flesh_lantern', img)


# ============================================================
# 5. FALSE TOTEM — ídolo de pedra com runas
# ============================================================
def false_totem():
    img = blank()
    d = ImageDraw.Draw(img)
    # Base pedra preta
    d.rectangle([5, 11, 10, 15], fill=(20, 15, 25, 255), outline=(60, 40, 80, 255))
    # Idol body (impossible geometry — non-Euclidean)
    d.polygon([(5, 11), (4, 7), (5, 4), (7, 2), (10, 2),
               (12, 5), (11, 9), (10, 11)],
              fill=(30, 20, 40, 255), outline=(80, 60, 120, 255))
    # Asymmetric face — 3 eyes (impossible)
    img.putpixel((6, 5), (200, 30, 30, 255))
    img.putpixel((9, 5), (200, 30, 30, 255))
    img.putpixel((7, 7), (200, 30, 30, 255))  # third eye
    # Mouth (silent scream)
    d.line([(7, 9), (9, 9)], fill=(120, 20, 20, 255), width=1)
    # Floating runes (small bright pixels around)
    rune_positions = [(2, 3), (13, 4), (1, 8), (14, 8), (3, 14), (13, 13)]
    for rx, ry in rune_positions:
        img.putpixel((rx, ry), (180, 80, 220, 255))
    # Engraving lines
    d.line([(6, 8), (10, 8)], fill=(80, 50, 120, 255), width=1)
    save('false_totem', img)


# ============================================================
# 6. INFECTION NEEDLE — seringa biomecânica
# ============================================================
def infection_needle():
    img = blank()
    d = ImageDraw.Draw(img)
    # Needle tip (top — sharp metal)
    d.line([(7, 0), (8, 0)], fill=(180, 180, 200, 255), width=1)
    d.line([(7, 1), (8, 1)], fill=(220, 220, 240, 255), width=1)
    d.line([(7, 2), (8, 2)], fill=(180, 180, 200, 255), width=1)
    # Drop of infectious fluid on tip
    img.putpixel((7, 3), (140, 0, 220, 255))
    # Syringe tube (black metal + glass)
    d.rectangle([6, 4, 9, 10], fill=(30, 20, 35, 255), outline=(15, 10, 18, 255))
    # Glowing infected fluid inside (purple-pink)
    d.rectangle([7, 5, 8, 9], fill=(180, 30, 220, 255))
    # Bubbles in fluid
    img.putpixel((7, 6), (255, 150, 255, 255))
    img.putpixel((8, 7), (255, 150, 255, 255))
    # Pulsating tubes around (flesh growth)
    d.line([(5, 6), (4, 7)], fill=(140, 30, 60, 255), width=1)
    d.line([(10, 6), (11, 7)], fill=(140, 30, 60, 255), width=1)
    # Plunger (top of syringe, metallic)
    d.rectangle([6, 11, 9, 13], fill=(100, 80, 110, 255), outline=(40, 30, 50, 255))
    # Bone handle (bottom)
    d.line([(7, 14), (8, 14)], fill=(200, 180, 160, 255), width=1)
    d.line([(6, 15), (9, 15)], fill=(180, 160, 140, 255), width=1)
    # Flesh growths on side
    img.putpixel((4, 9), (160, 30, 60, 255))
    img.putpixel((11, 9), (160, 30, 60, 255))
    save('infection_needle', img)


# ============================================================
# 7. BOOK OF IMPOSSIBLE GEOMETRY — tome eldritch
# ============================================================
def book_impossible():
    img = blank()
    d = ImageDraw.Draw(img)
    # Book cover (ancient dark leather)
    d.rectangle([2, 2, 13, 14], fill=(50, 25, 15, 255), outline=(20, 10, 5, 255))
    # Spine highlight
    d.line([(2, 3), (2, 13)], fill=(80, 45, 25, 255), width=1)
    # Cover engraving (impossible diagram — penrose-like)
    d.line([(5, 5), (10, 5)], fill=(180, 30, 30, 255), width=1)
    d.line([(5, 5), (5, 10)], fill=(180, 30, 30, 255), width=1)
    d.line([(10, 5), (10, 10)], fill=(180, 30, 30, 255), width=1)
    d.line([(5, 10), (10, 10)], fill=(180, 30, 30, 255), width=1)
    # Central eye symbol
    d.ellipse([6, 6, 9, 9], fill=(220, 200, 50, 255))
    img.putpixel((7, 7), (5, 0, 0, 255))
    # Page edges (yellowed)
    d.line([(13, 3), (13, 13)], fill=(180, 160, 130, 255), width=1)
    # Glowing glyphs on edges (animated feel via bright pixels)
    img.putpixel((3, 4), (255, 100, 200, 255))
    img.putpixel((12, 11), (255, 100, 200, 255))
    img.putpixel((4, 12), (255, 100, 200, 255))
    # Scratches on cover (worn)
    d.line([(7, 11), (9, 12)], fill=(20, 10, 5, 255), width=1)
    d.line([(3, 8), (4, 9)], fill=(20, 10, 5, 255), width=1)
    save('book_impossible', img)


# ============================================================
# 8. MIMIC HEART — coração vivo pulsante
# ============================================================
def mimic_heart():
    img = blank()
    d = ImageDraw.Draw(img)
    # Heart shape — two lobes + point
    # Left lobe
    d.ellipse([2, 3, 9, 10], fill=(180, 20, 30, 255), outline=(100, 5, 15, 255))
    # Right lobe
    d.ellipse([7, 3, 14, 10], fill=(180, 20, 30, 255), outline=(100, 5, 15, 255))
    # Bottom point
    d.polygon([(3, 8), (13, 8), (8, 14)], fill=(180, 20, 30, 255), outline=(100, 5, 15, 255))
    # Highlights (wet shiny look)
    d.ellipse([4, 4, 6, 6], fill=(255, 100, 100, 255))
    d.ellipse([9, 4, 11, 6], fill=(255, 100, 100, 255))
    # Veins (darker red)
    d.line([(4, 8), (5, 11)], fill=(100, 5, 15, 255), width=1)
    d.line([(11, 8), (10, 11)], fill=(100, 5, 15, 255), width=1)
    d.line([(8, 9), (8, 12)], fill=(100, 5, 15, 255), width=1)
    # Glowing corruption (purple-pink dots — impossible blood)
    img.putpixel((6, 6), (255, 100, 200, 255))
    img.putpixel((10, 6), (255, 100, 200, 255))
    # Aorta cut at top (chunks of vein)
    d.rectangle([6, 1, 9, 3], fill=(140, 10, 20, 255), outline=(80, 5, 10, 255))
    img.putpixel((7, 2), (200, 30, 40, 255))
    save('mimic_heart', img)


# ============================================================
# 9. RED TAPE — VHS vermelha corrupta
# ============================================================
def red_tape():
    img = blank()
    d = ImageDraw.Draw(img)
    # VHS body (red plastic + black sides)
    d.rectangle([1, 3, 14, 13], fill=(140, 20, 20, 255), outline=(60, 5, 5, 255))
    # Top label area (whitish, stained)
    d.rectangle([2, 4, 13, 8], fill=(200, 180, 160, 255), outline=(80, 60, 40, 255))
    # Label scribbling (red marker?)
    d.line([(3, 6), (12, 6)], fill=(180, 20, 20, 255), width=1)
    d.line([(3, 7), (7, 7)], fill=(180, 20, 20, 255), width=1)
    # Blood splatter on label
    img.putpixel((9, 5), (200, 30, 30, 255))
    img.putpixel((10, 6), (200, 30, 30, 255))
    img.putpixel((11, 7), (160, 20, 20, 255))
    # Two reel windows (clear, black inside)
    d.ellipse([3, 9, 6, 12], fill=(20, 10, 10, 255), outline=(60, 5, 5, 255))
    d.ellipse([9, 9, 12, 12], fill=(20, 10, 10, 255), outline=(60, 5, 5, 255))
    # Magnetic tape visible through windows (brown)
    img.putpixel((4, 10), (100, 60, 30, 255))
    img.putpixel((5, 11), (100, 60, 30, 255))
    img.putpixel((10, 10), (100, 60, 30, 255))
    img.putpixel((11, 11), (100, 60, 30, 255))
    # Damage/static texture (white noise on body)
    img.putpixel((2, 13), (200, 200, 200, 255))
    img.putpixel((7, 12), (200, 200, 200, 255))
    img.putpixel((13, 4), (200, 200, 200, 255))
    # Cracks
    d.line([(8, 9), (8, 13)], fill=(40, 5, 5, 255), width=1)
    save('red_tape', img)


# ============================================================
# 10. NULL BELL — sino preto com void cracks
# ============================================================
def null_bell():
    img = blank()
    d = ImageDraw.Draw(img)
    # Bell crown (top tapered)
    d.rectangle([7, 1, 8, 3], fill=(40, 30, 50, 255), outline=(15, 10, 20, 255))
    # Bell body (trapezoidal)
    d.polygon([(5, 3), (10, 3), (12, 11), (3, 11)],
              fill=(30, 20, 40, 255), outline=(15, 10, 20, 255))
    # Bell rim (bottom)
    d.line([(3, 11), (12, 11)], fill=(80, 60, 100, 255), width=1)
    d.line([(3, 12), (12, 12)], fill=(15, 10, 20, 255), width=1)
    # Void cracks — purple-pink glowing
    d.line([(7, 4), (6, 8)], fill=(180, 30, 200, 255), width=1)
    d.line([(9, 5), (10, 9)], fill=(180, 30, 200, 255), width=1)
    d.line([(7, 8), (8, 10)], fill=(180, 30, 200, 255), width=1)
    # Engravings — small symbols
    img.putpixel((5, 5), (200, 80, 220, 255))
    img.putpixel((10, 6), (200, 80, 220, 255))
    img.putpixel((6, 9), (200, 80, 220, 255))
    img.putpixel((9, 10), (200, 80, 220, 255))
    # Clapper inside (dangling)
    d.line([(7, 12), (7, 14)], fill=(40, 30, 50, 255), width=1)
    img.putpixel((7, 14), (60, 40, 80, 255))
    img.putpixel((8, 14), (60, 40, 80, 255))
    # Floating dust (small specs around)
    img.putpixel((1, 5), (100, 80, 140, 200))
    img.putpixel((14, 8), (100, 80, 140, 200))
    img.putpixel((2, 10), (100, 80, 140, 200))
    save('null_bell', img)


def main():
    print('Generating 10 Eldritch Artifact textures...')
    watching_eye()
    black_signal_radio()
    hollow_mask()
    flesh_lantern()
    false_totem()
    infection_needle()
    book_impossible()
    mimic_heart()
    red_tape()
    null_bell()
    print('[OK] Done - 10 textures generated')


if __name__ == '__main__':
    main()
