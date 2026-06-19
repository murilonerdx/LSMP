"""r119: Procedural assets for Spell Weaver + Grimoire + Modifier glyphs.

Generates:
- gui/spell_weaver.png (256x256, area 176x166 used)
- gui/grimoire.png (256x256, area 176x200 used)
- block/spell_weaver.png (16x16)
- item/grimoire_book.png (16x16)
- item/modifier_*.png (13 modifier item textures, 16x16 each)
- models + blockstate + lang entries

Style: dark purple + gold ritual/mystic, consistent with r118 Inscriber.
"""
from PIL import Image, ImageDraw
import os, json, math

ROOT = os.path.dirname(__file__)
TEX_GUI    = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/gui"))
TEX_BLOCK  = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/block"))
TEX_ITEM   = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/item"))
MODEL_BLK  = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/models/block"))
MODEL_ITM  = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/models/item"))
BLOCKSTATES = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/blockstates"))
for d in [TEX_GUI, TEX_BLOCK, TEX_ITEM, MODEL_BLK, MODEL_ITM, BLOCKSTATES]:
    os.makedirs(d, exist_ok=True)


def gui_panel(W, H, base, top_band, accent):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W-1, H-1], fill=(20, 10, 35, 255))
    d.rectangle([3, 3, W-4, H-4], fill=base)
    d.rectangle([6, 6, W-7, 18], fill=top_band)
    d.line([(6, 18), (W-7, 18)], fill=accent)
    return img, d


def slot(d, cx, cy, recess=(45, 25, 65, 255), inner=(25, 15, 45, 255)):
    d.rectangle([cx - 1, cy - 1, cx + 17, cy + 17], fill=recess)
    d.rectangle([cx, cy, cx + 16, cy + 16], fill=inner)


def gold_slot(d, cx, cy):
    d.rectangle([cx - 2, cy - 2, cx + 18, cy + 18], fill=(180, 140, 60, 255))
    d.rectangle([cx, cy, cx + 16, cy + 16], fill=(25, 15, 45, 255))


def make_spell_weaver_gui():
    W, H = 176, 166
    img, d = gui_panel(W, H,
                       base=(80, 50, 100, 255),
                       top_band=(60, 25, 90, 255),
                       accent=(220, 180, 60, 255))
    # inner lighter panel
    d.rectangle([6, 22, W-7, 64], fill=(150, 110, 170, 255))

    # base slot @ (20,20) — left
    gold_slot(d, 20-1, 20-1)
    # 7 modifier slots @ (25 + i*18, 55)
    for i in range(7):
        slot(d, 25 + i * 18 - 1, 55 - 1)
    # output slot @ (152, 20)
    gold_slot(d, 152-1, 20-1)

    # decorative "→" between base and modifiers (small purple line)
    for x in range(38, 152, 4):
        d.point((x, 28), fill=(255, 230, 100, 200))

    # player inv area
    for row in range(3):
        for col in range(9):
            slot(d, 8 + col * 18 - 1, 84 + row * 18 - 1)
    for col in range(9):
        slot(d, 8 + col * 18 - 1, 142 - 1, recess=(75, 50, 100, 255))

    img.save(os.path.join(TEX_GUI, "spell_weaver.png"))


def make_grimoire_gui():
    W, H = 176, 200
    img, d = gui_panel(W, H,
                       base=(70, 30, 30, 255),  # leather brown
                       top_band=(40, 15, 15, 255),
                       accent=(220, 180, 60, 255))
    # Header decoration
    d.rectangle([6, 22, W-7, 28], fill=(120, 60, 40, 255))
    # 9 scroll slots in row @ y=30
    for i in range(9):
        slot(d, 8 + i * 18 - 1, 30 - 1)
    # player inv @ y=70 (3 rows) + hotbar @ y=128
    for row in range(3):
        for col in range(9):
            slot(d, 8 + col * 18 - 1, 70 + row * 18 - 1)
    for col in range(9):
        slot(d, 8 + col * 18 - 1, 128 - 1, recess=(75, 50, 100, 255))
    img.save(os.path.join(TEX_GUI, "grimoire.png"))


def make_spell_weaver_block():
    img = Image.new("RGBA", (16, 16), (45, 25, 70, 255))
    d = ImageDraw.Draw(img)
    # Octagonal base
    d.polygon([(2, 6), (5, 3), (10, 3), (13, 6), (13, 12), (10, 15), (5, 15), (2, 12)],
              fill=(130, 90, 160, 255))
    # Central rune (8-point star)
    d.point((8, 5), fill=(255, 230, 100, 255))
    d.point((8, 11), fill=(255, 230, 100, 255))
    d.point((4, 8), fill=(255, 230, 100, 255))
    d.point((12, 8), fill=(255, 230, 100, 255))
    d.point((5, 5), fill=(255, 200, 80, 255))
    d.point((11, 5), fill=(255, 200, 80, 255))
    d.point((5, 11), fill=(255, 200, 80, 255))
    d.point((11, 11), fill=(255, 200, 80, 255))
    d.point((8, 8), fill=(255, 255, 255, 255))
    img.save(os.path.join(TEX_BLOCK, "spell_weaver.png"))


def make_grimoire_book():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Book body — leather brown
    d.rectangle([2, 2, 13, 14], fill=(100, 50, 30, 255))
    # Cover detail
    d.rectangle([3, 3, 12, 13], fill=(140, 70, 40, 255))
    # Pages on right (open look)
    d.rectangle([12, 3, 14, 13], fill=(230, 220, 180, 255))
    d.line([(12, 3), (12, 13)], fill=(180, 170, 130, 255))
    # Golden clasp
    d.rectangle([6, 7, 9, 10], fill=(220, 180, 60, 255))
    d.point((7, 8), fill=(255, 230, 80, 255))
    # Glowing rune in center
    d.point((7, 6), fill=(255, 100, 250, 255))
    d.point((8, 5), fill=(255, 150, 250, 255))
    img.save(os.path.join(TEX_ITEM, "grimoire_book.png"))


# Modifier glyph icons — distinct iconography per modifier
MODIFIER_ICONS = {
    "amplify": [(255, 180, 0), "+"],
    "aoe":     [(255, 80, 80), "o"],
    "pierce":  [(220, 220, 220), "|"],
    "multishot": [(80, 200, 255), "/"],
    "chain":   [(255, 240, 80), "z"],
    "ignite":  [(255, 100, 50), "F"],
    "freeze":  [(150, 220, 255), "*"],
    "knockback": [(180, 180, 180), ">"],
    "lifesteal": [(180, 30, 30), "V"],
    "range":   [(80, 220, 80), "→"],
    "sustain": [(220, 100, 220), "~"],
    "penetrate": [(50, 50, 80), "X"],
    "apolao":  [(180, 0, 0), "★"],
}


def make_modifier_icon(name, color, symbol):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Disk background — circle filled with darker shade of color
    dim = tuple(max(0, c // 3) for c in color) + (255,)
    bright = color + (255,)
    d.ellipse([1, 1, 14, 14], fill=dim)
    d.ellipse([3, 3, 12, 12], fill=bright)
    # Light dot in center
    d.point((7, 7), fill=(255, 255, 255, 255))
    d.point((8, 8), fill=(255, 255, 255, 255))
    # Border ring
    d.ellipse([1, 1, 14, 14], outline=(220, 180, 60, 255))
    # Sigil rune marks (simple)
    if symbol == "+":
        d.line([(7, 4), (7, 11)], fill=(255, 255, 255, 255))
        d.line([(4, 7), (11, 7)], fill=(255, 255, 255, 255))
    elif symbol == "o":
        d.ellipse([5, 5, 10, 10], outline=(255, 255, 255, 255))
    elif symbol == "|":
        d.line([(7, 3), (7, 12)], fill=(255, 255, 255, 255))
        d.line([(8, 3), (8, 12)], fill=(255, 255, 255, 255))
    elif symbol == "/":
        d.line([(4, 11), (11, 4)], fill=(255, 255, 255, 255))
        d.line([(5, 11), (5, 11)], fill=(255, 255, 255, 255))
    elif symbol == "z":
        d.line([(5, 5), (9, 5)], fill=(255, 255, 255, 255))
        d.line([(9, 5), (5, 10)], fill=(255, 255, 255, 255))
        d.line([(5, 10), (10, 10)], fill=(255, 255, 255, 255))
    elif symbol == "F":
        d.line([(5, 4), (5, 11)], fill=(255, 255, 255, 255))
        d.line([(5, 4), (10, 4)], fill=(255, 255, 255, 255))
        d.line([(5, 7), (9, 7)], fill=(255, 255, 255, 255))
    elif symbol == "*":
        d.point((7, 4), fill=(255, 255, 255, 255))
        d.point((7, 11), fill=(255, 255, 255, 255))
        d.point((4, 7), fill=(255, 255, 255, 255))
        d.point((11, 7), fill=(255, 255, 255, 255))
        d.point((4, 4), fill=(255, 255, 255, 255))
        d.point((11, 11), fill=(255, 255, 255, 255))
        d.point((4, 11), fill=(255, 255, 255, 255))
        d.point((11, 4), fill=(255, 255, 255, 255))
    elif symbol == ">":
        d.line([(4, 4), (10, 7)], fill=(255, 255, 255, 255))
        d.line([(4, 11), (10, 7)], fill=(255, 255, 255, 255))
    elif symbol == "V":
        d.line([(4, 4), (7, 11)], fill=(255, 255, 255, 255))
        d.line([(7, 11), (10, 4)], fill=(255, 255, 255, 255))
    elif symbol == "→":
        d.line([(3, 7), (12, 7)], fill=(255, 255, 255, 255))
        d.line([(9, 4), (12, 7)], fill=(255, 255, 255, 255))
        d.line([(9, 10), (12, 7)], fill=(255, 255, 255, 255))
    elif symbol == "~":
        d.line([(3, 7), (5, 5)], fill=(255, 255, 255, 255))
        d.line([(5, 5), (8, 9)], fill=(255, 255, 255, 255))
        d.line([(8, 9), (11, 5)], fill=(255, 255, 255, 255))
    elif symbol == "X":
        d.line([(4, 4), (11, 11)], fill=(255, 255, 255, 255))
        d.line([(4, 11), (11, 4)], fill=(255, 255, 255, 255))
    elif symbol == "★":
        # 5-pointed star
        pts = []
        cx, cy = 7.5, 7.5
        for i in range(10):
            angle = math.radians(i * 36 - 90)
            r = 5 if i % 2 == 0 else 2
            pts.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
        d.polygon(pts, fill=(255, 255, 100, 255), outline=(255, 230, 50, 255))
    img.save(os.path.join(TEX_ITEM, f"modifier_{name}.png"))


# Spell scroll textures for new spells (use existing pattern: solid color disk)
NEW_SPELLS = {
    "wings_of_source": (255, 240, 100),
    "phase_dash": (170, 100, 255),
    "wind_step": (200, 255, 200),
    "levitate_self": (200, 220, 255),
    "blink": (160, 80, 240),
    "solar_apocalypse": (255, 120, 30),
    "eldritch_meteor": (90, 30, 130),
    "divine_judgment_apolao": (255, 240, 200),
    "apocalypse": (200, 0, 50),
}


def make_spell_icon(name, color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Scroll body
    d.rectangle([2, 3, 13, 12], fill=(220, 200, 160, 255))
    d.rectangle([3, 4, 12, 11], fill=(245, 230, 195, 255))
    # Side rolls
    d.line([(2, 2), (2, 13)], fill=(180, 150, 100, 255))
    d.line([(13, 2), (13, 13)], fill=(180, 150, 100, 255))
    # Rune in center — colored
    cx, cy = 7, 7
    bright = color + (255,)
    d.ellipse([cx - 2, cy - 2, cx + 3, cy + 3], fill=bright)
    d.point((cx, cy), fill=(255, 255, 255, 255))
    # Top + bottom flourish
    d.point((7, 4), fill=bright)
    d.point((7, 10), fill=bright)
    img.save(os.path.join(TEX_ITEM, f"spell_{name}.png"))


# ───────── Generate everything ─────────
make_spell_weaver_gui()
make_grimoire_gui()
make_spell_weaver_block()
make_grimoire_book()

for name, (color, symbol) in MODIFIER_ICONS.items():
    make_modifier_icon(name, color, symbol)

for name, color in NEW_SPELLS.items():
    make_spell_icon(name, color)

# Block model JSON
with open(os.path.join(MODEL_BLK, "spell_weaver.json"), "w") as f:
    json.dump({"parent": "minecraft:block/cube_all",
               "textures": {"all": "liberthia:block/spell_weaver"}}, f)

with open(os.path.join(BLOCKSTATES, "spell_weaver.json"), "w") as f:
    json.dump({"variants": {"": {"model": "liberthia:block/spell_weaver"}}}, f)

with open(os.path.join(MODEL_ITM, "spell_weaver.json"), "w") as f:
    json.dump({"parent": "liberthia:block/spell_weaver"}, f)

with open(os.path.join(MODEL_ITM, "grimoire_book.json"), "w") as f:
    json.dump({"parent": "minecraft:item/generated",
               "textures": {"layer0": "liberthia:item/grimoire_book"}}, f)

# Item model JSONs for modifiers
for name in MODIFIER_ICONS.keys():
    with open(os.path.join(MODEL_ITM, f"modifier_{name}.json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"liberthia:item/modifier_{name}"}}, f)

# Item model JSONs for new spells
for name in NEW_SPELLS.keys():
    with open(os.path.join(MODEL_ITM, f"spell_{name}.json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"liberthia:item/spell_{name}"}}, f)

print(f"r119: Generated assets — 2 GUIs, 1 block, 1 grimoire item, {len(MODIFIER_ICONS)} modifier icons, "
      f"{len(NEW_SPELLS)} spell icons, all JSONs.")
