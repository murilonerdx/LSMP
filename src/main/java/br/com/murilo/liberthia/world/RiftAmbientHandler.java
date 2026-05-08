package br.com.murilo.liberthia.world;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spawna partículas roxas nos rifts dimensionais perto de cada jogador.
 *
 * <p>A cada {@link #PERIOD} ticks, lista os rifts dentro de {@link #RANGE}
 * blocos do jogador e manda partículas via {@code ServerLevel.sendParticles}.
 * Funciona independente de chunk loading porque é tickado pelo jogador.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class RiftAmbientHandler {

    private static final int PERIOD = 10;
    private static final int RANGE = 64;
    private static final int RANGE_SQ = RANGE * RANGE;

    private RiftAmbientHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % PERIOD != 0) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        RiftSavedData data = RiftSavedData.get(level);
        BlockPos origin = sp.blockPosition();

        for (BlockPos r : data.getRifts()) {
            if (r.distSqr(origin) > RANGE_SQ) continue;

            // Coluna de partículas roxas + brilho central
            level.sendParticles(sp, ParticleTypes.PORTAL,
                    true,
                    r.getX() + 0.5, r.getY() + 0.5, r.getZ() + 0.5,
                    8,
                    0.6, 1.0, 0.6,
                    0.05);
            level.sendParticles(sp, ParticleTypes.SOUL_FIRE_FLAME,
                    true,
                    r.getX() + 0.5, r.getY() + 1.5, r.getZ() + 0.5,
                    2,
                    0.2, 0.4, 0.2,
                    0.02);
            level.sendParticles(sp, ParticleTypes.END_ROD,
                    true,
                    r.getX() + 0.5, r.getY() + 0.3, r.getZ() + 0.5,
                    1,
                    0.1, 0.5, 0.1,
                    0.01);
        }
    }
}
