"""Procedural textures for Alquimia de Sangue (Fase 3)."""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
ITEM = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/item")
BLOCK = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/block")
for d in (ITEM, BLOCK):
    os.makedirs(d, exist_ok=True)


def blood_vial(filled):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # vial body
    d.rectangle([5, 6, 10, 13], fill=(220, 220, 230, 255), outline=(160, 160, 170, 255))
    d.rectangle([6, 4, 9, 5], fill=(180, 180, 190, 255))  # neck
    # cork or liquid
    if filled:
        d.rectangle([6, 7, 9, 12], fill=(180, 20, 20, 255))
        d.point([(7, 8)], fill=(255, 100, 100, 255))
        d.rectangle([6, 2, 9, 3], fill=(120, 80, 50, 255))  # cork
        name = "blood_vial_filled.png"
    else:
        name = "blood_vial.png"
    img.save(os.path.join(ITEM, name))


def congealed_blood():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # clot lump
    d.ellipse([3, 4, 12, 12], fill=(70, 10, 10, 255))
    d.ellipse([5, 6, 10, 10], fill=(140, 20, 20, 255))
    d.point([(7, 7)], fill=(200, 50, 50, 255))
    img.save(os.path.join(ITEM, "congealed_blood.png"))


def flesh_thread():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # wound string diagonal
    for i in range(12):
        x = 2 + i
        y = 13 - i
        d.point([(x, y)], fill=(160, 60, 60, 255))
        if i % 2 == 0:
            d.point([(x, y - 1)], fill=(120, 30, 30, 255))
    img.save(os.path.join(ITEM, "flesh_thread.png"))


def cauldron_block():
    # Iron rim + bloody interior visible. We'll paint one face.
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # dark iron ring
    d.rectangle([0, 0, 15, 15], fill=(60, 60, 65, 255))
    d.rectangle([1, 1, 14, 14], fill=(90, 90, 95, 255))
    # blood interior
    d.rectangle([3, 3, 12, 12], fill=(110, 15, 15, 255))
    d.rectangle([4, 4, 11, 11], fill=(160, 25, 25, 255))
    d.point([(6, 6), (9, 9)], fill=(200, 60, 60, 255))
    # rivets
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14), (8, 1), (1, 8), (14, 8), (8, 14)]:
        d.point([(x, y)], fill=(40, 40, 40, 255))
    img.save(os.path.join(BLOCK, "blood_cauldron_side.png"))
    # top: more visible blood pool
    top = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    td = ImageDraw.Draw(top)
    td.rectangle([0, 0, 15, 15], fill=(50, 50, 55, 255))
    td.rectangle([1, 1, 14, 14], fill=(30, 30, 35, 255))
    td.ellipse([2, 2, 13, 13], fill=(120, 15, 15, 255))
    td.ellipse([4, 4, 11, 11], fill=(180, 30, 30, 255))
    td.ellipse([6, 6, 9, 9], fill=(230, 60, 60, 255))
    top.save(os.path.join(BLOCK, "blood_cauldron_top.png"))
    # bottom
    bot = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bot)
    bd.rectangle([0, 0, 15, 15], fill=(40, 40, 45, 255))
    bd.rectangle([2, 2, 13, 13], fill=(60, 60, 65, 255))
    bot.save(os.path.join(BLOCK, "blood_cauldron_bottom.png"))


blood_vial(False)
blood_vial(True)
congealed_blood()
flesh_thread()
cauldron_block()
print("alchemy textures done.")
