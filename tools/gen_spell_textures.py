"""r112: Procedural texture generator pros 50 spell scrolls.

Cada spell gera um PNG 16x16 com:
- Background: pergaminho amarelado base
- Faixa central: cor da escola
- Símbolo único centrado (varia por spell — runic mark)
- Bordas: 2 cantos dobrados (visual de scroll enrolado)
"""

from PIL import Image, ImageDraw
import os

OUT = os.path.join(os.path.dirname(__file__),
                   "..", "src", "main", "resources",
                   "assets", "liberthia", "textures", "item")
os.makedirs(OUT, exist_ok=True)

# (id, school) — school determines color
SPELLS = [
    # Fire (orange)
    ("fireball", "FIRE"), ("burning_dash", "FIRE"), ("inferno", "FIRE"),
    ("magma_bomb", "FIRE"), ("sun_beam", "FIRE"), ("phoenix_reborn", "FIRE"),
    ("cauterize", "FIRE"), ("heat_wave", "FIRE"),
    # Ice (cyan)
    ("frostbolt", "ICE"), ("ice_spike", "ICE"), ("frost_nova", "ICE"),
    ("glacial_storm", "ICE"), ("frost_step", "ICE"), ("frozen_ground", "ICE"),
    ("ray_of_frost", "ICE"), ("ice_lance", "ICE"),
    # Lightning (yellow)
    ("lightning_bolt", "LIGHTNING"), ("spark_burst", "LIGHTNING"),
    ("chain_lightning", "LIGHTNING"), ("shock", "LIGHTNING"),
    ("thunder_step", "LIGHTNING"), ("storm_cloud", "LIGHTNING"),
    ("static_field", "LIGHTNING"), ("lightning_lance_spell", "LIGHTNING"),
    # Blood (dark red)
    ("blood_step", "BLOOD"), ("lifedrain", "BLOOD"), ("heartstop", "BLOOD"),
    ("blood_spear", "BLOOD"), ("sanguine_bind", "BLOOD"),
    ("crimson_mist", "BLOOD"), ("vampiric_touch", "BLOOD"),
    ("blood_pact", "BLOOD"),
    # Eldritch (dark purple)
    ("void_tentacle", "ELDRITCH"), ("mind_spike", "ELDRITCH"),
    ("eldritch_blast", "ELDRITCH"), ("soul_tear", "ELDRITCH"),
    ("madness_wave", "ELDRITCH"), ("cosmic_void_spell", "ELDRITCH"),
    # Holy (white-gold)
    ("greater_heal", "HOLY"), ("smite", "HOLY"), ("divine_light", "HOLY"),
    ("sun_strike", "HOLY"), ("holy_lance_spell", "HOLY"),
    ("healing_aura", "HOLY"), ("sacred_ground", "HOLY"),
    ("judgment", "HOLY"),
    # Nature (green)
    ("vine_tangle", "NATURE"), ("earth_wall", "NATURE"),
    ("stone_shard", "NATURE"), ("wisps_heal", "NATURE"),
    ("roots", "NATURE"), ("bramble_storm", "NATURE"),
    # Evocation (mixed - magic_missile=eldritch, bone_spear=blood, etc)
    ("magic_missile", "ELDRITCH"), ("bone_spear", "BLOOD"),
    ("magic_shield", "HOLY"), ("summon_vex", "ELDRITCH"),
]

# School colors (RGB)
COLORS = {
    "FIRE":      ((255, 102, 51),  (200, 60, 20)),
    "ICE":       ((102, 204, 255), (40, 120, 200)),
    "LIGHTNING": ((255, 255, 68),  (200, 180, 30)),
    "BLOOD":     ((153, 0, 51),    (90, 0, 30)),
    "ELDRITCH":  ((102, 51, 204),  (50, 20, 120)),
    "HOLY":      ((255, 238, 170), (220, 190, 80)),
    "NATURE":    ((51, 170, 51),   (20, 110, 20)),
}

# Parchment base color
PARCH_LIGHT = (228, 210, 168)
PARCH_DARK = (180, 158, 110)
PARCH_BORDER = (120, 100, 60)


def hash_str(s):
    """Stable hash → int."""
    h = 0
    for c in s:
        h = (h * 31 + ord(c)) & 0xFFFFFFFF
    return h


def draw_symbol(draw, cx, cy, color, seed):
    """Desenha um símbolo runico único baseado no seed."""
    rng = seed
    # Tipo de símbolo
    sym_type = rng % 6
    light = tuple(min(255, c + 60) for c in color)
    if sym_type == 0:
        # Triangle pointing up
        draw.line([(cx, cy - 2), (cx - 2, cy + 2)], fill=color)
        draw.line([(cx, cy - 2), (cx + 2, cy + 2)], fill=color)
        draw.line([(cx - 2, cy + 2), (cx + 2, cy + 2)], fill=color)
    elif sym_type == 1:
        # Circle
        draw.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], outline=color)
        draw.point((cx, cy), fill=light)
    elif sym_type == 2:
        # X / cross
        draw.line([(cx - 2, cy - 2), (cx + 2, cy + 2)], fill=color)
        draw.line([(cx - 2, cy + 2), (cx + 2, cy - 2)], fill=color)
    elif sym_type == 3:
        # Lightning Z
        draw.line([(cx - 2, cy - 2), (cx + 1, cy)], fill=color)
        draw.line([(cx + 1, cy), (cx - 1, cy)], fill=color)
        draw.line([(cx - 1, cy), (cx + 2, cy + 2)], fill=color)
    elif sym_type == 4:
        # Star (5 points simplificado)
        draw.line([(cx, cy - 2), (cx, cy + 2)], fill=color)
        draw.line([(cx - 2, cy), (cx + 2, cy)], fill=color)
        draw.point((cx - 1, cy - 1), fill=light)
        draw.point((cx + 1, cy - 1), fill=light)
        draw.point((cx - 1, cy + 1), fill=light)
        draw.point((cx + 1, cy + 1), fill=light)
    else:
        # Diamond
        draw.line([(cx, cy - 2), (cx - 2, cy)], fill=color)
        draw.line([(cx, cy - 2), (cx + 2, cy)], fill=color)
        draw.line([(cx - 2, cy), (cx, cy + 2)], fill=color)
        draw.line([(cx + 2, cy), (cx, cy + 2)], fill=color)


def make_spell(spell_id, school):
    light, dark = COLORS[school]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # ─── Parchment body (rolo central) ───
    # Top scroll cap
    d.rectangle([1, 1, 14, 3], fill=PARCH_DARK)
    # Body
    d.rectangle([1, 3, 14, 12], fill=PARCH_LIGHT)
    # Bottom scroll cap
    d.rectangle([1, 12, 14, 14], fill=PARCH_DARK)
    # Borders
    d.rectangle([1, 1, 14, 14], outline=PARCH_BORDER)

    # Side highlights (top/bottom curls)
    d.line([(0, 2), (0, 13)], fill=PARCH_BORDER)
    d.line([(15, 2), (15, 13)], fill=PARCH_BORDER)
    d.line([(2, 2), (13, 2)], fill=light)
    d.line([(2, 13), (13, 13)], fill=light)

    # ─── Faixa central da escola ───
    d.rectangle([3, 6, 12, 9], fill=light)
    d.line([(3, 6), (12, 6)], fill=dark)
    d.line([(3, 9), (12, 9)], fill=dark)

    # ─── Símbolo runico no centro ───
    seed = hash_str(spell_id)
    draw_symbol(d, 7, 7, dark, seed)

    # ─── Extra detail: 2 manchas decorativas em volta ───
    if seed & 1:
        d.point((4, 4), fill=light)
        d.point((11, 11), fill=light)
    else:
        d.point((11, 4), fill=light)
        d.point((4, 11), fill=light)

    return img


count = 0
for spell_id, school in SPELLS:
    img = make_spell(spell_id, school)
    out_path = os.path.join(OUT, f"spell_{spell_id}.png")
    img.save(out_path)
    count += 1

print(f"Generated {count} spell textures in {OUT}")
