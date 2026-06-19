package br.com.murilo.liberthia.magic.factory;

import br.com.murilo.liberthia.LiberthiaMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * r148: Carregador de SpellRecipes a partir de JSON.
 *
 * <p>Lê todos os arquivos em {@code data/<modid>/spells/*.json} — qualquer mod
 * pode adicionar spells! Suporte a datapack: jogador pode dropar JSONs num
 * datapack e modificar/adicionar spells SEM mexer em código Java.
 *
 * <p>Roda no boot do server e em todo {@code /reload}. Se um JSON falhar parse,
 * loga o erro e segue o resto (não derruba o server).
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Vanilla {@link SimpleJsonResourceReloadListener} entrega Map de
 *       {@code ResourceLocation → JsonElement}</li>
 *   <li>Pra cada entry: tenta {@link SpellRecipe#fromJson(JsonObject)}</li>
 *   <li>Sucesso: {@link SpellRecipeRegistry#register(SpellRecipe)} (que também
 *       integra com {@link br.com.murilo.liberthia.magic.spell.SpellLibrary})</li>
 *   <li>Erro: loga + segue</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpellRecipeLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "spells";

    public SpellRecipeLoader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons,
                          ResourceManager mgr, ProfilerFiller profiler) {
        // Limpa o registry — re-load completo a cada /reload
        SpellRecipeRegistry.clear();

        int loaded = 0;
        int errors = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation rl = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                // Se o JSON não tem "id" explícito, usa o nome do arquivo (rl.path())
                if (!json.has("id")) {
                    json.addProperty("id", rl.getPath());
                }
                SpellRecipe recipe = SpellRecipe.fromJson(json);
                SpellRecipeRegistry.register(recipe);
                loaded++;
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[SpellFactory] Failed to load {}: {}", rl, t.toString());
                errors++;
            }
        }
        LiberthiaMod.LOGGER.info("[SpellFactory] Loaded {} spell recipes ({} errors)", loaded, errors);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SpellRecipeLoader());
    }
}
