"""r51: Gera 10 textures dos Admin Cosmic Artifacts."""
from PIL import Image, ImageDraw
import os, json, math, random

ROOT = os.path.dirname(__file__)
ITEM_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                       'liberthia', 'textures', 'item')
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')

SIZE = 16
random.seed(666)


def blank(): return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def save_item(name, img):
    img.save(os.path.join(ITEM_OUT, f'{name}.png'))
    with open(os.path.join(MODEL_OUT, f'{name}.json'), 'w') as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"liberthia:item/{name}"}}, f, indent=2)
    print(f'[OK] {name}')


# ===== 1. BLACK VEIL — cloth fragment with stars =====
def black_veil():
    img = blank()
    d = ImageDraw.Draw(img)
    # Wavy cloth shape
    d.polygon([(2, 3), (5, 1), (8, 2), (11, 1), (14, 3),
               (13, 14), (10, 13), (7, 14), (4, 13), (3, 14)],
              fill=(8, 5, 15, 255), outline=(40, 30, 80, 255))
    # Inner darker
    d.polygon([(4, 5), (8, 4), (12, 5), (12, 12), (8, 12), (4, 12)],
              fill=(2, 0, 5, 255))
    # Stars inside
    for sx, sy in [(5, 6), (7, 8), (9, 7), (11, 9), (6, 10), (10, 11)]:
        img.putpixel((sx, sy), (220, 200, 255, 255))
    # Emissive edge
    for ex, ey in [(2, 3), (14, 3), (3, 14), (14, 14)]:
        img.putpixel((ex, ey), (160, 80, 200, 255))
    # Ash particle outside
    img.putpixel((0, 7), (60, 40, 90, 180))
    img.putpixel((15, 6), (60, 40, 90, 180))
    save_item('black_veil', img)


# ===== 2. TENDRIL CROWN — flesh tentacles =====
def tendril_crown():
    img = blank()
    d = ImageDraw.Draw(img)
    # Crown band (black flesh)
    d.rectangle([2, 9, 13, 12], fill=(40, 5, 20, 255), outline=(120, 30, 80, 255))
    # 5 tendrils rising
    for i, sx in enumerate([3, 6, 8, 10, 12]):
        sy_top = 1 + (i % 2)
        d.line([(sx, 9), (sx, sy_top)], fill=(60, 10, 30, 255), width=2)
        # Wiggle dots
        for dy in range(sy_top, 9, 2):
            ox = 1 if (dy + i) % 2 == 0 else -1
            if 0 <= sx + ox < SIZE:
                img.putpixel((sx + ox, dy), (180, 50, 120, 255))
    # Eyes embedded
    img.putpixel((5, 10), (220, 30, 30, 255))
    img.putpixel((10, 10), (220, 30, 30, 255))
    img.putpixel((7, 11), (220, 30, 30, 255))
    # Purple veins glowing
    d.line([(2, 11), (13, 11)], fill=(140, 30, 200, 255), width=1)
    # Wet reflection
    img.putpixel((4, 9), (255, 150, 200, 220))
    img.putpixel((11, 9), (255, 150, 200, 220))
    # Suckers
    img.putpixel((3, 6), (200, 50, 100, 255))
    img.putpixel((12, 6), (200, 50, 100, 255))
    save_item('tendril_crown', img)


# ===== 3. FALSE SUN — burning black sphere =====
def false_sun():
    img = blank()
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r <= 7:
                if r <= 2:
                    img.putpixel((x, y), (255, 100, 50, 255))  # core
                elif r <= 3.5:
                    img.putpixel((x, y), (180, 30, 20, 255))
                elif r <= 5:
                    img.putpixel((x, y), (60, 10, 10, 255))
                elif r <= 6.5:
                    img.putpixel((x, y), (15, 5, 8, 255))  # dark corona
                else:
                    img.putpixel((x, y), (40, 20, 30, 180))  # outer halo
    # Orbiting debris
    d = ImageDraw.Draw(img)
    for angle, r in [(0.3, 5.5), (2.1, 6), (4.0, 5.8)]:
        px = int(cx + math.cos(angle) * r)
        py = int(cy + math.sin(angle) * r)
        if 0 <= px < SIZE and 0 <= py < SIZE:
            img.putpixel((px, py), (255, 200, 100, 255))
    # Distortion edges
    img.putpixel((1, 7), (80, 30, 50, 200))
    img.putpixel((14, 8), (80, 30, 50, 200))
    save_item('false_sun', img)


# ===== 4. MIRROR PULSE — liquid metal =====
def mirror_pulse():
    img = blank()
    d = ImageDraw.Draw(img)
    # Octagonal mirror frame
    d.polygon([(4, 1), (11, 1), (15, 5), (15, 10), (11, 14),
               (4, 14), (1, 10), (1, 5)],
              fill=(60, 70, 90, 255), outline=(160, 180, 220, 255))
    # Liquid surface (chrome)
    d.polygon([(5, 3), (10, 3), (12, 5), (12, 10), (10, 12),
               (5, 12), (3, 10), (3, 5)],
              fill=(180, 200, 230, 255))
    # Inner reflection — distorted player face
    img.putpixel((7, 6), (40, 20, 60, 255))  # eye 1
    img.putpixel((9, 6), (40, 20, 60, 255))  # eye 2
    d.line([(6, 9), (10, 9)], fill=(80, 40, 100, 255), width=1)  # mouth
    # Highlights
    d.line([(4, 4), (5, 5)], fill=(255, 255, 255, 255), width=1)
    d.line([(11, 11), (10, 10)], fill=(255, 255, 255, 200), width=1)
    # Distortion cracks
    d.line([(7, 5), (8, 9)], fill=(120, 140, 180, 200), width=1)
    save_item('mirror_pulse', img)


# ===== 5. SILENT BELL — obsidian metal =====
def silent_bell():
    img = blank()
    d = ImageDraw.Draw(img)
    # Top spike
    d.line([(7, 0), (8, 0)], fill=(80, 70, 100, 255), width=1)
    d.line([(7, 1), (8, 1)], fill=(60, 50, 80, 255), width=1)
    # Bell body
    d.polygon([(4, 2), (11, 2), (13, 11), (2, 11)],
              fill=(15, 10, 25, 255), outline=(80, 70, 100, 255))
    # Void cracks (purple)
    d.line([(6, 4), (5, 9)], fill=(180, 30, 200, 255), width=1)
    d.line([(10, 5), (11, 10)], fill=(180, 30, 200, 255), width=1)
    d.line([(8, 7), (8, 10)], fill=(180, 30, 200, 255), width=1)
    # Runes (glowing dots)
    for rx, ry in [(4, 5), (10, 7), (5, 9), (11, 9)]:
        img.putpixel((rx, ry), (200, 100, 220, 255))
    # Rim
    d.line([(2, 11), (13, 11)], fill=(120, 110, 140, 255), width=1)
    d.line([(2, 12), (13, 12)], fill=(15, 10, 25, 255), width=1)
    # NO clapper (silent) — empty interior dark
    d.rectangle([6, 12, 9, 14], fill=(5, 0, 10, 255))
    # Floating dust outside
    img.putpixel((0, 6), (140, 100, 180, 150))
    img.putpixel((15, 8), (140, 100, 180, 150))
    img.putpixel((1, 14), (140, 100, 180, 150))
    save_item('silent_bell', img)


# ===== 6. OPEN EYE — giant eye relic =====
def open_eye():
    img = blank()
    d = ImageDraw.Draw(img)
    # Outer flesh socket
    d.ellipse([1, 4, 14, 12], fill=(80, 20, 30, 255), outline=(40, 10, 15, 255))
    # White eyeball with veins
    d.ellipse([2, 5, 13, 11], fill=(230, 220, 210, 255))
    # Red veins
    d.line([(3, 6), (5, 7)], fill=(180, 30, 30, 220), width=1)
    d.line([(11, 6), (10, 7)], fill=(180, 30, 30, 220), width=1)
    d.line([(3, 10), (5, 9)], fill=(180, 30, 30, 220), width=1)
    d.line([(11, 10), (10, 9)], fill=(180, 30, 30, 220), width=1)
    # Iris (deep purple-red)
    d.ellipse([5, 6, 10, 10], fill=(140, 30, 50, 255))
    # Pupil
    d.ellipse([6, 7, 9, 9], fill=(0, 0, 0, 255))
    # Bright reflection
    img.putpixel((6, 7), (255, 255, 255, 255))
    # Eyelid hint at edges
    d.line([(2, 8), (1, 8)], fill=(60, 10, 15, 255), width=1)
    d.line([(13, 8), (14, 8)], fill=(60, 10, 15, 255), width=1)
    # Tear
    img.putpixel((4, 11), (150, 0, 0, 255))
    img.putpixel((4, 12), (200, 30, 30, 255))
    save_item('open_eye', img)


# ===== 7. THREAD OF DISTANCE — white emissive fiber =====
def thread_of_distance():
    img = blank()
    d = ImageDraw.Draw(img)
    # Single thread curving impossibly
    # Path: (1, 3) → (5, 5) → (3, 9) → (8, 7) → (6, 12) → (12, 10) → (10, 14)
    path = [(1, 3), (5, 5), (3, 9), (8, 7), (6, 12), (12, 10), (10, 14)]
    for i in range(len(path) - 1):
        d.line([path[i], path[i + 1]], fill=(240, 240, 220, 255), width=1)
    # Bright glow points at vertices
    for px, py in path:
        if 0 <= px < SIZE and 0 <= py < SIZE:
            img.putpixel((px, py), (255, 255, 255, 255))
    # Emissive halo (soft yellow pixels around)
    for px, py in path:
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = px + dx, py + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE:
                if img.getpixel((nx, ny))[3] == 0:
                    img.putpixel((nx, ny), (200, 200, 180, 100))
    # Ghost duplicates (subtle alpha threads)
    d.line([(2, 4), (6, 6)], fill=(180, 180, 200, 80), width=1)
    d.line([(7, 11), (11, 11)], fill=(180, 180, 200, 80), width=1)
    save_item('thread_of_distance', img)


# ===== 8. FLESH SIGNAL — bone-flesh radio =====
def flesh_signal():
    img = blank()
    d = ImageDraw.Draw(img)
    # Bone frame
    d.rectangle([2, 4, 13, 13], fill=(220, 200, 170, 255), outline=(140, 110, 80, 255))
    # Flesh growths on edges
    d.line([(2, 5), (1, 7)], fill=(140, 30, 60, 255), width=1)
    d.line([(13, 5), (14, 7)], fill=(140, 30, 60, 255), width=1)
    img.putpixel((1, 8), (180, 50, 80, 255))
    img.putpixel((14, 8), (180, 50, 80, 255))
    # Speaker (breathing flesh)
    d.ellipse([3, 6, 7, 10], fill=(120, 30, 50, 255), outline=(60, 10, 20, 255))
    # Mouth/speaker hole
    d.ellipse([4, 7, 6, 9], fill=(20, 0, 10, 255))
    img.putpixel((5, 8), (255, 100, 100, 255))  # tongue
    # Frequency dial (right)
    d.ellipse([9, 6, 12, 9], fill=(40, 20, 30, 255), outline=(180, 50, 80, 255))
    # Needle
    d.line([(11, 8), (12, 6)], fill=(255, 50, 50, 255), width=1)
    # Bone antenna with flesh tips
    d.line([(8, 0), (8, 4)], fill=(220, 200, 170, 255), width=1)
    img.putpixel((7, 0), (180, 50, 80, 255))  # flesh growth top
    img.putpixel((8, 0), (200, 70, 100, 255))
    img.putpixel((9, 0), (180, 50, 80, 255))
    # LEDs blinking
    img.putpixel((4, 12), (220, 30, 30, 255))
    img.putpixel((10, 12), (220, 30, 30, 255))
    # Vein lines
    d.line([(3, 11), (12, 11)], fill=(140, 30, 60, 255), width=1)
    save_item('flesh_signal', img)


# ===== 9. DEEP WATER — black ocean orb =====
def deep_water():
    img = blank()
    d = ImageDraw.Draw(img)
    cx, cy = 7.5, 7.5
    # Outer glass orb
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            if r <= 7:
                if r >= 6.5:
                    img.putpixel((x, y), (40, 60, 80, 220))
                elif r >= 5:
                    img.putpixel((x, y), (10, 20, 40, 255))  # dark water
                elif r >= 3:
                    img.putpixel((x, y), (5, 15, 30, 255))   # deeper
                elif r >= 1:
                    img.putpixel((x, y), (0, 5, 15, 255))    # abyssal
                else:
                    img.putpixel((x, y), (0, 0, 5, 255))     # void core
    # Bubbles
    for bx, by in [(5, 5), (10, 7), (8, 10), (6, 11)]:
        if img.getpixel((bx, by))[3] > 0:
            img.putpixel((bx, by), (180, 200, 220, 220))
    # Internal shadow shape (eel/whale silhouette)
    d.line([(4, 9), (8, 8)], fill=(2, 2, 8, 255), width=1)
    d.line([(8, 8), (11, 9)], fill=(2, 2, 8, 255), width=1)
    # Deep-sea glow (tiny bioluminescence)
    img.putpixel((6, 7), (60, 180, 200, 255))
    img.putpixel((10, 8), (60, 180, 200, 255))
    # Reflection on outer glass
    img.putpixel((4, 4), (200, 220, 240, 255))
    img.putpixel((11, 11), (180, 200, 220, 180))
    save_item('deep_water', img)


# ===== 10. AUDIENCE MARK — cosmic sigil =====
def audience_mark():
    img = blank()
    d = ImageDraw.Draw(img)
    cx, cy = 7.5, 7.5
    # Pentagram outer
    points = []
    for i in range(10):
        angle = -math.pi / 2 + i * (math.pi / 5)
        r = 6 if i % 2 == 0 else 2.5
        points.append((cx + math.cos(angle) * r, cy + math.sin(angle) * r))
    d.polygon(points, outline=(200, 30, 50, 255), fill=(20, 0, 30, 220))
    # Central eye-shape
    d.ellipse([5, 6, 10, 9], fill=(220, 200, 50, 255))
    d.ellipse([6, 7, 9, 8], fill=(20, 0, 0, 255))
    img.putpixel((7, 7), (255, 220, 100, 255))
    # Tip glow points
    for i in range(5):
        angle = -math.pi / 2 + i * (math.pi * 2 / 5)
        tx = int(cx + math.cos(angle) * 6)
        ty = int(cy + math.sin(angle) * 6)
        if 0 <= tx < SIZE and 0 <= ty < SIZE:
            img.putpixel((tx, ty), (255, 255, 0, 255))
    # Reality distortion (cosmic ash)
    for px, py in [(0, 2), (15, 4), (1, 14), (14, 13)]:
        img.putpixel((px, py), (100, 30, 150, 180))
    # Surrounding glyph dots
    for ax in range(0, SIZE, 4):
        if img.getpixel((ax, 0))[3] == 0:
            img.putpixel((ax, 0), (140, 40, 180, 200))
    save_item('audience_mark', img)


def main():
    print('Generating 10 Admin Artifact textures...')
    black_veil()
    tendril_crown()
    false_sun()
    mirror_pulse()
    silent_bell()
    open_eye()
    thread_of_distance()
    flesh_signal()
    deep_water()
    audience_mark()
    print('[OK] Done')


if __name__ == '__main__':
    main()
