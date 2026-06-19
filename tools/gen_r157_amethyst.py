#!/usr/bin/env python3
"""r157: Generate Vazio Roxo (amethyst void category) — 10 new spells.

Includes the masterpiece "Vazio Roxo" with elaborate VFX/effects, plus
9 supporting amethyst-themed spells.
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
SPELLS_DIR = ROOT / "data" / "liberthia" / "spells"

AMETHYST = (122, 61, 255)  # Roxo ametista
LILAC = (178, 102, 255)
DEEP_VIOLET = (43, 0, 64)
WHITE_VIOLET = (231, 204, 255)
DARK_MAGENTA = (75, 0, 110)

SPELLS = [
    {
        "id": "factory_vazio_roxo",
        "name": "§5§lVazio Roxo",
        "school": "ELDRITCH",
        "rarity": "EPIC",
        "mana": 90,
        "cooldown": 400,
        "damage": 28,
        "range": 35,
        "type": "PROJECTILE",
        "lore": "Esfera ametista densa que colapsa espaço. Silenciosa. Pesada. Anormal.",
        "behavior": {
            "speed": 0.6,
            "lifetime_ticks": 100,
            "pierce": False,
            "aoe_radius": 5.5,
            "ignite_blocks": False,
            "knockback_strength": 0.0
        },
        "vfx": {
            "color_primary": "#7A3DFF",
            "color_secondary": "#2B0040",
            "trail_density": 30,
            "trail_size": 2.4,
            "impact_scale": 3.5,
            "impact_particles": 280,
            "screen_shake": 1.4,
            "impact_light": 13
        },
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "hungry_void",       "duration": 200, "magnitude": 1.0},
            {"type": "CUSTOM_EFFECT", "effect": "vertigo",           "duration": 200, "magnitude": 1.0},
            {"type": "CUSTOM_EFFECT", "effect": "dimensional_blindness", "duration": 100, "magnitude": 0.0},
            {"type": "KNOCKBACK", "magnitude": 2.5}
        ],
        "element": "ELDRITCH",
        "category": "DESTRUCTION"
    },
    {
        "id": "factory_amethyst_lance",
        "name": "Lança de Ametista",
        "school": "ELDRITCH",
        "rarity": "RARE",
        "mana": 45,
        "cooldown": 90,
        "damage": 14,
        "range": 30,
        "type": "PROJECTILE",
        "lore": "Lança cristalina perfura através de armaduras.",
        "behavior": {"speed": 2.8, "lifetime_ticks": 70, "pierce": True, "aoe_radius": 0.5},
        "vfx": {"color_primary": "#7A3DFF", "color_secondary": "#E7CCFF",
                "trail_density": 16, "trail_size": 1.6, "impact_scale": 1.2,
                "impact_particles": 80, "screen_shake": 0.4, "impact_light": 11},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "ghost_touch", "duration": 80, "magnitude": 0.0}
        ],
        "element": "ELDRITCH",
        "category": "PROJECTILE"
    },
    {
        "id": "factory_void_singularity",
        "name": "Singularidade do Vazio",
        "school": "ELDRITCH",
        "rarity": "EPIC",
        "mana": 80,
        "cooldown": 350,
        "damage": 18,
        "range": 28,
        "type": "AOE_BURST",
        "lore": "Mini buraco negro ametista. Puxa tudo pro centro antes de explodir.",
        "behavior": {"radius": 7.0, "aoe_radius": 7.0, "lifetime_ticks": 100},
        "vfx": {"color_primary": "#A020F0", "color_secondary": "#4B006E",
                "trail_density": 25, "trail_size": 2.0, "impact_scale": 2.5,
                "impact_particles": 180, "screen_shake": 1.0, "impact_light": 12},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "hungry_void", "duration": 200, "magnitude": 1.0},
            {"type": "KNOCKBACK", "magnitude": 2.0}
        ],
        "element": "ELDRITCH",
        "category": "AOE"
    },
    {
        "id": "factory_amethyst_shards",
        "name": "Estilhaços de Ametista",
        "school": "ELDRITCH",
        "rarity": "UNCOMMON",
        "mana": 30,
        "cooldown": 60,
        "damage": 8,
        "range": 22,
        "type": "PROJECTILE",
        "lore": "Múltiplos shards triangulares deixam trilhas roxas.",
        "behavior": {"speed": 1.9, "lifetime_ticks": 50, "pierce": False, "aoe_radius": 1.0},
        "vfx": {"color_primary": "#B266FF", "color_secondary": "#7A3DFF",
                "trail_density": 12, "trail_size": 1.0, "impact_scale": 0.9,
                "impact_particles": 50, "screen_shake": 0.2, "impact_light": 9},
        "effects": [],
        "element": "ELDRITCH",
        "category": "PROJECTILE"
    },
    {
        "id": "factory_gravity_well",
        "name": "Poço Gravitacional",
        "school": "ELDRITCH",
        "rarity": "RARE",
        "mana": 55,
        "cooldown": 200,
        "damage": 0,
        "range": 24,
        "type": "AURA",
        "lore": "Cria um poço gravitacional ametista que puxa mobs por 6 segundos.",
        "behavior": {"radius": 8.0, "lifetime_ticks": 120, "aoe_radius": 8.0},
        "vfx": {"color_primary": "#7A3DFF", "color_secondary": "#140018",
                "trail_density": 20, "trail_size": 1.5, "impact_scale": 1.5,
                "impact_particles": 120, "screen_shake": 0.5, "impact_light": 10},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "magnet_fist", "duration": 120, "magnitude": 1.0},
            {"type": "CUSTOM_EFFECT", "effect": "time_dilation", "duration": 120, "magnitude": 0.0}
        ],
        "element": "ELDRITCH",
        "category": "AOE"
    },
    {
        "id": "factory_dimensional_rupture",
        "name": "Ruptura Dimensional",
        "school": "ELDRITCH",
        "rarity": "EPIC",
        "mana": 70,
        "cooldown": 280,
        "damage": 16,
        "range": 26,
        "type": "TARGETED",
        "lore": "Cria pequenos teleportes aleatórios em volta da vítima.",
        "behavior": {"radius": 4.0, "aoe_radius": 4.0},
        "vfx": {"color_primary": "#A020F0", "color_secondary": "#E7CCFF",
                "trail_density": 18, "trail_size": 1.4, "impact_scale": 1.3,
                "impact_particles": 100, "screen_shake": 0.6, "impact_light": 11},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "reverse_gravity", "duration": 100, "magnitude": 1.0},
            {"type": "CUSTOM_EFFECT", "effect": "nightmare", "duration": 100, "magnitude": 0.0}
        ],
        "element": "ELDRITCH",
        "category": "UTILITY"
    },
    {
        "id": "factory_void_aegis",
        "name": "Égide do Vazio",
        "school": "ELDRITCH",
        "rarity": "RARE",
        "mana": 50,
        "cooldown": 180,
        "damage": 0,
        "range": 0,
        "type": "SELF_BUFF",
        "lore": "Escudo cristalino ametista. Liberthia Blessing + Mirror Walk.",
        "behavior": {"lifetime_ticks": 200},
        "vfx": {"color_primary": "#7A3DFF", "color_secondary": "#FFFFFF",
                "trail_density": 15, "trail_size": 1.3, "impact_scale": 1.0,
                "impact_particles": 60, "screen_shake": 0.0, "impact_light": 12},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "liberthia_blessing", "duration": 200, "magnitude": 0.0},
            {"type": "CUSTOM_EFFECT", "effect": "mirror_walk", "duration": 200, "magnitude": 0.0}
        ],
        "element": "ELDRITCH",
        "category": "UTILITY"
    },
    {
        "id": "factory_crystal_storm",
        "name": "Tempestade de Cristais",
        "school": "ELDRITCH",
        "rarity": "EPIC",
        "mana": 75,
        "cooldown": 300,
        "damage": 12,
        "range": 25,
        "type": "AOE_BURST",
        "lore": "Chuva de shards ametistas devastadora em área.",
        "behavior": {"radius": 6.0, "aoe_radius": 6.0, "rain_strikes": 20},
        "vfx": {"color_primary": "#B266FF", "color_secondary": "#4B006E",
                "trail_density": 22, "trail_size": 1.5, "impact_scale": 1.8,
                "impact_particles": 140, "screen_shake": 0.8, "impact_light": 11},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "pox_swarm", "duration": 100, "magnitude": 0.0}
        ],
        "element": "ELDRITCH",
        "category": "AOE"
    },
    {
        "id": "factory_void_dash",
        "name": "Dash do Vazio",
        "school": "ELDRITCH",
        "rarity": "RARE",
        "mana": 40,
        "cooldown": 100,
        "damage": 10,
        "range": 20,
        "type": "DASH",
        "lore": "Dash através de dimensões. Deixa rastro escuro.",
        "behavior": {"dashDistance": 14.0, "speed": 3.0},
        "vfx": {"color_primary": "#7A3DFF", "color_secondary": "#140018",
                "trail_density": 18, "trail_size": 1.5, "impact_scale": 1.0,
                "impact_particles": 80, "screen_shake": 0.5, "impact_light": 10},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "shadow_double", "duration": 80, "magnitude": 0.0}
        ],
        "element": "ELDRITCH",
        "category": "DASH"
    },
    {
        "id": "factory_void_heart",
        "name": "§5§lCoração do Vazio Roxo",
        "school": "ELDRITCH",
        "rarity": "EPIC",
        "mana": 120,
        "cooldown": 600,
        "damage": 40,
        "range": 35,
        "type": "AOE_BURST",
        "lore": "Variante suprema. Múltiplas luas ametistas colapsam numa singularidade gigante. Eclipse temporário.",
        "behavior": {"radius": 10.0, "aoe_radius": 10.0, "lifetime_ticks": 140},
        "vfx": {"color_primary": "#7A3DFF", "color_secondary": "#05010A",
                "trail_density": 40, "trail_size": 3.0, "impact_scale": 5.0,
                "impact_particles": 400, "screen_shake": 2.0, "impact_light": 15},
        "effects": [
            {"type": "CUSTOM_EFFECT", "effect": "hungry_void", "duration": 300, "magnitude": 2.0},
            {"type": "CUSTOM_EFFECT", "effect": "time_dilation", "duration": 300, "magnitude": 1.0},
            {"type": "CUSTOM_EFFECT", "effect": "vertigo", "duration": 300, "magnitude": 1.0},
            {"type": "CUSTOM_EFFECT", "effect": "dimensional_blindness", "duration": 200, "magnitude": 0.0},
            {"type": "KNOCKBACK", "magnitude": 4.0}
        ],
        "element": "ELDRITCH",
        "category": "DESTRUCTION"
    },
]


def main():
    SPELLS_DIR.mkdir(parents=True, exist_ok=True)
    for spell in SPELLS:
        fname = spell["id"] + ".json"
        (SPELLS_DIR / fname).write_text(
            json.dumps(spell, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"OK {fname}")
    print(f"\nTotal: {len(SPELLS)} amethyst/void spells")


if __name__ == "__main__":
    main()
