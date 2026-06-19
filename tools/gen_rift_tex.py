from PIL import Image, ImageDraw
import math, random, os
B = "src/main/resources/assets/liberthia/textures/block"
os.makedirs(B, exist_ok=True)

def C(v): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, 255)

def base(bg):
    im = Image.new("RGBA", (16, 16), C(bg)); d = ImageDraw.Draw(im)
    # borda levemente mais escura p/ ladrilhar
    d.rectangle([0, 0, 15, 0], fill=C(0)); d.rectangle([0, 15, 15, 15], fill=C(0))
    return im, d

def cracks(d, cx, cy, color, n, rng, seed):
    random.seed(seed)
    for k in range(n):
        a = random.random() * math.tau
        x, y = cx, cy
        for step in range(rng):
            a += (random.random() - 0.5) * 1.1     # zigue-zague (fratura, não oval)
            x += math.cos(a); y += math.sin(a)
            if 0 <= x < 16 and 0 <= y < 16:
                d.point((x, y), fill=color)
                if step % 3 == 0:
                    d.point((min(15, x + 1), y), fill=color)

# dimensional_flux — fratura vermelha de energia
im, d = base(0x140810)
cracks(d, 8, 8, C(0x401018), 6, 16, 11)
cracks(d, 8, 8, C(0xE83040), 5, 14, 7)
cracks(d, 8, 8, C(0xFF8060), 4, 9, 3)
for (x, y) in [(8, 7), (7, 8), (9, 9)]: d.point((x, y), fill=C(0xFFE0C0))
im.save(B + "/dimensional_flux.png")

# warped_space — distorção magenta fragmentada
im, d = base(0x1A0E22)
random.seed(21)
for _ in range(7):
    x0, y0 = random.randint(1, 14), random.randint(1, 14)
    d.line([x0, y0, x0 + random.randint(-6, 6), y0 + random.randint(-6, 6)], fill=C(0x9030C0), width=1)
cracks(d, 8, 8, C(0xC050E0), 4, 12, 5)
for (x, y) in [(4, 5), (11, 6), (6, 11), (12, 12)]: d.point((x, y), fill=C(0xF0A0FF))
im.save(B + "/warped_space.png")

# void_scar — vazio negro com estrelas + rasgo irregular
im, d = base(0x05040A)
random.seed(33)
for _ in range(14):
    d.point((random.randint(0, 15), random.randint(0, 15)), fill=C(0xC0C0E0))
cracks(d, 8, 8, C(0x2A2050), 5, 15, 9)      # rasgo escuro
cracks(d, 8, 8, C(0x6040A0), 3, 10, 4)
im.save(B + "/void_scar.png")

# rift_residue — estilhaços de cristal roxo
im, d = base(0x1C1030)
random.seed(45)
for _ in range(6):
    cx, cy = random.randint(3, 12), random.randint(3, 12)
    pts = [(cx, cy - 3), (cx + 2, cy), (cx, cy + 3), (cx - 2, cy)]
    d.polygon(pts, fill=C(0x6A30B0), outline=C(0xB070E0))
    d.point((cx, cy), fill=C(0xE0B0FF))
im.save(B + "/rift_residue.png")
print("texturas das fendas refeitas (fraturas irregulares, sem oval)")
