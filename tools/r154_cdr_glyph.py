"""r154: Texture + model + lang pro Cooldown Reduction Glyph."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(".")
TEX = ROOT / "src/main/resources/assets/liberthia/textures/item"
MOD = ROOT / "src/main/resources/assets/liberthia/models/item"
LANG_EN = ROOT / "src/main/resources/assets/liberthia/lang/en_us.json"
LANG_PT = ROOT / "src/main/resources/assets/liberthia/lang/pt_br.json"

# Texture: ampulheta com seta circular (símbolo de tempo + reduce)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Cores: glyph azul-ciano com glow
glyph = (100, 200, 255, 255)
glyph_bright = (200, 255, 255, 255)
glyph_dark = (50, 120, 180, 255)
gold = (255, 220, 100, 255)

# Ampulheta no centro
# Top triangle (5,2-11,7)
d.polygon([(5, 2), (11, 2), (8, 7)], fill=glyph)
# Bottom triangle (5,13-11,8)
d.polygon([(5, 13), (11, 13), (8, 8)], fill=glyph)
# Centro (constriction)
img.putpixel((8, 7), glyph_bright)
img.putpixel((8, 8), glyph_bright)

# Borda dourada (highlight)
d.line([(5, 2), (11, 2)], fill=gold)
d.line([(5, 13), (11, 13)], fill=gold)

# Seta circular ao redor (símbolo de "recurring/cycle")
img.putpixel((1, 4), glyph_dark)
img.putpixel((2, 3), glyph_dark)
img.putpixel((3, 2), glyph_dark)
img.putpixel((14, 11), glyph_dark)
img.putpixel((13, 12), glyph_dark)
img.putpixel((12, 13), glyph_dark)

# Sparkles
img.putpixel((1, 14), glyph_bright)
img.putpixel((14, 1), glyph_bright)

img.save(TEX / "glyph_cooldown_reduction.png")
print(f"+ {TEX / 'glyph_cooldown_reduction.png'}")

# Item model
model = {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "liberthia:item/glyph_cooldown_reduction"}
}
(MOD / "glyph_cooldown_reduction.json").write_text(json.dumps(model, indent=2), encoding="utf-8")
print(f"+ {MOD / 'glyph_cooldown_reduction.json'}")

# Lang
for path, lang in [(LANG_EN, "en"), (LANG_PT, "pt")]:
    data = json.loads(path.read_text(encoding="utf-8"))
    key = "item.liberthia.glyph_cooldown_reduction"
    data[key] = "Cooldown Reduction Glyph" if lang == "en" else "Glifo de Redução de Cooldown"
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"+ {path.name}")

print("\nDone.")
