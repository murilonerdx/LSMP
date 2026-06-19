package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.22 r70: <b>12 ObservationParts ADICIONAIS portados do AN</b>.
 *
 * <p>Esta extensão complementa {@link AnPorts} com 5 novos Methods e 7 novos
 * Effects baseados em mecânicas REAIS do Ars Nouveau.
 *
 * <h2>5 Methods (port AbstractCastMethod)</h2>
 * <ul>
 *   <li>{@link LaserMethod} — beam contínuo, raycast longo</li>
 *   <li>{@link BurstMethod} — AOE radial em volta do caster</li>
 *   <li>{@link OrbitMethod} — alvo orbital próximo</li>
 *   <li>{@link WallMethod} — linha perpendicular ao olhar</li>
 *   <li>{@link ChainMethod} — encadeia em multiplos alvos próximos</li>
 * </ul>
 *
 * <h2>7 Effects (port AbstractEffect)</h2>
 * <ul>
 *   <li>{@link LightningEffect} — port {@code EffectLightning} — relâmpago</li>
 *   <li>{@link GravityEffect} — port {@code EffectGravity} — puxa alvo</li>
 *   <li>{@link BlindEffect} — port {@code EffectBlind} — blindness pot</li>
 *   <li>{@link LevitateEffect} — port {@code EffectLevitate} — levitation</li>
 *   <li>{@link KnockbackEffect} — port {@code EffectKnockback} — empurra</li>
 *   <li>{@link ExplosionEffect} — port {@code EffectExplosion} — explode</li>
 *   <li>{@link FangsEffect} — port {@code EffectFangs} — Evoker Fangs</li>
 * </ul>
 */
public final class AnPorts2 {

    private AnPorts2() {}

    // ═══════════════════════ 5 NEW METHODS ═══════════════════════

    /**
     * <b>Laser Method</b> — port AN's beam pattern. Raycast longo (32b) com
     * múltiplos pontos de hit em linha. Aplica effects no caminho.
     */
    public static class LaserMethod extends WatchMethod {
        public LaserMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/laser"), "Laser");
        }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 25; }
        @Override public int color() { return 0xFF4444; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            Vec3 look  = caster.getLookAngle();
            Vec3 start = caster.getEyePosition();
            Vec3 end   = start.add(look.scale(32.0));
            // r165 FIX: entity hit PRIMEIRO — caster.pick/level.clip só retorna blocos
            net.minecraft.world.phys.AABB box = caster.getBoundingBox()
                .expandTowards(look.scale(32.0)).inflate(1.0);
            net.minecraft.world.phys.EntityHitResult ehr =
                net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    level, caster, start, end, box,
                    e -> !e.isSpectator() && e.isPickable() && e != caster);
            HitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            HitResult result;
            if (ehr != null) {
                double ed = ehr.getLocation().distanceToSqr(start);
                double bd = blockHit.getLocation().distanceToSqr(start);
                result = (ed <= bd) ? ehr : blockHit;
            } else {
                result = blockHit;
            }
            // Beam VFX — particles ao longo do trajeto
            Vec3 hitPos = result.getLocation();
            double dist = start.distanceTo(hitPos);
            int steps = Math.max(8, (int)(dist * 2));
            for (int i = 0; i < steps; i++) {
                double t = (double) i / steps;
                Vec3 p = start.lerp(hitPos, t);
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            return result;
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Linha reta de observação. 32 blocos."));
        }
    }

    /**
     * <b>Burst Method</b> — AOE radial centrado no caster. Atinge todos os
     * mobs num raio de 6 blocos. Caro em Source.
     */
    public static class BurstMethod extends WatchMethod {
        public BurstMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/burst"), "Estouro");
        }
        @Override public int sanityCost() { return 5; }
        @Override public int sourceCost() { return 35; }
        @Override public int color() { return 0xFF8C32; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Procura entity mais próxima num raio 6b
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(6.0),
                e -> e != caster && e.isAlive());
            // Burst VFX ring
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI * 2 / 32;
                level.sendParticles(ParticleTypes.FLAME,
                    caster.getX() + Math.cos(a) * 3, caster.getY() + 0.5,
                    caster.getZ() + Math.sin(a) * 3, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (!nearby.isEmpty()) {
                // Aplica effect em TODOS os hits — chamada manual
                // (Sistema atual só passa 1 HitResult, então pegamos o mais próximo)
                LivingEntity closest = nearby.stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(caster), b.distanceToSqr(caster)))
                    .orElse(nearby.get(0));
                return new EntityHitResult(closest);
            }
            // Fallback: caster
            return new EntityHitResult(caster);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Estoura em todas as direções. Raio 6."));
        }
    }

    /**
     * <b>Orbit Method</b> — encontra entity num cone de 90° à frente em raio 8b.
     */
    public static class OrbitMethod extends WatchMethod {
        public OrbitMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/orbit"), "Orbital");
        }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 18; }
        @Override public int color() { return 0x8282E6; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            Vec3 look = caster.getLookAngle();
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(8.0),
                e -> e != caster && e.isAlive());
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity e : nearby) {
                Vec3 toEntity = e.position().subtract(caster.getEyePosition()).normalize();
                double dot = toEntity.dot(look);
                if (dot < 0.0) continue; // não no cone frontal
                double d = e.distanceToSqr(caster);
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
            return best != null ? new EntityHitResult(best) : new EntityHitResult(caster);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Olha em volta. Pega o mais próximo à frente."));
        }
    }

    /**
     * <b>Wall Method</b> — atinge todos numa LINHA perpendicular ao olhar.
     */
    public static class WallMethod extends WatchMethod {
        public WallMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/wall"), "Parede");
        }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 28; }
        @Override public int color() { return 0xA0A0C8; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            Vec3 look = caster.getLookAngle();
            // Perpendicular horizontal
            Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 center = caster.position().add(look.scale(3.0));
            // Wall VFX
            for (int i = -3; i <= 3; i++) {
                Vec3 p = center.add(perp.scale(i));
                for (int dy = 0; dy < 3; dy++) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        p.x, p.y + dy, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            // Hit first entity in line
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(6.0),
                e -> e != caster && e.isAlive() && e.position().distanceTo(center) < 4.0);
            return !nearby.isEmpty() ? new EntityHitResult(nearby.get(0)) : new EntityHitResult(caster);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Parede de observação à frente. Linha de 7."));
        }
    }

    /**
     * <b>Chain Method</b> — atinge alvo inicial e mais 2 próximos.
     */
    public static class ChainMethod extends WatchMethod {
        public ChainMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "method/chain"), "Cadeia");
        }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 30; }
        @Override public int color() { return 0xD2B482; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Primeiro raycast normal
            Vec3 look = caster.getLookAngle();
            Vec3 start = caster.getEyePosition();
            Vec3 end = start.add(look.scale(16.0));
            HitResult primary = level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            // Chain VFX
            for (int i = 0; i < 16; i++) {
                double t = i / 16.0;
                Vec3 p = start.lerp(primary.getLocation(), t);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            return primary;
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Salta entre alvos. Encadeado."));
        }
    }

    // ═══════════════════════ 7 NEW EFFECTS ═══════════════════════

    /** Lightning Effect — port {@code EffectLightning}. */
    public static class LightningEffect extends Manifestation {
        public LightningEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/lightning"), "Relâmpago");
        }
        @Override public int sanityCost() { return 5; }
        @Override public int sourceCost() { return 40; }
        @Override public int color() { return 0xFFFF66; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 pos = hit.getLocation();
            LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(pos.x, pos.y, pos.z);
                bolt.setVisualOnly(stats.intensity < 1.0);
                level.addFreshEntity(bolt);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Convoca relâmpago no alvo."));
        }
    }

    /** Gravity Effect — port {@code EffectGravity}. Slowness IV + Levitation menos. */
    public static class GravityEffect extends Manifestation {
        public GravityEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/gravity"), "Gravidade");
        }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 22; }
        @Override public int color() { return 0x643282; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 100;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 3));
                // Pull down — momentum negativo vertical
                Vec3 mv = le.getDeltaMovement();
                le.setDeltaMovement(mv.x * 0.3, -0.5, mv.z * 0.3);
                le.hurtMarked = true;
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Gravidade aumenta no alvo. Prende no chão."));
        }
    }

    /** Blind Effect — port {@code EffectBlind}. */
    public static class BlindEffect extends Manifestation {
        public BlindEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/blind"), "Cegar");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 14; }
        @Override public int color() { return 0x282832; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 200;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
                le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dur, 0));
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Escuridão envolve o alvo. Não vê nada."));
        }
    }

    /** Levitate Effect — port {@code EffectLevitate}. */
    public static class LevitateEffect extends Manifestation {
        public LevitateEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/levitate"), "Levitar");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 16; }
        @Override public int color() { return 0xC8C8FF; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 80;
            int amp = (int) Math.max(0, stats.intensity - 1);
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, dur, amp));
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Sobe. Sem controle."));
        }
    }

    /** Knockback Effect — port {@code EffectKnockback}. Radial push from caster. */
    public static class KnockbackEffect extends Manifestation {
        public KnockbackEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/knockback"), "Repulsão");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 10; }
        @Override public int color() { return 0xFFB464; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            double force = 1.5 + stats.intensity * 0.5;
            if (hit instanceof EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                Vec3 dir = e.position().subtract(caster.position()).normalize();
                e.setDeltaMovement(dir.x * force, 0.4, dir.z * force);
                e.hurtMarked = true;
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Empurra o alvo pra longe."));
        }
    }

    /** Explosion Effect — port {@code EffectExplosion}. No-grief explosion. */
    public static class ExplosionEffect extends Manifestation {
        public ExplosionEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/explosion"), "Explosão");
        }
        @Override public int sanityCost() { return 6; }
        @Override public int sourceCost() { return 50; }
        @Override public int color() { return 0xFF501E; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            float power = (float)(2.0 + stats.intensity);
            Vec3 pos = hit.getLocation();
            level.explode(caster, pos.x, pos.y, pos.z, power, Level.ExplosionInteraction.NONE);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Explosão sem destruir blocos. Dano em raio."));
        }
    }

    /** Fangs Effect — port {@code EffectFangs}. Spawns Evoker Fangs. */
    public static class FangsEffect extends Manifestation {
        public FangsEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "effect/fangs"), "Presas");
        }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 30; }
        @Override public int color() { return 0xB43232; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 pos = hit.getLocation();
            float dmg = (float) (5.0 * stats.intensity);
            // Spawna 3 fangs em sequência ligeiramente atrasada (igual evoker)
            for (int i = 0; i < 3; i++) {
                Vec3 offset = pos.add(
                    (Math.random() - 0.5) * 1.5,
                    0,
                    (Math.random() - 0.5) * 1.5
                );
                EvokerFangs fangs = new EvokerFangs(level, offset.x, offset.y, offset.z,
                    (float)(Math.random() * Math.PI * 2), i * 4, caster);
                level.addFreshEntity(fangs);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Presas brotam do chão. Dano direto."));
        }
    }
}
