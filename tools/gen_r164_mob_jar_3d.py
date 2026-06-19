#!/usr/bin/env python3
"""r164: Mob Jar 3D — gera modelo BlockBench-style com 6 cubos (base, corpo,
gargalo, cork, glow) + textura 16x16 com gradient verde-glass + tampa marrom.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia")

# ─── TEXTURA 16x16 ───
def gen_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # Cork (top 3 rows) — marrom + textura wood
    cork_base = (101, 67, 33, 255)
    cork_dark = (75, 50, 25, 255)
    cork_light = (140, 95, 55, 255)
    for x in range(16):
        for y in range(0, 3):
            c = cork_base
            if y == 0: c = cork_dark
            if (x + y) % 2 == 0 and y > 0: c = cork_light
            if x == 0 or x == 15: c = cork_dark
            px[x, y] = c

    # Glass body (rows 3-13) — verde claro com transparência simulada
    glass_base = (140, 200, 160, 200)  # verde acinzentado semi-transparente
    glass_high = (200, 240, 210, 220)
    glass_low  = (90, 140, 110, 200)
    for x in range(16):
        for y in range(3, 13):
            # Sides darker, center lighter
            cx = abs(x - 7.5) / 7.5  # 0 center, 1 edge
            if cx > 0.85:
                px[x, y] = glass_low
            elif cx < 0.35 and y % 3 == 0:
                px[x, y] = glass_high  # reflection highlight
            else:
                px[x, y] = glass_base
    # Inner glow specks (alguma criatura presa)
    px[6, 7] = (255, 240, 120, 255)
    px[9, 9] = (255, 230, 100, 230)
    px[7, 11] = (200, 230, 255, 180)

    # Bottom rim (rows 13-15) — verde escuro
    rim_dark = (60, 100, 75, 255)
    rim_mid  = (90, 140, 110, 255)
    for x in range(16):
        for y in range(13, 16):
            c = rim_mid if y == 14 else rim_dark
            if x == 0 or x == 15: c = (40, 70, 55, 255)
            px[x, y] = c

    img.save(ROOT / "textures" / "block" / "mob_jar.png")
    # Item texture = same
    img.save(ROOT / "textures" / "item" / "mob_jar.png")
    print("OK texture mob_jar.png 16x16")


# ─── MODELO 3D ───
def gen_block_model():
    """Modelo de jarro com 6 cubos: base ampla → corpo → gargalo → cork."""
    # Texture UVs mapeiam slices da textura 16x16:
    # - cork (y 0..3) → top of cork lid
    # - body (y 3..13) → sides of glass body
    # - rim (y 13..16) → bottom base

    model = {
        "parent": "block/block",
        "ambientocclusion": False,
        "textures": {
            "0": "liberthia:block/mob_jar",
            "particle": "liberthia:block/mob_jar"
        },
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, 0, 0],
                "translation": [0, 2.5, 0],
                "scale": [0.55, 0.55, 0.55]
            },
            "firstperson_righthand": {
                "rotation": [0, 45, 0],
                "translation": [0, 0, 0],
                "scale": [0.6, 0.6, 0.6]
            },
            "gui": {
                "rotation": [30, 45, 0],
                "translation": [0, 0, 0],
                "scale": [0.625, 0.625, 0.625]
            },
            "ground": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 0],
                "scale": [0.4, 0.4, 0.4]
            },
            "fixed": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [0.5, 0.5, 0.5]
            }
        },
        "elements": [
            # ── Base ampla (y 0..2) — ocupa quase todo bloco ──
            {
                "name": "base",
                "from": [2, 0, 2],
                "to": [14, 2, 14],
                "faces": {
                    "down":  {"uv": [0, 13, 16, 16], "texture": "#0", "cullface": "down"},
                    "up":    {"uv": [0, 13, 16, 16], "texture": "#0"},
                    "north": {"uv": [0, 13, 16, 16], "texture": "#0"},
                    "south": {"uv": [0, 13, 16, 16], "texture": "#0"},
                    "west":  {"uv": [0, 13, 16, 16], "texture": "#0"},
                    "east":  {"uv": [0, 13, 16, 16], "texture": "#0"}
                }
            },
            # ── Corpo principal (y 2..10) — corpo do jarro ──
            {
                "name": "body",
                "from": [3, 2, 3],
                "to": [13, 11, 13],
                "faces": {
                    "up":    {"uv": [3, 3, 13, 13], "texture": "#0"},
                    "north": {"uv": [3, 3, 13, 13], "texture": "#0"},
                    "south": {"uv": [3, 3, 13, 13], "texture": "#0"},
                    "west":  {"uv": [3, 3, 13, 13], "texture": "#0"},
                    "east":  {"uv": [3, 3, 13, 13], "texture": "#0"}
                }
            },
            # ── Ombro/curva (y 10..12) — afina pro gargalo ──
            {
                "name": "shoulder",
                "from": [4, 11, 4],
                "to": [12, 12, 12],
                "faces": {
                    "up":    {"uv": [4, 3, 12, 11], "texture": "#0"},
                    "north": {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "south": {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "west":  {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "east":  {"uv": [4, 3, 12, 4], "texture": "#0"}
                }
            },
            # ── Gargalo (y 12..14) ──
            {
                "name": "neck",
                "from": [5, 12, 5],
                "to": [11, 14, 11],
                "faces": {
                    "up":    {"uv": [5, 3, 11, 9], "texture": "#0"},
                    "north": {"uv": [5, 3, 11, 5], "texture": "#0"},
                    "south": {"uv": [5, 3, 11, 5], "texture": "#0"},
                    "west":  {"uv": [5, 3, 11, 5], "texture": "#0"},
                    "east":  {"uv": [5, 3, 11, 5], "texture": "#0"}
                }
            },
            # ── Cork / tampa (y 14..16) — usa região cork da textura ──
            {
                "name": "cork",
                "from": [4, 14, 4],
                "to": [12, 16, 12],
                "faces": {
                    "up":    {"uv": [0, 0, 8, 3], "texture": "#0", "cullface": "up"},
                    "down":  {"uv": [0, 0, 8, 3], "texture": "#0"},
                    "north": {"uv": [0, 0, 8, 3], "texture": "#0"},
                    "south": {"uv": [0, 0, 8, 3], "texture": "#0"},
                    "west":  {"uv": [0, 0, 8, 3], "texture": "#0"},
                    "east":  {"uv": [0, 0, 8, 3], "texture": "#0"}
                }
            },
            # ── Lip / borda do gargalo (y 13..14 mais largo) ──
            {
                "name": "lip",
                "from": [4, 13, 4],
                "to": [12, 14, 12],
                "faces": {
                    "up":    {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "north": {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "south": {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "west":  {"uv": [4, 3, 12, 4], "texture": "#0"},
                    "east":  {"uv": [4, 3, 12, 4], "texture": "#0"}
                }
            }
        ]
    }
    path = ROOT / "models" / "block" / "mob_jar.json"
    path.write_text(json.dumps(model, indent=2), encoding="utf-8")
    print("OK block model mob_jar.json (6 elements)")


# ─── ITEM MODEL — usa o block model 3D ───
def gen_item_model():
    model = {
        "parent": "liberthia:block/mob_jar"
    }
    path = ROOT / "models" / "item" / "mob_jar.json"
    path.write_text(json.dumps(model, indent=2), encoding="utf-8")
    print("OK item model mob_jar.json (parents block)")


def main():
    gen_texture()
    gen_block_model()
    gen_item_model()
    print("\nDONE — Mob Jar agora é 3D com cork marrom + corpo verde-glass.")


if __name__ == "__main__":
    main()
