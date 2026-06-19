"""v2: Multi-texture voxelizer que RESPEITA per-face qual textura original cada face usa.

Diferenca da v1:
- v1: usava 1 textura "primary" como ref pra todo o atlas (cores erradas)
- v2: pra cada submesh, anota o range de faces + textura associada
- v2: pra cada box, calcula UV NO ATLAS olhando o submesh correto

Pipeline:
1. Carrega Scene do GLTF
2. Pra cada submesh: extrai vertices, faces, UVs, textura
3. Concatena tudo em um mesh merged + mapping face_idx -> submesh_idx
4. Build atlas combinando todas texturas (cada uma com sua regiao)
5. Voxeliza
6. Pra cada box, junta faces dos voxels que ele cobre
7. Pra CADA face, mapeia UV original -> UV no atlas baseado em qual submesh ela eh
8. Box UV = bounding rect das UVs no atlas
"""
import sys, os, json, base64, uuid, argparse
from collections import defaultdict
from datetime import datetime

import trimesh
import numpy as np
from PIL import Image


def collect_submeshes_with_textures(scene_or_mesh):
    """Itera scene + extrai (mesh, texture_image, name) por submesh.

    Retorna lista de dicts: {'mesh': trimesh, 'tex_img': PIL.Image|None, 'name': str}.
    """
    items = []

    def extract_texture(geom):
        """Tenta extrair PIL Image da material PBR."""
        try:
            if not hasattr(geom, 'visual'):
                return None
            vis = geom.visual
            if hasattr(vis, 'material') and vis.material is not None:
                mat = vis.material
                # PBR baseColorTexture
                for attr_name in ['image', 'baseColorTexture']:
                    img = getattr(mat, attr_name, None)
                    if img is not None and hasattr(img, 'size'):
                        return img
        except Exception:
            pass
        return None

    if isinstance(scene_or_mesh, trimesh.Scene):
        for name, geom in scene_or_mesh.geometry.items():
            items.append({
                'mesh': geom,
                'tex_img': extract_texture(geom),
                'name': name
            })
    else:
        items.append({
            'mesh': scene_or_mesh,
            'tex_img': extract_texture(scene_or_mesh),
            'name': 'mesh'
        })

    return items


def build_atlas_with_mapping(submeshes_items, fallback_textures_dir=None):
    """Constroi atlas. Retorna (atlas_PIL, list of (offset_u, offset_v, scale_u, scale_v)
    onde indice = indice do submesh.

    Submeshes sem textura recebem region "default" (cinza) no atlas.
    """
    # Junta texturas unicas (algumas submeshes compartilham a mesma textura)
    unique_textures = {}  # id(image) -> PIL image
    texture_to_submeshes = defaultdict(list)  # id(image) -> [submesh indices]

    for i, sm in enumerate(submeshes_items):
        ti = sm['tex_img']
        if ti is None:
            continue
        tid = id(ti)
        if tid not in unique_textures:
            unique_textures[tid] = ti
        texture_to_submeshes[tid].append(i)

    # Se nenhum submesh tem textura embedded, tenta fallback da pasta
    if not unique_textures and fallback_textures_dir and os.path.isdir(fallback_textures_dir):
        print(f"      no embedded textures — using fallback dir: {fallback_textures_dir}")
        files = sorted([f for f in os.listdir(fallback_textures_dir)
                         if f.lower().endswith('.png')])
        for i, f in enumerate(files):
            try:
                img = Image.open(os.path.join(fallback_textures_dir, f)).convert("RGBA")
                # Sintetiza um id unico pelo path
                tid = f
                unique_textures[tid] = img
                # Distribui sequencialmente: cada submesh i pega textura i % len(files)
                # (fallback bruto — melhor que nada)
            except Exception as e:
                print(f"      skip {f}: {e}")
        # Rebuild texture_to_submeshes — atribui em round-robin
        keys = list(unique_textures.keys())
        for i, sm in enumerate(submeshes_items):
            if keys:
                key = keys[i % len(keys)]
                texture_to_submeshes[key].append(i)

    # Pack em atlas
    if not unique_textures:
        # Sem nenhuma textura — atlas cinza 64x64
        atlas = Image.new("RGBA", (64, 64), (180, 180, 180, 255))
        submesh_atlas_map = [(0, 0, 1, 1)] * len(submeshes_items)
        return atlas, submesh_atlas_map

    tex_list = list(unique_textures.items())  # [(tid, img), ...]
    n_unique = len(tex_list)
    grid_n = int(np.ceil(np.sqrt(n_unique)))

    max_dim = max(img.size[0] for _, img in tex_list)
    max_dim_h = max(img.size[1] for _, img in tex_list)
    cell = max(max_dim, max_dim_h)
    raw_atlas_size = grid_n * cell
    pow2 = 32
    while pow2 < raw_atlas_size:
        pow2 *= 2

    atlas = Image.new("RGBA", (pow2, pow2), (0, 0, 0, 0))

    # Cada textura unica ocupa uma regiao no atlas
    texture_regions = {}  # tid -> (u_off, v_off, u_scale, v_scale)
    for i, (tid, img) in enumerate(tex_list):
        col = i % grid_n
        row = i // grid_n
        px = col * cell
        py = row * cell
        atlas.paste(img, (px, py))
        u_off = px / pow2
        v_off = py / pow2
        u_scale = img.size[0] / pow2
        v_scale = img.size[1] / pow2
        texture_regions[tid] = (u_off, v_off, u_scale, v_scale)

    # Mapeia submesh_idx -> (offset_u, offset_v, scale_u, scale_v)
    submesh_atlas_map = []
    for i in range(len(submeshes_items)):
        # Acha qual textura esse submesh usa
        found = False
        for tid, sm_indices in texture_to_submeshes.items():
            if i in sm_indices:
                submesh_atlas_map.append(texture_regions[tid])
                found = True
                break
        if not found:
            submesh_atlas_map.append((0, 0, 1, 1))

    return atlas, submesh_atlas_map


def concatenate_with_face_tracking(submeshes_items):
    """Concatena submeshes em 1 mesh + retorna lista face_idx -> submesh_idx."""
    all_vertices = []
    all_faces = []
    all_uvs = []
    face_to_submesh = []

    v_offset = 0
    for sm_idx, sm in enumerate(submeshes_items):
        m = sm['mesh']
        verts = m.vertices
        faces = m.faces
        all_vertices.append(verts)
        # Faces precisam ter v_offset adicionado
        all_faces.append(faces + v_offset)
        # UVs
        if hasattr(m.visual, 'uv') and m.visual.uv is not None:
            uvs = m.visual.uv
        else:
            uvs = np.zeros((len(verts), 2))
        all_uvs.append(uvs)
        # Track origem das faces
        for _ in faces:
            face_to_submesh.append(sm_idx)
        v_offset += len(verts)

    vertices = np.vstack(all_vertices)
    faces = np.vstack(all_faces)
    uvs = np.vstack(all_uvs)
    return vertices, faces, uvs, np.array(face_to_submesh)


def voxelize_from_mesh(vertices, faces, resolution):
    """Voxeliza usando trimesh."""
    m = trimesh.Trimesh(vertices=vertices, faces=faces, process=False)
    bounds = m.bounds
    extent = bounds[1] - bounds[0]
    pitch = float(extent.max()) / resolution
    vox = m.voxelized(pitch=pitch).fill()
    grid = np.zeros((resolution, resolution, resolution), dtype=bool)
    mx = vox.matrix
    sx, sy, sz = mx.shape
    grid[:min(sx, resolution), :min(sy, resolution), :min(sz, resolution)] = \
        mx[:min(sx, resolution), :min(sy, resolution), :min(sz, resolution)]
    return grid, pitch, bounds


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
    ap.add_argument("--textures-dir", default=None,
                     help="Fallback dir se GLTF nao tem textura embedded")
    ap.add_argument("--name", required=True)
    ap.add_argument("--resolution", type=int, default=24)
    ap.add_argument("--mod-id", default="liberthia")
    ap.add_argument("--mod-root", default=None)
    ap.add_argument("--bbmodel-out", required=True)
    args = ap.parse_args()

    print(f"[1/8] Loading scene: {args.input}")
    scene = trimesh.load(args.input)
    if isinstance(scene, trimesh.Scene):
        print(f"      Scene com {len(scene.geometry)} submeshes")
    else:
        print(f"      Single mesh")

    print(f"[2/8] Collecting submeshes + textures...")
    submeshes = collect_submeshes_with_textures(scene)
    print(f"      {len(submeshes)} submeshes coletados")
    has_embed = sum(1 for sm in submeshes if sm['tex_img'] is not None)
    print(f"      Com textura embedded: {has_embed}/{len(submeshes)}")

    print(f"[3/8] Building atlas...")
    atlas, submesh_atlas_map = build_atlas_with_mapping(submeshes, args.textures_dir)
    print(f"      Atlas: {atlas.size[0]}x{atlas.size[1]}")
    print(f"      Submeshes mapeados ao atlas: {len(submesh_atlas_map)}")

    print(f"[4/8] Concatenating with face tracking...")
    vertices, faces, uvs, face_to_submesh = concatenate_with_face_tracking(submeshes)
    print(f"      Merged: {len(vertices)} verts, {len(faces)} faces")

    print(f"[5/8] Voxelizing in {args.resolution}^3...")
    grid, pitch, bounds = voxelize_from_mesh(vertices, faces, args.resolution)
    n_vox = int(grid.sum())
    print(f"      voxels: {n_vox}")

    print(f"[6/8] Greedy meshing...")
    boxes = greedy_3d(grid)
    print(f"      boxes: {len(boxes)}")

    print(f"[7/8] Mapping faces -> voxels...")
    triangles_center = vertices[faces].mean(axis=1)
    voxel_to_faces = defaultdict(list)
    for fi, c in enumerate(triangles_center):
        v = ((c - bounds[0]) / pitch).astype(int)
        v = np.clip(v, 0, args.resolution - 1)
        voxel_to_faces[tuple(v)].append(fi)
    print(f"      voxels com faces: {len(voxel_to_faces)}")

    print(f"[8/8] Computing per-box UVs (using submesh-specific atlas regions)...")
    box_uvs = []
    for box in boxes:
        x0, y0, z0, w, h, d = box
        face_indices = []
        for dx in range(w):
            for dy in range(h):
                for dz in range(d):
                    face_indices.extend(voxel_to_faces.get((x0+dx, y0+dy, z0+dz), []))

        # Expand pra vizinhos se vazio
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

        if not face_indices:
            box_uvs.append([8.0, 8.0, 9.0, 9.0])
            continue

        # IMPORTANT: pra cada face, transforma UV original -> atlas UV usando o submesh dela
        atlas_uvs = []
        for fi in face_indices:
            sm_idx = int(face_to_submesh[fi])
            u_off, v_off, u_scale, v_scale = submesh_atlas_map[sm_idx]
            for vi in faces[fi]:
                u, v = uvs[vi]
                # Clamp UV original em [0,1]
                u = max(0.0, min(1.0, u))
                v = max(0.0, min(1.0, v))
                # Mapeia pra atlas
                atlas_u = u_off + u * u_scale
                atlas_v = v_off + v * v_scale
                atlas_uvs.append([atlas_u, atlas_v])

        atlas_uvs = np.array(atlas_uvs)
        u_min, v_min = atlas_uvs.min(axis=0)
        u_max, v_max = atlas_uvs.max(axis=0)

        # Pad pra evitar pixel zero
        if u_max - u_min < 0.005:
            u_max = min(1.0, u_min + 0.005)
        if v_max - v_min < 0.005:
            v_max = min(1.0, v_min + 0.005)

        # Minecraft UV: 0-16 range, V flipped
        box_uvs.append([
            float(u_min * 16),
            float((1.0 - v_max) * 16),
            float(u_max * 16),
            float((1.0 - v_min) * 16),
        ])

    # === Writing files ===
    mod_root = args.mod_root or os.path.normpath(
        os.path.join(os.path.dirname(__file__), "..", "src/main/resources"))
    assets = os.path.join(mod_root, "assets", args.mod_id)
    for sub in ["textures/block", "models/block", "models/item", "blockstates"]:
        os.makedirs(os.path.join(assets, sub), exist_ok=True)

    atlas_path = os.path.join(assets, "textures", "block", f"{args.name}.png")
    atlas.save(atlas_path)
    print(f"")
    print(f"[OK] atlas: {os.path.relpath(atlas_path, mod_root)}")

    scale = 16.0 / args.resolution
    elements_mc = []
    elements_bb = []
    for i, (x, y, z, w, h, d) in enumerate(boxes):
        uv = box_uvs[i]
        mc_uv = [round(v, 3) for v in uv]
        elements_mc.append({
            "from": [round(x*scale, 3), round(y*scale, 3), round(z*scale, 3)],
            "to":   [round((x+w)*scale, 3), round((y+h)*scale, 3), round((z+d)*scale, 3)],
            "faces": {dir_: {"uv": mc_uv, "texture": "#0"} for dir_ in
                      ["north", "east", "south", "west", "up", "down"]}
        })
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

    mc_json = {
        "credit": "multi_tex_voxelize_v2 — per-face submesh UV mapping",
        "textures": {"0": f"{args.mod_id}:block/{args.name}",
                      "particle": f"{args.mod_id}:block/{args.name}"},
        "elements": elements_mc
    }
    with open(os.path.join(assets, "models", "block", f"{args.name}.json"), "w") as f:
        json.dump(mc_json, f, indent=2)
    with open(os.path.join(assets, "models", "item", f"{args.name}.json"), "w") as f:
        json.dump({"parent": f"{args.mod_id}:block/{args.name}"}, f)
    with open(os.path.join(assets, "blockstates", f"{args.name}.json"), "w") as f:
        json.dump({"variants": {"": {"model": f"{args.mod_id}:block/{args.name}"}}}, f)

    # bbmodel
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
            "namespace": args.mod_id, "id": "0", "particle": True,
            "render_mode": "default", "visible": True, "mode": "bitmap",
            "saved": False, "uuid": str(uuid.uuid4()),
            "relative_path": "", "source": f"data:image/png;base64,{atlas_b64}"
        }]
    }
    with open(args.bbmodel_out, "w") as f:
        json.dump(bb, f, indent=2)
    print(f"[OK] bbmodel: {args.bbmodel_out}")
    print(f"")
    print(f"=== DONE === Abre no Blockbench:")
    print(f"  {args.bbmodel_out}")


if __name__ == "__main__":
    main()
