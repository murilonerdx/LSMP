"""r148: Gera 9 JSON spell recipes restantes pra cobrir todos os 12 SpellTypes."""
import json
from pathlib import Path

OUT = Path("src/main/resources/data/liberthia/spells")
OUT.mkdir(parents=True, exist_ok=True)

SPELLS = [
    {
        "id": "factory_death_beam",
        "name": "Feixe Mortal",
        "school": "ELDRITCH", "rarity": "EPIC",
        "mana": 60, "cooldown": 200, "damage": 30, "range": 32,
        "type": "BEAM",
        "lore": "Feixe de pura aniquilação eldritch que desfia a realidade.",
        "behavior": {"beam_max_ticks": 40, "beam_range": 32},
        "vfx": {"color_primary": "#aa00ff", "trail_density": 14, "impact_scale": 1.5,
                "screen_shake": 1.0, "screen_shake_ticks": 14},
        "effects": [{"type": "WITHER", "duration": 120, "amplifier": 1}, {"type": "BLINDNESS", "duration": 100}]
    },
    {
        "id": "factory_meteor",
        "name": "Meteoro",
        "school": "FIRE", "rarity": "EPIC",
        "mana": 70, "cooldown": 240, "damage": 25, "range": 32,
        "type": "EXPLOSION",
        "lore": "Invoca um meteoro flamejante no ponto mirado. Explosão massiva.",
        "behavior": {"radius": 5.0, "ignite_blocks": True},
        "vfx": {"color_primary": "#ff4400", "impact_scale": 2.5, "impact_particles": 200,
                "screen_shake": 1.5, "screen_shake_ticks": 20, "impact_light": 15},
        "effects": [{"type": "IGNITE", "duration": 160}, {"type": "KNOCKBACK", "magnitude": 1.2}]
    },
    {
        "id": "factory_frost_nova",
        "name": "Nova Gélida",
        "school": "ICE", "rarity": "UNCOMMON",
        "mana": 30, "cooldown": 100, "damage": 8, "range": 6,
        "type": "NOVA",
        "lore": "Onda congelante explode do caster, congelando tudo ao redor.",
        "behavior": {"radius": 6.0},
        "vfx": {"color_primary": "#aaeeff", "color_secondary": "#ffffff", "impact_scale": 1.8,
                "screen_shake": 0.6}
    , "effects": [{"type": "FREEZE", "duration": 100}, {"type": "SLOWNESS", "duration": 140, "amplifier": 2}]
    },
    {
        "id": "factory_dragon_breath",
        "name": "Sopro do Dragão",
        "school": "FIRE", "rarity": "RARE",
        "mana": 45, "cooldown": 120, "damage": 12, "range": 8,
        "type": "CONE",
        "lore": "Jato de fogo em cone à frente do caster. Inflama tudo no caminho.",
        "behavior": {"radius": 8.0, "cone_angle": 70.0, "ignite_blocks": True},
        "vfx": {"color_primary": "#ff7700", "trail_density": 14, "impact_scale": 1.2,
                "screen_shake": 0.5}
        , "effects": [{"type": "IGNITE", "duration": 120}]
    },
    {
        "id": "factory_meteor_storm",
        "name": "Tempestade de Meteoros",
        "school": "FIRE", "rarity": "EPIC",
        "mana": 90, "cooldown": 400, "damage": 12, "range": 28,
        "type": "RAIN",
        "lore": "12 meteoros caem em sequência em uma área. Devastação total.",
        "behavior": {"radius": 8.0, "rain_strikes": 12, "rain_duration_ticks": 100, "ignite_blocks": True},
        "vfx": {"color_primary": "#ff5500", "impact_scale": 1.4, "screen_shake": 0.8, "impact_light": 14},
        "effects": [{"type": "IGNITE", "duration": 100}]
    },
    {
        "id": "factory_burning_aura",
        "name": "Aura Flamejante",
        "school": "FIRE", "rarity": "RARE",
        "mana": 40, "cooldown": 200, "damage": 3, "range": 5,
        "type": "AURA",
        "lore": "Aura ígnea ao redor do caster por 8s. Pulsa dano em todos próximos.",
        "behavior": {"radius": 4.5, "aura_duration_ticks": 160, "aura_tick_interval": 20, "ignite_blocks": False},
        "vfx": {"color_primary": "#ff6600", "color_secondary": "#ffaa00", "impact_scale": 0.8,
                "screen_shake": 0.2},
        "effects": [{"type": "IGNITE", "duration": 40}]
    },
    {
        "id": "factory_vampiric_touch",
        "name": "Toque Vampírico",
        "school": "BLOOD", "rarity": "UNCOMMON",
        "mana": 18, "cooldown": 50, "damage": 6, "range": 4,
        "type": "TOUCH",
        "lore": "Toque drenante — rouba HP do alvo e cura o caster.",
        "behavior": {"touch_range": 4.0},
        "vfx": {"color_primary": "#cc0000", "color_secondary": "#660000", "impact_scale": 1.0,
                "screen_shake": 0.3},
        "effects": [{"type": "LIFESTEAL", "magnitude": 1.0}, {"type": "WITHER", "duration": 80}]
    },
    {
        "id": "factory_iron_skin",
        "name": "Pele de Ferro",
        "school": "NATURE", "rarity": "UNCOMMON",
        "mana": 25, "cooldown": 200, "damage": 0, "range": 0,
        "type": "SELF",
        "lore": "Endurece a pele do caster. Resistência III + Absorção por 30s.",
        "behavior": {},
        "vfx": {"color_primary": "#88aa44", "impact_scale": 1.0, "screen_shake": 0.1},
        "effects": [
            {"type": "RESISTANCE", "duration": 600, "amplifier": 2},
            {"type": "ABSORPTION", "duration": 600, "amplifier": 1}
        ]
    },
    {
        "id": "factory_phase_dash",
        "name": "Investida Espectral",
        "school": "ELDRITCH", "rarity": "RARE",
        "mana": 22, "cooldown": 60, "damage": 0, "range": 0,
        "type": "DASH",
        "lore": "Salta pra frente atravessando alvos. Mobility tool.",
        "behavior": {"dash_distance": 12.0},
        "vfx": {"color_primary": "#9966ff", "trail_density": 18, "trail_size": 1.6,
                "impact_scale": 0.8, "screen_shake": 0.3}
    }
]

for spell in SPELLS:
    name = spell["id"]
    path = OUT / f"{name}.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(spell, f, indent=2, ensure_ascii=False)
    print(f"Wrote {path}")

print(f"\nTotal: {len(SPELLS)} spell recipes generated.")
