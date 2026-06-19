"""
r164: Generate 3D blockbench-style item models for armor pieces.

The current armor models use `parent: minecraft:item/generated` with empty
`children: []`, which results in a flat 2D sprite in the inventory. This
script replaces them with real 3D models containing `elements` (cubes) so
the inventory icon shows a chunky helmet/chestplate/leggings/boots silhouette.

Generates models for all matter+blood+order+sanguine armor sets:
- helmet:    8x8x8 cube at head position
- chestplate: torso block 8x12x4 with shoulder bumps
- leggings:  2 leg cuboids 4x10x4
- boots:     2 foot cuboids 4x4x6
"""

import json
import os

# Each set has 4 pieces (helmet/chestplate/leggings/boots)
ARMOR_SETS = [
    "dark_matter",
    "clear_matter",
    "yellow_matter",
    "blood",
    "order",
    "sanguine",
    "sanguine_ward",
    "containment_suit",
]
PIECES = ["helmet", "chestplate", "leggings", "boots"]

ROOT = os.path.join("src", "main", "resources", "assets", "liberthia", "models", "item")


def all_faces(uv, tex="#layer0"):
    """Generate a faces dict where every face uses the same UV+texture."""
    return {
        "north": {"uv": uv, "texture": tex},
        "south": {"uv": uv, "texture": tex},
        "east":  {"uv": uv, "texture": tex},
        "west":  {"uv": uv, "texture": tex},
        "up":    {"uv": uv, "texture": tex},
        "down":  {"uv": uv, "texture": tex},
    }


def make_helmet():
    """3D head-shaped helmet — base + crown + visor strip."""
    return [
        # Base of helmet (covers head)
        {"from": [4, 4, 4], "to": [12, 12, 12],
         "faces": all_faces([0, 0, 16, 16])},
        # Crown / top plate (slight elevation)
        {"from": [5, 12, 5], "to": [11, 13, 11],
         "faces": all_faces([2, 0, 14, 4])},
        # Visor / face plate
        {"from": [4, 5, 3], "to": [12, 9, 4],
         "faces": all_faces([0, 7, 16, 11])},
    ]


def make_chestplate():
    """3D torso-shaped chestplate — body + shoulders + collar."""
    return [
        # Main torso plate (front+back, 4 deep)
        {"from": [4, 2, 6], "to": [12, 14, 10],
         "faces": all_faces([0, 0, 16, 16])},
        # Left shoulder pauldron
        {"from": [2, 11, 6], "to": [4, 14, 10],
         "faces": all_faces([0, 0, 4, 6])},
        # Right shoulder pauldron
        {"from": [12, 11, 6], "to": [14, 14, 10],
         "faces": all_faces([0, 0, 4, 6])},
        # Collar/neck ring on top
        {"from": [6, 14, 7], "to": [10, 15, 9],
         "faces": all_faces([6, 0, 10, 2])},
    ]


def make_leggings():
    """3D leggings — 2 leg cuboids + waist belt."""
    return [
        # Waist belt
        {"from": [4, 12, 6], "to": [12, 14, 10],
         "faces": all_faces([0, 0, 16, 4])},
        # Left leg
        {"from": [4, 2, 6], "to": [8, 12, 10],
         "faces": all_faces([0, 4, 8, 16])},
        # Right leg
        {"from": [8, 2, 6], "to": [12, 12, 10],
         "faces": all_faces([8, 4, 16, 16])},
    ]


def make_boots():
    """3D boots — 2 foot cuboids with ankle cuffs."""
    return [
        # Left boot foot
        {"from": [3, 0, 5], "to": [8, 3, 11],
         "faces": all_faces([0, 12, 10, 16])},
        # Right boot foot
        {"from": [8, 0, 5], "to": [13, 3, 11],
         "faces": all_faces([0, 12, 10, 16])},
        # Left ankle cuff
        {"from": [4, 3, 6], "to": [8, 6, 10],
         "faces": all_faces([0, 9, 8, 12])},
        # Right ankle cuff
        {"from": [8, 3, 6], "to": [12, 6, 10],
         "faces": all_faces([8, 9, 16, 12])},
    ]


PIECE_ELEMENTS = {
    "helmet":     make_helmet,
    "chestplate": make_chestplate,
    "leggings":   make_leggings,
    "boots":      make_boots,
}

# Display transforms — tuned so the 3D model looks good in GUI/hand/ground/head
DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 90, 0], "translation": [0, 4, 0], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {
        "rotation": [0, 270, 0], "translation": [0, 4, 0], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {
        "rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {
        "rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {
        "rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {
        "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [1.1, 1.1, 1.1]},
    "fixed": {
        "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "head": {
        "rotation": [0, 0, 0], "translation": [0, 13, 0], "scale": [1.0, 1.0, 1.0]},
}


def gen_model(set_name: str, piece: str) -> dict:
    """Build a complete blockbench-style model dict."""
    tex_path = f"liberthia:item/{set_name}_{piece}"
    return {
        "_comment": f"r164 3D armor — {set_name} {piece}",
        "credit": "Liberthia r164 procedural 3D armor",
        "parent": "minecraft:block/block",
        "textures": {
            "layer0":   tex_path,
            "particle": tex_path,
        },
        "elements": PIECE_ELEMENTS[piece](),
        "display": DISPLAY,
    }


def main():
    written = 0
    for set_name in ARMOR_SETS:
        for piece in PIECES:
            path = os.path.join(ROOT, f"{set_name}_{piece}.json")
            if not os.path.exists(path):
                # Skip pieces that don't exist for this set
                continue
            model = gen_model(set_name, piece)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(model, f, indent=2)
            written += 1
            print(f"  [ok] {set_name}_{piece}")
    # Veinbound only has chestplate
    vb_path = os.path.join(ROOT, "veinbound_chestplate.json")
    if os.path.exists(vb_path):
        with open(vb_path, "w", encoding="utf-8") as f:
            json.dump(gen_model("veinbound", "chestplate"), f, indent=2)
        written += 1
        print("  [ok] veinbound_chestplate")
    print(f"\nGenerated {written} 3D armor models.")


if __name__ == "__main__":
    main()
