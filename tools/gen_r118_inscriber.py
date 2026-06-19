"""r118: GUI texture (176x166) + block texture (16x16) pro Glyph Inscriber."""
from PIL import Image, ImageDraw
import os, json, math

ROOT = os.path.dirname(__file__)
TEX_GUI = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                         "assets", "liberthia", "textures", "gui"))
TEX_BLOCK = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                           "assets", "liberthia", "textures", "block"))
MODEL_BLOCK = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                             "assets", "liberthia", "models", "block"))
MODEL_ITEM = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                            "assets", "liberthia", "models", "item"))
BLOCKSTATES = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                             "assets", "liberthia", "blockstates"))
for d in [TEX_GUI, TEX_BLOCK, MODEL_BLOCK, MODEL_ITEM, BLOCKSTATES]:
    os.makedirs(d, exist_ok=True)


# ─── GUI background 256x256 (only 176x166 used) ────────────────────
def make_gui_bg():
    """Background bonito: parchment central com borda roxa + ornaments runicos."""
    # MC GUI textures são geralmente 256x256 com a area do GUI no top-left
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    W, H = 176, 166
    # Outer border (purple frame)
    d.rectangle([0, 0, W-1, H-1], fill=(40, 20, 60, 255))
    # Inner panel (parchment-like, dark purple)
    d.rectangle([3, 3, W-4, H-4], fill=(80, 60, 110, 255))
    d.rectangle([4, 4, W-5, H-5], fill=(120, 100, 140, 255))
    d.rectangle([6, 6, W-7, H-7], fill=(178, 158, 184, 255))

    # Glowing top header (title area)
    d.rectangle([6, 6, W-7, 18], fill=(60, 30, 100, 255))
    # Header underline
    d.line([(6, 18), (W-7, 18)], fill=(180, 130, 220, 255))

    # ─── Slot tiles (cross pattern around center y=35) ────────────
    # Standard MC slot is 18x18 with the inner 16x16 dark
    def slot(cx, cy):
        # Outer recess
        d.rectangle([cx - 1, cy - 1, cx + 17, cy + 17], fill=(55, 35, 75, 255))
        # Inner dark
        d.rectangle([cx, cy, cx + 16, cy + 16], fill=(35, 18, 50, 255))

    # 4 inputs (cross) + output
    # Slot positions (sync com Menu.addSlot — offset -1 because slot adds 1)
    for cx, cy in [(80-1, 17-1), (80-1, 53-1), (62-1, 35-1), (98-1, 35-1)]:
        slot(cx, cy)
    # Output slot (with golden border)
    cx, cy = 134-1, 35-1
    d.rectangle([cx - 2, cy - 2, cx + 18, cy + 18], fill=(180, 140, 60, 255))
    d.rectangle([cx, cy, cx + 16, cy + 16], fill=(35, 18, 50, 255))

    # ─── Runic ornaments on corners ───────────────────────────────
    # Top-left ornament
    d.line([(10, 22), (16, 22)], fill=(180, 130, 220, 255))
    d.line([(10, 22), (10, 28)], fill=(180, 130, 220, 255))
    d.point((10, 22), fill=(255, 200, 255, 255))
    # Top-right
    d.line([(W-17, 22), (W-11, 22)], fill=(180, 130, 220, 255))
    d.line([(W-11, 22), (W-11, 28)], fill=(180, 130, 220, 255))
    d.point((W-11, 22), fill=(255, 200, 255, 255))

    # Bottom decorative rune (separator above inventory)
    d.line([(8, 73), (W-9, 73)], fill=(120, 80, 160, 255))
    # 3 runes on the separator
    for x in [40, 88, 136]:
        d.ellipse([x-2, 71, x+2, 75], outline=(255, 200, 255, 255))
        d.point((x, 73), fill=(255, 230, 255, 255))

    # ─── Player inventory area (8..165, 84..142) standard ─────────
    # Vanilla MC: player inventory slots start at (8, 84), hotbar at (8, 142)
    # 3 rows of 9 + 1 hotbar row
    for row in range(3):
        for col in range(9):
            cx_p = 8 + col * 18
            cy_p = 84 + row * 18
            d.rectangle([cx_p - 1, cy_p - 1, cx_p + 17, cy_p + 17], fill=(55, 35, 75, 255))
            d.rectangle([cx_p, cy_p, cx_p + 16, cy_p + 16], fill=(35, 18, 50, 255))
    # Hotbar
    for col in range(9):
        cx_p = 8 + col * 18
        cy_p = 142
        d.rectangle([cx_p - 1, cy_p - 1, cx_p + 17, cy_p + 17], fill=(75, 50, 100, 255))
        d.rectangle([cx_p, cy_p, cx_p + 16, cy_p + 16], fill=(35, 18, 50, 255))

    return img


# ─── Block texture ─────────────────────────────────────────────────
def make_inscriber_block():
    """Top do bloco: parchment com glyph runico."""
    img = Image.new("RGBA", (16, 16), (60, 40, 80, 255))
    d = ImageDraw.Draw(img)
    # Wood/parchment central
    d.rectangle([1, 1, 14, 14], fill=(150, 110, 80, 255))
    d.rectangle([2, 2, 13, 13], fill=(200, 170, 130, 255))
    # Carved rune (central circle + 4 points)
    d.ellipse([5, 5, 11, 11], outline=(80, 40, 120, 255))
    d.point((8, 4), fill=(120, 60, 180, 255))
    d.point((8, 11), fill=(120, 60, 180, 255))
    d.point((4, 8), fill=(120, 60, 180, 255))
    d.point((11, 8), fill=(120, 60, 180, 255))
    # Central glow
    d.point((7, 7), fill=(255, 200, 255, 255))
    d.point((8, 8), fill=(255, 200, 255, 255))
    return img


make_gui_bg().save(os.path.join(TEX_GUI, "glyph_inscriber.png"))
make_inscriber_block().save(os.path.join(TEX_BLOCK, "glyph_inscriber.png"))

# Block model JSON
with open(os.path.join(MODEL_BLOCK, "glyph_inscriber.json"), "w") as f:
    json.dump({"parent": "minecraft:block/cube_all",
               "textures": {"all": "liberthia:block/glyph_inscriber"}}, f)

# Blockstate
with open(os.path.join(BLOCKSTATES, "glyph_inscriber.json"), "w") as f:
    json.dump({"variants": {"": {"model": "liberthia:block/glyph_inscriber"}}}, f)

# Item model (block-parented)
with open(os.path.join(MODEL_ITEM, "glyph_inscriber.json"), "w") as f:
    json.dump({"parent": "liberthia:block/glyph_inscriber"}, f)

print("r118 Glyph Inscriber assets gerados: GUI bg 256x256 + block 16x16 + 3 JSONs")
