package br.com.murilo.liberthia.magic.spell.vfx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * r168: <b>SpellVfx</b> — fluent VFX choreography engine.
 *
 * <p>Compose layered, time-delayed visual effects for spells using a chainable
 * builder. Each step queues a {@link ServerLevel#getServer()} task via
 * {@link ScheduledVfx#schedule}, so the whole sequence plays out across many
 * ticks while the caster method returns immediately.
 *
 * <h2>Example</h2>
 * <pre>
 * SpellVfx.builder()
 *     .at(caster.position().add(0,1,0))
 *         .pack2(Pack2VfxCatalog.Type.MANDALA, 6, 1.5f)   // windup
 *         .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f)
 *     .delay(15)
 *     .at(targetPos)
 *         .pack2(Pack2VfxCatalog.Type.ERUPTION, 0, 2.5f)  // impact
 *         .ring(Pack2VfxCatalog.Type.RUNIC_SEAL, 9, 4f)
 *     .delay(20)
 *     .at(targetPos)
 *         .pack2(Pack2VfxCatalog.Type.FIREWORK, 0, 3f)    // finale
 *     .execute(level, caster);
 * </pre>
 */
public final class SpellVfx {

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<Step> steps = new ArrayList<>();
        private Vec3 currentPos = Vec3.ZERO;
        private int currentDelay = 0;

        private Builder() {}

        // ── Position anchors ───────────────────────────────────────────────

        /** Subsequent effects fire at this world position. */
        public Builder at(Vec3 pos) {
            this.currentPos = pos;
            return this;
        }
        public Builder at(double x, double y, double z) { return at(new Vec3(x, y, z)); }
        public Builder atEntity(Entity ent, double yOff) { return at(ent.position().add(0, yOff, 0)); }
        public Builder atEntityHead(LivingEntity le)     { return at(le.position().add(0, le.getBbHeight() * 0.9, 0)); }

        /** All subsequent effects are delayed by an additional {@code ticks}. */
        public Builder delay(int ticks) {
            this.currentDelay += Math.max(0, ticks);
            return this;
        }

        // ── Pack-2 sprite VFX ──────────────────────────────────────────────

        /** Spawn a Pack-2 billboard VFX at current position. */
        public Builder pack2(Pack2VfxCatalog.Type type, int colorIdx, float scale) {
            final Vec3 p = currentPos;
            steps.add(new Step(currentDelay, (ctx) -> Pack2VfxCatalog.spawn(
                    ctx.level, p, ctx.caster, type, colorIdx, scale)));
            return this;
        }

        /** Spawn a Pack-2 effect riding an entity (follows it). */
        public Builder pack2Ride(LivingEntity host, Pack2VfxCatalog.Type type, int colorIdx, float scale) {
            steps.add(new Step(currentDelay, (ctx) -> {
                SpriteVfxEntity ent = new SpriteVfxEntity(ctx.level, ctx.caster,
                        host.position(), type, colorIdx, scale);
                ent.startRiding(host, true);
                ctx.level.addFreshEntity(ent);
            }));
            return this;
        }

        // ── Pack-1 effects (for ground circles, motion-axis trails, etc.) ──

        /** Spawn a Pack-1 effect (with orientation modes — ground/motion). */
        public Builder pack1(SpriteVfxRegistry.Type type, float scale) {
            final Vec3 p = currentPos;
            steps.add(new Step(currentDelay, (ctx) ->
                    SpriteVfxRegistry.spawn(ctx.level, p, ctx.caster, type, scale)));
            return this;
        }

        /** Convenience — spawns a {@link SpriteVfxRegistry.Type#PROTECTION} circle. */
        public Builder protectionCircle(float scale) { return pack1(SpriteVfxRegistry.Type.PROTECTION, scale); }
        /** Convenience — spawns a {@link SpriteVfxRegistry.Type#CASTING} circle. */
        public Builder castingCircle(float scale)    { return pack1(SpriteVfxRegistry.Type.CASTING, scale); }

        // ── Particle bursts ────────────────────────────────────────────────

        /** Burst {@code count} particles at the current position. */
        public Builder particles(ParticleOptions p, int count, double spread, double speed) {
            final Vec3 pos = currentPos;
            steps.add(new Step(currentDelay, (ctx) ->
                    ctx.level.sendParticles(p, pos.x, pos.y, pos.z, count,
                            spread, spread, spread, speed)));
            return this;
        }

        /** Ring of particles at the current position. */
        public Builder particleRing(ParticleOptions p, double radius, int count) {
            final Vec3 pos = currentPos;
            steps.add(new Step(currentDelay, (ctx) -> {
                for (int i = 0; i < count; i++) {
                    double a = (i / (double) count) * Math.PI * 2;
                    ctx.level.sendParticles(p,
                            pos.x + Math.cos(a) * radius,
                            pos.y,
                            pos.z + Math.sin(a) * radius,
                            1, 0, 0, 0, 0.02);
                }
            }));
            return this;
        }

        // ── Sound ──────────────────────────────────────────────────────────

        public Builder sound(SoundEvent ev, float volume, float pitch) {
            final Vec3 pos = currentPos;
            steps.add(new Step(currentDelay, (ctx) ->
                    ctx.level.playSound(null,
                            BlockPos.containing(pos),
                            ev, SoundSource.PLAYERS, volume, pitch)));
            return this;
        }
        public Builder sound(SoundEvent ev, float volume) { return sound(ev, volume, 1.0F); }

        // ── Custom action (escape hatch) ───────────────────────────────────

        public Builder run(Consumer<PlayContext> action) {
            steps.add(new Step(currentDelay, action));
            return this;
        }

        // ── Execution ──────────────────────────────────────────────────────

        /** Fire the whole sequence. Future-scheduled actions use ScheduledVfx. */
        public void execute(ServerLevel level, LivingEntity caster) {
            PlayContext ctx = new PlayContext(level, caster);
            for (Step s : steps) {
                if (s.delay <= 0) {
                    safeRun(s.action, ctx);
                } else {
                    final Step st = s;
                    ScheduledVfx.schedule(level, st.delay, () -> safeRun(st.action, ctx));
                }
            }
        }

        private static void safeRun(Consumer<PlayContext> a, PlayContext ctx) {
            try { a.accept(ctx); } catch (Throwable ignored) {}
        }
    }

    // ── Internal types ─────────────────────────────────────────────────────

    public static final class PlayContext {
        public final ServerLevel level;
        public final LivingEntity caster;
        public PlayContext(ServerLevel level, LivingEntity caster) {
            this.level = level; this.caster = caster;
        }
    }

    private static final class Step {
        final int delay;
        final Consumer<PlayContext> action;
        Step(int d, Consumer<PlayContext> a) { delay = d; action = a; }
    }

    private SpellVfx() {}
}
