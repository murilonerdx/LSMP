"""r152: Regenera as 7 texturas magic_fire COM TRANSPARÊNCIA REAL.

Bug r149: as texturas anteriores tinham alpha=255 em TUDO. Mesmo com
cross model + RenderType.cutout, o fundo aparecia preto pq o PNG tinha
fundo opaco.

Fix: gera flame-shaped texture com TRUE transparency no background.
"""
from PIL import Image, ImageDraw
import random
from pathlib import Path

OUT = Path("src/main/resources/assets/liberthia/textures/block")

# (id, primary, secondary, tertiary) — 3 cores pra gradiente da chama
SCHOOLS = [
    ("magic_fire_fire",      (255, 100, 0),   (255, 200, 50),  (255, 50, 0)),
    ("magic_fire_ice",       (150, 220, 255), (240, 250, 255), (60, 130, 200)),
    ("magic_fire_lightning", (255, 255, 100), (255, 255, 220), (220, 180, 50)),
    ("magic_fire_blood",     (180, 30, 30),   (255, 80, 80),   (90, 10, 10)),
    ("magic_fire_eldritch",  (150, 60, 220),  (220, 150, 255), (80, 20, 130)),
    ("magic_fire_holy",      (255, 230, 130), (255, 255, 220), (200, 150, 50)),
    ("magic_fire_nature",    (80, 180, 60),   (180, 240, 100), (40, 100, 30)),
]

def make_flame(name, primary, light, dark):
    """16x16 flame shape com transparência REAL."""
    random.seed(hash(name))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))  # TUDO TRANSPARENTE inicial

    # Flame shape: ampla na base (y=15), afina no topo (y=2)
    # Pra cada linha vertical, define o range X coberto
    for y in range(2, 16):
        # progresso 0..1 (base = 0, topo = 1)
        t = (y - 2) / 13.0
        # Width da chama vai de 12 (base) a 2 (topo)
        half_width = int(6 - t * 5)
        cx = 8

        for x in range(cx - half_width, cx + half_width + 1):
            if x < 0 or x >= 16: continue
            # Determina cor pelo gradiente
            # Centro = light, meio = primary, borda = dark
            dist = abs(x - cx) / max(1, half_width)
            r = random.random()
            if dist < 0.3 and r < 0.7:
                color = light + (255,)
            elif dist < 0.7:
                color = primary + (255,)
            else:
                color = dark + (255,)

            # Adiciona "flicker" — alguns pixels random ficam transparentes pra dar feel de chama
            if y < 6 and random.random() < 0.3:
                continue  # pixel transparente no topo (efeito flicker)

            img.putpixel((x, y), color)

    # Add some spark particles flutuando ao redor (alpha parcial seria ideal mas cutout não suporta — alpha=255 ou 0)
    # Spark dots:
    for _ in range(8):
        sx = random.randint(2, 13)
        sy = random.randint(2, 14)
        # Só põe se pixel atual for transparente (não cobre chama)
        if img.getpixel((sx, sy))[3] == 0:
            img.putpixel((sx, sy), light + (255,))

    return img

print("=== Regenerating magic_fire textures with PROPER alpha ===")
for name, p, l, d in SCHOOLS:
    path = OUT / f"{name}.png"
    img = make_flame(name, p, l, d)
    img.save(path)
    # Verify alpha
    alphas = [img.getpixel((x, y))[3] for y in range(16) for x in range(16)]
    transparent = sum(1 for a in alphas if a == 0)
    print(f"+ {name}.png — transparent pixels: {transparent}/256")

print("\nDone.")
