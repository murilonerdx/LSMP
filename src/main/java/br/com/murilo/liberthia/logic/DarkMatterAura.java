package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Helper compartilhado pra emitir partículas finas de "aura de matéria escura"
 * em volta de blocos de infecção (CORRUPTED_SOIL, SCARRED_EARTH, SCARRED_STONE,
 * CRYSTALLIZER).
 *
 * <p>Visualmente: pequenos pontos roxos pairando próximos do bloco, dando a
 * impressão de contaminação ambiente sem alterar o bloco visualmente.
 *
 * <p>Detectável pelo {@code SampleVialItem} via scan de raio.
 */
public final class DarkMatterAura {

    private DarkMatterAura() {}

    /**
     * Emite partículas finas em volta do bloco source.
     *
     * @param level   nível (client-side, animateTick)
     * @param pos     posição do bloco source
     * @param random  RNG do animateTick
     * @param intensity multiplicador de frequência (1.0 = padrão, 2.0 = mais denso)
     */
    public static void emit(Level level, BlockPos pos, RandomSource random, float intensity) {
        // ~30% chance por animateTick × intensity
        if (random.nextFloat() >= 0.30f * intensity) return;

        // Sorteia offset em raio ±3 blocos (mantém leve)
        double dx = (random.nextDouble() - 0.5) * 6.0;
        double dy = (random.nextDouble() - 0.5) * 3.0 + 0.5;
        double dz = (random.nextDouble() - 0.5) * 6.0;

        double x = pos.getX() + 0.5 + dx;
        double y = pos.getY() + 0.5 + dy;
        double z = pos.getZ() + 0.5 + dz;

        // Velocidade quase nula — flutua no ar
        double vx = (random.nextDouble() - 0.5) * 0.01;
        double vy = random.nextDouble() * 0.02;
        double vz = (random.nextDouble() - 0.5) * 0.01;

        // PORTAL é roxinho e pequeno — combina com matéria escura
        level.addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);

        // Eventualmente um WITCH (verde-roxo) pra dar variação
        if (random.nextFloat() < 0.10f) {
            level.addParticle(ParticleTypes.WITCH,
                    x, y + 0.2, z,
                    (random.nextDouble() - 0.5) * 0.005,
                    random.nextDouble() * 0.01,
                    (random.nextDouble() - 0.5) * 0.005);
        }
    }
}
