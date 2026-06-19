package br.com.murilo.liberthia.magic.spell.vfx;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.146 r114: <b>HelixSpawner</b> — gera partículas em padrão helicoidal
 * em volta de um eixo. Usado pra:
 * <ul>
 *   <li>Cast windup (helix subindo do chão pro caster durante channel)</li>
 *   <li>Buff aura permanente (helix orbital)</li>
 *   <li>Summon ritual (helix descendente de spawn)</li>
 * </ul>
 *
 * <p>Spawna {@link ConfigurableParticleOptions} (já existe no mod) com cor
 * da escola — não polui com novas particle types.
 *
 * <p>Original code. Usa engine de partícula própria do Liberthia.
 */
public final class HelixSpawner {

    private HelixSpawner() {}

    /**
     * Spawna helix em volta de um centro.
     *
     * @param sl       ServerLevel
     * @param center   ponto central
     * @param school   cor da escola
     * @param radius   raio do helix (blocos)
     * @param height   altura total (blocos)
     * @param turns    quantas voltas completas
     * @param density  partículas por tick (típico 4-8)
     * @param phase    offset de rotação (0-2π, varia por tick pra dar movimento)
     * @param emissive se true, partícula emissive (full bright)
     */
    public static void spawn(ServerLevel sl, Vec3 center, SpellSchool school,
                              double radius, double height, double turns,
                              int density, double phase, boolean emissive) {
        int hex = school.colorHex();
        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;

        ConfigurableParticleOptions opt = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                r, g, b, 0.85F,
                0.25F, 0.04F,      // startSize → endSize (shrink)
                14,                 // lifetime
                0.0F, 0.92F,        // gravity, friction
                0.4F,               // spinSpeed
                false, emissive, true);

        for (int i = 0; i < density; i++) {
            double t = (double) i / density;
            double angle = phase + t * Math.PI * 2 * turns;
            double y = center.y + t * height;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            sl.sendParticles(opt, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /** Helix duplo (2 fios espiralados, 180° apart) — visual ainda mais rico. */
    public static void spawnDouble(ServerLevel sl, Vec3 center, SpellSchool school,
                                    double radius, double height, double turns,
                                    int densityEach, double phase) {
        spawn(sl, center, school, radius, height, turns, densityEach, phase, true);
        spawn(sl, center, school, radius, height, turns, densityEach, phase + Math.PI, true);
    }

    /** Anel horizontal (helix com 0 height, 1 turn) — usado em explosões radiais. */
    public static void spawnRing(ServerLevel sl, Vec3 center, SpellSchool school,
                                  double radius, int density, double phase) {
        spawn(sl, center, school, radius, 0, 1, density, phase, true);
    }
}
