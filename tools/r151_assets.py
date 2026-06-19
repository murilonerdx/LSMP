"""r151: Mega asset gen — effect textures, lang entries, 10 advanced spells."""
import json
import random
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(".")
EFFECT_TEX_DIR = ROOT / "src/main/resources/assets/liberthia/textures/mob_effect"
SPELLS_DIR = ROOT / "src/main/resources/data/liberthia/spells"
PARTICLE_TEX_DIR = ROOT / "src/main/resources/assets/liberthia/textures/particle"
LANG_EN = ROOT / "src/main/resources/assets/liberthia/lang/en_us.json"
LANG_PT = ROOT / "src/main/resources/assets/liberthia/lang/pt_br.json"

EFFECT_TEX_DIR.mkdir(parents=True, exist_ok=True)
SPELLS_DIR.mkdir(parents=True, exist_ok=True)
PARTICLE_TEX_DIR.mkdir(parents=True, exist_ok=True)

# ════════════════════════════════════════════════════════════════════════
# Part 1: Effect Textures (15 PNGs 18x18)
# ════════════════════════════════════════════════════════════════════════

EFFECTS = [
    # id, primary, glyph_style, en, pt
    ("void_touch",         (50, 20, 80),    "tentacle", "Void Touch",           "Toque do Vazio"),
    ("whispers",           (110, 80, 140),  "spiral",   "Whispers",             "Sussurros"),
    ("static_vision",      (170, 170, 180), "static",   "Static Vision",        "Visão Estática"),
    ("time_fracture",      (90, 90, 130),   "clock",    "Time Fracture",        "Fratura do Tempo"),
    ("hollow_hunger",      (130, 110, 70),  "skull",    "Hollow Hunger",        "Fome Oca"),
    ("aetheric_shift",     (170, 220, 255), "wave",     "Aetheric Shift",       "Mudança Etérea"),
    ("soul_bleed",         (190, 30, 90),   "drop",     "Soul Bleed",           "Sangramento da Alma"),
    ("mana_surge",         (0, 200, 255),   "lightning","Mana Surge",           "Surto de Mana"),
    ("arcane_ward",        (150, 110, 240), "shield",   "Arcane Ward",          "Proteção Arcana"),
    ("astral_sight",       (255, 240, 130), "eye",      "Astral Sight",         "Visão Astral"),
    ("elemental_resonance",(255, 160, 80),  "star",     "Elemental Resonance",  "Ressonância Elemental"),
    ("mind_fortress",      (130, 220, 180), "brain",    "Mind Fortress",        "Fortaleza Mental"),
    ("chronosurge",        (255, 180, 60),  "swirl",    "Chronosurge",          "Surto do Tempo"),
    ("void_armor",         (50, 20, 80),    "shield",   "Void Armor",           "Armadura do Vazio"),
    ("dragon_breath",      (255, 100, 30),  "flame",    "Dragon Breath",        "Sopro do Dragão"),
]

def hex_color(c):
    return c + (255,)

def make_effect_icon(eid, color, style):
    """Cria 18x18 PNG icon."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    base = hex_color(color)
    bright = hex_color(tuple(min(255, c + 60) for c in color))
    dark = hex_color(tuple(max(0, c - 50) for c in color))

    # Background circle
    d.ellipse([1, 1, 16, 16], fill=base, outline=dark)

    # Style-specific glyph
    if style == "tentacle":
        # 4 tentacle curves
        for cx in [5, 9, 13]:
            for y in range(4, 14):
                if (cx + y) % 3 == 0:
                    img.putpixel((cx, y), bright)
                    img.putpixel((cx + 1, y), dark)
    elif style == "spiral":
        # Spiral arc
        for t in range(20):
            a = t * 0.4
            r = t * 0.3
            x = int(9 + r * (1 if t % 2 else -1) * 0.5)
            y = int(9 + r * 0.3)
            if 0 <= x < 18 and 0 <= y < 18:
                img.putpixel((x, y), bright)
    elif style == "static":
        # Random noise pixels
        random.seed(eid)
        for _ in range(30):
            x = random.randint(3, 14)
            y = random.randint(3, 14)
            img.putpixel((x, y), bright)
    elif style == "clock":
        d.ellipse([4, 4, 13, 13], outline=bright)
        d.line([(9, 5), (9, 9)], fill=bright)  # 12
        d.line([(9, 9), (12, 9)], fill=bright)  # 3
    elif style == "skull":
        d.ellipse([5, 4, 12, 10], fill=bright)
        img.putpixel((7, 7), dark)
        img.putpixel((10, 7), dark)
        d.rectangle([6, 11, 11, 13], fill=bright)
        for x in [7, 9]:
            img.putpixel((x, 12), dark)
    elif style == "wave":
        for x in range(3, 15):
            y = 9 + int(2 * ((x % 4) - 2))
            img.putpixel((x, y), bright)
            img.putpixel((x, y + 1), dark)
    elif style == "drop":
        d.polygon([(9, 4), (5, 12), (13, 12)], fill=bright)
        img.putpixel((9, 10), dark)
    elif style == "lightning":
        d.polygon([(8, 3), (10, 8), (8, 8), (10, 14), (6, 8), (8, 8)], fill=bright)
    elif style == "shield":
        d.polygon([(9, 3), (4, 6), (4, 11), (9, 15), (14, 11), (14, 6)], outline=bright, fill=base)
        d.line([(9, 5), (9, 13)], fill=bright)
    elif style == "eye":
        d.ellipse([3, 6, 14, 12], outline=bright, fill=base)
        d.ellipse([7, 7, 11, 11], fill=dark)
        img.putpixel((9, 9), bright)
    elif style == "star":
        d.polygon([(9, 2), (11, 7), (16, 7), (12, 10), (14, 15), (9, 12), (4, 15), (6, 10), (2, 7), (7, 7)], fill=bright)
    elif style == "brain":
        d.ellipse([3, 4, 14, 12], fill=bright, outline=dark)
        d.line([(9, 4), (9, 12)], fill=dark)
        for y in [6, 8, 10]:
            d.line([(5, y), (13, y)], fill=dark)
    elif style == "swirl":
        d.arc([3, 3, 14, 14], 0, 270, fill=bright, width=2)
        img.putpixel((9, 4), dark)
    elif style == "flame":
        d.polygon([(9, 3), (6, 9), (8, 7), (7, 13), (10, 9), (11, 12), (12, 7), (10, 9)], fill=bright)
        d.polygon([(9, 6), (8, 10), (10, 10)], fill=dark)

    return img

print("=== Effect textures ===")
for eid, color, style, _, _ in EFFECTS:
    path = EFFECT_TEX_DIR / f"{eid}.png"
    make_effect_icon(eid, color, style).save(path)
    print(f"+ {path.name}")

# ════════════════════════════════════════════════════════════════════════
# Part 2: Lang entries
# ════════════════════════════════════════════════════════════════════════

def update_lang(path, lang):
    data = json.loads(path.read_text(encoding="utf-8"))
    for eid, _, _, en, pt in EFFECTS:
        key = f"effect.liberthia.{eid}"
        data[key] = pt if lang == "pt" else en
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"+ {path.name}")

print("\n=== Lang entries ===")
update_lang(LANG_EN, "en")
update_lang(LANG_PT, "pt")

# ════════════════════════════════════════════════════════════════════════
# Part 3: 10 new advanced spells (using new fields: crit, knockback, element, category)
# ════════════════════════════════════════════════════════════════════════

# IMPORTANT: r151 effects podem ser referenciados via type="VOID_TOUCH" etc no JSON,
# MAS o SpellEffectSpec só conhece vanilla MobEffects + custom 6 (IGNITE, FREEZE, etc).
# Pra novos effects: usa POISON/etc OU adiciona type novo. Por enquanto uso vanilla + as 6 já feitas.

SPELLS = [
    {
        "id": "factory_critical_strike", "name": "Golpe Crítico",
        "school": "FIRE", "rarity": "RARE",
        "category": "CRITICAL", "element": "FIRE",
        "mana": 30, "cooldown": 60, "damage": 8, "range": 24,
        "type": "PROJECTILE",
        "lore": "60% chance de crítico. Dano dobrado em headshots.",
        "behavior": {"speed": 2.5, "crit_chance": 0.6, "crit_multiplier": 2.5,
                     "knockback_strength": 0.4, "lifetime_ticks": 50},
        "vfx": {"color_primary": "#ffaa00", "trail_density": 12, "impact_scale": 1.4,
                "screen_shake": 0.8, "impact_light": 14},
        "effects": [{"type": "IGNITE", "duration": 60}]
    },
    {
        "id": "factory_steam_explosion", "name": "Explosão de Vapor",
        "school": "FIRE", "rarity": "EPIC",
        "category": "EXPLOSION", "element": "FIRE", "secondary_element": "WATER",
        "mana": 55, "cooldown": 140, "damage": 14, "range": 16,
        "type": "EXPLOSION",
        "lore": "Vapor superquente — escalda + cega. Element combo: STEAM.",
        "behavior": {"radius": 5.0, "height": 4.0, "knockback_strength": 1.0,
                     "crit_chance": 0.2, "crit_multiplier": 2.0},
        "vfx": {"color_primary": "#aaaaee", "color_secondary": "#ffffff",
                "impact_scale": 2.0, "screen_shake": 1.2, "impact_light": 13,
                "impact_particles": 180},
        "effects": [{"type": "BLINDNESS", "duration": 80}, {"type": "IGNITE", "duration": 60}]
    },
    {
        "id": "factory_lava_lance", "name": "Lança de Lava",
        "school": "FIRE", "rarity": "RARE",
        "category": "DESTRUCTION", "element": "FIRE", "secondary_element": "EARTH",
        "mana": 40, "cooldown": 80, "damage": 18, "range": 28,
        "type": "PROJECTILE",
        "lore": "Lança fundida — perfura blocos. Element combo: LAVA.",
        "behavior": {"speed": 2.2, "pierce": True, "penetrate_blocks": True,
                     "lifetime_ticks": 80, "ignite_blocks": True,
                     "knockback_strength": 0.8, "aoe_radius": 1.5},
        "vfx": {"color_primary": "#ff3300", "color_secondary": "#aa1100",
                "trail_density": 16, "trail_size": 1.8,
                "impact_scale": 1.6, "screen_shake": 0.9, "impact_light": 15},
        "effects": [{"type": "IGNITE", "duration": 200}]
    },
    {
        "id": "factory_ice_storm", "name": "Tempestade de Gelo",
        "school": "ICE", "rarity": "EPIC",
        "category": "AOE", "element": "WATER", "secondary_element": "AIR",
        "mana": 75, "cooldown": 220, "damage": 8, "range": 28,
        "type": "RAIN",
        "lore": "Lanças de gelo do céu — element combo: ICE.",
        "behavior": {"radius": 9.0, "rain_strikes": 16, "rain_duration_ticks": 120,
                     "knockback_strength": 0.3},
        "vfx": {"color_primary": "#aaeeff", "color_secondary": "#ffffff",
                "impact_scale": 1.2, "screen_shake": 0.6, "impact_light": 12},
        "effects": [{"type": "FREEZE", "duration": 120}, {"type": "SLOWNESS", "duration": 160, "amplifier": 2}]
    },
    {
        "id": "factory_sand_blast", "name": "Jato de Areia",
        "school": "NATURE", "rarity": "UNCOMMON",
        "category": "PROJECTILE", "element": "EARTH", "secondary_element": "AIR",
        "mana": 22, "cooldown": 40, "damage": 6, "range": 14,
        "type": "CONE",
        "lore": "Jato abrasivo — cega + lentidão. Element combo: SAND.",
        "behavior": {"radius": 7.0, "cone_angle": 55.0},
        "vfx": {"color_primary": "#ddcc88", "trail_density": 16, "impact_scale": 1.1},
        "effects": [{"type": "BLINDNESS", "duration": 100}, {"type": "SLOWNESS", "duration": 80, "amplifier": 1}]
    },
    {
        "id": "factory_mud_prison", "name": "Prisão de Lama",
        "school": "NATURE", "rarity": "UNCOMMON",
        "category": "UTILITY", "element": "WATER", "secondary_element": "EARTH",
        "mana": 28, "cooldown": 100, "damage": 2, "range": 16,
        "type": "EXPLOSION",
        "lore": "Lama pesada prende alvos no chão. Element combo: MUD.",
        "behavior": {"radius": 3.5, "knockback_strength": 0.0},
        "vfx": {"color_primary": "#664422", "impact_scale": 1.0},
        "effects": [{"type": "SLOWNESS", "duration": 400, "amplifier": 4}, {"type": "WEAKNESS", "duration": 200}]
    },
    {
        "id": "factory_chrono_dash", "name": "Investida do Tempo",
        "school": "ELDRITCH", "rarity": "RARE",
        "category": "DASH", "element": "AIR",
        "mana": 26, "cooldown": 60, "damage": 4, "range": 0,
        "type": "DASH",
        "lore": "Investida acelerada — concede Chronosurge.",
        "behavior": {"dash_distance": 18.0, "knockback_strength": 0.5, "crit_chance": 0.35, "crit_multiplier": 2.0},
        "vfx": {"color_primary": "#ffaa44", "trail_density": 24, "trail_size": 2.0,
                "screen_shake": 0.5, "impact_light": 13}
    },
    {
        "id": "factory_dragon_roar", "name": "Rugido do Dragão",
        "school": "FIRE", "rarity": "EPIC",
        "category": "CHANNELED", "element": "FIRE", "secondary_element": "AIR",
        "mana": 60, "cooldown": 200, "damage": 16, "range": 14,
        "type": "CONE",
        "lore": "Rugido massivo — fogo + ar. Element combo: LIGHTNING.",
        "behavior": {"radius": 14.0, "cone_angle": 75.0, "ignite_blocks": True,
                     "knockback_strength": 2.0, "crit_chance": 0.25, "crit_multiplier": 2.0},
        "vfx": {"color_primary": "#ffaa00", "color_secondary": "#ffff66",
                "trail_density": 24, "impact_scale": 1.8,
                "screen_shake": 1.4, "screen_shake_ticks": 18, "impact_light": 15},
        "effects": [{"type": "IGNITE", "duration": 160}]
    },
    {
        "id": "factory_void_lance", "name": "Lança do Vazio",
        "school": "ELDRITCH", "rarity": "EPIC",
        "category": "DESTRUCTION", "element": "NONE",
        "mana": 60, "cooldown": 100, "damage": 26, "range": 36,
        "type": "PROJECTILE",
        "lore": "Penetra blocos. Crítico drenante.",
        "behavior": {"speed": 2.8, "pierce": True, "penetrate_blocks": True,
                     "lifetime_ticks": 100, "crit_chance": 0.4, "crit_multiplier": 3.0,
                     "knockback_strength": 0.6},
        "vfx": {"color_primary": "#5a1a8a", "color_secondary": "#dd66ff",
                "trail_density": 18, "trail_size": 1.4,
                "impact_scale": 1.8, "screen_shake": 1.0, "impact_light": 11},
        "effects": [{"type": "WITHER", "duration": 100}, {"type": "BLINDNESS", "duration": 80}]
    },
    {
        "id": "factory_aetheric_pulse", "name": "Pulso Etéreo",
        "school": "HOLY", "rarity": "RARE",
        "category": "AOE", "element": "AIR",
        "mana": 40, "cooldown": 140, "damage": 0, "range": 8,
        "type": "NOVA",
        "lore": "Pulso etéreo — buff defensivo em aliados.",
        "behavior": {"radius": 8.0, "knockback_strength": 1.0},
        "vfx": {"color_primary": "#aaccff", "impact_scale": 1.4, "impact_light": 13},
        "effects": [
            {"type": "ABSORPTION", "duration": 600, "amplifier": 2},
            {"type": "RESISTANCE", "duration": 400, "amplifier": 1},
            {"type": "REGENERATION", "duration": 200, "amplifier": 1}
        ]
    }
]

# Fix the mud_prison typo (0F if False — was Python eval shenanigan)
for s in SPELLS:
    if s["id"] == "factory_mud_prison":
        s["behavior"]["knockback_strength"] = 0.0

print("\n=== Spell JSONs ===")
for s in SPELLS:
    path = SPELLS_DIR / f"{s['id']}.json"
    path.write_text(json.dumps(s, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"+ {s['id']}.json")

print(f"\nDone. {len(EFFECTS)} effects + {len(SPELLS)} spells")
