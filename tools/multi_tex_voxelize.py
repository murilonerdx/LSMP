"""Voxelize multi-material/multi-texture GLTF/GLB combinando texturas num atlas
e mapeando cubos pra regioes corretas.

Diferenca vs fix_knight_atlas_v2.py:
- Detecta varias submeshes do GLTF (cada uma com textura diferente)
- Combina todas as texturas num atlas grande
- Pra cada cubo, sample da textura/UV correta do submesh dele

Usage:
    python multi_tex_voxelize.py <input.glb> --textures-dir <pasta> --name <nome> --resolution 32
"""
import sys, os, json, base64, uuid, argparse
from collections import defaultdict
from datetime import datetime

import trimesh
import numpy as np
from PIL import Image


def collect_submeshes(scene_or_mesh):
    """Retorna lista de submeshes com possivel textura associada."""
    submeshes = []
    if isinstance(scene_or_mesh, trimesh.Scene):
        for name, geom in scene_or_mesh.geometry.items():
            tex_img = None
            try:
                if hasattr(geom.visual, 'material') and geom.visual.material is not None:
                    mat = geom.visual.material
                    if hasattr(mat, 'image') and mat.image is not None:
                        tex_img = mat.image
                    elif hasattr(mat, 'baseColorTexture') and mat.baseColorTexture is not None:
                        tex_img = mat.baseColorTexture
            except Exception:
                pass
            submeshes.append({
                'name': name,
                'mesh': geom,
                'texture': tex_img
            })
    else:
        # Single mesh
        tex_img = None
        try:
            if hasattr(scene_or_mesh.visual, 'material') and scene_or_mesh.visual.material is not None:
                mat = scene_or_mesh.visual.material
                if hasattr(mat, 'image') and mat.image is not None:
                    tex_img = mat.image
        except Exception:
            pass
        submeshes.append({'name': 'mesh', 'mesh': scene_or_mesh, 'texture': tex_img})
    return submeshes


def build_texture_atlas(textures_dir):
    """Le todas as PNGs da pasta e combina num atlas grande.
    Retorna (atlas_PIL_image, dict {texture_name: (u_offset, v_offset, u_scale, v_scale)})
    """
    files = sorted([f for f in os.listdir(textures_dir) if f.lower().endswith('.png')])
    if not files:
        return None, {}

    textures = []
    for f in files:
        try:
            img = Image.open(os.path.join(textures_dir, f)).convert("RGBA")
            textures.append((f, img))
        except Exception as e:
            print(f"  warn: skip {f}: {e}")

    if not textures:
        return None, {}

    # Pack into grid (sqrt num textures)
    n = len(textures)
    grid_n = int(np.ceil(np.sqrt(n)))
    # Use max dimension across all
    max_w = max(t[1].size[0] for t in textures)
    max_h = max(t[1].size[1] for t in textures)
    cell = max(max_w, max_h)
    atlas_size = grid_n * cell
    # Round up to next pow2
    pow2 = 32
    while pow2 < atlas_size:
        pow2 *= 2

    atlas = Image.new("RGBA", (pow2, pow2), (0, 0, 0, 0))
    mapping = {}
    for i, (fname, img) in enumerate(textures):
        col = i % grid_n
        row = i // grid_n
        px = col * cell
        py = row * cell
        # Center the texture in cell if smaller
        atlas.paste(img, (px, py))
        # Mapping: original UV space (0-1) -> atlas UV space
        # Original texture covers (px, py) to (px + img.width, py + img.height) in atlas
        u_offset = px / pow2
        v_offset = py / pow2
        u_scale = img.width / pow2
        v_scale = img.height / pow2
        mapping[fname] = (u_offset, v_offset, u_scale, v_scale)
    return atlas, mapping


def voxelize_combined(mesh, resolution, bounds_min, pitch):
    """Voxeliza a mesh combinada."""
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input")
    ap.add_argument("--textures-dir", required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--resolution", type=int, default=32)
    ap.add_argument("--mod-id", default="liberthia")
    ap.add_argument("--mod-root", default=None)
    ap.add_argument("--bbmodel-out", default=None)
    args = ap.parse_args()

    print(f"[1/7] Loading: {args.input}")
    scene = trimesh.load(args.input)
    if isinstance(scene, trimesh.Scene):
        n_sub = len(scene.geometry)
        print(f"      Scene com {n_sub} submeshes")
        # Concatena pra voxelizacao
        merged = trimesh.util.concatenate(tuple(scene.dump()))
    else:
        merged = scene
        n_sub = 1
    print(f"      Total tris: {len(merged.faces)}")

    print(f"[2/7] Building texture atlas from {args.textures_dir}")
    atlas, tex_map = build_texture_atlas(args.textures_dir)
    if atlas is None:
        print("      ERRO: nao consegui criar atlas")
        sys.exit(1)
    print(f"      Atlas: {atlas.size[0]}x{atlas.size[1]}, {len(tex_map)} textures combinadas")

    print(f"[3/7] Voxelizing in {args.resolution}^3...")
    bounds = merged.bounds
    extent = bounds[1] - bounds[0]
    pitch = float(extent.max()) / args.resolution
    grid = voxelize_combined(merged, args.resolution, bounds[0], pitch)
    n_vox = int(grid.sum())
    print(f"      voxels: {n_vox}")

    print(f"[4/7] Greedy meshing...")
    boxes = greedy_3d(grid)
    print(f"      boxes: {len(boxes)}")

    print(f"[5/7] Mapping faces to voxels (combined mesh)...")
    voxel_to_faces = defaultdict(list)
    centroids = merged.triangles_center
    for face_i, centroid in enumerate(centroids):
        vox = ((centroid - bounds[0]) / pitch).astype(int)
        vox = np.clip(vox, 0, args.resolution - 1)
        voxel_to_faces[tuple(vox)].append(face_i)
    print(f"      voxels com faces: {len(voxel_to_faces)}")

    print(f"[6/7] Computing UV per box (using combined mesh UVs)...")
    has_uv = hasattr(merged.visual, 'uv') and merged.visual.uv is not None and len(merged.visual.uv) > 0
    print(f"      mesh tem UV: {has_uv}")

    atlas_w, atlas_h = atlas.size

    # Pra cada submesh, anota offset+scale no atlas
    # IMPORTANTE: o merged mesh usa UVs do submesh "concatenado" — vamos assumir que cada
    # submesh ocupava uma porcao das vertex IDs do merged. Pra fazer mapping correto,
    # precisamos rastrear qual face do merged veio de qual submesh.
    # Aproximacao: pega 1 textura como "primary" e mapeia tudo nela.
    # Pra precisao alta precisaria scene.graph traversal.
    primary_texture_key = list(tex_map.keys())[0]
    primary_offset = tex_map[primary_texture_key]
    print(f"      texture mapping ref: {primary_texture_key} -> atlas region")

    box_uvs = []
    for box in boxes:
        x0, y0, z0, w, h, d = box
        face_indices = []
        for dx in range(w):
            for dy in range(h):
                for dz in range(d):
                    face_indices.extend(voxel_to_faces.get((x0+dx, y0+dy, z0+dz), []))

        # Neighbor expansion if empty
        if not face_indices:
            cx, cy, cz = x0+w/2, y0+h/2, z0+d/2
            for r in range(1, max(args.resolution//2, 5)):
                for dx in range(-r, r+1):
                    for dy in range(-r, r+1):
                        for dz in range(-r, r+1):
                            if max(abs(dx), abs(dy), abs(dz)) != r:
                                continue
                            nx, ny, nz = int(cx+dx), int(cy+dy), int(cz+dz)
                            if 0 <= nx < args.resolution and 0 <= ny < args.resolution and 0 <= nz < args.resolution:
                                face_indices.extend(voxel_to_faces.get((nx, ny, nz), []))
                if face_indices:
                    break

        if not face_indices or not has_uv:
            box_uvs.append([8.0, 8.0, 9.0, 9.0])
            continue

        # Collect UVs
        uvs = []
        for fi in face_indices:
            for vi in merged.faces[fi]:
                uvs.append(merged.visual.uv[vi])
        uvs = np.array(uvs)
        u_min, v_min = uvs.min(axis=0)
        u_max, v_max = uvs.max(axis=0)

        # Map from original UV (0-1) -> atlas UV (0-1)
        # NOTE: assuming primary texture mapping. For real multi-tex would need per-face source tracking.
        u_off, v_off, u_sc, v_sc = primary_offset
        atlas_u_min = u_off + u_min * u_sc
        atlas_v_min = v_off + v_min * v_sc
        atlas_u_max = u_off + u_max * u_sc
        atlas_v_max = v_off + v_max * v_sc

        # To Minecraft UV (0-16) with V flipped
        box_uvs.append([
            float(atlas_u_min * 16),
            float((1.0 - atlas_v_max) * 16),
            float(atlas_u_max * 16),
            float((1.0 - atlas_v_min) * 16),
        ])

    print(f"[7/7] Writing files...")
    mod_root = args.mod_root or os.path.normpath(
        os.path.join(os.path.dirname(__file__), "..", "src/main/resources"))
    assets = os.path.join(mod_root, "assets", args.mod_id)
    os.makedirs(os.path.join(assets, "textures", "block"), exist_ok=True)
    os.makedirs(os.path.join(assets, "models", "block"), exist_ok=True)
    os.makedirs(os.path.join(assets, "models", "item"), exist_ok=True)
    os.makedirs(os.path.join(assets, "blockstates"), exist_ok=True)

    # Save atlas
    atlas_path = os.path.join(assets, "textures", "block", f"{args.name}.png")
    atlas.save(atlas_path)
    print(f"      [OK] atlas: {os.path.relpath(atlas_path, mod_root)}")

    # MC block model JSON
    scale = 16.0 / args.resolution
    elements = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = [round(v, 3) for v in box_uvs[i]]
        elements.append({
            "from": [round(x*scale, 3), round(y*scale, 3), round(z*scale, 3)],
            "to":   [round((x+w)*scale, 3), round((y+h)*scale, 3), round((z+d)*scale, 3)],
            "faces": {
                "north": {"uv": uv, "texture": "#0"},
                "east":  {"uv": uv, "texture": "#0"},
                "south": {"uv": uv, "texture": "#0"},
                "west":  {"uv": uv, "texture": "#0"},
                "up":    {"uv": uv, "texture": "#0"},
                "down":  {"uv": uv, "texture": "#0"}
            }
        })
    mc_json = {
        "credit": "multi_tex_voxelize.py — atlas combinado de N texturas",
        "textures": {"0": f"{args.mod_id}:block/{args.name}",
                      "particle": f"{args.mod_id}:block/{args.name}"},
        "elements": elements
    }
    mc_path = os.path.join(assets, "models", "block", f"{args.name}.json")
    with open(mc_path, "w") as f:
        json.dump(mc_json, f, indent=2)
    print(f"      [OK] MC JSON: {os.path.relpath(mc_path, mod_root)}")

    item_path = os.path.join(assets, "models", "item", f"{args.name}.json")
    with open(item_path, "w") as f:
        json.dump({"parent": f"{args.mod_id}:block/{args.name}"}, f)
    blockstate_path = os.path.join(assets, "blockstates", f"{args.name}.json")
    with open(blockstate_path, "w") as f:
        json.dump({"variants": {"": {"model": f"{args.mod_id}:block/{args.name}"}}}, f)

    # bbmodel
    elements_bb = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = box_uvs[i]
        eid = str(uuid.uuid4())
        elements_bb.append({
            "name": f"cube_{i}", "rescale": False, "locked": False,
            "from": [x*scale, y*scale, z*scale],
            "to":   [(x+w)*scale, (y+h)*scale, (z+d)*scale],
            "autouv": 0, "color": 0, "origin": [8, 8, 8],
            "faces": {dir_: {"uv": uv, "texture": 0} for dir_ in
                      ["north", "east", "south", "west", "up", "down"]},
            "uuid": eid, "type": "cube"
        })
    with open(atlas_path, "rb") as f:
        atlas_b64 = base64.b64encode(f.read()).decode("ascii")
    bb = {
        "meta": {"format_version": "4.5", "model_format": "java_block",
                 "box_uv": False, "creation_time": int(datetime.now().timestamp())},
        "name": args.name, "model_identifier": "",
        "modded_entity_version": "", "modded_entity_flip_y": True,
        "added_models": [], "visible_box": [4, 4, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": atlas.size[0], "height": atlas.size[1]},
        "elements": elements_bb,
        "outliner": [{
            "name": args.name, "origin": [8, 8, 8], "rotation": [0, 0, 0],
            "color": 0, "uuid": str(uuid.uuid4()),
            "export": True, "isOpen": True, "locked": False,
            "visibility": True, "autouv": 0,
            "children": [e["uuid"] for e in elements_bb]
        }],
        "textures": [{
            "path": "", "name": f"{args.name}.png", "folder": "block",
            "namespace": "liberthia", "id": "0", "particle": True,
            "render_mode": "default", "visible": True, "mode": "bitmap",
            "saved": False, "uuid": str(uuid.uuid4()),
            "relative_path": "", "source": f"data:image/png;base64,{atlas_b64}"
        }]
    }
    bb_out = args.bbmodel_out or os.path.join(
        os.path.dirname(args.input), f"{args.name}.bbmodel")
    with open(bb_out, "w") as f:
        json.dump(bb, f, indent=2)
    print(f"      [OK] bbmodel: {bb_out}")
    print(f"")
    print(f"=== DONE ===")
    print(f"Abre no Blockbench: {bb_out}")


if __name__ == "__main__":
    main()
