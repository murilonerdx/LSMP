package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.22 r72: <b>10 UtilityGlyphs portados do AN.</b>
 *
 * <h2>7 Effects utility</h2>
 * <ul>
 *   <li>{@link PlaceBlockEffect} — port {@code EffectPlaceBlock}</li>
 *   <li>{@link BreakBlockEffect} — port {@code EffectBreakBlock}</li>
 *   <li>{@link ConjureWaterEffect} — port {@code EffectConjureWater}</li>
 *   <li>{@link LightEffect} — port {@code EffectLight}</li>
 *   <li>{@link SnareEffect} — port {@code EffectSnare}</li>
 *   <li>{@link HexEffect} — port {@code EffectHex} (random debuff)</li>
 *   <li>{@link PickupEffect} — port {@code EffectPickup}</li>
 * </ul>
 *
 * <h2>3 Augments (Distortions)</h2>
 * <ul>
 *   <li>{@link PierceAugment} — port {@code AugmentPierce}</li>
 *   <li>{@link SplitAugment} — port {@code AugmentSplit}</li>
 *   <li>{@link AoeAugment} — port {@code AugmentAOE}</li>
 * </ul>
 */
public final class UtilityGlyphs {

    private UtilityGlyphs() {}

    // ═══════════════════════ 7 EFFECTS UTILITY ═══════════════════════

    /** Place Block — places dirt/stone where hit. Useful pra construção. */
    public static class PlaceBlockEffect extends Manifestation {
        public PlaceBlockEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/place_block"), "Colocar Bloco");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 8; }
        @Override public int color() { return 0x886655; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (!(hit instanceof BlockHitResult bhr)) return;
            BlockPos target = bhr.getBlockPos().relative(bhr.getDirection());
            if (level.getBlockState(target).canBeReplaced()) {
                // Type baseado em intensity: stone/dirt/cobblestone
                var blockState = stats.intensity > 1.5 ? Blocks.STONE.defaultBlockState() :
                                 stats.intensity > 0.7 ? Blocks.COBBLESTONE.defaultBlockState() :
                                                          Blocks.DIRT.defaultBlockState();
                level.setBlock(target, blockState, 3);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§7Materializa um bloco onde olhar.")); }
    }

    /** Break Block — breaks 1 block at hit. */
    public static class BreakBlockEffect extends Manifestation {
        public BreakBlockEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/break_block"), "Quebrar Bloco");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 12; }
        @Override public int color() { return 0xA08866; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (!(hit instanceof BlockHitResult bhr)) return;
            BlockPos target = bhr.getBlockPos();
            var state = level.getBlockState(target);
            if (state.getDestroySpeed(level, target) >= 0 && !state.isAir()) {
                level.destroyBlock(target, true, caster);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§7Quebra o bloco mirado. Drop incluído.")); }
    }

    /** Conjure Water — places water source block. */
    public static class ConjureWaterEffect extends Manifestation {
        public ConjureWaterEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/conjure_water"), "Conjurar Água");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 10; }
        @Override public int color() { return 0x3399FF; }
        @Override public SpellClass spellClass() { return SpellClass.WATER; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (!(hit instanceof BlockHitResult bhr)) return;
            BlockPos target = bhr.getBlockPos().relative(bhr.getDirection());
            if (level.getBlockState(target).canBeReplaced()) {
                level.setBlock(target, Blocks.WATER.defaultBlockState(), 3);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§bMaterializa source block de água.")); }
    }

    /** Light — places torch / lantern. */
    public static class LightEffect extends Manifestation {
        public LightEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/light"), "Iluminar");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 8; }
        @Override public int color() { return 0xFFEE88; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (!(hit instanceof BlockHitResult bhr)) return;
            BlockPos target = bhr.getBlockPos().relative(bhr.getDirection());
            if (level.getBlockState(target).canBeReplaced()) {
                level.setBlock(target, Blocks.TORCH.defaultBlockState(), 3);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§eColoca tocha no ar/superfície.")); }
    }

    /** Snare — heavy slowness + nausea. Pode prender entity por 5s. */
    public static class SnareEffect extends Manifestation {
        public SnareEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/snare"), "Armadilha");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 15; }
        @Override public int color() { return 0x666644; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                int dur = stats.duration > 0 ? stats.duration : 100;
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 5));
                le.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, -10));
                le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, 3));
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§7Prende o alvo no chão por 5s.")); }
    }

    /** Hex — random debuff. */
    public static class HexEffect extends Manifestation {
        private static final MobEffectInstance[] HEX_POOL = new MobEffectInstance[] {
            new MobEffectInstance(MobEffects.WITHER, 200, 1),
            new MobEffectInstance(MobEffects.POISON, 200, 1),
            new MobEffectInstance(MobEffects.WEAKNESS, 300, 1),
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2),
            new MobEffectInstance(MobEffects.BLINDNESS, 200, 0),
            new MobEffectInstance(MobEffects.HUNGER, 200, 2),
            new MobEffectInstance(MobEffects.CONFUSION, 200, 0),
            new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 2)
        };
        public HexEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/hex"), "Maldição");
        }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 25; }
        @Override public int color() { return 0x664488; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                // 3 random effects
                java.util.Set<Integer> picked = new java.util.HashSet<>();
                while (picked.size() < 3 && picked.size() < HEX_POOL.length) {
                    picked.add((int)(Math.random() * HEX_POOL.length));
                }
                for (int i : picked) {
                    le.addEffect(new MobEffectInstance(HEX_POOL[i]));
                }
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5Aplica 3 debuffs aleatórios.")); }
    }

    /** Pickup — pulls items in radius to caster. */
    public static class PickupEffect extends Manifestation {
        public PickupEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "util/pickup"), "Atrair Itens");
        }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 10; }
        @Override public int color() { return 0xCCCC44; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 center = caster.position();
            var items = level.getEntitiesOfClass(ItemEntity.class,
                caster.getBoundingBox().inflate(16.0), e -> true);
            for (var item : items) {
                Vec3 dir = center.subtract(item.position()).normalize();
                item.setDeltaMovement(dir.scale(0.8));
                item.setPickUpDelay(0);
                level.sendParticles(ParticleTypes.GLOW,
                    item.getX(), item.getY()+0.5, item.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§ePuxa items em 16b pro caster.")); }
    }

    // ═══════════════════════ 3 AUGMENTS (Distortions) ═══════════════════════

    /** Pierce — projectile passa por inimigos (modifica stats.duration como pierce count). */
    public static class PierceAugment extends Distortion {
        public PierceAugment() {
            super(new ResourceLocation(LiberthiaMod.MODID, "aug/pierce"), "Perfurar");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 15; }
        @Override public int color() { return 0xCC8844; }
        @Override
        public int sanityCostForPart(ObservationPart parent) { return 2; }
        @Override
        public void applyToStats(ObservationStats.Builder b, ObservationPart augmented) {
            // Pierce aumenta reach (+2 alcance) e intensity (+0.5)
            b.addReach(2.0).addIntensity(0.5);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§6Projéteis perfuram. Reach +2.")); }
    }

    /** Split — N projéteis simultâneos. Stats.echoCount armazena split count. */
    public static class SplitAugment extends Distortion {
        public SplitAugment() {
            super(new ResourceLocation(LiberthiaMod.MODID, "aug/split"), "Multiplicar");
        }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 20; }
        @Override public int color() { return 0xDD66CC; }
        @Override
        public int sanityCostForPart(ObservationPart parent) { return 3; }
        @Override
        public void applyToStats(ObservationStats.Builder b, ObservationPart augmented) {
            // Cada Split adiciona +1 echo (= mais "cópias" do effect)
            b.addEchoCount(1);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§dMultiplica o feitiço +1×.")); }
    }

    /** AOE — aumenta raio de efeito. */
    public static class AoeAugment extends Distortion {
        public AoeAugment() {
            super(new ResourceLocation(LiberthiaMod.MODID, "aug/aoe"), "Área");
        }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 18; }
        @Override public int color() { return 0x77CC77; }
        @Override
        public int sanityCostForPart(ObservationPart parent) { return 3; }
        @Override
        public void applyToStats(ObservationStats.Builder b, ObservationPart augmented) {
            b.addReach(3.0).addIntensity(-0.2); // mais área, menos dano por alvo
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§aRaio +3, intensidade −20%.")); }
    }
}
