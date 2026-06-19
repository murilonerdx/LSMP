package br.com.murilo.liberthia.cosmic.nightmare;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r178: <b>Pesadelo ao dormir</b> — ao acordar, há uma chance de a tela "piscar"
 * num pesadelo: blackout → flash de rosto → sussurro. Gateado em
 * {@code cosmic_horror_enabled} (default OFF). Cooldown pra não repetir toda noite.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class NightmareSleepManager {

    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static final long COOLDOWN = 6000L; // 5 min reais entre pesadelos

    private NightmareSleepManager() {}

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicHorrorEnabled.get()) return;
        long now = sp.serverLevel().getGameTime();
        Long last = LAST.get(sp.getUUID());
        if (last != null && now - last < COOLDOWN) return;
        if (sp.getRandom().nextFloat() > 0.30F) return; // 30% de pesadelo
        LAST.put(sp.getUUID(), now);

        // sequência: blackout curto → flash de rosto → sussurro
        ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.BLACKOUT, 18, 0, ""));
        sp.serverLevel().getServer().tell(new net.minecraft.server.TickTask(
                sp.serverLevel().getServer().getTickCount() + 18, () -> {
            if (sp.isAlive()) ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.FLASH, 22, sp.getRandom().nextInt(11), ""));
        }));
        sp.serverLevel().getServer().tell(new net.minecraft.server.TickTask(
                sp.serverLevel().getServer().getTickCount() + 46, () -> {
            if (sp.isAlive()) ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.WHISPER, 80, 0, "não acorde"));
        }));
    }
}
