"""r34: REWRITE de assets ocultos.
- 5 chalk marks com DESIGNS DIFERENTES (não mesmo pentagrama recolorido)
- 5 velas com modelagem profissional (cera derretida + chama + ceramica base)
- 10 sigilos com símbolos GENUINAMENTE diferentes por entidade
"""
import os, math, random
from PIL import Image, ImageDraw, ImageFilter

random.seed(34)
BASE = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources"
TEX_BLOCK = os.path.join(BASE, "assets/liberthia/textures/block")
TEX_ITEM = os.path.join(BASE, "assets/liberthia/textures/item")


# ──────────────────────────────────────────────────────────────────
# CHALK MARKS — design ÚNICO por cor
# ──────────────────────────────────────────────────────────────────

def chalk_pentagram(d, color, size=16):
    """Branco — pentagram clássico, banishing/proteção."""
    cx, cy = size // 2, size // 2
    pts = []
    for i in range(5):
        a = math.radians(-90 + 72 * i)
        pts.append((cx + math.cos(a) * 7, cy + math.sin(a) * 7))
    seq = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    for i in range(5):
        d.line([seq[i], seq[i + 1]], fill=(*color, 255))
    d.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), outline=(*color, 200))


def chalk_hexagram(d, color, size=16):
    """Dourado — Estrela de David, geometria sagrada angélica."""
    cx, cy = size // 2, size // 2
    pts = []
    for i in range(6):
        a = math.radians(60 * i)
        pts.append((cx + math.cos(a) * 6, cy + math.sin(a) * 6))
    d.polygon([pts[0], pts[2], pts[4]], outline=(*color, 255))
    d.polygon([pts[1], pts[3], pts[5]], outline=(*color, 255))
    # ponto central
    d.ellipse((cx - 1, cy - 1, cx + 1, cy + 1), fill=(*color, 255))


def chalk_enochian(d, color, size=16):
    """Roxo — sigilo Enoquiano (cruz dupla + olhos)."""
    cx, cy = size // 2, size // 2
    # cruz central
    d.line([(cx - 6, cy), (cx + 6, cy)], fill=(*color, 255))
    d.line([(cx, cy - 6), (cx, cy + 6)], fill=(*color, 255))
    # X diagonais
    d.line([(cx - 4, cy - 4), (cx + 4, cy + 4)], fill=(*color, 200))
    d.line([(cx + 4, cy - 4), (cx - 4, cy + 4)], fill=(*color, 200))
    # 4 olhos nas pontas
    for dx, dy in [(0, -7), (0, 7), (-7, 0), (7, 0)]:
        d.ellipse((cx + dx - 1, cy + dy - 1, cx + dx + 1, cy + dy + 1), fill=(*color, 255))


def chalk_goetic(d, color, size=16):
    """Vermelho — Sigilo de Bael (Goetia, primeiro demônio)."""
    cx, cy = size // 2, size // 2
    # círculo externo
    d.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), outline=(*color, 255))
    # triângulo invertido (descida)
    d.polygon([(cx - 5, cy - 4), (cx + 5, cy - 4), (cx, cy + 5)], outline=(*color, 255))
    # ondas dentro (caos)
    for i in range(3):
        y = cy + i - 1
        d.line([(cx - 2, y), (cx - 1, y - 1), (cx, y), (cx + 1, y - 1), (cx + 2, y)],
               fill=(*color, 200))


def chalk_necro(d, color, size=16):
    """Preto — cruz invertida + caveira pequena (necromancia)."""
    cx, cy = size // 2, size // 2
    # cruz invertida (vertical longo, horizontal curto no topo)
    d.line([(cx, cy - 6), (cx, cy + 7)], fill=(*color, 255), width=1)
    d.line([(cx - 3, cy - 5), (cx + 3, cy - 5)], fill=(*color, 255))
    # círculo embaixo (foco necromântico)
    d.ellipse((cx - 5, cy + 3, cx + 5, cy + 7), outline=(*color, 255))
    # 2 pontos no círculo (olhos vazios)
    d.point((cx - 2, cy + 5), fill=(*color, 255))
    d.point((cx + 2, cy + 5), fill=(*color, 255))


def gen_chalk(name, color, sym_func):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    sym_func(d, color)
    img.save(os.path.join(TEX_BLOCK, f"chalk_mark_{name}.png"))


# ──────────────────────────────────────────────────────────────────
# VELAS — modelagem detalhada cera + chama + base
# ──────────────────────────────────────────────────────────────────

def gen_candle(name, color, lit=False):
    """Vela detalhada: base cerâmica, cera com drips, pavio, chama (se lit)."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # base cerâmica (8×2 px wide)
    d.rectangle((4, 13, 12, 15), fill=(80, 70, 60, 255))
    d.line([(4, 13), (11, 13)], fill=(120, 100, 80, 255))
    # cera principal (cylindrical body)
    d.rectangle((6, 5, 10, 13), fill=color + (255,))
    # highlight esquerdo (brilho lateral)
    light = (min(color[0] + 60, 255), min(color[1] + 60, 255), min(color[2] + 60, 255))
    d.rectangle((6, 5, 7, 13), fill=light + (255,))
    # shadow direito
    shadow = (max(color[0] - 50, 0), max(color[1] - 50, 0), max(color[2] - 50, 0))
    d.rectangle((9, 5, 10, 13), fill=shadow + (255,))
    # cera derretida (drips irregulares)
    drips = [(5, 9), (10, 7), (5, 11), (10, 10)]
    for dx, dy in drips:
        d.point((dx, dy), fill=light + (255,))
        d.point((dx, dy + 1), fill=color + (255,))
    # pavio (preto/marrom)
    d.line([(8, 3), (8, 5)], fill=(30, 20, 10, 255))
    if lit:
        # chama (gradient laranja → branco)
        d.polygon([(8, 0), (7, 2), (8, 3)], fill=(255, 200, 80, 255))
        d.polygon([(8, 0), (9, 2), (8, 3)], fill=(255, 220, 130, 255))
        d.polygon([(8, 1), (8, 3)], fill=(255, 255, 200, 255))
        # halo glow (1px alpha)
        for dx, dy in [(7, 0), (9, 0), (8, -1)]:
            if 0 <= dy < 16 and 0 <= dx < 16:
                d.point((dx, dy), fill=(255, 220, 100, 100))
    img.save(os.path.join(TEX_BLOCK,
                          f"candle_occult_{name}" + ("_lit" if lit else "") + ".png"))


# ──────────────────────────────────────────────────────────────────
# SIGILOS — 10 designs ÚNICOS, cada um com símbolo distinto
# ──────────────────────────────────────────────────────────────────

def sigil_base(d, color, parchment_color=(240, 220, 180)):
    """Background pergaminho consistente."""
    # Pergaminho com bordas tortas
    d.polygon([(2, 2), (14, 2), (14, 14), (2, 14)], fill=parchment_color + (255,))
    # Bordas mais escuras (envelhecido)
    border = (parchment_color[0] - 30, parchment_color[1] - 40, parchment_color[2] - 50)
    d.rectangle((2, 2, 14, 2), fill=border + (255,))
    d.rectangle((2, 14, 14, 14), fill=border + (255,))
    # Manchas (3 manchas marrons aleatórias)
    for _ in range(3):
        x, y = random.randint(3, 13), random.randint(3, 13)
        d.point((x, y), fill=(120, 90, 50, 180))


def sigil_foliot(d):
    """Foliot — raízes/galhos (espírito da terra)."""
    sigil_base(d, (140, 100, 60))
    # Tronco vertical
    d.line([(8, 4), (8, 12)], fill=(60, 40, 20, 255), width=1)
    # Galhos laterais
    d.line([(8, 6), (5, 4)], fill=(60, 40, 20, 255))
    d.line([(8, 6), (11, 4)], fill=(60, 40, 20, 255))
    d.line([(8, 9), (4, 8)], fill=(60, 40, 20, 255))
    d.line([(8, 9), (12, 8)], fill=(60, 40, 20, 255))
    # Folhas (verdes pequenas)
    for x, y in [(5, 3), (11, 3), (4, 7), (12, 7)]:
        d.point((x, y), fill=(80, 140, 50, 255))


def sigil_djinni(d):
    """Djinni — espiral de vento + meia-lua (espírito do ar/Arábia)."""
    sigil_base(d, (180, 200, 220))
    # Meia-lua (símbolo árabe)
    d.arc([6, 4, 12, 10], 270, 90, fill=(60, 80, 120, 255), width=2)
    # Espiral de vento (Bezier-ish)
    for t in range(0, 90, 5):
        a = math.radians(t * 4)
        r = 0.5 + t * 0.05
        x = 6 + math.cos(a) * r
        y = 10 + math.sin(a) * r
        if 3 <= x <= 13 and 3 <= y <= 13:
            d.point((x, y), fill=(80, 120, 180, 255))


def sigil_afrit(d):
    """Afrit — chamas estilizadas em triângulo (fogo árabe)."""
    sigil_base(d, (200, 150, 100))
    # Triângulo de fogo
    d.polygon([(5, 12), (11, 12), (8, 4)], outline=(180, 40, 20, 255))
    # Chamas dentro (3 ondas)
    for y, w in [(11, 4), (9, 3), (7, 2)]:
        d.line([(8 - w, y), (8 + w, y)], fill=(220, 80, 30, 255))
    d.point((8, 5), fill=(255, 200, 100, 255))  # ponta da chama


def sigil_bael(d):
    """Bael — sigilo goético REAL (cruz com gancho + círculo)."""
    sigil_base(d, (180, 100, 100))
    cx, cy = 8, 9
    # Sigilo de Bael (real, do Lemegeton): cruz com hooks nas laterais
    d.line([(cx, cy - 4), (cx, cy + 4)], fill=(120, 20, 30, 255))  # vertical
    d.line([(cx - 3, cy), (cx + 3, cy)], fill=(120, 20, 30, 255))  # horizontal
    # Ganchos
    d.line([(cx - 3, cy), (cx - 3, cy - 2)], fill=(120, 20, 30, 255))
    d.line([(cx + 3, cy), (cx + 3, cy + 2)], fill=(120, 20, 30, 255))
    d.line([(cx, cy - 4), (cx + 1, cy - 4)], fill=(120, 20, 30, 255))
    d.line([(cx, cy + 4), (cx - 1, cy + 4)], fill=(120, 20, 30, 255))
    # Círculo embaixo
    d.ellipse((cx - 2, cy + 4, cx + 2, cy + 6), outline=(120, 20, 30, 255))


def sigil_lucifer(d):
    """Lúcifer — Sigil of Lucifer (real, do Grimorium Verum)."""
    sigil_base(d, (240, 220, 100))
    cx, cy = 8, 9
    # V invertido (cornos)
    d.line([(cx - 4, cy + 3), (cx, cy - 3)], fill=(150, 100, 30, 255))
    d.line([(cx + 4, cy + 3), (cx, cy - 3)], fill=(150, 100, 30, 255))
    # Triângulo invertido dentro
    d.polygon([(cx - 2, cy - 1), (cx + 2, cy - 1), (cx, cy + 2)],
              outline=(180, 130, 50, 255))
    # X no fundo
    d.line([(cx - 3, cy + 5), (cx + 3, cy + 5)], fill=(150, 100, 30, 255))


def sigil_sandalphon(d):
    """Sandalphon — orações ascendendo (linhas verticais + olhos)."""
    sigil_base(d, (240, 230, 200))
    # Pilar central (oração subindo)
    d.line([(8, 4), (8, 13)], fill=(180, 140, 80, 255), width=1)
    # Linhas ascendentes pequenas (asas/penas)
    for y in [11, 9, 7]:
        d.line([(6, y), (8, y - 1)], fill=(180, 140, 80, 255))
        d.line([(10, y), (8, y - 1)], fill=(180, 140, 80, 255))
    # Coroa no topo (Malkuth)
    d.line([(6, 4), (10, 4)], fill=(220, 180, 100, 255))
    d.point((6, 3), fill=(220, 180, 100, 255))
    d.point((8, 3), fill=(220, 180, 100, 255))
    d.point((10, 3), fill=(220, 180, 100, 255))


def sigil_metatron(d):
    """Metatron — cubo de Metatron (13 círculos conectados)."""
    sigil_base(d, (255, 240, 200))
    # 7 círculos: 1 central + 6 satélites
    centers = [(8, 9)]
    for i in range(6):
        a = math.radians(60 * i)
        centers.append((8 + math.cos(a) * 3, 9 + math.sin(a) * 3))
    # Linhas conectando todos com todos (simplificado)
    for i, c1 in enumerate(centers):
        for c2 in centers[i + 1:]:
            d.line([c1, c2], fill=(180, 140, 60, 100))
    # Círculos por cima
    for cx, cy in centers:
        d.ellipse((cx - 1, cy - 1, cx + 1, cy + 1),
                  outline=(150, 110, 40, 255))


def sigil_necro(d):
    """Necromante — caveira estilizada."""
    sigil_base(d, (100, 90, 90))
    cx, cy = 8, 9
    # Crânio
    d.ellipse((cx - 3, cy - 3, cx + 3, cy + 2), fill=(220, 200, 180, 255))
    # Olhos vazios
    d.point((cx - 1, cy - 1), fill=(0, 0, 0, 255))
    d.point((cx + 1, cy - 1), fill=(0, 0, 0, 255))
    # Mandíbula
    d.rectangle((cx - 2, cy + 2, cx + 2, cy + 4), fill=(200, 180, 160, 255))
    for x in [cx - 1, cx + 1]:
        d.point((x, cy + 3), fill=(0, 0, 0, 255))
    # Hexagrama atrás (poder necromântico)
    pts = []
    for i in range(6):
        a = math.radians(60 * i)
        pts.append((cx + math.cos(a) * 5, cy + math.sin(a) * 5))
    d.polygon([pts[0], pts[2], pts[4]], outline=(60, 30, 30, 200))
    d.polygon([pts[1], pts[3], pts[5]], outline=(60, 30, 30, 200))


def sigil_banishing(d):
    """Banimento — pentagrama do LBRP da Golden Dawn (apontando pra fora)."""
    sigil_base(d, (240, 240, 240))
    cx, cy = 8, 9
    # Círculo
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), outline=(80, 80, 80, 255))
    # Pentagrama RIGHT-SIDE-UP (banishing)
    pts = []
    for i in range(5):
        a = math.radians(-90 + 72 * i)
        pts.append((cx + math.cos(a) * 4.5, cy + math.sin(a) * 4.5))
    seq = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    for i in range(5):
        d.line([seq[i], seq[i + 1]], fill=(120, 120, 200, 255))


def sigil_dimensional(d):
    """Salto Dimensional — espiral + olho central (portal)."""
    sigil_base(d, (180, 150, 220))
    cx, cy = 8, 9
    # Espiral
    for t in range(0, 720, 10):
        a = math.radians(t)
        r = 0.5 + t * 0.005
        x = cx + math.cos(a) * r
        y = cy + math.sin(a) * r
        if 3 <= x <= 13 and 3 <= y <= 13:
            d.point((x, y), fill=(80, 40, 150, 255))
    # Olho no centro
    d.ellipse((cx - 2, cy - 1, cx + 2, cy + 1), fill=(255, 240, 200, 255))
    d.point((cx, cy), fill=(0, 0, 0, 255))


def gen_sigil(name, sym_func):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    sym_func(d)
    img.save(os.path.join(TEX_ITEM, f"sigil_{name}.png"))


# ──────────────────────────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    # CHALKS — designs únicos por cor
    gen_chalk("white",  (240, 240, 240), chalk_pentagram)
    gen_chalk("golden", (255, 215, 50), chalk_hexagram)
    gen_chalk("purple", (170, 90, 220), chalk_enochian)
    gen_chalk("red",    (220, 40, 60), chalk_goetic)
    gen_chalk("black",  (40, 40, 40), chalk_necro)
    print("CHALK MARKS — 5 unique designs generated.")

    # VELAS — detalhadas (unlit + lit)
    for name, color in [
        ("white",  (240, 240, 240)),
        ("golden", (220, 180, 80)),
        ("purple", (140, 80, 200)),
        ("red",    (200, 40, 60)),
        ("black",  (40, 40, 40)),
    ]:
        gen_candle(name, color, lit=False)
        gen_candle(name, color, lit=True)
    print("CANDLES — 5 colors × 2 states (lit/unlit) = 10 textures generated.")

    # SIGILOS — 10 designs únicos
    gen_sigil("foliot",       sigil_foliot)
    gen_sigil("djinni",       sigil_djinni)
    gen_sigil("afrit",        sigil_afrit)
    gen_sigil("bael",         sigil_bael)
    gen_sigil("lucifer",      sigil_lucifer)
    gen_sigil("sandalphon",   sigil_sandalphon)
    gen_sigil("metatron",     sigil_metatron)
    gen_sigil("necro",        sigil_necro)
    gen_sigil("banishing",    sigil_banishing)
    gen_sigil("dimensional",  sigil_dimensional)
    print("SIGILS — 10 UNIQUE designs (raízes, meia-lua, chamas, Bael real, Lucifer real, etc) generated.")
