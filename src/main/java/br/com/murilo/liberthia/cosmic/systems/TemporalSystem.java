package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * r81 #7/18 — <b>Temporal Horror</b>.
 *
 * <p>Tempo instável. Em exposição alta (&gt;= 50), 5% de chance a cada
 * 600t (30s) de simular "rewind" — som de mob distante repete, particle
 * trail aparece atrás do player como se ele tivesse caminhado por ali
 * recentemente, e o tempo do mundo "tropeça" (sub-tick).
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Exposição passiva: +0.1/tick quando o player passa de 24000 ticks
 *       jogados (player "velho")</li>
 *   <li>50+: glitches de eco</li>
 *   <li>80+: action bar message "tempo escorrega"</li>
 * </ul>
 */
public final class TemporalSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.TEMPORAL;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        // Player jogou mais de 1 dia em ticks ⇒ ganha exposição lenta
        if (sp.tickCount > 24000 && gameTime % 100 == 0) {
            state.addExposure(HorrorType.TEMPORAL, 0.1F);
        }

        float exp = state.getExposure(HorrorType.TEMPORAL);

        // Eco de movimento — particles trail simulando posição passada
        if (exp >= 50 && gameTime % 600 == 0 && Math.random() < 0.05) {
            spawnTimeEcho(sp, level);
        }

        // Action bar — só 1x por 5 min
        if (exp >= 80 && state.canFireEvent("temporal_msg", gameTime, 6000)) {
            sp.displayClientMessage(Component.literal(
                    "§5§o✦ o tempo escorrega entre seus dedos"), true);
            state.markEventFired("temporal_msg", gameTime);
        }

        // Decay
        if (gameTime % 200 == 0 && level.isDay()) {
            state.decayExposure(HorrorType.TEMPORAL, baseDecayRate());
        }
    }

    private void spawnTimeEcho(ServerPlayer sp, ServerLevel level) {
        // Particles atrás do player simulando rastro
        var look = sp.getLookAngle();
        for (int i = 1; i <= 5; i++) {
            double px = sp.getX() - look.x * i;
            double py = sp.getY() + 0.3;
            double pz = sp.getZ() - look.z * i;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    px, py, pz, 1, 0.1, 0.1, 0.1, 0);
        }
    }
}
