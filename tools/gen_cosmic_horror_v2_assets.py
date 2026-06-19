"""
r45: Gera sprites animados + texturas pra Cosmic Horror Expansion.

Particles (4 frames cada, 16x16):
 - tentacle_writhe    : tentáculo se contorcendo (movimento orgânico)
 - vulto_shadow       : vulto humano negro com fade
 - glaring_eye_pulse  : olho que pulsa abrindo/fechando
 - cursed_pulse       : aura roxa-vermelha pulsando
 - whisper_wisp       : fumaça whispering com olhos
 - demon_glyph        : sigilo demoníaco rotativo

Items (16x16, 1 frame):
 - cursed_effigy           : boneco amaldiçoado (não solta)
 - watcher_mark            : marca rúnica vermelha
 - phantom_caller          : sino fantasma cinza
 - vulto_lens              : lente preta com olho
 - insanity_crown          : coroa torcida com tentáculos
 - tendril_sigil           : sigilo de tentáculo
 - voice_curse_amulet      : amuleto com boca aberta
 - silent_witness_cloak    : capa fragmentada (curio)
"""
from PIL import Image, ImageDraw
import os
import math
import random

ROOT = os.path.dirname(__file__)
PART_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources',
                       'assets', 'liberthia', 'textures', 'particle')
ITEM_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources',
                       'assets', 'liberthia', 'textures', 'item')
os.makedirs(PART_OUT, exist_ok=True)
os.makedirs(ITEM_OUT, exist_ok=True)

SIZE = 16
FRAMES = 4
random.seed(666)


def blank():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def save_part(name, frames):
    for i, f in enumerate(frames):
        f.save(os.path.join(PART_OUT, f'{name}_{i}.png'))
    print(f'  [OK] particle/{name}_[0-{FRAMES-1}].png')


def save_item(name, img):
    img.save(os.path.join(ITEM_OUT, f'{name}.png'))
    print(f'  [OK] item/{name}.png')


# ============================================================
# PARTICLES
# ============================================================

def gen_tentacle_writhe():
    """Tentáculo se contorcendo verticalmente. Curva senoidal."""
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        phase = f * (math.pi / 2)
        # Tentáculo de cima pra baixo com curvatura senoidal
        for y in range(2, 14):
            t = y / 14.0
            # Curva que oscila por frame
            offset = math.sin(t * math.pi * 2 + phase) * 3
            x = 7.5 + offset
            # Espessura varia (mais grosso no meio)
            thick = max(1, int(2 * (1 - abs(t - 0.5) * 2)))
            for dx in range(-thick, thick + 1):
                px = int(x + dx)
                if 0 <= px < SIZE:
                    # Cor: roxo escuro com gradient interno claro
                    if dx == 0:
                        color = (180, 80, 220, 255)
                    elif abs(dx) <= 1:
                        color = (100, 30, 140, 255)
                    else:
                        color = (50, 15, 80, 220)
                    img.putpixel((px, y), color)
        # Ventosas (pixels pulsantes ao longo)
        for sy in range(3, 13, 3):
            sx = 7 + int(math.sin(sy / 14.0 * math.pi * 2 + phase) * 3)
            if 0 <= sx < SIZE:
                img.putpixel((sx, sy), (255, 150, 200, 255))
        # Ponta com chama de olho
        tip_y = 1
        d.ellipse([6, tip_y, 9, tip_y + 2], fill=(180, 0, 60, 255))
        img.putpixel((7, tip_y + 1), (255, 100, 100, 255))
        frames.append(img)
    save_part('tentacle_writhe', frames)


def gen_vulto_shadow():
    """Vulto humano negro com fade-in/fade-out."""
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        # Alpha cycle: fade in (0-1) → full (1-2) → fade out (2-3)
        if f == 0:
            global_alpha = 100
        elif f == 1:
            global_alpha = 200
        elif f == 2:
            global_alpha = 200
        else:
            global_alpha = 80
        # Cabeça
        head = (5, 5, global_alpha)
        d.ellipse([6, 1, 10, 5], fill=(5, 5, 10, global_alpha))
        # Torso (trapezoide)
        d.polygon([(6, 5), (10, 5), (12, 14), (4, 14)],
                  fill=(10, 5, 20, global_alpha))
        # Braços pendentes
        d.line([(6, 6), (3, 12)], fill=(10, 5, 20, global_alpha), width=2)
        d.line([(10, 6), (13, 12)], fill=(10, 5, 20, global_alpha), width=2)
        # Olhos vermelhos brilhantes (sempre visíveis enquanto vulto está)
        if global_alpha > 100:
            img.putpixel((7, 3), (220, 30, 30, 255))
            img.putpixel((9, 3), (220, 30, 30, 255))
        # Distortion pixels (pixel preto aleatório em volta)
        for _ in range(3):
            ox = random.randint(0, 15)
            oy = random.randint(0, 15)
            if img.getpixel((ox, oy))[3] == 0:
                img.putpixel((ox, oy), (5, 0, 10, 80))
        frames.append(img)
    save_part('vulto_shadow', frames)


def gen_glaring_eye_pulse():
    """Olho gigantesco que abre e fecha lentamente."""
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        # Abertura varia
        opening = [3, 5, 6, 5][f]  # narrow → wide → narrow
        cx, cy = 7.5, 7.5
        # Eyeball (white)
        d.ellipse([cx - 6, cy - opening / 2, cx + 6, cy + opening / 2],
                  fill=(220, 220, 200, 255), outline=(60, 30, 30, 255))
        # Iris (red)
        d.ellipse([cx - 3, cy - opening / 3, cx + 3, cy + opening / 3],
                  fill=(180, 30, 30, 255))
        # Pupil (preto vertical, como gato)
        d.ellipse([cx - 1, cy - opening / 2 + 0.5, cx + 1, cy + opening / 2 - 0.5],
                  fill=(5, 0, 0, 255))
        # Veins (red lines crossing the white)
        for vy in [cy - 2, cy + 2]:
            d.line([(cx - 5, vy), (cx - 2, vy + 0.5)], fill=(150, 20, 20, 200), width=1)
            d.line([(cx + 5, vy), (cx + 2, vy + 0.5)], fill=(150, 20, 20, 200), width=1)
        # Tear (drop on left at frame 2)
        if f == 2:
            d.ellipse([3, 10, 4, 12], fill=(150, 0, 0, 220))
        frames.append(img)
    save_part('glaring_eye_pulse', frames)


def gen_cursed_pulse():
    """Aura roxa-vermelha pulsando — dot pattern."""
    frames = []
    for f in range(FRAMES):
        img = blank()
        cx, cy = 7.5, 7.5
        # Radial pulse with frame-shifted radius
        max_r = [3, 5, 7, 5][f]
        for y in range(SIZE):
            for x in range(SIZE):
                dx = x - cx
                dy = y - cy
                r = math.sqrt(dx * dx + dy * dy)
                if r <= max_r:
                    # Color depends on distance from center
                    t = r / max_r
                    red = int(180 - t * 100)
                    green = int(20 + t * 10)
                    blue = int(100 + t * 60)
                    alpha = int(255 * (1 - t))
                    img.putpixel((x, y), (red, green, blue, alpha))
        # Random sparkles
        random.seed(f * 13 + 7)
        for _ in range(5):
            sx = random.randint(0, 15)
            sy = random.randint(0, 15)
            if img.getpixel((sx, sy))[3] > 0:
                img.putpixel((sx, sy), (255, 100, 200, 255))
        frames.append(img)
    save_part('cursed_pulse', frames)


def gen_whisper_wisp():
    """Fumaça com pequenos olhos dentro — wispy upward motion."""
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        # Smoke blobs at varying positions per frame
        smoke_color = (60, 50, 80, 200)
        offset_y = -f  # rises by 1 pixel per frame
        for i in range(8):
            cx = 4 + i + math.sin((i + f) * 0.5) * 2
            cy = 13 - i + offset_y
            if 0 <= cy < SIZE and 0 <= cx < SIZE:
                d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=smoke_color)
        # Eyes dentro do wisp
        if f == 1 or f == 2:
            img.putpixel((6, 6 + offset_y), (200, 30, 30, 255))
            img.putpixel((9, 7 + offset_y), (200, 30, 30, 255))
        # Top wisp ascending
        d.ellipse([6, 2 + offset_y, 9, 4 + offset_y], fill=(40, 30, 60, 180))
        frames.append(img)
    save_part('whisper_wisp', frames)


def gen_demon_glyph():
    """Sigilo demoníaco rotativo (pentagram-style)."""
    frames = []
    for f in range(FRAMES):
        img = blank()
        d = ImageDraw.Draw(img)
        cx, cy = 7.5, 7.5
        # Outer circle
        d.ellipse([1, 1, 14, 14], outline=(180, 20, 20, 255))
        # Rotating star (5 points)
        rot = f * (math.pi / 8)
        points = []
        for i in range(10):
            angle = rot + i * (math.pi / 5)
            r = 5 if i % 2 == 0 else 2
            points.append((cx + math.cos(angle - math.pi / 2) * r,
                           cy + math.sin(angle - math.pi / 2) * r))
        d.polygon(points, outline=(255, 50, 50, 255))
        # Center inner symbol
        d.ellipse([6, 6, 9, 9], fill=(150, 0, 0, 255))
        d.ellipse([7, 7, 8, 8], fill=(255, 200, 0, 255))
        # Glow points at star tips
        for i in range(5):
            angle = rot + i * (math.pi * 2 / 5)
            tx = int(cx + math.cos(angle - math.pi / 2) * 5)
            ty = int(cy + math.sin(angle - math.pi / 2) * 5)
            if 0 <= tx < SIZE and 0 <= ty < SIZE:
                img.putpixel((tx, ty), (255, 220, 0, 255))
        frames.append(img)
    save_part('demon_glyph', frames)


# ============================================================
# ITEMS (single 16x16 textures)
# ============================================================

def gen_cursed_effigy():
    """Boneco amaldiçoado — silhueta humanoide com pregos."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Body — bege cinza, costurado
    d.rectangle([5, 4, 10, 12], fill=(120, 90, 60, 255), outline=(60, 40, 20, 255))
    # Head ball
    d.ellipse([5, 1, 10, 5], fill=(140, 100, 70, 255), outline=(60, 40, 20, 255))
    # Arms
    d.line([(5, 6), (2, 9)], fill=(120, 90, 60, 255), width=2)
    d.line([(10, 6), (13, 9)], fill=(120, 90, 60, 255), width=2)
    # Legs
    d.line([(7, 12), (6, 15)], fill=(120, 90, 60, 255), width=2)
    d.line([(8, 12), (9, 15)], fill=(120, 90, 60, 255), width=2)
    # Pregos — pixels pretos espetados
    for nx, ny in [(6, 6), (9, 8), (7, 10), (8, 5)]:
        img.putpixel((nx, ny), (40, 20, 20, 255))
        img.putpixel((nx, ny - 1), (200, 200, 100, 255))  # cabeça do prego
    # Costuras (linhas escuras)
    d.line([(5, 7), (10, 7)], fill=(40, 20, 10, 255), width=1)
    # Olhos vermelhos
    img.putpixel((6, 3), (220, 30, 30, 255))
    img.putpixel((9, 3), (220, 30, 30, 255))
    # Boca cosida (X)
    img.putpixel((7, 4), (40, 20, 10, 255))
    img.putpixel((8, 4), (40, 20, 10, 255))
    save_item('cursed_effigy', img)


def gen_watcher_mark():
    """Marca runica vermelha — eye-rune com tentáculos."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Background sigil (dark purple)
    d.ellipse([2, 2, 13, 13], fill=(20, 10, 30, 255), outline=(120, 40, 200, 255))
    # Central eye
    d.ellipse([5, 6, 10, 9], fill=(220, 30, 30, 255))
    d.ellipse([6, 7, 9, 8], fill=(5, 0, 0, 255))
    img.putpixel((7, 7), (255, 220, 100, 255))
    # Tentacle radiating out (4 directions)
    for dx, dy in [(0, -1), (0, 1), (-1, 0), (1, 0)]:
        for step in range(1, 5):
            x = 7 + dx * step
            y = 7 + dy * step
            if 0 <= x < SIZE and 0 <= y < SIZE:
                img.putpixel((x, y), (180, 30, 100, 255))
    # Outer rune marks (4 pixels at cardinal points)
    for px, py in [(7, 0), (7, 15), (0, 7), (15, 7)]:
        img.putpixel((px, py), (255, 80, 200, 255))
    save_item('watcher_mark', img)


def gen_phantom_caller():
    """Sino fantasma cinza com olhos."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Bell shape
    d.polygon([(4, 3), (11, 3), (12, 11), (3, 11)],
              fill=(60, 60, 80, 240), outline=(20, 20, 30, 255))
    # Bell rim
    d.line([(3, 11), (12, 11)], fill=(100, 100, 130, 255), width=1)
    # Crown of bell
    d.rectangle([6, 1, 9, 3], fill=(80, 80, 100, 255), outline=(20, 20, 30, 255))
    # Clapper inside (chain + ball)
    d.line([(7, 4), (7, 9)], fill=(40, 40, 50, 255), width=1)
    d.ellipse([6, 9, 8, 11], fill=(40, 40, 50, 255))
    # Eyes on bell surface
    img.putpixel((5, 6), (220, 30, 30, 255))
    img.putpixel((9, 6), (220, 30, 30, 255))
    # Ghost wisps escaping (top)
    for wx, wy in [(2, 0), (4, 1), (10, 0), (13, 1)]:
        img.putpixel((wx, wy), (200, 200, 220, 180))
    save_item('phantom_caller', img)


def gen_vulto_lens():
    """Lente preta com olho que vê fantasmas."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Frame metálico circular
    d.ellipse([1, 1, 14, 14], fill=(40, 40, 50, 255), outline=(120, 120, 140, 255))
    # Lens interior (preto profundo)
    d.ellipse([3, 3, 12, 12], fill=(5, 0, 15, 255))
    # Iris cosmica roxa
    d.ellipse([5, 5, 10, 10], fill=(80, 30, 140, 255))
    # Pupila vertical de gato
    d.ellipse([7, 5, 8, 10], fill=(5, 0, 0, 255))
    # Reflexo lateral (highlight)
    img.putpixel((5, 5), (200, 150, 255, 200))
    # Cabo lateral (handle)
    d.rectangle([13, 13, 15, 15], fill=(80, 60, 40, 255), outline=(40, 30, 20, 255))
    save_item('vulto_lens', img)


def gen_insanity_crown():
    """Coroa torcida com tentáculos saindo."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Crown band
    d.rectangle([2, 8, 13, 11], fill=(60, 30, 100, 255), outline=(150, 80, 220, 255))
    # Crown spikes (5 spikes)
    spikes_x = [2, 4, 7, 10, 13]
    spike_heights = [5, 7, 8, 7, 5]
    for sx, sh in zip(spikes_x, spike_heights):
        sy_top = 8 - sh
        d.polygon([(sx, 8), (sx + 1, sy_top), (sx + 2, 8)],
                  fill=(80, 40, 140, 255), outline=(150, 80, 220, 255))
    # Center jewel (red eye)
    d.ellipse([6, 9, 9, 11], fill=(220, 30, 30, 255))
    img.putpixel((7, 10), (255, 200, 100, 255))
    # Tentacles emerging from band
    for dx in [3, 6, 9, 12]:
        for dy in range(12, 16):
            offset = int(math.sin(dy * 0.8) * 1.5)
            px = dx + offset
            if 0 <= px < SIZE:
                img.putpixel((px, dy), (140, 30, 100, 255))
    save_item('insanity_crown', img)


def gen_tendril_sigil():
    """Sigilo de tentáculo — espiral com olho."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Background dark
    d.ellipse([1, 1, 14, 14], fill=(15, 5, 25, 255), outline=(120, 40, 180, 255))
    # Spiral tentacle (one outward arm)
    cx, cy = 7.5, 7.5
    for i in range(0, 12):
        angle = i * 0.5
        r = i * 0.4
        px = int(cx + math.cos(angle) * r)
        py = int(cy + math.sin(angle) * r)
        if 0 <= px < SIZE and 0 <= py < SIZE:
            img.putpixel((px, py), (200, 40, 140, 255))
            # Spreading width
            if i > 5:
                if 0 <= px + 1 < SIZE:
                    img.putpixel((px + 1, py), (160, 30, 110, 220))
    # Central eye
    d.ellipse([6, 6, 9, 9], fill=(220, 200, 50, 255))
    img.putpixel((7, 7), (5, 0, 0, 255))
    save_item('tendril_sigil', img)


def gen_voice_curse_amulet():
    """Amuleto com boca aberta gritando."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Amulet pendant (golden)
    d.ellipse([2, 4, 13, 14], fill=(180, 140, 30, 255), outline=(100, 70, 10, 255))
    # Open mouth (silver-black)
    d.ellipse([5, 6, 10, 12], fill=(20, 10, 20, 255), outline=(150, 50, 50, 255))
    # Teeth (white sharps)
    for tx in [5, 7, 9]:
        img.putpixel((tx, 8), (240, 240, 230, 255))
        img.putpixel((tx + 1, 7), (240, 240, 230, 255))
    # Tongue (red center)
    img.putpixel((7, 10), (200, 30, 30, 255))
    # Chain top
    d.line([(7, 0), (7, 4)], fill=(120, 100, 30, 255), width=1)
    img.putpixel((6, 0), (180, 140, 30, 255))
    img.putpixel((7, 0), (200, 160, 50, 255))
    img.putpixel((8, 0), (180, 140, 30, 255))
    save_item('voice_curse_amulet', img)


def gen_silent_witness_cloak():
    """Capa fragmentada com vários olhos pequenos."""
    img = blank()
    d = ImageDraw.Draw(img)
    # Cloak shape — irregular fabric
    d.polygon([(3, 2), (12, 2), (14, 12), (13, 14), (9, 14), (8, 11), (7, 14), (3, 14), (2, 11)],
              fill=(30, 20, 50, 255), outline=(80, 30, 120, 255))
    # Eyes scattered
    eye_positions = [(5, 5), (10, 4), (7, 7), (4, 9), (11, 8), (8, 11), (6, 13)]
    for ex, ey in eye_positions:
        if 0 <= ex < SIZE and 0 <= ey < SIZE:
            img.putpixel((ex, ey), (220, 220, 100, 255))
    # Tears in fabric
    for tx, ty in [(5, 6), (10, 11)]:
        img.putpixel((tx, ty), (15, 5, 25, 255))
    save_item('silent_witness_cloak', img)


# ============================================================
# RUN ALL
# ============================================================

def main():
    print('=== PARTICLES ===')
    gen_tentacle_writhe()
    gen_vulto_shadow()
    gen_glaring_eye_pulse()
    gen_cursed_pulse()
    gen_whisper_wisp()
    gen_demon_glyph()
    print('=== ITEMS ===')
    gen_cursed_effigy()
    gen_watcher_mark()
    gen_phantom_caller()
    gen_vulto_lens()
    gen_insanity_crown()
    gen_tendril_sigil()
    gen_voice_curse_amulet()
    gen_silent_witness_cloak()
    print('[OK] Done')


if __name__ == '__main__':
    main()
