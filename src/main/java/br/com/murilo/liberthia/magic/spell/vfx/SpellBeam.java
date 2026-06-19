package br.com.murilo.liberthia.magic.spell.vfx;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.observation.particle.LineData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.146 r114: <b>SpellBeam</b> — helper estático que desenha feixes
 * encadeados de partículas entre N pontos, usando o LineParticle existente.
 *
 * <h2>Uso típico</h2>
 * <ul>
 *   <li>Chain lightning: passa lista de targets, conecta um→outro com beams</li>
 *   <li>Beam spell: 2 pontos (caster→alvo), single beam segment</li>
 *   <li>Tendril/tentacle: caster→alvo com leve curvatura (passa pontos intermediários)</li>
 * </ul>
 *
 * <h2>Visual</h2>
 * Cada segmento é uma série de LineData particles spawnadas com lerp
 * (start→dest) ao longo do lifetime — efeito de "raio elétrico" sustentado.
 * Cor automática da escola passada.
 *
 * <p>Original code. Reusa o LineParticle que já existe no Liberthia desde r64.
 */
public final class SpellBeam {

    private SpellBeam() {}

    /** Desenha 1 segmento de feixe entre 2 pontos. */
    public static void drawSegment(ServerLevel sl, Vec3 from, Vec3 to,
                                    SpellSchool school, float thickness, int lifeTicks) {
        int hex = school.colorHex();
        // Densidade: 4 particles per block of distance
        double dist = from.distanceTo(to);
        int count = (int) Math.max(2, Math.min(40, dist * 4));
        Vec3 step = to.subtract(from).scale(1.0 / count);

        for (int i = 0; i <= count; i++) {
            Vec3 p = from.add(step.scale(i));
            // pequena vibração radial pra dar feel de raio
            double jitter = thickness * 0.15;
            double jx = (Math.random() - 0.5) * jitter;
            double jy = (Math.random() - 0.5) * jitter;
            double jz = (Math.random() - 0.5) * jitter;

            LineData ld = LineData.rgb(hex, thickness, lifeTicks,
                    p.x + jx, p.y + jy, p.z + jz);
            sl.sendParticles(ld, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Desenha cadeia de feixes ligando N pontos em sequência (chain lightning). */
    public static void drawChain(ServerLevel sl, Vec3[] points,
                                  SpellSchool school, float thickness, int lifeTicks) {
        if (points.length < 2) return;
        for (int i = 0; i < points.length - 1; i++) {
            drawSegment(sl, points[i], points[i + 1], school, thickness, lifeTicks);
        }
    }

    /** Desenha feixe com curvatura senoidal (estilo tentáculo/cobra). */
    public static void drawCurved(ServerLevel sl, Vec3 from, Vec3 to,
                                   SpellSchool school, float thickness, int lifeTicks,
                                   float amplitude, int waves) {
        double dist = from.distanceTo(to);
        int count = (int) Math.max(4, Math.min(60, dist * 6));
        Vec3 dir = to.subtract(from).normalize();
        // Perpendicular axis pra curvatura (cross com up)
        Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 perp = dir.cross(up).normalize();

        int hex = school.colorHex();
        for (int i = 0; i <= count; i++) {
            double t = (double) i / count;
            Vec3 base = from.add(to.subtract(from).scale(t));
            // Senoidal curve perpendicular ao eixo
            double offset = Math.sin(t * Math.PI * waves) * amplitude;
            Vec3 p = base.add(perp.scale(offset));

            LineData ld = LineData.rgb(hex, thickness, lifeTicks, p.x, p.y, p.z);
            sl.sendParticles(ld, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
