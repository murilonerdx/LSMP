"""
Gerador de texturas pra Matter Extractor / Tank / Pipes.

Saída: 16x16 PNGs em src/main/resources/assets/liberthia/textures/block/.

- 5 tanks (matter_tank_empty/25/50/75/full): cubo de vidro azulado com retângulo
  central de líquido violeta proporcional ao nível. Borda metálica cinza no topo.
- 3 pipes (matter_pipe_dark/clear/yellow): faixa horizontal central com gradient
  do tipo + borda metálica nas pontas.
- 3 extractor faces (top/side/front): runa central pulsante (placeholder estático,
  detalhes em violeta sobre preto).
"""

import os
from PIL import Image, ImageDraw

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                       "assets", "liberthia", "textures", "block")
os.makedirs(OUT_DIR, exist_ok=True)


def make_tank(level: int) -> Image.Image:
    """Gera tank texture com fillage (0..4 = 0%..100%)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Borda metálica cinza no topo (linha superior + bordas laterais)
    for x in range(16):
        img.putpixel((x, 0), (130, 130, 140, 255))
        img.putpixel((x, 1), (90, 90, 100, 255))
        img.putpixel((x, 15), (90, 90, 100, 255))
    for y in range(16):
        img.putpixel((0, y), (90, 90, 100, 255))
        img.putpixel((15, y), (90, 90, 100, 255))

    # Vidro azulado (translucent) — só desenha pixels INTERNOS (2..13)
    # Preenche TODO o miolo com glass (alpha baixo)
    for y in range(2, 15):
        for x in range(1, 15):
            # Borda interna do "vidro" cinza claro
            if x == 1 or x == 14:
                img.putpixel((x, y), (110, 110, 130, 200))
                continue
            # Glass tile interior
            img.putpixel((x, y), (180, 200, 240, 100))

    # Linhas de "shine" verticais (reflexo do vidro) — colunas alternadas
    for y in range(3, 14):
        if y % 3 == 0:
            img.putpixel((4, y), (220, 230, 255, 150))
            img.putpixel((11, y), (220, 230, 255, 150))

    # Preenche o fluido conforme o nível (de baixo pra cima)
    # 14 pixels disponíveis (linhas 2..14, excluindo top metal e bottom border)
    fluid_top_row = {
        0: -1,      # vazio: sem fluido
        1: 12,      # 25%: do bottom até y=12 (3 linhas)
        2: 8,       # 50%: até y=8 (7 linhas)
        3: 5,       # 75%: até y=5 (10 linhas)
        4: 2,       # cheio: até y=2 (13 linhas)
    }
    top = fluid_top_row[level]
    if top >= 0:
        for y in range(top, 15):
            for x in range(2, 14):
                # Cor violeta: gradient mais escuro embaixo, claro em cima
                pct = (y - top) / max(1, 14 - top)
                r = int(0x80 + (1 - pct) * 0x20)
                g = int(0x60 + (1 - pct) * 0x20)
                b = int(0xE0 + (1 - pct) * 0x10)
                img.putpixel((x, y), (r, g, b, 230))
        # Highlight na linha do topo do fluido (brilho)
        for x in range(2, 14):
            img.putpixel((x, top), (200, 180, 255, 240))

    return img


def make_pipe(name: str, color: tuple, accent: tuple) -> Image.Image:
    """Gera pipe texture 16x16 com gradient + borda metálica nas pontas."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    # Faixa horizontal central — pipe cylinder
    # Linhas 4..11 (centro do cubo)
    for y in range(4, 12):
        for x in range(0, 16):
            # Gradient vertical: mais escuro nas extremidades, claro no meio
            d = abs(y - 7.5)
            pct = 1 - (d / 3.5)
            r = int(color[0] * pct + 30 * (1 - pct))
            g = int(color[1] * pct + 30 * (1 - pct))
            b = int(color[2] * pct + 30 * (1 - pct))
            img.putpixel((x, y), (r, g, b, 255))

    # Borda metálica cinza nas pontas (3 pixels esquerda e direita)
    for y in range(4, 12):
        for x in (0, 1, 14, 15):
            img.putpixel((x, y), (100, 100, 110, 255))
        for x in (2, 13):
            img.putpixel((x, y), (140, 140, 150, 255))

    # Linhas pretas nas bordas top/bottom do pipe
    for x in range(0, 16):
        img.putpixel((x, 3), (40, 40, 50, 255))
        img.putpixel((x, 12), (40, 40, 50, 255))

    # Detalhe brilhante no centro horizontal
    for x in range(3, 13):
        img.putpixel((x, 7), accent)

    return img


def make_extractor(face: str) -> Image.Image:
    """Gera face do extractor (top/side/front). Runa violeta sobre preto."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    # Base preta com bordas mais escuras
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (10, 5, 20, 255))

    # Borda metálica cinza
    for x in range(16):
        img.putpixel((x, 0), (80, 80, 90, 255))
        img.putpixel((x, 15), (80, 80, 90, 255))
    for y in range(16):
        img.putpixel((0, y), (80, 80, 90, 255))
        img.putpixel((15, y), (80, 80, 90, 255))

    if face == "front":
        # Runa central pulsante — placeholder estático: estrela 4-pontas violeta
        # Círculo central
        for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:
            img.putpixel((x, y), (200, 100, 255, 255))
        # Pontas (diagonal)
        for x, y, c in [
            (5, 5, (140, 60, 200, 255)),
            (10, 5, (140, 60, 200, 255)),
            (5, 10, (140, 60, 200, 255)),
            (10, 10, (140, 60, 200, 255)),
        ]:
            img.putpixel((x, y), c)
        # Linhas conectando
        for i in range(6, 10):
            img.putpixel((i, 4), (100, 40, 180, 255))
            img.putpixel((i, 11), (100, 40, 180, 255))
            img.putpixel((4, i), (100, 40, 180, 255))
            img.putpixel((11, i), (100, 40, 180, 255))
        # Sparkles
        img.putpixel((2, 2), (220, 180, 255, 200))
        img.putpixel((13, 2), (220, 180, 255, 200))
        img.putpixel((2, 13), (220, 180, 255, 200))
        img.putpixel((13, 13), (220, 180, 255, 200))
    elif face == "top":
        # Top: rune circular simples + indicador central
        for r, c in [(1, (100, 40, 180, 255)), (2, (140, 60, 200, 255))]:
            for x, y in [(7 - r, 7), (8 + r - 1, 7), (7, 7 - r), (7, 8 + r - 1),
                         (7 - r, 8), (8 + r - 1, 8), (8, 7 - r), (8, 8 + r - 1)]:
                if 0 <= x < 16 and 0 <= y < 16:
                    img.putpixel((x, y), c)
        for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:
            img.putpixel((x, y), (220, 150, 255, 255))
    else:  # side
        # Lateral: linhas verticais decorativas + faixa central
        for y in range(3, 13):
            img.putpixel((3, y), (60, 30, 100, 255))
            img.putpixel((12, y), (60, 30, 100, 255))
        # Faixa central horizontal
        for x in range(4, 12):
            img.putpixel((x, 7), (140, 60, 200, 255))
            img.putpixel((x, 8), (100, 40, 180, 255))

    return img


def main():
    # Tanks
    for level, name in [(0, "empty"), (1, "25"), (2, "50"), (3, "75"), (4, "full")]:
        img = make_tank(level)
        path = os.path.join(OUT_DIR, f"matter_tank_{name}.png")
        img.save(path)
        print(f"Wrote {path}")

    # Pipes
    pipe_specs = [
        ("dark",   (110, 60, 180), (200, 120, 255, 255)),     # violeta escuro com accent brilhante
        ("clear",  (220, 230, 255), (255, 255, 255, 255)),    # branco perolado
        ("yellow", (255, 217, 74), (255, 255, 150, 255)),     # dourado
    ]
    for name, color, accent in pipe_specs:
        img = make_pipe(name, color, accent)
        path = os.path.join(OUT_DIR, f"matter_pipe_{name}.png")
        img.save(path)
        print(f"Wrote {path}")

    # Extractor faces
    for face in ["top", "side", "front"]:
        img = make_extractor(face)
        path = os.path.join(OUT_DIR, f"matter_extractor_{face}.png")
        img.save(path)
        print(f"Wrote {path}")

    print("Done.")


if __name__ == "__main__":
    main()
