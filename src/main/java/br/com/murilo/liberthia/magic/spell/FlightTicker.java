package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.151 r119: <b>FlightTicker</b> — gerencia o efeito do feitiço
 * "wings_of_source". Drena Source enquanto o player tem o flight buff ativo,
 * e remove as habilidades de voo quando o tempo acaba ou Source chega a zero.
 *
 * <p>O feitiço grava {@code liberthia.flight_until} no NBT do player. Esse ticker
 * checa a cada tick:
 * <ul>
 *   <li>Se passou do tempo → remove flight</li>
 *   <li>Se Source < custo/tick → remove flight</li>
 *   <li>Senão: -1 Source/segundo, spawn de partículas</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class FlightTicker {

    private static final String FLIGHT_UNTIL = "liberthia.flight_until";
    private static final int DRAIN_PER_TICK = 1; // 20/segundo seria muito; usamos 1 a cada 20 ticks
    private static final int TICK_INTERVAL = 20;

    private FlightTicker() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;

        var data = sp.getPersistentData();
        if (!data.contains(FLIGHT_UNTIL)) return;

        long until = data.getLong(FLIGHT_UNTIL);
        long now = sp.level().getGameTime();

        // Tempo acabou
        if (now >= until) {
            endFlight(sp);
            return;
        }

        // Tick-based drain
        if (now % TICK_INTERVAL == 0) {
            int src = SourceData.get(sp);
            if (src < DRAIN_PER_TICK) {
                endFlight(sp);
                sp.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§c⚠ Source insuficiente — voo cessou"), true);
                return;
            }
            SourceData.consume(sp, DRAIN_PER_TICK);
        }

        // VFX: trilha de partículas dourada nos pés
        if (sp.level() instanceof ServerLevel sl && now % 4 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    sp.getX(), sp.getY() + 0.1, sp.getZ(),
                    2, 0.2, 0, 0.2, 0.02);
        }
    }

    private static void endFlight(ServerPlayer sp) {
        sp.getPersistentData().remove(FLIGHT_UNTIL);
        if (!sp.isCreative() && !sp.isSpectator()) {
            sp.getAbilities().mayfly = false;
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }
    }
}
