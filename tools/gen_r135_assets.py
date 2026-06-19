"""r135: gera assets pros AN-features novos.

- entity/drygmy.png: textura 64x64 (axolotl UV) recolorida verde
- item/mob_jar.png: jarra de vidro com tampa
- item/spell_mirror.png: espelho oval com runas
- block/agronomic_sourcelink.png: bloco moss-verde com runa
- item/drygmy_egg.png: spawn egg (vanilla template)
"""
from PIL import Image, ImageDraw
import os, math, json

ROOT = os.path.dirname(__file__)
ENT = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/entity"))
ITM = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/item"))
BLK = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/block"))
MODEL_ITM = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/models/item"))
MODEL_BLK = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/models/block"))
BLOCKSTATES = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/blockstates"))


def make_drygmy_texture():
    """Pinta TODO o 64x64 verde com gradient (axolotl UV)."""
    img = Image.new("RGBA", (64, 64), (130, 200, 100, 255))
    d = ImageDraw.Draw(img)
    # Noise gradient
    import random
    random.seed(42)
    for y in range(64):
        for x in range(64):
            r = random.randint(-20, 20)
            base = img.getpixel((x, y))
            new = (max(0, min(255, base[0] + r)),
                   max(0, min(255, base[1] + r + 10)),
                   max(0, min(255, base[2] + r)),
                   255)
            img.putpixel((x, y), new)
    # Add eyes accent (small purple dots — drygmy é gentil)
    d = ImageDraw.Draw(img)
    d.point((10, 8), fill=(150, 80, 200, 255))
    d.point((14, 8), fill=(150, 80, 200, 255))
    img.save(os.path.join(ENT, "drygmy.png"))


def make_mob_jar_icon():
    """Mob jar 16×16 — jarra de vidro com tampa marrom."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Body of jar (glass)
    d.rectangle([4, 5, 11, 13], fill=(200, 230, 240, 180))
    d.line([(4, 5), (4, 13)], fill=(150, 180, 200, 255))
    d.line([(11, 5), (11, 13)], fill=(150, 180, 200, 255))
    # Lid (brown leather)
    d.rectangle([3, 2, 12, 5], fill=(120, 70, 30, 255))
    d.rectangle([4, 3, 11, 4], fill=(180, 110, 50, 255))
    # Cork lump on top
    d.point((7, 1), fill=(80, 50, 20, 255))
    d.point((8, 1), fill=(80, 50, 20, 255))
    # Glass highlights
    d.point((5, 6), fill=(255, 255, 255, 255))
    d.point((6, 7), fill=(255, 255, 255, 180))
    # Bottom rim
    d.rectangle([3, 13, 12, 14], fill=(140, 170, 190, 255))
    img.save(os.path.join(ITM, "mob_jar.png"))


def make_spell_mirror_icon():
    """Spell Mirror 16×16 — espelho oval com runas roxas."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Frame (golden)
    d.ellipse([2, 1, 13, 14], fill=(180, 140, 60, 255))
    d.ellipse([3, 2, 12, 13], outline=(220, 180, 100, 255))
    # Mirror surface (reflective purple/silver)
    d.ellipse([4, 3, 11, 12], fill=(180, 180, 220, 255))
    # Reflection highlights
    d.line([(5, 5), (7, 7)], fill=(255, 255, 255, 200))
    d.line([(6, 4), (8, 6)], fill=(255, 255, 255, 200))
    # Magic rune in center (purple)
    d.point((7, 8), fill=(150, 80, 200, 255))
    d.point((8, 8), fill=(180, 100, 230, 255))
    d.line([(7, 7), (8, 9)], fill=(180, 100, 230, 255))
    # Handle/grip below
    d.rectangle([7, 14, 8, 15], fill=(140, 100, 40, 255))
    img.save(os.path.join(ITM, "spell_mirror.png"))


def make_agronomic_sourcelink_texture():
    """Bloco 16×16 — moss verde com runa gold."""
    img = Image.new("RGBA", (16, 16), (40, 80, 30, 255))
    d = ImageDraw.Draw(img)
    # Moss noise base
    import random
    random.seed(7)
    for _ in range(60):
        x, y = random.randint(0, 15), random.randint(0, 15)
        c = random.choice([(60, 100, 40), (30, 70, 20), (80, 120, 50)])
        d.point((x, y), fill=c + (255,))
    # Center rune (green-gold)
    d.ellipse([5, 5, 10, 10], outline=(220, 200, 50, 255))
    d.point((7, 7), fill=(255, 240, 80, 255))
    d.point((8, 8), fill=(255, 240, 80, 255))
    # Sprout symbol
    d.line([(7, 5), (8, 4)], fill=(180, 230, 100, 255))
    d.line([(8, 5), (9, 4)], fill=(180, 230, 100, 255))
    img.save(os.path.join(BLK, "agronomic_sourcelink.png"))


def make_drygmy_egg_icon():
    """Spawn egg simples — verde claro com pintas."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Egg shape
    d.ellipse([4, 2, 11, 14], fill=(136, 204, 68, 255))
    d.ellipse([5, 3, 10, 13], outline=(100, 160, 50, 255))
    # Spots
    d.point((6, 6), fill=(200, 230, 100, 255))
    d.point((9, 8), fill=(200, 230, 100, 255))
    d.point((7, 10), fill=(200, 230, 100, 255))
    img.save(os.path.join(ITM, "drygmy_egg.png"))


# Model JSONs
def write_model_item(name, parent="minecraft:item/generated", texture_key=None):
    j = {"parent": parent}
    if texture_key:
        j["textures"] = {"layer0": texture_key}
    with open(os.path.join(MODEL_ITM, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


def write_model_block(name, texture_key):
    j = {"parent": "minecraft:block/cube_all", "textures": {"all": texture_key}}
    with open(os.path.join(MODEL_BLK, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


def write_blockstate(name):
    j = {"variants": {"": {"model": f"liberthia:block/{name}"}}}
    with open(os.path.join(BLOCKSTATES, f"{name}.json"), "w") as f:
        json.dump(j, f, indent=2)


# Generate
make_drygmy_texture()
make_mob_jar_icon()
make_spell_mirror_icon()
make_agronomic_sourcelink_texture()
make_drygmy_egg_icon()

# Models
write_model_item("mob_jar", texture_key="liberthia:item/mob_jar")
write_model_item("spell_mirror", texture_key="liberthia:item/spell_mirror")
write_model_item("drygmy_egg", parent="minecraft:item/template_spawn_egg")
write_model_item("agronomic_sourcelink", parent="liberthia:block/agronomic_sourcelink")
write_model_block("agronomic_sourcelink", "liberthia:block/agronomic_sourcelink")
write_blockstate("agronomic_sourcelink")

print("[OK] r135 assets generated:")
print("  - entity/drygmy.png (64x64)")
print("  - item/mob_jar.png, spell_mirror.png, drygmy_egg.png")
print("  - block/agronomic_sourcelink.png + model + blockstate")
