"""GLTF/OBJ -> Minecraft Block Model JSON converter with greedy meshing.

Solução pro problema "Blockbench importa um bilhão de cubos do GLTF":
1. Voxeliza a mesh em baixa resolução (default 16^3)
2. Aplica GREEDY MESHING (agrupa cubos adjacentes em retângulos maiores)
3. Outputs JSON Minecraft block model com poucos elementos grandes

USAGE:
    python gltf_to_minecraft.py input.gltf [--resolution 16] [--name my_candle]

OUTPUT:
    - assets/liberthia/models/block/<name>.json — modelo 3D do bloco
    - assets/liberthia/models/item/<name>.json  — modelo item parented
    - assets/liberthia/blockstates/<name>.json  — blockstate single variant
    - assets/liberthia/textures/block/<name>.png — atlas single-color flat
"""
import sys
import os
import json
import argparse
from collections import defaultdict

try:
    import trimesh
    import numpy as np
    from PIL import Image
except ImportError as e:
    print(f"FALTA dependência: {e}. Roda: pip install trimesh numpy pillow")
    sys.exit(1)


def voxelize_mesh(mesh, resolution=16):
    """Voxeliza mesh em grid resolution^3 alinhado pela bounding box.

    Retorna numpy bool array de shape (resolution, resolution, resolution)
    onde True = voxel ocupado, e a "cor média" de cada voxel (RGBA tuple)."""
    # Centraliza e normaliza pra unit cube
    bounds = mesh.bounds
    extent = bounds[1] - bounds[0]
    max_dim = extent.max()
    if max_dim == 0:
        max_dim = 1
    pitch = max_dim / resolution

    # Voxelize via trimesh
    vox = mesh.voxelized(pitch=pitch)
    vox = vox.fill()  # preenche interior

    # Pega matriz boolean
    matrix = vox.matrix  # shape (X, Y, Z)
    # Pad/crop pra resolution exata
    target_shape = (resolution, resolution, resolution)
    padded = np.zeros(target_shape, dtype=bool)
    sx, sy, sz = matrix.shape
    cx = min(sx, resolution)
    cy = min(sy, resolution)
    cz = min(sz, resolution)
    padded[:cx, :cy, :cz] = matrix[:cx, :cy, :cz]
    return padded


def sample_colors(mesh, voxel_grid, resolution):
    """Sample mesh color pra cada voxel (se mesh tem textura/material)."""
    # Color array: 4 channels RGBA por voxel
    colors = np.zeros((resolution, resolution, resolution, 4), dtype=np.uint8)
    # Fallback: cor base do material
    if hasattr(mesh.visual, 'main_color'):
        base = mesh.visual.main_color
        if base is not None:
            base_rgba = tuple(int(c) for c in base[:4])
        else:
            base_rgba = (200, 200, 200, 255)
    elif hasattr(mesh.visual, 'face_colors') and mesh.visual.face_colors is not None:
        # Pega cor média das faces
        avg = mesh.visual.face_colors.mean(axis=0)
        base_rgba = tuple(int(c) for c in avg[:4])
    else:
        base_rgba = (200, 200, 200, 255)

    for x in range(resolution):
        for y in range(resolution):
            for z in range(resolution):
                if voxel_grid[x, y, z]:
                    colors[x, y, z] = base_rgba
    return colors


def greedy_mesh_layer(slice_2d):
    """Greedy meshing num único slice 2D.
    Retorna lista de retângulos (x, y, w, h) que cobrem todos os True pixels."""
    rects = []
    used = np.zeros_like(slice_2d, dtype=bool)
    H, W = slice_2d.shape
    for y in range(H):
        for x in range(W):
            if slice_2d[y, x] and not used[y, x]:
                # Acha largura máxima
                w = 1
                while x + w < W and slice_2d[y, x + w] and not used[y, x + w]:
                    w += 1
                # Acha altura máxima pra essa largura
                h = 1
                while y + h < H:
                    if not all(slice_2d[y + h, x + k] and not used[y + h, x + k]
                               for k in range(w)):
                        break
                    h += 1
                # Marca como usado
                used[y:y + h, x:x + w] = True
                rects.append((x, y, w, h))
    return rects


def greedy_mesh_3d(voxel_grid):
    """Faz greedy meshing 3D agrupando voxels em boxes maiores.
    Estratégia: itera Z layers, faz greedy 2D em X-Y, depois tenta estender em Z."""
    X, Y, Z = voxel_grid.shape
    used = np.zeros_like(voxel_grid, dtype=bool)
    boxes = []  # cada box = (x0, y0, z0, w, h, d)

    for z in range(Z):
        for y in range(Y):
            for x in range(X):
                if voxel_grid[x, y, z] and not used[x, y, z]:
                    # Estende em X
                    w = 1
                    while x + w < X and voxel_grid[x + w, y, z] and not used[x + w, y, z]:
                        w += 1
                    # Estende em Y
                    h = 1
                    while y + h < Y:
                        if not all(voxel_grid[x + k, y + h, z] and not used[x + k, y + h, z]
                                   for k in range(w)):
                            break
                        h += 1
                    # Estende em Z
                    d = 1
                    while z + d < Z:
                        ok = True
                        for ky in range(h):
                            for kx in range(w):
                                if not voxel_grid[x + kx, y + ky, z + d] or used[x + kx, y + ky, z + d]:
                                    ok = False
                                    break
                            if not ok:
                                break
                        if not ok:
                            break
                        d += 1
                    # Marca usado
                    used[x:x + w, y:y + h, z:z + d] = True
                    boxes.append((x, y, z, w, h, d))
    return boxes


def boxes_to_minecraft_json(boxes, resolution, texture_ref="liberthia:block/template"):
    """Converte boxes (x, y, z, w, h, d) em formato Minecraft block model JSON.

    Minecraft block coords: 0..16 (espaço de 1 bloco). Vamos escalar resolução pra esse range.
    """
    scale = 16.0 / resolution
    elements = []
    for (x, y, z, w, h, d) in boxes:
        elem = {
            "from": [round(x * scale, 2), round(y * scale, 2), round(z * scale, 2)],
            "to":   [round((x + w) * scale, 2), round((y + h) * scale, 2), round((z + d) * scale, 2)],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#0"},
                "east":  {"uv": [0, 0, 16, 16], "texture": "#0"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#0"},
                "west":  {"uv": [0, 0, 16, 16], "texture": "#0"},
                "up":    {"uv": [0, 0, 16, 16], "texture": "#0"},
                "down":  {"uv": [0, 0, 16, 16], "texture": "#0"},
            }
        }
        elements.append(elem)

    model = {
        "credit": "Converted from GLTF by gltf_to_minecraft.py (greedy meshing)",
        "textures": {
            "0": texture_ref,
            "particle": texture_ref
        },
        "elements": elements
    }
    return model


def generate_flat_texture(color, output_path, size=16):
    """Cria textura 16×16 single-color com gradient/highlight básico."""
    img = Image.new("RGBA", (size, size), color)
    # Add tiny pixel variation pra não ficar liso
    arr = np.array(img)
    noise = np.random.randint(-15, 15, size=arr[:, :, :3].shape, dtype=np.int16)
    arr[:, :, :3] = np.clip(arr[:, :, :3].astype(np.int16) + noise, 0, 255).astype(np.uint8)
    Image.fromarray(arr).save(output_path)


def main():
    parser = argparse.ArgumentParser(description="GLTF/OBJ -> Minecraft block model")
    parser.add_argument("input", help="Path do arquivo .gltf/.glb/.obj de entrada")
    parser.add_argument("--resolution", type=int, default=16,
                        help="Resolução voxel (8/16/32). Maior=mais detalhe, mais cubos.")
    parser.add_argument("--name", default=None, help="Nome do bloco no mod")
    parser.add_argument("--mod-id", default="liberthia", help="Mod ID")
    parser.add_argument("--out-dir", default=None,
                        help="Root assets dir (default: ../src/main/resources)")
    args = parser.parse_args()

    if not os.path.exists(args.input):
        print(f"ERRO: arquivo não encontrado: {args.input}")
        sys.exit(1)

    name = args.name or os.path.splitext(os.path.basename(args.input))[0]
    name = name.lower().replace(" ", "_").replace("-", "_")

    root = args.out_dir or os.path.normpath(
        os.path.join(os.path.dirname(__file__), "..", "src/main/resources"))

    assets = os.path.join(root, "assets", args.mod_id)
    paths = {
        "block_model": os.path.join(assets, "models", "block", f"{name}.json"),
        "item_model":  os.path.join(assets, "models", "item",  f"{name}.json"),
        "blockstate":  os.path.join(assets, "blockstates", f"{name}.json"),
        "texture":     os.path.join(assets, "textures", "block", f"{name}.png"),
    }
    for p in paths.values():
        os.makedirs(os.path.dirname(p), exist_ok=True)

    print(f"[1/5] Carregando mesh: {args.input}")
    mesh = trimesh.load(args.input, force='mesh')
    if isinstance(mesh, trimesh.Scene):
        # Combina todos os geometries da scene num único mesh
        mesh = trimesh.util.concatenate(tuple(mesh.dump()))
    print(f"      tris: {len(mesh.faces)}, vertices: {len(mesh.vertices)}")

    print(f"[2/5] Voxelizando em {args.resolution}^3...")
    voxel_grid = voxelize_mesh(mesh, resolution=args.resolution)
    n_voxels = int(voxel_grid.sum())
    print(f"      voxels ocupados: {n_voxels}/{args.resolution**3}")

    print(f"[3/5] Greedy meshing (agrupando cubos contíguos)...")
    boxes = greedy_mesh_3d(voxel_grid)
    print(f"      gerou {len(boxes)} boxes (vs {n_voxels} cubos individuais) "
          f"-> redução de {(1 - len(boxes) / max(1, n_voxels)) * 100:.1f}%")

    if len(boxes) > 384:
        print(f"      [AVISO] {len(boxes)} elements >limite de Minecraft (~384). "
              f"Reduz --resolution.")

    print(f"[4/5] Gerando JSON model...")
    texture_ref = f"{args.mod_id}:block/{name}"
    model_json = boxes_to_minecraft_json(boxes, args.resolution, texture_ref)
    with open(paths["block_model"], "w") as f:
        json.dump(model_json, f, indent=2)

    # Item model (parented)
    item_model = {"parent": f"{args.mod_id}:block/{name}"}
    with open(paths["item_model"], "w") as f:
        json.dump(item_model, f, indent=2)

    # Blockstate (single variant)
    blockstate = {"variants": {"": {"model": f"{args.mod_id}:block/{name}"}}}
    with open(paths["blockstate"], "w") as f:
        json.dump(blockstate, f, indent=2)

    # Texture — tenta extrair cor do material, senão usa cinza
    try:
        if hasattr(mesh.visual, 'main_color') and mesh.visual.main_color is not None:
            color = tuple(int(c) for c in mesh.visual.main_color[:4])
        else:
            color = (180, 100, 200, 255)  # roxo default
    except Exception:
        color = (180, 100, 200, 255)
    generate_flat_texture(color, paths["texture"])

    print(f"[5/5] Arquivos gerados:")
    for k, v in paths.items():
        rel = os.path.relpath(v, root)
        print(f"      * {k}: {rel}")
    print()
    print(f"[OK] Bloco '{name}' pronto. Agora REGISTRE-O em Java:")
    print(f"   ModBlocks.{name.upper()} via DeferredRegister")
    print(f"   ModItems.{name.upper()}_ITEM via BlockItem")
    print(f"   Lang entry: block.{args.mod_id}.{name}")


if __name__ == "__main__":
    main()
