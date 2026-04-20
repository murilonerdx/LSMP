package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blood Infection block — infectious corruption. Spreads to adjacent normal
 * terrain (grass/dirt/stone/sand) and infects entities that step on it or
 * walk nearby with Blood Infection II.
 * Contained by 4+ chalk symbols nearby.
 */
public class BloodInfectionBlock extends Block {
    public BloodInfectionBlock(Properties p) { super(p); }

    @Override public boolean isRandomlyTicking(BlockState s) { return true; }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        // Altar dependency: no spread without an uncontained altar nearby
        if (!BloodAltarBlock.hasActiveAltarNearby(level, pos, 20)) return;
        // Spread onto adjacent vanilla natural blocks (Y limited, anti-float)
        for (int i = 0; i < 3; i++) {
            int ox = rng.nextInt(3) - 1;
            int oy = rng.nextInt(3) - 1; // -1..+1 only
            int oz = rng.nextInt(3) - 1;
            if (ox == 0 && oy == 0 && oz == 0) continue;
            BlockPos target = pos.offset(ox, oy, oz);
            BlockState ts = level.getBlockState(target);
            // Anti-float: require solid block below the target
            BlockPos belowTarget = target.below();
            if (!level.getBlockState(belowTarget).isFaceSturdy(level, belowTarget, net.minecraft.core.Direction.UP))
                continue;
            if (ts.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                    || ts.is(net.minecraft.world.level.block.Blocks.DIRT)
                    || ts.is(net.minecraft.world.level.block.Blocks.STONE)
                    || ts.is(net.minecraft.world.level.block.Blocks.SAND)
                    || ts.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
                    || ts.is(net.minecraft.world.level.block.Blocks.PODZOL)
                    || ts.is(net.minecraft.world.level.block.Blocks.MYCELIUM)) {
                level.setBlockAndUpdate(target, this.defaultBlockState());
                level.sendParticles(BloodParticles.BLOOD,
                        target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                        8, 0.3, 0.2, 0.3, 0.05);
                level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5,
                        6, 0.35, 0.1, 0.35, 0.02);
                break;
            }
        }
        // Infect nearby entities (except blood-kin)
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2.5))) {
            if (e instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(e)) continue;
            e.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 300, 1, false, true, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        // Constant red dust for visibility
        for (int i = 0; i < 2; i++) {
            level.addParticle(BloodParticles.BLOOD,
                    pos.getX() + rng.nextDouble(),
                    pos.getY() + 1.02,
                    pos.getZ() + rng.nextDouble(),
                    (rng.nextDouble() - 0.5) * 0.05, 0.04, (rng.nextDouble() - 0.5) * 0.05);
        }
        if (rng.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CRIMSON_SPORE,
                    pos.getX() + rng.nextDouble(),
                    pos.getY() + 1.05,
                    pos.getZ() + rng.nextDouble(),
                    0, 0.02, 0);
        }
        if (rng.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.3 + rng.nextDouble() * 0.4,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.3 + rng.nextDouble() * 0.4,
                    0, 0.02, 0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (BloodKin.is(entity)) return;
        if (!level.isClientSide && entity instanceof LivingEntity le
                && !(entity instanceof Player p && p.isCreative())) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 400, 1, false, true, true));
        }
    }
}
