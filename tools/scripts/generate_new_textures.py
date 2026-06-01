from PIL import Image, ImageDraw
import os, random, math

BASE = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures"
os.makedirs(f"{BASE}/item", exist_ok=True)
os.makedirs(f"{BASE}/mob_effect", exist_ok=True)

def save(img, path):
    full = f"{BASE}/{path}"
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("saved", path)

# Gravity Trap
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.ellipse([3, 3, 12, 12], fill=(10, 0, 20, 255))
for r in range(6, 2, -1):
    a = 100 + r*20
    d.ellipse([8-r, 8-r, 8+r, 8+r], outline=(120 + r*10, 30, 180, a))
d.ellipse([7, 7, 9, 9], fill=(200, 100, 255, 255))
for _ in range(10):
    x = random.randint(1, 14); y = random.randint(1, 14)
    if abs(x-8) + abs(y-8) > 6:
        d.point((x, y), fill=(180, 80, 220, 200))
save(img, "item/gravity_trap.png")

# Revelation Lens
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.ellipse([2, 4, 13, 11], fill=(200, 240, 255, 255), outline=(80, 160, 200, 255))
d.ellipse([5, 5, 10, 10], fill=(0, 180, 220, 255))
d.ellipse([6, 6, 9, 9], fill=(0, 30, 60, 255))
d.point((7, 6), fill=(255, 255, 255, 255))
for a in [0, 45, 90, 135]:
    rad = math.radians(a)
    x = int(8 + math.cos(rad) * 7); y = int(7 + math.sin(rad) * 4)
    if 0 <= x < 16 and 0 <= y < 16:
        d.point((x, y), fill=(180, 240, 255, 200))
save(img, "item/revelation_lens.png")

# Gravity Anchor
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rectangle([7, 2, 9, 12], fill=(60, 60, 70, 255))
d.rectangle([3, 10, 13, 12], fill=(50, 50, 60, 255))
d.rectangle([3, 12, 4, 14], fill=(60, 60, 70, 255))
d.rectangle([12, 12, 13, 14], fill=(60, 60, 70, 255))
d.ellipse([6, 0, 10, 4], outline=(80, 80, 90, 255), width=1)
for _ in range(5):
    x = random.randint(1, 14); y = random.randint(1, 14)
    d.point((x, y), fill=(200, 50, 50, 120))
save(img, "item/gravity_anchor.png")

# Freeze Staff
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for y in range(5, 16):
    d.point((y-5+5, y), fill=(100, 70, 40, 255))
    d.point((y-5+6, y), fill=(80, 55, 30, 255))
d.polygon([(3, 5), (8, 0), (13, 5), (10, 8), (6, 8)], fill=(200, 240, 255, 255), outline=(100, 180, 220, 255))
d.polygon([(6, 4), (8, 2), (10, 4), (9, 6), (7, 6)], fill=(255, 255, 255, 255))
for pt in [(3, 2), (14, 3), (2, 6), (13, 7)]:
    d.point(pt, fill=(220, 240, 255, 255))
save(img, "item/freeze_staff.png")

# Marking Stick
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for i in range(10):
    d.point((i+3, 13-i), fill=(180, 140, 60, 255))
    d.point((i+4, 13-i), fill=(140, 100, 40, 255))
d.ellipse([1, 10, 5, 14], fill=(220, 30, 30, 255), outline=(100, 0, 0, 255))
d.point((2, 11), fill=(255, 200, 200, 255))
d.rectangle([11, 1, 14, 4], fill=(255, 220, 60, 255), outline=(180, 140, 0, 255))
save(img, "item/marking_stick.png")

# Execution Stick
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for i in range(10):
    d.point((i+3, 13-i), fill=(80, 50, 100, 255))
    d.point((i+4, 13-i), fill=(50, 30, 70, 255))
d.ellipse([10, 0, 15, 5], fill=(140, 50, 200, 255), outline=(60, 20, 100, 255))
d.point((11, 1), fill=(220, 180, 255, 255))
d.ellipse([1, 10, 5, 14], fill=(180, 30, 220, 255))
save(img, "item/execution_stick.png")

# Summon Staff
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for i in range(10):
    d.point((i+3, 13-i), fill=(60, 40, 30, 255))
    d.point((i+4, 13-i), fill=(40, 25, 20, 255))
d.polygon([(10, 5), (13, 0), (15, 5), (13, 7)], fill=(80, 220, 100, 255), outline=(30, 120, 50, 255))
d.point((12, 2), fill=(180, 255, 200, 255))
d.point((5, 10), fill=(150, 255, 170, 255))
d.point((2, 13), fill=(150, 255, 170, 255))
save(img, "item/summon_staff.png")

# Improved syringe
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rectangle([12, 8, 15, 9], fill=(200, 200, 210, 255))
d.rectangle([3, 7, 12, 10], fill=(230, 240, 250, 180), outline=(150, 180, 200, 255))
d.rectangle([4, 7, 10, 10], fill=(180, 100, 220, 230))
d.rectangle([4, 7, 10, 7], fill=(220, 150, 240, 255))
d.rectangle([1, 6, 3, 11], fill=(120, 120, 130, 255), outline=(60, 60, 70, 255))
d.rectangle([0, 7, 1, 10], fill=(150, 150, 160, 255))
d.point((5, 8), fill=(255, 220, 255, 255))
d.point((8, 8), fill=(255, 220, 255, 200))
save(img, "item/white_matter_syringe.png")

def effect_icon(color_main, color_accent, pattern):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 16, 16], fill=color_main)
    d.ellipse([2, 2, 15, 15], outline=color_accent, width=1)
    if pattern == "dark":
        d.ellipse([6, 6, 11, 11], fill=(20, 0, 30, 255))
        for pt in [(4, 9), (13, 9), (9, 4), (9, 13)]:
            d.point(pt, fill=color_accent)
    elif pattern == "radiation":
        for a in [0, 120, 240]:
            rad = math.radians(a)
            x = int(9 + math.cos(rad) * 4); y = int(9 + math.sin(rad) * 4)
            d.ellipse([x-2, y-2, x+2, y+2], fill=color_accent)
        d.ellipse([7, 7, 10, 10], fill=(255, 255, 100, 255))
    elif pattern == "shield":
        d.rectangle([8, 4, 9, 13], fill=(255, 255, 255, 255))
        d.rectangle([5, 8, 12, 9], fill=(255, 255, 255, 255))
        d.ellipse([7, 7, 10, 10], fill=color_accent)
    return img

save(effect_icon((60, 20, 80, 255), (180, 80, 220, 255), "dark"), "mob_effect/dark_infection.png")
save(effect_icon((80, 90, 30, 255), (230, 240, 80, 255), "radiation"), "mob_effect/radiation_sickness.png")
save(effect_icon((40, 120, 180, 255), (180, 230, 255, 255), "shield"), "mob_effect/clear_shield.png")

print("Done!")
