from PIL import Image, ImageDraw
import math, random

# Atlas 64x64 casando com MiteModel:
#   body 6x4x7 @ (0,0)   -> (0,0)-(26,11)
#   head 4x3x3 @ (0,12)  -> (0,12)-(14,18)  front (3,15)-(7,18)
#   leg  1x4x1 @ (28,0)  -> (28,0)-(32,5)
#   antenna 1x4x1 @ (28,6) -> (28,6)-(32,11)
#   wing 8x1x6 @ (34,0)  -> (34,0)-(62,7)  top-face (40,0)-(48,6)
A = "src/main/resources/assets/liberthia/textures/entity"
S = 64
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
random.seed(387)

def fill(x0, y0, x1, y1, col):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), col + (255,))

def mottle(x0, y0, x1, y1, base, spots, n):
    fill(x0, y0, x1, y1, base)
    for _ in range(n):
        x = random.randint(x0, x1 - 1); y = random.randint(y0, y1 - 1)
        img.putpixel((x, y), random.choice(spots) + (255,))

# corpo — quitina escura iridescente (roxo/azul)
mottle(0, 0, 26, 11, (52, 46, 62), [(80, 60, 100), (40, 38, 56), (64, 70, 110)], 70)

# cabeça — mais escura + olhos vermelhos na face frontal (3,15)-(7,18)
mottle(0, 12, 14, 18, (40, 36, 50), [(30, 28, 40), (56, 48, 66)], 18)
img.putpixel((4, 16), (190, 40, 40, 255)); img.putpixel((4, 15), (120, 20, 20, 255))
img.putpixel((6, 16), (190, 40, 40, 255)); img.putpixel((6, 15), (120, 20, 20, 255))

# patas — escuras
mottle(28, 0, 32, 5, (34, 30, 38), [(24, 22, 28), (50, 44, 54)], 8)

# antenas — escuras com ponta clara (topo y=6..7)
fill(28, 6, 32, 11, (38, 34, 44))
fill(28, 6, 32, 7, (150, 140, 165))  # ponta sensitiva

# asas — escamosa cinza-violeta com veias + olho-falso (mariposa)
mottle(34, 0, 62, 7, (138, 122, 152), [(110, 98, 126), (158, 144, 172), (96, 86, 112)], 60)
# veias
for vx in range(36, 60, 4):
    for y in range(0, 7):
        if vx < 62:
            img.putpixel((vx, y), (92, 82, 108, 255))
# olho-falso na face superior (40,0)-(48,6)
cx, cy = 44, 3
for y in range(0, 6):
    for x in range(40, 48):
        dist = math.hypot(x - cx, y - cy)
        if dist <= 2.6:
            img.putpixel((x, y), ((215, 205, 230) if dist > 1.2 else (30, 24, 40)) + (255,))

img.save(A + "/mite.png")
print("mite.png gerado (64x64 UV: quitina iridescente + olhos + asas com olho-falso)")
