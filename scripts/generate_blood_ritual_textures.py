"""Generate textures for blood ritual + armor content."""
import os, struct, zlib, random

BASE = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures"

def png_write(path, pixels, w=16, h=16):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    raw = b""
    for y in range(h):
        raw += b"\x00"
        for x in range(w):
            r,g,b,a = pixels[y][x]
            raw += bytes([r,g,b,a])
    data = zlib.compress(raw)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", data))
        f.write(chunk(b"IEND", b""))

def noisy(base, variance=20, w=16, h=16, seed=0):
    random.seed(seed)
    pix = []
    for y in range(h):
        row = []
        for x in range(w):
            r,g,b = base
            dr = random.randint(-variance, variance)
            r = max(0, min(255, r+dr))
            g = max(0, min(255, g+dr))
            b = max(0, min(255, b+dr))
            row.append((r,g,b,255))
        pix.append(row)
    return pix

def pulsing_flesh(seed=0):
    random.seed(seed)
    pix = []
    for y in range(16):
        row = []
        for x in range(16):
            # dark red with blotches
            base_r = 130 + random.randint(-30, 40)
            base_g = 20 + random.randint(-10, 25)
            base_b = 25 + random.randint(-10, 30)
            # dark veins
            if (x + y) % 7 == 0 or (x*y) % 11 == 0:
                base_r = max(0, base_r - 60)
                base_g = max(0, base_g - 10)
                base_b = max(0, base_b - 10)
            row.append((base_r, base_g, base_b, 255))
        pix.append(row)
    return pix

# Chalk symbol (semi-transparent white mark on alpha)
def chalk_symbol():
    pix = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    # circle + cross
    for y in range(16):
        for x in range(16):
            dx, dy = x-7.5, y-7.5
            d = (dx*dx + dy*dy) ** 0.5
            if 5.0 < d < 6.5:
                pix[y][x] = (230,230,230,230)
            if abs(dx) < 0.8 and d < 7:
                pix[y][x] = (245,245,245,240)
            if abs(dy) < 0.8 and d < 7:
                pix[y][x] = (245,245,245,240)
    return pix

def chalk_item():
    pix = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    # diagonal white stick
    for i in range(16):
        x, y = i, 15-i
        for dx in (-1,0,1):
            nx = x+dx
            if 0 <= nx < 16:
                pix[y][nx] = (245,245,245,255)
    return pix

def blood_altar_top():
    pix = noisy((80, 10, 15), 15, seed=3)
    # central red runic mark
    for y in range(16):
        for x in range(16):
            dx, dy = x-7.5, y-7.5
            d = (dx*dx+dy*dy)**0.5
            if d < 2.5:
                pix[y][x] = (200, 20, 20, 255)
            elif 3.0 < d < 4.0:
                pix[y][x] = (140, 0, 0, 255)
    return pix

def blood_altar_side():
    pix = noisy((60, 10, 14), 12, seed=4)
    # blood drip
    for x in range(2, 15, 3):
        drip_len = random.randint(3, 10)
        for dy in range(drip_len):
            y = dy + 2
            if 0 <= y < 16:
                pix[y][x] = (170, 15, 15, 255)
    return pix

def blood_cure_pill():
    pix = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x-7.5, y-7.5
            d = (dx*dx+dy*dy)**0.5
            if d < 5:
                if x < 8:
                    pix[y][x] = (220,230,220,255)
                else:
                    pix[y][x] = (180, 25, 25, 255)
            if d < 1.5:
                pix[y][x] = (255, 255, 255, 255)
    return pix

def solid_armor(col, piece):
    pix = noisy(col, 10, seed=hash(piece) % 100)
    return pix

def order_armor(piece):
    # white/gold with cross
    pix = noisy((230, 225, 210), 12, seed=hash(piece) % 100)
    # gold trim
    for y in range(16):
        for x in range(16):
            if x == 0 or x == 15 or y == 0 or y == 15:
                pix[y][x] = (210, 170, 60, 255)
    return pix

def blood_armor(piece):
    pix = pulsing_flesh(seed=hash(piece) % 100)
    # dark bone spikes
    for x in [2,8,13]:
        for y in range(16):
            if y < 4:
                pix[y][x] = (200, 200, 180, 255)
    return pix

def blood_scythe():
    pix = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    # wooden handle diagonal
    for i in range(16):
        x, y = i, 15-i
        for dx in (-1,0,1):
            nx = x+dx
            if 0 <= nx < 16:
                pix[y][nx] = (60, 30, 15, 255)
    # red curved blade top
    for y in range(0, 8):
        for x in range(2, 12):
            if (x-2) + (y) < 10 and y < 5 + (x-2)//2:
                pix[y][x] = (180, 20, 20, 255)
    return pix

def holy_staff():
    pix = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        x, y = i, 15-i
        for dx in (-1,0,1):
            nx = x+dx
            if 0 <= nx < 16:
                pix[y][nx] = (220, 200, 140, 255)
    # glowing orb top
    for y in range(16):
        for x in range(16):
            dx, dy = x-2, y-2
            d = (dx*dx+dy*dy)**0.5
            if d < 2.5:
                pix[y][x] = (255, 255, 200, 255)
    return pix

def sanctify_orb():
    pix = [[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x-7.5, y-7.5
            d = (dx*dx+dy*dy)**0.5
            if d < 6:
                t = 1.0 - d/6
                pix[y][x] = (int(255*t + 200*(1-t)), int(250*t + 200*(1-t)), int(180*t + 150*(1-t)), 255)
    return pix

# Write all textures
textures = {
    # blocks
    os.path.join(BASE, "block", "chalk_symbol.png"): chalk_symbol(),
    os.path.join(BASE, "block", "blood_altar_top.png"): blood_altar_top(),
    os.path.join(BASE, "block", "blood_altar_side.png"): blood_altar_side(),
    os.path.join(BASE, "block", "blood_altar_bottom.png"): noisy((40, 5, 10), 10, seed=7),
    os.path.join(BASE, "block", "living_flesh.png"): pulsing_flesh(seed=1),
    os.path.join(BASE, "block", "flesh_mother.png"): pulsing_flesh(seed=2),
    os.path.join(BASE, "block", "attacking_flesh.png"): pulsing_flesh(seed=3),
    # items
    os.path.join(BASE, "item", "chalk.png"): chalk_item(),
    os.path.join(BASE, "item", "chalk_symbol.png"): chalk_symbol(),
    os.path.join(BASE, "item", "blood_altar.png"): blood_altar_top(),
    os.path.join(BASE, "item", "living_flesh.png"): pulsing_flesh(seed=1),
    os.path.join(BASE, "item", "flesh_mother.png"): pulsing_flesh(seed=2),
    os.path.join(BASE, "item", "attacking_flesh.png"): pulsing_flesh(seed=3),
    os.path.join(BASE, "item", "blood_cure_pill.png"): blood_cure_pill(),
    # armor
    os.path.join(BASE, "item", "blood_helmet.png"): blood_armor("h"),
    os.path.join(BASE, "item", "blood_chestplate.png"): blood_armor("c"),
    os.path.join(BASE, "item", "blood_leggings.png"): blood_armor("l"),
    os.path.join(BASE, "item", "blood_boots.png"): blood_armor("b"),
    os.path.join(BASE, "item", "order_helmet.png"): order_armor("h"),
    os.path.join(BASE, "item", "order_chestplate.png"): order_armor("c"),
    os.path.join(BASE, "item", "order_leggings.png"): order_armor("l"),
    os.path.join(BASE, "item", "order_boots.png"): order_armor("b"),
    # weapons
    os.path.join(BASE, "item", "blood_scythe.png"): blood_scythe(),
    os.path.join(BASE, "item", "holy_smite_staff.png"): holy_staff(),
    os.path.join(BASE, "item", "sanctify_orb.png"): sanctify_orb(),
}

for path, pix in textures.items():
    png_write(path, pix)
    print("wrote", path)

print(f"\nTotal: {len(textures)} textures.")
