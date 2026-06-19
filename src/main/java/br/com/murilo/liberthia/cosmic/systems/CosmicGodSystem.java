package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * r81 #1/18 — <b>Cosmic Horror</b> (Lovecraft).
 *
 * <p>Sentir um deus dormindo abaixo da realidade. Ticka particles raras
 * de SOUL no céu, adiciona exposição quando o player olha pra cima
 * por muito tempo, gera um "olhar do horizonte" subtle.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>A cada 200 ticks (10s), 10% de chance de spawnar particles em
 *       formação distante simulando "olho dormindo"</li>
 *   <li>Player olhando pra cima (pitch &lt; -50°) por 10s seguidos: +5 exposição</li>
 *   <li>Em altitudes &gt;= 200: exposição passiva +0.3/tick</li>
 * </ul>
 */
public final class CosmicGodSystem implements HorrorSystem {

    private final java.util.Map<java.util.UUID, Integer> lookUpStreak = new java.util.HashMap<>();

    @Override
    public HorrorType type() {
        return HorrorType.COSMIC;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        java.util.UUID id = sp.getUUID();

        // (1) Stare-up streak — olhar pra cima carrega exposição
        if (sp.getXRot() < -50) {
            int streak = lookUpStreak.getOrDefault(id, 0) + 1;
            lookUpStreak.put(id, streak);
            if (streak >= 200) {  // 10s
                state.addExposure(HorrorType.COSMIC, 5.0F);
                lookUpStreak.put(id, 0);
                // Tira blindness brief — "viu algo grande demais"
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, true, false));
            }
        } else {
            lookUpStreak.remove(id);
        }

        // (2) High altitude exposure — ar fino, mente vaga
        if (sp.getY() >= 200 && gameTime % 20 == 0) {
            state.addExposure(HorrorType.COSMIC, 0.3F);
        }

        // (3) Distant eye particle — só visível em horror alto
        if (gameTime % 200 == 0 && state.getExposure(HorrorType.COSMIC) > 30) {
            if (Math.random() < 0.1) {
                spawnDistantEye(sp, level);
            }
        }

        // (4) Decay em overworld dia
        if (gameTime % 100 == 0 && isSafe(sp, level)) {
            state.decayExposure(HorrorType.COSMIC, baseDecayRate());
        }
    }

    private void spawnDistantEye(ServerPlayer sp, ServerLevel level) {
        // Forma um olho distante composto de particles SOUL (~50 blocos acima, lateral)
        double a = Math.random() * Math.PI * 2;
        double r = 40 + Math.random() * 20;
        double cx = sp.getX() + Math.cos(a) * r;
        double cy = sp.getY() + 30 + Math.random() * 10;
        double cz = sp.getZ() + Math.sin(a) * r;
        // 12 particles em círculo + 4 no centro (formando "olho")
        for (int i = 0; i < 12; i++) {
            double ta = (i / 12.0) * Math.PI * 2;
            double tr = 2.5;
            level.sendParticles(ParticleTypes.SOUL,
                    cx + Math.cos(ta) * tr, cy, cz + Math.sin(ta) * tr,
                    1, 0.05, 0.05, 0.05, 0);
        }
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 1, 0.2, 0.2, 0.2, 0);
        }
    }

    private boolean isSafe(ServerPlayer sp, ServerLevel level) {
        // Dia + overworld + acima do nível do mar = safe
        return level.dimension().toString().contains("overworld")
                && level.isDay()
                && sp.getY() >= 60
                && sp.getY() < 180;
    }
}
