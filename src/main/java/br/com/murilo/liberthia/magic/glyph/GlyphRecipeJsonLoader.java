package br.com.murilo.liberthia.magic.glyph;

import br.com.murilo.liberthia.LiberthiaMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/**
 * v0.1.151 r119: <b>GlyphRecipeJsonLoader</b> — carrega receitas Spirit Glyph
 * de arquivos JSON em {@code data/liberthia/glyph_recipes/*.json}.
 *
 * <p>Layout JSON:
 * <pre>
 * {
 *   "display_name": "Bola de Fogo Composta",
 *   "output": { "item": "liberthia:spell_fireball", "count": 1 },
 *   "ingredients": [
 *     { "item": "liberthia:wisp_essence", "count": 2 },
 *     { "item": "liberthia:astral_dust",  "count": 1 }
 *   ]
 * }
 * </pre>
 *
 * <p>Reload via {@code /reload} ou ao iniciar server. Sobreescreve as receitas
 * hardcoded do {@link SpiritGlyphRecipeInit} (que ainda funcionam como fallback).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public class GlyphRecipeJsonLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public GlyphRecipeJsonLoader() {
        super(GSON, "glyph_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                          ResourceManager rm, ProfilerFiller profiler) {
        int loaded = 0;
        for (var entry : jsonMap.entrySet()) {
            try {
                SpiritGlyphRecipes.Recipe r = parseRecipe(entry.getValue().getAsJsonObject());
                if (r != null) {
                    SpiritGlyphRecipes.register(r);
                    loaded++;
                }
            } catch (Exception e) {
                LiberthiaMod.LOGGER.warn("[GlyphRecipeJsonLoader] failed to parse {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        LiberthiaMod.LOGGER.info("[GlyphRecipeJsonLoader] loaded {} JSON glyph recipes", loaded);
    }

    private SpiritGlyphRecipes.Recipe parseRecipe(JsonObject obj) {
        String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : "Recipe";
        JsonObject outObj = obj.getAsJsonObject("output");
        ResourceLocation outId = new ResourceLocation(outObj.get("item").getAsString());
        Item outItem = ForgeRegistries.ITEMS.getValue(outId);
        if (outItem == null) return null;
        int outCount = outObj.has("count") ? outObj.get("count").getAsInt() : 1;
        final int finalOutCount = Math.max(1, outCount);
        final Item finalOutItem = outItem;

        java.util.List<SpiritGlyphRecipes.Ingredient> ings = new java.util.ArrayList<>();
        for (JsonElement ingEl : obj.getAsJsonArray("ingredients")) {
            JsonObject ingObj = ingEl.getAsJsonObject();
            ResourceLocation ingId = new ResourceLocation(ingObj.get("item").getAsString());
            Item ingItem = ForgeRegistries.ITEMS.getValue(ingId);
            if (ingItem == null) continue;
            int count = ingObj.has("count") ? ingObj.get("count").getAsInt() : 1;
            final Item finalIngItem = ingItem;
            ings.add(new SpiritGlyphRecipes.Ingredient(() -> finalIngItem, count));
        }
        return new SpiritGlyphRecipes.Recipe(
                displayName,
                () -> new ItemStack(finalOutItem, finalOutCount),
                ings.toArray(new SpiritGlyphRecipes.Ingredient[0]));
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new GlyphRecipeJsonLoader());
    }
}
