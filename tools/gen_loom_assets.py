"""Gera assets da Loom Dimension — 3 monstros + worm + laser + portal block."""
import os, random
from PIL import Image, ImageDraw, ImageFilter

random.seed(33)
BASE = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources"
ENT = os.path.join(BASE, "assets/liberthia/textures/entity")
ITM = os.path.join(BASE, "assets/liberthia/textures/item")
BLK = os.path.join(BASE, "assets/liberthia/textures/block")
for d in [ENT, ITM, BLK]:
    os.makedirs(d, exist_ok=True)


def gen_loom_watcher():
    """Humanoid 64×32 texture (zombie format). All dark with purple eyes."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # base color: dark gray
    d.rectangle((0, 0, 64, 32), fill=(20, 18, 30, 255))
    # head front (8x8 at 8,8)
    d.rectangle((8, 8, 16, 16), fill=(40, 35, 50, 255))
    # eyes (glowing purple)
    d.rectangle((10, 11, 11, 12), fill=(180, 80, 255, 255))
    d.rectangle((13, 11, 14, 12), fill=(180, 80, 255, 255))
    # body
    d.rectangle((20, 20, 28, 32), fill=(15, 12, 25, 255))
    img.save(os.path.join(ENT, "loom_watcher.png"))


def gen_loom_peripheral():
    """All black with white glowing eyes."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 64, 32), fill=(5, 5, 8, 255))
    # head front
    d.rectangle((8, 8, 16, 16), fill=(0, 0, 0, 255))
    # white glowing eyes
    d.rectangle((10, 11, 11, 12), fill=(255, 255, 255, 255))
    d.rectangle((13, 11, 14, 12), fill=(255, 255, 255, 255))
    # body — all black
    d.rectangle((20, 20, 28, 32), fill=(0, 0, 0, 255))
    img.save(os.path.join(ENT, "loom_peripheral.png"))


def gen_loom_screamer():
    """Red/dark with screaming mouth (open)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 64, 32), fill=(50, 10, 15, 255))
    # head front
    d.rectangle((8, 8, 16, 16), fill=(120, 30, 40, 255))
    # eyes (red glow)
    d.rectangle((10, 11, 11, 12), fill=(255, 50, 50, 255))
    d.rectangle((13, 11, 14, 12), fill=(255, 50, 50, 255))
    # mouth OPEN (screaming)
    d.rectangle((10, 13, 14, 15), fill=(0, 0, 0, 255))
    d.rectangle((10, 13, 14, 14), fill=(180, 30, 40, 255))
    # body
    d.rectangle((20, 20, 28, 32), fill=(80, 20, 30, 255))
    img.save(os.path.join(ENT, "loom_screamer.png"))


def gen_loom_worm():
    """Silverfish-like texture 64×32. Gray with red blood."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 64, 32), fill=(80, 80, 90, 255))
    # spots blood-red
    for _ in range(20):
        x, y = random.randint(0, 60), random.randint(0, 28)
        d.rectangle((x, y, x + 1, y + 1), fill=(180, 30, 30, 255))
    img.save(os.path.join(ENT, "loom_worm.png"))


def gen_dark_matter_laser():
    """Item icon — cyber-gun with purple core."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # gun body
    d.rectangle((1, 6, 11, 11), fill=(40, 20, 60, 255))
    d.rectangle((1, 6, 11, 7), fill=(80, 50, 110, 255))  # highlight
    # barrel
    d.rectangle((11, 7, 15, 10), fill=(20, 10, 30, 255))
    d.ellipse((14, 7, 16, 10), fill=(180, 50, 255, 255))
    # grip
    d.rectangle((2, 11, 5, 15), fill=(30, 15, 45, 255))
    # power core (purple glowing)
    d.rectangle((5, 8, 9, 10), fill=(150, 50, 220, 255))
    d.rectangle((6, 8, 8, 9), fill=(255, 200, 255, 255))
    img.save(os.path.join(ITM, "dark_matter_laser.png"))


def gen_loom_portal():
    """Portal block texture — swirling dark purple/black."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # base dark
    for x in range(16):
        for y in range(16):
            v = (x * y + (x - y) * 3) % 100
            r = max(20, 80 - v)
            g = max(0, 20 - v // 4)
            b = max(40, 120 - v // 2)
            d.point((x, y), fill=(r, g, b, 200))
    # spiral hint
    import math
    for t in range(0, 360, 6):
        a = math.radians(t)
        radius = 4 + (t / 90)
        if radius > 7: break
        x = 8 + math.cos(a) * radius
        y = 8 + math.sin(a) * radius
        d.point((x, y), fill=(180, 80, 255, 220))
    img.save(os.path.join(BLK, "loom_portal.png"))


if __name__ == "__main__":
    gen_loom_watcher()
    gen_loom_peripheral()
    gen_loom_screamer()
    gen_loom_worm()
    gen_dark_matter_laser()
    gen_loom_portal()
    print("LOOM assets generated.")
