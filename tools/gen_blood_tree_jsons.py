"""
Gera todos os JSONs (blockstates + models de bloco/item) pra o Blood Tree
family, copiando os patterns dos sanguine_* existentes.

Outputs:
- blockstates/{blood_log, stripped_blood_log, blood_planks, blood_leaves,
               blood_sapling, blood_stairs, blood_slab, blood_fence,
               blood_fence_gate, blood_door, blood_trapdoor}.json
- models/block/* (vários por block, ex: stairs tem 3, door tem 8, etc)
- models/item/* (1 por block)
"""
import json
import os

ROOT = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia"
BS = os.path.join(ROOT, "blockstates")
BM = os.path.join(ROOT, "models", "block")
IM = os.path.join(ROOT, "models", "item")
os.makedirs(BS, exist_ok=True)
os.makedirs(BM, exist_ok=True)
os.makedirs(IM, exist_ok=True)

def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"OK  {path}")

# ============================================================
# 1. LOG (vertical + horizontal)
# ============================================================
write_json(os.path.join(BS, "blood_log.json"), {
    "variants": {
        "axis=y": {"model": "liberthia:block/blood_log"},
        "axis=x": {"model": "liberthia:block/blood_log_horizontal", "x": 90, "y": 90},
        "axis=z": {"model": "liberthia:block/blood_log_horizontal", "x": 90}
    }
})
write_json(os.path.join(BM, "blood_log.json"), {
    "parent": "minecraft:block/cube_column",
    "textures": {"side": "liberthia:block/blood_log", "end": "liberthia:block/blood_log_top"}
})
write_json(os.path.join(BM, "blood_log_horizontal.json"), {
    "parent": "minecraft:block/cube_column_horizontal",
    "textures": {"side": "liberthia:block/blood_log", "end": "liberthia:block/blood_log_top"}
})
write_json(os.path.join(IM, "blood_log.json"), {"parent": "liberthia:block/blood_log"})

# Stripped log
write_json(os.path.join(BS, "stripped_blood_log.json"), {
    "variants": {
        "axis=y": {"model": "liberthia:block/stripped_blood_log"},
        "axis=x": {"model": "liberthia:block/stripped_blood_log_horizontal", "x": 90, "y": 90},
        "axis=z": {"model": "liberthia:block/stripped_blood_log_horizontal", "x": 90}
    }
})
write_json(os.path.join(BM, "stripped_blood_log.json"), {
    "parent": "minecraft:block/cube_column",
    "textures": {"side": "liberthia:block/stripped_blood_log", "end": "liberthia:block/stripped_blood_log_top"}
})
write_json(os.path.join(BM, "stripped_blood_log_horizontal.json"), {
    "parent": "minecraft:block/cube_column_horizontal",
    "textures": {"side": "liberthia:block/stripped_blood_log", "end": "liberthia:block/stripped_blood_log_top"}
})
write_json(os.path.join(IM, "stripped_blood_log.json"), {"parent": "liberthia:block/stripped_blood_log"})

# ============================================================
# 2. PLANKS
# ============================================================
write_json(os.path.join(BS, "blood_planks.json"), {
    "variants": {"": {"model": "liberthia:block/blood_planks"}}
})
write_json(os.path.join(BM, "blood_planks.json"), {
    "parent": "minecraft:block/cube_all",
    "textures": {"all": "liberthia:block/blood_planks"}
})
write_json(os.path.join(IM, "blood_planks.json"), {"parent": "liberthia:block/blood_planks"})

# ============================================================
# 3. LEAVES
# ============================================================
write_json(os.path.join(BS, "blood_leaves.json"), {
    "variants": {"": {"model": "liberthia:block/blood_leaves"}}
})
write_json(os.path.join(BM, "blood_leaves.json"), {
    "parent": "minecraft:block/leaves",
    "textures": {"all": "liberthia:block/blood_leaves"}
})
write_json(os.path.join(IM, "blood_leaves.json"), {"parent": "liberthia:block/blood_leaves"})

# ============================================================
# 4. SAPLING
# ============================================================
write_json(os.path.join(BS, "blood_sapling.json"), {
    "variants": {"": {"model": "liberthia:block/blood_sapling"}}
})
write_json(os.path.join(BM, "blood_sapling.json"), {
    "parent": "minecraft:block/cross",
    "textures": {"cross": "liberthia:block/blood_sapling"}
})
write_json(os.path.join(IM, "blood_sapling.json"), {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "liberthia:block/blood_sapling"}
})

# ============================================================
# 5. STAIRS
# ============================================================
write_json(os.path.join(BM, "blood_stairs.json"), {
    "parent": "minecraft:block/stairs",
    "textures": {"bottom": "liberthia:block/blood_planks", "top": "liberthia:block/blood_planks",
                 "side": "liberthia:block/blood_planks"}
})
write_json(os.path.join(BM, "blood_stairs_inner.json"), {
    "parent": "minecraft:block/inner_stairs",
    "textures": {"bottom": "liberthia:block/blood_planks", "top": "liberthia:block/blood_planks",
                 "side": "liberthia:block/blood_planks"}
})
write_json(os.path.join(BM, "blood_stairs_outer.json"), {
    "parent": "minecraft:block/outer_stairs",
    "textures": {"bottom": "liberthia:block/blood_planks", "top": "liberthia:block/blood_planks",
                 "side": "liberthia:block/blood_planks"}
})
write_json(os.path.join(IM, "blood_stairs.json"), {"parent": "liberthia:block/blood_stairs"})

# Stairs blockstate — 4 facings × 2 halves × 5 shapes = 40 variants
stair_variants = {}
# Helper pra adicionar variants stairs vanilla padrão
# Padrão derivado direto do sanguine_stairs.json (32 variants principais)
STAIR_TABLE = [
    # (facing, half, shape, model_suffix, x, y)
    ("north", "bottom", "straight",    "",      None, 270),
    ("north", "bottom", "inner_left",  "inner", None, 180),
    ("north", "bottom", "inner_right", "inner", None, 270),
    ("north", "bottom", "outer_left",  "outer", None, 180),
    ("north", "bottom", "outer_right", "outer", None, 270),
    ("north", "top",    "straight",    "",      180,  270),
    ("north", "top",    "inner_left",  "inner", 180,  270),
    ("north", "top",    "inner_right", "inner", 180,  180),
    ("north", "top",    "outer_left",  "outer", 180,  270),
    ("north", "top",    "outer_right", "outer", 180,  180),
    ("east",  "bottom", "straight",    "",      None, None),
    ("east",  "bottom", "inner_left",  "inner", None, 270),
    ("east",  "bottom", "inner_right", "inner", None, None),
    ("east",  "bottom", "outer_left",  "outer", None, 270),
    ("east",  "bottom", "outer_right", "outer", None, None),
    ("east",  "top",    "straight",    "",      180,  None),
    ("east",  "top",    "inner_left",  "inner", 180,  None),
    ("east",  "top",    "inner_right", "inner", 180,  270),
    ("east",  "top",    "outer_left",  "outer", 180,  None),
    ("east",  "top",    "outer_right", "outer", 180,  270),
    ("south", "bottom", "straight",    "",      None, 90),
    ("south", "bottom", "inner_left",  "inner", None, None),
    ("south", "bottom", "inner_right", "inner", None, 90),
    ("south", "bottom", "outer_left",  "outer", None, None),
    ("south", "bottom", "outer_right", "outer", None, 90),
    ("south", "top",    "straight",    "",      180,  90),
    ("south", "top",    "inner_left",  "inner", 180,  90),
    ("south", "top",    "inner_right", "inner", 180,  None),
    ("south", "top",    "outer_left",  "outer", 180,  90),
    ("south", "top",    "outer_right", "outer", 180,  None),
    ("west",  "bottom", "straight",    "",      None, 180),
    ("west",  "bottom", "inner_left",  "inner", None, 90),
    ("west",  "bottom", "inner_right", "inner", None, 180),
    ("west",  "bottom", "outer_left",  "outer", None, 90),
    ("west",  "bottom", "outer_right", "outer", None, 180),
    ("west",  "top",    "straight",    "",      180,  180),
    ("west",  "top",    "inner_left",  "inner", 180,  180),
    ("west",  "top",    "inner_right", "inner", 180,  90),
    ("west",  "top",    "outer_left",  "outer", 180,  180),
    ("west",  "top",    "outer_right", "outer", 180,  90),
]
for facing, half, shape, suffix, x, y in STAIR_TABLE:
    key = f"facing={facing},half={half},shape={shape}"
    model = "liberthia:block/blood_stairs" + (f"_{suffix}" if suffix else "")
    entry = {"model": model}
    if x is not None: entry["x"] = x
    if y is not None: entry["y"] = y
    if x is not None or y is not None or shape != "straight":
        entry["uvlock"] = True
    # straight north sem nada de rotation foi não-uvlock no sanguine, mas ele tinha y=270 sempre. OK manter uvlock
    stair_variants[key] = entry

write_json(os.path.join(BS, "blood_stairs.json"), {"variants": stair_variants})

# ============================================================
# 6. SLAB
# ============================================================
write_json(os.path.join(BS, "blood_slab.json"), {
    "variants": {
        "type=bottom": {"model": "liberthia:block/blood_slab"},
        "type=top": {"model": "liberthia:block/blood_slab_top"},
        "type=double": {"model": "liberthia:block/blood_planks"}
    }
})
write_json(os.path.join(BM, "blood_slab.json"), {
    "parent": "minecraft:block/slab",
    "textures": {"bottom": "liberthia:block/blood_planks", "top": "liberthia:block/blood_planks",
                 "side": "liberthia:block/blood_planks"}
})
write_json(os.path.join(BM, "blood_slab_top.json"), {
    "parent": "minecraft:block/slab_top",
    "textures": {"bottom": "liberthia:block/blood_planks", "top": "liberthia:block/blood_planks",
                 "side": "liberthia:block/blood_planks"}
})
write_json(os.path.join(IM, "blood_slab.json"), {"parent": "liberthia:block/blood_slab"})

# ============================================================
# 7. FENCE
# ============================================================
write_json(os.path.join(BS, "blood_fence.json"), {
    "multipart": [
        {"apply": {"model": "liberthia:block/blood_fence_post"}},
        {"when": {"north": "true"}, "apply": {"model": "liberthia:block/blood_fence_side", "uvlock": True}},
        {"when": {"east": "true"},  "apply": {"model": "liberthia:block/blood_fence_side", "y": 90,  "uvlock": True}},
        {"when": {"south": "true"}, "apply": {"model": "liberthia:block/blood_fence_side", "y": 180, "uvlock": True}},
        {"when": {"west": "true"},  "apply": {"model": "liberthia:block/blood_fence_side", "y": 270, "uvlock": True}}
    ]
})
write_json(os.path.join(BM, "blood_fence_post.json"), {
    "parent": "minecraft:block/fence_post",
    "textures": {"texture": "liberthia:block/blood_planks"}
})
write_json(os.path.join(BM, "blood_fence_side.json"), {
    "parent": "minecraft:block/fence_side",
    "textures": {"texture": "liberthia:block/blood_planks"}
})
write_json(os.path.join(BM, "blood_fence_inventory.json"), {
    "parent": "minecraft:block/fence_inventory",
    "textures": {"texture": "liberthia:block/blood_planks"}
})
write_json(os.path.join(IM, "blood_fence.json"), {"parent": "liberthia:block/blood_fence_inventory"})

# ============================================================
# 8. FENCE GATE
# ============================================================
gate_variants = {}
for facing, y in [("south", None), ("west", 90), ("north", 180), ("east", 270)]:
    for in_wall in [False, True]:
        for open_ in [False, True]:
            suffix = ""
            if in_wall: suffix += "_wall"
            if open_: suffix += "_open"
            model = "liberthia:block/blood_fence_gate" + suffix
            key = f"facing={facing},in_wall={'true' if in_wall else 'false'},open={'true' if open_ else 'false'}"
            entry = {"model": model, "uvlock": True}
            if y is not None: entry["y"] = y
            gate_variants[key] = entry
write_json(os.path.join(BS, "blood_fence_gate.json"), {"variants": gate_variants})
for tmpl in ["template_fence_gate", "template_fence_gate_open", "template_fence_gate_wall", "template_fence_gate_wall_open"]:
    model_name = tmpl.replace("template_", "")
    write_json(os.path.join(BM, f"blood_{model_name}.json"), {
        "parent": f"minecraft:block/{tmpl}",
        "textures": {"texture": "liberthia:block/blood_planks"}
    })
write_json(os.path.join(IM, "blood_fence_gate.json"), {"parent": "liberthia:block/blood_fence_gate"})

# ============================================================
# 9. DOOR
# ============================================================
# Door blockstate (4 facings × 2 halves × 2 hinges × 2 powered × 2 open = 64 vars)
door_variants = {}
FACINGS = [("east", 0), ("south", 90), ("west", 180), ("north", 270)]
HALVES = ["lower", "upper"]
HINGES = ["left", "right"]
for facing, base_y in FACINGS:
    for half in HALVES:
        for hinge in HINGES:
            for powered in ["true", "false"]:
                for open_ in ["true", "false"]:
                    # Modelo: bottom_left, bottom_right, top_left, top_right (+ _open variants)
                    half_word = "bottom" if half == "lower" else "top"
                    model = f"liberthia:block/blood_door_{half_word}_{hinge}"
                    extra_y = 0
                    if open_ == "true":
                        model += "_open"
                        # Open rotates: derived from sanguine pattern
                        if hinge == "left":
                            extra_y = 90
                        else:  # right
                            extra_y = 270
                    y = (base_y + extra_y) % 360
                    key = f"facing={facing},half={half},hinge={hinge},open={open_},powered={powered}"
                    entry = {"model": model}
                    if y != 0: entry["y"] = y
                    door_variants[key] = entry
write_json(os.path.join(BS, "blood_door.json"), {"variants": door_variants})

# Door models — 8 variantes (bottom/top × left/right × closed/open)
DOOR_PARTS = [
    ("bottom_left",       "block/door_bottom_left"),
    ("bottom_left_open",  "block/door_bottom_left_open"),
    ("bottom_right",      "block/door_bottom_right"),
    ("bottom_right_open", "block/door_bottom_right_open"),
    ("top_left",          "block/door_top_left"),
    ("top_left_open",     "block/door_top_left_open"),
    ("top_right",         "block/door_top_right"),
    ("top_right_open",    "block/door_top_right_open"),
]
for suffix, parent in DOOR_PARTS:
    write_json(os.path.join(BM, f"blood_door_{suffix}.json"), {
        "parent": f"minecraft:{parent}",
        "textures": {
            "top": "liberthia:block/blood_door_top",
            "bottom": "liberthia:block/blood_door_bottom"
        }
    })
write_json(os.path.join(IM, "blood_door.json"), {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "liberthia:item/blood_door"}
})

# ============================================================
# 10. TRAPDOOR
# ============================================================
trap_variants = {}
FACINGS_T = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
for facing, base_y in FACINGS_T:
    for half in ["bottom", "top"]:
        for open_ in ["true", "false"]:
            for powered in ["true", "false"]:
                if open_ == "false":
                    if half == "bottom":
                        model = "liberthia:block/blood_trapdoor_bottom"
                        x = None
                        y = base_y if base_y != 0 else None
                    else:  # top
                        model = "liberthia:block/blood_trapdoor_top"
                        x = None
                        y = base_y if base_y != 0 else None
                else:  # open
                    model = "liberthia:block/blood_trapdoor_open"
                    if half == "bottom":
                        x = None
                        y = base_y if base_y != 0 else None
                    else:  # top open — derived from sanguine: x=180, y=base+180 % 360
                        x = 180
                        y_val = (base_y + 180) % 360
                        y = y_val if y_val != 0 else None
                key = f"facing={facing},half={half},open={open_},powered={powered}"
                entry = {"model": model}
                if y is not None: entry["y"] = y
                if x is not None: entry["x"] = x
                trap_variants[key] = entry
write_json(os.path.join(BS, "blood_trapdoor.json"), {"variants": trap_variants})

for suffix, parent in [
    ("bottom", "block/template_orientable_trapdoor_bottom"),
    ("top",    "block/template_orientable_trapdoor_top"),
    ("open",   "block/template_orientable_trapdoor_open"),
]:
    write_json(os.path.join(BM, f"blood_trapdoor_{suffix}.json"), {
        "parent": f"minecraft:{parent}",
        "textures": {"texture": "liberthia:block/blood_trapdoor"}
    })
write_json(os.path.join(IM, "blood_trapdoor.json"), {"parent": "liberthia:block/blood_trapdoor_bottom"})

print("\nAll JSONs written.")
