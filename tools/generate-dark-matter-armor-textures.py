#!/usr/bin/env python3
"""
Gera texturas da Dark Matter armor a partir das Yellow Matter armor —
mesmo formato, hue shift amarelo → roxo profundo / preto-violeta.
"""
from pathlib import Path
from PIL import Image

ROOT = Path("src/main/resources/assets/liberthia/textures")

def yellow_to_dark(r, g, b, a):
    if a == 0:
        return (0, 0, 0, 0)
    # Yellow (R alto, G alto, B baixo) -> Dark purple (R médio, G baixo, B alto)
    # Estratégia: inverte parcialmente — usa GREEN como base de "intensidade",
    # transforma em violeta escuro
    intensity = (r + g) / 2.0 / 255.0  # 0..1
    # Cores resultantes
    new_r = int(20 + intensity * 80)   # 20..100 (roxo escuro)
    new_g = int(8 + intensity * 30)    # 8..38 (quase preto)
    new_b = int(35 + intensity * 120)  # 35..155 (violeta)
    return (
        max(0, min(255, new_r)),
        max(0, min(255, new_g)),
        max(0, min(255, new_b)),
        a
    )

def convert(src_path: Path, dst_path: Path):
    src = Image.open(src_path).convert("RGBA")
    dst = Image.new("RGBA", src.size, (0, 0, 0, 0))
    px_src = src.load()
    px_dst = dst.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px_src[x, y]
            px_dst[x, y] = yellow_to_dark(r, g, b, a)
    dst.save(dst_path, "PNG", optimize=False)
    print(f"  OK  {dst_path}")

print("[dark_matter armor textures]")
for piece in ("helmet", "chestplate", "leggings", "boots"):
    src = ROOT / "item" / f"yellow_matter_{piece}.png"
    dst = ROOT / "item" / f"dark_matter_{piece}.png"
    convert(src, dst)

for layer in (1, 2):
    src = ROOT / "models/armor" / f"yellow_matter_layer_{layer}.png"
    dst = ROOT / "models/armor" / f"dark_matter_layer_{layer}.png"
    convert(src, dst)

print("Done.")
