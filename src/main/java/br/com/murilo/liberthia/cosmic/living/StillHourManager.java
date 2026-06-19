package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r47: <b>The Still Hour</b> — evento global aleatório.
 *
 * <h2>Trigger</h2>
 * A cada ~2-6h de gameplay (real time, baseado em server tick), 60s
 * de silêncio absoluto.
 *
 * <h2>Durante o evento</h2>
 * <ul>
 *   <li>Ambient sounds NÃO param (impossível desligar globalmente),
 *       mas TODOS players recebem um {@code Darkness} muito leve +
 *       som de "vazio" sustentado.</li>
 *   <li>Random hallucinations: silhuetas gigantes distantes (impossible_moon),
 *       structures fantasmas, fake_entity peripheral.</li>
 *   <li>NÃO mata mobs nem ataca players — é puramente psicológico.</li>
 * </ul>
 *
 * <h2>Fim</h2>
 * Audio volta com um WARDEN_SONIC_BOOM low-vol pra "estouro" dramático.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class StillHourManager {

    public static final int DURATION_TICKS = 1200; // 60s
    public static final int MIN_INTERVAL = 144000; // 2h em ticks
    public static final int MAX_INTERVAL = 432000; // 6h em ticks

    /** Last tick que o evento começou. -1 = nunca. */
    private static long activeStartTick = -1;
    /** Tick em que o PRÓXIMO evento pode disparar (rolar dado). */
    private static long nextRollTick = -1;

    private StillHourManager() {}

    public static boolean isActive() {
        return activeStartTick > 0;
    }

    public static int ticksRemaining(long now) {
        if (activeStartTick < 0) return 0;
        return (int) Math.max(0, DURATION_TICKS - (now - activeStartTick));
    }

    /** Curator pode forçar. */
    public static void forceStart(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        activeStartTick = now;
        onStart(server);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();

        if (activeStartTick > 0) {
            // Active
            long elapsed = now - activeStartTick;
            if (elapsed >= DURATION_TICKS) {
                onEnd(server);
                activeStartTick = -1;
                // Schedule next
                nextRollTick = now + MIN_INTERVAL +
                        (int) (Math.random() * (MAX_INTERVAL - MIN_INTERVAL));
                return;
            }
            // During — tick hallucinations
            if (elapsed % 30 == 0) {
                tickDuringSilence(server, elapsed);
            }
            return;
        }

        // Schedule first
        if (nextRollTick < 0) {
            nextRollTick = now + MIN_INTERVAL +
                    (int) (Math.random() * (MAX_INTERVAL - MIN_INTERVAL));
        }

        // Time to roll
        if (now >= nextRollTick) {
            // Only trigger if at least 1 player online
            if (!server.getPlayerList().getPlayers().isEmpty()) {
                activeStartTick = now;
                onStart(server);
            } else {
                // Re-schedule
                nextRollTick = now + MIN_INTERVAL;
            }
        }
    }

    private static void onStart(MinecraftServer server) {
        LiberthiaMod.LOGGER.info("[StillHour] BEGIN");
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            sp.displayClientMessage(Component.literal(
                    "§8§o*tudo fica subitamente quieto*"), true);
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DARKNESS, DURATION_TICKS, 0, false, false));
            // Initial hallucination
            HallucinationManager.force(sp, HallucinationType.HEARTBEAT_PULSE, 1.0F, 60, "");
        }
    }

    private static void tickDuringSilence(MinecraftServer server, long elapsed) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            // Each player gets unique hallucinations during silence
            float rand = (float) Math.random();
            HallucinationType type;
            if (rand < 0.30F) type = HallucinationType.IMPOSSIBLE_MOON;
            else if (rand < 0.55F) type = HallucinationType.FAKE_ENTITY_PERIPHERAL;
            else if (rand < 0.75F) type = HallucinationType.SHADOW_MOVEMENT;
            else if (rand < 0.90F) type = HallucinationType.HEARTBEAT_PULSE;
            else type = HallucinationType.TEMPORAL_GHOST;

            if (Math.random() < 0.4) {
                HallucinationManager.force(sp, type, 0.7F + (elapsed / 2400.0F), 30, "");
            }
        }
    }

    private static void onEnd(MinecraftServer server) {
        LiberthiaMod.LOGGER.info("[StillHour] END");
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            sp.displayClientMessage(Component.literal(
                    "§7§o*o som retorna de uma só vez*"), true);
            // "BOOM" do silêncio voltando
            try {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(
                                net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM),
                        net.minecraft.sounds.SoundSource.AMBIENT,
                        sp.getX(), sp.getY(), sp.getZ(),
                        0.6F, 0.4F, sp.level().random.nextLong()));
            } catch (Throwable ignored) {}
            HallucinationManager.force(sp, HallucinationType.SCREEN_GLITCH_BURST, 0.8F, 20, "");
        }
    }
}
