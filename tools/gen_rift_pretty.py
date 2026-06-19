from PIL import Image, ImageDraw, ImageFilter
import math, random

A = "src/main/resources/assets/liberthia/textures/entity"
S = 128
cx = cy = S / 2.0

random.seed(77)
# nebulosas internas (manchas de cor que dão profundidade ao "outro lado")
blobs = [(random.randint(38, 90), random.randint(32, 96), random.randint(16, 34),
          random.choice([(190, 70, 230), (110, 70, 235), (60, 170, 230), (210, 90, 190), (90, 120, 255)]))
         for _ in range(8)]

def outline_r(ang):
    # contorno ORGÂNICO arredondado (não um slit vertical) — leves ondulações
    return 47 + 5 * math.sin(ang * 3 + 0.6) + 3.5 * math.cos(ang * 5 + 1.2) + 2.5 * math.sin(ang * 8)

img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
px = img.load()
for y in range(S):
    for x in range(S):
        dx = x - cx
        dy = (y - cy) * 0.9          # leve achatamento vertical → mais "portal", menos slit
        dist = math.hypot(dx, dy)
        ang = math.atan2(dy, dx)
        R = outline_r(ang)
        if dist <= R:
            t = dist / R              # 0 centro → 1 borda
            # espaço profundo: centro quase preto → índigo
            r = int(6 + 18 * t); g = int(3 + 8 * t); b = int(22 + 55 * t)
            for (bx, by, br, (nr, ng, nb)) in blobs:   # nebulosa
                bd = math.hypot(x - bx, y - by)
                if bd < br:
                    w = (1 - bd / br) ** 2 * 0.65
                    r = min(255, int(r + nr * w)); g = min(255, int(g + ng * w)); b = min(255, int(b + nb * w))
            if t > 0.80:               # borda brilhante (rim glow ciano/branco)
                k = (t - 0.80) / 0.20
                r = min(255, int(r + 170 * k)); g = min(255, int(g + 215 * k)); b = min(255, int(b + 255 * k))
            px[x, y] = (r, g, b, 255)
        elif dist - R < 6:             # halo suave fora da borda
            k = 1 - (dist - R) / 6
            px[x, y] = (120, 200, 255, int(140 * k))

# estrelas dentro do portal
random.seed(12)
for _ in range(110):
    x = random.randint(0, S - 1); y = random.randint(0, S - 1)
    dx = x - cx; dy = (y - cy) * 0.9; ang = math.atan2(dy, dx)
    if math.hypot(dx, dy) < outline_r(ang) - 3:
        b = random.choice([255, 230, 210, 255, 190])
        col = (b, b, 255, 255) if random.random() < 0.5 else (b, b, b, 255)
        px[x, y] = col
        if random.random() < 0.22:     # brilho em cruz
            for (ox, oy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nx, ny = x + ox, y + oy
                if 0 <= nx < S and 0 <= ny < S and px[nx, ny][3] > 0:
                    px[nx, ny] = (min(255, col[0]), min(255, col[1]), 255, 220)

# rachaduras finas saindo da borda (energia vazando)
d = ImageDraw.Draw(img); random.seed(5)
for _ in range(7):
    a = random.random() * math.tau
    R = outline_r(a)
    x0 = cx + math.cos(a) * R; y0 = cy + math.sin(a) * R / 0.9
    ln = random.randint(8, 20)
    x1 = cx + math.cos(a) * (R + ln); y1 = cy + math.sin(a) * (R + ln) / 0.9
    d.line([x0, y0, x1, y1], fill=(160, 215, 255, 210), width=1)

# glow suave (halo) por baixo, original por cima
glow = img.filter(ImageFilter.GaussianBlur(1.4))
out = Image.alpha_composite(glow, img)
out.save(A + "/dimensional_rift.png")
print("dimensional_rift.png refeita: janela cósmica bonita (nebulosa+estrelas+borda brilhante, 128px)")
