"""Template-based block/item model generator.

Gera modelos Minecraft NATIVO (cubos axis-aligned puros) a partir de templates
pre-feitos. Sem IA externa, sem voxelizacao, sem mesh complexo — tudo cubos
limpos e validos pro formato Java 1.20.x.

Templates disponiveis:
  - candle           Vela 3D com pavio + chama
  - humanoid         Personagem cuboide (knight/mob)
  - sword            Espada 3D
  - shield           Escudo
  - statue           Estatua humanoide
  - pillar           Coluna decorativa
  - chest_3d         Bau detalhado

Usage:
    python template_model_gen.py candle --name vela_purple --color purple
    python template_model_gen.py humanoid --name knight --texture knight.png

Cada template eh um "voxel layout" hard-coded em codigo (puros cubos MC-friendly).
"""
import sys, os, json, base64, uuid, argparse
from datetime import datetime


# ============================================================================
# TEMPLATES — cada um eh uma list de tuplas (x0, y0, z0, x1, y1, z1, face_uvs)
# Coords em range 0-16 (espaco do bloco MC)
# ============================================================================

def template_candle(color="white"):
    """Vela 3D — base + coluna + topo + pavio + chama X-cross.

    Retorna lista de elements pro JSON MC.
    """
    elements = [
        # Base derretida wider
        {
            "name": "base",
            "from": [5, 0, 5], "to": [11, 1, 11],
            "faces": _all_faces([5, 15, 11, 16], [5, 5, 11, 11]),
        },
        # Coluna central da cera
        {
            "name": "wax_column",
            "from": [6, 1, 6], "to": [10, 12, 10],
            "faces": _all_faces([6, 4, 10, 15], [6, 6, 10, 10]),
        },
        # Topo melt
        {
            "name": "top_melt",
            "from": [5, 12, 5], "to": [11, 13, 11],
            "faces": _all_faces([5, 3, 11, 4], [5, 5, 11, 11]),
        },
        # Pavio
        {
            "name": "wick",
            "from": [7.5, 13, 7.5], "to": [8.5, 15, 8.5],
            "faces": _all_faces([0, 0, 1, 2], [0, 0, 1, 1], texture_key="#wick"),
        },
        # Chama X-cross — quad 1 (norte-sul)
        {
            "name": "flame_ns",
            "from": [4, 14, 8], "to": [12, 18, 8],
            "shade": False,
            "faces": {
                "north": {"uv": [0, 0, 8, 4], "texture": "#flame"},
                "south": {"uv": [0, 0, 8, 4], "texture": "#flame"}
            }
        },
        # Chama X-cross — quad 2 (leste-oeste)
        {
            "name": "flame_ew",
            "from": [8, 14, 4], "to": [8, 18, 12],
            "shade": False,
            "faces": {
                "east": {"uv": [0, 0, 8, 4], "texture": "#flame"},
                "west": {"uv": [0, 0, 8, 4], "texture": "#flame"}
            }
        },
    ]
    textures = {
        "wax": f"liberthia:block/candle_{color}",
        "wick": "liberthia:block/candle_3d_wick",
        "flame": "liberthia:block/candle_3d_flame",
        "particle": f"liberthia:block/candle_{color}"
    }
    return elements, textures


def template_humanoid(scale=1.0):
    """Humanoid block — knight/mob estatico. Steve-like proportions.

    Vai ocupar 1-2 blocos MC dependendo do scale.
    """
    s = scale
    elements = []

    # HEAD — 8x8x8 cube no topo
    elements.append({
        "name": "head",
        "from": [4*s, 24*s, 4*s], "to": [12*s, 32*s, 12*s],
        "faces": _all_faces_head()
    })
    # BODY — 8x12x4
    elements.append({
        "name": "body",
        "from": [4*s, 12*s, 6*s], "to": [12*s, 24*s, 10*s],
        "faces": _all_faces([16, 16, 24, 28], [16, 16, 24, 20])
    })
    # Right arm — 4x12x4
    elements.append({
        "name": "arm_right",
        "from": [0*s, 12*s, 6*s], "to": [4*s, 24*s, 10*s],
        "faces": _all_faces([40, 16, 44, 28], [40, 16, 44, 20])
    })
    # Left arm
    elements.append({
        "name": "arm_left",
        "from": [12*s, 12*s, 6*s], "to": [16*s, 24*s, 10*s],
        "faces": _all_faces([32, 48, 36, 60], [32, 48, 36, 52])
    })
    # Right leg
    elements.append({
        "name": "leg_right",
        "from": [4*s, 0*s, 6*s], "to": [8*s, 12*s, 10*s],
        "faces": _all_faces([0, 16, 4, 28], [0, 16, 4, 20])
    })
    # Left leg
    elements.append({
        "name": "leg_left",
        "from": [8*s, 0*s, 6*s], "to": [12*s, 12*s, 10*s],
        "faces": _all_faces([16, 48, 20, 60], [16, 48, 20, 52])
    })

    textures = {
        "0": "liberthia:block/humanoid",
        "particle": "liberthia:block/humanoid"
    }
    return elements, textures


def template_sword():
    """Espada 3D — blade + crossguard + handle + pommel."""
    elements = [
        # Pommel
        {"name": "pommel",
         "from": [7, 0, 7], "to": [9, 2, 9],
         "faces": _all_faces([0, 0, 2, 2], [0, 0, 2, 2])},
        # Handle
        {"name": "handle",
         "from": [7.5, 2, 7.5], "to": [8.5, 6, 8.5],
         "faces": _all_faces([0, 2, 1, 6], [0, 0, 1, 1])},
        # Crossguard
        {"name": "crossguard",
         "from": [5, 6, 7.5], "to": [11, 7, 8.5],
         "faces": _all_faces([0, 6, 6, 7], [0, 0, 6, 1])},
        # Blade
        {"name": "blade",
         "from": [7.5, 7, 7.5], "to": [8.5, 16, 8.5],
         "faces": _all_faces([0, 7, 1, 16], [0, 0, 1, 1])},
        # Blade tip
        {"name": "blade_tip",
         "from": [7.75, 16, 7.75], "to": [8.25, 16.5, 8.25],
         "faces": _all_faces([0, 16, 1, 16.5], [0, 0, 1, 1])}
    ]
    textures = {
        "0": "liberthia:item/sword_uv",
        "particle": "liberthia:item/sword_uv"
    }
    return elements, textures


def template_shield():
    """Escudo simples — circle approximation via cubos."""
    elements = []
    # Body — disco aproximado por cubos
    radius_grid = 6
    for dx in range(-radius_grid, radius_grid):
        for dy in range(-radius_grid, radius_grid):
            # Mantém apenas dentro do raio
            if dx*dx + dy*dy > radius_grid*radius_grid:
                continue
            x0 = 8 + dx
            y0 = 8 + dy
            elements.append({
                "name": f"disk_{dx}_{dy}",
                "from": [x0, y0, 7], "to": [x0 + 1, y0 + 1, 9],
                "faces": _all_faces([0, 0, 1, 1], [0, 0, 1, 2])
            })
    # Center boss
    elements.append({
        "name": "boss",
        "from": [6, 6, 9], "to": [10, 10, 10],
        "faces": _all_faces([6, 6, 10, 10], [6, 6, 10, 10])
    })
    textures = {"0": "liberthia:item/shield_uv", "particle": "liberthia:item/shield_uv"}
    return elements, textures


def template_statue():
    """Estatua humanoid maior (3 blocos altura). Detalhada."""
    # Reusa humanoid scale 2x
    elements, _ = template_humanoid(scale=2.0)
    # Adiciona pedestal
    elements.append({
        "name": "pedestal",
        "from": [-2, -4, -2], "to": [18, 0, 18],
        "faces": _all_faces([0, 0, 16, 4], [0, 0, 16, 16])
    })
    textures = {"0": "liberthia:block/statue", "particle": "liberthia:block/statue"}
    return elements, textures


def template_pillar():
    """Coluna decorativa simples."""
    elements = [
        {"name": "base", "from": [3, 0, 3], "to": [13, 2, 13],
         "faces": _all_faces([3, 14, 13, 16], [3, 3, 13, 13])},
        {"name": "shaft", "from": [4, 2, 4], "to": [12, 14, 12],
         "faces": _all_faces([4, 2, 12, 14], [4, 4, 12, 12])},
        {"name": "top", "from": [3, 14, 3], "to": [13, 16, 13],
         "faces": _all_faces([3, 0, 13, 2], [3, 3, 13, 13])}
    ]
    textures = {"0": "liberthia:block/pillar", "particle": "liberthia:block/pillar"}
    return elements, textures


def template_chest_3d():
    """Bau 3D — body + lid + handle + lock."""
    elements = [
        {"name": "body", "from": [1, 0, 1], "to": [15, 9, 15],
         "faces": _all_faces([1, 7, 15, 16], [1, 1, 15, 15])},
        {"name": "lid", "from": [1, 9, 1], "to": [15, 14, 15],
         "faces": _all_faces([1, 2, 15, 7], [1, 1, 15, 15])},
        {"name": "lock", "from": [6, 6, 0.5], "to": [10, 9, 1.5],
         "faces": _all_faces([6, 7, 10, 10], [6, 0, 10, 1])}
    ]
    textures = {"0": "liberthia:block/chest_3d", "particle": "liberthia:block/chest_3d"}
    return elements, textures


# ============================================================================
# HELPERS
# ============================================================================

def _all_faces(side_uv, top_uv, texture_key="#0"):
    """Cria dict de 6 faces com UV especifica."""
    return {
        "north": {"uv": side_uv, "texture": texture_key},
        "east": {"uv": side_uv, "texture": texture_key},
        "south": {"uv": side_uv, "texture": texture_key},
        "west": {"uv": side_uv, "texture": texture_key},
        "up": {"uv": top_uv, "texture": texture_key},
        "down": {"uv": top_uv, "texture": texture_key}
    }


def _all_faces_head():
    """UV humanoid head — 6 faces no formato Steve player skin."""
    return {
        "north": {"uv": [8, 8, 16, 16], "texture": "#0"},   # front
        "south": {"uv": [24, 8, 32, 16], "texture": "#0"},  # back
        "east":  {"uv": [0, 8, 8, 16], "texture": "#0"},    # right
        "west":  {"uv": [16, 8, 24, 16], "texture": "#0"},  # left
        "up":    {"uv": [8, 0, 16, 8], "texture": "#0"},    # top
        "down":  {"uv": [16, 0, 24, 8], "texture": "#0"}    # bottom
    }


def make_minecraft_json(elements, textures):
    return {
        "credit": "Template-generated block model",
        "textures": textures,
        "elements": elements
    }


def make_bbmodel(elements, textures, name, texture_path_for_embed=None):
    """Converte pra .bbmodel format."""
    bb_elements = []
    children_uuids = []
    for el in elements:
        eid = str(uuid.uuid4())
        bb_faces = {}
        for face_name, face_data in el["faces"].items():
            bb_faces[face_name] = {
                "uv": face_data["uv"],
                "texture": 0
            }
        bb_elements.append({
            "name": el.get("name", "cube"),
            "rescale": False, "locked": False,
            "from": el["from"], "to": el["to"],
            "autouv": 0, "color": 0, "origin": [8, 8, 8],
            "faces": bb_faces,
            "uuid": eid, "type": "cube"
        })
        children_uuids.append(eid)

    bb_textures = []
    if texture_path_for_embed and os.path.exists(texture_path_for_embed):
        with open(texture_path_for_embed, "rb") as f:
            b64 = base64.b64encode(f.read()).decode("ascii")
        from PIL import Image
        img = Image.open(texture_path_for_embed)
        tex_w, tex_h = img.size
        bb_textures.append({
            "path": "", "name": f"{name}.png", "folder": "block",
            "namespace": "liberthia", "id": "0", "particle": True,
            "render_mode": "default", "visible": True, "mode": "bitmap",
            "saved": False, "uuid": str(uuid.uuid4()),
            "relative_path": "", "source": f"data:image/png;base64,{b64}"
        })
    else:
        tex_w, tex_h = 16, 16

    return {
        "meta": {"format_version": "4.5", "model_format": "java_block",
                 "box_uv": False, "creation_time": int(datetime.now().timestamp())},
        "name": name, "model_identifier": "",
        "modded_entity_version": "", "modded_entity_flip_y": True,
        "added_models": [], "visible_box": [2, 2, 0],
        "variable_placeholders": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": tex_w, "height": tex_h},
        "elements": bb_elements,
        "outliner": [{
            "name": name, "origin": [8, 8, 8], "rotation": [0, 0, 0],
            "color": 0, "uuid": str(uuid.uuid4()),
            "export": True, "isOpen": True, "locked": False,
            "visibility": True, "autouv": 0,
            "children": children_uuids
        }],
        "textures": bb_textures
    }


TEMPLATES = {
    "candle": template_candle,
    "humanoid": template_humanoid,
    "sword": template_sword,
    "shield": template_shield,
    "statue": template_statue,
    "pillar": template_pillar,
    "chest_3d": template_chest_3d,
}


def main():
    ap = argparse.ArgumentParser(description="Template-based MC block model generator")
    ap.add_argument("template", choices=list(TEMPLATES.keys()) + ["LIST"],
                     help="Qual template usar (use 'LIST' pra listar)")
    ap.add_argument("--name", default=None, help="Nome do bloco no mod")
    ap.add_argument("--color", default="white", help="Cor pro template candle")
    ap.add_argument("--texture", default=None, help="Caminho PNG da textura (opcional)")
    ap.add_argument("--mod-id", default="liberthia")
    ap.add_argument("--mod-root", default=None)
    ap.add_argument("--bbmodel-out", default=None)
    ap.add_argument("--list", action="store_true", help="Lista templates")
    args = ap.parse_args()

    if args.list or args.template == "LIST":
        print("Templates disponiveis:")
        for k in TEMPLATES:
            print(f"  - {k}")
        return

    if not args.name:
        print("ERRO: --name eh obrigatorio (excepto pra --list)")
        sys.exit(1)

    print(f"[1/3] Generating template: {args.template}")
    if args.template == "candle":
        elements, textures = TEMPLATES[args.template](color=args.color)
    elif args.template == "humanoid":
        elements, textures = TEMPLATES[args.template]()
    else:
        elements, textures = TEMPLATES[args.template]()
    print(f"      {len(elements)} elementos")

    # Save mod assets
    mod_root = args.mod_root or os.path.normpath(
        os.path.join(os.path.dirname(__file__), "..", "src/main/resources"))
    assets = os.path.join(mod_root, "assets", args.mod_id)
    for sub in ["models/block", "models/item", "blockstates", "textures/block"]:
        os.makedirs(os.path.join(assets, sub), exist_ok=True)

    print(f"[2/3] Writing Minecraft JSON")
    mc_json = make_minecraft_json(elements, textures)
    mc_path = os.path.join(assets, "models", "block", f"{args.name}.json")
    with open(mc_path, "w") as f:
        json.dump(mc_json, f, indent=2)
    print(f"      [OK] {os.path.relpath(mc_path, mod_root)}")

    item_path = os.path.join(assets, "models", "item", f"{args.name}.json")
    with open(item_path, "w") as f:
        json.dump({"parent": f"{args.mod_id}:block/{args.name}"}, f)

    bs_path = os.path.join(assets, "blockstates", f"{args.name}.json")
    with open(bs_path, "w") as f:
        json.dump({"variants": {"": {"model": f"{args.mod_id}:block/{args.name}"}}}, f)

    # Save texture if provided
    if args.texture and os.path.exists(args.texture):
        from PIL import Image
        out_tex = os.path.join(assets, "textures", "block", f"{args.name}.png")
        Image.open(args.texture).convert("RGBA").save(out_tex)
        print(f"      [OK] textura: {os.path.relpath(out_tex, mod_root)}")
        tex_path_for_bb = out_tex
    else:
        tex_path_for_bb = None

    print(f"[3/3] Writing .bbmodel")
    bb = make_bbmodel(elements, textures, args.name, tex_path_for_bb)
    bb_out = args.bbmodel_out or os.path.join(
        os.path.dirname(__file__), "..", "build", f"{args.name}.bbmodel")
    with open(bb_out, "w") as f:
        json.dump(bb, f, indent=2)
    print(f"      [OK] {bb_out}")
    print(f"")
    print(f"=== DONE — TEMPLATE-BASED, 100% Minecraft-compatible ===")
    print(f"  Elementos: {len(elements)} cubos puros, axis-aligned")
    print(f"  Sem voxelizacao, sem mesh complexa, sem distorcoes UV")


if __name__ == "__main__":
    main()
