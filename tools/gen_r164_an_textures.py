#!/usr/bin/env python3
"""r164: Gera texturas TEMÁTICAS pros blocos que tinham texturas genéricas:
- 4 source links (volcanic/mycelial/vitalic/alchemical) — pega do Ars-Nouveau ref
- spell_prism (cristal violeta refrativo)
- spell_turret (canhão arcano)
- spell_sensor (olho-cristal observador)
- scryer_oculus (esfera de cristal vidente)
- ritual_brazier (braseiro místico)
- mage_cauldron (caldeirão vanilla recolorido roxo)

Estratégia: 16x16 pixel art procedural cada uma com tema próprio.
"""
import shutil
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\block")
AN = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\Ars-Nouveau\src\main\resources\assets\ars_nouveau\textures\block")
ITEM_DIR = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures\item")


# ─── 1) Copia as 4 source link textures direto do Ars-Nouveau (assets quality já bons) ─
def copy_an_sourcelinks():
    for name in ["volcanic", "mycelial", "vitalic", "alchemical"]:
        src = AN / f"{name}_sourcelink.png"
        dst = ROOT / f"{name}_sourcelink.png"
        if src.exists():
            shutil.copy(src, dst)
            # Também duplica pro item texture
            shutil.copy(src, ITEM_DIR / f"{name}_sourcelink.png")
            print(f"  OK {name}_sourcelink.png (copiado de AN)")
        else:
            print(f"  SKIP {name} (AN não tem)")


# ─── 2) Texturas geradas procedurally ─
def gen_spell_prism():
    """Cristal violeta refrativo — quadrado roxo com facetas brilhantes."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    purple_dark = (60, 30, 100, 255)
    purple_mid = (130, 70, 200, 255)
    purple_bright = (180, 130, 240, 255)
    purple_glow = (220, 200, 255, 255)
    # Background dark
    for y in range(16):
        for x in range(16):
            px[x, y] = (30, 15, 50, 255)
    # Crystal diamond shape
    for y in range(2, 14):
        for x in range(2, 14):
            cx = 7.5 - x
            cy = 7.5 - y
            d = abs(cx) + abs(cy)
            if d < 6:
                if d < 2:
                    px[x, y] = purple_glow
                elif d < 4:
                    px[x, y] = purple_bright
                elif d < 5:
                    px[x, y] = purple_mid
                else:
                    px[x, y] = purple_dark
    # Highlights (4 corners)
    px[5, 5] = (255, 230, 255, 255)
    px[10, 10] = (255, 230, 255, 255)
    img.save(ROOT / "spell_prism.png")
    shutil.copy(ROOT / "spell_prism.png", ITEM_DIR / "spell_prism.png")
    print("  OK spell_prism.png (cristal violeta)")


def gen_spell_turret():
    """Canhão arcano — base cinza-azul + barril metálico + glow ciano."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (60, 70, 90, 255)
    base_d = (40, 50, 70, 255)
    metal = (140, 150, 170, 255)
    metal_d = (90, 100, 120, 255)
    rune_cyan = (80, 200, 255, 255)
    # Stone base
    for y in range(16):
        for x in range(16):
            px[x, y] = base_d if (x + y) % 4 == 0 else base
    # Barrel hole center (5x5)
    for y in range(6, 11):
        for x in range(6, 11):
            cx = 8 - x
            cy = 8 - y
            if cx*cx + cy*cy < 7:
                px[x, y] = metal
    # Inner hole
    for y in range(7, 10):
        for x in range(7, 10):
            px[x, y] = (10, 10, 20, 255)
    # Glow runes (corners)
    px[3, 3] = rune_cyan
    px[12, 3] = rune_cyan
    px[3, 12] = rune_cyan
    px[12, 12] = rune_cyan
    # Metal bands
    for x in range(16):
        if x % 2 == 0:
            px[x, 1] = metal_d
            px[x, 14] = metal_d
    img.save(ROOT / "spell_turret.png")
    shutil.copy(ROOT / "spell_turret.png", ITEM_DIR / "spell_turret.png")
    print("  OK spell_turret.png (canhão arcano)")


def gen_spell_sensor():
    """Olho-cristal observador — esfera vermelha que 'vê'."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    dark = (40, 30, 40, 255)
    iris_red = (200, 30, 30, 255)
    iris_dark = (130, 10, 10, 255)
    pupil = (10, 5, 5, 255)
    glow = (255, 200, 100, 255)
    # Base dark
    for y in range(16):
        for x in range(16):
            px[x, y] = dark
    # Iris (large red circle)
    for y in range(3, 14):
        for x in range(3, 14):
            cx = 7.5 - x
            cy = 7.5 - y
            d2 = cx*cx + cy*cy
            if d2 < 30:
                px[x, y] = iris_red
            if d2 < 8:
                px[x, y] = pupil
    # Outer ring
    for y in range(2, 14):
        for x in range(2, 14):
            cx = 7.5 - x
            cy = 7.5 - y
            d2 = cx*cx + cy*cy
            if 30 < d2 < 40:
                px[x, y] = iris_dark
    # Highlight (top-left)
    px[5, 5] = glow
    px[6, 5] = glow
    img.save(ROOT / "spell_sensor.png")
    shutil.copy(ROOT / "spell_sensor.png", ITEM_DIR / "spell_sensor.png")
    print("  OK spell_sensor.png (olho-cristal)")


def gen_scryer_oculus():
    """Esfera de cristal vidente — globo ciano translucent com runas."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (20, 25, 50, 255)
    glass = (80, 180, 220, 220)
    glass_high = (180, 230, 255, 240)
    star = (255, 255, 200, 255)
    rune = (200, 220, 255, 255)
    # Dark base
    for y in range(16):
        for x in range(16):
            px[x, y] = base
    # Glass sphere
    for y in range(2, 14):
        for x in range(2, 14):
            cx = 7.5 - x
            cy = 7.5 - y
            d2 = cx*cx + cy*cy
            if d2 < 35:
                # Apply gradient
                if d2 < 8:
                    px[x, y] = glass_high
                elif d2 < 20:
                    px[x, y] = glass
                else:
                    px[x, y] = (50, 100, 140, 220)
    # Star sparkles
    px[6, 5] = star
    px[9, 7] = star
    px[7, 10] = star
    # Runes around
    px[1, 8] = rune
    px[14, 8] = rune
    px[8, 1] = rune
    px[8, 14] = rune
    img.save(ROOT / "scryer_oculus.png")
    shutil.copy(ROOT / "scryer_oculus.png", ITEM_DIR / "scryer_oculus.png")
    print("  OK scryer_oculus.png (esfera vidente)")


def gen_ritual_brazier():
    """Braseiro místico — taça de pedra com chama dourada/roxa."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    stone_d = (50, 45, 50, 255)
    stone = (90, 80, 90, 255)
    stone_l = (130, 120, 130, 255)
    fire_gold = (255, 200, 100, 255)
    fire_red = (255, 100, 30, 255)
    fire_purple = (200, 100, 255, 255)
    # Bottom: stone bowl
    for x in range(16):
        for y in range(10, 16):
            cx = abs(x - 7.5)
            if cx > 7: c = (0, 0, 0, 0)
            elif cx > 6: c = stone_d
            elif cx > 5: c = stone
            else: c = stone_l if (x + y) % 3 == 0 else stone
            px[x, y] = c
    # Top edge rim
    for x in range(2, 14):
        px[x, 9] = stone_d
        px[x, 10] = stone
    # Fire inside
    for x in range(4, 12):
        for y in range(4, 10):
            cx = abs(x - 7.5)
            cy = abs(y - 7)
            if cx + cy*0.5 < 4:
                if y < 6:
                    px[x, y] = fire_purple
                elif y < 8:
                    px[x, y] = fire_gold
                else:
                    px[x, y] = fire_red
    # Sparks
    px[5, 3] = fire_purple
    px[10, 4] = fire_gold
    img.save(ROOT / "ritual_brazier.png")
    shutil.copy(ROOT / "ritual_brazier.png", ITEM_DIR / "ritual_brazier.png")
    print("  OK ritual_brazier.png (braseiro místico)")


def gen_mage_cauldron():
    """Mage Cauldron — copia vanilla cauldron + tint roxo nos lados."""
    # Vanilla cauldron texture path
    import os
    vanilla = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\Ars-Nouveau\src\main\resources\assets\ars_nouveau\textures\block")
    # Sem vanilla acessível — gera procedural similar
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    iron_d = (40, 40, 45, 255)
    iron = (70, 70, 80, 255)
    iron_l = (100, 100, 115, 255)
    purple_glow = (140, 80, 200, 255)
    # Side texture (cauldron-like rivets)
    for y in range(16):
        for x in range(16):
            px[x, y] = iron
    # Vertical rivets
    for x in [2, 8, 14]:
        for y in range(2, 14):
            px[x, y] = iron_d
    # Top/bottom bands
    for x in range(16):
        px[x, 0] = iron_d
        px[x, 1] = iron_l
        px[x, 14] = iron_l
        px[x, 15] = iron_d
    # Purple glow runes
    px[5, 7] = purple_glow
    px[10, 7] = purple_glow
    px[7, 10] = purple_glow
    img.save(ROOT / "mage_cauldron.png")
    shutil.copy(ROOT / "mage_cauldron.png", ITEM_DIR / "mage_cauldron.png")
    print("  OK mage_cauldron.png (caldeirão roxo)")


def main():
    print("=== r164: Texturas AN-style ===\n")
    print("--- Source Links (copy AN) ---")
    copy_an_sourcelinks()
    print("\n--- Texturas procedurais ---")
    gen_spell_prism()
    gen_spell_turret()
    gen_spell_sensor()
    gen_scryer_oculus()
    gen_ritual_brazier()
    gen_mage_cauldron()
    print("\nDONE")


if __name__ == "__main__":
    main()
