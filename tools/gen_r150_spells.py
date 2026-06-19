"""r150: 20 spells novos com elementos mesclados, particles avançadas, mob effects custom."""
import json
from pathlib import Path

OUT = Path("src/main/resources/data/liberthia/spells")
OUT.mkdir(parents=True, exist_ok=True)

# 20 spells variados — combos de elementos + efeitos criativos
SPELLS = [
    # ─── COMBO ELEMENTS (5) ───
    {
        "id": "factory_aurora_beam", "name": "Feixe Aurora",
        "school": "ICE", "rarity": "RARE",
        "mana": 45, "cooldown": 120, "damage": 14, "range": 24,
        "type": "BEAM",
        "lore": "Feixe que alterna entre gelo e fogo — congela e queima simultaneamente.",
        "behavior": {"beam_max_ticks": 30, "beam_range": 24},
        "vfx": {"color_primary": "#88ddff", "color_secondary": "#ff8866",
                "impact_scale": 1.4, "screen_shake": 0.6, "impact_light": 13},
        "effects": [
            {"type": "FREEZE", "duration": 60},
            {"type": "IGNITE", "duration": 80}
        ]
    },
    {
        "id": "factory_tempest_strike", "name": "Tempestade Glacial",
        "school": "LIGHTNING", "rarity": "RARE",
        "mana": 40, "cooldown": 100, "damage": 11, "range": 28,
        "type": "MULTI_SHOT",
        "lore": "Três raios congelantes em leque. Eletrocuta e congela ao mesmo tempo.",
        "behavior": {"speed": 2.2, "projectile_count": 3, "spread_degrees": 30,
                     "lifetime_ticks": 60},
        "vfx": {"color_primary": "#aaccff", "color_secondary": "#ffffaa",
                "trail_density": 14, "trail_size": 1.5, "screen_shake": 0.7},
        "effects": [
            {"type": "FREEZE", "duration": 40},
            {"type": "SLOWNESS", "duration": 100, "amplifier": 2}
        ]
    },
    {
        "id": "factory_hemorrhage_wave", "name": "Onda Hemorrágica",
        "school": "BLOOD", "rarity": "RARE",
        "mana": 38, "cooldown": 100, "damage": 9, "range": 8,
        "type": "CONE",
        "lore": "Cone de espinhos de sangue. Causa hemorragia + envenenamento.",
        "behavior": {"radius": 8.0, "cone_angle": 60.0},
        "vfx": {"color_primary": "#aa0044", "color_secondary": "#66aa33",
                "trail_density": 12, "impact_scale": 1.2},
        "effects": [
            {"type": "WITHER", "duration": 100, "amplifier": 1},
            {"type": "POISON", "duration": 120, "amplifier": 1},
            {"type": "LIFESTEAL", "magnitude": 0.4}
        ]
    },
    {
        "id": "factory_soul_shatter", "name": "Estilhaço da Alma",
        "school": "ELDRITCH", "rarity": "EPIC",
        "mana": 70, "cooldown": 240, "damage": 22, "range": 24,
        "type": "EXPLOSION",
        "lore": "Explosão divino-eldritch. Quebra a sanidade e cura aliados próximos.",
        "behavior": {"radius": 6.0},
        "vfx": {"color_primary": "#cc88ff", "color_secondary": "#ffeeaa",
                "impact_scale": 2.2, "impact_particles": 180,
                "screen_shake": 1.4, "screen_shake_ticks": 16, "impact_light": 15},
        "effects": [
            {"type": "BLINDNESS", "duration": 100},
            {"type": "NAUSEA", "duration": 120}
        ]
    },
    {
        "id": "factory_volcanic_storm", "name": "Tempestade Vulcânica",
        "school": "FIRE", "rarity": "EPIC",
        "mana": 85, "cooldown": 360, "damage": 14, "range": 30,
        "type": "RAIN",
        "lore": "Meteoros + raios caem em sequência numa área. Devastação dupla.",
        "behavior": {"radius": 10.0, "rain_strikes": 15, "rain_duration_ticks": 120,
                     "ignite_blocks": True},
        "vfx": {"color_primary": "#ff5500", "color_secondary": "#ffff44",
                "impact_scale": 1.6, "screen_shake": 1.0, "impact_light": 15},
        "effects": [{"type": "IGNITE", "duration": 140}]
    },

    # ─── SELF BUFFS (6) ───
    {
        "id": "factory_shadow_cloak", "name": "Manto das Sombras",
        "school": "ELDRITCH", "rarity": "RARE",
        "mana": 35, "cooldown": 400, "damage": 0, "range": 0,
        "type": "SELF",
        "lore": "Vira invisível e ganha velocidade. 20s de invisibilidade.",
        "behavior": {},
        "vfx": {"color_primary": "#552288", "impact_scale": 1.0, "screen_shake": 0.2},
        "effects": [
            {"type": "STRENGTH", "duration": 400, "amplifier": 0},
            {"type": "SPEED", "duration": 400, "amplifier": 2}
        ]
    },
    {
        "id": "factory_stone_shield", "name": "Escudo de Pedra",
        "school": "NATURE", "rarity": "UNCOMMON",
        "mana": 28, "cooldown": 240, "damage": 0, "range": 0,
        "type": "SELF",
        "lore": "Endurece a pele. Resistência IV + Absorção + leve lentidão.",
        "behavior": {},
        "vfx": {"color_primary": "#88aa44", "impact_scale": 1.0, "screen_shake": 0.1},
        "effects": [
            {"type": "RESISTANCE", "duration": 600, "amplifier": 3},
            {"type": "ABSORPTION", "duration": 600, "amplifier": 2},
            {"type": "SLOWNESS", "duration": 600, "amplifier": 0}
        ]
    },
    {
        "id": "factory_frost_armor", "name": "Armadura Gélida",
        "school": "ICE", "rarity": "UNCOMMON",
        "mana": 30, "cooldown": 280, "damage": 0, "range": 0,
        "type": "AURA",
        "lore": "Aura gélida — inimigos próximos ficam lentos enquanto buff dura.",
        "behavior": {"radius": 4.5, "aura_duration_ticks": 200, "aura_tick_interval": 20},
        "vfx": {"color_primary": "#aaeeff", "impact_scale": 0.8, "screen_shake": 0.2},
        "effects": [
            {"type": "SLOWNESS", "duration": 60, "amplifier": 1},
            {"type": "FREEZE", "duration": 30}
        ]
    },
    {
        "id": "factory_berserker_rage", "name": "Fúria do Berserker",
        "school": "BLOOD", "rarity": "RARE",
        "mana": 35, "cooldown": 360, "damage": 0, "range": 0,
        "type": "SELF",
        "lore": "Sangue ferve. Força III + Absorção II — preço: lentidão menor.",
        "behavior": {},
        "vfx": {"color_primary": "#cc0000", "impact_scale": 1.2, "screen_shake": 0.4},
        "effects": [
            {"type": "STRENGTH", "duration": 500, "amplifier": 2},
            {"type": "ABSORPTION", "duration": 500, "amplifier": 1},
            {"type": "FIRE_RESISTANCE", "duration": 500, "amplifier": 0}
        ]
    },
    {
        "id": "factory_divine_mantle", "name": "Manto Divino",
        "school": "HOLY", "rarity": "RARE",
        "mana": 40, "cooldown": 300, "damage": 0, "range": 5,
        "type": "AURA",
        "lore": "Aura curativa em volta do caster. Regenera self e aliados.",
        "behavior": {"radius": 6.0, "aura_duration_ticks": 300, "aura_tick_interval": 20},
        "vfx": {"color_primary": "#ffe680", "impact_scale": 1.0, "screen_shake": 0.1},
        "effects": [
            {"type": "REGENERATION", "duration": 40, "amplifier": 1},
            {"type": "GLOWING", "duration": 40}
        ]
    },
    {
        "id": "factory_arcane_ward", "name": "Proteção Arcana",
        "school": "ELDRITCH", "rarity": "RARE",
        "mana": 30, "cooldown": 200, "damage": 0, "range": 0,
        "type": "SELF",
        "lore": "Reduz dano mágico em 50% por 20s. Ative antes de batalha contra mages.",
        "behavior": {},
        "vfx": {"color_primary": "#9966ff", "impact_scale": 1.1},
        "effects": [
            {"type": "RESISTANCE", "duration": 400, "amplifier": 2}
        ]
    },

    # ─── PROJECTILE VARIANTS (5) ───
    {
        "id": "factory_mind_lance", "name": "Lança Mental",
        "school": "ELDRITCH", "rarity": "RARE",
        "mana": 28, "cooldown": 70, "damage": 13, "range": 32,
        "type": "HOMING",
        "lore": "Míssil que persegue alvos. Nauseia ao impacto.",
        "behavior": {"speed": 1.4, "homing": True, "lifetime_ticks": 120},
        "vfx": {"color_primary": "#aa66ff", "trail_density": 14, "screen_shake": 0.5},
        "effects": [
            {"type": "NAUSEA", "duration": 200},
            {"type": "WEAKNESS", "duration": 120, "amplifier": 1}
        ]
    },
    {
        "id": "factory_wisp_swarm", "name": "Enxame de Wisps",
        "school": "NATURE", "rarity": "UNCOMMON",
        "mana": 30, "cooldown": 50, "damage": 4, "range": 20,
        "type": "MULTI_SHOT",
        "lore": "5 pequenos wisps mágicos numa nuvem. Cada um pica leve.",
        "behavior": {"speed": 1.5, "projectile_count": 5, "spread_degrees": 50,
                     "lifetime_ticks": 50, "pierce": True},
        "vfx": {"color_primary": "#aaffaa", "color_secondary": "#88ff00",
                "trail_density": 6, "trail_size": 0.7, "impact_scale": 0.6},
        "effects": [{"type": "POISON", "duration": 60, "amplifier": 1}]
    },
    {
        "id": "factory_solar_flare", "name": "Erupção Solar",
        "school": "HOLY", "rarity": "RARE",
        "mana": 35, "cooldown": 90, "damage": 10, "range": 10,
        "type": "CONE",
        "lore": "Cone de luz divina. Cega + queima + brilha alvos por 15s.",
        "behavior": {"radius": 10.0, "cone_angle": 80.0},
        "vfx": {"color_primary": "#ffeeaa", "color_secondary": "#ffffff",
                "trail_density": 18, "impact_scale": 1.4, "impact_light": 15},
        "effects": [
            {"type": "IGNITE", "duration": 100},
            {"type": "BLINDNESS", "duration": 60},
            {"type": "GLOWING", "duration": 300}
        ]
    },
    {
        "id": "factory_necrotic_storm", "name": "Tempestade Necrótica",
        "school": "BLOOD", "rarity": "EPIC",
        "mana": 75, "cooldown": 280, "damage": 9, "range": 28,
        "type": "RAIN",
        "lore": "Chuva de sangue necrótico. Wither contínuo numa área grande.",
        "behavior": {"radius": 9.0, "rain_strikes": 18, "rain_duration_ticks": 140},
        "vfx": {"color_primary": "#660033", "color_secondary": "#330000",
                "impact_scale": 1.0, "screen_shake": 0.7, "impact_light": 8},
        "effects": [
            {"type": "WITHER", "duration": 160, "amplifier": 1},
            {"type": "WEAKNESS", "duration": 120}
        ]
    },
    {
        "id": "factory_thunder_step", "name": "Passo do Trovão",
        "school": "LIGHTNING", "rarity": "RARE",
        "mana": 22, "cooldown": 60, "damage": 6, "range": 0,
        "type": "DASH",
        "lore": "Salto elétrico — explode no início e fim. Aturde inimigos.",
        "behavior": {"dash_distance": 14.0},
        "vfx": {"color_primary": "#ffff66", "trail_density": 20, "trail_size": 1.8,
                "impact_scale": 1.2, "screen_shake": 0.7, "impact_light": 14}
    },

    # ─── AREA + UTILITY (4) ───
    {
        "id": "factory_earthquake", "name": "Terremoto",
        "school": "NATURE", "rarity": "RARE",
        "mana": 50, "cooldown": 180, "damage": 12, "range": 8,
        "type": "NOVA",
        "lore": "Onda sísmica do caster. Knockback + lentidão.",
        "behavior": {"radius": 8.0},
        "vfx": {"color_primary": "#aa7733", "color_secondary": "#553311",
                "impact_scale": 2.0, "screen_shake": 1.5, "screen_shake_ticks": 25,
                "impact_particles": 200},
        "effects": [
            {"type": "KNOCKBACK", "magnitude": 1.5},
            {"type": "SLOWNESS", "duration": 100, "amplifier": 2}
        ]
    },
    {
        "id": "factory_glacial_tomb", "name": "Túmulo Glacial",
        "school": "ICE", "rarity": "RARE",
        "mana": 40, "cooldown": 140, "damage": 6, "range": 6,
        "type": "NOVA",
        "lore": "Congela tudo em volta. Inimigos não conseguem se mover.",
        "behavior": {"radius": 7.0},
        "vfx": {"color_primary": "#aaeeff", "color_secondary": "#ffffff",
                "impact_scale": 1.8, "impact_particles": 150, "impact_light": 14},
        "effects": [
            {"type": "FREEZE", "duration": 200},
            {"type": "SLOWNESS", "duration": 200, "amplifier": 4}
        ]
    },
    {
        "id": "factory_phoenix_dive", "name": "Mergulho da Fênix",
        "school": "FIRE", "rarity": "RARE",
        "mana": 38, "cooldown": 140, "damage": 16, "range": 0,
        "type": "DASH",
        "lore": "Mergulha pra frente — explode no fim. Imuniza fogo durante o vôo.",
        "behavior": {"dash_distance": 16.0},
        "vfx": {"color_primary": "#ff5500", "color_secondary": "#ffaa00",
                "trail_density": 22, "trail_size": 1.8,
                "impact_scale": 1.8, "screen_shake": 0.8, "impact_light": 15},
        "effects": [{"type": "FIRE_RESISTANCE", "duration": 60}]
    },
    {
        "id": "factory_vampires_kiss", "name": "Beijo do Vampiro",
        "school": "BLOOD", "rarity": "RARE",
        "mana": 25, "cooldown": 70, "damage": 10, "range": 4,
        "type": "TOUCH",
        "lore": "Toque íntimo. Drena vida + envelhece o alvo (weakness).",
        "behavior": {"touch_range": 4.0},
        "vfx": {"color_primary": "#aa0044", "impact_scale": 1.0, "screen_shake": 0.3},
        "effects": [
            {"type": "LIFESTEAL", "magnitude": 1.5},
            {"type": "WEAKNESS", "duration": 200, "amplifier": 2},
            {"type": "SLOWNESS", "duration": 100, "amplifier": 1}
        ]
    },
]

assert len(SPELLS) == 20, f"expected 20 spells, got {len(SPELLS)}"

for spell in SPELLS:
    path = OUT / f"{spell['id']}.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(spell, f, indent=2, ensure_ascii=False)
    print(f"+ {spell['id']}.json")

print(f"\nTotal: {len(SPELLS)} spells generated")
