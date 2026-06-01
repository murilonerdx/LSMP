from PIL import Image, ImageDraw
import os

BASE = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures"

def save(img, path):
    full = f"{BASE}/{path}"
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("saved", path)

# Holy Blade - golden sword with white glow
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Blade (diagonal)
for i in range(10):
    d.point((14 - i, 1 + i), fill=(255, 255, 240, 255))
    d.point((13 - i, 1 + i), fill=(255, 240, 180, 255))
    d.point((15 - i, 2 + i), fill=(255, 230, 150, 255))
# Cross guard
d.rectangle([3, 10, 7, 11], fill=(220, 180, 60, 255), outline=(120, 90, 30, 255))
# Handle
d.rectangle([4, 12, 6, 15], fill=(90, 60, 30, 255))
# Pommel - glowing
d.rectangle([3, 14, 7, 15], fill=(255, 230, 100, 255))
# Glow sparkles
d.point((13, 3), fill=(255, 255, 255, 255))
d.point((11, 5), fill=(255, 255, 220, 255))
d.point((9, 7), fill=(255, 255, 220, 255))
save(img, "item/holy_blade.png")

# Holy Hammer - big head with golden runes
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Hammer head
d.rectangle([2, 2, 12, 7], fill=(230, 220, 180, 255), outline=(120, 100, 60, 255))
d.rectangle([3, 3, 11, 6], fill=(250, 235, 190, 255))
# Runes on head
d.point((5, 4), fill=(255, 200, 50, 255))
d.point((7, 4), fill=(255, 200, 50, 255))
d.point((9, 4), fill=(255, 200, 50, 255))
d.point((6, 5), fill=(255, 220, 80, 255))
d.point((8, 5), fill=(255, 220, 80, 255))
# Handle
d.rectangle([6, 8, 8, 15], fill=(80, 55, 25, 255), outline=(40, 25, 10, 255))
# Grip wrap
d.rectangle([6, 10, 8, 10], fill=(180, 20, 20, 255))
d.rectangle([6, 12, 8, 12], fill=(180, 20, 20, 255))
# Shine
d.point((4, 3), fill=(255, 255, 255, 255))
save(img, "item/holy_hammer.png")

# Blood Fountain block - dark red with pulsing core
img = Image.new("RGBA", (16, 16), (255, 30, 30, 255))
d = ImageDraw.Draw(img)
# Base fill - dark red
d.rectangle([0, 0, 15, 15], fill=(100, 10, 10, 255))
# Inner fleshy mass
d.rectangle([2, 2, 13, 13], fill=(150, 20, 20, 255))
d.rectangle([3, 3, 12, 12], fill=(180, 30, 30, 255))
# Pulsing core
d.ellipse([5, 5, 10, 10], fill=(220, 50, 50, 255))
d.ellipse([6, 6, 9, 9], fill=(255, 100, 100, 255))
d.ellipse([7, 7, 8, 8], fill=(255, 200, 200, 255))
# Blood drips
d.point((1, 4), fill=(180, 0, 0, 255))
d.point((14, 6), fill=(180, 0, 0, 255))
d.point((2, 11), fill=(160, 0, 0, 255))
d.point((13, 13), fill=(160, 0, 0, 255))
# Veins
d.line([(0, 7), (3, 7)], fill=(70, 5, 5, 255))
d.line([(12, 7), (15, 7)], fill=(70, 5, 5, 255))
d.line([(7, 0), (7, 3)], fill=(70, 5, 5, 255))
d.line([(7, 12), (7, 15)], fill=(70, 5, 5, 255))
save(img, "block/blood_fountain.png")

print("Done!")
