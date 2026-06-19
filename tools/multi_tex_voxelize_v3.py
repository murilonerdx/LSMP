"""v3: Preserve a HIERARQUIA do GLTF E aplica texturas corretas por submesh.

Resultado:
- bbmodel com tree intacta (badan_atas, kaki_kiri, etc.)
- N texturas SEPARADAS (uma por submesh group, nao atlas)
- Cada cubo aponta pra SUA textura nominal

Voxeliza CADA submesh individualmente (preservando posicao no espaco mundial),
depois agrupa por submesh original no outliner.
"""
import sys, os, json, base64, uuid, argparse
from collections import defaultdict
from datetime import datetime

import trimesh
import numpy as np
from PIL import Image


def collect_submeshes(scene_or_mesh):
    items = []

    def get_tex(geom):
        try:
            vis = geom.visual
            if hasattr(vis, 'material') and vis.material is not None:
                mat = vis.material
                for attr in ['image', 'baseColorTexture']:
                    img = getattr(mat, attr, None)
                    if img is not None and hasattr(img, 'size'):
                        return img
        except Exception:
            pass
        return None

    if isinstance(scene_or_mesh, trimesh.Scene):
        for name, geom in scene_or_mesh.geometry.items():
            items.append({'mesh': geom, 'tex': get_tex(geom), 'name': name})
    else:
        items.append({'mesh': scene_or_mesh, 'tex': get_tex(scene_or_mesh), 'name': 'root'})
    return items


def voxelize_submesh(submesh, global_bounds_min, pitch, resolution):
    """Voxeliza um submesh, mantendo posicao no grid mundial unificado."""
    m = submesh
    try:
        vox = m.voxelized(pitch=pitch).fill()
    except Exception:
        return np.zeros((resolution, resolution, resolution), dtype=bool)
    mx = vox.matrix
    # Origem do voxelization eh em m.bounds[0], precisa offsetar pro grid mundial
    sm_min = m.bounds[0]
    offset = ((sm_min - global_bounds_min) / pitch).astype(int)

    # Cria grid no espaco mundial
    grid = np.zeros((resolution, resolution, resolution), dtype=bool)
    ox, oy, oz = offset
    sx, sy, sz = mx.shape
    # Place mx into global grid at offset
    ex = min(resolution, ox + sx) - max(0, ox)
    ey = min(resolution, oy + sy) - max(0, oy)
    ez = min(resolution, oz + sz) - max(0, oz)
    if ex <= 0 or ey <= 0 or ez <= 0:
        return grid

    src_ox = max(0, -ox)
    src_oy = max(0, -oy)
    src_oz = max(0, -oz)
    dst_ox = max(0, ox)
    dst_oy = max(0, oy)
    dst_oz = max(0, oz)

    grid[dst_ox:dst_ox+ex, dst_oy:dst_oy+ey, dst_oz:dst_oz+ez] = \
        mx[src_ox:src_ox+ex, src_oy:src_oy+ey, src_oz:src_oz+ez]
    return grid


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


def compute_submesh_box_uvs(submesh_mesh, boxes, pitch, global_bounds_min, resolution, tex_size):
    """Pra cada box voxelizado desse submesh, encontra a UV bounding rect
    nas UVs originais do submesh (que ja apontam pra textura especifica dele).
    """
    if not (hasattr(submesh_mesh.visual, 'uv') and submesh_mesh.visual.uv is not None):
        return [[0, 0, 16, 16]] * len(boxes)
    uvs = submesh_mesh.visual.uv

    # Map faces -> voxel (no grid global)
    voxel_to_faces = defaultdict(list)
    centers = submesh_mesh.triangles_center
    for fi, c in enumerate(centers):
        v = ((c - global_bounds_min) / pitch).astype(int)
        v = np.clip(v, 0, resolution - 1)
        voxel_to_faces[tuple(v)].append(fi)

    box_uvs = []
    for box in boxes:
        x0, y0, z0, w, h, d = box
        face_indices = []
        for dx in range(w):
            for dy in range(h):
                for dz in range(d):
                    face_indices.extend(voxel_to_faces.get((x0+dx, y0+dy, z0+dz), []))

        # Expand vizinhos
        if not face_indices:
            cx, cy, cz = x0+w/2, y0+h/2, z0+d/2
            for r in range(1, max(resolution//4, 4)):
                for dx in range(-r, r+1):
                    for dy in range(-r, r+1):
                        for dz in range(-r, r+1):
                            if max(abs(dx), abs(dy), abs(dz)) != r:
                                continue
                            nx, ny, nz = int(cx+dx), int(cy+dy), int(cz+dz)
                            if 0 <= nx < resolution and 0 <= ny < resolution and 0 <= nz < resolution:
                                face_indices.extend(voxel_to_faces.get((nx, ny, nz), []))
                if face_indices:
                    break

        if not face_indices:
            box_uvs.append([8.0, 8.0, 9.0, 9.0])
            continue

        # Coleta UVs no espaco 0-1
        all_uv = []
        for fi in face_indices:
            for vi in submesh_mesh.faces[fi]:
                all_uv.append(uvs[vi])
        all_uv = np.array(all_uv)
        u_min, v_min = all_uv.min(axis=0)
        u_max, v_max = all_uv.max(axis=0)
        if u_max - u_min < 0.005:
            u_max = min(1.0, u_min + 0.005)
        if v_max - v_min < 0.005:
            v_max = min(1.0, v_min + 0.005)

        # IMPORTANT: cada submesh ja tem sua textura propria — UV vai DIRETO pra ela
        # Minecraft UV: 0-tex_size range, V flipped
        # Em bbmodel: usa o range da resolucao do bbmodel (resolution: {width, height})
        # Pra simplicidade: usa range 0-16 (padrao MC)
        box_uvs.append([
            float(u_min * 16),
            float((1.0 - v_max) * 16),
            float(u_max * 16),
            float((1.0 - v_min) * 16),
        ])
    return box_uvs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input")
    ap.add_argument("--textures-dir", default=None)
    ap.add_argument("--name", required=True)
    ap.add_argument("--resolution", type=int, default=32)
    ap.add_argument("--bbmodel-out", required=True)
    args = ap.parse_args()

    print(f"[1/6] Loading scene: {args.input}")
    scene = trimesh.load(args.input)
    submeshes = collect_submeshes(scene)
    n_sub = len(submeshes)
    n_with_tex = sum(1 for s in submeshes if s['tex'] is not None)
    print(f"      Submeshes: {n_sub} (com textura embedded: {n_with_tex})")

    # Compute global bounds (combining all submeshes)
    print(f"[2/6] Computing global bounds...")
    all_verts = np.vstack([s['mesh'].vertices for s in submeshes])
    global_min = all_verts.min(axis=0)
    global_max = all_verts.max(axis=0)
    global_extent = global_max - global_min
    pitch = float(global_extent.max()) / args.resolution
    print(f"      bounds: {global_min} -> {global_max}")
    print(f"      pitch: {pitch:.4f}")

    # Load fallback textures se nao tem embedded
    fallback_textures = []
    if n_with_tex == 0 and args.textures_dir and os.path.isdir(args.textures_dir):
        print(f"[3/6] Loading fallback textures from {args.textures_dir}")
        files = sorted([f for f in os.listdir(args.textures_dir)
                         if f.lower().endswith('.png')])
        for f in files:
            try:
                img = Image.open(os.path.join(args.textures_dir, f)).convert("RGBA")
                fallback_textures.append((f, img))
            except Exception:
                pass
        print(f"      {len(fallback_textures)} fallback textures carregadas")

    # Build bbmodel: cada submesh = group separado com sua textura
    print(f"[4/6] Voxelizing each submesh...")
    bb_elements = []
    bb_groups = []
    bb_textures = []
    texture_id_map = {}  # nome_textura -> bbmodel texture id
    next_tex_id = 0

    scale = 16.0 / args.resolution
    total_boxes = 0

    for sm_idx, sm in enumerate(submeshes):
        sm_mesh = sm['mesh']
        sm_name = sm['name'][:50].replace(' ', '_')
        # Pegar textura (embedded ou fallback)
        tex_img = sm['tex']
        if tex_img is None and fallback_textures:
            tex_img = fallback_textures[sm_idx % len(fallback_textures)][1]
        if tex_img is None:
            # Cria textura cinza default
            tex_img = Image.new("RGBA", (16, 16), (180, 180, 180, 255))

        # ID da textura (reusa se ja foi adicionada)
        tex_key = id(tex_img)
        if tex_key not in texture_id_map:
            texture_id_map[tex_key] = next_tex_id
            # Adiciona ao bbmodel
            import io
            buf = io.BytesIO()
            tex_img.save(buf, format='PNG')
            b64 = base64.b64encode(buf.getvalue()).decode("ascii")
            bb_textures.append({
                "path": "", "name": f"{sm_name}_tex.png", "folder": "block",
                "namespace": "liberthia", "id": str(next_tex_id), "particle": next_tex_id == 0,
                "render_mode": "default", "visible": True, "mode": "bitmap",
                "saved": False, "uuid": str(uuid.uuid4()),
                "relative_path": "", "source": f"data:image/png;base64,{b64}"
            })
            next_tex_id += 1
        tex_id = texture_id_map[tex_key]
        tex_w, tex_h = tex_img.size

        # Voxeliza esse submesh
        grid = voxelize_submesh(sm_mesh, global_min, pitch, args.resolution)
        if not grid.any():
            continue
        boxes = greedy_3d(grid)
        if not boxes:
            continue
        total_boxes += len(boxes)

        # UVs por box (relativos a textura desse submesh)
        box_uvs = compute_submesh_box_uvs(sm_mesh, boxes, pitch, global_min,
                                            args.resolution, (tex_w, tex_h))

        # Cria elements bbmodel pra esse submesh
        group_children_uuids = []
        for i, (x, y, z, w, h, d) in enumerate(boxes):
            uv = box_uvs[i]
            eid = str(uuid.uuid4())
            bb_elements.append({
                "name": f"{sm_name}_cube_{i}", "rescale": False, "locked": False,
                "from": [x*scale, y*scale, z*scale],
                "to":   [(x+w)*scale, (y+h)*scale, (z+d)*scale],
                "autouv": 0, "color": 0, "origin": [8, 8, 8],
                "faces": {dir_: {"uv": uv, "texture": tex_id} for dir_ in
                          ["north", "east", "south", "west", "up", "down"]},
                "uuid": eid, "type": "cube"
            })
            group_children_uuids.append(eid)

        # Group pro submesh
        bb_groups.append({
            "name": sm_name, "origin": [8, 8, 8], "rotation": [0, 0, 0],
            "color": 0, "uuid": str(uuid.uuid4()),
            "export": True, "isOpen": False, "locked": False,
            "visibility": True, "autouv": 0,
            "children": group_children_uuids
        })

    print(f"      {total_boxes} cubos totais em {len(bb_groups)} grupos")

    print(f"[5/6] Building bbmodel structure...")
    # Root group containing all submesh groups
    root_group = {
        "name": args.name, "origin": [8, 8, 8], "rotation": [0, 0, 0],
        "color": 0, "uuid": str(uuid.uuid4()),
        "export": True, "isOpen": True, "locked": False,
        "visibility": True, "autouv": 0,
        "children": bb_groups
    }

    # Pegar resolucao da textura primaria (primeira)
    if bb_textures:
        # Decode primeira textura pra pegar size
        first_tex_src = bb_textures[0]['source'].replace("data:image/png;base64,", "")
        first_tex_bytes = base64.b64decode(first_tex_src)
        import io
        first_tex_img = Image.open(io.BytesIO(first_tex_bytes))
        prim_w, prim_h = first_tex_img.size
    else:
        prim_w, prim_h = 16, 16

    bb = {
        "meta": {"format_version": "4.5", "model_format": "java_block",
                 "box_uv": False, "creation_time": int(datetime.now().timestamp())},
        "name": args.name, "model_identifier": "",
        "modded_entity_version": "", "modded_entity_flip_y": True,
        "added_models": [], "visible_box": [4, 4, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": prim_w, "height": prim_h},
        "elements": bb_elements,
        "outliner": [root_group],
        "textures": bb_textures
    }

    print(f"[6/6] Writing bbmodel: {args.bbmodel_out}")
    with open(args.bbmodel_out, "w", encoding="utf-8") as f:
        json.dump(bb, f, indent=2)
    sz = os.path.getsize(args.bbmodel_out)
    print(f"      [OK] {sz//1024} KB")
    print(f"")
    print(f"=== DONE === Abre no Blockbench:")
    print(f"  {args.bbmodel_out}")
    print(f"")
    print(f"Hierarquia preservada com {len(bb_groups)} grupos + {len(bb_textures)} texturas separadas.")


if __name__ == "__main__":
    main()
