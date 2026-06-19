"""Generator de texturas ocultas REAIS — sigilos goéticos, pentagramas
enoquianos, geometria sephirot, máscaras tribais. 32×32 pra detalhe + downscale.
"""
import os, json, math, random
from PIL import Image, ImageDraw, ImageFilter

random.seed(666)
BASE = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources"
TEX_ITEM = os.path.join(BASE, "assets/liberthia/textures/item")
os.makedirs(TEX_ITEM, exist_ok=True)

SIZE = 32  # render 32x32 e downscale pra 16x16 pra mais detalhe


def new_img():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def downscale(img):
    return img.resize((16, 16), Image.LANCZOS)


def hex_lines(d, cx, cy, r, color):
    """Hexagram (estrela de 6 pontas)."""
    pts = []
    for i in range(6):
        a = math.radians(60 * i)
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    # 2 triângulos
    d.polygon([pts[0], pts[2], pts[4]], outline=color, width=1)
    d.polygon([pts[1], pts[3], pts[5]], outline=color, width=1)


def penta_lines(d, cx, cy, r, color, inverted=False):
    """Pentagrama. Inverted = ponta pra baixo (selo de Baphomet)."""
    pts = []
    for i in range(5):
        a = math.radians(-90 + 72 * i + (180 if inverted else 0))
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    seq = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    for i in range(5):
        d.line([seq[i], seq[i + 1]], fill=color, width=1)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=1)


def enoch_sigil(d, cx, cy, r, color):
    """Sigilo enoquiano — círculo c/ cruz interna + arcos."""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=1)
    d.ellipse([cx - r + 2, cy - r + 2, cx + r - 2, cy + r - 2], outline=color)
    d.line([(cx - r, cy), (cx + r, cy)], fill=color)
    d.line([(cx, cy - r), (cx, cy + r)], fill=color)
    for i in range(4):
        a = math.radians(45 + 90 * i)
        x = cx + math.cos(a) * (r - 3)
        y = cy + math.sin(a) * (r - 3)
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=color)


def goetic_seal(d, cx, cy, r, color1, color2):
    """Sigilo goético — círculos concêntricos + linhas radiais + glifo central."""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color1, width=2)
    d.ellipse([cx - r + 3, cy - r + 3, cx + r - 3, cy + r - 3], outline=color2)
    # 8 linhas radiais
    for i in range(8):
        a = math.radians(45 * i)
        x1 = cx + math.cos(a) * (r - 4)
        y1 = cy + math.sin(a) * (r - 4)
        x2 = cx + math.cos(a) * r
        y2 = cy + math.sin(a) * r
        d.line([(x1, y1), (x2, y2)], fill=color1)
    # glifo central — V invertido + ponto
    d.line([(cx - 3, cy + 2), (cx, cy - 3)], fill=color2)
    d.line([(cx, cy - 3), (cx + 3, cy + 2)], fill=color2)
    d.ellipse([cx - 1, cy + 2, cx + 1, cy + 4], fill=color2)


def sephirot_tree(d, cx, cy, color):
    """Mini Árvore da Vida sephirot — 10 círculos conectados."""
    sef_pos = [
        (cx, cy - 12),  # Kether
        (cx - 5, cy - 7), (cx + 5, cy - 7),  # Chokmah, Binah
        (cx - 5, cy - 2), (cx + 5, cy - 2),  # Chesed, Geburah
        (cx, cy + 1),  # Tiphareth
        (cx - 5, cy + 4), (cx + 5, cy + 4),  # Netzach, Hod
        (cx, cy + 7),  # Yesod
        (cx, cy + 12),  # Malkuth
    ]
    edges = [(0,1),(0,2),(1,2),(1,3),(2,4),(3,4),(3,5),(4,5),(5,6),(5,7),(6,7),(6,8),(7,8),(8,9),(0,5),(1,5),(2,5),(3,6),(4,7)]
    for a, b in edges:
        d.line([sef_pos[a], sef_pos[b]], fill=color)
    for p in sef_pos:
        d.ellipse([p[0] - 1.5, p[1] - 1.5, p[0] + 1.5, p[1] + 1.5], fill=color)


def add_glow(img, center=(16, 16), radius=10, color=(200, 150, 255)):
    """Adiciona glow radial."""
    glow = Image.new('RGBA', img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    for r in range(radius, 0, -1):
        a = int(40 * (1 - r / radius))
        d.ellipse([center[0] - r, center[1] - r, center[0] + r, center[1] + r],
                  fill=(color[0], color[1], color[2], a))
    glow = glow.filter(ImageFilter.GaussianBlur(2))
    return Image.alpha_composite(glow, img)


# ──────────────── COSMIC HORROR ITEMS ────────────────

def make_whispering_veil():
    """Véu fantasmagórico com olho central."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # cloth folds
    for y in range(8, 28):
        offset = int(math.sin(y / 3.0) * 2)
        d.line([(6 + offset, y), (26 + offset, y)],
               fill=(80, 40, 110, 220 - y * 3))
    # central eye
    d.ellipse((12, 14, 20, 22), fill=(220, 180, 255, 255))
    d.ellipse((14, 16, 18, 20), fill=(20, 0, 30, 255))
    d.ellipse((15, 17, 17, 19), fill=(255, 255, 255, 255))
    return downscale(add_glow(img, (16, 18), 8, (180, 100, 220)))


def make_eyes_of_abyss():
    """Olho de Azathoth — espiral fractal com pupila vertical."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # outer ring
    d.ellipse((4, 4, 28, 28), fill=(60, 30, 100, 255))
    d.ellipse((6, 6, 26, 26), fill=(30, 0, 60, 255))
    # spiral fragments
    for t in range(0, 360, 15):
        a = math.radians(t)
        r1 = 8 + (t / 45) * 0.5
        r2 = r1 + 1.5
        x1 = 16 + math.cos(a) * r1
        y1 = 16 + math.sin(a) * r1
        x2 = 16 + math.cos(a) * r2
        y2 = 16 + math.sin(a) * r2
        d.line([(x1, y1), (x2, y2)], fill=(180, 100, 240, 255))
    # vertical slit pupil
    d.ellipse((14, 8, 18, 24), fill=(255, 220, 80, 255))
    d.ellipse((15, 12, 17, 20), fill=(0, 0, 0, 255))
    return downscale(add_glow(img, (16, 16), 12, (200, 100, 255)))


def make_cursed_cradle():
    """Berço de Lilith — coruna invertida + lágrima de sangue."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # inverted crown (Lilith / fallen)
    d.polygon([(8, 18), (12, 8), (14, 12), (16, 6), (18, 12), (20, 8), (24, 18),
               (24, 22), (8, 22)], fill=(40, 10, 20, 255))
    d.line([(8, 18), (24, 18)], fill=(120, 30, 40, 255))
    # blood drop
    d.polygon([(16, 24), (13, 28), (16, 30), (19, 28)], fill=(180, 20, 40, 255))
    d.ellipse((15, 25, 17, 27), fill=(255, 80, 100, 255))
    # 3 small skulls inside crown
    for x in [11, 16, 21]:
        d.ellipse((x - 2, 14, x + 2, 18), fill=(220, 200, 180, 255))
        d.point((x - 1, 16), fill=(0, 0, 0, 255))
        d.point((x + 1, 16), fill=(0, 0, 0, 255))
    return downscale(img)


def make_pendulum_of_dread():
    """Pêndulo de Foucault — bola com glifo + corrente."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # chain
    for y in range(2, 16, 2):
        d.point((16, y), fill=(180, 180, 200, 255))
    # large ball
    d.ellipse((6, 16, 26, 30), fill=(80, 70, 110, 255))
    d.ellipse((8, 18, 24, 28), fill=(120, 100, 150, 255))
    # central glyph — hexagram
    hex_lines(d, 16, 23, 4, (200, 180, 240, 255))
    # highlight
    d.ellipse((10, 19, 14, 22), fill=(220, 200, 255, 200))
    return downscale(add_glow(img, (16, 23), 7, (150, 100, 220)))


def make_lantern_of_false_memory():
    """Lanterna hauntológica — chama fantasma azul cyan."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # handle
    d.line([(16, 2), (16, 8)], fill=(100, 100, 120, 255))
    d.ellipse((14, 1, 18, 5), outline=(140, 140, 160, 255))
    # body (rusted iron)
    d.rectangle((8, 9, 24, 26), fill=(50, 45, 60, 255))
    d.rectangle((9, 10, 23, 25), fill=(70, 65, 80, 255))
    # window
    d.rectangle((11, 12, 21, 23), fill=(10, 30, 60, 255))
    # ghostly flame
    d.polygon([(16, 22), (13, 17), (15, 14), (16, 11), (17, 14), (19, 17)],
              fill=(80, 200, 255, 200))
    d.polygon([(16, 21), (15, 18), (16, 16), (17, 18)],
              fill=(180, 240, 255, 255))
    # ghost wisps
    for i in range(4):
        x = random.randint(11, 21)
        y = random.randint(13, 22)
        d.point((x, y), fill=(220, 240, 255, 180))
    # base
    d.rectangle((6, 26, 26, 28), fill=(40, 35, 50, 255))
    return downscale(add_glow(img, (16, 17), 8, (80, 200, 255)))


def make_tongue_of_old_ones():
    """Língua glossolalia — boca aberta com língua bifurcada + glifos."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # mouth opening
    d.polygon([(8, 14), (24, 14), (22, 22), (10, 22)], fill=(20, 0, 10, 255))
    # teeth top
    for x in range(8, 24, 3):
        d.polygon([(x, 14), (x + 1, 17), (x + 2, 14)], fill=(240, 240, 220, 255))
    # teeth bottom
    for x in range(10, 22, 3):
        d.polygon([(x, 22), (x + 1, 19), (x + 2, 22)], fill=(240, 240, 220, 255))
    # forked tongue
    d.line([(16, 24), (16, 20)], fill=(180, 40, 60, 255), width=2)
    d.line([(16, 20), (13, 17)], fill=(180, 40, 60, 255), width=2)
    d.line([(16, 20), (19, 17)], fill=(180, 40, 60, 255), width=2)
    d.line([(13, 17), (12, 14)], fill=(220, 80, 100, 255))
    d.line([(19, 17), (20, 14)], fill=(220, 80, 100, 255))
    # enochian sigils around
    enoch_sigil(d, 5, 5, 3, (150, 50, 80, 255))
    enoch_sigil(d, 27, 5, 3, (150, 50, 80, 255))
    enoch_sigil(d, 5, 27, 3, (150, 50, 80, 255))
    enoch_sigil(d, 27, 27, 3, (150, 50, 80, 255))
    return downscale(img)


def make_hourglass_of_regression():
    """Ampulheta de Cronos — areia azul fluindo pra cima (tempo invertido)."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # frame
    d.line([(7, 3), (25, 3)], fill=(180, 140, 80, 255), width=2)
    d.line([(7, 28), (25, 28)], fill=(180, 140, 80, 255), width=2)
    # glass (upper + lower bulbs)
    d.polygon([(8, 4), (24, 4), (16, 15)], outline=(220, 220, 240, 255))
    d.polygon([(16, 16), (8, 27), (24, 27)], outline=(220, 220, 240, 255))
    # sand IN UPPER (filling — time going BACK)
    d.polygon([(10, 5), (22, 5), (16, 14)], fill=(100, 180, 255, 220))
    # sand DRAIN at top — small amount IN LOWER
    d.polygon([(15, 16), (17, 16), (16, 17)], fill=(100, 180, 255, 220))
    d.line([(16, 13), (16, 18)], fill=(150, 220, 255, 255))
    # cronos glyph at center
    d.ellipse((14, 14, 18, 18), fill=(200, 230, 255, 255))
    return downscale(add_glow(img, (16, 12), 6, (100, 180, 255)))


def make_void_seer_orb():
    """Orbe de Nyarlathotep — esfera vazio com tentáculos."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # outer void
    d.ellipse((4, 4, 28, 28), fill=(10, 0, 20, 255))
    # event horizon swirl
    for r in range(11, 4, -1):
        c = int(180 - r * 12)
        d.ellipse((16 - r, 16 - r, 16 + r, 16 + r),
                  outline=(c, c // 2, c, 255))
    # tentacles emerging
    for i in range(6):
        a = math.radians(60 * i + 15)
        x1 = 16 + math.cos(a) * 12
        y1 = 16 + math.sin(a) * 12
        x2 = 16 + math.cos(a) * 15
        y2 = 16 + math.sin(a) * 15
        d.line([(x1, y1), (x2, y2)], fill=(80, 20, 100, 255), width=2)
        # curl at end
        cx = x2 + math.cos(a + 0.5) * 1.5
        cy = y2 + math.sin(a + 0.5) * 1.5
        d.line([(x2, y2), (cx, cy)], fill=(80, 20, 100, 255))
    # central white eye
    d.ellipse((14, 14, 18, 18), fill=(255, 255, 255, 255))
    d.ellipse((15, 15, 17, 17), fill=(0, 0, 0, 255))
    return downscale(add_glow(img, (16, 16), 14, (150, 50, 200)))


# ──────────────── ANGEL / CEREMONIAL MAGIC ────────────────

def make_halo_of_light():
    """Sigilo de Metatron — cubo de Metatron (13 círculos fractal)."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # central + 6 satélites primários + 6 secundários (Metatron's Cube simplificado)
    centers = [(16, 16)]
    r1 = 6
    for i in range(6):
        a = math.radians(60 * i)
        centers.append((16 + math.cos(a) * r1, 16 + math.sin(a) * r1))
    r2 = 12
    for i in range(6):
        a = math.radians(60 * i + 30)
        centers.append((16 + math.cos(a) * r2, 16 + math.sin(a) * r2))
    # linhas entre tudo
    for i, c1 in enumerate(centers):
        for c2 in centers[i + 1:]:
            d.line([c1, c2], fill=(255, 240, 180, 60))
    # círculos
    for cx, cy in centers:
        d.ellipse((cx - 2, cy - 2, cx + 2, cy + 2),
                  outline=(255, 230, 150, 255), width=1)
    return downscale(add_glow(img, (16, 16), 14, (255, 220, 100)))


def make_wings_of_ascension():
    """Asas de Lúcifer Caído — asas negras com chama branca interna."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # left wing
    pts_l = [(15, 8), (3, 12), (5, 18), (8, 22), (12, 24), (14, 20)]
    d.polygon(pts_l, fill=(15, 5, 25, 255))
    # right wing mirror
    pts_r = [(17, 8), (29, 12), (27, 18), (24, 22), (20, 24), (18, 20)]
    d.polygon(pts_r, fill=(15, 5, 25, 255))
    # feather details (lighter purple)
    for i in range(4):
        d.line([(13 - i * 2, 12 + i), (10 - i * 2, 14 + i)],
               fill=(60, 30, 80, 255))
        d.line([(19 + i * 2, 12 + i), (22 + i * 2, 14 + i)],
               fill=(60, 30, 80, 255))
    # central body / star
    d.polygon([(16, 9), (18, 14), (16, 19), (14, 14)], fill=(255, 255, 240, 255))
    d.line([(16, 9), (16, 19)], fill=(255, 220, 100, 255))
    return downscale(add_glow(img, (16, 14), 8, (200, 180, 255)))


def make_angel_tear_amulet():
    """Lágrima de Sandalphon — gota cristalina com hexagrama dentro."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # chain
    d.line([(16, 2), (16, 6)], fill=(220, 220, 230, 255))
    d.ellipse((14, 1, 18, 5), outline=(180, 180, 200, 255))
    # large teardrop
    d.polygon([(16, 6), (8, 18), (12, 26), (20, 26), (24, 18)],
              fill=(200, 230, 255, 230))
    d.polygon([(16, 8), (10, 18), (14, 24), (18, 24), (22, 18)],
              fill=(240, 250, 255, 200))
    # hexagram inside
    hex_lines(d, 16, 18, 4, (255, 240, 180, 255))
    # bright pulse center
    d.ellipse((14, 16, 18, 20), fill=(255, 255, 230, 255))
    return downscale(add_glow(img, (16, 18), 10, (255, 240, 200)))


def make_spirit_anchor():
    """Cordão de Prata Astral — cordão prateado em espiral + âncora."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # silver spiral cord
    for t in range(0, 360 * 3, 8):
        a = math.radians(t)
        r = 3 + (t / 100)
        x = 16 + math.cos(a) * r
        y = 8 + math.sin(a) * r * 0.7 + t / 30
        if y > 28: break
        d.ellipse((x - 0.5, y - 0.5, x + 0.5, y + 0.5),
                  fill=(220, 230, 255, 200 - t // 8))
    # anchor at bottom
    d.line([(16, 22), (16, 28)], fill=(180, 180, 220, 255), width=2)
    d.arc([10, 22, 22, 30], 0, 180, fill=(220, 220, 240, 255), width=2)
    # top star (body)
    d.polygon([(16, 4), (18, 8), (16, 12), (14, 8)], fill=(255, 255, 255, 255))
    return downscale(add_glow(img, (16, 12), 14, (200, 220, 255)))


def make_choir_bell():
    """Sino dos Ophanim — sino dourado com 3 rodas-com-olhos."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # bell body
    d.polygon([(10, 8), (22, 8), (24, 22), (8, 22)], fill=(200, 160, 60, 255))
    d.polygon([(11, 9), (21, 9), (22, 21), (10, 21)], fill=(240, 200, 100, 255))
    # bell mouth
    d.rectangle((8, 22, 24, 24), fill=(180, 140, 50, 255))
    # clapper
    d.line([(16, 24), (16, 27)], fill=(120, 100, 50, 255), width=2)
    d.ellipse((15, 26, 17, 28), fill=(180, 150, 70, 255))
    # 3 rings with eyes (Ophanim)
    for i, (ox, oy) in enumerate([(8, 4), (24, 4), (16, 30)]):
        d.ellipse((ox - 3, oy - 3, ox + 3, oy + 3),
                  outline=(255, 220, 150, 255))
        d.ellipse((ox - 1, oy - 1, ox + 1, oy + 1), fill=(255, 255, 230, 255))
        d.point((ox, oy), fill=(0, 0, 0, 255))
    return downscale(add_glow(img, (16, 15), 12, (255, 220, 120)))


def make_divine_smite_rod():
    """Vara de Salomão — bastão com selo de Salomão (hexagrama) no topo."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # rod
    d.line([(8, 26), (24, 10)], fill=(220, 180, 100, 255), width=2)
    d.line([(9, 26), (23, 12)], fill=(255, 220, 140, 255))
    # base wrapped grip
    d.rectangle((6, 24, 12, 30), fill=(80, 50, 30, 255))
    for y in range(25, 30):
        d.line([(7, y), (11, y)], fill=(120, 80, 40, 255))
    # seal of Solomon (hexagram) at top
    d.ellipse((19, 5, 31, 17), fill=(40, 30, 20, 255))
    d.ellipse((20, 6, 30, 16), fill=(80, 60, 30, 255))
    hex_lines(d, 25, 11, 4, (255, 240, 180, 255))
    d.ellipse((23, 9, 27, 13), fill=(255, 255, 230, 255))
    return downscale(add_glow(img, (25, 11), 10, (255, 230, 150)))


def make_soul_mirror():
    """Espelho Hermético — círculo prateado com olho refletido + ouroboros."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # frame
    d.ellipse((4, 4, 28, 28), fill=(120, 120, 140, 255))
    d.ellipse((6, 6, 26, 26), fill=(60, 60, 80, 255))
    # mirror surface
    d.ellipse((8, 8, 24, 24), fill=(220, 230, 240, 255))
    # ouroboros (snake eating tail)
    for t in range(0, 360, 15):
        a = math.radians(t)
        r = 7
        x = 16 + math.cos(a) * r
        y = 16 + math.sin(a) * r
        d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(60, 100, 60, 255))
    # eye reflection
    d.ellipse((13, 14, 19, 18), fill=(255, 255, 255, 255))
    d.ellipse((14, 15, 18, 17), fill=(80, 40, 120, 255))
    d.ellipse((15, 16, 17, 17), fill=(0, 0, 0, 255))
    # handle
    d.line([(16, 24), (16, 30)], fill=(80, 80, 100, 255), width=3)
    return downscale(add_glow(img, (16, 16), 14, (200, 220, 255)))


def make_spirit_compass():
    """Pêndulo de Radiestesia — pêndulo cristal + roda divinatória."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # circular base/dial
    d.ellipse((4, 8, 28, 28), fill=(50, 40, 70, 255))
    d.ellipse((5, 9, 27, 27), fill=(80, 70, 100, 255))
    # cardinal points + extras (4 cardinal + 4 inter)
    for i in range(8):
        a = math.radians(45 * i - 90)
        x = 16 + math.cos(a) * 9
        y = 18 + math.sin(a) * 9
        d.line([(16, 18), (x, y)], fill=(180, 160, 220, 255))
    # crystal pendulum
    d.line([(16, 4), (16, 18)], fill=(120, 100, 140, 255))
    d.polygon([(16, 4), (13, 9), (16, 14), (19, 9)], fill=(180, 220, 255, 255))
    d.polygon([(16, 6), (14, 9), (16, 12), (18, 9)], fill=(240, 250, 255, 255))
    # center pivot
    d.ellipse((14, 16, 18, 20), fill=(80, 60, 100, 255))
    d.ellipse((15, 17, 17, 19), fill=(220, 200, 255, 255))
    return downscale(add_glow(img, (16, 9), 7, (200, 180, 255)))


def make_holy_water_bucket():
    """Água Lustral — frasco de vidro com líquido azul brilhante + cruz."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # cork stopper
    d.rectangle((13, 3, 19, 7), fill=(100, 70, 40, 255))
    # neck
    d.rectangle((13, 7, 19, 11), fill=(180, 220, 230, 200))
    # body (round flask)
    d.ellipse((6, 10, 26, 28), fill=(160, 200, 220, 200))
    d.ellipse((7, 11, 25, 27), fill=(120, 180, 220, 150))
    # liquid
    d.ellipse((8, 14, 24, 26), fill=(80, 160, 240, 200))
    d.ellipse((9, 15, 23, 25), fill=(120, 200, 255, 220))
    # holy cross inside
    d.line([(16, 16), (16, 24)], fill=(255, 255, 255, 255), width=2)
    d.line([(13, 19), (19, 19)], fill=(255, 255, 255, 255), width=2)
    # highlight glint
    d.ellipse((10, 14, 13, 17), fill=(220, 240, 255, 150))
    return downscale(add_glow(img, (16, 20), 10, (120, 200, 255)))


def make_angel_wing_feather():
    """Pena de Cherub — pena branca com gradiente dourado + olhinhos."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # quill
    d.line([(8, 28), (16, 4)], fill=(180, 180, 200, 255), width=2)
    # vanes (gradient white to gold)
    for t in range(0, 22):
        ax = 16 - t * 0.4
        ay = 4 + t
        width = 8 - abs(t - 10) // 2
        gold = 1.0 - t / 30
        color = (int(255 - 30 * gold), int(255 - 60 * gold), int(220 - 80 * gold), 255)
        d.line([(ax, ay), (ax - width, ay + width // 2)], fill=color)
        d.line([(ax, ay), (ax + width, ay + width // 2)], fill=color)
    # 3 small eyes (Cherubim have many eyes)
    for x, y in [(10, 12), (20, 14), (12, 22)]:
        d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(255, 230, 100, 255))
        d.point((x, y), fill=(0, 0, 0, 255))
    return downscale(add_glow(img, (16, 16), 10, (255, 240, 180)))


def make_seraph_blade():
    """Lâmina Serafim — espada com chama dourada e Tetragrammaton."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # pommel
    d.ellipse((10, 24, 16, 30), fill=(255, 200, 60, 255))
    d.ellipse((11, 25, 15, 29), fill=(255, 240, 150, 255))
    # grip
    d.line([(13, 24), (13, 21)], fill=(180, 100, 50, 255), width=3)
    # crossguard
    d.line([(9, 21), (17, 21)], fill=(255, 220, 100, 255), width=2)
    # blade
    d.polygon([(13, 21), (15, 21), (25, 4), (24, 3), (23, 4)],
              fill=(255, 250, 220, 255))
    d.line([(14, 20), (24, 5)], fill=(255, 230, 150, 255))
    # holy flame at tip
    d.polygon([(22, 6), (26, 2), (29, 5), (25, 8)], fill=(255, 180, 80, 200))
    d.polygon([(23, 5), (26, 3), (27, 6)], fill=(255, 230, 150, 255))
    # Hebrew letter יהוה (YHVH) glow at crossguard
    d.point((11, 22), fill=(255, 255, 200, 255))
    d.point((13, 22), fill=(255, 255, 200, 255))
    d.point((15, 22), fill=(255, 255, 200, 255))
    return downscale(add_glow(img, (24, 5), 12, (255, 220, 130)))


def make_prayer_book():
    """Livro do Êxodo — livro encadernado em couro com sephirot tree na capa."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # book closed
    d.rectangle((4, 4, 28, 28), fill=(80, 30, 30, 255))
    d.rectangle((5, 5, 27, 27), fill=(120, 50, 40, 255))
    # spine
    d.rectangle((4, 4, 7, 28), fill=(50, 20, 20, 255))
    # gold trim
    d.rectangle((4, 4, 28, 5), fill=(220, 180, 80, 255))
    d.rectangle((4, 27, 28, 28), fill=(220, 180, 80, 255))
    # sephirot tree centered (small)
    sephirot_tree(d, 17, 16, (240, 200, 100, 255))
    # gold corner ornaments
    for cx, cy in [(8, 8), (24, 8), (8, 24), (24, 24)]:
        d.line([(cx - 1, cy), (cx + 1, cy)], fill=(220, 180, 80, 255))
        d.line([(cx, cy - 1), (cx, cy + 1)], fill=(220, 180, 80, 255))
    return downscale(add_glow(img, (17, 16), 10, (220, 180, 80)))


# ──────────────── MAIN ────────────────

GENERATORS = {
    "whispering_veil": make_whispering_veil,
    "eyes_of_abyss": make_eyes_of_abyss,
    "cursed_cradle": make_cursed_cradle,
    "pendulum_of_dread": make_pendulum_of_dread,
    "lantern_of_false_memory": make_lantern_of_false_memory,
    "tongue_of_old_ones": make_tongue_of_old_ones,
    "hourglass_of_regression": make_hourglass_of_regression,
    "void_seer_orb": make_void_seer_orb,
    "halo_of_light": make_halo_of_light,
    "wings_of_ascension": make_wings_of_ascension,
    "angel_tear_amulet": make_angel_tear_amulet,
    "spirit_anchor": make_spirit_anchor,
    "choir_bell": make_choir_bell,
    "divine_smite_rod": make_divine_smite_rod,
    "soul_mirror": make_soul_mirror,
    "spirit_compass": make_spirit_compass,
    "holy_water_bucket": make_holy_water_bucket,
    "angel_wing_feather": make_angel_wing_feather,
    "seraph_blade": make_seraph_blade,
    "prayer_book": make_prayer_book,
}

if __name__ == "__main__":
    for name, fn in GENERATORS.items():
        img = fn()
        path = os.path.join(TEX_ITEM, name + ".png")
        img.save(path)
        print(f"saved {name}.png")
    print(f"\nTotal: {len(GENERATORS)} occult textures generated")
