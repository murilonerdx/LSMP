from PIL import Image, ImageDraw
import random

# Atlas 64x64 casando com AmalgamModel:
#   torso 10x14x8 @ (0,0)   -> (0,0)-(36,22)  front (8,8)-(18,22)
#   head 7x6x7   @ (0,24)   -> (0,24)-(28,37) front (7,31)-(14,37)
#   arm_big 4x12x4 @ (40,0) -> (40,0)-(56,16)
#   arm_small 3x9x3 @ (40,18)->(40,18)-(52,30)
#   leg 4x8x4    @ (40,34)  -> (40,34)-(56,46)
#   tendril 2x7x2 @ (0,40)  -> (0,40)-(8,49)
A = "src/main/resources/assets/liberthia/textures/entity"
S = 64
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
random.seed(587)

def mottle(x0, y0, x1, y1, base, spots, n):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), base + (255,))
    for _ in range(n):
        x = random.randint(x0, x1 - 1); y = random.randint(y0, y1 - 1)
        img.putpixel((x, y), random.choice(spots) + (255,))

FLESH = (118, 76, 74)
SPOTS = [(78, 48, 50), (150, 100, 96), (96, 60, 60), (165, 135, 128)]

# torso, cabeça, membros — carne crua mosqueada
mottle(0, 0, 36, 22, FLESH, SPOTS, 130)
mottle(0, 24, 28, 37, (104, 66, 64), SPOTS, 70)
mottle(40, 0, 56, 16, (98, 62, 60), SPOTS, 45)
mottle(40, 18, 52, 30, (98, 62, 60), SPOTS, 30)
mottle(40, 34, 56, 46, (90, 56, 56), SPOTS, 40)
mottle(0, 40, 8, 49, (84, 40, 46), [(60, 28, 34), (120, 60, 64)], 14)

def sewn_eye(cx, cy):
    """olho costurado/cego: linha escura + pontos de sutura verticais."""
    for x in range(cx - 2, cx + 3):
        if 0 <= x < S: img.putpixel((x, cy), (40, 24, 26, 255))
    for x in range(cx - 2, cx + 3, 2):
        if 0 <= x < S:
            img.putpixel((x, cy - 1), (70, 50, 50, 255))
            img.putpixel((x, cy + 1), (70, 50, 50, 255))

def blind_eye(cx, cy):
    """olho cego leitoso."""
    for oy in range(-1, 2):
        for ox in range(-1, 2):
            img.putpixel((cx + ox, cy + oy), (205, 200, 190, 255))
    img.putpixel((cx, cy), (150, 150, 160, 255))  # pupila apagada

# olhos no torso (cluster) — maioria costurada, 1-2 cegos
sewn_eye(11, 11); sewn_eye(15, 13); sewn_eye(12, 17)
blind_eye(16, 10); blind_eye(10, 15)
# costura/boca grande vertical no torso
for y in range(12, 20):
    img.putpixel((13, y), (44, 26, 28, 255))
for y in range(12, 20, 2):
    img.putpixel((12, y), (74, 52, 52, 255)); img.putpixel((14, y), (74, 52, 52, 255))

# olhos na cabeça
sewn_eye(9, 33); sewn_eye(12, 34)
blind_eye(10, 35)

img.save(A + "/amalgam.png")
print("amalgam.png gerado (64x64 UV: carne fundida + cicatrizes + olhos cegos/costurados)")
