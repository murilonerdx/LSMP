"""Generate lang entries for all magic/horror items + blocks."""

import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
LANG_DIR = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "liberthia", "lang")

# (key, pt-br, en-us)
ENTRIES = [
    # ─── Creative tabs (renomeadas) ───
    ("itemGroup.liberthia.magic_arsenal", "Liberthia: Magia", "Liberthia: Magic"),
    ("itemGroup.liberthia.horror_framework", "Liberthia: Horror", "Liberthia: Horror"),

    # ─── ORBS ───
    ("item.liberthia.orb_mana_boost", "Orbe de Mana", "Mana Orb"),
    ("item.liberthia.orb_cooldown", "Orbe de Velocidade", "Cooldown Orb"),
    ("item.liberthia.orb_spell_damage", "Orbe de Dano Mágico", "Spell Damage Orb"),
    ("item.liberthia.orb_health", "Orbe da Vitalidade", "Health Orb"),
    ("item.liberthia.orb_source_regen", "Orbe de Regeneração de Source", "Source Regen Orb"),

    # ─── FOCUS gemstones ───
    ("item.liberthia.focus_fire", "Foco de Fogo", "Fire Focus"),
    ("item.liberthia.focus_ice", "Foco de Gelo", "Ice Focus"),
    ("item.liberthia.focus_lightning", "Foco do Raio", "Lightning Focus"),
    ("item.liberthia.focus_blood", "Foco de Sangue", "Blood Focus"),
    ("item.liberthia.focus_eldritch", "Foco Eldritch", "Eldritch Focus"),
    ("item.liberthia.focus_holy", "Foco Sagrado", "Holy Focus"),
    ("item.liberthia.focus_nature", "Foco da Natureza", "Nature Focus"),

    # ─── Iconic Spells ───
    ("item.liberthia.spectral_hammer", "Martelo Espectral", "Spectral Hammer"),
    ("item.liberthia.lightning_lance", "Lança do Raio", "Lightning Lance"),
    ("item.liberthia.devour_jaw", "Mandíbula Devoradora", "Devour Jaw"),
    ("item.liberthia.ice_tomb_spell", "Tumba de Gelo", "Ice Tomb"),
    ("item.liberthia.ascension_spell", "Ascensão", "Ascension"),
    ("item.liberthia.heat_surge_spell", "Onda de Calor", "Heat Surge"),
    ("item.liberthia.spell_bow", "Arco de Magia", "Spell Bow"),

    # ─── Utility ───
    ("item.liberthia.jar_of_light", "Jarro da Luz", "Jar of Light"),
    ("item.liberthia.enchanters_gauntlet", "Manopla do Encantador", "Enchanter's Gauntlet"),
    ("item.liberthia.repository", "Repositório", "Repository"),
    ("block.liberthia.repository", "Repositório", "Repository"),

    # ─── Crops ───
    ("item.liberthia.mage_bloom_seed", "Semente de Mage Bloom", "Mage Bloom Seed"),
    ("item.liberthia.mage_bloom_fiber", "Fibra de Mage Bloom", "Mage Bloom Fiber"),
    ("item.liberthia.source_berry", "Baga de Source", "Source Berry"),

    # ─── Wizard armor — Chestplates ───
    ("item.liberthia.wizard_chest_fire", "Peitoral do Mago do Fogo", "Pyromancer Chestplate"),
    ("item.liberthia.wizard_chest_ice", "Peitoral do Mago do Gelo", "Cryomancer Chestplate"),
    ("item.liberthia.wizard_chest_lightning", "Peitoral do Mago do Raio", "Electromancer Chestplate"),
    ("item.liberthia.wizard_chest_blood", "Peitoral do Mago do Sangue", "Blood Mage Chestplate"),
    ("item.liberthia.wizard_chest_eldritch", "Peitoral Eldritch", "Eldritch Chestplate"),
    ("item.liberthia.wizard_chest_holy", "Peitoral Sagrado", "Holy Chestplate"),
    ("item.liberthia.wizard_chest_nature", "Peitoral da Natureza", "Nature Chestplate"),

    # ─── Wizard armor — Helmets / Legs / Boots (21 items) ───
    ("item.liberthia.wizard_helm_fire", "Capacete do Mago do Fogo", "Pyromancer Helm"),
    ("item.liberthia.wizard_legs_fire", "Calças do Mago do Fogo", "Pyromancer Legs"),
    ("item.liberthia.wizard_boots_fire", "Botas do Mago do Fogo", "Pyromancer Boots"),
    ("item.liberthia.wizard_helm_ice", "Capacete do Mago do Gelo", "Cryomancer Helm"),
    ("item.liberthia.wizard_legs_ice", "Calças do Mago do Gelo", "Cryomancer Legs"),
    ("item.liberthia.wizard_boots_ice", "Botas do Mago do Gelo", "Cryomancer Boots"),
    ("item.liberthia.wizard_helm_lightning", "Capacete do Mago do Raio", "Electromancer Helm"),
    ("item.liberthia.wizard_legs_lightning", "Calças do Mago do Raio", "Electromancer Legs"),
    ("item.liberthia.wizard_boots_lightning", "Botas do Mago do Raio", "Electromancer Boots"),
    ("item.liberthia.wizard_helm_blood", "Capacete do Mago do Sangue", "Blood Mage Helm"),
    ("item.liberthia.wizard_legs_blood", "Calças do Mago do Sangue", "Blood Mage Legs"),
    ("item.liberthia.wizard_boots_blood", "Botas do Mago do Sangue", "Blood Mage Boots"),
    ("item.liberthia.wizard_helm_eldritch", "Capacete Eldritch", "Eldritch Helm"),
    ("item.liberthia.wizard_legs_eldritch", "Calças Eldritch", "Eldritch Legs"),
    ("item.liberthia.wizard_boots_eldritch", "Botas Eldritch", "Eldritch Boots"),
    ("item.liberthia.wizard_helm_holy", "Capacete Sagrado", "Holy Helm"),
    ("item.liberthia.wizard_legs_holy", "Calças Sagradas", "Holy Legs"),
    ("item.liberthia.wizard_boots_holy", "Botas Sagradas", "Holy Boots"),
    ("item.liberthia.wizard_helm_nature", "Capacete da Natureza", "Nature Helm"),
    ("item.liberthia.wizard_legs_nature", "Calças da Natureza", "Nature Legs"),
    ("item.liberthia.wizard_boots_nature", "Botas da Natureza", "Nature Boots"),

    # ─── Magic Blocks ───
    ("block.liberthia.magic_fire_fire", "Fogo Mágico", "Magic Fire"),
    ("block.liberthia.magic_fire_ice", "Fogo Mágico do Gelo", "Ice Magic Fire"),
    ("block.liberthia.magic_fire_lightning", "Fogo Mágico do Raio", "Lightning Magic Fire"),
    ("block.liberthia.magic_fire_blood", "Fogo Mágico de Sangue", "Blood Magic Fire"),
    ("block.liberthia.magic_fire_eldritch", "Fogo Mágico Eldritch", "Eldritch Magic Fire"),
    ("block.liberthia.magic_fire_holy", "Fogo Mágico Sagrado", "Holy Magic Fire"),
    ("block.liberthia.magic_fire_nature", "Fogo Mágico Natural", "Nature Magic Fire"),
    ("block.liberthia.magelight_torch", "Tocha Mágica", "Magelight Torch"),
    ("block.liberthia.mob_jar", "Jarro de Mobs", "Mob Jar"),
    ("block.liberthia.potion_jar", "Jarro de Poção", "Potion Jar"),
    ("block.liberthia.scryer_oculus", "Óculo de Vidência", "Scryer Oculus"),
    ("block.liberthia.fire_wall", "Muro de Fogo", "Fire Wall"),
    ("block.liberthia.ice_wall", "Muro de Gelo", "Ice Wall"),
    ("block.liberthia.lightning_wall", "Muro do Raio", "Lightning Wall"),
    ("block.liberthia.holy_wall", "Muro Sagrado", "Holy Wall"),
    ("block.liberthia.whirlwind", "Turbilhão Mágico", "Whirlwind"),
    ("block.liberthia.auto_miner", "Minerador Automático", "Auto Miner"),
    ("block.liberthia.mage_cauldron", "Caldeirão do Mago", "Mage Cauldron"),
    ("block.liberthia.inscription_table", "Mesa de Inscrição", "Inscription Table"),
    ("block.liberthia.scroll_forge", "Forja de Pergaminhos", "Scroll Forge"),
    ("block.liberthia.spell_prism", "Prisma de Feitiço", "Spell Prism"),
    ("block.liberthia.spell_turret", "Torreta de Feitiço", "Spell Turret"),
    ("block.liberthia.spell_sensor", "Sensor de Feitiço", "Spell Sensor"),
    ("block.liberthia.ritual_brazier", "Braseiro de Ritual", "Ritual Brazier"),
    ("block.liberthia.volcanic_sourcelink", "Sourcelink Vulcânico", "Volcanic Sourcelink"),
    ("block.liberthia.mycelial_sourcelink", "Sourcelink Micelial", "Mycelial Sourcelink"),
    ("block.liberthia.vitalic_sourcelink", "Sourcelink Vitálico", "Vitalic Sourcelink"),
    ("block.liberthia.alchemical_sourcelink", "Sourcelink Alquímico", "Alchemical Sourcelink"),
    ("block.liberthia.weave_basic", "Trama Mágica", "Magic Weave"),
    ("block.liberthia.weave_chest", "Baú de Trama", "Weave Chest"),
    ("block.liberthia.weave_holy", "Trama Sagrada", "Holy Weave"),

    # ─── Cosmic Horror Items ───
    ("item.liberthia.mirror_mask", "Máscara Espelhada", "Mirror Mask"),
    ("item.liberthia.fractured_scripture", "Escritura Fragmentada", "Fractured Scripture"),
    ("item.liberthia.halo_of_abaddon", "Auréola de Abaddon", "Halo of Abaddon"),
    ("item.liberthia.veinbound_chestplate", "Peitoral Venoso", "Veinbound Chestplate"),

    # ─── Cosmic Horror Eggs ───
    ("item.liberthia.empty_man_egg", "Ovo de Homem-Vazio", "Empty Man Egg"),
    ("item.liberthia.observer_egg", "Ovo de Observador", "Observer Egg"),
    ("item.liberthia.absence_egg", "Ovo da Ausência", "Absence Egg"),
    ("item.liberthia.remembered_egg", "Ovo do Lembrado", "Remembered Egg"),

    # ─── Wizard Eggs ───
    ("item.liberthia.pyromancer_egg", "Ovo de Piromante", "Pyromancer Egg"),
    ("item.liberthia.cryomancer_egg", "Ovo de Criomante", "Cryomancer Egg"),
    ("item.liberthia.electromancer_egg", "Ovo de Eletromante", "Electromancer Egg"),
    ("item.liberthia.necromancer_egg", "Ovo de Necromante", "Necromancer Egg"),
    ("item.liberthia.eldritch_cultist_egg", "Ovo de Cultista Eldritch", "Eldritch Cultist Egg"),
    ("item.liberthia.apothecarist_egg", "Ovo de Boticário", "Apothecarist Egg"),
    ("item.liberthia.keeper_egg", "Ovo de Guardião", "Keeper Egg"),
    ("item.liberthia.archevoker_egg", "Ovo de Arquievocador", "Archevoker Egg"),

    # ─── Familiar Eggs ───
    ("item.liberthia.wisp_picker_egg", "Ovo de Wisp Coletor", "Wisp Picker Egg"),
    ("item.liberthia.grove_sprite_egg", "Ovo de Sprite do Bosque", "Grove Sprite Egg"),
    ("item.liberthia.soul_reaper_egg", "Ovo de Ceifador de Almas", "Soul Reaper Egg"),
    ("item.liberthia.whelp_egg", "Ovo de Whelp", "Whelp Egg"),
    ("item.liberthia.carbuncle_egg", "Ovo de Carbúnculo", "Carbuncle Egg"),
    ("item.liberthia.amethyst_golem_egg", "Ovo de Golem de Ametista", "Amethyst Golem Egg"),

    # ─── Boss Eggs ───
    ("item.liberthia.abyssal_lich_egg", "Ovo de Lich Abissal", "Abyssal Lich Egg"),
    ("item.liberthia.lich_stalker_egg", "Ovo de Espreitador do Lich", "Lich Stalker Egg"),
    ("item.liberthia.lich_hunter_egg", "Ovo de Caçador do Lich", "Lich Hunter Egg"),

    # ─── Entity names ───
    ("entity.liberthia.empty_man", "Homem-Vazio", "Empty Man"),
    ("entity.liberthia.observer", "Observador", "Observer"),
    ("entity.liberthia.absence", "Ausência", "Absence"),
    ("entity.liberthia.remembered", "Lembrado", "Remembered"),
    ("entity.liberthia.pyromancer", "Piromante", "Pyromancer"),
    ("entity.liberthia.cryomancer", "Criomante", "Cryomancer"),
    ("entity.liberthia.electromancer", "Eletromante", "Electromancer"),
    ("entity.liberthia.necromancer", "Necromante", "Necromancer"),
    ("entity.liberthia.eldritch_cultist", "Cultista Eldritch", "Eldritch Cultist"),
    ("entity.liberthia.apothecarist", "Boticário", "Apothecarist"),
    ("entity.liberthia.keeper", "Guardião Sagrado", "Holy Keeper"),
    ("entity.liberthia.archevoker", "Arquievocador", "Archevoker"),
    ("entity.liberthia.wisp_picker", "Wisp Coletor", "Wisp Picker"),
    ("entity.liberthia.grove_sprite", "Sprite do Bosque", "Grove Sprite"),
    ("entity.liberthia.soul_reaper", "Ceifador de Almas", "Soul Reaper"),
    ("entity.liberthia.whelp", "Whelp", "Whelp"),
    ("entity.liberthia.carbuncle", "Carbúnculo", "Carbuncle"),
    ("entity.liberthia.amethyst_golem", "Golem de Ametista", "Amethyst Golem"),
    ("entity.liberthia.abyssal_lich", "Lich Abissal", "Abyssal Lich"),
    ("entity.liberthia.lich_stalker", "Espreitador do Lich", "Lich Stalker"),
    ("entity.liberthia.lich_hunter", "Caçador do Lich", "Lich Hunter"),
    ("entity.liberthia.frozen_humanoid", "Estátua Congelada", "Frozen Statue"),
]


def merge_lang(path, lang_idx):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    added = 0
    for key, pt, en in ENTRIES:
        val = pt if lang_idx == 0 else en
        if key not in data:
            data[key] = val
            added += 1
        else:
            # Update existing for fresh translations
            data[key] = val
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    return added


pt_added = merge_lang(os.path.join(LANG_DIR, "pt_br.json"), 0)
en_added = merge_lang(os.path.join(LANG_DIR, "en_us.json"), 1)
print(f"pt_br: +{pt_added} entries / en_us: +{en_added} entries (total {len(ENTRIES)} keys)")
