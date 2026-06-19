"""r48: Gera textura do Reflection Seed — semente metálica preta reflectiva."""
from PIL import Image, ImageDraw
import os, json, math

ROOT = os.path.dirname(__file__)
ITEM_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                       'liberthia', 'textures', 'item')
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')

SIZE = 16
img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Outer void halo
for y in range(SIZE):
    for x in range(SIZE):
        dx = x - 7.5
        dy = y - 7.5
        r = math.sqrt(dx*dx + dy*dy)
        if r <= 7.5:
            if r >= 6.5:
                img.putpixel((x, y), (15, 5, 25, 180))
            elif r >= 5:
                img.putpixel((x, y), (40, 20, 70, 255))
            elif r >= 3.5:
                img.putpixel((x, y), (20, 10, 40, 255))
            elif r >= 2:
                # Inner mirror surface — gradient purple to silver
                t = (r - 2) / 1.5
                gr = int(120 - t * 60)
                gg = int(80 - t * 40)
                gb = int(160 - t * 30)
                img.putpixel((x, y), (gr, gg, gb, 255))
            else:
                # Core (light cosmic energy)
                img.putpixel((x, y), (220, 200, 255, 255))

# Reflection sheen — diagonal line
d.line([(5, 4), (4, 6)], fill=(255, 255, 255, 220), width=1)
d.line([(11, 11), (12, 9)], fill=(180, 180, 220, 200), width=1)

# Tiny crack patterns (impossible geometry)
img.putpixel((6, 9), (10, 0, 20, 255))
img.putpixel((9, 6), (10, 0, 20, 255))
img.putpixel((10, 10), (10, 0, 20, 255))

# Floating particle around (cosmic glow)
img.putpixel((1, 7), (180, 80, 220, 200))
img.putpixel((14, 8), (180, 80, 220, 200))
img.putpixel((7, 1), (180, 80, 220, 200))

img.save(os.path.join(ITEM_OUT, 'reflection_seed.png'))
print('[OK] item/reflection_seed.png')

with open(os.path.join(MODEL_OUT, 'reflection_seed.json'), 'w') as f:
    json.dump({"parent": "minecraft:item/generated",
               "textures": {"layer0": "liberthia:item/reflection_seed"}}, f, indent=2)
print('[OK] models/item/reflection_seed.json')
