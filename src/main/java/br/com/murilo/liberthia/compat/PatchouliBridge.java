package br.com.murilo.liberthia.compat;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * r147: Bridge opcional pro mod Patchouli (vazkii).
 *
 * <p>O Liberthia compila SEM Patchouli como dependência. Em runtime, este
 * helper detecta se Patchouli está carregado e usa reflection pra abrir
 * o livro guiado. Se Patchouli não estiver presente, falha silenciosamente
 * (caller deve fazer fallback pra GUI custom).
 *
 * <h2>Vantagens da reflection</h2>
 * <ul>
 *   <li>Build NÃO depende de Patchouli (sem maven config extra)</li>
 *   <li>Mod funciona standalone (sem Patchouli) E com Patchouli</li>
 *   <li>Zero crash se Patchouli for removido depois</li>
 * </ul>
 *
 * <h2>API do Patchouli usada</h2>
 * <pre>{@code
 *   vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(serverPlayer, bookId)
 * }</pre>
 *
 * Reference: https://github.com/VazkiiMods/Patchouli/wiki
 */
public final class PatchouliBridge {

    private PatchouliBridge() {}

    /** Cached state — evita chamar ModList.isLoaded em todo right-click. */
    private static Boolean cachedAvailable = null;

    /** Cached reflected handles. */
    private static Object cachedApi;
    private static Method cachedOpenMethod;

    /** Verdadeiro se Patchouli estiver instalado E carregado. */
    public static boolean isAvailable() {
        if (cachedAvailable == null) {
            cachedAvailable = ModList.get() != null && ModList.get().isLoaded("patchouli");
            if (cachedAvailable) {
                LiberthiaMod.LOGGER.info("[PatchouliBridge] Patchouli detected — using it for Manual");
            } else {
                LiberthiaMod.LOGGER.info("[PatchouliBridge] Patchouli not installed — using custom Manual GUI");
            }
        }
        return cachedAvailable;
    }

    /**
     * Tenta abrir o livro guiado do Patchouli pro player.
     *
     * @param player jogador (server-side)
     * @param bookId ResourceLocation do livro (ex: "liberthia:liberthia_manual")
     * @return true se abriu, false se falhou (Patchouli ausente ou erro de reflection)
     */
    public static boolean openBook(ServerPlayer player, ResourceLocation bookId) {
        if (!isAvailable()) return false;

        try {
            Object api = getApi();
            Method open = getOpenMethod(api);
            open.invoke(api, player, bookId);
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn(
                    "[PatchouliBridge] Falha ao abrir livro {} via Patchouli: {}",
                    bookId, t.toString());
            return false;
        }
    }

    /** Lazy-init do PatchouliAPI singleton via reflection. */
    private static Object getApi() throws Exception {
        if (cachedApi == null) {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Method get = apiClass.getMethod("get");
            cachedApi = get.invoke(null);
        }
        return cachedApi;
    }

    /** Lazy-init do método openBookGUI via reflection. */
    private static Method getOpenMethod(Object api) throws NoSuchMethodException {
        if (cachedOpenMethod == null) {
            // Signature: openBookGUI(ServerPlayer, ResourceLocation)
            cachedOpenMethod = api.getClass()
                    .getMethod("openBookGUI", ServerPlayer.class, ResourceLocation.class);
        }
        return cachedOpenMethod;
    }
}
