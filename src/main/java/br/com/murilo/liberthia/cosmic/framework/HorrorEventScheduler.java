package br.com.murilo.liberthia.cosmic.framework;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.systems.MemeticSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r81: <b>Global Horror Event Scheduler</b>.
 *
 * <p>Roda eventos server-wide periódicos que afetam <i>todos players ao
 * mesmo tempo</i>, em vez de per-player. São raros (alguns minutos entre
 * eventos), narrativamente impactantes, e shareados.
 *
 * <h2>Eventos implementados</h2>
 * <ul>
 *   <li>{@code REPEATING_DAY} — sunrise repetido, sons em loop</li>
 *   <li>{@code DEAD_FUTURE} — flash de "futuro morto" (particles + sounds)</li>
 *   <li>{@code EMERGENCY_TRANSMISSION} — todos recebem title VHS</li>
 *   <li>{@code SHARED_DREAM} — players em Spirit World tem evento conjunto</li>
 * </ul>
 *
 * <h2>Cooldowns globais</h2>
 * Cada evento tem cooldown >= 1 hora (72000 ticks) pra que sejam raros.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HorrorEventScheduler {

    private static long lastRepeatingDay = 0;
    private static long lastDeadFuture = 0;
    private static long lastEmergencyTransmission = 0;
    private static long lastMemeticDecay = 0;

    /** Cooldown padrão em ticks. */
    private static final long EVENT_CD = 72000; // 1h

    private HorrorEventScheduler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        long tick = server.getTickCount();

        // Memetic decay — diminui 1 por 5 min
        if (tick - lastMemeticDecay >= 6000) {
            MemeticSystem.decayGlobal();
            lastMemeticDecay = tick;
        }

        // r183: eventos globais de horror só se ALGUÉM estiver com sanidade < 40%
        if (server.getPlayerList().getPlayers().stream().noneMatch(
                p -> br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(p) < 40)) return;

        // Repeating Day: 1% chance por hora
        if (tick - lastRepeatingDay >= EVENT_CD && Math.random() < 0.01 / 20.0) {
            triggerRepeatingDay(server);
            lastRepeatingDay = tick;
        }

        // Dead Future: 0.5% chance por hora
        if (tick - lastDeadFuture >= EVENT_CD && Math.random() < 0.005 / 20.0) {
            triggerDeadFuture(server);
            lastDeadFuture = tick;
        }

        // Emergency Transmission: 2% chance por hora
        if (tick - lastEmergencyTransmission >= EVENT_CD && Math.random() < 0.02 / 20.0) {
            triggerEmergencyTransmission(server);
            lastEmergencyTransmission = tick;
        }
    }

    /** "Repeating Day" — todos players ouvem o mesmo som de sunrise. */
    private static void triggerRepeatingDay(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) >= 40) continue; // r183: só low-sanity
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                    20, 100, 30)); // animação ANTES do texto (senão o timing custom é ignorado)
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.literal("§5§l...")));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal("§7§oo mesmo dia. de novo.")));
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(),
                    net.minecraft.sounds.SoundSource.AMBIENT, 1.5F, 0.4F);
            HorrorFramework.getState(sp).addExposure(HorrorType.EXISTENTIAL, 20.0F);
            HorrorFramework.getState(sp).addExposure(HorrorType.TEMPORAL, 15.0F);
        }
        LiberthiaMod.LOGGER.info("[HorrorEvent] REPEATING_DAY triggered globally");
    }

    /** "Dead Future" — flash de futuro morto (particles + dim sky). */
    private static void triggerDeadFuture(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) >= 40) continue; // r183: só low-sanity
            if (!(sp.level() instanceof ServerLevel sl)) continue;
            // Ash particles caindo ao redor
            for (int i = 0; i < 40; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 12;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH,
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + 4 + Math.random() * 4,
                        sp.getZ() + Math.sin(a) * r,
                        2, 0.3, 0.3, 0.3, 0.01);
            }
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                    10, 60, 20));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.literal("§8§l...")));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal("§4§oé assim que termina")));
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                    net.minecraft.sounds.SoundSource.AMBIENT, 0.5F, 0.4F);
            HorrorFramework.getState(sp).addExposure(HorrorType.TEMPORAL, 25.0F);
            HorrorFramework.getState(sp).addExposure(HorrorType.EXISTENTIAL, 15.0F);
        }
        LiberthiaMod.LOGGER.info("[HorrorEvent] DEAD_FUTURE triggered globally");
    }

    /** Emergency Transmission VHS broadcast. */
    private static void triggerEmergencyTransmission(MinecraftServer server) {
        String[] msgs = {
                "§7§l[ §l█§r§7§l SIGNAL LOST §l█§r§7§l ]",
                "§7§l[ §l█§r§7§l TEST PATTERN §l█§r§7§l ]",
                "§7§l[ §l█§r§7§l UNKNOWN TRANSMISSION §l█§r§7§l ]"
        };
        String[] subs = {
                "§r§4§oattention all citizens — do not look",
                "§r§5§obreathing detected — source unknown",
                "§r§7§o…they are inside the walls…"
        };
        int idx = (int)(Math.random() * msgs.length);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) >= 40) continue; // r183: só low-sanity
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                    10, 80, 10));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.literal(msgs[idx])));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal(subs[idx])));
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                    net.minecraft.sounds.SoundSource.MASTER, 1.0F, 0.5F);
            HorrorFramework.getState(sp).addExposure(HorrorType.ANALOG, 20.0F);
        }
        LiberthiaMod.LOGGER.info("[HorrorEvent] EMERGENCY_TRANSMISSION triggered globally");
    }
}
