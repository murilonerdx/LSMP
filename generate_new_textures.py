"""Generate 16x16 pixel textures for new Liberthia blocks and items."""
from PIL import Image, ImageDraw
import random, os

BLOCK_DIR = "src/main/resources/assets/liberthia/textures/block"
ITEM_DIR = "src/main/resources/assets/liberthia/textures/item"
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

def noise_fill(img, base_rgb, var=20):
    """Fill image with noisy pixels around a base color."""
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = max(0, min(255, base_rgb[0] + random.randint(-var, var)))
            g = max(0, min(255, base_rgb[1] + random.randint(-var, var)))
            b = max(0, min(255, base_rgb[2] + random.randint(-var, var)))
            px[x, y] = (r, g, b, 255)

def save(img, path):
    img.save(path)
    print(f"  Created: {path}")

# --- BLOCK TEXTURES ---

# Corrupted Stone - dark purple stone
img = Image.new("RGBA", (16, 16))
noise_fill(img, (60, 30, 80), 15)
# Add stone-like cracks
d = ImageDraw.Draw(img)
d.line([(3,5),(7,6),(10,4)], fill=(40,15,55,255))
d.line([(2,11),(6,12),(9,10)], fill=(40,15,55,255))
save(img, f"{BLOCK_DIR}/corrupted_stone.png")

# Infection Vein - dark veins on stone
img = Image.new("RGBA", (16, 16))
noise_fill(img, (80, 80, 85), 10)  # Stone background
d = ImageDraw.Draw(img)
# Purple veins
for _ in range(5):
    x1, y1 = random.randint(0,15), random.randint(0,15)
    x2, y2 = x1+random.randint(-4,4), y1+random.randint(-4,4)
    d.line([(x1,y1),(x2,y2)], fill=(120,30,180,255), width=1)
# Glow spots
for _ in range(3):
    x, y = random.randint(2,13), random.randint(2,13)
    d.point((x,y), fill=(180,60,255,255))
save(img, f"{BLOCK_DIR}/infection_vein.png")

# Spore Bloom - dark flower
img = Image.new("RGBA", (16, 16), (0,0,0,0))
d = ImageDraw.Draw(img)
# Stem
d.line([(8,15),(8,8)], fill=(40,60,30,255), width=1)
# Petals (dark purple)
for dx, dy in [(-2,-1),(2,-1),(0,-3),(-1,-2),(1,-2)]:
    d.ellipse([7+dx-1, 7+dy-1, 7+dx+1, 7+dy+1], fill=(130,30,170,255))
# Center
d.point((8,7), fill=(200,50,255,255))
d.point((7,8), fill=(200,50,255,255))
save(img, f"{BLOCK_DIR}/spore_bloom.png")

# Corrupted Log - side
img = Image.new("RGBA", (16, 16))
noise_fill(img, (55, 35, 65), 12)
d = ImageDraw.Draw(img)
# Bark lines
for y in [2,5,8,11,14]:
    d.line([(0,y),(15,y)], fill=(35,20,45,255))
# Purple streaks
for _ in range(4):
    x = random.randint(2,13)
    d.line([(x,0),(x+1,15)], fill=(100,30,140,200))
save(img, f"{BLOCK_DIR}/corrupted_log.png")

# Corrupted Log - top
img = Image.new("RGBA", (16, 16))
noise_fill(img, (50, 30, 60), 10)
d = ImageDraw.Draw(img)
# Ring pattern
d.ellipse([3,3,12,12], outline=(80,40,100,255))
d.ellipse([5,5,10,10], outline=(70,35,90,255))
d.point((8,8), fill=(120,50,160,255))
save(img, f"{BLOCK_DIR}/corrupted_log_top.png")

# White Matter TNT
img = Image.new("RGBA", (16, 16))
noise_fill(img, (230, 240, 255), 10)
d = ImageDraw.Draw(img)
# TNT-like bands
d.rectangle([0,0,15,3], fill=(200,220,255,255))
d.rectangle([0,12,15,15], fill=(200,220,255,255))
# Center marking
d.text((4,5), "WM", fill=(150,180,255,255))
save(img, f"{BLOCK_DIR}/white_matter_tnt.png")

# --- ITEM TEXTURES ---

def make_sword(name, blade_rgb, guard_rgb):
    img = Image.new("RGBA", (16,16), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # Blade diagonal
    for i in range(8):
        d.point((3+i, 12-i), fill=(*blade_rgb, 255))
        d.point((4+i, 12-i), fill=(*[min(255,c+30) for c in blade_rgb], 255))
    # Guard
    d.line([(4,11),(6,9)], fill=(*guard_rgb, 255), width=1)
    # Handle
    d.line([(1,15),(3,13)], fill=(100,70,40,255), width=1)
    save(img, f"{ITEM_DIR}/{name}.png")

def make_pickaxe(name, head_rgb):
    img = Image.new("RGBA", (16,16), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # Handle
    d.line([(2,14),(8,8)], fill=(100,70,40,255), width=1)
    # Head
    d.line([(5,5),(11,5)], fill=(*head_rgb, 255), width=2)
    d.line([(6,4),(10,4)], fill=(*[min(255,c+20) for c in head_rgb], 255))
    save(img, f"{ITEM_DIR}/{name}.png")

def make_axe(name, head_rgb):
    img = Image.new("RGBA", (16,16), (0,0,0,0))
    d = ImageDraw.Draw(img)
    # Handle
    d.line([(2,14),(9,7)], fill=(100,70,40,255), width=1)
    # Head
    d.polygon([(8,4),(12,6),(11,9),(8,7)], fill=(*head_rgb, 255))
    save(img, f"{ITEM_DIR}/{name}.png")

# Clear Matter tools (cyan)
make_sword("clear_matter_sword", (76,201,240), (100,220,250))
make_pickaxe("clear_matter_pickaxe", (76,201,240))
make_axe("clear_matter_axe", (76,201,240))

# Yellow Matter tools (gold)
make_sword("yellow_matter_sword", (244,180,0), (255,200,50))
make_pickaxe("yellow_matter_pickaxe", (244,180,0))
make_axe("yellow_matter_axe", (244,180,0))

# Yellow Matter Shield
img = Image.new("RGBA", (16,16), (0,0,0,0))
d = ImageDraw.Draw(img)
d.polygon([(8,1),(2,4),(2,11),(8,14),(14,11),(14,4)], fill=(244,180,0,255), outline=(200,150,0,255))
d.polygon([(8,3),(4,5),(4,10),(8,12),(12,10),(12,5)], fill=(255,210,50,255))
save(img, f"{ITEM_DIR}/yellow_matter_shield.png")

# Containment Suit pieces
for piece, color in [("helmet",(180,200,180)), ("chestplate",(160,190,170)), ("leggings",(150,180,160)), ("boots",(140,170,150))]:
    img = Image.new("RGBA", (16,16), (0,0,0,0))
    noise_fill(img, color, 8)
    d = ImageDraw.Draw(img)
    # Yellow accents
    d.line([(3,3),(12,3)], fill=(244,180,0,255))
    d.line([(3,12),(12,12)], fill=(244,180,0,255))
    # Visor for helmet
    if piece == "helmet":
        d.rectangle([5,5,10,8], fill=(76,201,240,180))
    save(img, f"{ITEM_DIR}/containment_suit_{piece}.png")

# Protection Ruby
img = Image.new("RGBA", (16,16), (0,0,0,0))
d = ImageDraw.Draw(img)
# Diamond/ruby shape
d.polygon([(8,2),(3,7),(8,13),(13,7)], fill=(220,180,255,255), outline=(180,120,220,255))
# Inner glow
d.polygon([(8,4),(5,7),(8,11),(11,7)], fill=(240,220,255,255))
d.point((8,7), fill=(255,255,255,255))
save(img, f"{ITEM_DIR}/protection_ruby.png")

print("\nAll textures generated!")
