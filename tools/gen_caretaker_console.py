"""r50: Gera textura do Caretaker Console — tablet/monitor metálico preto."""
from PIL import Image, ImageDraw
import os, json

ROOT = os.path.dirname(__file__)
ITEM_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                       'liberthia', 'textures', 'item')
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')

SIZE = 16
img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Frame externo (preto metálico)
d.rectangle([1, 2, 14, 13], fill=(15, 12, 20, 255), outline=(80, 60, 100, 255))
# Tela interna (escura com brilho)
d.rectangle([2, 3, 13, 11], fill=(20, 10, 40, 255), outline=(60, 30, 80, 255))
# "Linhas de texto" fake na tela (verde-roxo glow)
d.line([(3, 5), (12, 5)], fill=(140, 80, 200, 255), width=1)
d.line([(3, 7), (10, 7)], fill=(140, 80, 200, 255), width=1)
d.line([(3, 9), (11, 9)], fill=(140, 80, 200, 255), width=1)
# Cursor blink (red pixel)
img.putpixel((11, 9), (220, 30, 30, 255))
# LEDs no rodapé
img.putpixel((4, 13), (50, 220, 50, 255))     # green LED on
img.putpixel((7, 13), (220, 30, 30, 255))     # red LED
img.putpixel((10, 13), (220, 200, 30, 255))   # yellow LED
# Antena no topo
d.line([(7, 0), (7, 2)], fill=(80, 60, 100, 255), width=1)
img.putpixel((6, 0), (200, 80, 220, 255))
img.putpixel((7, 0), (180, 70, 200, 255))
img.putpixel((8, 0), (200, 80, 220, 255))
# Reflexo no canto da tela
img.putpixel((3, 4), (200, 150, 220, 200))
img.putpixel((12, 10), (200, 150, 220, 200))
# Botoes laterais
img.putpixel((0, 5), (50, 40, 60, 255))
img.putpixel((0, 8), (50, 40, 60, 255))
img.putpixel((15, 5), (50, 40, 60, 255))
img.putpixel((15, 8), (50, 40, 60, 255))

img.save(os.path.join(ITEM_OUT, 'caretaker_console.png'))
print('[OK] item/caretaker_console.png')

with open(os.path.join(MODEL_OUT, 'caretaker_console.json'), 'w') as f:
    json.dump({"parent": "minecraft:item/generated",
               "textures": {"layer0": "liberthia:item/caretaker_console"}}, f, indent=2)
print('[OK] models/item/caretaker_console.json')
