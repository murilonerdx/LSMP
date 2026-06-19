from PIL import Image, ImageDraw
import math, random, os

A = "src/main/resources/assets/liberthia/textures"
os.makedirs(A + "/mob_effect", exist_ok=True)
os.makedirs(A + "/entity", exist_ok=True)
os.makedirs(A + "/item", exist_ok=True)

def C(v, a=255): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, a)

# ── ícones de efeito 18x18 ──
def memory_icon():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    # cérebro/espiral roxa
    for r, col in [(7, 0x3A1052), (5, 0x6A20A0), (3, 0x9B30D0)]:
        d.ellipse([9 - r, 9 - r, 9 + r, 9 + r], outline=C(col), width=1)
    random.seed(1)
    for _ in range(10):
        a = random.random() * math.tau; rr = random.random() * 6
        d.point((9 + math.cos(a) * rr, 9 + math.sin(a) * rr), fill=C(0xE0A0FF))
    im.save(A + "/mob_effect/dimensional_memory.png")

def infection_icon():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    # verme verde sinuoso
    pts = []
    for i in range(16):
        x = 2 + i; y = 9 + math.sin(i * 0.9) * 4
        pts.append((x, y))
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=C(0x2A5C16), width=3)
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=C(0x4FA02A), width=1)
    random.seed(2)
    for _ in range(8):
        d.point((random.randint(1, 16), random.randint(1, 16)), fill=C(0x9AE060))
    im.save(A + "/mob_effect/dimensional_blight.png")

def paranoia_icon():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    # olho inquietante
    d.ellipse([1, 5, 16, 13], outline=C(0x3A2C66), width=1, fill=C(0x150E2A))
    d.ellipse([6, 5, 12, 13], fill=C(0x6A4FB5))            # íris
    d.ellipse([8, 7, 11, 11], fill=C(0x0A0614))            # pupila
    d.point((9, 8), fill=C(0xC0B0FF))                      # brilho
    im.save(A + "/mob_effect/dimensional_paranoia.png")

memory_icon(); infection_icon(); paranoia_icon()

# ── verme dimensional 64x64 (billboard) ──
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
random.seed(7)
cx = 32
# corpo segmentado vertical sinuoso (roxo->verde, tema dimensional)
y = 6
seg = 0
while y < 58:
    x = cx + math.sin(y * 0.25) * 10
    rad = 7 - abs(y - 32) * 0.04
    col = 0x6A30B0 if seg % 2 == 0 else 0x3FA02A
    d.ellipse([x - rad, y - rad, x + rad, y + rad], fill=C(col), outline=C(0x1A0A2A))
    # brilho central
    d.ellipse([x - rad * 0.4, y - rad * 0.4, x + rad * 0.4, y + rad * 0.4], fill=C(0xC0A0FF, 180))
    y += 6; seg += 1
# cabeça com mandíbulas
hx = cx + math.sin(6 * 0.25) * 10
d.ellipse([hx - 8, 2, hx + 8, 16], fill=C(0x8030C0), outline=C(0x1A0A2A))
d.point((hx - 3, 8), fill=C(0xFF4060)); d.point((hx + 3, 8), fill=C(0xFF4060))  # olhos vermelhos
im.save(A + "/entity/dimensional_worm.png")

# ── soro dimensional 16x16 (frasco com líquido roxo) ──
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
# rolha
d.rectangle([6, 1, 9, 3], fill=C(0x6B4226))
# gargalo
d.rectangle([6, 3, 9, 5], fill=C(0xBFC8D0))
# corpo do frasco (vidro)
d.ellipse([3, 5, 12, 15], outline=C(0xBFC8D0), width=1, fill=C(0x2A1838))
# líquido roxo brilhante
d.ellipse([4, 8, 11, 14], fill=C(0x9B30D0))
d.ellipse([5, 9, 9, 12], fill=C(0xC060F0))
d.point((6, 10), fill=C(0xF0C0FF))  # brilho
# partículas em volta
d.point((2, 7), fill=C(0xC060F0)); d.point((13, 9), fill=C(0xC060F0))
im.save(A + "/item/dimensional_serum.png")

print("texturas das doenças dimensionais geradas (3 icones + verme + soro)")
