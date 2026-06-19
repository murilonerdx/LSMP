#!/usr/bin/env python3
"""r159: Gera texturas + model JSONs + lang entries pra novos componentes
do Arcane Workbench.

Componentes:
- magic_tablet
- arcane_orb
- school_rune_{fire,ice,lightning,blood,eldritch,holy,nature}
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
TEX_DIR = ROOT / "assets" / "liberthia" / "textures" / "item"
MODEL_DIR = ROOT / "assets" / "liberthia" / "models" / "item"
EN_PATH = ROOT / "assets" / "liberthia" / "lang" / "en_us.json"
PT_PATH = ROOT / "assets" / "liberthia" / "lang" / "pt_br.json"

SCHOOL_COLORS = {
    "fire":      (255, 102, 51),
    "ice":       (102, 204, 255),
    "lightning": (255, 255, 68),
    "blood":     (153, 0,   51),
    "eldritch":  (122, 61,  255),
    "holy":      (255, 238, 170),
    "nature":    (51,  170, 51),
}


def gen_tablet():
    """Tablet: pedra esculpida com runa central."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Stone base
    for y in range(2, 14):
        for x in range(2, 14):
            v = int((math.sin(x * 0.8) + math.cos(y * 0.6)) * 8)
            r = 80 + v
            d.point((x, y), fill=(r, r - 10, r - 20, 255))
    # Engraved border
    for i in range(2, 14):
        d.point((i, 2), fill=(40, 30, 20, 255))
        d.point((i, 13), fill=(40, 30, 20, 255))
        d.point((2, i), fill=(40, 30, 20, 255))
        d.point((13, i), fill=(40, 30, 20, 255))
    # Center rune (X)
    for i in range(4, 12):
        d.point((i, i), fill=(180, 140, 80, 255))
        d.point((i, 15 - i), fill=(180, 140, 80, 255))
    img.save(TEX_DIR / "magic_tablet.png")


def gen_orb():
    """Orb: esfera roxa com glow."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 8, 8
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d2 = dx * dx + dy * dy
            if d2 <= 36:
                # Gradient: bright center → dark edges
                t = 1.0 - (d2 / 36.0)
                r = int(80 + 140 * t)
                g = int(40 + 80 * t)
                b = int(180 + 75 * t)
                img.putpixel((x, y), (r, g, b, 255))
            elif d2 <= 49:
                img.putpixel((x, y), (40, 20, 80, 200))
    # Highlight
    img.putpixel((6, 5), (255, 200, 255, 255))
    img.putpixel((7, 5), (240, 180, 240, 255))
    img.save(TEX_DIR / "arcane_orb.png")


def gen_school_rune(school: str, color):
    """School Rune: tablet com símbolo único da escola na cor da escola."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r, g, b = color
    # Dark hexagonal base
    pts = [(8, 1), (14, 4), (14, 11), (8, 14), (2, 11), (2, 4), (8, 1)]
    d.polygon(pts, fill=(30, 20, 50, 255), outline=(60, 40, 100, 255))
    # School symbol in middle
    if school == "fire":
        # flame
        for i in range(7):
            d.point((8, 4 + i), fill=(r, g, b, 255))
        d.point((6, 8), fill=(r, g, b, 255))
        d.point((10, 8), fill=(r, g, b, 255))
        d.point((7, 11), fill=(r, g, b, 255))
        d.point((9, 11), fill=(r, g, b, 255))
    elif school == "ice":
        # diamond
        for i in range(5):
            d.point((8 - i, 8 - 4 + i), fill=(r, g, b, 255))
            d.point((8 + i, 8 - 4 + i), fill=(r, g, b, 255))
            d.point((8 - i, 8 + 4 - i), fill=(r, g, b, 255))
            d.point((8 + i, 8 + 4 - i), fill=(r, g, b, 255))
    elif school == "lightning":
        # zig-zag bolt
        pts = [(8, 4), (6, 7), (9, 7), (7, 12)]
        for i in range(len(pts) - 1):
            d.line([pts[i], pts[i + 1]], fill=(r, g, b, 255), width=1)
    elif school == "blood":
        # drop
        d.ellipse((6, 6, 10, 11), fill=(r, g, b, 255))
        d.point((8, 4), fill=(r, g, b, 255))
        d.point((8, 5), fill=(r, g, b, 255))
    elif school == "eldritch":
        # eye
        d.ellipse((4, 6, 12, 10), outline=(r, g, b, 255), width=1)
        d.ellipse((6, 7, 10, 9), fill=(r, g, b, 255))
    elif school == "holy":
        # cross
        for i in range(4, 13):
            d.point((8, i), fill=(r, g, b, 255))
        for i in range(5, 12):
            d.point((i, 8), fill=(r, g, b, 255))
    elif school == "nature":
        # leaf
        d.ellipse((5, 5, 11, 12), outline=(r, g, b, 255), width=1)
        for i in range(5, 12):
            d.point((8, i), fill=(r, g, b, 255))
    img.save(TEX_DIR / f"school_rune_{school}.png")


def write_simple_model(name: str):
    """Cria item model generated apontando pra textura."""
    (MODEL_DIR / f"{name}.json").write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{name}"}
    }, indent=2), encoding="utf-8")


def update_lang():
    en_extra = {
        "item.liberthia.magic_tablet":         "Magic Tablet",
        "item.liberthia.arcane_orb":           "Arcane Orb",
        "item.liberthia.school_rune_fire":     "Fire School Rune",
        "item.liberthia.school_rune_ice":      "Ice School Rune",
        "item.liberthia.school_rune_lightning":"Lightning School Rune",
        "item.liberthia.school_rune_blood":    "Blood School Rune",
        "item.liberthia.school_rune_eldritch": "Eldritch School Rune",
        "item.liberthia.school_rune_holy":     "Holy School Rune",
        "item.liberthia.school_rune_nature":   "Nature School Rune",
    }
    pt_extra = {
        "item.liberthia.magic_tablet":         "Tablet Magica",
        "item.liberthia.arcane_orb":           "Orbe Arcano",
        "item.liberthia.school_rune_fire":     "Runa da Escola do Fogo",
        "item.liberthia.school_rune_ice":      "Runa da Escola do Gelo",
        "item.liberthia.school_rune_lightning":"Runa da Escola do Raio",
        "item.liberthia.school_rune_blood":    "Runa da Escola do Sangue",
        "item.liberthia.school_rune_eldritch": "Runa da Escola Eldritch",
        "item.liberthia.school_rune_holy":     "Runa da Escola Sagrada",
        "item.liberthia.school_rune_nature":   "Runa da Escola da Natureza",
    }
    for path, extra in [(EN_PATH, en_extra), (PT_PATH, pt_extra)]:
        d = json.loads(path.read_text(encoding="utf-8"))
        d.update(extra)
        path.write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    gen_tablet()
    write_simple_model("magic_tablet")
    print("OK magic_tablet")

    gen_orb()
    write_simple_model("arcane_orb")
    print("OK arcane_orb")

    for school, color in SCHOOL_COLORS.items():
        gen_school_rune(school, color)
        write_simple_model(f"school_rune_{school}")
        print(f"OK school_rune_{school}")

    update_lang()
    print("OK lang entries (EN + PT)")


if __name__ == "__main__":
    main()
