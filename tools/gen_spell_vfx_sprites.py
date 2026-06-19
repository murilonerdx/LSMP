"""
r42: Gera 10 spell VFX sprite sheets animados.

Cada sprite = strip vertical de 4 frames 16×16 → PNG 16×64.
Salvo em assets/liberthia/textures/particle/spell_<name>_0.png ... _3.png
(formato Minecraft: 1 arquivo por frame, particle JSON faz a animação)

Sprites criados:
 1. fire_blast       — chama vermelho/laranja/amarelo
 2. ice_lance        — lança de gelo cyan/branco
 3. void_orb         — esfera escura com chromatic ring
 4. light_ray        — raio dourado com sparkles
 5. blood_shot       — orbe vermelho com pingo
 6. arcane_missile   — estrela 5 pontas roxa
 7. earth_spike      — espinho rochoso marrom
 8. lightning_bolt   — zigzag azul-branco
 9. shadow_dart      — dardo preto com fumaça
10. nature_thorn     — espinho verde com folhas
"""
from PIL import Image, ImageDraw
import os
import math
import random

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'particle')
os.makedirs(OUT, exist_ok=True)

SIZE = 16
FRAMES = 4
random.seed(42)


def blank():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def save_frames(name, frames):
    """Salva 4 frames como spell_<name>_0..3.png"""
    for i, f in enumerate(frames):
        f.save(os.path.join(OUT, f'spell_{name}_{i}.png'))
    print(f'  [OK] spell_{name}_[0-{FRAMES-1}].png')


def filled_circle(d, cx, cy, r, color):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)


def gradient_circle(img, cx, cy, max_r, colors):
    """colors = lista de (r, color) — pinta anéis concêntricos."""
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)
            for ring_r, color in colors:
                if r <= ring_r:
                    px[x, y] = color
                    break


# ============================================================
# 1. FIRE BLAST — chama com flickering
# ============================================================
def gen_fire_blast():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        # Núcleo amarelo claro
        cx, cy = 7.5, 7.5 + math.sin(f * 0.8) * 0.5
        # Outer flame red
        gradient_circle(img, cx, cy, 7, [
            (3, (255, 240, 180, 255)),
            (4.5, (255, 180, 50, 255)),
            (6, (220, 80, 20, 255)),
            (7, (140, 30, 10, 200)),
        ])
        # Flame "tongues" subiarondo
        for i in range(4):
            angle = f * 0.5 + i * (math.pi / 2)
            ex = cx + math.cos(angle) * 5
            ey = cy + math.sin(angle) * 5
            r = 1 + (f % 2)
            d.ellipse([ex - r, ey - r, ex + r, ey + r],
                      fill=(255, 200 - f * 20, 50, 200))
        # Pixel sparks aleatórios
        for _ in range(3):
            sx = int(cx + random.uniform(-5, 5))
            sy = int(cy + random.uniform(-5, 5))
            if 0 <= sx < SIZE and 0 <= sy < SIZE:
                img.putpixel((sx, sy), (255, 255, 200, 255))
        frames.append(img)
    save_frames('fire_blast', frames)


# ============================================================
# 2. ICE LANCE — lança de gelo
# ============================================================
def gen_ice_lance():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        # Diamond shape (lance)
        cx, cy = 7.5, 7.5
        # Sharp diamond
        d.polygon([(cx, 1), (13, cy), (cx, 14), (2, cy)],
                  fill=(150, 220, 255, 255), outline=(255, 255, 255, 255))
        # Inner brighter diamond
        d.polygon([(cx, 4), (11, cy), (cx, 11), (4, cy)],
                  fill=(220, 245, 255, 255))
        # Center white-blue glow
        d.ellipse([6, 6, 9, 9], fill=(255, 255, 255, 255))
        # Frost crystals rotating
        for i in range(3):
            angle = f * 0.6 + i * (2 * math.pi / 3)
            ex = cx + math.cos(angle) * 6.5
            ey = cy + math.sin(angle) * 6.5
            if 0 <= int(ex) < SIZE and 0 <= int(ey) < SIZE:
                img.putpixel((int(ex), int(ey)), (255, 255, 255, 255))
        frames.append(img)
    save_frames('ice_lance', frames)


# ============================================================
# 3. VOID ORB — esfera escura com chromatic ring
# ============================================================
def gen_void_orb():
    frames = []
    for f in range(FRAMES):
        img = blank()
        cx, cy = 7.5, 7.5
        # Anel chromatic shift por frame
        gradient_circle(img, cx, cy, 7, [
            (2, (5, 0, 15, 255)),   # núcleo escuro
            (3.5, (40, 5, 60, 255)),
            (5, (90, 10, 130, 255)),
            (6, (140, 20, 200, 200)),
            (7, (60, 0, 100, 100)),
        ])
        # Chromatic ring — RGB rotation
        d = ImageDraw.Draw(img)
        for i in range(8):
            angle = f * 0.4 + i * (math.pi / 4)
            r = 6 + math.sin(f * 0.5 + i) * 0.3
            ex = cx + math.cos(angle) * r
            ey = cy + math.sin(angle) * r
            # RGB shift
            c_idx = (i + f) % 3
            if c_idx == 0:
                color = (255, 50, 100, 200)
            elif c_idx == 1:
                color = (50, 200, 255, 200)
            else:
                color = (180, 50, 255, 200)
            if 0 <= int(ex) < SIZE and 0 <= int(ey) < SIZE:
                img.putpixel((int(ex), int(ey)), color)
        frames.append(img)
    save_frames('void_orb', frames)


# ============================================================
# 4. LIGHT RAY — raio dourado com sparkles
# ============================================================
def gen_light_ray():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx, cy = 7.5, 7.5
        # Star-cross light
        d.line([(cx, 1), (cx, 14)], fill=(255, 245, 180, 255), width=2)
        d.line([(1, cy), (14, cy)], fill=(255, 245, 180, 255), width=2)
        # Diagonal rays (rotating)
        rot = f * 0.4
        for angle in [rot, rot + math.pi / 2]:
            ex1 = cx + math.cos(angle + math.pi / 4) * 6
            ey1 = cy + math.sin(angle + math.pi / 4) * 6
            ex2 = cx + math.cos(angle + math.pi / 4 + math.pi) * 6
            ey2 = cy + math.sin(angle + math.pi / 4 + math.pi) * 6
            d.line([(ex1, ey1), (ex2, ey2)], fill=(255, 220, 100, 200), width=1)
        # Central blazing bright
        d.ellipse([6, 6, 9, 9], fill=(255, 255, 255, 255))
        d.ellipse([7, 7, 8, 8], fill=(255, 255, 255, 255))
        # Sparkles
        for _ in range(3 + f):
            sx = random.randint(2, 13)
            sy = random.randint(2, 13)
            img.putpixel((sx, sy), (255, 255, 200, 255))
        frames.append(img)
    save_frames('light_ray', frames)


# ============================================================
# 5. BLOOD SHOT — orbe vermelho com pingo
# ============================================================
def gen_blood_shot():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx, cy = 7.5, 7 + f * 0.2  # drips downward
        # Main red orb
        gradient_circle(img, cx, cy, 6, [
            (2, (255, 100, 100, 255)),
            (3.5, (220, 30, 30, 255)),
            (5, (160, 15, 15, 255)),
            (6, (90, 5, 5, 220)),
        ])
        # Drip below
        drip_y = cy + 5 + f * 0.5
        if drip_y < SIZE - 1:
            d.ellipse([cx - 1, drip_y, cx + 1, drip_y + 1], fill=(200, 20, 20, 255))
        # Highlight (glossy)
        d.ellipse([cx - 2, cy - 2, cx, cy], fill=(255, 180, 180, 255))
        frames.append(img)
    save_frames('blood_shot', frames)


# ============================================================
# 6. ARCANE MISSILE — estrela 5 pontas roxa
# ============================================================
def gen_arcane_missile():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx, cy = 7.5, 7.5
        # 5-pointed star — rotating
        rot = f * 0.3
        points = []
        for i in range(10):
            angle = rot + i * (math.pi / 5)
            r = 6 if i % 2 == 0 else 2.5
            points.append((cx + math.cos(angle) * r, cy + math.sin(angle) * r))
        d.polygon(points, fill=(180, 80, 255, 255), outline=(255, 180, 255, 255))
        # Center glow
        d.ellipse([6, 6, 9, 9], fill=(255, 220, 255, 255))
        # Sparkles outside
        for _ in range(2):
            sx = random.randint(0, 15)
            sy = random.randint(0, 15)
            if img.getpixel((sx, sy))[3] == 0:
                img.putpixel((sx, sy), (200, 150, 255, 200))
        frames.append(img)
    save_frames('arcane_missile', frames)


# ============================================================
# 7. EARTH SPIKE — espinho rochoso marrom
# ============================================================
def gen_earth_spike():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx = 7.5
        # Triangular spike pointing UP — grows by frame
        tip_y = 2 - f * 0.3
        base_y = 14 + f * 0.2
        d.polygon([(cx, tip_y), (12, base_y), (3, base_y)],
                  fill=(120, 80, 40, 255), outline=(80, 50, 20, 255))
        # Stone shading (lighter on left)
        d.polygon([(cx, tip_y), (cx, base_y), (3, base_y)],
                  fill=(150, 100, 60, 255))
        # Cracks
        d.line([(cx, tip_y + 2), (cx + 1, base_y - 2)], fill=(60, 30, 10, 255), width=1)
        d.line([(6, base_y - 3), (7, base_y - 1)], fill=(60, 30, 10, 255), width=1)
        # Rocks falling
        for i in range(3):
            rx = cx + math.cos(f + i * 2) * 5
            ry = base_y + 1 + f * 0.5
            if 0 <= int(rx) < SIZE and 0 <= int(ry) < SIZE:
                img.putpixel((int(rx), int(ry)), (100, 60, 30, 255))
        frames.append(img)
    save_frames('earth_spike', frames)


# ============================================================
# 8. LIGHTNING BOLT — zigzag azul-branco
# ============================================================
def gen_lightning_bolt():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        # Zigzag path — pontos com offset por frame
        random.seed(f * 7 + 31)
        path = [(7, 1)]
        y = 1
        x = 7
        while y < 15:
            y += 2
            x += random.randint(-2, 2)
            x = max(2, min(13, x))
            path.append((x, y))
        # Draw bolt: white core, blue glow
        for i in range(len(path) - 1):
            d.line([path[i], path[i + 1]], fill=(180, 220, 255, 255), width=2)
        for i in range(len(path) - 1):
            d.line([path[i], path[i + 1]], fill=(255, 255, 255, 255), width=1)
        # Branches
        for i in range(1, len(path) - 1, 2):
            bx, by = path[i]
            ex = bx + random.choice([-3, 3])
            ey = by + random.randint(0, 2)
            if 0 <= ex < SIZE and 0 <= ey < SIZE:
                d.line([(bx, by), (ex, ey)], fill=(150, 200, 255, 220), width=1)
        # Glow pixels at endpoints
        for px, py in path:
            if 0 <= px < SIZE and 0 <= py < SIZE:
                img.putpixel((px, py), (255, 255, 255, 255))
        frames.append(img)
    save_frames('lightning_bolt', frames)


# ============================================================
# 9. SHADOW DART — dardo preto com fumaça
# ============================================================
def gen_shadow_dart():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx, cy = 7.5, 7.5
        # Dart shape (diamond + tail)
        d.polygon([(cx, 2), (11, cy), (cx, 13), (4, cy)],
                  fill=(20, 10, 30, 255), outline=(60, 30, 80, 255))
        # Tail smoke (trailing back)
        for i in range(4):
            tx = cx + math.cos(f * 0.5 + i) * 0.5
            ty = 13 + i
            if ty < SIZE:
                d.ellipse([tx - 1, ty - 1, tx + 1, ty + 1],
                          fill=(40 + i * 10, 20, 60, 200 - i * 40))
        # Dark core glow (paradoxical "dark shine")
        d.ellipse([6, 6, 9, 9], fill=(80, 30, 120, 255))
        d.ellipse([7, 7, 8, 8], fill=(120, 60, 180, 255))
        # Smoke wisps around
        for i in range(2):
            sx = int(cx + math.cos(f + i * 2) * 5)
            sy = int(cy + math.sin(f + i * 2) * 5)
            if 0 <= sx < SIZE and 0 <= sy < SIZE:
                img.putpixel((sx, sy), (30, 15, 45, 180))
        frames.append(img)
    save_frames('shadow_dart', frames)


# ============================================================
# 10. NATURE THORN — espinho verde com folhas
# ============================================================
def gen_nature_thorn():
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx = 7.5
        # Vine-like vertical stem
        d.line([(cx, 1), (cx, 14)], fill=(40, 100, 30, 255), width=2)
        # Thorns
        for y in [3, 6, 9, 12]:
            side = 1 if y % 2 == 0 else -1
            tx = cx + side * 3
            ty = y + (f % 2)
            d.polygon([(cx, y), (tx, ty), (cx, y + 1)],
                      fill=(180, 50, 40, 255), outline=(120, 30, 20, 255))
        # Leaves (animated growth)
        leaf_size = 2 + f
        d.ellipse([cx - leaf_size, 5 - leaf_size // 2,
                   cx + leaf_size, 7 + leaf_size // 2],
                  fill=(60, 160, 50, 220), outline=(30, 100, 20, 255))
        d.ellipse([cx - leaf_size + 1, 5 - leaf_size // 2 + 1,
                   cx + leaf_size - 1, 7 + leaf_size // 2 - 1],
                  fill=(100, 200, 70, 200))
        # Pollen/spores
        for _ in range(2):
            px = random.randint(2, 13)
            py = random.randint(2, 13)
            img.putpixel((px, py), (200, 255, 100, 200))
        frames.append(img)
    save_frames('nature_thorn', frames)


# ============================================================
# RUN ALL
# ============================================================
def main():
    print(f'Output: {OUT}')
    print('Generating 10 spell VFX sprite sheets...')
    gen_fire_blast()
    gen_ice_lance()
    gen_void_orb()
    gen_light_ray()
    gen_blood_shot()
    gen_arcane_missile()
    gen_earth_spike()
    gen_lightning_bolt()
    gen_shadow_dart()
    gen_nature_thorn()
    print('[OK] Done - 10 sprites x 4 frames = 40 PNG files')


if __name__ == '__main__':
    main()
