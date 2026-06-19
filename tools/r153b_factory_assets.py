"""r153b: Factory Spell Scroll texture + model + verify all factory spell JSONs.

Cria:
- factory_spell_scroll.png — pergaminho neutro (será tintado por ItemColor por school)
- factory_spell_scroll.json — item model
- Verifica que todos os JSONs de spell em data/liberthia/spells/ são válidos
"""
import json
import random
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(".")
TEX = ROOT / "src/main/resources/assets/liberthia/textures/item"
MOD = ROOT / "src/main/resources/assets/liberthia/models/item"
SPELLS = ROOT / "src/main/resources/data/liberthia/spells"
LANG_EN = ROOT / "src/main/resources/assets/liberthia/lang/en_us.json"
LANG_PT = ROOT / "src/main/resources/assets/liberthia/lang/pt_br.json"

TEX.mkdir(parents=True, exist_ok=True)
MOD.mkdir(parents=True, exist_ok=True)

# ════════════════════════════════════════════════════════════════════════
# 1. Factory Spell Scroll texture — pergaminho com glyph
# ════════════════════════════════════════════════════════════════════════

def make_scroll_texture():
    """16x16 scroll de pergaminho. Branco — será tintado por ItemColor."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Scroll body (white parchment)
    paper = (240, 230, 200, 255)
    paper_dark = (180, 165, 130, 255)
    paper_shadow = (140, 125, 95, 255)

    # Rolo superior + inferior
    d.rectangle([2, 2, 13, 4], fill=paper_dark)
    d.rectangle([2, 11, 13, 13], fill=paper_dark)

    # Corpo do pergaminho (área central onde aparece a tint)
    d.rectangle([2, 5, 13, 10], fill=paper)

    # Sombras (não pinta com ItemColor — fica fixo)
    for y in range(5, 11):
        img.putpixel((2, y), paper_shadow)  # left edge
        img.putpixel((13, y), paper_shadow)  # right edge

    # Glyph mágico no centro (estrela 4-pontas escura — será visível mesmo tintado)
    glyph = (60, 30, 80, 255)
    glyph_bright = (255, 255, 255, 255)
    # Estrela
    img.putpixel((7, 6), glyph_bright)
    img.putpixel((8, 6), glyph_bright)
    img.putpixel((6, 7), glyph)
    img.putpixel((7, 7), glyph_bright)
    img.putpixel((8, 7), glyph_bright)
    img.putpixel((9, 7), glyph)
    img.putpixel((7, 8), glyph)
    img.putpixel((8, 8), glyph)

    # Sparkle dots
    img.putpixel((5, 5), glyph_bright)
    img.putpixel((10, 5), glyph_bright)
    img.putpixel((5, 9), glyph_bright)
    img.putpixel((10, 9), glyph_bright)

    # Ribbon ties (top/bottom)
    ribbon = (180, 30, 30, 255)
    img.putpixel((1, 3), ribbon)
    img.putpixel((14, 3), ribbon)
    img.putpixel((1, 12), ribbon)
    img.putpixel((14, 12), ribbon)

    return img

print("=== Factory Spell Scroll ===")
img = make_scroll_texture()
img.save(TEX / "factory_spell_scroll.png")
print(f"  + textures/item/factory_spell_scroll.png")

# ════════════════════════════════════════════════════════════════════════
# 2. Item model JSON
# ════════════════════════════════════════════════════════════════════════

model = {
    "parent": "minecraft:item/generated",
    "textures": {
        "layer0": "liberthia:item/factory_spell_scroll"
    }
}
(MOD / "factory_spell_scroll.json").write_text(
    json.dumps(model, indent=2), encoding="utf-8")
print("  + models/item/factory_spell_scroll.json")

# ════════════════════════════════════════════════════════════════════════
# 3. Lang entries for ALL factory spells (display names)
# ════════════════════════════════════════════════════════════════════════

print("\n=== Lang entries for factory spells ===")
spells = list(SPELLS.glob("factory_*.json"))
print(f"Found {len(spells)} factory spell JSONs")

# Lê display names from JSONs e adiciona ao lang
en_data = json.loads(LANG_EN.read_text(encoding="utf-8"))
pt_data = json.loads(LANG_PT.read_text(encoding="utf-8"))

# Add lang entry pro factory_spell_scroll item base
en_data["item.liberthia.factory_spell_scroll"] = "Spell Scroll (Empty)"
pt_data["item.liberthia.factory_spell_scroll"] = "Pergaminho Vazio"

added = 0
for path in spells:
    if "KITCHEN_SINK" in path.name: continue
    try:
        d = json.loads(path.read_text(encoding="utf-8"))
        spell_id = d.get("id", path.stem)
        name = d.get("name", spell_id)
        # spell tooltip uses display name from SpellDef (already done in code)
        # Mas garante lang key consistente
        key = f"spell.liberthia.{spell_id}"
        if key not in en_data:
            en_data[key] = name
            pt_data[key] = name
            added += 1
    except Exception as e:
        print(f"  ! skip {path.name}: {e}")

LANG_EN.write_text(json.dumps(en_data, indent=2, ensure_ascii=False), encoding="utf-8")
LANG_PT.write_text(json.dumps(pt_data, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"  + {added} spell lang entries")

print("\nDone.")
