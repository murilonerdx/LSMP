#!/usr/bin/env python3
"""
Gera texturas 16x16 PNG das 3 pílulas de matter (clear/dark/yellow).

Cada pílula é uma cápsula horizontal pixel-art com 2 metades:
  - metade esquerda: cor base da matter
  - metade direita: cor secundária (clareada)

Layout estilo Minecraft (sem anti-alias, cores chapadas, pixels nítidos):

    ....RRRRRRRRR.....
    ...RRRRRRRRRRRR....
    ..RRRRRRRRrrrrrr...
    .RRRRRRRRRrrrrrr...
    .RRRRRRRRRrrrrrr...
    ..RRRRRRRRrrrrrr...
    ...RRRRRRRRRR.....

Roda: python tools/generate-matter-pills-textures.py
Saída: src/main/resources/assets/liberthia/textures/item/{clear,dark,yellow}_matter_pill.png
"""
from pathlib import Path
from PIL import Image, ImageDraw

# ────────────────────────────────────────────────────────────────────────
# PALETAS — base, brilho, sombra. Estilo Minecraft (cores chapadas).
# ────────────────────────────────────────────────────────────────────────

# Clear Matter Pill — azul muito claro + branco leitoso
CLEAR_BASE     = (224, 240, 255, 255)  # azul-gelo
CLEAR_HIGHLIGHT = (255, 255, 255, 255) # branco puro
CLEAR_SHADOW   = (170, 200, 230, 255)  # azul mais saturado

# Dark Matter Pill — preto roxo + violeta neon
DARK_BASE      = (20, 10, 30, 255)     # quase preto com tom roxo
DARK_HIGHLIGHT = (100, 50, 150, 255)   # roxo neon
DARK_SHADOW    = (5, 0, 10, 255)       # preto

# Yellow Matter Pill — amarelo vivo + amarelo limão
YELLOW_BASE     = (240, 200, 40, 255)  # amarelo ouro
YELLOW_HIGHLIGHT = (255, 240, 100, 255) # amarelo limão claro
YELLOW_SHADOW   = (180, 130, 0, 255)   # amarelo queimado

# Outline preto pra dar definição (estilo Minecraft)
OUTLINE = (0, 0, 0, 255)

OUT_DIR = Path("src/main/resources/assets/liberthia/textures/item")


def draw_pill(base_left, hi_left, sh_left,
              base_right, hi_right, sh_right,
              path: Path):
    """
    Pinta uma cápsula 16x16 com 2 metades. Metade esquerda usa as cores
    left, direita usa as cores right. Sombra na borda inferior, highlight
    na borda superior.
    """
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # Estrutura da cápsula — linhas y=4 a y=11 contêm o pill.
    # Bordas (outline) usam OUTLINE preto.
    # Layout dos pixels (16 colunas, 8 linhas úteis):
    #   col 2,3 = arredondamento esquerdo
    #   col 4..7 = metade esquerda (base_left)
    #   col 8..11 = metade direita (base_right)
    #   col 12,13 = arredondamento direito
    #
    # Cada linha tem:
    #   y=4: outline top (col 4-11)
    #   y=5,6: highlight (lighter)
    #   y=7,8,9: base
    #   y=10: shadow
    #   y=11: outline bottom

    def left_color(y):
        if y == 4 or y == 11: return OUTLINE
        if y in (5, 6):       return hi_left
        if y == 10:           return sh_left
        return base_left

    def right_color(y):
        if y == 4 or y == 11: return OUTLINE
        if y in (5, 6):       return hi_right
        if y == 10:           return sh_right
        return base_right

    # Pinta o corpo da cápsula (col 4..11)
    for y in range(4, 12):
        for x in range(4, 8):
            px[x, y] = left_color(y)
        for x in range(8, 12):
            px[x, y] = right_color(y)

    # Arredondamento esquerdo (col 2, 3) — vai estreitando
    px[2, 6] = OUTLINE
    px[2, 7] = OUTLINE
    px[2, 8] = OUTLINE
    px[2, 9] = OUTLINE
    px[3, 5] = OUTLINE
    px[3, 6] = hi_left
    px[3, 7] = base_left
    px[3, 8] = base_left
    px[3, 9] = sh_left
    px[3, 10] = OUTLINE

    # Arredondamento direito (col 12, 13)
    px[13, 6] = OUTLINE
    px[13, 7] = OUTLINE
    px[13, 8] = OUTLINE
    px[13, 9] = OUTLINE
    px[12, 5] = OUTLINE
    px[12, 6] = hi_right
    px[12, 7] = base_right
    px[12, 8] = base_right
    px[12, 9] = sh_right
    px[12, 10] = OUTLINE

    # Linha divisória central (col 7→8) — outline preto sutil entre as 2 metades
    for y in range(5, 11):
        px[7, y] = OUTLINE
        # col 8 fica como base_right normal (sem mexer)

    # Brilho diagonal (sparkle) — 1 pixel branco extra no canto sup-esq
    # pra dar reflexo de superfície brilhante (estilo poção do MC)
    px[5, 5] = (255, 255, 255, 200)

    # Salva
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")
    print(f"  OK {path}")


def main():
    print("Gerando texturas das 3 pílulas de matter...")

    # Clear: branco-azulado em ambas metades (consistência visual com tema clear)
    draw_pill(
        CLEAR_BASE, CLEAR_HIGHLIGHT, CLEAR_SHADOW,
        CLEAR_BASE, CLEAR_HIGHLIGHT, CLEAR_SHADOW,
        OUT_DIR / "clear_matter_pill.png",
    )

    # Dark: preto/roxo em ambas metades
    draw_pill(
        DARK_BASE, DARK_HIGHLIGHT, DARK_SHADOW,
        DARK_BASE, DARK_HIGHLIGHT, DARK_SHADOW,
        OUT_DIR / "dark_matter_pill.png",
    )

    # Yellow: amarelo vivo
    draw_pill(
        YELLOW_BASE, YELLOW_HIGHLIGHT, YELLOW_SHADOW,
        YELLOW_BASE, YELLOW_HIGHLIGHT, YELLOW_SHADOW,
        OUT_DIR / "yellow_matter_pill.png",
    )

    print("\n3 texturas geradas em", OUT_DIR)


if __name__ == "__main__":
    main()
