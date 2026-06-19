#!/usr/bin/env python3
"""r155 Phase 3: Generate per-spell scroll textures + model overrides."""
import json
import os
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
SPELLS_DIR = ROOT / "data" / "liberthia" / "spells"
TEX_DIR = ROOT / "assets" / "liberthia" / "textures" / "item"
MODEL_DIR = ROOT / "assets" / "liberthia" / "models" / "item"

SCHOOL_COLORS = {
    "FIRE":      (255, 102, 51),
    "ICE":       (102, 204, 255),
    "LIGHTNING": (255, 255, 68),
    "BLOOD":     (153, 0,   51),
    "ELDRITCH":  (102, 51,  204),
    "HOLY":      (255, 238, 170),
    "NATURE":    (51,  170, 51),
}

PARCHMENT = (212, 180, 130)


def draw_parchment_base(d):
    for x in range(2, 14):
        for y in range(1, 4):
            d.point((x, y), fill=(140, 100, 50))
        d.point((x, 1), fill=(80, 50, 20))
        d.point((x, 3), fill=(80, 50, 20))
    for x in range(3, 13):
        for y in range(4, 12):
            v = int((math.sin(x * 0.5) + math.cos(y * 0.7)) * 6)
            r, g, b = PARCHMENT
            d.point((x, y), fill=(r + v, g + v, b + v))
    for x in range(2, 14):
        for y in range(12, 15):
            d.point((x, y), fill=(140, 100, 50))
        d.point((x, 12), fill=(80, 50, 20))
        d.point((x, 14), fill=(80, 50, 20))
    for x in [1, 14]:
        d.point((x, 1), fill=(80, 50, 20))
        d.point((x, 2), fill=(120, 80, 40))
        d.point((x, 3), fill=(80, 50, 20))
        d.point((x, 12), fill=(80, 50, 20))
        d.point((x, 13), fill=(120, 80, 40))
        d.point((x, 14), fill=(80, 50, 20))


def draw_seal(d, color):
    cx, cy = 8, 11
    for x in range(cx - 2, cx + 3):
        for y in range(cy - 1, cy + 2):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy <= 4:
                d.point((x, y), fill=color)
    light = tuple(min(255, c + 60) for c in color)
    d.point((cx - 1, cy - 1), fill=light)


def draw_icon_explosion(d, color):
    cx, cy = 8, 7
    d.point((cx, cy), fill=color)
    for r in range(1, 3):
        d.point((cx, cy - r), fill=color)
        d.point((cx, cy + r), fill=color)
        d.point((cx - r, cy), fill=color)
        d.point((cx + r, cy), fill=color)


def draw_icon_destruction(d, color):
    cx, cy = 8, 7
    for i in range(-2, 3):
        d.point((cx + i, cy + i), fill=color)
        d.point((cx - i, cy + i), fill=color)
    d.point((cx, cy), fill=(255, 255, 255))


def draw_icon_dash(d, color):
    cx, cy = 8, 7
    for i in range(-3, 4):
        d.point((cx + i, cy), fill=color)
    d.point((cx + 2, cy - 1), fill=color)
    d.point((cx + 2, cy + 1), fill=color)
    d.point((cx + 1, cy - 2), fill=color)
    d.point((cx + 1, cy + 2), fill=color)


def draw_icon_ray(d, color):
    cx, cy = 8, 7
    for i in range(-3, 4):
        d.point((cx + i, cy), fill=color)
    for i in range(-2, 3):
        d.point((cx + i, cy - 1), fill=color)
        d.point((cx + i, cy + 1), fill=color)


def draw_icon_critical(d, color):
    cx, cy = 8, 7
    for x in range(-2, 3):
        d.point((cx + x, cy - 2 + abs(x)), fill=color)
        d.point((cx + x, cy + 2 - abs(x)), fill=color)
    d.point((cx, cy), fill=(255, 255, 255))


def draw_icon_utility(d, color):
    cx, cy = 8, 7
    for y in range(-2, 3):
        d.point((cx - 2, cy + y), fill=color)
        d.point((cx + 2, cy + y), fill=color)
    for x in range(-1, 2):
        d.point((cx + x, cy + 2), fill=color)


def draw_icon_summon(d, color):
    cx, cy = 8, 7
    rad = 2.5
    for i in range(5):
        ang = -math.pi / 2 + i * 2 * math.pi / 5
        x = int(cx + math.cos(ang) * rad)
        y = int(cy + math.sin(ang) * rad)
        d.point((x, y), fill=color)
    d.point((cx, cy), fill=color)


def draw_icon_aoe(d, color):
    cx, cy = 8, 7
    for ang_step in range(0, 360, 30):
        ang = math.radians(ang_step)
        d.point((int(cx + math.cos(ang) * 3), int(cy + math.sin(ang) * 3)), fill=color)
    d.point((cx, cy), fill=color)
    d.point((cx - 1, cy), fill=color)
    d.point((cx + 1, cy), fill=color)
    d.point((cx, cy - 1), fill=color)
    d.point((cx, cy + 1), fill=color)


def draw_icon_channeled(d, color):
    cx, cy = 8, 7
    pts = [(0, -2), (-1, -1), (0, 0), (-1, 1), (1, 2)]
    for dx, dy in pts:
        d.point((cx + dx, cy + dy), fill=color)


def draw_icon_instant(d, color):
    cx, cy = 8, 6
    for y in range(0, 3):
        d.point((cx, cy + y), fill=color)
    d.point((cx, cy + 4), fill=color)


def draw_icon_projectile(d, color):
    cx, cy = 8, 7
    d.point((cx - 2, cy), fill=color)
    d.point((cx - 1, cy), fill=color)
    d.point((cx, cy), fill=color)
    d.point((cx + 1, cy), fill=color)
    d.point((cx + 2, cy - 1), fill=color)
    d.point((cx + 2, cy + 1), fill=color)
    d.point((cx + 1, cy - 2), fill=color)
    d.point((cx + 1, cy + 2), fill=color)


def draw_icon_mixed(d, color):
    cx, cy = 8, 7
    pts = [(0, -2), (2, -1), (2, 1), (0, 2), (-2, 1), (-2, -1)]
    for dx, dy in pts:
        d.point((cx + dx, cy + dy), fill=color)


CATEGORY_ICONS = {
    "EXPLOSION": draw_icon_explosion,
    "DESTRUCTION": draw_icon_destruction,
    "DASH": draw_icon_dash,
    "RAY": draw_icon_ray,
    "CRITICAL": draw_icon_critical,
    "UTILITY": draw_icon_utility,
    "SUMMON": draw_icon_summon,
    "AOE": draw_icon_aoe,
    "CHANNELED": draw_icon_channeled,
    "INSTANT": draw_icon_instant,
    "PROJECTILE": draw_icon_projectile,
    "MIXED": draw_icon_mixed,
}


def category_from_type(type_str):
    return {
        "PROJECTILE": "PROJECTILE",
        "BEAM": "RAY",
        "AOE_BURST": "AOE",
        "SUMMON": "SUMMON",
        "DASH": "DASH",
        "SELF_BUFF": "UTILITY",
        "EXPLOSION": "EXPLOSION",
        "CHANNELED_BEAM": "CHANNELED",
        "AURA": "AOE",
        "TARGETED": "INSTANT",
        "MIXED": "MIXED",
    }.get((type_str or "").upper(), "PROJECTILE")


def gen_texture(out_path, school, category, primary_color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_parchment_base(d)
    icon_fn = CATEGORY_ICONS.get(category, draw_icon_projectile)
    icon_fn(d, primary_color)
    draw_seal(d, primary_color)
    img.save(out_path)


def main():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    spells = []
    for jf in sorted(SPELLS_DIR.glob("*.json")):
        if "KITCHEN_SINK" in jf.name:
            continue
        with open(jf, encoding="utf-8") as f:
            data = json.load(f)
        spell_id = data["id"]
        school = data.get("school", "ELDRITCH").upper()
        category = data.get("category", "").upper() or category_from_type(data.get("type", "PROJECTILE"))
        spells.append({"id": spell_id, "school": school, "category": category})

    print("Found {} spells".format(len(spells)))

    for s in spells:
        primary = SCHOOL_COLORS.get(s["school"], SCHOOL_COLORS["ELDRITCH"])
        tex_path = TEX_DIR / "factory_spell_scroll_{}.png".format(s["id"].replace("factory_", ""))
        gen_texture(tex_path, s["school"], s["category"], primary)
        model_path = MODEL_DIR / "factory_spell_scroll_{}.json".format(s["id"].replace("factory_", ""))
        model_path.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "liberthia:item/factory_spell_scroll_{}".format(s["id"].replace("factory_", ""))}
        }, indent=2))

    overrides = []
    for i, s in enumerate(spells):
        overrides.append({
            "predicate": {"liberthia:spell_index": float(i + 1)},
            "model": "liberthia:item/factory_spell_scroll_{}".format(s["id"].replace("factory_", ""))
        })
    master = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "liberthia:item/factory_spell_scroll"},
        "overrides": overrides
    }
    (MODEL_DIR / "factory_spell_scroll.json").write_text(json.dumps(master, indent=2))

    out_lines = ["// r155 Phase 3 - auto-generated factory spell index map",
                 "// DO NOT EDIT MANUALLY"]
    for i, s in enumerate(spells):
        out_lines.append('idx.put("{}", {});'.format(s["id"], i + 1))
    (Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod") / "spell_index_map.txt").write_text("\n".join(out_lines))

    print("OK - generated {} textures + models + master JSON".format(len(spells)))


if __name__ == "__main__":
    main()
