package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.22 r68: <b>8 ObservationParts portados DIRETO do Ars Nouveau source.</b>
 *
 * <p>Diferente das MoreParts.java (que tem nomes/efeitos temáticos cosmic horror
 * inventados), estes 8 parts replicam mecânicas REAIS do AN que eu li no source:
 *
 * <h2>2 Methods (port AbstractCastMethod)</h2>
 * <ul>
 *   <li>{@link TouchMethod} — port {@code MethodTouch} — aplica em entity em raio 3b</li>
 *   <li>{@link SelfMethod} — port {@code MethodSelf} — aplica no caster</li>
 * </ul>
 *
 * <h2>6 Effects (port AbstractEffect)</h2>
 * <ul>
 *   <li>{@link IgniteEffect} — port {@code EffectIgnite} (li source direto)</li>
 *   <li>{@link HarmEffect} — port {@code EffectHarm} — magic damage</li>
 *   <li>{@link HealEffect} — port {@code EffectHeal} — heal target</li>
 *   <li>{@link FreezeEffect} — port {@code EffectFreeze} — slowness + freeze ticks</li>
 *   <li>{@link LaunchEffect} — port {@code EffectLaunch} — knockback vertical</li>
 *   <li>{@link SlowfallEffect} — port {@code EffectSlowfall} — slow falling</li>
 * </ul>
 *
 * <p>Cada effect respeita SpellStats: AMP_VALUE pra damage, EXTEND_TIME pra duration,
 * igual AN's pattern. POTION_TIME + extends → duration final.
 */
public final class AnPorts {

    private AnPorts() {}

    // ═══════════════════════ METHODS (port AbstractCastMethod) ═══════════════════════

    /**
     * <b>Touch Method</b> — port AN's {@code MethodTouch}.
     *
     * <p>Em vez de raycast longo (DirectGaze), aplica em entity no raio 3b
     * em volta do caster. Igual AN: pega closest LivingEntity, fallback pra block.
     */
    public static class TouchMethod extends WatchMethod {
        public TouchMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/touch"), "Toque");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 5; }
        @Override public int color() { return 0xCCFFCC; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Procura entity próxima (igual AN's MethodTouch)
            Vec3 origin = caster.position();
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(3.0),
                e -> e != caster && e.isAlive());
            LivingEntity closest = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity e : nearby) {
                double d = e.position().distanceToSqr(origin);
                if (d < bestDist) {
                    bestDist = d;
                    closest = e;
                }
            }
            if (closest != null) {
                return new EntityHitResult(closest);
            }
            // Fallback: bloco logo à frente
            Vec3 look = caster.getLookAngle();
            Vec3 start = caster.getEyePosition();
            Vec3 end = start.add(look.scale(2.0));
            return level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Alcance imediato. O toque é a observação mais íntima."));
        }
    }

    /**
     * <b>Self Method</b> — port AN's {@code MethodSelf}.
     *
     * <p>O caster é o alvo. Útil pra spells de buff (Heal, Slowfall, etc).
     */
    public static class SelfMethod extends WatchMethod {
        public SelfMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/self"), "Em Si");
        }
        @Override public int sanityCost() { return 0; }
        @Override public int sourceCost() { return 2; }
        @Override public int color() { return 0xFFEEAA; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            return new EntityHitResult(caster);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Você se observa. Todo feitiço cai sobre si."));
        }
    }

    // ═══════════════════════ EFFECTS (port AbstractEffect) ═══════════════════════

    /**
     * <b>Ignite Effect</b> — port AN's {@code EffectIgnite}.
     * <p>Li direto: {@code rayTraceResult.getEntity().setRemainingFireTicks(duration * 20)}.
     * Em block: {@code BaseFireBlock.canBePlacedAt(world, blockpos1, face)}.
     *
     * <p>Custos default AN: POTION_TIME=3s, EXTEND_TIME=2s.
     */
    public static class IgniteEffect extends Manifestation {
        public IgniteEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/ignite"), "Ignitar");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 15; }
        @Override public int color() { return 0xFF7733; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int baseDurationSec = 3;
            int extraDurationSec = (int) (2 * (stats.duration / 60.0)); // duration mult
            int fireTicks = (baseDurationSec + extraDurationSec) * 20;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.setRemainingFireTicks(fireTicks);
            } else if (hit instanceof BlockHitResult bhr) {
                // Pattern AN: tenta colocar fogo na face adjacente
                var face = bhr.getDirection();
                var pos = bhr.getBlockPos().relative(face);
                if (level.getBlockState(pos).canBeReplaced()
                        && net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, pos, face)) {
                    level.setBlock(pos,
                        net.minecraft.world.level.block.BaseFireBlock.getState(level, pos), 3);
                }
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Calor da observação fixa. O alvo arde."));
        }
    }

    /**
     * <b>Harm Effect</b> — port AN's {@code EffectHarm}. Magic damage scaled with intensity.
     */
    public static class HarmEffect extends Manifestation {
        public HarmEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/harm"), "Ferir");
        }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 25; }
        @Override public int color() { return 0xBB0000; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            float baseDamage = 5.0F;
            float damage = (float) (baseDamage * stats.intensity);
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.hurt(level.damageSources().magic(), damage);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Dano direto. A observação corta como vidro."));
        }
    }

    /**
     * <b>Heal Effect</b> — port AN's {@code EffectHeal}. Cura proporcional à intensity.
     */
    public static class HealEffect extends Manifestation {
        public HealEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/heal"), "Curar");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 20; }
        @Override public int color() { return 0x55EE55; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            float baseHeal = 6.0F;
            float heal = (float) (baseHeal * stats.intensity);
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.heal(heal);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Restaura vitalidade. O olhar costura."));
        }
    }

    /**
     * <b>Freeze Effect</b> — port AN's {@code EffectFreeze}. Slowness + Mining Fatigue
     * + freeze ticks (vanilla powder snow effect).
     */
    public static class FreezeEffect extends Manifestation {
        public FreezeEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/freeze"), "Congelar");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 18; }
        @Override public int color() { return 0x88DDFF; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int durTicks = stats.duration > 0 ? stats.duration : 100;
            int amp = (int) Math.max(0, stats.intensity - 1);
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durTicks, amp + 1));
                le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durTicks, amp));
                // Pattern AN: aplica freeze ticks (powder snow effect)
                le.setTicksFrozen(le.getTicksFrozen() + durTicks);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7A observação congela. O alvo lentifica."));
        }
    }

    /**
     * <b>Launch Effect</b> — port AN's {@code EffectLaunch}. Knockback up.
     */
    public static class LaunchEffect extends Manifestation {
        public LaunchEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/launch"), "Arremessar");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 10; }
        @Override public int color() { return 0xFFFFAA; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            double launchPower = 1.0 + stats.intensity * 0.5;
            if (hit instanceof EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                Vec3 mv = e.getDeltaMovement();
                e.setDeltaMovement(mv.x, launchPower, mv.z);
                e.hurtMarked = true; // sync movement to client
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Empurrão vertical. Quem é visto sobe."));
        }
    }

    /**
     * <b>Slowfall Effect</b> — port AN's {@code EffectSlowfall}. Slow Falling potion effect.
     */
    public static class SlowfallEffect extends Manifestation {
        public SlowfallEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/slowfall"), "Queda Lenta");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 8; }
        @Override public int color() { return 0xEEDDFF; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int durTicks = stats.duration > 0 ? stats.duration : 200;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, durTicks, 0));
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Gravidade observada com paciência. Não cai."));
        }
    }
}
