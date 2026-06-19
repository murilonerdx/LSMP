package br.com.murilo.liberthia.cosmic.curse;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r45: Registra quais items são "amaldiçoados" no
 * {@link CursedItemRegistry}.
 *
 * <p>Registrado no server start (depois de items já carregados).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CursedItemInit {

    private CursedItemInit() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        try {
            CursedItemRegistry.register(ModItems.CURSED_EFFIGY.get());
            CursedItemRegistry.register(ModItems.VOICE_CURSE_AMULET.get());
            CursedItemRegistry.register(ModItems.SILENT_WITNESS_CLOAK.get());
            LiberthiaMod.LOGGER.info("[CursedItemInit] r45 — 3 cursed items registered");
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[CursedItemInit] register error: {}", t.toString());
        }
    }
}
