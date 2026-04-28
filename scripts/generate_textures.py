# Liberthia Mod - Texture Generator
# Generates all missing textures for blocks, items, entities, and fluids.
from PIL import Image, ImageDraw
import os, random, math

random.seed(42)
BASE = os.path.dirname(os.path.abspath(__file__))
BLOCK_DIR = os.path.join(BASE, "src", "main", "resources", "assets", "liberthia", "textures", "block")
ITEM_DIR = os.path.join(BASE, "src", "main", "resources", "assets", "liberthia", "textures", "item")
ENTITY_DIR = os.path.join(BASE, "src", "main", "resources", "assets", "liberthia", "textures", "entity")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)
os.makedirs(ENTITY_DIR, exist_ok=True)

def nc(base, v=15):
    r, g, b = base
    return (max(0, min(255, r + random.randint(-v, v))),
            max(0, min(255, g + random.randint(-v, v))),
            max(0, min(255, b + random.randint(-v, v))))

def fill_noisy(img, base, v=12):
    for x in range(img.width):
        for y in range(img.height):
            img.putpixel((x, y), nc(base, v))

def draw_veins(img, color, count=5):
    for _ in range(count):
        x, y = random.randint(0, img.width - 1), random.randint(0, img.height - 1)
        for _ in range(random.randint(4, 10)):
            nx = max(0, min(img.width - 1, x + random.choice([-1, 0, 1])))
            ny = max(0, min(img.height - 1, y + random.choice([-1, 0, 1])))
            img.putpixel((nx, ny), nc(color, 8))
            x, y = nx, ny

def draw_cracks(img, color, count=3):
    for _ in range(count):
        x = random.randint(2, img.width - 3)
        y = random.randint(0, 3)
        for _ in range(random.randint(6, 14)):
            if 0 <= x < img.width and 0 <= y < img.height:
                img.putpixel((x, y), nc(color, 5))
            x += random.choice([-1, 0, 0, 1])
            y += 1
            x = max(0, min(img.width - 1, x))

def draw_border(img, color):
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, img.width - 1, img.height - 1], outline=color)

def fill_region(img, x0, y0, w, h, color):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            if 0 <= x < img.width and 0 <= y < img.height:
                img.putpixel((x, y), nc(color, 10) + (255,))

# === BLOCK TEXTURES (16x16) ===

def gen_infection_heart():
    img = Image.new("RGB", (16, 16))
    fill_noisy(img, (61, 15, 92), 10)
    draw_veins(img, (139, 0, 0), 8)
    cx, cy = 8, 8
    for x in range(16):
        for y in range(16):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d < 4:
                f = 1.0 - d / 4
                b = img.getpixel((x, y))
                glow = (255, 0, 255)
                img.putpixel((x, y), tuple(min(255, int(b[i] * (1 - f * 0.7) + glow[i] * f * 0.7)) for i in range(3)))
    draw_border(img, (30, 5, 45))
    img.save(os.path.join(BLOCK_DIR, "infection_heart.png"))
    print("  [block] infection_heart.png")

def gen_quarantine_ward():
    img = Image.new("RGB", (16, 16))
    fill_noisy(img, (160, 210, 219), 8)
    for i in range(0, 16, 4):
        for y in range(16):
            img.putpixel((i, y), nc((192, 192, 192), 10))
        for x in range(16):
            img.putpixel((x, i), nc((192, 192, 192), 10))
    for _ in range(6):
        x, y = random.randint(1, 14), random.randint(1, 14)
        img.putpixel((x, y), nc((230, 245, 255), 5))
    draw_border(img, (120, 170, 180))
    img.save(os.path.join(BLOCK_DIR, "quarantine_ward.png"))
    print("  [block] quarantine_ward.png")

def gen_scarred_earth():
    img = Image.new("RGB", (16, 16))
    fill_noisy(img, (107, 66, 38), 12)
    draw_veins(img, (92, 58, 110), 4)
    draw_cracks(img, (74, 55, 40), 4)
    for _ in range(8):
        x, y = random.randint(0, 15), random.randint(0, 15)
        img.putpixel((x, y), nc((130, 90, 55), 10))
    img.save(os.path.join(BLOCK_DIR, "scarred_earth.png"))
    print("  [block] scarred_earth.png")

def gen_scarred_stone():
    img = Image.new("RGB", (16, 16))
    fill_noisy(img, (128, 128, 128), 10)
    draw_veins(img, (107, 76, 125), 3)
    draw_cracks(img, (90, 90, 90), 5)
    for _ in range(10):
        x, y = random.randint(0, 15), random.randint(0, 15)
        img.putpixel((x, y), nc((100, 100, 100), 8))
    img.save(os.path.join(BLOCK_DIR, "scarred_stone.png"))
    print("  [block] scarred_stone.png")

def gen_unstable_matter():
    img = Image.new("RGB", (16, 16))
    cx, cy = 8, 8
    for x in range(16):
        for y in range(16):
            angle = math.atan2(y - cy, x - cx)
            dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            swirl = math.sin(angle * 2 + dist * 0.5)
            if swirl > 0:
                img.putpixel((x, y), nc((26, 10, 46), 8))
            else:
                img.putpixel((x, y), nc((224, 224, 255), 8))
    for _ in range(5):
        sx, sy = random.randint(2, 13), random.randint(2, 13)
        img.putpixel((sx, sy), nc((255, 102, 0), 15))
    draw_border(img, (50, 20, 70))
    img.save(os.path.join(BLOCK_DIR, "unstable_matter.png"))
    print("  [block] unstable_matter.png")

# === ITEM TEXTURES (16x16) ===

def gen_field_journal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 1, 12, 14], fill=(139, 69, 19))
    d.rectangle([5, 2, 12, 13], fill=(245, 222, 179))
    d.line([(4, 1), (4, 14)], fill=(100, 50, 10))
    for y in [4, 6, 8, 10, 12]:
        d.line([(6, y), (11, y)], fill=(150, 130, 100))
    d.line([(7, 1), (7, 0)], fill=(180, 30, 30))
    img.save(os.path.join(ITEM_DIR, "field_journal.png"))
    print("  [item] field_journal.png")

def gen_eye_of_horus_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 4, 13, 11], fill=(255, 215, 0), outline=(200, 160, 0))
    d.ellipse([5, 5, 10, 10], fill=(106, 13, 173))
    d.ellipse([6, 6, 9, 9], fill=(20, 0, 40))
    img.putpixel((7, 6), (255, 255, 255, 255))
    d.line([(1, 7), (3, 7)], fill=(255, 215, 0))
    d.line([(12, 7), (14, 7)], fill=(255, 215, 0))
    d.line([(1, 8), (2, 10)], fill=(200, 160, 0))
    d.line([(14, 8), (13, 10)], fill=(200, 160, 0))
    img.save(os.path.join(ITEM_DIR, "eye_of_horus.png"))
    print("  [item] eye_of_horus.png")

def gen_equilibrium_fragment():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (11, 5), (10, 12), (8, 14), (6, 12), (5, 5)], fill=(255, 215, 0), outline=(200, 170, 0))
    d.polygon([(8, 3), (9, 6), (9, 10), (8, 12), (7, 10), (7, 6)], fill=(255, 255, 230))
    for p in [(8, 2), (10, 5), (6, 5), (8, 13)]:
        img.putpixel(p, (255, 255, 255, 255))
    for x in range(16):
        for y in range(16):
            if img.getpixel((x, y))[3] == 0:
                dist = math.sqrt((x - 8) ** 2 + (y - 7) ** 2)
                if dist < 6:
                    img.putpixel((x, y), (255, 230, 100, int(60 * (1 - dist / 6))))
    img.save(os.path.join(ITEM_DIR, "equilibrium_fragment.png"))
    print("  [item] equilibrium_fragment.png")

def gen_expedition_tracker():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 2, 12, 13], fill=(112, 128, 144), outline=(80, 90, 100))
    d.rectangle([5, 3, 11, 9], fill=(10, 30, 10))
    d.rectangle([6, 4, 10, 8], fill=(0, 60, 0))
    img.putpixel((8, 6), (0, 255, 0, 255))
    img.putpixel((7, 5), (0, 180, 0, 255))
    d.line([(6, 6), (10, 6)], fill=(0, 100, 0))
    d.rectangle([5, 10, 7, 12], fill=(80, 80, 80))
    d.rectangle([9, 10, 11, 12], fill=(80, 80, 80))
    d.line([(8, 2), (8, 0)], fill=(150, 150, 150))
    img.putpixel((8, 0), (200, 200, 200, 255))
    img.save(os.path.join(ITEM_DIR, "expedition_tracker.png"))
    print("  [item] expedition_tracker.png")

def gen_matter_ampoule():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([5, 3, 10, 13], fill=(192, 232, 255, 140), outline=(160, 200, 230, 200))
    d.rectangle([6, 7, 9, 12], fill=(139, 0, 255, 220))
    d.rectangle([6, 6, 9, 7], fill=(160, 50, 255, 180))
    d.rectangle([6, 2, 9, 4], fill=(180, 180, 190))
    d.rectangle([7, 1, 8, 2], fill=(200, 200, 210))
    d.line([(6, 4), (6, 11)], fill=(220, 240, 255, 100))
    img.putpixel((8, 9), (180, 100, 255, 200))
    img.save(os.path.join(ITEM_DIR, "matter_ampoule.png"))
    print("  [item] matter_ampoule.png")

# === ENTITY TEXTURES ===

def gen_corrupted_zombie():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    sc = (80, 30, 100)
    vc = (30, 10, 40)
    ec = (200, 0, 255)
    for r in [(0, 0, 32, 16), (16, 16, 24, 16), (0, 16, 16, 16), (16, 48, 16, 16), (40, 16, 16, 16), (32, 48, 16, 16)]:
        fill_region(img, r[0], r[1], r[2], r[3], sc)
    for ex in range(10, 12):
        img.putpixel((ex, 11), ec + (255,))
    for ex in range(13, 15):
        img.putpixel((ex, 11), ec + (255,))
    for mx in range(10, 14):
        img.putpixel((mx, 13), vc + (255,))
    for _ in range(20):
        sx, sy = random.randint(0, 63), random.randint(0, 63)
        if img.getpixel((sx, sy))[3] > 0:
            for _ in range(random.randint(3, 8)):
                nx = max(0, min(63, sx + random.choice([-1, 0, 1])))
                ny = max(0, min(63, sy + random.choice([-1, 0, 1])))
                if img.getpixel((nx, ny))[3] > 0:
                    img.putpixel((nx, ny), nc(vc, 5) + (255,))
                    sx, sy = nx, ny
    img.save(os.path.join(ENTITY_DIR, "corrupted_zombie.png"))
    print("  [entity] corrupted_zombie.png")

def gen_spore_spitter():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    bc = (40, 100, 30)
    sp = (100, 40, 130)
    ec = (255, 0, 0)
    for r in [(32, 4, 8, 8), (32, 0, 8, 4), (40, 0, 8, 4), (0, 0, 12, 12), (0, 12, 24, 20), (18, 0, 14, 12)]:
        fill_region(img, r[0], r[1], r[2], r[3], bc)
    for _ in range(15):
        x, y = random.randint(0, 63), random.randint(0, 31)
        if img.getpixel((x, y))[3] > 0:
            img.putpixel((x, y), nc(sp, 10) + (255,))
    for ex in [34, 36, 38]:
        img.putpixel((ex, 7), ec + (255,))
    img.save(os.path.join(ENTITY_DIR, "spore_spitter.png"))
    print("  [entity] spore_spitter.png")

def gen_dark_consciousness():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    sc = (15, 5, 25)
    ec = (180, 0, 255)
    gc = (60, 20, 80)
    for r in [(0, 0, 32, 16), (16, 16, 24, 16), (0, 16, 16, 16), (40, 16, 16, 16)]:
        fill_region(img, r[0], r[1], r[2], r[3], sc)
    for ex in range(10, 12):
        img.putpixel((ex, 11), ec + (255,))
    for ex in range(13, 15):
        img.putpixel((ex, 11), ec + (255,))
    for _ in range(12):
        sx, sy = random.randint(0, 63), random.randint(0, 31)
        if img.getpixel((sx, sy))[3] > 0:
            for _ in range(random.randint(4, 10)):
                nx = max(0, min(63, sx + random.choice([-1, 0, 1])))
                ny = max(0, min(31, sy + random.choice([-1, 0, 1])))
                if img.getpixel((nx, ny))[3] > 0:
                    img.putpixel((nx, ny), nc(gc, 8) + (255,))
                    sx, sy = nx, ny
    img.save(os.path.join(ENTITY_DIR, "dark_consciousness.png"))
    print("  [entity] dark_consciousness.png")

def gen_eye_of_horus_entity():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([12, 20, 51, 44], fill=(40, 10, 60, 200))
    cx, cy = 32, 32
    for x in range(64):
        for y in range(64):
            dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if dist < 12:
                f = dist / 12
                r = int(106 * (1 - f) + 40 * f)
                g = int(13 * (1 - f) + 10 * f)
                b = int(173 * (1 - f) + 60 * f)
                img.putpixel((x, y), (r, g, b, 230))
    d.ellipse([27, 27, 37, 37], fill=(10, 0, 20, 255))
    d.ellipse([29, 28, 32, 31], fill=(255, 255, 255, 200))
    d.ellipse([10, 18, 53, 46], outline=(255, 200, 0, 255), width=2)
    d.line([(8, 32), (2, 32)], fill=(255, 215, 0, 200), width=2)
    d.line([(55, 32), (61, 32)], fill=(255, 215, 0, 200), width=2)
    d.line([(4, 34), (8, 42)], fill=(200, 160, 0, 180), width=2)
    d.line([(59, 34), (55, 42)], fill=(200, 160, 0, 180), width=2)
    for x in range(64):
        for y in range(64):
            if img.getpixel((x, y))[3] == 0:
                dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
                if dist < 28:
                    img.putpixel((x, y), (120, 40, 180, int(40 * (1 - dist / 28))))
    img.save(os.path.join(ENTITY_DIR, "eye_of_horus.png"))
    print("  [entity] eye_of_horus.png")

# === FLUID TEXTURES ===

def gen_fluid_still(fn, base, alpha=180):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            w = math.sin(x * 0.8 + y * 0.5) * 10
            c = nc(base, 8)
            a = max(100, min(220, alpha + int(w)))
            img.putpixel((x, y), c + (a,))
    img.save(os.path.join(BLOCK_DIR, fn))
    print(f"  [fluid] {fn}")

def gen_fluid_flow(fn, base, alpha=160):
    img = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    for fr in range(2):
        yo = fr * 16
        for x in range(16):
            for y in range(16):
                w = math.sin(x * 0.6 + (y + fr * 4) * 0.8) * 12
                c = nc(base, 10)
                a = max(80, min(200, alpha + int(w)))
                img.putpixel((x, y + yo), c + (a,))
    img.save(os.path.join(BLOCK_DIR, fn))
    print(f"  [fluid] {fn}")

# === MAIN ===
if __name__ == "__main__":
    print("=" * 50)
    print("Liberthia Mod - Texture Generator")
    print("=" * 50)
    print("\n[Fase 1] Blocos...")
    gen_infection_heart()
    gen_quarantine_ward()
    gen_scarred_earth()
    gen_scarred_stone()
    gen_unstable_matter()
    print("\n[Fase 2] Itens...")
    gen_field_journal()
    gen_eye_of_horus_item()
    gen_equilibrium_fragment()
    gen_expedition_tracker()
    gen_matter_ampoule()
    print("\n[Fase 3] Entidades...")
    gen_corrupted_zombie()
    gen_spore_spitter()
    gen_dark_consciousness()
    gen_eye_of_horus_entity()
    print("\n[Fase 4] Fluidos...")
    gen_fluid_still("clear_matter_still.png", (200, 240, 255), 150)
    gen_fluid_flow("clear_matter_flow.png", (180, 230, 255), 130)
    gen_fluid_still("yellow_matter_still.png", (255, 200, 50), 160)
    gen_fluid_flow("yellow_matter_flow.png", (255, 190, 40), 140)
    print("\n" + "=" * 50)
    print("Concluido! Todas as texturas geradas.")
    print("=" * 50)
