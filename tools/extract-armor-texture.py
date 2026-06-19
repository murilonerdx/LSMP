"""
Extrai a textura PNG embutida em base64 dentro do .bbmodel da armadura
e salva no caminho que o Minecraft procura: assets/<modid>/textures/models/armor/<material>_layer_1.png

Uso: python extract-armor-texture.py <input.bbmodel> <output.png>
"""
import json
import base64
import sys
from pathlib import Path

def main(bbmodel_path: str, output_path: str):
    src = Path(bbmodel_path)
    dst = Path(output_path)

    data = json.loads(src.read_text(encoding='utf-8'))
    textures = data.get('textures', [])
    if not textures:
        print(f"ERROR: nenhuma textura encontrada em {src}", file=sys.stderr)
        sys.exit(1)

    # Pega a primeira textura (modelos de armor geralmente têm 1 só)
    tex = textures[0]
    source = tex.get('source', '')
    if not source.startswith('data:image/png;base64,'):
        print(f"ERROR: textura nao eh PNG base64: {source[:50]}...", file=sys.stderr)
        sys.exit(1)

    b64 = source.split(',', 1)[1]
    png_bytes = base64.b64decode(b64)

    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(png_bytes)
    print(f"OK: {len(png_bytes)} bytes -> {dst}")


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Uso: python extract-armor-texture.py <input.bbmodel> <output.png>", file=sys.stderr)
        sys.exit(2)
    main(sys.argv[1], sys.argv[2])
