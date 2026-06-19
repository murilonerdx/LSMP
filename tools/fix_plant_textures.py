"""r143: Regenera texturas das plantas (Ghost Mushroom + Whisper Petal Bush)
com transparência REAL — modelo cross precisa de pixels alfa=0 onde nao tem planta."""
from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/liberthia/textures/block"

# WHISPER PETAL BUSH — flor lilás compacta no centro inferior
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))  # fully transparent
d = ImageDraw.Draw(img)
# Stem (verde escuro) — só o caule fininho
d.line([(8, 15), (8, 9)], fill=(60, 110, 50, 255))
d.line([(7, 14), (7, 11)], fill=(50, 100, 40, 255))
# Petals — 4 pétalas radiais
petal_color = (210, 170, 230, 255)
center_color = (255, 230, 255, 255)
# pétala topo
d.ellipse([6, 4, 10, 8], fill=petal_color)
# pétala esquerda
d.ellipse([3, 6, 8, 10], fill=petal_color)
# pétala direita
d.ellipse([8, 6, 13, 10], fill=petal_color)
# pétala baixo (menor)
d.ellipse([6, 8, 10, 12], fill=petal_color)
# Centro brilhante
d.ellipse([7, 7, 9, 9], fill=center_color)
d.point((8, 8), fill=(255, 255, 255, 255))
img.save(OUT + "/whisper_petal_bush.png")

# GHOST MUSHROOM — cogumelo branco com glow azul
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Stem white-blueish
d.rectangle([7, 10, 9, 15], fill=(220, 230, 250, 255))
d.point((7, 15), fill=(200, 215, 240, 255))
d.point((9, 15), fill=(200, 215, 240, 255))
# Cap (mushroom top) — half-ellipse white
d.ellipse([3, 3, 13, 11], fill=(240, 240, 250, 255), outline=(180, 200, 230, 255))
# Cut off bottom of cap (we want mushroom shape not full ellipse)
d.rectangle([3, 9, 13, 11], fill=(0, 0, 0, 0))
d.ellipse([3, 3, 13, 10], fill=(240, 240, 250, 255), outline=(180, 200, 230, 255))
# Glow spots azulados
d.point((5, 5), fill=(150, 220, 255, 255))
d.point((10, 4), fill=(150, 220, 255, 255))
d.point((7, 6), fill=(180, 230, 255, 255))
# Bottom shadow under cap
d.line([(4, 10), (12, 10)], fill=(120, 140, 180, 255))
img.save(OUT + "/ghost_mushroom.png")

print("Fixed plant textures with transparent backgrounds")
