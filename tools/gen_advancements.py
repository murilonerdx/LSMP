"""r130: gera arvore de advancements pra Liberthia.

5 tabs/categorias:
- MATTER (Dark/Clear/Yellow matter — caminho de tech)
- MAGIC (spells, glyphs, grimoire, weaver)
- SPIRIT (Spirit World exploration)
- COSMIC (Cosmic Horror items)
- BOSSES (boss kills)

Cada advancement tem:
- icon (item)
- title + description
- criteria (got_item / killed_entity / etc)
- parent (chain)
"""
import os, json

ROOT = os.path.dirname(__file__)
ADV_ROOT = os.path.normpath(os.path.join(ROOT, "..",
    "src/main/resources/data/liberthia/advancements"))


def adv(category, name, title_en, title_pt, desc_en, desc_pt,
        icon_item, criteria_type="impossible", criteria_value=None,
        parent=None, frame="task", background=None, hidden=False):
    """Cria 1 advancement JSON."""
    crit = {}
    if criteria_type == "impossible":
        crit["root"] = {"trigger": "minecraft:impossible"}
    elif criteria_type == "inventory_changed":
        crit["has_item"] = {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": [criteria_value]}]}
        }
    elif criteria_type == "killed_entity":
        crit["killed"] = {
            "trigger": "minecraft:player_killed_entity",
            "conditions": {"entity": {"type": criteria_value}}
        }
    elif criteria_type == "changed_dimension":
        crit["entered"] = {
            "trigger": "minecraft:changed_dimension",
            "conditions": {"to": criteria_value}
        }
    elif criteria_type == "consume_item":
        crit["consumed"] = {
            "trigger": "minecraft:consume_item",
            "conditions": {"item": {"items": [criteria_value]}}
        }
    elif criteria_type == "any_item":
        # Multiple items - OR
        crit["got_any"] = {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": criteria_value}]}
        }

    display = {
        "icon": {"item": icon_item},
        "title": {"translate": f"advancement.liberthia.{name}.title"},
        "description": {"translate": f"advancement.liberthia.{name}.description"},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": True,
        "hidden": hidden
    }
    if background:
        display["background"] = background

    j = {
        "display": display,
        "criteria": crit,
        "requirements": [[k] for k in crit.keys()] if len(crit) > 1 else None
    }
    if j["requirements"] is None:
        j.pop("requirements")
    if parent:
        j["parent"] = parent

    # Lang entries (pt_br + en_us)
    lang_en = {
        f"advancement.liberthia.{name}.title": title_en,
        f"advancement.liberthia.{name}.description": desc_en
    }
    lang_pt = {
        f"advancement.liberthia.{name}.title": title_pt,
        f"advancement.liberthia.{name}.description": desc_pt
    }

    path = os.path.join(ADV_ROOT, category, f"{name}.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(j, f, indent=2)

    return lang_en, lang_pt


def main():
    all_lang_en = {}
    all_lang_pt = {}

    # ===============================================================
    # MATTER TAB (8 advancements) — caminho de tech materialista
    # ===============================================================
    e, p = adv("matter", "root",
        "Welcome to Liberthia", "Bem-vindo a Liberthia",
        "Discover the Three Matters", "Descubra as Três Matérias",
        "liberthia:dark_matter_shard",
        background="liberthia:textures/gui/advancements/bg_matter.png",
        frame="task")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "first_dark_matter",
        "Embrace the Dark", "Abrace a Escuridão",
        "Obtain Dark Matter Shard", "Obtenha Fragmento de Matéria Escura",
        "liberthia:dark_matter_shard",
        criteria_type="inventory_changed", criteria_value="liberthia:dark_matter_shard",
        parent="liberthia:matter/root")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "first_clear_matter",
        "Pure Light", "Luz Pura",
        "Obtain Clear Matter Shard", "Obtenha Fragmento de Matéria Clara",
        "liberthia:clear_matter_shard",
        criteria_type="inventory_changed", criteria_value="liberthia:clear_matter_shard",
        parent="liberthia:matter/root")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "first_yellow_matter",
        "Solar Touch", "Toque Solar",
        "Obtain Yellow Matter Shard", "Obtenha Fragmento de Matéria Amarela",
        "liberthia:yellow_matter_shard",
        criteria_type="inventory_changed", criteria_value="liberthia:yellow_matter_shard",
        parent="liberthia:matter/root")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "matter_ingot",
        "Refined", "Refinado",
        "Smelt any Matter Ingot", "Fundir qualquer Lingote de Matéria",
        "liberthia:dark_matter_ingot",
        criteria_type="any_item",
        criteria_value=["liberthia:dark_matter_ingot", "liberthia:clear_matter_ingot",
                        "liberthia:yellow_matter_ingot"],
        parent="liberthia:matter/first_dark_matter")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "matter_extractor",
        "Industrial Age", "Era Industrial",
        "Build a Matter Extractor", "Construa um Extrator de Matéria",
        "liberthia:matter_extractor",
        criteria_type="inventory_changed", criteria_value="liberthia:matter_extractor",
        parent="liberthia:matter/matter_ingot")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "matter_pill",
        "Alchemist", "Alquimista",
        "Brew a Matter Pill", "Prepare uma Pílula de Matéria",
        "liberthia:matter_pill_dm",
        criteria_type="any_item",
        criteria_value=["liberthia:matter_pill_dm", "liberthia:matter_pill_cm",
                        "liberthia:matter_pill_ym"],
        parent="liberthia:matter/matter_extractor")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("matter", "boss_crown",
        "King of Matter", "Rei da Matéria",
        "Wear the Boss Crown", "Use a Coroa do Chefe",
        "liberthia:boss_crown",
        criteria_type="inventory_changed", criteria_value="liberthia:boss_crown",
        parent="liberthia:matter/matter_pill",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    # ===============================================================
    # MAGIC TAB (10 advancements)
    # ===============================================================
    e, p = adv("magic", "root",
        "The Path of Magic", "O Caminho da Magia",
        "Discover Source magic", "Descubra a magia do Source",
        "liberthia:spell_fireball",
        background="liberthia:textures/gui/advancements/bg_magic.png")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "first_spell",
        "Apprentice", "Aprendiz",
        "Cast your first spell", "Lance seu primeiro feitiço",
        "liberthia:spell_fireball",
        criteria_type="any_item",
        criteria_value=["liberthia:spell_fireball", "liberthia:spell_frostbolt",
                        "liberthia:spell_lightning_bolt", "liberthia:spell_magic_missile"],
        parent="liberthia:magic/root")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "first_glyph",
        "Glyph Reader", "Leitor de Glyphs",
        "Obtain any Glyph", "Obtenha qualquer Glyph",
        "liberthia:glyph_fireball",
        criteria_type="any_item",
        criteria_value=["liberthia:glyph_fireball", "liberthia:glyph_method_self",
                        "liberthia:glyph_effect_harm", "liberthia:glyph_amplify"],
        parent="liberthia:magic/first_spell")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "scribes_table",
        "Inscriber", "Inscritor",
        "Build a Scribes Table", "Construa uma Mesa de Escriba",
        "liberthia:scribes_table",
        criteria_type="inventory_changed", criteria_value="liberthia:scribes_table",
        parent="liberthia:magic/first_glyph")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "grimoire",
        "Spell Collector", "Colecionador de Feitiços",
        "Craft the Grimoire Book", "Crafte o Livro Grimório",
        "liberthia:grimoire_book",
        criteria_type="inventory_changed", criteria_value="liberthia:grimoire_book",
        parent="liberthia:magic/scribes_table")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "spell_weaver",
        "Weaver of Reality", "Tecelão da Realidade",
        "Build a Spell Weaver", "Construa um Tear de Feitiços",
        "liberthia:spell_weaver",
        criteria_type="inventory_changed", criteria_value="liberthia:spell_weaver",
        parent="liberthia:magic/grimoire")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "glyph_inscriber",
        "Master Inscriber", "Inscritor Mestre",
        "Build a Glyph Inscriber", "Construa uma Inscrição de Glyph",
        "liberthia:glyph_inscriber",
        criteria_type="inventory_changed", criteria_value="liberthia:glyph_inscriber",
        parent="liberthia:magic/scribes_table")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "apolao_spell",
        "Apollo Tier", "Tier Apolão",
        "Obtain an Apolão (legendary) spell scroll", "Obtenha um feitiço Apolão (lendário)",
        "liberthia:spell_apocalypse",
        criteria_type="any_item",
        criteria_value=["liberthia:spell_solar_apocalypse", "liberthia:spell_eldritch_meteor",
                        "liberthia:spell_apocalypse", "liberthia:spell_void"],
        parent="liberthia:magic/spell_weaver",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "spirit_robes",
        "Robes of the Spirit", "Mantos do Espírito",
        "Wear full Spirit Robes set", "Use o conjunto completo de Mantos do Espírito",
        "liberthia:spirit_robes_chest",
        criteria_type="inventory_changed", criteria_value="liberthia:spirit_robes_chest",
        parent="liberthia:magic/scribes_table")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("magic", "void_spell",
        "Embrace the Void", "Abrace o Vazio",
        "Obtain Void laser spell", "Obtenha o feitiço Vazio Laser",
        "liberthia:spell_void_laser",
        criteria_type="inventory_changed", criteria_value="liberthia:spell_void_laser",
        parent="liberthia:magic/apolao_spell",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    # ===============================================================
    # SPIRIT WORLD TAB (6 advancements)
    # ===============================================================
    e, p = adv("spirit", "root",
        "Beyond the Veil", "Além do Véu",
        "Discover the Spirit World", "Descubra o Mundo Espiritual",
        "liberthia:spiritual_connection",
        background="liberthia:textures/gui/advancements/bg_spirit.png")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("spirit", "enter_spirit",
        "Crossing Over", "A Travessia",
        "Enter the Spirit World", "Entre no Mundo Espiritual",
        "liberthia:spiritual_link",
        criteria_type="changed_dimension", criteria_value="liberthia:spirit_world",
        parent="liberthia:spirit/root")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("spirit", "sourcestone",
        "Source of Power", "Fonte de Poder",
        "Mine Sourcestone Ore", "Minere Minério de Sourcestone",
        "liberthia:sourcestone_ore",
        criteria_type="any_item",
        criteria_value=["liberthia:sourcestone_ore", "liberthia:sourcestone"],
        parent="liberthia:spirit/enter_spirit")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("spirit", "spirit_conduit",
        "Conduit Builder", "Construtor de Conduítes",
        "Place a Spirit Conduit", "Coloque um Conduíte Espiritual",
        "liberthia:spirit_conduit",
        criteria_type="inventory_changed", criteria_value="liberthia:spirit_conduit",
        parent="liberthia:spirit/sourcestone")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("spirit", "source_transmuter",
        "Dimensional Transmuter", "Transmutador Dimensional",
        "Build the Source Transmuter", "Construa o Transmutador de Source",
        "liberthia:source_transmuter",
        criteria_type="inventory_changed", criteria_value="liberthia:source_transmuter",
        parent="liberthia:spirit/spirit_conduit",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("spirit", "wisp_essence",
        "Spirit Collector", "Coletor de Espíritos",
        "Collect Wisp Essence", "Colete Essência de Wisp",
        "liberthia:wisp_essence",
        criteria_type="inventory_changed", criteria_value="liberthia:wisp_essence",
        parent="liberthia:spirit/enter_spirit")
    all_lang_en.update(e); all_lang_pt.update(p)

    # ===============================================================
    # COSMIC HORROR TAB (8 advancements)
    # ===============================================================
    e, p = adv("cosmic", "root",
        "The Watchers", "Os Observadores",
        "Sense something watching you", "Sinta algo te observando",
        "liberthia:mirror_mask",
        background="liberthia:textures/gui/advancements/bg_cosmic.png")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "first_horror_item",
        "Touched by Madness", "Tocado pela Loucura",
        "Obtain any cursed Cosmic item", "Obtenha qualquer item Cósmico amaldiçoado",
        "liberthia:fractured_scripture",
        criteria_type="any_item",
        criteria_value=["liberthia:mirror_mask", "liberthia:fractured_scripture",
                        "liberthia:halo_of_abaddon", "liberthia:veinbound_chestplate"],
        parent="liberthia:cosmic/root")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "creative_grimoire",
        "Forbidden Knowledge", "Conhecimento Proibido",
        "Find the Creative Grimoire", "Encontre o Grimório Criativo",
        "liberthia:creative_grimoire",
        criteria_type="inventory_changed", criteria_value="liberthia:creative_grimoire",
        parent="liberthia:cosmic/first_horror_item",
        hidden=True)
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "kill_empty_man",
        "Empty Inside", "Vazio Por Dentro",
        "Kill an Empty Man", "Mate um Homem Vazio",
        "liberthia:empty_man_egg",
        criteria_type="killed_entity", criteria_value="liberthia:empty_man",
        parent="liberthia:cosmic/first_horror_item")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "kill_observer",
        "Watch Me Watch You", "Veja-me Vendo Você",
        "Kill an Observer", "Mate um Observador",
        "liberthia:observer_egg",
        criteria_type="killed_entity", criteria_value="liberthia:observer",
        parent="liberthia:cosmic/first_horror_item")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "all_horror_kills",
        "Hunter of the Unseen", "Caçador do Não-Visto",
        "Kill all 4 Cosmic Horror mobs", "Mate todos os 4 mobs Cósmicos",
        "liberthia:halo_of_abaddon",
        criteria_type="impossible",
        parent="liberthia:cosmic/kill_empty_man",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "caretaker_console",
        "The Caretaker Speaks", "O Cuidador Fala",
        "Use the Caretaker Console", "Use o Console do Cuidador",
        "liberthia:caretaker_console",
        criteria_type="inventory_changed", criteria_value="liberthia:caretaker_console",
        parent="liberthia:cosmic/root",
        hidden=True)
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("cosmic", "horror_artifact",
        "Eldritch Collector", "Colecionador Eldritch",
        "Obtain an Eldritch Artifact", "Obtenha um Artefato Eldritch",
        "liberthia:black_veil",
        criteria_type="any_item",
        criteria_value=["liberthia:black_veil", "liberthia:tendril_crown",
                        "liberthia:silent_bell", "liberthia:open_eye"],
        parent="liberthia:cosmic/first_horror_item")
    all_lang_en.update(e); all_lang_pt.update(p)

    # ===============================================================
    # BOSSES TAB (5 advancements)
    # ===============================================================
    e, p = adv("bosses", "root",
        "Champions of Liberthia", "Campeões de Liberthia",
        "Defeat bosses to prove yourself", "Derrote chefes para provar seu valor",
        "liberthia:netherite_seal",
        background="liberthia:textures/gui/advancements/bg_bosses.png")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("bosses", "kill_flesh_mother",
        "Slayer of the Mother", "Matador da Mãe",
        "Kill the Flesh Mother", "Mate a Mãe da Carne",
        "liberthia:bone_seal",
        criteria_type="killed_entity", criteria_value="liberthia:flesh_mother_boss",
        parent="liberthia:bosses/root",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("bosses", "kill_warden",
        "Warden Slayer", "Matador do Warden",
        "Kill the Blood Warden", "Mate o Warden de Sangue",
        "liberthia:gold_seal",
        criteria_type="killed_entity", criteria_value="liberthia:blood_warden",
        parent="liberthia:bosses/root",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("bosses", "kill_lich",
        "Lich Slayer", "Matador do Lich",
        "Defeat the Abyssal Lich", "Derrote o Lich Abissal",
        "liberthia:diamond_seal",
        criteria_type="killed_entity", criteria_value="liberthia:abyssal_lich",
        parent="liberthia:bosses/root",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    e, p = adv("bosses", "all_seals",
        "Master of Seals", "Mestre dos Selos",
        "Collect all 4 seal tiers", "Colete todos os 4 níveis de selo",
        "liberthia:netherite_seal",
        criteria_type="any_item",
        criteria_value=["liberthia:bone_seal", "liberthia:gold_seal",
                        "liberthia:diamond_seal", "liberthia:netherite_seal"],
        parent="liberthia:bosses/root",
        frame="challenge")
    all_lang_en.update(e); all_lang_pt.update(p)

    # Save lang
    lang_path_en = os.path.normpath(os.path.join(ROOT, "..",
        "src/main/resources/assets/liberthia/lang/en_us.json"))
    lang_path_pt = os.path.normpath(os.path.join(ROOT, "..",
        "src/main/resources/assets/liberthia/lang/pt_br.json"))

    for path, additions in [(lang_path_en, all_lang_en), (lang_path_pt, all_lang_pt)]:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        data.update(additions)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

    n_adv = len(all_lang_en)
    print(f"[OK] Gerados {n_adv} advancements em 5 categorias:")
    print(f"  - matter:  8 advancements")
    print(f"  - magic:  10 advancements")
    print(f"  - spirit:  6 advancements")
    print(f"  - cosmic:  8 advancements")
    print(f"  - bosses:  5 advancements")
    print(f"\n  Lang entries adicionados:")
    print(f"    pt_br.json: +{n_adv * 2} keys")
    print(f"    en_us.json: +{n_adv * 2} keys")


if __name__ == "__main__":
    main()
