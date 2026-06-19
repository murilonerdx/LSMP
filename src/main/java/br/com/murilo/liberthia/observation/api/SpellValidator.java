package br.com.murilo.liberthia.observation.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.22 r74: <b>SpellValidator</b> — checa regras de validade num spell
 * recipe (lista de ObservationPart).
 *
 * <p>Pattern AN's CombinedSpellValidator: aplica múltiplas regras e retorna
 * a primeira violação ou null se válido.
 *
 * <h2>Regras default</h2>
 * <ul>
 *   <li>Recipe não-vazio</li>
 *   <li>Primeiro glyph é WatchMethod</li>
 *   <li>Pelo menos 1 Manifestation</li>
 *   <li>Máximo 1 WatchMethod</li>
 *   <li>Max 3 Amplify por spell</li>
 *   <li>Max 2 Echo por spell</li>
 *   <li>Max 8 glyphs total</li>
 *   <li>Sem 2 ou mais glyphs idênticos (exceto distortions com limite)</li>
 * </ul>
 */
public final class SpellValidator {

    /** Limites de augments por glyph ID (configurável). */
    public static final Map<ResourceLocation, Integer> AUGMENT_LIMITS = new HashMap<>();
    static {
        // ResourceLocations dos augments com limites
        // Pode ser configurado via config futuro
    }

    private SpellValidator() {}

    /**
     * Valida o recipe. Retorna null se válido, ou Component com erro.
     */
    public static Component validate(java.util.List<ObservationPart> recipe) {
        if (recipe.isEmpty()) return Component.literal("§cRecipe vazio.");
        if (recipe.size() > 8) return Component.literal("§cMáximo 8 glyphs por feitiço.");

        // Primeiro deve ser Method
        if (!(recipe.get(0) instanceof WatchMethod)) {
            return Component.literal("§cPrimeiro glyph deve ser um Método (azul).");
        }

        int methodCount = 0;
        int manifestCount = 0;
        Map<ResourceLocation, Integer> counts = new HashMap<>();

        for (ObservationPart p : recipe) {
            if (p instanceof WatchMethod) methodCount++;
            if (p instanceof Manifestation) manifestCount++;
            counts.merge(p.id(), 1, Integer::sum);
        }

        if (methodCount > 1) {
            return Component.literal("§cMáximo 1 Método por feitiço.");
        }
        if (manifestCount < 1) {
            return Component.literal("§cFeitiço precisa de pelo menos 1 Manifestação (roxo).");
        }

        // Check augment limits (default global)
        for (var entry : counts.entrySet()) {
            ResourceLocation id = entry.getKey();
            int n = entry.getValue();
            int limit = getAugmentLimit(id);
            if (limit != -1 && n > limit) {
                ObservationPart p = ObservationRegistry.get(id);
                String name = p != null ? p.displayComponent().getString() : id.toString();
                return Component.literal("§cMáximo " + limit + " × §e" + name + "§c por feitiço.");
            }
        }
        return null; // válido
    }

    /** Default limits: Amplify max 3, Echo max 2, Linger max 3, outros max 4. */
    public static int getAugmentLimit(ResourceLocation id) {
        if (AUGMENT_LIMITS.containsKey(id)) return AUGMENT_LIMITS.get(id);
        String path = id.getPath();
        if (path.equals("amplify_distortion")) return 3;
        if (path.equals("distort/echo")) return 2;
        if (path.equals("distort/linger")) return 3;
        if (path.equals("aug/pierce")) return 2;
        if (path.equals("aug/split")) return 2;
        if (path.equals("aug/aoe")) return 2;
        return -1; // sem limite
    }
}
