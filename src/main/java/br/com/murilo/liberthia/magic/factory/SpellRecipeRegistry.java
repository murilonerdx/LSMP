package br.com.murilo.liberthia.magic.factory;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * r148: Registry central das spells criadas via factory.
 *
 * <p>Spells criadas via JSON ({@link SpellRecipeLoader}) ou builder
 * são guardadas aqui. Acessíveis por:
 * <ul>
 *   <li>{@link #get(String)} — busca por id</li>
 *   <li>{@link #all()} — lista todas</li>
 *   <li>Como BONUS, registradas TAMBÉM no {@link SpellLibrary} pra
 *       integrar com sistema universal de spells (DynamicSpellItem
 *       usa SpellLibrary.get sob o capô)</li>
 * </ul>
 */
public final class SpellRecipeRegistry {

    private static final Map<String, SpellRecipe> RECIPES = new LinkedHashMap<>();
    private static final Map<String, SpellDef> COMPILED = new LinkedHashMap<>();

    private SpellRecipeRegistry() {}

    /** Registra uma recipe (idempotente — sobrescreve se mesmo id). */
    public static void register(SpellRecipe recipe) {
        RECIPES.put(recipe.id, recipe);
        SpellDef def = SpellFactory.toSpellDef(recipe);
        COMPILED.put(recipe.id, def);
        // Inscreve no SpellLibrary pra reuso com UniversalSpellScrollItem
        SpellLibrary.registerExternal(def);
        LiberthiaMod.LOGGER.info("[SpellFactory] Registered: {} ({})", recipe.id, recipe.type);
    }

    public static SpellRecipe get(String id) { return RECIPES.get(id); }

    public static SpellDef getCompiled(String id) { return COMPILED.get(id); }

    public static Collection<SpellRecipe> all() { return Collections.unmodifiableCollection(RECIPES.values()); }

    public static Collection<String> ids() { return Collections.unmodifiableCollection(RECIPES.keySet()); }

    public static boolean has(String id) { return RECIPES.containsKey(id); }

    public static int count() { return RECIPES.size(); }

    /** Limpa tudo — chamado antes de re-carregar JSONs via /reload. */
    public static void clear() {
        RECIPES.clear();
        COMPILED.clear();
    }
}
