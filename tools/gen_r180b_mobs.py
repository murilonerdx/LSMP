"""
r180b: texturas — Arquivista do Fim (skin 64x64) + Amalgamado Cego (billboard 64x64)
+ Olho Parasita (billboard 32x32).
"""
import os, random, math
from PIL import Image, ImageDraw

random.seed(1802)
A = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources/assets/liberthia"
ENT = os.path.join(A, "textures/entity"); os.makedirs(ENT, exist_ok=True)

def c(v): return max(0, min(255, int(v)))

# ── Arquivista do Fim — skin humanoide 64x64 (manto escuro, tinta dourada) ──
def gen_archivist():
    W = H = 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255)); px = img.load()
    body = (38, 34, 28); dark = (20, 18, 14); ink = (200, 170, 70)
    for y in range(H):
        for x in range(W):
            f = 1.12 - 0.30 * (y / float(H)); jit = random.randint(-9, 9)
            r, g, b = body
            if random.random() < 0.13: r, g, b = dark
            if random.random() < 0.05: r, g, b = ink   # respingos de tinta dourada
            px[x, y] = (c(r*f+jit), c(g*f+jit), c(b*f+jit), 255)
    # rosto vazio escuro + olhos pálidos
    for yy in range(8, 16):
        for xx in range(8, 16):
            px[xx, yy] = (12, 11, 9, 255)
    for ex in (10, 13):
        px[ex, 11] = (220, 210, 180, 255)
    img.save(os.path.join(ENT, "end_archivist.png")); print("OK end_archivist.png")

# ── Amalgamado Cego — billboard 64x64 (massa carnuda coberta de olhos) ──
def gen_amalgam():
    W = H = 64; img = Image.new("RGBA", (W, H), (0, 0, 0, 0)); px = img.load()
    cx, cy = 32, 34
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - cx, y - cy) / 30.0
            if d > 1.0: continue
            n = random.uniform(-0.08, 0.08)
            shade = 1.0 - d * 0.55 + n
            r = c(150 * shade + 40); g = c(40 * shade + 12); b = c(48 * shade + 16)
            px[x, y] = (r, g, b, 255)
    # olhos espalhados (amarelos com pupila)
    for _ in range(14):
        ex = random.randint(8, 55); ey = random.randint(10, 55)
        if px[ex, ey][3] == 0: continue
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if 0 <= ex+dx < W and 0 <= ey+dy < H and px[ex+dx, ey+dy][3] > 0:
                    px[ex+dx, ey+dy] = (235, 220, 120, 255)
        px[ex, ey] = (10, 8, 8, 255)  # pupila
    img.save(os.path.join(ENT, "blind_amalgam.png")); print("OK blind_amalgam.png")

# ── Olho Parasita — billboard 32x32 (um olho injetado com tentáculos) ──
def gen_parasitic_eye():
    W = H = 32; img = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    # esclera
    d.ellipse((6, 8, 25, 23), fill=(225, 215, 205, 255), outline=(120, 30, 30, 255))
    # veias
    for _ in range(6):
        a = random.uniform(0, 2*math.pi)
        d.line((16, 15, 16+math.cos(a)*8, 15+math.sin(a)*6), fill=(180, 40, 40, 255))
    # íris + pupila
    d.ellipse((12, 11, 20, 19), fill=(150, 90, 40, 255))
    d.ellipse((14, 13, 18, 17), fill=(10, 8, 8, 255))
    d.point((15, 14), fill=(255, 255, 255, 255))
    # tentáculos pendurados
    for tx in (10, 16, 22):
        d.line((tx, 22, tx+random.randint(-2, 2), 30), fill=(150, 40, 48, 255))
    img.save(os.path.join(ENT, "parasitic_eye.png")); print("OK parasitic_eye.png")

if __name__ == "__main__":
    gen_archivist(); gen_amalgam(); gen_parasitic_eye()
