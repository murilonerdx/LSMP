"""
r179: assets da Fenda Dimensional + 4 blocos de irregularidade dimensional.

Gera:
  - textures/entity/dimensional_rift.png  (64x64 — rasgo vertical no ar, interior estrelado)
  - textures/item/rift_opener.png         (16x16 — abridor de fenda)
  - 4 block textures (16x16) + blockstates + models + item models
  - models/item/rift_opener.json
"""
import os, math, random
from PIL import Image, ImageDraw

random.seed(179)
BASE = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources/assets/liberthia"
ENT = os.path.join(BASE, "textures/entity")
TBLK = os.path.join(BASE, "textures/block")
TITM = os.path.join(BASE, "textures/item")
MBLK = os.path.join(BASE, "models/block")
MITM = os.path.join(BASE, "models/item")
BSTATE = os.path.join(BASE, "blockstates")
for d in [ENT, TBLK, TITM, MBLK, MITM, BSTATE]:
    os.makedirs(d, exist_ok=True)

def clamp(v): return max(0, min(255, int(v)))

# ─────────────────────────── Fenda (entity, 64x64) ───────────────────────────
def gen_rift():
    W = H = 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    cx, cy = W / 2.0, H / 2.0
    # almôndega/lente vertical: largura varia com a altura (fino nas pontas)
    for y in range(H):
        ny = (y - cy) / (H * 0.5)          # -1..1
        if abs(ny) >= 1.0:
            continue
        halfw = (1.0 - ny * ny) * (W * 0.30)   # perfil de lente
        for x in range(W):
            dx = x - cx
            if abs(dx) > halfw:
                continue
            edge = abs(dx) / max(0.5, halfw)    # 0 centro .. 1 borda
            if edge > 0.82:
                # borda brilhante: ciano→magenta
                t = (edge - 0.82) / 0.18
                r = clamp(120 + 135 * t)
                g = clamp(220 - 180 * t)
                b = 255
                a = 255
            else:
                # interior: vazio espiritual escuro com leve gradiente roxo
                core = 1.0 - edge
                r = clamp(20 + 30 * core)
                g = clamp(6 + 10 * core)
                b = clamp(34 + 46 * core)
                a = clamp(150 + 90 * core)
            px[x, y] = (r, g, b, a)
    # estrelas no interior (o "outro lado")
    for _ in range(40):
        y = random.randint(2, H - 3)
        ny = (y - cy) / (H * 0.5)
        if abs(ny) >= 0.95:
            continue
        halfw = (1.0 - ny * ny) * (W * 0.30) * 0.8
        if halfw < 1:
            continue
        x = int(cx + (random.random() * 2 - 1) * halfw)
        if 0 <= x < W and px[x, y][3] > 0:
            b = random.choice([(230, 200, 255), (200, 230, 255), (255, 255, 255)])
            px[x, y] = (b[0], b[1], b[2], 255)
    img.save(os.path.join(ENT, "dimensional_rift.png"))

# ─────────────────────────── rift_opener (item 16) ───────────────────────────
def gen_rift_opener():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # cabo escuro
    d.line((5, 13, 9, 9), fill=(40, 20, 55, 255), width=2)
    # lâmina/chave que rasga o ar — sliver roxo brilhante
    for i in range(8):
        y = 9 - i
        x = 9 + i // 2
        d.point((x, y), fill=(190, 90, 255, 255))
        d.point((x + 1, y), fill=(120, 220, 255, 255))
    # ponta cintilante
    d.point((13, 2), fill=(255, 255, 255, 255))
    d.point((12, 3), fill=(220, 200, 255, 255))
    img.save(os.path.join(TITM, "rift_opener.png"))

# ─────────────────────────── 4 blocos de irregularidade ──────────────────────
def noise_block(name, base, accents, stars=False, glitch=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = random.random()
            c = base
            if v < 0.30:
                c = random.choice(accents)
            r, g, b = c
            # leve variação
            jit = random.randint(-12, 12)
            px[x, y] = (clamp(r + jit), clamp(g + jit), clamp(b + jit), 255)
    if glitch:
        # tiras deslocadas RGB-split
        for _ in range(4):
            y = random.randint(0, 15)
            col = random.choice([(255, 40, 120), (40, 255, 200), (200, 120, 255)])
            for x in range(16):
                if random.random() < 0.7:
                    px[x, y] = (col[0], col[1], col[2], 255)
    if stars:
        for _ in range(8):
            x, y = random.randint(0, 15), random.randint(0, 15)
            px[x, y] = (235, 225, 255, 255)
    img.save(os.path.join(TBLK, name + ".png"))

def write_block_assets(name):
    # blockstate
    with open(os.path.join(BSTATE, name + ".json"), "w") as f:
        f.write('{"variants":{"":{"model":"liberthia:block/%s"}}}' % name)
    # block model (cube_all)
    with open(os.path.join(MBLK, name + ".json"), "w") as f:
        f.write('{"parent":"minecraft:block/cube_all","textures":{"all":"liberthia:block/%s"}}' % name)
    # item model (parent block)
    with open(os.path.join(MITM, name + ".json"), "w") as f:
        f.write('{"parent":"liberthia:block/%s"}' % name)

def gen_blocks():
    # 1) rift_residue — cristal roxo
    noise_block("rift_residue", (60, 28, 92), [(120, 60, 180), (160, 100, 220), (90, 200, 230)])
    # 2) warped_space — glitch magenta/preto
    noise_block("warped_space", (18, 12, 22), [(180, 30, 120), (30, 30, 40)], glitch=True)
    # 3) void_scar — preto estrelado
    noise_block("void_scar", (8, 6, 14), [(16, 10, 26), (30, 18, 44)], stars=True)
    # 4) unstable_matter — vermelho/roxo instável
    noise_block("dimensional_flux", (70, 18, 30), [(180, 50, 40), (140, 40, 120), (220, 120, 60)])
    for n in ["rift_residue", "warped_space", "void_scar", "dimensional_flux"]:
        write_block_assets(n)

def gen_rift_opener_model():
    with open(os.path.join(MITM, "rift_opener.json"), "w") as f:
        f.write('{"parent":"minecraft:item/generated","textures":{"layer0":"liberthia:item/rift_opener"}}')

if __name__ == "__main__":
    gen_rift()
    gen_rift_opener()
    gen_rift_opener_model()
    gen_blocks()
    print("OK dimensional rift + 4 irregularity blocks assets generated")
