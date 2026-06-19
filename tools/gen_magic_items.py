"""Generate textures + JSON models for ALL remaining magic items
(orbs, focus, wizard armor, special spells, crops, bow)."""

from PIL import Image, ImageDraw
import os
import json

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "liberthia", "textures", "item")
MODEL_ITEM = os.path.join(ROOT, "src", "main", "resources", "assets",
                          "liberthia", "models", "item")

SCHOOL_COLORS = {
    "fire":      ((255, 102, 51),  (160, 60, 20),  (255, 200, 50)),
    "ice":       ((102, 204, 255), (40, 120, 200), (200, 240, 255)),
    "lightning": ((255, 255, 68),  (200, 180, 30), (255, 255, 200)),
    "blood":     ((153, 0, 51),    (90, 0, 30),    (200, 30, 80)),
    "eldritch":  ((102, 51, 204),  (50, 20, 120),  (180, 100, 255)),
    "holy":      ((255, 238, 170), (220, 190, 80), (255, 255, 220)),
    "nature":    ((51, 170, 51),   (20, 110, 20),  (120, 230, 120)),
    "neutral":   ((140, 120, 100), (90, 70, 50),   (200, 180, 160)),
}

ITEMS = [
    # ORBS — round magical orbs (5)
    ("orb_mana_boost", "eldritch", "orb"),
    ("orb_cooldown", "ice", "orb"),
    ("orb_spell_damage", "fire", "orb"),
    ("orb_health", "blood", "orb"),
    ("orb_source_regen", "holy", "orb"),
    # FOCUS — gemstones (7)
    ("focus_fire", "fire", "gem"),
    ("focus_ice", "ice", "gem"),
    ("focus_lightning", "lightning", "gem"),
    ("focus_blood", "blood", "gem"),
    ("focus_eldritch", "eldritch", "gem"),
    ("focus_holy", "holy", "gem"),
    ("focus_nature", "nature", "gem"),
    # WEAPONS
    ("spectral_hammer", "eldritch", "hammer"),
    ("lightning_lance", "lightning", "spear"),
    ("devour_jaw", "blood", "jaw"),
    ("jar_of_light", "holy", "lantern"),
    ("enchanters_gauntlet", "fire", "gauntlet"),
    ("ice_tomb_spell", "ice", "tome"),
    ("ascension_spell", "holy", "tome"),
    ("heat_surge_spell", "fire", "tome"),
    ("spell_bow", "eldritch", "bow"),
    # WIZARD ARMOR — Chest (7) + Helm (7) + Legs (7) + Boots (7)
    *[(f"wizard_chest_{s}", s, "chest") for s in ["fire","ice","lightning","blood","eldritch","holy","nature"]],
    *[(f"wizard_helm_{s}", s, "helm") for s in ["fire","ice","lightning","blood","eldritch","holy","nature"]],
    *[(f"wizard_legs_{s}", s, "legs") for s in ["fire","ice","lightning","blood","eldritch","holy","nature"]],
    *[(f"wizard_boots_{s}", s, "boots") for s in ["fire","ice","lightning","blood","eldritch","holy","nature"]],
    # CROPS / FOOD
    ("mage_bloom_seed", "nature", "seed"),
    ("mage_bloom_fiber", "nature", "fiber"),
    ("source_berry", "eldritch", "berry"),
    # JAR (item form — see also block)
    ("repository", "neutral", "chest_item"),
]


def hsh(s):
    h = 0
    for c in s:
        h = (h * 31 + ord(c)) & 0xFFFFFFFF
    return h


def draw_orb(d, light, dark, accent):
    # 3D orb effect
    d.ellipse([3, 3, 12, 12], fill=dark, outline=accent)
    d.ellipse([4, 4, 11, 11], fill=light)
    d.point((6, 5), fill=accent)
    d.point((7, 5), fill=accent)
    d.line([(11, 11), (12, 12)], fill=dark)


def draw_gem(d, light, dark, accent):
    # Diamond gem
    d.line([(7, 2), (7, 13)], fill=dark)
    d.line([(8, 2), (8, 13)], fill=dark)
    for i, w in enumerate([0, 1, 2, 3, 4, 5, 6, 6, 6, 5, 4, 3, 2, 1, 0]):
        if i + 1 >= 16: break
        d.line([(7 - w, i + 1), (8 + w, i + 1)], fill=light)
    # Highlight
    d.point((6, 5), fill=accent)
    d.point((7, 5), fill=accent)


def draw_hammer(d, light, dark, accent):
    # Diagonal hammer
    d.rectangle([3, 1, 13, 5], fill=dark)
    d.rectangle([4, 2, 12, 4], fill=light)
    d.line([(8, 5), (4, 14)], fill=dark)
    d.line([(9, 5), (5, 14)], fill=dark)
    d.point((4, 14), fill=accent)


def draw_spear(d, light, dark, accent):
    # Vertical spear with diamond tip
    d.line([(8, 14), (8, 4)], fill=dark)
    d.line([(7, 13), (7, 5)], fill=light)
    d.polygon([(8, 1), (5, 5), (8, 4), (11, 5)], fill=light, outline=accent)


def draw_jaw(d, light, dark, accent):
    # Skull jaw
    d.polygon([(3, 4), (3, 11), (13, 11), (13, 4)], fill=dark, outline=accent)
    # Teeth
    for x in range(4, 13, 2):
        d.line([(x, 11), (x, 13)], fill=light)


def draw_lantern(d, light, dark, accent):
    # Hanging lantern
    d.rectangle([5, 3, 10, 12], fill=light)
    d.rectangle([4, 12, 11, 14], fill=dark)
    d.rectangle([4, 2, 11, 4], fill=dark)
    d.line([(7, 0), (7, 2)], fill=dark)
    d.line([(8, 0), (8, 2)], fill=dark)
    # Glow center
    d.point((7, 7), fill=accent)
    d.point((8, 7), fill=accent)
    d.point((7, 8), fill=accent)
    d.point((8, 8), fill=accent)


def draw_gauntlet(d, light, dark, accent):
    # Gauntlet/glove
    d.rectangle([3, 6, 13, 13], fill=dark)
    # Fingers
    for x in [3, 6, 9, 12]:
        d.rectangle([x, 2, x + 2, 6], fill=light)
    d.rectangle([3, 7, 13, 12], fill=light)
    d.point((8, 9), fill=accent)


def draw_tome(d, light, dark, accent):
    # Book/tome
    d.rectangle([2, 2, 13, 13], fill=dark)
    d.rectangle([3, 3, 12, 12], fill=light)
    d.line([(7, 2), (7, 13)], fill=dark)
    d.line([(8, 2), (8, 13)], fill=dark)
    # Star symbol
    d.point((5, 6), fill=accent)
    d.point((10, 6), fill=accent)
    d.point((5, 9), fill=accent)
    d.point((10, 9), fill=accent)


def draw_bow(d, light, dark, accent):
    # Bow curve
    pts = [(13, 2), (14, 5), (14, 8), (13, 11), (11, 13)]
    for x, y in pts:
        d.point((x, y), fill=dark)
    # String
    d.line([(13, 2), (13, 13)], fill=accent)
    # Body curve
    for y in range(3, 12):
        x = 13 - int(2 * abs(y - 7) / 4)
        d.point((x, y), fill=light)


def draw_chest(d, light, dark, accent):
    # Wizard chestplate
    d.rectangle([3, 3, 12, 14], fill=dark)
    d.rectangle([4, 4, 11, 13], fill=light)
    # Pauldrons
    d.rectangle([2, 3, 4, 6], fill=dark)
    d.rectangle([11, 3, 13, 6], fill=dark)
    # Center gem
    d.point((7, 7), fill=accent)
    d.point((8, 7), fill=accent)
    d.point((7, 8), fill=accent)
    d.point((8, 8), fill=accent)


def draw_helm(d, light, dark, accent):
    # Wizard helmet/hood
    d.rectangle([3, 4, 12, 13], fill=dark)
    d.rectangle([4, 5, 11, 12], fill=light)
    # Pointy top
    d.polygon([(7, 1), (4, 5), (11, 5), (8, 1)], fill=light, outline=dark)
    d.point((8, 3), fill=accent)
    d.point((7, 3), fill=accent)


def draw_legs(d, light, dark, accent):
    # Wizard pants
    d.rectangle([3, 2, 12, 14], fill=dark)
    d.rectangle([4, 3, 7, 13], fill=light)
    d.rectangle([8, 3, 11, 13], fill=light)
    d.rectangle([3, 2, 12, 4], fill=accent)


def draw_boots(d, light, dark, accent):
    # Wizard boots
    d.rectangle([3, 7, 7, 14], fill=dark)
    d.rectangle([8, 7, 12, 14], fill=dark)
    d.rectangle([3, 12, 13, 14], fill=light)
    d.line([(5, 7), (5, 11)], fill=accent)
    d.line([(10, 7), (10, 11)], fill=accent)


def draw_seed(d, light, dark, accent):
    # Small seeds
    pts = [(6, 6), (9, 6), (6, 9), (9, 9), (7, 11)]
    for x, y in pts:
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=dark)
    d.point((7, 7), fill=accent)


def draw_fiber(d, light, dark, accent):
    # Strands
    for i in range(0, 16, 3):
        d.line([(i, 3), (i + 2, 12)], fill=light)
    d.line([(2, 8), (14, 8)], fill=dark)


def draw_berry(d, light, dark, accent):
    # Berry cluster
    for x, y in [(5, 5), (10, 6), (7, 8), (5, 11), (10, 11)]:
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=light, outline=dark)
    d.point((6, 5), fill=accent)


def draw_chest_item(d, light, dark, accent):
    """Repository chest item."""
    d.rectangle([2, 5, 13, 13], fill=dark)
    d.rectangle([3, 6, 12, 12], fill=light)
    d.rectangle([2, 5, 13, 7], fill=accent)
    d.line([(7, 5), (7, 13)], fill=dark)
    d.line([(8, 5), (8, 13)], fill=dark)


KIND_FNS = {
    "orb": draw_orb, "gem": draw_gem, "hammer": draw_hammer,
    "spear": draw_spear, "jaw": draw_jaw, "lantern": draw_lantern,
    "gauntlet": draw_gauntlet, "tome": draw_tome, "bow": draw_bow,
    "chest": draw_chest, "helm": draw_helm, "legs": draw_legs,
    "boots": draw_boots, "seed": draw_seed, "fiber": draw_fiber,
    "berry": draw_berry, "chest_item": draw_chest_item,
}

count = 0
for name, school, kind in ITEMS:
    light, dark, accent = SCHOOL_COLORS[school]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    KIND_FNS[kind](d, light, dark, accent)
    img.save(os.path.join(TEX_ITEM, f"{name}.png"))

    # Item model — for items that aren't block items
    if kind != "chest_item":  # repository already has chest_item parent
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"liberthia:item/{name}"}
        }
        path = os.path.join(MODEL_ITEM, f"{name}.json")
        # Don't overwrite block-parented models
        if not os.path.exists(path) or os.path.getsize(path) < 200:
            with open(path, "w") as f:
                json.dump(model, f)
    count += 1

print(f"Generated {count} item textures")
