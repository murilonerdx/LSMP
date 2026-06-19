"""r121 v2: Refinar — preencher 100% do canvas dos mobs + chalks com stick mais grosso/cor saturada.

Diferenças vs v1:
- Mobs: pintar TODOS os 64×32 / 64×64 pixels (sem alpha 0). Vanilla UV não fica visivel se houver pixel transparente em UV usada.
- Chalks: stick 3-pixel wide horizontal-diagonal com cor central pura + side highlights claros.
"""
from PIL import Image, ImageDraw
import os, math, random

ROOT = os.path.dirname(__file__)
TEX_BLOCK = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/block"))
TEX_ITEM  = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/item"))
TEX_ENT   = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/entity"))


# ═══════════════════════════════════════════════════════════════════════════
# CHALKS — Refinados (stick mais GROSSO, cor mais SATURADA, ponta clara)
# ═══════════════════════════════════════════════════════════════════════════
def make_chalk_v2(filepath, color):
    """Chalk 16×16: stick diagonal 4 pixels de largura na direção perpendicular,
    cor central PURA, com gradient lateral, ponta white claro."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = color + (255,)
    cshadow = (max(0, c[0] - 60), max(0, c[1] - 60), max(0, c[2] - 60), 255)
    chl = (min(255, c[0] + 50), min(255, c[1] + 50), min(255, c[2] + 50), 255)
    cmid = (min(255, c[0] + 20), min(255, c[1] + 20), min(255, c[2] + 20), 255)

    # Diagonal de (2,13) até (13,2)
    # Para cada ponto da linha central, pinta uma "secção" perpendicular de 4 pixels
    for t in range(0, 12):
        # Centro da seção
        cx = 2 + t
        cy = 13 - t
        # Pinta a coluna perpendicular ao stick
        # Como diagonal de 45°, perpendicular = (1,1) direction
        # offsets em (+1,+1), (0,0), (-1,-1), (-2,-2)
        # Mas tô fazendo o stick "espesso na horizontal" — coluna vertical 4px
        for ox, oy, role in [(-1, -1, "hl"), (0, 0, "core"), (1, 1, "shadow"), (-2, -2, "hl_dim")]:
            px, py = cx + ox, cy + oy
            if 0 <= px < 16 and 0 <= py < 16:
                if role == "core":
                    d.point((px, py), fill=c)
                elif role == "hl":
                    d.point((px, py), fill=chl)
                elif role == "shadow":
                    d.point((px, py), fill=cshadow)
                elif role == "hl_dim":
                    d.point((px, py), fill=cmid)

    # TOP TIP (cantosup-direita) — ponta usada com dust branco
    d.point((13, 2), fill=(255, 255, 255, 255))
    d.point((14, 2), fill=(255, 255, 255, 220))
    d.point((13, 3), fill=chl)
    d.point((14, 3), fill=(c[0], c[1], c[2], 180))

    # BOTTOM TIP — base segurada (escura)
    d.point((2, 13), fill=cshadow)
    d.point((1, 14), fill=cshadow)
    d.point((2, 14), fill=cshadow)

    img.save(filepath)


def make_chalk_symbol_item_v2():
    """chalk_symbol item — sigilo claro: triângulo com olho dentro."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    # Outer ring
    d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], outline=(255, 250, 220, 255))
    # Inner triangle (eye of providence)
    d.polygon([(cx, cy - 4), (cx - 4, cy + 3), (cx + 4, cy + 3)],
              outline=(255, 250, 220, 255))
    # Eye em centro
    d.ellipse([cx - 2, cy - 1, cx + 1, cy + 1], fill=(50, 30, 80, 255))
    d.point((cx, cy), fill=(255, 220, 100, 255))
    # 4 sparkles
    d.point((cx - 5, cy - 5), fill=(255, 220, 100, 200))
    d.point((cx + 5, cy - 5), fill=(255, 220, 100, 200))
    d.point((cx - 5, cy + 5), fill=(255, 220, 100, 200))
    d.point((cx + 5, cy + 5), fill=(255, 220, 100, 200))
    img.save(os.path.join(TEX_ITEM, "chalk_symbol.png"))


# ═══════════════════════════════════════════════════════════════════════════
# MOBS — Preencher 100% do canvas com cor base
# ═══════════════════════════════════════════════════════════════════════════
def make_silverfish_full(filepath, body_color, accent_color, segments=True, glow_back=False):
    """Pinta 64×32 INTEIRO com gradient da body_color (sem alpha 0)."""
    img = Image.new("RGBA", (64, 32), body_color + (255,))
    d = ImageDraw.Draw(img)

    bshadow = (max(0, body_color[0] - 40),
               max(0, body_color[1] - 40),
               max(0, body_color[2] - 40), 255)
    bhl = (min(255, body_color[0] + 35),
           min(255, body_color[1] + 35),
           min(255, body_color[2] + 35), 255)
    accent = accent_color + (255,)

    # Texture noise sutil — variação por linha
    random.seed(7)
    for y in range(32):
        for x in range(64):
            r_var = random.randint(-12, 12)
            base = img.getpixel((x, y))
            new = (max(0, min(255, base[0] + r_var)),
                   max(0, min(255, base[1] + r_var)),
                   max(0, min(255, base[2] + r_var)),
                   255)
            img.putpixel((x, y), new)
    d = ImageDraw.Draw(img)

    if segments:
        # Stripes horizontais a cada 4 pixels (segmento body)
        for y in [4, 8, 12, 16, 20, 24, 28]:
            for x in range(64):
                d.point((x, y), fill=bshadow)

    # Highlight stripe top
    for x in range(64):
        d.point((x, 0), fill=bhl)
        d.point((x, 1), fill=bhl)

    # 4 olhos (head UV) na coluna superior — silverfish UV head em (0,0)-(28,8)
    # Olhos esquerdos
    for ox, oy in [(6, 4), (7, 4), (6, 5), (7, 5)]:
        d.point((ox, oy), fill=accent)
    # Olhos direitos (espelhado pra head right face)
    for ox, oy in [(18, 4), (19, 4), (18, 5), (19, 5)]:
        d.point((ox, oy), fill=accent)

    if glow_back:
        # Glow dots ao longo do corpo (body UV: ~24..40 x, 8..16 y)
        for x in [26, 30, 34, 38]:
            d.point((x, 12), fill=accent)
            d.point((x, 13), fill=accent)

    img.save(filepath)


def make_humanoid_full(filepath, base_color, shirt_color, accent_color,
                        glow_eyes=True, robe=False):
    """Pinta 64×64 INTEIRO. Skin = base_color por trás, shirt sobreposto, accent eyes."""
    # Background = shirt color (cobre tudo que não for repintado)
    img = Image.new("RGBA", (64, 64), shirt_color + (255,))
    d = ImageDraw.Draw(img)

    skin = base_color + (255,)
    skin_shadow = (max(0, base_color[0] - 30), max(0, base_color[1] - 30),
                   max(0, base_color[2] - 30), 255)
    shirt = shirt_color + (255,)
    sshadow = (max(0, shirt_color[0] - 40), max(0, shirt_color[1] - 40),
               max(0, shirt_color[2] - 40), 255)
    shl = (min(255, shirt_color[0] + 30), min(255, shirt_color[1] + 30),
           min(255, shirt_color[2] + 30), 255)

    def paint(x0, y0, w, h, color):
        d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=color)

    # === HEAD === UV (0,0)-(64,16) covers all 6 faces (32 wide per "row" of 6 faces in steve format)
    # We'll fill top half with skin
    paint(0, 0, 64, 16, skin)
    # Eyes — front face is (8,8)-(16,16)
    eye_c = accent_color + (255,) if glow_eyes else (15, 15, 15, 255)
    paint(9, 11, 2, 2, eye_c)
    paint(13, 11, 2, 2, eye_c)
    # Mouth
    d.line([(10, 14), (13, 14)], fill=skin_shadow)
    # Head shading — bottom of head bottom face (8,0)-(16,8) keep skin
    # Add gradient — face slightly darker near jaw
    for x in range(8, 16):
        d.point((x, 15), fill=skin_shadow)

    # === BODY === UV (16,16)-(40,32) — vest/shirt
    paint(16, 16, 24, 16, shirt)
    # Top highlight
    for x in range(20, 28):
        d.point((x, 16), fill=shl)
    # Robe down center
    if robe:
        for y in range(20, 32):
            d.point((28, y), fill=sshadow)
            d.point((27, y), fill=sshadow)
    # Body bottom shadow
    for x in range(20, 28):
        d.point((x, 31), fill=sshadow)

    # === ARMS === Right arm (40,16)-(56,32). Left arm (32,48)-(48,64) Steve format
    paint(40, 16, 16, 16, shirt)
    paint(32, 48, 16, 16, shirt)
    # Wrist skin showing
    paint(40, 28, 8, 4, skin)
    paint(32, 60, 8, 4, skin)
    # Arm highlights
    for x in range(44, 52):
        d.point((x, 16), fill=shl)
    for x in range(36, 44):
        d.point((x, 48), fill=shl)

    # === LEGS === Right leg (0,16)-(16,32). Left leg (16,48)-(32,64)
    paint(0, 16, 16, 16, sshadow)
    paint(16, 48, 16, 16, sshadow)
    # Boots at the bottom (last 4 rows)
    paint(0, 28, 16, 4, (20, 15, 10, 255))
    paint(16, 60, 16, 4, (20, 15, 10, 255))

    # === REMAINING AREAS to NOT be transparent ===
    # The body layer (overlay) goes (16,32)-(40,48). Fill with same shirt.
    paint(16, 32, 24, 16, shirt)
    # Arm overlay 2nd layer (48,48)-(64,64) AND (40,32)-(56,48)
    paint(40, 32, 16, 16, shirt)
    paint(48, 48, 16, 16, shirt)
    # Leg overlay 2nd layer (0,32)-(16,48) AND (0,48)-(16,64)
    paint(0, 32, 16, 16, sshadow)
    paint(0, 48, 16, 16, sshadow)

    img.save(filepath)


# ═══════════════════════════════════════════════════════════════════════════
# Execute
# ═══════════════════════════════════════════════════════════════════════════
CHALK_PALETTE = {
    "chalk":  (245, 240, 230),
    "white":  (245, 240, 230),
    "black":  (35, 32, 32),
    "red":    (215, 45, 45),
    "purple": (170, 80, 220),
    "golden": (240, 195, 70),
}

for name, color in CHALK_PALETTE.items():
    make_chalk_v2(os.path.join(TEX_ITEM, f"{'chalk' if name == 'chalk' else 'chalk_' + name}.png"), color)

# blood_chalk
make_chalk_v2(os.path.join(TEX_ITEM, "blood_chalk.png"), (160, 30, 30))
# observation_chalk
make_chalk_v2(os.path.join(TEX_ITEM, "observation_chalk.png"), (120, 100, 220))
# chalk_symbol (item)
make_chalk_symbol_item_v2()

# === MOBS: refazer com canvas preenchido ===
make_silverfish_full(os.path.join(TEX_ENT, "void_larva", "void_larva.png"),
                      body_color=(85, 35, 130), accent_color=(255, 100, 250),
                      segments=True, glow_back=True)
make_silverfish_full(os.path.join(TEX_ENT, "blood_worm.png"),
                      body_color=(155, 35, 45), accent_color=(255, 220, 50),
                      segments=True, glow_back=False)
make_silverfish_full(os.path.join(TEX_ENT, "gore_worm.png"),
                      body_color=(180, 65, 55), accent_color=(255, 100, 100),
                      segments=True, glow_back=False)
make_silverfish_full(os.path.join(TEX_ENT, "flesh_crawler.png"),
                      body_color=(195, 130, 130), accent_color=(255, 70, 70),
                      segments=True, glow_back=False)
make_silverfish_full(os.path.join(TEX_ENT, "loom_worm.png"),
                      body_color=(75, 55, 105), accent_color=(170, 130, 230),
                      segments=True, glow_back=True)

make_humanoid_full(os.path.join(TEX_ENT, "disarmer.png"),
                    base_color=(85, 75, 70), shirt_color=(45, 35, 35),
                    accent_color=(230, 70, 70), glow_eyes=True, robe=False)
make_humanoid_full(os.path.join(TEX_ENT, "weaving_shade.png"),
                    base_color=(45, 35, 55), shirt_color=(60, 40, 80),
                    accent_color=(210, 110, 255), glow_eyes=True, robe=True)
make_humanoid_full(os.path.join(TEX_ENT, "loom_watcher.png"),
                    base_color=(50, 40, 60), shirt_color=(70, 50, 100),
                    accent_color=(255, 70, 200), glow_eyes=True, robe=True)
make_humanoid_full(os.path.join(TEX_ENT, "loom_peripheral.png"),
                    base_color=(55, 45, 65), shirt_color=(80, 60, 110),
                    accent_color=(255, 110, 220), glow_eyes=True, robe=True)
make_humanoid_full(os.path.join(TEX_ENT, "loom_screamer.png"),
                    base_color=(75, 40, 45), shirt_color=(125, 50, 60),
                    accent_color=(255, 220, 80), glow_eyes=True, robe=True)

print("r121 v2: refined chalks (saturated stick) + 5 worm mobs + 4 humanoid mobs (full canvas painted)")
