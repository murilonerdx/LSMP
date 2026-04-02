package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.backend.BackendClient;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModConfigEvents {
    private ModConfigEvents() {
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == LiberthiaConfig.SERVER_SPEC) {
            BackendClient.reloadFromConfig();
        }
    }
}
