"""r121: HQ texture redo — candles, chalk, runes, broken mob textures.

Cada função abaixo gera uma textura individual com lógica pixel-art
intencional (não noise). O objetivo é VISUAL CLARO, não detalhe.
"""
from PIL import Image, ImageDraw, ImageFilter
import os, math, random

ROOT = os.path.dirname(__file__)
TEX_BLOCK = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/block"))
TEX_ITEM  = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/item"))
TEX_ENT   = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/entity"))
for d in [TEX_BLOCK, TEX_ITEM, TEX_ENT, os.path.join(TEX_ENT, "void_larva")]:
    os.makedirs(d, exist_ok=True)


# ╔════════════════════════════════════════════════════════════════════════
# ║ VELAS (10 texturas — 5 cores × 2 estados)
# ╚════════════════════════════════════════════════════════════════════════
CANDLE_COLORS = {
    "black":  {"wax": (35, 30, 40),   "wax_lit": (60, 50, 65),   "drip": (15, 10, 20)},
    "purple": {"wax": (95, 50, 140),  "wax_lit": (140, 80, 200), "drip": (55, 25, 90)},
    "red":    {"wax": (170, 35, 35),  "wax_lit": (220, 60, 60),  "drip": (110, 15, 15)},
    "white":  {"wax": (235, 230, 220),"wax_lit": (255, 250, 240),"drip": (180, 175, 165)},
    "golden": {"wax": (215, 175, 70), "wax_lit": (250, 215, 110),"drip": (155, 115, 30)},
}


def make_candle(name, lit):
    """Vela 16×16 — coluna central de cera, pavio, base mais escura.
    Lit version: pavio brilha amarelo + glow ao redor."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = CANDLE_COLORS[name]
    wax = c["wax"] + (255,)
    drip = c["drip"] + (255,)

    # Pavio (wick) — sai do topo
    wick_top_y = 2 if lit else 2
    wick_bottom_y = 6
    if not lit:
        # Pavio preto natural
        d.line([(7, wick_top_y), (7, wick_bottom_y)], fill=(20, 15, 10, 255))
        d.line([(8, wick_top_y), (8, wick_bottom_y)], fill=(40, 30, 20, 255))
    else:
        # Pavio queimando — base preta, ponta cinza
        d.line([(7, 4), (7, wick_bottom_y)], fill=(20, 15, 10, 255))
        d.line([(8, 4), (8, wick_bottom_y)], fill=(20, 15, 10, 255))
        # Chama: 3 layers — base laranja, mid amarelo, topo branco
        # bottom (laranja)
        d.point((7, 3), fill=(255, 130, 30, 255))
        d.point((8, 3), fill=(255, 150, 40, 255))
        d.point((6, 3), fill=(220, 80, 20, 255))
        d.point((9, 3), fill=(220, 80, 20, 255))
        # mid (amarelo)
        d.point((7, 2), fill=(255, 220, 80, 255))
        d.point((8, 2), fill=(255, 230, 100, 255))
        # top (branco)
        d.point((7, 1), fill=(255, 255, 200, 255))
        d.point((8, 1), fill=(255, 255, 220, 255))
        # Aura externa transparente
        for x, y, a in [(6, 2, 100), (9, 2, 100), (5, 3, 60),
                         (10, 3, 60), (6, 1, 80), (9, 1, 80)]:
            d.point((x, y), fill=(255, 200, 80, a))

    # Corpo da vela — coluna 6..9, y 7..14
    for y in range(7, 15):
        # Gradient: mais clara no topo, mais escura na base
        t = (y - 7) / 8.0
        r = max(0, min(255, int(wax[0] * (1 - t * 0.25))))
        g = max(0, min(255, int(wax[1] * (1 - t * 0.25))))
        b = max(0, min(255, int(wax[2] * (1 - t * 0.25))))
        d.line([(6, y), (9, y)], fill=(r, g, b, 255))
        # Side highlights — 1px coluna mais clara à esquerda
        hl_r = min(255, r + 40)
        hl_g = min(255, g + 40)
        hl_b = min(255, b + 40)
        d.point((6, y), fill=(hl_r, hl_g, hl_b, 255))
        # Shadow direita
        sh_r = max(0, r - 30)
        sh_g = max(0, g - 30)
        sh_b = max(0, b - 30)
        d.point((9, y), fill=(sh_r, sh_g, sh_b, 255))

    # Base derretida — wax pingou um pouco mais largo no fundo
    d.rectangle([5, 14, 10, 15], fill=drip)
    d.point((4, 14), fill=drip)
    d.point((11, 14), fill=drip)
    # Pinguinho de cera escorrendo (variável por cor)
    drip_x = 5 if name in ("purple", "red") else 10
    d.point((drip_x, 13), fill=drip)

    img.save(os.path.join(TEX_BLOCK, f"candle_occult_{name}{'_lit' if lit else ''}.png"))


# ╔════════════════════════════════════════════════════════════════════════
# ║ CHALKS (item) — bastão de giz
# ╚════════════════════════════════════════════════════════════════════════
CHALK_COLORS = {
    "white":  (245, 240, 230),
    "black":  (40, 35, 35),
    "red":    (200, 50, 50),
    "purple": (160, 80, 200),
    "golden": (235, 195, 80),
}


def make_chalk(name, color):
    """Giz diagonal canto-canto, 16×16. Ponta pintada na cor + ponta branca."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = color + (255,)
    # Tom escuro pra sombra
    cshadow = (max(0, c[0] - 50), max(0, c[1] - 50), max(0, c[2] - 50), 255)
    chl = (min(255, c[0] + 35), min(255, c[1] + 35), min(255, c[2] + 35), 255)

    # Giz inclinado a 45°: do (3,12) ate (12,3)
    # Stick body 3 pixels de largura
    for offset in [-1, 0, 1]:
        for t in range(0, 10):
            # Posição ao longo do giz
            x = 3 + t
            y = 12 - t
            # Stick perpendicular: usa offset em (-y, x) direction
            for o in [offset]:
                px, py = x + o, y + o
                if 0 <= px < 16 and 0 <= py < 16:
                    if o == 0:
                        d.point((px, py), fill=c)
                    elif o == -1:
                        d.point((px, py), fill=chl)
                    elif o == 1:
                        d.point((px, py), fill=cshadow)

    # Top tip: ligeiramente mais brilhante (ponta usada / dust)
    d.point((12, 3), fill=(255, 255, 255, 255))
    d.point((11, 4), fill=chl)
    d.point((13, 4), fill=chl)
    # Pequeno dust trail
    d.point((13, 3), fill=(c[0], c[1], c[2], 180))
    d.point((14, 3), fill=(c[0], c[1], c[2], 100))

    # Bottom tip: base mais escura (gripped)
    d.point((3, 12), fill=cshadow)
    d.point((2, 13), fill=cshadow)
    d.point((4, 11), fill=cshadow)

    img.save(os.path.join(TEX_ITEM, f"chalk_{name}.png"))


def make_chalk_base():
    """Chalk base sem cor (branco padrão). 16×16."""
    make_chalk_into_file("chalk", (245, 240, 230))


def make_chalk_into_file(filename, color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = color + (255,)
    cshadow = (max(0, c[0] - 50), max(0, c[1] - 50), max(0, c[2] - 50), 255)
    chl = (min(255, c[0] + 35), min(255, c[1] + 35), min(255, c[2] + 35), 255)
    for offset in [-1, 0, 1]:
        for t in range(0, 10):
            x, y = 3 + t, 12 - t
            for o in [offset]:
                px, py = x + o, y + o
                if 0 <= px < 16 and 0 <= py < 16:
                    if o == 0:
                        d.point((px, py), fill=c)
                    elif o == -1:
                        d.point((px, py), fill=chl)
                    elif o == 1:
                        d.point((px, py), fill=cshadow)
    d.point((12, 3), fill=(255, 255, 255, 255))
    d.point((11, 4), fill=chl)
    d.point((3, 12), fill=cshadow)
    d.point((2, 13), fill=cshadow)
    img.save(os.path.join(TEX_ITEM, f"{filename}.png"))


def make_blood_chalk():
    """Blood chalk — dark crimson + bone tip."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = (130, 25, 25, 255)
    cshadow = (60, 10, 10, 255)
    chl = (180, 60, 60, 255)
    for offset in [-1, 0, 1]:
        for t in range(0, 10):
            x, y = 3 + t, 12 - t
            for o in [offset]:
                px, py = x + o, y + o
                if 0 <= px < 16 and 0 <= py < 16:
                    if o == 0:
                        d.point((px, py), fill=c)
                    elif o == -1:
                        d.point((px, py), fill=chl)
                    elif o == 1:
                        d.point((px, py), fill=cshadow)
    # Tip drips blood
    d.point((12, 3), fill=(220, 30, 30, 255))
    d.point((13, 4), fill=(180, 20, 20, 255))
    d.point((13, 3), fill=(200, 10, 10, 180))
    # Base: bone (ossso branco)
    d.point((3, 12), fill=(230, 220, 200, 255))
    d.point((2, 13), fill=(180, 170, 150, 255))
    img.save(os.path.join(TEX_ITEM, "blood_chalk.png"))


def make_observation_chalk():
    """Observation chalk — purple-blue mystical + glowing tip."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = (110, 90, 200, 255)
    cshadow = (50, 35, 110, 255)
    chl = (170, 150, 250, 255)
    for offset in [-1, 0, 1]:
        for t in range(0, 10):
            x, y = 3 + t, 12 - t
            for o in [offset]:
                px, py = x + o, y + o
                if 0 <= px < 16 and 0 <= py < 16:
                    if o == 0:
                        d.point((px, py), fill=c)
                    elif o == -1:
                        d.point((px, py), fill=chl)
                    elif o == 1:
                        d.point((px, py), fill=cshadow)
    # Mystic glow at tip
    d.point((12, 3), fill=(255, 255, 255, 255))
    d.point((11, 4), fill=(220, 200, 255, 255))
    d.point((13, 4), fill=(220, 200, 255, 255))
    d.point((13, 3), fill=(170, 150, 250, 200))
    d.point((14, 2), fill=(170, 150, 250, 120))
    # Sparkles ao redor da ponta
    d.point((14, 4), fill=(255, 240, 255, 150))
    d.point((10, 2), fill=(200, 180, 255, 100))
    img.save(os.path.join(TEX_ITEM, "observation_chalk.png"))


def make_chalk_symbol_item():
    """chalk_symbol item — generic glyph icon (5-point star sigil)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    # Outer ring
    d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], outline=(220, 220, 220, 255))
    # Inner ring
    d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], outline=(180, 180, 180, 180))
    # 5-point star
    pts = []
    for i in range(10):
        ang = math.radians(i * 36 - 90)
        r = 5 if i % 2 == 0 else 2
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    d.polygon(pts, outline=(255, 255, 255, 255))
    img.save(os.path.join(TEX_ITEM, "chalk_symbol.png"))


# ╔════════════════════════════════════════════════════════════════════════
# ║ RUNAS NO CHÃO — chalk_mark, chalk_glyph, chalk_symbol, rune_block
# ╚════════════════════════════════════════════════════════════════════════
def make_chalk_mark(name, color):
    """Chalk mark no chão 16×16 — círculo com runa central. Transparente."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = color + (255,)
    cdim = (color[0], color[1], color[2], 180)
    cfaint = (color[0], color[1], color[2], 100)

    cx, cy = 8, 8
    # Círculo externo
    d.ellipse([cx - 7, cy - 7, cx + 6, cy + 6], outline=c)
    # Círculo interno
    d.ellipse([cx - 4, cy - 4, cx + 3, cy + 3], outline=cdim)
    # 4 marcas radiais (cross + diagonal)
    for ang_deg in [0, 90, 180, 270]:
        ang = math.radians(ang_deg)
        x = int(cx + 5.5 * math.cos(ang))
        y = int(cy + 5.5 * math.sin(ang))
        d.point((x, y), fill=c)
    # Cross interior (mark central)
    d.line([(cx, cy - 2), (cx, cy + 2)], fill=cdim)
    d.line([(cx - 2, cy), (cx + 2, cy)], fill=cdim)
    # Central glow
    d.point((cx, cy), fill=(min(255, c[0] + 50), min(255, c[1] + 50), min(255, c[2] + 50), 255))

    img.save(os.path.join(TEX_BLOCK, f"chalk_mark_{name}.png"))


def make_chalk_glyph(idx):
    """4 chalk_glyph variants — sigilos diferentes."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    c = (240, 230, 220, 230)
    cglow = (255, 250, 200, 255)

    # Outer subtle circle
    d.ellipse([cx - 7, cy - 7, cx + 6, cy + 6], outline=(c[0], c[1], c[2], 100))

    if idx == 0:
        # Pentagrama
        pts = []
        for i in range(5):
            ang = math.radians(i * 72 - 90)
            pts.append((cx + 5 * math.cos(ang), cy + 5 * math.sin(ang)))
        # Conecta cada vértice ao 2º próximo (5-point star)
        for i in range(5):
            a = pts[i]
            b = pts[(i + 2) % 5]
            d.line([a, b], fill=c)
    elif idx == 1:
        # Hexagrama (estrela de 6 pontas)
        # Triângulo apontando pra cima
        d.polygon([(cx, cy - 5), (cx - 4, cy + 3), (cx + 4, cy + 3)], outline=c)
        # Triângulo apontando pra baixo
        d.polygon([(cx, cy + 5), (cx - 4, cy - 3), (cx + 4, cy - 3)], outline=c)
    elif idx == 2:
        # Triângulo invertido com olho central
        d.polygon([(cx, cy + 5), (cx - 5, cy - 4), (cx + 5, cy - 4)], outline=c)
        d.ellipse([cx - 2, cy - 2, cx + 1, cy + 1], outline=c)
        d.point((cx, cy), fill=cglow)
    elif idx == 3:
        # Spiral
        for t in range(0, 20):
            ang = t * 0.6
            r = t * 0.3
            x = int(cx + r * math.cos(ang))
            y = int(cy + r * math.sin(ang))
            if 0 <= x < 16 and 0 <= y < 16:
                d.point((x, y), fill=c)

    # Center glow (todos)
    d.point((cx, cy), fill=cglow)

    img.save(os.path.join(TEX_BLOCK, f"chalk_glyph_{idx}.png"))


def make_chalk_symbol_block():
    """Generic chalk_symbol no chão — círculo+triângulo+olho."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    c = (235, 225, 215, 220)
    # Outer circle
    d.ellipse([cx - 7, cy - 7, cx + 6, cy + 6], outline=c)
    # Inner triangle
    d.polygon([(cx, cy - 4), (cx - 4, cy + 3), (cx + 4, cy + 3)], outline=c)
    # Eye in center
    d.line([(cx - 2, cy), (cx + 2, cy)], fill=c)
    d.point((cx, cy), fill=(255, 250, 200, 255))
    img.save(os.path.join(TEX_BLOCK, "chalk_symbol.png"))


def make_rune_block():
    """Rune block — runa nórdica num "círculo de pedra" no chão."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Background stone-ish
    d.rectangle([0, 0, 15, 15], fill=(80, 70, 90, 255))
    # Noise (just a few darker pixels for texture)
    random.seed(42)
    for _ in range(20):
        px = random.randint(0, 15)
        py = random.randint(0, 15)
        d.point((px, py), fill=(60, 50, 70, 255))
    # Stone border ring
    d.ellipse([1, 1, 14, 14], outline=(140, 120, 160, 255))
    # Carved rune — purple glow
    cx, cy = 8, 8
    # Algiz rune ╪╪╪ (Y vertical com tracinhos)
    d.line([(cx, cy - 4), (cx, cy + 4)], fill=(200, 140, 250, 255))
    d.line([(cx, cy - 4), (cx - 2, cy - 2)], fill=(200, 140, 250, 255))
    d.line([(cx, cy - 4), (cx + 2, cy - 2)], fill=(200, 140, 250, 255))
    d.point((cx, cy), fill=(255, 220, 255, 255))
    # Glow
    d.point((cx - 1, cy), fill=(180, 110, 240, 200))
    d.point((cx + 1, cy), fill=(180, 110, 240, 200))
    img.save(os.path.join(TEX_BLOCK, "rune_block.png"))
    img.save(os.path.join(TEX_ITEM, "rune_block.png"))


# ╔════════════════════════════════════════════════════════════════════════
# ║ MOB TEXTURES (refazendo as "zoadas")
# ╚════════════════════════════════════════════════════════════════════════

def fill_alpha(img, alpha=0):
    """Cria base com alpha. Útil pra não-pintadas regiões."""
    return img


def make_silverfish_skin(filepath, body_color, accent_color, glow=False):
    """64×32 silverfish UV layout — pinta segments do corpo.

    Vanilla silverfish UV (simplificado):
    - Head: (0,0) to (16,8)
    - Body segments: (0,8) to (40,24)
    - Legs: scattered
    Vou pintar TUDO body color com accent stripes.
    """
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    body = body_color + (255,)
    bshadow = (max(0, body_color[0] - 40),
               max(0, body_color[1] - 40),
               max(0, body_color[2] - 40), 255)
    bhl = (min(255, body_color[0] + 35),
           min(255, body_color[1] + 35),
           min(255, body_color[2] + 35), 255)
    accent = accent_color + (255,)

    # Preenche todo o quadrado top-left 40×24 (área usada pelos vanilla silverfish UV)
    for y in range(32):
        for x in range(64):
            # Apenas dentro de regiões usadas
            if (x < 40 and y < 24) or (x < 32 and y < 12):
                # Gradient leve
                t = (y % 4) / 4.0
                cc = (int(body[0] * (1 - t * 0.2)),
                      int(body[1] * (1 - t * 0.2)),
                      int(body[2] * (1 - t * 0.2)), 255)
                d.point((x, y), fill=cc)

    # Segmentação: 4 stripes verticais pretos a cada 8 px (representa segments)
    for seg_x in [8, 16, 24, 32]:
        if seg_x < 40:
            d.line([(seg_x, 8), (seg_x, 23)], fill=bshadow)

    # Highlight stripe no topo do corpo (back/spine)
    for x in range(0, 40):
        d.point((x, 8), fill=bhl)

    # Eyes — 2 olhos accent na head area (head UV at y=0..8)
    d.point((10, 4), fill=accent)
    d.point((11, 4), fill=accent)
    d.point((10, 5), fill=accent)
    d.point((11, 5), fill=accent)
    # Other eye
    d.point((22, 4), fill=accent)
    d.point((23, 4), fill=accent)
    d.point((22, 5), fill=accent)
    d.point((23, 5), fill=accent)

    if glow:
        # Glow accent dots nas costas
        for x in [4, 12, 20, 28, 36]:
            d.point((x, 14), fill=accent)
            d.point((x, 15), fill=accent)

    img.save(filepath)


def make_void_larva():
    """Larva roxa segmentada."""
    make_silverfish_skin(
        os.path.join(TEX_ENT, "void_larva", "void_larva.png"),
        body_color=(80, 30, 130),
        accent_color=(255, 100, 250),
        glow=True
    )


def make_blood_worm():
    make_silverfish_skin(
        os.path.join(TEX_ENT, "blood_worm.png"),
        body_color=(150, 30, 40),
        accent_color=(255, 220, 50),
        glow=False
    )


def make_gore_worm():
    make_silverfish_skin(
        os.path.join(TEX_ENT, "gore_worm.png"),
        body_color=(180, 60, 50),
        accent_color=(255, 100, 100),
        glow=False
    )


def make_flesh_crawler():
    make_silverfish_skin(
        os.path.join(TEX_ENT, "flesh_crawler.png"),
        body_color=(200, 130, 130),
        accent_color=(255, 60, 60),
        glow=False
    )


def make_loom_worm():
    make_silverfish_skin(
        os.path.join(TEX_ENT, "loom_worm.png"),
        body_color=(70, 50, 100),
        accent_color=(160, 120, 220),
        glow=True
    )


def make_humanoid_skin(filepath, base_color, shirt_color, accent_color,
                        glow_eyes=True, robe=False):
    """64×64 humanoid skin (player layout). Pinta cabeça + corpo + braços + pernas
    com cor da pele/clothing."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    skin = base_color + (255,)
    shirt = shirt_color + (255,)
    sshadow = (max(0, shirt_color[0] - 40),
               max(0, shirt_color[1] - 40),
               max(0, shirt_color[2] - 40), 255)
    shl = (min(255, shirt_color[0] + 30),
           min(255, shirt_color[1] + 30),
           min(255, shirt_color[2] + 30), 255)
    skin_shadow = (max(0, base_color[0] - 30),
                   max(0, base_color[1] - 30),
                   max(0, base_color[2] - 30), 255)

    # Helper: paint rect
    def paint(x0, y0, w, h, color):
        d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=color)

    # HEAD — 8x8 face areas: (8,8)-(16,16) front, (0,8)-(8,16) right side, etc.
    # Full head UV: (0,0)-(32,16) covers all 6 faces of head box
    paint(0, 0, 32, 16, skin)
    # Add shadow under to bottom of head UV
    for x in range(8, 24):
        d.point((x, 15), fill=skin_shadow)
    # Eyes — front face is (8,8)-(16,16)
    if glow_eyes:
        d.point((10, 11), fill=accent_color + (255,))
        d.point((13, 11), fill=accent_color + (255,))
    else:
        d.point((10, 11), fill=(20, 20, 20, 255))
        d.point((13, 11), fill=(20, 20, 20, 255))
    # Mouth
    d.line([(11, 13), (12, 13)], fill=skin_shadow)

    # BODY — UV (16,16)-(40,32)
    paint(16, 16, 24, 16, shirt)
    # Highlight strip top body
    for x in range(16, 40):
        d.point((x, 16), fill=shl)
    # Center robe line if robe
    if robe:
        for y in range(16, 32):
            d.point((28, y), fill=sshadow)

    # ARMS — UV (40,16)-(56,32) right arm; (32,48)-(56,64) is left in new format
    # Right arm:
    paint(40, 16, 16, 16, shirt)
    # Left arm:
    paint(32, 48, 16, 16, shirt)
    # Wrists slightly skin-color
    for x in range(40, 48):
        d.point((x, 31), fill=skin)
    for x in range(32, 40):
        d.point((x, 63), fill=skin)

    # LEGS — UV (0,16)-(16,32) right leg; (16,48)-(32,64) left leg
    leg_color = sshadow  # legs darker
    paint(0, 16, 16, 16, leg_color)
    paint(16, 48, 16, 16, leg_color)
    # Boot detail
    for x in range(0, 8):
        d.point((x, 31), fill=(20, 15, 10, 255))
    for x in range(16, 24):
        d.point((x, 63), fill=(20, 15, 10, 255))

    img.save(filepath)


def make_disarmer():
    """Disarmer — bandit shadowy humanoid."""
    make_humanoid_skin(
        os.path.join(TEX_ENT, "disarmer.png"),
        base_color=(70, 65, 60),     # pele acinzentada
        shirt_color=(40, 30, 30),    # roupa preta
        accent_color=(220, 60, 60),  # olhos vermelhos
        glow_eyes=True,
        robe=False
    )


def make_weaving_shade():
    """Weaving Shade — sombra com olhos roxos."""
    make_humanoid_skin(
        os.path.join(TEX_ENT, "weaving_shade.png"),
        base_color=(30, 25, 40),
        shirt_color=(50, 35, 70),
        accent_color=(200, 100, 255),
        glow_eyes=True,
        robe=True
    )


def make_loom_humanoid_skins():
    """Loom watcher/peripheral/screamer — same humanoid pattern com cores diferentes."""
    make_humanoid_skin(
        os.path.join(TEX_ENT, "loom_watcher.png"),
        base_color=(35, 30, 45),
        shirt_color=(60, 40, 90),
        accent_color=(255, 60, 200),
        glow_eyes=True,
        robe=True
    )
    make_humanoid_skin(
        os.path.join(TEX_ENT, "loom_peripheral.png"),
        base_color=(40, 35, 50),
        shirt_color=(70, 50, 100),
        accent_color=(255, 100, 220),
        glow_eyes=True,
        robe=True
    )
    make_humanoid_skin(
        os.path.join(TEX_ENT, "loom_screamer.png"),
        base_color=(60, 30, 35),
        shirt_color=(110, 40, 50),
        accent_color=(255, 220, 80),
        glow_eyes=True,
        robe=True
    )


def make_spell_orb():
    """Spell orb projectile — 16×16 round glow purple/magic core."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    # Outer halo
    for r in range(7, 0, -1):
        if r >= 6:
            color = (180, 100, 220, 80)
        elif r >= 4:
            color = (160, 80, 200, 160)
        elif r >= 2:
            color = (200, 140, 240, 220)
        else:
            color = (255, 220, 255, 255)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    # Central bright pin
    d.point((cx, cy), fill=(255, 255, 255, 255))
    # Sparkles
    d.point((cx - 4, cy), fill=(220, 180, 255, 200))
    d.point((cx + 4, cy), fill=(220, 180, 255, 200))
    d.point((cx, cy - 4), fill=(220, 180, 255, 200))
    d.point((cx, cy + 4), fill=(220, 180, 255, 200))
    img.save(os.path.join(TEX_ENT, "spell_orb.png"))


# ╔════════════════════════════════════════════════════════════════════════
# ║ EXECUTE
# ╚════════════════════════════════════════════════════════════════════════

# Velas: 5 cores × 2 estados
for color_name in CANDLE_COLORS.keys():
    make_candle(color_name, lit=False)
    make_candle(color_name, lit=True)

# Chalks (item): 5 cores + base + observation + blood + chalk_symbol
make_chalk_into_file("chalk", (245, 240, 230))
for cn, c in CHALK_COLORS.items():
    make_chalk(cn, c)
make_blood_chalk()
make_observation_chalk()
make_chalk_symbol_item()

# Chalk marks (chão): 5 cores
for cn, c in CHALK_COLORS.items():
    make_chalk_mark(cn, c)

# Chalk glyphs: 4 variants
for i in range(4):
    make_chalk_glyph(i)

# Chalk symbol no chão
make_chalk_symbol_block()

# Rune block
make_rune_block()

# Mobs
make_void_larva()
make_blood_worm()
make_gore_worm()
make_flesh_crawler()
make_loom_worm()
make_disarmer()
make_weaving_shade()
make_loom_humanoid_skins()
make_spell_orb()

print("r121: regenerated 10 candles, 8 chalks, 5 chalk_marks, 4 chalk_glyphs, "
      "1 chalk_symbol(item), 1 chalk_symbol(block), 1 rune_block, "
      "5 worm mobs, 4 humanoid mobs, 1 spell_orb")
