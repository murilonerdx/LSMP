#!/usr/bin/env python3
"""
Gera texturas da Clear Matter armor copiando o formato das Yellow Matter
armor (mesma forma, mesma resolução) e mudando o tom pra azul-claro/cyan.

Base: yellow_matter_{helmet,chestplate,leggings,boots}.png + layer_1/2

Output:
- textures/item/clear_matter_{helmet,chestplate,leggings,boots}.png
- textures/models/armor/clear_matter_layer_1.png (já existe — vou regerar)
- textures/models/armor/clear_matter_layer_2.png (já existe — vou regerar)

Hue shift simples: pixels amarelos (R≈G>>B) viram cyan-azul (B>>R≈G).
Preserva alpha e tons escuros/claros.
"""
from pathlib import Path
from PIL import Image

ROOT = Path("src/main/resources/assets/liberthia/textures")

# Mapeamento de cor: amarelo -> azul ciano
# Yellow tem tipicamente RGB tipo (240, 200, 40) ou variações
# Clear/Cyan: trocar canal R↔B, dar realce azul
def yellow_to_cyan(r, g, b, a):
    if a == 0:
        return (0, 0, 0, 0)
    # Mapeia "intensidade de amarelo" pra intensidade de cyan
    # Yellow alto = R alto + G alto + B baixo
    # Cyan/azul: B alto + G médio + R baixo
    # Estratégia: troca R com B mantendo G, e adiciona um tom azul claro
    new_r = b              # antes era B baixo -> mantém R baixo
    new_g = g              # G permanece (faz cyan)
    new_b = max(r, b + 40) # B alto puxado do R original

    # Clamp 0..255
    new_r = max(0, min(255, new_r))
    new_g = max(0, min(255, new_g))
    new_b = max(0, min(255, new_b))
    return (new_r, new_g, new_b, a)


def convert(src_path: Path, dst_path: Path):
    src = Image.open(src_path).convert("RGBA")
    dst = Image.new("RGBA", src.size, (0, 0, 0, 0))
    px_src = src.load()
    px_dst = dst.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px_src[x, y]
            px_dst[x, y] = yellow_to_cyan(r, g, b, a)
    dst.save(dst_path, "PNG", optimize=False)
    print(f"  OK  {src_path.name} -> {dst_path}")


# Item icons (formato 64x32 que o user gosta — "parece armadura")
print("[clear_matter armor textures]")
for piece in ("helmet", "chestplate", "leggings", "boots"):
    src = ROOT / "item" / f"yellow_matter_{piece}.png"
    dst = ROOT / "item" / f"clear_matter_{piece}.png"
    convert(src, dst)

# Armor model layers (worn texture) — mesmo hue shift
for layer in (1, 2):
    src = ROOT / "models/armor" / f"yellow_matter_layer_{layer}.png"
    dst = ROOT / "models/armor" / f"clear_matter_layer_{layer}.png"
    convert(src, dst)

print("Done.")
