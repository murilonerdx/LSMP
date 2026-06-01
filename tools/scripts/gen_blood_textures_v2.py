"""Pretty blood-themed textures using procedural gradients, veins, blisters."""
import os, math, random
from PIL import Image, ImageDraw, ImageFilter

BASE = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures"
os.makedirs(os.path.join(BASE, "block"), exist_ok=True)
os.makedirs(os.path.join(BASE, "item"), exist_ok=True)

def clamp(v, lo=0, hi=255): return max(lo, min(hi, int(v)))

def fbm_noise(w, h, octaves=4, seed=0):
    """Simple value-noise fBm."""
    rng = random.Random(seed)
    # start from coarse grid, bilinearly upsample, sum
    field = [[0.0]*w for _ in range(h)]
    amp = 1.0
    total_amp = 0.0
    for o in range(octaves):
        freq = 2 ** (o + 1)
        gw, gh = max(2, freq), max(2, freq)
        grid = [[rng.random() for _ in range(gw)] for _ in range(gh)]
        for y in range(h):
            gy = y / h * (gh - 1)
            y0 = int(gy); y1 = min(gh-1, y0+1); ty = gy - y0
            for x in range(w):
                gx = x / w * (gw - 1)
                x0 = int(gx); x1 = min(gw-1, x0+1); tx = gx - x0
                v00 = grid[y0][x0]; v10 = grid[y0][x1]
                v01 = grid[y1][x0]; v11 = grid[y1][x1]
                vx0 = v00*(1-tx) + v10*tx
                vx1 = v01*(1-tx) + v11*tx
                v = vx0*(1-ty) + vx1*ty
                field[y][x] += v * amp
        total_amp += amp
        amp *= 0.55
    for y in range(h):
        for x in range(w):
            field[y][x] /= total_amp
    return field

def fleshy(base, seed, veins=True, blisters=True, bright=False):
    W = H = 16
    noise = fbm_noise(W, H, 5, seed)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(W):
            n = noise[y][x]
            # Remap noise into dark red -> bright red
            t = (n - 0.35) * 1.6
            t = max(0, min(1, t))
            r = clamp(base[0] + t * (base[0] * 0.9 + 60))
            g = clamp(base[1] + t * 35)
            b = clamp(base[2] + t * 35)
            # darker pits
            if n < 0.35:
                r = clamp(r - 40); g = clamp(g - 15); b = clamp(b - 10)
            px[x, y] = (r, g, b, 255)
    # Overlay veins: random irregular curves
    if veins:
        rng = random.Random(seed + 17)
        d = ImageDraw.Draw(img)
        for _ in range(3 + rng.randint(0, 3)):
            # start point
            x, y = rng.randint(0, W-1), rng.randint(0, H-1)
            ang = rng.random() * math.tau
            length = rng.randint(6, 14)
            for step in range(length):
                ang += (rng.random() - 0.5) * 0.8
                nx = int(x + math.cos(ang))
                ny = int(y + math.sin(ang))
                if 0 <= nx < W and 0 <= ny < H:
                    pr, pg, pb, pa = px[nx, ny]
                    px[nx, ny] = (clamp(pr - 55), clamp(pg - 15), clamp(pb - 10), 255)
                x, y = nx % W, ny % H
    # Blisters: bright highlights with dark rim
    if blisters:
        rng = random.Random(seed + 53)
        for _ in range(rng.randint(2, 4)):
            cx, cy = rng.randint(2, W-3), rng.randint(2, H-3)
            rad = rng.choice([1, 1, 2])
            for y in range(max(0,cy-rad-1), min(H, cy+rad+2)):
                for x in range(max(0,cx-rad-1), min(W, cx+rad+2)):
                    dx, dy = x-cx, y-cy
                    dist = math.hypot(dx, dy)
                    if dist <= rad - 0.2:
                        # bright highlight
                        r, g, b, a = px[x, y]
                        px[x, y] = (clamp(r + 60), clamp(g + 30), clamp(b + 30), 255)
                    elif dist <= rad + 0.6:
                        # dark rim
                        r, g, b, a = px[x, y]
                        px[x, y] = (clamp(r - 35), clamp(g - 10), clamp(b - 10), 255)
    if bright:
        for y in range(H):
            for x in range(W):
                r, g, b, a = px[x, y]
                px[x, y] = (clamp(r+20), clamp(g+8), clamp(b+8), 255)
    return img

def blood_altar_top():
    W = H = 16
    noise = fbm_noise(W, H, 4, 1001)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    # dark stone base with blood pool
    for y in range(H):
        for x in range(W):
            dx, dy = x-7.5, y-7.5
            d = math.hypot(dx, dy)
            n = noise[y][x]
            # outer stone
            if d > 6.5:
                base = (55 + int(n*30), 50 + int(n*20), 50 + int(n*20))
            elif d > 5:
                # rim carving
                base = (30, 25, 25)
            elif d > 2:
                # blood pool - gradient
                t = 1 - (d - 2) / 3
                r = clamp(120 + t*80 + n*30)
                g = clamp(15 + t*10)
                b = clamp(20 + t*12)
                base = (r, g, b)
            else:
                # center glyph: dark symbol
                if abs(dx) < 0.6 or abs(dy) < 0.6 or abs(abs(dx) - abs(dy)) < 0.6:
                    base = (40, 5, 5)
                else:
                    r = clamp(180 + n*40)
                    base = (r, clamp(20 + n*10), clamp(25 + n*10))
            px[x, y] = (base[0], base[1], base[2], 255)
    return img

def blood_altar_side():
    W = H = 16
    noise = fbm_noise(W, H, 4, 1002)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(W):
            n = noise[y][x]
            # dark stone
            r = clamp(55 + n*35)
            g = clamp(50 + n*25)
            b = clamp(52 + n*25)
            # brick lines
            if y % 5 == 0 or (y % 5 == 2 and ((y // 5) % 2 == 0 and x % 8 == 4 or (y // 5) % 2 == 1 and x % 8 == 0)):
                r = clamp(r - 25); g = clamp(g - 20); b = clamp(b - 20)
            px[x, y] = (r, g, b, 255)
    # blood drips
    d = ImageDraw.Draw(img)
    rng = random.Random(2002)
    for _ in range(3):
        sx = rng.randint(2, 13)
        # drip head
        d.ellipse((sx-1, 1, sx+1, 3), fill=(180, 20, 20, 255))
        length = rng.randint(4, 11)
        for y in range(2, 2 + length):
            if 0 <= y < H:
                col = (clamp(170 - y*5), clamp(20 - y), clamp(20 - y), 255)
                px[sx, y] = col
                if rng.random() < 0.3 and sx + 1 < W:
                    px[sx + 1, y] = col
    return img

def blood_altar_bottom():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    noise = fbm_noise(16, 16, 3, 999)
    for y in range(16):
        for x in range(16):
            n = noise[y][x]
            r = clamp(35 + n*25)
            px[x, y] = (r, r-5, r-3, 255)
    return img

def chalk_symbol():
    W = H = 16
    img = Image.new("RGBA", (W, H), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # outer circle
    d.ellipse((1, 1, 14, 14), outline=(240, 240, 240, 230), width=1)
    # inner pentagram-ish cross
    d.line((8, 2, 8, 13), fill=(245, 245, 245, 240), width=1)
    d.line((2, 8, 13, 8), fill=(245, 245, 245, 240), width=1)
    d.line((3, 3, 12, 12), fill=(220, 220, 220, 200), width=1)
    d.line((12, 3, 3, 12), fill=(220, 220, 220, 200), width=1)
    # small dots
    for pt in [(4,4),(11,4),(4,11),(11,11)]:
        d.point(pt, fill=(250, 250, 250, 230))
    return img

def chalk_item():
    img = Image.new("RGBA", (16, 16), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # chalk stick diagonal with shading
    for i in range(13):
        x = 1 + i; y = 14 - i
        d.point((x, y), fill=(255, 255, 255, 255))
        d.point((x-1, y), fill=(230, 230, 230, 255))
        d.point((x, y+1), fill=(200, 200, 200, 255))
    # tip chalk dust
    d.point((14, 1), fill=(255, 255, 255, 255))
    d.point((13, 1), fill=(240, 240, 240, 255))
    d.point((14, 2), fill=(240, 240, 240, 255))
    return img

def blood_cure_pill():
    img = Image.new("RGBA", (16, 16), (0,0,0,0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x-7.5, y-7.5
            d = math.hypot(dx, dy)
            if d < 5:
                # half red half white
                if x < 8:
                    shade = 1 - d/5 * 0.4
                    px[x,y] = (clamp(220*shade), clamp(225*shade), clamp(215*shade), 255)
                else:
                    shade = 1 - d/5 * 0.3
                    px[x,y] = (clamp(195*shade + 30), clamp(25*shade), clamp(30*shade), 255)
            if d < 1.5:
                px[x,y] = (255, 245, 245, 255)  # gloss
    # seam line
    d = ImageDraw.Draw(img)
    d.line((8, 3, 8, 12), fill=(50, 10, 10, 255), width=1)
    return img

def armor_piece(base_col, accent, seed, holy=False):
    W = H = 16
    noise = fbm_noise(W, H, 4, seed)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(W):
            n = noise[y][x]
            r = clamp(base_col[0] + n*40)
            g = clamp(base_col[1] + n*30)
            b = clamp(base_col[2] + n*30)
            px[x, y] = (r, g, b, 255)
    d = ImageDraw.Draw(img)
    # trim
    d.rectangle((0, 0, 15, 15), outline=accent, width=1)
    if holy:
        # cross on chest
        d.line((7, 4, 7, 11), fill=accent, width=1)
        d.line((8, 4, 8, 11), fill=accent, width=1)
        d.line((5, 6, 10, 6), fill=accent, width=1)
        d.line((5, 7, 10, 7), fill=accent, width=1)
    else:
        # spike/vein
        d.line((3, 3, 3, 12), fill=(200, 200, 180, 255), width=1)
        d.line((12, 3, 12, 12), fill=(200, 200, 180, 255), width=1)
    return img

def weapon_scythe():
    img = Image.new("RGBA", (16, 16), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # handle (dark wood, diagonal)
    for i in range(14):
        x, y = 1 + i, 14 - i
        d.point((x, y), fill=(70, 35, 20, 255))
        d.point((x+1, y), fill=(90, 45, 25, 255))
        d.point((x, y-1), fill=(60, 30, 15, 255))
    # blade (red crescent top-right)
    pts = [(8,0),(9,0),(10,1),(11,1),(12,2),(13,3),(13,4),(12,4),(11,3),(10,2),(9,2),(9,3),(10,4),(11,5),(12,6)]
    for p in pts:
        d.point(p, fill=(200, 25, 25, 255))
    # blade shine
    d.point((11, 2), fill=(255, 180, 180, 255))
    d.point((12, 3), fill=(230, 120, 120, 255))
    # blade inner dark
    for p in [(9,1),(10,3),(11,4)]:
        d.point(p, fill=(110, 10, 10, 255))
    return img

def weapon_staff():
    img = Image.new("RGBA", (16, 16), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # wooden shaft
    for i in range(12):
        x, y = 3 + i, 14 - i
        d.point((x, y), fill=(210, 185, 130, 255))
        d.point((x-1, y), fill=(170, 150, 100, 255))
        d.point((x, y+1), fill=(140, 120, 80, 255))
    # gold wrap
    for y in [6, 10]:
        for dx in range(3):
            d.point((11-y+dx, y), fill=(220, 170, 40, 255))
    # glowing orb at top
    for y in range(5):
        for x in range(5):
            dx, dy = x-2, y-2
            dist = math.hypot(dx, dy)
            if dist < 2.3:
                t = 1 - dist/2.3
                r = clamp(255 - (1-t)*30)
                g = clamp(245 - (1-t)*20)
                b = clamp(180 + t*40)
                d.point((1+x, 1+y), fill=(r, g, b, 255))
    # center bright
    d.point((3, 3), fill=(255, 255, 240, 255))
    return img

def weapon_orb():
    img = Image.new("RGBA", (16, 16), (0,0,0,0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x-7.5, y-7.5
            dist = math.hypot(dx, dy)
            if dist < 6:
                t = 1 - dist/6
                # white-gold glow
                r = clamp(200 + t*55)
                g = clamp(200 + t*55)
                b = clamp(150 + t*80)
                px[x,y] = (r, g, b, 255)
            elif dist < 7:
                px[x,y] = (150, 120, 60, 200)
    # core shine
    for y in range(16):
        for x in range(16):
            dx, dy = x-5, y-5
            if math.hypot(dx, dy) < 2:
                px[x,y] = (255, 255, 240, 255)
    return img

def blood_infection_block():
    """Block that oozes infection — dark maroon with glowing yellow spots."""
    W = H = 16
    noise = fbm_noise(W, H, 5, 7001)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(W):
            n = noise[y][x]
            t = max(0, min(1, (n-0.3)*1.8))
            r = clamp(70 + t*80)
            g = clamp(10 + t*20)
            b = clamp(15 + t*25)
            # greenish-yellow pustules
            if n > 0.75:
                r = clamp(180 + n*40); g = clamp(180); b = clamp(60)
            px[x,y] = (r, g, b, 255)
    # dark cracks
    d = ImageDraw.Draw(img)
    rng = random.Random(7001)
    for _ in range(4):
        x, y = rng.randint(0,15), rng.randint(0,15)
        ang = rng.random()*math.tau
        for step in range(8):
            ang += (rng.random()-0.5)*1.0
            x = int(x + math.cos(ang)) % 16
            y = int(y + math.sin(ang)) % 16
            r,g,b,a = px[x,y]
            px[x,y] = (clamp(r-50), clamp(g-5), clamp(b-5), 255)
    return img

def blood_infestation_block():
    """Block teeming with visible worms — pink with writhing wormy shapes."""
    W = H = 16
    noise = fbm_noise(W, H, 4, 7002)
    img = Image.new("RGBA", (W, H))
    px = img.load()
    for y in range(H):
        for x in range(W):
            n = noise[y][x]
            r = clamp(120 + n*60)
            g = clamp(45 + n*40)
            b = clamp(55 + n*35)
            px[x,y] = (r, g, b, 255)
    # Worm squiggles (draw a few curves)
    d = ImageDraw.Draw(img)
    rng = random.Random(7002)
    for _ in range(6):
        x = rng.randint(1, 14); y = rng.randint(1, 14)
        ang = rng.random()*math.tau
        prev = (x, y)
        for step in range(rng.randint(5, 11)):
            ang += (rng.random()-0.5)*1.2
            nx = int(prev[0] + math.cos(ang))
            ny = int(prev[1] + math.sin(ang))
            nx %= 16; ny %= 16
            # worm body
            r,g,b,a = px[nx, ny]
            px[nx, ny] = (clamp(r+30), clamp(g-10), clamp(b-5), 255)
            # worm underside
            if ny+1 < 16:
                r2,g2,b2,a2 = px[nx, ny+1]
                px[nx, ny+1] = (clamp(r2-25), clamp(g2-10), clamp(b2-10), 255)
            prev = (nx, ny)
    return img

def blood_volcano_top():
    """Top of volcano — glowing magma-blood crater."""
    W = H = 16
    img = Image.new("RGBA", (W, H))
    px = img.load()
    noise = fbm_noise(W, H, 4, 8001)
    for y in range(H):
        for x in range(W):
            dx, dy = x-7.5, y-7.5
            dist = math.hypot(dx, dy)
            n = noise[y][x]
            if dist < 3:
                # inner glowing core
                t = 1 - dist/3
                r = clamp(230 + t*25)
                g = clamp(80 + t*40 + n*30)
                b = clamp(20 + t*10)
                px[x,y] = (r, g, b, 255)
            elif dist < 5:
                # blood pool
                t = 1 - (dist-3)/2
                r = clamp(160 + t*60 + n*20)
                g = clamp(20 + t*15)
                b = clamp(25 + t*15)
                px[x,y] = (r, g, b, 255)
            elif dist < 6.5:
                # rim
                r = clamp(60 + n*30)
                g = clamp(25 + n*15)
                b = clamp(25 + n*15)
                px[x,y] = (r, g, b, 255)
            else:
                # outer rock
                r = clamp(45 + n*35)
                px[x,y] = (r, clamp(30 + n*20), clamp(32 + n*20), 255)
    return img

def blood_volcano_side():
    """Side of volcano — rock with flowing lava-blood veins."""
    W = H = 16
    img = Image.new("RGBA", (W, H))
    px = img.load()
    noise = fbm_noise(W, H, 4, 8002)
    for y in range(H):
        for x in range(W):
            n = noise[y][x]
            r = clamp(50 + n*35)
            g = clamp(35 + n*25)
            b = clamp(35 + n*25)
            px[x,y] = (r, g, b, 255)
    # glowing blood veins flowing down
    d = ImageDraw.Draw(img)
    rng = random.Random(8002)
    for _ in range(4):
        sx = rng.randint(1, 14)
        x = sx
        for y in range(16):
            x += rng.randint(-1, 1)
            x = max(0, min(15, x))
            intensity = 1 - y/16 * 0.4
            r = clamp(200 * intensity + 40)
            g = clamp(50 * intensity + 10)
            b = clamp(40 * intensity + 10)
            px[x, y] = (r, g, b, 255)
            # glow edge
            if x+1 < 16:
                rr,gg,bb,_ = px[x+1, y]
                px[x+1, y] = (clamp(rr+30), clamp(gg+5), clamp(bb+3), 255)
    return img

# === Write all ===
out = {
    "block/living_flesh.png": fleshy((135, 20, 30), 101),
    "block/flesh_mother.png": fleshy((155, 25, 35), 102, bright=True),
    "block/attacking_flesh.png": fleshy((110, 15, 20), 103),
    "block/blood_altar_top.png": blood_altar_top(),
    "block/blood_altar_side.png": blood_altar_side(),
    "block/blood_altar_bottom.png": blood_altar_bottom(),
    "block/chalk_symbol.png": chalk_symbol(),
    "block/blood_infection_block.png": blood_infection_block(),
    "block/blood_infestation_block.png": blood_infestation_block(),
    "block/blood_volcano_top.png": blood_volcano_top(),
    "block/blood_volcano_side.png": blood_volcano_side(),
    "block/blood_volcano_bottom.png": blood_altar_bottom(),
    "item/living_flesh.png": fleshy((135, 20, 30), 101),
    "item/flesh_mother.png": fleshy((155, 25, 35), 102, bright=True),
    "item/attacking_flesh.png": fleshy((110, 15, 20), 103),
    "item/blood_altar.png": blood_altar_top(),
    "item/chalk.png": chalk_item(),
    "item/chalk_symbol.png": chalk_symbol(),
    "item/blood_cure_pill.png": blood_cure_pill(),
    "item/blood_helmet.png": armor_piece((110, 20, 25), (200, 200, 180, 255), 201),
    "item/blood_chestplate.png": armor_piece((110, 20, 25), (200, 200, 180, 255), 202),
    "item/blood_leggings.png": armor_piece((110, 20, 25), (200, 200, 180, 255), 203),
    "item/blood_boots.png": armor_piece((110, 20, 25), (200, 200, 180, 255), 204),
    "item/order_helmet.png": armor_piece((230, 225, 210), (220, 175, 50, 255), 301, holy=True),
    "item/order_chestplate.png": armor_piece((230, 225, 210), (220, 175, 50, 255), 302, holy=True),
    "item/order_leggings.png": armor_piece((230, 225, 210), (220, 175, 50, 255), 303, holy=True),
    "item/order_boots.png": armor_piece((230, 225, 210), (220, 175, 50, 255), 304, holy=True),
    "item/blood_scythe.png": weapon_scythe(),
    "item/holy_smite_staff.png": weapon_staff(),
    "item/sanctify_orb.png": weapon_orb(),
    "item/blood_infection_block.png": blood_infection_block(),
    "item/blood_infestation_block.png": blood_infestation_block(),
    "item/blood_volcano.png": blood_volcano_top(),
}
for rel, img in out.items():
    p = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
print(f"wrote {len(out)} textures")
