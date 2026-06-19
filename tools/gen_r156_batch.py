#!/usr/bin/env python3
"""r156 mega-batch generator.

Generates:
- 20 mob effect textures (16x16) for status HUD/inventory
- 64 new spell JSONs in data/liberthia/spells/
- regenerates factory_spell_scroll texture index map
"""
import json
import math
import os
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
EFFECT_TEX_DIR = ROOT / "assets" / "liberthia" / "textures" / "mob_effect"
SPELLS_DIR = ROOT / "data" / "liberthia" / "spells"
SPELL_TEX_DIR = ROOT / "assets" / "liberthia" / "textures" / "item"
SPELL_MODEL_DIR = ROOT / "assets" / "liberthia" / "models" / "item"

# ===== Effects: 20 textures =====
EFFECTS = [
    ("dimensional_blindness", (107, 45, 196)),
    ("devil_footsteps",       (204, 17, 0)),
    ("blood_moon_aura",       (170, 0,  51)),
    ("shadow_double",         (17,  17, 17)),
    ("whispers",              (75,  0,  130)),
    ("vertigo",               (102, 255, 153)),
    ("hungry_void",           (0,   0,  51)),
    ("ghost_touch",           (204, 238, 255)),
    ("soul_link",             (255, 102, 170)),
    ("haunted_inventory",     (153, 51, 204)),
    ("mirror_walk",           (204, 255, 255)),
    ("time_dilation",         (139, 127, 191)),
    ("reverse_gravity",       (255, 170, 0)),
    ("magnet_fist",           (255, 102, 51)),
    ("pox_swarm",             (102, 136, 51)),
    ("nightmare",             (75,  0,  128)),
    ("liberthia_blessing",    (255, 224, 102)),
    ("crystal_bloom",         (255, 153, 204)),
    ("ominous_aura",          (85,  0,  0)),
    ("stardust",              (170, 255, 255)),
]


def gen_effect_texture(out_path, color, seed):
    """16×16 procedural icon with concentric ring + symbol in given color."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r, g, b = color
    # Background circle (dark version of color)
    dark = (max(0, r-100), max(0, g-100), max(0, b-100))
    for x in range(16):
        for y in range(16):
            dx, dy = x-7.5, y-7.5
            dist = math.sqrt(dx*dx+dy*dy)
            if dist <= 7:
                d.point((x, y), fill=(*dark, 255))
            elif dist <= 7.8:
                d.point((x, y), fill=(*dark, 200))
    # Bright pattern based on seed
    pattern_type = seed % 5
    if pattern_type == 0:  # cross
        for i in range(3, 13):
            d.point((i, 8), fill=(*color, 255))
            d.point((8, i), fill=(*color, 255))
    elif pattern_type == 1:  # star
        for i in range(4):
            d.point((8, 4+i), fill=(*color, 255))
            d.point((8, 12-i), fill=(*color, 255))
            d.point((4+i, 8), fill=(*color, 255))
            d.point((12-i, 8), fill=(*color, 255))
        # Diagonals
        for i in range(3):
            d.point((6+i, 6+i), fill=(*color, 255))
            d.point((10-i, 6+i), fill=(*color, 255))
    elif pattern_type == 2:  # ring of dots
        for ang in range(0, 360, 45):
            rad = math.radians(ang)
            x = int(8 + math.cos(rad) * 4)
            y = int(8 + math.sin(rad) * 4)
            d.point((x, y), fill=(*color, 255))
        d.point((8, 8), fill=(*color, 255))
    elif pattern_type == 3:  # spiral
        for t in range(20):
            ang = t * 0.6
            rad = 0.3 * t
            x = int(8 + math.cos(ang) * rad)
            y = int(8 + math.sin(ang) * rad)
            if 0 <= x < 16 and 0 <= y < 16:
                d.point((x, y), fill=(*color, 255))
    else:  # triangle
        for i in range(7):
            d.point((8, 4+i), fill=(*color, 255))
            for j in range(i+1):
                d.point((8-j, 4+i), fill=(*color, 255))
                d.point((8+j, 4+i), fill=(*color, 255))
    # Highlight
    d.point((6, 6), fill=(255, 255, 255, 255))
    img.save(out_path)


# ===== Spells: 64 new ones (categorized) =====
def s(spell_id, name, school, rarity, mana, cooldown, dmg, type_, lore, behavior=None, vfx=None, effects=None,
      element=None, category=None):
    return {
        "id": "factory_" + spell_id,
        "name": name,
        "school": school,
        "rarity": rarity,
        "mana": mana,
        "cooldown": cooldown,
        "damage": dmg,
        "range": 24,
        "type": type_,
        "lore": lore,
        "behavior": behavior or {"speed": 1.5, "lifetime_ticks": 60, "pierce": False, "aoe_radius": 2},
        "vfx": vfx or {"color_primary": "#aa66ff", "color_secondary": "#ffffff",
                       "trail_density": 8, "trail_size": 1.2, "impact_scale": 1.0,
                       "impact_particles": 60, "screen_shake": 0.3, "impact_light": 10},
        "effects": effects or [],
        "element": element or school,
        "category": category or "PROJECTILE",
    }


def get_spells():
    out = []

    # ─── 18 spells exploring new custom effects (each applies one of the 20 effects) ───
    out.append(s("dim_blind_curse", "Maldição da Cegueira Dimensional", "ELDRITCH", "EPIC", 60, 200, 6, "TARGETED",
                 "Inflige cegueira dimensional — vítima vê pelo olho de outro jogador.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"dimensional_blindness","duration":200,"magnitude":0.0}],
                 vfx={"color_primary":"#6b2dc4","color_secondary":"#aa00ff","trail_density":12,"trail_size":1.4,
                      "impact_scale":1.0,"impact_particles":40,"screen_shake":0.5,"impact_light":12}, category="UTILITY"))
    out.append(s("devil_walk", "Caminhar do Diabo", "BLOOD", "RARE", 40, 150, 0, "SELF_BUFF",
                 "Cada passo deixa magic fire blood no chão.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"devil_footsteps","duration":600}], category="UTILITY",
                 vfx={"color_primary":"#cc1100","color_secondary":"#ff6600","trail_density":10,"trail_size":1.2,
                      "impact_scale":0.8,"impact_particles":30,"screen_shake":0.2,"impact_light":15}))
    out.append(s("blood_moon_aura", "Aura da Lua Sangrenta", "BLOOD", "EPIC", 70, 300, 0, "AURA",
                 "Inimigos próximos ganham blood_infection.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"blood_moon_aura","duration":400}], category="AOE",
                 vfx={"color_primary":"#aa0033","color_secondary":"#ff0044","trail_density":15,"trail_size":1.5,
                      "impact_scale":1.5,"impact_particles":80,"screen_shake":0.6,"impact_light":12}))
    out.append(s("shadow_clone", "Clone Sombrio", "ELDRITCH", "UNCOMMON", 30, 100, 0, "SELF_BUFF",
                 "Um duplo sombrio segue você.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"shadow_double","duration":600}], category="UTILITY"))
    out.append(s("whispers_curse", "Sussurros Eternos", "ELDRITCH", "RARE", 35, 120, 0, "TARGETED",
                 "Sons assustadores sussurram aos ouvidos da vítima.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"whispers","duration":400}], category="UTILITY"))
    out.append(s("vertigo_curse", "Vertigem", "ELDRITCH", "UNCOMMON", 25, 80, 0, "TARGETED",
                 "Confusão constante na vítima.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"vertigo","duration":400}], category="UTILITY"))
    out.append(s("void_pull", "Sucção do Vazio", "ELDRITCH", "RARE", 40, 100, 0, "SELF_BUFF",
                 "Items próximos são puxados pra você.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"hungry_void","duration":400}], category="UTILITY"))
    out.append(s("ghost_step", "Passo Fantasma", "ELDRITCH", "UNCOMMON", 25, 80, 0, "SELF_BUFF",
                 "Slow-falling permanente.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"ghost_touch","duration":300}], category="UTILITY"))
    out.append(s("soul_link", "Vínculo de Alma", "HOLY", "RARE", 50, 150, 0, "TARGETED",
                 "Liga a alma de um aliado à sua.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"soul_link","duration":600}], category="UTILITY"))
    out.append(s("haunted_inv", "Inventário Assombrado", "ELDRITCH", "EPIC", 60, 200, 0, "TARGETED",
                 "Items na hotbar trocam de posição aleatoriamente.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"haunted_inventory","duration":300}], category="UTILITY"))
    out.append(s("mirror_walk", "Andar no Espelho", "ICE", "UNCOMMON", 30, 100, 0, "SELF_BUFF",
                 "Aura espelhada gera glow particles.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"mirror_walk","duration":400}], category="UTILITY"))
    out.append(s("time_dilate", "Dilatação Temporal", "ELDRITCH", "EPIC", 60, 200, 0, "AURA",
                 "Inimigos próximos ficam lentos.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"time_dilation","duration":400}], category="AOE"))
    out.append(s("levitate_self", "Gravidade Reversa", "ELDRITCH", "RARE", 50, 150, 0, "SELF_BUFF",
                 "Levitação periódica.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"reverse_gravity","duration":400}], category="UTILITY"))
    out.append(s("magnet_pull", "Punho Magnético", "LIGHTNING", "UNCOMMON", 25, 80, 0, "SELF_BUFF",
                 "Items voam pra você de longe.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"magnet_fist","duration":300}], category="UTILITY"))
    out.append(s("pox_swarm", "Enxame Pútrido", "NATURE", "RARE", 40, 120, 0, "AURA",
                 "Particles de praga ao redor; inimigos cegam.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"pox_swarm","duration":400}], category="AOE"))
    out.append(s("nightmare", "Pesadelo", "ELDRITCH", "EPIC", 65, 250, 0, "TARGETED",
                 "Pesadelos constantes na vítima.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"nightmare","duration":600}], category="UTILITY"))
    out.append(s("blessing", "Bênção de Liberthia", "HOLY", "LEGENDARY", 80, 300, 0, "SELF_BUFF",
                 "Cura periódica + glowing.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"liberthia_blessing","duration":400}], category="UTILITY"))
    out.append(s("crystal_bloom", "Floração Cristal", "NATURE", "UNCOMMON", 25, 80, 0, "SELF_BUFF",
                 "Onde você pisa, flores brotam.",
                 effects=[{"type":"CUSTOM_EFFECT","effect":"crystal_bloom","duration":300}], category="UTILITY"))

    # ─── 18 utility/healing/CC ───
    out.append(s("group_heal", "Cura em Grupo", "HOLY", "RARE", 50, 100, 0, "AURA",
                 "Cura 8HP em todos aliados próximos.",
                 effects=[{"type":"HEAL","magnitude":8}], category="AOE"))
    out.append(s("regeneration_field", "Campo de Regeneração", "HOLY", "UNCOMMON", 30, 80, 0, "AURA",
                 "Regen II em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:regeneration","duration":200,"magnitude":1}], category="AOE"))
    out.append(s("mass_slow", "Lentidão em Massa", "ICE", "UNCOMMON", 25, 80, 0, "AURA",
                 "Slowness II em inimigos.",
                 effects=[{"type":"EFFECT","effect":"minecraft:slowness","duration":300,"magnitude":1}], category="AOE"))
    out.append(s("mass_root", "Raízes em Massa", "NATURE", "RARE", 45, 150, 4, "AURA",
                 "Raízes prendem inimigos próximos.",
                 effects=[{"type":"EFFECT","effect":"minecraft:slowness","duration":200,"magnitude":4}], category="AOE"))
    out.append(s("mass_silence", "Silêncio em Massa", "ELDRITCH", "RARE", 40, 120, 0, "AURA",
                 "Silenciar inimigos (mining_fatigue).",
                 effects=[{"type":"EFFECT","effect":"minecraft:mining_fatigue","duration":300,"magnitude":4}], category="AOE"))
    out.append(s("group_haste", "Pressa em Grupo", "LIGHTNING", "UNCOMMON", 25, 80, 0, "AURA",
                 "Haste II em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:haste","duration":400,"magnitude":1}], category="AOE"))
    out.append(s("group_strength", "Força em Grupo", "FIRE", "RARE", 40, 120, 0, "AURA",
                 "Strength II em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:strength","duration":400,"magnitude":1}], category="AOE"))
    out.append(s("group_resist", "Resistência em Grupo", "HOLY", "RARE", 40, 120, 0, "AURA",
                 "Resistance II em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:resistance","duration":400,"magnitude":1}], category="AOE"))
    out.append(s("group_invis", "Invisibilidade em Grupo", "ELDRITCH", "EPIC", 60, 200, 0, "AURA",
                 "Invisibility em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:invisibility","duration":400,"magnitude":0}], category="AOE"))
    out.append(s("group_glow", "Aura Brilhante", "HOLY", "UNCOMMON", 20, 60, 0, "AURA",
                 "Glowing em todos próximos.",
                 effects=[{"type":"EFFECT","effect":"minecraft:glowing","duration":600,"magnitude":0}], category="AOE"))
    out.append(s("mana_burst", "Explosão Manica", "ELDRITCH", "RARE", 45, 100, 6, "EXPLOSION",
                 "Explosão de magia pura.",
                 effects=[{"type":"KNOCKBACK","magnitude":2}], category="EXPLOSION"))
    out.append(s("blink_step", "Piscar de Olhos", "ELDRITCH", "UNCOMMON", 20, 50, 0, "DASH",
                 "Teleporta 8 blocos à frente.",
                 effects=[{"type":"TELEPORT","magnitude":8}], category="DASH"))
    out.append(s("purify_aura", "Aura Purificadora", "HOLY", "RARE", 50, 150, 0, "AURA",
                 "Remove debuffs negativos de aliados.",
                 effects=[{"type":"PURIFY","magnitude":1}], category="AOE"))
    out.append(s("mana_drain", "Drenar Mana", "ELDRITCH", "UNCOMMON", 25, 80, 5, "TARGETED",
                 "Drena mana do alvo e te devolve.",
                 effects=[{"type":"DRAIN_MANA","magnitude":30}], category="UTILITY"))
    out.append(s("life_drain", "Drenar Vida", "BLOOD", "RARE", 40, 150, 6, "TARGETED",
                 "Drena vida e cura você.",
                 effects=[{"type":"LIFESTEAL","magnitude":1}], category="UTILITY"))
    out.append(s("group_speed", "Velocidade em Grupo", "LIGHTNING", "UNCOMMON", 20, 60, 0, "AURA",
                 "Speed II em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:speed","duration":400,"magnitude":1}], category="AOE"))
    out.append(s("group_jump", "Salto em Grupo", "NATURE", "UNCOMMON", 20, 60, 0, "AURA",
                 "Jump Boost II em aliados.",
                 effects=[{"type":"EFFECT","effect":"minecraft:jump_boost","duration":400,"magnitude":1}], category="AOE"))
    out.append(s("dispell", "Dispelir", "HOLY", "RARE", 40, 100, 0, "TARGETED",
                 "Remove todos buffs do alvo.",
                 effects=[{"type":"DISPELL","magnitude":1}], category="UTILITY"))

    # ─── 10 dano crítico (meteoros etc) ───
    for i, (name, school, dmg, lore) in enumerate([
        ("Chuva de Meteoros", "FIRE", 25, "Múltiplos meteoros caem em área."),
        ("Meteoro Sangrento", "BLOOD", 30, "Meteoro de sangue impacta com terremoto."),
        ("Cometa Glacial", "ICE", 28, "Cometa de gelo congela área."),
        ("Asteroide Eldritch", "ELDRITCH", 35, "Asteroide do vazio."),
        ("Pillar Divino", "HOLY", 32, "Coluna divina cai do céu."),
        ("Raio Cosmic", "LIGHTNING", 30, "Raio cósmico devastador."),
        ("Tempestade Meteoritica", "FIRE", 40, "Tempestade contínua de pedras flamejantes."),
        ("Lança da Aniquilação", "ELDRITCH", 45, "Lança massiva do espaço."),
        ("Choque Sísmico", "NATURE", 28, "Onda sísmica em área ampla."),
        ("Bomba Atômica Arcana", "ELDRITCH", 50, "Detonação massiva, único ultimate."),
    ]):
        out.append(s(f"meteor_{i:02d}", name, school, "EPIC" if dmg < 35 else "LEGENDARY", 80+dmg, 400, dmg,
                     "AOE_BURST", lore, category="DESTRUCTION",
                     vfx={"color_primary":"#ff5500","color_secondary":"#ffaa00","trail_density":20,"trail_size":2.0,
                          "impact_scale":2.5,"impact_particles":120,"screen_shake":1.0,"impact_light":15}))

    # ─── 5 dash variants ───
    for i, (name, school, dmg, lore) in enumerate([
        ("Investida Vulcânica", "FIRE", 8, "Dash em chamas."),
        ("Investida Gelada", "ICE", 7, "Dash em gelo congelante."),
        ("Investida Sangrenta", "BLOOD", 10, "Dash que rouba vida."),
        ("Investida da Luz", "HOLY", 6, "Dash com proteção divina."),
        ("Investida Sombria", "ELDRITCH", 9, "Dash através de dimensões."),
    ]):
        out.append(s(f"dash_{i:02d}", name, school, "RARE", 30+i*5, 80, dmg, "DASH", lore, category="DASH",
                     vfx={"color_primary":"#aa6633","color_secondary":"#ff9966","trail_density":15,"trail_size":1.5,
                          "impact_scale":1.2,"impact_particles":40,"screen_shake":0.4,"impact_light":10}))

    # ─── 5 imortalidade ───
    for i, (name, school, lore) in enumerate([
        ("Pacto Imortal", "HOLY", "Imune a dano por 10s."),
        ("Forma Etérea", "ELDRITCH", "Intangível por 6s."),
        ("Casca Adamantina", "NATURE", "Resistência infinita por 5s."),
        ("Sangue Eterno", "BLOOD", "Regen massivo por 8s."),
        ("Tempo Parado", "ICE", "Slow time effect."),
    ]):
        out.append(s(f"immortal_{i:02d}", name, school, "LEGENDARY", 100, 600, 0, "SELF_BUFF", lore,
                     effects=[{"type":"EFFECT","effect":"minecraft:resistance","duration":200,"magnitude":4}],
                     category="UTILITY",
                     vfx={"color_primary":"#FFE066","color_secondary":"#ffffff","trail_density":18,"trail_size":1.8,
                          "impact_scale":1.5,"impact_particles":80,"screen_shake":0.5,"impact_light":15}))

    # ─── 8 movimento ───
    for i, (name, school, lore, effect) in enumerate([
        ("Asas Solares", "FIRE", "Voar com asas de fogo.", "minecraft:slow_falling"),
        ("Passo do Vento", "NATURE", "Velocidade máxima.", "minecraft:speed"),
        ("Caminhar nas Águas", "HOLY", "Andar sobre água.", "minecraft:water_breathing"),
        ("Salto da Lua", "ELDRITCH", "Salto altíssimo.", "minecraft:jump_boost"),
        ("Patinar no Gelo", "ICE", "Slide rápido.", "minecraft:speed"),
        ("Forma Líquida", "ICE", "Atravessar tudo.", "minecraft:invisibility"),
        ("Galope Sanguíneo", "BLOOD", "Mais speed se ferido.", "minecraft:speed"),
        ("Aceleração Quântica", "LIGHTNING", "Speed máximo + jump.", "minecraft:speed"),
    ]):
        out.append(s(f"movement_{i:02d}", name, school, "UNCOMMON" if i<4 else "RARE", 30+i*4, 80, 0,
                     "SELF_BUFF", lore,
                     effects=[{"type":"EFFECT","effect":effect,"duration":300,"magnitude":2}], category="DASH"))

    return out


def main():
    EFFECT_TEX_DIR.mkdir(parents=True, exist_ok=True)
    SPELLS_DIR.mkdir(parents=True, exist_ok=True)

    # Generate 20 effect textures
    for i, (name, color) in enumerate(EFFECTS):
        gen_effect_texture(EFFECT_TEX_DIR / (name + ".png"), color, i)
    print("OK — generated %d effect textures" % len(EFFECTS))

    # Generate 64 spell JSONs
    spells = get_spells()
    for spell in spells:
        # Use just the id field as filename (already prefixed with "factory_")
        fname = spell["id"] + ".json"
        (SPELLS_DIR / fname).write_text(json.dumps(spell, indent=2, ensure_ascii=False), encoding="utf-8")
    print("OK — generated %d spell JSONs" % len(spells))


if __name__ == "__main__":
    main()
