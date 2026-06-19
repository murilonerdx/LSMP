#!/usr/bin/env python3
"""r164: Gera texturas 16x16 + model JSON + lang entries para os 9 magic
accessories (rings, gloves, belts × mana/sanity/power).

Cada item tem cor temática e shape básico — ring=anel circular, glove=mão,
belt=fivela horizontal. Glow sutil baseado no efeito.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
TEX_DIR = ROOT / "assets" / "liberthia" / "textures" / "item"
MODEL_DIR = ROOT / "assets" / "liberthia" / "models" / "item"
LANG_FILE = ROOT / "assets" / "liberthia" / "lang" / "en_us.json"
LANG_PT = ROOT / "assets" / "liberthia" / "lang" / "pt_br.json"

# Effect colors (gem + glow)
EFFECT_COLORS = {
    "mana":   {"gem": (85, 200, 255),  "glow": (140, 220, 255), "dark": (30, 70, 110)},
    "sanity": {"gem": (192, 128, 255), "glow": (220, 180, 255), "dark": (80, 40, 130)},
    "power":  {"gem": (255, 119, 102), "glow": (255, 170, 130), "dark": (130, 50, 30)},
}

# Slot shapes
def draw_ring(img, gem_color, glow_color, dark_color):
    """Anel: circulo dourado com gem no topo."""
    draw = ImageDraw.Draw(img, 'RGBA')
    # Ring band (gold)
    gold = (200, 165, 60, 255)
    gold_d = (140, 110, 30, 255)
    # Outer circle
    for x in range(2, 14):
        for y in range(4, 14):
            cx, cy = x - 7.5, y - 9
            r2 = cx * cx + cy * cy
            if 16 <= r2 <= 36:
                img.putpixel((x, y), gold)
            elif 9 < r2 < 16:
                img.putpixel((x, y), gold_d)
    # Gem on top (3x3)
    img.putpixel((7, 4), gem_color + (255,))
    img.putpixel((8, 4), gem_color + (255,))
    img.putpixel((7, 3), glow_color + (255,))
    img.putpixel((8, 3), glow_color + (255,))
    img.putpixel((6, 4), dark_color + (255,))
    img.putpixel((9, 4), dark_color + (255,))


def draw_glove(img, gem_color, glow_color, dark_color):
    """Luva: silhueta de mão com gem no dorso."""
    draw = ImageDraw.Draw(img, 'RGBA')
    leather = (90, 50, 30, 255)
    leather_l = (130, 80, 50, 255)
    leather_d = (60, 30, 15, 255)
    # Palm
    for x in range(4, 12):
        for y in range(6, 14):
            img.putpixel((x, y), leather)
    # Highlights left edge
    for y in range(6, 14):
        img.putpixel((4, y), leather_d)
        img.putpixel((11, y), leather_d)
    # Fingers
    for fx in [4, 6, 8, 10]:
        for y in range(3, 7):
            img.putpixel((fx, y), leather)
            img.putpixel((fx + 1, y), leather_l)
    # Thumb
    img.putpixel((3, 8), leather)
    img.putpixel((3, 9), leather)
    img.putpixel((3, 10), leather_l)
    # Gem on back (3x2)
    img.putpixel((7, 9), gem_color + (255,))
    img.putpixel((8, 9), gem_color + (255,))
    img.putpixel((7, 10), glow_color + (255,))
    img.putpixel((8, 10), glow_color + (255,))
    # Cuff
    for x in range(3, 13):
        img.putpixel((x, 14), dark_color + (255,))
    for x in range(3, 13):
        img.putpixel((x, 13), gem_color + (255,))


def draw_belt(img, gem_color, glow_color, dark_color):
    """Cinto: fita horizontal com fivela central."""
    leather = (60, 40, 25, 255)
    leather_l = (100, 70, 45, 255)
    leather_d = (35, 20, 10, 255)
    metal = (180, 165, 130, 255)
    metal_d = (110, 95, 70, 255)
    # Strap
    for x in range(0, 16):
        img.putpixel((x, 7), leather_d)
        img.putpixel((x, 8), leather)
        img.putpixel((x, 9), leather)
        img.putpixel((x, 10), leather_l)
        img.putpixel((x, 11), leather_d)
    # Buckle (4x4 center)
    for x in range(6, 11):
        for y in range(5, 13):
            img.putpixel((x, y), metal)
    # Buckle outline
    for y in range(5, 13):
        img.putpixel((6, y), metal_d)
        img.putpixel((10, y), metal_d)
    for x in range(6, 11):
        img.putpixel((x, 5), metal_d)
        img.putpixel((x, 12), metal_d)
    # Gem in buckle center (3x3)
    for x in range(7, 10):
        for y in range(7, 11):
            img.putpixel((x, y), dark_color + (255,))
    img.putpixel((8, 8), gem_color + (255,))
    img.putpixel((8, 9), gem_color + (255,))
    img.putpixel((7, 9), glow_color + (255,))
    img.putpixel((9, 9), glow_color + (255,))


SHAPE_FUNCS = {"ring": draw_ring, "glove": draw_glove, "belt": draw_belt}


ITEMS = [
    # (item_id, slot_kind, effect_key, display_en, display_pt)
    ("ring_mana_flow",        "ring",  "mana",   "Ring of Mana Flow",      "Anel do Fluxo de Mana"),
    ("ring_lucidity",         "ring",  "sanity", "Ring of Lucidity",       "Anel da Lucidez"),
    ("ring_arcane_power",     "ring",  "power",  "Ring of Arcane Power",   "Anel do Poder Arcano"),
    ("glove_mana_channeler",  "glove", "mana",   "Mana Channeler's Glove", "Luva do Canalizador"),
    ("glove_lucid_veil",      "glove", "sanity", "Lucid Veil Glove",       "Luva do Véu Lúcido"),
    ("glove_battlemage",      "glove", "power",  "Battlemage Gauntlet",    "Manopla do Mago de Guerra"),
    ("belt_mana_reservoir",   "belt",  "mana",   "Mana Reservoir Belt",    "Cinto do Reservatório de Mana"),
    ("belt_sanctum_sash",     "belt",  "sanity", "Sanctum Sash",           "Faixa do Santuário"),
    ("belt_archmage",         "belt",  "power",  "Archmage Belt",          "Cinto do Arquimago"),
]


def gen_texture(item_id, shape, effect):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colors = EFFECT_COLORS[effect]
    SHAPE_FUNCS[shape](img, colors["gem"], colors["glow"], colors["dark"])
    out = TEX_DIR / f"{item_id}.png"
    img.save(out)
    return out


def gen_model(item_id):
    model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{item_id}"}
    }
    out = MODEL_DIR / f"{item_id}.json"
    out.write_text(json.dumps(model, indent=2), encoding="utf-8")
    return out


def update_lang():
    # English
    en = json.loads(LANG_FILE.read_text(encoding="utf-8")) if LANG_FILE.exists() else {}
    pt = json.loads(LANG_PT.read_text(encoding="utf-8")) if LANG_PT.exists() else {}
    for item_id, shape, effect, en_name, pt_name in ITEMS:
        key = f"item.liberthia.{item_id}"
        en[key] = en_name
        pt[key] = pt_name
    LANG_FILE.write_text(json.dumps(en, indent=2, ensure_ascii=False), encoding="utf-8")
    LANG_PT.write_text(json.dumps(pt, indent=2, ensure_ascii=False), encoding="utf-8")


def main():
    for item_id, shape, effect, _, _ in ITEMS:
        gen_texture(item_id, shape, effect)
        gen_model(item_id)
        print(f"  OK {item_id} ({shape} / {effect})")
    update_lang()
    print(f"\nDONE — {len(ITEMS)} textures, models, lang entries gerados.")


if __name__ == "__main__":
    main()
