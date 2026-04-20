package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blood Volcano — a dramatic, persistent blood-erupting block.
 * Continuous lava-blood particle column, periodic massive eruptions with shockwave,
 * spawns worms, spreads blood flesh in a wide radius, and sets nearby entities
 * ablaze with bloody fire particles.
 */
public class BloodVolcanoBlock extends Block {
    public BloodVolcanoBlock(Properties p) { super(p); }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        // constant lava-blood column
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.3 + rng.nextDouble() * 0.4,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.3 + rng.nextDouble() * 0.4,
                    (rng.nextDouble() - 0.5) * 0.2,
                    0.3 + rng.nextDouble() * 0.5,
                    (rng.nextDouble() - 0.5) * 0.2);
        }
        for (int i = 0; i < 8; i++) {
            level.addParticle(BloodParticles.BLOOD,
                    pos.getX() + rng.nextDouble(),
                    pos.getY() + 1.05 + rng.nextDouble() * 3.0,
                    pos.getZ() + rng.nextDouble(),
                    (rng.nextDouble() - 0.5) * 0.3,
                    0.2 + rng.nextDouble() * 0.4,
                    (rng.nextDouble() - 0.5) * 0.3);
        }
        // ambient embers
        if (rng.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    (rng.nextDouble()-0.5)*0.4, 0.3, (rng.nextDouble()-0.5)*0.4);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        // Spread blood infection/flesh in wide radius
        for (int i = 0; i < 5; i++) {
            int dx = rng.nextInt(11) - 5;
            int dy = rng.nextInt(3) - 1;
            int dz = rng.nextInt(11) - 5;
            BlockPos p = pos.offset(dx, dy, dz);
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced()) {
                Block choice;
                float r = rng.nextFloat();
                if (r < 0.4F) choice = ModBlocks.BLOOD_INFECTION_BLOCK.get();
                else if (r < 0.7F) choice = ModBlocks.BLOOD_INFESTATION_BLOCK.get();
                else if (r < 0.9F) choice = ModBlocks.LIVING_FLESH.get();
                else choice = ModBlocks.ATTACKING_FLESH.get();
                level.setBlockAndUpdate(p, choice.defaultBlockState());
            }
        }

        // Spawn worms frequently
        for (int tries = 0; tries < 3; tries++) {
            int dx = rng.nextInt(5) - 2;
            int dz = rng.nextInt(5) - 2;
            BlockPos sp = pos.offset(dx, 1, dz);
            if (level.getBlockState(sp).isAir()) {
                var type = rng.nextInt(3) == 0 ? ModEntities.GORE_WORM : ModEntities.FLESH_CRAWLER;
                var worm = type.get().create(level);
                if (worm != null) {
                    worm.moveTo(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, rng.nextFloat() * 360F, 0F);
                    level.addFreshEntity(worm);
                    break;
                }
            }
        }

        // Massive eruption
        if (rng.nextFloat() < 0.15F) {
            erupt(level, pos, rng);
        }
    }

    private void erupt(ServerLevel level, BlockPos pos, RandomSource rng) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
        level.sendParticles(BloodParticles.BLOOD,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                220, 2.5, 3.0, 2.5, 0.8);
        level.sendParticles(ParticleTypes.LAVA,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                40, 1.0, 2.0, 1.0, 0.6);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                60, 2.0, 2.5, 2.0, 0.2);
        level.sendParticles(ParticleTypes.FLASH,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3.0F, 0.3F);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.2F, 0.5F);

        AABB aoe = new AABB(pos).inflate(10.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aoe)) {
            if (e instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(e)) continue;
            double dx = e.getX() - (pos.getX() + 0.5);
            double dz = e.getZ() - (pos.getZ() + 0.5);
            double len = Math.max(0.1, Math.sqrt(dx*dx + dz*dz));
            e.push(dx/len * 1.4, 1.0, dz/len * 1.4);
            e.hurt(level.damageSources().magic(), 8.0F);
            e.setSecondsOnFire(4);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (BloodKin.is(entity)) return;
        if (!level.isClientSide && entity instanceof LivingEntity le
                && !(entity instanceof Player p && p.isCreative())) {
            le.hurt(le.damageSources().onFire(), 3.0F);
            le.setSecondsOnFire(3);
        }
    }
}
