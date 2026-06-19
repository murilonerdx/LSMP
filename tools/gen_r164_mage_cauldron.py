"""
r164: Generate the 4 textures for the 3D Mage Cauldron model.

- mage_cauldron.png         (outside walls — dark iron with magic runes)
- mage_cauldron_inside.png  (inside walls — slightly lighter dark)
- mage_cauldron_top.png     (top rim — bronze/gold trim)
- mage_cauldron_brew.png    (animated brew surface — glowing purple liquid)
"""

from PIL import Image, ImageDraw
import os
import random

OUT = os.path.join("src", "main", "resources", "assets", "liberthia", "textures", "block")
os.makedirs(OUT, exist_ok=True)

def save(img: Image.Image, name: str):
    path = os.path.join(OUT, f"{name}.png")
    img.save(path)
    print(f"  [ok] {name}.png")


def make_outside():
    """Dark iron cauldron texture 16x16 with engraved runes."""
    random.seed(42)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Dark iron base
    for x in range(16):
        for y in range(16):
            # gradient + grain
            base = 50 + random.randint(-12, 12)
            img.putpixel((x, y), (base, base - 5, base + 10, 255))
    # Add 4 horizontal "bands" (decorative iron rings)
    for ring_y in (3, 8, 13):
        for x in range(16):
            r = 90 + random.randint(-15, 15)
            img.putpixel((x, ring_y), (r, r - 10, r - 20, 255))
    # Magic runes — purple dots scattered
    for _ in range(8):
        x, y = random.randint(2, 13), random.randint(1, 14)
        img.putpixel((x, y), (140, 60, 200, 255))
    save(img, "mage_cauldron")


def make_inside():
    """Slightly darker interior than outside."""
    random.seed(99)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            base = 35 + random.randint(-8, 8)
            img.putpixel((x, y), (base, base, base + 8, 255))
    save(img, "mage_cauldron_inside")


def make_top():
    """Bronze rim with subtle gold sparkle."""
    random.seed(7)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            # Bronze gradient
            base_r = 120 + random.randint(-15, 15)
            base_g = 80 + random.randint(-10, 10)
            base_b = 30 + random.randint(-8, 8)
            img.putpixel((x, y), (base_r, base_g, base_b, 255))
    # Some bright gold flecks
    for _ in range(10):
        x, y = random.randint(0, 15), random.randint(0, 15)
        img.putpixel((x, y), (220, 180, 60, 255))
    save(img, "mage_cauldron_top")


def make_brew():
    """Glowing purple-pink magic brew liquid surface."""
    random.seed(13)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            # Base brew color (purple gradient)
            r = 130 + random.randint(-15, 25)
            g = 50 + random.randint(-10, 15)
            b = 180 + random.randint(-15, 25)
            img.putpixel((x, y), (r, g, b, 255))
    # Glowing highlights
    for _ in range(12):
        x, y = random.randint(1, 14), random.randint(1, 14)
        img.putpixel((x, y), (255, 200, 255, 255))
    # Bubble dots
    for _ in range(6):
        x, y = random.randint(2, 13), random.randint(2, 13)
        img.putpixel((x, y), (220, 160, 240, 255))
    save(img, "mage_cauldron_brew")


def main():
    make_outside()
    make_inside()
    make_top()
    make_brew()
    print("\nGenerated 4 Mage Cauldron textures.")


if __name__ == "__main__":
    main()
