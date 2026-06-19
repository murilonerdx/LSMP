"""
Gera texturas PNG 16x16 pixel-art para items custom do mod.

Cada função gera uma textura com cor/forma relacionada ao nome do item:
- matter_cure: frasco azul claro com sigilo de cruz médica
- daily_pill: cápsula bipartida (branco/amarelo) horizontal
- speed_upgrade: relâmpago (forma de raio) vermelho
- efficiency_upgrade: engrenagem azul-ciano
- capacity_upgrade: baú estilizado (caixa amber com cadeado)

Output: PNGs em src/main/resources/assets/liberthia/textures/item/<nome>.png
"""
from PIL import Image, ImageDraw
from pathlib import Path

OUT = Path("src/main/resources/assets/liberthia/textures/item")
OUT.mkdir(parents=True, exist_ok=True)

# Cores principais — pixel-art Minecraft costuma usar paletas de 4-8 tons por item
# pra dar profundidade. Cada cor base tem 2-3 shades.

def matter_cure(path):
    """Frasco médico azul + cruz branca + brilho."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Tampa do frasco (cinza escuro topo)
    d.rectangle((6, 1, 9, 2), fill=(60, 60, 70, 255))
    d.rectangle((5, 2, 10, 4), fill=(100, 100, 110, 255))
    # Pescoço
    d.rectangle((6, 4, 9, 5), fill=(150, 200, 230, 255))
    # Corpo do frasco (gradient azul claro)
    d.rectangle((4, 5, 11, 14), fill=(80, 150, 220, 255))
    d.rectangle((5, 5, 10, 14), fill=(120, 190, 240, 255))
    # Highlights laterais (transparência simulada — borda mais clara)
    d.line((4, 6, 4, 13), fill=(180, 220, 250, 255))
    d.line((11, 6, 11, 13), fill=(50, 100, 170, 255))
    # Base
    d.rectangle((5, 14, 10, 15), fill=(50, 100, 170, 255))
    # Cruz médica branca no centro
    d.rectangle((7, 8, 8, 12), fill=(255, 255, 255, 255))
    d.rectangle((6, 9, 9, 10), fill=(255, 255, 255, 255))
    # Brilho/glow no canto superior esquerdo
    d.point((5, 6), fill=(255, 255, 255, 255))
    d.point((6, 5), fill=(220, 240, 255, 255))
    img.save(path)
    print(f"OK: {path}")


def daily_pill(path):
    """Cápsula horizontal bipartida (branca + amarela) com brilho."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Cápsula — formato pill horizontal
    # Metade esquerda branca
    d.ellipse((2, 6, 8, 10), fill=(240, 240, 240, 255))
    d.rectangle((5, 6, 8, 10), fill=(240, 240, 240, 255))
    # Metade direita amarela
    d.rectangle((8, 6, 11, 10), fill=(255, 210, 60, 255))
    d.ellipse((8, 6, 14, 10), fill=(255, 210, 60, 255))
    # Divisão central (linha vertical sombreada)
    d.line((8, 6, 8, 9), fill=(180, 180, 180, 255))
    # Shading
    d.line((3, 9, 13, 9), fill=(200, 200, 200, 255), width=1)  # base shadow esq
    # Apenas a parte amarela tem shadow na base
    d.line((8, 9, 13, 9), fill=(190, 150, 30, 255))
    # Highlight
    d.line((4, 7, 6, 7), fill=(255, 255, 255, 255))
    d.line((9, 7, 11, 7), fill=(255, 240, 120, 255))
    img.save(path)
    print(f"OK: {path}")


def speed_upgrade(path):
    """Relâmpago vermelho/laranja (forma de raio) em base de placa metálica."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Base placa metálica (quadrado com chanfros)
    d.rectangle((2, 2, 13, 13), fill=(70, 70, 75, 255))
    d.rectangle((3, 3, 12, 12), fill=(110, 110, 120, 255))
    d.rectangle((4, 4, 11, 11), fill=(90, 90, 100, 255))
    # Cantos chanfrados (4 pontos)
    d.point((2, 2), fill=(0, 0, 0, 0))
    d.point((13, 2), fill=(0, 0, 0, 0))
    d.point((2, 13), fill=(0, 0, 0, 0))
    d.point((13, 13), fill=(0, 0, 0, 0))
    # Relâmpago — forma de raio (sequência de pixels diagonais)
    bolt = [
        (9, 3), (8, 4), (9, 4),     # topo
        (8, 5), (7, 6), (8, 6),
        (7, 7), (8, 7), (9, 7),     # base do "V" superior
        (7, 8), (6, 9), (7, 9),
        (6, 10), (5, 11), (6, 11),  # ponta inferior
        (5, 12),
    ]
    for x, y in bolt:
        d.point((x, y), fill=(255, 50, 30, 255))
    # Glow amarelo em volta do raio (1 pixel offset)
    glow_offsets = [
        (10, 3), (10, 4),
        (9, 5), (9, 6),
        (8, 8), (7, 10), (7, 11),
        (6, 12),
    ]
    for x, y in glow_offsets:
        d.point((x, y), fill=(255, 200, 50, 255))
    img.save(path)
    print(f"OK: {path}")


def efficiency_upgrade(path):
    """Engrenagem azul/ciano centrada em placa metálica."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Base placa metálica
    d.rectangle((2, 2, 13, 13), fill=(70, 70, 75, 255))
    d.rectangle((3, 3, 12, 12), fill=(110, 110, 120, 255))
    d.rectangle((4, 4, 11, 11), fill=(90, 90, 100, 255))
    d.point((2, 2), fill=(0, 0, 0, 0))
    d.point((13, 2), fill=(0, 0, 0, 0))
    d.point((2, 13), fill=(0, 0, 0, 0))
    d.point((13, 13), fill=(0, 0, 0, 0))
    # Engrenagem (gear) — 8 dentes ao redor + buraco central
    gear_color = (60, 180, 220, 255)
    gear_shade = (40, 140, 180, 255)
    # 4 dentes principais (norte, sul, leste, oeste)
    d.rectangle((7, 3, 8, 4), fill=gear_color)    # N
    d.rectangle((7, 11, 8, 12), fill=gear_color)  # S
    d.rectangle((3, 7, 4, 8), fill=gear_color)    # W
    d.rectangle((11, 7, 12, 8), fill=gear_color)  # E
    # Corpo principal (círculo aproximado)
    d.rectangle((6, 5, 9, 10), fill=gear_color)
    d.rectangle((5, 6, 10, 9), fill=gear_color)
    # Dentes diagonais (4 cantos)
    d.point((4, 4), fill=gear_color)
    d.point((11, 4), fill=gear_color)
    d.point((4, 11), fill=gear_color)
    d.point((11, 11), fill=gear_color)
    # Sombra na parte inferior pra dar volume
    d.line((6, 10, 9, 10), fill=gear_shade)
    d.line((5, 9, 10, 9), fill=gear_shade)
    # Buraco central (preto/azul escuro)
    d.rectangle((7, 7, 8, 8), fill=(20, 60, 90, 255))
    # Highlight branco
    d.point((6, 5), fill=(180, 240, 255, 255))
    img.save(path)
    print(f"OK: {path}")


def capacity_upgrade(path):
    """Baú estilizado amber/laranja com cadeado dourado."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Base placa metálica
    d.rectangle((2, 2, 13, 13), fill=(70, 70, 75, 255))
    d.rectangle((3, 3, 12, 12), fill=(110, 110, 120, 255))
    d.rectangle((4, 4, 11, 11), fill=(90, 90, 100, 255))
    d.point((2, 2), fill=(0, 0, 0, 0))
    d.point((13, 2), fill=(0, 0, 0, 0))
    d.point((2, 13), fill=(0, 0, 0, 0))
    d.point((13, 13), fill=(0, 0, 0, 0))
    # Baú amber
    d.rectangle((4, 6, 11, 11), fill=(170, 100, 30, 255))  # corpo madeira
    d.rectangle((5, 7, 10, 10), fill=(200, 130, 50, 255))  # face frontal mais clara
    # Tampa
    d.rectangle((4, 5, 11, 6), fill=(140, 80, 20, 255))
    # Trava de ouro central
    d.rectangle((7, 7, 8, 9), fill=(255, 200, 50, 255))
    d.point((7, 7), fill=(255, 240, 120, 255))
    # Cantos com prego (escuro)
    d.point((4, 5), fill=(60, 30, 10, 255))
    d.point((11, 5), fill=(60, 30, 10, 255))
    d.point((4, 11), fill=(60, 30, 10, 255))
    d.point((11, 11), fill=(60, 30, 10, 255))
    # Sombra na base
    d.line((4, 11, 11, 11), fill=(100, 60, 20, 255))
    img.save(path)
    print(f"OK: {path}")


if __name__ == "__main__":
    matter_cure(OUT / "matter_cure.png")
    daily_pill(OUT / "daily_pill.png")
    speed_upgrade(OUT / "speed_upgrade.png")
    efficiency_upgrade(OUT / "efficiency_upgrade.png")
    capacity_upgrade(OUT / "capacity_upgrade.png")
    print("\nTodas as 5 texturas geradas.")
