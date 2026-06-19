from PIL import Image, ImageDraw
import math, random

# Atlas 64x64 casando com CrawlerModel:
#   body 8x5x12 @ (0,0)    -> (0,0)-(40,17)
#   head 6x5x5  @ (0,18)   -> (0,18)-(22,28)
#   upper_jaw 6x2x5 @ (24,18) -> (24,18)-(46,25)  front (29,23)-(35,25)
#   lower_jaw 6x2x5 @ (24,26) -> (24,26)-(46,33)  front (29,31)-(35,33)
#   leg 2x6x2  @ (48,0)    -> (48,0)-(56,8)
A = "src/main/resources/assets/liberthia/textures/entity"
S = 64
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
random.seed(287)

def fill(x0, y0, x1, y1, col):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), col + (255,))

def mottle(x0, y0, x1, y1, base, spots, n):
    fill(x0, y0, x1, y1, base)
    for _ in range(n):
        x = random.randint(x0, x1 - 1); y = random.randint(y0, y1 - 1)
        c = random.choice(spots)
        img.putpixel((x, y), c + (255,))

# corpo — carne escura mosqueada + veias
mottle(0, 0, 40, 17, (74, 62, 55), [(58, 46, 42), (92, 70, 60), (60, 50, 46)], 90)
for _ in range(14):
    x0 = random.randint(0, 39); y0 = random.randint(0, 16); ang = random.random() * math.tau
    ln = random.randint(3, 8)
    x1 = max(0, min(39, int(x0 + math.cos(ang) * ln))); y1 = max(0, min(16, int(y0 + math.sin(ang) * ln)))
    d.line([x0, y0, x1, y1], fill=(150, 40, 40, 255), width=1)

# cabeça — mais escura
mottle(0, 18, 22, 28, (58, 50, 46), [(44, 38, 36), (72, 60, 54)], 30)

# mandíbulas — osso pálido
fill(24, 18, 46, 25, (206, 196, 173))
fill(24, 26, 46, 33, (200, 190, 168))
# dentes nas faces frontais (linhas verticais escuras)
for x in range(29, 35):
    if (x - 29) % 2 == 0:
        for y in range(23, 25):
            img.putpixel((x, y), (120, 95, 70, 255))
        for y in range(31, 33):
            img.putpixel((x, y), (110, 88, 64, 255))

# patas — quitina escura com borda clara
mottle(48, 0, 56, 8, (46, 40, 42), [(34, 30, 32), (62, 54, 56)], 18)
for y in range(0, 8):
    img.putpixel((48, y), (70, 62, 64, 255))

img.save(A + "/crawler.png")
print("crawler.png gerado (64x64 UV: corpo carnudo + mandibulas com dentes + patas)")
