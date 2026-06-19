"""
Gerador de texturas placeholder pros items novos do Liberthia (v0.1.30).

Não é arte refinada — só sprites 16x16 com cores temáticas pra os items
funcionarem em-jogo sem aparecer como "missing texture". Substituir
depois por arte de verdade.

Items gerados:
- purified_dark_matter_ingot.png   (preto/roxo escuro com brilho violeta)
- purified_clear_matter_ingot.png  (branco/cinza claro com brilho ciano)
- purified_yellow_matter_ingot.png (dourado com brilho amarelo)
- refined_containment_pendant.png  (cristal branco em moldura dourada)
- refined_containment_glove.png    (luva de couro com cristal branco no dorso)
"""
from PIL import Image
import os
import random

OUT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item"
os.makedirs(OUT, exist_ok=True)
random.seed(42)  # determinístico

def img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def px(im, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), color)

def rect(im, x1, y1, x2, y2, color):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            px(im, x, y, color)

# ─── Ingot helper ────────────────────────────────────────────────
# Lingote no estilo Minecraft: barra horizontal com gradient + brilho
def make_ingot(out_name, base, light, dark, sparkle):
    im = img()
    # Sombra
    rect(im, 2, 11, 13, 12, dark)
    # Corpo do lingote (chanfros)
    rect(im, 3, 5, 12, 11, base)
    # Topo bevel claro
    rect(im, 3, 5, 12, 5, light)
    # Esquerda bevel
    rect(im, 3, 5, 3, 10, light)
    # Sombra direita
    rect(im, 12, 6, 12, 11, dark)
    # Sombra base
    rect(im, 4, 11, 12, 11, dark)
    # Bevel externo
    px(im, 2, 5, dark)
    px(im, 13, 5, dark)
    px(im, 2, 11, dark)
    px(im, 13, 11, dark)
    # Sparkles diagonais — "purified" brilha
    px(im, 5, 7, sparkle)
    px(im, 6, 8, sparkle)
    px(im, 9, 6, sparkle)
    px(im, 10, 7, sparkle)
    px(im, 7, 9, (255, 255, 255, 255))
    # ✦ marca no centro
    px(im, 8, 8, sparkle)
    im.save(os.path.join(OUT, out_name))
    print(f"wrote {out_name}")

# ─── Purified ingots ──────────────────────────────────────────────
# Dark Matter purificado: violeta profundo com brilho ametista
make_ingot("purified_dark_matter_ingot.png",
           base=(50, 20, 80, 255),       # roxo escuro
           light=(120, 70, 180, 255),    # roxo claro
           dark=(20, 5, 35, 255),
           sparkle=(220, 180, 255, 255)) # brilho violeta

# Clear Matter purificado: branco perolado com brilho ciano
make_ingot("purified_clear_matter_ingot.png",
           base=(230, 230, 240, 255),    # branco perolado
           light=(255, 255, 255, 255),
           dark=(150, 160, 180, 255),
           sparkle=(180, 240, 255, 255)) # brilho ciano

# Yellow Matter purificado: dourado quente
make_ingot("purified_yellow_matter_ingot.png",
           base=(220, 175, 50, 255),
           light=(255, 235, 110, 255),
           dark=(140, 95, 20, 255),
           sparkle=(255, 250, 180, 255))

# ─── Refined Containment Pendant ──────────────────────────────────
# Pingente: pedaço de cristal branco no centro, moldura dourada, corrente
def make_pendant():
    im = img()
    # Corrente (linha cinza no topo, em zig-zag pra parecer corrente)
    for x in [4, 6, 9, 11]:
        px(im, x, 0, (180, 180, 180, 255))
    for x in [5, 7, 8, 10]:
        px(im, x, 1, (200, 200, 200, 255))
    # Argola de conexão
    px(im, 7, 2, (220, 200, 80, 255))
    px(im, 8, 2, (220, 200, 80, 255))
    px(im, 7, 3, (220, 200, 80, 255))
    px(im, 8, 3, (220, 200, 80, 255))
    # Moldura dourada (losango)
    GOLD = (210, 170, 40, 255)
    GOLD_LIGHT = (255, 220, 100, 255)
    GOLD_DARK = (140, 100, 10, 255)
    # Bordas do losango
    rect(im, 6, 4, 9, 4, GOLD)
    rect(im, 5, 5, 10, 5, GOLD)
    rect(im, 4, 6, 11, 6, GOLD)
    rect(im, 4, 7, 11, 11, GOLD)
    rect(im, 5, 12, 10, 12, GOLD)
    rect(im, 6, 13, 9, 13, GOLD)
    rect(im, 7, 14, 8, 14, GOLD)
    # Highlights dourados
    rect(im, 6, 4, 9, 4, GOLD_LIGHT)
    rect(im, 5, 5, 5, 5, GOLD_LIGHT)
    rect(im, 4, 6, 4, 6, GOLD_LIGHT)
    # Sombras direita/base
    rect(im, 10, 11, 11, 11, GOLD_DARK)
    rect(im, 9, 12, 10, 12, GOLD_DARK)
    rect(im, 8, 13, 9, 13, GOLD_DARK)
    px(im, 8, 14, GOLD_DARK)
    # Cristal branco no centro
    CRYST = (240, 245, 255, 255)
    CRYST_BR = (200, 230, 255, 255)
    rect(im, 6, 7, 9, 10, CRYST)
    rect(im, 7, 8, 8, 8, (255, 255, 255, 255))  # brilho
    px(im, 5, 8, CRYST_BR)
    px(im, 5, 9, CRYST_BR)
    px(im, 7, 11, CRYST_BR)
    px(im, 8, 11, CRYST_BR)
    # Sparkle
    px(im, 9, 7, (255, 255, 255, 255))
    px(im, 6, 9, (180, 220, 255, 255))
    im.save(os.path.join(OUT, "refined_containment_pendant.png"))
    print("wrote refined_containment_pendant.png")

make_pendant()

# ─── Refined Containment Glove ────────────────────────────────────
# Luva de couro marrom com cristal branco no dorso
def make_glove():
    im = img()
    LEATHER = (110, 70, 35, 255)
    LEATHER_LIGHT = (160, 110, 60, 255)
    LEATHER_DARK = (65, 35, 15, 255)
    CRYST = (235, 245, 255, 255)
    CRYST_BR = (170, 210, 255, 255)
    # Palma (centro/baixo)
    rect(im, 4, 8, 11, 14, LEATHER)
    rect(im, 5, 7, 10, 7, LEATHER)
    # Dedos (4 columns acima)
    rect(im, 4, 3, 5, 8, LEATHER)
    rect(im, 6, 2, 7, 8, LEATHER)
    rect(im, 8, 2, 9, 8, LEATHER)
    rect(im, 10, 3, 11, 8, LEATHER)
    # Polegar
    rect(im, 3, 9, 3, 12, LEATHER)
    # Bevel claro nos dedos (lado esquerdo)
    rect(im, 4, 3, 4, 8, LEATHER_LIGHT)
    rect(im, 6, 2, 6, 8, LEATHER_LIGHT)
    rect(im, 8, 2, 8, 8, LEATHER_LIGHT)
    rect(im, 10, 3, 10, 8, LEATHER_LIGHT)
    rect(im, 4, 8, 11, 8, LEATHER_LIGHT)
    # Sombras direita
    rect(im, 5, 4, 5, 8, LEATHER_DARK)
    rect(im, 7, 3, 7, 8, LEATHER_DARK)
    rect(im, 9, 3, 9, 8, LEATHER_DARK)
    rect(im, 11, 4, 11, 8, LEATHER_DARK)
    rect(im, 4, 14, 11, 14, LEATHER_DARK)
    # Punho (manga)
    rect(im, 3, 13, 12, 14, (80, 50, 20, 255))
    rect(im, 3, 13, 12, 13, LEATHER_LIGHT)
    # Cristal branco refinado no dorso (centro)
    rect(im, 7, 10, 8, 11, CRYST)
    px(im, 7, 10, (255, 255, 255, 255))
    px(im, 6, 10, CRYST_BR)
    px(im, 9, 11, CRYST_BR)
    px(im, 7, 12, CRYST_BR)
    # Sparkle
    px(im, 8, 11, (255, 255, 255, 255))
    im.save(os.path.join(OUT, "refined_containment_glove.png"))
    print("wrote refined_containment_glove.png")

make_glove()

print("\nAll textures generated.")
