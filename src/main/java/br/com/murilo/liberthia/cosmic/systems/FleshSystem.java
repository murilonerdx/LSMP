package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * r81 #9/18 — <b>Flesh Horror</b> (The Thing / Dead Space style).
 *
 * <p>Player começa a sentir <i>algo crescendo</i>. Particles DAMAGE_INDICATOR
 * (gotas vermelhas) caem do corpo, sons de respiração distantes. Em níveis
 * altos, dano de "carne mutante" — perdendo HP lentamente.
 *
 * <p>Trigger: ficar perto de blood_logs/sangue (Liberthia tem isso) ou
 * em biomas de horror corporal.
 */
public final class FleshSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.FLESH;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 60 != 0) return;

        // Detecta blocos de sangue/flesh próximos
        int fleshNearby = countFleshBlocks(sp, level);
        if (fleshNearby > 0) {
            state.addExposure(HorrorType.FLESH, Math.min(5.0F, fleshNearby * 0.5F));
        }

        float exp = state.getExposure(HorrorType.FLESH);

        // Gotas de "sangue/carne" caindo do player
        if (exp >= 30 && Math.random() < 0.5) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    sp.getX(), sp.getY() + 1, sp.getZ(),
                    3, 0.2, 0.1, 0.2, 0.01);
        }

        // Dano leve em 60+
        if (exp >= 60 && gameTime % 200 == 0) {
            sp.hurt(sp.damageSources().wither(), 0.5F);
            sp.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1, true, false));
        }

        // Decay
        if (fleshNearby == 0 && gameTime % 100 == 0) {
            state.decayExposure(HorrorType.FLESH, baseDecayRate() * 1.5F);
        }
    }

    private int countFleshBlocks(ServerPlayer sp, ServerLevel level) {
        int count = 0;
        net.minecraft.core.BlockPos pos = sp.blockPosition();
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -8; dz <= 8; dz += 2) {
                    var bs = level.getBlockState(pos.offset(dx, dy, dz));
                    String name = bs.getBlock().getDescriptionId().toLowerCase();
                    if (name.contains("blood") || name.contains("flesh") || name.contains("sanguine")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
