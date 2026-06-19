"""r145: Gera textura de Observer com aparencia toda de madeira.

64x64 com layout do Zombie (ModelLayers.ZOMBIE) — preenche todos os UVs
com pattern de madeira nodada/grain pra parecer um totem/estatua de madeira.
"""
from PIL import Image, ImageDraw
import random

random.seed(173)  # SCP-173

OUT = "src/main/resources/assets/liberthia/textures/entity/observer.png"

W, H = 64, 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))  # transparent base
d = ImageDraw.Draw(img)

# Wood palette — varios tons de marrom
WOOD_DARK = (66, 41, 22, 255)
WOOD_MID = (101, 65, 35, 255)
WOOD_BASE = (133, 90, 50, 255)
WOOD_LIGHT = (165, 117, 70, 255)
WOOD_HIGHLIGHT = (185, 140, 90, 255)
WOOD_KNOT_DARK = (40, 22, 10, 255)
WOOD_GRAIN = (88, 55, 28, 255)

def fill_wood_region(x0, y0, x1, y1, vertical_grain=True):
    """Preenche uma regiao com pattern de madeira."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            # Base color com noise
            n = random.random()
            if n < 0.05:
                c = WOOD_DARK
            elif n < 0.25:
                c = WOOD_MID
            elif n < 0.75:
                c = WOOD_BASE
            elif n < 0.95:
                c = WOOD_LIGHT
            else:
                c = WOOD_HIGHLIGHT
            img.putpixel((x, y), c)

    # Adiciona linhas de grain (verticais ou horizontais)
    if vertical_grain:
        # Linhas verticais de grain
        for gx in range(x0, x1):
            if random.random() < 0.15:
                # linha vertical de tom diferente
                color = WOOD_GRAIN if random.random() < 0.7 else WOOD_DARK
                for gy in range(y0, y1):
                    if random.random() < 0.7:
                        img.putpixel((gx, gy), color)
    else:
        for gy in range(y0, y1):
            if random.random() < 0.15:
                color = WOOD_GRAIN if random.random() < 0.7 else WOOD_DARK
                for gx in range(x0, x1):
                    if random.random() < 0.7:
                        img.putpixel((gx, gy), color)

    # Knot ocasional (no de madeira)
    if random.random() < 0.3 and (x1 - x0) >= 4 and (y1 - y0) >= 4:
        kx = random.randint(x0 + 1, x1 - 3)
        ky = random.randint(y0 + 1, y1 - 3)
        # circulo escuro pequeno
        d.ellipse([kx, ky, kx+2, ky+2], fill=WOOD_KNOT_DARK)
        d.point((kx+1, ky+1), fill=WOOD_DARK)

# ─── Zombie/Player UV layout (64x64) ──────────────────────────
# Head: top=0,0-32,16 (face) + 32,0-64,16 (hat overlay)
# Body: 16,16-40,32 (front+back+sides) — main torso strip
# Arms: 40,16-56,32 (right arm) + 32,48-48,64 (left arm)
# Legs: 0,16-16,32 (right leg) + 16,48-32,64 (left leg)
# Hat overlay: 32,0-64,16
# Jacket overlay: 16,32-40,48
# Sleeves: 40,32-56,48 + 48,48-64,64
# Pants: 0,32-16,48 + 0,48-16,64

# Para um look "totem de madeira", preencho TODAS as areas com madeira
# verticalmente granulada (vertical_grain=True)

# Head — todo o quadrado 0-32 x 0-16
fill_wood_region(0, 0, 32, 16, vertical_grain=True)
# Hat overlay
fill_wood_region(32, 0, 64, 16, vertical_grain=True)

# Body strip
fill_wood_region(16, 16, 40, 32, vertical_grain=True)
# Right arm
fill_wood_region(40, 16, 56, 32, vertical_grain=True)
# Right leg
fill_wood_region(0, 16, 16, 32, vertical_grain=True)
# Pants right
fill_wood_region(0, 32, 16, 48, vertical_grain=True)
# Jacket overlay
fill_wood_region(16, 32, 40, 48, vertical_grain=True)
# Sleeve right
fill_wood_region(40, 32, 56, 48, vertical_grain=True)
# Pants left
fill_wood_region(0, 48, 16, 64, vertical_grain=True)
# Left leg
fill_wood_region(16, 48, 32, 64, vertical_grain=True)
# Left arm
fill_wood_region(32, 48, 48, 64, vertical_grain=True)
# Sleeve left
fill_wood_region(48, 48, 64, 64, vertical_grain=True)

# ─── Adiciona "olhos" no rosto pra dar um look de totem assustador ─
# Face zone is on the head — depending on the zombie layout the face
# is the front of the head cube. In standard layout:
# Front of head face: x=8-16, y=8-16 (within the head 32x16 layout)
EYE_BLACK = (15, 8, 5, 255)
EYE_HIGHLIGHT = (220, 200, 100, 255)

# Olho esquerdo (no rosto)
d.rectangle([10, 11, 11, 12], fill=EYE_BLACK)
img.putpixel((10, 11), EYE_HIGHLIGHT)  # brilho

# Olho direito
d.rectangle([13, 11, 14, 12], fill=EYE_BLACK)
img.putpixel((13, 11), EYE_HIGHLIGHT)

# Boca (linha horizontal escura)
for x in range(10, 15):
    img.putpixel((x, 14), WOOD_KNOT_DARK)

# Crack/rachadura no peito (no body, vista frontal: x=20-28, y=20-32)
for y in range(20, 30):
    img.putpixel((24, y), WOOD_KNOT_DARK)
    if random.random() < 0.4:
        img.putpixel((23, y), WOOD_DARK)

img.save(OUT)
print(f"Saved {OUT}")
print(f"Size: {img.size}")
