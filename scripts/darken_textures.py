"""Darken all infection block textures to be predominantly black with subtle dark purple hints."""
from PIL import Image, ImageDraw
import os
import random

BLOCK_DIR = "src/main/resources/assets/liberthia/textures/block"

def save(img, path):
    img.save(path)
    print(f"  Created: {path}")

# --- Corrupted Soil: very dark earth with faint purple veins ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
random.seed(100)
for y in range(16):
    for x in range(16):
        r = random.randint(2, 8)
        g = random.randint(0, 3)
        b = random.randint(3, 12)
        img.putpixel((x, y), (r, g, b, 255))
# Subtle purple veins
for _ in range(6):
    sx = random.randint(0, 15)
    sy = random.randint(0, 15)
    for i in range(3):
        nx = min(15, max(0, sx + random.randint(-1, 1)))
        ny = min(15, max(0, sy + random.randint(-1, 1)))
        img.putpixel((nx, ny), (15, 0, 20, 255))
        sx, sy = nx, ny
save(img, f"{BLOCK_DIR}/corrupted_soil.png")

# --- Corrupted Stone: black stone with barely visible dark gray cracks ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
random.seed(200)
for y in range(16):
    for x in range(16):
        r = random.randint(3, 10)
        g = random.randint(2, 6)
        b = random.randint(4, 14)
        img.putpixel((x, y), (r, g, b, 255))
# Dark cracks
for _ in range(4):
    sx = random.randint(0, 15)
    sy = random.randint(0, 15)
    for i in range(5):
        nx = min(15, max(0, sx + random.randint(-1, 1)))
        ny = min(15, max(0, sy + random.choice([-1, 0, 1])))
        img.putpixel((nx, ny), (18, 5, 25, 255))
        sx, sy = nx, ny
save(img, f"{BLOCK_DIR}/corrupted_stone.png")

# --- Infection Growth: dark black tendril with faint purple glow ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))  # transparent background
random.seed(300)
# Central tendril
for y in range(16):
    cx = 7 + random.randint(-1, 1)
    for dx in range(-1, 2):
        x = cx + dx
        if 0 <= x < 16:
            intensity = random.randint(5, 15)
            img.putpixel((x, y), (intensity, 0, intensity + 5, 255))
# Side tendrils
for _ in range(3):
    ty = random.randint(3, 13)
    direction = random.choice([-1, 1])
    for i in range(random.randint(2, 4)):
        x = 7 + direction * (i + 1)
        if 0 <= x < 16:
            img.putpixel((x, ty), (10, 0, 15, 255))
save(img, f"{BLOCK_DIR}/infection_growth.png")

# --- Infection Vein: dark stone with glowing purple vein pattern ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
random.seed(400)
for y in range(16):
    for x in range(16):
        r = random.randint(3, 8)
        g = random.randint(2, 5)
        b = random.randint(3, 10)
        img.putpixel((x, y), (r, g, b, 255))
# Glowing veins
for _ in range(3):
    sx = random.randint(0, 15)
    sy = random.randint(0, 15)
    for i in range(8):
        nx = min(15, max(0, sx + random.randint(-1, 1)))
        ny = min(15, max(0, sy + random.randint(-1, 1)))
        img.putpixel((nx, ny), (30, 5, 45, 255))
        # Glow around vein
        for gx in range(-1, 2):
            for gy in range(-1, 2):
                px, py = nx + gx, ny + gy
                if 0 <= px < 16 and 0 <= py < 16 and (gx != 0 or gy != 0):
                    old = img.getpixel((px, py))
                    if old[2] < 20:
                        img.putpixel((px, py), (12, 2, 20, 255))
        sx, sy = nx, ny
save(img, f"{BLOCK_DIR}/infection_vein.png")

# --- Corrupted Log: dark black wood with purple corruption lines ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
random.seed(500)
for y in range(16):
    for x in range(16):
        # Wood grain pattern - vertical lines
        base = 5 + (3 if x % 3 == 0 else 0)
        r = random.randint(2, base)
        g = random.randint(1, 4)
        b = random.randint(3, base + 3)
        img.putpixel((x, y), (r, g, b, 255))
# Corruption veins
for _ in range(5):
    sy = random.randint(0, 15)
    for x in range(16):
        if random.random() < 0.6:
            sy = min(15, max(0, sy + random.randint(-1, 1)))
            img.putpixel((x, sy), (20, 3, 30, 255))
save(img, f"{BLOCK_DIR}/corrupted_log.png")

# --- Corrupted Log Top: dark circular rings ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
random.seed(501)
for y in range(16):
    for x in range(16):
        dx = x - 7.5
        dy = y - 7.5
        dist = (dx*dx + dy*dy) ** 0.5
        ring = int(dist) % 3
        base = 6 + ring * 2
        r = random.randint(2, base)
        g = random.randint(1, 3)
        b = random.randint(3, base + 4)
        img.putpixel((x, y), (r, g, b, 255))
save(img, f"{BLOCK_DIR}/corrupted_log_top.png")

# --- Spore Bloom: dark flower with subtle purple tips ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))  # transparent
random.seed(600)
# Stem
for y in range(8, 16):
    img.putpixel((7, y), (8, 3, 12, 255))
    img.putpixel((8, y), (8, 3, 12, 255))
# Bloom petals (cross pattern)
for dx in range(-2, 3):
    for dy in range(-2, 3):
        dist = abs(dx) + abs(dy)
        if dist <= 2:
            x, y = 7 + dx, 5 + dy
            if 0 <= x < 16 and 0 <= y < 16:
                intensity = max(5, 25 - dist * 8)
                img.putpixel((x, y), (intensity, 0, intensity + 10, 255))
# Bright center
img.putpixel((7, 5), (35, 5, 50, 255))
img.putpixel((8, 5), (35, 5, 50, 255))
save(img, f"{BLOCK_DIR}/spore_bloom.png")

# --- Wormhole Block: animated portal frames (4 frames stacked vertically for .mcmeta) ---
frames = 4
img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 255))
for frame in range(frames):
    random.seed(700 + frame * 13)
    oy = frame * 16
    for y in range(16):
        for x in range(16):
            dx = x - 7.5
            dy = y - 7.5
            dist = (dx*dx + dy*dy) ** 0.5
            # Swirl effect shifted per frame
            angle = (dist * 0.5 + frame * 1.5)
            swirl = (1.0 + (0.5 * ((angle % 3.14) / 3.14))) if dist < 7 else 0
            if dist < 6:
                intensity = int(40 * (1.0 - dist / 6.0) * swirl)
                r = max(0, min(255, intensity // 3))
                g = 0
                b = max(0, min(255, intensity))
                img.putpixel((x, oy + y), (r, g, b, 255))
            else:
                img.putpixel((x, oy + y), (3, 0, 5, 255))
    # Bright center shifts per frame
    cx = 7 + (frame % 2)
    cy = oy + 7 + ((frame + 1) % 2)
    for ddx in range(-1, 2):
        for ddy in range(-1, 2):
            px, py = cx + ddx, cy + ddy
            if 0 <= px < 16 and 0 <= py < 16 * frames:
                img.putpixel((px, py), (60, 10, 90, 255))
save(img, f"{BLOCK_DIR}/wormhole_block.png")

print("\nAll darkened textures generated!")
