"""r150: Gera texturas (64x64 wood) + lang entries pros 8 Wooden Horrors."""
import json
import random
from pathlib import Path
from PIL import Image, ImageDraw

TEX_DIR = Path("src/main/resources/assets/liberthia/textures/entity")
TEX_DIR.mkdir(parents=True, exist_ok=True)

LANG_EN = Path("src/main/resources/assets/liberthia/lang/en_us.json")
LANG_PT = Path("src/main/resources/assets/liberthia/lang/pt_br.json")

# Variants: (id, primary_color, dark, grain, display_en, display_pt)
VARIANTS = [
    ("charcoal",         (40, 35, 32),    (15, 12, 10),  (60, 50, 45),    "Charcoal Wooden Horror",       "Horror de Madeira Carvão"),
    ("pale_oak",         (220, 200, 160), (170, 150, 120), (200, 180, 140),"Pale Oak Wooden Horror",       "Horror de Carvalho Pálido"),
    ("rotted_birch",     (130, 130, 115), (80, 80, 65),  (110, 110, 95),  "Rotted Birch Wooden Horror",   "Horror de Bétula Apodrecida"),
    ("bleeding_maple",   (130, 40, 40),   (70, 15, 15),  (180, 60, 60),   "Bleeding Maple Wooden Horror", "Horror de Bordo Sangrento"),
    ("mossy",            (75, 110, 55),   (40, 65, 30),  (100, 140, 70),  "Mossy Wooden Horror",          "Horror Musgoso"),
    ("frozen_pine",      (140, 200, 220), (80, 130, 160),(180, 220, 240), "Frozen Pine Wooden Horror",    "Horror de Pinho Gelado"),
    ("burning_acacia",   (220, 110, 40),  (160, 60, 20), (240, 160, 80),  "Burning Acacia Wooden Horror", "Horror de Acácia Flamejante"),
    ("cursed_mahogany",  (90, 50, 110),   (50, 25, 70),  (130, 80, 150),  "Cursed Mahogany Wooden Horror","Horror de Mogno Amaldiçoado"),
]

def make_wood_texture(name, base, dark, light):
    """Cria textura 64x64 humanoid (PLAYER layout) totalmente de madeira."""
    random.seed(hash(name))
    W, H = 64, 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Player UV regions (cobre todo o corpo de uma vez se preencher essas)
    regions = [
        (0, 0, 32, 16),    # head
        (32, 0, 64, 16),   # hat
        (16, 16, 40, 32),  # body
        (40, 16, 56, 32),  # right arm
        (0, 16, 16, 32),   # right leg
        (32, 48, 48, 64),  # left arm
        (16, 48, 32, 64),  # left leg
        (16, 32, 40, 48),  # jacket
        (0, 32, 16, 48),   # right pants
        (0, 48, 16, 64),   # left pants
        (48, 48, 64, 64),  # left sleeve
        (40, 32, 56, 48),  # right sleeve
    ]

    for x0, y0, x1, y1 in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r = random.random()
                if r < 0.1: c = dark + (255,)
                elif r < 0.7: c = base + (255,)
                else: c = light + (255,)
                img.putpixel((x, y), c)
        # Vertical grain lines
        for gx in range(x0, x1):
            if random.random() < 0.18:
                for gy in range(y0, y1):
                    if random.random() < 0.6:
                        img.putpixel((gx, gy), dark + (255,))
        # Knot
        if random.random() < 0.35 and (x1 - x0) >= 6 and (y1 - y0) >= 6:
            kx = random.randint(x0 + 2, x1 - 4)
            ky = random.randint(y0 + 2, y1 - 4)
            d.ellipse([kx, ky, kx + 2, ky + 2], fill=(15, 10, 8, 255))

    # Eyes on face (head front: 8,8-16,16 — center 11,11)
    eye_glow = (255, 255, 100, 255)
    img.putpixel((10, 11), eye_glow)
    img.putpixel((13, 11), eye_glow)
    img.putpixel((9, 11), (200, 200, 50, 255))
    img.putpixel((14, 11), (200, 200, 50, 255))
    # Mouth crack
    for x in range(10, 15):
        img.putpixel((x, 14), (10, 5, 5, 255))

    return img

print("=== Generating wooden horror textures ===")
for vid, primary, dark, light, _, _ in VARIANTS:
    path = TEX_DIR / f"wooden_{vid}.png"
    img = make_wood_texture(vid, primary, dark, light)
    img.save(path)
    print(f"  + {path.name}")

# Lang entries
print("\n=== Adding lang entries ===")
def update_lang(path, lang_key):
    data = json.loads(path.read_text(encoding="utf-8"))
    for vid, _, _, _, en, pt in VARIANTS:
        entity_key = f"entity.liberthia.wooden_{vid}"
        egg_key = f"item.liberthia.wooden_{vid}_spawn_egg"
        display = pt if lang_key == "pt" else en
        data[entity_key] = display
        data[egg_key] = f"{display.split(' Wooden')[0]} Spawn Egg" if lang_key == "en" else f"Ovo de {display.replace('Horror de ', '')}"
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"  + {path.name}")

update_lang(LANG_EN, "en")
update_lang(LANG_PT, "pt")

print("\nDone.")
