"""Gera arquivo .bbmodel (formato nativo Blockbench) a partir de mesh GLTF/OBJ.

Vantagem: você abre direto no Blockbench, mexe nos cubos visualmente, e exporta como
JSON quando estiver feliz. Compatível com Java Block/Item project type.

Usage:
    python gltf_to_bbmodel.py input.gltf [--name knight] [--resolution 24]
"""
import sys
import os
import json
import argparse
import uuid
from datetime import datetime

try:
    import trimesh
    import numpy as np
except ImportError as e:
    print(f"FALTA dep: {e}. Roda: pip install trimesh numpy")
    sys.exit(1)


def voxelize(mesh, resolution):
    bounds = mesh.bounds
    extent = bounds[1] - bounds[0]
    pitch = float(extent.max()) / resolution
    if pitch == 0:
        pitch = 1
    vox = mesh.voxelized(pitch=pitch).fill()
    mx = vox.matrix
    out = np.zeros((resolution, resolution, resolution), dtype=bool)
    sx, sy, sz = mx.shape
    out[:min(sx, resolution), :min(sy, resolution), :min(sz, resolution)] = \
        mx[:min(sx, resolution), :min(sy, resolution), :min(sz, resolution)]
    return out


def greedy_3d(grid):
    X, Y, Z = grid.shape
    used = np.zeros_like(grid, dtype=bool)
    boxes = []
    for z in range(Z):
        for y in range(Y):
            for x in range(X):
                if grid[x, y, z] and not used[x, y, z]:
                    w = 1
                    while x + w < X and grid[x + w, y, z] and not used[x + w, y, z]:
                        w += 1
                    h = 1
                    while y + h < Y:
                        ok = all(grid[x + k, y + h, z] and not used[x + k, y + h, z]
                                 for k in range(w))
                        if not ok:
                            break
                        h += 1
                    d = 1
                    while z + d < Z:
                        ok = True
                        for ky in range(h):
                            for kx in range(w):
                                if not grid[x + kx, y + ky, z + d] or used[x + kx, y + ky, z + d]:
                                    ok = False
                                    break
                            if not ok:
                                break
                        if not ok:
                            break
                        d += 1
                    used[x:x + w, y:y + h, z:z + d] = True
                    boxes.append((x, y, z, w, h, d))
    return boxes


def boxes_to_bbmodel(boxes, resolution, name, texture_path=None):
    """Constrói estrutura .bbmodel pro Blockbench."""
    scale = 16.0 / resolution
    project_uuid = str(uuid.uuid4())

    elements = []
    for (x, y, z, w, h, d) in boxes:
        elem_uuid = str(uuid.uuid4())
        elements.append({
            "name": "cube",
            "rescale": False,
            "locked": False,
            "from": [x * scale, y * scale, z * scale],
            "to":   [(x + w) * scale, (y + h) * scale, (z + d) * scale],
            "autouv": 0,
            "color": 0,
            "origin": [8, 8, 8],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": 0},
                "east":  {"uv": [0, 0, 16, 16], "texture": 0},
                "south": {"uv": [0, 0, 16, 16], "texture": 0},
                "west":  {"uv": [0, 0, 16, 16], "texture": 0},
                "up":    {"uv": [0, 0, 16, 16], "texture": 0},
                "down":  {"uv": [0, 0, 16, 16], "texture": 0}
            },
            "uuid": elem_uuid,
            "type": "cube"
        })

    # Outliner — agrupa todos os elements num único grupo (pra Blockbench mostrar como tree)
    group_children = [e["uuid"] for e in elements]
    outliner = [{
        "name": name,
        "origin": [8, 8, 8],
        "rotation": [0, 0, 0],
        "color": 0,
        "uuid": str(uuid.uuid4()),
        "export": True,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": group_children
    }]

    # Textures
    textures = []
    if texture_path and os.path.exists(texture_path):
        try:
            import base64
            with open(texture_path, "rb") as f:
                b64 = base64.b64encode(f.read()).decode("ascii")
            textures.append({
                "path": "",
                "name": "knight_texture.png",
                "folder": "block",
                "namespace": "liberthia",
                "id": "0",
                "particle": True,
                "render_mode": "default",
                "visible": True,
                "mode": "bitmap",
                "saved": False,
                "uuid": str(uuid.uuid4()),
                "relative_path": "",
                "source": f"data:image/png;base64,{b64}"
            })
        except Exception as e:
            print(f"  warn: nao consegui embedar textura: {e}")

    bbmodel = {
        "meta": {
            "format_version": "4.5",
            "model_format": "java_block",
            "box_uv": False,
            "creation_time": int(datetime.now().timestamp())
        },
        "name": name,
        "model_identifier": "",
        "modded_entity_version": "",
        "modded_entity_flip_y": True,
        "added_models": [],
        "visible_box": [2, 2, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "outliner": outliner,
        "textures": textures
    }
    return bbmodel


def main():
    parser = argparse.ArgumentParser(description="GLTF → Blockbench .bbmodel")
    parser.add_argument("input", help="Caminho do arquivo .gltf/.glb/.obj")
    parser.add_argument("--name", default=None, help="Nome do modelo")
    parser.add_argument("--resolution", type=int, default=24)
    parser.add_argument("--output", default=None, help="Caminho de saida do .bbmodel")
    parser.add_argument("--texture", default=None, help="Caminho da textura PNG (opcional, embeda no bbmodel)")
    args = parser.parse_args()

    name = args.name or os.path.splitext(os.path.basename(args.input))[0]
    name = name.lower().replace(" ", "_").replace("-", "_")

    output = args.output or os.path.join(
        os.path.dirname(args.input), f"{name}.bbmodel")

    print(f"[1/4] Carregando mesh: {args.input}")
    mesh = trimesh.load(args.input, force='mesh')
    if isinstance(mesh, trimesh.Scene):
        mesh = trimesh.util.concatenate(tuple(mesh.dump()))
    print(f"      tris: {len(mesh.faces)}, vertices: {len(mesh.vertices)}")

    print(f"[2/4] Voxelizando em {args.resolution}^3...")
    grid = voxelize(mesh, args.resolution)
    print(f"      voxels: {int(grid.sum())}")

    print(f"[3/4] Greedy meshing...")
    boxes = greedy_3d(grid)
    print(f"      gerou {len(boxes)} cubos")

    print(f"[4/4] Gerando .bbmodel...")
    bbm = boxes_to_bbmodel(boxes, args.resolution, name, args.texture)
    with open(output, "w") as f:
        json.dump(bbm, f, indent=2)
    print(f"      [OK] salvo: {output}")
    print(f"")
    print(f"Abre no Blockbench: arrasta '{output}' pra dentro da janela do Blockbench")


if __name__ == "__main__":
    main()
