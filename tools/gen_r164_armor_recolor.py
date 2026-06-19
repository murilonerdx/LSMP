#!/usr/bin/env python3
"""r164: User pediu pra copiar o model JSON e texturas do dark_matter_chestplate
pra outras armaduras só mudando cor.

Estratégia:
1. Lê dark_matter_<piece>.png como source template (25x19 ish)
2. Pra cada (color × piece), faz hue shift + save
3. Copia o JSON do dark_matter_chestplate template pra cada destino, ajustando
   o textures.layer0
"""
import json
import shutil
from pathlib import Path
from PIL import Image
import colorsys

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia")
TEX_DIR = ROOT / "textures" / "item"
MODEL_DIR = ROOT / "models" / "item"

PIECES = ["helmet", "chestplate", "leggings", "boots"]

# (armor_name, hue_target_degrees, brightness_mult)
# hue_target = 0=red, 30=orange, 60=yellow, 120=green, 180=cyan, 240=blue, 280=purple, 320=pink
COLORS = {
    "clear_matter":   (180, 1.15),  # ciano-claro
    "yellow_matter":  (50,  1.10),  # amarelo-dourado
    "blood":          (0,   0.95),  # vermelho-sangue
    "order":          (45,  1.20),  # dourado holy
    "sanguine":       (350, 0.90),  # vermelho-vinho
    "containment_suit": (60, 1.10), # amarelo hazmat
}

SOURCE_COLOR = "dark_matter"   # template


def hue_shift(img, target_hue, brightness):
    """Reapinta uma imagem RGBA, deslocando o hue dominante pro target."""
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size

    # Detecta hue médio dos pixels não-transparentes
    out = Image.new("RGBA", (w, h))
    out_px = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 20:
                out_px[x, y] = (r, g, b, a)
                continue
            # Converte pra HSV
            h_, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            # Substitui hue, mantém saturação e modifica brilho
            new_h = (target_hue / 360.0)
            new_v = min(1.0, v * brightness)
            nr, ng, nb = colorsys.hsv_to_rgb(new_h, s, new_v)
            out_px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return out


def gen_textures():
    for color_name, (hue, bright) in COLORS.items():
        for piece in PIECES:
            src_path = TEX_DIR / f"{SOURCE_COLOR}_{piece}.png"
            dst_path = TEX_DIR / f"{color_name}_{piece}.png"
            if not src_path.exists():
                print(f"  SKIP {color_name}_{piece}: source {src_path.name} não existe")
                continue
            src_img = Image.open(src_path)
            shifted = hue_shift(src_img, hue, bright)
            shifted.save(dst_path)
            print(f"  OK {color_name}_{piece}.png (hue={hue}°, bright={bright})")


def gen_models():
    """Copia o JSON template do dark_matter_<piece>.json mudando layer0."""
    for color_name in COLORS.keys():
        for piece in PIECES:
            src_path = MODEL_DIR / f"{SOURCE_COLOR}_{piece}.json"
            dst_path = MODEL_DIR / f"{color_name}_{piece}.json"
            if not src_path.exists():
                # Fallback: cria modelo padrão simples
                model = {
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"liberthia:item/{color_name}_{piece}"}
                }
                dst_path.write_text(json.dumps(model, indent=2), encoding="utf-8")
                print(f"  OK {color_name}_{piece}.json (fallback simple)")
                continue
            # Lê o template e substitui texture path
            model = json.loads(src_path.read_text(encoding="utf-8"))
            if "textures" in model and "layer0" in model["textures"]:
                model["textures"]["layer0"] = f"liberthia:item/{color_name}_{piece}"
            # Também atualiza name interno do group, se houver
            if "groups" in model:
                for gr in model["groups"]:
                    if "name" in gr and gr["name"].startswith(SOURCE_COLOR):
                        gr["name"] = gr["name"].replace(SOURCE_COLOR, color_name)
            dst_path.write_text(json.dumps(model, indent=2), encoding="utf-8")
            print(f"  OK {color_name}_{piece}.json (model copy)")


def main():
    print("=== Texturas ===")
    gen_textures()
    print("\n=== Models ===")
    gen_models()
    print(f"\nDONE — {len(COLORS)} colors × {len(PIECES)} pieces = {len(COLORS) * len(PIECES)} arquivos.")


if __name__ == "__main__":
    main()
