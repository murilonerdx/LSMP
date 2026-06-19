"""v2: MANTÉM a textura UV original do GLTF (detalhada),
mapeia cada cubo pra REGIÃO correta dela (em vez de gerar atlas simplificado).

Diferença vs v1:
- v1: gerava atlas pequeno com 1 cor por cubo
- v2: usa textura ORIGINAL (com todos os detalhes) e cada cubo aponta pra retângulo UV específico

Algoritmo:
1. Pra cada triângulo da mesh, descobre em qual voxel o centroid cai
2. Pra cada box do greedy mesh, junta TODOS os triângulos que caem dentro dele
3. Pega o bounding rect UV desses triângulos
4. Aplica esse rect como UV das 6 faces do cubo
"""
import sys, os, json, base64, uuid, argparse
from collections import defaultdict
from datetime import datetime

import trimesh
import numpy as np
from PIL import Image


def voxelize(mesh, resolution):
    bounds = mesh.bounds
    extent = bounds[1] - bounds[0]
    pitch = float(extent.max()) / resolution
    vox = mesh.voxelized(pitch=pitch).fill()
    mx = vox.matrix
    out = np.zeros((resolution, resolution, resolution), dtype=bool)
    sx, sy, sz = mx.shape
    out[:min(sx, resolution), :min(sy, resolution), :min(sz, resolution)] = \
        mx[:min(sx, resolution), :min(sy, resolution), :min(sz, resolution)]
    return out, pitch, bounds


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


def map_faces_to_voxels(mesh, pitch, bounds_min, resolution):
    """Pra cada face da mesh, descobre em qual voxel o centroid cai."""
    voxel_to_faces = defaultdict(list)
    centroids = mesh.triangles_center  # shape (n_faces, 3)
    for face_i, centroid in enumerate(centroids):
        vox = ((centroid - bounds_min) / pitch).astype(int)
        vox = np.clip(vox, 0, resolution - 1)
        voxel_to_faces[tuple(vox)].append(face_i)
    return voxel_to_faces


def compute_box_uvs(box, voxel_to_faces, mesh, resolution):
    """Pra um box (x0,y0,z0,w,h,d), encontra UV bounding rect via faces da mesh.

    Retorna [u_min, v_min, u_max, v_max] em range 0-16 (Minecraft format).
    """
    x0, y0, z0, w, h, d = box
    # Coleta face indices de todos voxels dentro do box
    face_indices = []
    for dx in range(w):
        for dy in range(h):
            for dz in range(d):
                key = (x0 + dx, y0 + dy, z0 + dz)
                face_indices.extend(voxel_to_faces.get(key, []))

    # Se sem match (cubo interior), expande pra voxels vizinhos
    if not face_indices:
        # Centro do box
        cx = x0 + w / 2
        cy = y0 + h / 2
        cz = z0 + d / 2
        # Procura em raio crescente
        for r in range(1, max(resolution // 2, 5)):
            for dx in range(-r, r + 1):
                for dy in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        # Apenas borda do cubo de raio r
                        if max(abs(dx), abs(dy), abs(dz)) != r:
                            continue
                        nx = int(cx + dx)
                        ny = int(cy + dy)
                        nz = int(cz + dz)
                        if 0 <= nx < resolution and 0 <= ny < resolution and 0 <= nz < resolution:
                            face_indices.extend(voxel_to_faces.get((nx, ny, nz), []))
            if face_indices:
                break

    if not face_indices:
        # Ultimo fallback: 1 pixel da textura no centro
        return [8.0, 8.0, 9.0, 9.0]

    # Pega UVs de todos vértices dessas faces
    uvs = []
    for fi in face_indices:
        for v_idx in mesh.faces[fi]:
            uv = mesh.visual.uv[v_idx]
            uvs.append(uv)
    uvs = np.array(uvs)

    u_min, v_min = uvs.min(axis=0)
    u_max, v_max = uvs.max(axis=0)

    # Pad pequeno se o rect for muito pequeno (evita pixel zero)
    if u_max - u_min < 0.005:
        u_max = min(1.0, u_min + 0.005)
    if v_max - v_min < 0.005:
        v_max = min(1.0, v_min + 0.005)

    # Convert pra Minecraft UV (0-16 range, V flipped)
    return [
        float(u_min * 16),
        float((1.0 - v_max) * 16),
        float(u_max * 16),
        float((1.0 - v_min) * 16),
    ]


def make_bbmodel(boxes, box_uvs, resolution, name, texture_path):
    scale = 16.0 / resolution
    elements = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = box_uvs[i]
        elem_uuid = str(uuid.uuid4())
        elements.append({
            "name": f"cube_{i}",
            "rescale": False,
            "locked": False,
            "from": [x * scale, y * scale, z * scale],
            "to":   [(x + w) * scale, (y + h) * scale, (z + d) * scale],
            "autouv": 0,
            "color": 0,
            "origin": [8, 8, 8],
            "faces": {
                "north": {"uv": uv, "texture": 0},
                "east":  {"uv": uv, "texture": 0},
                "south": {"uv": uv, "texture": 0},
                "west":  {"uv": uv, "texture": 0},
                "up":    {"uv": uv, "texture": 0},
                "down":  {"uv": uv, "texture": 0}
            },
            "uuid": elem_uuid,
            "type": "cube"
        })

    group_uuid = str(uuid.uuid4())
    outliner = [{
        "name": name,
        "origin": [8, 8, 8],
        "rotation": [0, 0, 0],
        "color": 0,
        "uuid": group_uuid,
        "export": True,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": [e["uuid"] for e in elements]
    }]

    # Embed textura ORIGINAL no bbmodel
    with open(texture_path, "rb") as f:
        tex_b64 = base64.b64encode(f.read()).decode("ascii")

    # Pega dimensões reais da textura
    tex_img = Image.open(texture_path)
    tw, th = tex_img.size

    textures = [{
        "path": "",
        "name": f"{name}.png",
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
        "source": f"data:image/png;base64,{tex_b64}"
    }]

    return {
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
        # IMPORTANTE: resolution = dimensões reais da textura
        "resolution": {"width": tw, "height": th},
        "elements": elements,
        "outliner": outliner,
        "textures": textures
    }


def make_minecraft_json(boxes, box_uvs, resolution, texture_ref):
    scale = 16.0 / resolution
    elements = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = [round(v, 3) for v in box_uvs[i]]
        elements.append({
            "from": [round(x * scale, 3), round(y * scale, 3), round(z * scale, 3)],
            "to":   [round((x + w) * scale, 3), round((y + h) * scale, 3), round((z + d) * scale, 3)],
            "faces": {
                "north": {"uv": uv, "texture": "#0"},
                "east":  {"uv": uv, "texture": "#0"},
                "south": {"uv": uv, "texture": "#0"},
                "west":  {"uv": uv, "texture": "#0"},
                "up":    {"uv": uv, "texture": "#0"},
                "down":  {"uv": uv, "texture": "#0"}
            }
        })
    return {
        "credit": "v2 — uses ORIGINAL GLTF texture; per-cube UV rect mapping",
        "textures": {"0": texture_ref, "particle": texture_ref},
        "elements": elements
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input")
    ap.add_argument("--texture", required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--resolution", type=int, default=24)
    ap.add_argument("--mod-id", default="liberthia")
    ap.add_argument("--mod-root", default=None)
    ap.add_argument("--bbmodel-out", default=None)
    args = ap.parse_args()

    print(f"[1/6] Loading mesh: {args.input}")
    mesh = trimesh.load(args.input, force='mesh')
    if isinstance(mesh, trimesh.Scene):
        mesh = trimesh.util.concatenate(tuple(mesh.dump()))
    print(f"      tris: {len(mesh.faces)}")
    if not (hasattr(mesh.visual, 'uv') and mesh.visual.uv is not None):
        print("      ERRO: mesh sem UV mapping. Nao da pra usar v2.")
        sys.exit(1)

    tex_img = Image.open(args.texture).convert("RGBA")
    print(f"[2/6] Textura original: {args.texture} ({tex_img.size[0]}x{tex_img.size[1]})")

    print(f"[3/6] Voxelizando em {args.resolution}^3...")
    grid, pitch, bounds = voxelize(mesh, args.resolution)
    n_vox = int(grid.sum())
    print(f"      voxels: {n_vox}")

    print(f"[4/6] Greedy meshing...")
    boxes = greedy_3d(grid)
    print(f"      boxes: {len(boxes)}")

    print(f"[5/6] Mapeando UVs por cubo (mantendo textura original)...")
    v2f = map_faces_to_voxels(mesh, pitch, bounds[0], args.resolution)
    print(f"      voxels com faces: {len(v2f)}")
    box_uvs = []
    boxes_no_match = 0
    for box in boxes:
        uv = compute_box_uvs(box, v2f, mesh, args.resolution)
        box_uvs.append(uv)
        # Check fallback rate
        if uv == [8.0, 8.0, 9.0, 9.0]:
            boxes_no_match += 1
    print(f"      boxes com UV ok: {len(boxes) - boxes_no_match}/{len(boxes)}")

    print(f"[6/6] Gerando arquivos...")
    mod_root = args.mod_root or os.path.normpath(
        os.path.join(os.path.dirname(__file__), "..", "src/main/resources"))
    assets = os.path.join(mod_root, "assets", args.mod_id)

    # Copia textura ORIGINAL pro mod (NAO gera atlas)
    tex_out = os.path.join(assets, "textures", "block", f"{args.name}.png")
    os.makedirs(os.path.dirname(tex_out), exist_ok=True)
    tex_img.save(tex_out)
    print(f"      [OK] texture (original copy): {os.path.relpath(tex_out, mod_root)}")

    # MC JSON
    mc_json = make_minecraft_json(boxes, box_uvs, args.resolution,
                                   f"{args.mod_id}:block/{args.name}")
    mc_path = os.path.join(assets, "models", "block", f"{args.name}.json")
    os.makedirs(os.path.dirname(mc_path), exist_ok=True)
    with open(mc_path, "w") as f:
        json.dump(mc_json, f, indent=2)
    print(f"      [OK] MC JSON: {os.path.relpath(mc_path, mod_root)}")

    item_path = os.path.join(assets, "models", "item", f"{args.name}.json")
    os.makedirs(os.path.dirname(item_path), exist_ok=True)
    with open(item_path, "w") as f:
        json.dump({"parent": f"{args.mod_id}:block/{args.name}"}, f)

    blockstate_path = os.path.join(assets, "blockstates", f"{args.name}.json")
    os.makedirs(os.path.dirname(blockstate_path), exist_ok=True)
    with open(blockstate_path, "w") as f:
        json.dump({"variants": {"": {"model": f"{args.mod_id}:block/{args.name}"}}}, f)

    # bbmodel
    bb = make_bbmodel(boxes, box_uvs, args.resolution, args.name, args.texture)
    bb_out = args.bbmodel_out or os.path.join(
        os.path.dirname(args.input), f"{args.name}_v2.bbmodel")
    with open(bb_out, "w") as f:
        json.dump(bb, f, indent=2)
    print(f"      [OK] bbmodel: {bb_out}")
    print(f"")
    print(f"=== DONE === Re-abre no Blockbench:")
    print(f"    {bb_out}")


if __name__ == "__main__":
    main()
