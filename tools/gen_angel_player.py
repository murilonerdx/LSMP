"""Gera modelo de Anjo player-shaped pra Blockbench.

Modelo:
  - Base player Steve (head 8x8x8, body 8x12x4, arms 4x12x4, legs 4x12x4)
  - + Aureola (anel dourado acima da cabeca)
  - + 2 asas grandes (multi-layer com gradient)
  - + Detalhes douradoss (peitoral armor angelica)
  - + Glow points (olhos brilhantes)

Texturas:
  - Skin 64x64 com tons azul celeste + dourado + branco

Outputs:
  - angel_player.bbmodel    pronto pra Blockbench
  - angel_skin.png          texture 64x64 player skin
"""
import os
import json
import uuid
import base64
import math
from datetime import datetime

from PIL import Image, ImageDraw

OUT_DIR = os.path.dirname(__file__)
OUT_BBMODEL = os.path.join(OUT_DIR, "angel_player.bbmodel")
OUT_SKIN = os.path.join(OUT_DIR, "angel_skin.png")


def make_skin_texture():
    """Cria skin 64x64 - layout Steve. Tons celestes + dourados."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Cores do anjo
    SKIN = (220, 240, 255, 255)        # celeste claro
    SKIN_DARK = (180, 210, 240, 255)
    SKIN_HL = (240, 250, 255, 255)
    BODY = (130, 200, 255, 255)         # azul angelical
    BODY_DARK = (90, 160, 220, 255)
    BODY_HL = (170, 230, 255, 255)
    GOLD = (255, 215, 90, 255)
    GOLD_DARK = (200, 160, 50, 255)
    GOLD_HL = (255, 240, 180, 255)
    EYE = (255, 255, 255, 255)
    EYE_GLOW = (180, 230, 255, 255)
    BOOT = (60, 80, 140, 255)            # bota escura

    # === HEAD === (8x8 textures, 6 faces no layout Steve)
    # Front face: (8,8)-(16,16)
    d.rectangle([8, 8, 15, 15], fill=SKIN)
    # Eyes (2 pixels each, glowing)
    d.rectangle([9, 11, 10, 12], fill=EYE_GLOW)
    d.rectangle([13, 11, 14, 12], fill=EYE_GLOW)
    d.point((9, 11), fill=EYE)
    d.point((14, 12), fill=EYE)
    # Mouth (subtle smile)
    d.line([(10, 14), (13, 14)], fill=SKIN_DARK)
    # Cheek highlights
    d.point((10, 13), fill=SKIN_HL)
    d.point((13, 13), fill=SKIN_HL)
    # Halo glow on forehead
    d.line([(11, 9), (12, 9)], fill=GOLD_HL)

    # Right side: (0,8)-(8,16)
    d.rectangle([0, 8, 7, 15], fill=SKIN)
    d.rectangle([1, 8, 6, 15], fill=SKIN_DARK)
    # Left side: (16,8)-(24,16)
    d.rectangle([16, 8, 23, 15], fill=SKIN)
    d.rectangle([17, 8, 22, 15], fill=SKIN_DARK)
    # Back: (24,8)-(32,16)
    d.rectangle([24, 8, 31, 15], fill=SKIN)
    # Top: (8,0)-(16,8) — hair/halo glow
    d.rectangle([8, 0, 15, 7], fill=SKIN_HL)
    # Add gold streak on top (suggests halo shadow)
    d.line([(10, 0), (13, 0)], fill=GOLD)
    d.line([(9, 1), (14, 1)], fill=GOLD_HL)
    # Bottom (chin): (16,0)-(24,8)
    d.rectangle([16, 0, 23, 7], fill=SKIN_DARK)

    # === BODY === UV (16,16)-(40,32)
    # Front: (20,20)-(28,32)
    d.rectangle([20, 20, 27, 31], fill=BODY)
    # Highlight stripe top
    for x in range(20, 28):
        d.point((x, 20), fill=BODY_HL)
    # Center "amulet" gold
    d.rectangle([23, 22, 24, 24], fill=GOLD)
    d.point((23, 22), fill=GOLD_HL)
    # Angelic chest cross/V
    d.line([(22, 26), (25, 26)], fill=GOLD_DARK)
    d.line([(22, 27), (23, 28)], fill=GOLD_DARK)
    d.line([(25, 27), (24, 28)], fill=GOLD_DARK)
    # Side
    d.rectangle([16, 20, 19, 31], fill=BODY_DARK)
    # Right side
    d.rectangle([28, 20, 31, 31], fill=BODY_DARK)
    # Back: (32,20)-(40,32)
    d.rectangle([32, 20, 39, 31], fill=BODY)
    # Wings attachment point on back (gold detail)
    d.point((34, 24), fill=GOLD)
    d.point((37, 24), fill=GOLD)
    # Top body face
    d.rectangle([20, 16, 27, 19], fill=BODY_HL)
    # Bottom body face
    d.rectangle([28, 16, 35, 19], fill=BODY_DARK)

    # === RIGHT ARM === (40,16)-(48,32)
    # Front (44,20)-(48,32)
    d.rectangle([44, 20, 47, 31], fill=BODY)
    # Arm sides
    d.rectangle([40, 20, 43, 31], fill=BODY_DARK)
    d.rectangle([48, 20, 51, 31], fill=BODY_DARK)
    # Back
    d.rectangle([52, 20, 55, 31], fill=BODY)
    # Top
    d.rectangle([44, 16, 47, 19], fill=BODY_HL)
    # Bottom (wrist) — skin
    d.rectangle([48, 16, 51, 19], fill=SKIN)
    # Hand (last 3 rows of front face)
    d.rectangle([44, 29, 47, 31], fill=SKIN)
    # Gold bracelet
    d.line([(44, 28), (47, 28)], fill=GOLD)

    # === LEFT ARM === (32,48)-(48,64) - Steve format
    d.rectangle([36, 52, 39, 63], fill=BODY)  # front
    d.rectangle([32, 52, 35, 63], fill=BODY_DARK)  # right
    d.rectangle([40, 52, 43, 63], fill=BODY_DARK)  # left
    d.rectangle([44, 52, 47, 63], fill=BODY)  # back
    d.rectangle([36, 48, 39, 51], fill=BODY_HL)  # top
    d.rectangle([40, 48, 43, 51], fill=SKIN)  # bottom (wrist)
    d.rectangle([36, 61, 39, 63], fill=SKIN)  # hand
    d.line([(36, 60), (39, 60)], fill=GOLD)

    # === RIGHT LEG === (0,16)-(16,32)
    d.rectangle([4, 20, 7, 31], fill=BODY_DARK)
    d.rectangle([0, 20, 3, 31], fill=BODY_DARK)
    d.rectangle([8, 20, 11, 31], fill=BODY_DARK)
    d.rectangle([12, 20, 15, 31], fill=BODY_DARK)
    d.rectangle([4, 16, 7, 19], fill=BODY)  # top
    d.rectangle([8, 16, 11, 19], fill=BOOT)  # bottom (boot)
    # Boot (last 3 rows)
    d.rectangle([4, 29, 7, 31], fill=BOOT)
    d.rectangle([0, 29, 3, 31], fill=BOOT)
    d.rectangle([8, 29, 11, 31], fill=BOOT)
    d.rectangle([12, 29, 15, 31], fill=BOOT)
    # Gold boot trim
    d.line([(4, 28), (7, 28)], fill=GOLD)

    # === LEFT LEG === (16,48)-(32,64)
    d.rectangle([20, 52, 23, 63], fill=BODY_DARK)
    d.rectangle([16, 52, 19, 63], fill=BODY_DARK)
    d.rectangle([24, 52, 27, 63], fill=BODY_DARK)
    d.rectangle([28, 52, 31, 63], fill=BODY_DARK)
    d.rectangle([20, 48, 23, 51], fill=BODY)
    d.rectangle([24, 48, 27, 51], fill=BOOT)
    d.rectangle([20, 61, 23, 63], fill=BOOT)
    d.rectangle([16, 61, 19, 63], fill=BOOT)
    d.rectangle([24, 61, 27, 63], fill=BOOT)
    d.rectangle([28, 61, 31, 63], fill=BOOT)
    d.line([(20, 60), (23, 60)], fill=GOLD)

    # === EXTRA AREAS PARA AUREOLA E ASAS ===
    # Halo texture region (top-right corner): (48,0) onwards - small ring
    # 16x4 region for halo
    d.rectangle([48, 0, 63, 3], fill=GOLD_DARK)
    d.rectangle([49, 0, 62, 2], fill=GOLD)
    d.rectangle([50, 0, 61, 1], fill=GOLD_HL)

    # === WING TEXTURES === (48,4)-(64,32) - large wing region
    # Wing layer 1 — outer (white-gold feathers)
    d.rectangle([48, 4, 63, 31], fill=(240, 240, 255, 255))
    # Feather lines
    for y in range(5, 31, 3):
        d.line([(49, y), (62, y)], fill=GOLD_HL)
    # Tip darker
    d.line([(60, 5), (62, 30)], fill=GOLD)

    img.save(OUT_SKIN)
    print(f"[OK] Skin texture saved: {OUT_SKIN}")
    return OUT_SKIN


def _cube(name, frm, to, uv_origin=None, origin=None):
    """Cria estrutura de cubo bbmodel.

    uv_origin: (u, v) origin no atlas (formato Steve box_uv mode).
    """
    cube_uuid = str(uuid.uuid4())
    return {
        "name": name,
        "rescale": False,
        "locked": False,
        "from": frm,
        "to": to,
        "autouv": 0,
        "color": 0,
        "origin": origin or [0, 0, 0],
        "uv_offset": uv_origin or [0, 0],
        "box_uv": True,
        "uuid": cube_uuid,
        "type": "cube"
    }


def _group(name, children_uuids, origin=None, color=0):
    """Bone/group bbmodel."""
    return {
        "name": name,
        "origin": origin or [0, 0, 0],
        "rotation": [0, 0, 0],
        "color": color,
        "uuid": str(uuid.uuid4()),
        "export": True,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": children_uuids
    }


def make_bbmodel(skin_path):
    """Cria bbmodel completo player + halo + wings."""
    elements = []

    # === HEAD === 8x8x8 cube
    head_cube = _cube("head", [-4, 24, -4], [4, 32, 4],
                       uv_origin=[0, 0], origin=[0, 24, 0])
    elements.append(head_cube)

    # === BODY === 8x12x4
    body_cube = _cube("body", [-4, 12, -2], [4, 24, 2],
                       uv_origin=[16, 16], origin=[0, 24, 0])
    elements.append(body_cube)

    # === RIGHT ARM === 4x12x4
    rarm_cube = _cube("right_arm", [-8, 12, -2], [-4, 24, 2],
                       uv_origin=[40, 16], origin=[-5, 22, 0])
    elements.append(rarm_cube)

    # === LEFT ARM === 4x12x4
    larm_cube = _cube("left_arm", [4, 12, -2], [8, 24, 2],
                       uv_origin=[32, 48], origin=[5, 22, 0])
    elements.append(larm_cube)

    # === RIGHT LEG === 4x12x4
    rleg_cube = _cube("right_leg", [-4, 0, -2], [0, 12, 2],
                       uv_origin=[0, 16], origin=[-2, 12, 0])
    elements.append(rleg_cube)

    # === LEFT LEG === 4x12x4
    lleg_cube = _cube("left_leg", [0, 0, -2], [4, 12, 2],
                       uv_origin=[16, 48], origin=[2, 12, 0])
    elements.append(lleg_cube)

    # === HALO === ring acima da cabeca (8 cubos formando anel)
    halo_cubes = []
    halo_radius = 5
    halo_y = 35  # acima da head (que termina em y=32)
    halo_segments = 12
    for i in range(halo_segments):
        ang = (i / halo_segments) * 2 * math.pi
        cx = halo_radius * math.cos(ang)
        cz = halo_radius * math.sin(ang)
        # Cube pequeno na posicao (0.6 unidades)
        cube_size = 0.7
        c = _cube(f"halo_seg_{i}",
                  [cx - cube_size/2, halo_y - 0.3, cz - cube_size/2],
                  [cx + cube_size/2, halo_y + 0.3, cz + cube_size/2],
                  uv_origin=[48, 0], origin=[0, 24, 0])
        elements.append(c)
        halo_cubes.append(c["uuid"])

    # === WINGS === 2 asas grandes (cada uma com 3 layers de cubos feathered)
    wing_cubes_right = []
    wing_cubes_left = []

    # Right wing — cresce pra direita+atras
    # Camada 1: base attached ao body
    for i, (x_off, y_off, z_off, w, h, d_size) in enumerate([
        # base attachment
        (-8, 18, 0, 4, 6, 2),
        # mid section
        (-12, 14, 1, 6, 10, 2),
        # outer feathers
        (-18, 10, 2, 8, 14, 2),
        # tip
        (-23, 6, 2, 6, 16, 2),
    ]):
        c = _cube(f"right_wing_{i}",
                  [x_off, y_off, z_off],
                  [x_off + w, y_off + h, z_off + d_size],
                  uv_origin=[48, 4 + i*5], origin=[-4, 22, 1])
        elements.append(c)
        wing_cubes_right.append(c["uuid"])

    # Left wing — espelhado
    for i, (x_off, y_off, z_off, w, h, d_size) in enumerate([
        (4, 18, 0, 4, 6, 2),
        (6, 14, 1, 6, 10, 2),
        (10, 10, 2, 8, 14, 2),
        (17, 6, 2, 6, 16, 2),
    ]):
        c = _cube(f"left_wing_{i}",
                  [x_off, y_off, z_off],
                  [x_off + w, y_off + h, z_off + d_size],
                  uv_origin=[48, 4 + i*5], origin=[4, 22, 1])
        elements.append(c)
        wing_cubes_left.append(c["uuid"])

    # === BUILD GROUPS (hierarchy) ===
    # Halo group
    halo_group = _group("Halo", halo_cubes, origin=[0, 24, 0])
    # Wings groups
    right_wing_group = _group("Right_Wing", wing_cubes_right, origin=[-4, 22, 1])
    left_wing_group = _group("Left_Wing", wing_cubes_left, origin=[4, 22, 1])

    # Body group contains body cube + wings
    body_group = _group("body", [body_cube["uuid"], right_wing_group["uuid"], left_wing_group["uuid"]],
                         origin=[0, 24, 0])

    # Head group contains head + halo
    head_group = _group("head", [head_cube["uuid"], halo_group["uuid"]],
                         origin=[0, 24, 0])

    # Arm/leg groups
    rarm_group = _group("right_arm", [rarm_cube["uuid"]], origin=[-5, 22, 0])
    larm_group = _group("left_arm", [larm_cube["uuid"]], origin=[5, 22, 0])
    rleg_group = _group("right_leg", [rleg_cube["uuid"]], origin=[-2, 12, 0])
    lleg_group = _group("left_leg", [lleg_cube["uuid"]], origin=[2, 12, 0])

    outliner = [
        head_group,
        body_group,
        rarm_group,
        larm_group,
        rleg_group,
        lleg_group,
    ]

    # Embed texture
    with open(skin_path, "rb") as f:
        b64 = base64.b64encode(f.read()).decode("ascii")
    textures = [{
        "path": "",
        "name": "skin",
        "folder": "",
        "namespace": "",
        "id": "0",
        "particle": True,
        "render_mode": "default",
        "visible": True,
        "mode": "bitmap",
        "saved": False,
        "uuid": str(uuid.uuid4()),
        "relative_path": "",
        "source": f"data:image/png;base64,{b64}"
    }]

    bbmodel = {
        "meta": {
            "format_version": "4.5",
            "model_format": "skin",     # player skin format
            "box_uv": True,
            "creation_time": int(datetime.now().timestamp())
        },
        "name": "AngelPlayer",
        "model_identifier": "",
        "modded_entity_version": "",
        "modded_entity_flip_y": True,
        "added_models": [],
        "visible_box": [3, 4, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "skin_model": "steve",
        "skin_slim": False,
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "outliner": outliner,
        "textures": textures
    }

    with open(OUT_BBMODEL, "w", encoding="utf-8") as f:
        json.dump(bbmodel, f, indent=2)
    print(f"[OK] bbmodel saved: {OUT_BBMODEL}")


def main():
    print("=" * 60)
    print("ANGEL PLAYER MODEL GENERATOR")
    print("=" * 60)
    print("\n[1/2] Generating skin texture...")
    skin = make_skin_texture()
    print(f"\n[2/2] Generating .bbmodel...")
    make_bbmodel(skin)
    print(f"\n=== DONE ===")
    print(f"\nOutputs:")
    print(f"  - {OUT_BBMODEL}")
    print(f"  - {OUT_SKIN}")
    print(f"\nCONTEM:")
    print(f"  * Steve player base (head/body/arms/legs)")
    print(f"  * Halo dourado (12 cubos formando anel)")
    print(f"  * 2 asas grandes (4 cubos cada, feather layers)")
    print(f"  * Skin azul-celeste + detalhes dourados")
    print(f"\nABRE NO BLOCKBENCH:")
    print(f"  1. Blockbench > File > Open Model")
    print(f"  2. Selecionar: {OUT_BBMODEL}")
    print(f"  3. Vai abrir como projeto Skin (player customization)")


if __name__ == "__main__":
    main()
