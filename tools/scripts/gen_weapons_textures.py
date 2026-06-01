"""Fase 4 textures."""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
ITEM = os.path.join(ROOT, "src/main/resources/assets/liberthia/textures/item")
os.makedirs(ITEM, exist_ok=True)


def hemomancer_staff():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # dark wooden shaft diagonal
    for i in range(12):
        x = 2 + i
        y = 14 - i
        d.point([(x, y)], fill=(40, 20, 15))
        d.point([(x, y - 1)], fill=(60, 30, 20))
    # spiral wrap
    for i in range(6):
        x = 4 + i
        y = 12 - i
        d.point([(x, y)], fill=(90, 20, 20))
    # orb at top
    d.ellipse([10, 1, 15, 6], fill=(140, 20, 20), outline=(200, 40, 40))
    d.point([(12, 3)], fill=(255, 120, 120))
    img.save(os.path.join(ITEM, "hemomancer_staff.png"))


def blood_bow():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # arc (bow body)
    d.arc([2, 1, 13, 14], start=270, end=90, fill=(80, 20, 20), width=2)
    # string
    d.line([(12, 2), (12, 13)], fill=(200, 200, 200))
    # handle
    d.line([(11, 6), (11, 9)], fill=(40, 10, 10))
    # blood drip on bottom
    d.point([(4, 13)], fill=(200, 30, 30))
    d.point([(4, 14)], fill=(140, 20, 20))
    img.save(os.path.join(ITEM, "blood_bow.png"))


def blood_ritual_dagger():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # handle
    d.rectangle([2, 11, 5, 14], fill=(50, 20, 10))
    # guard
    d.rectangle([1, 10, 6, 10], fill=(120, 120, 130))
    # blade
    for i in range(9):
        d.point([(5 + i, 9 - i)], fill=(200, 200, 210))
        d.point([(6 + i, 9 - i)], fill=(150, 150, 160))
    # blood channel
    for i in range(6):
        d.point([(6 + i, 8 - i)], fill=(160, 30, 30))
    img.save(os.path.join(ITEM, "blood_ritual_dagger.png"))


def blood_pact_amulet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # chain
    for i, x in enumerate([4, 6, 8, 10, 12]):
        d.point([(x, 2 + (i % 2))], fill=(180, 180, 180))
    # pendant frame
    d.polygon([(8, 4), (13, 9), (8, 14), (3, 9)], outline=(180, 180, 180))
    # heart core
    d.ellipse([5, 7, 8, 10], fill=(180, 30, 30))
    d.ellipse([8, 7, 11, 10], fill=(180, 30, 30))
    d.polygon([(5, 9), (8, 12), (11, 9)], fill=(180, 30, 30))
    d.point([(7, 8)], fill=(255, 100, 100))
    img.save(os.path.join(ITEM, "blood_pact_amulet.png"))


hemomancer_staff()
blood_bow()
blood_ritual_dagger()
blood_pact_amulet()
print("weapons textures done.")
