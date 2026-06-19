package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * r81 #8/18 — <b>Dimensional Horror</b>.
 *
 * <p>Realidades sangrando. Quando o player muda de dimensão muitas vezes,
 * dimensão atual ganha "ecos" da anterior — particles de portal, sons
 * de outra dim, etc.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Cada {@link #onDimensionChange} aumenta um contador interno</li>
 *   <li>Acima de 5 dim changes (sessão), ganha exposição passiva +0.5/tick</li>
 *   <li>Em exposure &gt;= 30, spawna particles random de "portal" no ar</li>
 * </ul>
 */
public final class DimensionalSystem implements HorrorSystem {

    private final java.util.Map<java.util.UUID, Integer> dimSwapCount = new java.util.HashMap<>();

    @Override
    public HorrorType type() {
        return HorrorType.DIMENSIONAL;
    }

    @Override
    public void onDimensionChange(ServerPlayer sp, HorrorState state) {
        int n = dimSwapCount.merge(sp.getUUID(), 1, Integer::sum);
        if (n >= 5) {
            state.addExposure(HorrorType.DIMENSIONAL, 8.0F);
        }
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        int swaps = dimSwapCount.getOrDefault(sp.getUUID(), 0);
        if (swaps >= 5 && gameTime % 60 == 0) {
            state.addExposure(HorrorType.DIMENSIONAL, 0.5F);
        }

        float exp = state.getExposure(HorrorType.DIMENSIONAL);
        if (exp >= 30 && gameTime % 100 == 0 && Math.random() < 0.3) {
            // Portal-leak particles
            double a = Math.random() * Math.PI * 2;
            double r = 3 + Math.random() * 4;
            double px = sp.getX() + Math.cos(a) * r;
            double py = sp.getY() + 1 + Math.random();
            double pz = sp.getZ() + Math.sin(a) * r;
            level.sendParticles(ParticleTypes.PORTAL, px, py, pz, 5, 0.3, 0.3, 0.3, 0.02);
        }

        // Sound bleed em exposure muito alta
        if (exp >= 70 && gameTime % 400 == 0 && Math.random() < 0.2) {
            sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.4F, 0.7F);
        }

        // Decay
        if (gameTime % 200 == 0 && swaps < 3) {
            state.decayExposure(HorrorType.DIMENSIONAL, baseDecayRate() * 2);
        }
    }
}
