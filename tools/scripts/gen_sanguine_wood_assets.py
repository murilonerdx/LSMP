"""Generate the entire Sanguine Wood asset family + Possessed mob spawn eggs.

Outputs:
  - 7 textures (PIL): sanguine_log, sanguine_log_top, stripped_sanguine_log,
                      stripped_sanguine_log_top, sanguine_planks,
                      sanguine_leaves, sanguine_sapling, sanguine_door_top,
                      sanguine_door_bottom, sanguine_trapdoor
  - blockstate + block_model + item_model JSONs for all 15 wood blocks
  - 2 item models for the door (different — uses 2D layer0 sprite)
"""
import os, json, random
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "liberthia")
TEX_BLOCK = os.path.join(BASE, "textures", "block")
TEX_ITEM  = os.path.join(BASE, "textures", "item")
BLOCKSTATES = os.path.join(BASE, "blockstates")
MODELS_BLOCK = os.path.join(BASE, "models", "block")
MODELS_ITEM = os.path.join(BASE, "models", "item")
for d in (TEX_BLOCK, TEX_ITEM, BLOCKSTATES, MODELS_BLOCK, MODELS_ITEM):
    os.makedirs(d, exist_ok=True)
S = 16
NS = "liberthia"

def write(p, obj):
    with open(p, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)

def save_tex(name, img, kind="block"):
    target = TEX_BLOCK if kind == "block" else TEX_ITEM
    img.save(os.path.join(target, name + ".png"))

# =====================================================================
# 1. TEXTURES
# =====================================================================

def log_side(seed):
    """Vertical bark — vertical streaks, dark red base."""
    img = Image.new("RGBA", (S, S), (90, 25, 30, 255))
    d = ImageDraw.Draw(img)
    random.seed(seed)
    for x in range(S):
        for y in range(S):
            v = random.randint(50, 110)
            r = max(0, min(255, v + 30))
            g = max(0, min(255, v // 4))
            b = max(0, min(255, v // 4))
            d.point((x, y), fill=(r, g, b, 255))
    # vertical bark streaks
    for x in [2, 7, 11, 14]:
        for y in range(S):
            d.point((x, y), fill=(40, 5, 10, 255))
    return img

def log_top(seed):
    """Cross-section rings."""
    img = Image.new("RGBA", (S, S), (110, 30, 35, 255))
    d = ImageDraw.Draw(img)
    random.seed(seed)
    cx, cy = 8, 8
    for r in range(1, 9):
        col = (130 - r * 8, 25 + r * 3, 25 + r * 3, 255)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=col)
    # noise
    for _ in range(40):
        x, y = random.randint(0, 15), random.randint(0, 15)
        v = random.randint(50, 90)
        d.point((x, y), fill=(v + 30, v // 4, v // 4, 255))
    return img

def stripped_side(seed):
    img = Image.new("RGBA", (S, S), (170, 70, 60, 255))
    d = ImageDraw.Draw(img)
    random.seed(seed)
    for x in range(S):
        for y in range(S):
            v = random.randint(120, 200)
            r = max(0, min(255, v + 20))
            g = max(0, min(255, v // 2))
            b = max(0, min(255, v // 3))
            d.point((x, y), fill=(r, g, b, 255))
    return img

def stripped_top(seed):
    img = Image.new("RGBA", (S, S), (180, 80, 70, 255))
    d = ImageDraw.Draw(img)
    random.seed(seed)
    for r in range(1, 9):
        col = (200 - r * 5, 80 + r * 4, 60 + r * 4, 255)
        d.ellipse([8 - r, 8 - r, 8 + r, 8 + r], outline=col)
    return img

def planks(seed):
    img = Image.new("RGBA", (S, S), (140, 50, 45, 255))
    d = ImageDraw.Draw(img)
    random.seed(seed)
    for x in range(S):
        for y in range(S):
            v = random.randint(80, 160)
            r = max(0, min(255, v + 30))
            g = max(0, min(255, v // 3))
            b = max(0, min(255, v // 3))
            d.point((x, y), fill=(r, g, b, 255))
    # plank seams
    for y in [3, 11]:
        for x in range(S):
            d.point((x, y), fill=(40, 5, 10, 255))
    for x in [7, 8]:
        for y in range(0, 4):
            d.point((x, y), fill=(40, 5, 10, 255))
        for y in range(11, 16):
            d.point((x, y), fill=(40, 5, 10, 255))
    return img

def leaves(seed):
    img = Image.new("RGBA", (S, S), (140, 30, 50, 255))
    d = ImageDraw.Draw(img)
    random.seed(seed)
    for x in range(S):
        for y in range(S):
            r = random.randint(110, 180)
            g = random.randint(10, 60)
            b = random.randint(20, 70)
            d.point((x, y), fill=(r, g, b, 255))
    # darker veins
    for _ in range(30):
        x, y = random.randint(0, 15), random.randint(0, 15)
        d.point((x, y), fill=(40, 5, 10, 255))
    return img

def sapling():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # stem
    d.line([7, 14, 8, 5], fill=(80, 30, 25, 255), width=1)
    d.line([8, 14, 9, 5], fill=(60, 20, 15, 255))
    # leaves cluster
    d.ellipse([4, 1, 11, 7], fill=(140, 30, 50, 255), outline=(70, 10, 20, 255))
    d.point((7, 3), fill=(200, 60, 80, 255))
    d.point((9, 4), fill=(200, 60, 80, 255))
    return img

def door_bottom(seed):
    img = planks(seed)
    d = ImageDraw.Draw(img)
    # handle dot on right side mid
    d.ellipse([12, 7, 14, 9], fill=(220, 200, 80, 255), outline=(120, 90, 20, 255))
    return img

def door_top(seed):
    img = planks(seed + 1)
    d = ImageDraw.Draw(img)
    # window slot
    d.rectangle([5, 4, 10, 9], fill=(40, 5, 10, 255), outline=(20, 0, 5, 255))
    d.line([7, 4, 7, 9], fill=(80, 20, 25, 255))
    return img

def trapdoor(seed):
    img = planks(seed)
    d = ImageDraw.Draw(img)
    # iron studs corners
    for cx, cy in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(180, 180, 180, 255))
    return img

save_tex("sanguine_log",       log_side(1))
save_tex("sanguine_log_top",   log_top(2))
save_tex("sanguine_wood",      log_side(1))     # same texture as log side
save_tex("stripped_sanguine_log",     stripped_side(3))
save_tex("stripped_sanguine_log_top", stripped_top(4))
save_tex("stripped_sanguine_wood",    stripped_side(3))
save_tex("sanguine_planks",    planks(5))
save_tex("sanguine_leaves",    leaves(6))
save_tex("sanguine_sapling",   sapling())
save_tex("sanguine_door_top",    door_top(7))
save_tex("sanguine_door_bottom", door_bottom(8))
save_tex("sanguine_trapdoor",    trapdoor(9))

# Door 2D inventory icon (item layer)
door_item = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(door_item)
# tall door silhouette
d.rectangle([5, 1, 11, 14], fill=(140, 50, 45, 255), outline=(40, 5, 10, 255))
d.rectangle([6, 3, 10, 6], fill=(40, 5, 10, 255))
d.point((9, 9), fill=(220, 200, 80, 255))
door_item.save(os.path.join(TEX_ITEM, "sanguine_door.png"))

# =====================================================================
# 2. BLOCKSTATES + MODELS
# =====================================================================

# helper to write blockstate
def bs(name, variants):
    write(os.path.join(BLOCKSTATES, name + ".json"), {"variants": variants})

# helper to write block model
def bm(name, parent, textures):
    write(os.path.join(MODELS_BLOCK, name + ".json"),
          {"parent": parent, "textures": textures})

# helper to write item model with parent
def im_parent(name, parent):
    write(os.path.join(MODELS_ITEM, name + ".json"), {"parent": parent})

# helper for handheld-style item
def im_layer(name, layer0):
    write(os.path.join(MODELS_ITEM, name + ".json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": layer0}})

# ---- LOG / WOOD / STRIPPED ----
def pillar(name, side, end, axis_aware_top_eq_side=False):
    bm(name, "minecraft:block/cube_column", {"side": side, "end": end})
    bm(name + "_horizontal", "minecraft:block/cube_column_horizontal", {"side": side, "end": end})
    bs(name, {
        "axis=y": {"model": f"{NS}:block/{name}"},
        "axis=x": {"model": f"{NS}:block/{name}_horizontal", "x": 90, "y": 90},
        "axis=z": {"model": f"{NS}:block/{name}_horizontal", "x": 90}
    })
    im_parent(name, f"{NS}:block/{name}")

pillar("sanguine_log",   side=f"{NS}:block/sanguine_log",   end=f"{NS}:block/sanguine_log_top")
pillar("sanguine_wood",  side=f"{NS}:block/sanguine_log",   end=f"{NS}:block/sanguine_log")
pillar("stripped_sanguine_log",  side=f"{NS}:block/stripped_sanguine_log",  end=f"{NS}:block/stripped_sanguine_log_top")
pillar("stripped_sanguine_wood", side=f"{NS}:block/stripped_sanguine_log",  end=f"{NS}:block/stripped_sanguine_log")

# ---- PLANKS ----
bm("sanguine_planks", "minecraft:block/cube_all", {"all": f"{NS}:block/sanguine_planks"})
bs("sanguine_planks", {"": {"model": f"{NS}:block/sanguine_planks"}})
im_parent("sanguine_planks", f"{NS}:block/sanguine_planks")

# ---- LEAVES ----
bm("sanguine_leaves", "minecraft:block/leaves", {"all": f"{NS}:block/sanguine_leaves"})
bs("sanguine_leaves", {"": {"model": f"{NS}:block/sanguine_leaves"}})
im_parent("sanguine_leaves", f"{NS}:block/sanguine_leaves")

# ---- SAPLING ----
bm("sanguine_sapling", "minecraft:block/cross", {"cross": f"{NS}:block/sanguine_sapling"})
bs("sanguine_sapling", {"": {"model": f"{NS}:block/sanguine_sapling"}})
im_layer("sanguine_sapling", f"{NS}:block/sanguine_sapling")

# ---- STAIRS ----
def stairs(name, base):
    bm(name,         "minecraft:block/stairs",       {"bottom": base, "top": base, "side": base})
    bm(name + "_inner", "minecraft:block/inner_stairs", {"bottom": base, "top": base, "side": base})
    bm(name + "_outer", "minecraft:block/outer_stairs", {"bottom": base, "top": base, "side": base})
    variants = {}
    facings = ["north", "east", "south", "west"]
    facing_y = {"north": 270, "east": 0, "south": 90, "west": 180}
    for facing in facings:
        for half in ["bottom", "top"]:
            for shape in ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]:
                key = f"facing={facing},half={half},shape={shape}"
                model = name
                y = facing_y[facing]
                x = 0
                uvlock = False
                if shape == "straight":
                    model = name
                elif shape.startswith("inner"):
                    model = name + "_inner"
                    if shape == "inner_left":
                        y = (y + 270) % 360
                else:
                    model = name + "_outer"
                    if shape == "outer_left":
                        y = (y + 270) % 360
                if half == "top":
                    x = 180
                    if shape != "straight":
                        if shape.endswith("_left"):
                            y = (y + 90) % 360
                        else:
                            y = (y + 270) % 360
                v = {"model": f"{NS}:block/{model}"}
                if x: v["x"] = x
                if y: v["y"] = y
                if x or y: v["uvlock"] = True
                variants[key] = v
    bs(name, variants)
    im_parent(name, f"{NS}:block/{name}")

stairs("sanguine_stairs", f"{NS}:block/sanguine_planks")

# ---- SLAB ----
bm("sanguine_slab",     "minecraft:block/slab",     {"bottom": f"{NS}:block/sanguine_planks", "top": f"{NS}:block/sanguine_planks", "side": f"{NS}:block/sanguine_planks"})
bm("sanguine_slab_top", "minecraft:block/slab_top", {"bottom": f"{NS}:block/sanguine_planks", "top": f"{NS}:block/sanguine_planks", "side": f"{NS}:block/sanguine_planks"})
bs("sanguine_slab", {
    "type=bottom": {"model": f"{NS}:block/sanguine_slab"},
    "type=top":    {"model": f"{NS}:block/sanguine_slab_top"},
    "type=double": {"model": f"{NS}:block/sanguine_planks"}
})
im_parent("sanguine_slab", f"{NS}:block/sanguine_slab")

# ---- FENCE ----
bm("sanguine_fence_post", "minecraft:block/fence_post", {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_fence_side", "minecraft:block/fence_side", {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_fence_inventory", "minecraft:block/fence_inventory", {"texture": f"{NS}:block/sanguine_planks"})
fence_mp = {
    "multipart": [
        {"apply": {"model": f"{NS}:block/sanguine_fence_post"}},
        {"when": {"north": "true"}, "apply": {"model": f"{NS}:block/sanguine_fence_side", "uvlock": True}},
        {"when": {"east":  "true"}, "apply": {"model": f"{NS}:block/sanguine_fence_side", "y": 90, "uvlock": True}},
        {"when": {"south": "true"}, "apply": {"model": f"{NS}:block/sanguine_fence_side", "y": 180, "uvlock": True}},
        {"when": {"west":  "true"}, "apply": {"model": f"{NS}:block/sanguine_fence_side", "y": 270, "uvlock": True}}
    ]
}
write(os.path.join(BLOCKSTATES, "sanguine_fence.json"), fence_mp)
im_parent("sanguine_fence", f"{NS}:block/sanguine_fence_inventory")

# ---- FENCE GATE ----
bm("sanguine_fence_gate",          "minecraft:block/template_fence_gate",         {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_fence_gate_open",     "minecraft:block/template_fence_gate_open",    {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_fence_gate_wall",     "minecraft:block/template_fence_gate_wall",    {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_fence_gate_wall_open","minecraft:block/template_fence_gate_wall_open",{"texture": f"{NS}:block/sanguine_planks"})
fg_variants = {}
facing_y = {"south": 0, "west": 90, "north": 180, "east": 270}
for facing in facing_y:
    for in_wall in ["false", "true"]:
        for opn in ["false", "true"]:
            key = f"facing={facing},in_wall={in_wall},open={opn}"
            base = "sanguine_fence_gate"
            if in_wall == "true": base += "_wall"
            if opn == "true": base += "_open"
            v = {"model": f"{NS}:block/{base}", "uvlock": True}
            if facing_y[facing]:
                v["y"] = facing_y[facing]
            fg_variants[key] = v
bs("sanguine_fence_gate", fg_variants)
im_parent("sanguine_fence_gate", f"{NS}:block/sanguine_fence_gate")

# ---- BUTTON ----
bm("sanguine_button",          "minecraft:block/button",         {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_button_pressed",  "minecraft:block/button_pressed", {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_button_inventory","minecraft:block/button_inventory",{"texture": f"{NS}:block/sanguine_planks"})
btn_variants = {}
face_xy = {
    "floor":   (0, 0),    # default
    "wall":    (90, 0),   # rotated
    "ceiling": (180, 0)
}
facing_y = {"north": 180, "east": 270, "south": 0, "west": 90}
for face, (x, _) in face_xy.items():
    for facing, fy in facing_y.items():
        for powered in ["false", "true"]:
            key = f"face={face},facing={facing},powered={powered}"
            mdl = "sanguine_button_pressed" if powered == "true" else "sanguine_button"
            y = fy if face != "ceiling" else (fy + 180) % 360
            v = {"model": f"{NS}:block/{mdl}"}
            if x: v["x"] = x
            if y: v["y"] = y
            v["uvlock"] = True
            btn_variants[key] = v
bs("sanguine_button", btn_variants)
im_parent("sanguine_button", f"{NS}:block/sanguine_button_inventory")

# ---- PRESSURE PLATE ----
bm("sanguine_pressure_plate",     "minecraft:block/pressure_plate_up",  {"texture": f"{NS}:block/sanguine_planks"})
bm("sanguine_pressure_plate_down","minecraft:block/pressure_plate_down",{"texture": f"{NS}:block/sanguine_planks"})
bs("sanguine_pressure_plate", {
    "powered=false": {"model": f"{NS}:block/sanguine_pressure_plate"},
    "powered=true":  {"model": f"{NS}:block/sanguine_pressure_plate_down"}
})
im_parent("sanguine_pressure_plate", f"{NS}:block/sanguine_pressure_plate")

# ---- DOOR ----
def door_models(name):
    parts = ["bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
             "top_left",    "top_left_open",    "top_right",    "top_right_open"]
    for p in parts:
        bm(name + "_" + p, f"minecraft:block/door_{p}",
           {"top": f"{NS}:block/sanguine_door_top", "bottom": f"{NS}:block/sanguine_door_bottom"})

door_models("sanguine_door")

door_variants = {}
facing_y = {"east": 0, "south": 90, "west": 180, "north": 270}
for facing, fy in facing_y.items():
    for half in ["lower", "upper"]:
        for hinge in ["left", "right"]:
            for opn in ["false", "true"]:
                key = f"facing={facing},half={half},hinge={hinge},open={opn},powered=false"
                key2 = f"facing={facing},half={half},hinge={hinge},open={opn},powered=true"
                top = "top" if half == "upper" else "bottom"
                model = f"sanguine_door_{top}_{hinge}{'_open' if opn=='true' else ''}"
                y = fy
                if opn == "true":
                    if hinge == "right":
                        y = (y + 270) % 360
                    else:
                        y = (y + 90) % 360
                v = {"model": f"{NS}:block/{model}"}
                if y: v["y"] = y
                door_variants[key] = v
                door_variants[key2] = v
bs("sanguine_door", door_variants)
im_layer("sanguine_door", f"{NS}:item/sanguine_door")

# ---- TRAPDOOR ----
def trap_models(name):
    bm(name + "_top",    "minecraft:block/template_orientable_trapdoor_top",
       {"texture": f"{NS}:block/sanguine_trapdoor"})
    bm(name + "_bottom", "minecraft:block/template_orientable_trapdoor_bottom",
       {"texture": f"{NS}:block/sanguine_trapdoor"})
    bm(name + "_open",   "minecraft:block/template_orientable_trapdoor_open",
       {"texture": f"{NS}:block/sanguine_trapdoor"})

trap_models("sanguine_trapdoor")
td_variants = {}
facing_y = {"north": 0, "east": 90, "south": 180, "west": 270}
for facing, fy in facing_y.items():
    for half in ["bottom", "top"]:
        for opn in ["false", "true"]:
            key_base = f"facing={facing},half={half},open={opn}"
            if opn == "true":
                model = "sanguine_trapdoor_open"
                v = {"model": f"{NS}:block/{model}"}
                if fy: v["y"] = fy
                if half == "top":
                    v["x"] = 180
                    v["y"] = ((v.get("y", 0) + 180) % 360) if "y" in v else 180
            else:
                model = "sanguine_trapdoor_top" if half == "top" else "sanguine_trapdoor_bottom"
                v = {"model": f"{NS}:block/{model}"}
                if fy: v["y"] = fy
            for powered in ["false", "true"]:
                td_variants[key_base + f",powered={powered}"] = v
bs("sanguine_trapdoor", td_variants)
im_parent("sanguine_trapdoor", f"{NS}:block/sanguine_trapdoor_bottom")

# =====================================================================
# 3. POSSESSED MOB SPAWN EGG ITEM MODELS
# =====================================================================

for n in ["possessed_zombie_spawn_egg", "possessed_skeleton_spawn_egg"]:
    write(os.path.join(MODELS_ITEM, n + ".json"),
          {"parent": "minecraft:item/template_spawn_egg"})

print("OK sanguine wood + possessed assets")
