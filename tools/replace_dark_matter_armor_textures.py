"""Substitui as texturas dos 4 itens de Dark Matter armor (helmet/chestplate/
leggings/boots) que aparecem no inventário, tabs, GUI.

NÃO mexe nas texturas do modelo do player (textures/models/armor/*).

Estilo: roxo MUITO escuro (#1E0F2E) com highlights lavanda escuro, mimickando
o layout vanilla das peças vanilla mas em paleta cosmic horror.
"""
from PIL import Image, ImageDraw
import os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'item')
SIZE = 16

# Paleta roxo cósmico
SHADOW = (15, 8, 27, 255)     # quase preto-violeta
BASE = (30, 15, 46, 255)      # roxo escuro principal
MID = (58, 31, 79, 255)       # roxo médio (highlight sutil)
LIGHT = (90, 45, 122, 255)    # lavanda escura (highlight forte)
GLOW = (130, 70, 175, 255)    # acento brilhante (poucos pixels)
BLACK = (0, 0, 0, 0)          # transparente


def base_img():
    return Image.new('RGBA', (SIZE, SIZE), BLACK)


# ============================================================
# HELMET — formato de capacete (cobre cabeça)
# Layout vanilla: U invertido aberto embaixo
# ============================================================
def helmet():
    img = base_img()
    d = ImageDraw.Draw(img)
    # Topo da cabeça (faixa horizontal superior)
    d.rectangle([4, 3, 11, 4], fill=BASE)
    # Laterais cabeça
    d.rectangle([3, 4, 4, 10], fill=BASE)  # lateral esq
    d.rectangle([11, 4, 12, 10], fill=BASE)  # lateral dir
    # "Coroa" superior — pico central
    d.rectangle([5, 2, 10, 3], fill=BASE)
    d.point((6, 1), fill=MID)
    d.point((9, 1), fill=MID)
    # Frente do helmet (cobre testa) com gap pros olhos
    d.rectangle([5, 5, 10, 7], fill=MID)  # testa
    # Gap dos olhos (transparente / mais escuro)
    d.point((6, 6), fill=SHADOW)
    d.point((9, 6), fill=SHADOW)
    # Queixo / proteção lateral
    d.rectangle([4, 8, 11, 10], fill=BASE)
    d.point((4, 10), fill=SHADOW)
    d.point((11, 10), fill=SHADOW)
    # Highlights sutis (poucos pixels)
    d.point((5, 4), fill=LIGHT)
    d.point((10, 4), fill=LIGHT)
    d.point((7, 5), fill=GLOW)
    d.point((8, 5), fill=GLOW)
    # Sombra inferior
    d.line([(4, 11), (11, 11)], fill=SHADOW)
    img.save(os.path.join(OUT, 'dark_matter_helmet.png'))


# ============================================================
# CHESTPLATE — peitoral
# Layout vanilla: faixa larga horizontal com partes nos ombros
# ============================================================
def chestplate():
    img = base_img()
    d = ImageDraw.Draw(img)
    # Faixa central (corpo)
    d.rectangle([3, 4, 12, 11], fill=BASE)
    # Ombros (extensões nas laterais)
    d.rectangle([2, 3, 4, 7], fill=BASE)   # ombro esq
    d.rectangle([11, 3, 13, 7], fill=BASE)  # ombro dir
    # Detalhe central (decoração) — V invertido / símbolo
    d.line([(6, 5), (8, 7)], fill=MID)
    d.line([(8, 7), (10, 5)], fill=MID)
    d.point((8, 8), fill=GLOW)  # gem central
    d.point((8, 9), fill=LIGHT)
    # Highlights nos ombros
    d.point((3, 4), fill=LIGHT)
    d.point((12, 4), fill=LIGHT)
    # Sombras laterais
    d.line([(2, 7), (4, 7)], fill=SHADOW)
    d.line([(11, 7), (13, 7)], fill=SHADOW)
    # Borda inferior (cinta)
    d.line([(3, 11), (12, 11)], fill=SHADOW)
    d.line([(3, 12), (12, 12)], fill=BASE)
    # Detalhes "buracos" (decoração textura)
    d.point((5, 8), fill=SHADOW)
    d.point((11, 8), fill=SHADOW)
    d.point((4, 5), fill=SHADOW)
    d.point((12, 5), fill=SHADOW)
    img.save(os.path.join(OUT, 'dark_matter_chestplate.png'))


# ============================================================
# LEGGINGS — calças (2 pernas)
# Layout vanilla: cinto top + 2 pernas verticais
# ============================================================
def leggings():
    img = base_img()
    d = ImageDraw.Draw(img)
    # Cinto (faixa horizontal superior)
    d.rectangle([3, 2, 12, 4], fill=MID)
    # Cinto detalhe (highlight central)
    d.line([(3, 3), (12, 3)], fill=LIGHT)
    d.point((7, 3), fill=GLOW)  # gem central do cinto
    d.point((8, 3), fill=GLOW)
    # Perna esquerda
    d.rectangle([4, 5, 7, 14], fill=BASE)
    # Perna direita
    d.rectangle([8, 5, 11, 14], fill=BASE)
    # Gap entre as pernas (mantém transparente já)
    # Highlights frontais nas pernas
    d.line([(5, 6), (5, 13)], fill=MID)
    d.line([(9, 6), (9, 13)], fill=MID)
    # Sombras laterais nas pernas
    d.line([(4, 6), (4, 14)], fill=SHADOW)
    d.line([(7, 6), (7, 14)], fill=SHADOW)
    d.line([(8, 6), (8, 14)], fill=SHADOW)
    d.line([(11, 6), (11, 14)], fill=SHADOW)
    # Tornozelo (sombra base)
    d.line([(4, 14), (7, 14)], fill=SHADOW)
    d.line([(8, 14), (11, 14)], fill=SHADOW)
    # Joelho detalhe
    d.point((5, 9), fill=LIGHT)
    d.point((9, 9), fill=LIGHT)
    img.save(os.path.join(OUT, 'dark_matter_leggings.png'))


# ============================================================
# BOOTS — botas (2 botas separadas)
# Layout vanilla: 2 botas baixas, mais largas em baixo
# ============================================================
def boots():
    img = base_img()
    d = ImageDraw.Draw(img)
    # Bota esquerda — formato L
    # Cano superior
    d.rectangle([3, 5, 6, 10], fill=BASE)
    # Pé (estende pra frente)
    d.rectangle([2, 10, 7, 13], fill=BASE)
    # Bota direita — espelho
    d.rectangle([9, 5, 12, 10], fill=BASE)
    d.rectangle([8, 10, 13, 13], fill=BASE)
    # Sola (sombra)
    d.line([(2, 13), (7, 13)], fill=SHADOW)
    d.line([(8, 13), (13, 13)], fill=SHADOW)
    # Salto (detalhe traseiro)
    d.point((6, 13), fill=SHADOW)
    d.point((12, 13), fill=SHADOW)
    # Highlights frontais
    d.line([(3, 6), (3, 9)], fill=MID)
    d.line([(9, 6), (9, 9)], fill=MID)
    d.point((2, 11), fill=LIGHT)
    d.point((8, 11), fill=LIGHT)
    # Sombras laterais
    d.line([(6, 6), (6, 9)], fill=SHADOW)
    d.line([(12, 6), (12, 9)], fill=SHADOW)
    # Topo da bota (borda)
    d.line([(3, 5), (6, 5)], fill=LIGHT)
    d.line([(9, 5), (12, 5)], fill=LIGHT)
    # Brilho discreto na ponta do pé
    d.point((3, 12), fill=GLOW)
    d.point((9, 12), fill=GLOW)
    img.save(os.path.join(OUT, 'dark_matter_boots.png'))


if __name__ == '__main__':
    helmet()
    chestplate()
    leggings()
    boots()
    print(f'Replaced 4 Dark Matter armor item textures in {OUT}')
