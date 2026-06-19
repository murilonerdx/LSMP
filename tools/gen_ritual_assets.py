"""Generator de assets do sistema ocultista r32 — chalks, candles, sigilos,
ritual circle, spirit miner, bound crystals, lighter, dagger, chalice.
Gera texturas + modelos + blockstates + loot tables.
"""
import os, json, math, random
from PIL import Image, ImageDraw, ImageFilter

random.seed(13)
BASE = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources"

TEX_ITEM = os.path.join(BASE, "assets/liberthia/textures/item")
TEX_BLOCK = os.path.join(BASE, "assets/liberthia/textures/block")
MODEL_ITEM = os.path.join(BASE, "assets/liberthia/models/item")
MODEL_BLOCK = os.path.join(BASE, "assets/liberthia/models/block")
BLOCKSTATE = os.path.join(BASE, "assets/liberthia/blockstates")
LOOT = os.path.join(BASE, "data/liberthia/loot_tables/blocks")
for d in [TEX_ITEM, TEX_BLOCK, MODEL_ITEM, MODEL_BLOCK, BLOCKSTATE, LOOT]:
    os.makedirs(d, exist_ok=True)


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)


CHALKS = {
    "white":  (255, 255, 255),
    "golden": (255, 215, 0),
    "purple": (138, 43, 226),
    "red":    (204, 0, 51),
    "black":  (40, 40, 40),
}


# ────────────── TEXTURES ──────────────

def gen_chalk_item(name, color):
    """Pequeno bastão de giz com cor."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # diagonal stick (chalk)
    for off in range(7):
        d.line([(3 + off, 11), (12 + off - 5, 3 + 0)], fill=(*color, 255))
    d.line([(3, 12), (13, 2)], fill=(max(color[0] - 30, 0), max(color[1] - 30, 0), max(color[2] - 30, 0), 255))
    # tip highlight
    d.ellipse((11, 1, 14, 4), fill=(255, 255, 255, 200))
    img.save(os.path.join(TEX_ITEM, f"chalk_{name}.png"))


def gen_chalk_mark_block(name, color):
    """Textura do chalk mark (pentagrama no chão)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # background transparent — só desenha simbolo central
    cx, cy = 8, 8
    # pentagrama com transparência
    pts = []
    for i in range(5):
        a = math.radians(-90 + 72 * i)
        pts.append((cx + math.cos(a) * 6, cy + math.sin(a) * 6))
    seq = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    for i in range(5):
        d.line([seq[i], seq[i + 1]], fill=(*color, 220))
    d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), outline=(*color, 180))
    img.save(os.path.join(TEX_BLOCK, f"chalk_mark_{name}.png"))


def gen_candle_block(name, color):
    """Vela cilíndrica colorida."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # wax body
    d.rectangle((6, 6, 10, 14), fill=(*color, 255))
    # highlight
    d.rectangle((6, 6, 7, 14), fill=(min(color[0] + 50, 255), min(color[1] + 50, 255), min(color[2] + 50, 255), 255))
    # wick
    d.line([(8, 3), (8, 6)], fill=(50, 30, 10, 255))
    # plate
    d.rectangle((4, 14, 12, 15), fill=(80, 80, 80, 255))
    img.save(os.path.join(TEX_BLOCK, f"candle_occult_{name}.png"))


def gen_candle_lit(name, color):
    """Vela acesa com chama."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((6, 6, 10, 14), fill=(*color, 255))
    d.rectangle((6, 6, 7, 14), fill=(min(color[0] + 50, 255), min(color[1] + 50, 255), min(color[2] + 50, 255), 255))
    d.line([(8, 3), (8, 6)], fill=(50, 30, 10, 255))
    d.rectangle((4, 14, 12, 15), fill=(80, 80, 80, 255))
    # flame
    d.polygon([(8, 0), (7, 2), (7, 4), (8, 5), (9, 4), (9, 2)], fill=(255, 220, 50, 255))
    d.polygon([(8, 1), (8, 4)], fill=(255, 255, 200, 255))
    img.save(os.path.join(TEX_BLOCK, f"candle_occult_{name}_lit.png"))


def gen_sigil(name, color, symbol_func=None):
    """Pergaminho com sigilo no centro."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # parchment
    d.polygon([(2, 3), (14, 3), (14, 13), (2, 13)], fill=(220, 200, 160, 255))
    d.line([(2, 3), (14, 3)], fill=(180, 160, 120, 255))
    d.line([(2, 13), (14, 13)], fill=(180, 160, 120, 255))
    # symbol
    if symbol_func:
        symbol_func(d, color)
    img.save(os.path.join(TEX_ITEM, f"sigil_{name}.png"))


def sym_circle_dot(d, color):
    d.ellipse((5, 6, 11, 12), outline=(*color, 255))
    d.ellipse((7, 8, 9, 10), fill=(*color, 255))


def sym_pentagram(d, color):
    cx, cy = 8, 9
    pts = []
    for i in range(5):
        a = math.radians(-90 + 72 * i)
        pts.append((cx + math.cos(a) * 3.5, cy + math.sin(a) * 3.5))
    seq = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    for i in range(5):
        d.line([seq[i], seq[i + 1]], fill=(*color, 255))


def sym_hexagram(d, color):
    cx, cy = 8, 9
    pts = []
    for i in range(6):
        a = math.radians(60 * i)
        pts.append((cx + math.cos(a) * 3.5, cy + math.sin(a) * 3.5))
    d.polygon([pts[0], pts[2], pts[4]], outline=(*color, 255))
    d.polygon([pts[1], pts[3], pts[5]], outline=(*color, 255))


def sym_cross(d, color):
    d.line([(8, 5), (8, 13)], fill=(*color, 255), width=1)
    d.line([(5, 9), (11, 9)], fill=(*color, 255), width=1)


def sym_eye(d, color):
    d.ellipse((4, 7, 12, 11), fill=(*color, 255))
    d.ellipse((6, 8, 10, 10), fill=(0, 0, 0, 255))


def sym_skull(d, color):
    d.ellipse((5, 5, 11, 11), fill=(*color, 255))
    d.point((7, 8), fill=(0, 0, 0, 255))
    d.point((9, 8), fill=(0, 0, 0, 255))
    d.line([(7, 10), (9, 10)], fill=(0, 0, 0, 255))


def gen_lighter():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # body (chrome zippo style)
    d.rectangle((5, 6, 11, 14), fill=(180, 180, 200, 255))
    d.rectangle((5, 6, 5, 14), fill=(120, 120, 140, 255))
    # cap
    d.rectangle((5, 4, 11, 6), fill=(150, 150, 170, 255))
    # flame
    d.polygon([(8, 0), (7, 2), (7, 4), (8, 5), (9, 4), (9, 2)], fill=(255, 180, 50, 255))
    d.polygon([(8, 1), (8, 4)], fill=(255, 255, 200, 255))
    # ridges
    for y in [8, 10, 12]:
        d.line([(5, y), (11, y)], fill=(140, 140, 160, 255))
    img.save(os.path.join(TEX_ITEM, "lighter.png"))


def gen_dagger():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # pommel + grip
    d.ellipse((6, 13, 10, 16), fill=(200, 50, 50, 255))
    d.rectangle((7, 10, 9, 13), fill=(80, 30, 30, 255))
    # crossguard
    d.line([(4, 10), (12, 10)], fill=(160, 160, 200, 255), width=2)
    # blade
    d.polygon([(7, 10), (9, 10), (8, 1)], fill=(220, 220, 240, 255))
    d.line([(8, 1), (8, 10)], fill=(140, 140, 160, 255))
    # blood drop
    d.ellipse((7, 2, 9, 4), fill=(180, 30, 40, 255))
    img.save(os.path.join(TEX_ITEM, "ritual_dagger.png"))


def gen_chalice():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # foot
    d.rectangle((5, 14, 11, 15), fill=(255, 215, 0, 255))
    # stem
    d.rectangle((7, 9, 9, 14), fill=(255, 215, 0, 255))
    # bowl
    d.polygon([(4, 4), (12, 4), (11, 9), (5, 9)], fill=(255, 215, 0, 255))
    d.polygon([(5, 4), (11, 4), (10, 8), (6, 8)], fill=(180, 30, 40, 255))  # wine
    # gem
    d.ellipse((7, 11, 9, 13), fill=(100, 200, 250, 255))
    img.save(os.path.join(TEX_ITEM, "ritual_chalice.png"))


def gen_ritual_circle_top():
    """Top texture do bloco ritual circle — pentagram dourado em obsidiana."""
    img = Image.new("RGBA", (16, 16), (15, 5, 25, 255))
    d = ImageDraw.Draw(img)
    # noise base
    for _ in range(80):
        x, y = random.randint(0, 15), random.randint(0, 15)
        c = random.randint(10, 30)
        d.point((x, y), fill=(c, c // 2, c, 255))
    # pentagram dourado
    cx, cy = 8, 8
    pts = []
    for i in range(5):
        a = math.radians(-90 + 72 * i)
        pts.append((cx + math.cos(a) * 6.5, cy + math.sin(a) * 6.5))
    seq = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    for i in range(5):
        d.line([seq[i], seq[i + 1]], fill=(255, 220, 100, 255))
    d.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), outline=(255, 220, 100, 220))
    img.save(os.path.join(TEX_BLOCK, "ritual_circle_top.png"))


def gen_ritual_circle_side():
    img = Image.new("RGBA", (16, 16), (15, 5, 25, 255))
    d = ImageDraw.Draw(img)
    for _ in range(80):
        x, y = random.randint(0, 15), random.randint(0, 15)
        c = random.randint(10, 40)
        d.point((x, y), fill=(c, c // 2, c, 255))
    # rune central
    d.rectangle((6, 4, 10, 12), outline=(255, 220, 100, 255))
    d.line([(8, 4), (8, 12)], fill=(255, 220, 100, 255))
    img.save(os.path.join(TEX_BLOCK, "ritual_circle_side.png"))


def gen_spirit_miner():
    img = Image.new("RGBA", (16, 16), (60, 60, 80, 255))
    d = ImageDraw.Draw(img)
    # rivets
    for cx, cy in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        d.ellipse((cx - 1, cy - 1, cx + 1, cy + 1), fill=(120, 120, 150, 255))
    # spiritual eye in center
    d.ellipse((5, 5, 11, 11), fill=(20, 0, 30, 255))
    d.ellipse((6, 6, 10, 10), fill=(150, 100, 220, 255))
    d.ellipse((7, 7, 9, 9), fill=(0, 0, 0, 255))
    # pickaxe glyph
    d.line([(2, 14), (5, 11)], fill=(180, 150, 80, 255))
    d.line([(11, 11), (14, 14)], fill=(180, 150, 80, 255))
    img.save(os.path.join(TEX_BLOCK, "spirit_miner.png"))


def gen_bound_crystal(name, color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # outer crystal
    d.polygon([(8, 1), (13, 6), (13, 11), (8, 15), (3, 11), (3, 6)], fill=(*color, 220))
    # inner highlight
    d.polygon([(8, 3), (11, 6), (11, 10), (8, 13), (5, 10), (5, 6)], fill=(min(color[0]+60, 255), min(color[1]+60, 255), min(color[2]+60, 255), 255))
    # core
    d.ellipse((7, 7, 9, 9), fill=(255, 255, 255, 255))
    img.save(os.path.join(TEX_ITEM, f"bound_{name}_crystal.png"))


# ────────────── MODELS / BLOCKSTATES / LOOT ──────────────

def cube_all_model(name):
    return {"parent": "minecraft:block/cube_all", "textures": {"all": f"liberthia:block/{name}"}}


def chalk_mark_blockstate_variants(name):
    """5 variantes pra cada shape do chalk mark (DOT/LINE/CIRCLE/SIGIL = same texture)."""
    variants = {}
    for shape in ["dot", "line", "circle", "sigil"]:
        variants[f"shape={shape}"] = {"model": f"liberthia:block/chalk_mark_{name}"}
    return {"variants": variants}


def chalk_mark_model(name):
    """Modelo flat — quadrado fino no chão."""
    return {
        "parent": "minecraft:block/thin_block",
        "textures": {
            "texture": f"liberthia:block/chalk_mark_{name}",
            "particle": f"liberthia:block/chalk_mark_{name}"
        },
        "elements": [
            {
                "from": [0, 0, 0], "to": [16, 1, 16],
                "faces": {
                    "down":  {"uv": [0, 0, 16, 16], "texture": "#texture"},
                    "up":    {"uv": [0, 0, 16, 16], "texture": "#texture"}
                }
            }
        ]
    }


def candle_unlit_model(name):
    """Modelo da vela apagada."""
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "all": f"liberthia:block/candle_occult_{name}",
            "particle": f"liberthia:block/candle_occult_{name}"
        },
        "elements": [
            {"from": [6, 0, 6], "to": [10, 8, 10],
             "faces": {
                 "down":  {"uv": [6, 6, 10, 10], "texture": "#all"},
                 "up":    {"uv": [6, 6, 10, 10], "texture": "#all"},
                 "north": {"uv": [6, 0, 10, 8], "texture": "#all"},
                 "south": {"uv": [6, 0, 10, 8], "texture": "#all"},
                 "west":  {"uv": [6, 0, 10, 8], "texture": "#all"},
                 "east":  {"uv": [6, 0, 10, 8], "texture": "#all"}
             }}
        ]
    }


def candle_lit_model(name):
    """Modelo da vela acesa — mesma base + flame."""
    return candle_unlit_model(name)  # mesma textura (já tem flame na _lit.png)


def candle_blockstate(name):
    return {
        "variants": {
            "lit=false": {"model": f"liberthia:block/candle_occult_{name}"},
            "lit=true":  {"model": f"liberthia:block/candle_occult_{name}_lit"}
        }
    }


def ritual_circle_model():
    return {
        "parent": "minecraft:block/cube",
        "textures": {
            "down": "liberthia:block/ritual_circle_top",
            "up": "liberthia:block/ritual_circle_top",
            "north": "liberthia:block/ritual_circle_side",
            "south": "liberthia:block/ritual_circle_side",
            "west": "liberthia:block/ritual_circle_side",
            "east": "liberthia:block/ritual_circle_side",
            "particle": "liberthia:block/ritual_circle_top"
        }
    }


def loot_drop_self(name):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"liberthia:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]
        }]
    }


def loot_empty():
    return {"type": "minecraft:block", "pools": []}


def item_generated_model(name):
    return {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{name}"}
    }


# ────────────── GENERATE ──────────────

def main():
    # CHALKS (items + mark blocks)
    for name, color in CHALKS.items():
        gen_chalk_item(name, color)
        gen_chalk_mark_block(name, color)
        # models
        write_json(os.path.join(MODEL_ITEM, f"chalk_{name}.json"), item_generated_model(f"chalk_{name}"))
        write_json(os.path.join(MODEL_BLOCK, f"chalk_mark_{name}.json"), chalk_mark_model(name))
        write_json(os.path.join(BLOCKSTATE, f"chalk_mark_{name}.json"), chalk_mark_blockstate_variants(name))
        # loot: chalk mark drops nothing
        write_json(os.path.join(LOOT, f"chalk_mark_{name}.json"), loot_empty())

    # CANDLES (items + blocks)
    for name, color in CHALKS.items():
        gen_candle_block(name, color)
        gen_candle_lit(name, color)
        # models
        write_json(os.path.join(MODEL_BLOCK, f"candle_occult_{name}.json"), candle_unlit_model(name))
        write_json(os.path.join(MODEL_BLOCK, f"candle_occult_{name}_lit.json"), candle_lit_model(name + "_lit") if False else
                   {"parent": "minecraft:block/block",
                    "textures": {"all": f"liberthia:block/candle_occult_{name}_lit",
                                 "particle": f"liberthia:block/candle_occult_{name}_lit"},
                    "elements": [{"from": [6, 0, 6], "to": [10, 8, 10],
                                  "faces": {f: {"uv": [6, 0 if f in ("up","down") else 0, 10, 16 if f not in ("up","down") else 10], "texture": "#all"}
                                            for f in ("down", "up", "north", "south", "west", "east")}}]})
        write_json(os.path.join(BLOCKSTATE, f"candle_occult_{name}.json"), candle_blockstate(name))
        write_json(os.path.join(MODEL_ITEM, f"candle_occult_{name}.json"),
                   {"parent": f"liberthia:block/candle_occult_{name}"})
        write_json(os.path.join(LOOT, f"candle_occult_{name}.json"), loot_drop_self(f"candle_occult_{name}"))

    # SIGILS (10 com símbolos distintos)
    SIGILS = [
        ("foliot", (100, 80, 130), sym_circle_dot),
        ("djinni", (80, 180, 230), sym_hexagram),
        ("afrit", (220, 50, 30), sym_pentagram),
        ("bael", (40, 0, 60), sym_pentagram),
        ("lucifer", (255, 230, 130), sym_pentagram),
        ("sandalphon", (250, 220, 130), sym_hexagram),
        ("metatron", (255, 240, 180), sym_hexagram),
        ("necro", (60, 30, 30), sym_skull),
        ("banishing", (240, 240, 240), sym_cross),
        ("dimensional", (160, 100, 220), sym_eye),
    ]
    for name, color, sym in SIGILS:
        gen_sigil(name, color, sym)
        write_json(os.path.join(MODEL_ITEM, f"sigil_{name}.json"), item_generated_model(f"sigil_{name}"))

    # Lighter, Dagger, Chalice
    gen_lighter()
    write_json(os.path.join(MODEL_ITEM, "lighter.json"), item_generated_model("lighter"))
    gen_dagger()
    write_json(os.path.join(MODEL_ITEM, "ritual_dagger.json"), item_generated_model("ritual_dagger"))
    gen_chalice()
    write_json(os.path.join(MODEL_ITEM, "ritual_chalice.json"), item_generated_model("ritual_chalice"))

    # Bound crystals
    for name, color in [("foliot", (140, 100, 200)), ("djinni", (100, 200, 240)), ("afrit", (240, 100, 60))]:
        gen_bound_crystal(name, color)
        write_json(os.path.join(MODEL_ITEM, f"bound_{name}_crystal.json"),
                   item_generated_model(f"bound_{name}_crystal"))

    # Ritual Circle
    gen_ritual_circle_top()
    gen_ritual_circle_side()
    write_json(os.path.join(MODEL_BLOCK, "ritual_circle.json"), ritual_circle_model())
    write_json(os.path.join(BLOCKSTATE, "ritual_circle.json"),
               {"variants": {"": {"model": "liberthia:block/ritual_circle"}}})
    write_json(os.path.join(MODEL_ITEM, "ritual_circle.json"),
               {"parent": "liberthia:block/ritual_circle"})
    write_json(os.path.join(LOOT, "ritual_circle.json"), loot_drop_self("ritual_circle"))

    # Spirit Miner
    gen_spirit_miner()
    write_json(os.path.join(MODEL_BLOCK, "spirit_miner.json"), cube_all_model("spirit_miner"))
    write_json(os.path.join(BLOCKSTATE, "spirit_miner.json"),
               {"variants": {"": {"model": "liberthia:block/spirit_miner"}}})
    write_json(os.path.join(MODEL_ITEM, "spirit_miner.json"),
               {"parent": "liberthia:block/spirit_miner"})
    write_json(os.path.join(LOOT, "spirit_miner.json"), loot_drop_self("spirit_miner"))

    print("ALL occult assets generated.")


if __name__ == "__main__":
    main()
