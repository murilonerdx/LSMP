package br.com.murilo.liberthia.magic.spell.vfx;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * v0.1.146 r115: <b>ScheduledVfx</b> — engine de efeitos visuais agendados
 * por tick. Permite criar efeitos SUSTAINED (não single-burst) sem precisar
 * criar Entity nova pra cada um.
 *
 * <h2>Casos de uso</h2>
 * <ul>
 *   <li><b>Ground Decal</b> — anel de partículas no chão que expande + fade out</li>
 *   <li><b>Area Pulse</b> — esfera de partículas que pulsa por N ticks</li>
 *   <li><b>Sustained Beam</b> — feixe entre 2 pontos por N ticks</li>
 *   <li><b>Aura</b> — partículas em volta de uma entidade por N ticks (follows entity)</li>
 * </ul>
 *
 * <h2>Como funciona</h2>
 * <ol>
 *   <li>Spell registra um efeito via static {@code spawn*()} method</li>
 *   <li>ScheduledVfx mantém List de {@link Effect} ativos</li>
 *   <li>{@link TickEvent.ServerTickEvent} END → itera List, executa tick() de cada</li>
 *   <li>Quando ageTicks ≥ maxTicks, remove da lista</li>
 * </ol>
 *
 * <p>Server-only — particles sao spawn via {@code sendParticles()} que sincroniza
 * client-side automatico.
 *
 * <p>Original code 100%. Não derivado.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ScheduledVfx {

    private static final List<Effect> ACTIVE = new ArrayList<>();
    /**
     * r157: Buffer de adds durante tick — evita {@link java.util.ConcurrentModificationException}
     * quando uma effect chama {@code spawn*()} dentro de seu próprio tick (ex: meteor storm
     * agendando próximo meteor, beam spawnando aoe burst no fim, etc.).
     */
    private static final List<Effect> PENDING_ADDS = new ArrayList<>();
    private static volatile boolean ticking = false;

    private ScheduledVfx() {}

    private static void addEffect(Effect e) {
        if (ticking) {
            synchronized (PENDING_ADDS) { PENDING_ADDS.add(e); }
        } else {
            ACTIVE.add(e);
        }
    }

    /** Efeito base — subclasses customizam o tick(). */
    public static abstract class Effect {
        public final ServerLevel level;
        public final Vec3 center;
        public final SpellSchool school;
        public final int maxTicks;
        public int age;

        protected Effect(ServerLevel level, Vec3 center, SpellSchool school, int maxTicks) {
            this.level = level;
            this.center = center;
            this.school = school;
            this.maxTicks = Math.max(1, maxTicks);
            this.age = 0;
        }

        /** Progresso 0.0 (início) → 1.0 (fim). */
        protected float progress() {
            return Math.min(1F, (float) age / maxTicks);
        }

        /** Chamado a cada server tick enquanto ativo. Implementar VFX aqui. */
        public abstract void tick();

        /** Sobrescrever pra spawnar 1-shot effects ao terminar (ex: explosion final). */
        public void onEnd() {}
    }

    // ─── Public spawn helpers ───────────────────────────────────────────

    /**
     * Ground Decal — anel de partículas no chão que expande de 0 até maxRadius
     * ao longo de duration ticks, com fade-out nos últimos 30%.
     */
    public static void spawnGroundDecal(ServerLevel sl, Vec3 center, SpellSchool school,
                                         double maxRadius, int duration) {
        addEffect(new GroundDecalEffect(sl, center, school, duration, maxRadius));
    }

    /**
     * Area Pulse — esfera de partículas que pulsa (densidade varia com sin wave)
     * por N ticks, raio fixo.
     */
    public static void spawnAreaPulse(ServerLevel sl, Vec3 center, SpellSchool school,
                                       double radius, int duration) {
        addEffect(new AreaPulseEffect(sl, center, school, duration, radius));
    }

    /**
     * Sustained Beam — feixe sustentado entre 2 pontos por N ticks.
     * Re-spawna o beam a cada tick com leve jitter pra dar feel dinâmico.
     */
    public static void spawnSustainedBeam(ServerLevel sl, Vec3 from, Vec3 to,
                                           SpellSchool school, float thickness, int duration) {
        addEffect(new SustainedBeamEffect(sl, from, school, duration, to, thickness));
    }

    /** r148: wrapper conveniente — beam com thickness padrão 0.4. */
    public static void spawnBeam(ServerLevel sl, Vec3 from, Vec3 to,
                                  SpellSchool school, int duration) {
        spawnSustainedBeam(sl, from, to, school, 0.4F, duration);
    }

    /**
     * r148: Aura — partículas em volta de uma entidade VIVA por N ticks.
     * Acompanha o movimento da entidade (re-spawna em volta dela a cada tick).
     */
    public static void spawnAura(ServerLevel sl, net.minecraft.world.entity.LivingEntity host,
                                  SpellSchool school, double radius, int duration) {
        addEffect(new AuraEffect(sl, host, school, duration, radius));
    }

    /** r148: Agenda um Runnable pra rodar após {@code delayTicks} server ticks. */
    public static void schedule(ServerLevel sl, int delayTicks, Runnable action) {
        addEffect(new DelayedRunEffect(sl, school0(sl), delayTicks, action));
    }

    /** Helper interno — schedule não precisa de school real. */
    private static SpellSchool school0(ServerLevel sl) { return SpellSchool.FIRE; }

    public static int activeCount() { return ACTIVE.size(); }

    public static void clearAll() { ACTIVE.clear(); }

    // ─── Tick driver ────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Flush pending adds from previous tick (effects added during tick())
        synchronized (PENDING_ADDS) {
            if (!PENDING_ADDS.isEmpty()) {
                ACTIVE.addAll(PENDING_ADDS);
                PENDING_ADDS.clear();
            }
        }
        if (ACTIVE.isEmpty()) return;

        ticking = true;
        try {
            // r157: iterate by INDEX with snapshot of size — adds from tick() vão pra
            // PENDING_ADDS via addEffect(), evitando ConcurrentModificationException.
            // Removes são acumulados em "toRemove" e aplicados em batch ao final.
            java.util.List<Effect> toRemove = null;
            int n = ACTIVE.size();
            for (int i = 0; i < n; i++) {
                Effect e = ACTIVE.get(i);
                try {
                    e.tick();
                } catch (Throwable t) {
                    LiberthiaMod.LOGGER.warn("[ScheduledVfx] tick error: {}", t.toString());
                }
                e.age++;
                if (e.age >= e.maxTicks) {
                    try { e.onEnd(); } catch (Throwable ignored) {}
                    if (toRemove == null) toRemove = new ArrayList<>();
                    toRemove.add(e);
                }
            }
            if (toRemove != null) ACTIVE.removeAll(toRemove);
        } finally {
            ticking = false;
        }
    }

    // ═══ Concrete Effect implementations ═════════════════════════════════

    /**
     * Anel no chão expandindo. Spawna 24 partículas por tick em raio crescente.
     * Fade-out: nos últimos 30% das partículas saem com alpha menor (não dá
     * pra controlar alpha diretamente — usamos partículas menores).
     */
    static class GroundDecalEffect extends Effect {
        final double maxRadius;

        GroundDecalEffect(ServerLevel level, Vec3 center, SpellSchool school,
                          int maxTicks, double maxRadius) {
            super(level, center, school, maxTicks);
            this.maxRadius = maxRadius;
        }

        @Override
        public void tick() {
            float p = progress();
            // Ease-out cubic — expande rápido depois desacelera
            double curRadius = maxRadius * (1 - Math.pow(1 - p, 3));
            // 24 partículas no anel + 4 extras radiais
            int density = 24 + (int)(p * 8);
            double phase = age * 0.1; // rotação lenta
            HelixSpawner.spawnRing(level, center.add(0, 0.05, 0), school,
                    curRadius, density, phase);
        }

        @Override
        public void onEnd() {
            // Final burst — anel completo de uma vez
            HelixSpawner.spawnRing(level, center.add(0, 0.1, 0), school, maxRadius, 40, 0);
        }
    }

    /**
     * Esfera de partículas que pulsa.
     */
    static class AreaPulseEffect extends Effect {
        final double radius;

        AreaPulseEffect(ServerLevel level, Vec3 center, SpellSchool school,
                        int maxTicks, double radius) {
            super(level, center, school, maxTicks);
            this.radius = radius;
        }

        @Override
        public void tick() {
            // Pulse cada 5t: spawn 8-12 particles em pontos random na esfera
            if (age % 3 != 0) return;
            float p = progress();
            // Fade-out density nos últimos 40%
            int density = (int)(12 * (1 - Math.max(0, p - 0.6) / 0.4));
            for (int i = 0; i < density; i++) {
                // Random ponto numa esfera
                double theta = Math.random() * Math.PI * 2;
                double phi = Math.acos(2 * Math.random() - 1);
                double r = radius * (0.5 + Math.random() * 0.5); // 50-100% raio
                double x = center.x + r * Math.sin(phi) * Math.cos(theta);
                double y = center.y + r * Math.cos(phi);
                double z = center.z + r * Math.sin(phi) * Math.sin(theta);
                // Spawn um SpellTrail particle no ponto
                br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData data =
                        new br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData(
                                school, 0.6F, 14);
                level.sendParticles(data, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Beam contínuo entre 2 pontos. Re-desenhado a cada 2 ticks com jitter.
     */
    static class SustainedBeamEffect extends Effect {
        final Vec3 to;
        final float thickness;

        SustainedBeamEffect(ServerLevel level, Vec3 from, SpellSchool school,
                            int maxTicks, Vec3 to, float thickness) {
            super(level, from, school, maxTicks);
            this.to = to;
            this.thickness = thickness;
        }

        @Override
        public void tick() {
            if (age % 2 != 0) return;
            SpellBeam.drawSegment(level, center, to, school, thickness, 6);
        }
    }

    /**
     * r148: Aura que segue uma entidade — orbita partículas em volta dela
     * por N ticks. Para se a entidade morrer.
     */
    static class AuraEffect extends Effect {
        final net.minecraft.world.entity.LivingEntity host;
        final double radius;

        AuraEffect(ServerLevel level, net.minecraft.world.entity.LivingEntity host,
                   SpellSchool school, int maxTicks, double radius) {
            super(level, host.position(), school, maxTicks);
            this.host = host;
            this.radius = radius;
        }

        @Override
        public void tick() {
            if (!host.isAlive()) {
                age = maxTicks; // termina
                return;
            }
            // 6 partículas orbitando em formação helicoidal
            double phase = age * 0.18;
            for (int i = 0; i < 6; i++) {
                double a = phase + (Math.PI * 2 / 6) * i;
                double y = host.getY() + 0.5 + Math.sin(age * 0.1 + i) * 0.3;
                double x = host.getX() + Math.cos(a) * radius;
                double z = host.getZ() + Math.sin(a) * radius;
                br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData data =
                        new br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData(
                                school, 0.7F, 12);
                level.sendParticles(data, x, y, z, 1, 0, 0, 0, 0.02);
            }
        }
    }

    /**
     * r148: Roda um Runnable após {@code maxTicks} server ticks. Único uso
     * de Effect com tick() vazio + onEnd() = ação.
     */
    static class DelayedRunEffect extends Effect {
        final Runnable action;

        DelayedRunEffect(ServerLevel level, SpellSchool school, int delayTicks, Runnable action) {
            super(level, Vec3.ZERO, school, Math.max(1, delayTicks));
            this.action = action;
        }

        @Override public void tick() { /* no-op */ }

        @Override
        public void onEnd() {
            try { action.run(); }
            catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[ScheduledVfx] delayed action error: {}", t.toString());
            }
        }
    }
}
