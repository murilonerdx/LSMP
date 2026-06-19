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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r61: <b>12 novos glyphs</b> de Observation Casting. Cada um implementa
 * uma {@link WatchMethod}, {@link Manifestation}, ou {@link Distortion}.
 *
 * <h2>WatchMethods novos</h2>
 * <ul>
 *   <li>{@link PeripheralMethod} — periferia (raycast desviado)</li>
 *   <li>{@link MemoryMethod} — última posição que olhou (storage)</li>
 *   <li>{@link SilenceMethod} — só dispara se ninguém te observa</li>
 *   <li>{@link ReflectionMethod} — projeta hit pela trás do player</li>
 * </ul>
 *
 * <h2>Manifestations novas</h2>
 * <ul>
 *   <li>{@link SilenceManifestation} — slows + blindness AOE</li>
 *   <li>{@link MirrorManifestation} — invoke clone target</li>
 *   <li>{@link WhisperManifestation} — força hallucinations no target</li>
 *   <li>{@link DecayManifestation} — wither + dim radiation</li>
 *   <li>{@link GlimpseManifestation} — glow target 30s</li>
 * </ul>
 *
 * <h2>Distortions novas</h2>
 * <ul>
 *   <li>{@link LingerDistortion} — duration +60t</li>
 *   <li>{@link EchoDistortion} — echoCount +1</li>
 *   <li>{@link SecretDistortion} — perception -1 (mais escondido)</li>
 * </ul>
 */
public final class MoreParts {

    private MoreParts() {}

    // ──────────────────────── WATCH METHODS ────────────────────────

    public static class PeripheralMethod extends WatchMethod {
        public PeripheralMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "watch/peripheral"), "Olhar Periférico");
        }
        @Override public int sanityCost() { return 2; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Random side (±60°) instead of direct gaze
            Vec3 look = caster.getLookAngle();
            double angle = (Math.random() - 0.5) * Math.PI / 1.5; // ±60°
            double cos = Math.cos(angle), sin = Math.sin(angle);
            Vec3 sideways = new Vec3(
                look.x * cos - look.z * sin,
                look.y,
                look.x * sin + look.z * cos);
            Vec3 start = caster.getEyePosition();
            Vec3 end = start.add(sideways.scale(24));
            return level.clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, caster));
        }
        @Override public List<Component> lore() {
            return List.of(
                Component.literal("§5§oVocê não olhou direto."),
                Component.literal("§8§oFoi no canto do olho."));
        }
    }

    public static class MemoryMethod extends WatchMethod {
        public static final ResourceLocation MEM_KEY = new ResourceLocation(LiberthiaMod.MODID, "memory_pos");
        public MemoryMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "watch/memory"), "Memória");
        }
        @Override public int sanityCost() { return 3; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Look at last position stored in player NBT
            var data = caster.getPersistentData();
            if (!data.contains("liberthia.memory_pos_x")) {
                caster.displayClientMessage(Component.literal(
                    "§7§oNada se lembra com você."), true);
                ctx.cancel(ObservationContext.CancelReason.WATCH_FAILED);
                return null;
            }
            Vec3 pos = new Vec3(
                data.getDouble("liberthia.memory_pos_x"),
                data.getDouble("liberthia.memory_pos_y"),
                data.getDouble("liberthia.memory_pos_z"));
            return new BlockHitResult(pos,
                net.minecraft.core.Direction.UP,
                net.minecraft.core.BlockPos.containing(pos), false);
        }
        @Override public List<Component> lore() {
            return List.of(
                Component.literal("§5§oOlhe pra onde já olhou."),
                Component.literal("§8§oNão fica."));
        }
    }

    public static class SilenceMethod extends WatchMethod {
        public SilenceMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "watch/silence"), "Silêncio");
        }
        @Override public int sanityCost() { return 4; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Check if any other player looking at caster
            for (var p : level.players()) {
                if (p == caster) continue;
                Vec3 toCaster = caster.position().subtract(p.position()).normalize();
                if (toCaster.dot(p.getLookAngle()) > 0.93 && p.distanceTo(caster) < 32) {
                    caster.displayClientMessage(Component.literal(
                        "§7§oAlguém está olhando."), true);
                    ctx.cancel(ObservationContext.CancelReason.WATCH_FAILED);
                    return null;
                }
            }
            // r179 FIX: retorna a entidade mirada (Entity.pick() só pega blocos);
            // fallback pro hit de bloco se não estiver mirando ninguém.
            net.minecraft.world.entity.LivingEntity living =
                    br.com.murilo.liberthia.util.EntityRaycast.pickLiving(caster, 48.0);
            if (living != null) return new EntityHitResult(living);
            return caster.pick(48, 0, false);
        }
        @Override public List<Component> lore() {
            return List.of(
                Component.literal("§5§oSó funciona quando ninguém olha."),
                Component.literal("§8§oAchei? Cancela."));
        }
    }

    public static class ReflectionMethod extends WatchMethod {
        public ReflectionMethod() {
            super(new ResourceLocation(LiberthiaMod.MODID, "watch/reflection"), "Reflexão");
        }
        @Override public int sanityCost() { return 3; }
        @Override
        public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
            // Hit point = behind the player, projected forward
            Vec3 behind = caster.position().subtract(caster.getLookAngle().scale(4));
            return new BlockHitResult(behind,
                net.minecraft.core.Direction.UP,
                net.minecraft.core.BlockPos.containing(behind), false);
        }
        @Override public List<Component> lore() {
            return List.of(
                Component.literal("§5§oO espaço atrás existe também."),
                Component.literal("§8§oNão olhe pra trás pra confirmar."));
        }
    }

    // ──────────────────────── MANIFESTATIONS ────────────────────────

    public static class SilenceManifestation extends Manifestation {
        public SilenceManifestation() {
            super(new ResourceLocation(LiberthiaMod.MODID, "manifest/silence"), "Silenciar");
        }
        @Override public int sanityCost() { return 3; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 epicenter = hit.getLocation();
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                    caster.getBoundingBox().move(epicenter.subtract(caster.position())).inflate(stats.reach))) {
                if (le == caster) continue;
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stats.duration, 4, true, true));
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, stats.duration, 0, true, true));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stats.duration, 2, true, true));
            }
            // Particles white ash
            for (int i = 0; i < 40; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * stats.reach;
                level.sendParticles(ParticleTypes.WHITE_ASH,
                    epicenter.x + Math.cos(a) * r,
                    epicenter.y + Math.random() * 2,
                    epicenter.z + Math.sin(a) * r,
                    1, 0.05, 0.05, 0.05, 0);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oNada se move."));
        }
    }

    public static class MirrorManifestation extends Manifestation {
        public MirrorManifestation() {
            super(new ResourceLocation(LiberthiaMod.MODID, "manifest/mirror"), "Espelhar");
        }
        @Override public int sanityCost() { return 6; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            // Spawn ReflectionEntity at hit position
            try {
                var reflection = br.com.murilo.liberthia.registry.ModEntities
                    .REFLECTION_ENTITY.get().create(level);
                if (reflection != null) {
                    Vec3 pos = hit.getLocation();
                    reflection.moveTo(pos.x, pos.y, pos.z, caster.getYRot(), 0);
                    reflection.copyEquipmentFrom(caster);
                    reflection.getPersistentData().putLong("liberthia.mirror_despawn_at",
                        level.getGameTime() + stats.duration);
                    level.addFreshEntity(reflection);
                    level.sendParticles(ParticleTypes.PORTAL,
                        pos.x, pos.y + 1, pos.z, 30, 0.3, 0.5, 0.3, 0.1);
                }
            } catch (Throwable ignored) {}
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oVocê — outra vez."));
        }
    }

    public static class WhisperManifestation extends Manifestation {
        public WhisperManifestation() {
            super(new ResourceLocation(LiberthiaMod.MODID, "manifest/whisper"), "Sussurrar");
        }
        @Override public int sanityCost() { return 3; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof ServerPlayer targetSp) {
                try {
                    br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager.playDistantWhispers(targetSp);
                    br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager.playFalseFootsteps(targetSp, true);
                    targetSp.displayClientMessage(Component.literal(
                        "§5§o✦ você ouve algo."), true);
                } catch (Throwable ignored) {}
            } else if (hit instanceof EntityHitResult ehr2 && ehr2.getEntity() instanceof Mob mob) {
                mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, stats.duration, 0));
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oVoz que não é sua."));
        }
    }

    public static class DecayManifestation extends Manifestation {
        public DecayManifestation() {
            super(new ResourceLocation(LiberthiaMod.MODID, "manifest/decay"), "Decaimento");
        }
        @Override public int sanityCost() { return 5; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 epicenter = hit.getLocation();
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                    caster.getBoundingBox().move(epicenter.subtract(caster.position())).inflate(stats.reach))) {
                if (le == caster) continue;
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, stats.duration, 1, true, true));
                le.hurt(caster.damageSources().magic(), (float)(stats.intensity * 2));
            }
            for (int i = 0; i < 25; i++) {
                level.sendParticles(ParticleTypes.SQUID_INK,
                    epicenter.x + (Math.random() - 0.5) * stats.reach * 2,
                    epicenter.y + Math.random() * 2,
                    epicenter.z + (Math.random() - 0.5) * stats.reach * 2,
                    1, 0.03, 0.03, 0.03, 0.01);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oTudo se desfaz, observando ou não."));
        }
    }

    public static class GlimpseManifestation extends Manifestation {
        public GlimpseManifestation() {
            super(new ResourceLocation(LiberthiaMod.MODID, "manifest/glimpse"), "Vislumbre");
        }
        @Override public int sanityCost() { return 2; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
                caster.displayClientMessage(Component.literal(
                    "§5§oVocê o vê."), true);
            }
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oQuem você olha, glow."));
        }
    }

    // ──────────────────────── DISTORTIONS ────────────────────────

    public static class LingerDistortion extends Distortion {
        public LingerDistortion() {
            super(new ResourceLocation(LiberthiaMod.MODID, "distort/linger"), "Persistir");
        }
        @Override public int sanityCost() { return 2; }
        @Override
        public void applyToStats(ObservationStats.Builder b, @Nullable ObservationPart augmented) {
            b.addDuration(60);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oO efeito não se vai."));
        }
    }

    public static class EchoDistortion extends Distortion {
        public EchoDistortion() {
            super(new ResourceLocation(LiberthiaMod.MODID, "distort/echo"), "Eco");
        }
        @Override public int sanityCost() { return 3; }
        @Override
        public void applyToStats(ObservationStats.Builder b, @Nullable ObservationPart augmented) {
            b.addEchoCount(1);
            b.intensity = b.intensity * 0.7; // each echo is weaker
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oRepete. Mais fraco."));
        }
    }

    public static class SecretDistortion extends Distortion {
        public SecretDistortion() {
            super(new ResourceLocation(LiberthiaMod.MODID, "distort/secret"), "Secreto");
        }
        @Override public int sanityCost() { return 1; }
        @Override
        public void applyToStats(ObservationStats.Builder b, @Nullable ObservationPart augmented) {
            b.perceptionLevel = Math.max(0, b.perceptionLevel - 1);
        }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§5§oNinguém precisa saber."));
        }
    }
}
