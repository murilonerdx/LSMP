"""Fixa o problema "cada cubo mostra a textura inteira esticada".

Como funciona:
1. Pra cada cubo do voxel grid, acha o ponto mais proximo na mesh original
2. Pega a UV daquele ponto -> sample da textura -> cor do cubo
3. Gera atlas NOVO onde cada cubo tem um quadradinho de cor solida
4. Atualiza UVs no bbmodel pra cada cubo apontar pro SEU quadradinho

Result: knight com cores corretas (silver/azul/laranja distintas por parte do corpo).
"""
import sys, os, json, base64, uuid, argparse
from datetime import datetime

try:
    import trimesh
    import numpy as np
    from PIL import Image
except ImportError as e:
    print(f"FALTA dep: {e}. Roda: pip install trimesh numpy pillow")
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


def sample_box_colors(mesh, boxes, pitch, bounds_min, tex_img):
    """Pra cada box, amostra cor da textura UV no ponto mais proximo da mesh."""
    has_uv = (hasattr(mesh.visual, 'uv') and
              mesh.visual.uv is not None and
              len(mesh.visual.uv) > 0)
    print(f"      mesh has UV: {has_uv}")

    centers_world = []
    for (x0, y0, z0, w, h, d) in boxes:
        cx = x0 + w / 2.0
        cy = y0 + h / 2.0
        cz = z0 + d / 2.0
        wp = bounds_min + np.array([cx, cy, cz]) * pitch
        centers_world.append(wp)
    centers_world = np.array(centers_world)

    # Bulk closest_point query
    try:
        closest, dist, face_idx = trimesh.proximity.closest_point(
            mesh, centers_world)
    except Exception as e:
        print(f"      closest_point falhou: {e}, usando fallback grey")
        return [(150, 150, 150, 255)] * len(boxes)

    colors = []
    tex_w, tex_h = tex_img.size
    for i, (face_i, closest_pt) in enumerate(zip(face_idx, closest)):
        face_i = int(face_i)
        try:
            if not has_uv:
                colors.append((150, 150, 150, 255))
                continue
            face_verts_idx = mesh.faces[face_i]
            verts = mesh.vertices[face_verts_idx]
            uvs = mesh.visual.uv[face_verts_idx]
            # Barycentric
            bary = trimesh.triangles.points_to_barycentric(
                np.array([verts]), np.array([closest_pt]))[0]
            # Interp UV
            uv = bary[0] * uvs[0] + bary[1] * uvs[1] + bary[2] * uvs[2]
            tx = int(uv[0] * tex_w) % tex_w
            ty = int((1.0 - uv[1]) * tex_h) % tex_h  # Y flip
            tx = max(0, min(tex_w - 1, tx))
            ty = max(0, min(tex_h - 1, ty))
            color = tex_img.getpixel((tx, ty))
            if len(color) == 3:
                color = (*color, 255)
            colors.append(color)
        except Exception as e:
            colors.append((150, 150, 150, 255))
    return colors


def build_atlas(colors, cell_px=4):
    """Constroi PNG atlas: grid de cells, cada cell = cor solida do box."""
    n = len(colors)
    grid_n = int(np.ceil(np.sqrt(n)))
    raw_size = grid_n * cell_px
    # Round to next pow2 (min 16)
    pow2 = 16
    while pow2 < raw_size:
        pow2 *= 2
    atlas = Image.new("RGBA", (pow2, pow2), (0, 0, 0, 255))

    uvs = []  # per-box UV in 0-16 minecraft range
    for i, color in enumerate(colors):
        col = i % grid_n
        row = i // grid_n
        px0 = col * cell_px
        py0 = row * cell_px
        px1 = px0 + cell_px
        py1 = py0 + cell_px
        for dx in range(cell_px):
            for dy in range(cell_px):
                atlas.putpixel((px0 + dx, py0 + dy), color)
        # UV em 0-16 (Minecraft format)
        u0 = (px0 / pow2) * 16
        v0 = (py0 / pow2) * 16
        u1 = (px1 / pow2) * 16
        v1 = (py1 / pow2) * 16
        # Pequeno inset pra evitar bleed entre cubos vizinhos
        inset = 0.001
        uvs.append((u0 + inset, v0 + inset, u1 - inset, v1 - inset))

    return atlas, uvs, pow2


def make_bbmodel(boxes, box_uvs, resolution, name, atlas_path):
    scale = 16.0 / resolution
    elements = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = list(box_uvs[i])
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

    # Embed atlas
    with open(atlas_path, "rb") as f:
        atlas_b64 = base64.b64encode(f.read()).decode("ascii")
    textures = [{
        "path": "",
        "name": f"{name}_atlas.png",
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
        "source": f"data:image/png;base64,{atlas_b64}"
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
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "outliner": outliner,
        "textures": textures
    }


def make_minecraft_json(boxes, box_uvs, resolution, texture_ref):
    scale = 16.0 / resolution
    elements = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = list(box_uvs[i])
        elements.append({
            "from": [round(x * scale, 3), round(y * scale, 3), round(z * scale, 3)],
            "to":   [round((x + w) * scale, 3), round((y + h) * scale, 3), round((z + d) * scale, 3)],
            "faces": {
                "north": {"uv": [round(v, 3) for v in uv], "texture": "#0"},
                "east":  {"uv": [round(v, 3) for v in uv], "texture": "#0"},
                "south": {"uv": [round(v, 3) for v in uv], "texture": "#0"},
                "west":  {"uv": [round(v, 3) for v in uv], "texture": "#0"},
                "up":    {"uv": [round(v, 3) for v in uv], "texture": "#0"},
                "down":  {"uv": [round(v, 3) for v in uv], "texture": "#0"}
            }
        })
    return {
        "credit": "Fixed atlas version — per-cube color sampled from GLTF UV",
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

    print(f"[2/6] Loading texture: {args.texture}")
    tex_img = Image.open(args.texture).convert("RGBA")
    print(f"      size: {tex_img.size}")

    print(f"[3/6] Voxelizing in {args.resolution}^3...")
    grid, pitch, bounds = voxelize(mesh, args.resolution)
    n_vox = int(grid.sum())
    print(f"      voxels: {n_vox}")

    print(f"[4/6] Greedy meshing...")
    boxes = greedy_3d(grid)
    print(f"      boxes: {len(boxes)}")

    print(f"[5/6] Sampling colors per box...")
    colors = sample_box_colors(mesh, boxes, pitch, bounds[0], tex_img)
    unique_colors = len(set(colors))
    print(f"      unique colors: {unique_colors}")

    print(f"[6/6] Building atlas + bbmodel + MC JSON...")
    atlas, uvs, atlas_size = build_atlas(colors, cell_px=4)
    print(f"      atlas size: {atlas_size}x{atlas_size}")

    mod_root = args.mod_root or os.path.normpath(
        os.path.join(os.path.dirname(__file__), "..", "src/main/resources"))
    assets = os.path.join(mod_root, "assets", args.mod_id)

    # Save atlas
    atlas_path = os.path.join(assets, "textures", "block", f"{args.name}.png")
    os.makedirs(os.path.dirname(atlas_path), exist_ok=True)
    atlas.save(atlas_path)
    print(f"      [OK] atlas:  {os.path.relpath(atlas_path, mod_root)}")

    # Save Minecraft model JSON
    mc_json = make_minecraft_json(boxes, uvs, args.resolution,
                                   f"{args.mod_id}:block/{args.name}")
    mc_path = os.path.join(assets, "models", "block", f"{args.name}.json")
    os.makedirs(os.path.dirname(mc_path), exist_ok=True)
    with open(mc_path, "w") as f:
        json.dump(mc_json, f, indent=2)
    print(f"      [OK] MC JSON: {os.path.relpath(mc_path, mod_root)}")

    # Item + blockstate
    item_path = os.path.join(assets, "models", "item", f"{args.name}.json")
    os.makedirs(os.path.dirname(item_path), exist_ok=True)
    with open(item_path, "w") as f:
        json.dump({"parent": f"{args.mod_id}:block/{args.name}"}, f)
    blockstate_path = os.path.join(assets, "blockstates", f"{args.name}.json")
    os.makedirs(os.path.dirname(blockstate_path), exist_ok=True)
    with open(blockstate_path, "w") as f:
        json.dump({"variants": {"": {"model": f"{args.mod_id}:block/{args.name}"}}}, f)

    # Save bbmodel
    bb = make_bbmodel(boxes, uvs, args.resolution, args.name, atlas_path)
    bb_out = args.bbmodel_out or os.path.join(
        os.path.dirname(args.input), f"{args.name}_fixed.bbmodel")
    with open(bb_out, "w") as f:
        json.dump(bb, f, indent=2)
    print(f"      [OK] bbmodel: {bb_out}")

    print(f"")
    print(f"=== DONE === Re-abre o bbmodel no Blockbench:")
    print(f"    {bb_out}")


if __name__ == "__main__":
    main()
