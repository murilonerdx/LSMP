"""r74: textures for ores, mana berry, source relay, grimoire tiers, bookwyrm egg."""
from PIL import Image, ImageDraw
import os, random

BLOCK = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\block"
ITEM = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item"
SIZE = 16

def stone_base():
    img = Image.new("RGBA", (SIZE, SIZE), (110, 110, 110, 255))
    d = ImageDraw.Draw(img)
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x * 11 + y * 7) % 13
            if n > 10:
                img.putpixel((x, y), (130, 130, 130, 255))
            elif n < 3:
                img.putpixel((x, y), (80, 80, 80, 255))
    return img, d

def deepslate_base():
    img = Image.new("RGBA", (SIZE, SIZE), (50, 50, 55, 255))
    d = ImageDraw.Draw(img)
    for x in range(SIZE):
        for y in range(SIZE):
            n = (x * 7 + y * 13) % 13
            if n > 9:
                img.putpixel((x, y), (70, 70, 75, 255))
            elif n < 3:
                img.putpixel((x, y), (30, 30, 35, 255))
    return img, d

def sourcestone_ore():
    img, d = stone_base()
    # Source crystals (purple)
    for _ in range(5):
        x, y = random.randint(2, 13), random.randint(2, 13)
        d.point((x, y), fill=(180, 100, 230, 255))
        if x+1 < 16: d.point((x+1, y), fill=(140, 80, 200, 255))
        if y+1 < 16: d.point((x, y+1), fill=(140, 80, 200, 255))
    return img

def spirit_gem_ore():
    img, d = deepslate_base()
    # Glowing gems (cyan-purple)
    for _ in range(4):
        x, y = random.randint(3, 12), random.randint(3, 12)
        d.polygon([(x, y), (x+1, y-1), (x+2, y), (x+1, y+1)], fill=(180, 220, 255, 255), outline=(220, 240, 255, 255))
    return img

def mana_berry_stage_0():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Small sprout
    d.line([7, 14, 7, 11], fill=(80, 130, 50, 255))
    d.line([7, 11, 6, 10], fill=(120, 200, 100, 255))
    d.line([7, 11, 8, 10], fill=(120, 200, 100, 255))
    return img

def mana_berry_stage_1():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Bigger sprout, small berries
    for y in range(8, 14):
        d.point((7, y), fill=(80, 130, 50, 255))
    d.line([5, 10, 7, 9], fill=(120, 200, 100, 255))
    d.line([9, 10, 7, 9], fill=(120, 200, 100, 255))
    d.point((6, 9), fill=(180, 100, 220, 255))
    d.point((8, 9), fill=(180, 100, 220, 255))
    return img

def mana_berry_stage_2():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Full bush growing
    d.rectangle([4, 4, 11, 14], fill=(100, 160, 60, 255))
    # Leaves
    for x in range(3, 13):
        for y in range(4, 14):
            n = (x * 7 + y * 5) % 7
            if n < 3:
                img.putpixel((x, y), (80, 130, 50, 255))
    # Berries (immature)
    d.point((6, 7), fill=(180, 100, 220, 255))
    d.point((9, 7), fill=(180, 100, 220, 255))
    return img

def mana_berry_stage_3():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Mature bush full of berries
    d.rectangle([3, 3, 12, 14], fill=(100, 160, 60, 255))
    for x in range(3, 13):
        for y in range(3, 14):
            n = (x * 7 + y * 5) % 7
            if n < 3:
                img.putpixel((x, y), (80, 130, 50, 255))
    # Glowing mana berries
    berries = [(5, 5), (8, 6), (10, 5), (6, 9), (9, 9), (5, 11), (10, 12), (7, 12)]
    for bx, by in berries:
        d.ellipse([bx-1, by-1, bx+1, by+1], fill=(220, 150, 255, 255), outline=(255, 200, 255, 255))
    return img

def mana_berry_item():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Single juicy berry
    d.ellipse([4, 5, 11, 12], fill=(180, 100, 220, 255), outline=(80, 30, 130, 255))
    d.line([7, 5, 8, 3], fill=(80, 130, 50, 255))
    d.point((6, 7), fill=(255, 200, 255, 255))
    d.point((9, 9), fill=(180, 100, 220, 255))
    return img

def source_relay():
    img = Image.new("RGBA", (SIZE, SIZE), (40, 30, 60, 255))
    d = ImageDraw.Draw(img)
    # Metal frame
    d.rectangle([0, 0, 15, 15], outline=(150, 100, 200, 255))
    d.rectangle([1, 1, 14, 14], outline=(80, 60, 110, 255))
    # Center crystal
    d.polygon([(8, 3), (5, 8), (8, 13), (11, 8)], fill=(220, 150, 255, 255), outline=(255, 200, 255, 255))
    d.point((8, 8), fill=(255, 255, 255, 255))
    # Sparkles in corners
    d.point((3, 3), fill=(255, 200, 255, 255))
    d.point((12, 3), fill=(255, 200, 255, 255))
    d.point((3, 12), fill=(255, 200, 255, 255))
    d.point((12, 12), fill=(255, 200, 255, 255))
    return img

def grimoire_book(cover_color, accent):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 4, 13], fill=tuple(int(c*0.5) for c in cover_color[:3]) + (255,), outline=(20, 10, 35, 255))
    d.rectangle([4, 2, 13, 13], fill=cover_color, outline=(20, 10, 35, 255))
    d.line([13, 3, 13, 12], fill=(220, 200, 170, 255))
    # Magical symbol on cover
    d.ellipse([6, 5, 11, 10], outline=accent, width=1)
    d.point((8, 7), fill=accent)
    # Corner decoration
    d.point((6, 4), fill=accent); d.point((11, 4), fill=accent)
    d.point((6, 11), fill=accent); d.point((11, 11), fill=accent)
    return img

def bookwyrm_egg():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Egg shape (oval)
    d.ellipse([4, 3, 11, 13], fill=(157, 77, 214, 255), outline=(20, 10, 35, 255))
    # Highlights
    d.line([5, 5, 6, 4], fill=(220, 180, 255, 255))
    # Dots (eggshell pattern with gold)
    spots = [(6, 7), (9, 8), (7, 10), (5, 9), (10, 6), (8, 11)]
    for x, y in spots:
        d.point((x, y), fill=(255, 230, 100, 255))
    return img

# Stage textures for mana berry
m0 = mana_berry_stage_0(); m0.save(os.path.join(BLOCK, "mana_berry_bush_stage0.png"))
m1 = mana_berry_stage_1(); m1.save(os.path.join(BLOCK, "mana_berry_bush_stage1.png"))
m2 = mana_berry_stage_2(); m2.save(os.path.join(BLOCK, "mana_berry_bush_stage2.png"))
m3 = mana_berry_stage_3(); m3.save(os.path.join(BLOCK, "mana_berry_bush_stage3.png"))

# Ore textures
sourcestone_ore().save(os.path.join(BLOCK, "sourcestone_ore.png"))
spirit_gem_ore().save(os.path.join(BLOCK, "spirit_gem_ore.png"))

# Source relay
source_relay().save(os.path.join(BLOCK, "source_relay.png"))
source_relay().save(os.path.join(ITEM, "source_relay.png"))

# Items
mana_berry_item().save(os.path.join(ITEM, "mana_berry.png"))
mana_berry_stage_3().save(os.path.join(ITEM, "mana_berry_bush.png"))
sourcestone_ore().save(os.path.join(ITEM, "sourcestone_ore.png"))
spirit_gem_ore().save(os.path.join(ITEM, "spirit_gem_ore.png"))

# Grimoire books
grimoire_book((100, 70, 150, 255), (200, 200, 255, 255)).save(os.path.join(ITEM, "grimoire_apprentice.png"))
grimoire_book((150, 50, 100, 255), (255, 200, 100, 255)).save(os.path.join(ITEM, "grimoire_master.png"))
grimoire_book((50, 20, 80, 255), (255, 100, 255, 255)).save(os.path.join(ITEM, "grimoire_archmage.png"))

# Bookwyrm spawn egg
bookwyrm_egg().save(os.path.join(ITEM, "bookwyrm_spawn_egg.png"))

print("r74: textures generated.")
