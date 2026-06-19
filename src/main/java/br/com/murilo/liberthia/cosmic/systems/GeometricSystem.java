package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * r81 #15/18 — <b>Geometric Horror</b>.
 *
 * <p>Math impossível. Particles seguem padrões fractais — pra player em
 * exposição alta, vê figuras geométricas (espiral de Fibonacci, triangulos)
 * formadas no ar próximo. Em níveis muito altos, ouvir um pulse rate
 * cardíaco descompassado.
 */
public final class GeometricSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.GEOMETRIC;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 40 != 0) return;

        // Exposição passa quando player vê céu noturno + chunks com structures
        if (!level.isDay() && level.canSeeSky(sp.blockPosition())) {
            state.addExposure(HorrorType.GEOMETRIC, 0.3F);
        }

        float exp = state.getExposure(HorrorType.GEOMETRIC);
        if (exp >= 40 && Math.random() < 0.3) {
            spawnFractalPattern(sp, level, gameTime);
        }

        // Decay
        if (gameTime % 200 == 0 && level.isDay()) {
            state.decayExposure(HorrorType.GEOMETRIC, baseDecayRate());
        }
    }

    private void spawnFractalPattern(ServerPlayer sp, ServerLevel level, long t) {
        // Espiral de Fibonacci — particles em pos calculadas
        double phi = 1.618033988;
        for (int i = 0; i < 12; i++) {
            double angle = i * phi * Math.PI * 2 + (t * 0.001);
            double r = Math.sqrt(i) * 0.6;
            double px = sp.getX() + Math.cos(angle) * r;
            double py = sp.getY() + 2 + Math.sin(angle) * 0.5;
            double pz = sp.getZ() + Math.sin(angle) * r;
            level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0);
        }
    }
}
