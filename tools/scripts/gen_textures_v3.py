"""
gen_textures_v3.py — Consolidated, quality texture generator for Liberthia.

Generates:
  - Armor layers (64x32 vanilla layout, Blood/Order re-colored on diamond silhouette)
  - Blood blocks with fBm organic noise + veins + blisters (animated where applicable)
  - Transparent chalk symbol (RGBA, no black bg)
  - Animated blood fluid still/flow + mcmeta
  - Blood spike block texture

Requires: Pillow. Run:  python gen_textures_v3.py
"""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
BLK = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/block")
ARMOR = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/models/armor")

os.makedirs(BLK, exist_ok=True)
os.makedirs(ARMOR, exist_ok=True)

# ----------------------------------------------------------------------
# Noise helpers
# ----------------------------------------------------------------------
def value_noise(w, h, scale, seed):
    rng = random.Random(seed)
    cw, ch = max(1, w // scale), max(1, h // scale)
    grid = [[rng.random() for _ in range(cw + 2)] for _ in range(ch + 2)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            fx, fy = x / scale, y / scale
            ix, iy = int(fx), int(fy)
            tx, ty = fx - ix, fy - iy
            # smoothstep
            sx = tx * tx * (3 - 2 * tx)
            sy = ty * ty * (3 - 2 * ty)
            a = grid[iy][ix]
            b = grid[iy][ix + 1]
            c = grid[iy + 1][ix]
            d = grid[iy + 1][ix + 1]
            top = a + (b - a) * sx
            bot = c + (d - c) * sx
            out[y][x] = top + (bot - top) * sy
    return out


def fbm(w, h, seed, octaves=4):
    acc = [[0.0] * w for _ in range(h)]
    amp = 1.0
    total_amp = 0.0
    scale = 8
    for o in range(octaves):
        n = value_noise(w, h, max(1, scale), seed + o * 1009)
        for y in range(h):
            for x in range(w):
                acc[y][x] += n[y][x] * amp
        total_amp += amp
        amp *= 0.5
        scale = max(1, scale // 2)
    for y in range(h):
        for x in range(w):
            acc[y][x] /= total_amp
    return acc


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# ----------------------------------------------------------------------
# Block textures
# ----------------------------------------------------------------------
def flesh_block(path, seed=1, palette=None, veins=True, blisters=True, highlight=(255, 180, 180)):
    """Organic flesh block — fBm noise between two reds + dark veins + pink blisters."""
    palette = palette or ((120, 15, 15), (180, 35, 35))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    noise = fbm(16, 16, seed, octaves=4)
    pxl = img.load()
    for y in range(16):
        for x in range(16):
            n = noise[y][x]
            n = max(0.0, min(1.0, (n - 0.3) / 0.5))
            c = lerp(palette[0], palette[1], n)
            pxl[x, y] = (*c, 255)

    draw = ImageDraw.Draw(img)
    rng = random.Random(seed + 77)
    if veins:
        for _ in range(3):
            x, y = rng.randint(0, 15), rng.randint(0, 15)
            for _ in range(rng.randint(4, 10)):
                nx = max(0, min(15, x + rng.randint(-2, 2)))
                ny = max(0, min(15, y + rng.randint(-2, 2)))
                draw.line((x, y, nx, ny), fill=(40, 5, 5, 255))
                x, y = nx, ny
    if blisters:
        for _ in range(rng.randint(2, 4)):
            cx, cy = rng.randint(2, 13), rng.randint(2, 13)
            r = rng.randint(1, 2)
            draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(220, 70, 70, 255))
            draw.point((cx, cy), fill=(*highlight, 255))

    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


def living_flesh(path, seed):
    flesh_block(path, seed=seed, palette=((100, 10, 10), (200, 40, 40)))


def attacking_flesh(path, seed):
    flesh_block(path, seed=seed, palette=((140, 15, 15), (220, 50, 20)),
                highlight=(255, 220, 100))


def flesh_mother(path, seed):
    flesh_block(path, seed=seed, palette=((80, 5, 30), (170, 25, 50)),
                highlight=(255, 150, 200))


def blood_infection(path, seed):
    # Dark grass-like with blood splatter
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    noise = fbm(16, 16, seed, octaves=3)
    pxl = img.load()
    for y in range(16):
        for x in range(16):
            n = max(0.0, min(1.0, (noise[y][x] - 0.25) / 0.5))
            c = lerp((50, 30, 30), (130, 25, 25), n)
            pxl[x, y] = (*c, 255)
    draw = ImageDraw.Draw(img)
    rng = random.Random(seed + 13)
    for _ in range(12):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        draw.point((x, y), fill=(180, 20, 20, 255))
    for _ in range(4):
        cx, cy = rng.randint(1, 14), rng.randint(1, 14)
        draw.ellipse((cx - 1, cy - 1, cx + 1, cy + 1), fill=(40, 0, 0, 255))
    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


def blood_infestation(path, seed):
    # Stone-like with red infestation veins
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    noise = fbm(16, 16, seed, octaves=4)
    pxl = img.load()
    for y in range(16):
        for x in range(16):
            n = max(0.0, min(1.0, (noise[y][x] - 0.3) / 0.4))
            c = lerp((70, 60, 60), (110, 90, 90), n)
            pxl[x, y] = (*c, 255)
    draw = ImageDraw.Draw(img)
    rng = random.Random(seed + 29)
    # Red cracks
    for _ in range(4):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        for _ in range(rng.randint(4, 9)):
            nx = max(0, min(15, x + rng.randint(-2, 2)))
            ny = max(0, min(15, y + rng.randint(-2, 2)))
            draw.line((x, y, nx, ny), fill=(180, 15, 15, 255))
            x, y = nx, ny
    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


def blood_altar_top(path, seed):
    # Ritual symbol on dark flesh
    flesh_block(path, seed=seed, palette=((60, 5, 5), (160, 20, 20)), blisters=False)
    img = Image.open(path).convert("RGBA")
    draw = ImageDraw.Draw(img)
    draw.ellipse((3, 3, 12, 12), outline=(30, 0, 0, 255), width=1)
    draw.line((8, 3, 8, 12), fill=(200, 50, 50, 255))
    draw.line((3, 8, 12, 8), fill=(200, 50, 50, 255))
    draw.point((8, 8), fill=(255, 220, 80, 255))
    img.save(path)


def blood_altar_side(path, seed):
    flesh_block(path, seed=seed, palette=((50, 5, 5), (140, 20, 20)), blisters=True)


def blood_altar_bottom(path, seed):
    flesh_block(path, seed=seed, palette=((30, 0, 0), (100, 15, 15)),
                veins=False, blisters=False)


def blood_volcano_top(path, seed):
    # Incandescent crater
    img = Image.new("RGBA", (16, 16))
    pxl = img.load()
    noise = fbm(16, 16, seed, octaves=4)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.sqrt(dx * dx + dy * dy) / 8.0
            d = min(1.0, d)
            glow = 1.0 - d
            n = noise[y][x]
            base = lerp((40, 0, 0), (120, 10, 10), n)
            hot = lerp(base, (255, 180, 30), glow * 0.8)
            pxl[x, y] = (*hot, 255)
    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


def blood_volcano_side(path, seed):
    flesh_block(path, seed=seed, palette=((40, 10, 5), (150, 30, 10)), blisters=False)


def blood_volcano_bottom(path, seed):
    flesh_block(path, seed=seed, palette=((20, 0, 0), (80, 5, 5)),
                veins=False, blisters=False)


def chalk_symbol(path):
    """Transparent chalk decal — RGBA, fully transparent background."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    white = (245, 245, 245, 220)
    # Pentagram-ish symbol
    draw.ellipse((1, 1, 14, 14), outline=white, width=1)
    # Five-point star
    cx, cy = 7.5, 7.5
    pts = []
    for i in range(5):
        ang = -math.pi / 2 + i * (2 * math.pi / 5)
        pts.append((cx + math.cos(ang) * 5.5, cy + math.sin(ang) * 5.5))
    # Connect every other vertex
    for i in range(5):
        a = pts[i]
        b = pts[(i + 2) % 5]
        draw.line((a, b), fill=white, width=1)
    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


def blood_spike(path, seed):
    """Spiky blood-stained dirt texture for the spike block."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pxl = img.load()
    noise = fbm(16, 16, seed, octaves=3)
    for y in range(16):
        for x in range(16):
            n = max(0.0, min(1.0, (noise[y][x] - 0.25) / 0.5))
            # Bottom: earthy; top: bloody
            t_y = y / 15.0
            ground = lerp((60, 30, 20), (100, 50, 30), n)
            bloody = lerp((150, 20, 20), (220, 40, 40), n)
            c = lerp(ground, bloody, 1.0 - t_y)
            pxl[x, y] = (*c, 255)
    draw = ImageDraw.Draw(img)
    rng = random.Random(seed + 31)
    # Drip drops
    for _ in range(6):
        x, y = rng.randint(0, 15), rng.randint(0, 12)
        draw.point((x, y), fill=(255, 60, 60, 255))
        if y + 1 < 16:
            draw.point((x, y + 1), fill=(180, 20, 20, 255))
    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


# ----------------------------------------------------------------------
# Animated blood fluid
# ----------------------------------------------------------------------
def blood_fluid_animated(still_path, flow_path, frames=8):
    """Generate animated still (16×(16*frames)) and flow (32×(32*frames)) PNGs."""
    # Still
    still = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        noise = fbm(16, 16, 4242 + f * 31, octaves=3)
        pxl = still.load()
        for y in range(16):
            for x in range(16):
                n = noise[y][x]
                pulse = 0.5 + 0.5 * math.sin(2 * math.pi * f / frames)
                base = lerp((110, 10, 10), (200, 30, 30), n)
                c = lerp(base, (230, 50, 30), pulse * 0.25)
                pxl[x, 16 * f + y] = (*c, 230)
    still.save(still_path)
    print(f"  [ok] {os.path.basename(still_path)}")

    # Flow (32 wide)
    flow = Image.new("RGBA", (32, 32 * frames))
    for f in range(frames):
        noise = fbm(32, 32, 9988 + f * 37, octaves=3)
        pxl = flow.load()
        for y in range(32):
            for x in range(32):
                n = noise[y][x]
                shift = (y + f * 2) / 32.0
                wave = 0.5 + 0.5 * math.sin(shift * math.pi * 2)
                base = lerp((100, 8, 8), (210, 40, 30), n)
                c = lerp(base, (250, 70, 40), wave * 0.3)
                pxl[x, 32 * f + y] = (*c, 235)
    flow.save(flow_path)
    print(f"  [ok] {os.path.basename(flow_path)}")


# ----------------------------------------------------------------------
# Armor layers (64×32 vanilla layout)
# ----------------------------------------------------------------------
def armor_layer(path, layer, base, vein, highlight, seed=7):
    """Generate a 64×32 armor layer using the *vanilla* diamond-armor UV
    layout. The UV regions (per the Bedrock/Java model) are:

      Head  : x=0..32,  y=0..16   (6 faces of 8×8 cube)
      Hat   : x=32..64, y=0..16   (layer 1 only; still safe to paint both)
      Body  : x=16..40, y=16..32  (8×12×4 torso net)
      Arm   : x=40..56, y=16..32  (4×12×4 arm net — shared L/R)
      Leg   : x=0..16,  y=16..32  (4×12×4 leg net — shared L/R)

    Painting exactly these regions (and leaving the rest transparent) is
    what prevents Minecraft from stretching "pixels" into giant cubes.
    """
    w, h = 64, 32
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    pxl = img.load()
    noise = fbm(w, h, seed, octaves=4)

    # Regions to paint. Layer 1 is full armor (helm+chest+arms+legs).
    # Layer 2 is leggings/boots (body+legs). We paint all regions on
    # both layers for visual completeness — unused UV simply isn't
    # sampled by the respective armor slot.
    regions_layer1 = [
        (0,  0,  32, 16),  # head faces
        (32, 0,  64, 16),  # hat (inflated helmet overlay)
        (16, 16, 40, 32),  # body
        (40, 16, 56, 32),  # arm
        (0,  16, 16, 32),  # leg
    ]
    regions_layer2 = [
        (16, 16, 40, 32),  # body (leggings waist)
        (0,  16, 16, 32),  # leg (leggings + boots)
    ]
    regions = regions_layer1 if layer == 1 else regions_layer2

    draw = ImageDraw.Draw(img)
    for (x0, y0, x1, y1) in regions:
        # Fill with fBm-noise-tinted base color
        for y in range(y0, y1):
            for x in range(x0, x1):
                n = noise[y][x]
                n = max(0.0, min(1.0, (n - 0.3) / 0.5))
                c = lerp(base, vein, n)
                pxl[x, y] = (*c, 255)
        # Add vein streaks inside the region
        rng = random.Random(seed + x0 * 7 + y0 * 13)
        streak_count = max(3, (x1 - x0) * (y1 - y0) // 40)
        for _ in range(streak_count):
            sx, sy = rng.randint(x0, x1 - 1), rng.randint(y0, y1 - 1)
            for _ in range(rng.randint(2, 4)):
                nx = max(x0, min(x1 - 1, sx + rng.randint(-2, 2)))
                ny = max(y0, min(y1 - 1, sy + rng.randint(-1, 1)))
                draw.line((sx, sy, nx, ny), fill=(*highlight, 255))
                sx, sy = nx, ny
        # Rim highlight along region edges (armor plate edge feel)
        for x in range(x0, x1):
            pxl[x, y0] = (*highlight, 255)
            pxl[x, y1 - 1] = (*vein, 255)
        for y in range(y0, y1):
            pxl[x0, y] = (*highlight, 255)
            pxl[x1 - 1, y] = (*vein, 255)
    img.save(path)
    print(f"  [ok] {os.path.basename(path)}")


# ----------------------------------------------------------------------
# MCMeta writers
# ----------------------------------------------------------------------
def write_mcmeta(path, frametime=4, frames=8):
    with open(path, "w", encoding="utf-8") as f:
        f.write(
            '{\n'
            '  "animation": {\n'
            f'    "frametime": {frametime},\n'
            '    "interpolate": true\n'
            '  }\n'
            '}\n'
        )


# ----------------------------------------------------------------------
# Main
# ----------------------------------------------------------------------
def main():
    print("=== Liberthia textures v3 ===")

    print("\nBlocks:")
    living_flesh(os.path.join(BLK, "living_flesh.png"), seed=11)
    attacking_flesh(os.path.join(BLK, "attacking_flesh.png"), seed=22)
    flesh_mother(os.path.join(BLK, "flesh_mother.png"), seed=33)
    blood_infection(os.path.join(BLK, "blood_infection_block.png"), seed=44)
    blood_infestation(os.path.join(BLK, "blood_infestation_block.png"), seed=55)
    blood_altar_top(os.path.join(BLK, "blood_altar_top.png"), seed=66)
    blood_altar_side(os.path.join(BLK, "blood_altar_side.png"), seed=67)
    blood_altar_bottom(os.path.join(BLK, "blood_altar_bottom.png"), seed=68)
    blood_volcano_top(os.path.join(BLK, "blood_volcano_top.png"), seed=77)
    blood_volcano_side(os.path.join(BLK, "blood_volcano_side.png"), seed=78)
    blood_volcano_bottom(os.path.join(BLK, "blood_volcano_bottom.png"), seed=79)
    blood_spike(os.path.join(BLK, "blood_spike.png"), seed=88)

    print("\nChalk (transparent):")
    chalk_symbol(os.path.join(BLK, "chalk_symbol.png"))

    print("\nBlood fluid (animated):")
    still = os.path.join(BLK, "blood_still.png")
    flow = os.path.join(BLK, "blood_flow.png")
    blood_fluid_animated(still, flow, frames=8)
    write_mcmeta(still + ".mcmeta", frametime=4, frames=8)
    write_mcmeta(flow + ".mcmeta", frametime=4, frames=8)
    print(f"  [ok] blood_still.png.mcmeta")
    print(f"  [ok] blood_flow.png.mcmeta")

    print("\nArmor (64×32):")
    # Blood palette
    armor_layer(os.path.join(ARMOR, "blood_layer_1.png"), 1,
                (107, 10, 10), (60, 5, 5), (194, 32, 32), seed=201)
    armor_layer(os.path.join(ARMOR, "blood_layer_2.png"), 2,
                (107, 10, 10), (60, 5, 5), (194, 32, 32), seed=202)
    # Order palette
    armor_layer(os.path.join(ARMOR, "order_layer_1.png"), 1,
                (212, 175, 55), (158, 115, 20), (255, 248, 220), seed=301)
    armor_layer(os.path.join(ARMOR, "order_layer_2.png"), 2,
                (212, 175, 55), (158, 115, 20), (255, 248, 220), seed=302)

    print("\nDone.")


if __name__ == "__main__":
    main()
