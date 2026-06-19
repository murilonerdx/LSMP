from PIL import Image, ImageDraw, ImageFilter
import math, os, random
DIR = "src/main/resources/assets/liberthia/textures/entity"
os.makedirs(DIR, exist_ok=True)
S = 64

def C(v, a=255): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, a)
def lt(c, f): return tuple(min(255, int(x + (255 - x) * f)) for x in c[:3]) + (c[3] if len(c) > 3 else 255,)
def dk(c, f): return tuple(int(x * (1 - f)) for x in c[:3]) + (c[3] if len(c) > 3 else 255,)
def newL(): return Image.new("RGBA", (S, S), (0, 0, 0, 0))

def glow(color, blobs, blur=6, a=150):
    g = newL(); d = ImageDraw.Draw(g)
    for (x, y, r) in blobs:
        d.ellipse([x - r, y - r, x + r, y + r], fill=(C(color)[0], C(color)[1], C(color)[2], a))
    return g.filter(ImageFilter.GaussianBlur(blur))

def compose(*layers):
    out = newL()
    for l in layers:
        if l is not None: out = Image.alpha_composite(out, l)
    return out

def eye(d, cx, cy, w, h, iris, pupil="slit", veins=True):
    ic = C(iris)
    scl = lt(ic, 0.78)
    d.ellipse([cx - w, cy - h, cx + w, cy + h], fill=scl, outline=dk(ic, 0.6), width=2)
    if veins:
        for a in range(0, 360, 24):
            ex = cx + math.cos(math.radians(a)) * w * 0.9; ey = cy + math.sin(math.radians(a)) * h * 0.9
            d.line([cx, cy, ex, ey], fill=(150, 30, 30, 120), width=1)
    ir = min(w, h) * 0.62
    d.ellipse([cx - ir, cy - ir, cx + ir, cy + ir], fill=ic, outline=dk(ic, 0.4))
    d.ellipse([cx - ir * 0.55, cy - ir * 0.55, cx + ir * 0.55, cy + ir * 0.55], fill=lt(ic, 0.25))
    if pupil == "slit":
        d.ellipse([cx - ir * 0.22, cy - ir * 0.8, cx + ir * 0.22, cy + ir * 0.8], fill=(6, 4, 8, 255))
    else:
        d.ellipse([cx - ir * 0.45, cy - ir * 0.45, cx + ir * 0.45, cy + ir * 0.45], fill=(6, 4, 8, 255))
    d.ellipse([cx - ir * 0.45, cy - ir * 0.6, cx - ir * 0.05, cy - ir * 0.2], fill=(255, 255, 255, 210))

def legs(d, cx, cy, n, length, col, spread=2.2):
    for i in range(n):
        side = -1 if i < n // 2 else 1
        t = (i % (n // 2)) / max(1, (n // 2 - 1))
        x2 = cx + side * length; y2 = cy + (t - 0.5) * length * spread
        mx = cx + side * length * 0.5; my = cy + (t - 0.5) * length * 0.7 - 6
        d.line([cx, cy, mx, my], fill=col, width=2); d.line([mx, my, x2, y2], fill=col, width=2)

def save(name, sharp, glw=None): compose(glw, sharp).save(DIR + "/" + name + ".png")

# 1 cinder_parasite
g = glow(0xFF8030, [(32, 34, 16)], 7, 170); s = newL(); d = ImageDraw.Draw(s)
d.polygon([32, 6, 46, 32, 32, 52, 18, 32], fill=C(0xC83410), outline=C(0xFF7020))
d.polygon([32, 14, 40, 32, 32, 46, 24, 32], fill=C(0xFF9020)); d.polygon([32, 20, 36, 32, 32, 42, 28, 32], fill=C(0xFFE060))
d.ellipse([28, 28, 36, 38], fill=(255, 255, 210, 255))
for a in (20, 90, 160, 250, 320):
    x = 32 + math.cos(math.radians(a)) * 20; y = 34 + math.sin(math.radians(a)) * 20; d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=C(0xFFC040))
save("cinder_parasite", s, g)

# 2 gaze_leech
g = glow(0xA030D0, [(32, 26, 18), (32, 46, 10)], 6, 150); s = newL(); d = ImageDraw.Draw(s)
for i in range(5):
    x = 20 + i * 6; d.line([x, 40, x - 3 + random.randint(-1, 1), 60], fill=C(0x5A1880), width=3)
eye(d, 32, 26, 22, 15, 0xA030D0, "slit"); save("gaze_leech", s, g)

# 3 blind_weaver
g = glow(0x5050C0, [(32, 34, 17)], 6, 130); s = newL(); d = ImageDraw.Draw(s)
legs(d, 32, 32, 8, 22, C(0x2A2A55)); d.ellipse([20, 30, 44, 54], fill=C(0x23234A), outline=C(0x4848B0)); d.ellipse([24, 18, 40, 34], fill=C(0x303060))
for (ex, ey) in [(27, 23), (31, 22), (35, 22), (39, 23), (29, 27), (37, 27)]: d.ellipse([ex, ey, ex + 2, ey + 2], fill=C(0x90E0FF))
for a in range(0, 360, 45): d.line([32, 40, 32 + math.cos(math.radians(a)) * 30, 40 + math.sin(math.radians(a)) * 22], fill=(120, 120, 200, 40))
save("blind_weaver", s, g)

# 4 maw_crawler
g = glow(0xB02828, [(32, 32, 18)], 6, 150); s = newL(); d = ImageDraw.Draw(s)
legs(d, 32, 32, 6, 18, C(0x3A1010)); d.ellipse([10, 12, 54, 52], fill=C(0x4A1414), outline=C(0xB03030), width=2); d.ellipse([18, 18, 46, 46], fill=(14, 4, 6, 255))
for a in range(0, 360, 28):
    x = 32 + math.cos(math.radians(a)) * 15; y = 32 + math.sin(math.radians(a)) * 15; x2 = 32 + math.cos(math.radians(a)) * 9; y2 = 32 + math.sin(math.radians(a)) * 9
    d.polygon([x - 2, y, x + 2, y, x2, y2], fill=(235, 225, 205, 255))
d.ellipse([27, 27, 37, 37], fill=C(0xFF4030)); d.ellipse([30, 30, 34, 34], fill=(255, 220, 180, 255)); save("maw_crawler", s, g)

# 5 whisper_mite
g = glow(0x60C060, [(32, 34, 11)], 5, 120); s = newL(); d = ImageDraw.Draw(s)
legs(d, 32, 36, 6, 12, C(0x1E3A1E)); d.ellipse([24, 30, 40, 46], fill=C(0x244024), outline=C(0x60A060)); d.ellipse([26, 22, 38, 34], fill=C(0x305030))
d.line([29, 24, 25, 16], fill=C(0x60A060)); d.line([35, 24, 39, 16], fill=C(0x60A060))
d.ellipse([28, 26, 31, 29], fill=C(0xA0FFA0)); d.ellipse([33, 26, 36, 29], fill=C(0xA0FFA0)); d.ellipse([30, 36, 34, 42], fill=C(0x90FF90)); save("whisper_mite", s, g)

# 6 dread_orb
g = glow(0x7028C0, [(32, 32, 24)], 8, 160); s = newL(); d = ImageDraw.Draw(s)
for r in range(26, 6, -2):
    f = (26 - r) / 20; d.ellipse([32 - r, 32 - r, 32 + r, 32 + r], fill=lt(C(0x5018A0), f * 0.6))
for a in range(0, 360, 30): d.arc([10, 10, 54, 54], a, a + 18, fill=(200, 150, 255, 120), width=2)
eye(d, 32, 32, 11, 9, 0xB060FF, "round", veins=False)
for a in (0, 72, 144, 216, 288):
    x = 32 + math.cos(math.radians(a)) * 28; y = 32 + math.sin(math.radians(a)) * 28; d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=C(0xD0A0FF))
save("dread_orb", s, g)

# 7 flesh_watcher
g = glow(0xC05858, [(32, 34, 18)], 6, 140); s = newL(); d = ImageDraw.Draw(s)
pts = [(32 + math.cos(math.radians(a)) * (16 + (a * 7 % 11)), 34 + math.sin(math.radians(a)) * (15 + (a * 5 % 9))) for a in range(0, 360, 18)]
d.polygon(pts, fill=C(0x6E2A2A), outline=C(0xC05858))
for (ex, ey, r) in [(24, 28, 4), (40, 26, 5), (30, 40, 3), (44, 40, 4), (20, 40, 3)]: eye(d, ex, ey, r, r, 0xE08080, "round", veins=False)
for x in (26, 34, 42): d.line([x, 48, x, 56], fill=C(0x4A1A1A), width=2)
save("flesh_watcher", s, g)

# 8 void_tick
g = glow(0x3838C0, [(32, 38, 13)], 6, 140); s = newL(); d = ImageDraw.Draw(s)
legs(d, 32, 34, 8, 16, C(0x14143A)); d.ellipse([20, 28, 44, 52], fill=C(0x10103A), outline=C(0x3838A8)); d.ellipse([26, 24, 38, 34], fill=C(0x1A1A48))
for r in range(10, 2, -2): d.ellipse([32 - r, 42 - r // 2, 32 + r, 42 + r // 2], fill=lt(C(0x3030C0), (10 - r) / 8))
d.ellipse([28, 26, 31, 29], fill=C(0x80A0FF)); d.ellipse([34, 26, 37, 29], fill=C(0x80A0FF)); save("void_tick", s, g)

# 9 gloom_moth
g = glow(0x6060A0, [(20, 30, 12), (44, 30, 12)], 6, 110); s = newL(); d = ImageDraw.Draw(s)
for sx in (-1, 1):
    d.polygon([32, 30, 32 + sx * 26, 16, 32 + sx * 30, 40, 32 + sx * 14, 44], fill=C(0x2A2A40), outline=C(0x484868))
    ex = 32 + sx * 20; d.ellipse([ex - 4, 26, ex + 4, 34], fill=C(0x6060A0)); d.ellipse([ex - 2, 28, ex + 2, 32], fill=(20, 10, 30, 255))
d.ellipse([28, 22, 36, 48], fill=C(0x383850)); d.line([30, 22, 24, 12], fill=C(0x484868)); d.line([34, 22, 40, 12], fill=C(0x484868))
d.ellipse([29, 24, 31, 26], fill=C(0xC0C0FF)); d.ellipse([33, 24, 35, 26], fill=C(0xC0C0FF)); save("gloom_moth", s, g)

# 10 rot_eye
g = glow(0x88B020, [(32, 30, 16)], 6, 140); s = newL(); d = ImageDraw.Draw(s)
for i in range(6):
    x = 18 + i * 6; d.line([x, 42, x + random.randint(-2, 2), 58], fill=C(0x4A5A10), width=3)
eye(d, 32, 28, 20, 14, 0x88B020, "slit")
for (sx, sy) in [(22, 20), (42, 22), (30, 36), (40, 34)]: d.ellipse([sx, sy, sx + 3, sy + 3], fill=C(0xC0E040))
save("rot_eye", s, g)

# 11 scream_larva
g = glow(0xD8B848, [(32, 30, 15)], 6, 140); s = newL(); d = ImageDraw.Draw(s)
for i, y in enumerate(range(34, 58, 5)):
    r = 8 - i; d.ellipse([32 - r, y, 32 + r, y + 6], fill=lt(C(0x8A7020), i * 0.08), outline=C(0x5A4810))
d.ellipse([18, 12, 46, 40], fill=C(0xB89838), outline=C(0xD8B848)); d.ellipse([24, 20, 40, 38], fill=(12, 8, 4, 255))
for x in range(24, 40, 4):
    d.polygon([x, 20, x + 2, 20, x + 1, 25], fill=(240, 235, 210, 255)); d.polygon([x, 38, x + 2, 38, x + 1, 33], fill=(240, 235, 210, 255))
d.ellipse([22, 15, 27, 20], fill=(255, 250, 200, 255)); d.ellipse([37, 15, 42, 20], fill=(255, 250, 200, 255))
for r in (24, 30): d.arc([32 - r, 30 - r, 32 + r, 30 + r], 200, 340, fill=(255, 240, 160, 70), width=2)
save("scream_larva", s, g)

# 12 mirror_spawn
g = glow(0x8888C8, [(32, 32, 16)], 6, 130); s = newL(); d = ImageDraw.Draw(s)
d.polygon([32, 6, 52, 28, 42, 56, 18, 52, 12, 26], fill=C(0x2A2A40), outline=C(0x9090D0)); d.polygon([32, 12, 46, 30, 36, 48, 22, 44], fill=C(0x4A4A70))
eye(d, 33, 30, 9, 11, 0xB0B0FF, "slit", veins=False)
for (a, b, c, e) in [(14, 18, 26, 30), (44, 20, 52, 34), (20, 46, 30, 50)]: d.line([a, b, c, e], fill=(220, 220, 255, 150), width=1)
save("mirror_spawn", s, g)

# 13 parasite_host
g = glow(0x60C838, [(32, 34, 18)], 7, 150); s = newL(); d = ImageDraw.Draw(s)
for a in range(0, 360, 40):
    x = 32 + math.cos(math.radians(a)) * 26; y = 34 + math.sin(math.radians(a)) * 24; d.line([32, 34, x, y], fill=C(0x2A5018), width=3); d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=C(0x80E040))
d.ellipse([16, 20, 48, 52], fill=C(0x305018), outline=C(0x60C838))
for (ex, ey, r) in [(26, 30, 4), (40, 30, 4), (33, 42, 5)]: eye(d, ex, ey, r, r, 0xA0FF40, "round", veins=False)
for (sx, sy) in [(22, 26), (44, 38), (28, 46)]: d.ellipse([sx, sy, sx + 5, sy + 5], fill=C(0x90D050))
save("parasite_host", s, g)

# 14 colossal_eye
g = glow(0x9838D8, [(32, 32, 28)], 9, 170); s = newL(); d = ImageDraw.Draw(s)
for a in range(0, 360, 15):
    x = 32 + math.cos(math.radians(a)) * 30; y = 32 + math.sin(math.radians(a)) * 30; d.line([32, 32, x, y], fill=(120, 50, 200, 90), width=2)
d.ellipse([4, 12, 60, 52], fill=lt(C(0x9838D8), 0.7), outline=dk(C(0x9838D8), 0.5), width=3)
eye(d, 32, 32, 26, 18, 0x9838D8, "slit")
for a in range(0, 360, 45):
    x = 32 + math.cos(math.radians(a)) * 30; y = 32 + math.sin(math.radians(a)) * 22; d.ellipse([x - 3, y - 2, x + 3, y + 2], fill=lt(C(0x9838D8), 0.6)); d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(8, 4, 10, 255))
save("colossal_eye", s, g)

print("14 texturas 64x64 detalhadas com brilho geradas")
