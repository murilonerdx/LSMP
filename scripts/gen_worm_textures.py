import os, struct, zlib, random
BASE = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\entity"
os.makedirs(BASE, exist_ok=True)
def png(path, pix, w=32, h=16):
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t+d) & 0xffffffff)
    raw = b""
    for y in range(h):
        raw += b"\x00"
        for x in range(w):
            raw += bytes(pix[y][x])
    data = zlib.compress(raw)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", data))
        f.write(chunk(b"IEND", b""))

def worm_tex(base_col, accent, seed):
    random.seed(seed)
    pix = [[(0,0,0,0) for _ in range(32)] for _ in range(16)]
    # fill rectangles matching model UV: head 0,0 5x4x4; seg 0,8 4x3.5x4; tail 16,8 2x2x4
    for y in range(16):
        for x in range(32):
            # default: body pattern, noisy red
            r,g,b = base_col
            dr = random.randint(-25, 25)
            r = max(0, min(255, r+dr))
            g = max(0, min(255, g+dr//2))
            b = max(0, min(255, b+dr//2))
            # vein lines
            if (x*3 + y*2) % 11 == 0:
                r = max(0, r-40); g=max(0,g-15); b=max(0,b-15)
            # specular speckle
            if random.random() < 0.04:
                r,g,b = accent
            pix[y][x] = (r,g,b,255)
    return pix

png(os.path.join(BASE, "blood_worm.png"), worm_tex((150, 25, 30), (220, 80, 80), 1))
png(os.path.join(BASE, "flesh_crawler.png"), worm_tex((120, 35, 45), (200, 100, 120), 2))
png(os.path.join(BASE, "gore_worm.png"), worm_tex((90, 15, 20), (180, 40, 40), 3))
print("done")
