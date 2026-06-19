"""
Gerador de texturas pro Blood Tree set (16×16 PNGs).

Outputs:
- block/blood_log.png         — troco lateral, vermelho-vinho com veias verticais
- block/blood_log_top.png     — topo do tronco, anéis concêntricos vinho/escuro
- block/stripped_blood_log.png — versão sem casca, paleta mais clara
- block/stripped_blood_log_top.png
- block/blood_planks.png      — tábuas verticais com veias claras
- block/blood_leaves.png      — folhas vermelhas escuras com alpha (cutout)
- block/blood_sapling.png     — sapling cross com mudinha vermelha
- block/blood_door_top.png    — metade superior da porta
- block/blood_door_bottom.png — metade inferior
- block/blood_trapdoor.png    — trapdoor
- item/blood_door.png         — ícone do item da porta

Determinístico (seed fixa) pra evitar diff trash.
"""
from PIL import Image
import os
import random

ROOT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures"
BLOCK = os.path.join(ROOT, "block")
ITEM = os.path.join(ROOT, "item")
os.makedirs(BLOCK, exist_ok=True)
os.makedirs(ITEM, exist_ok=True)

random.seed(0xB100D)

def new_img(alpha=False, bg=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), bg)

def px(im, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), color)

def rect(im, x1, y1, x2, y2, color):
    for y in range(max(0, y1), min(16, y2 + 1)):
        for x in range(max(0, x1), min(16, x2 + 1)):
            px(im, x, y, color)

def jitter(c, amt):
    r, g, b, a = c
    j = random.randint(-amt, amt)
    return (max(0, min(255, r + j)), max(0, min(255, g + j)), max(0, min(255, b + j)), a)

# ─── Cores temáticas ─────────────────────────────────────────────
WINE_DARK  = (60, 10, 14, 255)    # base do tronco
WINE_MID   = (90, 16, 16, 255)    # médio
WINE_LIGHT = (138, 32, 32, 255)   # veias claras
BLOOD_RED  = (170, 30, 30, 255)   # sangue vivo
NEAR_BLACK = (30, 6, 8, 255)      # sombras
PLANK_BASE = (110, 32, 26, 255)   # tábuas
PLANK_DARK = (74, 18, 14, 255)
PLANK_LITE = (148, 50, 40, 255)
LEAF_DARK  = (50, 8, 12, 255)
LEAF_MID   = (90, 18, 22, 255)
LEAF_LIGHT = (140, 30, 36, 255)
SAP_GREEN  = (60, 70, 30, 255)    # caule fino
SAP_LEAF   = (130, 28, 32, 255)
STRIP_BASE = (160, 60, 50, 255)
STRIP_DARK = (110, 40, 32, 255)
STRIP_LITE = (200, 90, 70, 255)


# ─── Blood log (side) ─────────────────────────────────────────────
def gen_blood_log_side(path):
    im = new_img(bg=WINE_MID)
    # Veias verticais escuras
    for x in [1, 4, 7, 10, 13]:
        for y in range(16):
            if random.random() < 0.85:
                px(im, x, y, jitter(WINE_DARK, 8))
    # Veias claras intermitentes
    for x in [3, 8, 12]:
        for y in range(16):
            if random.random() < 0.4:
                px(im, x, y, jitter(WINE_LIGHT, 12))
    # Manchas escuras aleatórias (nós)
    for _ in range(6):
        cx, cy = random.randint(2, 13), random.randint(2, 13)
        px(im, cx, cy, NEAR_BLACK)
        if random.random() < 0.5:
            px(im, cx + 1, cy, jitter(WINE_DARK, 6))
    # Bordas mais escuras
    rect(im, 0, 0, 0, 15, jitter(NEAR_BLACK, 4))
    rect(im, 15, 0, 15, 15, jitter(NEAR_BLACK, 4))
    im.save(path)

# ─── Blood log top (rings) ─────────────────────────────────────────
def gen_blood_log_top(path):
    im = new_img(bg=WINE_MID)
    cx, cy = 7, 7
    for y in range(16):
        for x in range(16):
            # distância do centro
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            ring = int(d2 ** 0.5)
            if ring % 3 == 0:
                px(im, x, y, jitter(WINE_DARK, 6))
            elif ring % 3 == 1:
                px(im, x, y, jitter(WINE_MID, 8))
            else:
                px(im, x, y, jitter(WINE_LIGHT, 10))
    # Cerne — núcleo vermelho-sangue
    rect(im, 7, 7, 8, 8, BLOOD_RED)
    px(im, 6, 7, jitter(BLOOD_RED, 12))
    px(im, 7, 6, jitter(BLOOD_RED, 12))
    # Bark — borda escura
    for x in range(16):
        px(im, x, 0, jitter(NEAR_BLACK, 8))
        px(im, x, 15, jitter(NEAR_BLACK, 8))
        px(im, 0, x, jitter(NEAR_BLACK, 8))
        px(im, 15, x, jitter(NEAR_BLACK, 8))
    im.save(path)

# ─── Stripped variants ─────────────────────────────────────────────
def gen_stripped_log_side(path):
    im = new_img(bg=STRIP_BASE)
    for x in range(16):
        for y in range(16):
            r = random.random()
            if r < 0.18:
                px(im, x, y, jitter(STRIP_DARK, 10))
            elif r < 0.32:
                px(im, x, y, jitter(STRIP_LITE, 12))
    # Veias verticais
    for x in [3, 7, 11]:
        for y in range(16):
            if random.random() < 0.6:
                px(im, x, y, jitter(STRIP_DARK, 6))
    im.save(path)

def gen_stripped_log_top(path):
    im = new_img(bg=STRIP_BASE)
    cx, cy = 7, 7
    for y in range(16):
        for x in range(16):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            ring = int(d2 ** 0.5)
            if ring % 3 == 0:
                px(im, x, y, jitter(STRIP_DARK, 6))
            elif ring % 3 == 2:
                px(im, x, y, jitter(STRIP_LITE, 8))
            else:
                px(im, x, y, jitter(STRIP_BASE, 6))
    im.save(path)

# ─── Blood planks ─────────────────────────────────────────────────
def gen_blood_planks(path):
    im = new_img(bg=PLANK_BASE)
    # 4 tábuas verticais (4px cada)
    for i in range(4):
        x_start = i * 4
        # Variação de tons por tábua
        plank_tone = jitter(PLANK_BASE, 10)
        rect(im, x_start, 0, x_start + 3, 15, plank_tone)
        # Linha divisória escura
        if i > 0:
            for y in range(16):
                px(im, x_start, y, jitter(PLANK_DARK, 4))
        # Veias horizontais
        for y in [2, 6, 11, 14]:
            for x in range(x_start, x_start + 4):
                if random.random() < 0.5:
                    px(im, x, y, jitter(PLANK_LITE, 8))
        # Manchas escuras (nós da madeira)
        if random.random() < 0.5:
            nx = random.randint(x_start, x_start + 2)
            ny = random.randint(2, 13)
            px(im, nx, ny, jitter(PLANK_DARK, 6))
            px(im, nx, ny + 1, jitter(PLANK_DARK, 6))
    im.save(path)

# ─── Blood leaves (com alpha) ─────────────────────────────────────
def gen_blood_leaves(path):
    im = new_img(alpha=True)
    # Base: padrão denso de folhas com ~85% opacidade
    for y in range(16):
        for x in range(16):
            r = random.random()
            if r < 0.08:
                # Pixel transparente (vazio entre folhas)
                continue
            if r < 0.45:
                px(im, x, y, jitter(LEAF_MID, 8))
            elif r < 0.75:
                px(im, x, y, jitter(LEAF_DARK, 6))
            else:
                px(im, x, y, jitter(LEAF_LIGHT, 10))
    # Pontos vermelho-sangue (frutas/brilho)
    for _ in range(4):
        sx, sy = random.randint(1, 14), random.randint(1, 14)
        px(im, sx, sy, BLOOD_RED)
    im.save(path)

# ─── Blood sapling (cross com galho fino + folhas) ────────────────
def gen_blood_sapling(path):
    im = new_img(alpha=True)
    # Caule fino central
    for y in range(8, 16):
        px(im, 7, y, SAP_GREEN)
        px(im, 8, y, jitter(SAP_GREEN, 6))
    # Folhas — formato de cruz pequeno
    for y in range(3, 11):
        for x in range(4, 12):
            d = abs(x - 7) + abs(y - 7)
            if d <= 4 and random.random() < 0.75:
                if random.random() < 0.3:
                    px(im, x, y, jitter(LEAF_DARK, 8))
                elif random.random() < 0.6:
                    px(im, x, y, jitter(LEAF_MID, 8))
                else:
                    px(im, x, y, jitter(SAP_LEAF, 10))
    # Sangue pingando
    px(im, 6, 11, BLOOD_RED)
    px(im, 9, 12, BLOOD_RED)
    im.save(path)

# ─── Blood door (bottom + top) ────────────────────────────────────
def gen_blood_door_bottom(path):
    im = new_img(bg=PLANK_BASE)
    # Mesma base de planks
    for y in range(16):
        for x in range(16):
            r = random.random()
            if r < 0.2:
                px(im, x, y, jitter(PLANK_DARK, 8))
            elif r < 0.35:
                px(im, x, y, jitter(PLANK_LITE, 10))
    # Padrão de tábuas verticais
    for x in [3, 7, 11]:
        for y in range(16):
            px(im, x, y, jitter(PLANK_DARK, 4))
    # Maçaneta na altura ~6
    rect(im, 12, 6, 14, 8, (180, 140, 40, 255))
    px(im, 13, 7, (220, 180, 60, 255))
    im.save(path)

def gen_blood_door_top(path):
    im = new_img(bg=PLANK_BASE)
    for y in range(16):
        for x in range(16):
            r = random.random()
            if r < 0.2:
                px(im, x, y, jitter(PLANK_DARK, 8))
            elif r < 0.35:
                px(im, x, y, jitter(PLANK_LITE, 10))
    for x in [3, 7, 11]:
        for y in range(16):
            px(im, x, y, jitter(PLANK_DARK, 4))
    # Decoração no topo — gota de sangue
    px(im, 7, 4, BLOOD_RED)
    px(im, 7, 5, BLOOD_RED)
    px(im, 8, 5, BLOOD_RED)
    px(im, 8, 6, jitter(BLOOD_RED, 10))
    im.save(path)

# ─── Blood trapdoor ───────────────────────────────────────────────
def gen_blood_trapdoor(path):
    im = new_img(bg=PLANK_BASE)
    for y in range(16):
        for x in range(16):
            r = random.random()
            if r < 0.2:
                px(im, x, y, jitter(PLANK_DARK, 8))
            elif r < 0.35:
                px(im, x, y, jitter(PLANK_LITE, 10))
    # Padrão horizontal (trapdoor é mais wide)
    for y in [3, 7, 11]:
        for x in range(16):
            px(im, x, y, jitter(PLANK_DARK, 4))
    # Reforço metálico nas pontas
    for x in [1, 14]:
        for y in range(1, 15):
            px(im, x, y, (100, 100, 100, 255))
    im.save(path)

# ─── Item icon pra blood door ─────────────────────────────────────
def gen_blood_door_item(path):
    im = new_img(alpha=True)
    # Silhueta de porta
    rect(im, 4, 1, 12, 15, PLANK_BASE)
    # Borda
    for x in range(4, 13):
        px(im, x, 1, NEAR_BLACK)
        px(im, x, 15, NEAR_BLACK)
    for y in range(1, 16):
        px(im, 4, y, NEAR_BLACK)
        px(im, 12, y, NEAR_BLACK)
    # Linhas verticais (tábuas)
    for y in range(2, 15):
        for x in [6, 9]:
            px(im, x, y, jitter(PLANK_DARK, 6))
    # Maçaneta
    px(im, 11, 8, (220, 180, 60, 255))
    # Detalhes — gotas
    px(im, 7, 4, BLOOD_RED)
    px(im, 9, 11, BLOOD_RED)
    im.save(path)


def main():
    out = {
        "blood_log.png":           lambda p: gen_blood_log_side(p),
        "blood_log_top.png":       lambda p: gen_blood_log_top(p),
        "stripped_blood_log.png":  lambda p: gen_stripped_log_side(p),
        "stripped_blood_log_top.png": lambda p: gen_stripped_log_top(p),
        "blood_planks.png":        lambda p: gen_blood_planks(p),
        "blood_leaves.png":        lambda p: gen_blood_leaves(p),
        "blood_sapling.png":       lambda p: gen_blood_sapling(p),
        "blood_door_bottom.png":   lambda p: gen_blood_door_bottom(p),
        "blood_door_top.png":      lambda p: gen_blood_door_top(p),
        "blood_trapdoor.png":      lambda p: gen_blood_trapdoor(p),
    }
    for name, fn in out.items():
        path = os.path.join(BLOCK, name)
        # Reset seed per file pra diff estável
        random.seed(hash(name) & 0xFFFFFFFF)
        fn(path)
        print(f"OK  {path}")

    # Item: blood_door
    random.seed(0xD00D)
    gen_blood_door_item(os.path.join(ITEM, "blood_door.png"))
    print(f"OK  {os.path.join(ITEM, 'blood_door.png')}")


if __name__ == "__main__":
    main()
