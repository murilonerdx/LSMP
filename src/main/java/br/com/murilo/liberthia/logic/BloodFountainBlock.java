package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Blood Fountain — pulses blood around it, spawns worms, and erupts like a
 * tiny volcano every ~300 ticks. Uses vanilla water as "blood" (colored red
 * via client-side particle overlay + damage indicator particles).
 */
public class BloodFountainBlock extends Block {

    public BloodFountainBlock(Properties props) {
        super(props.randomTicks().strength(3.5F, 12.0F).lightLevel(s -> 6));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !BloodKin.is(entity)) {
            living.hurt(level.damageSources().magic(), 2.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        // Spread blood (Living Flesh — NOT water) around the fountain
        for (int i = 0; i < 3; i++) {
            int dx = rng.nextInt(5) - 2;
            int dy = rng.nextInt(2) - 1;
            int dz = rng.nextInt(5) - 2;
            BlockPos p = pos.offset(dx, dy, dz);
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced() || cur.is(Blocks.WATER) || cur.is(Blocks.LAVA)) {
                float roll = rng.nextFloat();
                Block choice;
                if (roll < 0.45F) choice = ModBlocks.BLOOD_FLUID_BLOCK.get(); // red liquid
                else if (roll < 0.7F) choice = ModBlocks.LIVING_FLESH.get();
                else if (roll < 0.9F) choice = ModBlocks.ATTACKING_FLESH.get();
                else choice = ModBlocks.BLOOD_INFECTION_BLOCK.get();
                level.setBlockAndUpdate(p, choice.defaultBlockState());
            }
        }

        // Spawn worms (20% chance per random tick)
        if (rng.nextFloat() < 0.20F) {
            trySpawnWorm(level, pos, rng);
        }

        // Eruption every few ticks
        if (rng.nextFloat() < 0.08F) {
            erupt(level, pos, rng);
        }

        // Ambient pulse particles
        level.sendParticles(BloodParticles.BLOOD,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                8, 0.4, 0.3, 0.4, 0.05);
    }

    private void trySpawnWorm(ServerLevel level, BlockPos pos, RandomSource rng) {
        var type = ModEntities.BLOOD_WORM.get();
        for (int tries = 0; tries < 4; tries++) {
            int dx = rng.nextInt(5) - 2;
            int dz = rng.nextInt(5) - 2;
            BlockPos sp = pos.offset(dx, 1, dz);
            if (level.getBlockState(sp).isAir() && level.getBlockState(sp.above()).isAir()) {
                var worm = type.create(level);
                if (worm != null) {
                    worm.moveTo(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, rng.nextFloat() * 360F, 0F);
                    level.addFreshEntity(worm);
                    return;
                }
            }
        }
    }

    private void erupt(ServerLevel level, BlockPos pos, RandomSource rng) {
        // Visual eruption: huge burst of blood particles
        level.sendParticles(BloodParticles.BLOOD,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                120, 1.5, 2.0, 1.5, 0.6);
        level.sendParticles(ParticleTypes.LAVA,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                8, 0.5, 0.3, 0.5, 0.3);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                30, 1.0, 1.0, 1.0, 0.05);

        // Sound: ghast + fire blast
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 0.4F);
        level.playSound(null, pos, SoundEvents.GHAST_HURT, SoundSource.BLOCKS, 1.5F, 0.6F);

        // Knock back and bleed nearby entities
        AABB aoe = new AABB(pos).inflate(6.0);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, aoe);
        for (LivingEntity e : victims) {
            if (BloodKin.is(e)) continue;
            double dx = e.getX() - (pos.getX() + 0.5);
            double dz = e.getZ() - (pos.getZ() + 0.5);
            double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            e.push(dx / len * 0.8, 0.6, dz / len * 0.8);
            e.hurt(level.damageSources().magic(), 4.0F);
        }

        // Toss blood blobs outward — LIVING_FLESH blocks, not water
        for (int i = 0; i < 6; i++) {
            int dx = rng.nextInt(9) - 4;
            int dz = rng.nextInt(9) - 4;
            BlockPos p = pos.offset(dx, rng.nextInt(3), dz);
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced()) {
                // Eruption scatters blood liquid, not flesh
                level.setBlockAndUpdate(p, ModBlocks.BLOOD_FLUID_BLOCK.get().defaultBlockState());
            }
        }

        // Occasionally spew a worm mid-air
        if (rng.nextFloat() < 0.5F) {
            trySpawnWorm(level, pos, rng);
        }
    }
}
