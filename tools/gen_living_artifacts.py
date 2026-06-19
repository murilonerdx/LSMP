"""r47: Gera 4 texturas dos Living Artifacts.
- geometry_key: chave preta metálica com ângulos impossíveis
- low_signal: rádio quebrado preto/cinza
- pale_thread: fio branco enrolado
- mirror_fruit: fruta refletora
"""
from PIL import Image, ImageDraw
import os, math, json

ROOT = os.path.dirname(__file__)
ITEM_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                       'liberthia', 'textures', 'item')
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')
SIZE = 16


def blank():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def save_item(name, img):
    img.save(os.path.join(ITEM_OUT, f'{name}.png'))
    with open(os.path.join(MODEL_OUT, f'{name}.json'), 'w') as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"liberthia:item/{name}"}}, f, indent=2)
    print(f'[OK] {name}')


# 1. GEOMETRY KEY
def gen_geometry_key():
    img = blank()
    d = ImageDraw.Draw(img)
    # Key shaft (impossible angle — looks Z-bent)
    d.line([(3, 12), (8, 7)], fill=(200, 180, 200, 255), width=2)
    d.line([(8, 7), (12, 11)], fill=(200, 180, 200, 255), width=2)
    d.line([(12, 11), (14, 6)], fill=(200, 180, 200, 255), width=2)
    # Key bow (round head — impossible square)
    d.rectangle([1, 11, 5, 14], fill=(80, 60, 100, 255), outline=(40, 30, 60, 255))
    d.line([(2, 11), (4, 14)], fill=(160, 120, 200, 255), width=1)
    # Floating runes on shaft
    img.putpixel((7, 7), (200, 80, 255, 255))
    img.putpixel((11, 11), (200, 80, 255, 255))
    img.putpixel((13, 6), (200, 80, 255, 255))
    # Cosmic glow at tip
    img.putpixel((14, 5), (255, 200, 255, 255))
    img.putpixel((15, 5), (255, 200, 255, 255))
    save_item('geometry_key', img)


# 2. LOW SIGNAL
def gen_low_signal():
    img = blank()
    d = ImageDraw.Draw(img)
    # Radio body (dark cracked plastic)
    d.rectangle([2, 5, 13, 14], fill=(30, 30, 35, 255), outline=(10, 10, 12, 255))
    # Speaker grill (lines)
    for y in range(7, 12, 2):
        for x in range(3, 8):
            img.putpixel((x, y), (15, 15, 18, 255))
    # Frequency display (orange-red LED)
    d.rectangle([9, 7, 12, 10], fill=(80, 30, 10, 255), outline=(40, 15, 5, 255))
    d.line([(10, 8), (11, 9)], fill=(255, 100, 50, 255), width=1)
    # Antenna broken (jagged)
    d.line([(6, 5), (5, 1)], fill=(60, 60, 70, 255), width=1)
    d.line([(5, 1), (7, 3)], fill=(60, 60, 70, 255), width=1)
    # Cracks
    d.line([(2, 9), (4, 11)], fill=(5, 5, 8, 255), width=1)
    d.line([(11, 12), (13, 13)], fill=(5, 5, 8, 255), width=1)
    # Static pixels (random scattered)
    for px, py in [(3, 6), (8, 13), (12, 6), (4, 14)]:
        img.putpixel((px, py), (200, 200, 200, 255))
    save_item('low_signal', img)


# 3. PALE THREAD
def gen_pale_thread():
    img = blank()
    d = ImageDraw.Draw(img)
    # Coiled thread — concentric ovals
    d.ellipse([3, 5, 12, 11], outline=(240, 240, 230, 255), width=1)
    d.ellipse([5, 6, 10, 10], outline=(220, 220, 210, 255), width=1)
    d.ellipse([6, 7, 9, 9], outline=(200, 200, 190, 255), width=1)
    # Free end dangling
    d.line([(12, 8), (14, 13)], fill=(230, 230, 220, 255), width=1)
    d.line([(14, 13), (12, 15)], fill=(220, 220, 210, 255), width=1)
    # Slight glow
    img.putpixel((7, 8), (255, 255, 250, 255))
    img.putpixel((8, 8), (255, 255, 250, 255))
    # Cold shimmer points
    img.putpixel((3, 5), (200, 220, 255, 255))
    img.putpixel((12, 11), (200, 220, 255, 255))
    save_item('pale_thread', img)


# 4. MIRROR FRUIT
def gen_mirror_fruit():
    img = blank()
    d = ImageDraw.Daw if hasattr(ImageDraw, 'Daw') else ImageDraw.Draw
    d = ImageDraw.Draw(img)
    # Fruit body (apple-like) reflective silver
    d.ellipse([3, 4, 12, 13], fill=(160, 180, 200, 255), outline=(80, 90, 110, 255))
    # Mirror reflection highlight (impossible)
    d.ellipse([5, 5, 8, 8], fill=(220, 230, 240, 255))
    # Distorted reflection — small dark spots showing "wrong world"
    img.putpixel((6, 6), (5, 0, 10, 255))
    img.putpixel((9, 9), (5, 0, 10, 255))
    img.putpixel((10, 7), (40, 5, 80, 255))
    # Stem
    d.rectangle([7, 1, 8, 4], fill=(80, 60, 40, 255))
    # Leaf
    d.polygon([(8, 3), (11, 1), (12, 3)], fill=(50, 100, 60, 255), outline=(20, 60, 30, 255))
    # Cracks on fruit (mirror-like)
    d.line([(4, 8), (6, 10)], fill=(80, 100, 120, 255), width=1)
    d.line([(11, 6), (13, 9)], fill=(80, 100, 120, 255), width=1)
    save_item('mirror_fruit', img)


def main():
    print('Generating 4 Living Artifact textures...')
    gen_geometry_key()
    gen_low_signal()
    gen_pale_thread()
    gen_mirror_fruit()
    print('[OK] Done')


if __name__ == '__main__':
    main()
