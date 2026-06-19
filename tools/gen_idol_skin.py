"""r178: gera a textura do Idol (O Idolo) — skin de player 64x64, figura PALIDA e
alta com a CABECA escura/sem rosto (void). Pele palida doentia, leves estrias.
"""
import random
from pathlib import Path
from PIL import Image

random.seed(1781)
TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/liberthia/textures/entity"
TEX.mkdir(parents=True, exist_ok=True)

PALE = (198, 196, 188, 255)
PALE_LO = (170, 168, 162, 255)
HEAD = (44, 44, 50, 255)       # cabeca sombria
FACE = (10, 10, 13, 255)       # rosto vazio (quase preto)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

# preenche tudo de pele palida (com leve ruido/estrias)
for y in range(64):
    for x in range(64):
        n = random.randint(-12, 8)
        base = PALE if (x // 2 + y // 3) % 7 else PALE_LO
        px[x, y] = (max(0, base[0] + n), max(0, base[1] + n), max(0, base[2] + n), 255)

# regiao da CABECA (linhas 0..16, colunas 0..32) -> sombria
for y in range(0, 16):
    for x in range(0, 32):
        n = random.randint(-8, 8)
        px[x, y] = (max(0, HEAD[0] + n), max(0, HEAD[1] + n), max(0, HEAD[2] + n), 255)

# FACE frontal (8,8)-(16,16) -> vazio quase preto
for y in range(8, 16):
    for x in range(8, 16):
        px[x, y] = FACE

out = TEX / "idol.png"
img.save(out)
print("wrote", out)
