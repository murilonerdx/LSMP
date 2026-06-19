package br.com.murilo.liberthia.voice;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Roda a cada server tick — chama {@link LiberthiaVoicePlugin#tickInactivity()}
 * a cada 10 ticks (500ms) pra detectar utterances que precisam ser flushed.
 *
 * Também limpa sessions quando players desconectam.
 *
 * <p>r180b: GUARDADO com {@link #voicechatLoaded()} — Simple Voice Chat é opcional.
 * Sem ele, NÃO tocar em {@link LiberthiaVoicePlugin} (que referencia a API do
 * voicechat), senão um servidor sem o voicechat instalado crasha no 1º tick com
 * {@code NoClassDefFoundError: de/maxhenkel/voicechat/api/VoicechatPlugin}.
 */
@Mod.EventBusSubscriber(modid = "liberthia")
public final class VoiceTickWatcher {
    private VoiceTickWatcher() {}

    private static Boolean voicePresent;
    private static boolean voicechatLoaded() {
        if (voicePresent == null) {
            try { voicePresent = ModList.get().isLoaded("voicechat"); }
            catch (Throwable t) { voicePresent = false; }
        }
        return voicePresent;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!voicechatLoaded()) return;
        if (event.getServer().getTickCount() % 10 == 0) {
            LiberthiaVoicePlugin.tickInactivity();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!voicechatLoaded()) return;
        LiberthiaVoicePlugin.resetSession(event.getEntity().getUUID());
    }
}
