"""r117: Spirit Robes armor + Spirit Conduit + Source Transmuter — textures + JSON."""
from PIL import Image, ImageDraw
import os, json, math

ROOT = os.path.dirname(__file__)
TEX_ITEM = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                          "assets", "liberthia", "textures", "item"))
TEX_BLOCK = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                           "assets", "liberthia", "textures", "block"))
TEX_ARMOR = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                           "assets", "liberthia", "textures", "models", "armor"))
MODEL_BLOCK = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                             "assets", "liberthia", "models", "block"))
MODEL_ITEM = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                            "assets", "liberthia", "models", "item"))
BLOCKSTATES = os.path.normpath(os.path.join(ROOT, "..", "src", "main", "resources",
                                             "assets", "liberthia", "blockstates"))
for d in [TEX_ITEM, TEX_BLOCK, TEX_ARMOR, MODEL_BLOCK, MODEL_ITEM, BLOCKSTATES]:
    os.makedirs(d, exist_ok=True)


# ─── ARMOR ITEM ICONS (16x16) ─────────────────────────────────────
def alpha_circle(d, img, cx, cy, radius, color, alpha=255, falloff=1.0):
    r = max(1, int(radius))
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if not (0 <= x < 16 and 0 <= y < 16):
                continue
            dx = x - cx; dy = y - cy
            dist = math.sqrt(dx*dx + dy*dy)
            if dist > radius:
                continue
            f = math.pow(1.0 - (dist / radius), falloff)
            a = int(alpha * f)
            if a <= 0: continue
            ex = img.getpixel((x, y))
            img.putpixel((x, y), (
                min(255, ex[0] + int(color[0] * a / 255)),
                min(255, ex[1] + int(color[1] * a / 255)),
                min(255, ex[2] + int(color[2] * a / 255)),
                min(255, ex[3] + a)))


def make_robes_helm():
    """Hood roxo translúcido + glow no topo."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Hood shape
    d.polygon([(4, 3), (7, 1), (9, 1), (12, 3), (13, 8), (12, 14), (4, 14), (3, 8)],
              fill=(70, 30, 120, 230), outline=(40, 15, 70, 255))
    # Face cutout (dark inside)
    d.rectangle([6, 8, 10, 13], fill=(15, 5, 25, 250))
    # Glow ornament forehead
    alpha_circle(d, img, 8, 5, 2.5, (180, 100, 240), 220, 0.6)
    return img


def make_robes_chest():
    """Chest roxo com gem central + ombros."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Body
    d.polygon([(4, 2), (12, 2), (13, 4), (13, 14), (3, 14), (3, 4)],
              fill=(70, 30, 120, 240), outline=(40, 15, 70, 255))
    # Shoulder pauldrons (lighter)
    d.rectangle([3, 2, 5, 5], fill=(110, 60, 170, 255))
    d.rectangle([11, 2, 13, 5], fill=(110, 60, 170, 255))
    # Center gem
    alpha_circle(d, img, 8, 8, 2.5, (200, 150, 255), 255, 0.5)
    d.point((8, 7), fill=(255, 255, 255, 255))
    # Trim
    d.line([(4, 13), (12, 13)], fill=(180, 130, 220, 255))
    return img


def make_robes_legs():
    """Pernas longas de tecido com glow nas barras."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 2 legs split
    d.rectangle([3, 1, 7, 14], fill=(60, 25, 100, 240), outline=(35, 12, 60, 255))
    d.rectangle([9, 1, 13, 14], fill=(60, 25, 100, 240), outline=(35, 12, 60, 255))
    # Side trim glow
    d.line([(3, 7), (3, 13)], fill=(180, 120, 220, 200))
    d.line([(13, 7), (13, 13)], fill=(180, 120, 220, 200))
    return img


def make_robes_boots():
    """Botas baixas com sole iluminado."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 2 boots
    d.rectangle([2, 6, 7, 13], fill=(50, 20, 90, 240), outline=(30, 10, 50, 255))
    d.rectangle([9, 6, 14, 13], fill=(50, 20, 90, 240), outline=(30, 10, 50, 255))
    # Glowing sole
    d.line([(2, 13), (7, 13)], fill=(180, 100, 220, 255))
    d.line([(9, 13), (14, 13)], fill=(180, 100, 220, 255))
    d.point((4, 13), fill=(255, 200, 255, 255))
    d.point((11, 13), fill=(255, 200, 255, 255))
    return img


# ─── ARMOR LAYERS (64x32) for player model ─────────────────────────
def make_armor_layer_1():
    """Layer 1: chest + helm + boots — overlay on player model.
    Format: 64x32 (vanilla armor texture format)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Helm region (left top)
    d.rectangle([8, 0, 24, 8], fill=(70, 30, 120, 220))
    d.rectangle([0, 8, 32, 16], fill=(70, 30, 120, 220))
    # Chest region (middle)
    d.rectangle([16, 16, 40, 32], fill=(70, 30, 120, 220))
    d.rectangle([0, 20, 16, 32], fill=(70, 30, 120, 220))
    # Boots regions (right) — small rectangles
    d.rectangle([40, 16, 56, 32], fill=(50, 20, 90, 220))
    # Trim accent on chest
    d.line([(16, 16), (40, 16)], fill=(180, 130, 220, 255))
    return img


def make_armor_layer_2():
    """Layer 2: leggings — 64x32."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Belt region
    d.rectangle([16, 16, 40, 20], fill=(50, 20, 90, 230))
    # Legs region
    d.rectangle([0, 20, 16, 32], fill=(60, 25, 100, 220))
    d.rectangle([16, 20, 32, 32], fill=(60, 25, 100, 220))
    return img


# ─── BLOCK TEXTURES (16x16) ───────────────────────────────────────
def make_spirit_conduit():
    """Pillar texture: tall purple stone with glowing seams."""
    img = Image.new("RGBA", (16, 16), (40, 20, 60, 255))
    d = ImageDraw.Draw(img)
    # Carved seams (vertical lines)
    for x in [4, 8, 12]:
        d.line([(x, 0), (x, 15)], fill=(20, 5, 30, 255))
    # Glowing runes
    for y in [3, 8, 13]:
        d.point((4, y), fill=(180, 100, 220, 255))
        d.point((8, y), fill=(220, 150, 255, 255))
        d.point((12, y), fill=(180, 100, 220, 255))
    # Mid line glow
    d.line([(2, 7), (13, 7)], fill=(120, 60, 180, 255))
    return img


def make_source_transmuter():
    """Cube texture: dark metal with glowing center crystal."""
    img = Image.new("RGBA", (16, 16), (60, 60, 70, 255))
    d = ImageDraw.Draw(img)
    # Outer frame
    d.rectangle([0, 0, 15, 15], outline=(30, 30, 40, 255))
    d.rectangle([1, 1, 14, 14], outline=(100, 100, 120, 255))
    # Inner crystal panel
    d.rectangle([4, 4, 11, 11], fill=(80, 40, 130, 255))
    alpha_circle(d, img, 7, 7, 3, (200, 150, 255), 230, 0.5)
    d.point((7, 7), fill=(255, 230, 255, 255))
    return img


# ─── Write everything ─────────────────────────────────────────────
TASKS = [
    # Armor item icons
    ("item", "spirit_robes_helm", make_robes_helm),
    ("item", "spirit_robes_chest", make_robes_chest),
    ("item", "spirit_robes_legs", make_robes_legs),
    ("item", "spirit_robes_boots", make_robes_boots),
    # Block textures
    ("block", "spirit_conduit", make_spirit_conduit),
    ("block", "source_transmuter", make_source_transmuter),
]

for kind, name, fn in TASKS:
    img = fn()
    if kind == "item":
        img.save(os.path.join(TEX_ITEM, f"{name}.png"))
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": f"liberthia:item/{name}"}}
        with open(os.path.join(MODEL_ITEM, f"{name}.json"), "w") as f:
            json.dump(model, f)
    elif kind == "block":
        img.save(os.path.join(TEX_BLOCK, f"{name}.png"))
        # Block model (cube_all)
        bmodel = {"parent": "minecraft:block/cube_all",
                  "textures": {"all": f"liberthia:block/{name}"}}
        with open(os.path.join(MODEL_BLOCK, f"{name}.json"), "w") as f:
            json.dump(bmodel, f)
        # Blockstate
        bstate = {"variants": {"": {"model": f"liberthia:block/{name}"}}}
        with open(os.path.join(BLOCKSTATES, f"{name}.json"), "w") as f:
            json.dump(bstate, f)
        # Item model parent the block model
        imodel = {"parent": f"liberthia:block/{name}"}
        with open(os.path.join(MODEL_ITEM, f"{name}.json"), "w") as f:
            json.dump(imodel, f)

# Armor layers (special location)
make_armor_layer_1().save(os.path.join(TEX_ARMOR, "spirit_robes_layer_1.png"))
make_armor_layer_2().save(os.path.join(TEX_ARMOR, "spirit_robes_layer_2.png"))

print(f"r117 assets gerados: 6 item/block + 2 armor layers")
