package br.com.murilo.liberthia.magic.factory;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import com.google.gson.JsonObject;
import net.minecraft.world.item.Rarity;

import java.util.List;

/**
 * r148: <b>SpellRecipe</b> — definição declarativa de um feitiço.
 *
 * <p>Tudo que um spell precisa pra ser instanciado pelo {@link SpellFactory}
 * está aqui. Pode vir de:
 * <ul>
 *   <li>JSON em {@code data/liberthia/spells/<id>.json}</li>
 *   <li>Builder fluente {@link SpellRecipeBuilder}</li>
 *   <li>Construção direta (testes)</li>
 * </ul>
 *
 * <p>É um DTO imutável. {@link SpellFactory#toCastLambda} converte em
 * {@code SpellDef.cast} executável.
 *
 * <h2>JSON Format</h2>
 * <pre>{@code
 * {
 *   "id": "fireball_v2",
 *   "name": "Fireball Mark II",
 *   "school": "FIRE",
 *   "rarity": "UNCOMMON",
 *   "mana": 25,
 *   "cooldown": 60,
 *   "damage": 8,
 *   "range": 24,
 *   "type": "PROJECTILE",
 *   "lore": "Antigos bárbaros usavam isto...",
 *   "behavior": {
 *     "speed": 1.5,
 *     "lifetime_ticks": 80,
 *     "pierce": false,
 *     "aoe_radius": 2.5,
 *     "ignite_blocks": true,
 *     "projectile_count": 1,
 *     "spread_degrees": 0
 *   },
 *   "vfx": { "color_primary": "#ff5500", "trail_density": 8, "screen_shake": 0.6 },
 *   "effects": [
 *     { "type": "IGNITE", "duration": 80 },
 *     { "type": "KNOCKBACK", "magnitude": 0.5 }
 *   ]
 * }
 * }</pre>
 */
public final class SpellRecipe {

    public final String id;
    public final String name;
    public final SpellSchool school;
    public final Rarity rarity;
    public final int mana;
    public final int cooldown;
    public final float damage;
    public final float range;
    public final SpellType type;
    public final String lore;
    public final SpellBehavior behavior;
    public final SpellVfxProfile vfx;
    public final List<SpellEffectSpec> effects;
    /** r151: element 1 (base or dual). NONE = sem element. */
    public final SpellElement element;
    /** r151: element 2 (combina com element pra formar dual). NONE = single. */
    public final SpellElement secondaryElement;
    /** r151: meta-category (display/filter). */
    public final SpellCategory category;

    public SpellRecipe(String id, String name, SpellSchool school, Rarity rarity,
                       int mana, int cooldown, float damage, float range,
                       SpellType type, String lore,
                       SpellBehavior behavior, SpellVfxProfile vfx,
                       List<SpellEffectSpec> effects,
                       SpellElement element, SpellElement secondaryElement,
                       SpellCategory category) {
        this.id = id;
        this.name = name;
        this.school = school;
        this.rarity = rarity;
        this.mana = mana;
        this.cooldown = cooldown;
        // r151: damage modifier por element
        SpellElement effective = (element != null && secondaryElement != null && secondaryElement != SpellElement.NONE)
                ? SpellElement.combine(element, secondaryElement) : (element != null ? element : SpellElement.NONE);
        this.damage = damage * effective.damageMultiplier;
        this.range = range;
        this.type = type;
        this.lore = lore == null ? "" : lore;
        this.behavior = behavior == null ? SpellBehavior.defaults() : behavior;
        this.vfx = vfx == null ? SpellVfxProfile.defaults() : vfx;
        this.effects = effects == null ? List.of() : List.copyOf(effects);
        this.element = element == null ? SpellElement.NONE : element;
        this.secondaryElement = secondaryElement == null ? SpellElement.NONE : secondaryElement;
        this.category = category == null ? SpellCategory.PROJECTILE : category;
    }

    /** Parse from full JSON object. Required fields throw if missing. */
    /**
     * r177: tolera tipos "legados" dos JSON que não existem no enum {@link SpellType}
     * (eram 46 spells falhando com IllegalArgumentException). Mapeia para o tipo de
     * delivery equivalente; desconhecido → PROJECTILE em vez de crashar o load.
     */
    private static SpellType parseType(String raw) {
        String s = raw == null ? "" : raw.trim().toUpperCase();
        switch (s) {
            case "SELF_BUFF": case "BUFF":                  return SpellType.SELF;     // buff no próprio caster
            case "AOE_BURST": case "BURST": case "AOE":     return SpellType.NOVA;     // estouro em área ao redor
            case "TARGETED": case "TARGET": case "SINGLE_TARGET": return SpellType.HOMING; // alvo único à distância
            default:
                try { return SpellType.valueOf(s); }
                catch (IllegalArgumentException e) { return SpellType.PROJECTILE; }
        }
    }

    public static SpellRecipe fromJson(JsonObject j) {
        String id = j.get("id").getAsString();
        String name = j.get("name").getAsString();
        SpellSchool school = SpellSchool.valueOf(j.get("school").getAsString().toUpperCase());
        Rarity rarity = j.has("rarity")
                ? Rarity.valueOf(j.get("rarity").getAsString().toUpperCase())
                : Rarity.COMMON;
        int mana = j.has("mana") ? j.get("mana").getAsInt() : 10;
        int cooldown = j.has("cooldown") ? j.get("cooldown").getAsInt() : 40;
        float damage = j.has("damage") ? j.get("damage").getAsFloat() : 4F;
        float range = j.has("range") ? j.get("range").getAsFloat() : 16F;
        SpellType type = j.has("type")
                ? parseType(j.get("type").getAsString())
                : SpellType.PROJECTILE;
        String lore = j.has("lore") ? j.get("lore").getAsString() : "";

        SpellBehavior behavior = j.has("behavior")
                ? SpellBehavior.fromJson(j.getAsJsonObject("behavior"))
                : SpellBehavior.defaults();
        SpellVfxProfile vfx = j.has("vfx")
                ? SpellVfxProfile.fromJson(j.getAsJsonObject("vfx"))
                : SpellVfxProfile.defaults();
        List<SpellEffectSpec> effects = j.has("effects")
                ? SpellEffectSpec.fromJsonArray(j.getAsJsonArray("effects"))
                : List.of();
        // r151
        SpellElement element = SpellElement.parse(j.has("element") ? j.get("element").getAsString() : null);
        SpellElement secondary = SpellElement.parse(j.has("secondary_element") ? j.get("secondary_element").getAsString() : null);
        SpellCategory category = SpellCategory.parse(j.has("category") ? j.get("category").getAsString() : null);

        return new SpellRecipe(id, name, school, rarity, mana, cooldown, damage, range,
                type, lore, behavior, vfx, effects, element, secondary, category);
    }
}
