"""Generate placeholder textures for new blocks and items in Liberthia mod."""
from PIL import Image, ImageDraw
import os
import random

BLOCK_DIR = "src/main/resources/assets/liberthia/textures/block"
ITEM_DIR = "src/main/resources/assets/liberthia/textures/item"
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

def save(img, path):
    img.save(path)
    print(f"  Created: {path}")

# --- Glitch Block textures (8 phases) ---
# Each phase has a different scrambled/corrupted look
glitch_colors = [
    (20, 0, 30),    # dark purple
    (0, 0, 0),      # pure black
    (40, 0, 60),    # medium purple
    (10, 10, 10),   # near black
    (60, 0, 90),    # bright purple
    (5, 5, 15),     # dark blue-black
    (80, 0, 50),    # magenta-ish
    (0, 0, 20),     # dark navy
]

for phase in range(8):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)
    base = glitch_colors[phase]
    random.seed(phase * 42)  # Deterministic per phase

    # Fill with base color
    draw.rectangle([0, 0, 15, 15], fill=base + (255,))

    # Add glitch lines (horizontal scan lines)
    for y in range(16):
        if random.random() < 0.4:
            glitch_r = min(255, base[0] + random.randint(20, 100))
            glitch_g = random.randint(0, 30)
            glitch_b = min(255, base[2] + random.randint(20, 80))
            offset = random.randint(-3, 3)
            for x in range(max(0, offset), min(16, 16 + offset)):
                img.putpixel((x, y), (glitch_r, glitch_g, glitch_b, 255))

    # Add random bright pixels (digital noise)
    for _ in range(random.randint(5, 15)):
        x = random.randint(0, 15)
        y = random.randint(0, 15)
        c = random.choice([(255, 0, 255, 255), (0, 255, 255, 255), (255, 255, 0, 255), (255, 0, 0, 255)])
        img.putpixel((x, y), c)

    save(img, f"{BLOCK_DIR}/glitch_block_{phase}.png")

# --- Wormhole Block ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
draw = ImageDraw.Draw(img)
# Dark portal with purple swirl
for y in range(16):
    for x in range(16):
        dx = x - 7.5
        dy = y - 7.5
        dist = (dx*dx + dy*dy) ** 0.5
        if dist < 6:
            intensity = int(120 * (1.0 - dist / 6.0))
            r = intensity // 2
            g = 0
            b = intensity
            img.putpixel((x, y), (r, g, b, 255))
        else:
            img.putpixel((x, y), (15, 0, 25, 255))
# Add bright center
for dx in range(-1, 2):
    for dy in range(-1, 2):
        img.putpixel((8 + dx, 8 + dy), (180, 50, 255, 255))
save(img, f"{BLOCK_DIR}/wormhole_block.png")

# --- Clear Matter Bucket ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
# Bucket shape
draw.rectangle([3, 4, 12, 14], fill=(180, 180, 180, 255))  # bucket body
draw.rectangle([4, 5, 11, 13], fill=(150, 220, 255, 255))   # clear matter fill
draw.rectangle([5, 2, 10, 4], fill=(200, 200, 200, 255))    # bucket rim
draw.line([3, 1, 5, 1], fill=(160, 160, 160, 255))          # handle
draw.line([10, 1, 12, 1], fill=(160, 160, 160, 255))
draw.line([5, 0, 10, 0], fill=(160, 160, 160, 255))
save(img, f"{ITEM_DIR}/clear_matter_bucket.png")

# --- Yellow Matter Bucket ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
draw.rectangle([3, 4, 12, 14], fill=(180, 180, 180, 255))
draw.rectangle([4, 5, 11, 13], fill=(244, 180, 0, 255))     # yellow matter fill
draw.rectangle([5, 2, 10, 4], fill=(200, 200, 200, 255))
draw.line([3, 1, 5, 1], fill=(160, 160, 160, 255))
draw.line([10, 1, 12, 1], fill=(160, 160, 160, 255))
draw.line([5, 0, 10, 0], fill=(160, 160, 160, 255))
save(img, f"{ITEM_DIR}/yellow_matter_bucket.png")

# --- White Matter Syringe ---
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
# Syringe body (diagonal)
for i in range(10):
    x = 3 + i
    y = 12 - i
    img.putpixel((x, y), (220, 220, 240, 255))
    img.putpixel((x, y-1), (220, 220, 240, 255))
# Plunger end
draw.line([2, 13, 3, 12], fill=(180, 180, 200, 255))
draw.line([1, 14, 2, 13], fill=(180, 180, 200, 255))
# Needle tip
draw.line([13, 2, 14, 1], fill=(200, 200, 200, 255))
draw.line([14, 1, 15, 0], fill=(255, 255, 255, 255))
# White matter glow in barrel
for i in range(4, 8):
    x = 3 + i
    y = 12 - i
    img.putpixel((x, y), (255, 255, 255, 255))
save(img, f"{ITEM_DIR}/white_matter_syringe.png")

print("\nAll textures generated!")
