package br.com.murilo.liberthia.cosmic.scare;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r177: <b>piscar no escuro</b> — se ATIVADO por comando para um player e ele estiver
 * no ESCURO (luz &le; 7), a cada N minutos a tela dá uma PISCADA (pálpebras pretas
 * fecham e abrem). Per-player, configurável:
 * {@code /liberthia scare blinkmode <alvos> on|off [minutos]}.
 *
 * <p>Estado no {@code player.getPersistentData()} → sobrevive relog.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BlinkInDarkManager {

    private static final String NBT_ON = "liberthia.blink_dark";
    private static final String NBT_INTERVAL = "liberthia.blink_interval";
    private static final String NBT_NEXT = "liberthia.blink_next";
    private static final int LIGHT_DARK = 7; // <= 7 conta como "escuro"

    private BlinkInDarkManager() {}

    public static void setEnabled(ServerPlayer p, boolean on, int minutes) {
        var d = p.getPersistentData();
        if (on) {
            d.putBoolean(NBT_ON, true);
            d.putInt(NBT_INTERVAL, Math.max(1, minutes) * 60 * 20);
            d.putLong(NBT_NEXT, 0L);
        } else {
            d.remove(NBT_ON);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return; // checa a cada 2s
        var d = sp.getPersistentData();
        if (!d.getBoolean(NBT_ON)) return;
        // só pisca quando está no escuro
        if (sp.serverLevel().getMaxLocalRawBrightness(sp.blockPosition()) > LIGHT_DARK) return;

        long now = sp.serverLevel().getGameTime();
        int interval = d.contains(NBT_INTERVAL) ? d.getInt(NBT_INTERVAL) : 12000; // 10 min default
        long next = d.getLong(NBT_NEXT);
        if (next == 0L) { d.putLong(NBT_NEXT, now + interval); return; } // arma o 1º
        if (now < next) return;
        d.putLong(NBT_NEXT, now + interval);
        ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.BLINK, 14, 0, ""));
    }
}
