package br.com.murilo.liberthia.observation.vfx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * v0.1.22 r61: <b>Casting VFX</b> — helper pra spawn particles cinematográficas
 * em cada fase de execução de spell.
 *
 * <h2>Inspirado em (não copiado)</h2>
 * AN's ProjectileTimeline com onCast/onTrail/onResolving/onFlair. Aqui temos
 * 3 fases simples:
 *
 * <ul>
 *   <li>{@link #spiralIn(ServerPlayer, Vec3, int)} — pré-cast, espiral pra dentro</li>
 *   <li>{@link #burstOut(ServerLevel, Vec3, int, int)} — momento do resolve</li>
 *   <li>{@link #lingerAt(ServerLevel, Vec3, int, int)} — fade after</li>
 * </ul>
 *
 * <p>Todos respeitam o `color` do spell (RGB int).
 */
public final class CastVfx {

    private CastVfx() {}

    /** Spiral particles entrando no caster antes do cast — usa GlowParticle custom. */
    public static void spiralIn(ServerPlayer caster, Vec3 center, int color) {
        if (!(caster.level() instanceof ServerLevel level)) return;

        // r63: usa NOSSA custom GlowParticle (additive blend, fade alpha, color)
        var glow = new br.com.murilo.liberthia.observation.particle.GlowData(
            color, 0.5F, 1.0F, 20);

        // 20 particles em espiral
        for (int i = 0; i < 20; i++) {
            double t = i / 20.0;
            double angle = t * Math.PI * 4;
            double radius = (1.0 - t) * 2.5;
            double y = caster.getY() + 0.5 + t * 1.0;
            double x = caster.getX() + Math.cos(angle) * radius;
            double z = caster.getZ() + Math.sin(angle) * radius;
            level.sendParticles(glow, x, y, z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Burst radial no momento da resolution — usa GlowParticle custom. */
    public static void burstOut(ServerLevel level, Vec3 center, int color, int intensity) {
        // r63: usa nossa custom GlowParticle pra hit burst (additive, big size)
        var glowBig = new br.com.murilo.liberthia.observation.particle.GlowData(
            color, 1.0F, 1.0F, 25);

        int count = Math.max(15, intensity * 10);
        for (int i = 0; i < count; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * intensity;
            double dy = (Math.random() - 0.5) * 1.5;
            level.sendParticles(glowBig,
                center.x + Math.cos(a) * r,
                center.y + dy,
                center.z + Math.sin(a) * r,
                1, 0.05, 0.05, 0.05, 0.02);
        }
        // Soul flare central (extra mood)
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            center.x, center.y + 0.5, center.z,
            Math.min(20, intensity * 4), 0.3, 0.5, 0.3, 0.03);
    }

    /** Smoke ribbon lingering after the effect. */
    public static void lingerAt(ServerLevel level, Vec3 center, int color, int duration) {
        Vector3f rgb = rgb(color);
        DustParticleOptions dust = new DustParticleOptions(rgb, 0.6F);

        int particles = Math.min(30, duration / 4);
        for (int i = 0; i < particles; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 0.3 + Math.random() * 1.5;
            level.sendParticles(dust,
                center.x + Math.cos(a) * r,
                center.y + Math.random() * 2,
                center.z + Math.sin(a) * r,
                1, 0.01, 0.05, 0.01, 0.01);
        }
        // SMOKE for atmosphere
        level.sendParticles(ParticleTypes.SMOKE,
            center.x, center.y + 1, center.z,
            5, 0.2, 0.3, 0.2, 0.01);
    }

    /** Particle line entre 2 pontos — agora usa LineParticle REAL com lerp interno. */
    public static void beamBetween(ServerLevel level, Vec3 start, Vec3 end, int color) {
        // r64: LineParticle interpola start → end internamente via tick()
        var line = br.com.murilo.liberthia.observation.particle.LineData.rgb(
            color, 0.4F, 16, end.x, end.y, end.z);
        // Spawn 8 particles ao longo do start, cada uma se move pra end
        Vec3 dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        for (int i = 0; i < 8; i++) {
            double t = i / 8.0;
            Vec3 p = start.add(dir.scale(dist * t));
            level.sendParticles(line, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static Vector3f rgb(int color) {
        return new Vector3f(
            ((color >> 16) & 0xFF) / 255F,
            ((color >> 8) & 0xFF) / 255F,
            (color & 0xFF) / 255F);
    }
}
