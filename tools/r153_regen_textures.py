"""r153: Regenera magic_fire (TUDO transparent menos a chama) + candle (totalmente do zero).

Os arquivos anteriores tinham probemas: magic_fire teve alpha correto mas o user achou ainda
errado, e candle ficou tiny demais com cores erradas.

Aqui:
- magic_fire: chama OBVIA + glow halo + tons brilhantes
- candle: 16x16 com candle vela bem maior, cera + pavio + chama
"""
import random
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path("src/main/resources/assets/liberthia/textures/block")

# ════════════════════════════════════════════════════════════════════════
# MAGIC FIRE — chamas grandes coloridas, glow brilhante, fundo 100% transparente
# ════════════════════════════════════════════════════════════════════════

FIRE_VARIANTS = [
    ("magic_fire_fire",      (255, 80, 0),    (255, 220, 80),  (200, 30, 0)),
    ("magic_fire_ice",       (130, 220, 255), (240, 255, 255), (60, 110, 200)),
    ("magic_fire_lightning", (255, 255, 80),  (255, 255, 255), (220, 180, 30)),
    ("magic_fire_blood",     (190, 30, 30),   (255, 100, 100), (90, 5, 5)),
    ("magic_fire_eldritch",  (170, 60, 220),  (230, 150, 255), (90, 20, 130)),
    ("magic_fire_holy",      (255, 240, 130), (255, 255, 230), (220, 170, 30)),
    ("magic_fire_nature",    (90, 200, 60),   (200, 255, 130), (40, 110, 30)),
]

def make_fire_texture(name, mid, bright, dark):
    random.seed(hash(name))
    W, H = 16, 16
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # Pinta a chama: forma de "flame" (mais larga no centro-baixo, afina em cima)
    # Linhas Y de 4 a 15 (chama tem 12 linhas de altura)
    for y in range(3, 16):
        t = (y - 3) / 12.0  # 0..1 (base..topo)
        half_w = max(1, int(6 * (1 - t * 0.85)))  # 6 → 1
        cx = 8

        for x in range(cx - half_w, cx + half_w + 1):
            if x < 0 or x >= W: continue
            dist = abs(x - cx) / max(1, half_w)

            # Pixel pode ser bright/mid/dark dependendo da posição
            r = random.random()
            if dist < 0.25:
                color = bright + (255,)
            elif dist < 0.7:
                color = mid + (255,)
            else:
                color = dark + (255,)

            # Skip alguns no topo pra efeito flicker
            if t > 0.7 and r < 0.4:
                continue

            img.putpixel((x, y), color)

    # 6 sparks aleatórios em volta — extra ambient
    for _ in range(6):
        sx = random.randint(2, 13)
        sy = random.randint(2, 14)
        if img.getpixel((sx, sy))[3] == 0:
            img.putpixel((sx, sy), bright + (255,))

    return img

print("=== Magic Fire textures ===")
for name, mid, bright, dark in FIRE_VARIANTS:
    path = OUT / f"{name}.png"
    img = make_fire_texture(name, mid, bright, dark)
    img.save(path)
    a = sum(1 for y in range(16) for x in range(16) if img.getpixel((x, y))[3] > 0)
    print(f"  + {name}.png ({a} opaque pixels)")

# ════════════════════════════════════════════════════════════════════════
# CANDLE OCCULT — vela GRANDE bem visível, com cera + pavio + chama (lit)
# ════════════════════════════════════════════════════════════════════════

# (id, wax_color, lit?)
CANDLES = [
    ("candle_occult_white",      (240, 240, 230), False),
    ("candle_occult_white_lit",  (240, 240, 230), True),
    ("candle_occult_red",        (180, 30, 30),   False),
    ("candle_occult_red_lit",    (180, 30, 30),   True),
    ("candle_occult_purple",     (110, 50, 160),  False),
    ("candle_occult_purple_lit", (110, 50, 160),  True),
    ("candle_occult_golden",     (220, 180, 40),  False),
    ("candle_occult_golden_lit", (220, 180, 40),  True),
    ("candle_occult_black",      (40, 40, 50),    False),
    ("candle_occult_black_lit",  (40, 40, 50),    True),
]

def make_candle_texture(name, wax_color, lit):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Atlas style — 16x16 mas só preenche a região do candle (4 wide x 10 tall, center)
    # Region: x=6-10, y=4-14 (corpo da vela)
    wax_dark = tuple(max(0, c - 50) for c in wax_color)
    wax_light = tuple(min(255, c + 40) for c in wax_color)

    # Corpo da vela (4 wide x 10 tall)
    body = [
        (6, 4, 9, 14),  # x0, y0, x1, y1 (inclusive)
    ]
    for x0, y0, x1, y1 in body:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                # Lado esquerdo = dark (sombra), centro = light, direito = mid
                if x == x0:
                    img.putpixel((x, y), wax_dark + (255,))
                elif x == x1:
                    img.putpixel((x, y), wax_dark + (255,))
                elif x == x0 + 1:
                    img.putpixel((x, y), wax_light + (255,))
                else:
                    img.putpixel((x, y), wax_color + (255,))

    # Pavio (wick) — y=2-4, x=7-8
    wick_color = (30, 20, 15, 255)
    for y in range(2, 4):
        img.putpixel((7, y), wick_color)
        img.putpixel((8, y), wick_color)

    if lit:
        # Chama acima do pavio: y=0-2, x=6-9
        flame_yellow = (255, 220, 50, 255)
        flame_orange = (255, 130, 30, 255)
        # Núcleo brilhante
        img.putpixel((7, 0), flame_yellow)
        img.putpixel((8, 0), flame_yellow)
        img.putpixel((7, 1), (255, 255, 200, 255))
        img.putpixel((8, 1), (255, 255, 200, 255))
        img.putpixel((6, 1), flame_orange)
        img.putpixel((9, 1), flame_orange)
        img.putpixel((7, 2), flame_yellow)
        img.putpixel((8, 2), flame_yellow)
    else:
        # Pavio queimado (escuro escuro no topo)
        img.putpixel((7, 1), (20, 15, 10, 255))
        img.putpixel((8, 1), (20, 15, 10, 255))

    return img

print("\n=== Candle textures ===")
for name, color, lit in CANDLES:
    path = OUT / f"{name}.png"
    img = make_candle_texture(name, color, lit)
    img.save(path)
    print(f"  + {name}.png")

print("\nDone.")
