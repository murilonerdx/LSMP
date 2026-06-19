from PIL import Image, ImageDraw
import math, random, os

A = "src/main/resources/assets/liberthia/textures"
os.makedirs(A + "/item", exist_ok=True)
os.makedirs(A + "/block", exist_ok=True)
os.makedirs(A + "/entity", exist_ok=True)
os.makedirs(A + "/mob_effect", exist_ok=True)

def C(v, a=255): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, a)

# ── adaga 16x16 diagonal (cabo embaixo-esq, lâmina topo-dir) ──
def dagger(name, blade, edge, gem):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    # cabo
    d.line([3, 13, 5, 11], fill=C(0x3A2A1A), width=2)
    d.line([2, 14, 4, 12], fill=C(0x5A4028), width=1)
    # guarda
    d.line([4, 12, 7, 9], fill=C(0x8A8A9A), width=1)
    d.point((5, 9), fill=C(gem)); d.point((6, 10), fill=C(gem))
    # lâmina diagonal
    for i in range(8):
        x = 6 + i; y = 10 - i
        if 0 <= x < 16 and 0 <= y < 16:
            d.point((x, y), fill=C(blade))
            if x + 1 < 16: d.point((x + 1, y), fill=C(edge))
            if y - 1 >= 0: d.point((x, y - 1), fill=C(edge))
    # ponta
    d.point((14, 2), fill=C(0xFFFFFF))
    im.save(A + "/item/" + name + ".png")

dagger("rift_cutter_t1", 0x9FD0FF, 0xE8F4FF, 0x4FA0FF)   # azul
dagger("rift_cutter_t2", 0xFF9A55, 0xFFD0A0, 0xFF5020)   # nether/laranja
dagger("rift_cutter_t3", 0xC060FF, 0xF0C0FF, 0xFF40FF)   # violeta cósmico

# ── bloco da forja 16x16 (pedra negra + runas roxas + núcleo) ──
im = Image.new("RGBA", (16, 16), C(0x141018)); d = ImageDraw.Draw(im)
random.seed(99)
for _ in range(40):
    x, y = random.randint(0, 15), random.randint(0, 15)
    d.point((x, y), fill=C(random.choice([0x1C1626, 0x0E0A14, 0x241A30])))
# borda
d.rectangle([0, 0, 15, 15], outline=C(0x2A2038))
# runas roxas nas bordas
for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
    d.point((x, y), fill=C(0xB060FF)); d.point((x, y + 1) if y < 8 else (x, y - 1), fill=C(0x8030C0))
# núcleo brilhante central (fenda)
d.ellipse([5, 4, 10, 11], fill=C(0x3A1060))
for i in range(6):
    yy = 4 + i
    w = 1 + int(2 * math.sin(i / 5 * math.pi))
    d.line([8 - w, yy, 8 + w, yy], fill=C(0xC060FF))
d.point((8, 7), fill=C(0xF0D0FF))
im.save(A + "/block/rift_forge.png")

# ── ícone de efeito 18x18: radiação dimensional (violeta + símbolo radioativo) ──
im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.ellipse([1, 1, 16, 16], fill=C(0x1A0E2E), outline=C(0x6A30B0))
cx = cy = 9
for k in range(3):
    a0 = math.radians(90 + k * 120 - 30); a1 = math.radians(90 + k * 120 + 30)
    d.pieslice([3, 3, 14, 14], math.degrees(a0), math.degrees(a1), fill=C(0xC060FF))
d.ellipse([7, 7, 11, 11], fill=C(0x1A0E2E))
d.ellipse([8, 8, 10, 10], fill=C(0xF0C0FF))
im.save(A + "/mob_effect/dimensional_radiation.png")

# ── REFAZER dimensional_rift.png 64x64: rasgo JAGGED irregular (não oval/fenda) ──
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
random.seed(7)
# vazio estrelado dentro de um contorno de fratura irregular
# 1) define um polígono irregular (fratura) — bordas dentadas, não simétrico
cx, cy = 32, 32
pts = []
n = 22
for i in range(n):
    ang = i / n * math.tau
    # raio irregular: varia muito (dentes) + assimétrico vertical
    base = 16 + 9 * math.sin(ang * 3 + 1.3) + 6 * math.cos(ang * 5)
    jag = random.randint(-6, 6)
    r = max(6, base + jag)
    pts.append((cx + math.cos(ang) * r * 0.7, cy + math.sin(ang) * r))
# interior = vazio escuro
d.polygon(pts, fill=C(0x05030A))
# estrelas dentro
for _ in range(40):
    x, y = random.randint(8, 56), random.randint(6, 58)
    # só desenha se "dentro" aproximado
    if (x - cx) ** 2 / (20 ** 2) + (y - cy) ** 2 / (28 ** 2) < 1:
        d.point((x, y), fill=C(random.choice([0xFFFFFF, 0xC0C0FF, 0x9090E0])))
# 2) borda da fratura — linha quebrada brilhante (roxo→branco), espessura variável
for i in range(len(pts)):
    p0 = pts[i]; p1 = pts[(i + 1) % len(pts)]
    d.line([p0, p1], fill=C(0x8030C0), width=2)
    d.line([p0, p1], fill=C(0xE0A0FF), width=1)
# 3) rachaduras saindo do centro (raios irregulares)
for k in range(5):
    a = random.random() * math.tau
    x, y = cx, cy
    for step in range(18):
        a += (random.random() - 0.5) * 1.0
        x += math.cos(a) * 1.4; y += math.sin(a) * 1.6
        if 0 <= x < 64 and 0 <= y < 64:
            d.point((x, y), fill=C(0xB070E0))
im.save(A + "/entity/dimensional_rift.png")

print("texturas da adaga + forja + radiacao + fenda(jagged) geradas")
