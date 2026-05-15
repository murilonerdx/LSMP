package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = "liberthia", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModConfigEvents {
    private ModConfigEvents() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        // Intencionalmente vazio. NÃO chame event.getConfig().getFullPath() — durante o
        // sync de config no handshake de login do client, o config interno é um
        // SimpleCommentedConfig (in-memory) e não um CommentedFileConfig (do disco).
        // O cast quebra com ClassCastException e o handshake aborta → cliente recebe
        // "server sent an invalid packet" e não consegue conectar.
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        // Idem onLoad — sem getFullPath aqui.
    }
}
